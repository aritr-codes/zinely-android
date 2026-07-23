package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One CSS `box-shadow` layer: `0 <dy> <blur> <color>`. The frozen spec never offsets a shadow on x,
 * and never uses spread, so neither is modelled.
 */
@Immutable
public data class ZinelyShadowLayer(val dy: Dp, val blur: Dp, val color: Color)

/**
 * The frozen depth ladder — `--shadow-1` / `--shadow-2` / `--shadow-lift` from the DESIGN-FROZEN
 * trilogy. "Flat 2.0": a sheet lifts off the desk, nothing floats.
 *
 * These are **data, not a Modifier**. Compose has no multi-layer coloured shadow, so the modifier
 * that draws them lands with the first component that needs it (M1). Shipping the draw code here,
 * with no caller, would be a speculative abstraction.
 */
@Immutable
public data class ZinelyElevation(
    /** `--shadow-1` — a resting sheet. */
    val shadow1: List<ZinelyShadowLayer>,
    /** `--shadow-2` — a raised card. */
    val shadow2: List<ZinelyShadowLayer>,
    /** `--shadow-lift` — the picked-up sheet (drag, open sheet). */
    val shadowLift: List<ZinelyShadowLayer>,
)

private val LightShadow = Color(0xFF23201C)

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
