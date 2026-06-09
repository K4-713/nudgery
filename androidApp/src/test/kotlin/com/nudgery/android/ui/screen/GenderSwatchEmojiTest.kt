// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.SkinTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [genderSwatchEmoji], the sample shown on each gender swatch in the Settings emoji
 * defaults. ENGINEERING_DECISIONS.md ED-7 requires that gender and skin-tone defaults *compose*
 * ("neutral person -> woman -> woman + tone"), so when a skin tone is selected on the Settings page
 * the gender example people must restyle to that tone — the bug these tests pin down.
 */
class GenderSwatchEmojiTest {

    private val PERSON = 0x1F9D1   // neutral person
    private val WOMAN = 0x1F469    // woman base
    private val MAN = 0x1F468      // man base
    private val DARK = 0x1F3FF     // dark skin-tone modifier
    private val personSample = cp(PERSON)

    /** Builds a string from explicit code points, so tests never depend on invisible literals. */
    private fun cp(vararg codePoints: Int): String = buildString {
        for (c in codePoints) appendCodePoint(c)
    }

    private fun StringBuilder.appendCodePoint(c: Int) {
        if (c <= 0xFFFF) {
            append(c.toChar())
        } else {
            val o = c - 0x10000
            append((0xD800 + (o shr 10)).toChar())
            append((0xDC00 + (o and 0x3FF)).toChar())
        }
    }

    private fun codePointsOf(text: String): List<Int> {
        val out = ArrayList<Int>(text.length)
        var i = 0
        while (i < text.length) {
            val high = text[i]
            if (high.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                out.add(0x10000 + ((high.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00))
                i += 2
            } else {
                out.add(high.code); i++
            }
        }
        return out
    }

    // ENGINEERING_DECISIONS.md ED-7: defaults compose, so selecting a skin tone restyles the
    // (neutral) gender example person to that tone instead of leaving it platform-default.
    @Test
    fun TDD_genderSwatchAppliesSelectedSkinTone() {
        val result = genderSwatchEmoji(personSample, Gender.NEUTRAL, SkinTone.DARK)
        assertEquals(listOf(PERSON, DARK), codePointsOf(result))
    }

    // ENGINEERING_DECISIONS.md ED-7: gender and skin tone compose together (woman + tone), so an
    // explicitly-gendered example person is *also* toned when a skin tone is selected.
    @Test
    fun TDD_genderSwatchAppliesToneToGenderedForm() {
        val woman = genderSwatchEmoji(personSample, Gender.WOMAN, SkinTone.DARK)
        val womanCps = codePointsOf(woman)
        assertTrue("woman swatch should resolve to the woman base", womanCps.contains(WOMAN))
        assertTrue("woman swatch should carry the selected tone", womanCps.contains(DARK))

        val man = genderSwatchEmoji(personSample, Gender.MAN, SkinTone.DARK)
        val manCps = codePointsOf(man)
        assertTrue("man swatch should resolve to the man base", manCps.contains(MAN))
        assertTrue("man swatch should carry the selected tone", manCps.contains(DARK))
    }

    // ED-7 / ED-6: the DEFAULT tone applies no modifier, so the example person stays in the
    // platform-neutral form (no skin-tone code point appended).
    @Test
    fun TDD_genderSwatchDefaultToneLeavesPersonUntoned() {
        val result = genderSwatchEmoji(personSample, Gender.NEUTRAL, SkinTone.DEFAULT)
        assertEquals(listOf(PERSON), codePointsOf(result))
    }
}
