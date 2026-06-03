package com.nudgery.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.nudgery.android.R

/**
 * Open-source licenses screen. The AboutLibraries Gradle plugin generates the dependency + license
 * data into `R.raw.aboutlibraries` at build time (including the manually-declared bundled font); we
 * parse it with aboutlibraries-core and render it here in the app's own style, rather than pulling
 * in the library's Compose-Multiplatform UI module (which would conflict with our AndroidX Compose).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val libraries = remember {
        val json = context.resources.openRawResource(R.raw.aboutlibraries)
            .bufferedReader().use { it.readText() }
        Libs.Builder().withJson(json).build().libraries.sortedBy { it.name.lowercase() }
    }

    // The library whose license text is currently shown in a dialog, or null when none is open.
    var shownLibrary by remember { mutableStateOf<Library?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(libraries, key = { it.uniqueId }) { library ->
                LicenseRow(library = library, onClick = { shownLibrary = library })
                HorizontalDivider()
            }
        }
    }

    shownLibrary?.let { library ->
        LicenseDialog(library = library, onDismiss = { shownLibrary = null })
    }
}

@Composable
private fun LicenseRow(library: Library, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        val title = library.artifactVersion?.let { "${library.name} $it" } ?: library.name
        Text(text = title, style = MaterialTheme.typography.bodyLarge)

        val authors = library.developers.mapNotNull { it.name }.joinToString(", ")
        if (authors.isNotBlank()) {
            Text(
                text = authors,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val licenseNames = library.licenses.joinToString(", ") { it.name }
        if (licenseNames.isNotBlank()) {
            Text(
                text = licenseNames,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LicenseDialog(library: Library, onDismiss: () -> Unit) {
    // Show every license attached to the library; fall back to name + URL when no full text shipped.
    val body = library.licenses.joinToString("\n\n\n") { license ->
        license.licenseContent?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(license.name, license.url).joinToString("\n")
    }.ifBlank { stringResource(R.string.licenses_no_text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(library.name) },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.licenses_close)) }
        }
    )
}
