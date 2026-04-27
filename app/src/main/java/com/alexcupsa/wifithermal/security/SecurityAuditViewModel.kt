package com.alexcupsa.wifithermal.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.engine.security.SecurityAuditor
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityAuditResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SecurityAuditUiState(
    val results: List<ProcessedScanResult> = emptyList(),
    val audit: SecurityAuditResult? = null,
)

@HiltViewModel
class SecurityAuditViewModel @Inject constructor(
    scanState: WifiScanStateRepository,
) : ViewModel() {

    val uiState: StateFlow<SecurityAuditUiState> = scanState.scanResults
        .map { results ->
            SecurityAuditUiState(
                results = results,
                audit = if (results.isNotEmpty()) SecurityAuditor.audit(results) else null,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityAuditUiState())
}
