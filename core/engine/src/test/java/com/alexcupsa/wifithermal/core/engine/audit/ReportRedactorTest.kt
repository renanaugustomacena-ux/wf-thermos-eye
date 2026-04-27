package com.alexcupsa.wifithermal.core.engine.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRedactorTest {

    @Test
    fun `NONE keeps bssid intact`() {
        assertEquals(
            "AA:BB:CC:DD:EE:FF",
            ReportRedactor.maskBssid("AA:BB:CC:DD:EE:FF", ReportRedactor.Level.NONE),
        )
    }

    @Test
    fun `PARTIAL keeps last 4 hex digits and uppercases`() {
        assertEquals(
            "**:**:**:**:EE:FF",
            ReportRedactor.maskBssid("aa:bb:cc:dd:ee:ff", ReportRedactor.Level.PARTIAL),
        )
    }

    @Test
    fun `PARTIAL on malformed BSSID returns redacted placeholder`() {
        assertEquals(
            "**redacted**",
            ReportRedactor.maskBssid("not a mac", ReportRedactor.Level.PARTIAL),
        )
    }

    @Test
    fun `FULL replaces with sequential pseudonym`() {
        assertEquals("AP-000", ReportRedactor.maskBssid("anything", ReportRedactor.Level.FULL, 0))
        assertEquals("AP-007", ReportRedactor.maskBssid("anything", ReportRedactor.Level.FULL, 7))
        assertEquals("AP-042", ReportRedactor.maskBssid("anything", ReportRedactor.Level.FULL, 42))
    }

    @Test
    fun `NONE keeps ssid intact`() {
        assertEquals(
            "CorpInternal",
            ReportRedactor.maskSsid("CorpInternal", ReportRedactor.Level.NONE),
        )
    }

    @Test
    fun `PARTIAL keeps ssid for IT review`() {
        assertEquals(
            "CorpInternal",
            ReportRedactor.maskSsid("CorpInternal", ReportRedactor.Level.PARTIAL),
        )
    }

    @Test
    fun `FULL hashes ssid to stable opaque tag`() {
        val a = ReportRedactor.maskSsid("CorpInternal", ReportRedactor.Level.FULL)
        val b = ReportRedactor.maskSsid("CorpInternal", ReportRedactor.Level.FULL)
        val c = ReportRedactor.maskSsid("Other", ReportRedactor.Level.FULL)
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertTrue(a.startsWith("ssid-"))
        assertEquals(13, a.length) // "ssid-" + 8 hex
    }

    @Test
    fun `FULL on empty ssid returns hidden marker`() {
        assertEquals("<hidden>", ReportRedactor.maskSsid("", ReportRedactor.Level.FULL))
    }

    @Test
    fun `stableHash produces 8 hex chars`() {
        val hash = ReportRedactor.stableHash("test")
        assertTrue("expected ssid- prefix, got $hash", hash.startsWith("ssid-"))
        assertEquals(13, hash.length)
    }
}
