package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/** Test tag on the empty-state container. */
public const val EditorEmptyStateTestTag: String = "editor-empty-state"

/** The two add-action labels — owned by the supply tray (the single action home, [ADR-033]). Shared as
 *  the tray's visible + spoken labels; the empty state references them only to *name* what's below. */
public const val AddPhotoActionLabel: String = Copy.EmptyState.ADD_A_PHOTO
public const val AddWordsActionLabel: String = Copy.EmptyState.ADD_WORDS

/** Test tag on the decorative downward cue that ties the invitation to the supply shelf below. */
public const val EmptyStateTrayCueTag: String = "empty-state-tray-cue"

/** Headline for the **first** blank page — the warm welcome (VOICE empty states). */
public const val FirstPageInvitationHeadline: String = Copy.EmptyState.FIRST_PAGE_HEADLINE

/** Headline for a **later** blank page — the lighter "fresh page" variant (VOICE empty states). */
public const val LaterPageInvitationHeadline: String = Copy.EmptyState.LATER_PAGE_HEADLINE

/**
 * The cozy first-run invitation — shown on the canvas when the current page has no elements. It turns a
 * blank sheet (which reads as a void) into an encouraging "let's make something cute" prompt, so a
 * first-time user feels invited rather than faced with a void.
 *
 * ### ⚠ Unfrozen surface — the analogy is the Library's `.empty`
 *
 * `v21-bench.html` draws **no empty page**: the prototype's page always carries three elements, so the
 * Bench freeze specifies nothing for this state. The nearest frozen surface is
 * **`v21-library.html .empty`** — already implemented as
 * [ZineShelfEmpty][com.aritr.zinely.feature.library.ZineShelfEmpty] — and it is the right analogy for the
 * strongest possible reason: it is the *same moment*. Both screens have a container with nothing in it and
 * must say so without reading as loss, and both answer with the same four-part column — an illustration of
 * the thing that is missing, a `--voice` headline, a `--ink-soft` sentence bounded in `ch`, and a
 * `leaf-tint` pill carrying the privacy promise. Copying its grammar is what makes the shelf and the page
 * read as one product rather than two apps that both happen to be empty.
 *
 * Every value below is therefore transcribed from `v21-library.html:284-307`, with **two** adaptations,
 * both named where they occur: the box padding (there is no dock to clear here) and the illustration
 * (the Library illustrates *sheets becoming a book*; a blank page illustrates *supplies*).
 *
 * **Invitation-only — no buttons ([ADR-033](../DECISIONS.md#adr-033)).** The add action lives solely in
 * the always-visible bottom bar (the thumb-zone home), so no add verb ever appears twice at once on a
 * blank page. Since [OD-21](../design/V2-SPEC-DEFECTS.md#d-047-ruling) that bar draws a single `Add` and
 * the two verbs live in the chooser it opens. This surface only *invites and orients*.
 *
 * **It wears no `--frame` ring**, and that is the rule rather than an oversight: the ring is one per
 * screen and the Bench spends it on `.add` ([BenchBottomBar]). The Library's `.empty` *does* wear one,
 * because there the empty state's `.start` **is** the screen's single primary action. Here the primary
 * action is the bar underneath, still on screen, still ringed. A second ring would make both decoration.
 *
 * Accessibility: this overlay is non-interactive text + ornament — the sticker cluster is not announced,
 * and the actionable, labelled controls are the bar's (each a ≥48dp `Role.Button`). The headline carries
 * `heading()` because the analogous `.empty h2` is an `<h2>`: transcription of the frozen markup, and a
 * TalkBack landmark the V2 implementation dropped.
 *
 * The headline follows the page's position (VOICE empty states): the **first** page keeps the warm
 * welcome ([FirstPageInvitationHeadline]); a **later** blank page uses the lighter "fresh page" variant
 * ([LaterPageInvitationHeadline]). Only the headline changes.
 *
 * Its subcopy still says *"from the supplies below"* and its cue still points at the retired shelf —
 * [D-050](../design/V2-SPEC-DEFECTS.md#d-050), deliberately open: the wording is the owner's.
 *
 * @param modifier sizing/placement applied by the host (typically centered over the page).
 * @param firstPage whether the current page is the first page of the zine — selects the headline copy.
 *   Defaults to `true` (the welcoming line) for standalone previews/tests.
 */
@Composable
public fun EditorEmptyState(
    modifier: Modifier = Modifier,
    firstPage: Boolean = true,
) {
    val colors = ZinelyTheme.v21Colors
    Column(
        modifier = modifier
            .testTag(EditorEmptyStateTestTag)
            // ⚠ Adaptation 1. `.empty{padding:var(--gap-2xl) var(--gap-2xl) 150px}` — the 150px is the
            // Library's dock clearance and the 36px sides are a full-screen shelf's. This overlay sits on
            // the *page*, which is already inset by `.content{inset:14px}` and is 266px wide in the
            // prototype; 36px of further inset on each side would leave the 29ch sentence nowhere to go.
            // `--gap-xl` is the frozen scale's next step down and is also the 24dp the V2 build used, so
            // the box is unchanged in size and is now expressed in the V2.1 scale.
            .padding(ZinelyV21Dimens.gapXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `.empty{gap:var(--gap-md)}` — 12dp, which V2 already spent here.
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
    ) {
        SupplyCluster()

        // `.empty h2{font-family:var(--voice);font-size:1.75rem;font-weight:700;line-height:1.12;
        //   margin:var(--gap-xs) 0 0}`. Averia 700 — the only weight above 400 the bundled voice face has.
        Text(
            text = if (firstPage) FirstPageInvitationHeadline else LaterPageInvitationHeadline,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = HeadlineSize,
                lineHeight = HeadlineLineHeight,
                color = colors.ink,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = HeadlineMarginTop)
                .semantics { heading() },
        )

        // `.empty p{margin:0;color:var(--ink-soft);max-width:29ch;line-height:1.55;font-size:.94rem}`.
        // Names the two ways to start AND points to where they live, so the button-less invitation still
        // answers "what do I do next?" without re-presenting the actions.
        val bodyStyle = TextStyle(
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = BodySize,
            lineHeight = BodyLineHeight,
            color = colors.inkSoft,
        )
        Text(
            text = Copy.EmptyState.SUPPLY_CUE,
            style = bodyStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = measureCharacters(BodyMaxCharacters, bodyStyle)),
        )

        // Orientation cue: a subtle downward chevron beneath the invitation, pointing the eye to where the
        // add actions live. It is a flourish that also does a job — purely static (no motion, so the
        // reduced-motion path is a no-op) and `clearAndSetSemantics` strips it from the a11y tree, so it
        // adds no screen-reader noise.
        //
        // Its ink moved from `inkSoft` at 70% to plain `inkSoft`. An alpha'd `inkSoft` lands on top of
        // `inkFaint`, and §4.1 forbids `inkFaint` from setting anything a reader has to see; a hand-mixed
        // approximation of a banned token is the same defect with a longer spelling.
        Box(modifier = Modifier.testTag(EmptyStateTrayCueTag)) {
            Text(
                text = TrayCueGlyph,
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = TrayCueSize,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.inkSoft,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/**
 * ⚠ **Adaptation 2 — the illustration.** `.tf` illustrates the Library's own promise (loose sheets → a
 * folded book) and would be a lie on a blank page, which is about *supplies*, not about binding. So the
 * V2 sticker cluster's **shape is kept** — three tilted craft marks, the same three glyphs, the same three
 * tilts — and only its ink is re-mixed. That is the smallest change that removes V1's `teal`/`coral` (a
 * palette V2.1 does not publish and cannot be mapped into) without inventing a new illustration after a
 * freeze, which is what *"do not invent new visual ideas"* rules out.
 *
 * Each card is `.sheet-ill`/`.book-ill`'s recipe verbatim (`v21-library.html:292-301`): a 1.5dp `ink`
 * border, a 3dp hard shadow in `inkLine`, a small rotation, over a ground the frozen corpus already uses
 * as a fill — `paper`, `leaf` (`.book-ill`), and `berryTint` (`v21-bench.html:211 .photo`). Every glyph is
 * on a measured pairing: `ink` on `paper`, `onLeaf` on `leaf`, `ink` on `berryTint`. The V2 cards drew
 * white glyphs on mid-tone fills, which is where their contrast went.
 *
 * Ornamental only, and silent.
 *
 * ⚠ **It was not silent, and the KDoc that said so was wrong for a month.** "Inside a non-interactive
 * text column" is not the same claim as "absent from the a11y tree", and only the first was ever true:
 * each [CraftCard]'s glyph is a `Text`, every `Text` contributes a `TextView` to the *platform*
 * `AccessibilityNodeInfo` tree, and TalkBack duly read `"✿"`, `"❀"`, `"★"` — three characters with no
 * spoken name — before the headline. No Compose semantics test could see it: they assert against the
 * merged semantics tree, where an unmerged decorative `Text` is unremarkable. Found by
 * `adb shell uiautomator dump` on `RZCYA1VBQ2H`, which is the only reader that agrees with TalkBack.
 * `clearAndSetSemantics {}` on the [Row] silences the cluster whole — the same treatment the tray cue
 * below already carried, and the reason it never had this bug.
 */
@Composable
private fun SupplyCluster() {
    val colors = ZinelyTheme.v21Colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        modifier = Modifier.clearAndSetSemantics { },
    ) {
        CraftCard(colors.paper, colors.ink, tilt = -9f, glyph = "✿")
        CraftCard(colors.leaf, colors.onLeaf, tilt = 6f, glyph = "❀")
        CraftCard(colors.berryTint, colors.ink, tilt = 12f, glyph = "★")
    }
}

/** One craft mark — a tilted card on the frozen illustration recipe, with a centred glyph. */
@Composable
private fun CraftCard(ground: Color, mark: Color, tilt: Float, glyph: String) {
    val colors = ZinelyTheme.v21Colors
    Box(
        modifier = Modifier
            .size(CardSize)
            .graphicsLayer { rotationZ = tilt }
            // ⚠ Nothing that clips may sit LEFT of the shadow — it paints outside the node's own bounds.
            .zinelyV21HardShadow(CardShadow, colors.inkLine, CardShape)
            .clip(CardShape)
            .background(ground)
            .border(CardBorder, colors.ink, CardShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = CardGlyphSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = mark,
            ),
        )
    }
}

/**
 * `max-width:29ch` — the advance width of 29 `0` glyphs in the paragraph's own style, which is what the
 * CSS `ch` unit measures. [ZineShelfEmpty][com.aritr.zinely.feature.library.ZineShelfEmpty] measures its
 * own the same way and for the same reason.
 */
@Composable
private fun measureCharacters(count: Int, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style, count) {
        val zeros = measurer.measure("0".repeat(count), style, softWrap = false)
        with(density) { zeros.size.width.toDp() }
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html` `.empty` — see the KDoc for why that file.
// ---------------------------------------------------------------------------------------------

/** `.empty h2{font-size:1.75rem;line-height:1.12;margin:var(--gap-xs) 0 0}` = 28px over 31.36px. */
private val HeadlineSize = 28.sp
private val HeadlineLineHeight = 1.12.em
private val HeadlineMarginTop = ZinelyV21Dimens.gapXs

/** `.empty p{font-size:.94rem;line-height:1.55;max-width:29ch}` = 15.04px. */
private val BodySize = 15.04.sp
private val BodyLineHeight = 1.55.em
private const val BodyMaxCharacters = 29

/** `.empty .pv{font-size:.75rem;font-weight:600;margin-top:var(--gap-xs)}` = 12px. */

/**
 * `.sheet-ill{border:1.5px solid var(--ink);box-shadow:3px 3px 0 var(--ink-line)}` at the V2 cluster's own
 * 48dp box and `--br-md` corner, both of which V2.1's scale already names.
 */
private val CardSize = 48.dp
private val CardBorder = 1.5.dp
private val CardShadow = 3.dp
private val CardShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
private val CardGlyphSize = 20.sp

/** The orientation chevron (U+2304), unchanged from V2 — [D-050] leaves its fate to the owner. */
private const val TrayCueGlyph = "⌄"
private val TrayCueSize = 20.sp
