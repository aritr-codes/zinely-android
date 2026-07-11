# M5 (Proof · the Fold) — batch decomposition

> **Status:** pre-implementation batch design (2026-07-11). Planning only — **no code begun**.
> Derived from the DESIGN-FROZEN [proof.html](../design/v1/proof.html) **only**. Sequences the
> Compose implementation of the frozen Proof surface onto the existing app, mirroring the M3 B1→B4
> discipline. Authority for *what ships* stays with [PRD](../PRD.md)/[ROADMAP](../ROADMAP.md); this
> doc owns only the order and shape of the M5 work. Folds in the former M4 imposition-truth
> checkpoint ([parity plan M4 §](../COMPOSE-V1-PARITY-PLAN.md), [ADR-050](../DECISIONS.md#adr-050)).

---

## 0. What the frozen spec actually is (and why M5 ≠ a pure reskin)

`proof.html` is **one surface, three internal acts**, not three screens:

- **One shared frame:** a topbar (loss-safe **back** · zine **name** · three **progress creases**), a
  single `.acts` container that cross-fades/slides between acts, and **one** bottom action bar whose
  primary/secondary buttons reconfigure per act (`configurePrimary()` / `onPrimary()` / `setAct()`).
- **Act 1 — The Sheet:** the *imposed* landscape sheet (8 panels, top row flipped 180°, three vertical
  + one horizontal crease, the **one** coral cut across the two centre columns, a "printer can't reach"
  dead-band), an honesty legend, and front/back cover confidence cards.
- **Act 2 — Print:** the honest recipe (Scale 100%/Actual · Landscape · Paper A4/Letter · Single-sided),
  the single-sided note, an export row (Save PDF · Share · Print), a "print one test sheet" line, and
  two bottom sheets (paper-size chooser, share).
- **Act 3 — The Fold:** a **5-step** pausable fold guide (stepped SVG diagrams, prev/next + dots +
  ←/→ keys), then the **climax**: cover swings shut → book settles → shelf-line draws → words arrive →
  actions arrive, delivered in **timed beats** with the `success` haptic; a reduced-motion path that
  jumps straight to the finished static state.
- **Overlays:** loading (sweep), error (recoverable), empty; snackbar + toast.

The app today is **three navigation destinations**, each with its own `*Destination` host in
[ZinelyNavHost.kt](../../app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt) reading the shared
editor VM: `PreviewRoute` → `ExportRoute` → `CompletionRoute` (ADR-041 auto-lands on Completion after a
successful export share).

**Two consequences make M5 structural, not cosmetic** — this is why M2/M3 needed no ADR but **M5 does**:

### Decision A — collapse three routes into one 3-act screen *(architecture → ADR-051)*
The frozen progress creases, act-slide transitions, single shared action bar, and the in-spec
`savePdf → snack "Fold now" → setAct(2)` hand-off all describe **intra-screen act state**, not
inter-route navigation. Recommended: **one `ProofRoute` + `ProofScreen`** holding `act: 0..2` state,
replacing the three routes; the export→fold auto-advance (ADR-041) becomes an **act transition**, which
is *closer* to ADR-041's intent, not a regression. The shared-editor-VM seam
(`getBackStackEntry(EditorRoute)`) and the read-only `ExportViewModel` are **preserved** — only the
three leaf routes merge. This must be settled **before B1 code**, because it defines B1's shape.

### Decision B — the reader-booklet has no place in the frozen Proof *(scope → ADR-051, may need user/PRD sign-off)*
`PreviewScreen` pages the document in **reading order** (the reader's little book). The frozen Proof
Act 1 is the **imposed sheet** — a different artifact (it's today's *decorative* sheet in `ExportScreen`,
`decorativeImpositionRows`). The frozen V1 Proof **supersedes** the old Preview+Export+Completion triad;
it contains **no reading-order pager**. Dropping a shipped, user-facing feature is a **scope** call, not
a reskin — it must be recorded and, per the Documentation Rule, likely wants explicit PRD/user
acknowledgement. **Do not silently delete the reader.** Options: (B-drop) accept supersession — the
imposed-sheet-first Proof is the frozen intent; (B-keep) preserve the reader as a pre-Proof step — but
that is **not in the frozen spec**, so it would require updating the HTML spec first (HTML-first
workflow). **Recommended: B-drop, recorded in ADR-051, surfaced to the user before B1.**

> **RESOLVED (2026-07-11, user):** **B-drop.** The imposed-sheet-first Proof supersedes the
> reading-order reader-booklet — the frozen design intent. `PreviewScreen.kt` and `PreviewRoute` are
> retired across B1→B5. ADR-051 records this as an accepted scope change (feature supersession, not a
> regression); PRD scope note to follow when ADR-051 is authored in B1.

> **These two decisions are the M5 spine and are authored as [ADR-051] within B1** (not a separate
> no-code batch), exactly as M0/M1 carried their ADRs. B1 must not start until A and B are answered.

---

## 1. Invariants every batch must hold (the frozen contract)

- **Imposition:** Act 1 order/rotation **derives from the engine** via
  `decorativeImpositionRows(SingleSheet8.TOP_ROW_ROTATED)` — **no raw layout array anywhere in Compose**
  (the folded-in M4 checkpoint). ADR-007 stays the single imposition source of truth; the corrected
  frozen grid ([ADR-050](../DECISIONS.md#adr-050)) already matches it.
- **Tokens/components:** read M0 `LocalZinely*` tokens and M1 `Z*` components; **never** re-introduce
  M3-role abuse or inline literals. `ZPrimaryButtonMetrics.Proof` (54/22/16/16) already exists.
- **Behaviour reskin-invariant:** `ExportViewModel.export`, the shared-VM seam, the OS share/open edges,
  and the ADR-041 payoff hand-off survive; only chrome + the route topology change.
- **A11y / keyboard / reduced-motion / haptics:** loss-safe back everywhere; the fold step-nav is
  keyboard-driven (←/→) with `aria-live` step captions mirrored to Compose live regions; the four-verb
  haptic vocabulary (`tick`/`snap`/`boundary`/`success`) wired to step advance / export / boundary /
  climax; every animation degrades to the already-correct static state under `rememberReduceMotion()`.
- **Privacy:** no network; share/open stay OS-delegated; "Zinely uploads nothing" copy retained.

---

## 2. Batches

Decompose by frozen **region/act**, ascending in content risk, each independently reviewable and each
re-recording its own goldens — the M3 B1→B4 template (frame first, then content, then climax, then
seal).

### B1 — Proof scaffold: one route, act-nav shell, structural ADR
1. **Purpose:** stand up the single `ProofScreen` frame — shared topbar (loss-safe back · name ·
   3 progress creases), the `.acts` container with the act state machine (`act 0..2`, slide-in /
   slide-back transitions, reduced-motion instant), and the one bottom action bar with per-act
   primary/secondary config. **Empty act bodies** (placeholders). Collapse `Preview/Export/Completion`
   routes into `ProofRoute` behind the shared-VM seam; author **[ADR-051]** (Decisions A + B).
   Include the topbar **act-status live caption** (`#actLabel role="status"`, "Step 1 of 3 · The
   sheet") as a Compose live region announced on every act change — it is topbar chrome and belongs here.
2. **Files:** **new** `ProofScreen.kt`; `ZinelyNavHost.kt` (+ `EditorRoute.kt` routes) — merge three
   composables into one `ProofDestination`; `DECISIONS.md` (ADR-051); `COMPOSE-V1-PARITY-PLAN.md` +
   `ROADMAP.md` (M5-in-progress). `PreviewScreen.kt` retained until B-drop lands (removed in B2/B5).
3. **Risk:** **High** — the only structural change in M5; touches nav topology + ADR-041 hand-off.
4. **Review focus:** act state machine correctness (no dead/duplicate primary — spec RF-1); shared-VM
   seam + ADR-041 auto-advance preserved as an act transition; back is loss-safe at every act; the
   act-label live region announces act changes; ADR-051 soundly records A + B; no reader-booklet deletion
   without B recorded. **Guard:** the frozen `#segAct` Sheet/Print/Fold buttons live in the *prototype
   dock* (review scaffolding) — the progress creases are passive (`aria-hidden`); do **not** transcribe
   the dock control as a production tappable act-switcher.
5. **Parity gate:** topbar + progress creases + action-bar chrome pixel-match frozen `proof.html` at
   phone width, light + dark; act-transition timing reads `--fast`/`--base` tokens, not literals.
6. **Tests:** `ProofScreenTest` — act advance/back, progress-crease state, per-act primary label/icon,
   act-label live-region text, loss-safe back; a golden of the empty 3-act frame (light+dark).
7. **Device validation:** deferred to B4/B5 (no act content yet); smoke-nav on device optional.

### B2 — Act 1 "The Sheet" (imposed preview + engine-truth checkpoint)
1. **Purpose:** the imposed sheet exactly as it prints — paper surface, dead-band, 8 cells with the top
   row flipped, three vertical + one horizontal crease, the **one** coral cut + "one cut" label, honesty
   legend, front/back cover cards. Order/rotation **derived from the engine** (folded-in M4). Relocate
   `decorativeImpositionRows` into the Proof and extend its drift guard.
2. **Files:** `ProofScreen.kt` (Act 1 body); move `decorativeImpositionRows`/`DecorativePanel` out of
   `ExportScreen.kt` into a Proof-local home; `DecorativeImpositionOrderTest.kt` (retarget/extend);
   reuse `ZPaperSurface`. Begin retiring `PreviewScreen.kt` per ADR-051 B-drop.
3. **Risk:** **Medium** — carries the engine-truth checkpoint; must not encode a raw array or drift from
   `TOP_ROW_ROTATED`.
4. **Review focus:** zero raw imposition literals in Compose; cell flip/cut/crease geometry matches the
   frozen grid **and** the engine (ADR-050 table); dead-band + cut are decorative (`role=img`, cleared
   a11y subtree) with the sheet's single aria-label preserved.
5. **Parity gate:** Act 1 pixel-match vs frozen `proof.html` (sheet, cells, creases, cut label, legend,
   cover cards), light + dark, phone + tablet; **plus** the engine-truth golden — the corrected frozen
   grid already equals the engine, so **no carve-out**.
6. **Tests:** extended `DecorativeImpositionOrderTest` (order+rotation from `TOP_ROW_ROTATED`); Act 1
   golden (light+dark, phone+tablet).
7. **Device validation:** none specific (static illustration).

### B3 — Act 2 "Print" (the honest recipe + export wiring)
1. **Purpose:** the four recipe rows (Scale 100%/Actual + Landscape as `warn`, Paper with Change,
   Single-sided), the single-sided note, the export row (Save PDF · Share · Print), the "print one test
   sheet" line, and the paper-size + share bottom sheets — wired to the shipped `ExportViewModel.export`
   and the OS share edge. In-spec `savePdf → snack "Fold now" → act 3` hand-off (ADR-041, now intra-screen).
2. **Files:** `ProofScreen.kt` (Act 2 body); reuse `ZSheet` (paper/share choosers), `ZMenuItem`
   (+ `ZSelectedStyle` proco variant), `ZButton`, `ZSnackbar`; `ProofDestination` wiring in
   `ZinelyNavHost.kt` (export/share/print → VM; preserve the read-only `ExportViewModel`).
3. **Risk:** **Medium** — wires real export/share behaviour and two modals through the new single-route
   host without regressing ADR-039/ADR-041. **Scope call inside B3:** the shipped app has **no OS print
   path** (only PDF/PNG export + OS share; the frozen `#printNow` is a stub snackbar). B3 must decide
   explicitly whether the **Print** action invokes the OS `PrintManager` (a **net-new** integration —
   then B3 is not a pure rewire) or maps onto the existing share edge. Record the choice; if net-new,
   it is an ADR-worthy addition, not reskin.
4. **Review focus:** print honesty never overstates the printer; paper choice flows to export; share/open
   stay OS-delegated (privacy); snackbar focus-to-action + 5s (RI-4 accepted limitation documented);
   `ExportViewModel` single-flight + error banner preserved.
5. **Parity gate:** Act 2 pixel-match (recipe rows incl. `warn` coral styling, note, export row, test
   line, both sheets), light + dark, phone + tablet.
6. **Tests:** **new** coverage for the previously-untested `ExportViewModel.export` happy/error/single-
   flight paths; Act 2 interaction test (paper chooser, share sheet, export → snackbar → act 3); Act 2
   golden (incl. open sheet).
7. **Device validation:** TalkBack + keyboard pass on the recipe + sheets (F3), folded into the B5 device sweep.

### B4 — Act 3 "The Fold" (the signature climax)
1. **Purpose:** the 5-step fold guide (stepped diagrams, prev/next + dots + ←/→ keys, live-region step
   captions) and the **climax** — cover-close → settle → shelf-line → words → actions in timed beats with
   the `success` haptic — plus the reduced-motion static finished state. The whole delight budget.
2. **Files:** `ProofScreen.kt` (Act 3 body + climax choreography); a Proof-local `FoldDiagram`
   (stepped, replacing `CompletionScreen`'s 4-step static diagrams with the frozen 5-step set); reuse
   `ZButton`, motion tokens, `ZinelyHaptics`, `rememberReduceMotion`. Retire `CompletionScreen.kt`.
3. **Risk:** **High** — highest delight/risk surface; beat choreography, focus management on reveal,
   reduced-motion correctness, and it reskins the *previously-untested* payoff.
4. **Review focus:** exactly one finish action at the last step (no dead primary — spec RF-1); climax
   beats gated + collapsed under reduced motion (book/words/actions all correct static); `success` haptic
   fires once, reduced-motion-silenced; ←/→ keys + step dots + live-region captions match the spec;
   focus lands on "Your zine is a book." after the reveal.
5. **Parity gate:** Act 3 pixel-match — each of the 5 steps, the step nav/dots, and the finished-book
   state — light + dark, phone + tablet; **reduced-motion golden** proving the static finished state.
6. **Tests:** `ProofScreen` fold tests (step advance/back, keyboard, last-step finish, reduced-motion
   jumps to finished); climax beat sequencing via `mainClock`; Act 3 + finished + reduced-motion goldens.
7. **Device validation:** **required** — on-device TalkBack + keyboard pass on step-nav and the staged
   climax reveal (the F3 non-automatable gate), and a device check that the `success` haptic fires.

### B5 — Overlays + parity seal
1. **Purpose:** loading (sweep) / error (recoverable) / empty overlays, snackbar + toast, cross-act
   reduced-motion + theme + tablet sweep; remove the retired screens/routes/tests; the mandated
   Physical-Workflow re-review + Design-Director emotional review.
2. **Files:** `ProofScreen.kt` (overlays via `ZStatusPane`/`ZSweep`/`ZToast`); delete
   `PreviewScreen.kt`/`ExportScreen.kt`/`CompletionScreen.kt` remnants + their obsolete routes/tests;
   `COMPOSE-V1-PARITY-PLAN.md` + `ROADMAP.md` (M5 DONE).
3. **Risk:** **Medium** — deletions + the full parity/emotional sign-off.
4. **Review focus:** no dead code / orphaned routes/tests; empty vs error vs loading are distinct and
   recoverable; every accepted limitation (RI-4 snackbar focus, dark graphical-text 3:1) still documented.
5. **Parity gate:** the full M5 freeze-in-Compose gate — pixel + motion + interaction + a11y + theme +
   tablet + reduced-motion all green across all three acts + overlays.
6. **Tests:** overlay/state tests; full `:feature:editor` + `:app` suites green; all M5 goldens recorded
   on the pinned CI image.
7. **Device validation:** the consolidated device TalkBack/keyboard sweep across all acts + overlays.

---

## 3. Recommended first batch — **B1**

Same reasoning that produced the B1→B4 editor workflow: **build the frame before the content.** In M3,
B1 was the selection chrome — the structural skeleton every later batch re-attached to. Here B1 is the
3-act shell + the route collapse + the structural ADR:

1. **It is the only load-bearing structural change.** Decisions A (one route) and B (booklet
   disposition) define the shape of *every* other batch; resolving them first — with the **least**
   content built on top — is where a wrong call is cheapest to reverse. Building Act content before the
   frame means restructuring it twice.
2. **It unblocks B2–B4.** Each act body hangs off the scaffold's act state + shared action bar; none can
   be built or golden-tested without the frame.
3. **It isolates the risk.** The nav-topology change + ADR-041 hand-off + shared-VM seam are reviewable
   in isolation against empty act bodies, before any delight work risks rework.
4. **It forces the scope decision to the front.** B (dropping the reader-booklet) must be surfaced to the
   user/PRD before pixels are spent — B1 is where that happens, not a late surprise.

**Pre-condition for B1 — both decisions now settled:** A = collapse to one `ProofRoute` (architecture);
B = **drop the reader-booklet** (scope, confirmed by the user 2026-07-11). ADR-051 records both. B1 is
unblocked on the decision front; it awaits explicit authorization to begin implementation.

---

## 4. Already implemented — do **not** rebuild

- **Imposition engine + derive-and-guard:** `core:imposition` (ADR-007), and
  `decorativeImpositionRows` + `DecorativeImpositionOrderTest` (S7.2 `57ed568`) — **relocate, don't
  rebuild** (the folded-in M4 checkpoint).
- **M0 tokens** (`ZinelyColors`/`Motion`/`Haptics`/`Typography`/`Dimens`, `ReduceMotion`) and **M1
  components** (`ZButton` incl. `…Metrics.Proof`, `ZSheet`, `ZMenuItem`, `ZSnackbar`, `ZToast`,
  `ZStatusPane`, `ZSweep`, `ZPaperSurface`, `ZFocusRing`, `ZAccessibleControl`).
- **`ExportViewModel.export`** (single-flight PDF/PNG, off-thread, `ready` one-shot event) and the OS
  share/open edges in the destinations — **preserve behaviour**, re-wire to the single route.
- **ADR-041** payoff hand-off — **preserve intent** as an intra-screen act transition.
- **The corrected frozen grid** ([ADR-050](../DECISIONS.md#adr-050)) already matches the engine — **no
  imposed-sheet parity carve-out**.

## 5. Open items surfaced (not resolved here)

- **Decision B (reader-booklet)** — RESOLVED B-drop (user, 2026-07-11); ADR-051 to carry the PRD scope note.
- The pre-existing **M2 shelf goldens untracked/misplaced** and **F4 stale palette** loose ends are out
  of M5 scope (tracked in the parity plan) — do not fold into M5.
