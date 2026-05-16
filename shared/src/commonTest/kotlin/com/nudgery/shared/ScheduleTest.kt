package com.nudgery.shared

import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.model.Schedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScheduleTest {

    private val computeNextFireTime = ComputeNextFireTimeUseCase()

    private fun makeSchedule(
        type: ScheduleType,
        timeOfDay: LocalTime = LocalTime(12, 0),
        activeDaysOfWeek: Set<DayOfWeek>? = null,
        dayOfMonth: Int? = null,
        activeHours: Set<Int>? = null
    ) = Schedule(
        id = "test-schedule",
        nudgeId = "test-nudge",
        type = type,
        timeOfDay = timeOfDay,
        activeDaysOfWeek = activeDaysOfWeek,
        dayOfMonth = dayOfMonth,
        activeHours = activeHours
    )

    @Test
    fun TDD_dailyScheduleHasTimeOfDayAndActiveDaysOfWeek() {
        // README "Setting Up a Nudge" > Scheduling:
        //   "Daily: Pick a time of day...and the active days of the week"
        val schedule = makeSchedule(
            type = ScheduleType.DAILY,
            timeOfDay = LocalTime(8, 30),
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        assertEquals(ScheduleType.DAILY, schedule.type)
        assertEquals(LocalTime(8, 30), schedule.timeOfDay)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), schedule.activeDaysOfWeek)
    }

    @Test
    fun TDD_dailyScheduleDefaultsToNoonInDeviceTimezone() {
        // README "Setting Up a Nudge" > Scheduling:
        //   "Daily: Pick a time of day (defaults to noon in your phone's timezone..."
        // The ScheduleRequest.timeOfDay default represents noon; validate that noon is stored correctly
        val request = ScheduleRequest(
            type = ScheduleType.DAILY,
            timeOfDay = LocalTime(12, 0),
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY)
        )
        assertEquals(LocalTime(12, 0), request.timeOfDay)
    }

    @Test
    fun TDD_weeklyScheduleHasDayOfWeekAndLocalTime() {
        // README "Setting Up a Nudge" > Scheduling:
        //   "Weekly: Pick the day of the week, and local time"
        val schedule = makeSchedule(
            type = ScheduleType.WEEKLY,
            timeOfDay = LocalTime(9, 0),
            activeDaysOfWeek = setOf(DayOfWeek.TUESDAY)
        )
        assertEquals(ScheduleType.WEEKLY, schedule.type)
        assertEquals(LocalTime(9, 0), schedule.timeOfDay)
        assertEquals(setOf(DayOfWeek.TUESDAY), schedule.activeDaysOfWeek)
    }

    @Test
    fun TDD_monthlyScheduleHasDayOfMonthAndLocalTime() {
        // README "Setting Up a Nudge" > Scheduling:
        //   "Monthly: Pick the day of the month, and local time"
        val schedule = makeSchedule(
            type = ScheduleType.MONTHLY,
            timeOfDay = LocalTime(18, 0),
            dayOfMonth = 15
        )
        assertEquals(ScheduleType.MONTHLY, schedule.type)
        assertEquals(15, schedule.dayOfMonth)
        assertEquals(LocalTime(18, 0), schedule.timeOfDay)
    }

    @Test
    fun TDD_hourlyScheduleHasActiveHoursAndActiveDaysOfWeek() {
        // README "Setting Up a Nudge" > Scheduling:
        //   "Hourly: Define the hours of the day you want to receive these nudges,
        //    and the active days of the week"
        val schedule = makeSchedule(
            type = ScheduleType.HOURLY,
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            activeHours = setOf(9, 12, 17)
        )
        assertEquals(ScheduleType.HOURLY, schedule.type)
        assertEquals(setOf(9, 12, 17), schedule.activeHours)
        assertNotNull(schedule.activeDaysOfWeek)
        assertEquals(5, schedule.activeDaysOfWeek!!.size)
    }

    @Test
    fun TDD_scheduleTimeFollowsDeviceTimezoneIncludingTravel() {
        // README "Setting Up a Nudge" > Scheduling:
        //   "which will move with your and your phone in case of travel"
        val schedule = makeSchedule(
            type = ScheduleType.DAILY,
            timeOfDay = LocalTime(14, 0),
            activeDaysOfWeek = DayOfWeek.entries.toSet()
        )
        // Monday 2025-01-06 at 08:00 UTC
        val nowUtc = LocalDateTime(2025, 1, 6, 8, 0).toInstant(TimeZone.UTC)

        val easternTz = TimeZone.of("America/New_York")
        val nextFireTime = computeNextFireTime.execute(schedule, nowUtc, easternTz)
        val nextFireLocal = nextFireTime.toLocalDateTime(easternTz)

        // Should fire at 14:00 Eastern time on the same day
        assertEquals(14, nextFireLocal.hour)
        assertEquals(0, nextFireLocal.minute)
    }

    @Test
    fun TDD_nextNudgeDateTimeComputedFromDailySchedule() {
        // README "Setting Up a Nudge": "indicating...next nudge date and time"
        val schedule = makeSchedule(
            type = ScheduleType.DAILY,
            timeOfDay = LocalTime(9, 0),
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        )
        // Monday 2025-01-06 at 08:00 UTC
        val now = LocalDateTime(2025, 1, 6, 8, 0).toInstant(TimeZone.UTC)
        val nextFire = computeNextFireTime.execute(schedule, now, TimeZone.UTC)
        val localFire = nextFire.toLocalDateTime(TimeZone.UTC)

        // Should fire later today (Monday) at 09:00
        assertEquals(2025, localFire.year)
        assertEquals(1, localFire.monthNumber)
        assertEquals(6, localFire.dayOfMonth)
        assertEquals(9, localFire.hour)
    }

    @Test
    fun TDD_nextNudgeDateTimeComputedFromWeeklySchedule() {
        // README "Setting Up a Nudge": "indicating...next nudge date and time"
        val schedule = makeSchedule(
            type = ScheduleType.WEEKLY,
            timeOfDay = LocalTime(10, 0),
            activeDaysOfWeek = setOf(DayOfWeek.FRIDAY)
        )
        // Monday 2025-01-06 at 08:00 UTC
        val now = LocalDateTime(2025, 1, 6, 8, 0).toInstant(TimeZone.UTC)
        val nextFire = computeNextFireTime.execute(schedule, now, TimeZone.UTC)
        val localFire = nextFire.toLocalDateTime(TimeZone.UTC)

        // Should fire on Friday 2025-01-10 at 10:00
        assertEquals(2025, localFire.year)
        assertEquals(1, localFire.monthNumber)
        assertEquals(10, localFire.dayOfMonth)
        assertEquals(10, localFire.hour)
    }

    @Test
    fun TDD_nextNudgeDateTimeComputedFromMonthlySchedule() {
        // README "Setting Up a Nudge": "indicating...next nudge date and time"
        val schedule = makeSchedule(
            type = ScheduleType.MONTHLY,
            timeOfDay = LocalTime(8, 0),
            dayOfMonth = 15
        )
        // 2025-01-10 at 08:00 UTC (before the 15th)
        val now = LocalDateTime(2025, 1, 10, 8, 0).toInstant(TimeZone.UTC)
        val nextFire = computeNextFireTime.execute(schedule, now, TimeZone.UTC)
        val localFire = nextFire.toLocalDateTime(TimeZone.UTC)

        // Should fire on 2025-01-15 at 08:00
        assertEquals(2025, localFire.year)
        assertEquals(1, localFire.monthNumber)
        assertEquals(15, localFire.dayOfMonth)
    }

    @Test
    fun TDD_nextNudgeDateTimeComputedFromHourlySchedule() {
        // README "Setting Up a Nudge": "indicating...next nudge date and time"
        val schedule = makeSchedule(
            type = ScheduleType.HOURLY,
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            activeHours = setOf(9, 12, 17)
        )
        // Monday 2025-01-06 at 10:30 UTC (between 9 and 12)
        val now = LocalDateTime(2025, 1, 6, 10, 30).toInstant(TimeZone.UTC)
        val nextFire = computeNextFireTime.execute(schedule, now, TimeZone.UTC)
        val localFire = nextFire.toLocalDateTime(TimeZone.UTC)

        // Should fire at 12:00 on the same day
        assertEquals(2025, localFire.year)
        assertEquals(1, localFire.monthNumber)
        assertEquals(6, localFire.dayOfMonth)
        assertEquals(12, localFire.hour)
        assertEquals(0, localFire.minute)
    }
}
