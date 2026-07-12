package com.chaddy50.concerttracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chaddy50.concerttracker.data.local.dao.ComposerDao
import com.chaddy50.concerttracker.data.local.dao.PerformanceDao
import com.chaddy50.concerttracker.data.local.dao.PerformerDao
import com.chaddy50.concerttracker.data.local.dao.SetListEntryDao
import com.chaddy50.concerttracker.data.local.dao.SyncOperationDao
import com.chaddy50.concerttracker.data.local.dao.VenueDao
import com.chaddy50.concerttracker.data.local.dao.WorkDao
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity
import com.chaddy50.concerttracker.data.local.entity.SyncOperationEntity
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.HeadlinePerformerEntity
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.local.entity.FeaturedPerformerEntity
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.entity.WorkComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity

@Database(
    entities = [
        PerformanceEntity::class,
        VenueEntity::class,
        PerformerEntity::class,
        WorkEntity::class,
        ComposerEntity::class,
        SetListEntryEntity::class,
        HeadlinePerformerEntity::class,
        WorkComposerEntity::class,
        FeaturedPerformerEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ConcertTrackerDatabase : RoomDatabase() {
    abstract fun performanceDao(): PerformanceDao
    abstract fun setListEntryDao(): SetListEntryDao
    abstract fun venueDao(): VenueDao
    abstract fun performerDao(): PerformerDao
    abstract fun workDao(): WorkDao
    abstract fun composerDao(): ComposerDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        const val DATABASE_NAME = "concert_tracker.db"
    }
}
