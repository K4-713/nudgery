package com.nudgery.shared.model

enum class QuestionType {
    YES_NO,
    NUMBER,
    OPTION_SINGLE,
    OPTION_MULTI,
    TEXT;

    val isOptionType: Boolean get() = this == OPTION_SINGLE || this == OPTION_MULTI
    val isValidForMainQuestion: Boolean get() = this != TEXT
}
