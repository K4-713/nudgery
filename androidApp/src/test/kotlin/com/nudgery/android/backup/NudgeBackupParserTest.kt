package com.nudgery.android.backup

import com.nudgery.shared.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeBackupParserTest {

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
