package com.nudgery.buildtools.emoji

/**
 * Parses Unicode CLDR annotation XML (`common/annotations/en.xml` and `annotationsDerived/en.xml`)
 * into per-emoji search keywords (ENGINEERING_DECISIONS.md ED-10). Build-time only.
 *
 * Each emoji has two annotation lines:
 * ```
 * <annotation cp="😀">cheerful | grin | happy | smile</annotation>   <!-- keywords -->
 * <annotation cp="😀" type="tts">grinning face</annotation>          <!-- short name -->
 * ```
 * Only the keyword line (no `type`) is collected; the short name already comes from emoji-test.txt.
 */
object CldrAnnotationParser {

    private val ANNOTATION = Regex("""<annotation cp="(.*?)"( type="tts")?>(.*?)</annotation>""")

    /**
     * Returns emoji (FE0F-normalized) -> ordered, de-duplicated keyword list, merged across the
     * given XML documents (later documents add to earlier ones).
     */
    fun parseKeywords(vararg annotationXml: String): Map<String, List<String>> {
        val byKey = LinkedHashMap<String, LinkedHashSet<String>>()
        for (xml in annotationXml) {
            for (match in ANNOTATION.findAll(xml)) {
                if (match.groupValues[2].isNotEmpty()) continue // type="tts" is the short name, not keywords
                val key = normalizeKey(unescapeXml(match.groupValues[1]))
                val keywords = unescapeXml(match.groupValues[3])
                    .split('|').map { it.trim() }.filter { it.isNotEmpty() }
                if (keywords.isNotEmpty()) byKey.getOrPut(key) { LinkedHashSet() }.addAll(keywords)
            }
        }
        return byKey.mapValues { it.value.toList() }
    }

    /**
     * Matches emoji-test.txt's fully-qualified forms to CLDR's by ignoring the U+FE0F emoji
     * presentation selector, which CLDR's `cp` attribute typically omits.
     */
    fun normalizeKey(emoji: String): String = emoji.filter { it != '\uFE0F' }

    private fun unescapeXml(s: String): String = s
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")
        .replace("&amp;", "&") // last, so "&amp;lt;" doesn't become "<"
}
