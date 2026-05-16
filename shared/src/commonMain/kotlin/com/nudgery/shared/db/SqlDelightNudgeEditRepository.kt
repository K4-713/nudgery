package com.nudgery.shared.db

import com.nudgery.shared.model.NudgeEdit
import com.nudgery.shared.repository.NudgeEditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightNudgeEditRepository(private val database: NudgeryDatabase) : NudgeEditRepository {

    override suspend fun getByNudgeId(nudgeId: String): List<NudgeEdit> = withContext(Dispatchers.Default) {
        database.nudgeEditQueries.selectByNudgeId(nudgeId).executeAsList().map { it.toDomain() }
    }

    override suspend fun insert(edit: NudgeEdit) = withContext(Dispatchers.Default) {
        database.nudgeEditQueries.insert(
            id = edit.id,
            nudgeId = edit.nudgeId,
            editedAt = edit.editedAt.toString(),
            fieldChanged = edit.fieldChanged,
            previousValue = edit.previousValue
        )
    }

    private fun com.nudgery.shared.db.NudgeEdit.toDomain() = NudgeEdit(
        id = id,
        nudgeId = nudgeId,
        editedAt = Instant.parse(editedAt),
        fieldChanged = fieldChanged,
        previousValue = previousValue
    )
}
