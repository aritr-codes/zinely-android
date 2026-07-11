package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The **B1** Proof scaffold (M5, [ADR-051]): the 3-act frame — top bar, progress creases, act state
 * machine, per-act action bar, and act-status live region — over empty act bodies (B2–B4 content).
 * Asserts the frozen `proof.html` `setAct`/`configurePrimary` behaviour, not the (not-yet-built) content.
 * Robolectric NATIVE, matching the sibling screen suites.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var backCount = 0

    private fun setProof() {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(zineName = "Corner Store Poems", onBack = { backCount++ })
            }
        }
    }

    @Test
    fun `opens on the sheet act - step 1 caption, print-setup primary, no secondary`() {
        setProof()

        composeRule.onNodeWithTag(ProofScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Step 1 of 3 · The sheet")
        composeRule.onNodeWithText("Print setup").assertIsDisplayed()
        // Sheet has no back secondary (configurePrimary shows it only on Print).
        composeRule.onNodeWithTag(ProofSecondaryTestTag).assertDoesNotExist()
    }

    @Test
    fun `primary advances sheet to print - step 2 caption, now-fold-it primary, back secondary`() {
        setProof()

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick()

        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Step 2 of 3 · Print")
        composeRule.onNodeWithText("Now fold it").assertIsDisplayed()
        composeRule.onNodeWithTag(ProofSecondaryTestTag).assertIsDisplayed()
    }

    @Test
    fun `the back secondary on print returns to the sheet act`() {
        setProof()

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // → Print
        composeRule.onNodeWithTag(ProofSecondaryTestTag).performClick() // ← Sheet

        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Step 1 of 3 · The sheet")
        composeRule.onNodeWithText("Print setup").assertIsDisplayed()
    }

    @Test
    fun `primary on print advances to fold - step 3 caption, global primary hidden`() {
        setProof()

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // → Print
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // → Fold

        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Step 3 of 3 · Fold")
        // The fold step nav owns the primary (B4); the global primary is hidden on Fold.
        composeRule.onNodeWithTag(ProofPrimaryTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofSecondaryTestTag).assertDoesNotExist()
    }

    @Test
    fun `the sheet act shows the imposed sheet, one aria-label, and both cover cards`() {
        setProof()

        // Act 1 body (B2): the imposed sheet is present with exactly one image description…
        composeRule.onNodeWithTag(ProofSheetPreviewTestTag).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Your zine imposed on one landscape sheet: eight panels, " +
                "the top row upside-down, with one cut line across the centre.",
        ).assertIsDisplayed()
        // …and the front/back confidence cards (below the sheet — exist, may need scrolling to see).
        composeRule.onNodeWithText("Front cover").assertExists()
        composeRule.onNodeWithText("Back cover").assertExists()
    }

    @Test
    fun `the imposed sheet belongs to act 1 only - it is gone on the print act`() {
        setProof()
        composeRule.onNodeWithTag(ProofSheetPreviewTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // → Print

        composeRule.onNodeWithTag(ProofSheetPreviewTestTag).assertDoesNotExist()
    }

    @Test
    fun `the progress creases are present as one decorative node`() {
        setProof()
        composeRule.onNodeWithTag(ProofProgressTestTag).assertIsDisplayed()
    }

    @Test
    fun `loss-safe back invokes onBack`() {
        setProof()
        composeRule.onNodeWithContentDescription("Back to the bench (your work is saved)").performClick()
        assertEquals(1, backCount)
    }
}
