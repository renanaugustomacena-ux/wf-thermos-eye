package com.alexcupsa.wifithermal.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.LocationStateRepository
import com.alexcupsa.wifithermal.core.data.repository.RssiSampleRepository
import com.alexcupsa.wifithermal.core.engine.audit.Triangulator
import com.alexcupsa.wifithermal.core.model.audit.RssiSample
import com.alexcupsa.wifithermal.core.model.audit.TriangulationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TriangulationUiState(
    val bssid: String = "",
    val sampleCount: Int = 0,
    val samples: List<RssiSample> = emptyList(),
    val result: TriangulationResult? = null,
    val deviceLat: Double? = null,
    val deviceLon: Double? = null,
    val distanceFromDeviceMetres: Double? = null,
    val message: String? = null,
)

@HiltViewModel
class TriangulationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sampleRepo: RssiSampleRepository,
    locationRepo: LocationStateRepository,
) : ViewModel() {

    private val bssid: String = savedStateHandle.get<String>("bssid").orEmpty()

    private val _samples = MutableStateFlow<List<RssiSample>>(emptyList())
    private val _refresh = MutableStateFlow(0L)

    val state: StateFlow<TriangulationUiState> = combine(
        _samples,
        _refresh,
        locationRepo.location,
    ) { samples, _, deviceLoc ->
        val result = if (samples.size >= Triangulator.MIN_SAMPLES) Triangulator.triangulate(samples) else null
        val distance = if (result != null && deviceLoc != null) {
            Triangulator.distanceMetres(deviceLoc.lat, deviceLoc.lon, result.estimatedLat, result.estimatedLon)
        } else null
        TriangulationUiState(
            bssid = bssid,
            sampleCount = samples.size,
            samples = samples,
            result = result,
            deviceLat = deviceLoc?.lat,
            deviceLon = deviceLoc?.lon,
            distanceFromDeviceMetres = distance,
            message = when {
                bssid.isEmpty() -> "Missing BSSID"
                samples.isEmpty() -> "No samples yet — walk around with the app open while the rogue is in range."
                samples.size < Triangulator.MIN_SAMPLES ->
                    "Need ${Triangulator.MIN_SAMPLES} samples (have ${samples.size}). Move to a different position."
                else -> null
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TriangulationUiState(bssid = bssid))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _samples.value = if (bssid.isEmpty()) emptyList() else sampleRepo.samplesFor(bssid, limit = 200)
            _refresh.value = System.currentTimeMillis()
        }
    }
}
