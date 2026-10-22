package com.aritr.zinely.core.render

import java.io.File
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The provenance model ([SupplyProvenance], ADR-139): every Art piece says where it came from, and the
 * claim is checkable against bytes.
 *
 * The frozen HTML is the spec. `v21-bench.html` A39 holds the 124 ids and which of them are redrawn or
 * imported; `v21-colophon.html` holds the six credited sets and their notice text. This test reads both
 * so a later edit to either one, or to the Kotlin, cannot drift without a red test. Upstream SVGs and
 * licence texts are **test resources** (`art-provenance/<set>/`), copied byte-for-byte from the A39
 * archive; they never enter the APK.
 */
class SupplyProvenanceTest {

    private val allowedSpdx = setOf("MIT", "ISC", "Apache-2.0", "CC0-1.0", "PDM")

    // -- the frozen spec, parsed from the HTML --

    private class Row(val id: String, val name: String)
    private class Meta(val subject: String, val source: String, val redraw: Boolean)

    private fun repoFile(relative: String): File {
        // Gradle runs module tests with the module directory as the working directory.
        val candidates = listOf(File("../../$relative"), File(relative))
        return candidates.firstOrNull { it.isFile } ?: error("cannot find $relative from ${File(".").absolutePath}")
    }

    private val benchHtml: String by lazy { repoFile("docs/design/mockups/v21-bench.html").readText() }
    private val colophonHtml: String by lazy { repoFile("docs/design/mockups/v21-colophon.html").readText() }

    private val a39Rows: List<Row> by lazy {
        val section = benchHtml.substringAfter("const ART_A39=[").substringBefore("// id: [subject")
        Regex("""^\s*\['(?:[^'\\]|\\.)*','([a-z]+\.[a-z]+)','((?:[^'\\]|\\.)*)'""", RegexOption.MULTILINE)
            .findAll(section).map { Row(it.groupValues[1], it.groupValues[2]) }.toList()
    }

    private val a39Meta: Map<String, Meta> by lazy {
        val section = benchHtml.substringAfter("const A39_META={").substringBefore("};")
        Regex("""'([a-z]+\.[a-z]+)':\['((?:[^'\\]|\\.)*)','((?:[^'\\]|\\.)*)',(true|false),""")
            .findAll(section).associate { it.groupValues[1] to Meta(it.groupValues[2], it.groupValues[3], it.groupValues[4] == "true") }
    }

    private class Credit(
        val key: String,
        val title: String,
        val count: Int,
        val licence: String,
        val holder: String,
        val notice: String,
    )

    private fun unescapeJs(raw: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\') { out.append(c); i++; continue }
            when (val n = raw[i + 1]) {
                'n' -> out.append('\n')
                't' -> out.append('\t')
                'u' -> { out.append(raw.substring(i + 2, i + 6).toInt(16).toChar()); i += 4 }
                else -> out.append(n)
            }
            i += 2
        }
        return out.toString()
    }

    private val credits: List<Credit> by lazy {
        val section = colophonHtml.substringAfter("const ART_CREDITS=[").substringBefore("\n(function(){")
        // Possessive quantifiers: the notices are thousands of characters long, and a plain
        // alternation under * recurses once per character and overflows the stack.
        val str = """"((?:[^"\\]++|\\.)*+)""""
        Regex("""\[\s*$str,\s*$str,\s*(\d+),\s*$str,\s*$str,\s*$str\s*]""")
            .findAll(section).map {
                Credit(
                    key = unescapeJs(it.groupValues[1]), title = unescapeJs(it.groupValues[2]),
                    count = it.groupValues[3].toInt(), licence = unescapeJs(it.groupValues[4]),
                    holder = unescapeJs(it.groupValues[5]), notice = unescapeJs(it.groupValues[6]),
                )
            }.toList()
    }

    private fun resource(path: String): ByteArray {
        val stream = SupplyProvenanceTest::class.java.classLoader.getResourceAsStream("art-provenance/$path")
        assertNotNull(stream, "missing test resource art-provenance/$path")
        return stream!!.use { it.readBytes() }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // -- the tests --

    @Test
    fun `the parsed spec is the 124 ids, 17 redraws, 40 imports and six sets the freeze names`() {
        assertEquals(124, a39Rows.size)
        assertEquals(124, a39Rows.map { it.id }.toSet().size)
        assertEquals(17, a39Meta.values.count { it.redraw })
        assertEquals(6, credits.size)
        assertEquals(40, credits.sumOf { it.count })
    }

    @Test
    fun `given every shipped outline and every A39 id, when looked up, then each has a kind`() {
        val ids = SupplyCatalog.OUTLINES.keys + a39Rows.map { it.id }
        ids.forEach { id ->
            assertNotNull(SupplyProvenanceCatalog.of(id), "$id has no provenance record")
        }
        // The catalogue is exactly the 124 A39 ids; every wired outline is one of them.
        assertEquals(a39Rows.map { it.id }.toSet(), SupplyProvenanceCatalog.ALL.map { it.id }.toSet())
        assertEquals(SupplyProvenanceCatalog.ALL.size, SupplyProvenanceCatalog.ALL.map { it.id }.toSet().size)
        assertTrue(SupplyCatalog.OUTLINES.keys.all { SupplyProvenanceCatalog.of(it) != null })
    }

    @Test
    fun `given the A39 metadata, when kinds are compared, then redrawn and imported match the freeze`() {
        val setByTitle = ArtSet.entries.associateBy { it.title }
        a39Rows.forEach { row ->
            val meta = a39Meta.getValue(row.id)
            val record = SupplyProvenanceCatalog.getValue(row.id)
            val expected = when {
                setByTitle.containsKey(meta.source) -> ProvenanceKind.IMPORTED
                meta.redraw -> ProvenanceKind.REDRAWN
                else -> ProvenanceKind.DRAWN
            }
            assertEquals(expected, record.kind, "${row.id} kind")
            if (expected == ProvenanceKind.IMPORTED) {
                assertEquals(setByTitle.getValue(meta.source), record.imported!!.set, "${row.id} set")
            } else {
                assertEquals(null, record.imported, "${row.id} must not carry an import record")
            }
        }
    }

    @Test
    fun `given an import, when its record is read, then it is complete and pinned`() {
        val imported = SupplyProvenanceCatalog.ALL.filter { it.kind == ProvenanceKind.IMPORTED }
        assertEquals(40, imported.size)
        val pin = Regex("""/([0-9a-f]{40}|v?\d+\.\d+\.\d+)/""")
        imported.forEach { record ->
            val source = record.imported!!
            assertTrue(allowedSpdx.contains(record.spdx), "${record.id} SPDX ${record.spdx}")
            assertTrue(source.itemUrl.startsWith("https://"), "${record.id} url")
            assertTrue(pin.containsMatchIn(source.itemUrl), "${record.id} url is not at a pin: ${source.itemUrl}")
            assertTrue(Regex("""\d{4}-\d{2}-\d{2}""").matches(source.retrieved), "${record.id} date ${source.retrieved}")
            assertTrue(source.modification.isNotBlank(), "${record.id} modification")
            assertTrue(source.file.startsWith("${source.set.key}/"), "${record.id} file under its set directory")
        }
    }

    @Test
    fun `given every set, when its SPDX is read, then it is on the allowed list`() {
        ArtSet.entries.forEach { assertTrue(allowedSpdx.contains(it.spdx), "${it.key} ${it.spdx}") }
    }

    @Test
    fun `given each upstream file, when hashed, then the sha256 equals the record`() {
        SupplyProvenanceCatalog.ALL.mapNotNull { it.imported }.forEach {
            assertEquals(it.sha256, sha256(resource(it.file)), "sha256 of ${it.file}")
        }
        ArtSet.entries.forEach { set ->
            assertTrue(set.licenceFiles.isNotEmpty(), "${set.key} ships no licence text")
            set.licenceFiles.forEach {
                assertEquals(it.sha256, sha256(resource("${set.key}/${it.name}")), "sha256 of ${set.key}/${it.name}")
            }
        }
    }

    @Test
    fun `given the resource directory, when listed, then every file is a record and nothing else`() {
        val root = File(SupplyProvenanceTest::class.java.classLoader.getResource("art-provenance")!!.toURI())
        val onDisk = root.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
        val recorded = SupplyProvenanceCatalog.ALL.mapNotNull { it.imported?.file }.toSet() +
            ArtSet.entries.flatMap { set -> set.licenceFiles.map { "${set.key}/${it.name}" } }
        assertEquals(recorded, onDisk)
    }

    @Test
    fun `given the colophon freeze, when compared, then the Kotlin sets are the credits list`() {
        assertEquals(credits.map { it.key }, ArtSet.entries.map { it.key })
        credits.forEach { credit ->
            val set = ArtSet.entries.single { it.key == credit.key }
            assertEquals(credit.title, set.title)
            assertEquals(credit.licence, set.licenceLabel)
            assertEquals(credit.holder, set.holder)
            assertEquals(credit.count, SupplyProvenanceCatalog.piecesIn(set).size, "${set.key} piece count")
        }
        // Every set is credited: no set is empty, none is missing from the credits list.
        ArtSet.entries.forEach { assertTrue(SupplyProvenanceCatalog.piecesIn(it).isNotEmpty(), "${it.key} has no pieces") }
    }

    @Test
    fun `given the bundled notices, when read, then each equals the freeze and names every piece`() {
        val names = a39Rows.associate { it.id to unescapeJs(it.name) }
        credits.forEach { credit ->
            val file = repoFile("feature/editor/src/main/assets/licences/art/${credit.key}.txt")
            val text = file.readText(Charsets.UTF_8)
            assertEquals(credit.notice, text.replace("\r\n", "\n"), "${credit.key} notice differs from the frozen colophon")
            val set = ArtSet.entries.single { it.key == credit.key }
            SupplyProvenanceCatalog.piecesIn(set).forEach { record ->
                assertTrue(text.contains(names.getValue(record.id)), "${credit.key} notice omits '${names.getValue(record.id)}' (${record.id})")
            }
            set.licenceFiles.filter { !it.name.endsWith(".html") }
                .forEach {
                    val licence = String(resource("${set.key}/${it.name}"), Charsets.UTF_8).trim().replace("\r\n", "\n")
                    assertTrue(text.contains(licence.take(200)), "${credit.key} notice does not carry ${it.name}")
                }
        }
    }
}
