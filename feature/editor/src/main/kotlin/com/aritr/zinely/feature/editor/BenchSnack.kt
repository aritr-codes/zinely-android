package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the frozen snackbar (`.snack`). */
public const val BenchSnackTestTag: String = "bench-snack"

/** Test tag on its one action (`.snack button`). */
public const val BenchSnackActionTestTag: String = "bench-snack-action"

/** Frozen `.snack{left:14px;right:14px}` (`v2-bench.html:361`). */
internal val BenchSnackInsetH = 14.dp

/** Frozen `.snack{bottom:12px}` (`v2-bench.html:361`). */
internal val BenchSnackInsetBottom = 12.dp

/** Frozen `.snack{border-radius:12px}` (`v2-bench.html:361`). */
internal val BenchSnackRadius = 12.dp

/** Frozen `.snack{padding:12px 14px}` (`v2-bench.html:361`). */
internal val BenchSnackPaddingH = 14.dp
internal val BenchSnackPaddingV = 12.dp

/** Frozen `.snack{gap:10px}` (`v2-bench.html:361`). */
internal val BenchSnackGap = 10.dp

/** Frozen `.snack{font-size:13px}` and `.snack button{font-size:13px}` (`v2-bench.html:361`, `:364`). */
internal val BenchSnackTextSize = 13.sp

/** Frozen `.snack{transform:translateY(16px)}` at rest (`v2-bench.html:362`). */
internal val BenchSnackEnterOffset = 16.dp

/** Frozen `.snack{transition:transform .22s var(--standard),opacity .22s}` (`v2-bench.html:362`). */
internal const val BenchSnackMillis: Int = 220

/** Frozen `del()` — the soft-delete snack stands for **3200ms** (`v2-bench.html:627`). */
public const val BenchSnackDeleteMillis: Long = 3200L

/**
 * Frozen `applyInk()` — the buttonless ink snack stands for **1600ms** (`v2-bench.html:617`).
 *
 * C4 owns the *variant*, not what raises it: the ink path itself is **C6**'s. Declared here so the value
 * lives beside the one it is deliberately different from, and so C6 inherits a constant rather than a
 * re-measurement.
 */
public const val BenchSnackInkMillis: Long = 1600L

/**
 * The frozen snackbar — `.snack` (`v2-bench.html:361-364`, markup `:443`);
 * [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.11, 4.12 and 4.15.
 *
 * ### An inverted surface, and the one token that exists for it
 *
 * `--ink` ground under `--paper` text is the only place in the Bench where the artifact's ink becomes a
 * *surface*. Its action label is `--accent-on-ink`, a token the palette publishes **solely** for this
 * pairing — matcha re-tuned to hold on a dark ground, because plain `--matcha` does not. Using
 * `colors.matcha` here would pass every structural test and fail the contrast one, which is why row 4.12's
 * mutation is exactly that substitution.
 *
 * ### Why it is not [ZSnackbar]
 *
 * `ZSnackbar` is the V1 component the Library and the Proof use, and it is not this shape: this one is
 * frozen at a 12dp radius on `--ink`, enters from `translateY(16px)` over `.22s`, and carries an action
 * that is sometimes absent. The two are not a re-skin of one another and collapsing them would change the
 * Library. Recorded as a deviation from ADR-089 row 4.9's *"delete/undo surface"* in ADR-094.
 *
 * @param visible drives both the frozen properties — the 16dp rise and the fade — from one flag.
 * @param message the line. `Text deleted.` in the delete case; `Ink · <name>` in C6's.
 * @param actionLabel `null` gives the **buttonless** variant (row 4.15). The freeze hides the button by
 *   setting `display:none` on it and restores it on timeout; a null label is the same thing said once.
 */
@Composable
internal fun BenchSnack(
    visible: Boolean,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
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
            .padding(bottom = BenchSnackInsetBottom)
            .graphicsLayer {
                alpha = progress
                translationY = rise
            }
            .testTag(BenchSnackTestTag)
            .clip(RoundedCornerShape(BenchSnackRadius))
            .background(colors.ink)
            .padding(horizontal = BenchSnackPaddingH, vertical = BenchSnackPaddingV),
        horizontalArrangement = Arrangement.spacedBy(BenchSnackGap, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = colors.paper,
            fontSize = BenchSnackTextSize,
            fontFamily = ZinelyTheme.v2Typography.work,
            modifier = Modifier
                .weight(1f)
                // The message is the whole point of the surface appearing, so it announces itself. Polite,
                // not assertive: a deletion the user just performed is a confirmation, not an alarm.
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = message
                },
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = colors.accentOnInk,
                fontSize = BenchSnackTextSize,
                fontWeight = FontWeight.Bold,
                fontFamily = ZinelyTheme.v2Typography.work,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .testTag(BenchSnackActionTestTag)
                    .clearAndSetSemantics {
                        contentDescription = actionLabel
                        role = Role.Button
                        onClick { onAction(); true }
                    },
            )
        }
    }
}

/**
 * The frozen `labelOf(n)` (`v2-bench.html`) reduced to what this product actually has on a page: a
 * text box or a photo. The freeze names its stand-in shapes; the product names the real thing.
 */
internal fun benchDeleteLabel(pages: List<com.aritr.zinely.core.model.Page>, id: String): String {
    val element = pages.firstNotNullOfOrNull { page -> page.elements.firstOrNull { it.id == id } }
    return if (element is com.aritr.zinely.core.model.TextElement) BenchAddTextTitle else BenchAddPhotoTitle
}

/**
 * Frozen `snackText.textContent = labelOf(n) + ' deleted.'` (`v2-bench.html:626`) — the full stop is the
 * freeze's, and it is kept: the line is a sentence about something that happened, not a label.
 */
internal fun benchDeletedMessage(label: String): String = Copy.Snack.deleted(label)

