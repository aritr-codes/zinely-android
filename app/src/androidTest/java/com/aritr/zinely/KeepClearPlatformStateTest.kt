package com.aritr.zinely

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.ElementSemanticsLayer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **OD-49's verification debt, closed on hardware.**
 *
 * [ADR-102 §12.12](../../../../../../docs/DECISIONS.md#adr-102-p2c) gives an element outside the printer's
 * reach a spoken **state**, and P2c's device pass could not read it back: `uiautomator dump` emits no
 * `state-description` attribute, `dumpsys accessibility` dumps no node attributes, and Samsung's TTS logs no
 * utterance text. The pass therefore accepted the drawn mark on evidence and **booked the spoken half**.
 *
 * This is the closing move, and it exists because a Compose semantics assertion is not evidence about the
 * platform. `ElementSemanticsLayerTest` reads the *merged semantics tree*; TalkBack reads
 * [AccessibilityNodeInfo]. The two disagree often enough that this project has already shipped a defect
 * through the gap — `ReframeControls.ZoomButton` passed `assertIsNotEnabled` in Robolectric while telling the
 * platform it was enabled ([ADR-058](../../../../../../docs/DECISIONS.md#adr-058)), and `BenchPageGrid`'s
 * `Role.Button` cells reported `selected=false` to the platform while the semantics tree said otherwise. So
 * this test walks the **real** node tree, through `UiAutomation`, and reads what a screen reader would get.
 *
 * It also re-verifies a change made *after* P2c's device pass was accepted: a review found that an explicit
 * `stateDescription` **replaces** Compose's own *"Selected"* / *"Not selected"*, and that for `Role.Button`
 * that default is the only channel selection reaches the platform through. The warning therefore carries the
 * selection word ([Copy.A11y.outsidePrintReachState]) — a user-visible accessibility change, which by the
 * mandatory-verification rule cannot stand on the pass that predates it.
 *
 * ⚠ **API 30+.** `AccessibilityNodeInfo.getStateDescription` is API 30; the app's `minSdk` is 24, where the
 * same state travels in the compat extras bundle instead. The behaviour is not gated — only this reading of
 * it is.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class KeepClearPlatformStateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** 100×100pt page against [com.aritr.zinely.core.imposition.Imposer]'s 17pt inset. */
    private val pageSizePt = PtSize(100.0, 100.0)

    /** x=2pt: across the boundary on the left, by 15pt. */
    private val crossing = Transform(xPt = 2.0, yPt = 40.0, widthPt = 20.0, heightPt = 20.0)

    private fun show(selection: Set<String>) {
        val state = EditorUiState(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(
                    Page(
                        index = 0,
                        role = PageRole.INTERIOR,
                        elements = listOf(TextElement(id = "a", transform = crossing, text = "hi")),
                    ),
                ),
            ),
            currentPageIndex = 0,
            selection = selection,
            view = ViewState(screenPxPerPt = 2f),
            interaction = Interaction.Idle,
            canUndo = false,
            canRedo = false,
        )
        composeRule.setContent {
            MaterialTheme {
                ElementSemanticsLayer(
                    uiState = state,
                    dispatch = {},
                    modifier = Modifier.size(200.dp, 200.dp),
                    pageSizePt = pageSizePt,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Every `stateDescription` in the live window, read from the platform tree.
     *
     * Polled rather than read once: Compose exports its semantics to the platform asynchronously once an
     * accessibility client is attached, and obtaining `UiAutomation` is what attaches one. A single read
     * immediately after `waitForIdle()` is a race that fails empty — which would look exactly like the
     * defect this test is meant to detect, and is the trap
     * [DEVICE-VERIFICATION.md](../../../../../../docs/DEVICE-VERIFICATION.md) warns costs an hour.
     */
    private fun platformStateDescriptions(): List<String> {
        // ⚠ Touching `uiAutomation` is what attaches the accessibility client, and Compose only begins
        // exporting semantics to the platform *after* it observes one. Reading `rootInActiveWindow` in the
        // same breath returns a tree with the ComposeView present and none of its content — which is
        // indistinguishable from the defect this test looks for. The first cut of this probe did exactly
        // that and reported `states=[]` against an app that was answering correctly all along; a tree dump
        // is what told the two apart. Settle first, then poll.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        Thread.sleep(SETTLE_MS)
        repeat(POLL_ATTEMPTS) {
            val found = mutableListOf<String>()
            automation.rootInActiveWindow?.let { collectStates(it, found) }
            if (found.isNotEmpty()) return found
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return emptyList()
    }

    private fun collectStates(node: AccessibilityNodeInfo, into: MutableList<String>) {
        node.stateDescription?.toString()?.takeIf { it.isNotBlank() }?.let { into += it }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectStates(it, into) }
        }
    }

    @Test
    fun a_selected_element_outside_the_printers_reach_says_so_to_the_platform() {
        show(selection = setOf("a"))
        val states = platformStateDescriptions()
        assertTrue(
            "the platform tree carried no state description at all — states=$states",
            states.isNotEmpty(),
        )
        assertTrue(
            "no node told the platform the content leaves the printer's reach — states=$states",
            states.any { it.contains(Copy.A11y.OUTSIDE_PRINT_REACH) },
        )
        assertTrue(
            "the warning replaced the selection word instead of carrying it — states=$states",
            states.any { it == Copy.A11y.outsidePrintReachState(selected = true) },
        )
    }

    /**
     * The drawn mark needs a selection; the spoken one does not — a maker exploring the page by touch meets
     * elements they have never selected, and *"this will be cut off"* is what they are exploring to find out.
     * The selection word still has to be right, which here means *"Not selected"*.
     */
    @Test
    fun an_unselected_element_outside_the_reach_still_says_so_and_still_says_not_selected() {
        show(selection = emptySet())
        val states = platformStateDescriptions()
        assertTrue(
            "no node reported the reach warning for an unselected element — states=$states",
            states.any { it == Copy.A11y.outsidePrintReachState(selected = false) },
        )
    }

    private companion object {
        const val SETTLE_MS = 1_000L
        const val POLL_ATTEMPTS = 20
        const val POLL_INTERVAL_MS = 250L
    }
}
