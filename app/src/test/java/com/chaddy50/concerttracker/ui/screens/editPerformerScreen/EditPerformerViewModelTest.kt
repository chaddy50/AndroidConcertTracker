package com.chaddy50.concerttracker.ui.screens.editPerformerScreen

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.navigation.routes.PerformerUpdatedResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPerformerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val performersRepository: PerformersRepository = mockk()

    private fun performer(
        id: String = "pe1",
        name: String = "Yo-Yo Ma",
        type: PerformerType = PerformerType.SOLO,
        specialty: String? = "Cellist",
        musicbrainzId: String? = "mb1"
    ) = Performer(id, name, type, specialty, musicbrainzId)

    private fun viewModel() = EditPerformerViewModel(
        SavedStateHandle(mapOf("performerId" to "pe1")), performersRepository
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
    fun `load populates drafts to Ready on Success`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(PerformerEditUiState.Ready, viewModel.uiState)
        assertEquals("Yo-Yo Ma", viewModel.draftName)
        assertEquals(PerformerType.SOLO, viewModel.draftType)
        assertEquals("Cellist", viewModel.draftSpecialty)
    }

    @Test
    fun `load shows NotFound when the performer is missing`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns null
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(PerformerEditUiState.NotFound, viewModel.uiState)
    }

    @Test
    fun `initial state is Loading before the load completes`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        assertEquals(PerformerEditUiState.Loading, viewModel.uiState)
    }

    @Test
    fun `isNameEditable starts false`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()
        assertFalse(viewModel.isNameEditable)
    }

    @Test
    fun `load seeds a null specialty`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer(specialty = null)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertNull(viewModel.draftSpecialty)
    }

    @Test
    fun `enableNameEditing flips isNameEditable to true`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.enableNameEditing()
        assertTrue(viewModel.isNameEditable)
    }

    @Test
    fun `updateDraftName updates the name`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftName("Corrected Name")
        assertEquals("Corrected Name", viewModel.draftName)
    }

    @Test
    fun `updateDraftType updates the type`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftType(PerformerType.CONDUCTOR)
        assertEquals(PerformerType.CONDUCTOR, viewModel.draftType)
    }

    @Test
    fun `updateDraftSpecialty updates and blanks to null`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftSpecialty("Conductor")
        assertEquals("Conductor", viewModel.draftSpecialty)

        viewModel.updateDraftSpecialty("")
        assertNull(viewModel.draftSpecialty)
    }

    @Test
    fun `canSave is true when Ready and name is non-blank`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.canSave)
    }

    @Test
    fun `canSave is false when the name is blank`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.enableNameEditing()
        viewModel.updateDraftName("")
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `canSave is false when not Ready`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns null
        val viewModel = viewModel()
        advanceUntilIdle()
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `savePerformer invokes onSaved with the corrected fields on Success`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        coEvery { performersRepository.updatePerformer(any(), any(), any(), any(), any()) } returns
            ApiResult.Success(performer(type = PerformerType.CONDUCTOR, specialty = "Conductor"))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.updateDraftType(PerformerType.CONDUCTOR)
        viewModel.updateDraftSpecialty("Conductor")

        var result: PerformerUpdatedResult? = null
        viewModel.savePerformer { result = it }
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals("pe1", result!!.id)
        assertEquals("CONDUCTOR", result!!.type)
        assertEquals("Conductor", result!!.specialty)
        assertNull(viewModel.saveError)
        assertFalse(viewModel.isSaving)
    }

    @Test
    fun `savePerformer passes the current drafts including an edited name and preserved musicbrainzId`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        coEvery { performersRepository.updatePerformer(any(), any(), any(), any(), any()) } returns
            ApiResult.Success(performer())
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.enableNameEditing()
        viewModel.updateDraftName("New Name")
        viewModel.updateDraftType(PerformerType.CONDUCTOR)
        viewModel.updateDraftSpecialty("Conductor")

        viewModel.savePerformer {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            performersRepository.updatePerformer("pe1", "New Name", PerformerType.CONDUCTOR, "Conductor", "mb1")
        }
    }

    @Test
    fun `savePerformer sets saveError and does not invoke onSaved on Error`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        coEvery { performersRepository.updatePerformer(any(), any(), any(), any(), any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = viewModel()
        advanceUntilIdle()

        var saved = false
        viewModel.savePerformer { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertNotNull(viewModel.saveError)
        assertFalse(viewModel.isSaving)
    }

    @Test
    fun `savePerformer sets isSaving while the save is in flight`() = runTest {
        coEvery { performersRepository.getPerformer("pe1") } returns performer()
        coEvery { performersRepository.updatePerformer(any(), any(), any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1_000)
            ApiResult.Success(performer())
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.savePerformer {}
        runCurrent() // start the coroutine up to the suspended repository call
        assertTrue(viewModel.isSaving)
        advanceUntilIdle()
        assertFalse(viewModel.isSaving)
    }
}
