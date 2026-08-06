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
and a frozen state with no reachable trigger (D-032). **The count is now thirty-three: twenty-three resolved, ten
open** — the twenty-first being [D-001](#d-001--v2-benchhtml-header-contradicts-the-freeze-record), closed by
**Phase C / C0** on 2026-08-01, the first Phase C package to land, and the twenty-second [D-032](#d-032), ruled
the same day. The twenty-third is **[D-033](#d-033)** — raised by C1's own blocker check and amended out of existence the same day. The full statement of what Phase C owes is
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

*The paragraph that follows is the record as it stood when Phase C opened; the sentence at its end brings it to the present.*

That left **three entries that block a Phase C package** — D-028 at C6, D-031 at C2, D-032 at C1's keep-clear
row. **None blocked C0, which has since landed.** D-032 was ruled on 2026-08-01 and fenced a single row rather
than the package — but **C1's own pre-implementation blocker check then raised [D-033](#d-033), which does block
the package**: it decides the rectangle every other C1 property is positioned against. That blocker was itself
ruled the same day — option (c), [the third amendment to a frozen V2 surface](#d-033-amendment) — so **C1 is
unblocked and no entry blocks it**. **D-031 was ruled on 2026-08-01 (OD-9)** and C1 is committed at `29a3819`. **As of 2026-08-05, NO entry fences Phase-C work.** D-028 was the last, and it was ruled that day — [OD-24](#d-028-ruling), option (c), the file's **eighth** amendment. C9 remains blocked by [D-012](#d-012), which is answered *inside* it by design. [D-035](#d-035) blocked all of Phase C for part of one day — C1's device verification found the dimmed sheet left the user's own content at **1.60:1** — and was [ruled the same day](#d-035-ruling). [D-034](#d-034) fenced C2's `.ctx*` rows for a day — raised by C2's own pre-implementation check the moment OD-9 was applied, [ruled 2026-08-02](#d-034-ruling) as **keep both**, with C2 split into **C2a** and **C2b** and both now unblocked. Live entries that fence nothing — D-012 (answered *in* C9), D-023, D-029, D-030 — are listed below and in the [roadmap's ledger](../COMPOSE-V2-ROADMAP.md).

| Open | Owing to | One line |
|---|---|---|
| ~~[**D-028**](#d-028)~~ | ✅ **RESOLVED 2026-08-05** — [owner ruling](#d-028-ruling), **OD-24**, option (c); the frozen Bench is amended an **eighth** time | the ink **target** selects the bands: a text element is offered **Inks + Neutrals** and `Paper tints` are not drawn for it, because they are paper — by the band's own label and by the presets' third slot. A preset applies **`[0]`**, the primary ink, not the accent. **No contrast floor is imposed on in-page text ink**, deliberately. [ADR-055](../DECISIONS.md#adr-055) Decision 6's exclusivity is superseded; the Type bar's five stay reachable under OD-11 |
| [**D-029**](#d-029) | ⏳ **an owner ruling** — **the phase that takes H1** (no longer Phase C) | the holding shelf and `DecorElement` are net-new: no model, no persistence, no scope, and a GC relationship |
| [**D-030**](#d-030) | ⏳ **an owner ruling** — **the phase that takes variable page counts** (no longer Phase C) | the frozen nav runs 12 pages and adds/deletes them; the product has one fixed 8-page format |
| ~~[**D-031**](#d-031)~~ | ✅ **RESOLVED 2026-08-01** — [owner ruling](#d-031-ruling), OD-9 | the Bench had no exits: Font and Size stay **drawn** with no invented capability, Read reuses [ADR-086](../DECISIONS.md#adr-086)'s hand-off, back reuses the existing stack, and **redo is kept** — the frozen bar specifies the editing surface, not the product's whole capability |
| ~~[**D-037**](#d-037)~~ | ✅ **RESOLVED 2026-08-02** — [owner ruling](#d-037-ruling), OD-13 | the dim shipped without either of the freeze's two ways out of it. `Intent.ClearSelection` exists in the reducer and **nothing dispatches it**; the freeze's `canvas` click and Done button are unowned and C4's respectively. A stuck selection now fades everything else the user wrote, to **2.78:1**, undismissably. Pass 1 passed; **Pass 2 failed**. Ruled **(a)**: C2a adds tap-to-deselect, and selection becomes a **transient** editing state rather than a modal one |
| ~~[**D-044**](#d-044)~~ | ✅ **CLOSED — amendment MADE to `v2-bench.html:408`, and [ruled OD-17](#d-044-ruling) 2026-08-04: an owner-approved COMPANION amendment to OD-16, separately owned, not a consequence of it** | the frozen `.styletb` chips are **value displays** (`Fraunces`, `A 23`) wired to nothing. Transcribed literally they would claim a font and a size that are false for every element that is neither — [OD-9](#d-031-ruling)'s *invents nothing* cuts against the value, not for it. C3 ships them labelled by **verb** (`Font` · `Size` · `Ink`), matching the context bar; the swatch stays a value because row 3.9 makes it a *true* report. `v2-bench.html:408-410` is what needs editing |
| ~~[**D-043**](#d-043)~~ | ✅ **RESOLVED 2026-08-03** — [owner ruling](#d-043-ruling), OD-16, option **(b)** | ruled: **−96 is a maximum, not an unconditional literal.** `edit()` lifts by `min(96, slack + clearance)`; the frozen Bench was amended first and its prototype renders unchanged, because on its own geometry the two terms still sum past 96. Device evidence closed it: `SM-A176B` has **4.2dp of slack against a 96dp demand**, confirming the premise — but (a)'s predicted symptom never appeared, because the un-clipped canvas was *masking* it. That mask is [D-045](#d-045), created by the same ruling and landed in the same package. Two costs priced and recorded, not fixed: 43dp of a page-bottom box stays behind the row (the typed line clears by 23dp), and the page top still leaves the canvas when clearance demands the ceiling — clipped now, not painted over the chrome |
| ~~[**D-047**](#d-047)~~ | ✅ **RESOLVED 2026-08-04 — [owner ruling](#d-047-ruling), OD-21, Option A; the frozen Bench is AMENDED for the fifth time** | the frozen `.bar` drew **three** slots while [OD-9](#d-031-ruling) keeps redo, [OD-11](#d-034-ruling)/[OD-14](#d-039-ruling) keep **both** shipped add verbs, and `.add`'s handler opens a chooser whose region OD-2 re-seated beyond Phase C. Ruled: the bar becomes **`Undo · Redo · Add · Done`**; `Add` opens the frozen chooser with **only its Text and Photo rows released into C4**, Art staying fenced behind C8 — *“a fence reassignment, not a capability reassignment”*; the chooser uses the shipped `ZSheet`; **Text reuses `addTextAndEdit`** so C3's in-place model is untouched; `EditorSupplyTray` is retired. One line of frozen markup added (`#redoBtn`, `:466`), **no CSS changed**. The accepted price, recorded: both add verbs sit one tap deeper than they do today |
| ~~[**D-048**](#d-048)~~ | ✅ **RESOLVED 2026-08-04 by rulings already in hand — a recorded deviation, not an owner question, recorded exactly as [D-042](#d-042) was. No OD number, because none was needed** | `Done` follows the frozen two-state behaviour (`:653`): while a session is open C3's `#doneEdit` owns *finish* and the bar's `Done` is **withheld at the frozen `.icon-btn:disabled` `.35`** (`:269`) — [OD-14](#d-039-ruling)'s own method, as C2b applied it, using a presentation the file already draws; with no session the bar's `Done` owns *clear selection*, which is deselect's **first drawn control**, since [OD-13](#d-037-ruling) gave it only a gesture and a gesture has no presentation for OD-14 to count. **`Preview ›` does not move** — OD-9's *reuse, don't invent* is satisfied by leaving it exactly where it ships |
| [**D-060**](#d-060) | 🟦 **OPEN — raised by C6's Device Verification Pass 2, 2026-08-06; a defect in the FROZEN FILE, not in the Compose. Not a merge blocker; C6 is `Accepted` with it open** | the ink popover offers the swatch **`Ink` twice** — it is a member of both the `INKS` array (`v2-bench.html:596`) and the `NEUT` array (`:598`), and [OD-24](#d-028-ruling) draws both bands for a text target. On the device that is two near-identical near-black circles in two rows, announced identically to a screen reader, of which only one takes the selection ring. The owner already counted this — OD-24 says *"Inks + Neutrals — 13 distinct swatches"* over 14 drawn — so it is **not** a new decision; what is undecided is whether the duplicate should be **renamed** so the two can be told apart. A fix amends the frozen file first |
| ~~[**D-059**](#d-059)~~ | ✅ **RESOLVED 2026-08-05 — [owner ruling](#d-059-ruling), OD-23, Option (a); the frozen Bench is AMENDED for the seventh time** | [OD-22](#d-053-ruling) made the thumb's interior *the real page*. The sheet's ground is still the room's `--paper`, which in dark theme is `#2F2A22` — so in dark theme the miniature draws the page's content with **no paper under it**, and the user's own text measures **1.21:1** against the sheet (8.02:1 in light, same document, same words). This is [D-035](#d-035)'s failure a second time, one surface along; [OD-12](#d-035-ruling) ruled *the artifact does not dim* and the frozen file implements it as `.page{--paper:#F7F2E7}` (`v2-bench.html:222`) — an island scoped to `.page`, which the thumb is not. Extending it to `.pthumb` is a **seventh amendment to the frozen Bench** and therefore the owner's call, not the implementer's. |
| ~~[**D-058**](#d-058)~~ | ✅ **FIXED 2026-08-05 — a C5 defect, found by independent review** | C5 emitted the frozen `.navrow` **below** the bar. The freeze opens `.navrow` at `v2-bench.html:481` and `.bar` at `:488`, both in `.phone`'s normal flow, so the sheets belong above `Undo · Redo · Add · Done`. Nothing caught it: every test measured one row against itself, and ADR-095's own device checklist described the frozen order while the build did the opposite. The row is moved and the *relation* is now asserted — in `BenchC5Test` and in CI-31's `SurfaceTraversalOrderTest`, where it is also the reading order. |
| ~~[**D-057**](#d-057)~~ | ✅ **FIXED 2026-08-05 — a C5 defect, found by Device Pass 1** | The frozen `.cur{z-index:2}` was transcribed as `Modifier.zIndex`, which reorders the **platform accessibility tree** as well as the paint: the current sheet was published last, so a screen reader met page 1 after every other page. `traversalIndex` did not override it. The `zIndex` is dropped — a recorded deviation under the freeze's own allowance for post-freeze accessibility work — and its cost is a shadow tail, measured from the freeze's own geometry. |
| ~~[**D-056**](#d-056)~~ | ✅ **FIXED 2026-08-05 — a C5 defect, found by Device Pass 1** | C5 read `PageRole` to decide which sheets are covers, on an ADR-095 §3 clearance claiming the model carries those roles. It does not: every document is built with every page `INTERIOR`. Three frozen rows — 5.5a, 5.9's cover clause, 5.14 — were dead code on every real document, and passed anyway because all three test fixtures fabricated the roles. Covers are read by **position** now, as the freeze always said. |
| ~~[**D-055**](#d-055)~~ | ✅ **FIXED 2026-08-04 — a C4 test defect, found by C5's verification and repaired outside the C5 fence** | `ZinelyNavHostTest` waited ten seconds for the text *"Add a photo"* as its definition of **Ready**; [OD-21](#d-047-ruling) retired that shelf, so two `:app` tests had been failing with `ComposeTimeoutException` since `026d15a`. Reproduced in a clean worktree at `HEAD` — with no C5 change present — before it was attributed. Now waits on `BenchBottomBarTestTag`, which is what Ready actually draws |
| ~~[**D-054**](#d-054)~~ | ✅ **FIXED 2026-08-04 — a C4 golden defect, found by C5's verification and repaired outside the C5 fence** | C4 added the `Redo` mark to `ZinelyV2Icons` without re-recording `:core:ui`'s four catalog goldens, which have depicted a 36-mark set against a 37-mark code base ever since. It survived because `testDebugUnitTest` **captures** and only `verifyRoborazziDebug` **compares** — and C4's run named the former. Both goldens re-recorded; the sole delta is the new mark and the one-cell shift |
| ~~[**D-053**](#d-053)~~ | ✅ **RESOLVED 2026-08-04 — [owner ruling](#d-053-ruling), OD-22, Option (c); the frozen Bench is AMENDED for the sixth time** | the frozen  () is a **blank 26×34 paper sheet with three faint rules** drawn on every interior page; the shipped  draws a **live miniature of the real page** through the canvas's own  tape. Transcribing the freeze removes the only place you can see what is on another page **and** states something false on most pages ([D-044](#d-044)'s class, which [OD-17](#d-044-ruling) fixed by amending the file); keeping the miniature diverges from the surface [§E.2](V2-BENCH-REVIEW.md) was proudest of. No existing ruling reached the thumb's interior — OD-2 settled the count, D-009 the targets. **Ruled: amend first.** `.pthumb i` and its `<i>` are deleted from the specification, the interior becomes the real rendered page, and every other frozen property — size, radius, spine, shadow, transition, the `.cur` lift and the strawberry dot — is preserved and transcribed. **The grid is not amended.** The accepted price, recorded: at 26×34dp the miniature is a smudge, and C5 does not enlarge the thumb to compensate |
| [**D-052**](#d-052) | 🟦 **OPEN — raised by C4's Device Verification Pass 2, 2026-08-04; placement policy, not C4's fence** | `Add › Text` drops the new box **on top of** what is already on the page: the arriving box's editing outline enclosed an existing "Hello", so the page read as one block holding both the old words and the cursor. The drop comes from `addTextAndEdit`, which [OD-21](#d-047-ruling) required C4 to reuse **by name**; what C4 changed is the frequency — the route is now two taps from anywhere, so the collision is met on pages that already have content |
| [**D-051**](#d-051) | 🟦 **OPEN — raised by C4's Device Verification Pass 2, 2026-08-04; a defect in the FROZEN FILE, not in the Compose** | the chooser's `Photo` row is marked with the *replace / refresh* glyph — `v2-bench.html:721` builds it from `ICON.replace`, and `BenchAddChooser.kt` transcribes it faithfully. Beside `Text`'s clean `A` it reads as *"replace the photo"*. It matters more now than in the prototype: after [OD-21](#d-047-ruling) retired the shelf's *"Add a photo"* card, this glyph is the **only** visual the verb has. A fix amends the frozen file first, which is the owner's act |
| [**D-050**](#d-050) | 🟦 **OPEN — raised by C4, 2026-08-04; copy, and copy is owner-owned** | the empty page still says *"Grab a photo or a few words from the **supplies below**"* (`Copy.kt:175`) and still points a chevron at the shelf (`EmptyStateTrayCueTag`, `EditorEmptyState.kt:37`) — and [OD-21](#d-047-ruling) retired the shelf. The invitation now names a surface that is not on screen, one tap before the user meets a single `Add`. **Not fixed in C4:** the strings are product voice, not frozen CSS, and rewriting them is a wording decision |
| [**D-049**](#d-049) | 🟦 **OPEN — routed to C4 by [C2a's Pass 2](../DECISIONS.md#adr-091-completion-device) as P2-1; not a merge blocker. RE-MEASURED under C4's bar 2026-08-04: 1500 px → 1069 px, a 28.7 % loss of sheet height, larger than the 17 % first recorded** | the sheet measures `1028×1454 px` unselected and `850×1202 px` selected — a **17 % linear shrink**, and **28.7 % of sheet height once a selection also raises the frozen `.ctx` bar** — because the bottom chrome takes its space from the canvas (`.bar{flex:none}`, `v2-bench.html:267`). Pre-existing; [D-037](#d-037) did not cause it, but dismissal now makes the user meet it on **every** deselect, in the growing direction. Against [OD-12](#d-035-ruling)'s *the editor represents the physical printed artifact*, a sheet that resizes when you stop touching it is *"worth someone's decision"* — take the height, or overlay it and amend |
| [**D-046**](#d-046) | ⛔ **OPEN — [ruled OD-18](#d-046-ruling) 2026-08-04: stays open; no repo-wide sweep, no reopening of C3; repaired by each file's owning package or a dedicated docs-maintenance package** | the [OD-16](#d-043-ruling) amendment moved every address below the insert, stranding **25 explicit `v2-bench.html:NNN` citations across ten files C3 does not own**. C3 repaired only the files it owns, each verified against the frozen file. Three mechanical sweeps were attempted and all three did damage — including flattening [ADR-093 §1](../DECISIONS.md#adr-093)'s own drift table — so the enumeration and the working method are recorded instead of a fourth sweep across clean files. Documentation accuracy only; no runtime effect |
| [**D-045**](#d-045) | ✅ **FIXED in C3, 2026-08-03 — a parity defect, no ruling and no amendment** | the frozen `.canvasArea{overflow:hidden}` (`v2-bench.html:171`) was never implemented in Compose. Free while the pan was zero; on device once C3 made it real the panned sheet painted over the top bar in **every** editing session, leaving `Preview ›` invisible and — read off the platform tree — still `clickable=true` at full bounds. **A control you cannot see and can press is the defect; the paper over the chrome is how you notice it.** Fixed with `clipToBounds()` alongside the OD-16 clamp, because either alone makes the other worse |
| ~~**D-043 (original entry)**~~ | *superseded — kept for the record* | the frozen `edit()` lifts the page a literal `translateY(-96px)`. The prototype can afford it: a 324px page inside a 744px phone has slack above it. The **shipped page is *contained* in the canvas** and typically height-bound, so its slack is ≈ 0 — and an element in the top **96dp of screen** is lifted clean out of view by the gesture meant to reveal it. **96dp is the wrong unit to judge the severity in:** it is `96 / scale` points of *page*, so it is the top **≈ 73 %** of the page on the 300×400dp test host where it was reproduced, against ≈ 22 % on the measured `SM-A176B` canvas — worst on the smallest canvases, and the 73 % is the figure that actually has evidence. Reproduced deterministically, not inferred. [ADR-093](../DECISIONS.md#adr-093) row 3.1 specifies the literal and row 3.1a forbids correcting it unilaterally, so it **ships as specified** and the remedy is the owner's |
| [**D-042**](#d-042) | ✅ **RESOLVED by rulings already in hand — a recorded deviation, not an owner question** | [ADR-089](../DECISIONS.md#adr-089) rows 3.5–3.7 name `TypeBar.kt` as the control to **re-skin** into the frozen `.styletb`, which would delete size, align, bold, italic and five inks. [OD-11](#d-034-ruling) already says *"no existing editor capability is removed"* and [OD-14](#d-039-ruling) restates it, so that reading is dead by ruling. `.styletb` ships as the **editing state's** row — a state the product has no style control in — and `TypeBar.kt` is untouched. **The escalation was the error**; the review caught it |
| [**D-041**](#d-041) | 🟦 **OPEN — pre-existing, not C2b's, found while proving D-040 reachable** | leaving a page while a text session is open **orphans an empty text box**: `Intent.GoToPage → leavePage` clears selection and interaction without running `endTextSession`, so the blank-box cleanup that every other exit performs never happens. The box is autosaved, invisible on the page, and exports as nothing. Found on hardware; **no fix attempted — it is reducer behaviour, outside C2b's fence** |
| [**D-040**](#d-040) | ✅ **FIXED in C2b, no ruling needed — and reachable, which took two goes to establish** | the frozen bar offered **Size** and **Ink** on a *still-blank* text box, which the reducer refuses to style — and the tap was a **dead end that swallowed the toolbar**: it hid the frozen bar and raised nothing in its place, with no state that brought either back. Found by review, not by a test. Fixed under the ruling that already covers it ([OD-9](#d-031-ruling)): the two verbs ship inert there, exactly as `Font` does. **The implementer then claimed on device Pass 1 that the state was unreachable, and that claim was false** — see the correction below; the blank box is two taps away, and the guard was afterwards watched working on hardware |
| [**D-039**](#d-039) | ✅ **RESOLVED 2026-08-02 by owner ruling — [OD-14](#d-039-ruling)** | since C2b the Bench offers the same verb twice — **`Delete` in both bars, and `Reframe` in both a bar and an on-canvas chip**. C2b's device Pass 2 **did not pass** on this, and [ADR-092](../DECISIONS.md#adr-092) was held at `Proposed` until [OD-14](#d-039-ruling) resolved it; it is now `Accepted`. one per bar, both announcing the same word to TalkBack. Not an accident and not a bug: it is the priced cost of [OD-11](#d-034-ruling)'s *additive*. Both delete the selection, so the duplication is redundant rather than ambiguous, and any real disambiguation is a **third** mechanism neither bar specifies. Deferred to device Pass 1, where the sweep can be heard rather than reasoned about |
| [**D-038**](#d-038) | 🟦 **OPEN — an owner ruling, and it fences nothing** | the frozen photo bar offers **Replace**, and the product cannot honour it. `Intent.ReplaceImage(id, assetId)` exists in the reducer and is **dispatched from nowhere**; the only picker (`RequestAddImage` → `PickAndDecodeImage` → `CommitAddImage`) **creates** an element rather than re-pointing one, so reaching Replace is a flow change, not a wiring. C2b ships it **drawn and disabled** under [OD-9](#d-031-ruling)'s class — *a control the freeze draws stays drawn and invents nothing* — exactly as `Font` does. The question is whether the capability should exist |
| [**D-036**](#d-036) | 🟦 **OPEN — documentation only, fences nothing** | the frozen Bench draws **four** resize handles; the editor has **eight**, and the extra four carry axis-constrained resize. C2a kept all eight under [OD-11](#d-034-ruling) (*the frozen vocabulary is additive; no existing capability is removed*), which the review confirmed is the right reading. What is owed is the **canonical file catching up** with that ruling — recommendation **(a)**, draw eight |
| ~~[**D-035**](#d-035)~~ | ✅ **RESOLVED 2026-08-02** — [owner ruling](#d-035-ruling), OD-12 | the dark theme dimmed the sheet while the document's content ink stayed black — correctly, because it prints — leaving the user's own words at **1.60:1**. Ruled: **the artifact does not dim; the room around it may.** The frozen `.page` becomes a light-theme island of eight restated light tokens; `.phone` still dims |
| ~~[**D-034**](#d-034)~~ | ✅ **RESOLVED 2026-08-02** — [owner ruling](#d-034-ruling), OD-11 | the frozen `.ctx` is a **verb** bar and the shipped `EditorContextBar` is the **WCAG 2.5.7** single-pointer twin of the drag gestures — **and they are not mutually exclusive.** The frozen bar is **additive**; the transform controls stay, because a parity phase does not remove an accessibility path. C2 splits into **C2a** (unblocked) and **C2b** (`.ctx*`) |
| ~~[**D-032**](#d-032)~~ | ✅ **RESOLVED 2026-08-01** — [owner ruling](#d-032-ruling), OD-10 (C1 half) | the keep-clear warn state was never triggered in the freeze; the ruling makes it **transient interaction guidance**, so no content analysis is needed |
| ~~[**D-033**](#d-033)~~ | ✅ **RESOLVED 2026-08-01** — [the frozen Bench amended](#d-033-amendment) | the frozen page was not the document's panel and the 16px cue was not the 17pt safe area; the page is now 229×324 and the cue a truthful uniform 18.5px |
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
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) lines 424-427, 493, 496-503 · [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III · [V2-IDENTITY.md](V2-IDENTITY.md) §4 · [V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §H4 |
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
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 37, 125, 148, 163 · [`v2-bench.html`](mockups/v2-bench.html) lines 52, 213, 231, 269 · [`v2-proof.html`](mockups/v2-proof.html) lines 177, 224, 246 |
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
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) line 44 · [`v2-proof.html`](mockups/v2-proof.html) line 24 |
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

- `ZinelyV2Dimens`' KDoc described `--r` as *declared* at `v2-bench.html:44` / `v2-proof.html:24` and D-006
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
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 61, 84, 105, 149, 170 · [`v2-bench.html`](mockups/v2-bench.html) lines 132, 242 · [`v2-proof.html`](mockups/v2-proof.html) — no focus rule anywhere. *(Re-anchored 2026-08-01: the citations were captured before the [D-024 amendment](#d-024-amendment) moved the Library and the [D-010 amendment](#d-010-amendment) moved the Bench.)* |
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
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) line 105 → now line 98 · [`v2-proof.html`](mockups/v2-proof.html) line 98 → now line 112 (both moved by the amendment below) |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | Theme defect in the frozen specification — **does not block A4** |
| **Status** | ✅ **RESOLVED 2026-08-01** — owner ruling, applied as an [amendment to both frozen files](#d-010-amendment) |

**What it says.** Every other shadow in the Bench and the Proof takes its colour from
`var(--frame-shadow)`, which is re-derived for dark (`rgba(58,48,32,.28)` → `rgba(0,0,0,.5)`). Two do
not:

- `v2-bench.html:105` — `.page` — `0 14px 30px -14px rgba(58,48,32,.4), 0 2px 5px rgba(58,48,32,.14)`
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

**[`v2-bench.html`](mockups/v2-bench.html)** — light (`:root` `:50`, `:root[data-theme="light"]` `:126`):

```css
--page-shadow:rgba(58,48,32,.4); --page-contact:rgba(58,48,32,.14);
```

dark (`@media (prefers-color-scheme:dark)` `:65`, `:root[data-theme="dark"]` `:76`):

```css
--page-shadow:rgba(0,0,0,.6); --page-contact:rgba(0,0,0,.5);
```

and `.page` at `:118`:

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
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 36, 43, 52, 61, 93, 119, 122 · [`v2-bench.html`](mockups/v2-bench.html) line 44 · [`v2-proof.html`](mockups/v2-proof.html) line 24 |
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
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) line 171 · [`v2-bench.html`](mockups/v2-bench.html) lines 143, 293 · [`v2-proof.html`](mockups/v2-proof.html) lines 259-261. *(Re-anchored 2026-08-01, as [D-008](#d-008--two-of-the-three-frozen-surfaces-specify-no-focus-appearance-and-one-removes-it)'s were.)* |
| **Found** | 2026-07-28, during Phase A / A5 (motion) |
| **Severity** | **Accessibility inconsistency in the frozen specification** — does not block A5 |
| **Status** | **Open by owner ruling** (2026-07-28) — deliberately unresolved in Phase A; the behavioural decision belongs to **Phase C**, on physical devices |

**What they say.** Every file honours `prefers-reduced-motion`, which is the good news and is worth
stating plainly — this is not a missing-accessibility defect like **D-008**. But no two files honour it
the same way:

| File | Rule |
|---|---|
| `v2-library.html:171` | `*{transition:none!important}` — kills transitions; says nothing about animations (the Library has none) |
| `v2-bench.html:293` | `*{transition-duration:.01ms!important; animation:none!important}` — collapses transitions, **disables animations outright** |
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
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) line 18 · [`v2-bench.html`](mockups/v2-bench.html) line 55 · [`v2-proof.html`](mockups/v2-proof.html) line 36 |
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
| **Artifacts** | Platform constraint against the whole frozen trilogy — every `--grain` rule (`v2-library.html` 59/105/110 · `v2-bench.html` 94/107/119 · `v2-proof.html` 75/95/115/173) |
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
| **Artifacts** | [`v2-proof.html`](mockups/v2-proof.html) lines 310 and 390 · [`v2-bench.html`](mockups/v2-bench.html) line 379 against [`v2-proof.html`](mockups/v2-proof.html) line 308 |
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
| `v2-bench.html:379` | `M20 6 9 17l-5-5` | the Bench's *Done* button |

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
([`v2-bench.html:51,46`](mockups/v2-bench.html)) — note that the dark half is **stronger**, which is exactly
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
| **Status** | ✅ **RESOLVED 2026-08-05** — [owner ruling OD-24](#d-028-ruling), option (c). `v2-bench.html` **amended first**, per the HTML-first rule |

**This is not [D-003](#d-003--the-maker-palette-is-ten-inks-or-nineteen-depending-on-which-document-you-read) again.** D-003 asked *how big is the maker's palette* and was ruled: three bands, three categories, three collections, implemented in `ZinelyContentInks` as three distinct types. That ruling settles what the popover **contains**. It does not touch what an **ink applied to text** may be, and that is a different document's decision.

**The conflict.** [ADR-055](../DECISIONS.md#adr-055) is `Accepted`, both of its human gates are closed, and its Decision 6 reads:

> *"the **five brand text-inks** map onto fixed `ColorRgba` values … the text `INKS` palette (5) is **deliberately distinct** in the frozen design from the image spot-ink `FIELD` set (4 vivid field inks) — text inks are AA-tuned for legibility; the two are not unified and **must not be conflated** into a shared table."*

The shipped Type bar implements exactly that: five swatches, AA-tuned, one `ColorRgba` each. The frozen Bench's text element carries an **Ink** verb (`:410`) that opens the H4 popover, and `applyInk` is bound to **every** `.sw2` in all three bands (`:476-477`) — so under the freeze a title can be set to `Fog #B7AD93` or `Cream #F1E9D6` on `--paper`, neither of which is AA-tuned for text and one of which is nearly invisible on paper.

Both documents are internally coherent and they were authored for different products: ADR-055 for the V1 Bench (frozen `design/v1/bench.html`, 2026-07-16), the H4 palette for the V2 Bench (frozen 2026-07-28). This is the same **staleness-by-authorship-date** shape as [D-005](#d-005--the-library-and-the-bench-set-the-same-role-in-two-different-serifs-at-two-different-weights) / [D-011](#d-011--the-library-declares-neither-easing-token-and-animates-on-a-curve-found-nowhere-else) / [D-022](#d-022--the-librarys-scrim-is-a-theme-invariant-literal-while-the-corpus-publishes-a-theme-aware-one) — with one difference that matters: **there the stale artifact was a frozen HTML file; here it is an `Accepted` ADR with two closed human gates.** The register's precedents do not reach that far, which is why this is raised rather than assumed.

**A second question rides with it, and it has no answer anywhere.** `TextStyle.color` is a single value. The frozen presets are **three-colour recipes** (`Warm zine` = `#B0503F` · `#E7B53C` · `#F1E9D6`), and the prototype resolves that by applying `PRESETS[i][1][1]` — *the second colour* (`:478`). A palette of three landing as "the second one" reads like prototype convenience rather than design; if it is design, it should be stated, and if it is not, the product needs a rule.

**Consequence of leaving it.** Neither reading is expensive to build and both are expensive to change afterwards: the ink row is a control with goldens, mutations, an announcement path and an AA gate attached to whichever set it offers.

**Owner decision requested.** (a) The frozen H4 palette wins and ADR-055 Decision 6's five-ink mapping is **superseded** for V2 — with a stated position on whether any AA floor applies to in-page text, since the frozen swatches are not tuned for it. (b) ADR-055 wins and the popover offers only the five for **text**, the full nineteen for decor — which departs from the frozen file at a visible control and needs the D-022 treatment. (c) Something narrower. And, either way: what does a three-colour preset do to one element?

---

#### Gate evidence, measured 2026-08-05 at C6's pre-implementation gate {#d-028-evidence}

Added before the ruling, not to steer it, but because three things assumed above turn out to be
**false or unstated**, and the owner should not be asked to rule on a description of the conflict that
is wrong. Citations re-anchored to the current frozen file per [D-046](#d-046)/[OD-18](#d-046-ruling): this
defect's own `:404-407` and `:410` resolve to **`:562-565`** and **`:568`**, and `:476-483` to **`:655-658`**.

> **The first draft of this block explained that drift as *"stale by ~+93 after seven amendments"*, and the
> independent planning review disproved it twice.** The offsets are **not one number** — C6's sixteen
> ADR-089 addresses drift by **+93, +133, +138 and +160** — and the amendments are **not the cause**: at the
> DESIGN FREEZE commit `4494e95`, before any amendment existed, `:250` was already `.caption .state` and
> `:213` was already `.chips`. These citations never resolved against this file. The full per-row
> re-anchoring is [ADR-096 §1](../DECISIONS.md#adr-096-drift). *An approximate offset is not a
> re-anchoring; it is a guess that happens to be checkable.*

**1. The two palettes are not a subset and a superset. They are disjoint, and two names collide at
different values.** The shipped five (`TypeBar.kt:86-92`) against the frozen `INKS` (`:562`):

| | shipped ADR-055 | frozen Bench | same? |
|---|---|---|---|
| `Ink` | `#23201C` | `#2A251E` | **no** |
| `Ochre` | `#7A5E12` | `#D19A3C` | **no** |
| `Coral` `#A63C22` · `Teal` `#2A9D8F` · `Blue` `#264653` | shipped | **absent from all nineteen** | — |

So option (a) is not *"widen five to nineteen"* — it **replaces** every ink a user can currently pick,
and re-points two names at different colours. A document already saved carries the old value; under (a)
its ink would not be selectable in the new control, and `.sw2.sel` would show **nothing selected** on a
document the user coloured themselves. That is survivable only because [OD-11](#d-034-ruling) keeps the
Type bar reachable — the old five stay pickable *somewhere* — and it is worth the owner knowing that the
mitigation is an accident of OD-11 rather than a designed migration path.

**2. The AA premise is not true of either set, and is least true of the frozen one.** Contrast against
paper-white, which is what `Background.None` (`Document.kt:36`, the shipped default) actually prints:

- **Frozen nineteen: 13 of 18 distinct swatches fall below AA 4.5**, and **10 fall below even 3.0:1**. All
  five paper tints are effectively invisible as text — `Cream #F1E9D6` measures **1.21:1**, `Sky` 1.24,
  `Sage` 1.25, `Blush` 1.30, `Kraft` 1.47 — and `Fog #B7AD93` 2.23, `Aqua #57B0A9` 2.56. Only `Ink`,
  `Slate`, `Forest`, `Brick` and `Plum` clear AA.
- **Shipped five: 4 of 5 clear AA** — but **`Teal #2A9D8F` measures 3.32:1**, so ADR-055's own claim that
  the five are *"AA-tuned for legibility"* is **not literally true** of the set it ships. The floor is
  already not absolute, which weakens the strongest argument for (b) without settling anything.

> **The count above was first written as *"9 below 3:1"*, and the independent planning review found the
> arithmetic wrong against my own output: `Aqua #57B0A9` at **2.56:1** was omitted. It is **10**.

**2a. The floor is not a question — the owner already set one, and the frozen set fails it.** The only
contrast floor this product has ruled is
[D-002](#d-002--two-frozen-cover-inks-put-their-titles-below-aa-for-normal-text) (2026-07-30): *"The
governing floor for cover titles is **3.0:1**. No frozen colours change."* That ruling was about **cover
titles on cover fills**, not in-page text on paper, so it does not transfer by itself — but it is the
owner's stated view of what "legible enough" means in this product, and read against it the framing is
not *"13 miss AA"* but ***"10 of 18 miss the floor this owner has already chosen once."*** Option (a)
therefore either extends D-002's 3.0:1 to in-page text — in which case ten frozen swatches are offered
knowing they fail it — or states a different floor for text, or states that none applies. Silence is the
one answer that would leave the AA gate with nothing to assert against.

**2b. Under the freeze's own preset rule, all three presets apply an ink below 3.0:1.** The prototype
applies `PRESETS[i][1][1]` — the **second** colour (`:658`) — and measured against paper-white that is:

| preset | applied `[1]` | contrast | its `[0]`, unused | contrast |
|---|---|---|---|---|
| Two-colour | `#E27F89` | **2.75:1** | `#2A251E` | 15.20:1 |
| Warm zine | `#E7B53C` | **1.90:1** | `#B0503F` | 5.17:1 |
| Cool zine | `#57B0A9` | **2.56:1** | `#3E5E3A` | 7.32:1 |

Every preset's **first** colour clears AA and every preset's **second** fails 3.0:1. So the "which of the
three colours" question in the paragraph above is not a tie-breaking detail: the rule the prototype
happens to implement is the one that produces the *least* legible result available in all three cases.
That is the strongest evidence yet that `[1]` is prototype convenience rather than design — but it stays
the owner's call, because reading it as convenience and switching to `[0]` would change what a frozen
control visibly does.

**3. The conflict is narrower than this defect states: it is text-only, and the freeze agrees.** The
paragraph above reads `:410`/`toolsFor` as giving an image the `Ink` verb. Re-read on the current file,
`toolsFor` has three branches (`:567-571`): text gets `Ink` (`:568`), **photo does not** (`:569` —
`Reframe · Replace · Delete`), and the `Replace · Ink · Delete` set at `:570` is the **decor** fallback,
which [OD-2](../DECISIONS.md#adr-089) re-seated beyond Phase C and which `benchVerbKindOf` refuses
outright (`BenchContextBar.kt:118`). The shipped photo verbs match the freeze exactly. **No image ink is
at stake in C6**, and option (b)'s *"the full nineteen for decor"* describes a surface Phase C cannot
reach.

**4. Persistence and export constrain nothing.** `TextStyle.color` is a free `ColorRgba` of four ints
(`Document.kt:114-130`), serialized as such; there is no enum, no palette table and no id in the
document. Any of the nineteen is already storable today, and
[`SharedTextLayout.kt:46`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/SharedTextLayout.kt)
(`color = style.color.toArgb()`) →
[`CanvasReplayer.kt:85`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/CanvasReplayer.kt)
(`fillPaint.color = command.color.toArgb()`) is the single engine ADR-028 fixes for preview, export and read. **Neither option needs a
migration, and neither can produce an export that differs from what the editor showed.** The decision is
therefore purely about *what the control offers*, which is the cleanest possible form of it.

---

#### ✅ OWNER RULING — 2026-08-05 (OD-24): option (c), and the freeze's own labels carry it {#d-028-ruling}

> **Option (c).** The frozen bands win, with `Paper tints` fenced for a text target; presets apply `[0]`;
> no contrast floor is imposed on in-page text ink.

**What the gate had missed, and what changed the answer.** The measurement above framed this as a contrast
problem, and it is not one. Three things in the frozen file, read together, decode the control:

1. The band's own label is **`Paper tints`** (`v2-bench.html:688`), and the popover's caption calls the whole
   thing *"the maker palette — **riso spot-inks**, named, in bands"* (`:698`).
2. `applyInk` sets `t.style.color` (`:701`) — text only. So binding `applyInk` to **every** `.sw2` in all three
   bands (`:693`) is *one handler over one node list*, not a design statement.
3. **Two of the three presets end in a paper tint.** `Warm zine` = `#B0503F` · `#E7B53C` · **`#F1E9D6` =
   Cream**; `Cool zine` = `#3E5E3A` · `#57B0A9` · **`#DDE9EE` = Sky**. A preset is **[ink, accent, paper]** —
   which is the same file saying, in a third place, that tints are paper.

**1. The ink target selects the bands.** `openInk()` reads `kindOf(selNode)` (`:688`); a **text** target is offered
**Inks + Neutrals** — 13 distinct swatches — and the `Paper tints` band is **not drawn** for it. Any other
target keeps all three. `TINTS`, `bandHTML` and the band's entire visual language stay frozen and intact, and
the band returns the moment a paper target exists. **Nothing in Phase C has one** ([OD-2](../DECISIONS.md#adr-089)
re-seated `.decor` and the tray), so C6 draws **two** swatch bands.

This is [OD-21](#d-047-ruling)'s shape exactly, and the ruling adopts its words: **a fence reassignment, not a
capability reassignment.** Withholding a band is a departure from the freeze at a visible control, which is
[D-022](#d-022)'s class — so it is amended in the specification **first**, and this is the file's **eighth**
amendment.

**2. Presets apply `[0]`, the primary ink.** `PRESETS[i][1][1]||[0]` → `PRESETS[i][1][0]` (`:696`). Measured on
paper-white, `[1]` is the **least legible colour available in all three recipes** — 2.75:1, 1.90:1, 2.56:1 —
while every `[0]` clears AA at 15.20, 5.17 and 7.32. A rule that reliably picks the worst of three is
convenience, not design; and now that `[2]` is known to be the paper, `[1]` is known to be the accent.

**3. No contrast floor is imposed on in-page text ink — deliberately, and it is recorded so the absence is not
read later as an oversight.** A riso palette that clears AA is not a riso palette: `Strawberry` 2.75:1 and
`Sunflower` 1.90:1 **are** the medium. And contrast is a property of the **pairing**, not of the swatch, on a
page whose paper the maker may later change — so a floor applied to a colour would be measuring the wrong
thing. With the tints fenced, the darkest remaining risk is `Fog #B7AD93` at 2.23:1, which is a light mark
rather than an invisible one. [D-002](#d-002--two-frozen-cover-inks-put-their-titles-below-aa-for-normal-text)'s
**3.0:1** stays where it was ruled, on cover titles. **C6's AA gate asserts nothing on this control, on
purpose.**

**4. What this does to [ADR-055](../DECISIONS.md#adr-055) Decision 6.** Its **exclusivity** — *"the two … must
not be conflated"* — is **superseded** for V2: the V2 text ink comes from the frozen bands. Its **five values
are not deleted and nothing migrates**: `TextStyle.color` is a free `ColorRgba` (`Document.kt:114-121`), so
every existing document renders unchanged, and [OD-11](#d-034-ruling) keeps the shipped Type bar — and its
`Coral` / `Teal` / `Blue` — reachable. Recorded plainly: the fact that no user loses a colour they already
applied is a **consequence of OD-11**, not of a designed migration path.

**What the ruling does not do.** It does not give paper an ink target (that is the phase that takes it), does
not touch the `.decor` branch [OD-2](../DECISIONS.md#adr-089) re-seated, does not alter `.inkuse`'s copy or its
live count, and does not move `Preview`.


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
| **Status** | ✅ **RESOLVED 2026-08-01** by owner ruling (OD-9) — see [the ruling](#d-031-ruling) |

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

#### The ruling — owner, 2026-08-01 (OD-9) {#d-031-ruling}

> The frozen Bench intentionally specifies the **editing surface, not the complete application flow.**
>
> - **Font and Size are contextual editing affordances only.** They shall remain **visually present** in Phase C where specified. They shall **not invent functionality** beyond what the repository already supports.
> - **Read** continues to use the existing Editor → Proof hand-off already established by [ADR-086](../DECISIONS.md#adr-086).
> - **Back** continues to use the existing navigation stack.
> - **Redo** continues to reuse the existing editor redo behaviour.
> - **No new navigation architecture. No new editor workflow. No new feature.**
>
> If implementing any frozen affordance would require capability that does not already exist in the repository, **document that explicitly and reuse the existing behaviour rather than inventing a new one.**

**What this settles, entry by entry.**

| | Ruled | Consequence for Phase C |
|---|---|---|
| **Font** | present, no invented capability | [ADR-055](../DECISIONS.md#adr-055) excludes font choice and [ADR-028](../DECISIONS.md#adr-028) renders one bundled family, so **there is no existing behaviour to reuse.** The verb is drawn as frozen and recorded as **specified-but-unreachable** — the disposition [D-030](#d-030)'s 1→32 page morph already carries. What it must *look* like when it cannot act is the one thing this ruling does not reach; see [D-034](#d-034) |
| **Size** | present, reuse what exists | ADR-055's shipped **Type bar owns size** (the coalesced stepper over `TypeSizesPt`). Size routes there. No new surface |
| **Read** | existing hand-off | [ADR-086](../DECISIONS.md#adr-086)'s Editor → Proof route, unchanged |
| **Back** | existing stack | [ADR-051](../DECISIONS.md#adr-051)'s loss-safe back, unchanged. No new appearance is invented for it |
| **Redo** | **kept** | The shipped `EditorSupplyTray` redo stays. The freeze's three-control bar is therefore **not** an exhaustive set — recorded here so C4 does not read `Undo · Add · Done` as a removal instruction |

**Redo is the load-bearing half.** The entry framed it as *kept vs removed with the freeze*, and the ruling keeps it: the frozen bar specifies the editing surface, not the product's full capability, so a control the freeze omits is not thereby deleted. That sentence is what makes the rest coherent — it is the same reasoning that lets Font stay drawn without acquiring a handler.

**One thing the ruling does not reach, raised as [D-034](#d-034).** It settles what the *verbs* mean. It does not settle what becomes of the **eight discrete transform controls the shipped `EditorContextBar` already carries** — the surface [ADR-089](../DECISIONS.md#adr-089) row 2.10 names as C2's re-skin target, and which exists to satisfy **WCAG 2.5.7**. Applying this ruling faithfully — *no new feature, reuse what exists* — is precisely what surfaces the conflict.

---

### D-034 — the frozen contextual bar and the shipped one are different controls, and the shipped one is an accessibility conformance path {#d-034}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:190-196`, `:429-441` · [`EditorContextBar.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorContextBar.kt) · [ADR-029](../DECISIONS.md#adr-029) §6 · [ADR-053](../DECISIONS.md#adr-053) §5 · [ADR-089](../DECISIONS.md#adr-089) row 2.10 |
| **Found** | 2026-08-01, during **C2's pre-implementation blocker check**, immediately after [OD-9](#d-031-ruling) |
| **Severity** | **Transcribing the freeze would remove a WCAG 2.5.7 conformance path** — fenced C2's rows 2.10–2.13 |
| **Status** | ✅ **RESOLVED 2026-08-02** by owner ruling (**OD-11**) — option **(b) keep both**, plus the review's option (e). See [the ruling](#d-034-ruling) |

**Two controls, one slot, different contents.** The frozen `.ctx` is a **verb** bar — `toolsFor()` at `:429-432` gives text **Edit · Font · Size · Ink · Delete**, photo **Reframe · Replace · Delete**. The shipped `EditorContextBar` is a **transform** bar: nudge ×4, scale ×2, rotate ×2, the Type-bar toggle, delete. Both appear on selection; both sit at the bottom. `Delete` is the only control they share.

**The shipped one is not decoration.** Its own contract says what it is:

> *"The visible single-pointer transform controls (**ADR-029 §6, WCAG 2.5.7**) — the on-screen twin of the gesture layer … each control is a ≥48dp `IconButton` that dispatches the **same** reducer intent the gesture commit and the per-element custom actions use, so the touch, a11y-action, and visible-button paths are one code path."*

[ADR-053](../DECISIONS.md#adr-053) §5 states the same principle for Reframe: *"Discrete controls are authoritative; gestures are enhancements (accessibility floor)."*

**Why the per-element custom actions do not cover it.** `ElementSemanticsLayer` does publish move/resize/rotate as `customActions`, so a **screen-reader** user keeps a path. WCAG 2.5.7 (*Dragging Movements*, AA) is not about screen readers: it requires a **single-pointer alternative** to anything achievable by dragging, for a user who can tap but cannot drag and who is **not** running assistive technology. Custom accessibility actions are exposed only through AT, so they do not satisfy it. Remove the visible bar and move/resize/rotate become drag-only for that user.

**Why this needs the owner and not the implementer.** Every available answer is a decision with a cost, and none of them is a re-skin:

| | What it does | What it costs |
|---|---|---|
| **(a) Transcribe the freeze** | `.ctx` becomes the verb bar; the transform controls go | drops a stated WCAG 2.5.7 conformance path, and contradicts ADR-029 §6 and ADR-053 §5 |
| **(b) Keep both** | the verb bar plus a transform surface | adds a control the frozen design does not contain — the same "not parity either" objection [D-031](#d-031) raised against keeping redo, which OD-9 nonetheless resolved in favour of keeping |
| **(c) Re-seat the verb bar** | `.ctx` waits; the transform bar stands for Phase C | C2 loses its headline surface, and rows 2.10–2.13 go with it |
| **(d) Merge them** | one bar carrying verbs **and** transforms | invents a composition the freeze does not specify — the thing OD-2 and OD-9 both forbid |

**And a fifth, which is about *scope* rather than the answer — raised by the review of this entry.** This defect fences ADR-089 rows **2.10–2.13**, not package C2. The rest of C2's frozen selectors — `.el*`, `.sel`, `.handle*`, `.content.focusing`, `@keyframes mat` — are the selection outline, the four handles, the dim and the materialise, and none of them depends on what the toolbar contains.

| | What it does | What it costs |
|---|---|---|
| **(e) Split the package** | **C2a** — selection, handles, dim, materialise — proceeds now; **C2b** — the `.ctx` bar — waits for (a)–(d) | nothing but a package letter. The precedent is one entry away: OD-10's D-032 half **fenced a single row of C1's table rather than C1**, and C1 shipped |

Option (e) is orthogonal — the owner may take it *and* any of (a)–(d).

### D-044 — the frozen style chips display values they cannot change {#d-044}

> ## ✅ OWNER RULING — **OD-17**, 2026-08-04 {#d-044-ruling}
>
> *"D-044 — Approved. I acknowledge that the frozen Bench received two amendments in the same edit. Treat D-044 as an
> explicit **owner-approved companion amendment** to OD-16. Record it exactly as a separate amendment rather than as
> an inferred consequence of OD-16. No further implementation changes are required for D-044."*
>
> **What this settles, and it is not a formality.** The independent review of the reopened C3 flagged that a
> **design-frozen** document received **two** changes inside a single edit made under a ruling scoped to one — OD-16's
> clamp and this entry's chip relabelling — and asked for an owner nod rather than an implementer's inference. The
> nod is given, and the two amendments are **separately owned from here on**: OD-16 owns `EDIT_PAN_MAX`/`editPanPx()`
> and the `edit()` call site; **OD-17 owns the chip labels at `v2-bench.html:408-410`**. Neither is a consequence of
> the other, and a future reader undoing one must not assume it carries the other with it.
>
> The implementation was already correct and is unchanged by this ruling — it landed in C3 before the ruling existed,
> which is exactly why the ruling was owed.

**Raised during C3 implementation, 2026-08-03. A ✎ canonical amendment, ✅ ruled OD-17 on 2026-08-04** — it is filed here
because the HTML-first rule requires the *specification* to change before the Compose does, and C3 shipped the
deviation without recording it anywhere. [ADR-093](../DECISIONS.md#adr-093) row 3.6 carries the ruling; this entry is
the amendment owed to `v2-bench.html`.

**What the freeze draws.** `.styletb`'s three inert chips are **value displays**: `Fraunces` (a family name), `A 23`
(a size readout) and a `.sw` colour dot (`v2-bench.html:408-410`). None of the three has a handler
([ADR-093 §3](../DECISIONS.md#adr-093)), so all three ship inert under [OD-9](#d-031-ruling).

**Why the values cannot be transcribed.** OD-9's clause is *a control the freeze draws stays drawn and **invents
nothing***. A value display is not neutral the way a dimmed verb is: it makes a claim about the user's artifact. In a
prototype with one hard-coded specimen, `Fraunces` and `A 23` are true. In the product they would be **false for every
element that is neither** — and a chip that says `A 23` over 14pt text is a worse defect than a chip whose vocabulary
differs from the freeze's, because the user has no way to know it is lying.

The chips therefore carry the **verb** labels the context bar already uses for the same three controls
(`Copy.BenchVerbs.FONT/SIZE/INK`) — inventing no capability, claiming nothing false, and matching the wording the
Bench already speaks. **The swatch is the exception and stays a value**: row 3.9 requires it to seed from the
element's own computed colour (`:553`), which is a true report, so a coral heading shows coral.

**Status: ✅ amendment MADE, 2026-08-03.** `v2-bench.html:408` now reads `Font` / `Size`, with the reasoning inline,
and the `.sw` swatch is unchanged. This entry is the record of why.

An earlier cut of this entry filed the amendment and left the frozen file untouched — which inverts the HTML-first
rule, since [CLAUDE.md](../../CLAUDE.md) requires the specification to change *first* and the Compose to follow. The
independent re-review caught it. Filing is not amending, and the precedent here is that the frozen file gets edited
([D-033](#d-033) did, and so did [D-035](#d-035)).

### D-043 — the frozen page pan lifts a top-of-page element out of view {#d-043}

**Found during C3 implementation, 2026-08-03, by the implementation itself — not by a review and not by inspection.**
[ADR-093](../DECISIONS.md#adr-093) row **3.1a** predicted a failure of exactly this class before any code was written;
what it predicted was under-clearance (*"a literal pass can ship a page that still hides the caret"*). The failure is
real and the direction is the other one: the pan **over**-lifts.

**The frozen mechanic.** `edit()` sets `pageWrap.style.transform='translateY(-96px)'` (`v2-bench.html:551`), settled
back by `endEdit()` (`:558`). `.pageWrap` is `position:absolute;inset:0` inside the canvas area, and the page it
carries is `229×324` inside a `344×744` phone (`:187`, `:202-205`). There is a **band of empty canvas above the
page** for the lift to consume.

**What the product does instead.** The Bench canvas *contains* the page — `scale = min(w/pageW, h/pageH)`, and on a
portrait page the binding dimension is height. The page therefore fills the canvas vertically and the slack above it is
near zero. A −96dp lift has nothing to spend and takes it out of the page itself.

| | frozen prototype | shipped Bench |
|---|---|---|
| page height | 324px inside a 744px phone | fills the canvas (measured **437dp** on `SM-A176B`) |
| slack above the page | substantial | ≈ 0 |
| what −96 consumes | the empty band | **the top 96dp of screen — `96 / scale` pt of page: ≈ 73 % of it on the 300×400dp test host, ≈ 22 % on the measured device canvas** |
| element at the page top | still visible, lifted | **off the top of the canvas** |

**The evidence, which is a test and not an argument.** `EditorScreenTest.the_frozen_pan_lifts_a_top_of_page_element_out_of_view`
places a box at page `(20, 20)` and opens a session: the editing surface **exists and is not displayed**.
*(That test still exists and still places the same box on the same host. Since OD-16 it is named
`a_top_of_page_element_stays_in_view_while_it_is_edited` and asserts `assertIsDisplayed` — the inversion **is** the
record of the remedy, kept in place rather than deleted. The paragraphs in this entry describe the build that had
the defect and are left as written; the ruling block above says what replaced it.)* Neutralising
the pan constant makes the two ordinary session tests pass again — which is how the cause was established rather than
guessed. On that host (100pt page, scale ≈ 1.32 dp/pt) the 96dp lift removes the top **≈ 73 %** of the page from view,
which is why every other C3 test now places its box at page-space **y ≥ 76pt**: anything higher passes
`assertIsDisplayed` on a sliver or not at all, and an assertion that survives on a sliver is decoration. The
screen-level golden `bench_editing_state_light.png` is captured on a **360×720dp** host for the same reason — the
first cut used 300×400dp, where the edited element was panned out of frame and the golden pictured an editing state
with nothing being edited, while still passing.

**Why it ships anyway.** Row 3.1's assertion *is* the literal −96, its mutation is `−96 → −48`, and row 3.1a says in
terms that the freeze's number *"is transcribable and will be transcribed"* and that the remedy on failure is the
documented fallback, **not an invention by the implementer**. Correcting it silently would also be the one thing a
frozen property table exists to prevent.

**What is owed — a choice among four, none of which C3 may make alone.**

| | what it does | cost |
|---|---|---|
| **(a) keep −96 literally** | maximum fidelity to the freeze | an element in the top 96dp cannot be seen while it is edited |
| **(b) clamp the pan** — lift by `min(96dp, slack + what the element needs)` | never hides the edited element | the constant stops being a constant; the freeze specifies a distance, not a rule |
| **(c) pan only when the element would be occluded** | no lift when none is needed | same objection, plus a second state to verify |
| **(d) the documented bottom-sheet fallback** ([§E.6](V2-BENCH-REVIEW.md)) | a known-good shape, already written down | abandons in-place editing, which is the whole of C3 |

**Recommendation: (b) or (c), and the choice belongs with the device passes.** Both are *"the frozen number, applied
where the frozen geometry differs"* rather than a redesign, and row 3.1a already frames the clearance as the property
the number exists to serve. But the same paragraph forbids the implementer choosing, and Pass 1 can measure the real
clearance on the real page instead of arguing about it. ~~**C3 ships (a).**~~ — superseded by the ruling below.

#### D-043 — owner ruling, 2026-08-03 {#d-043-ruling}

**OD-16. Option (b), the clamp, approved.** The ruling in the owner's own words:

> *"The frozen Bench is amended so that the rigid −96px translation is no longer an unconditional literal. It becomes
> the maximum translation, clamped by the available slack above the page plus the minimum clearance required to keep
> the edited content visible. Treat this as a frozen-spec amendment, not an implementation shortcut. Also create and
> implement D-045 in the same package… The clamp and the clipping correction must land together so that neither fix
> makes the other defect worse."*

**What the device measured, and why (a)'s stated cost never appeared.** On `SM-A176B` (411×891dp, 2.625 px/dp) the
page's top sits at 227px against a canvas top of 216px — **11px = 4.2dp of real slack against a 96dp demand**, which
confirms this entry's *slack ≈ 0* premise on hardware. *(This is the **pre-amendment** round's reading. Re-measured
on the amended build, the same device reports the paper beginning at `y226` against a canvas top of `y226` —
**slack 0 within a pixel** ([ADR-093 §8](../DECISIONS.md#adr-093-device)). Both are kept and neither is averaged:
between them they bracket the slack on this device at **0–4dp against a 96dp demand**, which is the premise OD-16
was ruled on and is unaffected by which end of that range is exact.)* But the predicted symptom — *"an element in the top 96dp cannot
be seen while it is edited"* — **did not reproduce**. The element stayed visible, and Pass 1 found the reason: the
canvas does not clip, so the over-lifted page was *painted over the top bar* rather than cut off. `Preview ›` was
invisible and, read off the platform `AccessibilityNodeInfo` tree, still `clickable=true` at its full bounds. The
defect was being **masked by a second defect** — which is [D-045](#d-045), and why the two land together.

**What the amendment is.** `edit()` lifts by `min(96, slack + clearance)`; `endEdit()` is untouched and still settles
back `translateY(0)`. **The prototype's own motion changes, and an earlier draft of this paragraph denied it.**
Computed from the file's own CSS: the `.canvasArea` is ~486px tall around a 324px `.page`, so the slack above the
page is ~81px — *less* than 96. Editing the **title** therefore lifts `min(96, 81 + 0) ≈ 81px`, fifteen pixels
short of the old literal; editing the **body** text, which sits low enough for the docked `.kbstack` to occlude it,
returns to the full 96px ceiling. Claiming *byte-for-byte identical* was wrong twice over — wrong about the
arithmetic, and wrong in kind, because a spec amendment that quietly changes the specimen is the exact thing this
register exists to catch. The frozen file was amended first, per the HTML-first rule, and carries the full
derivation in its header (`v2-bench.html`, `AMENDED 2026-08-03`) and at the site.

**Two consequences are recorded rather than fixed, because the ruling's word is *maximum*.**

1. An element deep at the **page bottom** can need more lift than 96dp. Measured: 43dp of the edited box stays behind
   the docked `.kbstack`. The **typed line clears it by 23dp**, which is the property row 3.1a exists to serve, so
   this is a priced cost and not a reopening.
2. When clearance genuinely demands the full 96dp the page's top still leaves the canvas — now **clipped** by D-045
   rather than painted over the chrome.

**Where the passes disagreed, recorded rather than averaged.** Pass 1 read the old build as faithful (the pan was
96.00dp to the pixel, and the return's residue — asserted then, **measured only afterwards** on the retained
screencaps — is 1408 differing pixels between the pre-session and post-session rest frames, every one of them
inside the system clock strip at `y39–73` and **zero below `y110`**, i.e. none of them the artifact); Pass 2 read
the same motion as *"the paper slid under the clock"* — a malfunction. Both were right: the transcription was exact and its consequence was wrong. That
disagreement **is** the finding, and it is what the amendment answers.

### D-045 — the Bench canvas never clipped to its own bounds {#d-045}

**Found on hardware during the OD-16 device evidence, 2026-08-03, and created by the same ruling that closed D-043.**

**This is a parity defect, not a spec amendment.** The frozen `.canvasArea` has said `overflow:hidden`
(`v2-bench.html:171`) since the freeze; the Compose host simply never implemented it. Nothing in the specification
changes — which is why D-045 has no `AMENDED` block of its own.

**Why it stayed invisible until C3.** With the pan pinned at zero the canvas had nothing to overflow with. C3 made the
pan real, and on `SM-A176B` the panned sheet's top went to **y −25px**: the paper painted over the top app bar and up
under the status bar in **every** editing session, at every element position. Measured rather than described — the
pixel at `(900,170)`, inside the `Preview ›` button's own reported bounds, was `#F7F2E8` paper and not `#322D25`
chrome.

**The defect is the accessibility one, not the cosmetic one.** `Preview ›` kept its full platform bounds and
`clickable=true` while being completely covered. A control the user cannot see and can still press is a worse fault
than paper in the wrong place; the paper is only how you notice it.

**Why it cannot ship alone, in either direction.** Clipping without the clamp leaves the paper on the top bar whenever
clearance needs the full 96dp. Clamping without the clip would have converted D-043 from a defect you can *see* into
one you cannot — the over-lifted element cut off instead of drawn on the chrome. The owner's ruling requires both in
one package for exactly this reason.

**Status: ✅ FIXED in C3, 2026-08-03.** `EditorScreen.kt`'s canvas carries `clipToBounds()`.
[ADR-093](../DECISIONS.md#adr-093) row 3.14 owns it. **Post-fix, measured the same way as the defect** — a screencap
of an open session on the amended build, probed **inside `Preview ›`'s own reported bounds**: `(950,53)` is
`(231,113,83)`, the coral label, and `(700,53)` is `(50,45,37)`, the bar's chrome. Neither is paper. *(The first
post-fix reading offered here probed `(900,170)` again on a **top-of-page** element, where the amended pan is `0`
and nothing overflows in the first place — a green result that would have been green with the clip deleted. The
independent review caught it; the numbers above replace it.)*

⚠ **Its unit assertion is deliberately narrower than the defect.** `BenchC3Test.the_canvas_clips_whatever_leaves_it`
proves the clip with a selected element's resize handles overflowing the canvas *downward*, because the case that
actually mattered on hardware — the panned page overflowing *upward* — needs the pan to exceed the slack, which needs
the clearance term, which needs an **IME**, which Robolectric does not have. The upward case is
[device checklist](../DECISIONS.md#adr-093-device-checklist) item 10 and is verified there, not here.

### D-047 — the frozen bar has three slots; the rulings that govern it require five {#d-047}

> ## ✅ OWNER RULING — **OD-21**, 2026-08-04 {#d-047-ruling}
>
> **Option A. The frozen Bench is amended.**
>
> > The bottom bar becomes **`Undo · Redo · Add · Done`**. The `Add` control opens the frozen Add chooser.
> > **Only the Text and Photo rows are released into C4.** The Art row remains fenced behind C8 exactly as
> > OD-2 already requires. **This is a fence reassignment, not a capability reassignment.** The chooser
> > continues to use the existing `ZSheet` implementation. The Text action must continue to reuse the existing
> > `addTextAndEdit` flow so the C3 in-place editing model remains unchanged. Redo remains in the bottom bar.
> > `EditorSupplyTray` is retired as planned.
>
> **What was amended, and in which order.** The frozen Bench first, per the HTML-first rule that has governed
> every amendment in this programme. `v2-bench.html` gains a header block recording the ruling and **one line
> of markup** — `#redoBtn` at [`:466`](mockups/v2-bench.html), a second `.icon-btn` drawn immediately after
> `#undoBtn` and, like it, `disabled` at rest, because this prototype has no redo stack and
> [OD-9](#d-031-ruling)'s own formula is that a control the freeze draws **invents nothing**. **No CSS
> changed:** `.icon-btn` (`:268`) already sizes it, `.add` (`:271`) still takes the residual width, and `.bar`
> (`:267`) is a flex row whose height and gap are unaffected. Geometry checked rather than assumed —
> `44×3 + 10×3 + 32` padding = **194px** fixed, leaving `.add` ≈ 217dp on the 411dp device this
> programme measures on.
>
> **What was *not* amended.** The three `.opt` rows at `:720-722` stand exactly as frozen, **Art included**.
> What changed is only which package may *build* which row — which is what the ruling means by a fence
> reassignment. Art stays behind C8 and behind [V2-BENCH-REVIEW §E.6](V2-BENCH-REVIEW.md)'s legal pass.
>
> **The cost this ruling knowingly accepts**, recorded because a package should not discover its own price
> later: both add verbs now sit one tap deeper than they do today. `Add photo` and `Add words` are two direct
> controls on the shipped shelf; after C4 they are two rows behind one `Add`. Nothing is removed —
> [OD-11](#d-034-ruling) holds — but the shortest path to each grows by one tap, and that is a real change
> to the editor's most common action. It is the price of the freeze's own three-slot bar, and the ruling pays
> it deliberately.
>
> **The amendment moved every address below it** — `+23` above the bar's markup, `+24` below — which is
> [D-046](#d-046)'s condition again, now from a second cause. C4 re-anchored **its own** citations and verified
> each against the selector it names; per [OD-18](#d-046-ruling) it did **not** sweep the files it does not
> own, and the drift those files now carry is D-046's, not C4's.


**Raised 2026-08-04 by C4's mandatory pre-implementation blocker check. ⛔ Blocks C4.**

**What the artifact says.** `v2-bench.html:464-468` draws `.bar` as exactly three controls in a fixed
geometry: `.icon-btn` Undo (44×44, `disabled` at rest), `.add` (`flex:1`, the residual width), `.icon-btn`
Done (44×44) — `:267` gives the row `height:66px`, `gap:10px`, `padding:0 16px 4px`. `.add`'s handler is
`$('addBtn').onclick = openSupply()` (`:762`).

**Why that cannot be transcribed.** Three rulings already in hand each add something the three slots do not
hold, and none of them can be satisfied by deleting another:

1. **[OD-9](#d-031-ruling) keeps redo.** ADR-089 row 4.5 states the frozen file contains no Redo anywhere and
   the ruling answers it directly: *"redo is kept."* The shipped control is `EditorSupplyTray`'s fourth card
   (`EditorSupplyTray.kt:147-157`). That is a **fourth** control the bar must carry, or a second surface that
   must survive beside it.
2. **[OD-11](#d-034-ruling) / [OD-14](#d-039-ruling) keep both add verbs.** The shipped bottom surface is not
   one `Add`; it is `Add photo` (`Intent.RequestAddImage`) and `Add words`, two distinct capabilities on two
   distinct cards (`EditorSupplyTray.kt:118-136`), wired at `EditorScreen.kt:1173-1181`. *"No existing editor
   capability is removed"* forbids collapsing them into one button unless the one button reaches both.
3. **The frozen `Add`'s own destination is out of phase.** `$('addBtn').onclick` (`:762`) calls `openSupply()`
   (`:718-729`), which builds the `.sheet` — **C8**'s region, re-seated *beyond Phase C* by OD-2. So `Add`
   cannot be wired as drawn, and OD-9's *"the destinations reuse what ships"* has nothing single to reuse.

> **Correction, 2026-08-04, made at C4's implementation gate before any production code.** This entry's first
> draft said *"what ships is two verbs, not a chooser. No chooser exists in the repository."* The second
> sentence is true and the first is **misleading**: a chooser exists **in the freeze**, and reading it changes
> the shape of the decision. `openSupply()` (`:718-724`) builds three `.opt` rows — **Text**, **Photo**, **Art**
> — and `:729` narrates the intent in the freeze's own words: *"Add stays three verbs — Text · Photo · Art."*
> **Two of those three are exactly the two capabilities that ship** (`data-a="text"` places a text element
> *ready to edit*; `data-a="photo"` places a photo), so the frozen `Add` does **not** ask for a capability to be
> collapsed — it asks for one to be **routed**. Only the third, `Art` → `openArt()`, is genuinely out of reach:
> it is the H3 drawer, re-seated by OD-2 and fenced by [V2-BENCH-REVIEW §E.6](V2-BENCH-REVIEW.md) pending the
> legal pass that OD-8 records. What remains true, and is why this entry still blocks: **`.sheet`, `.supply` and
> `.opt` are not in C4's frozen fence** — C4 owns `.bar`, `.icon-btn*`, `.add*`, `.status`, `.saved*`, `.snack*`
> — so building the chooser at all is C8's surface arriving early, however small the two-verb version is.

**And the surface it replaces is not shaped like it.** `EditorSupplyTray` is a `Supplies`-headed shelf of four
tilted cards on `--desk` (`EditorSupplyTray.kt:90-158`), each with a glyph over a label. `.bar` is a 66px
chrome strip with no heading and no labels but one. This is a **replacement**, not a re-skin, and every card it
drops has to land somewhere the ruling permits.

**Why it is not resolvable from the rulings in hand.** The two readings the rulings leave open are materially
different products, and both are defensible:

| | (a) the bar absorbs everything | (b) the bar is additive, the shelf survives | (c) the bar as drawn, `Add` → a **two-verb** chooser |
|---|---|---|---|
| shape | `Undo · Redo · Add · Done` — four controls in a row the freeze draws with three | frozen `.bar` exactly as drawn, `EditorSupplyTray` kept beneath it (the C2b precedent, OD-11) | frozen `.bar` exactly as drawn — three slots |
| `Add` | must reach both add verbs ⇒ the freeze's own chooser, minus `Art` | unchanged: the shelf keeps both verbs | the freeze's `.sheet` at `:718-724` with the `Art` row withheld until C8 |
| redo | a fourth `.icon-btn` in the bar | stays on the shelf | **unhoused** — the freeze gives it no slot, so it needs one |
| OD-14 | satisfied — one presentation each | **violated** — Undo and Add would each be drawn twice at once | satisfied for Add; redo still needs a home |
| the freeze | amended: `.bar` gains a control | honoured literally | honoured literally, but **builds `.sheet`, which is C8's fence** |

Reading (a) requires an **amendment to the frozen Bench** — adding a control to a frozen region is a UX change,
and the HTML-first rule makes that the owner's act, as it was for [D-010](#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark),
[D-033](#d-033), [OD-12](#d-035-ruling) and [OD-16](#d-043-ruling). Reading (b) requires an owner to decide that
OD-14 does not reach this case. Reading (c) requires an owner to release a slice of **C8's fence** into C4 and to
say where redo lives. **An implementer cannot pick between them**, which is exactly the condition
[D-042](#d-042) records as the difference between a recorded deviation and an escalation.

**What is not in question.** Nothing here proposes removing a capability, and no reading does. Geometry is not
the obstacle either: four controls fit — `44·3 + 10·3 + 32 = 194px` fixed on a 411dp device leaves `.add` ample
residual width. The obstacle is *what the bar is allowed to contain*.

---

### D-048 — if the bar's `Done` becomes the Read hand-off, the hand-off is drawn twice {#d-048}

> ## ✅ RESOLVED BY RULINGS ALREADY IN HAND — 2026-08-04 {#d-048-ruling}
>
> **The owner ruled that this is not an owner decision.** *“Record it exactly as [D-042](#d-042) was
> recorded”* — a recorded deviation with its reasoning, not an escalation. No number is assigned, because
> no ruling was needed.
>
> **`Done` follows the frozen two-state behaviour at `:653`.** The states are assigned, not duplicated:
>
> | when | who owns *finish editing* | who owns *clear selection* | the bar's `Done` |
> |---|---|---|---|
> | an editing session is active | C3's style-row `#doneEdit` ([ADR-093](../DECISIONS.md#adr-093) row 3.6) | — | **withheld**, drawn at the frozen `.icon-btn:disabled` `opacity:.35` (`:269`) |
> | no session | — | **the bar's `Done`** | live |
>
> **Why this needed no amendment, in three steps.** The withholding is [OD-14](#d-039-ruling)'s **own method**,
> applied exactly as C2b applied it — *“every withheld control returns the instant the frozen bar stands
> down”* — and it uses a presentation **this file already draws** for `#undoBtn`, so nothing is invented.
> Deselect gains its **first drawn control**: [OD-13](#d-037-ruling) gave the capability a *gesture*
> (`EditorGestures.kt:62`, tap → `Intent.SelectAt`, miss reducing to `ClearSelection`), and a gesture has no
> visible presentation for OD-14 to count as a second one. It also answers the WCAG 2.5.7 argument
> [ADR-029 §6](../DECISIONS.md#adr-029) makes for `EditorContextBar`: a discrete single-pointer path to a
> capability that previously had only a gesture.
>
> **Preview does not move.** It stays exactly where it ships (`EditorScreen.kt:625-637`, wired unconditionally
> in production at `ZinelyNavHost.kt:95` and `:419`). [OD-9](#d-031-ruling)'s *reuse, don't invent* is satisfied
> by **leaving it alone** — [ADR-089](../DECISIONS.md#adr-089) row 4.6 names `EditorScreen(onPreview)` as the
> wiring to preserve, not a destination to relocate. Relocating it would have been the amendment; not
> relocating it is the reading that needs none.


**Raised 2026-08-04 by C4's mandatory pre-implementation blocker check. ⛔ Blocks C4 row 4.6.**

**What the artifact says.** `$('doneBtn').onclick` (`v2-bench.html:653`) does two things and neither is a
hand-off: if the phone is `editing` it calls `endEdit()`, otherwise it calls `deselect()`. ADR-089 row 4.6
records that the frozen Bench has *"no route to Read / the Proof, and no back affordance"*, names
`EditorScreen(onPreview)` as the surviving target, and OD-9 rules the hand-off *reuses* ADR-086's route.

**The contradiction.** OD-9 says reuse what ships. What ships is a **top-end `Preview ›` text button**
(`EditorScreen.kt:625-637`, `EditorPreviewActionTestTag`) — the same control C3's D-045 evidence measured at
`(950,53)` on hardware. If C4 also binds the bar's `Done` to `onPreview`, the Editor→Proof hand-off is
presented **twice simultaneously**, which is precisely what [OD-14](#d-039-ruling) forbids and precisely the
shape of the defect ([D-039](#d-039)) that produced it.

**But the alternative empties the control.** `doneBtn`'s two frozen jobs are both already owned and already
shipped:

- *end the edit session* — C3 built it as the style row's own `.done` chip (`v2-bench.html:433`, `#doneEdit`;
  [ADR-093](../DECISIONS.md#adr-093) row 3.6), device-verified `clickable=true enabled=true`;
- *deselect* — [OD-13](#d-037-ruling) ruled selection transient and C2a built tap-outside-to-dismiss
  ([ADR-091](../DECISIONS.md#adr-091) row 2.14), device-verified 3/3.

So transcribed literally, the bar's `Done` is a third presentation of two actions that already have one each —
OD-14 again, from the other direction. **Every reading of row 4.6 collides with OD-14**; which collision is
acceptable is a decision about what `Done` *means* on this screen, and that is the owner's.

**Not a blocker for the rest of C4.** `.status`, `.saved`, `.snack` and the soft-delete rows are untouched by
this and can be built whichever way `Done` is ruled.

---

### D-060 — the ink popover offers the same swatch twice, under one name {#d-060}

**Raised 2026-08-06 by C6's Device Verification Pass 2 on `SM-A176B` / Android 16.** 🟦 Open. **Not a merge
blocker — [ADR-096](../DECISIONS.md#adr-096) is `Accepted` with this open.** **This is a defect in the frozen
file, not in the Compose implementation** — which is precisely why it is filed here, exactly as
[D-051](#d-051) was.

**What was seen.** With a text element selected, the popover draws `Inks` (ten swatches) then `Neutrals`
(four). The **tenth ink and the first neutral are the same colour** — `#2A251E` — so the user is shown two
near-identical near-black circles, one at the end of one row and one at the start of the next. Both are
named `Ink`. To a sighted user they are indistinguishable at 26dp; to a screen-reader user the platform tree
reads `… Plum, Ink` then `Ink, Slate, …`, with nothing to tell them apart. Selecting either shows the
selection ring on **one** of them.

**The implementation is faithful, and deliberately so.** `v2-bench.html:596` puts `#2A251E` in `INKS` and
`:598` puts it in `NEUT`; [OD-24](#d-028-ruling) draws both bands for a text target. C6's independent review
raised the ring behaviour as RF-1 — the first implementation ringed *both* — and the fix makes the ring
belong to the Inks-band instance, the first match, which is a defensible reading but is a Compose choice
standing in for a spec that does not say.

**This is not a new owner decision, which is why C6 did not stop.** OD-24 §1 states the offer as
*"a **text** target is offered **Inks + Neutrals** — **13 distinct swatches**"*. Ten plus four is fourteen
drawn; thirteen distinct is the count **after** the duplicate is removed. The owner counted the duplicate,
in the ruling, and drew both bands anyway. The device measurement agrees from the other side: 14 nodes,
13 distinct values ([ADR-096 §9.1](../DECISIONS.md#adr-096-pass1)).

**What is undecided** is only whether the duplicate should be *told apart* — a rename, so that the near-black
in `Neutrals` is not also called `Ink`. That is a change to a frozen surface's copy, so it amends
`v2-bench.html` first and `Copy.BenchInk` second, and the amendment is the owner's to make. Until then the
popover is correct and mildly confusing, which is a fair description of the freeze it implements.

---

### D-059 — the miniature lost its paper, and in dark theme the page went with it {#d-059}

**Raised 2026-08-05 by C5's Device Pass 1 on `SM-A176B` / Android 16 / density 420. ⛔ OWNER DECISION OWED —
this blocks C5's acceptance and no work-around has been applied.**

**What was seen.** With the device in dark theme, a sheet in the filmstrip draws the page's *elements* — the
photo card, the words — directly on the sheet's own dark ground. The page's paper is missing, so a text
element renders dark ink on a dark sheet. Measured from the screenshot, on the front cover of a real
document carrying the words *"Page one"*:

| theme | sheet ground | the page's own text | contrast |
|---|---|---|---|
| dark | `#312C24` | `#201D18` | **1.21 : 1** |
| light | `#F7F2E7` | `#584636` | 8.02 : 1 |

Same document, same words, same build. In light theme the miniature is a picture of the page; in dark theme
it is a photo floating in a hole.

**Why it happens, and why nothing caught it.** The frozen `.pthumb{background:var(--paper)}` (`:282`) resolves
against the **room's** `--paper`, which the dark theme sets to `#2F2A22` (`:162`). The page is different: the
freeze makes `.page` a light island — `.page{--paper:#F7F2E7;--ink:#2A251E;…}` (`:222`) — the amendment
[OD-12](#d-035-ruling) produced when [D-035](#d-035) found the same failure on the canvas at 1.60:1. The thumb
never needed that island, because until [OD-22](#d-053-ruling) its interior was three faint placeholder rules
that carried no user content at all. **OD-22 changed what is inside the sheet and nobody revisited what is
underneath it.** The dark golden passes because it was recorded from this build, and no probe compares the
thumb's ground to the page's — the same shape of hole D-035 fell through on C1.

**Why this is the owner's decision and not the implementer's.** Fixing it means the thumb's interior no longer
paints the room's `--paper` — a **frozen property**, changed in a frozen file, which is a visual amendment and
not one of the four things a freeze permits after the fact. Every previous amendment (six of them) was an
owner ruling. The principle behind OD-12 plainly reaches this case; its *implementation* was scoped to
`.page`, and extending that scope is an amendment.

**Options, with what each costs.**

- **(a) Extend the light island to the thumb's interior.** The sheet's interior paints the page's paper
  (`#F7F2E7`) in both themes; the sheet's edge, spine, shadow and the row around it stay chrome. The
  miniature then matches the canvas, which is what OD-22 said it is a miniature *of*. Cost: a dark-theme
  strip of eight small light rectangles — brighter chrome than the freeze draws, and C1 already paid this
  price knowingly for the canvas. One golden re-records; a probe and a mutation guard it.
- **(b) Give only the page's *ink* the island** (dark-theme thumbs keep the dark ground, but the page's text
  renders in a light ink). Cost: the miniature stops being a faithful reduction of the page — the same
  objection that retired the placeholder rules, in a subtler form. Not recommended.
- **(c) Accept it as a limitation of the miniature at 26×34dp.** Cost: in dark theme the strip tells you
  nothing about pages whose content is text, which is most pages of most zines, and OD-22's accepted price
  was *"a smudge rather than a picture"* — not *"nothing at all"*.

**Not a blocker for anything else in C5.** Every other row is built, asserted and device-verified; this is one
paint decision on one surface. But the strip is C5's centrepiece, and shipping it half-legible in dark theme
would repeat, knowingly, the defect C1 was reopened for.

---

#### Owner ruling — OD-23 {#d-059-ruling}

**Ruled 2026-08-05. Option (a): extend the light island to the thumb's interior. The frozen Bench is amended
for the seventh time.**

**What the amendment says.** `.pthumb` becomes a light-theme island in the same manner `.page` has been one
since [OD-12](#d-035-ruling) — restated in `docs/design/mockups/v2-bench.html` immediately above the existing
`.pthumb` rule, with the amendment recorded in the file's own amendment log:

```css
.pthumb{--paper:#F7F2E7;--paper-edge:#EEE6D4;--ink:#2A251E;--ink-soft:#5B5347;--ink-faint:#8C8269;}
```

**Five tokens, not eight — and that is the whole of the ruling's precision.** `.page` restates eight;
`--matcha` and `--strawberry` are deliberately **not** carried into `.pthumb`, because on this surface they are
not the page's ink at all: the spine, the `.cur` border and the current dot are the **row's** marks *on* the
sheet, and they must read against the chrome the row is drawn in, not against the paper. The same distinction
C1 drew when it left the sheet's *shadow* to the room ([D-010](#d-010)'s lesson: an artifact that lightens its
room is a second defect, not a fix). `--frame-shadow`, `--chrome` and `--desk` are likewise untouched.

**Nothing else changes.** Sheet geometry, the 1.16 lift, the 7dp pitch, the spine, the `.cur` border, the dot,
the row's ground, the transition — all as frozen. **The page grid is *not* amended**: its cells are drawn at a
size where the room's own paper never produced the failure, and widening the amendment past the defect is how
freezes rot.

**How it was implemented.** HTML first (the amendment above), then Compose: `benchThumbIsland(room)` in
`BenchPageNav.kt` returns the room's colours with exactly those five replaced by `zinelyV2LightColors()`, and
`BenchPageThumb` provides it through `LocalZinelyV2Colors` around the rendered page while painting the sheet's
own ground and edge from it. Guarded three ways — a pure set-based assertion that the five change and the
row's marks do not (`BenchC5Test.the_sheet_takes_the_page_s_five_tokens_and_leaves_the_row_s_marks_alone`), a
threshold-free raster probe on the dark golden that the sheet's interior is `#F7F2E7` **and** the row's ground
is still chrome, and mutation **M36** (`paper = light.paper` → `paper = room.paper`).

**The accepted price, recorded.** In dark theme the filmstrip is a row of small light rectangles — brighter
chrome than the freeze draws. That is the same price C1 paid knowingly for the canvas, and it is the reason the
island stops at five tokens.

---

### D-058 — the navigation row shipped below the bar {#d-058}

**Raised 2026-08-05 by C5's independent review. ✅ FIXED the same day.**

The frozen `.phone` opens `.navrow` at `v2-bench.html:481` and `.bar` at `:488`, both in normal flow — the
filmstrip of sheets sits **above** `Undo · Redo · Add · Done`, nearer the page it navigates. C5's Column
emitted them the other way round.

**Why nothing caught it.** Every C5 test measured a row against *itself* — the nav row's own 56dp height, the
bar's own contents, the grid's bottom edge against whichever row happened to be under the canvas. A
*relation* between two rows was asserted nowhere, so the inversion was invisible to 38 focused tests, 25
mutations and five goldens. Worse, [ADR-095 §7](../DECISIONS.md#adr-095-device-checklist)'s device checklist
stated the frozen order as fact — *"sits above C4's bar"* — so the manual pass would have read the checklist,
seen the opposite on the screen, and had to decide which document was wrong.

**The fix.** `BenchPageNav` is emitted between the canvas and the bar, and the relation is now asserted
twice: geometrically in `BenchC5Test.the_navigation_row_sits_above_the_bar_as_the_freeze_stacks_them`, and as
*reading* order in CI-31's `SurfaceTraversalOrderTest`, where the sheets now precede the bar's four verbs.
The screen-level goldens (`editor_screen_light/dark`, `bench_editing_state_light`) were re-recorded and show
the frozen stack.

**The lesson, which is the general one.** A suite that only ever measures each component against its own
specification cannot see a composition defect. Both reviews of this package found their strongest finding by
reading the frozen file against the emitted tree, not by running anything.

---

### D-057 — `z-index` moved the reading order, not just the paint {#d-057}

**Raised 2026-08-05 by C5's Device Pass 1. ✅ FIXED the same day.**

`Modifier.zIndex(2f)` on the current sheet — a literal transcription of the frozen `.cur{z-index:2}`
(`v2-bench.html:288`) — reorders the children Compose publishes to the platform accessibility tree. The
current sheet was therefore the **last** traversal stop in the strip: a screen-reader user met the page they
were on after all the others. `Modifier.semantics { traversalIndex = … }` on each sheet, with
`isTraversalGroup` on the strip, did **not** override it.

**Caught by** `BenchPageNavA11yTest.every_sheet_is_one_named_traversal_stop`, which asserts the platform's
own traversal sequence, and independently by CI-31's `SurfaceTraversalOrderTest`. Neither the merged-tree
semantics assertions nor any golden could see it.

**The fix and its price.** The `zIndex` is not transcribed. That is a deviation from a frozen property, and
it is permitted in terms: *"Allowed after freeze: … accessibility improvements"* (CLAUDE.md, DESIGN FREEZE).
The price was measured from the freeze's own numbers rather than assumed — at `scale(1.16)` a 26dp sheet
overhangs 2.08dp a side into a 7dp gap, so no two sheets ever overlap and the raise protected only the
current sheet's shadow tail against a neighbour 7dp away. Reading order is a conformance path; a shadow tail
is not.

---

### D-056 — the cover treatments were dead code on every real document {#d-056}

**Raised 2026-08-05 by C5's Device Pass 1. ✅ FIXED the same day.**

C5 decided cover-ness from `PageRole`, on the strength of an [ADR-095 §3](../DECISIONS.md#adr-095-blockers)
clearance that called it *"strictly better"* than the freeze's `i===1||i===NP`. The product never assigns
those roles: `EditorBootstrap.kt:26` and `RoomProjectRepository.kt:475` build every page as
`PageRole.INTERIOR`, and `FRONT_COVER`/`BACK_COVER` appear in `src/main` only in `:core:imposition`'s
`Convention.kt:56-63`, which maps **panel** roles at print time.

So rows 5.5a (the matcha spine), 5.9's cover clause (*"(front cover)"* / *"(back)"*) and 5.14 (the
`COVER`/`BACK` badge) never fired. On device: no badge, no spine, and the platform tree read *"Page 1 of 8"*.

**Why no instrument caught it.** All three C5 test fixtures constructed pages with fabricated cover roles, so
every assertion, golden probe and mutation exercised a data shape the product cannot produce. This is the
sharpest instance yet of the rule that a test fixture is a claim about reality: the fixtures now build pages
exactly as the product does, and the reason is recorded at each of the three.

**Fixed** by `benchCoverAt(pageNumber, pageCount)` — the freeze's own rule, read by both the strip and the
grid. See [ADR-095 §3a](../DECISIONS.md#adr-095-cover-correction) for why this needed no owner ruling.

---

### D-055 — two `:app` navigation tests waited ten seconds for copy C4 had retired {#d-055}

**Raised 2026-08-04 by C5's cross-module full verification. ✅ FIXED the same day — a test defect belonging to
[C4](../DECISIONS.md#adr-094), repaired outside the C5 commit fence.**

`ZinelyNavHostTest` used *"the supply tray is up"* as its definition of **Ready**, and waited for the literal
text `"Add a photo"`. [OD-21](#d-047-ruling) retired that shelf: the frozen `.bar` draws `Undo · Redo · Add ·
Done` and names no medium, so the string stopped existing on the editor screen. Two tests —
`the single Proof surface stacks above the editor and loss-safe back returns to it` and `a fast reopen of the
just-closed project boots Ready - never the busy error` — therefore spent ten seconds waiting for a phrase that
could never arrive and failed with `ComposeTimeoutException`.

**Attributed to C4 on evidence, not on argument.** Both failures were reproduced in a clean `git worktree` at
`HEAD` — that is, with **no C5 change present at all** — before any attribution was made. The cause is visible
in `git show 026d15a`: C4 replaced the shelf and did not update these two `:app` waits.

**Fixed** by waiting on `BenchBottomBarTestTag` instead: the bar is what "Ready" actually draws, and a test tag
cannot be retired by a copy change the way the phrase it replaced was.

**The lesson is the one [ADR-094 §6.12](../DECISIONS.md#adr-094) already records, in a second costume.** That
entry was about a task Gradle skipped as up-to-date; this one is about a *module the verification command never
named*. Both produce the same false green, and the same repair: run the whole tree, with `--rerun-tasks`.

---

### D-054 — the V2 catalog goldens went stale the moment C4 added an icon {#d-054}

**Raised 2026-08-04 by C5's cross-module full verification. ✅ FIXED the same day — a golden defect belonging to
[C4](../DECISIONS.md#adr-094), repaired outside the C5 commit fence.**

C4 added the **`Redo` mark** to `ZinelyV2Icons` (`026d15a`) — the icon [OD-21](#d-047-ruling)'s fourth control
needed — and updated the icon set's own parity test, but did not re-record `:core:ui`'s catalog goldens. All
**four** are affected — `v2_catalog_icons_light/dark.png` and `v2_catalog_all_light/dark.png` — though only the
two light ones failed the verify run, because a golden is compared only by the test that captures it and the
dark pair had not been reached before the build stopped. They have depicted a 36-mark set ever since, while the
code draws 37; every mark after the insertion point shifts one cell, so the comparison fails across the whole
sheet even though exactly one glyph is new.

**It survived because nothing looked.** `verifyRoborazziDebug` is what compares a golden; `testDebugUnitTest`
merely *captures*. C4's verification ran the latter, so the mismatch sat unread from `026d15a` until a run that
named the verify task on the whole tree.

**Fixed** by re-recording all four and reading the compare images: the sole change is the `Redo` mark and the
one-cell shift it causes. (The first draft of this entry said *"two goldens"*, counting the two that failed
rather than the four that moved — corrected after independent review checked the diff against the prose.) See [D-055](#d-055) for the same class of miss, found in the same run.

---

### D-053 — the frozen page thumb is an abstraction; the shipped one is the page {#d-053}

> ## ✅ OWNER RULING — **OD-22**, 2026-08-04 {#d-053-ruling}
>
> *"Adopt Option (c). The frozen Bench is amended. The page strip continues to display live page miniatures
> rather than abstract placeholder sheets… The amendment should preserve the frozen visual language —
> thumbnail size, border, lift, animation, current-page treatment, overall navigation layout — but the interior
> of the thumbnail becomes the real rendered page. The placeholder ruled lines are removed from the frozen
> specification. The page grid remains independent and continues to follow the frozen design."*
>
> **The reasoning the ruling gives:** the frozen `.pthumb` *"was appropriate for the HTML prototype but no
> longer represents the shipped editor truthfully"*; the live miniature is *"the only place where the user can
> understand another page's contents without navigating to it"*; and replacing it with generic rules would
> *"intentionally discard useful information and repeat the same class of problem previously resolved by
> OD-17."* It names its own precedent — **OD-16, OD-17, OD-21** — and the principle they share: *when literal
> parity would reduce truthful communication, amend the frozen specification first rather than degrading the
> implementation.*
>
> **The amendment is made, and it is the sixth to a frozen V2 surface.** `v2-bench.html`: the `.pthumb i` rule
> is **deleted** (formerly `:263`) and `buildFilm()` no longer creates an `<i>` (formerly `:698`); both sites
> now carry a comment naming this ruling. Everything else about the thumb and the row around it is untouched
> and is what C5 transcribes — `26×34`, the asymmetric `1.5/3/3/1.5` radius, `--paper` on a `--paper-edge`
> hairline, the 2px spine (`--matcha` on cover and back), the contact shadow, the `.2s var(--settle)`
> transition, `.cur`'s `scale(1.16) translateY(-2px)` with its matcha border and lifted shadow, the 4px
> `--strawberry` dot, and the `.navrow` / `.filmstrip` layout. **The grid is not amended.**
>
> **Two consequences recorded rather than discovered later:**
>
> 1. **At 26×34dp a page miniature is a smudge, not a picture.** It reads as *"something is on this page, and
>    roughly where"* — which is the capability the ruling protects — not as *"which photo."* The frozen size is
>    preserved in terms, so C5 does not enlarge the thumb to compensate.
> 2. **There is no placeholder left to fall back on.** The rules are gone from the specification, so an empty
>    page renders as an empty sheet, which is what it is.
>
> [V2-BENCH-REVIEW §E.2](V2-BENCH-REVIEW.md)'s phrase *"faint text lines"* describes the treatment this ruling
> removes. It is the historical record of the 2026-07-28 freeze and is left intact — **superseded here, not
> rewritten.**

**Raised 2026-08-04 by C5's pre-implementation blocker check ([ADR-095 §3](../DECISIONS.md#adr-095-blockers)),
before any production code — and ✅ **RULED THE SAME DAY as [OD-22](#d-053-ruling), Option (c)**, above. What
follows is the question as it was put.**

**What the freeze specifies.** `.pthumb` (`v2-bench.html:259-265`) is a **26×34 blank paper sheet** — `--paper`,
a `--paper-edge` hairline, an asymmetric radius so the spine reads squarer, a 2px spine, a contact shadow. Its
interior is `.pthumb i` (`:263`): **three 1px `--ink-faint` rules at 3px pitch**, drawn on every page except the
cover and the back (`:698`). They are a *drawing of text*, not text — the prototype has no document to draw.

**What the product ships today.** `EditorPageStrip` renders a **live miniature of the real page**: the page's own
`SceneRenderer` tape replayed through `PagePreview`, the same render path the canvas uses
(`feature/editor/…/EditorPageStrip.kt:204-244`). A card with a photo on it looks like a card with a photo on it.
An empty page renders blank and keeps a faint page number so it stays legible (`:177-184`). It has been that way
since the strip was built, and its KDoc says why in one line: *"a card looks like the page it navigates to rather
than a numbered placeholder."*

**Why this is a decision and not a parity fix.** Transcribing `.pthumb` literally does two things at once:

1. It **removes information that ships** — the only place in the editor where you can see what is on another page
   without going there. Nothing else in the product answers *"which one was the photo page?"*
2. It **replaces that information with a statement that is false on most pages.** Three ruled lines say *this
   page holds a few lines of text.* On this document most pages are empty, and one may hold a full-bleed photo.
   That is the same class of defect as [D-044](#d-044) — a frozen chip claiming a value that is not the
   element's — which the owner ruled ([OD-17](#d-044-ruling)) by **amending the frozen file** so the control
   states a truth rather than a plausible-looking constant.

Keeping the miniature, on the other hand, is a **visible divergence from a frozen surface** on the one property
the freeze's own physicality audit was proudest of — [§E.2](V2-BENCH-REVIEW.md) records the navigation being
*"rebuilt as little paper sheets: real edges, a spine, faint text lines … Slider → riffled cards."*

**What no existing ruling settles.** [OD-2](../DECISIONS.md#adr-089) settled the page *count*.
[D-009](#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp)
settled the *touch targets* on this exact surface. [OD-9](#d-031-ruling) / [OD-11](#d-034-ruling) /
[OD-14](#d-039-ruling) govern verbs and toolbars. None of them reaches the thumb's interior. And the §E.2 audit
that produced the frozen treatment was auditing **the prototype's slider pips** — it was not weighing live
miniatures against paper sheets, because the prototype never had any to weigh.

**The options, stated so the ruling can be a choice rather than an essay:**

| | What ships | Cost |
|---|---|---|
| **(a) Transcribe the freeze** | 26×34 paper sheets with three faint rules; no page content anywhere in the strip | the *"which page was that?"* capability is lost, and the rules are false on most pages. The frozen appearance is exact |
| **(b) Keep the miniature inside the frozen sheet** | the frozen 26×34 box, spine, radius, shadow, lift and dot — with the **live page** drawn inside instead of `.pthumb i` | every frozen property except `.pthumb i` is met; the interior diverges, and at 26×34 the miniature is a smudge rather than a picture |
| **(c) Amend the frozen file first** | whichever of (a)/(b) the owner wants, written into `v2-bench.html` before Compose moves — the [OD-16](#d-043-ruling)/[OD-17](#d-044-ruling)/[OD-21](#d-047-ruling) pattern | one more amendment to a frozen surface; and it is the only route that leaves the HTML and the app agreeing |

**A note that belongs to the owner and not to the implementer:** the *grid* has no such conflict. `.pgcell` is
net-new — nothing ships that it replaces — so whatever is ruled here, the grid can transcribe the freeze without
removing anything. If (b) or (c) is chosen for the strip, whether the grid's cells also show content is a
separate question, and C5 will not answer it by inference.

**Until it is ruled, C5 builds every row except 5.8**, and does not draw a thumb interior of either kind.

---

### D-052 — `Add › Text` drops the new box on top of what is already on the page {#d-052}

**Raised 2026-08-04 by C4's Device Verification Pass 2** (Samsung SM-A176B, Android 16, debug build).
🟦 Open. **Not a merge blocker** — it is the shipped placement rule meeting a new one-tap route to it, not a
regression C4 introduced.

**What happened, before I knew why.** The page held one text box reading *"Hello"*. I tapped `Add`, then
`Text`. A new box arrived already in its editing session — correct, and pleasant — but its outline was drawn
*around* the existing "Hello", so for the length of the session the page read as **one** block containing
both my old words and my cursor. Typing `Bench` produced a box that, once the session ended, sat directly
over the old one; the platform tree confirms two elements, `Text: Bench` and `Text: Hello`, occupying
adjacent, touching rectangles. A first-time user has no way to tell which words are the ones they are
editing, and the obvious reading — *"it put my new text inside my old text"* — is wrong.

**Why it is C4-adjacent and not C4's.** The drop position comes from the placement path C4 reuses **by
name** under [OD-21](#d-047-ruling) — *"The Text action must continue to reuse the existing `addTextAndEdit`
flow so the C3 in-place editing model remains unchanged."* — and reusing it unchanged is what
the ruling required. What C4 changed is the *frequency*: `Add › Text` is two taps from anywhere, so the
collision is now met on a page that already has content, which the empty-state route never did.

**What a fix would decide, if ruled:** whether a placement offsets from, or avoids, the occupied region —
a placement-policy decision that belongs to whoever owns adding, not to the package that rebuilt the bar.

---

### D-051 — the Add chooser offers `Photo` under a *replace* glyph {#d-051}

**Raised 2026-08-04 by C4's Device Verification Pass 2.** 🟦 Open. **Not a merge blocker.** **This is a
defect in the frozen file, not in the Compose implementation** — which is precisely why it is filed here.

**What it is.** The chooser's `Photo` row draws two circular arrows chasing each other — the *refresh /
replace / sync* glyph. On the device, beside a `Text` row marked with a clean `A`, it reads as *"replace
the photo"* or *"sync"*, and it is the one row in the sheet whose mark does not name what the row does.

**The implementation is faithful.** `v2-bench.html:721` builds that row with `ICON.replace`, and
`BenchAddChooser.kt:139` uses `ZinelyV2Icons.Replace`. The freeze says replace; the Compose says replace.
Changing it in Compose alone would be a Compose-side departure from a frozen surface, which the HTML-first
workflow forbids in those terms — *"any UX change after freeze must first update the HTML specification"*.

**Why it matters more here than it did in the prototype.** In the frozen file the row was one of three in a
narrated demo. After [OD-21](#d-047-ruling) this sheet is **the only route to adding a photo** in the whole
editor: the shelf that used to carry an unambiguous *"Add a photo"* card is retired, so this glyph is now
the single visual the user has to recognise the verb by.

**What a fix would touch, if ruled:** `v2-bench.html:721` first (a photo/image mark — `ICON.image`-class
artwork rather than `ICON.replace`), then `BenchAddChooser.kt` and the chooser's device screenshot. It is a
one-glyph amendment to a frozen surface, and the amendment is the owner's to make.

---

### D-050 — the empty page still sends the user to a shelf that no longer exists {#d-050}

**Raised 2026-08-04 by C4, while reading its own golden.** 🟦 Open. **Not a merge blocker**, and
deliberately not fixed in C4.

**What it is.** `Copy.EmptyState.SUPPLY_CUE` reads *"Grab a photo or a few words from the supplies
below."* (`core/copy/…/Copy.kt:175` — it was `:126` when this defect was raised; C4's own
[§6.12](../DECISIONS.md#adr-094-deviations) relocation inserted 49 lines above it, and a citation left to rot
is the [D-046](#d-046) failure mode, so it is corrected here rather than added to that pile), and `EditorEmptyState` draws a chevron beneath it pointing at the
shelf (`EmptyStateTrayCueTag`, `feature/editor/…/EditorEmptyState.kt:37`). Both were true until
[OD-21](#d-047-ruling) retired `EditorSupplyTray`: the bottom of the screen now holds one `Add`, and the
two add verbs live behind it. The line names a surface that is not there, and the arrow points at a bar
whose contents no longer match the sentence.

**How it was found.** Not by a failing test — every test passed. It is legible in
`editor_screen_light.png`, C4's re-recorded screen golden: the invitation and the new bar are in the same
frame, and the sentence and the frame disagree.

**Why C4 does not fix it.** These are product-voice strings and a teaching gesture, not frozen CSS. The
frozen file's own empty state is not part of C4's fence, the owner owns the wording, and rewriting an
invitation is a design change wearing a parity fix's clothes. Recorded here so it reaches the owner as a
decision rather than being made by an implementer mid-package.

**What a fix would touch, if ruled:** `Copy.EmptyState.SUPPLY_CUE`, and whether the cue chevron survives
at all now that its target is a single control rather than a shelf of four.

### D-049 — the sheet resizes 17–29 % every time you select or dismiss, and `.bar` is where the fix lives {#d-049}

**Raised 2026-08-02 by [C2a's Pass 2](../DECISIONS.md#adr-091-completion-device) as **P2-1**; filed here
2026-08-04 by C4's blocker check, which is the package it was routed to. 🟦 Open — not a merge blocker for C4,
but it is the one thing C2a explicitly said *"is worth someone's decision."***

**Measured, on hardware.** The page renders `1028×1454 px` with nothing selected and `850×1202 px` with a
selection — a **17 % linear shrink** — because the transform bar takes its space from the canvas rather than
overlaying it. It is **pre-existing** and [D-037](#d-037) did not cause it.

**Why C2a routed it here rather than fixing it.** Before OD-13 the user met the resize once, on selecting.
Dismissal did not exist. Now it is met on **every** dismissal too, in the *growing* direction — and
[OD-12](#d-035-ruling) rules that *the editor represents the physical printed artifact*. A sheet of paper that
changes size when you stop touching it is a statement about the artifact, not about the chrome. `.bar` is C4's
fence and the bottom chrome's space-taking is what C4 rebuilds, so C4 is where any fix belongs.

**Re-measured under C4's bar, 2026-08-04** — checklist item 11 of [ADR-094 §7](../DECISIONS.md#adr-094-device-checklist),
on a Samsung SM-A176B (Android 16, density 420dpi), reading the drawn sheet out of the device raster rather
than the tree: **1500 px tall at rest, 1069 px selected — a 431 px, 28.7 % loss of sheet height.** The
number is *larger* than the 17 % C2a recorded, because C2a measured the transform bar alone and a selection
now also raises the frozen `.ctx` verb bar above it (the [D-039](#d-039-ruling) split). The decision below is
unchanged and still the owner's; only the size of what it decides has been corrected.

**What the decision actually is:** whether the bottom chrome continues to *take* canvas height (the shipped
behaviour, and what `.bar{flex:none}` at `v2-bench.html:267` specifies inside a flex column), or overlays it so
the sheet holds still. The freeze specifies the former. Choosing the latter is an amendment; choosing the
former is accepting a measured Pass 2 observation, on the record, as shipped behaviour. Either is legitimate —
**neither is the implementer's to choose**, which is why C2a wrote *"the fix, if any"*.

---

### D-046 — the OD-16 amendment stranded 25 line citations in files C3 does not own {#d-046}

> ## ✅ OWNER RULING — **OD-18**, 2026-08-04 {#d-046-ruling}
>
> *"D-046 — Leave it open. Do not expand C3's scope to perform a repository-wide citation sweep. The stale citations
> belong to the documents that own them and should be corrected incrementally by their owning packages or by a
> dedicated documentation-maintenance package. Do not reopen C3 for documentation-only cleanup."*
>
> **The ruling endorses the refusal, and names the owner.** C3's decision not to sweep is upheld, and the repair is
> assigned by **document ownership** rather than to whoever broke it: each stale citation is corrected by the package
> that next touches its file, or by a dedicated documentation-maintenance package. C3 is **not** to be reopened for
> it. This entry stays ⛔ **OPEN** as the standing record of what is owed and where.

**Raised 2026-08-03 by the independent review of the reopened C3. ⛔ OPEN — deliberately not fixed inside C3, and
[✅ ruled OD-18](#d-046-ruling) on 2026-08-04 to stay that way.**

**What it is.** [OD-16](#d-043-ruling)'s amendment inserted ~64 lines into
[`v2-bench.html`](mockups/v2-bench.html), moving every address below the insertion point. C3 repaired the citations
in the files C3 already owns, each verified by opening the frozen file at the cited line. **Twenty-five explicit
`v2-bench.html:NNN` citations remain stale in ten files this package does not touch**, plus an unaudited number of
bare `` `:NNN` `` references:

| file | stale citations |
|---|---|
| `core/ui/.../ZinelyContentInks.kt` | 9 |
| `feature/editor/.../BenchContextBar.kt` | 4 |
| `core/ui/.../ZinelyContentInksTest.kt` | 3 |
| `core/ui/.../ZinelyV2Icons.kt` | 2 |
| `feature/editor/.../BenchContextBarTest.kt` | 2 |
| `core/ui/.../ZinelyElevation.kt`, `ZinelyV2Colors.kt`, `ZinelyV2Motion.kt`, `ZinelyV2Typography.kt`, `feature/editor/.../SnapGuides.kt` | 1 each |

**Why it is filed rather than swept.** Three mechanical repair passes were attempted during this package and all
three did damage: the first rewrote citations belonging to **other** mockups whose line numbers collided, the second
flattened [ADR-093 §1](../DECISIONS.md#adr-093)'s drift table — whose entire content is *"ADR-089 said X, it is
actually at Y"* — into a table where both columns said Y, and the third began modifying ten clean files inside a
package whose ruling says **"do not perform unrelated documentation cleanup"**. The working tree was restored each
time. A fourth sweep across files C3 has no business touching, at the acceptance gate, is not a repair — it is the
same mistake with more surface area.

**How to fix it when it is scheduled.** The method that works is in
[HANDOVER lesson 8k](../COMPOSE-V2-HANDOVER.md): rebuild the old→new map by diffing the frozen file against its own
previous revision (`difflib` over lines), then rewrite a token **only** when the backticked selector on that same
line is present at the old address and absent at the new one — verified per token, never per table. And re-check any
table whose *purpose* is to record a stale address before trusting the result.

**Scope, stated precisely so a reader does not over-read it.** This entry covers the **25 citations in files C3 does
not own**, which this amendment broke. Two other populations exist and are **not** D-046's:

1. **Citations in C3's own files** — repaired inside C3, each verified against the frozen file. The independent
   re-review found four this entry's original scoping let fall through the gap (`EditorScreenTest.kt`,
   `DECISIONS.md` ×2, `V2-SPEC-DEFECTS.md` ×2); those are fixed, not filed.
2. **~18 further citations in `DECISIONS.md`, `V2-SPEC-DEFECTS.md` and `COMPOSE-V2-ROADMAP.md` that were already
   wrong at `HEAD`** — verified individually (`:105`, `:118`, `:159`, `:233`, `:244`, `:293`, `:379`, `:400`,
   `:414`, `:424`, `:493`, `:565` among them). They predate this amendment and are **pre-existing debt, not this
   change's**. Named here because a reader would otherwise assume the prose citations had been audited; they have
   not been.

**A second cause, added 2026-08-04.** [OD-21](#d-047-ruling) amended the frozen Bench again — a header block and one line of markup — moving every address below it by **+23** above the bar's markup and **+24** below. The population this entry enumerates has therefore drifted a second time, and the specific `:NNN` values listed above are the **post-OD-16** ones, not the current file. C4 re-anchored only its own citations, verified each against the selector it names, and repaired the frozen file's **own** internal citations (eleven of them, since leaving the spec self-inconsistent is the failure this register exists to catch). Everything else was left alone, as [OD-18](#d-046-ruling) requires. **The enumeration above should be re-derived, not trusted, whenever this is finally scheduled** — the method is unchanged and is in [HANDOVER lesson 8k](../COMPOSE-V2-HANDOVER.md).

**Severity.** Documentation accuracy, no user impact, no runtime effect. Every stale citation points into a design
document, not into code.

### D-042 — the plan said re-skin the style panel; two rulings already forbid it {#d-042}

**Raised 2026-08-02 at C3's pre-implementation blocker check, before any production code — and resolved the same day,
against this implementer's own recommendation to escalate it.**

> ⚠ **This entry was first filed as ⛔ OPEN, requesting an owner ruling, and C3 was stopped waiting for one. That was
> wrong.** The independent review returned **NO-GO on the stop**: [OD-11](#d-034-ruling) and [OD-14](#d-039-ruling) each
> state, unconditionally, that no existing editor capability is removed — which kills the re-skin reading outright and
> leaves a plan defect, not a question. The escalation had tested only those rulings' *permission* rationales (space
> competition, simultaneity) and never their *prohibitions*, and the draft contradicted itself: one paragraph said
> OD-11 *"refused this exact trade"*, the next said *"neither existing ruling reaches it"*. The precedent it should
> have followed was one package earlier — [ADR-092 §1](../DECISIONS.md#adr-092) cleared **five** candidate owner
> decisions against rulings in hand and shipped. **Kept as a deviation record, because the analysis below is correct
> and only its conclusion was not.**

**What the freeze specifies.** `.styletb` (`v2-bench.html:261`) lives inside `.kbstack`, which is `translateY(110%)`
— **offscreen except under `.editing`** (`:259-260`). It is an editing-mode row, and it holds exactly four things
(`:452-454`):

| chip | wired in the freeze to | |
|---|---|---|
| **Fraunces** | — | **nothing.** The string appears at `:7`, `:69`, `:408` and `:421`; no handler exists |
| **A 23** | — | **nothing.** The string appears once, at `:408` |
| **Ink** (+ a 14px `.sw` dot) | — | **nothing.** `#editColour` has no listener anywhere; `openInk` is bound only inside `buildCtx` (`:515`, `:583`), to the **`.ctx` bar's** Ink verb. This entry first claimed otherwise — the review disproved it, and with it the *"pulls C6 forward"* arm: `.inkpop` is never reached from `.styletb` under any reading |
| **Done** | `#doneEdit` → `endEdit()` | C3's, unambiguously |

In the frozen product there is **no style control in the selected state at all**: `.ctx`'s `Size` has no handler
([D-031](#d-031)) and its `Ink` opens C6's popover.

**What the product ships.** `TypeBar.kt` ([ADR-055](../DECISIONS.md#adr-055)) is a four-row panel in the **selected**
state: a ten-value **size** stepper, **align** ×3, **bold**, **italic**, and **five inks** — thirteen controls, each
wired to `Intent.StyleText`, each reachable today. It is reachable *because* [OD-9](#d-031-ruling) routed the frozen
`Size` verb to it rather than inventing `.inkpop` early, and [ADR-092](../DECISIONS.md#adr-092) routed `Ink` there too.

**The contradiction, stated plainly.** [ADR-089](../DECISIONS.md#adr-089) rows 3.5–3.7 give the Compose target as a
`TypeBar.kt` **re-skin**. Taken literally, C3 would:

- **remove** size, align, bold, italic and five inks;
- **replace** them with two chips that do nothing and one that leads into a blocked package;
- **move** what survives out of the state that has it today, since `.styletb` exists only while editing.

A parity phase does not delete editor capability. [OD-2](#d-029) said Phase C *"does NOT introduce new editor
capabilities"*; [OD-11](#d-034-ruling) refused the mirror image of this trade when the frozen `.ctx` would have
deleted eight transform controls.

**Why the existing rulings *do* decide it — the correction.**

| ruling | why it does not decide this |
|---|---|
| [OD-11](#d-034-ruling) | **decides it.** *"no existing accessibility capability is removed; **no existing editor capability is removed**"* — unconditional, and reading (b) removes five. The space-competition argument was OD-11's reason for *permitting* two bars; it is not a limit on what it *forbids*, and the first draft of this entry confused the two |
| [OD-14](#d-039-ruling) | **restates the prohibition** (*"existing editor capabilities must be preserved · no functionality is removed"*) and imposes nothing further: these two are never on screen at the same moment — `EditorScreen.kt:267` gates `styleTarget` on `interaction !is EditingText`, so the Type bar is structurally unreachable while editing. Nothing to assign away, so OD-14 **permits** reading (a) |
| [OD-9](#d-031-ruling) *drawn, inert, inventing nothing* | decides the two dead chips cleanly. It says nothing about whether the panel they would displace survives |

**The two readings, and what each ships.**

| | (a) `.styletb` is **additive** — an editing-state row | (b) `.styletb` **replaces** `TypeBar.kt`, as ADR-089 says |
|---|---|---|
| what the user gains | a style row while typing, where there is none today | fidelity to the frozen editing surface |
| what the user loses | nothing | size, align, bold, italic, five inks |
| the two dead chips | ship inert beside `Font` and `Replace`, under OD-9 | same |
| the `Ink` chip | opens the shipped Type bar, as OD-9 did for `Size` — or ships inert until C6 | must open `.inkpop`, which [D-028](#d-028) blocked until [OD-24](#d-028-ruling) (2026-08-05); C6 builds it |
| cost | two style surfaces exist, in two different states, with different vocabularies — a real coherence cost, and **C4**'s to resolve | a capability regression, and a blocked dependency pulled forward |
| contradicts | [ADR-089](../DECISIONS.md#adr-089) row 3.5's stated Compose target | [OD-2](#d-029), [OD-11](#d-034-ruling), and the parity principle |

**Resolution: (a), and it was never the implementer's to choose — it was already chosen.** No user loses a control
they have today; ADR-089 row 3.5 disagrees with a *ruling*, so the row is corrected, not the product. Row 3.5's
Compose target becomes a **new** editing-state row (`BenchStyleRow.kt`) and `TypeBar.kt` is untouched — asserted, by
counting the Type bar's nodes in the selected state before and after C3.

**The residual question answers itself too.** Whether the `Ink` chip should open the shipped Type bar (OD-9's move for
`Size`) or ship inert is settled by the freeze: **the chip is not wired there either.** All three chips ship inert
under [OD-9](#d-031-ruling), beside `Font` and `Replace`. The swatch still *reports* the element's colour (ADR-093 row
3.9), which is the one live thing the freeze gives it.

**What is left is a cost, not a question.** Two style surfaces with different vocabularies — a four-row panel when
selected, a chip row when editing. Visible, real, and **C4's**, recorded here on the [ADR-092 §2](../DECISIONS.md#adr-092)
precedent rather than stopping the package for it.

### D-041 — leaving a page mid-session orphans an empty text box {#d-041}

**Found 2026-08-02 on `SM-A176B` / Android 16, while trying to prove [D-040](#d-040)'s state reachable.** It is not a
C2b defect and nothing in C2b touches it; it is filed because it was seen, it persists to disk, and the next package to
own the editing surface should decide about it rather than rediscover it.

**What happens.** *Add words* places a blank `TextElement`, autosaves it (`EditorReducer.kt:32–33`) and opens a session.
Every deliberate exit from that session routes through `endTextSession`, which **deletes a box whose resulting text is
blank** — that is the rule that stops an accidental *Add words* leaving litter behind. But `Intent.GoToPage` routes
through `leavePage` (`:166–169`), which clears selection and interaction *and never calls it*. Tapping the next page in
the filmstrip therefore leaves the empty box on the page it came from.

**Observed:** *Add words → Page 2 → Page 1* leaves `{"type":"text","id":"el-1",…,"text":""}` in the project's
`document.json`, and the box is there on the page — selectable, invisible, and exporting as nothing.

**Why it is worth a line rather than a shrug.** The box is not harmless furniture: it is an element a first-time user
never knowingly created, it sits in the tap order, and it is what made [D-040](#d-040)'s "impossible" state trivially
reproducible. The two obvious readings — *leavePage should clean up like every other exit*, or *the blank box is
legitimate scaffolding and the deliberate exits are the odd ones out* — point at different fixes, which is why this is
filed rather than patched.

### D-040 — Size and Ink on a blank text box: a dead end that took the toolbar with it {#d-040}

**Raised 2026-08-02 by the independent review of D-039's implementation, and fixed in the same change.** No owner
ruling is needed — [OD-9](#d-031-ruling) already decides this class — but it is recorded because it was a real
trap, it shipped in `cacc9b2`, and **nothing caught it**: not the 1449-test suite, not device Pass 1, not Pass 2.

> ⚠ **A correction, and then a correction of the correction — both kept, because the second is the lesson.**
> On the second device round the implementer tried to reach this state, failed, and wrote the entry up as
> *unreachable*: `EditorReducer.endTextSession` **deletes** a text box whose resulting text is blank, so a box added
> and left empty was indeed gone on the next look. **That generalisation was false, and the independent review
> falsified it the same day.** `endTextSession` governs only the *end of a session*; two ordinary routes go around it:
> 
> - **Undo.** Add words → type → commit pushes `EditTextCommand(before = the blank placed box, after = "hi")`
>   (`EditorReducer.kt:306–308`) → **Undo** inverts it, restoring `text = ""`, and `stepHistory` preserves the
>   selection (`:332`). Blank box, selected, `Idle`.
> - **Leave the page mid-session.** `Intent.GoToPage → leavePage` (`:166–169`) clears selection and interaction
>   **without** running `endTextSession` at all, so the blank box is never cleaned up. It is then sitting on the page,
>   and one tap selects it.
> 
> The second route was **reproduced on hardware** (SM-A176B / Android 16): Add words → back → Page 2 → Page 1 leaves
> `{"type":"text","id":"el-1",…,"text":""}` in the autosaved `document.json`, and tapping it raises the frozen bar
> with **`Size` and `Ink` `clickable=false enabled=false`** while `Edit` and `Delete` stay live — the guard working,
> watched, in the state that was supposed not to exist. **The original record was right; the correction was wrong.**
> It is left standing rather than deleted, because the failure mode it demonstrates — *one failed attempt to reach a
> state, generalised into proof that it cannot be reached* — is exactly the reasoning device verification exists to
> catch, and it survived a Pass 1 that was otherwise clean.

**The chain.** `benchVerbKindOf` keys on element *type*, so a blank text box got the whole frozen text set,
`Size` and `Ink` live. Tapping either ran `typeBarOpen = true`, and then three independent guards conspired:

| | |
|---|---|
| the frozen bar carries a `!typeBarOpen` term | → **the bar hides** |
| the Type bar is composed only `if (styleTarget != null && typeBarOpen)`, and `styleTarget` requires `text.isNotBlank()` ([ADR-055](../DECISIONS.md#adr-055)) | → **nothing appears in its place** |
| the reset effect is keyed on `styleTarget?.id`, which was already `null` and stays `null` | → **it never re-runs, so `typeBarOpen` stays true** |

So a single tap on a blank box made the contextual toolbar vanish **and kept it vanished across later
selections** — select a photo next and the key is unchanged, so the frozen bar still does not return. No
capability was lost (the transform bar's Delete and the Reframe chip both come back, since they key on the
same `ctxVisible`), but the user is left with a toolbar that disappeared for no visible reason and no way back.

**Why it is the mirror of a rule the corpus already had.** `TypeBarTest.a_still_blank_text_box_is_not_offered_style`
exists precisely to stop the *transform* bar advertising a control the reducer would silently refuse. C2b put
the same offer on a second bar without carrying the guard across — the recurring shape of this programme's
defects: a rule enforced in one place, and a new surface that does not know about it.

Both surfaces now refuse the same offer. That matters more than it looked: the blank box is not a hypothetical
the persistence layer might one day hand us — **this build writes one to disk itself**, because `Intent.PlaceText`
autosaves at placement (`EditorReducer.kt:32–33`) and `leavePage` can end the session without ever cleaning it up.

**The fix, which invents nothing.** `benchContextVerbs(kind, styleable)` marks `Size` and `Ink`
`enabled = false` for a blank box, putting them in the class [OD-9](#d-031-ruling) already established for
`Font` and `Replace` — *a control the freeze draws stays drawn and invents nothing*. A blank box's `Delete`
stays live, since a blank box is the one you most want to be rid of. Mutation-tested: restoring
`styleable = true` kills the assertion.

### D-039 — the Bench now offers the same verb twice, in the ear and in the eye {#d-039}

**Raised 2026-08-02 by C2b's own verification — as a test failure, not a review note.** It does not fence anything.

**What happened.** [OD-11](#d-034-ruling) ruled the frozen `.ctx` verb bar **additive**: the shipped `EditorContextBar` is the
WCAG 2.5.7 single-pointer twin of the drag gestures ([ADR-029](../DECISIONS.md#adr-029) §6), and a parity phase does not delete
an accessibility path. Both bars legitimately carry a Delete. The moment the second one landed,
`TypeBarTest`'s `onNodeWithContentDescription("Delete")` stopped resolving — *"Expected exactly '1' node but found '2'"* — which
is the clearest possible statement of the consequence: **a screen reader's linear sweep now meets the same word twice.**

**Why it was not simply fixed.** Both available fixes are worse than the thing they fix:

| | |
|---|---|
| re-label `EditorContextBar`'s Delete | precisely the "re-skinned or weakened" that [ADR-092](../DECISIONS.md#adr-092) §4 and OD-11 forbid |
| re-label the frozen bar's Delete | breaks parity with `toolsFor()` (`v2-bench.html:496`), where the freeze says *Delete* — and the freeze is canonical |

Any *real* disambiguation is therefore a **third** mechanism — a per-bar container or heading label, or collection semantics —
which is a design decision, not an implementation detail, and so is the owner's.

**How bad is it, honestly.** Not the [ADR-058](../DECISIONS.md#adr-058) class. There, the tree **lied**: a control announced
itself enabled and did nothing. Here nothing lies — both controls announce *Delete*, both delete the selection, and a user who
activates either gets exactly what they were promised. The cost is a redundant word in a linear sweep: a navigational
annoyance, not a WCAG failure and not a breach of trust. It is pinned by
[ADR-092](../DECISIONS.md#adr-092) row 2.13d, an assertion that fails the moment either label moves, so it cannot rot quietly.

**The device answered, and the answer was worse than the question.** C2b's Pass 2 (2026-08-02, SM-A176B) found the
duplication is **visible**, not merely audible — which this entry, written from the code, did not foresee. With a photo
selected, the on-canvas `Reframe` chip sits on the image and the frozen bar's `Reframe` sits about 150px below it: the
same word twice, at the same moment, in one glance. The first-time-user note, written before the reason was known, was
*"there are two Reframe buttons on my screen — did I do something wrong?"* A screen reader meeting `Delete` twice is an
annoyance; a beginner meeting `Reframe` twice reads it as a malfunction, and this app's stated audience is beginners.

**So Pass 2 did not pass, and [ADR-092](../DECISIONS.md#adr-092) was held at `Proposed` because of it** — until [OD-14](#d-039-ruling) below resolved it and a second device round passed both ways. Pass 1 says the code
is right and Pass 2 says the screen is confusing; the handbook forbids averaging them. Option **(b)** below was written
for the ear and is now the weaker answer — labelling the containers does nothing for the eye.

### Owner ruling — OD-14: assign responsibilities, do not duplicate presentation {#d-039-ruling}

**Ruled 2026-08-02.** The repository had demonstrated that keeping *both* bars faithfully produces duplicated
actions which are technically correct and confusing to a first-time user. The ruling keeps both and removes the
duplication by deciding **who presents what**:

> The frozen contextual toolbar remains additive. `EditorContextBar` remains, as required by
> [OD-11](#d-034-ruling) and [ADR-029](../DECISIONS.md#adr-029) §6. However, **identical actions must not be
> presented twice simultaneously.** Accordingly: each capability shall have **one primary visible presentation
> at a time**; accessibility capabilities must be preserved; existing editor capabilities must be preserved;
> **no functionality is removed**; D-039 is resolved by **assigning responsibilities** rather than duplicating
> presentation.

**The assignment, and why it falls where it does.**

| Owner | Verbs | Why this owner |
|---|---|---|
| the frozen `.ctx` bar | **Edit · Font · Size · Ink · Delete** and **Reframe · Replace · Delete** | the element verbs, which the freeze specifies and which it presents as **words** rather than glyphs |
| `EditorContextBar` | **move ×4 · scale ×2 · rotate ×2 · order ×2** | exactly the verbs [ADR-029](../DECISIONS.md#adr-029) §6 exists for: *drag* is the gesture with no single-pointer twin, and these ten are what that argument covers. `Delete` never needed a twin — it was never a gesture |
| the on-canvas chip | — | `Reframe` is now offered by name, in a bar, instead of by a pill over the artwork |

**The minimum implementation, which is three lines and one parameter.** While the frozen bar is up
(`ctxVisible`), `EditorContextBar` withholds **`Delete`** and the `ReframeAffordanceChip` withholds itself.
Both return the instant the bar stands down — an open text session, an open Type bar, a Reframe, a
multi-selection, or an element kind with no frozen verb set — so at no moment is a capability absent from the
screen. Nothing was deleted, no component was re-skinned, and the ten transform verbs are untouched in every
state, which is the part OD-11 actually protects.

**What was tried and rejected as beyond the minimum.** `Size` and `Ink` open the same Type bar that the
transform bar's **Style** control opens, so standing Style down looks like the same tidy-up. It is not: Style
is the Type bar's *disclosure toggle* — the control that both opens and closes it — so withholding it leaves
the open panel with no way back. 22 `TypeBarTest` failures said so before any reasoning did. And the ruling is
about *identical actions presented twice*, whose evidence was two controls wearing **the same word**; "Text
style" is a different offer that happens to share a destination. Left alone, and left for device Pass 2 to
judge fresh rather than argued away here.

**What is owed.** A judgement that can only be made with a device in hand: **does the duplicate register as confusing when you
hear it?** That question belongs to C2b's device Pass 2, and this entry exists so the answer reaches the owner rather than
being settled by whoever is holding the phone.

**Options for the owner**

| | Disposition |
|---|---|
| **(a)** | **Leave it.** Redundant, not ambiguous; both do the same thing. Cheapest, and defensible. |
| **(b)** | **Label the containers** — give each bar a semantics container name ("Transform", "Actions"), so the sweep announces which bar it is in before the verbs. Invents no visible UI and touches neither bar's controls. |
| **(c)** | Revisit OD-11 and let one bar go — **not recommended**, and named only for completeness: it is the accessibility path OD-11 exists to protect. |

**Recommendation, revised after Pass 2, and since superseded by [OD-14](#d-039-ruling) above: (b) is no longer sufficient.** The device decided, which was the whole point of running two passes — and it decided against the cheap fix. What is needed is a rule for *which* bar owns a verb the two share, so that each verb appears once: the frozen bar is the one the freeze specifies and the one a sighted beginner will reach for, and `EditorContextBar`'s value under [ADR-029](../DECISIONS.md#adr-029) §6 is the **transform** verbs (move, resize, rotate, order) that no gesture-free path otherwise offers — not `Delete`, which the frozen bar now carries. Dropping the shared verbs from the transform bar would remove no capability from any input method, but it touches a fenced component and rests on an accessibility argument, so it is put rather than taken. The on-canvas `Reframe` chip is the third copy and the easiest to retire.

### D-038 — the frozen photo bar offers Replace, and nothing can reach it {#d-038}

**Raised 2026-08-02 by C2b's pre-implementation blocker check. It does not fence C2b**, which ships the verb under an
existing ruling; it is filed because the ruling it ships under decides *appearance*, and this entry is about *capability*.

**What the freeze says.** `toolsFor('photo')` is **Reframe · Replace · Delete** (`v2-bench.html:496`). Reframe and Delete
map onto intents the editor already dispatches. Replace does not.

**What the repository says.** Verified, not inferred:

| | |
|---|---|
| `Intent.ReplaceImage(id, assetId)` | **exists in the reducer** ([`EditorReducer.kt:97`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/EditorReducer.kt#L97)) and is **dispatched from nowhere** in any `src/main` |
| the only image picker | `Intent.RequestAddImage` → `Effect.PickAndDecodeImage` → `Intent.CommitAddImage` — which **adds a new element** |
| what Replace would need | that same pick, carried back to an **existing element's id** — a new parameter on the effect, i.e. a change to the effect protocol |

**Why this is not another D-037.** [D-037](#d-037) looked identical from the outside — an intent sitting in the reducer with
nothing dispatching it — and its fix was one line, because `SelectAt`'s miss branch already produced the state. Here the
missing piece is a **flow**: a picker that knows which element it is replacing. The owner scoped D-037 as *"completion of an
existing capability, not a new feature"*, and that scoping is exactly what does **not** transfer, so it is asked rather than
assumed.

**What C2b did in the meantime.** Shipped `Replace` **drawn and disabled**, under [OD-9](#d-031-ruling)'s class — *the freeze
specifies the editing surface, not the whole application flow*, so a control it draws stays drawn and invents nothing. It is
announced as disabled to the platform, not merely inert to touch ([ADR-092](../DECISIONS.md#adr-092) row 2.13a). The bar is
faithful; the capability is the owner's call.

**Options for the owner**

| | Disposition |
|---|---|
| **(a)** | **Wire it** — carry a target id through `Effect.PickAndDecodeImage` so a pick can re-point an existing photo. A real capability a user would expect from a bar that offers it, and the reducer half already exists. Not C2b's fence; it needs its own package. |
| **(b)** | **Leave it drawn and disabled**, as C2b shipped it, and record it as specified-but-unreachable beside `Font`. |
| **(c)** | Amend the frozen Bench to drop `Replace` from the photo set, so spec and product agree. |

**Recommendation: (a), scheduled — not now.** A user who sees *Replace* on a photo has been told the app can replace it, and
`Font` is a weaker case only because [ADR-055](../DECISIONS.md#adr-055) genuinely excludes font choice by design, while
nothing excludes this. **(b)** is the honest interim, which is why C2b shipped it; **(c)** removes something the product
plausibly wants. The cost of (a) is one effect parameter and one reducer path that is already written and already tested.

### D-037 — the dim shipped without either of the two ways the freeze gives the user out of it {#d-037}

| | |
|---|---|
| **Status** | ✅ **RESOLVED 2026-08-02** by owner ruling (**OD-13**) — option **(a)**, selection is transient. See [the ruling](#d-037-ruling); implemented as [ADR-091](../DECISIONS.md#adr-091) row 2.14 |

**Raised 2026-08-02 by C2a's Pass 2 device verification, which it failed. It blocked C2a's acceptance and therefore
C2b.** Pass 1 passed completely — the dim is exact to one channel step of the frozen `opacity:.4` on hardware. This
entry is about what that exactness does not answer.

**What happens.** Select an element on a page that has others. Every other element fades to **2.78:1** against the
sheet — correct, and precisely what `.content.focusing .el:not(.sel-focus){opacity:.4}` specifies. Then try to stop
selecting it. Tapping empty paper does nothing. Tapping the desk does nothing. The dim persists until the page is
changed or the element deleted.

**The gap is older than C2a; C2a is what turns it into a defect.** Verified in the repository, not inferred:

| | |
|---|---|
| `Intent.ClearSelection` | **exists in the reducer** ([`EditorReducer.kt:26`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/EditorReducer.kt#L26)) and is **dispatched from nowhere** in `feature/editor/src/main` |
| how selection is set | long-press only ([`EditorGestures.kt:78`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorGestures.kt#L78)) — **as of [OD-13](#d-037-ruling), a plain tap too, which is what closes this entry** |
| how selection is cleared | a page change ([`EditorReducer.kt:198`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/EditorReducer.kt#L198)) — that is the whole list |

The frozen Bench gives the user **two** exits on the same screen, and C2a implemented neither, because neither is
in C2a's fence:

```js
canvas.addEventListener('click',function(){ … deselect();});         // v2-bench.html:627
$('doneBtn').onclick=function(e){ … deselect();};                    // v2-bench.html:629
```

`.bar` and its Done button are **C4**. The tap-to-deselect is unowned by any package because the freeze expresses
it as canvas behaviour rather than as a selector.

**Why this is a Pass 2 failure and not a backlog item.** Before C2a a stuck selection cost an outline. After it, a
stuck selection **fades everything else the user has written**, indefinitely, with no gesture that undoes it. The
screen's question is *"How do I change this page?"*, and it now answers a question the user did not ask —
*"which one thing are you working on?"* — with no way to say *"none, for now."* Below WCAG 1.4.3's 4.5:1, applied
to the user's own words, and undismissable.

**Options for the owner**

| | Disposition |
|---|---|
| **(a)** | **Let C2a add tap-to-deselect** — one `dispatch(Intent.ClearSelection)` on a page tap. The reducer intent already exists; nothing new is invented. It is the freeze's own `canvas` click, and it widens C2a's fence by one behaviour. |
| **(b)** | **Hold C2a's dim behind C4** — implement `.el`/`.sel`/`.handle`/materialise now and land `.content.focusing` when Done exists. Keeps the fence exactly; costs a second pass over this file. |
| **(c)** | Accept the dim as it stands and carry F1 as a known limitation until C4. |

**Recommendation: (a).** It is the smallest change that makes the screen honest, it uses a reducer intent that is
already written and already tested, and it restores a pairing the freeze always had. **(c)** ships a state the user
cannot leave, which is the shape [ADR-058](../DECISIONS.md#adr-058) and the *"every screen answers the user's
current question"* principle exist to prevent.

#### Owner ruling — OD-13 (2026-08-02): selection is transient, not modal {#d-037-ruling}

**Recommendation (a) chosen.** Selection is dismissed by tapping anywhere outside the current selection:

| Tap lands on | What happens |
|---|---|
| blank paper | selection cleared |
| the studio desk, outside the page | selection cleared |
| another selectable element | normal selection **transfer** — *no intermediate clear* |
| (a transform gesture begins) | unchanged; the existing interaction model still owns it |

No confirmation step. No persistent selection mode. **Selection is therefore a transient editing state, not a
modal state** — and this is *completion of an existing capability, not a new feature*: the intent, the reducer
branch, the state and its tests were all already in the repository; only the interaction that dispatches them
was missing.

**How C2a implemented it** ([`EditorGestures.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorGestures.kt),
[ADR-091](../DECISIONS.md#adr-091)). One line — `onTap → Intent.SelectAt` — satisfies all four clauses, because
`SelectAt`'s hit-test **miss** branch already reduces to exactly `ClearSelection`'s state
(`selection = emptySet()`, `EditorReducer.kt:24` vs `:26`). Blank paper and the desk both miss and therefore
clear; another element is selected in the **same** reduction, so no frame exists in which the page reads as
deselected — which the ruling asks for explicitly and which a UI-side "clear, then select" would have broken.
That distinction is not a matter of taste: mutating the implementation to `ClearSelection; SelectAt` fails
`tapOnAnotherElement_transfersSelectionWithNoIntermediateClear`, which inspects the per-reduction selection
history rather than only the end state.

**The cost, stated because it is not free.** `detectTapGestures` must wait out the double-tap window before it
knows a tap was single, so deselection resolves after that timeout rather than instantly. That is inherent to a
surface carrying both a click and a dblclick — which the freeze specifies — and deselecting on *down* would fire
on the way into a double-tap and make text editing flicker.

### D-036 — the frozen Bench draws four resize handles; the editor has eight, and the fourth pair is a capability {#d-036}

**Raised 2026-08-02 by C2a's pre-implementation blocker check. It does not fence C2a**, which proceeds under an
existing ruling; it is filed because the HTML-first workflow requires the canonical file to be corrected rather
than quietly diverged from.

`v2-bench.html:159` positions exactly four handles:

```css
.handle.tl{left:-10px;top:-10px}.handle.tr{right:-10px;top:-10px}
.handle.bl{left:-10px;bottom:-10px}.handle.br{right:-10px;bottom:-10px}
```

`ResizeHandle.entries` in `:core:editor` has **eight** — those four corners and four edge midpoints — and
`TransformMath.resizeByHandle` gives them different behaviour: a corner resizes **both** axes, an edge resizes
**one**. Transcribing the freeze literally would therefore delete axis-constrained resize, which is capability,
not decoration.

**Why this is not a new owner decision.** It is the same shape as [D-034](#d-034), and
[OD-11](#d-034-ruling) already ruled that shape: during a parity phase the frozen vocabulary is **additive**, and
*"no existing editor capability is removed."* C2a applied that ruling — all eight handles are kept and all eight
take the frozen `.handle` appearance ([ADR-091 §1(b)](../DECISIONS.md#adr-091) row 2.6a). The independent review
agreed the reading is right and that escalating it as a fresh decision would have been wrong.

**What is still owed, and why it is filed anyway.** [CLAUDE.md](../../CLAUDE.md) makes the HTML prototype the
canonical design source and requires any post-freeze change to *update the specification first*. Keeping eight
handles is a **visual** divergence from the canonical file, however well-grounded the capability argument is. So
the disposition here is a documentation act, not a design one: the frozen Bench should draw the four edge
handles it omits, under the same amendment precedent [D-024](#d-024-amendment), [D-033](#d-033) and
[D-035](#d-035-ruling) established.

**Options for the owner**

| | Disposition |
|---|---|
| **(a)** | **Amend the frozen Bench to draw eight handles** — the file catches up with the ruling already made, and spec and product agree again. |
| **(b)** | Leave the freeze at four and carry this as a permanently recorded, permanently accepted divergence. |
| **(c)** | Overrule OD-11 for this case and drop the four edge handles — which removes axis-constrained resize from the editor. |

**Recommendation: (a).** It changes no code and no behaviour; it makes the canonical file true. **(c)** is listed
for completeness and is the only option that costs the user something.

**Owner disposition (2026-08-02).** *Do not block implementation on D-036.* It is recorded as **documentation /
spec alignment only**. The canonical Bench **may later** be amended to depict the eight retained handles
established by [OD-11](#d-034-ruling); that amendment **does not affect C2a's acceptance**. D-036 therefore stays
open as a documentation item and fences nothing.

### D-035 — the dark theme dims the sheet, and the document's own ink does not follow it {#d-035}

| | |
|---|---|
| **Artifacts** | [`EditorScreen.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt) `benchPageSurface()` · [`ZinelyV2Colors.kt`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Colors.kt) `paper` · [ADR-055](../DECISIONS.md#adr-055) (content inks) · [ADR-090](../DECISIONS.md#adr-090) · [`v2-bench.html`](mockups/v2-bench.html) `:59`, `:117` |
| **Found** | 2026-08-02, by **C1's device verification** — both passes, `SM-A176B` / `RZCYA1VBQ2H`, Android 16 (SDK 36) |
| **Severity** | **The user's own words are unreadable in the dark theme** — 1.60:1. Blocked C1's acceptance, and therefore all of Phase C |
| **Status** | ✅ **RESOLVED 2026-08-02** by owner ruling (**OD-12**) — option **(a)**, the artifact does not dim. See [the ruling](#d-035-ruling) |

**Measured, on the device, on one document.**

| Screen | Sheet | Content ink | Contrast |
|---|---|---|---|
| **Editor**, dark, after C1 | `#2F2A22` (V2 `paper`) | `#000000` | **1.60:1** ⛔ |
| **Editor**, light | `#F7F2E7` | `#000000` | 18.82:1 ✅ |
| **Read / Proof**, dark, **same document, same minute** | `#EDE6D9` | `#000000` | **16.92:1** ✅ |

**Neither half is a bug on its own, which is why no test caught it.** The content ink is black because it is
*document data that prints* — theme-independent by design, and correctly so: a PDF that changed with the phone's
night setting would be a defect far worse than this one. The sheet is `#2F2A22` because the **frozen Bench dims it**
(`:59`), and C1 transcribed that faithfully. The defect is the *seam*: C1 replaced
`.background(ZinelyTheme.colors.paper)` with `benchPageSurface()`, and V1's dark `paper` was a **lit** `#EDE6D9`
while V2's is a **dimmed** `#2F2A22`. Every unit test, every golden and the whole frozen property table stayed
green, because each half is individually right.

**The frozen Bench does answer this — for its own mock content.** It draws `.t-title` / `.t-body` in `--ink`, which
*is* theme-aware, so in the prototype the dimmed sheet carries cream text. The product cannot copy that, because
the prototype's "content" is CSS and the product's is a document that has to print.

**Why this is the owner's and not the implementer's.** It is the same shape as [D-004](#d-004--the-frozen-zine-content-is-set-in-fraunces-the-render-engine-can-only-draw-inter) — the freeze and the engine disagreeing about the *artifact* rather than the chrome — and every answer is a product decision:

| | What it does | What it costs |
|---|---|---|
| **(a) The artifact never dims** | the editor's sheet stays paper-coloured in dark, as the Read screen already does; the *room* goes dark around it | departs from the frozen Bench on `.page`'s dark value — an amendment to a frozen surface, the fourth. But it is what the shipped product already does one screen away, and it keeps the editor honest about the print |
| **(b) Dim the sheet, invert the ink for display only** | the canvas draws content ink lightened when dark | the editor stops showing what will print — the screen that exists to answer *"how do I change this page?"* would answer it in colours the paper will never have. Also needs a per-ink display mapping that ADR-055 does not define |
| **(c) Dim the sheet, and make content ink a theme-aware document default** | black becomes cream in dark | the PDF changes with the phone's night setting. Rejected on sight here, listed only so the option is on the record |
| **(d) Keep as built, document as a limitation** | nothing changes | ships a screen where the user's own words are invisible at 1.60:1. Not viable |

**Owner decision requested.** (a) is the only one consistent with both the Read screen and the print, and it is what
this register would recommend — but it amends a frozen surface, which is not the implementer's to do.

**Note on scope.** This is not confined to text. Any content the engine draws dark — strokes, dark photo regions,
the [ADR-055](../DECISIONS.md#adr-055) inks tuned for AA **on light paper** — sits on the same dimmed sheet. The
five text inks are AA-tuned against `#F7F2E7`; none of them was tuned against `#2F2A22`.

#### The ruling — owner, 2026-08-02 (OD-12) {#d-035-ruling}

> **The editor represents the physical printed artifact.** The user's content inks are print colours and therefore remain theme-independent. **The editor must never make the user's own content unreadable in order to express a dark application theme.**
>
> Option **(a)**. The artifact itself **does not dim**; the room around it may. The page surface shown in the editor remains the same printable paper surface used by the Read / Proof experience. Dark theme darkens the surrounding studio chrome, **not the sheet**. The five ADR-055 content inks remain unchanged.
>
> Amend the frozen Bench where required, using the precedent of [D-024](#d-024) and [D-033](#d-033). **Do not invent a second content palette. Do not recolour the document. Do not weaken print fidelity.**

**The amendment: the sheet is a light-theme island.** `.page` re-declares eight on-paper tokens — `--paper`, `--paper-edge`, `--ink`, `--ink-soft`, `--ink-faint`, `--matcha`, `--matcha-text`, `--strawberry-text` — with their **light** values, so everything drawn on the sheet inherits paper-coloured surroundings in both themes. No colour is invented: every value restates one already in `:root`. Light renders byte-for-byte as before, because in light the declarations restate what they override. `.phone` is **not** amended and still dims — that is the ruling's other half, and the room is what goes dark.

**Eight, and not the ninth.** The first implementation provided the whole light scheme rather than the declared eight, which also lightened `--page-shadow` / `--page-contact` — the sheet's own shadow. In dark that drew a warm-brown shadow on a dark desk: exactly [D-010](#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark), reinstated inside the fix for this entry, and certified by a re-recorded golden. Review caught it; the pixels below the sheet were *brighter* than the ground. **The shadow is the room's, because the shadow is the sheet's mark on the desk rather than part of the sheet** — and a test now asserts the Compose island and the frozen `.page` block name the same set, in both directions.

**What it costs, recorded rather than smoothed over.** The editor's sheet is V2's `#F7F2E7`; the Read/Proof screen's is still V1's `paper` (`#EDE6D9` in dark). Both are lit, so the ruling holds on both screens — but they are not the *same* lit, and in dark the same page is about 4% brighter in the editor than in Read. That is the V1/V2 coexistence of [ADR-071](../DECISIONS.md#adr-071), not this ruling, and it closes when Phase D brings the Proof onto V2. **`v2-proof.html` dims its own `.page` by the same mechanism and is NOT amended here** — a different frozen surface, whose shipped screen is already correct; it is Phase D's to amend, under this ruling.

---

#### The ruling — owner, 2026-08-02 (OD-11) {#d-034-ruling}

> The frozen `.ctx` specifies the **contextual editing vocabulary**. The shipped `EditorContextBar` specifies an **accessibility-preserving transform affordance**. **These are not mutually exclusive.**
>
> **Option (b) — keep both.** The frozen contextual toolbar is **additive**. The existing transform bar remains because it is an accessibility conformance path ([ADR-029](../DECISIONS.md#adr-029) §6 / WCAG 2.5.7) and **must not be removed or weakened during a parity phase**. Accordingly: the frozen `.ctx` is implemented as specified; the existing transform controls remain available; **no existing accessibility capability is removed; no existing editor capability is removed.**
>
> Also accept the review recommendation — **option (e)**: split C2 into **C2a** (selection, handles, focus state, the contextual selection framework) and **C2b** (the contextual toolbar, `.ctx*`). The fence applies **only to rows 2.10–2.13**. **C2a is fully unblocked.**

**What this settles.** The premise the entry could not decide for itself — whether two bars in one slot is a parity failure or a parity *requirement* — is answered: during a phase whose objective is parity, an accessibility path already in the product is not a candidate for removal, and a frozen surface that omits it was never describing it. This is the same reasoning OD-9 used to keep redo, now applied to a conformance path rather than a convenience, which is why it lands the same way and harder.

**Consequence for the packages.**

| | |
|---|---|
| **C2a** — `.el*`, `.sel`, `.handle*`, `.content.focusing`, `@keyframes mat` | ▶ **unblocked.** No entry fences it |
| **C2b** — `.ctx*`, [ADR-089](../DECISIONS.md#adr-089) rows 2.10–2.13 | ▶ **unblocked by this ruling**, and now carries an explicit non-removal invariant: C2b adds the verb bar and **leaves `EditorContextBar` reachable**. Its FPT must assert that the transform controls survive the package, and the assertion's mutation is *delete them* |
| **C4** | unchanged — already released by OD-9 |

**The one thing implementation must now get right.** Two bars that both appear on selection and both sit at the bottom is a *composition* the freeze does not draw, and the ruling makes it required rather than forbidden. How the two occupy the slot — stacked, switched, or one summoning the other — is not specified by either source, and C2b's own pre-implementation check owes that question an answer before its production code. It is a **layout** question, not a capability one; if answering it needs a new capability, that is a new entry, not an implementer's call.

**Owner decision requested.** Which of the four, or another. Note that **OD-9's own logic points at (b)**: it kept redo on the reasoning that the frozen bar specifies the editing surface rather than the product's full capability, and the transform controls are the same kind of omission — a shipped capability the prototype never had to model. That is a reading, not a ruling, which is why C2 stops here instead of proceeding on it.

**Precedent.** [D-033](#d-033) was raised the same way by C1's own pre-implementation check and ruled the same day. The check is doing what [ADR-085](../DECISIONS.md#adr-085) change 2 exists to make it do — finding the decision before the code rather than after it.

---

### D-032 — the keep-clear cue has a frozen appearance, no trigger, and a written trigger the product cannot compute {#d-032}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:101-103`, `:292` · [V2-BENCH-IA-INTERACTION.md §A.4](V2-BENCH-IA-INTERACTION.md) · [BP-4](V2-BENCH-PRINCIPLES.md) |
| **Found** | 2026-08-01, during **Phase C planning** ([ADR-089](../DECISIONS.md#adr-089)) |
| **Severity** | **A frozen state with no reachable trigger** — affects package **C1**; the resting cue is unblocked |
| **Status** | ✅ **RESOLVED 2026-08-01** by owner ruling (OD-10, C1 half) — see [the ruling](#d-032-ruling) |

**What is frozen and complete.** `.keepclear` at rest: `inset:16px`, `1px dashed var(--ink-faint)`, `border-radius:3px`, `opacity:.32`. It is in the markup (`:292`) and it transitions on `opacity .3s, border-color .3s`. Nothing about the resting cue is ambiguous, and nothing like it exists in the repository — `grep -ri "keep.\?clear"` over `core`, `feature` and `app` returns zero, so the whole cue is net-new.

**What is not.** `.keepclear.warn` — `opacity:.9`, `border-color:var(--strawberry-text)` — is declared at `:103` and **the class is never added anywhere in the file's 254 lines of script**. The frozen prototype cannot enter its own warn state.

The written authority does specify the trigger, and that is where the problem is. [§A.4](V2-BENCH-IA-INTERACTION.md): *"faint and warm at rest, brightening **only when text or a face crosses it**"*. Two clauses, two very different costs:

- **text crossing the inset** is computable today — the layout is known, the inset is known, the intersection is arithmetic over the existing `SharedTextLayout` bounds. This half is implementable as specified.
- **a face crossing the inset** requires **face detection on the user's photo**. The product bundles no such engine; the nearest ones are ML Kit (a Google Play Services dependency) or a bundled model (APK weight and a second decode path). It is also the first feature in Zinely that would *analyse the content of a user's photo*, which is not a network question — the analysis is on-device — but it is squarely a **privacy-principle** question, and [PRD §5](../PRD.md#5-product-principles-non-negotiable) is where that is decided, not here.

**Why this is not "just implement the text half".** Shipping the text clause alone gives a cue that fires for a title over the trim and stays quiet for a face over the trim — the case a maker is most likely to regret at the fold, and the one [BP-4](V2-BENCH-PRINCIPLES.md) names first. A cue that is right half the time teaches the maker to distrust it, which is worse than a cue that is honestly scoped to text.

**Owner decision requested.** (a) The warn state fires on **text only**, and the design's *"or a face"* is amended to say so. (b) Face detection is in scope for the Bench, with its dependency and privacy consequences ruled explicitly. (c) The warn state is deferred whole and C1 ships the resting cue alone, with the entry left open against the phase that takes it.

#### The ruling — owner, 2026-08-01 (OD-10, C1 half) {#d-032-ruling}

None of the three offered options was taken. The ruling re-frames what the warn state *is*, and the
face-detection problem dissolves rather than being decided:

> **The warning state is NOT a continuous editing indicator.** It is shown only while the user's current
> interaction would cause content to enter the required keep-clear area.
>
> - idle → no warning
> - content already inside the keep-clear area after editing finishes → no persistent warning
> - dragging, resizing or editing that would move content into the forbidden region → warning shown live
> - once the interaction ends, the warning disappears
>
> **The warning is therefore transient guidance, not document state.**

**Why this closes the face question without answering it.** The trigger is the *geometry of the element
being manipulated* against the keep-clear rect — the element's bounds, not what the element depicts. A photo
is tested as a box like any other element, so nothing needs to know whether a face is inside it. The written
*"text or a face"* of [§A.4](V2-BENCH-IA-INTERACTION.md) is superseded for implementation purposes: no
content analysis of any kind is performed, no detection engine is added, and [PRD §5](../PRD.md#5-product-principles-non-negotiable)
is not engaged. The corpus wording is left as written and this entry is its authority — the design says what
the cue *means*, the ruling says when it *fires*.

**Why "transient, not document state" is the load-bearing half.** A cue that persisted while content sat in
the keep-clear area would be a standing accusation about a document the maker has already decided to keep —
and, since backgrounds bleed freely by [BP-4](V2-BENCH-PRINCIPLES.md), often a wrong one. Scoping it to the
live interaction makes it a *nudge during the act*, which is what BP-4's "felt, not taught" asks for. It also
means the warn state holds **no persisted or reducer state**: it is derived per frame from the in-flight
gesture, in the same family as the render-only snap guides ([ADR-029 §5.4](../DECISIONS.md#adr-029)) and
explicitly *not* an undoable document fact.

**What it does not settle.** The ruling gives the warn state's *trigger*. It does not say what rectangle the
keep-clear cue draws — see [D-033](#d-033), raised the same day, which blocks the resting cue this one sits on
top of.

---

### D-033 — the frozen page is not the document's page, and the keep-clear inset is not the document's safe area {#d-033}

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) `:97` (`.page`), `:101` (`.keepclear`), `:105` (`.guide.v`), `:107` (`.pagenum`) · [`SingleSheet8Imposer.kt`](../../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/SingleSheet8Imposer.kt) · [`Imposer.kt`](../../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/Imposer.kt) `:28` · [`LayoutValidator.kt`](../../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/LayoutValidator.kt) `:85` · [BP-4](V2-BENCH-PRINCIPLES.md) · [ADR-012](../DECISIONS.md#adr-012) |
| **Found** | 2026-08-01, at the start of **C1**, by the pre-implementation blocker check ADR-089 requires |
| **Severity** | **Blocked package C1** — it decided the geometry every other C1 property is positioned against |
| **Status** | ✅ **RESOLVED 2026-08-01** by owner ruling — option (c), **the frozen Bench amended**. See [the amendment](#d-033-amendment) |

**The two numbers do not agree, and [ADR-089 row 1.5](../DECISIONS.md#adr-089) already half-says so.** That row
notes *"the 212:326 ratio is **not** the document's trim; it is the frozen depiction of it"* — correctly. What
no document says is **which of the two C1 actually draws**, and the answer changes every pixel in the package.

| | frozen depiction | the document's real geometry |
|---|---|---|
| page | `212 × 326` px, ratio **0.6503** | panel `210.4725 × 297.638` pt, ratio **0.7071** |
| keep-clear | uniform `inset:16px` → **7.55 %** of width, **4.91 %** of height | `safeAreaInsetPt = 17.0` (≈6 mm) → **8.08 %** of width, **5.71 %** of height |

The panel is `A4.landscape()` tiled 4 × 2 by [`SingleSheet8Imposer`](../../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/SingleSheet8Imposer.kt)
— `841.890/4 × 595.276/2`. Both shapes are portrait, so this is not an orientation error; they are simply a
different page, about 8.7 % apart in aspect.

**Why the mismatch cannot be scaled away.** A uniform inset stays uniform only when the box it insets shares
the aspect ratio of the geometry it depicts. Draw the *frozen* `212 × 326` box and depict the real 17 pt safe
area truthfully, and the inset must become **non-uniform** — ≈17.1 px on the sides, ≈18.6 px top and bottom —
which the frozen `inset:16px` is not. So there is no implementation that keeps the frozen page box *and* tells
the truth about trimming. The choice is forced:

- **(a) Literal parity.** Draw the page at the frozen `212 × 326` proportion with a uniform 16 dp keep-clear.
  Matches the frozen file exactly and satisfies the FPT's planned probe and its `16 → 0` mutation. The cue then
  depicts **no real boundary** — it is decorative, and sits ~0.5 % of the page width inside where the real safe
  area falls on one axis and ~0.8 % on the other.
- **(b) Semantic parity.** Draw the page at the document's real panel aspect and the keep-clear at the real
  `safeAreaInsetPt` scaled by the same `screenPxPerPt` every other canvas layer already reads. The cue then
  means what [BP-4](V2-BENCH-PRINCIPLES.md) says it means, and the page box matches what will actually print —
  at the cost of differing from the frozen depiction in both ratio and inset, which the goldens must state.

**Why this is not implementation's call.** [BP-4](V2-BENCH-PRINCIPLES.md) defines this cue as the whole of the
product's print-correctness story — *"the three pro print boundaries collapse into one soft keep-clear inset"* —
and [ADR-012](../DECISIONS.md#adr-012) makes the safe-area inset a print-correctness invariant, enforced in code
by `LayoutValidator`'s `SAFE_NOT_IN_PANEL` check. Choosing (a) ships a print app whose only print-safety line is
decorative; choosing (b) knowingly departs from a frozen file in a phase whose objective is parity. Either is
defensible and neither is inferable — the corpus states the appearance, the engine states the boundary, and no
accepted ADR or ruling states which governs when they disagree.

**It is not confined to the keep-clear.** The same question decides:

- whether the page box is a **fixed dp size** or is driven by `uiState.view.screenPxPerPt`, which
  [`PagePreview`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/PagePreview.kt),
  `SnapGuides` and `SelectionChrome` already share as one viewport — a fixed-size page would put the frame and
  the render on two different scales, the exact divergence
  [`EditorScreen.kt:610-621`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt)
  documents as already having been fixed once;
- what `.guide.v`'s *"full height less 8px"* (row 1.10) and `.pagenum`'s `top:7 right:10` (row 1.11) are measured
  against, since both are frozen in the depiction's pixels;
- and, transitively, [D-032](#d-032)'s warn state, whose trigger is an intersection test against whichever
  rectangle this ruling picks.

**Owner decision requested.** (a) Literal parity — the frozen `212 × 326` box and a uniform 16 dp cue; the cue is
decorative and the register records that it is. (b) Semantic parity — the real panel aspect and the real
`safeAreaInsetPt`, with the divergence from the frozen depiction stated in C1's golden KDoc exactly as
[OD-4](../DECISIONS.md#adr-089) required for the typeface. (c) A design amendment that re-draws the frozen page
at the document's real aspect, making the two agree in the corpus rather than in Compose — the [D-024](#d-024)
precedent, and the only option that leaves nothing diverging.

#### The amendment — owner ruling, 2026-08-01 {#d-033-amendment}

**Option (c).** *"The page depicted by the frozen Bench must match the real document geometry rather than
remaining a stylised approximation."* This is the **third** amendment to a frozen V2 surface, after
[D-024](#d-024) (which added design) and [D-010](#d-010-amendment) (which corrected a token). It is the first
that changes a **dimension**, and it follows D-024's precedent exactly: the corpus is edited first, and Compose
implements the corpus — never the reverse.

Two declarations changed in [`v2-bench.html`](mockups/v2-bench.html):

| | before | after | why that number |
|---|---|---|---|
| `.page` (`:117`) | `212 × 326` px, ratio **0.6503** | **`229 × 324`** px, ratio **0.70679** | the real panel is `210.4725 × 297.638` pt (ratio 0.70714) — A4 landscape tiled 4 × 2 by [`SingleSheet8Imposer`](../../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/SingleSheet8Imposer.kt). At height 324 the ideal width is **229.11 px**, so 229 is **0.11 px** off — sub-pixel |
| `.keepclear` (`:121`) | uniform `16px` — depicting nothing | **uniform `18.5px`** | `safeAreaInsetPt = 17.0` scaled by the page box: **1.08803** px/pt horizontally (229/210.4725) and **1.08857** vertically (324/297.638), giving 18.496 and 18.506 px |

**The two axes agreeing to 0.01px is the whole point.** A uniform inset is only honest on a box that shares the
aspect of the geometry it depicts; on the old 212 × 326 page the same 17 pt needed ≈17.1 px sideways and ≈18.6 px
top-and-bottom, and no single number was truthful. Fixing the aspect is what *earns* the uniform inset — the two
halves of this amendment are one change, not two.

**Why 324 and not 297.** The obvious choice was `210 × 297` — ratio 0.70707, a near-perfect 1 px ≈ 1 pt depiction,
and pleasingly the millimetre figures of A4. It was rejected on evidence: the frozen content stack (title 23px/1.15
+ photo 96 + body at `22ch` + sticker 40, with three 12px gaps) measures **≈270 px**, and a 297-tall page leaves a
content box of 253. It would have **overflowed the frozen prototype** — an amendment that breaks the artifact it is
amending. 324 keeps the content box at 185 × 280 and is **2 px shorter** than the old page, so the canvas budget is
unchanged too.

**What the page box now governs.** Per the ruling, `.page` is the canonical geometry for `.keepclear`, `.guide`,
`.pagenum`, [D-032](#d-032)'s intersection test, and the Compose `PagePreview` / `SnapGuides` / `SelectionChrome`
viewport. In practice that means the Compose page is driven by the one `screenPxPerPt` those layers already share
rather than by a fixed dp size — the divergence
[`EditorScreen.kt`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt) already
documents as having been fixed once, and which a fixed-size page would have reintroduced.

**Untouched.** Colours, shadows, type, motion, script, and `.content`'s 22px inset — which still sits *inside* the
keep-clear (22 > 18.5), as it did before (22 > 16). Every `v2-bench.html` line citation in the corpus moved **+20**
below `:10` and was re-anchored and re-verified against the selector it names.

**One observation, deliberately not acted on.** The Proof's `.pageStage` is `206 × 300` — ratio **0.6867**, also not
the panel's 0.7071, though far closer than the Bench's was. The ruling names the Bench, and *"update only the
planning/specification documents required by this ruling"*, so the Proof is **not** amended here. It is recorded for
the phase that meets it (**Phase D**), and this paragraph is that record.

---

## Resolved

| ID | Defect | Resolved |
|---|---|---|
| **D-033** | The frozen page is not the document's page, and the keep-clear inset is not the document's safe area | 2026-08-01 — owner ruling, option (c): **the frozen Bench is amended**, on the [D-024](#d-024) precedent. `.page` 212×326 → **229×324** (ratio 0.70679, 0.11px off ideal) so the depiction carries the real panel's aspect; `.keepclear` 16px → **18.5px**, the engine's `safeAreaInsetPt = 17.0` scaled — the two axes agreeing to 0.01px is what licenses one uniform number. The page box is now canonical geometry for `.keepclear`, `.guide`, `.pagenum`, D-032's intersection test and the Compose viewport. [Amendment](#d-033-amendment) and entry kept above. |
| **D-035** | The dark theme dims the sheet, and the document's own ink does not follow it | 2026-08-02 — owner ruling (**OD-12**), option (a): *"the editor represents the physical printed artifact"*, so the artifact **does not dim** and only the room around it may. The frozen `.page` becomes a light-theme island restating eight on-paper tokens; `.phone` still dims; the five ADR-055 content inks are untouched and print fidelity is unchanged. The sheet's **shadow stays the room's** — lightening it reinstated [D-010](#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark) inside this fix, caught in review. [Ruling](#d-035-ruling) and entry kept above. |
| **D-037** | The dim shipped without either of the two ways the freeze gives the user out of it | 2026-08-02 — owner ruling (**OD-13**), option **(a)**: **selection is a transient editing state, not a modal one.** A tap anywhere outside the selection dismisses it — blank paper, the studio desk, or another element (which **transfers**, in one reduction, with no intermediate clear). No confirmation step, no persistent selection mode. Scoped by the owner as *completion of an existing capability, not a new feature*: `Intent.ClearSelection`, its reducer branch, the selection state and their tests were all already in the repository, and only the interaction that dispatches them was missing. Implemented as one `onTap → Intent.SelectAt` ([ADR-091](../DECISIONS.md#adr-091) row 2.14), which suffices because `SelectAt`'s hit-test **miss** branch already reduces to `ClearSelection`'s exact state. Raised, and closed, by the device pass that exists for it. [Ruling](#d-037-ruling) and entry kept above. |
| **D-034** | The frozen contextual bar and the shipped one are different controls, and the shipped one is an accessibility conformance path | 2026-08-02 — owner ruling (**OD-11**), option **(b) keep both**: the frozen `.ctx` is the contextual editing **vocabulary**, `EditorContextBar` is an accessibility-preserving **transform** affordance, and *"these are not mutually exclusive."* The frozen bar is **additive**; the transform controls remain, because a parity phase does not remove or weaken a WCAG 2.5.7 path ([ADR-029](../DECISIONS.md#adr-029) §6). Review's option **(e)** also accepted: C2 splits into **C2a** (selection — unblocked) and **C2b** (`.ctx*`), the fence having covered rows 2.10–2.13 only. [Ruling](#d-034-ruling) and entry kept above. |
| **D-031** | The frozen Bench draws four controls that go nowhere, and drops one the product ships | 2026-08-01 — owner ruling (**OD-9**): the freeze specifies the **editing surface, not the whole application flow**. Font and Size stay drawn as contextual affordances and invent no capability (ADR-055 excludes font choice, so Font is **specified-but-unreachable**; Size routes to the shipped Type bar). Read reuses [ADR-086](../DECISIONS.md#adr-086)'s Editor → Proof hand-off, back reuses the existing stack, and **redo is kept** — a control the freeze omits is not thereby deleted. Applying the ruling immediately surfaced [D-034](#d-034), which it does not reach. [Ruling](#d-031-ruling) and entry kept above. |
| **D-032** | The keep-clear cue has a frozen appearance, no trigger, and a written trigger the product cannot compute | 2026-08-01 — owner ruling (OD-10, C1 half): the warn state is **transient guidance, not document state**. It shows only while an in-flight interaction would move content into the keep-clear area, and disappears when the interaction ends; content already inside after editing draws no persistent warning. None of the three offered options was taken — the trigger became the manipulated element's **bounds**, so face detection is not needed and [PRD §5](../PRD.md#5-product-principles-non-negotiable) is not engaged. [Ruling](#d-032-ruling) and entry kept above. |
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
