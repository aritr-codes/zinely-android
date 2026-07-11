package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.min

/** Test tag on the page-strip container. */
public const val EditorPageStripTestTag: String = "editor-page-strip"

/** Per-card test tag suffix: `"$EditorPageStripTestTag-card-<pageNumber>"` (1-based). */
public fun editorPageCardTag(pageNumber: Int): String = "$EditorPageStripTestTag-card-$pageNumber"

/** Per-card mini-render test tag suffix: `"$EditorPageStripTestTag-thumb-<pageNumber>"` (1-based). */
public fun editorPageThumbTag(pageNumber: Int): String = "$EditorPageStripTestTag-thumb-$pageNumber"

/**
 * The scrapbook **page navigator** (docs/design/editor-visual-direction.md §4) — a strip of small
 * paper *cards*, one per page, laid on the "desk" band under the canvas. It is the on-screen carrier
 * of `Intent.GoToPage`: tapping a card selects that page. Before this, only page 0 of the eight-page
 * `SINGLE_SHEET_8` document was reachable in the mounted editor.
 *
 * Visual language: each card is a **live miniature page preview** — the page's own
 * [SceneRenderer] tape replayed through [PagePreview], the *same* render path the main canvas uses
 * ([EditorPagePreview]), so a card looks like the page it navigates to rather than a numbered
 * placeholder. Off-current cards sit at a slight handmade tilt; the current card is lifted,
 * upright-ish, and marked with a strip of "tape" (`tertiary`). An **empty** page renders as a blank
 * sheet, so it keeps a faint page number to stay legible and inviting (DESIGN-RULES 4, 10). All
 * decoration rides a real control — the card *is* the navigation affordance, the thumbnail *is* the
 * page.
 *
 * Accessibility: each card is a [selectable] with `Role.Tab`, a "Page N" content description, and a
 * selected/not-selected state, so the strip is a proper page picker to TalkBack; the tilt, tape, and
 * the mini-render itself are decorative (the thumbnail clears no semantics it needs and adds none —
 * the card owns the label). Touch targets are ≥48dp.
 *
 * Stateless: [pages], [currentPageIndex], [pageSizePt] and [defaults] are read from the hoisted editor
 * state by the host; [onSelectPage] dispatches `Intent.GoToPage`. The reducer clears selection and
 * returns to Idle on a page change, so no ephemeral gesture state can leak across the switch.
 *
 * @param pages the document pages (cards rendered in order); each card mini-renders its page.
 * @param currentPageIndex the page currently shown on the canvas (lifted + taped).
 * @param pageSizePt the page/panel size in points — the same hoisted size the main canvas renders at,
 *   so a thumbnail is a faithful scaled-down twin of the page.
 * @param defaults document defaults the renderer folds (background); same value the canvas uses.
 * @param onSelectPage invoked with the tapped page index (the host dispatches `Intent.GoToPage`).
 * @param modifier sizing/placement applied by the host.
 * @param imageBytes import-master byte source for image elements; defaults to the missing-asset
 *   placeholder (the host threads its real source so thumbnails match the canvas).
 */
@Composable
public fun EditorPageStrip(
    pages: List<Page>,
    currentPageIndex: Int,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
) {
    if (pages.isEmpty()) return
    Row(
        modifier = modifier
            .testTag(EditorPageStripTestTag)
            // The rail sits on the frozen "desk" surface (bench.html `.rail` over `--desk`), not the
            // abused Legacy `background` slate.
            .background(ZinelyTheme.colors.desk)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pages.forEachIndexed { i, page ->
            PageCard(
                pageNumber = i + 1,
                page = page,
                current = i == currentPageIndex,
                pageSizePt = pageSizePt,
                defaults = defaults,
                imageBytes = imageBytes,
                onClick = { onSelectPage(i) },
            )
        }
    }
}

/** One paper page card. Tilt is deterministic per page so it never jitters across recompositions. */
@Composable
private fun PageCard(
    pageNumber: Int,
    page: Page,
    current: Boolean,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.colors
    // Deterministic handmade tilt: small alternating angle by page parity (the current card overrides).
    val tilt = if (current) -3f else if (pageNumber % 2 == 0) 2.5f else -2f

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 66.dp)
            .graphicsLayer {
                rotationZ = tilt
                if (current) {
                    translationY = -10.dp.toPx()
                    scaleX = 1.08f
                    scaleY = 1.08f
                }
            }
            .selectable(
                selected = current,
                onClick = onClick,
                role = Role.Tab,
            )
            .testTag(editorPageCardTag(pageNumber))
            .semantics {
                contentDescription = "Page $pageNumber"
                stateDescription = if (current) "Current page" else "Not selected"
            },
        contentAlignment = Alignment.Center,
    ) {
        // The paper card itself (a hair smaller than the touch box); clips the mini-render to its edge.
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 60.dp)
                .shadow(if (current) 8.dp else 3.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            PageThumbnail(
                page = page,
                pageSizePt = pageSizePt,
                defaults = defaults,
                imageBytes = imageBytes,
                modifier = Modifier
                    .testTag(editorPageThumbTag(pageNumber))
                    .size(width = 44.dp, height = 60.dp),
            )
            // Empty page renders as a blank sheet — keep a faint page number so it stays legible and
            // inviting (a content card needs no number; its thumbnail already identifies it).
            if (page.elements.isEmpty()) {
                Text(
                    text = "$pageNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    color = colors.inkSoft,
                )
            }
        }
        // Tape marker on the current page (decorative; sits above the card's top edge). The tape uses the
        // frozen `--yellow` authorial ink (the migrated Legacy `tertiary` role, byte-identical).
        // DIVERGENCE (reported, not redesigned): bench.html marks the current panel with an inset
        // `2px var(--coral-strong)` ring (`.panelbtn.cur`), not a yellow tape strip — a conflict between
        // the frozen Bench spec and editor-visual-direction.md's tape metaphor, left for a design decision.
        if (current) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { rotationZ = -4f; translationY = -4.dp.toPx() }
                    .size(width = 30.dp, height = 13.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.yellow),
            )
        }
    }
}

/**
 * A live miniature of one [page]: the page's [SceneRenderer] tape replayed through [PagePreview] — the
 * same render path [EditorPagePreview] uses, so the thumbnail is a faithful scaled-down twin of the
 * canvas (no second rendering model). The page is **contained** within the card: [BoxWithConstraints]
 * measures the card, the fit scale (device-px per point) sizes a [PagePreview] to the page's aspect,
 * and any leftover sliver shows the paper card behind it. Purely decorative — the parent card owns the
 * navigation control + a11y label.
 */
@Composable
private fun PageThumbnail(
    page: Page,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        // Degenerate sizes (zero page or zero box) → nothing to draw; the paper card stands in.
        if (pageSizePt.width <= 0.0 || pageSizePt.height <= 0.0 || wPx <= 0f || hPx <= 0f) return@BoxWithConstraints

        // Contain: fit the whole page into the card, device-px per point.
        val scale = min(wPx / pageSizePt.width, hPx / pageSizePt.height).toFloat()
        // Recomputed only when the page content / size / defaults change (value-equality keys), so an
        // edit to one page re-renders only that card — the strip stays light per keystroke.
        val tape = remember(page, pageSizePt, defaults) {
            SceneRenderer.render(page, pageSizePt, defaults)
        }
        val density = LocalDensity.current
        val outW = with(density) { (pageSizePt.width * scale).toFloat().toDp() }
        val outH = with(density) { (pageSizePt.height * scale).toFloat().toDp() }
        PagePreview(
            tape = tape,
            sheet = pageSizePt,
            screenPxPerPt = scale,
            modifier = Modifier.size(width = outW, height = outH),
            imageBytes = imageBytes,
        )
    }
}
