// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nudgery.shared.db.NudgeryDatabase
import com.nudgery.shared.db.SqlDelightNudgeRepository
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.usecase.ReorderNudgesUseCase
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NudgeReorderTest {

    private lateinit var repos: TestRepositories
    private lateinit var nudges: NudgeRepository
    private lateinit var reorder: ReorderNudgesUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        nudges = repos.nudgeRepository
        reorder = ReorderNudgesUseCase(nudges)
    }

    private fun nudge(id: String, createdAt: String) = Nudge(
        id = id,
        name = id,
        isEnabled = true,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt)
    )

    private suspend fun seedThree() {
        nudges.insert(nudge("a", "2026-01-01T00:00:00Z"))
        nudges.insert(nudge("b", "2026-01-02T00:00:00Z"))
        nudges.insert(nudge("c", "2026-01-03T00:00:00Z"))
    }

    @Test
    fun TDD_newNudgesAppendInCreationOrder() = runTest {
        // ENGINEERING_DECISIONS.md ED-19: inserts append (sortOrder = MAX+1), so the list stays in
        // creation order until the user reorders, and existing nudges are never reshuffled.
        seedThree()

        val ordered = nudges.observeAll().first()
        assertEquals(listOf("a", "b", "c"), ordered.map { it.id })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
    }

    @Test
    fun TDD_reorderPersistsNewOrder() = runTest {
        // ENGINEERING_DECISIONS.md ED-19: reordering rewrites positions to a dense 0..n sequence in
        // the new order, and that order is what the list query returns afterward.
        seedThree()

        reorder.execute(listOf("c", "a", "b"))

        val ordered = nudges.observeAll().first()
        assertEquals(listOf("c", "a", "b"), ordered.map { it.id })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
    }

    @Test
    fun TDD_migration3BackfillsSortOrderByCreatedAt() {
        // ENGINEERING_DECISIONS.md ED-19: migration 3 adds sortOrder and backfills it by createdAt
        // order, so an upgrading user's existing list keeps the order they already saw.
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // The pre-sortOrder (v3) Nudge table, seeded out of creation order to prove the backfill.
        driver.execute(
            null,
            """
            CREATE TABLE Nudge (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            );
            """.trimIndent(),
            0
        )
        fun seed(id: String, createdAt: String) = driver.execute(
            null,
            "INSERT INTO Nudge (id, name, isEnabled, createdAt, updatedAt) " +
                "VALUES ('$id', '$id', 1, '$createdAt', '$createdAt');",
            0
        )
        seed("b", "2026-01-02T00:00:00Z")
        seed("a", "2026-01-01T00:00:00Z")
        seed("c", "2026-01-03T00:00:00Z")

        NudgeryDatabase.Schema.migrate(driver, 3L, 4L)

        val database = NudgeryDatabase(driver)
        val rows = database.nudgeQueries.selectAll().executeAsList()
        assertEquals(listOf("a", "b", "c"), rows.map { it.id })
        assertEquals(listOf(0L, 1L, 2L), rows.map { it.sortOrder })
    }
}
