# Changelog

All notable changes to Zinely are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Pre-1.0 policy.** Zinely is pre-release. While the major version is `0`, the
> minor version (`0.y`) is bumped per completed **vertical-slice milestone** and the
> patch (`0.y.z`) per fix/refinement within one. The public surface is not yet stable.
> **`1.0.0` ships when a first-time user can create *and* export a real zine** — i.e.
> the [MVP exit criteria](docs/ROADMAP.md#mvp--one-great-format-done-right) are met.
> Each released `0.y.0` maps to a build phase in the [roadmap](docs/ROADMAP.md).

## [Unreleased]

### Added
- **S7.1 — choose your paper when you start a zine** ([ADR-047](docs/DECISIONS.md#adr-047)):
  **Start a zine** (empty-shelf CTA and shelf FAB alike) now asks *"What paper will you print
  on?"* — **Letter** (8.5 × 11 in) or **A4** (210 × 297 mm) — and creates the zine on the paper
  you pick; nothing is created until you choose, and "Not now" backs out. Ends the shelf's
  hardcoded Letter: the imposition, render, export, and project store have carried the paper
  size end-to-end since S1, so an A4 zine previews, exports, and folds correctly. The
  v0.6.0-alpha.1 scope (what ships now vs. post-alpha: text styling, layout presets,
  calibration ruler, asset GC) is recorded in [PRD §7.3](docs/PRD.md#73-alpha-release-scope--v060-alpha1-adr-047).

- **S6.5 — nav re-root: Home is the app** ([ADR-046](docs/DECISIONS.md#adr-046), the final
  S6 slice): `ZinelyNavHost` now starts at a new `HomeRoute` — the single back-stack root —
  hosting the S6.2–6.4 "My zines" shelf. A card tap (or **Start a zine**, which now creates
  *and opens* the new zine, single-flight) pushes `EditorRoute(id)` with `launchSingleTop`;
  returning is only ever a pop, and Completion's "Keep editing" still pops to the existing
  editor entry. A fast reopen of a just-closed zine no longer risks the spurious
  "Couldn't open" boot error: the editor bootstrap awaits the single-writer release through
  the same 5 s `AutosaveSessionGate` policy the shelf mutations use
  (`EditorAutosaveBinderFactory.awaitNoSession`; timeout ⇒ a warm "still saving" error).
  Leaving the shelf commits pending undoable deletes (leaving = snackbar dismissal; a failed
  commit un-hides the card and messages, and never blocks the open), and an open that lands
  while the shelf is away is discarded on return — navigation only ever follows a fresh tap.
  The shelf re-reads the store on every return (`WhileSubscribed(0)`), so recency labels and
  thumbnails are fresh after an editing session. `HomeScreen` gained an explicit `storeEmpty` signal so a shelf
  filtered to zero by pending deletes never shows the empty invitation. New host-level
  Robolectric nav tests (`TestNavHostController` + a debug-only `HiltTestActivity`) cover
  the whole back-stack policy — the graph's first.

- **S6.4 — Home shelf thumbnails (built on the unwired shelf)**
  ([ADR-045](docs/DECISIONS.md#adr-045)): each "My zines" card now shows a page-1
  miniature rendered through the proven parity path — the `SceneRenderer` tape replayed
  by the shared `CanvasReplayer` via a new thin `:render-android` `ThumbnailRenderer`
  (paper-white, 320 px longest edge, the export font/image stack) — so **a thumbnail is
  a miniature of the export by construction**. Thumbnails are produced pull-based on
  shelf observation by an `:app` `ShelfThumbnailProducer` (IO, one mutex, capped
  in-memory LRU) and cached as a **derived, never-authoritative** PNG at
  `cacheDir/thumbnails/<id>.png`, invalidated by a single stamp: the PNG's mtime is set
  to `document.json`'s mtime and validity is exact equality. Rename doesn't regenerate
  (content unchanged); duplicate renders fresh; any failure shows a warm paper
  placeholder — the shelf never breaks. New narrow `:data-android` seam
  `ProjectDocumentLayout.documentFile` (over the internal `ProjectPaths` chokepoint);
  ADR-031's no-sweeper invariant untouched (a thumbnail is never a GC root). Navigation
  unchanged — Home remains unwired until S6.5.
- **S6.3 — Home shelf actions: create · rename · duplicate · undoable delete
  (testable-only until S6.5)** ([ADR-044](docs/DECISIONS.md#adr-044)): "Start a zine"
  returns to the empty shelf (ending the ADR-043 §5 named deviation) and as a content-shelf
  FAB — one tap creates "My zine" (`SINGLE_SHEET_8` · `LETTER`, the bootstrap-seed defaults);
  each card gains an overflow menu with Rename (gentle **[ Keep name ] [ Rename ]** dialog,
  blank disabled, trimmed in the VM), Duplicate, and a confirm-less Delete with a snackbar
  undo window (the card hides instantly; Undo restores it with no store call; dismissal
  commits `deleteProject`; a failed commit un-hides the card — the shelf never lies). The
  **ADR-042 open-editor exclusion is now enforced inside `RoomProjectRepository`**: a
  `ProjectSessionGate` over `AutosaveCoordinatorFactory`'s new by-id `awaitReleased`
  (the ADR-030 Rec1 seam realised) gates rename/delete targets and the duplicate source;
  a session still live at the bound refuses with the new **`DataError.Busy`** ("That zine
  is still saving — try again in a moment."). Navigation is untouched: Home remains
  unwired, so every action is reachable only in tests until the S6.5 re-root.
- **S6.2 — Home · "My zines" read-only shelf, built-but-unwired**
  ([ADR-043](docs/DECISIONS.md#adr-043)): a stateless `HomeScreen` in `:feature:editor`
  (paper-card list — title, "8-page mini · Letter/A4", warm "Edited …" recency — plus a
  CTA-less empty-state invitation) and an MVVM `HomeViewModel` in `:app` observing
  `ProjectRepository.observeProjects()`. Deliberately **not registered in `ZinelyNavHost`**
  (Codex Required Fix): the app still boots into `EditorRoute("default")`, and no
  create/duplicate/delete/rename affordance exists anywhere (ADR-042 hard invariants).
  Shelf actions arrive in S6.3, thumbnails S6.4, navigation wiring + re-root S6.5.
- **S6.1 — Room-backed project store** ([ADR-042](docs/DECISIONS.md#adr-042)): the
  `ProjectRepository` contract gets its first real implementation in `:data-android` —
  an observable multi-project metadata index (Room `projects` table, schema exported)
  over the per-project files, which stay the source of truth (`document.json` + a new
  atomic `meta.json` sidecar for title/createdAt). Existing on-disk projects — including
  the S4 `"default"` seed — are adopted by an idempotent reconcile scan (no destructive
  migration; nav unchanged). Create/rename/duplicate/delete land file-first with the row
  re-derived; duplicates share content-addressed assets (new GC live-root by
  construction, no sweeper shipped). Data layer only — the Home/My-zines shelf arrives
  in S6.2+.
- Design language + onboarding philosophy as canonical pre-implementation design:
  [docs/design/DESIGN-LANGUAGE.md](docs/design/DESIGN-LANGUAGE.md) and refreshed
  interactive mockups under [docs/design/mockups/](docs/design/mockups/).
- Editor **empty-state invitation** — a cozy, encouraging first-run surface on a blank
  page with discoverable "add a photo" / "add words" supply actions (replaces the dead
  blank canvas; contextual guidance instead of hidden gestures). "Add words" places an
  empty text box and opens its editor immediately — straight to typing, no placeholder.

> These entries belong to the in-progress **0.5.0** (`SUX`) milestone. The completed
> editor/UI foundation below is **0.4.0** — tag that commit, not the SUX work-in-progress.

### Fixed
- **S7.0 — photo import works on real devices**
  ([ADR-031 §Review 2b](docs/DECISIONS.md#adr-031)): every on-device import failed with
  "That image couldn't be added." — `ImportMasterDecoder.readBounds` null-guarded the result
  of a bounds-only `BitmapFactory.decodeStream` (null **by contract**) instead of the stream.
  Fixed at the guard, with the decoder's first headless regression suite
  (`ImportMasterDecoderTest`, Robolectric NATIVE + fresh-stream-per-open shadow resolver).

### Removed
- **The `"default"` seed-on-miss bootstrap** ([ADR-030](docs/DECISIONS.md#adr-030) §4,
  retired by [ADR-046](docs/DECISIONS.md#adr-046) §3): the editor no longer silently
  creates a blank document for a missing id — `NotFound` is an honest boot error with a
  **Back to your shelf** action. First run lands on the empty shelf's **Start a zine**
  invitation instead of a pre-seeded editor. Existing installs keep their zine: the on-disk
  `"default"` project was already adopted as an ordinary shelf row by the ADR-042 reconcile
  (zero migration), and deleting it now really deletes it (no re-seed on next boot —
  the ADR-042 hard-invariant #1 and ADR-044 §3 delete-honesty arcs close).

### Changed
- Roadmap re-sequenced: the next milestone targets the **first-time creation experience**
  (onboarding, empty state, discoverability, supply tray, contextual hints) ahead of more
  editor power, per [ADR-008](docs/DECISIONS.md#adr-008). See [ROADMAP.md](docs/ROADMAP.md).

## [0.4.0] — 2026-06-28 — Editor foundation

The editor is mounted in the real app and every page of the zine is reachable.

### Added
- `:core:editor` — pure MVI reducer (intents, command/memento undo, hit-test, snap,
  z-order, transform math); fully unit-tested ([ADR-029](docs/DECISIONS.md#adr-029)).
- `:feature:editor` — interaction surface: store + effect runner, gesture pipeline,
  selection chrome with live document-order preview, opposite-anchor resize handles,
  live snap guides (preview == commit), accessibility context bar + element semantics
  (WCAG 2.5.7), race-safe text-edit session, host `EditorScreen`.
- `:app` — single-Activity navigation mounting the editor on a fixed `"default"` project,
  `EditorViewModel`/`EditorBootstrap` (seed-on-miss + imposition-derived page size),
  autosave binder, content-addressed asset store, and interactive image import
  ([ADR-030](docs/DECISIONS.md#adr-030), [ADR-031](docs/DECISIONS.md#adr-031)).
- **Scrapbook page navigator** — all eight pages reachable via a styled page strip
  (`Intent.GoToPage`); "workbench" theme foundation replacing the default template theme.
- Roborazzi selection-chrome goldens (CI-gated).

### Changed
- Canonical docs reconciled with the real checkout (truthful module/persistence/export
  state); `AssetStore`/`:core:data-storage` GC comments aligned with the deferred sweeper.

## [0.3.0] — 2026-06-25 — Rendering pipeline

One scene model, two backends, with proven preview/export parity.

### Added
- `:core:render` — pure scene → ordered draw-command tape (`SceneRenderer`,
  `computeImageBlit`); only depends on `:core:model`, zero Android
  ([ADR-027](docs/DECISIONS.md#adr-027)).
- `:render-android` — single `CanvasReplayer` + two export providers (PDF in PostScript
  points, raster @ 300 DPI), point-space `SharedTextLayout`, crop-aware `ImageBlitter`,
  bundled **Inter** font with an MVP-charset cmap coverage guard
  ([ADR-028](docs/DECISIONS.md#adr-028)).
- Headless-CI Roborazzi raster + text parity goldens; image + PDF write/parity proofs
  verified on-device (compile-checked in CI).

## [0.2.0] — 2026-06-20 — Persistence

Durable, offline-first, on-device storage.

### Added
- `:core:data` — versioned `@Serializable` document tree, `DocumentSerializer` +
  migration, validation, repository / `DataResult` contracts, content-addressed asset +
  `.zine` manifest contracts ([ADR-003](docs/DECISIONS.md#adr-003)).
- `:core:data-storage` — pure-JVM durability core: atomic file store
  (temp → fsync → atomic rename → dir-fsync + `.bak` recovery), autosave coordinator,
  content-addressed `FileAssetStore` ([ADR-021](docs/DECISIONS.md#adr-021),
  [ADR-022](docs/DECISIONS.md#adr-022), [ADR-025](docs/DECISIONS.md#adr-025)).
- `:data-android` — Android adapters: real `Os.fsync` file-system ops, file-only
  `DocumentRepository`, autosave coordinator factory + lifecycle binder, Hilt graph
  ([ADR-026](docs/DECISIONS.md#adr-026)).

### Changed
- `minSdk` ratified at **24** ([ADR-024](docs/DECISIONS.md#adr-024)); CI runs the
  pure-Kotlin core Android-free via `ZINELY_CORE_ONLY`.

### Deferred
- Room project metadata, `ProjectRepository`, and the asset GC sweeper remain unbuilt
  (persistence is currently file-only and single-project).

## [0.1.0] — 2026-06-19 — Imposition engine

The riskiest, most isolatable thing first: the math that makes a folded zine correct.

### Added
- `:core:model` — `ZineDocument` / `Page` / `Element` / geometry in physical points.
- `:core:imposition` — single-sheet 8-page mapping (panels + rotations, fold/cut guides,
  SVG proof sheet); pure Kotlin, golden-tested against the imposition oracle
  ([ADR-007](docs/DECISIONS.md#adr-007)). Tagged `v0.1.0-imposition-engine`.

[Unreleased]: https://github.com/aritr-codes/zinely-android/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/aritr-codes/zinely-android/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/aritr-codes/zinely-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/aritr-codes/zinely-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/aritr-codes/zinely-android/releases/tag/v0.1.0-imposition-engine
