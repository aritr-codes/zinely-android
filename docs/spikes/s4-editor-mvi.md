# Spike (SEED) — S4 editor MVI

> **Status: SEED / inputs-only (2026-06-25).** This is **not** a design yet — it collects the
> requirements and references the next session will turn into the actual MVI design (store / intents /
> reducer, document-tree place/transform, undo/redo, gesture→`AffineTransform2D`), Codex-review it, and
> record the accepted decisions as an ADR (extending [ADR-005](../DECISIONS.md#adr-005) /
> [ADR-013](../DECISIONS.md#adr-013), the editor=MVI decisions). Do **not** treat anything here as locked.
>
> **Builds on (already landed):** the S3 render tier and the S4 **preview host**
> (`:feature:editor` `PagePreview`, PR #19) + the S4 editor **mockups** (PR #18). The editor renders
> through the existing `PagePreview` over the shared `CanvasReplayer` — preview==export is already proven,
> so this spike owns *state + interaction*, not rendering.

---

## 1. Purpose & boundary

Design the stateful editor on top of the stateless preview host. The editor is **MVI**
([ADR-005](../DECISIONS.md#adr-005)): `Intent → pure reducer → new EditorState` exposed as `StateFlow`,
with I/O (autosave, image decode) on a side-effect channel — never in the reducer
([RESEARCH R5.2](../RESEARCH.md#r52-mvi-for-the-editor)).

**In scope (to design next session):** `EditorState` shape; the intent set (select / place / move /
scale / rotate / reorder / delete / edit-text / add-image / page-nav); the pure reducer; the
document-tree mutations (place/transform an element in page-local points); undo/redo; and the
gesture→`AffineTransform2D` pipeline that feeds committed geometry back into the tree.

**Out of scope:** rendering (done — `PagePreview`/`CanvasReplayer`), export/imposition (S5), persistence
wiring (S2B autosave already exists; the editor dispatches to it).

---

## 2. Inputs to consume

| Input | What it gives this spike |
|---|---|
| `:feature:editor` `PagePreview` (PR #19) | The render surface: `PagePreview(tape, sheet, screenPxPerPt, pageOffset, modifier, imageBytes)`. Editor produces the `DrawCommand` tape from the document tree and hoists `screenPxPerPt`/`pageOffset` as view state. |
| Editor mockups (PR #18, `docs/design/editor-mockups/`) | Layout + flow + tool affordances: bottom tool bar, selection + transform handles, contextual toolbar, text/image flows, layers/z-order, page nav, export entry. **Throwaway** — distill decisions here, don't treat the HTML as spec. |
| [RESEARCH R5](../RESEARCH.md#r5-canvas--scene-graph-editor-architecture) | MVI reducer, **command + field-memento undo**, **begin/update/commit** drag coalescing via `graphicsLayer{}` lambda, flat element list + hit-testing + snapping as pure functions. |
| [RESEARCH R9](../RESEARCH.md#r9-editor-reference--zine-maker-cross-product-study) | `zine-maker` data shapes (uniform `Element` + per-type `properties`), **persist resolved geometry / bake transform on commit**, integer `zIndex` reorder, data-driven imposition. |
| [RESEARCH R10](../RESEARCH.md#r10-editor-accessibility--wcag-22-aa--androidmaterial-3-guidelines) | The accessibility requirements below — **load-bearing for the intent set and the contextbar**. |

---

## 3. Gesture → `AffineTransform2D` (the load-bearing mechanism)

The interaction core. Locked gesture map (from the mockups + [Compose multi-touch](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch)):
**long-press select · 1-finger drag move · pinch scale · two-finger twist rotate · double-tap edit text.**

- ✅ Compose `detectTransformGestures { centroid, pan, zoom, rotation -> }` yields pan (`Offset`), zoom
  (`Float`), and rotation (degrees) about a centroid in **one** callback → emit a **single**
  `TransformGesture` intent the reducer folds into the element's geometry, not three racing intents.
- 🟦 **Persist resolved geometry, derive the matrix at render** ([R9.4](../RESEARCH.md#r94-recommendation--recommendation)):
  live drag updates *preview-only* state through a `graphicsLayer{}` lambda
  ([R5.3](../RESEARCH.md#r53-coalescing-a-drag-into-one-undo-step)); on gesture end, **bake** the
  accumulated scale/rotate/translate into committed `{x, y, w, h, rotationDeg}` (points) via one
  `TransformCommand(ids, before, after)` — avoids stored-matrix drift. `:core:render` computes the
  `AffineTransform2D` from that geometry (already the contract).

> Open questions for the design session: decomposed fields vs a stored matrix in the model; scale anchor
> (centroid vs handle corner); snapping thresholds (`~8px/zoom`, [R5.4](../RESEARCH.md#r54-scene-model-hit-testing-snapping)); group transforms.

---

## 4. Accessibility requirements (non-negotiable, from [RESEARCH R10](../RESEARCH.md#r10-editor-accessibility--wcag-22-aa--androidmaterial-3-guidelines))

These shape the **intent set and the contextbar**, so they belong in the design from the start, not as a retrofit:

- **Non-gesture transform path (WCAG 2.5.7).** Every gesture transform MUST have a single-pointer
  equivalent: nudge buttons + scale/rotate steppers in the selection contextbar, dispatching the **same**
  intents as the gestures, plus `Modifier.semantics { customActions }` per selected element. TalkBack /
  Switch-Access cannot pinch or rotate.
- **Page-Canvas semantics.** `PagePreview` is currently semantically silent (a `testTag` only). The editor
  must expose page/element semantics (a focusable node per placed element as selection lands) or an
  accessible layers list. Mark the Canvas decorative only if identical content + controls exist elsewhere.
- **Focus visible (2.4.7)**, **≥48dp targets** (handles, contextbar icon buttons, chips — Material 3),
  **non-text contrast ≥3:1** (use `--md-outline`, not surface-variant hairlines), **real button/list/heading
  semantics (1.3.1)**, and **edge-to-edge + `WindowInsets`** so bars/FAB clear the gesture-nav handle.

---

## 5. To produce next session

1. `EditorState` + intent set + pure reducer (TDD, pure-JVM where possible).
2. Document-tree place/transform mutations in points; uniform `Element`/`properties` shapes.
3. Undo/redo = command objects with field-level mementos + begin/update/commit gesture coalescing.
4. Gesture→`AffineTransform2D` pipeline (§3) with the accessibility-mandated non-gesture path (§4).
5. Codex review → ADR (extend ADR-005/013) → gated build under the existing CI.

> Per the Documentation Rule: the accepted decisions land as an **ADR**; durable evidence is already in
> [RESEARCH R5/R9/R10](../RESEARCH.md). This seed is superseded once the real design spike is written.
