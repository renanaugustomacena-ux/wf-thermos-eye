package com.alexcupsa.wifithermal.heatmap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.ApSummary
import com.alexcupsa.wifithermal.core.data.repository.MeasurementRepository
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.engine.heatmap.HeatmapEngine
import com.alexcupsa.wifithermal.core.model.ColorScheme
import com.alexcupsa.wifithermal.core.model.HeatmapConfig
import com.alexcupsa.wifithermal.core.model.HeatmapGrid
import com.alexcupsa.wifithermal.core.model.MeasurementSample
import com.alexcupsa.wifithermal.core.model.Survey
import com.alexcupsa.wifithermal.core.model.WifiBand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class HeatmapMode { BSSID, BAND_2_4, BAND_5 }

data class HeatmapUiState(
    val survey: Survey? = null,
    val grid: HeatmapGrid? = null,
    val config: HeatmapConfig = HeatmapConfig(),
    val mode: HeatmapMode = HeatmapMode.BAND_2_4,
    val selectedBssid: String? = null,
    val apSummaries: List<ApSummary> = emptyList(),
    val generating: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val surveyRepository: SurveyRepository,
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {

    private val surveyId: Long = savedStateHandle.get<Long>("surveyId") ?: 0L

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val survey = surveyRepository.getSurveyById(surveyId)
            val summaries = measurementRepository.getApSummary(surveyId)
            _uiState.update { it.copy(survey = survey, apSummaries = summaries) }
            generateHeatmap()
        }
    }

    fun setMode(mode: HeatmapMode) {
        _uiState.update { it.copy(mode = mode, selectedBssid = null) }
        viewModelScope.launch { generateHeatmap() }
    }

    fun selectBssid(bssid: String) {
        _uiState.update { it.copy(mode = HeatmapMode.BSSID, selectedBssid = bssid) }
        viewModelScope.launch { generateHeatmap() }
    }

    fun setColorScheme(scheme: ColorScheme) {
        _uiState.update { it.copy(config = it.config.copy(colorScheme = scheme)) }
    }

    private suspend fun generateHeatmap() {
        _uiState.update { it.copy(generating = true, errorMessage = null) }

        try {
            val samples = loadSamples()
            if (samples.size < MIN_SAMPLES) {
                _uiState.update {
                    it.copy(
                        grid = null,
                        generating = false,
                        errorMessage = "Need at least $MIN_SAMPLES measurement points (have ${samples.size})",
                    )
                }
                return
            }

            val grid = withContext(Dispatchers.Default) {
                HeatmapEngine.generate(
                    samples = samples,
                    width = GRID_SIZE,
                    height = GRID_SIZE,
                    config = _uiState.value.config,
                )
            }

            _uiState.update { it.copy(grid = grid, generating = false) }
        } catch (e: CancellationException) {
            // Structured concurrency: never swallow cancellation. The viewModelScope
            // was cancelled (ViewModel cleared) — propagate so child accounting stays
            // honest.
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(generating = false, errorMessage = e.message) }
        }
    }

    private suspend fun loadSamples(): List<MeasurementSample> = withContext(Dispatchers.IO) {
        when (val mode = _uiState.value.mode) {
            HeatmapMode.BSSID -> {
                val bssid = _uiState.value.selectedBssid
                    ?: _uiState.value.apSummaries.firstOrNull()?.bssid
                    ?: return@withContext emptyList()
                _uiState.update { it.copy(selectedBssid = bssid) }
                measurementRepository.getMeasurementSamples(surveyId, bssid)
            }
            HeatmapMode.BAND_2_4 -> measurementRepository.getBandMeasurementSamples(surveyId, WifiBand.BAND_2_4_GHZ)
            HeatmapMode.BAND_5 -> measurementRepository.getBandMeasurementSamples(surveyId, WifiBand.BAND_5_GHZ)
        }
    }

    companion object {
        private const val GRID_SIZE = 100
        private const val MIN_SAMPLES = 3
    }
}
