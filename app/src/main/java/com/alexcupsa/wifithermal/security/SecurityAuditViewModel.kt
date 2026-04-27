package com.alexcupsa.wifithermal.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.engine.audit.ScopeGuard
import com.alexcupsa.wifithermal.core.engine.security.SecurityAuditor
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityAuditResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SecurityAuditUiState(
    val results: List<ProcessedScanResult> = emptyList(),
    val audit: SecurityAuditResult? = null,
    val scopeActive: Boolean = false,
)

@HiltViewModel
class SecurityAuditViewModel @Inject constructor(
    scanState: WifiScanStateRepository,
    scopeRepo: AuthorizationManifestRepository,
) : ViewModel() {

    val uiState: StateFlow<SecurityAuditUiState> = combine(
        scanState.scanResults,
        scopeRepo.scope,
    ) { results, scope ->
        val now = System.currentTimeMillis()
        val active = scope != null && !scope.isExpired(now)
        val inputs = if (active) ScopeGuard.filter(results, scope!!) else emptyList()
        SecurityAuditUiState(
            results = inputs,
            audit = if (inputs.isNotEmpty()) SecurityAuditor.audit(inputs) else null,
            scopeActive = active,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecurityAuditUiState())
}
