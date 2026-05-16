package com.nudgery.shared

import kotlin.test.Test

class AnswerRecordingTest {

    @Test
    fun TDD_answerRecordedWithNudgeAndQuestionReference() {
        // ARCHITECTURE.md Answer: nudgeId FK → Nudge, questionId FK → Question
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_answerRecordedWithTimestamp() {
        // ARCHITECTURE.md Answer.recordedAt: "Instant"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_answerIsHiddenDefaultsFalse() {
        // ARCHITECTURE.md Answer.isHidden: hidden rows excluded from visualizations;
        //   newly recorded answers are visible by default
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_answerCanBeMarkedHidden() {
        // README "Editing Nudges": "you can select individual answers and tag them as hidden"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_hiddenAnswersExcludedFromVisualizationData() {
        // README "Editing Nudges": "Hidden rows no longer appear in the data visualization"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_answerValueDataCannotBeEdited() {
        // README "Editing Nudges": "While you cannot edit Nudge answer data once entered..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_answerNowRecordsAnswerOutsideOfNotification() {
        // README "Viewing Nudges": "an 'Answer Now' button that you can use if you missed a
        //   Nudge notification, or if you want to add a data point immediately"
        TODO("TDD skeleton")
    }
}
