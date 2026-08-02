package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One CSS `box-shadow` layer: `0 <dy> <blur> [spread] <color>`. The frozen spec never offsets a shadow
 * on x, so that is still not modelled.
 *
 * **[spread] arrived with the V2 Bench and this doc used to deny it existed.** The M0 audit's "never
 * uses spread" held for the V1 trilogy and was carried forward as a fact; the frozen `.ctx` contextual
 * bar (`v2-bench.html:211`, `0 12px 30px -12px`) is the first layer in the corpus that needs it
 * ([ADR-092](../../../../../../../../docs/DECISIONS.md#adr-092) row 2.10a). It defaults to zero, so every
 * existing layer is unchanged.
 */
@Immutable
public data class ZinelyShadowLayer(
    val dy: Dp,
    val blur: Dp,
    val color: Color,
    /** CSS spread: the shadow shape grows by this on all sides before blurring; negative shrinks it. */
    val spread: Dp = 0.dp,
)

/**
 * The frozen depth ladder — `--shadow-1` / `--shadow-2` / `--shadow-lift` from the DESIGN-FROZEN
 * trilogy. "Flat 2.0" (§2.4): **depth is drawn by a cast shadow only.** There is **no tonal
 * elevation** in this system — a raised object does not lighten its own surface the way Material 3
 * does; it casts a shadow onto the desk and nothing more. Each tier below names the §2.4 role it
 * plays; the *colour* of the shadow is theme-dependent (warm ink in light, pure black in dark) but
 * the *ladder* (which tier means what) is fixed.
 *
 * **Open gap (audit row 2, stays OPEN):** §2.4 assigns a role to each *existing* tier, but the design
 * system does not yet answer "which tier does a *new* object belong to?" — there is no rule mapping an
 * object's kind to shadow1/shadow2/shadowLift. Recorded here so the gap is visible at the code; its
 * resolution is not part of CI-38 (documentation-only, no value change).
 *
 * These are **data, not a Modifier**. Compose has no multi-layer coloured shadow, so the modifier
 * that draws them lands with the first component that needs it (M1). Shipping the draw code here,
 * with no caller, would be a speculative abstraction.
 */
@Immutable
public data class ZinelyElevation(
    /** `--shadow-1` — §2.4 role: **a resting sheet**, flat on the desk. The default, barely-lifted tier. */
    val shadow1: List<ZinelyShadowLayer>,
    /** `--shadow-2` — §2.4 role: **a raised card** — a surface a step above the desk (a stacked card). */
    val shadow2: List<ZinelyShadowLayer>,
    /** `--shadow-lift` — §2.4 role: **the picked-up sheet** — actively lifted (a drag, an open sheet). */
    val shadowLift: List<ZinelyShadowLayer>,
)

// The shadow tint in light theme is the `ink` token (theme-invariant literal 0xFF23201C); referenced
// here rather than re-spelt so the palette owns the value (CI-94, both-theme-identical rename).
private val LightShadow = zinelyLightColors().ink

/** Light `:root` shadows (shelf.html:44-46). */
public fun zinelyLightElevation(): ZinelyElevation = ZinelyElevation(
    shadow1 = listOf(
        ZinelyShadowLayer(1.dp, 2.dp, LightShadow.copy(alpha = 0.10f)),
    ),
    shadow2 = listOf(
        ZinelyShadowLayer(10.dp, 22.dp, LightShadow.copy(alpha = 0.16f)),
        ZinelyShadowLayer(2.dp, 5.dp, LightShadow.copy(alpha = 0.10f)),
    ),
    shadowLift = listOf(
        ZinelyShadowLayer(18.dp, 34.dp, LightShadow.copy(alpha = 0.22f)),
        ZinelyShadowLayer(3.dp, 8.dp, LightShadow.copy(alpha = 0.14f)),
    ),
)

/** Dark `:root[data-theme="dark"]` shadows (shelf.html:70-72) — deeper, and pure black. */
public fun zinelyDarkElevation(): ZinelyElevation = ZinelyElevation(
    shadow1 = listOf(
        ZinelyShadowLayer(1.dp, 2.dp, Color.Black.copy(alpha = 0.50f)),
    ),
    shadow2 = listOf(
        ZinelyShadowLayer(12.dp, 26.dp, Color.Black.copy(alpha = 0.55f)),
        ZinelyShadowLayer(2.dp, 6.dp, Color.Black.copy(alpha = 0.40f)),
    ),
    shadowLift = listOf(
        ZinelyShadowLayer(20.dp, 40.dp, Color.Black.copy(alpha = 0.62f)),
        ZinelyShadowLayer(4.dp, 10.dp, Color.Black.copy(alpha = 0.50f)),
    ),
)
