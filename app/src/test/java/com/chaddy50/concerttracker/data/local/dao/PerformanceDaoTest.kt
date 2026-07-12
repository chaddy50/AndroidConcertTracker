package com.chaddy50.concerttracker.data.local.dao

import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformanceDaoTest {

    private lateinit var db: ConcertTrackerDatabase
    private lateinit var performanceDao: PerformanceDao
    private lateinit var setListEntryDao: SetListEntryDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        performanceDao = db.performanceDao()
        setListEntryDao = db.setListEntryDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insert(id: String, status: PerformanceStatus, date: String, syncState: String = "SYNCED") {
        db.venueDao().upsert(listOf(VenueEntity("v1", "Hall", "o", "way")))
        performanceDao.upsert(
            PerformanceEntity(id = id, date = date, status = status.name, venueId = "v1", syncState = syncState)
        )
    }

    @Test
    fun `observeUpcoming returns only upcoming sorted by date ascending`() = runTest {
        insert("a", PerformanceStatus.UPCOMING, "2024-03-01T00:00:00Z")
        insert("b", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")
        insert("c", PerformanceStatus.ATTENDED, "2024-02-01T00:00:00Z")

        val result = performanceDao.observeUpcoming().first()

        assertEquals(listOf("b", "a"), result.map { it.performance.id })
    }

    @Test
    fun `observeNextUpcoming returns earliest upcoming and null when none`() = runTest {
        assertNull(performanceDao.observeNextUpcoming().first())
        insert("a", PerformanceStatus.UPCOMING, "2024-03-01T00:00:00Z")
        insert("b", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")

        assertEquals("b", performanceDao.observeNextUpcoming().first()?.performance?.id)
    }

    @Test
    fun `observeRecentlyAttended returns attended after cutoff only`() = runTest {
        insert("recent", PerformanceStatus.ATTENDED, "2024-02-20T00:00:00Z")
        insert("old", PerformanceStatus.ATTENDED, "2024-01-01T00:00:00Z")
        insert("upcoming", PerformanceStatus.UPCOMING, "2024-02-25T00:00:00Z")

        val result = performanceDao.observeRecentlyAttended("2024-02-01T00:00:00Z").first()

        assertEquals(listOf("recent"), result.map { it.performance.id })
    }

    @Test
    fun `observePast returns past statuses excluding upcoming, date descending`() = runTest {
        insert("attended", PerformanceStatus.ATTENDED, "2024-01-01T00:00:00Z")
        insert("missed", PerformanceStatus.MISSED, "2024-03-01T00:00:00Z")
        insert("upcoming", PerformanceStatus.UPCOMING, "2024-02-01T00:00:00Z")

        val result = performanceDao.observePast().first()

        assertEquals(listOf("missed", "attended"), result.map { it.performance.id })
    }

    @Test
    fun `observePerformance returns matching row and null when absent`() = runTest {
        insert("a", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")

        assertEquals("a", performanceDao.observePerformance("a").first()?.performance?.id)
        assertNull(performanceDao.observePerformance("missing").first())
    }

    @Test
    fun `deleteSyncedNotIn removes only absent SYNCED rows and preserves unsynced local work`() = runTest {
        insert("keep", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")
        insert("drop", PerformanceStatus.UPCOMING, "2024-02-01T00:00:00Z")
        insert("pending", PerformanceStatus.UPCOMING, "2024-03-01T00:00:00Z", syncState = "PENDING")

        performanceDao.deleteSyncedNotIn(listOf("keep"))

        // "drop" (synced, absent) is pruned; "keep" and the unsynced "pending" survive.
        assertEquals(listOf("keep", "pending"), performanceDao.observeUpcoming().first().map { it.performance.id })
    }

    @Test
    fun `observe queries exclude PENDING_DELETE rows`() = runTest {
        insert("visible", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")
        insert("tombstone", PerformanceStatus.UPCOMING, "2024-02-01T00:00:00Z", syncState = "PENDING_DELETE")

        assertEquals(listOf("visible"), performanceDao.observeUpcoming().first().map { it.performance.id })
        assertNull(performanceDao.observePerformance("tombstone").first())
    }

    @Test
    fun `deleting a performance cascades its set list entries`() = runTest {
        insert("a", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")
        setListEntryDao.upsert(listOf(SetListEntryEntity(id = "s1", performanceId = "a", workId = "w1", order = 1)))
        assertEquals("a", setListEntryDao.getPerformanceIdFor("s1"))

        performanceDao.delete("a")

        assertNull(setListEntryDao.getPerformanceIdFor("s1"))
    }

    @Test
    fun `getPerformanceIdFor returns null for unknown entry`() = runTest {
        assertNull(setListEntryDao.getPerformanceIdFor("nope"))
    }
}
