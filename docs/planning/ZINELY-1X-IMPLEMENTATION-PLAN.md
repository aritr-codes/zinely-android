# Zinely 1.x — implementation plan

Status: **PLAN — proposal for the owner. Nothing here is decided or authorised to build.**
Date: 2026-09-25, revised the same day from the [readiness audit](ZINELY-1X-READINESS-AUDIT.md) ·
Author: research session (Claude) · Base: `origin/main` @ `eb75cf7` (Step 0 merged; beta.5 released: tag
`v0.9.0-beta.5` at merge `32da280`, GitHub pre-release, APK only; website updated at `5f7707a`).
Code evidence was gathered at `5f7707a`. Step 0 changed no `src/main` file (it added tests, fixtures, one
Gradle task run in the existing CI step, a `.gitattributes` line and [ARCHITECTURE §4.1](../ARCHITECTURE.md)), so those `file:line` citations still hold at
`eb75cf7` except where re-cited below.

> **Implementation status** (this plan's one status record; the [§5](#5-sequencing) table repeats it per step).
>
> | Step | Status | Record |
> |---|---|---|
> | **0 · Guards** (F1a, F3, F4) | ✅ **COMPLETE — merged 2026-09-26** | PR #75 · merge `eb75cf7ae78a91c5df5c331ccb85ef19520ec87a` · commits `8ed6d29` (guards) and `f455cd9` (A1 hardening) · [ARCHITECTURE §4.1](../ARCHITECTURE.md) |
> | 1 onward | Not started | — |
>
> **Step 0 deferred advisories** (from its independent review; the owner deferred them; **not blockers** for
> any step):
> - **A2:** no minSdk-24 privacy case, so a permission declared with `maxSdkVersion` ≤ 27 is invisible to
>   `PrivacyManifestTest`.
> - **A3:** the allow-list admits `androidx` and `org.jetbrains` by prefix, so groups such as `androidx.browser`,
>   `androidx.credentials` or a `kotlinx-rpc` artifact would pass.
> - **A4:** `schema-shape-v3.txt` is not hash-pinned.
>
> **F6** (portability denylist) stays **deferred**: optional, the owner's call ([decision gate](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)).
Inputs: [future-product research](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md) (proposal only), checked
against the repository. **Where the research and an authoritative record disagree, the record wins** and this
plan says so ([§2](#2-where-the-research-was-corrected-by-the-repository)).
Why a new `docs/planning/` folder: the owner asked for it, and it holds a cross-cutting plan plus briefs;
[`docs/proposals/`](../proposals/) holds single-surface design proposals. Neither folder overrides an ADR,
the constitution or a frozen spec.

> **The planning set, and which document does what.**
>
> ```
> research → readiness audit (evidence) → corrected brief (spec) → fresh implementation session
> ```
>
> | Document | Role |
> |---|---|
> | This plan | Directions, foundations, sequence |
> | [Readiness audit](ZINELY-1X-READINESS-AUDIT.md) | Evidence and corrections, already folded into this plan and the briefs |
> | [Decision gate](ZINELY-1X-DECISION-GATE.md) | The eight owner questions, framed but not decided |
> | [Owner decision brief](ZINELY-1X-OWNER-DECISION-BRIEF.md) | The same eight questions as neutral options, plus the HTML amendments awaiting owner approval |
> | [Handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md) | The prompt that opens every implementation session |
> | Briefs 01–05 | One spec per direction |
>
> A brief that still carries a "Corrections pending" banner is **not** ready to implement.
>
> **How to use this.**
> - The owner resolves the [decision gate](ZINELY-1X-DECISION-GATE.md) questions that the chosen step needs.
> - A fresh session then starts **one** step of [§5](#5-sequencing), opened with the
>   [handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md).
> - Every UI item still follows the mandatory pipeline in
>   [CLAUDE.md](../../CLAUDE.md#html-first-ui-workflow-mandatory): HTML spec → freeze → Compose → pixel parity
>   → both device passes → review. That pipeline is most of the cost of every UI item below.
> - This plan decides nothing by itself. Each direction becomes an ADR (or a ROADMAP/PRD change) when started.

**Briefs:**
[01 Visible ownership](BRIEF-01-VISIBLE-OWNERSHIP.md) ·
[02 Three voices](BRIEF-02-THREE-VOICES.md) ·
[03 Printer test page](BRIEF-03-PRINTER-TEST-PAGE.md) ·
[04 Reading order and alt text](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md) ·
[05 Materials: frames](BRIEF-05-MATERIALS-FRAMES.md)

---

## 1. The product today (re-read from the repository)

*Snapshot re-checked at `5f7707a`, with the Tests row updated at `eb75cf7`; **non-authoritative**: each row points at the record that owns the fact.*

| | What is true on `origin/main` | Evidence |
|---|---|---|
| **Identity** | A pocket press: photos, words and bundled Art made into one 8-page single-sheet zine, read on the phone, saved or shared as a PDF, printed and folded at home. Offline, no account, no network | [PRD](../PRD.md), [constitution](../zinely-constitution.md), [ADR-118](../DECISIONS.md#adr-118) |
| **Spine** | Shelf → Bench → Proof, each answering one question | [ADR-103](../DECISIONS.md#adr-103), [CLAUDE.md](../../CLAUDE.md#product-principle-every-screen-answers-the-users-current-question) |
| **Frozen design** | `v21-library`, `v21-bench` (incl. Add chooser and Art sheet), `v21-proof` (incl. fold guide), `v21-colophon`, `backup-restore`, `app-entry`, `theme-37596`. `v21-typebar` and `v21-reframe` headers say "proposal, not frozen" while ZINE-DIRECTION N2 marks them frozen: **conflict, owner to confirm** | `docs/design/mockups/*`, [V21-SPEC](../design/V21-SPEC.md) |
| **Document** | `ZineDocument` schema **v3**; `Page` has **no id** (index kept by `renumber()`); three element types (Image, Text, Decor); `ZineFormat` = `SINGLE_SHEET_8` only; the validator requires exactly 8 pages | `core/model/.../Document.kt:28,58-63,83-169`, `DefaultDocumentValidator.kt:33-54` |
| **Schema evolution** | Strict v*N*→v*N*+1 chain; a newer document is refused (`SchemaTooNew`); JSON uses `ignoreUnknownKeys = true`, so a field added **without** a version bump is silently dropped by an older build on save | `DocumentMigrations.kt:59-86`, `JsonDocumentSerializer.kt:38-54` |
| **Editor** | Pure MVI reducer; undo = in-memory list of field-level `Command` mementos, **unlabelled and uncapped**; `AddPage`/`DeletePage` exist but no UI dispatches them | `core/editor/.../EditorModel.kt:72-75`, `Command.kt`, `EditorReducer.kt:340-360,437-444` |
| **Render** | One draw tape (`FillRect`, `DrawImage`+copier flag, `DrawShape`, `DrawTextBox`) and **one** replayer (`CanvasReplayer`) serving Bench, page nav, Read and export. The Proof print sheet is a **schematic** (page numbers), not a render of the zine | `core/render/DrawCommand.kt`, `render-android/CanvasReplayer.kt:45`, `ProofSheet.kt:100-146` |
| **Text** | Line breaking by `StaticLayout` at render time; the document stores no line breaks and no layout version; **one document font family (Inter)**, any other name falls back to Inter | `SharedTextLayout.kt:38-60`, `DocumentFontRegistry.kt:88-113` |
| **Assets** | Import masters are **JPEG q90, ≤4096 px**, content-addressed; transparency is flattened (to white after beta.5); **no asset GC exists** (storage only grows — a published known limitation) | `ImportMasterDecoder.kt:28-161`, `FileAssetStore.kt:25-28` |
| **Art** | 32 bundled supplies in four frozen families; decor = one even-odd path tinted in one ink; Flip (ADR-113) already mirrors Art (`EditorReducer.kt:212`). **Six holed supplies already ship** (`paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `mark.registration`, `fix.corner`), and the rectangle hit test makes a tap in a hole select the piece, not the photo beneath | `SupplyCatalog.kt:162-169,241-262,284-287,412-414,490,503-544`, `HitTest.kt:21-36` |
| **Accessibility** | Per-element semantics with 13–15 custom actions; TalkBack order = **list order** (see [§2](#2-where-the-research-was-corrected-by-the-repository)); Read mode clears page semantics, so TalkBack hears nothing of the zine there | `ElementSemanticsLayer.kt:80-132`, `EditorA11y.kt:109-186`, `ProofRead.kt:554,629,674` |
| **Ownership** | Files are truth, Room is an index (ADR-042); whole-library `.zine` v2 backup/restore (ADR-110); single-zine v1 manifest exists but nothing reads or writes it; every backup domain excluded from cloud **and** device transfer (ADR-030 §7) | `ZineLibraryBackup*`, `data_extraction_rules.xml` |
| **Export** | `SheetComposer` → one-page vector `PdfDocument`; Save to Downloads / Share; no in-app print (ADR-052) | `SheetComposer.kt:52-86`, `ZineExporter.kt` |
| **Tests** | 273 unit test files at `eb75cf7` (269 before Step 0; 117 in `feature:editor`), jqwik property tests in four core modules, 132 Roborazzi goldens verified in CI with `--rerun-tasks`; **no instrumented tests run in CI**. Since Step 0: frozen, SHA-pinned fixtures (document v1–v3 JSON and a `.zine` v2 library archive), a schema-shape snapshot, a privacy-manifest test and a CI dependency allow-list ([ARCHITECTURE §4.1](../ARCHITECTURE.md)) | `.github/workflows/ci.yml:44,121-146` (at `eb75cf7`) |
| **Roadmap** | Planned: Google Play. Exploring: fold clarity study; creative tools (fonts → Art pack and frames → transparency → preset cutouts → freehand/automatic). "No new app feature is committed for the next release" | [ROADMAP](../ROADMAP.md#current-priorities) |
| **Known limits (beta.5)** | As the release notes put it: transparent pictures show a white box on the cream page (exports print correctly); on Android 7–9, choosing "don't ask again" leaves Save PDF on its error until storage is allowed in Settings (emulator-verified only); replaced photo assets are retained; with TalkBack, About and its pages open on the Back button and Back returns to the top of the previous screen; the zine actions sheet's dimmed area is read as an unlabelled button; no font choice; printing happens in another app. **Addressed by this plan:** the scrim (step 2), font choice (step 8). **Not scheduled here** (ROADMAP engineering follow-ups): the 7–9 "don't ask again" dead end, a real-phone 7–9 check, retained assets (F5), TalkBack focus on About (Brief 04 keeps programmatic focus out of scope), About/ADR-116 parity | [beta.5 release notes](../releases/0.9.0-beta.5.md#known-limitations) |

## 2. Where the research was corrected by the repository

The [readiness audit §2](ZINELY-1X-READINESS-AUDIT.md#2-plan-accuracy) then found six wrong premises in the
first version of this plan (P1–P6). They are corrected in place below and in the briefs.

| Research said | Repository says | Consequence for this plan |
|---|---|---|
| TalkBack reads in **z-order** | It reads in **list** order. `ReorderCommand` changes `zIndex` only (`Command.kt:35-45`), so after *Bring forward* TalkBack order diverges from paint order, contradicting the layer's own comment (`ElementSemanticsLayer.kt:84-85`) | A latent a11y defect, not only an enhancement. Brief 04 part A fixes it |
| Ink lift/"paper scan" is a research idea | It is **EXPERIMENTAL** in ZINE-DIRECTION ("prove separately, touch nothing core") and P3 in PRODUCT-DIRECTION; the phrase "ink lift" exists only in the research | Stays Exploring; not in 1.x wave 1 or 2 |
| Page reorder needs stable page ids first | Reorder alone is a permutation plus `renumber()`; ids matter for the tray, page exchange and multi-format. But `MakeImageSpreadCommand` pairs neighbouring pages (ADR-109), so reorder must keep or break spreads **explicitly** | Page ids only when a feature needs them; spread handling is part of any reorder spec |
| Asset GC is an ADR-025 concern to protect | **No GC exists at all** | Anything that adds assets (ink lift, transparency, sample zine) grows storage; a GC is a real foundation before those, not before wave 1 |
| Starters are a Tribunal KEEP | They are, **and** ZINE-DIRECTION and BETA-DIRECTION say "Templates gallery — DO NOT BUILD / DO NOT IMPLEMENT". Two authoritative records conflict | Owner decision ([§9](#9-owner-decisions-needed-before-implementation)); not scheduled |
| Three voices need a schema change | `TextStyle.fontFamily` already exists as a string and is serialised (`Document.kt:189-196`); an older build keeps the string but renders Inter. Averia and Fraunces set narrower than Inter (⚠️ audit estimate, re-measure), so in an older build the last lines of a text box are **clipped** — layout loss the ROADMAP forbids | Not a free "no bump". Either accept clipping in restored zines, or ride the v4 bump and accept that whole backups then refuse to restore on older builds — an owner trade-off ([decision gate Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)) |
| A portability guard is "days" | A real guard (a non-JVM compile target) *is* the multiplatform conversion. A text denylist is cheap but incomplete | Denylist test when the owner opts in (F6, optional, deferred); conversion only on iOS evidence ([§7](#7-ios-preparation-architecture-only)) |
| OWNER-CHECKLIST: "Mirror verb … `DecorElement.mirrored` has zero UI callers" | **Stale.** ADR-113 Flip toggles `mirrored` for Art (`EditorReducer.kt:212`, `FlipTray.kt:333`); the KDoc at `Intent.kt:52-53` is stale too | No Mirror work; close the checklist row as docs hygiene |
| Frames could be slot composites | **Withdrawn** by ADR-107 R3 (even-odd outlines turn overlaps into holes) | Frames = single authored outlines inside the frozen families |
| `ADR-030 §7` governs backups | Confirmed: §7 is the privacy invariant (`allowBackup=false`, D2D exclusions) | Phone migration amends ADR-030 §7 |

## 3. Post-beta directions

Eight directions. D1–D5 have briefs and a place in the [§5 sequence](#5-sequencing); D6–D8 are later.
Complexity is relative (S / M / L), including the HTML-first pipeline.

### D1 — Ownership you can see *(Shelf)* → [Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md)

- **User problem:**
  - Zines live only on the phone. Changing phones silently arrives at an empty Shelf.
  - The backup sheet claims "kept safe" before any backup exists.
  - A failed backup reads like a failed restore.
- **Opportunity:** make ownership a visible, *true* fact. Part 1: "last backup saved", plus honest backup
  failures. Part 2: a changing-phones line, once cross-device restore is verified. Part 3: device transfer
  (O1; research leans to no).
- **Fits Zinely:** constitution Article 3 ("including its survival"); Tribunal "backup/restore/migration —
  constitutional debt, overdue"; ADR-110.
- **UX:** one dated fact and honest wording on a sheet the maker opens on purpose. No nagging, no notifications.
- **Technical:** a last-backup record on the existing DataStore (not in documents); backup-failure copy
  separate from restore copy, including for a zine that is invisible because it can't be opened; deleting the
  partial file a failed backup leaves. No archive or schema change.
- **Dependencies:** the `backup-restore.html` amendment (owner-approved, since it changes frozen copy); the
  physical cross-device pass for part 2.
- **Risk:** low.
- **Complexity:** S.
- **Sequence:** step 1 ([§5](#5-sequencing)).

### D2 — A voice for your words *(Bench typography)* → [Brief 02](BRIEF-02-THREE-VOICES.md)

- **User problem:** every Zinely zine is set in Inter, and a permanently disabled *Font* control sits on the
  Bench.
- **Opportunity:** a small set of named voices chosen per text element, not a font picker. ZINE-DIRECTION
  names Averia, Fraunces and Inter; whether all three may set zine text is the owner's
  [Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8).
- **Not implementation-ready.** Blockers:
  - Averia's Latin-only coverage, which falls back to a device font silently;
  - the editing surface always draws Inter, and fakes italic;
  - older builds clip the text, unless the zine rides the v4 bump, which makes whole backups unrestorable
    on older builds;
  - V2-CONSTITUTION §III points both ways on zine text;
  - owner questions O8, O10, O12 and O14 ([decision gate](ZINELY-1X-DECISION-GATE.md) Q2, Q4, Q5), and
    PR #70's acceptance work (its removal is already approved).
- **Technical (when unblocked):**
  - family-aware coverage in `core:model`;
  - an editing surface that uses the document's fonts;
  - bundled static TTFs, unmodified;
  - one Fraunces optical size;
  - an ADR superseding ADR-055's exclusion.
- **Risk:** APK size (~0.5 MB estimated); coverage; small printed x-heights.
- **Complexity:** M.
- **Sequence:** step 8, after the wave-1 release.

### D3 — A first print that works *(Proof)* → [Brief 03](BRIEF-03-PRINTER-TEST-PAGE.md) (⚠️ still "Corrections pending"; see the [owner decision brief §3](ZINELY-1X-OWNER-DECISION-BRIEF.md#3-briefs-02-and-03))

- **User problem:** the first print fails (shrunk, clipped, wrong paper) and the maker blames themselves.
- **Opportunity, staged, stopping at the first stage that is enough:**
  1. Stage 1: informational guidance in Proof's Print step, which turns the fold lines already on every sheet
     into a scale check.
  2. Stage 2: an optional static, self-explaining test page.

  A per-device reach profile (O13) is research-recommended against. Any inset change is its own session with
  an ADR.
- **Prerequisite:** a **physical print study** that establishes what Android print paths actually do. The
  app must not promise "Actual size" until the study shows it can be reached.
- **Dependencies:** `v21-proof.html` amendment; ADR-039 (edge-to-edge tiling) is a constraint, not a defect.
- **Complexity:** S (stage 1).
- **Sequence:** step 5.

### D4 — Zines everyone can read *(accessibility)* → [Brief 04](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md)

- **User problem:**
  - TalkBack order can disagree with what is drawn on top.
  - Read mode speaks nothing from the page.
  - Blind *readers* get nothing from photos.
  - Undo is silent about what it undid.
- **Opportunity:**
  - A1: spatial reading order, with a large-element guard so full-page pieces don't swallow the page.
  - A2: relative phrases, after a wording spec.
  - A3: named undo, with labels derived from commands.
  - B: maker-written alt text, which needs a Read-semantics design.
- **Technical:** A is pure ordering in core plus the semantics layer. B is a model field, so it rides the v4
  bump.
- **Verification:** `uiautomator dump` shows child order, not traversal order. Compose focus tests do not prove
  TalkBack behaviour. A device listen is the evidence.
- **Complexity:** A S · B M.
- **Sequence:** A1 step 2, A3 step 3, B step 7.

### D5 — Richer materials *(Art)* → [Brief 05](BRIEF-05-MATERIALS-FRAMES.md)

- **User problem:** there are few frames to put around a photo. The six holed pieces that ship can't be
  tapped through to the photo beneath. The library has ~19 more researched pieces waiting (ADR-107 R1
  backlog).
- **Opportunity:** decorative frames as single-outline overlay decor inside the frozen families, plus the
  owner's pick from the backlog. Composite frames are withdrawn (ADR-107 R3).
- **Dependencies:**
  - the hole-aware hit test (step 4);
  - a `v21-bench.html` set amendment (ADR-107 R1a);
  - the owner's O9 and stretch policy (Q6, D-100);
  - O14 or the fold study (Q5).
- **Technical:** outlines in `SupplyCatalog`, names in `Copy.Supplies`. No schema bump. An older build keeps
  an unknown `supplyId` as an invisible, selectable box: a release-notes line.
- **Complexity:** S–M.
- **Sequence:** step 9.

### D6 — Photo looks, transparency, then paper-in *(images)*

- **User problem:** most disappointing Copier results come from poor input levels; see-through PNGs lose
  their transparency; hand-made marks can't get in.
- **Opportunity, in order:** (1) auto-levels in front of the existing Copier — no new control, no schema;
  (2) a `look` field generalising `copier` (named single-ink screens) in its own later bump (not v4, [F1b](#4-foundations--only-what-the-repository-actually-needs)); (3) an **asset-model
  ADR** deciding source alpha and derived assets; (4) ink lift only after (3) and an owner promotion.
- **Three different "transparencies" in the records — do not conflate:** (i) *source alpha* — a see-through
  PNG, flattened at import today (what (3) means); (ii) the ROADMAP's *Photo transparency* — a per-image
  **opacity** control ([creative-tools assessment](../ROADMAP.md#creative-tools-assessment-2026-09-12),
  "Medium"); (iii) ZINE-DIRECTION's *Transparency: DO NOT BUILD* — a transparent **export**. This plan does
  **not** schedule (ii): it stays a ROADMAP Exploring item the owner can swap in for D6-(1); it is not
  dropped by omission. Art never gets opacity either way (SUPPLIES-SPEC).
- **Fits Zinely:** ADR-106 pattern (render-time, flag on the tape); photocopier identity.
- **Technical:** pure functions beside `Photocopier.kt`; masters are JPEG-only, so transparency changes the
  import pipeline, the backup validator (`image/jpeg` only) and storage — and there is no GC.
- **Risk:** kitsch filters (V2-IDENTITY forbids a global "riso" filter); storage growth.
- **Complexity:** (1) S · (2) M · (3) decision · (4) M–L. **Architecture change first:** yes for (3)/(4).
  **Belongs:** (1) wave 2; (2) its own bump after v4; (3)/(4) later 1.x.

### D7 — Room to grow *(pages)*

- **User problem:** pages cannot be reordered or duplicated; eight pages are the ceiling.
- **Opportunity:** page reorder/duplicate (Tribunal KEEP, minor; ZINE-DIRECTION X8; the frozen page grid
  already says "drag to reorder"), then a 16-page booklet admitted *by procedure*.
- **Technical:** reorder = `ReorderPagesCommand` (permutation memento) + explicit spread handling. 16 pages =
  a second `Imposer`, `ZineFormat` value, validator change, two-sided Proof flow, D-030 ruling.
- **Risk:** spreads silently breaking; the 24-page-first-zine over-ambition.
- **Complexity:** reorder M · 16-page L. **Architecture change first:** 16-page yes (D-030, formats).
  **Belongs:** reorder 1.x wave 2–3; 16-page later.

### D8 — Permission to start *(first run)*

- **User problem:** a blank 8-page zine asks a first-timer for permission and ambition at once.
- **Opportunity:** a seeded sample zine designed to be printed unmodified, and a browsed prompt library.
- **Constraint if built:** constitution Article 7's starter bright line — *"a starter finished unmodified
  is a known failure"*; overwriting must be the path of least resistance.
- **Blocked:** records conflict (Tribunal KEEP vs ZINE-/BETA-DIRECTION DO NOT BUILD templates); content needs
  real photos by real people (never the owner's private photos); no first-run prototype exists.
- **Complexity:** prompts S, sample M (content). **Belongs:** later, after the owner rules.

**Not directions (named so they are not mistaken for omissions):** Clippings tray / X2 (D-029 Q1–Q3 open;
owner ruling O4); drawing layer (Tribunal "admissible someday"); in-app PrintManager (ADR-052; needs a 1:1
device test); import-PDF; background removal (ML Kit disqualified; a bundled-model spike only); localisation
(needs a resource layer); iOS ([§7](#7-ios-preparation-architecture-only)).

## 4. Foundations — only what the repository actually needs

*Stable numbering:* Step 0's tests and `app/build.gradle.kts` cite these rows as "1.x plan §4 F1a / F3 / F4", and
[ARCHITECTURE §4.1](../ARCHITECTURE.md) cites "1.x implementation plan, step 0". Keep this section's number and
the F-labels stable.

| # | Foundation | Why this repo needs it | Before | Size |
|---|---|---|---|---|
| **F1a** | **Schema shape guard, now, against v3.** A JVM test walks `ZineDocument.serializer().descriptor` recursively (`elementNames`, `getElementDescriptor`, `isElementOptional`, `kind`, every sealed subclass and enum) and compares it with a checked-in `schema-shape-v3.txt`. It fails when the shape changes without `CURRENT_SCHEMA_VERSION` changing. It **would** have caught ADR-106's copier field (a new field appears in the descriptor); it cannot see a changed *default value*, so it is paired with the F3 fixture corpus. No custom serializers exist, so the walk sees everything ([audit §4 F1](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit)) | `ignoreUnknownKeys = true`: an unbumped field is silently lost when an older build saves (the ADR-113 failure mode) | ✅ **done in step 0** (PR #75): `DocumentSchemaShapeTest`, `schema-shape-v3.txt` | S |
| **F1b** | **One schema bump (v4), later, alone.** v4 adds exactly: `description` on `ImageElement` and `DecorElement` (D4-B's alt-text field, no UI), and a layout-engine marker ([§7](#7-ios-preparation-architecture-only)). The v3→v4 migrator is an identity step: the new fields default to absent. **`look` is not in v4**: D6-(2) is undesigned, so its values and its `copier: true` → look migrator come with their own later bump. Page ids only if a feature needs them. **Release boundary:** a version number guards only the fields released with it, because the serializer stamps `CURRENT_SCHEMA_VERSION` on every save (`JsonDocumentSerializer.kt:45-48`). So v4 is released together with step 7, its field's first user. D2 rides v4 only if it lands before that release; after it, D2 takes v5 or accepts clipping on v4 builds ([decision gate Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)). Add `explicitNulls = false` (experimental API; `core:model` has no nullable fields today, so no stored bytes change). Six readers key off the version, including `DefaultDocumentValidator.kt:25`; they need tests (v4 accepted, v5 refused), not edits. **Consequence:** once a library holds one v4 zine, its whole backup refuses to restore on any older build (`ZineLibraryBackupStager.kt:367-376`, `ZineLibraryBackupValidator.kt:80`). The release notes must say so | each bump pays the older-build and restore-matrix cost again, so bumps are few and deliberate | D4-B, possibly D2 — **only after wave 1 is released** | M |
| **F2** | **Undo labels — corrected.** Every edit command already records what changed (before/after mementos, or the placed/deleted element or page itself, `Command.kt`), so a pure `Command.editLabel(doc)` derives the label (the document supplies the element's kind); no `History` or `committing()` change (23 call sites untouched). Three ambiguities remain: *duplicate vs add* (both `PlaceCommand`); *Reset framing vs a reframe ending at FULL/FILL* (`EditorReducer.kt:188-192`); and restack direction (an adjacent swap, `ZOrder.kt:39-43`). No cap is needed (mementos hold no bitmaps). Specified in [Brief 04 A3](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md) | Named undo needs it | named undo (D4-A3) | S |
| **F3** | **Fixture corpus + format note** — one writer-produced library archive (see below; not a device export) and v1/v2/v3 `document.json` files, all **fictional content**, checked in under `core/data-storage/src/test/resources/` and `core/data/src/test/resources/`, restored by every build. Today the writer→stager round trip is built in memory, so a change to both sides passes every test. **Producing the archive:** it is writer-produced at `5f7707a` from test-built entries, not a device export (the Android repository assembles the manifest metadata on a device, `RoomProjectRepository.kt:456-470`); run the unchanged writer over fictional zines whose photos are real JPEG bytes (restore enforces `image/jpeg`, and the JVM tests fake photo metadata), in a JVM test run once, and check the bytes in; no device needed. The "spec" is a short section in ADR-110 or ARCHITECTURE, not a new document. Finishing or deleting the unused single-zine v1 package can wait | fixtures are what make F1 and any cross-device claim testable | ✅ **done in step 0** (PR #75): `DocumentFixtureCorpusTest`, `LibraryBackupFixtureTest`; the format note is ARCHITECTURE §4.1. The archive test stages only (the `:data-android` commit and Room reconcile are not exercised) | S |
| **F4** | **Privacy guard.** (1) A Robolectric `PrivacyManifestTest` asserting the exact requested-permission set and `allowBackup=false`. **Pin `@Config(sdk = [28])`**: Robolectric's real `PackageParser` drops a `maxSdkVersion="28"` permission when the test SDK is higher, so at SDK 29+ `WRITE_EXTERNAL_STORAGE` vanishes and the test fails; never "fix" that by deleting the assertion, because it is what proves the merged manifest was read. Unit tests see the **debug** merged manifest (test components added), so assert permissions and `allowBackup` only. Re-derive the release set with `processReleaseManifest` first. (2) Keep a **dependency allow-list**: the privacy invariant bans network and analytics *libraries*, not only their traffic. If O5 runs the IAP trial, Play Billing needs an allow-list entry **and an ADR** — the guard is never widened quietly | makes the constitution's promise mechanical; cheap | ✅ **done in step 0** (PR #75): `PrivacyManifestTest` at SDK 28 plus one SDK 34 case (at SDK 28 Robolectric also reports `READ_EXTERNAL_STORAGE`, a platform split permission of `WRITE_EXTERNAL_STORAGE`); `:app:checkDependencyAllowlist` in CI. Advisories A2/A3 deferred | S |
| **F5** | **Asset-model ADR + asset GC**: source-alpha/derived assets and a mark-and-sweep collector. The ROADMAP parks image GC behind import/undo/recovery safety ([Parked](../ROADMAP.md#engineering-follow-ups--not-extra-public-feature-promises)), and [ADR-031](../DECISIONS.md#adr-031) §2 requires import to pin a hash before any sweep — that pin comes first | masters are JPEG-only and storage never shrinks today | D6-(3)/(4), sample zine | M–L |
| **F6** | **Portability denylist test** for the five I/O-free core modules (optional). Use word-boundary regexes and strip comments first: a bare `Math.` matches `TransformMath.`/`FramingMath.`, and `Locale` matches a KDoc. Run once to see the real violation list, then allow-list those lines | keeps iOS open for almost nothing ([§7](#7-ios-preparation-architecture-only)) | nothing; owner's call whether now or later | S |

**Deliberately not recommended** (the repository does not need them): a new export abstraction (one replayer
already serves four surfaces — protect it); a layout representation change (StaticLayout at render time is
fine until a second platform exists); an undo architecture rewrite (Command mementos work); new module
boundaries; design-token evolution (tokens are frozen and sufficient for every wave-1 item).

## 5. Sequencing

Revised 2026-09-25 after the D1 deep audit. The ordering rule is **reduce architectural risk and improve the
actual maker experience**, not ship the most features. Each step is **one fresh implementation session**,
opened with the [handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md).

```mermaid
flowchart TB
    S0["0 · Guards ✅ merged (PR #75)<br/>privacy test · schema-shape guard · fixture archive"]
    A1["1a · backup-restore.html amendment<br/>(design session)"] --> S1["1 · D1 part 1<br/>last backup saved + honest backup failures"]
    S1 --> S1B["1b · Restore honesty<br/>(if Q8 schedules it; needs a spec)"]
    S1B -. "if scheduled" .-> REL
    P70["PR #70 acceptance<br/>(rebase, merge or close)"] --> S3
    S2["2 · D4-A1<br/>spatial reading order + scrim fix"] --> S3["3 · D4-A3<br/>named undo"]
    Q3["Q3 · tap-through scope"] --> S4["4 · Tap-through hit test<br/>(scope per Q3)"]
    PS["Print study (people + printers)"] --> S5["5 · D3 stage 1<br/>print guidance"]
    Q8["Q8 · release shape"] --> REL
    S0 --> REL
    S1 --> REL
    S3 --> REL
    S4 --> REL
    S5 -. "only if ready" .-> REL["Release wave 1<br/>(release session, owner-approved)"]
    REL --> S6["6 · F1b schema v4 bump, alone<br/>(held unreleased)"]
    S6 --> S7["7 · D4-B alt text<br/>(needs a Read-semantics design)"]
    S7 --> REL4["Release: v4<br/>(owner-approved)"]
    S6 -. "if D2 lands before the v4 release (Q2)" .-> S8["8 · D2 voices"]
    G2["Q2 · Q4 · Q5 + PR #70 accepted"] --> S8
    S4 --> S9["9 · D5 frames"]
    G6["Q5 · Q6 + bench amendment"] --> S9
    XD["Cross-device restore pass (two phones)"] --> D1P2["D1 part 2<br/>'Changing phones?'"]
    FS["Fold study (people)"] --> G2
    FS --> G6
    FS --> FX["Fold-study fixes<br/>(Proof captions)"]
```

| Step | Why it comes here | What it unlocks | What it deliberately does **not** depend on |
|---|---|---|---|
| **0 · Guards — ✅ COMPLETE** (merged 2026-09-26: PR #75, merge `eb75cf7`, commits `8ed6d29` + `f455cd9`) — F4 `PrivacyManifestTest` (SDK-28 pin) + dependency allow-list; F1a schema-shape guard at v3; F3 fixture archive (writer-produced at `5f7707a`, v1–v3 JSON, fictional content). Deferred advisories A2/A3/A4 are not blockers | Cheapest step, and it protects everything after it. The D1 audit found that a symmetric writer+stager change passes every test while breaking the beta.4-r3/beta.5 backup files **already in makers' hands**. The fixture is the only thing that catches that | Safe work on the backup sheet (step 1), the v4 bump (step 6), and the D1 part 2 verification | Owner rulings, HTML, device, UI |
| **1 · D1 part 1** (after **1a**, the `backup-restore.html` amendment) | Highest maker value per line of change. It gives every maker one true fact about their own backup, and it fixes words that mislead today: a safety title with no backup, backup errors in restore wording, a failure that points at nothing the maker can find. It is also the smallest visible change | D1 part 2, once the cross-device pass is done; step 1b | O1, F3 (already done in step 0), the schema, the fold study |
| **1b · Restore honesty** (if [Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects) schedules it) — **not ready: no spec yet** | The D1 audit found shipped messages that contradict what happened: "Restore cancelled." after zines were added, "Couldn't read that file" after a successful restore, a full disk called "damaged", a poisoned local photo blamed on the backup file, and "Backup cancelled." after a complete file was saved. Same sheet and same error mapping as step 1, so it follows it. Constitution Article 5 treats these as bugs. **Before it can open:** a spec (a Brief 01 section or its own short brief), and its restore states drawn in the same `backup-restore.html` amendment as step 1 (1a), so the frozen file is amended once | — | Schema, O1, the cross-device pass |
| **2 · D4-A1 + `ZineActionScrim` fix** | Fixes two **shipped** accessibility defects: TalkBack order disagrees with what is drawn on top, and an unlabelled scrim button. No HTML step, no visual change, no owner ruling; the ordering rule is a ZINELY-DESIGN-SYSTEM §4.5 clause, which that document's amendment rule requires to be **recorded as an ADR** (next free number on `main`: 119). It can run **before** step 1 if 1a isn't frozen yet | Step 3 (named undo shares the one-speaker announcement decision); part B later | HTML amendments, schema, the fold study |
| **3 · D4-A3 named undo** | Makers can hear and see what Undo did. It replaces the hard-coded English "Changed page N". Its `v21-bench.html` amendment replaces a frozen post-undo "Put back" toast that Compose never built. It comes after step 2 because both touch the announcement channel and share one TalkBack listen pass | — | `History` / `committing()` changes (labels derive from commands), schema |
| **4 · Tap-through hit test** (scope per [Q3](ZINELY-1X-DECISION-GATE.md#q3-tap-through-hit-testing-how-far-it-reaches)) | **Moved up** from the last slot. **ADR:** either an ADR amending the `core:editor` "depends only on `:core:model`" module rule, or an injected outline lookup that needs none ([Brief 05](BRIEF-05-MATERIALS-FRAMES.md#interaction-details)). Today a tap inside any of the six holed pieces selects the piece, never the photo beneath, so the photo can't be reached without *Send backward*. The box hit test is inherited behaviour, so changing it is an **interaction change**: Q3 chooses holed pieces only, all Art, or no change, and a `v21-bench.html` behaviour note comes first. Pure `core:render` logic plus tests, with a touch tolerance for thin pieces | D5 | O9, the stretch ruling, the fold study |
| **5 · D3 stage 1** (after the **print study**) | The print study must first establish what Android print paths actually do. Advice the maker can't follow is worse than none. Stage 1 is informational copy inside Proof's Print step (a `v21-proof.html` amendment) | Q7 (test page, reach profile, the "Actual size" promise, the inset) | O13, a test page, any inset change (its own later session with an HTML amendment and an ADR amending ADR-012, only if Q7 calls for it). **The release does not wait for this step** |
| **Release wave 1** | The v4 bump makes a whole library backup unrestorable on any older build. Makers should first have a release containing steps 0–5 on the **current** format, so the format change ships alone and is easy to reason about. A release session; the owner approves the shape in [Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects). Steps 0–4 (plus 1b if scheduled) are enough; step 5 joins only if the print study has reported | Step 6 | The print study, step 5 |
| **6 · F1b schema v4, alone** | One bump, with nothing else in the session. **While v4 is held, no release is cut from `main`** between steps 6 and 7: the `description` field (no UI), the layout-engine marker, `explicitNulls=false`, an identity v3→v4 migrator, and tests for all six version readers against the fixtures. **Step 0 made this mechanical:** the build fails on purpose until the bump adds `schema-shape-v4.txt`, `document-v4.json` with its expected document and its `FIXTURE_SHA256` pin, and moves `WRITER_PINNED_VERSION` to 4 (`DocumentFixtureCorpusTest`). The v1–v3 fixtures and the v2 archive are frozen and are never regenerated. **v4 is held unreleased until step 7 lands**, so the field ships with its first user and no second v4 shape exists. If Q2 picks refusal and D2 rides v4, the hold and the no-release rule extend to step 8. Release notes must say backups made after it need this version or newer | Step 7; step 8 by the release boundary | Any feature UI; `look` (D6-(2) takes its own bump) |
| **7 · D4-B alt text** | Photos become describable to a blind reader, but only in Read on the maker's phone: `PdfDocument` has no alt-text API, so printed and exported zines gain nothing. It needs a Read-semantics design first, because Read mode speaks nothing from the page today | A reader's edition, later | D2 |
| **8 · D2 voices** | The most visible creative gain, but the least ready. It needs Q2, Q4 and Q5, PR #70's acceptance, a family-aware coverage table, and an editing surface that uses the document's own fonts. It rides v4 only if it lands before v4 is released and Q2 chooses refusal; otherwise it takes v5 or accepts clipping (Q2) | — | D4-B |
| **9 · D5 frames** | Last, because it needs the most rulings (Q3, Q5, Q6) and step 4. Frames are content work (drawn outlines) on top of fixed mechanics | The Art backlog | Schema (frames are decor) |

**Proposed insertion: PR #70 acceptance before step 3** (🟦 added 2026-09-25 by the final planning review; the
removal is already approved, so this is sequencing, not a ruling). The draft branch `editor/remove-unavailable-font`
forks from `ad86586`, before beta.5, and changes `v21-bench.html`, `EditorScreen.kt` and `DECISIONS.md`
(ADR-115). Steps 3 and 4 amend the same frozen file, and step 3 changes `EditorScreen.kt`. Merging or closing
PR #70 first (after its TalkBack and HTML-parity acceptance) avoids two open amendments to one frozen spec and
an ADR-number collision. ✅ The branch no longer merges cleanly into `main` (conflicts in `CHANGELOG.md` and `DECISIONS.md`), so it needs a rebase first. If it can't be settled by then, step 3 rebases onto whichever lands first.

**Runs in parallel (people, not code):**
- the **fold-clarity study** (gates steps 8–9 via Q5). Its findings get their own small session, **fold-study
  fixes**: Proof's fold captions, in one `v21-proof.html` amendment shared with step 5 where timing allows;
- the **print study**: printers on A4 and Letter **and** 2–3 first-time makers following the guidance
  (gates step 5);
- the **cross-device restore pass** (gates D1 part 2, a small session of its own afterwards).

**What changed from the earlier order, and why:**
- **D1 moved ahead of D4-A1.** The D1 audit found concrete misleading states on the backup sheet, and every
  maker has a library to lose.
- **The hit test moved from sixth to fourth.** It fixes a shipped reach problem, needs only Q3 (decidable
  today), and every later frame depends on it.
- **The v4 bump is now its own step, after a release.** Previously it was bundled with features.
- **D2 moved out of wave 1.** It is blocked on three owner questions, PR #70's acceptance, and two
  engineering prerequisites.
- **The release no longer waits for print guidance.** Step 5 depends on a people study; it joins wave 1
  only if the study has reported.

**Later 1.x:** D6-(1) auto-levels (no schema); D7 page reorder; F5 then transparency; ink lift (if promoted);
starters (if ruled); copy-shop card; reader's edition.
**2.x:** 16 pages, Clippings tray, page exchange, drawing, iOS.

## 6. Cross-cutting rules for every direction

- **HTML first.** Every UI change amends a frozen prototype before Compose; the amendment is reviewed and
  re-frozen. Where this plan needs a frozen spec to change, the brief names the decision instead of making it.
- **Old zines keep working; preview matches print** (ROADMAP creative-tools constraints). Honest current
  state:
  - An **older build silently renders an unknown font as Inter**; `isRegistered` has no production callers.
    For a narrower face, that clips the last lines of a text box.
  - An older build draws **nothing** for an unknown `supplyId`, yet the invisible box is still selectable.
  - A newer zine reaches an older build only through a library-backup restore, or a downgrade.
  - So each direction that adds a font, look or supply either ships inside a schema bump (v4 only if it lands
    before v4 is released; otherwise the next number), or its release notes
    state the fallback. With the bump, the **whole backup** refuses to restore on older builds, not just that
    zine. Which failure to accept is an owner trade-off, never a default.
- **Schema v4 is not introduced casually.** It rides one planned bump (F1b), after wave 1 is released. A
  released version guards only the fields it shipped with; anything added later takes the next number.
- **Goldens:** read `*_compare.png` before any re-record; `bash tools/grun.sh gold` before claiming visual
  neutrality ([CLAUDE.md](../../CLAUDE.md)).
- **Both device passes** for every UI item. Read the platform tree (`uiautomator dump`) for semantics, but know
  that it shows child order, not TalkBack traversal. **Compose focus tests do not prove TalkBack behaviour**;
  only a listen on a real device does.
- **Step 0's guards stay green.** The fixtures and `schema-shape-v3.txt` are frozen (only the planned v4 bump,
  step 6, adds new ones); the dependency allow-list widens only by review ([§4](#4-foundations--only-what-the-repository-actually-needs) F4).
- **Public copy** moves an item to *Available* only when it is in a published tag ([ADR-118](../DECISIONS.md#adr-118)).

## 7. iOS preparation (architecture only)

**Findings (portability audit, re-checked at `5f7707a`).** The four-way classification (safe to share later /
Android-only by necessity / hard to port / things to avoid coupling further) and the eight coupling rules
every session must follow are in [audit §10](ZINELY-1X-READINESS-AUDIT.md#10-architecture--ios-implications).
Each brief has its own "iOS portability notes". The summary:

- The seven non-UI `core:*` modules (`core:ui` is Android Compose) are pure JVM (`kotlin.jvm`, JUnit 5 + jqwik) with **no `android.*` imports**.
  `model`, `render`, `editor` and `copy` have **zero** `java.*` imports; `imposition` has one (`BigDecimal`,
  `SvgProofSheetRenderer.kt:95`); `data` one (`MessageDigest`, `ContentHasher.kt:14`); `data-storage` has 46
  (java.nio, java.util.zip, MessageDigest) — it is the only module that needs real porting (okio/expect-actual).
- Import-free JVM calls: `codePointAt`/`Character.charCount` (`TextCoverage.kt:58-59`), `Math.addExact`
  (three data files), `"%02x".format` (two).
- **The Android seam is narrow:** one replayer (`CanvasReplayer`), one text layout (`SharedTextLayout`,
  StaticLayout), `PdfDocument`, `BitmapFactory` decoding (there is no Coil).
- **Pure files sit in Android modules.**
  - In `render-android`: `ExportScale`, `SelectionChromeGeometry`, `DocumentFontRegistry` and `CmapCoverage`.
    `CmapCoverage` uses one JVM `String(bytes, charset)` call.
  - In `feature:editor`: `EditorStore`, `EditorEffects`, `SupplyPlacement`, `FramingDraft` and `BenchState`.
  - Moving the `feature:editor` ones is not free: `EditorStore` and `EditorEffects` bring kotlinx.coroutines
    into `core:editor`, and `SupplyPlacement` and `EditorEffects` bring `core:copy`. Move a file only when a
    direction already touches it, and decide the dependency in that session.
- **1.x traps to avoid:**
  - D5's hit test belongs in `core:render` as a pure function, not in Compose pointer code.
  - D1's "last backed up" date belongs behind a small store interface, with DataStore in `data-android` only.
  - D2's coverage table belongs in `core:model`, verified against the font files by an Android test.
  - D4's spatial order belongs in `core:editor`.
- **Text is the real parity risk:** the document stores no line breaks and no layout version, so another
  platform could reflow a zine differently.

**Recommendation:**

| Question | Answer |
|---|---|
| Extract shared domain logic? | **It is already extracted.** Keep it that way: F6 denylist test if the owner opts in (optional, deferred); move the four pure files to core when next touched |
| Introduce KMP boundaries? | **Not yet.** Convert (`core:model` + `core:copy` first, then render/editor/imposition, data-storage last) only when iOS demand is evidenced. CI's `:core:*:test` becomes `jvmTest` then |
| Leave Android UI untouched? | **Yes.** Compose stays Android; nothing in 1.x should be designed around sharing UI |
| Native SwiftUI later? | **The likely answer** (KMP core + SwiftUI UI), decided by a 3–4-week spike whose exit criteria are in the [research §22.2](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#222-recommendation): text parity, VoiceOver custom actions on the canvas, AirPrint duplex, HEIC import, privacy manifest |
| Cheap insurance now | (1) F6; (2) a **text-determinism decision** before any second platform: bundled fonts only (true today) and a layout-engine marker recorded in the document with the next schema bump (F1) so reflow is at least detectable; (3) never let `java.time`/`UUID`/`String.format` into core (F6 catches them) |

## 8. Website

**Done 2026-09-25.** The beta.5 website update was published (merge `5f7707a`, live-verified) under
[ADR-118](../DECISIONS.md#adr-118). From here on, the rule is the same for every direction: public copy moves an
item to *Available* only after it is in a tagged release. Nothing from this plan appears on the site until the
owner accepts a direction, and then only as Planned or Exploring.

## 9. Owner decisions needed before implementation

This plan resolves none of these. The [decision gate](ZINELY-1X-DECISION-GATE.md) reduces them to eight
questions (five decidable today), with evidence and framing; this table is the full register. Each is a row in
[OWNER-CHECKLIST](../OWNER-CHECKLIST.md#15-product--design-authorship); the checklist is the index, and the
linked record owns the decision.

| # | Decision | Options | Blocks | Tracked at |
|---|---|---|---|---|
| O1 | **Phone migration** | A: allow device-to-device transfer (Android 12+ only); B: keep exclusions; show "last backup saved" (no reminder, Article 4); C: both | D1 scope (B is buildable without A) | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · ADR-030 §7 |
| O2 | **Starters** | Tribunal KEEP (sample zine, prompt library) vs ZINE-/BETA-DIRECTION "DO NOT BUILD templates". If built: Article 7's bright line — *a starter finished unmodified is a known failure* | D8 | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · [constitution Art. 7](../zinely-constitution.md#article-7--the-maker-makes-it) |
| O3 | **Ink lift** | promote from EXPERIMENTAL to Planned (after the asset-model ADR, F5) or leave | D6-(4) | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · research §14.2 |
| O4 | **Tray / X2 timing** | schedule X2 (answer D-029 Q1–Q3) or keep unscheduled | Clippings tray | [checklist §1.1 D-029](../OWNER-CHECKLIST.md#11-v2-spec-defects--designv2-spec-defectsmd) · OD-2 |
| O5 | **Paid-pack clock** | which build is the "first bundled-supplies release" (beta.2 / beta.3 / r3), and run, reshape or retire the IAP trial; not deciding ratifies free-forever | nothing in 1.x technically (F4 if run); the clock runs out ~Aug–Sep 2027 | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) |
| O6 | **Play signing** | upload the existing release key as the app signing key (side-loaded users can update) vs a Google-generated key (they must reinstall and restore) | Play publication, the ROADMAP's only *Planned* item; irreversible, so decide with O15 ([gate](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)) | [checklist §3 Play path](../OWNER-CHECKLIST.md#3-release--credentials) |
| O7 | **PR #70** | **Already approved** as a design judgment (ROADMAP In development); remaining is acceptance work (hands-on TalkBack, rendered HTML parity), not a decision | D2 sequencing | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · ROADMAP In development |
| O8 | **Font set** | confirm ZINE-DIRECTION's three voices (Averia · Fraunces · Inter) — PRD §13 Q3 is still open | D2 | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) (existing Q3 row) |
| O9 | **Art backlog selection** | which of the ~19 researched supplies (and which frames) ship, and who authors the outlines | D5 | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · ADR-107 R1 |
| O10 | **Typebar/Reframe freeze status** | confirm whether `v21-typebar.html`/`v21-reframe.html` are frozen (headers vs ZINE-DIRECTION N2 disagree) | D2's HTML amendment route | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · V21-SPEC |
| O11 | **Page count (D-030)** | fixed eight forever, or variable pages in a later phase | D7 16-page (not reorder) | [checklist §1.1 D-030](../OWNER-CHECKLIST.md#11-v2-spec-defects--designv2-spec-defectsmd) |
| O12 | **V2-CONSTITUTION Amendment 1 vs the Hand voice** | (a) Hand only for short text, disabled above a threshold; (b) amend the constitution; (c) two voices (Book + Plain) | D2 | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · [Brief 02](BRIEF-02-THREE-VOICES.md#experience) |
| O13 | **Printer reach profile** | test page gives guidance only (part 1), or a remembered reach also widens the keep-clear warning (part 2; amends `v21-bench` and the keep-clear rulings) | D3 part 2 | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · [Brief 03](BRIEF-03-PRINTER-TEST-PAGE.md) |
| O14 | **Fold-study order** | may D2 (a creative tool) start before the fold-clarity study reports? The ROADMAP forbids reordering that gate without owner approval | D2 timing | [checklist §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship) · [ROADMAP](../ROADMAP.md#exploring--recommended-next-decisions-in-order) |
| O15 | **Android developer verification** | register as a verified developer before it applies: from 30 Sep 2026 in four countries, globally in 2027 — side-loaded APKs will need it, so this gates GitHub distribution too, not only Play | every public APK after the cut-over | [checklist §3](../OWNER-CHECKLIST.md#3-release--credentials) · [research §23](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#23-website-strategy) |

The gate also asks two questions this register did not have: **tap-through scope**
([Q3](ZINELY-1X-DECISION-GATE.md#q3-tap-through-hit-testing-how-far-it-reaches)) and **the next release and the
restore-honesty defects** ([Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)).

## 10. Handoff

- **The owner's next sitting:** the [decision gate](ZINELY-1X-DECISION-GATE.md). Q1 (developer
  verification) has a calendar date. Step 0 is complete. None of the eight questions blocks steps 1, 2 or 3 of [§5](#5-sequencing) (step 1b waits for Q8 and a spec; step 3 follows PR #70's acceptance); steps 1 and 3 still need the owner to approve their HTML amendments. Q3 gates step 4 and Q8 the release.
- **Starting a step:** open one fresh session with the
  [handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md), filled in from the step's brief and the recorded
  rulings. One step per session.
- **Within a step:** the ADR comes first. Check the next free number in `DECISIONS.md`; ADR-115 is reserved by
  PR #70. Then the HTML amendment, then Compose.
- **Step 0 is complete** (PR #75). The next implementation step is 1 (after its `backup-restore.html`
  amendment is approved), or 2, which needs no owner ruling.
- **Nothing in the planning set changed code, tests, frozen specs, ADRs, the website or any release branch.**

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Re-based onto `eb75cf7` after Step 0 merged (PR #75). Step 0 marked complete with its commits; F1a/F3/F4 marked done; deferred advisories A2/A3/A4 and F6 recorded as non-blocking (§2/§7 "F6 now" wording aligned); §6 adds the Step 0 guard rule; step 6 names the fixture moves Step 0 now enforces; Tests snapshot and CI line citations re-checked. No step reordered, no owner question decided. |
| 2026-09-25 | Plan and five briefs written at `5c40e7b`; two reviews reconciled. |
| 2026-09-25 | After two independent reviews (technical, product): v4 contents and release boundary fixed (v4 = alt-text field + marker, released with alt text; `look` later; D2 after that takes v5); tap-through made an owner-scoped interaction change (Q3); restore-honesty step 1b and Q8 added; release no longer waits for the print study; fold-study fixes, makers in the print study and the inset slot added; O1 reminder removed; O6 and O7 corrected; alt-text value stated honestly. |
| 2026-09-25 | Re-based onto `5f7707a` after the beta.5 release. Readiness-audit corrections folded in: foundations F1 split (guard now, bump later), F2 undo labels derived from commands, F3 not a D1 prerequisite, F4 SDK-28 pin and allow-list kept. Sequence revised after the D1 deep audit. Decision gate and handoff template added. |
