package com.nudgery.android.ui.nav

const val ARG_NUDGE_ID = "nudgeId"
const val ARG_SCHEDULED_AT = "scheduledAt"

sealed class NudgeryScreen(val route: String) {
    object NudgeList : NudgeryScreen("nudge_list")
    object CreateNudge : NudgeryScreen("create_nudge")
    object EditNudge : NudgeryScreen("edit_nudge/{$ARG_NUDGE_ID}") {
        fun createRoute(nudgeId: String) = "edit_nudge/$nudgeId"
    }
    object NudgeDetail : NudgeryScreen("nudge_detail/{$ARG_NUDGE_ID}") {
        fun createRoute(nudgeId: String) = "nudge_detail/$nudgeId"
    }
    object AnswerForm : NudgeryScreen("answer_form/{$ARG_NUDGE_ID}?$ARG_SCHEDULED_AT={$ARG_SCHEDULED_AT}") {
        fun createRoute(nudgeId: String, scheduledAt: Long? = null): String {
            val base = "answer_form/$nudgeId"
            return if (scheduledAt != null) "$base?$ARG_SCHEDULED_AT=$scheduledAt" else base
        }
    }
    object Settings : NudgeryScreen("settings")
    object About : NudgeryScreen("about")
}
