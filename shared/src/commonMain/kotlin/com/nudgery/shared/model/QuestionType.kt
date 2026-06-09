// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.model

enum class QuestionType {
    YES_NO,
    SCALE,
    NUMBER,
    OPTION_SINGLE,
    OPTION_MULTI,
    TEXT,
    /**
     * Free-form emoji-only answer. Stored, exported, and charted exactly like [TEXT] (ED-1); it
     * differs only in input, which is restricted to emoji via the emoji picker.
     */
    EMOJI;

    val isOptionType: Boolean get() = this == OPTION_SINGLE || this == OPTION_MULTI

    /** Whether this is a free-form (no fixed answer set) text-like type: [TEXT] or [EMOJI]. */
    val isFreeformType: Boolean get() = this == TEXT || this == EMOJI

    /**
     * Whether a main question of this type can have follow-up questions. Free-form types cannot:
     * they have no fixed set of answers to define a follow-up trigger condition on. All others can.
     */
    val allowsFollowUps: Boolean get() = !isFreeformType
}
