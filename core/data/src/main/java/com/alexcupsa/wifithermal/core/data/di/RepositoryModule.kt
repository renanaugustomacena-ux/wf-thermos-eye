package com.alexcupsa.wifithermal.core.data.di

import com.alexcupsa.wifithermal.core.data.repository.FloorPlanRepository
import com.alexcupsa.wifithermal.core.data.repository.FloorPlanRepositoryImpl
import com.alexcupsa.wifithermal.core.data.repository.MeasurementRepository
import com.alexcupsa.wifithermal.core.data.repository.MeasurementRepositoryImpl
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSurveyRepository(impl: SurveyRepositoryImpl): SurveyRepository

    @Binds
    abstract fun bindMeasurementRepository(impl: MeasurementRepositoryImpl): MeasurementRepository

    @Binds
    abstract fun bindFloorPlanRepository(impl: FloorPlanRepositoryImpl): FloorPlanRepository
}
