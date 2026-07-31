# Compose Implementation Rules — the session opener

> **Read this at the start of every Compose implementation session.** It is the distilled, non-negotiable core of
> [COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md) and [V2-CONSTITUTION.md](design/V2-CONSTITUTION.md).
> If you only read one file before writing code, read this — then open the frozen HTML for the screen you're building.

---

## How a package runs

The shape of a package, start to finish. It changes *when* work happens, never *whether* it happens
([ADR-085](DECISIONS.md#adr-085)).

### 1. What you read to start

**The entry point is [COMPOSE-V2-HANDOVER.md](COMPOSE-V2-HANDOVER.md) §0.** It is the navigation layer — it says
where the work actually is, what the last packages cost, and which rulings are live. Then read:

- the **package's own section** of [COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md) (not the whole roadmap),
- **only the ADRs the handover or the roadmap section names**, plus any ADR this package will modify, and
- the **open entries** of [V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md). Always — not only when §0 mentions
  them. An open entry is a live owner question that may govern a value you are about to pin, and routing to it
  must not depend on §0 having remembered to say so.

**Do not read [DECISIONS.md](DECISIONS.md) end to end.** It is ~105k words and a package touches a handful of its
entries; reading it whole costs a large fraction of a session and returns almost nothing the handover has not
already routed you to. The handover *navigates* the ADRs — it does not replace them. When a decision matters, open
that ADR and read it in full; a summary of a ruling is a claim, and this workflow does not run on claims.

### 2. The frozen property table (before you write production code)

Every package opens with a **frozen property table** — the list of properties the freeze specifies, each bound to
where it comes from, what will implement it, and what will prove it. It is the implementation checklist and, at
the gate, the evidence record. Format and rules:
[COMPOSE-IMPLEMENTATION-GUIDE.md §8.1](COMPOSE-IMPLEMENTATION-GUIDE.md#81-the-frozen-property-table).

It exists because *"which frozen properties have no test at all?"* is the question that has found the most
defects in this programme, and asking it at the gate means rework. The table asks it first.

**Every row terminates in exactly one of four states — never in an unspecified pending state**
([ADR-087](DECISIONS.md#adr-087)):

- ✅ **Implemented** — carries the test that exists and its mutation KILLED.
- ≡ **Equivalent mutant** — carries the *proof*, not the argument.
- ⏳ **Owner ruling required** — carries the register entry by number.
- ✎ **Canonical design amendment required** — carries the entry, and what the amendment must add.

`∅ intentionally untested` is a sub-case of ✅, not a fifth state. A row marked only "blocked" or "deferred" is
an unfinished table: those words hide *who owes what*, and B5's eight blocked rows turned out to owe four
different answers to four different people.

### 3. The verification order

Run in this order. Each step's output is the next step's input; running them out of order is how work gets done twice.

1. **Production implementation** — transcribe the freeze.
2. **Focused tests** — one per frozen-property-table row.
3. **The "cannot fail" review** — *mid-package*, see below.
4. **Mutation testing** — every planned mutation in the table; survivors are gaps or proven equivalents, never "probably fine".
5. **Record goldens** — narrow `--tests` filter only.
6. **Verify goldens** — `-Proborazzi.test.verify=true`. A recorded golden is not evidence until this passes.
7. **Independent review** — a Review Agent that did not write the code.
8. **Review reconciliation** — every finding accepted, partially accepted, or rejected with evidence.
9. **Final verification** — full affected suites + all V2 golden classes verify clean.
10. **Owner approval → commit.**

**Any test added or changed at step 8 re-enters at step 3.** It gets the "cannot fail" lens and its own planned
mutation before step 9. This is not optional and it is not new: B4's blind assertion was written **by the fix for
a review finding**, and B4 ran a second nineteen-mutation battery after reconciliation for exactly this reason.
Step 9 cannot catch it — a full green suite is what a blind assertion looks like.

### 4. The mid-package adversarial review

The specialised **"find the assertions that cannot fail"** review runs at step 3 — **after the tests exist, before
the ADR is written, before goldens are recorded, before the review package is assembled.** It is mandatory and it
is separate from the independent review at step 7; the two lenses find different things, and in B4 the narrow one
found three blind assertions the general one did not.

It moved earlier because of what it costs late: a non-discriminating assertion discovered at the gate invalidates
the mutation battery, the goldens and the ADR's evidence block that were all built on top of it. Discovered at
step 3, it invalidates a test.

### 5. Approval gates

**Implement → independent review → reconcile findings → stop → owner approves → commit.**

There is **one** owner gate per package and it sits immediately before the commit. The old intermediate approval —
*"may I now run the review?"* — is removed: it gated a step that is mandatory anyway, so it could only ever say
yes. The owner still approves everything that enters history, and still rules every design question; nothing about
what the owner decides has changed, only how many times a package stops to ask.

### 6. Build execution

- **Do not pass `--no-daemon`.** The daemon is Gradle's own default and it works here — measured on an identical
  up-to-date run: **3s warm vs 19s with `--no-daemon`**. Across a package's ~46 mutation runs (B4's actual count:
  27 + 19) that flag alone costs about a quarter-hour of pure JVM startup, and it was carried forward from an
  environment note rather than
  from a measurement. *(The working tree also carries an **uncommitted** `gradle.properties` with local tuning —
  explicit `daemon`, configuration cache, `parallel`, heap sizes. The measurement above was taken with it. Do not
  assume a clean checkout has it; the `--no-daemon` ruling holds either way, because the daemon is the default.)*
- **Filter tests while iterating** (`--tests "*ZineDockTest*"`); run the **full affected suites at the gate**
  (step 9), never as the inner loop.
- **A mutation battery may run in a parallel git worktree** if — and only if — repository isolation is guaranteed.
  The lock is not optional and the rule that produced it is not softened: *a driver's files are off-limits to you
  until it exits*, and a mutation result is evidence only if you know what was in the file when the test ran.

This section changes execution efficiency only. Every verification standard above it is unchanged.

---

## The checklist

Run down this list before and during every session. If you can't tick a box, stop and resolve it.

- ☐ **HTML is canonical.** The frozen prototype is the specification, not a reference. Match its result.
- ☐ **No redesign during implementation.** The design is frozen. Reproduce it; don't improve it.
- ☐ **If the HTML is wrong, fix the HTML first.** Any change to what a screen should look like or do goes into the
  frozen HTML spec first (owner gate) — then into Compose. Never the reverse.
- ☐ **Pixel parity before optimisation.** Make it faithful first, fast second.
- ☐ **Behaviour parity before refactoring.** Match the interaction/animation/editing feel before you tidy the code.
- ☐ **Accessibility is not optional.** Assert against the *platform* a11y tree (TalkBack / `adb uiautomator dump`),
  not only Compose semantics. Every gesture has a named custom-action twin and a visible non-gesture fallback.
- ☐ **Every deviation requires justification.** Platform truth, an HTML bug, or added a11y are the only reasons —
  and each is written down. "Looks better / easier to build" is not a reason.
- ☐ **Every completed phase ends with screenshots** (light + dark), attached to the review.
- ☐ **Every screen requires a side-by-side comparison** against its frozen HTML before it's called done.
- ☐ **No feature creep.** No control, state, screen, or capability that isn't in the frozen spec. Route new ideas
  to the owner.
- ☐ **Repository truth always beats assumptions.** Read the actual file/commit/test/HTML. A summary or memory is a
  claim, not ground truth.
- ☐ **Migration is additive; V1 survives until C0.** A V2 package lands beside V1 and touches no V1 `src/main`
  file. Each surface enrols in the same commit that migrates it ([ADR-080](DECISIONS.md#adr-080)).
- ☐ **No duplicated design truth.** One property, one frozen source. Never a second copy of a design value —
  including a copy inside a test.
- ☐ **No silent approximation.** If you cannot reproduce a frozen property exactly, say so; do not render
  something close and move on.
- ☐ **Platform limitations fail honestly.** Where the platform cannot do what the freeze asks, omit and document
  it — never emulate it into something that only looks similar (D-014, D-018).
- ☐ **If the frozen design is ambiguous, stop and report.** Silence in the corpus is not an invitation to
  interpolate ([D-020](design/V2-SPEC-DEFECTS.md#d-020--the-shelf-states-a-fixed-two-column-grid-with-no-breakpoint-and-phase-b-verifies-on-foldables)).
- ☐ **Never resolve an owner decision yourself.** Measuring something real licenses *asking*, not deciding. If you
  find yourself writing a new test for when a divergence deserves a register entry, that is the moment to raise one.

---

## The verification standards (none of these is optional)

Six rules, each of which exists because skipping it shipped a defect through a green suite.

- **A test must demonstrate discrimination before it asserts parity.** A test that rebuilds the value it pins
  cannot fail. **The last eight consecutive packages — A6, A7, A8, A9, B1, B2, B3, B4 — each produced at least
  one** assertion blind to the defect class its own name claimed to gate. Every one was caught by review; none by
  the suite. Assume the same failure is in your own work until you have mutated the code and watched a test go red.
- **A recorded golden is not evidence until it has passed verify.** Roborazzi in record mode *overwrites*; only
  `-Proborazzi.test.verify=true` compares.
- **Mutation testing is mandatory.** Every frozen-property-table row has a planned mutation. A survivor is either
  a gap or a **proven** equivalent mutant — proven with evidence, not asserted.
- **Independent review is mandatory** and never self-review: the Review Agent did not write the code, validates
  actual repository state, and returns GO / GO WITH FIXES / NO-GO.
- **The specialised "cannot fail" review is mandatory** and separate from it (verification order step 3).
- **Device verification is mandatory, both passes.** Pass 1 (built right, asserted against the *platform* a11y
  tree) and Pass 2 (would a stranger understand it). If they disagree, the disagreement is the finding.

---

## The invariants you can never break (know these cold)

These are constitutional; violating one is a NO-GO regardless of how good the screen looks.

- **One engine, one draw path** — preview == export == read (`CanvasReplayer`, [ADR-028](DECISIONS.md#adr-028)).
  Never a second way to render a page.
- **No per-edit render** — covers/pages are recipe-driven, not cached rasters ([ADR-069](DECISIONS.md#adr-069)).
- **The page never drifts, reflows, or resizes while editing** — rigid whole-page pan, settles back pixel-identical.
- **Never-silent failure + loss-safe back** — export errors always surface; leaving never loses work ([ADR-051](DECISIONS.md#adr-051)).
- **Print honesty** — no fake "Print"; 100% actual size; Save PDF + Share ([ADR-052](DECISIONS.md#adr-052)).
- **READ-first** — the finished-zine reveal belongs to Read, not the Bench ([ADR-058](DECISIONS.md#adr-058)).
- **Chrome = matcha + strawberry + consequence only.** Warmth lives in *content*, never in new chrome colour.
- **Privacy invariant** — no network library, no analytics SDK, no path that uploads user content. Offline-first.
- **Every screen answers its one user question** (Library "which zine?" · Bench "how do I change this page?" ·
  Read "what have I made?" · Proof "how do I print it right?" · Fold "how do I fold it?").

---

## When you're unsure

1. Open the **frozen HTML** for the screen. It probably answers you.
2. If not, check the screen's **authoring spec** ([V2-BENCH-*](design/), [V2-PROOF-*](design/), [V2-TOKENS.md](design/V2-TOKENS.md)) and the relevant **ADR**.
3. Still unsure, or the frozen artifact itself looks wrong? **Stop and raise it with the owner** — don't guess, and
   don't quietly diverge. A silent divergence creates a second source of truth, which is the one thing this whole
   workflow exists to prevent.
4. Then **log it in [V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md)** — the register for defects found *in* the
   frozen artifacts (contradictions, stale text, two frozen files disagreeing). Raising it in a session is not
   enough; the session ends and the finding goes with it. Entries are not blockers by default — most are logged,
   classified, and left for the design corpus to clean up — but an entry that genuinely blocks says so, and names
   the phase it blocks.

---

## Where a fact lives — one fact, one home

Write each thing once, in the place that owns it, and **cross-reference instead of repeating**. A fact with two
homes drifts, and the stale copy is indistinguishable from the true one.

| Home | Owns |
|---|---|
| **ADR** ([DECISIONS.md](DECISIONS.md)) | Decisions, their evidence, owner rulings, review outcomes, and the package's frozen property table |
| **KDoc** (next to the code) | The implementation *mechanism* and the local reasoning — why this modifier order, what this constant is derived from |
| **Handover** ([COMPOSE-V2-HANDOVER.md](COMPOSE-V2-HANDOVER.md)) | Cross-package lessons, navigation, and the workflow reminders a fresh session would otherwise re-derive |
| **Spec-defect register** ([V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md)) | Defects found *in* the frozen artifacts, and their rulings |

Two corollaries. A KDoc that restates a ruling **outlives** it — link the ADR instead; that exact defect was fixed
in `ZineCover.kt` and reintroduced in `ZineShelf.kt` in the same phase. And historical ADRs are **not rewritten**
to match a later ruling: an ADR is a record of what was decided when, and editing it destroys the only evidence
that the decision was ever made.

---

*A one-page distillation. Authority: [V2-CONSTITUTION.md](design/V2-CONSTITUTION.md) ·
[COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md) · [COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md).
Workflow shape: [ADR-085](DECISIONS.md#adr-085).*
