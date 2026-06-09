// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.util

/**
 * Splits [text] into its individual emoji, so distinct emoji written without a space (e.g. 🐶🐱)
 * become separate elements while a single emoji's modifiers, variation selectors, ZWJ-joined parts,
 * and regional-indicator (flag) pairs stay attached. Non-emoji characters act as separators and are
 * dropped. Keycap sequences (e.g. 5️⃣) are recognized even though their base is an ASCII character.
 *
 * Shared by the packed-bubble chart's tokenizer and the emoji-only answer validator
 * (ENGINEERING_DECISIONS.md ED-2).
 */
internal fun extractEmojiWords(text: String): List<String> {
    val words = mutableListOf<String>()
    val current = StringBuilder()
    var afterZwj = false       // previous code point was a zero-width joiner
    var regionalRun = 0        // count of consecutive regional indicators in `current`

    fun flush() {
        if (current.isNotEmpty()) words.add(current.toString())
        current.clear()
        afterZwj = false
        regionalRun = 0
    }

    var index = 0
    while (index < text.length) {
        val high = text[index]
        val pairedLow = if (high.isHighSurrogate() && index + 1 < text.length) text[index + 1] else null
        val (codePoint, charCount) = if (pairedLow != null && pairedLow.isLowSurrogate()) {
            combineSurrogates(high, pairedLow) to 2
        } else {
            high.code to 1
        }
        val nextCodePoint = if (index + charCount < text.length) text[index + charCount].code else -1

        when {
            isEmojiJoiner(codePoint) -> {
                if (current.isNotEmpty()) {
                    current.append(text, index, index + charCount)
                    afterZwj = codePoint == 0x200D
                    regionalRun = 0
                }
            }
            isEmojiStart(codePoint) -> {
                val regional = codePoint in 0x1F1E6..0x1F1FF
                val continuesCurrent = current.isEmpty() ||
                    afterZwj ||                          // ZWJ sequence continues this emoji
                    (regional && regionalRun == 1)       // second half of a flag
                if (!continuesCurrent) flush()
                current.append(text, index, index + charCount)
                afterZwj = false
                regionalRun = if (regional) regionalRun + 1 else 0
            }
            // A keycap base (0-9, #, *) starts an emoji only when it is actually keycapped, i.e.
            // followed by the keycap combining mark or its emoji-presentation selector — so bare
            // digits and text aren't mistaken for emoji.
            isKeycapBase(codePoint) && (nextCodePoint == 0x20E3 || nextCodePoint == 0xFE0F) -> {
                if (current.isNotEmpty()) flush()
                current.append(text, index, index + charCount)
                afterZwj = false
                regionalRun = 0
            }
            else -> flush()
        }
        index += charCount
    }
    flush()
    return words
}

/**
 * True when [text] is exactly one emoji and nothing else — the same notion of "an emoji" the bubble
 * chart's tokenizer uses ([extractEmojiWords]), so multi-code-point emoji (skin tones, variation
 * selectors, ZWJ sequences, regional-indicator flag pairs) all count as one. The packed bubble chart
 * uses this to draw single-emoji bubbles much larger, since a lone emoji has room to spare.
 */
fun isSingleEmoji(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false
    val words = extractEmojiWords(trimmed)
    return words.size == 1 && words.first() == trimmed
}

/**
 * Keeps only the emoji in [text], dropping every non-emoji character (letters, digits, punctuation,
 * whitespace) and concatenating the emoji in order. Constrains EMOJI-question input to emoji only
 * regardless of source — picker, paste, or keyboard (ENGINEERING_DECISIONS.md ED-2).
 */
fun sanitizeToEmoji(text: String): String = extractEmojiWords(text).joinToString("")

/**
 * True when [text] is non-empty and contains only emoji — the storage invariant guarded at save
 * time for EMOJI answers (ED-2). Equivalent to "[sanitizeToEmoji] would leave [text] unchanged".
 */
fun isEmojiOnly(text: String): Boolean = text.isNotEmpty() && sanitizeToEmoji(text) == text

/** Removes the last whole emoji from [text] — a backspace for an emoji answer string (ED-13). */
fun dropLastEmoji(text: String): String = extractEmojiWords(text).dropLast(1).joinToString("")

private fun combineSurrogates(high: Char, low: Char): Int =
    0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)

/** A code point that begins (or is) an emoji. */
private fun isEmojiStart(codePoint: Int): Boolean = when (codePoint) {
    in 0x1F000..0x1FAFF -> true   // emoticons, pictographs, transport, supplemental, flags
    in 0x2600..0x27BF -> true     // miscellaneous symbols and dingbats
    in 0x2B00..0x2BFF -> true     // miscellaneous symbols and arrows
    in 0x2300..0x23FF -> true     // miscellaneous technical (⌚ ⏰ ▶ …)
    in 0x25A0..0x25FF -> true     // geometric shapes
    in 0x2190..0x21FF -> true     // arrows
    0x2122, 0x2139, 0x203C, 0x2049, 0x24C2, 0x3030, 0x303D, 0x3297, 0x3299 -> true
    else -> false
}

/** Code points that extend an emoji already in progress (joiner, variation, skin tone, keycap). */
private fun isEmojiJoiner(codePoint: Int): Boolean =
    codePoint == 0x200D || codePoint == 0x20E3 ||
        codePoint in 0xFE00..0xFE0F || codePoint in 0x1F3FB..0x1F3FF

/** A code point that can be the base of a keycap emoji: digits 0-9, '#', and '*'. */
private fun isKeycapBase(codePoint: Int): Boolean =
    codePoint in 0x30..0x39 || codePoint == 0x23 || codePoint == 0x2A
