package com.nudgery.android.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.backup.allNudgesBackupFileBase
import com.nudgery.android.settings.ThemePreference
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nudgery.android.settings.EMOJI_SCALE_MAX
import com.nudgery.android.settings.EMOJI_SCALE_MIN
import com.nudgery.android.ui.theme.ChartPalettePreference
import com.nudgery.android.ui.theme.paletteStops
import com.nudgery.shared.emoji.EmojiDefaults
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.PlatformEmojiGlyphFilter
import com.nudgery.shared.emoji.SkinTone
import com.nudgery.android.viewmodel.CollisionResolution
import com.nudgery.android.viewmodel.ImportStatus
import com.nudgery.android.viewmodel.SettingsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val importSuccessMessage = stringResource(R.string.settings_import_success)
    val importStatus = uiState.importStatus
    val importFailureMessage = if (importStatus is ImportStatus.Failure)
        stringResource(R.string.settings_import_failure, importStatus.message)
    else null

    LaunchedEffect(importStatus) {
        when (importStatus) {
            is ImportStatus.BulkSuccess -> {
                val message = if (importStatus.imported == 1 &&
                    importStatus.skipped == 0 && importStatus.failed == 0) {
                    importSuccessMessage
                } else buildString {
                    append(context.getString(R.string.settings_import_all_success, importStatus.imported))
                    if (importStatus.skipped > 0) {
                        append(" ")
                        append(context.getString(R.string.settings_import_skipped, importStatus.skipped))
                    }
                    if (importStatus.failed > 0) {
                        append(" ")
                        append(context.getString(R.string.settings_import_unreadable, importStatus.failed))
                    }
                }
                snackbarHostState.showSnackbar(message)
                viewModel.clearImportStatus()
            }
            is ImportStatus.Failure -> {
                if (importFailureMessage != null) {
                    snackbarHostState.showSnackbar(importFailureMessage)
                }
                viewModel.clearImportStatus()
            }
            else -> Unit
        }
    }

    // When "back up all" has serialized every nudge, zip the per-nudge JSONs and share the archive.
    val backupAllFiles by viewModel.backupAllFiles.collectAsState()
    val backupEmptyMessage = stringResource(R.string.settings_backup_all_empty)
    LaunchedEffect(backupAllFiles) {
        val entries = backupAllFiles ?: return@LaunchedEffect
        if (entries.isEmpty()) {
            snackbarHostState.showSnackbar(backupEmptyMessage)
            viewModel.clearBackupAll()
            return@LaunchedEffect
        }
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val zipFile = File(exportDir, "${allNudgesBackupFileBase(today)}.zip")
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            entries.forEach { entry ->
                zos.putNextEntry(ZipEntry(entry.fileName))
                zos.write(entry.content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        viewModel.clearBackupAll()
    }

    if (importStatus is ImportStatus.Collision) {
        ImportCollisionDialog(
            incomingName = importStatus.incomingName,
            showRepeatForAll = importStatus.hasMore,
            onResolve = { resolution, repeatForAll -> viewModel.resolveCollision(resolution, repeatForAll) },
            // Dismissing (back / tap-outside) skips just this one and continues the batch.
            onDismiss = { viewModel.resolveCollision(CollisionResolution.SKIP, repeatForAll = false) }
        )
    }

    // Accepts either a single-nudge JSON backup or an all-nudges ZIP; the file content decides which.
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        if (looksLikeZip(bytes)) {
            viewModel.importAllFromBackups(readJsonEntriesFromZip(bytes))
        } else {
            viewModel.importNudgeFromBackup(bytes.toString(Charsets.UTF_8))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val systemIsDark = isSystemInDarkTheme()
        val isDark = when (uiState.themePreference) {
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
            ThemePreference.SYSTEM -> systemIsDark
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionLabel(stringResource(R.string.settings_theme))

            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    label = stringResource(R.string.settings_theme_system),
                    selected = uiState.themePreference == ThemePreference.SYSTEM,
                    onSelect = { viewModel.setTheme(ThemePreference.SYSTEM) }
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = uiState.themePreference == ThemePreference.LIGHT,
                    onSelect = { viewModel.setTheme(ThemePreference.LIGHT) }
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = uiState.themePreference == ThemePreference.DARK,
                    onSelect = { viewModel.setTheme(ThemePreference.DARK) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionLabel(stringResource(R.string.settings_charts))

            Column(modifier = Modifier.selectableGroup()) {
                PaletteOption(
                    label = stringResource(R.string.settings_palette_spectrum),
                    description = stringResource(R.string.settings_palette_spectrum_description),
                    palette = ChartPalettePreference.SPECTRUM,
                    selected = uiState.chartPalette == ChartPalettePreference.SPECTRUM,
                    isDark = isDark,
                    onSelect = { viewModel.setChartPalette(ChartPalettePreference.SPECTRUM) }
                )
                PaletteOption(
                    label = stringResource(R.string.settings_palette_horizon),
                    description = stringResource(R.string.settings_palette_horizon_description),
                    palette = ChartPalettePreference.HORIZON,
                    selected = uiState.chartPalette == ChartPalettePreference.HORIZON,
                    isDark = isDark,
                    onSelect = { viewModel.setChartPalette(ChartPalettePreference.HORIZON) }
                )
                PaletteOption(
                    label = stringResource(R.string.settings_palette_ember),
                    description = stringResource(R.string.settings_palette_ember_description),
                    palette = ChartPalettePreference.EMBER,
                    selected = uiState.chartPalette == ChartPalettePreference.EMBER,
                    isDark = isDark,
                    onSelect = { viewModel.setChartPalette(ChartPalettePreference.EMBER) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionLabel(stringResource(R.string.settings_emoji_defaults))
            EmojiDefaultSelectors(
                skinTone = uiState.defaultEmojiSkinTone,
                gender = uiState.defaultEmojiGender,
                emojiScale = uiState.emojiScale,
                onSkinTone = viewModel::setDefaultEmojiSkinTone,
                onGender = viewModel::setDefaultEmojiGender,
                onEmojiScale = viewModel::setEmojiScale,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_bold_text),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_bold_text_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.boldText,
                    onCheckedChange = { viewModel.setBoldText(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ExactAlarmDiagnosticRow()

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionLabel(stringResource(R.string.settings_import))

            val isImporting = uiState.importStatus is ImportStatus.InProgress
            TextButton(
                onClick = { viewModel.exportAllNudges() },
                enabled = backupAllFiles == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_backup_all_button),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(
                onClick = { filePicker.launch("application/*") },
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = if (isImporting)
                        stringResource(R.string.settings_import_in_progress)
                    else
                        stringResource(R.string.settings_import_button),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TextButton(
                onClick = onAboutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_about),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExactAlarmDiagnosticRow() {
    val context = LocalContext.current
    val granted = rememberExactAlarmGranted()

    SettingsSectionLabel(stringResource(R.string.settings_diagnostics))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_exact_alarm),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (granted)
                    stringResource(R.string.settings_exact_alarm_granted)
                else
                    stringResource(R.string.settings_exact_alarm_not_granted),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TextButton(onClick = { openExactAlarmSettings(context) }) {
                Text(stringResource(R.string.settings_exact_alarm_open_settings))
            }
        }
    }
}

@Composable
private fun ImportCollisionDialog(
    incomingName: String,
    showRepeatForAll: Boolean,
    onResolve: (CollisionResolution, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var repeatForAll by remember(incomingName) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_collision_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.import_collision_body, incomingName))
                if (showRepeatForAll) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { repeatForAll = !repeatForAll }
                    ) {
                        Checkbox(checked = repeatForAll, onCheckedChange = { repeatForAll = it })
                        Text(stringResource(R.string.import_collision_repeat_all))
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onResolve(CollisionResolution.REPLACE, repeatForAll) }) {
                    Text(stringResource(R.string.import_collision_replace))
                }
                TextButton(onClick = { onResolve(CollisionResolution.COPY, repeatForAll) }) {
                    Text(stringResource(R.string.import_collision_copy))
                }
                TextButton(onClick = { onResolve(CollisionResolution.SKIP, repeatForAll) }) {
                    Text(stringResource(R.string.import_collision_skip))
                }
            }
        }
    )
}

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun PaletteOption(
    label: String,
    description: String,
    palette: ChartPalettePreference,
    selected: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        val stops = palette.paletteStops.run { if (isDark) darkStops else lightStops }
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(12.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Brush.horizontalGradient(stops))
        )
    }
}

/** True if [bytes] begin with the ZIP local-file-header magic ("PK"). */
private fun looksLikeZip(bytes: ByteArray): Boolean =
    bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

/** Reads the UTF-8 contents of every ".json" entry in a ZIP backup. */
private fun readJsonEntriesFromZip(bytes: ByteArray): List<String> {
    val contents = mutableListOf<String>()
    ZipInputStream(bytes.inputStream()).use { zis ->
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                contents.add(zis.readBytes().toString(Charsets.UTF_8))
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    return contents
}

/**
 * Default skin-tone (ED-6) and gender (ED-7) selectors, shown as swatches of the actual emoji
 * variants — the convention both Android and iOS keyboards use. The Fitzpatrick/gender names are
 * not shown as labels (matching those keyboards) but are exposed as content descriptions for
 * screen readers, mirroring TalkBack/VoiceOver.
 */
@Composable
private fun EmojiDefaultSelectors(
    skinTone: SkinTone,
    gender: Gender,
    emojiScale: Float,
    onSkinTone: (SkinTone) -> Unit,
    onGender: (Gender) -> Unit,
    onEmojiScale: (Float) -> Unit,
) {
    val handSample = "🖐️" // 🖐️ — a skin-tone-capable sample
    val personSample = "🧑"     // 🧑 — a genderable sample

    val tones = listOf(
        SkinTone.DEFAULT to R.string.settings_skin_tone_default,
        SkinTone.LIGHT to R.string.settings_skin_tone_light,
        SkinTone.MEDIUM_LIGHT to R.string.settings_skin_tone_medium_light,
        SkinTone.MEDIUM to R.string.settings_skin_tone_medium,
        SkinTone.MEDIUM_DARK to R.string.settings_skin_tone_medium_dark,
        SkinTone.DARK to R.string.settings_skin_tone_dark,
    )
    val genders = listOf(
        Gender.NEUTRAL to R.string.settings_gender_neutral,
        Gender.WOMAN to R.string.settings_gender_woman,
        Gender.MAN to R.string.settings_gender_man,
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.settings_emoji_skin_tone),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.selectableGroup().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tones.forEach { (tone, nameRes) ->
                EmojiSwatch(
                    emoji = EmojiDefaults.applySkinTone(handSample, tone),
                    contentDescription = stringResource(nameRes),
                    selected = skinTone == tone,
                    onSelect = { onSkinTone(tone) }
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_emoji_gender),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genders.forEach { (g, nameRes) ->
                EmojiSwatch(
                    emoji = EmojiDefaults.applyGender(personSample, g),
                    contentDescription = stringResource(nameRes),
                    selected = gender == g,
                    onSelect = { onGender(g) }
                )
            }
        }

        // Emoji size (ED-14): a slider with a live sample emoji from a small "fun" shortlist.
        Text(
            text = stringResource(R.string.settings_emoji_size),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        var sliderValue by remember(emojiScale) { mutableFloatStateOf(emojiScale) }
        // Fun "size" sample: wild animals (no typical urban pets, nothing with political overtones),
        // filtered to what this device can actually render so it never shows tofu.
        val sample = remember {
            val candidates = listOf(
                "🦒", "🦓", "🦏", "🦘", "🦥", "🦦", "🐧", "🦉", "🦩", "🐙", "🐳",
                "🦚", "🦔", "🐉", "🦄", "🐢"
            )
            val glyphFilter = PlatformEmojiGlyphFilter()
            candidates.filter { glyphFilter.canRender(it) }.ifEmpty { candidates }.random()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(text = sample, fontSize = (20 * sliderValue).sp)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it; onEmojiScale(it) },
                valueRange = EMOJI_SCALE_MIN..EMOJI_SCALE_MAX,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmojiSwatch(
    emoji: String,
    contentDescription: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(if (selected) 2.dp else 1.dp, borderColor, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}
