package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the frozen top status strip (`.status`). */
public const val BenchStatusStripTestTag: String = "bench-status-strip"

/** Test tag on the autosave chip inside it (`.saved`). */
public const val BenchSavedChipTestTag: String = "bench-saved-chip"

/** Frozen `.status{height:26px}` (`v2-bench.html:190`). */
internal val BenchStatusHeight = 26.dp

/** Frozen `.status{padding:0 20px}` (`v2-bench.html:190`). */
internal val BenchStatusPaddingH = 20.dp

/** Frozen `.status{font-size:11px}` (`v2-bench.html:190`). */
internal val BenchStatusTextSize = 11.sp

/** Frozen `.saved{gap:5px}` (`v2-bench.html:191`). */
internal val BenchSavedGap = 5.dp

/** Frozen `.saved{transition:opacity .4s var(--standard)}` (`v2-bench.html:191`). */
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
 * The frozen autosave line (`v2-bench.html:390`), verbatim.
 *
 * `✿` is U+273F and the bundled faces do not all carry it.
 * [D-021](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-021-ruling) already ruled that question
 * for the whole programme — *"keep the literal characters exactly as defined by the frozen HTML… bundled-font
 * coverage does not justify changing the design. Platform fallback is acceptable."* So it is transcribed, not
 * substituted, and the device pass reads what the platform actually draws.
 */
public const val BenchSavedMark: String = Copy.Status.SAVED_MARK
public const val BenchSavedWord: String = Copy.Status.SAVED_WORD
public const val BenchSavedQualifier: String = Copy.Status.SAVED_QUALIFIER

/**
 * The spoken form. The flower is decoration and is never required to parse the meaning (VOICE rule 7), so
 * the live region announces the sentence without it — the same split the retired `EditorSavedConfirmation`
 * made between its `"Saved ✨"` and the `"Saved"` it spoke.
 */
public const val BenchSavedSpokenLabel: String = Copy.Status.SAVED_SPOKEN

/**
 * The frozen top status strip — `.status` and its `.saved` chip (`v2-bench.html:190-192`, markup `:390`);
 * [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.9 and 4.10.
 *
 * ### What replaces `"the bench"`
 *
 * Nothing, and that is a decision rather than an omission. The freeze's left slot reads `the bench`, which is
 * **prototype narration** of exactly the same class as `cap()` at `v2-bench.html:501` — it names the screen
 * to someone reading a design file, not to someone making a zine. The obvious product substitute would be the
 * project's title, and `EditorUiState` (`core/editor/…/EditorModel.kt:110`) does not carry one: no title
 * reaches the editor at all. Inventing a route for one would be new capability, which
 * [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) forbids in the same breath as it
 * keeps redo. So the slot is **empty**, which removes nothing and invents nothing — OD-9's own formula — and
 * the strip keeps its frozen height, padding and two-slot shape so the chip lands exactly where the freeze
 * puts it. Recorded in ADR-094 rather than left to this comment.
 *
 * ### Why this replaces `EditorSavedConfirmation` rather than joining it
 *
 * Both say *your work is saved*. Two simultaneous presentations of one message is
 * [OD-14](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-039-ruling)'s defect, and the freeze
 * assigns the message to this strip. The **capability** — a transient, earned reassurance driven by a real
 * autosave event — is unchanged, so [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling)
 * is satisfied: only the presentation moves. [SavedConfirmationVisibleMs] stays the shared 1600ms window, and
 * it already matched the freeze before this package touched it. The old composable is **retired**, not left
 * beside this one: two surfaces for one message is OD-14's defect whether or not both are on screen at once.
 *
 * @param savedVisible the host's existing autosave flag — the same one the retired chip took.
 */
@Composable
internal fun BenchStatusStrip(
    savedVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    val alpha by animateFloatAsState(
        targetValue = if (savedVisible) 1f else 0f,
        animationSpec = ZinelyTheme.v2Motion.standard(BenchSavedFadeMillis),
        label = "bench-saved-fade",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BenchStatusHeight)
            .testTag(BenchStatusStripTestTag)
            .padding(horizontal = BenchStatusPaddingH),
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
        // caught by reading the platform tree rather than a screenshot. The animation state lives on the
        // outer `animateFloatAsState`, so the fade still runs from and to the right value.
        //
        // The condition is `savedVisible || alpha > 0f`, not `alpha > 0f` alone, and both halves are load-
        // bearing: on the frame the save signal lands the flag is already true while the animation has not
        // ticked yet (alpha is still exactly 0), so alpha alone would withhold the chip for a frame — it
        // did, and `a_save_signal_shows_the_saved_confirmation` caught it. The alpha half then keeps the
        // node alive through the fade-*out*, after the flag has gone false, so the chip leaves by fading
        // rather than vanishing.
        if (savedVisible || alpha > 0f) {
        Text(
            text = buildAnnotatedString {
                append(BenchSavedMark)
                append(" ")
                // Frozen `.saved b{font-weight:600}` — the word alone is bold, not the whole line.
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(BenchSavedWord) }
                append(BenchSavedQualifier)
            },
            color = colors.matchaText,
            fontSize = BenchStatusTextSize,
            fontFamily = ZinelyTheme.v2Typography.work,
            modifier = Modifier
                .testTag(BenchSavedChipTestTag)
                // Opacity, not composition: the frozen chip fades and the fade is the assertion. Composed
                // only while it has something to say, so TalkBack does not meet a permanently present node
                // announcing a save that has not happened — the defect C3's style row shipped once and
                // `SurfaceTraversalOrderTest` caught by reading the platform tree rather than a screenshot.
                .graphicsLayer { this.alpha = alpha }
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = BenchSavedSpokenLabel
                },
        )
        }
    }
}
