package com.aritr.zinely.data.android

import kotlinx.coroutines.withTimeoutOrNull

/**
 * The ADR-044 §1 open-editor exclusion seam: [RoomProjectRepository] consults this before any
 * id-targeted mutation (rename/delete gate the target, duplicate its source) so a mutation can
 * never race a live editor autosave session's out-of-mutex `document.json` writes (the ADR-042
 * hard invariant — e.g. a teardown flush resurrecting a just-deleted project).
 */
internal fun interface ProjectSessionGate {
    /**
     * Suspend until [projectId] has no live editor session, within the implementation's time bound.
     * Returns `true` when the id is session-free (the common case is instantly — no session, or a
     * closing editor's async release landing within the bound) and `false` when a session is still
     * live at the bound, in which case the caller refuses the mutation with [DataError.Busy].
     */
    suspend fun awaitNoSession(projectId: String): Boolean
}

/**
 * The production gate: [AutosaveCoordinatorFactory.awaitReleased] bounded by [timeoutMs]. Under the
 * nav invariants (shelf and editor never simultaneously active) the only real wait is the editor's
 * asynchronous teardown flush, which completes well inside the default bound; an actively-open
 * session never releases, so the bound is what turns a would-be hang into an honest `Busy`.
 */
internal class AutosaveSessionGate(
    private val factory: AutosaveCoordinatorFactory,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : ProjectSessionGate {

    override suspend fun awaitNoSession(projectId: String): Boolean =
        withTimeoutOrNull(timeoutMs) { factory.awaitReleased(projectId) } != null

    internal companion object {
        /** Generous vs the teardown flush (one atomic write + fsync), tiny vs a user retry. */
        const val DEFAULT_TIMEOUT_MS = 5_000L
    }
}
