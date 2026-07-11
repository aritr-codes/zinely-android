package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.components.ZMenuItem
import com.aritr.zinely.ui.components.ZSelectedStyle
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.ZToolButton
import com.aritr.zinely.ui.components.ZToolButtonMetrics
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme

// Test tags for the Act 2 body the ProofScreen suite addresses.
public const val ProofSavePdfTestTag: String = "proof-save-pdf"
public const val ProofShareTestTag: String = "proof-share"
public const val ProofChangePaperTestTag: String = "proof-change-paper"
public const val ProofPaperSheetTestTag: String = "proof-paper-sheet"
public const val ProofShareSheetTestTag: String = "proof-share-sheet"

/**
 * Which OS edge the host hands a finished Proof export to. The host maps [OPEN] → `ACTION_VIEW` (open the
 * PDF in the user's viewer) and [SEND] → `ACTION_SEND` (the OS share chooser). Kept feature-local so the
 * screen never imports the app's export types (ADR-039 delivery-agnostic seam).
 */
public enum class ProofExportTarget { OPEN, SEND }

/**
 * **Act 2 — Print** (M5 B3, `proof.html` `#act1`, DESIGN-FROZEN, freeze-amended per [ADR-052]). The
 * honest print recipe — the four settings that pre-empt the silent home-print bugs (fit-to-page shrink,
 * portrait default, A4/Letter mismatch, double-sided) — the single-sided note, and the export row.
 *
 * Per [ADR-052] the frozen third export action **Print** is dropped: the app has no OS `PrintManager`
 * path, and the system print dialog has no actual-size control (it would silently reintroduce the
 * fit-to-page shrink the recipe exists to prevent). The export row ships the two honest backends —
 * **Save PDF** ([ProofExportTarget.OPEN]) and **Share** (the paper/share sheets → [ProofExportTarget.SEND]).
 *
 * Stateless beyond the two sheet toggles: [paper] + [onPaperSelected] are hoisted (the host threads the
 * chosen size into the export so `export == what you see`); [onExportPdf] fires the shipped
 * `ExportViewModel.export` via the host; [exportBusy] disables the export row while a render is in flight.
 *
 * ponytail: no loading sweep / save-confirmation snackbar / "Fold now" hand-off / toasts / test-sheet
 * line here — those are the B5 overlay/notification family (the frozen `loadwrap`/`snackbar`/`toast`),
 * and the ADR-041 post-export → Fold hand-off waits for the Fold act (B4). B3 is the recipe + the two
 * honest export edges; the OS view/share sheet is the export feedback.
 */
@Composable
internal fun ProofPrintAct(
    paper: PaperSize,
    onPaperSelected: (PaperSize) -> Unit,
    onExportPdf: (ProofExportTarget) -> Unit,
    exportBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.colors
    val haptics = ZinelyTheme.haptics
    var showPaper by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // .lead
        Column(
            modifier = Modifier.padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BasicText(
                text = "Print it just like this",
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            BasicText(
                text = "These four settings keep your zine the right size and in the right order. " +
                    "Most printers already default to them.",
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                ),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecipeRow(
                warn = true,
                label = "Scale",
                value = emphasised("100% · Actual size", " — not “Fit to page”"),
                icon = { tint -> StrokedGlyph(ICON_SCALE, tint) },
            )
            RecipeRow(
                warn = true,
                label = "Orientation",
                value = emphasised("Landscape", " — a portrait default breaks the fold"),
                icon = { tint -> StrokedGlyph(ICON_LANDSCAPE, tint) },
            )
            RecipeRow(
                warn = false,
                label = "Paper",
                value = plain(paper.displayName),
                icon = { tint -> StrokedGlyph(ICON_PAPER, tint) },
                trailing = {
                    ChangeButton(
                        onClick = { haptics.perform(ZinelyHaptic.Tick); showPaper = true },
                    )
                },
            )
            RecipeRow(
                warn = false,
                label = "Sides",
                value = plain("Single-sided — one side only"),
                icon = { tint -> StrokedGlyph(ICON_SIDES, tint) },
            )

            SingleSidedNote()

            // export row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ZToolButton(
                    onClick = { haptics.perform(ZinelyHaptic.Tick); onExportPdf(ProofExportTarget.OPEN) },
                    metrics = ZToolButtonMetrics.ProofExport,
                    text = "Save PDF",
                    enabled = !exportBusy,
                    modifier = Modifier.weight(1f).testTag(ProofSavePdfTestTag),
                    icon = { tint -> StrokedGlyph(ICON_SAVE, tint) },
                )
                ZToolButton(
                    onClick = { haptics.perform(ZinelyHaptic.Tick); showShare = true },
                    metrics = ZToolButtonMetrics.ProofExport,
                    text = "Share",
                    enabled = !exportBusy,
                    modifier = Modifier.weight(1f).testTag(ProofShareTestTag),
                    icon = { tint -> ShareGlyph(tint) },
                )
            }
        }
    }

    // Paper-size chooser (frozen #paperSheet).
    ZSheet(
        visible = showPaper,
        onDismiss = { showPaper = false },
        title = "Paper size",
        sub = "Match this to the paper in your printer, so nothing gets clipped or shrunk.",
        modifier = Modifier.testTag(ProofPaperSheetTestTag),
    ) {
        PaperSize.entries.forEach { option ->
            ZMenuItem(
                label = option.displayName,
                subLabel = option.paperSub,
                selected = option == paper,
                selectedStyle = ZSelectedStyle.Coral,
                onClick = {
                    haptics.perform(ZinelyHaptic.Tick)
                    onPaperSelected(option)
                    showPaper = false
                },
            )
        }
    }

    // Share chooser (frozen #shareSheet). Both options route to the OS share chooser (ACTION_SEND) —
    // on Android that one chooser is where the user picks Files *or* an app; the frozen files/apps split
    // is a single OS surface here. Privacy: the PDF stays local; the user chooses where it goes.
    ZSheet(
        visible = showShare,
        onDismiss = { showShare = false },
        title = "Share your zine",
        sub = "The PDF stays on your device — you choose where it goes. Nothing is uploaded by Zinely.",
        modifier = Modifier.testTag(ProofShareSheetTestTag),
    ) {
        ZMenuItem(
            label = "Save to Files",
            icon = { tint -> StrokedGlyph(ICON_FOLDER, tint) },
            onClick = { haptics.perform(ZinelyHaptic.Tick); showShare = false; onExportPdf(ProofExportTarget.SEND) },
        )
        ZMenuItem(
            label = "Send to an app",
            icon = { tint -> ShareGlyph(tint) },
            onClick = { haptics.perform(ZinelyHaptic.Tick); showShare = false; onExportPdf(ProofExportTarget.SEND) },
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
    val colors = ZinelyTheme.colors
    val chipTint = if (warn) colors.coralText else colors.teal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.field)
            .border(1.dp, colors.fieldEdge, RoundedCornerShape(14.dp))
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
                    color = colors.onDeskFaint,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 12.sp,
                ),
            )
            BasicText(
                text = value,
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        if (trailing != null) trailing()
    }
}

/** `.chg` — the coral-text "Change" affordance opening the paper chooser. */
@Composable
private fun ChangeButton(onClick: () -> Unit) {
    val colors = ZinelyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 6.dp)
            .testTag(ProofChangePaperTestTag),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "Change",
            style = TextStyle(
                color = colors.coralText,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** `.singlenote` — the calm double-sided reassurance. */
@Composable
private fun SingleSidedNote() {
    val colors = ZinelyTheme.colors
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(17.dp)) { InfoGlyph(colors.onDeskFaint) }
        BasicText(
            text = buildAnnotatedString {
                append("If your printer asks about double-sided, choose ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("single-sided") }
                append(" (or “off”). A mini-zine prints on one side, then folds.")
            },
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
            ),
        )
    }
}

// ---- value builders -------------------------------------------------------------------------

/** A recipe value whose lead phrase is the `<em>` coral-text emphasis (font-style normal, per spec). */
@Composable
private fun emphasised(emphasis: String, rest: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = ZinelyTheme.colors.coralText)) { append(emphasis) }
    append(rest)
}

@Composable
private fun plain(text: String) = buildAnnotatedString { append(text) }

private val PaperSize.displayName: String
    get() = when (this) {
        PaperSize.A4 -> "A4"
        PaperSize.LETTER -> "Letter"
    }

private val PaperSize.paperSub: String
    get() = when (this) {
        PaperSize.A4 -> "210 × 297 mm — most of the world"
        PaperSize.LETTER -> "8.5 × 11 in — US & Canada"
    }

// ---- glyphs (frozen 24×24 SVGs; decorative, aria-hidden via the chip's clear) ----------------

private const val ICON_SCALE = "M4 8h16M4 8l3-3h10l3 3M4 8v11h16V8M9 13h6"
private const val ICON_LANDSCAPE = "M3 6h18v12H3z"
private const val ICON_PAPER = "M5 3h14v18H5z"
private const val ICON_SIDES = "M4 7h16v10H4zM8 7v10"
private const val ICON_SAVE = "M12 4v10m0 0l-4-4m4 4l4-4M5 18h14"
private const val ICON_FOLDER = "M4 7a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"

@Composable
private fun StrokedGlyph(pathData: String, tint: Color, strokeWidth: Float = 2f) {
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
private fun ShareGlyph(tint: Color) {
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
