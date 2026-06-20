package com.aritr.zinely.core.data.serialization

import kotlinx.serialization.json.JsonObject

/**
 * A pure, single-step forward migrator that upgrades a document's JSON from [fromVersion] to
 * [toVersion] (always `fromVersion + 1`). Migrators transform **structure only** — they must not
 * touch the `schemaVersion` field; [DocumentMigrations] owns that bookkeeping and the chain order.
 *
 * Migrators are pure functions (no I/O, deterministic) so they are unit-tested against golden
 * old→new JSON fixtures, mirroring the imposition-engine test style (S2 spike §6/§9).
 *
 * **Internal by design** ([ADR-020], Codex review 2026-06-19): JSON-tree migration is an
 * implementation detail of [JsonDocumentSerializer], **not** the app-wide migration abstraction.
 * The format-neutral contract is [DocumentSerializer]; a future non-JSON format would carry its own
 * migration mechanism behind that same boundary. Keeping this `internal` stops JSON-tree transforms
 * from leaking outward and making the format-swap escape hatch cosmetic.
 */
internal interface DocumentMigrator {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(input: JsonObject): JsonObject
}
