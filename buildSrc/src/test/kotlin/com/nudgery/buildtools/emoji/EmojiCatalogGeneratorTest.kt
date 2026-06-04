package com.nudgery.buildtools.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogGeneratorTest {

    // A base emoji with a skin-tone variant, a base with a hair variant, and a plain base with neither.
    private val sample = """
        # group: People & Body

        # subgroup: hand-fingers-open
        1F44B                ; fully-qualified     # 👋 E0.6 waving hand
        1F44B 1F3FD          ; fully-qualified     # 👋🏽 E1.0 waving hand: medium skin tone

        # subgroup: person
        1F9D1                ; fully-qualified     # 🧑 E5.0 person
        1F9D1 1F3FB          ; fully-qualified     # 🧑🏻 E5.0 person: light skin tone
        1F9D1 200D 1F9B0     ; fully-qualified     # 🧑‍🦰 E12.1 person: red hair
        1F9D1 1F3FD 200D 1F9B0 ; fully-qualified   # 🧑🏽‍🦰 E12.1 person: medium skin tone, red hair

        # subgroup: face-smiling
        1F600                ; fully-qualified     # 😀 E1.0 grinning face
    """.trimIndent()

    private fun baseConcepts() = EmojiCatalogGenerator.baseConcepts(EmojiTestParser.parse(sample))

    @Test
    fun derivesOnlyTonelessHairlessBaseConcepts() {
        // ED-5: the catalog holds base concepts; toned and hair variants are excluded (derived by rule).
        val names = baseConcepts().map { it.name }
        assertEquals(listOf("waving hand", "person", "grinning face"), names)
    }

    @Test
    fun flagsSkinToneAndHairCapability() {
        // ED-6/ED-8: a base is flagged when a toned or hair variant of it exists.
        val byName = baseConcepts().associateBy { it.name }

        assertTrue("waving hand has a skin-tone variant", byName.getValue("waving hand").acceptsSkinTone)
        assertFalse("waving hand has no hair variant", byName.getValue("waving hand").hairCapable)

        assertTrue("person has a skin-tone variant", byName.getValue("person").acceptsSkinTone)
        assertTrue("person has a hair variant", byName.getValue("person").hairCapable)

        assertFalse("grinning face takes no skin tone", byName.getValue("grinning face").acceptsSkinTone)
        assertFalse(byName.getValue("grinning face").hairCapable)
    }

    @Test
    fun emitsChunkedCompilableLookingSource() {
        // ED-5: source is emitted as chunked listOf() functions under the runtime package.
        val source = EmojiCatalogGenerator.generateSource(baseConcepts())
        assertTrue(source.contains("package com.nudgery.shared.emoji"))
        assertTrue(source.contains("internal object GeneratedEmojiCatalog"))
        assertTrue(source.contains("private fun chunk0()"))
        assertTrue(source.contains("addAll(chunk0())"))
        // The plain base emoji appears with both capability flags false.
        assertTrue(source.contains("EmojiCatalogEntry(\"😀\", \"grinning face\""))
    }

    @Test
    fun chunksLargeInputAcrossMultipleFunctions() {
        // ED-5: more than CHUNK_SIZE entries split into multiple functions (64 KB method-limit guard).
        val many = (1..(EmojiCatalogGenerator.CHUNK_SIZE + 5)).map {
            EmojiCatalogGenerator.BaseConcept("😀", "e$it", "g", "s", "1.0", false, false)
        }
        val source = EmojiCatalogGenerator.generateSource(many)
        assertTrue("expected a second chunk", source.contains("private fun chunk1()"))
        assertTrue(source.contains("addAll(chunk1())"))
    }

    @Test
    fun attachesKeywordsByNormalizedEmoji() {
        // ED-10: CLDR keywords are matched to base concepts by their FE0F-normalized emoji.
        val keywords = mapOf(CldrAnnotationParser.normalizeKey("😀") to listOf("happy", "smile"))
        val concepts = EmojiCatalogGenerator.baseConcepts(EmojiTestParser.parse(sample), keywords)
        assertEquals(listOf("happy", "smile"), concepts.first { it.name == "grinning face" }.keywords)
        assertEquals(emptyList<String>(), concepts.first { it.name == "person" }.keywords)
    }

    @Test
    fun emitsKeywordsAsListLiteralOrEmptyList() {
        // ED-10: keywords are emitted as listOf("…"); absent keywords become emptyList().
        val withKeywords = listOf(
            EmojiCatalogGenerator.BaseConcept("😀", "grinning face", "g", "s", "1.0", false, false, listOf("happy", "smile"))
        )
        assertTrue(EmojiCatalogGenerator.generateSource(withKeywords).contains("listOf(\"happy\", \"smile\")"))

        val withoutKeywords = listOf(
            EmojiCatalogGenerator.BaseConcept("😀", "grinning face", "g", "s", "1.0", false, false, emptyList())
        )
        assertTrue(EmojiCatalogGenerator.generateSource(withoutKeywords).contains("false, false, emptyList())"))
    }

    @Test
    fun escapesStringLiteralSpecialCharacters() {
        val tricky = listOf(
            EmojiCatalogGenerator.BaseConcept("😀", "price \$5 \"x\"", "g", "s", "1.0", false, false)
        )
        val source = EmojiCatalogGenerator.generateSource(tricky)
        assertTrue(source.contains("""price \${'$'}5 \"x\""""))
    }
}
