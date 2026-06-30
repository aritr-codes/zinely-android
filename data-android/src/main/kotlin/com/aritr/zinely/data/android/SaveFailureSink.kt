package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The latest unresolved autosave failure for one project (ADR-026 §5). [failureCount] is the number
 * of failures **reported since the last [SaveFailureSink.clear]** for this project. Since ADR-037 the
 * coordinator's synchronous outcome listener `clear`s the project on every **durably-confirmed save**,
 * so this is now a true **consecutive-failure** tally (it resets to zero the moment a save succeeds),
 * not merely since-last-manual-dismiss. Each completed save delivers exactly one outcome (no lossy
 * transport), so no failure goes untallied.
 */
public data class SaveFailure(
    val projectId: String,
    val error: DataError,
    val failureCount: Int,
)

/**
 * Application-scoped, in-memory sink for autosave failures (ADR-026 §5). The
 * [AutosaveCoordinatorFactory][com.aritr.zinely.data.android.AutosaveCoordinatorFactory] is the sole
 * feeder: it wires the [AutosaveCoordinator][com.aritr.zinely.core.data.storage.AutosaveCoordinator]'s
 * synchronous outcome listener so a failed save calls [report] and a **durably-confirmed** save calls
 * [clear] (ADR-037 silent-recovery clear). The editor observes [failures] to surface a "couldn't save"
 * cue; the user can also dismiss it explicitly via [clear].
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
