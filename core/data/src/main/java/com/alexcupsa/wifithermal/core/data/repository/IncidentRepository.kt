package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.database.dao.IncidentDao
import com.alexcupsa.wifithermal.core.database.entity.IncidentEntity
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import com.alexcupsa.wifithermal.core.model.audit.IncidentKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Append-only audit log. Used both as forensic record (timestamps + bssid +
 * evidence) and as input for time-series anomaly analysis in v0.5+.
 */
@Singleton
class IncidentRepository @Inject constructor(
    private val dao: IncidentDao,
) {
    fun recent(limit: Int = 200): Flow<List<IncidentEvent>> =
        dao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    fun forBssid(bssid: String): Flow<List<IncidentEvent>> =
        dao.getByBssid(bssid).map { list -> list.map { it.toDomain() } }

    fun countSince(sinceMs: Long): Flow<Int> = dao.countSince(sinceMs)

    suspend fun record(event: IncidentEvent): Long = dao.insert(event.toEntity())

    suspend fun pruneBefore(cutoffMs: Long): Int = dao.pruneBefore(cutoffMs)

    suspend fun clear() = dao.deleteAll()
}

private fun IncidentEntity.toDomain() = IncidentEvent(
    id = id,
    timestamp = timestamp,
    kind = runCatching { IncidentKind.valueOf(kind) }.getOrDefault(IncidentKind.UNKNOWN_AP_OBSERVED),
    bssid = bssid,
    ssid = ssid,
    severity = runCatching { AlertSeverity.valueOf(severity) }.getOrDefault(AlertSeverity.INFO),
    summary = summary,
    evidenceJson = evidenceJson,
)

private fun IncidentEvent.toEntity() = IncidentEntity(
    id = id,
    timestamp = timestamp,
    kind = kind.name,
    bssid = bssid,
    ssid = ssid,
    severity = severity.name,
    summary = summary,
    evidenceJson = evidenceJson,
)
