package com.nudgery.android.backup

import kotlinx.datetime.LocalDate

/** Compact `YYYYMMDD` form of [date] for export filenames (e.g. 20260601). */
fun compactExportDate(date: LocalDate): String =
    "${date.year}" +
        date.monthNumber.toString().padStart(2, '0') +
        date.dayOfMonth.toString().padStart(2, '0')

/**
 * Base filename (no extension) for a single nudge's export on [date], e.g.
 * "Good Dog Sightings-nudge-20260601". Always carries the nudge name, the word "nudge", and the date.
 */
fun nudgeExportFileBase(nudgeName: String, date: LocalDate): String =
    "${nudgeBackupFileName(nudgeName)}-nudge-${compactExportDate(date)}"

/** Filename (no extension) for the all-nudges backup archive on [date], e.g. "nudges-20260601". */
fun allNudgesBackupFileBase(date: LocalDate): String = "nudges-${compactExportDate(date)}"

/**
 * Builds a human-friendly base filename (no extension) for a nudge backup from the nudge's name.
 *
 * Letters, digits, spaces, hyphens and underscores are kept; any other character (punctuation,
 * emoji, etc.) becomes a space, which is then collapsed. If nothing readable survives — e.g. the
 * name is entirely emoji — the file is named after the emoji's Unicode names instead
 * (🐶 → "dog-face", 🌟✨ → "glowing-star-sparkles"). Falls back to "nudge" when all else fails.
 */
fun nudgeBackupFileName(nudgeName: String): String {
    val readable = buildString {
        nudgeName.forEach { c ->
            append(if (c.isLetterOrDigit() || c == '-' || c == '_') c else ' ')
        }
    }.replace(Regex("\\s+"), " ").trim()

    val base = if (readable.isNotEmpty()) readable else emojiNameOf(nudgeName)
    return base.take(MAX_FILENAME_LENGTH).trim().ifBlank { FALLBACK_NAME }
}

private const val MAX_FILENAME_LENGTH = 60
private const val MAX_EMOJI_PARTS = 4
private const val FALLBACK_NAME = "nudge"

/** Joins the Unicode names of up to [MAX_EMOJI_PARTS] meaningful code points into a slug. */
private fun emojiNameOf(name: String): String {
    val parts = mutableListOf<String>()
    var index = 0
    while (index < name.length && parts.size < MAX_EMOJI_PARTS) {
        val codePoint = name.codePointAt(index)
        index += Character.charCount(codePoint)
        if (Character.isWhitespace(codePoint) || isEmojiModifier(codePoint)) continue
        val unicodeName = Character.getName(codePoint) ?: continue
        val slug = unicodeName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        if (slug.isNotEmpty() && slug != parts.lastOrNull()) parts.add(slug)
    }
    return parts.joinToString("-")
}

/**
 * Returns [desired] if it is not already in [taken], otherwise appends " (2)", " (3)", … until the
 * result is unique. Used to keep nudge names / backup filenames distinct when backing up or
 * restoring many nudges at once.
 */
fun disambiguateName(desired: String, taken: Set<String>): String {
    if (desired !in taken) return desired
    var suffix = 2
    while ("$desired ($suffix)" in taken) suffix++
    return "$desired ($suffix)"
}

/** Code points that modify or join emoji and carry no useful name on their own. */
private fun isEmojiModifier(codePoint: Int): Boolean =
    codePoint == 0x200D ||            // zero-width joiner
    codePoint in 0xFE00..0xFE0F ||    // variation selectors
    codePoint in 0x1F3FB..0x1F3FF ||  // skin-tone modifiers
    codePoint == 0x20E3               // combining enclosing keycap
