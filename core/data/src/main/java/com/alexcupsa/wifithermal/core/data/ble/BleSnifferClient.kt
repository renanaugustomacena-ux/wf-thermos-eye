package com.alexcupsa.wifithermal.core.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.CapturedHandshakeRepository
import com.alexcupsa.wifithermal.core.data.repository.IncidentRepository
import com.alexcupsa.wifithermal.core.engine.audit.OffensiveScopeGuard
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import com.alexcupsa.wifithermal.core.model.audit.IncidentKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE central that connects to an ESP32-based 802.11 sniffer companion
 * (firmware design in /companion/README.md) and forwards parsed capture
 * frames to [CapturedHandshakeRepository], with strict
 * [OffensiveScopeGuard] enforcement at the gateway.
 *
 * Out-of-scope frames are dropped and counted as IncidentEvents with kind
 * [IncidentKind.SCOPE_VIOLATION_BLOCKED] (added in v0.8.0). The scope guard
 * is the SAME guard the repository uses — checking twice is intentional:
 * the BLE layer rejects fast (no DB write); the repo rejects again as
 * defense in depth.
 *
 * This client is inert until [start] is called from an Activity holding
 * [Manifest.permission.BLUETOOTH_SCAN] and [Manifest.permission.BLUETOOTH_CONNECT].
 * Currently no auto-discovery — the operator picks "Connect Sniffer" from
 * the Layer B UI which calls [start].
 */
@Singleton
class BleSnifferClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureRepo: CapturedHandshakeRepository,
    private val incidentRepo: IncidentRepository,
    private val scopeRepo: AuthorizationManifestRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _state = MutableStateFlow(BleState.IDLE)
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val _connectedDevice = MutableStateFlow<String?>(null)
    val connectedDevice: StateFlow<String?> = _connectedDevice.asStateFlow()

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()

    private var gatt: BluetoothGatt? = null

    enum class BleState {
        IDLE,
        UNSUPPORTED,
        PERMISSION_MISSING,
        ADAPTER_OFF,
        SCANNING,
        CONNECTING,
        CONNECTED,
        ERROR,
    }

    fun isPermissionGranted(): Boolean {
        val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        return scan == PackageManager.PERMISSION_GRANTED && connect == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (adapter == null) {
            _state.value = BleState.UNSUPPORTED
            return
        }
        if (!isPermissionGranted()) {
            _state.value = BleState.PERMISSION_MISSING
            return
        }
        if (!adapter.isEnabled) {
            _state.value = BleState.ADAPTER_OFF
            return
        }
        startScan()
    }

    fun stop() {
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        gatt?.close()
        gatt = null
        _connectedDevice.value = null
        _state.value = BleState.IDLE
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        _state.value = BleState.SCANNING
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleSnifferProtocol.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        adapter?.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val bondedAdapter = adapter ?: return
            // Connect to the first matching device. Operator should ensure
            // only one sniffer is in range during pairing.
            runCatching { bondedAdapter.bluetoothLeScanner?.stopScan(this) }
            connectTo(device)
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = BleState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectTo(device: BluetoothDevice) {
        _state.value = BleState.CONNECTING
        gatt = device.connectGatt(context, /* autoConnect */ false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state.value = BleState.CONNECTED
                    _connectedDevice.value = g.device.address
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _state.value = BleState.IDLE
                    _connectedDevice.value = null
                    runCatching { g.close() }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(BleSnifferProtocol.SERVICE_UUID) ?: run {
                _state.value = BleState.ERROR
                return
            }
            val capture = service.getCharacteristic(BleSnifferProtocol.CHAR_CAPTURE) ?: return
            g.setCharacteristicNotification(capture, true)
            val descriptor = capture.getDescriptor(BleSnifferProtocol.CCCD_DESCRIPTOR)
            descriptor?.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            descriptor?.let { g.writeDescriptor(it) }

            // Push current authorized BSSID list to sniffer so it filters at source.
            pushAuthorizedBssids(g, service)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid != BleSnifferProtocol.CHAR_CAPTURE) return
            handleCapturePayload(value)
        }
    }

    @SuppressLint("MissingPermission")
    private fun pushAuthorizedBssids(g: BluetoothGatt, service: android.bluetooth.BluetoothGattService) {
        val configChar = service.getCharacteristic(BleSnifferProtocol.CHAR_CONFIG) ?: return
        val list = scopeRepo.scope.value?.offensiveScope?.authorizedBssids.orEmpty()
        val bytes = BleSnifferProtocol.encodeAuthorizedBssidList(list)
        configChar.value = bytes
        g.writeCharacteristic(configChar)
    }

    private fun handleCapturePayload(bytes: ByteArray) {
        val frame = BleSnifferProtocol.parseFrame(bytes) ?: return
        scope.launch {
            val decision = OffensiveScopeGuard.evaluate(scopeRepo.scope.value, frame.bssid)
            when (decision) {
                is OffensiveScopeGuard.Decision.Allowed -> {
                    val id = captureRepo.record(
                        bssid = frame.bssid,
                        ssid = frame.ssid,
                        kind = frame.kind,
                        payloadHex = frame.payloadHex,
                        sourceDevice = _connectedDevice.value ?: "ble-sniffer",
                    )
                    if (id != null) {
                        _frameCount.value = _frameCount.value + 1
                    }
                }
                is OffensiveScopeGuard.Decision.Refused -> {
                    incidentRepo.record(
                        IncidentEvent(
                            id = 0,
                            timestamp = System.currentTimeMillis(),
                            kind = IncidentKind.SCOPE_VIOLATION_BLOCKED,
                            bssid = frame.bssid,
                            ssid = frame.ssid,
                            severity = AlertSeverity.HIGH,
                            summary = "Sniffer offered out-of-scope ${frame.kind.name} for ${frame.bssid}: ${decision.reason.name}",
                            evidenceJson = """{"reason":"${decision.reason.name}","kind":"${frame.kind.name}"}""",
                        ),
                    )
                }
            }
        }
    }
}
