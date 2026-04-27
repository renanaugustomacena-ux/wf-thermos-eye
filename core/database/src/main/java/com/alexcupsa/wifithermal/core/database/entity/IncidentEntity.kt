package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incidents",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["bssid"]),
        Index(value = ["kind"]),
        Index(value = ["severity"]),
    ],
)
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val kind: String,
    val bssid: String,
    val ssid: String?,
    val severity: String,
    val summary: String,
    val evidenceJson: String,
)
