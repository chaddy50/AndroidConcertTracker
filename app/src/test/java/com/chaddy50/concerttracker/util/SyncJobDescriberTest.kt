package com.chaddy50.concerttracker.util

import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.WorksRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncJobDescriberTest {

    private val performancesRepository: PerformancesRepository = mockk()
    private val setListEntriesRepository: SetListEntriesRepository = mockk()
    private val performersRepository: PerformersRepository = mockk()
    private val worksRepository: WorksRepository = mockk()

    private val describer = SyncJobDescriber(
        performancesRepository, setListEntriesRepository, performersRepository, worksRepository
    )

    private fun job(entityType: SyncEntityType, entityId: String) = SyncJob(
        id = 1,
        entityId = entityId,
        entityType = entityType,
        operationType = SyncOperationType.UPDATE,
        failed = false
    )

    private fun performance(setList: List<SetListEntry> = emptyList()) = Performance(
        id = "p1",
        date = "2024-06-01T19:00:00Z",
        venue = Venue("v1", "Carnegie Hall", "1", "way"),
        status = PerformanceStatus.UPCOMING,
        setList = setList
    )

    @Test
    fun `performance resolves venue name and date`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance())

        val enriched = describer.describe(job(SyncEntityType.PERFORMANCE, "p1"))

        assertEquals("Carnegie Hall", enriched.description)
        assertEquals("2024-06-01T19:00:00Z", enriched.performanceDateIso)
    }

    @Test
    fun `set list entry resolves work title and its performance date`() = runTest {
        val entry = SetListEntry(id = "e1", work = Work("w1", "Symphony No. 5"), order = 1)
        coEvery { setListEntriesRepository.getParentPerformanceId("e1") } returns "p1"
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance(setList = listOf(entry)))

        val enriched = describer.describe(job(SyncEntityType.SET_LIST_ENTRY, "e1"))

        assertEquals("Symphony No. 5", enriched.description)
        assertEquals("2024-06-01T19:00:00Z", enriched.performanceDateIso)
    }

    @Test
    fun `performer resolves its name and no date`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns Performer("pe1", "Yo-Yo Ma", PerformerType.SOLO)

        val enriched = describer.describe(job(SyncEntityType.PERFORMER, "pe1"))

        assertEquals("Yo-Yo Ma", enriched.description)
        assertNull(enriched.performanceDateIso)
    }

    @Test
    fun `a missing target leaves context null rather than throwing`() = runTest {
        coEvery { worksRepository.getWork("gone") } returns null

        val enriched = describer.describe(job(SyncEntityType.WORK, "gone"))

        assertNull(enriched.description)
        assertNull(enriched.performanceDateIso)
        assertEquals(SyncEntityType.WORK, enriched.entityType)
    }
}
