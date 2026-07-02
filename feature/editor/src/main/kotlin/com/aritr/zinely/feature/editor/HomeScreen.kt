package com.aritr.zinely.feature.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Test tag on the shelf list (the Content state's scrollable column of zine cards). */
public const val HomeShelfTestTag: String = "home-shelf"

/** Test tag on the Loading spinner container. */
public const val HomeLoadingTestTag: String = "home-loading"

/** Test tag on the empty-shelf invitation container. */
public const val HomeEmptyStateTestTag: String = "home-empty-state"

/** Headline for the empty shelf (SCREEN-INVENTORY §Home; its CTA arrives with S6.3 create). */
public const val HomeEmptyHeadline: String = "Nothing here yet — let's change that."

/** Test tag for one zine card on the shelf, keyed by its stable project [id]. */
public fun homeCardTestTag(id: String): String = "home-card-$id"

/**
 * One zine on the shelf — the feature-local UI model, carrying only what the card shows
 * (ADR-043). The host ViewModel maps `ProjectSummary` → this, so this module stays free of
 * `:core:data`; [id] is the project id the tap hands back for `EditorRoute(id)` navigation.
 */
public data class HomeZineCard(
    val id: String,
    val title: String,
    val formatLabel: String,
    val editedLabel: String,
)

/**
 * Home · "My zines" — the S6.2 READ-ONLY shelf (ADR-043; SCREEN-INVENTORY §Home): a cozy list of
 * paper cards, newest first *as given* (ordering is the `ProjectRepository` contract, never
 * re-derived here), each opening its zine via [onOpenZine]. Deliberately shelf-only this slice:
 * **no create/duplicate/delete/rename affordance exists** (the ADR-042 hard invariant — mutation
 * UI ships in S6.3 behind the open-editor exclusion), and the empty state is an invitation without
 * its Start-a-zine CTA until that action exists. Stateless; the host owns state and navigation.
 *
 * @param loading true until the project store first answers — shows a quiet spinner so the empty
 *   invitation never flashes before the shelf is actually known to be empty.
 * @param cards the shelf, newest first; empty shows the invitation.
 * @param onOpenZine tap handler, called with the tapped card's project id.
 */
@Composable
public fun HomeScreen(
    loading: Boolean,
    cards: List<HomeZineCard>,
    onOpenZine: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "My zines",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize().testTag(HomeLoadingTestTag),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            cards.isEmpty() -> HomeEmptyShelf(Modifier.fillMaxSize())

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(HomeShelfTestTag),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    HomeZineCardRow(
                        card = card,
                        // A hair of alternating tilt — handmade, not template-made
                        // (DESIGN-LANGUAGE §2); static, so reduced motion is untouched.
                        tilt = if (index % 2 == 0) -0.6f else 0.6f,
                        onOpen = { onOpenZine(card.id) },
                    )
                }
            }
        }
    }
}

/** One paper card on the shelf: title over format + recency, the whole card one ≥48dp tap. */
@Composable
private fun HomeZineCardRow(card: HomeZineCard, tilt: Float, onOpen: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .rotate(tilt)
            .testTag(homeCardTestTag(card.id))
            .semantics { role = Role.Button }
            .clickable(onClickLabel = "Open ${card.title}") { onOpen() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = card.formatLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = card.editedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The empty shelf — warm invitation, deliberately **CTA-less in S6.2**: the canonical Home spec
 * (SCREEN-INVENTORY §Home) pairs this headline with a Start-a-zine button, but that button IS the
 * create action, which ships in S6.3 behind the ADR-042 open-editor exclusion. Until then an
 * actionless invitation beats a disabled button (reads broken) or a mutation-shaped stub. In
 * practice this state is unreachable while boot seeds `"default"` (ADR-030 §4) — it exists, warm
 * and tested, for the S6.5 re-root.
 */
@Composable
private fun HomeEmptyShelf(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag(HomeEmptyStateTestTag).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = HomeEmptyHeadline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Every zine you make will line up right here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "works offline · stays on your phone",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
