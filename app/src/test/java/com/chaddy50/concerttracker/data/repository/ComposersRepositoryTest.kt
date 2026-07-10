package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposersRepositoryTest {

    private lateinit var db: ConcertTrackerDatabase
    private lateinit var composersRepository: ComposersRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        composersRepository = ComposersRepository(db.composerDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `searchComposers emits cached composers mapped to domain`() = runTest {
        db.composerDao().upsert(listOf(ComposerEntity("c1", "Mozart", "Mozart, W.A.", "oo1")))
        val composers = composersRepository.searchComposers("").first()
        assertEquals(listOf("c1"), composers.map { it.id })
        assertEquals("oo1", composers.single().openOpusId)
    }

    @Test
    fun `searchComposers filters by case-insensitive name substring`() = runTest {
        db.composerDao().upsert(
            listOf(
                ComposerEntity("c1", "Mozart"),
                ComposerEntity("c2", "Bach")
            )
        )
        assertEquals(listOf("c1"), composersRepository.searchComposers("moz").first().map { it.id })
    }
}
