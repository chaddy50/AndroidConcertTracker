package com.chaddy50.concerttracker.ui.screens.settingsScreen

import androidx.compose.runtime.snapshots.Snapshot
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.ServerUrlValidationError
import com.chaddy50.concerttracker.data.repository.ServerValidationRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsRepository: SettingsRepository = mockk()
    private val serverValidationRepository: ServerValidationRepository = mockk()
    private val performancesRepository: PerformancesRepository = mockk()
    private val loadedUrl = "https://old.example"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.serverUrl } returns flowOf(loadedUrl)
        coEvery { settingsRepository.saveServerUrl(any()) } just Runs
        coEvery { serverValidationRepository.validate(any()) } returns ApiResult.Success(Unit)
        coEvery { performancesRepository.loadPerformances() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        settingsRepository = settingsRepository,
        serverValidationRepository = serverValidationRepository,
        performancesRepository = performancesRepository,
        applicationScope = CoroutineScope(testDispatcher)
    )

    /** Mutates state and flushes the snapshot so the live `snapshotFlow` validator observes it. */
    private fun type(vm: SettingsViewModel, url: String) {
        vm.onServerUrlChanged(url)
        Snapshot.sendApplyNotifications()
    }

    // region init / input state

    @Test
    fun `init loads the persisted server url`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(loadedUrl, vm.serverUrl)
    }

    @Test
    fun `state starts clear`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.isValidating)
        assertNull(vm.validationError)
        assertFalse(vm.showInvalidExitDialog)
        assertFalse(vm.exitApproved)
    }

    @Test
    fun `onServerUrlChanged updates state and never persists per keystroke`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onServerUrlChanged("https://typing.example")

        assertEquals("https://typing.example", vm.serverUrl)
        coVerify(exactly = 0) { settingsRepository.saveServerUrl(any()) }
    }

    // endregion

    // region live validation

    @Test
    fun `typing an invalid-format url surfaces INVALID_FORMAT without a network call`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        type(vm, "not a url")
        advanceUntilIdle()

        assertEquals(ServerUrlValidationError.INVALID_FORMAT, vm.validationError)
        coVerify(exactly = 0) { serverValidationRepository.validate(any()) }
    }

    @Test
    fun `typing a valid reachable url clears the error`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        type(vm, "https://new.example")
        advanceUntilIdle()

        assertNull(vm.validationError)
        coVerify(exactly = 1) { serverValidationRepository.validate("https://new.example") }
    }

    @Test
    fun `typing a valid but unreachable url surfaces the mapped error`() = runTest {
        coEvery { serverValidationRepository.validate(any()) } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val vm = viewModel()
        advanceUntilIdle()

        type(vm, "https://down.example")
        advanceUntilIdle()

        assertEquals(ServerUrlValidationError.UNREACHABLE, vm.validationError)
    }

    @Test
    fun `typing back to the loaded url requires no network call`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        type(vm, loadedUrl)
        advanceUntilIdle()

        assertNull(vm.validationError)
        coVerify(exactly = 0) { serverValidationRepository.validate(any()) }
    }

    // endregion

    // region onAttemptExit — unchanged / valid

    @Test
    fun `exit with no change approves immediately without validating, saving, or reloading`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAttemptExit()
        advanceUntilIdle()

        assertTrue(vm.exitApproved)
        coVerify(exactly = 0) { serverValidationRepository.validate(any()) }
        coVerify(exactly = 0) { settingsRepository.saveServerUrl(any()) }
        coVerify(exactly = 0) { performancesRepository.loadPerformances() }
    }

    @Test
    fun `exit with a changed valid url saves it, reloads performances, and approves exit`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onServerUrlChanged("  https://new.example  ")
        vm.onAttemptExit()
        advanceUntilIdle()

        assertTrue(vm.exitApproved)
        assertFalse(vm.showInvalidExitDialog)
        coVerify(exactly = 1) { settingsRepository.saveServerUrl("https://new.example") }
        coVerify(exactly = 1) { performancesRepository.loadPerformances() }
    }

    @Test
    fun `exit does not re-validate a url the live validator already confirmed`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        type(vm, "https://new.example")
        advanceUntilIdle()
        vm.onAttemptExit()
        advanceUntilIdle()

        // One validation total: the live one; the exit reuses the cached verdict.
        coVerify(exactly = 1) { serverValidationRepository.validate("https://new.example") }
        assertTrue(vm.exitApproved)
    }

    // endregion

    // region onAttemptExit — invalid

    @Test
    fun `exit with a changed invalid url shows the dialog and does not save or reload`() = runTest {
        coEvery { serverValidationRepository.validate(any()) } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onServerUrlChanged("https://down.example")
        vm.onAttemptExit()
        advanceUntilIdle()

        assertTrue(vm.showInvalidExitDialog)
        assertFalse(vm.exitApproved)
        coVerify(exactly = 0) { settingsRepository.saveServerUrl(any()) }
        coVerify(exactly = 0) { performancesRepository.loadPerformances() }
    }

    @Test
    fun `exit with a format-invalid url shows the dialog without a network call`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onServerUrlChanged("garbage")
        vm.onAttemptExit()
        advanceUntilIdle()

        assertTrue(vm.showInvalidExitDialog)
        coVerify(exactly = 0) { serverValidationRepository.validate(any()) }
    }

    // endregion

    // region dialog actions

    @Test
    fun `onKeepEditing hides the dialog and stays`() = runTest {
        coEvery { serverValidationRepository.validate(any()) } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onServerUrlChanged("https://down.example")
        vm.onAttemptExit()
        advanceUntilIdle()

        vm.onKeepEditing()

        assertFalse(vm.showInvalidExitDialog)
        assertFalse(vm.exitApproved)
        assertEquals("https://down.example", vm.serverUrl)
    }

    @Test
    fun `onDiscardChanges reverts to the loaded url, approves exit, and persists nothing`() = runTest {
        coEvery { serverValidationRepository.validate(any()) } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onServerUrlChanged("https://down.example")
        vm.onAttemptExit()
        advanceUntilIdle()

        vm.onDiscardChanges()
        advanceUntilIdle()

        assertEquals(loadedUrl, vm.serverUrl)
        assertFalse(vm.showInvalidExitDialog)
        assertTrue(vm.exitApproved)
        coVerify(exactly = 0) { settingsRepository.saveServerUrl(any()) }
        coVerify(exactly = 0) { performancesRepository.loadPerformances() }
    }

    // endregion
}
