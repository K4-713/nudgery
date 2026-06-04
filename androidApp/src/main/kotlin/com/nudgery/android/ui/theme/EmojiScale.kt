package com.nudgery.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.times
import com.nudgery.shared.util.isEmojiOnly

/**
 * Global emoji size multiplier (ENGINEERING_DECISIONS.md ED-14), provided at the app root from the
 * user's setting; 1.0 = default. Emoji *surfaces* (the picker grid, the EMOJI-answer display)
 * multiply their base size by this; emoji embedded in text ride the text size and ignore it.
 */
val LocalEmojiScale = compositionLocalOf { 1f }

/**
 * Scales [base]'s font size by the global emoji scale (ED-14) **only when [text] is emoji-only**
 * (`isEmojiOnly`); mixed text+emoji and plain text are returned unchanged. Use at any display site
 * that can show an emoji-only string (nudge names, question text, raw-data answers, …).
 */
@Composable
fun emojiScaledStyle(text: String, base: TextStyle): TextStyle {
    val scale = LocalEmojiScale.current
    return if (scale != 1f && base.fontSize.isSpecified && isEmojiOnly(text)) {
        base.copy(fontSize = base.fontSize * scale)
    } else {
        base
    }
}

/**
 * Vertical breathing room kept above and below an enlarged emoji title so the scaled glyph isn't
 * flush against the (grown) app-bar edges.
 */
private val EMOJI_TITLE_VERTICAL_PADDING = 16.dp

/**
 * The height a single-row `TopAppBar` needs so an **emoji-only**, emoji-scaled title (ED-14) isn't
 * clipped by the bar's fixed [defaultHeight] (ENGINEERING_DECISIONS.md ED-15). Material's small top
 * app bar centers the title in a fixed-height container and clips to bounds, so a title scaled past
 * that height (a lone emoji at a high [scale]) gets cropped.
 *
 * For ordinary titles — mixed text, plain text, or [scale] 1.0 — this returns [defaultHeight]
 * unchanged. For an emoji-only title scaled up, it returns a height that fits the scaled glyph plus
 * [EMOJI_TITLE_VERTICAL_PADDING], but never less than [defaultHeight]. The conversion is done in a
 * [Density] scope so the result tracks the device's font-scale setting, not just [baseTitleSize].
 */
fun Density.emojiScaledAppBarHeight(
    text: String,
    scale: Float,
    baseTitleSize: TextUnit,
    defaultHeight: Dp
): Dp {
    if (scale <= 1f || !baseTitleSize.isSp || !isEmojiOnly(text)) return defaultHeight
    val scaledGlyphHeight = (baseTitleSize * scale).toDp()
    return maxOf(defaultHeight, scaledGlyphHeight + EMOJI_TITLE_VERTICAL_PADDING)
}
