// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.util

import com.nudgery.shared.notification.AlertPresenter

/** Records the nudge ids whose alerts were dismissed, so tests can assert ED-18 behavior. */
class FakeAlertPresenter : AlertPresenter {
    val dismissed = mutableListOf<String>()

    override fun dismissAlert(nudgeId: String) {
        dismissed += nudgeId
    }

    fun reset() {
        dismissed.clear()
    }
}
