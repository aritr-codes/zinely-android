package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme

// Test tags for the print-details panel the ProofScreen suite addresses. The export row's tags moved to
// `ProofScreen.kt` with the controls (ADR-101 P2); `ProofChangePaperTestTag`/`ProofPaperSheetTestTag`
// went with the chooser sheet P3 deleted.
public const val ProofPaperSegmentsTestTag: String = "proof-paper-segments"
public const val ProofTestSheetTestTag: String = "proof-test-sheet"
public const val ProofAlreadyDoneTestTag: String = "proof-already-done"
public const val ProofFoldLinkTestTag: String = "proof-fold-link"

/** The one-sheet zine every shipping format seeds ([ADR-028](../../../../../../docs/DECISIONS.md#adr-028)). */
private const val DEFAULT_BOOKLET_PAGES = 8

/**
 * Which delivery the host gives a finished Proof export. The host maps [SEND] → the OS share chooser
 * (`ACTION_SEND`) and [SAVE] → a durable copy in shared Downloads (ADR-054). Kept feature-local so the
 * screen never imports the app's export types (ADR-039 delivery-agnostic seam); the host maps this onto its
 * app-level `ExportDestination`.
 */
public enum class ProofExportTarget { SEND, SAVE }

/**
 * **The print-details drawer, as one `.dbody` panel** (`proof.html` `#drawer-details`, DESIGN-FROZEN,
 * freeze-amended per [ADR-052]) — ADR-101 P3.
 *
 * Five sections, one scroll: the paper question answered inline by a segmented control; what the app has
 * already handled; ADR-052's recipe; the test-sheet card; and what the imposition is, illustrated by the
 * sheet itself.
 *
 * ### What P3 changed, and why each one is not cosmetic
 *
 * **It is one panel now, not two stacked acts.** P1 re-homed `ProofSheetAct` and `ProofPrintAct` into
 * the drawer unchanged, which put two independently scrolling regions at `weight(1f)` each inside one
 * bounded column: half a drawer apiece, each with its own lead heading, neither showing its whole
 * content. That arrangement is also what forced the drawer's traversal test to give up on geometry —
 * content scrolled out of either half still reports bounds, so "Front cover" measured *above* the sheet
 * it sits under. One scroll restores the assertion.
 *
 * **The paper chooser sheet is deleted.** It was a `Dialog` raised over the `Dialog` already holding this
 * panel, to answer a two-option question whose two options fit on one row of the panel that asked it.
 *
 * **ADR-052's recipe is kept, and the frozen panel contains none of it.** Searching `v21-proof.html` for
 * these phrases returns one hit, and it is a code comment about sheet geometry. They stay because a home
 * printer will silently ruin a zine; the band's `.done` carries the three-word version for the walk to
 * the printer, and this is where the reason lives. Per [ADR-052] the frozen third export action **Print**
 * remains dropped: the app has no OS `PrintManager` path, and the system print dialog has no actual-size
 * control — it would silently reintroduce the fit-to-page shrink this list exists to prevent.
 *
 * **The frozen `ALL SET` checklist is *not* built, and that is a ruling rather than an omission.** Its
 * three green ticks assert *"Everything sits safely inside the edges"*, *"Your photos are sharp enough
 * to print — checked at print size, not screen size"* and *"One cut, down the middle"*. Only the third is
 * true by construction. **Nothing in this repository checks image resolution against print size** — there
 * is no DPI computation anywhere in `core`, `feature` or `app` — so that tick would be a green mark
 * beside a check that never ran, on the one screen whose job is to be trusted about printing. The first
 * is no better: the imposer holds a safe-area inset, but content is clipped to the *panel*, not to the
 * safe rect, so a photo dragged to the page edge lands in the strip the tick says nothing lands in. A real
 * print-resolution check is a **feature**, and it is worth building; it is not a label.
 *
 * **But deleting the false claims deleted the section's job too, and the design review caught that.** The
 * first draft of P3 left the panel as five blocks of instructions to the user — *"it reads like
 * homework"* — and pointed at the honesty legend as the replacement, which does not survive contact: the
 * legend was `clearAndSetSemantics`-hidden and sits at the bottom of a scroll roughly twice its viewport.
 * A replacement that is aria-hidden and below the fold is not one. The third frozen tick (*one cut, down
 * the middle*) was also true by construction and went out as collateral. So [Copy.ProofPrint.SECT_ALREADY]
 * restores the reassurance without the tick grammar — facts about the artifact, in plain lines — and the
 * legend is no longer hidden from TalkBack.
 *
 * State is hoisted: [paper] + [onPaperSelected] (the host threads the chosen size into the export, so
 * `export == what you see`).
 */
@Composable
internal fun ProofPrintDetailsPanel(
    paper: PaperSize,
    onPaperSelected: (PaperSize) -> Unit,
    onOpenFold: () -> Unit,
    modifier: Modifier = Modifier,
    // The document's real page count, for the imposition explainer. Defaulted to the one-sheet zine every
    // shipping format seeds, so the goldens and the paint suite mount unchanged — but threaded from the
    // host, because a hardcoded eight is what made this panel contradict the band and the ticket.
    pageCount: Int = DEFAULT_BOOKLET_PAGES,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
    ) {
        // ── 1. The paper question, answered inline ────────────────────────────────────────────
        SectionLabel(Copy.ProofPrint.SECT_PAPER, first = true)
        PaperSegments(paper = paper, onPaperSelected = onPaperSelected)

        // ── 2. What the app already handled — the honest half of the frozen ALL SET block ─────
        SectionLabel(Copy.ProofPrint.SECT_ALREADY)
        AlreadyDone()

        // ── 3. ADR-052's recipe, which the frozen panel does not contain ──────────────────────
        SectionLabel(Copy.ProofPrint.SECT_DIALOG)
        BodyLine(Copy.ProofPrint.DIALOG_HINT, Modifier.padding(bottom = 12.dp))
        Column(
            // The 460dp cap the retired act carried. Below it on a phone; on a tablet it keeps the recipe
            // rows from stretching to the sheet's wider 520dp cap, which the P3 review caught them doing.
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecipeRow(
                warn = true,
                label = Copy.ProofPrint.SCALE_LABEL,
                value = emphasised(Copy.ProofPrint.SCALE_VALUE, Copy.ProofPrint.SCALE_EMPHASIS),
                icon = { tint -> StrokedGlyph(ICON_SCALE, tint) },
            )
            RecipeRow(
                warn = true,
                label = Copy.ProofPrint.ORIENTATION_LABEL,
                value = emphasised(Copy.ProofPrint.LANDSCAPE, Copy.ProofPrint.ORIENTATION_EMPHASIS),
                icon = { tint -> StrokedGlyph(ICON_LANDSCAPE, tint) },
            )
            // The Paper row keeps its place in the recipe — this is the list you read *at the print
            // dialog*, and the size is one of the four things to check there — but it no longer carries
            // a "Change" button: the question is asked and answered at the top of this same panel.
            RecipeRow(
                warn = false,
                label = Copy.ProofPrint.PAPER_LABEL,
                value = plain(paper.displayName),
                icon = { tint -> StrokedGlyph(ICON_PAPER, tint) },
            )
            RecipeRow(
                warn = false,
                label = Copy.ProofPrint.SIDES_LABEL,
                value = plain(Copy.ProofPrint.SIDES_VALUE),
                icon = { tint -> StrokedGlyph(ICON_SIDES, tint) },
            )
            SingleSidedNote()
        }

        // ── 4. The test sheet ─────────────────────────────────────────────────────────────────
        SectionLabel(Copy.ProofPrint.SECT_TEST)
        TestSheetCard()

        // ── 5. What the sheet is, with the sheet as its illustration ──────────────────────────
        SectionLabel(Copy.ProofPrint.SECT_BOOKLET)
        // A zero-page document cannot reach a shipping path, but it can reach a test host that mounts this
        // panel without a document — and *"we lay your 0 pages onto one sheet"* is worse than the default.
        BodyLine(Copy.ProofPrint.bookletHint(pageCount.takeIf { it > 0 } ?: DEFAULT_BOOKLET_PAGES), Modifier.padding(bottom = 12.dp))
        ProofImposedSheetBlock()
        FoldGuideLink(onOpenFold, Modifier.padding(top = 14.dp))
    }
}

/**
 * The `WHAT WE'VE ALREADY DONE` lines: neutral dots, deliberately **not** ticks.
 *
 * The visual grammar of a passed check is what made the frozen `ALL SET` block a lie — a green tick beside
 * *"your photos are sharp enough to print"* claims a check ran on the user's content, and none does. These
 * are statements about the artifact this app produced, so they get the styling of statements. Three text
 * nodes, no merge: each is a separate fact and reads better one at a time.
 */
@Composable
private fun AlreadyDone() {
    val colors = ZinelyTheme.v21Colors
    Column(
        modifier = Modifier.fillMaxWidth().testTag(ProofAlreadyDoneTestTag),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        listOf(
            Copy.ProofPrint.PAPER_HINT,
            Copy.ProofPrint.ALREADY_CUT,
            Copy.ProofPrint.ALREADY_MARGIN,
        ).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // A 4dp disc on the first line's optical centre — a bullet, not a verdict.
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.inkFaint)
                        .clearAndSetSemantics { },
                )
                BodyLine(line)
            }
        }
    }
}

/** The panel's closing pointer into the fold drawer — see [Copy.ProofPrint.SEE_HOW_TO_FOLD]. */
@Composable
private fun FoldGuideLink(onOpenFold: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { haptics.perform(ZinelyHaptic.Tick); onOpenFold() }
            .padding(horizontal = 4.dp)
            .testTag(ProofFoldLinkTestTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = Copy.ProofPrint.SEE_HOW_TO_FOLD,
            style = TextStyle(
                color = colors.leafText,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** A `.sect` rule: the uppercase tracked label that opens each block of the panel. */
@Composable
private fun SectionLabel(text: String, first: Boolean = false, modifier: Modifier = Modifier) {
    BasicText(
        text = text.uppercase(),
        modifier = modifier.padding(top = if (first) 2.dp else 22.dp, bottom = 8.dp),
        style = TextStyle(
            color = ZinelyTheme.v21Colors.inkSoft,
            fontFamily = ZinelyTheme.typography.shell,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.13.em,
        ),
    )
}

/** A `.paperhint` / `.cardh` line: soft body copy under a section label. */
@Composable
private fun BodyLine(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = ZinelyTheme.v21Colors.inkSoft,
            fontFamily = ZinelyTheme.typography.shell,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
        ),
    )
}

/**
 * The frozen `.paperseg` — two buttons, one pressed.
 *
 * This replaced a "Change" button that raised a chooser **sheet**, i.e. a `Dialog` over the `Dialog`
 * already holding this panel, to answer a two-option question whose two options fit on one row of the
 * panel that asked it. Announced as a radio group, because that is what it is: `Role.RadioButton` with
 * `selected`, rather than two unrelated buttons.
 *
 * **What the platform actually says is *"A4, Selected"* — not *"A4, selected, 1 of 2"*, which is what an
 * earlier draft of ADR-101 §6.6 claimed on no evidence.** Measured in `ProofPaperSegmentsA11yTest`: the
 * selection reaches a service as `stateDescription`, but the role collapses to `android.view.View` (a
 * known Compose behaviour for a control wrapping child content, already recorded by
 * `ZButtonPlatformA11yTest`), so nothing can count the segment within its group. The state is the part
 * that matters and it is there; the position is not, and both facts are now assertions rather than prose.
 */
@Composable
private fun PaperSegments(paper: PaperSize, onPaperSelected: (PaperSize) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .testTag(ProofPaperSegmentsTestTag),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Explicit order, not `PaperSize.entries` — the enum declares LETTER first, so iterating it put
        // US Letter in the left segment against a frozen design that reads A4 then US Letter. The panel
        // owns its own reading order; an enum's declaration order is not a design decision, and the
        // retired chooser sheet had been quietly inheriting the same one.
        listOf(PaperSize.A4, PaperSize.LETTER).forEach { option ->
            val selected = option == paper
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.leaf else colors.paper)
                    .border(
                        width = 1.5.dp,
                        // Selected takes the fill's own edge; unselected takes the hairline. `ink` on an
                        // unselected chip would make four equally loud chips out of a one-of-four choice.
                        color = if (selected) colors.leaf else colors.hair,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { haptics.perform(ZinelyHaptic.Tick); onPaperSelected(option) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = option.displayName,
                    style = TextStyle(
                        // `--on-leaf`, not white: the palette measures the label on a leaf fill, and a
                        // hardcoded white is the exact defect the frozen file's `.seal` comment records.
                        color = if (selected) colors.onLeaf else colors.ink,
                        fontFamily = ZinelyTheme.typography.shell,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/**
 * The frozen `.testcard` — *"New printer? Print one test sheet first…"*.
 *
 * The single strongest wasted-sheet guard in the freeze, and it existed nowhere in Compose until P3;
 * the review that read this surface as a first-time user is what found that. It is one node to a screen
 * reader: the bold lead and the body are one sentence, and the bookmark glyph is decoration.
 */
@Composable
private fun TestSheetCard() {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.paper)
            .border(1.5.dp, colors.hair, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag(ProofTestSheetTestTag)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(19.dp).clearAndSetSemantics {}) {
            StrokedGlyph(ICON_BOOKMARK, colors.inkSoft, strokeWidth = 1.8f)
        }
        BasicText(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold)) {
                    append(Copy.ProofPrint.TEST_SHEET_LEAD)
                }
                append(Copy.ProofPrint.TEST_SHEET_BODY)
            },
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
            ),
        )
    }
}

/** One `.rrow`: field card, icon chip (teal, or coral-text when [warn]), label + value, optional trailing. */
@Composable
private fun RecipeRow(
    warn: Boolean,
    label: String,
    value: androidx.compose.ui.text.AnnotatedString,
    icon: @Composable (tint: Color) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = ZinelyTheme.v21Colors
    // `leafText`, not `leaf`: this tints a small glyph beside a label, and the palette measures the text
    // form on paper. `jamText` for the warning, for the same reason.
    val chipTint = if (warn) colors.jamText else colors.leafText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.paper)
            .border(1.dp, colors.hair, RoundedCornerShape(14.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(chipTint.copy(alpha = 0.14f))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(20.dp)) { icon(chipTint) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(
                text = label,
                style = TextStyle(
                    // `inkSoft`, not `inkFaint`: the palette forbids `inkFaint` from setting text at all
                    // (3.04:1 on paper, against a 4.5 floor), and a 12sp row label is the smallest text on
                    // the panel. The V1 token this replaced, `onDeskFaint`, had no such rule, so the
                    // mechanical rename was the defect.
                    color = colors.inkSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 12.sp,
                ),
            )
            BasicText(
                text = value,
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        if (trailing != null) trailing()
    }
}


/** `.singlenote` — the calm double-sided reassurance. */
@Composable
private fun SingleSidedNote() {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(17.dp)) { InfoGlyph(colors.inkFaint) }
        BasicText(
            text = buildAnnotatedString {
                append(Copy.ProofPrint.SIDES_HELP_PREFIX)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(Copy.ProofPrint.SIDES_HELP_BOLD) }
                append(Copy.ProofPrint.SIDES_HELP_SUFFIX)
            },
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
            ),
        )
    }
}

// ---- value builders -------------------------------------------------------------------------

/** A recipe value whose lead phrase is the `<em>` emphasis (font-style normal, per spec) — V2.1 `jamText`. */
@Composable
private fun emphasised(emphasis: String, rest: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = ZinelyTheme.v21Colors.jamText)) { append(emphasis) }
    append(rest)
}

@Composable
private fun plain(text: String) = buildAnnotatedString { append(text) }

/** Shared with the band, which names the paper in `.ready`'s summary. */
internal val PaperSize.displayName: String
    get() = when (this) {
        PaperSize.A4 -> Copy.Paper.A4
        PaperSize.LETTER -> Copy.Paper.LETTER
    }

// ---- glyphs (frozen 24×24 SVGs; decorative, aria-hidden via the chip's clear) ----------------

private const val ICON_SCALE = "M4 8h16M4 8l3-3h10l3 3M4 8v11h16V8M9 13h6"
private const val ICON_LANDSCAPE = "M3 6h18v12H3z"
private const val ICON_PAPER = "M5 3h14v18H5z"
private const val ICON_SIDES = "M4 7h16v10H4zM8 7v10"
private const val ICON_BOOKMARK = "M6 3h12v18l-6-3-6 3z"

// Save PDF's glyph lives here with the rest of the frozen set; the band draws it. `ICON_FOLDER` sat beside
// it until the P3 review found it dead — it was the deleted share chooser's "Save to Files" row, and the
// comment that used to say "the commit row's two glyphs" outlived the second one by a whole package.
internal const val ICON_SAVE: String = "M12 4v10m0 0l-4-4m4 4l4-4M5 18h14"

@Composable
internal fun StrokedGlyph(pathData: String, tint: Color, strokeWidth: Float = 2f) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(Modifier.fillMaxSize()) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(path, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** The share glyph — three nodes + two links (circles aren't path strings, so drawn directly). */
@Composable
internal fun ShareGlyph(tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val u = size.minDimension / 24f
        val w = 2f * u
        drawCircle(tint, 2.4f * u, Offset(6f * u, 12f * u), style = Stroke(w))
        drawCircle(tint, 2.4f * u, Offset(18f * u, 6f * u), style = Stroke(w))
        drawCircle(tint, 2.4f * u, Offset(18f * u, 18f * u), style = Stroke(w))
        drawLine(tint, Offset(8.1f * u, 11f * u), Offset(15.9f * u, 7f * u), w)
        drawLine(tint, Offset(8.1f * u, 13f * u), Offset(15.9f * u, 17f * u), w)
    }
}

/** The info glyph — ringed "i" (circle + stem + dot), for the single-sided note. */
@Composable
private fun InfoGlyph(tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val u = size.minDimension / 24f
        drawCircle(tint, 9f * u, Offset(12f * u, 12f * u), style = Stroke(2f * u))
        drawLine(tint, Offset(12f * u, 11f * u), Offset(12f * u, 16f * u), 2f * u, cap = StrokeCap.Round)
        drawCircle(tint, 1.1f * u, Offset(12f * u, 8f * u))
    }
}
