// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.EmojiPeople
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import com.nudgery.android.R
import com.nudgery.android.ui.theme.GhostText
import com.nudgery.android.ui.theme.LocalEmojiScale
import com.nudgery.android.ui.theme.raisedSurfaceColor
import com.nudgery.shared.emoji.EmojiCatalogEntry
import com.nudgery.shared.emoji.EmojiDefaults
import com.nudgery.shared.emoji.EmojiSearch
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.EmojiCatalog
import com.nudgery.shared.emoji.PlatformEmojiGlyphFilter
import com.nudgery.shared.emoji.SkinTone

/** Unicode groups shown as picker tabs, in display order, each with an outline (wireframe) icon in
 *  the app's icon style. The "Component" group (skin tones, hair) is omitted. */
private val CATEGORY_TABS: List<Pair<String, ImageVector>> = listOf(
    "Smileys & Emotion" to Icons.Outlined.Mood,
    "People & Body" to Icons.Outlined.EmojiPeople,
    "Animals & Nature" to Icons.Outlined.Pets,
    "Food & Drink" to Icons.Outlined.Restaurant,
    "Travel & Places" to Icons.Outlined.DirectionsCar,
    "Activities" to Icons.Outlined.Celebration,
    "Objects" to Icons.Outlined.EmojiObjects,
    "Symbols" to Icons.Outlined.EmojiSymbols,
    "Flags" to Icons.Outlined.Flag,
)

/**
 * Inline, always-open emoji picker (ENGINEERING_DECISIONS.md ED-13): search field, top category
 * tabs (Recents first, under a clock), and a device-font grid (ED-3) filtered to what the device can
 * render (ED-4). A tap applies the user's default skin tone/gender (ED-6/ED-7) and calls [onPick];
 * the picker stays open so the user can build a multi-emoji answer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(
    recents: List<String>,
    defaultSkinTone: SkinTone,
    defaultGender: Gender,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One-time: keep only emoji this device can render, grouped by Unicode group (ED-4). The same
    // filter also measures glyph width, so multi-person emoji that render double-wide can be laid out
    // without overlapping (see the grid's span, below).
    val glyphFilter = remember { PlatformEmojiGlyphFilter() }
    val renderable = remember(glyphFilter) {
        EmojiCatalog.entries.filter { glyphFilter.canRender(it.emoji) }
    }
    // Fold each genderable concept's woman/man forms into its neutral entry (ED-7): they're offered
    // on long-press, not as their own cells, so the grid shows one cell per concept (in the user's
    // default gender) rather than the neutral + every gendered duplicate.
    val displayEntries = remember(renderable) {
        val folded = EmojiDefaults.foldedGenderVariantEmoji(renderable)
        renderable.filterNot { it.emoji in folded }
    }
    val byGroup = remember(displayEntries) { displayEntries.groupBy { it.group } }
    val recentsLabel = stringResource(R.string.emoji_recents_tab)
    val tabs = remember(byGroup, recentsLabel) {
        listOf(EmojiTab(Icons.Outlined.Schedule, recentsLabel, entries = null)) +
            CATEGORY_TABS.mapNotNull { (group, icon) ->
                byGroup[group]?.let { EmojiTab(icon, group, it) }
            }
    }

    // Open on Recents if the user has any; otherwise start on the first content category
    // (Smileys & Emotion), matching standard Android/iOS keyboard behavior.
    var selectedTab by remember { mutableIntStateOf(if (recents.isNotEmpty()) 0 else 1) }
    var query by remember { mutableStateOf("") }
    val emojiScale = LocalEmojiScale.current // ED-14: emoji surfaces honor the global scale

    // Picking an emoji applies it and then dismisses the search keyboard so the whole answer screen
    // (chosen-emoji display, Save/Cancel) is visible again — otherwise a tap during search silently
    // fills the answer behind the keyboard. The search text is left intact (clear it via the field's
    // ✕). A no-op when the keyboard is already down (browsing categories/recents).
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val pick: (String) -> Unit = { emoji ->
        onPick(emoji)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // The emoji strings to show: search results, recents, or the selected category — defaults applied
    // (so the grid previews exactly what a tap inserts). Recomputed only when an input changes.
    val cells: List<PickerCell> = remember(query, selectedTab, defaultSkinTone, defaultGender, recents, tabs, glyphFilter) {
        fun cellFor(display: String, entry: EmojiCatalogEntry?) =
            PickerCell(display, entry, isWide = glyphFilter.isWide(display))
        fun of(entry: EmojiCatalogEntry) =
            cellFor(displayForm(entry, defaultSkinTone, defaultGender, glyphFilter), entry)
        val tabEntries = tabs[selectedTab].entries
        when {
            query.isNotBlank() -> EmojiSearch.search(query, displayEntries).map { of(it) }
            tabEntries == null -> recents.map { cellFor(it, null) } // recents tab: final picks, no variant tray
            else -> tabEntries.map { of(it) }
        }
    }
    var expandedCell by remember { mutableIntStateOf(-1) }

    // A subtly raised fill (ED-aware): always behind the category tabs, and behind the whole picker
    // while a long-press variant menu is open, so the picker stands out from the screen behind it.
    val raisedBackground = raisedSurfaceColor()
    val variantMenuOpen = expandedCell != -1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (variantMenuOpen) raisedBackground else Color.Transparent)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { GhostText(stringResource(R.string.emoji_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            // A circle-✕ to wipe the search text (the keyboard, and so search mode, can be dismissed
            // by a pick — leaving the query filled — so the user needs an explicit way to clear it).
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = stringResource(R.string.emoji_search_clear)
                        )
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Category tabs are hidden while searching (the grid shows ranked results instead).
        // Compact wireframe tabs: outline icons, tight spacing, and a fixed dp size so the emoji
        // scale (ED-14) never enlarges them.
        if (query.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(raisedBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.contentDescription,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Cells hug the glyph plus a *constant* gap, so blank space doesn't scale up with the emoji
        // size (ED-14) — only the glyph does. Floored at the 48dp minimum touch target, which only
        // governs at the smallest scale. `.sp.toDp()` tracks the glyph's true size incl. font scale.
        val cellMinSize = with(LocalDensity.current) {
            maxOf(EMOJI_GRID_MIN_CELL, (28 * emojiScale).sp.toDp() + EMOJI_GRID_CELL_GAP)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = cellMinSize),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            itemsIndexed(
                cells,
                // Double-wide multi-person glyphs get two columns so they don't overflow their cell
                // and overlap neighbors (clamped so a narrow grid never asks for more than it has).
                span = { _, cell -> GridItemSpan(if (cell.isWide) minOf(2, maxLineSpan) else 1) }
            ) { index, cell ->
                // Skin-tone/gender variants, or null if there are none (ED-8). Only variants the
                // device renders as a single combined glyph are offered — some multi-person emoji
                // have a combined glyph only without skin tone, and a toned variant that falls back to
                // separate component glyphs shouldn't appear in the tray.
                val variants = remember(cell, glyphFilter) {
                    cell.entry?.let { entry ->
                        val all = EmojiDefaults.variants(entry)
                        val baseWidth = glyphFilter.glyphWidth(all.first())
                        all.filter { glyphFilter.glyphWidth(it) <= baseWidth * COMBINED_GLYPH_TOLERANCE }
                    }?.takeIf { it.size > 1 }
                }
                // The cell fills its grid slot so the glyph centers (even gaps on both sides) and the
                // whole cell — not just the glyph — is the tap target. A small inner box hugs the
                // glyph so the variant-corner triangle anchors to the emoji rather than the cell edge.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { pick(cell.display) },
                            onLongClick = if (variants != null) ({ expandedCell = index }) else null
                        )
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = cell.display, fontSize = (28 * emojiScale).sp, textAlign = TextAlign.Center)
                        if (variants != null) {
                            // Affordance: a small corner triangle marks an emoji whose variants
                            // (gender/skin tone) are reachable by long-press (ED-8).
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size((8 * emojiScale).dp)
                                    .clip(VariantCornerTriangle)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    if (variants != null) {
                        DropdownMenu(
                            expanded = expandedCell == index,
                            onDismissRequest = { expandedCell = -1 }
                        ) {
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                                variants.forEach { variant ->
                                    Text(
                                        text = variant,
                                        fontSize = (28 * emojiScale).sp,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .clickable { pick(variant); expandedCell = -1 }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Floor for a picker grid cell — the Material 48dp minimum touch target. Only governs at the
 *  smallest emoji scale; above that the cell is glyph-driven (see `cellMinSize`). */
private val EMOJI_GRID_MIN_CELL = 48.dp

/** Constant breathing room added around a grid glyph. Fixed (not scaled), so the gaps between emoji
 *  stay the same as the emoji scale (ED-14) grows rather than widening proportionally. */
private val EMOJI_GRID_CELL_GAP = 12.dp

/** A right triangle filling the bottom-end corner — the "this emoji has variants" affordance (ED-8). */
private val VariantCornerTriangle = GenericShape { size, _ ->
    moveTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** A grid cell: the [display] emoji (defaults applied), the source [entry] for variant lookup (null
 *  for recents, which are already-final picks with no variant tray), and [isWide] when the glyph
 *  renders double-wide (a multi-person sequence) and so needs a wider cell to avoid overlapping. */
private data class PickerCell(val display: String, val entry: EmojiCatalogEntry?, val isWide: Boolean)

/** A picker tab: its wireframe [icon], accessibility [contentDescription], and the [entries] it
 *  shows ([entries] is null for the Recents tab, which renders the dynamic recents list). */
private data class EmojiTab(
    val icon: ImageVector,
    val contentDescription: String,
    val entries: List<EmojiCatalogEntry>?,
)

/** Largest a tone/gender variant may render relative to its neutral base and still count as a single
 *  combined glyph. The font draws a ligated variant at ~the base width; a variant it can't combine
 *  falls back to separate component glyphs and is much wider. */
private const val COMBINED_GLYPH_TOLERANCE = 1.4f

/**
 * The string to display and insert for [entry] with the user's [skinTone]/[gender] applied — but
 * falling back to a form the device renders as a single glyph when the toned/gendered form doesn't
 * ligate. Some multi-person emoji (e.g. people holding hands) have a combined glyph only without skin
 * tone; rather than show that as an expanded run of component emoji, prefer the combined glyph,
 * keeping as much of the preference as still fits (drop tone first, then gender, then neutral base).
 */
private fun displayForm(
    entry: EmojiCatalogEntry,
    skinTone: SkinTone,
    gender: Gender,
    glyphFilter: PlatformEmojiGlyphFilter
): String {
    val baseWidth = glyphFilter.glyphWidth(entry.emoji)
    val candidates = listOf(
        EmojiDefaults.apply(entry, skinTone, gender),         // full preference
        EmojiDefaults.apply(entry, SkinTone.DEFAULT, gender), // drop tone, keep gender
        EmojiDefaults.apply(entry, skinTone, Gender.NEUTRAL)  // drop gender, keep tone
    )
    return candidates.firstOrNull { glyphFilter.glyphWidth(it) <= baseWidth * COMBINED_GLYPH_TOLERANCE }
        ?: entry.emoji // neutral base always combines (it is why the entry is shown)
}
