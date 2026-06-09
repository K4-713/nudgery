// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
