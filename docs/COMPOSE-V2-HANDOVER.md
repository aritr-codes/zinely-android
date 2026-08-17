# Compose V2 Implementation — Handover

> **Paste-in brief for a fresh Compose implementation session with no prior context.** It summarises months of
> design work so a new session can begin immediately, faithfully, without reconstructing anything. Read this, then
> the five documents it points to, then open the frozen HTML for the screen you're building.

---

## 0. Where the work actually is (2026-08-06)

**Read this block, then §6, then stop — unless your package is named below.** Everything after it is the record of
Phases A and B, kept because it carries rulings that still govern, not because it is where the work is.

> ### The one-paragraph answer
>
> **PHASE C IS COMPLETE (2026-08-06). C0, C1, C2a, C2b, C3, C4, C5, C6 and C9 are all DONE and ACCEPTED.**
> C0 was documentation-only — no ADR, no device passes. The eight production packages carry
> [ADR-090](DECISIONS.md#adr-090) through [ADR-097](DECISIONS.md#adr-097), **every one `Accepted`, each with both
> device-verification passes recorded**. C9 — Integration was the last: the four Bench states made explicit and
> wired, the reduced-motion policy bound at every call site, and the maker's **place** persisted through process
> death. It closed on the ruling it existed to obtain — **[OD-25](design/V2-SPEC-DEFECTS.md#d-012-ruling) ratified
> the Bench's reduced-motion rule (option a) on C9's hardware evidence**, discharging D-012 and
> [OD-10](DECISIONS.md#adr-089)'s live half. **No code changed for that ruling**, and no frozen file was amended
> by C9.
>
> **PHASE D — THE PROOF — IS IN PLANNING (2026-08-06). Its planning package is
> [ADR-098](DECISIONS.md#adr-098), `Proposed`. PHASE D HAS NOT OPENED. **Both dependency-free packages are now
> open: D1 is ▶ unblocked by the OD-41 ruling of 2026-08-07, and D0 is ▶ unblocked by the
> [OD-29](DECISIONS.md#adr-098-od29) ruling of 2026-08-09; every other package is fenced.**
> ~~D0 waits on OD-29~~ — *corrected 2026-08-09: OD-29 was answered, disposition (b), token discipline for a
> V2 surface means **traceability to the frozen source** rather than the absence of a literal, and D9's
> enrolment hold dissolved with it.* ⚠ **`unfenced` ≠ `free to
> start`** — D5 and D8 carry no ruling of their own but both depend on D1, and **only D0 and D1 have no
> package dependency at all.** The `Depends on` column is the only authority for order.
> Eleven packages (D0…D10), a **complete selector-level frozen property table** over all 702 lines of
> [`v2-proof.html`](design/mockups/v2-proof.html), and ~~**thirteen owner decisions raised — two answered
> (OD-37, OD-41), eleven still owed**~~ **eighteen owner decisions raised — eight answered (OD-29, OD-37,
> OD-41, and OD-42…OD-46 from the 2026-08-09 session), ten still owed** —
> [OD-29…OD-46](COMPOSE-V2-ROADMAP.md#phase-d--what-is-owed-before-it-starts). **D1's package ADR can now
> reach `Accepted`**: OD-42/43/44 answered [D-076](design/V2-SPEC-DEFECTS.md#d-076) and OD-45/46 answered
> [D-077](design/V2-SPEC-DEFECTS.md#d-077), and **no canonical amendment was required by any of the five**.
> Three things a new session should know before reading anything else:
>
> 1. **The Proof already exists in Compose.** One route, one surface (`ProofScreen.kt`, four acts), and
>    **READ-first already ships**. No navigation change is required. But all five Proof files read **zero** V2
>    tokens, and the Compose answers to the **V1-era `proof.html`**, not `v2-proof.html`. Phase D is a re-skin.
> 2. **The packages are ordered by golden blast radius, not by rulings-in-hand.** `:core:ui`'s eleven `Z*`
>    components are 100% V1 and are consumed by both the V2 Bench and the V2 Proof; converting them **after**
>    the Proof re-records ~110 rasters twice. D1 comes before D2.
> 3. **The baseline was red and is now green — `fc33bca`.** `main` at `fdb8319` failed in three places, all
>    Phase C residue: `:app`'s nav tests had been timing out **undetected since C4**, `ZinelyV2DimensTest`
>    asserted a shadow count a committed amendment had changed, and four catalog goldens were never
>    re-recorded. All three fixes had been authored 2026-08-04 and carried uncommitted for three days, which
>    is why C5's, C6's and C9's green sweeps were honest about the tree they ran in and silent about the tree
>    they committed. **`fc33bca`** landed them; a detached worktree reporting an empty
>    `git status --porcelain` then verified **1603 / 0 / 0 / 1 across 12 modules** with Roborazzi at
>    `recorded 0 · changed 0`. ⚠ **The tag `compose-v2-bench-complete` still points at `fdb8319` and is
>    deliberately not moved** ([OD-37a](COMPOSE-V2-ROADMAP.md#phase-c-addendum-record-integrity)) — the
>    milestone and the verified baseline are two commits, on purpose. **[OD-37](DECISIONS.md#adr-098-od37) is
>    answered** — which removed D1's *baseline* blocker and nothing else. ~~**D0 waits on
>    [OD-29](DECISIONS.md#adr-098-od29).**~~ **[OD-29](DECISIONS.md#adr-098-od29) was ANSWERED 2026-08-09,
>    disposition (b), and D0 is ▶ unblocked.** **[OD-41](DECISIONS.md#adr-098-od41) — D1's scope ruling — was
>    ANSWERED 2026-08-07, disposition (a): Phase D re-bases the shared `:core:ui` `Z*` components, D1 stands
>    as published, and D1 is ▶ unblocked.** [ADR-097 §7](DECISIONS.md#adr-097) deviation 5 (`Accepted`) put
>    that act *"outside **a parity phase's** fence"* — **and it is not superseded**: Phase C was ruled a
>    parity phase by OD-2, Phase D never was, and
>    [D-016](design/V2-SPEC-DEFECTS.md#d-016--two-of-phase-as-acceptance-criteria-cannot-be-met-by-a-phase-forbidden-to-touch-product-surface)
>    (2026-07-30) had already ruled that modifying existing product surfaces *"necessarily belongs to Phase
>    D."* §7.5 is annotated, not amended. Since 2026-08-06 every
>    package closeout captures `git status --porcelain` before the final run **and** before the commit.
>
> **There is no next Phase C package.** Scheduling the re-seated H1/H3 work is a separate owner act that has
> not been performed.
>
> **There is no C7 and no C8.** [Owner ruling OD-2](COMPOSE-V2-ROADMAP.md#re-seated-beyond-phase-c) (2026-08-01)
> re-seated the holding tray (H1) and the Add / Art drawer (H3) beyond Phase C; **their letters are not reused
> and C9 is not renumbered.** Scheduling that re-seated work is a separate owner act that has not been performed.
> If you are looking for a live "C7", it belongs to the **V1 conformance programme's** own milestone numbering —
> a different ladder — see [§0.1](#01-two-programmes-two-c-ladders) below.
>
> **No register entry blocks a Phase C package, and none is owed a ruling**
> ([V2-SPEC-DEFECTS](design/V2-SPEC-DEFECTS.md)). **D-012 is RESOLVED** ([OD-25](design/V2-SPEC-DEFECTS.md#d-012-ruling)),
> which was the last entry that ever gated one. Carried forward as **design debt**, all explicitly non-blocking
> and none owed a decision: D-023, D-049, D-050, **D-051** (OD-26), D-052, D-060, **D-064** (OD-27),
> **D-065** (OD-28). Withdrawn the day they were raised, kept as record: D-061, D-062, D-063. Re-seated with
> H1/H3 and no longer Phase C's: D-029, D-030. **One accessibility debt travels on undischarged**: the empty
> state's sticker cluster announces `✿`/`❀`/`★` to the platform a11y tree against its own documented contract,
> found by [C1's Pass 1](DECISIONS.md#adr-090-device-verification) and **not** closed by C9.

<a id="01-two-programmes-two-c-ladders"></a>
### 0.1 Two programmes, two "C" ladders — read the prefix

Two separate programmes in this repository number their milestones `C0…C9`, and they are **not** the same ladder:

| | ladder | where it lives |
|---|---|---|
| **Compose V2 Phase C** | C0 · C1 · C2a · C2b · C3 · C4 · C5 · C6 · ~~C7~~ · ~~C8~~ · C9 | [COMPOSE-V2-ROADMAP](COMPOSE-V2-ROADMAP.md), [ADR-089](DECISIONS.md#adr-089) |
| **V1 conformance** | C0 … C10, including a **live C7 — "Shelf and Proof residuals"** | [V1-CONFORMANCE-INVENTORY](V1-CONFORMANCE-INVENTORY.md) |

So *"C7"* unqualified is ambiguous, and *"C6"* and *"C3"* appear in both. **Always write "Phase C C7" or
"conformance C7".** This is a naming clarification only — it changes no package, no ruling and no scope. It exists
because the V2 roadmap already warns that *"a label that quietly changes meaning is how two documents start
disagreeing while both look correct"*, and this is that hazard sitting one namespace over.

> ⚠ **The paragraph below is the record of Phase C as it was being built, kept for the rulings it carries.**
> Phase C is **COMPLETE** — see §0 above. Read this for *why* things are the way they are, never for *where the
> work is*.

**Phases A and B are CLOSED. Phase C — the Bench — has BEGUN.** Its planning package is
[**ADR-089**](DECISIONS.md#adr-089), `Accepted` on owner GO: **eight packages**, and a **complete selector-level
frozen property table** covering every rule and every scripted behaviour in
[`v2-bench.html`](design/mockups/v2-bench.html), written before any production code. **C0 — corpus cleanup,
documentation only — is done** (`d21e4dd`), closing [D-001](design/V2-SPEC-DEFECTS.md#d-001--v2-benchhtml-header-contradicts-the-freeze-record).
**C1 — the studio surface, and the phase's first production code — is DONE and ACCEPTED** (2026-08-02):
independently reviewed twice, and **[both device-verification passes recorded and passed](DECISIONS.md#adr-090-device-verification)**
on `SM-A176B` / Android 16. [ADR-090](DECISIONS.md#adr-090) is `Accepted`.
Getting there cost three device sessions and two owner rulings. Pass 2 failed twice — first on the blank-page
invitation (**1.15:1**, chrome on the wrong palette), then on **the user's own content** (**1.60:1**: the sheet
dimmed at night while content ink stays black, because it prints). The second became
[D-035 / OD-12](design/V2-SPEC-DEFECTS.md#d-035-ruling) — *the artifact does not dim, the room around it may* —
and the frozen Bench was amended a fourth time, making `.page` a **light-theme island** of eight restated light
tokens. Content ink now measures **18.82:1** in dark. Both defects are guarded by assertions, not goldens: twice
in this package a golden was re-recorded over the defect it should have caught.
**C2a, C2b and C3 are all ACCEPTED. C3 — in-place text editing and the rigid page pan, the centrepiece — closed 2026-08-04 and committed on `feat/c3-inplace-edit-and-page-pan` ([ADR-093](DECISIONS.md#adr-093), `Accepted`).**
**[C5](COMPOSE-V2-ROADMAP.md) — page navigation — is ✅ DONE and ACCEPTED 2026-08-05 (`5576c0e`), with both device passes run from the beginning on an amended build: [ADR-095](DECISIONS.md#adr-095) is `Accepted`, 31 property rows, 44 mutations / 44 killed, 1544 tests / 0 failures.** It needed **two** owner rulings, the second found by hardware halfway through Device Pass 1 — ✅ **[OD-23](design/V2-SPEC-DEFECTS.md#d-059-ruling), Option (a): the frozen Bench is amended a *seventh* time**, `.pthumb` becoming a light-theme island of five tokens after the dark-theme miniature measured **1.21:1** against its own sheet (now 13.97:1). **C6 — the ink popover — is ✅ DONE and ACCEPTED 2026-08-06** (the detail is two paragraphs up; this paragraph kept its C6 sentences from the day C6 was still blocked on hardware, and they are corrected here rather than left to contradict §0). Review returned **GO WITH FIXES** and its eight Required Fixes are reconciled in [§6](DECISIONS.md#adr-096) — the two that mattered were a selection ring that fired **twice** on `Ink` (a colour the freeze puts in two bands) and an **18dp gap where the freeze collapses two 9px margins to 9**, the latter invisible to the recorded frame that was supposed to cover it. Its blocker, [D-028](design/V2-SPEC-DEFECTS.md#d-028), was ruled 2026-08-05: ✅ **[OD-24](design/V2-SPEC-DEFECTS.md#d-028-ruling), option (c) — the frozen Bench is amended an *eighth* time.** The ink **target** selects the bands: a text element is offered `Inks` + `Neutrals`, and `Paper tints` are **fenced** — they are paper by the band's own frozen label, by the caption's *“riso spot-inks”*, and by the presets' third slot, which in two of three recipes is a tint **by value** (`Cream`, `Sky`), which is what decoded `[ink, accent, paper]`. A preset therefore applies **`[0]`**, its primary ink, where the prototype applied the accent — measured, the least legible colour available in all three recipes. **No contrast floor is imposed on in-page text ink, and that is a decision, not an omission**: a riso palette that clears AA is not a riso palette, and contrast is a property of the *pairing*. [ADR-055](DECISIONS.md#adr-055) Decision 6's exclusivity is superseded; **nothing migrates** (`TextStyle.color` is a free `ColorRgba`) and OD-11 keeps the Type bar's `Coral`/`Teal`/`Blue` reachable. [ADR-096](DECISIONS.md#adr-096) is `Accepted` with a **50-row** property table (37 of them carrying a mutation of their own); new `BenchInkPopover.kt`, wired in `EditorScreen.kt`, `Copy.BenchInk` for every swatch name. The gate's measured evidence sits in the register ([gate evidence](design/V2-SPEC-DEFECTS.md#d-028-evidence)); the citation re-anchoring and the property-gap audit sit in [ADR-096 §1](DECISIONS.md#adr-096-drift) and [§2](DECISIONS.md#adr-096-gaps). **Its own independent planning review returned GO WITH FIXES with seven Required Fixes, all reconciled** — including two of my own arithmetic and diagnostic errors (the sub-3.0:1 count is **10**, not 9; the citation drift is four offsets and predates every amendment) and one **falsely claimed gap**, `applyInk`'s snackbar, which ADR-089 row 4.13 and [ADR-094](DECISIONS.md#adr-094) row 4.15 already own and C4 already shipped. **C6 may begin the moment D-028 is ruled and not before** — written at C6's gate, and discharged: OD-24 ruled it on 2026-08-05 and C6 was accepted on 2026-08-06. Previously, at its own gate: **C5 was ▶ IN IMPLEMENTATION; that gate raised one owner question and it was ruled the same day.** ✅ **[OD-22](design/V2-SPEC-DEFECTS.md#d-053-ruling), Option (c): the frozen Bench is amended a sixth time** — `.pthumb i` and its `<i>` are deleted from the specification, the thumb's interior becomes the **real rendered page**, every other frozen property of the thumb and the row is preserved and transcribed, and **the grid is not amended**. The accepted price, recorded before the code: at 26×34dp the miniature is a smudge, not a picture, and C5 does not enlarge the thumb to compensate. [ADR-095](DECISIONS.md#adr-095) is `Proposed` with a 29-row property table written before any production code. Its [blocker check](DECISIONS.md#adr-095-blockers) raised **one** owner question and cleared everything else from rulings in hand: ⛔ [**D-053**](design/V2-SPEC-DEFECTS.md#d-053) — the frozen `.pthumb` is a blank paper sheet with three faint rules, while the shipped `EditorPageStrip` draws a **live miniature of the real page** through the canvas's own `SceneRenderer` tape. Transcribing the freeze removes the only place in the editor where you can see what is on another page, *and* draws three lines that are false on most pages ([D-044](design/V2-SPEC-DEFECTS.md#d-044)'s class, which OD-17 fixed by amending the file); keeping the miniature diverges from the surface [§E.2](design/V2-BENCH-REVIEW.md) was proudest of. Also found at the gate: **all 16 of ADR-089's C5 citations were stale** after five Bench amendments (re-anchored in [§1](DECISIONS.md#adr-095-drift)), and two frozen properties ADR-089 never listed — `.pgcell:active` and `.pthumb`'s transition — are carried as rows rather than implemented silently.

**[C6](COMPOSE-V2-ROADMAP.md) — the ink popover — is ✅ DONE and ACCEPTED 2026-08-06, with both device passes run from the beginning on `SM-A176B` / Android 16 ([ADR-096 §9](DECISIONS.md#adr-096-device)): [ADR-096](DECISIONS.md#adr-096) is `Accepted`, **50** property rows (37 of them carrying a mutation of their own), **40 mutations / 40 killed** over a GREEN control, 41 tests, two recorded frames, and a full-suite regression of 1 585 tests with 0 failures. It was unblocked by [OD-24](design/V2-SPEC-DEFECTS.md#d-028-ruling) — the frozen Bench's eighth amendment — which fenced `Paper tints` from a text target and ruled that a preset applies its primary ink. **Pass 1 failed the first build on two frozen properties the whole apparatus had missed** — `Done` not at the card's far edge, and the snack drawing over the popover — both hidden inside blocks the property table had signed off to a recorded Roborazzi frame that had been recorded from the defect. They became rows **6.2c** and **6.1i** and mutations **M39** and **M40**, and both passes were re-run from the beginning. Pass 2 found **no implementation defect**, five design observations and one deferred accessibility defect (no pane title on the popover). Two observations are the owner's to rule if they are ever to change, and the first is now filed as [D-060](design/V2-SPEC-DEFECTS.md#d-060): `Ink` is offered twice under one name (it is a member of both the Inks and the Neutrals band), and a preset pill draws three dots but applies one. **TalkBack itself was not run** — a Samsung permission activity consumed the automated input — so accessibility claims rest on the platform `AccessibilityNodeInfo` tree.

**[C4](COMPOSE-V2-ROADMAP.md) — the bar, the status chip, the snackbar — is ✅ DONE 2026-08-04, with both device passes run and passed on `SM-A176B` / Android 16 ([ADR-094 §9](DECISIONS.md#adr-094-device)).** Four new composables (`BenchBottomBar`, `BenchStatusStrip`, `BenchAddChooser`, `BenchSnack`); `EditorSupplyTray` and `EditorSavedConfirmation` retired with their tests and goldens; the soft delete drawn with **C2a/C3's existing cover-override seam**, no new rendering path. **946 tests / 0 failures** under `--rerun-tasks`, Roborazzi clean, **31 of 31 mutations caught** over three rounds. Independent review: **GO WITH FIXES**, all three Required Fixes reconciled — the snack was anchored against `.phone` when the freeze positions `.snack` inside `.canvasArea`; the frozen `:disabled` `.35` faded the glyph but not the outline; TalkBack's `Delete` bypassed the reversible path. **A fourth defect surfaced only when `:core:copy:test` was forced to re-run** — six C4 prose literals outside `Copy`, invisible because the guardrail task had been up-to-date across two earlier "full" runs. **Three frictions filed, none fixed inside C4:** 🟦 [D-051](design/V2-SPEC-DEFECTS.md#d-051) (the `Photo` row's *replace* glyph — a defect in the **frozen file**, so the fix is an owner amendment), 🟦 [D-052](design/V2-SPEC-DEFECTS.md#d-052) (`Add › Text` drops the new box over existing content) and 🟦 [D-050](design/V2-SPEC-DEFECTS.md#d-050). [D-049](design/V2-SPEC-DEFECTS.md#d-049) was **re-measured at 28.7 %**, larger than the 17 % on record. *(How it opened:)* [ADR-094](DECISIONS.md#adr-094) is `Proposed` with a 23-row property table written before any production code. Its [blocker check](DECISIONS.md#adr-094-blockers) raised two questions and **both closed the same day**. **[D-047 → OD-21](design/V2-SPEC-DEFECTS.md#d-047-ruling), Option A:** the frozen Bench is amended a fifth time, the bar becomes **`Undo · Redo · Add · Done`**, `Add` opens the frozen chooser with only its **Text and Photo** rows released into C4 — *“a fence reassignment, not a capability reassignment”* — Art stays behind C8, the chooser uses the shipped `ZSheet`, **Text reuses `addTextAndEdit`** so C3's in-place model is untouched, and `EditorSupplyTray` retires. **[D-048](design/V2-SPEC-DEFECTS.md#d-048-ruling) needed no ruling at all** — recorded as D-042 was: `Done` keeps its two frozen states, withheld at the frozen `:disabled` `.35` during a session and owning *clear selection* otherwise; **`Preview ›` does not move**. Carried, unruled: **[D-049](design/V2-SPEC-DEFECTS.md#d-049)**, the 17 % sheet resize. **The price C4 ships knowingly:** both add verbs sit one tap deeper than they do today. *(Below, on C2a:)*
**[C3](COMPOSE-V2-ROADMAP.md) — in-place text editing and the rigid page pan — is ✅ DONE and ACCEPTED 2026-08-04 ([ADR-093](DECISIONS.md#adr-093), `Accepted`); three independent reviews and both device passes are behind it.** C3 was **reopened by an owner ruling after implementation**: the frozen `translateY(-96px)` pan assumed canvas slack the shipped contained page does not have ([D-043](design/V2-SPEC-DEFECTS.md#d-043)), and [**OD-16**](design/V2-SPEC-DEFECTS.md#d-043-ruling) ruled option (b) — *−96 is a **maximum**, spent as `min(96dp, slack + clearance)`*. The frozen Bench was amended **first**, per the HTML-first rule, and the amendment changes the prototype's own motion (the title now lifts ~81px; lower elements still reach the 96px ceiling). The device evidence that produced the ruling also found a second defect the first was **hiding behind** — [D-045](design/V2-SPEC-DEFECTS.md#d-045), the canvas never honouring `.canvasArea{overflow:hidden}`, which left `Preview ›` invisible **and still `clickable` on the platform tree** in every editing session. Both landed together, as the ruling required: either alone makes the other worse. C2b took the long way there: its first device Pass 2 failed on [D-039](design/V2-SPEC-DEFECTS.md#d-039), the same verb offered twice on one screen, and [ADR-092](DECISIONS.md#adr-092) was held at `Proposed` until the owner ruled [**OD-14**](design/V2-SPEC-DEFECTS.md#d-039-ruling): *both bars stay, but identical actions are never presented twice at once — assign responsibilities instead of duplicating presentation.* Element verbs went to the frozen bar, transform verbs to the shipped one, and the on-canvas Reframe chip yields to the verb that names it; every withheld control returns the instant the frozen bar stands down, so **no capability is ever off-screen**. Both device passes were then re-run from the beginning, reusing no earlier evidence, and [**both pass**](DECISIONS.md#adr-092-device-2). C1 was accepted and committed (`23e1a91`). C2a followed:
**[C2a](COMPOSE-V2-ROADMAP.md) — selection: the outline, the eight handles, the dim and the materialise — is ✅ DONE and ACCEPTED 2026-08-02.**
[ADR-091](DECISIONS.md#adr-091) opened it with a property-level table before any production code, the independent review
returned **GO WITH FIXES** and all three Required Fixes were reconciled, and the full suite plus both golden gates are
green. Then the device answered differently on each pass. **Pass 1 passed** — the dim lands within one channel step of
the frozen `opacity:.4`, the outline measures `#5E6B2F` at 5.20:1 in *both* themes, content ink holds 18.81:1.
**Pass 2 failed**: once you select something there is no way to stop, so the dim — which fades everything else the user
wrote to 2.78:1 — cannot be dismissed. `Intent.ClearSelection` exists in the reducer and nothing dispatches it; the
freeze's two exits are a canvas click (unowned) and Done (**C4**). That was ⛔ [D-037](design/V2-SPEC-DEFECTS.md#d-037),
**ruled the same day as [OD-13](design/V2-SPEC-DEFECTS.md#d-037-ruling), option (a): selection is a transient editing
state, not a modal one** — a tap anywhere outside it dismisses it, and a tap on another element *transfers* with no
intermediate clear. The owner scoped it as *completion of an existing capability, not a new feature*, and it landed as
one line: `onTap → Intent.SelectAt`, which covers every clause because `SelectAt`'s hit-test **miss** already reduces to
`ClearSelection`'s exact state ([ADR-091](DECISIONS.md#adr-091) row 2.14). Four tests, two mutations, a second
independent review (**GO WITH FIXES** — both Required Fixes documentation, both reconciled). **[D-036](design/V2-SPEC-DEFECTS.md#d-036)
was ruled documentation-only and fences nothing.** Both device passes were then re-run from the beginning
against the completed build, reusing none of the earlier evidence, and **[both pass](DECISIONS.md#adr-091-completion-device)**:
dismissal works on paper, on the desk and as a transfer, a drag still transforms, and all eight handles swallow a
tap so reaching for one never deselects. Pass 1 disproved one of the ADR's own claims (row 2.8a's *"invisible in
practice"* — a neighbour inside the selection's chrome quad keeps full-strength ink), corrected in place. Pass 2
carries **P2-1: the sheet resizes 17 % on every select/dismiss**, pre-existing and `.bar`-shaped, so **C4**'s. The
owner's document was restored and the restoration **verified from the persisted file rather than the screen**.
**[ADR-091](DECISIONS.md#adr-091) is `Accepted`.**

***Dated snapshot — superseded, kept as the record of 2026-08-02. C2a and C2b were both accepted that day;
the next package is now [C9](COMPOSE-V2-ROADMAP.md#phase-c-packages). The rulings it records (OD-9, OD-11,
the C2 split, the WCAG 2.5.7 non-removal invariant) still govern.*** This block previously opened with a `~~`
that was never closed, so it rendered live and told a fresh session the next package was C2.

**C2 was the next package, split into C2a and C2b by OD-11, and both were unblocked — C1's acceptance was the
last gate:** [D-031](design/V2-SPEC-DEFECTS.md#d-031) was ruled on 2026-08-01
(**OD-9** — the freeze specifies the editing surface, not the whole application flow; Font and Size stay drawn and
invent nothing, Read and back reuse what exists, redo is kept), and applying that ruling immediately raised
**[D-034](design/V2-SPEC-DEFECTS.md#d-034)**: the frozen `.ctx` is a **verb** bar, while the `EditorContextBar` it
would replace is the **WCAG 2.5.7** single-pointer twin of the drag gestures. Transcribing the freeze would delete
eight discrete move/resize/rotate controls. **[Ruled 2026-08-02 (OD-11): keep both.](design/V2-SPEC-DEFECTS.md#d-034-ruling)**
The two are not mutually exclusive — the frozen bar is the editing vocabulary, the shipped one an
accessibility-preserving transform affordance — so `.ctx` is **additive** and the transform controls remain, because a
parity phase does not remove or weaken a WCAG 2.5.7 conformance path. The same ruling **splits C2 into C2a**
(`.el*`, `.sel`, `.handle*`, `.content.focusing`, `@keyframes mat`) **and C2b** (`.ctx*`, rows 2.10–2.13); C2b carries
the non-removal invariant as a frozen-property-table assertion whose mutation is *delete them*.
**No C2 production code had been written at the time of this snapshot;** C2a and C2b shipped on 2026-08-02.
C1's own pre-implementation blocker check raised **[D-033](design/V2-SPEC-DEFECTS.md#d-033)** — the frozen
`212×326` page was not the document's `210.47×297.64` panel, and the frozen uniform `16px` keep-clear was not
its `17pt` `safeAreaInsetPt`, so nothing stated which rectangle the print-correctness cue draws. The owner
**amended the frozen Bench** the same day (`.page` **229×324**, `.keepclear` **18.5px**), making the page box
canonical geometry; C1 then **derives** the cue from `Imposer.DEFAULT_SAFE_AREA_INSET_PT` rather than
transcribing the frozen literal, so it keeps depicting the engine's boundary if that boundary ever moves.
[D-032](design/V2-SPEC-DEFECTS.md#d-032) was ruled the same day (OD-10) and row 1.9 shipped with C1 — the warn
state is transient interaction guidance, not document state: derived per frame from the in-flight gesture, held
in no reducer, and unable to outlive the interaction. What is
owed, and by which package, is
[COMPOSE-V2-ROADMAP.md § Phase C — what is owed before it starts](COMPOSE-V2-ROADMAP.md#phase-c--what-is-owed-before-it-starts).

**The one sentence a fresh session needs about Phase C: the frozen Bench specifies a *studio*, the repository
ships a *document editor*, and the owner ruled that Phase C re-skins the editor rather than building the
studio.** Nine of the frozen file's regions re-skin something that exists. **Four things rested on capability
that does not: H1, H2, H3 and `DecorElement`** — H4's maker inks already exist, so the four are three of the
studio additions plus the element kind. Verified, not assumed: `Element` in `core:model` is `ImageElement | TextElement` (`DecorElement` is net-new
by [IA §A.2](design/V2-BENCH-IA-INTERACTION.md)'s own words), `grep -ri "decorelement\|keep.\?clear"` over
`core`, `feature` and `app` returns **zero**, the holding shelf has no store or schema anywhere, and the product
has exactly one format — `SINGLE_SHEET_8(pageCount = 8)` — against a frozen navigation authored at twelve pages
that scales to thirty-two and offers *add / delete*. Phase C's Objective said **"no feature additions"** while
its Deliverables named those four: a contradiction of the same shape as
[D-016](design/V2-SPEC-DEFECTS.md#d-016--two-of-phase-as-acceptance-criteria-cannot-be-met-by-a-phase-forbidden-to-touch-product-surface),
raised rather than adjudicated, and **resolved by owner ruling OD-2 on 2026-08-01 in favour of the objective.**

**The four rulings of 2026-08-01, which are what changed** ([ADR-089 §5](DECISIONS.md#adr-089)):

- **OD-1** — Phase B is complete; [ADR-086](DECISIONS.md#adr-086) is `Accepted`.
- **OD-2** — **Phase C is a parity phase over the existing editor architecture and introduces no new editor
  capability.** The holding shelf (H1), `DecorElement`, variable page counts, page add/delete/reorder and the
  Art drawer (H3) are **re-seated beyond Phase C** —
  [roadmap § Re-seated beyond Phase C](COMPOSE-V2-ROADMAP.md#re-seated-beyond-phase-c). **C7 and C8 no longer
  exist**, their letters are not reused, and C9 is not renumbered.
- **OD-3** — the frozen Bench and Proof are **amended** with a dedicated `--page-shadow` / `--page-contact`
  pair. **Transcribe the amended file**; light rendering is unchanged. **C1 landed the Kotlin half**
  (`pageShadow` / `pageContact` in `ZinelyV2Colors`); the Proof's `.zpage` remains Phase D's.
- **OD-4** — Phase C's acceptance criteria **exclude literal document-typeface parity for `.t-title` and
  `.t-body` only**. Everything else stays literal parity. D-004 does **not** move forward.

**Phase C planning raised five register entries before a line of code** — [D-028](design/V2-SPEC-DEFECTS.md#d-028)
(the Ink verb offers nineteen swatches where `Accepted` [ADR-055](DECISIONS.md#adr-055) pins five),
[D-029](design/V2-SPEC-DEFECTS.md#d-029) (the holding shelf has no model, no persistence, no scope — while
[§E.4](design/V2-BENCH-REVIEW.md) makes its persistence a *build invariant* and [ADR-025](DECISIONS.md#adr-025)'s
sweeper would treat an ungathered photo as unreferenced), [D-030](design/V2-SPEC-DEFECTS.md#d-030) (twelve pages
against a fixed eight), [D-031](design/V2-SPEC-DEFECTS.md#d-031) (**the Bench has no exits** — Font, Size, the
Read hand-off and back are drawn or expected and wired to nothing, and **redo** exists in the product and not in
the freeze), and [D-032](design/V2-SPEC-DEFECTS.md#d-032) (the keep-clear warn state is declared and *never
triggered anywhere in the frozen script*, and its written trigger — *"text **or a face**"* — needs face
detection). **D-029 and D-030 remain open but are no longer Phase C's**: OD-2 sent them with the capability they
describe. **No entry now blocks a Phase C package.** D-028 fenced C6 and was ruled on 2026-08-05 by
[OD-24](design/V2-SPEC-DEFECTS.md#d-028-ruling); **D-034** fenced C2 and was ruled by OD-11. D-032 (OD-10)
shipped in C1, and D-031 was ruled by OD-9 — which is what surfaced D-034.

**Two Phase C blockers were not new and were easy to miss because they read as settled — both are now answered.**
[**D-004**](design/V2-SPEC-DEFECTS.md#d-004--the-frozen-zine-content-is-set-in-fraunces-the-render-engine-can-only-draw-inter)
is deferred to **Phase D** with three prohibitions — *no workaround, no temporary font substitution, no second
rendering path* — and the frozen Bench's page text is `var(--serif)` = Fraunces, so the page cannot reach
*literal* parity in Phase C. OD-4 wrote that divergence into the phase's criteria, narrowed to `.t-title` and
`.t-body`, **and left D-004 where it was.** And
[**D-010**](design/V2-SPEC-DEFECTS.md#d-010--the-page-shadow-is-hard-coded-to-the-light-theme-and-does-not-adapt-in-the-dark)
— the page's shadow spelled out as a light-theme literal, correct in light and subtly wrong in dark, *the
failure mode nobody screenshots* — is **resolved** by the [OD-3 amendment](design/V2-SPEC-DEFECTS.md#d-010-amendment).
Two more, [D-008](design/V2-SPEC-DEFECTS.md#d-008--two-of-the-three-frozen-surfaces-specify-no-focus-appearance-and-one-removes-it)
and [D-009](design/V2-SPEC-DEFECTS.md#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp),
have their **approach** ruled and **close in this phase**: the Bench's two `outline:none` rules are *not*
transcribed, and its 26px swatches and 23×19px tray fold keep their paint and gain 48dp of reachable area — with
overlap resolved by hit-region priority, never by shrinking a region back under the floor.

**And one re-seated surface was fenced by the freeze itself, not by any judgement made here.**
[V2-BENCH-REVIEW §E.6](design/V2-BENCH-REVIEW.md), the owner-approved freeze, says of the Art drawer and the
maker-ink namespace: *"**do NOT freeze into implementation until a review + legal pass clears them.**"* The
colour-namespace ADR exists — [ADR-072](DECISIONS.md#adr-072). **The asset-layer ADR does not exist anywhere in
this log, and no legal pass is recorded.** OD-2 re-seated the Art drawer; the freeze would have fenced it
anyway, and it is the product's only network path.

**Phase B closed at `2842603`.** The V2 Library **is** the app's Home route (B5, `03223da`), and
[ADR-088](DECISIONS.md#adr-088) followed it — the paper chooser now draws A4 and Letter at one physical scale,
derived from `PaperSize` rather than restated as literals. [ADR-086](DECISIONS.md#adr-086) is `Accepted` and
Phase B's gate is recorded, by **owner ruling OD-1 of 2026-08-01**.

---

**Everything below this line is the Phase A and Phase B record.** It is kept verbatim because its rulings still
govern — read it for those, not for status.

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

**B5 — the screen — is BUILT, ACCEPTED and COMMITTED at `03223da`** ([ADR-086](DECISIONS.md#adr-086),
`Accepted` by owner ruling OD-1 of 2026-08-01 — independently reviewed **GO WITH FIXES**, fixes applied). Its
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
8a. **A mutation harness must prove it can report a PASS before any verdict it gives means anything.** C3's
   driver reported *"13 killed, 0 survivors"* having **executed no tests at all**: it invoked
   `cmd /c gradlew.bat` by bare name, `cmd` never resolved it, every run returned `rc 1` with
   `'gradlew.bat' is not recognized`, and the verdict logic inferred KILLED from the non-zero exit. A
   perfect score is the *expected* output of a harness that is simply broken, which makes it the least
   trustworthy result a battery can produce and the one least likely to be questioned. Three rules follow:
   run a **control** with no mutation applied and abort unless it passes; read the verdict from the **JUnit
   XML** — failures *and* whether the filter matched any test at all, since "no tests ran" is an unguarded
   row, not a kill — and never from an exit code alone; and use an **absolute** wrapper path. *(Scope: only
   C3's driver had this defect. `mutate.sh` uses `./gradlew` from bash and resolves; earlier packages' own
   drivers were not re-run and were not re-verified here.)*
8b. **`verifyRoborazziDebug`, not `testDebugUnitTest`, for any mutation whose property is paint.** Under a
   plain test task Roborazzi records nothing and compares nothing, so every golden-guarded row passes
   trivially. And even on the right task, `changeThreshold = 0.02f` means a 1px hairline across a 400px row
   — about 0.25 % of the frame — **cannot move the gate**. A raster is a net for what nobody thought to
   assert; it is not an assertion about fine paint. Count those pixels explicitly.
8d. **A mutation is evidence about the property on its label only if the edit changes that property and
   nothing else.** C3 shipped two that did not. One swapped a whole chip for a different control, which
   deletes a node and duplicates a tag: all five failures it produced were *cardinality* errors, so it
   killed nothing about the enabled-ness it was named for. Another deleted a chip’s padding under a label
   about the chip’s *announced bounds* — two different defects, and the one that actually shipped once
   (`testTag` below `padding`) went on surviving. Prefer the **minimal** edit; if you cannot make one, the
   mutation is testing something else and should be renamed to whatever that is.
8e. **Attribute each kill to the test that actually produced it, and check.** Two of C3’s were credited to
   suites that ran and *passed* — including one credited to a platform-tree suite that was not even in the
   filter. The battery was right and the write-up was wrong, which is the harder error to notice: the
   headline number was true, so nothing prompted a second look.
8f. **A perfect score is a statement about the mutations you chose, not about your tests.** C3’s own
   fourteen came back 14/14. An independent reviewer then chose six and **five survived**, three of them
   frozen properties the ADR already listed as asserted. A battery written by the author of the code will
   flatter it; the cheapest correction is to have someone else pick a handful.
8c. **Put `testTag` above `padding`, not below it.** Modifier order decides what the semantics node's bounds
   *are*. C3 tagged the style row after its padding, so every geometric assertion about "the row" was
   measuring its inner content box — and a raster probe aimed at the row's top hairline read the chips
   instead, and reported a **deleted** hairline as present. The mutation that exposed it produced a
   byte-identical raster, which is what finally proved the probe, not the code, was wrong.

**C3's device passes added four more, and the first is the one that would have been missed forever.**

8g. **A defect whose predicted symptom does not reproduce is not thereby absent — find out why it didn't.**
   [D-043](design/V2-SPEC-DEFECTS.md#d-043) said an element in the top 96dp would be *lifted out of view* by
   its own edit gesture, and on hardware it stayed perfectly visible. The tempting reading was "the unit-test
   host exaggerated it." The real reason was a **second defect masking the first**: the canvas never clipped,
   so the over-lifted page was painted over the top bar instead of being cut off
   ([D-045](design/V2-SPEC-DEFECTS.md#d-045)). Fixing the clip alone would have *created* the reported
   symptom; fixing the pan alone would have left paper on the chrome. Two defects that hide each other have to
   be found and landed together, and the only thing that surfaced the pair was refusing to accept a
   non-reproduction as good news.
8h. **When a frozen constant is affordable only because of the prototype's own geometry, it is a rule wearing
   a number's clothes.** `−96px` works in the freeze because a 229×324 page sits inside a 344×744 phone with
   a band of empty canvas above it. The shipped Bench *contains* the page, so that band measured **4.2dp** on
   device. The freeze was not wrong and the transcription was not wrong — the number was load-bearing on an
   assumption the freeze never had to state. Ask of any frozen literal: *what does this depend on that the
   prototype gets for free?*
8i. **Argue severity in the unit the ruling will be made in.** D-043 was escalated as "the top ≈ 73 % of the
   page", which is `96/scale` **points of page** and varies per host. What actually decided OD-16 was
   **slack: 4.2dp available against 96dp demanded**. The page-relative figure was true and nearly useless;
   one measured number on real hardware settled it.
8j. **Robolectric has no IME, so anything that depends on the keyboard has no unit test — say so in the row.**
   Half of C3's amended pan (the clearance term) and the half of D-045 that actually mattered on device are
   both unobservable in Robolectric for this one structural reason. They are carried by pure-function tests
   plus named device-checklist items. A row that quietly has no assertion is worse than a row that says it
   has none.
8k. **Amending a frozen file invalidates every `:NNN` citation to it, and a find-and-replace table is the
   wrong tool for repairing them.** OD-16's amendment moved ~64 lines of `v2-bench.html` and stranded dozens
   of references. The first repair pass used a hand-written old→new map applied to bare `` `:NNN` `` tokens,
   which did three kinds of damage: it rewrote citations belonging to **other** mockups whose line numbers
   happened to collide (`v2-library.html`'s `.done h4` `:224`, a `v2-proof.html` `:485`); it "corrected" rows
   whose stated old address was already stale from an *earlier* amendment, moving them somewhere new and
   equally wrong; and it flattened [ADR-093 §1](DECISIONS.md#adr-093)'s drift table — whose entire content is
   *"ADR-089 said X, it is actually at Y"* — into a table where both columns said Y. What works: rebuild the
   map by **diffing the file against its own previous revision** (`difflib` over lines gives the exact
   old→new mapping), then rewrite a token only when the backticked selector on that same doc line is present
   at the old address and absent at the new one. Verified per token, not per table. And re-check any table
   whose *purpose* is to record a stale address — those are the ones a correctness pass will silently destroy.
   **This lesson is written from three failed attempts, and the repair is only partly done.** C3 repaired the
   citations in the files C3 already owns, each one verified by opening the frozen file at the cited line. The
   **25 explicit citations stranded in ten files C3 does not touch** are filed as
   [D-046](design/V2-SPEC-DEFECTS.md#d-046) rather than swept — a fourth mechanical pass across clean files, in a
   package under an owner instruction not to do unrelated cleanup, is how the first three went wrong. An earlier
   draft of this lesson described the method as though it had been applied everywhere; it had not, and the
   independent review caught the claim.

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
- **B · Library** — **✅ CLOSED 2026-08-01** (B1–B5, [ADR-081](DECISIONS.md#adr-081)…[ADR-084](DECISIONS.md#adr-084),
  [ADR-086](DECISIONS.md#adr-086), plus [ADR-088](DECISIONS.md#adr-088)) — pixel parity to the frozen Library, and
  the V2 Library is now the app's Home route.
- **C · Bench** — **✅ CLOSED 2026-08-06** (C0, C1, C2a, C2b, C3, C4, C5, C6, C9; [ADR-089](DECISIONS.md#adr-089)
  through [ADR-097](DECISIONS.md#adr-097), every one `Accepted`; merged `8a30051`, tagged
  `compose-v2-bench-complete`; there is no C7 and no C8) — pixel + interaction +
  animation + editing-behaviour parity, on top of the **existing** engine. *"No feature additions"* was the line
  to read carefully: the frozen Bench contains four studio additions (H1–H4) plus a net-new element kind, and
  **three of the four — H1, H2 and H3 — plus `DecorElement` are capability the repository does not have.** Only
  **H4**, the maker inks, already exists (`ZinelyContentInks`, under D-003's ruling). **Owner ruling OD-2
  (2026-08-01) settled it in favour of the
  objective** — the phase is **eight packages** and the capability is
  [re-seated](COMPOSE-V2-ROADMAP.md#re-seated-beyond-phase-c). See §0 and the roadmap's
  [what is owed](COMPOSE-V2-ROADMAP.md#phase-c--what-is-owed-before-it-starts).
- **D · Proof** ◀ **IN PLANNING — not opened; D1 ▶ unblocked (OD-41 answered 2026-08-07) and D0 ▶ unblocked (OD-29 answered 2026-08-09). D5 and D8 carry no ruling of their own but both depend on D1** ([ADR-098](DECISIONS.md#adr-098), `Proposed`) —
  pixel + print-flow + fold-guide + a11y parity, for the shipped single-sheet-8 stage, **plus** the two
  obligations re-seated here by owner ruling: retiring V1's parallel token objects and defining what enrolment
  in `config/token-enrolment.txt` even *means* for a V2 surface. **Eleven packages**, D0…D10, ordered by golden
  blast radius; ~~**eleven owner decisions owed of thirteen raised** — OD-37 and OD-41 are answered~~
  **ten owner decisions owed of eighteen raised** — OD-29, OD-37, OD-41 and OD-42…OD-46 are answered
  ([OD-29…OD-46](COMPOSE-V2-ROADMAP.md#phase-d--what-is-owed-before-it-starts)); ~~**eight of eleven packages
  fenced before any is opened**~~ **seven of eleven packages fenced, and the two with no dependency — D0 and
  D1 — are both open**. The Proof is **already built as V1** and READ-first already ships — this phase changes which
  frozen document the surface answers to, and closes the gap where the imposed sheet is a **schematic rather
  than a render of the user's own pages**.
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
- **Privacy** — no network/analytics/cloud; offline-first. **No network path exists for assets at any stage** ([ADR-104](DECISIONS.md#adr-104) / [Constitution Amendment 3](design/V2-CONSTITUTION.md#amendment-log), 2026-08-15, which withdrew the previously permitted opt-in keyword-only asset search).
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
6. Begin your package on an explicit owner GO. **Phases A and B are closed**; rebuilding any of them is the
   mistake this section exists to prevent. **Phase C is CLOSED as of 2026-08-06 — C0, C1, C2a, C2b, C3, C4, C5,
   C6 and C9 are all DONE and ACCEPTED, and there is no next Phase C package**; there is no C7 and no C8 —
   read [ADR-089](DECISIONS.md#adr-089) and the roadmap's
   [what is owed](COMPOSE-V2-ROADMAP.md#phase-c--what-is-owed-before-it-starts) before writing a line of anything
   that claims to continue it,
   and note that **no owner decision now fences a package's work** — D-028 / OD-6 fenced C6 and was ruled on
   2026-08-05 by [OD-24](design/V2-SPEC-DEFECTS.md#d-028-ruling). ([D-034 / OD-11](design/V2-SPEC-DEFECTS.md#d-034-ruling)
   fenced C2's `.ctx*` rows for a day and was ruled on 2026-08-02.) Other decisions remain live without fencing a
   package: **D-012**'s half of OD-10 is answered *in* C9, and D-023 / D-029 / D-030 are open against whichever phase
   takes the re-seated capability. **What C9 needs before it starts is not a ruling — it is its own ADR, written
   before any production code**, per [ADR-089 §2.2](DECISIONS.md#adr-089).
7. For any screen, open its **frozen HTML** first; it is the spec.

*The design is done. Your job is faithful execution. If something feels like it should change, it goes into the
frozen HTML first (owner gate) — not into the code.*

---

*Handover written 2026-07-28 by the Design Custodian at the close of the V2 Design Program. §0 and the Phase A
status added 2026-07-29 (package A10); updated 2026-07-30 at the **Phase A closeout**, when the D-002, D-006
and D-016 owner rulings were recorded and Phase A's gate passed. Updated 2026-07-31 at the **Workflow V2
revision** ([ADR-085](DECISIONS.md#adr-085)): this document became the package entry point, and §4/§5/§8 took the
frozen property table, the verification order, the mid-package review and the single owner gate. §0 rewritten
2026-08-01 at the **close of Phase B and the opening of Phase C's planning gate** ([ADR-089](DECISIONS.md#adr-089)):
the Phase A and Phase B record below it is left verbatim, because a status block edited to match its outcome
stops being evidence of anything — with the single exception of B5's status line, which was **wrong** rather
than merely superseded once the commit landed and the owner accepted it. §0 rewritten again later the same day
when the owner ruled **OD-1 · OD-2 · OD-3 · OD-4**.*
