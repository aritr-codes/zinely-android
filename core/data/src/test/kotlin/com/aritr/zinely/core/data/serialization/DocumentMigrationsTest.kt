package com.aritr.zinely.core.data.serialization

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The migration framework chains pure `vN → vN+1` JSON transforms up to a target version (S2 spike
 * §6, [ADR-020]). Migrators see structure only; the registry owns the `schemaVersion` bookkeeping
 * and the chain-integrity guarantees. With `CURRENT_SCHEMA_VERSION == 1` the app ships zero real
 * migrators, so these tests drive the framework with synthetic ones.
 */
class DocumentMigrationsTest {

    /** A test migrator that stamps `vN_applied = true` so we can observe which steps ran. */
    private fun stamping(from: Int): DocumentMigrator = object : DocumentMigrator {
        override val fromVersion = from
        override val toVersion = from + 1
        override fun migrate(input: JsonObject): JsonObject = buildJsonObject {
            input.forEach { (k, v) -> put(k, v) }
            put("v${from}_applied", true)
        }
    }

    private fun doc(version: Int): JsonObject = buildJsonObject { put("schemaVersion", version) }

    @Test
    fun `document already at target is returned unchanged`() {
        val migrations = DocumentMigrations(emptyList(), targetVersion = 1)
        val input = doc(1)
        assertEquals(input, migrations.migrate(input))
    }

    @Test
    fun `single migrator upgrades v1 to v2 and bumps schemaVersion`() {
        val migrations = DocumentMigrations(listOf(stamping(1)), targetVersion = 2)
        val out = migrations.migrate(doc(1))
        assertEquals(2, out["schemaVersion"]!!.jsonPrimitive.int)
        assertEquals(JsonPrimitive(true), out["v1_applied"])
    }

    @Test
    fun `chained migrators upgrade v1 through v3 in order`() {
        val migrations = DocumentMigrations(listOf(stamping(1), stamping(2)), targetVersion = 3)
        val out = migrations.migrate(doc(1))
        assertEquals(3, out["schemaVersion"]!!.jsonPrimitive.int)
        assertEquals(JsonPrimitive(true), out["v1_applied"])
        assertEquals(JsonPrimitive(true), out["v2_applied"])
    }

    @Test
    fun `migration starts at the document version not the lowest migrator`() {
        val migrations = DocumentMigrations(listOf(stamping(1), stamping(2)), targetVersion = 3)
        val out = migrations.migrate(doc(2))
        assertEquals(3, out["schemaVersion"]!!.jsonPrimitive.int)
        assertNull(out["v1_applied"], "v1 migrator must not run for a v2 document")
        assertEquals(JsonPrimitive(true), out["v2_applied"])
    }

    @Test
    fun `a gap in the migrator chain throws MissingMigrator`() {
        val migrations = DocumentMigrations(listOf(stamping(1)), targetVersion = 3)
        val ex = assertThrows<MissingMigratorException> { migrations.migrate(doc(1)) }
        assertEquals(2, ex.fromVersion)
    }

    @Test
    fun `a non-contiguous migrator step is rejected at construction`() {
        val skip = object : DocumentMigrator {
            override val fromVersion = 1
            override val toVersion = 3 // illegal: must be fromVersion + 1
            override fun migrate(input: JsonObject) = input
        }
        assertThrows<InvalidMigratorChainException> { DocumentMigrations(listOf(skip), targetVersion = 3) }
    }

    @Test
    fun `duplicate fromVersion migrators are rejected at construction`() {
        assertThrows<InvalidMigratorChainException> {
            DocumentMigrations(listOf(stamping(1), stamping(1)), targetVersion = 2)
        }
    }

    @Test
    fun `a missing schemaVersion is rejected`() {
        val migrations = DocumentMigrations(emptyList(), targetVersion = 1)
        assertThrows<MissingSchemaVersionException> { migrations.migrate(buildJsonObject { put("format", "single_sheet_8") }) }
    }

    @Test
    fun `a newer-than-target document is refused, not silently degraded`() {
        val migrations = DocumentMigrations(listOf(stamping(1)), targetVersion = 2)
        val ex = assertThrows<NewerSchemaVersionException> { migrations.migrate(doc(5)) }
        assertEquals(5, ex.documentVersion)
        assertEquals(2, ex.supportedVersion)
    }
}
