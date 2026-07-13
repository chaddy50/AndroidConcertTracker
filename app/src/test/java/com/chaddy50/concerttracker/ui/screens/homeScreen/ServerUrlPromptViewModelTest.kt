package com.chaddy50.concerttracker.ui.screens.homeScreen

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt.ServerUrlPromptViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerUrlPromptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsRepository: SettingsRepository = mockk()
    private val performancesRepository: PerformancesRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.saveServerUrl(any()) } just Runs
        coEvery { performancesRepository.loadPerformances() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWithServerUrl(url: String) = ServerUrlPromptViewModel(
        settingsRepository = settingsRepository.also { every { it.serverUrl } returns flowOf(url) },
        performancesRepository = performancesRepository
    )

    // region init — prompt visibility

    @Test
    fun `showPrompt is true when server url is empty`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()

        assertTrue(viewModel.showPrompt)
    }

    @Test
    fun `showPrompt is false when server url is already set`() = runTest {
        val viewModel = viewModelWithServerUrl("https://server.example")
        advanceUntilIdle()

        assertFalse(viewModel.showPrompt)
    }

    @Test
    fun `showPrompt is true when server url is whitespace only`() = runTest {
        val viewModel = viewModelWithServerUrl("   ")
        advanceUntilIdle()

        assertTrue(viewModel.showPrompt)
    }

    @Test
    fun `showPrompt is false before the init read completes`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        // No advanceUntilIdle: the suspend serverUrl read has not run yet.
        assertFalse(viewModel.showPrompt)
    }

    @Test
    fun `init only reads the server url and does not save or refresh`() = runTest {
        viewModelWithServerUrl("")
        advanceUntilIdle()

        coVerify(exactly = 0) { settingsRepository.saveServerUrl(any()) }
        coVerify(exactly = 0) { performancesRepository.loadPerformances() }
    }

    @Test
    fun `only the first server url emission decides the prompt`() = runTest {
        val serverUrlFlow = MutableStateFlow("")
        every { settingsRepository.serverUrl } returns serverUrlFlow
        val viewModel = ServerUrlPromptViewModel(settingsRepository, performancesRepository)
        advanceUntilIdle()
        assertTrue(viewModel.showPrompt)

        serverUrlFlow.value = "https://server.example"
        advanceUntilIdle()

        // A later emission on the same Flow does not re-toggle the one-shot decision.
        assertTrue(viewModel.showPrompt)
    }

    // endregion

    // region input state

    @Test
    fun `serverUrlInput starts empty`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()

        assertEquals("", viewModel.serverUrlInput)
    }

    @Test
    fun `onServerUrlInputChanged captures input verbatim`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()

        viewModel.onServerUrlInputChanged("https://x")

        assertEquals("https://x", viewModel.serverUrlInput)
    }

    @Test
    fun `onServerUrlInputChanged keeps the latest value`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()

        viewModel.onServerUrlInputChanged("first")
        viewModel.onServerUrlInputChanged("second")

        assertEquals("second", viewModel.serverUrlInput)
    }

    @Test
    fun `onServerUrlInputChanged can clear the field`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()

        viewModel.onServerUrlInputChanged("something")
        viewModel.onServerUrlInputChanged("")

        assertEquals("", viewModel.serverUrlInput)
    }

    // endregion

    // region onConfirm

    @Test
    fun `onConfirm saves the entered url`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("https://server.example")

        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify { settingsRepository.saveServerUrl("https://server.example") }
    }

    @Test
    fun `onConfirm trims surrounding whitespace before saving`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("  https://server.example  ")

        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify { settingsRepository.saveServerUrl("https://server.example") }
    }

    @Test
    fun `onConfirm hides the prompt`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("https://server.example")

        viewModel.onConfirm()
        advanceUntilIdle()

        assertFalse(viewModel.showPrompt)
    }

    @Test
    fun `onConfirm triggers a single performances refresh`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("https://server.example")

        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify(exactly = 1) { performancesRepository.loadPerformances() }
    }

    @Test
    fun `onConfirm saves before refreshing`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("https://server.example")

        viewModel.onConfirm()
        advanceUntilIdle()

        coVerifyOrder {
            settingsRepository.saveServerUrl("https://server.example")
            performancesRepository.loadPerformances()
        }
    }

    @Test
    fun `onConfirm still refreshes and hides the prompt when the refresh fails offline`() = runTest {
        coEvery { performancesRepository.loadPerformances() } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("https://server.example")

        viewModel.onConfirm()
        advanceUntilIdle()

        coVerify(exactly = 1) { performancesRepository.loadPerformances() }
        assertFalse(viewModel.showPrompt)
    }

    @Test
    fun `onConfirm hides the prompt on a successful refresh`() = runTest {
        val viewModel = viewModelWithServerUrl("")
        advanceUntilIdle()
        viewModel.onServerUrlInputChanged("https://server.example")

        viewModel.onConfirm()
        advanceUntilIdle()

        assertFalse(viewModel.showPrompt)
    }

    // endregion
}
