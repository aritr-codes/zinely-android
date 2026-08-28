package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag as semanticsTestTag
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.FlipAxis
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import kotlinx.coroutines.delay

internal const val FlipTrayTestTag = "flip-tray"
internal const val FlipLeftRightTestTag = "flip-left-right"
internal const val FlipTopBottomTestTag = "flip-top-bottom"
internal const val FlipDoneTestTag = "flip-done"

/** Frozen Bench A22: one compact, immediately-applied two-axis tray for one Photo or Art piece. */
@Composable
internal fun FlipTray(
    visible: Boolean,
    element: Element?,
    onToggle: (FlipAxis) -> Unit,
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
            val shape = RoundedCornerShape(ZinelyV21Dimens.radiusLg)
            val trayWidth = maxWidth
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .heightIn(max = maxHeight - 24.dp)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(FlipTrayTestTag),
                color = colors.surface,
                contentColor = colors.ink,
                shape = shape,
                border = BorderStroke(1.5.dp, colors.ink),
                shadowElevation = ZinelyV21Dimens.gapXs,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(ZinelyV21Dimens.gapMd),
                    verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Copy.Editor.FLIP,
                            fontFamily = ZinelyV21Fonts.Voice,
                            fontWeight = FontWeight.Bold,
                        )
                        Button(
                            onClick = onDone,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                                .testTag(FlipDoneTestTag),
                            shape = RoundedCornerShape(percent = 50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.leaf,
                                contentColor = colors.onLeaf,
                            ),
                        ) { Text(Copy.Editor.DONE) }
                    }
                    Text(
                        text = Copy.Editor.FLIP_HELP,
                        color = colors.inkSoft,
                        fontFamily = ZinelyV21Fonts.Work,
                    )

                    val horizontal = element.horizontalFlipOn()
                    val vertical = element.verticalFlipOn()
                    val stack = trayWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
                    if (stack) {
                        Column(verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
                            FlipChoice(
                                label = Copy.Editor.LEFT_RIGHT,
                                checked = horizontal,
                                axis = FlipAxis.HORIZONTAL,
                                testTag = FlipLeftRightTestTag,
                                focusRequester = firstFocusRequester,
                                onToggle = onToggle,
                            )
                            FlipChoice(
                                label = Copy.Editor.TOP_BOTTOM,
                                checked = vertical,
                                axis = FlipAxis.VERTICAL,
                                testTag = FlipTopBottomTestTag,
                                onToggle = onToggle,
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
                            FlipChoice(
                                label = Copy.Editor.LEFT_RIGHT,
                                checked = horizontal,
                                axis = FlipAxis.HORIZONTAL,
                                testTag = FlipLeftRightTestTag,
                                focusRequester = firstFocusRequester,
                                onToggle = onToggle,
                                modifier = Modifier.weight(1f),
                            )
                            FlipChoice(
                                label = Copy.Editor.TOP_BOTTOM,
                                checked = vertical,
                                axis = FlipAxis.VERTICAL,
                                testTag = FlipTopBottomTestTag,
                                onToggle = onToggle,
                                modifier = Modifier.weight(1f),
                            )
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
            firstFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun FlipChoice(
    label: String,
    checked: Boolean,
    axis: FlipAxis,
    testTag: String,
    onToggle: (FlipAxis) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val content = if (checked) colors.onLeaf else Color(0xFF27270F)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(if (checked) colors.leaf else colors.paper, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
            .border(1.5.dp, colors.ink, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
            .toggleable(value = checked, role = Role.Button) { onToggle(axis) }
            .clearAndSetSemantics {
                // The visible label is a child of toggleable. Name the actionable parent explicitly so the
                // platform AccessibilityNodeInfo does not expose an unnamed checkable View to TalkBack.
                contentDescription = when (axis) {
                    FlipAxis.HORIZONTAL -> Copy.A11y.FLIP_LEFT_RIGHT
                    FlipAxis.VERTICAL -> Copy.A11y.FLIP_TOP_BOTTOM
                }
                role = Role.Button
                toggleableState = ToggleableState(checked)
                stateDescription = if (checked) Copy.BenchVerbs.COPIER_ON else Copy.BenchVerbs.COPIER_OFF
                onClick { onToggle(axis); true }
                semanticsTestTag = testTag
            }
            .padding(ZinelyV21Dimens.gapSm),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (axis == FlipAxis.HORIZONTAL) Icons.Filled.SwapHoriz else Icons.Filled.SwapVert,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = content,
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Element?.horizontalFlipOn(): Boolean = when (this) {
    is ImageElement -> flippedHorizontally
    is DecorElement -> mirrored
    else -> false
}

private fun Element?.verticalFlipOn(): Boolean = when (this) {
    is ImageElement -> flippedVertically
    is DecorElement -> flippedVertically
    else -> false
}
