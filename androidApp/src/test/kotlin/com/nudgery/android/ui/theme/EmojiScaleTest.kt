// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [emojiScaledAppBarHeight] (ENGINEERING_DECISIONS.md ED-15): a top app bar grows to fit
 * an emoji-only, emoji-scaled title and otherwise keeps its default height.
 *
 * Uses a unit-scale [Density] (density 1, fontScale 1) so an N-sp glyph converts to N dp.
 */
class EmojiScaleTest {

    // titleLarge base size; matches theme/Type.kt.
    private val baseTitleSize = 26.sp
    // An arbitrary body size for the emojiScaledStyle (ED-14) content tests.
    private val baseBodySize = 20.sp
    private val defaultHeight = 64.dp
    private val unitDensity = Density(density = 1f, fontScale = 1f)

    // ED-15: emoji-only title scaled up grows the bar past the default height.
    @Test
    fun TDD_emojiOnlyTitleGrowsBarPastDefault() {
        val height = with(unitDensity) {
            emojiScaledAppBarHeight("🐶", scale = 2.5f, baseTitleSize = baseTitleSize, defaultHeight = defaultHeight)
        }
        // 26sp * 2.5 = 65dp glyph + 16dp breathing room = 81dp, which exceeds the 64dp default.
        assertEquals(81.dp, height)
        assertTrue("emoji title should grow the bar", height > defaultHeight)
    }

    // ED-15: a title mixing emoji and text rides the text size and keeps the default height.
    @Test
    fun TDD_mixedTextTitleKeepsDefault() {
        val height = with(unitDensity) {
            emojiScaledAppBarHeight("Walk 🐶", scale = 2.5f, baseTitleSize = baseTitleSize, defaultHeight = defaultHeight)
        }
        assertEquals(defaultHeight, height)
    }

    // ED-15: a plain-text title keeps the default height regardless of scale.
    @Test
    fun TDD_plainTextTitleKeepsDefault() {
        val height = with(unitDensity) {
            emojiScaledAppBarHeight("Walk the dog", scale = 2.5f, baseTitleSize = baseTitleSize, defaultHeight = defaultHeight)
        }
        assertEquals(defaultHeight, height)
    }

    // ED-15: at scale 1.0 the bar is unchanged even for an emoji-only title.
    @Test
    fun TDD_unscaledEmojiKeepsDefault() {
        val height = with(unitDensity) {
            emojiScaledAppBarHeight("🐶", scale = 1f, baseTitleSize = baseTitleSize, defaultHeight = defaultHeight)
        }
        assertEquals(defaultHeight, height)
    }

    // ED-15: growth is floored at the default — a low scale that fits never shrinks the bar.
    @Test
    fun TDD_lowScaleNeverDropsBelowDefault() {
        val height = with(unitDensity) {
            emojiScaledAppBarHeight("🐶", scale = 1.1f, baseTitleSize = baseTitleSize, defaultHeight = defaultHeight)
        }
        // 26sp * 1.1 = 28.6dp + 16dp = 44.6dp, below the 64dp default, so the default holds.
        assertEquals(defaultHeight, height)
    }

    // The global emoji scale (ED-14) enlarges an emoji-only string by the scale factor.
    @Test
    fun TDD_emojiOnlyStringScales() {
        val base = TextStyle(fontSize = baseBodySize)
        val scaled = emojiScaledStyle("🐶", base, scale = 2f)
        assertEquals(baseBodySize * 2f, scaled.fontSize)
    }

    // ED-14: a string mixing text and emoji rides the text size and is left unscaled.
    @Test
    fun TDD_mixedTextStringDoesNotScale() {
        val base = TextStyle(fontSize = baseBodySize)
        assertEquals(baseBodySize, emojiScaledStyle("Walk 🐶", base, scale = 2f).fontSize)
    }

    // ED-14: plain text is left unscaled regardless of the scale factor.
    @Test
    fun TDD_plainTextStringDoesNotScale() {
        val base = TextStyle(fontSize = baseBodySize)
        assertEquals(baseBodySize, emojiScaledStyle("Walk the dog", base, scale = 2f).fontSize)
    }

    // ED-14: at scale 1.0 even an emoji-only string is returned unchanged.
    @Test
    fun TDD_unitScaleLeavesEmojiUnchanged() {
        val base = TextStyle(fontSize = baseBodySize)
        assertEquals(baseBodySize, emojiScaledStyle("🐶", base, scale = 1f).fontSize)
    }
}
