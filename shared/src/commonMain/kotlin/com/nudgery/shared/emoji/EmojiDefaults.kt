package com.nudgery.shared.emoji

/** The user's default emoji skin tone (ED-6). [DEFAULT] applies no modifier (platform neutral form). */
enum class SkinTone(val modifier: Int?) {
    DEFAULT(null),
    LIGHT(0x1F3FB),
    MEDIUM_LIGHT(0x1F3FC),
    MEDIUM(0x1F3FD),
    MEDIUM_DARK(0x1F3FE),
    DARK(0x1F3FF),
}

/** The user's default emoji gender (ED-7). */
enum class Gender { NEUTRAL, WOMAN, MAN }

/**
 * Applies the user's default skin tone (ED-6) and gender (ED-7) to a picked emoji, by rule.
 *
 * Both defaults apply **only** to a base/neutral form that supports the variant and **never**
 * override a variant the user explicitly picked; they compose (neutral person → woman → woman +
 * tone). Gender is resolved from a mapping derived from the catalog (neutral ↔ woman ↔ man); skin
 * tone is inserted after the emoji's base component, superseding a following VS16 presentation
 * selector. Older-adult and child figures are not gender-mapped in v1 (their neutral/woman/man forms
 * are unrelated code points the structural derivation can't link).
 */
object EmojiDefaults {

    private val PERSON_COMPONENTS = setOf(0x1F9D1, 0x1F468, 0x1F469) // person, man, woman
    private const val FEMALE_SIGN = 0x2640
    private const val MALE_SIGN = 0x2642
    private const val ZWJ = 0x200D
    private const val PERSON_PLACEHOLDER = -1 // unifies person/man/woman when neutralizing a key

    /**
     * All distinct skin-tone × gender variants of [entry], base form first (ED-8 long-press tray).
     * A single-element result means the emoji has no such variants. Hair/direction variants are not
     * included in v1.
     */
    fun variants(entry: EmojiCatalogEntry): List<String> {
        val out = LinkedHashSet<String>()
        for (gender in Gender.entries) for (tone in SkinTone.entries) out.add(apply(entry, tone, gender))
        return out.toList()
    }

    /**
     * The emoji of the explicitly-gendered (woman/man) forms of every genderable concept in
     * [entries] that *also* has a neutral form there (ED-7). The picker hides these from its
     * top-level grid: the neutral concept stands in for the whole group — shown in the user's
     * default gender (ED-7) and expandable to every variant on long-press (ED-8) — so a gendered
     * form never gets its own cell (which would otherwise duplicate the default-gender form).
     *
     * Computed from the same (already device-filtered, ED-4) [entries] the grid shows, so a group is
     * only collapsed when its neutral form is actually present to represent it.
     */
    fun foldedGenderVariantEmoji(entries: List<EmojiCatalogEntry>): Set<String> =
        buildGenderGroups(entries).values
            .flatMap { byGender -> byGender.filterKeys { it != Gender.NEUTRAL }.values }
            .toSet()

    /** Applies [gender] then [skinTone] to a picked catalog [entry]. */
    fun apply(entry: EmojiCatalogEntry, skinTone: SkinTone, gender: Gender): String {
        var emoji = applyGender(entry.emoji, gender)
        if (skinTone != SkinTone.DEFAULT && entry.acceptsSkinTone) {
            emoji = applySkinTone(emoji, skinTone)
        }
        return emoji
    }

    /**
     * Returns the [gender] variant of [emoji] if it is a neutral gendered concept; otherwise returns
     * [emoji] unchanged (it has no gendered forms, or it is already explicitly gendered).
     */
    fun applyGender(emoji: String, gender: Gender): String {
        if (gender == Gender.NEUTRAL) return emoji
        val codePoints = decodeCodePoints(emoji)
        if (classifyGender(codePoints) != Gender.NEUTRAL) return emoji // explicit choice, don't override
        val group = genderGroups[genderNeutralKey(codePoints)] ?: return emoji
        return group[gender] ?: emoji
    }

    /**
     * Inserts [skinTone]'s modifier after the emoji's base component (superseding a VS16 right after
     * it). No-op when [skinTone] is [SkinTone.DEFAULT] or [emoji] is already skin-toned.
     */
    fun applySkinTone(emoji: String, skinTone: SkinTone): String {
        val modifier = skinTone.modifier ?: return emoji
        val codePoints = decodeCodePoints(emoji)
        if (codePoints.isEmpty() || codePoints.any { it in 0x1F3FB..0x1F3FF }) return emoji
        val out = ArrayList<Int>(codePoints.size + 1)
        out.add(codePoints[0])
        out.add(modifier)
        // The modifier supersedes the base component's emoji-presentation selector, if present.
        val rest = if (codePoints.size > 1 && codePoints[1] == 0xFE0F) 2 else 1
        for (i in rest until codePoints.size) out.add(codePoints[i])
        return encodeCodePoints(out)
    }

    // --- Gender mapping derived from the catalog (ED-7) ---

    private val genderGroups: Map<String, Map<Gender, String>> by lazy {
        buildGenderGroups(GeneratedEmojiCatalog.entries)
    }

    /** Groups single-person genderable concepts by their gender-neutralized key into {gender → emoji}. */
    internal fun buildGenderGroups(entries: List<EmojiCatalogEntry>): Map<String, Map<Gender, String>> =
        entries
            .map { it.emoji to decodeCodePoints(it.emoji) }
            .filter { (_, cps) -> isSinglePerson(cps) }
            .groupBy({ (_, cps) -> genderNeutralKey(cps) }, { (emoji, cps) -> classifyGender(cps) to emoji })
            .mapValues { (_, pairs) -> pairs.toMap() }
            .filter { (_, byGender) -> Gender.NEUTRAL in byGender && byGender.size > 1 }

    /** WOMAN if a woman component/female sign is present, MAN for the male counterparts, else NEUTRAL. */
    private fun classifyGender(codePoints: List<Int>): Gender = when {
        codePoints.any { it == 0x1F469 || it == FEMALE_SIGN } -> Gender.WOMAN
        codePoints.any { it == 0x1F468 || it == MALE_SIGN } -> Gender.MAN
        else -> Gender.NEUTRAL
    }

    /** Excludes couples/families so only single-person concepts are gender-mapped. */
    private fun isSinglePerson(codePoints: List<Int>): Boolean =
        codePoints.count { it in PERSON_COMPONENTS } <= 1

    /** A key invariant across a concept's gender forms: person components unified, gender signs and variation selectors removed. */
    private fun genderNeutralKey(codePoints: List<Int>): String {
        val out = ArrayList<Int>(codePoints.size)
        for (cp in codePoints) {
            when {
                cp in PERSON_COMPONENTS -> out.add(PERSON_PLACEHOLDER)
                cp == FEMALE_SIGN || cp == MALE_SIGN -> {
                    if (out.isNotEmpty() && out.last() == ZWJ) out.removeAt(out.lastIndex)
                }
                cp in 0xFE00..0xFE0F -> Unit
                else -> out.add(cp)
            }
        }
        return out.joinToString(",")
    }
}

/** Decodes a string into Unicode code points (handles surrogate pairs). */
private fun decodeCodePoints(text: String): List<Int> {
    val out = ArrayList<Int>(text.length)
    var i = 0
    while (i < text.length) {
        val high = text[i]
        if (high.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
            out.add(0x10000 + ((high.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00))
            i += 2
        } else {
            out.add(high.code)
            i++
        }
    }
    return out
}

/** Encodes code points back into a string (astral code points become surrogate pairs). */
private fun encodeCodePoints(codePoints: List<Int>): String = buildString {
    for (cp in codePoints) {
        if (cp <= 0xFFFF) {
            append(cp.toChar())
        } else {
            val offset = cp - 0x10000
            append((0xD800 + (offset shr 10)).toChar())
            append((0xDC00 + (offset and 0x3FF)).toChar())
        }
    }
}
