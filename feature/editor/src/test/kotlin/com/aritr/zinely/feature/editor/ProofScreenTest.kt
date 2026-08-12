package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.components.ZSheetCloseTestTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Proof surface frame, as of [ADR-101](../../../../../../../docs/DECISIONS.md#adr-101) **P1**: the
 * reader, the band beneath it, and the two bottom drawers that replaced the Sheet → Print → Fold climb.
 *
 * **What this suite stopped asserting, and why that is the point.** Until P1 it asserted `setAct` /
 * `configurePrimary`: three step captions, a per-act primary and secondary, the imposed sheet appearing in
 * act 1 and vanishing in act 2, and three progress creases. None of those exist now — not because they
 * regressed but because the accepted V2.1 design retires the climb. The behaviour that had to *survive*
 * the restructure is what this suite now guards, and it is deliberately the same list ADR-101 §3 names:
 * the print recipe, the export contract, the fold guide and its climax, the error overlay, and the
 * ADR-041 hand-off that names the saved file.
 *
 * Robolectric NATIVE, matching the sibling screen suites.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A real phone window, not Robolectric's 320x470dp default — which is smaller than any shipped device and
// simply drops content that falls outside it. `SurfaceTraversalOrderTest` pins the same qualifiers for the
// same reason, having once asserted a Proof surface whose step navigation was below the viewport.
//
// It matters more here than it looks: the drawers are bottom-anchored and wrap their content, so the
// available body height is a function of the screen. At 470dp the fold drawer's climax reveal is clipped —
// mounted, laid out, and unreachable, with no scroll parent to rescue it. That is a genuine small-screen
// defect in hosting the whole climax in a drawer, it is evidence for the frozen design's choice to raise
// `.done` in the *band* instead, and it is carried to ADR-101 §5's open question rather than fixed here.
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ProofScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var backCount = 0

    private fun setProof(startDrawer: ProofDrawer = ProofDrawer.None) {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    onBack = { backCount++ },
                    startDrawer = startDrawer,
                )
            }
        }
    }

    // ---- The surface frame ---------------------------------------------------------------------

    @Test
    fun `the surface lands on the reader with one opener and no climb`() {
        setProof()

        composeRule.onNodeWithTag(ProofScreenTestTag).assertIsDisplayed()
        // The band's single opener, in place of a bottom bar that reconfigured itself per act.
        composeRule.onNodeWithTag(ProofReadyTestTag).assertIsDisplayed()
        // The retired climb's chrome: no step captions, and no secondary to step back with.
        composeRule.onNodeWithText("Print setup").assertDoesNotExist()
        composeRule.onNodeWithText("Now fold it").assertDoesNotExist()
    }

    /**
     * The drawers are the whole navigation state, and they are mutually exclusive by construction
     * ([ProofDrawer] is an enum precisely so "both open" is unrepresentable). This asserts the observable
     * half of that: opening the details drawer does not leave the fold guide mounted behind it.
     */
    @Test
    fun `the ready row opens the print details, and only that drawer`() {
        setProof()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()

        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertDoesNotExist()
    }

    @Test
    fun `the top bar's fold icon opens the fold drawer`() {
        setProof()

        composeRule.onNodeWithTag(ProofFoldOpenTestTag).performClick()

        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertIsDisplayed()
    }

    /**
     * ADR-051's *"you can always leave, nothing is lost"*, unchanged in meaning and extended in reach: the
     * top-bar back is the loss-safe exit, and it no longer does two different things depending on which
     * act you were standing in, because there are none. A drawer is dismissed by system back or the scrim
     * — window-level modality [ZSheet][com.aritr.zinely.ui.components.ZSheet] gets from hosting in a
     * Dialog — so the top-bar back is only ever reachable with every drawer shut, and cannot be the thing
     * that strands a user inside one.
     */
    @Test
    fun `the loss-safe back leaves the surface, and says so`() {
        setProof()

        composeRule.onNodeWithContentDescription("Back to the bench (your work is saved)").performClick()

        assertEquals(1, backCount)
    }

    /**
     * **Back dismisses an open drawer before it will leave the surface — so no drawer can strand you.**
     *
     * This is the clause [ADR-101](../../../../../../../docs/DECISIONS.md#adr-101) §3 item 3 records as a
     * *modification* of [ADR-051]'s "you can always leave, nothing is lost", and it shipped in P1 asserted
     * in prose and in a KDoc with **no test behind it at all** until review said so. The mechanism is
     * `ZSheet`'s `Dialog` host, which routes system back to `onDismissRequest` — but "the component we
     * used probably does that" is precisely the kind of claim this repository does not accept from a
     * summary, and it is one line to check.
     */
    @Test
    fun `system back dismisses an open drawer instead of leaving the surface`() {
        setProof(ProofDrawer.Details)
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertIsDisplayed()

        org.robolectric.shadows.ShadowDialog.getLatestDialog()?.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        assertEquals("dismissing a drawer is not leaving the surface", 0, backCount)
        // …and the surface is still there behind it, with its opener ready to be used again.
        composeRule.onNodeWithTag(ProofReadyTestTag).assertIsDisplayed()
    }

    // ---- The band: `.ready`, `.commit`, `.done` (ADR-101 P2) ------------------------------------

    private var lastExport: ProofExportTarget? = null

    /** Eight pages, so the band's summary counts something real. */
    private fun pages(count: Int = 8): List<Page> = (0 until count).map { i ->
        Page(index = i, role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR)
    }

    /**
     * Mount the Proof with a hoisted paper state, the export edge captured, and a live `savedSignals`
     * the test can fire to reach `.done`.
     */
    private fun setProofBand(
        exportBusy: Boolean = false,
        saved: MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 1),
        startDrawer: ProofDrawer = ProofDrawer.None,
    ) {
        composeRule.setContent {
            var paper by remember { mutableStateOf(PaperSize.A4) }
            ZinelyTheme {
                ProofScreen(
                    onBack = { backCount++ },
                    paper = paper,
                    onPaperSelected = { paper = it },
                    onExportPdf = { lastExport = it },
                    exportBusy = exportBusy,
                    savedSignals = saved,
                    pages = pages(),
                    startDrawer = startDrawer,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Mount the Proof with a hoisted paper state and the print details already open. */
    private fun setProofOnDetails() = setProofBand(startDrawer = ProofDrawer.Details)

    /**
     * `.ready` is **one** control announced as one string — not a heading a screen reader has to
     * assemble from three siblings, and not the frozen `aria-label="Print details"` that would have
     * thrown the summary away. The destination rides on the click action instead.
     */
    @Test
    fun `the ready row summarises the job in one label and names its destination on the action`() {
        setProofBand()

        composeRule.onNodeWithTag(ProofReadyTestTag)
            .assertContentDescriptionEquals(Copy.Proof.readyLabel(8, Copy.Paper.A4))
        // The sighted reading is the same two lines, not a truncation of them.
        composeRule.onNodeWithText(Copy.Proof.READY_WHEN_YOU_ARE).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.Proof.readySummary(8, Copy.Paper.A4)).assertIsDisplayed()

        val stop = composeRule.onNodeWithTag(ProofReadyTestTag).fetchSemanticsNode()
        assertEquals(
            Copy.Proof.PRINT_DETAILS,
            stop.config[SemanticsActions.OnClick].label,
        )
    }

    /** Before any save the band commits; the completion is not pre-drawn. */
    @Test
    fun `the band offers both commit actions and no completion until something is saved`() {
        setProofBand()

        composeRule.onNodeWithTag(ProofSavePdfTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofShareTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofDoneTestTag).assertDoesNotExist()
    }

    @Test
    fun `save pdf requests a SAVE-target export`() {
        lastExport = null
        setProofBand()
        composeRule.onNodeWithTag(ProofSavePdfTestTag).performClick()
        assertEquals(ProofExportTarget.SAVE, lastExport)
    }

    /**
     * Share goes straight to the OS chooser. The frozen `#shareSheet` asked *"Save to Files"* or *"Send
     * to an app"* first — two rows that called the same code, because on Android that one chooser is
     * where the user picks Files *or* an app. A menu whose branches are indistinguishable teaches the
     * user that this app's choices are decorative; it was deleted, not reworded.
     */
    @Test
    fun `share requests a SEND-target export with no intermediate menu`() {
        lastExport = null
        setProofBand()
        composeRule.onNodeWithTag(ProofShareTestTag).performClick()
        assertEquals(ProofExportTarget.SEND, lastExport)
        composeRule.onNodeWithText("Save to Files").assertDoesNotExist()
    }

    /**
     * The single-flight. The frozen prototype has no busy state for these buttons because `doSave()` is
     * instant in HTML and an eight-page render is not; without this a double-tap fires two concurrent
     * renders at the same destination.
     */
    @Test
    fun `the commit row disables while a render is in flight`() {
        setProofBand(exportBusy = true)
        composeRule.onNodeWithTag(ProofSavePdfTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ProofShareTestTag).assertIsNotEnabled()
    }

    /**
     * CI-93 on the **platform** tree: the busy commit row must report `enabled = false` to TalkBack, not
     * merely to the merged semantics tree the assertion above reads — the `f4faaa4` defect was exactly
     * that disagreement. (Class is not asserted: a ZToolButton's `Role.Button` is set via `clickable()`
     * over merged content, so it surfaces as `android.view.View` — the reported CI-30 divergence.)
     */
    @Test
    fun `the busy commit row reports disabled buttons on the platform tree`() {
        setProofBand(exportBusy = true)

        val save = composeRule.onNodeWithTag(ProofSavePdfTestTag).platformNode(composeRule.activity)
        assertFalse("Save PDF must be disabled to the platform while a render is in flight", save.isEnabled)

        val share = composeRule.onNodeWithTag(ProofShareTestTag).platformNode(composeRule.activity)
        assertFalse("Share must be disabled to the platform while a render is in flight", share.isEnabled)
    }

    /**
     * The [ADR-041](../../../../../../../docs/DECISIONS.md#adr-041) hand-off and the
     * [ADR-054](../../../../../../../docs/DECISIONS.md#adr-054) promise inside it, now carried by the
     * band's `.done` block rather than by a snackbar that expired in five seconds. It names the file the
     * exporter *actually* wrote, which is what makes "we kept a copy" checkable rather than asserted.
     */
    @Test
    fun `a successful save raises the completion naming the saved file, whose action opens the fold`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProofBand(saved = saved)

        // The host's signal after a successful Save-PDF render carries the ACTUAL saved display name
        // (ExportSaved.displayName, ext included) — `.done` must name that file, not zineName.
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Saved to your phone").assertIsDisplayed()
        // …and it carries ADR-052's recipe in three words. This is the last sentence read before the
        // walk to a printer, and the drawer holding the full version is one nothing forces you through.
        composeRule.onNodeWithText(
            "In Downloads — “zine.pdf”. Print it at 100%, landscape, one side — then fold it up.",
        ).assertIsDisplayed()
        // The commit pair gives way to the completion — no second Save sitting beside a finished one.
        composeRule.onNodeWithTag(ProofSavePdfTestTag).assertDoesNotExist()

        composeRule.onNodeWithTag(ProofFoldItUpTestTag).performClick()
        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertIsDisplayed()
    }

    /**
     * **The departure from the frozen band, asserted so it cannot be undone by accident.** `.band.saved`
     * hides `.ready` as well as `.commit`; here it does not. The print recipe is most needed *after* the
     * PDF exists — that is when the user walks to a printer — and hiding it on save makes the guidance
     * unreachable at the moment it pays for itself.
     */
    @Test
    fun `the completion keeps the print details reachable, under a heading named for that moment`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProofBand(saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        // Keeping the row and leaving its heading alone was the first version of this, and it put
        // "Ready when you are" directly above "Saved to your phone" — ready for what, I just did it.
        composeRule.onNodeWithText(Copy.Proof.READY_WHEN_YOU_ARE).assertDoesNotExist()
        composeRule.onNodeWithText(Copy.Proof.BEFORE_YOU_PRINT).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofReadyTestTag)
            .assertContentDescriptionEquals(Copy.Proof.readyLabel(8, Copy.Paper.A4, saved = true))

        composeRule.onNodeWithTag(ProofReadyTestTag).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertIsDisplayed()
    }

    /**
     * A saved PDF was imposed for the paper that was chosen when it was rendered. Change the paper and
     * "Saved to your phone" would go on being true about a file that no longer matches the recipe beside
     * it — so the completion drops, which is also the only route back to Save.
     */
    @Test
    fun `changing the paper clears the completion so the pdf can be re-saved`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProofBand(saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        composeRule.onNodeWithText(Copy.Paper.LETTER).performScrollTo().performClick()
        org.robolectric.shadows.ShadowDialog.getLatestDialog()?.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofDoneTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofSavePdfTestTag).assertIsDisplayed()
    }

    // ---- The print details drawer (ADR-052's recipe) ---------------------------------------------

    /**
     * [ADR-052](../../../../../../../docs/DECISIONS.md#adr-052)'s four recipe rows, which exist because a
     * home printer will silently ruin a zine — *actual size, not "fit to page"*; *landscape, or the fold
     * breaks*; the paper; single-sided.
     *
     * **This is the assertion ADR-101 §3 item 4 was written to make load-bearing.** The frozen
     * `v21-proof.html` details drawer contains none of these rows; searching it for their phrases returns
     * one hit, and that hit is a code comment about sheet geometry. Nothing but this test stands between
     * the re-skin and quietly deleting guidance that saves a real user a wasted sheet of paper.
     */
    @Test
    fun `the print details carry the four recipe rows - and no print button`() {
        setProofOnDetails()

        composeRule.onNodeWithText("Scale").assertExists()
        composeRule.onNodeWithText("Orientation").assertExists()
        composeRule.onNodeWithText("Paper").assertExists()
        composeRule.onNodeWithText("Sides").assertExists()
        // ADR-052: the frozen third export action "Print" is dropped — no such button remains.
        composeRule.onNodeWithText("Print").assertDoesNotExist()
    }

    /**
     * The imposed sheet lives inside the details drawer — and, since P3, in its **last** section, as the
     * illustration for *"how it becomes a booklet"* rather than the first thing a drawer titled "Print
     * details" shows you. Hence the scroll: it is below the fold of one panel now, not the top half of
     * two stacked ones.
     */
    @Test
    fun `the imposed sheet is in the print details, not on the reader`() {
        setProof()
        composeRule.onNodeWithTag(ProofSheetPreviewTestTag).assertDoesNotExist()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()

        composeRule.onNodeWithTag(ProofSheetPreviewTestTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Your zine imposed on one landscape sheet: eight panels, " +
                "the top row upside-down, with one cut line across the centre.",
        ).assertIsDisplayed()
    }

    /**
     * The paper question is answered **inside** the panel now. P3 deleted the chooser sheet it used to
     * raise — a `Dialog` over the `Dialog` already holding this panel, for a two-option question whose
     * two options fit on one row of the panel that asked it.
     */
    @Test
    fun `the paper segments answer inline and picking Letter updates the recipe`() {
        setProofOnDetails()

        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Paper size").assertDoesNotExist() // the retired chooser sheet
        segment(Copy.Paper.LETTER).performClick()

        // Selection is announced as a radio group, not as two unrelated buttons.
        segment(Copy.Paper.LETTER).assertIsSelected()
        segment(Copy.Paper.A4).assertIsNotSelected()
        // …and the recipe's Paper row follows it: two "Letter" nodes now, the segment and the row.
        composeRule.onAllNodesWithText(Copy.Paper.LETTER).assertCountEquals(2)
    }

    /** One paper segment, addressed inside the group — "Letter" also appears in the recipe's Paper row. */
    private fun segment(label: String) = composeRule.onNode(
        hasText(label) and hasAnyAncestor(hasTestTag(ProofPaperSegmentsTestTag)),
    )

    /**
     * The frozen `.dclose`, owed since P1 and paid here. A drawer covers most of the screen with content
     * that scrolls and named no way out; scrim tap and system back worked, but neither is visible.
     */
    @Test
    fun `each drawer draws a close button that dismisses it`() {
        setProofOnDetails()
        composeRule.onNodeWithTag(ZSheetCloseTestTag).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        assertEquals("closing a drawer is not leaving the surface", 0, backCount)

        // "Each", plural — the P3 review pointed out this test asserted one drawer and named two. The
        // fold drawer is the one that most needs the affordance: it is the taller of the two.
        composeRule.onNodeWithContentDescription(Copy.Proof.HOW_TO_FOLD).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ZSheetCloseTestTag).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZSheetCloseTestTag).assertDoesNotExist()
        assertEquals("closing a drawer is not leaving the surface", 0, backCount)
    }

    /**
     * **The stale-file notice — the P3 design review's highest finding, and the one that is a trust trap
     * rather than a rough edge.**
     *
     * Changing the paper after a save correctly drops `.done`: the PDF in Downloads was imposed for the
     * old size. But it happens inside a `Dialog`-backed drawer, so the band mutates behind a scrim and the
     * user meets the effect without the cause — *"Saved to your phone" became "Save PDF" while I wasn't
     * looking; it deleted my file*. That is the `0.9.0-beta.1` Preview shape exactly. The notice is the
     * fix, and the clause about the old file still being in Downloads is the part that does the work.
     */
    @Test
    fun `changing the paper after a save says so instead of silently dropping the completion`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProofBand(saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofPaperChangedTestTag).assertDoesNotExist()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        segment(Copy.Paper.LETTER).performClick()
        composeRule.onNodeWithTag(ZSheetCloseTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofDoneTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofPaperChangedTestTag)
            .assertIsDisplayed()
            .assertTextEquals(
                Copy.Proof.paperChangedResave(newPaper = Copy.Paper.LETTER, oldPaper = Copy.Paper.A4),
            )
    }

    /**
     * And it is only raised by a change that *invalidates something*. Picking a paper size before any save
     * has nothing to retract, so a warning there would be noise teaching the user to ignore the real one.
     */
    @Test
    fun `changing the paper before a save raises no notice`() {
        setProofOnDetails()
        segment(Copy.Paper.LETTER).performClick()
        composeRule.onNodeWithTag(ZSheetCloseTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofPaperChangedTestTag).assertDoesNotExist()
    }

    /**
     * The honest replacement for the frozen `ALL SET` checklist: three facts about the artifact, and the
     * two false ticks — *"everything sits safely inside the edges"*, *"your photos are sharp enough to
     * print"* — nowhere on the surface. The second is asserted absent by name because it is the one that
     * would be a green mark beside a check this repository does not perform at all.
     */
    @Test
    fun `the panel reassures with facts it can back, and draws no tick it cannot`() {
        setProofOnDetails()

        composeRule.onNodeWithTag(ProofAlreadyDoneTestTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(Copy.ProofPrint.ALREADY_CUT).assertExists()
        composeRule.onNodeWithText(Copy.ProofPrint.ALREADY_MARGIN).assertExists()
        composeRule.onNodeWithText("Your photos are sharp enough to print", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Everything sits safely inside the edges", substring = true)
            .assertDoesNotExist()
    }

    /**
     * The panel names a print dialog this app never opens, so it has to say where that dialog is — and the
     * section titled *how it becomes a booklet* has to say how, rather than ending at a picture.
     */
    @Test
    fun `the panel says the app does not print, and points at the fold guide`() {
        setProofOnDetails()

        composeRule.onNodeWithText(Copy.ProofPrint.DIALOG_HINT).assertExists()
        composeRule.onNodeWithTag(ProofFoldLinkTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()

        // The fold drawer replaced the details drawer; the panel behind it is gone, not stacked.
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        composeRule.onNodeWithText(Copy.ProofFold.INTRO_BODY).assertExists()
    }

    /**
     * The test-sheet card — the freeze's strongest wasted-sheet guard, which existed nowhere in Compose
     * until P3. One node to a screen reader: the bold lead and the body are one sentence.
     */
    @Test
    fun `the panel carries the test-sheet card`() {
        setProofOnDetails()
        composeRule.onNodeWithTag(ProofTestSheetTestTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            Copy.ProofPrint.TEST_SHEET_LEAD + Copy.ProofPrint.TEST_SHEET_BODY,
        ).assertExists()
    }

    // ---- The fold drawer, its guide and its climax ----------------------------------------------


    /** Mount the Proof with the fold drawer already open. */
    private fun setProofOnFold() {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    onBack = { backCount++ },
                    startDrawer = ProofDrawer.Fold,
                )
            }
        }
    }

    // The step nav lives inside the guide's vertical scroll — scroll it into the hit-testable position
    // before injecting the click.
    private fun stepForward() =
        composeRule.onNodeWithTag(ProofStepNextTestTag).performClick()

    @Test
    fun `the fold drawer opens on the eight-step guide - step 1, prev disabled`() {
        setProofOnFold()

        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 1 OF 8")
        composeRule.onNodeWithTag(ProofStepPrevTestTag).assertIsNotEnabled()
        // CI-93: the disabled Previous must report disabled to the PLATFORM tree TalkBack reads (f4faaa4).
        val prev = composeRule.onNodeWithTag(ProofStepPrevTestTag).platformNode(composeRule.activity)
        assertFalse("Previous must be disabled to the platform on the first fold step", prev.isEnabled)
        // Mid-guide there is no finish action yet — it appears only on the last step.
        composeRule.onNodeWithTag(ProofPrimaryTestTag).assertDoesNotExist()
    }

    @Test
    fun `the next arrow advances the fold steps and prev walks them back`() {
        setProofOnFold()

        stepForward()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 2 OF 8")
        composeRule.onNodeWithTag(ProofStepPrevTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofStepPrevTestTag).performClick()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 1 OF 8")
    }

    // The ←/→ step-nav path now lives on the fold drawer's own content rather than the surface root,
    // because ZSheet hosts in a Dialog and a root handler would never see keys aimed at another window.
    // Still not unit-tested here: Robolectric's focus owner + key dispatch don't reliably drive a preview
    // handler, so an automated assertion would be flaky. Verified by review and on device (the F3 gate).

    /**
     * The finish action moved into the drawer in P1, and this test is why it had to move somewhere
     * deliberate: it used to live in the per-act bottom bar, which the restructure deletes. Left alone it
     * would have taken "It's folded — show me" with it.
     */
    @Test
    fun `the last step swaps the next arrow for the one finish primary`() {
        setProofOnFold()

        repeat(FOLD_LAST_STEP) { stepForward() } // → step 8 (the last)
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 8 OF 8")
        // Exactly one finish action, and the next arrow is gone (no dead primary, no double-next).
        composeRule.onNodeWithTag(ProofStepNextTestTag).assertDoesNotExist()
        // No longer "— show me": there is nothing left to show. The climax it promised is retired
        // (ADR-101 §6.8); the button acknowledges and closes the drawer.
        composeRule.onNodeWithText("It’s folded").assertIsDisplayed()
    }

    /**
     * **The dots are a way back, not decoration** (ADR-101 P4).
     *
     * The frozen markup has always made each one a button with its own label and `aria-current`; Compose
     * drew eight discs under `clearAndSetSemantics`. At five steps that was a shrug. At eight it means the
     * likeliest thing to happen mid-fold — losing your place — costs six arrow-presses to check, and a
     * screen-reader user is told nothing about position beyond the counter.
     */
    @Test
    fun `any fold step can be reached directly from its dot`() {
        setProofOnFold()

        composeRule.onNodeWithTag(proofStepDotTag(5)).performClick()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 6 OF 8")
        // …and back to a step already passed, which is the case the arrows make expensive.
        composeRule.onNodeWithTag(proofStepDotTag(1)).performClick()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 2 OF 8")
    }

    /** The dot for the step you are on is the selected one, and exactly one is. */
    @Test
    fun `the current fold step is the selected dot`() {
        setProofOnFold()
        stepForward()

        composeRule.onNodeWithTag(proofStepDotTag(1)).assertIsSelected()
        composeRule.onNodeWithTag(proofStepDotTag(0)).assertIsNotSelected()
        composeRule.onNodeWithTag(proofStepDotTag(7)).assertIsNotSelected()
    }

    /**
     * The legend, and the line that makes a step checkable.
     *
     * Neither existed in Compose before P4. The legend is what stops eight diagrams being eight separate
     * puzzles — **five** marks whose meaning never changes between steps. It shipped with three, and both
     * additions were bug fixes rather than features: `move` because every arrow was drawing in the cut's
     * red, and `push or pull` because steps 4 and 7 use the hollow Yoshizawa–Randlett *action* arrow, which
     * states a force rather than a destination and so cannot be folded into `move` without saying something
     * untrue. All five are asserted here, because the fifth is the one a clipped row loses first and this
     * is the test named for the legend. The `.foldnow` line is the only
     * thing that tells a user whether they did the step *right*: an instruction says what to do, and only
     * this says what the paper should look like afterwards.
     */
    @Test
    fun `the guide carries the fixed legend and says what you should be holding`() {
        setProofOnFold()

        composeRule.onNodeWithTag(ProofFoldLegendTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.ProofFold.LEGEND_CREASE).assertExists()
        composeRule.onNodeWithText(Copy.ProofFold.LEGEND_FOLD_NOW).assertExists()
        composeRule.onNodeWithText(Copy.ProofFold.LEGEND_CUT).assertExists()
        composeRule.onNodeWithText(Copy.ProofFold.LEGEND_MOVE).assertExists()
        composeRule.onNodeWithText(Copy.ProofFold.LEGEND_ACT).assertExists()

        // Unmerged: the caption and this line are deliberately one live region, so the merged tree has
        // only the parent — which is the property the drawer wants and the reason the tag hides.
        composeRule.onNodeWithTag(ProofStepHoldingTestTag, useUnmergedTree = true)
            .assertTextEquals(Copy.ProofFold.STEP_HOLDING[0])
        stepForward()
        // Unmerged: the caption and this line are deliberately one live region, so the merged tree has
        // only the parent — which is the property the drawer wants and the reason the tag hides.
        composeRule.onNodeWithTag(ProofStepHoldingTestTag, useUnmergedTree = true)
            .assertTextEquals(Copy.ProofFold.STEP_HOLDING[1])
    }

    /**
     * Eight steps, each exactly one physical action ([V21-SPEC §5.2]). The built five were a *different*
     * sequence, not a shorter one: its step 1 was *"fold the sheet in half three times, then open it
     * flat"* — three actions in one instruction, to someone holding paper for the first time.
     */
    @Test
    fun `the guide walks eight steps and the precondition leaves after the first`() {
        setProofOnFold()

        Copy.ProofFold.STEP_CAPTIONS.forEachIndexed { i, caption ->
            if (i > 0) stepForward()
            composeRule.onNodeWithTag(ProofStepTitleTestTag)
                .assertTextEquals(Copy.ProofFold.stepOf(i + 1, 8).uppercase())
            composeRule.onNodeWithText(caption).assertExists()
            // **Step 1 only** — the frozen `#foldIntro` gate. A precondition stops being useful the
            // moment you are past it, and *"Got your printed sheet?"* asked at step 5 of someone visibly
            // holding one costs two lines in a drawer that is short of them.
            //
            // The gate was briefly removed, because on a device its disappearance moved the nav row 108px
            // between steps 1 and 2 and a second tap in the same place hit a dot instead of the arrow.
            // Showing it always fixed that by deleting the variable; what actually needed fixing was the
            // anchoring. The drawer now wraps its content and sits on the screen's bottom edge, so losing
            // the subtitle shrinks it from the **top** and the controls do not move —
            // `ProofFoldNarrowTest` asserts exactly that, at two font scales.
            val precondition = composeRule.onNodeWithText(Copy.ProofFold.INTRO_BODY)
            if (i == 0) precondition.assertExists() else precondition.assertDoesNotExist()
        }
    }

    /**
     * **The guide ends by getting out of the way.** It used to end in a revealed book — a schematic
     * drawing of a booklet shown to somebody holding the real one — with two exits wired to the same
     * destination. [ADR-101 §6.8](../../../../../../../docs/DECISIONS.md#adr-101-p4-device) retired it.
     * What is asserted now is the whole of the contract: the drawer closes, the surface is intact behind
     * it, and the guide is back at step 1 for a second copy.
     */
    @Test
    fun `finishing the fold closes the drawer and leaves the guide ready to run again`() {
        setProofOnFold()
        repeat(FOLD_LAST_STEP) { stepForward() }
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 8 OF 8")

        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // finish
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertDoesNotExist()
        // The zine is what you land on — the surface behind the drawer, not a celebration of it.
        composeRule.onNodeWithTag(ProofReadyTestTag).assertIsDisplayed()

        // Reopened, the guide starts over rather than resuming a fold that is done.
        composeRule.onNodeWithTag(ProofFoldOpenTestTag).performClick()
        composeRule.onNodeWithTag(ProofStepTitleTestTag).assertTextEquals("STEP 1 OF 8")
    }

    /**
     * **The band hears the claim** — [ADR-101 §6.9](../../../../../../../docs/DECISIONS.md#adr-101-p4-climax).
     *
     * The finish button asks the user to *declare* something, and for one review round declaring it changed
     * nothing on screen: the band still headlined *"Saved to your phone"* over *"…then fold it up"* under a
     * leaf stamp primary reading **Fold it up**. Behaviourally correct — the save had not changed — and read
     * cold as *the app wasn't listening*, which is [ADR-058](../../../../../../../docs/DECISIONS.md#adr-058)'s
     * failure one screen over. Asking for a report and not reacting to it is worse than not asking.
     *
     * The guide stays reachable, because forgetting step 8 five seconds later is ordinary; what must not
     * survive is the *instruction*, and the surviving control is asserted to be the quiet one by its label.
     */
    @Test
    fun `declaring the fold done answers the user instead of repeating the instruction`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProofBand(saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofFoldItUpTestTag).performClick()
        repeat(FOLD_LAST_STEP) { stepForward() }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick() // "It's folded"
        composeRule.waitForIdle()

        composeRule.onNodeWithText(Copy.Proof.NICE_THATS_A_ZINE).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.Proof.foldedInDownloads("zine.pdf")).assertIsDisplayed()
        // The instruction is gone in both places it was stated — the headline's tail and the primary.
        composeRule.onNodeWithText(Copy.Proof.savedInDownloads("zine.pdf")).assertDoesNotExist()
        composeRule.onNodeWithText(Copy.Proof.FOLD_IT_UP).assertDoesNotExist()
        // Demoted, not deleted: same destination, quiet label.
        composeRule.onNodeWithTag(ProofFoldItUpTestTag).assertTextEquals(Copy.Proof.HOW_TO_FOLD)
        composeRule.onNodeWithTag(ProofFoldItUpTestTag).performClick()
        composeRule.onNodeWithTag(ProofFoldGuideTestTag).assertIsDisplayed()
    }

    // ---- Overlays & the post-export hand-off (ADR-041, ADR-054) --------------------------------

    /**
     * The error replaces the reader **and** the band, leaving only the loss-safe back. That single
     * behaviour is what answers the still-open D-066 / OD-32 without the frozen band needing a failure
     * state of its own: a recovery action never has to compete with a live Save.
     */
    @Test
    fun `a failed export shows the recoverable error overlay with one retry, back still available`() {
        var retried = 0
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    onBack = { backCount++ },
                    exportFailed = true,
                    onRetryExport = { retried++ },
                )
            }
        }

        composeRule.onNodeWithTag(ProofErrorPaneTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Couldn’t make the PDF").assertIsDisplayed()
        // The band is replaced too — no opener sitting beside a failure notice.
        composeRule.onNodeWithTag(ProofReadyTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofPrimaryTestTag).assertDoesNotExist()
        // …but the loss-safe back stays available (the Proof "back everywhere" invariant).
        composeRule.onNodeWithContentDescription("Back to the bench (your work is saved)").assertIsDisplayed()

        composeRule.onNodeWithTag(ProofRetryTestTag).performClick()
        assertEquals(1, retried)
    }

    // The ADR-041 / ADR-054 post-export hand-off is asserted with the band above: P2 retired the
    // transient snackbar that used to carry it in favour of the persistent `.done` block.

    /** Android's "Remove animations" (`ANIMATOR_DURATION_SCALE = 0`) — the reduced-motion signal. */
    private fun forceReduceMotion() {
        android.provider.Settings.Global.putFloat(
            org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }
}
