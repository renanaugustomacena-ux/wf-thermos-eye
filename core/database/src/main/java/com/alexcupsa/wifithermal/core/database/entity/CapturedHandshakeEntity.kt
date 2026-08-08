package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "captured_handshakes",
    indices = [
        Index(value = ["bssid"]),
        Index(value = ["status"]),
        Index(value = ["capturedAt"]),
    ],
)
data class CapturedHandshakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bssid: String,
    val ssid: String,
    val kind: String,
    val payloadHex: String,
    val capturedAt: Long,
    val sourceDevice: String,
    val status: String,
    val backendJobId: String?,
    val crackResult: String?,
)
