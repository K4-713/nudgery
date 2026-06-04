package com.nudgery.android.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Global emoji size multiplier (ENGINEERING_DECISIONS.md ED-14), provided at the app root from the
 * user's setting; 1.0 = default. Emoji *surfaces* (the picker grid, the EMOJI-answer display)
 * multiply their base size by this; emoji embedded in text ride the text size and ignore it.
 */
val LocalEmojiScale = compositionLocalOf { 1f }
