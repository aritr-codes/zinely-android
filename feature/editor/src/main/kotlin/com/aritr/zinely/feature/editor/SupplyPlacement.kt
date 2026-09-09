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
 * The six **fixings** — the half of *Tape & fixings* that is not tape.
 *
 * ⚠ **Enumerated, never prefix-matched.** `supplyId.startsWith("fix.")` would have been shorter and is the
 * same mistake this file's [benchSupplyFamily] already warns about from the other direction: a prefix is
 * not a fact about a supply, it is a naming habit, and the day a `tape.*` id names something compact — or a
 * `fix.*` id names something long, which washi tape on a fixing id would be — the habit decides the size
 * and nobody is asked. Adding a member here is a sizing decision someone makes, which is the point.
 */
internal val BenchFixings: Set<String> = setOf(
    "fix.corner",
    "fix.staple",
    "fix.clip",
    "fix.stitch",
    "fix.grommet",
    "fix.pushpin",
)

/**
 * The key [BenchSupplyDefaults] files the fixings' constant under.
 *
 * Not a [Copy.Supplies] family name because **it is not a family** — the four families are the freeze's and
 * the copy layer's, and inventing a fifth would put a heading on the Art sheet that the maker never asked
 * for. What splits here is *sizing*, one level below the family, and this key says so by not pretending
 * otherwise. See [D-092](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-092).
 */
internal const val BenchFixingsSizingKey: String = "__sizing:fixings"
// ⚠ The value was `"fixings (sizing only — not a family)"` — self-documenting, and `CopyNoProseLiteralTest`
// rejected it as a user-facing prose literal outside `Copy` (ADR-060 / CI-81). The gate was right for the
// wrong reason: it cannot tell a map key from a sentence, and the fix is to stop writing a key that reads
// like one. `__sizing:` cannot collide with a family name, which is the property that actually matters.

/**
 * §5.2's one constant per **sizing group**. **Every number here is owed a ruling** (see this file's header).
 *
 * ⚠ This said *"one constant per family"* and the map now holds five entries keyed on four families plus
 * one non-family. [D-092](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-092-ruling) added the
 * fifth and this sentence was not updated with it — a framing line left describing the shape the data had
 * before the change is how the *next* reader concludes the fixings entry is a mistake.
 *
 *  - **Tape — long.** §5.2's own word for it. Over half the page's width at a 4.5:1 aspect, which is a
 *    strip you can see is a strip.
 *  - **Fixings — compact.** ⚠ **Added 2026-08-20, [D-092](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-092).**
 *    This entry did not exist, and the family's other three inherited tape's 4.5:1: a photo corner authored
 *    1:1 landed as a **long flat sliver with its pocket reduced to a slit**, over half the page wide. Not
 *    recognisable as a photo corner, and a maker who taps a tile showing a neat square corner gets something
 *    that looks like a mistake. Found on a device the day the fixings were first authored — before that
 *    `tape.torn` was the only member anyone could place, so the family default was never *wrong*, it was
 *    never *exercised*.
 *  - **Stamps & marks — small.** §5.2's word again. A stamp is punctuation on a page, not an element of
 *    it: 16 % of the page's width, square, which on a typical eighth-sheet page is roughly a thumbprint.
 *  - **Cut paper — a piece of paper.** The largest of the four, because a torn strip, a window frame and
 *    a speech tag are all things you lay *under* or *around* other content rather than beside it.
 *  - **Cut shapes — a drawn shape.** Mid-sized and square: big enough to be a deliberate block of ink,
 *    small enough that the first thing a maker does is not shrink it. See the header's ⚠ on `shape.rule`.
 */
internal val BenchSupplyDefaults: Map<String, BenchSupplyDefault> = mapOf(
    Copy.Supplies.TAPE_AND_FIXINGS to BenchSupplyDefault(widthFraction = 0.55, aspect = 4.5),
    BenchFixingsSizingKey to BenchSupplyDefault(widthFraction = 0.20, aspect = 1.0),
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
 * (`tape`, `fix`, `mark`, `paper`, `shape`) carry four families, because *Tape & fixings* mixes two tapes
 * with six fixings. Deriving
 * a family from an id is how TalkBack shipped saying *"Rect shape"*.
 */
internal fun benchSupplyFamily(supplyId: String): String? =
    Copy.Supplies.BY_FAMILY.entries.firstOrNull { (_, supplies) -> supplyId in supplies }?.key

/**
 * A [Transform] for a newly placed supply: centred on the page, at 0°, sized by its family (§5, §5.1, §5.2).
 *
 * @throws IllegalArgumentException if [supplyId] is not one of the thirty-two. That is a programming error,
 *   not a runtime condition — the only caller is the Art sheet, which iterates the same map this reads —
 *   and a silent fallback size would be the app inventing craft knowledge it does not have.
 */
internal fun benchSupplyPlacement(supplyId: String, pageSizePt: PtSize): Transform {
    val (width, height) = benchSupplySizePt(supplyId, pageSizePt)
    return Transform(
        xPt = (pageSizePt.width - width) / 2.0,
        yPt = (pageSizePt.height - height) / 2.0,
        widthPt = width,
        heightPt = height,
        // §5.1, stated rather than defaulted: a supply lands flat. Do not reintroduce a tilt here.
        rotationDegrees = 0.0,
    )
}

/**
 * A [Transform] for a supply **replacing** one already on the page: the incoming supply's own §5.2 family
 * size, kept where the outgoing one was.
 *
 * ### The ruling this implements, and the two it rejected
 *
 * The frozen spec does not say what happens to a supply's size when it is swapped, and the three readings
 * are materially different. **Owner ruling, 2026-08-17: re-apply the incoming family's default scale.**
 *
 * Rejected — *preserve the transform exactly*, which is what the photo `Replace` precedent does
 * (`EditImageCommand`: "Replace differs in `assetId`; geometry is preserved by the reducer"). It does not
 * carry over, and the reason is that a photo has no intrinsic proportion the app knows about while a supply
 * does: swapping a 4.5:1 torn tape for a 1:1 star inside the tape's box yields a star stretched four and a
 * half times, and the maker did not ask for a stretched star. Also rejected — *keep the area, adopt the
 * aspect*, which is kinder but has no precedent here and hides §5.2 behind arithmetic nobody can predict.
 *
 * **Two things are deliberately preserved, and neither is the size.**
 *  - **The centre**, so a replaced supply does not teleport to page centre. §5's "lands at page centre"
 *    governs a *landing*; a replacement is not a landing, and moving a mark the maker had positioned would
 *    read as the app losing their work.
 *  - **The rotation**, for the same reason. §5.1 fixes the angle a supply *arrives* at, not the angle it
 *    must stay at — the maker may have turned it, and that is their composition, not the placement's.
 *
 * [current] is the outgoing supply's transform; only its centre and rotation are read.
 */
internal fun benchSupplyReplacement(
    supplyId: String,
    pageSizePt: PtSize,
    current: Transform,
): Transform {
    val (width, height) = benchSupplySizePt(supplyId, pageSizePt)
    // Centre-anchored rather than origin-anchored: growing a stamp into a tape from a shared top-left
    // would walk the mark down and right across the page on every swap.
    return Transform(
        xPt = current.xPt + (current.widthPt - width) / 2.0,
        yPt = current.yPt + (current.heightPt - height) / 2.0,
        widthPt = width,
        heightPt = height,
        rotationDegrees = current.rotationDegrees,
    )
}

/**
 * §5.2's size for one supply on one page, shared by [benchSupplyPlacement] and [benchSupplyReplacement] so
 * the two cannot disagree about what a family's default is — the whole point of the replace ruling is that
 * a replaced supply gets *the same* size a freshly-placed one would.
 *
 * @throws IllegalArgumentException if [supplyId] is not one of the thirty-two. That is a programming error,
 *   not a runtime condition — the callers iterate the same map this reads — and a silent fallback size
 *   would be the app inventing craft knowledge it does not have.
 */
private fun benchSupplySizePt(supplyId: String, pageSizePt: PtSize): Pair<Double, Double> {
    val family = requireNotNull(benchSupplyFamily(supplyId)) {
        "$supplyId is in no family in Copy.Supplies.BY_FAMILY — a supply has no default size without one"
    }
    // The sizing key is the family, EXCEPT for the compact fixings — one aspect cannot serve both halves of a
    // family whose name contains the word "and", and *Tape & fixings* is the only one of the four that is
    // not physically homogeneous ([D-092]). Read before the per-supply override so `shape.rule` still wins.
    val sizingKey = if (supplyId in BenchFixings) BenchFixingsSizingKey else family
    val default = BenchSupplyOverrides[supplyId] ?: BenchSupplyDefaults.getValue(sizingKey)
    var width = pageSizePt.width * default.widthFraction
    var height = width / default.aspect
    val maxHeight = pageSizePt.height * BenchSupplyMaxHeightFraction
    if (height > maxHeight) {
        height = maxHeight
        width = height * default.aspect
    }
    return width to height
}
