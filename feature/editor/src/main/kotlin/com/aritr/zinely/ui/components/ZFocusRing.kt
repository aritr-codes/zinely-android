package com.aritr.zinely.ui.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.aritr.zinely.ui.theme.ZinelyDimens
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * The frozen `:focus-visible` ring: 3px solid coral-strong, offset 2px. The CSS rule's global
 * `border-radius:6px` is a lowest-common-denominator wart of a universal selector; in Compose the
 * ring follows each component's own [cornerRadius] (pass the component's radius; the frozen 6px
 * [ZinelyDimens.FocusRingRadius] is the fallback for unshaped targets).
 *
 * Driven by the component's own focus [interactionSource] — on Android, focus interactions surface
 * for keyboard/d-pad navigation, which is exactly the `:focus-visible` (not `:focus`) distinction.
 */
@Composable
public fun Modifier.zinelyFocusRing(
    interactionSource: InteractionSource,
    cornerRadius: Dp = ZinelyDimens.FocusRingRadius,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    if (!focused) return this
    val color = ZinelyTheme.colors.coralStrong
    return drawBehind {
        val width = ZinelyDimens.FocusRingWidth.toPx()
        val offset = ZinelyDimens.FocusRingOffset.toPx()
        // Ring sits outside the bounds: outline-offset 2px, stroke centred on the outline path.
        val inflate = offset + width / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(-inflate, -inflate),
            size = Size(size.width + 2 * inflate, size.height + 2 * inflate),
            cornerRadius = CornerRadius(cornerRadius.toPx() + inflate),
            style = Stroke(width = width),
        )
    }
}
