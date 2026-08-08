package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import com.alexcupsa.wifithermal.core.model.audit.OffensiveScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffensiveScopeGuardTest {

    @Test
    fun `null scope returns NO_SCOPE_LOADED`() {
        val decision = OffensiveScopeGuard.evaluate(scope = null, targetBssid = "AA:BB:CC:DD:EE:FF")
        assertTrue(decision is OffensiveScopeGuard.Decision.Refused)
        assertEquals(
            OffensiveScopeGuard.Decision.Reason.NO_SCOPE_LOADED,
            (decision as OffensiveScopeGuard.Decision.Refused).reason,
        )
    }

    @Test
    fun `expired scope returns SCOPE_EXPIRED`() {
        val scope = makeScope(
            offensive = OffensiveScope(authorizedBssids = listOf("AA:BB:CC:DD:EE:FF"), authorizedAt = 0),
            expiresAt = 100_000L,
        )
        val decision = OffensiveScopeGuard.evaluate(scope, "AA:BB:CC:DD:EE:FF", nowMs = 200_000L)
        assertTrue(decision is OffensiveScopeGuard.Decision.Refused)
        assertEquals(
            OffensiveScopeGuard.Decision.Reason.SCOPE_EXPIRED,
            (decision as OffensiveScopeGuard.Decision.Refused).reason,
        )
    }

    @Test
    fun `null offensive block returns OFFENSIVE_BLOCK_MISSING`() {
        val scope = makeScope(offensive = null)
        val decision = OffensiveScopeGuard.evaluate(scope, "AA:BB:CC:DD:EE:FF")
        assertTrue(decision is OffensiveScopeGuard.Decision.Refused)
        assertEquals(
            OffensiveScopeGuard.Decision.Reason.OFFENSIVE_BLOCK_MISSING,
            (decision as OffensiveScopeGuard.Decision.Refused).reason,
        )
    }

    @Test
    fun `empty authorizedBssids returns EMPTY_AUTHORIZED_LIST`() {
        val scope = makeScope(
            offensive = OffensiveScope(authorizedBssids = emptyList(), authorizedAt = 0),
        )
        val decision = OffensiveScopeGuard.evaluate(scope, "AA:BB:CC:DD:EE:FF")
        assertTrue(decision is OffensiveScopeGuard.Decision.Refused)
        assertEquals(
            OffensiveScopeGuard.Decision.Reason.EMPTY_AUTHORIZED_LIST,
            (decision as OffensiveScopeGuard.Decision.Refused).reason,
        )
    }

    @Test
    fun `unauthorized bssid returns BSSID_NOT_AUTHORIZED`() {
        val scope = makeScope(
            offensive = OffensiveScope(authorizedBssids = listOf("AA:BB:CC:DD:EE:FF"), authorizedAt = 0),
        )
        val decision = OffensiveScopeGuard.evaluate(scope, "11:22:33:44:55:66")
        assertTrue(decision is OffensiveScopeGuard.Decision.Refused)
        assertEquals(
            OffensiveScopeGuard.Decision.Reason.BSSID_NOT_AUTHORIZED,
            (decision as OffensiveScopeGuard.Decision.Refused).reason,
        )
    }

    @Test
    fun `authorized bssid returns Allowed`() {
        val scope = makeScope(
            offensive = OffensiveScope(authorizedBssids = listOf("AA:BB:CC:DD:EE:FF"), authorizedAt = 0),
        )
        val decision = OffensiveScopeGuard.evaluate(scope, "AA:BB:CC:DD:EE:FF")
        assertTrue(decision is OffensiveScopeGuard.Decision.Allowed)
    }

    @Test
    fun `bssid match is case insensitive`() {
        val scope = makeScope(
            offensive = OffensiveScope(authorizedBssids = listOf("AA:BB:CC:DD:EE:FF"), authorizedAt = 0),
        )
        val decision = OffensiveScopeGuard.evaluate(scope, "aa:bb:cc:dd:ee:ff")
        assertTrue(decision is OffensiveScopeGuard.Decision.Allowed)
    }

    @Test
    fun `prefix match is rejected (offensive requires exact bssid)`() {
        val scope = makeScope(
            offensive = OffensiveScope(authorizedBssids = listOf("AA:BB:CC"), authorizedAt = 0),
        )
        val decision = OffensiveScopeGuard.evaluate(scope, "AA:BB:CC:DD:EE:FF")
        assertTrue("prefix must NOT count as authorization", decision is OffensiveScopeGuard.Decision.Refused)
    }

    @Test
    fun `multi-bssid list authorizes each entry independently`() {
        val scope = makeScope(
            offensive = OffensiveScope(
                authorizedBssids = listOf("AA:BB:CC:11:22:33", "DD:EE:FF:44:55:66"),
                authorizedAt = 0,
            ),
        )
        assertTrue(OffensiveScopeGuard.isAllowed(scope, "AA:BB:CC:11:22:33"))
        assertTrue(OffensiveScopeGuard.isAllowed(scope, "DD:EE:FF:44:55:66"))
        assertFalse(OffensiveScopeGuard.isAllowed(scope, "11:22:33:44:55:66"))
    }

    @Test
    fun `OffensiveScope isAuthorized empty returns false even on identity`() {
        val s = OffensiveScope(authorizedBssids = emptyList(), authorizedAt = 0)
        assertFalse(s.isAuthorized(""))
        assertFalse(s.isAuthorized("AA:BB:CC:DD:EE:FF"))
    }

    private fun makeScope(
        offensive: OffensiveScope?,
        expiresAt: Long? = null,
    ) = AuthorizationScope(
        organizationName = "Test",
        authorizedBy = "Test",
        authorizedAt = 0,
        expiresAt = expiresAt,
        ssidPatterns = listOf("Test*"),
        bssidPrefixes = emptyList(),
        notes = "",
        offensiveScope = offensive,
    )
}
