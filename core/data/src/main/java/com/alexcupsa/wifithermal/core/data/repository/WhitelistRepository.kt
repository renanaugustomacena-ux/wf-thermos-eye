package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.database.dao.WhitelistDao
import com.alexcupsa.wifithermal.core.database.entity.AuthorizedApEntity
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import com.alexcupsa.wifithermal.core.model.audit.DeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/write side of the corporate whitelist. Membership is the only
 * primary signal the audit engine consumes — anything not here is a
 * candidate rogue (subject to ScopeGuard filtering upstream).
 */
@Singleton
class WhitelistRepository @Inject constructor(
    private val dao: WhitelistDao,
) {
    fun observeAll(): Flow<List<AuthorizedAccessPoint>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    fun count(): Flow<Int> = dao.count()

    suspend fun get(bssid: String): AuthorizedAccessPoint? = dao.getByBssid(bssid)?.toDomain()

    suspend fun bssidSet(): Set<String> = dao.getAllBssids().toSet()

    suspend fun authorizedSsidsFor(bssid: String): List<AuthorizedAccessPoint> =
        // Find all authorized APs that share this BSSID (should be 0 or 1)
        // OR use ssid lookup separately when needed by callers.
        listOfNotNull(dao.getByBssid(bssid)?.toDomain())

    suspend fun authorizedBssidsForSsid(ssid: String): List<AuthorizedAccessPoint> =
        dao.getBySsid(ssid).map { it.toDomain() }

    suspend fun upsert(ap: AuthorizedAccessPoint) {
        dao.upsert(ap.toEntity())
    }

    suspend fun remove(bssid: String) {
        dao.deleteByBssid(bssid)
    }

    suspend fun clear() {
        dao.deleteAll()
    }
}

private fun AuthorizedApEntity.toDomain() = AuthorizedAccessPoint(
    bssid = bssid,
    ssid = ssid,
    location = location,
    owner = owner,
    deviceType = runCatching { DeviceType.valueOf(deviceType) }.getOrDefault(DeviceType.OTHER),
    expectedSecurity = runCatching { SecurityType.valueOf(expectedSecurity) }
        .getOrDefault(SecurityType.UNKNOWN),
    notes = notes,
    authorizedAt = authorizedAt,
)

private fun AuthorizedAccessPoint.toEntity() = AuthorizedApEntity(
    bssid = bssid,
    ssid = ssid,
    location = location,
    owner = owner,
    deviceType = deviceType.name,
    expectedSecurity = expectedSecurity.name,
    notes = notes,
    authorizedAt = authorizedAt,
)
