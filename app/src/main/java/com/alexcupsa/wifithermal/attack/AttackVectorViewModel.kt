package com.alexcupsa.wifithermal.attack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.common.root.RootResult
import com.alexcupsa.wifithermal.core.common.root.RootShell
import com.alexcupsa.wifithermal.core.data.root.SavedPasswordExtractor
import com.alexcupsa.wifithermal.core.data.root.SavedWifiCredential
import com.alexcupsa.wifithermal.core.engine.keygen.KeygenResult
import com.alexcupsa.wifithermal.core.engine.keygen.VendorKeygen
import com.alexcupsa.wifithermal.core.engine.router.AdminBruteForce
import com.alexcupsa.wifithermal.core.engine.router.BruteForceProgress
import com.alexcupsa.wifithermal.core.engine.router.BruteForceResult
import com.alexcupsa.wifithermal.core.engine.router.RouterDetector
import com.alexcupsa.wifithermal.core.engine.router.RouterInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttackVectorUiState(
    val isRooted: Boolean? = null,
    val rootCheckInProgress: Boolean = false,
    val savedPasswords: List<SavedWifiCredential> = emptyList(),
    val savedPasswordsLoading: Boolean = false,
    val savedPasswordsError: String? = null,
    val keygenResult: KeygenResult? = null,
    val keygenTargetBssid: String = "",
    val keygenTargetSsid: String = "",
    val routerInfo: RouterInfo? = null,
    val routerDetecting: Boolean = false,
    val bruteForceProgress: BruteForceProgress? = null,
    val bruteForceResult: BruteForceResult? = null,
    val bruteForceRunning: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class AttackVectorViewModel @Inject constructor(
    private val rootShell: RootShell,
    private val savedPasswordExtractor: SavedPasswordExtractor,
) : ViewModel() {

    private val _state = MutableStateFlow(AttackVectorUiState())
    val state: StateFlow<AttackVectorUiState> = _state.asStateFlow()

    init {
        checkRoot()
    }

    fun checkRoot() {
        viewModelScope.launch {
            _state.update { it.copy(rootCheckInProgress = true) }
            val rooted = rootShell.isRooted()
            _state.update { it.copy(isRooted = rooted, rootCheckInProgress = false) }
        }
    }

    fun extractSavedPasswords() {
        viewModelScope.launch {
            _state.update { it.copy(savedPasswordsLoading = true, savedPasswordsError = null) }
            when (val result = savedPasswordExtractor.extract()) {
                is RootResult.Success -> {
                    _state.update {
                        it.copy(
                            savedPasswords = result.data,
                            savedPasswordsLoading = false,
                            statusMessage = "Extracted ${result.data.size} credentials",
                        )
                    }
                }
                is RootResult.Error -> {
                    _state.update {
                        it.copy(
                            savedPasswordsError = result.message,
                            savedPasswordsLoading = false,
                        )
                    }
                }
                RootResult.NoRoot -> {
                    _state.update {
                        it.copy(
                            savedPasswordsError = "Root access not available",
                            savedPasswordsLoading = false,
                        )
                    }
                }
                RootResult.Timeout -> {
                    _state.update {
                        it.copy(
                            savedPasswordsError = "Operation timed out",
                            savedPasswordsLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun updateKeygenTarget(ssid: String, bssid: String) {
        _state.update {
            it.copy(
                keygenTargetSsid = ssid,
                keygenTargetBssid = bssid,
                keygenResult = null,
            )
        }
    }

    fun runKeygen() {
        val current = _state.value
        if (current.keygenTargetBssid.isBlank()) return

        val result = VendorKeygen.generate(current.keygenTargetSsid, current.keygenTargetBssid)
        _state.update {
            it.copy(
                keygenResult = result,
                statusMessage = result?.let { r ->
                    "Generated ${r.candidates.size} candidates for ${r.vendor}"
                } ?: "Vendor not recognized for BSSID ${current.keygenTargetBssid}",
            )
        }
    }

    fun detectRouter() {
        viewModelScope.launch {
            _state.update { it.copy(routerDetecting = true, routerInfo = null) }
            val gateway = RouterDetector.detectGateway()
            if (gateway != null) {
                val info = RouterDetector.scanRouterPorts(gateway)
                _state.update {
                    it.copy(
                        routerInfo = info,
                        routerDetecting = false,
                        statusMessage = "Router detected at $gateway",
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        routerDetecting = false,
                        statusMessage = "No router gateway detected",
                    )
                }
            }
        }
    }

    fun startBruteForce() {
        val routerInfo = _state.value.routerInfo ?: return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    bruteForceRunning = true,
                    bruteForceResult = null,
                    bruteForceProgress = null,
                )
            }
            val result = AdminBruteForce.bruteForce(
                routerInfo = routerInfo,
                onProgress = { progress ->
                    _state.update { it.copy(bruteForceProgress = progress) }
                },
            )
            _state.update {
                it.copy(
                    bruteForceResult = result,
                    bruteForceRunning = false,
                    statusMessage = when (result) {
                        is BruteForceResult.Success -> "SUCCESS: ${result.username}:${result.password} (${result.method})"
                        is BruteForceResult.Exhausted -> "All credentials exhausted"
                        is BruteForceResult.RateLimited -> "Rate limited by router"
                        is BruteForceResult.Error -> "Error: ${result.message}"
                    },
                )
            }
        }
    }

    fun clearStatus() {
        _state.update { it.copy(statusMessage = null) }
    }
}
