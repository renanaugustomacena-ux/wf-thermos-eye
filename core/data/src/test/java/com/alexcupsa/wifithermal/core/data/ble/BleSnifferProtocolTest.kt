package com.alexcupsa.wifithermal.core.data.ble

import com.alexcupsa.wifithermal.core.model.audit.CaptureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleSnifferProtocolTest {

    @Test
    fun `parses well-formed PMKID frame`() {
        val frame = byteArrayOf(
            0x01, // version
            0x01, // kind = PMKID
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(), // bssid
            0x04, // ssid len
            'T'.code.toByte(), 'e'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(),
            0x03, 0x00, // payload len = 3
            0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(),
        )
        val parsed = BleSnifferProtocol.parseFrame(frame)
        assertEquals(CaptureKind.PMKID, parsed?.kind)
        assertEquals("AA:BB:CC:DD:EE:FF", parsed?.bssid)
        assertEquals("Test", parsed?.ssid)
        assertEquals("deadbe", parsed?.payloadHex)
    }

    @Test
    fun `parses EAPOL kind`() {
        val frame = byteArrayOf(
            0x01,
            0x02, // kind = EAPOL_4WAY
            0x11, 0x22, 0x33, 0x44, 0x55, 0x66,
            0x00, // ssid len = 0 (hidden)
            0x02, 0x00,
            0xCA.toByte(), 0xFE.toByte(),
        )
        val parsed = BleSnifferProtocol.parseFrame(frame)
        assertEquals(CaptureKind.EAPOL_4WAY, parsed?.kind)
        assertEquals("11:22:33:44:55:66", parsed?.bssid)
        assertEquals("", parsed?.ssid)
        assertEquals("cafe", parsed?.payloadHex)
    }

    @Test
    fun `wrong version returns null`() {
        val frame = byteArrayOf(0x02, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertNull(BleSnifferProtocol.parseFrame(frame))
    }

    @Test
    fun `unknown kind returns null`() {
        val frame = byteArrayOf(0x01, 0x09, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertNull(BleSnifferProtocol.parseFrame(frame))
    }

    @Test
    fun `truncated frame returns null`() {
        assertNull(BleSnifferProtocol.parseFrame(byteArrayOf(0x01, 0x01, 0xAA.toByte())))
    }

    @Test
    fun `payload length mismatch returns null`() {
        val frame = byteArrayOf(
            0x01, 0x01,
            0, 0, 0, 0, 0, 0,
            0x01,                                  // ssid len
            'A'.code.toByte(),
            0x05, 0x00,                            // payload len = 5
            0xCA.toByte(), 0xFE.toByte(),          // only 2 bytes provided
        )
        assertNull(BleSnifferProtocol.parseFrame(frame))
    }

    @Test
    fun `encodeAuthorizedBssidList joins with commas and uppercases`() {
        val bytes = BleSnifferProtocol.encodeAuthorizedBssidList(
            listOf("aa:bb:cc:dd:ee:ff", "11:22:33:44:55:66"),
        )
        assertEquals(
            "AA:BB:CC:DD:EE:FF,11:22:33:44:55:66",
            String(bytes, Charsets.US_ASCII),
        )
    }

    @Test
    fun `encodeAuthorizedBssidList empty returns empty bytes`() {
        val bytes = BleSnifferProtocol.encodeAuthorizedBssidList(emptyList())
        assertEquals(0, bytes.size)
    }
}
