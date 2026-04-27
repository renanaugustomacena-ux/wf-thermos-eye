package com.alexcupsa.wifithermal.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.ScanStatus
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.service.WifiScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val totalAps: Int = 0,
    val aps24: Int = 0,
    val aps5: Int = 0,
    val surveyCount: Int = 0,
    val connectedAp: ProcessedScanResult? = null,
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val signalDistribution: Map<SignalQuality, Int> = emptyMap(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    surveyRepository: SurveyRepository,
    scanState: WifiScanStateRepository,
) : AndroidViewModel(application) {

    val uiState: StateFlow<DashboardUiState> = combine(
        scanState.scanResults,
        scanState.scanStatus,
        surveyRepository.getAllSurveys(),
    ) { results, status, surveys ->
        val distribution = results.groupBy { it.signalQuality }.mapValues { it.value.size }
        DashboardUiState(
            totalAps = results.size,
            aps24 = results.count { it.band.name == "BAND_2_4_GHZ" },
            aps5 = results.count { it.band.name == "BAND_5_GHZ" },
            surveyCount = surveys.size,
            connectedAp = results.firstOrNull { it.isConnected },
            scanStatus = status,
            signalDistribution = distribution,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun triggerScan() {
        application.startForegroundService(WifiScanService.singleScanIntent(application))
    }
}
