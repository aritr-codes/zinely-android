# Zinely — Roadmap

> **The single source of truth for phasing.** *Every roadmap change is reflected here.* Scope detail per phase lives in [PRD.md](PRD.md); the "how" in [ARCHITECTURE.md](ARCHITECTURE.md); rationale in [DECISIONS.md](DECISIONS.md). No dates are committed yet — phases are ordered, not scheduled.

- **Status:** Draft v0.1 · 2026-06-19

## Phase overview

```mermaid
timeline
    title Zinely phases (ordered, not dated)
    MVP : One format, done right
        : 8-page single-sheet, photo+text, auto-imposition
        : home-print PDF/image, projects+autosave, undo
    V1 : A real editor
        : snapping/guides, crop, templates, fonts
        : .zine backup/restore, PrintManager, a11y
    V2 : More formats & expression
        : 16-page saddle-stitch, drawing/stickers
        : custom fonts, print-shop export groundwork
    Future : Reach & polish
        : KMP iOS/desktop, commercial prepress
        : on-device AI, P2P sharing, stylus
```

## Guiding sequence

The build order inside every phase follows risk: **prove the riskiest, most isolatable thing first.** That is why the **imposition engine** (pure Kotlin, fully testable) is the first vertical spike — see [spikes/imposition-engine.md](spikes/imposition-engine.md) and [ADR-007](DECISIONS.md#adr-007).

```mermaid
flowchart LR
    S1["Spike: imposition engine\n(+ SVG proof sheet)"] --> S2["Data/storage layer\n(Room meta + JSON doc)"]
    S2 --> S3["Render pipeline\n(shared scene → preview/export)"]
    S3 --> S4["Editor (MVI)\nplace/transform/undo"]
    S4 --> S5["Export flow\nPDF + image + share"]
    S5 --> MVP(["MVP complete"])
```

> **Status:** **S1–S4 are implemented and on `main`.** S1 imposition engine (pure-Kotlin `:core:model` + `:core:imposition`, 95 tests, milestone `v0.1.0-imposition-engine`); S2 persistence (`:core:data` contracts + pure-JVM `:core:data-storage` durability core/asset store + Android `:data-android` adapters); S3 render (`:core:render` pure tier + `:render-android` PDF/raster backends); S4 editor (`:core:editor` MVI core + `:feature:editor` interaction surface, now **mounted in `:app`** with interactive image import and autosave). Each was TDD'd and Codex-reviewed per increment.
>
> **What is NOT yet built**, despite the MVP scope below: production persistence is **file-only and single-project** — `data-android` ships a file-backed `DocumentRepository` writing `projects/<id>/document.json` on one fixed `"default"` project; **Room metadata, `ProjectRepository`, and the asset GC/sweeper are deferred** (no multi-project store yet). The render/**export backends exist** in `:render-android`, but there is **no user-facing export, share, print, or home/library flow** wired yet. **S5 (export flow) and the Room-backed project layer remain the next build steps.**

---

## MVP — "one great format, done right"
**Goal:** a beginner prints a correct 8-page zine in under 10 minutes, fully offline.

- 8-page single-sheet zine; Letter + A4.
- Photo placement (move/resize/rotate, fit/fill); text placement (bundled fonts, size/color/align).
- Single/double/full per-page layouts.
- Automatic imposition ([ADR-007](DECISIONS.md#adr-007)).
- Home-print-ready PDF (vector text) + 300 DPI image export ([ADR-001](DECISIONS.md#adr-001), [ADR-011](DECISIONS.md#adr-011)).
- Print correctness: safe area, fold/cut guides, calibration ruler, "Actual size" guidance ([ADR-012](DECISIONS.md#adr-012)).
- Projects: create/open/duplicate/delete, thumbnails.
- Autosave + crash recovery ([ADR-009](DECISIONS.md#adr-009)).
- Command-based undo/redo ([ADR-005](DECISIONS.md#adr-005)).
- Share via FileProvider; in-app fold instructions.

**Exit criteria:** all MVP functional requirements in [PRD §10](PRD.md#10-functional-requirements-mvp) pass; printed test zines fold to 1→8 reliably; no network calls; no crash data loss in dogfooding.

## V1 — "a real editor"
- Snapping / alignment guides ([R5.4](RESEARCH.md#r54-scene-model-hit-testing-snapping--verified--assumption)).
- On-device crop / rotate / basic adjustments (no remote processing).
- Templates & themes; richer typography; bundled font expansion.
- Page reorder / duplicate.
- **`.zine` backup & restore** via SAF ([ADR-009](DECISIONS.md#adr-009)).
- Android **PrintManager** in-app print path ([R2.3](RESEARCH.md#r23-system-print-framework--recommendation)).
- Calibration test sheet; thumbnails everywhere.
- Full accessibility pass; dark theme; Baseline Profile.

## V2 — "more formats & expression"
- Additional impositions: 4-page, **16-page saddle-stitch** (double-sided + binding guidance) — a distinct imposition family ([R1.7](RESEARCH.md#r17-variants--pitfalls--disputed--assumption)).
- Drawing / stickers / freehand layer.
- **Custom font import** (`.ttf`).
- **Print-shop export groundwork**: bleed, trim/crop marks, margins — still RGB ([ADR-002](DECISIONS.md#adr-002)).
- Multi-page spreads; batch export; grid/layers panel.
- Optional, explicit, user-initiated community sharing (network strictly opt-in).

## Future vision
- **KMP / Compose Multiplatform** (iOS + desktop) reusing the pure-Kotlin core.
- **Commercial prepress** (CMYK/ICC/PDF-X) via a real PDF engine — likely an off-device step, weighed against offline-first ([R2.7](RESEARCH.md#r27-third-party-pdf-libs--future)).
- On-device AI layout/auto-caption suggestions (privacy-preserving, no cloud).
- Peer-to-peer / Wi-Fi-Direct `.zine` sharing (no central server).
- Local template/plugin ecosystem; tablet + stylus first-class; print-shop partner export profiles.

---

## Change log
| Date | Change | Linked ADR / PRD |
|---|---|---|
| 2026-06-19 | Initial roadmap established | [PRD §7](PRD.md#7-scope--mvp) |
| 2026-06-19 | S1 imposition engine spike implemented (pure Kotlin, 95 tests, Codex-reviewed) | [ADR-007](DECISIONS.md#adr-007) |
| 2026-06-19 | S2 decision gate **cleared** — ADR-019…023 all Accepted (autosave, asset ownership/GC, fidelity); S2 implementation unblocked | [ADR-021](DECISIONS.md#adr-021), [ADR-022](DECISIONS.md#adr-022), [ADR-023](DECISIONS.md#adr-023) |
| 2026-06-19 | **S2A pure-Kotlin data core implemented** (`:core:data`: schema, serializer+migration, validation, repo/asset contracts; TDD, Codex-reviewed); ADR-015 resolved + ADR-020 amended | [ADR-015](DECISIONS.md#adr-015), [ADR-020](DECISIONS.md#adr-020), [spike §11](spikes/data-storage-layer.md#11-implementation-status--s2a-pure-kotlin-data-core-2026-06-19) |
| 2026-06-20 | **S2A merged** (PR #4); follow-ups: `minSdk 24` ratified, CI added (core JVM tests), S2B asset-GC race test plan documented | [ADR-024](DECISIONS.md#adr-024), [spike §9.1](spikes/data-storage-layer.md#91-mandatory-s2b-tests--asset-gc-race-closure-adr-022) |
| 2026-06-20 | **S2B kicked off** (PR #5 merged; ARCHITECTURE §15.5 drift reconciled). Layering set: pure-JVM `:core:data-storage` (durability/GC, CI-tested) + Android `:data-android` adapters; ADR-022 race closure re-anchored on pins+generation (mtime demoted to secondary guard) | [ADR-025](DECISIONS.md#adr-025), [ADR-022 amendment](DECISIONS.md#adr-022) |
| 2026-06-24 | **S3 `:core:render` design accepted + pure-JVM tier implemented** (Codex GO on design ×3 rounds and on code). Pure page→draw-command tape (only dep `:core:model`, 23 tests, TDD); image fit/crop via shared `computeImageBlit` with decoder-truth intrinsic (seam A); point-space shared `StaticLayout` text path. **Android parity backend tier (Roborazzi preview==export) still remains** — S3 not complete | [ADR-027](DECISIONS.md#adr-027), [spike](spikes/core-render.md) |
| 2026-06-24 | **S3 Android backend tier design accepted — ADR-028** (Codex GO-WITH-FIXES ×2, repo-validated, all reconciled). New gated `:render-android` module; one `CanvasReplayer` + two canvas providers; PDF draws in PostScript points (separate raster scale); crop-aware region decode; bundled self-covering MVP-charset fonts; Robolectric-NATIVE Roborazzi multi-scale **raw-`CanvasReplayer` raster/PDF parity goldens** (Compose preview-host parity owed by S4). **Design only — `:render-android` not scaffolded; S3 still incomplete until the G1–G6 build lands** | [ADR-028](DECISIONS.md#adr-028), [spike](spikes/core-render-android-backend.md) |
| 2026-06-25 | **S3 Android backend tier BUILT + MERGED** (`:render-android`, G1–G6): one `CanvasReplayer` + two export providers, point-space `SharedTextLayout`, crop-aware `ImageBlitter`, bundled **Inter** (MVP charset + cmap coverage guard). Roborazzi raster + text parity goldens are **headless-CI-gated**; image + PDF write/parity proofs run on-device (compile-checked in CI). Closes S3 raster+PDF parity (Compose preview-host parity proven in S4 Step 1). | [ADR-028](DECISIONS.md#adr-028), [spike](spikes/core-render-android-backend.md) |
| 2026-06-25 | **S4 Step 1 preview host + pure `:core:editor` MVI merged.** PR #19: stateless `PagePreview` Compose `drawIntoCanvas` host over the same `CanvasReplayer`, `preview == export` proven headless (discharges Codex Required-fix C). PR #20: pure **`:core:editor`** reducer — `EditorModel`/`Intent`/`EditorReducer`/`HitTest`/`Snap`/`Command`, 43 pure-JVM tests; **ADR-029 Accepted**. | [ADR-029](DECISIONS.md#adr-029), [spike](spikes/s4-editor-mvi.md) |
| 2026-06-26 | **S4 `:feature:editor` interaction surface MERGED** (PR #21 — 10 increments, each Codex-reviewed): store + effect runner, gesture pipeline, selection chrome + live document-order preview, opposite-anchor resize, live snap guides (preview==commit), a11y contextbar + element semantics (WCAG 2.5.7), race-safe text-edit session, host `EditorScreen`, and **selection-chrome Roborazzi goldens** (CI-gated). **Editor not yet wired into `:app` navigation** — that + `pageSizePt`/image-pipeline/autosave-binder at the app/DI layer is the next step. | [ADR-029](DECISIONS.md#adr-029), [spike §10.10–§10.11](spikes/s4-editor-mvi.md) |
| 2026-06-27 | **S4 editor mounted in `:app`** (PR #23): single-Activity `ZinelyNavHost` on a fixed `"default"` project, `EditorViewModel`/`EditorBootstrap` (seed-on-miss + imposition-derived page size), autosave binder, and content-addressed asset store + interactive image import. | [ADR-030](DECISIONS.md#adr-030), [ADR-031](DECISIONS.md#adr-031) |
| 2026-06-28 | **Doc-truthfulness reconciliation** (Codex onboarding review GO-WITH-FIXES): corrected stale "no app UI / S2B-next" status and persistence/export overstatement across `README.md`, `ARCHITECTURE.md`, `ROADMAP.md`; aligned `AssetStore`/`core:data-storage` GC comments with the deferred-sweeper reality. No code behavior changed. | [review](reviews/2026-06-27-onboarding-review-claude-brief.md) |

> When phase contents change, add a row here and update the affected phase section + any new [ADR](DECISIONS.md).
