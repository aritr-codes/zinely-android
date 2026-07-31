# Compose V2 Implementation — Handover

> **Paste-in brief for a fresh Compose implementation session with no prior context.** It summarises months of
> design work so a new session can begin immediately, faithfully, without reconstructing anything. Read this, then
> the five documents it points to, then open the frozen HTML for the screen you're building.

---

## 0. Where the work actually is (2026-07-31)

**Phase A — Foundation — is CLOSED. Its gate passed on 2026-07-30 by owner ruling.** Nine implementation
packages (A1–A9), nine ADRs ([ADR-071](DECISIONS.md#adr-071)…[ADR-079](DECISIONS.md#adr-079)), each
independently reviewed and approved, plus A10 (documentation, [ADR-080](DECISIONS.md#adr-080)). Do not
rebuild it. The full record of what was built, what was built
*differently* than this document plans, and why, is the
[Phase A completion record](COMPOSE-V2-ROADMAP.md#phase-a-completion-record-2026-07-29) — **read that
before §6 below**, because §6 describes the plan and the record describes the outcome.

**What exists now:** a V2 design foundation in `:core:ui` — colours, maker inks, typography, shape and
elevation, motion, the paper grain, 36 icons, an accessibility control seam and canvas node tree, and a
debug-only catalog verified pixel-by-pixel against [V2-TOKENS.md](design/V2-TOKENS.md). No product screen,
no route, nothing user-facing.

**Two things Phase A did not do, by design:** V2 tokens currently sit **beside** V1's rather than replacing
them, and `config/token-enrolment.txt` enrols zero packages. Convergence happens surface by surface across
Phases B–D, each package enrolling in the same commit that migrates it. This is **scheduled duplication**,
confirmed as the migration architecture by the D-016 ruling — not drift, and not a parallel design system.

**The gate, and how it closed.** Phase A's criterion *"everything routes through tokens"* requires editing
V1 product components, which Phase A forbids; it is therefore **re-seated to Phase D** by owner ruling
([**D-016**](design/V2-SPEC-DEFECTS.md#d-016--two-of-phase-as-acceptance-criteria-cannot-be-met-by-a-phase-forbidden-to-touch-product-surface),
2026-07-30). The companion criterion *"no duplicate design system"* is **met by confirmation** of the
migration architecture — [ADR-080](DECISIONS.md#adr-080), now `Accepted`. Two further rulings landed with
it: **D-002** fixes the cover-title contrast floor at **3.0:1** with no design change, and **D-006** deleted
the dead `--r:18px` token from the frozen Bench and Proof. Nothing from Phase A is awaiting an owner.

**Where Phase B is.** It **started on 2026-07-30** by owner GO, split into five packages
([Phase B packages](COMPOSE-V2-ROADMAP.md#phase-b-packages-sequencing-is-the-implementers-call-the-phases-criteria-above-are-unchanged)).
**B1 — the Maker's Cover — is built, independently reviewed (GO WITH FIXES, fixes applied), and committed**
([ADR-081](DECISIONS.md#adr-081), `Accepted`): a new `com.aritr.zinely.feature.library` package plus
`Modifier.zinelyV2Shadow` in `:core:ui`.

**B2 — the shelf — is built, independently reviewed (GO WITH FIXES, fixes applied) and committed**
([ADR-082](DECISIONS.md#adr-082),
`Accepted`): `ZineShelf` + `ZineShelfItem`, two fixed columns under a "Your shelf" heading that **scrolls away
with the covers** because the frozen markup makes it a full-width cell inside the scroll rather than a bar above
it. It **paints no ground** — `.shelf` declares no background, so the desk is B5's screen to fill — and it
defers `.zine` (press transform, focus ring, tap) whole to **B3**, which is safe because a resting `.zine`
paints nothing. B2 raised **D-020** and it was **ruled the same day**, costing no rework.

**B3 — interaction — is built, independently reviewed (GO WITH FIXES, fixes applied) and committed**
([ADR-083](DECISIONS.md#adr-083), `Accepted`). It adds `ZineOnShelf` (the two gestures, the press transform,
the focus ring, and the always-visible `⋯`), `ZineActionSheet` + `ZineActionScrim` + `ZineAction` (five rows
over a scrim, with the zine's format and date disclosed **there** rather than on every cover), and a second
gesture on `:core:ui`'s `Modifier.zinelyV2Control`.

**B4 — the empty state and the dock — is built, independently reviewed (GO WITH FIXES, fixes applied) and
committed 2026-07-31** (`97744e6`, [ADR-084](DECISIONS.md#adr-084), `Accepted`). It adds `ZineShelfEmpty` (the
loose sheet → arrow → little book transformation, a serif line and two of body copy) and `ZineDock` (the band
that fades up into the desk, and the "Make a zine" button standing in it).

**B5 — the screen — is BUILT and STOPPED at the pre-commit gate** ([ADR-086](DECISIONS.md#adr-086),
`Proposed` — independently reviewed **GO WITH FIXES**, fixes applied; awaiting owner acceptance, and **nothing is committed**). Its
[frozen property table](DECISIONS.md#adr-086-fpt) — the first one this programme has written, under
[ADR-085](DECISIONS.md#adr-085) — found, *before* any code, that **8 of its 23 rows had no frozen source**, and
raised **[D-024](design/V2-SPEC-DEFECTS.md#d-024)**, **[D-025](design/V2-SPEC-DEFECTS.md#d-025)**,
**[D-026](design/V2-SPEC-DEFECTS.md#d-026)** and the `token-enrolment.txt` conflict.

**What B5 actually changed.** The app's **Home route now hosts `ZineLibraryScreen`** — the first user-visible V2
surface — with four states (Loading · Error · Empty · Content), the dock standing in all four, and the existing
create / rename / duplicate / delete-with-undo flows reused whole (D-025). Covers are now **persisted**: the
cover types moved to `core:model`, `ProjectMeta` and the Room index each gained `coverSurface`/`coverStamp`, and
the index moved to **schema v2 via an additive `Migration(1,2)`** rather than destructive fallback. V1's
`HomeScreen` and its suite are untouched and still green; what changed is which screen the route hosts.

**Three things B5 learned that outlive it.** *(1)* Its tests found a **real production defect on their first
run** — `syncRowFromDisk` built the returned `ProjectSummary` a second time by hand beside the row it had just
written, and the hand-built one dropped the new field, so a freshly created zine came back **coverless** while
disk and index both held its cover. Two construction sites for one projection is the defect; one site is the
fix. *(2)* Mid-package review found **two rows terminating on nothing** — one ✅ with no test file at all, one ✅
on a *code comment* — which is [ADR-087](DECISIONS.md#adr-087)'s whole purpose, caught by the ruling it was
written for. *(3)* `Modifier.windowInsetsPadding` is **consumption-aware**, so the obvious "a second consumer"
mutation is an **equivalent mutant**; the assertion that actually holds the inset measures the *workspace*, not
the button. **13 mutations applied, 13 killed; 8 goldens recorded** (four states × two themes). B5 raised one
new defect, **[D-027](design/V2-SPEC-DEFECTS.md#d-027)** (the metadata line's vocabulary), which does not block
it, and reported one **pre-existing** gap it did not cause and did not fix:
`EditorCoverageNoticeGoldenTest` (A9, `b0f2ad1`) has **no recorded rasters at HEAD**.

**All four were ruled the same day and all four are closed. No table row is blocked.** D-025: *reuse the
existing flows — and **Share & export is a route into the Proof**, not a shelf-level export.* D-026: *a
duplicate generates a **new** cover — duplicate content, not visual identity — and **legacy zines get a cover
on first presentation, then persist it**.* Enrolment: *struck from B5 and re-seated to Phase D on the
[ADR-080](DECISIONS.md#adr-080) precedent; **D-007 untouched**, `TokenDisciplineTest` untouched.* And
**[D-024](design/V2-SPEC-DEFECTS.md#d-024-ruling): Loading and Error are product states and belong in the
canonical design** — so `v2-library.html` was **[amended](design/V2-SPEC-DEFECTS.md#d-024-amendment)** to add
them, the first V2 amendment that *adds* design rather than deleting dead specification.

**Read the amended file, not your memory of it.** It now carries an `AMENDED` block in its freeze header and
four states rather than two: `.ph` / `body.is-loading`, `.fail` / `.retry` / `body.is-error`, plus two new
prototype toggles. Everything frozen on 2026-07-27 is unchanged. Two rulings ride with it: **the dock stands in
all four states** (it belongs to the *workspace*, not the loaded content — there is one workspace grammar), and
the **loading debounce is implementation, not design**, deliberately absent from the HTML and owned by B5 as a
seam ([ADR-086](DECISIONS.md#adr-086)).

**Read those three rulings before writing B5, because three of the four went against the reading an implementer
would most plausibly have taken.** A duplicate does *not* inherit its cover — two identical covers on a
covers-only shelf cannot answer *"which zine is mine?"*, and [ADR-083](DECISIONS.md#adr-083) moved every
distinguishing detail into the action sheet. "Share & export" is a *route*, not a capability. Loading and Error
went to the **design corpus**, not into Compose. Each of those, found after implementation, would have been
rework of a screen plus its tests, its mutations and its goldens.

**Two consequences that are easy to miss.** *"Reuse the existing delete flow"* means the **undo comes with it**
([ADR-046](DECISIONS.md#adr-046) §4) — a V2 shelf that deleted immediately would be a new concept, not a reused
one. And Share & export must push `EditorRoute` **then** `ProofRoute`: the Proof resolves the shared ViewModel
off the editor's live back-stack entry ([ADR-026](DECISIONS.md#adr-026)), so a direct navigate to the Proof
**throws at runtime**.

**The one-sentence version a fresh session needs: the frozen Library is a prototype with six hard-coded zines,
so it never reads a store, never waits, never fails, and never navigates anywhere.** B1–B4 never met that,
because each of them built a *thing on the screen* and the freeze draws those completely. B5 builds the
*screen*, and a screen is made of states and destinations — which is the half a design prototype does not have.
Expect the same shape at the first integration package of every phase, not just this one.

**And this is what the frozen property table is for.** Every earlier package found its design questions *while
implementing them* — B4 found four that way and shipped an ADR trying to settle one of them itself, which
review rejected. Listing every frozen property **and its source** before writing code surfaces the properties
that have **no source** while the cost of finding out is still a paragraph. Eight gaps, found in a planning
pass, with no tests, no mutation battery and no goldens yet built on top of the answers — and **all eight were
closed within a day of being asked.**

**And it produced a workflow rule** ([ADR-087](DECISIONS.md#adr-087)): all eight gaps were labelled *"blocked"*,
and that one word was doing four different jobs — two needed a **design amendment**, four needed a **routing
ruling**, one an **identity ruling**, and one was **not the package's work at all**. Every frozen-property-table
row now terminates in exactly one of four named states, because the state names *who owes what* and "blocked"
does not.

**Three things about B4 a fresh session will otherwise re-derive.** First, `.empty` **replaces** the shelf —
`body.is-empty .shelf{display:none}` — so B4 ships both halves and **B5 chooses between them** with real
project data; nothing here composes a grid or a slot. Second, the dock is **inert**: `pointer-events:none` on
the band with `auto` on the button alone, because the band covers the bottom ~150dp of the shelf and a dock
that consumed touches would make the last row of covers unreachable through what looks like empty desk.
Third, `.start` has **no handler in the frozen file at all**, so the CTA reports the press and routes nowhere
— the paper chooser is B5's hand-over, on exactly the reading ADR-083 applied to the sheet's five rows.

**Three of B4's four design questions were already ruled**, which is what a working register looks like:
**D-005** names `.empty h2` by selector (so the headline is Fraunces **500**, not the file's stale 600),
**D-011** names `.start` by line (so the press rides `ZinelyV2Standard`, not the file's bare `ease`), and
**D-021** covers the `＋` — U+FF0B, the *fullwidth* plus, absent from all seven bundled faces, drawn by the
platform's fallback.

**The fourth is [D-023](design/V2-SPEC-DEFECTS.md#d-023), it is open, and how it got there is the lesson.**
`.start{color:var(--paper)}` where the Bench and Proof use `--on-matcha`. B4 **decided this itself** and wrote
the reasoning into an ADR: unlike D-005/D-011/D-022 the value is not *broken* — declared in both themes,
inverts with them, clears AA both ways at **5.20:1** light (`#F7F2E7` on `#5E6B2F`) and **5.12:1** dark
(`#2F2A22` on `#93A257`) against `--on-matcha`'s own 5.80 and 5.72 — so, it argued, *a divergence is only a
register entry when the Library's version cannot work.*

Independent review rejected that, and a fresh session should read why rather than re-derive it. **The test
does not describe the rulings it claims to distinguish.** D-005's Georgia stack rendered perfectly well and
D-011's `ease` is a perfectly valid curve; neither was broken. Both were ruled stale on **authorship date** —
the Library was frozen before the corpus it now sits beside. D-022's ruling then wrote the general form down
(*"where the Library file contradicts a token the corpus publishes, the corpus wins"*) and said **a fourth
will appear**. And B3, holding *two* rulings pointing one way, still declined to act on them: *"two rulings
pointing the same way are a strong hint, not a ruling."* B4 held three and a stated rule. **The standing
rule is that measuring something real licenses asking, not deciding** — if you find yourself writing a new
test for when to raise an entry, that is the moment to raise one.

**Two things about B3 a fresh session will otherwise re-derive the hard way.** First, the seam ends in
`clearAndSetSemantics`, so **a control cannot contain another control** — the `⋯` is a sibling of the cover,
and B1's `overlay` slot on `ZineCover` was *deleted* rather than worked around. Second, the sheet **does not
dismiss when an action is chosen** — and that is a *deferral*, not a transcription: `:195-209` wires the scrim
and the `⋯` and nothing else, so the frozen file specifies **nothing** here, and nothing is not "stays open".
Each action's destination is **B5**'s, and holding still is the narrowest thing an implementation can do.

**B3 raised two defects and both were ruled the same day — opposite ways, which is the lesson.**
[**D-021**](design/V2-SPEC-DEFECTS.md#d-021-ruling): the six frozen marks are literal characters and three
(`✎`, `⧉`, `⋯`) are absent from the bundled Inter, so the device's fallback draws them — **keep them exactly
as frozen**, because *"bundled-font coverage does not justify changing the design"*. Their variation across
devices is specified behaviour for the B5 passes to *record*, not fix.
[**D-022**](design/V2-SPEC-DEFECTS.md#d-022-ruling): the Library's scrim is a theme-invariant literal while
the corpus publishes a theme-aware `--scrim` — **the corpus is authoritative**, so `ZineActionScrim` takes the
token. That makes the scrim **the only V2 value not taken from the frozen Library file**, and it is the third
of a set with **D-005** (serif) and **D-011** (easings): where the Library contradicts a corpus token, the
corpus wins. Both entries reported the same *kind* of finding, and the split is the precedent —
**measuring something real licenses asking, not changing.**

**Goldens in this repository were never assertions until B3.** B1 and B2 *recorded* rasters and never ran
verify, and Roborazzi in record mode **overwrites** rather than compares — so `v2_cover_pressed_light.png` sat
committed and stale, failing `-Proborazzi.test.verify=true` at HEAD. B3 corrected it on owner direction. Two
habits follow: run `-Proborazzi.test.verify=true`, because it is the only run that proves a raster means
anything; and record with a **narrow** `--tests` filter, because a broad `*GoldenTest` silently rewrites
unrelated V1 rasters (it rewrote 37 during B3; all were restored). Two pre-existing verify failures are known
and are not B3's: `EditorCoverageNoticeGoldenTest` has no committed references at all, and the intermittent
V1 `ReframeSessionTest > an unreadable photo is refused entry to reframe`.

**Read [ADR-083's review outcome](DECISIONS.md#adr-083-review) alongside ADR-082's before writing B4's tests.**
B3 ran fourteen mutations against production and **two survived**, both of them B3's own tests: one that
painted its own copy of the scrim literal instead of composing production's, and a `border-radius:20px` that
had no test at all. Independent review then found **two more of the same kind**: the focus ring had no test
whatsoever (offset 6→60dp and radius 9→0dp both survived the whole suite), and the D-021 tofu control was
`U+E000` — **which the bundled Inter actually maps to a real glyph**, so the test that claimed to detect tofu
passed on guaranteed tofu, and the false fact had reached a defect entry awaiting an owner ruling.

Four rules — written for B4, carried to B5 — in the order they cost the most:

1. **Verify the assumption the assertion rests on, not just the assertion.** Every font fact in B3 was
   measured except the control's; the control's was the one that broke.
2. **A test that rebuilds the value it pins cannot fail.** Compose production, don't copy its constants.
3. **A Roborazzi golden in record mode overwrites rather than compares** — it is not an assertion. Any
   property visible only in a raster needs a pixel test of its own.
4. **Ask which frozen properties have no test at all.** That question found the radius *and* the ring; it is
   worth more than re-reading the ones that do.

Two traps that cost an hour each and are not obvious: Compose **declines focus in touch mode**, so
`requestFocus()` fails silently — request `InputMode.Keyboard` (which is what `:focus-visible` means anyway);
and the ground outside a cover is **not** clean, because B1's shadow tints it about ten pixels out.

**B4 added four more, and three of them are about the test harness rather than the design.** They are cheap
to read and were not cheap to find:

5. **`Modifier.padding` on a `Text` does not appear in that node's semantics bounds.** Compose reports the
   bounds *inside* the padding, so a claim about a CSS margin has to be asserted as a **displacement** from
   something else, never as a taller box. B4's arrow test was written the wrong way round first.
6. **The Compose rule accepts one `setContent` per test.** Every yardstick a test needs — a reference
   rendering, a second theme, a `ch` measure — must stand in the *same* composition as the subject, or be
   its own `@Test`.
7. **A CSS margin on a centre-aligned flex item moves it by half its value**, because it is the margin *box*
   that gets centred. `Modifier.offset` would move it the whole way and be wrong by a factor of two.
8. **Run one mutation driver, and do not edit the sources while it runs.** B4's first battery outlived an
   aborted tool call and overlapped its own re-run: two processes editing the same two files reported a
   survivor that was not one and two constant failures that belonged to the other process's edits. The
   results looked exactly like findings. The script now takes a lock — but the lock only stops a *second
   driver*, and B4 then hit the same class again from the other side by editing a KDoc **while** a driver
   held the file, which left a live mutation (`.clickable {}` on the dock) in the tree. Two rules, not one:
   a mutation result is only evidence if you know what was in the file when the test ran, and a driver's
   files are **off-limits to you** until it exits. Docs are safe to edit; sources are not.

**B4's review added three more, and the first is the most valuable thing the package produced.**

9. **A pixel test that measures a bounding box cannot resolve one pixel.** `(firstInkRow + lastInkRow) / 2`
   quantises — a 1px shift moves the first row by one and the last by zero once antialiasing is counted, so
   it reports **half** the displacement. Use a **luminance-weighted centroid**, which moves continuously.
10. **Never anchor two rendered subjects on their own containers' centres.** A container of odd height puts
    `center.y` on a half-pixel and one of even height does not, so the comparison carries half a pixel of
    pure parity noise. Give the reference the **subject's own structure** and measure each against something
    inside itself — B4's plus is compared to the label beside it, in both rows, so every property of the
    container cancels.
11. **Distinguish an equivalent mutant from a gap.** Two of B4's survivors cannot be killed by any test:
    Skia clamps an overflowing corner radius exactly as CSS does, and under `justify-content:center` only the
    *difference* between a top and bottom padding reaches the layout. Record them as equivalent, with the
    evidence — otherwise the next session writes a flaky test to chase a value that has no effect.

**Read [ADR-082's review outcome](DECISIONS.md#adr-082-review) too.** B2's own
ten-mutation battery passed while the grid was fed `zines.reversed()` — the sixth package in this programme
whose assertions were blind to the defect class their names claimed to gate. The rule that came out of it:
an ordering, mapping or identity claim cannot be tested by asserting that each element exists *and* that
positions ascend, because a permutation satisfies both. The element and its position must meet in one
matcher. Two smaller traps from the same review: `assertEquals(expected, actual, delta)` passes at
`|Δ| == delta`, so a `1f` tolerance accepts an off-by-one pixel; and a doc block that outlives the ruling it
describes is a defect, which B2 both fixed in `ZineCover.kt` and reintroduced in `ZineShelf.kt`.

Both packages are **additive**: V1's shelf keeps its route, so nothing user-visible has changed and the V2
Library becomes the app's Library at **B5**.

**B1 ships no cover assigner.** `ZineCoverSurface`, `ZineCoverStamp` and `ZineCoverRecipe` exist; the
function that picks one for a real zine does not. Independent review found the reflection guard meant to
enforce D-017 ("never derived from the title") could not hold that ruling *regardless of how it was
written* — it checked a parameter's type, and the ruling is about information flow, which no signature
check decides. Rather than patch the guard a sixth time, the assigner and the guard both move to **B5**,
where an actual create-and-persist call site exists to check directly. **Do not re-add an assigner to B1's
package** — build it in B5, next to the persisted field.

**B1 raised three defects and all three were ruled and applied on 2026-07-30** — and they are the rulings a new
session most needs, because each states what a *printed object* is rather than how to draw one:

- [**D-017**](design/V2-SPEC-DEFECTS.md#d-017-ruling) — a cover is **assigned once at creation and persisted**.
  Not derived from the title (a rename must not repaint a physical object), not round-robin, not inferred from
  neighbours. It supersedes [ADR-069](DECISIONS.md#adr-069)'s title-hash mechanism for V2 covers, and it makes
  the persisted surface+stamp field a **hard prerequisite of B5**.
- [**D-018**](design/V2-SPEC-DEFECTS.md#d-018-ruling) — **omit** the ink band below API 29; no emulation, no
  substitute blend mode. Same ceiling as D-014's grain, so both are **one** Known Limitation.
- [**D-019**](design/V2-SPEC-DEFECTS.md#d-019-ruling) — a **printed artifact never mirrors**; chrome may. Already
  answers B2's grid, Phase C's page sheets and Phase D's imposed sheet.

One item is still owed a ruling and is reported in the roadmap rather than the register: Phase B's *"8pt"*
spacing criterion contradicts the **D-007** ruling that no spacing scale is published.

**[D-020](design/V2-SPEC-DEFECTS.md#d-020--the-shelf-states-a-fixed-two-column-grid-with-no-breakpoint-and-phase-b-verifies-on-foldables)
is the one to read before touching any layout**, because its ruling is general: *"future adaptive layouts require
a future frozen design rather than implementation inference."* Where the frozen corpus is **silent** rather than
contradictory, silence is not an invitation to interpolate — not from a neighbouring width, not from another
screen, and not from V1's answer to the same question. Concretely for the Library: two columns at every width,
no maximum cover width, and a foldable showing two large covers is *specified*, so the device passes record it
rather than fix it.

**Two defects B2 found. One was corrected by owner direction; one is still open:**
- ✅ `ZineCover.kt`'s `@param` block described the title-hash mechanism **D-017** deleted, making committed code
  contradict an accepted ruling. **Corrected** with B2, as documentation only.
- ⏳ V1's `ReframeSessionTest > an_unreadable_photo_is_refused_entry_to_reframe` fails **intermittently** in
  full-suite order — it passed in isolation with and without B2, failed one full `--rerun-tasks` run, and passed
  the next. Pre-existing and order-dependent, not B2's, and **not yet triaged**.

**B1's independent review is complete and reconciled** — [CLAUDE.md](../CLAUDE.md#multi-agent-workflow)'s
*"never self-approves"* held: the review ran across a multi-hour provider outage, was resumed from its
preserved transcript rather than restarted, and was not replaced by self-review at any point. Verdict and
finding-by-finding reconciliation are in [ADR-081](DECISIONS.md#adr-081)'s Decision 7.

---

## 1. The product

**Zinely** is a **privacy-first, offline-first Android app for making small, printable zines** on your own phone.
Kotlin · Jetpack Compose · Material 3 · on-device PDF/image export. **No account, no cloud, no network, no
analytics.** You start from a blank sheet and end with a folded little book you can hold.

The feeling being built: **a quiet café where you make tiny books with your hands** — calm, warm, handmade, and
private. The measure of success (the "Handmade Test"): a user should describe Zinely as *"it feels like making
tiny books in a quiet café,"* not *"it has a nice UI." *

## 2. The design philosophy (one paragraph)

**The interface stays quiet; the creations carry the warmth.** Chrome is restrained (few colours, calm surfaces,
the page is always the hero); all colour, texture, and expressiveness live in the user's zines — paper, inks,
covers — not in the app. This split is the load-bearing architecture: the app can stay calm for years while every
user's shelf becomes more *theirs*. Everything is honest (what you preview is exactly what prints), physical
(paper/press/shelf metaphors, not files/dialogs), and collected (zines are objects on a shelf, not files in a
list). Full statement: **[V2-CONSTITUTION.md](design/V2-CONSTITUTION.md)** — read it first; it is the highest
authority and it outranks everything, including the code.

## 3. The frozen artefacts (the canonical spec)

The design phase is **over**. These are frozen and authoritative — reproduce them, don't reinterpret them:

| Surface | Frozen HTML (the spec) | Authoring intent |
|---|---|---|
| **Library** ("which zine?") | [`design/mockups/v2-library.html`](design/mockups/v2-library.html) | V2 design docs |
| **Bench / editor** ("how do I change this page?") | [`design/mockups/v2-bench.html`](design/mockups/v2-bench.html) | [V2-BENCH-PRINCIPLES.md](design/V2-BENCH-PRINCIPLES.md) · [V2-BENCH-IA-INTERACTION.md](design/V2-BENCH-IA-INTERACTION.md) · [V2-BENCH-REVIEW.md](design/V2-BENCH-REVIEW.md) §E |
| **Proof** ("how do I print it right?") | [`design/mockups/v2-proof.html`](design/mockups/v2-proof.html) | [V2-PROOF-IA-INTERACTION.md](design/V2-PROOF-IA-INTERACTION.md) Part E |
| **Identity** (colour/covers/materials) | [`design/mockups/v2-materials.html`](design/mockups/v2-materials.html) · [`v2-living-audit.html`](design/mockups/v2-living-audit.html) | [V2-IDENTITY.md](design/V2-IDENTITY.md) · [V2-IDENTITY-AUDIT.md](design/V2-IDENTITY-AUDIT.md) |
| **Tokens** (locked palette) | — | [V2-TOKENS.md](design/V2-TOKENS.md) |

**Palette (chrome):** warm `paper #F7F2E7` / `desk #ECE3D1`, `ink #2A251E`, **`matcha #5E6B2F`** (the one action
colour), `strawberry #E98F97` (sparing punctuation), `consequence #A6382A` (delete/error only). Dark = **warm
charcoal, re-derived not inverted.** Type = **Fraunces (voice) + Inter (work)**, permanent. **Maker inks** (on the
*artifact*, never chrome) = the frozen **Bench H4 10-ink set**: Matcha #7C8A3F · Forest #3E5E3A · Strawberry
#E27F89 · Brick #B0503F · Sunflower #E7B53C · Ochre #D19A3C · Aqua #57B0A9 · Cornflower #6E86C9 · Plum #8A5A9B ·
Ink #2A251E.

## 4. The methodology (how you work)

1. **HTML is canonical.** Match the frozen prototype's *result*, idiomatically in Compose (M3, hoisted state,
   `collectAsStateWithLifecycle`, stateless children) — you're reproducing the design, not porting `<div>`s.
2. **No redesign, no interpretation, no feature creep.** If the HTML is wrong, **fix the HTML first** (owner gate),
   then the code — never the reverse.
3. **Deviate only for** platform truth (real a11y/IME/back/haptics), a genuine HTML bug, or *more* accessibility —
   and log every deviation with its reason.
4. **Repository truth beats summaries.** Read the actual file/commit/test/HTML before relying on it.
5. **The Bench and Proof already exist in code** — a real engine you **preserve, not rewrite** (see §6).

**This document is the package entry point.** Start at **§0** — it says where the work actually is — then read
**only your package's section** of the roadmap, **only the ADRs §0 or that section names** (plus any ADR your
package will modify), and — always, whether or not §0 mentions them — the **open entries** of
[V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md), because an open entry is a live owner question that may govern a
value you are about to pin. **Do not read [DECISIONS.md](DECISIONS.md) end to end**: it is ~105k words, a package
touches a handful of entries, and reading it whole costs a large fraction of a session. §0 is the **navigation
layer, not a replacement for the ADRs** — when a decision matters, open that ADR and read it in full. A summary
of a ruling is a claim, and rule 4 above applies to this file too.

**Every package opens with a [frozen property table](COMPOSE-IMPLEMENTATION-GUIDE.md#81-the-frozen-property-table)**
— each frozen property bound to its source, its implementation target, the assertion that will pin it, the
mutation that must break that assertion, and an explicit marker for equivalent-mutant candidates and for anything
deliberately left untested. It lives in the package's ADR, it is written *before* production code, and it is
subordinate to the frozen HTML: a row with no frozen source is not a frozen property.

Full guide: **[COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md)**. One-page opener you re-read
each session — and which now carries the package workflow (verification order, gates, build execution):
**[COMPOSE-IMPLEMENTATION-RULES.md](COMPOSE-IMPLEMENTATION-RULES.md)**. Workflow shape and its evidence:
**[ADR-085](DECISIONS.md#adr-085)**.

## 5. The review process

- **The order the work runs in:** production implementation → focused tests → **the "cannot fail" review** →
  mutation testing → record goldens → verify goldens → independent review → reconciliation → final verification →
  owner approval → commit — and **any test added or changed during reconciliation re-enters at the "cannot fail"
  step** with its own mutation. Full statement:
  [COMPOSE-IMPLEMENTATION-RULES.md §3](COMPOSE-IMPLEMENTATION-RULES.md#3-the-verification-order).
- **Mid-package (mandatory):** the specialised **"find the assertions that cannot fail"** review, run once the
  tests exist and *before* the ADR, the goldens and the review package. A different lens from the per-PR review,
  not a rehearsal of it — B4's narrow pass found three blind assertions the general one missed. Early, because a
  non-discriminating assertion found at the gate invalidates the battery, the rasters and the evidence block
  stacked on top of it.
- **Per PR:** an **independent Review Agent** (never the implementer) validates actual repo state, classifies
  findings **Required Fix / Recommended Improvement / Observation**, and returns **GO / GO WITH FIXES / NO-GO**.
  The implementer reconciles every Required Fix or surfaces the disagreement explicitly.
- **One owner gate per package, and it is the last one:** implement → review → reconcile → **stop** → owner
  approves → commit. The old intermediate approval before *running* the review is gone; it gated a mandatory step
  ([ADR-085](DECISIONS.md#adr-085)). Nothing the owner decides has changed — only how often a package stops to ask.
- **Device verification is mandatory, two passes:** Pass 1 (Developer — is it built right? assert the *platform*
  a11y tree) and Pass 2 (First-time user — would a stranger understand it?). A feature is accepted only when both
  pass; if they disagree, the disagreement is the finding.
- **Per phase:** a review gate (parity screenshots + side-by-side + device passes + no feature creep).

## 6. The implementation phases

Sequence and gates: **[COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md)**. In brief:

- **A · Foundation** — **✅ CLOSED 2026-07-30 (gate passed; [D-016](design/V2-SPEC-DEFECTS.md#d-016--two-of-phase-as-acceptance-criteria-cannot-be-met-by-a-phase-forbidden-to-touch-product-surface) ruled)** — theme, tokens, typography, motion, elevation, icons,
  CompositionLocals, paper system, a11y infra. *No product screens.* The parenthetical this list used to carry —
  *"this foundation is the **same** migration as the V1 conformance token work — do it once, not twice"* — is the
  intent, but it is **not what Phase A could deliver**: converging the two systems means editing V1 components,
  which Phase A forbids. V2 landed additively; convergence is Phases B–D, package by package, and the
  token-routing requirement is now a **Phase D** exit criterion ([ADR-080](DECISIONS.md#adr-080),
  `Accepted`). Do not read this line as a statement about the code today.
- **B · Library** ◀ **next** — pixel parity to the frozen Library. The closest to a clean re-skin; sets the parity bar.
- **C · Bench** — pixel + interaction + animation + editing-behaviour parity, on top of the **existing** engine.
  No feature additions.
- **D · Proof** — pixel + print-flow + fold-guide + a11y parity, for the shipped single-sheet-8 stage.
- **E · Cross-product polish** — make the three feel like one product (motion, transitions, haptics, dark mode).
- **F · Reality validation** — physical devices; only now are tiny fidelity-serving adjustments allowed.

**The Bench/Proof are not greenfield — preserve these:** `CanvasReplayer` (one draw path), `ElementSemanticsLayer`
(canvas a11y), command-undo + `AutosaveCoordinator` + "Saved ✨", direct-manipulation resize handles (48dp), and
the anti-desync viewport defence. The one interaction that is **new-in-V2** (already frozen inside `v2-bench.html`, not a new decision here) is
**in-place text editing with a rigid whole-page pan** on IME insets (page moves as one body, returns pixel-identical) — **conditioned on a device Pass-2
pixel-identical proof; fall back to hardening the bottom-sheet editor if the proof fails.**

## 7. The non-negotiable rules

Constitutional invariants — breaking one is a NO-GO no matter how good the screen looks:

- **One engine, one draw path:** preview == export == read ([ADR-028](DECISIONS.md#adr-028)). No second render path.
- **No per-edit render** — recipes, not cached rasters ([ADR-069](DECISIONS.md#adr-069)).
- **The page never drifts/reflows/resizes while editing** — rigid pan, pixel-identical rest.
- **Never-silent failure + loss-safe back** ([ADR-051](DECISIONS.md#adr-051)).
- **Print honesty** — no fake "Print"; 100% actual size; Save PDF + Share ([ADR-052](DECISIONS.md#adr-052)).
- **READ-first** — the finished-book reveal is Read's, not the Bench's ([ADR-058](DECISIONS.md#adr-058)).
- **Chrome = matcha + strawberry + consequence only** — warmth comes from content, never new chrome colour.
- **Privacy** — no network/analytics/cloud; offline-first; optional asset search sends a keyword only, never content.
- **Accessibility is not optional** — platform-tree truth; every gesture has a named action twin + visible fallback;
  AA gated in CI.
- **Every screen answers its one user question**; a correct answer to the wrong question is a defect.
- **MVI for the Bench** ([ADR-005](DECISIONS.md#adr-005)); clean architecture; sealed `Result`; Hilt/KSP; `jvmToolchain(21)`.

## 8. Definition of done (per change)

Frozen property table closed out · code + tests pass · every planned mutation KILLED or **proven** equivalent ·
goldens pass `-Proborazzi.test.verify=true` · docs updated in the same change (decisions → ADRs) · UI: pixel
parity verified + both device passes accepted · no new network/account/cloud dependency · privacy & offline
invariants intact · the "cannot fail" review run mid-package · reviewed by an independent Review Agent · owner
approved before the commit.

---

## Start here

1. **Read §0 of this document.** It is the entry point: where the work actually is, what the last packages cost,
   which rulings are live. Everything below is what §0 sends you to.
2. Read **[V2-CONSTITUTION.md](design/V2-CONSTITUTION.md)** (why) → **[COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md)** (how) → **your package's section** of **[COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md)** (what, in order). Not the whole roadmap, and **not all of [DECISIONS.md](DECISIONS.md)** — only the ADRs §0 or that section names, plus any your package will modify.
3. Read the **[Phase A completion record](COMPOSE-V2-ROADMAP.md#phase-a-completion-record-2026-07-29)** — what
   already exists, and the four things a new engineer gets wrong that are not visible in the code.
4. Keep **[COMPOSE-IMPLEMENTATION-RULES.md](COMPOSE-IMPLEMENTATION-RULES.md)** open as your checklist — it carries
   the package workflow as well as the rules.
5. Write the package's **frozen property table** into its ADR *before* any production code.
6. Begin **Phase B — Library** on an explicit owner GO. Phase A is closed; building any of it again is the
   mistake this section exists to prevent.
7. For any screen, open its **frozen HTML** first; it is the spec.

*The design is done. Your job is faithful execution. If something feels like it should change, it goes into the
frozen HTML first (owner gate) — not into the code.*

---

*Handover written 2026-07-28 by the Design Custodian at the close of the V2 Design Program. §0 and the Phase A
status added 2026-07-29 (package A10); updated 2026-07-30 at the **Phase A closeout**, when the D-002, D-006
and D-016 owner rulings were recorded and Phase A's gate passed. Updated 2026-07-31 at the **Workflow V2
revision** ([ADR-085](DECISIONS.md#adr-085)): this document became the package entry point, and §4/§5/§8 took the
frozen property table, the verification order, the mid-package review and the single owner gate.*
