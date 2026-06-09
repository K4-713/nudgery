// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.util

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
