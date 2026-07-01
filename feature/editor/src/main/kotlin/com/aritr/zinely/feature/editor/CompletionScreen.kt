package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Test tag on the whole Completion surface. */
public const val CompletionScreenTestTag: String = "completion-screen"

/** Test tag on the primary "Send to a friend" action. */
public const val CompletionSendTestTag: String = "completion-send"

/** Test tag on the secondary "Open it" action. */
public const val CompletionOpenTestTag: String = "completion-open"

/** Test tag on the "Keep editing" action. */
public const val CompletionKeepEditingTestTag: String = "completion-keep-editing"

/** Test tag on the "How to fold it" instructions card. */
public const val CompletionFoldStepsTestTag: String = "completion-fold-steps"

/** Which delivery the user asked for — a feature-local intent the host maps to a share/open Intent. */
public enum class CompletionAction { SEND, OPEN }

/** One fold instruction: a tiny static diagram (drawn by [FoldDiagram]) beside its plain-language text. */
private data class FoldStep(val n: Int, val text: String)

private val FOLD_STEPS = listOf(
    FoldStep(1, "Fold the sheet in half, the long way. Press the crease flat."),
    FoldStep(2, "Fold it in half again, then open this last fold back up."),
    FoldStep(3, "Snip the little slit in the middle — just along the center crease."),
    FoldStep(4, "Push the ends together so the slit opens, and fold it into a tiny book."),
)

/**
 * The S5 **Completion · fold-steps** screen (docs/design/SCREEN-INVENTORY.md#completion--fold-steps,
 * mockups/completion.html) — the payoff peak: "you made a little book." It celebrates, then never
 * assumes the fold is obvious — four plain-language steps, each with a small **static** diagram.
 *
 * Stateless: the host ([`:app`] `CompletionDestination`) owns the export/share/open and drives [working]
 * (which action is running; both disable) and any [errorMessage]. The screen makes no export decision and
 * runs no animation, so it is trivially reduced-motion-safe ([ADR-040]).
 *
 * Reuses the shipped export seam (no parallel path): "Send to a friend" and "Open it" both render the
 * *current* document to a file via the same `ExportViewModel`; the host maps the finished file to an
 * `ACTION_SEND` chooser or an `ACTION_VIEW` open. "Keep editing" returns to the editor (a true "make
 * another" awaits the multi-project layer, ADR-040 deferral).
 *
 * @param onSendToFriend primary — export + hand to the OS share sheet.
 * @param onOpenIt secondary — export + open in a viewer.
 * @param onKeepEditing return to the editor (the honest "make another" for the single-project MVP).
 * @param onBack navigate back (to Export).
 * @param working the action currently running (its button shows progress; both disable), or null.
 * @param errorMessage a transient export-failure banner, or null.
 * @param onDismissError acknowledge the error banner.
 * @param modifier sizing/placement for the whole screen.
 */
@Composable
public fun CompletionScreen(
    onSendToFriend: () -> Unit,
    onOpenIt: () -> Unit,
    onKeepEditing: () -> Unit,
    onBack: () -> Unit,
    working: CompletionAction? = null,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val busy = working != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .testTag(CompletionScreenTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back" },
            ) { Text("‹  Back") }
        }

        // Hero (static — no confetti/animation; the mockup's sparkle is decorative and reduced-motion-off).
        Text(
            text = "Your zine is ready! 🎉",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp),
        )
        Text(
            text = "You made a little book. That's the whole thing.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp),
        )
        DecorativeBooklet()

        if (errorMessage != null) {
            Surface(
                color = colors.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissError) { Text("Got it") }
                }
            }
        }

        // How to fold it — never assumed known.
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag(CompletionFoldStepsTestTag),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✦  How to fold it",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FOLD_STEPS.forEach { step -> FoldStepRow(step) }
            }
        }

        // Actions.
        Button(
            onClick = onSendToFriend,
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .testTag(CompletionSendTestTag)
                .semantics { contentDescription = "Send to a friend" },
        ) {
            if (working == CompletionAction.SEND) {
                CircularProgressIndicator(
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text("Send to a friend")
            }
        }
        Text(
            text = "opens your phone's own share sheet",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onOpenIt,
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CompletionOpenTestTag)
                    .semantics { contentDescription = "Open it" },
            ) {
                if (working == CompletionAction.OPEN) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("Open it")
                }
            }
            OutlinedButton(
                onClick = onKeepEditing,
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CompletionKeepEditingTestTag)
                    .semantics { contentDescription = "Keep editing" },
            ) { Text("Keep editing") }
        }

        // Honest privacy line: Zinely itself never uploads; the file only leaves when the user shares it.
        Text(
            text = "Made on your phone — Zinely uploads nothing. It's yours until you share it.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
        )
    }
}

/** One step: a small static fold diagram, a numbered badge, and the plain-language instruction. */
@Composable
private fun FoldStepRow(step: FoldStep) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FoldDiagram(step.n)
        Surface(color = colors.secondary, shape = RoundedCornerShape(50)) {
            Text(
                text = step.n.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            text = step.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A tiny static diagram of the fold at [step] (1..4): a paper square with the crease / cut / booklet mark.
 * Purely illustrative — cleared from the a11y tree (the step text carries the meaning). No animation, so
 * it needs no reduced-motion branch.
 */
@Composable
private fun FoldDiagram(step: Int) {
    val colors = MaterialTheme.colorScheme
    val paper = colors.surfaceVariant
    val ink = colors.onSurfaceVariant
    val coral = colors.error // the "cut/attention" accent, matching the sheet's cut guides
    Canvas(
        modifier = Modifier
            .size(width = 44.dp, height = 40.dp)
            .clearAndSetSemantics { },
    ) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        val stroke = 2f * density
        when (step) {
            1 -> {
                // Full sheet, dashed vertical crease down the middle.
                val w = size.width * 0.8f
                val h = size.height * 0.85f
                val left = (size.width - w) / 2
                val top = (size.height - h) / 2
                drawRect(paper, topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h))
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width / 2, top),
                    androidx.compose.ui.geometry.Offset(size.width / 2, top + h),
                    strokeWidth = stroke, pathEffect = dash)
            }
            2 -> {
                // Folded once → narrower sheet, a second dashed crease.
                val w = size.width * 0.42f
                val h = size.height * 0.85f
                val left = (size.width - w) / 2
                val top = (size.height - h) / 2
                drawRect(paper, topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h))
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width / 2, top),
                    androidx.compose.ui.geometry.Offset(size.width / 2, top + h),
                    strokeWidth = stroke, pathEffect = dash)
            }
            3 -> {
                // Sheet with a coral cut along the middle of the center crease.
                val w = size.width * 0.8f
                val h = size.height * 0.85f
                val left = (size.width - w) / 2
                val top = (size.height - h) / 2
                drawRect(paper, topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h))
                drawLine(coral, androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.35f),
                    androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.65f),
                    strokeWidth = stroke * 1.5f)
            }
            else -> {
                // A tiny open booklet: two leaning pages meeting at a spine.
                val h = size.height * 0.7f
                val top = (size.height - h) / 2
                val mid = size.width / 2
                val pageW = size.width * 0.3f
                drawRect(paper, topLeft = androidx.compose.ui.geometry.Offset(mid - pageW, top),
                    size = androidx.compose.ui.geometry.Size(pageW, h),
                    style = Stroke(width = stroke))
                drawRect(paper, topLeft = androidx.compose.ui.geometry.Offset(mid, top),
                    size = androidx.compose.ui.geometry.Size(pageW, h),
                    style = Stroke(width = stroke))
                drawLine(ink, androidx.compose.ui.geometry.Offset(mid, top),
                    androidx.compose.ui.geometry.Offset(mid, top + h), strokeWidth = stroke)
            }
        }
    }
}

/**
 * A decorative little folded booklet (mockup's finished-book hero). Purely illustrative and static, so it
 * is cleared from the a11y tree; the celebration is carried by the surrounding copy.
 */
@Composable
private fun DecorativeBooklet() {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(4.dp, 8.dp, 8.dp, 4.dp),
        modifier = Modifier
            .padding(vertical = 10.dp)
            .size(width = 110.dp, height = 134.dp)
            .graphicsLayer { rotationZ = -2.4f }
            .clearAndSetSemantics { },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 42.dp)
                    .background(colors.error, RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "My\nSummer",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
