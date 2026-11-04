package com.aritr.zinely.ui.components

import androidx.compose.ui.platform.AccessibilityManager
import org.junit.Assert.assertEquals
import org.junit.Test

/** #160: a transient bar's stay, as [transientTimeoutMillis] judges it from what the platform recommends. */
class RecommendedTimeoutTest {

    private fun recommending(millis: Long) = object : AccessibilityManager {
        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean,
        ): Long = millis
    }

    @Test
    fun `with no accessibility manager the frozen time stands`() {
        assertEquals(4_000L, null.transientTimeoutMillis(4_000L, hasAction = true))
    }

    @Test
    fun `a longer time the phone asks for is honoured, up to the longest Android offers`() {
        assertEquals(30_000L, recommending(30_000L).transientTimeoutMillis(4_000L, hasAction = true))
        assertEquals(120_000L, recommending(120_000L).transientTimeoutMillis(4_000L, hasAction = true))
    }

    @Test
    fun `before Android 10 with TalkBack a bar with a button is not left up forever`() {
        // Compose answers Long.MAX_VALUE there (no "Time to take action" setting exists before API 29), and a
        // Shelf Undo bar that never goes holds every notice and delete prompt queued behind it.
        assertEquals(
            LONGEST_ACCESSIBILITY_TIMEOUT_MILLIS,
            recommending(Long.MAX_VALUE).transientTimeoutMillis(4_000L, hasAction = true),
        )
    }

    @Test
    fun `the controls hint reaches the platform only when the bar has a button`() {
        var asked: Boolean? = null
        val manager = object : AccessibilityManager {
            override fun calculateRecommendedTimeoutMillis(
                originalTimeoutMillis: Long,
                containsIcons: Boolean,
                containsText: Boolean,
                containsControls: Boolean,
            ): Long = originalTimeoutMillis.also { asked = containsControls }
        }
        manager.transientTimeoutMillis(3_200L, hasAction = false)
        assertEquals(false, asked)
        manager.transientTimeoutMillis(3_200L, hasAction = true)
        assertEquals(true, asked)
    }
}
