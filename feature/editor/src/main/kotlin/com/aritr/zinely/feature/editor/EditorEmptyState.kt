package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the empty-state container. */
public const val EditorEmptyStateTestTag: String = "editor-empty-state"

/** The two add-action labels — owned by the supply tray (the single action home, [ADR-033]). Shared as
 *  the tray's visible + spoken labels; the empty state references them only to *name* what's below. */
public const val AddPhotoActionLabel: String = "Add a photo"
public const val AddWordsActionLabel: String = "Add words"

/** Test tag on the decorative downward cue that ties the invitation to the supply shelf below. */
public const val EmptyStateTrayCueTag: String = "empty-state-tray-cue"

/** Headline for the **first** blank page — the warm welcome (VOICE empty states). */
public const val FirstPageInvitationHeadline: String = "Let's make something cute ✨"

/** Headline for a **later** blank page — the lighter "fresh page" variant (VOICE empty states). */
public const val LaterPageInvitationHeadline: String = "A fresh page. What goes here?"

/**
 * The cozy first-run invitation (docs/design/DESIGN-LANGUAGE.md §8/§9) — shown on the canvas when the
 * current page has no elements. It turns a blank sheet (which reads as a void) into an encouraging
 * "let's make something cute" prompt, so a first-time user feels invited rather than faced with a void.
 *
 * **Invitation-only — no buttons ([ADR-033](../DECISIONS.md#adr-033)).** The add actions live solely in
 * the always-visible [EditorSupplyTray] (the thumb-zone home, DESIGN-RULES 3/7), so "Add a photo" /
 * "Add words" never appear twice at once on a blank page. This surface only *invites and orients* — its
 * subcopy names the two ways to start and points to the supplies on the shelf just below. Warm,
 * first-person microcopy and the privacy reassurance are part of the identity, never system-error-shaped.
 *
 * Accessibility: this overlay is non-interactive text + ornament — the decorative sticker cluster is not
 * announced, and the actionable, labelled controls are the tray's (each a ≥48dp `Role.Button`). Colours
 * come from the theme, so it inherits the zine identity.
 *
 * The headline follows the page's position (VOICE empty states): the **first** page keeps the warm
 * welcome ([FirstPageInvitationHeadline]); a **later** blank page uses the lighter "fresh page" variant
 * ([LaterPageInvitationHeadline]). Only the headline changes — the subcopy, cue, and invitation-only
 * rule are identical, so the tray still solely owns the add actions on every blank page.
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
    // The invitation overlays the blank paper, so its ink pairs with the paper tokens (headline `--ink`,
    // supporting copy `--ink-soft`) — the same on-paper vocabulary bench.html's empty-panel ghost uses.
    // The sticker cluster carries the frozen authorial inks (teal / paper-edge / coral).
    val colors = ZinelyTheme.colors
    val type = ZinelyTheme.typography
    Column(
        modifier = modifier
            .testTag(EditorEmptyStateTestTag)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Decorative "sticker cluster" — three tilted craft shapes. Asset-free (drawn), and not
        // announced to screen readers (purely ornamental).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StickerBlob(colors.teal, tilt = -9f, glyph = "✿")
            StickerBlob(colors.paperEdge, tilt = 6f, glyph = "❀")
            StickerBlob(colors.coral, tilt = 12f, glyph = "★")
        }

        Text(
            text = if (firstPage) FirstPageInvitationHeadline else LaterPageInvitationHeadline,
            // The product's display voice (Fraunces) for the headline, matching the frozen `--voice`.
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = type.voice),
            fontWeight = FontWeight.Bold,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        // Names the two ways to start AND points to where they live (the supply shelf just below), so the
        // button-less invitation still answers "what do I do next?" without re-presenting the actions.
        Text(
            text = "Grab a photo or a few words from the supplies below.",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = type.shell),
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(4.dp))
        Text(
            text = "works offline · stays on your phone",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = type.shell),
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
        )

        // Orientation cue: a subtle downward chevron beneath the invitation, pointing the eye to the
        // supply shelf just below the canvas where the add actions live (ADR-033 follow-up). It is a
        // flourish that also does a job (DESIGN-RULES 10) — purely static (no motion, so the reduced-
        // motion path is a no-op) and `clearAndSetSemantics` strips it from the a11y tree, so it adds
        // no screen-reader noise; the tray's "Supplies" heading gives TalkBack the same orientation.
        Box(modifier = Modifier.testTag(EmptyStateTrayCueTag)) {
            Text(
                text = "⌄",
                style = MaterialTheme.typography.titleLarge,
                color = colors.inkSoft.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** One decorative craft shape — a tilted rounded blob with a centered glyph. Ornamental only. */
@Composable
private fun StickerBlob(color: androidx.compose.ui.graphics.Color, tilt: Float, glyph: String) {
    Box(
        modifier = Modifier
            .graphicsLayer { rotationZ = tilt }
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}
