package com.aritr.zinely.feature.library

import android.graphics.Insets
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **The safe-area inset is consumed exactly once** — [ADR-086](docs/DECISIONS.md#adr-086) row 13.
 *
 * ```css
 * .dock{padding:22px 20px calc(22px + env(safe-area-inset-bottom))}
 * ```
 *
 * ### Why this file exists
 *
 * [ADR-084](docs/DECISIONS.md#adr-084) decision 4 named double-consumption as the risk B5 would inherit:
 * B4's dock transcribes `env(safe-area-inset-bottom)` as a **consuming** pad, so a second consumer above
 * it — a `navigationBarsPadding()` on the screen root, one line, entirely plausible — silently counts the
 * inset twice and lifts the one control the empty state offers.
 *
 * Row 13 planned a *structural* assertion ("no `windowInsetsPadding` on the root"), and B5 first shipped
 * that as a **code comment**. The mid-package review refused it: a comment is not an artifact, and under
 * Robolectric's zero bottom inset the planned mutation changes no bounds at all, so nothing in the package
 * could have killed it. [ADR-087](docs/DECISIONS.md#adr-087) requires a row to terminate on an artifact.
 *
 * ### So the inset is *supplied*, rather than waited for
 *
 * Robolectric reports a zero bottom inset at every qualifier — which is why the ADR marks the *numeric*
 * half ∅ and sends it to device Pass 1. But "consumed once" is a claim about a **difference**, not about a
 * value: dispatch a known inset into the view hierarchy, and a screen that consumes it once moves the dock
 * by exactly that much, while a screen that consumes it twice moves it by twice as much. The zero-inset
 * baseline is measured in the same run, so the assertion is on the delta and needs no frozen pixel.
 *
 * ### ⚠️ The delta below is smaller evidence than it reads as
 *
 * Measured while adding a second case here: `dispatchApplyWindowInsets` on this `ComponentActivity` is
 * **applied by the decor view as padding**, because nothing in this harness calls `enableEdgeToEdge`. The
 * content view therefore *shrinks* by 48 and the `AndroidComposeView` below it sees an already-consumed
 * inset — the root's `boundsInRoot` goes `0..960` → `0..912` on dispatch, and Compose's
 * `WindowInsets.navigationBars` reads **zero** throughout.
 *
 * So the 48dp the assertion measures is the window getting shorter, not the dock consuming anything. The
 * test still kills the double-consumption defect it was written for — a second consumer would still not
 * move the button, and half two still catches a root pad — but it does **not** prove the dock consumes,
 * and it cannot observe anything else that reads the inset. The attempted second case (that the shelf's
 * bottom clearance grows with the dock's, see [zineDockClearance]) was deleted rather than kept green:
 * under a zero inset it could only ever have asserted `0 == 0`. That claim belongs to device Pass 1,
 * which is where ADR-086 row 13 already sends the numeric half.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [30])
class ZineLibraryInsetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a bottom safe-area inset lifts the dock's control by exactly one inset`() {
        composeRule.setContent {
            ZinelyTheme {
                ZineLibraryScreen(
                    state = LibraryShelfState.Loading,
                    events = emptyFlow(),
                    onOpenZine = {},
                    onShareExport = {},
                    onStartZine = {},
                    onRenameZine = { _, _ -> },
                    onDuplicateZine = {},
                    onDeleteZine = {},
                    onDeleteUndo = {},
                    onDeleteCommit = {},
                    onRetry = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()

        val baseline = startBottom()
        dispatchBottomInset(INSET)
        val lifted = startBottom()

        // Half one: the dock consumes the inset. Zero consumers give a delta of 0 and the one control the
        // empty state offers sits under the navigation bar.
        assertEquals(
            "the dock does not consume the safe-area inset",
            INSET.toFloat(),
            baseline - lifted,
            HALF_PIXEL,
        )

        // Half two, and the one row 13 is actually about: **the screen root consumes nothing.** A
        // `navigationBarsPadding()` here is consumption-aware, so it would NOT move the button — the delta
        // above stays exactly 48 and half one passes — but it inarguably shrinks the workspace: the shelf
        // stops 48dp short of the bottom edge, losing that much scroll room, and the dock's gradient no
        // longer reaches the edge it fades into. The region is what shows it, so the region is what is
        // measured.
        assertEquals(
            "the screen root pads its bottom — something above the dock is consuming the inset too",
            bounds(ZineLibraryTestTag),
            bounds(ZineLibraryShelfTestTag),
        )
    }

    private fun bounds(tag: String) =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** The bottom edge of the `Make a zine` control — what the user's thumb has to reach. */
    private fun startBottom(): Float =
        composeRule.onNodeWithTag(ZineStartTestTag).fetchSemanticsNode().boundsInRoot.bottom

    private fun dispatchBottomInset(bottom: Int) {
        // The platform type, not the compat one: `:feature:editor` does not depend on `androidx.core`
        // directly, and API 30 (this file's qualifier) is where `WindowInsets.Type` lands.
        val insets = WindowInsets.Builder()
            .setInsets(WindowInsets.Type.systemBars(), Insets.of(0, 0, 0, bottom))
            .build()
        composeRule.runOnUiThread {
            composeRule.activity.window.decorView.dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
    }

    private companion object {
        /** Large enough that a doubled consumption is unmistakable, and a whole number of pixels at 1x. */
        const val INSET = 48
        const val HALF_PIXEL = 0.5f
    }
}
