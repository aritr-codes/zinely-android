package com.aritr.zinely.ui.components

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press

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
 *
 * ### V2.1 — ADR-102 P8
 *
 * Every button in the three V2.1 prototypes is the same object: **paper or a flat content colour,
 * under a `1.5px solid var(--ink)` border, at `--br-pill`, over a `Npx Npx 0 var(--ink-line)` printed
 * shadow.** What varies is the fill and the depth. So the four primitives below keep their metrics
 * presets and their signatures — five call sites in two other modules are pinned to them — and change
 * only what they paint:
 *
 * - **the press is no longer an animation.** `animateFloatAsState` driving `translationY` + `scale`
 *   is deleted; the object offsets down-right and its shadow shortens, in one step, via
 *   [zinelyV21Pressable]. There is nothing to downgrade under `prefers-reduced-motion` because the
 *   state change *is* the position;
 * - **the soft coral/stamp bloom is deleted.** V2.1 has no blurred shadow on any button. The
 *   [ZPrimaryButton] `shadow` parameter and [ZPrimaryButtonMetrics.restShadowAlpha] survive as API
 *   (concurrent call sites still pass them) but are **no longer painted** — noted at each;
 * - **`--field` / `--field-edge` / coral / stamp are gone from the palette.** The fills below are the
 *   corpus's own `--leaf` / `--paper`, and the inks are `--ink` / `--ink-soft` / `--jam-text`;
 * - **no `--frame` ring.** `.start` and `.btn-save` both wear one, and both are *one per screen*. A
 *   shared primitive that hard-coded it would put a ring on every screen that mounts it, so the ring
 *   stays at the call site ([BenchBottomBar]'s `.add` is the pattern).
 */

// ----- primary (the screen's filled action) --------------------------------------------------

/** Frozen scalars of the one filled action per surface. */
@Immutable
public data class ZPrimaryButtonMetrics(
    val minHeight: Dp,
    val hPadding: Dp,
    val radius: Dp,
    val fontSize: TextUnit,
    val gap: Dp,
    val iconSize: Dp,
    /**
     * **Vestigial under V2.1 and no longer painted.** It was the alpha of the resting
     * `0 8px 20px rgba(198,78,52,a)` bloom; V2.1 draws no blurred shadow on any button. Kept because
     * removing a property of a public data class is a source-breaking change, and P8 is explicitly
     * forbidden from moving this API while other packages are editing its call sites.
     */
    val restShadowAlpha: Float,
) {
    public companion object {
        /** `v21-library.html .start` */
        public val Shelf: ZPrimaryButtonMetrics =
            ZPrimaryButtonMetrics(56.dp, 30.dp, ZinelyV21Dimens.radiusPill, 17.sp, 10.dp, 20.dp, 0.34f)

        /** `v21-bench.html .add` */
        public val Bench: ZPrimaryButtonMetrics =
            ZPrimaryButtonMetrics(52.dp, 22.dp, ZinelyV21Dimens.radiusPill, 15.5.sp, 9.dp, 19.dp, 0.32f)

        /** `v21-proof.html .btn-save` */
        public val Proof: ZPrimaryButtonMetrics =
            ZPrimaryButtonMetrics(54.dp, 22.dp, ZinelyV21Dimens.radiusPill, 16.sp, 10.dp, 20.dp, 0.32f)
    }
}

/**
 * The two fills the corpus gives a full-width action.
 *
 * The names are V2's and are kept because they are public API; what they *mean* is now
 * `v21-proof.html`'s pair — [Coral] is `.btn-save`'s `--leaf` on `--on-leaf`, [Stamp] is
 * `.btn-share`'s `--paper` on `--ink-soft`. Neither coral nor stamp exists in the V2.1 palette.
 */
public enum class ZPrimaryFill { Coral, Stamp }

/**
 * `.start` / `.btn-save` / `.btn-share`: a filled pill under a 1.5px ink border, on the
 * [ZinelyV21Press.Hero] tier — 4dp at rest, `translate(2px,2px)` and 1dp pressed. The tier is the one
 * the corpus table assigns those three classes by name; it is not read off the depth.
 *
 * [metrics] is required — each surface passes its own frozen preset.
 *
 * @param shadow **ignored under V2.1** — it overrode the V2 blurred bloom, and there is no bloom. The
 *   parameter is retained so concurrently-edited call sites keep compiling.
 */
@Composable
public fun ZPrimaryButton(
    text: String,
    onClick: () -> Unit,
    metrics: ZPrimaryButtonMetrics,
    modifier: Modifier = Modifier,
    fill: ZPrimaryFill = ZPrimaryFill.Coral,
    @Suppress("UNUSED_PARAMETER") shadow: List<ZinelyShadowLayer>? = null,
    enabled: Boolean = true,
    icon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val background = when (fill) {
        ZPrimaryFill.Coral -> colors.leaf
        ZPrimaryFill.Stamp -> colors.paper
    }
    val contentColor = when (fill) {
        ZPrimaryFill.Coral -> colors.onLeaf
        ZPrimaryFill.Stamp -> colors.inkSoft
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(metrics.radius)
    Row(
        modifier = modifier
            // Ring and shadow both paint outside the node, so both precede the `clip`.
            .zinelyFocusRing(interactionSource, metrics.radius)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Hero, colors.inkLine, shape)
            .clip(shape)
            .background(background)
            .border(BorderWidth, colors.ink, shape)
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
        if (icon != null) Box(Modifier.size(metrics.iconSize)) { icon(contentColor) }
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = metrics.fontSize,
                fontWeight = FontWeight.Bold,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            ),
        )
    }
}

// ----- stamp (secondary emphasis) ------------------------------------------------------------

/**
 * `v21-proof.html .btn-share`: `--paper` under the same ink border, `--ink-soft` label, and the same
 * [ZinelyV21Press.Hero] depth its sibling `.btn-save` carries — the corpus puts two Hero buttons on
 * that one row, which is why the tier table stopped calling Hero *"the one primary action"*.
 *
 * V2 drew this flat, with `--shadow-2` and no press rule at all. V2.1 gives it both.
 */
@Composable
public fun ZStampButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Hero, colors.inkLine, shape)
            .clip(shape)
            .background(colors.paper)
            .border(BorderWidth, colors.ink, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = StampMinHeight)
            .padding(horizontal = StampPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = StampFontSize,
                fontWeight = FontWeight.SemiBold,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            ),
        )
    }
}

// ----- icon button --------------------------------------------------------------------------

/**
 * `v21-proof.html .iconbtn` / `v21-bench.html .icon-btn` (byte-identical): a 44×44 paper pill under a
 * 1.5px ink border, a 20px `--ink-soft` glyph, and `2px 2px 0 var(--ink-line)`. The 44px visual is
 * wrapped in the frozen ≥48dp touch target.
 *
 * **Press tier [ZinelyV21Press.Flat]** — both class names appear in the tier table's Flat row, and
 * both `:active` rules write `box-shadow:0 0 0`: it presses **flush**.
 *
 * **Disabled is not a press.** `.icon-btn:disabled{opacity:.35;box-shadow:none}` — the shadow is
 * dropped outright rather than shortened, and the dim reaches the border and the fill, not only the
 * glyph. V2 dimmed the glyph alone at `.5` because it had no border to dim.
 */
@Composable
public fun ZIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (tint: Color) -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    // 44×44 visual inside an explicit 48dp touch box — the frozen ≥48dp target. (An outer
    // minimumInteractiveComponentSize() is overridden by the inner size(44) under test; the
    // explicit wrapper is unambiguous.)
    Box(
        modifier = modifier
            .size(IconButtonTouchTarget)
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
                .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
                // `:disabled{box-shadow:none}` — absent, not shortened. Chaining nothing is how a
                // caller states the two behaviours [zinelyV21Pressable] deliberately cannot express.
                .then(
                    if (enabled) {
                        Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
                    } else {
                        Modifier
                    },
                )
                .alpha(if (enabled) 1f else IconDisabledAlpha)
                .size(IconButtonSize)
                .clip(shape)
                .background(colors.paper)
                .border(BorderWidth, colors.ink, shape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(IconGlyphSize)) { icon(colors.inkSoft) }
        }
    }
}

// ----- tool (bordered secondary) -------------------------------------------------------------

/** Frozen scalars of the bordered secondary family. Radius is `--br-pill` in every V2.1 variant. */
@Immutable
public data class ZToolButtonMetrics(
    val minHeight: Dp,
    val hPadding: Dp,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
    val gap: Dp,
    val iconSize: Dp,
    /** `.btn-share`-style variants speak in `--ink-soft`; every other variant in `--ink`. */
    val softText: Boolean = false,
    /**
     * **Vestigial under V2.1.** It gated the one V2 variant that had a `:active{translateY(1px)}`
     * rule. Every V2.1 button presses, so the tier is chosen by shape (icon-only → Flat, text →
     * Raised) and this flag no longer selects anything. Kept as public API for the same reason
     * [ZPrimaryButtonMetrics.restShadowAlpha] is.
     */
    val pressTranslate: Boolean = false,
) {
    public companion object {
        /** shelf sort control */
        public val ShelfSort: ZToolButtonMetrics =
            ZToolButtonMetrics(48.dp, 14.dp, 13.5.sp, FontWeight.Medium, 6.dp, 15.dp, softText = true)

        /** bench tool (and the icon-only variant: pass no text) */
        public val BenchTool: ZToolButtonMetrics =
            ZToolButtonMetrics(48.dp, 15.dp, 14.sp, FontWeight.Medium, 8.dp, 18.dp)

        /** proof ghost button */
        public val ProofGhost: ZToolButtonMetrics =
            ZToolButtonMetrics(52.dp, 16.dp, 14.5.sp, FontWeight.SemiBold, 8.dp, 18.dp)

        /** proof export-row tool */
        public val ProofExport: ZToolButtonMetrics =
            ZToolButtonMetrics(52.dp, 14.dp, 14.5.sp, FontWeight.SemiBold, 8.dp, 19.dp, pressTranslate = true)

        /**
         * `v21-proof.html .fnav` (44×44 square, 18dp glyph: pass no text).
         *
         * Was 52×52 with a 22dp glyph, from the retired `.stepnav button`. The V2.1 fold guide's own
         * `.fnav` is 44px, and at eight steps the difference stopped being cosmetic: a 52dp pair either
         * side of eight dot targets overflowed a 360dp window, and `ZSheet` clips. Matching the freeze
         * fixed the overflow — see ADR-101 §6.7.
         */
        public val ProofStepNav: ZToolButtonMetrics =
            ZToolButtonMetrics(44.dp, 0.dp, 14.5.sp, FontWeight.SemiBold, 0.dp, 18.dp)
    }
}

/**
 * The bordered secondary family: `--paper` fill, 1.5px `--ink` border, `--br-pill`.
 *
 * **Icon-only (null [text]) is transcribed; the text variants are extrapolated, and the two are not
 * the same kind of claim.** Icon-only *is* `v21-proof.html .fnav` — **[ZinelyV21Press.Flat]**, named
 * in the tier table by that class, with `:disabled{opacity:.3;box-shadow:none}` read off the rule.
 *
 * The four text-bearing presets have **no V2.1 selector at all**: `.sortbtn`, `.tool`, `.ghostbtn` and
 * `.exportrow .tool` were V2 classes and none survives the re-freeze, which is why their companion
 * entries below no longer cite one. They are given **[ZinelyV21Press.Raised]** because `.retry` is the
 * corpus's paper-on-ink-border secondary *text* button and wears that tier — an extrapolation from the
 * nearest surviving class, stated as one. It is not interpolated between tiers, which is the thing
 * [ZinelyV21Press] forbids; a call site that knows better should be re-pointed at a real selector when
 * its own package converts.
 *
 * [danger] = `.act.danger`'s `--jam-text` label and icon. `--jam` is the corpus's only urgent colour,
 * and as *text* it is always `--jam-text` (the one documented exception is the Library's 28.8px
 * display `!`, which is not this).
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
    val colors = ZinelyTheme.v21Colors
    val contentColor = when {
        danger -> colors.jamText
        metrics.softText -> colors.inkSoft
        else -> colors.ink
    }
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconOnly = text == null
    val press = if (iconOnly) ZinelyV21Press.Flat else ZinelyV21Press.Raised
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
            // `.fnav:disabled{box-shadow:none}` — the depth is absent while disabled, not shortened.
            .then(
                if (enabled) {
                    Modifier.zinelyV21Pressable(pressed, press, colors.inkLine, shape)
                } else {
                    Modifier
                },
            )
            // `opacity:.3` on the whole control, so the border dims with the label.
            .alpha(if (enabled) 1f else ToolDisabledAlpha)
            .clip(shape)
            .background(colors.paper)
            .border(BorderWidth, colors.ink, shape)
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
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = metrics.fontSize,
                    fontWeight = metrics.fontWeight,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
        }
    }
}

private fun Modifier.describedAs(description: String): Modifier =
    semantics { contentDescription = description }

// ---------------------------------------------------------------------------------------------
// The frozen values shared by the family.
// ---------------------------------------------------------------------------------------------

/** `border:1.5px solid var(--ink)` — every button in all three prototypes. */
private val BorderWidth = 1.5.dp

/** `.icon-btn{width:44px;height:44px}` and its 20px glyph, drawn inside the 48dp touch minimum. */
private val IconButtonTouchTarget = 48.dp
private val IconButtonSize = 44.dp
private val IconGlyphSize = 20.dp

/** `.icon-btn:disabled{opacity:.35}` and `.fnav:disabled{opacity:.3}`. */
private const val IconDisabledAlpha = 0.35f
private const val ToolDisabledAlpha = 0.3f

/** `.btn-share{padding:0 var(--gap-lg);font-size:.88rem}` — over the frozen 52dp minimum. */
private val StampMinHeight = 52.dp
private val StampPadding = ZinelyV21Dimens.gapLg
private val StampFontSize = 14.08.sp
