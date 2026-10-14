package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ReorderOp
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import kotlinx.coroutines.delay

/** Test tag on the tray card; its controls are tagged `"$ArrangeTrayTestTag-<spoken label>"`. */
internal const val ArrangeTrayTestTag: String = "arrange-tray"
internal const val ArrangeDoneTestTag: String = "arrange-done"

/** `.arr-row button` keeps A14's look: no ground, pill tint under the finger, a 17dp glyph. */
private val ArrangeButtonShape: Shape = RoundedCornerShape(percent = 50)
private val ArrangeGlyphSize = 17.dp

/**
 * The Arrange tray, frozen Bench A33 ([ADR-129](../../../../../../../../docs/DECISIONS.md#adr-129)).
 *
 * The single-pointer twin of the drag gestures (ADR-029 §6, WCAG 2.5.7): move, size, turn and layer the
 * selected piece by taps. Until A33 these buttons were a two-row bar in the screen's layout that took its
 * height from the page on every select (D-113 item 2). Now the `Arrange` verb opens them here, in a card
 * that replaces the verb bar exactly as the Flip tray (A22) does, so the page keeps one size.
 *
 * Every button dispatches the same intent, at the same step, as the element's own accessibility actions
 * ([EditorA11y]); each tap is one undo step. Nudge, scale and rotate act on the reducer's selection;
 * layer is id-scoped, which is why the tray takes [elementId].
 *
 * The tray does not close when the selection moves to another piece; the host closes it on Done, Back,
 * deselect, a text session, Reframe and a page change.
 */
// The arrows and turns use the non-AutoMirrored icons on purpose: page-space "left" is screen-left in
// any layout direction, so RTL mirroring would point the arrow the wrong way.
@Suppress("DEPRECATION")
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArrangeTray(
    visible: Boolean,
    elementId: String?,
    dispatch: (Intent) -> Unit,
    onDone: () -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val motion = if (ZinelyTheme.motion.reduceMotion) 0 else 180
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(motion)) + slideInVertically(tween(motion)) { 10 },
        exit = fadeOut(tween(motion)) + slideOutVertically(tween(motion)) { 10 },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val colors = ZinelyTheme.v21Colors
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .heightIn(max = maxHeight - 24.dp)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics { paneTitle = Copy.BenchVerbs.ARRANGE }
                    .testTag(ArrangeTrayTestTag),
                color = colors.surface,
                contentColor = colors.ink,
                shape = RoundedCornerShape(ZinelyV21Dimens.radiusLg),
                border = BorderStroke(1.5.dp, colors.ink),
                shadowElevation = ZinelyV21Dimens.gapXs,
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = ZinelyV21Dimens.gapSm,
                        top = ZinelyV21Dimens.gapSm,
                        end = ZinelyV21Dimens.gapSm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Copy.BenchVerbs.ARRANGE,
                            fontFamily = ZinelyV21Fonts.Voice,
                            fontWeight = FontWeight.Bold,
                        )
                        FlipDoneButton(
                            onClick = onDone,
                            testTag = ArrangeDoneTestTag,
                            modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 48.dp),
                        )
                    }
                    // Only the groups scroll: the title and Done stay put, so a tray taller than the canvas
                    // at large text cannot lose its exit (D-112).
                    FlowRow(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = ZinelyV21Dimens.gapSm),
                        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
                        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
                    ) {
                        ArrangeGroup(Copy.Editor.ARRANGE_MOVE) {
                            ArrangeButton(Icons.Filled.KeyboardArrowLeft, Copy.A11y.MOVE_LEFT, firstFocusRequester) {
                                dispatch(Intent.Nudge(PtPoint(-EditorA11y.NUDGE_STEP_PT, 0.0)))
                            }
                            ArrangeButton(Icons.Filled.KeyboardArrowUp, Copy.A11y.MOVE_UP) {
                                dispatch(Intent.Nudge(PtPoint(0.0, -EditorA11y.NUDGE_STEP_PT)))
                            }
                            ArrangeButton(Icons.Filled.KeyboardArrowDown, Copy.A11y.MOVE_DOWN) {
                                dispatch(Intent.Nudge(PtPoint(0.0, EditorA11y.NUDGE_STEP_PT)))
                            }
                            ArrangeButton(Icons.Filled.KeyboardArrowRight, Copy.A11y.MOVE_RIGHT) {
                                dispatch(Intent.Nudge(PtPoint(EditorA11y.NUDGE_STEP_PT, 0.0)))
                            }
                        }
                        ArrangeGroup(Copy.Editor.ARRANGE_SIZE) {
                            ArrangeButton(Icons.Filled.Remove, Copy.A11y.MAKE_SMALLER) {
                                dispatch(Intent.ScaleBy(1.0 / EditorA11y.SCALE_STEP_FACTOR))
                            }
                            ArrangeButton(Icons.Filled.Add, Copy.A11y.MAKE_LARGER) {
                                dispatch(Intent.ScaleBy(EditorA11y.SCALE_STEP_FACTOR))
                            }
                        }
                        ArrangeGroup(Copy.Editor.ARRANGE_TURN) {
                            ArrangeButton(Icons.Filled.RotateLeft, Copy.A11y.ROTATE_COUNTERCLOCKWISE) {
                                dispatch(Intent.RotateBy(-EditorA11y.ROTATE_STEP_DEGREES))
                            }
                            ArrangeButton(Icons.Filled.RotateRight, Copy.A11y.ROTATE_CLOCKWISE) {
                                dispatch(Intent.RotateBy(EditorA11y.ROTATE_STEP_DEGREES))
                            }
                        }
                        ArrangeGroup(Copy.Editor.ARRANGE_LAYER) {
                            ArrangeButton(Icons.Filled.FlipToBack, Copy.A11y.SEND_BACKWARD) {
                                elementId?.let { dispatch(Intent.Reorder(it, ReorderOp.SEND_BACKWARD)) }
                            }
                            ArrangeButton(Icons.Filled.FlipToFront, Copy.A11y.BRING_FORWARD) {
                                elementId?.let { dispatch(Intent.Reorder(it, ReorderOp.BRING_FORWARD)) }
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            // Let AnimatedVisibility publish its child before moving accessibility focus.
            withFrameNanos { }
            if (motion > 0) delay(motion.toLong())
            runCatching { firstFocusRequester.requestFocus() }
        }
    }
}

/** `.arr-group`: a visible name over its buttons. The name is a heading, so a screen reader can jump by group. */
@Composable
private fun ArrangeGroup(name: String, buttons: @Composable () -> Unit) {
    Column {
        Text(
            text = name,
            color = ZinelyTheme.v21Colors.inkSoft,
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.92.sp,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapHair)) { buttons() }
    }
}

/**
 * One ≥48dp control: a 17dp `inkSoft` glyph over no ground, inside a pill that tints `butterTint` under
 * the finger. The spoken label is the button's name; the glyph is decorative.
 */
@Composable
private fun ArrangeButton(
    icon: ImageVector,
    description: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fire = benchTap(ZinelyHaptic.Tick, onClick)
    IconButton(
        onClick = fire,
        interactionSource = interaction,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .testTag("$ArrangeTrayTestTag-$description")
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                // ⚠ `clearAndSetSemantics` also deletes the OnClick action IconButton set, and the platform
                // derives `isClickable` from that action alone: without this line TalkBack met these as
                // non-clickable buttons (measured on SM-A176B for the bar these replace).
                onClick { fire(); true }
            },
    ) {
        Box(
            modifier = Modifier
                .clip(ArrangeButtonShape)
                .background(if (pressed) colors.butterTint else Color.Transparent)
                .padding(ZinelyV21Dimens.gapSm),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.inkSoft,
                modifier = Modifier.size(ArrangeGlyphSize),
            )
        }
    }
}
