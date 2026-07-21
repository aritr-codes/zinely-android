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

### Added — Style your text · [ADR-055](docs/DECISIONS.md#adr-055)

Implemented as B1–B4 (style intent · Type bar · keyboard + haptics · parity fix and doc reconciliation), each batch independently reviewed. Signed off on 2026-07-21: the pixel-parity goldens were recorded on the pinned CI image and reviewed against the frozen design, and the accessibility pass was run on a physical phone against this exact signed build — so [ADR-055](docs/DECISIONS.md#adr-055) is Accepted.

- **Text can be styled.** Select a text block and tap the new **Text style** (`Aa`) control to open the Type bar: set the **size**, the **alignment** (left / center / right), **bold**, **italic**, and the **colour** from the five Zinely text inks. The block updates live on the page — what you see is what prints.
- **Every change is one undo.** Any style change can be undone in a single step, including a whole run of size taps. There is no "apply" or "cancel" — undo is the cancel.
- **Keyboard.** `Ctrl/Cmd + B` and `Ctrl/Cmd + I` toggle bold and italic on a selected text block.

**Accessibility.** Every control is a real labelled button — no jargon dropdowns, no gesture-only affordances. Alignment announces as a single-select group, bold and italic as independent toggles, and each change is spoken. Every control is tappable well beyond its painted size, so the frozen layout is kept without shrinking any target: the steppers and toggles reach a full 48dp, and the closely-spaced colour swatches reach 48dp tall by at least 40dp wide. The Type bar buzzes on each accepted change (silent when reduced motion is on).

**Compatibility.** Existing zines are unaffected and open unchanged at their current styling — no file-format change, no migration. Offline/privacy invariants intact: no network, no account, no new dependency.

### Fixed — reopening a zine and adding to it could make it unopenable

Found during the on-device accessibility pass, on a physical phone, and fixed before the beta.

- **Adding an element to a reopened zine could permanently break it.** Every element gets an id, and the counter those ids come from was rebuilt from scratch each time a project was opened instead of continuing from what the project already contained. So the first photo or text box added after reopening a zine reused an id that was already in use. The result was silent at the time and fatal afterwards: the zine saved normally, and then would not open again — the editor showed "Couldn't open this project." from that point on, with no way back through the app. It also made the two elements sharing an id behave as one: both read as selected to a screen reader, and undo would have removed both.

  It is fixed at the source — the counter now starts past every id the zine already holds — so **an affected zine repairs itself the next time it is opened, with no file-format change and nothing for you to do**. A zine that was *already* saved with duplicate ids stays unopenable, because the damage is on disk rather than in the code; no beta tester can be carrying one, because the beta requires a fresh install (see the signing note in the release notes).

### Known limitations

- **Font choice is not in this milestone.** Text renders in the single bundled Inter family; choosing a font is planned for V1 and needs more families bundled first. Bold and italic use real bundled Inter faces, not synthesised ones.
- **Some text inks are low-contrast.** The five inks include authorial values (teal in particular) that fall below AA as body text on white. They are offered as-is; a beginner-safe default is a later call.
- **Non-Latin text still renders blank** — unchanged, and tied to the existing Latin-first font bundle.
- **Styling is per-block, not per-character.** A block has one size, colour, alignment, and weight; mixed styling inside one block is not supported.

## [0.8.0] — 2026-07-15 — Save to your phone

### Added — Save a copy to your phone · [ADR-054](docs/DECISIONS.md#adr-054)

Merged to `main` as B1–B4 (merge `7e2fa74`; Downloads backend · export-flow wiring · frozen Proof behaviour · doc reconciliation), each batch independently reviewed (GO / GO WITH FIXES, reconciled). Packaged as a debug-signed side-load build (a real release keystore is still deferred, per [ADR-047](docs/DECISIONS.md#adr-047)).

- **Save PDF writes to Downloads.** "Save PDF" now saves a permanent copy of your zine into the device's shared **Downloads** — one tap, no chooser — where it stays, visible in the Files/Downloads app, after Zinely closes. A confirmation names the file: _Saved “…” to Downloads_, with the "Fold now" hand-off.
- **Share is unchanged.** "Share" still sends a copy through the OS share sheet (`ACTION_SEND`) exactly as before.

**Accessibility.** Existing behaviour preserved — the confirmation stays a `role=status` announcement; focus, keyboard flow, and reduced-motion behaviour are unchanged.

**Compatibility.** API 29+ writes through MediaStore (no permission needed); API 24–28 uses the legacy public-Downloads File path. Offline/privacy invariants intact — no network, no account, no new dependency; Zinely writes only its own export file.

### Known limitations

- **API 24–28 asks for a storage permission.** On Android 7–9 the first save triggers a one-time runtime permission prompt (`WRITE_EXTERNAL_STORAGE`); later saves are one-tap. A denial routes to the existing "Couldn't make the PDF" surface.
- **Broad legacy storage permission.** On API ≤28 that permission grants broad shared-storage access rather than Downloads-scoped; Zinely only ever writes its own export file there.
- **Replace Picture UI still absent** — Reframe's replace-photo affordance is not yet wired (Future Enhancement).
- **Long-press context menu still absent** — the element long-press visual menu is not yet built (Future Enhancement).

## [0.7.0] — 2026-07-14 — Image Framing

### Added — Image Framing (Reframe) · [ADR-053](docs/DECISIONS.md#adr-053)

Merged to `main` as IF1–IF5 (final implementation commit `685f753`); device pixel-parity (P1–P5, M1) and accessibility semantics both re-verified on-device — PASS. Not yet in a packaged build.

- **Reframe a photo in place.** Double-tap a photo (or its "Reframe photo" action) to open a dedicated Reframe surface: pan and pinch-zoom the picture inside its fixed frame, over a live scrim + rule-of-thirds guide.
- **Fill / Whole photo fit control.** Fill (crop-to-cover) is the new default for newly placed photos; "Whole photo" fits the whole image with margins. Existing documents are byte-identical — new placements only.
- **One reframe = one undo.** A whole Reframe session bakes to a single undoable edit; leaving the panel commits, Cancel discards, switching pages commits then navigates. **Reset framing** returns a photo to its placement default.

**Accessibility.** Every Reframe control is labeled and reachable in a logical traversal order; the fit toggle exposes selected state; zoom exposes a spoken readout; a full hardware-keyboard path (arrows nudge, Shift = coarser, +/− zoom, Enter save, Esc cancel) drives the same actions as touch; announcements force-speak on repeat and the first-run coach-mark respects reduced-motion.

**Implementation.** Framing persists via the existing `ImageElement.crop`/`fit` — no `:core:model` or `:core:render` change; the live draft is preview-only and never enters the reducer mid-gesture (`preview == commit` by shared pure math). The visual surface implements the DESIGN-FROZEN [`bench.html`](docs/design/v1/bench.html), re-verified on-device (pixel-parity P1–P5/M1 and accessibility semantics both PASS).

**Compatibility.** Offline/privacy invariants intact — no network, account, or new dependency; no document-format migration (existing zines open unchanged; only new photos default to Fill).

_Next up: post-alpha S7.x — save-a-copy export landed (see [Unreleased]); text styling next._

## [0.6.0-alpha.1] — 2026-07-07 — First installable alpha

The first build handed to early testers: the full create → edit → export → print spine.
Bundles the never-separately-released **0.5.0** (`SUX` first-time creation) and **0.6.0**
(S5 export/share + S6 project layer & Home) milestone work plus the S7 alpha-push slices.
Gate evidence: the physical print/fold test passed (printed at 100%, folded, 1→8 order/
rotation/scale correct), and the "text missing in preview" field report was triaged to the
known [ADR-028](docs/DECISIONS.md#adr-028) Latin-first charset limitation (see below) —
full triage in [the release assessment](docs/reviews/2026-07-04-alpha-release-assessment.md).

### Known limitations (alpha)

- **Export is share/open-only — no copy is saved to your phone yet.** "Print at home" and
  "Save as image" hand the finished sheet to the app you pick (share sheet / viewer); Zinely
  writes only a temporary internal file, nothing appears in your gallery or Downloads unless
  the app you share to saves it. A real "Save to your phone" is the first post-alpha slice
  ([ADR-039](docs/DECISIONS.md#adr-039) deferral).
- **Text is English-first Latin only.** The bundled font covers the
  [MVP charset](docs/DECISIONS.md#adr-028) (ASCII + Latin-1 letters + common punctuation);
  other scripts (e.g. Bengali, Hindi, CJK) and emoji may render blank or degraded.
- **The editor page sits left with a gap on the right** on some screens (page is fit
  top-left, not centred). Cosmetic only — preview, export, and print are unaffected.
- **Alpha builds are debug-signed**: a future properly-signed build will require
  uninstall + reinstall (projects on the device are lost).
- **App storage grows with every photo import** (replaced/deleted photos are not yet
  reclaimed — [ADR-031 §2](docs/DECISIONS.md#adr-031)).
- Print at **100% / Actual size** — printer "fit to page" breaks the fold alignment
  (the in-app note says the same).
- Alpha scope vs the MVP (single-style text, no fit/fill control, no layout presets, no
  calibration ruler): [PRD §7.3](docs/PRD.md#73-alpha-release-scope--v060-alpha1-adr-047).

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

> The `SUX`-era entries above were built under the **0.5.0** milestone; the alpha is the
> first release that ships them.

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

[Unreleased]: https://github.com/aritr-codes/zinely-android/compare/v0.8.0...HEAD
[0.8.0]: https://github.com/aritr-codes/zinely-android/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/aritr-codes/zinely-android/compare/v0.6.0-alpha.1...v0.7.0
[0.6.0-alpha.1]: https://github.com/aritr-codes/zinely-android/compare/v0.4.0...v0.6.0-alpha.1
[0.4.0]: https://github.com/aritr-codes/zinely-android/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/aritr-codes/zinely-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/aritr-codes/zinely-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/aritr-codes/zinely-android/releases/tag/v0.1.0-imposition-engine
