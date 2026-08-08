package com.alexcupsa.wifithermal.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ApDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    scanState: WifiScanStateRepository,
) : ViewModel() {

    private val bssid: String = savedStateHandle.get<String>("bssid").orEmpty()

    val ap: StateFlow<ProcessedScanResult?> = scanState.scanResults
        .map { results -> results.firstOrNull { it.bssid == bssid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
