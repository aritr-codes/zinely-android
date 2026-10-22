package com.aritr.zinely.feature.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * W1 task 6 / #146: with Android's "Remove animations" on, a page change puts the filmstrip at its target
 * at once; a smooth slide is motion the reader asked not to have.
 */
@RunWith(RobolectricTestRunner::class)
class BenchPageNavReducedMotionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_reduced_motion_when_the_page_changes_then_the_filmstrip_is_at_its_target_after_one_frame() {
        android.provider.Settings.Global.putFloat(
            org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        val pages = (0 until 12).map { Page(index = it, role = PageRole.INTERIOR, elements = emptyList()) }
        var current by mutableIntStateOf(0)
        composeRule.setContent {
            ZinelyTheme {
                BenchPageNav(
                    pages = pages,
                    currentPageIndex = current,
                    pageSizePt = PtSize(100.0, 130.0),
                    defaults = DocumentDefaults(),
                    onSelectPage = {},
                    onOpenGrid = {},
                )
            }
        }
        composeRule.waitForIdle()
        fun scrolled() = composeRule.onNodeWithTag(BenchFilmstripTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange].value()

        composeRule.mainClock.autoAdvance = false
        current = 9
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        val afterOneFrame = scrolled()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val settled = scrolled()
        assertTrue("page 9 must scroll the strip", settled > 0f)
        assertEquals(settled, afterOneFrame, 0.5f)
    }
}
