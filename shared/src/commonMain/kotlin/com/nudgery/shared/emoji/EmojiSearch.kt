// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.emoji

/**
 * Typeahead emoji search (ENGINEERING_DECISIONS.md ED-11): ranks catalog entries by how well they
 * match a query against each emoji's name and CLDR keywords (ED-10). Pure, in-memory, and shared by
 * both platforms — a linear scan over a few thousand entries needs no index.
 *
 * Ranking, best first: exact name > name-word prefix > exact keyword > keyword prefix. A multi-word
 * query ANDs its tokens (every token must match the entry). There is deliberately **no** fuzzy/typo
 * or semantic matching in v1 — "related" means the curated CLDR keywords.
 *
 * Two concerns are the caller's, by composition: device renderability (ED-4) — pass only the
 * entries the device can draw — and default skin tone/gender (ED-6/ED-7), applied when an entry is
 * picked, not here.
 */
object EmojiSearch {

    private const val TIER_EXACT_NAME = 0
    private const val TIER_NAME_PREFIX = 1
    private const val TIER_KEYWORD_EXACT = 2
    private const val TIER_KEYWORD_PREFIX = 3

    private val TOKEN_SPLIT = Regex("\\s+")
    private val NAME_WORD_SPLIT = Regex("[\\s:,]+")

    /**
     * Returns the entries of [entries] matching [query], best match first. A blank query returns an
     * empty list (search is inactive; the picker shows its normal browse grid instead).
     */
    fun search(
        query: String,
        entries: List<EmojiCatalogEntry> = GeneratedEmojiCatalog.entries,
    ): List<EmojiCatalogEntry> {
        val tokens = query.lowercase().split(TOKEN_SPLIT).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()

        return entries
            .mapNotNull { entry ->
                var worstTier = TIER_EXACT_NAME
                var tierSum = 0
                for (token in tokens) {
                    val tier = matchTier(token, entry) ?: return@mapNotNull null // AND: all tokens
                    if (tier > worstTier) worstTier = tier
                    tierSum += tier
                }
                Scored(entry, worstTier, tierSum)
            }
            // Strongest weakest-token match first, then total relevance, then shorter/alphabetical names.
            .sortedWith(
                compareBy({ it.worstTier }, { it.tierSum }, { it.entry.name.length }, { it.entry.name })
            )
            .map { it.entry }
    }

    private class Scored(val entry: EmojiCatalogEntry, val worstTier: Int, val tierSum: Int)

    /** The best (lowest) tier at which [token] matches [entry]'s name or keywords, or null. */
    private fun matchTier(token: String, entry: EmojiCatalogEntry): Int? {
        val name = entry.name.lowercase()
        if (name == token) return TIER_EXACT_NAME
        val nameWords = name.split(NAME_WORD_SPLIT)
        if (nameWords.any { it.isNotEmpty() && it.startsWith(token) }) return TIER_NAME_PREFIX
        if (entry.keywords.any { it.equals(token, ignoreCase = true) }) return TIER_KEYWORD_EXACT
        if (entry.keywords.any { it.startsWith(token, ignoreCase = true) }) return TIER_KEYWORD_PREFIX
        return null
    }
}
