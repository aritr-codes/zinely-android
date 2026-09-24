package com.aritr.zinely.ui.a11y

import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext

/**
 * Lets a `FocusRequester` move **TalkBack's** focus in touch mode.
 *
 * ## The invariant this protects
 *
 * TalkBack follows *input* focus: when a node gains it, Compose sends `TYPE_VIEW_FOCUSED` and TalkBack moves
 * its own focus there. But a `clickable` is focusable only in keyboard input mode, and a TalkBack user is in
 * **touch** mode — so `requestFocus()` on a control silently fails and TalkBack stays wherever the platform
 * put it (the first node of the new screen). The 0.9.0-beta.5 device pass found exactly that on About: every
 * focus-return `FocusRequester` was correct and none of them did anything under TalkBack, because the tests
 * that proved them all forced `InputMode.Keyboard` first.
 *
 * Place this **before** the modifier that owns the focus target (`zinelyV2Control`, `focusable`), and after
 * any `focusRequester` for it. While touch exploration is on, the target can take focus in touch mode too.
 * With it off, nothing changes — so a sighted touch user never sees a focus ring appear after Back.
 *
 * @param onlyUnderScreenReader for a non-interactive target such as a heading: focusable **only** while
 *   touch exploration is on, so a keyboard user does not get an extra Tab stop on a line of text.
 */
public fun Modifier.screenReaderFocus(onlyUnderScreenReader: Boolean = false): Modifier =
    this then ScreenReaderFocusElement(onlyUnderScreenReader)

private data class ScreenReaderFocusElement(val onlyUnderScreenReader: Boolean) :
    ModifierNodeElement<ScreenReaderFocusNode>() {
    override fun create() = ScreenReaderFocusNode(onlyUnderScreenReader)
    override fun update(node: ScreenReaderFocusNode) {
        node.onlyUnderScreenReader = onlyUnderScreenReader
    }
}

private class ScreenReaderFocusNode(var onlyUnderScreenReader: Boolean) :
    Modifier.Node(), FocusPropertiesModifierNode, CompositionLocalConsumerModifierNode {
    // Read at every focus request rather than cached, so turning TalkBack on or off needs no recomposition.
    override fun applyFocusProperties(focusProperties: FocusProperties) {
        val manager = currentValueOf(LocalContext).getSystemService(AccessibilityManager::class.java)
        val screenReader = manager?.isTouchExplorationEnabled == true
        if (screenReader) {
            focusProperties.canFocus = true
        } else if (onlyUnderScreenReader) {
            focusProperties.canFocus = false
        }
    }
}
