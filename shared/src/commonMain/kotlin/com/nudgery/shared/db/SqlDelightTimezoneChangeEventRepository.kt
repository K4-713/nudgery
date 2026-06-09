// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.db

import com.nudgery.shared.model.TimezoneChangeEvent
import com.nudgery.shared.repository.TimezoneChangeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightTimezoneChangeEventRepository(
    private val database: NudgeryDatabase
) : TimezoneChangeEventRepository {

    override suspend fun insert(event: TimezoneChangeEvent) = withContext(Dispatchers.Default) {
        database.timezoneChangeEventQueries.insert(
            id = event.id,
            changedAt = event.changedAt.toString(),
            fromTimezone = event.fromTimezone,
            toTimezone = event.toTimezone
        )
    }

    override suspend fun getAll(): List<TimezoneChangeEvent> = withContext(Dispatchers.Default) {
        database.timezoneChangeEventQueries.selectAll().executeAsList().map { it.toDomain() }
    }

    private fun com.nudgery.shared.db.TimezoneChangeEvent.toDomain() = TimezoneChangeEvent(
        id = id,
        changedAt = Instant.parse(changedAt),
        fromTimezone = fromTimezone,
        toTimezone = toTimezone
    )
}
