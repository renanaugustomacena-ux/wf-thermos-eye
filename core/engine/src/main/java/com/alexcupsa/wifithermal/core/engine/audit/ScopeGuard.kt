package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope

/**
 * Hard filter applied before any audit logic touches a scan result. Drops
 * BSSIDs that are out of the operator-declared scope so accidentally
 * captured neighbor networks never enter detectors, repositories, or logs.
 *
 * Two gates apply, OR-combined:
 * - SSID match: pattern can be exact ("CorpInternal") or prefix ("Corp*")
 * - BSSID OUI prefix match: e.g. "AA:BB:CC" matches all Cisco hardware
 *
 * If both lists are empty, NOTHING is in scope. This is deliberate — an
 * empty scope must produce zero alerts, never wildcard.
 */
object ScopeGuard {

    fun inScope(result: ProcessedScanResult, scope: AuthorizationScope): Boolean {
        if (scope.ssidPatterns.isEmpty() && scope.bssidPrefixes.isEmpty()) return false
        return matchesSsidPattern(result.ssid, scope.ssidPatterns) ||
            matchesBssidPrefix(result.bssid, scope.bssidPrefixes)
    }

    fun filter(
        results: List<ProcessedScanResult>,
        scope: AuthorizationScope,
    ): List<ProcessedScanResult> = results.filter { inScope(it, scope) }

    private fun matchesSsidPattern(ssid: String, patterns: List<String>): Boolean {
        if (ssid.isEmpty()) return false
        return patterns.any { pattern ->
            when {
                pattern.endsWith("*") -> ssid.startsWith(pattern.dropLast(1))
                else -> ssid == pattern
            }
        }
    }

    private fun matchesBssidPrefix(bssid: String, prefixes: List<String>): Boolean {
        val normalized = bssid.uppercase()
        return prefixes.any { prefix -> normalized.startsWith(prefix.uppercase()) }
    }
}
