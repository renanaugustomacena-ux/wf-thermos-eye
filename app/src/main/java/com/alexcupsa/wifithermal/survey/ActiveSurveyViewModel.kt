package com.alexcupsa.wifithermal.survey

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.FloorPlanRepository
import com.alexcupsa.wifithermal.core.data.repository.MeasurementRepository
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.model.ApMeasurement
import com.alexcupsa.wifithermal.core.model.FloorPlan
import com.alexcupsa.wifithermal.core.model.MeasurementPoint
import com.alexcupsa.wifithermal.core.model.Position
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.Survey
import com.alexcupsa.wifithermal.service.WifiScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveSurveyUiState(
    val survey: Survey? = null,
    val floorPlan: FloorPlan? = null,
    val measurements: List<MeasurementPoint> = emptyList(),
    val currentScanResults: List<ProcessedScanResult> = emptyList(),
    val isRecording: Boolean = false,
    val tapPosition: Position? = null,
)

@HiltViewModel
class ActiveSurveyViewModel @Inject constructor(
    private val application: Application,
    savedStateHandle: SavedStateHandle,
    private val surveyRepository: SurveyRepository,
    private val measurementRepository: MeasurementRepository,
    private val floorPlanRepository: FloorPlanRepository,
    private val scanState: WifiScanStateRepository,
) : AndroidViewModel(application) {

    private val surveyId: Long = savedStateHandle.get<Long>("surveyId") ?: 0L

    private val _survey = MutableStateFlow<Survey?>(null)
    private val _floorPlan = MutableStateFlow<FloorPlan?>(null)
    private val _isRecording = MutableStateFlow(false)
    private val _tapPosition = MutableStateFlow<Position?>(null)

    private val _completionEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val completionEvents: SharedFlow<Unit> = _completionEvents.asSharedFlow()

    val uiState: StateFlow<ActiveSurveyUiState> = combine(
        _survey,
        _floorPlan,
        measurementRepository.getMeasurements(surveyId),
        scanState.scanResults,
        combine(_isRecording, _tapPosition) { rec, tap -> rec to tap },
    ) { survey, floorPlan, measurements, scanResults, (recording, tapPos) ->
        ActiveSurveyUiState(
            survey = survey,
            floorPlan = floorPlan,
            measurements = measurements,
            currentScanResults = scanResults,
            isRecording = recording,
            tapPosition = tapPos,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveSurveyUiState())

    init {
        viewModelScope.launch {
            val survey = surveyRepository.getSurveyById(surveyId)
            _survey.value = survey
            survey?.floorPlanId?.let { fpId ->
                _floorPlan.value = floorPlanRepository.getFloorPlan(fpId)
            }
        }
    }

    fun startRecording() {
        _isRecording.value = true
        application.startForegroundService(WifiScanService.startIntent(application))
    }

    fun stopRecording() {
        _isRecording.value = false
        application.startForegroundService(WifiScanService.stopIntent(application))
    }

    fun onFloorPlanTap(x: Double, y: Double) {
        _tapPosition.value = Position(x, y)
    }

    fun recordMeasurement() {
        val pos = _tapPosition.value ?: return
        val scanResults = scanState.scanResults.value
        if (scanResults.isEmpty()) return

        viewModelScope.launch {
            val point = MeasurementPoint(
                position = pos,
                timestamp = System.currentTimeMillis(),
                apMeasurements = scanResults.map { sr ->
                    ApMeasurement(
                        bssid = sr.bssid,
                        ssid = sr.ssid,
                        rssi = sr.rssi,
                        smoothedRssi = sr.smoothedRssi,
                        frequency = sr.frequency,
                        channel = sr.channel,
                        channelWidth = sr.channelWidth,
                        band = sr.band,
                        security = sr.security,
                        standard = sr.standard,
                    )
                },
            )
            measurementRepository.addMeasurement(surveyId, point)
            _tapPosition.value = null
        }
    }

    fun completeSurvey() {
        viewModelScope.launch {
            stopRecording()
            surveyRepository.completeSurvey(surveyId)
            _completionEvents.tryEmit(Unit)
        }
    }
}
