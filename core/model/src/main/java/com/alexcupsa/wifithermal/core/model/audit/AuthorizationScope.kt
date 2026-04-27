package com.alexcupsa.wifithermal.core.model.audit

import kotlinx.serialization.Serializable

/**
 * Operator-supplied declaration of what the audit tool is allowed to
 * inspect. Loaded once at app start. Anything observed that does not match
 * this scope is silently dropped before reaching detectors, repositories
 * or logs.
 *
 * Two scope tiers, intentionally separated:
 *
 *  - The top-level Layer A scope (ssidPatterns / bssidPrefixes) governs
 *    PASSIVE observation: discovery, logging, mapping, anomaly detection.
 *    This is the scope ScopeGuard enforces.
 *
 *  - The optional [offensiveScope] block governs LAYER B operations
 *    (PMKID/handshake capture, cracking). Enforced by OffensiveScopeGuard.
 *    Empty list (or null block) = Layer B inert. Each entry is a *single*
 *    BSSID, not a prefix — offensive ops require exact match.
 *
 *  Layer B without an offensiveScope block, or with empty authorizedBssids,
 *  refuses every offensive operation. By design.
 */
@Serializable
data class AuthorizationScope(
    val organizationName: String,
    val authorizedBy: String,
    val authorizedAt: Long,
    val expiresAt: Long?,
    /** SSID exact match or prefix patterns ending in '*'. Empty = match nothing. */
    val ssidPatterns: List<String>,
    /** OUI prefixes (e.g. "AA:BB:CC") that are corporate hardware. Optional gate. */
    val bssidPrefixes: List<String>,
    val notes: String,
    /** Layer B authorization. Null = Layer B fully disabled. */
    val offensiveScope: OffensiveScope? = null,
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
        expiresAt != null && nowMs > expiresAt
}

/**
 * Authorization manifest for offensive (Layer B) operations.
 *
 * `authorizedBssids` is exact-match only — no prefix expansion, no
 * wildcards. Each MAC must be explicitly enumerated. This is the gate the
 * OffensiveScopeGuard enforces; with an empty list every Layer B operation
 * (handshake capture, PMKID extract, dictionary attack) is refused.
 *
 * The list is editable from the Authorization UI. The repo also ships a
 * top-level AUTHORIZATION.md as a human-readable mirror of the operator's
 * intent — that file is not consulted by the runtime, only by humans
 * reviewing the codebase.
 */
@Serializable
data class OffensiveScope(
    val authorizedBssids: List<String>,
    val authorizedAt: Long,
    val notes: String = "",
) {
    fun isAuthorized(bssid: String): Boolean {
        if (authorizedBssids.isEmpty()) return false
        val normalized = bssid.uppercase()
        return authorizedBssids.any { it.uppercase() == normalized }
    }
}
