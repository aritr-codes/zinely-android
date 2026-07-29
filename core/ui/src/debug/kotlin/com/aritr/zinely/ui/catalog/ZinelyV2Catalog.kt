package com.aritr.zinely.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Fonts
import com.aritr.zinely.ui.theme.ZinelyV2Icon
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Grain
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.rememberZinelyV2GrainBrush
import com.aritr.zinely.ui.theme.rememberZinelyV2Icon
import com.aritr.zinely.ui.theme.zinelyV2Grain

/**
 * The **V2 foundation catalog** — Phase A's acceptance surface, and deliberately not a product screen.
 *
 * ## Why this lives in `src/debug`
 *
 * Phase A's gate is *zero product surface*, and this is the one thing Phase A renders. `src/debug` is the
 * honest home for that: the catalog compiles into debug builds and is visible to `testDebugUnitTest` and
 * `recordRoborazziDebug`, and it is **absent from a release AAR entirely** — so it cannot become a product
 * screen by accident, and it costs the shipped app nothing. Nothing navigates to it; it has no route.
 *
 * ## What it is for
 *
 * Not a showcase. It exists so the foundation can be **verified as reproduced rather than as resembling**.
 * Every section is built to be *measured*, which is why the swatches are flat un-bordered fills, the
 * specimens are single words on a known ground, and every item carries a stable test tag: a catalog laid
 * out for looking at would be a catalog no test could read a value off.
 *
 * That is the split this package rests on:
 *
 * - **Parity** is proved against the **corpus** — `ZinelyV2CatalogParityTest` parses the colour table out
 *   of [V2-TOKENS.md](docs/design/V2-TOKENS.md) at run time and asserts the rendered pixel equals the hex
 *   the document states. The oracle is the design, not this implementation.
 * - **Regression** is proved by the Roborazzi goldens, which lock whatever parity established.
 *
 * A golden alone could only ever prove that today's Compose output equals yesterday's. Record a golden of
 * a wrong catalog and the gate stays green forever. So the goldens are the second proof here, never the
 * first — the same two-proof shape the V1 component goldens use, with the behavioural half sharpened from
 * *"some ink was drawn"* to *"this exact value from the frozen corpus reached the screen"*.
 */
@Composable
public fun ZinelyV2Catalog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .testTag(ZinelyV2CatalogTags.Root)
            .background(ZinelyTheme.v2Colors.desk)
            .padding(CatalogPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        ZinelyV2CatalogPalette()
        ZinelyV2CatalogType()
        ZinelyV2CatalogIcons()
        ZinelyV2CatalogMaterial()
    }
}

/**
 * The chrome palette, one flat swatch per role.
 *
 * **Flat, un-bordered, and 48dp square on purpose.** The parity test reads the centre pixel and compares
 * it to the hex in V2-TOKENS.md, so anything that antialiases into the middle — a rounded corner, a
 * hairline, a shadow, a grain overlay — would make an exactly-equal comparison impossible and force the
 * test down to a tolerance. A tolerance is precisely the difference between *reproduced* and *looks
 * similar*, so the swatch is shaped to keep the comparison exact.
 *
 * The roles are named exactly as V2-TOKENS.md names them, because the test matches on those names: a role
 * renamed here without being renamed in the corpus fails as a missing swatch rather than passing quietly.
 */
@Composable
public fun ZinelyV2CatalogPalette(modifier: Modifier = Modifier) {
    val c = ZinelyTheme.v2Colors
    val roles: List<Pair<String, Color>> = listOf(
        "paper" to c.paper,
        "paperEdge" to c.paperEdge,
        "desk" to c.desk,
        "deskEdge" to c.deskEdge,
        "ink" to c.ink,
        "inkSoft" to c.inkSoft,
        "inkFaint" to c.inkFaint,
        "matcha" to c.matcha,
        "matchaText" to c.matchaText,
        "matchaTint" to c.matchaTint,
        "strawberry" to c.strawberry,
        "strawberryText" to c.strawberryText,
        "strawberryTint" to c.strawberryTint,
        "consequence" to c.consequence,
    )
    Column(
        modifier = modifier.testTag(ZinelyV2CatalogTags.Palette),
        verticalArrangement = Arrangement.spacedBy(SwatchGap),
    ) {
        roles.chunked(SwatchesPerRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SwatchGap)) {
                row.forEach { (role, color) ->
                    Column(modifier = Modifier.width(SwatchSize)) {
                        Box(
                            Modifier
                                .testTag(ZinelyV2CatalogTags.swatch(role))
                                .size(SwatchSize)
                                .background(color),
                        )
                        BasicText(role, style = labelStyle(c.ink))
                    }
                }
            }
        }
        // `onMatcha` is the one ★ pairing V2-TOKENS.md states as a relationship rather than as a row, so
        // it is shown as the relationship: the label on its fill, which is the only place it is ever used.
        //
        // Which makes it the one swatch here whose OWN colour the parity test cannot read — the box is
        // filled with `matcha`, and `onMatcha` appears only as 9sp glyphs. That is not a gap: `onMatcha`
        // is pinned literally in both themes by `ZinelyV2ColorsTest`, and its contrast against `matcha`
        // is one of the six ★ pairings `ZinelyV2ContrastTest` gates. This is a demonstration of a
        // relationship already proven elsewhere, not a parity claim.
        Box(
            Modifier
                .testTag(ZinelyV2CatalogTags.swatch("onMatcha"))
                .size(SwatchSize)
                .background(c.matcha),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            BasicText("Aa", style = labelStyle(c.onMatcha))
        }
    }
}

/**
 * Type specimens — the same word in each family and weight the foundation carries.
 *
 * The specimens are **identical strings on identical grounds**, differing only in the one property under
 * test, because the parity assertion is a *difference* assertion: a bundled family that silently fell back
 * to the platform default, or a weight that never reached the renderer, both produce output that looks
 * plausible and is wrong. Two specimens that must differ, and do not, is the only cheap way to catch that
 * headlessly — you cannot assert "this is Fraunces" from a bitmap, but you can assert "this is not the
 * same shape as Inter".
 */
@Composable
public fun ZinelyV2CatalogType(modifier: Modifier = Modifier) {
    val c = ZinelyTheme.v2Colors
    val t = ZinelyTheme.v2Typography
    Column(
        modifier = modifier.testTag(ZinelyV2CatalogTags.Type),
        verticalArrangement = Arrangement.spacedBy(SwatchGap),
    ) {
        Specimen("voice-400", c, TextStyle(fontFamily = ZinelyV2Fonts.Voice, fontWeight = FontWeight.Normal, fontSize = SpecimenSize))
        Specimen("voice-500", c, TextStyle(fontFamily = ZinelyV2Fonts.Voice, fontWeight = FontWeight.Medium, fontSize = SpecimenSize))
        Specimen("voice-600", c, TextStyle(fontFamily = ZinelyV2Fonts.Voice, fontWeight = FontWeight.SemiBold, fontSize = SpecimenSize))
        Specimen("work-400", c, TextStyle(fontFamily = ZinelyV2Fonts.Work, fontWeight = FontWeight.Normal, fontSize = SpecimenSize))
        Specimen("work-600", c, TextStyle(fontFamily = ZinelyV2Fonts.Work, fontWeight = FontWeight.SemiBold, fontSize = SpecimenSize))
        // The two styles the foundation actually declares, rendered as declared.
        Specimen("base", c, t.base)
        Specimen("sectionLabel", c, t.sectionLabel)
    }
}

@Composable
private fun Specimen(name: String, c: com.aritr.zinely.ui.theme.ZinelyV2Colors, style: TextStyle) {
    Box(
        Modifier
            .testTag(ZinelyV2CatalogTags.specimen(name))
            .width(SpecimenWidth)
            .background(c.paper),
    ) {
        BasicText(SpecimenWord, style = style.copy(color = c.ink))
    }
}

/**
 * All 36 marks of [ZinelyV2Icons.All], each at the paint the frozen design gives it.
 *
 * A mark whose paint the corpus does not fix — 26 of the 36, because [ADR-077](docs/DECISIONS.md#adr-077)
 * makes weight a property of the *container* — is drawn at the catalog's own stated weight rather than a
 * pretended canonical one. That is a catalog decision, not a design one: the point here is that every mark
 * *draws*, and that no two marks draw the same, which is checkable without knowing a call site's weight.
 */
@Composable
public fun ZinelyV2CatalogIcons(modifier: Modifier = Modifier) {
    val c = ZinelyTheme.v2Colors
    Column(
        modifier = modifier.testTag(ZinelyV2CatalogTags.Icons),
        verticalArrangement = Arrangement.spacedBy(SwatchGap),
    ) {
        ZinelyV2Icons.All.chunked(IconsPerRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SwatchGap)) {
                row.forEach { icon -> CatalogIcon(icon, c.ink, c.paper) }
            }
        }
    }
}

@Composable
private fun CatalogIcon(icon: ZinelyV2Icon, tint: Color, ground: Color) {
    val vector: ImageVector = rememberZinelyV2Icon(
        icon = icon,
        paint = icon.frozenPaint ?: CatalogIconPaint,
    )
    Box(
        Modifier
            .testTag(ZinelyV2CatalogTags.icon(icon.name))
            .size(IconCell)
            .background(ground),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Icon(imageVector = vector, contentDescription = null, tint = tint)
    }
}

/**
 * The paper material: the same sheet with and without the [zinelyV2Grain] overlay, side by side.
 *
 * Two panels rather than one, because grain is only meaningful as a *difference*. A single grained panel
 * proves nothing — it is a slightly-off cream rectangle either way — whereas a pair lets the test assert
 * that the overlay actually changed the surface, and lets a reader see the size of the change the frozen
 * design asked for. On API 24–28 the two panels are identical by design ([D-014](docs/design/V2-SPEC-DEFECTS.md):
 * `BlendMode.Softlight` does not exist there and the honest fallback is flat paper), which is itself worth
 * being able to see.
 */
@Composable
public fun ZinelyV2CatalogMaterial(modifier: Modifier = Modifier) {
    val c = ZinelyTheme.v2Colors
    val brush = rememberZinelyV2GrainBrush()
    Row(
        modifier = modifier.testTag(ZinelyV2CatalogTags.Material),
        horizontalArrangement = Arrangement.spacedBy(SwatchGap),
    ) {
        Box(
            Modifier
                .testTag(ZinelyV2CatalogTags.PaperPlain)
                .size(PaperPanel)
                .background(c.paper),
        )
        Box(
            Modifier
                .testTag(ZinelyV2CatalogTags.PaperGrained)
                .size(PaperPanel)
                .background(c.paper)
                .zinelyV2Grain(brush, CatalogGrainAlpha),
        )
    }
}

@Composable
private fun labelStyle(ink: Color) = TextStyle(
    fontFamily = ZinelyV2Fonts.Work,
    fontWeight = FontWeight.Normal,
    fontSize = 9.sp,
    color = ink,
)

/** Stable tags for every measurable item in the catalog. Tests address the catalog only through these. */
public object ZinelyV2CatalogTags {
    public const val Root: String = "v2-catalog"
    public const val Palette: String = "v2-catalog-palette"
    public const val Type: String = "v2-catalog-type"
    public const val Icons: String = "v2-catalog-icons"
    public const val Material: String = "v2-catalog-material"
    public const val PaperPlain: String = "v2-paper-plain"
    public const val PaperGrained: String = "v2-paper-grained"

    /** The swatch for a chrome role, named exactly as [V2-TOKENS.md](docs/design/V2-TOKENS.md) names it. */
    public fun swatch(role: String): String = "v2-swatch-$role"

    /** A type specimen, named `<family>-<weight>` or after the declared style. */
    public fun specimen(name: String): String = "v2-specimen-$name"

    /** A mark, named by [ZinelyV2Icon.name]. */
    public fun icon(name: String): String = "v2-icon-$name"
}

// --- catalog layout constants -------------------------------------------------------------------
// These size the catalog, and are NOT design tokens: nothing in the frozen corpus specifies how a
// swatch grid should look, because the corpus does not contain a swatch grid. They are named and
// grouped here so a reader can tell at a glance that no V2 value is being invented below.

private val CatalogPadding = 16.dp
private val SectionGap = 20.dp
private val SwatchGap = 8.dp
private val SwatchSize = 48.dp
private const val SwatchesPerRow = 5
private val SpecimenWidth = 220.dp
private val SpecimenSize = 24.sp
private const val SpecimenWord = "Zinely"
private val IconCell = 32.dp

/**
 * Exactly one grain tile. Two tiles per panel would show the pattern's repeat and would be the better
 * demonstration, but two 280dp panels do not fit the catalog's width — so the panel is one tile and says
 * so, rather than claiming a repeat it does not show. Referenced, not re-typed: a second `140.dp` literal
 * beside [ZinelyV2Grain.SourceTileSize] is the duplicate-token problem in miniature.
 */
private val PaperPanel = ZinelyV2Grain.SourceTileSize
private const val IconsPerRow = 9

/**
 * The weight the catalog draws an unfixed mark at. A display choice, never a design claim ([ADR-077]
 * makes stroke weight a property of the *container*) — but set to the value every frozen paint in
 * [ZinelyV2Icons] actually uses, so a reader who copies it out of here copies something real. A
 * near-miss would be worse than an obvious placeholder.
 */
private val CatalogIconPaint = ZinelyV2IconPaint.Stroke(width = 1.6f)

/**
 * The Library `.cover` grain, drawn exactly as [ZinelyV2Grain]'s frozen table states it: the 140dp source
 * tile at effective alpha **1.00**.
 *
 * This used to be 0.20 — "mid-band of the Bench/Proof range" — which was an invented number describing no
 * frozen surface at all, since the default brush is the Library's 140px tile and the Library asks for 1.00.
 * The pairing was doubly wrong: it made the panel a surface the design does not specify, and at 0.20 on
 * light paper the overlay moved the sheet by a single LSB, so the "see the size of the change the frozen
 * design asked for" claim was false. One real surface, drawn at its real strength.
 */
private const val CatalogGrainAlpha = 1.00f
