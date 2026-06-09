// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.emoji.normalizeEmojiPresentation
import kotlin.test.Test
import kotlin.test.assertEquals

class EmojiPresentationTest {

    // "❤" is U+2764 with no presentation selector; its emoji-presentation form adds U+FE0F.
    private val redHeartText = "\u2764"
    private val redHeartEmoji = "\u2764\uFE0F"

    @Test
    fun TDD_appliesEmojiPresentationToTextDefaultSymbols() {
        // ENGINEERING_DECISIONS.md ED-9: a dual-use symbol gains the U+FE0F emoji-presentation selector.
        assertEquals(redHeartEmoji, normalizeEmojiPresentation(redHeartText))
    }

    @Test
    fun TDD_leavesAlreadyQualifiedEmojiUnchanged() {
        assertEquals("😀", normalizeEmojiPresentation("😀"))
        assertEquals(redHeartEmoji, normalizeEmojiPresentation(redHeartEmoji))
    }

    @Test
    fun TDD_dropsNonEmojiWhileNormalizing() {
        // The normalizer runs the emoji extractor, so non-emoji is removed and symbols are qualified.
        assertEquals("$redHeartEmoji😀", normalizeEmojiPresentation("a${redHeartText}b😀"))
    }

    @Test
    fun TDD_leavesPickerVariantsUnchanged() {
        // A skin-toned variant isn't a base concept, so it's passed through as-is.
        assertEquals("👍🏽", normalizeEmojiPresentation("👍🏽"))
    }
}
