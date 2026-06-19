package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Base type for any failure while migrating a persisted document. */
public sealed class DocumentMigrationException(message: String) :
    DocumentSerializationException(message)

/** The document JSON has no readable `schemaVersion` — it cannot be migrated safely. */
public class MissingSchemaVersionException :
    DocumentMigrationException("Document JSON is missing a readable integer 'schemaVersion'")

/** No migrator is registered to upgrade from [fromVersion] to [fromVersion] + 1. */
public class MissingMigratorException(public val fromVersion: Int) :
    DocumentMigrationException("No migrator registered for schema v$fromVersion -> v${fromVersion + 1}")

/** The supplied migrator set is not a contiguous, non-overlapping `vN -> vN+1` chain. */
public class InvalidMigratorChainException(message: String) : DocumentMigrationException(message)

/**
 * Ordered registry of [DocumentMigrator]s that upgrades a document's JSON to [targetVersion] (S2
 * spike §6, [ADR-020]). The registry — not the migrators — owns the `schemaVersion` field: after
 * each step it stamps the new version, so migrators stay focused on structure.
 *
 * Behaviour:
 * - **at target** → returned unchanged;
 * - **older** → each `vN -> vN+1` migrator applied in order; a gap throws [MissingMigratorException];
 * - **newer than target** → returned unchanged so the serializer can decode tolerantly
 *   (`ignoreUnknownKeys`) rather than corrupt a document written by a future app version (spike §6).
 *
 * Chain integrity is validated **at construction**: every migrator must satisfy
 * `toVersion == fromVersion + 1` and no two may share a `fromVersion`.
 */
public class DocumentMigrations(
    migrators: List<DocumentMigrator>,
    public val targetVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    private val byFromVersion: Map<Int, DocumentMigrator>

    init {
        val map = HashMap<Int, DocumentMigrator>(migrators.size)
        for (migrator in migrators) {
            if (migrator.toVersion != migrator.fromVersion + 1) {
                throw InvalidMigratorChainException(
                    "Migrator ${migrator.fromVersion}->${migrator.toVersion} is not a single step " +
                        "(toVersion must equal fromVersion + 1)",
                )
            }
            if (map.put(migrator.fromVersion, migrator) != null) {
                throw InvalidMigratorChainException(
                    "Duplicate migrator for schema v${migrator.fromVersion}",
                )
            }
        }
        byFromVersion = map
    }

    /** Migrate [input] up to [targetVersion], stamping `schemaVersion` after each applied step. */
    public fun migrate(input: JsonObject): JsonObject {
        val version = readSchemaVersion(input)
        if (version >= targetVersion) return input

        var current = input
        var fromVersion = version
        while (fromVersion < targetVersion) {
            val migrator = byFromVersion[fromVersion] ?: throw MissingMigratorException(fromVersion)
            current = withSchemaVersion(migrator.migrate(current), migrator.toVersion)
            fromVersion = migrator.toVersion
        }
        return current
    }

    private fun readSchemaVersion(input: JsonObject): Int {
        val raw = input["schemaVersion"] ?: throw MissingSchemaVersionException()
        return (raw as? JsonPrimitive)?.intOrNull ?: throw MissingSchemaVersionException()
    }

    private fun withSchemaVersion(obj: JsonObject, version: Int): JsonObject =
        JsonObject(obj.toMutableMap().apply { put("schemaVersion", JsonPrimitive(version)) })
}
