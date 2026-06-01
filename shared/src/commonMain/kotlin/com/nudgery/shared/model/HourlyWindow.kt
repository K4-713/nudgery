package com.nudgery.shared.model

/**
 * Helpers for the HOURLY schedule's "first nudge / last nudge" window.
 *
 * An hourly window is a contiguous run of hours starting at the first nudge's hour and ending at
 * the last nudge's hour, stepping one hour at a time. The window may wrap past midnight (e.g. a
 * start of 20:00 and end of 02:00 covers 20, 21, 22, 23, 0, 1, 2). All nudges in the window fire
 * at the same minute (the minute of the first nudge time, stored on `Schedule.timeOfDay`).
 *
 * The set of hours is persisted unordered in `Schedule.activeHours`; [orderedHourlyWindow] recovers
 * the fire order from it.
 */

private const val HOURS_PER_DAY = 24

/**
 * Builds the ordered list of hours for a window running from [startHour] through [endHour]
 * (both 0–23, inclusive), wrapping past midnight when [endHour] is earlier in the day than
 * [startHour]. When [startHour] == [endHour] the window is a single hour.
 */
fun buildHourlyWindow(startHour: Int, endHour: Int): List<Int> {
    val hours = mutableListOf<Int>()
    var hour = startHour
    while (true) {
        hours.add(hour)
        if (hour == endHour) break
        hour = (hour + 1) % HOURS_PER_DAY
    }
    return hours
}

/**
 * Recovers the ordered hours of a window from a stored, unordered [activeHours] set. The window's
 * start is the hour immediately following the largest cyclic gap between selected hours (i.e. the
 * long "off" stretch), so a wrapped window is reconstructed in fire order. Returns an empty list
 * for an empty set.
 *
 * Legacy schedules whose hours are not contiguous are still ordered deterministically from the
 * same start point, preserving same-day firing for each selected hour.
 */
fun orderedHourlyWindow(activeHours: Set<Int>): List<Int> {
    if (activeHours.isEmpty()) return emptyList()
    val sorted = activeHours.sorted()
    if (sorted.size == 1) return sorted

    var startIndex = 0
    var largestGap = Int.MIN_VALUE
    for (i in sorted.indices) {
        val current = sorted[i]
        val next = sorted[(i + 1) % sorted.size]
        val gap = (next - current + HOURS_PER_DAY) % HOURS_PER_DAY
        // `>=` so that, when gaps tie (e.g. a full 24-hour window where every gap is 1), the wrap
        // gap after the last sorted hour wins and the window starts at the lowest hour (00:00),
        // keeping it monotonic with no spurious midnight wrap.
        if (gap >= largestGap) {
            largestGap = gap
            startIndex = (i + 1) % sorted.size
        }
    }
    return List(sorted.size) { offset -> sorted[(startIndex + offset) % sorted.size] }
}
