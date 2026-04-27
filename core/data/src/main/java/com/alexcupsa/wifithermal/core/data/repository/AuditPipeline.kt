package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.engine.audit.AnomalyDetector
import com.alexcupsa.wifithermal.core.engine.audit.RogueDetector
import com.alexcupsa.wifithermal.core.engine.audit.ScopeGuard
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AnomalyFlag
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import com.alexcupsa.wifithermal.core.model.audit.IncidentKind
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert
import com.alexcupsa.wifithermal.core.model.audit.RssiSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the audit pipeline. Lives across the
 * application process (Singleton, internal SupervisorJob scope so its
 * collector is not tied to any ViewModel).
 *
 * Responsibilities:
 * 1. Filter scan results through [ScopeGuard] using the loaded scope manifest.
 *    Out-of-scope BSSIDs never reach detectors, repositories, or logs.
 * 2. Run [RogueDetector] on the in-scope subset and publish the resulting
 *    alerts as [alerts] for the UI to consume.
 * 3. Persist incident transitions (new alerts) via [IncidentRepository].
 *    Same-key alerts are deduplicated for [INCIDENT_DEDUP_MS].
 * 4. Correlate scan results with the latest [LocationStateRepository] fix
 *    and persist [RssiSample]s for in-scope BSSIDs so [Triangulator] has
 *    historical input.
 * 5. Run [AnomalyDetector] on persisted incidents and publish the
 *    resulting [anomalies] for the dashboard.
 *
 * No offensive primitives. Pipeline only reads beacons + GPS, writes
 * findings to local Room. Layer B (capture/cracking) sits behind a
 * documented authorization gate not in this class.
 */
@Singleton
class AuditPipeline @Inject constructor(
    private val scanState: WifiScanStateRepository,
    private val whitelistRepo: WhitelistRepository,
    private val incidentRepo: IncidentRepository,
    private val rssiSampleRepo: RssiSampleRepository,
    private val scopeRepo: AuthorizationManifestRepository,
    private val locationRepo: LocationStateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val recentlyRecordedKeys = mutableMapOf<String, Long>()

    private val _alerts = MutableStateFlow<List<RogueAlert>>(emptyList())
    val alerts: StateFlow<List<RogueAlert>> = _alerts.asStateFlow()

    private val _anomalies = MutableStateFlow<List<AnomalyFlag>>(emptyList())
    val anomalies: StateFlow<List<AnomalyFlag>> = _anomalies.asStateFlow()

    init {
        scope.launch {
            combine(
                scanState.scanResults,
                whitelistRepo.observeAll(),
                scopeRepo.scope,
            ) { results, whitelist, currentScope ->
                Triple(results, whitelist, currentScope)
            }.collect { (results, whitelist, currentScope) ->
                processCycle(results, whitelist, currentScope)
            }
        }

        scope.launch {
            // Anomaly detection refreshes when persisted incidents change.
            incidentRepo.recent(limit = 500).collect { incidents ->
                _anomalies.value = AnomalyDetector.analyze(incidents)
            }
        }
    }

    private suspend fun processCycle(
        results: List<ProcessedScanResult>,
        whitelist: List<AuthorizedAccessPoint>,
        scopeManifest: AuthorizationScope?,
    ) {
        val now = System.currentTimeMillis()
        val expired = scopeManifest?.isExpired(now) ?: false

        if (scopeManifest == null || expired) {
            _alerts.value = emptyList()
            return
        }

        val inScope = ScopeGuard.filter(results, scopeManifest)
        val newAlerts = RogueDetector.analyze(inScope, whitelist, now)
        _alerts.value = newAlerts

        // Persist new alert transitions as incidents.
        mutex.withLock {
            for (alert in newAlerts) {
                val key = alertKey(alert)
                val previous = recentlyRecordedKeys[key]
                if (previous != null && now - previous < INCIDENT_DEDUP_MS) continue
                recentlyRecordedKeys[key] = now
                incidentRepo.record(alert.toIncidentEvent(now))
            }
            // Periodically prune the in-memory dedup map.
            if (recentlyRecordedKeys.size > DEDUP_MAP_PRUNE_AT) {
                recentlyRecordedKeys.entries.removeAll { now - it.value > INCIDENT_DEDUP_MS * 2 }
            }
        }

        // Correlate with current GPS fix and persist RSSI samples for
        // in-scope BSSIDs that have a sufficiently fresh fix.
        val deviceLoc = locationRepo.location.value
        if (deviceLoc != null && now - deviceLoc.timestamp < LOCATION_STALE_MS) {
            val samples = inScope.map { result ->
                RssiSample(
                    bssid = result.bssid,
                    rssi = result.rssi,
                    frequencyMhz = result.frequency,
                    lat = deviceLoc.lat,
                    lon = deviceLoc.lon,
                    accuracyM = deviceLoc.accuracyM,
                    timestamp = now,
                )
            }
            rssiSampleRepo.recordBatch(samples)
        }
    }

    private fun alertKey(alert: RogueAlert): String = when (alert) {
        is RogueAlert.UnknownAccessPoint -> "${alert.bssid}|UNKNOWN|${alert.severity}"
        is RogueAlert.EvilTwin -> "${alert.bssid}|EVIL_TWIN|${alert.impersonatedSsid}"
    }

    private fun RogueAlert.toIncidentEvent(now: Long): IncidentEvent = when (this) {
        is RogueAlert.UnknownAccessPoint -> IncidentEvent(
            id = 0,
            timestamp = now,
            kind = IncidentKind.UNKNOWN_AP_OBSERVED,
            bssid = bssid,
            ssid = ssid,
            severity = severity,
            summary = "Unknown AP '${ssid.ifEmpty { "<hidden>" }}' rssi $rssi dBm crypto ${security.name}",
            evidenceJson = """{"rssi":$rssi,"vendor":${vendor.jsonOrNull()},"security":"${security.name}"}""",
        )
        is RogueAlert.EvilTwin -> IncidentEvent(
            id = 0,
            timestamp = now,
            kind = IncidentKind.EVIL_TWIN_DETECTED,
            bssid = bssid,
            ssid = impersonatedSsid,
            severity = severity,
            summary = "EVIL TWIN of '$impersonatedSsid' (real BSSID $authorizedBssid)",
            evidenceJson = """{"rssi":$rssi,"authorizedBssid":"$authorizedBssid","vendor":${vendor.jsonOrNull()}}""",
        )
    }

    private fun String?.jsonOrNull(): String =
        this?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"

    @Suppress("unused")
    fun severityHistogram(alerts: List<RogueAlert>): Map<AlertSeverity, Int> =
        alerts.groupBy { it.severity }.mapValues { it.value.size }

    companion object {
        private const val INCIDENT_DEDUP_MS = 5 * 60_000L
        private const val LOCATION_STALE_MS = 60_000L
        private const val DEDUP_MAP_PRUNE_AT = 200
    }
}
