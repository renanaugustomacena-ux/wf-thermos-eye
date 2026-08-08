package com.alexcupsa.wifithermal.layerb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.ble.BleSnifferClient
import com.alexcupsa.wifithermal.core.data.cracking.CrackingBackendClient
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.CapturedHandshakeRepository
import com.alexcupsa.wifithermal.core.engine.audit.OffensiveScopeGuard
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import com.alexcupsa.wifithermal.core.model.audit.CapturedHandshake
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LayerBUiState(
    val scope: AuthorizationScope? = null,
    val offensiveAuthorized: Boolean = false,
    val authorizedBssids: List<String> = emptyList(),
    val bleState: BleSnifferClient.BleState = BleSnifferClient.BleState.IDLE,
    val connectedDevice: String? = null,
    val frameCount: Int = 0,
    val captures: List<CapturedHandshake> = emptyList(),
    val backendUrl: String? = null,
    val message: String? = null,
)

@HiltViewModel
class LayerBViewModel @Inject constructor(
    private val captureRepo: CapturedHandshakeRepository,
    private val scopeRepo: AuthorizationManifestRepository,
    private val ble: BleSnifferClient,
    private val backend: CrackingBackendClient,
) : ViewModel() {

    val state: StateFlow<LayerBUiState> = combine(
        captureRepo.observeAll(),
        scopeRepo.scope,
        ble.state,
        ble.connectedDevice,
        ble.frameCount,
    ) { captures, scope, bleState, device, frames ->
        val offensive = scope?.offensiveScope
        LayerBUiState(
            scope = scope,
            offensiveAuthorized = offensive?.authorizedBssids?.isNotEmpty() == true,
            authorizedBssids = offensive?.authorizedBssids.orEmpty(),
            bleState = bleState,
            connectedDevice = device,
            frameCount = frames,
            captures = captures,
            backendUrl = backend.backendUrl(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LayerBUiState())

    fun startSniffer() {
        if (!ble.isPermissionGranted()) return
        ble.start()
    }

    fun stopSniffer() {
        ble.stop()
    }

    fun submitForCracking(capture: CapturedHandshake) {
        viewModelScope.launch {
            val outcome = backend.submit(capture)
            // outcome is reflected by capture row status update in repo
            @Suppress("UNUSED_VARIABLE") val _consumed = outcome
        }
    }

    fun pollCrackingResult(capture: CapturedHandshake) {
        viewModelScope.launch {
            backend.pollJob(capture)
        }
    }

    fun deleteCapture(capture: CapturedHandshake) {
        viewModelScope.launch { captureRepo.delete(capture.id) }
    }

    fun setBackendUrl(url: String) {
        backend.setBackendUrl(url)
    }

    fun setAuthToken(token: String) {
        backend.setAuthToken(token)
    }

    fun isPermissionGranted(): Boolean = ble.isPermissionGranted()

    fun guardDecisionFor(bssid: String): OffensiveScopeGuard.Decision =
        OffensiveScopeGuard.evaluate(scopeRepo.scope.value, bssid)
}
