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
- If both are silent, follow **ARCHITECTURE.md** and the [house conventions](#9-house-rules), and pick the calmest
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

- **Per PR:** an independent **Review Agent** (never the implementer) validates *actual repository state* — it
  treats claims as untrusted, reads the diff/tests, classifies findings **Required Fix / Recommended Improvement /
  Observation**, and returns **GO / GO WITH FIXES / NO-GO**. The implementer reconciles every Required Fix or
  surfaces the disagreement explicitly. (See [CLAUDE.md](../CLAUDE.md) multi-agent workflow.)
- **Per phase (roadmap A–F):** a **phase review gate** — parity screenshots captured, side-by-side comparison
  done, device passes accepted, no feature creep. A phase is not "done" until its gate in
  [COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md) is met.
- **Per release:** a **Release Agent** verifies scope/changelog/versioning/known-limitations against
  [ROADMAP.md](ROADMAP.md) and the [PRD](PRD.md).

---

## 11. House rules

- **Docs ship with code.** Documentation is updated in the *same change* as the work — stale docs are bugs. Don't
  restate a decision; link to its ADR.
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
