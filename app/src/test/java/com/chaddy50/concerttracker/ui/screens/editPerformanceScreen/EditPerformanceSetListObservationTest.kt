package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import com.chaddy50.concerttracker.testJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reproduces the real "add a work to an existing performance" flow at the layer the Edit Performance
 * screen relies on: after the refactor it observes the set list from Room, so a createSetListEntry
 * write-through must make EditPerformanceViewModel.currentSetList re-emit with no manual refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPerformanceSetListObservationTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var performancesRepository: PerformancesRepository
    private lateinit var setListEntriesRepository: SetListEntriesRepository

    private fun parentJson(setList: String) = """
        {"id":"p1","date":"2024-06-01T19:00:00Z","venue":{"id":"v1","name":"Hall","osm_id":"1","osm_type":"way"},"performers":[],"status":"UPCOMING","set_list":[$setList]}
    """.trimIndent()

    private val entryWithComposer =
        """{"id":"p1_s1","work":{"id":"w1","title":"Symphony","composers":[{"id":"c1","name":"Beethoven"}]},"order":1,"featured_performers":[]}"""

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        performancesRepository = PerformancesRepository(
            settingsRepository, OkHttpClient(), json, db,
            db.performanceDao(), db.setListEntryDao(), db.venueDao(),
            db.performerDao(), db.workDao(), db.composerDao()
        )
        setListEntriesRepository = SetListEntriesRepository(
            settingsRepository, OkHttpClient(), json, db.setListEntryDao(), performancesRepository
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `currentSetList updates after a set-list entry is created against the server`() = runTest {
        // VM init: loadPerformance() fetches the (empty) performance and caches it in Room.
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(parentJson(setList = "")))
        val viewModel = EditPerformanceViewModel(
            SavedStateHandle(mapOf("id" to "p1")), performancesRepository
        )
        advanceUntilIdle()
        assertTrue("set list should start empty", viewModel.currentSetList.isEmpty())

        // Add an entry: POST returns the new entry, then the write-through re-fetches the parent.
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(entryWithComposer))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(parentJson(setList = entryWithComposer)))
        val result = setListEntriesRepository.createSetListEntry(SetListEntryCreateRequest("p1", "w1", 1, emptyList()))
        advanceUntilIdle()

        // localize: did the write-through reach Room?
        val cached = performancesRepository.observePerformance("p1").first()
        System.out.println("REPRO RESULT=$result CACHED_SETLIST=${cached?.setList?.map { it.id }}")
        assertEquals(listOf("p1_s1"), viewModel.currentSetList.map { it.id })
    }
}
