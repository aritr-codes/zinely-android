# M0–M3 project reconciliation (Compose V1 parity re-skin)

> **Status:** reconciliation report (2026-07-11). Read-only. This document **does not** rewrite
> history, ADRs, the ROADMAP, or any implementation. It reconciles the [COMPOSE-V1-PARITY-PLAN](../COMPOSE-V1-PARITY-PLAN.md)
> (2026-07-08) and the DESIGN-FROZEN trilogy against the **actual repository state** at branch
> `feat/m0-design-system` (HEAD `f1edf45`), and closes with a recommendation for M4.
>
> Every claim below is grounded in the repo (commits, ADRs, source `file:line`), not in the plan's
> own summaries — per the Documentation Rule and the review principle *repository state beats summaries*.

## What was reconciled against

| Source | State read |
|---|---|
| DESIGN-FROZEN trilogy | `docs/design/v1/{shelf,bench,proof}.html` — verified `proof.html:727` |
| Parity plan | `docs/COMPOSE-V1-PARITY-PLAN.md` (2026-07-08) |
| ADRs | `docs/DECISIONS.md` — ADR-007, ADR-014–018, ADR-048, ADR-049 |
| Implementation | `core/imposition/*`, `feature/editor/*`, `app/.../export/*`, git log `a787aed..f1edf45` |
| Reviews | `docs/reviews/2026-06-27-*`, `docs/reviews/2026-07-04-alpha-release-assessment.md` |
| Roadmap | `docs/ROADMAP.md` |

**Milestone status (ground truth):** M0 ✅, M1 ✅, M2 ✅ (code), M3 ✅ (code) — all committed on the
**unmerged** branch `feat/m0-design-system`. M4/M5/M6 do not exist yet.

---

## 1. Assumptions that proved correct

1. **"A re-skin of a working app, not a green-field migration"** (plan §0). Held. M0–M3 changed pixels,
   not behaviour: no route, reducer, or DI change appears in the M2/M3 commit ranges. Nav / MVI store /
   Hilt graph / a11y mirror survived untouched, exactly as the plan's "keep as-is" table predicted.
2. **The frozen `:root` token block is a single source of truth**, byte-identical across the three
   surfaces. M0 encoded it once ([ADR-048](../DECISIONS.md#adr-048)) and every later surface reads it.
3. **The imposition engine is done and physically validated; "build the engine" is not a milestone.**
   `core:imposition` is unchanged since S1, still `Accepted` under [ADR-007](../DECISIONS.md#adr-007),
   ~95 tests green including property-based (`ImpositionPropertiesTest`, jqwik) and golden SVG tests.
4. **Behaviour is reskin-invariant.** Confirmed: the reskin batches carry `feat(bench)`/`feat(shelf)`
   subjects only; no behavioural intents were added.
5. **The design-system layer was the real work and correctly the first milestone.** M0 and M1 are the
   only two milestones that produced ADRs — because they are the only two that introduced new
   architecture (the token `CompositionLocal` layer; the `Z*` component library).

## 2. Assumptions that proved false (or already overtaken)

1. **F1 was never actually "open" by the time M0 started.** Memory and prior framing carried the
   frozen-Proof-vs-engine conflict (6/8 cells) as an *open owner decision*. In fact `proof.html:727`
   already reads `const LAYOUT = [4,3,2,1, 5,6,7,0]` — the spec was corrected to the engine on
   2026-07-08, before any Compose parity work. **The conflict is closed. The stale item is the memory
   note, not the repo.**
2. **The plan names the wrong engine artifact for M4.** M4 says "consume `SvgProofSheetRenderer` … in
   the Proof preview." But `SvgProofSheetRenderer` emits an **SVG string**, which Compose/Android cannot
   render natively; it exists only as the L2 golden/proof artifact. The real device render path is
   `ImpositionLayout → SheetComposer/CanvasReplayer` (`render-android`) replaying `DrawCommand`s through
   the `contentToSheet` affine. **M4 must not wire `SvgProofSheetRenderer` into Compose.**
3. **M4's "derive, never re-encode + guard test" is largely already implemented — and predates the
   plan.** The S7.2 checkpoint (`57ed568`, 2026-07-04) removed a hardcoded Compose layout array (it had
   drifted to `5·4·3·6 / 8·1·2·7`) and replaced it with derivation from the convention. Today
   `ExportScreen.decorativeImpositionRows(SingleSheet8.TOP_ROW_ROTATED)` (`ExportScreen.kt:258-270`)
   derives order/rotation by inverting `spec.cellOf`, with **no literal array**, guarded by
   `DecorativeImpositionOrderTest`. The anti-drift discipline M4 was going to introduce is in place.
4. **The frozen Proof Act-1 sheet is decorative, not a live imposition render.** `proof.html` builds
   Act-1 from a JS `LAYOUT` grid; the Compose equivalent is the same *decorative 4×2 thumbnail* pattern
   `ExportScreen` already ships (`DecorativeImpositionSheet`, `clearAndSetSemantics{}`, "Not a live
   render"). So M4's job is **derivation parity**, not standing up a live proof renderer.

## 3. Milestones that shifted

- **M3 was executed as four explicit batches B1–B4** (selection chrome → stage/artifact → editor chrome
  → overlays), not the single milestone the plan wrote. This is a healthy split of the highest-surface
  reskin and should be the template for M5.
- **M2 was executed as ~8 sequential feature commits** (cover recipe/glyphs → cover object → card/grid
  → sheets → chrome/states → fail-and-retry → HomeScreen assembly), also finer-grained than the plan's
  single "Shelf parity" milestone.
- **M4 shrank below even its "Low" complexity rating.** With derivation + drift guard already present
  and the Act-1 sheet being decorative, M4 reduces to: relocate the convention-derived decorative sheet
  into the unified Proof Act-1 and assert it there. It is now a candidate to **fold into M5**.

## 4. Work that disappeared (planned, then correctly not built)

| Dropped | Why | Recorded in |
|---|---|---|
| `ZDialog` | Frozen trilogy has zero dialogs; sheets are the only modal idiom | ADR-049 |
| `ZCard` | The recurring surface is the physical `ZPaperSurface` | ADR-049 |
| Prototype dock as shipped UI | Review scaffolding; Roborazzi drives theme/size directly | ADR-049 |
| Spacing scale / radius scale | Frozen CSS defines neither — shipping one = a second source of truth | ADR-048 |
| Hover states | Spec's own `@media (hover:none)` precedent | ADR-049 |
| `dynamicColor` param | Deleted, not defaulted-false | ADR-048 |
| `BOTTOM_ROW_ROTATED` convention | Dropped from v1 | ADR-007 |
| Hardcoded Compose imposition array | Replaced by convention derivation (had drifted) | S7.2 `57ed568` |

## 5. Work that moved

- **Theme package** relocated `:app` → `:feature:editor` (same FQN `com.aritr.zinely.ui.theme`), because
  `:app` depends on `:feature:editor` and never the reverse (ADR-048).
- **Font bundling (F2)** pulled forward into M0 as a hard privacy prerequisite (Fraunces bundled; CDN
  path cannot ship).
- **Multi-layer shadow `Modifier`** deferred from M0 → landed in M1 with its first caller
  (`ZinelyShadow.kt`).
- **Loading/error chrome migration** moved from a component task to per-screen (M1 → applied in M2).
- **Imposition-order truth** moved from a hardcoded Compose array → derived from `ConventionSpec`
  (S7.2, pre-plan).
- **Device a11y passes (F3)** moved into per-milestone gates (M2/M3/M5) plus the M6 sign-off.

## 6. Architecture decisions that became permanent

- **[ADR-007](../DECISIONS.md#adr-007) — imposition engine.** `SingleSheet8.TOP_ROW_ROTATED`
  (`cellOf`/`rotationOf`/`roleOf`), pure-Kotlin zero-Android core, printer-free SVG-proof validation.
  Unchanged, physically validated (alpha print), now consumed by both the real export path (`:app`
  `ZineExporter`) and, decoratively-by-derivation, in Compose. Permanent.
- **[ADR-048](../DECISIONS.md#adr-048) — M0.** Tokens live in `:feature:editor` (no new
  `:core:designsystem` module); express only globally-frozen tokens (no spacing/radius scale);
  `dynamicColor` deleted. Permanent foundation every surface reads.
- **[ADR-049](../DECISIONS.md#adr-049) — M1.** `ZSheet` is custom Dialog-hosted (M3 `ModalBottomSheet`
  rejected); sheets are the only modal idiom; component library = patterns shared by ≥2 surfaces.
  Permanent chrome vocabulary.
- **The imposition consumer contract.** Renderers apply `PanelPlacement.contentToSheet` directly and
  **never** re-derive rotation from `rotation`+`bounds`; the single source of imposition truth is the
  `ConventionSpec`. Now enforced in Compose by `DecorativeImpositionOrderTest`. Permanent.

## 7. Engineering practices that should now be institutional

1. **Derive, don't duplicate — and guard the derivation.** The `5·4·3·6` drift incident proved that any
   UI-side copy of a core truth *will* drift. Rule: **no layout/order/rotation literal in UI code**;
   derive from the core convention and pin it with a drift test. (Already true for the export sheet;
   make it the explicit M4/M5 gate for the Proof Act-1.)
2. **Property-based tests for the pure core.** `ImpositionPropertiesTest` (jqwik) catches invariants
   example tests miss (half-turn self-inverse, tiling, affine composition). Keep for `core:model`/`render`.
3. **Batch a high-surface reskin into reviewable slices.** M3's B1–B4 is the model for M5.
4. **HTML-first, spec-correction-before-code.** F1 was handled in the right *spirit* — the frozen HTML
   was corrected before Compose consumed it — but the correction needs a durable ADR home (see §8).
5. **Golden-on-CI-only.** Local Roborazzi is a capture no-op (alpha F4); parity is verified on the
   pinned CI image. This must be stated at every UI milestone's DoD (and see the M2 golden flag in §8).

## 8. Reconciliation flags (documentation drift / loose ends — no code touched)

These are the gaps between what the repo *did* and what the durable docs *record*. None is a code
defect; all are Documentation-Rule debt.

1. **ROADMAP.md is stale.** It contains **no M0–M6 parity rows**, stops at 2026-07-07, and never
   references the parity plan, ADR-048, or ADR-049 — yet M3 Bench work is committed. This violates
   "every roadmap change → ROADMAP.md." The parity track is invisible to the roadmap.
2. **The F1 freeze correction has no ADR.** `proof.html` was edited post-DESIGN-FREEZE
   (`[6,7,0,1,5,4,3,2]` → `[4,3,2,1,5,6,7,0]`) and narrated as a reviewed GO — but only inside the
   parity plan, which **explicitly disclaims ADR authority** ("not an ADR"). There is no ADR-007
   amendment and DECISIONS.md contains no record of the correction. A post-freeze change to the
   authoritative spec must live in an ADR (or a recorded freeze amendment), not a planning artifact.
3. **M2 shelf goldens are untracked and misplaced.** 10 `shelf_*.png` sit uncommitted in
   `feature/editor/src/test/roborazzi/` — the **editor** module, though these are Home/Shelf goldens —
   and the matrix is incomplete (`loading`/`many` missing some theme×device variants). M2 pixel-parity
   is not locked on the branch.
4. **No ADR for M2/M3.** Defensible — both are pure reskins introducing no architecture decision, so the
   absence is arguably correct (only M0/M1 changed architecture). Flagged as a judgment call, not a
   definitive violation.
5. **The whole re-skin is unmerged.** M0–M3 live off `main` on `feat/m0-design-system`.
6. **Alpha open items intersect the reskin.** B3 (preview-text parity, triaged as bug-until-disproven on
   the shared `SceneRenderer→PagePreview` path) and B4 (editor right-side gap) touch M3/M5 surfaces —
   confirm neither regressed before M6 sign-off.

---

## Recommendation for M4

**Do not proceed with M4 unchanged, and amend the roadmap.** Both are needed; they are cheap and
independent of the reskin code.

**Amend the roadmap track (documentation, before or alongside M4):**
- Add the M0–M6 parity track to ROADMAP.md with M0/M1/M2/M3 marked done (flag #1).
- Record the F1 freeze correction as an ADR-007 amendment or a dedicated ADR (flag #2).
- Fix M2 golden placement + commit/CI-verify them (flag #3).

**Refine M4 itself (do not run the plan's version verbatim):**
- **Drop** "consume `SvgProofSheetRenderer`" — wrong artifact; Compose cannot render its SVG (§2.2).
- **Recognise** the derive-and-guard core already exists (`decorativeImpositionRows` +
  `DecorativeImpositionOrderTest`, §2.3) — M4 is not greenfield discipline, it is *relocation*.
- **Rescope** M4 to: bring the convention-derived decorative Act-1 sheet into the (M5) unified Proof,
  assert the engine-truth derivation there, and keep the drift guard. Because the Act-1 sheet only
  exists *inside* the unified Proof, **M4 is a strong candidate to fold into M5** — or to remain a
  one-test checkpoint executed with M5, not a standalone milestone.

**Net:** the engine half of M4 was already solved (S7.2) and the spec conflict is already closed. What
remains of M4 is small, mostly a relocation into M5, plus the documentation debt in §8. The roadmap
should be amended to make the parity track visible and to give the F1 correction an ADR home before M4
formally opens.

*Stop point per brief: no code, ADRs, ROADMAP, or implementation changed by this report.*
