package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.imposition.ConventionSpec
import com.aritr.zinely.core.imposition.SingleSheet8
import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.Rotation
import com.aritr.zinely.ui.components.ZPaperSurface
import com.aritr.zinely.ui.components.zinelyShadow
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme

// Test tags for the Act 1 body the ProofScreen suite and golden address.
public const val ProofSheetPreviewTestTag: String = "proof-sheet-preview"
public const val ProofFrontCoverTestTag: String = "proof-front-cover"
public const val ProofBackCoverTestTag: String = "proof-back-cover"

/** One decorative panel: the booklet page number in a grid cell, and whether it prints upside-down. */
internal data class DecorativePanel(val pageNumber: Int, val flipped: Boolean)

/**
 * The imposed sheet's rows, derived straight from the canonical engine convention so the picture can
 * never drift from the real imposition (a hardcoded copy did once: 5·4·3·6 / 8·1·2·7). For
 * [SingleSheet8.TOP_ROW_ROTATED] this yields top `5 4 3 2` (flipped) / bottom `6 7 8 1` (upright) —
 * the folded-in M4 engine-truth checkpoint ([ADR-050]), guarded by [DecorativeImpositionOrderTest].
 *
 * Relocated from `ExportScreen.kt` into the Proof (M5 B2, [ADR-051]) — the imposed sheet is now Act 1's
 * artifact. **The single imposition source of truth stays the engine (ADR-007); no raw layout array.**
 */
internal fun decorativeImpositionRows(
    spec: ConventionSpec = SingleSheet8.TOP_ROW_ROTATED,
): List<List<DecorativePanel>> {
    val pageAt = spec.cellOf.entries.associate { (page, cell) -> cell to page }
    val rowCount = spec.cellOf.values.maxOf { it.row } + 1
    val colCount = spec.cellOf.values.maxOf { it.col } + 1
    return List(rowCount) { row ->
        List(colCount) { col ->
            val page = pageAt.getValue(GridCell(row, col))
            DecorativePanel(page, spec.rotationOf.getValue(page) == Rotation.HALF)
        }
    }
}

/**
 * **Act 1 — The Sheet** (M5 B2, `proof.html` `#act0`, DESIGN-FROZEN). The imposed landscape sheet
 * exactly as it prints: the eight engine-ordered panels with the top row upside-down, the three vertical
 * + one horizontal fold creases, the **one** coral cut across the two centre columns with its "one cut"
 * label, the calm "printer can't reach here" dead-band, the honesty legend, and the front/back cover
 * confidence cards.
 *
 * The panel order and rotation come from the engine via [decorativeImpositionRows] — never a raw array.
 * The whole sheet is a single `role=img` node with one description; every decorative sub-part is cleared
 * from the a11y tree (the meaning is the label + the surrounding copy).
 *
 * ponytail: cells carry the engine-derived **page number**, a schematic stand-in — real per-panel
 * artwork needs the document tree threaded through the Proof VM seam (deferred with `zineName`, a later
 * batch). Act 1 is a static illustration here, so no entrance animation is added (the B1 act-slide is
 * the entrance); reduced motion is therefore trivially satisfied.
 */
@Composable
internal fun ProofSheetAct(modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // .lead — the reassurance: the scramble is on purpose.
        Column(
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BasicText(
                text = "This is your sheet",
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = "One page, printed on one side. It looks scrambled on purpose — " +
                    "the fold puts every page in order.",
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        ImposedSheet()
        HonestyLegend()
        CoverCards()
    }
}

/** The physical imposed sheet: paper ground, 8 engine-ordered cells, creases, the one cut, dead-band. */
@Composable
private fun ImposedSheet() {
    val colors = ZinelyTheme.colors
    val rows = remember { decorativeImpositionRows() }
    // Dark overrides only the dead-band border (spec: rgba(0,0,0,.14)); everything else is theme-neutral.
    val isDark = colors.desk.luminance() < 0.5f
    val deadband = if (isDark) Color.Black.copy(alpha = 0.14f) else colors.inkFaint.copy(alpha = 0.20f)
    val sheetShape = RoundedCornerShape(3.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .testTag(ProofSheetPreviewTestTag)
            // box-shadow: var(--shadow-lift), 0 2px 0 var(--paper-edge)
            .zinelyShadow(ZinelyTheme.elevation.shadowLift + ZinelyShadowLayer(2.dp, 0.dp, colors.paperEdge), sheetShape)
            .clip(sheetShape)
            // ponytail: flat paper. The frozen top-left corner vignette (radial rgba(0,0,0,.03),
            // proof.html:184) is dropped — a .03-alpha flourish; add a radial brush if parity asks.
            .background(colors.paper)
            .aspectRatio(297f / 210f)
            // role="img" with ONE label; every decorative child below is thereby cleared from a11y.
            .clearAndSetSemantics {
                role = Role.Image
                contentDescription = "Your zine imposed on one landscape sheet: eight panels, " +
                    "the top row upside-down, with one cut line across the centre."
            },
        contentAlignment = Alignment.Center,
    ) {
        // z2 — the 8 imposed cells (top row flipped 180°), engine-ordered.
        Column(Modifier.fillMaxSize()) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { panel -> SheetCell(panel, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
        // z3 — fold creases + the one coral cut.
        CreasesAndCut()
        // z5 — the "one cut" chip, just above the centre line.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = -16.dp.toPx() }
                .zinelyShadow(ZinelyTheme.elevation.shadow1, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(colors.paper)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            BasicText(
                text = "ONE CUT",
                style = TextStyle(
                    color = colors.coralStrong,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.06.em,
                ),
            )
        }
        // z4 — the "printer can't reach" dead-band: a calm 9px translucent frame over the outer edge.
        // ponytail: solid frame only. The frozen 45° hatch fill + 1px inner hairline (proof.html:191-195)
        // are dropped — ≤.14-alpha texture the translucent frame already carries; add a hatch brush if
        // pixel-parity review wants the weave back.
        Box(Modifier.fillMaxSize().border(9.dp, deadband))
    }
}

/** One imposed cell: the engine page number as a schematic stand-in, flipped with the top row. */
@Composable
private fun SheetCell(panel: DecorativePanel, modifier: Modifier) {
    val colors = ZinelyTheme.colors
    Box(
        modifier = modifier.graphicsLayer { rotationZ = if (panel.flipped) 180f else 0f },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = panel.pageNumber.toString(),
            style = TextStyle(
                color = colors.inkFaint,
                fontFamily = ZinelyTheme.typography.voice,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** Three vertical (25/50/75%) + one horizontal (50%) dashed valley creases, and the one solid cut. */
@Composable
private fun CreasesAndCut() {
    val colors = ZinelyTheme.colors
    val crease = colors.inkFaint.copy(alpha = 0.5f)
    val cut = colors.coralStrong
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val inset = h * 0.03f
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        // 3 vertical creases, top 3% → bottom 3%
        listOf(0.25f, 0.5f, 0.75f).forEach { fx ->
            drawLine(crease, Offset(w * fx, inset), Offset(w * fx, h - inset), 1.dp.toPx(), pathEffect = dash)
        }
        // 1 horizontal crease at the midline, left 3% → right 3%
        val insetX = w * 0.03f
        drawLine(crease, Offset(insetX, h / 2f), Offset(w - insetX, h / 2f), 1.dp.toPx(), pathEffect = dash)
        // the ONE cut — solid coral across the two centre columns (25% → 75%)
        drawLine(cut, Offset(w * 0.25f, h / 2f), Offset(w * 0.75f, h / 2f), 2.dp.toPx(), cap = StrokeCap.Butt)
    }
}

/** The honesty legend (`aria-hidden`): fold lines · the one cut · printer can't reach here. */
@Composable
private fun HonestyLegend() {
    val colors = ZinelyTheme.colors
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem("fold lines") {
            Canvas(Modifier.size(16.dp, 8.dp)) {
                drawLine(
                    colors.inkFaint.copy(alpha = 0.5f),
                    Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
            }
        }
        LegendItem("the one cut") {
            Canvas(Modifier.size(16.dp, 8.dp)) {
                drawLine(colors.coralStrong, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2.dp.toPx())
            }
        }
        LegendItem("printer can’t reach here") {
            // ponytail: solid swatch — the frozen 45° hatch fill (proof.html:235) is flattened to the
            // same translucent fill as the sheet dead-band; a 16×10 texture no one reads. Matches above.
            Box(
                Modifier
                    .size(16.dp, 10.dp)
                    .border(2.dp, colors.inkFaint.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    .background(colors.inkFaint.copy(alpha = 0.20f), RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, key: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        key()
        BasicText(
            text = label,
            style = TextStyle(
                color = ZinelyTheme.colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 11.5.sp,
            ),
        )
    }
}

/** Front/back confidence cards — small upright mini-pages of the cover panels (page 1 and page 8). */
@Composable
private fun CoverCards() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        CoverCard("Front cover", pageNumber = 1, testTag = ProofFrontCoverTestTag)
        CoverCard("Back cover", pageNumber = 8, testTag = ProofBackCoverTestTag)
    }
}

@Composable
private fun CoverCard(caption: String, pageNumber: Int, testTag: String) {
    val colors = ZinelyTheme.colors
    Column(
        modifier = Modifier.widthIn(max = 150.dp).width(120.dp).testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // proof mini-page paper tuple: 4dp @ .10 (ZPaperSurface doc); portrait 210/297.
        ZPaperSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(210f / 297f)
                .clearAndSetSemantics { },
            shadow = ZinelyTheme.elevation.shadow2,
            boundEdgeWidth = 4.dp,
            boundEdgeAlpha = 0.10f,
        ) {
            BasicText(
                text = pageNumber.toString(),
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    color = colors.inkFaint,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        BasicText(
            text = caption,
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
