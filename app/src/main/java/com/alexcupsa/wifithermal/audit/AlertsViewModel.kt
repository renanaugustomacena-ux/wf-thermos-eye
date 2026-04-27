package com.alexcupsa.wifithermal.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.WhitelistRepository
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.engine.audit.RogueDetector
import com.alexcupsa.wifithermal.core.engine.audit.ScopeGuard
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import com.alexcupsa.wifithermal.core.model.audit.DeviceType
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<RogueAlert> = emptyList(),
    val severityFilter: AlertSeverity? = null,
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val whitelistRepo: WhitelistRepository,
    scanState: WifiScanStateRepository,
    scopeRepo: AuthorizationManifestRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow<AlertSeverity?>(null)

    val state: StateFlow<AlertsUiState> = combine(
        scanState.scanResults,
        whitelistRepo.observeAll(),
        scopeRepo.scope,
        _filter,
    ) { results, whitelist, scope, filter ->
        val now = System.currentTimeMillis()
        val alerts = if (scope == null || scope.isExpired(now)) {
            emptyList()
        } else {
            val inScope = ScopeGuard.filter(results, scope)
            RogueDetector.analyze(inScope, whitelist, now)
        }
        val filtered = if (filter != null) alerts.filter { it.severity == filter } else alerts
        AlertsUiState(
            alerts = filtered.sortedBy { it.severity.ordinal },
            severityFilter = filter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    fun setFilter(severity: AlertSeverity?) {
        _filter.value = severity
    }

    fun authorize(alert: RogueAlert) {
        val ap = AuthorizedAccessPoint(
            bssid = alert.bssid,
            ssid = when (alert) {
                is RogueAlert.UnknownAccessPoint -> alert.ssid
                is RogueAlert.EvilTwin -> alert.impersonatedSsid
            },
            location = null,
            owner = null,
            deviceType = DeviceType.OTHER,
            expectedSecurity = when (alert) {
                is RogueAlert.UnknownAccessPoint -> alert.security
                is RogueAlert.EvilTwin -> SecurityType.UNKNOWN
            },
            notes = "Authorized from alert at ${Instant.now()}",
            authorizedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            whitelistRepo.upsert(ap)
        }
    }
}
