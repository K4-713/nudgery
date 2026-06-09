// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.buildHourlyWindow
import com.nudgery.shared.model.orderedHourlyWindow
import kotlin.test.Test
import kotlin.test.assertEquals

class HourlyWindowTest {

    @Test
    fun TDD_buildHourlyWindow_simpleRange() {
        // README: "from the first time through the last"
        assertEquals(listOf(8, 9, 10, 11), buildHourlyWindow(8, 11))
    }

    @Test
    fun TDD_buildHourlyWindow_singleHourWhenStartEqualsEnd() {
        assertEquals(listOf(9), buildHourlyWindow(9, 9))
    }

    @Test
    fun TDD_buildHourlyWindow_wrapsPastMidnight() {
        // README: "The window may wrap past midnight (for example, 8:00 PM to 2:00 AM)"
        assertEquals(listOf(20, 21, 22, 23, 0, 1, 2), buildHourlyWindow(20, 2))
    }

    @Test
    fun TDD_buildHourlyWindow_fullDay() {
        assertEquals((0..23).toList(), buildHourlyWindow(0, 23))
    }

    @Test
    fun TDD_orderedHourlyWindow_recoversWrapOrderFromUnorderedSet() {
        // The stored set is unordered; fire order must be recovered with the wrap intact.
        assertEquals(
            listOf(20, 21, 22, 23, 0, 1, 2),
            orderedHourlyWindow(setOf(2, 22, 0, 20, 1, 23, 21))
        )
    }

    @Test
    fun TDD_orderedHourlyWindow_nonWrappingRangeStartsAtLowestHour() {
        assertEquals(listOf(8, 9, 10, 11), orderedHourlyWindow(setOf(11, 8, 10, 9)))
    }

    @Test
    fun TDD_orderedHourlyWindow_fullDayStartsAtMidnightWithNoWrap() {
        // A full 24-hour window must stay monotonic (start 00:00) so no hour is pushed to the
        // next calendar day.
        assertEquals((0..23).toList(), orderedHourlyWindow((0..23).toSet()))
    }

    @Test
    fun TDD_orderedHourlyWindow_emptySet() {
        assertEquals(emptyList(), orderedHourlyWindow(emptySet()))
    }
}
