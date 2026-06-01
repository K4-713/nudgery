package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.android.backup.NudgeBackupParser
import com.nudgery.android.backup.disambiguateName
import com.nudgery.android.backup.nudgeBackupFileName
import com.nudgery.android.settings.AppSettings
import com.nudgery.android.settings.ThemePreference
import com.nudgery.android.ui.theme.ChartPalettePreference
import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.usecase.DeleteNudgeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.ImportNudgeRequest
import com.nudgery.shared.usecase.ImportNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data object InProgress : ImportStatus()
    /** Import paused on a name collision, awaiting the user's choice. */
    data class Collision(val incomingName: String, val hasMore: Boolean) : ImportStatus()
    /** Terminal summary for any import (single or batch). */
    data class BulkSuccess(val imported: Int, val skipped: Int, val failed: Int) : ImportStatus()
    data class Failure(val message: String) : ImportStatus()
}

/** How the user chose to resolve an import name collision. */
enum class CollisionResolution { REPLACE, COPY, SKIP }

/** One nudge serialized for an all-nudges backup: a per-nudge filename and its JSON content. */
data class BackupEntry(val fileName: String, val content: String)

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val boldText: Boolean = false,
    val chartPalette: ChartPalettePreference = ChartPalettePreference.SPECTRUM,
    val importStatus: ImportStatus = ImportStatus.Idle
)

class SettingsViewModel(
    private val appSettings: AppSettings,
    private val importNudge: ImportNudgeUseCase,
    private val deleteNudge: DeleteNudgeUseCase,
    private val nudgeRepository: NudgeRepository,
    private val backupParser: NudgeBackupParser,
    private val exportAnswers: ExportAnswersUseCase
) : ViewModel() {

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)

    // Emitted when "back up all" has serialized every nudge; the screen zips and shares the result.
    // null = idle, empty list = there were no nudges to back up.
    private val _backupAllFiles = MutableStateFlow<List<BackupEntry>?>(null)
    val backupAllFiles: StateFlow<List<BackupEntry>?> = _backupAllFiles.asStateFlow()

    // In-flight import; null when no import is running. Mutated only on the main dispatcher.
    private var importSession: ImportSession? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.themePreference,
        appSettings.boldText,
        appSettings.chartPalette,
        _importStatus
    ) { theme, bold, palette, importStatus ->
        SettingsUiState(theme, bold, palette, importStatus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(pref: ThemePreference) {
        viewModelScope.launch { appSettings.setThemePreference(pref) }
    }

    fun setBoldText(bold: Boolean) {
        viewModelScope.launch { appSettings.setBoldText(bold) }
    }

    fun setChartPalette(palette: ChartPalettePreference) {
        viewModelScope.launch { appSettings.setChartPalette(palette) }
    }

    fun importNudgeFromBackup(jsonContent: String) = startImport(listOf(jsonContent))

    /**
     * Imports nudges from the JSON contents of one or more backups (a single JSON or every entry in
     * a backup ZIP). Each name collision pauses for the user to Replace / Import as copy / Skip; the
     * choice can be repeated for all remaining collisions in the batch.
     */
    fun importAllFromBackups(jsonContents: List<String>) = startImport(jsonContents)

    private fun startImport(jsonContents: List<String>) {
        _importStatus.update { ImportStatus.InProgress }
        viewModelScope.launch {
            val takenNames = nudgeRepository.observeAll().first().mapTo(mutableSetOf()) { it.name }
            val requests = ArrayDeque<ImportNudgeRequest>()
            var failed = 0
            var firstFailureMessage: String? = null
            jsonContents.forEach { json ->
                when (val parsed = backupParser.parse(json)) {
                    is NudgeBackupParser.ParseResult.Failure -> {
                        failed++
                        if (firstFailureMessage == null) firstFailureMessage = parsed.message
                        Log.w(TAG, "Skipping unreadable backup: ${parsed.message}")
                    }
                    is NudgeBackupParser.ParseResult.Success -> requests.add(parsed.request)
                }
            }
            if (requests.isEmpty()) {
                // A single unreadable file gets the specific parse error; otherwise just summarize.
                val message = firstFailureMessage
                _importStatus.update {
                    if (jsonContents.size == 1 && message != null) ImportStatus.Failure(message)
                    else ImportStatus.BulkSuccess(imported = 0, skipped = 0, failed = failed)
                }
                return@launch
            }
            importSession = ImportSession(remaining = requests, takenNames = takenNames, failed = failed)
            processNextImport()
        }
    }

    /** Resolves the currently-shown collision, optionally applying the same choice to the rest. */
    fun resolveCollision(resolution: CollisionResolution, repeatForAll: Boolean) {
        val session = importSession ?: return
        val request = session.pending ?: return
        session.pending = null
        if (repeatForAll) session.applyToAll = resolution
        _importStatus.update { ImportStatus.InProgress }
        viewModelScope.launch {
            applyResolution(resolution, request, session)
            processNextImport()
        }
    }

    /** Imports each remaining nudge, pausing whenever a name collision needs a user decision. */
    private suspend fun processNextImport() {
        val session = importSession ?: return
        while (session.remaining.isNotEmpty()) {
            val request = session.remaining.removeFirst()
            if (request.name !in session.takenNames) {
                importNudge.execute(request)
                session.takenNames.add(request.name)
                session.imported++
                continue
            }
            val standingChoice = session.applyToAll
            if (standingChoice != null) {
                applyResolution(standingChoice, request, session)
                continue
            }
            session.pending = request
            _importStatus.update {
                ImportStatus.Collision(request.name, hasMore = session.remaining.isNotEmpty())
            }
            return
        }
        Log.i(TAG, "Import complete: ${session.imported} imported, ${session.skipped} skipped, ${session.failed} failed")
        _importStatus.update { ImportStatus.BulkSuccess(session.imported, session.skipped, session.failed) }
        importSession = null
    }

    private suspend fun applyResolution(
        resolution: CollisionResolution,
        request: ImportNudgeRequest,
        session: ImportSession
    ) {
        when (resolution) {
            CollisionResolution.REPLACE -> {
                nudgeRepository.observeAll().first()
                    .firstOrNull { it.name == request.name }
                    ?.let { deleteNudge.execute(it.id) }
                importNudge.execute(request)
                session.takenNames.add(request.name)
                session.imported++
            }
            CollisionResolution.COPY -> {
                val newName = disambiguateName(request.name, session.takenNames)
                importNudge.execute(request.copy(name = newName))
                session.takenNames.add(newName)
                session.imported++
            }
            CollisionResolution.SKIP -> session.skipped++
        }
    }

    /** Serializes every nudge to its own JSON, named after the nudge, for an all-nudges ZIP backup. */
    fun exportAllNudges() {
        viewModelScope.launch {
            val nudges = nudgeRepository.observeAll().first()
            val usedFileNames = mutableSetOf<String>()
            val entries = nudges.map { nudge ->
                val base = disambiguateName(nudgeBackupFileName(nudge.name), usedFileNames)
                usedFileNames.add(base)
                BackupEntry(fileName = "$base.json", content = exportAnswers.execute(nudge.id, ExportFormat.JSON))
            }
            Log.i(TAG, "Prepared all-nudges backup with ${entries.size} nudge(s)")
            _backupAllFiles.update { entries }
        }
    }

    fun clearBackupAll() {
        _backupAllFiles.update { null }
    }

    fun clearImportStatus() {
        _importStatus.update { ImportStatus.Idle }
    }

    /** In-flight state for an import that may pause on collisions. */
    private class ImportSession(
        val remaining: ArrayDeque<ImportNudgeRequest>,
        val takenNames: MutableSet<String>,
        val failed: Int,
        var imported: Int = 0,
        var skipped: Int = 0,
        var applyToAll: CollisionResolution? = null,
        var pending: ImportNudgeRequest? = null
    )
}
