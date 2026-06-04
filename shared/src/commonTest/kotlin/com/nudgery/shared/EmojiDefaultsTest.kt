package com.nudgery.shared

import com.nudgery.shared.emoji.EmojiCatalogEntry
import com.nudgery.shared.emoji.EmojiDefaults
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.SkinTone
import kotlin.test.Test
import kotlin.test.assertEquals

class EmojiDefaultsTest {

    /** Builds a string from explicit code points, so tests never depend on invisible literals. */
    private fun cp(vararg codePoints: Int): String = buildString {
        for (c in codePoints) {
            if (c <= 0xFFFF) {
                append(c.toChar())
            } else {
                val o = c - 0x10000
                append((0xD800 + (o shr 10)).toChar())
                append((0xDC00 + (o and 0x3FF)).toChar())
            }
        }
    }

    private val thumbsUp = cp(0x1F44D)
    private val victoryHand = cp(0x270C, 0xFE0F)        // ✌️ — text-default symbol carrying VS16
    private val person = cp(0x1F9D1)
    private val woman = cp(0x1F469)
    private val man = cp(0x1F468)
    private val grinning = cp(0x1F600)
    private val healthWorker = cp(0x1F9D1, 0x200D, 0x2695, 0xFE0F)        // 🧑‍⚕️ neutral
    private val manHealthWorker = cp(0x1F468, 0x200D, 0x2695, 0xFE0F)     // 👨‍⚕️

    // --- Skin tone (ED-6) ---

    @Test
    fun TDD_appliesSkinToneModifierToASimpleEmoji() {
        assertEquals(cp(0x1F44D, 0x1F3FD), EmojiDefaults.applySkinTone(thumbsUp, SkinTone.MEDIUM))
    }

    @Test
    fun TDD_skinToneSupersedesVs16() {
        // The skin-tone modifier replaces the base component's VS16 (✌️ → ✌🏽).
        assertEquals(cp(0x270C, 0x1F3FD), EmojiDefaults.applySkinTone(victoryHand, SkinTone.MEDIUM))
    }

    @Test
    fun TDD_skinToneNeverOverridesAnExistingTone() {
        val toned = cp(0x1F44D, 0x1F3FD)
        assertEquals(toned, EmojiDefaults.applySkinTone(toned, SkinTone.DARK))
    }

    @Test
    fun TDD_defaultSkinToneIsANoOp() {
        assertEquals(thumbsUp, EmojiDefaults.applySkinTone(thumbsUp, SkinTone.DEFAULT))
    }

    // --- Gender (ED-7) ---

    @Test
    fun TDD_genderMapsBareNeutralFigureToWomanAndMan() {
        assertEquals(woman, EmojiDefaults.applyGender(person, Gender.WOMAN))
        assertEquals(man, EmojiDefaults.applyGender(person, Gender.MAN))
    }

    @Test
    fun TDD_genderMapsNeutralRoleSequence() {
        assertEquals(manHealthWorker, EmojiDefaults.applyGender(healthWorker, Gender.MAN))
    }

    @Test
    fun TDD_genderNeverOverridesAnExplicitChoice() {
        // An explicitly-woman emoji is not flipped to the default gender.
        assertEquals(woman, EmojiDefaults.applyGender(woman, Gender.MAN))
    }

    @Test
    fun TDD_genderLeavesNonGenderedEmojiUnchanged() {
        assertEquals(grinning, EmojiDefaults.applyGender(grinning, Gender.WOMAN))
        assertEquals(person, EmojiDefaults.applyGender(person, Gender.NEUTRAL))
    }

    // --- Composition via apply() (ED-6 + ED-7) ---

    @Test
    fun TDD_applyComposesGenderThenSkinTone() {
        val entry = catalogEntry(healthWorker, acceptsSkinTone = true)
        // neutral health worker → man → man + medium skin tone (👨🏽‍⚕️).
        assertEquals(
            cp(0x1F468, 0x1F3FD, 0x200D, 0x2695, 0xFE0F),
            EmojiDefaults.apply(entry, SkinTone.MEDIUM, Gender.MAN)
        )
    }

    @Test
    fun TDD_applyDoesNotToneAnEmojiThatDoesNotAcceptSkinTone() {
        val entry = catalogEntry(grinning, acceptsSkinTone = false)
        assertEquals(grinning, EmojiDefaults.apply(entry, SkinTone.MEDIUM, Gender.NEUTRAL))
    }

    private fun catalogEntry(emoji: String, acceptsSkinTone: Boolean) = EmojiCatalogEntry(
        emoji = emoji, name = "x", group = "g", subgroup = "s", emojiVersion = "1.0",
        acceptsSkinTone = acceptsSkinTone, hairCapable = false, keywords = emptyList()
    )
}
