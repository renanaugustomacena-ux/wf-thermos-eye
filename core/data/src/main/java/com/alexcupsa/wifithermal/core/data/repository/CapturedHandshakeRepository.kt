package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.database.dao.CapturedHandshakeDao
import com.alexcupsa.wifithermal.core.database.entity.CapturedHandshakeEntity
import com.alexcupsa.wifithermal.core.engine.audit.OffensiveScopeGuard
import com.alexcupsa.wifithermal.core.model.audit.CaptureKind
import com.alexcupsa.wifithermal.core.model.audit.CapturedHandshake
import com.alexcupsa.wifithermal.core.model.audit.CrackStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists Layer B captures. Every insertion is hard-gated by
 * [OffensiveScopeGuard] — a [CapturedHandshake] for an out-of-scope BSSID
 * cannot enter the database. Defense in depth: the BLE bridge also checks,
 * but if a future caller forgets, the repository still refuses.
 */
@Singleton
class CapturedHandshakeRepository @Inject constructor(
    private val dao: CapturedHandshakeDao,
    private val scopeRepo: AuthorizationManifestRepository,
) {
    fun observeAll(): Flow<List<CapturedHandshake>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForBssid(bssid: String): Flow<List<CapturedHandshake>> =
        dao.observeForBssid(bssid).map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): CapturedHandshake? = dao.getById(id)?.toDomain()

    /**
     * Returns null if the BSSID is not authorized for offensive operations.
     * Caller should treat null as "rejected" and surface the
     * [OffensiveScopeGuard.evaluate] decision separately for the audit log.
     */
    suspend fun record(
        bssid: String,
        ssid: String,
        kind: CaptureKind,
        payloadHex: String,
        sourceDevice: String,
        capturedAt: Long = System.currentTimeMillis(),
    ): Long? {
        val decision = OffensiveScopeGuard.evaluate(scopeRepo.scope.value, bssid)
        if (decision !is OffensiveScopeGuard.Decision.Allowed) return null

        val id = dao.insert(
            CapturedHandshakeEntity(
                bssid = bssid.uppercase(),
                ssid = ssid,
                kind = kind.name,
                payloadHex = payloadHex,
                capturedAt = capturedAt,
                sourceDevice = sourceDevice,
                status = CrackStatus.PENDING.name,
                backendJobId = null,
                crackResult = null,
            ),
        )
        return id
    }

    suspend fun updateOutcome(
        id: Long,
        status: CrackStatus,
        backendJobId: String? = null,
        crackResult: String? = null,
    ) {
        dao.updateOutcome(id, status.name, backendJobId, crackResult)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    fun countByStatus(status: CrackStatus): Flow<Int> = dao.countByStatus(status.name)
}

private fun CapturedHandshakeEntity.toDomain() = CapturedHandshake(
    id = id,
    bssid = bssid,
    ssid = ssid,
    kind = runCatching { CaptureKind.valueOf(kind) }.getOrDefault(CaptureKind.PMKID),
    payloadHex = payloadHex,
    capturedAt = capturedAt,
    sourceDevice = sourceDevice,
    status = runCatching { CrackStatus.valueOf(status) }.getOrDefault(CrackStatus.PENDING),
    backendJobId = backendJobId,
    crackResult = crackResult,
)
