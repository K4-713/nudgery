package com.nudgery.shared

import kotlin.test.Test

class DataExportTest {

    @Test
    fun TDD_exportProducesCsvFile() {
        // README "Viewing Nudges": "a raw table of the answer data which can be exported
        //   to a CSV/TSV file"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_exportProducesTsvFile() {
        // README "Viewing Nudges": "...to a CSV/TSV file"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_exportRowContainsAnswerValue() {
        // ARCHITECTURE.md "Export produces one row per Answer, joined with its Question,
        //   Nudge, and any relevant QuestionOption text"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_exportRowContainsQuestionText() {
        // ARCHITECTURE.md "...joined with its Question..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_exportRowContainsNudgeName() {
        // ARCHITECTURE.md "...joined with its...Nudge..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_exportRowContainsOptionTextForOptionTypeAnswers() {
        // ARCHITECTURE.md "...and any relevant QuestionOption text"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_exportRowContainsAnswerTimestamp() {
        // ARCHITECTURE.md Answer.recordedAt — timestamp should be included in the export record
        TODO("TDD skeleton")
    }
}
