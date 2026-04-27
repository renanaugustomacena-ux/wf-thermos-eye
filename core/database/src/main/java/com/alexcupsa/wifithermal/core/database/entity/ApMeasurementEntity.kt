package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ap_measurements",
    foreignKeys = [ForeignKey(
        entity = MeasurementPointEntity::class,
        parentColumns = ["id"],
        childColumns = ["measurementPointId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("measurementPointId"), Index("bssid")],
)
data class ApMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measurementPointId: Long,
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val smoothedRssi: Double,
    val frequency: Int,
    val channel: Int,
    val channelWidth: String,
    val band: String,
    val security: String,
    val standard: String,
    val capabilities: String,
)
