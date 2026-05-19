package com.nudgery.android.ui.nav

const val ARG_NUDGE_ID = "nudgeId"

sealed class NudgeryScreen(val route: String) {
    object NudgeList : NudgeryScreen("nudge_list")
    object CreateNudge : NudgeryScreen("create_nudge")
    object EditNudge : NudgeryScreen("edit_nudge/{$ARG_NUDGE_ID}") {
        fun createRoute(nudgeId: String) = "edit_nudge/$nudgeId"
    }
    object NudgeDetail : NudgeryScreen("nudge_detail/{$ARG_NUDGE_ID}") {
        fun createRoute(nudgeId: String) = "nudge_detail/$nudgeId"
    }
    object AnswerForm : NudgeryScreen("answer_form/{$ARG_NUDGE_ID}") {
        fun createRoute(nudgeId: String) = "answer_form/$nudgeId"
    }
    object Settings : NudgeryScreen("settings")
    object About : NudgeryScreen("about")
}
