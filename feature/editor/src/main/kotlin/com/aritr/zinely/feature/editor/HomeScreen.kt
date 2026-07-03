package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow

/** Test tag on the shelf list (the Content state's scrollable column of zine cards). */
public const val HomeShelfTestTag: String = "home-shelf"

/** Test tag on the Loading spinner container. */
public const val HomeLoadingTestTag: String = "home-loading"

/** Test tag on the empty-shelf invitation container. */
public const val HomeEmptyStateTestTag: String = "home-empty-state"

/** Headline for the empty shelf (SCREEN-INVENTORY §Home), paired with its Start-a-zine CTA. */
public const val HomeEmptyHeadline: String = "Nothing here yet — let's change that."

/** Test tag on the rename dialog, its title field, and its confirm button (ADR-044 §4). */
public const val HomeRenameDialogTestTag: String = "home-rename-dialog"
public const val HomeRenameFieldTestTag: String = "home-rename-field"
public const val HomeRenameConfirmTestTag: String = "home-rename-confirm"

/** Test tag for one zine card on the shelf, keyed by its stable project [id]. */
public fun homeCardTestTag(id: String): String = "home-card-$id"

/** Test tag for a card's overflow-menu button. */
public fun homeCardMenuTestTag(id: String): String = "home-card-menu-$id"

/** Test tag for a card's rendered page-1 thumbnail image (ADR-045). */
public fun homeCardThumbnailTestTag(id: String): String = "home-card-thumb-$id"

/** Test tag for a card's warm paper placeholder, shown whenever no thumbnail exists. */
public fun homeCardThumbnailPlaceholderTestTag(id: String): String = "home-card-thumb-placeholder-$id"

/** The undo snackbar's message for a hidden-pending-delete zine (VOICE: gentle, undoable). */
public fun homeDeletedMessage(title: String): String = "“$title” deleted"

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
    /**
     * The rendered page-1 thumbnail (ADR-045), already decoded off-main by the host; `null` shows
     * the warm paper placeholder — a missing thumbnail is never a broken card.
     */
    val thumbnail: ImageBitmap? = null,
)

/**
 * One-shot shelf event from the host to the screen (ADR-044 §3/§5) — feature-local so the module
 * stays free of `:core:data`. Queued events, not observable state: each is consumed exactly once
 * by the screen's snackbar loop, so multiple deletes serialise and recomposition can't replay one.
 */
public sealed interface HomeShelfEvent {
    /**
     * A delete was requested and the card is already hidden: show the undo snackbar for [title];
     * Undo → `onDeleteUndo(id)`, dismiss/timeout → `onDeleteCommit(id)` (the actual store delete).
     */
    public data class DeletePrompt(val id: String, val title: String) : HomeShelfEvent

    /** A transient, warm, already-worded message (mutation failures). */
    public data class Message(val text: String) : HomeShelfEvent
}

/**
 * Home · "My zines" (read shelf ADR-043 + S6.3 actions ADR-044; SCREEN-INVENTORY §Home): a cozy
 * list of paper cards, newest first *as given* (ordering is the `ProjectRepository` contract,
 * never re-derived here), each opening its zine via [onOpenZine]. **Start a zine** is always
 * reachable — the empty invitation's CTA (the ADR-043 §5 deviation is over) and an extended FAB on
 * the content shelf. Each card carries an overflow menu: Rename (gentle dialog, blank disabled),
 * Duplicate, and a confirm-less Delete whose undo window is the snackbar driven by [events]
 * ([HomeShelfEvent.DeletePrompt] → Undo ⇒ [onDeleteUndo], dismissed ⇒ [onDeleteCommit]). The
 * open-editor exclusion lives in the data layer (`DataError.Busy`), not here — failures arrive
 * back as warm [HomeShelfEvent.Message]s. Stateless but for the rename draft and the snackbar;
 * the host owns state, timing of nothing (the snackbar owns the undo window), and navigation.
 *
 * [storeEmpty] is the honest empty-shelf signal (ADR-044 §3, wired by ADR-046): the invitation
 * shows only when the STORE is empty — a shelf filtered to zero visible [cards] by pending
 * undoable deletes is a zero-card shelf (with the FAB still reachable), never the invitation.
 */
@Composable
public fun HomeScreen(
    loading: Boolean,
    storeEmpty: Boolean,
    cards: List<HomeZineCard>,
    events: Flow<HomeShelfEvent>,
    onOpenZine: (String) -> Unit,
    onStartZine: () -> Unit,
    onRenameZine: (String, String) -> Unit,
    onDuplicateZine: (String) -> Unit,
    onDeleteZine: (String) -> Unit,
    onDeleteUndo: (String) -> Unit,
    onDeleteCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // The collector outlives recompositions; always call the latest handlers.
    val currentUndo by rememberUpdatedState(onDeleteUndo)
    val currentCommit by rememberUpdatedState(onDeleteCommit)
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is HomeShelfEvent.DeletePrompt -> {
                    val result = snackbarHostState.showSnackbar(
                        message = homeDeletedMessage(event.title),
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> currentUndo(event.id)
                        SnackbarResult.Dismissed -> currentCommit(event.id)
                    }
                }
                is HomeShelfEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    var renameTarget by remember { mutableStateOf<HomeZineCard?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The empty state carries its own inline CTA; doubling it with a FAB would shout. A
            // zero-card (pending-delete-filtered) shelf keeps the FAB: Start a zine stays reachable.
            if (!loading && !storeEmpty) {
                ExtendedFloatingActionButton(onClick = onStartZine) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Start a zine")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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

                storeEmpty -> HomeEmptyShelf(onStartZine, Modifier.fillMaxSize())

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
                            onRename = { renameTarget = card },
                            onDuplicate = { onDuplicateZine(card.id) },
                            onDelete = { onDeleteZine(card.id) },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        HomeRenameDialog(
            card = target,
            onRename = { title ->
                renameTarget = null
                onRenameZine(target.id, title)
            },
            onKeepName = { renameTarget = null },
        )
    }
}

/**
 * One paper card on the shelf: title over format + recency; the card body is one ≥48dp open tap,
 * and the trailing overflow menu holds the S6.3 actions (Rename / Duplicate / Delete).
 */
@Composable
private fun HomeZineCardRow(
    card: HomeZineCard,
    tilt: Float,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
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
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeCardThumbnail(
                card = card,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
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
            Box(modifier = Modifier.padding(end = 4.dp)) {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag(homeCardMenuTestTag(card.id)),
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options for ${card.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = { menuOpen = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

/**
 * The card's little page-1 render (ADR-045, SCREEN-INVENTORY §Home "paper-card thumbnail") — a
 * fixed paper-shaped slot so cards never jump when a thumbnail arrives. No thumbnail (still
 * rendering, unreadable document, decode failure) shows a soft blank paper placeholder: warm,
 * never broken. Decorative — the title text is the card's accessible name, so no description.
 */
@Composable
private fun HomeCardThumbnail(card: HomeZineCard, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val thumbnail = card.thumbnail
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().testTag(homeCardThumbnailTestTag(card.id)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(homeCardThumbnailPlaceholderTestTag(card.id)),
            )
        }
    }
}

/**
 * The gentle rename dialog (VOICE: **[ Keep name ] [ Rename ]**, never "Are you sure"). The draft
 * starts as the current title; a blank draft disables Rename — the VM additionally trims and
 * treats blank as keep, so this is belt over that brace (ADR-044 §4).
 */
@Composable
private fun HomeRenameDialog(
    card: HomeZineCard,
    onRename: (String) -> Unit,
    onKeepName: () -> Unit,
) {
    var draft by remember(card.id) { mutableStateOf(card.title) }
    AlertDialog(
        onDismissRequest = onKeepName,
        modifier = Modifier.testTag(HomeRenameDialogTestTag),
        title = { Text("Rename this zine?") },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                modifier = Modifier.testTag(HomeRenameFieldTestTag),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(draft) },
                enabled = draft.isNotBlank(),
                modifier = Modifier.testTag(HomeRenameConfirmTestTag),
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onKeepName) { Text("Keep name") } },
    )
}

/**
 * The empty shelf — the canonical SCREEN-INVENTORY §Home pairing: warm invitation + its
 * **Start a zine** CTA (S6.3, ADR-044 §4 — this ends the ADR-043 §5 named deviation). In practice
 * this state is unreachable while boot seeds `"default"` (ADR-030 §4) — it exists, warm and
 * tested, for the S6.5 re-root.
 */
@Composable
private fun HomeEmptyShelf(onStartZine: () -> Unit, modifier: Modifier = Modifier) {
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
        Spacer(Modifier.size(16.dp))
        Button(onClick = onStartZine) { Text("Start a zine") }
        Spacer(Modifier.size(16.dp))
        Text(
            text = "works offline · stays on your phone",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
