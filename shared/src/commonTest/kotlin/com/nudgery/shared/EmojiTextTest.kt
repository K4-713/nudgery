package com.nudgery.shared

import com.nudgery.shared.util.isEmojiOnly
import com.nudgery.shared.util.sanitizeToEmoji
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmojiTextTest {

    @Test
    fun TDD_sanitizeToEmojiKeepsOnlyEmoji() {
        // ENGINEERING_DECISIONS.md ED-2: EMOJI input is constrained to emoji; non-emoji is dropped.
        assertEquals("😀🐱", sanitizeToEmoji("a😀b🐱c"))
        assertEquals("😀🐱", sanitizeToEmoji("😀 🐱"), "whitespace is dropped")
        assertEquals("", sanitizeToEmoji("hello"))
        assertEquals("👍🏽", sanitizeToEmoji("👍🏽"), "skin tone is preserved")
        assertEquals("👨‍👩‍👧", sanitizeToEmoji("family 👨‍👩‍👧 here"), "ZWJ sequence kept whole")
    }

    @Test
    fun TDD_isEmojiOnlyGuardsTheStorageInvariant() {
        // ED-2: only emoji-only strings pass the save-time guard.
        assertTrue(isEmojiOnly("😀"))
        assertTrue(isEmojiOnly("😀🐱"))
        assertTrue(isEmojiOnly("👍🏽"))
        assertFalse(isEmojiOnly(""), "empty is not emoji-only")
        assertFalse(isEmojiOnly("😀 🐱"), "a space is not emoji")
        assertFalse(isEmojiOnly("a😀"))
        assertFalse(isEmojiOnly("hello"))
    }

    @Test
    fun TDD_recognizesKeycapEmoji() {
        // ED-2: keycap emoji (e.g. 5️⃣ = '5' U+FE0F U+20E3) must be kept, not dropped as a bare digit.
        val keycapFive = "5\uFE0F\u20E3"
        assertEquals(keycapFive, sanitizeToEmoji(keycapFive))
        assertTrue(isEmojiOnly(keycapFive))
        assertEquals("", sanitizeToEmoji("5"), "a bare digit is not an emoji")
        assertFalse(isEmojiOnly("55"))
    }
}
