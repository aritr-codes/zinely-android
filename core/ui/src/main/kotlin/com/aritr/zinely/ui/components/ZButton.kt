package com.aritr.zinely.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * The button vocabulary of the DESIGN-FROZEN trilogy. Four primitives, transcribed from the frozen
 * CSS — nothing is invented here; every scalar is a named preset pinned to its `.class` of origin
 * so a call site cannot silently render another surface's metrics (pre-M1 review, Required Fix 1).
 *
 * Hover states (`translateY(-2px)` lift, hover backgrounds/borders) are deliberately not
 * implemented: the spec itself treats hover as a pointer-only affordance (`@media (hover:none)`),
 * no parity golden ever exercises it, and every target device is touch (ADR-049). The `:active`
 * press transforms ARE implemented.
 *
 * No haptic parameters: verbs fire per action, not per widget — callers own
 * [com.aritr.zinely.ui.theme.ZinelyHaptics.perform].
 */

// ----- primary (coral CTA) ------------------------------------------------------------------

/** Frozen scalars of the one coral action per surface. */
@Immutable
public data class ZPrimaryButtonMetrics(
    val minHeight: Dp,
    val hPadding: Dp,
    val radius: Dp,
    val fontSize: TextUnit,
    val gap: Dp,
    val iconSize: Dp,
    /** Alpha of the resting `0 8px 20px rgba(198,78,52,a)` shadow layer. */
    val restShadowAlpha: Float,
) {
    public companion object {
        /** shelf.html `.start` */
        public val Shelf: ZPrimaryButtonMetrics =
            ZPrimaryButtonMetrics(56.dp, 30.dp, 18.dp, 17.sp, 10.dp, 20.dp, 0.34f)

        /** bench.html `.proof` */
        public val Bench: ZPrimaryButtonMetrics =
            ZPrimaryButtonMetrics(52.dp, 22.dp, 16.dp, 15.5.sp, 9.dp, 19.dp, 0.32f)

        /** proof.html `.primary` */
        public val Proof: ZPrimaryButtonMetrics =
            ZPrimaryButtonMetrics(54.dp, 22.dp, 16.dp, 16.sp, 10.dp, 20.dp, 0.32f)
    }
}

public enum class ZPrimaryFill { Coral, Stamp }

/**
 * The coral (or proof's `.primary.stamp`) call to action. [metrics] is required — each surface
 * passes its own frozen preset. [shadow] overrides the fill's default rest shadow (shelf's retry
 * button is `.start` metrics + stamp fill + the token `--shadow-2`, shelf.html:414).
 */
@Composable
public fun ZPrimaryButton(
    text: String,
    onClick: () -> Unit,
    metrics: ZPrimaryButtonMetrics,
    modifier: Modifier = Modifier,
    fill: ZPrimaryFill = ZPrimaryFill.Coral,
    shadow: List<ZinelyShadowLayer>? = null,
    enabled: Boolean = true,
    icon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val colors = ZinelyTheme.colors
    val background = when (fill) {
        ZPrimaryFill.Coral -> colors.coralStrong
        ZPrimaryFill.Stamp -> colors.stamp
    }
    // 0 8px 20px rgba(<fill>, a), 0 2px 0 rgba(0,0,0,.12) — first layer coral(198,78,52) or
    // stamp(38,70,83,.3) per proof.html `.primary.stamp`.
    val restShadow = shadow ?: listOf(
        ZinelyShadowLayer(
            dy = 8.dp,
            blur = 20.dp,
            color = when (fill) {
                ZPrimaryFill.Coral -> Color(0xFFC64E34).copy(alpha = metrics.restShadowAlpha)
                ZPrimaryFill.Stamp -> Color(0xFF264653).copy(alpha = 0.3f)
            },
        ),
        ZinelyShadowLayer(dy = 2.dp, blur = 0.dp, color = Color.Black.copy(alpha = 0.12f)),
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = ZinelyTheme.motion.fast(),
        label = "primaryPress",
    )
    val shape = RoundedCornerShape(metrics.radius)
    Row(
        modifier = modifier
            .zinelyFocusRing(interactionSource, metrics.radius)
            .graphicsLayer {
                // :active{ transform:translateY(1px) scale(.99) } at --fast
                translationY = press * 1.dp.toPx()
                scaleX = 1f - press * 0.01f
                scaleY = 1f - press * 0.01f
            }
            .zinelyShadow(restShadow, shape)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = metrics.minHeight)
            .padding(horizontal = metrics.hPadding),
        horizontalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Box(Modifier.size(metrics.iconSize)) { icon(Color.White) }
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = metrics.fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.01.em,
            ),
        )
    }
}

// ----- stamp (secondary emphasis, flat) -----------------------------------------------------

/**
 * bench.html/proof.html `.btn-stamp` (byte-identical): stamp fill, `--shadow-2`, no press/hover
 * transforms in the spec. Consumers: bench + proof error retry, proof empty CTA. Shelf's retry is
 * NOT this — it is [ZPrimaryButton] with [ZPrimaryButtonMetrics.Shelf], [ZPrimaryFill.Stamp] and
 * `shadow = ZinelyTheme.elevation.shadow2`.
 */
@Composable
public fun ZStampButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .zinelyFocusRing(interactionSource, 16.dp)
            .zinelyShadow(ZinelyTheme.elevation.shadow2, shape)
            .clip(shape)
            .background(ZinelyTheme.colors.stamp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color(0xFFF4EFE6),
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

// ----- icon button --------------------------------------------------------------------------

/**
 * bench.html/proof.html `.iconbtn`: 44×44, radius 12, transparent, 22px icon. The 44px visual is
 * wrapped in the frozen ≥48dp touch target ([minimumInteractiveComponentSize]). Disabled =
 * on-desk-faint at 50% (bench undo/redo).
 */
@Composable
public fun ZIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (tint: Color) -> Unit,
) {
    val colors = ZinelyTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    // 44×44 visual inside an explicit 48dp touch box — the frozen ≥48dp target. (An outer
    // minimumInteractiveComponentSize() is overridden by the inner size(44) under test; the
    // explicit wrapper is unambiguous.)
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .describedAs(contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .zinelyFocusRing(interactionSource, 12.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .alpha(if (enabled) 1f else 0.5f),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(22.dp)) {
                icon(if (enabled) colors.onDesk else colors.onDeskFaint)
            }
        }
    }
}

// ----- tool (field-filled, bordered secondary) ----------------------------------------------

/** Frozen scalars of the field-filled bordered button family. Radius is 14px in every variant. */
@Immutable
public data class ZToolButtonMetrics(
    val minHeight: Dp,
    val hPadding: Dp,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
    val gap: Dp,
    val iconSize: Dp,
    /** shelf `.sortbtn` speaks in on-desk-soft; every other variant in on-desk. */
    val softText: Boolean = false,
    /** proof `.exportrow .tool:active{ translateY(1px) }` — the only pressed rule in the family. */
    val pressTranslate: Boolean = false,
) {
    public companion object {
        /** shelf.html `.sortbtn` */
        public val ShelfSort: ZToolButtonMetrics =
            ZToolButtonMetrics(48.dp, 14.dp, 13.5.sp, FontWeight.Medium, 6.dp, 15.dp, softText = true)

        /** bench.html `.tool` (and `.tool.icononly`: pass no text) */
        public val BenchTool: ZToolButtonMetrics =
            ZToolButtonMetrics(48.dp, 15.dp, 14.sp, FontWeight.Medium, 8.dp, 18.dp)

        /** proof.html `.ghostbtn` */
        public val ProofGhost: ZToolButtonMetrics =
            ZToolButtonMetrics(52.dp, 16.dp, 14.5.sp, FontWeight.SemiBold, 8.dp, 18.dp)

        /** proof.html `.exportrow .tool` */
        public val ProofExport: ZToolButtonMetrics =
            ZToolButtonMetrics(52.dp, 14.dp, 14.5.sp, FontWeight.SemiBold, 8.dp, 19.dp, pressTranslate = true)

        /** proof.html `.stepnav button` (52×52 square: pass no text) */
        public val ProofStepNav: ZToolButtonMetrics =
            ZToolButtonMetrics(52.dp, 0.dp, 14.5.sp, FontWeight.SemiBold, 0.dp, 22.dp)
    }
}

/**
 * The `.tool`/`.ghostbtn`/`.sortbtn`/`.stepnav` family: `--field` fill, 1px `--field-edge` border,
 * radius 14. Icon-only (null [text]) renders the square variant (`.tool.icononly` 48×48, stepnav
 * 52×52 — the square side is [ZToolButtonMetrics.minHeight]). Disabled = `opacity:.4`
 * (proof stepnav). [danger] = coral-text label+icon (bench `.tool.danger`).
 */
@Composable
public fun ZToolButton(
    onClick: () -> Unit,
    metrics: ZToolButtonMetrics,
    modifier: Modifier = Modifier,
    text: String? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    icon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val colors = ZinelyTheme.colors
    val contentColor = when {
        danger -> colors.coralText
        metrics.softText -> colors.onDeskSoft
        else -> colors.onDesk
    }
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (metrics.pressTranslate && pressed) 1f else 0f,
        animationSpec = ZinelyTheme.motion.fast(),
        label = "toolPress",
    )
    val iconOnly = text == null
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .zinelyFocusRing(interactionSource, 14.dp)
            .graphicsLayer { translationY = press * 1.dp.toPx() }
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(colors.field)
            .border(1.dp, colors.fieldEdge, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .let { if (iconOnly) it.size(metrics.minHeight) else it.defaultMinSize(minHeight = metrics.minHeight) }
            .let { if (iconOnly) it else it.padding(horizontal = metrics.hPadding) }
            .let { if (contentDescription != null) it.describedAs(contentDescription) else it },
        horizontalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Box(Modifier.size(metrics.iconSize)) { icon(contentColor) }
        if (text != null) {
            BasicText(
                text = text,
                style = TextStyle(
                    color = contentColor,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = metrics.fontSize,
                    fontWeight = metrics.fontWeight,
                ),
            )
        }
    }
}

private fun Modifier.describedAs(description: String): Modifier =
    semantics { contentDescription = description }
