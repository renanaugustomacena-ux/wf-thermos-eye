package com.alexcupsa.wifithermal.core.engine.signal

import kotlin.math.abs

/**
 * Sliding-window outlier detector based on the Median Absolute Deviation (MAD).
 *
 * The modified Z-score is computed as:
 *     Zm = 0.6745 * (x - median) / MAD
 *
 * Values with |Zm| > [threshold] are classified as outliers and are NOT added
 * to the window, preventing contamination of the reference distribution.
 *
 * The constant 0.6745 is the 0.75 quantile of the standard normal, making the
 * MAD a consistent estimator of the standard deviation for normal data.
 *
 * Requires at least 3 samples before it begins rejecting.
 */
class OutlierRejector(
    private val windowSize: Int = 10,
    private val threshold: Double = 3.0,
) {
    private val window = ArrayDeque<Double>(windowSize)

    /**
     * Returns `true` if [value] is an outlier relative to the current window.
     *
     * Non-outlier values are automatically appended to the window (evicting
     * the oldest entry when the window is full). Outlier values are discarded.
     */
    fun isOutlier(value: Double): Boolean {
        if (window.size < 3) {
            window.addLast(value)
            return false
        }

        val sorted = window.sorted()
        val median = sorted[sorted.size / 2]
        val mad = sorted.map { abs(it - median) }.sorted()[sorted.size / 2]

        val modifiedZScore = if (mad > 0) 0.6745 * (value - median) / mad else 0.0
        val outlier = abs(modifiedZScore) > threshold

        if (!outlier) {
            if (window.size >= windowSize) window.removeFirst()
            window.addLast(value)
        }

        return outlier
    }

    /** Clear the sliding window. */
    fun reset() {
        window.clear()
    }
}
