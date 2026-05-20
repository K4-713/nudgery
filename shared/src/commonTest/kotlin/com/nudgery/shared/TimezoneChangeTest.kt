package com.nudgery.shared

import com.nudgery.shared.model.NotificationFire
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TimezoneChangeEvent
import com.nudgery.shared.usecase.CatchUpMissedFiresUseCase
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for timezone change handling:
 * 1. TimezoneChangeEvent audit records are written correctly.
 * 2. Traveling east (clock jumps forward): today's nudge is missed in the new timezone
 *    → catch-up triggers a FireNow so the user is still asked.
 * 3. Traveling west (clock jumps back): today's nudge already fired in the old timezone,
 *    will fire again in the new timezone via the normal schedule → catch-up does NOT
 *    fire immediately (it schedules normally, letting the alarm run at the right local time).
 */
class TimezoneChangeTest {

    private lateinit var repos: TestRepositories
    private lateinit var catchUpUseCase: CatchUpMissedFiresUseCase

    // Daily at noon, every day.
    private val dailyNoonSchedule = Schedule(
        id = "sched-1",
        nudgeId = "nudge-1",
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet(),
        dayOfMonth = null,
        activeHours = null
    )

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        catchUpUseCase = CatchUpMissedFiresUseCase(
            repos.notificationFireRepository,
            ComputeNextFireTimeUseCase()
        )
    }

    // -------------------------------------------------------------------------
    // Repository: records are stored and retrieved in reverse-chronological order.
    // -------------------------------------------------------------------------

    @Test
    fun TDD_timezoneChangeEventsAreStoredAndRetrievedInOrder() = runTest {
        val first = TimezoneChangeEvent(
            id = "evt-1",
            changedAt = Instant.parse("2026-05-01T10:00:00Z"),
            fromTimezone = "America/New_York",
            toTimezone = "Europe/London"
        )
        val second = TimezoneChangeEvent(
            id = "evt-2",
            changedAt = Instant.parse("2026-05-15T14:00:00Z"),
            fromTimezone = "Europe/London",
            toTimezone = "Asia/Tokyo"
        )

        repos.timezoneChangeEventRepository.insert(first)
        repos.timezoneChangeEventRepository.insert(second)

        val all = repos.timezoneChangeEventRepository.getAll()
        assertEquals(2, all.size)
        // selectAll orders by changedAt DESC — most recent first.
        assertEquals("evt-2", all[0].id)
        assertEquals("evt-1", all[1].id)
    }

    @Test
    fun TDD_timezoneChangeEventPreservesAllFields() = runTest {
        val event = TimezoneChangeEvent(
            id = "evt-1",
            changedAt = Instant.parse("2026-05-20T09:30:00Z"),
            fromTimezone = "America/Chicago",
            toTimezone = "America/Los_Angeles"
        )
        repos.timezoneChangeEventRepository.insert(event)

        val retrieved = repos.timezoneChangeEventRepository.getAll().first()
        assertEquals(event.id, retrieved.id)
        assertEquals(event.changedAt, retrieved.changedAt)
        assertEquals(event.fromTimezone, retrieved.fromTimezone)
        assertEquals(event.toTimezone, retrieved.toTimezone)
    }

    // -------------------------------------------------------------------------
    // TDD_eastwardTravelCausesFireNowForMissedNudge
    //
    // README "Setting Up a Nudge": nudge fires at the scheduled local time.
    // When the clock jumps forward (eastward travel), today's scheduled time may
    // have already passed in the new timezone even though the notification never
    // fired. Catch-up must detect this and return FireNow.
    //
    // Scenario:
    //   Nudge: daily at noon. Last fired yesterday noon EST (= 17:00 UTC).
    //   User travels EST → CET (+6h). Phone switches timezones.
    //   Boot time equivalent: 14:00 CET (= 13:00 UTC). Today noon CET (= 11:00 UTC)
    //   has already passed, so it was missed.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_eastwardTravelCausesFireNowForMissedNudge() = runTest {
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-18T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // Last fire was yesterday noon EST = 17:00 UTC.
        repos.notificationFireRepository.insert(
            NotificationFire("fire-1", nudge.id, Instant.parse("2026-05-19T17:00:00Z"))
        )

        // After switching to CET (UTC+1), "now" is 14:00 CET = 13:00 UTC.
        // Today noon CET = 11:00 UTC — already in the past.
        val nowAfterTimezoneChange = Instant.parse("2026-05-20T13:00:00Z")
        val cet = TimeZone.of("Europe/Paris") // UTC+1 in May (CEST = UTC+2; use a UTC+1 stand-in)

        // Use UTC+1 explicitly to keep the test timezone-database-independent.
        val utcPlusOne = TimeZone.of("Etc/GMT-1")
        val result = catchUpUseCase.execute(nudge, dailyNoonSchedule, nowAfterTimezoneChange, utcPlusOne)

        assertIs<CatchUpMissedFiresUseCase.Result.FireNow>(result,
            "Eastward timezone change that causes today's scheduled time to be missed " +
                    "must trigger a catch-up notification")
        // The missed fire should be today at noon in UTC+1 = 11:00 UTC.
        assertEquals(
            Instant.parse("2026-05-20T11:00:00Z"),
            result.missedScheduledAt
        )
    }

    // -------------------------------------------------------------------------
    // TDD_westwardTravelDoesNotFireImmediately
    //
    // When the clock jumps backward (westward travel), the nudge may have already
    // fired in the old timezone. The catch-up use case should see that the next
    // scheduled fire (in the new timezone) is still in the future and return
    // ScheduleNext — letting the alarm fire at the correct local time.
    //
    // Scenario:
    //   Nudge: daily at noon. Last fired today at 09:30 UTC (noon CET = UTC+1, roughly).
    //   User travels CET → EST (−6h). Phone switches.
    //   Equivalent "now" from EST perspective: 04:00 EST = 09:00 UTC.
    //   Next noon EST = 17:00 UTC — still in the future → ScheduleNext.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_westwardTravelDoesNotFireImmediately() = runTest {
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-18T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // Nudge fired today at 11:00 UTC (noon in UTC+1).
        repos.notificationFireRepository.insert(
            NotificationFire("fire-1", nudge.id, Instant.parse("2026-05-20T11:00:00Z"))
        )

        // After switching to EST (UTC-5), "now" is 04:30 EST = 09:30 UTC.
        // Next noon EST = 17:00 UTC, which hasn't happened yet.
        val nowAfterTimezoneChange = Instant.parse("2026-05-20T09:30:00Z")
        val utcMinusFive = TimeZone.of("Etc/GMT+5")

        val result = catchUpUseCase.execute(nudge, dailyNoonSchedule, nowAfterTimezoneChange, utcMinusFive)

        assertIs<CatchUpMissedFiresUseCase.Result.ScheduleNext>(result,
            "Westward timezone change must not fire immediately when next local noon " +
                    "is still in the future — the alarm should fire at the scheduled time")
    }

    // -------------------------------------------------------------------------
    // TDD_noEventsStoredWhenNoneInserted
    // -------------------------------------------------------------------------
    @Test
    fun TDD_noEventsStoredWhenNoneInserted() = runTest {
        assertTrue(
            repos.timezoneChangeEventRepository.getAll().isEmpty(),
            "Repository should be empty before any events are inserted"
        )
    }

    private fun makeNudge(createdAt: Instant) = Nudge(
        id = "nudge-1",
        name = "Test Nudge",
        isEnabled = true,
        createdAt = createdAt,
        updatedAt = createdAt
    )
}
