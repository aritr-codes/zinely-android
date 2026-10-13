package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Library on a small phone (360x640) with the system text size turned up. Each case is a control or
 * a line of copy that was cut off there with no way to reach it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp", sdk = [28])
class LibrarySmallPhoneTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the empty shelf's paragraph is whole and reachable at the largest text`() {
        setContent(fontScale = 2f) { ZineShelfEmpty() }

        val body = composeRule.onNodeWithText(EMPTY_BODY, substring = true)
        assertFalse("the paragraph is cut short", body.textLayout().didOverflowHeight)
        body.performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the shelf that did not open keeps Try again whole and reachable at the largest text`() {
        setContent(fontScale = 2f) { ZineShelfFail(onRetry = {}) }

        val retry = composeRule.onNodeWithTag(ZineRetryTestTag)
        assertTrue("Try again is squeezed", retry.fetchSemanticsNode().size.height >= 48)
        retry.performScrollTo().assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun setContent(fontScale: Float, content: @Composable BoxScope.() -> Unit) {
        // A Dialog reads the text size from its own context, not from `LocalDensity`, so a sheet is only
        // at this size if the activity is.
        composeRule.activity.resources.run {
            configuration.fontScale = fontScale
            @Suppress("DEPRECATION")
            updateConfiguration(configuration, displayMetrics)
        }
        composeRule.setContent {
            ZinelyTheme {
                CompositionLocalProvider(
                    LocalZinelyMotion provides ZinelyMotion(reduceMotion = true),
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) {
                    Box(Modifier.fillMaxSize(), content = content)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val got = fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("no text layout to read", got == true && results.isNotEmpty())
        return results.first()
    }

    private companion object {
        const val EMPTY_BODY = "One sheet of paper becomes"
    }
}
