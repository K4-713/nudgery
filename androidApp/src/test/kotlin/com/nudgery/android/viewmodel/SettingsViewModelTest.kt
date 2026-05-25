package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.model.Nudge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repos: TestViewModelRepositories
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repos = TestViewModelRepositories()
        viewModel = repos.settingsViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun backupJson(name: String) = """
        {
          "nudge": { "name": "$name", "isEnabled": true },
          "schedule": { "type": "DAILY", "timeOfDay": "09:00", "activeDaysOfWeek": ["MONDAY"] },
          "questions": [{ "orderIndex": 0, "text": "Test question?", "type": "YES_NO" }],
          "answers": []
        }
    """.trimIndent()

    private suspend fun insertNudgeNamed(name: String): Nudge {
        val nudge = Nudge(
            id = "existing-$name",
            name = name,
            isEnabled = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        repos.nudgeRepo.insert(nudge)
        return nudge
    }

    @Test
    fun TDD_import_whenNoNameConflict_succeeds() = runTest {
        // Settings: importing a backup with a unique name completes immediately
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.importNudgeFromBackup(backupJson("Brand New Nudge"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Success)
    }

    @Test
    fun TDD_import_whenNameAlreadyExists_showsCollisionState() = runTest {
        // Settings: if a nudge with the same name exists, import pauses for user confirmation
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")

        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        val status = viewModel.uiState.value.importStatus
        assertTrue("Expected NameCollision but got $status", status is ImportStatus.NameCollision)
    }

    @Test
    fun TDD_import_collision_holdsIncomingNameInState() = runTest {
        // The collision state must carry the original name so the dialog can display it
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")

        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        val status = viewModel.uiState.value.importStatus as ImportStatus.NameCollision
        assertEquals("Exercise", status.pendingRequest.name)
    }

    @Test
    fun TDD_import_collision_doesNotCreateNudgeYet() = runTest {
        // No nudge should be persisted while the collision dialog is pending
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")

        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        assertEquals(1, repos.nudgeRepo.observeAll().first().size)
    }

    @Test
    fun TDD_import_rename_importsNudgeWithNewName() = runTest {
        // Settings: confirming a rename imports the nudge under the new name
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.confirmImportRename("Exercise (copy)")
        advanceUntilIdle()

        val imported = repos.nudgeRepo.observeAll().first().find { it.name == "Exercise (copy)" }
        assertNotNull("Nudge with new name should exist", imported)
    }

    @Test
    fun TDD_import_rename_statusBecomesSuccess() = runTest {
        // After rename confirmation, status should transition to Success
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.confirmImportRename("Exercise (copy)")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Success)
    }

    @Test
    fun TDD_import_replace_deletesExistingNudge() = runTest {
        // Settings: confirming replace removes the existing nudge
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val existing = insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.confirmImportReplace()
        advanceUntilIdle()

        assertNull(repos.nudgeRepo.getById(existing.id))
    }

    @Test
    fun TDD_import_replace_createsNudgeWithOriginalName() = runTest {
        // Settings: replacing keeps the original name on the newly imported nudge
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.confirmImportReplace()
        advanceUntilIdle()

        val imported = repos.nudgeRepo.observeAll().first().find { it.name == "Exercise" }
        assertNotNull("Replaced nudge should exist with original name", imported)
    }

    @Test
    fun TDD_import_replace_statusBecomesSuccess() = runTest {
        // After replace confirmation, status should transition to Success
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.confirmImportReplace()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Success)
    }
}
