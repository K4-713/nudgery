package com.nudgery.shared

import kotlin.test.Test

class NotificationSchedulerTest {

    @Test
    fun TDD_enabledNudgeSchedulesNotificationOnSave() {
        // README "Setting Up a Nudge": "Enabled Nudges will send you notifications when it's
        //   time to answer your questions"
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_disabledNudgeDoesNotScheduleNotifications() {
        // README "Setting Up a Nudge": "Enabled Nudges will send you notifications..."
        //   — the inverse: a disabled nudge must not fire
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_disablingANudgeCancelsItsScheduledNotifications() {
        // README "Viewing Nudges": "whether or not it is enabled"
        //   — toggling enabled off must cancel any pending notifications
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_enablingANudgeReschedulesItsNotifications() {
        // README "Viewing Nudges": "Enabled Nudges will send you notifications..."
        //   — toggling enabled on must re-register the notification schedule
        TODO("TDD skeleton")
    }

    @Test
    fun TDD_editingScheduleReschedulesNotifications() {
        // README "Editing Nudges": "Nudge configuration [can] be edited" — changing the
        //   schedule must update the pending notification trigger
        TODO("TDD skeleton")
    }
}
