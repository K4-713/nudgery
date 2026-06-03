package com.nudgery.shared.emoji

import android.graphics.Paint

/**
 * Android [EmojiGlyphFilter] backed by the platform font's glyph coverage via [Paint.hasGlyph]
 * (added in API 23; the app's minSdk is 26, so no compat wrapper is needed and no dependency is
 * added). Returns true only when the device can actually render [emoji] (ED-4).
 *
 * [Paint] is not thread-safe; this filter is meant to be used from a single thread (the picker's
 * filtering coroutine). A fresh paint is fine — `hasGlyph` consults the system font, not paint state.
 */
class PlatformEmojiGlyphFilter(private val paint: Paint = Paint()) : EmojiGlyphFilter {
    override fun canRender(emoji: String): Boolean = paint.hasGlyph(emoji)
}
