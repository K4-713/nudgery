package com.nudgery.shared.util

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
