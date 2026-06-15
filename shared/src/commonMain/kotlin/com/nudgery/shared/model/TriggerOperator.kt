// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.model

enum class TriggerOperator {
    EQ,
    GT,
    GTE,
    LT,
    LTE,
    CONTAINS,
    /** The follow-up is shown unconditionally, regardless of the main answer. */
    ALWAYS
}
