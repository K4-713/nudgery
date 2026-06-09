// SPDX-License-Identifier: CC0-1.0

package com.nudgery.buildtools.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CldrAnnotationParserTest {

    private val annotations = """
        <ldml>
          <annotations>
            <annotation cp="😀">cheerful | grin | happy</annotation>
            <annotation cp="😀" type="tts">grinning face</annotation>
          </annotations>
        </ldml>
    """.trimIndent()

    private val derived = """
        <annotation cp="😀">smile | teeth</annotation>
    """.trimIndent()

    @Test
    fun parsesKeywordsAndIgnoresTheTtsShortName() {
        val map = CldrAnnotationParser.parseKeywords(annotations)
        val key = CldrAnnotationParser.normalizeKey("😀")
        assertEquals(listOf("cheerful", "grin", "happy"), map[key])
        assertFalse("the tts short name is not a keyword", map.getValue(key).contains("grinning face"))
    }

    @Test
    fun mergesAcrossDocumentsAndDeduplicates() {
        val map = CldrAnnotationParser.parseKeywords(annotations, derived)
        assertEquals(listOf("cheerful", "grin", "happy", "smile", "teeth"), map[CldrAnnotationParser.normalizeKey("😀")])
    }

    @Test
    fun normalizeKeyStripsFe0fSoFullyQualifiedFormsMatch() {
        // CLDR cp omits the U+FE0F that emoji-test.txt's fully-qualified form carries.
        val map = CldrAnnotationParser.parseKeywords("""<annotation cp="A">k1 | k2</annotation>""")
        assertEquals("A", CldrAnnotationParser.normalizeKey("A\uFE0F"))
        assertEquals(listOf("k1", "k2"), map[CldrAnnotationParser.normalizeKey("A\uFE0F")])
    }

    @Test
    fun unescapesXmlEntities() {
        val map = CldrAnnotationParser.parseKeywords("""<annotation cp="X">rock &amp; roll | a &lt; b</annotation>""")
        assertEquals(listOf("rock & roll", "a < b"), map["X"])
    }
}
