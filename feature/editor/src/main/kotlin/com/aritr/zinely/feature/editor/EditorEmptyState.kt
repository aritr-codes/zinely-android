package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Test tag on the empty-state container. */
public const val EditorEmptyStateTestTag: String = "editor-empty-state"

/** Content descriptions = stable hooks for the two supply actions (also the spoken a11y labels). */
public const val AddPhotoActionLabel: String = "Add a photo"
public const val AddWordsActionLabel: String = "Add words"

/**
 * The cozy first-run invitation (docs/design/DESIGN-LANGUAGE.md §8/§9) — shown on the canvas when the
 * current page has no elements. It turns a blank sheet (which reads as a void) into an encouraging
 * "let's make something cute" prompt with two **visible** supplies, so a first-time user can start
 * without discovering a hidden gesture (the UX-audit findings: empty-state, discoverability, visible
 * add-text, contextual guidance).
 *
 * Both actions are hoisted: [onAddPhoto] dispatches `Intent.RequestAddImage` (the same path as the
 * app's photo entry point) and [onAddText] dispatches `Intent.PlaceText` (a seed text box the user then
 * edits) — wired by the host so this composable stays pure and testable. Warm, first-person microcopy
 * and the privacy reassurance are part of the identity, never system-error-shaped.
 *
 * Accessibility: the two supplies are real M3 buttons (≥48dp, `Role.Button`) with the action text as
 * their spoken label; the decorative sticker cluster is not announced. Colours come from the theme
 * (primary = coral supply, paper card for the secondary), so it inherits the zine identity.
 *
 * @param onAddPhoto invoked when the user taps "Add a photo".
 * @param onAddText invoked when the user taps "Add words".
 * @param modifier sizing/placement applied by the host (typically centered over the page).
 */
@Composable
public fun EditorEmptyState(
    onAddPhoto: () -> Unit,
    onAddText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
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
            StickerBlob(colors.secondary, tilt = -9f, glyph = "✿")
            StickerBlob(colors.surfaceVariant, tilt = 6f, glyph = "❀")
            StickerBlob(colors.primary, tilt = 12f, glyph = "★")
        }

        Text(
            text = "Let's make something cute ✨",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Start with a photo or a few words.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onAddPhoto,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) { Text("🖼️  $AddPhotoActionLabel") }

            OutlinedButton(
                onClick = onAddText,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("✏️  $AddWordsActionLabel") }
        }

        Spacer(Modifier.size(4.dp))
        Text(
            text = "works offline · stays on your phone",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
        Text(glyph, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}
