// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [fullScreenChartTitle]. DESIGN.md "Chart" / detail screen: a full-screen chart is titled
 * by the question being charted, not the chart-style name (the style is chosen separately in the
 * chart-type picker). The chart-style label is only a fallback for a question with no text.
 */
class FullScreenChartTitleTest {

    @Test
    fun TDD_fullScreenTitleIsTheQuestionTextNotTheChartStyle() {
        // The title is the question text even though a chart style label ("Heat Map") is available.
        val title = fullScreenChartTitle(questionText = "Did you have a headache today?", chartStyleLabel = "Heat Map")
        assertEquals("Did you have a headache today?", title)
    }

    @Test
    fun TDD_fullScreenTitleFallsBackToChartStyleWhenQuestionHasNoText() {
        // An emoji-only / empty question has no text, so the chart-style label stands in (never blank).
        assertEquals("Packed Bubble", fullScreenChartTitle(questionText = null, chartStyleLabel = "Packed Bubble"))
        assertEquals("Packed Bubble", fullScreenChartTitle(questionText = "", chartStyleLabel = "Packed Bubble"))
        assertEquals("Packed Bubble", fullScreenChartTitle(questionText = "   ", chartStyleLabel = "Packed Bubble"))
    }
}
