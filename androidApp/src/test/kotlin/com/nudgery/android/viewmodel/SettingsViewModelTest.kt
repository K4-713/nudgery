// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.SkinTone
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.usecase.QuestionValidationProblem
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

    /** A backup whose follow-up (orderIndex 1) has no trigger condition — invalid per ED-26. */
    private fun invalidBackupJson(name: String) = """
        {
          "nudge": { "name": "$name", "isEnabled": true },
          "schedule": { "type": "DAILY", "timeOfDay": "09:00", "activeDaysOfWeek": ["MONDAY"] },
          "questions": [
            { "orderIndex": 0, "text": "Did it happen?", "type": "YES_NO" },
            { "orderIndex": 1, "text": "Why?", "type": "TEXT" }
          ],
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

    private suspend fun nudgeNames() = repos.nudgeRepo.observeAll().first().map { it.name }

    @Test
    fun TDD_setEmojiDefaults_persistAndSurfaceInState() = runTest {
        // ED-6/ED-7: choosing the default skin tone and gender persists and shows in settings state.
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(SkinTone.DEFAULT, viewModel.uiState.value.defaultEmojiSkinTone)
        assertEquals(Gender.NEUTRAL, viewModel.uiState.value.defaultEmojiGender)

        viewModel.setDefaultEmojiSkinTone(SkinTone.MEDIUM)
        viewModel.setDefaultEmojiGender(Gender.WOMAN)
        viewModel.setEmojiScale(2.0f)
        advanceUntilIdle()

        assertEquals(SkinTone.MEDIUM, viewModel.uiState.value.defaultEmojiSkinTone)
        assertEquals(Gender.WOMAN, viewModel.uiState.value.defaultEmojiGender)
        assertEquals(2.0f, viewModel.uiState.value.emojiScale)
    }

    @Test
    fun TDD_import_whenNoNameConflict_succeeds() = runTest {
        // Settings: importing a backup with a unique name completes immediately
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.importNudgeFromBackup(backupJson("Brand New Nudge"))
        advanceUntilIdle()

        val status = viewModel.uiState.value.importStatus
        assertTrue("Expected BulkSuccess but got $status", status is ImportStatus.BulkSuccess)
        assertEquals(1, (status as ImportStatus.BulkSuccess).imported)
        assertTrue(nudgeNames().contains("Brand New Nudge"))
    }

    @Test
    fun TDD_import_singleInvalidBackup_promptsNeedsFix() = runTest {
        // ED-26: a single backup with a setup problem (here a follow-up with no trigger) pauses for
        // the user to cancel or fix, rather than importing silently.
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.importNudgeFromBackup(invalidBackupJson("Broken"))
        advanceUntilIdle()

        val status = viewModel.uiState.value.importStatus
        assertTrue("Expected NeedsFix but got $status", status is ImportStatus.NeedsFix)
        assertTrue((status as ImportStatus.NeedsFix).problem is QuestionValidationProblem.MissingFollowUpTrigger)
        assertTrue("Nothing imported yet", nudgeNames().isEmpty())
    }

    @Test
    fun TDD_fixInvalidImport_importsThenSignalsEditorNavigation() = runTest {
        // Fix imports the nudge as-is (answers and all) and signals the editor to open at the step
        // where the problem can be corrected — the follow-ups step for a missing trigger.
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.importNudgeFromBackup(invalidBackupJson("Imported"))
        advanceUntilIdle()

        viewModel.fixInvalidImport()
        advanceUntilIdle()

        assertTrue("Nudge should be imported as-is", nudgeNames().contains("Imported"))
        val nav = viewModel.fixNavigation.value
        assertNotNull("A fix navigation should be emitted", nav)
        assertEquals("A missing trigger routes to the follow-ups step", 1, nav!!.editStep)
    }

    @Test
    fun TDD_cancelInvalidImport_importsNothing() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.importNudgeFromBackup(invalidBackupJson("Nope"))
        advanceUntilIdle()

        viewModel.cancelInvalidImport()
        advanceUntilIdle()

        assertTrue("Nothing should be imported", nudgeNames().isEmpty())
        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Idle)
        assertNull(viewModel.fixNavigation.value)
    }

    @Test
    fun TDD_import_whenNameAlreadyExists_promptsCollision() = runTest {
        // "ask every time a nudge is imported with a name collision"
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")

        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        val status = viewModel.uiState.value.importStatus
        assertTrue("Expected Collision but got $status", status is ImportStatus.Collision)
        assertEquals("Exercise", (status as ImportStatus.Collision).incomingName)
        assertTrue("Single import has nothing more to repeat over", !status.hasMore)
        // Nothing persisted while awaiting the decision
        assertEquals(1, repos.nudgeRepo.observeAll().first().size)
    }

    @Test
    fun TDD_collision_replace_removesExistingAndKeepsName() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val existing = insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.resolveCollision(CollisionResolution.REPLACE, repeatForAll = false)
        advanceUntilIdle()

        assertNull("Existing nudge should be replaced", repos.nudgeRepo.getById(existing.id))
        assertEquals(listOf("Exercise"), nudgeNames())
        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.BulkSuccess)
    }

    @Test
    fun TDD_collision_copy_importsAsRenamedCopy() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.resolveCollision(CollisionResolution.COPY, repeatForAll = false)
        advanceUntilIdle()

        val names = nudgeNames()
        assertTrue("Original kept", names.contains("Exercise"))
        assertTrue("Copy added with disambiguated name", names.contains("Exercise (2)"))
    }

    @Test
    fun TDD_collision_skip_importsNothing() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Exercise")
        viewModel.importNudgeFromBackup(backupJson("Exercise"))
        advanceUntilIdle()

        viewModel.resolveCollision(CollisionResolution.SKIP, repeatForAll = false)
        advanceUntilIdle()

        assertEquals(listOf("Exercise"), nudgeNames())
        val status = viewModel.uiState.value.importStatus as ImportStatus.BulkSuccess
        assertEquals(0, status.imported)
        assertEquals(1, status.skipped)
    }

    @Test
    fun TDD_batchImport_promptsForEachCollisionSeparately() = runTest {
        // "it should happen for each one if it's a batch import"
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Alpha")
        insertNudgeNamed("Beta")

        viewModel.importAllFromBackups(listOf(backupJson("Alpha"), backupJson("Beta")))
        advanceUntilIdle()

        // First collision (Alpha), more remain
        val first = viewModel.uiState.value.importStatus as ImportStatus.Collision
        assertEquals("Alpha", first.incomingName)
        assertTrue("More collisions remain in the batch", first.hasMore)

        viewModel.resolveCollision(CollisionResolution.SKIP, repeatForAll = false)
        advanceUntilIdle()

        // Second collision (Beta) is then prompted on its own
        val second = viewModel.uiState.value.importStatus as ImportStatus.Collision
        assertEquals("Beta", second.incomingName)
        assertTrue("Last item, nothing more", !second.hasMore)
    }

    @Test
    fun TDD_batchImport_repeatForAll_appliesChoiceToRemaining() = runTest {
        // "a checkbox for 'Repeat for all'"
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Alpha")
        insertNudgeNamed("Beta")

        viewModel.importAllFromBackups(listOf(backupJson("Alpha"), backupJson("Beta")))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Collision)

        viewModel.resolveCollision(CollisionResolution.COPY, repeatForAll = true)
        advanceUntilIdle()

        // No second prompt; both imported as copies
        val status = viewModel.uiState.value.importStatus
        assertTrue("Should finish without a second prompt, got $status", status is ImportStatus.BulkSuccess)
        val names = nudgeNames()
        assertTrue(names.contains("Alpha (2)"))
        assertTrue(names.contains("Beta (2)"))
        assertEquals(2, (status as ImportStatus.BulkSuccess).imported)
    }

    @Test
    fun TDD_batchImport_nonCollidingEntriesImportWithoutPrompting() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        insertNudgeNamed("Alpha")

        viewModel.importAllFromBackups(listOf(backupJson("Alpha"), backupJson("Carol")))
        advanceUntilIdle()

        // Only the colliding Alpha prompts; Carol will import once we skip Alpha
        assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Collision)
        viewModel.resolveCollision(CollisionResolution.SKIP, repeatForAll = false)
        advanceUntilIdle()

        assertTrue("New, non-colliding nudge imported", nudgeNames().contains("Carol"))
    }
}
