package com.nudgery.buildtools.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiTestParserTest {

    private val sample = """
        # emoji-test.txt
        # Version: 16.0

        # group: Smileys & Emotion

        # subgroup: face-smiling
        1F600                                      ; fully-qualified     # 😀 E1.0 grinning face

        # group: People & Body

        # subgroup: person-role
        1F9D1 200D 2695 FE0F                       ; fully-qualified     # 🧑‍⚕️ E12.1 health worker
        1F468 200D 2695 FE0F                       ; fully-qualified     # 👨‍⚕️ E4.0 man health worker
        1F469 200D 2695 FE0F                       ; fully-qualified     # 👩‍⚕️ E4.0 woman health worker
        1F469 1F3FD 200D 2695 FE0F                 ; fully-qualified     # 👩🏽‍⚕️ E4.0 woman health worker: medium skin tone

        # subgroup: component
        1F3FB                                      ; component           # 🏻 E1.0 light skin tone
    """.trimIndent()

    @Test
    fun parsesEntriesWithGroupSubgroupNameAndVersion() {
        // ENGINEERING_DECISIONS.md ED-5: parse emoji-test.txt into a categorized list.
        val entries = EmojiTestParser.parse(sample)
        assertEquals(6, entries.size)

        val grinning = entries.first()
        assertEquals(listOf(0x1F600), grinning.codePoints)
        assertEquals("😀", grinning.emoji)
        assertEquals("grinning face", grinning.name)
        assertEquals("1.0", grinning.emojiVersion)
        assertEquals("Smileys & Emotion", grinning.group)
        assertEquals("face-smiling", grinning.subgroup)
        assertEquals(EmojiQualification.FULLY_QUALIFIED, grinning.qualification)
    }

    @Test
    fun assignsTheCurrentGroupAndSubgroup() {
        val healthWorker = EmojiTestParser.parse(sample).first { it.name == "health worker" }
        assertEquals("People & Body", healthWorker.group)
        assertEquals("person-role", healthWorker.subgroup)
        assertEquals("12.1", healthWorker.emojiVersion)
    }

    @Test
    fun detectsSkinToneModifierAndComponentStatus() {
        val entries = EmojiTestParser.parse(sample)
        assertTrue(entries.first { it.name.contains("medium skin tone") }.hasSkinToneModifier)
        assertFalse(entries.first { it.name == "health worker" }.hasSkinToneModifier)
        assertEquals(EmojiQualification.COMPONENT, entries.first { it.name == "light skin tone" }.qualification)
    }
}
