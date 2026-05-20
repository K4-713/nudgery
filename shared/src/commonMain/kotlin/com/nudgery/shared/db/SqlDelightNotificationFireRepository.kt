package com.nudgery.shared.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.nudgery.shared.model.NotificationFire
import com.nudgery.shared.repository.NotificationFireRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightNotificationFireRepository(private val database: NudgeryDatabase) : NotificationFireRepository {

    override suspend fun insert(fire: NotificationFire) = withContext(Dispatchers.Default) {
        database.notificationFireQueries.insertFire(
            id = fire.id,
            nudgeId = fire.nudgeId,
            firedAt = fire.firedAt.toString()
        )
    }

    override suspend fun getMostRecentByNudgeId(nudgeId: String): NotificationFire? =
        withContext(Dispatchers.Default) {
            database.notificationFireQueries
                .selectMostRecentByNudgeId(nudgeId)
                .executeAsOneOrNull()
                ?.toDomain()
        }

    override fun observeMostRecentByNudgeId(nudgeId: String): Flow<NotificationFire?> =
        database.notificationFireQueries.observeMostRecentByNudgeId(nudgeId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    override fun observeAll(): Flow<List<NotificationFire>> =
        database.notificationFireQueries.observeAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    private fun com.nudgery.shared.db.NotificationFire.toDomain() = NotificationFire(
        id = id,
        nudgeId = nudgeId,
        firedAt = Instant.parse(firedAt)
    )
}
