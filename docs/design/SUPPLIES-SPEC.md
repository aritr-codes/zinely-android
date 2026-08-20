# SUPPLIES-SPEC — the third primitive

**Status:** **Decided** 2026-08-15 by the owner (*"proceed with the decisions that will be best for Zinely"*) · recorded as [ADR-105](../DECISIONS.md#adr-105)
**Authority:** implements [ADR-104](../DECISIONS.md#adr-104) (asset layer) under [Amendment 3](V2-CONSTITUTION.md#amendment-log).
**Supersedes:** [`ZINE-DIRECTION.md`](ZINE-DIRECTION.md) **§9.1–§9.5**, which reduce to a pointer here. §9.6
(ships-now / waits / never) stays where it is — it is direction, not specification.
**Scope:** the design record. ⚠ **It is no longer documentation-only** — S2, S3, S4, S6 and S7 ship, and
12 of the 16 outlines are authored in `SupplyCatalog`. Where this document and the code disagree, [§10.1](#101-status)
records which is which; the code is authoritative for what exists, this document for what was decided.

> **Documentation Rule check, corrected.** The first draft claimed §9 was "five paragraphs of strategy"
> that this document would not duplicate. That was false: §9 is a six-subsection sub-spec that already
> owned the field list, the sixteen, add/edit/behave, tilt, a11y and blast radius. Leaving both alive
> created five duplicate sources of truth — exactly what the rule forbids. The fix is supersession, stated
> above and mirrored in §9 itself, not a second opinion filed alongside the first.

---

## 0. The four escalated calls — decided

These four reversed or corrected owner-level text, which [`V2-CONSTITUTION.md` §VI](V2-CONSTITUTION.md)
reserves to the owner. They were escalated once, with a recommendation each, and the owner ruled: *"proceed
with the decisions that will be best for Zinely."* All four are now closed below, with the reasoning that
decided them. Nothing here is left open.

| | Call | Ruling |
|---|---|---|
| **O-A** | Which palette tints decor | **The content palette — all three bands.** §II.9 is satisfied by coverage, and the 5-ink text set exists for a reason that does not apply to decor |
| **O-B** | Does §IV's skeuomorphism ban reach the supplies | **No — §IV governs chrome; §V already permits content packs.** Cited, not assumed |
| **O-C** | Restore §9.2's sixteen, or adopt the redraft | **Restore verbatim.** The redraft is withdrawn; the one filing oddity is noted, not fixed |
| **O-D** | Does the photocopier filter precede this | **Yes.** X3b before X1 |

### O-A · decor tints from the content palette — all three bands

The error first: `v21-bench.html:74` — text I wrote under the ADR-104 amendment — says decor is *"tint[ed]
from the five named inks"*, and `ZINE-DIRECTION.md:393` repeated it. **Five is the count of V2.1 _chrome_
roles** (`leaf`/`berry`/`jam`/`butter`/`inkFaint`), and [ADR-090](../DECISIONS.md#adr-090) separates chrome
from the artifact precisely so chrome values never land on the sheet. Tinting decor from chrome roles would
break the colour-role law twice: `butter` is *"material only, never an action, never text"*, `berry` is
punctuation, not a fill.

**What "five" almost certainly was.** `TypeBar.kt:104-109` ships `TextInk` — exactly five. And its KDoc says
why: they are **contrast-corrected**, using *"yellow-as-text (`#7A5E12`, AA on both papers, bench RF4)
rather than the `yellow` token itself"*, with an explicit warning at `:102` that this set is *"Distinct
from the image spot-ink field set — the two must not be conflated (ADR-055 Decision 6)."* The five-ink
restriction exists **because text must stay legible**.

**That reason does not reach decor.** A supply is coverage, not a glyph — nothing has to be read against
it. So the constraint that justifies five has no purchase here, and applying it anyway would be conflating
exactly the two sets ADR-055 says not to conflate.

> **Ruling: decor tints from the content palette — `makerInks` (10) · `paperTints` (5) · `neutrals` (4).**
> It is what the frozen `applyInk` already does (`ZinelyContentInks.kt:34-45`, quoting the owner's D-003
> ruling that *"The complete maker palette consists of: Inks · Paper Tints · Neutrals"*), it makes
> `CHANGE_INK` nearly free because `BenchInkPopover` already ships with all three bands named
> (`:176`, `:190`, `:199`), and it adds no fourth colour vocabulary to a product that already carries three.
>
> The counter-argument — that a paper tint laid as a mark reads as nothing — is true and is the maker's
> call. Pale-on-pale is a legitimate riso result, not a mistake to prevent, and it is undone in one tap.
> §II.9 material consequence is satisfied by the **single-coverage** rule (§2), not by narrowing the swatches.
>
> **Binding language:** never write *"nineteen inks."* `ZinelyContentInks.kt:34` heads that section *"three
> categories — not nineteen inks"*; `:49`: *"A neutral is not an ink and a paper tint is not an ink."* This
> document says **swatches** throughout.

⚠ **Consequence — a real inconsistency this exposes, not one it creates.** The product now has three colour
vocabularies: `TextInk` (5, contrast-corrected), the bench popover (19 swatches), and cover inks. The first
two even disagree on values — `Coral #A63C22` vs `Brick #B0503F`, and `Teal #2A9D8F` has no maker-set
equivalent. Each divergence is individually justified (ADR-055 Decision 6; D-003), but nothing states the
rule that governs *which set a new surface joins*. **Filed to the NOW phase terminology pass**, not fixed
here — it predates this spec and touching it is a separate change.

`v21-bench.html:74` is corrected as a **bug fix to an amendment note** — a factually wrong colour-source
reference — which the freeze permits.

> ⚠ **A duplicate section stood here and was removed on 2026-08-18.** It repeated [O-A](#o-a--decor-tints-from-the-content-palette--all-three-bands)'s
> subject — the decor palette — under an `O-B` heading, and it was the *earlier* draft: it ended in a 🟦 **Recommend**
> where O-A above ends in a **Ruling**. Two sources of truth for one closed call, the second of them stale, inside the
> section whose own opening promises *"Nothing here is left open."* The Documentation Rule names this exactly. The real
> **O-B** — the skeuomorphism question §0's table lists — is immediately below and was never in doubt.

### O-B · §IV names two of these supplies as banned skeuomorphs

[`V2-CONSTITUTION.md:282`](V2-CONSTITUTION.md) bans *"literal coffee stains, **deckle edges**, **torn
paper**, handwriting fonts, tilted 'polaroid' frames. Warmth is **structural**…, never a costume."* This
spec ships `torn tape strip` and hand-torn cut paper.

> **Ruling: §IV governs chrome, not content — and §V already said so.** §V's growth table permits *"Asset
> packs / stickers / motifs… tintable **coverage not colour** (1 asset × N inks)"*, which is exactly
> §2/§3.3's single-colour fill. The owner therefore already sanctioned content supplies constitutionally;
> this reading applies that clause rather than amending anything.
>
> **The distinction that makes §IV coherent:** a torn edge painted onto the *app's furniture* is costume —
> the software pretending to be a physical object it is not. A torn edge the maker *places on their page*
> is the material itself, and refusing it would be the app pretending paper doesn't tear. §IV's own
> sentence carries this: *"Warmth is **structural**… never a costume."* A supply is structural — it is the
> thing being made, not a decoration applied to the thing making it.
>
> **Cited, not assumed** (§4.2). Shipping two of §IV's five named skeuomorphs in silence would have been
> the implicit-amendment failure mode Amendments 1 and 3 were both written about.

### O-C · the sixteen: §9.2's published set, restored verbatim

The first draft silently replaced §9.2's vocabulary — **11 of 16 differed**. Two losses were substantive:
the **halftone dot cluster**, which carries ADR-104's own thesis (*"the zine vocabulary is a **process**
vocabulary… this is why the photocopier filter outranks the supply set"*), and the **straight rule /
marker underline**, the dividers §9.2 argues a composition tool cannot do without.

> **Ruling: restore §9.2's sixteen verbatim** (§4). It is published, argued and costed, and reopening a
> settled vocabulary is the churn the owner asked to avoid.
>
> **On the two flagged oddities — one is not a defect, one is cosmetic.** *Torn tape strip* vs *torn strip*
> only look like duplicates: one is **tape**, one is **paper**, which is the exact distinction the two
> families exist to hold. Keep both. *Marker underline* genuinely is a pen mark filed under Cut paper — but
> the family label is a heading in a picker, the supply is useful and distinctly hand-made, and the
> alternatives all cost something real: moving it displaces a **process** mark (the halftone cluster or
> registration cross, the two that carry ADR-104's thesis), and dropping it loses hand-drawn emphasis that
> `shape.rule`'s clean line does not replace. **Ship it as published**; the mis-filing is noted for the S5
> authoring pass, where it costs nothing to reconsider with the drawings in hand.

### O-D · the photocopier filter outranks this whole document

ADR-104's Model D concludes *"**This is why the photocopier filter outranks the supply set**"*, and
`ZINE-DIRECTION.md` X3b calls it *"the single highest identity-per-line item in the product."*

> **Ruling: the filter ships before the sixteen.** This document is the *lower*-ranked half of ADR-104's
> model and says so; §10's sequence sits behind X3b, recorded in `ZINE-DIRECTION.md:715` (X1's depends-on
> column). It changes no content here.
>
> The reason is worth keeping visible: **shoot → dither → print** is the loop that makes Zinely a zine tool
> rather than a photo-layout tool, and it needs no vocabulary at all. Sixteen supplies improve a zine; the
> filter is what makes the output *look like a zine*. Shipping the supplies first would be decorating a
> product before it has its voice.

---

## 1. What Supplies is

**Art is the verb. Supplies is the drawer.** You tap *Art* to add something; what you are choosing from is
your Supplies. That is the naming resolution, and it is already drawn in `v21-bench.html`.

The target feeling is **the material cabinet of the Zine studio**, and explicitly *not* a stock asset
browser with the internet unplugged. The difference is observable, not rhetorical:

| A material cabinet | A disconnected stock browser |
|---|---|
| You can see everything at once | You search because you can't |
| Items are few and reusable | Items are many and single-use |
| Variety comes from *your* hand | Variety comes from *its* catalogue |
| Running out is a prompt to combine | Running out is a dead end |

Where a design choice below could go either way, the cabinet wins.

⚠ **One open amendment this does not close.** `ZINE-DIRECTION.md` A7 keeps a *"**Supplies tray** — a
persistent drawer, distinct from the Art chooser"* as a live proposal, sequenced at X2 after X1, and
`SCREEN-INVENTORY.md:110-118` carries a 🔭 Supply-tray entry. This document specifies the **Art chooser**
only. A7 stays open; it is not silently collapsed into this.

---

## 2. The primitive: `DecorElement`

`Element`'s own KDoc anticipated this — *"Sealed so new kinds (shapes, V2) are additive"*
([`Document.kt:63`](../../core/model/src/main/kotlin/com/aritr/zinely/core/model/Document.kt)).
`DecorElement` is the **third and final** member of that sealed interface (§7).

```kotlin
/**
 * A placed supply — one authored mark from the Zinely cabinet, laid down in one colour.
 *
 * Carries an *identifier*, never geometry: the outline lives in `SupplyCatalog` as code, so a document
 * stays small, a supply cannot be hand-edited into something unauthored, and redrawing a supply improves
 * every zine that used it. Same intent-not-pixels seam that lets [ImageElement] carry an `assetId`
 * instead of bytes (ADR-027).
 */
@Serializable
@SerialName("decor")
public data class DecorElement(
    override val id: String,
    override val transform: Transform,
    override val zIndex: Int,
    /** Stable catalogue key, e.g. `tape.torn`. Never a content hash — supplies are authored, not imported. */
    val supplyId: String,
    /** One swatch from the content palette (§0 O-A). Single-coverage, so tinting is exact. */
    val ink: ColorRgba,
    /** Reflects the outline across its own vertical centre line. Nine of sixteen are asymmetric. */
    val mirrored: Boolean = false,
) : Element
```

**Two new fields and one flag.** Position, size, rotation and stacking are already carried by `Transform`
and `zIndex`. `mirrored` genuinely cannot ride on `Transform` — it has exactly five fields
(`Document.kt:53-59`) with no scale term, and `DefaultDocumentValidator.kt:106` *errors* on non-positive
width/height, so negative-scale is doubly unavailable.

**Deliberately absent:**

| Rejected | Reason |
|---|---|
| `opacity` | Riso and photocopy lay ink down or they don't. The one-layer blend lesson (D-020) already cost us once. Also on [ZINE-DIRECTION §9.6](ZINE-DIRECTION.md)'s **Never** list. |
| `strokeWidth` / `strokeColor` | A mark is ink laid down, not a stroked outline. A supply that reads outlined is *authored* as a closed ring (§3.3). |
| `path` / geometry | Puts an authoring tool in the document. Breaks "curated, not infinite" at the type level. |
| `secondaryInk` | A pack feature, not a beta feature. Additive later. |

### 2.1 Serialization — no migrator

`Element` is `@Serializable` and sealed, and `JsonDocumentSerializer.kt:39` sets
`classDiscriminator = "type"`. kotlinx registers sealed subclasses automatically, so `DecorElement` needs
`@Serializable` + `@SerialName("decor")` and **no manual polymorphic wiring, no schema version bump, and
no migrator.**

⚠ This **contradicts `ZINE-DIRECTION.md:715` (X1)**, which costs the work as *"Schema v1→v2 + migrator"*,
and §9.1's 🟨 INFERRED note saying the same.

🔴 **CORRECTED 2026-08-16 BY IMPLEMENTATION — and the sentence above was wrong twice, in opposite
directions. `ZINE-DIRECTION.md:715` was right and this section was not.** What actually shipped is
**schema v1→v2 *with* a migrator**, and the two errors are worth separating because they fail differently:

1. **The bump is required, and this section's "no bump" was the dangerous half.** Ruled in
   [D-029 Q5](V2-SPEC-DEFECTS.md#d-029-ruling-2026-08-16): `copier` (ADR-106) was a *defaulted field*, so an
   old build silently drops it, but `DecorElement` is a **new sealed discriminator**, so `0.9.0-beta.1`
   **fails to parse the whole document**. A beta tester who opens a zine containing one supply gets a broken
   zine, not a plainer one. ADR-021's `NewerSchemaVersionException` is what turns that into an honest *"this
   zine needs a newer Zinely"* — and it is now load-bearing for a **released** beta.
2. **A migrator is required by mechanism even though nothing needs migrating.** `DocumentMigrations` demands
   a **contiguous chain** and throws `MissingMigratorException` on a gap, so bumping to 2 without a 1→2 entry
   breaks *every existing zine* — the exact opposite of what the bump is for. An **identity** migrator ships.
   The "additive and total" argument was right about the *content* and wrong about the *plumbing*: nothing
   needs transforming, and something still has to be registered.

The general lesson, which is why this correction is this long: *"no migration is needed"* is a statement
about data, and *"no migrator is needed"* is a statement about a registry that enforces its own continuity.
They are not the same sentence, and only one of them was true.

⚠ **Forward-compatibility is one-way.** A document containing `decor` fails to parse on a build predating
this change; `ignoreUnknownKeys = true` does not rescue an unknown sealed discriminator. Already true of
the format, not made worse — but `DecorElement` must land *before* any curated pack, never alongside one.

### 2.2 Validation — and where it can actually live

`DefaultDocumentValidator.validateElement`'s `when (element)` has **no `else`**
(`DefaultDocumentValidator.kt:79-96`), so adding `DecorElement` is a **compile error until handled**.

But the first draft placed two rules there that cannot be built there:

- `core/data/build.gradle.kts:22` declares `api(project(":core:model"))` as its **only** project
  dependency, under a header stating S2A is *"deliberately Android-free."*
- `SupplyCatalog` sits in `:core:render`; `:core:data` does not depend on it, and the reverse edge would
  invert the layering.
- `ZinelyContentInks` lives in `:core:ui` — an `android.library` module gated behind `ZINELY_CORE_ONLY`
  in `settings.gradle.kts:49-52` — and its values are `androidx.compose.ui.graphics.Color`. Reaching it
  from `:core:data` breaks the Android-free core **and** the pure-JVM CI job.

> **Ruling.** Validation in `:core:data` checks only what `:core:model` can see: `decor.supplyId.blank`
> (error) and `decor.supplyId.malformed` (error — must match `^[a-z]+\.[a-z]+$`). **Catalogue membership
> and palette membership are checked at the render/UI boundary, not in the document validator.** An unknown
> `supplyId` renders as nothing and is reported by the editor, which is the layer that knows the catalogue.
> No ink validation at all: an off-palette colour renders correctly, so refusing to open the zine would be
> the worse harm.

This keeps the `ink` field's KDoc free of any `:core:ui` link — a `:core:model` type must carry zero
Android dependencies (CLAUDE.md), and the first draft's `[ZinelyContentInks.makerInks]` reference violated
that in a doc comment.

---

## 3. The render contract: `DrawShape`

The load-bearing engineering finding of this pass, and it corrects a published claim.

### 3.1 The constraint — ✅ **CLOSED 2026-08-16 by package P2**

> **This section is kept as written and marked, not rewritten.** It states the constraint that motivated
> the whole design, and P2 removed it: `core:render` now emits **four** commands, and the fourth is
> `DrawShape`. `SupplyOutline` and `SupplyCatalog`'s first four supplies ship with it. **P3 then armed the
> replayer** (2026-08-16): `SceneRenderer` emits `DrawShape` for a `DecorElement` with an authored
> outline, and `CanvasReplayer` paints it with its own anti-aliased `Paint` and `Path.FillType.EVEN_ODD`.
> **The ink reaches paper on the raster surfaces.** What remains open is the *print* surface — see §3.5:
> the PDF half of the hole test cannot run under Robolectric and has never executed.

`core:render` emits exactly three commands — `FillRect`, `DrawImage`, `DrawTextBox`
(`DrawCommand.kt:26,40,55`). **There is no path-drawing command.**

`ZINE-DIRECTION.md:441` states *"✅ **PDF export is already vector-capable** (`PdfPageRenderer.kt:13-18`),
so authored supplies print as vectors at any size — no raster stickers,"* repeated at `:512`. That is true
of the **backend** and false of the **tape**: the PDF canvas can draw a vector path, but nothing in the
pipeline can currently ask it to. Both sites are corrected.

### 3.2 The cost — one branch, four surfaces

A pre-verification claim held that this costs implementations in both a PDF and a raster renderer. **That
is wrong, and the truth is cheaper.** There is exactly one replayer:

> *"the single draw path shared by every backend (ADR-028 'one replayer, two canvas providers'). Preview,
> raster export, and PDF export differ only in which Canvas and pageToDevice/decodePxPerPt they pass in"*
> — `CanvasReplayer.kt:18-20`

Every `DrawCommand` consumer routes through it: `PdfPageRenderer.kt:47`, `RasterPageRenderer.kt:42`,
`PagePreview.kt:94`, and `SheetComposer.kt:70,106,133` (imposed multi-up sheets). So one `is DrawShape ->`
arm serves **preview + PNG + PDF + imposed sheet**. `SheetGuides.kt` *produces* `FillRect`s rather than
consuming the tape, and `ImageBlitter` is a replayer collaborator, not a second replayer.

**The vector invariant, correctly sourced.** The first draft argued from `StaticLayout.draw` keeping text
vector — a different code path (glyph runs, which Skia may itself convert to paths), so analogy rather than
precedent. The real invariant is about the **paint**: Skia's PDF device rasterises as a fallback for path
effects, mask filters, colour filters and exotic blend modes, and `PdfDocument.Page` documents its own
unsupported operations. **A solid-colour SrcOver fill stays vector**, and even-odd maps to the `f*`
operator. AA is simply ignored by the PDF backend — which is why §3.5's decision costs nothing in print.

**✅ The spike ran (2026-08-16) and closed the question — including the shape of the spike itself.**

Everything above is confirmed against the code. Export draws through `android.graphics.pdf.PdfDocument`'s
`Canvas` (`PdfPageRenderer.kt:38,45,48,57`; `SheetComposer.kt:59-83`) — there is **no content-stream writer
and no hand-emitted operator anywhere in the repo**, so Skia's PDF device produces the vectors for us and
even-odd does reach the file as `f*`. The cost of a path is one `DrawCommand` subtype, one `is DrawShape ->`
arm and one extra `Paint`. There was never an operator problem to design around.

**But the proposed spike — "diff PDF operators rather than pixels" — was the wrong instrument**, and that is
the more useful result. An operator diff needs a PDF parser we do not have, and it cannot see the one error
this design can actually make. The right check is a **hole test**: render a ring whose inner and outer
subpaths are wound the **same direction**, and assert the centre pixel is paper-white while just-inside-the-
wall is not, on both the PNG and the PDF rasterised back (the harness already exists —
`PdfSurfaceParityInstrumentedTest.kt:124-125,196-197` with `PdfRasterizer`). Same-direction winding is
load-bearing: opposite winding passes under *both* fill rules and proves nothing. That one check fails on a
dropped fill rule, a missing scale fold, a wrong composition order, and a silent raster fallback — with no
PDF parsing at all.

**Five constraints the spike attached to §3.3**, each of which would otherwise ship silently:

1. **`localClip` must always be `null`.** `DrawCommand.kt:15` says *"all coordinates are page-local points"*,
   but a unit outline's local space is `0..1`; a points-valued clip would crop it to a 1×1pt sliver.
2. **The unit-square fold needs a scale term.** `SceneRenderer.localToPage` is translate·rotate with **no
   scale**, so a unit outline renders 1pt × 1pt. The scale is **non-uniform** — `CanvasReplayer`'s
   `uniformScale()` is the wrong helper.
3. **`DrawShape` needs its own `Paint`.** The shared `fillPaint` pins `isAntiAlias=false`; mutating it moves
   every existing `FillRect` golden. The PDF backend ignores AA entirely, so `isAntiAlias=true` costs nothing
   in print — which is exactly what §3.5 assumed.
4. **Even-odd lives on `Path.fillType`, not on the `Paint`.** Forget it and every hole fills *identically on
   all four surfaces* — meaning surface-parity testing is structurally blind to it. This is the one defect
   our parity strategy cannot catch, so it gets its own assertion.
5. **No `MaskFilter`, no `PathEffect`, no perspective row.** Those are the triggers that make SkPDF quietly
   rasterise instead of emitting vectors. A torn edge is torn in the authored geometry, never filtered.

⚠ Two claims remain strong rather than settled: nothing ran on a device, so *"`f*` reaches the file"* and
*"AA is ignored"* are verified against upstream Skia source but are assumptions for our API level until the
hole test runs. And no independent Review Agent read the spike.

### 3.3 The command

```kotlin
/**
 * A filled, single-colour outline in element-local points — one placed supply.
 *
 * Fill only, **even-odd**: holes work without authoring winding direction correctly, which matters
 * because outlines are drawn by hand rather than generated. A supply that reads as an outline is a
 * closed ring, not a stroke — so there is no cap/join/miter surface to get wrong or to diff against.
 *
 * [localClip] is always `null` here, and that is an invariant rather than a default: this command's local
 * space is the authored **unit square** (0..1), not the page-local points every other `DrawCommand` uses,
 * so a clip expressed in points would crop the supply to a 1x1pt sliver (§3.2).
 */
public data class DrawShape(
    val outline: SupplyOutline,
    val ink: ColorRgba,
    override val localToPage: AffineTransform2D,
) : DrawCommand {
    /** Not a defaulted parameter — see above. A caller must not be able to pass one. */
    override val localClip: PtRect? get() = null
}

/** Pure-Kotlin geometry — no `android.graphics.Path` in `core:render`. Authored in a unit square (0..1). */
public data class SupplyOutline(val subpaths: List<Subpath>)
public data class Subpath(val start: PtPoint, val segments: List<Segment>)
public sealed interface Segment {
    public data class LineTo(val to: PtPoint) : Segment
    public data class CubicTo(val c1: PtPoint, val c2: PtPoint, val to: PtPoint) : Segment
}
```

Subpaths are always closed — a fill of an open path is its implicit closure anyway, so a `closed` flag
would be a lie you could set to `false`. Quadratics are omitted: every quadratic is a cubic.

**As shipped (P2), with three departures from the block above** — the block is amended to match the code,
because the code is now the thing that runs:

1. **`localClip` is `get() = null`, not a defaulted parameter.** The earlier draft called the null an
   invariant one paragraph after making it a value a caller could pass. A defaulted parameter is a
   *suggestion*; this is a **property with no setter**, so the `PtRect(0, 0, w, h)` that every other
   element command legitimately passes cannot be handed to this one and crop a supply to a sliver.
2. **`Segment.to` is hoisted onto the interface**, so a traversal can walk the outline without a `when`
   over the segment kinds — which the unit-square check and the shoelace area check both need.
3. **Unit-square containment is enforced in `Subpath.init`, control points included** (§4.1 rule 1). This
   is why it matters that it is enforced by *construction* rather than by a test: it binds the outlines
   nobody has drawn yet, which are the ones a house-style designer will hand over.

### 3.4 Purity is preserved — and the seam differs from `DrawImage` on purpose

`SceneRenderer.buildScene` is *"A pure function of `(Page, defaults)` alone — no asset resolver, no I/O, no
Android"* (`SceneRenderer.kt:23`), verified: no injected resolver, no I/O.

`DrawImage` carries an unresolved `assetId` because resolving it means reading bytes. **Supply outlines
need no I/O** — they are code. So `SupplyCatalog` lives in `:core:render` as a pure
`Map<String, SupplyOutline>` and `buildScene` resolves the outline *itself*, emitting a self-contained
`DrawShape`. Strictly better than mirroring `DrawImage`: the backend gains no new resolver dependency, and
the whole supply system is unit-testable in pure JVM with no device and no fixture files.

#### 3.4.1 ⚠ The unit-square fold — a real defect in the first draft

The first draft said outlines are authored in a unit square and that only `mirrored` folds into
`localToPage`. **That renders every supply 1pt × 1pt.** `SceneRenderer.localToPage` (`:90-97`) is
`translate(x,y) × [T(c)·R(deg)·T(-c)]` — translate and rotate, **with no scale term at all.**

The fold must therefore be, in order:

```
localToPage = translate(x, y) · [T(c) · R(deg) · T(-c)] · scale(w, h) · mirror?
```

with `mirror` an x-reflection about `0.5` applied in unit space before the scale.

**The scale is non-uniform** (`w ≠ h` in general), which the first draft never acknowledged and which is
the stamp-distortion case §3.3 hand-waved. Consequences, both real:

- A stamp or registration cross stretched non-uniformly is *wrong*, not stylish. **The editor constrains
  `mark.*` and `shape.circle` to uniform scale**; tape and cut paper stretch freely, because stretching
  tape is what tape does. This is an editor rule, not a render rule — the tape stays dumb.
- `AffineTransform2D` (`:41-63`) offers only `identity`, `translate`, `halfTurnAbout`, `rotateDeg`. **A
  `scale(sx, sy)` factory is a new addition to `:core:model`**, unlisted in the first draft's cost table.

### 3.5 Anti-aliasing — a real consequence, stated plainly

`CanvasReplayer.fillPaint` pins `isAntiAlias = false`, deliberately, *"so geometric fills diff at zero
tolerance"* (`:46-51`). A torn tape edge or a halftone cluster drawn without AA is visibly jagged.
`DrawShape` needs its **own** `Paint` with `isAntiAlias = true`.

Consequence: **shape goldens cannot assert at zero tolerance.** Decision — **assert the emitted `DrawShape`
tape** (the stronger test, and exactly the thing shared by all four surfaces), **plus one device golden per
family at a stated tolerance** (four goldens). Print is unaffected: AA is ignored by the PDF backend (§3.2).

**The load-bearing check is none of those four — it is the hole test** (§3.2), and it is named here so the
golden plan is not mistaken for the proof. Render a ring whose inner and outer subpaths are wound the
**same direction**; assert the centre is paper-white and just-inside-the-wall is not, on the PNG *and* on
the PDF rasterised back. One assertion catches a dropped fill rule, a missing scale fold, a wrong
composition order, and a silent raster fallback. Four tolerance goldens catch none of them reliably: a
dropped fill rule fills the holes **identically on every surface**, so cross-surface comparison is blind to
it by construction, and a tolerance wide enough to absorb AA is wide enough to absorb a small solid region.

P2 has already pinned the half that lives in pure Kotlin — `SupplyOutlineRingTest` proves the
representation *can* express a same-direction-wound ring, which is the precondition.

**✅ P3 ran the pixels — on one surface of two, and the distinction is the finding.**

- **Raster: proved, and proved to be capable of failing.** `holeTest_sameWoundRing_leavesItsCentreUnfilled`
  passes, and flipping `EVEN_ODD → WINDING` turns it and its discriminator twin **red**. A hole test that
  has never been seen to fail is a hole test nobody has checked.
- **Print: not proved, and it is now the second thing only hardware can close.** `PdfDocument` does not run
  under Robolectric, so `PdfSurfaceParityInstrumentedTest.supplyRing_holeSurvivesOnBothSurfaces` is
  compile-checked and **has never executed**. *"`f*` reaches the file"* and *"AA is ignored by the PDF
  backend"* remain sourced-but-unmeasured, exactly as §3.2 recorded them.
- ⚠ **The hole test catches less than its first comment claimed.** It was documented as failing on a missing
  scale fold and a wrong composition order too. **Mutation proved otherwise** — deleting the scale term and
  reversing the composition order each left it *green*, because it builds `localToPage` by hand and never
  routes through `SceneRenderer`. Those two are caught by `SceneRendererDecorTest` and
  `sceneRendererFold_putsAnAuthoredSupplyOnItsOwnBox`. No coverage was missing; the **attribution** was
  wrong, and a test whose comment overstates its reach is worse than one with no comment at all.
- **The four tolerance goldens (below) are consciously DEFERRED, not forgotten.** Only one of the four
  families is authored, so three of them would be goldens of nothing; and a record-mode run is precisely the
  act that can bless drift. They land with the outlines still owed.

---

## 4. The vocabulary — §9.2's sixteen, restored

The four families are frozen (`v21-bench.html:844`). The sixteen below are **`ZINE-DIRECTION.md §9.2`
verbatim**; the first draft's redraft is withdrawn (§0 O-C).

> **This table is the vocabulary, not the labels.** Two of the sixteen are written here as slash-pairs and
> a screen reader cannot say a slash, so the *spoken and drawn* name of each supply lives in one place —
> `Copy.Supplies.BY_FAMILY` — which departs from this prose in five places and documents why at each one
> ([D-083](V2-SPEC-DEFECTS.md#d-083-ruling)). ⚠ Two strings escaped that rule and are owed a move into
> `Copy.Supplies`: the Art sheet's own **"Art"** title and **"Recent · ⭐ favourites"** heading, which P-G
> had to define inside `feature:editor`. A copy object that holds fifteen of a surface's seventeen strings
> is not yet the single source of truth it claims to be. Where the two disagree, **the copy wins**; the
> Art sheet's `aria-label`s were reconciled to it on 2026-08-16 (amendment **A5**).

| Family | The four | The distinction it holds |
|---|---|---|
| **Tape & fixings** | torn tape strip · photo corner · staple · paper clip | Things that *attach* — they read as holding something down, so they invite layering over a photo. |
| **Stamps & marks** | star/asterisk · arrow · halftone dot cluster · registration cross | Things that *point*, plus the two that name the **process**. The zine's punctuation. |
| **Cut paper** | torn strip · cut-out window frame · cut label/speech tag · marker underline | Hand-**torn** edges and the things you cut *out* of a sheet. |
| **Cut shapes** | rectangle · circle · triangle · straight rule | Scissor-clean geometry, plus the divider a composition tool cannot do without. |

`supplyId`s: `tape.torn` · `fix.corner` · `fix.staple` · `fix.clip` · `mark.asterisk` · `mark.arrow` ·
`mark.halftone` · `mark.registration` · `paper.strip` · `paper.window` · `paper.tag` · `paper.underline` ·
`shape.rect` · `shape.circle` · `shape.triangle` · `shape.rule`.

**Two published oddities left exactly as published**, flagged rather than fixed by stealth: *torn tape
strip* and *torn strip* are near-neighbours; *marker underline* is a pen mark filed under Cut paper.

**Why "Cut paper" and "Cut shapes" are both needed** — the edge tells you which hand made it. Torn = the
material's hand. Cut = yours. A zine that is all torn reads sloppy; one that is all cut reads corporate.

**The halftone cluster and registration cross are the two that matter most.** They are *process* marks —
the residue of reproduction, which is ADR-104's thesis about what zine vocabulary actually is. Dropping
them, as the first draft did, cost the family its argument.

### 4.1 Authoring rule (binding)

Every outline is drawn in a unit square, fill-only, even-odd, with no self-intersections that depend on
winding. Outlines ship as Kotlin source in `SupplyCatalog`, reviewed like code — not as a data file, and
not imported from anywhere.

ADR-104's `source → verify licence → curate → package → ship` governs **packs**; these sixteen are authored
in-house and carry no third-party licence. But ADR-104 also says *"curation and provenance is a **process**,
not a feature"*, and S5 is exactly where an outline gets traced off an icon set by a well-meaning
contributor. **S5's definition of done is a one-line attestation per supply** — authored from scratch, no
reference art traced, repo licence — plus a colophon entry alongside the font licences (X11).

**The attestation binds the engineer-authored four as well** (P2, 2026-08-16), and that is worth stating
because the temptation runs the other way: `shape.rect` and `shape.circle` are *obviously* not traced off
anyone's icon set, so an attestation on them reads as ceremony. It is not — an attestation that appears
only where it feels necessary is an attestation nobody can rely on, because its absence stops meaning
anything. It lives in `SupplyCatalog`'s KDoc until X11 gives it a colophon to move into.

**Of the four rules in this section, only two can be machine-checked, and P2 checked both.** Rule 1 (unit
square) is now enforced *by construction* in `Subpath.init`, control points included; rule 2 (encloses an
area) is a shoelace test rather than a segment count. Rules 3 and 4 — fill-only, and no winding-dependent
self-intersection — are **unassertable by design**: winding-independence is a property of a shape's
relationship to a fill rule, not of its coordinates. That gap *is* the argument for §4.1's "reviewed like
code, not a data file": the two rules a machine cannot check are exactly the two a reviewer must.

### 4.2 Why torn edges are permitted here and banned in the chrome

Required citation, per §0 O-B. [`V2-CONSTITUTION.md:282`](V2-CONSTITUTION.md) bans *"literal coffee stains,
**deckle edges**, **torn paper**, handwriting fonts, tilted 'polaroid' frames. Warmth is **structural**…,
never a costume."* Two of the sixteen are named in that list, so the permission has to be shown rather than
assumed.

**§V already grants it.** The growth table permits *"Asset packs / stickers / motifs… tintable **coverage
not colour** (1 asset × N inks)"* — which is precisely §2's single-colour `DecorElement` and §3.3's
fill-only `DrawShape`. Content supplies were sanctioned constitutionally before this document existed.

**And §IV's own sentence draws the line.** *Warmth is structural, never a costume.* A torn edge painted
onto the app's furniture is costume — software pretending to be a physical object it isn't. A torn edge the
maker **places on their page** is structural: it is the thing being made. Refusing it would be the opposite
error, an app pretending paper doesn't tear.

The rule, stated once so it can be applied without re-deriving: **§IV governs the studio; the supplies are
what the studio is stocked with.** A chrome surface may never wear a material. A content primitive may
never be anything else.

---

### 4.3 The worked curation list — [ADR-107 R1](../DECISIONS.md#adr-107)'s first deliverable {#curation-list}

R1 sets a target of ~60 and **refuses to assert a per-family number**, because a count asserted up front
hides the risk that matters: [R2's art-fatigue mechanism operating *inside* a family](../DECISIONS.md#adr-107).
Fifteen near-duplicate fasteners are as legible a repetition as any shuffle. This section is the count
worked rather than asserted.

**The answer is ~51, not 60, and one family is why.**

| Family | Ships | Proposed | Total | Ceiling reached? |
|---|---|---|---|---|
| **Tape & fixings** — things that *attach* | 4 | 10 | **14** | No — the physical world has many fasteners |
| **Stamps & marks** — things that *point*, plus the *process* | 4 | 11 | **15** | No — process marks are a deep seam |
| **Cut paper** — hand-torn edges, things cut *out* | 4 | 9 | **13** | No |
| **Cut shapes** — scissor-clean geometry | 4 | 5 | **9** | ⚠ **Yes** — see §4.3.5 |
| | **16** | **35** | **51** | |

#### 4.3.1 The filter that did the work

Every candidate was asked R1's question — *does it attach, point, tear, or cut?* — and then a second one
that turned out to exclude more than the first:

> **Can the maker already make this with a verb they have?**

Rotation, scale and mirror are existing maker verbs ([§5](#5-how-sixteen-becomes-large--combinatorics-without-proceduralism)).
A supply the maker can already produce from another supply is not a new supply; it is a duplicate with a
name, and shipping it is the art-fatigue mechanism dressed as generosity. This is what makes *Cut shapes*
the family that runs out, and it runs out for a principled reason rather than a lack of imagination.

#### 4.3.2 Tape & fixings — 4 shipping, 10 proposed

| id | Name | Source | Why it is not a duplicate |
|---|---|---|---|
| `tape.masking` | Masking tape | derivable | Clean-cut ends against `tape.torn`'s ragged ones — the two read as different acts |
| `fix.pushpin` | Push pin | Phosphor `fill` | |
| `fix.bulldog` | Bulldog clip | Phosphor `fill` | Clamps a top edge; `fix.clip` slides over a corner |
| `fix.safety` | Safety pin | Openclipart CC0 | |
| `fix.stitch` | Saddle stitch | derivable | The binding a zine actually uses. Arguably the most on-theme mark in the list |
| `fix.grommet` | Eyelet | derivable | A ring with a hole — a second subpath, which the even-odd rule already supports |
| `fix.band` | Rubber band | derivable | |
| `fix.thread` | Thread tie | Openclipart CC0 | |
| `fix.seal` | Wax seal | Openclipart CC0 | |
| `fix.punch` | Punch hole | derivable | Binding, not fastening — but it answers *attach* |

**Excluded, with the reason recorded so nobody re-proposes them:** binder clip (near-duplicate of
`fix.bulldog` — one silhouette, pick one) · bent staple (near-duplicate of `fix.staple`) · album mounting
tab (near-duplicate of `fix.corner`) · straight pin (near-duplicate of `fix.pushpin`).
**Marginal, held back:** washi strip — its patterned edge is authoring work whose only distinction from
`tape.masking` is a repeat, and a repeat is the thing §5 is most suspicious of.

#### 4.3.3 Stamps & marks — 4 shipping, 11 proposed

The family holds two seams. *Pointing* is the obvious one; the *process* seam is the one that makes this a
zine tool rather than a sticker book, and ADR-104 ranks it highest.

| id | Name | Seam | Source |
|---|---|---|---|
| `mark.hand` | Pointing hand | point | Phosphor `fill` — the printer's manicule, historically exact for the medium |
| `mark.chevron` | Chevron | point | derivable |
| `mark.bang` | Exclamation | point | Phosphor `fill` |
| `mark.query` | Question mark | point | Phosphor `fill` |
| `mark.tick` | Check | point | Phosphor `fill` |
| `mark.burst` | Starburst | point | derivable |
| `mark.spiral` | Spiral | point | derivable |
| `mark.crop` | Crop marks | **process** | derivable |
| `mark.bar` | Colour bar | **process** | derivable |
| `mark.scan` | Copier streak | **process** | derivable |
| `mark.perf` | Perforation | **process** | derivable |

**Excluded:** moiré (near-duplicate of `mark.halftone` — a different artifact in theory, indistinguishable
at supply scale) · **halftone at further densities** — this is the near-duplicate trap in its purest form,
and density is a *variation* axis, not four supplies · bullet dot (`shape.circle` at small scale; the maker
has scale) · an `X` glyph, which collides with `mark.registration` at a glance.
**Marginal, held back:** toner smudge and ink bleed — both are organic blots whose distinction from each
other is a judgement nobody has made yet.

#### 4.3.4 Cut paper — 4 shipping, 9 proposed

| id | Name | Source |
|---|---|---|
| `paper.deckle` | Deckle edge | Openclipart CC0 |
| `paper.stub` | Ticket stub | derivable — the perforation is a dashed edge |
| `paper.banner` | Banner | derivable |
| `paper.label` | Luggage tag | derivable |
| `paper.envelope` | Envelope | Phosphor `fill` |
| `paper.dogear` | Folded corner | derivable |
| `paper.hole` | Torn hole | Openclipart CC0 |
| `paper.bubble` | Speech bubble | derivable |
| `paper.stamp` | Postage stamp | derivable |

**Excluded:** index card (near-duplicate of `paper.label` once both are rectangles with a treated edge) ·
a second torn strip at another tear width (`paper.strip` with scale) · thought bubble — genuinely distinct
in comics, but it is `paper.bubble` plus a trail of circles the maker can place, which makes it a
composition rather than a supply.

⚠ `paper.bubble` sits close to the shipping `paper.tag` (*Speech tag*). They are kept apart because one is
a **cut label** — a rectangle with a notch — and the other a **drawn balloon**; if curation finds they read
the same on a page, `paper.bubble` is the one to drop.

#### 4.3.5 Cut shapes — 4 shipping, 5 proposed, and the family is done

| id | Name | Source |
|---|---|---|
| `shape.hex` | Hexagon | derivable |
| `shape.arc` | Arc | derivable |
| `shape.quarter` | Quarter round | derivable |
| `shape.plus` | Cross | derivable |
| `shape.ring` | Ring | derivable — hole as a second subpath |

**This family cannot honestly reach fifteen, and the reason is a feature.** Its most obvious candidates are
all things the maker can already make:

| Not shipped | Because |
|---|---|
| Square | `shape.rect` scaled to 1:1 |
| Oval | `shape.circle` scaled non-uniformly |
| Diamond | a square **rotated** 45° |
| Rounded rectangle | `shape.rect` — the radius is not a supply |
| Pentagon · octagon | near-duplicates of `shape.hex` at supply scale |
| Star polygon | collides with `mark.asterisk`, which is already named *Star* |

Adding any of them would ship a supply the maker can already produce — a duplicate with a name. **A family
that can only reach its quota by splitting hairs should stop short**, which is exactly what R1 authorises.

#### 4.3.6 What this list is not

- **Not authored yet.** Every `derivable` row is a claim that the mark is geometry rather than house style,
  on the precedent that *Cut shapes* shipped early for exactly that reason. Each is owed the same
  attestation the sixteen carry (§4.1), and any row that turns out to need a designer's hand joins
  [S5](#10-cost-and-sequence)'s remaining four rather than blocking the rest.
- **Not licence-cleared yet.** Every `Phosphor` and `Openclipart` row is owed its ADR-104 record — source ·
  licence · attribution · modification · redistribution — before it enters the binary. Sourcing is a
  process, not a lookup.
- **Not a ruling on names.** The drawn and spoken name of every supply lives in `Copy.Supplies`, which
  already departs from this document in five places and documents why at each. Where the two disagree,
  **the copy wins**.

---

## 5. How sixteen becomes large — combinatorics without proceduralism

The hard constraint: *"the output should feel authored, not procedurally generic."*

**The possibility space, counted honestly.** Nine of sixteen supplies are asymmetric, so mirror yields
`9×2 + 7 = 25` supply variants. The palette is 19 swatches but **18 distinct colours** — `Ink #2A251E`
appears verbatim in both `makerInks` and `neutrals` (`ZinelyContentInks.kt:52-54`, flagged there as
deliberate). So **450 distinct single placements** before position, size, rotation or stacking; two
supplies layered is already ~200,000 ordered pairs.

The number is not the argument. **Scarcity is not the risk; sameness is** — and that claim survives at any
of these counts. Sameness is prevented by putting every multiplier in the maker's hand and none in a
generator:

- ✅ Rotation, scale, position, colour, mirror, z-order — all maker-controlled, all existing verbs.
- ✅ Repetition is *manual*. Placing four asterisks in a row is a compositional act and looks like one.
✅ **This clause was challenged on 2026-08-18 and upheld** — [ADR-107 R2](../DECISIONS.md#adr-107).
The owner asked for pseudo-randomised combination; the implementer recommended a maker-triggered shuffle
**before reading this section**, and withdrew it on reading it. Three grounds carried the ban, and the
second is not in this section's original argument: the clause also bans *composition presets*, which are
not random — so it is about **the app proposing arrangements at all**. The shipped `READY-MADE PALETTES`
are the apparent counter-example and turn out to prove the rule, on [§5.2](#s-5-2-ruling)'s own line: a
palette is craft knowledge about the *material*, an arrangement of marks is a *compositional decision*.
The literature agrees where this section argued from taste — modular systems fail through **legible**
repetition (Bethesda's *art fatigue*), which is what a shuffle over templates manufactures.

- ❌ **No "surprise me", no shuffle, no randomiser, no auto-arrange, no composition presets.** These are the
  mechanisms that make output read as generated. Banned — and already on [ZINE-DIRECTION §9.6](ZINE-DIRECTION.md)'s **Never** list.
- ❌ **No procedural variation of the outline** (no "randomise the tear"). Four authored tears beat infinite
  generated ones: a generated tear has no hand in it, and every maker gets a different one.

### 5.1 A supply lands flat, at 0° — reversing §9.3

`ZINE-DIRECTION.md:423` (§9.3) specifies *"a small rotation derived deterministically from the element id."*

**Ruling: supplies land at 0°, and §9.3's tilt clause is withdrawn.** The frozen bench states the rule
itself (`v21-bench.html` §*the banner*, `:23-24`): *"the page never tilts. You are working on it; it sits square to you.
(Tilt is for objects at rest…)"* A supply is placed *on* the page, in the act of working — so the tilt law
(`V21-SPEC.md:494`) does not reach it. It governs the studio, not the page.

The deeper reason: a pre-tilted supply is a compositional decision the app made and the maker didn't, on
the one surface where the maker's hand is the entire product. A deterministic-hash tilt also looks random
right up until two makers notice identical zines tilted identically.

This is a **reversal of published direction and is recorded as one**, not made by omission.

### 5.2 Per-family default scale — the one place the app supplies craft

With 0° landing, one colour, no opacity, no stroke and no size logic, a first placement is a flat glyph
dropped at page centre — and Pass 2 is where "restrained" and "unfinished" get separated.

**Ruling: each family lands at its own default size** — tape lands long, a stamp lands small, a rule lands
wide. This is the app supplying *craft knowledge about the material*, not making a compositional decision,
which is exactly the line §5.1 draws. It costs one constant per family.

#### ✓ RULING — 2026-08-16: four family constants **plus exactly one named override**, and the numbers {#s-5-2-ruling}

*Ruled by the implementer under the standing owner delegation, on the [D-082](V2-SPEC-DEFECTS.md#d-082-rulings) precedent.*

**This section contradicted itself and S7 found it by trying to obey it.** The ruling sentence says *one
constant per **family***; its own three examples end with *"a rule lands wide"* — and `shape.rule` is a
**member** of *Cut shapes*, not a family. You cannot implement both sentences.

S7's first pass implemented the ruling and dropped the example, leaving the rule square. That was the wrong
half to drop, for a reason worth keeping: **Cut shapes is the only family production can currently reach**,
so the spec's single reachable example was the one thing the implementation got visibly wrong. A rule that
lands as a square is not a rule.

**So: per-family defaults, plus one named override, pinned by a test to exactly one entry** — the test is
the load-bearing half, because an override map with no cap is just per-supply sizing with extra steps, and
per-supply sizing is what §5.2 exists to refuse.

| Sizing group | Width (fraction of page) | Aspect | Reading |
|---|---|---|---|
| **Tape** (`tape.*`) | 0.55 | 4.5 | lands long |
| **Fixings** (`fix.*`, enumerated) | 0.20 | 1.0 | lands compact — added 2026-08-20, [D-092](V2-SPEC-DEFECTS.md#d-092-ruling) |
| **Stamps & marks** | 0.16 | 1.0 | lands small |
| **Cut paper** | 0.45 | 1.0 | lands as a piece of paper |
| **Cut shapes** | 0.30 | 1.0 | lands as a shape |
| *override* `shape.rule` | 0.70 | 5.0 | lands wide — §5.2's own example |

Plus a clamp at 0.6 of page height, so a landing can never exceed the page it lands on.

⚠ **AMENDED 2026-08-20 — the key is a *sizing group*, not a family, and the first column says so.**
[D-092](V2-SPEC-DEFECTS.md#d-092-ruling): `fix.corner` is authored 1:1 and inherited tape's 4.5:1, so it
landed as a long flat sliver over half the page wide with its pocket reduced to a slit. **One aspect cannot
serve both halves of a family whose name contains the word "and"** — and *Tape & fixings* is the only one of
the four that is not physically homogeneous, which is why the rule held until the fixings were authored.
The override cap is untouched at one entry: three per-supply overrides would have been per-supply sizing
arriving by instalment. The four families are unchanged — a sizing group is one level below a family, and
inventing a fifth family would put a fifth heading on the Art sheet.

⚠ **The numbers are an implementation reading, not a measurement.** §5.2 gave three adjectives and no
figures. These are defensible and they are unverified. **Pass 2 is what settles them**, and the day it does,
this table is the thing to edit — which is exactly how the `fixings` row arrived: a Pass 1 device run, not a
re-reading of the spec.

---

## 6. The maker's own materials

Supplies is the *provided* vocabulary. Per ADR-104, the maker's own material matters more once the online
layer is closed.

**What already works.** Photo import is complete: content-addressed by sha256, deduplicated, import-master
preserved (ADR-022), reframable (ADR-053). A screenshot, a scan, a photo of a real torn edge — all already
first-class `ImageElement`s.

**The gap.** `app/src/main/AndroidManifest.xml:26-30` declares exactly one `intent-filter`, MAIN/LAUNCHER.
**Zinely is not a share target.** The most natural Android gesture for this product — see something, share
it into your zine — does nothing.

**Ruling: `ACTION_SEND` / `ACTION_SEND_MULTIPLE` for `image/*` is the highest-value item in this document**,
and the clearest expression of the Android advantage: an iOS-first competitor cannot make *share into the
thing you are making* feel this ordinary. It touches no network.

⚠ **It is small in code and not small in verification.** `MainActivity` declares no `launchMode`, so a
single-Activity app receiving `ACTION_SEND` needs a task-unique `launchMode` + `onNewIntent`, task-affinity behaviour,
multi-image handling, permission-less URI reads, and cold-start-into-import. Called out because the first
draft called it "small" without qualification. Vindicated on hardware: `singleTop` — the obvious answer,
and the one the first implementation shipped — is *wrong*, because the share sheet launches with
`FLAG_ACTIVITY_NEW_TASK`. See [ARCHITECTURE §8](../ARCHITECTURE.md#8-navigation).

⚠ **This re-sequences X12.** `ZINE-DIRECTION.md:747` places share-sheet receive in NEXT and Direct Share /
clipboard in LATER. Moving it first is a §15 edit, made there — not shadowed from here.

**Clipboard paste** is second and cheaper, but wants a paste affordance the Bench does not have, so it
queues behind the frozen-spec amendment batch.

---

## 7. The complete primitive model — three, and closed

| Primitive | Answers | Status |
|---|---|---|
| `TextElement` | "what does it say?" | Ships |
| `ImageElement` | "what did I bring?" | Ships |
| `DecorElement` | "what did I make it out of?" | This spec |

**Three is the whole set, closed for beta.** Not because more is impossible, but because each is a full
vertical — model, validation, scene, replay, a11y, context bar, undo, golden coverage. A fourth primitive
proposed during beta should be rejected on cost, and any candidate for one (a drawing tool, a QR code, a
page number) tested first against whether it is really a supply.

---

## 8. Accessibility contract

`EditorA11y` ships **11 shared actions** (`EditorA11y.kt:61-77`, counted), wired through
`ElementSemanticsLayer.kt:93,120`. Decor inherits all 11.

| | Text | Image | Decor |
|---|---|---|---|
| Shared | 11 | 11 | 11 |
| Type-specific | `EDIT_TEXT` | `REFRAME_PHOTO`, `RESET_FRAMING` | `REPLACE_SUPPLY`, `CHANGE_INK` |
| **Total** | **12** | **13** | **13** |

> **As shipped, 2026-08-17: decor has all 13.** Both `Change ink` (`Copy.A11y.CHANGE_INK` →
> `Intent.InkSupply`) and `Replace supply` (`Copy.A11y.REPLACE_SUPPLY` → `Intent.ReplaceSupply`) are
> advertised, and each is gated on its callback being present so no host can advertise a dead one. Decor is
> the **first element kind whose frozen verb set is fully implemented** — text still owes `Font`, photo
> still owes `Replace` ([D-038](V2-SPEC-DEFECTS.md#d-038)).
>
> <details><summary>Superseded note, 2026-08-17 morning</summary>
>
> **As shipped: decor has 12, not 13.** `Change ink` is live (`Copy.A11y.CHANGE_INK` →
> `Intent.InkSupply`); **`Replace supply` is not advertised at all**, because the flow behind it does not
> exist and an action that dispatches nothing costs a blind maker a gesture to discover the same emptiness a
> sighted maker sees greyed out. The table below states the target, and the gap is one action wide.
> </details>

⚠ **This reverses `ZINE-DIRECTION.md:429`**, which states decor needs *"no type-specific action, and none
is needed."* That is wrong, and the frozen spec is why: `v21-bench.html:71` fixes the decor verb set at
**Replace / Ink / Delete**. The Constitution's Interaction clause requires that *"every gesture-driven
action has a named custom accessibility action"* — so `Ink` and `Replace` each demand one. Delete is
already shared. §9.4 and §12's table are corrected.

**Content description** is `"<supply name>, <size>, <colour name>"` — e.g. *"Star, medium, berry"*.
`ZINE-DIRECTION.md:428` had this right and the first draft dropped size; restored.

⚠ **Two naming facts, both corrected from the first draft.**

- **The 19 swatch names already ship.** `benchInkName` maps all three bands — `BenchInkPopover.kt:176`
  (maker), `:190` (tints), `:199` (neutrals). The first draft claimed *"those labels do not exist yet"*,
  misreading `ZinelyContentInks.kt:80` ("leaves the labels to the copy layer") as evidence of absence when
  it states where they live. **S6's real scope is 16 supply names, not 35.**
- **`Ink` is ambiguous** — the same string names a maker ink and a neutral. TalkBack must disambiguate
  (band-qualify the neutral) or two placements read identically.

There is no `EDIT` action for decor because there is nothing inside a supply to edit — which is an argument
*for* the authored-outline design, not merely a consequence of it.

---

## 9. What Supplies is not

- ⚠ **Amendment PROPOSED 2026-08-18 by [ADR-107 R5](../DECISIONS.md#adr-107) — `Proposed`, pending
  [D-080](V2-SPEC-DEFECTS.md#d-080). Nothing below is amended yet; read the ADR before acting.** In short:
  the **search-field** bullet's own premise is *the sixteen*, and ADR-107 R1 spends it — so at ~60 both the
  family **chips** and a **text field** are proposed to ship, on ADR-104's own finding that *"a large
  library without excellent search is worse than none"*. The **tags/filters/sort** bullet states no premise
  and is therefore a straight reversal, recorded as one. ⚠ A first draft of R5 deferred the text field and
  claimed amendment **A5** had removed the chips *"for the identical sixteen-item reason"*; independent
  review showed A5's own comment (`v21-bench.html:451-456`) gives different grounds — filtering ruled out,
  and families already heading their own sections. The second is the real one, and it expires on **scroll
  depth**, not on the sixteen.
- **No search field.** Sixteen items fit on one screen; a search box over sixteen advertises an absence.
  This is the frozen file's own reasoning (`v21-bench.html:447-450`) and what ADR-104 physically removed.
  Amendment **A5** applied the same reasoning to the four family *chips*, which filtered the same sixteen:
  a chip row is that box with four buttons instead of a caret, so it was removed, not re-tasked.
- **No categories beyond the four.** No tags, no filters, no sort.
- **Favourites and recents are ⚠ DEFERRED, not banned.** The first draft banned them and argued for it.
  That was wrong on authority and on product. `v21-bench.html:70` — written under ADR-104 — says *"The
  favourites star stays specified (deferred in sequencing, **not removed from the spec**)"*; the sheet
  draws `☆` on every tile (`:846`) and captions it *"Recent and ⭐ cut long-session friction"* (`:864`);
  `ZINE-DIRECTION.md:655` says *"Not struck, just not first."* Banning them would remove controls the
  frozen file draws — a new amendment I have no authority to make. And the product argument was weak: *"the
  drawer is the same every time you open it"* describes a cabinet nobody uses. A real cabinet is exactly
  where the thing you reach for forty times ends up in front.
- **No packs UI in beta.** Curated packs are post-beta (ADR-104). Shipping the shelf before any pack exists
  is dead UI, which Amendment 3 exists to prevent.
- **No emoji, no imported SVG, no custom shape drawing.** Each re-opens provenance — the question
  Amendment 3 closed. ⚠ **This one does *not* expire with library size** ([ADR-107 R5](../DECISIONS.md#adr-107)):
  OpenMoji was rejected for CC BY-SA, whose ShareAlike would follow into a stranger's printed zine.
  Emoticon-*style* marks authored from a permitted set are ordinary material — the ban is on the licence
  attached to the artifact, never on the shape.

---

## 10. Cost and sequence

Sits behind X3b (the photocopier filter, §0 O-D).

| # | Step | Touches | Note |
|---|---|---|---|
| **S1** | `ACTION_SEND` share target | manifest, `MainActivity` launchMode + `onNewIntent`, nav host, existing import pipeline | **Independent of S2–S8.** Highest value/cost ratio here. |
| **S2** | `DecorElement` + `AffineTransform2D.scale` + validation | `core:model`, `core:data` | See S2′ — does **not** compile alone. |
| **S2′** | The type-switch sites that break with it | `core:editor` (`Elements.kt:17,22`), `core:render` (`SceneRenderer.kt:56`), `feature:editor` (`EditorA11y.kt:31`), `core:data` | **S2 and S2′ land as one commit.** Four else-less `when`s, not one. |
| **S3** | `DrawShape` + `SupplyOutline` + `SupplyCatalog` + the unit-square fold (§3.4.1) | `core:render` | Pure JVM, no device. |
| **S4** | Replay branch + AA `Paint` | `render-android` | One `when` arm, four surfaces (§3.2). |
| **S5** | Author the 16 outlines + attestations (§4.1) | `SupplyCatalog`, colophon | Design work for the last four. **Not** gated on O-B/O-C — both closed in §0. |
| **S6** | 16 supply names | `core:copy` | Ink names already ship. |
| **S7** | Art sheet, Decor context bar (Replace/Ink/Delete), per-family default scale, uniform-scale constraint | `feature:editor` | See S7′. |
| **S7′** | The **silent** seams — `as?` casts and `is`-guards across `feature:editor` (`LivePreview.kt:78`, `EditorA11y.kt`, `EditorGestures.kt:52`, and 8 in `EditorScreen.kt`) | `feature:editor` | **The expensive half.** These fail no test: a decor element that cannot be gestured or previewed, silently. ⚠ The counts here were **stale on arrival** — "6 in `EditorScreen.kt`" was 8 when P-G counted. Re-count before costing; a survey number written once and cited twice is not a measurement. |
| **S8** | Test fixtures — `DocumentSchemaPropertiesTest.kt:19-32` (jqwik arbitraries), `DocumentSchemaTest.kt:15` (wire contract) | `core:model` tests | Unlisted in the first draft. |
| **S9** | Both device-verification passes | — | Pass 2 asks whether the drawer reads as a drawer. |

⚠ **`BenchContextBar` is not fully compile-guarded.** The `error(...)` at `:125` is the documented crash
site, but `benchVerbKindOf` at `:129-133` ends `else -> null` — so `DecorElement` would silently produce
*no context bar* rather than a compile error. **The `else -> null` is the thing that actually needs
removing**, and it is the more dangerous of the two because it is quiet.

**The claim most worth attacking first:** §3.2's vector invariant. The paint-level reasoning is sound and
better-sourced than the first draft's text analogy, but it is still argument rather than measurement. One
page, diffing PDF operators, settles it.

> ✅ **Attacked, 2026-08-16, and it held — but the proposed instrument did not.** The spike found export
> draws through `PdfDocument`'s `Canvas`, so Skia emits the operators and there was never a missing-operator
> problem to solve. It also found that "diff PDF operators" is the *wrong* check: it needs a parser we do
> not have and is blind to the one error this design can make. §3.2 and §3.5 now specify the hole test
> instead. **A spike is allowed to reject its own method, and this one's method was the part that was wrong.**

### 10.1 What has actually landed, 2026-08-16

| Step | State |
|---|---|
| **S1** | ✅ Shipped. Also found the `singleTop` second-task defect on device, which no test could see. |
| **S2 / S2′** | ✅ Shipped as one change (P1). Schema **v1 → v2 with an identity migrator** — the migrator is required because `DocumentMigrations` enforces a contiguous chain and throws on a gap. |
| **S3** | ✅ Shipped (P2). `DrawShape` · `SupplyOutline` · `SupplyCatalog`. |
| **S4** | ✅ **Shipped (P3).** `CanvasReplayer:132` sets `shapePaint.color` and calls `canvas.drawPath(command.outline.toPath(), shapePaint)` — its **own** paint, AA on, deliberately not the pinned `fillPaint` (§3.5). ⚠ This row read *"stubbed, not done"* until 2026-08-17, when **the device contradicted the document**: a placed `shape.rect` drew as a filled square on SM-A176B. The row was stale from the commit that closed it. *A status table is a claim like any other.* |
| **S5** | ⏳ **12 of 16.** *Cut shapes* shipped first as derivable geometry — and then the same reading was applied to the rest, which is where the earlier status was wrong twice over. ⚠ **The twelve were recorded as "gated on O-B/O-C", and they never were**: [§0](#0-the-four-escalated-calls--decided) closes all four escalated calls in as many words (*"Nothing here is left open"*). What actually held them was a designer's hand, which is a resource, not a ruling — and the second package proved eight of them did not need one. ⚠ **Derivability does not run along family lines.** The eight added (`mark.registration` · `mark.halftone` · `mark.asterisk` · `mark.arrow` · `paper.window` · `paper.tag` · `fix.staple` · `fix.corner`) come from three families; *Cut shapes* was simply the family authored first. **Four remain**, each for a reason recorded in `SupplyCatalog`: `tape.torn` · `paper.strip` · `paper.underline` need an authored *tear* (§5 bans generating one), and `fix.clip` is a **wire object in a fill-only renderer** — the one row of [§4.3](#curation-list)'s "derivable" column that did not survive contact with `DrawShape`. `outlineOf()` returns `null` for each. |
| **S6** | ✅ Shipped. Sixteen names in `core:copy`, five documented departures from §4's prose. |
| **S7 / S7′** | 🟡 **Partly landed (P-G).** The **Art sheet** exists — sixteen tiles, four headings, four inert (twelve when it landed) ([ADR-105 amendment](../DECISIONS.md#adr-105), [D-086](V2-SPEC-DEFECTS.md#d-086)) — and the `INK` **routing** defect is fixed: `EditorScreen` now derives `inkPopoverVisible = inkPopoverOpen && inkTarget != null`, with all five consumers reading the derived value, so the stranded empty-popover state is **unconstructible** rather than guarded at one entry. The verb stays disabled. ⚠ The *trigger* is still unexercisable for decor precisely because that verb is disabled — the fix is pinned by construction and by the live text path, not by a decor tap. **S7-placement then closed the loop:** `Intent.PlaceSupply`, the [§5.2 scale ruling](#s-5-2-ruling), the Add chooser's **Art row released**, and the sheet wired so an authored tile places and an inert tile stays a no-op. **Still owed:** S7′'s silent seams — two of which S7-placement found by mutation and fixed (`benchDeleteLabel` said *"Photo deleted."* for a supply; `benchInkCount` under-counted the print cost).

✅ **S7′ surveyed 2026-08-17 and it is CLOSED: zero broken silent seams.** 24 type-branching sites in `feature:editor` were read — **20 SAFE, 4 correct-by-accident, 0 broken**. **This step's premise was stale in three separate ways**, and the pattern is worth keeping: `benchVerbKindOf`'s dangerous `else -> null` no longer exists (it is an exhaustive three-arm `when`); `LivePreview.kt:78` is in `core:editor`, not `feature:editor`, and its `if (element !is TextElement) return@map element` is correct; and the row's "expensive half" framing was wrong — P1–P3 landing exhaustive `when`s over the sealed type had *already* closed the seams as a side effect. Hit-test, drag, resize and z-order turn out to be type-blind by construction (`HitTest` reads transform and z only), so a supply inherited them for free.

⚠ The four **correct-by-accident** sites were not defects and were left alone: `EditorScreen.kt:1424` (`styleable … ?: true`) and `:1427` (`copier ?: false`) fed a verb set that ignores both for `DECOR`, and `BenchInkPopover.kt:125-142`'s `DECOR` arm was *live code on an unreachable path*. Each was right only because a second thing was true elsewhere. **They become defects the moment decor Ink is enabled** — which is exactly the next package.

✅ **Followed up 2026-08-19, after `S7 · Change ink` shipped. The landmine did not go off, and for two different reasons — neither of them luck.**

- The two `EditorScreen` sites were **rewritten**, not survived: `:1534-1537` is now an exhaustive `when` over the element type (`is DecorElement, is ImageElement, null -> false`), and `:1540` reads `copier` off `ImageElement` specifically. The elvis defaults that made them accidental are gone.
- `BenchInkPopover.kt:138-143` is unchanged and is now **correct by ruling, not by reachability** — [§0 O-A](#o-a--decor-tints-from-the-content-palette--all-three-bands): *decor tints from the content palette, all three bands.* O-A states and rejects the exact objection this note was worried about: *"a paper tint laid as a mark reads as nothing — is true and is the maker's call."*

⚠ **What did change is which arm is load-bearing.** `BenchContextBar.kt:151-168`'s `PHOTO` verb set is Reframe / Copier / Replace / Delete — **there is no photo `Ink` verb at all**, so the arm's `PHOTO` half is the unreachable one and `DECOR` is now the *sole* consumer of the `Paper tints` band. Measured against the only paper a page can carry (no `Background.Solid` is constructed anywhere in `feature/`, so every supply is drawn on `#F4EFE6`/`#EDE6D9`): Cream 1.06:1 · Blush 1.13:1 · Sky 1.08:1 · Sage 1.09:1 · Kraft 1.28:1. That is O-A's accepted outcome quantified, not a new finding — but it means the TEXT fence and the DECOR grant now rest on one ruling each rather than on one ruling and one accident.

*The lesson for costing: a step described as "the expensive half" was measured, not re-estimated, and the measurement retired it. The `⚠` on this table warning that its own counts were stale was right, and understated.* |
| **S8** | ✅ Shipped with S2. |
| **S9** | 🟡 **Pass 1 run 2026-08-18 (SM-A176B, Android 16); Pass 2 begun.** The blocker in this row's own text is gone — S4 ships, and twelve supplies now draw. **Pass 1: five of six questions passed** — all eight new marks render; `mark.halftone` stays a legible lattice at its 0.16 landing (the prediction most expected to fail); `mark.registration`'s arm/ring join reads joined; `paper.tag` reads as speech; and the **platform** `AccessibilityNodeInfo` tree agrees with the semantics tree on all sixteen tiles — exactly the four unauthored report `enabled=false clickable=false`. **One Pass 1 defect: [D-092](V2-SPEC-DEFECTS.md#d-092)** — a photo corner lands as a 4.5:1 sliver because *Tape & fixings* carries tape's aspect. ⚠ **Pass 2 raised [D-093](V2-SPEC-DEFECTS.md#d-093)**: every tile is drawn hollow and every supply lands solid. **Acceptance requires both passes**, so S9 stays open until D-086, D-092 and D-093 are ruled.

✅ **All three ruled and implemented 2026-08-20** ([ADR-108 accepted](../DECISIONS.md#adr-108-shipped)): the tile now renders the authored outline itself, `BenchArtGlyphs` is deleted, `v21-bench.html`'s glyphs are generated from `SupplyCatalog` (amendment **A7**), the four unauthored tiles carry `Not available yet` and draw no mark, and the fixings get their own sizing constant. ⚠ **S9 still stays open**, and for the reason it was always going to: the three rulings were *implemented* on the strength of a golden diff and a spec reading, and **neither pass has been re-run on a device against the built result**. A Pass 2 finding closed by an implementer who then declares his own fix understood is the disqualification [Pass 2](../../CLAUDE.md#pass-2--first-time-user-verification) names, word for word. |
| **S7 · Replace supply** | ✅ **Shipped**, closing decor's verb set. `Intent.ReplaceSupply` → the same `EditDecorCommand` (id, ink, mirror and z survive a swap; only `supplyId` and `transform` change, enforced by the `copy` naming exactly those two). The Art sheet is re-summoned as a **picker**: `artSheetFor: BenchArtPurpose?` replaces the old `Boolean`, so the sheet cannot be open in replace mode with nothing to replace, nor in place mode holding a stale id — the [D-091](V2-SPEC-DEFECTS.md#d-091) lesson applied *before* the defect rather than after it. **Owner ruling, 2026-08-17: a replacement takes the incoming family's §5.2 scale, at the outgoing element's centre and rotation.** The two rejected readings and why are recorded at `benchSupplyReplacement`. No snack: unlike a placement, a delete or an ink, a swap redraws the selected mark in place under the maker's own eyes.

**Pass 1 device verification — PASS**, SM-A176B / Android 16, 2026-08-17, and **re-run 2026-08-18 against the committed build `126d84e`** — the first time this evidence is attached to a named commit rather than to a working tree. Both runs agree. On the platform `AccessibilityNodeInfo` tree **all three decor verbs read `clickable=true enabled=true`** — `Replace` flipped from `false` where TalkBack reads it. Tapping it re-opened the cabinet as a picker; tapping *Circle* swapped the outline **in place**, node bounds byte-identical (`[430,736][650,956]` before and after) because both are *Cut shapes* and share a family default.

**The ruling itself was proved by swapping to *Straight rule***, the one supply carrying a §5.2 override (0.70 width, 5:1). Result `[283,797][797,910]`: centre held at x=540 (846→847, one pixel of rounding) and width grew 220px→514px — which is 0.70 of the ~733px page against the circle's 0.30, i.e. **exactly a fresh placement's size for the incoming supply**. The node's height reads 126px rather than the drawn ~103px because `ElementSemanticsLayer` inflates every node to a 48dp minimum touch target (48 × 2.625 = 126); that is the a11y floor, not the geometry. Three undos restored the page. — **Reproduced exactly on `126d84e`:** rectangle `[430,736][650,956]` (w=220, centre 540) → straight rule `[283,784][797,910]` (w=514, centre 540). The centre held to the pixel this time, where the 2026-08-17 run rounded by one, and the width ratio is the same 0.70-against-0.30. Undo then went `enabled=false` with Redo live, which is the page back at its original state rather than merely looking like it.

⚠ Same two gaps as *Change ink*: the decor row is now observed — [D-090](V2-SPEC-DEFECTS.md#d-090) closed 2026-08-18, on the local host only, and the **custom action** was exercised only in Robolectric — `uiautomator dump` cannot show custom actions, so hearing it belongs to the owner's TalkBack pass. |
| **S7 · Change ink** | ✅ **Shipped.** [`Intent.InkSupply`] → `EditDecorCommand` (one tap, one undo step, no entry when the ink is unchanged); the decor `Ink` verb enabled; `benchInkTargetOf` widened to text-or-decor with `benchInkColorOf` beside it; the popover's bands routed from `ctxKind` instead of a `TEXT` literal, which connected the `PHOTO, DECOR ->` arm that had been live code on an unreachable path; and §8's `Change ink` custom action added, so a supply can be recoloured by TalkBack. **`Replace supply` shipped in the row above**, closing the pair. *(This sentence read "remains inert" for half a day while the row above it said ✅ Shipped — caught by independent review. Two rows of one table disagreeing is the same failure as two documents disagreeing, at smaller scale.)* Independent review returned **NO-GO** on the first cut and was right: the a11y action opened nothing on its primary path ([D-091](V2-SPEC-DEFECTS.md#d-091), fixed, regression test verified by mutation).

**Pass 1 device verification — PASS**, SM-A176B / Android 16, 2026-08-17. Read from the platform `AccessibilityNodeInfo` tree, not merged semantics: `Ink` is `clickable=true enabled=true` and `Replace` is still `clickable=false enabled=false`, so the enablement is real where TalkBack reads it. The popover opens on a supply carrying the **decor** band set — `INKS · PAPER TINTS · NEUTRALS · READY-MADE PALETTES` — which is the first time `benchInkBands`' `PHOTO, DECOR ->` arm has rendered anywhere. Picking *Strawberry* recoloured the mark; the ring followed the element's own ink; the popover's cost note read *"This one uses 4"*, so `benchInkCount` counts a supply's ink. Undo restored the previous ink exactly, and a second Undo removed the placement.

⚠ Two things Pass 1 does **not** cover: the palette is now observed — [D-090](V2-SPEC-DEFECTS.md#d-090) closed 2026-08-18, on the local host only, and `Change ink` was exercised through the visible verb — the **custom action itself** is covered only by the Robolectric regression test, because `uiautomator dump` cannot show custom actions. Hearing it is part of the owner's TalkBack listen pass. |

**The sequencing lesson S5 taught:** it was written as one indivisible block of design work, and a quarter
of it was not design work at all. A step that mixes "needs a house style" with "is a rectangle" will always
look blocked by its hardest quarter.
