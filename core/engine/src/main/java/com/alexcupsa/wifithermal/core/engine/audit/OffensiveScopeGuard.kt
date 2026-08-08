package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import com.alexcupsa.wifithermal.core.model.audit.OffensiveScope

/**
 * Hard gate for Layer B operations. Stateless. Every entry point that
 * accepts a captured handshake / PMKID / dictionary-attack request must
 * route through here BEFORE persisting, transmitting, or processing the
 * artefact.
 *
 * Default-deny:
 *  - No [AuthorizationScope] loaded                    → REFUSED
 *  - Scope expired                                     → REFUSED
 *  - [OffensiveScope] block missing                    → REFUSED
 *  - [OffensiveScope.authorizedBssids] empty           → REFUSED
 *  - Target BSSID not in authorizedBssids (exact MAC)  → REFUSED
 *  - All gates pass                                    → ALLOWED
 *
 * Out-of-scope attempts are not silently dropped at this layer —
 * callers receive [Decision.Refused] with the specific reason so the
 * incident log can persist a SCOPE_VIOLATION_BLOCKED record. Audit trail
 * matters more than UX smoothness for offensive operations.
 *
 * BSSID matching is uppercase-normalised. No prefix matching, no wildcards.
 */
object OffensiveScopeGuard {

    sealed interface Decision {
        data object Allowed : Decision

        data class Refused(val reason: Reason, val message: String) : Decision

        enum class Reason {
            NO_SCOPE_LOADED,
            SCOPE_EXPIRED,
            OFFENSIVE_BLOCK_MISSING,
            EMPTY_AUTHORIZED_LIST,
            BSSID_NOT_AUTHORIZED,
        }
    }

    fun evaluate(
        scope: AuthorizationScope?,
        targetBssid: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Decision {
        if (scope == null) {
            return Decision.Refused(
                reason = Decision.Reason.NO_SCOPE_LOADED,
                message = "No authorization manifest loaded; Layer B is inert by default.",
            )
        }
        if (scope.isExpired(nowMs)) {
            return Decision.Refused(
                reason = Decision.Reason.SCOPE_EXPIRED,
                message = "Authorization manifest has expired; Layer B refused until reauthorized.",
            )
        }
        val offensive = scope.offensiveScope
            ?: return Decision.Refused(
                reason = Decision.Reason.OFFENSIVE_BLOCK_MISSING,
                message = "Authorization manifest does not include an offensive scope block.",
            )
        if (offensive.authorizedBssids.isEmpty()) {
            return Decision.Refused(
                reason = Decision.Reason.EMPTY_AUTHORIZED_LIST,
                message = "Offensive scope is declared but authorizedBssids is empty.",
            )
        }
        if (!offensive.isAuthorized(targetBssid)) {
            return Decision.Refused(
                reason = Decision.Reason.BSSID_NOT_AUTHORIZED,
                message = "Target $targetBssid is not in the authorized BSSID list.",
            )
        }
        return Decision.Allowed
    }

    /**
     * Convenience for callsites that only need the boolean.
     */
    fun isAllowed(scope: AuthorizationScope?, targetBssid: String, nowMs: Long = System.currentTimeMillis()): Boolean =
        evaluate(scope, targetBssid, nowMs) is Decision.Allowed
}
