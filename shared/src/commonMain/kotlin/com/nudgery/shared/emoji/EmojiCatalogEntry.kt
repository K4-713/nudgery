package com.nudgery.shared.emoji

/**
 * One emoji in the generated catalog (ENGINEERING_DECISIONS.md ED-5): a base concept — no skin-tone
 * modifier, no hair component — flagged with the variants that can be derived by rule. Produced at
 * build time by the `:shared:generateEmojiCatalog` task from Unicode's `emoji-test.txt`.
 *
 * Consumers (search, picker, defaults) depend on this model, not on how it was generated, so the
 * underlying storage shape stays swappable (see ED-5).
 */
data class EmojiCatalogEntry(
    /** The base emoji string (device-font rendered — ED-3). */
    val emoji: String,
    /** CLDR short name, e.g. `"grinning face"`. */
    val name: String,
    val group: String,
    val subgroup: String,
    /** Emoji version the concept was introduced in, e.g. `"5.0"`. */
    val emojiVersion: String,
    /** True when a Fitzpatrick skin-tone variant of this concept exists (ED-6). */
    val acceptsSkinTone: Boolean,
    /** True when a hair-component variant of this concept exists (ED-8, pick-time only). */
    val hairCapable: Boolean,
    /** CLDR search keywords for typeahead (ED-10); empty when none are annotated. */
    val keywords: List<String> = emptyList(),
)
