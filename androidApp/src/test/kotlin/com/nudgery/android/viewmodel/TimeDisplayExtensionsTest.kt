package com.nudgery.android.viewmodel

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeDisplayExtensionsTest {

    private val tz = TimeZone.UTC

    private fun instantAt(date: LocalDate, time: LocalTime) =
        LocalDateTime(date, time).toInstant(tz)

    private val today = LocalDate(2026, 5, 19)
    private val now = instantAt(today, LocalTime(10, 0))

    @Test
    fun TDD_nextFireTime_tomorrow_showsTomorrow() {
        // Times tomorrow (local) should display as "Tomorrow at ..."
        val tomorrow = instantAt(LocalDate(2026, 5, 20), LocalTime(14, 30))
        val result = tomorrow.toLocalDisplayString(tz, now)
        assertTrue("Expected 'Tomorrow' prefix, got: $result", result.startsWith("Tomorrow at"))
    }

    @Test
    fun TDD_nextFireTime_tomorrow_includesTime() {
        val tomorrow = instantAt(LocalDate(2026, 5, 20), LocalTime(14, 30))
        val result = tomorrow.toLocalDisplayString(tz, now)
        assertEquals("Tomorrow at 2:30 PM", result)
    }

    @Test
    fun TDD_nextFireTime_otherDay_showsFullMonthName() {
        // Month name should be written out in full, not abbreviated
        val nextWeek = instantAt(LocalDate(2026, 5, 26), LocalTime(9, 0))
        val result = nextWeek.toLocalDisplayString(tz, now)
        assertTrue("Expected full month name 'May', got: $result", result.contains("May"))
        assertFalse("Should not use abbreviated month 'Jan', got: $result", result.contains("Jan"))
    }

    @Test
    fun TDD_nextFireTime_otherDay_showsMonthAndDay() {
        val nextWeek = instantAt(LocalDate(2026, 5, 26), LocalTime(9, 0))
        val result = nextWeek.toLocalDisplayString(tz, now)
        assertEquals("May 26 at 9 AM", result)
    }

    @Test
    fun TDD_nextFireTime_longMonthName_writtenOut() {
        // e.g. "September" not "Sep"
        val sept = instantAt(LocalDate(2026, 9, 3), LocalTime(8, 0))
        val result = sept.toLocalDisplayString(tz, now)
        assertEquals("September 3 at 8 AM", result)
    }

    @Test
    fun TDD_nextFireTime_noYear() {
        // Year should not appear in the formatted string
        val nextYear = instantAt(LocalDate(2027, 1, 1), LocalTime(12, 0))
        val result = nextYear.toLocalDisplayString(tz, now)
        assertFalse("Year should not be shown, got: $result", result.contains("2027"))
    }

    @Test
    fun TDD_nextFireTime_noTimezoneNotation() {
        val someDay = instantAt(LocalDate(2026, 6, 15), LocalTime(15, 0))
        val result = someDay.toLocalDisplayString(tz, now)
        assertFalse("No UTC marker 'Z'", result.contains("Z"))
        assertFalse("No offset marker '+'", result.contains("+"))
        assertFalse("No ISO separator 'T'", result.contains("T"))
    }

    @Test
    fun TDD_nextFireTime_today_notLabelledTomorrow() {
        // "Today" is not in scope, but it definitely shouldn't say "Tomorrow"
        val laterToday = instantAt(today, LocalTime(20, 0))
        val result = laterToday.toLocalDisplayString(tz, now)
        assertFalse("Should not say 'Tomorrow' for today, got: $result", result.contains("Tomorrow"))
    }

    @Test
    fun TDD_nextFireTime_dayAfterTomorrow_notLabelledTomorrow() {
        val dayAfterTomorrow = instantAt(LocalDate(2026, 5, 21), LocalTime(10, 0))
        val result = dayAfterTomorrow.toLocalDisplayString(tz, now)
        assertFalse("Only tomorrow should say 'Tomorrow', got: $result", result.contains("Tomorrow"))
        assertEquals("May 21 at 10 AM", result)
    }
}
