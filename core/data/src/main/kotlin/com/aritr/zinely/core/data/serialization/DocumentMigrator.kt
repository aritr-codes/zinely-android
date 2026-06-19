package com.aritr.zinely.core.data.serialization

import kotlinx.serialization.json.JsonObject

/**
 * A pure, single-step forward migrator that upgrades a document's JSON from [fromVersion] to
 * [toVersion] (always `fromVersion + 1`). Migrators transform **structure only** — they must not
 * touch the `schemaVersion` field; [DocumentMigrations] owns that bookkeeping and the chain order.
 *
 * Migrators are pure functions (no I/O, deterministic) so they are unit-tested against golden
 * old→new JSON fixtures, mirroring the imposition-engine test style (S2 spike §6/§9).
 */
public interface DocumentMigrator {
    public val fromVersion: Int
    public val toVersion: Int
    public fun migrate(input: JsonObject): JsonObject
}
