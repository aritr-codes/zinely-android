package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Test tag on the page-strip container. */
public const val EditorPageStripTestTag: String = "editor-page-strip"

/** Per-card test tag suffix: `"$EditorPageStripTestTag-card-<pageNumber>"` (1-based). */
public fun editorPageCardTag(pageNumber: Int): String = "$EditorPageStripTestTag-card-$pageNumber"

/**
 * The scrapbook **page navigator** (docs/design/editor-visual-direction.md §4) — a strip of small
 * paper *cards*, one per page, laid on the "desk" band under the canvas. It is the on-screen carrier
 * of `Intent.GoToPage`: tapping a card selects that page. Before this, only page 0 of the eight-page
 * `SINGLE_SHEET_8` document was reachable in the mounted editor.
 *
 * Visual language: each card is paper (`surface`) on the desk (`background`); off-current cards sit at
 * a slight handmade tilt, the current card is lifted, upright-ish, and marked with a strip of "tape"
 * (`tertiary`); a page that already holds elements shows a small ink dot (`primary`). All decoration
 * rides a real control — the card *is* the navigation affordance.
 *
 * Accessibility: each card is a [selectable] with `Role.Tab`, a "Page N" content description, and a
 * selected/not-selected state, so the strip is a proper page picker to TalkBack; the visual tilt and
 * tape are decorative only. Touch targets are ≥48dp.
 *
 * Stateless: [currentPageIndex], [pageCount] and [pageHasContent] are read from the hoisted editor
 * state by the host; [onSelectPage] dispatches `Intent.GoToPage`. The reducer clears selection and
 * returns to Idle on a page change, so no ephemeral gesture state can leak across the switch.
 *
 * @param pageCount total pages in the document (cards rendered `0 until pageCount`).
 * @param currentPageIndex the page currently shown on the canvas (lifted + taped).
 * @param pageHasContent whether page `i` holds at least one element (drives the ink dot).
 * @param onSelectPage invoked with the tapped page index (the host dispatches `Intent.GoToPage`).
 * @param modifier sizing/placement applied by the host.
 */
@Composable
public fun EditorPageStrip(
    pageCount: Int,
    currentPageIndex: Int,
    pageHasContent: (Int) -> Boolean,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 0) return
    Row(
        modifier = modifier
            .testTag(EditorPageStripTestTag)
            .background(MaterialTheme.colorScheme.background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until pageCount) {
            PageCard(
                pageNumber = i + 1,
                current = i == currentPageIndex,
                hasContent = pageHasContent(i),
                onClick = { onSelectPage(i) },
            )
        }
    }
}

/** One paper page card. Tilt is deterministic per page so it never jitters across recompositions. */
@Composable
private fun PageCard(
    pageNumber: Int,
    current: Boolean,
    hasContent: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
        // The paper card itself (a hair smaller than the touch box).
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 60.dp)
                .shadow(if (current) 8.dp else 3.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$pageNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                color = if (current) colors.onSurface else colors.onSurfaceVariant,
            )
            // Ink dot: this page already holds elements.
            if (hasContent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.primary),
                )
            }
        }
        // Tape marker on the current page (decorative; sits above the card's top edge).
        if (current) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { rotationZ = -4f; translationY = -4.dp.toPx() }
                    .size(width = 30.dp, height = 13.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.tertiary),
            )
        }
    }
}
