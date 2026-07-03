package com.aritr.zinely.data.android.di

import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.AutosaveSessionGate
import com.aritr.zinely.data.android.EditorAutosaveBinder
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * Hand-written assisted factory for [EditorAutosaveBinder] (PR-A Step 7, design §10/§11). Chosen over
 * Hilt `@AssistedInject`/`@AssistedFactory` so the **frozen** [EditorAutosaveBinder] keeps zero DI
 * annotations and no `dagger.assisted` coupling. The three application-scoped collaborators are
 * constructor-injected; [projectId] and [snapshotProvider] are the runtime parameters supplied when a
 * project is opened.
 *
 * Constructor-injectable (`@Inject`), so Hilt provides it with no `@Provides` method. It is the only
 * `@Inject` constructor introduced by Step 7 — and it is a **new** class, never a frozen adapter.
 *
 * The binder no longer needs the [com.aritr.zinely.data.android.SaveFailureSink]: every save outcome
 * reaches the sink through the coordinator's outcome listener, wired by [AutosaveCoordinatorFactory]
 * (ADR-037).
 */
public class EditorAutosaveBinderFactory @Inject constructor(
    private val factory: AutosaveCoordinatorFactory,
    // @param: pins the qualifier to the constructor value parameter (what Dagger reads) and opts out
    // of the KT-73255 default-target migration warning.
    @param:AutosaveScope private val autosaveScope: CoroutineScope,
) {
    /** The ADR-046 §2 reopen gate — the SAME AutosaveSessionGate policy (and 5 s bound) the repository's
     * mutation gate uses, so the editor and the store can never drift on what "busy" means. */
    private val sessionGate = AutosaveSessionGate(factory)

    /**
     * Await the single-writer slot for [projectId] before reopening it (ADR-030 Rec1, realised by
     * ADR-046 §2): a just-popped editor's release is asynchronous (teardown flush → unregister), so a
     * fast reopen must wait for it rather than race [create]'s duplicate-id rejection. Returns `true`
     * when the slot is free (immediately when no session exists) and `false` when a session is still
     * live at the gate's bound — the caller surfaces a warm "still saving" boot error, never a hang.
     */
    public suspend fun awaitNoSession(projectId: String): Boolean =
        sessionGate.awaitNoSession(projectId)

    /**
     * Build the binder for an open project. Constructing the binder **eagerly registers** the project
     * with the single-writer factory (ADR-026 §2), so call this only with a real, currently-open
     * [projectId]; the caller (editor layer) owns disposing the returned binder when the project closes.
     */
    public fun create(
        projectId: String,
        snapshotProvider: DocumentSnapshotProvider,
    ): EditorAutosaveBinder =
        EditorAutosaveBinder(factory, projectId, snapshotProvider, autosaveScope)
}
