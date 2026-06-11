// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the FAB-promotion rule for the main list's empty state.
 * DESIGN.md → Empty States → Main List: the corner "＋" FAB appears only once at least one nudge
 * exists; while the list is empty (or still loading), it is suppressed so the centered empty-state
 * call-to-action is the single way to create the first nudge.
 */
class EmptyNudgeListTest {

    @Test
    fun TDD_cornerFab_hidden_whileStillLoading() {
        // null = not yet loaded; nothing should flash over the (eventual) empty state.
        assertFalse(showsCornerCreateFab(null))
    }

    @Test
    fun TDD_cornerFab_hidden_whenListEmpty() {
        // Empty list = the welcoming empty state owns the only create affordance.
        assertFalse(showsCornerCreateFab(emptyList<Any>()))
    }

    @Test
    fun TDD_cornerFab_shown_whenNudgesExist() {
        // Once a nudge exists, the FAB returns to its normal corner position.
        assertTrue(showsCornerCreateFab(listOf("a-nudge")))
    }
}
