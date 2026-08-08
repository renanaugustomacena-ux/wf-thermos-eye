package com.alexcupsa.wifithermal.core.engine.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * Comprehensive unit tests for the signal processing engine.
 *
 * All classes under test are pure computation with no Android or I/O
 * dependencies, so these run as standard JVM unit tests.
 */
class SignalProcessingTest {

    // ---------------------------------------------------------------
    // RssiKalmanFilter
    // ---------------------------------------------------------------

    @Test
    fun kalman_firstMeasurementReturnedAsIs() {
        val filter = RssiKalmanFilter(processNoise = 0.008, measurementNoise = 4.0)
        val result = filter.update(-60.0)
        assertEquals(-60.0, result, 0.1)
    }

    @Test
    fun kalman_currentEstimateMatchesLastUpdate() {
        val filter = RssiKalmanFilter()
        filter.update(-55.0)
        assertEquals(-55.0, filter.currentEstimate(), 0.1)
        filter.update(-57.0)
        assertEquals(filter.update(-56.0), filter.currentEstimate(), 0.0)
    }

    @Test
    fun kalman_currentEstimateBeforeInitializationIsZero() {
        val filter = RssiKalmanFilter()
        assertEquals(0.0, filter.currentEstimate(), 0.0)
    }

    @Test
    fun kalman_convergenceOnRepeatedValues() {
        val filter = RssiKalmanFilter(processNoise = 0.008, measurementNoise = 4.0)
        var estimate = 0.0
        for (i in 1..50) {
            estimate = filter.update(-70.0)
        }
        // After 50 identical measurements the estimate must have converged
        // very close to the measurement value.
        assertEquals(-70.0, estimate, 0.1)
    }

    @Test
    fun kalman_spikeRejection() {
        val filter = RssiKalmanFilter(processNoise = 0.008, measurementNoise = 4.0)
        // Feed 20 steady readings at -60 dBm to build a strong prior.
        for (i in 1..20) {
            filter.update(-60.0)
        }
        // Inject a single spike to -20 dBm.
        val afterSpike = filter.update(-20.0)
        // The Kalman filter should heavily attenuate the spike.
        // With low process noise and high measurement noise the gain is small,
        // so the estimate should stay much closer to -60 than to -20.
        assertTrue(
            "Estimate after spike should remain below -50 dBm but was $afterSpike",
            afterSpike < -50.0,
        )
    }

    @Test
    fun kalman_resetClearsState() {
        val filter = RssiKalmanFilter()
        filter.update(-70.0)
        filter.update(-70.0)
        filter.reset()
        // After reset, the very next update must seed again.
        assertEquals(0.0, filter.currentEstimate(), 0.0)
        val result = filter.update(-40.0)
        assertEquals(-40.0, result, 0.1)
    }

    @Test
    fun kalman_tracksDriftCorrectly() {
        // The filter should slowly track a drifting signal rather than
        // snapping instantly.
        val filter = RssiKalmanFilter(processNoise = 0.008, measurementNoise = 4.0)
        for (i in 1..20) filter.update(-60.0)
        // Now signal drifts to -65 over many samples.
        var estimate = 0.0
        for (i in 1..50) estimate = filter.update(-65.0)
        // After 50 steps it should be very close to -65.
        assertEquals(-65.0, estimate, 0.5)
    }

    // ---------------------------------------------------------------
    // OutlierRejector
    // ---------------------------------------------------------------

    @Test
    fun outlierRejector_firstThreeValuesNeverRejected() {
        val rejector = OutlierRejector(windowSize = 10, threshold = 3.0)
        assertFalse(rejector.isOutlier(100.0))
        assertFalse(rejector.isOutlier(-999.0))
        assertFalse(rejector.isOutlier(42.0))
    }

    @Test
    fun outlierRejector_steadyStreamThenExtremeIsRejected() {
        val rejector = OutlierRejector(windowSize = 10, threshold = 3.0)
        // Build a stable window around -60 dBm.
        for (i in 1..8) {
            assertFalse(rejector.isOutlier(-60.0 + (i % 2)))
        }
        // An extreme spike should be flagged as an outlier.
        assertTrue(rejector.isOutlier(-10.0))
    }

    @Test
    fun outlierRejector_valuesCloseToMedianNotRejected() {
        val rejector = OutlierRejector(windowSize = 10, threshold = 3.0)
        // Seed with values spread around -60.
        val seed = listOf(-62.0, -59.0, -61.0, -60.0, -58.0)
        seed.forEach { rejector.isOutlier(it) }
        // A value within normal RSSI jitter should not be rejected.
        assertFalse(rejector.isOutlier(-57.0))
        assertFalse(rejector.isOutlier(-63.0))
    }

    @Test
    fun outlierRejector_windowEvictsOldest() {
        val rejector = OutlierRejector(windowSize = 5, threshold = 3.0)
        // Fill with values that have some variance so MAD > 0.
        rejector.isOutlier(-60.0)
        rejector.isOutlier(-58.0)
        rejector.isOutlier(-62.0)
        rejector.isOutlier(-59.0)
        rejector.isOutlier(-61.0)
        // Window: [-60,-58,-62,-59,-61], median=-60, MAD=1
        // Values within ~3*MAD/0.6745 ≈ 4.45 of median are accepted.
        // Feed -56 (4 units away, z=0.6745*4/1=2.7 < 3.0) — accepted.
        assertFalse(rejector.isOutlier(-56.0))
        // Now window is [-58,-62,-59,-61,-56] (oldest -60 evicted).
        // This proves the window evicts the oldest entry when full.
        assertFalse(rejector.isOutlier(-57.0))
    }

    @Test
    fun outlierRejector_resetClearsWindow() {
        val rejector = OutlierRejector(windowSize = 10, threshold = 3.0)
        for (i in 1..5) rejector.isOutlier(-60.0)
        rejector.reset()
        // After reset, we are back to warm-up: first three values never rejected.
        assertFalse(rejector.isOutlier(999.0))
        assertFalse(rejector.isOutlier(-999.0))
        assertFalse(rejector.isOutlier(0.0))
    }

    @Test
    fun outlierRejector_identicalValuesNeverRejectSameValue() {
        // When all values are identical, MAD = 0 so modifiedZScore = 0.
        // Since 0 < threshold, the same value should never be rejected.
        val rejector = OutlierRejector(windowSize = 10, threshold = 3.0)
        for (i in 1..10) {
            assertFalse(rejector.isOutlier(-55.0))
        }
    }

    @Test
    fun outlierRejector_outlierDoesNotContaminateWindow() {
        val rejector = OutlierRejector(windowSize = 10, threshold = 3.0)
        // Build a window with variance so MAD > 0.
        rejector.isOutlier(-60.0)
        rejector.isOutlier(-58.0)
        rejector.isOutlier(-62.0)
        rejector.isOutlier(-59.0)
        rejector.isOutlier(-61.0)
        rejector.isOutlier(-60.0)
        // Window has median ≈ -60, MAD ≈ 1.0
        // -10 is 50 units from median: z = 0.6745 * 50 / 1.0 = 33.7 >> 3.0
        assertTrue(rejector.isOutlier(-10.0))
        // Rejected values don't enter the window, so another extreme is also rejected.
        assertTrue(rejector.isOutlier(-10.0))
        // Normal values near -60 are still accepted.
        assertFalse(rejector.isOutlier(-59.0))
    }

    // ---------------------------------------------------------------
    // SignalProcessor
    // ---------------------------------------------------------------

    @Test
    fun signalProcessor_differentBssidsGetIndependentFilters() {
        val processor = SignalProcessor()
        // Feed very different RSSI values to two BSSIDs.
        val a = processor.smooth("AA:AA:AA:AA:AA:AA", -40)
        val b = processor.smooth("BB:BB:BB:BB:BB:BB", -80)
        // Each BSSID's first reading should seed its own filter, so the
        // estimates must match their respective raw values.
        assertEquals(-40.0, a, 0.1)
        assertEquals(-80.0, b, 0.1)
    }

    @Test
    fun signalProcessor_smoothReturnsReasonableValues() {
        val processor = SignalProcessor()
        // Feed consistent RSSI values and confirm the smoothed output
        // stays within a reasonable range.
        for (i in 1..20) {
            val smoothed = processor.smooth("CC:CC:CC:CC:CC:CC", -65)
            assertTrue(
                "Smoothed value $smoothed out of reasonable range",
                smoothed in -70.0..-60.0,
            )
        }
    }

    @Test
    fun signalProcessor_outlierRejectedReturnsPreviousEstimate() {
        val processor = SignalProcessor()
        // Feed slightly varied values to build a non-zero MAD in the rejector.
        val inputs = intArrayOf(-60, -58, -62, -59, -61, -60, -58, -62, -59, -61, -60, -58, -62, -59, -61)
        for (v in inputs) {
            processor.smooth("DD:DD:DD:DD:DD:DD", v)
        }
        val beforeSpike = processor.smooth("DD:DD:DD:DD:DD:DD", -60)
        // Inject a huge spike — should be rejected by outlier rejector.
        val afterSpike = processor.smooth("DD:DD:DD:DD:DD:DD", -10)
        assertEquals(beforeSpike, afterSpike, 0.5)
    }

    @Test
    fun signalProcessor_resetAllClearsAllState() {
        val processor = SignalProcessor()
        processor.smooth("EE:EE:EE:EE:EE:EE", -50)
        processor.smooth("FF:FF:FF:FF:FF:FF", -70)
        processor.resetAll()
        // After resetAll, the first call for any BSSID should seed fresh.
        val result = processor.smooth("EE:EE:EE:EE:EE:EE", -90)
        assertEquals(-90.0, result, 0.1)
    }

    @Test
    fun signalProcessor_resetSingleBssid() {
        val processor = SignalProcessor()
        processor.smooth("AA:AA:AA:AA:AA:AA", -50)
        processor.smooth("BB:BB:BB:BB:BB:BB", -70)
        processor.reset("AA:AA:AA:AA:AA:AA")
        // AA should be fresh-seeded; BB should retain its state.
        val freshA = processor.smooth("AA:AA:AA:AA:AA:AA", -90)
        assertEquals(-90.0, freshA, 0.1)
        // BB's second reading should reflect Kalman filtering, not a fresh seed.
        val continuedB = processor.smooth("BB:BB:BB:BB:BB:BB", -70)
        assertEquals(-70.0, continuedB, 0.5)
    }

    // ---------------------------------------------------------------
    // PathLossModel
    // ---------------------------------------------------------------

    @Test
    fun pathLoss_atReferenceRssiDistanceIsApproxOneMetre() {
        val freq = 2437 // channel 6, 2.4 GHz band
        val refRssi = PathLossModel.referenceRssi(freq)
        val distance = PathLossModel.estimateDistance(refRssi, freq, pathLossExponent = 3.0)
        assertEquals(1.0, distance, 0.1)
    }

    @Test
    fun pathLoss_strongerSignalGivesShorterDistance() {
        val freq = 2437
        val distNear = PathLossModel.estimateDistance(-30.0, freq)
        val distFar = PathLossModel.estimateDistance(-70.0, freq)
        assertTrue(
            "Stronger signal (-30) should yield shorter distance than weaker (-70)",
            distNear < distFar,
        )
    }

    @Test
    fun pathLoss_distanceClampedToMinimum() {
        val freq = 2437
        // An absurdly strong RSSI that would compute to sub-0.1 m distance.
        val distance = PathLossModel.estimateDistance(0.0, freq, pathLossExponent = 2.0)
        assertTrue(
            "Distance should be clamped to >= 0.1 but was $distance",
            distance >= 0.1,
        )
    }

    @Test
    fun pathLoss_distanceClampedToMaximum() {
        val freq = 2437
        // An absurdly weak RSSI that would compute to enormous distance.
        val distance = PathLossModel.estimateDistance(-120.0, freq, pathLossExponent = 2.0)
        assertTrue(
            "Distance should be clamped to <= 200 but was $distance",
            distance <= 200.0,
        )
    }

    @Test
    fun pathLoss_predictRssiIsInverseOfEstimateDistance() {
        val freq = 5180 // 5 GHz band
        val n = 3.0
        val originalRssi = -55.0
        val distance = PathLossModel.estimateDistance(originalRssi, freq, n)
        val reconstructed = PathLossModel.predictRssi(distance, freq, n)
        // If the distance was not clamped, the round-trip must be identity.
        assertEquals(originalRssi, reconstructed, 0.1)
    }

    @Test
    fun pathLoss_predictRssiFloorDistanceAtPointOne() {
        val freq = 2437
        // Distance of 0.0 should be treated as 0.1 (floor).
        val rssiAtZero = PathLossModel.predictRssi(0.0, freq)
        val rssiAtFloor = PathLossModel.predictRssi(0.1, freq)
        assertEquals(rssiAtZero, rssiAtFloor, 0.0)
    }

    @Test
    fun pathLoss_calibrateWithKnownData() {
        val freq = 2437
        val refRssi = PathLossModel.referenceRssi(freq)
        // Synthesize measurements consistent with n=3.0:
        //   rssi = refRssi - 10 * 3.0 * log10(d)
        val knownExponent = 3.0
        val measurements = listOf(2.0, 5.0, 10.0, 20.0).map { d ->
            d to (refRssi - 10 * knownExponent * log10(d))
        }
        val calibrated = PathLossModel.calibrateExponent(measurements, freq)
        assertEquals(knownExponent, calibrated, 0.1)
    }

    @Test
    fun pathLoss_calibrateWithDegenerateDataReturnsFallback() {
        val freq = 2437
        // All measurements at d <= 0.1 are skipped, leaving denominator ~ 0.
        val measurements = listOf(
            0.05 to -40.0,
            0.08 to -42.0,
            0.1 to -41.0,
        )
        val calibrated = PathLossModel.calibrateExponent(measurements, freq)
        assertEquals(3.0, calibrated, 0.0)
    }

    @Test
    fun pathLoss_24GhzVs5GhzReferenceRssiDifference() {
        val ref24 = PathLossModel.referenceRssi(2437)
        val ref5 = PathLossModel.referenceRssi(5180)
        // 5 GHz suffers higher free-space path loss, so the reference RSSI at
        // 1 m is lower (more negative / smaller) than 2.4 GHz.
        assertTrue(
            "5 GHz ref RSSI ($ref5) should be lower than 2.4 GHz ref RSSI ($ref24)",
            ref5 < ref24,
        )
        // The difference should be approximately 20*log10(5180/2437) ~ 6.6 dB.
        val expectedDiffDb = 20 * log10(5180.0 / 2437.0)
        assertEquals(expectedDiffDb, ref24 - ref5, 0.1)
    }

    @Test
    fun pathLoss_calibrateExponentIsClamped() {
        val freq = 2437
        val refRssi = PathLossModel.referenceRssi(freq)
        // Craft measurements that would produce an exponent > 6.0.
        // Use a very steep drop: rssi = refRssi - 10 * 8.0 * log10(d)
        val extremeMeasurements = listOf(2.0, 5.0, 10.0).map { d ->
            d to (refRssi - 10 * 8.0 * log10(d))
        }
        val calibrated = PathLossModel.calibrateExponent(extremeMeasurements, freq)
        assertTrue("Calibrated exponent should be <= 6.0 but was $calibrated", calibrated <= 6.0)

        // Craft measurements that would produce an exponent < 1.5.
        val shallowMeasurements = listOf(2.0, 5.0, 10.0).map { d ->
            d to (refRssi - 10 * 0.5 * log10(d))
        }
        val calibratedLow = PathLossModel.calibrateExponent(shallowMeasurements, freq)
        assertTrue("Calibrated exponent should be >= 1.5 but was $calibratedLow", calibratedLow >= 1.5)
    }

    @Test
    fun pathLoss_estimateDistanceWithDifferentExponents() {
        val freq = 2437
        val rssi = -60.0
        val distLowN = PathLossModel.estimateDistance(rssi, freq, pathLossExponent = 2.0)
        val distHighN = PathLossModel.estimateDistance(rssi, freq, pathLossExponent = 4.0)
        // A higher path-loss exponent means the same RSSI drop implies a
        // shorter actual distance (more attenuation per metre).
        assertTrue(
            "Higher exponent ($distHighN) should give shorter distance than lower ($distLowN)",
            distHighN < distLowN,
        )
    }

    @Test
    fun pathLoss_predictRssiDecreasesWithDistance() {
        val freq = 2437
        val rssiClose = PathLossModel.predictRssi(1.0, freq)
        val rssiFar = PathLossModel.predictRssi(10.0, freq)
        assertTrue(
            "RSSI at 10 m ($rssiFar) should be weaker than at 1 m ($rssiClose)",
            rssiFar < rssiClose,
        )
    }

    @Test
    fun pathLoss_calibrateEmptyListReturnsFallback() {
        val calibrated = PathLossModel.calibrateExponent(emptyList(), 2437)
        assertEquals(3.0, calibrated, 0.0)
    }
}
