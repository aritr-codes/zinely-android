package com.aritr.zinely.ui.a11y

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected

/**
 * The V2 control seam: a pressable that reaches the **platform** accessibility tree as its role, carrying
 * its label and its enabled state on the *same* node.
 *
 * ## Why this exists (ADR-059, and the fact ADR-059 does not state)
 *
 * [ADR-059](docs/DECISIONS.md#adr-059) records that the V1 `ZButton` family announces to TalkBack as
 * `android.view.View` rather than `android.widget.Button`, and schedules the V1 fix to C4/C6/C7. This is
 * not that fix — V1 is untouched. This is the seam **V2 components are built on**, so the defect is not
 * re-inherited by every surface Phases B–D add.
 *
 * ADR-059 states the rule as *leaf or `clearAndSetSemantics`*. The sharper statement, and the one that
 * predicts which shapes break: **the role and the label survive only when the control collapses to ONE
 * platform node.** Any child contributing semantics stops the collapse. `ReframeControls.NudgeCell` keeps
 * its `Role.Button` class on a plain `clickable(role = …).semantics { }` because its only child is
 * `Icon(contentDescription = null)`, which contributes nothing. `ZoomButton` had the identical modifier
 * chain and *lost* the class, because a `Text` child split it into three nodes: click and `disabled` on
 * one, label on a second, role on a third. TalkBack landed on the labelled node, which reported
 * `enabled = true` — a disabled control announcing itself as available. The Z* buttons all wrap a
 * `BasicText`, which is exactly why they collapse to `android.view.View`.
 *
 * Measured here, on a container wrapping a text child (`ZinelyV2ControlPlatformA11yTest` pins all three):
 *
 * | shape | class | spoken name |
 * |---|---|---|
 * | `clickable(role) + semantics { contentDescription }` | `android.view.View` | `null` |
 * | the same, through this seam | `android.widget.Button` | the label |
 *
 * The label is lost as well as the class, which ADR-059 does not record.
 *
 * **What is load-bearing here, and what is not.** Every line below was mutation-tested against that suite.
 * `clearAndSetSemantics` is the whole mechanism — replacing it with plain `semantics` turns four tests red.
 * `minimumInteractiveComponentSize()` and `clickable` sitting *outside* the clear are each caught too. But
 * an earlier draft also re-declared `role` inside the cleared block and called `disabled()` when the
 * control was disabled, on the belief that the clear discards what `clickable` set. **It does not**:
 * `clickable` is a separate modifier node outside the cleared region, so its role and its `disabled` flag
 * reach the platform on their own, and deleting either line left the suite green. They were **inert
 * defensive code justified by a mechanism that does not occur**, so they are gone. If Compose ever changes
 * and the clear does start swallowing them, the class and enabled-bit tests go red and say so.
 *
 * ## What this deliberately does not decide
 *
 * **No focus or press appearance.** `indication` is passed through and defaults to `null`. V2's focus
 * indicator is [D-008](docs/design/V2-SPEC-DEFECTS.md) — an owner-deferred question the frozen HTML does
 * not answer, scheduled to Phases B/C. Defaulting to a Material ripple would be inventing an unfrozen
 * visual; drawing nothing is the honest foundation state, and a call site that needs an indication supplies
 * one. [interactionSource] is exposed for the same reason: the focus ring, when it is designed, hangs off it.
 *
 * **No haptics parameter**, on the same reasoning as V1's [com.aritr.zinely.ui.components.zinelyControl]:
 * the spec fires haptic verbs per *action*, never per widget, so callers own it inside [onClick]. The one
 * consequence of that stance is stated rather than left implicit: when [onLongClick] is supplied this
 * modifier **suppresses the platform's own long-press buzz** (`hapticFeedbackEnabled = false`), because a
 * caller that fires its own verb inside [onLongClick] would otherwise play two — the fifth, unnamed verb
 * V1's `ShelfCard` had to silence for the same reason. A caller that wants the platform buzz fires it
 * itself, so the vocabulary stays in one place.
 *
 * ## Long-press, and why it is here rather than at the call site
 *
 * B3's shelf item is the first V2 control with a second gesture, and the frozen Library states both on the
 * same object: *"tap = open zine · long-press = actions"* (`v2-library.html:199`). Hand-rolling
 * `combinedClickable` beside this seam would duplicate the collapse discipline above — the part that took a
 * shipped defect to learn — so the seam grows the gesture instead. [onLongClickLabel] is not optional
 * decoration: [COMPOSE-IMPLEMENTATION-GUIDE.md](docs/COMPOSE-IMPLEMENTATION-GUIDE.md) §6 requires that
 * *"every gesture has a named custom action twin"*, and an unlabelled long-press reaches TalkBack as an
 * anonymous `ACTION_LONG_CLICK` a user has no way to discover.
 *
 * **The long-press *timeout* is the platform's, not the frozen file's.** The mock script fires at 420ms
 * (`:202`); `combinedClickable` uses `ViewConfiguration.getLongPressTimeout()`. The guide's own rule decides
 * it — *"the HTML is a browser mock; the device is real … implement the platform's correct behaviour, and
 * note the deviation"* — and this is the note. Reproducing 420ms would mean a hand-written gesture detector
 * disagreeing with every other long-press on the device.
 *
 * ## One ordering constraint, and it bites quietly
 *
 * This modifier **ends** in `clearAndSetSemantics`, so anything semantic chained after it is discarded —
 * including a `testTag`. `Modifier.testTag("x").zinelyV2Control(…)` works; `.zinelyV2Control(…).testTag("x")`
 * leaves a node nothing can find. Put tags and any additional semantics **before** the control. Layout and
 * paint modifiers are unaffected and may go either side.
 *
 * @param label the spoken name. Required and non-null: a cleared node has no children left to borrow a name
 *   from, so an unlabelled control would reach TalkBack silent.
 * @param onClick the activation, fired by touch and by an accessibility service alike.
 * @param enabled when false, the platform node reports `enabled = false` — the bit `f4faaa4` lied about.
 *   Carried by `clickable`, which the clear does not reach.
 * @param role the semantic role. Reaches the platform as the node's class (`Role.Button` →
 *   `android.widget.Button`), which is the whole point of the seam.
 * @param selected the selected state for a toggle/radio/tab; `null` (the default) declares nothing rather
 *   than declaring "not selected", so a plain button does not tell a service it is an unselected something.
 * @param interactionSource press/focus/hover stream; supply one to hang a focus indicator off it.
 * @param indication the press/focus indication. Deliberately `null` by default — see above.
 * @param onLongClick a second gesture on the same control, or `null` for a plain one-gesture button. Reaches
 *   the platform as `ACTION_LONG_CLICK` on the same node as the click.
 * @param onLongClickLabel the spoken name of that gesture, required whenever [onLongClick] is supplied —
 *   the "named custom action twin" the implementation guide asks for.
 */
public fun Modifier.zinelyV2Control(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    selected: Boolean? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
): Modifier = this
    .minimumInteractiveComponentSize()
    // `clickable` sits OUTSIDE `clearAndSetSemantics` so its click action is not among the semantics the
    // clear discards — the ordering IF5 had to correct on the Reframe chip, where a cleared click stopped
    // firing under assistive tech. `combinedClickable` sits in exactly the same place for the same reason.
    .then(
        if (onLongClick == null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                role = role,
                onClick = onClick,
            )
        } else {
            require(onLongClickLabel != null) {
                "a long-press must be named: an unlabelled ACTION_LONG_CLICK is undiscoverable"
            }
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                role = role,
                // The caller fires its own verb inside onLongClick; the platform's would be a second one.
                hapticFeedbackEnabled = false,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
        },
    )
    // The collapse. Without this a container wrapping a text child reaches TalkBack as
    // `android.view.View` with no name at all. `role` and the `disabled` flag are NOT re-declared here:
    // they ride `clickable`, which sits outside the clear, and mutation-testing shows re-declaring them
    // changes nothing. Only what `clickable` cannot say belongs in this block.
    .clearAndSetSemantics {
        contentDescription = label
        selected?.let { this.selected = it }
    }
