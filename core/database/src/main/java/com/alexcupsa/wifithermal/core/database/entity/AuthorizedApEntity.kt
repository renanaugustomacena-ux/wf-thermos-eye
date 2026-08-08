package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "authorized_aps",
    indices = [
        Index(value = ["ssid"]),
        Index(value = ["deviceType"]),
    ],
)
data class AuthorizedApEntity(
    @PrimaryKey val bssid: String,
    val ssid: String,
    val location: String?,
    val owner: String?,
    val deviceType: String,
    val expectedSecurity: String,
    val notes: String?,
    val authorizedAt: Long,
)
