package com.nudgery.shared

import kotlin.test.Test

class NudgeEditTest {

    @Test
    fun TDD_nudgeBaseQuestionTypeCannotBeChanged() {
        // README "Editing Nudges": "Nudge configuration [can] be edited, except for the
        //   base type of the main question"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_nudgeScheduleCanBeEdited() {
        // README "Editing Nudges": "Nudge configuration [can] be edited..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_editingQuestionTextOffersChoiceBetweenSplitAndInPlace() {
        // README "Editing Nudges": "If you edit the main question text or selectable option text,
        //   you will be asked if you would prefer to split the Nudge instead of editing in place"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_editingOptionTextOffersChoiceBetweenSplitAndInPlace() {
        // README "Editing Nudges": "If you edit...selectable option text, you will be asked if
        //   you would prefer to split the Nudge instead of editing in place"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_splitEditDisablesOldNudge() {
        // README "Editing Nudges": "The old version of the Nudge and all its related data will
        //   be preserved, and the old Nudge will be disabled"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_splitEditPreservesOldNudgeData() {
        // README "Editing Nudges": "The old version of the Nudge and all its related data will
        //   be preserved..."
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_splitEditCreatesNewEnabledNudgeWithEdits() {
        // README "Editing Nudges": "The edits will essentially be a new Nudge, which will be
        //   enabled going forward"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_inPlaceEditSavesNudgeEditAuditRecord() {
        // README "Editing Nudges": "a note recording the date/time and contents of the edit
        //   will be saved with the Nudge"
        // ARCHITECTURE.md NudgeEdit: editedAt, fieldChanged, previousValue
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_inPlaceEditAuditRecordContainsPreviousValue() {
        // ARCHITECTURE.md NudgeEdit.previousValue: "Value before the edit"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_inPlaceEditAuditRecordContainsFieldChanged() {
        // ARCHITECTURE.md NudgeEdit.fieldChanged: "Which field was changed"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_inPlaceEditRetainsOldAnswerDataWithNewQuestionText() {
        // README "Editing Nudges": "The old data will be kept with the new question text..."
        TODO("TDD skeleton")
    }
}
