package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.database.dao.RssiSampleDao
import com.alexcupsa.wifithermal.core.database.entity.RssiSampleEntity
import com.alexcupsa.wifithermal.core.model.audit.RssiSample
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists RSSI samples paired with the device's GPS position at the time
 * of capture. Used by [com.alexcupsa.wifithermal.core.engine.audit.Triangulator]
 * to estimate the physical location of a rogue AP.
 *
 * Out-of-scope BSSIDs are filtered upstream — this repo trusts its inputs.
 */
@Singleton
class RssiSampleRepository @Inject constructor(
    private val dao: RssiSampleDao,
) {
    suspend fun record(sample: RssiSample) {
        dao.insert(sample.toEntity())
    }

    suspend fun recordBatch(samples: List<RssiSample>) {
        if (samples.isEmpty()) return
        dao.insertAll(samples.map { it.toEntity() })
    }

    suspend fun samplesFor(bssid: String, limit: Int = 200): List<RssiSample> =
        dao.forBssid(bssid, limit).map { it.toDomain() }

    suspend fun countFor(bssid: String): Int = dao.countForBssid(bssid)

    suspend fun pruneBefore(beforeMs: Long): Int = dao.pruneBefore(beforeMs)
}

private fun RssiSampleEntity.toDomain() = RssiSample(
    bssid = bssid,
    rssi = rssi,
    frequencyMhz = frequencyMhz,
    lat = lat,
    lon = lon,
    accuracyM = accuracyM,
    timestamp = timestamp,
)

private fun RssiSample.toEntity() = RssiSampleEntity(
    bssid = bssid,
    rssi = rssi,
    frequencyMhz = frequencyMhz,
    lat = lat,
    lon = lon,
    accuracyM = accuracyM,
    timestamp = timestamp,
)
