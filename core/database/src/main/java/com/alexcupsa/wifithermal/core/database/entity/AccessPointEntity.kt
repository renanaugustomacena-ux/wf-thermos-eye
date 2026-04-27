package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_points")
data class AccessPointEntity(
    @PrimaryKey val bssid: String,
    val ssid: String,
    val vendor: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val bestRssi: Int,
    val primaryFrequency: Int,
    val primaryChannel: Int,
    val security: String,
    val notes: String?,
    val isHidden: Boolean,
    val isFavorite: Boolean,
)
