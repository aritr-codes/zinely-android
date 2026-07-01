package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import kotlin.math.min

/** Test tag on the whole Preview surface. */
public const val PreviewScreenTestTag: String = "preview-screen"

/** Test tag on the "page N of M" indicator label. */
public const val PreviewPageLabelTestTag: String = "preview-page-label"

/** Test tag on the booklet render area. */
public const val PreviewBookletTestTag: String = "preview-booklet"

/**
 * The S5 **Preview** screen (docs/design/SCREEN-INVENTORY.md#preview, mockups/preview.html) — the warm,
 * reader-facing booklet a user pages through *before* export. It is deliberately **not** the imposition
 * sheet: it shows [pages] in **reading order** (the reader's little book, cover → interior → back),
 * each page rendered through the *same* [SceneRenderer] → [PagePreview] path the editor canvas and the
 * page-strip thumbnails use ([EditorPagePreview], [EditorPageStrip]) — so there is no parallel rendering
 * model, and what you preview is what will print (`preview == export`, [ADR-006]/[ADR-028]).
 *
 * The reader/imposition split is intentional (SCREEN-INVENTORY): the imposition engine ([ADR-007]) maps
 * these reading-order pages onto a folded sheet at *export* time; here we only ever show the reader's
 * sequence, one page at a time, so the moment reads as pride ("I made this"), not prepress.
 *
 * Flow:
 *  - **primary** — [onPrintAndFold]: advance to Export · Print & fold (SCREEN-INVENTORY, the payoff peak).
 *  - **secondary** — [onBack]: return to editing.
 *  - **browse** — prev/next arrows + dots + a "page N of M" label page through the booklet.
 *
 * Stateless beyond the current page cursor (kept in `rememberSaveable` so a rotation preserves the page
 * the user is looking at). All content is hoisted; the host ([`:app`] `PreviewDestination`) reads it from
 * the *shared* editor store so the preview reflects the live document (including edits not yet flushed).
 *
 * @param pages the document pages in reading order (rendered one at a time).
 * @param pageSizePt the page size in points (imposition-derived; also the page clip) — same hoisted size
 *   the editor renders at, so a previewed page is a faithful scaled twin of the canvas.
 * @param defaults document defaults the renderer folds (background); same value the canvas uses.
 * @param onBack invoked by the secondary "Back to editing" action.
 * @param onPrintAndFold invoked by the primary "Print & fold" action (the Export seam — real export lands
 *   in the next S5 step; the host wires a warm placeholder until then).
 * @param modifier sizing/placement for the whole screen.
 * @param imageBytes import-master byte source for image elements; defaults to the missing-asset placeholder
 *   (the host threads its real source so the booklet matches the canvas).
 */
@Composable
public fun PreviewScreen(
    pages: List<Page>,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    onBack: () -> Unit,
    onPrintAndFold: () -> Unit,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
) {
    val colors = MaterialTheme.colorScheme
    val total = pages.size
    var index by rememberSaveable { mutableIntStateOf(0) }
    // Clamp defensively so a shrinking document (a page deleted while backgrounded) never strands the
    // cursor past the end; total==0 is guarded below.
    if (index > total - 1) index = (total - 1).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(PreviewScreenTestTag),
    ) {
        // Secondary action: back to editing (top-start, out of the thumb zone the primary owns).
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back to editing" },
            ) { Text("‹  Back to editing") }
        }

        // The pride line (mockup: "Your zine · Here's your little book.").
        Text(
            text = "Your zine",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Text(
            text = "Here's your little book.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
        )

        // The booklet + page-through arrows (fills the middle).
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PagerArrow(
                glyph = "‹",
                description = "Previous page",
                enabled = index > 0,
                onClick = { if (index > 0) index-- },
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (total > 0) {
                    BookletPage(
                        page = pages[index],
                        pageSizePt = pageSizePt,
                        defaults = defaults,
                        imageBytes = imageBytes,
                    )
                }
            }
            PagerArrow(
                glyph = "›",
                description = "Next page",
                enabled = index < total - 1,
                onClick = { if (index < total - 1) index++ },
            )
        }

        // Indicator: dots + "page N of M".
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until total) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == index) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (i == index) colors.tertiary else colors.outlineVariant),
                )
            }
        }
        Text(
            text = "page ${index + 1} of $total",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .testTag(PreviewPageLabelTestTag),
        )

        // Primary action: Print & fold (thumb zone). Enabled — it is the one obvious next step
        // (DESIGN-LANGUAGE §3); real PDF/image export lands in the next S5 step, the host wires the seam.
        Button(
            onClick = onPrintAndFold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
        ) { Text("Print & fold") }
    }
}

/** A round, generously-tappable page-through arrow; disabled (and dimmed) at the ends of the booklet. */
@Composable
private fun PagerArrow(
    glyph: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .shadow(if (enabled) 4.dp else 0.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
    }
}

/**
 * One booklet page, contained (fit whole) into the available area and dressed as a little paper sheet
 * (soft shadow, rounded corners, a hair of handmade tilt). The render itself is the shared
 * [SceneRenderer] → [PagePreview] path — no second rendering model (same as [EditorPageStrip]'s thumbnail).
 */
@Composable
private fun BookletPage(
    page: Page,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag(PreviewBookletTestTag),
        contentAlignment = Alignment.Center,
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        if (pageSizePt.width <= 0.0 || pageSizePt.height <= 0.0 || wPx <= 0f || hPx <= 0f) return@BoxWithConstraints

        // Contain, leaving a little breathing room so the paper sheet + its shadow are visible.
        val scale = (min(wPx / pageSizePt.width, hPx / pageSizePt.height) * 0.9).toFloat()
        val tape = remember(page, pageSizePt, defaults) {
            SceneRenderer.render(page, pageSizePt, defaults)
        }
        val density = LocalDensity.current
        val outW = with(density) { (pageSizePt.width * scale).toFloat().toDp() }
        val outH = with(density) { (pageSizePt.height * scale).toFloat().toDp() }
        Surface(
            modifier = Modifier
                .size(width = outW, height = outH)
                .graphicsLayer { rotationZ = -0.8f }
                .shadow(14.dp, RoundedCornerShape(4.dp)),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            PagePreview(
                tape = tape,
                sheet = pageSizePt,
                screenPxPerPt = scale,
                modifier = Modifier.fillMaxSize(),
                imageBytes = imageBytes,
            )
        }
    }
}
