package com.nudgery.buildtools.emoji

/**
 * Turns parsed [EmojiTestEntry]s into the runtime emoji catalog source (ENGINEERING_DECISIONS.md
 * ED-5). Build-time only.
 *
 * The catalog holds **base concepts** — fully-qualified emoji with no skin-tone modifier and no hair
 * component — each flagged with whether it `acceptsSkinTone` and is `hairCapable`, so the picker can
 * derive those variants by rule (ED-6/ED-8) instead of enumerating thousands of combinations.
 *
 * The source is emitted as chunked `listOf(...)` functions: a single initializer for ~1,900 entries
 * would exceed the JVM's 64 KB per-method bytecode limit, so entries are split across [CHUNK_SIZE]-d
 * functions. (If keyword data — ED-10 — later pushes generated size too high, the documented
 * fallback is a packed-string representation; see ED-5.)
 */
object EmojiCatalogGenerator {

    /**
     * Entries per generated chunk function, kept well under the 64 KB per-method bytecode limit.
     * Sized conservatively because each entry also carries a CLDR keyword list (ED-10).
     */
    const val CHUNK_SIZE = 100

    private const val PACKAGE = "com.nudgery.shared.emoji"

    data class BaseConcept(
        val emoji: String,
        val name: String,
        val group: String,
        val subgroup: String,
        val emojiVersion: String,
        val acceptsSkinTone: Boolean,
        val hairCapable: Boolean,
        /** CLDR search keywords (ED-10); empty when none are annotated. */
        val keywords: List<String> = emptyList(),
    )

    /**
     * Derives the catalog's base concepts (variant-capability flags + search keywords) from all
     * entries. [keywordsByKey] maps an FE0F-normalized emoji to its CLDR keywords (see
     * [CldrAnnotationParser]); concepts with no annotation get an empty list.
     */
    fun baseConcepts(
        entries: List<EmojiTestEntry>,
        keywordsByKey: Map<String, List<String>> = emptyMap(),
    ): List<BaseConcept> {
        val fullyQualified = entries.filter { it.qualification == EmojiQualification.FULLY_QUALIFIED }

        // A base concept appears with a skin tone if some toned entry, with its tone stripped, equals
        // it; it is hair-capable if some hair entry, with hair (and the joining ZWJ) and any tone
        // stripped, equals it.
        val skinToneBaseKeys: Set<List<Int>> = fullyQualified
            .filter { it.hasSkinToneModifier }
            .map { it.codePoints.filterNot { cp -> cp in 0x1F3FB..0x1F3FF } }
            .toSet()
        val hairBaseKeys: Set<List<Int>> = fullyQualified
            .filter { it.hasHairComponent }
            .map { normalizeToBase(it.codePoints) }
            .toSet()

        return fullyQualified
            .filterNot { it.hasSkinToneModifier || it.hasHairComponent }
            .map { entry ->
                BaseConcept(
                    emoji = entry.emoji,
                    name = entry.name,
                    group = entry.group,
                    subgroup = entry.subgroup,
                    emojiVersion = entry.emojiVersion,
                    acceptsSkinTone = entry.codePoints in skinToneBaseKeys,
                    hairCapable = entry.codePoints in hairBaseKeys,
                    keywords = keywordsByKey[CldrAnnotationParser.normalizeKey(entry.emoji)].orEmpty(),
                )
            }
    }

    /** Strips skin-tone modifiers, hair components, and each hair component's joining ZWJ. */
    private fun normalizeToBase(codePoints: List<Int>): List<Int> {
        val out = ArrayList<Int>(codePoints.size)
        for (cp in codePoints) {
            when {
                cp in 0x1F3FB..0x1F3FF -> Unit // skin tone
                cp in 0x1F9B0..0x1F9B3 -> {     // hair component: also drop the ZWJ we just added
                    if (out.isNotEmpty() && out.last() == 0x200D) out.removeAt(out.lastIndex)
                }
                else -> out.add(cp)
            }
        }
        return out
    }

    fun generateSource(baseConcepts: List<BaseConcept>): String {
        val chunks = baseConcepts.chunked(CHUNK_SIZE)
        return buildString {
            appendLine("// GENERATED FILE — DO NOT EDIT.")
            appendLine("// Source: emoji-data/emoji-test.txt (Unicode emoji-test.txt, UTS #51).")
            appendLine("// Produced by the :shared:generateEmojiCatalog Gradle task (ENGINEERING_DECISIONS.md ED-5).")
            appendLine("package $PACKAGE")
            appendLine()
            appendLine("internal object GeneratedEmojiCatalog {")
            appendLine("    val entries: List<EmojiCatalogEntry> = buildList(${baseConcepts.size}) {")
            chunks.indices.forEach { appendLine("        addAll(chunk$it())") }
            appendLine("    }")
            chunks.forEachIndexed { index, chunk ->
                appendLine()
                appendLine("    private fun chunk$index(): List<EmojiCatalogEntry> = listOf(")
                chunk.forEach { c ->
                    appendLine(
                        "        EmojiCatalogEntry(\"${esc(c.emoji)}\", \"${esc(c.name)}\", " +
                            "\"${esc(c.group)}\", \"${esc(c.subgroup)}\", \"${esc(c.emojiVersion)}\", " +
                            "${c.acceptsSkinTone}, ${c.hairCapable}, ${keywordsLiteral(c.keywords)}),"
                    )
                }
                appendLine("    )")
            }
            appendLine("}")
        }
    }

    /** Renders a keyword list as a Kotlin `listOf("…")` literal, or `emptyList()` when empty. */
    private fun keywordsLiteral(keywords: List<String>): String =
        if (keywords.isEmpty()) "emptyList()"
        else keywords.joinToString(prefix = "listOf(", postfix = ")") { "\"${esc(it)}\"" }

    /** Escapes the characters that are special inside a Kotlin double-quoted string literal. */
    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
}
