# Compose Implementation Guide — the implementation constitution

> **This is not a task list.** It is the *philosophy* of how Zinely V2 is implemented in Jetpack Compose. A new
> engineer should be able to read this once and know how every implementation decision is made, what is negotiable
> and what is not, and how their work will be judged.
>
> **Governing authority:** [V2-CONSTITUTION.md](design/V2-CONSTITUTION.md). This guide serves that constitution;
> where they appear to conflict, the constitution wins and this guide is the bug.
> **Companion documents:** [COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md) (the phases) ·
> [COMPOSE-IMPLEMENTATION-RULES.md](COMPOSE-IMPLEMENTATION-RULES.md) (the per-session checklist) ·
> [ARCHITECTURE.md](ARCHITECTURE.md) (the technical source of truth).

---

## 1. The one idea

**You are reproducing a finished design, not designing.** The design phase is over. Library, Bench and Proof are
DESIGN FROZEN. Your job is to make the running app match the frozen HTML specification faithfully — pixel, motion,
interaction, and accessibility. The implementation team should never need to *interpret* the design; they execute
it. When you feel the urge to improve a screen, that urge is out of scope — route it to the owner as a proposed
HTML-spec change, and keep building.

---

## 2. How implementation decisions are made

Decisions flow down a fixed authority chain. When in doubt, climb it:

```
V2-CONSTITUTION.md          (immutable truths — why)
   └── frozen HTML specs    (the canonical visual + interaction spec — what)
         └── ADRs           (recorded technical decisions — how, and why that way)
               └── ARCHITECTURE.md / house conventions   (the engineering means)
                     └── your code
```

- If the **HTML** answers the question, the HTML is right. Match it.
- If the HTML is silent and an **ADR** answers it, follow the ADR.
- If both are silent, follow **ARCHITECTURE.md** and the [house conventions](#11-house-rules), and pick the calmest
  option consistent with the constitution.
- If you believe a **frozen artifact is itself wrong**, stop — see §4.

**Repository truth beats assumptions and summaries.** The code, commits, tests, and frozen HTML are authoritative;
a memory, a summary, or "I'm pretty sure" is not. Read the actual file before you rely on it.

---

## 3. How the frozen HTML is treated

The frozen prototypes are the **canonical specification**, not references or inspiration:

- [`docs/design/mockups/v2-library.html`](design/mockups/v2-library.html) — Library (frozen 2026-07-27)
- [`docs/design/mockups/v2-bench.html`](design/mockups/v2-bench.html) — Bench / editor (frozen, `4494e95`)
- [`docs/design/mockups/v2-proof.html`](design/mockups/v2-proof.html) — Proof (frozen, `caf431c`)

Their authoring specs — [V2-BENCH-PRINCIPLES.md](design/V2-BENCH-PRINCIPLES.md),
[V2-BENCH-IA-INTERACTION.md](design/V2-BENCH-IA-INTERACTION.md), [V2-BENCH-REVIEW.md](design/V2-BENCH-REVIEW.md) §E,
[V2-PROOF-IA-INTERACTION.md](design/V2-PROOF-IA-INTERACTION.md) Part E, and the token/identity docs — explain the
*intent* behind the pixels. Read them when a pixel's *reason* matters (it usually does).

**Read the HTML as a spec of layout, spacing, type, colour, state, and motion — not as literal markup to port.**
You are not transpiling `<div>`s to `Box`es; you are reproducing the *result* the HTML defines, idiomatically in
Compose (Material 3, `collectAsStateWithLifecycle`, hoisted state, stateless children).

---

## 4. When implementation may differ from the HTML — and when it must never

### It may differ only for these reasons (and each is logged):
- **Platform truth the HTML can't express** — real `AccessibilityNodeInfo` behaviour, IME insets, system back,
  haptics, RTL, font scaling. The HTML is a browser mock; the device is real. Match the *intent*, implement the
  *platform's* correct behaviour, and note the deviation.
- **A genuine HTML bug or omission** — a state the prototype never drew, a contradiction between two prototypes.
- **Accessibility that the mock under-specifies** — you always add *more* a11y, never less.

Every such deviation is **written down with its justification** (in the PR and, if it's a real design gap, as a
proposed HTML-spec change).

### It must never differ for these reasons:
- "I think this looks better." — Out of scope. The design is frozen.
- "This is easier to build." — Build the harder faithful thing, or raise the spec question; don't quietly diverge.
- "Users might prefer…" — That is a design hypothesis, and design is closed. Route it to the owner.
- Adding a feature, control, screen, or state not in the frozen spec. — **No feature creep.**

### The golden rule: **if the HTML is wrong, fix the HTML first.**
Any change to what a screen *should* look like or do goes **into the frozen HTML specification first** (an owner
gate — the owner amends the freeze), and only then into Compose. Never the reverse. Code that "corrects" the
design without the spec catching up creates two sources of truth, which is the exact failure the whole HTML-first
workflow exists to prevent.

---

## 5. Pixel-parity expectations

Parity is the acceptance bar, not an aspiration.

- Every screen is verified **side-by-side against its frozen HTML** — layout, spacing (8pt rhythm), typography
  (Fraunces/Inter, sizes, weights, tracking), colour (exact token values, both themes), and every interaction
  state (rest, focus, pressed, selected, disabled, error, empty).
- **[Roborazzi](ARCHITECTURE.md) screenshot/diff tests** lock render fidelity for the pure-render surfaces and
  guard against regression. Golden baselines are recorded on the **pinned CI image** (see
  [ADR-058](DECISIONS.md#adr-058) discipline) so diffs are meaningful.
- Differences are **findings**: each is either fixed or explicitly accepted with a reason. A difference is never
  silently ignored, and "close enough" is not a verdict.
- **Parity comes before optimisation.** Make it faithful first; make it fast second (§8).

For the **Bench** and **Proof**, parity is broader than pixels — it includes **interaction parity, animation
parity, and behaviour parity** (editing feel, fold guide, print flow). A pixel-perfect Bench that *edits* wrong
has failed parity.

---

## 6. Accessibility expectations

Accessibility is a first-class acceptance criterion, never a follow-up.

- **The platform tree is the truth.** Assert against what TalkBack actually reads (`AccessibilityNodeInfo` via
  `adb shell uiautomator dump`), not only the merged Compose semantics tree. They can disagree — that exact class
  of defect shipped through a green Robolectric suite ([ADR-058](DECISIONS.md#adr-058) branch).
- **Every gesture has a named custom action twin and a visible non-gesture fallback.** Canvas interactions expose
  a virtual a11y node-tree with a named custom action per gesture; nothing is gesture-only.
- **AA contrast (body ≥ 4.5:1)** on the ★ pairings in [V2-TOKENS.md](design/V2-TOKENS.md) is gated in CI.
- **Font scaling, RTL, reduced-motion, and TalkBack traversal order** are part of "done," not extras.

---

## 7. Animation philosophy

- **Calm and sparing.** Motion marks the two emotional peaks (e.g. a paper-settle at library and completion), not
  every transition. If a transition doesn't *mean* something, it's a cut, not an animation.
- **Always leaves a persistent static end-state.** Teaching animations (the Proof fold) are opt-in and resolve to
  a static frame that carries the same information — the user who never plays the animation loses nothing.
- **`prefers-reduced-motion` / the platform reduce-motion setting is honoured** everywhere.
- **Animation parity** is part of Bench/Proof acceptance — match the frozen motion, don't invent new flourishes.

---

## 8. Testing philosophy

Follow the [ARCHITECTURE.md](ARCHITECTURE.md) testing strategy; the philosophy behind it:

- **Push logic into pure, platform-free helpers** (`core:model`, `core:imposition`, `core:render` carry zero
  Android deps) and unit-test them on the JVM. A thin framework seam wraps platform APIs so the core stays testable.
- **Test the invariant, not the incident.** The `analyzeTextCoverage` defect ([ADR-070](DECISIONS.md#adr-070)) was a pure,
  well-tested function with *zero production consumers* — green tests, broken product. Tests must cover that the
  logic is **wired**, not only that it's correct in isolation.
- **Roborazzi** for render/visual fidelity; **ViewModel integration tests with fakes**; **Compose UI tests** for
  interaction; **device verification** for what only a device can tell you (§below). Given-When-Then.
- **The reproducibility guarantee is a test target:** preview == export == read is enforced by parity tests
  (`PagePreviewParityTest`), because a single engine ([ADR-028](DECISIONS.md#adr-028)) is a constitutional invariant.

### 8.1 The frozen property table

**Every implementation package opens with one, before any production code is written.** It is the pre-implementation
artifact that turns *"which frozen properties have no test at all?"* — the question that has found more defects in
this programme than any other — from a gate-time audit into the first thing you do.

**It is subordinate to the frozen HTML and can never become a second source of design truth.** Every row's
**Source** cell names a frozen file and a selector or line; a row that cannot name one is not a frozen property
and does not belong in the table. The table *records* what the freeze says. It never decides it, and where the two
disagree the freeze is right and the table is a bug. (This is A9's rule for verification artifacts, applied.)

**Where it lives:** in the package's own ADR, under a `Frozen property table` heading — authored before
implementation, closed out at the gate with actual results. It is evidence, and the ADR owns evidence; it gets no
file of its own.

**Format** — seven fields, six columns. The rows below are B4's, shown as the shape to copy:

| # | Property | Source | Target | Planned assertion | Planned mutation | Note |
|---|---|---|---|---|---|---|
| 1 | `.start` fill `--matcha`, label `--paper` | `v2-library.html:91` `.start{}` | `ZineDock.kt` `StartButton` | `the button is matcha and its label is paper, by day` — pixel probe inside the fill and on the glyph | label takes `--on-matcha` instead | ⚠ *contested:* [D-023](design/V2-SPEC-DEFECTS.md#d-023) is **open** — pin the freeze, and say in the cell that a ruling may move it |
| 2 | fore-edge corner radius | `v2-library.html:112` `.book-ill::after{border-radius:0 4px 4px 0}` on a 3px-wide element | `ZineShelfEmpty.kt` `ForeEdgeRadius` | fore-edge corner pixel probe | `1.5.dp` → the frozen literal `4.dp` | ≡ *candidate:* CSS clamps an overflowing radius to half the box, and **Skia applies the same clamp** — so both render identically |
| 3 | `.start` click destination | `v2-library.html` — **no handler wired at all** | reports the press, routes nowhere | — | — | ∅ *the freeze specifies nothing here; the paper chooser is B5's route hand-over. Nothing is not "goes nowhere on purpose" — it is undecided, and the narrowest thing an implementation can do is hold still* |

- **Property** — the frozen thing, in the freeze's own vocabulary.
- **Source** — file + selector/line. Mandatory. No source, no row.
- **Target** — the file and composable/constant that implements it, or `—` if intentionally unimplemented.
- **Planned assertion** — the named test that will pin it, and *how* it discriminates. "There is a test" is not
  an entry; a test that rebuilds the value it pins cannot fail.
- **Planned mutation** — the specific edit to production that must break that test.
- **Note** — one of two markers, or empty:
  - **`≡` equivalent-mutant candidate** — a mutation you expect to survive because the platform collapses the
    difference. Recording the suspicion up front is what stops the next session writing a flaky test to chase a
    value that has no effect. A survivor is only *proven* equivalent with evidence (e.g. byte-identical goldens).
  - **`∅` intentionally untested** — with its justification **in the cell**. An untested property is a decision
    and gets recorded as one; an untested property with no justification is a gap wearing a marker.
  - **`⚠` contested** — the value is pinned as frozen but an **open** spec-defect entry may move it. Name the
    entry. Pinning a contested value silently is how a package ends up defending a ruling it never had; the
    marker is what turns it back into a question for the owner.

At the gate the table is closed out: planned assertions become the test names that exist, planned mutations become
KILLED / SURVIVED-and-proven-equivalent, and any row that moved gets one line saying why.

#### Row termination — every row ends in exactly one of four states

**No row may remain in an unspecified pending state** ([ADR-087](DECISIONS.md#adr-087)). At the package gate each
row terminates as:

| State | Means | Evidence it must carry |
|---|---|---|
| ✅ **Implemented** | the property is built and pinned | the test that exists, and its mutation KILLED |
| ≡ **Equivalent mutant** | the mutation cannot be killed by any test | the **proof** — e.g. byte-identical goldens — not the argument |
| ⏳ **Owner ruling required** | the property depends on a decision that is not the implementer's | the register entry, by number |
| ✎ **Canonical design amendment required** | the property has no frozen source and needs one | the register entry, and what the amendment must add |

The `∅ intentionally untested` marker is **not** a fifth terminal state — it is a sub-case of ✅, and its
justification is the evidence. A row with no state, or a note that merely says "deferred", is an unfinished table
and the package is not at its gate.

Why this rule exists: B5's table had eight rows with no frozen source, and *"blocked"* was doing four different
jobs at once — two needed a design amendment, four needed a routing ruling, one needed an identity ruling, and one
was not the package's work at all. Four different answers were owed to four different people. Naming the terminal
state names **who owes what**, and a row that cannot name one is a row nobody has actually decided.

### Device verification (mandatory, two passes)
Every UI feature/UX change is verified on a physical device by **two readers of the same screen**:
- **Pass 1 — Developer:** does it behave exactly as specified? (correctness, regressions, a11y via the *platform*
  tree, render parity, persistence, performance.)
- **Pass 2 — First-time user:** would a real person naturally understand what this screen wants? Confusion is
  itself evidence; a screen that is correct but misleading is a defect.
- A feature is accepted only when **both** pass. If the passes disagree, **the disagreement is the finding** —
  resolve it; never let "the code is correct" (Pass 1) overrule "but it misleads" (Pass 2).

Record device, OS version, build (and TalkBack version for a11y passes).

---

## 9. Performance philosophy

- **Faithful first, fast second.** Never trade parity or honesty for speed. But a calm product *feels* calm only
  if it's responsive — jank breaks the illusion as surely as a wrong colour.
- **Respect the engine invariants.** No second draw path, no per-edit render pipeline ([ADR-069](DECISIONS.md#adr-069)) —
  these are also the performance-honest path. Covers are recipes, not cached rasters.
- **The page must never drift, reflow, or resize during editing.** The rigid whole-page pan on IME insets is a
  performance-and-trust invariant: it settles back pixel-identical. Measure it on a real device.
- Optimise with evidence (measure on-device), not by speculation, and never by adding a cache/abstraction the
  problem doesn't demonstrably need.

---

## 10. Design review checkpoints

Implementation is reviewed, like design was. The rhythm:

- **Mid-package (mandatory):** the specialised **"find the assertions that cannot fail"** review, run **after the
  tests exist and before the ADR, the goldens and the review package** — verification-order step 3 in
  [COMPOSE-IMPLEMENTATION-RULES.md](COMPOSE-IMPLEMENTATION-RULES.md#3-the-verification-order). It is a *different
  lens* from the per-PR review, not a rehearsal of it: in B4 the narrow one found three blind assertions the
  general one did not. It sits early because a non-discriminating assertion found at the gate invalidates the
  mutation battery, the goldens and the ADR evidence block built on top of it; found here, it invalidates a test.
- **Per PR:** an independent **Review Agent** (never the implementer) validates *actual repository state* — it
  treats claims as untrusted, reads the diff/tests, classifies findings **Required Fix / Recommended Improvement /
  Observation**, and returns **GO / GO WITH FIXES / NO-GO**. The implementer reconciles every Required Fix or
  surfaces the disagreement explicitly. (See [CLAUDE.md](../CLAUDE.md) multi-agent workflow.)
- **Per phase (roadmap A–F):** a **phase review gate** — parity screenshots captured, side-by-side comparison
  done, device passes accepted, no feature creep. A phase is not "done" until its gate in
  [COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md) is met.
- **Per release:** a **Release Agent** verifies scope/changelog/versioning/known-limitations against
  [ROADMAP.md](ROADMAP.md) and the [PRD](PRD.md).

**One owner gate per package, and it is the last one:** implement → independent review → reconcile findings →
**stop** → owner approves → commit. The intermediate approval that used to sit *before* the review was removed
([ADR-085](DECISIONS.md#adr-085)) — it gated a mandatory step, so it could only ever say yes. The owner still
approves everything that enters history and still rules every design question; only the number of stops changed.

---

## 11. House rules

- **Docs ship with code.** Documentation is updated in the *same change* as the work — stale docs are bugs. Don't
  restate a decision; link to its ADR.
- **One fact, one home.** ADRs own decisions, evidence, owner rulings and the frozen property table; KDoc owns the
  implementation mechanism and local reasoning; the handover owns cross-package lessons and navigation; the
  spec-defect register owns defects found in the frozen artifacts. Cross-reference; never copy. A KDoc that
  restates a ruling outlives it. **Historical ADRs are not rewritten** to match a later ruling — an ADR records
  what was decided *when*, and editing it destroys the evidence that it was decided at all.
- **Build execution:** never `--no-daemon` — the daemon is Gradle's default and works here
  ([the measurement](COMPOSE-IMPLEMENTATION-RULES.md#6-build-execution)). Filter tests while iterating; run full affected suites at the gate. A mutation
  battery may use a parallel worktree only with guaranteed repository isolation — one driver at a time, and its
  files are off-limits until it exits. Execution efficiency only; no verification standard moves.
- **Clean architecture + repository pattern; unidirectional data flow.** MVVM for screens; **MVI for the Bench**
  ([ADR-005](DECISIONS.md#adr-005)). Navigate from UI, not ViewModels; type-safe `@Serializable` routes; single Activity.
- **DI = Hilt + KSP. Async = Coroutines/Flow with injected dispatchers; no LiveData.** Errors cross a sealed
  `Result<T>` boundary; repositories never swallow exceptions.
- **The privacy invariant is a hard gate.** No networking library, no analytics SDK, no code path that uploads
  user content. A PR that adds network access must justify itself against the [PRD principles](PRD.md).
- **Pure helpers over embedded logic; thin seams over platform coupling; document non-obvious invariants where the
  code lives.**
- **Build:** Gradle KTS + version catalog; `jvmToolchain(21)`.
- **A change is done only when:** code + tests pass · docs updated · decisions are ADRs · UI has pixel parity +
  both device passes · no new network/account/cloud dependency · privacy & offline invariants intact.

---

## 12. What protects quality, in one paragraph

Faithfulness is protected by a single source of visual truth (the frozen HTML), a fixed authority chain (§2), an
adversarial review that trusts the repo over the summary (§10), a device-verification pass that asks not only "is
it right?" but "would a stranger understand it?" (§8), and a standing refusal to redesign, add features, or invent
motion during implementation (§4). Hold those, and a new session — with no memory of the design months — can build
the right thing on the first try.

---

*Written 2026-07-28 by the Design Custodian at the close of the V2 Design Program.*
