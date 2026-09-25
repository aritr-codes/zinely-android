package com.aritr.zinely.core.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Schema-shape guard (1.x plan §4 F1a). The persisted shape of [ZineDocument] may change only together
 * with [CURRENT_SCHEMA_VERSION].
 *
 * Why: the document JSON is read with `ignoreUnknownKeys = true`, so a field added without a bump is
 * silently dropped when an older build opens and re-saves the zine (the ADR-113 failure mode). This test
 * walks the generated serializer descriptors — every class, sealed subclass and enum reachable from the
 * root — and compares the result with the checked-in `schema-shape-v<N>.txt` for the current version.
 * No custom serializers exist, so the walk sees the whole wire shape.
 *
 * Limit: a descriptor records whether a field *has* a default, not its value. A changed default is
 * caught by the document fixture corpus in `:core:data` (1.x plan §4 F3), not here.
 *
 * When it fails: if the change is intended, bump [CURRENT_SCHEMA_VERSION] (with its migrator and ADR)
 * and add `schema-shape-v<N+1>.txt` from the rendered shape in the failure message. Never edit an
 * existing shape file to make this pass. One exception: the rendering names a few kotlinx-internal
 * descriptors (`kotlinx.serialization.Sealed<…>`), so a library upgrade can change the text with no
 * wire change. Confirm with the fixture corpus that nothing on disk moved, then re-record the file in a
 * reviewed commit that says so.
 */
class DocumentSchemaShapeTest {

    @Test
    fun `document shape matches the checked-in shape for the current schema version`() {
        val fileName = "schema-shape-v$CURRENT_SCHEMA_VERSION.txt"
        val actual = renderShape(ZineDocument.serializer().descriptor)
        val expected = javaClass.getResource("/$fileName")?.readText()?.replace("\r\n", "\n")

        assertNotNull(expected) {
            "No $fileName. A schema bump needs its own shape file; the current shape is:\n$actual"
        }
        assertEquals(expected, actual) {
            "The persisted ZineDocument shape changed but CURRENT_SCHEMA_VERSION is still " +
                "$CURRENT_SCHEMA_VERSION. Bump the version, or undo the shape change."
        }
    }

    /**
     * One line per type (`serialName kind`), then one indented line per element
     * (`name: type`, plus `(optional)` when it has a default). Types appear once, in breadth-first
     * order from the root. Collections are written inline as `List<T>` and walked into — they are not
     * types of their own, and every list shares one serial name, so deduplicating them by name would
     * silently skip `List<Element>`. Enum entries are listed and not expanded; primitives are leaves.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun renderShape(root: SerialDescriptor): String = buildString {
        val byName = HashMap<String, SerialDescriptor>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val descriptor = queue.removeFirst()
            if (descriptor.isCollection()) {
                repeat(descriptor.elementsCount) { queue.addLast(descriptor.getElementDescriptor(it)) }
                continue
            }
            if (descriptor.kind is PrimitiveKind) continue
            // Types are deduplicated by serial name, so two different types sharing one (say, a future
            // Background subclass named "image") would hide the second one's fields. Refuse instead.
            val first = byName.putIfAbsent(descriptor.serialName, descriptor)
            if (first != null) {
                check(first == descriptor) { "Two different types share the serial name '${descriptor.serialName}'" }
                continue
            }
            appendLine("${descriptor.serialName} ${descriptor.kind}")
            for (i in 0 until descriptor.elementsCount) {
                val child = descriptor.getElementDescriptor(i)
                val optional = if (descriptor.isElementOptional(i)) " (optional)" else ""
                appendLine("  ${descriptor.getElementName(i)}: ${typeName(child)}$optional")
                if (descriptor.kind != SerialKind.ENUM) queue.addLast(child)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun typeName(descriptor: SerialDescriptor): String =
        if (descriptor.isCollection()) {
            (0 until descriptor.elementsCount).joinToString(", ", "${descriptor.kind}<", ">") {
                typeName(descriptor.getElementDescriptor(it))
            }
        } else {
            descriptor.serialName
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun SerialDescriptor.isCollection(): Boolean = kind == StructureKind.LIST || kind == StructureKind.MAP
}
