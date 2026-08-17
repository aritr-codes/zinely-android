package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform

/**
 * Where a supply lands, and at what size — SUPPLIES-SPEC §5, §5.1, §5.2.
 *
 * Pure and platform-free, and it lives here for the same reason `centeredTextBox` (`EditorScreen.kt`)
 * and `defaultImagePlacement` (`AndroidImagePickDecodePipeline.kt`) do: the reducer takes a [Transform]
 * from its caller, and computing this one needs `Copy.Supplies.BY_FAMILY`, which `:core:editor` does
 * not depend on.
 *
 * ### What the spec fixes, and what it left to whoever implemented it
 *
 * Fixed, and implemented verbatim:
 *
 *  - **§5.1 — a supply lands flat, at 0°.** `rotationDegrees` is `0.0` and there is no hash, no seed and
 *    no jitter anywhere in this file. §5.1 withdrew `ZINE-DIRECTION.md` §9.3's deterministic tilt as a
 *    *"compositional decision the app made and the maker didn't"*, and recorded the reversal as one.
 *  - **§5 — page centre.** §5.2's own opening sentence describes a first placement as *"a flat glyph
 *    dropped at page centre"*, which is also what `centeredTextBox` and `defaultImagePlacement` already
 *    do for the other two primitives. One landing spot for all three.
 *  - **§5.2 — each family lands at its own default size**, *"one constant per family"*.
 *
 * ⚠ **Not fixed: the numbers.** §5.2 rules the *rule* and gives three adjectives — *"tape lands long, a
 * stamp lands small, a rule lands wide"* — and no measurements. [BenchSupplyDefaults] below is therefore
 * an implementation reading, not a transcription, and every constant in it is **owed a ruling**. Each is
 * argued from the material at its declaration so the ruling has something to accept or reject.
 *
 * ⚠ **And §5.2's third adjective does not fit its own mechanism.** *"A rule lands wide"* names
 * `shape.rule`, which is **not** a family — it is one of the four *Cut shapes*, alongside the rectangle,
 * the circle and the triangle, and a per-family constant cannot make one of the four wide without making
 * all four wide. So either §5.2 means per-*supply* defaults (and its own "one constant per family" costing
 * is wrong), or the rule example is loose.
 *
 * This implements **both**: per family, plus exactly one named exception ([BenchSupplyOverrides]) for the
 * supply §5.2 names. The first draft implemented only the family rule and left the rule square. Independent
 * review showed why that was the wrong half to drop: *Cut shapes* is the **only** family production can
 * currently reach, so the sole observable effect of §5.2 today was the one example the spec spells out —
 * and it was the one contradicted. `SupplyCatalog`'s own `RULE` KDoc, written before this package, already
 * assumed otherwise in as many words: *"without it the two outlines would be identical and only their
 * default size would differ."* A per-family-only reading makes `shape.rect` and `shape.rule` land in the
 * same box. **The exception is still owed the same ruling as the four constants** — it is not a licence to
 * add a second one without going back to the spec.
 */

/**
 * One family's landing size: [widthFraction] of the page's width, at [aspect] = width ÷ height.
 *
 * A **fraction of the page**, never a point value, for the reason the rest of this codebase already
 * places things that way: the page is 1/8 of a sheet and the sheet is a user setting, so a constant in
 * points would land a supply differently on A4 and Letter for no reason a maker could name.
 *
 * The aspect is carried separately rather than as a second fraction so a family's *proportion* survives
 * the page-height clamp in [benchSupplyPlacement] — a clamped tape stays tape-shaped.
 */
internal data class BenchSupplyDefault(val widthFraction: Double, val aspect: Double)

/**
 * §5.2's one constant per family. **Every number here is owed a ruling** (see this file's header).
 *
 *  - **Tape & fixings — long.** §5.2's own word for tape. Over half the page's width at a 4.5:1 aspect,
 *    which is a strip you can see is a strip; the family's other three (corner, staple, clip) are small
 *    fixings, and they are the strongest argument that §5.2's mechanism is under-specified — but a
 *    family's default is its namesake's, which is the reading the spec's own "tape lands long" supports.
 *  - **Stamps & marks — small.** §5.2's word again. A stamp is punctuation on a page, not an element of
 *    it: 16 % of the page's width, square, which on a typical eighth-sheet page is roughly a thumbprint.
 *  - **Cut paper — a piece of paper.** The largest of the four, because a torn strip, a window frame and
 *    a speech tag are all things you lay *under* or *around* other content rather than beside it.
 *  - **Cut shapes — a drawn shape.** Mid-sized and square: big enough to be a deliberate block of ink,
 *    small enough that the first thing a maker does is not shrink it. See the header's ⚠ on `shape.rule`.
 */
internal val BenchSupplyDefaults: Map<String, BenchSupplyDefault> = mapOf(
    Copy.Supplies.TAPE_AND_FIXINGS to BenchSupplyDefault(widthFraction = 0.55, aspect = 4.5),
    Copy.Supplies.STAMPS_AND_MARKS to BenchSupplyDefault(widthFraction = 0.16, aspect = 1.0),
    Copy.Supplies.CUT_PAPER to BenchSupplyDefault(widthFraction = 0.45, aspect = 1.0),
    Copy.Supplies.CUT_SHAPES to BenchSupplyDefault(widthFraction = 0.30, aspect = 1.0),
)

/**
 * The **one** per-supply exception §5.2 names by name: *"a rule lands wide"*.
 *
 * `shape.rule` is a divider — the thing you put *between* two things — and a divider that arrives as tall
 * as it is wide is not one. It lands at 70 % of the page's width on the outline's own 5:1 proportion (the
 * bar occupies the middle fifth of its box, `SupplyCatalog.RULE`), so the drawn ink is a line rather than a
 * band, and it is visibly not the rectangle its family-mate `shape.rect` places.
 *
 * ⚠ Keep this map at one entry unless the spec grows another example. It exists because §5.2's own
 * illustration names a supply its stated mechanism cannot express — not because per-supply sizing is the
 * design. Every additional entry is a per-family constant quietly giving up.
 */
internal val BenchSupplyOverrides: Map<String, BenchSupplyDefault> = mapOf(
    "shape.rule" to BenchSupplyDefault(widthFraction = 0.70, aspect = 5.0),
)

/**
 * No supply lands taller than this fraction of the page, whatever its family's aspect works out to.
 *
 * The same 0.6 `defaultImagePlacement` bounds a photo by, and for the same reason: a first placement that
 * already fills the page leaves the maker nothing to compose *with*. It binds only where a family's aspect
 * would otherwise overflow a short page; on the portrait pages the product actually makes, nothing hits it.
 */
private const val BenchSupplyMaxHeightFraction: Double = 0.6

/**
 * The family [supplyId] belongs to, or `null` if the copy layer does not know it.
 *
 * ⚠ Read from [Copy.Supplies.BY_FAMILY], **never** from `supplyId.substringBefore('.')`. Five prefixes
 * (`tape`, `fix`, `mark`, `paper`, `shape`) carry four families, because *Tape & fixings* is one tape plus
 * three fixings — so the prefix is right for eleven of the sixteen and silently wrong for three. Deriving
 * a family from an id is how TalkBack shipped saying *"Rect shape"*.
 */
internal fun benchSupplyFamily(supplyId: String): String? =
    Copy.Supplies.BY_FAMILY.entries.firstOrNull { (_, supplies) -> supplyId in supplies }?.key

/**
 * A [Transform] for a newly placed supply: centred on the page, at 0°, sized by its family (§5, §5.1, §5.2).
 *
 * @throws IllegalArgumentException if [supplyId] is not one of the sixteen. That is a programming error,
 *   not a runtime condition — the only caller is the Art sheet, which iterates the same map this reads —
 *   and a silent fallback size would be the app inventing craft knowledge it does not have.
 */
internal fun benchSupplyPlacement(supplyId: String, pageSizePt: PtSize): Transform {
    val family = requireNotNull(benchSupplyFamily(supplyId)) {
        "$supplyId is in no family in Copy.Supplies.BY_FAMILY — a supply has no default size without one"
    }
    val default = BenchSupplyOverrides[supplyId] ?: BenchSupplyDefaults.getValue(family)
    var width = pageSizePt.width * default.widthFraction
    var height = width / default.aspect
    val maxHeight = pageSizePt.height * BenchSupplyMaxHeightFraction
    if (height > maxHeight) {
        height = maxHeight
        width = height * default.aspect
    }
    return Transform(
        xPt = (pageSizePt.width - width) / 2.0,
        yPt = (pageSizePt.height - height) / 2.0,
        widthPt = width,
        heightPt = height,
        // §5.1, stated rather than defaulted: a supply lands flat. Do not reintroduce a tilt here.
        rotationDegrees = 0.0,
    )
}
