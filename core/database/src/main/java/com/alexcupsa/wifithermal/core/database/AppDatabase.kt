package com.alexcupsa.wifithermal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alexcupsa.wifithermal.core.database.dao.IncidentDao
import com.alexcupsa.wifithermal.core.database.dao.WhitelistDao
import com.alexcupsa.wifithermal.core.database.entity.AuthorizedApEntity
import com.alexcupsa.wifithermal.core.database.entity.IncidentEntity

@Database(
    entities = [
        AuthorizedApEntity::class,
        IncidentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun whitelistDao(): WhitelistDao
    abstract fun incidentDao(): IncidentDao
}
