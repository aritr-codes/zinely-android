package com.aritr.zinely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * How a `role="menuitemradio"` item marks its selection. The two frozen surfaces that have radio
 * menus made **opposite** choices — this is spec-driven divergence, not speculative flexibility:
 * - [WeightAndCheck] — shelf sort menu: selected = weight 600 + a trailing check, deliberately not
 *   coral (`shelf.html` "selection carried by the check glyph + weight, not coral").
 * - [Coral] — proof paper menu: selected = coral-text label **and** the leading check glyph
 *   revealed (`proof.html`: the item's leading svg IS the check, opacity 0 when unselected).
 */
public enum class ZSelectedStyle { WeightAndCheck, Coral }

/**
 * One `.menu-list button`: min-height 52, radius 10, gap 14, 15.5sp shell, 19px icon in
 * on-desk-soft; `danger` speaks coral-text. Plain action items pass `selected = null`; radio items
 * pass true/false plus the surface's [selectedStyle]. Menus are plain [Column]s at call sites —
 * the spec's `.menu-list` has no styling of its own worth a component (pre-M1 review).
 */
@Composable
public fun ZMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (tint: Color) -> Unit)? = null,
    danger: Boolean = false,
    subLabel: String? = null,
    selected: Boolean? = null,
    selectedStyle: ZSelectedStyle = ZSelectedStyle.WeightAndCheck,
) {
    val colors = ZinelyTheme.colors
    val isSelected = selected == true
    val coralSelected = isSelected && selectedStyle == ZSelectedStyle.Coral
    val contentColor = when {
        danger -> colors.coralText
        coralSelected -> colors.coralText
        else -> colors.onDesk
    }
    val iconTint = when {
        danger -> colors.coralText
        coralSelected -> colors.coralText
        else -> colors.onDeskSoft
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .zinelyFocusRing(interactionSource, 10.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = if (selected != null) Role.RadioButton else Role.Button,
                onClick = onClick,
            )
            .let { if (selected != null) it.semantics { this.selected = selected } else it }
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            // Proof-style radio: the leading glyph slot IS the check, hidden until selected.
            selected != null && selectedStyle == ZSelectedStyle.Coral ->
                Box(Modifier.size(19.dp).alpha(if (isSelected) 1f else 0f)) { CheckGlyph(iconTint) }
            icon != null ->
                Box(Modifier.size(19.dp)) { icon(iconTint) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = contentColor,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 15.5.sp,
                    fontWeight = if (isSelected && selectedStyle == ZSelectedStyle.WeightAndCheck) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                ),
            )
            if (subLabel != null) {
                BasicText(
                    text = subLabel,
                    style = TextStyle(
                        color = colors.onDeskFaint,
                        fontFamily = ZinelyTheme.typography.shell,
                        fontSize = 11.5.sp,
                    ),
                )
            }
        }
        // Shelf-style radio: trailing check revealed on selection (`.check{ margin-left:auto }`).
        if (selected != null && selectedStyle == ZSelectedStyle.WeightAndCheck) {
            Box(Modifier.size(19.dp).alpha(if (isSelected) 1f else 0f)) { CheckGlyph(contentColor) }
        }
    }
}

/** The spec's check: `M5 12l4 4 10-10`, stroke 2.2, round caps, on a 24-unit viewBox. */
@Composable
internal fun CheckGlyph(tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / 24f
        val path = Path().apply {
            moveTo(5f * u, 12f * u)
            lineTo(9f * u, 16f * u)
            lineTo(19f * u, 6f * u)
        }
        drawPath(
            path,
            color = tint,
            style = Stroke(width = 2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

