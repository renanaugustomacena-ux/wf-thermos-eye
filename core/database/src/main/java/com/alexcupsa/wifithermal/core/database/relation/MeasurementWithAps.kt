package com.alexcupsa.wifithermal.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.alexcupsa.wifithermal.core.database.entity.ApMeasurementEntity
import com.alexcupsa.wifithermal.core.database.entity.MeasurementPointEntity

data class MeasurementWithAps(
    @Embedded val point: MeasurementPointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "measurementPointId",
    )
    val apMeasurements: List<ApMeasurementEntity>,
)
