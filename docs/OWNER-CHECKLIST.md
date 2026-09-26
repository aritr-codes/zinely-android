# Owner checklist — what is blocked on you

**This document owns nothing.** It is an index of work an AI implementer *cannot* close, pointing at the
document that does own each item. Per the [Documentation Rule](../CLAUDE.md#documentation-rule-mandatory),
every ruling, defect and decision below lives in its authoritative file — this page only says where to look
and why you are the only one who can close it.

**Maintained by the Implementer Agent.** Items are added as they are discovered and struck as you close
them. If anything here could be closed by an implementer, that is a bug in this list — say so and I'll take it.

**Last swept:** 2026-08-18 · **89 open items** · ⛔ **5 of them are merge blockers** (§below)

> **2026-09-25 (1.x plan, proposal):** 1.x owner rows added to §1.5 and §3 (developer verification), two
> existing rows reframed, and the `Mirror` row flagged as likely stale. ⚠ **The 89 above was already stale:**
> counting open `☐` table rows and `- ☐` bullets gives **70 at `5f7707a` and 81 after this change**. That
> method may miss items written in other shapes (§2.1–§2.2), so the header is left for a full sweep rather
> than replaced with a number nobody re-derived section by section.

> ⚠ **The count read 70 and the file held 80.** Corrected 2026-08-18 by counting the rows rather than
> trusting the header — which is how it drifted: each new row incremented a number nobody re-derived.
> This is an index of work owed to the owner, so an undercount is the failure mode that matters. The
> per-section tally, **counted from the rows, not carried forward**:
> `26 · 10 · 7 · 14 · 7` (§1) · `6 · 4 · 3` (§2) · `8` (Play Store) · `4` (§4) = **89**.
>
> ⚠ **It drifted twice more on 2026-08-18 alone, both times the same way.** The header read 87 while the
> file held 86; adding a row incremented it to 88, so it was wrong before *and* after. Re-derived to 87,
> then a further row made it 88 — this figure is the count of `☐` rows, taken after the last edit.
> 🟦 A number kept by hand beside a list kept by hand will keep doing this. Same shape as
> [D-096](design/V2-SPEC-DEFECTS.md#d-096), where the durable fix was to stop writing the fragile part by
> hand — here that means **deriving the header from the rows**, not remembering to bump it.

---

## Why an item lands here

| Reason | Meaning |
|---|---|
| **Decision** | The frozen spec doesn't rule on it; choosing would be redesigning a frozen surface |
| **Judgement** | A first-time-user reading — knowing the implementation *disqualifies* me ([Pass 2](../CLAUDE.md#pass-2--first-time-user-verification)) |
| **Physical** | A real device, a real printer, a real pair of ears |
| **Credential** | A key, an account, a push, a person to send it to |

---

## ⛔ Merge blockers — `feat/supplies-p3-art-sheet` is **NO-GO** today

Independent merge-readiness review, 2026-08-18. The branch is **12 commits / 421 files / +81,878** — it
carries the whole **V2.1 re-skin (Library, Bench, Proof) plus supplies P1–P3**, not the Art sheet its name
describes. ⚠ It cannot be split: `3c3d152` is a single-parent squash of PR #58 that already fused Library,
Proof, Bench and supplies P1/P2 into one 390-file commit. **Merge it whole — but rename it.**

| # | Blocker | Category | Yours or mine |
|---|---|---|---|
| **B1** | ✅ **CLOSED 2026-08-19.** `record-goldens.yml` run `32244557532` on the pinned image (`ubuntu-24.04`, JDK 21, UTC/en_US), and all **8 PNGs** committed from that one run — the three Art-sheet goldens and D-090's five Windows-recorded decor goldens, exactly as this row scoped it. The diff was read before it was blessed: the three Art-sheet goldens change **15.65 % / 22.32 % / 18.56 %** of frame, every changed pixel inside the tile grid (`bbox=(64,102,722,841)`, and `(64,102,722,1063)` where the recents row extends it), which is the eight tiles going disabled → live and nothing else — no layout shift, no glyph change, no heading moved. The five decor goldens differ in **bytes only**: zero pixels differ by more than 2, so they are a provenance fix, not a visual one. | ~~Release Blocker~~ → **Closed** | Was **yours**; the workflow only produces artifacts and never commits, so running it and reading the diff was mine to do. The PNGs land in PR #60 |
| **B2** | ✅ **CLOSED 2026-08-22.** [D-089](design/V2-SPEC-DEFECTS.md#d-089-ruling) keeps both surfaces and stacks the placement snack above the complete measured context bar with 12dp clear between their painted bounds. The coexistence case and its 1.8×-text growth are regression-tested. | ~~Release Blocker~~ → **Closed** | Owner ruled; implemented in `e47554f` |
| **B3** | ✅ **CLOSED 2026-08-20.** [D-092](design/V2-SPEC-DEFECTS.md#d-092-ruling) ruled with D-086 and D-093: the fixings get their own sizing constant (0.20 · 1:1), the override cap stays at one entry, and the four families are unchanged. A photo corner lands square. | ~~Release Blocker~~ → **Closed** | Was **yours**; ruled 2026-08-20 |
| **B4** | **Device Pass 2 on the supplies drawer needs *your* reading.** ✅ Pass 1 re-run 2026-08-20 on the built APK (`1204db9`) and passed. Pass 2 was also run and raised [D-103](design/V2-SPEC-DEFECTS.md#d-103) + [D-104](design/V2-SPEC-DEFECTS.md#d-104) — but it was run **by the implementer**, which [Pass 2](../CLAUDE.md#pass-2--first-time-user-verification) disqualifies from judging whether the screen explains itself. What is owed is a first-time reading, not the pass. [Definition of done](../CLAUDE.md#definition-of-done-for-a-change) item 4 requires **both** ⚠ *(This row read "has never been run" until 2026-08-20 — it was left stale by the very commit that recorded the run, in the same file.)* | **Release Blocker** | **Yours** (already #3 below) |
| **B5** | ✅ **CLOSED 2026-08-25.** [ADR-107](DECISIONS.md#adr-107) is accepted with a staged 16→32 first wave and an explicit no-randomisation boundary. The existing sixteen no longer ship under a proposed ADR. | ~~Release Blocker~~ → **Closed** | Owner ruled; D-080 separately tracks the new surface's visual freeze |

⚠ **[D-102](design/V2-SPEC-DEFECTS.md#d-102) — the merge blocker I filed is WITHDRAWN; a different,
real problem replaces it.** The branch merges into **`origin/main`** (its actual target) with 12 conflicted
files / 36 hunks, all ordinary squash-workflow rewriting. My *"46 commits behind, 104 conflicts"* was
measured against a **local `main` that has never been pushed**.

⛔ **What is real: local `main` holds 45 commits `origin` has never seen** — V2 Library Phase B and Phase D,
2026-07-30 to 2026-08-09 — while `origin/main` advanced along the supplies line, which local `main` lacks
entirely. **Two lines of real work, neither containing the other, one of them existing only on this
machine.** A disk failure loses 45 reviewed commits. 🟦 **Push local `main` and reconcile it deliberately,
as its own act** — not folded into a feature merge. **Decision + Credential** (it needs a push).

⚠ **A red `feature:editor` run still needs its evidence classified** — a genuine regression, a golden owed a
re-record (B1), or [D-101](design/V2-SPEC-DEFECTS.md#d-101)'s environment-specific Robolectric decoder window.
That decoder residue is distinct from **#57**, whose order-dependent unavailable-photo fixture was replaced by
a deterministic loader seam on 2026-09-10. Do not clear a red gate by retrying it: inspect the failing test and
decoder evidence. Before trusting a green Reframe suite, still read the `skipped` count because unrelated
decode-capability guards can intentionally skip on hosts whose native decoder is unavailable.

**Known limitations to carry into release notes** (do **not** block the merge): the PDF/vector parity suite now
has real-device evidence (5/5, SM-A176B / Android 16, 2026-08-25), all sixteen frozen Art supplies are live,
and D-083/D-103 are closed. The owner completed the first-person TalkBack listen pass and physical print/fold
checks on 2026-08-25; beta-cohort photocopier feedback remains ongoing evidence rather than a release gate.

~~⚠ **Also true and not about this branch:** local `main` is **46 commits ahead of `origin/main` and
diverged** — `git pull --ff-only` fails.~~ ✅ **No longer true (checked 2026-09-23):** local `main` and
`origin/main` are the same commit (`72e620e`, PR #73's merge), and `git merge --ff-only origin/main`
succeeds. Whether the 45 local-only commits described above were integrated or dropped was not
re-verified here.

---

## Start here — the six that actually hold things up

Everything else can wait behind these.

| # | Item | Why it's first |
|---|---|---|
| **1** | **[Back up the keystore](RELEASING.md)** (§ below, item R-1) | **Irreversible.** The passwords exist only on this machine and were never printed. The first symptom of skipping it is a build you cannot ship — and every tester uninstalling and losing their zines |
| **2** | ~~**[D-089](design/V2-SPEC-DEFECTS.md#d-089-ruling) — snack vs. context bar**~~ | ✅ **CLOSED 2026-08-22.** The snack stacks above the measured context bar with the owner-ruled 12dp clear gap; focused normal- and large-text coverage remains green. |
| **3** | **Pass 2 on the supplies drawer — *your* first-time reading** | Blocks the same merge. ✅ Pass 1 re-run and passed 2026-08-20 against the built APK (`1204db9`, SM-A176B / Android 16). Pass 2 was run by the implementer and raised [D-103](design/V2-SPEC-DEFECTS.md#d-103) + [D-104](design/V2-SPEC-DEFECTS.md#d-104); the reading that closes it cannot be his |
| **4** | **[Accept or reject ADR-098](DECISIONS.md#adr-098)** | Status is `Proposed`, deliberately. **Phase D cannot open at all** until you rule, and twelve further decisions sit behind it |
| **5** | **[CI-22](V1-CONFORMANCE-INVENTORY.md) — re-freeze the V1 HTML** | The declared **critical path** of the whole V1 conformance programme (CI-40 → CI-64 → CI-74). C0 is *not started* |
| **6** | **[CI-14](reviews/CI-14-motion-baseline-protocol.md) — motion & haptics baseline** | "Startable today." Until it exists, *any* duration change is made against a tie-break nobody has taken |

---

## 1. Rulings & decisions

### 1.1 V2 spec defects — [`design/V2-SPEC-DEFECTS.md`](design/V2-SPEC-DEFECTS.md)

Amending a frozen V2 surface is reserved to you (V2-CONSTITUTION §VI); an implementer may not edit the freeze.

| ☐ | Rule | Question | Blocks |
|---|---|---|---|
| ☑ | ~~[D-097](design/V2-SPEC-DEFECTS.md#d-097)~~ | **FIXED 2026-08-18** — an untouched Reframe announced *"Framing saved."*, pushed an undo step and autosaved, 56 % of the time. One shared `FramingMath.sameFraming` now decides it for both callers; **13 new tests**, incl. the 288-case aspect table ADR-109 asks for — walked against the real `Framing`, not a copy of it | ✅ **Device Pass 1 done** (SM-A176B / Android 16, 16:9 photo): untouched commit leaves `document.json` unchanged to the byte and second, and one Undo reverts the *real* reframe. ⚠ **The spoken line was not heard** — a live region is not dumpable; fold *"Framing unchanged."* into the next TalkBack listen pass |
| ☑ | ~~[D-089](design/V2-SPEC-DEFECTS.md#d-089-ruling)~~ | **CLOSED 2026-08-22.** Keep both surfaces and stack the snack above the context bar with 12dp clear between their painted bounds. | Implemented in `e47554f`; coexistence and 1.8× text are regression-tested |
| ☑ | ~~[D-092](design/V2-SPEC-DEFECTS.md#d-092)~~ | **CLOSED 2026-08-20.** The sizing key is not the family: *Tape & fixings* splits into tape (0.55 · 4.5:1) and fixings (0.20 · 1:1), enumerated rather than prefix-matched. ⚠ A test was pinning the defect — it asserted `fix.corner` lands at the same size as tape, green, because it was written from the implementation. |
| ☑ | ~~[D-093](design/V2-SPEC-DEFECTS.md#d-093)~~ | **CLOSED 2026-08-20 — reading 1, with its mechanism replaced.** The tile renders the authored outline itself rather than a hand-drawn glyph; `BenchArtGlyphs` is deleted and `v21-bench.html`'s glyphs are generated from `SupplyCatalog` (A7). ⚠ "Make the tiles solid" as recommended would have been wrong for the three marks that have real holes, and would not have found `mark.halftone`'s glyph drawing **seven** dots for a **sixteen**-dot mark. |
| ☑ | ~~[D-094](design/V2-SPEC-DEFECTS.md#d-094)~~ | **CLOSED 2026-08-29 — `Photo` keeps the framed-picture mark and `Art` uses A15/A16's owner-approved collage of two paper scraps and one printed dot.** | Compose transcribes the frozen geometry; labels, targets, traversal and TalkBack descriptions are unchanged, while the distinct mark adds a non-colour cue |
| ☑ | ~~[D-103](design/V2-SPEC-DEFECTS.md#d-103)~~ | **CLOSED 2026-08-22 — A9 chose legibility without capping text scale.** The fallback tile draws `Not / yet` on two intentional lines while its disabled state still speaks `Not available yet`. A11 subsequently authored the final four supplies, so no unavailable tile is currently reachable; the fallback remains correct for a future temporarily unauthored entry. | Implemented and regression-tested in `883455d`; the completed cabinet at font scale 1.8 is pinned by `BenchArtSheetTest` and `BenchArtSheetPlatformA11yTest` |
| ☑ | ~~[D-104](design/V2-SPEC-DEFECTS.md#d-104)~~ | **CLOSED 2026-08-22 — the frozen sixteen are complete.** `TAPE & FIXINGS` now contains Torn tape and Paper clip; `CUT PAPER` contains Torn strip and Marker underline. A11 froze the four paths before implementation without changing membership, names, order or headings. | Device-verified on SM-A176B; this does not accept ADR-107's proposed larger library |
| ⛔ | [D-105](design/V2-SPEC-DEFECTS.md#d-105) | **Three files you told me never to touch lost their uncommitted changes** — `README.md`, `docs/RESEARCH.md`, `gradle.properties`, discarded by a `git reset --hard` I ran for my own convenience on 2026-08-20. Unrecoverable: never staged, no VS Code history, no shadow copies. **Nothing is owed to me here** — the row exists so the loss is on the record rather than in a chat log | **Not a ruling.** If you can reconstruct any of the three, that is the only open action, and it is yours |
| ☐ | [D-096](design/V2-SPEC-DEFECTS.md#d-096) | **Cite the frozen mockups by name, not by line number?** ~50 line citations into `v21-bench.html` were audited and **all are now resolved** — but the audit's own scope was wrong: a full sweep of `docs/` found **72** citations across **ten** documents, not ~50 across four. Pass 2 fixed **31 live-and-wrong** (11 in `ZINE-DIRECTION.md` alone) and marked **14 as historical records**, including all of `docs/proposals/` and `docs/reviews/`, which are dated and correct as they stand. ⚠ The *policy* is still yours: whether every future citation must carry a name | ⚠ Third pass at this (A5 booked it, A6 inherited it). A bare line number cannot distinguish "look here" from "this is what it said then", so the sweep cannot hold. Recommended: cite `§openSupply()` / `.keepclear` / `A5`, which amendments do not move |
| ☑ | ~~[D-095](design/V2-SPEC-DEFECTS.md#d-095)~~ | **CLOSED 2026-08-18, reading 1** — the `Registration` tile drew a plus crossing a ring; the authored mark stops its arms on the ring and leaves the centre bare. Tile redrawn in `v21-bench.html` (**amendment log A6**) and in `BenchArtSheet.kt` | Closed the day it was filed because the answer was **forced** — even-odd means the outline cannot move. ⚠ **No precedent for D-086 / D-092 / D-093 / D-094**, which are open design questions |
| ☑ | ~~[D-086](design/V2-SPEC-DEFECTS.md#d-086)~~ | **CLOSED 2026-08-20 — a fourth reading none of the three candidates was.** The four unauthored tiles draw **no mark** and carry `Not available yet`, the string `stateDescription` always spoke. Drawing a glyph for a supply nobody has authored was depicting an invention, which A5's DEPICT latitude cannot license. | Ruled 2026-08-20 with D-092 / D-093 |
| ☑ | ~~[**ADR-107**](DECISIONS.md#adr-107)~~ | **CLOSED 2026-08-25 — staged expansion accepted.** First grow 16→32 inside the four families; keep the remaining candidates as backlog; ship local search, filters, Recent and Favourites; do not randomise catalogue, outlines, placement or composition. | Scope ruled. The separate D-080 row below owns rendered visual freeze |
| ☐ | [**ADR-109**](DECISIONS.md#adr-109) | **Accept or reject** one photo spanning two facing pages. The engineering is cheap and needs nothing from you — two ordinary images, no schema bump, no imposition change. **What is yours is the control:** `v21-bench.html` must gain a spread action (recommended home: the selected-photo context bar, not the Add chooser) before any Compose work | Design freeze. The ADR's own skeptical pass rates this the feature's weakest point, not its schema |
| ☐ | [ADR-109](DECISIONS.md#adr-109) §skeptical pass | **Where does the maker first see the join?** Every screen in the product shows **one page at a time**, deliberately — Bench edits one page, Read refuses a spread view on the record (ADR-101 P5), Fold draws topology, Print draws nothing. So the first sight of the continuous image is the **printed sheet**. Rule on the copy and the moment | ⚠ This is the feature's acceptance criterion. It **cannot** be answered by adding a spread preview — that is the redesign ADR-101 P5 already refused |
| ☐ | [D-100](design/V2-SPEC-DEFECTS.md#d-100) | **`SUPPLIES-SPEC §3.4.1`'s uniform-scale rule was never built**, and two KDoc blocks said it was. A maker can stretch `shape.circle` into an ellipse and `mark.registration` into an oval today. ⚠ Because supplies are **fill-only even-odd outlines**, a stretched registration mark breaks the exact tangent [D-095](design/V2-SPEC-DEFECTS.md#d-095) was filed to fix — *the mark D-095 made correct can be made incorrect again with a drag*. Lock `mark.*`+`shape.circle` · strike §3.4.1 · or accept the ellipse | **Decision** — all sixteen handles behave identically today, so making four differ is a change to a frozen interaction, not a bug fix. 🟦 Rule with [D-092](design/V2-SPEC-DEFECTS.md#d-092): that one asks what a supply *lands* at, this one what a maker may do to it after |
| ☐ | [README.md](../README.md) · [CLAUDE.md](../CLAUDE.md#documentation-system) | ⚠ **The doc index is eight documents behind, and I am not allowed to fix it.** `README.md` owns *"index of all docs"*. This branch adds `SUPPLIES-SPEC.md`, `ZINE-DIRECTION.md`, `ZINE-WORLD.md`, `BETA-DIRECTION.md`, `PRODUCT-DIRECTION.md`, `V21-SPEC.md`, `V21-RESEARCH.md` and **this file** — and indexes none. `e3141f0` even edited `CLAUDE.md` to make `OWNER-CHECKLIST.md` canonical while the index it names does not know it exists | **Structural.** `README.md` is on my [never-touch list](#standing-constraints-i-observe), so this Documentation-Rule breach is the one item on this page **no implementer can close by working harder**. Either lift the constraint for the index rows, or add them yourself. 🟦 I can draft the exact rows on request |
| ☐ | [D-098](design/V2-SPEC-DEFECTS.md#d-098) | **Should the imposition engine check its own output at runtime?** `LayoutValidator` is a complete structural checker — panel/page bijections, tiling, transform consistency, fold topology — with **no production caller**: it runs only in `core:imposition` tests. Options: leave it test-only · call it in debug builds · call it in the export path and surface issues as an export error | **Decision.** ⚠ Not the defect — D-098 fixed the two docs that *claimed* it already runs. This is the question those docs made it look like we had already answered |
| ☑ | ~~[D-080](design/V2-SPEC-DEFECTS.md#d-080) + [D-107](design/V2-SPEC-DEFECTS.md#d-107)~~ | **CLOSED + IMPLEMENTED 2026-08-26 — A16's combined Art + Ink iteration is DESIGN FROZEN and shipped in Compose.** The named three-column material cabinet, compact family filters, Recent/Favourites rails, visible current ink, stable 5×2 ink grid and honestly labelled starting palettes are implemented. | Document/render semantics are unchanged; focused Compose, platform accessibility and visual regressions protect the package |
| ☑ | ~~[D-108](design/V2-SPEC-DEFECTS.md#d-108)~~ | **CLOSED 2026-08-26 — keep the already-frozen live `Ink`, remove fake `Font` / `Size` buttons, and say `More styles after Done`.** | OD-52; HTML DESIGN FROZEN before Compose; focused tests, goldens and normal/maximum-font Samsung passes complete |
| ☑ | ~~[D-083](design/V2-SPEC-DEFECTS.md#d-083)~~ | **CLOSED 2026-08-22 — the visible pigments remain `Ink`; the accessible swatches are `Spot ink` and `Neutral ink`, while the opener remains the verb `Ink`.** | Implemented in `883455d`; platform `AccessibilityNodeInfo` coverage proves all three names are distinct. The manual listen-pass row below remains intentionally open |
| ☑ | ~~[D-081 Q10](design/V2-SPEC-DEFECTS.md#d-081)~~ | **CLOSED 2026-08-24 — A13 seeds each shared-photo cascade from images already on the actual current page.** Text/decor do not count; the ordinary picker remains centred. | The consecutive one-photo "it lost my photo" misread is now fenced by `ShareInDrainTest` |
| ☑ | ~~[D-079](design/V2-SPEC-DEFECTS.md#d-079)~~ | **CLOSED + IMPLEMENTED 2026-08-24 — option (b).** The sole product-level privacy sentence lives in the Shelf-owned Colophon, reached as the second quiet dock action beside `Backups`; its HTML and state contract are DESIGN FROZEN. | Compose, preferred-paper persistence, focused tests, four visual baselines, and normal/maximum-font Samsung passes are complete. The `37596.jpg` palette is governed separately by `THEME-37596-FREEZE.md` |
| ☑ | ~~[D-078](design/V2-SPEC-DEFECTS.md#d-078)~~ | **CLOSED 2026-08-24 — A14 keeps the full transform set and wraps it into centred rows at full 48dp targets.** | `FlowRow` plus platform `OnClick` restoration; narrow-host and platform-tree regression coverage fence the original failure |
| ☐ | [D-030](design/V2-SPEC-DEFECTS.md#d-030) | Real fixed 8 pages, or does variable page count arrive? | Filmstrip/dots morph, grid add/delete |
| ☐ | [D-029 Q1–Q3](design/V2-SPEC-DEFECTS.md#d-029) | The keep-shelf's scope, home and lifetime (Q4 closed) | The tray/shelf capability entirely |
| ☑ | ~~[D-038](design/V2-SPEC-DEFECTS.md#d-038)~~ | **CLOSED + VERIFIED 2026-08-31 — Replace Photo ships through the existing picker/decode/AssetStore pipeline.** | One reducer command, one autosave/undo step, cancel/no-op safety, distinct Photo/Decor routes, platform labels, and two-reader Samsung verification complete |
| ☐ | [D-036](design/V2-SPEC-DEFECTS.md#d-036) | Four frozen resize handles vs. eight shipped | — |
| ☐ | [D-027](design/V2-SPEC-DEFECTS.md#d-027) | Does the shelf sheet's metadata line say "Edited …", with week granularity? | — |
| ☐ | [D-023](design/V2-SPEC-DEFECTS.md#d-023) | Does the Library's `--paper` primary-button label become `--on-matcha`? | Library's primary action stays off-corpus |
| ☐ | [D-090](design/V2-SPEC-DEFECTS.md#d-090) | **Re-record the five decor goldens on the pinned CI image** (`record-goldens.yml` is `workflow_dispatch` on a pushed branch, so only you can run it) | The decor verb row and the decor ink palette are now observed — but on the Windows dev host, **not the host that gates them** |
| ☑ | ~~**Re-record the three Art-sheet goldens on the pinned CI image**~~ — `bench_art_sheet_light` · `bench_art_sheet_dark` · `bench_art_sheet_with_recents_light` | ✅ **Done 2026-08-19**, run `32244557532`, batched with D-090's five as this row asked. Eight tiles went from the disabled treatment to the live one when `SupplyCatalog` reached 12 of 16 (2026-08-18); the recorded pixels now depict that. ⚠ **The diff was read before it was committed** and it is confined to the tile grid. A measurement note worth keeping: the first three passes of that comparison reported "pixels identical" for images that differ across a fifth of the frame — a lazily-opened `PIL.Image` inside a loop, never `.load()`ed. **A golden diff is not read until a sampled pixel disagrees**; the byte hash said they differed all along and was believed only fourth |
| ☑ | ~~**Re-record the three Art-sheet goldens *again* (2026-08-20)**~~ | ✅ **Done**, run `32350445350` on the pinned image, batched into the same commit as the change that moved them. The diff was read first: **6.39 % / 6.40 % / 6.55 %** of frame, every changed pixel inside the tile grid (`bbox=(65,230,687,989)`, extended to `1211` where the recents row adds one). 104 of the 107 recorded PNGs came back byte-identical, so nothing else moved. |

### 1.2 Phase D decisions — [`DECISIONS.md` ADR-098 §5](DECISIONS.md#adr-098-gate)

**All of these are behind item 4 above** — accepting ADR-098 itself.

| ☐ | ID | Decision | Blocks |
|---|---|---|---|
| ☐ | OD-30 | Fraunces vs. Inter for the nine document-content selectors | D2, D5 |
| ☐ | OD-32 | Amend the Proof to add Loading/Error states, or rule them out of the freeze | D3 |
| ☐ | OD-33 | Does the Proof checklist get a real failure state? | D4 |
| ☐ | OD-34 | Does the mini-sheet track the A4/Letter selection? | D4 |
| ☐ | OD-35 | The fold animation: rendering, rest state, reduced-motion path | D6 |
| ☐ | OD-36 | Is Save terminal, or does the band return? | D7 |
| ☐ | OD-38 | The empty-state sticker cluster's a11y announcement; assign it an id | The phase gate's honesty |
| ☐ | OD-39 | Four corpus-integrity items in the frozen Proof (dead CSS, unused tokens, `flash()`) | D0, D3 |
| ☐ | OD-40 | Confirm the per-page a11y contract is binding from prose | D2, D10 |

### 1.3 Proposed ADRs never accepted — [`DECISIONS.md`](DECISIONS.md)

| ☐ | ADR | Subject | Forcing function |
|---|---|---|---|
| ☐ | [ADR-090](DECISIONS.md#adr-090) | The scrim amendment — *"awaiting owner adoption. Nothing in this section is in force"* | Enforcing "the artifact does not dim" against four surfaces that draw a dim |
| ☐ | [ADR-014](DECISIONS.md#adr-014) | Public-API stability rules for `core:model` geometry | `core:render`'s first external consumer |
| ☐ | [ADR-016](DECISIONS.md#adr-016) | Closed enums vs. open specs for paper sizes / zine formats | The second imposition format |
| ☐ | [ADR-017](DECISIONS.md#adr-017) | Bleed, clip and safe-area semantics | V2 print-shop export |
| ☐ | [ADR-018](DECISIONS.md#adr-018) | Versioning of imposition convention names and fold/cut ids | **Must be decided before any enters a persisted `.zine` schema** |
| ☐ | — | The butter allow-list conflict, V2.1 §3.2 vs §4.1 — *"owner ruling owed"*, stated twice | The next butter-token question |

### 1.4 V1 conformance C0 — [`V1-CONFORMANCE-INVENTORY.md`](V1-CONFORMANCE-INVENTORY.md)

The whole milestone is **Not started** ([ROADMAP.md](ROADMAP.md)) and is the declared critical path.

| ☐ | ID | Ruling | Blocks |
|---|---|---|---|
| ☐ | CI-20 / CI-21 / **CI-22** | Re-freeze the V1 HTML with the radius, type-register and spacing decisions | **CI-22 is the critical path** |
| ☐ | CI-06 | A-2: Field · Row · Notice · Menu (+ Sheet, popover) | C4 |
| ☐ | CI-07 | A-3: the consequence colour and four control states | *Today there is no colour for a delete or a failure* |
| ☐ | CI-08 | A-4: the Underway band, the fourth motion job, cancellation | — |
| ☐ | CI-11 | A-7: a screen class whose subject is the tool | Settings / About / Backup / Recovery |
| ☐ | CI-15 | Supersede or re-affirm ADR-033's editor empty state | C6 — restyling without a ruling *is* silent supersession |
| ☐ | CI-16 | Supersede or re-accept the optimistic "Saved ✨" | C6 |
| ☐ | CI-17 | Decide, or formally date the deferral of, the Read page turn | — |
| ☐ | CI-19 | Decide the imposed sheet's blank panels | — |
| ☐ | CI-23 | Delete or keep the two zero-call-site shared components | *"The ruling cannot be an engineer's"* — deletion also deletes goldens |
| ☐ | CI-80 | The card→editor morph mechanism | Not decided by any accepted document |
| ☐ | CI-98 | D-6: is hand-placement rotation *placement* or *effect*? | — |
| ☐ | CI-99 | Assign a register to six type roles — `Heading` cannot be guessed | — |

### 1.5 Product & design authorship

> The [1.x decision gate](planning/ZINELY-1X-DECISION-GATE.md) reduces the 1.x plan's O1–O15 to eight questions with evidence and framing (it reframes O12, notes that O7's removal is already approved, and adds the tap-through scope, the D5 stretch policy, the D3 "Actual size" question and the next-release shape). Read it before answering any 1.x row below; record each answer in the row's linked record.

| ☐ | Item | Where | Note |
|---|---|---|---|
| ☐ | **Author or commission the four remaining hand-drawn supply outlines** | [SUPPLIES-SPEC.md](design/SUPPLIES-SPEC.md) | ⚠ Was twelve; eight were authored 2026-08-18 and needed no house style at all. **Only three need a hand** — `tape.torn` · `paper.strip` · `paper.underline` all need the same authored *tear*, so they are one commission, not three. `fix.clip` is not a style problem: a paper clip is a **wire** object and the renderer is fill-only, so it must be drawn as the closed ribbon around the wire. `outlineOf()` returns `null` for each. Blocks S5 and S9 |
| ☑ | ~~Choose the bundled font set (which OFL families) — Q3~~ **Ruled 2026-09-26** ([gate Q2](planning/ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)): document voices Book = Fraunces, Plain = Inter; Hand deferred | [PRD.md §13](PRD.md) · [1.x plan O8](planning/ZINELY-1X-IMPLEMENTATION-PLAN.md#9-owner-decisions-needed-before-implementation) | Blocks typography. ZINE-DIRECTION X6 names three voices; confirming them is plan O8 |
| ☐ | Settle brand / visual identity direction — Q4 | [PRD.md §13](PRD.md) | Blocks UI theme |
| ☐ | Decide V2.1 prototypes for Read · Fold · first-run | [V21-SPEC.md](design/V21-SPEC.md) | Three surfaces, no frozen artifact |
| ☐ | Is `+ Add` suppressed while a card's green `Done` shows? — OD-14 | [BETA-UX-REVIEW.md](BETA-UX-REVIEW.md) | Never ruled; recorded as owed |
| ☐ | ⚠ **Likely stale (found 2026-09-25, verify and strike):** [ADR-113](DECISIONS.md#adr-113) Flip already toggles `mirrored` for Art (`EditorReducer.kt:212`, `FlipTray.kt:333`), and the KDoc at `Intent.kt:52-53` is stale too. Original row: **Does the maker get a `Mirror` verb, and when?** | [Intent.kt:40](../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/Intent.kt#L40) | `DecorElement.mirrored` **exists in the model and is unreachable from the UI** — zero callers. **Nine of the sixteen supplies are asymmetric**, so a torn tape or corner fix cannot be flipped. The frozen decor verb set is Replace/Ink/Delete, so adding a fourth verb is an amendment, not an implementation. The code calls it *"a maker verb that arrives later"* — this is the item that decides when "later" is |
| ☐ | **Run the one-time paid-pack trial, or ratify free-forever: decide by about 30 Aug to 9 Sep 2027** | [constitution §VII](zinely-constitution.md#vii-the-feature-tribunal) · [research §26](research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#26-foundational-decisions-to-make-now) | The Tribunal sets the trigger: *"decide within one year of the first bundled-supplies release"*, and not deciding ratifies free-forever. Supplies landed in `e8f2145` (2026-08-17); the first build after it is beta.3 (2026-08-30) and the first public one is `v0.9.0-beta.4-r3` (tagged 2026-09-09). Which one counts as "release" is yours to say (beta.2, 2026-08-16, is an earlier and weaker candidate: supplies in the file format, none on a page). Internal only: the website shows no pricing |
| ☐ | **1.x plan (proposal, 2026-09-25) — starters** (O2): build a sample zine / prompt library, or keep "templates DO NOT BUILD"? | [1.x plan §9](planning/ZINELY-1X-IMPLEMENTATION-PLAN.md#9-owner-decisions-needed-before-implementation) · [constitution Art. 7](zinely-constitution.md#article-7--the-maker-makes-it) | Tribunal KEEP vs ZINE-/BETA-DIRECTION DO NOT BUILD — two authoritative records conflict. If built, Article 7's bright line applies |
| ☐ | **1.x plan — ink lift** (O3): promote from EXPERIMENTAL to Planned, or leave | [1.x plan §9](planning/ZINELY-1X-IMPLEMENTATION-PLAN.md#9-owner-decisions-needed-before-implementation) | Only after an asset-model ADR and GC (plan F5) |
| ☐ | **1.x plan — PR #70** (O7): removal **already approved** as a design judgment ([ROADMAP](ROADMAP.md#in-development--built-or-being-tested-not-in-the-public-download)); what remains is acceptance work (hands-on TalkBack, rendered HTML parity), not a ruling ([gate](planning/ZINELY-1X-DECISION-GATE.md#already-recorded--not-questions)) | [1.x plan §9](planning/ZINELY-1X-IMPLEMENTATION-PLAN.md#9-owner-decisions-needed-before-implementation) | Sequencing of Direction D2 |
| ◐ | **1.x plan — tap-through scope** — **Ruled 2026-09-26:** the six holed pieces, outline hit test, crop marks excluded. Still owed: approve the drawn `v21-bench.html` behaviour note (step 4) | [decision gate Q3](planning/ZINELY-1X-DECISION-GATE.md#q3-tap-through-hit-testing-how-far-it-reaches) | Plan step 4; D5 |
| ◐ | **1.x plan — next release and restore honesty** — **Ruled 2026-09-26:** wave 1 on v3 before v4; restore-honesty fixes in step 1b; late "Backup cancelled." in step 1; skip-and-list in wave 1 (never silently partial; ADR-110 amendment). Still owed: approve the drawn `backup-restore.html` amendment and the release | [decision gate Q8](planning/ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects) | Plan steps 1b and 6–8 |
| ◐ | **1.x plan — frames** (O9) — **Direction ruled 2026-09-26:** two hand-cut frames, no nine-slice, resize decided per piece, after step 4. **Still owed: the two frames' names, visuals, set membership and order (your visual approval); a landing-size override only if a frame needs one.** Backlog supplies beyond frames stay unselected. Originally: which of ~19 backlog supplies and which frames ship; who authors outlines; **and the stretch policy** ([D-100](design/V2-SPEC-DEFECTS.md)) — [decision gate Q6](planning/ZINELY-1X-DECISION-GATE.md#q6-frames-o9--stretch) | [1.x plan §9](planning/ZINELY-1X-IMPLEMENTATION-PLAN.md#9-owner-decisions-needed-before-implementation) · [ADR-107](DECISIONS.md#adr-107) | Direction D5 |
| ☑ | **1.x plan — are `v21-typebar` / `v21-reframe` frozen?** (O10) — **Ruled 2026-09-26** ([gate Q4](planning/ZINELY-1X-DECISION-GATE.md#q4-typebar--reframe-specs-o10)): freeze TypeBar after its correction, freeze Reframe as shipped (plan step Q4-F) | [1.x plan §9](planning/ZINELY-1X-IMPLEMENTATION-PLAN.md#9-owner-decisions-needed-before-implementation) · [V21-SPEC](design/V21-SPEC.md) | File headers say "proposal, not frozen"; ZINE-DIRECTION N2 is a to-do ("Freeze …"), not a frozen claim |
| ◐ | **Ruled 2026-09-26** ([gate Q2](planning/ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)): scope ruling — a document voice is not a UI typeface; Book + Plain; no bump. **Still owed: the Fraunces minimum print size after a printed page.** Originally: **1.x plan — which typefaces may set zine text** (O12, reframed): does V2-CONSTITUTION §III govern zine text or only the UI? If zine text: Hand for short text only, amend §III, two voices (which two), or a different imperfect face | [decision gate Q2](planning/ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8) · [V2-CONSTITUTION](design/V2-CONSTITUTION.md) | §III names "the maker's own short strings" and "zine body" but ends "No fourth UI typeface"; *"The imperfect face never sets running text"* is binding either way. Blocks D2 |
| ◐ | **1.x plan — print guidance** — **Ruled 2026-09-26** ([gate Q7](planning/ZINELY-1X-DECISION-GATE.md#q7-print-o13--d3-stage-2)): design result-based guidance now, ship after the print study; "100 %" only where the print app offers scale. **Still owed after the [study](planning/STUDY-PRINT-AND-FOLD-PROTOCOL.md):** (O13 + D3 stage 2) guidance only, or a static test page too; a per-device reach profile or one global margin; may the app promise "Actual size" on Android? | [decision gate Q7](planning/ZINELY-1X-DECISION-GATE.md#q7-print-o13--d3-stage-2) | Decide after the physical print study. Any inset change amends `v21-bench`, ADR-012 and the keep-clear rulings |
| ◐ | **1.x plan — fold-study order** (O14) — **Ruled 2026-09-26** ([gate Q5](planning/ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14)): run it now with the print study; no longer a gate on D2/D5; you may reprioritise if it finds a fundamental failure. Still owed: run the sessions ([protocol](planning/STUDY-PRINT-AND-FOLD-PROTOCOL.md)) | [decision gate Q5](planning/ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14) · [ROADMAP](ROADMAP.md#exploring--recommended-next-decisions-in-order) | The ROADMAP says that gate is not reordered without owner approval |
| ☐ | **Phone migration: should Android's phone-to-phone transfer carry the Shelf?** | [research §18.2](research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#182-the-device-transfer-finding-needs-an-owner-ruling) · [`data_extraction_rules.xml`](../app/src/main/res/xml/data_extraction_rules.xml) | Today every domain is excluded from cloud backup *and* device transfer, so a new phone starts with an empty Shelf unless the maker restores a `.zine` backup. Changing that is a privacy-invariant (ADR-030) decision. Restoring a backup on a second phone is also still untested; the Download page says so. Research (2026-09-25): on Android 12+ `allowBackup="false"` does not stop device transfer, and Google's transfer skips side-loaded apps ([D1 audit](planning/ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones); O1, [can wait](planning/ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)) |

---

## 2. Device & physical verification

### 2.1 Pass 2 — first-time-user reading of the supplies drawer
**Reason:** Judgement · **Blocks:** S7-placement merge · Pass 1 ✅ **re-run and passed 2026-08-20** against
the built APK (`1204db9`, SM-A176B / Android 16), superseding the 2026-08-17 run

⚠ **The pass has been *run*; what is owed is *your reading of it*.** I ran both passes on 2026-08-20 and
Pass 2 raised the two findings below. That does not close this section: [Pass 2](../CLAUDE.md#pass-2--first-time-user-verification)
disqualifies the implementer from judging whether a screen explains itself, and I wrote the screen. The
pre-registered questions still want a first-time answer.

Three questions are pre-registered. Write down *why it felt wrong before you knew the reason* — that
sentence is the finding, and it's usually worth more than the fix.

**Already found on 2026-08-20, so you are not looking for these — they are the ones a knowing reader
could still see:**

- ~~[D-103](design/V2-SPEC-DEFECTS.md#d-103)~~ — ✅ **CLOSED 2026-08-22.** A9 chose the compact visual
  `Not / yet` while preserving the fuller spoken state. A11 then completed all sixteen marks, so the
  unavailable fallback is no longer shown by the current cabinet.
- [D-104](design/V2-SPEC-DEFECTS.md#d-104) — `TAPE & FIXINGS` now shows no tape (two of its four are
  unauthored), and `CUT PAPER` shows a window and a speech tag. Closing the tile-level over-promise
  ([D-086](design/V2-SPEC-DEFECTS.md#d-086)) moved it up to the heading.

1. ~~**[D-086](design/V2-SPEC-DEFECTS.md#d-086)**~~ — ✅ **CLOSED 2026-08-20.** The four unauthored tiles now draw no mark and say `Not available yet`. The question as posed — "coming soon", or "broken"? — was answered by removing the thing that made it ambiguous: a picture of a supply nobody has drawn.
2. ~~**[D-088](design/V2-SPEC-DEFECTS.md#d-088)**~~ — ✅ **SUPERSEDED 2026-08-29.** A15/A16 amended the
   freeze with Art's collage mark; [D-094](design/V2-SPEC-DEFECTS.md#d-094-ruling) records the implemented ruling.
3. **New** — `shape.rect` fills with `ink`, landing as a near-black square centred on whatever is already there. Correct by spec. Does it read as *art*, or as a redaction?

### 2.2 The TalkBack listen pass — [`DEVICE-VERIFICATION.md` §3.1](DEVICE-VERIFICATION.md)
**Reason:** Physical. Samsung TalkBack logs no utterances, and `uiautomator dump` **structurally cannot
expose `stateDescription`** — so no dump I take substitutes for an ear.

| ☐ | Question | Decides |
|---|---|---|
| ☑ | Is an import summary spoken **twice**? | **OWNER-CONFIRMED 2026-08-25** — first-person TalkBack pass completed with no blocking issue reported |
| ☑ | Does `Copier` speak its On/Off state? | **OWNER-CONFIRMED 2026-08-25** — first-person TalkBack pass completed with no blocking issue reported |
| ☑ | Is an import landing mid-transition announced at all? | **OWNER-CONFIRMED 2026-08-25** — first-person TalkBack pass completed with no blocking issue reported |
| ☑ | Do the opener and two ink swatches speak `Ink`, `Spot ink`, and `Neutral ink`? | **OWNER-CONFIRMED 2026-08-25** — manual confirmation complements the platform-tree regression coverage for [D-083](design/V2-SPEC-DEFECTS.md#d-083) |
| ☑ | **Step 2 ([ADR-119](DECISIONS.md#adr-119)):** on a real phone, does swiping through a Bench page go top to bottom, left to right — and stay the same after *Bring to front / Send to back* and after *Make spread* (the spread photo read first)? Does TalkBack skip the dimmed area behind the zine actions sheet, and do a tap there and Back still close it? | **OWNER-CONFIRMED 2026-09-26** on SM-A176B (TalkBack 16.2.00.13): photo → tape → text after restack and spread, reverse swipe the exact reverse, the scrim never focused, Back closes. Sighted listen: proxy evidence ([ADR-119](DECISIONS.md#adr-119)) |
| ☑ | Does a supply's **`Change ink`** custom action work under real TalkBack? | **OWNER-CONFIRMED 2026-08-25** — manual confirmation complements the regression coverage for [D-091](design/V2-SPEC-DEFECTS.md#d-091) |

### 2.3 The print pass — [`DEVICE-VERIFICATION.md` §3.2](DEVICE-VERIFICATION.md)

| ☐ | Item | Note |
|---|---|---|
| ☑ | Print a `Copier`-filtered page and judge it | **OWNER-CONFIRMED 2026-08-25** — physical print check completed with no blocking issue reported |
| ☑ | Physical printer for fold validation — PRD Q2 | **OWNER-CONFIRMED 2026-08-25** — physical print/fold check completed |
| ☐ | Collect beta-cohort feedback on the photocopier print | [CHANGELOG.md](../CHANGELOG.md): *"Please print one and say what you see — that feedback is the test"* |

### 2.4 Instrumented runs that have never executed

| ☐ | Item | Why it matters |
|---|---|---|
| ☑ | **Run the PDF-surface hole test on hardware** | **CLOSED 2026-08-25.** `PdfSurfaceParityInstrumentedTest` passed 5/5 on SM-A176B / Android 16, including the shared supply-outline replay path. It remains a hardware gate because `PdfDocument` does not run under Robolectric |
| ☐ | Two-pass device verification for every V2.1 surface as it lands | Reference device SM-A176B |

---

## 3. Release & credentials

### ☑ `0.9.0-beta.5` — the maintenance release (APK only)

Scope, per the owner's 2026-09-23 decisions: stability fixes (PR #73), the Android 7–9 Save PDF permission
request ([ADR-054 §8](DECISIONS.md#adr-054) amendment), the About maker's note with Licences & credits
([ADR-116](DECISIONS.md#adr-116)/[ADR-117](DECISIONS.md#adr-117)), and the off-main-thread Reframe load.
No AAB, no Play submission. PR #70 (Font removal) is **not** in it. Release notes:
[CHANGELOG `0.9.0-beta.5`](../CHANGELOG.md).

| ☐ | Gate | Note |
|---|---|---|
| ☑ | PR #73 stability fixes merged | `72e620e`, CI run `35858746779` green on `main` |
| ☑ | Save PDF permission request merged | Merged to `main` with the release through PR #74; independent review GO |
| ☑ | **Rule on "Don't ask again"** for the Android 7–9 storage prompt | **Ruled 2026-09-25: stays a Known Limitation for beta.5** (no copy or Settings change). The Android 9 run confirmed it: after "Don't ask again", Try again shows no dialog and stays on "Couldn’t make the PDF" |
| ☑ | Android 7–9 Save PDF on an emulator or device | **Passed 2026-09-24 on an Android 9 (API 28) emulator**: first Save asks; Deny → error, no file; Try again asks again; Allow → exactly one valid A4 PDF; Share never asks. No physical Android 7–9 phone was used. Evidence in the [tester package](releases/0.9.0-beta.5.md) |
| ☑ | Samsung Pass 1 | **Run 2026-09-24; re-checked 2026-09-25 on the final APK** (`d957f1f`): install over the previous build keeps data; About → credits → licence → Back chain; Start a zine sheet under TalkBack; Save PDF; Share. The TalkBack focus item is a Known Limitation (row below). Nothing was printed on paper; Backups was opened, not exercised |
| ☑ | Samsung Pass 2 — *your* first-time reading | **Owner: PASS, 2026-09-25** — nothing confusing. (The agent reading of 2026-09-24 is in the tester package.) |
| ☑ | TalkBack listen | **Owner: PASS, 2026-09-25**, on the Samsung: About, Licences & credits, licence, Save PDF and Share behave as intended; entry and return focus is the documented Known Limitation |
| ☑ | **Rule on the TalkBack focus finding** | **Ruled 2026-09-25: Known Limitation.** A fix (`596d52d`) moved Compose focus but Samsung TalkBack did not follow it on the device, so it was removed (`d957f1f`). The unlabelled sheet backdrop found on the same pass is fixed |
| ☑ | R-1 keystore backup (below) | **Owner: done and verified, 2026-09-25** |
| ☑ | **"Proceed with the beta.5 release"** | **Given 2026-09-25.** Tag `v0.9.0-beta.5` on the PR #74 merge commit and a GitHub pre-release with the APK. No AAB, no Play. The website download, changelog and roadmap pages move to beta.5 in the website workflow, after the release exists |

### ☑ R-1 — Back up the keystore *(done 2026-09-25, owner-verified)*
[`RELEASING.md`](RELEASING.md) — *"No agent, script, or CI job can do this or verify it was done."* The
passwords exist only in `keystore.properties` on this machine; they were generated in a shell and never
printed. Nothing in the repo or build output would reveal the backup is missing.

- ☑ Copy `zinely-release.jks` + `keystore.properties` to **two independently-failing** places
- ☑ Verify with `keytool -list -v … -alias zinely`

### ◐ Android developer verification *(ruled 2026-09-26 — [gate Q1](planning/ZINELY-1X-DECISION-GATE.md#q1-developer-verification-o15))*
*Corrected 2026-09-26:* the phase starting **30 Sep 2026** covers installs from named app stores in Brazil,
Indonesia, Singapore and Thailand — **not** side-loaded GitHub APKs (Google's FAQ, updated 15 Jul 2026). The
**global phase in 2027** (month unpublished) covers side-loaded apps too, so it is GitHub's real deadline.
Registration proves **package name + signing-key ownership**. Route chosen: **Play Console**, because Zinely
plans both outside-Play (GitHub) and Play distribution; no separate Android Developer Console account.

- ☑ Registered as an individual Google Play Console developer (owner, 2026-09-26)
- ☐ Finish the address verification — administrative; does not block GitHub distribution
- ☐ Register `com.aritr.zinely` with the existing release-key fingerprint before the 2027 global phase; re-check Google's date before each release
- ☐ Do **not** modify signing configuration; the existing signing identity must stay compatible with Play

### ☐ Play Store path *(only if production is the goal)*

| ☐ | Item | Note |
|---|---|---|
| ◐ | Create + identity-verify a Play Console account | Individual account created (owner, 2026-09-26); address verification pending. *"The only step that can miss a ship date on its own"* |
| ☐ | Start the 12-testers-for-14-continuous-days closed test | **Required for a new personal account** before production access (owner, 2026-09-26): at least 12 testers continuously opted in for 14 days. Real humans, wall-clock gated; part of future launch planning |
| ☐ | Enrol in Play App Signing at first upload | **One-time irreversible** console choice. ⚠ Decide first whether GitHub beta users must be able to update from Play in place: that only works if Play signs with the *existing* Zinely release key (upload it as the app signing key). A Google-generated key makes Play builds unable to update the side-loaded app, so moving means uninstalling, which deletes the library unless the maker has a backup. [RELEASING §4.2](RELEASING.md#42-the-artifact) currently assumes a Google-held signing key (and its `versionCode 3` note predates beta.4-r3, which is versionCode 9). |
| ☐ | Complete data-safety form, content rating, target-audience declarations | Legal attestations signed by a person; answers pre-drafted |
| ☐ | Produce the feature graphic (1024×500) | *"The one asset with no source in this repository"* |
| ☐ | Take store screenshots on a real device from a release build | — |
| ☑ | ~~Host the privacy policy at a public URL~~ | Done 2026-09-09 through GitHub Pages ([RELEASING §4.6](RELEASING.md#46-what-is-still-owed-before-submitting)) |

### ☑ Repository state
- ☑ ~~Local `main` is **46 commits ahead of `origin/main`** and diverged (`git pull --ff-only` fails).~~ Checked 2026-09-23: local `main` equals `origin/main` (`72e620e`).

---

## 4. Measurement

| ☐ | Item | Where | Note |
|---|---|---|---|
| ☐ | **Record the motion-and-haptics baseline (CI-14)** | [protocol](reviews/CI-14-motion-baseline-protocol.md) | Device recording + subjective banding. Blocks CI-08's duration half and every C3a duration change — *any duration changed before this is changed against a tie-break nobody has taken* |
| ☐ | Measure whether **150 dpi** survives a home printer | [DEVICE-VERIFICATION.md](DEVICE-VERIFICATION.md) | A provisional, unmeasured number. Only paper can measure it |
| ☐ | Confirm `f*` reaches the PDF file, and that AA is ignored by the PDF backend | [SUPPLIES-SPEC.md](design/SUPPLIES-SPEC.md) | *"Sourced-but-unmeasured."* Needs the instrumented run above |

---

## Standing constraints I observe

Recorded here so you can hold me to them:

- **Never touched:** `README.md` · `docs/RESEARCH.md` · `gradle.properties` · `37596.jpg` · `acdec/`
- **Commits only when you ask.**
- **I never self-approve** — every substantive change goes to an independent Review Agent first.
