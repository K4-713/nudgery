// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.emoji.EmojiCatalogEntry
import com.nudgery.shared.emoji.EmojiSearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmojiSearchTest {

    private fun entry(name: String, vararg keywords: String) =
        EmojiCatalogEntry("🐱", name, "Animals & Nature", "animal", "1.0", false, false, keywords.toList())

    @Test
    fun TDD_ranksExactNameThenNamePrefixThenKeyword() {
        // ED-11: exact name > name(-word) prefix > keyword.
        val exactName = entry("cat")
        val namePrefix = entry("cat face")
        val keywordOnly = entry("kitten", "cat")
        val results = EmojiSearch.search("cat", listOf(keywordOnly, namePrefix, exactName))
        assertEquals(listOf("cat", "cat face", "kitten"), results.map { it.name })
    }

    @Test
    fun TDD_matchesByKeywordNotInTheName() {
        // ED-11/ED-10: keywords make an emoji findable by words that aren't in its name.
        val grinning = entry("grinning face", "happy", "smile")
        val results = EmojiSearch.search("happy", listOf(grinning))
        assertEquals(listOf("grinning face"), results.map { it.name })
    }

    @Test
    fun TDD_multiWordQueryAndsItsTokens() {
        // ED-11: every token must match (AND). A name-word prefix matches any word of the name.
        val redHeart = entry("red heart", "love")
        val blueHeart = entry("blue heart", "love")
        assertEquals(listOf("red heart"), EmojiSearch.search("red heart", listOf(redHeart, blueHeart)).map { it.name })
        assertTrue(EmojiSearch.search("red love nonsense", listOf(redHeart, blueHeart)).isEmpty())
    }

    @Test
    fun TDD_blankQueryReturnsNothing() {
        assertTrue(EmojiSearch.search("", listOf(entry("cat"))).isEmpty())
        assertTrue(EmojiSearch.search("   ", listOf(entry("cat"))).isEmpty())
    }

    @Test
    fun TDD_searchesTheRealCatalogByNameAndKeyword() {
        // Smoke test against the generated catalog: a name match ranks first, and a keyword-only
        // query still finds the emoji (grinning face has no "happy" in its name).
        val catResults = EmojiSearch.search("cat")
        assertEquals("cat", catResults.first().name, "exact-name match ranks first")
        assertTrue(
            EmojiSearch.search("happy").any { it.name == "grinning face" },
            "grinning face is found by its CLDR keyword 'happy'"
        )
    }
}
