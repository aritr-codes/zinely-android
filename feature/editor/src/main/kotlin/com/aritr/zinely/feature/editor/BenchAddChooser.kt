package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.toImageVector

/** Test tag on the Add chooser's row container. */
public const val BenchAddChooserTestTag: String = "bench-add-chooser"

/** Per-row test tags. */
public const val BenchAddChooserTextTag: String = "bench-add-chooser-text"
public const val BenchAddChooserPhotoTag: String = "bench-add-chooser-photo"
public const val BenchAddChooserArtTag: String = "bench-add-chooser-art"

/** The frozen sheet title (`v21-bench.html:784`). */
public const val BenchAddChooserTitle: String = Copy.AddChooser.TITLE

/** The frozen `Text` row (`v21-bench.html:785`), title and subtitle verbatim. */
public const val BenchAddTextTitle: String = Copy.AddChooser.TEXT_TITLE
public const val BenchAddTextSubtitle: String = Copy.AddChooser.TEXT_SUBTITLE

/** The frozen `Photo` row (`v21-bench.html:786`), title and subtitle verbatim. */
public const val BenchAddPhotoTitle: String = Copy.AddChooser.PHOTO_TITLE
public const val BenchAddPhotoSubtitle: String = Copy.AddChooser.PHOTO_SUBTITLE

/** The frozen `Art` row (`v21-bench.html:841`), title and subtitle verbatim. */
public const val BenchAddArtTitle: String = Copy.AddChooser.ART_TITLE
public const val BenchAddArtSubtitle: String = Copy.AddChooser.ART_SUBTITLE

/** Frozen `.supply{gap:var(--gap-sm)}` (`v21-bench.html:370`) — 8, unchanged from V2. */
internal val BenchOptGap = ZinelyV21Dimens.gapSm

/**
 * Frozen `.opt{padding:var(--gap-md);gap:var(--gap-md)}` (`v21-bench.html:371-372`) — 12 on every side and
 * 12 between the tile and the label, where V2 asked for 13/12/13.
 */
internal val BenchOptPaddingH = ZinelyV21Dimens.gapMd
internal val BenchOptPaddingV = ZinelyV21Dimens.gapMd
internal val BenchOptGapInner = ZinelyV21Dimens.gapMd

/**
 * Frozen `.opt{border-radius:var(--br-md)}` (`v21-bench.html:372`) — 14, where V2 drew 13.
 *
 * A real radius rather than a pill: `.opt` is a **card**, not a chrome control, and V2.1 keeps that
 * distinction. The shape is named because the press shadow and the clip both have to build from the same
 * outline.
 */
internal val BenchOptRadius = ZinelyV21Dimens.radiusMd
internal val BenchOptShape: RoundedCornerShape = RoundedCornerShape(BenchOptRadius)

/** Frozen `.opt`/`.opt .ico{border:1.5px solid var(--ink)}` (`v21-bench.html:372`, `:376`). V2 drew a 1dp `--chrome-line`. */
internal val BenchOptBorder = 1.5.dp

/**
 * Frozen `.opt .ico{width:40px;height:40px;border-radius:var(--br-sm)}` (`v21-bench.html:375`) — 40 on an
 * 8dp radius, where V2 drew 38 on 10.
 */
internal val BenchOptIcoSize = 40.dp
internal val BenchOptIcoRadius = ZinelyV21Dimens.radiusSm

/** Frozen `.opt .ico svg{width:20px;height:20px;stroke-width:1.8}` (`v21-bench.html:377`). V2 drew 19 at 1.7. */
internal val BenchOptGlyphSize = 20.dp
internal const val BenchOptStroke: Float = 1.8f

/** A15/A16's distinct chooser marks, named here so a mapping regression is testable without pixels. */
internal val BenchAddPhotoGlyph = ZinelyV2Icons.Art
internal val BenchAddArtGlyph = ZinelyV2Icons.Collage

/** Frozen `.opt .tx{gap:var(--gap-hair)}` (`v21-bench.html:379`) — between the title and its subtitle. */
internal val BenchOptTextGap = ZinelyV21Dimens.gapHair

/**
 * Frozen `.opt .tx b{font-family:var(--voice);font-weight:700;font-size:1rem}` (`v21-bench.html:380`).
 *
 * The title moved from the sans face to the **voice** face and from 14.5sp/600 to a real 16sp Bold: the row
 * now names its medium in the same hand the sheet titles itself in.
 */
internal val BenchOptTitleSize = 16.sp

/** Frozen `.opt .tx span{font-size:.73rem;color:var(--ink-soft)}` (`v21-bench.html:381`) — 11.68sp. */
internal val BenchOptSubtitleSize = 11.68.sp

/**
 * The frozen **Add chooser** — `openSupply()`'s sheet (`v21-bench.html:833-847`, CSS `:435-446` — ⚠ these
 * addresses were re-verified 2026-08-16 after ADR-104's amendment shifted the file by ~+49 lines; the
 * previous citations pointed at `toast()`), with two
 * of its three rows; [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.4a–4.4d, re-skinned
 * to V2.1 by [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102) package P4.
 *
 * ### Why three rows now, when two was right before
 *
 * The freeze narrates its own intent at `v21-bench.html:846` — *"Add stays three verbs — Text · Photo ·
 * Art."* [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling) released **only Text
 * and Photo** into C4, and this file held `Art` back on a reason that was never "the freeze doesn't ask for
 * it": *a control that reports truthfully and then does nothing when tapped invites the press harder than a
 * blank one does* (C3's Pass 2). A row that led nowhere would have been that control, so **absent, not
 * disabled** was the honest state while `DecorElement` did not exist.
 *
 * **That reason has expired, and it expired on the merits rather than on a schedule.** `DecorElement` is in
 * the model, `DrawShape` is emitted by `SceneRenderer` and painted by `CanvasReplayer`, `Intent.PlaceSupply`
 * places one, and [BenchArtSheet] is the frozen cabinet. Tapping `Art` now opens a drawer a maker can take
 * something out of — the row is live, not truthful-and-inert, so the sentence that kept it out no longer
 * describes it.
 *
 * ### The part-authored-catalogue question, argued rather than assumed
 *
 * Some supplies have no authored outline and are drawn inert inside the sheet ([BenchArtSheet], OD-9) —
 * twelve of sixteen when this was written, four as of 2026-08-18. It is fair to ask whether the row should
 * wait for them. It should not, for three reasons and one that decides it, **none of which turn on the
 * count** — which is why shrinking twelve to four changed nothing here:
 *
 *  - The inertness is **one level down**. This row's promise is *"tape, stamps and cut paper live here"*,
 *    and that is true: the sheet shows all sixteen under §4's four headings, names each one, and says
 *    *"Not yet"* on the ones it cannot lay down. Nothing here over-promises.
 *  - Withholding it makes the ones that *do* work unreachable — the placement path would ship with no
 *    production call site at all, which is the state this package exists to end.
 *  - The alternative failure is worse and less recoverable: a maker who never learns the drawer exists
 *    cannot ask for the ones that are still missing.
 *
 * ⚠ What is **not** settled here is whether a dim tile among live ones is an acceptable first
 * impression. [BenchArtSheet] already flags that for Pass 2, and adding this row is what finally puts a
 * first-time maker in front of it. If Pass 2 rejects the sheet, this row is the fence to re-raise.
 *
 * ### Photo and Art scan as different materials
 *
 * A15/A16 amended the frozen chooser before this implementation: `Photo` keeps the frame-and-horizon mark,
 * while `Art` gets a collage of two overlapping paper scraps and one printed dot. D-094 records the owner
 * ruling. Both rows keep their full visible labels and single-node accessibility descriptions, so the icon
 * is a non-colour scanning cue rather than the only way to tell the actions apart.
 *
 * ### Why the sheet is [ZSheet] and not a new surface
 *
 * OD-21 says so in terms — *"the chooser continues to use the existing `ZSheet` implementation"* — and the
 * shipped component already carries the frozen sheet's shape: the 22dp top radius, the grip, the serif
 * title, the `translateY` entrance and the scrim. Six production call sites depend on it. Building a second
 * one would be the duplication this phase keeps removing.
 *
 * ### Both destinations are the shipped ones
 *
 * `Text` dispatches through [addTextAndEdit], which places the box **and opens the C3 session on it** — the
 * freeze says the same thing in its own narration (*"a new text block drops in ready to edit"*),
 * so the frozen design and the shipped flow already agreed before this package existed. OD-21 requires that
 * reuse **by name**, precisely so C3's in-place editing model survives C4 untouched. `Photo` dispatches
 * `Intent.RequestAddImage`, the same intent the retired shelf sent to the same picker. `Art` hands off to
 * [BenchArtSheet], which is the frozen `openArt()`.
 *
 * @param visible whether the sheet is open.
 * @param onAddArt open the Art sheet. Like the other two it is called **after** [onDismiss], so exactly one
 *   sheet is on screen at a time — the freeze swaps `#sheet`'s own `innerHTML` and so cannot make the
 *   mistake two Compose `Dialog`s can.
 */
@Composable
internal fun BenchAddChooser(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddText: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddArt: () -> Unit,
) {
    ZSheet(visible = visible, onDismiss = onDismiss, title = BenchAddChooserTitle) {
        BenchAddChooserBody(
            onAddText = { onDismiss(); onAddText() },
            onAddPhoto = { onDismiss(); onAddPhoto() },
            onAddArt = { onDismiss(); onAddArt() },
        )
    }
}

/**
 * The chooser rows without their window host. Production's Add → Art path reuses one [ZSheet] and swaps
 * this body for [BenchArtSheetBody], matching the frozen `#sheet.innerHTML` transition and avoiding a
 * second platform-window cold start. The standalone [BenchAddChooser] remains the focused component seam.
 */
@Composable
internal fun BenchAddChooserBody(
    onAddText: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddArt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(BenchAddChooserTestTag),
        verticalArrangement = Arrangement.spacedBy(BenchOptGap),
    ) {
        BenchAddOption(
            icon = ZinelyV2Icons.Font.toImageVector(
                BenchOptGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchOptStroke),
            ),
            title = BenchAddTextTitle,
            subtitle = BenchAddTextSubtitle,
            testTag = BenchAddChooserTextTag,
            onClick = onAddText,
        )
        // `ICON_PICTURE` — the frame-plus-horizon mark. Its catalogue name predates the A15/A16
        // distinction; in this chooser it is the frozen Photo mark.
        BenchAddOption(
            icon = BenchAddPhotoGlyph.toImageVector(
                BenchOptGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchOptStroke),
            ),
            title = BenchAddPhotoTitle,
            subtitle = BenchAddPhotoSubtitle,
            testTag = BenchAddChooserPhotoTag,
            onClick = onAddPhoto,
        )
        // A15/A16's owner-approved collage: two paper scraps plus one printed dot, visibly distinct
        // from Photo without changing the row's label, target, focus order, or semantics.
        BenchAddOption(
            icon = BenchAddArtGlyph.toImageVector(
                BenchOptGlyphSize,
                ZinelyV2IconPaint.Stroke(BenchOptStroke),
            ),
            title = BenchAddArtTitle,
            subtitle = BenchAddArtSubtitle,
            testTag = BenchAddChooserArtTag,
            onClick = onAddArt,
        )
    }
}

/**
 * Frozen `.opt` (`v21-bench.html:371-381`) — a `paper` card outlined in real ink, standing on
 * [ZinelyV21Press.Raised]'s 3dp printed shadow, with a butter-tinted glyph tile and a two-line label.
 *
 * ### Three V2 habits this sheds
 *
 * The V2 row was a 1dp `--chrome-line` outline **over nothing** — the sheet showed through it — with a
 * `--matcha`-tinted tile and no depth at all. All three are gone: the card has its own `paper` ground, the
 * outline is ink at the language's 1.5dp pen, and `.opt:active{transform:translate(2px,2px);box-shadow:1px
 * 1px 0}` is exactly [ZinelyV21Press.Raised], which the corpus already assigns to `.opt` by name.
 *
 * The tile's `butterTint` is **material, not state**: it is the colour of the paper the glyph is printed
 * on, and the 1.5dp ink ring around it is what carries any meaning. That is the V21-SPEC §3.2 reading, and
 * it is why the tile is not `leaf` or `berry` — nothing about `Text` is more affirmative than `Photo`.
 *
 * It wears **no** `--frame` ring. The ring is one-per-screen and the Bench spends it on `.add`
 * ([BenchBottomBar]); a second one here would make both decoration.
 *
 * The row is one accessibility node, not three: the title, the subtitle and the tile are one control, and
 * TalkBack should offer one target reading *"Text. Type words onto the page."* rather than three fragments
 * a user has to reassemble.
 */
@Composable
private fun BenchAddOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val choose = benchTap(action = onClick)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // ⚠ Nothing that clips may sit to the LEFT of the press — the shadow paints up to 3dp outside
            // the node's own bounds, and a clip above it cuts the shadow off. The `clip` is downstream on
            // purpose. The sheet's own bottom padding is what gives the shadow room to land.
            .zinelyV21Pressable(pressed, ZinelyV21Press.Raised, colors.inkLine, BenchOptShape)
            .clip(BenchOptShape)
            .background(colors.surface)
            .border(BenchOptBorder, colors.ink, BenchOptShape)
            .clickable(interactionSource = interaction, indication = null, onClick = choose)
            .testTag(testTag)
            .clearAndSetSemantics {
                contentDescription = Copy.AddChooser.optionLabel(title, subtitle)
                role = Role.Button
                onClick { choose(); true }
            }
            .padding(horizontal = BenchOptPaddingH, vertical = BenchOptPaddingV),
        horizontalArrangement = Arrangement.spacedBy(BenchOptGapInner),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BenchOptIcoSize)
                .clip(RoundedCornerShape(BenchOptIcoRadius))
                .background(colors.butterTint)
                .border(BenchOptBorder, colors.ink, RoundedCornerShape(BenchOptIcoRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                // `.opt .ico{color:var(--ink-soft)}` — the glyph inherits it through `currentColor`.
                tint = colors.inkSoft,
                modifier = Modifier.size(BenchOptGlyphSize),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(BenchOptTextGap)) {
            Text(
                text = title,
                color = colors.ink,
                fontSize = BenchOptTitleSize,
                // `.opt .tx b{font-weight:700}` in `--voice` — a real Bold in the display face.
                fontWeight = FontWeight.Bold,
                fontFamily = ZinelyV21Fonts.Voice,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            )
            Text(
                text = subtitle,
                color = colors.inkSoft,
                fontSize = BenchOptSubtitleSize,
                // `.opt{font-family:var(--sans)}` — the subtitle inherits the row's own face.
                fontFamily = ZinelyV21Fonts.Work,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            )
        }
    }
}
