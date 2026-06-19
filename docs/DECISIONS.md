# Zinely — Architecture Decision Records (ADRs)

> **The single authoritative home for every significant decision.** If a decision is referenced anywhere else (ARCHITECTURE, PRD, ROADMAP, code comments), it links *here* by ID — it is never re-decided elsewhere. See the **Documentation Rule** in [/CLAUDE.md](../CLAUDE.md).

**How to use this log**
- One ADR per decision. Never edit an Accepted ADR's decision in place — **supersede** it with a new ADR and set the old one's status to `Superseded by ADR-NNN`.
- Status: `Proposed` → `Accepted` → (`Superseded` | `Deprecated`).
- Major decisions (architecture, storage, rendering, export, editor, data model) follow the **Review Workflow**: propose → Codex review → reconcile → record here. The Codex outcome is noted in the ADR.
- Evidence lives in [RESEARCH.md](RESEARCH.md); cite the relevant `R#` section rather than restating sources.

### Index
| ID | Decision | Status |
|---|---|---|
| [ADR-001](#adr-001) | Native `PdfDocument` for MVP PDF export (no third-party lib) | Accepted |
| [ADR-002](#adr-002) | MVP scope = home-print-ready, not commercial prepress | Accepted |
| [ADR-003](#adr-003) | Storage = Room metadata + serialized JSON document blob | Accepted |
| [ADR-004](#adr-004) | Imported photos copied-in via Photo Picker (no URI references) | Accepted |
| [ADR-005](#adr-005) | Editor = MVI + command-based undo with field-level mementos | Accepted |
| [ADR-006](#adr-006) | One shared scene renderer, two backends (preview + export) | Accepted |
| [ADR-007](#adr-007) | Pure-Kotlin imposition engine; canonical NASA/Chandra layout; SVG proof sheet | Accepted |
| [ADR-008](#adr-008) | Beginner-first UX with progressive disclosure | Accepted |
| [ADR-009](#adr-009) | Durability = autosave + atomic rename + SAF `.zine` backup; no cloud | Accepted |
| [ADR-010](#adr-010) | Free, no monetization for MVP; bundle only license-clear fonts | Accepted |
| [ADR-011](#adr-011) | Raster export = ARGB_8888 @300 DPI, decode-to-target, EXIF-normalize | Accepted |
| [ADR-012](#adr-012) | Print correctness = exact paper size, safe-area inset, calibration ruler | Accepted |
| [ADR-013](#adr-013) | App architecture = Clean + single Activity + Compose + Hilt | Accepted |
| [ADR-014](#adr-014) | Geometry & transform public-API stability (finite guarantees, `times()` visibility) | Proposed |
| [ADR-015](#adr-015) | Validation result contract (`List<ValidationIssue>` vs `ValidationResult`) | Proposed |
| [ADR-016](#adr-016) | Format & paper extensibility (closed enums vs open specs) | Proposed |
| [ADR-017](#adr-017) | Bleed, clip & safe-area semantics | Proposed |
| [ADR-018](#adr-018) | Imposition convention & guide-ID versioning | Proposed |
| [ADR-019](#adr-019) | Domain-layer threshold — no `:core:domain` until ViewModels outgrow repositories | Accepted |
| [ADR-020](#adr-020) | Document serialization = kotlinx JSON behind a swappable `DocumentSerializer` | Accepted |
| [ADR-021](#adr-021) | Autosave timing & durability policy | Proposed |
| [ADR-022](#adr-022) | Asset GC & ownership model (ref-count + deferred undo-safe sweep) | Proposed |
| [ADR-023](#adr-023) | Asset fidelity — store one export-capped import master, derive tiers | Proposed |

> ADR-014 to ADR-018 are **follow-ups surfaced by the [ADR-007](#adr-007) release-candidate audit** (2026-06-19): rationale/risks/future only, no decision, no engine change.
> ADR-019 to ADR-023 resolve the **S2 open questions O1–O5** from the [data-storage spike](spikes/data-storage-layer.md#8-open-questions--candidate-adrs); each records alternatives, tradeoffs, and a recommendation, was Codex-reviewed, and is Accepted where justified.

---

## ADR-001 {#adr-001}
**Native `android.graphics.pdf.PdfDocument` for MVP PDF export — no third-party PDF library.**
- **Status:** Accepted (2026-06-19)
- **Context:** We need print-quality, on-device, offline PDF export. Question was whether the platform API gives crisp vector text or whether we need iText/PdfBox-Android.
- **Decision:** Use `PdfDocument` + `Canvas`. Evidence ([R2.2](RESEARCH.md#r22-androidgraphicspdfpdfdocument--verified)) confirms its Skia backend emits **true vector, selectable text with embedded subset fonts** — sufficient for home print. No third-party lib in MVP.
- **Consequences:** Zero added bundle weight / license risk; fully offline. Hard ceiling: sRGB only, no Trim/Bleed boxes — see ADR-002. Bundle our own licensed TTFs for reproducible output.
- **Review:** Cross-model (Codex) review of the architecture flagged exactly this boundary; research corroborated. Reconciled: native is correct for MVP.

## ADR-002 {#adr-002}
**MVP scope is "home-print-ready," not "commercial print-ready."**
- **Status:** Accepted (2026-06-19) · user-confirmed
- **Context:** "Print-ready" is overloaded. `PdfDocument` cannot produce CMYK/spot/ICC color, PDF/X, or Trim/Bleed boxes ([R2.2](RESEARCH.md#r22-androidgraphicspdfpdfdocument--verified)).
- **Decision:** MVP targets correct printing on a consumer inkjet/laser at 100% scale. We never market "commercial print-ready." Prepress (CMYK/ICC/PDF-X, bleed, crop marks) is 🔭 FUTURE and likely off-device — which conflicts with offline-first and is therefore explicitly deferred.
- **Consequences:** Honest claims; lean MVP. Print-shop creators are a post-MVP audience. Scope tracked in [PRD.md](PRD.md).

## ADR-003 {#adr-003}
**Storage = Room metadata table + a versioned serialized JSON document blob per zine (not a fully relational element tree).**
- **Status:** Accepted (2026-06-19)
- **Context:** The zine document is a page→element tree with transforms. Options: relational (Room-tree) vs metadata + serialized blob ([R4.1](RESEARCH.md#r41-architectures-compared--verified)).
- **Decision:** Room holds queryable **metadata** (id, title, timestamps, paperSize, format, thumbnail, documentPath, schemaVersion). The **document** is `kotlinx.serialization` JSON in a per-project file. JSON over Protobuf for MVP: human-readable, debuggable, schema-version via optional/defaulted fields + `ignoreUnknownKeys`.
- **Consequences:** Clean 1:1 in-memory↔on-disk model that maps to Compose state, undo, and single-file export; whole-file rewrite per save (acceptable at zine scale). Document schema versioned independently of Room. **Protobuf** and **hybrid/relational** are 🔭 FUTURE if write-amplification or cross-document search ever dominates.
- **Review:** Research-backed ([R4.2](RESEARCH.md#r42-recommendation--recommendation)). To get a Codex pass before the data-model spike.

## ADR-004 {#adr-004}
**Imported photos are copied into app-specific storage; selection via the Photo Picker. No retained MediaStore URIs.**
- **Status:** Accepted (2026-06-19)
- **Context:** Referenced URIs break when the user moves/deletes the source and complicate backup ([R4.4](RESEARCH.md#r44-image-import--verified--recommendation)).
- **Decision:** Copy-in on import (no storage permission needed), dedupe by content hash, generate thumbnails, clean orphans, show per-project storage size. Select images with the **Photo Picker** (permission-free).
- **Consequences:** Self-contained, durable, privacy-strong projects; higher disk use (mitigated by dedupe + visible size). Honors "photos never leave the device."

## ADR-005 {#adr-005}
**Editor uses MVI with command-based undo/redo carrying field-level mementos; gestures coalesce via begin/update/commit.**
- **Status:** Accepted (2026-06-19)
- **Context:** The direct-manipulation editor (move/resize/rotate, snapping, z-order) is the highest-complexity component. Undo design must be right from day one ([R5.1](RESEARCH.md#r51-undoredo--verified), [R5.3](RESEARCH.md#r53-coalescing-a-drag-into-one-undo-step--verified)).
- **Decision:** Single immutable `EditorState` + pure reducer. Undo unit = **command objects** whose inverse stores only touched fields (the tldraw/Excalidraw/Figma hybrid), **not** per-frame snapshots. A drag = one command via `onDragStart`/`onDrag` (preview only) / `onDragEnd` (commit). Live transforms run through a `Modifier.graphicsLayer{}` lambda so frame updates skip the reducer. Snapping & hit-testing are pure functions outside history.
- **Consequences:** Atomic semantic undo at low memory; 60–120fps drags; testable geometry. Rest of app stays MVVM (ADR-013).
- **Review:** Research-backed; flag for Codex before the editor spike.

## ADR-006 {#adr-006}
**One shared scene renderer with two backends (Compose preview + Android `Canvas` → PDF/Bitmap); text driven through the same Android text path in both.**
- **Status:** Accepted (2026-06-19)
- **Context:** Preview↔export divergence (especially text layout) is a classic WYSIWYG bug class. A "shared renderer" only holds if text metrics/wrapping are shared too.
- **Decision:** A pure scene→draw-command function feeds both backends; coordinates in **points**. Text is measured/drawn via the **same Android `StaticLayout`/`Paint` path** in preview and export (rendered into Compose via `drawIntoCanvas`). Verified by Roborazzi diff tests (preview vs export of the same page).
- **Consequences:** WYSIWYG by construction. Slightly more plumbing for text in preview. Codex flagged this caveat in the architecture review; reconciled by mandating the shared text path.

## ADR-007 {#adr-007}
**Imposition is a pure-Kotlin, zero-Android module; canonical layout = NASA/Chandra convention (top row rotated 180°, cover at grid (1,3)); printer-free validation via an SVG proof sheet.**
- **Status:** Accepted (2026-06-19)
- **Context:** Wrong imposition = every printed zine is wrong — the #1 correctness risk. Multiple template conventions exist ([R1](RESEARCH.md#r1-imposition-geometry--single-sheet-8-page-mini-zine)).
- **Decision:** Pure-Kotlin engine, fully unit-testable, no Android deps. Adopt the VERIFIED oracle in [R1.2](RESEARCH.md#r12-page--cell-mapping-the-oracle--verified) as the golden test (pages 2–5 → 180°; 1,6,7,8 → 0°). Expose a single convention flag (default canonical). Emit a **pure-Kotlin SVG proof sheet** (panel numbers, logical pages, orientation arrows, fold + cut guides) so fold logic is validated in a browser without a printer.
- **Consequences:** Highest-risk logic is isolated, testable, and reviewable. Full design in [spikes/imposition-engine.md](spikes/imposition-engine.md).
- **Review (completed 2026-06-19):** Codex confirmed the geometry + mapping are **correct**. Accepted refinements, folded into the spike: (a) emit an explicit per-panel `contentToSheet` affine transform + panel-local safe/clip rects so the consumer never re-derives rotation (rotate-about-center contract); (b) make the rotation rule **convention-scoped** via a named `ConventionSpec`, not a format-universal invariant; (c) **drop the under-specified `BOTTOM_ROW_ROTATED` mode** from v1 (any alternate needs its own spec + goldens); (d) model the cut as lying **on** the horizontal fold (`onFoldId`) and validate topology, not just geometry; (e) deterministic, locale-independent SVG; configurable safe inset; bleed explicitly out of scope. No open disagreements. See the [reconciliation table](spikes/imposition-engine.md#review--codex-critical-review-reconciled-2026-06-19).
- **Implementation (completed 2026-06-19):** Built test-first on `feat/imposition-engine` as pure-Kotlin `:core:model` + `:core:imposition` (zero Android deps, `explicitApi()`, JDK 21). Five phases, each Codex-reviewed and reconciled: (1) geometry + `AffineTransform2D` + enums; (2) `SingleSheet8Imposer` (panel bounds, `contentToSheet`, fold/cut geometry); (3) `LayoutValidator` — strengthened after review to an **independent** transform check (corner-mapping, not engine-formula recompute), exact `1..N` page set, safe-area **margin** check, `clip == panel`, and full fold/cut topology; (4) `SvgProofSheetRenderer` (each panel placed via an SVG `matrix(...)` built from `contentToSheet`; `BigDecimal` plain-decimal formatting); (5) edge-case + jqwik property + committed golden SVG tests. **95 tests, all green.** Matrix math and the R1.2 oracle were independently confirmed correct by Codex at every phase.

## ADR-008 {#adr-008}
**Beginner-first UX with progressive disclosure.**
- **Status:** Accepted (2026-06-19) · user-confirmed
- **Decision:** Default surface is dead-simple (tap-to-place, few options, templates lead). Power features are revealed progressively, not removed. Resolves the "simple vs powerful" tension toward simplicity first.
- **Consequences:** Drives editor IA and onboarding in [PRD.md](PRD.md). Power-creator depth phases in per [ROADMAP.md](ROADMAP.md).

## ADR-009 {#adr-009}
**Data durability = autosave + write-temp-then-atomic-rename + user-initiated `.zine` backup/restore via SAF. No cloud.**
- **Status:** Accepted (2026-06-19) · user-confirmed
- **Context:** No cloud means device loss = data loss unless we make local durability first-class ([R4.3](RESEARCH.md#r43-crash-safety--verified), [R4.5](RESEARCH.md#r45-saf-backup--scoped-storage--verified)).
- **Decision:** Debounced autosave writing to a temp file then atomic-renaming over the good file; Room WAL for metadata; "restore unsaved changes" on relaunch. User-controlled backup/restore as a self-contained `.zine` zip (document + images) via `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`. Autosave/recovery in MVP; `.zine` backup in V1.
- **Consequences:** Crash-safe local-first storage; portability without a hosted service. Honors "no cloud storage."

## ADR-010 {#adr-010}
**Free, no monetization for MVP; bundle only license-clear fonts/assets.**
- **Status:** Accepted (2026-06-19) · user-confirmed
- **Decision:** No billing, ads, or paywall in MVP. Bundle only OFL/clearly-licensed fonts. Revisit monetization later.
- **Consequences:** Fits the privacy/indie ethos; no billing surface to build. Custom-font import deferred to V2 ([ROADMAP.md](ROADMAP.md)).

## ADR-011 {#adr-011}
**Raster export = ARGB_8888 at 300 DPI, decode-to-target, EXIF-normalized, recycled; PNG for line-art / JPG for photo-heavy.**
- **Status:** Accepted (2026-06-19)
- **Context:** A full sheet is ~33–35 MB ARGB; OOM risk on low-heap devices ([R2.6](RESEARCH.md#r26-raster-export-at-300-dpi--memory--verified)).
- **Decision:** Letter 2550×3300 / A4 2480×3508. Decode user images bounds-first (`inJustDecodeBounds` + `inSampleSize`) to placement size; normalize EXIF orientation; stream `compress()` to the `OutputStream`; `recycle()` immediately; one sheet at a time off-main-thread. PNG default for flat art, JPG for photo. `RGB_565` only when no alpha/gradients.
- **Consequences:** Predictable memory; correct orientation; crisp output.

## ADR-012 {#adr-012}
**Print correctness = export at exact paper size + safe-area inset (~6 mm / 0.25") + on-sheet calibration ruler + "Actual size, Fit-to-page OFF" guidance.**
- **Status:** Accepted (2026-06-19)
- **Context:** Driver "Fit to page" silently rescales fixed geometry; consumer printers can't print to the edge ([R2.4](RESEARCH.md#r24-fit-to-page-silently-breaks-imposition--verified), [R2.5](RESEARCH.md#r25-non-printable-margins--full-bleed--verified--recommendation)).
- **Decision:** Export each paper size at its exact dimensions (no cross-size reliance); keep all content, fold lines, and cut marks inside a ~6 mm safe inset; print a 1 in / 50 mm calibration ruler; surface explicit "print at 100% / Actual size, turn Fit-to-page OFF" instructions in the export flow. Full-bleed is a 🔭 FUTURE advanced option.
- **Consequences:** Folds/cuts land correctly on real home printers; white border accepted as correct behavior.

## ADR-013 {#adr-013}
**App architecture = Clean architecture + repository pattern + single Activity + Compose/Material 3 + Hilt(KSP) + type-safe Navigation Compose; MVVM everywhere except the MVI editor (ADR-005).**
- **Status:** Accepted (2026-06-19)
- **Decision:** Unidirectional data flow (Compose → ViewModel → repository → data sources). `StateFlow` UI state, injected `CoroutineDispatcher`s, `Result<T>` error boundary. Pure-Kotlin `core` modules isolated from Android. Full technical detail in [ARCHITECTURE.md](ARCHITECTURE.md).
- **Consequences:** Standard, testable, modularization-ready. MVI scoped to the editor where state-transition density justifies it.

## ADR-014 {#adr-014}
**Public-API stability rules for the geometry & affine-transform layer (`core:model`).**
- **Status:** Proposed (2026-06-19) — raised by the [ADR-007 RC audit](#adr-007).
- **Context / rationale:** `core:model` is consumed *unmodified* by the future Android render and PDF/raster export layers, so its public surface is effectively an SDK boundary once those land. Two asymmetries exist today: (1) `PtPoint`/`PtRect`/`PtSize` reject NaN/∞ at construction but `AffineTransform2D` does not, so a hand-built transform can carry invalid numeric state into validation/render paths; (2) `AffineTransform2D.times()` and the raw `a..f` fields are `public`, widening the surface more than the consumer contract (`map`, `then`, `contentToSheet`) strictly requires.
- **Options under consideration:** (a) add finite-value `require` to `AffineTransform2D` to match the primitives; (b) keep raw `a..f` public (Android/SVG/PDF consumers need matrix values) but consider `internal` for `times()`; (c) introduce an explicit `@since`/binary-compatibility policy before the first non-spike consumer.
- **Risks:** Deferring (a) lets invalid transforms reach renderers as silent garbage rather than a fail-fast error. Tightening visibility *after* a consumer ships is a breaking change, so the window to decide is before S3 (render pipeline).
- **Future considerations:** Revisit when `core:render` ([ADR-006](#adr-006)) is designed; that is the first real external consumer and the natural forcing function. The imposition engine itself only ever emits finite transforms, so this is not a correctness issue for the merged spike.

## ADR-015 {#adr-015}
**Return contract of `LayoutValidator` — raw issue list vs a structured result.**
- **Status:** Proposed (2026-06-19) — raised by the [ADR-007 RC audit](#adr-007).
- **Context / rationale:** `LayoutValidator.validate()` returns `List<ValidationIssue>` (empty = sound). The export pipeline will *gate* on this (block export when invalid, possibly warn-but-allow for non-fatal issues). A bare list pushes "is this fatal?" logic onto every caller and has no place for summaries, counts, or an explicit `isValid`.
- **Options under consideration:** keep `List<ValidationIssue>` (simple, machine-readable, already deterministic-ordered); or wrap in `ValidationResult(isValid, errors, warnings, issues)` once `Severity.WARNING` is actually used (today every issue is `ERROR`).
- **Risks:** Changing the return type after the export layer consumes it is breaking. The `Severity.WARNING` enum value is currently unused — a latent "we meant to distinguish these" that should be resolved deliberately, not by accretion.
- **Future considerations:** Decide alongside the export-gating design (S5) or whenever the first `WARNING`-severity check is introduced — whichever comes first.

## ADR-016 {#adr-016}
**Extensibility of paper sizes and zine formats — closed enums vs open specs.**
- **Status:** Proposed (2026-06-19) — raised by the [ADR-007 RC audit](#adr-007).
- **Context / rationale:** `PaperSize` (Letter, A4) and `ZineFormat` (`SINGLE_SHEET_8`) are enums — a closed world. This is correct and safe for the MVP's "one great format" scope, but it forecloses custom paper (e.g. Legal, B5, user-defined) and new formats (4-page, 16-page saddle-stitch — [ROADMAP V2](ROADMAP.md#v2--more-formats--expression)) without a source change to the core model.
- **Options under consideration:** keep enums through MVP/V1; or introduce open `PaperSpec(widthPt, heightPt)` / `FormatSpec(rows, cols, pageCount, convention)` value types when the second format lands, with the enums kept as canonical presets.
- **Risks:** Premature generalization adds surface and validation burden now for value not realized until V2. Conversely, retrofitting open specs after several call sites assume the enum is churn. The `Imposer.convention: ConventionSpec` shape (one named convention per imposer) is workable but not elegant for multi-convention families.
- **Future considerations:** Force the decision when implementing the **second** imposition format (V2 16-page saddle-stitch is the likely trigger); single-format MVP does not need it.

## ADR-017 {#adr-017}
**Bleed, clip, and safe-area semantics.**
- **Status:** Proposed (2026-06-19) — raised by the [ADR-007 RC audit](#adr-007).
- **Context / rationale:** `LayoutValidator` currently hard-enforces `clipLocalBounds == panelLocalBounds` because this engine renders the full panel with no bleed ([ADR-012](#adr-012) keeps content inside a safe inset; full-bleed is explicitly 🔭 FUTURE). The `clipLocalBounds` field exists to *future-proof* for bleed, but its meaning under bleed is undefined: bleed content extends **past** the trim/panel edge, so `clip` would become larger than `panelLocalBounds`, inverting today's invariant.
- **Options under consideration:** define the invariant chain under bleed as `safe ⊆ panel ⊆ clip` (clip grows outward by the bleed amount) and relax the validator accordingly; specify how trim/crop marks interact with the safe inset and the cut line.
- **Risks:** Introducing bleed is a **deliberate contract change**, not an additive field — the `clip == panel` validation must be loosened in lockstep or it will reject every bleed layout. Getting the trim-vs-bleed-vs-safe relationship wrong reintroduces the #1 print-correctness risk the engine was built to retire.
- **Future considerations:** Resolve when V2 print-shop export groundwork (bleed, trim/crop marks) is scheduled ([ROADMAP V2](ROADMAP.md#v2--more-formats--expression)). No change to the MVP engine.

## ADR-018 {#adr-018}
**Versioning & stability of imposition convention names and fold/cut identifiers.**
- **Status:** Proposed (2026-06-19) — raised by the [ADR-007 RC audit](#adr-007).
- **Context / rationale:** The convention is keyed by name (`"TOP_ROW_ROTATED"`) and fold/cut guides by string id (`"H-center"`, `"V-quarter-1"`, `onFoldId`). These are an implicit contract: a stored `.zine` document, a golden test, or a render layer may key off them. There is no declared policy for how they evolve (rename, add, deprecate) without breaking persisted documents or downstream consumers.
- **Options under consideration:** treat convention names and guide ids as **stable, append-only identifiers** with a documented registry; or version the `ConventionSpec` (e.g. a `version` field) so persisted layouts can be re-resolved across changes.
- **Risks:** If a stored document or template references `"TOP_ROW_ROTATED"` and the name later changes, old projects break — a data-durability regression against [ADR-009](#adr-009). Golden tests already pin the current strings, which is good, but pinning ≠ a stability guarantee.
- **Future considerations:** Decide before any convention name/id is exposed in a persisted `.zine` schema (S2 storage layer) or a user-facing template — that is the first point where the identifier becomes a durability obligation.

## ADR-019 {#adr-019}
**Domain-layer threshold: no `:core:domain` for the MVP; repository interfaces live in `:core:data` and graduate to a domain module only when ViewModel logic outgrows them. (Resolves S2 open question O1.)**
- **Status:** Accepted (2026-06-19)
- **Context:** Clean architecture ([ADR-013](#adr-013)) permits a `:core:domain` layer (use cases + repository interfaces). For an offline, beginner-first MVP most "use cases" would be thin pass-throughs to a repository, so the layer may be ceremony before it earns its keep.
- **Alternatives considered:**
  - **A — No `:core:domain`; interfaces in `:core:data`** *(recommended)*: ViewModels depend on repository interfaces defined in `:core:data`. Fewest modules; least boilerplate. *Risk:* ViewModels can accrete business logic that should live in use cases.
  - **B — Full `:core:domain` now**: use cases + interfaces from day one. Clean separation, but most use cases are trivial wrappers today → boilerplate without payoff.
  - **C — Interfaces in `:core:model`**: keeps interfaces "central," but `:core:model` must stay a pure, dependency-free data module; repository interfaces drag in `Result`/coroutine concerns and muddy that purity.
- **Tradeoffs:** A optimizes for present simplicity and is **reversible** (extracting a domain module later is mechanical); B optimizes for a separation we don't yet need; C violates the model-purity invariant.
- **Recommended direction:** **A.** Introduce `:core:domain` only when **multiple ViewModels duplicate the same orchestration/business rules**, or a ViewModel's logic spans multiple repositories with no natural home. That duplication is the explicit extraction trigger (refined per Codex review — the signal is cross-ViewModel duplication, not a single fat ViewModel alone).
- **Consequences:** Fewer modules for S2–S4; watch cross-ViewModel rule duplication as the signal to extract. No impact on the shipped engine.
- **Review (2026-06-19):** Codex — **promote to Accepted**; sound for a beginner-first MVP, matches the `:core:data` boundary, reversible. Adopted its refinement of the extraction trigger.

## ADR-020 {#adr-020}
**Document serialization = `kotlinx.serialization` JSON, accessed only through a single `DocumentSerializer` interface so the wire format is swappable. (Resolves O2.)**
- **Status:** Accepted (2026-06-19)
- **Context:** The zine document tree ([ADR-003](#adr-003)) is persisted as a file blob. Format choice affects debuggability, schema evolution, size/speed, and future KMP.
- **Alternatives considered:**
  - **A — kotlinx JSON behind a `DocumentSerializer` interface** *(recommended)*: human-readable, diff-friendly, tolerant evolution (`ignoreUnknownKeys`), KMP-ready; the interface isolates the one swap point.
  - **B — Protobuf now**: compact and fast, but binary (hard to debug/inspect), rigid schema, and overkill for MVP document sizes.
  - **C — Moshi/Gson JSON**: reflection-based, weaker sealed-class/`@SerialName` support, not multiplatform.
  - **D — kotlinx JSON called directly (no interface)**: simplest, but couples every call site to the format, making a later swap a wide refactor.
- **Tradeoffs:** JSON trades bytes/speed for readability, debuggability, and painless additive evolution — the right priorities pre-1.0; the interface (A vs D) costs one indirection but buys a clean escape hatch to Protobuf if write-amplification is ever measured ([ADR-014](#adr-014)/[risks R5](spikes/data-storage-layer.md#7-risks--mitigations)).
- **Recommended direction:** **A.** kotlinx JSON now; all (de)serialization through `DocumentSerializer`; revisit format only with profiling evidence.
- **Boundary caveat (per Codex):** the `DocumentSerializer` boundary makes **call sites** swappable, not the persisted schema. A later Protobuf must still carry a **format marker**, retain a **legacy-JSON reader**, and own format detection/migration. Therefore: design `DocumentMigrator`s to operate on **typed/canonical document versions**, not raw `JsonElement` trees — otherwise the Protobuf escape hatch is cosmetic. The serializer (not call sites) owns legacy-format detection.
- **Consequences:** Debuggable documents; one swap point; KMP-aligned with the pure core. Migrators stay format-neutral.
- **Review (2026-06-19):** Codex — **promote to Accepted**; JSON is correct pre-1.0 for inspectability/fixtures/iteration. Adopted the boundary caveat (canonical-version migrators + serializer-owned legacy detection) so the swap claim is real, not cosmetic.

## ADR-021 {#adr-021}
**Autosave = per-edit debounce + hard flush on lifecycle stop + a max-latency cap during continuous editing, with a single-writer guarantee. (Resolves O3.)**
- **Status:** Proposed (2026-06-19)
- **Context:** [ADR-009](#adr-009) mandates no lost work + crash safety + no main-thread jank. The timing policy decides the worst-case data-loss window and the write-amplification cost.
- **Alternatives considered:**
  - **A — Debounce (reset per edit) + flush on `ON_STOP`/`onPause` + max-latency cap + single-writer** *(recommended)*: coalesces bursts; bounds loss to one debounce window; guarantees a save when backgrounded; the cap prevents "never saves during a long continuous edit."
  - **B — Save on every change**: zero loss window but write-amplifies and janks on large documents.
  - **C — Save only on background/close**: cheap, but a crash mid-session loses everything since open.
  - **D — Fixed periodic timer only**: may fire mid-keystroke or too rarely; ignores edit cadence.
- **Tradeoffs:** A is the standard editor compromise — small added complexity (debounce + mutex) for a low write cost. **Honest loss bound:** the true worst case is "everything since the **last completed save**," not "one debounce window" — if the process is killed during a pending/in-flight flush, that debounce's edits are lost. `ON_STOP`/`onPause` is **best-effort**, not a process-death guarantee, and abrupt death has no callback. The **constants** (~600 ms debounce, ~10 s max-latency) are empirical/tunable.
- **Recommended direction:** **A** for the *coalescing* policy, with the interval/cap as named tunables validated on a low-end device.
- **Open durability contract (why this stays Proposed):** before Accept, define — (1) the precise loss bound we promise the user; (2) lifecycle-flush *completion* semantics (await the in-flight save on `ON_STOP`?); (3) cancellation behavior of a superseded debounce; (4) whether "never lose work" requires a tiny **write-ahead op-log / dirty journal** (durable "pending edit" record written synchronously) in addition to the debounced full-document save. If yes, the journal — not the debounce — is the durability mechanism and the debounced save is an optimization.
- **Consequences (provisional):** requires a per-project single-writer mutex and an immutable snapshot at save time, decoupled from MVI command application ([ADR-005](#adr-005)).
- **Review (2026-06-19):** Codex — **KEEP Proposed.** Direction is right but the durability claim was too strong; `ON_STOP` does not survive low-memory kill mid-flush. Resolve the durability contract (loss bound, flush completion, journal-or-not) before Accept. A small op-log may be required to honor "never lose work" ([ADR-009](#adr-009)).

## ADR-022 {#adr-022}
**Asset garbage collection = reference counting + a deferred sweep with a grace window (so undo can resurrect a removed element), plus periodic disk/table reconciliation. Never inline-delete asset bytes. (Resolves O4.)**
- **Status:** Proposed (2026-06-19)
- **Context:** Images are copied in and content-addressed ([ADR-004](#adr-004)); removing an element or project drops a reference. Deleting bytes eagerly conflicts with undo ([ADR-005](#adr-005)), which can restore a "deleted" element.
- **Alternatives considered:**
  - **A — Ref-count + deferred WorkManager sweep (grace window) + reconciliation** *(recommended)*: `refCount == 0` assets are deleted only after a grace boundary (undo cleared / project close), off the hot path, crash-safe; a reconciliation pass fixes disk/table drift.
  - **B — Inline delete on element/project removal**: reclaims space immediately but **breaks undo** (the asset an undo needs is already gone) and runs deletion mid-session.
  - **C — No GC**: simplest, but orphaned assets leak storage indefinitely.
  - **D — GC on app start**: janks startup and still races undo within a session.
- **Tradeoffs:** A defers space reclaim slightly in exchange for **undo-safety** and crash-safety — the correct priority given undo is a headline feature. B's immediacy is incompatible with undo; C/D fail on leakage or correctness.
- **Recommended direction:** **A** for the *mechanism* (ref-count + deferred undo-safe sweep + reconciliation). Two sub-decisions remain open and block Accept:
- **Open: asset ownership model (must resolve before Accept).** Content-addressing implies one file can back many references; a naïve **per-project `refCount`** is unsafe — if project X drops its last reference, the sweep must not delete `assets/<hash>` while project Y still references that hash. Options:
  - **(i) Per-project asset store** (`projects/<id>/assets/<hash>`): dedup *within* a project only; GC and `.zine` backup are trivially self-contained; cross-project dedup is lost. *Leans recommended for MVP simplicity + backup portability.*
  - **(ii) Global content-addressed store** (`assets/<hash>`) with a **global** hash ref-count or a `project_asset` join table: dedupes across projects, but GC must check **global** references and backup must copy referenced bytes into the `.zine`.
  This corrects an inconsistency in the [storage spike §2/§5](spikes/data-storage-layer.md#2-storage-model--what-lives-where-and-why) (a per-project `asset.projectId` over a global store is contradictory).
- **Open: concrete grace boundary.** "Undo cleared or project close" must become a **persisted/schedulable state** — a `deletableAt` tombstone + an **active-session guard** so a WorkManager sweep can never delete an asset the open editor's undo could resurrect. Project-close is only safe if undo history is session-only (it is, per [ADR-005](#adr-005)).
- **Consequences (provisional):** Bounded, slightly delayed reclaim; couples GC to the undo horizon ([ADR-005](#adr-005)) and durability ([ADR-009](#adr-009)).
- **Review (2026-06-19):** Codex — **KEEP Proposed.** Mechanism is right; never-inline-delete is correct. Hold pending (a) a concrete asset-ownership model (global hash ref-count/join table **or** per-project store) that handles shared hashes, and (b) `deletableAt`/active-session semantics. Caught a real spec bug: per-project ref-count over a global content-addressed store is unsafe.

## ADR-023 {#adr-023}
**Asset fidelity = store a single imported original capped to the export ceiling resolution; derive edit/preview bitmaps on demand. Do not persist multiple fidelity tiers in the MVP. (Resolves O5.)**
- **Status:** Proposed (2026-06-19)
- **Context:** Editing wants small bitmaps; 300 DPI export ([ADR-011](#adr-011)) wants fidelity. Phone photos (12–50 MP) are far larger than any single panel needs, so storing them untouched wastes space and memory.
- **Alternatives considered:**
  - **A — Store one original capped at the export ceiling; derive tiers at runtime** *(recommended)*: cap the longest edge at the maximum useful resolution (a full-bleed sheet at 300 DPI, e.g. Letter ≈ 2550×3300) **with crop/zoom headroom (~1.5×)**; Coil derives edit/preview bitmaps. One persisted file.
  - **B — Store the full-resolution original untouched**: maximum fidelity, but unbounded storage and OOM risk on import.
  - **C — Store only a downsampled edit copy**: smallest, but permanently caps export quality (cannot re-export larger).
  - **D — Persist multiple tiers (original + edit + thumbnail)**: flexible, but multiplies storage and adds a tier-sync burden.
- **Tradeoffs:** A bounds storage/memory while preserving enough resolution for a full-sheet 300 DPI export; the residual risk is a user cropping into a *tiny* region of a capped image and losing sharpness — mitigated by the headroom multiplier and acceptable for a beginner home-print MVP. The exact cap/headroom is **empirical** and must be validated against real export output.
- **Recommended direction:** **A** as the *approach* (cap-and-derive). Held Proposed because the cap is an **irreversible** quality decision needing a measured policy, plus honest naming/UX.
- **Open before Accept (per Codex):**
  - **Measured cap policy:** the single cap must be the **max across all supported papers** (A4 is taller than Letter at 300 DPI) **plus future full-bleed** headroom — not just Letter. Validate the multiplier with real phone photos and **aggressive crops** (the failure mode is cropping into a tiny region of a capped image).
  - **Honest naming:** call the stored file an **"import master" / optimized import**, never the "original" — the full camera original is **not** retained. UX/schema/docs must make the non-retention explicit so re-crop/zoom/re-export expectations are correct.
- **Consequences (provisional):** Bounded storage; a defined quality ceiling; derive-on-demand keeps editing memory low ([ADR-011](#adr-011)).
- **Review (2026-06-19):** Codex — **KEEP Proposed.** Bounded storage is reasonable but this is an irreversible discard; lock only after a measured cap (max paper + bleed) with crop/zoom quality tests, and rename "original" → import master with explicit non-retention UX.
