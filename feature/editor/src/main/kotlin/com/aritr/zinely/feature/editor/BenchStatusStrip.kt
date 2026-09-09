package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.toImageVector

/** Test tag on the frozen top status strip (`.status`). */
public const val BenchStatusStripTestTag: String = "bench-status-strip"

/** Test tag on the autosave chip inside it (`.saved`). */
public const val BenchSavedChipTestTag: String = "bench-saved-chip"

/**
 * ⚠ **V2.1's `.status` declares no height** (`v21-bench.html:165-166`), where V2 pinned `height:26px`.
 * `flex:none` survives, so the strip is exactly its padding plus its tallest child and never takes the
 * residual column space. Re-pinning a height would make the freeze's generous new padding decorative —
 * the same reasoning [BenchBarGap]'s KDoc records for `.bar`.
 */
internal val BenchStatusPaddingTop = ZinelyV21Dimens.gapLg
internal val BenchStatusPaddingH = ZinelyV21Dimens.gapLg
internal val BenchStatusPaddingBottom = ZinelyV21Dimens.gapXs

// ⚠ `.status{font-size:.7rem;font-weight:600;color:var(--ink-soft)}` is deliberately **not** transcribed
// as a constant: the strip's only child that draws text is `.saved`, which overrides all three. V2 kept a
// `BenchStatusTextSize` that nothing could ever have applied — a frozen value with no consumer reads as
// coverage and is not.

/** Frozen `.saved{gap:var(--gap-xs)}` (`v21-bench.html:167`) — 4dp, where V2 asked for 5. */
internal val BenchSavedGap = ZinelyV21Dimens.gapXs

/**
 * Frozen `.saved{font-size:.68rem;font-weight:700;letter-spacing:.09em;text-transform:uppercase}`
 * (`v21-bench.html:167-168`) = 10.88px.
 *
 * V2 set the chip as sentence-case running text with one bold word inside it. V2.1 sets the whole chip as
 * a small uppercase **label**, which is why the `SpanStyle` that used to bold `Saved` alone is gone: there
 * is no longer a lighter half for it to stand against.
 */
internal val BenchSavedTextSize = 10.88.sp
internal val BenchSavedTracking = 0.09.em

/** Frozen `.saved{border:1.5px solid var(--hair);border-radius:var(--br-pill)}` (`v21-bench.html:169`). */
internal val BenchSavedBorder = 1.5.dp
internal val BenchSavedShape: Shape = RoundedCornerShape(percent = 50)

/** Frozen `.saved{padding:var(--gap-xs) var(--gap-sm);transform:rotate(-1deg)}` (`v21-bench.html:170`). */
internal val BenchSavedPaddingV = ZinelyV21Dimens.gapXs
internal val BenchSavedPaddingH = ZinelyV21Dimens.gapSm
internal const val BenchSavedRotation: Float = -1f

/** Frozen `.saved svg{width:12px;height:12px;stroke-width:3}` (`v21-bench.html:171`). */
internal val BenchSavedGlyphSize = 12.dp
internal const val BenchSavedStroke: Float = 3f

/**
 * ⚠ **V2.1's `.saved` declares no `transition`** — the prototype's chip is permanent, so it never had one
 * to declare. The product's chip is *transient*, and a save confirmation that blinks out of existence is a
 * worse thing than the freeze is silent about; the fade is therefore **retained behaviour** from V2
 * (`v2-bench.html:191`), not transcribed geometry. Capability, which
 * [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling) protects — not appearance,
 * which the freeze owns.
 */
internal const val BenchSavedFadeMillis: Int = 400

/**
 * How long the confirmation stays up before it dismisses itself (ms) — the host owns the timer, this is
 * the shared constant the host and its tests agree on.
 *
 * **1600ms was already the shipped value** before C4 touched it, and it is also the frozen one
 * (`flashSaved()`, `v2-bench.html:501`): the two agreed, so [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094)
 * row 4.10 changes the presentation and not the window. It moved here from the retired
 * `EditorSavedConfirmation`, keeping its name so every existing caller and test reads unchanged.
 */
public const val SavedConfirmationVisibleMs: Long = 1600L

/**
 * The frozen autosave word (`v21-bench.html:526`), verbatim: `<span class="saved"><svg …/>Saved</span>`.
 *
 * ⚠ **V2's flower and qualifier are gone from the paint.** V2's chip read `✿ **Saved** · on this device`;
 * V2.1's markup carries a drawn **check** and the single word `Saved`, uppercased by
 * `text-transform`. The `✿` was decoration that D-021 kept only because the frozen file drew it — and
 * V2.1's frozen file does not — so keeping it would now be the invention, not the transcription. The
 * qualifier survives where it always did the work: [BenchSavedSpokenLabel] still says *"Saved on this
 * device"*, and the shelf's own privacy pill (`.empty .pv`, [EditorEmptyState]) states the promise in
 * words. Flagged for the owner rather than quietly absorbed: this is the freeze removing drawn copy, and
 * `the_saved_chip_no_longer_paints_the_flower_or_the_qualifier` asserts the absence so it cannot creep back.
 */
public const val BenchSavedWord: String = Copy.Status.SAVED_WORD

/**
 * The spoken form. The chip's drawn word is one uppercase label; the live region announces the sentence,
 * including the *on this device* the paint no longer carries — the same split the retired
 * `EditorSavedConfirmation` made between its `"Saved ✨"` and the `"Saved"` it spoke.
 */
public const val BenchSavedSpokenLabel: String = Copy.Status.SAVED_SPOKEN

/**
 * The frozen top status strip — `.status` and its `.saved` chip (`v21-bench.html:165-172`, markup `:514`);
 * [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.9 and 4.10, re-skinned to V2.1 under
 * [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102).
 *
 * ### What replaces `"the bench"`
 *
 * Nothing, and that is a decision rather than an omission. The freeze's left slot reads `the bench`, which is
 * **prototype narration** of exactly the same class as `cap()` — it names the screen to someone reading a
 * design file, not to someone making a zine. The obvious product substitute would be the project's title, and
 * `EditorUiState` (`core/editor/…/EditorModel.kt:110`) does not carry one: no title reaches the editor at all.
 * Inventing a route for one would be new capability, which
 * [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) forbids in the same breath as it
 * keeps redo. So the slot is **empty**, which removes nothing and invents nothing — OD-9's own formula — and
 * the strip keeps its frozen padding and two-slot shape so the chip lands exactly where the freeze puts it.
 *
 * ### The chip is now an object, not coloured text
 *
 * V2 drew the confirmation as `--matcha-text` on the bare chrome. V2.1 gives it a **body**: `leafTint`
 * ground, a 1.5dp `hair` rule, pill radius, and a 1° lean. That is the same move `.empty .pv` makes on the
 * Library ([ZineShelfEmpty][com.aritr.zinely.feature.library.ZineShelfEmpty]) — *information the screen
 * wants read is given an object to sit in rather than a colour to be noticed by* — so the two screens now
 * say "this is safe" in one shared shape.
 *
 * The border is **`hair`, not `ink`**. That is not a violation of *drawn line = ink*: `hair` is the frozen
 * file's own token here, and it has to be, because a full-strength ink rule around a 20dp chip in the
 * quietest strip on the screen would outweigh the page beneath it. The rule the corpus keeps is that a
 * border contrasts with what it sits on; `hair` is ink at 16% and does.
 *
 * ### Why this replaces `EditorSavedConfirmation` rather than joining it
 *
 * Both say *your work is saved*. Two simultaneous presentations of one message is
 * [OD-14](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-039-ruling)'s defect, and the freeze
 * assigns the message to this strip. The **capability** — a transient, earned reassurance driven by a real
 * autosave event — is unchanged, so [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling)
 * is satisfied: only the presentation moves. [SavedConfirmationVisibleMs] stays the shared 1600ms window.
 *
 * @param savedVisible the host's existing autosave flag — the same one the retired chip took.
 */
@Composable
internal fun BenchStatusStrip(
    savedVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val alpha by animateFloatAsState(
        targetValue = if (savedVisible) 1f else 0f,
        animationSpec = ZinelyTheme.v2Motion.standard(BenchSavedFadeMillis),
        label = "bench-saved-fade",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(BenchStatusStripTestTag)
            .padding(
                top = BenchStatusPaddingTop,
                start = BenchStatusPaddingH,
                end = BenchStatusPaddingH,
                bottom = BenchStatusPaddingBottom,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The frozen left slot, deliberately empty — see the KDoc. It is still a slot, because
        // `justify-content:space-between` on two children is what pins the chip to the end; dropping the
        // child would leave the chip start-aligned and the strip would no longer be the frozen shape.
        Box(Modifier)
        // Composed only while it has something to say. An alpha of 0 hides pixels, not nodes: left
        // permanently composed, the chip stays in the platform accessibility tree announcing a save that
        // has not happened — the exact defect C3's style row shipped once and `SurfaceTraversalOrderTest`
        // caught by reading the platform tree rather than a screenshot.
        //
        // The condition is `savedVisible || alpha > 0f`, not `alpha > 0f` alone, and both halves are load-
        // bearing: on the frame the save signal lands the flag is already true while the animation has not
        // ticked yet (alpha is still exactly 0), so alpha alone would withhold the chip for a frame — it
        // did, and `a_save_signal_shows_the_saved_confirmation` caught it. The alpha half then keeps the
        // node alive through the fade-*out*, after the flag has gone false, so the chip leaves by fading
        // rather than vanishing.
        if (savedVisible || alpha > 0f) {
            Row(
                modifier = Modifier
                    .testTag(BenchSavedChipTestTag)
                    // Opacity and lean on one layer: the frozen `transform:rotate(-1deg)` and the retained
                    // fade. Nothing downstream clips a shadow here — `.saved` casts none.
                    .graphicsLayer {
                        this.alpha = alpha
                        rotationZ = BenchSavedRotation
                    }
                    .clip(BenchSavedShape)
                    .background(colors.leafTint)
                    .border(BenchSavedBorder, colors.hair, BenchSavedShape)
                    .padding(
                        horizontal = BenchSavedPaddingH,
                        vertical = BenchSavedPaddingV,
                    )
                    .clearAndSetSemantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = BenchSavedSpokenLabel
                    },
                horizontalArrangement = Arrangement.spacedBy(BenchSavedGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // `.saved svg` is `M4 12l5 5 11-12`; [ZinelyV2Icons.Done] is `M20 6 9 17l-5-5` — the same
                // mark, drawn from the other end. The frozen path is not in the icon set and adding it
                // would edit `core:ui`, which this package does not touch; the substitution is a stroke
                // check at the frozen 12dp/3.0, and it is flagged here rather than left to be discovered.
                Icon(
                    imageVector = ZinelyV2Icons.Done.toImageVector(
                        BenchSavedGlyphSize,
                        ZinelyV2IconPaint.Stroke(BenchSavedStroke),
                    ),
                    contentDescription = null,
                    // `stroke:currentColor` — the chip's own ground-aware `--on-leaf`.
                    tint = colors.onLeaf,
                    modifier = Modifier.size(BenchSavedGlyphSize),
                )
                Text(
                    // `text-transform:uppercase`. "Saved" carries no dotted `i`, so the default-locale
                    // fold is the ROOT fold and needs no `Locale` argument to be correct.
                    text = BenchSavedWord.uppercase(),
                    color = colors.onLeaf,
                    fontSize = BenchSavedTextSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = BenchSavedTracking,
                    fontFamily = ZinelyV21Fonts.Work,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                )
            }
        }
    }
}
