package com.alexcupsa.wifithermal.core.engine.audit

/**
 * Pure-function redactor for fields that may end up in reports shared
 * outside the security team. Operator picks a level per-export; same input
 * is rewritten consistently across CSV and PDF.
 *
 * - NONE      = full data, internal use only
 * - PARTIAL   = BSSID masked except last 4 hex digits; SSID kept (HR can
 *               still recognise the network name)
 * - FULL      = BSSID replaced with stable pseudonym ("AP-001"); SSID
 *               replaced with deterministic short hash. Use when sharing
 *               with parties that should not learn corporate network names.
 */
object ReportRedactor {

    enum class Level { NONE, PARTIAL, FULL }

    fun maskBssid(bssid: String, level: Level, pseudonymIndex: Int = 0): String = when (level) {
        Level.NONE -> bssid
        Level.PARTIAL -> {
            val parts = bssid.split(":")
            if (parts.size == 6) {
                "**:**:**:**:${parts[4]}:${parts[5]}".uppercase()
            } else {
                "**redacted**"
            }
        }
        Level.FULL -> "AP-${"%03d".format(pseudonymIndex.coerceAtLeast(0))}"
    }

    fun maskSsid(ssid: String, level: Level): String = when (level) {
        Level.NONE -> ssid
        Level.PARTIAL -> ssid
        Level.FULL -> if (ssid.isEmpty()) "<hidden>" else stableHash(ssid)
    }

    /**
     * Lightweight 32-bit FNV-1a hash, hex-encoded. Not cryptographic — its
     * job is to give a stable, short, non-recoverable identifier in human-
     * readable form. Two SSIDs with the same name always hash the same way
     * across reports so cross-referencing remains possible without leaking
     * the original string.
     */
    fun stableHash(value: String): String {
        var hash = 0x811c9dc5L
        for (b in value.encodeToByteArray()) {
            hash = hash xor (b.toLong() and 0xff)
            hash = (hash * 0x01000193L) and 0xffffffffL
        }
        return "ssid-${"%08x".format(hash)}"
    }
}
