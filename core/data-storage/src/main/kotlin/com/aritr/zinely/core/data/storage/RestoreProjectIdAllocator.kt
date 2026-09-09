package com.aritr.zinely.core.data.storage

/**
 * Allocates the local ids used by an additive library restore (ADR-110).
 *
 * A source id is retained when it is safe and free. A collision never overwrites: [mintId] is called
 * until it returns a safe id absent from both the current library and this restore batch. The Android
 * adapter supplies its existing UUID generator; keeping this decision pure makes collision behavior
 * deterministic and independently testable before Room or SAF is involved.
 */
public object RestoreProjectIdAllocator {
    public fun allocate(
        sourceProjectIds: List<String>,
        existingProjectIds: Set<String>,
        mintId: () -> String,
    ): List<String> {
        require(sourceProjectIds.distinct().size == sourceProjectIds.size) {
            "A restore cannot contain duplicate source project ids"
        }
        require(existingProjectIds.all(SAFE_ID::matches)) { "Existing project ids must be safe" }

        val occupied = existingProjectIds.toMutableSet()
        return sourceProjectIds.map { sourceId ->
            require(SAFE_ID.matches(sourceId)) { "Source project id is unsafe" }
            val localId = if (occupied.add(sourceId)) {
                sourceId
            } else {
                mintUnique(occupied, mintId)
            }
            localId
        }
    }

    private fun mintUnique(occupied: MutableSet<String>, mintId: () -> String): String {
        repeat(MAX_MINT_ATTEMPTS) {
            val candidate = mintId()
            require(SAFE_ID.matches(candidate)) { "Minted project id is unsafe" }
            if (occupied.add(candidate)) return candidate
        }
        error("Could not mint a unique local project id")
    }

    private const val MAX_MINT_ATTEMPTS: Int = 1_000
    private val SAFE_ID: Regex = Regex("^[A-Za-z0-9_-]{1,64}$")
}
