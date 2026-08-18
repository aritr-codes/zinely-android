package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2IconShape

/** Test tag on the Art sheet's scrolling body. */
public const val BenchArtSheetTestTag: String = "bench-art-sheet"

/**
 * Per-tile test tag on a family shelf: `bench-art-tile-<supplyId>`.
 *
 * The Recent shelf has [benchArtRecentTileTestTag] instead, because a recently-used supply appears on
 * **both** shelves — the frozen prototype does exactly that with `SUP[8]` and `SUP[6]` — and one tag on two
 * nodes is a tag that can no longer address either.
 */
public fun benchArtTileTestTag(supplyId: String): String = "bench-art-tile-$supplyId"

/** Per-tile test tag on the Recent shelf. See [benchArtTileTestTag] for why it is a separate namespace. */
public fun benchArtRecentTileTestTag(supplyId: String): String = "bench-art-recent-tile-$supplyId"

/** Per-heading test tag: `bench-art-label-<family>`. */
public fun benchArtLabelTestTag(family: String): String = "bench-art-label-$family"

/**
 * The frozen `<h3>Art</h3>` (`v21-bench.html:855`).
 *
 * ⚠ **Owed to `core:copy`.** Every other user-facing string this module renders comes from [Copy]; this one
 * and [BenchArtRecentHeading] have no home there yet because S6's scope was the sixteen supply *names*
 * (SUPPLIES-SPEC §10, `Copy.Supplies`). They belong in `Copy.Supplies` beside [Copy.Supplies.BY_FAMILY] and
 * should move there in the same change that adds them — recorded here rather than left as an anonymous
 * literal, so the debt is visible at the call site.
 */
public const val BenchArtSheetTitle: String = "Art"

/**
 * The frozen `Recent · ⭐ favourites` heading (`v21-bench.html:856`). Drawn **only** when a caller supplies
 * a non-empty recents list — see [BenchArtSheet]'s note on the deferral.
 */
public const val BenchArtRecentHeading: String = "Recent · ⭐ favourites"

/** Frozen `.grid{grid-template-columns:repeat(4,1fr)}` (`v21-bench.html:459`). */
internal const val BenchArtGridColumns: Int = 4

/** Frozen `.grid{gap:var(--gap-sm)}` (`v21-bench.html:459`) — 8, the gap between tiles in both axes. */
internal val BenchArtGridGap = ZinelyV21Dimens.gapSm

/** Frozen `.lbl{margin:var(--gap-md) 0 var(--gap-sm)}` (`v21-bench.html:458`) — 12 above, 8 below. */
internal val BenchArtLabelSpaceAbove = ZinelyV21Dimens.gapMd
internal val BenchArtLabelSpaceBelow = ZinelyV21Dimens.gapSm

/** Frozen `.lbl{font-size:.62rem}` (`v21-bench.html:457`) — 9.92sp at the prototype's 16px root. */
internal val BenchArtLabelSize = 9.92.sp

/**
 * Frozen `.lbl{letter-spacing:.12em}`. Expressed in `em` rather than converted to sp because CSS `em`
 * tracking scales with the type size, and this label is one the user's font-scale setting moves.
 */
internal val BenchArtLabelTracking = 0.12.em

/** Frozen `.tile{border-radius:var(--br-sm)}` (`v21-bench.html:460`) — 8. */
internal val BenchArtTileShape: RoundedCornerShape = RoundedCornerShape(ZinelyV21Dimens.radiusSm)

/** Frozen `.tile{border:1.5px solid var(--ink)}` (`v21-bench.html:460`) — the language's pen. */
internal val BenchArtTileBorder = 1.5.dp

/** Frozen `.tile svg{width:60%;height:60%}` (`v21-bench.html:466`) — a fraction, not a dp. */
internal const val BenchArtGlyphFraction: Float = 0.6f

/** Frozen `.tile svg{stroke-width:1.7}` (`v21-bench.html:466`). */
internal const val BenchArtGlyphStroke: Float = 1.7f

/**
 * Frozen `.tile:nth-child(3n){--tt:-1.6deg}` / `.tile:nth-child(4n){--tt:1.4deg}` (`v21-bench.html:463-464`).
 *
 * ⚠ **Positional, and `.grid` restarts the count per section** — so every family row carries the identical
 * leaf · leaf · berry · butter pattern. That is not a transcription error; it is the *accepted price*
 * amendment **A5** books as an open observation for the owner (`v21-bench.html:1074-1077`): the old
 * eight-tile grid varied, the sixteen-tile one cannot, and fixing it needs a new selector, which A4's bar
 * says needs a ruling. Transcribed as frozen rather than quietly improved.
 *
 * `nth-child(12)` matches **both** rules and `4n` is declared later, so `4n` wins — hence the modulo-4 test
 * first. A reader who reverses these two lines gets a berry tile at position 12 and nowhere else, which is
 * exactly the kind of difference a 2 % raster threshold cannot see.
 */
internal fun benchArtTintIndex(oneBasedPosition: Int): Int = when {
    oneBasedPosition % 4 == 0 -> 2
    oneBasedPosition % 3 == 0 -> 1
    else -> 0
}

/** Frozen `.tile{--tt:0}` / `3n:-1.6deg` / `4n:1.4deg`, by [benchArtTintIndex]. */
internal val BenchArtTilt = floatArrayOf(0f, -1.6f, 1.4f)

/**
 * The sixteen tile glyphs, `supplyId` → the frozen `SUP` entry's SVG children (`v21-bench.html:848-854`).
 *
 * ### ⚠ These are not the authored outlines, and must never become them
 *
 * Amendment **A5** states it directly (`v21-bench.html:1063-1066`): *"The sixteen glyphs in `SUP` DEPICT the
 * supplies; they are not the authored outlines … a mockup must not become their source."* The outlines are
 * reviewed Kotlin in [SupplyCatalog] with a per-supply attestation (SUPPLIES-SPEC §4.1). These are 24-unit
 * chooser **icons**, in exactly the sense every other mark in `ZinelyV2Icons` is one, and they exist for all
 * sixteen supplies whether or not an outline does. What the freeze specifies here is that sixteen
 * *distinguishable* marks appear, in §4's order, under §4's four headings.
 *
 * `mark.asterisk` reuses the frozen file's own `DECOR.star` rather than a seventeenth glyph — the freeze
 * does the same, and for the same reason.
 *
 * Modelled as [ZinelyV2IconShape] rather than as raw path strings so `<circle>` and `<rect>` keep their own
 * parameters: SVG's shape-equivalence conversion is the part a human gets wrong, and `:core:ui` already has
 * it written down, tested, and shared with the V2 set. They are **not** [com.aritr.zinely.ui.theme.ZinelyV2Icon]s
 * — that set is pinned by a bidirectional set-equality test against the three `v2-*.html` files, and adding a
 * V2.1 Bench mark to it would break the very invariant that makes it trustworthy.
 */
internal val BenchArtGlyphs: Map<String, List<ZinelyV2IconShape>> = linkedMapOf(
    "tape.torn" to listOf(
        ZinelyV2IconShape.Path("M4 8h16v8H4z"),
        ZinelyV2IconShape.Path("M4 8l-1.6 2 1.6 2-1.6 2 1.6 2"),
        ZinelyV2IconShape.Path("M20 8l1.6 2-1.6 2 1.6 2-1.6 2"),
    ),
    "fix.corner" to listOf(
        ZinelyV2IconShape.Path("M5 5h14v14H5z"),
        ZinelyV2IconShape.Path("M5 13V5h8z"),
    ),
    "fix.staple" to listOf(
        ZinelyV2IconShape.Path("M4 15V9h16v6"),
        ZinelyV2IconShape.Path("M4 15h5M15 15h5"),
    ),
    "fix.clip" to listOf(
        ZinelyV2IconShape.Path("M15 7v8a4 4 0 01-8 0V7a2.6 2.6 0 015.2 0v8a1.2 1.2 0 01-2.4 0V8"),
    ),
    // `DECOR.star` (`v21-bench.html:641`), reused by the freeze itself.
    "mark.asterisk" to listOf(
        ZinelyV2IconShape.Path("M12 3l2.6 6.2 6.4.6-4.9 4.2 1.5 6.3L12 17l-5.6 3.3 1.5-6.3L3 9.8l6.4-.6z"),
    ),
    "mark.arrow" to listOf(
        ZinelyV2IconShape.Path("M4 12h15"),
        ZinelyV2IconShape.Path("M13 6l6 6-6 6"),
    ),
    "mark.halftone" to listOf(
        ZinelyV2IconShape.Circle(7f, 8f, 1.5f),
        ZinelyV2IconShape.Circle(13f, 7f, 2.2f),
        ZinelyV2IconShape.Circle(18f, 10f, 1.3f),
        ZinelyV2IconShape.Circle(9f, 14f, 2.2f),
        ZinelyV2IconShape.Circle(15f, 13f, 1.5f),
        ZinelyV2IconShape.Circle(18f, 17f, 1f),
        ZinelyV2IconShape.Circle(11f, 19f, 1.3f),
    ),
    "mark.registration" to listOf(
        ZinelyV2IconShape.Circle(12f, 12f, 6f),
        ZinelyV2IconShape.Path("M12 2v20M2 12h20"),
    ),
    "paper.strip" to listOf(
        ZinelyV2IconShape.Path("M8 5h8v14H8z"),
        ZinelyV2IconShape.Path("M8 5l2-1.6 2 1.6 2-1.6 2 1.6"),
        ZinelyV2IconShape.Path("M8 19l2 1.6 2-1.6 2 1.6 2-1.6"),
    ),
    "paper.window" to listOf(
        ZinelyV2IconShape.Path("M3 4h18v16H3z"),
        ZinelyV2IconShape.Path("M8 9h8v6H8z"),
    ),
    "paper.tag" to listOf(
        ZinelyV2IconShape.Path("M4 5h16v10H9l-4 4v-4H4z"),
    ),
    "paper.underline" to listOf(
        ZinelyV2IconShape.Path("M4 14c4 2 12 2 16-1"),
        ZinelyV2IconShape.Path("M4 18c4 2 12 2 16-1"),
    ),
    "shape.rect" to listOf(
        ZinelyV2IconShape.Rect(4f, 6f, 16f, 12f),
    ),
    "shape.circle" to listOf(
        ZinelyV2IconShape.Circle(12f, 12f, 8f),
    ),
    "shape.triangle" to listOf(
        ZinelyV2IconShape.Path("M12 4l8 15H4z"),
    ),
    "shape.rule" to listOf(
        ZinelyV2IconShape.Path("M3 12h18"),
        ZinelyV2IconShape.Path("M3 9.5v5M21 9.5v5"),
    ),
)

/**
 * The frozen **Art sheet** — `openArt()` (`v21-bench.html:847-865`, CSS `:457-468`), as amended by
 * **A5** on 2026-08-16; SUPPLIES-SPEC §4, §5 and §9; ADR-104 / Constitution Amendment 3.
 *
 * **One grid. Sixteen tiles, shelved under the four family headings, and nothing else.**
 *
 * ### What is deliberately absent
 *
 * No search field, no chip row, no filter, no sort. All three were *removed by ruling*, not left
 * unimplemented: SUPPLIES-SPEC §9 — *"No categories beyond the four. **No tags, no filters, no sort.**"* —
 * and A5's reasoning that a chip which filters is that search box with four buttons instead of a caret.
 * A5 also refuses the tempting re-task (chips as section anchors) on freeze grounds: *changing a control's
 * semantics from filter to navigation is an interaction redesign*, which DESIGN FREEZE forbids outright,
 * where deleting a control the spec has emptied is removal of dead UI. **Do not reintroduce a navigation
 * affordance here.** The whole cabinet is on the screen at once; that is the feature.
 *
 * ### The families come from the copy, never from the id
 *
 * Iteration is over [Copy.Supplies.BY_FAMILY], whose key *is* the heading and whose value *is* the family's
 * four supplies in §4's frozen order. Nothing here reads `supplyId.substringBefore('.')`, and nothing here
 * should start: **five prefixes carry four families** (`tape` and `fix` are both *Tape & fixings*), and
 * deriving a name from an id is precisely how TalkBack shipped saying *"Rect shape"*.
 *
 * ### The ☆ is not drawn, and that is the deferral rather than an omission
 *
 * SUPPLIES-SPEC §9 defers favourites and recents (*"Not struck, just not first"*), and A5 books the mockup's
 * own star as a **known defect**: `.fav` is `role="button"` inside the tile's `<button>`, which is invalid
 * interactive nesting with no valid Compose transcription — *a control cannot contain a control*. A5's
 * instruction to whoever implements it is explicit: *"a shape that is a sibling of the tile, not a child of
 * it."* Following the deferral, this draws neither the star nor a Recent row of its own invention. The
 * [recent] parameter exists so the row's **structure** is here when recents land, and defaults to empty, so
 * today the sheet neither invents which two supplies a first-time maker has recently used nor deletes the
 * shelf they will go on. Nothing about the star is decided here; it is left to the change that earns it.
 *
 * ### The twelve supplies with no authored outline — the deliberate part
 *
 * [SupplyCatalog] is **4 of 16** (SUPPLIES-SPEC §10.1, S5): only the *Cut shapes* family is authored, and
 * `outlineOf()` returns `null` for the other twelve. Three readings were available and two are wrong:
 *
 * 1. **Draw four tiles.** Rejected — the freeze specifies sixteen under four headings, and dropping three
 *    families is the visual redesign the freeze forbids. It would also make *Tape & fixings* look like a
 *    thing the product does not have, when it is a thing the product has not drawn yet.
 * 2. **Draw sixteen live tiles.** Rejected — `SceneRenderer` emits nothing for an unauthored `supplyId`, so
 *    picking one places a real, selectable, movable element that paints **no pixels**. That is not a missing
 *    feature; from the maker's chair it is the app losing their action, which is the `0.9.0-beta.1` Preview
 *    failure in miniature.
 * 3. **Draw sixteen, and let the twelve say what they are.** Taken. Each of the twelve is drawn with its own
 *    frozen glyph, reports `disabled` to the *platform* tree, carries no `clickable` at all, and says why in
 *    `stateDescription` — [Copy.BenchVerbs.NOT_YET], the string this corpus already uses for exactly this.
 *
 * The glyph is **not** a placeholder and nothing is invented: [BenchArtGlyphs] is the freeze's own depiction
 * of that supply, which exists independently of the outline (see its KDoc). What the missing outline governs
 * is whether the tile can be *picked*, not what it looks like — so a null never reaches the screen as a
 * blank tile with no explanation.
 *
 * This follows [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) — *a control the
 * freeze draws is kept drawn and invents nothing* — the same ruling that governs [BenchStyleRow]'s three
 * inert chips and decor's own `Replace`/`Ink` verbs. ⚠ It runs *against* [BenchAddChooser]'s reasoning for
 * omitting the `Art` row (*"a control that reports truthfully and then does nothing when tapped invites the
 * press harder than a blank one does"*), and the difference is which document is binding: the freeze draws
 * no `Art` row obligation the chooser must meet, and draws sixteen tiles here in terms. Where the freeze
 * specifies the control, OD-9 keeps it drawn; where it does not, absence is cheaper. **Flagged for Pass 2**
 * — twelve dim tiles out of sixteen is a first-open impression no test can score, and it is the reading most
 * likely to be wrong.
 *
 * ### Not wired to the Add chooser yet — deliberately
 *
 * There is no `Intent` that places a [com.aritr.zinely.core.model.DecorElement] (SUPPLIES-SPEC §10, S7's
 * remaining half), and `CanvasReplayer`'s `DrawShape` arm is still the stub S4 leaves *"deliberately and
 * visibly"* drawing nothing (§10.1). Adding the `Art` row to [BenchAddChooser] today would open a cabinet
 * a maker cannot take anything out of. This composable is the frozen surface, complete and tested; the row
 * lands with the placement it needs.
 *
 * @param recent supplies to shelve under the frozen `Recent · ⭐ favourites` heading. Empty (the default)
 *   draws no heading and no row — the §9 deferral, expressed as data rather than as deleted markup.
 * @param onPick the tile's own handler. Called with the `supplyId`; never called for a supply whose outline
 *   is unauthored, because those tiles carry no click at all.
 */
@Composable
internal fun BenchArtSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (supplyId: String) -> Unit,
    recent: List<String> = emptyList(),
) {
    ZSheet(visible = visible, onDismiss = onDismiss, title = BenchArtSheetTitle) {
        BenchArtSheetBody(onPick = onPick, recent = recent)
    }
}

/**
 * The sheet's body without the `Dialog` window — split out for the same reason [ZSheet] splits
 * `ZSheetSurface`: a `Dialog`'s own window is invisible to the decor-view golden harness, so the raster
 * tests compose this inside `ZSheetSurface` instead. Production always goes through [BenchArtSheet].
 */
@Composable
internal fun BenchArtSheetBody(onPick: (supplyId: String) -> Unit, recent: List<String> = emptyList()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BenchArtSheetTestTag),
    ) {
        val sections = buildList {
            if (recent.isNotEmpty()) add(BenchArtRecentHeading to recent)
            Copy.Supplies.BY_FAMILY.forEach { (family, supplies) ->
                add(family to supplies.keys.toList())
            }
        }
        val recentShelf = recent.isNotEmpty()
        sections.forEachIndexed { index, (heading, supplyIds) ->
            BenchArtLabel(heading, collapsed = index == 0)
            BenchArtGrid(
                supplyIds = supplyIds,
                onPick = onPick,
                tag = if (recentShelf && index == 0) ::benchArtRecentTileTestTag else ::benchArtTileTestTag,
            )
        }
    }
}

/**
 * Frozen `.lbl` — the section heading the four families now wear (`v21-bench.html:457-458`).
 *
 * A5's *"nothing new is drawn"* claim rests on this: `.lbl` was already in the corpus, already a section
 * heading, and the amendment added **no CSS rule at all** — which A5 calls the tell that the corpus already
 * had the vocabulary. `--ink-soft` on `--paper` measures 6.16:1 light / 6.32:1 dark.
 *
 * It is a real heading to the accessibility tree, not decoration: without it the sixteen tiles arrive as one
 * undifferentiated run and the four families — the only structure left on this surface — become invisible to
 * exactly the user who cannot see the layout. That is `heading()`, and it is an **addition** rather than a
 * transcription — the freeze writes `.lbl` as a bare `<div>`, so the markup does not authorise the role and
 * the argument has to be made rather than cited. It is made on A5's own precedent: giving the tile an
 * `aria-label` was *"one defect fixed in passing, which the freeze permits as an accessibility fix"*, and
 * this is the same defect one level up. The corpus already treats it as house style (`ZineShelf`,
 * `ZineShelfEmpty`, `ZineShelfFail`, `EditorEmptyState`), and `ZineShelfTest` calls it *"exactly the kind of
 * property that could be dropped in a green suite"* — so it is asserted, not merely set.
 *
 * ### Two things CSS does that Compose has to be told
 *
 * **`text-transform:uppercase` is presentation.** The glyphs are upper-cased; the *name* is not, so the node
 * carries `Copy.Supplies`' own casing as its `contentDescription`. A screen reader announcing `TAPE &
 * FIXINGS` is at the mercy of its own abbreviation heuristics, and the family names are the one vocabulary
 * §4 froze.
 *
 * **Adjacent CSS margins collapse and Compose paddings do not.** `.sheet h3` ends in a `--gap-md` bottom
 * margin and `.lbl` opens with a `--gap-md` top margin; CSS renders **12px between them**, not 24 — A5's own
 * measurement subtracts exactly this (*"less the 12px the `h3`'s bottom margin and the first `.lbl`'s top
 * margin collapse into one"*). So the first heading drops its top space; every later one keeps it, because
 * `.grid` has no bottom margin to collapse with.
 *
 * @param collapsed whether this heading directly follows the sheet title, per the margin note above.
 */
@Composable
private fun BenchArtLabel(text: String, collapsed: Boolean) {
    Text(
        text = text.uppercase(),
        color = ZinelyTheme.v21Colors.inkSoft,
        fontSize = BenchArtLabelSize,
        // `.lbl{font-weight:700}` in the inherited body face — the sheet's sans, not its voice.
        fontWeight = FontWeight.Bold,
        fontFamily = ZinelyV21Fonts.Work,
        letterSpacing = BenchArtLabelTracking,
        lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        modifier = Modifier
            .padding(
                top = if (collapsed) 0.dp else BenchArtLabelSpaceAbove,
                bottom = BenchArtLabelSpaceBelow,
            )
            .testTag(benchArtLabelTestTag(text))
            .semantics {
                contentDescription = text
                heading()
            },
    )
}

/**
 * Frozen `.grid` — four equal columns, 8dp apart (`v21-bench.html:459`).
 *
 * Rows of weighted children rather than a `LazyVerticalGrid`: the sheet is already the scrolling container
 * (`.sheet{overflow:auto}`), a lazy grid nested in it needs a bounded height it has no honest value for, and
 * §9's whole claim is that **sixteen items fit on one screen** — there is nothing here to virtualise. The
 * last row is padded with empty weights so a short section keeps square tiles instead of stretching them.
 */
@Composable
private fun BenchArtGrid(supplyIds: List<String>, onPick: (String) -> Unit, tag: (String) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(BenchArtGridGap)) {
        supplyIds.chunked(BenchArtGridColumns).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(BenchArtGridGap)) {
                row.forEachIndexed { columnIndex, supplyId ->
                    BenchArtTile(
                        supplyId = supplyId,
                        // `nth-child` counts within the grid, from 1 — see [benchArtTintIndex].
                        position = rowIndex * BenchArtGridColumns + columnIndex + 1,
                        onPick = onPick,
                        testTag = tag(supplyId),
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(BenchArtGridColumns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Frozen `.tile` — a square of tinted paper, outlined in ink, standing on [ZinelyV21Press.Flat]'s 2dp
 * printed shadow and tilted by its position in the grid (`v21-bench.html:460-466`).
 *
 * The tint is **material, not state** (V21-SPEC §3.2): leaf, berry and butter here are the colour of the
 * paper the mark is printed on, and they cycle by grid position, so nothing about a berry tile means
 * anything different from a leaf one. The 1.5dp ink ring is what carries every meaning the tile has.
 *
 * ### The name is the supply's own, and it is the whole label
 *
 * `contentDescription` is [Copy.Supplies.NAMES]`[supplyId]`, which SUPPLIES-SPEC §8 calls the naming
 * contract arriving at the chooser — and A5 records that fixing this *was* an accessibility fix the freeze
 * permits, because the frozen tile was a `<button>` wrapping a bare `<svg>` and announced nothing at all.
 * The glyph is `contentDescription = null` beneath it: one control, one name, no fragments to reassemble.
 *
 * ### Unauthored supplies are drawn, disabled, and say so
 *
 * See [BenchArtSheet]'s note. Two consequences of being inert, both taken from [BenchStyleRow]'s inert chip
 * so the two surfaces cannot disagree about what disabled looks like:
 *
 * - **It sheds its printed shadow entirely** — the corpus rule is `.icon-btn:disabled{box-shadow:none}`
 *   (`v21-bench.html:345`), and it is not an alpha: pressed means *you are pushing this*, disabled means
 *   *there is nothing here to push*.
 * - **The dim reaches the outline, not only the mark.** Fading the glyph and leaving a full-strength ink
 *   ring draws *heavier* chrome than the freeze does.
 *
 * It carries no `clickable` modifier at all, deliberately: `disabled()` alone still leaves a node an
 * accessibility service may offer to activate, and `assertIsNotEnabled` passing against Compose's **merged**
 * tree while the platform `AccessibilityNodeInfo` says `enabled` is a defect this programme has already
 * shipped once (ADR-058, `ReframeControls.ZoomButton`). `BenchArtSheetPlatformA11yTest` reads the real tree.
 */
@Composable
private fun BenchArtTile(
    supplyId: String,
    position: Int,
    onPick: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val tint = benchArtTintIndex(position)
    val ground = when (tint) {
        1 -> colors.berryTint
        2 -> colors.butterTint
        else -> colors.leafTint
    }
    val mark = when (tint) {
        1 -> colors.jamText
        2 -> colors.inkSoft
        else -> colors.leafText
    }
    // The tile can be picked only if picking it would put something on the page. `SupplyCatalog` is the one
    // place that knows, and it is asked here rather than assumed from the `shape.` prefix — that all four
    // authored ids share that prefix is a coincidence of which family was cheapest to author first, stated
    // in the catalogue's own KDoc.
    val authored = SupplyCatalog.outlineOf(supplyId) != null
    // Named from the copy, never derived. A missing entry is a programming error, not a fallback: an id the
    // copy layer does not name is an id the user would hear read as its own key.
    val name = requireNotNull(Copy.Supplies.NAMES[supplyId]) {
        "$supplyId has no name in Copy.Supplies — the Art sheet must never speak a supplyId"
    }
    val glyph = rememberBenchArtGlyph(supplyId)
    val pick = benchTap { onPick(supplyId) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dim = ZinelyV21Dimens.disabledAlpha

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // `transform:rotate(var(--tt))` sits OUTSIDE the press in CSS — a transform carries the
            // element's `box-shadow` with it — so the rotation must be to the left of the shadow, or the
            // tile tilts and its printed shadow stays square to the sheet.
            .graphicsLayer { rotationZ = BenchArtTilt[tint] }
            // ⚠ Nothing that clips may sit to the LEFT of the press. The `clip` is downstream on purpose.
            .then(
                if (authored) {
                    Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, BenchArtTileShape)
                } else {
                    Modifier
                },
            )
            .clip(BenchArtTileShape)
            .background(if (authored) ground else ground.copy(alpha = dim))
            .border(
                BenchArtTileBorder,
                if (authored) colors.ink else colors.ink.copy(alpha = dim),
                BenchArtTileShape,
            )
            .then(
                if (authored) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = pick)
                } else {
                    // ⚠ Inert must still be *hittable*. Found by the S7 wiring test the moment the sheet
                    // gained a production call site: a tile with no pointer-input node at all is not in the
                    // hit path, so the touch fell through to [ZSheet]'s full-screen scrim **sibling**,
                    // whose `clickable` is `onDismiss` — tapping any of the twelve closed the cabinet. From
                    // the maker's chair that is *"Not yet"* answered by the app leaving the room, which is
                    // a worse sentence than the one the tile is trying to say.
                    //
                    // It observes the gesture and **consumes nothing**: consuming the down would win the
                    // drag away from any ancestor scroll, and twelve of sixteen tiles that cannot be
                    // scrolled past would be the next defect. Being in the hit path is the whole fix.
                    //
                    // This is not a click: no `clickable`, no `onClick` semantics, nothing an accessibility
                    // service can activate. The platform-tree assertion in `BenchArtSheetPlatformA11yTest`
                    // is what holds that line.
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
                    }
                },
            )
            .testTag(testTag)
            .clearAndSetSemantics {
                contentDescription = name
                role = Role.Button
                if (authored) {
                    onClick { pick(); true }
                } else {
                    disabled()
                    // The reason rides state, never the name: the tile is still called *Torn tape*.
                    stateDescription = Copy.BenchVerbs.NOT_YET
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = null,
            tint = if (authored) mark else mark.copy(alpha = dim),
            modifier = Modifier.fillMaxSize(BenchArtGlyphFraction),
        )
    }
}

/**
 * The tile's mark as an [ImageVector], built once per supply.
 *
 * Built here rather than declared as a constant for the reason `ZinelyV2Icons` gives at length: an
 * `ImageVector` bakes `strokeLineWidth` in, and in this design **an icon does not own its stroke weight —
 * the container does**. `.tile svg{stroke-width:1.7}` is the container's, so it is applied here, at the one
 * call site that has it.
 */
@Composable
private fun rememberBenchArtGlyph(supplyId: String): ImageVector = remember(supplyId) {
    val shapes = requireNotNull(BenchArtGlyphs[supplyId]) {
        "$supplyId has no frozen glyph — the Art sheet draws the freeze's sixteen and invents none"
    }
    val builder = ImageVector.Builder(
        name = supplyId,
        defaultWidth = BenchArtGlyphViewport.dp,
        defaultHeight = BenchArtGlyphViewport.dp,
        viewportWidth = BenchArtGlyphViewport,
        viewportHeight = BenchArtGlyphViewport,
    )
    shapes.forEach { shape ->
        builder.addPath(
            pathData = PathParser().parsePathString(shape.pathData()).toNodes(),
            // `fill:none;stroke:currentColor` — opaque black is the placeholder `Icon`'s tint replaces,
            // which is how `currentColor` behaves and how Material's own icons are built.
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = BenchArtGlyphStroke,
            // `.tile svg{stroke-linecap:round;stroke-linejoin:round}` (`v21-bench.html:467`).
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    builder.build()
}

/** `svg(d)` writes `viewBox="0 0 24 24"` for every mark in the file (`v21-bench.html:627`). */
private const val BenchArtGlyphViewport: Float = 24f

/**
 * Why the Art sheet is open — SUPPLIES-SPEC §8.
 *
 * The frozen `openArt()` had one caller and needed no such distinction. Two verbs now summon the same
 * cabinet, and they do opposite things with the tile the maker taps: one adds a supply to the page, the
 * other swaps the outline of a supply already on it. Modelling that as *purpose* rather than as a flag plus
 * a target id means the sheet cannot be open in "replace" mode with no element to replace, and cannot be
 * open in "place" mode while still holding a stale id from the last swap.
 */
internal sealed interface BenchArtPurpose {

    /** Add ▸ Art: the tapped tile becomes a new element (`Intent.PlaceSupply`). */
    data object Place : BenchArtPurpose

    /**
     * A selected supply's `Replace` verb: the tapped tile becomes [id]'s new outline
     * (`Intent.ReplaceSupply`). The id is captured when the sheet opens rather than read from the live
     * selection when a tile is tapped — the sheet is a `Dialog` over the bench, and reading the selection
     * later would let a selection change underneath it retarget the swap.
     */
    data class Replace(val id: String) : BenchArtPurpose
}
