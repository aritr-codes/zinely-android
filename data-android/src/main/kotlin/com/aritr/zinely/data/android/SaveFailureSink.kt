package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The latest unresolved autosave failure for one project (ADR-026 §5). [failureCount] is the number
 * of failures **reported since the last [SaveFailureSink.clear]** for this project — the frozen
 * autosave coordinator emits failures only (no success events), so it is a since-last-clear tally,
 * not a strictly-consecutive one. It also counts only failures the collector actually observed: the
 * coordinator's `failures` flow drops oldest on overflow, so a dropped failure is not tallied here.
 */
public data class SaveFailure(
    val projectId: String,
    val error: DataError,
    val failureCount: Int,
)

/**
 * Application-scoped, in-memory sink for background autosave failures (ADR-026 §5). A later wiring
 * step collects the [AutosaveCoordinator][com.aritr.zinely.core.data.storage.AutosaveCoordinator]'s
 * `failures` flow into [report]; the editor observes [failures] to surface a "couldn't save" cue and
 * calls [clear] once the failure is resolved or dismissed.
 *
 * **In-memory only:** failures do not survive process death. On relaunch the editor reloads the
 * durable `document.json`, which is itself the recovery (ADR-026 accepted limitation) — so no
 * cross-process persistence is needed or provided here.
 *
 * A consumer interested in a single project derives its slice without a per-key flow:
 * `failures.map { it[projectId] }.distinctUntilChanged()`.
 */
public interface SaveFailureSink {
    /** Latest unresolved failure per project id; an absent key means no known failure. */
    public val failures: StateFlow<Map<String, SaveFailure>>

    /** Record [error] as the latest failure for [projectId], bumping its since-last-clear count. */
    public fun report(projectId: String, error: DataError)

    /** Discard any recorded failure for [projectId] (resolved or user-dismissed). */
    public fun clear(projectId: String)

    /** Discard every recorded failure (e.g. project switch, workspace reset). */
    public fun clearAll()
}

/**
 * Default [SaveFailureSink]. State is a single [MutableStateFlow] of the per-project map; [report]
 * and [clear] mutate it with [update], whose lock-free compare-and-set is safe for the concurrent
 * callers this sink has (the coordinator's IO dispatcher reporting in, the UI thread observing).
 * The update lambdas are pure (no side effects), as `update` may retry them under contention.
 */
public class InMemorySaveFailureSink : SaveFailureSink {

    private val _failures = MutableStateFlow<Map<String, SaveFailure>>(emptyMap())
    override val failures: StateFlow<Map<String, SaveFailure>> = _failures.asStateFlow()

    override fun report(projectId: String, error: DataError) {
        _failures.update { current ->
            val priorCount = current[projectId]?.failureCount ?: 0
            current + (projectId to SaveFailure(projectId, error, priorCount + 1))
        }
    }

    override fun clear(projectId: String) {
        // Return the same map when the key is absent so the StateFlow does not emit a no-op change.
        _failures.update { current -> if (projectId in current) current - projectId else current }
    }

    override fun clearAll() {
        _failures.update { current -> if (current.isEmpty()) current else emptyMap() }
    }
}
