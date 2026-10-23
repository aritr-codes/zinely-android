package com.aritr.zinely.core.render

/**
 * Where an Art piece came from (ADR-139, ADR-137 "each piece's entry").
 *
 *  - [DRAWN]: in-house, from nothing (the 17 shipped pieces that keep their drawing, and every A34/A39
 *    variant we drew).
 *  - [REDRAWN]: a shipped id whose drawing the A39 freeze replaced with an in-house redraw (ADR-132,
 *    ADR-139: 17 ids). Still in-house, so it carries no third-party notice.
 *  - [IMPORTED]: silhouetted or re-stroked from an open-source set (40 pieces, six sets). Carries a
 *    notice in Licences & credits, never in a zine, its PDF or its pictures (ADR-104).
 */
public enum class ProvenanceKind { DRAWN, REDRAWN, IMPORTED }

/** One upstream licence text kept as a test resource, with the sha256 it must still have. */
public data class LicenceFile(val name: String, val sha256: String)

/**
 * The six open-source sets Art pieces were imported from, in the order Licences & credits lists them
 * (`v21-colophon.html` A39). [key] names the bundled notice (`assets/licences/art/<key>.txt`) and the
 * test-resource directory (`art-provenance/<key>/`); [title], [licenceLabel] and [holder] are the credit
 * row's words, frozen in the colophon mockup.
 *
 * Production code uses the strings (the credits screen) and nothing else: the upstream files and
 * [licenceFiles] are verified by `SupplyProvenanceTest` against test resources that never ship.
 */
public enum class ArtSet(
    public val key: String,
    public val title: String,
    /** SPDX id of the licence the pieces are used under. */
    public val spdx: String,
    /** The licence as the credit row prints it ("CC0 1.0", not "CC0-1.0"). */
    public val licenceLabel: String,
    public val holder: String,
    public val licenceFiles: List<LicenceFile>,
) {
    DOODLE(
        "doodle", "Doodle Icons by Khushmeen Sidhu", "CC0-1.0", "CC0 1.0", "Khushmeen Sidhu",
        listOf(LicenceFile("CC0-1.0-legalcode.txt", "a2010f343487d3f7618affe54f789f5487602331c0a8d03f49e9a7c547cf0499"), LicenceFile("creator-page-icons.html", "fb463b4db73a4c4e51f93fd1d9dc1804fc37eb00329831e5d503018244b17ce8"), LicenceFile("MIRROR-LICENSE.txt", "02445da25615635ac61f567282464d2a3a8843153f6f4a02b1621aa42546b402")),
    ),
    OREO(
        "oreo", "Doodle Icons by Oreo Design", "MIT", "MIT", "Nicole Tang",
        listOf(LicenceFile("LICENSE", "1e421a86e86d79a09920314e2b3768f5d44c8a50e71d991ba0af131b511a8e35")),
    ),
    FLUENT(
        "fluent", "Fluent Emoji (High Contrast)", "MIT", "MIT", "Microsoft Corporation",
        listOf(LicenceFile("LICENSE", "c2cfccb812fe482101a8f04597dfc5a9991a6b2748266c47ac91b6a5aae15383")),
    ),
    LUCIDE(
        "lucide", "Lucide", "ISC", "ISC", "Lucide Icons and Contributors",
        listOf(LicenceFile("LICENSE", "b495047bd93a9b06913511076f504daba17d5bbeb3e0650f3bb53a4220329c57")),
    ),
    PHOSPHOR(
        "phosphor", "Phosphor Icons", "MIT", "MIT", "Phosphor Icons",
        listOf(LicenceFile("LICENSE", "b5b1f1da112d18ea2147decfd48ddc1bf2b5aeb6c265381579340e95b15a2bb2")),
    ),
    TABLER(
        "tabler", "Tabler Icons", "MIT", "MIT", "Paweł Kuna",
        listOf(LicenceFile("LICENSE", "b740a1d46122672da62833e97f7e7c8a13fa85cbc7445b584b297cc00dde93db")),
    ),    ;
}

/** The import half of a record: which file, at which pin, and what we did to it. */
public data class ImportedSource(
    public val set: ArtSet,
    /** The upstream file's URL at the pinned commit or tag (never a moving branch). */
    public val itemUrl: String,
    /** Path under `art-provenance/` of the byte-identical copy kept as a test resource. */
    public val file: String,
    public val sha256: String,
    /** Date the file was fetched from upstream (ISO). */
    public val retrieved: String,
    /** What was done to the upstream drawing to make the piece, in one line. */
    public val modification: String,
)

/** One Art piece's provenance. [imported] is present exactly when [kind] is [ProvenanceKind.IMPORTED]. */
public data class SupplyProvenance(
    public val id: String,
    public val kind: ProvenanceKind,
    public val imported: ImportedSource? = null,
) {
    init {
        require((kind == ProvenanceKind.IMPORTED) == (imported != null)) {
            "$id: an import record is present exactly when the kind is IMPORTED"
        }
    }

    /** SPDX id of the third-party licence, or `null` for an in-house piece. */
    public val spdx: String? get() = imported?.set?.spdx
}

/**
 * The provenance of all 124 A39 Art ids (`v21-bench.html` A39), keyed by the supply id written into
 * saved documents. It is a superset of [SupplyCatalog.OUTLINES] while the families are being wired
 * (ADR-139), so a later family commit cannot add an outline without a record: the test that every id has
 * a kind covers both lists.
 */
public object SupplyProvenanceCatalog {
    private const val RETRIEVED: String = "2026-10-15"

    private fun drawn(id: String) = SupplyProvenance(id, ProvenanceKind.DRAWN)

    private fun redrawn(id: String) = SupplyProvenance(id, ProvenanceKind.REDRAWN)

    private fun imported(
        id: String,
        set: ArtSet,
        itemUrl: String,
        file: String,
        sha256: String,
        modification: String,
    ) = SupplyProvenance(
        id,
        ProvenanceKind.IMPORTED,
        ImportedSource(set, itemUrl, file, sha256, RETRIEVED, modification),
    )

    /** Every record, in A39 order (Tape & fixings, Stamps & marks, Cut paper, Cut shapes). */
    public val ALL: List<SupplyProvenance> = listOf(
        redrawn("tape.torn"),
        drawn("tape.tornb"),
        redrawn("fix.corner"),
        drawn("fix.cornerb"),
        redrawn("fix.staple"),
        redrawn("fix.clip"),
        drawn("fix.clipb"),
        imported(
            "fix.clipc", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/outline/paperclip.svg",
            "tabler/outline_paperclip.svg",
            "f1ede65f6ee259b7d754d8d77d67cb1029bb7d96d7d92f8b469847a641d394ed",
            "Stroke outlined at width 2.5 and fitted to 22.4; no other edit.",
        ),
        redrawn("tape.masking"),
        drawn("tape.maskingb"),
        drawn("fix.stitch"),
        drawn("fix.stitchb"),
        drawn("fix.stitchc"),
        drawn("fix.grommet"),
        drawn("fix.grommetb"),
        drawn("fix.grommetc"),
        drawn("fix.pushpin"),
        drawn("fix.pushpinb"),
        drawn("fix.pushpinc"),
        imported(
            "fix.pushpind", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/pin.svg",
            "oreo/pin.svg",
            "d6f73eb83ed043f7cd1391f4734a37c05bb2424d28c3934343323772d615cab6",
            "Head outline filled solid, needle stroked at width 2.5, side view.",
        ),
        imported(
            "fix.pushpine", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/pin.svg",
            "oreo/pin.svg",
            "d6f73eb83ed043f7cd1391f4734a37c05bb2424d28c3934343323772d615cab6",
            "Same edit as fix.pushpind, rotated 45 degrees.",
        ),
        imported(
            "fix.needle", ArtSet.FLUENT,
            "https://raw.githubusercontent.com/microsoft/fluentui-emoji/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/assets/Sewing%20needle/High%20Contrast/sewing_needle_high_contrast.svg",
            "fluent/sewing_needle_high_contrast.svg",
            "2e1593b75765a552604205665119dd393342e0e5fbff390a00eca327a162fa6f",
            "Line work grown by 0.65 units and fitted to 22.5, drawn in one ink.",
        ),
        imported(
            "tape.plaster", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/outline/bandage.svg",
            "tabler/outline_bandage.svg",
            "dc9cda84c40ba9366e2115f04ae088d5b744e2edb62c3165ee3b2ede3e2fc5db",
            "Stroke outlined at width 2.5; zero-length dot lines became discs; D-131: dots 2.02 wide, spread to 1.5 gaps.",
        ),
        imported(
            "fix.sticker", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/outline/sticker.svg",
            "tabler/outline_sticker.svg",
            "a62fe7bc181ca2453f5d220a7094dfeeddb01233982c27a19a4b553a7f919a99",
            "Stroke outlined at width 2.5; D-131: the peel corners' micro-segments merged into one smooth arc each.",
        ),
        redrawn("mark.asterisk"),
        imported(
            "mark.asteriskb", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/star.svg",
            "doodle/interface_star.svg",
            "559606da628a0fc4c716a2caf7f173fb17b24446859ffdc2469e33b37ffbff31",
            "Outline filled to a solid silhouette (interior hole dropped).",
        ),
        imported(
            "mark.asteriskc", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/bbed884d15354b5cebf2493371f20dc2d5e83eaf/icons/outline/asterisk.svg",
            "tabler/outline_asterisk.svg",
            "88899c951b65b2ffeef2c822edd63a0ac57cbd72f2495b0e385f9776e6b9f2a9",
            "Stroke outlined at width 2.5 and fitted.",
        ),
        redrawn("mark.arrow"),
        drawn("mark.arrowb"),
        drawn("mark.arrowc"),
        imported(
            "mark.arrowd", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/arrows/arrow-right.svg",
            "doodle/arrows_arrow-right.svg",
            "19393fa7bef1731530bcfdbce0e0c81c58078459f607f72c49acf79494a30217",
            "Strokes unioned, outline offset to house weight, fitted 23 wide.",
        ),
        imported(
            "mark.arrowe", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/arrow-right.svg",
            "oreo/arrow-right.svg",
            "c91e35d6ad584b356a314b6602221195f619a96a62b24964f8597eebc0d49d1e",
            "Centrelines re-stroked at width 2.5 and fitted 23 wide.",
        ),
        drawn("mark.halftone"),
        drawn("mark.halftoneb"),
        drawn("mark.registration"),
        drawn("mark.registrationb"),
        drawn("mark.registrationc"),
        redrawn("mark.hand"),
        drawn("mark.handb"),
        imported(
            "mark.handc", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/hand-gestures/point-right.svg",
            "doodle/hand-gestures_point-right.svg",
            "4b6f1443de6a4fec2a218b4223c3358e1ca7acffc333db2148009aaccb152af4",
            "Palm hole dropped to a solid silhouette.",
        ),
        drawn("mark.crop"),
        drawn("mark.cropb"),
        drawn("mark.cropc"),
        redrawn("mark.bar"),
        drawn("mark.barb"),
        drawn("mark.scan"),
        drawn("mark.scanb"),
        drawn("mark.scanc"),
        drawn("mark.perf"),
        drawn("mark.perfb"),
        drawn("mark.perfc"),
        drawn("mark.burst"),
        drawn("mark.burstb"),
        drawn("mark.burstc"),
        imported(
            "mark.burstd", ArtSet.FLUENT,
            "https://raw.githubusercontent.com/microsoft/fluentui-emoji/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/assets/Collision/High%20Contrast/collision_high_contrast.svg",
            "fluent/collision_high_contrast.svg",
            "5b529bc09575cdba963fd9d7457c1794d04983595b3caf5c3407ce4e2b336f96",
            "Knocked-out inner star dropped; solid silhouette.",
        ),
        imported(
            "mark.sparkle", ArtSet.FLUENT,
            "https://raw.githubusercontent.com/microsoft/fluentui-emoji/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/assets/Sparkles/High%20Contrast/sparkles_high_contrast.svg",
            "fluent/sparkles_high_contrast.svg",
            "e81ddf90425004f63d5316c9d3a48f333bd40de23a91c5a6ced599c4aef85d5a",
            "Rescaled and fitted only.",
        ),
        imported(
            "mark.cancel", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/bbed884d15354b5cebf2493371f20dc2d5e83eaf/icons/outline/ripple.svg",
            "tabler/outline_ripple.svg",
            "f288378ee9a00b68b86c86ce71f696d2461b56cfb3b34c671e1cb3d945216733",
            "Stroke outlined at width 2.5 and fitted.",
        ),
        imported(
            "mark.heart", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/heart.svg",
            "doodle/interface_heart.svg",
            "fdba727e07d9f71d9a36641595b39342e9a05533bb0ff883e897eb0c942d0795",
            "Outline interior dropped to a solid silhouette.",
        ),
        imported(
            "mark.tick", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/tick.svg",
            "doodle/interface_tick.svg",
            "2b879e7a06f10fc272d41cf9c4a10023c3add7bc97336fbcc5af3cd158bca201",
            "Outline offset to width 2.5 and fitted 23 wide.",
        ),
        imported(
            "mark.cross", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/cross.svg",
            "doodle/interface_cross.svg",
            "63a90339dd09b813b77647aaa1fbcdb37f3e5a41713cc6939aec77b79d806995",
            "Outline offset to width 2.5 and fitted.",
        ),
        imported(
            "mark.scissors", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/cut.svg",
            "doodle/interface_cut.svg",
            "817e19076b2adf271829f3bd4a43af2af50ebd1c8a752bb288cef93474b88ddc",
            "Thickened to width 2.5; finger holes kept; D-131: both holes 1.2x larger about their centres.",
        ),
        imported(
            "mark.squiggle", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/bbed884d15354b5cebf2493371f20dc2d5e83eaf/icons/outline/scribble.svg",
            "tabler/outline_scribble.svg",
            "d59b8f0290d7536032228ef45fc8b1ad74e21f4e6135f9f4e234b7c6d2f87374",
            "Stroke outlined at width 2.5 and fitted.",
        ),
        redrawn("paper.strip"),
        drawn("paper.stripb"),
        drawn("paper.stripc"),
        drawn("paper.window"),
        drawn("paper.windowb"),
        drawn("paper.windowc"),
        imported(
            "paper.cutwindow", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/square.svg",
            "doodle/interface_square.svg",
            "90f767adce2f4da01359a20242e117caf1a37fe02a5f1ab1d51946e12b30a275",
            "Brush outline kept, ink grown 0.9 per side, fitted.",
        ),
        redrawn("paper.tag"),
        drawn("paper.tagb"),
        imported(
            "paper.chat", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/chat.svg",
            "oreo/chat.svg",
            "7c8eaf364d412144119ae22d0884a3e75493a6f0423e3b11bc3d6b27cc3fa06d",
            "Re-stroked at width 2.5, silhouette taken (dots dropped), scaled.",
        ),
        redrawn("paper.underline"),
        drawn("paper.underlineb"),
        drawn("paper.stub"),
        drawn("paper.stubb"),
        drawn("paper.stubc"),
        imported(
            "paper.ticket", ArtSet.FLUENT,
            "https://raw.githubusercontent.com/microsoft/fluentui-emoji/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/assets/Admission%20tickets/High%20Contrast/admission_tickets_high_contrast.svg",
            "fluent/admission_tickets_high_contrast.svg",
            "ea77e11932393546a9a1640cd3944557be427ad20c579e70ae28239b1606b324",
            "Silhouette of the outer ticket; inner detail dropped.",
        ),
        drawn("paper.stamp"),
        drawn("paper.stampb"),
        drawn("paper.stampc"),
        drawn("paper.deckle"),
        drawn("paper.deckleb"),
        drawn("paper.decklec"),
        drawn("paper.hole"),
        drawn("paper.holeb"),
        drawn("paper.holec"),
        drawn("paper.holed"),
        drawn("paper.dogear"),
        drawn("paper.dogearb"),
        drawn("paper.dogearc"),
        imported(
            "paper.page", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/file.svg",
            "oreo/file.svg",
            "36bdc740ba19c676a226fe7b0ac2cf696e9cf4bb77220032429880e19a6e4cd3",
            "Silhouette; fold line cut out as a 1.8 gap so the flap is its own island.",
        ),
        imported(
            "paper.sticky", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/sticky-note.svg",
            "oreo/sticky-note.svg",
            "be32e61685a3e95a678d5d22fe743c6a01b39cc653adc9f0731132a273563270",
            "Silhouette; fold line cut out as a 1.8 gap.",
        ),
        drawn("paper.border"),
        drawn("paper.borderb"),
        drawn("paper.borderc"),
        drawn("paper.borderd"),
        drawn("paper.round"),
        drawn("paper.roundb"),
        drawn("paper.roundc"),
        imported(
            "paper.balloon", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/message.svg",
            "doodle/interface_message.svg",
            "2ad654460eada964218b7458656baf4bd8b63ea8378ada75c1a6d8baab76d1ab",
            "Brush outline filled to a solid silhouette (dots dropped), scaled, handles smoothed.",
        ),
        imported(
            "paper.thought", ArtSet.FLUENT,
            "https://raw.githubusercontent.com/microsoft/fluentui-emoji/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/assets/Thought%20balloon/High%20Contrast/thought_balloon_high_contrast.svg",
            "fluent/thought_balloon_high_contrast.svg",
            "1994312c051b280fac4568a66b6357bfa1f51b50364e8db8476e8cc42e4521cd",
            "Silhouette of the outline emoji; small trailing bubble shrunk to pass the gap rule.",
        ),
        imported(
            "paper.shout", ArtSet.FLUENT,
            "https://raw.githubusercontent.com/microsoft/fluentui-emoji/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/assets/Right%20anger%20bubble/High%20Contrast/right_anger_bubble_high_contrast.svg",
            "fluent/right_anger_bubble_high_contrast.svg",
            "137cf29f8d123bab58df46cd286c65b00972b300960f04459bc6f02fafc6d818",
            "Silhouette of the outline emoji.",
        ),
        imported(
            "paper.bookmark", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/bookmark.svg",
            "oreo/bookmark.svg",
            "556dc7021c1d95503c709d4fbc3d7db807abf3455d4d907813f80d8a028993ae",
            "Silhouette of the re-stroked outline.",
        ),
        imported(
            "paper.ribbon", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/bookmark.svg",
            "doodle/interface_bookmark.svg",
            "861ffc2eb6f90b5d90bb9da88fed43c3da8257ae4c325d8fe100555f3bcb164a",
            "Brush silhouette, scaled.",
        ),
        imported(
            "paper.gifttag", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/tag.svg",
            "oreo/tag.svg",
            "3c9c4b0ddfb3f431aa98aef4202a8a29c954765691f41737f771650d31d92cb9",
            "Silhouette of the re-stroked outline; eyelet cut as a 3.0 hole.",
        ),
        redrawn("shape.rect"),
        drawn("shape.rectb"),
        imported(
            "shape.rectc", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/square.svg",
            "doodle/interface_square.svg",
            "90f767adce2f4da01359a20242e117caf1a37fe02a5f1ab1d51946e12b30a275",
            "Silhouette of the filled hand-drawn outline, scaled.",
        ),
        imported(
            "shape.rectd", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/rectangle.svg",
            "doodle/interface_rectangle.svg",
            "83abf1c0e9179cd0ec79cebf08ca348bb2c6ce317be6afb153359caf273d99bf",
            "One 0.11-unit step in the top edge removed.",
        ),
        redrawn("shape.circle"),
        redrawn("shape.triangle"),
        drawn("shape.triangleb"),
        redrawn("shape.rule"),
        imported(
            "shape.ruleb", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/outline/ripple.svg",
            "tabler/outline_ripple.svg",
            "f288378ee9a00b68b86c86ce71f696d2461b56cfb3b34c671e1cb3d945216733",
            "Kept one of three waves, re-stroked at house weight, full width.",
        ),
        imported(
            "shape.rulec", ArtSet.LUCIDE,
            "https://raw.githubusercontent.com/lucide-icons/lucide/1.57.0/icons/waves-horizontal.svg",
            "lucide/waves-horizontal.svg",
            "b31f6534ef601e7c138922e99b0557ae61db7e5704c1a05806ecb78b2eaad443",
            "Kept one wave, re-stroked at house weight, full width.",
        ),
        redrawn("shape.ring"),
        drawn("shape.ringb"),
        imported(
            "shape.blob", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/filled/blob.svg",
            "tabler/filled_blob.svg",
            "a84f6e1da4c6f324e33a921cd5b72516affc1db4d52a0cd49720eb1d9fb023a2",
            "Silhouette only, scaled.",
        ),
        imported(
            "shape.scallop", ArtSet.PHOSPHOR,
            "https://raw.githubusercontent.com/phosphor-icons/core/v2.0.8/assets/fill/seal-fill.svg",
            "phosphor/fill_seal-fill.svg",
            "815d2ed46a602145601ac932ad4ea452ee60841fc9f79dd2aa2248ea40b680f9",
            "Silhouette and scale only.",
        ),
        imported(
            "shape.flower", ArtSet.TABLER,
            "https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/filled/flower.svg",
            "tabler/filled_flower.svg",
            "318b3c4bf44b3b943ecba9edfd6774128cd69bbd2323f83e45c00c8109b954fd",
            "Silhouette only, scaled.",
        ),
        imported(
            "shape.hex", ArtSet.PHOSPHOR,
            "https://raw.githubusercontent.com/phosphor-icons/core/v2.0.8/assets/fill/hexagon-fill.svg",
            "phosphor/fill_hexagon-fill.svg",
            "750b85cbc3245bfb5c176fb7ec24bfb5a941f230d30ceec2ec37a069cf519982",
            "Silhouette and scale only.",
        ),
        imported(
            "shape.cloud", ArtSet.DOODLE,
            "https://raw.githubusercontent.com/svatsa159/react-doodle-icons/b51da0371e992b5d03e768fd1f570e8e9500c617/src/icons/interface/cloud.svg",
            "doodle/interface_cloud.svg",
            "803bf37f53f91a2c9d69963134346e3f9155aca06c585a4054a8de82b0548c38",
            "Silhouette and scale only.",
        ),
        imported(
            "shape.cloudb", ArtSet.OREO,
            "https://raw.githubusercontent.com/oreo-design/doodle-icons/38de0ac9fc7524870c8e1f18373cf56f32a5028b/icons/clouds.svg",
            "oreo/clouds.svg",
            "87cc7f507832a465cd42f39a32060ad0126d6c41049321c000548c07e961000a",
            "Kept the large cloud, re-stroked at width 2.5, silhouette scaled.",
        ),
    )

    private val byId: Map<String, SupplyProvenance> = ALL.associateBy { it.id }

    /** The record for [supplyId], or `null` for an id outside the A39 list. */
    public fun of(supplyId: String): SupplyProvenance? = byId[supplyId]

    /** Like [of] but throws for an unknown id. */
    public fun getValue(supplyId: String): SupplyProvenance =
        byId[supplyId] ?: error("no provenance for $supplyId")

    /** The imported pieces taken from [set], in A39 order. */
    public fun piecesIn(set: ArtSet): List<SupplyProvenance> = ALL.filter { it.imported?.set == set }
}
