package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The frozen riso palette — a one-to-one transcription of the `:root` custom properties in the
 * DESIGN-FROZEN HTML trilogy (`docs/design/v1/{shelf,bench,proof}.html`, frozen 2026-07-08). All
 * three files declare byte-identical token blocks; `shelf.html` is the transcription source.
 *
 * Invariant: **the HTML is the specification.** A token value may only change here after it has
 * changed in the HTML spec first (CLAUDE.md, "HTML-first UI workflow"). `ZinelyColorsTest` pins
 * every value so drift fails the build.
 *
 * Dynamic color is off by design (fixed print-brand identity) — see [ZinelyTheme].
 *
 * Note the split between *paper* and *desk*: `ink*` is what sits on a sheet of paper, `onDesk*` is
 * what sits on the surface behind it. They coincide in light (`--on-desk:var(--ink)`) and diverge in
 * dark (the paper stays lit and warm, so ink on it stays dark). Do not collapse them.
 */
@Immutable
public data class ZinelyColors(
    /** `--paper` — the sheet. */
    val paper: Color,
    /** `--paper-2` — a second, slightly deeper sheet (stacked cards, inner faces). */
    val paper2: Color,
    /** `--paper-edge` — the cut edge of a sheet. */
    val paperEdge: Color,
    /** `--ink` — primary text **on paper**. */
    val ink: Color,
    /** `--ink-soft` — secondary text on paper. */
    val inkSoft: Color,
    /** `--ink-faint` — tertiary/decorative on paper. Never load-bearing text. */
    val inkFaint: Color,
    /** `--coral` — the accent. Decorative / large / fills only, never small text. */
    val coral: Color,
    /** `--coral-strong` — coral FILL under white text (AA 4.6:1). The primary button, and the focus ring. */
    val coralStrong: Color,
    /** `--coral-text` — coral as TEXT on light shell surfaces (paper/field/menu), AA ≥5:1. */
    val coralText: Color,
    /** `--teal` — authorial ink. Sub-AA as text (2.9:1); a documented artist choice, not a default. */
    val teal: Color,
    /** `--yellow` — authorial ink. */
    val yellow: Color,
    /** `--stamp` — authorial ink. */
    val stamp: Color,
    /** `--desk` — the surface the sheets sit on. */
    val desk: Color,
    /** `--desk-edge` — the desk's far edge. */
    val deskEdge: Color,
    /** `--shelf-line` — the hairline a sheet rests on. */
    val shelfLine: Color,
    /** `--on-desk` — primary text **on the desk**. */
    val onDesk: Color,
    /** `--on-desk-soft` — secondary text on the desk (≥5:1). */
    val onDeskSoft: Color,
    /** `--on-desk-faint` — tertiary/decorative on the desk. Never load-bearing text. */
    val onDeskFaint: Color,
    /** `--scrim` — dims everything behind an open sheet. */
    val scrim: Color,
    /** `--field` — text-input fill. */
    val field: Color,
    /** `--field-edge` — text-input border. */
    val fieldEdge: Color,
    /** `--menu` — popup-menu fill. */
    val menu: Color,
)

// The literals below are the frozen `:root` bytes. rgba() tokens are expressed as base × alpha so
// the fraction stays exact rather than rounding through an 8-bit hex channel.

/** `:root` — the light token block (shelf.html:36-63). */
public fun zinelyLightColors(): ZinelyColors = ZinelyColors(
    paper = Color(0xFFF4EFE6),
    paper2 = Color(0xFFEFE8DA),
    paperEdge = Color(0xFFE1D8C7),
    ink = Color(0xFF23201C),
    inkSoft = Color(0xFF6B6358),
    inkFaint = Color(0xFF9C9385),
    coral = Color(0xFFE76F51),
    coralStrong = Color(0xFFC64E34),
    coralText = Color(0xFFA63C22),
    teal = Color(0xFF2A9D8F),
    yellow = Color(0xFFE9C46A),
    stamp = Color(0xFF264653),
    desk = Color(0xFFE7DECE),
    deskEdge = Color(0xFFDBD1BF),
    shelfLine = Color(0xFF23201C).copy(alpha = 0.10f),
    onDesk = Color(0xFF23201C), // --on-desk:var(--ink)
    onDeskSoft = Color(0xFF5E574C),
    onDeskFaint = Color(0xFF726A5C),
    scrim = Color(0xFF23201C).copy(alpha = 0.42f),
    field = Color(0xFFFBF8F1),
    fieldEdge = Color(0xFFDED4C2),
    menu = Color(0xFFFBF8F1),
)

/**
 * `:root[data-theme="dark"]` — the dark token block (shelf.html:65-79).
 *
 * Only the tokens the spec actually overrides differ. `ink*`, `coral`, `coral-strong`, `teal`,
 * `yellow` and `stamp` deliberately inherit their light values: the sheet stays lit, so ink on it
 * stays dark. Do not "fix" that.
 */
public fun zinelyDarkColors(): ZinelyColors = zinelyLightColors().copy(
    paper = Color(0xFFEDE6D9),
    paper2 = Color(0xFFE4DBCB),
    paperEdge = Color(0xFFC7BCA6),
    desk = Color(0xFF201F1E),
    deskEdge = Color(0xFF161514),
    shelfLine = Color.White.copy(alpha = 0.06f),
    onDesk = Color(0xFFEFE9DD),
    onDeskSoft = Color(0xFFC3BBAC),
    onDeskFaint = Color(0xFF8C8577),
    scrim = Color.Black.copy(alpha = 0.58f),
    field = Color(0xFF2B2A28),
    fieldEdge = Color(0xFF413E39),
    menu = Color(0xFF2B2A28),
    coralText = Color(0xFFE76F51), // coral as text on the dark menu — AA 5.4:1
)
