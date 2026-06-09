// SPDX-License-Identifier: CC0-1.0

package com.nudgery.buildtools.emoji

/**
 * Parses Unicode's `emoji-test.txt` (UTS #51 test data) into structured entries. This is a
 * **build-time** tool (ENGINEERING_DECISIONS.md ED-5): it runs during the `generateEmojiCatalog`
 * Gradle task to produce the generated runtime catalog, and is never shipped in the app. Format:
 *
 * ```
 * 1F468 200D 2695 FE0F ; fully-qualified # 👨‍⚕️ E4.0 man health worker
 * ```
 *
 * with `# group:` / `# subgroup:` comment lines partitioning the entries (CLDR display order).
 */
object EmojiTestParser {

    fun parse(text: String): List<EmojiTestEntry> {
        val entries = ArrayList<EmojiTestEntry>()
        var group = ""
        var subgroup = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("# group:") -> group = line.substringAfter("# group:").trim()
                line.startsWith("# subgroup:") -> subgroup = line.substringAfter("# subgroup:").trim()
                line.isEmpty() || line.startsWith("#") -> Unit // license header, notes, blanks
                else -> parseEntry(line, group, subgroup)?.let(entries::add)
            }
        }
        return entries
    }

    private fun parseEntry(line: String, group: String, subgroup: String): EmojiTestEntry? {
        val semicolon = line.indexOf(';')
        if (semicolon < 0) return null
        val codePoints = line.substring(0, semicolon).trim()
            .split(' ').filter { it.isNotEmpty() }.map { it.toInt(16) }
        if (codePoints.isEmpty()) return null

        val afterStatus = line.substring(semicolon + 1)
        val hash = afterStatus.indexOf('#')
        if (hash < 0) return null
        val qualification = EmojiQualification.fromToken(afterStatus.substring(0, hash).trim())

        // Comment is "<emoji> E<version> <name>": the emoji glyph has no spaces, so split into 3.
        val comment = afterStatus.substring(hash + 1).trim()
        val parts = comment.split(Regex("\\s+"), limit = 3)
        val version = parts.getOrNull(1)?.removePrefix("E").orEmpty()
        val name = parts.getOrNull(2).orEmpty()

        val emoji = StringBuilder().apply { codePoints.forEach { appendCodePoint(it) } }.toString()
        return EmojiTestEntry(codePoints, emoji, qualification, version, name, group, subgroup)
    }
}

/** One parsed row of `emoji-test.txt`. */
data class EmojiTestEntry(
    val codePoints: List<Int>,
    val emoji: String,
    val qualification: EmojiQualification,
    /** Emoji version introduced in, e.g. `"4.0"` (the `E4.0` token, prefix stripped). */
    val emojiVersion: String,
    /** CLDR short name, e.g. `"man health worker"`. */
    val name: String,
    val group: String,
    val subgroup: String,
) {
    /** True when a Fitzpatrick skin-tone modifier (U+1F3FB–U+1F3FF) is present (ED-6). */
    val hasSkinToneModifier: Boolean get() = codePoints.any { it in 0x1F3FB..0x1F3FF }

    /** True when a hair component (U+1F9B0–U+1F9B3: red, curly, white, bald) is present (ED-8). */
    val hasHairComponent: Boolean get() = codePoints.any { it in 0x1F9B0..0x1F9B3 }
}

enum class EmojiQualification {
    FULLY_QUALIFIED,
    MINIMALLY_QUALIFIED,
    UNQUALIFIED,
    COMPONENT;

    companion object {
        fun fromToken(token: String): EmojiQualification = when (token) {
            "fully-qualified" -> FULLY_QUALIFIED
            "minimally-qualified" -> MINIMALLY_QUALIFIED
            "unqualified" -> UNQUALIFIED
            "component" -> COMPONENT
            else -> throw IllegalArgumentException("Unknown emoji-test.txt status: '$token'")
        }
    }
}
