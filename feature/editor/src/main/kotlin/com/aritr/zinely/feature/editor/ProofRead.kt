package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.min

/** Test tag on the Read act's pager. */
public const val ProofReadPagerTestTag: String = "proof-read-pager"

/** Test tag on the "page N of M" caption under the sheet. */
public const val ProofReadCaptionTestTag: String = "proof-read-caption"

/** Per-page test tag: `"proof-read-page-<pageNumber>"` (1-based, reading order). */
public fun proofReadPageTag(pageNumber: Int): String = "proof-read-page-$pageNumber"

/**
 * **Act 0 — Read** ([ADR-058](../../../../../../docs/DECISIONS.md#adr-058)): the finished zine, one page
 * per screen, swiped left to right in **reading order**. It answers the only question the Proof surface
 * did not: *what did I make?*
 *
 * Document order **is** reading order — imposition is what rearranges pages for the printer, and it stays
 * confined to the print acts. So this walks `pages` as they are, and deliberately shows none of the
 * printer's furniture: no imposed page numbers, no fold lines, no cut line, no editing chrome. The frozen
 * distinction the print world draws between *reader spreads* and *printer spreads* is the same one this
 * act draws against [ProofSheetAct].
 *
 * **No geometry of its own.** Each page is `SceneRenderer.render(page, …)` replayed through [PagePreview]
 * — the identical path [EditorPageStrip]'s thumbnails and the main canvas use, and the same
 * `CanvasReplayer` the PDF export replays ([ADR-028](../../../../../../docs/DECISIONS.md#adr-028)). That
 * is what keeps `read == preview == export` structural rather than a promise: there is no second way to
 * draw a page to disagree with.
 *
 * Accessibility: the pager is a labelled, swipeable region; the caption below it is a polite live region,
 * so a screen-reader user hears which page they landed on rather than inferring it from a silent swipe.
 * The rendered page itself is a Canvas with no text nodes — it cannot be read aloud, which is honest (its
 * content is ink on paper), and is why the caption carries the position.
 *
 * @param pages the document's pages, in document (= reading) order.
 * @param pageSizePt the page size in points — the same hoisted size the editor canvas renders at.
 * @param defaults document defaults the renderer folds in (background); same value the canvas uses.
 * @param imageBytes import-master byte source for image elements; the default renders the missing-asset
 *   placeholder, exactly as [PagePreview] documents.
 * @param modifier sizing/placement applied by [ProofScreen].
 */
@Composable
public fun ProofReadAct(
    pages: List<Page>,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
) {
    val colors = ZinelyTheme.colors
    val typography = ZinelyTheme.typography
    // A zero-page document cannot happen through any shipping path (every format seeds its pages), but a
    // pager with zero pages throws rather than drawing nothing — so the guard is structural, not defensive
    // decoration.
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(ProofReadPagerTestTag)
                .semantics { contentDescription = Copy.ProofRead.CONTENT_DESCRIPTION },
            // A slice of the neighbouring pages peeks in at each edge: it says "there is more this way"
            // without a hint chip, and it is what makes the surface read as a booklet rather than a slide
            // deck. The pager still snaps one whole page at a time.
            contentPadding = PaddingValues(horizontal = 34.dp),
            pageSpacing = 14.dp,
        ) { index ->
            ReadPage(
                page = pages[index],
                pageNumber = index + 1,
                pageSizePt = pageSizePt,
                defaults = defaults,
                imageBytes = imageBytes,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // The position readout. `liveRegion` so a swipe is announced — the page render is a Canvas with
        // no readable nodes, so without this a screen-reader user turning pages hears nothing at all.
        BasicText(
            text = Copy.ProofRead.pageOf(pagerState.currentPage + 1, pages.size),
            modifier = Modifier
                .padding(top = 10.dp, bottom = 4.dp)
                .testTag(ProofReadCaptionTestTag)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = typography.shell,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * One page of the reader: the page's own render, contained within the available space and sitting on a
 * lifted paper card so it reads as a sheet you could pick up rather than a rectangle on a dark desk.
 *
 * The paper card is sized to the **page**, not to the slot — same rule the editor canvas follows since the
 * two-scales defect: the backing and the render take one scale, so they cannot disagree about where the
 * sheet is.
 */
@Composable
private fun ReadPage(
    page: Page,
    pageNumber: Int,
    pageSizePt: PtSize,
    defaults: DocumentDefaults,
    imageBytes: AssetBytesSource,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        if (pageSizePt.width <= 0.0 || pageSizePt.height <= 0.0 || wPx <= 0f || hPx <= 0f) {
            return@BoxWithConstraints
        }

        val scale = min(wPx / pageSizePt.width, hPx / pageSizePt.height).toFloat()
        val tape = remember(page, pageSizePt, defaults) {
            SceneRenderer.render(page, pageSizePt, defaults)
        }
        val density = LocalDensity.current
        val outW = with(density) { (pageSizePt.width * scale).toFloat().toDp() }
        val outH = with(density) { (pageSizePt.height * scale).toFloat().toDp() }

        Box(
            modifier = Modifier
                .testTag(proofReadPageTag(pageNumber))
                .size(outW, outH)
                .shadow(10.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(ZinelyTheme.colors.paper)
                // The card is decoration around the render; the act's own label and the live caption carry
                // the a11y meaning, so this adds no traversable stop per page.
                .clearAndSetSemantics { },
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
