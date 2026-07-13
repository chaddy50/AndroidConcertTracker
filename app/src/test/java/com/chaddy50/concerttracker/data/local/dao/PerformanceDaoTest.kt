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

    // Home tabs partition purely by date now; status must not affect placement. Fixed instant so tests
    // are deterministic (never Instant.now()).
    private val now = "2024-06-01T00:00:00Z"

    private suspend fun insert(id: String, status: PerformanceStatus, date: String, syncState: String = "SYNCED") {
        db.venueDao().upsert(listOf(VenueEntity("v1", "Hall", "o", "way")))
        performanceDao.upsert(
            PerformanceEntity(id = id, date = date, status = status.name, venueId = "v1", syncState = syncState)
        )
    }

    @Test
    fun `observeUpcoming returns date now-or-later ascending, ignoring status`() = runTest {
        // Future-dated ATTENDED is still upcoming (status no longer filters); past-dated is excluded.
        insert("futureAttended", PerformanceStatus.ATTENDED, "2024-07-01T00:00:00Z")
        insert("futureUpcoming", PerformanceStatus.UPCOMING, "2024-06-15T00:00:00Z")
        insert("past", PerformanceStatus.UPCOMING, "2024-05-01T00:00:00Z")

        val result = performanceDao.observeUpcoming(now).first()

        assertEquals(listOf("futureUpcoming", "futureAttended"), result.map { it.performance.id })
    }

    @Test
    fun `observeUpcoming includes a performance dated exactly now`() = runTest {
        insert("exactlyNow", PerformanceStatus.UPCOMING, now)

        assertEquals(listOf("exactlyNow"), performanceDao.observeUpcoming(now).first().map { it.performance.id })
    }

    @Test
    fun `observeNextUpcoming returns earliest future row skipping past, null when none`() = runTest {
        assertNull(performanceDao.observeNextUpcoming(now).first())
        insert("past", PerformanceStatus.UPCOMING, "2024-05-01T00:00:00Z")
        assertNull(performanceDao.observeNextUpcoming(now).first())

        insert("later", PerformanceStatus.UPCOMING, "2024-07-01T00:00:00Z")
        // Soonest future row wins even though its status is CANCELLED (status ignored).
        insert("soonest", PerformanceStatus.CANCELLED, "2024-06-10T00:00:00Z")

        assertEquals("soonest", performanceDao.observeNextUpcoming(now).first()?.performance?.id)
    }

    @Test
    fun `observeNextUpcoming includes a performance dated exactly now`() = runTest {
        insert("exactlyNow", PerformanceStatus.UPCOMING, now)

        assertEquals("exactlyNow", performanceDao.observeNextUpcoming(now).first()?.performance?.id)
    }

    @Test
    fun `observeRecentlyAttended returns rows within cutoff and now, ignoring status, descending`() = runTest {
        val cutoff = "2024-05-01T00:00:00Z"
        insert("recent", PerformanceStatus.SKIPPED, "2024-05-20T00:00:00Z")   // in window, non-attended -> included
        insert("recent2", PerformanceStatus.ATTENDED, "2024-05-10T00:00:00Z") // in window
        insert("tooOld", PerformanceStatus.ATTENDED, "2024-04-01T00:00:00Z")  // before cutoff -> excluded
        insert("future", PerformanceStatus.ATTENDED, "2024-07-01T00:00:00Z")  // >= now -> excluded

        val result = performanceDao.observeRecentlyAttended(cutoff, now).first()

        assertEquals(listOf("recent", "recent2"), result.map { it.performance.id })
    }

    @Test
    fun `observePast returns all rows before now, ignoring status, descending`() = runTest {
        // A past-dated UPCOMING row (the stale "June 15" case) now correctly lands in Past.
        insert("staleUpcoming", PerformanceStatus.UPCOMING, "2024-05-15T00:00:00Z")
        insert("attended", PerformanceStatus.ATTENDED, "2024-04-01T00:00:00Z")
        insert("future", PerformanceStatus.UPCOMING, "2024-07-01T00:00:00Z") // excluded

        val result = performanceDao.observePast(now).first()

        assertEquals(listOf("staleUpcoming", "attended"), result.map { it.performance.id })
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
        // Early nowIso so all rows count as upcoming here (this test is about deletion, not date filtering).
        assertEquals(listOf("keep", "pending"), performanceDao.observeUpcoming("2000-01-01T00:00:00Z").first().map { it.performance.id })
    }

    @Test
    fun `observe queries exclude PENDING_DELETE rows`() = runTest {
        insert("visible", PerformanceStatus.UPCOMING, "2024-01-01T00:00:00Z")
        insert("tombstone", PerformanceStatus.UPCOMING, "2024-02-01T00:00:00Z", syncState = "PENDING_DELETE")

        assertEquals(listOf("visible"), performanceDao.observeUpcoming("2000-01-01T00:00:00Z").first().map { it.performance.id })
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
