package com.nudgery.android.viewmodel

import com.nudgery.shared.model.ScheduleType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleFormStateTest {

    private val noonDaily = ScheduleFormState(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet()
    )

    // Day abbreviation tests

    @Test
    fun TDD_dayAbbreviations_monday() {
        // docs: day abbreviations should be M, Tu, W, Th, F, Sa, Su
        assertEquals("M", DayOfWeek.MONDAY.toAbbreviation())
    }

    @Test
    fun TDD_dayAbbreviations_tuesday() {
        assertEquals("Tu", DayOfWeek.TUESDAY.toAbbreviation())
    }

    @Test
    fun TDD_dayAbbreviations_wednesday() {
        assertEquals("W", DayOfWeek.WEDNESDAY.toAbbreviation())
    }

    @Test
    fun TDD_dayAbbreviations_thursday() {
        assertEquals("Th", DayOfWeek.THURSDAY.toAbbreviation())
    }

    @Test
    fun TDD_dayAbbreviations_friday() {
        assertEquals("F", DayOfWeek.FRIDAY.toAbbreviation())
    }

    @Test
    fun TDD_dayAbbreviations_saturday() {
        assertEquals("Sa", DayOfWeek.SATURDAY.toAbbreviation())
    }

    @Test
    fun TDD_dayAbbreviations_sunday() {
        assertEquals("Su", DayOfWeek.SUNDAY.toAbbreviation())
    }

    // Schedule description — day grouping tests

    @Test
    fun TDD_scheduleDescription_allDays_showsEveryDay() {
        // docs: if every day, say "Every Day"
        val description = noonDaily.copy(activeDaysOfWeek = DayOfWeek.entries.toSet()).toDescription()
        assert("Every Day" in description) { "Expected 'Every Day' in: $description" }
    }

    @Test
    fun TDD_scheduleDescription_weekdays_showsWeekdays() {
        // docs: Mon-Fri should show "Weekdays"
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        val description = noonDaily.copy(activeDaysOfWeek = weekdays).toDescription()
        assert("Weekdays" in description) { "Expected 'Weekdays' in: $description" }
    }

    @Test
    fun TDD_scheduleDescription_weekends_showsWeekends() {
        // docs: Sat-Sun should show "Weekends"
        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val description = noonDaily.copy(activeDaysOfWeek = weekends).toDescription()
        assert("Weekends" in description) { "Expected 'Weekends' in: $description" }
    }

    @Test
    fun TDD_scheduleDescription_customDays_usesShortAbbreviations() {
        // docs: custom day sets use M, Tu, W, Th, F, Sa, Su abbreviations
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val description = noonDaily.copy(activeDaysOfWeek = days).toDescription()
        assert("M, W, F" in description) { "Expected 'M, W, F' in: $description" }
    }

    @Test
    fun TDD_scheduleDescription_customDays_noThreeLetterAbbreviations() {
        // docs: no longer using Mon/Tue/Wed style abbreviations
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        val description = noonDaily.copy(activeDaysOfWeek = days).toDescription()
        assert("Tue" !in description) { "Found old abbreviation 'Tue' in: $description" }
        assert("Thu" !in description) { "Found old abbreviation 'Thu' in: $description" }
        assert("Sat" !in description) { "Found old abbreviation 'Sat' in: $description" }
        assert("Tu, Th, Sa" in description) { "Expected 'Tu, Th, Sa' in: $description" }
    }

    @Test
    fun TDD_scheduleDescription_allDays_notAbbreviated() {
        // docs: "Every Day" instead of listing all 7 abbreviations
        val description = noonDaily.toDescription()
        assert("M, Tu, W, Th, F, Sa, Su" !in description) { "Should use 'Every Day', not list all days: $description" }
    }
}
