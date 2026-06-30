package com.chaddy50.concerttracker.dependencyInjection

import android.content.Context
import androidx.room.Room
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.dao.ComposerDao
import com.chaddy50.concerttracker.data.local.dao.PerformanceDao
import com.chaddy50.concerttracker.data.local.dao.PerformerDao
import com.chaddy50.concerttracker.data.local.dao.SetListEntryDao
import com.chaddy50.concerttracker.data.local.dao.VenueDao
import com.chaddy50.concerttracker.data.local.dao.WorkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ConcertTrackerDatabase =
        Room.databaseBuilder(
            context,
            ConcertTrackerDatabase::class.java,
            ConcertTrackerDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun providePerformanceDao(database: ConcertTrackerDatabase): PerformanceDao =
        database.performanceDao()

    @Provides
    fun provideSetListEntryDao(database: ConcertTrackerDatabase): SetListEntryDao =
        database.setListEntryDao()

    @Provides
    fun provideVenueDao(database: ConcertTrackerDatabase): VenueDao = database.venueDao()

    @Provides
    fun providePerformerDao(database: ConcertTrackerDatabase): PerformerDao =
        database.performerDao()

    @Provides
    fun provideWorkDao(database: ConcertTrackerDatabase): WorkDao = database.workDao()

    @Provides
    fun provideComposerDao(database: ConcertTrackerDatabase): ComposerDao =
        database.composerDao()
}
