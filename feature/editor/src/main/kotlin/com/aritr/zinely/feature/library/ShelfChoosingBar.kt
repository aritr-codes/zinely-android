package com.aritr.zinely.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press

internal const val ShelfChoosingBarTestTag: String = "shelf-choosing-bar"
internal const val ShelfStopChoosingTestTag: String = "shelf-stop-choosing"
internal const val ShelfChoosingCountTestTag: String = "shelf-choosing-count"

/**
 * `.a47-bar` — while choosing several zines, the heading gives way to one bar that stays put while the zines
 * scroll under it (A47.3): a drawn ✕, then the count.
 *
 * ```css
 * .a47-bar{position:absolute;left:0;right:0;top:0;z-index:30;display:flex;align-items:center;gap:var(--gap-sm);
 *   padding:var(--gap-lg) var(--gap-lg) var(--gap-md) var(--gap-sm);background:var(--desk);
 *   border-bottom:1.5px dashed var(--hair)}
 * .a47-count{flex:1;font-family:var(--voice);font-weight:700;font-size:1.5rem;line-height:1.15;overflow-wrap:anywhere}
 * ```
 *
 * It takes every touch that lands on it, so nothing scrolled under it can be pressed through it.
 *
 * @param count how many are chosen; never 0 (A47.4).
 */
@Composable
internal fun ShelfChoosingBar(count: Int, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier
            .testTag(ShelfChoosingBarTestTag)
            .fillMaxWidth()
            // Being hit is enough: Compose gives a touch to the topmost sibling under it, never to the shelf
            // beneath as well. Nothing is consumed, which would cancel the ✕'s own tap.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .background(colors.desk)
            .drawBehind {
                val w = BarRule.toPx()
                drawLine(
                    colors.hair,
                    Offset(0f, size.height - w / 2f),
                    Offset(size.width, size.height - w / 2f),
                    strokeWidth = w,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(BarDash.toPx(), BarDash.toPx())),
                )
            }
            .padding(
                start = ZinelyV21Dimens.gapSm,
                top = ZinelyV21Dimens.gapLg,
                end = ZinelyV21Dimens.gapLg,
                bottom = ZinelyV21Dimens.gapMd,
            ),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StopChoosing(onStop)
        Text(
            text = Copy.Select.selectedCount(count),
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = CountTextSize,
                lineHeight = CountTextSize * 1.15f,
                color = colors.ink,
            ),
            // `role="status" aria-live="polite"`: "3 selected" is said as it changes.
            modifier = Modifier
                .weight(1f)
                .testTag(ShelfChoosingCountTestTag)
                .clearAndSetSemantics {
                    contentDescription = Copy.Select.selectedCount(count)
                    liveRegion = LiveRegionMode.Polite
                },
        )
    }
}

/**
 * `.a47-done` — the ✕, **drawn** as the back chevron and the three dots are: a mark the app owns.
 *
 * ```css
 * .a47-done{width:48px;height:48px;border:1.5px solid var(--ink);border-radius:var(--br-pill);
 *   background:var(--surface);color:var(--ink);box-shadow:2px 2px 0 var(--ink-line)}
 * .a47-done:active{transform:translate(1px,1px);box-shadow:1px 1px 0 var(--ink-line)}
 * .a47-done:focus-visible{outline:2px solid var(--ink);outline-offset:3px}
 * <path d="M2 2l10 10M12 2 2 12"/>  (viewBox 14, drawn at 14px, stroke-width 2)
 * ```
 */
@Composable
private fun StopChoosing(onStop: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .zinelyV21Pressable(pressed, ZinelyV21Press.Inline, colors.inkLine, CircleShape)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusPill, StopFocusOffset)
            .size(StopSize)
            .clip(CircleShape)
            .background(colors.surface)
            .border(StopBorder, colors.ink, CircleShape)
            .testTag(ShelfStopChoosingTestTag)
            .zinelyV2Control(label = Copy.Select.STOP_SELECTING, interactionSource = interaction, onClick = onStop),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(CrossSize)) {
            val k = size.width / CrossViewBox
            val w = CrossStroke * k
            drawLine(colors.ink, Offset(2f * k, 2f * k), Offset(12f * k, 12f * k), w, StrokeCap.Round)
            drawLine(colors.ink, Offset(12f * k, 2f * k), Offset(2f * k, 12f * k), w, StrokeCap.Round)
        }
    }
}

private val BarRule = 1.5.dp
private val BarDash = 3.dp
private val CountTextSize = 24.sp
private val StopSize = 48.dp
private val StopBorder = 1.5.dp
private val StopFocusOffset = 3.dp
private val CrossSize = 14.dp
private const val CrossViewBox = 14f
private const val CrossStroke = 2f
