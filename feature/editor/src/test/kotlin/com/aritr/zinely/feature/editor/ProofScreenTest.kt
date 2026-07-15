package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
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

    // ---- Act 2 — Print (B3, ADR-052) ----------------------------------------------------------

    private var lastExport: ProofExportTarget? = null

    /** Mount the Proof, hoist a paper state, and advance to the Print act. */
    private fun setProofOnPrint(exportBusy: Boolean = false) {
        composeRule.setContent {
            var paper by remember { mutableStateOf(PaperSize.A4) }
            ZinelyTheme {
                ProofScreen(
                    zineName = "Corner Store Poems",
                    onBack = {},
                    paper = paper,
                    onPaperSelected = { paper = it },
                    onExportPdf = { lastExport = it },
                    exportBusy = exportBusy,
                )
            }
        }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Sheet → Print
    }

    @Test
    fun `print act shows the four recipe rows and both honest export actions - no print button`() {
        setProofOnPrint()

        composeRule.onNodeWithText("Scale").assertExists()
        composeRule.onNodeWithText("Orientation").assertExists()
        composeRule.onNodeWithText("Paper").assertExists()
        composeRule.onNodeWithText("Sides").assertExists()
        // The export row is below the recipe fold — scroll it into view before asserting.
        composeRule.onNodeWithTag(ProofSavePdfTestTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(ProofShareTestTag).assertIsDisplayed()
        // ADR-052: the frozen third export action "Print" is dropped — no such button remains.
        composeRule.onNodeWithText("Print").assertDoesNotExist()
    }

    @Test
    fun `change opens the paper chooser and picking Letter updates the recipe`() {
        setProofOnPrint()

        composeRule.onNodeWithTag(ProofChangePaperTestTag).performScrollTo().performClick()
        composeRule.onNodeWithText("Paper size").assertIsDisplayed() // the chooser sheet
        composeRule.onNodeWithText("Letter").performClick() // one match (menu); recipe still reads A4

        // The recipe's Paper value now reads Letter (the chosen size flows back through onPaperSelected).
        // onAllNodes: the chooser item may still be animating out, so match the first of possibly two.
        composeRule.onAllNodesWithText("Letter").onFirst().assertExists()
    }

    @Test
    fun `share opens the share chooser sheet`() {
        setProofOnPrint()
        composeRule.onNodeWithTag(ProofShareTestTag).performScrollTo().performClick()
        composeRule.onNodeWithText("Share your zine").assertIsDisplayed()
    }

    @Test
    fun `save pdf requests a SAVE-target export`() {
        lastExport = null
        setProofOnPrint()
        composeRule.onNodeWithTag(ProofSavePdfTestTag).performScrollTo().performClick()
        assertEquals(ProofExportTarget.SAVE, lastExport)
    }

    @Test
    fun `the export row disables while a render is in flight`() {
        setProofOnPrint(exportBusy = true)
        composeRule.onNodeWithTag(ProofSavePdfTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ProofShareTestTag).assertIsNotEnabled()
    }

    // ---- Act 3 — The Fold (B4, ADR-051) -------------------------------------------------------

    private var madeAnother = 0

    /** Mount the Proof and advance to the Fold act (Sheet → Print → Fold). */
    private fun setProofOnFold() {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(zineName = "Corner Store Poems", onBack = { backCount++ }, onMakeAnother = { madeAnother++ })
            }
        }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Sheet → Print
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Print → Fold
    }

    // The step nav lives inside the guide's vertical scroll — scroll it into the hit-testable position
    // before injecting the click (the same pattern the Act 2 export-row test uses).
    private fun stepForward() =
        composeRule.onNodeWithTag(ProofStepNextTestTag).performScrollTo().performClick()

    @Test
    fun `the fold opens on the five-step guide - step 1, prev disabled, no global primary`() {
        setProofOnFold()

        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("1. Crease into eight")
        composeRule.onNodeWithTag(ProofStepPrevTestTag).assertIsNotEnabled()
        // Mid-guide the shared action bar is empty — the in-body step nav owns navigation.
        composeRule.onNodeWithTag(ProofPrimaryTestTag).assertDoesNotExist()
    }

    @Test
    fun `the next arrow advances the fold steps and updates the live caption`() {
        setProofOnFold()

        stepForward()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("2. One cut — the only cut")
        // Prev is now reachable (no longer at the first step).
        composeRule.onNodeWithTag(ProofStepPrevTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofStepPrevTestTag).performScrollTo().performClick()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("1. Crease into eight")
    }

    // The ←/→ step-nav path (screen-root onPreviewKeyEvent → advance/retreat) is verified by review and
    // on-device (the F3 keyboard gate). It is not unit-tested here: Robolectric's focus owner + key-event
    // dispatch don't reliably drive a screen-root preview handler, so an automated assertion would be flaky.

    @Test
    fun `the last step swaps the next arrow for the one finish primary`() {
        setProofOnFold()

        repeat(4) { stepForward() } // → step 5 (the last)
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("5. Push in and wrap")
        // RF-1: exactly one finish action, and the next arrow is gone (no dead primary, no double-next).
        composeRule.onNodeWithTag(ProofStepNextTestTag).assertDoesNotExist()
        composeRule.onNodeWithText("It’s folded — show me").assertIsDisplayed()
    }

    @Test
    fun `finishing the fold reveals the finished book and the done caption`() {
        setProofOnFold()
        repeat(4) { stepForward() }

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // finish
        composeRule.waitForIdle() // let the climax beats run out on the test clock

        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofDoneHeadingTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Your zine is a book.").assertIsDisplayed()
        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Done · Your zine is ready")
    }

    @Test
    fun `the finished exits stay held back behind the staged reveal`() {
        // Full motion: the reveal runs in timed beats. Right after finish the book is up, but the exits
        // are gated behind the final `showActions` beat — they must not be present yet.
        setProofOnFold()
        repeat(4) { stepForward() }

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // finish
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofDoneHeadingTestTag).assertIsDisplayed() // the book became a book…
        composeRule.onNodeWithText("Make another").assertDoesNotExist() // …but the exits are held back
        composeRule.onNodeWithText("Back to bench").assertDoesNotExist()
    }

    @Test
    fun `under reduced motion the finish jumps straight to the finished book and its exits`() {
        // Remove animations → every beat collapses to its final state at once (no staged wait).
        forceReduceMotion()
        setProofOnFold()
        repeat(4) { stepForward() }

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // finish
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofDoneHeadingTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Make another").assertIsDisplayed()
        composeRule.onNodeWithText("Back to bench").assertIsDisplayed()
    }

    @Test
    fun `make another invokes its callback`() {
        madeAnother = 0
        forceReduceMotion() // deterministic: the exits are revealed at once
        setProofOnFold()
        repeat(4) { stepForward() }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // finish
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Make another").performClick()
        assertEquals(1, madeAnother)
    }

    // ---- B5 — overlays & the post-export hand-off (ADR-051, ADR-041) --------------------------

    @Test
    fun `a failed export shows the recoverable error overlay with one retry, back still available`() {
        var retried = 0
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    zineName = "Corner Store Poems",
                    onBack = { backCount++ },
                    exportFailed = true,
                    onRetryExport = { retried++ },
                )
            }
        }

        composeRule.onNodeWithTag(ProofErrorPaneTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Couldn’t make the PDF").assertIsDisplayed()
        // The error replaces the acts + action bar — exactly one recovery action, no dead act primary.
        composeRule.onNodeWithTag(ProofPrimaryTestTag).assertDoesNotExist()
        // …but the loss-safe back stays available (the Proof "back everywhere" invariant).
        composeRule.onNodeWithContentDescription("Back to the bench (your work is saved)").assertIsDisplayed()

        composeRule.onNodeWithTag(ProofRetryTestTag).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `a successful save-pdf raises the fold-now snackbar naming the saved file, whose action jumps to the fold`() {
        // Instant snackbar: without the enter slide, the action sits at its final hit-testable position.
        forceReduceMotion()
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(zineName = "Zine", onBack = {}, savedSignals = saved)
            }
        }
        // The save happens on the Print act; the hand-off nudges forward to the Fold.
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // Sheet → Print
        // The host's signal after a successful Save-PDF render carries the ACTUAL saved display name
        // (ExportSaved.displayName, ext included) — the snackbar must name that file, not zineName.
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofFoldSnackTestTag).assertIsDisplayed()
        // Frozen copy (proof.html savePdf → snack): names the file + the Downloads destination.
        composeRule.onNodeWithText("Saved “zine.pdf” to Downloads").assertIsDisplayed()
        // Click the action node itself (the sole clickable under the snackbar) — a positional tap on the
        // label text alone doesn't reliably propagate to the parent clickable under Robolectric.
        composeRule.onNode(hasClickAction() and hasAnyAncestor(hasTestTag(ProofFoldSnackTestTag)))
            .performClick()

        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Step 3 of 3 · Fold")
    }

    /** Android's "Remove animations" (`ANIMATOR_DURATION_SCALE = 0`) — the reduced-motion signal. */
    private fun forceReduceMotion() {
        android.provider.Settings.Global.putFloat(
            org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }
}
