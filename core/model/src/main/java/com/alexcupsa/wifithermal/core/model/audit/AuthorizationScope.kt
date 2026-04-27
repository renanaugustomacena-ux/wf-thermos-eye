package com.alexcupsa.wifithermal.core.model.audit

import kotlinx.serialization.Serializable

/**
 * Operator-supplied declaration of what the audit tool is allowed to
 * inspect. Loaded once at app start. Anything observed that does not match
 * this scope is silently dropped before reaching detectors, repositories
 * or logs.
 *
 * The model is intentionally narrow — passive observation of authorized
 * corporate Wi-Fi only. Out-of-scope BSSIDs are not stored and not
 * reported, so accidental capture of neighbor networks does not become a
 * legal liability.
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
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
        expiresAt != null && nowMs > expiresAt
}
