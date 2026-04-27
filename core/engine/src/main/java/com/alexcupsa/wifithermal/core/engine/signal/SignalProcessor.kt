package com.alexcupsa.wifithermal.core.engine.signal

import java.util.concurrent.ConcurrentHashMap

/**
 * Per-BSSID RSSI signal processor combining outlier rejection and Kalman filtering.
 *
 * Thread-safe: each BSSID gets its own independent [OutlierRejector] and
 * [RssiKalmanFilter] stored in [ConcurrentHashMap]s, so concurrent scans from
 * different threads will not corrupt each other's state.
 *
 * Usage:
 * ```
 *   val processor = SignalProcessor()
 *   val smoothed = processor.smooth("AA:BB:CC:DD:EE:FF", -67)
 * ```
 */
class SignalProcessor {
    private val filters = ConcurrentHashMap<String, RssiKalmanFilter>()
    private val rejectors = ConcurrentHashMap<String, OutlierRejector>()

    /**
     * Process a raw RSSI reading for the given [bssid].
     *
     * 1. The value is checked against the outlier detector for this BSSID.
     * 2. If it is an outlier, the last Kalman estimate is returned (or [rawRssi]
     *    if no estimate exists yet).
     * 3. Otherwise the Kalman filter is updated and the new estimate is returned.
     */
    fun smooth(bssid: String, rawRssi: Int): Double {
        val rejector = rejectors.getOrPut(bssid) { OutlierRejector() }
        if (rejector.isOutlier(rawRssi.toDouble())) {
            return filters[bssid]?.currentEstimate() ?: rawRssi.toDouble()
        }
        val filter = filters.getOrPut(bssid) { RssiKalmanFilter() }
        return filter.update(rawRssi.toDouble())
    }

    /** Remove filter and rejection state for a single BSSID. */
    fun reset(bssid: String) {
        filters.remove(bssid)
        rejectors.remove(bssid)
    }

    /** Remove all per-BSSID state. */
    fun resetAll() {
        filters.clear()
        rejectors.clear()
    }
}
