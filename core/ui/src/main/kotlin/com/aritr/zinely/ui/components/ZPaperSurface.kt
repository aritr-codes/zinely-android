package com.aritr.zinely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * A physical paper sheet, not a Material card: paper fill, the `0 2px 0 --paper-edge` hard edge
 * under the blur ladder, and the signature left **bound-edge** gradient with asymmetric corners
 * (tighter on the spine side). The three surfaces use it at different scales — shelf covers
 * (3/5/5/3), bench pages and proof minis/book (2/4/4/2) —
 * so radii/edge are parameters carrying each caller's frozen values, not invented presets.
 *
 * Frozen bound-edge tuples per consumer (post-M1 review, Required Fix): shelf covers 7dp @ .14;
 * bench pages 6dp @ .10; proof mini-pages 4dp @ .10 (`.fbcard .mini-page::before`); proof book
 * cover 5dp @ .14 (`.book .cover::before`) — proof has TWO differently-scaled paper surfaces.
 */
@Composable
public fun ZPaperSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(topStart = 2.dp, topEnd = 4.dp, bottomEnd = 4.dp, bottomStart = 2.dp),
    shadow: List<ZinelyShadowLayer> = ZinelyTheme.elevation.shadow2,
    boundEdgeWidth: Dp = 6.dp,
    boundEdgeAlpha: Float = 0.10f,
    usePaper2: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = ZinelyTheme.colors
    val paper = if (usePaper2) colors.paper2 else colors.paper
    val hardEdge = ZinelyShadowLayer(dy = 2.dp, blur = 0.dp, color = colors.paperEdge)
    Box(
        modifier = modifier
            .zinelyShadow(shadow + hardEdge, shape)
            .clip(shape)
            .background(paper)
            // ::before — the bound spine: linear-gradient(90deg, rgba(0,0,0,a), transparent)
            .drawBehind {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = boundEdgeAlpha), Color.Transparent),
                        endX = boundEdgeWidth.toPx(),
                    ),
                    size = Size(boundEdgeWidth.toPx(), size.height),
                )
            },
        content = content,
    )
}
