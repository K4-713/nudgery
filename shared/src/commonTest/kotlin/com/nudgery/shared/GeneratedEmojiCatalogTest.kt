package com.nudgery.shared

import com.nudgery.shared.emoji.GeneratedEmojiCatalog
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratedEmojiCatalogTest {

    private val entries = GeneratedEmojiCatalog.entries

    @Test
    fun TDD_generatedCatalogIsPopulatedWithBaseConcepts() {
        // ENGINEERING_DECISIONS.md ED-5: the generated catalog is present and holds many base concepts.
        assertTrue(entries.size > 1000, "expected a populated catalog, got ${entries.size}")
        assertTrue(
            entries.any { it.emoji == "😀" && it.name == "grinning face" },
            "a well-known emoji should be present"
        )
    }

    @Test
    fun TDD_catalogHoldsOnlyBaseConceptsNotVariants() {
        // ED-5: only base concepts are stored; skin-tone and hair variants are derived by rule.
        val offenders = entries.filter { entry ->
            entry.emoji.toCodePoints().any { it in 0x1F3FB..0x1F3FF || it in 0x1F9B0..0x1F9B3 }
        }
        assertTrue(
            offenders.isEmpty(),
            "catalog should contain no toned/hair variants, found e.g. ${offenders.take(3).map { it.name }}"
        )
    }

    @Test
    fun TDD_attachesCldrKeywordsForSearch() {
        // ED-10: catalog entries carry CLDR search keywords for typeahead.
        val grinning = entries.first { it.name == "grinning face" }
        assertTrue(grinning.keywords.isNotEmpty(), "grinning face should have CLDR keywords")
        assertTrue(
            entries.count { it.keywords.isNotEmpty() } > entries.size / 2,
            "most concepts should carry keywords"
        )
    }

    @Test
    fun TDD_flagsSkinToneCapableConcepts() {
        // ED-6: concepts with skin-tone variants are flagged so the picker can derive them by rule.
        assertTrue(entries.any { it.acceptsSkinTone }, "some concepts should accept skin tone")
        val wavingHand = entries.firstOrNull { it.name == "waving hand" }
        assertNotNull(wavingHand, "waving hand should be in the catalog")
        assertTrue(wavingHand.acceptsSkinTone, "waving hand has skin-tone variants")
    }
}

/** Decodes a String into Unicode code points (common-Kotlin has no built-in code-point iterator). */
private fun String.toCodePoints(): List<Int> {
    val out = ArrayList<Int>(length)
    var i = 0
    while (i < length) {
        val high = this[i]
        if (high.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            out.add(0x10000 + ((high.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00))
            i += 2
        } else {
            out.add(high.code)
            i++
        }
    }
    return out
}
