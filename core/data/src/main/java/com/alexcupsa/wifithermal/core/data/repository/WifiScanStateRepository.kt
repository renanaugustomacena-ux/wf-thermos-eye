package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.ScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the live WiFi scan state shared between the scan service and any UI
 * consumer. Replaces the previous companion-object state on WifiScanService,
 * which leaked across service restarts and made the scan loop untestable.
 */
@Singleton
class WifiScanStateRepository @Inject constructor() {

    private val _scanResults = MutableStateFlow<List<ProcessedScanResult>>(emptyList())
    val scanResults: StateFlow<List<ProcessedScanResult>> = _scanResults.asStateFlow()

    private val _scanStatus = MutableStateFlow(ScanStatus.IDLE)
    val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

    fun updateResults(results: List<ProcessedScanResult>) {
        _scanResults.value = results
    }

    fun updateStatus(status: ScanStatus) {
        _scanStatus.value = status
    }
}
