# Brief 05 — Materials: decorative frames and the Art backlog

> This brief incorporates the findings of [ZINELY-1X-READINESS-AUDIT.md](ZINELY-1X-READINESS-AUDIT.md). If this brief conflicts with an older research document, this brief and the cited authoritative ADR/decision take precedence. It never overrides an Accepted ADR, the V2 constitution or a frozen spec — where it needs one changed, it says so and names the amendment.

Status: **implementation brief, not authorised.** Direction D5 of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md).
Base: `origin/main` @ `5f7707a` (beta.5 released). Revised 2026-09-25 from the readiness audit.
**Readiness, by part:**
- **Tap-through hit test (plan step 4):** READY AFTER the owner answers [Q3](ZINELY-1X-DECISION-GATE.md#q3-tap-through-hit-testing-how-far-it-reaches) (holed pieces only, all Art, or no change) and approves its `v21-bench.html` behaviour note. Gates 1–3 do **not** apply to it.
- **D5 frames (plan step 9):** BLOCKED BY O9 + the stretch ruling ([Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch)), O14 or the fold study ([Q5](ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14)), the merged hit-test session, and the `v21-bench.html` set amendment.

## Gates before an implementation session may start

Gates 1–3 and 5 apply to **D5 frames** only; gate 4 is the hit-test session's own gate.

1. **Owner — O9, which frames and backlog supplies ship, and who authors the outlines** ([Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch)). [ADR-107](../DECISIONS.md#adr-107) R1 leaves ~19 researched supplies in the backlog, and R1a makes set membership the owner's call.
2. **Owner — the stretch policy** ([Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch); this is the open [D-100](../design/V2-SPEC-DEFECTS.md#d-100)). The options are free stretch (today), a uniform-scale lock per supply, or a lock for the ring-like pieces only. The ruling also covers the six holed pieces that already ship. If it adds per-supply landing sizes, it must also rule on SUPPLIES-SPEC §5.2 (see *Data model*).
3. **Owner — O14, or run the fold study** ([Q5](ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14)). The ROADMAP says the creative slice (fonts, the Art pack, decorative frames) "does not reorder the retained release, fold-study or acceptance gates without owner approval" (`ROADMAP.md:92-95`).
4. **Owner, then work — the tap-through hit-test session** ([plan §5](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing) step 4). The box hit test is inherited behaviour, so changing it is an **interaction change**: the owner chooses its scope in [Q3](ZINELY-1X-DECISION-GATE.md#q3-tap-through-hit-testing-how-far-it-reaches) and approves a `v21-bench.html` behaviour note before any code. Then it is small, with its own branch and review, and can run any time after step 0. D5 may not start until it has merged. Its design is in *Interaction details* below.
5. **Work — the frozen-HTML amendment**: a reviewed change to `docs/design/mockups/v21-bench.html` that adds the chosen supplies to the Art sheet (its set, names, order and search tags). It comes before any Compose work. See *UI/UX proposal*.
6. **Owner, optional — ratify "imperfect surface, perfect mechanics"** (research proposal D13; [decision gate, "Decisions that can wait"](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)). This does not block D5. This brief uses the phrase as an evaluation lens only.

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

Six more supplies have empty space inside their box without having a hole. They are made of separate parts: halftone, saddle stitch, crop marks, colour bar, copier streak and perforation (`:229-239`, `:408-410`, `:427-453`). `mark.crop` (four corner Ls around an empty centre) already works as a frame in practice.

✅ **These pieces already ship with a defect.** The hit test is a box test (`HitTest.kt:21-36`): it rotates the tap into the element's frame, checks it against `±w/2, ±h/2`, and picks the topmost element by `(zIndex, listIndex)`. So:
- a tap in a frame's empty centre selects the frame, never the photo beneath it;
- double-tap to Reframe that photo is blocked the same way (`EditorReducer.kt:137-145` uses the same `topmostAt`);
- the only workarounds are Send backward and TalkBack.

Adding more frames before this is fixed would multiply the defect.

✅ **Resize is free-aspect.** Handle resize sets width and height independently (`TransformMath.kt:77-78`). Pinch is uniform (`:30-47`). The SUPPLIES-SPEC §3.4.1 uniform-scale lock was never built (`AffineTransform2D.kt:60-65`, D-100). So a window frame on a 4:3 box gets uneven borders (`WINDOW_BORDER` is 0.14 of each side, `:242`), and a ring becomes an ellipse.

**Carried over and still correct:**
- *Mirror* is not missing. ADR-113 Flip toggles `DecorElement.mirrored` (`EditorReducer.kt:212`, `FlipTray.kt:333`, `EditorA11y.kt:158-161`). The KDoc at `Intent.kt:53` is stale, and so is the OWNER-CHECKLIST Mirror row (already flagged "likely stale", `OWNER-CHECKLIST.md:205`).
- *Composite frames* (corner + edge slots) were withdrawn by ADR-107 R3, because even-odd turns any overlap into a hole.

## Product audit

### User value
🟨 Putting a photo in a window, mat or torn hole is a common collage move (an assumption, not measured). Six pieces already offer it, but the photo underneath can't be tapped. So the first user value is the hit-test fix, not new frames: it makes the pieces that already ship work. New frames add variety on top of that (a torn-paper mat, a perforated stamp frame, a ticket window) and nothing else. Instant-photo borders are out: they imitate an object rather than cut one, which fails ADR-107 R1's test, and V2-CONSTITUTION §IV names tilted "polaroid" frames as costume.

### Creative value
**Frames that belong in Zinely** reproduce what a maker does with scissors, a torn page or a photocopier: a window cut into paper, a torn hole, a perforated stamp edge, a copier-grey mat. They pass ADR-107 R1's test, "Does it attach, point, tear, or cut?" ("cut"), and its second filter: can the maker already make this with a verb they have?

**Canva-frame drift starts** when the app does the composing:
- photo-in-frame masks that snap and clip the photo (Canva's model, below);
- frames that fit themselves to a photo;
- themed or seasonal borders, or flourishes with no physical source;
- frames with words in them ("Happy birthday");
- frame+photo groups.

Each of these is an app-proposed arrangement, which ADR-107 R2 refuses. **The line: a frame is a piece of paper the maker lays down; it never holds, fits or clips a photo.**

### UX cost
- No new surface: frames are tiles in the existing families.
- The hit-test fix changes what a tap does over the decor in Q3's scope. A tap on the empty part of an outline reaches whatever is underneath, and falls back to the decor only if nothing is. Over blank paper nothing changes.
  - **(a) holed pieces only:** six pieces change; everything else keeps its box.
  - **(b) all Art:** all 32 supplies change — circle corners, a triangle's empty half, the gaps in halftone, crop marks' empty centre, the staple, the paper clip. Over a photo, thin pieces get harder to grab: a rule's box is 5× taller than its bar.
- The stretch rule changes how handles behave for some pieces (Gate 2).

### Cognitive load
The Art sheet grows by the owner's pick, which could be 3–4 frames ([Q6](ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch)'s framing) or more backlog pieces. Families and search already carry that load (ADR-107 R5). Scroll depth is the owner's to accept under R1a.

### Accessibility
- Each frame needs a spoken name. Decor already gets Flip, Ink and Replace custom actions (`EditorA11y.kt`).
- TalkBack is the one path the hit-test defect never touched: accessibility focus reaches elements by node, not by tap. The fix must not change the semantics tree.
- 🟨 **Estimate: a frame's band may be below 48dp on a phone.** The *Cut paper* default is 45 % of the page width (`SupplyPlacement.kt:121`). On an A4 eighth-page (~74 mm wide) that makes a ~33 mm frame with a ~4.7 mm band, so a few mm on screen. That is below the [48dp target guidance](https://developer.android.com/guide/topics/ui/accessibility/apps). This is why *Interaction details* asks for a touch tolerance.
- How decor is spoken is D4's sub-decision ([audit §11](ZINELY-1X-READINESS-AUDIT.md#11-owner-decisions)). D5 adds no new rule for it.

### Offline-first and ownership
Outlines are code in `SupplyCatalog`, bundled, authored from scratch, and attested in the file's header (`SupplyCatalog.kt:41-48`). There is no download, no pack store and no licence surface. Each new outline needs the same attestation.

### Physical publishing (PDF and paper)
- Outlines are vector, even-odd, and fill-only through the one replayer (`DrawCommand.kt:86` `DrawShape`), so the frame the maker sees is the frame that prints.
- Solid bands photocopy well.
- The risk is a frame used as a **page border**. [ADR-039](../DECISIONS.md#adr-039) tiles edge to edge, and home printers can't reach the paper edge. A border has to sit inside the Bench keep-clear inset (17 pt) or the printer clips it unevenly. Treat it as ordinary decor the maker sizes inside keep-clear. There is no page-level frame feature.

### Product identity
The lens is **"imperfect surface, perfect mechanics"**. The owner uses it to evaluate directions. It appears as research proposal D13 and as a website story beat in [ADR-118](../DECISIONS.md#adr-118), but it is **not ratified as a product principle**.

Frames are where the two halves meet:
- **Surface, allowed to be rough:** a torn or deckled outline, an uneven hand-cut band, a slightly off-square window. This is where authored imperfection belongs (ADR-107: "feel authored").
- **Mechanics, which must be exact:**
  - a tap in the hole must reach the photo, every time, on every holed piece;
  - the drawn hole and the hit hole must be the same shape, in the same place after rotate, flip and resize. They come from the same outline, which is why the test belongs in `core:render`;
  - dragging a frame over a photo snaps centre to centre (`Snap.kt:17-24` snaps box edges and centres), so centring is exact without outline-aware snapping;
  - resize must do what the stretch ruling says, and nothing else.

**What to cut to avoid becoming a generic editor:** masks, attached frames, auto-fit, frame+photo grouping, themed borders and text-bearing frames. Any of these turns a material into a template.

## What others teach us

- **Canva's frames are masks.** The maker drags a photo onto a frame, it "snaps into place" and is cropped to the frame's shape. Double-click edits the crop ([Canva Help: Use frames](https://www.canva.com/help/using-frames/)). → That is exactly the attached, app-composed model. It confirms the choice to keep Zinely's frames as overlays the maker positions, and to keep photo cut-outs a separate direction (ROADMAP: "medium to large").
- **Vector hit-testing uses the painted interior, not the box.** In SVG, the default `visiblePainted` targets an element "when the pointer is over a 'painted' area", meaning its fill or stroke ([SVG 2, Interactivity](https://svgwg.org/svg2-draft/interact.html)). The fill-rule decides "what parts of the canvas are included inside the shape" ([SVG 2, Painting](https://svgwg.org/svg2-draft/painting.html)), so an even-odd hole does not catch the pointer. → This is the precedent for testing the outline, even-odd, in unit space.
- **Figma gives an escape hatch for stacked layers.** A "Select layer" context menu lists the layers under the cursor, and a modifier "deep select" picks nested ones ([Figma Learn: Select layers and objects](https://help.figma.com/hc/en-us/articles/360040449873-Select-layers-and-objects)). → Zinely does not need a menu while a tap in the hole passes through. If Pass 2 finds pieces still unreachable, this is the pattern to consider, not a hidden gesture.
- **Keeping borders fixed while stretching needs its own mechanism.** Android's NinePatch marks which parts stretch and which stay fixed ([Android: NinePatch drawables](https://developer.android.com/develop/ui/views/graphics/drawables#ninepatch-drawables)). → Even borders on a stretched window frame would need a new outline concept, not a flag. That is why the stretch ruling is a real choice (Gate 2), not a detail.

## User story

*As a maker, I want to lay a hand-cut frame over my photo, tint it with my ink and move it like any other piece of Art, and still be able to tap through the hole to reach my photo.*

## Experience

Bench → Add → Art → *Cut paper* (and, if the owner's set says so, *Cut shapes* or *Stamps & marks*) shows the chosen frames. Tap one and it lands at page centre, selected (ADR-105), tintable, flippable, deletable and replaceable, like every other supply. The maker drags it over the photo (centre snap helps), resizes it by the stretch rule, and taps inside the hole to select or double-tap the photo.

## UI/UX proposal

- **Amend `v21-bench.html` first** (ADR-107 R1a). The amendment:
  - adds each chosen supply to the staged list `ART_FIRST_WAVE` (`:1736-1753`), in the owner's order, under its family (`FAMILIES` `:867`, `openArt` `:874`);
  - adds each supply's search tags to `ART_TAGS` (`:1754`);
  - updates the hole note at `:480` and `:1432`, which still lists only `paper.window`, `fix.corner` and `mark.registration`;
  - records the change in the amendment log.

  Tile path data is **generated from the catalogue**, not drawn by hand (`BenchArtSheetParityTest`, noted at `:874-879`). The outlines therefore exist before the HTML is final, and the amendment and the outline commit land together.
- **Hit-test session:** no visual change, but an interaction change. A one-paragraph "taps pass through empty outline areas" behaviour note, scoped as Q3 rules, goes into the `v21-bench.html` amendment log **before** the code, and the owner approves it (CLAUDE.md: interaction changes after freeze update the HTML first).
- Names follow the `Copy.Supplies` rules: one spoken word or a short phrase, no slash pairs, no prefix collisions, no trademarks (`SuppliesCopyTest`). Search tags include "frame", "border" or "mat" where they fit.

## Interaction details

**Hit test (its own session, before D5):**

1. A pure function in `core:render`: `SupplyOutline` even-odd containment of a unit-space point.
   - Flatten each cubic into a fixed number of line segments, then count ray crossings across **all** subpaths; odd means inside.
   - It is deterministic and uses `kotlin.math` only (coupling rule 1, [audit §10](ZINELY-1X-READINESS-AUDIT.md#10-architecture--ios-implications)).
2. `HitTest` becomes two-pass:
   - **Pass 1:** pick the topmost element whose *hit area* contains the point.
     - Text and image elements use the **box**, unchanged, so a tap on empty text or a `Fit.FIT` letterbox still selects them.
     - Decor **in Q3's scope** uses the **outline** (the six holed pieces under (a); every supply under (b)); other decor keeps the box. Map the tap into unit space by undoing, in this order: translation to the box centre, rotation (as `contains` does today), scale (`u = (lx + w/2)/w`, `v = (ly + h/2)/h`), then `mirrored` (`u → 1−u`) and `flippedVertically` (`v → 1−v`). This is the exact inverse of the render fold (`SceneRenderer.kt:144-146`).
     - Decor whose `supplyId` has no outline keeps the **box**, so an unknown piece stays selectable and deletable.
   - **Pass 2:** if nothing matched, fall back to today's box test. A tap in a hole over blank paper still selects the frame.
3. **Touch tolerance** (🟦; settle it in the hit-test session): treat a decor outline as *near-hit* when any of ~8 samples on a small circle around the point is inside. The caller supplies the radius in page points (`≈ 24dp / zoom`), as `Snap` already takes `thresholdPt`. That needs a parameter on `SelectAt`/`DoubleTapAt` or on `HitTest`. If it grows past that, stop and ask.
   - **An exact hit beats a near-hit.** Pass 1 first looks for the topmost element that contains the point itself; a near-hit on decor wins only when no element contains the point. Without this rule, small holes swallow the tap: at default size the eyelet's hole radius is ≈12 pt and the registration cross's ≈6.5 pt, both smaller than a ≈14–15 pt tolerance at fit zoom (🟨 estimate), so a tap at the hole's centre would hit the band and never reach the photo.
4. **Module seam.** `core:editor` cannot see `SupplyCatalog`. Either:
   - add `implementation(project(":core:render"))` — no cycle, because `core:render` depends only on `core:model` — and update the "Depends ONLY on `:core:model`" header in `core/editor/build.gradle.kts:10` and the ARCHITECTURE module graph; or
   - inject a `(supplyId) -> SupplyOutline?` lookup.

   🟦 The dependency is the smaller change: `EditorReducer` is an `object`, so a lookup would have to be threaded through `reduce`.
5. **What it affects:** `SelectAt` (`EditorReducer.kt:56`), `BeginEditTextAt` (`:120`), `DoubleTapAt` (`:137`) and `benchTapIntent` (`EditorGestures.kt:49`). A drag moves the selection as it stood at touch-down (`EditorGestures.kt:146`), so dragging is unaffected.

**Frames (D5):**
- A frame is **one outline, one ink**: outer contour plus inner-hole subpath, even-odd, with no overlapping subpaths (rule 4 is a review obligation that no assertion can check; `SupplyCatalog.kt:110-115`).
- Frames are **overlays**. Moving the photo does not move the frame.
- Default size: the *Cut paper* constant (45 % width, square) unless the stretch ruling says otherwise.
- Art never gets opacity (SUPPLIES-SPEC).

## Current architecture touchpoints

Re-verified on `5f7707a`.

| Concern | Where |
|---|---|
| Outlines, catalogue, lookup | `core/render/.../SupplyCatalog.kt:508-544` (`OUTLINES`), `:547` (`outlineOf`); holed pieces in the Problem table |
| Outline types | `core/render/.../SupplyOutline.kt:46-78` |
| Render fold (inverse needed for hit test) | `SceneRenderer.kt:97` (unknown id → nothing), `:144-146` (scale · mirror in unit space) |
| Hit test | `core/editor/.../HitTest.kt:21-36`; callers `EditorReducer.kt:56,120,137`, `EditorGestures.kt:49` |
| Resize / scale | `TransformMath.kt:30-47` (pinch, uniform), `:77-78` (handles, free-aspect); `AffineTransform2D.kt:60-68` |
| Placement | `feature/editor/.../SupplyPlacement.kt:117-123` (defaults), `:133-139` (the `{shape.rule}`-only override); pinned by `SupplyPlacementTest.kt:93` |
| Names, families, tags | `core/copy/.../Copy.kt:497` (`Supplies`), `:512` (`BY_FAMILY`), `:560` (`TAGS`) |
| Art sheet | `BenchArtSheet.kt:594-595` (`requireNotNull` for outline and name) |
| Unknown supply | `DefaultDocumentValidator.kt:109-112` (membership not checked), `EditorA11y.kt:63` (fallback label) |
| Module deps | `core/editor/build.gradle.kts:9,18`; `core/render/build.gradle.kts:19` |
| Frozen spec | `v21-bench.html:480,867,874,1432,1736-1754` |

## Files/modules likely affected

- **Hit-test session:** `core:render` (containment function and tests); `core:editor` (`HitTest`, build script, maybe `Intent` for the tolerance); `feature:editor` (the tolerance at the call site, if adopted); docs: ARCHITECTURE (module graph), the `v21-bench.html` amendment-log note, CHANGELOG.
- **D5:** `core:render` (new outlines with attestation); `core:copy` (names, `BY_FAMILY`, `TAGS`); `v21-bench.html` (the amendment); `feature:editor` goldens (Art sheet counts change); docs: SUPPLIES-SPEC catalogue, ADR-107 progress note, CHANGELOG, ROADMAP; strike the OWNER-CHECKLIST O9 row and the stale Mirror row.
- **If the stretch ruling adds a lock:** `TransformMath`/`ResizeHandle` plus a per-supply list, which is D-100's fix. That is its own session.

## Data model implications

**No schema bump.** A new `supplyId` is data. Do not use D5 to introduce v4: one v4 zine makes the **whole** library backup unrestorable on an older build (`ZineLibraryBackupStager.kt:367-376`).

✅ **Older builds show an invisible, selectable box, not "nothing".** A newer zine reaches an older build only through a library-backup restore or a downgrade. There, an unknown `supplyId`:
- passes validation (`DefaultDocumentValidator.kt:109-112`);
- draws nothing (`SceneRenderer.kt:97`) and exports nothing;
- is kept on save;
- is still hit-testable by its box, so it catches taps meant for the photo under it in that build;
- is spoken with the fallback label (`EditorA11y.kt:63`).

This needs a release note ("zines that use the new frames need this version or later to show and print them"). 🟦 Consider a visible placeholder for unknown supplies later (see *Future extension*).

**Stretch policy and the placement pin.** A per-supply landing aspect (e.g. 4:3 for a photo frame) breaks the `{shape.rule}`-only override (`SupplyPlacement.kt:133-139`), its rule that "every additional entry is a per-family constant quietly giving up" (`:135`), and the test that pins it (`SupplyPlacementTest.kt:93`). ADR-107 also lists "no collision with §5.2's override map" and "no new entry on §3.4.1's uniform-scale list" as consequences of R3's withdrawal. Any such entry is an owner ruling on §5.2, not an implementer's edit.

🟦 **Recommendation (research, not decided):**
- keep frames as overlay decor;
- **no page-level frame**;
- **no frame property on `ImageElement`**. It would be a schema change; older builds would silently drop it on re-save (`ignoreUnknownKeys`, `JsonDocumentSerializer.kt:40`); it would conflict with photo flip and Reframe's clip; and it would go against ADR-107 R2 (the maker arranges, the app never proposes arrangements).

## Testing strategy

- **JVM (`core:render`)** — containment tests:
  - for every catalogue outline, the centre of each of the six holed pieces is outside, and a point in each band is inside;
  - one jqwik property: for random rotation, flip and non-uniform scale, `HitTest` on the mapped point agrees with unit-space containment;
  - outline authoring tests for each new supply: closed, inside the unit square, subpath count, and a raster check that the centre is empty and the band is filled;
  - the existing catalogue-closure tests.
- **JVM (`core:editor`)** — `HitTestTest`:
  - frame over photo, tap in the hole → photo;
  - tap on the band → frame;
  - frame over blank paper, tap in the hole → frame (fallback);
  - with the tolerance on, a tap at the exact centre of the eyelet and of the registration cross, each at its default size over a photo → photo (exact hit beats near-hit);
  - under Q3 (a), a non-holed piece (e.g. `mark.crop`) keeps its box;
  - unknown `supplyId` → box;
  - text and image unchanged.
- **JVM (reducer)** — `DoubleTapAt` in the hole opens Reframe on the photo.
- **Robolectric** — a parity check that the pure containment matches Android `Path` even-odd containment on a sample grid, away from edges. This guards "drawn hole = hit hole".
- **Roborazzi** — each new frame in two inks, flipped and unflipped; the Art sheet at its new counts. Run `bash tools/grun.sh gold` and read `*_compare.png` before any re-record.
- **Compatibility** — a document with an unknown `supplyId` loads, renders nothing, re-saves the id unchanged, and is still selectable.
- **`SuppliesCopyTest`** — no collisions; `BenchArtSheetParityTest` stays green against the amended HTML.
- **Device, both passes (CLAUDE.md):**
  - Pass 1: tap through each holed piece onto a photo; double-tap → Reframe; band grabbable; TalkBack unchanged; the new frames in PDF.
  - Pass 2: does a first-time maker expect the tap to pass through? Can they find the frame again?
  - Tap delivery is device-only (`EditorGestures.kt:42-44`).

## Accessibility considerations

- Every frame has a spoken name and the decor actions. TalkBack selection goes by node, so hole-aware hit-testing must leave the semantics tree unchanged. Assert this on the device, not in Compose tests.
- The band's touch size is below 48dp (the tolerance above). Record the band size and the result in Pass 1.
- A frame placed over a photo is read in list order until D4-A1's spatial order lands.

## Design-system implications

None. Supplies are content, tinted with the existing maker inks. New outlines must look authored, not procedurally generated (SUPPLIES-SPEC §5, ADR-107 R2).

## iOS portability notes

- The containment function and the two-pass `HitTest` are pure Kotlin in `core:render`/`core:editor`, so they carry over as they are. Keep them free of `java.*` and `Math.`.
- Do not use Android `Path.contains`/`Region` for the hit test. That would bind selection to one platform's rasteriser. Use it only as the Robolectric parity oracle.
- Outlines stay code in `SupplyCatalog`, not assets.

## Acceptance criteria

1. The hit-test session has merged first. A tap in the hole of each of the six shipping holed pieces over a photo selects the photo. A double-tap there opens Reframe. With nothing underneath, the piece is still selected.
2. The drawn hole matches the hit hole under rotation, both flips and non-uniform scale (property test plus Robolectric parity).
3. Text, image and unknown-supply hit behaviour is unchanged (tests).
4. The owner's O9 set is authored, attested and passes the outline tests. Its membership, names, order and tags match the amended `v21-bench.html`, and the parity test is green.
5. Resize follows the stretch ruling, as recorded (ADR or D-100 ruling), with a test for each rule it sets.
6. Each frame renders identically on Bench, page nav, Read and PDF, and flip and ink work (goldens).
7. The unknown-`supplyId` compatibility test is green, and the release notes state the older-build limitation.
8. The stale Mirror records are corrected. The OWNER-CHECKLIST O9 row is struck with a link to the ruling.
9. Both device passes are accepted, and independent review returns GO.

## Stop conditions

- *D5 frames only:* the owner has not recorded an O9 set or a stretch ruling, or O14 is unanswered and the fold study has not run. (These do not stop the hit-test session.)
- A chosen frame can't be drawn as one outline without overlapping subpaths. That means a composite, which R3 withdrew.
- The stretch ruling needs border-preserving (nine-slice-like) scaling. That is a new render concept for the one replayer and needs its own ADR.
- A frame needs a per-supply landing size and no §5.2 ruling covers it.
- The hit-test change would need a change to the semantics tree, the reducer's command set, or a schema field.
- Q3 is unanswered, or the owner has not approved the `v21-bench.html` behaviour note.
- The touch tolerance needs more than one parameter on the tap intents or on `HitTest`.

## Out of scope

- Frames attached to photos, and any frame property on `ImageElement`.
- Page-level frames.
- Composite or slot frames (ADR-107 R3).
- Opacity.
- User-saved combinations (Waits).
- Downloadable packs.
- Photo shape cutouts (masks; their own direction).
- Outline-aware snapping.
- A "select layer under the tap" menu.
- Everything in [audit §15](ZINELY-1X-READINESS-AUDIT.md#15-what-not-to-change-yet).

## Future extension

- **A visible placeholder for unknown supplies** (a neutral hatched box plus a label). It would stop future packs from appearing as invisible boxes in builds that already have it. It can't help beta.5. It is a visible change, so it needs an HTML amendment. Worth doing before a second pack.
- **Photo-attached frames and preset cutouts**, after transparency (ROADMAP order). Both need a schema change and a spec.
- **Personal packs** from the maker's own marks via ink lift (O3, experimental).

## Change log

| Date | Change |
|---|---|
| 2026-09-25 | After both reviews: readiness split by part (the hit test is not blocked by O9/O14); the hit test is an interaction change whose scope the owner rules in Q3 (holed pieces only vs all 32 supplies), with the HTML note first; exact hit beats a tolerance near-hit so small holes pass taps through; "six more" non-holed pieces (was "seven"); instant-photo example removed (§IV); ROADMAP anchor and build-script line fixed. |
| 2026-09-25 | Folded the readiness-audit corrections: six holed supplies ship (the audit said five and missed `fix.corner`); the box hit-test defect and a hole-aware two-pass fix as a prerequisite session; the `v21-bench.html` set amendment is required (ADR-107 R1a; P5); free-aspect resize and the unbuilt §3.4.1 lock (D-100) made the stretch policy an owner gate; per-supply defaults break the `{shape.rule}` pin; older builds show an invisible, selectable box; the O14 fold-study gate; recommendation against page frames and an `ImageElement` frame property; base moved to `5f7707a`. Added the product audit, research and readiness line. |
