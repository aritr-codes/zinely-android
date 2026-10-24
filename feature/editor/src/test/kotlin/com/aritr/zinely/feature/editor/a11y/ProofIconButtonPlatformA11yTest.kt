package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.ICON_BACK
import com.aritr.zinely.feature.editor.ProofIconButton
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The Proof's bordered back pill, reused as the Bench's way back to My Shelf (A42). On the phone
 * (SM-A176B, Android 16, W1 09ae4668) the platform tree gave the clickable node an empty content-desc and class
 * `View`, with the label on a non-clickable child. TalkBack reads that tree, so the name and the role must sit on
 * the one clickable node.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofIconButtonPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun assertNamedButton(label: String) {
        // Given the pill with its label
        composeRule.setContent {
            ZinelyTheme {
                ProofIconButton(onClick = {}, contentDescription = label, path = ICON_BACK, modifier = Modifier.testTag("pill"))
            }
        }

        // Then the merged tree has one clickable node, named, a Button
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNodeWithTag("pill")
            .assertHasClickAction()
            .assert(hasContentDescription(label))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        // And no other node carries the label, merged or not
        composeRule.onAllNodesWithContentDescription(label, useUnmergedTree = true).assertCountEquals(1)

        // And the platform node TalkBack reads is that same clickable, named Button
        val node = composeRule.onNodeWithTag("pill").platformNode(composeRule.activity)
        assertTrue(node.isClickable)
        assertEquals(label, node.contentDescription)
        assertEquals("android.widget.Button", node.className)
    }

    @Test
    fun bench_back_to_shelf_is_one_named_button() = assertNamedButton(Copy.Status.BACK_TO_SHELF_SAVED)

    @Test
    fun proof_back_is_one_named_button() = assertNamedButton(Copy.Proof.BACK_TO_BENCH_SAVED)

    @Test
    fun how_to_fold_is_one_named_button() = assertNamedButton(Copy.Proof.HOW_TO_FOLD)
}
