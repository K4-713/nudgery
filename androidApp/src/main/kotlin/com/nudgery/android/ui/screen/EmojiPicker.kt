package com.nudgery.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.nudgery.android.R
import com.nudgery.android.ui.theme.LocalEmojiScale
import com.nudgery.shared.emoji.EmojiCatalogEntry
import com.nudgery.shared.emoji.EmojiDefaults
import com.nudgery.shared.emoji.EmojiSearch
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.EmojiCatalog
import com.nudgery.shared.emoji.PlatformEmojiGlyphFilter
import com.nudgery.shared.emoji.SkinTone

/** Unicode groups shown as picker tabs, in display order, each with a representative emoji "icon"
 *  (device-font rendered, so no icon dependency). The "Component" group (skin tones, hair) is omitted. */
private val CATEGORY_TABS: List<Pair<String, String>> = listOf(
    "Smileys & Emotion" to "😀", // 😀
    "People & Body" to "🧑",       // 🧑
    "Animals & Nature" to "🐻",    // 🐻
    "Food & Drink" to "🍔",        // 🍔
    "Travel & Places" to "✈️",     // ✈️
    "Activities" to "⚽",                // ⚽
    "Objects" to "💡",             // 💡
    "Symbols" to "❤️",             // ❤️
    "Flags" to "🏁",               // 🏁
)

private const val RECENTS_TAB_EMOJI = "🕐" // 🕐

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
    // One-time: keep only emoji this device can render, grouped by Unicode group (ED-4).
    val renderable = remember {
        val glyphFilter = PlatformEmojiGlyphFilter()
        EmojiCatalog.entries.filter { glyphFilter.canRender(it.emoji) }
    }
    val byGroup = remember(renderable) { renderable.groupBy { it.group } }
    val tabs = remember(byGroup) {
        listOf(RecentsTab) + CATEGORY_TABS.mapNotNull { (group, tabEmoji) ->
            byGroup[group]?.let { CategoryTab(tabEmoji, it) }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val emojiScale = LocalEmojiScale.current // ED-14: emoji surfaces honor the global scale

    // The emoji strings to show: search results, recents, or the selected category — defaults applied
    // (so the grid previews exactly what a tap inserts). Recomputed only when an input changes.
    val cells: List<PickerCell> = remember(query, selectedTab, defaultSkinTone, defaultGender, recents, tabs) {
        fun of(entry: EmojiCatalogEntry) = PickerCell(entry.applyDefaults(defaultSkinTone, defaultGender), entry)
        when {
            query.isNotBlank() -> EmojiSearch.search(query, renderable).map { of(it) }
            tabs[selectedTab] is RecentsTab -> recents.map { PickerCell(it, null) } // recents have no variant tray
            else -> (tabs[selectedTab] as CategoryTab).entries.map { of(it) }
        }
    }
    var expandedCell by remember { mutableIntStateOf(-1) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.emoji_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Category tabs are hidden while searching (the grid shows ranked results instead).
        if (query.isBlank()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(if (tab is CategoryTab) tab.tabEmoji else RECENTS_TAB_EMOJI, fontSize = 20.sp) }
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = (48 * emojiScale).dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            itemsIndexed(cells) { index, cell ->
                // Skin-tone/gender variants for this emoji, or null if it has none (ED-8).
                val variants = remember(cell) {
                    cell.entry?.let { EmojiDefaults.variants(it) }?.takeIf { it.size > 1 }
                }
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .combinedClickable(
                            onClick = { onPick(cell.display) },
                            onLongClick = if (variants != null) ({ expandedCell = index }) else null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = cell.display, fontSize = (28 * emojiScale).sp, textAlign = TextAlign.Center)
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
                                            .clickable { onPick(variant); expandedCell = -1 }
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

/** A grid cell: the [display] emoji (defaults applied) and the source [entry] for variant lookup
 *  (null for recents, which are already-final picks with no variant tray). */
private data class PickerCell(val display: String, val entry: EmojiCatalogEntry?)

private sealed interface EmojiTab
private data object RecentsTab : EmojiTab
private data class CategoryTab(val tabEmoji: String, val entries: List<EmojiCatalogEntry>) : EmojiTab

private fun EmojiCatalogEntry.applyDefaults(skinTone: SkinTone, gender: Gender): String =
    EmojiDefaults.apply(this, skinTone, gender)
