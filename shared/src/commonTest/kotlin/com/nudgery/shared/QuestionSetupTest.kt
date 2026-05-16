package com.nudgery.shared

import kotlin.test.Test

class QuestionSetupTest {

    @Test
    fun TDD_mainQuestionCannotBeTextType() {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types,
        //   plus a freeform Text type" — TEXT is only valid as a follow-up
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpQuestionCanBeYesNo() {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpQuestionCanBeNumber() {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpQuestionCanBeOptionSingle() {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpQuestionCanBeOptionMulti() {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpQuestionCanBeText() {
        // README "Setting Up a Nudge": "...plus a freeform Text type"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpTriggeredByExactAnswerValue() {
        // README "Setting Up a Nudge": "you will be able to add follow-up questions for specific answers"
        // README "What Can You Use": "if your boss scored 7 or greater on being annoying that day,
        //   it could then ask you for some brief notes"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpTriggeredByAnswerRange() {
        // README "Setting Up a Nudge": "Follow-up questions for specific answers or ranges of answers"
        // ARCHITECTURE.md Question.triggerOperator: "EQ, GTE, LTE, etc. Allows range-based follow-up triggers"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_mainQuestionHasOrderIndexZero() {
        // ARCHITECTURE.md Question.orderIndex: "0 = main question"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_followUpQuestionsHaveOrderIndexGreaterThanZero() {
        // ARCHITECTURE.md Question.orderIndex: "subsequent questions are follow-ups"
        TODO("TDD skeleton")
    }
}
