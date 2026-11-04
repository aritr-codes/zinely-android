package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aritr.zinely.feature.editor.FOLD_LAST_STEP
import com.aritr.zinely.feature.editor.ProofExportTarget
import com.aritr.zinely.feature.editor.ProofPrimaryTestTag
import com.aritr.zinely.feature.editor.ProofStepNextTestTag
import com.aritr.zinely.feature.editor.ProofFoldItUpTestTag
import com.aritr.zinely.feature.editor.ProofFoldLinkTestTag
import com.aritr.zinely.feature.editor.ProofOpenPdfTestTag
import com.aritr.zinely.feature.editor.ProofPrintDetailsPanel
import com.aritr.zinely.feature.editor.ProofReadyTestTag
import com.aritr.zinely.feature.editor.ProofRetryTestTag
import com.aritr.zinely.feature.editor.ProofSavePdfTestTag
import com.aritr.zinely.feature.editor.ProofScreen
import com.aritr.zinely.feature.editor.ProofShareTestTag
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #156: the Proof's text-labelled actions, read off the **platform** tree TalkBack reads.
 *
 * On the phone (W1, PR #154) Save PDF, Share, Open PDF and Fold it up were clickable `android.view.View`
 * nodes with their label on a child, so TalkBack said the word but never "button". The merged-tree
 * assertions in `ProofScreenTest` passed throughout. Each control here must be ONE clickable, enabled,
 * named `android.widget.Button`, and must not claim a checked state it does not have.
 *
 * The fold guide's "It's folded" uses the same `ProofBandButton` but lives in a `Dialog`-hosted drawer,
 * which this harness cannot reach (see `ProofPaperSegmentsA11yTest`); it is covered by the shared composable.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ProofBandButtonPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** @param label the content description the node must carry, or `null` when its own text names it. */
    private fun assertOneNamedButton(tag: String, label: String?) {
        val node = composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)
        assertEquals("$tag class", "android.widget.Button", node.className)
        assertEquals("$tag name", label, node.contentDescription)
        assertTrue("$tag clickable", node.isClickable)
        assertTrue("$tag enabled", node.isEnabled)
        assertFalse("$tag must not claim a checked state", node.isCheckable)
        assertTrue("$tag laid out", node.hasNonEmptyBounds)
    }

    private fun setBand(saved: MutableSharedFlow<String>, onOpenSavedPdf: (() -> Unit)? = {}) {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(onBack = {}, savedSignals = saved, onOpenSavedPdf = onOpenSavedPdf)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun commit_row_actions_are_named_buttons() {
        // Given the band before any save
        setBand(MutableSharedFlow(extraBufferCapacity = 1))

        // Then Save PDF and Share are each one named Button
        assertOneNamedButton(ProofSavePdfTestTag, Copy.Proof.SAVE_PDF)
        assertOneNamedButton(ProofShareTestTag, Copy.Proof.SHARE)
    }

    @Test
    fun done_row_actions_are_named_buttons() {
        // Given a saved PDF the host can open
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setBand(saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        // Then Open PDF, Share and Fold it up are each one named Button
        assertOneNamedButton(ProofOpenPdfTestTag, Copy.Proof.OPEN_PDF)
        assertOneNamedButton(ProofShareTestTag, Copy.Proof.SHARE)
        assertOneNamedButton(ProofFoldItUpTestTag, Copy.Proof.FOLD_IT_UP)
    }

    @Test
    fun folded_how_to_fold_link_is_a_named_button() {
        // Given a saved PDF, and the fold guide walked to its end and declared done
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setBand(saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofFoldItUpTestTag).performClick()
        repeat(FOLD_LAST_STEP) { composeRule.onNodeWithTag(ProofStepNextTestTag).performClick() }
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick()
        composeRule.waitForIdle()

        // Then the demoted "How to fold" link is a Button whose own text names it. It was never split:
        // the link IS the text node, so the role and the words already share one platform node.
        assertOneNamedButton(ProofFoldItUpTestTag, label = null)
        composeRule.onNodeWithTag(ProofFoldItUpTestTag).assertTextEquals(Copy.Proof.HOW_TO_FOLD)
    }

    @Test
    fun ready_row_is_a_named_button() {
        // Given the band
        setBand(MutableSharedFlow(extraBufferCapacity = 1))

        // Then the print-details opener is one Button carrying its summary
        assertOneNamedButton(ProofReadyTestTag, Copy.Proof.readyLabel(0, Copy.Paper.A4))

        // And it keeps its height: the seam's 48dp minimum, applied inside the row's padding, grew it by ~10dp
        // (about 62dp against the unchanged ~51dp). The outer `heightIn(48)` already sets the target.
        val node = composeRule.onNodeWithTag(ProofReadyTestTag).fetchSemanticsNode()
        val heightDp = node.size.height / node.layoutInfo.density.density
        assertTrue("the ready row grew to ${heightDp}dp", heightDp < 56f)
    }

    @Test
    fun error_retry_is_a_named_button() {
        // Given a failed export
        composeRule.setContent {
            ZinelyTheme { ProofScreen(onBack = {}, failedTarget = ProofExportTarget.SAVE) }
        }
        composeRule.waitForIdle()

        // Then the single recovery action is one named Button
        assertOneNamedButton(ProofRetryTestTag, Copy.Common.TRY_AGAIN)
    }

    @Test
    fun print_details_fold_link_is_a_named_button() {
        // Given the print-details panel (rendered directly: its drawer is a separate window)
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    ProofPrintDetailsPanel(paper = PaperSize.A4, onPaperSelected = {}, onOpenFold = {})
                }
            }
        }
        composeRule.waitForIdle()
        // It closes the panel's scroll: off screen, the platform builds no node for it.
        composeRule.onNodeWithTag(ProofFoldLinkTestTag).performScrollTo()
        composeRule.waitForIdle()

        // Then "See how to fold" is one named Button
        assertOneNamedButton(ProofFoldLinkTestTag, Copy.ProofPrint.SEE_HOW_TO_FOLD)
    }
}
