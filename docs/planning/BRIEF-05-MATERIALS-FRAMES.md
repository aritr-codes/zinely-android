# Brief 05 — Materials: decorative frames and the Art backlog

> This brief incorporates the findings of [ZINELY-1X-READINESS-AUDIT.md](ZINELY-1X-READINESS-AUDIT.md) and the owner's rulings of 2026-09-26 ([decision gate Q3, Q5, Q6](ZINELY-1X-DECISION-GATE.md)). If this brief conflicts with an older research document, this brief and the cited authoritative ADR/decision take precedence. It never overrides an Accepted ADR, the V2 constitution or a frozen spec — where it needs one changed, it says so and names the amendment.

Status: **implementation brief, not authorised.** Direction D5 of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md).
Base: `origin/main` @ `0aa7a7d`. Revised 2026-09-26 for the owner's rulings; code facts re-verified on that commit.
**Readiness, by part:**
- **Outline hit test (plan step 4):** READY AFTER (1) the owner approves the [`v21-bench.html` amendment](#v21-benchhtml-amendment-specification) below, and (2) PR #70 is settled (merged or closed), because it amends the same file. Scope is ruled ([Q3](ZINELY-1X-DECISION-GATE.md#q3-tap-through-hit-testing-how-far-it-reaches)): the six holed pieces.
- **D5 frames (plan step 9):** BLOCKED BY the owner's visual approval of **two** frames (names, visuals, set membership, order) and by step 4 having merged. The fold study is **not** a gate ([Q5](ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14), owner 2026-09-26).

## Gates before an implementation session may start

**Outline hit test (step 4):**

1. **Owner — approve the `v21-bench.html` amendment** ([specification below](#v21-benchhtml-amendment-specification)). The box hit test is inherited behaviour, so changing it is an **interaction change**, and interaction changes after freeze update the HTML first (CLAUDE.md, *DESIGN FREEZE*). No Compose or core code before approval.
2. **Work — PR #70 settled.** It amends the same `v21-bench.html` (and holds ADR-115). Amending the file on top of an open PR invites a line-shifting conflict in a file whose citations are line numbers.
3. **Work — the [ADR draft](#adr-draft--outline-hit-testing-for-holed-supplies) recorded** in `DECISIONS.md` under the next free number at implementation time, and independently reviewed.

**D5 frames (step 9):**

4. **Owner — visual approval of two frames** ([Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch)). This brief calls them *Frame A* and *Frame B*. Their **visuals** are the owner's by [Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch); their **set membership, drawn names and order** are the owner's by [ADR-107](../DECISIONS.md#adr-107) R1a (`DECISIONS.md:11485-11486`). R1a leaves the outline *authoring* to the implementer, which Q6 delegates under the owner's visual approval. Search tags are not covered by R1a: they are drafted in the set amendment (gate 7) and reviewed with it. Nothing here proposes names or visuals.
5. **Work — step 4 merged.** Adding frames before the hole passes taps through would multiply the defect.
6. **Work — the per-piece resize list finalised** and recorded against [D-100](../design/V2-SPEC-DEFECTS.md#d-100). The owner ruled that resize is decided per piece and delegated the list; the recommendation is in [*Frames (D5)*](#frames-d5).
7. **Work — the frozen-HTML set amendment**: a reviewed change to `docs/design/mockups/v21-bench.html` that adds the two approved frames to the Art sheet (set, names, order, search tags) and records the per-piece resize behaviour. It comes before any Compose work. See *UI/UX proposal*.
8. **Owner, optional — ratify "imperfect surface, perfect mechanics"** (research proposal D13; [decision gate, "Decisions that can wait"](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)). This does not block D5. This brief uses the phrase as an evaluation lens only.

The fold study runs now, combined with the print study where practical ([Q5](ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14)). It is evidence for Proof's fold instructions, not a gate on this brief. If it uncovers a fundamental product failure, the owner may reprioritise on that evidence.

## Problem

The owner's reopened creative-tools list includes **frames** ([ROADMAP creative-tools assessment](../ROADMAP.md#creative-tools-assessment-2026-09-12): "small to medium for decorative overlays; medium to large for attached frames"). The earlier claim that "no frames exist" was wrong. ✅ **Six holed supplies already ship.** Each outline has an inner subpath, and the even-odd fill turns it into a hole:

| Supply | Name | Outline (`SupplyCatalog.kt`) |
|---|---|---|
| `paper.window` | Window frame | `:241-262` |
| `paper.hole` | Torn hole | `:490-497` |
| `shape.ring` | Ring | `:503-505` |
| `fix.grommet` | Eyelet | `:412-414` (byte-identical to the ring) |
| `mark.registration` | Registration cross | `:162-169` (arms plus an annulus) |
| `fix.corner` | Photo corner | `:284-287` (a triangle with its pocket cut out) — **missed by the audit** |

Six more supplies have empty space inside their box without having a hole. They are made of separate parts: halftone, saddle stitch, crop marks, colour bar, copier streak and perforation (`:229-239`, `:408-410`, `:427-453`). `mark.crop` (four corner Ls around an empty centre) works as a frame in practice, but it is **not** in the first hit-test scope (Q3). A later proposal could give it a geometric "inside the corner marks" region; that is not approved.

✅ **These pieces already ship with a defect.** The hit test is a box test (`HitTest.kt:21-36`): it rotates the tap into the element's frame, checks it against `±w/2, ±h/2`, and picks the topmost element by `(zIndex, listIndex)`. So:
- a tap in a frame's empty centre selects the frame, never the photo beneath it;
- double-tap to Reframe that photo is blocked the same way (`EditorReducer.kt:137-145` uses the same `topmostAt`);
- the only workarounds are Send backward and TalkBack.

✅ **Resize is free-aspect.** Handle resize sets width and height independently (`TransformMath.kt:77-78`). Pinch is uniform (`:30-47`). The SUPPLIES-SPEC §3.4.1 uniform-scale lock was never built (`AffineTransform2D.kt:60-65`, D-100). So a window frame on a 4:3 box gets uneven borders (`WINDOW_BORDER` is 0.14 of each side, `:242`), and a ring becomes an ellipse. The owner ruled 2026-09-26 that resize is **decided per piece** ([Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch)), with no nine-slice.

**Carried over and still correct:**
- *Mirror* is not missing. ADR-113 Flip toggles `DecorElement.mirrored` (`EditorReducer.kt:212`, `FlipTray.kt:333`, `EditorA11y.kt:158-161`). The KDoc at `Intent.kt:53` is stale, and so is the OWNER-CHECKLIST Mirror row (already flagged "likely stale", `OWNER-CHECKLIST.md:205`).
- *Composite frames* (corner + edge slots) were withdrawn by ADR-107 R3, because even-odd turns any overlap into a hole.

## Product audit

### User value
🟨 Putting a photo in a window, mat or torn hole is a common collage move (an assumption, not measured). Six pieces already offer it, but the photo underneath can't be tapped. So the first user value is the hit-test fix, not new frames: it makes the pieces that already ship work. Two new frames then add variety. Instant-photo borders are out: they imitate an object rather than cut one, which fails ADR-107 R1's test, and V2-CONSTITUTION §IV names tilted "polaroid" frames as costume.

### Creative value
**Frames that belong in Zinely** reproduce what a maker does with scissors, a torn page or a photocopier: a window cut into paper, a torn hole, a perforated edge. They pass ADR-107 R1's test, "Does it attach, point, tear, or cut?" ("cut"), and its second filter: can the maker already make this with a verb they have? The owner ruled a **hand-cut** visual character for the first two.

**Canva-frame drift starts** when the app does the composing:
- photo-in-frame masks that snap and clip the photo (Canva's model, below);
- frames that fit themselves to a photo;
- themed or seasonal borders, or flourishes with no physical source;
- frames with words in them ("Happy birthday");
- frame+photo groups.

Each of these is an app-proposed arrangement, which ADR-107 R2 refuses. **The line: a frame is a piece of paper the maker lays down; it never holds, fits or clips a photo.**

### UX cost
- No new surface: frames are tiles in the existing families.
- The hit-test fix changes what a tap does over **six** pieces (Q3). A tap on the empty part of their outline reaches whatever is underneath, and falls back to the piece only if nothing is. Every other piece keeps its box. Over blank paper nothing changes, apart from the small tolerance just outside a holed piece's ink (*Interaction details*).
- Per-piece resize changes how handles behave on the locked pieces (Gate 6).

### Cognitive load
The Art sheet grows by two tiles. Families and search already carry that load (ADR-107 R5).

### Accessibility
- Each frame needs a spoken name. Decor already gets Flip, Ink and Replace custom actions (`EditorA11y.kt`).
- TalkBack is the one path the hit-test defect never touched: `ElementSemanticsLayer` places each element's node by its **box** (`ElementSemanticsLayer.kt:88`, `SelectionChromeGeometry.outlineDevicePx(element.transform, …)`), not through `HitTest`. The fix leaves the semantics tree unchanged, unless implementation evidence shows a separate accessibility change is needed — which would be its own reviewed change.
- 🟨 **Estimate: a frame's band may be below 48dp on a phone.** The *Cut paper* default is 45 % of the page width (`SupplyPlacement.kt:121`). On an A4 eighth-page (~74 mm wide) that makes a ~33 mm frame with a ~4.7 mm band, so a few mm on screen. That is below the [48dp target guidance](https://developer.android.com/guide/topics/ui/accessibility/apps). The touch tolerance does **not** fix this over a photo, because the photo contains the tap exactly and an exact hit wins (see *Interaction details*). Record the band size and grab success in Pass 1.
- How decor is spoken is D4's sub-decision ([audit §11](ZINELY-1X-READINESS-AUDIT.md#11-owner-decisions)). D5 adds no new rule for it.

### Offline-first and ownership
Outlines are code in `SupplyCatalog`, bundled, authored from scratch, and attested in the file's header (`SupplyCatalog.kt:41-48`). There is no download, no pack store and no licence surface. Each new outline needs the same attestation.

### Physical publishing (PDF and paper)
- Outlines are vector, even-odd, and fill-only through the one replayer (`DrawCommand.kt:86` `DrawShape`), so the frame the maker sees is the frame that prints.
- Solid bands photocopy well.
- The risk is a frame used as a **page border**. [ADR-039](../DECISIONS.md#adr-039) tiles edge to edge, and home printers can't reach the paper edge. A border has to sit inside the Bench keep-clear inset (17 pt) or the printer clips it unevenly. Treat it as ordinary decor the maker sizes inside keep-clear. There is no page-level frame feature. ⚠ Whether one global inset is right is an open question of the print study ([Q7](ZINELY-1X-DECISION-GATE.md#q7-print-o13--d3-stage-2)). Its output is **guidance for how frames are used, not a gate**: neither step 4 nor step 9 waits for it.

### Product identity
The lens is **"imperfect surface, perfect mechanics"**. The owner uses it to evaluate directions. It appears as research proposal D13 and as a website story beat in [ADR-118](../DECISIONS.md#adr-118), but it is **not ratified as a product principle**.

Frames are where the two halves meet:
- **Surface, allowed to be rough:** a hand-cut band, a slightly off-square window. This is where authored imperfection belongs (ADR-107: "feel authored"; ADR-105 D-2: "§IV governs the studio; supplies are what the studio is stocked with", `DECISIONS.md:11021-11027`).
- **Mechanics, which must be exact:**
  - a tap in the hole must reach what is beneath, every time, on every holed piece;
  - the drawn hole and the hit hole must be the same shape, in the same place after rotate, flip and resize. They come from the same outline, which is why the containment test lives in `core:render`;
  - dragging a frame over a photo snaps centre to centre (`Snap.kt:17-24` snaps box edges and centres), so centring is exact without outline-aware snapping;
  - resize must do what the per-piece list says, and nothing else.

**What to cut to avoid becoming a generic editor:** masks, attached frames, auto-fit, frame+photo grouping, themed borders, text-bearing frames and nine-slice borders. Any of these turns a material into a template.

## What others teach us

- **Canva's frames are masks.** The maker drags a photo onto a frame, it "snaps into place" and is cropped to the frame's shape. Double-click edits the crop ([Canva Help: Use frames](https://www.canva.com/help/using-frames/)). → That is exactly the attached, app-composed model. It confirms the choice to keep Zinely's frames as overlays the maker positions, and to keep photo cut-outs a separate direction (ROADMAP: "medium to large").
- **Vector hit-testing uses the painted interior, not the box.** In SVG, the default `visiblePainted` targets an element "when the pointer is over a 'painted' area", meaning its fill or stroke ([SVG 2, Interactivity](https://svgwg.org/svg2-draft/interact.html)). The fill-rule decides "what parts of the canvas are included inside the shape" ([SVG 2, Painting](https://svgwg.org/svg2-draft/painting.html)), so an even-odd hole does not catch the pointer. → This is the precedent for testing the outline, even-odd, in unit space.
- **Figma gives an escape hatch for stacked layers.** A "Select layer" context menu lists the layers under the cursor, and a modifier "deep select" picks nested ones ([Figma Learn: Select layers and objects](https://help.figma.com/hc/en-us/articles/360040449873-Select-layers-and-objects)). → Zinely does not need a menu while a tap in the hole passes through. If Pass 2 finds pieces still unreachable, this is the pattern to consider, not a hidden gesture.
- **Keeping borders fixed while stretching needs its own mechanism.** Android's NinePatch marks which parts stretch and which stay fixed ([Android: NinePatch drawables](https://developer.android.com/develop/ui/views/graphics/drawables#ninepatch-drawables)). → The owner ruled **no nine-slice**. A frame whose band must stay even is therefore locked to uniform scale; one that may stretch accepts uneven bands as part of its hand-cut character.

## User story

*As a maker, I want to lay a hand-cut frame over my photo, tint it with my ink and move it like any other piece of Art, and still be able to tap through the hole to reach my photo.*

## Experience

Bench → Add → Art → the family the owner assigns shows the two frames. Tap one and it lands at page centre, selected (ADR-105), tintable, flippable, deletable and replaceable, like every other supply. The maker drags it over the photo (centre snap helps), resizes it as its per-piece rule allows, and taps inside the hole to select or double-tap the photo.

## UI/UX proposal

- **Step 4 (hit test):** no visual change, but an interaction change. The [amendment below](#v21-benchhtml-amendment-specification) goes into `v21-bench.html` **before** the code, and the owner approves it.
- **Step 9 (frames): amend `v21-bench.html` first** (ADR-107 R1a). The amendment:
  - adds Frame A and Frame B to the staged list `ART_FIRST_WAVE` (`:1736-1753`), in the owner's order, under the owner's family (`FAMILIES` `:867`, `openArt` `:874`);
  - adds their search tags to `ART_TAGS` (`:1754`);
  - adds them to the hole-bearing list the step-4 amendment corrected (`:479-481`, `:1431-1432`), if they have holes;
  - records the per-piece resize behaviour of every supply the lock changes, since handles behaving differently per piece is an interaction change on a frozen surface;
  - records the change in the amendment log.

  Tile path data is **generated from the catalogue**, not drawn by hand (`BenchArtSheetParityTest`, noted at `:874-879`). The outlines therefore exist before the HTML is final, and the amendment and the outline commit land together.
- Names follow the `Copy.Supplies` rules: one spoken word or a short phrase, no slash pairs, no prefix collisions, no trademarks (`SuppliesCopyTest`). Search tags include "frame", "border" or "mat" where they fit. The names themselves are the owner's.

## Interaction details

**Hit test (step 4, its own session, before D5).** The rules, as the owner ruled them (Q3):

- **Scope:** exactly the six holed pieces — `paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `fix.corner`, `mark.registration`. Crop marks and every other supply keep the box.
- **Drawn region wins:** a tap on the ink of an in-scope piece selects it. An exact hit (the point itself is inside some element's hit area) beats a near-hit.
- **Empty region passes through:** a tap in the hole, or elsewhere in the box but off the ink, reaches the element beneath.
- **Small touch tolerance** around the ink (value below).
- **Nothing beneath:** the piece stays selectable by its box.
- **TalkBack unchanged:** `ElementSemanticsLayer` keeps boxes.

Mechanics:

1. A pure even-odd containment function in `core:render` over a `SupplyOutline` and a unit-space point (math in the [ADR draft](#adr-draft--outline-hit-testing-for-holed-supplies)). Deterministic, `kotlin.math` only (coupling rule 1, [audit §10](ZINELY-1X-READINESS-AUDIT.md#10-architecture--ios-implications)).
2. `HitTest.topmostAt` resolves in three passes, each topmost-first by `(zIndex, listIndex)`:
   - **Exact:** the topmost element whose *hit area* contains the point. Text, image, out-of-scope decor and decor with no outline use the **box**, unchanged (so a tap on empty text or a `Fit.FIT` letterbox still selects them). In-scope decor uses its **outline**.
   - **Near:** if nothing matched exactly, the topmost in-scope decor whose outline lies within the tolerance radius of the point.
   - **Box fallback:** if still nothing, today's box test. A tap in a hole over blank paper selects the piece.
   - Unit-space mapping, the exact inverse of the render fold (`SceneRenderer.kt:138-146`: `localToPage · scale(w, h) · mirrorX? · mirrorY?`): subtract the box centre and un-rotate (as `contains` does today), then `u = (lx + w/2)/w`, `v = (ly + h/2)/h`, then `u → 1−u` if `mirrored`, `v → 1−v` if `flippedVertically`. The two mirrors commute, so their order is free.
3. **Touch tolerance.** The caller supplies the radius in page points (`tolerancePt = radiusDp · density / screenPxPerPt`), as `Snap` already takes `thresholdPt`. That is one parameter on `SelectAt`, `BeginEditTextAt` and `DoubleTapAt` and on `HitTest.topmostAt`, defaulting to 0. If it grows past that, stop and ask. A near-hit samples ~8 points on the circle of that radius; any sample inside the outline is a near-hit.
   - 🟦 **Recommendation: 8 dp.** Under exact-first, the tolerance decides only taps that no element contains exactly: just outside an in-scope piece's ink over blank paper (where today's box does not reach beyond the box edge), and taps between stacked holed pieces. It cannot make the band easier to grab over a photo, since the photo contains the point. So its job is modest and its cost is real: every dp of radius is blank paper that no longer clears the selection. 8 dp (≈ 4.8 pt at fit zoom, 🟨 from the ≈ 0.6 pt/dp estimate) forgives a fingertip landing just off a thin band without making a deselect tap near a piece unreliable. The earlier ≈ 24 dp figure assumed near-hits could compete with exact hits; they cannot under the owner's rule.
   - ⚠ **Open, for Pass 2:** if makers cannot grab a thin band over a photo, the choices are to let a near-hit on in-scope ink compete by z-order at a radius no larger than the smallest default-size hole allows (the eyelet's hole radius ≈ 12 pt, the registration cross's ≈ 6.5 pt, 🟨), or a Figma-style layer escape hatch. Either is a new owner ruling, not an implementer's tweak.
4. **Module seam:** decided in the ADR draft (🟦 `core:editor` → `core:render`).
5. **What it affects:** `SelectAt` (`EditorReducer.kt:55-56`), `BeginEditTextAt` (`:120`), `DoubleTapAt` (`:137-146`) and `benchTapIntent` (`EditorGestures.kt:47-54`). A drag moves the selection as it stood at touch-down (`EditorGestures.kt:146`), so dragging is unaffected. `DoubleTapAt` keeps its type switch: through a hole onto a photo it opens Reframe; on a piece's ink it stays the deliberate decor no-op (`:141-145`).

### Frames (D5)

Constraints only; the frames themselves are the owner's (Gate 4).

- **Two frames, Frame A and Frame B,** hand-cut in character, each passing ADR-107 R1's "cut" test. R1a makes set, names and order the owner's call; R3 forbids composites.
- A frame is **one outline, one ink**: outer contour plus inner-hole subpath, even-odd, with no overlapping subpaths (rule 4 is a review obligation that no assertion can check; `SupplyCatalog.kt:110-115`). A holed frame joins the hit-test scope by being added to the in-scope list in the same change (the list is explicit, not inferred from subpath count).
- Frames are **overlays**. Moving the photo does not move the frame.
- ADR-105 D-2 permits the torn or hand-cut look on content: "**§IV governs the studio; supplies are what the studio is stocked with**" (`DECISIONS.md:11021-11027`).
- **No nine-slice.** Borders stretch with the box unless the piece is locked.
- **Landing size:** the family default (*Cut paper* is 45 % width, square, `SupplyPlacement.kt:121`). A non-square landing (e.g. 4:3 for a photo frame) needs an entry in the `{shape.rule}`-only override map (`SupplyPlacement.kt:133-139`), which the file itself calls "a per-family constant quietly giving up" and `SupplyPlacementTest.kt:93` pins. Any such entry is an owner ruling on SUPPLIES-SPEC §5.2, not an implementer's edit. **Who owns what:** landing at the family default is delegated ([gate Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch)); a per-supply override is the owner's.
- **Print inset:** see *Physical publishing*. The print study's output is guidance, not a gate on step 9.
- Art never gets opacity (SUPPLIES-SPEC).

**Per-piece resize (🟦 RECOMMENDATION; the owner ruled "per piece" and delegated the list).** The implementation session finalises it and records it as D-100's ruling:

| Behaviour | Supplies | Why |
|---|---|---|
| **Uniform-scale lock** (handles keep aspect; pinch unchanged) | `shape.circle`, `shape.ring`, `fix.grommet`, `mark.registration`, `mark.asterisk`, `mark.burst`, `mark.hand` | Round or radial pieces whose meaning is their proportion; a stretched registration cross breaks D-095's bare centre. |
| **Free stretch** (today) | strips, tape and cut paper, including `mark.perf`, `mark.bar`, `mark.scan` | Stretching a strip is what a strip does. |
| **Decide with the visuals** | Frame A, Frame B | Even band → lock; hand-cut band that may go uneven → free. |

Every other supply keeps today's free stretch unless the implementation session finds a reason, recorded in D-100. The lock touches `TransformMath`/`ResizeHandle` plus a per-supply set; it is its own small session (or the first commit of step 9), with a `v21-bench.html` behaviour note (Gate 7).

## ADR draft — outline hit testing for holed supplies

> **ADR-NNN (next free at implementation time) — outline hit testing for holed supplies.** Status: *Draft*, to be recorded in `DECISIONS.md` by the step-4 session. Step 2 (reading order) claims the next free number first; ADR-115 is reserved on PR #70's branch.

**Context.** Six supplies ship with an even-odd hole (`paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `fix.corner`, `mark.registration`). `HitTest` (`core/editor/.../HitTest.kt:21-36`) tests the rotated box, so a tap in a hole selects the piece and never reaches the photo or text beneath. Double-tap to Reframe and tap-to-edit-text are blocked the same way, because `SelectAt`, `BeginEditTextAt`, `DoubleTapAt` and `benchTapIntent` all call `topmostAt` (`EditorReducer.kt:56,120,137`, `EditorGestures.kt:49`). The outline that draws the hole lives in `core:render` (`SupplyCatalog.outlineOf`, `SupplyOutline`: closed subpaths of lines and cubics in the unit square, even-odd, fill-only), reached by the render fold `localToPage · scale(w, h) · mirrorX? · mirrorY?` (`SceneRenderer.kt:97-107,138-146`). `core:editor` depends only on `core:model` (`core/editor/build.gradle.kts:10,18`). The owner ruled the scope and rules on 2026-09-26 (decision gate Q3).

**Decision.**
1. **Scope.** Exactly the six pieces above, named in one explicit set in `core:render`. Crop marks and all other supplies keep the box. A later piece joins by being added to the set in a reviewed change.
2. **Rules.** Exact hit beats near-hit; the drawn region of an in-scope piece is its hit area; the empty region passes through; a small tolerance catches taps just off the ink when nothing is hit exactly; with nothing beneath, the box keeps the piece selectable. Resolution is three passes (exact → near → box), each topmost-first by `(zIndex, listIndex)`.
3. **Containment math.** Even-odd ray casting in unit space across **all** subpaths: cast a ray in +u; for each edge `(a, b)` count a crossing when `(a.v > v) != (b.v > v)` and the intersection's `u` exceeds the point's `u`; odd ⇒ inside. The half-open comparison makes a point on a vertex count once and keeps the result deterministic. Cubics are flattened by uniform subdivision into **16** segments; a unit test asserts that for every catalogue cubic the maximum distance from the curve to its chords is below **0.002** of the unit square (≈ 0.2 pt on a 100 pt piece, far below a fingertip); if a catalogue cubic fails the bound, raise the segment count, never the bound (🟦 values, unmeasured). Differences from Android's own flattening are confined to that band at the edge, which the Robolectric parity test excludes. A zero width or height maps to "not inside".
4. **Tolerance.** One `tolerancePt` parameter (default 0) on the three tap intents and on `HitTest.topmostAt`, computed by the caller from a dp radius. 🟦 **8 dp**, for the reasons in *Interaction details* §3.
5. **Module seam.** 🟦 **`core:editor` gains `implementation(project(":core:render"))`.** This amends the "Depends ONLY on `:core:model`" header in `core/editor/build.gradle.kts` to "depends on `:core:model` and `:core:render`; `:core:render` must never depend on `:core:editor`". The containment function and the in-scope set live in `core:render`, beside the outlines.
6. **Semantics.** `ElementSemanticsLayer` keeps its box nodes. No change to the semantics tree, the command set or the schema.

**Why the dependency, not an injected lookup (🟦).**
- **Nothing in the documented rules forbids it.** ARCHITECTURE's layer rules restrict `core:imposition` and `core:render` to `core:model` (`ARCHITECTURE.md:60`) and say nothing about `core:editor`'s dependencies. ADR-029's module split names `:core:editor` as pure with `hitTest` in it, and puts it in the core CI lane "like `:core:render`" (`DECISIONS.md:540`); it sets no dependency ban. The only "ONLY `:core:model`" statement is the build-script header. No cycle: `core:render` depends on `core:model` in production and on `core:copy` for tests only (`core/render/build.gradle.kts:19,25`).
- **It enforces "drawn hole = hit hole" by construction.** The hit test reads the same `SupplyCatalog` the replayer draws. An injected `(supplyId) -> SupplyOutline?` would let a test or a second caller pass a lookup that disagrees with the catalogue — the exact divergence this ADR exists to prevent.
- **It is the smaller change.** `EditorReducer` is an `object` called as `reduce(model, intent)`; a lookup would have to be threaded through `reduce`, the store and every reducer test, or smuggled into `EditorModel`, which is data.
- **Cost accepted:** `core:editor` now compiles against the whole render core. Mitigation: `implementation`, not `api`, and a review rule that `core:editor` uses only `SupplyCatalog.outlineOf`, the in-scope set and the containment function.

**Consequences.**
- ARCHITECTURE: add `core:editor → core:render` to the module graph and state the direction rule under *Layer rules*; update the build-script header (`core/editor/build.gradle.kts:10`, "Depends ONLY on :core:model") in the same change. The same claim is repeated in a test comment, `FramingMathTest.kt:57` ("`:core:editor` depends only on `:core:model`, so we assert that invariant directly here"); update its wording in the same change. The test's direct assertion stays: it guards the clamp itself, whatever the module graph.
- The six pieces pass taps through their holes over photos and text; over blank paper they behave as today.
- A thin band over a photo is not easier to grab (the photo wins exactly); Pass 2 tells us whether that matters (*Interaction details* §3, ⚠ Open).
- iOS: pure Kotlin, no `java.*`, no `Math.`; carries over unchanged.
- Older builds keep the box test; nothing is persisted, so no compatibility question.
- No golden changes are expected: nothing draws differently. If any golden moves, that is a defect, not a re-record.

**Alternatives considered.**
- *Inject an outline lookup into the reducer* — rejected above.
- *Move outlines or containment into `core:model`* — rejected: outlines are render content, and widening `core:model`'s surface is what ADR-029 declined for `inverse()` (ADR-014).
- *All 32 supplies by outline* — rejected by the owner (Q3): thin pieces become harder to grab over photos, and the empty parts of non-holed pieces are not holes a maker cuts.
- *Android `Path.contains`/`Region`* — rejected: binds selection to one platform's rasteriser; used only as the Robolectric parity oracle.
- *A large (≈ 24 dp) tolerance competing with exact hits* — rejected: at fit zoom it exceeds the eyelet's and the registration cross's default hole radii, so their holes would never pass a tap through.
- *A "select layer under the tap" menu* — deferred; the escape hatch if Pass 2 finds unreachable pieces.

**Tests.**
- **JVM `core:render`:** for each of the six outlines, the hole's centre is outside and a point in each band is inside; half-open edge cases (vertex, horizontal edge) deterministic; the 16-segment flattening bound for every catalogue cubic; a jqwik property that for random rotation, both flips and non-uniform scale, the page-space mapping plus unit containment agrees with the forward fold.
- **JVM `core:editor` (`HitTestTest`):** window over photo, tap in the hole → photo; tap on the band → window; window over blank paper, tap in the hole → window (box fallback); eyelet and registration cross at default size over a photo, tap at the hole's exact centre with the tolerance on → photo; a mirrored and a rotated window over a photo → photo, with the hole off-centre so a missing flip would fail; `mark.crop` over a photo, tap in its centre → `mark.crop` (box kept); unknown `supplyId` → box; text and image unchanged; tap just outside the ink over blank paper within the tolerance → piece, beyond it → nothing.
- **JVM reducer:** `DoubleTapAt` through a window's hole onto a photo opens Reframe; `BeginEditTextAt` through a hole onto text opens the text session; `DoubleTapAt` on the band stays a no-op.
- **JVM `feature:editor`:** `benchTapIntent` through a hole onto an already-selected text re-enters editing.
- **Robolectric:** pure containment matches Android `Path` even-odd containment on a sample grid, away from edges, for all six.
- **Goldens:** none expected to change. Run `bash tools/grun.sh gold` and confirm.
- **Device, both passes (CLAUDE.md):** Pass 1 — tap through each of the six onto a photo; double-tap → Reframe; band grabbable; `uiautomator dump` shows the semantics tree unchanged; record band size and zoom. Pass 2 — does a first-time maker expect the tap to pass through, and can they get the frame back? Tap delivery is device-only (`EditorGestures.kt:42-44`).

**Review.** Independent Review Agent before merge; outcome recorded here.

## v21-bench.html amendment specification

The owner approves this before any step-4 code. It is a specification; `docs/design/mockups/v21-bench.html` is not edited by this brief.

1. **Append a new amendment-log entry** at the end of the file (after the A23 block, `:2235-2255`), numbered next free at amendment time (A26 unless PR #70 takes it). The log is "appended, never inserted" (`:1179-1193`), so no line above moves. Content:
   > *Ax · (date) (owner ruling, decision gate Q3) — taps pass through empty outline areas.* On six pieces — `paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `fix.corner`, `mark.registration` — a tap on the drawn ink selects the piece, and a tap in the empty part of its outline (the hole, or the box around the ink) reaches whatever lies beneath: a photo, words or another piece. Double-tapping a photo through a hole opens Reframe. A small tolerance around the ink catches a fingertip that lands just off it when nothing else is under the finger. If nothing is beneath, the piece is selected as before. Every other piece, crop marks included, keeps its whole box. Screen-reader selection is unchanged. No pixel of this file changes; this is behaviour only.
2. **Correct the stale hole lists in place, line-for-line** (the log's rule: "the rules they amend change in place, line-for-line", `:1189-1190`):
   - `:479-481` (the A7 comment on `.tile svg`) lists `paper.window`, `fix.corner`, `mark.registration` as the only holed marks. Rewrap to list all six **within the same three lines**.
   - `:1431-1432` (A7's ruling text) says those three "are the three marks that have one". This was true for A7's sixteen; it is not for the shipped catalogue. Correct it in place to the six, within the same two lines, and let the new entry say A7's count was updated. (If the owner prefers the log's history untouched, leave `:1431-1432` and let the new entry state that A7's list is superseded; either way the present-tense claim must not stand uncorrected.)
3. **Verify** after editing: `wc -l` unchanged except the appended block, and `BenchArtSheetParityTest` still green (the tiles do not change).

## Current architecture touchpoints

Re-verified on `0aa7a7d`.

| Concern | Where |
|---|---|
| Outlines, catalogue, lookup | `core/render/.../SupplyCatalog.kt:508-544` (`OUTLINES`), `:547` (`outlineOf`); holed pieces in the Problem table |
| Outline types | `core/render/.../SupplyOutline.kt:46-78` (subpaths of `LineTo`/`CubicTo`, even-odd by invariant, `:11-30`) |
| Draw command | `DrawCommand.kt:86` (`DrawShape`) |
| Render fold (inverse needed for hit test) | `SceneRenderer.kt:97` (unknown id → nothing), `:97-107` (decor → `DrawShape` with `mirrored`/`flippedVertically`), `:138-146` (`unitSquareFold`: scale · mirror X · mirror Y) |
| Hit test | `core/editor/.../HitTest.kt:21-36`; callers `EditorReducer.kt:56,120,137`, `EditorGestures.kt:49` |
| Accessibility nodes | `ElementSemanticsLayer.kt:88` (box via `SelectionChromeGeometry.outlineDevicePx`) |
| Resize / scale | `TransformMath.kt:30-47` (pinch, uniform), `:77-78` (handles, free-aspect); `AffineTransform2D.kt:60-68` |
| Placement | `feature/editor/.../SupplyPlacement.kt:117-123` (defaults), `:133-139` (the `{shape.rule}`-only override); pinned by `SupplyPlacementTest.kt:93` |
| Names, families, tags | `core/copy/.../Copy.kt:497` (`Supplies`), `:512` (`BY_FAMILY`), `:560` (`TAGS`) |
| Art sheet | `BenchArtSheet.kt:594-595` (`requireNotNull` for outline and name) |
| Unknown supply | `DefaultDocumentValidator.kt:109-112` (membership not checked), `EditorA11y.kt:63` (fallback label) |
| Module deps | `core/editor/build.gradle.kts:10` (header), `:18`; `core/render/build.gradle.kts:19,25`; `ARCHITECTURE.md:60` (layer rules) |
| Frozen spec | `v21-bench.html:479-481,867,874,1179-1193,1431-1432,1736-1754,2235-2255` |

## Files/modules likely affected

- **Hit-test session:** `core:render` (containment, in-scope set, tests); `core:editor` (`HitTest`, `Intent` tolerance, build script, tests); `feature:editor` (tolerance at the call sites, `benchTapIntent`); docs: the ADR, ARCHITECTURE (module graph and layer rule), the `v21-bench.html` amendment, CHANGELOG.
- **Resize lock:** `TransformMath`/`ResizeHandle` plus the per-supply set; D-100 ruling; `v21-bench.html` behaviour note.
- **D5:** `core:render` (two outlines with attestation, in-scope set if holed); `core:copy` (names, `BY_FAMILY`, `TAGS`); `v21-bench.html` (the set amendment); `feature:editor` goldens (Art sheet counts change); docs: SUPPLIES-SPEC catalogue, ADR-107 progress note, CHANGELOG, ROADMAP; strike the OWNER-CHECKLIST O9 row and the stale Mirror row.

## Data model implications

**No schema bump.** A new `supplyId` is data, and hit testing persists nothing. Do not use D5 to introduce v4: one v4 zine makes the **whole** library backup unrestorable on an older build (`ZineLibraryBackupStager.kt:367-376`).

✅ **Older builds show an invisible, selectable box, not "nothing".** A newer zine reaches an older build only through a library-backup restore or a downgrade. There, an unknown `supplyId`:
- passes validation (`DefaultDocumentValidator.kt:109-112`);
- draws nothing (`SceneRenderer.kt:97`) and exports nothing;
- is kept on save;
- is still hit-testable by its box, so it catches taps meant for the photo under it in that build;
- is spoken with the fallback label (`EditorA11y.kt:63`).

This needs a release note ("zines that use the new frames need this version or later to show and print them"). 🟦 Consider a visible placeholder for unknown supplies later (see *Future extension*).

**Landing size and the placement pin.** See *Frames (D5)*: a per-supply landing aspect is an owner ruling on §5.2. ADR-107 also lists "no collision with §5.2's override map" and "no new entry on §3.4.1's uniform-scale list" as consequences of R3's withdrawal; the per-piece resize list is the owner's delegated replacement for the second, recorded in D-100.

🟦 **Recommendation (research, not decided):**
- keep frames as overlay decor;
- **no page-level frame**;
- **no frame property on `ImageElement`**. It would be a schema change; older builds would silently drop it on re-save (`ignoreUnknownKeys`, `JsonDocumentSerializer.kt:40`); it would conflict with photo flip and Reframe's clip; and it would go against ADR-107 R2 (the maker arranges, the app never proposes arrangements).

## Testing strategy

- **Hit test:** as listed in the [ADR draft](#adr-draft--outline-hit-testing-for-holed-supplies).
- **Resize lock:** per locked supply, a handle drag keeps aspect; per free supply, it does not; pinch unchanged for all.
- **Frames — JVM (`core:render`):** outline authoring tests for each new supply: closed, inside the unit square, subpath count, and a raster check that the centre is empty and the band is filled; the existing catalogue-closure tests; if holed, the containment tests above.
- **Roborazzi** — each new frame in two inks, flipped and unflipped; the Art sheet at its new counts. Run `bash tools/grun.sh gold` and read `*_compare.png` before any re-record.
- **Compatibility** — a document with an unknown `supplyId` loads, renders nothing, re-saves the id unchanged, and is still selectable.
- **`SuppliesCopyTest`** — no collisions; `BenchArtSheetParityTest` stays green against the amended HTML.
- **Device, both passes (CLAUDE.md)** for each session: the hit-test passes in the ADR draft; for frames, the new frames in PDF, the resize behaviour on a locked and a free piece, and Pass 2's "does this read as a piece of paper I laid down?".

## Accessibility considerations

- Every frame has a spoken name and the decor actions. TalkBack selection goes by node, so outline hit testing leaves the semantics tree unchanged. Assert this on the device (`uiautomator dump`), not in Compose tests.
- The band's touch size is below 48dp, and the tolerance does not help over a photo. Record the band size and the result in Pass 1.
- A frame placed over a photo is read in list order until D4-A1's spatial order lands.

## Design-system implications

None. Supplies are content, tinted with the existing maker inks. New outlines must look authored, not procedurally generated (SUPPLIES-SPEC §5, ADR-107 R2).

## iOS portability notes

- The containment function and the three-pass `HitTest` are pure Kotlin in `core:render`/`core:editor`, so they carry over as they are. Keep them free of `java.*` and `Math.`.
- Do not use Android `Path.contains`/`Region` for the hit test. That would bind selection to one platform's rasteriser. Use it only as the Robolectric parity oracle.
- Outlines stay code in `SupplyCatalog`, not assets.

## Acceptance criteria

1. The hit-test session has merged first, with its ADR recorded and its `v21-bench.html` amendment approved. A tap in the hole of each of the six holed pieces over a photo selects the photo; a double-tap there opens Reframe. With nothing underneath, the piece is still selected. `mark.crop` keeps its box.
2. The drawn hole matches the hit hole under rotation, both flips and non-uniform scale (property test plus Robolectric parity). No golden changed.
3. Text, image, out-of-scope decor and unknown-supply hit behaviour is unchanged (tests). The semantics tree is unchanged on device.
4. The owner has visually approved Frame A and Frame B. They are authored, attested and pass the outline tests. Membership, names, order and tags match the amended `v21-bench.html`, and the parity test is green.
5. Resize follows the per-piece list as recorded in D-100, with a test for each rule it sets.
6. Each frame renders identically on Bench, page nav, Read and PDF, and flip and ink work (goldens).
7. The unknown-`supplyId` compatibility test is green, and the release notes state the older-build limitation.
8. The stale Mirror records are corrected. The OWNER-CHECKLIST O9 row is struck with a link to the ruling.
9. Both device passes are accepted for each session, and independent review returns GO.

## Stop conditions

- *Hit test:* the owner has not approved the `v21-bench.html` amendment, or PR #70 is still open.
- The hit-test change would need a change to the semantics tree, the reducer's command set or a schema field.
- The touch tolerance needs more than one parameter on the tap intents and `HitTest`.
- The module seam needs more of `core:render` than the outline lookup, the in-scope set and the containment function.
- *Frames:* the owner has not visually approved two frames, or step 4 has not merged.
- A chosen frame can't be drawn as one outline without overlapping subpaths. That means a composite, which R3 withdrew.
- A frame needs border-preserving (nine-slice-like) scaling. The owner ruled it out.
- A frame needs a per-supply landing size and no §5.2 ruling covers it.

## Out of scope

- Crop marks (and any other non-holed piece) in the hit-test scope.
- Frames attached to photos, and any frame property on `ImageElement`.
- Page-level frames.
- Composite or slot frames (ADR-107 R3).
- Nine-slice borders.
- Opacity.
- User-saved combinations (Waits).
- Downloadable packs.
- Photo shape cutouts (masks; their own direction).
- Outline-aware snapping.
- A "select layer under the tap" menu.
- Everything in [audit §15](ZINELY-1X-READINESS-AUDIT.md#15-what-not-to-change-yet).

## Future extension

- **`mark.crop` with a geometric "inside the corner marks" region**, so a tap inside crop marks passes through. Not approved; a proposal would need its own Q3-style ruling and HTML note.
- **A visible placeholder for unknown supplies** (a neutral hatched box plus a label). It would stop future packs from appearing as invisible boxes in builds that already have it. It can't help beta.5. It is a visible change, so it needs an HTML amendment. Worth doing before a second pack.
- **Photo-attached frames and preset cutouts**, after transparency (ROADMAP order). Both need a schema change and a spec.
- **Personal packs** from the maker's own marks via ink lift (O3, experimental).

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Final planning audit fixes: the print study's output is guidance, not a gate on step 4 or 9; gate 4 re-sourced (visuals by Q6; set membership, drawn names and order by ADR-107 R1a, `DECISIONS.md:11485-11486`; R1a gives outline authoring to the implementer; search tags are not in R1a and are reviewed in the set amendment); landing-size ownership aligned with gate Q6 (family default delegated; per-supply override the owner's); ADR draft now also names the `FramingMathTest.kt:57` comment beside the build-script header. |
| 2026-09-26 | Owner rulings Q3/Q5/Q6 applied. Hit test: scope fixed to the six holed pieces (crop marks excluded; a geometric crop region noted as unapproved future work); exact → near → box resolution; tolerance re-derived — under exact-first it cannot help a band over a photo, so 🟦 8 dp replaces ≈ 24 dp; TalkBack stays on boxes (`ElementSemanticsLayer.kt:88`). Added the ADR draft (module seam 🟦 `core:editor` → `core:render`, containment math, tests, no golden changes) and the `v21-bench.html` amendment specification (append-only log entry; the stale three-piece lists at `:479-481`/`:1431-1432` become six). Frames: two, hand-cut, no nine-slice, "Frame A / Frame B" pending the owner's visual approval; per-piece resize list as a 🟦 recommendation to be finalised in D-100. Gates: O14 and the fold study removed (evidence for Proof only); PR #70 must be settled before step 4. Base `0aa7a7d`; render-fold citation corrected to `:138-146`. |
| 2026-09-25 | After both reviews: readiness split by part (the hit test is not blocked by O9/O14); the hit test is an interaction change whose scope the owner rules in Q3 (holed pieces only vs all 32 supplies), with the HTML note first; exact hit beats a tolerance near-hit so small holes pass taps through; "six more" non-holed pieces (was "seven"); instant-photo example removed (§IV); ROADMAP anchor and build-script line fixed. |
| 2026-09-25 | Folded the readiness-audit corrections: six holed supplies ship (the audit said five and missed `fix.corner`); the box hit-test defect and a hole-aware two-pass fix as a prerequisite session; the `v21-bench.html` set amendment is required (ADR-107 R1a; P5); free-aspect resize and the unbuilt §3.4.1 lock (D-100) made the stretch policy an owner gate; per-supply defaults break the `{shape.rule}` pin; older builds show an invisible, selectable box; the O14 fold-study gate; recommendation against page frames and an `ImageElement` frame property; base moved to `5f7707a`. Added the product audit, research and readiness line. |
