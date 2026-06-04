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

    /**
     * True when [emoji] renders much wider than a single-cell emoji on this device's font — i.e. a
     * multi-person ZWJ sequence (couple, kiss, family, two people holding hands) the font draws as
     * side-by-side figures. The picker gives these a wider grid cell so the glyph doesn't overflow
     * and overlap its neighbors. Measured against a single-person reference glyph (so the ratio is
     * independent of [Paint.getTextSize]) and tracks the actual device font rather than assuming a
     * fixed list of code points.
     */
    fun isWide(emoji: String): Boolean =
        singleEmojiWidth > 0f && paint.measureText(emoji) > singleEmojiWidth * WIDE_RATIO_THRESHOLD

    /**
     * Advance width of [emoji] with this paint's text size, in px. Used to compare a tone/gender
     * variant's rendering to its base: a variant that the font combines into one glyph has ~the same
     * width as the base, while one that falls back to separate component glyphs is much wider.
     */
    fun glyphWidth(emoji: String): Float = paint.measureText(emoji)

    /** Advance width of one single-person glyph, measured once with this paint's text size. */
    private val singleEmojiWidth: Float by lazy { paint.measureText(REFERENCE_SINGLE_EMOJI) }

    private companion object {
        /** 🧑 person (U+1F9D1) — a representative one-cell-wide emoji. */
        const val REFERENCE_SINGLE_EMOJI = "🧑"
        /** A glyph wider than 1.5× the reference is treated as multi-figure; doubles measure ~2×. */
        const val WIDE_RATIO_THRESHOLD = 1.5f
    }
}
