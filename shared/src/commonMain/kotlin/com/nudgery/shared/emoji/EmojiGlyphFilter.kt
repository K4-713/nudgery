package com.nudgery.shared.emoji

/**
 * Decides whether the current device can actually render a given emoji, so the picker and search
 * only ever surface emoji the device can draw — never tofu — and availability tracks the device
 * rather than a cross-platform intersection (ENGINEERING_DECISIONS.md ED-4).
 *
 * The implementation is platform-specific (Android uses the platform font's glyph coverage; iOS
 * will use its glyph-availability equivalent), so the shared catalog/search depend on this seam and
 * tests can substitute a fake.
 */
interface EmojiGlyphFilter {
    fun canRender(emoji: String): Boolean
}
