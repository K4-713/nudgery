// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.usecase

import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.orderedHourlyWindow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class ComputeNextFireTimeUseCase {

    fun execute(
        schedule: Schedule,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Instant {
        val localNow = now.toLocalDateTime(timeZone)
        return when (schedule.type) {
            ScheduleType.DAILY -> computeNextDaily(schedule, localNow, timeZone)
            ScheduleType.WEEKLY -> computeNextWeekly(schedule, localNow, timeZone)
            ScheduleType.MONTHLY -> computeNextMonthly(schedule, localNow, timeZone)
            ScheduleType.HOURLY -> computeNextHourly(schedule, localNow, timeZone)
        }
    }

    private fun computeNextDaily(
        schedule: Schedule,
        localNow: LocalDateTime,
        timeZone: TimeZone
    ): Instant {
        val activeDays = schedule.activeDaysOfWeek ?: DayOfWeek.entries.toSet()
        var candidate = localNow.date
        repeat(8) {
            if (candidate.dayOfWeek in activeDays) {
                val candidateDateTime = LocalDateTime(candidate, schedule.timeOfDay)
                if (candidateDateTime > localNow) {
                    return candidateDateTime.toInstant(timeZone)
                }
            }
            candidate = candidate.plus(1, DateTimeUnit.DAY)
        }
        error("Could not compute next daily fire time — schedule has no active days")
    }

    private fun computeNextWeekly(
        schedule: Schedule,
        localNow: LocalDateTime,
        timeZone: TimeZone
    ): Instant {
        val activeDays = schedule.activeDaysOfWeek ?: emptySet()
        var candidate = localNow.date
        repeat(14) {
            if (candidate.dayOfWeek in activeDays) {
                val candidateDateTime = LocalDateTime(candidate, schedule.timeOfDay)
                if (candidateDateTime > localNow) {
                    return candidateDateTime.toInstant(timeZone)
                }
            }
            candidate = candidate.plus(1, DateTimeUnit.DAY)
        }
        error("Could not compute next weekly fire time — no active day found within 14 days")
    }

    private fun computeNextMonthly(
        schedule: Schedule,
        localNow: LocalDateTime,
        timeZone: TimeZone
    ): Instant {
        val dayOfMonth = schedule.dayOfMonth ?: 1
        var year = localNow.year
        var month = localNow.monthNumber
        repeat(3) {
            val candidateDate = try {
                LocalDate(year, month, dayOfMonth)
            } catch (_: IllegalArgumentException) {
                null
            }
            if (candidateDate != null) {
                val candidateDateTime = LocalDateTime(candidateDate, schedule.timeOfDay)
                if (candidateDateTime > localNow) {
                    return candidateDateTime.toInstant(timeZone)
                }
            }
            if (month == 12) { year++; month = 1 } else month++
        }
        error("Could not compute next monthly fire time")
    }

    private fun computeNextHourly(
        schedule: Schedule,
        localNow: LocalDateTime,
        timeZone: TimeZone
    ): Instant {
        val activeDays = schedule.activeDaysOfWeek ?: DayOfWeek.entries.toSet()
        val window = orderedHourlyWindow(schedule.activeHours ?: emptySet())
        if (window.isEmpty() || activeDays.isEmpty()) {
            error("Could not compute next hourly fire time — no active hours or days")
        }
        val minute = schedule.timeOfDay.minute

        // Each session is anchored to the day it starts on (an active day). Hours after the window
        // wraps past midnight belong to that same start day's session, so we generate every fire
        // for a start day before moving on. We begin one day in the past so a session started
        // yesterday whose post-midnight tail is still ahead of `now` is not missed.
        var startDay = localNow.date.minus(1, DateTimeUnit.DAY)
        repeat(10) {
            if (startDay.dayOfWeek in activeDays) {
                var previousHour = -1
                var dayOffset = 0
                for (hour in window) {
                    // A drop in the hour value means the window has wrapped into the next calendar day.
                    if (previousHour >= 0 && hour <= previousHour) dayOffset++
                    previousHour = hour
                    val fireDate = startDay.plus(dayOffset, DateTimeUnit.DAY)
                    val candidateDateTime = LocalDateTime(fireDate, LocalTime(hour, minute))
                    if (candidateDateTime > localNow) {
                        return candidateDateTime.toInstant(timeZone)
                    }
                }
            }
            startDay = startDay.plus(1, DateTimeUnit.DAY)
        }
        error("Could not compute next hourly fire time — no active day found within range")
    }
}
