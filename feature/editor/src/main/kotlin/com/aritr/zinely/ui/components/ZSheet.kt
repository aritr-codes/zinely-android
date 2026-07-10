package com.aritr.zinely.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.roundToInt

/** Stable test tags (parity-plan M1: stable test tags preserved/introduced). */
public const val ZSheetScrimTestTag: String = "zSheetScrim"
public const val ZSheetSurfaceTestTag: String = "zSheetSurface"

/**
 * The frozen modal system — `.scrim` + `.sheet` — shared by all eight sheets of the trilogy.
 *
 * Deliberately NOT Material3's ModalBottomSheet (ADR-049): the frozen sheet has **no
 * drag-to-dismiss** (the grip is decorative — zero pointer handlers in the spec), and M3's sheet
 * imposes its own motion. Hosting in a plain [Dialog] instead gives window-level modality (focus
 * containment ≙ the spec's `inert`), back-dismiss ≙ Escape, and TalkBack isolation for free; the
 * scrim fade and `translateY(102%)` slide are ours, driven by the frozen `--base` motion token.
 *
 * Dismissal paths, per spec: scrim tap, system back. The Dialog stays mounted until the exit slide
 * completes, so closing is animated, not a hard cut.
 */
@Composable
public fun ZSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible
    if (!visibleState.currentState && !visibleState.targetState) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // The spec's scrim is ours (`--scrim`, animated); kill the window's own dim so they don't stack.
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }

        val motion = ZinelyTheme.motion
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(motion.base()),
                exit = fadeOut(motion.base()),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(ZSheetScrimTestTag)
                        .background(ZinelyTheme.colors.scrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                )
            }
            AnimatedVisibility(
                visibleState = visibleState,
                modifier = Modifier.align(Alignment.BottomCenter),
                // transform:translateY(102%) -> none at --base
                enter = slideInVertically(motion.base()) { (it * 1.02f).roundToInt() },
                exit = slideOutVertically(motion.base()) { (it * 1.02f).roundToInt() },
            ) {
                ZSheetSurface(title = title, sub = sub, modifier = modifier, content = content)
            }
        }
    }
}

/**
 * The sheet body without the Dialog window — split out so goldens can rasterize it in a plain host
 * (a Dialog's window is invisible to the decor-view capture harness; pre-M1 review, Required Fix 4).
 */
@Composable
internal fun ZSheetSurface(
    title: String,
    sub: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ZinelyTheme.colors
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Column(
        modifier = modifier
            .testTag(ZSheetSurfaceTestTag)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            // box-shadow:0 -18px 44px rgba(0,0,0,.28) — upward
            .zinelyShadow(
                listOf(ZinelyShadowLayer(dy = (-18).dp, blur = 44.dp, color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.28f))),
                shape,
            )
            .clip(shape)
            .background(colors.menu)
            // padding:8px 20px calc(24px + env(safe-area-inset-bottom)) — plus IME for sheets with fields
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
            .semantics { paneTitle = title },
    ) {
        // .grip: 38×4, r2, field-edge, margin 8px auto 14px
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 14.dp)
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.fieldEdge),
        )
        // h3: voice 19/600, margin 2px 2px 4px
        BasicText(
            text = title,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 4.dp),
            style = TextStyle(
                color = colors.onDesk,
                fontFamily = ZinelyTheme.typography.voice,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        // .sub: 13px on-desk-soft, margin 0 2px 16px
        if (sub != null) {
            BasicText(
                text = sub,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 16.dp),
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 13.sp,
                ),
            )
        }
        content()
    }
}
