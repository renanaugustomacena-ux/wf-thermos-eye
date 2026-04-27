package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rssi_samples",
    indices = [
        Index(value = ["bssid"]),
        Index(value = ["timestamp"]),
    ],
)
data class RssiSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val timestamp: Long,
)
