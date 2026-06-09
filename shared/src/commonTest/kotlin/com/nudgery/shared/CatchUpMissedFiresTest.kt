// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.NotificationFire
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
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
import kotlin.test.assertNotEquals

/**
 * Tests for catch-up behavior when the phone is powered off long enough to miss one or more
 * scheduled notification fires. Covers README "Setting Up a Nudge" / notification delivery
 * guarantees and the missed-nudge indicator contract.
 */
class CatchUpMissedFiresTest {

    private lateinit var repos: TestRepositories
    private lateinit var useCase: CatchUpMissedFiresUseCase

    // Fixed reference: "Wednesday 2026-05-20 14:00 UTC" as the moment the phone boots.
    // All test scenarios are expressed relative to this point.
    private val bootTime = Instant.parse("2026-05-20T14:00:00Z")
    private val tz = TimeZone.UTC

    // Daily schedule that fires every day at noon UTC.
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
        useCase = CatchUpMissedFiresUseCase(repos.notificationFireRepository, ComputeNextFireTimeUseCase())
    }

    // -------------------------------------------------------------------------
    // TDD_noMissedFireWhenCreatedAfterMostRecentScheduledTime
    //
    // README "Setting Up a Nudge": notifications fire at the scheduled time.
    // A brand-new nudge whose first scheduled fire is in the future must not
    // trigger a catch-up notification.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_noMissedFireWhenCreatedAfterMostRecentScheduledTime() = runTest {
        // Nudge created at 13:00 — today's noon has already passed, next fire is tomorrow noon.
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-20T13:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)

        val result = useCase.execute(nudge, dailyNoonSchedule, bootTime, tz)

        assertIs<CatchUpMissedFiresUseCase.Result.ScheduleNext>(result,
            "No catch-up should occur when the first fire time is still in the future")
    }

    // -------------------------------------------------------------------------
    // TDD_singleMissedFireReturnsFireNow
    //
    // Phone was off since the last successful fire (yesterday noon). Today's noon
    // fire was missed. Expect FireNow with today's noon.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_singleMissedFireReturnsFireNow() = runTest {
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-18T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // Last recorded fire was yesterday noon — phone went off after that.
        repos.notificationFireRepository.insert(
            NotificationFire("fire-1", nudge.id, Instant.parse("2026-05-19T12:00:00Z"))
        )

        val result = useCase.execute(nudge, dailyNoonSchedule, bootTime, tz)

        assertIs<CatchUpMissedFiresUseCase.Result.FireNow>(result,
            "Should fire now for the missed noon notification")
        assertEquals(
            Instant.parse("2026-05-20T12:00:00Z"),
            result.missedScheduledAt,
            "FireNow should carry today's noon as the missed scheduled time"
        )
    }

    // -------------------------------------------------------------------------
    // TDD_mostRecentMissedFireSelectedWhenMultipleMissed
    //
    // Phone was off for two days. Three noon fires were missed (Saturday, Sunday,
    // Monday in this scenario). Only the most recent missed fire should be returned.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_mostRecentMissedFireSelectedWhenMultipleMissed() = runTest {
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-15T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // Last recorded fire was three days ago noon.
        repos.notificationFireRepository.insert(
            NotificationFire("fire-1", nudge.id, Instant.parse("2026-05-17T12:00:00Z"))
        )

        val result = useCase.execute(nudge, dailyNoonSchedule, bootTime, tz)

        assertIs<CatchUpMissedFiresUseCase.Result.FireNow>(result,
            "Multiple missed fires should still return FireNow")
        assertEquals(
            Instant.parse("2026-05-20T12:00:00Z"),
            result.missedScheduledAt,
            "Only the most recent missed fire time should be selected"
        )
        assertNotEquals(
            Instant.parse("2026-05-18T12:00:00Z"),
            result.missedScheduledAt,
            "An older missed fire time must not be returned"
        )
    }

    // -------------------------------------------------------------------------
    // TDD_noMissedFireWhenLastFireWasToday
    //
    // Phone fired the notification successfully this morning, then the user
    // turned it off and back on. No catch-up should occur.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_noMissedFireWhenLastFireWasToday() = runTest {
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-18T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // Last fire was today at noon — still within the current period.
        repos.notificationFireRepository.insert(
            NotificationFire("fire-1", nudge.id, Instant.parse("2026-05-20T12:00:00Z"))
        )

        val result = useCase.execute(nudge, dailyNoonSchedule, bootTime, tz)

        assertIs<CatchUpMissedFiresUseCase.Result.ScheduleNext>(result,
            "No catch-up when the most recent fire already happened today")
    }

    // -------------------------------------------------------------------------
    // TDD_usesCreatedAtAsReferenceWhenNoFiresExist
    //
    // A nudge that has never fired (e.g., created yesterday, phone died before
    // the first notification) should still catch up.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_usesCreatedAtAsReferenceWhenNoFiresExist() = runTest {
        // Nudge created yesterday at 8 AM — it should have fired at yesterday noon.
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-19T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // No NotificationFire records at all.

        val result = useCase.execute(nudge, dailyNoonSchedule, bootTime, tz)

        assertIs<CatchUpMissedFiresUseCase.Result.FireNow>(result,
            "createdAt should be used as reference when no fires have been recorded")
        // The most recent past fire relative to bootTime (14:00) is today noon.
        assertEquals(
            Instant.parse("2026-05-20T12:00:00Z"),
            result.missedScheduledAt
        )
    }

    // -------------------------------------------------------------------------
    // TDD_olderMissedFiresAreAbandoned
    //
    // Only ONE catch-up notification should be issued — the older fires must
    // not be delivered. Verified indirectly: FireNow carries only one time.
    // -------------------------------------------------------------------------
    @Test
    fun TDD_olderMissedFiresAreAbandoned() = runTest {
        val nudge = makeNudge(createdAt = Instant.parse("2026-05-10T08:00:00Z"))
        repos.nudgeRepository.insert(nudge)
        repos.scheduleRepository.insert(dailyNoonSchedule)
        // Last fire was a week ago — 7 noon fires were missed.
        repos.notificationFireRepository.insert(
            NotificationFire("fire-1", nudge.id, Instant.parse("2026-05-13T12:00:00Z"))
        )

        val result = useCase.execute(nudge, dailyNoonSchedule, bootTime, tz)

        assertIs<CatchUpMissedFiresUseCase.Result.FireNow>(result)
        // Only the most recent missed fire (today noon) is returned, not any older one.
        assertEquals(
            Instant.parse("2026-05-20T12:00:00Z"),
            result.missedScheduledAt,
            "Catch-up must deliver only the most recent missed fire, abandoning all older ones"
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
