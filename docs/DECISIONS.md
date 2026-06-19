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
| [ADR-015](#adr-015) | Validation result contract — document validation returns a structured `ValidationResult`; imposition keeps its list until a unification pass | Accepted |
| [ADR-016](#adr-016) | Format & paper extensibility (closed enums vs open specs) | Proposed |
| [ADR-017](#adr-017) | Bleed, clip & safe-area semantics | Proposed |
| [ADR-018](#adr-018) | Imposition convention & guide-ID versioning | Proposed |
| [ADR-019](#adr-019) | Domain-layer threshold — no `:core:domain` until ViewModels outgrow repositories | Accepted |
| [ADR-020](#adr-020) | Document serialization = kotlinx JSON behind a swappable `DocumentSerializer` | Accepted |
| [ADR-021](#adr-021) | Autosave = single-writer debounced atomic full-save + `onStop` flush + `.bak`; no op-log (MVP) | Accepted |
| [ADR-022](#adr-022) | Asset ownership = global content-addressed store + mark-and-sweep + grace window | Accepted |
| [ADR-023](#adr-023) | Asset fidelity = one 4096 px import master, original discarded, derive tiers | Accepted |

> ADR-014, ADR-016 to ADR-018 are **follow-ups surfaced by the [ADR-007](#adr-007) release-candidate audit** (2026-06-19): rationale/risks/future only, no decision, no engine change. **ADR-015 was resolved during S2A** (2026-06-19) when document validation introduced the first real `Severity.WARNING`.
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
**Validation result contract — document validation returns a structured `ValidationResult`; the imposition `LayoutValidator` keeps its `List<ValidationIssue>` until an explicit unification pass.**
- **Status:** Accepted (2026-06-19) — raised by the [ADR-007 RC audit](#adr-007); resolved in S2A when the first real `Severity.WARNING` appeared.
- **Context:** Two validators now exist. The imposition [`LayoutValidator`](spikes/imposition-engine.md) returns `List<ValidationIssue>` (empty = sound); the new S2A [`DocumentValidator`](spikes/data-storage-layer.md) needs explicit `isValid` gating (export/save block on errors, [ADR-021]) **and** a real warning level — `text.empty` is non-blocking. The trigger ADR-015 named ("decide when the first `WARNING` is actually used") has arrived.
- **Alternatives considered:**
  - **A — Structured `ValidationResult` for documents; imposition keeps its list for now** *(recommended)*: `ValidationResult(issues)` exposes `errors`/`warnings`/`isValid`; coded issues carry a node `path`. Imposition is untouched (out of S2A scope) and unified later. Two shapes briefly coexist, but each is the right tool for its caller today.
  - **B — Unify both validators on one shared result now**: cleanest end-state, but drags an imposition refactor (and its golden tests) into S2A, widening scope and risk for no MVP benefit.
  - **C — Keep document validation on a bare `List<ValidationIssue>` too**: rejected — it has no `isValid`/severity split, exactly the gap that blocks export-gating and forces every caller to re-derive "is this fatal?".
- **Tradeoffs:** A trades momentary duplication for scope safety and is **reversible** (a later pass folds imposition onto the same shape). The brief two-shape coexistence is acceptable because the document `ValidationResult` is deliberately designed to be that future unification target — this is a chosen shape, not accretion.
- **Recommended direction:** **A.** `core.data.validation.ValidationResult` (`Severity` ERROR/WARNING, coded `ValidationIssue` with `path`, `isValid = no errors`). Warnings never block; only errors gate. Imposition's list is unified onto this shape when the export-gating design (S5) lands.
- **Consequences:** Document validation has a real structured result and a used `WARNING` level; export/save can gate on `isValid`; the imposition validator stays as-is until S5. The unification is tracked as the trigger for the next pass.
- **Review (2026-06-19):** Codex — **promote to Accepted**; the first real warning (`text.empty`) is the right trigger, and scoping it as "document now, imposition later, do not half-unify" avoids the accretion ADR-015 warned about.

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
- **Amendment (2026-06-19, S2A implementation; Codex-reviewed):** the original caveat's "design migrators on typed/canonical versions, **not** raw `JsonElement` trees" is **too absolute for v1** and is refined, not reversed. For the JSON-only MVP (schema v1, zero real migrators), **JSON-tree migrators are permitted *inside* `JsonDocumentSerializer`** and are kept `internal` so they are never the app-wide migration abstraction (that remains the format-neutral `DocumentSerializer`). What keeps the Protobuf escape hatch *real* rather than cosmetic, and is implemented now: (1) an explicit persisted **format marker** (`_encoding`) so detection is never heuristic; (2) the **serializer owns** format + schema-version detection and legacy dispatch; (3) a **newer-than-current document is refused** (`NewerSchemaVersionException`), never tolerantly decoded-then-saved (which would silently downgrade — [ADR-021] durability); (4) a golden **structural-migration** test proving JSON-tree migration preserves moved/renamed data (the reason it beats tolerant-decode fixup). **Revisit trigger:** the *first* of {a real structural `v1→v2` migration, or adoption of a non-JSON persisted format} forces per-version typed snapshots + a one-time legacy-JSON→typed bridge. Codex verdict: reconciliation sound; do not refactor to typed snapshots at v1.

## ADR-021 {#adr-021}
**Autosave = a single-writer, debounced *atomic full-document save* (temp → fsync → atomic rename → dir-fsync) that keeps one prior-good `.bak`, force-flushed synchronously on `ON_STOP` and explicit editor-exit. No operation log in the MVP. (Resolves O3; durability contract closed.)**
- **Status:** Accepted (2026-06-19)
- **Problem:** The document tree lives in memory and is persisted as one JSON file per project ([ADR-003](#adr-003)). We must define *how often* we save, *how* a save is made crash-atomic, and *the exact data-loss bound we promise the user* — given Android can kill the process at any time with no callback.
- **Architectural constraints:**
  - [ADR-009](#adr-009): no lost work, crash-safe, no main-thread jank.
  - [ADR-003](#adr-003): the document is a JSON tree (not relational), so the unit of persistence is the whole tree, serialized via the [ADR-020](#adr-020) `DocumentSerializer`.
  - [ADR-005](#adr-005): undo/redo is **in-memory and session-only** — it is not the durability mechanism.
  - Privacy/offline: durability is purely on-device; no remote replica to fall back on.
  - Autosave logic lives in `:core:data` (Android `FileChannel`/lifecycle), never in the pure core.
- **Future requirements it must not foreclose:** the same atomic-write primitive must serve `.zine` backup export (V1); an optional write-ahead op-log could be added later if "zero-loss"/power-loss durability or collaborative history (Future) becomes a requirement — the `DocumentSerializer` boundary and single-writer actor make that additive.
- **Alternatives considered:**
  - **A — Debounced atomic full-save + synchronous `ON_STOP` flush + single-writer** *(recommended)*: coalesces bursts; rewrites the whole (small, KB-scale) JSON tree atomically; matches local-first editors (Excalidraw/tldraw debounce a full JSON snapshot). Loss bound: *since the last completed save*.
  - **B — A + a synchronous append-only op-log / journal**: tightest bound (last committed op ≈ 0 loss) but pays a synchronous write per edit and adds replay/recovery code — the main bug surface — for marginal benefit at zine scale.
  - **C — Document (or op-log) in SQLite/Room WAL**: crash-durability + auto-recovery for free, but turns the clean JSON tree into rows/opaque blobs, hurting portability/debuggability and fighting [ADR-003](#adr-003).
  - **D — Save on every change**: zero window but write-amplifies and janks.
  - **E — Save only on background/close**: a crash mid-session loses everything since open.
- **Tradeoff matrix:**

  | Approach | Loss bound | Write cost | Recovery-code complexity | Portability / debuggability |
  |---|---|---|---|---|
  | **A (chosen)** | since last completed save (≈ ≤ debounce; 0 on clean exit) | low (1 atomic rewrite per burst) | low (`.bak` fallback only) | high (plain JSON file) |
  | B | last committed op (≈ 0) | high (sync I/O per edit) | **high** (log replay) | high |
  | C | last commit / last checkpoint | medium | low (SQLite-managed) | **low** (rows/blob) |
  | D | 0 | **very high** | low | high |
  | E | whole session | very low | low | high |

- **Decision — A, with this concrete durability contract:**
  - **Single-writer:** one per-project serialization actor (a `Mutex`/`Channel` on an injected IO dispatcher) so saves never interleave; each save reads an **immutable document snapshot** decoupled from MVI command application ([ADR-005](#adr-005)).
  - **Cadence:** debounce **1 s** after the last edit, with a hard **max-latency cap of 5 s** so a long continuous edit still flushes. A superseded debounce is cancelled (latest-wins); the cap is measured from the first un-saved edit.
  - **Atomic write, every save:** serialize → write `document.json.tmp` → `FileDescriptor.sync()` → atomic `rename` over `document.json` → best-effort directory fsync. Before the rename, the previous good file is retained as `document.json.bak`.
  - **Flush triggers:** debounce fire, max-latency cap, **a synchronous flush that awaits the in-flight save on `ON_STOP`**, and explicit "leave editor."
  - **Open-time recovery:** if `document.json` is missing/corrupt (interrupted rename / power loss), fall back to `document.json.bak`. This is the backstop for the one residual gap below.
  - **No op-log in MVP** — justified: the tree is small and undo is in-memory; the `.bak` + atomic rename guarantee a *completed save is never corrupted*, and the `ON_STOP` flush drives loss to 0 on a clean stop. **A crash before the next save completes still loses the un-saved edits in the current debounce/cap window (≤ ~5 s)** — that bounded window, not zero, is the accepted MVP cost. A write-ahead op-log (option B) is the path to literal per-edit zero-loss and is deferred; the single-writer + [ADR-020](#adr-020) `DocumentSerializer` boundary keep it additive.
- **Reconciliation with [ADR-009](#adr-009):** ADR-009's "no lost work" is satisfied as **no loss of saved/committed work** (atomic write + `.bak` never corrupt or drop a completed save) **plus a small, bounded loss of in-flight edits** (≤ the debounce/cap window; 0 on clean `ON_STOP`). It is explicitly **not** a claim that every keystroke survives an arbitrary kill — that stricter guarantee needs the deferred op-log. This interpretation is the Accepted contract; ADR-009's prose is read through it.
- **Loss bound promised to the user:** *"Your work is saved within about a second of you pausing, and always when you leave the editor. In a crash or forced close you can lose at most the last few seconds of edits since the last autosave — never the whole project."* Residual gaps: (a) an app crash during the un-saved window loses ≤ ~5 s of edits (bounded, accepted); (b) power loss interleaved with the rename, mitigated by the `.bak` fallback. `ON_STOP` is best-effort and does **not** survive a low-memory kill mid-flush ([Android process lifecycle](https://developer.android.com/guide/components/activities/process-lifecycle)), which the bound already accounts for.
- **Cross-subsystem impact:**
  - **S2 Data:** defines `DocumentRepository.save()` (atomic-write + `.bak` + dir-fsync), the per-project single-writer mutex, the debounce/cap constants, and open-time recovery. No op-log table; no document-in-SQLite.
  - **S3 Render:** none directly — the renderer reads an immutable snapshot; autosave serializes the *same* snapshot, so render and save never contend.
  - **S4 Editor:** the ViewModel feeds document changes to an `AutosaveCoordinator` (debounce + cap); it **must** invoke the synchronous flush on `ON_STOP`/`onPause` and on editor-exit. Undo stays in-memory.
  - **S5 Export:** export must run against a flushed/known document state ("save-before-export"); export itself is a read, no autosave coupling.
  - **`.zine` package:** reuses the identical atomic-write primitive for the backup archive; a half-written `.zine` is never swapped in.
- **Evidence:** atomic write = temp→fsync(file)→rename→fsync(dir), with directory fsync required for durability ([evanjones.ca](https://www.evanjones.ca/durability-filesystem.html)); `onStop` is the last guaranteed callback and abrupt kill has none ([Activity lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle)); local-first editors debounce full JSON snapshots ([tldraw persistence](https://tldraw.dev/docs/persistence)). Landed in [RESEARCH R6](RESEARCH.md#r6-local-autosave-durability--crash-safety--verified--recommendation).
- **Review (2026-06-19):** Codex — first pass **HOLD** (the original draft overclaimed ≈0 app-crash loss and left ADR-009 unreconciled). After removing the overclaim and adding the ADR-009 reconciliation (committed-save durability vs bounded in-flight loss), Codex: **ACCEPT** — "no op-log is defensible for MVP." → **Accepted.**

## ADR-022 {#adr-022}
**Asset ownership = a single *global* content-addressed store `assets/<sha256>`. Liveness is decided by *mark-and-sweep* over the union of every project document's referenced hashes plus the live in-memory undo/redo stacks — not by reference counting. A deferred WorkManager sweep deletes only orphans whose file mtime is older than a grace window. The Room asset table is an index/cache, never the source of truth. `.zine` export is a copy-projection of the project's blobs. (Resolves O4; ownership model closed.)**
- **Status:** Accepted (2026-06-19)
- **Problem:** Imported images are copied in and content-addressed by sha256 ([ADR-004](#adr-004)). We must define *who owns the bytes* and *when they may be deleted* so that we (a) dedup, (b) never delete bytes an undo could resurrect, (c) correctly handle the **same image used by two projects** (shared hash), and (d) still produce a self-contained `.zine` backup.
- **Architectural constraints:**
  - [ADR-005](#adr-005): undo/redo is in-memory, session-only — a "removed" image must keep its bytes while undo can restore it.
  - [ADR-003](#adr-003): the project **document JSON is the authoritative reference set**; any DB index is derived from it.
  - [ADR-009](#adr-009): crash-safe, no jank — deletion must be off the hot path and self-correcting after a crash.
  - Privacy/offline: all in app-private storage; reclamation matters (phone photos are large).
- **Future requirements it must not foreclose:** cross-project dedup as project count grows; portable `.zine` import/export (V1) that merges assets without collisions; possible shared-asset libraries (Future).
- **Alternatives considered:**
  - **A — Per-project store** (`projects/<id>/assets/<hash>`), dedup within a project: trivially self-contained backup, but loses cross-project dedup and duplicates the same photo across projects.
  - **B — Global store + reference counting** (global count or `project_asset` join table): immediate reclaim, but ref-counts are **fragile** — one missed increment/decrement (crash mid-update, a lost undo edge) silently deletes live bytes; needs tombstones anyway.
  - **C — Global store + mark-and-sweep from document roots + grace window** *(recommended)*: liveness is **recomputed from authoritative roots** each sweep, so it self-heals after crashes and makes shared-hash correctness automatic (reachable from *any* project ⇒ live). This is the git / Nix / IPFS / Jackrabbit-Oak consensus.
- **Tradeoff matrix:**

  | Model | Cross-project dedup | Undo-safety | Shared-hash correctness | Crash self-heal | Backup portability |
  |---|---|---|---|---|---|
  | A per-project | ✗ (duplicates) | easy (local bytes) | N/A (each owns a copy) | n/a | trivial (zip the dir) |
  | B global + refcount | ✓ | needs tombstone | only if count never desyncs (**fragile**) | ✗ (count drifts) | copy-out on export |
  | **C global + mark-sweep (chosen)** | ✓ | ✓ (undo stack is a root) | ✓ **by construction** | ✓ (recomputed from roots) | copy-projection on export |

- **Decision — C, with this concrete model:**
  - **Store:** one global `assets/<sha256>`; new blobs are written to a temp name and atomically renamed into place (no half-written content-addressed files).
  - **Roots (the live set):** the union of (i) every project document's referenced asset hashes, (ii) the hashes held by the **live in-memory undo/redo stacks** of any open project, and (iii) the hashes in the **in-flight-import set** (below). While an image sits in undo history its hash is a root, so its bytes are never swept.
  - **GC trigger:** opportunistic + deferred via a **WorkManager** worker (e.g. on background / daily), never on the delete action — deletes feel instant, reclamation is lazy.
  - **Grace window:** the sweep computes the live set, then deletes only orphan files whose on-disk **mtime is older than a grace window (≥ 24 h)**. This protects (i) an import that wrote bytes just before the document reference committed, (ii) crash mid-edit, and (iii) the window between process death (which drops the in-memory undo roots) and the next sweep. Mirrors git `gc.pruneExpire` and Cassandra `gc_grace`.
  - **Reconciliation / source of truth:** the Room `asset` index `(sha256 → localPath, w, h, mime, firstSeen, mtime)` is a **cache/optimization**. The sweep reconciles `assets/` against the document-derived live set; a Room↔disk mismatch is resolved in favour of *disk-vs-documents*, so a corrupt index can never authorize deleting live bytes — only the documents can.
  - **Backup:** the global store gives runtime dedup; `.zine` export is a **projection** — resolve the project document's hashes, copy *those* blobs into the archive. Import copies incoming blobs into the global store keyed by hash, dedup-merging with anything already present. Export and the store are not competing sources of truth.
  - **Import↔GC race (closed):** the dangerous case is an **old orphan** (already on disk, mtime past the grace window, currently unreferenced) that a new import is about to *reuse* (same hash) — a sweep that snapshotted the live set before the import's document-commit could delete it. Closure: on import, after content-addressing, the importer **(1) registers the hash in an in-flight-import set** (an additional root, item (iii) above) and **(2) touches the existing blob to refresh its mtime**, both *before* writing the document reference; deletion runs **under a store mutex** and **re-checks `mtime > grace` at unlink time**. Freshly written blobs are already protected by their new mtime; this extends the same protection to reused old orphans. The hash leaves the in-flight set once the document reference is committed.
  - **Reference counting rejected** (option B): self-healing mark-and-sweep is both safer and, given few projects and small documents, cheap enough to run periodically. A refcount may later be added purely as a *fast-path hint*, never as the authority.
- **Cross-subsystem impact:**
  - **S2 Data:** `AssetStore` over global `assets/<sha256>` (temp→rename writes, an in-flight-import set, a store mutex shared with GC); the Room `asset` table as a derived index; a `GcWorker` (WorkManager) implementing mark-and-sweep + grace with an at-unlink mtime re-check; documents reference assets by hash.
  - **S3 Render:** the renderer resolves an image element's hash → loads bytes via Coil from the store; it must degrade gracefully if a hash is unexpectedly missing, and it never triggers GC.
  - **S4 Editor:** removing an element does **not** delete bytes (undo-safe); undo restores by re-referencing the hash; the open project's undo stack is a GC root; import adds a blob + a document reference.
  - **S5 Export:** export reads referenced blobs; for `.zine` it copies the projection; it never deletes.
  - **`.zine` package:** self-contained = the document + exactly its referenced blobs; import merges by hash (dedup on the way in). Manifest lists the hashes.
- **Cross-ADR note (hashing):** assets are content-addressed by the **stored master bytes** ([ADR-023](#adr-023)), i.e. after capping/EXIF-normalisation — not the camera original. Re-encode need not be bitwise-deterministic across devices: liveness uses whatever hash the document recorded, so any non-determinism only lowers the dedup hit-rate, never breaks GC correctness.
- **Evidence:** reachability-from-roots + grace-period pruning is the git / Nix / IPFS / Oak pattern; ref-count desync silently deletes live data; tombstone grace windows prevent data resurrection. Landed in [RESEARCH R7](RESEARCH.md#r7-content-addressed-asset-ownership--garbage-collection--verified--recommendation).
- **Review (2026-06-19):** Codex — first pass **HOLD**: flagged a real race where an **old orphan** past the grace window, about to be reused by a concurrent import, could be swept before the document commit. Closed with an in-flight-import root set + mtime-refresh-before-commit + store mutex + at-unlink mtime re-check. Codex: **ACCEPT** — "reused-old-orphan race closed." → **Accepted.**

## ADR-023 {#adr-023}
**Asset fidelity = store one *import master* per image, capped to 4096 px on the longest edge (EXIF-normalised on import); discard the camera original. Derive edit/preview/export bitmaps on demand. Schema and UX name it "import master," never "original," and surface that the full camera original is not retained. (Resolves O5; cap measured.)**
- **Status:** Accepted (2026-06-19)
- **Problem:** Editing wants small fast bitmaps; 300 DPI export ([ADR-011](#adr-011)) wants print fidelity. Phone photos (12–50 MP) far exceed any single panel's need, and decoding them whole risks OOM. We must pick *what bytes we keep* and *at what resolution* — an **irreversible** discard once the original is dropped.
- **Architectural constraints:**
  - [ADR-011](#adr-011): 300 DPI raster export; a full sheet ARGB_8888 ≈ 34 MB — memory is bounded.
  - [ADR-004](#adr-004): assets are content-addressed by stored bytes; the stored master *is* the addressed object ([ADR-022](#adr-022)).
  - [ADR-001](#adr-001)/[ADR-012](#adr-012): MVP papers are Letter + A4; full-bleed is future-facing.
  - Privacy: not keeping a second full-res copy of the user's photo is a privacy plus.
- **The pixel math (300 DPI):** `px = inches × 300`.

  | Surface | Letter (8.5×11″) | A4 (210×297 mm) |
  |---|---|---|
  | Full sheet, full-bleed | 2550 × 3300 (8.4 MP) | 2480 × **3508** (8.7 MP) |
  | One panel (¼ W × ½ H) | 638 × 1650 | 620 × 1754 |

  The largest longest-edge a single photo ever needs at print scale is **3508 px** (A4 full-bleed full sheet). A panel needs ~1650–1754 px.
- **Future requirements it must not foreclose:** larger formats / print-shop export (V2) may want a higher ceiling — the cap is a single named constant, and re-import is always available; full-bleed headroom is already covered by the chosen cap.
- **Alternatives considered:**
  - **A — Keep the full camera original**: maximum fidelity + crop headroom, but 15–25 MB per 50 MP photo and OOM-prone decoding.
  - **B — One capped "import master" (4096 px) + derive edit/preview/export on demand** *(recommended)*: exceeds the 3508 px worst case with crop headroom, ~5 MB/photo, OOM-safe.
  - **C — Only a downsampled edit copy**: smallest, but can underfeed a full-bleed export and kills re-export quality.
  - **D — Multi-tier (original + edit + thumb)**: max flexibility, but multiplies storage and adds tier-sync burden.
- **Tradeoff matrix:**

  | Strategy | Print quality | Crop headroom | Storage / photo | Memory / OOM |
  |---|---|---|---|---|
  | A full original | max | max | 15–25 MB | OOM-prone |
  | **B 4096 master (chosen)** | excellent (≥ full-sheet need) | good | ~5 MB | safe |
  | C edit-copy only | risky (may underfeed bleed) | poor | smallest | safe |
  | D multi-tier | max | max | highest | safe |

- **Decision — B, cap = 4096 px longest edge:**
  - **Why 4096:** it clears the 3508 px A4 full-bleed worst case (×1.17 margin) and gives real crop headroom — a 50 %-linear crop still yields 2048 px ≈ a full panel at 300 DPI / well above the 240 PPI "indistinguishable" floor. It is a power-of-two friendly to `inSampleSize`/GPU.
  - **Import pipeline:** decode-bounds → `inSampleSize` → downscale to ≤ 4096 longest edge → **normalise EXIF orientation once** → re-encode (JPEG q ≈ 90 for photos; PNG/lossless when alpha/graphics) → store as the master; **the camera original is discarded** after the master is written.
  - **Derivation:** edit/preview bitmaps are Coil-downsampled to the on-screen panel size (≈ hundreds of px, ~1 MB); export composites by sampling the master at the panel's exact target pixel box at 300 DPI — never the original, never the whole master at once where region decode suffices.
  - **Honest naming:** schema field and UX call it **import master / optimized import**; the import flow makes clear the full original is not retained, so re-crop/zoom/re-export expectations are correct.
- **Cross-subsystem impact:**
  - **S2 Data:** `AssetStore.import()` performs decode-bounds → downscale(4096) → EXIF-normalise → re-encode → hash → store; the stored asset is the master; the `asset` row records the master's dims/mime. No "original" field.
  - **S3 Render:** derives preview/edit bitmaps from the master via Coil; export samples the master at the target box; the renderer never needs the original.
  - **S4 Editor:** crop/zoom operate on the 4096 master with headroom; transforms are stored in points, applied to derived bitmaps.
  - **S5 Export:** samples the master at the exact 300 DPI panel box; quality meets full-sheet need; the cap bounds maximum print size to ≈ a zine sheet (acceptable for MVP scope).
  - **`.zine` package:** stores the master (not an original); a restored project has identical fidelity, and backups are ~5 MB/image rather than 15–25 MB.
- **Edge case (noted):** a tiny photo blown up to a full-sheet full-bleed poster sits at ≈ 4096/3508 ≈ 300 PPI — fine; a user wanting a print *larger than a zine sheet* is capped. Acceptable for a Letter/A4 home-print MVP; revisit if larger formats enter scope (ROADMAP V2).
- **Evidence:** 300 PPI is the print-quality target, 240 PPI the "indistinguishable" floor; proxy-and-master is standard in photo/video editing; Android large-bitmap decoding (`inSampleSize`, `BitmapRegionDecoder`) and EXIF normalisation are required to avoid OOM/rotation bugs. Landed in [RESEARCH R8](RESEARCH.md#r8-imported-image-fidelity--storage--verified--recommendation).
- **Review (2026-06-19):** Codex — **ACCEPT** (first pass): "4096px is justified for the stated MVP … discarding the camera original is acceptable if the UI/schema honestly call the retained file an 'import master' … hashing post-downscale/EXIF master bytes is correct; non-deterministic re-encode reduces dedupe hits but does not break liveness." → **Accepted.**
