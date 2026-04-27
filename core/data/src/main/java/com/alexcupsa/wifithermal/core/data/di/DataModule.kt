package com.alexcupsa.wifithermal.core.data.di

import android.content.Context
import androidx.room.Room
import com.alexcupsa.wifithermal.core.database.AppDatabase
import com.alexcupsa.wifithermal.core.database.dao.AccessPointDao
import com.alexcupsa.wifithermal.core.database.dao.FloorPlanDao
import com.alexcupsa.wifithermal.core.database.dao.MeasurementDao
import com.alexcupsa.wifithermal.core.database.dao.SurveyDao
import com.alexcupsa.wifithermal.core.engine.signal.SignalProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // v0.x: schema is still settling. Destructive migration is the
        // explicit policy until v1 ships, at which point real Migration
        // objects will replace this and exportSchema will be flipped on.
        @Suppress("DEPRECATION")
        return Room.databaseBuilder(context, AppDatabase::class.java, "wifi_thermal_scanner.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSurveyDao(db: AppDatabase): SurveyDao = db.surveyDao()

    @Provides
    @Singleton
    fun provideMeasurementDao(db: AppDatabase): MeasurementDao = db.measurementDao()

    @Provides
    @Singleton
    fun provideFloorPlanDao(db: AppDatabase): FloorPlanDao = db.floorPlanDao()

    @Provides
    @Singleton
    fun provideAccessPointDao(db: AppDatabase): AccessPointDao = db.accessPointDao()

    @Provides
    @Singleton
    fun provideSignalProcessor(): SignalProcessor = SignalProcessor()
}
