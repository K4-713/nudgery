// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.repository.NudgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightNudgeRepository(private val database: NudgeryDatabase) : NudgeRepository {

    override fun observeAll(): Flow<List<Nudge>> =
        database.nudgeQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Nudge?> =
        database.nudgeQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    override suspend fun getById(id: String): Nudge? = withContext(Dispatchers.Default) {
        database.nudgeQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insert(nudge: Nudge) = withContext(Dispatchers.Default) {
        database.nudgeQueries.insert(
            id = nudge.id,
            name = nudge.name,
            isEnabled = if (nudge.isEnabled) 1L else 0L,
            createdAt = nudge.createdAt.toString(),
            updatedAt = nudge.updatedAt.toString()
        )
    }

    override suspend fun update(nudge: Nudge) = withContext(Dispatchers.Default) {
        database.nudgeQueries.update(
            name = nudge.name,
            isEnabled = if (nudge.isEnabled) 1L else 0L,
            updatedAt = nudge.updatedAt.toString(),
            id = nudge.id
        )
    }

    override suspend fun setEnabled(nudgeId: String, isEnabled: Boolean) = withContext(Dispatchers.Default) {
        val now = kotlinx.datetime.Clock.System.now().toString()
        database.nudgeQueries.setEnabled(
            isEnabled = if (isEnabled) 1L else 0L,
            updatedAt = now,
            id = nudgeId
        )
    }

    override suspend fun delete(nudgeId: String) = withContext(Dispatchers.Default) {
        database.nudgeQueries.delete(nudgeId)
    }

    override suspend fun reorder(orderedNudgeIds: List<String>) = withContext(Dispatchers.Default) {
        database.nudgeQueries.transaction {
            orderedNudgeIds.forEachIndexed { index, id ->
                database.nudgeQueries.updateSortOrder(sortOrder = index.toLong(), id = id)
            }
        }
    }

    private fun com.nudgery.shared.db.Nudge.toDomain() = Nudge(
        id = id,
        name = name,
        isEnabled = isEnabled != 0L,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
        sortOrder = sortOrder.toInt()
    )
}
