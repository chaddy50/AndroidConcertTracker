package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusWork
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val openOpusRepository: OpenOpusRepository = mockk()

    private val work = OpenOpusWork(id = "w1", title = "Symphony No. 5", genre = "Orchestral")

    private fun viewModel(composerId: String = "c1") = WorkSearchViewModel(
        SavedStateHandle(mapOf("composerId" to composerId, "composerCompleteName" to "Beethoven")),
        openOpusRepository
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchWorks shows Results on Success`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("c1") } returns
            ApiResult.Success(listOf(work))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(WorkSearchUiState.Results(listOf(work)), viewModel.uiState)
    }

    @Test
    fun `fetchWorks shows Error carrying errorType on failure`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("c1") } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(WorkSearchUiState.Error(ApiErrorType.Type.NETWORK), viewModel.uiState)
    }

    @Test
    fun `blank composerId does not trigger an API call`() = runTest {
        val viewModel = viewModel(composerId = "")

        advanceUntilIdle()

        assertTrue(viewModel.uiState is WorkSearchUiState.Idle)
        coVerify(exactly = 0) { openOpusRepository.getWorksByComposer(any()) }
    }
}
