package com.aritr.zinely.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * One object standing on the shelf: the title it is called and the cover it is printed on.
 *
 * **Two fields, not three.** The frozen markup carries a `data-sub` on every `.zine` too
 * (`"A4 · 2 days ago"`, `v2-library.html:149-154`) — and the frozen design's whole point is that the
 * shelf **does not show it**: *"Covers only — no metadata line … Format & date are disclosed there, not
 * stamped on every card"* (`:142-144`). That string is read by the action sheet's header, which is
 * **B3**. Carrying a `subtitle` here that nothing draws would be scaffolding for a package that has not
 * been written, so B3 adds it when B3 has a reader for it.
 *
 * No identity field either. [ZineShelf] leaves its grid keyed by position, which is correct for a list
 * that cannot yet reorder; **B5** brings real project data and, with it, the stable key.
 */
internal data class ZineShelfItem(
    val title: String,
    val recipe: ZineCoverRecipe,
)

/**
 * The test handle on one placed cover, keyed by its position on the shelf.
 *
 * Published from production for the reason V1's `homeCardTestTag(id)` is: the shelf's geometry is only
 * checkable at the **cover's** own bounds, and a cover is not otherwise addressable — searching for its
 * title finds the `Text` *inside* it, whose bounds are inset by the cover's own `padding:15px 15px 18px`.
 * Measuring that node instead reports the column gap as 177px and the side padding as 37px, which is how
 * this constant came to exist: those were the first numbers [ZineShelfTest] produced.
 *
 * **Position, not identity.** Matching the position-keyed grid — B5 brings the project id and this
 * becomes a function of that.
 */
internal fun zineShelfCoverTestTag(index: Int): String = "shelf-cover-$index"

/**
 * The frozen Library's shelf — `v2-library.html:46-49`, `:147-155`.
 *
 * ```
 * .shelf{flex:1 1 auto;overflow-y:auto;padding:30px 22px 152px;
 *        display:grid;grid-template-columns:1fr 1fr;gap:28px 20px;align-content:start}
 * .shelf::-webkit-scrollbar{width:0}
 * .shelf-head{grid-column:1 / -1;padding:2px 2px 0}
 * ```
 *
 * Two columns of [ZineCover] under one quiet heading, scrolling as one region. The frozen file's own
 * comment states what this screen is: *"no header chrome, no metadata line — the covers ARE the
 * screen"* (`:45`). There is no wordmark, no count, no search, no sort and no toolbar, and those
 * absences are the design rather than an unfinished state — V1's search and sort were dropped by owner
 * ruling, not overlooked ([ADR-081](docs/DECISIONS.md#adr-081), ruling 2).
 *
 * ### The heading scrolls away, because it is a cell in the grid
 *
 * `.shelf-head` is a `grid-column:1 / -1` **item inside the scrolling `.shelf`**, not a bar above it.
 * So "Your shelf" travels up with the covers and leaves the viewport. That is a one-character difference
 * to write and an invisible one to review — a pinned header composes perfectly, looks deliberate, and
 * is a different screen — so it is asserted (`the heading scrolls away with the covers`).
 *
 * ### This shelf paints no ground, and that is the frozen reading
 *
 * `.shelf` declares no `background`; the desk it sits on belongs to `.phone{background:var(--desk)}`
 * (`:41-43`) — the app window, which is **B5**'s screen. So this composable is transparent and B5 owes
 * the desk fill, exactly as B1's cover owes its own desk to whatever places it. B2's own rasters supply
 * a desk the way [ZineCoverGoldenTest] does, for the same reason.
 *
 * ### Two columns, always — and that is the ruling, not an omission
 *
 * `grid-template-columns:1fr 1fr` carries **no media query anywhere in the frozen file**, so the literal
 * transcription is a fixed two columns at every width. V1's shelf is responsive by contrast
 * (`shelfColumns`: 2 · 3 · 4 · 5), and Phase B's device verification includes **foldables**
 * ([COMPOSE-V2-ROADMAP.md](docs/COMPOSE-V2-ROADMAP.md)), where two columns means two very large covers.
 * Inventing a breakpoint here would have been inventing design, so B2 transcribed the freeze and raised
 * the gap instead — and **[D-020](docs/design/V2-SPEC-DEFECTS.md#d-020-ruling)** was then **ruled** in
 * favour of exactly that: *"The frozen design defines a two-column shelf. No breakpoint exists. No
 * responsive behaviour exists. No maximum cover width exists. Do not invent any of them."* An adaptive
 * shelf requires a **future frozen design**, never an inference made here — so this file holds no
 * `BoxWithConstraints`, no window-size class and no width branch, deliberately.
 *
 * ### The bottom padding is 152px with nothing under it yet
 *
 * `padding-bottom:152px` exists to clear the `.dock` and its "Make a zine" button (`:88-90`), which is
 * **B4**. Trimming it to what B2 alone appears to need would be a silent deviation that B4 then has to
 * discover, so the frozen value is transcribed whole and the space below the last cover is simply empty
 * until B4 fills it.
 *
 * ### What this composable deliberately does not hold
 *
 * `.zine` — the grid item wrapping each cover — carries the press transform
 * (`:active{transform:translateY(2px) scale(.985)}`), the focus ring
 * (`:focus-visible{outline:2px solid var(--matcha-text);outline-offset:6px}`), `cursor:pointer` and
 * `-webkit-tap-highlight-color` (`:51-54`). Every one of those is **interaction**, which is **B3** —
 * the package that also brings the long-press, the `⋯` button and the action sheet those states exist to
 * announce. At rest `.zine` paints nothing at all: no fill, no border, no padding, no transform. So a
 * cover placed straight into the grid is pixel-identical to a cover inside a resting `.zine`, and B3
 * adds the wrapper together with the gesture that gives it something to react to. [ZineCover] already
 * takes `pressed` as state for exactly this hand-off, so B3 wires a source to it rather than reworking
 * B1.
 *
 * @param zines the objects on the shelf, in the order they are given. The frozen file states no sort —
 *   V1's sort control was dropped, so ordering is the caller's, and B5's data layer answers for it.
 * @param modifier the caller's. `.shelf{flex:1 1 auto}` means **the shelf takes the space left over**,
 *   so a caller that gives it none gets a grid sized to its content and no scrolling; B5's screen passes
 *   `fillMaxSize()`.
 */
@Composable
internal fun ZineShelf(
    zines: List<ZineShelfItem>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ShelfColumns),
        modifier = modifier,
        contentPadding = ShelfPadding,
        verticalArrangement = Arrangement.spacedBy(ShelfRowGap),
        horizontalArrangement = Arrangement.spacedBy(ShelfColumnGap),
    ) {
        // `grid-column:1 / -1` — a full-width cell, so the row gap below it is the same 28px that
        // separates the cover rows. Nothing about this is a header component; it is a wide cell.
        item(span = { GridItemSpan(maxLineSpan) }) { ShelfHeading() }

        // No `key`: position is the identity a list that cannot reorder actually has. B5 supplies the
        // stable one with real projects — asserting a key here would be asserting B5's data model.
        itemsIndexed(zines) { index, zine ->
            ZineCover(
                title = zine.title,
                recipe = zine.recipe,
                modifier = Modifier.testTag(zineShelfCoverTestTag(index)),
            )
        }
    }
}

/**
 * `.shelf-head h1.serif` — *"Your shelf"*, `:48-49`, `:148`.
 *
 * ```
 * .shelf-head{grid-column:1 / -1;padding:2px 2px 0}
 * .shelf-head h1{margin:0;font-size:1.62rem;font-weight:600;letter-spacing:-.01em;color:var(--ink)}
 * ```
 *
 * **Fraunces 500, not the file's 600** — the same **D-005** ruling B1's cover title follows, and this
 * heading is one of the three the ruling names by selector (*"`.sh-ttl`, `.shelf-head h1`, `.empty
 * h2`"*). The 600 in the frozen CSS is an artefact of the Iowan/Georgia stack the Library was authored
 * against, and the Constitution outranks it. The register is the authority; the HTML is stale here.
 *
 * **A heading, not decorated text.** The frozen markup is an `<h1>`, so this carries `heading()`
 * semantics: TalkBack navigates by heading, and dropping the role turns a landmark into a stray phrase
 * while looking identical on screen. `<h1>` is transcription, not an accessibility addition of our own.
 *
 * No `lineHeight` is set, and that is deliberate: `h1` declares none, and the Library's `body` declares
 * none either, so the browser resolves `normal` — the font's own metrics. Compose does the same when
 * `lineHeight` is left unspecified. Pinning a number here would invent leading the design never states.
 */
@Composable
private fun ShelfHeading() {
    Text(
        text = ShelfHeadingText,
        style = TextStyle(
            fontFamily = ZinelyTheme.v2Typography.voice,
            // D-005: Fraunces 500 is the canonical weight for the shared serif role.
            fontWeight = FontWeight.Medium,
            fontSize = ShelfHeadingSize,
            letterSpacing = ShelfHeadingTracking,
            color = ZinelyTheme.v2Colors.ink,
        ),
        modifier = Modifier
            .padding(ShelfHeadingPadding)
            .semantics { heading() },
    )
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v2-library.html` at the lines named against each.
//
// Per-component literals, as B1's cover has them: V2 publishes no spacing scale. The D-007 ruling
// found the frozen CSS only 16.7% on the 8pt grid and left spacing "per-component as frozen"
// (ADR-074), and this shelf is the evidence — 30 · 22 · 152 · 28 · 20 · 2 puts exactly one of six
// values on the grid. A `ShelfSpacing` scale here would be inventing what that ruling declined.
// ---------------------------------------------------------------------------------------------

/** `grid-template-columns:1fr 1fr` — no breakpoint in the frozen file. See D-020. */
private const val ShelfColumns = 2

/** `.shelf{padding:30px 22px 152px}` — the 152px clears the `.dock`, which is B4's. */
private val ShelfPadding = PaddingValues(start = 22.dp, top = 30.dp, end = 22.dp, bottom = 152.dp)

/**
 * `.shelf{gap:28px 20px}` — CSS `gap` is **row-gap first**, then column-gap.
 *
 * Named apart rather than carried as one value because they are not one value, and transposing them is
 * the defect this package is most likely to ship: 28 between the rows and 20 between the columns looks
 * plausible either way round on a screenshot. `the row gap and the column gap are not interchangeable`
 * asserts both numbers and that they differ.
 */
private val ShelfRowGap = 28.dp
private val ShelfColumnGap = 20.dp

/** `.shelf-head{padding:2px 2px 0}` — 2px on three sides, nothing at the bottom. */
private val ShelfHeadingPadding = PaddingValues(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 0.dp)

/** `.shelf-head h1{margin:0}` — the heading's own text, `:148`. */
private const val ShelfHeadingText = "Your shelf"

/**
 * `font-size:1.62rem` against the browser's 16px root is **25.92px**, carried unrounded for B1's
 * reason: 25.92 → 26 is a visible change to the first heading a user reads, and the frozen file states
 * the ratio, not the pixel.
 */
private val ShelfHeadingSize = 25.92.sp

/** `letter-spacing:-.01em` — kept in `em`, the unit the CSS states, so it tracks the size. */
private val ShelfHeadingTracking = (-0.01).em
