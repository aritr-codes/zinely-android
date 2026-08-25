package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/** Test tag on the frozen snackbar (`.snack`). */
public const val BenchSnackTestTag: String = "bench-snack"

/** Test tag on its one action (`.snack button`). */
public const val BenchSnackActionTestTag: String = "bench-snack-action"

/** Frozen `.snack{left:14px;right:14px}` (`v21-bench.html:450`) — unchanged from V2. */
internal val BenchSnackInsetH = 14.dp

/**
 * The snack's bottom inset **in this host's coordinates**, which is not the frozen number.
 *
 * ⚠ **Deviation, and it is a re-parenting rather than a re-measurement.** V2 put `.snack` inside
 * `.canvasArea` at `bottom:12px`; V2.1 makes it a direct child of `.phone` at `bottom:88px`
 * (`v21-bench.html:484`, markup `:570`). The 88 is measured from the *phone's* foot and therefore spans the
 * bottom bar and the page-navigation row — both of which sit **below** the canvas `EditorScreen` anchors
 * this composable to, so transcribing 88 literally would push the snack 88dp up from a floor that is
 * already above the bar. The equivalent in canvas coordinates is the 12 that is here.
 *
 * Moving the anchor is `EditorScreen`'s call, not P4's, and it is left for the owner: the honest fix is to
 * hoist the snack to the screen root and then transcribe the 88.
 */
internal val BenchSnackInsetBottom = 12.dp

/**
 * Frozen `.snack{border-radius:var(--br-pill)}` (`v21-bench.html:492`), where V2 drew a 12dp radius.
 * A percent shape rather than [ZinelyV21Dimens.radiusPill]'s 999dp, for the reason `BenchBarShape` records.
 */
internal val BenchSnackShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/**
 * Frozen `.snack{padding:var(--gap-md) var(--gap-sm) var(--gap-md) var(--gap-lg)}` (`v21-bench.html:492`) —
 * top 12, right 8, bottom 12, left 16. Asymmetric on purpose: the line needs the room, the button already
 * carries its own 12dp of padding on the right.
 */
internal val BenchSnackPadding = PaddingValues(
    start = ZinelyV21Dimens.gapLg,
    top = ZinelyV21Dimens.gapMd,
    end = ZinelyV21Dimens.gapSm,
    bottom = ZinelyV21Dimens.gapMd,
)

/** Frozen `.snack{gap:var(--gap-md)}` (`v21-bench.html:491`) — 12, where V2 gapped 10. */
internal val BenchSnackGap = ZinelyV21Dimens.gapMd

/** Frozen `.snack{border:1.5px solid var(--ink-line)}` (`v21-bench.html:491`) — **new in V2.1**; see [BenchSnack]. */
internal val BenchSnackBorder = 1.5.dp

/** Frozen `.snack{font-size:.79rem}` (`v21-bench.html:492`) — 12.64sp at the prototype's 16px root. */
internal val BenchSnackTextSize = 12.64.sp

/**
 * Frozen `.snack button{font-size:.78rem}` (`v21-bench.html:528`) — the action reads a hair smaller than
 * the line.
 *
 * ⚠ **This cited `:466`, and `:466` was never `.snack`** — it was `.tile svg`, in the Art sheet, before
 * amendment A7 moved it to `:472`. The citation was wrong when it was written, not broken by A7, and the
 * A7 citation sweep found it precisely because the sweep could not map it. Re-aimed at the real rule; this
 * is a [D-096](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-096) instance, and the second one
 * that sweep has surfaced.
 */
internal val BenchSnackActionTextSize = 12.48.sp

/** Frozen `.snack button{padding:var(--gap-xs) var(--gap-md)}` (`v21-bench.html:502`). */
internal val BenchSnackActionPaddingH = ZinelyV21Dimens.gapMd
internal val BenchSnackActionPaddingV = ZinelyV21Dimens.gapXs

/** Frozen `.snack{transform:translateY(8px) …}` at rest (`v21-bench.html:493`). V2 rose 16. */
internal val BenchSnackEnterOffset = 8.dp

/**
 * Frozen `.snack{transform:… rotate(-.6deg)}` (`v21-bench.html:493`, `:483`) — **new in V2.1**, and it is
 * carried in *both* states, so it is a resting property rather than part of the entrance.
 *
 * The toast is a scrap of paper dropped on the desk, not a system surface: the tilt is what says so. It is
 * the one place in this file where V2.1 adds a mark rather than replacing one.
 */
internal const val BenchSnackRotationDeg: Float = -0.6f

/**
 * A8's 2dp allowance for the rotated pill's painted corner. Without it, a nominal 12dp layout gap is
 * only about 10.5dp between transformed bounds on a phone-width canvas (the D-089 regression fixture).
 */
internal val BenchSnackStackRotationAllowance = 2.dp

/** Frozen `.snack{transition:opacity .18s,transform .18s}` (`v21-bench.html:494`). V2 asked for 220ms. */
internal const val BenchSnackMillis: Int = 180

/** Frozen `toast()` — the snack stands for **3200ms** (`v21-bench.html:763`). Unchanged from V2. */
public const val BenchSnackDeleteMillis: Long = 3200L

/**
 * The buttonless ink snack stands for **1600ms**.
 *
 * C4 owns the *variant*, not what raises it: the ink path itself is **C6**'s. Declared here so the value
 * lives beside the one it is deliberately different from, and so C6 inherits a constant rather than a
 * re-measurement.
 *
 * ⚠ **No longer frozen.** V2's `applyInk()` gave the buttonless toast its own shorter life; V2.1 has a
 * single `toast(msg, undoable)` with one 3200ms timer for both variants (`v21-bench.html:759-763`). The
 * value is kept because shortening the dwell of a toast nobody can act on is a behaviour the product
 * already ships and P4 is a re-skin — but it is now a product decision with no frozen backing, and it is
 * `EditorScreen` that reads it. Owner call.
 */
public const val BenchSnackInkMillis: Long = 1600L

/**
 * The frozen snackbar — `.snack` (`v21-bench.html:450-480`, markup `:570`);
 * [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.11, 4.12 and 4.15, re-skinned to V2.1
 * by [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102) package P4.
 *
 * ### An inverted surface, and the one border rule that has to bend for it
 *
 * `ink` ground under `paper` text is the only place in the Bench where the artifact's ink becomes a
 * *surface*, and V2.1 gives it a **border** V2 did not have — `1.5px solid var(--ink-line)`. The freeze
 * annotates its own exception at `v21-bench.html:485-490`: everything else in the language is outlined in
 * `ink`, and an ink outline on an ink ground is invisible, so this one control is outlined in the *shadow*
 * ink instead. The rule being kept is *"a border contrasts with what it sits on"*, not *"a border is
 * always ink"*.
 *
 * ### The action label lost `--accent-on-ink`, and that token is retired here
 *
 * V2 drew `Undo` in a matcha re-tuned for a dark ground. V2.1 draws it in the snack's own `paper` and
 * **underlines** it (`v21-bench.html:497-502`): the freeze's own note records that the butter it tried
 * first measured 7.89:1 in light and 1.59:1 in dark, where the ink ground turns cream. Colour was doing
 * the work of saying *"this is the action"*; the underline does it at any theme. Substituting `butter`
 * back is exactly the mutation row 4.12 forbids, one token over.
 *
 * ### Why it is not [ZSnackbar]
 *
 * `ZSnackbar` is the V1 component the Library and the Proof use, and it is not this shape: this one is a
 * tilted pill on `ink`, enters from `translateY(8px)` over `.18s`, and carries an action that is sometimes
 * absent. The two are not a re-skin of one another and collapsing them would change the Library. Recorded
 * as a deviation from ADR-089 row 4.9's *"delete/undo surface"* in ADR-094.
 *
 * @param visible drives both the frozen animated properties — the 8dp rise and the fade — from one flag.
 *   The tilt is **not** one of them; see [BenchSnackRotationDeg].
 * @param message the line. `Text deleted.` in the delete case; `Ink · <name>` in C6's.
 * @param actionLabel `null` gives the **buttonless** variant (row 4.15). The freeze hides the button by
 *   setting `display:none` on it and restores it on timeout; a null label is the same thing said once.
 * @param bottomClearance extra space reserved below the snack when another bottom-anchored surface is
 *   present. D-089 uses the context bar's complete footprint, leaving this component's own 12dp inset as
 *   the frozen gap between their painted bounds.
 */
@Composable
internal fun BenchSnack(
    visible: Boolean,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 0.dp,
) {
    val colors = ZinelyTheme.v21Colors
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        // Still routed through the V2 motion object: V2.1 changed the duration, not the arrival, and this
        // is where the reduced-motion downgrade lives ([ADR-075]). Same call [BenchStyleRow] makes.
        animationSpec = ZinelyTheme.v2Motion.standard(BenchSnackMillis),
        label = "bench-snack",
    )
    if (progress <= 0f) return
    val riseDp = BenchSnackEnterOffset * (1f - progress)
    val rise = with(LocalDensity.current) { riseDp.toPx() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BenchSnackInsetH)
            .padding(bottom = BenchSnackInsetBottom + bottomClearance)
            .graphicsLayer {
                alpha = progress
                translationY = rise
                // The tilt is in BOTH the rest and the shown rule, so it is not animated: the scrap of
                // paper is lying crooked on the desk the whole time, it does not straighten as it lands.
                rotationZ = BenchSnackRotationDeg
            }
            .testTag(BenchSnackTestTag)
            .clip(BenchSnackShape)
            .background(colors.surfaceSoft)
            // A transient confirmation is a warm support scrap: ordinary ink and border on surfaceSoft.
            .border(BenchSnackBorder, colors.ink, BenchSnackShape)
            .padding(BenchSnackPadding),
        horizontalArrangement = Arrangement.spacedBy(BenchSnackGap, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = colors.ink,
            fontSize = BenchSnackTextSize,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            modifier = Modifier
                .weight(1f)
                // The message is the whole point of the surface appearing, so it announces itself. Polite,
                // not assertive: a deletion the user just performed is a confirmation, not an alarm.
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = message
                },
        )
        // Undo takes a deletion back — the one Bench action whose window closes on its own, so the
        // hand is told it landed before the surface disappears.
        val act = benchTap(action = onAction)
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                // `paper`, underlined — see the class note. `butter` here is the retired exception.
                color = colors.ink,
                fontSize = BenchSnackActionTextSize,
                fontWeight = FontWeight.Bold,
                fontFamily = ZinelyV21Fonts.Work,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                // `text-underline-offset:3px;text-decoration-thickness:1.5px` have no Compose equivalent —
                // the underline is drawn at the platform's own offset and weight. Recorded, not faked.
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(BenchSnackShape)
                    .clickable(onClick = act)
                    .testTag(BenchSnackActionTestTag)
                    .clearAndSetSemantics {
                        contentDescription = actionLabel
                        role = Role.Button
                        onClick { act(); true }
                    }
                    .padding(
                        horizontal = BenchSnackActionPaddingH,
                        vertical = BenchSnackActionPaddingV,
                    ),
            )
        }
    }
}

/**
 * The frozen `labelOf(n)` (`v2-bench.html`) reduced to what this product actually has on a page. The
 * freeze names its stand-in shapes; the product names the real thing.
 *
 * ⚠ **Exhaustive over [com.aritr.zinely.core.model.Element], deliberately.** This was
 * `if (element is TextElement) … else PHOTO` — a two-way test over a three-way sealed hierarchy, so a
 * [com.aritr.zinely.core.model.DecorElement] was deleted as *"Photo deleted."* It shipped harmless only
 * because decor was unreachable; ADR-105 S7 made it reachable, and the same string is read aloud by the
 * TalkBack `Delete` action (`EditorA11y`), so the wrong word was also the spoken one. Found by
 * independent review of that package.
 *
 * **Do not reintroduce an `else` here** — the same instruction `benchVerbKindOf` carries, for the same
 * reason: the exhaustive `when` is the mechanism by which a fourth element kind is forced to declare its
 * name instead of silently inheriting someone else's.
 */
internal fun benchDeleteLabel(pages: List<com.aritr.zinely.core.model.Page>, id: String): String =
    when (pages.firstNotNullOfOrNull { page -> page.elements.firstOrNull { it.id == id } }) {
        is com.aritr.zinely.core.model.TextElement -> BenchAddTextTitle
        is com.aritr.zinely.core.model.ImageElement -> BenchAddPhotoTitle
        is com.aritr.zinely.core.model.DecorElement -> BenchAddArtTitle
        // The id names nothing — a deleted element's own snack is raised after the delete in one call
        // site, so the lookup can miss. The old `else` answered `Photo`; `Art` would be no better a
        // guess, so this keeps the historical answer for the one case that is genuinely unknown.
        null -> BenchAddPhotoTitle
    }

/**
 * Frozen `snackText.textContent = labelOf(n) + ' deleted.'` (`v2-bench.html:626`) — the full stop is the
 * freeze's, and it is kept: the line is a sentence about something that happened, not a label.
 */
internal fun benchDeletedMessage(label: String): String = Copy.Snack.deleted(label)
