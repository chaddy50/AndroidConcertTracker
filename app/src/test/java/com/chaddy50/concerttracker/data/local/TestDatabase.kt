package com.chaddy50.concerttracker.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** Builds an in-memory [ConcertTrackerDatabase] for Robolectric-backed DAO/repository tests. */
fun inMemoryDatabase(): ConcertTrackerDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        ConcertTrackerDatabase::class.java
    ).allowMainThreadQueries().build()
