package com.alexcupsa.wifithermal.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.ScanStatus
import com.alexcupsa.wifithermal.service.WifiScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SortMode { SIGNAL, NAME, CHANNEL, BAND, SECURITY }

data class ScanUiState(
    val results: List<ProcessedScanResult> = emptyList(),
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val sortMode: SortMode = SortMode.SIGNAL,
    val filterBand: String? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val application: Application,
    scanState: WifiScanStateRepository,
) : AndroidViewModel(application) {

    private val _sortMode = MutableStateFlow(SortMode.SIGNAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _filterBand = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ScanUiState> = combine(
        scanState.scanResults,
        scanState.scanStatus,
        _sortMode,
        _filterBand,
    ) { results, status, sort, band ->
        val filtered = if (band != null) {
            results.filter { it.band.name == band }
        } else {
            results
        }

        val sorted = when (sort) {
            SortMode.SIGNAL -> filtered.sortedByDescending { it.smoothedRssi }
            SortMode.NAME -> filtered.sortedBy { it.ssid.lowercase() }
            SortMode.CHANNEL -> filtered.sortedBy { it.channel }
            SortMode.BAND -> filtered.sortedBy { it.band.ordinal }
            SortMode.SECURITY -> filtered.sortedBy { it.security.ordinal }
        }

        ScanUiState(
            results = sorted,
            scanStatus = status,
            sortMode = sort,
            filterBand = band,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanUiState())

    fun startScanning() {
        application.startForegroundService(WifiScanService.startIntent(application))
    }

    fun stopScanning() {
        // Foreground-service routing: the service promotes itself in
        // onStartCommand before dispatching the action, so even the stop
        // signal must come through startForegroundService to satisfy the
        // Android 14 5-second startForeground deadline.
        application.startForegroundService(WifiScanService.stopIntent(application))
    }

    fun singleScan() {
        application.startForegroundService(WifiScanService.singleScanIntent(application))
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setFilterBand(band: String?) {
        _filterBand.value = band
    }
}
