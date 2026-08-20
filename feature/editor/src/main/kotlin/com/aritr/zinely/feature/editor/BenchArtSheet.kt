package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.core.render.SupplyOutline
import com.aritr.zinely.render.android.SupplyPainter
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press

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
 * The frozen `<h3>Art</h3>` (`v21-bench.html:882`).
 *
 * ⚠ **Owed to `core:copy`.** Every other user-facing string this module renders comes from [Copy]; this one
 * and [BenchArtRecentHeading] have no home there yet because S6's scope was the sixteen supply *names*
 * (SUPPLIES-SPEC §10, `Copy.Supplies`). They belong in `Copy.Supplies` beside [Copy.Supplies.BY_FAMILY] and
 * should move there in the same change that adds them — recorded here rather than left as an anonymous
 * literal, so the debt is visible at the call site.
 */
public const val BenchArtSheetTitle: String = "Art"

/**
 * The frozen `Recent · ⭐ favourites` heading (`v21-bench.html:883`). Drawn **only** when a caller supplies
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

/** Frozen `.tile svg{width:60%;height:60%}` (`v21-bench.html:484`) — a fraction, not a dp. */
internal const val BenchArtGlyphFraction: Float = 0.6f

// ⚠ `BenchArtGlyphStroke = 1.7f` stood here, transcribing `.tile svg{stroke-width:1.7}`. **A tile has no
// stroke any more** (A7): it is the mark, filled, with the renderer's own even-odd rule. The frozen line it
// quoted no longer says that, so keeping the constant would have pinned a measurement the freeze had stopped
// making — the quietest kind of stale transcription, because it still compiles and still looks cited.

/** The word an unauthored tile carries instead of a picture of a mark nobody has drawn (A7 / D-086). */
internal val BenchArtNotYetSize = 8.96.sp

/** `.tile.na .naw{letter-spacing:.08em}`. */
internal val BenchArtNotYetTracking = 0.08.em

/** `.tile.na .naw{opacity:.55}` — on top of the tile's own dim, exactly as the frozen CSS stacks them. */
internal const val BenchArtNotYetAlpha: Float = 0.55f

/**
 * Frozen `.tile:nth-child(3n){--tt:-1.6deg}` / `.tile:nth-child(4n){--tt:1.4deg}` (`v21-bench.html:463-464`).
 *
 * ⚠ **Positional, and `.grid` restarts the count per section** — so every family row carries the identical
 * leaf · leaf · berry · butter pattern. That is not a transcription error; it is the *accepted price*
 * amendment **A5** books as an open observation for the owner (`v21-bench.html:1101-1104`): the old
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
 * ⚠ **`BenchArtGlyphs` stood here and was deleted on 2026-08-20** — sixteen hand-authored 24-unit icons,
 * stroked, maintained beside `SupplyCatalog` and drawn instead of it. Every tile was therefore hollow while
 * every placement is filled, which is [D-093]
 * (../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-093); `mark.halftone` drew **seven** dots for a
 * mark that has **sixteen**; and two glyphs had already needed correcting for the same reason
 * (`v21-bench.html` A3, A6).
 *
 * It is deleted rather than corrected because correcting it fixes the instance and keeps the mechanism. The
 * tile now renders the authored outline itself ([BenchArtMark]), so the set has no second copy to drift from
 * — and a supply authored tomorrow needs no glyph work at all. `v21-bench.html`'s own `SUP` table is now
 * **generated** from the catalogue for the same reason (amendment **A7**).
 */

/**
 * The frozen **Art sheet** — `openArt()` (`v21-bench.html:863-892`, CSS `:457-468`), as amended by
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
 * ### The supplies with no authored outline — the deliberate part
 *
 * [SupplyCatalog] is **12 of 16** (SUPPLIES-SPEC §10.1, S5) and `outlineOf()` returns `null` for the rest.
 * It was 4 of 16 when this sheet was written, which is why the reasoning below counts twelve; **the count
 * is not wired into anything here** — a tile's live state is derived per tile from `outlineOf`, so the
 * catalogue growing needed no edit to this file. Three readings were available and two are wrong:
 *
 * 1. **Draw only the authored tiles.** Rejected — the freeze specifies sixteen under four headings, and
 *    dropping families is the visual redesign the freeze forbids. It would also make *Tape & fixings* look
 *    like a thing the product does not have, when it is a thing the product has not drawn yet.
 * 2. **Draw sixteen live tiles.** Rejected — `SceneRenderer` emits nothing for an unauthored `supplyId`, so
 *    picking one places a real, selectable, movable element that paints **no pixels**. That is not a missing
 *    feature; from the maker's chair it is the app losing their action, which is the `0.9.0-beta.1` Preview
 *    failure in miniature.
 * 3. **Draw sixteen, and let the unauthored ones say what they are.** Taken. Each reports `disabled` to the
 *    *platform* tree, carries no `clickable` at all, and says why in `stateDescription` —
 *    [Copy.BenchVerbs.NOT_YET], the string this corpus already uses for exactly this.
 *
 * ⚠ **Amended 2026-08-20 (A7 / [D-086]).** This paragraph used to end *"a null never reaches the screen as a
 * blank tile with no explanation"* — and the explanation it meant was a **picture**: each unauthored tile drew
 * its own frozen glyph. That was the one part of this reading that could not survive its own premise. There is
 * no authored outline for those four, so the glyph was not a depiction of the supply; it was an invention of
 * one, and A5's *"the glyphs DEPICT the supplies"* cannot license depicting something that does not exist.
 * The four now carry the **word** [Copy.BenchVerbs.NOT_YET] and no mark. Which is also the answer to D-086's real complaint:
 * `stateDescription` was speaking that word to a screen reader while a sighted maker got only a dimmer tile
 * and was left to infer it — and the most available inference for a dim tile you just tapped is *the app is
 * broken*, not *this one isn't built yet*.
 *
 * This follows [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) — *a control the
 * freeze draws is kept drawn and invents nothing* — the same ruling that governs [BenchStyleRow]'s three
 * inert chips and decor's own `Replace`/`Ink` verbs. ⚠ It runs *against* [BenchAddChooser]'s reasoning for
 * omitting the `Art` row (*"a control that reports truthfully and then does nothing when tapped invites the
 * press harder than a blank one does"*), and the difference is which document is binding: the freeze draws
 * no `Art` row obligation the chooser must meet, and draws sixteen tiles here in terms. Where the freeze
 * specifies the control, OD-9 keeps it drawn; where it does not, absence is cheaper. **Flagged for Pass 2**
 * — a dim tile among live ones is a first-open impression no test can score, and it is the reading most
 * likely to be wrong. ⚠ Shrinking twelve to four did **not** close that question and may have sharpened
 * it: a lone dim tile surrounded by working ones reads more like a malfunction than a wholly dim family
 * did, because its neighbours prove the feature works ([D-086](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-086-update)).
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
 * printed shadow and tilted by its position in the grid (`v21-bench.html:460-471`).
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
    // place that knows, and it is asked here rather than assumed from the `shape.` prefix — the authored ids
    // now span four prefixes across three families, which is the plainest demonstration that the prefix was
    // never the family. Stated in the catalogue's own KDoc.
    // ⚠ The outline is now READ, not merely counted. It was `outlineOf(supplyId) != null`, which asked the
    // catalogue the one question the tile no longer needs answered on its own: the mark itself is what the
    // tile draws, so `null` is both "cannot be picked" and "there is nothing to show" — one fact, one read.
    val outline = SupplyCatalog.outlineOf(supplyId)
    val authored = outline != null
    // Named from the copy, never derived. A missing entry is a programming error, not a fallback: an id the
    // copy layer does not name is an id the user would hear read as its own key.
    val name = requireNotNull(Copy.Supplies.NAMES[supplyId]) {
        "$supplyId has no name in Copy.Supplies — the Art sheet must never speak a supplyId"
    }
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
        if (outline != null) {
            BenchArtMark(outline = outline, color = mark)
        } else {
            // ⚠ **No glyph.** Drawing a picture of a mark nobody has authored is depicting an invention,
            // which is the one part of the sixteen-tiles reading that could not survive its own premise
            // (`v21-bench.html` A7). The word is what `stateDescription` has always spoken; the only thing
            // that changes is that a sighted maker is now told it too — [D-086].
            Text(
                // `.tile.na .naw{text-transform:uppercase}`. **Compose has to be told** — the same trap
                // [BenchArtLabel] documents for `.lbl` and solves the same way. The first draft of this
                // control dropped it and the freeze read NOT AVAILABLE YET while the app read
                // `Not available yet`; a review caught it, no assertion did, and the measurement test
                // below now exists so the next one is caught by the suite instead.
                text = Copy.BenchVerbs.NOT_YET.uppercase(),
                color = colors.inkSoft.copy(alpha = BenchArtNotYetAlpha),
                fontSize = BenchArtNotYetSize,
                fontWeight = FontWeight.Bold,
                // The sheet's sans, never its voice face — `.naw` inherits the body font exactly as `.lbl`
                // does, and `LocalTextStyle` on this surface is not guaranteed to be it.
                fontFamily = ZinelyV21Fonts.Work,
                letterSpacing = BenchArtNotYetTracking,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                // Two short words on a square tile: centred, and allowed to wrap rather than clip. ⚠ At a
                // large system font scale the word can still outgrow the tile — the tile is `clip`ped, and
                // `contentDescription` carries the supply's name regardless, so nothing becomes unreachable.
                // Flagged for Pass 2 rather than solved by shrinking text that is already the smallest on
                // the surface.
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The tile's mark — **the authored outline itself**, filled, through the same seam every page surface uses.
 *
 * This is the whole of [D-093](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-093)'s ruling
 * expressed as code. The sheet used to draw [BenchArtGlyphs], a hand-authored 24-unit icon set maintained
 * *beside* `SupplyCatalog`, stroked at the container's weight. Every tile was therefore a **hollow outline**
 * and every placement a **fill**, and the catalogue contains no hollow mark at all — so the tile was not a
 * simplification of the mark, it was a contradiction of it. Two glyphs had already needed correcting for the
 * same reason (`v21-bench.html` A3, A6), and this pass found a third: `mark.halftone`'s glyph drew **seven**
 * dots for a mark that has **sixteen**.
 *
 * Hand-correcting the glyphs would have fixed the instance and kept the mechanism. Rendering the outline
 * retires the mechanism: **a tile cannot mispredict a mark it is.** The corollary is that a supply authored
 * tomorrow needs no glyph work at all, which is what makes [ADR-107](../../../../../../../../docs/DECISIONS.md#adr-107)'s
 * expansion affordable.
 *
 * The box is square ([Modifier.aspectRatio] on the tile), so the unit square is not distorted here even
 * though [SupplyPainter] permits it — a placement may stretch tape; a *picture of* tape must not, or the
 * tile stops predicting again in the other direction.
 */
@Composable
private fun BenchArtMark(outline: SupplyOutline, color: Color) {
    val painter = remember { SupplyPainter() }
    val argb = color.toArgb()
    Canvas(modifier = Modifier.fillMaxSize(BenchArtGlyphFraction)) {
        drawIntoCanvas { canvas ->
            painter.drawUnitSquare(canvas.nativeCanvas, outline, argb, size.width, size.height)
        }
    }
}


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
