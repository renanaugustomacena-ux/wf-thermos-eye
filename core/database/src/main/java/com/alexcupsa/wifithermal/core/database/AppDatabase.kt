package com.alexcupsa.wifithermal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alexcupsa.wifithermal.core.database.dao.AccessPointDao
import com.alexcupsa.wifithermal.core.database.dao.FloorPlanDao
import com.alexcupsa.wifithermal.core.database.dao.MeasurementDao
import com.alexcupsa.wifithermal.core.database.dao.SurveyDao
import com.alexcupsa.wifithermal.core.database.entity.AccessPointEntity
import com.alexcupsa.wifithermal.core.database.entity.ApMeasurementEntity
import com.alexcupsa.wifithermal.core.database.entity.FloorPlanEntity
import com.alexcupsa.wifithermal.core.database.entity.MeasurementPointEntity
import com.alexcupsa.wifithermal.core.database.entity.SurveyEntity

@Database(
    entities = [
        SurveyEntity::class,
        MeasurementPointEntity::class,
        ApMeasurementEntity::class,
        FloorPlanEntity::class,
        AccessPointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun floorPlanDao(): FloorPlanDao
    abstract fun accessPointDao(): AccessPointDao
}
