package com.chaddy50.concerttracker.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chaddy50.concerttracker.data.repository.SyncOperationsRepository

/** Builds an in-memory [ConcertTrackerDatabase] for Robolectric-backed DAO/repository tests. */
fun inMemoryDatabase(): ConcertTrackerDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        ConcertTrackerDatabase::class.java
    ).allowMainThreadQueries().build()

/** A [SyncOperationsRepository] backed by this test database. */
fun ConcertTrackerDatabase.syncOperationsRepository(): SyncOperationsRepository =
    SyncOperationsRepository(syncOperationDao())
