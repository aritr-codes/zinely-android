# Editor — Visual Direction & Interaction Brief

> **Status:** Design brief · updated 2026-06-28 · scopes the editor UI/UX only (single fixed
> `"default"` project, editor already mounted per [ADR-030](../DECISIONS.md#adr-030)).
> The product-wide **why and feel** (emotional goals, onboarding, UX principles, accessibility,
> first-time journey) live in **[DESIGN-LANGUAGE.md](DESIGN-LANGUAGE.md)** — this doc is the
> editor-surface specifics under that umbrella. Phasing stays in [ROADMAP.md](../ROADMAP.md),
> decisions in [DECISIONS.md](../DECISIONS.md). Home / library / export flows remain **deferred**.

## 1. Why this work, and why now

The editor is fully wired underneath: `:core:editor` ships a complete MVI reducer
(`GoToPage`, `AddPage`, `DeletePage`, `Undo`, `Redo`, `PlaceText`, transform/reorder/delete)
and `:feature:editor` ships the canvas, gestures, resize handles, snap guides, selection
chrome, and a11y context bar. **But the mounted surface exposes almost none of it.**

Concretely, validated against the checkout:

| Capability (exists in core) | Reachable in the mounted UI? |
|---|---|
| Edit all **8 pages** (`Intent.GoToPage`) | ❌ — only page 0 is ever shown |
| Undo / redo (`Intent.Undo/Redo`, `canUndo/canRedo`) | ❌ — no affordance |
| Add text (`Intent.PlaceText` / double-tap) | ⚠️ only via undiscoverable double-tap |
| Add image | ✅ one bare `ExtendedFloatingActionButton` |
| Transform / reorder / delete | ✅ context bar (selection only) |

So the single highest-value gap is **functional, not cosmetic**: you cannot make an
8-page zine because 7 of the 8 pages are unreachable. The visual identity gap is the
second: the app still ships the **default Android Studio template theme** (Purple/Pink
`dynamicColor`, stock Material typography) and unicode-glyph placeholder buttons — it reads
as generic productivity software, the exact thing a zine tool should not be.

This brief co-designs both: the page navigator is the functional unlock and the first
carrier of the visual language.

## 2. Design principles

1. **Print-first, not screen-first.** The artifact is a folded paper booklet. The canvas
   is a sheet of paper on a worktable, not a document in a viewport.
2. **The workbench metaphor.** Surfaces are paper, card, and tape on a desk surface —
   chrome is "supplies" (a tool tray, a strip of page cards), not toolbars and app bars.
3. **Handmade over precise.** Slight rotations, torn/taped edges, soft paper shadows,
   stamp/sticker accents. Imperfection signals "you made this," not "a template made this."
4. **Functional first, decorative second.** Every handmade flourish sits on a control that
   does real work (a page card *navigates*; a tape strip *is* the selected-page marker).
   No decoration that isn't also an affordance or state.
5. **Legible and accessible.** Texture never costs contrast (WCAG AA on all text/controls),
   touch targets stay ≥48dp, and the existing a11y semantics layer is preserved untouched.

## 3. Visual language

- **Palette** — paper & ink, with a small set of "craft accent" tape/sticker colors:
  - `paper` warm off-white `#F4EFE6` (canvas + cards), `paperEdge` `#E7DFD0` (card edge/shadow).
  - `desk` muted slate `#3A3A3C` (the worktable behind the sheet) — frames the bright page.
  - `ink` near-black `#23201C` (text), `inkSoft` `#6B6358` (secondary).
  - accents: `tapeYellow #E9C46A`, `tapeCoral #E76F51`, `tapeTeal #2A9D8F`, `stampBlue #264653`.
- **Type** — a display/marker face for chrome labels (handmade feel) over a clean body face
  for editable content. MVP uses Material typography re-weighted; a bundled marker font is a
  follow-up (the render path already bundles **Inter** for export, ADR-028).
- **Texture** — subtle paper grain on the canvas + a soft drop shadow + a faint torn/taped
  top edge. Cheap to draw (one tint + one shadow + a few tape rectangles); no bitmap assets
  required for the MVP slice.
- **Dynamic color is OFF.** A print-brand app needs one consistent identity, not the user's
  wallpaper palette — so we replace `dynamicColor` defaults with the fixed zine scheme.

## 4. Interaction model (target)

```
┌─────────────────────────────────────────────┐
│  ░░ desk surface ░░                          │
│     ┌───────────────────────────────┐  ╲tape │   tool tray (right):
│     │                               │        │   ● add photo
│     │      paper sheet (page n)     │  + T   │   ● add text   (T)
│     │      — fit to canvas —        │  ↶ ↷   │   ● undo / redo (enabled by canUndo/Redo)
│     │                               │        │
│     └───────────────────────────────┘        │
│  [1][2][3*][4][5][6][7][8]   ← page strip     │   page cards: tap = GoToPage,
│   tape marks the current page; cards mini-render │ current page lifted + taped
├─────────────────────────────────────────────┤
│  ‹ › ʌ v  + −  ↻ ↺  ⤒ ⤓  ⌫   (on selection)   │   context bar: real icons on stamped chips
└─────────────────────────────────────────────┘
```

- **Page strip.** A horizontal row of eight small paper *cards* under the canvas. The current
  page is lifted, slightly rotated, and marked with a strip of "tape". Each card hosts a **live
  mini-render** of its page (the `SceneRenderer` → `PagePreview` path the main canvas uses), so a
  card looks like the page it navigates to; an empty page renders as a blank sheet and keeps a faint
  page number to stay legible. Tapping a card dispatches `Intent.GoToPage(index)`. This makes all
  eight pages reachable for the first time.
- **Supply tray (shipped — `EditorSupplyTray`).** Replaced the lone FAB: add-photo, add-text,
  undo, redo as visible "supply" buttons under the canvas; undo/redo bind to `canUndo`/`canRedo`.
  See §6 slice 4.
- **Context bar (shipped — `EditorContextBar`).** The on-selection transform controls now read as
  the zine identity: a "desk" band of small **stamped paper craft-chips**, each carrying a real
  Material icon (a slight handmade tilt) in place of the old unicode placeholder glyphs. The row
  scrolls horizontally so every ≥48dp control stays reachable on a narrow phone. Behavior, the
  dispatched intents, single-selection gating, and the spoken a11y labels are unchanged. See §6 slice 6.

## 5. Tradeoffs & alternatives considered

- **Cosmetic reskin first vs. functional-unlock first.** A pure theme reskin would look
  better in a screenshot but still ship a zine maker that can only edit one page. Rejected:
  the page strip delivers both a real capability and the first dose of identity.
- **Page strip vs. a swipe/pager for pages.** A horizontal pager is more "app-like" and
  hides the booklet structure; the strip keeps all eight pages visible at once (closer to
  laying spreads on a table) and is more discoverable. Chosen: strip. A pager remains a
  possible V1 gesture on top of it.
- **Real mini-renders vs. lightweight cards.** Live `SceneRenderer` thumbnails per card add
  decode/measure cost (each card hosts its own `PagePreview` → `CanvasReplayer`, so the strip is
  ~8 preview hosts) and widen the strip's inputs (it now takes the pages + page size + defaults,
  not just a content predicate). The first cut shipped styled cards with a content dot; the
  mini-render swap landed next as an isolated follow-up that left the interaction (one `GoToPage`
  per tap, same a11y) untouched. Accepted: the strip composes once and each card's tape is memoised
  per page, so per-edit work is one re-render of the changed card.
- **Texture via bitmap assets vs. drawn.** Bundled paper PNGs look richer but add binary
  assets and density variants. MVP draws texture with tint + shadow + shapes to keep the
  slice asset-free and reviewable; richer textures can land later behind the same surface.

## 6. Implementation slices

**Shipped (v0.4.0):**

1. **Zine theme foundation** (`:app`): fixed paper/ink/craft palette, `dynamicColor` off,
   so the identity is consistent and print-brand-led.
2. **`EditorPageStrip`** (`:feature:editor`): the scrapbook page navigator, hosted in
   `EditorScreen`, wiring the already-shipped `Intent.GoToPage`. The functional unlock —
   all eight pages reachable. **Mini-render upgrade (this slice):** each card now hosts a live
   `SceneRenderer` → `PagePreview` thumbnail of its page (the canvas render path), replacing the
   number+content-dot placeholder; behavior, `GoToPage`, the `Role.Tab`/"Page N" picker semantics,
   and ≥48dp targets unchanged. Empty pages keep a faint number for legibility.
6. **`EditorContextBar` restyle** (`:feature:editor`): the on-selection transform controls restyled to
   the zine identity — a "desk" band of stamped paper craft-chips, each carrying a real Material icon
   (handmade tilt) in place of the old unicode glyphs, with the row scrolling horizontally so every
   ≥48dp control stays reachable. The dispatched intents, single-selection gating, and spoken a11y
   labels are unchanged — the last unstyled editor chrome now reads as designed supplies. See §4.

**Next (current milestone — first-time creation experience, see
[DESIGN-LANGUAGE §9](DESIGN-LANGUAGE.md#9-implementation-priority)):**

3. **`EditorEmptyState`** (`:feature:editor`): the cozy first-run invitation shown when the
   current page has no elements — encouraging copy + discoverable "add a photo" / "add words"
   supplies (the audit's empty-state + discoverability + visible-add-text + contextual-guidance
   findings). Wires `Intent.RequestAddImage` and `Intent.PlaceText`.
4. **`EditorSupplyTray`** (`:feature:editor`): the always-visible scrapbook supply shelf under the
   canvas — **Add a photo** (`Intent.RequestAddImage`), **Add words** (the empty-state add-text
   behavior), **Undo** / **Redo** bound to `canUndo` / `canRedo`. Replaces the app-level lone "Add
   image" FAB (removed from `ZinelyNavHost`), so every primary action is a visible, thumb-reachable
   supply (DESIGN-RULES 1–3, 7) and undo/redo are reachable for the first time (DESIGN-RULES 6).
5. **`EditorMoveResizeHint`** (`:feature:editor`): a one-time contextual hint, persisted across sessions
   ([ADR-032](../DECISIONS.md#adr-032)) so it shows once per install. The moment a
   placed element is first single-selected (resize handles up, not editing) it floats in the canonical
   line ([VOICE §3](VOICE.md#3-microcopy-library)) — *"Drag to move it. Pinch to resize."* — teaching the
   two manipulations that have no discrete-control twin. Non-modal and non-blocking (declares no
   `pointerInput`; touches fall through to the gesture surface), with an explicit ≥48dp "Got it"; it also
   auto-dismisses the instant a live drag/resize begins (discovery *is* dismissal). **Now persisted across
   sessions** ([ADR-032](../DECISIONS.md#adr-032)): the "seen" flag lives in a local Preferences DataStore
   behind an `EditorOnboardingStore` seam (`:data-android`), read as a load-aware tri-state so the hint
   shows **once per install** and never flashes before its state loads — the editor's first persisted
   onboarding state and the reusable seam the Settings store grows into.

**Resolved — blank-page add-action de-dup ([ADR-033](../DECISIONS.md#adr-033)):** the empty-state
overlay and the tray no longer both surface add-photo / add-words. The **tray is the single action
home** (always visible, thumb zone); the `EditorEmptyState` overlay is now **invitation-only** — warm
copy + ornament + privacy line, **no buttons** — whose subcopy points to the supplies below. So each
add action appears exactly once on a blank page (DESIGN-RULES R3/R7), unambiguous for TalkBack too.

**Resolved — blank-page tray-orientation polish (ADR-033 follow-up):** the invitation and the shelf are
now tied together both ways. The `EditorEmptyState` carries a subtle **downward cue** (a static chevron,
no motion → reduced-motion-safe; cleared from the a11y tree, so it adds no screen-reader noise) that
points the eye to the supplies just below; and the `EditorSupplyTray` is titled with a quiet
**"Supplies" `heading()`** so a screen reader lands on a named section landmark before the four actions
(DESIGN-RULES 9). The four supplies, their intents, undo/redo gating, and add behaviors are unchanged.

Deferred to later slices (designed, not built now): bundled marker font, richer paper textures, and
a string-resource catalog ([ARCHITECTURE §15.6](../ARCHITECTURE.md#156-architectural-implications-surfaced-by-the-design-sprint-2026-06-28)).
Home / library / export stay out of scope entirely.
