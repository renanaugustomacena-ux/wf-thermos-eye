package com.alexcupsa.wifithermal.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.engine.wifi.ChannelAnalyzer
import com.alexcupsa.wifithermal.core.model.ChannelAnalysisResult
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChannelAnalysisUiState(
    val results: List<ProcessedScanResult> = emptyList(),
    val analysis: ChannelAnalysisResult? = null,
)

@HiltViewModel
class ChannelAnalysisViewModel @Inject constructor(
    scanState: WifiScanStateRepository,
) : ViewModel() {

    val uiState: StateFlow<ChannelAnalysisUiState> = scanState.scanResults
        .map { results ->
            ChannelAnalysisUiState(
                results = results,
                analysis = if (results.isNotEmpty()) ChannelAnalyzer.analyze(results) else null,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelAnalysisUiState())
}
