package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.imposition.ConventionSpec
import com.aritr.zinely.core.imposition.SingleSheet8
import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.Rotation

/** Test tag on the whole Export surface. */
public const val ExportScreenTestTag: String = "export-screen"

/** Test tag on the primary "Print at home (PDF)" card. */
public const val ExportPdfCardTestTag: String = "export-card-pdf"

/** Test tag on the secondary "Save as image (PNG)" card. */
public const val ExportPngCardTestTag: String = "export-card-png"

/** Test tag on the "Actual size" print-guidance note. */
public const val ExportActualSizeNoteTestTag: String = "export-actual-size"

/** Which export target a card produces — a feature-local mirror of the host's export format. */
public enum class ExportKind { PDF, PNG }

/**
 * The S5 **Export · Print & fold** screen (docs/design/SCREEN-INVENTORY.md#export--print--fold,
 * mockups/export.html) — the last step before a real, home-printable artifact. Unlike [PreviewScreen]
 * (the reader's booklet), this screen is *about* the imposition sheet: it shows "all 8 pages, one sheet"
 * and hands the user a vector PDF or a 300 DPI PNG ([ADR-039]).
 *
 * Deliberately jargon-free (the mockup's rule — no "vector / raster / 300 DPI" anywhere): the imposition
 * is a friendly, **decorative** picture (hidden from the a11y tree, not a live render), and the one real
 * correctness aid is the warm **"Actual size"** note ([ADR-012] — "Fit to page" is the classic
 * prints-wrong failure). "How do I fold it?" is a seam to the later Completion screen.
 *
 * Stateless: the host ([`:app`] `ExportDestination`) owns the render/share and drives [working] (which
 * card is rendering) and any [errorMessage].
 *
 * @param onPrintPdf primary — render + share the PDF.
 * @param onSavePng secondary — render + share the PNG.
 * @param onFoldHelp the deferred fold-instructions seam (Completion screen).
 * @param onBack return to editing.
 * @param working the format currently rendering (its card shows progress; both cards disable), or null.
 * @param errorMessage a transient export-failure banner, or null.
 * @param onDismissError acknowledge the error banner.
 * @param modifier sizing/placement for the whole screen.
 */
@Composable
public fun ExportScreen(
    onPrintPdf: () -> Unit,
    onSavePng: () -> Unit,
    onFoldHelp: () -> Unit,
    onBack: () -> Unit,
    working: ExportKind? = null,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val busy = working != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .testTag(ExportScreenTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back to editing" },
            ) { Text("‹  Back to editing") }
        }

        Text(
            text = "Print & fold",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Text(
            text = "We'll lay out all 8 pages on one sheet. Print it, fold it, done.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.deskTextSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        )

        DecorativeImpositionSheet()

        if (errorMessage != null) {
            Surface(
                color = colors.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissError) { Text("Got it") }
                }
            }
        }

        FormatCard(
            icon = "🖨️",
            title = "Print at home (PDF)",
            subtitle = "Send the sheet straight to your printer.",
            primary = true,
            enabled = !busy,
            loading = working == ExportKind.PDF,
            testTag = ExportPdfCardTestTag,
            onClick = onPrintPdf,
        )
        FormatCard(
            icon = "🖼️",
            title = "Save as image (PNG)",
            subtitle = "Keep a picture to print or share later.",
            primary = false,
            enabled = !busy,
            loading = working == ExportKind.PNG,
            testTag = ExportPngCardTestTag,
            onClick = onSavePng,
        )

        // The one real correctness aid, dressed as a warm sticky-note (ADR-012), never a warning box.
        Surface(
            color = colors.tertiaryContainer,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag(ExportActualSizeNoteTestTag),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📌 One little thing",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onTertiaryContainer,
                )
                Text(
                    text = "Print at 100% / Actual size — don't let your printer \"fit to page\", or the folds won't line up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onTertiaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        TextButton(
            onClick = onFoldHelp,
            // Disabled mid-render so the user can't navigate away while an export is in flight and then
            // return to a surprise share of the finished file (Codex RF2).
            enabled = !busy,
            modifier = Modifier.padding(bottom = 16.dp),
        ) { Text("How do I fold it?") }
    }
}

/** One big, friendly format choice: icon + name + one-line description; the primary is coral. */
@Composable
private fun FormatCard(
    icon: String,
    title: String,
    subtitle: String,
    primary: Boolean,
    enabled: Boolean,
    loading: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (primary) colors.primary else colors.surface
    val content = if (primary) colors.onPrimary else colors.onSurface
    Surface(
        color = container,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.6f }
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(testTag)
            .semantics { contentDescription = title },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = content)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.85f),
                )
            }
            if (loading) {
                CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** One decorative panel: the booklet page number in a grid cell, and whether it prints upside-down. */
internal data class DecorativePanel(val pageNumber: Int, val flipped: Boolean)

/**
 * The decorative sheet's rows, derived straight from the canonical engine convention so the picture
 * can never drift from the real imposition again (a hardcoded copy did: 5·4·3·6 / 8·1·2·7). For
 * [SingleSheet8.TOP_ROW_ROTATED] this yields top `5 4 3 2` (flipped) / bottom `6 7 8 1` (upright).
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
 * A decorative "all 8 pages, one sheet" picture (mockup's imposition thumbnail) — a 4×2 grid with the
 * top row visually flipped, like real imposition. Purely illustrative, so it is cleared from the a11y
 * tree; the meaning is carried by the surrounding copy. Not a live render (that is the export itself),
 * but its order/rotation comes from the canonical convention via [decorativeImpositionRows].
 */
@Composable
private fun DecorativeImpositionSheet() {
    val colors = MaterialTheme.colorScheme
    val rows = remember { decorativeImpositionRows() }
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .width(220.dp)
            .graphicsLayer { rotationZ = -0.8f }
            .clearAndSetSemantics { },
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) Spacer(modifier = Modifier.size(6.dp))
                ImpositionRow(row)
            }
        }
    }
}

@Composable
private fun ImpositionRow(panels: List<DecorativePanel>) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        panels.forEach { panel ->
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .size(width = 46.dp, height = 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surfaceVariant)
                    .graphicsLayer { rotationZ = if (panel.flipped) 180f else 0f },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = panel.pageNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
