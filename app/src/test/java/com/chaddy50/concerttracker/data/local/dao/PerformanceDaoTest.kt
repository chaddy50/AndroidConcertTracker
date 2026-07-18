package com.chaddy50.concerttracker.data.local.dao

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
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

    // Load every page of pagingPast(now) and return the row ids in order.
    private suspend fun pagingPastIds(pageSize: Int = 50): List<String> {
        val pager = TestPager(PagingConfig(pageSize = pageSize, enablePlaceholders = false), performanceDao.pagingPast(now))
        val ids = mutableListOf<String>()
        var page = pager.refresh() as LoadResult.Page
        ids += page.data.map { it.performance.id }
        while (page.nextKey != null) {
            page = pager.append() as LoadResult.Page
            ids += page.data.map { it.performance.id }
        }
        return ids
    }

    @Test
    fun `pagingPast returns all rows before now, ignoring status, descending`() = runTest {
        // A past-dated UPCOMING row (the stale "June 15" case) now correctly lands in Past.
        insert("staleUpcoming", PerformanceStatus.UPCOMING, "2024-05-15T00:00:00Z")
        insert("attended", PerformanceStatus.ATTENDED, "2024-04-01T00:00:00Z")
        insert("future", PerformanceStatus.UPCOMING, "2024-07-01T00:00:00Z") // excluded

        assertEquals(listOf("staleUpcoming", "attended"), pagingPastIds())
    }

    @Test
    fun `pagingPast excludes a row dated exactly now (strict less-than)`() = runTest {
        insert("exactlyNow", PerformanceStatus.ATTENDED, now)
        insert("past", PerformanceStatus.ATTENDED, "2024-05-01T00:00:00Z")

        assertEquals(listOf("past"), pagingPastIds())
    }

    @Test
    fun `pagingPast excludes PENDING_DELETE tombstones`() = runTest {
        insert("visible", PerformanceStatus.ATTENDED, "2024-05-01T00:00:00Z")
        insert("tombstone", PerformanceStatus.ATTENDED, "2024-04-01T00:00:00Z", syncState = "PENDING_DELETE")

        assertEquals(listOf("visible"), pagingPastIds())
    }

    @Test
    fun `pagingPast pages the full list in descending order without gaps or duplicates`() = runTest {
        // 25 past rows, dated Jan..(Jan+24 days) 2024, all before `now`.
        val expected = (24 downTo 0).map { day ->
            val id = "p%02d".format(day)
            insert(id, PerformanceStatus.ATTENDED, "2024-01-%02dT00:00:00Z".format(day + 1))
            id
        }

        // First page respects the page size (initialLoadSize pinned to pageSize so refresh loads one page).
        val firstPage = TestPager(
            PagingConfig(pageSize = 10, initialLoadSize = 10, enablePlaceholders = false),
            performanceDao.pagingPast(now)
        ).run { refresh() as LoadResult.Page }
        assertEquals(10, firstPage.data.size)
        // Concatenated pages equal the full DESC list with no gaps or duplicates.
        assertEquals(expected, pagingPastIds(pageSize = 10))
    }

    @Test
    fun `pagingPast returns an empty page when nothing is past`() = runTest {
        insert("future", PerformanceStatus.UPCOMING, "2024-07-01T00:00:00Z")

        val page = TestPager(PagingConfig(pageSize = 10, enablePlaceholders = false), performanceDao.pagingPast(now))
            .run { refresh() as LoadResult.Page }
        assertEquals(emptyList<String>(), page.data.map { it.performance.id })
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

    @Test
    fun `updateNotes sets notes and syncState while leaving other columns untouched`() = runTest {
        insert("a", PerformanceStatus.ATTENDED, "2024-01-01T00:00:00Z")

        performanceDao.updateNotes("a", "Great show", "PENDING")

        val row = performanceDao.getById("a")!!
        assertEquals("Great show", row.notes)
        assertEquals("PENDING", row.syncState)
        assertEquals("2024-01-01T00:00:00Z", row.date)
        assertEquals(PerformanceStatus.ATTENDED.name, row.status)
        assertEquals("v1", row.venueId)
    }

    @Test
    fun `updateNotes clears notes with an empty string`() = runTest {
        db.venueDao().upsert(listOf(VenueEntity("v1", "Hall", "o", "way")))
        performanceDao.upsert(
            PerformanceEntity(id = "a", date = now, status = "ATTENDED", venueId = "v1", notes = "old")
        )

        performanceDao.updateNotes("a", "", "PENDING")

        assertEquals("", performanceDao.getById("a")?.notes)
    }

    @Test
    fun `updateNotes is a no-op for an unknown id`() = runTest {
        insert("a", PerformanceStatus.ATTENDED, "2024-01-01T00:00:00Z")

        performanceDao.updateNotes("missing", "x", "PENDING")

        assertEquals("", performanceDao.getById("a")?.notes)
        assertEquals("SYNCED", performanceDao.getById("a")?.syncState)
    }

    @Test
    fun `observePerformance re-emits the new notes after updateNotes`() = runTest {
        insert("a", PerformanceStatus.ATTENDED, "2024-01-01T00:00:00Z")

        performanceDao.updateNotes("a", "Encore was superb", "PENDING")

        assertEquals("Encore was superb", performanceDao.observePerformance("a").first()?.performance?.notes)
    }

}
