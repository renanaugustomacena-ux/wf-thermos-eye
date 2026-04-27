package com.alexcupsa.wifithermal.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.engine.audit.ScopeGuard
import com.alexcupsa.wifithermal.core.engine.wifi.ChannelAnalyzer
import com.alexcupsa.wifithermal.core.model.ChannelAnalysisResult
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChannelAnalysisUiState(
    val results: List<ProcessedScanResult> = emptyList(),
    val analysis: ChannelAnalysisResult? = null,
    val scopeActive: Boolean = false,
)

@HiltViewModel
class ChannelAnalysisViewModel @Inject constructor(
    scanState: WifiScanStateRepository,
    scopeRepo: AuthorizationManifestRepository,
) : ViewModel() {

    val uiState: StateFlow<ChannelAnalysisUiState> = combine(
        scanState.scanResults,
        scopeRepo.scope,
    ) { results, scope ->
        val now = System.currentTimeMillis()
        val active = scope != null && !scope.isExpired(now)
        val inputs = if (active) ScopeGuard.filter(results, scope!!) else emptyList()
        ChannelAnalysisUiState(
            results = inputs,
            analysis = if (inputs.isNotEmpty()) ChannelAnalyzer.analyze(inputs) else null,
            scopeActive = active,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChannelAnalysisUiState())
}
