package com.alexcupsa.wifithermal.attack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.common.root.RootResult
import com.alexcupsa.wifithermal.core.common.root.RootShell
import com.alexcupsa.wifithermal.core.data.termux.TermuxBridge
import com.alexcupsa.wifithermal.core.data.termux.TermuxTool
import com.alexcupsa.wifithermal.core.engine.monitor.MonitorMode
import com.alexcupsa.wifithermal.core.engine.monitor.MonitorModeResult
import com.alexcupsa.wifithermal.core.engine.monitor.WifiInterface
import com.alexcupsa.wifithermal.core.engine.wps.WpsAttackEvent
import com.alexcupsa.wifithermal.core.engine.wps.WpsPinAttack
import com.alexcupsa.wifithermal.core.engine.wps.WpsTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WpsAttackUiState(
    val isRooted: Boolean? = null,
    val termuxInstalled: Boolean = false,
    val termuxTools: List<TermuxTool> = emptyList(),
    val wifiInterfaces: List<WifiInterface> = emptyList(),
    val selectedInterface: String? = null,
    val monitorInterface: String? = null,
    val monitorModeEnabled: Boolean = false,
    val targetBssid: String = "",
    val targetSsid: String = "",
    val targetChannel: Int = 1,
    val attackRunning: Boolean = false,
    val attackProgress: String = "",
    val attackResult: WpsAttackResult? = null,
    val captureRunning: Boolean = false,
    val captureProgress: String = "",
    val capturedPassword: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface WpsAttackResult {
    data class Success(val pin: String, val password: String) : WpsAttackResult
    data class Locked(val seconds: Int) : WpsAttackResult
    data object RateLimited : WpsAttackResult
    data class Failed(val reason: String) : WpsAttackResult
}

@HiltViewModel
class WpsAttackViewModel @Inject constructor(
    private val rootShell: RootShell,
    private val termuxBridge: TermuxBridge,
    private val wpsPinAttack: WpsPinAttack,
    private val monitorMode: MonitorMode,
) : ViewModel() {

    private val _state = MutableStateFlow(WpsAttackUiState())
    val state: StateFlow<WpsAttackUiState> = _state.asStateFlow()

    private var attackJob: Job? = null
    private var captureJob: Job? = null

    init {
        checkEnvironment()
    }

    fun checkEnvironment() {
        viewModelScope.launch {
            val rooted = rootShell.isRooted()
            val termuxInstalled = termuxBridge.isTermuxInstalled()
            val tools = if (termuxInstalled) termuxBridge.checkInstalledTools() else emptyList()

            val interfaces = if (rooted) {
                when (val result = monitorMode.listInterfaces()) {
                    is RootResult.Success -> result.data
                    else -> emptyList()
                }
            } else {
                emptyList()
            }

            _state.update {
                it.copy(
                    isRooted = rooted,
                    termuxInstalled = termuxInstalled,
                    termuxTools = tools,
                    wifiInterfaces = interfaces,
                    selectedInterface = interfaces.firstOrNull()?.name,
                )
            }
        }
    }

    fun selectInterface(name: String) {
        _state.update { it.copy(selectedInterface = name) }
    }

    fun updateTarget(ssid: String, bssid: String, channel: Int) {
        _state.update {
            it.copy(
                targetSsid = ssid,
                targetBssid = bssid,
                targetChannel = channel,
            )
        }
    }

    fun enableMonitorMode() {
        val interface_ = _state.value.selectedInterface ?: return
        viewModelScope.launch {
            _state.update { it.copy(statusMessage = "Enabling monitor mode...") }

            when (val result = monitorMode.enableMonitorMode(interface_)) {
                is MonitorModeResult.Success -> {
                    _state.update {
                        it.copy(
                            monitorInterface = result.monitorInterface,
                            monitorModeEnabled = true,
                            statusMessage = "Monitor mode enabled on ${result.monitorInterface}",
                        )
                    }
                }
                is MonitorModeResult.Failed -> {
                    _state.update {
                        it.copy(
                            errorMessage = result.reason,
                            monitorModeEnabled = false,
                        )
                    }
                }
            }
        }
    }

    fun disableMonitorMode() {
        val monInterface = _state.value.monitorInterface ?: return
        viewModelScope.launch {
            monitorMode.disableMonitorMode(monInterface)
            _state.update {
                it.copy(
                    monitorInterface = null,
                    monitorModeEnabled = false,
                    statusMessage = "Monitor mode disabled",
                )
            }
        }
    }

    fun startPixieDustAttack() {
        val monInterface = _state.value.monitorInterface ?: return
        val current = _state.value
        if (current.targetBssid.isBlank()) return

        attackJob?.cancel()
        attackJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    attackRunning = true,
                    attackResult = null,
                    attackProgress = "Starting Pixie Dust attack...",
                )
            }

            val target = WpsTarget(
                bssid = current.targetBssid,
                ssid = current.targetSsid,
                channel = current.targetChannel,
                wpsVersion = null,
                wpsLocked = false,
            )

            wpsPinAttack.attackWithReaver(
                interface_ = monInterface,
                target = target,
                pixieDust = true,
            ).collect { event ->
                when (event) {
                    is WpsAttackEvent.Progress -> {
                        _state.update { it.copy(attackProgress = event.status) }
                    }
                    is WpsAttackEvent.Success -> {
                        _state.update {
                            it.copy(
                                attackResult = WpsAttackResult.Success(event.pin, event.password),
                                capturedPassword = event.password,
                            )
                        }
                    }
                    is WpsAttackEvent.Locked -> {
                        _state.update {
                            it.copy(attackResult = WpsAttackResult.Locked(event.lockoutSeconds))
                        }
                    }
                    WpsAttackEvent.RateLimited -> {
                        _state.update { it.copy(attackResult = WpsAttackResult.RateLimited) }
                    }
                    is WpsAttackEvent.Failed -> {
                        _state.update { it.copy(attackResult = WpsAttackResult.Failed(event.reason)) }
                    }
                    WpsAttackEvent.Completed -> {
                        _state.update { it.copy(attackRunning = false) }
                    }
                }
            }
        }
    }

    fun startBruteForceAttack() {
        val monInterface = _state.value.monitorInterface ?: return
        val current = _state.value
        if (current.targetBssid.isBlank()) return

        attackJob?.cancel()
        attackJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    attackRunning = true,
                    attackResult = null,
                    attackProgress = "Starting PIN brute force...",
                )
            }

            val target = WpsTarget(
                bssid = current.targetBssid,
                ssid = current.targetSsid,
                channel = current.targetChannel,
                wpsVersion = null,
                wpsLocked = false,
            )

            wpsPinAttack.bruteForceCommonPins(
                interface_ = monInterface,
                target = target,
            ).collect { event ->
                when (event) {
                    is WpsAttackEvent.Progress -> {
                        _state.update {
                            it.copy(
                                attackProgress = "PIN ${event.pin}: ${event.attempt}/${event.total}",
                            )
                        }
                    }
                    is WpsAttackEvent.Success -> {
                        _state.update {
                            it.copy(
                                attackResult = WpsAttackResult.Success(event.pin, event.password),
                                capturedPassword = event.password,
                            )
                        }
                    }
                    is WpsAttackEvent.Locked -> {
                        _state.update {
                            it.copy(
                                attackProgress = "AP locked for ${event.lockoutSeconds}s, waiting...",
                            )
                        }
                    }
                    WpsAttackEvent.RateLimited -> {
                        _state.update {
                            it.copy(attackProgress = "Rate limited, waiting...")
                        }
                    }
                    is WpsAttackEvent.Failed -> {
                        _state.update { it.copy(attackResult = WpsAttackResult.Failed(event.reason)) }
                    }
                    WpsAttackEvent.Completed -> {
                        _state.update { it.copy(attackRunning = false) }
                    }
                }
            }
        }
    }

    fun startPmkidCapture() {
        val monInterface = _state.value.monitorInterface ?: return
        val current = _state.value

        captureJob?.cancel()
        captureJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    captureRunning = true,
                    captureProgress = "Starting PMKID capture...",
                )
            }

            val outputPath = "/data/local/tmp/pmkid_${System.currentTimeMillis()}.pcapng"

            monitorMode.capturePmkid(
                interface_ = monInterface,
                targetBssid = current.targetBssid.takeIf { it.isNotBlank() },
                outputPath = outputPath,
                durationSeconds = 60,
            ).collect { event ->
                when (event) {
                    is com.alexcupsa.wifithermal.core.engine.monitor.CaptureEvent.Status -> {
                        _state.update { it.copy(captureProgress = event.message) }
                    }
                    is com.alexcupsa.wifithermal.core.engine.monitor.CaptureEvent.Error -> {
                        _state.update {
                            it.copy(
                                captureRunning = false,
                                errorMessage = event.message,
                            )
                        }
                    }
                    else -> {}
                }
            }

            _state.update { it.copy(captureRunning = false) }
        }
    }

    fun startHandshakeCapture() {
        val monInterface = _state.value.monitorInterface ?: return
        val current = _state.value
        if (current.targetBssid.isBlank()) return

        captureJob?.cancel()
        captureJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    captureRunning = true,
                    captureProgress = "Starting handshake capture with deauth...",
                )
            }

            val outputPath = "/data/local/tmp/handshake_${System.currentTimeMillis()}.cap"

            monitorMode.captureHandshake(
                interface_ = monInterface,
                targetBssid = current.targetBssid,
                targetChannel = current.targetChannel,
                clientMac = null,
                outputPath = outputPath,
                deauthCount = 5,
                durationSeconds = 120,
            ).collect { event ->
                when (event) {
                    is com.alexcupsa.wifithermal.core.engine.monitor.CaptureEvent.Status -> {
                        _state.update { it.copy(captureProgress = event.message) }
                    }
                    is com.alexcupsa.wifithermal.core.engine.monitor.CaptureEvent.Error -> {
                        _state.update {
                            it.copy(
                                captureRunning = false,
                                errorMessage = event.message,
                            )
                        }
                    }
                    else -> {}
                }
            }

            _state.update { it.copy(captureRunning = false) }
        }
    }

    fun cancelAttack() {
        attackJob?.cancel()
        captureJob?.cancel()
        _state.update {
            it.copy(
                attackRunning = false,
                captureRunning = false,
                statusMessage = "Attack cancelled",
            )
        }
    }

    fun openTermux() {
        termuxBridge.launchTermux()
    }

    fun installTools() {
        termuxBridge.runInTermux(termuxBridge.buildInstallCommand())
    }

    fun clearMessages() {
        _state.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        attackJob?.cancel()
        captureJob?.cancel()
    }
}
