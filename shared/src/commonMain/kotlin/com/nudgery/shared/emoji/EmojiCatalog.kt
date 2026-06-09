// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.emoji

/**
 * Public entry point to the generated emoji catalog (ENGINEERING_DECISIONS.md ED-5). The generated
 * backing object is internal; consumers (the picker) depend on this stable surface and the
 * [EmojiCatalogEntry] model, not on how the data was produced.
 */
object EmojiCatalog {
    val entries: List<EmojiCatalogEntry> get() = GeneratedEmojiCatalog.entries
}
