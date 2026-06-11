// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.QuestionValidationProblem
import com.nudgery.shared.usecase.configProblem
import com.nudgery.shared.usecase.triggerProblem
import com.nudgery.shared.usecase.validateNudgeQuestions
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * ENGINEERING_DECISIONS.md ED-26: the use-case-side question validator that backstops options,
 * scale range, and follow-up triggers so a non-form path (notably import) can't store bad data.
 */
class QuestionValidationTest {

    @Test
    fun TDD_optionConfig_rejectsTooFewBlankAndTooMany() {
        val valid = QuestionRequest(text = "Pick", type = QuestionType.OPTION_SINGLE, options = listOf("A", "B"))
        assertNull(valid.configProblem())

        val tooFew = valid.copy(options = listOf("A"))
        assertIs<QuestionValidationProblem.NotEnoughOptions>(tooFew.configProblem())

        val blank = valid.copy(options = listOf("A", "  "))
        assertIs<QuestionValidationProblem.BlankOption>(blank.configProblem())

        val tooMany = valid.copy(options = (1..17).map { "opt$it" })
        assertIs<QuestionValidationProblem.TooManyOptions>(tooMany.configProblem())
    }

    @Test
    fun TDD_scaleConfig_rejectsNonAscendingRange() {
        val scale = QuestionRequest(text = "How tired?", type = QuestionType.SCALE)
        assertNull(scale.configProblem(), "default 0..10 valid")
        assertIs<QuestionValidationProblem.InvalidScaleRange>(scale.copy(scaleMin = 5, scaleMax = 5).configProblem())
        assertIs<QuestionValidationProblem.InvalidScaleRange>(scale.copy(scaleMin = 9, scaleMax = 2).configProblem())
    }

    @Test
    fun TDD_followUpTrigger_dependsOnMainType() {
        val noTrigger = QuestionRequest(text = "Why?", type = QuestionType.TEXT)
        // Yes/No or option main: an answer must be chosen.
        assertIs<QuestionValidationProblem.MissingFollowUpTrigger>(triggerProblem(QuestionType.YES_NO, noTrigger))
        assertNull(triggerProblem(QuestionType.YES_NO, noTrigger.copy(triggerAnswerValue = "YES")))
        // Number/Scale main: operator + numeric value required.
        assertIs<QuestionValidationProblem.MissingFollowUpTrigger>(
            triggerProblem(QuestionType.NUMBER, noTrigger.copy(triggerOperator = TriggerOperator.GTE))
        )
        assertIs<QuestionValidationProblem.MissingFollowUpTrigger>(
            triggerProblem(QuestionType.NUMBER, noTrigger.copy(triggerOperator = TriggerOperator.GTE, triggerAnswerValue = "-"))
        )
        assertNull(
            triggerProblem(QuestionType.NUMBER, noTrigger.copy(triggerOperator = TriggerOperator.GTE, triggerAnswerValue = "7"))
        )
    }

    @Test
    fun TDD_validateNudgeQuestions_findsFirstProblem() {
        val main = QuestionRequest(text = "Mood?", type = QuestionType.YES_NO)
        val goodFollowUp = QuestionRequest(text = "Why?", type = QuestionType.TEXT, triggerAnswerValue = "NO")
        assertNull(validateNudgeQuestions(main, listOf(goodFollowUp)))

        // A follow-up missing its trigger is reported.
        assertIs<QuestionValidationProblem.MissingFollowUpTrigger>(
            validateNudgeQuestions(main, listOf(goodFollowUp.copy(triggerAnswerValue = null)))
        )
        // The main question's own config is checked first.
        val badMain = QuestionRequest(text = "Pick", type = QuestionType.OPTION_SINGLE, options = listOf("only one"))
        assertIs<QuestionValidationProblem.NotEnoughOptions>(validateNudgeQuestions(badMain, emptyList()))
    }
}
