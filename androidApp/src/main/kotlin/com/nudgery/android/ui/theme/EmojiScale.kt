package com.nudgery.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
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
