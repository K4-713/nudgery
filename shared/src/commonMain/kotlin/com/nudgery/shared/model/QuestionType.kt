package com.nudgery.shared.model

enum class QuestionType {
    YES_NO,
    SCALE,
    NUMBER,
    OPTION_SINGLE,
    OPTION_MULTI,
    TEXT;

    val isOptionType: Boolean get() = this == OPTION_SINGLE || this == OPTION_MULTI

    /**
     * Whether a main question of this type can have follow-up questions. TEXT cannot: free text has
     * no fixed set of answers to define a follow-up trigger condition on. All other types can.
     */
    val allowsFollowUps: Boolean get() = this != TEXT
}
