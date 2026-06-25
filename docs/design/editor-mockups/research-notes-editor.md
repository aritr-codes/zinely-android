# Editor Mockup — Research Notes

Scope: on-canvas editing UX research feeding the **throwaway** HTML mockups in this folder
(`editor.html`, `text-edit.html`, `add-image.html`). These lock layout / flow / tool-affordances
for a future editor **MVI gesture → `AffineTransform2D`** spike. NOT a spec, NOT pixel-perfect.

Label key: ✅ VERIFIED (sourced) · 🟦 RECOMMENDATION.

---

## 1. Transform-handle conventions (selection bounding box)

✅ **VERIFIED — Best-in-class mobile editors use a rectangular bounding box with corner handles + a
detached rotation handle.** Canva's mobile editor draws a selection rectangle with round corner
handles for proportional scale and a dedicated rotate affordance; the selected element also raises a
contextual toolbar.
[Canva — editing on mobile](https://www.canva.com/help/editing-on-mobile/) ·
[Canva — rotate, flip & adjust elements](https://www.canva.com/help/adjust-elements/)

✅ **VERIFIED — Touch targets must be ≥48×48dp.** Material 3 and the Android accessibility guidance
both mandate a minimum touch target of 48dp even when the painted glyph is smaller; the touch region
is expanded around small visuals. Our handles render as ~16–24dp dots but carry a ≥48dp hit area.
[M3 — Accessibility / touch targets](https://m3.material.io/foundations/designing/accessibility) ·
[Android — Accessibility in Compose: key steps](https://developer.android.com/develop/ui/compose/accessibility/key-steps) ·
[Android — Make apps more accessible (48dp)](https://support.google.com/accessibility/android/answer/7101858)

🟦 **RECOMMENDATION — Handle layout for the spike:**
- **4 corner dots** = proportional (uniform) scale. Diagonal drag.
- **Rotation handle** detached **above the top edge** (a stem + circular grip), the long-standing
  convention popularised by desktop editors and carried into Canva/Keynote/Figma. Offset keeps it
  clear of the bounding box so it doesn't collide with a corner.
- **Bounding box outline** in `--md-primary`; handles filled `--md-on-primary` with a primary ring
  so they read on both light page and dark imagery.
- Omit mid-edge (non-uniform stretch) handles for MVP — they complicate aspect-ratio locking and are
  rarely used on phones. (🔭 revisit for text-box width handles later.)

---

## 2. Contextual toolbar vs. persistent bottom tool bar

✅ **VERIFIED — Direct-manipulation UIs keep actions on/near the object and reflect state
continuously.** NN/g defines direct manipulation as continuous representation of objects with
physical, reversible, immediately-visible actions — selection handles and an object-anchored toolbar
are the canonical pattern.
[NN/g — Direct Manipulation: Definition](https://www.nngroup.com/articles/direct-manipulation/)

🟦 **RECOMMENDATION — Two-tier toolbar model (matches Canva / Adobe Express / Google Slides):**
- **Persistent bottom tool bar** (M3, thumb-reach): document-level *insert/navigate* actions —
  Add text · Add image · Layers · Pages · Export. Always visible when nothing is mid-edit.
- **Contextual bar** appears just above the selection (or as the bottom bar's content) when an
  element is selected: element-specific actions (Edit, Duplicate, Delete, + type-specific:
  font/size/color/align for text; replace/crop/filter for image). Dismiss on deselect.
- This keeps the global bar stable (low cognitive load) while surfacing object actions inline.

---

## 3. Mobile gesture map (the part that feeds the MVI spike)

✅ **VERIFIED — Jetpack Compose provides first-class multi-touch transform primitives.** A single
`PointerInputScope.detectTransformGestures { centroid, pan, zoom, rotation -> }` delivers
**pan (Offset)**, **zoom (Float scale)** and **rotation (Float degrees)** around a **centroid** in
one callback — the exact inputs for composing an affine transform. `Modifier.transformable` +
`rememberTransformableState` wrap the same model declaratively. Tap/long-press/double-tap come from
`detectTapGestures { onTap, onDoubleTap, onLongPress }`.
[Android — Drag, swipe & fling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling) ·
[Android — Tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press) ·
[androidx.compose.foundation.gestures — API reference](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary)

🟦 **RECOMMENDATION — Gesture → intent map for the editor (mock annotates these):**

| Gesture | Compose source | MVI intent | Affine effect |
|---|---|---|---|
| **Single tap** on element | `onTap` | `SelectElement(id)` | none (show selection) |
| **Tap empty canvas** | `onTap` | `Deselect` | none |
| **Long-press** | `onLongPress` | `SelectElement` + raise context menu (drag-to-reorder grip) | none |
| **Double-tap** text | `onDoubleTap` | `EnterTextEdit(id)` | none (focus IME) |
| **One-finger drag** | `pan` (Offset) | `Translate(dx,dy)` | translation |
| **Two-finger pinch** | `zoom` (Float) | `Scale(factor, centroid)` | uniform scale about centroid |
| **Two-finger rotate** | `rotation` (deg) | `Rotate(deg, centroid)` | rotation about centroid |
| **Corner-dot drag** | custom `detectDragGestures` on handle | `Scale(...)` (uniform, about opposite corner) | scale |
| **Rotation-handle drag** | custom `detectDragGestures` on handle | `Rotate(...)` (about box center) | rotation |

🟦 **RECOMMENDATION — Compose pinch+rotate+pan in ONE transform per gesture frame** so the editor
state stays a single `AffineTransform2D` (translate ∘ rotate ∘ scale about centroid). This is the
core reason to mock the handle + gesture affordances now: the *centroid* and *which corner is the
scale anchor* directly determine the matrix the spike must build. Keeping uniform-scale-only for MVP
means the scale component stays a single scalar, simplifying the matrix and the inverse used for
hit-testing.

---

## 4. Image insert — offline / on-device emphasis

✅ **VERIFIED — Direct, recognizable, on-device sources reduce friction.** Canva's mobile insert
flow surfaces the device gallery/camera directly in a sheet. Zinely's privacy invariant (no network,
no cloud) means our picker shows **only** on-device sources — system photo picker / gallery — and we
say so explicitly in the UI copy.
[Canva — uploading & adding media on mobile](https://www.canva.com/help/upload-images/) ·
Zinely privacy invariant — see [PRD §5](../../PRD.md) / [ARCHITECTURE.md](../../ARCHITECTURE.md).

🟦 **RECOMMENDATION — Use the Android Photo Picker affordance** (system gallery, no storage
permission, no network) as the conceptual model for the bottom sheet. Sheet header copy states
"On-device only — nothing leaves your phone." Sources: Photos (gallery), Camera. No "from URL", no
stock, no cloud drives in MVP.

---

## Open questions for the spike (deferred)
- Mid-edge width handles for text boxes (reflow vs. scale)? 🔭 FUTURE.
- Snap/guide lines on drag (center + edge snapping)? 🔭 FUTURE.
- Rotation snapping to 0/45/90°? 🟦 likely yes — cheap and high-value.
