# V1 Conformance Inventory

> **What this is.** The complete, evidence-backed list of repository work required to satisfy
> [V1-IMPLEMENTATION-ROADMAP.md](V1-IMPLEMENTATION-ROADMAP.md). One discrepancy, one item. Every item
> carries its repository location, its authority, and the method that proves it closed.
>
> **What this is not.** Not a roadmap — the roadmap exists and owns order. Not a ticket list, not an
> implementation, not a design. **No item here decides anything**; where a decision is owed, the item
> records *that a decision is owed*, names its owner, and stops.

**Baseline:** `main` @ `57f1e8b`, `0.9.0-beta.1` (`versionCode 3`). Working tree as
[the roadmap records it](V1-IMPLEMENTATION-ROADMAP.md); no source file has changed since.

**Method.** Every count below was taken by command against that tree, and every command excludes
comment lines — `grep -vE '^[[:space:]]*(\*|//|/\*)'` — because the previous document in this
milestone shipped three inflated numbers from `grep` matching KDoc as if it were code. Where a number
here disagrees with the roadmap's, **this document's number and its command supersede it**, and the
disagreement is disclosed in [§0.1](#01-corrections-to-the-roadmaps-published-counts) rather than
silently absorbed.

**Priority is not encoded here.** Identifiers mean nothing but identity — CI-93 and CI-94 were added
after review and keep the numbers they were assigned rather than renumbering ninety-two cross-references,
and CI-43 is printed among the token items it belongs with while being assigned to the milestone its
prerequisites allow. **Order comes from the milestone field, which is the roadmap's, and from nothing
else.**

---

## 0. How to read an item

```
#### CI-nn · <the discrepancy, in one line>
- Status      OPTIONAL — the ruling or merge that closed it, dated, plus anything expressly NOT
              closed by it. An item with no Status line has no closure recorded *here*; that is
              not proof it is open (CI-13 landed in 6b46d0f and carries none).
- Location    file:line — every site of the one logical change
- Current     what the repository does today
- Required    what the corpus requires
- Authority   the exact document and section that requires it
- Milestone   C0 – C10 (C3 appears as its two roadmap halves, C3a / C3b)
- Prereq      the items that must close first
- Kind        mechanical · behavioural · visual · documentation · architectural
- Changes     pub (public behaviour) · a11y · motion · persist · render · tests · docs
- Risk        what goes wrong if this is done badly
- Verify      the method that proves it closed
```

**"One independently verifiable conformance change"** is the unit. Twenty files under one migration
are one item with twenty locations. One file with six unrelated gaps is six items. Where creating an
object (C4) and adopting it (C6/C7) are separated, they are **two items**: they land in different
milestones, are verified by different methods, and either can be done without the other — which is
precisely the failure the split exists to make visible.

### 0.1 The roadmap's published counts, re-measured — and three failed "corrections" of my own

The first draft of this section claimed to correct three of the roadmap's numbers. **An independent
review falsified all three**, and the way each failed is worth more than the numbers.

| Claim | Roadmap | First draft here | **True** | What happened |
|---|---|---|---|---|
| Colour tokens in `ZinelyColors` | 22 | 21 | **22 — the roadmap was right** | My published command was `grep -cE "^\s+val [a-zA-Z]+: Color,"`. `[a-zA-Z]+` **cannot match `paper2`** (`ZinelyColors.kt:26`). I broke a correct figure with a regex that silently dropped a token, and published the broken command beside it as if that made it checkable |
| Test methods | 1,009 | 1,026 | **1,009 under `src/test`** | Not a correction — an **undisclosed scope change**. `git ls-files \| grep -E 'src/test/.*\.kt$' \| xargs grep -hoE '@(Test\|Property)\b' \| wc -l` → 1,009 exactly. My figure added 18 instrumented `androidTest` methods **that no CI job runs on push** — which contradicts the sentence the number was supporting |
| Test files | 138 | 139 | **138 under `src/test`** | As above |
| `MaterialTheme.*` production reads | 16, Editor only | 16, Editor only | **16 ✓** | Independently confirmed; nothing changed |

**The lesson, recorded because it is the third distinct form this milestone's counting defect has
taken.** The roadmap counted comments as code. This document promised to fix that by publishing the
command beside the number — and then shipped a *published, comment-filtered* command that was wrong in
a different way, plus two scope changes dressed as accuracy. **A published command is not a verified
one.** Both of the genuine comment-filtering errors this document did commit (CI-28, CI-81) are
corrected in place and disclosed at their items.

*(An earlier unfiltered pass returned 186 files / 1,351 methods; both were inflated by generated
sources under `*/build/`. `@ParameterizedTest` returns zero in this repository; the annotation set is
`@Test` + jqwik's `@Property`.)*

Confirmed unchanged, each independently re-run: **591 `.dp` across 61 distinct values**; **80 `.sp`
across 25 distinct values**; **19 distinct `RoundedCornerShape` forms** across 72 occurrences
(geometrically 18 — `(2.dp, 4.dp, 4.dp, 2.dp)` and its named-argument spelling are one shape written
two ways, which is itself a small conformance smell); **48 `Color(0x`**; **61 goldens** (43
`:feature:editor`, 18 `:render-android`); **12 shelf / 16 proof / 8 component-and-chrome / 7
page-preview** goldens; **384 test methods across 49 files in the `:core:*` modules**; **22 distinct
`border-radius` values** in the frozen trilogy.

---

## 1. The inventory

### C0 · Specification reconciliation — no code

> Twenty-four items. **Not one touches `src/main`.** Every one of them is a decision the repository
> does not currently record, and every downstream milestone spends authority these create.

#### CI-01 · The design-system hub collision is unadjudicated — ✅ **DONE**
- **Status** ✅ **CLOSED 2026-07-24 — [ADR-061](DECISIONS.md#adr-061)**, owner ruling **Option (a)**: ZINELY-DESIGN-SYSTEM.md is the authoritative design-system source of truth, DESIGN-LANGUAGE.md is a companion reference, and §0.2's split table owns per-area authority. The *Current* paragraph below describes the pre-ruling state and is retained as the record of what was adjudicated. **One deferral rides the owner's README pass:** `README.md:60`'s index row still names the old hub, and this programme does not edit README.
- **Location** `docs/ZINELY-DESIGN-SYSTEM.md` §0.2 · `docs/design/DESIGN-LANGUAGE.md:3-11` · `README.md:60` · `docs/DECISIONS.md`
- **Current** `README.md:60` indexes `DESIGN-LANGUAGE.md` as the **"Design system hub"**, unqualified, while `ZINELY-DESIGN-SYSTEM.md` §0.2 declares *itself* **subordinate until an ADR adjudicates**. So the repository's index names one authority and the newer document defers to it — leaving no document that claims the role. *(An earlier draft of this item said both documents "describe themselves as the design authority." They do not: `DESIGN-LANGUAGE.md:3-11` explicitly calls itself "a companion reference… **not a parallel source of truth**" — as CI-02 quotes correctly. The collision is real; the characterisation was a rank error, in the first item every other C0 item depends on.)*
- **Required** Exactly one authoritative location per decision.
- **Authority** [CLAUDE.md · Documentation Rule](../CLAUDE.md#documentation-rule-mandatory); [ZINELY-DESIGN-SYSTEM §0.2](ZINELY-DESIGN-SYSTEM.md); [§15 open item 1](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** none · **Kind** documentation · **Changes** docs
- **Risk** Every C3–C7 item cites a document that has not been established as superior. An engineer resolving a collision in code resolves it at the wrong rank.
- **Verify** A dated ADR in `DECISIONS.md`; `DESIGN-LANGUAGE.md` header and the README row edited to match; Review Agent GO.

#### CI-02 · `DESIGN-LANGUAGE.md` specifies a palette the shipped app deliberately rejects
- **Location** `docs/design/DESIGN-LANGUAGE.md:64` (`desk #3A3A3C`), `:71` ("a friendly/marker face for chrome labels")
- **Current** The README-indexed hub specifies `#3A3A3C` and a marker face. `ZinelyColors` ships `desk = #E7DECE` (light) / `#201F1E` (dark) and Inter + Fraunces. `#3A3A3C` survives only as `Theme.kt:80 LegacyDesk`.
- **Required** No canonical document describes a rejected design.
- **Authority** [ZINELY-DESIGN-SYSTEM §9.4](ZINELY-DESIGN-SYSTEM.md) (discloses, does not resolve); [DESIGN-LANGUAGE:3-11](design/DESIGN-LANGUAGE.md) — *"not a parallel source of truth… the PRD / ARCHITECTURE / DECISIONS win"*
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** Low in code, high in onboarding: a stranger following the README finds the wrong palette first.
- **Verify** `grep -n "3A3A3C" docs/design/DESIGN-LANGUAGE.md` returns nothing, or returns it explicitly marked historical.

#### CI-03 · Two documents still describe a celebration that two ADRs retired
- **Location** `docs/design/DESIGN-LANGUAGE.md` §10 (confetti/sparkle at export) · `docs/design/VOICE.md`
- **Current** Both describe a confetti ending. [ADR-040](DECISIONS.md#adr-040) rejected it (*"a static payoff is calmer, testable, and reduced-motion-safe"*); [ADR-051](DECISIONS.md#adr-051) retired the screen it lived on.
- **Required** The corpus describes what ships.
- **Authority** [§15 open item 5](ZINELY-DESIGN-SYSTEM.md); [§9.4](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** A future implementer reads a live instruction to build a rejected animation.
- **Verify** Neither document instructs a confetti/sparkle payoff; the ADR-040/051 rationale is linked from where it was.

#### CI-04 · The README points engineers at retired prototypes as the working specification
- **Location** `README.md:65` → `docs/design/mockups/index.html`; the frozen spec at `docs/design/v1/{shelf,bench,proof}.html` is **not linked at all** (`grep -n "design/v1" README.md` → no matches)
- **Current** `docs/design/mockups/` holds pre-V1 prototypes including `completion.html`, `export.html`, `preview.html` — three screens retired by [ADR-051](DECISIONS.md#adr-051)/[ADR-052](DECISIONS.md#adr-052).
- **Required** The repository's index points at the specification the HTML-first workflow means.
- **Authority** [CLAUDE.md · HTML-first](../CLAUDE.md#html-first-ui-workflow-mandatory); [validation D-27](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — a location defect
- **Milestone** C0 · **Prereq** none · **Kind** documentation · **Changes** docs
- **Risk** An engineer told "the HTML is the specification" and following the index arrives at three retired screens. This is the cheapest item in the programme and one of the most expensive to leave.
- **Verify** `grep -n "design/v1" README.md` returns ≥1 match; no row describes `docs/design/mockups/` as the working reference.

#### CI-05 · No precedence order for rule-versus-rule inside the design system (A-1) — ✅ **DONE**
- **Status** ✅ **CLOSED 2026-07-24 — [ADR-064](DECISIONS.md#adr-064)**, owner ruling **Option (b)**: the ordered list with **accessibility ranked above the artifact's truth**; escalation clause mandatory, covering same-rank collisions. Landed as [§1.7](ZINELY-DESIGN-SYSTEM.md). Closes **D-2's remainder, D-3, D-7, D-8**. **D-1 is partial** — §1.7 supplies the missing axis, but deciding it at rank 4 would re-rank SCREEN-INVENTORY against DESIGN-RULES, so it **escalates** and its remainder is owed by **CI-11** (A-7's exempt screen class). **D-6 expressly NOT closed** — the A-1/A-5 duplicate claim is corrected in the validation report and D-6 stays open under **CI-09**. Two editorial follow-ons owed: §1.5's third column, and the §7.2/§8.2 pointers (the latter landed with this change). **Unblocks CI-09**, its last outstanding prerequisite.
- **Location** `docs/ZINELY-DESIGN-SYSTEM.md` (absent) · `docs/DECISIONS.md`
- **Current** Three precedence rules exist, each on a different axis (DESIGN-RULES: rule vs feature; SCREEN-INVENTORY: itself vs PRD/ROADMAP; §0.2: document vs document). **None settles rule vs rule.**
- **Required** One ordered list, plus the instruction that an unresolved collision is recorded as a defect and escalated, never resolved locally.
- **Authority** [validation A-1](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — *closes D-1, D-3, D-7, D-8; narrows D-2* (**corrected**: the source draft also claimed D-6, which A-5 claims too; D-6 is a content gap and belongs to CI-09)
- **Milestone** C0 · **Prereq** none · **Kind** documentation · **Changes** docs
- **Risk** Without it, every C4–C7 item that hits two competing rules is decided by whoever hit it first — silently, in a PR.
- **Verify** An ADR records accept/reject; if accepted, the ordered list is in the design system.

#### CI-06 · Ruling owed on A-2 — Field · Row · Notice · Menu (+ Sheet, popover)
- **Location** `docs/DECISIONS.md` · gates CI-46 … CI-52
- **Current** Not ruled. Three of the four already ship unnamed (see CI-46, CI-47, CI-48).
- **Required** Accept, reject or amend, in §5's three-part form.
- **Authority** [validation A-2](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [§1.6](ZINELY-DESIGN-SYSTEM.md) — a new object is a system change and needs an ADR
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** C4 either invents objects the system never sanctioned, or does not exist.
- **Verify** ADR recording accept/reject per object.

#### CI-07 · Ruling owed on A-3 — the consequence colour and four control states
- **Location** `docs/DECISIONS.md` · gates CI-38, CI-39, CI-53
- **Current** Not ruled.
- **Required** Accept, reject or amend. A-3's own argument is that a consequence colour is *not* a third accent, because it never marks a next action — so [§7.1](ZINELY-DESIGN-SYSTEM.md)'s prohibition survives.
- **Authority** [validation A-3](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [§7.1](ZINELY-DESIGN-SYSTEM.md); Premium Checklist #30, #37, #106, #107, #108
- **Milestone** C0 · **Prereq** CI-01, CI-13 · **Kind** documentation · **Changes** docs
- **Risk** Rejecting it leaves the repository with no colour for a delete or a failure — the state it is in today.
- **Verify** ADR; if accepted, the §7.1 job table gains one row and the prohibition is restated intact.

#### CI-08 · Ruling owed on A-4 — the Underway band, the fourth motion job, cancellation
- **Location** `docs/DECISIONS.md` · gates CI-40, CI-58
- **Current** Not ruled. Three bands defined; the design system marks §3.8 `> Open:`.
- **Required** Accept/reject the band, the truthful-progress rule (*"progress is truthful or absent — never decorative, never faked, and cancellable where the operation can be stopped"*), and §8.1's fourth announcement job.
- **Authority** [validation A-4](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [§3.8, §8.1](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** The band's **existence** is ADR-gated; its **duration** is separately gated on CI-14. Conflating them re-serialises C3a behind a device recording for no reason.
- **Verify** ADR; the duration explicitly deferred to CI-14 in the same ADR.

#### CI-09 · Ruling owed on A-5 — artifact versus representation ~~⚠️ *amendment*~~ — ✅ **DONE**
- **Status** ✅ **CLOSED 2026-07-24 — [ADR-065](DECISIONS.md#adr-065)**, owner ruling **Option (b)**: **A-5 REJECTED.** The artifact is square and **every representation of the artifact is square**; no exception for being a representation, no size threshold, no per-surface carve-out. Where §2.7's two rows both claim an object, the **paper row governs the corner**. **Not an amendment after all** — §2.7, §5.1 and §13's checklist are *re-affirmed*, RD-4 is *confirmed*, and the ADR supersedes nothing. **CI-73 therefore touches four files, as conformance rather than as a reversal.** Closes **D-5** and audit row 3. **D-6 stays open by owner intent and is re-scoped — it is no longer a radius question**, only rotation/placement semantics; re-owned by **CI-98** so CI-09's closure cannot orphan it. **Unblocks CI-20 and CI-12.**
- **Location** `docs/DECISIONS.md` · gates CI-42, CI-73 · ~~determines whether CI-73 touches **four files or zero**~~ **ruled: four files**
- **Current** Not ruled. [§2.7](ZINELY-DESIGN-SYSTEM.md) says *"Paper, and anything representing paper: square"*; [§5.1](ZINELY-DESIGN-SYSTEM.md) says paper never takes a corner radius; four production sites round it anyway (CI-73).
- **Required** A ruling in one direction. A-5 proposes the distinction; A-5 *also* states *"the alternative resolution — make every representation square too — is equally valid and cheaper; the defect is the ambiguity, not the direction."*
- **Authority** [validation A-5](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — the report's *"most reviewer-visible defect"*; [V1-DESIGN-REFINEMENT RD-4](V1-DESIGN-REFINEMENT.md), which files a rounded cream rectangle as a defect
- ~~**Also owns D-6**~~ — D-6 was deferred here by [ADR-064](DECISIONS.md#adr-064) on the reasoning that it asks whether a rotated page card *is* the artifact. [ADR-065](DECISIONS.md#adr-065) settles the corner geometry identically for both, so that identity question no longer arises for D-6, and **what remains is rotation semantics, not radius**. D-6 moves to **CI-98**.
- **Milestone** C0 · **Prereq** ~~CI-01, CI-05~~ **both satisfied** ([ADR-061](DECISIONS.md#adr-061), [ADR-064](DECISIONS.md#adr-064)) · **Kind** documentation · **Changes** docs
- **Risk** **This is the one C0 item an engineer will be tempted to decide.** ~~It reverses accepted text and needs an ADR that supersedes SYS-5, not a clarification.~~ **Superseded by the ruling:** that sentence assumed A-5 would be *accepted*. It was **rejected**, so nothing was reversed and nothing supersedes SYS-5. It does still set the [roadmap's](V1-IMPLEMENTATION-ROADMAP.md) shape-count condition — the answer being *no radius on the artifact or any representation of it*, which is a rule rather than a number.
- **Verify** ~~An ADR that names which resolution and supersedes the §2.7 clause; the §13 checklist echo edited to match.~~ **Restated to match the ruling that was made:** an ADR that names which resolution — done, [ADR-065](DECISIONS.md#adr-065) — and, because the resolution was rejection, that **supersedes nothing and edits neither §2.7 nor the §13 echo.** Applying the original criterion to ADR-065 would fail it for doing exactly what the owner ruled.

#### CI-10 · Ruling owed on A-6 — five type roles (Value · Input · Technical · Link · Section header) — ✅ **DONE**
- **Status** ✅ **CLOSED 2026-07-24 — [ADR-067](DECISIONS.md#adr-067)**, owner ruling **Option (d)**: all five roles accepted, **each naming its §2.1 register** — Value → Body, Input → Body, Technical → Metadata, Link → Body, Section header → Metadata. **No role enters without a register; no new size; NO SIXTH REGISTER — the five-register architecture is frozen** and §2.1 is textually unchanged. A-6's unnamed "fifth role" was **identified rather than deferred**: it is **Section header**, the only one with two live claims pointing in opposite directions (Subhead sits *above* Body, a list band sits *below* it), resolved to **Metadata**. §6 goes eleven roles → **sixteen**. Closes **D-21** and audit **row 20**. **Unblocks CI-21 — the last of the three HTML re-freezes still gated.**
- **Location** `docs/DECISIONS.md` · gates CI-41
- **Current** Not ruled. §6 defines eleven roles; A-6 adds five, *"roles only… no new sizes."*
- **Authority** [validation A-6](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [§6](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** Sixteen roles against 25 shipped sizes is a bigger reduction problem than eleven; the ruling changes C3b's size.
- **Verify** ADR; if accepted, §6 carries sixteen roles by purpose.

#### CI-11 · Ruling owed on A-7 — a screen class whose subject is the tool ⚠️ *principle-adjacent*
- **Location** `docs/DECISIONS.md`
- **Current** Not ruled. [§4.1](ZINELY-DESIGN-SYSTEM.md) admits only screens whose subject is the user's work.
- **Required** Accept/reject the utility class and its three conditions, **recorded as a narrow exemption from P1** rather than a gap closure.
- **Authority** [validation A-7](ZINELY-DESIGN-SYSTEM-VALIDATION.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** No current screen needs it — **it binds screens that do not exist yet** (Settings, About, Backup, Recovery). Deferring it is legitimate; deferring it silently is not.
- **Verify** ADR recording accept/reject **and** that it is an exemption.

#### CI-12 · Ruling owed on A-8 — modality and the scale clause ~~⚠️ *the scale clause is an amendment*~~ — ✅ **DONE**
- **Status** ✅ **CLOSED 2026-07-24 — [ADR-066](DECISIONS.md#adr-066)**, owner ruling **Option (d)**, ruling A-8's two clauses **separately**. **Modality accepted in full:** §11 gains an eighth rule — keyboard focus order follows the design's order, focus is **visibly** indicated by design not inheritance; touch-assuming rules must name their **keyboard and pointer** equivalents; **hover is out of scope** by decision, *unless a future ADR introduces hover-specific behaviour*. **Scale accepted as a *clarification and derivation*, not a restatement — so it was never an amendment:** §8.2's governing sentence is **retained verbatim and remains authoritative**, with the derivation rule added beneath it and **subordinate by construction** (where they could be read apart, the sentence above wins). **§5.3 growth guidance approved.** Closes **D-26**, **D-20** and the **hover** half of **D-16**. **Decides audit row 18's *resize* half (no) and escalates its §4.7-remedy half (🟡).** **Moves four of the longevity table's five ❌ cells — one to ✅ (density, by §5.3's growth line) and three to ⚠️**, because three carry work owed rather than closure. **Unblocks CI-22 → CI-40 → C3b, CI-33, and CI-31's keyboard half.** **One editorial follow-on is owed and is not a ruling:** §11 rule 8 makes naming a keyboard and pointer equivalent an **obligation**, and **§4.6, §3.1 and §3.7 do not yet carry theirs** — writing them in rides the next design-system pass, and until it lands the longevity table's thumb-zone cell is ⚠️ rather than ✅.
- **Location** `docs/DECISIONS.md` · gates CI-40 (spacing), CI-33
- **Authority** [validation A-8](ZINELY-DESIGN-SYSTEM-VALIDATION.md)
- **Milestone** C0 · **Prereq** ~~CI-01, CI-09~~ **both satisfied** ([ADR-061](DECISIONS.md#adr-061), [ADR-065](DECISIONS.md#adr-065)) — **RULABLE NOW** · **Kind** documentation · **Changes** docs
- **Risk** ~~The scale clause changes accepted text~~ — **superseded by the ruling:** the scale clause was accepted as a *derivation*, so no accepted text changed and **[CI-68](V1-CONFORMANCE-INVENTORY.md)'s authority is untouched** — it still measures against §8.2's original sentence, byte-identical. A-8 also closes four of the longevity table's ❌ cells including page-resize ×2. *(A-8 says "four of §5's **eight** ❌ cells"; the table has **five**. A-8's count was wrong, not the cells.)*
- **Verify** ~~ADR, with the amendment half flagged as an amendment.~~ **Restated to match the ruling made:** an ADR ruling both clauses — done, [ADR-066](DECISIONS.md#adr-066) — and, because the scale clause was accepted as a derivation rather than a restatement, one that **flags no amendment and leaves §8.2's sentence unedited**. Applying the original criterion would fail ADR-066 for doing what the owner ruled.

#### CI-13 · The 140-item Premium Checklist is filed where nobody looks
- **Location** `docs/V1-DESIGN-REFINEMENT.md` (inside a critique of a past release)
- **Current** The checklist covering focus, pressed, selected, disabled, loading and semantic colour lives inside a document about `0.9.0-beta.1`. The sufficiency validation's own first draft audited for gaps without opening it.
- **Required** A location where a reviewer of a *future* screen will find it.
- **Authority** [validation D-27](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — *"a location defect, not an oversight"*; §7's relocation
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** Every control state in C4 is specified in a document C4's implementer has no reason to open.
- **Verify** The checklist is reachable from the §0.2 split table's named owner; the README indexes it.

#### CI-14 · No motion-and-haptics baseline has been recorded on a device
- **Location** `docs/reviews/` (absent) · gates every duration change in C5
- **Current** Three documents describe the timing and disagree three ways: [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md) ~100–150 / ~200–300 / ~300–400 ms; the frozen HTML and `ZinelyMotion.kt:16-19` **two** values (`--fast` 130, `--base` 230) on one easing; the design system three bands, with A-4 proposing a fourth.
- **Required** A recording, then the bands.
- **Authority** [§3.8 `> Open:`](ZINELY-DESIGN-SYSTEM.md) — *"until a motion baseline is recorded on device. **That recording is a precondition for changing any of them.**"*; [V1-DESIGN-REFINEMENT, *"Where this document stops"*](V1-DESIGN-REFINEMENT.md); [§15 open item 2](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** none — **startable today, and one of the three things to start first** · **Kind** documentation · **Changes** docs, motion
- **Risk** Any duration changed before this is changed against a tie-break nobody has taken.
- **Verify** A dated record under `docs/reviews/` naming device, OS build, APK version, with a commit date **earlier than** any commit changing a duration constant.

#### CI-15 · The editor empty state is frozen by ADR-033 and disputed by the critique
- **Location** `feature/editor/.../EditorEmptyState.kt` (148 lines) · `docs/DECISIONS.md#adr-033`
- **Current** Frozen by an accepted ADR; disputed by [V1-DESIGN-ELEVATION](V1-DESIGN-ELEVATION.md).
- **Required** A superseding ADR or an explicit withdrawal — *"not a design system quietly restyling it."*
- **Authority** [§15 open item 3](ZINELY-DESIGN-SYSTEM.md); [DESIGN-RULES R4](design/DESIGN-RULES.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** C6 restyles this file (it carries 5 of the 16 `MaterialTheme` reads — the most of any file). Without a ruling, the restyle *is* the silent supersession §15 forbids.
- **Verify** An ADR superseding or re-affirming ADR-033, dated before C6 touches the file.

#### CI-16 · The optimistic "Saved ✨" contradicts the truth rule
- **Location** `feature/editor/.../EditorSavedConfirmation.kt` (144 lines) · `docs/DECISIONS.md#adr-034`
- **Current** [§9.7](ZINELY-DESIGN-SYSTEM.md)'s truth rule and ADR-034's accepted limitation are in live tension. The correction path exists (`EditorSaveFailure`, ADR-035/036) but the chip is still optimistic.
- **Required** A superseding ADR, or an explicit re-acceptance.
- **Authority** [§15 open item 6](ZINELY-DESIGN-SYSTEM.md); [§9.7](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** As CI-15 — C6 touches the file either way.
- **Verify** ADR recording supersede or re-accept.

#### CI-17 · The page turn in Read is left open
- **Location** `feature/editor/.../ProofRead.kt` (195 lines) · `docs/DECISIONS.md#adr-058`
- **Current** Left open by ADR-058; [§5.2](ZINELY-DESIGN-SYSTEM.md) records a preference, not a rule.
- **Required** A decision or a dated deferral.
- **Authority** [§15 open item 7](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** CI-01, CI-14 · **Kind** documentation · **Changes** docs, motion
- **Verify** ADR or dated deferral in `DECISIONS.md`.

#### CI-18 · Real shelf covers — wire or delete (**ship blocker #3**) — ✅ **DONE**
- **Status** ✅ **CLOSED 2026-07-24 — [ADR-069](DECISIONS.md#adr-069): DELETE.** Owner ruling. The pipeline is removed, not wired — with `ThumbnailRenderer`, `ShelfThumbnails`, `AndroidThumbnailRaster`, `ShelfThumbnailProducer`, `HomeModule` and their tests, plus the second-order dead `ProjectDocumentLayout` and its DI provider, which existed only to serve the producer. `HomeZineCard.thumbnail` and `HomeViewModel`'s thumbnail machinery are unwired. **`CanvasReplayer` and the export path are untouched**, as are `ShelfCover.kt`/`ShelfCoverRecipe.kt` — the riso cover the shelf actually draws. **CI-77 is executed by the same change** (see its Status). **Ship blocker #3 is closed *as written*** — nothing runs unread. Whether the shelf should ever show the user's real page 1 remains an open product question against DoD 3 and 7, re-scoped in [zinely-v1.md §7](zinely-v1.md).
- **Location** `app/.../home/ShelfThumbnailProducer.kt` (118), `app/.../home/AndroidThumbnailRaster.kt` (42), `app/.../home/HomeModule.kt` (61), `app/.../home/ShelfThumbnails.kt` (35), `feature/editor/.../ShelfCover.kt` (336), `ShelfCoverRecipe.kt` (84)
- **Current** *(pre-ruling; retained as the record of what was decided)* `HomeModule` **wired** the full `ThumbnailRenderer(CanvasReplayer(...))` stack into the production graph **unconditionally**, so the app *"renders, encodes and caches a PNG per zine per document edit — plus a decode per zine per cold start and up to 24 bitmaps in an LRU — **for output no surface displays**."* The shelf draws a title-hashed riso cover instead.
- **Required** *"It is dead weight either way: wire it or delete it, but do not leave it running unread."*
- **Authority** [ADR-045 closure note, 2026-07-20](DECISIONS.md#adr-045); [zinely-v1.md §7 blocker #3](zinely-v1.md) at `:123`, *"which outranks the Master Execution Plan's contrary baseline claim"*; [§12.2](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 (ruling) → CI-77 (execution) · **Prereq** none · **Kind** architectural · **Changes** render, persist (cache only), tests
- **Risk** **This is the only item in the inventory with a cost that is being paid right now**, on every device running the beta: battery, storage and cold-start decode for output nothing reads. It is also the only item whose disposition sits with a ship blocker rather than with design.
- **Verify** An ADR records wire-or-delete; `zinely-v1.md §7` blocker #3 closed or explicitly re-scoped.

#### CI-19 · The imposed sheet's blank panels are a separate deferral with a separate owner
- **Location** `feature/editor/.../ProofSheet.kt` (365) · `docs/DECISIONS.md#adr-058` Decision 7
- **Current** An ADR deferral, distinct from CI-18's owner decision. Recorded separately because the roadmap's source document conflates the two halves of [§15 open item 4](ZINELY-DESIGN-SYSTEM.md) exactly once.
- **Required** A decision or a re-dated deferral.
- **Authority** [ADR-058 Decision 7](DECISIONS.md#adr-058); [§12.2](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** Closing CI-18 and calling §12.2 satisfied. It is not — the sheet is the other half.
- **Verify** ADR-058 Decision 7 superseded or its deferral re-dated.

#### CI-20 · The frozen HTML must be re-frozen with the radius decision before any Compose radius work
- **Location** `docs/design/v1/shelf.html`, `bench.html`, `proof.html`
- **Current** 22 distinct `border-radius` values across the three files (`grep -oh "border-radius:[^;}]*" docs/design/v1/*.html | sed 's/border-radius://' | tr -d ' ' | sort -u | wc -l`).
- **Required** [§2.7](ZINELY-DESIGN-SYSTEM.md): *"Three radii exist. Nothing else does."*
- **Authority** [CLAUDE.md · HTML-first](../CLAUDE.md#html-first-ui-workflow-mandatory) — *"first update the HTML specification, then be implemented in Compose — never the reverse"*
- **Milestone** C0 · **Prereq** ~~CI-09~~ **satisfied** ([ADR-065](DECISIONS.md#adr-065) — three radii, and **no radius on the artifact or any representation of it**) — **UNBLOCKED** · **Kind** documentation · **Changes** docs
- **Risk** **The single most expensive sequencing mistake available** is starting CI-42 before this. Inverting the workflow makes Compose the specification and silently overrules `ZinelyDimens.kt:8-13`, whose refusal cites that same workflow.
- **Verify** The three files carry a re-freeze date **later than** the CI-09 ADR; the distinct-radius count equals what CI-09 ruled.

#### CI-21 · The frozen HTML must be re-frozen with the type registers
- **Location** `docs/design/v1/{shelf,bench,proof}.html`
- **Current** The HTML's sizes are the source of the 25 distinct `.sp` values in Compose.
- **Required** [§2.1](ZINELY-DESIGN-SYSTEM.md) five registers — **frozen, and no sixth** ([ADR-067](DECISIONS.md#adr-067)); [§6](ZINELY-DESIGN-SYSTEM.md) ~~eleven roles (sixteen if CI-10 accepts A-6)~~ **sixteen roles**, of which **five carry a named register** ([ADR-067](DECISIONS.md#adr-067)) and eleven do not ([CI-99](#ci-99--six-type-roles-have-no-register-and-heading-is-genuinely-ambiguous)); [§13](ZINELY-DESIGN-SYSTEM.md) *"No sizes invented for this screen."*
- **Authority** as CI-20
- **Milestone** C0 · **Prereq** ~~CI-10~~ **satisfied** ([ADR-067](DECISIONS.md#adr-067) — **sixteen** roles; the five new ones carry a register, the eleven older ones are [CI-99](#ci-99--six-type-roles-have-no-register-and-heading-is-genuinely-ambiguous)) — **UNBLOCKED** · **Kind** documentation · **Changes** docs
- **Re-freeze set complete 2026-07-24:** CI-20 (radius) freed by [ADR-065](DECISIONS.md#adr-065), CI-22 (spacing) by [ADR-066](DECISIONS.md#adr-066), CI-21 (type) by [ADR-067](DECISIONS.md#adr-067). **All three are now gated on nothing but the owner's own pass and may proceed together as one coordinated change** rather than three sequential ones.
- **Risk** **Sixteen** roles ([ADR-067](DECISIONS.md#adr-067)) cannot be derived from 25 sizes without deciding which sizes die, and **every death is a visual change on a goldened surface**. Deciding that in Compose is deciding it at the wrong rank.
- **Verify** Re-freeze date later than the CI-10 ADR; every register traceable to a line in the HTML.

#### CI-22 · The frozen HTML must be re-frozen with the spacing unit
- **Location** `docs/design/v1/{shelf,bench,proof}.html`
- **Required** [§2.2](ZINELY-DESIGN-SYSTEM.md): *"One spacing unit; every gap is a multiple of it."*
- **Authority** as CI-20; [§13](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** ~~CI-12 (A-8's scale clause)~~ **satisfied** ([ADR-066](DECISIONS.md#adr-066) — the scale clause was accepted as a derivation of §8.2, which is the clause this item waited on) — **UNBLOCKED** · **Kind** documentation · **Changes** docs
- **Risk** This is the gate on the programme's long pole (CI-40 → CI-64 → CI-74). It is the **longest designer chain** and the critical path runs through it.
- **Verify** Re-freeze date later than the CI-12 ADR; every gap in the three files a multiple of the named unit.

#### CI-23 · Two shared components have zero production call sites
- **Location** `ui/components/ZButton.kt:175` (`ZStampButton`) · `ui/components/ZAccessibleControl.kt:26` (`Modifier.zinelyControl`) · exercised only by `ZComponentGoldenTest.kt:78,98`
- **Current** `grep -rn "zinelyControl" feature/editor/src/main` returns the declaration and **two comments explaining that the Editor does not use it** (`TypeBar.kt:479`, `:502`). `ZStampButton` appears in no production file.
- **Required** [§1.6](ZINELY-DESIGN-SYSTEM.md) — *"every new token is a permanent tax"*; an object with no consumer is a tax with no return.
- **Milestone** C0 (ruling) → CI-52 (execution) · **Prereq** CI-06 · **Kind** documentation · **Changes** docs, tests
- **Risk** **Deleting either also deletes golden cases** (`z_components_light.png`, `z_components_dark.png` are the only goldens exercising them), which is why the ruling cannot be an engineer's.
- **Verify** An ADR records delete-or-adopt per component.

#### CI-98 · Ruling owed on D-6 — is hand-placement *rotation for effect*, or is it placement?
> *Numbered 98 but filed here, in C0, because that is its milestone. It was created after CI-97 and the
> inventory numbers by creation order, not by position.*
- **Location** `docs/DECISIONS.md` · [§8.3](ZINELY-DESIGN-SYSTEM.md) vs [§5.10](ZINELY-DESIGN-SYSTEM.md), [§4.3](ZINELY-DESIGN-SYSTEM.md), [R10](design/DESIGN-RULES.md)
- **Current** Not ruled. §8.3 forbids *"the page rotates for effect"*; §5.10 describes the page strip as *"small paper cards, **hand-placed with slight rotation**"*, and §4.3 and R10 require it. Both accepted.
- **Required** A ruling. At least three resolutions are live and this item decides none of them: (1) one clause distinguishing **rotation as placement** (a resting angle the object was set down at) from **rotation as effect** (the page turning because the interface is performing); (2) an explicit ruling that §5.10's hand-placement is a **stated exception** to §8.3; (3) an **identity** ruling that a strip thumbnail is not *"the page"* for §8.3's purposes — [ADR-065](DECISIONS.md#adr-065) rejected the artifact/representation distinction **for corner geometry only**, so this route remains open here.
- **Authority** [validation D-6](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [§8.3](ZINELY-DESIGN-SYSTEM.md); [ADR-065](DECISIONS.md#adr-065), which re-scoped this item to rotation semantics and **explicitly declined to extend itself over it**
- **Milestone** C0 · **Prereq** none — CI-09's closure removed its last dependency · **Kind** documentation · **Changes** docs
- **Risk** **Filed so CI-09's closure cannot orphan it.** D-6 was owned by CI-09 while it was believed to be a radius question; [ADR-065](DECISIONS.md#adr-065) settled corner geometry identically for page and thumbnail, which removed the radius half and left the rotation half with no owner. A defect that loses its owner by being half-solved is exactly the drift this inventory exists to prevent. Note it binds **C6/C7** — `ShelfCover.kt` and the page strip both draw the rotation today.
- **Verify** An ADR distinguishing placement from effect, or recording §5.10 as a stated exception; audit row 6 decidable afterwards.

#### CI-99 · Six type roles have no register, and Heading is genuinely ambiguous
> *Numbered 99 but filed here, in C0, because that is its milestone — numbering follows creation order,
> placement follows milestone.*
- **Location** [§6](ZINELY-DESIGN-SYSTEM.md)'s role table · [§2.1](ZINELY-DESIGN-SYSTEM.md)'s five registers · `docs/DECISIONS.md`
- **Current** [ADR-067](DECISIONS.md#adr-067) required every role it *added* to name its §2.1 register, and five did. **The original eleven were out of its scope and carry none.** Five of their names coincide with a register (Display, Subhead, Body, Metadata, Caption); **Heading, Button, Label, Instruction, Warning and Empty state have no register recorded anywhere in the corpus.**
- **Required** A register for each of the six — or an explicit ruling that a role may take its register from context. **Heading is the one that cannot be guessed:** [§6](ZINELY-DESIGN-SYSTEM.md) reserves **Display** for *"the wordmark, the ending, and the one sentence per screen that carries the product's voice"* — which would exclude Heading, **except that the product already sets screen titles in the display serif**, so ruling Heading away from Display is itself a visible change. Its candidates are therefore **Display**, **Subhead** or **Body** — and [§2.1](ZINELY-DESIGN-SYSTEM.md) places Subhead *above* Body, so the candidates sit on **opposite sides of Body**. That is the same opposite-direction ambiguity ADR-067's decision 3 was written to eliminate, left standing for the role that names every screen.
- **Authority** [ADR-067](DECISIONS.md#adr-067) decision 2 (*"Every role names its register, and no role may enter §6 without one"*) and decision 4 (**the five-register architecture is frozen — a role that cannot be expressed by the five is an owner decision, never a sixth register**); [§6](ZINELY-DESIGN-SYSTEM.md); [§2.1](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C0 · **Prereq** none — this item was *created by* [ADR-067](DECISIONS.md#adr-067) and never had one · **Kind** documentation · **Changes** docs
- **Risk** **Filed because ADR-067's first draft claimed this was already done.** Three separate lines asserted *"sixteen roles, each with its register named"* and *"C3b maps roles onto registers instead of negotiating them"* — false for eleven of the sixteen, and it would have told a [CI-41](#ci-41--sixteen-type-roles-are-specified-zero-are-implemented) engineer the mapping work was finished. **CI-41 cannot map rather than negotiate until this closes**, and Heading's register is a visible size change on every goldened surface that has a title.
- **Verify** An ADR records a register for each of the six, or the context rule; §6's table carries a register for **every** role, not five of sixteen.

#### CI-24 · The conformance track is not on the roadmap
- **Location** `docs/ROADMAP.md` · `CHANGELOG.md`
- **Current** ROADMAP.md carries no conformance track; no change-log row exists for any C-milestone.
- **Authority** [CLAUDE.md · Documentation Rule](../CLAUDE.md#documentation-rule-mandatory) — *"Every roadmap change → ROADMAP.md (and a change-log row)"*; [roadmap §0](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C0 · **Prereq** none · **Kind** documentation · **Changes** docs
- **Verify** ROADMAP.md carries the track; a change-log row exists per shipped milestone.

---

### C1 · Conformance guardrails — no visual change

> Ten items. **Zero pixels move.** The acceptance criterion for the whole milestone is that goldens
> are byte-identical after it merges — and that an *injected* defect makes each new net fail. A passing
> suite proves nothing about a net.

#### CI-25 · The largest surface in the app is the least goldened, and C6 will rewrite all of it
- **Location** Goldened Editor composables: `SelectionChrome` (3 goldens), `TypeBar` (2), `PagePreview` (7), `DeskText`/styled block (1). **Ungoldened:** `EditorScreen.kt` (1,006 lines), `EditorPageStrip.kt` (243), `EditorSupplyTray.kt` (220), `EditorContextBar.kt` (188), `EditorEmptyState.kt` (148), `EditorSaveFailure.kt` (197), `EditorSavedConfirmation.kt` (144), `EditorMoveResizeHint.kt` (113), `ReframeControls.kt` (474), `EditTextSession.kt` (135), `ResizeHandles.kt` (191) — **eleven composables, 3,059 lines, zero goldens**
- **Current** 43 of the 61 goldens live in `:feature:editor`, but 28 of those are Shelf (12) and Proof (16).
- **Required** A net before the migration.
- **Authority** [roadmap §1.1](V1-IMPLEMENTATION-ROADMAP.md) — this is a migration against a green suite; [ARCHITECTURE §11.1](ARCHITECTURE.md)
- **Milestone** C1 · **Prereq** none · **Kind** mechanical · **Changes** tests
- **Risk** Without this, C6 changes 3,841 lines with almost no screenshot coverage, and `recordRoborazzi` launders every unintended regression into a new baseline.
- **Verify** A golden per ungoldened composable, **light and dark**; the goldens recorded in a commit separate from any production change.

#### CI-26 · No test anywhere reads the platform accessibility tree
- **Location** `AccessibilityNodeInfo` appears **in no Kotlin file in this repository, production or test** — its only occurrence anywhere is `CLAUDE.md:243`, in the paragraph explaining why it matters. *(The roadmap cites `EditorEffects.kt:36` as a comment mentioning it; that line says `AccessibilityManager`, a different API. The finding survives unchanged — the citation was wrong in both file and symbol, and this document inherited it without opening the line.)*
- **Current** The suite asserts against Compose's *merged semantics* tree; TalkBack reads the *platform* `AccessibilityNodeInfo` tree. They are not the same thing.
- **Required** [§11.3](ZINELY-DESIGN-SYSTEM.md) — **"the platform's tree is the truth."**
- **Milestone** C1 · **Prereq** none · **Kind** architectural · **Changes** a11y, tests
- **Risk** This is the one class of defect the current suite **structurally cannot see**, and it is the class that most recently shipped: a zoom stepper that passed `assertIsNotEnabled` while telling the platform it was enabled (`f4faaa4`, `ReframeControls.ZoomButton`).
- **Verify** The harness exists and runs in CI; **a commit exists in which an injected `enabled`-state defect made it fail.**

#### CI-27 · There is no static gate on design-token discipline — and no static analysis at all
- **Location** `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `feature/editor/build.gradle.kts` — `grep -rn "detekt\|ktlint\|spotless\|lint"` returns **nothing**. `explicitApi()` in 9 modules is the only static gate in the repository.
- **Current** 591 `.dp`, 80 `.sp`, 72 `RoundedCornerShape(` and 48 `Color(0x` literals are unguarded (the last mostly legitimate — 34 are the palette itself in `ZinelyColors.kt`, 9 the legacy scheme in `Theme.kt` — which is why the check must be scoped by enrolment, not by file type).
- **Required** A test or lint that fails on a raw `.dp`/`.sp`/`Color(`/`RoundedCornerShape(` literal in an **enrolled** package, plus the committed enrolment list that defines the term.
- **Authority** [§13](ZINELY-DESIGN-SYSTEM.md)'s spacing and radius boxes; [roadmap §10.2](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C1 · **Prereq** none · **Kind** architectural · **Changes** tests
- **Risk** Without it §13's boxes are re-audited by hand forever, and "enrolled" stays a judgement instead of a committed file.
- **Verify** The check runs in CI; a package joins the enrolment list in the same commit that migrates it; **a commit exists in which an injected literal made CI fail.**

#### CI-28 · Two test suites totalling 419 tests are not named tasks in CI
- **Location** `.github/workflows/ci.yml:105` runs `:app:compileDebugKotlin :data-android:testDebugUnitTest`; `:121` runs `:render-android:verifyRoborazziDebug :feature:editor:verifyRoborazziDebug`
- **Current** `:app:testDebugUnitTest` (**96** tests — the roadmap's 97 counts one commented-out `@Test`) is **never invoked**; `:feature:editor`'s 323 tests are reached only as a side effect of `verifyRoborazziDebug`.
- **Required** Both as named tasks, so a Roborazzi task change cannot silently drop 323 tests.
- **Authority** [roadmap §10.3](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C1 · **Prereq** none · **Kind** mechanical · **Changes** tests
- **Risk** Low to fix, high to leave: the net C6 depends on can be disabled by an unrelated edit.
- **Verify** Both task names appear literally in `ci.yml`; a deliberately failing test in each fails the build.

#### CI-29 · `stateDescription` is produced in production and asserted nowhere
- **Location** Production: `EditorContextBar.kt:169`, `EditorPageStrip.kt:152`. Tests: none — `grep -rn stateDescription` over `*/src/test` returns nothing.
- **Required** [§11](ZINELY-DESIGN-SYSTEM.md) #1 — *"the visible twin is designed, not merely present"*; [DESIGN-RULES R9](design/DESIGN-RULES.md)
- **Milestone** C1 · **Prereq** none · **Kind** mechanical · **Changes** a11y, tests
- **Risk** C6 restyles both files. An unasserted announcement is an announcement that can vanish in a re-skin without a red test.
- **Verify** Each production `stateDescription` has an assertion; the pair lands in one commit (roadmap §7.3).

#### CI-30 · `Role` is asserted in exactly one file
- **Location** `TypeBarTest.kt:258-260` — the only `SemanticsProperties.Role` assertion in the repository
- **Required** [§11.3](ZINELY-DESIGN-SYSTEM.md); [DESIGN-RULES R9](design/DESIGN-RULES.md)
- **Milestone** C1 · **Prereq** CI-26 · **Kind** mechanical · **Changes** a11y, tests
- **Risk** Role is what TalkBack announces a control *as*. A button announced as a generic view passes every current assertion.
- **Verify** Role asserted wherever production sets it, on the platform tree per CI-26.

#### CI-31 · No keyboard-focus-order or traversal-order test exists
- **Location** repository-wide: absent
- **Current** `assertIsNotEnabled` appears in 4 test files (and 2 production sources), `assertIsEnabled` in 3, `onNodeWithContentDescription` in 12 — no ordering assertion anywhere.
- **Required** [Premium Checklist #64](ZINELY-DESIGN-SYSTEM.md) (§13.1, relocated there by CI-13 under [ADR-061](DECISIONS.md#adr-061)); [DESIGN-RULES per-screen checklist](design/DESIGN-RULES.md) — *"order logical"*
- **Authority note** [validation A-8](ZINELY-DESIGN-SYSTEM-VALIDATION.md) records that §11 did not mention **keyboard focus** order while the Premium Checklist requires focus order, and proposed adding it. **That addition is now made — [ADR-066](DECISIONS.md#adr-066), §11 rule 8 — and the traversal half never depended on it:** §4.5 and §11.6 already bind reading/traversal order in the accepted text. The traversal half was therefore delivered *additively*, asserting only what §4.5/§11.6 bind. The keyboard half was blocked on A-8 rather than on CI-13 — **and A-8's §11 clause now exists ([ADR-066](DECISIONS.md#adr-066) rule 8), so that block is lifted.**
- **Milestone** C1 · **Prereq** CI-13 — **landed** (`6b46d0f`, merged `6e55ab6`) · **Kind** mechanical · **Changes** a11y, tests
- **Verify** A traversal-order assertion per surface entry point.
- **Status** ◑ **Partially done** — the **traversal half is delivered; the keyboard-focus half named in this
  item's own title is not**, and was not scheduled here: it needs a focus-traversal harness *and* A-8's §11
  clause to assert against. **The clause now exists — [ADR-066](DECISIONS.md#adr-066) added §11 rule 8
  (2026-07-24), so the keyboard half is UNBLOCKED and schedulable.** What it still needs is the harness,
  and the `focusTarget()` caveat below still applies to `EditorScreen`'s root. `focusTarget()` on `EditorScreen`'s root is deliberately a focus stop with no
  accessibility semantics, so it can never appear in the tree this item asserts against.
  **Delivered:** `SurfaceTraversalOrderTest` (`:feature:editor`, test-only, zero `src/main`) asserts traversal
  order on **six** entry states across the three top-level destinations — Shelf, Editor, and the Proof's four
  acts (Read · Sheet · Print · Fold) — on the **platform `AccessibilityNodeInfo` tree** per
  [§11.3](ZINELY-DESIGN-SYSTEM.md), reusing the CI-26 harness, which gains `platformTraversalStops` to walk
  the platform tree's own child order. Each surface makes **two** assertions, because #64 names three orders:
  the stop sequence is pinned, and that sequence is monotonic in the platform's own reported `boundsInScreen`
  (§4.5, *"the visual order and the accessibility order are the same order"*). The second exists because the
  first cannot stand alone — the tree publishes children in declaration order and sets no re-sorting hints
  (measured: `UNDEFINED` on every node), so a sequence assertion alone is a declaration-order snapshot.
  Note plainly that the pinned sequences are **observed**, not lifted from a specification artifact: they
  detect that an order *changed*, and it is assertion 2 that judges an order against the surface's geometry.
  **Non-vacuity is permanent, not a reverted one-off:** two guards inject a control declared out of order and
  a control moved without being re-declared — the second asserting the sequence check **passes** on that
  broken layout — and every surface additionally asserts that at least one consecutive pair shares a row, so
  the geometry check's horizontal branch cannot quietly stop running.
- **Owed before final acceptance** ⚠ **On-device platform-tree verification is still owed.** §11.3 is quoted
  in full here because the second half of it is the part this item cannot satisfy: *"Read the real
  accessibility tree **on a real device** …; **a green suite is not evidence.**"* This is a Robolectric JVM
  check, and it reaches the platform tree through `getChildId`, a hidden framework method that resolves under
  Robolectric and is blocked by hidden-API enforcement on a device. It runs *before* a device is involved; it
  does not replace the `adb shell uiautomator dump` pass ([device verification](../CLAUDE.md#device-verification-mandatory)).
  Same posture as [CI-85](#ci-85--fourteen-accessibility-action-labels-are-hardcoded-in-the-editor).
- **Also not covered** The `ZSheet` `Dialog` surfaces (Shelf's three sheets, the Proof's paper/share
  choosers) — separate windows with their own roots, outside the Activity composition the harness walks.
  Non-entry conditional states (later Fold steps, error/loading/empty branches, selection-active Editor).
  One phone window only (`w430dp-h932dp-xhdpi`) — no tablet, no large text scale; content outside the window
  is absent from the tree and its absence is indistinguishable from a stop that does not exist.
- **Found, filed, not fixed** Two `src/main` defects this item surfaced now have their own items so that
  ticking CI-31 cannot close them by association: [CI-96](#ci-96--the-blank-page-invitations-sticker-cluster-is-announced-contradicting-the-comment-above-it)
  and [CI-97](#ci-97--the-print-recipes-change-affordance-is-an-unroled-control).

#### CI-32 · No contrast test exists
- **Location** repository-wide: absent
- **Required** [DESIGN-RULES R8](design/DESIGN-RULES.md) — *"Contrast meets AA, including over texture"*; [§11](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C1 · **Prereq** none · **Kind** mechanical · **Changes** a11y, tests
- **Risk** The palette is riso-warm and low-contrast by design; `onDeskFaint` and `inkFaint` are the tokens most likely to fail, and they are used for exactly the small text a contrast bug hides in.
- **Verify** A ratio assertion per foreground/background token pair in `ZinelyColors`, **light and dark**; failures either fixed or recorded as accepted with the ratio stated.

#### CI-33 · No golden is captured at a large text size or the smallest supported width
- **Location** All 43 `:feature:editor` goldens are captured at default font scale; widths are `phone`/`tablet` only (`git ls-files 'feature/editor/src/test/roborazzi/*'`)
- **Required** [roadmap §10.3](V1-IMPLEMENTATION-ROADMAP.md) — a golden *"at the smallest supported width and the largest text size"*; [§11](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C1 · **Prereq** ~~CI-12 (A-8's *density* clause)~~ **CI-12 (A-8's *modality* clause) — satisfied** ([ADR-066](DECISIONS.md#adr-066)) · **Kind** mechanical · **Changes** a11y, tests
- **Terminology corrected 2026-07-24 (owner ruling):** A-8 has **two** clauses, *Modality* and *Scale*. There is no "density clause" — *density* is one of the longevity cells A-8 closes, not a clause. What a smallest-width / large-text golden needs is **modality**. **This item is now unblocked.**
- **Risk** C3b's type work changes reflow, and **reflow changes clipping, ellipsis and line counts** — failures a golden diff shows but only if a golden exists at the size where they occur.
- **Verify** Each surface entry point has a large-text and smallest-width golden.

#### CI-93 · Disabled state is set in production and asserted only incidentally
- **Location** Production: `ZButton.kt:99, 143` (`ZPrimaryButton`), `:179, 192` (`ZStampButton`), `:224, 238, 250, 254` (`ZIconButton`), `:312` (`ZToolButton`), `ZAccessibleControl.kt` (`zinelyControl`), `EditorSupplyTray.kt`, `ReframeControls.kt`. Tests: `assertIsNotEnabled` in **4 test files** (`EditorSupplyTrayTest`, `ProofScreenTest`, `ReframeA11yTest`, `TypeBarTest`), `assertIsEnabled` in 3 — **all against the merged semantics tree**.
- **Current** The fourth of the four properties [roadmap §10.3](V1-IMPLEMENTATION-ROADMAP.md) requires asserted "wherever production sets them", and the only one with no item of its own — CI-29 covers `stateDescription`, CI-30 `Role`, CI-31 focus order. Everything else in this inventory that mentions "disabled" is about **defining** the state (CI-36, CI-44), never about proving the platform is told.
- **Required** [§11.3](ZINELY-DESIGN-SYSTEM.md) — *"the platform's tree is the truth"*; [roadmap §10.3](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C1 · **Prereq** CI-26 · **Kind** mechanical · **Changes** a11y, tests
- **Risk** **This is the exact defect class that shipped in `f4faaa4`** — a zoom stepper that passed `assertIsNotEnabled` while telling the platform it was enabled. CI-26 cites that defect as its own motivation and then does not close it: a harness with no assertion on the property that failed is a net with a hole where the fish went through. **The omission of this item from the first draft is itself the finding.**
- **Verify** Every production `enabled = false` path has a platform-tree assertion; a commit exists in which an injected mismatch made it fail.

---

### C2 · The `:core:ui` extraction — a pure move

#### CI-34 · The entire design system lives inside a feature module
- **Location** `feature/editor/src/main/kotlin/com/aritr/zinely/ui/theme/` (8 files) and `ui/components/` (12 files) → a new `core/ui/`; `settings.gradle.kts`; `app/build.gradle.kts:202`; `feature/editor/build.gradle.kts`; `feature/editor/src/test/roborazzi/` golden paths; the import graph
- **Current** `:app` depends on `:feature:editor` to draw a button. [ARCHITECTURE.md:91](ARCHITECTURE.md) already lists `:core:ui` under **planned**, not realised.
- **Required** Nothing in the design corpus — this is an architecture consequence. Every screen the validation derived (Settings, About, Backup…) would otherwise depend on the editor feature module.
- **Authority** [ARCHITECTURE §2](ARCHITECTURE.md#2-module--package-structure); [validation §6.1](ZINELY-DESIGN-SYSTEM-VALIDATION.md#61-if-zinely-gained-50-new-screens-tomorrow-would-they-still-look-like-one-product)
- **Milestone** C2 · **Prereq** **CI-25 … CI-33 (C1) only** — no token dependency, no design dependency
- **Kind** architectural · **Changes** tests (golden paths)
- **Risk** **Strictly increasing with time.** `GOLDEN_DIR = "src/test/roborazzi"` resolves per module, so the move invalidates every golden path. **Must never be split** — a partial extraction leaves two component homes, which is the duplicate-source-of-truth failure in code rather than docs. And it must not ride with anything else: a diff of moved files and changed files is unreviewable.
- **Verify** `verifyRoborazziDebug` green in both modules after the goldens are re-homed; **the diff contains renames and import lines only**; goldens byte-identical.

---

### C3a · Tokens — ADR-gated (no HTML dependency; nothing is drawn differently)

#### CI-35 · There is no colour whose job is consequence
- **Location** `ui/theme/ZinelyColors.kt:24-66` — **22 tokens** (`grep -cE "^\s+val " ZinelyColors.kt`), none named for danger, error or warning; consumers already improvising: `ZButton.kt:313` (`danger -> colors.coralText`), `ZMenuItem.kt:57` (`danger` flag)
- **Current** A delete, a failure and a destructive confirm are drawn from the accent that also means *this is your move*.
- **Required** [validation A-3](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — one colour whose job is *this will remove, or this has broken*, explicitly **not** a third accent; [§7.1](ZINELY-DESIGN-SYSTEM.md) survives intact
- **Milestone** C3a · **Prereq** CI-07, CI-34 · **Kind** visual (token only) · **Changes** render, tests
- **Risk** Low-medium: one token, and `ZinelyColorsTest` makes drift fail the build. **The risk is a second token arriving with it** — [§1.6](ZINELY-DESIGN-SYSTEM.md): *"every new token is a permanent tax."*
- **Verify** `ZinelyColorsTest` extended; the token's KDoc names its §7.1 job; **goldens byte-identical** (a moved golden here means a call site was migrated, which is C6/C7's work).

#### CI-36 · Four control states have no defined values
- **Location** `ui/components/` — pressed exists on `ZButton` only (`:121`, `:324`); disabled on `ZButton`/`zinelyControl` only (`:99`, `:179`, `:224`, `:312`); selected on `ZMenuItem` only (`:59`); focused via `ZFocusRing`; **no loading state anywhere**
- **Required** A defined treatment for disabled, focused, pressed and selected — the last stated for all three cases: on the page, in a list, and in a gallery.
- **Authority** [validation A-3](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [§5.8](ZINELY-DESIGN-SYSTEM.md); Premium Checklist #37, #106, #107, #108
- **Milestone** C3a · **Prereq** CI-07, CI-13, CI-35 · **Kind** visual (token only) · **Changes** render, tests
- **Verify** Each state has a token; goldens byte-identical until C4 consumes them.

#### CI-37 · The Underway band does not exist as a token
- **Location** `ui/theme/ZinelyMotion.kt:16-19` — two durations (`ZINELY_FAST_MILLIS = 130`, `ZINELY_BASE_MILLIS = 230`), one easing
- **Required** [§3.8](ZINELY-DESIGN-SYSTEM.md) three bands, [A-4](ZINELY-DESIGN-SYSTEM-VALIDATION.md) a fourth
- **Milestone** C3a (**existence**) · **Prereq** CI-08, CI-34 · **Kind** visual (token only) · **Changes** motion, tests
- **Risk** Its **duration** is gated on CI-14 and must not be set here. Adding the band and its number in one commit re-serialises C3a behind a device recording.
- **Verify** The band exists as a named token; its value is either the existing `base` or explicitly marked provisional pending CI-14.

#### CI-38 · The elevation tiers conform but are not recorded as conforming
- **Location** `ui/theme/ZinelyElevation.kt` (63 lines), `ui/theme/ZinelyShadow.kt` — three tiers (`shadow1`/`shadow2`/`shadowLift`), re-tuned per theme, one light source, no x-offset, no spread
- **Current** **This area already conforms**, including the per-theme re-tune [§2.6](ZINELY-DESIGN-SYSTEM.md) demands. It is recorded nowhere that it does.
- **Required** [§2.4](ZINELY-DESIGN-SYSTEM.md) — three tiers, one immovable light, *"shadow does the work; tonal elevation does not exist here."*
- **Milestone** C3a · **Prereq** CI-01 · **Kind** documentation · **Changes** docs
- **Risk** None to the code. The open question — which tier a *new* object belongs to ([audit row 2](ZINELY-DESIGN-SYSTEM-VALIDATION.md)) — is closed by no addition and stays open.
- **Verify** KDoc on each tier names its §2.4 role; the audit-row-2 gap is recorded as still open.

#### CI-94 · Five raw colour literals live outside the palette
- **Location** `ZButton.kt:114` (`Color(0xFFC64E34)` = `coralStrong`), `:115` (`Color(0xFF264653)` = `stamp`) · `ZinelyElevation.kt:33` (`LightShadow = Color(0xFF23201C)` = `ink`)
- **Current** 43 of the 48 `Color(0x` literals are legitimate — 34 are the palette itself, 9 the legacy scheme CI-69 deletes. **Five are not**, and they split into two kinds. **These three re-type a colour that is identical in both themes**: `coralStrong`, `stamp` and `ink` all sit outside `ZinelyColors.kt`'s dark override block, so replacing each literal with its token is a pure rename. *(The other two are **CI-95** — a separate item precisely because they are not.)*
- **Required** [roadmap §10.2](V1-IMPLEMENTATION-ROADMAP.md) — *"Every `Color(0x…)` literal in production lives in `ZinelyColors.kt`, and every token's KDoc names its §7.1 job"*; [§7.1](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C3a · **Prereq** CI-34 · **Kind** mechanical · **Changes** render, tests
- **Risk** Low individually; structurally it is the CI-27 check's first real test, and the reason that check must be scoped by *enrolment* rather than by file — a naive rule fails 34 times on the palette that defines the tokens.
- **Verify** **Every** token's KDoc names its §7.1 job, not only the token CI-35 adds; **goldens byte-identical in both themes** — these three values are theme-invariant, so a moved pixel means the wrong token was chosen. *(The repository-wide condition — no `Color(0x` outside `ZinelyColors.kt` — closes at CI-95, not here.)*

---

### C3b · Tokens — HTML-gated (each changes a drawn value; each needs the HTML first)

> Four items. **CI-43 is printed below with them** because it is the act that completes CI-41, but it is
> assigned to **C6** — it cannot land until CI-60 removes the last reader (see its Milestone line).

#### CI-39 · There is no spacing scale, by documented decision
- **Location** `ui/theme/ZinelyDimens.kt:8-13` (the refusal) and `:16-28` (four values: `MinTouchTarget`, three focus-ring values). Referenced **5 times in the entire application**. Against **591 `.dp` literals across 61 distinct values** in production.
- **Current** The KDoc refuses a scale *on purpose*: *"There is deliberately no spacing scale and no radius scale here… Inventing a scale would put a second, competing source of truth next to the HTML — exactly what the Documentation Rule and the HTML-first workflow forbid."*
- **Required** [§2.2](ZINELY-DESIGN-SYSTEM.md), [§13](ZINELY-DESIGN-SYSTEM.md) — *"One spacing unit; every gap is a multiple of it."*
- **Milestone** C3b · **Prereq** **CI-22 (the HTML re-freeze)**, CI-34, CI-27 · **Kind** architectural · **Changes** render, tests
- **Risk** **The highest in the programme.** Whoever adds this scale is overruling a documented decision that cites two standing rules — which is exactly why CI-22 must land first. The 61 distinct values are an upper bound on the spacing problem (they include sizes, strokes and radii), but no reading of them yields "one unit."
- **Verify** Every token traceable to a line in the re-frozen HTML; **goldens byte-identical** (migration is C6/C7); the CI-27 check enrolls no package yet.

#### CI-40 · There is no radius family
- **Location** `ui/theme/ZinelyDimens.kt:8-13` (the same refusal) against **19 distinct `RoundedCornerShape` forms** in 72 occurrences: `1, 2, 3, 4, 6, 8, 9, 10, 11, 12, 13, 14, 16 .dp`, `RoundedCornerShape(50)`, `RoundedCornerShape(metrics.radius)`, and four asymmetric forms
- **Required** [§2.7](ZINELY-DESIGN-SYSTEM.md) — *"Three radii exist. Nothing else does"*: paper square, chrome one shared radius, pills fully round.
- **Milestone** C3b · **Prereq** **CI-20**, CI-09, CI-39 · **Kind** architectural · **Changes** render, tests
- **Risk** As CI-39. `ZinelyDimens`'s KDoc says the trilogy takes *"sixteen distinct values"*; it now takes 22 — the KDoc is **dated, not wrong**, and is itself the drift this programme exists to end.
- **Verify** The count of distinct forms equals what CI-09 ruled; every form resolves to a named token.

#### CI-41 · Sixteen type roles are specified; zero are implemented
- **Location** `ui/theme/Type.kt` — `ZinelyTypography` carries **two `FontFamily` values and no roles**; the Material3 `Typography` at `:68-76` defines exactly one style (`bodyLarge`, `FontFamily.Default`). Against **80 `.sp` literals across 25 distinct sizes**: `9.5, 10, 10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5, 16, 17, 19, 19.5, 20, 21, 22, 23, 24, 26` plus `0.sp` and `0.5.sp` tracking
- **Required** [§6](ZINELY-DESIGN-SYSTEM.md) ~~eleven roles by purpose (sixteen if CI-10 accepts A-6)~~ **sixteen roles by purpose** ([ADR-067](DECISIONS.md#adr-067)), **five of the sixteen already mapped to a §2.1 register** ([ADR-067](DECISIONS.md#adr-067)); **the other eleven are not** — see [CI-99](#ci-99--six-type-roles-have-no-register-and-heading-is-genuinely-ambiguous), which must close before this item can map rather than negotiate; [§2.1](ZINELY-DESIGN-SYSTEM.md) five registers, **frozen**; [§13](ZINELY-DESIGN-SYSTEM.md) *"No sizes invented for this screen."*
- **Milestone** C3b · **Prereq** **CI-21**, ~~CI-10~~ **satisfied** ([ADR-067](DECISIONS.md#adr-067)), CI-34 · **Kind** architectural · **Changes** render, a11y, tests
- **Risk** **High, and different in kind from spacing:** type changes reflow, and reflow changes clipping, ellipsis and line counts — failures a golden diff shows but a human must judge. Compounded by Fraunces shipping as the static 9pt optical cut, because `FontVariation`'s `opsz` is ignored below API 26 and minSdk is 24 (`Type.kt:38-45`). **Collides with execution-plan F3** — coordinate, do not duplicate.
- **Verify** Every role traceable to a register in the re-frozen HTML; CI-33's large-text goldens re-checked; goldens otherwise byte-identical.

#### CI-42 · The UI and the PDF are drawn from two different font sets
- **Location** UI: `feature/editor/src/main/res/font/` — `inter_regular`, `inter_medium`, `inter_semibold`, `inter_bold`, `fraunces_semibold` (**5 faces**). Render/PDF: `render-android/src/main/assets/fonts/` — `Inter-Regular`, `Inter-Italic`, `Inter-Bold`, `Inter-BoldItalic` (**4 faces**).
- **Current** The two homes are not merely separate — **their contents diverge**. The export has no Medium and no SemiBold, and **no Fraunces at all**; the UI has no italics. Text the user styles on screen cannot be rendered at the same weight in the artifact, and the voice face does not exist in the printed object.
- **Required** [§5.1](ZINELY-DESIGN-SYSTEM.md) the artifact is the subject; [§6](ZINELY-DESIGN-SYSTEM.md) one set of roles; [ADR-039](DECISIONS.md#adr-039) `export == preview`
- **Authority note** [execution plan](zinely-v1-execution-plan.md) makes F3 a Foundation on the critical path and names closing this split as its content. **This item is shared with the feature axis; the execution plan is superior on its order.**
- **Milestone** C3b · **Prereq** CI-41 · **Kind** architectural · **Changes** render, tests
- **Risk** A typography conformance that stops at Compose makes the screen and the printed zine diverge — the one divergence this product cannot tolerate. **And the most natural fix for a missing weight is a CDN pull, which the privacy invariant forbids** (`Type.kt:18-22` makes the prototypes' `fonts.googleapis.com` pulls unreachable by construction; any work here must preserve that).
- **Verify** One registry; `:render-android:verifyRoborazziDebug` green across its 18 goldens; a printed sample compared against the screen at the same weights; **no network dependency added** (re-checked at CI-91).

#### CI-43 · The Material3 type scale never retired
- **Location** `ui/theme/Type.kt:68-76` and its provision at `Theme.kt:150`
- **Current** Its own KDoc says it is *"deliberately unchanged… Screens adopt `ZinelyTypography` as they are reskinned (M2–M5), and this scale retires with the last of them."* **The retirement never happened**: 16 production reads remain (CI-60).
- **Required** One type source.
- **Authority** [§6](ZINELY-DESIGN-SYSTEM.md); [§12.5](ZINELY-DESIGN-SYSTEM.md) — no platform default accepted without a decision
- **Milestone** **C6** · **Prereq** CI-41 (C3b), CI-60 · **Kind** mechanical · **Changes** render, tests
- **Risk** Deleting it before CI-60 is a compile break; deleting it after is a no-op. It is listed separately from CI-41 because it is the only item that proves CI-41 finished. *(The first draft filed this in C3b while giving it a C6 prerequisite — the only dependency-ordering violation in the inventory, and it contradicted this item's own risk line. Corrected: an item cannot sit in a milestone that closes before its prerequisite.)*
- **Verify** `Type.kt` contains no `androidx.compose.material3.Typography`; the app compiles.

---

### C4 · Object layer

> Eleven items. Each **creates** an object in `:core:ui`. Adoption at the call sites is C6/C7 and is
> listed there — the split is deliberate, because either half can ship without the other and only the
> split makes that visible.

#### CI-44 · `ZTextField` is not a Field
- **Location** `ui/components/ZTextField.kt:36-42` (the whole public signature) — one production call site, `ShelfSheets.kt:221`
- **Current** No disabled, no error, no label, no supporting text, no placeholder; `singleLine = true` is hardcoded at `:56`, not a parameter. Its KDoc scopes it to *"the frozen rename field."*
- **Required** [validation A-2](ZINELY-DESIGN-SYSTEM-VALIDATION.md) Field, in §5's three-part form, with A-3's states
- **Milestone** C4 · **Prereq** CI-06, CI-07, CI-34, CI-36 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Medium. Every screen the validation derived needs a field with an error state; the first one to need it will otherwise write a second field.
- **Verify** `ZComponentGoldenTest` extended per state, **light and dark**; the accessibility gate (CI-26) applies to the new control.

#### CI-45 · There is no Row primitive, and two have been invented independently
- **Location** `ShelfSheets.kt:110` (`PaperChoice`) · `ProofPrint.kt:246` (`RecipeRow`)
- **Current** Two private composables in two files solve the same problem with no shared ancestor.
- **Required** [validation A-2](ZINELY-DESIGN-SYSTEM-VALIDATION.md) Row — *"supplies the container the Switch, the sample gallery and both section headers need"*
- **Milestone** C4 · **Prereq** CI-06, CI-34 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** This is [validation §6.1](ZINELY-DESIGN-SYSTEM-VALIDATION.md)'s central prediction already happening twice, before any new screen exists.
- **Verify** One Row in `:core:ui` with a golden; CI-70/CI-72 adopt it.

#### CI-46 · There is no Notice primitive, and three have been invented independently
- **Location** `EditorSaveFailure.kt` (197 lines) · `EditorMoveResizeHint.kt` (113) · `ProofScreen.kt:416` (`ProofErrorPane`)
- **Current** Three implementations of one object: a message, an optional action, a dismiss.
- **Required** [validation A-2](ZINELY-DESIGN-SYSTEM-VALIDATION.md) Notice — *"the save-failure banner and the coach marks are Notices… this is largely writing down what exists before it is copied wrong"*
- **Milestone** C4 · **Prereq** CI-06, CI-34, CI-35 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Medium. Two of the three carry live regions and dismissal semantics; a merge that flattens them loses announcements. Adoption is CI-66 and must not ride along.
- **Verify** One Notice with goldens per variant; each of the three call sites' existing a11y assertions still green after CI-66.

#### CI-47 · There is no Menu container — menus are plain `Column`s at call sites
- **Location** `ui/components/ZMenuItem.kt:52-60` (the item exists, with `selected`, `danger`, `ZSelectedStyle`); its KDoc: *"Menus are plain `Column`s at call sites."* Consumers: `ShelfSheets.kt`, `ProofPrint.kt`
- **Required** [validation A-2](ZINELY-DESIGN-SYSTEM-VALIDATION.md) Menu; [§2.4](ZINELY-DESIGN-SYSTEM.md)'s tier list already names Sheet and popover
- **Milestone** C4 · **Prereq** CI-06, CI-34 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Low-medium. The container owns traversal order and the "one of these is selected" semantics — the part a plain `Column` cannot carry.
- **Verify** A Menu with a golden; a traversal-order assertion (CI-31) on it.

#### CI-48 · The only progress indicator in the app is a Material3 default on the first screen of a cold start
- **Location** `app/.../ZinelyNavHost.kt:9` (import), `:253`, `:328` — `CircularProgressIndicator()` in both boot paths. The design-system loading treatment, `Modifier.zinelySweep()` (`ZSweep.kt:28`), has **one** call site: `ShelfStates.kt:377`.
- **Current** A platform default, unstyled, on the surface a cold-started user sees first.
- **Required** [§12.5](ZINELY-DESIGN-SYSTEM.md) — no platform default accepted without a decision; [A-4](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — progress is truthful or absent
- **Milestone** C4 · **Prereq** CI-06, CI-08, CI-34 · **Kind** visual · **Changes** pub, render, tests
- **Risk** Low to change, high in perceived quality: [§1.3](ZINELY-DESIGN-SYSTEM.md)'s lowest-finish rule applies hardest to the first thing drawn.
- **Verify** A golden for the progress primitive and the boot path exercised in a test. *(The repository-wide condition — no `CircularProgressIndicator` in production — cannot close here: CI-78 removes the last uses on the bench and proof load surfaces, in C7. It is verified at CI-78, not at this item.)*

#### CI-49 · There is no Chip object; the type bar invents its own
- **Location** `TypeBar.kt:479-510` — the chip is built inline, deliberately not using `zinelyControl` (*"not the house helper"*, `:479`) and with `indication = null` re-derived by hand (`:502`)
- **Required** [§5.7](ZINELY-DESIGN-SYSTEM.md) Chip
- **Milestone** C4 · **Prereq** CI-06, CI-34, CI-36 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Medium. `TypeBarTest.kt:501` records a past regression here — *"`zinelyControl`'s 48dp minimum sat INSIDE `.size(40.dp)`"* — so the touch-target assertion must survive the extraction unchanged.
- **Verify** A Chip in `:core:ui` with light/dark goldens; `TypeBarTest`'s touch-target assertions green after CI-63 adopts it.

#### CI-50 · There is no Toolbar object; three surfaces implement one
- **Location** `EditorContextBar.kt` (188 lines) · `TypeBar.kt` (706) · `ProofPrint.kt:141-190` (the export row)
- **Required** [§5.9](ZINELY-DESIGN-SYSTEM.md) Toolbar; [§12.3](ZINELY-DESIGN-SYSTEM.md) chrome must not compete with the artifact
- **Milestone** C4 · **Prereq** CI-06, CI-34 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Medium-high. These three differ in real ways (the context bar carries `stateDescription`, the type bar carries a text-edit session). A premature merge is worse than three; the item is the **shared object**, not a forced unification — what unifies is decided by CI-06's ruling.
- **Verify** A Toolbar with goldens; each of the three either adopts it (C6/C7) or records why it cannot.

#### CI-51 · No component has a loading state
- **Location** `ui/components/` — repository-wide, zero
- **Required** [A-3](ZINELY-DESIGN-SYSTEM-VALIDATION.md)'s four states plus [A-4](ZINELY-DESIGN-SYSTEM-VALIDATION.md)'s Underway band; Premium Checklist #82, #101
- **Milestone** C4 · **Prereq** CI-07, CI-08, CI-36, CI-37, CI-48 · **Kind** visual · **Changes** pub, a11y, motion, render, tests
- **Risk** The export path is the one place the app blocks on real work (`exportBusy` at `ZinelyNavHost.kt:217` disables the row and draws nothing else). Today "busy" is expressed only as absence.
- **Verify** A loading state with a golden on every control that can be busy; the state announced (`a11y`).

#### CI-52 · Execute the ruling on the two zero-call-site components
- **Location** `ui/components/ZButton.kt:175` · `ui/components/ZAccessibleControl.kt:26` · `ZComponentGoldenTest.kt:78,98` · goldens `z_components_light.png`, `z_components_dark.png`
- **Current** As CI-23.
- **Milestone** C4 · **Prereq** **CI-23** · **Kind** mechanical · **Changes** pub, tests
- **Risk** Deleting removes golden cases; the golden re-record must be a separate, reviewed commit from the deletion.
- **Verify** Each component is deleted **or** has a production call site — whichever CI-23 ruled — and `ZComponentGoldenTest` matches.

#### CI-53 · The nav host's two boot-error branches are unstyled platform defaults
- **Location** `app/.../ZinelyNavHost.kt:257-267` and `:330-340` — a bare `Text(state.message)` plus an inline `androidx.compose.material3.TextButton` at `:265` and `:338`, in a `Column` with a `24.dp` literal
- **Current** The app already ships `ZStatusPane` — *"the shared error/empty chrome: a 56×56 r16 tinted badge, a serif 22sp heading, a soft 14.5sp body, then the caller's CTA"* — used by Shelf and Proof. The two error states a user actually hits on a failed reopen use none of it, and are drawn from `MaterialTheme` roles Theme.kt documents as abused.
- **Required** [§12.5](ZINELY-DESIGN-SYSTEM.md); [DESIGN-RULES R4, R5, R11](design/DESIGN-RULES.md) — *"no screen a beginner can get stuck on"*
- **Milestone** C4 · **Prereq** CI-34 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Low. Two branches, one existing component. Left alone, these are the least-finished screens in the product and they appear at the worst moment.
- **Verify** Both branches use `ZStatusPane`; goldens added; copy from VOICE (CI-82).

#### CI-95 · Two components hardcode the light value of a colour that differs in dark
- **Location** `ZButton.kt:203` (`ZStampButton`'s label) · `ZSnackbar.kt:98` — both `Color(0xFFF4EFE6)`
- **Current** That literal is **`paper` in the light theme only**. `ZinelyColors.kt:74` sets `paper = Color(0xFFF4EFE6)`; `:106` overrides it to `Color(0xFFEDE6D9)` in dark. Both components therefore draw *light* paper on a dark surface today, and **tokenising them changes what renders in dark.**
- **Required** [§2.6](ZINELY-DESIGN-SYSTEM.md) — *"two rooms, not one room inverted"*; [roadmap §10.2](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** **C4** · **Prereq** CI-34, CI-94 · **Kind** visual · **Changes** render, tests
- **Risk** **Filed apart from CI-94 because it is not the same kind of change.** An earlier draft grouped all five literals into one C3a item whose acceptance criterion is *"goldens byte-identical"* — a criterion these two sites cannot meet, in the one milestone whose entire purpose is that nothing moves. **A task that cannot pass its own gate is worse than an unscheduled one**: it gets forced through by re-recording, which is the golden laundering this programme exists to prevent.
- **Verify** The dark goldens move and **each diff is reviewed individually** as the intended correction; the light goldens are byte-identical. `ZStampButton`'s participation is contingent on CI-23 — if it is deleted, only `ZSnackbar.kt:98` remains.

---

### C5 · Motion and haptics

#### CI-54 · Fourteen production animation sites hardcode a duration
- **Location** `EditorSavedConfirmation.kt:88` (150), `:89` (200) · `EditorSaveFailure.kt:141` (150), `:142` (200) · `ProofFold.kt:325` (320), `:395` (550), `:400` (600), `:405` (420), `:410` (420) · `ReframeControls.kt:447` (300, 600) · `ShelfCover.kt:85` (380) · `ShelfStates.kt:458` (820), `:464` (700) · `ZSweep.kt:41` (1_500)
- **Current** `ZinelyMotion`'s own KDoc says *"Read it from `LocalZinelyMotion`; never hardcode a duration at a call site."* **The tokens are consumed at exactly two production sites, both in `ZButton` (`:124`, `:327`).** Everywhere else reads only `reduceMotion` and supplies its own number. `ProofScreen.kt:325` is the honest exception — a named `PROOF_ACT_MILLIS` constant with the frozen CSS quoted at `:124`.
- **Required** [§3.8](ZINELY-DESIGN-SYSTEM.md) three bands (four with A-4); every animation reads from the band it belongs to
- **Milestone** C5 · **Prereq** **CI-14**, CI-37 · **Kind** behavioural · **Changes** motion, render, tests
- **Risk** Medium-high, and **gated**: changing any of these before CI-14 changes them against a tie-break nobody has taken. Several *are* traceable to the specification — `ProofFold.kt:394` quotes `.55s` from the frozen CSS in a comment, `ZSweep.kt:41` quotes the 1.5 s shimmer — which is the finding underneath the finding: **the frozen HTML itself carries more than two durations**, so §3.8's three bands cannot be implemented from the token layer as it stands. That is what CI-14 has to resolve, and it is why this item is a C5 problem and not a search-and-replace.
- **Verify** No `tween(` with a literal duration in production; **device recording** at the frozen beats; a reduced-motion pass confirming the static state is unchanged.

#### CI-55 · No animation is interruptible from its current velocity
- **Location** every site in CI-54, plus `ZSheet.kt:90`, `ZSnackbar.kt:71`, `ZToast.kt:53`, `ShelfCover.kt:126`, `ProofScreen.kt:328-330`
- **Current** Every animation is a `tween`. **`tween` is not interruptible from velocity**; a gesture reversed mid-animation restarts rather than redirects.
- **Required** [§8.4](ZINELY-DESIGN-SYSTEM.md) — interruptibility from current position and velocity, called *"the single reliable divider between physical and animated"*
- **Milestone** C5 · **Prereq** CI-14, CI-54 · **Kind** behavioural · **Changes** motion, render, tests
- **Risk** **This is a property of every call site, not a token** — it cannot be fixed in `ZinelyMotion` and then declared done. It is the largest behavioural item in C5 and it is invisible in every screenshot.
- **Verify** **Device only.** Both passes; a recorded gesture-reversal at each animated surface.

#### CI-56 · Progress is expressed only as absence, and nothing is cancellable
- **Location** `ZinelyNavHost.kt:217` (`exportBusy` disables the export row) · `:253`, `:328` (the two spinners) · `ExportViewModel.kt` (`ExportUiState.Working`)
- **Required** [A-4](ZINELY-DESIGN-SYSTEM-VALIDATION.md) — *"progress is truthful or absent — never decorative, never faked, and cancellable where the operation can be stopped"*
- **Milestone** C5 · **Prereq** CI-08, CI-37, CI-48, CI-51 · **Kind** behavioural · **Changes** pub, a11y, motion, render, tests
- **Risk** Medium. A PDF render is the one operation long enough to need this; making it cancellable touches the export path, not just its chrome.
- **Verify** Progress is truthful or absent at every busy surface; cancellation exists where the operation can be stopped, or its absence is recorded as a limitation.

#### CI-57 · The Editor fires no house haptic at all
- **Location** 24 `haptics.perform(...)` call sites exist, in **`HomeScreen.kt` (4), `ProofPrint.kt` (6), `ProofScreen.kt` (7), `ShelfCard.kt` (2), `ShelfSheets.kt` (5)** — **zero in any Editor surface**, against `SnapGuides.kt`, `EditorGestures.kt` (181 lines), `ResizeHandles.kt`, `ReframeOverlay.kt`
- **Current** The one surface built entirely on continuous direct manipulation — drag, resize, rotate, snap-to-guide, zoom clamp — is the one surface with no physical feedback. `ZinelyHaptic.Snap` and `.Boundary` exist and are used only for a menu choice and a long-press.
- **Required** [§3.7](ZINELY-DESIGN-SYSTEM.md) — haptics for physical events, never the sole signal
- **Milestone** C5 · **Prereq** CI-14 · **Kind** behavioural · **Changes** pub, a11y, motion
- **Risk** Medium. A snap that is announced (`f4faaa4`'s lineage) but not felt is exactly the "correct but not physical" reading §8.4 warns about. Adding haptics to a gesture path also risks firing them per-frame.
- **Verify** **Device only**, both passes; a reduced-motion pass confirming silence (`ZinelyHaptics` already silences under reduced motion, `:55-57`).

#### CI-58 · The type bar invents a fifth haptic verb from the platform
- **Location** `TypeBar.kt:123` — `haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)`, guarded by a hand-written `if (!reduceMotion)`
- **Current** `ZinelyHaptics`'s KDoc: *"The four haptic verbs of the frozen spec — the complete vocabulary. **There is no fifth verb, and a surface never invents a pattern inline.**"* This site uses the Compose platform API, not `LocalZinelyHaptics`, and re-derives the reduced-motion guard the house type already applies.
- **Required** [§3.7](ZINELY-DESIGN-SYSTEM.md); [§12.5](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C5 · **Prereq** CI-14 · **Kind** behavioural · **Changes** pub, motion
- **Risk** Low to fix, and it is the only violation of a rule the codebase states in its own words.
- **Verify** No `performHapticFeedback` in production; the site reads a house verb; the reduced-motion guard comes from `ZinelyHaptics`.

#### CI-59 · Twenty of twenty-four haptic invocations fire on a plain tap
- **Location** `HomeScreen.kt:207,235,248,259` · `ProofPrint.kt:160,179,187,213,234,239` · `ProofScreen.kt:227,233,241,259,384,385` · `ShelfSheets.kt:316` — all `ZinelyHaptic.Tick` on button clicks
- **Current** `Boundary` fires twice (`ShelfCard.kt:157` long-press, `ShelfSheets.kt:253` delete), `Snap` three times (paper choice, rename commit, duplicate), `Success` once (`ProofScreen.kt:255`, the climax).
- **Required** [§3.7](ZINELY-DESIGN-SYSTEM.md) — physical events only
- **Milestone** C5 · **Prereq** CI-14 · **Kind** behavioural · **Changes** motion
- **Risk** **This item asserts no verdict.** Whether a tap is a "physical event" is a design reading, and the audit's outcome may be "all twenty are correct." The discrepancy recorded here is that **the audit §3.7 implies has never been performed**, not that the sites are wrong.
- **Verify** A dated audit record naming, per call site, which physical event the verb corresponds to — or removing it.

---

### C6 · Editor conformance — the floor

> Thirteen items across 14 files and **3,841** lines (`wc -l` over the fourteen Editor-family sources; the roadmap's 3,796 does not reproduce from that set). **The MVI core in `:core:editor` is untouched** — this
> is a re-skin. Decompose by surface, one surface per PR, never one Editor PR.

#### CI-60 · Sixteen production reads of `MaterialTheme`, all in the Editor
- **Location** `EditorEmptyState.kt:96,105,113,126,146` (5) · `EditorMoveResizeHint.kt:89,95,107` (3) · `EditorSupplyTray.kt:105,210,215` (3) · `EditorSavedConfirmation.kt:114,121` (2) · `EditorPageStrip.kt:179` · `EditorSaveFailure.kt:161` · `EditTextSession.kt:128`
- **Current** Seven files read a colour and type vocabulary `Theme.kt:14-24` explicitly forbids: *"never from `MaterialTheme.colorScheme` (whose roles are a different vocabulary)."*
- **Required** Every surface reads the token layer.
- **Authority** [§1.3](ZINELY-DESIGN-SYSTEM.md) lowest-finish; [§12.5](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C6 · **Prereq** CI-34, CI-39, CI-40, CI-41, C4 · **Kind** visual · **Changes** render, tests
- **Risk** **Highest in the programme.** These are the styles a screenshot diff catches only if a golden exists — CI-25 is the reason it will.
- **Verify** `grep -rnE 'MaterialTheme\.'` returns no non-comment line in `feature/**/src/main`; CI-25's goldens re-recorded in a separate commit with **each diff reviewed individually**.

#### CI-61 · Twelve Editor files import Material3 widgets
- **Location** `EditorContextBar.kt:27-29` (Icon, IconButton, Surface) · `EditorEmptyState.kt:13` · `EditorMoveResizeHint.kt:12-13` · `EditorPageStrip.kt:15` · `EditorSavedConfirmation.kt:16` · `EditorSaveFailure.kt:16-17` · `EditorScreen.kt:14-17` (ButtonDefaults, Surface, Text, TextButton) · `EditorSupplyTray.kt:12` · `EditTextSession.kt:8-9` (LocalTextStyle) · `ReframeControls.kt:37-39` · `TypeBar.kt:25-28` (LocalTextStyle, ProvideTextStyle, Surface, Text) · `DeskText.kt:3` (`ColorScheme` in a signature)
- **Current** Every Editor surface draws with `Text`, `Surface`, `Icon`, `IconButton` or `TextButton`. Shelf has **one** such import (`ShelfCard.kt:49`); Proof has **zero**.
- **Required** No Material3 in production outside `:core:ui`.
- **Authority** [§1.3](ZINELY-DESIGN-SYSTEM.md); [roadmap §10.2](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C6 · **Prereq** CI-34, C4 · **Kind** visual · **Changes** render, a11y, tests
- **Risk** Highest. `DeskText.kt:3` is the subtle one — an M3 type in a **public signature**, so removing it is an API change, not a re-skin.
- **Verify** No `androidx.compose.material3` import in `feature/**/src/main`; both device passes.

#### CI-62 · The text-edit panel is an ad-hoc `Surface`, not the Sheet object
- **Location** `EditorScreen.kt:845-849` — a Material3 `Surface` with `.imePadding()`
- **Required** [§2.4](ZINELY-DESIGN-SYSTEM.md)'s tier list; the Sheet object (`ZSheet.kt`), which already exists and is *"deliberately NOT Material3's ModalBottomSheet"* per [ADR-049](DECISIONS.md#adr-049)
- **Milestone** C6 · **Prereq** CI-34, CI-61 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Medium-high. `ZSheet` is a `ui.window.Dialog` — window-level modality, focus containment, TalkBack isolation. Moving the text panel into it **changes IME behaviour**, which is the exact interaction CI-68 must then re-verify.
- **Verify** The panel is a `ZSheet` or the deviation is recorded; the IME check in CI-68 re-run on device.

#### CI-63 · The Editor uses zero shared components
- **Location** all 14 Editor sources; the type bar's inline chip (`TypeBar.kt:479-510`), `EditorScreen.kt:557-561` (a `TextButton` coloured with `ButtonDefaults.textButtonColors(contentColor = ZinelyTheme.colors.coral)` — a token threaded into a Material widget), the three Notices, the two toolbars
- **Current** **Zero.** Proof uses 8 distinct shared components; Shelf uses 12; the Editor uses none. `EditorScreen.kt:561` is the tell — the token layer *is* reachable from the Editor, and the Editor reaches into it to colour a Material button rather than to use a house one.
- **Required** [§5](ZINELY-DESIGN-SYSTEM.md)'s objects, drawn once
- **Authority** [§1.3](ZINELY-DESIGN-SYSTEM.md) — *"perceived quality equals the finish of the least finished surface… raising the floor beats raising the ceiling, every time"*
- **Milestone** C6 · **Prereq** CI-44 … CI-53 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** **This is the item the programme exists for**, and the largest single visual change in it. One surface per PR.
- **Verify** Every Editor control is a `:core:ui` object or records why not; CI-25's goldens; both device passes; the accessibility gate with **the platform tree read on the Reframe and Type surfaces specifically**.

#### CI-64 · 167 `.dp` literals in Editor surfaces
- **Location** `ReframeControls.kt` (56), `TypeBar.kt` (33), `EditorPageStrip.kt` (18), `EditorScreen.kt` (11), `EditorSupplyTray.kt` (8), `EditorSaveFailure.kt` (8), `EditorEmptyState.kt` (6), `EditorMoveResizeHint.kt` (6), `ResizeHandles.kt` (5), `EditorContextBar.kt` (4), `EditorSavedConfirmation.kt` (4), `ReframeOverlay.kt` (2), `SelectionChrome.kt` (1), `ElementSemanticsLayer.kt` (1), `SnapGuides.kt` (1), `EditTextSession.kt` (1) = **165** — plus `ZinelyNavHost.kt` (2), counted here because they are the same migration
- **Required** [§2.2](ZINELY-DESIGN-SYSTEM.md), [§13](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C6 · **Prereq** CI-39, CI-27 · **Kind** visual · **Changes** render, tests
- **Risk** **Every literal touched is a pixel moved on a goldened surface.** `ReframeControls.kt`'s 56 are the densest concentration in the app and sit on the surface with the most recent shipped accessibility defect.
- **Verify** The packages join the CI-27 enrolment list in the same commit that migrates them; goldens re-recorded in a **separate** commit.

#### CI-65 · Twelve `.sp` literals in Editor surfaces, against roles that must replace them
- **Location** `ReframeControls.kt` (6), `TypeBar.kt` (6); the remaining Editor type comes through `MaterialTheme.typography` (CI-60) and `LocalTextStyle` (`EditTextSession.kt:8`, `TypeBar.kt:25`)
- **Required** [§6](ZINELY-DESIGN-SYSTEM.md) roles; [§13](ZINELY-DESIGN-SYSTEM.md) *"No sizes invented for this screen."*
- **Milestone** C6 · **Prereq** CI-41, CI-60 · **Kind** visual · **Changes** render, a11y, tests
- **Risk** Reflow. `TypeBar` is a horizontally-scrolling control strip where a size change moves every subsequent hit target — which is what `TypeBarTest.kt:501`'s recorded regression was about.
- **Verify** Zero `.sp` outside `Type.kt` in enrolled packages; CI-33's large-text goldens.

#### CI-66 · The Editor's three Notices adopt the one Notice
- **Location** `EditorSaveFailure.kt` · `EditorMoveResizeHint.kt` (Proof's third, `ProofScreen.kt:416`, is CI-72)
- **Required** as CI-46
- **Milestone** C6 · **Prereq** **CI-46**, CI-15, CI-16 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Both carry live regions and dismissal semantics; `EditorSaveFailure` is the honest correction to ADR-034 and its copy is keyed by failure kind. **Blocked on CI-15/CI-16** — restyling these before their ADRs rule is the silent supersession §15 forbids.
- **Verify** One Notice, three call sites; every existing a11y assertion on both files still green.

#### CI-67 · Seven `remember` accumulators in `EditorScreen` are deliberately outside the reducer
- **Location** `EditorScreen.kt` (1,006 lines) — live transform, resize override and reframe-draft accumulators; `FramingDraft.kt` (207 lines)
- **Current** Held out of `EditorReducer` on purpose, for per-frame gesture state.
- **Required** Nothing in the design corpus — **this item exists because C6 endangers them**, and an inventory that omits the thing most likely to break is not an inventory.
- **Authority** [ARCHITECTURE](ARCHITECTURE.md); [ADR-029](DECISIONS.md#adr-029); [roadmap §4 C6](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C6 · **Prereq** CI-25 · **Kind** architectural · **Changes** pub, tests
- **Risk** **A restructure that disturbs them breaks live transforms, resize overrides or reframe drafts in ways a screenshot will not show.** This is the one C6 risk goldens cannot cover.
- **Verify** The `:feature:editor` gesture tests green; **device only** for the continuous paths — drag, pinch, rotate, reframe — before and after each surface PR.

#### CI-68 · The page must not resize when chrome or the IME appears — unverified
- **Location** `EditorScreen.kt:11` (`imePadding` import), `:849` (applied around the text panel); the type bar, reframe controls and supply tray all mount from `EditorScreen`
- **Current** `imePadding()` is applied to the text panel. **Whether the *page* holds its size through that has never been verified.**
- **Required** [§8.2](ZINELY-DESIGN-SYSTEM.md), [§4.7](ZINELY-DESIGN-SYSTEM.md) — the page is the fixed point
- **Milestone** C6 · **Prereq** CI-62, CI-63 · **Kind** behavioural · **Changes** pub, render
- **Risk** [§8.2](ZINELY-DESIGN-SYSTEM.md) calls this *"the single most damaging violation available in this product."* **It is the single highest-value check in the milestone**, and it is a device check — a golden captures one keyboard state, not the transition.
- **Verify** **Device only.** Measure the page's drawn size with the type bar, reframe controls, tray and IME each shown and hidden; record the measurements.

#### CI-69 · The legacy Material3 scheme survives its own retirement condition
- **Location** `Theme.kt:78-85` (eight legacy colours), `:87-101` (`LegacyLightScheme`), `:103-117` (`LegacyDarkScheme`), provided at `:148-152`
- **Current** Its own comment: *"Each screen drops its MaterialTheme reads as it migrates onto the token layer (M2–M5); when the last one has, this scheme goes away with it."* It has not. Documented role abuse remains: `background` is a slate grey absent from the frozen palette; `surface` is paper.
- **Required** [§2.6](ZINELY-DESIGN-SYSTEM.md) two themes as equals; one colour source
- **Milestone** C6 — **the closing act** · **Prereq** CI-60, CI-61, CI-70, CI-71 · **Kind** mechanical · **Changes** render, tests
- **Risk** **Low after CI-60/61, catastrophic before**: deleting it while 16 reads remain is a mass visual regression. This item's whole value is that it is the proof the migration finished.
- **Verify** `LegacyLightScheme`/`LegacyDarkScheme` deleted; the app compiles; goldens byte-identical (nothing should have been reading them).

#### CI-70 · The app's root container is a Material3 `Scaffold`
- **Location** `app/.../MainActivity.kt:9` (import), `:35` — `Scaffold(modifier = Modifier.fillMaxSize())` wrapping the whole `NavHost`
- **Current** `Scaffold` draws `MaterialTheme.colorScheme.background`, which `Theme.kt:94` sets to `LegacyDesk = #3A3A3C` in light and `#1F1F21` in dark — **the exact stale colour CI-02 is correcting in the documentation**, painted behind the entire application.
- **Required** [§12.5](ZINELY-DESIGN-SYSTEM.md); [§7.1](ZINELY-DESIGN-SYSTEM.md) — the room is desk, and desk is `#E7DECE` / `#201F1E`
- **Milestone** C6 · **Prereq** CI-69 · **Kind** visual · **Changes** render, tests
- **Risk** Low-medium. Screens fill it, so it shows mainly at edges, during the boot window and under system-bar insets in edge-to-edge — the moments a user reads as "the app's colour."
- **Verify** No `Scaffold` in production, or it is given an explicit house container colour; a boot-window screenshot in **both** themes.

#### CI-71 · The Activity's platform theme is hardcoded light, and pre-dates Material3
- **Location** `app/src/main/res/values/themes.xml` — `<style name="Theme.Zinely" parent="android:Theme.Material.Light.NoActionBar" />`; applied at `AndroidManifest.xml:20` and `:25`
- **Current** The window theme is the **platform** Material (2014) *Light* theme. There is no `values-night/` variant. Compose flips with `isSystemInDarkTheme()`; the window behind it does not.
- **Required** [§2.6](ZINELY-DESIGN-SYSTEM.md) — *"two rooms, not one room inverted"*, both themes as equals; [§12.5](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C6 · **Prereq** CI-70 · **Kind** visual · **Changes** render
- **Risk** Low to fix; it produces a light flash on cold start in dark mode — the first frame of the product, in the wrong theme, before any Compose code runs. **Never split from CI-70**: fixing one and not the other leaves the same defect with a different cause.
- **Verify** A `values-night/` theme exists or the window background is theme-neutral; **device only** — a cold start in dark mode, recorded.

#### CI-96 · ✅ CLOSED 2026-08-14 — The blank-page invitation's sticker cluster is announced, contradicting the comment above it
- **Closed by** [ADR-102 §12.13](DECISIONS.md#adr-102-p3-p8). `SupplyCluster`'s `Row` carries `clearAndSetSemantics {}`; the three glyphs are struck from `SurfaceTraversalOrderTest`'s Editor sequence **in the same change**, and `EditorEmptyStateTest.the_supply_cluster_is_silent` was added and proven to fail without the fix. The KDoc that stated the false invariant now states what was false about it.
- **How it was actually found** Not by the pin, in the end — by `adb shell uiautomator dump` during a device pass on `RZCYA1VBQ2H`, which read the same three `TextView`s. Two independent readers reached the same node; the merged semantics tree, which is what most of this repo's a11y tests read, saw nothing either time. Worth keeping: the pin made the fix *provable*, the device made it *noticed*.
- **Location** `EditorEmptyState.kt:88-92` — the decorative `Row` of three `StickerBlob`s (`✿`, `❀`, `★`); the comment asserting the invariant is `:86-87`
- **Current** All three glyphs reach the **platform accessibility tree as three separate traversal stops**, on the first screen a new user meets with an empty page. Measured by `SurfaceTraversalOrderTest` (CI-31), which pins them as the current truth rather than filtering them out — a test that hid them would make this item unfalsifiable, which is how it survived.
- **Required** [DESIGN-RULES per-screen checklist](design/DESIGN-RULES.md) — *"decoration not announced"*; [§11.2](ZINELY-DESIGN-SYSTEM.md) — *"announce the artifact, not the chrome"*
- **Milestone** C6 · **Prereq** none · **Kind** mechanical · **Changes** a11y, tests
- **Risk** **The defect is not the deviation; it is that the code documents the opposite.** `:86-87` reads *"not announced to screen readers (purely ornamental)"* — an invariant stated where the code lives, per [CLAUDE.md](../CLAUDE.md#house-conventions), and **falsified by the platform tree**. A wrong comment is worse than no comment: every subsequent reader, human or agent, took it as settled and never checked. Filed apart from CI-31 because CI-31 closes when the *test* exists; this closes when the *cluster is silent*.
- **Verify** `clearAndSetSemantics {}` (or equivalent) on the cluster; the three glyphs disappear from CI-31's Editor sequence **in the same commit**, which is what makes the fix self-proving; goldens byte-identical (nothing drawn changes).

#### CI-97 · The Print recipe's "Change" affordance is an unroled control
- **Location** `ProofPrint.kt:300-309` (`ChangeButton`) — a `Box` with `.clickable()`, `ProofChangePaperTestTag`, and no `Role`
- **Current** The platform reports it as `android.view.View`, not `android.widget.Button`. It **is** a traversal stop — the platform flags it `isScreenReaderFocusable` because it is clickable — and its spoken label comes only from its child `BasicText`; there is no `contentDescription` and no role. A screen-reader user reaches it and is told "Change", not "Change, button".
- **Required** [§11.3](ZINELY-DESIGN-SYSTEM.md); [DESIGN-RULES R9](design/DESIGN-RULES.md); [ADR-059](DECISIONS.md#adr-059)
- **Milestone** C7 · **Prereq** none · **Kind** mechanical · **Changes** a11y, tests
- **Risk** The [ADR-059](DECISIONS.md#adr-059) Role→View family, met on a further surface — and the one place in the Print recipe where the user is invited to *change* something they were told to match. Found by CI-31 only incidentally: CI-31 asserts order, not role, so nothing in this programme was looking here.
- **Verify** `Role.Button` set and asserted on the **platform** tree per CI-26/CI-30 — a `clearAndSetSemantics`/leaf node, since [ZButtonPlatformA11yTest](../feature/editor/src/test/kotlin/com/aritr/zinely/feature/editor/a11y/ZButtonPlatformA11yTest.kt) records that a merged node's Role collapses to `android.view.View` on the platform tree; goldens byte-identical.

---

### C7 · Shelf and Proof residuals

#### CI-72 · Proof's Notice adopts the one Notice
- **Location** `ProofScreen.kt:416` (`ProofErrorPane`), `:80` (`ProofErrorPaneTestTag`), `:312` (call site)
- **Milestone** C7 · **Prereq** CI-46 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Low — `ProofScreenTest.kt:357` pins the test tag, which must survive.
- **Verify** The tag still resolves; the golden set re-recorded with each diff reviewed.

#### CI-73 · Four production sites draw the artifact with rounded corners
- **Location** `ProofSheet.kt:156` — `RoundedCornerShape(3.dp)`, **the imposed sheet itself** · `ProofSheet.kt:192-193` — `RoundedCornerShape(6.dp)` ×2 (shadow + clip), the sheet's inner face · `ShelfCover.kt:158` — `RoundedCornerShape(topStart = 3.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 3.dp)`, the booklet · `ShelfCard.kt:246` — `RoundedCornerShape(9.dp)`, the card plate
- **Current** [§5.1](ZINELY-DESIGN-SYSTEM.md): paper **"Never: … takes a corner radius."** [§2.7](ZINELY-DESIGN-SYSTEM.md): *"Paper, and anything representing paper: **Square**."*
- **RULED — [ADR-065](DECISIONS.md#adr-065), 2026-07-24:** A-5 rejected; the artifact **and every representation of it** are square. **All four sites are non-conformant and are squared.** No site is exempt for being a representation, and the sheet's inner face is not exempt for being inside the sheet. The "or zero" branch is gone.
- **Candidate fifth site — determination owed, not decided here.** `ProofSheet.kt:288-289` draws the *printer-reach* legend key as a 16×10dp `Box` with `RoundedCornerShape(2.dp)` on both its border and its fill. Its own comment says it stands in for *"the same translucent fill as the sheet dead-band"* — i.e. a key **depicting a region of the sheet**. Whether a legend key is chrome (a token in a legend, §2.7 row 2 or row 3) or a representation of paper (row 1, and therefore square) is a **design determination this item may not make**: ADR-065 forbids per-surface carve-outs, so it cannot be excluded silently, and it forbids inventing thresholds, so it cannot be excluded for being small. Raised because the enumeration above would otherwise read as exhaustive. **Resolve before CI-73 executes.**
- **Milestone** C7 · **Prereq** ~~**CI-09**~~ **satisfied** ([ADR-065](DECISIONS.md#adr-065)), CI-40 · **Kind** visual · **Changes** render, tests
- **Risk** **Four files change under one A-5 resolution and zero under the other** — which is why the ruling is CI-09's and not this item's. **Never split across the four sites:** rounding the sheet but not the cover is worse than either consistent answer, because the inconsistency *is* the defect A-5 names.
- **Verify** All four match the [ADR-065](DECISIONS.md#adr-065) ruling in one commit, **and the candidate fifth site's determination is recorded and honoured** — a pass on "all four" while an undetermined fifth stays rounded is the inconsistency A-5 named, wearing a green tick; the 28 shelf+proof goldens re-recorded with each diff reviewed individually.

#### CI-74 · 424 `.dp` literals in Shelf, Proof and the shared component layer
- **Location** Surfaces (**283**): `ShelfStates.kt` (72), `ProofSheet.kt` (42), `ProofFold.kt` (40), `ShelfSheets.kt` (27), `ShelfCard.kt` (26), `ProofPrint.kt` (26), `ShelfCover.kt` (22), `ProofScreen.kt` (18), `ProofRead.kt` (7), `HomeScreen.kt` (3). Component layer (**117**): `ZButton.kt` (53), `ZSheet.kt` (20), `ZSnackbar.kt` (13), `ZMenuItem.kt` (8), `ZPaperSurface.kt` (7), `ZStatusPane.kt` (6), `ZToast.kt` (6), `ZTextField.kt` (4). Theme (**24**): `ZinelyElevation.kt` (20), `ZinelyDimens.kt` (4 — the only four that are already tokens)
- **Arithmetic** 165 (CI-64) + 2 (nav host) + 283 + 117 + 24 = **591**, the repository total. Every `.dp` literal in production belongs to exactly one of these two items.
- **Required** [§2.2](ZINELY-DESIGN-SYSTEM.md), [§13](ZINELY-DESIGN-SYSTEM.md)
- **Milestone** C7 · **Prereq** CI-39, CI-27 · **Kind** visual · **Changes** render, tests
- **Risk** Low-medium — these surfaces are well goldened (12 shelf, 16 proof), which is exactly why the re-record must be reviewed diff by diff rather than accepted wholesale.
- **Verify** Packages enrolled in the same commit; **physical print validation** if any value inside `ProofSheet`/`ProofPrint` geometry moves (CI-90).

#### CI-75 · 68 `.sp` literals in Shelf, Proof and the component layer
- **Location** Surfaces (**45**): `ShelfStates.kt` (9), `ProofFold.kt` (9), `ProofPrint.kt` (8), `ProofSheet.kt` (8), `ShelfCard.kt` (4), `ShelfSheets.kt` (4), `ProofScreen.kt` (2), `ProofRead.kt` (1). Components (**20**): `ZButton.kt` (9), `ZStatusPane.kt` (3), `ZMenuItem.kt` (2), `ZSheet.kt` (2), `ZSnackbar.kt` (2), `ZToast.kt` (1), `ZTextField.kt` (1). `Type.kt` (**3**) — the only three that belong where they are.
- **Arithmetic** 12 (CI-65) + 68 = **80**, the repository total.
- **Required** [§6](ZINELY-DESIGN-SYSTEM.md) roles
- **Milestone** C7 · **Prereq** CI-41 · **Kind** visual · **Changes** render, a11y, tests
- **Risk** `ZStatusPane`'s 22sp/14.5sp and `ZTextField`'s 17sp are quoted from the frozen CSS in their KDoc — each is a role assignment, and the KDoc must move with it or the trace to the HTML is lost.
- **Verify** Zero `.sp` outside `Type.kt`; each role traceable to the re-frozen HTML.

#### CI-76 · Shelf's one Material3 import
- **Location** `ShelfCard.kt:49` — `import androidx.compose.material3.Text`
- **Current** The single M3 widget import outside the Editor, the component layer and the nav host.
- **Milestone** C7 · **Prereq** CI-34 · **Kind** mechanical · **Changes** render, tests
- **Risk** Trivial. Listed because [roadmap §10.2](V1-IMPLEMENTATION-ROADMAP.md)'s condition is repository-wide and one import fails it.
- **Verify** The import is gone; the shelf goldens byte-identical.

#### CI-77 · Execute the shelf-cover ruling — ✅ **DONE**
- **Status** ✅ **EXECUTED 2026-07-24 with the ruling** ([ADR-069](DECISIONS.md#adr-069)) rather than deferred to C7. The deletion is mechanical and its Verify criterion for the delete branch is met: `HomeModule` no longer exists, so it cannot construct the renderer; no PNG is written on edit; and because the thumbnail was **never drawn**, the shelf goldens are **byte-identical rather than re-recorded** — the Verify line below anticipated a re-record, which turned out to be unnecessary precisely because the field was unread.
- **Location** as CI-18, plus `HomeViewModel.kt` (341 lines)
- **Milestone** C7 · **Prereq** **CI-18** · **Kind** architectural · **Changes** pub, render, persist (cache only), tests
- **Risk** Touches `ShelfThumbnailProducer`/`AndroidThumbnailRaster` and therefore the render stack — **this is the one item in the programme that lands on the feature axis's F1/F4 as well as this one.** It is still not a schema change: the cache is `cacheDir/thumbnails/<id>.png`, *"derived like the Room index, never authoritative"* ([ADR-045](DECISIONS.md#adr-045) decision 3), so deletion loses nothing a user owns.
- **Verify** ~~If wired: the shelf draws the user's page 1, verified on device with a real zine. If deleted: `HomeModule` no longer constructs the renderer, no PNG is written on edit, and the shelf goldens are re-recorded once.~~ **Met on the delete branch, with one correction:** `HomeModule` no longer exists and no PNG is written on edit — but the goldens were **not** re-recorded and must not be. They are **byte-identical** (`unchanged 65`, verified with `--rerun-tasks --no-build-cache`), precisely because the thumbnail was never drawn. This line anticipated a re-record; a moved golden here would have been a **bug in the deletion**.

#### CI-78 · The loading treatment is used on one of the three surfaces that specify it
- **Location** `ZSweep.kt:28` (`Modifier.zinelySweep`) — one call site, `ShelfStates.kt:377`. Its own KDoc names three hosts: *"shelf skeleton covers, bench load page, proof load sheet."*
- **Current** Bench and Proof load through the nav host's `CircularProgressIndicator` (CI-48) instead.
- **Required** [§12.5](ZINELY-DESIGN-SYSTEM.md); the frozen spec, as quoted in the component's own KDoc
- **Milestone** C7 · **Prereq** CI-48 · **Kind** visual · **Changes** render, tests
- **Risk** Low. Note the reduced-motion behaviour is deliberate and must survive: the sweep *"does NOT remove the overlay — it freezes it in place at `opacity:.4`"*, so reduced-motion goldens keep the loading look.
- **Verify** Both remaining hosts use the sweep; a reduced-motion golden per host.

---

### C8 · Navigation continuity

#### CI-79 · The navigation graph has no transitions at all
- **Location** `ZinelyNavHost.kt:64-68` — `NavHost` takes no `enterTransition`, `exitTransition`, `popEnterTransition` or `popExitTransition`; all three `composable<T>{}` blocks (`:69`, `:76`, `:83`) take none
- **Current** Every navigation in the product is the framework default fade. The only animated act-change is `AnimatedContent` **inside** `ProofScreen` (`:321-336`).
- **Required** [§3.6](ZINELY-DESIGN-SYSTEM.md) — *"Continuity over replacement"*
- **Milestone** C8 · **Prereq** CI-14, CI-54, **C6 complete** · **Kind** behavioural · **Changes** pub, a11y, motion, render
- **Risk** **High**, and invisible in a screenshot — which is why it survived M6 entirely. Interacts with state restoration, predictive back, and `ProofScreen`'s `rememberSaveable` act ordinal (`ZinelyNavHost.kt:198`).
- **Verify** **Device only**, both passes; TalkBack unaffected by the transitions; reduced motion loses no information ([§8.6](ZINELY-DESIGN-SYSTEM.md)).

#### CI-80 · A card is replaced by the editor rather than becoming it
- **Location** `ShelfCard.kt` · `ZinelyNavHost.kt:71-74` (the navigate) · `EditorScreen.kt`
- **Required** [§5.4](ZINELY-DESIGN-SYSTEM.md) — the Card *"**becomes** the editor rather than being replaced by it"*
- **Milestone** C8 · **Prereq** **CI-79**, and a C0 ruling that does not yet exist (see below) · **Kind** behavioural · **Changes** pub, a11y, motion, render
- **Risk** **The mechanism is not decided by any accepted document.** NavHost transitions, a shared-element scope, or a single-composable morph are three different answers with three different costs, and a shared element animating the *card* into the *editor page* animates between two objects at different proportions — [§5.1](ZINELY-DESIGN-SYSTEM.md)'s *"holds its proportion truthfully"* constrains what is even legal. **This item's true prerequisite is a C0 decision nobody has scheduled**, which is why the roadmap files it as an open question rather than a milestone task.
- **Verify** The mechanism is named in an ADR before it is built; then device only, both passes.

---

### C9 · The copy layer — parallel from C1

#### CI-81 · There is no string layer
- **Location** `app/src/main/res/values/strings.xml` — **one entry** (`app_name`). **Zero** `stringResource` call sites across `app/src/main` and `feature/editor/src/main`. No `Strings.kt`.
- **Current** Copy is hardcoded literals inside composables. **170** prose-shaped literals, comment-filtered — `for f in $(find feature/editor/src/main -name '*.kt'); do grep -vE '^[[:space:]]*(\*|//|/\*)' "$f"; done | grep -ohE '"[^"]*[a-z] [a-z][^"]*"' | wc -l`. The roadmap publishes **274** from the same filter *without* the comment exclusion; 104 of those are KDoc prose, so 274 over-counts the population it names. 170 remains a **lower bound** in the other direction — the two-words-lowercase filter excludes single-word labels and glyph strings. The nearest thing to a copy object is two helpers, `homeDeletedMessage()` and `saveFailureText()`.
- **Required** [§13](ZINELY-DESIGN-SYSTEM.md) *"Copy is from VOICE; no placeholder, no system strings"*; [DESIGN-RULES R5](design/DESIGN-RULES.md)
- **Milestone** C9 · **Prereq** CI-25 … CI-33 (C1) **only** · **Kind** architectural · **Changes** pub, a11y, tests
- **Risk** **Low — and this is the programme's best parallel work.** A pure extraction with zero intended visual change, which makes it the ideal early exercise of C1's net. Today *"copy comes from VOICE" cannot be verified by any mechanical means*; it is hundreds of human comparisons per review.
- **Verify** Goldens byte-identical — **any diff is a bug in the extraction**; a test asserting no prose literal survives in enrolled packages.
- **Status** ✅ **Done** (C9 commit 1) — copy relocated to the new pure-Kotlin `:core:copy` `Copy` object per [ADR-060](DECISIONS.md#adr-060); the `homeDeletedMessage()`/`saveFailureText()` helpers now delegate to it. Guard `CopyNoProseLiteralTest` (pure-JVM, `:core:copy`) is green — the enrolled tree holds one allow-listed internal assertion and no prose literal. Goldens byte-identical (`:feature:editor:verifyRoborazziDebug` pass).

#### CI-82 · Five nav-host strings, including a placeholder in a share title
- **Location** `ZinelyNavHost.kt:178` (*"Share your zine"*), `:181` (*"No app on your phone can open that yet."*), `:203` — **`zineName = "Your zine"`**, `:265` (*"‹  Back to editing"*), `:338` (*"‹  Back to your shelf"*). Comment-filtered and de-duplicated; the roadmap's "12 distinct" counts comment prose, and `:264`/`:337` render `state.message`, a variable, not a literal.
- **Current** `:203` carries its own `ponytail` admitting it: the project title lives in Room metadata (ADR-042) and never threads through, so every shared zine is titled *"Your zine."*
- **Required** [§13](ZINELY-DESIGN-SYSTEM.md) — *"no placeholder"*, named directly
- **Milestone** C9 · **Prereq** CI-81 · **Kind** behavioural · **Changes** pub, tests
- **Risk** Low in code; this is the item a user sees in a share sheet, outside the app, with the product's name on it.
- **Verify** The real title threads through; no literal remains in the nav host.
- **Status** ◑ **Partially done** (C9 commit 1) — all five nav-host literals **relocated to `Copy.Nav`** character-identical (no literal remains in the nav host); the fallback stays `Copy.Nav.ZINE_NAME_FALLBACK = "Your zine"`. The **behavioural half** — threading the real Room/[ADR-042](DECISIONS.md#adr-042) project title through — is a **scheduled follow-up** (changes the rendered share title; not a pure relocation), deferred out of the byte-identical commit.

#### CI-83 · The only non-Compose modal in the app is a platform `Toast`
- **Location** `ZinelyNavHost.kt:26` (import), `:181` — `Toast.makeText(...).show()` on `ActivityNotFoundException`
- **Current** The app ships `ZToast` (`ZToast.kt:43`, used at `HomeScreen.kt:265`) and uses the platform one for the single failure path where no app can open an exported file.
- **Required** [§12.5](ZINELY-DESIGN-SYSTEM.md); [§2.4](ZINELY-DESIGN-SYSTEM.md)'s tier list
- **Milestone** C9 · **Prereq** CI-81 · **Kind** visual · **Changes** pub, a11y, render, tests
- **Risk** Low. A platform Toast is also unstyled, untestable in Compose, and announced differently by TalkBack.
- **Verify** No `android.widget.Toast` in production; the failure path exercised in a test.
- **Status** ◑ **Partially done** (C9 commit 1) — the Toast **message** relocated to `Copy.Nav.NO_APP_TO_OPEN`. The **mechanism swap** (`Toast`→`ZToast`) is a **scheduled follow-up post-C2**: it is not a string relocation, and `ZToast` lives under C2-owned `ui/components` — cleaner once `:core:ui` exists.

#### CI-84 · Punctuation is typed, not set
- **Location** every string site in CI-81 and CI-82 — e.g. `ZinelyNavHost.kt:265`, `:338` use `"‹  "` with two spaces
- **Required** [§2.1](ZINELY-DESIGN-SYSTEM.md) *"Set, don't type"* — typographic punctuation, one ellipsis character, non-breaking spaces before units
- **Milestone** C9 · **Prereq** CI-81 · **Kind** mechanical · **Changes** pub, tests
- **Risk** **One real trap:** [§2.1](ZINELY-DESIGN-SYSTEM.md)'s own exception — *"strings frozen by an accepted ADR change only when that ADR is superseded."* The sweep must skip ADR-frozen strings, and that exception is silent until violated. Pair every string change with its ADR check in the same commit.
- **Verify** A sweep record naming which strings were skipped and under which ADR.
- **Status** ⏭ **Deferred to a CI-capable pass** — the sweep changes rendered glyphs, so it is a **separate commit that re-records goldens**, but on this project goldens are recordable only on the pinned CI image ([ADR-058](DECISIONS.md#adr-058)); a local Windows re-record would commit platform-divergent baselines. Now that all copy lives in one `Copy` object ([ADR-060](DECISIONS.md#adr-060)), the sweep is a single-file review. Analysis + the ADR-frozen skip list are in the C9 hand-off; not executed in commit 1.

#### CI-85 · Fourteen accessibility action labels are hardcoded in the Editor
- **Location** `EditorA11y.kt:46,53,54,56-66` — *"Edit text", "Reframe photo", "Reset framing", "Move left/right/up/down", "Make larger", "Make smaller", "Rotate clockwise", "Rotate counterclockwise", "Bring forward", "Send backward", "Delete"*
- **Current** These are the **only path a screen-reader user has to every editing operation** — the discrete twins DESIGN-RULES R9 requires — and they live outside VOICE.
- **Required** [§13](ZINELY-DESIGN-SYSTEM.md); [DESIGN-RULES R5, R9](design/DESIGN-RULES.md); [§11](ZINELY-DESIGN-SYSTEM.md) #1 — *"the visible twin is designed, not merely present"*
- **Milestone** C9 · **Prereq** CI-81 · **Kind** behavioural · **Changes** pub, a11y, tests
- **Risk** Low mechanically, high in consequence: these strings are the accessible product. Any change must be re-verified on the platform tree (CI-26), not in merged semantics.
- **Verify** All 14 in the copy layer, traced to VOICE; `ReframeA11yTest`'s 9 tests green; the platform tree read on device.
- **Status** ✅ **Done** (C9 commit 1) — all 14 `EditorA11y` action labels + the `label()` returns relocated to `Copy.A11y`, **character-identical**; the visible context-bar twins (`EditorContextBar`) now share the same `Copy.A11y` constants, so the two paths can't drift. Merged-semantics tests green; **on-device platform-tree re-verification (CI-26) is still owed** before final acceptance, per the a11y merge gate.

#### CI-86 · The launcher label disagrees with every document's spelling of the product
- **Location** `app/src/main/res/values/strings.xml` — `<string name="app_name">zinely</string>`; used at `AndroidManifest.xml:17` and `:24`
- **Current** The one string resource in the repository is lowercase; [README](../README.md), the PRD and the whole corpus write **Zinely**.
- **Required** [VOICE](design/VOICE.md) owns every string, including this one.
- **Milestone** C9 · **Prereq** CI-81 · **Kind** documentation · **Changes** pub, docs
- **Risk** **This item asserts no verdict** — lowercase may be deliberate brand styling. What is recorded is that the product's most visible string has no owner's decision behind it, and the launcher is the first surface a user reads.
- **Verify** VOICE names the canonical form; `strings.xml` matches it.
- **Status** ✅ **Done** (C9 commit 1) — owner ruling: the launcher `app_name` is **`Zinely`** (matching README/PRD/corpus). One-line `strings.xml` edit; no golden impact (the launcher label is in no golden).

---

### C10 · Conformance audit and sign-off

#### CI-87 · No per-surface record exists for §13's mechanically-checkable boxes
- **Location** `docs/reviews/` (absent)
- **Required** [§13](ZINELY-DESIGN-SYSTEM.md)'s 36 boxes, of which **29 are mechanically checkable**, per surface
- **Milestone** C10 · **Prereq** all · **Kind** documentation · **Changes** docs
- **Verify** A committed record per surface.

#### CI-88 · No record exists for the boxes that cannot be mechanically checked
- **Location** `docs/reviews/` (absent)
- **Required** [§13](ZINELY-DESIGN-SYSTEM.md)'s **7 judgement boxes**, per surface — including *"the least-finished element on this screen is identified"* and *"someone who has never seen the screen has looked at it"*
- **Authority** [validation §3.11](ZINELY-DESIGN-SYSTEM-VALIDATION.md); [roadmap §10.4](V1-IMPLEMENTATION-ROADMAP.md)
- **Milestone** C10 · **Prereq** all · **Kind** documentation · **Changes** docs
- **Risk** **They are labelled as judgement, not deleted.** A checklist of only mechanical boxes passes screens that are lifeless, which is the worse failure. The record's *existence* is checkable; its content is not, and no condition may pretend otherwise.
- **Verify** A record per surface **with a named human and a date**. Never ticked.

#### CI-89 · No per-screen DESIGN-RULES record exists
- **Location** `docs/reviews/` (absent)
- **Required** [DESIGN-RULES](design/DESIGN-RULES.md) R1–R12 and the twelve-box per-screen checklist, per screen
- **Milestone** C10 · **Prereq** all · **Kind** documentation · **Changes** docs
- **Risk** DESIGN-RULES is the **merge gate**, not a review artifact — *"a rule is met or it isn't."* A screen shipped without it was shipped through a gate that was not run.
- **Verify** One committed record per screen.

#### CI-90 · No device-verification or physical-print record exists for the conformance work
- **Location** `docs/reviews/` (absent)
- **Required** [CLAUDE.md · Device Verification](../CLAUDE.md#device-verification-mandatory) — **both passes**, per surface, with Pass 2 by a reader who has not been told why the screen behaves as it does; plus physical print validation **if `ProofSheet`/`ProofPrint` geometry moved**
- **Milestone** C10 · **Prereq** CI-74, and every device-verified item (CI-55, CI-57, CI-63, CI-68, CI-71, CI-79, CI-80) · **Kind** documentation · **Changes** docs
- **Risk** [ADR-050](DECISIONS.md#adr-050) exists because the frozen HTML's imposed sheet disagreed with the validated engine in **6 of 8 cells**, and *"an independent from-scratch fold re-derivation proved the HTML illustration physically wrong."* A spacing token is the one kind of change that can silently break a physical object.
- **Verify** A dated record per surface naming device, OS build, APK version and TalkBack version. For print: ≥1 printer, 100 % scale, one cut, one fold, 1→8 order, rotation and scale verified. **Where the two passes disagreed, the disagreement and its resolution are written down.**

#### CI-91 · No adversarial review verdict against the whole corpus exists
- **Location** `docs/reviews/` (absent)
- **Required** [CLAUDE.md · Multi-agent workflow](../CLAUDE.md#multi-agent-workflow) — a Review Agent that did not produce the implementation, validating repository state and never summaries
- **Milestone** C10 · **Prereq** all · **Kind** documentation · **Changes** docs
- **Risk** Every finding in this inventory that was wrong before an independent reader checked it — three inflated counts and two rank errors in the roadmap alone — argues that this is the only defence that has worked on this project.
- **Verify** A committed **GO** with no open Required Fixes; the privacy and offline invariants re-checked (no network, no analytics, no account; fonts still bundled — see CI-42).

#### CI-92 · The release documentation does not describe a conformance programme
- **Location** `CHANGELOG.md` · `docs/ROADMAP.md` · `README.md:76`
- **Current** The README's index row for the roadmap says *"nine architecture-ordered milestones (C0–C9)"*; the document it describes has **eleven, C0–C10**. Introduced when a review restructured the graph.
- **Required** [CLAUDE.md · Documentation Rule](../CLAUDE.md#documentation-rule-mandatory) — docs ship with the code they describe
- **Milestone** C10 · **Prereq** CI-24 · **Kind** documentation · **Changes** docs
- **Risk** Trivial to fix; recorded because it is a live inconsistency **in the index that points at the document this inventory implements**, and an inventory that omits a defect it can see is not one.
- **Verify** The README row matches the roadmap; a change-log row per shipped milestone; `git status` clean.

---

## 2. Summary — inventory by milestone

| Milestone | Items | Count | Visual impact | The gate that closes it |
|---|---|---|---|---|
| **C0** Specification reconciliation | CI-01 … CI-24, **CI-98**, **CI-99** | **26** | none | Every §2 conflict has a dated ADR; `docs/design/v1/*.html` re-frozen later than those ADRs; a device motion baseline exists |
| **C1** Conformance guardrails | CI-25 … CI-33, **CI-93** | **10** | none (the criterion) | Goldens byte-identical **and** an injected defect makes each new net fail |
| **C2** `:core:ui` extraction | CI-34 | **1** | none | The diff contains renames and import lines only |
| **C3a** Tokens — ADR-gated | CI-35 … CI-38, **CI-94** | **5** | none (tokens added, call sites not migrated) | Every token traceable to a C0 ADR; goldens byte-identical |
| **C3b** Tokens — HTML-gated | CI-39 … CI-42 | **4** | none | Every token traceable to a line in the **re-frozen** HTML |
| **C4** Object layer | CI-44 … CI-53, **CI-95** | **11** | low, localised | `ZComponentGoldenTest` extended per object, light **and** dark; a11y gate on every new control |
| **C5** Motion and haptics | CI-54 … CI-59 | **6** | high, and almost invisible in a screenshot | **Device only** — recordings at the frozen beats; both passes |
| **C6** Editor conformance | CI-60 … CI-71, **CI-43**, **CI-96** | **14** | **the largest in the programme** | C1's Editor goldens light/dark, 323 tests green, both device passes, platform tree on Reframe and Type |
| **C7** Shelf and Proof residuals | CI-72 … CI-78, **CI-97** | **8** | medium | Goldens re-recorded with **each diff reviewed individually**; physical print if geometry moved |
| **C8** Navigation continuity | CI-79 … CI-80 | **2** | high, invisible in a still | Device only; TalkBack unaffected; reduced motion loses no information |
| **C9** Copy layer | CI-81 … CI-86 | **6** | **none intended** — any diff is a bug | Goldens byte-identical; a no-prose-literal test green in CI |
| **C10** Audit and sign-off | CI-87 … CI-92 | **6** | none | A committed Review Agent **GO** with no open Required Fixes |
| | | **99** | | |

**Where the weight is.** C0 holds 26 of 99 items — **a quarter of the programme and none of the code**.
C6 holds 14 and the largest share of the risk. C1 + C2 + C9 — **seventeen items** — have no dependency on
C0 at all and are the correct first engineering acts.

---

## 3. Summary — inventory by subsystem

| Subsystem | Items | Count |
|---|---|---|
| **Specification & authority** (ADRs, HTML, corpus documents) | CI-01 … CI-24, **CI-98**, **CI-99** | 26 |
| **Test & CI infrastructure** | CI-25 … CI-33, CI-91, CI-93 | 11 |
| **Module structure** | CI-34 | 1 |
| **Theme / token layer** (`ui/theme/`) | CI-35 … CI-43, CI-69, CI-94 | 11 |
| **Shared component layer** (`ui/components/`) | CI-44 … CI-53, CI-95 | 11 |
| **Motion & haptics** | CI-54 … CI-59 | 6 |
| **Editor surface** (`:feature:editor` editor family) | CI-60 … CI-68, CI-96 | 10 |
| **Application shell** (`:app` — Activity, window theme, nav host) | CI-70, CI-71, CI-79, CI-80, CI-82, CI-83 | 6 |
| **Shelf & Proof surfaces** | CI-72 … CI-78, CI-97 | 8 |
| **Copy** | CI-81, CI-84, CI-85, CI-86 | 4 |
| **Render / export pipeline** | CI-42, CI-77 | 2 |
| **Release documentation** | CI-87 … CI-90, CI-92 | 5 |
| | | **101*** |

<sub>\* Exceeds 99 by **two**: CI-42 is counted under typography **and** render, CI-77 under Shelf **and** render. Every item appears exactly once in [§2](#2-summary--inventory-by-milestone), which is the authoritative count. *(This footnote has now been wrong twice — four double-counts, then three, once with a total that did not match its own rows. Both errors had the same shape: a summary asserted from memory rather than counted from the rows above it. It is now counted.)*</sub>

**The reading that matters.** The **specification** subsystem is the largest, and it contains no code.
The **`:core:editor` MVI reducer, `:core:imposition`, `:core:render`, `:core:model`, `:core:data`,
`:core:data-storage`** subsystems contain **zero items** — 384 test methods and zero design surface,
invariant across the whole programme.

---

## 4. Summary — inventory by repository package

| Path | Items touching it | Count |
|---|---|---|
| `docs/` (DECISIONS, design/*, ROADMAP, README, reviews) | CI-01 … CI-24, CI-38, CI-87 … CI-92, **CI-98**, **CI-99** | 33 |
| `feature/editor/src/main/.../ui/theme/` | CI-35 … CI-41, CI-43, CI-69, CI-94 | 10 |
| `feature/editor/src/main/.../ui/components/` | CI-44 … CI-53, CI-74, CI-75, CI-78, CI-93, CI-94, CI-95 | 16 |
| `feature/editor/src/main/.../feature/editor/` — Editor family (14 files) | CI-49, CI-54, CI-57, CI-58, CI-60 … CI-68, CI-85, CI-96 | 15 |
| `feature/editor/src/main/.../feature/editor/` — Shelf family (7 files) | CI-54, CI-59, CI-73, CI-74, CI-75, CI-76, CI-77, CI-80 | 8 |
| `feature/editor/src/main/.../feature/editor/` — Proof family (5 files) | CI-19, CI-54, CI-59, CI-72, CI-73, CI-74, CI-75, CI-78, CI-97 | 9 |
| `feature/editor/src/test/` + `roborazzi/` | CI-25, CI-29 … CI-33, CI-52, CI-93 | 8 |
| `app/src/main/java/.../editor/ZinelyNavHost.kt` | CI-48, CI-53, CI-64, CI-79, CI-82, CI-83, CI-84 | 7 |
| `app/src/main/java/.../MainActivity.kt` | CI-70 | 1 |
| `app/src/main/java/.../home/` | CI-18, CI-77 | 2 |
| `app/src/main/res/` (`themes.xml`, `strings.xml`) | CI-71, CI-81, CI-86 | 3 |
| `app/src/test/` | CI-28 | 1 |
| `render-android/src/main/assets/fonts/` | CI-42 | 1 |
| `settings.gradle.kts`, `*/build.gradle.kts`, `gradle/libs.versions.toml` | CI-27, CI-34 | 2 |
| `.github/workflows/` | CI-26, CI-27, CI-28 | 3 |
| `docs/design/v1/*.html` | CI-20, CI-21, CI-22 | 3 |

**The concentration.** `ZinelyNavHost.kt` (394 lines) carries **seven** items — more than any other
single file, and it is in the module nobody thinks of as a UI module. `EditorScreen.kt` (1,006 lines)
carries seven (CI-25, CI-61, CI-62, CI-63, CI-64, CI-67, CI-68). `ProofSheet.kt` and `ShelfCover.kt` each carry the artifact-radius item, which is the one
that must never be split.

---

## 5. Summary — inventory by verification method

| Method | Items | Count |
|---|---|---|
| **Documentation review** (an ADR, a dated deferral, a committed record) | CI-01 … CI-13, CI-15 … CI-24, CI-38, CI-86 … CI-89, CI-91, CI-92, **CI-98**, **CI-99** | 32 |
| **Golden — byte-identical required** (a diff is a defect) | CI-25, CI-34, CI-35, CI-36, CI-37, CI-39 … CI-43, CI-52, CI-69, CI-76, CI-81, CI-94 | 15 |
| **Golden — re-recorded, each diff reviewed individually** | CI-44 … CI-51, CI-53, CI-60, CI-61, CI-64, CI-65, CI-66, CI-72 … CI-75, CI-78, CI-83, CI-95 | 21 |
| **Unit / instrumentation test** | CI-27, CI-28, CI-29, CI-30, CI-32, CI-56, CI-67, CI-82, CI-84 | 9 |
| **Accessibility — platform `AccessibilityNodeInfo` tree** | CI-26, CI-30, CI-31, CI-63, CI-85, CI-93, CI-96, CI-97 | 8 |
| **Device verification — both passes, no screenshot can see it** | CI-14, CI-54, CI-55, CI-57, CI-58, CI-59, CI-62, CI-63, CI-68, CI-70, CI-71, CI-79, CI-80, CI-90 | 14 |
| **Golden at a non-default configuration** (large text, smallest width, reduced motion) | CI-33, CI-41, CI-65, CI-78 | 4 |
| **Physical print validation** | CI-74, CI-77, CI-90 | 3 |
| **Injected-defect proof** (the net must be shown to catch something) | CI-26, CI-27, CI-31 | 3 |

<sub>Items appear under more than one method where more than one is required; [§2](#2-summary--inventory-by-milestone) remains the authoritative count.</sub>

**Three readings worth stating.**

1. **Thirty-two items — just under a third of the programme — are verified by reading a document, not by
   running anything.** That is not overhead. It is the measure of how much of this work is authority the
   repository does not yet record.
2. **Fourteen items can only be verified on a physical device**, and every one of them is invisible to
   the golden suite. Motion, haptics, continuity, the page-does-not-resize check and the dark-mode cold
   start all fail silently in CI.
3. **Two items are verified by proving a net catches an injected defect.** They are the only two whose
   verification is *not* "it passed" — because a passing suite proves nothing about a net, and the
   defect that shipped in `f4faaa4` passed everything.

---

## 6. Is every required repository change accounted for exactly once?

**No — and the honest answer has six parts.** Four categories escape by construction; **two are real
gaps in this document**, and both were found by an independent reader rather than by me.

### 6.1 Conditional items — the inventory's shape depends on rulings that do not exist

~~**CI-73 touches four files under one A-5 resolution and zero under the other.**~~ **RESOLVED
2026-07-24 — [ADR-065](DECISIONS.md#adr-065) rejected A-5, so CI-73 touches four files** (with a fifth
candidate to be determined, recorded in the item). The first conditional this section named is now
unconditional, and it resolved by ruling rather than by engineering, which is the whole argument of
§6.1. **The rest remain conditional:** CI-40's radius count,
~~CI-41's role count (eleven or sixteen)~~ **— resolved 2026-07-24, sixteen ([ADR-067](DECISIONS.md#adr-067))** — CI-51's existence, and every item gated on the eight additions
are the same: written against a decision C0 has not made. **They are counted once, but their *size* is
not yet knowable.** A ruling that rejects an addition deletes its items; a ruling that accepts A-7
creates items for screens that do not exist. This is the correct state — the alternative is an
inventory that pre-empts C0, which is the one thing the roadmap forbids it to do.

### 6.2 Screens that do not exist cannot have items

**Zero of the validation's twelve derived screens exist as routes** — Search, Import, Settings,
Duplicate, Export history, Template browser, Font picker, About, Backup, Recovery, first-run tutorial,
Storage. [Validation §6.1](ZINELY-DESIGN-SYSTEM-VALIDATION.md) predicts they will invent furniture; C4
exists so the first person to build one finds a Field and a Row instead. **No item can be written
against code nobody has written**, and conformance for these screens is a *gate on their creation*, not
an entry here.

### 6.3 Judgement cannot be inventoried, only recorded

CI-88 and CI-90 are the only honest treatment available for [§13](ZINELY-DESIGN-SYSTEM.md)'s seven
judgement boxes and for Pass 2 of device verification. **The record's existence is checkable; its
content is not.** Decomposing *"someone who has never seen the screen has looked at it"* into
inventory items would convert a judgement into a tick, which is precisely how a checklist starts
passing lifeless screens.

**Two of [roadmap §10.4](V1-IMPLEMENTATION-ROADMAP.md)'s five bullets have no item and are named here
rather than left implicit:** *"every visual change in C3–C7 traces to a line in the re-frozen HTML"* —
a human comparison, per change, that no query can perform — and *"every object drawn on any screen has
an entry in [§5](ZINELY-DESIGN-SYSTEM.md)"*, which requires judging what constitutes an object before
anything can be counted. They belong to CI-88's record.

### 6.4 The corpus's own gaps produce no items

[Validation §7.2](ZINELY-DESIGN-SYSTEM-VALIDATION.md) names **five defects and eight invention events
the eight additions do not close** — including the finding that [§1.3](ZINELY-DESIGN-SYSTEM.md)'s
lowest-finish rule **cannot fail a review** ([D-10](ZINELY-DESIGN-SYSTEM-VALIDATION.md)). An item
requires a rule to measure against. Where the corpus has no rule, this inventory is silent, and **a
conforming implementation of an incomplete system is still conforming.** That is a limit of the
definition, not of the inventory.

### 6.5 The category still escaping, and why

**The printed artifact.** This document has exactly two items on the render/export path — CI-42 (the
divergent font sets) and CI-77 (the thumbnail pipeline) — and both arrived through a *Compose*
concern. `:render-android` carries **18 goldens and 17 production files**, and it draws the object the
whole product exists to make. [§5.1](ZINELY-DESIGN-SYSTEM.md) governs paper, [§7.2](ZINELY-DESIGN-SYSTEM.md)
forbids our accent on the user's page, and [ADR-039](DECISIONS.md#adr-039) requires `export == preview`
— but **the design corpus was written from screens, and so was this inventory.** I walked
`feature/editor` and `app` file by file; I walked `render-android` only where a Compose item pointed
into it.

**Why it escapes:** the corpus supplies no way to fail the PDF. There is no §13 box for the artifact,
no DESIGN-RULES row for a printed page, and no device pass whose subject is the object in the user's
hands rather than the screen in front of them. CI-90 requires a physical print check **only if
geometry moves** — which means a conformance programme could complete without anyone comparing a
printed zine against the design system at all.

**What would close it:** a rule, at design rank, stating what the printed artifact must satisfy — and
then the items fall out of it. Recording it here rather than inventing that rule is the only move
available to a document whose first line refuses to design.

### 6.6 The second gap: text direction, script coverage and locale

**This inventory contains zero occurrences of RTL, `layoutDirection`, locale, or non-Latin script**
across ninety-four items — and [zinely-v1.md:121](zinely-v1.md) opens its ship-blocker list with
*"**Non-Latin fallback + input-time honesty → DoD 4 (worst standing violation)**"*, **blocker #1**.
I read that list to establish CI-18's rank from blocker #3 at `:123` — **two lines below** — and did not
carry blocker #1 into a single item.

It is a conformance concern, not only a feature one, and it touches items already written:
CI-42's two font sets are Inter and Fraunces, so a CJK, Indic or Arabic glyph has no face in either the
editor or the PDF; CI-41's type roles are defined against Latin metrics; CI-81's string extraction
is the natural moment `layoutDirection` and pluralisation would land, and it is filed as *"a pure
extraction with zero intended visual change."* In a mirrored layout it would not be.

**Why it escaped, stated plainly:** the design corpus is written in and about English, the roadmap it
derives from carries no internationalisation area among its twelve, and I inventoried against the
corpus rather than against the product. **Nothing in this document's method would have caught that** —
the method checks whether each rule has an item, never whether a rule is missing. That is the honest
limit of an inventory built from an accepted corpus, and it is why the ship-blocker list, which is
written from the *product*, outranks it.

**What would close it:** blocker #1 scheduled on the feature axis where it already lives, with a
conformance rule attached — after which CI-41, CI-42 and CI-81 each gain a clause rather than the
programme gaining a milestone.

---

*An inventory. It owns the enumeration of conformance work and nothing else. Order stays with
[V1-IMPLEMENTATION-ROADMAP.md](V1-IMPLEMENTATION-ROADMAP.md); scope with
[zinely-v1.md](zinely-v1.md)/[PRD](PRD.md)/[ROADMAP](ROADMAP.md); technical authority with
[ARCHITECTURE](ARCHITECTURE.md); design with the corpus. Every ruling this document names is named
because it belongs to somebody else. The Constitution wins every conflict.*
