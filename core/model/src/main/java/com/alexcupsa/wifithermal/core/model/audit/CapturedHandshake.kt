package com.alexcupsa.wifithermal.core.model.audit

import kotlinx.serialization.Serializable

/**
 * One captured artefact from a Layer B sniffer companion. Stored locally
 * and (optionally) submitted to a cracking backend over Tailscale.
 *
 * `bssid` MUST be in [OffensiveScope.authorizedBssids] at capture time —
 * the [com.alexcupsa.wifithermal.core.engine.audit.OffensiveScopeGuard]
 * rejects insertion of out-of-scope captures, and the same guard runs again
 * before any submission to the cracking backend (defense in depth).
 *
 * `payload` is the raw artefact bytes, hex-encoded for serialization.
 * Format depends on [kind]:
 *   - PMKID:    16-byte PMKID + AP MAC + client MAC + ESSID
 *               (hashcat -m 22000 PMKID/EAPOL combined hash format)
 *   - EAPOL_4WAY: full 4-way handshake captured frames, hashcat 22000 format
 */
@Serializable
data class CapturedHandshake(
    val id: Long,
    val bssid: String,
    val ssid: String,
    val kind: CaptureKind,
    val payloadHex: String,
    val capturedAt: Long,
    val sourceDevice: String,
    val status: CrackStatus,
    val backendJobId: String? = null,
    val crackResult: String? = null,
)

@Serializable
enum class CaptureKind { PMKID, EAPOL_4WAY }

@Serializable
enum class CrackStatus {
    /** Just captured, not yet submitted to backend. */
    PENDING,

    /** Submitted to cracking backend, waiting on result. */
    SUBMITTED,

    /** Backend returned a successful crack. crackResult holds the password. */
    CRACKED,

    /** Backend exhausted wordlist without finding a match. */
    EXHAUSTED,

    /** Submission rejected by OffensiveScopeGuard at submit time. */
    REJECTED_OUT_OF_SCOPE,

    /** Backend reachable but returned an error. */
    FAILED,
}
