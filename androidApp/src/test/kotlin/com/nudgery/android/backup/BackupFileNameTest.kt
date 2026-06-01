package com.nudgery.android.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileNameTest {

    @Test
    fun TDD_usesNudgeNameWhenReadable() {
        // README "Nudge Backup and Restore": single-nudge backup filename matches the nudge name
        assertEquals("Good Dog Sightings", nudgeBackupFileName("Good Dog Sightings"))
        assertEquals("Cozy Things Enjoyed", nudgeBackupFileName("Cozy Things Enjoyed"))
    }

    @Test
    fun TDD_replacesIllegalCharactersWithSpaces() {
        // Filesystem-unsafe characters must not appear in the filename
        assertEquals("Sky Majesty Meter", nudgeBackupFileName("Sky / Majesty: Meter?"))
        assertEquals("Tea Count", nudgeBackupFileName("  Tea   Count  "))
    }

    @Test
    fun TDD_keepsLettersDigitsHyphensUnderscores() {
        assertEquals("mood-tracker_2", nudgeBackupFileName("mood-tracker_2"))
    }

    @Test
    fun TDD_allEmojiNameUsesUnicodeNames() {
        // "unless the nudge name is all emoji, then use the emoji's name"
        val dog = nudgeBackupFileName("🐶")
        assertTrue("expected dog-face style slug, got '$dog'", dog.contains("dog"))
        assertEquals(dog, dog.lowercase()) // emoji-derived names are lowercase slugs

        val sparkleStar = nudgeBackupFileName("🌟✨")
        assertTrue("expected combined emoji names, got '$sparkleStar'",
            sparkleStar.contains("star") && sparkleStar.contains("sparkle"))
    }

    @Test
    fun TDD_mixedEmojiAndTextPrefersText() {
        assertEquals("Walks", nudgeBackupFileName("🐶 Walks"))
    }

    @Test
    fun TDD_blankOrUnnamedFallsBackToNudge() {
        assertEquals("nudge", nudgeBackupFileName(""))
        assertEquals("nudge", nudgeBackupFileName("   "))
    }

    @Test
    fun TDD_disambiguateNameAppendsSuffixOnlyWhenTaken() {
        // Backing up / restoring many nudges must not collide on duplicate names
        assertEquals("Tea", disambiguateName("Tea", emptySet()))
        assertEquals("Tea (2)", disambiguateName("Tea", setOf("Tea")))
        assertEquals("Tea (3)", disambiguateName("Tea", setOf("Tea", "Tea (2)")))
    }
}
