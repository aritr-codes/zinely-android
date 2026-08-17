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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.aritr.zinely.core.copy.Copy
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
 * **The imposed sheet, its legend and its two cover cards** (`proof.html` `.minisheet`, DESIGN-FROZEN):
 * the landscape sheet exactly as it prints — the eight engine-ordered panels with the top row
 * upside-down, the three vertical + one horizontal fold creases, the **one** coral cut across the two
 * centre columns with its "one cut" label, the calm "printer can't reach here" dead-band — then the
 * honesty legend and the front/back cover confidence cards.
 *
 * The panel order and rotation come from the engine via [decorativeImpositionRows] — never a raw array.
 * The whole sheet is a single `role=img` node with one description; every decorative sub-part is cleared
 * from the a11y tree (the meaning is the label + the surrounding copy).
 *
 * **It stopped being an act in ADR-101 P1 and stopped being a screen-sized block in P3.** It has no lead
 * of its own now: the *"This is your sheet / it looks scrambled on purpose"* pair was two headings deep
 * inside a drawer already titled "Print details", and the panel's own `HOW IT BECOMES A BOOKLET` section
 * says the same thing in the place the frozen design says it. So this is an illustration the panel places,
 * not a region that owns its scroll — which is what lets the drawer be one scrolling panel instead of two
 * fighting ones.
 *
 * ponytail: cells carry the engine-derived **page number**, a schematic stand-in — real per-panel
 * artwork needs the document tree threaded through the Proof VM seam (deferred with `zineName`, a later
 * batch). A static illustration, so no entrance animation and reduced motion is trivially satisfied.
 */
@Composable
internal fun ProofImposedSheetBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ImposedSheet()
        // Read *after* the picture, which is the whole point of it — see [Copy.ProofSheet.SCRAMBLED_CAPTION].
        BasicText(
            text = Copy.ProofSheet.SCRAMBLED_CAPTION,
            style = TextStyle(
                color = ZinelyTheme.v21Colors.inkSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            ),
        )
        HonestyLegend()
        CoverCards()
    }
}

/**
 * The physical imposed sheet: paper ground, 8 engine-ordered cells, creases, the one cut, dead-band.
 *
 * **Lit in both themes** ([ProofLitPaper]), and that is a **departure from the freeze**, recorded as one.
 * `v21-proof.html` freezes `.minisheet{background:var(--paper)}`, which themes with the room. Device
 * verification of P6 found what that produces: this sheet dark, sitting directly above two cover cards that
 * were lit, so one drawer showed the same piece of paper two ways — and the darker of the two was the one
 * the user is about to feed into a printer. Taken under DESIGN FREEZE's theme-compatibility allowance, the
 * same ground [BookLeaf] states for the reader's leaf, and recorded in [ADR-101 §6.11](../../../../../../../docs/DECISIONS.md#adr-101-p6-device).
 *
 * The palette is provided to the whole subtree rather than to the ground alone: swapping only the ground is
 * how the creases and numerals would have gone invisible instead.
 *
 * **The dead-band is not in the frozen spec.** `.minisheet` freezes a ground, a 1.5px `ink` border, the hard
 * shadow, the 4×2 panel grid and the dashed `jam` cut line — and nothing about *"printer can't reach here"*.
 * The band and its `inkFaint @ .20` are inherited from the V1 lineage, kept because the fact is true and
 * worth stating; they are an invention this file owns, not a reading of a rule. Said plainly here because
 * the previous comment cited line numbers in a `proof.html` that does not exist.
 */
@Composable
private fun ImposedSheet() = ProofLitPaper {
    val colors = ZinelyTheme.v21Colors
    val rows = remember { decorativeImpositionRows() }
    val deadband = colors.inkFaint.copy(alpha = 0.20f)
    val sheetShape = RoundedCornerShape(3.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .testTag(ProofSheetPreviewTestTag)
            // The lift, plus the paper's own 2dp edge. `ZinelyTheme.elevation` is a **separate**
            // CompositionLocal, so [ProofLitPaper] does not reach it and the lift stays the room's — which
            // is correct: a lit sheet on a dark desk casts the desk's shadow, not paper's. Only the
            // `paperEdge` layer beside it follows the sheet, and it must.
            .zinelyShadow(ZinelyTheme.elevation.shadowLift + ZinelyShadowLayer(2.dp, 0.dp, colors.paperEdge), sheetShape)
            .clip(sheetShape)
            // ponytail: flat paper — no corner vignette. (The V1-lineage comment here cited a `proof.html`
            // that does not exist; the frozen `.minisheet` has no vignette rule.)
            .background(colors.paper)
            .aspectRatio(297f / 210f)
            // role="img" with ONE label; every decorative child below is thereby cleared from a11y.
            .clearAndSetSemantics {
                role = Role.Image
                contentDescription = Copy.ProofSheet.CONTENT_DESCRIPTION
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
                text = Copy.ProofSheet.ONE_CUT,
                style = TextStyle(
                    // `jamText`, not `jam`. The frozen `.cutlbl` says `--jam-text` and the token's own KDoc
                    // says `jam` cannot carry text: `#CF4A28` on `#FFF6E8` is 4.25:1, under the 4.5:1 floor
                    // for 9.5sp. The *line* below stays `jam` — a 2dp rule is a graphical object at 3:1.
                    color = colors.jamText,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.06.em,
                ),
            )
        }
        // z4 — the "printer can't reach" dead-band: a calm 9px translucent frame over the outer edge.
        // ponytail: solid frame only, no hatch weave. See this composable's KDoc — the band has no frozen
        // rule to be faithful *to*; it is an inherited invention kept because the fact it states is true.
        Box(Modifier.fillMaxSize().border(9.dp, deadband))
    }
}

/** One imposed cell: the engine page number as a schematic stand-in, flipped with the top row. */
@Composable
private fun SheetCell(panel: DecorativePanel, modifier: Modifier) {
    val colors = ZinelyTheme.v21Colors
    Box(
        modifier = modifier.graphicsLayer { rotationZ = if (panel.flipped) 180f else 0f },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = panel.pageNumber.toString(),
            style = TextStyle(
                // `inkFaint` sets no text — see the token's own KDoc. A page number on the imposed sheet
                // is the whole content of its cell.
                color = colors.inkSoft,
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
    val colors = ZinelyTheme.v21Colors
    val crease = colors.inkFaint.copy(alpha = 0.5f)
    val cut = colors.jam
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

/**
 * The honesty legend: fold lines · the one cut · printer can't reach here.
 *
 * The frozen markup marks it `aria-hidden`, and P3 copied that — then leaned on it as the replacement for
 * the `ALL SET` block it had just declined to build, which the design review pointed out is a replacement
 * no screen-reader user receives. The `aria-hidden` was defensible while this was decoration beside a
 * labelled image; it is not, once these three lines are carrying facts nothing else on the panel states.
 * Merged into one node, so it is read as one legend rather than three orphan phrases; the swatches stay
 * decorative.
 */
@Composable
private fun HonestyLegend() {
    // **The swatches take the sheet's palette, not the room's** — a key whose mark differs from the mark it
    // keys is a key that has stopped working, and once [ImposedSheet] went lit in both themes this row was
    // pointing at `#A08B74` creases with an `#8C7B65` swatch. The *labels* stay in the room's palette: they
    // are text on the drawer, not marks on paper, and the drawer is where they have to be legible.
    //
    // The dashes and the dead-band swatch drop their .5/.2 alphas with the move. On paper those alphas are
    // texture; over the drawer's dark ground they composited to roughly 2:1, under the 3:1 a graphical
    // object needs. A key identifies by hue and shape — so the hue is the sheet's and the shape is the
    // sheet's, and the paper-only translucency is not carried across.
    val colors = rememberLitSheetPalette()
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(Copy.ProofSheet.LEGEND_FOLD_LINES) {
            Canvas(Modifier.size(16.dp, 8.dp)) {
                drawLine(
                    colors.inkFaint,
                    Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
            }
        }
        LegendItem(Copy.ProofSheet.LEGEND_ONE_CUT) {
            Canvas(Modifier.size(16.dp, 8.dp)) {
                drawLine(colors.jam, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2.dp.toPx())
            }
        }
        LegendItem(Copy.ProofSheet.LEGEND_PRINTER_REACH) {
            // ponytail: solid swatch — a 45° hatch is a 16×10 texture no one reads. The frozen V2.1
            // `.minisheet` has no dead-band rule at all to depart from (see [ImposedSheet]); this mark and
            // the band it keys are both inherited from the V1 lineage, and they match each other.
            Box(
                Modifier
                    .size(16.dp, 10.dp)
                    .border(2.dp, colors.inkFaint, RoundedCornerShape(2.dp))
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
                color = ZinelyTheme.v21Colors.inkSoft,
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
        CoverCard(Copy.ProofSheet.FRONT_COVER, pageNumber = 1, testTag = ProofFrontCoverTestTag)
        CoverCard(Copy.ProofSheet.BACK_COVER, pageNumber = 8, testTag = ProofBackCoverTestTag)
    }
}

@Composable
private fun CoverCard(caption: String, pageNumber: Int, testTag: String) {
    val colors = ZinelyTheme.v21Colors
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
                    // As above. 20sp `Medium` is not bold, so it takes no large-text exemption either.
                    color = colors.inkSoft,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        BasicText(
            text = caption,
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
