package com.alexcupsa.wifithermal.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.IncidentRepository
import com.alexcupsa.wifithermal.core.data.repository.WhitelistRepository
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.engine.audit.RogueDetector
import com.alexcupsa.wifithermal.core.engine.audit.ScopeGuard
import com.alexcupsa.wifithermal.core.model.ScanStatus
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val scope: AuthorizationScope? = null,
    val scopeExpired: Boolean = false,
    val totalApsObserved: Int = 0,
    val inScopeCount: Int = 0,
    val authorizedCount: Int = 0,
    val alertsBySeverity: Map<AlertSeverity, Int> = emptyMap(),
    val criticalAlerts: List<RogueAlert> = emptyList(),
    val recentIncidents: List<IncidentEvent> = emptyList(),
    val scanStatus: ScanStatus = ScanStatus.IDLE,
)

@HiltViewModel
class AuditDashboardViewModel @Inject constructor(
    scanState: WifiScanStateRepository,
    whitelistRepo: WhitelistRepository,
    incidentRepo: IncidentRepository,
    scopeRepo: AuthorizationManifestRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        scanState.scanResults,
        scanState.scanStatus,
        whitelistRepo.observeAll(),
        scopeRepo.scope,
        incidentRepo.recent(10),
    ) { results, status, whitelist, scope, incidents ->
        val now = System.currentTimeMillis()
        val expired = scope?.isExpired(now) ?: false
        val alerts = if (scope != null && !expired) {
            val inScope = ScopeGuard.filter(results, scope)
            RogueDetector.analyze(inScope, whitelist, now)
        } else {
            emptyList()
        }
        val inScopeCount = if (scope != null && !expired) ScopeGuard.filter(results, scope).size else 0

        DashboardUiState(
            scope = scope,
            scopeExpired = expired,
            totalApsObserved = results.size,
            inScopeCount = inScopeCount,
            authorizedCount = whitelist.size,
            alertsBySeverity = alerts.groupBy { it.severity }.mapValues { it.value.size },
            criticalAlerts = alerts.filter { it.severity == AlertSeverity.CRITICAL }.take(5),
            recentIncidents = incidents,
            scanStatus = status,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
