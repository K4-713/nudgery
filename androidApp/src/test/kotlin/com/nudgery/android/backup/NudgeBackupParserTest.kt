// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.backup

import com.nudgery.shared.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeBackupParserTest {

    @Test
    fun TDD_parsesOneYesPerDayFromBackup() {
        // ED-17: the One Yes Per Day flag round-trips through backups; absent (older backups) ⇒ false.
        val json = """
            {
              "nudge": { "name": "Headache", "isEnabled": true },
              "questions": [
                { "orderIndex": 0, "text": "Headache?", "type": "YES_NO", "collapsePerDay": true },
                { "orderIndex": 1, "text": "Worse than usual?", "type": "YES_NO" }
              ],
              "answers": []
            }
        """.trimIndent()

        val result = NudgeBackupParser().parse(json)
        assertTrue("expected successful parse, got $result", result is NudgeBackupParser.ParseResult.Success)
        val request = (result as NudgeBackupParser.ParseResult.Success).request
        assertTrue("explicit true is parsed", request.questions[0].collapsePerDay)
        assertFalse("absent defaults to false", request.questions[1].collapsePerDay)
    }

    @Test
    fun TDD_parsesEmojiQuestionTypeFromBackup() {
        // ED-1: EMOJI is serialized by name like every other question type, so backups round-trip it
        // (export writes `"type": "EMOJI"`; this verifies the import side reconstructs it).
        val json = """
            {
              "nudge": { "name": "Mood", "isEnabled": true },
              "questions": [
                { "orderIndex": 0, "text": "How do you feel?", "type": "EMOJI" }
              ],
              "answers": [
                {
                  "questionOrderIndex": 0,
                  "value": "😀",
                  "scheduledAt": "2026-06-01T12:00:00Z",
                  "answeredAt": "2026-06-01T12:00:00Z"
                }
              ]
            }
        """.trimIndent()

        val result = NudgeBackupParser().parse(json)
        assertTrue("expected successful parse, got $result", result is NudgeBackupParser.ParseResult.Success)
        val request = (result as NudgeBackupParser.ParseResult.Success).request
        assertEquals(QuestionType.EMOJI, request.questions.single().type)
        assertEquals("😀", request.answers.single().value)
    }
}
