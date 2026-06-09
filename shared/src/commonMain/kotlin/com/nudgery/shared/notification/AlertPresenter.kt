// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.notification

/**
 * Controls a nudge's posted "alert" notification from shared code without depending on any platform
 * notification API. Today it only needs to *dismiss* an alert — recording an answer clears that
 * nudge's outstanding alert (ED-18) so a lingering notification can't prompt a duplicate answer.
 * Posting still lives in the platform notification worker.
 */
interface AlertPresenter {
    /** Dismisses the outstanding alert for [nudgeId], if one is showing; a no-op when none is. */
    fun dismissAlert(nudgeId: String)
}
