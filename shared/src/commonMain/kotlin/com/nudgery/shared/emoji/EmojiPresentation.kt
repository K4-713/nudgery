// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.emoji

import com.nudgery.shared.util.extractEmojiWords

/**
 * Ensures emoji (not text) presentation for every emoji in [text] (ENGINEERING_DECISIONS.md ED-9).
 *
 * Each emoji is snapped to the catalog's fully-qualified base form, which carries the U+FE0F
 * emoji-presentation selector exactly where Unicode requires it — so a dual-use symbol like "❤"
 * (which some platforms render as monochrome text) becomes "❤️". Emoji not present as base concepts
 * in the catalog (e.g. skin-toned or hair variants the picker inserts already fully qualified) are
 * left unchanged. Non-emoji characters are dropped, since this also runs the emoji extractor.
 */
fun normalizeEmojiPresentation(text: String): String =
    extractEmojiWords(text).joinToString("") { word ->
        catalogEmojiByStrippedForm[stripVariationSelectors(word)] ?: word
    }

/** Catalog base emoji keyed by their variation-selector-stripped form, for presentation lookup. */
private val catalogEmojiByStrippedForm: Map<String, String> by lazy {
    GeneratedEmojiCatalog.entries.associate { stripVariationSelectors(it.emoji) to it.emoji }
}

/** Removes variation selectors (U+FE00–U+FE0F) so toned-down and fully-qualified forms compare equal. */
private fun stripVariationSelectors(text: String): String =
    text.filter { it.code !in 0xFE00..0xFE0F }
