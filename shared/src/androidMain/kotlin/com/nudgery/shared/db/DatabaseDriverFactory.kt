// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.nudgery.shared.db.NudgeryDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = NudgeryDatabase.Schema,
            context = context,
            name = "nudgery.db",
            callback = object : AndroidSqliteDriver.Callback(NudgeryDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            }
        )
}
