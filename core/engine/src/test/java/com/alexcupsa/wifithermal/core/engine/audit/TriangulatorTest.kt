package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.audit.RssiSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TriangulatorTest {

    @Test
    fun `fewer than 3 samples returns null`() {
        val samples = listOf(
            sample(45.4642, 9.1900, rssi = -50),
            sample(45.4644, 9.1902, rssi = -55),
        )
        assertNull(Triangulator.triangulate(samples))
    }

    @Test
    fun `mixed bssid throws`() {
        val samples = listOf(
            sample(45.4642, 9.1900, rssi = -50, bssid = "AA:BB:CC:00:00:01"),
            sample(45.4644, 9.1902, rssi = -55, bssid = "AA:BB:CC:00:00:02"),
            sample(45.4646, 9.1904, rssi = -60, bssid = "AA:BB:CC:00:00:03"),
        )
        var threw = false
        try {
            Triangulator.triangulate(samples)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `centroid biases toward strongest sample`() {
        // 3 samples in line. Strongest signal at point B → estimate should be
        // closer to B than to either of the other two.
        val pointA = Pair(45.4640, 9.1900)
        val pointB = Pair(45.4650, 9.1910)
        val pointC = Pair(45.4660, 9.1920)
        val samples = listOf(
            sample(pointA.first, pointA.second, rssi = -75),
            sample(pointB.first, pointB.second, rssi = -45),  // strongest
            sample(pointC.first, pointC.second, rssi = -75),
        )

        val result = Triangulator.triangulate(samples)
        assertNotNull(result)

        val distToA = Triangulator.distanceMetres(result!!.estimatedLat, result.estimatedLon, pointA.first, pointA.second)
        val distToB = Triangulator.distanceMetres(result.estimatedLat, result.estimatedLon, pointB.first, pointB.second)
        val distToC = Triangulator.distanceMetres(result.estimatedLat, result.estimatedLon, pointC.first, pointC.second)

        assertTrue("should be closest to strongest sample (B). dB=$distToB dA=$distToA dC=$distToC",
            distToB < distToA && distToB < distToC)
    }

    @Test
    fun `confidence increases with sample count`() {
        val tightCluster3 = (0 until 3).map { i ->
            sample(45.4640 + i * 0.0001, 9.1900 + i * 0.0001, rssi = -55)
        }
        val tightCluster10 = (0 until 10).map { i ->
            sample(45.4640 + i * 0.0001, 9.1900 + i * 0.0001, rssi = -55)
        }
        val r3 = Triangulator.triangulate(tightCluster3)!!
        val r10 = Triangulator.triangulate(tightCluster10)!!
        assertTrue("more samples should give >= confidence", r10.confidence >= r3.confidence)
    }

    @Test
    fun `confidence increases with baseline spread`() {
        val tight = (0 until 4).map { i ->
            sample(45.4640 + i * 0.00001, 9.1900 + i * 0.00001, rssi = -55) // ~1m apart
        }
        val wide = (0 until 4).map { i ->
            sample(45.4640 + i * 0.0001, 9.1900 + i * 0.0001, rssi = -55) // ~10m apart
        }
        val rTight = Triangulator.triangulate(tight)!!
        val rWide = Triangulator.triangulate(wide)!!
        assertTrue("wider baseline should give >= confidence", rWide.confidence >= rTight.confidence)
    }

    @Test
    fun `haversine distance sanity`() {
        // Known: 1 deg lat ~= 111 km
        val d = Triangulator.distanceMetres(45.0, 9.0, 46.0, 9.0)
        assertEquals(111_000.0, d, 1_000.0)
    }

    @Test
    fun `result baseline matches widest sample pair`() {
        val samples = listOf(
            sample(45.4640, 9.1900, rssi = -55), // origin
            sample(45.4640, 9.1900 + 0.0001, rssi = -55), // ~7.8m east
            sample(45.4640, 9.1900 + 0.0002, rssi = -55), // ~15.6m east
        )
        val result = Triangulator.triangulate(samples)!!
        assertTrue(
            "baseline ~ 15.6m, got ${result.baselineMetres}",
            abs(result.baselineMetres - 15.6) < 2.0,
        )
    }

    private fun sample(
        lat: Double,
        lon: Double,
        rssi: Int,
        bssid: String = "AA:BB:CC:00:00:01",
        accuracyM: Float = 5f,
    ) = RssiSample(
        bssid = bssid,
        rssi = rssi,
        frequencyMhz = 2412,
        lat = lat,
        lon = lon,
        accuracyM = accuracyM,
        timestamp = 1_700_000_000_000L,
    )
}
