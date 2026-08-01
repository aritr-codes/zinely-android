# V2 frozen-spec defect register

> **What this owns:** defects found *in the frozen V2 design artifacts* during Compose implementation —
> contradictions, stale text, omissions, and disagreements between two frozen files.
>
> **What it does not own:** implementation bugs (those are code review findings), design changes (those
> are owner amendments to the freeze), or deviations *from* the spec (those are logged in the PR and the
> relevant ADR).

## Why this register exists

The [implementation rules](../COMPOSE-IMPLEMENTATION-RULES.md) say that when a frozen artifact itself
looks wrong, the implementer **stops and raises it** rather than editing Compose around it or quietly
"correcting" the design. That rule produces findings that belong to the *design corpus*, not to any one
code change — and before this file existed they had nowhere to live, which meant the only ways to record
one were to bury it in an unrelated ADR or to let it evaporate.

Entries here are **not blockers by default.** A defect is logged, classified, and left for the design
corpus to clean up. If one genuinely blocks implementation, that is stated explicitly in its row and the
work stops until an owner ruling lands.

## How to use it

- One entry per defect. State what the artifact says, why it is wrong, and whether implementation depends
  on it.
- **Never fix the frozen artifact from an implementation session.** Amending a frozen surface is an owner
  act ([V2-CONSTITUTION.md §VI](V2-CONSTITUTION.md)); this register is the queue that feeds it.
- When an entry is resolved, mark it Resolved with the commit that cleaned it up. Do not delete it — the
  record of what was once contradictory is what stops it being reintroduced.

---

## Register at a glance (verified 2026-07-29, at the close of Phase A)

*Re-verified 2026-07-30 at the Phase A **closeout**, when the D-002, D-006 and D-016 rulings landed. The
heading keeps its original date because its slug is linked from
[COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md); renaming it would break that link.*

Sixteen defects were raised during Phase A: **ten resolved by owner ruling, six open** *at the close of Phase A*.
Nothing from Phase A awaited a ruling, and nothing from Phase A blocked Phase B. **The Phase A group now stands
at twelve resolved and four open** — [D-010](#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark)
and [D-001](#d-001--v2-benchhtml-header-contradicts-the-freeze-record) were both resolved on 2026-08-01, by the
phase they were deferred to.

**Phase B / B1 raised three more (D-017, D-018, D-019) and Phase B / B2 raised one (D-020). All four were ruled
the same day they were raised.** **Phase B / B3 then raised two — [D-021](#d-021--the-sheets-icons-are-unicode-characters-and-half-of-them-are-not-in-the-apps-own-font)
and [D-022](#d-022--the-librarys-scrim-is-a-theme-invariant-literal-while-the-corpus-publishes-a-theme-aware-one)
— and both were ruled the same day.** **Phase B / B4 raised one, [D-023](#d-023), and it is the first entry
since Phase A to reach the owner unruled.** **Phase B / B5's *planning* then raised three more — [D-024](#d-024),
[D-025](#d-025) and [D-026](#d-026) — and all three blocked B5. **All three were ruled the same day.**
[**D-025**](#d-025-ruling) is fully resolved (*reuse the existing flows; no shelf-level export*).
[**D-026**](#d-026-ruling) is ruled on the question that mattered — *a duplicate gets a **new** cover; duplicate
content, not visual identity* — with the pre-existing-zine backfill following from its principle and flagged for
confirmation. [**D-024**](#d-024-ruling) was ruled the way that costs the most and is worth the most: Loading and Error are
product states, they belong in the **canonical design**, and the answer arrives as an
[**amendment to the frozen HTML**](#d-024-amendment) — approved and **applied on 2026-07-31** — rather than as
prose, so parity stays verifiable. So the count **at the close of Phase B** was **twenty-seven: nineteen resolved, eight open** — the twenty-seventh, [**D-027**](#d-027), raised by B5's **mid-package adversarial review against shipped code** rather than by planning, which is the first entry to arrive that way — exactly
the six Phase A left, plus D-023. **Nothing from Phase B blocks anything.** (Phase C planning has since added five; see below.)

**The amendment is the first one to *add* design to a frozen V2 surface** (D-006 deleted dead specification;
this draws two states that never existed), and it carries two further rulings: **the dock stands in all four
states**, because it belongs to the workspace rather than to the loaded content, and **the loading debounce is
implementation behaviour that is deliberately kept out of the HTML** — a timing threshold is real, but the
canonical design cannot express or verify one, so it is an implementation seam recorded in
[ADR-086](../DECISIONS.md#adr-086).

**B5's three are the first entries in this programme raised *before* any production code was written**, which
is the frozen property table (**[ADR-085](../DECISIONS.md#adr-085)** change 2) doing the job it was added for:
listing every frozen property and its source *first* surfaces the properties that have **no source** while the
cost of finding that out is still a paragraph. All three are the same shape — **the frozen Library is a
prototype with six hard-coded zines, so it never reads a store, never waits, never fails, and never navigates
anywhere.** Integration is the first package to meet the edges of that, and the register's rule is unchanged:
where the corpus is silent, silence is not an invitation to interpolate ([D-020](#d-020--the-shelf-states-a-fixed-two-column-grid-with-no-breakpoint-and-phase-b-verifies-on-foldables)),
and the nearest source to interpolate *from* — V1's answer to the same question — is the one D-020 named.

**D-023 is the fourth of the D-005 / D-011 / D-022 set, which [D-022's ruling](#d-022-ruling) predicted in
terms.** It is also the first entry raised by *review* rather than by implementation: B4 met the divergence,
argued it closed, and shipped an ADR saying so. The argument was that the other three were **broken** while
`--paper` works — and it does not survive contact with the rulings themselves, because D-005's font stack
rendered fine and D-011's `ease` is a valid curve. All three were ruled stale on **authorship date**. The
register's rule is the one B3 wrote when it declined to act on two converging precedents: *measuring
something real licenses **asking**, not deciding.*

**The two B3 rulings went opposite ways, and the pair is the useful reading.** Both entries reported the same
kind of finding — *the implementation measured something the frozen design did not account for* — and the
answers diverge on whether the measurement bears on the **design** or on the **corpus**. D-021: a font's
coverage is an implementation fact, so the frozen characters stand and platform fallback is accepted.
D-022: the Library's scrim contradicts a token the corpus publishes, so the corpus wins and the code departs
from the Library file. **Measuring something real does not by itself license changing what was designed** —
it licenses asking, which is what B3 did in both cases.

One item that is *not* a defect entry is still owed a ruling and is recorded where it lives: Phase B's *"8pt"*
spacing acceptance criterion contradicts the **D-007** ruling — see
[COMPOSE-V2-ROADMAP.md Phase B](../COMPOSE-V2-ROADMAP.md#phase-b--library).

**Phase C planning then raised five — [D-028](#d-028) · [D-029](#d-029) · [D-030](#d-030) · [D-031](#d-031) ·
[D-032](#d-032) — all five before a line of production code, and all five ⏳ awaiting an owner** ([ADR-089](../DECISIONS.md#adr-089)).
They are the second run of the frozen property table doing what it was added for, and they are a **larger** find
than B5's: where B5's three were all one shape (*a prototype never reads a store, waits, fails or navigates*),
these are four different shapes — an accepted ADR contradicted by a later freeze (D-028), net-new capability with
no data model (D-029), a design navigating a product that does not exist (D-030), a screen with no exits (D-031),
and a frozen state with no reachable trigger (D-032). **The count is now thirty-two: twenty-one resolved, eleven
open** — the twenty-first being [D-001](#d-001--v2-benchhtml-header-contradicts-the-freeze-record), closed by
**Phase C / C0** on 2026-08-01, the first Phase C package to land. The full statement of what Phase C owes is
[COMPOSE-V2-ROADMAP.md § Phase C — what is owed before it starts](../COMPOSE-V2-ROADMAP.md#phase-c--what-is-owed-before-it-starts).

**On 2026-08-01 the owner ruled four questions** — OD-1 through OD-4 of [ADR-089 §5](../DECISIONS.md#adr-089) —
**and between them they touched four register entries: D-010, D-029, D-030 and D-004.** Only two of those are
from the Phase C group above; the other two are Phase A entries the rulings reached. The shape of each answer
matters more than the count. **[D-010](#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark)
is resolved by the second [amendment to a frozen V2 surface](#d-010-amendment)** — D-024's precedent applied a
second time, and the first time it has been applied to *two* files at once. **[D-029](#d-029) and
[D-030](#d-030) stay open but stop being Phase C's**: the ruling that Phase C is a parity phase re-seated the
capability they describe, so they now await the phase that takes it, not the phase that meets it. And
**[D-004](#d-004--the-frozen-zine-content-is-set-in-fraunces-the-render-engine-can-only-draw-inter) did not
move** — the ruling wrote the divergence into Phase C's *acceptance criteria* instead, which is the register's
own distinction between fixing a defect and recording one, and it leaves D-004 in Phase D untouched.

That leaves **three entries that block a Phase C package** — D-028 at C6, D-031 at C2, D-032 at C1's keep-clear
row. **None blocked C0, which has since landed**, and **none blocks C1's start**: D-032 fences a single row of
C1's table rather than the package.

| Open | Owing to | One line |
|---|---|---|
| [**D-028**](#d-028) | ⏳ **an owner ruling** — **Phase C / C6** | the Bench offers nineteen swatches to a text element; `Accepted` [ADR-055](../DECISIONS.md#adr-055) pins that control to five |
| [**D-029**](#d-029) | ⏳ **an owner ruling** — **the phase that takes H1** (no longer Phase C) | the holding shelf and `DecorElement` are net-new: no model, no persistence, no scope, and a GC relationship |
| [**D-030**](#d-030) | ⏳ **an owner ruling** — **the phase that takes variable page counts** (no longer Phase C) | the frozen nav runs 12 pages and adds/deletes them; the product has one fixed 8-page format |
| [**D-031**](#d-031) | ⏳ **an owner ruling** — **Phase C / C2, C4** | Font, Size, the Read hand-off and back have no destinations; redo exists in the product and not in the freeze |
| [**D-032**](#d-032) | ⏳ **an owner ruling** — **Phase C / C1** | the keep-clear warn state is never triggered in the freeze, and its written trigger needs face detection |
| [**D-023**](#d-023) | ⏳ **an owner ruling** | the Library labels its primary button `--paper` where the corpus publishes `--on-matcha` — the fourth of the D-005/D-011/D-022 set |
| [**D-027**](#d-027) | ⏳ **an owner ruling** | the action sheet's metadata line ships in a vocabulary the frozen file never uses — cosmetic, sheet-only, and **blocks nothing** |
| ~~[**D-001**](#d-001--v2-benchhtml-header-contradicts-the-freeze-record)~~ | ✅ **RESOLVED 2026-08-01** — [closed by Phase C / C0](#d-001-closure) | `v2-bench.html`'s header contradicted the freeze record. C0 deleted the stale header line and stripped the stale footer clause, keeping D-005's stand-in note and the [D-010 amendment](#d-010-amendment) that sits directly beneath the deleted line |
| [**D-004**](#d-004--the-frozen-zine-content-is-set-in-fraunces-the-render-engine-can-only-draw-inter) | **Phase D** (deferred by ruling; **unchanged** by the 2026-08-01 rulings) | the frozen zine content is set in Fraunces; the render engine can only draw Inter. Phase C **records the divergence in its acceptance criteria** for `.t-title` / `.t-body` rather than fixing it |
| [**D-008**](#d-008--two-of-the-three-frozen-surfaces-specify-no-focus-appearance-and-one-removes-it) | **Phase C** (approach settled) | two surfaces specify no focus appearance and one removes it |
| [**D-009**](#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp) | **Phase C** (approach settled) | no control declares a minimum touch target; most measure under 48dp. *Was "Phase B/C"; Phase B closed on 2026-08-01 without needing it, so C2 is where it lands* |
| ~~[**D-010**](#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark)~~ | ✅ **RESOLVED 2026-08-01** — [amendment applied](#d-010-amendment) | the page shadow was hard-coded to the light theme; the Bench and the Proof now carry a dedicated `--page-shadow` / `--page-contact` pair. Compose deferred to C1 / Phase D |
| [**D-012**](#d-012--the-three-frozen-files-write-three-different-reduced-motion-rules-and-one-of-them-would-strobe) | **Phase C** (deliberately unresolved) | three files write three different reduced-motion rules; one would strobe |

Resolved: **D-001 · D-002 · D-003 · D-005 · D-006 · D-007 · D-010 · D-011 · D-013 · D-014 · D-015 · D-016 ·
D-017 · D-018 · D-019 · D-020 · D-021 · D-022 · D-024 · D-025 · D-026** — full rows in [Resolved](#resolved) below.

**Three of the four Phase A entries still open are open *by owner ruling*, not by neglect** (D-008, D-009,
D-012): their approach is settled and they stay open until the phase that implements the affected surfaces can
verify it. Reading them as unattended work is the misreading this table exists to prevent. The fourth,
**D-004**, is deferred to Phase D by ruling. The other two of the original sixteen closed on the same day and
for the same reason: **D-010** by amendment and **D-001** by C0, each when the phase that owns its surface
arrived and dealt with it. That is the group working as intended, and it is worth saying once:
*deferred to the phase that can verify it* is a schedule, not a shelf.

**Both B3 entries were ruled the day they were raised, and one cost code.** D-021 confirmed B3 as built —
the literal characters stand. D-022 did not: it replaced the Library's stale scrim literal with the corpus
token, which is **the only value in B1, B2 or B3 that does not come from the frozen Library file**. That
exception exists by ruling, and [D-022's entry](#d-022-ruling) is the record a future reader needs when the
file and the code disagree.

**D-020 was ruled on the day B2 raised it** and required no code change, because B2 had transcribed the freeze
rather than closing the gap. Its ruling carries the register's broadest precedent so far — *"future adaptive
layouts require a future frozen design rather than implementation inference"* — which is why it is worth reading
even by a package that never touches a column count.

**The three B1 rulings are worth reading together**, because they answered the same kind of question three
times: what is a printed object? A cover's look is **assigned data, not a derivation** (D-017); a mark the
platform cannot print is **omitted, not approximated** (D-018); and a physical artifact **does not mirror**
however the chrome around it does (D-019). Phase A asked how to build the design system faithfully; these are
product semantics, and they now hold for every surface Phases B–D touch, not just the cover.

---

## Open

> Entries stay in this section after they are resolved, with a ✅ **RESOLVED** status line and the ruling
> appended — see [How to use it](#how-to-use-it). So "Open" is where every defect *lives*; it is the
> **Status** line, not the section, that tells you which are still open.

### D-001 — `v2-bench.html` header contradicts the freeze record

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-bench.html`](mockups/v2-bench.html), header comment and footer note |
| **Found** | 2026-07-28, during Phase A / A1 (independent review of the V2 chrome palette) |
| **Severity** | Documentation defect — **does not block implementation** |
| **Status** | ✅ **RESOLVED 2026-08-01** by **Phase C / C0**, the package that exists for it — see [the closure](#d-001-closure) below |

**What it says.** The file's own header makes both claims, nine lines apart:

- line 3 — `Zinely V2 — the Bench (Editor) — canonical HTML prototype. 🔒 DESIGN FROZEN 2026-07-28 (owner-approved; V2-BENCH-REVIEW.md §E.6).`
- line 10 — `NOT frozen. For owner critique.`

**Why it is wrong.** Line 10 is stale text carried over from the pre-freeze critique revision; the freeze
commit did not strip it. The freeze is real and is the operative status: commit
`4494e95 docs(v2): Bench (Editor) — 🔒 DESIGN FREEZE (owner-approved 2026-07-28)` is the last commit to
touch the file and **is an ancestor of `main`**, the working tree is clean, and
[COMPOSE-IMPLEMENTATION-GUIDE.md §3](../COMPOSE-IMPLEMENTATION-GUIDE.md) records the Bench as frozen at
that commit. Line 3 is correct; line 10 should be deleted.

**Does implementation depend on it?** No. The Bench's `:root` token block — the part A1 consumed, and the
part [ADR-071](../DECISIONS.md#adr-071) makes canonical for shared V2 implementation tokens — is
unambiguous regardless of which header line a reader believes. A1 proceeded on that basis.

**Why it still matters.** The Bench is the single most-read spec in the remaining programme: it is the
canonical source for shared implementation tokens (ADR-071 §2) and the whole authority for Phase C. A file
that tells a fresh session it is "for owner critique" invites exactly the reinterpretation the freeze
exists to prevent — and the risk grows as Phase C reads it more heavily. Clean it up in the design corpus
before Phase C begins.

**Owner disposition (2026-07-28):** *"Correctly identified. Do not modify the implementation. Instead, log
it as a documentation defect against the design repository… No implementation work depends on resolving
it, but it should be cleaned up in the design corpus."*

#### The closure — Phase C / C0, 2026-08-01 {#d-001-closure}

Two deletions, both text, neither inside `<style>` or `<script>`:

1. **Header line 10** — `NOT frozen. For owner critique.` — deleted. Line 3's freeze record stands.
2. **The footer note** — the clause *"Not frozen — for critique."* stripped from the `.foot` paragraph.
   The rest of that sentence — *"Fraunces + Inter stand in as Georgia / system-sans, pending real faces at
   Compose parity"* — is kept verbatim, because it is [D-005](#d-005--the-library-and-the-bench-set-the-same-role-in-two-different-serifs-at-two-different-weights)'s
   still-true stand-in note and not part of this defect. This half was **user-visible**: the header is a
   comment, but the footer renders on the page, so a reader of the prototype was being told it was a draft.

**What C0 deliberately did not do.** No selector, declaration, token, script or geometry was touched;
`v2-bench.html` lost two lines of prose and nothing else. In particular the
[D-010 amendment note](#d-010-amendment) sits directly beneath the deleted header line, and preserving it
was the one hazard in an otherwise trivial edit — it survives intact.

**One consequence worth recording, because it is the sort of thing that rots quietly.** Deleting a line
from the middle of a frozen file moves every line below it. C0 deleted one line, so everything below `:10`
moved up by one — but C0 was not the only edit in flight: the [D-010 amendment](#d-010-amendment) had
already pushed the file down by 9 to 13 lines depending on the region, so the **net** shift against the
last commit is +9 in the header, +10 to +12 through the `:root` blocks, and +13 from `.page` to the end of
the file. Every `v2-bench.html` and `v2-proof.html` citation in [ADR-089](../DECISIONS.md#adr-089)'s frozen
property table, in this register and in the ADR log was re-anchored to the current files, and **each was
re-verified by reading the line it now points at and confirming it is the selector the sentence names** —
arithmetic alone would have missed the six entries that were still on pre-amendment numbers and had never
been swept at all. A citation that silently points one line high is worse than no citation, because it
still looks like evidence.

**Addendum (2026-07-28, A3).** There is a **second** copy of the stale note, in the page footer — *"Fraunces
+ Inter stand in as Georgia / system-sans, pending real faces at Compose parity. Not frozen — for
critique."* (quoted as found; the surviving clause is now at `:380`). It is user-visible in the rendered prototype rather than buried in a comment, so
the cleanup should strip both. Only the trailing "Not frozen — for critique" is stale: the stand-in
clause is accurate and worth keeping (see D-005's closing note).

### D-002 — two frozen cover inks put their titles below AA for normal text

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) lines 68, 79-82 |
| **Found** | 2026-07-28, during Phase A / A2 (modelling the `content.*` namespace) |
| **Severity** | Accessibility question — **did not block A2** |
| **Status** | ✅ **RESOLVED** 2026-07-30 by owner ruling — see the resolution at the end of this entry |

**Measured.** The cover title (`.ct`, `color` from the ink class) against its own ink fill:

| Cover ink | Title on fill | vs 4.5:1 | vs 3.0:1 |
|---|---|---|---|
| Matcha `#F7F2E7` on `#7C8A3F` | **3.380:1** | ✗ fails | ✓ passes |
| Teal `#F7F2E7` on `#47857B` | **3.832:1** | ✗ fails | ✓ passes |
| Strawberry `#4A211F` on `#E27F89` | 4.992:1 | ✓ | ✓ |
| Ochre `#3A2A0E` on `#D19A3C` | 5.535:1 | ✓ | ✓ |
| Paper stock `#2A251E` on `#F1EBDA` | 12.763:1 | ✓ | ✓ |

**The question.** Which floor applies to a cover title? It is genuinely ambiguous, and the ambiguity
is in the corpus rather than in the reading:

- `.ct` is `font-size:1.16rem; font-weight:600` — about **18.56px semibold**. WCAG's large-text
  threshold is **18.66px bold**. The title falls just under on *both* counts (0.1px of size, and
  semibold rather than bold), so treating it as large text is a stretch rather than a fact.
- If it is **normal** text, the floor is 4.5:1 and **Matcha and Teal fail**.
- But cover inks carry **no ★** in [V2-TOKENS.md](V2-TOKENS.md), and
  [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III gates AA specifically on *"the ★-marked pairings"* —
  under which reading no CI floor is owed for cover inks at all.
- Yet [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) Phase B lists **"AA contrast per ink"** among
  the impl-gates that must be met, without naming a level.

So three governing documents point three ways, which is why this is logged rather than decided.

**What implementation did, pending the ruling.** `ZinelyContentInksTest` asserts the **3.0:1** floor —
the minimum defensible gate. This is deliberately *not* a resolution: it is strictly better than no
gate (a genuine regression still fails the build), while refusing to certify at 4.5:1 a pair that does
not clear it. **No frozen value was changed**, and no ink was quietly excluded to make the suite green.

**Why it should not be settled by implementation.** Every available fix is a design change: darken the
two ink fills, lighten their title colour, or raise the title's size/weight past the large-text
threshold. All three alter what the shelf looks like, so all three are owner amendments to the frozen
Library — [COMPOSE-IMPLEMENTATION-GUIDE.md §4](../COMPOSE-IMPLEMENTATION-GUIDE.md)'s golden rule
applies: if the HTML is wrong, the HTML is fixed first.

**✅ RESOLUTION — owner ruling, 2026-07-30.**

> *"The governing floor for cover titles is **3.0:1**. No frozen colours change. No HTML changes. No design
> amendment. If documentation wording implies otherwise, clarify the wording rather than changing the
> design."*

**What this settles.** The three-way disagreement is resolved in favour of the reading the corpus already
supported: cover inks carry no ★, and [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III gates AA on the
★-marked pairings. Matcha (3.380:1) and Teal (3.832:1) both clear 3.0:1 and **stand exactly as frozen**.
None of the three candidate fixes is adopted — no ink is darkened, no title colour lightened, no size or
weight raised.

**What changed in the repository.** Nothing in the design, and nothing in the implementation:
`ZinelyContentInksTest` already asserts the 3.0:1 floor, so the ruling **confirms the gate that exists**
rather than moving it. Per the ruling's wording clause, the one place that implied a stricter level —
[COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) Phase B's impl-gate *"AA contrast per ink"*, which named
no level — now states the 3.0:1 floor and cites this entry.

**One in-code comment was corrected with the ruling** — comment text only, no assertion changed:
`ZinelyContentInksTest` documented the floor as *"contested rather than settled"*, which the ruling makes
false. It now records the ruling and keeps the two readings as the explanation of *why* a ruling was needed.
The test name changed from `…clears the contested 3-to-1 floor…` to `…clears the ruled 3-to-1 floor…`.

### D-003 — the maker palette is ten inks or nineteen, depending on which document you read

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) lines 404-407, 473, 476-483 · [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III · [V2-IDENTITY.md](V2-IDENTITY.md) §4 · [V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §H4 |
| **Found** | 2026-07-28, during Phase A / A2 (independent review of the `content.*` namespace) |
| **Severity** | **Specification conflict** — did not block A2; would have blocked the Phase C ink popover |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**The conflict.** Four governing documents describe the maker's palette, and they do not describe the
same thing.

| Source | What it says the maker palette is |
|---|---|
| [V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §H4 (the directive that created it) | *"**~12–18 named inks** in three bands (**inks** · **paper tints** · **neutrals**)"* |
| **The frozen `v2-bench.html`** (the implementation of that directive) | **19 swatches in three bands** — `INKS` (10) + `TINTS` (5) + `NEUT` (4), plus 3 presets |
| [V2-IDENTITY.md](V2-IDENTITY.md) §4 | *"The **10** named riso spot inks the Bench already froze are the source of truth — **verbatim, no invention**"* — and enumerates exactly the `INKS` band |
| [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III | *"Maker inks (the **10-ink** Bench H4 set)"* — inherited from Identity |

The frozen prototype is unambiguous about behaviour: `bandHTML('Inks',INKS)+bandHTML('Paper
tints',TINTS)+bandHTML('Neutrals',NEUT)` (`:460`) renders all three bands, and `applyInk`
(`:463-470`) is bound to **every** `.sw2` swatch — so a maker can apply any of the nineteen as an ink.
The Constitution and Identity describe that same set as ten.

**Why this is not self-reconciling.** The 4-vs-10 puzzle between V2-TOKENS.md and the Constitution
*was* reconciled elsewhere in the corpus ([V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §8 — cover inks and
in-page inks are distinct axes), and A2 followed that ruling. The same search was run for this one and
the candidate reconciler makes it **worse, not better**: V2-IDENTITY.md §4 groups the ten *"for study
into four bands — greens & earth · warm · cool · neutral"*, which is a **different banding** from the
prototype's three, and then says *"any future widening is a Bench-H4 decision, not an Identity one"* —
pointing authority back at the Bench, which has already widened to nineteen. So Identity simultaneously
fixes the set at ten and delegates widening to a document that shows nineteen.

**Three concrete values have no token home.** The `NEUT` band contains `Slate #5B5347`,
`Stone #8C8269` and `Fog #B7AD93`. None appears in [V2-TOKENS.md](V2-TOKENS.md) or V2-IDENTITY.md's
enumeration. Note that Slate and Stone are byte-identical to light-theme chrome `--ink-soft` and
`--ink-faint` — so if they *are* content inks, the chrome/content wall has two sanctioned crossings
beyond the already-known `Ink #2A251E`, and any future "no content value equals a chrome value" lint
must know that up front. `TINTS` similarly contains `Cream #F1E9D6`, which is close to but **not** the
cover stock `#F1EBDA` — near-misses like that are exactly what a token audit exists to catch.

**What implementation did, pending the ruling.** A2 models **only the `INKS` band** — the ten the
Constitution and Identity name verbatim. This is the smallest defensible claim: porting all nineteen
would contradict the Constitution, and treating ten as complete would contradict the frozen prototype,
so the field is documented as *one band of three* and `ZinelyContentInks.makerInks` carries an explicit
"do not read this as the maker's supplies — read D-003 first" warning for the Phase C session.

**✅ RESOLUTION — owner ruling, 2026-07-28.**

> *"The frozen HTML is the authority. The complete maker palette consists of: **Inks · Paper Tints ·
> Neutrals**. The constitutional '10 maker inks' refers only to the INKS band. Paper Tints and Neutrals
> are **separate categories, not additional inks**. Do not merge them. Do not rename them. Implement all
> three groups exactly as frozen when Phase C arrives. Model them as three distinct collections so the
> architecture reflects the product language rather than flattening everything into a single list."*

Neither document was wrong; the question was miscast. The Constitution's "10 maker inks" was never a
claim about the size of the palette — it names **one band within it**. "Ten inks" and "nineteen
swatches" are both true because *ink* is a category, not a synonym for *swatch*.

**Implemented** in [`ZinelyContentInks`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyContentInks.kt)
as `makerInks` / `paperTints` / `neutrals` — three collections of three **distinct types**, which is
what makes "do not merge them" enforceable rather than merely written down: there is no `List<Color>`
the three can be concatenated into, so flattening the palette is a compile error. Recorded as
[ADR-072](../DECISIONS.md#adr-072) Decision 6. The three `PRESETS` are recipes over the bands rather
than tokens and remain Phase C's.

**Two things this surfaced that outlive the defect**, both now pinned by test:

- **Three sanctioned chrome/content value coincidences.** Neutral `Ink #2A251E`, `Slate #5B5347` and
  `Stone #8C8269` are byte-identical to light chrome `ink`, `inkSoft` and `inkFaint`. A future "no
  content value equals a chrome value" lint would therefore be **wrong**; the only value-level rule the
  corpus states is the `consequence` exclusion.
- **`Ink` legitimately appears in two bands** (spot ink *and* neutral), verbatim from source — pinned so
  it is never de-duplicated into one, which would silently change what the popover offers.

### D-004 — the frozen zine content is set in Fraunces; the render engine can only draw Inter

| | |
|---|---|
| **Artifacts** | [`v2-proof.html`](mockups/v2-proof.html) lines 120-137, 224 · [`DocumentFontRegistry.kt`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/DocumentFontRegistry.kt) lines 101-113 |
| **Found** | 2026-07-28, during Phase A / A3 (typography) |
| **Severity** | **Capability gap between a frozen spec and the shipped engine** — does not block A3 or any of Phase A |
| **Status** | Open — **deferred to Phase D by owner ruling (2026-07-28)**; explicitly *not* decided by A3 |

**What the spec says.** The Proof's zine content — the block the file itself labels `/* zine content
(real, not lorem) */` — is set entirely in `var(--serif)`, which `:33` defines as
`'Fraunces',Georgia,'Times New Roman',serif`:

| Selector | Role | Style |
|---|---|---|
| `.cover h2` (`:124`) | cover title | Fraunces 500, 27px |
| `.cover .sub` (`:125`) | cover subtitle | Fraunces **italic**, 13px |
| `.h` (`:126`) | page heading | Fraunces 500, 19px |
| `.b` (`:127`) | page body | Fraunces 400, 12.5px |
| `.pull` (`:128`) | pull-quote | Fraunces **italic**, 21px |
| `.zlist` (`:129`) | list | Fraunces 400, 13px |
| `.zcap` (`:133`) | photo caption | Fraunces 400, 12px |
| `.backc p` (`:137`) | back cover | Fraunces **italic**, 12px |

**What the engine carries.** `DocumentFontRegistry.Bundled` declares exactly one family — `Inter`, with
regular/bold/italic/bold-italic assets — and `defaultFamilyName = INTER`. A document asking for Fraunces
does not fail; `resolve()` falls back, and the page renders in Inter with no error surfaced. So the
mismatch is invisible at runtime, which is what makes it worth logging now rather than discovering it in
Phase D.

**Why this is not A3's to fix.** A3 bundles Fraunces for **chrome**, under `core/ui/src/main/res/font/`.
The render module has its own font pipeline reading `assets/fonts/` by design, because
[ADR-028](../DECISIONS.md#adr-028)'s one-engine rule means preview, export and read must all draw from
the *same* registry — adding a family there changes what every exported PDF can contain. That is a
document-model decision with an on-disk format consequence, not a theming one.

**It is also not a one-line addition.** The frozen content needs **italic** Fraunces (`:125`, `:128`,
`:137`), which chrome nowhere does — so honouring the spec means bundling a cut A3 deliberately did not, and
`DocumentFontFamily` has slots for regular/bold/italic/boldItalic but not for a 500 weight, while the
frozen content asks for 500 at two of its seven roles.

**Owner decision requested.** Three shapes, none of them implementation's to pick: (a) bundle Fraunces
into the render registry, accepting the APK cost and deciding how 500 maps onto a four-slot family; (b)
rule that the Proof's serif content is *mock illustration* of a user document rather than a spec for the
default document style, in which case nothing is owed and the Proof's own note should say so; or (c)
narrow it — e.g. Fraunces for cover titles only, where the product's voice actually shows.

**A prior note this replaces.** `DocumentFontRegistry`'s KDoc already anticipates the question — *"One
family today. That is a statement about what is bundled, not about what the registry supports"* — and
correctly routes expansion to *"the designer's font/preset curation, gated on its freeze"*. That freeze
has now happened, and it asks for a second family. The comment is still true; it is simply no longer
waiting on anything.

**✓ OWNER RULING — 2026-07-28: deferred to Phase D. Entry stays open; no work is owed before then.**

> *"Do not solve this during Phase A. Leave the current engine unchanged. This is an architectural
> decision governed by the one-engine rule ([ADR-028](../DECISIONS.md#adr-028)) and belongs to Phase D
> when the rendering/export pipeline is implemented. **No workaround. No temporary font substitution. No
> second rendering path.**"*

The three prohibitions are the operative part and are worth restating for the Phase B and C sessions that
will meet this defect before Phase D does. Each names a shortcut that would look locally reasonable:

- **No workaround** — do not paper over the silent fallback at the call site, and in particular do not
  "fix" it by making `resolve()` throw or warn. The silence is a symptom of the real decision, not the
  decision itself.
- **No temporary font substitution** — do not map Fraunces onto Inter, onto a system serif, or onto the
  chrome font resources A3 bundled. `:core:ui`'s `res/font/` and the render module's `assets/fonts/` are
  separate pipelines by design, and bridging them "just for now" is exactly how a second path starts.
- **No second rendering path** — [ADR-028](../DECISIONS.md#adr-028) requires preview, export and read to
  draw through one engine. A serif-capable path added anywhere short of the registry breaks that
  invariant, and it breaks it invisibly, since all three surfaces would still *render*.

So the correct behaviour until Phase D is that zine content continues to draw in Inter, and this entry
— not a code comment, not a TODO — is where that is recorded.

**Phase C's acceptance criteria now record the divergence — and this entry did not move. Owner ruling
OD-4, 2026-08-01** ([ADR-089 §5](../DECISIONS.md#adr-089)):

> *"Phase C's acceptance criteria explicitly exclude literal document-typeface parity. The exception is
> limited to `.t-title` and `.t-body`. Everything else in the frozen Bench remains literal parity. …
> Do not move D-004 forward. Do not invent an intermediate font solution."*

Phase C planning had put the question because a phase whose gate is *pixel parity* cannot pass one on a
page it is not allowed to draw correctly. The ruling answered it the way that costs nothing and hides
nothing: the criterion is **narrowed in writing**, to exactly the two Bench selectors that draw document
text, and the divergence is stated in C1's golden KDoc rather than silently baselined. Note how small the
exception is — the Bench's four serif *chrome* headings and even the "Fraunces" chip's own type are
literal parity today, because they are chrome and draw through `:core:ui`, not through the engine. The
gap is the page, and only the page.

The three prohibitions above are untouched by this, and so is the deferral to Phase D. **Recording a
divergence is not fixing it**, and this entry stays open precisely so the difference between the two
remains legible at the Phase D gate.

### D-005 — the Library and the Bench set the same role in two different serifs at two different weights

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 37, 125, 148, 163 · [`v2-bench.html`](mockups/v2-bench.html) lines 32, 193, 211, 249 · [`v2-proof.html`](mockups/v2-proof.html) lines 177, 224, 246 |
| **Found** | 2026-07-28, during Phase A / A3 (typography) |
| **Severity** | **Disagreement between two frozen files** — did not block A3; ruling owed before Phase B implements a Library heading |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**What they say.** The two files were frozen a day apart and declare the voice face differently.

| | Library (frozen 2026-07-27) | Bench + Proof (frozen 2026-07-28) |
|---|---|---|
| **How the serif is declared** | a literal stack at each call site — `.serif{font-family:"Iowan Old Style","Palatino Linotype",Palatino,Georgia,serif}` (`:37`) | a token — `--serif:'Fraunces',Georgia,'Times New Roman',serif` (`:22`) |
| **Named face** | Iowan Old Style / Palatino / Georgia. **Fraunces appears nowhere in the file.** | Fraunces |
| **Weight for a titled heading** | **600** — `.sh-ttl` (`:125`), 1.12rem | **500** — `.sheet h3` (`:198`, 17px), `.pgrid .pgh h3` (`:236`), `.inkpop h4` (`:180`), proof `.dhead h3` (`:163`), `.done h4` (`:232`) |

**The family half is already settled; the weight half is not.**
[V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III fixes *"Fraunces (voice) + Inter (work). Permanent."* —
which makes the Library's Iowan stack **stale text from before the `--serif` token existed**, not a
competing choice. That part needs no ruling, only a corpus cleanup.

The weight does need one. `.sh-ttl` at 600 and `.sheet h3` at 500 are the same role — a short serif
heading naming a thing, at ~17-18px — rendered two visibly different ways. And it is not the kind of
difference that survives being guessed at: the Library's headings are the first type a user ever sees,
so a Phase B session picking 500 "to match the Bench" or 600 "because the Library is frozen" is choosing
the product's opening voice by coin-flip.

Note also that the Library was authored against a *fallback* face. Iowan Old Style and Georgia are
heavier on the page than Fraunces at the same nominal weight, so 600 may have been chosen to look right
in Georgia and not because 600 is wanted in Fraunces. That is an argument for re-reading the Library at
Fraunces before ruling, rather than for reading its number literally.

**What implementation did, pending the ruling.** A3 bundles Fraunces at **400, 500 and 600** and
prejudges nothing. Whichever way the ruling lands, the face is already present and Phase B is a
per-component value, not a font-bundling change. 400 is independently required regardless (proof
`.foldcap`, `:210`, is serif body text at 14px with no weight set), so only one of the three cuts is
carried speculatively.

**Owner decision requested.** Does the Library's serif heading render at **500** (harmonising with the
Bench and Proof) or at **600** (as its own frozen file states)? And separately — a documentation
cleanup, not a design question — the Library's literal `"Iowan Old Style",Palatino,Georgia` stack should
become `var(--serif)` so a future reader is not told the product has two serifs.

**✅ RESOLUTION — owner ruling, 2026-07-28.**

> *"The Constitution is the higher authority. The canonical serif family for V2 is **Fraunces**. The
> canonical weight is **500**. The Library's earlier 600 weight reflects its original fallback stack
> (Iowan/Georgia), not a lasting design decision. When Phase B implements the Library, use **Fraunces 500
> for the shared serif role** unless a specific frozen component explicitly requires another weight."*

The ruling resolves the question by **authority rather than by arbitration**, which is why it settles
cleanly: two frozen files disagreeing is not a tie to be broken on the merits, because
[V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III already sits above both. It also confirms the reading this
entry raised on its own evidence — that 600 was chosen to look right *in Georgia*, and does not transfer
to Fraunces at the same nominal number.

**What Phase B does.** The Library's serif headings (`.sh-ttl`, `.shelf-head h1`, `.empty h2`) render in
**Fraunces 500**, matching the Bench and Proof. The escape clause is deliberately narrow: *"unless a
specific frozen component explicitly requires another weight"* means a component whose frozen CSS states
a weight for a reason of its own — not a component that merely happens to sit in the Library and
therefore inherits 600 from `.sh-ttl`. If a Phase B session believes it has found such a component, that
belief is a new register entry, not a local decision.

**What implementation does now: nothing.** A3 already bundles 400/500/600 and selects between them
nowhere, so the ruling requires **no code change** — which is what "bundling both prejudges nothing"
meant when it was claimed. The 600 cut **stays bundled** and is not now dead weight: V1's
`ZinelyFonts.Voice` is built on it and remains live until **C0** retires the V1 layer. Whether V2 chrome
still needs a 600 cut after C0 is a question for C0, not for this entry.

**Still owed to the design corpus** (documentation, not implementation): the Library's literal
`"Iowan Old Style","Palatino Linotype",Palatino,Georgia,serif` stack at `:37` should become `var(--serif)`,
and `.sh-ttl`'s `font-weight:600` at `:125` should become `500`, so that the frozen file stops stating a
family and a weight the Constitution has overruled. Until that lands, **this entry is the authority and
the Library HTML is stale on both counts** — which is precisely the situation the register exists to make
visible rather than leave to be rediscovered.

**A related note, not a defect.** The Bench and Proof both state that *"Georgia / system-sans stand in
here pending real faces at parity"* (`v2-bench.html:7`, `v2-proof.html:7`). The Library carries no such
note — but it does not need one, because it never names Fraunces at all: its Georgia is a genuine
declared fallback rather than a stand-in, which is the same fact from the other direction. Either way
**none of the three prototypes renders the shipping typefaces**, so a browser screenshot of them is not
a type-parity target — only their declared families, sizes, weights and line-heights are. A9's Roborazzi
baselines and any Phase B parity pass must compare against the CSS, not against a rendering of it.

**Where else the split shows.** The disagreement is not confined to sheet headings. The Library's zine
cover title (`.ct`, `:68`, taking its family from `.serif` at `:37` via the markup at `:150`) is serif at
**600**; the Proof's cover title (`.cover h2`, `:110`) is serif at **500**. Those two are not strictly the
same object — one is a shelf card drawn by chrome, the other is mock zine content drawn by the render
engine (D-004) — so this is listed as corroboration that 500-vs-600 is a systematic split across the two
freeze dates, not as a fourth independent defect.

### D-006 — the only shape token in V2 is declared and never used

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) line 24 · [`v2-proof.html`](mockups/v2-proof.html) line 24 |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | Dead specification — **did not block A4** |
| **Status** | ✅ **RESOLVED** 2026-07-30 by owner ruling — see the resolution at the end of this entry |

**What it says.** Both files declare `--r:18px` in `:root`, alongside `--serif`, `--sans`, `--settle`
and `--standard`. The Library declares no `--r` at all.

**Why it is wrong.** `--r` is referenced **zero times** in either file, and **no `border-radius` of
18px exists anywhere in V2** — not through the token and not as a literal. Every one of the sixteen
distinct chrome radii is written out at its use site: `50%`, 22, 20, 16, 14, 13, 12, 11, 10, 9, 8, 6,
5, 4, 3 and 2px, plus five asymmetric values such as the Library cover's `6px 9px 9px 6px`, whose
tighter left corners read as the spine of a printed thing. The three files do not even agree with one
another: the Library's bottom sheet is 20px where the Bench's is 22px, for the same kind of object.

So `--r` is a leftover from a revision in which a shared corner radius existed and was then designed
away, one component at a time, without the token being deleted.

**Does implementation depend on it?** No, and A4 deliberately did **not** port it. A token naming a
value that nothing uses is worse than no token: it invites a Phase B session to "restore consistency"
by applying an 18px radius the design never had, and it would pass review, because a token in the
foundation looks like an intention. `ZinelyV2Dimens` therefore carries no radius at all, and the
absence is documented at the code with a pointer here.

**✅ RESOLUTION — owner ruling, 2026-07-30.**

> *"The unused `--r` token is confirmed to be **dead specification**. **Delete it from the frozen HTML.** Do
> not introduce an 18px radius token into the implementation. Record the ruling."*

**Applied 2026-07-30.** `--r:18px;` is deleted from the `:root` block of both
[`v2-bench.html`](mockups/v2-bench.html) and [`v2-proof.html`](mockups/v2-proof.html) — the only two
declarations, and the token was referenced zero times in either file. This is an **owner-authorised
amendment to a frozen artifact** under [V2-CONSTITUTION.md §VI](V2-CONSTITUTION.md); it is a deletion of
dead specification, so no rendered pixel of either prototype changes and no parity target moves.

**No implementation change is owed.** A4 never ported it, which the ruling confirms as correct:
`ZinelyV2Dimens` publishes **no radius token**, and Phase B, C and D transcribe each component's radius at
its use site exactly as frozen — including the Library cover's asymmetric `6px 9px 9px 6px`. The hazard the
entry named is now closed at the source: there is no 18px token left for a later session to "restore
consistency" from.

**Two in-code corrections came with the ruling**, both mechanically required by it:

- `ZinelyV2Dimens`' KDoc described `--r` as *declared* at `v2-bench.html:24` / `v2-proof.html:24` and D-006
  as an open finding. It now records the deletion and the ruling. **KDoc only — no token added, no value
  changed.**
- `ZinelyV2DimensTest` **asserted the two declarations were present** (`assertEquals(… 2, declarations)`), so
  the deletion turned it red. The assertion is **inverted, not deleted**: it now pins the declarations at
  **zero**, so re-introducing `--r` fails the build. The zero-`var(--r)`-references and no-18px-literal
  assertions are unchanged.

### D-007 — the constitutional 8pt rhythm is not observable in the frozen CSS

| | |
|---|---|
| **Artifacts** | [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III *Spacing* · [V2-RESEARCH.md](V2-RESEARCH.md) §2.4, §3.9 · [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) Phase A deliverables · all three frozen mockups |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | **Conflict with the highest authority in the corpus** — held A4's spacing deliverable; would have blocked Phase B |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**What the governing documents say.** [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III, under the
invariants that are *"fixed for the life of V2"*:

> *"An **8pt rhythm** governs layout. Spacing is calm and generous; the page is given room."*

[V2-RESEARCH.md](V2-RESEARCH.md) §2.4 defines it concretely — *"Adopt an **8pt spacing scale**
(4/8/16/24/32/48…), tokenized"* — and §3.9 asks for *"one tokenized 8pt `space.*` scale for all
margin/padding/gap"*. [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) lists A4's deliverable as
*"the 8pt rhythm and the calm elevation model as reusable primitives"*.

**What the frozen CSS does.** Every `padding`, `margin` and `gap` value across the three files:

| | multiples of 8 | multiples of 4 | neither |
|---|---|---|---|
| Chrome only (hand-classified, N=204) | **16.7%** | 38.2% | 61.8% |
| All CSS incl. scaffolding (N=252) | **17.1%** | 37.3% | 62.7% |

The two measurements were taken independently and agree, so the finding does not depend on where the
chrome/scaffolding line is drawn. The most frequent values are **12** (25), **8** (23) and **2** (19),
followed by a continuous tail of 6, 7, 9, 10, 13 and 14 that no 4pt reading accommodates — 2, 6, 7, 9,
13 and 14px together account for more chrome spacing than every multiple of 8 combined. A distribution
with no gaps between 1 and 20px is the signature of hand-tuned optical spacing, not a stepped scale.

**Why this is not the same call A3 made.** A3 declined to publish a *type* scale on the same kind of
evidence, and that was safe: no document mandates one, so "there is no scale" contradicted nothing.
Here the invariant is stated by the **Constitution**, which outranks the frozen HTML. Both available
readings are therefore decisions, and both are expensive:

- **Transcribe the frozen literals.** Pixel-parity passes. The constitutional invariant is violated on
  every screen, and the foundation ships a "spacing scale" that is 62% off-grid.
- **Snap to 8pt.** The invariant holds. Roughly three in five spacing values in the frozen trilogy
  change, which is a **visual redesign of three DESIGN-FROZEN surfaces** — an owner amendment under
  [COMPOSE-IMPLEMENTATION-GUIDE.md §4](../COMPOSE-IMPLEMENTATION-GUIDE.md), and it would guarantee
  Phase B fails its own pixel-parity gate against the HTML it is implementing.

**The freeze appears to have anticipated this.** `v2-library.html:8`, *inside* the DESIGN FROZEN
banner, lists **"8pt rhythm"** among the *implementation-time gates (P3)* — alongside AA contrast per
ink and screen-reader paths. So the frozen file does not claim to satisfy the rhythm; it defers it to
implementation. That is a strong hint that the intended answer is "snap at implementation time" — but
it is a hint, and acting on it would silently redesign three frozen surfaces, which is precisely the
act the implementation rules reserve for the owner.

**What implementation did, pending the ruling.** **Nothing.** `ZinelyV2Dimens` publishes no spacing
value at all. This is not the A3 pattern of shipping a minimum defensible gate — here there is no
minimum: any scale published now *is* the answer, since Phase B would build against it. The rest of
A4 (the hairline, the focus stroke, the shadow primitive, the radius finding) does not depend on the
ruling and shipped normally.

**Owner decision requested.** Does V2 spacing (a) transcribe the frozen literals, accepting that the
constitutional 8pt invariant describes an aspiration the frozen surfaces do not meet; (b) snap to the
4/8/16/24/32/48 scale at implementation time, accepting that this amends three frozen surfaces and
that Phase B's parity gate must then compare against an amended HTML rather than today's; or (c)
something narrower — e.g. 8pt governs *layout* spacing (page margins, section gaps, the rhythm a user
perceives) while component-internal padding stays as frozen, which would reconcile the Constitution's
own wording (*"governs **layout**"*) with the measurement, since the off-grid values are concentrated
in component internals?

*(Recorded for the record, not as a resolution: (c) is the reading that requires the fewest documents
to be wrong. It is not adopted, because "which values are layout and which are component-internal" is
itself a design judgement across three frozen surfaces.)*

**✅ RESOLUTION — owner ruling, 2026-07-28. Close to option (c), and cleaner than the question assumed.**

> *"**Do not publish a V2 spacing scale.** The Constitution expresses an implementation **aspiration**,
> not a token inventory. The frozen HTML remains the canonical authority. Until the design corpus
> explicitly defines a shared spacing scale, spacing continues to live at the component level exactly
> as frozen. Macro layout rhythm should continue to follow the constitutional guidance where
> appropriate, but **no global spacing token set should be introduced**."*

The ruling dissolves the conflict rather than picking a side, by reading the two documents as
different kinds of statement: §III is an **aspiration about layout**, not an inventory of tokens, so a
frozen surface whose component padding is 13px is not in violation of it. That reading was available
in the Constitution's own wording — *"an 8pt rhythm governs **layout**"* — and this entry reached for
it as option (c) but declined to adopt it, because drawing the layout/component line across three
frozen surfaces is itself a design judgement. The ruling makes the line unnecessary: **nothing is
tokenised**, so there is no boundary to draw and no token for a component to route around.

**What implementation does: nothing, and now permanently.** `ZinelyV2Dimens` publishes no spacing
value, which is already its state — A4's held item is now its finished one. Phase B onward transcribes
each component's frozen `padding` / `margin` / `gap` at its call site, exactly as
[ADR-073](../DECISIONS.md#adr-073) established for type and [ADR-074](../DECISIONS.md#adr-074) for
radius. The V2 foundation therefore carries **no scale of any kind** — not type, not radius, not
spacing — and that consistency is the finding rather than an accident: the frozen trilogy is a
hand-tuned optical design, and the foundation's job is to hold what is genuinely shared, not to impose
a system the design does not have.

**What would reopen it.** Only the design corpus explicitly defining a shared spacing scale — the
ruling's own condition. The companion test does **not** guard this: a "literals stand" ruling needs no
corpus change, so `ZinelyV2DimensTest` stays green either way, and that limitation is stated at the
test. This entry is the record.

### D-008 — two of the three frozen surfaces specify no focus appearance, and one removes it

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 61, 84, 105, 149, 170 · [`v2-bench.html`](mockups/v2-bench.html) lines 112, 222 · [`v2-proof.html`](mockups/v2-proof.html) — no focus rule anywhere. *(Re-anchored 2026-08-01: the citations were captured before the [D-024 amendment](#d-024-amendment) moved the Library and the [D-010 amendment](#d-010-amendment) moved the Bench.)* |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | **Accessibility gap in the frozen specification** — does not block A4 |
| **Status** | **Open by owner ruling** (2026-07-28) — approach settled; stays open until Phase C implements and verifies the affected surfaces |

**What exists.** The Library specifies focus on four product controls (and one prototype-only control), all at **2px**:

| Control | Line | Rule |
|---|---|---|
| `.zine:focus-visible` | 61 | `outline:2px solid var(--matcha-text); outline-offset:6px; border-radius:9px` |
| `.more:focus-visible` | 84 | `outline:2px solid currentColor; outline-offset:0` |
| `.start:focus-visible` | 105 | `outline:2px solid var(--ink); outline-offset:3px` |
| `.retry:focus-visible` | 149 | `outline:2px solid var(--matcha-text); outline-offset:3px` — **added by the [D-024 amendment](#d-024-amendment)**, which drew the Error state this control belongs to. It is the one focus rule in the corpus that did not exist at the freeze |
| `.ctl:focus-visible` | 170 | `outline:2px solid var(--matcha-text); outline-offset:2px` — **prototype-only control**, not product UI; listed for completeness because it is the fifth and last focus rule in the corpus |

**What does not exist.** The **Bench and the Proof contain no `:focus`, `:focus-within` or
`:focus-visible` rule at all** — between them roughly two dozen interactive controls, including every
control in the editor. The Proof alone has some fourteen `<button>` elements with no specified focus
appearance.

**Worse than an omission, in one place.** The Bench sets `outline:none` on `.el` (`:112`) and
`.search input` (`:222`). `.el` is not decorative: it carries `tabindex="0"`, `role="button"` and an
Enter/Space `keydown` handler (`:516-517`), so it is a deliberately keyboard-operable control whose
focus indicator is deliberately removed, with nothing put in its place. A keyboard or switch-access
user can move focus through the Bench's elements and see nothing at all.

**Why this is the register's and not a code review's.** The frozen HTML is the specification. An
implementer who invents a focus ring for the Bench is designing a visual treatment for the product's
most complex surface — colour, offset, radius and how it reads against a user's own artwork — which
is a design act, not a parity act. An implementer who faithfully reproduces `outline:none` ships an
accessibility defect knowingly. Neither is implementation's call.

**Does implementation depend on it?** Not in A4 — `ZinelyV2Dimens.FocusRingWidth` carries the 2px all
four rules agree on, and no offset, since the Library's three product rules use three different
offsets (6px, 3px, 0). It binds in **Phase C**, when the Bench's controls are built.

**Owner decision requested.** What is the focus appearance for the Bench and the Proof? The narrowest
answer that resolves it is to extend the Library's treatment (2px `matchaText`, per-component offset)
across all three surfaces and delete the two `outline:none` rules — but that is a change to a frozen
surface and so belongs to the owner.

**✓ OWNER RULING — 2026-07-28: a platform responsibility, not a redesign. Entry stays open until Phase C.**

> *"These are **platform responsibilities, not redesign opportunities**. Implement accessible focus
> indicators … during Phase B and C in a manner that is **visually subordinate to the frozen design**.
> **Do not modify the visual design solely to satisfy these requirements.** Where the platform requires
> accessibility behaviour not represented in the HTML, the implementation guide already authorises
> those additions. Keep these defects open until the affected product surfaces are implemented and
> verified."*

This settles the question the entry could not: whether inventing a focus ring for the Bench is a design
act. It is not — it is a **platform obligation the HTML does not model**, in the same category as
TalkBack ordering or a touch-exploration path, and
[COMPOSE-IMPLEMENTATION-GUIDE.md](../COMPOSE-IMPLEMENTATION-GUIDE.md) already sanctions additions of
that kind. What the ruling forbids is the *other* move: reaching for a visual change — recolouring,
resizing or re-spacing a control — in order to make focus read well. **The indicator adapts to the
design; the design does not adapt to the indicator.**

**Consequence for Phase C.** The Bench's `outline:none` on `.el` and `.search input` is **not**
transcribed: reproducing it would ship a control that is keyboard-operable and invisibly focused. The
Library's own treatment (2px, `matchaText`, per-component offset) is the reference for what
"subordinate" means here, since it is the only focus appearance the design ever authored.

**Why it stays open.** The ruling settles the *approach*, not the outcome. The entry closes when Phase C
has implemented focus on the Bench and Proof **and** both device-verification passes have confirmed it
against the platform accessibility tree — the only place a focus indicator's real behaviour can be
read.

### D-009 — no control in the frozen trilogy declares a minimum touch target, and most measure well under 48dp

| | |
|---|---|
| **Artifacts** | all three frozen mockups (control sizing throughout) · [V2-RESEARCH.md](V2-RESEARCH.md) §2.4 |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | **Accessibility gap in the frozen specification** — does not block A4 |
| **Status** | **Open by owner ruling** (2026-07-28) — approach settled; stays open until **Phase C** implements and verifies the affected surfaces. *(Read "Phase B/C" until 2026-08-01; Phase B closed without meeting a control this bore on, so it lands in C2.)* |

**Measured.** Not one selector in any of the three files declares `min-height` or `min-width` on an
interactive control. Every `min-*` in the trilogy is either `min-height:100vh` on `body`, a
`min-width:0` flexbox overflow fix, or the Proof's `min-height:2.6em` reserving two lines of fold
caption. Control sizes are set with explicit `width`/`height` or fall out of padding, and they land
mostly below the 48dp floor:

| Surface | Control | Declared box |
|---|---|---|
| Bench | `.mat-item` / `.icon-btn` / `.add` | 46×46 · 44×44 · h44 |
| Bench | `.gridbtn` · `.styletb .chip` · `.chip2` | 34×34 · h34 · h32 |
| Bench | `.pthumb` · `.sw2` · `.toggle` | 26×34 · 26×26 · 38×22 |
| Bench | `.tray .fold` | ≈23×19 — the smallest control in V2 |
| Proof | `.fnav` · `.iconbtn` · `.dclose` | 40×40 · 38×38 · 30×30 |
| Library | `.more` | 34×34 |
| Library | `.start` · `.sheet .act` | ≈49 · ≈50 — the only controls that clear the floor by construction |

**Why this is one finding with D-007 and still needs its own answer.** [V2-RESEARCH.md](V2-RESEARCH.md)
§2.4 predicts exactly this coupling — *"generous 8pt spacing gives 48dp targets for free"*. The
frozen surfaces did not take the 8pt spacing, and did not get the targets. So the two defects share a
cause. They do **not** share a fix: if the owner rules under D-007 that the frozen literals stand, the
targets are still below the floor and still need an answer, so this is logged separately rather than
folded in.

**Why implementation must not quietly fix it.** The Android floor is 48dp and is not negotiable, but
the two ways to reach it are both design changes: grow the controls (which reflows three frozen
layouts — the Bench's filmstrip and swatch grid could not hold their current counts at 48dp), or keep
the visual size and extend the *touch* area beyond the drawn bounds. The second is invisible in a
screenshot and is very likely the intended answer, but it is a decision about overlapping hit regions
in a dense editor, and getting it wrong produces controls that steal each other's taps.

**Does implementation depend on it?** Not for the *fix*, but A4 does now publish the floor:
`ZinelyV2Dimens.MinTouchTarget = 48.dp`. An earlier draft withheld it on the grounds that the V2 spec
states no minimum and asserting one would be implementation inventing a design value. The independent
review falsified that, and correctly: **48dp is a platform floor, not a design value** — V1's
`ZinelyDimens` says so in as many words — and the V2 spec's silence is not a contradiction of it. It
also prejudges nothing, because *both* answers below presuppose the number; withholding it would only
have meant Phase B building 26×26 controls with nothing in the foundation naming what they must clear.
The defect is the gap between that floor and the frozen sizes, and it binds in **Phase B**.

**Owner decision requested.** Do the frozen control sizes stand with the touch area extended beyond
the drawn bounds to 48dp, or do the controls themselves grow (and the layouts that hold them change)?

**✓ OWNER RULING — 2026-07-28: as D-008 — platform responsibility, visually subordinate.**

> *"… Implement … minimum touch targets during Phase B and C in a manner that is **visually subordinate
> to the frozen design**. **Do not modify the visual design solely to satisfy these requirements.**"*

Read against this entry's two options the ruling is decisive: *"do not modify the visual design"* rules
out **growing the controls**, so the floor is reached by **extending the touch area beyond the drawn
bounds** — the invisible option. The Bench's filmstrip and swatch grid keep their frozen 26×34 and
26×26 appearance and gain 48dp of reachable area around it.

**The consequence Phase B and C must design for.** Extended hit regions in a dense editor **overlap**,
and the frozen Bench places controls closer together than 48dp in several places — the swatch grid and
filmstrip most obviously. Overlap is resolved by hit-region priority and proximity, **not** by shrinking
a region back below the floor. Getting it wrong produces controls that steal each other's taps: a defect
a screenshot cannot show and a semantics test will not catch, which makes it precisely a device-pass
finding.

**Why it stays open.** `ZinelyV2Dimens.MinTouchTarget = 48.dp` states the floor; nothing meets it yet,
because no control exists. The entry closes when the affected surfaces are implemented **and** verified
on-device against the platform accessibility tree.

### D-010 — the page shadow is hard-coded to the light theme and does not adapt in the dark

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) line 85 → now line 98 · [`v2-proof.html`](mockups/v2-proof.html) line 98 → now line 112 (both moved by the amendment below) |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | Theme defect in the frozen specification — **does not block A4** |
| **Status** | ✅ **RESOLVED 2026-08-01** — owner ruling, applied as an [amendment to both frozen files](#d-010-amendment) |

**What it says.** Every other shadow in the Bench and the Proof takes its colour from
`var(--frame-shadow)`, which is re-derived for dark (`rgba(58,48,32,.28)` → `rgba(0,0,0,.5)`). Two do
not:

- `v2-bench.html:85` — `.page` — `0 14px 30px -14px rgba(58,48,32,.4), 0 2px 5px rgba(58,48,32,.14)`
- `v2-proof.html:98` — `.zpage` — `0 16px 34px -16px rgba(58,48,32,.44), 0 2px 5px rgba(58,48,32,.14)`

`rgb(58,48,32)` is the **light-theme** value of `--frame-shadow`, spelled out rather than referenced.

**Why it is wrong.** The dark-theme blocks in all three files change colour tokens only — so these two
rules keep a warm-brown shadow on a dark desk, while every neighbouring surface switches to black.
`.page` and `.zpage` are the *zine itself*: the single most important object on both screens. This is
also the one place the two-layer cast-plus-contact composition the Library expresses with two tokens
(`--shadow` + `--contact`) is reproduced by hand, which is plausibly how the tokens were lost.

Note this is a **theme** defect, not a value defect: the light-theme rendering is correct and the
prototypes are usually read in light, which is why it survived the freeze.

**Does implementation depend on it?** Not in A4 — no shadow values are transcribed yet; A4 ships only
the [`ZinelyV2ShadowLayer`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Shadow.kt)
primitive. It binds in **Phase C** and **Phase D**, when the page and the proof page are drawn. Flagged
now because a faithful transcription would carry the bug into Compose and it would then be invisible:
the Compose page would look right in light and subtly wrong in dark, which is the failure mode nobody
screenshots.

**Owner decision requested** (or simply a corpus fix): should both rules use `var(--frame-shadow)`,
and if the two-layer composition is wanted in dark as well, does the Bench/Proof palette need a
`--contact` equivalent the way the Library has one?

**✓ OWNER RULING — 2026-07-28: deferred to Phase C. A product-surface concern, not a foundation one.**

> *"Leave unchanged until Phase C. This is a **product-surface concern, not a foundation concern**. **Do
> not create alternative shadow behaviour during Phase A.**"*

The prohibition is the operative part. The tempting Phase A move would be to add a "correct" page-shadow
definition to the foundation — a `pageShadow` token, or a dark variant of `frameShadow` — so Phase C
inherits the fix for free. That is exactly *alternative shadow behaviour*, and it would put a second,
unfrozen source of truth beside the HTML for the most important object on two screens. A4 therefore
ships only the [`ZinelyV2ShadowLayer`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Shadow.kt)
primitive, with no page shadow defined anywhere.

Phase C draws the page and decides there, with this entry in hand — the point of logging it now being
that a faithful transcription carries the bug into Compose, where it becomes invisible: correct in
light, subtly wrong in dark, which is the failure mode nobody screenshots.

#### D-010 — owner ruling, 2026-08-01 {#d-010-ruling}

Phase C arrived and asked, exactly as the 2026-07-28 deferral intended. The question put to the owner was
[OD-3 of ADR-089](../DECISIONS.md#adr-089); the ruling was **approved as recommended**:

> *"Amend the frozen Bench and Proof specifications to introduce a dedicated page cast/contact shadow pair,
> preserving today's light rendering while allowing correct dark rendering. Treat this exactly like the D-024
> amendment: spec first, rationale recorded, implementation deferred. Do not implement Kotlin."*

**Why not simply `var(--frame-shadow)`, which is what this entry first proposed.** Because it would have
fixed the dark by **changing the light**. `--frame-shadow` is one token at `.28`; `.page` composes two
layers at `.4` (cast) and `.14` (contact). One alpha cannot carry both, so substituting the token would have
altered the most important object on two screens in the theme where the prototypes are actually read — a
visual redesign, arriving under a bug fix's cover, on a frozen surface. The entry's own second question
turned out to be the right one: **the Bench and Proof palettes did need a `--contact` equivalent**, the way
the Library already has one. That the fix mirrors the Library is not a coincidence — *"Why it is wrong"*
above guessed the cause correctly, that the two-layer composition was reproduced by hand and its tokens
lost in the copying. The Library's dark pair is `--shadow:rgba(0,0,0,.6)` / `--contact:rgba(0,0,0,.5)`
([`v2-library.html`](mockups/v2-library.html) `:33-34`), which is where the amended dark values come from.

**What is deferred, and to where.** No Kotlin. The Bench's page is drawn at **Phase C / C1**
([ADR-089](../DECISIONS.md#adr-089) row 1.6) and the Proof's at **Phase D**; each transcribes the amended
file then. C1's planned mutation is *collapse both layers onto one token* — the exact single-token
implementation this amendment exists to prevent, which renders *almost* right in light and would otherwise
pass unnoticed.

#### The amendment {#d-010-amendment}

Applied to both files on **2026-08-01**. Each of the four `:root` blocks per file gains the pair, and the
one hand-written rule per file now references it:

**[`v2-bench.html`](mockups/v2-bench.html)** — light (`:root` `:30`, `:root[data-theme="light"]` `:66`):

```css
--page-shadow:rgba(58,48,32,.4); --page-contact:rgba(58,48,32,.14);
```

dark (`@media (prefers-color-scheme:dark)` `:45`, `:root[data-theme="dark"]` `:56`):

```css
--page-shadow:rgba(0,0,0,.6); --page-contact:rgba(0,0,0,.5);
```

and `.page` at `:98`:

```css
box-shadow:0 14px 30px -14px var(--page-shadow),0 2px 5px var(--page-contact);
```

**[`v2-proof.html`](mockups/v2-proof.html)** — identical in structure, with its own cast alpha of `.44`
(tokens at `:31`/`:46`/`:57`/`:67`), and `.zpage` at `:112`:

```css
box-shadow:0 16px 34px -16px var(--page-shadow),0 2px 5px var(--page-contact);
```

**Three properties of this amendment are worth stating, because each is a thing a later reader will want to
check rather than trust:**

1. **Light is preserved byte-for-byte.** `.4` and `.14` in the Bench, `.44` and `.14` in the Proof — the
   same numbers that were there before, moved from the declaration into a token. A light-theme screenshot
   taken before and after the amendment is identical, which is the strongest available evidence that this
   is a theme fix and not a redesign.
2. **Dark is re-derived, not inverted.** `rgba(0,0,0,.6)` / `rgba(0,0,0,.5)` come from the Library's own
   dark pair, the corpus's existing answer for the same kind of object on a dark ground — not from
   arithmetic on the light values, which would have been an invention.
3. **No `box-shadow` declaration was added, removed or split, and no spread changed.**
   [`ZinelyV2DimensTest`](../../core/ui/src/test/kotlin/com/aritr/zinely/ui/theme/ZinelyV2DimensTest.kt)
   parses the trilogy's shadow declarations and asserts there are twenty-seven of them with no positive
   spread; the amendment leaves that count and every geometry untouched. It asserts *colour* nowhere, which
   is the gap that let this defect exist for four days after the freeze and is worth remembering the next
   time a test is described as covering shadows.

Both files carry the rationale in their own header comments, so the amendment is legible to someone who
opens the spec without opening this register. **C0 preserved that block** when it stripped D-001's two
stale lines, which sat directly above it — the Bench's block is intact at `:11-19`.

### D-011 — the Library declares neither easing token and animates on a curve found nowhere else

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 36, 43, 52, 61, 93, 119, 122 · [`v2-bench.html`](mockups/v2-bench.html) line 24 · [`v2-proof.html`](mockups/v2-proof.html) line 24 |
| **Found** | 2026-07-28, during Phase A / A5 (motion) |
| **Severity** | Cross-file divergence — did not block A5 |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**What they say.** The Bench and Proof declare two easing tokens and use them throughout:
`--settle:cubic-bezier(.05,.7,.1,1)` (nine uses) and `--standard:cubic-bezier(.2,0,0,1)` (eight).

The Library declares **neither**. Its **seven** transitions — five on product chrome, two on
prototype scaffolding — use four different things instead:

| Line | Selector | Easing |
|---|---|---|
| 122 | `.sheet` — the action sheet's slide | `cubic-bezier(.2,.8,.2,1)` — **a curve that appears nowhere else in V2** |
| 52 | `.zine` — the card press | `ease` |
| 61 | `.cover` — the shadow response | `ease` |
| 93 | `.start` — the primary button press | *unspecified* → the CSS default, `ease` |
| 119 | `.scrim` | *unspecified* → `ease` |
| 36, 43 | `body`, `.phone` — **prototype scaffolding**, listed so the inventory is complete | *unspecified* → `ease` |

**Why it is wrong.** This is the same staleness as **D-005** (the serif), from the same cause: the
Library was **authored** a day earlier (`1b2e244`, 2026-07-27) and **frozen at 09:08 on 2026-07-28**,
four and three-quarter hours before the Bench (`4494e95`, 13:52) — so it was written before the shared
token layer existed and the freeze captured it mid-evolution. `cubic-bezier(.2,.8,.2,1)` is not a third design intention — it is what `--standard`
was on its way to becoming. The bare `ease` keyword is the browser default rather than a choice at all,
and it is the one curve the V2 system explicitly moved away from: `ease` is symmetric and slightly
back-loaded, which is exactly the "generic UI" feel the settle/standard pair was chosen to avoid.

**Does implementation depend on it?** Not in A5 — only the two tokenised easings are ported, and the
Library's literals are not (porting `cubic-bezier(.2,.8,.2,1)` into the foundation would give a stale
value the same standing as a frozen token, the mistake **D-006** avoided with `--r`). It binds in
**Phase B**, which cannot animate the Library's sheet without choosing a curve.

**Owner decision requested.** Does the Library's action sheet use **`--settle`** (it is a surface
coming to rest, which is what settle is for, and it is what the Bench's and Proof's equivalent sheets
use), and its remaining transitions **`--standard`**? That is the reading that makes the trilogy one
system; the alternative is that the Library's curves are deliberate and the corpus has three easings,
in which case the third needs a name and a token. Separately — a documentation cleanup, as with D-005
— the Library's `:root` should gain `--settle`/`--standard` so a future reader is not told the product
has two motion systems.

**✅ RESOLUTION — owner ruling, 2026-07-28.**

> *"The Bench and Proof establish the **canonical V2 motion language**. The Library's unique easing
> reflects its **earlier freeze state** rather than a lasting design decision. When Phase B implements
> Library motion, use the **canonical V2 easing tokens** defined by Bench and Proof."*

The same shape as the **D-005** ruling, and for the same reason: where the Library and the later-frozen
pair disagree, the disagreement is chronology rather than intent. `cubic-bezier(.2,.8,.2,1)` was
`--standard` on its way to becoming itself, and the bare `ease` keywords were never a choice at all —
`ease` is the CSS default.

**What Phase B does.** Library motion uses `ZinelyV2Settle` and `ZinelyV2Standard`. Applying the
**paper-versus-chrome** axis that [ADR-075](../DECISIONS.md#adr-075) Decision 1 established from the
Bench and Proof — which is now the canonical language, and so is the thing to apply — that reads as:

| Library transition | Curve | Why |
|---|---|---|
| `.sheet` (`:122`) — the action sheet sliding up | **settle** | a surface coming to rest; the Bench's and Proof's equivalent sheets both use settle |
| `.cover` (`:61`) — the cover's shadow response | **settle** | the cover is the paper object itself |
| `.zine` (`:52`) — the card press | **settle** | the frozen Bench uses settle for `.pthumb`, the same gesture on the same kind of object |
| `.start` (`:93`) — the primary button press | **standard** | chrome mechanism |
| `.scrim` (`:119`) — the scrim fade | **standard** | pure opacity, and the Bench and Proof both use standard for their scrims |

*(That table is the reading Phase B should start from, not an amendment: the ruling settles the
**token set**, and the per-component assignment is transcription against the canonical axis. Any
component where the axis genuinely does not decide is a new register entry, not a local judgement.)*

**What implementation does now: nothing.** A5 deliberately did not port the Library's literals
([ADR-075](../DECISIONS.md#adr-075) Decision 4, on the **D-006** precedent), so no stale curve entered
the foundation and no code changes. Durations are unaffected — the ruling settles easings, and the
Library's `.16s`, `.24s`, `.4s` and the rest remain per-component values transcribed as frozen.

**Still owed to the design corpus** (documentation, not implementation): the Library's `:root` should
gain `--settle` and `--standard`, `:122` should reference `var(--settle)` instead of
`cubic-bezier(.2,.8,.2,1)`, and the bare/absent easings at `:52`, `:61`, `:93` and `:119` should name a
token. Until that lands, **this entry is the authority and the Library HTML is stale**.

### D-012 — the three frozen files write three different reduced-motion rules, and one of them would strobe

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) line 171 · [`v2-bench.html`](mockups/v2-bench.html) lines 123, 273 · [`v2-proof.html`](mockups/v2-proof.html) lines 259-261. *(Re-anchored 2026-08-01, as [D-008](#d-008--two-of-the-three-frozen-surfaces-specify-no-focus-appearance-and-one-removes-it)'s were.)* |
| **Found** | 2026-07-28, during Phase A / A5 (motion) |
| **Severity** | **Accessibility inconsistency in the frozen specification** — does not block A5 |
| **Status** | **Open by owner ruling** (2026-07-28) — deliberately unresolved in Phase A; the behavioural decision belongs to **Phase C**, on physical devices |

**What they say.** Every file honours `prefers-reduced-motion`, which is the good news and is worth
stating plainly — this is not a missing-accessibility defect like **D-008**. But no two files honour it
the same way:

| File | Rule |
|---|---|
| `v2-library.html:171` | `*{transition:none!important}` — kills transitions; says nothing about animations (the Library has none) |
| `v2-bench.html:273` | `*{transition-duration:.01ms!important; animation:none!important}` — collapses transitions, **disables animations outright** |
| `v2-proof.html:260` | `*{transition-duration:.01ms!important; animation-duration:.01ms!important}` — collapses both |

**Why it matters, given they currently agree.** For the three animations that exist today — the Bench's
one-shot `mat` materialise, the Proof's one-shot `seal`, and nothing in the Library — all three rules
produce an acceptable result, which is how the divergence survived the freeze. They are still not
interchangeable, and the difference is not stylistic:

The Bench contains the trilogy's only **looping** animation — the text caret's
`animation:blink 1.05s steps(1) infinite` (`:123`). Collapsing an *infinite* animation's duration to
`.01ms`, as the Proof's rule does, does not calm it: it makes it repeat at ten thousand hertz. The
Bench's `animation:none` is the correct form, and it is correct precisely because the Bench is the file
that has a loop. So the two files each wrote the rule that suited what they contained, and the Proof's
rule is safe **only** because the Proof has no looping animation — a property of today's content, not
of the rule.

Since `prefers-reduced-motion` is in part a **photosensitivity** setting, "the rule that happens to be
safe for the current content" is not a standard worth carrying into a codebase where a shimmer, a
pulse or a progress indicator may be added later.

**The fact that makes this a real decision, not a tidy-up.** The three files were frozen in this order:

| File | Freeze commit | Frozen at | Rule |
|---|---|---|---|
| `v2-library.html` | `43a3cc9` | 2026-07-28 **09:08** | `transition:none` |
| `v2-bench.html` | `4494e95` | 2026-07-28 **13:52** | `animation:none` |
| `v2-proof.html` | `caf431c` | 2026-07-28 **15:53** | `animation-duration:.01ms` |

So the corpus's **most recent** statement is the Proof's — the corpus moved *away* from
`animation:none`, not toward it. That is very likely an oversight in a file that has no looping
animation to worry about rather than a considered reversal, but it is exactly the fact an owner needs
in order to rule, and it is the reason this entry cannot claim the matter is merely technical.

**What implementation did, and it is a choice.** A5 implements the **Bench's** reading — the older of
the two live statements — and makes the distinction explicit rather than implicit:
[`ZinelyV2Motion`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Motion.kt) collapses
one-shot durations to zero (*"arrive instantly, still arrive"*) via `durationMillis`, and exposes
`allowsContinuousMotion`, which is **false** under reduced motion, so a looping animation is gated off
entirely rather than run at zero duration.

An earlier draft of this entry claimed *"this is not implementation choosing between two design
options"*. That was wrong and is withdrawn: three written rules that are not equivalent, one of them the
most recent, is precisely a choice. What justifies making it here is not that no choice exists but that
one option is a **safety floor**: running an `infiniteRepeatable` at zero duration in Compose is not a
slow animation, it is an unbounded frame-rate loop, and `prefers-reduced-motion` is in part a
photosensitivity setting. Refusing to ship that should not require a ruling. The choice is also **free
to reverse**: the API is additive and has no callers, so a ruling either way costs one line.

**✓ OWNER RULING — 2026-07-28: stays open. The behavioural decision belongs to Phase C.**

> *"Leave open. Do not resolve this during Phase A. The implementation API is intentionally flexible
> enough to support whichever policy ultimately governs. The final behavioural decision belongs to
> **Phase C**, where motion can be evaluated on **physical devices against the implemented product**
> rather than isolated prototypes. **Do not hard-code assumptions before then.**"*

The ruling is right that the API's *shape* is policy-neutral, and it is worth being exact about why,
since "flexible enough" is a claim implementation should be able to evidence rather than accept:

- **`durationMillis(frozen)`** takes the component's own value and returns it or zero. Every candidate
  policy is expressible by changing what it returns; no call site changes.
- **`allowsContinuousMotion`** is a **question a call site asks**, not a policy it states. A call site
  written as `if (motion.allowsContinuousMotion) { /* infiniteRepeatable */ }` is correct under all
  three rules — only the answer moves.

So what is currently Bench-flavoured is the **value**, not the shape, and Phase C changes the value in
one place. Recorded plainly because *"the API is flexible"* is exactly the kind of claim that is assumed
true and turns out not to be at the moment someone relies on it.

**The one thing Phase C must not inherit unexamined.** The distinction itself — one-shot collapses,
continuous stops — is **platform behaviour rather than design policy**, and the owner approved it on
that basis at the A5 gate. What remains genuinely open is the narrower question the three files
disagree on: whether a *transition* is cancelled mid-flight (the Library's `transition:none`) or
completes instantly (the Bench's and Proof's `.01ms`), and which rule the corpus should state. Only the
latter guarantees an element still reaches its end state, which is why implementation reads `.01ms` as
zero rather than as "do not run" — but that too is a Phase C observation to confirm on a device, not a
Phase A conclusion.

**Why a device is the right venue.** Reduced motion is the one part of the motion system whose
correctness cannot be read off a prototype: the failure modes are a control that never arrives at its
end state, and a repeating animation that strobes. Neither is visible in a browser at rest, and neither
appears in a screenshot.

**Owner decision requested** (a corpus cleanup, not a design question): should all three `:root` blocks
carry the Bench's rule — `transition-duration:.01ms; animation:none` — so the prototypes state one
policy? Note that the Library's `transition:none` and the Bench's `.01ms` also differ in a subtler way:
`transition:none` cancels a transition mid-flight, while a `.01ms` duration lets it complete instantly,
and only the latter guarantees the element still reaches its end state.
---

### D-013 — the Library and the Bench bake different alpha into the same grain, so a cover shows it four to nearly seven times stronger

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) line 18 · [`v2-bench.html`](mockups/v2-bench.html) line 35 · [`v2-proof.html`](mockups/v2-proof.html) line 36 |
| **Found** | 2026-07-28, during Phase A / A6 (paper / grain) |
| **Severity** | Cross-file divergence in a **material** — did not block A6 |
| **Status** | ✅ **RESOLVED** 2026-07-29 by owner ruling — see the resolution at the end of this entry |

**The noise itself is identical in all three files** — same filter, same parameters, same tile:

```
<feTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/>
<feColorMatrix type='saturate' values='0'/>
```

on 140×140. What differs is one attribute on the `<rect>` the filter is applied to. The Bench and Proof
write `opacity='.5'`; the Library writes none. That alpha is **baked into the data URI**, so it
multiplies with the element's CSS `opacity` rather than replacing it, and only the product is
observable:

| Surface | Tile | Baked | CSS | **Effective** |
|---|---|---|---|---|
| Library `.cover` | 140px | 1.0 | — | **1.00** |
| Library `.sheet-ill` | 90px | 1.0 | — | **1.00** |
| Library `.book-ill` | 70px | 1.0 | — | **1.00** |
| Bench / Proof `body::before` (the desk) | 180px | .5 | .5 | **0.25** |
| Bench `.page::after` | 120px | .5 | .45 | **0.225** |
| Proof `.zpage::after` | 120px | .5 | .42 | **0.21** |
| Proof `.drawer::after` | 150px | .5 | .3 | **0.15** |
| Bench / Proof `.phone::after` | 150px | .5 | .35 | 0.175 — *prototype bezel, not product UI* |

Ten grain-drawing rules, and every one of them blends `soft-light`. The gap is **4× against the
desk and 6.7× against the Proof's drawer** — an earlier draft of this entry said "four to five",
which understated its own table.

**Why this is a question rather than obviously a bug.** Two readings both survive the evidence, and
implementation cannot choose between them from the corpus alone:

1. **It is deliberate.** The Library's grain lands on *saturated cover inks*; the Bench's and Proof's
   lands on near-white paper. `soft-light` is dramatically subtler over mid-tone colour than over a
   near-white ground — the same alpha genuinely does not produce the same perceived texture. On this
   reading the Library is compensating, and 1.00 over a cover may look like 0.25 over paper.
2. **It is drift.** The Library froze first, before the `opacity='.5'` convention existed; the Bench
   and Proof then established it, and the Library's `--grain` was never revisited. This is the same
   shape as the **D-005** and **D-011** rulings, both of which resolved *"the Library reflects its
   earlier freeze state."*

Reading 2 has precedent on its side. Reading 1 has physics on its side. The difference matters: at
1.00, soft-light noise over a mid-tone cover is a visible tooth; at 0.25 it is a suggestion. Getting
this wrong makes covers either flat or dirty, and it is not the kind of thing a code review catches.

**Owner decision requested.** Does a Library cover draw grain at its frozen effective **1.00**, or at
the Bench/Proof register (≈0.2–0.25)? If the answer is 1.00, the Library's `--grain` should gain the
same `opacity='.5'` as the others and the CSS opacities should carry the difference explicitly, so the
corpus states one material with per-surface strengths rather than two materials that happen to share a
filter.

**What implementation does now: nothing.** [`ZinelyV2Grain`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Grain.kt)
ships one tile at full strength and takes `alpha` per call site, exactly as
[ADR-076](../DECISIONS.md#adr-076) records. Every value in the table above is expressible without a
code change, so this ruling costs one argument at one call site whenever it lands. **No strength is
tokenised and no average is taken** — under the **D-007** ruling, strength stays at the component.

**✅ RESOLUTION — owner ruling, 2026-07-29: reading 1. The divergence is deliberate.**

> *"The frozen HTML is authoritative. **Do not normalize grain strength across materials.** Paper and
> printed covers are intentionally different physical materials, and their grain should remain exactly
> as frozen."*

The first ruling in this register to break the pattern the Library's other divergences established —
and the reason is instructive. **D-005** (serif weight) and **D-011** (easing) were both resolved as
*chronology*: the Library froze first, so where it disagrees with the later pair it is stale. The
obvious move was to apply that a third time. The ruling declines to, on the grounds that this
divergence is not a token that failed to get updated but a **statement about two materials**. Paper and
a printed cover take ink differently; a value that is identical across them would be the drift, not the
difference.

So the table above is not a defect table. It is the specification, and Phase B transcribes it
literally: `1.00` on the three Library surfaces, `0.15`–`0.25` on the Bench's and Proof's. The 4×–6.7×
gap is the design.

**What implementation does: nothing, and that was already true.**
[`ZinelyV2Grain`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Grain.kt) ships one
tile at full strength and takes `alpha` per call site, so every row above is already expressible. No
code change, no asset change, and — under the **D-007** ruling — still no strength token.

**One consequence worth naming for Phase B.** Because the ruling makes the Library's `--grain`
correct rather than stale, the tripwire in `ZinelyV2GrainTest` (which fires if the Library's rect ever
gains `opacity='.5'`) is now guarding the *right* behaviour rather than waiting for a fix: if that
assertion ever breaks, it means someone has normalised the materials, which this ruling forbids. Its
message should be read that way. **The frozen corpus is owed no cleanup here** — unlike D-005 and
D-011, nothing in the HTML is stale.

---

### D-014 — the paper material cannot be drawn at all on API 24–28, and the design has no reading for those devices

| | |
|---|---|
| **Artifacts** | Platform constraint against the whole frozen trilogy — every `--grain` rule (`v2-library.html` 59/105/110 · `v2-bench.html` 74/87/99 · `v2-proof.html` 75/95/115/173) |
| **Found** | 2026-07-29, during Phase A / A6 review |
| **Severity** | **Platform capability gap**, not a specification contradiction — did not block A6 |
| **Status** | ✅ **RESOLVED** 2026-07-29 by owner ruling — the safe floor is the permanent behaviour; see the end of this entry |

**What the platform does.** `android.graphics.BlendMode` is **API 29**. Below it Compose composites
through `PorterDuffXfermode`, whose mode table contains no soft-light, so `BlendMode.Softlight` falls
through to the default — **`SRC_OVER`**. The failure is not a subtler grain; it is the noise tile
painted *opaquely* over the surface. At the Library cover's effective strength of **1.00** (see
**D-013**) that is a flat grey rectangle where the artwork should be.

This project's `minSdk` is **24**, so API 24–28 is inside the supported range.

**Why this is a defect entry and not just an implementation note.** The frozen design describes one
material and assumes it always renders. It has no second reading — no fallback texture, no "flat
paper" variant, no statement about which surfaces matter enough to degrade differently. That absence
is exactly the kind of thing the register exists to name rather than let implementation fill in
silently, which is how a design decision gets made by a `when` branch.

**What implementation does now — the safe floor.** On API 24–28,
[`Modifier.zinelyV2Grain`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Grain.kt)
draws **nothing** and the surface stays flat. Losing the paper texture is a smaller and more honest
failure than replacing the artwork with grey, and unlike the grey it cannot be mistaken for a design
choice. The same shape as the **D-012** call: implement the option that cannot ship something
actively wrong, disclose it as a choice, and leave it free to reverse — here it is one branch with no
callers yet.

**Owner decision requested.** On API 24–28, should V2 paper surfaces (a) render flat, as now;
(b) render a static warm tint approximating the grain's average effect, which is authoring a second
material; or (c) be treated as out of scope, raising `minSdk` to 29? Note that (c) is not only a
grain question — it would also retire the `RuntimeShader` and `fontVariationSettings` ceilings that
shaped [ADR-073](../DECISIONS.md#adr-073) and [ADR-076](../DECISIONS.md#adr-076), so it is a product
decision with a much wider blast radius than this entry.

**This is verifiable, and was verified rather than assumed.** `AndroidPaint_androidKt.setNativeBlendMode`
branches on `SDK_INT >= 29`, and `AndroidBlendMode_androidKt.toPorterDuffMode` has no `SOFT_LIGHT`
case and returns `SRC_OVER` by default — read from the decompiled `ui-graphics` 1.10.4 artifact, not
from documentation.

**✅ RESOLUTION — owner ruling, 2026-07-29: option (a). Flat paper is correct, not a fallback.**

> *"The current implementation is approved. **Do not emulate Soft Light using another blend mode. Do
> not invent an approximation.** For API 24–28, rendering flat paper is the correct constitutional
> behaviour because it preserves **material honesty** rather than introducing an incorrect simulation."*

Implementation proposed this as a *safe floor* — the least-bad option, held provisionally until an
owner chose. The ruling accepts the behaviour and rejects the framing: it is not the least-bad option,
it is the right one. Options (b) (a static warm tint approximating the grain) and (c) (raise `minSdk`
to 29) are both refused, and (b) is refused on principle rather than on cost — an approximation of a
material is a **second material**, and V2 has one. A device that cannot draw the paper shows paper it
cannot draw rather than something that merely resembles it.

That reasoning generalises past this entry, which is why it is worth stating plainly: where the
platform cannot express the frozen design, **implementation omits rather than approximates**, and says
so. Nothing here is a fallback texture in waiting.

**What implementation does: nothing further.** `Modifier.zinelyV2Grain` is already a no-op below API
29, gated on Compose's own `BlendMode.isSupported()` — see
[ADR-076](../DECISIONS.md#adr-076) Decision 9. The gate stays; what changes is that its KDoc no longer
describes itself as provisional, and no future package should treat it as an open question.

**Not a limitation to hide.** API 24–28 users see V2's paper without its grain. That is a real,
documented difference and belongs in release notes when V2 ships — as a **Known Limitation**, in the
[CLAUDE.md](../../CLAUDE.md#release-categories--never-conflate) sense, not as a bug and not as silence.

---

### D-015 — two concepts are each drawn twice, with different geometry, and one pair is inside a single file

| | |
|---|---|
| **Artifacts** | [`v2-proof.html`](mockups/v2-proof.html) lines 310 and 390 · [`v2-bench.html`](mockups/v2-bench.html) line 359 against [`v2-proof.html`](mockups/v2-proof.html) line 308 |
| **Found** | 2026-07-29, during Phase A / A7 (icons) |
| **Severity** | Cross-file (and intra-file) inconsistency — did not block A7 |
| **Status** | ✅ **RESOLVED** 2026-07-29 by owner ruling — see the resolution at the end of this entry |

The trilogy contains 36 distinct icon marks across 42 placements. Two of those 36 are duplicates in
meaning but not in drawing.

**1. Chevron-right, twice, in the same file.**

| Where | Path | Used for |
|---|---|---|
| `v2-proof.html:376` | `M9 5l7 7-7 7` | the fold navigator's *next* button |
| `v2-proof.html:296` | `M9 6l6 6-6 6` | the READY band's affordance chevron |

They are not the same shape: the first spans 7 units and starts at y=5, the second spans 6 and starts
at y=6. The tell is the **left** chevron at `:276`, `M15 5l-7 7 7 7` — an exact mirror of the first and
not of the second. So `M9 6l6 6-6 6` is the one that does not belong to the pair, and its being the
only chevron on the resting band is either a deliberately smaller mark or a transcription that drifted.

**2. A check, twice, across two files.**

| Where | Path | Used for |
|---|---|---|
| `v2-proof.html:294` | `M4 12l5 5 11-12` | the READY tick, and three more placements (`:311` seal, `:344`, `:485`) |
| `v2-bench.html:359` | `M20 6 9 17l-5-5` | the Bench's *Done* button |

Same mark, drawn from opposite ends, with different proportions. The Proof's is used four times and is
the better-established of the two.

**Why this is worth a ruling rather than a quiet fix.** Collapsing either pair is a **redesign** — it
changes what a frozen surface renders — and choosing *which* geometry survives is a design judgement
implementation has no standing to make. Both are therefore transcribed exactly as found, as
[`ChevronRight`/`ChevronRightBand`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Icons.kt)
and `Tick`/`Done`. The cost of leaving it is small and precise: two extra entries in the set, and two
places where Phase B must pick the right one rather than the obvious one.

**Owner decision requested.** For each pair: are these one icon or two? If one, which geometry is
canonical — and the frozen file that loses should be amended, since the register does not permit
implementation to amend it. Note the chevron pair is the sharper case, because a single file drawing
the same affordance two ways is harder to read as intent than two files that froze at different times.

**Explicitly *not* raised here, having been checked:** the Library's `StampStar` (`:153`) and the
Bench's `Favourite` (`:599`) are also two stars with different geometry, but they are different things
— one is stroked cover *artwork*, the other a filled UI mark rendered in ochre at 12px. Likewise
`Shield` (`bench:461`) and `ShieldCheck` (`bench:605`) share an outline by design, the second adding a
tick. Neither is a defect, and both are noted so a later reader does not re-open them.

**✅ RESOLUTION — owner ruling, 2026-07-29: both pairs stand. Similarity is not identity.**

> *"**Do not deduplicate. Do not canonicalize. Do not select a preferred version.** The frozen HTML is
> authoritative. Treat each geometry as an **independent design asset** unless the design corpus
> explicitly declares otherwise. **Similarity is not sufficient evidence of identity.** If, in the
> future, the design intentionally converges those assets, that change belongs in the design corpus
> first — not in implementation."*

The ruling reframes the entry rather than answering the question it asked. This register was built to
raise things that *look* wrong so an owner can decide; the finding here was framed as "two marks that
ought to be one", and the ruling rejects the premise. Two geometries are two assets. The burden of proof
runs the other way: implementation may treat two marks as one only where the corpus **says** they are
one, and nothing in the corpus says it.

That is a stricter rule than it first sounds, and it is worth stating plainly because the pull in the
other direction is constant. A chevron that is nearly the mirror of another chevron is *nearly* — and
"nearly" is where an implementer's judgement starts substituting for a designer's. `ChevronRightBand`
sits on the resting READY band and `ChevronRight` on the fold navigator; that they differ by a unit of
span may be intent, drift, or neither, and implementation cannot tell the difference from the outside.
Keeping both costs two entries in a set. Merging them wrongly costs a surface that no longer matches
its specification, silently.

**What implementation does: nothing — which was already the case.** Both pairs were transcribed as
found in [A7](../DECISIONS.md#adr-077), as `ChevronRight`/`ChevronRightBand` and `Tick`/`Done`. No code
change, no asset change.

**What changes is the tripwire's meaning.** `ZinelyV2IconsTest` asserts that the two chevron geometries
remain distinct and that only `ChevronRight` mirrors `ChevronLeft`. Under this ruling that is no longer
a defect marker waiting to be cleared — it guards the ruling. If it fails, either the corpus has
converged the assets (a design act, which is legitimate and should be accompanied by a corpus change)
or implementation has quietly merged them (which this ruling forbids).

**No corpus cleanup is owed**, unlike D-005 and D-011. Nothing in the frozen HTML is stale here.

---

### D-016 — two of Phase A's acceptance criteria cannot be met by a phase forbidden to touch product surface

| | |
|---|---|
| **Artifact** | [`docs/COMPOSE-V2-ROADMAP.md`](../COMPOSE-V2-ROADMAP.md), Phase A "Acceptance criteria" |
| **Found** | 2026-07-29, during Phase A / A10 (documentation verification), and sharpened by its independent review |
| **Status** | ✅ **RESOLVED** 2026-07-30 by owner ruling — see the resolution at the end of this entry. Phase A's gate **passes** |
| **Depends on it** | [ADR-080](../DECISIONS.md#adr-080), whose Decision 1 this ruling settles — the ADR is now `Accepted` |

**What the artifact says.** Phase A's acceptance criteria include:

> *"No hard-coded colours, sizes, or fonts anywhere — everything routes through tokens."*
> *"Foundation is confirmed to be the **same** migration as the conformance token work (no duplicate system)."*

and its review gate requires *"Independent review confirms: exact token fidelity, **no parallel/duplicate
design system**, a11y infra present, zero product surface."*

**Why this is a defect in the plan, not in the implementation.** The same phase's Objective is *"nothing
product-facing"*, its first acceptance criterion is *"No product screen exists yet"*, and its gate demands
*"zero product surface"*. Routing an existing screen onto tokens means **editing that screen**; retiring a
duplicate token system means **deleting the old one out of the components that use it**. Both are edits to
V1 product code. The criteria are therefore not merely difficult inside Phase A — they are mutually
exclusive with Phase A's own definition, and no ordering of the work resolves it.

The repository state is unambiguous: [`config/token-enrolment.txt`](../../config/token-enrolment.txt) enrols
**zero** packages, and `ZinelyColors`/`ZinelyV2Colors`, `ZinelyDimens`/`ZinelyV2Dimens` and
`ZinelyTypography`/`ZinelyV2Typography` coexist.

**What Phase A did instead**, under a standing owner instruction repeated at every package gate — *additive
only · preserve V1* — was land V2 beside V1 without modifying a single V1 `src/main` file. That is a
coherent strategy, and it is the one nine consecutive owner approvals endorsed. It was never written down
as an interpretation of these criteria, which is the actual defect being logged.

**The question for the owner.** Two readings, and the implementation session declines to choose:

1. **Both criteria re-seat to Phase D's exit**, where the last surface is re-skinned and the last consumer
   migrated. Phase A's gate passes on its other conditions.
2. **Only criterion 4 re-seats.** Criterion 5's verb is *"confirmed to be"* — it asks for a recorded
   confirmation that V2 *is* the migration vehicle and V1 retires through it, not for completed
   convergence. On that reading [ADR-080](../DECISIONS.md#adr-080) Decision 2 already satisfies it, and it
   should be marked **met**.

Either way the convergence mechanism is the same and already exists: each of Phases B, C and D enrols its
package in `token-enrolment.txt` **in the same commit that migrates it**, which is the coupling the
enrolment file's own header already mandates — so convergence is continuously gated rather than deferred
to one migration at the end.

**Why this was not settled in-session.** [COMPOSE-IMPLEMENTATION-RULES.md](../COMPOSE-IMPLEMENTATION-RULES.md)
says to *"stop and raise it with the owner"* and log it here; [V2-CONSTITUTION.md §VI](V2-CONSTITUTION.md)
reserves amendment to the owner, *"never implicitly through implementation, and never by a design or
engineering session on its own initiative."* A first draft of ADR-080 re-seated the criteria on its own
authority and cited the Constitution for a stop-rule the Constitution does not contain. The review caught
the misattribution; this entry is the correction.

**✅ RESOLUTION — owner ruling, 2026-07-30. The second reading.**

> *"ADR-080 is **Accepted**. Only the token-routing clause re-seats to Phase D. The 'confirmed to be the
> same migration as the conformance token work (no duplicate system)' criterion is **satisfied by
> confirmation of the migration architecture and strategy**. The requirement that existing product surfaces
> route through V2 tokens **necessarily belongs to Phase D** because it requires modifying those product
> surfaces. **Phase A therefore passes its gate.**"*

**What this settles, clause by clause.**

| Phase A criterion | Disposition |
|---|---|
| *"No hard-coded colours, sizes, or fonts anywhere — everything routes through tokens"* | **Re-seated to Phase D's acceptance criteria**, where the last surface is re-skinned and the last consumer migrated. Written into [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) Phase D at closeout. |
| *"Foundation is confirmed to be the **same** migration… (no duplicate system)"* | **Met**, by [ADR-080](../DECISIONS.md#adr-080) Decision 2 — the recorded confirmation of the migration architecture and strategy. |
| Review gate — *"no parallel/duplicate design system"* | **Passed** on that reading: the coexistence of V1 and V2 token objects is *scheduled convergence* through a single migration, not a parallel system. |

**Consequences applied at closeout.** ADR-080 → `Accepted`; the completion record's two ❌ rows become
⏭️ *re-seated* and ✅ *met by confirmation*; Phase A's gate is recorded **passed**; Phase D gains the
re-seated clause; **Phase A is CLOSED**. Phase B remains unstarted and begins only on an explicit owner GO.

---

### D-017 — the frozen Library shows six covers and states no rule for giving a cover to a seventh zine

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) lines 149–154; [V2-IDENTITY.md §5](V2-IDENTITY.md) |
| **Found** | 2026-07-30, during Phase B / B1 (implementing the Maker's Cover) |
| **Severity** | Design gap — **does not block implementation**; B1 shipped a disclosed interpretation that one pure function replaced |
| **Status** | ✅ **RESOLVED 2026-07-30 by owner ruling** — assign at creation and persist; **do not** derive from the title. Ruling and consequences at the [foot of this entry](#d-017-ruling). |

**What it says.** The frozen shelf hard-authors six covers: `class="cover ink-matcha"` … `paper-s`, one
`.band` and one `.stamp` per cover, six titles. Every constant a cover needs is frozen — the four cover inks
and two paper stocks (lines 79–84), the band's geometry (line 67), the stamp's rotation and size, the title's
type. What is *not* anywhere in the trilogy is the sentence that decides **which** of those six surfaces a
newly-created zine prints on, or which of the six stamps it carries.

**Why that is a gap and not a detail.** A prototype's shelf is authored content; a product's shelf is user
data. The moment the Library draws a real zine list, something must map a zine to a surface and a stamp, and
the frozen corpus supplies no rule — so an implementation cannot transcribe this. It has to decide.

[V2-IDENTITY.md §5](V2-IDENTITY.md) is the nearest thing to an answer and stops one step short of one: it
names the model (*"a frozen grid × swappable ingredients"*, *"Freeze the grid; vary the ingredients"*) and the
goal (*"per-object individuality"*, so a row reads as a collection), but not **what varies them**. It is also a
design *proposal* document, not part of the frozen trilogy, so it could not settle this even if it did.

**What B1 decided, and disclosed rather than buried.** The cover is derived from the zine's title, by the
character-sum hash the **V1** shelf prototype states — [`docs/design/v1/shelf.html:527`](v1/shelf.html),
`h=(h+c.charCodeAt(0))|0`, and the same shape V1's Kotlin `shelfCoverHash` uses over four archetypes — fed
into two independent axes: `surface = h % 6`, `stamp = (h / 6) % 6`. (The V2 Library prototype has **no** hash
at all; its six covers are authored classes. So even the derivation's *shape* comes from V1, which is the
clearest single statement of this gap.) Deriving from the title
is what [ADR-069](../DECISIONS.md#adr-069) already records for V1's shelf and what keeps a shelf reproducible
with **no new persisted state**. Three properties follow, all asserted in `ZineCoverRecipeTest` so a future
reader meets them as known behaviour:

1. **A rename reprints the cover.** The look is a function of the title; change the title, get a new look.
2. **An anagram prints the same cover.** A character sum cannot distinguish `"Tiny poems"` from `"poems Tiny"`.
   (One deliberate divergence from the JS, shared with V1's Kotlin hash: `for (c in title)` sums both UTF-16
   units of an astral character where JS's `c.charCodeAt(0)` sums only the leading surrogate. It changes which
   cover an emoji-titled zine prints, nothing else, and is asserted rather than left to be discovered.)
3. **There is no distinctness guarantee.** The frozen file's own six titles land on **three** surfaces, because
   two pairs collide. So the Compose Library will not reproduce the frozen screenshot's six-way variety for
   those exact six titles — the *grammar* matches the freeze; the particular sheet does not.

**The alternatives, each with its own cost.**

| Rule | What it buys | What it costs |
|---|---|---|
| **Title hash** (what B1 ships) | no persisted state; deterministic; a shelf is reproducible from titles alone | renames reprint; no distinctness guarantee |
| **Persist the recipe at creation** | a cover survives a rename; distinctness can be enforced at insert | a data-model change (Room metadata / the [ADR-003](../DECISIONS.md#adr-003) document tree) and a migration |
| **Assign by insertion order** (round-robin) | the first six zines are guaranteed six different covers, exactly as frozen | a zine's own look depends on its **neighbours** — delete one and others reprint |
| **Let the maker choose** | no ambiguity at all | a feature that is not in the frozen design; belongs to the roadmap, not to Phase B |

**Owner question (answered below).** Accept the title-hash derivation as B1 first shipped it (and with it:
renames reprint, and one shelf may show a repeated surface), or rule for one of the other three?

### Owner ruling — 2026-07-30 {#d-017-ruling}

> *"Do not derive the cover surface from the title. Assign the cover surface once when the zine is created and
> persist that assignment. A physical object should retain its identity across renames. Do not use round-robin
> assignment. Do not infer from neighbouring zines. The persisted assignment becomes part of the zine's
> identity."*

**What changed in B1.** `zineCoverHash(title)` and `zineCoverRecipe(title)` are **deleted**. A second draft
briefly shipped `newZineCoverRecipe(random: Random)` here — drawing each axis independently, guarded by a
reflection test scanning for any function mapping a `String` to a cover. Independent review of B1 found the
guard could not hold the ruling regardless of how it was written: it checked for an exact `String` parameter
type, so a title-derived **seed** at a call site (`newZineCoverRecipe(Random(title.hashCode()))`) satisfies
every version of it while the title still reaches the cover. That is not a fixable gap in one test; a
signature check cannot decide an information-flow property, which is what "must the title never reach the
cover, by any path" actually is. (The guard itself went through five wrong versions before this was found —
each fix closing one bypass while opening or leaving another; the history is kept in this session's
implementer/reviewer transcript rather than restated here.)

**So B1 ships no assigner at all.** [ZineCoverSurface] and [ZineCoverStamp] vary independently — the frozen
grid × swappable ingredients [V2-IDENTITY.md](V2-IDENTITY.md) §5 describes — but nothing in B1 draws one from
the other or from anything else, because B1 has no caller to assign a cover *to*: it renders a given
`ZineCoverRecipe`, it does not decide one. The assigner — and a guard that can finally see the whole path
worth checking — land together in **Phase B / B5**, next to the persisted surface+stamp field the ruling
requires ([ADR-042](../DECISIONS.md#adr-042)'s project index / `meta.json` sidecar): an assigner with nowhere
to store its result "assigns" a cover that evaporates on the next recomposition, which is a different bug
than a title leaking in, but a bug all the same, and B5 is where there is finally an actual call site whose
one input can be checked directly instead of enumerated against.

**What the ruling's four clauses become, once assignment lands in B5:**

| Clause | Where it will be held |
|---|---|
| not from the title | the assigner's only parameter is the source of randomness, never the title — checked directly at its one call site, not by scanning the package |
| retained across renames | the persisted field is written once, at creation; a rename touches a different field entirely |
| no round-robin, no neighbours | assignment reads neither the shelf nor an index, so adding or deleting a zine cannot change another zine's cover |
| assigned once | the assigner is called from the create path only, with **no** default/no-argument overload that composition could reach for |

**One accepted consequence, to be recorded again at B5:** independent draws mean **two zines on one shelf may
print the same surface**, where the frozen file's six examples are all different. Guaranteeing distinctness
requires reading the shelf, which the ruling excludes twice over — so a repeat is the ruled behaviour.

**What is still owed, and by whom.** The field holding a zine's assigned surface and stamp belongs to the
project index and its `meta.json` sidecar ([ADR-042](../DECISIONS.md#adr-042)) and lands with the data wiring
in **Phase B / B5**, which is a **hard prerequisite** for routing the V2 Library rather than an optional
extra: a shelf that assigns without persisting would reprint every cover on every launch.

**And one thing this supersedes.** [ADR-069](../DECISIONS.md#adr-069) describes the shelf as drawing *"a
title-hashed riso cover … and keeps doing so"*. That mechanism is superseded **for V2 covers**; ADR-069's
load-bearing rule — a cover is a *recipe*, never a rendered page thumbnail — is untouched, and is why
assignment returns a recipe rather than anything raster. V1's shelf keeps its own title hash until C0.

---

### D-018 — the cover's ink band specifies `multiply`, which Android cannot honour below API 29

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) line 67 — `.band{…opacity:.9;mix-blend-mode:multiply}` |
| **Found** | 2026-07-30, during Phase B / B1 (pixel-verifying the band at two API levels) |
| **Severity** | Platform ceiling — the cover still draws; **the band's colour was wrong on API 24–28** |
| **Status** | ✅ **RESOLVED 2026-07-30 by owner ruling** — follow D-014: **omit the band**, do not emulate `multiply` and do not substitute another blend mode. Ruling at the [foot of this entry](#d-018-ruling). |

**What the platform does.** `BlendMode.Multiply` — like `BlendMode.Softlight` before it (D-014) — is
unsupported by the hardware-accelerated canvas below **API 29**, and it fails *silently*: the draw succeeds
and composites `SrcOver`. Verified at both levels in `ZineCoverRenderTest`, which reads the band's mean red
channel: on API 29 the band multiplies (matcha `#4E5A26` over `#7C8A3F` → a distinctly darker band); on
API 28 the same call lays the band's own colour at 90 % opacity instead.

**Why this one is not simply D-014 again.** D-014's ruling is that where the platform cannot express the
design, the implementation **omits and discloses** — flat paper is *correct*, not a fallback. Applied
literally here, the band would not be drawn at all on API 24–28. But the band is a printed cover's **only
printed mark** besides the stamp: omit it and a matcha cover becomes a flat matcha rectangle, which is a
larger deviation from the frozen design than a band of the wrong darkness. So the two available readings
disagree, and neither is obviously the owner's:

| Reading | On API 24–28 | Argument |
|---|---|---|
| **Draw it `SrcOver`** (what B1 first shipped) | band present, lighter/flatter than frozen | the cover keeps its composition; the deviation is a shade, not a missing element |
| **Omit the band** (D-014's precedent) | flat stock, no band | *"fail honestly rather than approximate"* — a `SrcOver` band **is** an approximation of a `multiply` band |

**What B1 first shipped, stated plainly.** The band was drawn on every API level, compositing `SrcOver` below
29. That is an approximation, which is the side of [COMPOSE-IMPLEMENTATION-RULES.md](../COMPOSE-IMPLEMENTATION-RULES.md)'s
*"platform limitations must fail honestly rather than approximating the design"* that this register exists to
flag rather than to hide — and it is what the ruling below reversed.

**Owner question (answered below).** On API 24–28: draw the band `SrcOver` (accept a lighter band), or omit it
(accept a flat stock)?

**Not in scope of the question:** raising `minSdk`. D-014 already ruled that out for the same platform gap.

### Owner ruling — 2026-07-30 {#d-018-ruling}

> *"Follow the precedent established by D-014. Do not emulate Multiply. Do not substitute another blend mode.
> If the platform cannot express the frozen design, omit the multiplied band rather than approximating it.
> Record this interpretation."*

**What changed in B1.** `drawInkBand` now returns early unless `BlendMode.Multiply.isSupported()` — the same
predicate style as [ZinelyV2Grain.IsSupported](#d-014--the-paper-material-cannot-be-drawn-at-all-on-api-2428-and-the-design-has-no-reading-for-those-devices),
asked of Compose rather than of `Build.VERSION` so the guard cannot drift from the compositing path it guards.
On API 24–28 a cover is therefore **stock, crease, fore-edge, stamp and title, with no printed band**.

**The interpretation, recorded as the ruling asks.** Two of the frozen cover's marks are now absent on those
devices — the grain (D-014) and the band (D-018) — and both absences have the same cause: one platform ceiling
at API 29, reached by two different blend modes. They are **one Known Limitation**, not two, and belong in the
release notes as one sentence: *on Android 9 and older, printed covers show their stock, crease and stamp
without the paper grain or the ink band.*

**The tests moved with the ruling, in both directions.** `below API 29 the band is omitted rather than
approximated` reads the same pixels the old assertion read and demands the opposite answer — every row the band
would have covered must be *exactly* the stock, so a band of any kind fails it. And the band's geometry test
(`the band spans exactly the frozen thirty-three to forty-seven percent`) moved to **sdk 29**, since that is now
the only API level where the band exists at all; with the grain live it finds the band's edges from row means
instead of single pixels, which is a stronger check than the one it replaced.

---

### D-019 — the frozen trilogy has no right-to-left reading, and a printed cover has a physical handedness

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) lines 57–68 (`border-radius`, `::before{left:9px}`, `::after{right:0}`); [`app/src/main/AndroidManifest.xml:19`](../../app/src/main/AndroidManifest.xml) — `android:supportsRtl="true"` |
| **Found** | 2026-07-30, during Phase B / B1 (implementer self-review, before the gate) |
| **Severity** | Localisation gap — the cover was internally *inconsistent* (logical corners, physical marks); that defect is fixed |
| **Status** | ✅ **RESOLVED 2026-07-30 by owner ruling** — the printed artifact does **not** mirror, in any locale. Ruling at the [foot of this entry](#d-019-ruling). |

**What the freeze says.** The cover's handedness is stated three times and always **physically**: the corner radii
are `6px 9px 9px 6px` (a tight bound edge on the left, wider cut edges on the right), the scored fold is at
`left:9px`, and the fore-edge shadow is at `right:0`. Nothing in the trilogy mentions direction, mirroring or
locale — a browser prototype simply had no reason to.

**Why it surfaced in code.** Compose's `RoundedCornerShape` takes **logical** corners that mirror under RTL, while
a `Brush` drawn at an absolute offset does not. The first B1 implementation mixed the two, so an RTL device would
have shown the bound edge's tight radius on the **right** while the crease stayed on the **left** — a cover creased
down its own cut edge. That composes cleanly and no LTR screenshot shows it, which is why the check now exists
(`ZineCoverRenderTest.a mirrored layout does not mirror the printed object`, verified to fail on the logical shape).

**What B1 does now, and what is still unruled.** The whole printed object is transcribed **physically** and
nothing mirrors — the literal reading of the frozen CSS — while the title continues to follow the layout
direction, exactly as it does in CSS. That makes the object self-consistent. It does not answer the design
question: **should an RTL shelf show right-bound books?** A real bookshelf in an RTL culture is bound on the
right, so a fully mirrored cover is a defensible localisation; it is also a *design* change to a frozen artifact,
which is not an implementer's call.

**Owner question (answered below).** Leave the printed object physical in every locale, or specify a mirrored
cover for RTL — in which case the frozen HTML gains an RTL reading first, per
[COMPOSE-IMPLEMENTATION-RULES.md](../COMPOSE-IMPLEMENTATION-RULES.md) ("if the HTML is wrong, fix the HTML first").
The same question will return for the shelf grid (B2) and the Bench (Phase C), so ruling it once is cheaper.

### Owner ruling — 2026-07-30 {#d-019-ruling}

> *"The printed object is physical. Do not mirror the printed cover based on locale. The physical binding edge,
> fore-edge and crease remain canonical exactly as frozen. Future UI chrome may adapt to RTL, but the printed
> artifact itself does not."*

**Confirms what B1 ships**, and settles the general rule rather than one component: the boundary is **chrome
versus artifact**, not Library versus Bench. Chrome may mirror; a printed object never does — which extends
the Constitution's own split (*the interface stays quiet; the creations carry the warmth*) into layout
direction, and answers the same question in advance for the shelf grid (B2), the Bench's page sheets (Phase C)
and the Proof's imposed sheet (Phase D).

`CoverShape` is `AbsoluteRoundedCornerShape` for exactly this reason, and the guard is
`ZineCoverRenderTest.a mirrored layout does not mirror the printed object` — verified to fail on the logical
shape, so it is not one more assertion that cannot fail. The **title** still follows the layout direction, as
it does in CSS: the text is content, not part of the printed object's geometry.

---

### D-020 — the shelf states a fixed two-column grid with no breakpoint, and Phase B verifies on foldables

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) line 46 — `.shelf{…grid-template-columns:1fr 1fr;gap:28px 20px…}` |
| **Found** | 2026-07-30, during Phase B / B2 (implementing the shelf) |
| **Severity** | Design gap — **did not block B2**, which transcribed the freeze literally |
| **Status** | ✅ **RESOLVED 2026-07-30 by owner ruling** — two columns, no breakpoint, no responsive behaviour, no maximum cover width, and **none of them to be invented**. Ruling at the [foot of this entry](#d-020-ruling). |

**What it says.** `grid-template-columns:1fr 1fr`, and **no `@media` query anywhere in the file** — the only
`@media` rules in `v2-library.html` are `prefers-color-scheme` (`:20`) and `prefers-reduced-motion` (`:138`).
The frozen mockup is a `392px` phone (`.phone{width:392px}`, `:41`), so two columns is what the design was
authored at and the only width it was ever read at.

**Why that is a gap rather than a detail.** Two columns is not a proportion, it is a count, and a count that is
right at 392dp is not automatically right at 1200dp. At a foldable's unfolded width a two-column shelf draws
two covers roughly 580dp across — a "small printed object" rendered nearly half a metre tall in the hand's
frame of reference, which is a different object than the design describes. And this is not hypothetical: Phase
B's own device verification list names **foldables** explicitly
([COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md)), so the question gets asked by the gate whether or not it is
answered before it.

**The evidence that the count was never considered, rather than considered and fixed.** V1's shelf — the same
product, the same screen — is **responsive**: `shelfColumns(width)` in `ShelfCard.kt` returns 2 · 3 · 4 · 5 at
560 / 820 / 1180dp. So the product already knows this question exists and already has an answer for it. The V2
freeze does not contradict that answer; it simply never states one, because it was authored as a single phone
screen. That is the clearest single reading of the gap: V2 is not *choosing* a fixed grid over V1's responsive
one, it is silent where V1 speaks.

**What B2 did, and did not do.** It transcribed `1fr 1fr` as a fixed two columns and **raised this entry**. It
did not port V1's breakpoints, average the two, or invent a threshold — the frozen design is unambiguous at the
width it was drawn for, so there is no ambiguity for an implementation to resolve, only a range the design does
not cover. Inventing a breakpoint would be inventing design, which is exactly what the B1 rulings established
must not happen quietly.

**The alternatives, each with its own cost.**

| Rule | What it buys | What it costs |
|---|---|---|
| **Fixed two columns** (what B2 ships) | literal parity with the freeze at every width; nothing invented | covers grow without limit on tablets and unfolded foldables |
| **Port V1's breakpoints** (2 · 3 · 4 · 5) | one consistent product behaviour; already shipped and understood | a V2 screen taking its layout rule from a V1 file the freeze does not reference |
| **Cap the cover's width, keep two columns** | the object stays object-sized; the two-column *composition* survives | introduces a maximum the frozen file never states, and leaves the row's spare width to be designed |
| **Declare V2 phone-only for now** | honest, and matches what was actually designed | the device-verification list already promises foldables |

**Owner decision requested (answered below).** Does the V2 shelf stay a fixed two columns at every width, or
does it adapt — and if it adapts, by V1's existing thresholds or by a rule stated for V2? A related and
separable question: if covers may grow, is there a maximum cover width, given the whole design premise is
*a small printed object*?

### Owner ruling — 2026-07-30 {#d-020-ruling}

> *"The frozen design defines a two-column shelf. No breakpoint exists. No responsive behaviour exists. No
> maximum cover width exists. Do not invent any of them. Future adaptive layouts require a future frozen
> design rather than implementation inference."*

**What changes in B2: nothing.** `ShelfColumns = 2` and the absence of any width branch were already the
literal transcription, so the ruling confirms the package as built rather than correcting it. This is the
first entry in the register to resolve that way, and the reason it could is that B2 raised the gap *instead of*
closing it — had it ported V1's breakpoints and disclosed them, the ruling would have been a rework.

**The load-bearing half of the ruling is the last sentence**, and it reaches much further than this shelf.
*"Future adaptive layouts require a future frozen design rather than implementation inference"* states the
direction of authority for every gap of this shape: where the frozen corpus is **silent** rather than
contradictory, silence is not an invitation to interpolate. An implementation may not derive a design rule
from a neighbouring width, a neighbouring screen, or V1's answer to the same question — which is what makes
this a general precedent and not a one-line fact about columns.

Three consequences worth naming, because each is a decision a later package might otherwise re-open:

1. **V1's `shelfColumns` (2 · 3 · 4 · 5) does not transfer.** It answers this question for V1's shelf and
   stops there. That V1 already has an answer is evidence the question is real, not evidence of what V2's
   answer should be.
2. **A tablet or unfolded foldable shows two large covers, and that is the specified behaviour** — not a
   defect for the device passes to file. It should be *recorded* by those passes rather than fixed by them;
   if it reads badly on real hardware, that observation is a request for a **new frozen design**, which is a
   design-track act, not an implementation one.
3. **No maximum cover width.** The ruling closes the separable sub-question this entry raised, so a cover's
   size follows its column and nothing clamps it.

### D-021 — the sheet's icons are Unicode characters, and half of them are not in the app's own font

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) lines 173–177 (`.ic` spans) and line 72 (the `⋯` on `.more`) |
| **Found** | 2026-07-30, during Phase B / B3 (implementing the sheet and the cover's two gestures) |
| **Severity** | Design gap — **did not block B3**, which transcribed the freeze literally |
| **Status** | ✅ **RESOLVED 2026-07-30 by owner ruling** — keep the literal characters exactly as frozen; do not substitute icons, do not redesign the marks, and **bundled-font coverage does not justify changing the design**. Platform fallback is acceptable. Ruling at the [foot of this entry](#d-021-ruling). |

**What it says.** Each action row's icon is a styled `<span class="ic">` holding a **literal character**:
`↗` (Open), `⇪` (Share & export), `✎` (Rename), `⧉` (Duplicate), `⌫` (Delete) — and the shelf's overflow
control is a sixth, `⋯`. None is an SVG, a path, or a named asset; the design specifies text.

**The measurement, not the assumption.** The bundled font files were parsed directly — every `cmap` subtable
of the app's Inter, including the format-12 table that carries its higher planes:

| Glyph | Role | In bundled Inter? |
|---|---|---|
| `↗` U+2197 | Open on the bench | ✅ yes |
| `⇪` U+21EA | Share & export | ✅ yes |
| `⌫` U+232B | Delete | ✅ yes |
| `✎` U+270E | Rename | ❌ **no** |
| `⧉` U+29C9 | Duplicate | ❌ **no** |
| `⋯` U+22EF | the shelf's `.more` | ❌ **no** |

So **three of the six frozen marks are drawn by whatever font the device falls back to**, and their weight,
width and optical size therefore vary by manufacturer and OS version. On a device with no fallback covering
them, the user sees a tofu box. That is not a hypothetical class of defect for this design in particular:
these six marks are the only iconography in the Library, and the `⋯` is the sole discoverable path to the
sheet for a user who never tries a long press.

**Why substituting is not available to an implementation.** A7 shipped a V2 icon set of thirty-six marks as
geometry ([ADR-079](../DECISIONS.md#adr-079)), which is where a mark of this kind would normally come from.
It has **no mark for *open* and none for *duplicate***. Choosing replacements — or drawing two new marks —
would be authoring iconography, which is a design act, and the B1 rulings established that an implementation
does not perform those quietly.

**What B3 did, and did not do.** It transcribed all six as text, exactly as frozen, and pinned the risk with
a test that renders each glyph beside **two tofu controls** and compares them pixel for pixel. Ink coverage
alone cannot do this — a tofu box paints *more* ink than a thin ellipsis does, so a coverage test would have
called tofu a successful render.

> **The first version of that test could not fail, and independent review proved it rather than argued it.**
> It used a single `U+E000` control on the reasoning that a Private Use Area codepoint is one no font
> carries. **The bundled Inter maps `U+E000` to a real glyph** (id 1863, all four weights), so the "tofu"
> control was an ordinary character and the comparison passed for any two glyphs that merely differed. The
> reviewer demonstrated it by rendering a genuinely uncovered codepoint through the passing test. The fix
> uses **two** codepoints verified absent from every bundled weight and asserts they render *identically*
> first: distinct codepoints can only look the same because neither has a glyph, so that shared raster **is**
> the tofu box, measured instead of assumed. Recorded here because this entry's rendering claim is the thing
> the owner is being asked to rule on, and it briefly rested on nothing.
On the test platform all six draw real marks; that is **one platform**, and it is the honest limit of what a
unit test can say here.

**The alternatives, each with its own cost.**

| Rule | What it buys | What it costs |
|---|---|---|
| **Keep the literal characters** (what B3 ships) | literal parity; nothing invented | three of six marks look different on every device, and can be tofu |
| **Extend the A7 icon set with *open* and *duplicate*, then use geometry throughout** | one controlled appearance everywhere; no fallback risk | authors two new design assets and departs from what the file specifies |
| **Bundle a font subset covering all six** | the frozen characters, drawn identically everywhere | ships a second text family for six glyphs |
| **Keep characters, accept a documented Known Limitation** | no work; honest | the limitation is *visual inconsistency*, which is harder to accept than D-014's flat paper |

**Owner decision requested (answered below).** Are the six marks **characters** (as frozen, with
device-dependent shape and a tofu risk), or are they **artwork** the design controls — and if artwork, does
A7's set get the two marks it lacks, or does a bundled subset carry the frozen codepoints? This is the same
question shape as
[**D-018**](#d-018--the-covers-ink-band-specifies-multiply-which-android-cannot-honour-below-api-29) — a
frozen visual the platform may not be able to honour — but its ruling does not answer it: omitting a glyph
leaves a row with no icon, which is a different act than omitting a decorative band.

### Owner ruling — 2026-07-30 {#d-021-ruling}

> *"Keep the literal characters exactly as defined by the frozen HTML. Do not substitute icons. Do not
> redesign the marks. Bundled-font coverage does not justify changing the design. Platform fallback is
> acceptable. Future design revisions may replace the glyphs explicitly if desired."*

**What changes in B3: nothing.** The six marks were already transcribed as text, so — like **D-020** before
it — this entry resolves by confirming the package as built. It could only resolve that way because B3 raised
the gap *instead of* closing it; had it substituted A7 marks and disclosed the substitution, the ruling would
have been a rework.

**The load-bearing sentence is the third**, and it is a general precedent about evidence rather than about
glyphs: *"bundled-font coverage does not justify changing the design."* A measurement about the
**implementation's own resources** is not, by itself, an argument about the **design**. B3 measured something
real — three of six marks fall through to the platform — and the ruling accepts the measurement while
rejecting the inference. Where the design specifies a character, the character is the specification; the
font's coverage is an implementation fact that may inform a *future design revision*, and only a design act
can change what is drawn.

Three consequences worth naming:

1. **`✎`, `⧉` and `⋯` will look different across devices, and that is the specified behaviour** — to be
   *recorded* by the B5 device passes rather than fixed by them, exactly as **D-020**'s two large covers are.
2. **A7's icon set is not extended for this.** Its lack of an *open* or *duplicate* mark is no longer a gap to
   close, because nothing is asking it to supply one.
3. **The tofu risk is accepted, not eliminated.** It stays pinned by
   `ZineActionSheetTest.every frozen glyph draws a real mark, not a tofu box`, which now measures its own
   control rather than assuming it (see the note above) — so if a future platform *does* draw tofu on the
   test host, the suite says so instead of the design quietly degrading.

### D-022 — the Library's scrim is a theme-invariant literal, while the corpus publishes a theme-aware one

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) line 119 — `.scrim{background:rgba(30,25,18,.36)}` — against the corpus's `--scrim` |
| **Found** | 2026-07-30, during Phase B / B3 (implementing the action sheet) |
| **Severity** | Design gap — **did not block B3**, which transcribed the freeze literally |
| **Status** | ✅ **RESOLVED 2026-07-30 by owner ruling** — **the corpus is authoritative**; implement the published light and dark scrim values rather than the theme-invariant literal. **Code changed** — the only place B3 does not transcribe the Library file. Ruling at the [foot of this entry](#d-022-ruling). |

**What it says.** The scrim's fill is written as a hard `rgba()` literal **outside the file's own `:root`**,
so the `@media (prefers-color-scheme: dark)` block at `:20` — which redefines every other colour the Library
uses — cannot reach it. The dark Library therefore dims with the same warm 36 % wash as the light one.

**Why that is a gap rather than a detail.** A scrim's job is to push a surface back; how much wash it takes
to do that depends entirely on what is behind it. Over the light desk, 36 % of a warm near-black reads as a
clear separation. Over the dark desk — which is already close to that colour — the same wash removes far less
contrast, so the sheet sits over a shelf that is still competing with it. The literal is not merely
untokenised; it produces a *different amount of dimming* in the two themes while claiming to be one value.

**The evidence this is staleness rather than intent.** The corpus already publishes a canonical, theme-aware
`--scrim`: `rgba(42,37,30,.34)` light and `rgba(0,0,0,.5)` dark
([`v2-bench.html:31,46`](mockups/v2-bench.html)) — note that the dark half is **stronger**, which is exactly
the correction the Library's literal is missing. Stronger still: the two other frozen files **already
disagreed** about this token (the Proof declares `.42`/`.55`) and that disagreement was **already ruled** —
Bench-canonical, per Q1, recorded in `ZinelyV2Colors.kt:123-129`. So V2's scrim has been adjudicated once
already, and the Library simply was not in the room, because its literal is not a token and nothing compared
it. This is the same shape as
[**D-005**](#d-005--the-library-and-the-bench-set-the-same-role-in-two-different-serifs-at-two-different-weights)
and [**D-011**](#d-011--the-library-declares-neither-easing-token-and-animates-on-a-curve-found-nowhere-else):
the Library was frozen earlier than the corpus it now sits beside, and carries pre-token values the rest of
V2 has since replaced. Both of those were ruled **the corpus wins**. This one has never been put to the
owner, which is the only reason it is open.

**What B3 did, and did not do.** It transcribed `rgba(30,25,18,.36)` and raised this entry — it did not
quietly adopt `ZinelyV2Colors.scrim` on the strength of the D-005/D-011 precedents. Two rulings pointing the
same way are a strong hint, not a ruling, and a scrim is a visible surface rather than an internal token. The
transcription is pinned by a test that samples the **production** scrim in both themes and asserts they are
identical, naming D-022 in its own failure message so the ruling has something to flip.

> The first version of that test painted its own copy of the literal, and a mutation that switched production
> to the theme-aware token left it green. It was rewritten to compose the production scrim. Recording that
> here because it is the register's own risk: an entry is only as honest as the test that pins it.

**The alternatives, each with its own cost.**

| Rule | What it buys | What it costs |
|---|---|---|
| **Keep the frozen literal** (what B3 ships) | literal parity; nothing invented | the dark Library is under-dimmed, and V2 has two scrims |
| **Adopt the corpus `--scrim`** (the D-005/D-011 answer) | one scrim across V2; the dark theme dims properly | the Library's rendered appearance changes from its frozen file |
| **Amend the frozen HTML to use the token, then implement** | the file and the code agree again | a design-track edit to a frozen artifact |

**Owner decision requested (answered below).** Does the Library's scrim stay the frozen literal, or does the
canonical theme-aware `--scrim` outrank it as the serif and the easings did? If the corpus wins, does the
frozen HTML get corrected in the same act (as **D-006** required) or does the register carry the divergence?

### Owner ruling — 2026-07-30 {#d-022-ruling}

> *"The frozen corpus defines a theme-aware scrim. The corpus is authoritative. Implement the published light
> and dark scrim values rather than a theme-invariant literal."*

**What changes in B3: one paint site, and it is the first of its kind in this programme.** `ZineActionScrim`
now takes `ZinelyV2Colors.scrim` — `rgba(42,37,30,.34)` light, `rgba(0,0,0,.5)` dark — and the Library's
`rgba(30,25,18,.36)` is **not transcribed**. Every other value in B1, B2 and B3 comes from the frozen Library
file; this is the single exception, and it is an exception **by ruling rather than by inference**, which is
the distinction the register exists to preserve.

**Why this is the third of a set, and the set is now a rule.** **D-005** (the serif), **D-011** (the easings)
and now D-022 (the scrim) are the same defect with three faces: *the Library was frozen before the corpus it
now sits beside, and carries pre-token values the rest of V2 has since replaced.* All three were ruled the
same way. The general form is worth stating because a fourth will appear: **where the Library file contradicts
a token the corpus publishes, the corpus wins** — the Library's value is evidence of *when* it was authored,
not of what was intended.

Two consequences, and one question the ruling deliberately leaves alone:

1. **The dark Library now dims properly.** The published dark scrim is the *stronger* wash (`.50` against
   `.34`), which is precisely the correction a theme-invariant literal could not express — the frozen literal
   under-dimmed a desk already close to its own colour.
2. **The rasters change.** `v2_sheet_dark.png` was re-recorded and the difference is plainly visible; that
   raster is now evidence of the ruling rather than of the freeze.
3. **The frozen HTML is *not* amended.** Unlike **D-006**, which had the dead `--r:18px` token deleted from
   the frozen files, the ruling does not ask for `v2-library.html:119` to be corrected — so the file and the
   code now legitimately disagree, and **this entry is the record of why**. A future reader diffing the two
   should land here rather than filing it as drift.

---

### D-023 — the Library labels its primary button `--paper`, while the corpus publishes `--on-matcha` for exactly that job {#d-023}

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) line 91 — `.start{background:var(--matcha);color:var(--paper)}` — against the corpus's `--on-matcha` |
| **Found** | 2026-07-31, during Phase B / B4 (implementing the dock), by **independent review** |
| **Severity** | Design gap — **does not block B4**, which transcribes the freeze literally |
| **Status** | ⏳ **OPEN — awaiting an owner ruling.** The fourth member of the D-005 / D-011 / D-022 set, which [D-022's ruling](#d-022-ruling) predicted in terms. |

**What it says.** `.start` is the Library's one primary action: a `--matcha` fill with a `--paper` label. Every
matcha fill in the **Bench** and the **Proof** takes `var(--on-matcha)` instead — a token the corpus declares
in both themes (`#FFFFFF` light, `#20240E` dark) and marks **★ AA-critical on `matcha`**
(`ZinelyV2Colors.kt:93`). The Library declares no `--on-matcha` at all, because it was frozen before the
shared token layer existed.

**Why this is the shape D-022 named.** That ruling stated the general form and said a fourth would appear:
*"where the Library file contradicts a token the corpus publishes, the corpus wins — the Library's value is
evidence of when it was authored, not of what was intended."* D-005 (the serif), D-011 (the easings) and
D-022 (the scrim) are the same defect with three faces, and all three were ruled the same way.

**Why it is nonetheless not obvious.** Unlike the other three, the Library's value here is **not broken**.
`--paper` is a real token, declared in both themes, and it inverts correctly with them. Measured on the
rendered pair rather than argued: **5.20:1** in light (`#F7F2E7` on `#5E6B2F`) and **5.12:1** in dark
(`#2F2A22` on `#93A257`), against `--on-matcha`'s own 5.80 and 5.72. Both clear AA in both themes. A cream
label is also the reading the rest of that screen supports, where nothing else is pure white — so the
question is genuinely *which of two working values is intended*, not whether one of them fails.

> **B4 first tried to settle this itself**, on the test *"a divergence earns a register entry when the
> Library's version cannot work, not whenever the corpus differs"*. Independent review rejected it, and the
> rejection is the useful part: that test does not describe the three rulings it claims to distinguish,
> because **D-005's font stack rendered fine and D-011's `ease` is a valid curve** — neither was broken, and
> both were ruled stale on *authorship date*, which is precisely the argument that applies here. B3, holding
> two rulings pointing one way, wrote that *"two rulings pointing the same way are a strong hint, not a
> ruling"* and raised D-022 rather than deciding. B4 held three rulings and a stated general rule. **The
> register's own precedent is to ask.**

**What B4 ships meanwhile.** The frozen `--paper`, transcribed, pinned by
`ZineDockTest.the button is matcha and its label is paper` in both themes — which asserts that no pixel of
the button is `--on-matcha`, and names this entry so the ruling has something to flip. Same shape B3 gave
the scrim.

**The alternatives.**

| Rule | What it buys | What it costs |
|---|---|---|
| **Keep `--paper`** (what B4 ships) | literal parity; nothing invented; a warmer label consistent with the screen | V2 has two answers for text on matcha, and the Library's is the one not marked AA-critical |
| **Adopt `--on-matcha`** (the D-005/D-011/D-022 answer) | one rule across V2 for the highest-traffic colour pair; slightly higher contrast | the Library's rendered appearance departs from its frozen file; one paint site, one assertion and two rasters change |
| **Amend the frozen HTML, then implement** | file and code agree again | a design-track edit to a frozen artifact |

**Owner decision requested.** Does `.start`'s label stay the frozen `--paper`, or does `--on-matcha` outrank
it as the serif, the easings and the scrim did? And if the corpus wins, is the frozen HTML corrected in the
same act (as **D-006** required) or does the register carry the divergence (as **D-022** chose)?

---

### D-024 — the frozen Library specifies a two-state screen; the real shelf has four states {#d-024}

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) line 117 — `body.is-empty .shelf{display:none} body.is-empty .empty{display:flex}` — the file's **only** state switch |
| **Found** | 2026-07-31, during Phase B / B5 planning (the frozen property table), before any production code |
| **Severity** | **BLOCKS B5.** B5's entire job is showing real project data, and two of the four states it will actually meet have no frozen appearance at all |
| **Status** | ✅ **RESOLVED 2026-07-31.** Ruled ([below](#d-024-ruling)), amendment **approved and applied** to `v2-library.html` — `.ph` / `body.is-loading`, `.fail` / `.retry` / `body.is-error`, the dock standing in all four states, and two new prototype toggles. **No longer blocks B5.** |

**What it says.** The frozen file has exactly two screen states: covers, or the transformation empty state.
It is a design prototype with six hard-coded zines, so it never reads a store, never waits, and never fails.

**What the store actually has.** `HomeUiState` (`app/src/main/java/com/aritr/zinely/home/HomeViewModel.kt:39`)
is `Loading | Empty | Error | Content`, and V1's shelf renders all four — a loading skeleton
(`HomeLoadingTestTag`), an error state with a retry that re-asks the store (`ShelfErrorState`), the empty
invitation, and the cards. **Loading and Error have no V2 design.**

**Why B5 cannot decide this itself.** Two constitutional rules pull in opposite directions and neither
resolves it:

- **Never-silent failure** ([ADR-051](../DECISIONS.md#adr-051)) forbids simply dropping the error state. A
  shelf that renders as *empty* when the store could not be read tells the user their zines are **gone**.
  That is the [ADR-058](../DECISIONS.md#adr-058) "it lost my work" failure exactly, and the Library's own
  question — *"which zine do I want?"* — is answered with a lie.
- **No silent approximation** and **D-020's** ruling (*"future adaptive layouts require a future frozen
  design rather than implementation inference"*) forbid inventing the appearance. Where the corpus is
  **silent**, silence is not an invitation to interpolate — not from V1's answer to the same question, which
  is the nearest and most tempting source.

**What B5 will not do.** Re-skin V1's loading skeleton and error state in V2 tokens. That is a visual design
act on a surface the freeze does not contain, and it is exactly the inference D-020 forbade.

**The alternatives.**

| Rule | What it buys | What it costs |
|---|---|---|
| **Amend the frozen HTML** to add a loading and an error state | the corpus regains a single source of visual truth; parity stays checkable | a design-track edit to a frozen artifact, and it is real design work, not a correction |
| **Rule that the two states are chrome, not design**, and specify them in prose (e.g. *"loading shows nothing; error shows the empty state's copy replaced by an honest failure line and a retry"*) | B5 proceeds without a design cycle | prose is a weaker spec than the HTML, and pixel parity has nothing to compare against |
| **Defer both to a later package** and have B5 hold the last-known shelf while loading | smallest B5 | the error path stays unbuilt, which is a never-silent-failure violation the moment the store fails |

**Owner decision requested.** What does the V2 Library show **while the store is being read**, and what does
it show **when the read fails**? And is the answer expressed as an amendment to `v2-library.html` (so parity
is verifiable) or as a written ruling B5 implements from prose?

#### D-024 — owner ruling, 2026-07-31 {#d-024-ruling}

> **Loading and Error are product states. They belong in the canonical design. Do not invent Compose behaviour.
> Do not resolve them through prose alone. Instead: prepare an amendment to the frozen HTML that adds canonical
> Loading and Error states for the Library. The HTML remains the design authority.**

The ruling takes the first alternative and rejects the other two in one move — it declines both *"implement from
prose"* and *"defer the error path"*. It is also the first amendment to a frozen V2 surface that **adds design**
rather than deleting dead specification (**D-006** removed `--r:18px`; this one draws two states that never
existed), so the amendment is authored as a proposal and lands only on approval.

**Consequence for B5: still blocked, but now on an artifact rather than on a question.** Parity is verified
against the frozen HTML; until the amendment is approved there is nothing to verify against, and building first
and back-filling the HTML would be the exact inversion the workflow forbids (*"if the HTML is wrong, fix the
HTML first — never the reverse"*).

#### The amendment {#d-024-amendment}

**Status: ✅ APPROVED AND APPLIED, 2026-07-31.** `v2-library.html` now carries it, under an `AMENDED` block in
the file's own freeze header. Everything frozen on 2026-07-27 is unchanged — **the amendment only adds.** It is
the first amendment to a V2 surface that *adds* design; **D-006** deleted dead specification (`--r:18px`), and
this one draws two states that never existed.

**Approved with two further rulings, both now in the file:**

1. **The dock stands in all four states — content, empty, loading, error.** *"The dock is part of the workspace
   rather than the loaded content."* It therefore does not wait for a read to succeed and never appears late.
   This required no CSS: `.dock` already sits outside `.empty` and no state rule targets it, so the amendment
   records the reason in a comment rather than adding a rule. **Do not introduce a second workspace grammar.**
2. **The loading debounce is implementation behaviour, not design, and is deliberately NOT in the HTML.** A
   threshold before the placeholders appear (so a fast read does not flash them) is a real requirement, but it
   is a timing seam, and encoding it in the canonical design would make the HTML the authority on something it
   cannot express or verify. It is recorded as an **implementation seam** in
   [ADR-086](../DECISIONS.md#adr-086) instead, which is where B5 owns it.

**Design reasoning, stated so it can be argued with rather than inferred from the CSS:**

1. **Loading is the desk with unprinted covers, not a spinner.** A spinner is app chrome, and the constitution
   puts warmth in the artifact and quiet in the chrome. Placeholders at the cover's own aspect ratio and radius
   read physically — *sheets not yet printed* — and they keep the shelf's geometry stable, so covers do not
   jump into place. They carry **no grain, no shadow, no stamp, no title**: an unprinted sheet has none of those.
2. **The shelf heading stays up during loading.** It is already frozen (`.shelf-head`), it costs no read, and
   holding it still means the screen does not visibly restructure when the data lands.
3. **Loading must not look like the empty state, even for one frame.** This is the load-bearing one. If a slow
   read renders the "Make your first little zine" invitation, a user with twelve zines is told they have none —
   the [ADR-058](../DECISIONS.md#adr-058) *"it lost my work"* failure, in the screen whose one question is
   *"which zine is mine?"*. The placeholders exist mainly to make that impossible.
4. **The error state borrows the empty state's structure and inverts its message.** Same centred column, same
   serif line, same body measure — because it is the same *kind* of moment (the shelf has nothing to show) and
   a second layout grammar would be a second design. What changes is the copy and the addition of a retry.
5. **The error copy reassures before it apologises.** *"Your zines are still on your phone"* is the first line
   after the headline, because the honest fact is that a failed read is not a loss, and the user's fear is that
   it is. This is never-silent-failure ([ADR-051](../DECISIONS.md#adr-051)) applied to reading rather than
   writing.
6. **Retry is a quiet control, not a second primary.** `.start` is the screen's one primary action and it stands
   in every state; a second matcha-filled button would compete with it. `.retry` therefore takes the paper +
   hairline grammar the sheet's rows and the prototype's own `.ctl` already use, at a real touch size.
7. **`--consequence` is not used.** The palette reserves it for *delete/error*, but a failed read destroys
   nothing. Colouring it as a consequence would say the opposite of point 5.

```css
  /* ── loading: the desk with unprinted covers.  Never the empty state — a slow read must not
     tell a user with twelve zines that they have none. ─────────────────────────────────────── */
  .ph{aspect-ratio:3/4;border-radius:6px 9px 9px 6px;background:var(--desk-edge)}
  body.is-loading .shelf .zine{display:none}
  body.is-loading .empty{display:none}

  /* ── error: the empty state's column, its message inverted.  Reassure, then explain. ─────── */
  .fail{position:absolute;inset:0;display:none;flex-direction:column;align-items:center;
    justify-content:center;text-align:center;padding:36px 40px 140px;gap:16px}
  .fail h2{font-size:1.72rem;margin:8px 0 0;font-weight:600;letter-spacing:-.01em}
  .fail p{margin:0;color:var(--ink-soft);max-width:28ch;line-height:1.55;font-size:.95rem}
  .retry{margin-top:6px;background:var(--paper);color:var(--ink);border:1px solid var(--hair);
    border-radius:12px;font-family:inherit;font-size:.95rem;font-weight:600;padding:13px 22px;cursor:pointer}
  .retry:focus-visible{outline:2px solid var(--matcha-text);outline-offset:3px}
  body.is-error .shelf{display:none} body.is-error .empty{display:none} body.is-error .fail{display:flex}
```

```html
  <!-- inside .shelf, after .shelf-head — shown only while loading -->
  <div class="ph-row"><div class="ph"></div><div class="ph"></div><div class="ph"></div><div class="ph"></div></div>

  <!-- sibling of .empty -->
  <div class="fail">
    <h2 class="serif">Your shelf didn’t open.</h2>
    <p>Your zines are still on your phone — something went wrong reading the shelf.</p>
    <button class="retry">Try again</button>
  </div>
```

*(The four `.ph` blocks are laid out by the existing `.shelf` grid, so they need no rule of their own beyond the
placeholder fill; the `ph-row` wrapper above is illustrative and would be dropped in favour of four bare `.ph`
children of `.shelf`. The prototype's `#em` toggle pattern extends to `#ld` / `#er` buttons so both new states
are demonstrable in the file, exactly as the empty state already is.)*

**Both open questions in the draft were answered on approval** and are recorded above: the dock stands in all
four states, and the debounce stays out of the design.

---

### D-025 — the sheet's five actions and the dock's CTA have no destinations, and three of them need UI the freeze does not contain {#d-025}

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) lines 168 (`.start`) and 173–177 (the five `.act` rows) — the script at `:186-210` wires **only** the scrim and `.more` |
| **Found** | 2026-07-31, during Phase B / B5 planning, before any production code |
| **Severity** | **BLOCKS B5.** Route hand-over is B5's named deliverable, and the freeze names no destination for any of the seven actions |
| **Status** | ✅ **RESOLVED 2026-07-31 by owner ruling — [see below](#d-025-ruling). Reuse the existing flows; invent no new product concept.** No longer blocks B5. |

**Why this is a fresh entry and not a re-raise.** [ADR-083](../DECISIONS.md#adr-083) and
[ADR-084](../DECISIONS.md#adr-084) both deferred these handlers to B5 as *"route hand-over"*, on the correct
reading that the frozen file wires nothing and **nothing is not "goes nowhere on purpose"**. That deferral
assumed the destinations existed and merely needed connecting. Planning B5 against repository truth shows
that is true of **three** of the seven and false of the other four.

| Frozen action | Destination in the repository | Verdict |
|---|---|---|
| tap a cover (`:199` *"tap = open zine"*) | `HomeViewModel.openZine(id)` → `EditorRoute` | ✅ exists, connect it |
| **Open on the bench** (`:173`) | the same route | ✅ exists, connect it |
| **Duplicate** (`:176`) | `HomeViewModel.duplicate(id)` → `duplicateProject` ([ADR-022](../DECISIONS.md#adr-022): same content hashes, new id). Needs no UI | ✅ exists, connect it — **but see [D-026](#d-026)**, since the copy is a new zine and D-017 makes a cover part of a zine's identity |
| **Rename** (`:175`) | `HomeViewModel.rename(id, title)` exists, but a rename needs a **text input**, and the freeze contains none | ⚠ UI not in the freeze |
| **Delete** (`:177`) | `delete` / `undoDelete` / `commitDelete` exist — V1 deletes **undoably behind a snackbar** ([ADR-046](../DECISIONS.md#adr-046) §4). The frozen sheet shows a `.danger` row and **no confirmation and no snackbar anywhere in the file** | ⚠ UI not in the freeze |
| **Make a zine** (`:168`) | `startZine(paperSize)` requires a paper size; V1 raises a **paper chooser** (`HomePaperChooserTestTag`, ADR-047). The frozen Library contains no chooser | ⚠ UI not in the freeze |
| **Share & export** (`:174`) | **no shelf-level equivalent exists at all.** Export lives behind the Proof surface (`EditorRoute` → `ProofRoute`, [ADR-051](../DECISIONS.md#adr-051)/[ADR-052](../DECISIONS.md#adr-052)) | ⚠ destination undefined |

**Why B5 cannot decide these itself.** Each of the four is a **visible surface**, not a wiring detail. Reusing
V1's rename dialog, delete snackbar and paper chooser inside a V2 screen puts V1 chrome on a V2 surface — and
the paper chooser in particular is a full sheet the user will meet immediately after pressing the Library's one
primary button. Designing V2 versions of them is a redesign during implementation, which the freeze forbids.
"Share & export" is worse than unstyled: it is **undefined behaviour**. Exporting from the shelf without
opening the zine would be a genuinely new capability, and routing it through the Proof means the row's label
promises something its destination does not do.

**What is not in doubt.** The three ✅ rows are unambiguous and B5 will wire them. This entry blocks the other
four, not the screen's structure.

**Owner decision requested.** For each of Rename, Delete, Make a zine and Share & export: does B5 (a) reuse
V1's existing surface unchanged, accepting V1 chrome inside a V2 screen until C0 converges it; (b) wait for
those surfaces to be added to the frozen corpus; or (c) something else per action? And specifically for
**Share & export** — is it a route into the existing Proof surface, or a shelf-level export that does not
exist yet?

#### D-025 — owner ruling, 2026-07-31 {#d-025-ruling}

> **Reuse existing behaviour. Do not invent new product concepts. Specifically: Rename → existing rename flow ·
> Delete → existing delete flow · Make a zine → existing creation flow · Share & export → route into the
> existing Proof flow. Do not introduce a separate shelf-level export concept.**

Option (a) throughout, and the last sentence closes the one genuinely open question: **there is no shelf-level
export.** The sheet row is a *route*, not a capability.

**What B5 wires, precisely:**

| Frozen action | Destination |
|---|---|
| tap a cover · **Open on the bench** | `HomeViewModel.openZine(id)` → `EditorRoute` |
| **Duplicate** | `HomeViewModel.duplicate(id)` → `duplicateProject` — with a **new** cover, per [D-026's ruling](#d-026-ruling) |
| **Rename** | the existing rename flow (`HomeViewModel.rename`) and its existing input surface |
| **Delete** | the existing delete flow — which is **undoable behind a snackbar** ([ADR-046](../DECISIONS.md#adr-046) §4), including its commit-on-leave semantics. "Reuse the existing flow" means the undo comes with it; a V2 shelf that deleted immediately would be a *new* concept, not a reused one |
| **Make a zine** | the existing creation flow, i.e. the paper chooser ([ADR-047](../DECISIONS.md#adr-047)) → `startZine(paperSize)` |
| **Share & export** | `EditorRoute` **then** `ProofRoute` — see the constraint below |

**The one integration constraint this creates.** `ProofRoute` resolves the *shared* editor ViewModel by fetching
the editor's live back-stack entry (`navController.getBackStackEntry(EditorRoute(projectId))`, the
[ADR-026](../DECISIONS.md#adr-026) single-writer seam). So "Share & export" **cannot** navigate straight to the
Proof: it must push the editor and then the Proof, leaving the editor on the stack underneath. That is not a
detail — a direct `navigate(ProofRoute)` would throw at runtime, and back from the Proof correctly lands on the
bench rather than the shelf, which is the existing flow's own behaviour and therefore what "reuse" means.

**One honest consequence, recorded rather than smoothed over.** Reusing V1's rename input, delete snackbar and
paper chooser puts **V1 chrome inside a V2 screen** from the moment the V2 Library takes the route. That is the
ruling's accepted cost and it is the [ADR-080](../DECISIONS.md#adr-080) migration architecture working as
designed — V2 lands surface by surface, and these three surfaces are not Phase B's. It ships as a **Known
Limitation** until the phase that re-skins them, and B5's device Pass 2 should record the seam rather than treat
it as a defect.

---

### D-026 — D-017 assigns a cover "once at creation", and every project that already exists was created before the field existed {#d-026}

| | |
|---|---|
| **Artifact** | the [**D-017** ruling](#d-017-ruling) — *"assign the cover surface once when the zine is created and persist that assignment"* |
| **Found** | 2026-07-31, during Phase B / B5 planning, before any production code |
| **Severity** | **BLOCKS the shelf for pre-existing projects.** Every zine on a real device today has no assignment |
| **Status** | ✅ **RESOLVED 2026-07-31 — [see below](#d-026-ruling).** Both questions ruled: a duplicate generates a **new** cover, and **legacy zines receive a cover on first presentation, then persist it**. |

**The gap.** B5 adds a persisted surface+stamp to the `meta.json` sidecar (`ProjectMeta`, ADR-042) and its Room
index. `ProjectMeta` today is `(title, createdAtEpochMs)` — no cover. So the ruling's *"at creation"* hook
covers every **future** zine and no **existing** one, and the shelf must draw a cover for those too.

**Why the obvious answer is still an owner call.** *"Assign on first read and persist"* is one line of code and
almost certainly right — but D-017 is explicitly a ruling about **identity**: *"the persisted assignment becomes
part of the zine's identity."* Choosing when an existing object acquires its identity is the same kind of
decision the ruling reserved, and the register's standing rule is that **measuring something real licenses
asking, not deciding**. B4 was corrected for exactly this move one package ago.

**The alternatives.**

| Rule | What it costs |
|---|---|
| **Backfill on first read, persist immediately** | the cover is stable ever after, but is assigned at an arbitrary moment (whenever the user next opened the app) rather than at creation |
| **Backfill in a one-shot migration**, seeded deterministically from the **id** | every existing zine gets its cover in one act; the id is not the title, so D-017's "not derived from the title" holds — but it *is* derivation, which the ruling's spirit resists |
| **Backfill at `createdAtEpochMs` order**, i.e. as if assigned in creation sequence | closest to "at creation" — but it is round-robin by another name, which D-017 named and forbade |

**A second creation path the ruling does not name: `duplicateProject`.** D-017 says *"assign once when the zine
is **created**"*, and a duplicate is created — it gets a new id, and [ADR-022](../DECISIONS.md#adr-022) makes it
a genuinely separate project over shared content. So does the copy **inherit** the original's cover or **draw a
new one**? Both readings are defensible from the ruling's own words, and they say opposite things about what a
zine *is*:

- **Inherit** — the cover belongs to the *work*, so a duplicate of "Sunday market" looks like "Sunday market".
  But then the shelf shows two identical objects, and the Library's whole question is *"which zine is mine?"* —
  which two identical covers cannot answer. This is the failure the covers-only shelf is most exposed to,
  because [ADR-083](../DECISIONS.md#adr-083) moved every distinguishing metadata line into the action sheet.
- **Draw a new one** — the cover belongs to the *object*, which is what *"the persisted assignment becomes part
  of the zine's identity"* most plainly says, and two physical copies of a zine printed on different stock are
  two objects. But a user who duplicates to make a variant may reasonably expect the variant to look related.

**Owner decision requested — two questions.** (1) How does a zine that **already exists** acquire its cover?
(2) When a zine is **duplicated**, does the copy inherit the original's cover or draw its own?

#### D-026 — owner ruling, 2026-07-31 {#d-026-ruling}

> **A cover is persistent visual identity. Assign a cover once when a zine is created. Persist it. When a zine
> is duplicated: generate a new cover. Duplicate content. Do not duplicate visual identity.**

Question (2) is answered outright, and against the reading that felt more intuitive: **a duplicate is a new
object and gets a new cover.** *"Duplicate content, not visual identity"* is the sentence that decides it, and
it protects the thing the covers-only shelf is most exposed to — two identical objects on a screen whose only
question is *"which zine is mine?"*, with every distinguishing detail moved into the action sheet by
[ADR-083](../DECISIONS.md#adr-083). It also completes D-017: a cover is not derived from the title, not
round-robin, not inferred from neighbours, **and not inherited**.

**Question (1) — legacy zines — ruled separately the same day, and it is a ruling, not an inference.** B5 raised
the reading rather than assuming it, and the owner adopted it in terms:

> **Legacy zines receive a cover on first presentation. The assigned cover is then persisted.**

So a zine that predates the field acquires its cover **the first time the shelf draws it**, that assignment is
**persisted immediately**, and thereafter it behaves exactly like one assigned at creation. It is the only one
of the three candidates that contradicts nothing already ruled: seeding from the id is *derivation*, which D-017
resisted in principle even though the id is not the title, and assigning in `createdAtEpochMs` order is
round-robin under another name, which D-017 named and forbade. The ruling keeps the load-bearing property —
**assigned once, never re-derived, part of the zine's identity thereafter** — and differs from *"at creation"*
only in *when* that one assignment happens for objects that existed before there was anything to assign.

**The assertion this earns.** *"Assign on first presentation"* and *"assign on every presentation"* are
indistinguishable in a single render, so the test that matters is the **second** read: a project with no stored
recipe gets one, and reading it again returns **the same** recipe. A test that only checks a cover appears would
pass on a re-drawing implementation, which is precisely the class of blind assertion this programme keeps
producing.

---

---

### D-027 — the sheet's metadata line is shipped in a vocabulary the frozen file never uses {#d-027}

| | |
|---|---|
| **Artifact** | [`v2-library.html`](mockups/v2-library.html) `:185-190`, `:214` — `data-sub` → `.sh-sub` |
| **Found** | 2026-07-31, by the **mid-package adversarial review** of Phase B / B5, against shipped code |
| **Severity** | **Cosmetic, and only inside the action sheet.** The shelf draws no metadata at all, so nothing on the Library's own surface is affected |
| **Status** | 🟡 **OPEN — owner ruling required.** B5 ships the reused formatter and pins it in a test; the entry records the difference rather than resolving it |

**The gap.** The frozen file gives five example subtitles: `"A4 · 2 days ago"`, `"Letter · today"`,
`"A4 · 5 days ago"`, `"A4 · 1 week ago"`, `"Letter · 2 weeks ago"`. B5 composes the line from V1's already
unit-tested `editedLabel`, exactly as [ADR-086](../DECISIONS.md#adr-086) row 8 planned — *"reuses `editedLabel`
… B5 asserts the **wiring**, not the formatter"*. The shipped strings therefore read `"A4 · Edited 2 days ago"`,
and they differ from the freeze in two ways:

1. **The word "Edited" appears nowhere in the frozen file.** Every frozen example is bare — the stock, the
   separator, the recency. `editedLabel` prefixes all five of its cases, because it was written for V1's card,
   where the line stands alone under a title and needs a verb.
2. **There is no week granularity.** `editedLabel` runs *just now · N minutes · N hours · yesterday · N days*,
   so the freeze's `1 week ago` renders as `Edited 7 days ago` and `2 weeks ago` as `Edited 14 days ago`.

**Why this is a question rather than a fix.** Row 8's own note licenses the formatter as the authority on the
words, on the ground that *"the frozen file shows five example values and defines no thresholds"* — which is
true of the **boundaries** and false of the **vocabulary**: the freeze does exhibit week-scale wording, and it
never says "Edited". Reading `data-sub` as five literal design decisions and reading it as five illustrations
of a shape are both defensible, and they lead to different code. Choosing between them is a design call on
copy that a user reads, which is the owner's, and [D-020](#d-020-ruling) is the standing warning against
implementers settling this class of question by inference.

**The alternatives.**

| Rule | What it costs |
|---|---|
| **Keep `editedLabel` as shipped** | one formatter, already tested, one vocabulary across V1 and V2 — but the sheet says a word the design does not, and loses week-scale wording |
| **A V2 recency formatter matching the five frozen examples** | exact parity in the sheet; a second formatter to keep, and two vocabularies in one app until V1's shelf is retired |
| **Amend the frozen file's five examples to the shipped strings** | one vocabulary and verifiable parity — but it edits the design to match the code, which is the direction [the freeze rule forbids](../CLAUDE.md#design-freeze) unless the owner intends it |

**Not blocking.** B5's row 8 asserts the wiring — paper first, separator, the formatter's own recency, and the
line drawn only in the sheet — and every one of those holds under either ruling. A ruling changes one function
and the literals in one test.

---

### D-028 — the Bench offers nineteen swatches to a text element; an accepted ADR pins that control to five {#d-028}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:404-407`, `:410` (`['Ink',ICON.colour,openInk]`), `:476-483` · [ADR-055](../DECISIONS.md#adr-055) Decision 6 · [`TypeBar.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/TypeBar.kt) `InkRow` |
| **Found** | 2026-08-01, during **Phase C planning** ([ADR-089](../DECISIONS.md#adr-089)), by the frozen property table — before any production code |
| **Severity** | **Conflict between an `Accepted` ADR and the frozen design** — blocks package **C6**, blocks nothing before it |
| **Status** | ⏳ **OPEN — owner ruling required** |

**This is not [D-003](#d-003--the-maker-palette-is-ten-inks-or-nineteen-depending-on-which-document-you-read) again.** D-003 asked *how big is the maker's palette* and was ruled: three bands, three categories, three collections, implemented in `ZinelyContentInks` as three distinct types. That ruling settles what the popover **contains**. It does not touch what an **ink applied to text** may be, and that is a different document's decision.

**The conflict.** [ADR-055](../DECISIONS.md#adr-055) is `Accepted`, both of its human gates are closed, and its Decision 6 reads:

> *"the **five brand text-inks** map onto fixed `ColorRgba` values … the text `INKS` palette (5) is **deliberately distinct** in the frozen design from the image spot-ink `FIELD` set (4 vivid field inks) — text inks are AA-tuned for legibility; the two are not unified and **must not be conflated** into a shared table."*

The shipped Type bar implements exactly that: five swatches, AA-tuned, one `ColorRgba` each. The frozen Bench's text element carries an **Ink** verb (`:410`) that opens the H4 popover, and `applyInk` is bound to **every** `.sw2` in all three bands (`:476-477`) — so under the freeze a title can be set to `Fog #B7AD93` or `Cream #F1E9D6` on `--paper`, neither of which is AA-tuned for text and one of which is nearly invisible on paper.

Both documents are internally coherent and they were authored for different products: ADR-055 for the V1 Bench (frozen `design/v1/bench.html`, 2026-07-16), the H4 palette for the V2 Bench (frozen 2026-07-28). This is the same **staleness-by-authorship-date** shape as [D-005](#d-005--the-library-and-the-bench-set-the-same-role-in-two-different-serifs-at-two-different-weights) / [D-011](#d-011--the-library-declares-neither-easing-token-and-animates-on-a-curve-found-nowhere-else) / [D-022](#d-022--the-librarys-scrim-is-a-theme-invariant-literal-while-the-corpus-publishes-a-theme-aware-one) — with one difference that matters: **there the stale artifact was a frozen HTML file; here it is an `Accepted` ADR with two closed human gates.** The register's precedents do not reach that far, which is why this is raised rather than assumed.

**A second question rides with it, and it has no answer anywhere.** `TextStyle.color` is a single value. The frozen presets are **three-colour recipes** (`Warm zine` = `#B0503F` · `#E7B53C` · `#F1E9D6`), and the prototype resolves that by applying `PRESETS[i][1][1]` — *the second colour* (`:478`). A palette of three landing as "the second one" reads like prototype convenience rather than design; if it is design, it should be stated, and if it is not, the product needs a rule.

**Consequence of leaving it.** Neither reading is expensive to build and both are expensive to change afterwards: the ink row is a control with goldens, mutations, an announcement path and an AA gate attached to whichever set it offers.

**Owner decision requested.** (a) The frozen H4 palette wins and ADR-055 Decision 6's five-ink mapping is **superseded** for V2 — with a stated position on whether any AA floor applies to in-page text, since the frozen swatches are not tuned for it. (b) ADR-055 wins and the popover offers only the five for **text**, the full nineteen for decor — which departs from the frozen file at a visible control and needs the D-022 treatment. (c) Something narrower. And, either way: what does a three-colour preset do to one element?

---

### D-029 — the studio's defining element has no data model, no persistence and no stated scope {#d-029}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:132-146`, `:341-346`, `:541-558` · [V2-BENCH-IA-INTERACTION.md §D.1](V2-BENCH-IA-INTERACTION.md) and **§A.2** · [V2-BENCH-REVIEW.md §E.4](V2-BENCH-REVIEW.md) · [`Document.kt`](../../core/model/src/main/kotlin/com/aritr/zinely/core/model/Document.kt) |
| **Found** | 2026-08-01, during **Phase C planning** ([ADR-089](../DECISIONS.md#adr-089)) |
| **Severity** | **Net-new capability presented as a re-skin** — no longer blocks anything in Phase C |
| **Status** | ⏳ **OPEN — owner ruling required, but no longer by Phase C.** [OD-2 (2026-08-01)](../DECISIONS.md#adr-089) re-seated H1 and `DecorElement` beyond the phase; this entry now awaits the phase that takes them |

**What the freeze specifies completely.** The tray's appearance and one gesture: `.tray` with its header, the `.trayrow`, `.mat-item` bits set down by hand at ±1–2px, the `＋ keep` action, the collapse toggle, and *"tapping a shelf item places it at page centre, pre-selected, with a materialise-at-spot animation"* ([§D.1](V2-BENCH-IA-INTERACTION.md)).

**What nothing specifies.** Where the gathered material *is*. Verified against the repository on 2026-08-01:

- `ZineDocument` has no shelf, no tray, no gathered collection. `grep -ri "keep.\?clear\|decorelement"` over `core`, `feature` and `app` returns **zero**.
- `Element` is `ImageElement | TextElement`. [§A.2](V2-BENCH-IA-INTERACTION.md) introduces a **third kind** — `DecorElement`, *"net-new"*, in its own words — which the frozen page then draws (`.decor`, `.sticker`, and every `placeDecor` call).
- The prototype's tray is a JS array that a page reload empties. [§E.4](V2-BENCH-REVIEW.md) promotes the opposite to a **build invariant**: *"reopening the Bench lands on the user's page, at the same page number, **materials still on the shelf**, exactly as left"* — explicitly *"freeze-blocking for the Compose build, not the HTML"*.

So the tray **must** persist, and nothing says where, in what, or for how long.

**Four questions, each with a different owner.**

1. **Scope** — is the shelf **per zine** or **per app**? "Your shelf" in the Library means the whole collection; "Your shelf" in the Bench means this tray. A photo gathered while making zine A: does it appear while making zine B?
2. **Home** — inside `ZineDocument` (so it rides autosave, undo and the document schema, and a gathered-but-unplaced photo is part of the document) or beside it (a new store, its own migration)? The first makes "gathered" undoable, which may or may not be wanted.
3. **Lifecycle** — [ADR-025](../DECISIONS.md#adr-025)'s asset store is **mark-and-sweep**. A gathered-but-never-placed photo is, to a sweeper, unreferenced. Either the shelf is a GC root or gathering a photo and closing the app silently deletes it.
4. **`DecorElement`** — a third element kind touches `core:model`, the serializer, the migration, the reducer, the hit-test, `CanvasReplayer` and `ElementSemanticsLayer`. It is the single largest piece of net-new engineering in Phase C and the roadmap's Phase C says *"no feature additions."*

**Why implementation must not settle it.** Every local answer is cheap and wrong in a different direction. `meta.json` is the nearest home because [B5 put the cover there](#d-026) — but a cover is one enum pair and a shelf is a list of asset references with a GC relationship. In-document is the nearest home because everything else the maker touches lives there — but then gathering a photo is an undoable document edit, which is a product statement nobody has made. This is [D-020](#d-020-ruling)'s rule at full strength: *where the corpus is silent, silence is not an invitation to interpolate*, and the nearest thing to interpolate from is the answer D-020 named.

**Owner decision requested.** The four questions above — or, if Phase C is a re-skin of the shipped editor after all, an explicit re-seating of H1 and `DecorElement` to a later stage, which is [ADR-089](../DECISIONS.md#adr-089)'s **OD-2**.

**✓ OWNER RULING — 2026-08-01 (OD-2): the re-seating, not the four questions.** *"Phase C remains a parity phase. It does NOT introduce new editor capabilities. Therefore: H1 materials shelf, DecorElement … any capability requiring new document-model concepts are explicitly re-seated beyond Phase C."*

**This entry stays open, and that is the point.** The four questions — where a gathered material lives, whether it survives the process, what its scope is, and how it relates to [ADR-025](../DECISIONS.md#adr-025)'s mark-and-sweep root set — are unchanged and unanswered. What changed is *who has to answer them and when*: not the phase that merely **meets** the capability while re-skinning a screen, but the phase that **takes** it. Phase C now transcribes `.tray` and `.decor` as frozen-and-unimplemented ([ADR-089](../DECISIONS.md#adr-089) rows 1.17 and the deliberately absent C7), and [§E.4](V2-BENCH-REVIEW.md)'s *"persistence of place"* build invariant travels with the capability rather than being quietly satisfied by a page index alone ([ADR-089](../DECISIONS.md#adr-089) row 9.3).

No phase has been assigned. Assigning one is a roadmap act, and the ruling did not make it — see [COMPOSE-V2-ROADMAP.md § Re-seated beyond Phase C](../COMPOSE-V2-ROADMAP.md#re-seated-beyond-phase-c).

---

### D-030 — the frozen Bench runs twelve pages and offers add/delete; the product has one fixed eight-page format {#d-030}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:389` (`NP=12`), `:291`, `:562-583` · [V2-BENCH-IA-INTERACTION.md §D.2](V2-BENCH-IA-INTERACTION.md) · [`ModelEnums.kt`](../../core/model/src/main/kotlin/com/aritr/zinely/core/model/ModelEnums.kt) `SINGLE_SHEET_8(pageCount = 8, rows = 2, cols = 4)` |
| **Found** | 2026-08-01, during **Phase C planning** ([ADR-089](../DECISIONS.md#adr-089)) |
| **Severity** | **The frozen navigation navigates a product that does not exist yet** — no longer blocks **C5** |
| **Status** | ⏳ **OPEN — owner ruling required, but no longer by Phase C.** [OD-2 (2026-08-01)](../DECISIONS.md#adr-089) re-seated variable page counts and page add/delete/reorder; C5 builds the nav over the document's real eight |

**The numbers.** The frozen file is authored at twelve pages: `NP=12`, the page number reads `3 / 12`, the filmstrip builds twelve thumbs, and the grid header says *"Your zine · 12 pages"*. [§D.2](V2-BENCH-IA-INTERACTION.md) states the intent behind it — *"zines now scale to ~32 pages"*, three shapes (dots ≤8 → filmstrip 9–32 → summoned grid), and the grid does *"jump / drag-reorder / **add** / **delete**"*.

The shipped product has exactly one format, `SINGLE_SHEET_8`, eight pages, fixed by the single sheet it folds from. [Phase D](../COMPOSE-V2-ROADMAP.md#phase-d--proof) states plainly that *"booklet / saddle-stitch / duplex are **out of this stage**"* — which is what a page count above eight would require.

**Why this is not arithmetic.** Three readings each produce different code, and two of them are product decisions:

| Reading | What gets built | What it costs |
|---|---|---|
| **The 12 is prototype content** | the filmstrip renders the document's real page count — 8 today — and no add/delete verb exists | honest today; **the dots shape is then dead code**, since ≤8 is *always* true and the filmstrip 9–32 never renders. Half of H2 is unreachable |
| **The 1→32 nav is the design and the format follows** | variable page counts, a second imposition format, add/delete/reorder verbs | this is the booklet stage Phase D defers, arriving inside Phase C through a filmstrip |
| **Build the three shapes now against 8** | all three shapes exist, only one is reachable | untestable branches and a screenshot nobody can produce; a golden of a state the product cannot enter is not evidence |

**Note the shape that follows from the first reading**, because it is easy to miss and it changes C5's whole property table: at eight pages the frozen morph *never fires*. The component the review called *"one component with three shapes"* has one reachable shape, and the ⊞ grid becomes the only way to see the zine whole — which may be exactly right, and is a product statement either way.

**Owner decision requested.** Does Phase C's page navigation render the document's real (fixed, eight) page count, with the dots/filmstrip morph and the grid's add/delete recorded as **specified-but-unreachable until a format that needs them exists**? Or does the 1→32 range come with a format change that belongs on the roadmap first?

**✓ OWNER RULING — 2026-08-01 (OD-2): the first reading.** Variable page counts and page add/delete are *"capability requiring new document-model concepts"* and are **re-seated beyond Phase C**. C5 renders the document's real, fixed eight.

**Two consequences the ruling makes concrete, and one it does not settle.**

- **`N` is read, never written.** C5's assertion is that the strip and the grid render `format.pageCount`, with `N = 12` **and** `N = 8` as planned mutations ([ADR-089](../DECISIONS.md#adr-089) row 5.16). Hard-coding eight would be right today and wrong the day a second format lands — the same class of bug as the twelve, arriving from the opposite direction.
- **The morph is specified-but-unreachable.** As this entry noted before the ruling: at eight pages the frozen 1→32 morph never fires, because the filmstrip's threshold sits above the only page count the product can produce. C5 transcribes the appearance it *can* reach and records the rest as unreachable — it does not build a threshold it cannot cross.
- **Unsettled:** which phase takes variable page counts, and whether it arrives as a format change (roadmap-first, as this entry proposed) or otherwise. The ruling re-seated the work without scheduling it.

---

### D-031 — the frozen Bench draws four controls that go nowhere, and drops one the product ships {#d-031}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:357-359`, `:410`, `:522`, and the whole file · [V2-BENCH-IA-INTERACTION.md §B.6](V2-BENCH-IA-INTERACTION.md) · [`EditorScreen.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt) (`onPreview`), [`EditorSupplyTray.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorSupplyTray.kt) (redo) |
| **Found** | 2026-08-01, during **Phase C planning** ([ADR-089](../DECISIONS.md#adr-089)) |
| **Severity** | **The screen has no exits** — blocks packages **C2** and **C4** |
| **Status** | ⏳ **OPEN — owner ruling required** |

This is [D-025](#d-025)'s shape one surface along: *the frozen file is a prototype, so it never navigates anywhere*. It is raised separately because the answers are not the same and one of them subtracts a shipped capability.

**Four things the freeze draws and wires to nothing.**

| | Frozen | Wired to |
|---|---|---|
| **Font** verb on a text selection | `:410` `['Font',ICON.font]` | nothing — the array has no third element |
| **Size** verb on a text selection | `:410` `['Size',ICON.size]` | nothing |
| the hand-off to **Read / the Proof** | *nowhere in the file*; `doneBtn` (`:522`) only deselects | — |
| **back** out of the Bench | *nowhere in the file* | — |

**And one the freeze does not draw at all: redo.** The bottom bar is `Undo · Add · Done` (`:357-359`). The shipped editor offers redo in `EditorSupplyTray`. Transcribing the bar therefore **removes a shipped capability**, which is not a re-skin; keeping redo adds a control the design does not contain, which is not parity either.

**Why each needs the owner and not the implementer.**

- **Font and Size.** [ADR-055](../DECISIONS.md#adr-055) is directly contrary on one of them: it **excludes font choice** from scope (*"production renders a single Inter family by ADR-028; offering a choice would require bundling more families"*) and reclassifies it to V1. So the frozen Bench draws a verb an accepted ADR ruled out of scope — and rides [D-004](#d-004--the-frozen-zine-content-is-set-in-fraunces-the-render-engine-can-only-draw-inter) besides. Size *is* shipped, inside the Type bar, so the frozen file offers as a top-level verb something ADR-055 put one level in. B3's precedent applies to both: **nothing is not "does nothing"** — the freeze specifies nothing here, and holding still is the narrowest thing an implementation can do, but a verb that visibly does nothing is not shippable.
- **The Read hand-off.** [§B.6](V2-BENCH-IA-INTERACTION.md) is explicit that the Bench *hands off to Read for the finished-book reveal* — *"ending on pride, not on imposition"* ([BP-7](V2-BENCH-PRINCIPLES.md), [ADR-058](../DECISIONS.md#adr-058)) — and the file provides no affordance for it. The screen as frozen is one a maker cannot leave except by the system back gesture.
- **Back.** [ADR-051](../DECISIONS.md#adr-051)'s **loss-safe back** is a constitutional invariant. It has no frozen appearance on this surface.

**Owner decision requested.** Name the destinations — the same ruling D-025 gave the Library's five actions — and rule on redo: **kept** as a platform affordance the design does not draw, or **removed** with the freeze. Note that "reuse the existing flow" carries its consequences with it, exactly as [D-025's ruling](#d-025-ruling) did for delete-with-undo.

---

### D-032 — the keep-clear cue has a frozen appearance, no trigger, and a written trigger the product cannot compute {#d-032}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:101-103`, `:292` · [V2-BENCH-IA-INTERACTION.md §A.4](V2-BENCH-IA-INTERACTION.md) · [BP-4](V2-BENCH-PRINCIPLES.md) |
| **Found** | 2026-08-01, during **Phase C planning** ([ADR-089](../DECISIONS.md#adr-089)) |
| **Severity** | **A frozen state with no reachable trigger** — affects package **C1**; the resting cue is unblocked |
| **Status** | ⏳ **OPEN — owner ruling required** |

**What is frozen and complete.** `.keepclear` at rest: `inset:16px`, `1px dashed var(--ink-faint)`, `border-radius:3px`, `opacity:.32`. It is in the markup (`:292`) and it transitions on `opacity .3s, border-color .3s`. Nothing about the resting cue is ambiguous, and nothing like it exists in the repository — `grep -ri "keep.\?clear"` over `core`, `feature` and `app` returns zero, so the whole cue is net-new.

**What is not.** `.keepclear.warn` — `opacity:.9`, `border-color:var(--strawberry-text)` — is declared at `:103` and **the class is never added anywhere in the file's 254 lines of script**. The frozen prototype cannot enter its own warn state.

The written authority does specify the trigger, and that is where the problem is. [§A.4](V2-BENCH-IA-INTERACTION.md): *"faint and warm at rest, brightening **only when text or a face crosses it**"*. Two clauses, two very different costs:

- **text crossing the inset** is computable today — the layout is known, the inset is known, the intersection is arithmetic over the existing `SharedTextLayout` bounds. This half is implementable as specified.
- **a face crossing the inset** requires **face detection on the user's photo**. The product bundles no such engine; the nearest ones are ML Kit (a Google Play Services dependency) or a bundled model (APK weight and a second decode path). It is also the first feature in Zinely that would *analyse the content of a user's photo*, which is not a network question — the analysis is on-device — but it is squarely a **privacy-principle** question, and [PRD §5](../PRD.md#5-product-principles-non-negotiable) is where that is decided, not here.

**Why this is not "just implement the text half".** Shipping the text clause alone gives a cue that fires for a title over the trim and stays quiet for a face over the trim — the case a maker is most likely to regret at the fold, and the one [BP-4](V2-BENCH-PRINCIPLES.md) names first. A cue that is right half the time teaches the maker to distrust it, which is worse than a cue that is honestly scoped to text.

**Owner decision requested.** (a) The warn state fires on **text only**, and the design's *"or a face"* is amended to say so. (b) Face detection is in scope for the Bench, with its dependency and privacy consequences ruled explicitly. (c) The warn state is deferred whole and C1 ships the resting cue alone, with the entry left open against the phase that takes it.

---

## Resolved

| ID | Defect | Resolved |
|---|---|---|
| **D-001** | `v2-bench.html`'s header contradicts the freeze record | 2026-08-01 — closed by **Phase C / C0**, the documentation-only package that existed for it: the stale header line deleted, the stale footer clause stripped, D-005's stand-in note and the [D-010 amendment](#d-010-amendment) kept. No selector, declaration or script touched. [Closure](#d-001-closure) and entry kept above. |
| **D-010** | The page shadow is hard-coded to the light theme and does not adapt in the dark | 2026-08-01 — owner ruling (OD-3 of [ADR-089](../DECISIONS.md#adr-089)): **amend the frozen Bench and Proof** with a dedicated `--page-shadow` (cast) + `--page-contact` (contact) pair, preserving light byte-for-byte and re-deriving dark from the Library's own pair. Spec first, per D-024's precedent; **Compose deferred** to Phase C / C1 and Phase D. The [amendment](#d-010-amendment) and the entry are kept above. |
| **D-003** | The maker palette is ten inks or nineteen, depending on which document you read | 2026-07-28 — owner ruling: three bands, three categories, three collections. Entry kept above with its full resolution. |
| **D-020** | The shelf states a fixed two-column grid with no breakpoint, and Phase B verifies on foldables | 2026-07-30 — owner ruling: two columns, no breakpoint, no responsive behaviour, no maximum cover width, **and none of them to be invented**; *"future adaptive layouts require a future frozen design rather than implementation inference"*. No code change owed — B2 had already transcribed the freeze. Entry kept above. |
| **D-005** | The Library and the Bench set the same role in two different serifs at two different weights | 2026-07-28 — owner ruling: the Constitution outranks both frozen files. Canonical serif is **Fraunces at 500**; the Library's 600 reflected its Georgia fallback. No code change owed. Entry kept above. |
| **D-007** | The constitutional 8pt rhythm is not observable in the frozen CSS | 2026-07-28 — owner ruling: §III is an implementation **aspiration**, not a token inventory. **No spacing scale is published**; spacing stays per-component exactly as frozen. Entry kept above. |
| **D-015** | Two concepts are each drawn twice, with different geometry | 2026-07-29 — owner ruling: **do not deduplicate, canonicalize, or pick a preferred version**. Each geometry is an independent design asset; similarity is not evidence of identity. Convergence, if ever wanted, belongs in the corpus first. No code change owed. Entry kept above. |
| **D-013** | The Library and the Bench bake different alpha into the same grain | 2026-07-29 — owner ruling: **deliberate, not drift**. Paper and printed covers are different physical materials; grain strength is **not** normalised and stays exactly as frozen. No code change owed, and no corpus cleanup owed either. Entry kept above. |
| **D-014** | The paper material cannot be drawn at all on API 24–28 | 2026-07-29 — owner ruling: rendering **flat paper is correct**, not a fallback. No emulation, no approximation, no `minSdk` bump — where the platform cannot express the design, implementation omits and discloses. Ships as a Known Limitation. Entry kept above. |
| **D-011** | The Library declares neither easing token and animates on a curve found nowhere else | 2026-07-28 — owner ruling: the Bench and Proof are the **canonical V2 motion language**; the Library's curve reflects its earlier freeze. Phase B uses the canonical tokens. No code change owed. Entry kept above. |
| **D-002** | Two frozen cover inks put their titles below AA for normal text | 2026-07-30 — owner ruling: the governing floor for cover titles is **3.0:1**. No frozen colour changes, no HTML change, no design amendment; wording that implied a stricter level was clarified instead. `ZinelyContentInksTest`'s existing 3.0 gate is confirmed. Entry kept above. |
| **D-006** | The only shape token in V2 is declared and never used | 2026-07-30 — owner ruling: **dead specification — delete it from the frozen HTML**, and introduce no 18px radius token. `--r:18px` removed from `v2-bench.html` and `v2-proof.html`; `ZinelyV2Dimens` still publishes no radius. Entry kept above. |
| **D-016** | Two of Phase A's acceptance criteria cannot be met by a phase forbidden to touch product surface | 2026-07-30 — owner ruling: **only the token-routing clause re-seats, to Phase D**; *"confirmed to be the same migration"* is **satisfied by confirmation** of the architecture and strategy ([ADR-080](../DECISIONS.md#adr-080), now `Accepted`). **Phase A passes its gate.** Entry kept above. |
| **D-017** | The frozen Library shows six covers and states no rule for giving a cover to a seventh zine | 2026-07-30 — owner ruling: **assign once at creation and persist**; do **not** derive from the title, round-robin, or infer from neighbours. The assignment *is* part of the zine's identity. B1's title hash deleted; persistence owed at **B5**. Entry kept above. |
| **D-018** | The cover's ink band specifies `multiply`, which Android cannot honour below API 29 | 2026-07-30 — owner ruling: **follow D-014 — omit the band**. No emulation, no substitute blend mode. Ships as one Known Limitation together with D-014's flat paper. Entry kept above. |
| **D-021** | The sheet's icons are Unicode characters, and half of them are not in the app's own font | 2026-07-30 — owner ruling: **keep the literal characters exactly as frozen**; no substitution, no redesign, and **bundled-font coverage does not justify changing the design**. Platform fallback accepted; a future design revision may replace the glyphs explicitly. No code change owed — B3 had already transcribed them. Entry kept above. |
| **D-022** | The Library's scrim is a theme-invariant literal, while the corpus publishes a theme-aware one | 2026-07-30 — owner ruling: **the corpus is authoritative**; implement the published light/dark values. **Code changed** (`ZineActionScrim` takes `ZinelyV2Colors.scrim`), making this the only V2 value not transcribed from the frozen Library file. Third of the D-005 / D-011 set: where the Library contradicts a corpus token, the corpus wins. Entry kept above. |
| **D-024** | The frozen Library specifies a two-state screen; the real shelf has four states | 2026-07-31 — owner ruling: **Loading and Error are product states and belong in the canonical design.** Not prose, not invented in Compose — **the frozen HTML was amended** (`.ph`/`body.is-loading`, `.fail`/`.retry`/`body.is-error`), the first V2 amendment that *adds* design. Two further rulings with it: the **dock stands in all four states** (it belongs to the workspace, not the loaded content — no second workspace grammar), and the **loading debounce is implementation, not design**, kept out of the HTML and recorded as a seam in [ADR-086](../DECISIONS.md#adr-086). Entry kept above. |
| **D-025** | Seven frozen actions have no destinations, and four need UI the freeze does not contain | 2026-07-31 — owner ruling: **reuse existing behaviour; invent no new product concept.** Rename/Delete/Make a zine take their existing flows (delete keeps its undo), Share & export **routes into the existing Proof** — and there is **no shelf-level export**. Consequence accepted: V1 chrome inside a V2 screen until the phase that re-skins it. Entry kept above. |
| **D-026** | D-017 assigns a cover "at creation", but existing zines predate the field and a duplicate is created too | 2026-07-31 — owner ruling: **a cover is persistent visual identity; assign once at creation and persist. A duplicate generates a NEW cover — duplicate content, not visual identity.** Completes D-017: not from the title, not round-robin, not from neighbours, **and not inherited**. **Legacy zines receive a cover on first presentation, then persist it** — ruled explicitly, not inferred. Entry kept above. |
| **D-019** | The frozen trilogy has no right-to-left reading, and a printed cover has a physical handedness | 2026-07-30 — owner ruling: **the printed artifact does not mirror**, in any locale; binding edge, fore-edge and crease stay exactly as frozen. Chrome may adapt to RTL; artifacts do not. Entry kept above. |

*(Resolved entries stay in place rather than being deleted — the record of what was once contradictory
is what stops it being reintroduced.)*

---

*Opened 2026-07-28 during the Compose V2 implementation programme; register verified against every entry's
status line on 2026-07-29 (package A10) and again on 2026-07-30 at the **Phase A closeout**, when the
D-002, D-006 and D-016 owner rulings were recorded — and extended the same day by **D-017**, **D-018** and
**D-019**, raised by Phase B / B1 and ruled on the same day ([ADR-081](../DECISIONS.md#adr-081)), then by
**D-020**, raised by Phase B / B2 and likewise ruled the same day
([ADR-082](../DECISIONS.md#adr-082)), and finally by **D-021** and **D-022**, raised by Phase B / B3
([ADR-083](../DECISIONS.md#adr-083)) and **both ruled the same day** — D-021 confirming the frozen characters,
D-022 replacing the Library's scrim with the corpus token. Extended once more on **2026-07-31** by
**[D-023](#d-023)**, raised against Phase B / B4 ([ADR-084](../DECISIONS.md#adr-084)) **by independent review
rather than by implementation**, and the first entry since Phase A to reach the owner **unruled**. Governed by
[V2-CONSTITUTION.md](V2-CONSTITUTION.md); process defined in
[COMPOSE-IMPLEMENTATION-RULES.md](../COMPOSE-IMPLEMENTATION-RULES.md).*
