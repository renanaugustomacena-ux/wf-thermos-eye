package com.alexcupsa.wifithermal.core.data.di

import android.content.Context
import androidx.room.Room
import com.alexcupsa.wifithermal.core.database.AppDatabase
import com.alexcupsa.wifithermal.core.database.dao.IncidentDao
import com.alexcupsa.wifithermal.core.database.dao.RssiSampleDao
import com.alexcupsa.wifithermal.core.database.dao.WhitelistDao
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
        return Room.databaseBuilder(context, AppDatabase::class.java, "wf_audit.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWhitelistDao(db: AppDatabase): WhitelistDao = db.whitelistDao()

    @Provides
    @Singleton
    fun provideIncidentDao(db: AppDatabase): IncidentDao = db.incidentDao()

    @Provides
    @Singleton
    fun provideRssiSampleDao(db: AppDatabase): RssiSampleDao = db.rssiSampleDao()

    @Provides
    @Singleton
    fun provideSignalProcessor(): SignalProcessor = SignalProcessor()
}
