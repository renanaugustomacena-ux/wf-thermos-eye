package com.alexcupsa.wifithermal.core.data.ble

import com.alexcupsa.wifithermal.core.model.audit.CaptureKind
import java.util.UUID

/**
 * Wire protocol shared between the Android client and the ESP32 sniffer
 * companion. Documented in /companion/README.md for the firmware author
 * to implement on the ESP32 side.
 *
 * Service UUID (random project-specific):
 *     7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01
 *
 * Characteristics:
 *     CAPTURE      7f9c8002-...  notify   sniffer -> phone capture frames
 *     CONFIG       7f9c8003-...  write    phone -> sniffer  authorized BSSID
 *                                          list, comma-separated MAC strings
 *     STATUS       7f9c8004-...  notify   sniffer -> phone health/heartbeat
 *
 * Capture frame binary format (little-endian):
 *
 *     [u8   version    ]  = 0x01
 *     [u8   kind       ]  = 0x01 PMKID, 0x02 EAPOL_4WAY
 *     [u8x6 bssid      ]  raw MAC, NOT colon-separated
 *     [u8   ssid_len   ]
 *     [u8xN ssid       ]  UTF-8, ssid_len bytes
 *     [u16  payload_len]  little-endian
 *     [u8xN payload    ]  hashcat 22000 hash bytes
 *
 * The phone client validates the BSSID via OffensiveScopeGuard BEFORE
 * persisting the frame. Out-of-scope frames are dropped and counted as
 * SCOPE_VIOLATION_BLOCKED incidents.
 */
object BleSnifferProtocol {

    val SERVICE_UUID: UUID = UUID.fromString("7f9c8001-1c4e-4d0e-9c0d-7e3a6f2b9d01")
    val CHAR_CAPTURE: UUID = UUID.fromString("7f9c8002-1c4e-4d0e-9c0d-7e3a6f2b9d01")
    val CHAR_CONFIG: UUID = UUID.fromString("7f9c8003-1c4e-4d0e-9c0d-7e3a6f2b9d01")
    val CHAR_STATUS: UUID = UUID.fromString("7f9c8004-1c4e-4d0e-9c0d-7e3a6f2b9d01")

    val CCCD_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val PROTOCOL_VERSION: Byte = 0x01
    private const val KIND_PMKID: Byte = 0x01
    private const val KIND_EAPOL: Byte = 0x02

    data class CaptureFrame(
        val kind: CaptureKind,
        val bssid: String,
        val ssid: String,
        val payloadHex: String,
    )

    /**
     * Parse a binary capture frame from the BLE characteristic value.
     * Returns null on any malformed input — never throws to caller.
     */
    fun parseFrame(bytes: ByteArray): CaptureFrame? {
        if (bytes.size < 12) return null
        if (bytes[0] != PROTOCOL_VERSION) return null
        val kind = when (bytes[1]) {
            KIND_PMKID -> CaptureKind.PMKID
            KIND_EAPOL -> CaptureKind.EAPOL_4WAY
            else -> return null
        }
        val bssid = formatMac(bytes, offset = 2)
        val ssidLen = bytes[8].toInt() and 0xff
        if (bytes.size < 9 + ssidLen + 2) return null
        val ssid = bytes.copyOfRange(9, 9 + ssidLen).toString(Charsets.UTF_8)
        val payloadLenLo = bytes[9 + ssidLen].toInt() and 0xff
        val payloadLenHi = bytes[10 + ssidLen].toInt() and 0xff
        val payloadLen = payloadLenLo or (payloadLenHi shl 8)
        val payloadStart = 11 + ssidLen
        if (bytes.size < payloadStart + payloadLen) return null
        val payload = bytes.copyOfRange(payloadStart, payloadStart + payloadLen)
        return CaptureFrame(
            kind = kind,
            bssid = bssid,
            ssid = ssid,
            payloadHex = payload.toHexLower(),
        )
    }

    /**
     * Encode the operator's authorized-BSSID list for transmission to the
     * sniffer via the CONFIG characteristic. Format: comma-separated upper-
     * case MAC strings, ASCII, no trailing comma.
     */
    fun encodeAuthorizedBssidList(bssids: List<String>): ByteArray =
        bssids.joinToString(",") { it.uppercase() }.toByteArray(Charsets.US_ASCII)

    private fun formatMac(bytes: ByteArray, offset: Int): String =
        (0..5).joinToString(":") { i -> "%02X".format(bytes[offset + i].toInt() and 0xff) }

    private fun ByteArray.toHexLower(): String =
        joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
