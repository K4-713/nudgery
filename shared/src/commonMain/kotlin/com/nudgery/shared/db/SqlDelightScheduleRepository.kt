package com.nudgery.shared.db

import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

class SqlDelightScheduleRepository(private val database: NudgeryDatabase) : ScheduleRepository {

    override suspend fun getByNudgeId(nudgeId: String): Schedule? = withContext(Dispatchers.Default) {
        database.scheduleQueries.selectByNudgeId(nudgeId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insert(schedule: Schedule) = withContext(Dispatchers.Default) {
        database.scheduleQueries.insert(
            id = schedule.id,
            nudgeId = schedule.nudgeId,
            type = schedule.type.name,
            timeOfDay = schedule.timeOfDay.toString(),
            activeDaysOfWeek = schedule.activeDaysOfWeek?.serializeDays(),
            dayOfMonth = schedule.dayOfMonth?.toLong(),
            activeHours = schedule.activeHours?.serializeInts()
        )
    }

    override suspend fun update(schedule: Schedule) = withContext(Dispatchers.Default) {
        database.scheduleQueries.update(
            type = schedule.type.name,
            timeOfDay = schedule.timeOfDay.toString(),
            activeDaysOfWeek = schedule.activeDaysOfWeek?.serializeDays(),
            dayOfMonth = schedule.dayOfMonth?.toLong(),
            activeHours = schedule.activeHours?.serializeInts(),
            nudgeId = schedule.nudgeId
        )
    }

    override suspend fun deleteByNudgeId(nudgeId: String) = withContext(Dispatchers.Default) {
        database.scheduleQueries.deleteByNudgeId(nudgeId)
    }

    private fun com.nudgery.shared.db.Schedule.toDomain() = Schedule(
        id = id,
        nudgeId = nudgeId,
        type = ScheduleType.valueOf(type),
        timeOfDay = LocalTime.parse(timeOfDay),
        activeDaysOfWeek = activeDaysOfWeek?.deserializeDays(),
        dayOfMonth = dayOfMonth?.toInt(),
        activeHours = activeHours?.deserializeInts()
    )

    private fun Set<DayOfWeek>.serializeDays(): String =
        joinToString(",") { it.isoDayNumber.toString() }

    private fun String.deserializeDays(): Set<DayOfWeek> =
        split(",").mapNotNull { it.toIntOrNull() }
            .map { DayOfWeek(it) }
            .toSet()

    private fun Set<Int>.serializeInts(): String = joinToString(",")

    private fun String.deserializeInts(): Set<Int> =
        split(",").mapNotNull { it.toIntOrNull() }.toSet()
}
