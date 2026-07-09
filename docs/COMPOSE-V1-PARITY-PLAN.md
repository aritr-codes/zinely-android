# Compose V1 Parity Plan — implementing the frozen HTML trilogy

> **Status:** planning artifact (2026-07-08). This is an **engineering execution plan**, not a
> product/scope change and not an ADR. It sequences the Compose implementation of the three
> DESIGN-FROZEN V1 surfaces — [Shelf](design/v1/shelf.html), [Bench](design/v1/bench.html),
> [Proof](design/v1/proof.html) — onto the existing, functionally-complete app. Scope authority for
> *what ships* stays with [PRD](PRD.md)/[ROADMAP](ROADMAP.md); technical authority stays with
> [ARCHITECTURE](ARCHITECTURE.md). This plan owns only *the order and shape of the parity work*.
>
> **Prime directive:** the frozen HTML is the specification. Compose **implements** it; it never
> reinterprets it. Any post-freeze UX change updates the HTML spec first, then Compose
> ([CLAUDE.md HTML-first workflow](../CLAUDE.md)).

---

## 0. The one fact that reshapes the plan

This is a **re-skin of a working app**, not a green-field migration.

The app is functionally complete through `v0.6.0-alpha.1`, and its **physical print test passed**
(100% scale, correct fold, 1→8 page order/rotation/scale — [ROADMAP version milestones](ROADMAP.md#version-milestones-semver)).
The three frozen surfaces already exist as shipped, tested screens:

| Frozen surface | Existing screen(s) | Redesign distance |
|---|---|---|
| **Shelf** | `HomeScreen` + `HomeViewModel` | Reskin only — layout/state sound |
| **Bench** | `EditorScreen` (+ ~20 sub-composables) + `EditorStore` (MVI) | Reskin of complex but sound structure |
| **Proof** | `PreviewScreen` + `ExportScreen` + `CompletionScreen` (3 screens) | Reskin **and** unify into the 3-act Proof |

Consequences for sequencing (these are the improvements over a naïve "build it up" order):

1. **The imposition engine is done and already physically validated.** `core:imposition`
   (`SingleSheet8Imposer`, `Convention.kt`, `LayoutValidator`, `GoldenProofTest`, [ADR-007](DECISIONS.md#adr-007),
   95+ tests) shipped in S1 and folded correctly in the alpha print test. "Build the imposition
   engine" is **not** a milestone. What remains is a **reconciliation checkpoint** (M4): make the
   Compose Proof *consume* `core:imposition` and prove the HTML's illustrative grid matches it.
2. **Behavior is reskin-invariant.** Nav graph + `@Serializable` routes + the shared-editor-VM seam,
   the MVI store + reducer + ViewModels, the Hilt/KSP graph, the accessibility semantics mirror, and
   the Roborazzi `preview==export` parity harness all survive the reskin untouched.
3. **The design-system layer is entirely net-new.** There is no CompositionLocal token layer, no
   shared component library, `Type.kt` is still Android-template boilerplate, and there are **zero
   haptics**. This is where nearly all the work lives — and why tokens/theme/type/motion/haptics is
   correctly the first milestone.

### ✅ Resolved — the frozen Proof sheet was corrected to the validated engine (2026-07-08)

The Proof's Act-1 "here's your printed sheet" illustration originally encoded a **different**
single-sheet-8 imposition from the **physically-validated shipping engine** — they disagreed in **6 of
8 cells**. The conflict was independently re-derived (a from-scratch fold simulation), and the HTML's
layout was proven **physically wrong**: its front cover (page 1) was back-to-back with page 4, so
opening the cover would land the reader on page 4, not page 2. The engine is correct and authoritative
(`Convention.kt` `TOP_ROW_ROTATED`; RESEARCH R1.2 VERIFIED oracle citing Chandra/NASA + Cambridge +
university guides; [ADR-007](DECISIONS.md#adr-007); alpha physical print test passed).

**Resolution taken — spec correction (option B).** `proof.html`'s Act-1 illustrative imposed-sheet
`LAYOUT` was corrected to mirror the engine (`[6,7,0,1,5,4,3,2]` → `[4,3,2,1,5,6,7,0]`), the only
change being the illustration and the developer comments that describe it — no interaction, motion,
typography, spacing, colour, copy, or layout change; DESIGN-FROZEN status retained with a recorded
freeze amendment. Independently reviewed cell-by-cell (all 8 cells + rotations now equal the engine)
→ **GO**.

| row | col0 | col1 | col2 | col3 | source (now identical) |
|---|---|---|---|---|---|
| top (flipped) | 5 | 4 | 3 | 2 | engine `TOP_ROW_ROTATED` **and** corrected `proof.html` |
| bottom | 6 | 7 | 8 | 1 | cover page 1 at (1,3), back cover page 8 at (1,2) |

**Consequence for the plan:** there is **no imposed-sheet parity carve-out**. The Proof's Act-1 sheet
is now a single source of truth with the engine — Compose consumes the engine, and the frozen HTML
illustration already matches it, so the imposed sheet is full pixel parity like every other region.

---

## Phase 1 — Readiness review (independent)

An independent Review Agent validated the three frozen files byte-for-byte against each other and
the design docs. **Verdict: GO.**

Key confirmations (evidence-checked, not summary-trusted):

- **Tokens byte-identical** across all three `:root` / `:root[data-theme="dark"]` blocks
  (`--paper:#F4EFE6`, `--coral:#E76F51`/`--coral-strong:#C64E34`/`--coral-text:#A63C22`,
  `--desk:#E7DECE`, shadows, `--field`, `--menu`). Bench/Proof headers say "inherited verbatim from
  the frozen Shelf" and the bytes confirm it.
- **Typography, motion, haptics, focus, reduced-motion all identical** across surfaces:
  `--shell:"Inter"` / `--voice:"Fraunces"`; `--ease:cubic-bezier(.2,.7,.3,1)`, `--fast:130ms`,
  `--base:230ms`; `HAPTIC={tick:[8],snap:[6,20,10],boundary:[24],success:[12,30,12,30]}`;
  `:focus-visible` 3px `--coral-strong`; blanket `prefers-reduced-motion` override.
- **Nav model coherent** — loss-safe back at every level, Shelf→Bench→Proof, autosave stated.
- **Freeze stamps mutually compatible**; imposition **panel numbering** consistent (0=Front cover …
  7=Back cover in both Bench and Proof). The Proof's imposed **sheet geometry** — originally an open
  item — has since been reconciled to the validated engine (see the [Resolved](#-resolved--the-frozen-proof-sheet-was-corrected-to-the-validated-engine-2026-07-08) note and F1).
- **Accepted, documented sub-AA limitations** (bench teal-as-text 2.9:1; proof dark graphical-text
  3:1 inside `role=img`; snackbar-focus-after-dismiss RI-4) — disclosed in-file, not hidden.

### Findings carried forward (reconciled by the Implementer)

| # | Finding | Class | Reconciliation |
|---|---|---|---|
| **F1** | proof.html originally declared `LAYOUT=[6,7,0,1,5,4,3,2]` a *topological* derivation "NOT physically folded" — **verified physically WRONG** (front cover's leaf-mate was page 4, not page 2), diverging from the validated engine in 6 of 8 cells. | Required (imposition) | **RESOLVED (2026-07-08).** The engine is authoritative (alpha print test). Rather than a permanent parity carve-out, the frozen `proof.html` Act-1 illustrative sheet was **corrected** to the engine layout (`→ [4,3,2,1,5,6,7,0]`) as a reviewed spec correction (option B), DESIGN-FROZEN retained. The imposed sheet is now a single source of truth with the engine — **no carve-out**; Compose consumes the engine and the frozen HTML already matches it. M4 remains an engine-truth reconciliation checkpoint (below), now with zero spec conflict. |
| **F2** | All three files load Inter+Fraunces from `fonts.googleapis.com`. That CDN path cannot ship (privacy invariant). Fraunces not yet bundled. | Recommended | **ACCEPT.** Pulled **into M0** as a hard prerequisite — pixel parity is impossible without the bundled display face. Inter is already bundled for export ([ADR-028](DECISIONS.md#adr-028)). |
| **F3** | On-device keyboard/TalkBack pass is non-automatable; pending for all three (bench gesture-verb semantics; proof fold step-nav + climax reveal). | Recommended | **ACCEPT.** Becomes explicit device-pass gates in M2/M3/M5 and the M6 sign-off. |
| **F4** | `DESIGN-LANGUAGE.md §2` + `mockups/tokens.css` still describe the **superseded** identity (old palette `#3A3A3C`/`#E7DFD0`, "marker/handwritten" type). Single-source-of-truth risk. | Recommended | **ACCEPT as a doc task, OUT of this code plan.** Flagged as pre-M0 doc reconciliation (below). Not done here — this plan makes no documentation rewrites. |
| **F5** | Interior panels are "Page 1–6" (Bench) vs "pages 2–7" (Proof comments) — prose only; Proof never renders interior numbers. | Observation | Note only; standardize user-facing language only if Proof ever surfaces interior numbers. |

**Pre-work flagged, not performed here:** reconcile the stale `DESIGN-LANGUAGE.md §2`/`mockups/tokens.css`
palette+type to point at the v1 tokens as canonical (F4), so no future implementer trusts the dead
palette. It is a documentation edit and belongs to a separate change.

---

## Phase 3 — Architecture review (existing Compose)

### Keep as-is (reskin-invariant — do not touch)

| Area | Evidence | Why it survives |
|---|---|---|
| Navigation | `ZinelyNavHost`, `EditorRoute` — single-Activity, `navigation-compose`, type-safe `@Serializable` routes, shared-editor-VM seam via `getBackStackEntry` ([ADR-046](DECISIONS.md#adr-046)) | Graph + back-stack policy are UI-agnostic; the reskin changes pixels, not routes |
| Editor state | `EditorStore` (MVI mailbox), `EditorReducer` (pure), `EditorModel`/`EditorUiState` ([ADR-005](DECISIONS.md#adr-005)) | Reducer/undo/gesture-coalescing are behavioral; visuals sit above |
| MVVM screens | `HomeViewModel`, `ExportViewModel` — `StateFlow` + `Channel` events, `collectAsStateWithLifecycle` | Stateless-hoisted screens already; swap chrome, keep state |
| DI | Hilt + KSP, `SingletonComponent`; `EditorAppModule`, `HomeModule`, data-android modules | Presentation-independent |
| A11y semantics | `ElementSemanticsLayer` (per-element focusable mirror, [ADR-029](DECISIONS.md#adr-029)), `EditorA11y` shared step constants (one intent, three input paths), live regions, ≥48dp discipline | **Crown-jewel asset** — preserve verbatim; new chrome must re-attach these hooks |
| Render parity | `PagePreviewParityTest` hand-rolled Bitmap pixel-equality; Roborazzi + Robolectric NATIVE goldens | `preview==export` guarantee must stay green through the reskin |

### Replace / build net-new (the redesign surface)

| Gap | Current reality | Needed |
|---|---|---|
| **Design-token layer** | Palette = 9 top-level `Color` vals mapped onto **abused** M3 roles (`surface`=paper, `background`=desk, `onSurfaceVariant`=paper-ink); no on-desk family, no coral-strong/coral-text/ink-faint; spacing/motion are inline literals (`12.dp`, `tween(150)`) | A `CompositionLocal` design system: full ~14-token riso palette, spacing scale, motion tokens (`--ease`/`--fast`/`--base`), shape/radius scale, haptic vocabulary. Threaded via `LocalZ*` providers wrapped by `ZinelyTheme` |
| **Typography** | `Type.kt` is template default — `FontFamily.Default`, only `bodyLarge` set | Two bundled voices: **Fraunces** (display/`--voice`) + **Inter** (shell/`--shell`), the full type scale, weight vocabulary |
| **Haptics** | **None exist** | Four-verb haptic API (`tick`/`snap`/`boundary`/`success`) over `HapticFeedback`/`Vibrator`, reduced-motion-gated |
| **Motion** | 2 `AnimatedVisibility` fades + static decorative tilts; `rememberReduceMotion()` lives in a feature file | Shared motion spec (durations/easings/enter-exit/spring), the Proof Fold climax choreography; centralize `rememberReduceMotion()` |
| **Shared components** | **No component library** — buttons/dialogs/snackbar/cards/banners all inline per screen, duplicated (`Surface(shape,color,tonalElevation)` card pattern repeated; two near-identical banner composables) | `Z*` component layer: buttons, sheets, scrim+`inert` modal, snackbar, toast, cards, dialogs, loading/error chrome, **accessible-control wrappers** that carry the semantics pattern |
| **Prototype dock** | HTML has a review-only Act/State/Theme/Width/Motion dock | Becomes a **debug-only** `@Preview`/parity harness — **not shipped UI** |

### Tech-debt / risk notes surfaced

- `EditorScreen` takes the whole `EditorStore` and collects internally (vs pure `(uiState, dispatch)`)
  — mild preview/test friction; the reskin can optionally hoist it but **need not** (out of scope
  unless it blocks parity).
- `addTextAndEdit` is a two-dispatch UI sequence owed a single reducer intent — pre-existing,
  **not** a parity concern; leave for a behavioral change.
- **Least-protected surfaces to redesign:** `PreviewScreen` and `ExportViewModel.export` have **no
  covering tests** — Proof (M5) carries the most test-writing burden, not just reskin.
- Visual goldens (`SelectionChromeGoldenTest`, render goldens) **will need re-recording** after
  chrome changes — expected, budget for it in every UI milestone.

---

## Phase 2 + 4 — Milestone roadmap

Infrastructure before UI; then surfaces in ascending risk (Shelf → Bench → Proof); imposition
reconciliation slotted before Proof but parallelizable because `core:imposition` is a pure,
UI-independent module. No dates ([per the brief](../CLAUDE.md)); complexity is relative.

```mermaid
flowchart LR
    M0["M0 · Design-system\nfoundation"] --> M1["M1 · Shared\ncomponent library"]
    M1 --> M2["M2 · Shelf\nparity"]
    M1 --> M3["M3 · Bench\nparity"]
    M4["M4 · Imposition\nreconcile + validate\n(parallel, pure core)"] --> M5["M5 · Proof\nparity (the Fold)"]
    M2 --> M5
    M3 --> M5
    M0 -.-> M4
    M5 --> M6["M6 · Full-app\nreview + sign-off"]
    M2 --> M6
    M3 --> M6
```

### M0 — Design-system foundation — ✅ DONE (2026-07-09, [ADR-048](DECISIONS.md#adr-048))
- **Goal:** the frozen `:root` token contract, expressed once in Compose, ready to thread. Palette,
  typography (bundled Fraunces + Inter), motion tokens, haptic vocabulary. No screen changes.
- **Files (as shipped):** the theme package moved `:app` → `:feature:editor` (same FQN
  `com.aritr.zinely.ui.theme`, because `:app` depends on `:feature:editor`, never the reverse);
  `Theme.kt` (token `CompositionLocal`s + `ZinelyTheme` accessors; `dynamicColor` **deleted**),
  `Type.kt` (bundled families); **new** `ZinelyColors.kt`, `ZinelyElevation.kt`, `ZinelyMotion.kt`,
  `ZinelyHaptics.kt`, `ZinelyDimens.kt`, `ReduceMotion.kt`; `Color.kt` **deleted**; fonts under
  `feature/editor/src/main/res/font/` + OFL licences in `assets/fonts/`; `VIBRATE` in the manifest.
- **Dependencies:** none (leaf infra). **F2 font bundling landed here.**
- **Review criteria:** every token value equals the frozen `:root` bytes (light + dark); dynamic
  color stays OFF; Fraunces/Inter load from bundled assets with **zero network**; reduced-motion
  primitive centralized; no M3-role abuse leaks into the public token API.
- **DoD:** ✅ `ZinelyTokensTest` — 15 green, every colour/shadow/duration/easing/haptic/dimension
  pinned to the frozen literals; full `:app` + `:feature:editor` suites green; `assembleDebug`
  packages all five faces with no `INTERNET` permission and no HTTP client. No `@Preview` swatch
  screen (nothing to swatch until M1 has components).
- **Deviations from the plan (recorded in ADR-048):** **no spacing scale, no radius/shape scale** —
  the frozen CSS defines neither, so shipping one would create a second source of truth beside the
  HTML. Components carry their own frozen values from M1. Elevation ships as *data*; the
  multi-layer-shadow `Modifier` lands with its first caller in M1.
- **Carried into M1/M2:** the pre-reskin M3 `colorScheme`/`Typography` are preserved byte-for-byte
  (un-migrated screens still read them); the role abuse unwinds per-screen. Fraunces is the static
  **9 pt** cut — `opsz` needs API 26, minSdk is 24 — and M2's pixel-parity gate is the check on it.
- **Complexity:** Medium.

### M1 — Shared component library
- **Goal:** the reusable `Z*` chrome the HTML shares across surfaces, each carrying a11y by
  construction.
- **Components:** buttons (primary/ghost/icon), scrim + `inert`-modal sheet, snackbar (`role=status`
  + Undo), toast, cards, dialogs, loading/error chrome (replacing the inline per-destination
  `CircularProgressIndicator`/error `Column`s), an **AccessibleControl** wrapper encoding the
  "spoken label + `Role.Button` + ≥48dp + cleared decoration" pattern, and the debug-only prototype
  harness.
- **Files:** **new** `ui/components/` (`ZButton.kt`, `ZSheet.kt`, `ZSnackbar.kt`, `ZToast.kt`,
  `ZCard.kt`, `ZDialog.kt`, `ZLoadError.kt`, `ZAccessibleControl.kt`); consumes M0 tokens.
- **Dependencies:** M0.
- **Review criteria:** each component matches the frozen shared-component behavior across all three
  surfaces; a11y semantics baked in (not left to callers); reduced-motion honored; stable test tags
  preserved/introduced.
- **DoD:** per-component Compose UI tests + Roborazzi goldens; a11y assertions; no inline
  re-implementation remains for these primitives on migrated screens.
- **Risks:** premature generalization (build only what ≥2 surfaces share; leave one-offs inline);
  the `inert`-modal focus-trap parity in Compose.
- **Complexity:** Medium.

### M2 — Shelf parity
- **Goal:** `HomeScreen` matches `shelf.html` pixel/motion/interaction/a11y.
- **Files:** `feature/editor/HomeScreen.kt` (+ its inline dialogs migrate to M1 `ZDialog`);
  `HomeViewModel` untouched (behavior invariant).
- **Dependencies:** M0, M1.
- **Review criteria:** parity gate (below) passes vs frozen Shelf; empty-shelf invitation, paper
  chooser, tilted cards, thumbnails, undoable delete snackbar all match; `HomeViewModel` state
  contract unchanged.
- **DoD:** parity goldens seeded from the Shelf HTML; existing Home tests green (tags preserved);
  device TalkBack/keyboard pass (F3); DESIGN-RULES 12-point checklist ✅.
- **Risks:** lowest — Home is well-tested and structurally sound. **This is the reference
  implementation that proves the parity harness for M3/M5.**
- **Complexity:** Low–Medium.

### M3 — Bench parity
- **Goal:** `EditorScreen` + sub-composables match `bench.html`, preserving MVI, gestures, and the
  a11y mirror.
- **Files:** `feature/editor/EditorScreen.kt` and the ~20 sub-composables (`EditorContextBar`,
  `EditorSupplyTray`, `EditorPageStrip`, `EditorPagePreview`, `SelectionChrome`, `ResizeHandles`,
  `EditorEmptyState`, saved/failure banners, `DeskText`, …); `EditorStore`/reducer untouched.
- **Dependencies:** M0, M1.
- **Review criteria:** parity gate vs frozen Bench; `ElementSemanticsLayer` + `EditorA11y`
  one-intent-three-paths preserved verbatim; gesture/undo behavior unchanged; the four haptic verbs
  wired to gesture commit/selection/snap/boundary.
- **DoD:** parity goldens; **re-recorded** `SelectionChromeGoldenTest` + editor goldens; all editor
  Compose tests green; device gesture-verb TalkBack pass (F3); 12-point checklist ✅.
- **Risks:** highest-surface-area reskin; re-attaching a11y semantics to every replaced chrome
  element; not regressing gesture/graphicsLayer performance; teal-as-text accepted-limitation stays
  documented.
- **Complexity:** High.

### M4 — Imposition reconciliation (engine-truth checkpoint)
- **Goal:** make the Compose Proof *consume* the validated `core:imposition` engine for its Act-1
  sheet. The freeze-vs-engine conflict is **already resolved** (F1 — the frozen HTML was corrected to
  the engine), so this is a pure wiring + guard checkpoint. **Not an engine build.**
- **Files:** **no `core:imposition` production change expected**; **new** *engine-truth* golden
  asserting the Compose imposed sheet == `SingleSheet8Imposer`/`Convention` output; consume
  `SvgProofSheetRenderer`/`ImpositionLayout`/`Convention` in the Proof preview.
- **Dependencies:** M0 (styling only); otherwise independent of M1/M2/M3 — **runs in parallel**.
  Blocks M5's Act-1 sheet.
- **Review criteria:** Proof derives its sheet from the engine and **never** re-encodes a raw layout
  array; the alpha physical-print result is cited as ground truth; the engine-truth golden is green.
  Because the corrected frozen HTML now matches the engine, the Compose sheet satisfies **both** the
  engine-truth golden and frozen-HTML pixel parity — no carve-out.
- **DoD:** engine-truth golden green; the shipping engine (not any prototype array) is the sole source
  of the imposed sheet in Compose; no raw imposition-layout literal anywhere in Compose.
- **Risks:** **Low.** Physical correctness is solved and the spec conflict is closed. The one
  low-level trap: accidentally porting a layout array instead of calling the engine.
- **Complexity:** Low.

### M5 — Proof parity (the Fold)
- **Goal:** unify `PreviewScreen` + `ExportScreen` + `CompletionScreen` into the frozen 3-act Proof
  (Sheet → Print → Fold), with the Fold climax getting the whole delight budget.
- **Files:** `feature/editor/PreviewScreen.kt`, `ExportScreen.kt`, `CompletionScreen.kt` (reconciled
  into the 3-act flow); consumes M4 imposition output; `ExportViewModel` behavior invariant.
- **Dependencies:** M0, M1, M4 (and M2/M3 patterns).
- **Review criteria:** parity gate vs frozen Proof — the honest print recipe (100%/Actual-size,
  Landscape, paper-match, single-sided), dead-band honesty, the 5-step fold guide, and the staged
  climax reveal (settle → shelf-line callback → words → actions) with the `success` haptic; print
  honesty never overstates what the printer can do. The Act-1 imposed sheet follows the engine (M4)
  **and** matches the corrected frozen HTML — full pixel parity, no exception.
- **DoD:** parity goldens; **new** tests for the previously-untested `PreviewScreen` and
  `ExportViewModel.export`; reduced-motion path degrades to the correct static finished state;
  device pass (F3); the mandated Physical-Workflow re-review + Design-Director emotional review both
  GO; 12-point checklist ✅.
- **Risks:** highest delight/risk surface; unifying three screens into one flow without regressing
  the shipped export/share behavior or the auto-land-on-completion nav ([ADR-041](DECISIONS.md#adr-041));
  climax choreography under reduced-motion.
- **Complexity:** High.

### M6 — Full-application review + parity sign-off
- **Goal:** whole-trilogy coherence and the non-automatable gates.
- **Scope:** cross-surface motion/theme/haptic consistency; dark-theme + tablet + landscape +
  reduced-motion passes on all three; re-record and verify all goldens; on-device TalkBack/keyboard
  sweep (F3); confirm zero network / bundled fonts (F2); confirm accepted limitations still
  documented (teal-as-text, dark graphical-text, RI-4 snackbar focus).
- **Dependencies:** M2, M3, M5.
- **Review criteria:** the trilogy reads as one product; every DESIGN-RULES checklist green; parity
  goldens green in CI.
- **DoD:** adversarial full-app Review Agent GO; no open Required fixes; parity harness green.
- **Complexity:** Medium.

---

## Phase 5 — Compose parity strategy

Parity is *verified*, not asserted. Each surface passes the same gate before it is considered frozen
in Compose.

| Parity dimension | How it is validated |
|---|---|
| **Pixel** | Roborazzi goldens (Robolectric NATIVE, real Skia) **seeded from the frozen HTML** — capture the HTML surface at reference widths, diff the Compose render against it; differences become classified review findings, fixed or explicitly accepted (never silently ignored). Reuse the existing hand-rolled Bitmap-diff pattern (`PagePreviewParityTest`) where `captureToImage()` is unavailable headless. The Proof Act-1 imposed sheet is additionally guarded by the M4 engine-truth golden; since the corrected frozen HTML now matches the engine, that region has **no parity carve-out**. |
| **Motion** | Compare against the frozen `--ease`/`--fast`/`--base` tokens (M0); assert durations/easings come from the token layer, not literals; verify the Proof climax beat sequence. |
| **Interaction** | Compose UI tests exercising the same flows the HTML defines (open/back loss-safety, sheet/scrim/modal, snackbar Undo, paper chooser, fold step-nav); stable test tags preserved. |
| **Accessibility** | Automated: semantics assertions (labels, `Role.Button`, live regions, ≥48dp) in Compose tests; the `ElementSemanticsLayer` mirror preserved. Manual: on-device TalkBack + keyboard pass per surface (F3) — the non-automatable gate. |
| **Theme** | Light + dark goldens per surface; token values proven equal to both frozen `:root` blocks; accepted sub-AA limitations stay documented. |
| **Tablet / landscape** | Golden + interaction runs at phone / tablet / landscape widths matching the HTML prototype dock's Width control. |
| **Reduced-motion** | Prove the static state is already correct (the existing invariant: "motion is decoration on an already-correct static state"); both animated and reduced paths tested, gated by the centralized `rememberReduceMotion()`. |

**Freeze-in-Compose gate per surface:** pixel + motion + interaction + a11y + theme + tablet +
reduced-motion parity all green, the [DESIGN-RULES 12-point checklist](design/DESIGN-RULES.md) all ✅,
the device a11y pass done, and an independent Review Agent GO. Proof additionally re-runs the
Physical-Workflow and Design-Director reviews.

---

## Phase 6 — First milestone recommendation

**Start with M0 — the design-system foundation.** Nothing else may begin until it lands.

Why M0 first:

1. **Everything references it.** Every surface, every shared component, every golden is defined *in
   terms of* the tokens. Parity is literally undefined until the palette, the two type voices, the
   motion tokens, and the haptic vocabulary exist in Compose.
2. **It is the highest-leverage error site.** The Review Agent confirmed the frozen `:root` block is
   byte-identical across all three surfaces — a single source of truth. Encode it wrong once and
   every downstream surface inherits the divergence. Encode it right once and three surfaces inherit
   correctness.
3. **It clears a hard privacy gate.** Fraunces must be **bundled** (F2) — the CDN path in the HTML
   cannot ship. Pixel parity is impossible until the display face renders from a bundled asset with
   zero network. That gate belongs at the very front.
4. **It is pure infra — cheap to review in isolation.** No screen changes, so it is unit- and
   snapshot-testable against the frozen spec and reviewable without any UI churn, before a single
   pixel of surface work risks rework.

Why everything else waits: M1 components are *styled with* M0 tokens; M2/M3/M5 surfaces are *built
from* M1 components; M4 renders *through* M0 styling. Building any surface before the tokens exist
means restyling it twice.

**Immediately after M0:** M1 (shared components), then **M2 (Shelf) as the reference reskin** — it
is the lowest-risk, best-tested surface, so it proves the parity harness and the DS end-to-end before
the high-risk Bench (M3) and Proof (M5) consume them. M4 (imposition reconciliation) can run in
parallel on the pure-core track at any point after M0.

---

## Constraints honored by this plan

No Compose implementation begun · no ADR written · no HTML changed · no existing documentation
rewritten · no product redesign · no feature added. This is planning and readiness only. The F4 doc
reconciliation and all code milestones are **proposed**, to be executed as separate reviewed changes.
