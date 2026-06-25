# Research notes — transform, layers, page-nav, export mockups

Scope: affordance/flow research feeding the throwaway editor mockups
(`transform.html`, `layers.html`, `page-nav.html`, `export.html`). These lock
*layout/flow/tool-affordances* for a future editor MVI spike — not a spec.

Labels: ✅ **VERIFIED** (sourced) · 🟦 **RECOMMENDATION**.

---

## 1. Gesture → transform mapping (feeds the MVI gesture→`AffineTransform2D` spike)

This is the load-bearing finding: the canvas gesture detector should emit one
combined transform delta, not three separate listeners.

- ✅ **VERIFIED** — Jetpack Compose's `detectTransformGestures` delivers
  `centroid`, `pan` (Offset), `zoom` (Float scale factor) **and** `rotation`
  (degrees) from a *single* multi-touch callback. That maps directly onto a
  translate · scale · rotate affine update around the gesture centroid.
  ([Compose — Multitouch: panning, zooming, rotating](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch))
- ✅ **VERIFIED** — Single-pointer move is a separate concern: `detectDragGestures`
  accumulates an `Offset` you add to the element's translation. Use it for
  drag-move; reserve `detectTransformGestures` for pinch/rotate.
  ([Compose — Drag, swipe, and fling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling))
- ✅ **VERIFIED** — Rotation is conventionally expressed in **degrees** and shown
  with a dedicated rotation handle plus a numeric readout; pro tools also offer
  snap increments. (Figma exposes a rotation field and handle; rotation is
  measured in degrees.)
  ([Figma — Adjust alignment, rotation, position, and dimensions](https://help.figma.com/hc/en-us/articles/360039956914-Adjust-alignment-rotation-and-position))

**🟦 RECOMMENDATION — gesture map (implemented in `transform.html`):**

| Gesture | Editor intent | Affine component |
|---|---|---|
| Long-press | select element | (selection only) |
| 1-finger drag | move | `translate(dx,dy)` |
| Pinch (2-finger) **or** corner-dot drag | scale | `scale(z)` about centroid |
| 2-finger twist **or** rotation-knob drag | rotate | `rotate(θ°)` about centroid |

- 🟦 Compose multi-touch already fuses pan+zoom+rotation per frame, so prefer a
  **single MVI `TransformGesture(centroid, pan, zoom, rotation)` intent** that the
  reducer folds into the element's affine matrix, rather than three intents that
  can race. Snap rotation to 0/15/45/90° and scale to keep-ratio by default.
- 🟦 Always surface a **live readout pill** (`x,y · 120% · 15°`) so the gesture's
  effect on the affine is legible and testable (golden assertions can read it).

---

## 2. Touch targets, handles & thumb reach

- ✅ **VERIFIED** — Reachability: the bottom-centre of a phone screen is the
  comfortable one-thumb zone; primary, frequent controls belong in a bottom bar,
  with hard-to-reach top corners reserved for rare actions.
  ([IxDF — The Thumb Zone: designing for mobile users](https://www.interaction-design.org/literature/article/the-thumb-zone-designing-for-mobile-users))
- 🟦 **RECOMMENDATION** — Render selection handles small (~18px dot) but wrap each
  in an **invisible ≥48dp hit-pad** (done via `::after { inset:-15px }` in
  `transform.html`). This keeps the box uncluttered while satisfying Android's
  minimum touch target. Primary tools live in the thumb-reach `.bottombar`.

---

## 3. Layers / z-order reorder

- ✅ **VERIFIED** — Standard mental model: **list order = paint order**; the
  top-most row is front-most (drawn last). Pro editors (Figma) let you reorder
  layers and also bring-forward / send-back; visibility toggles per layer.
  ([Figma — Adjust alignment, rotation, position, and dimensions](https://help.figma.com/hc/en-us/articles/360039956914-Adjust-alignment-rotation-and-position))
- 🟦 **RECOMMENDATION** — Use an **M3 standard bottom sheet** list with a
  drag-`⠿`-grip per row + a primary-colour drop-line for the landing slot
  (discoverable drag), **plus** four discrete arrange buttons
  (Back / Backward / Forward / Front) as the non-drag fallback. Selection is
  two-way bound between canvas and list. (Implemented in `layers.html`.)

---

## 4. Page / spread navigation

- 🟦 **RECOMMENDATION** — Filmstrip of page thumbnails that doubles as the pager
  (swipe-snap), with a primary 3px ring on the current page and a `n of N`
  + page-dots indicator. A trailing **＋ Add** appends a blank page; long-press a
  thumbnail to drag-reorder. A **Single / Spread** toggle switches the canvas and
  thumbnail shape (spread = two facing pages with a centre gutter) for booklet
  zines. Mirrors the Slides/Canva filmstrip mental model. (Implemented in `page-nav.html`.)
- ✅ **VERIFIED** — Bottom sheets are the M3 surface for secondary content/actions
  anchored to the bottom edge (used for the layers + export sheets).
  ([Material Design — Bottom sheets](https://m3.material.io/components/bottom-sheets/guidelines))

---

## 5. Export entry point (offline / home-print)

- 🟦 **RECOMMENDATION** — **M3 modal bottom sheet** with: an offline/privacy banner
  first ("stays on your device — no account, no cloud"), Format chips (PDF default
  / Images), Page-size chips (Letter / A4), home-print options (fold+cut guide
  marks, fit-to-printable-area), and ONE decisive primary **Export PDF** button.
  No cloud/share path — enforces the privacy invariant from the PRD.
  (Implemented in `export.html`.)
- ✅ **VERIFIED** — Bottom sheets / dialogs are the M3-sanctioned container for such
  a focused, dismissible task surface.
  ([Material Design — Bottom sheets](https://m3.material.io/components/bottom-sheets/guidelines))

---

### Open questions for the MVI spike
- Should rotation snapping be a held-modifier (free rotate) or always-on with a
  long-press-to-free escape hatch?
- Corner-dot scale: lock aspect by default with a modifier to free-scale, or expose
  a toggle in the contextbar?
- Spread view: does imposition happen at render time or is "spread" purely a
  viewing convenience over single-page documents? (defer to imposition spike)
