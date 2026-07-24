# Design System Validation — an attempt to break it

> **Status:** validation report · 2026-07-22 · **not a source of truth, and not a design document.**
> It proposes no screens, no features, no philosophy. Its only prescriptive section is §7, and that
> section is deliberately the smallest set of additions that closes what §2–§6 found.
>
> **Method:** the accepted documents are taken as accepted exactly as written. The question is not
> whether they are good. It is whether they are **sufficient** — whether a stranger could design new
> screens from them without inventing.

---

## 1. How this was run

### 1.1 The stance

I joined the project two years from now. I never met the people who wrote these documents. I have no
access to their reasoning, their Slack, or their taste — only the files. I have been asked to design
twelve screens that have never existed, and my instruction is to derive them from the system rather
than invent them.

**Every derivation below was actually attempted before it was scored.** Where I record "invention," it
is because I tried and could not get there, not because I predicted I could not.

### 1.2 The corpus, stated in full — and a method failure disclosed

The brief named four accepted documents. The derivations below in fact lean on eight, and pretending
otherwise would hide where the answers actually came from:

| Document | What it supplied |
|---|---|
| [Constitution](zinely-constitution.md) | Every ethical derivation. The strongest source in the corpus |
| [ZINELY-DESIGN-SYSTEM](ZINELY-DESIGN-SYSTEM.md) | The rules under test |
| [V1-DESIGN-DIRECTIONS](V1-DESIGN-DIRECTIONS.md) | P1–P10 |
| [V1-DESIGN-REFINEMENT](V1-DESIGN-REFINEMENT.md) | The findings — **and the 140-item Premium Checklist**, see below |
| [DESIGN-RULES](design/DESIGN-RULES.md) | R1–R12, the merge gate |
| [VOICE](design/VOICE.md) | Every string, and several state rules nobody would look for there |
| [SCREEN-INVENTORY](design/SCREEN-INVENTORY.md) | Purpose and primary action for three of the twelve screens |
| [DESIGN-LANGUAGE](design/DESIGN-LANGUAGE.md) | Onboarding philosophy, motion profile, haptics |

> **The first draft of this report was returned NO-GO for a failure worth recording, because it is the
> exact failure the report exists to detect.** I audited the design system for gaps without opening
> the **140-item Premium Checklist** inside V1-DESIGN-REFINEMENT — a document I had listed as accepted
> on the first page. Nine checklist items bear directly on six of the defects I had filed. A stranger
> auditing this product would have made the same mistake for the same reason: *the checklist is filed
> inside a critique*, and nobody looks for load-bearing rules inside a document whose title says it is
> a list of findings about a past release.
>
> **That location problem is itself a finding** (D-27), and correcting the error changed the report's
> shape rather than its conclusion: two of the six defects got *worse* on inspection, because the
> checklist turns out to **require states the design system provides no means of drawing.**

### 1.3 The four buckets, and what separates them

| Bucket | Test | Consequence |
|---|---|---|
| **Obvious** | A named rule answers it, and I can cite the rule | The system worked |
| **Interpretation** | A rule answers it, but only after I decide what the rule means | Survivable. A reviewer can adjudicate by argument |
| **Guessing** | Several rules bear on it and point different ways, or one rule bears on it and is silent on the case | Dangerous. There is a defensible answer *and* a defensible opposite |
| **Invention** | No rule bears on it. I must make something up and it becomes precedent | **A design-system defect** |

The boundary that matters is between *guessing* and *invention*, and it is not the one people expect.
Invention is visible: I know I made it up, and so does a reviewer. **Guessing is the expensive one**,
because I do not know I guessed — I cite a rule, the reviewer reads the same rule, and we both feel
covered while quietly disagreeing about what it said.

**A third category emerged during the correction**, and it turns out to be the most common failure of
all: **obligation without means.** A rule states that something must exist; no rule states what it
looks like or where its colour, tier or timing comes from. It is worse than a gap, because a gap
prompts a question and an unmet obligation prompts a confident invention that cites a rule.

### 1.4 One correction to the brief's framing

Three of the twelve screens are **already specified** in
[SCREEN-INVENTORY.md](design/SCREEN-INVENTORY.md) — Settings, Template picker, and the photo Import
path — with *purpose · primary action · secondary actions · emotional goal.*

**That answers "what is this screen for" and nothing about how it is drawn**, which is the design
system's entire job. Having an inventory entry moved zero of the invention events below. It did
surface the sharpest contradiction in the audit (D-1), because the inventory's Settings entry states
something DESIGN-RULES forbids.

### 1.5 What I did not do

I did not design the screens. Producing twelve good screens would prove that *I* can design; the
question is whether the **system** can. Where derivation succeeded I recorded the citation, not the
layout. Where it failed I recorded the failure, not my workaround.

---

## 2. The twelve derivations

### 2.1 Search

**Obvious.** The subject is the user's work ([§4.1](ZINELY-DESIGN-SYSTEM.md)). Results are zines, so
results are Cards on a Shelf (§5.3, §5.4) and inherit everything: paper, one light source, booklet
proportion, the card-becomes-editor transition (§3.6). Copy from [VOICE](design/VOICE.md). Reading
order equals accessibility order (§4.5), reinforced by Premium Checklist #64.

**Interpretation.** Whether search *is* the shelf filtered in place or a separate surface. P2
("everywhere is one place") pushes hard toward filtering in place, and I would defend that reading.

**Guessing.** Whether matched text in a title is emphasised, and how. §2.1 says emphasis is size,
weight and space *before* colour; none work inside a running title, and highlighting is the one case
where colour genuinely is the only mechanism.

**Invention — 3 events.**
1. **The search field.** No Field object in §5 — no resting state, no focus treatment, no clear
   affordance, no caret.
2. **The no-results state.** §9.10 and Premium Checklist #102 both say emptiness invites and never
   reports. Applied literally to *"no results for 'dog'"* that produces an invitation to make a new
   zine — the wrong answer, offered to someone looking for one they already made. **Both documents
   conflate empty-because-new with empty-because-filtered.**
3. **Search-in-progress.** Fast but not instant over a few hundred documents; §3.8 has no band for it.

---

### 2.2 Import (a batch of photos)

**Obvious.** The picker is the OS picker — shipped, privacy-preserving, no broad storage permission
([ADR-031](DECISIONS.md#adr-031), SCREEN-INVENTORY). Photos land on the page, selected. §7.4 governs
the aftermath: the user's photos are the most saturated things on screen, and should be.

**Interpretation.** Whether importing twelve photos places twelve elements or asks a question.
Article 1 leans one way; §1.2's first test leans the other. Adjudicable by argument.

**Guessing.** Where a failed import is reported. VOICE has the string (*"That photo didn't want to
come in. Try another?"*); the system has no object to put it in (D-12). Toast, banner, inline, dialog
— four defensible answers, and §5.6 rules out only the dialog.

**Invention — 3 events.**
1. **Progress of a real operation.** Decoding and copying twelve full-resolution photos takes real
   time. §3.8's bands are Instant, Brief, and Deliberate, and Deliberate is explicitly ring-fenced:
   *"nothing in the Deliberate band exists outside the ending."* **There is no band for work that is
   simply underway** — while Premium Checklist #82 requires that unavoidable loading "shows the shape
   of what is coming" and #101 requires a designed loading state. The obligation exists; the timing
   vocabulary to satisfy it does not.
2. **The indicator itself.** No object, and no colour: §7.1's accent means *"the user's next action,"*
   and progress is not an action.
3. **The system picker.** §12.5 makes it an anti-pattern to accept a platform default without a
   decision — but the OS picker cannot be restyled and shipping it is required by the privacy model.
   **The rule cannot be obeyed and cannot be waived**, because the system has no concept of a surface
   we do not own.

---

### 2.3 Settings

**Obvious.** Content: paper size, theme, about/privacy, fold help (SCREEN-INVENTORY). Copy register
from VOICE. No account, no analytics toggles — there is nothing to opt out of.

**Interpretation.** None worth the name. This screen is where the system fails hardest and earliest.

**Guessing — the compounding kind.** §4.1 requires me to name the screen's subject and says it is the
user's work "unless there is a stated reason." Settings contains no work. Either I state a reason —
the system does not say who may, or what qualifies — or the screen is mis-composed by rule.
Everything downstream inherits that guess.

**Invention — 5 events.**
1. **A Row.** Settings is nothing but rows: label, value, control, divider. §5 has no Row, so no rules
   for label/value alignment, for where a control sits, for what separates rows, or for press
   behaviour.
2. **A Switch.** No object, no on/off treatment.
3. **A Value.** "A4" beside "Paper size" matches none of §6's eleven roles. Metadata is defined
   narrowly as facts about *the artifact*; this is a fact about a setting.
4. **A list section header.** Subhead is "the missing register — third-most important," for grouping
   content and introducing a step. A list section header is a fourth thing.
5. **The primary action.** [R2/R3](design/DESIGN-RULES.md), §4.2 and Premium Checklist #58 all demand
   exactly one. SCREEN-INVENTORY says **"Primary action: none — it's a quiet utility surface."** Two
   accepted documents, opposite instructions (D-1).

---

### 2.4 Duplicate zine

**Obvious.** Almost all of it, and this is the system at its best. Duplicate is an action on a Card
(§5.4); it lives with Delete in the same menu; §9.2 requires the destructive neighbour be separated
and slowed, so duplicate is the *un*-ceremonied one. The new card arrives on a surface, so it settles
(§8.5). Continuity applies: it should be seen to come *from* the original (§3.6, and Premium Checklist
#80 — transitions preserve object identity).

**Interpretation.** Whether the copy is named by us or by the user immediately. Article 7 leans toward
asking; Article 6 leans toward not.

**Guessing.** Whether the new card is highlighted on arrival. Premium Checklist #106 requires selected
states be visible **on the object**; §3.3 defines selection only for objects being manipulated on the
page. The requirement exists; the appearance for a card on a shelf does not.

**Invention — 0 events.**

**This is the most useful data point in the report.** Duplicate derived cleanly because it is a new
action on **existing objects, in an existing composition, with an existing motion**. The system is
genuinely complete for that class — and it is a larger class than it sounds: anything about making,
arranging, previewing, printing or finishing a zine. Every failure below is at an object or a state
the system has never met.

---

### 2.5 Export history

**Obvious.** Subject is the user's work, so entries are their zines at booklet proportion. §5.11 and
Premium Checklist #123–124 apply to any thumbnail of an exported artifact: generated by the output
path, deterministic, never flattering.

**Interpretation.** Whether an export record is a Card or a Row. Both defensible; only one exists.

**Guessing.** Whether this screen may exist at all. §12.13's prohibition on engagement mechanics is
**Article 4 and constitutional**; only the *"state, not score"* phrasing is Feature-Tribunal, roadmap
rank. A list of past exports is arguably an accumulating record of activity — the shape Article 4
refuses. I would guess it is permissible because it is *the user's things* rather than *their streak*,
but I am guessing at the boundary of a constitutional article, which is the guess a new designer
should never make alone.

**Invention — 4 events.**
1. **A timestamp's type role.** "Yesterday, 4:12 PM" is not Metadata as defined, not Caption, not Body.
2. **A file size and a file name.** §6 has no technical register, and §10 pushes against showing them
   — but Article 2 requires the artifact be findable as a real file, so a real filename must
   eventually appear.
3. **A Row** (see 2.3).
4. **A date-grouped list header** (see 2.3).

---

### 2.6 Template browser

**Obvious.** Article 7's bright line is quoted and unambiguous: *"a starter finished unmodified is a
known failure of this article — starters must be designed so that overwriting them is the path of
least resistance."* SCREEN-INVENTORY adds that "start blank" is always present and equal. Premium
Checklist #122 — *"no illustration ever stands where the user's own work could stand"* — settles what
a template preview may not be.

**Interpretation.** How a preview conveys "this is yours to overwrite." Article 7 states the
requirement and leaves the mechanism open — correctly. That is a design problem, not a system gap.

**Guessing — structural.** §7.4 makes chrome quieter than "any content the user brings." A template
browser's content is **not brought by the user** — it is ours, at full strength, filling the screen.
§1.5 splits the world into *tool* (precise, quiet, ours) and *artifact* (personal, theirs). **A
template is ours and it is artifact-register.** It is in neither column, and the two rules bearing on
it give opposite instructions about how loud it may be.

**Invention — 2 events.**
1. **The tier and radius of a template card** (audit decisions 2–3). It represents paper, so §2.7 says
   square-cornered; it is a chrome affordance, so §2.7 says one shared radius. Both readings cite the
   same rule.
2. **The chosen-but-not-yet-placed state.** Premium Checklist #106 requires selected states be visible
   on the object; §3.3 defines the appearance only for on-page manipulation.

---

### 2.7 Font picker

**Obvious.** Fonts are supplies — Article 7, Feature Tribunal at roadmap rank (*"small bundled
supplies + fonts, license-clean — **KEEP, small**"*). Article 1 governs the list's length. Any
character that cannot render is announced, never silently dropped (Articles 2 and 5, Premium Checklist
#104). Premium Checklist #24 is directly on point and unusually valuable: *"the user's own content is
set in a face chosen for their content, not inherited from chrome."*

**Interpretation.** Whether the picker lives in the tray or over the page. §5.5 says the tray holds a
small, stable set of tools; a font list is neither.

**Guessing.** Whether a font's name is set **in that font** — the defining decision of the screen.
§1.5 says the tool's typography is "one scale, applied without exception," which forbids it; §1.1's
"the tool is precise so that the artifact can be personal" suggests showing the material honestly,
which requires it. Two rules from the same section, opposite answers.

**Invention — 2 events.**
1. **A scrolling gallery of samples.** No object; §4.4's "silence must be bounded" and §2.2's "density
   is per-surface" cannot govern a list whose length is data-dependent.
2. **The applied state vs. the previewing state** — two distinct states, no defined appearance between
   them.

---

### 2.8 About

**Obvious.** Register: display serif for identity (§6). Voice: quietly confident about privacy, never
preachy. §12.4 is directly on point and rules out most of what About screens normally contain.

**Interpretation.** Whether the privacy line appears here at all, given §10's "say it once, warmly,
where it lands" and [R12](design/DESIGN-RULES.md).

**Guessing.** Nothing significant — because there is nothing to hang a guess on.

**Invention — 3 events.**
1. **The subject.** §4.1 asks "the eye should land on ___, then ___, then ___." On About the answer is
   *the product*, which §12.3 and §1.2's second test both forbid. **A screen whose subject is
   legitimately the tool is unrepresentable in this system.**
2. **A version number.** [VOICE §4](design/VOICE.md) lists "version numbers in the UI" as do-not-ship,
   while Article 5's honesty duty and any bug-report workflow need one.
3. **A link, and its treatment.** No type role, no colour job, no pressed state.

---

### 2.9 Backup

**Obvious.** The constitution is at its strongest here. Article 3 makes durability and user-held
backup **constitutional duties** and settles the hard case explicitly: user-controlled destinations,
the share sheet, the user's own storage; *"a backup path that depends on infrastructure we run is
unconstitutional even as an opt-in."* This screen's *ethics* are fully derivable, which is worth
saying plainly.

**Interpretation.** Whether backup is a screen or an action on the shelf.

**Guessing.** What "done" feels like. §9.4 reserves celebration for genuine endings and Premium
Checklist #77 allows exactly one celebratory motion, at the ending — but a completed backup of two
years of work is a real reassurance moment. Silence (§4.4) or a mark (§9.7): both cite rules.

**Invention — 4 events.**
1. **Determinate progress**, again — and here the operation is long enough that hiding it is dishonest
   under Article 5 and Premium Checklist #104.
2. **Cancelling a running operation.** §3.1's interruptibility is about *animation*; Premium Checklist
   #70 and #90 are also about animation and gesture. **Nothing anywhere defines an operation that can
   be stopped**, though Article 5's "every failure offers an exit" implies one should exist.
3. **Partial success.** "47 of 52 zines backed up." Article 5 forbids silence about the five; VOICE has
   no string for it. *(This one belongs to VOICE, not to the design system — noted so it is not
   double-counted against §7.)*
4. **A destination chooser** — the share sheet is a platform surface (see 2.2, event 3).

---

### 2.10 Recovery

**Obvious.** Voice and stance: warm, specific, names the safety net when true, offers an exit and not
only a retry (Article 5, §9.8, VOICE). Never blames the user.

**Interpretation.** How much technical truth to show. Article 5 says honest before kind; §10 says plain
words, no codes.

**Guessing.** Whether a partially-recovered zine may be shown at all. §5.11 and Premium Checklist #123
say a preview is true; showing a half-restored zine as whole violates both. Showing it as damaged has
no defined appearance.

**Invention — 3 events.**
1. **The subject of a screen about the *absence* of the user's work.** §4.1 has no case for it. This is
   the one screen in the product where the composition's subject is a hole.
2. **A damaged or incomplete artifact's appearance.** §5.1 defines Paper as opaque, bright, whole.
   There is no "this one is wrong" state for the artifact itself.
3. **Destructive-but-necessary framing.** §9.2 assumes destruction is user-initiated and undoable.
   Recovery discards things because they are broken, which is neither.

---

### 2.11 First-run tutorial

**Obvious, and strongly so.** Article 6 forbids mandatory tutorials, tours before doing, and gating.
The Feature Tribunal permits *"first-run fold moment + seeded first project (skippable) — **KEEP** in
its skippable, non-gating form only"* at roadmap rank.
[DESIGN-LANGUAGE §4](design/DESIGN-LANGUAGE.md) supplies the philosophy — contextual, just-in-time.
VOICE supplies the hint strings verbatim **and their behaviour**: *"Never modal, never blocking,
always with an implicit/explicit 'got it.'"* P7 supplies the sequence. Premium Checklist #115 adds
that instructions expire and are not permanent furniture. **This is the best-served screen in the
report on everything except how it is drawn.**

**Interpretation.** Whether the fold moment counts as Deliberate-band motion. §3.8 reserves that band
for the ending; a first-run fold demo is an ending performed early. I would argue yes.

**Guessing.** Where a hint sits relative to the thing it explains, and whether it points.

**Invention — 2 events.**
1. **The hint / coach mark object.** It **ships today** and has canonical strings and behaviour rules
   in VOICE — but §5 defines no object, so its **elevation tier, its arrow, its spatial relationship to
   the element it describes, and its type role** are unspecified. *(An earlier draft claimed dismissal
   and blocking were also unspecified. VOICE specifies both; the claim was wrong and is narrowed.)*
2. **Skip.** "Skippable at every moment" is a constitutional requirement with no visual specification:
   skip must be low-emphasis *and* findable, and §4.2's "secondary actions are visibly quieter" is the
   only rule — which optimises against findability.

---

### 2.12 Storage management

**Obvious.** VOICE has the out-of-storage string ([ADR-036](DECISIONS.md#adr-036) lineage). Article 3
means nothing is deleted on the user's behalf. Premium Checklist #118 governs number formatting.

**Interpretation.** Whether this belongs in Settings or stands alone.

**Guessing.** Whether size figures appear per-zine on the shelf. §5.4's Card says name and quiet
metadata; whether a byte count is "metadata about the artifact" is defensible both ways.

**Invention — 4 events.**
1. **Every quantity on the screen.** Sizes, totals, a used/free proportion — no type role, no object.
2. **A data-visualisation of any kind.** §7.1 has no colour for a quantity, and §7.2 forbids
   meaning-by-colour without a legend, which is most of what a usage bar is.
3. **Bulk selection.** §3.3 defines selection as a state of one object you are holding; multi-select
   is a different concept with no rules anywhere.
4. **Progress**, for the third time.

---

### 2.13 The tally

| Screen | Obvious | Interpretation | Guessing | **Invention** |
|---|---|---|---|---|
| Duplicate zine | ●●●● | ● | ● | **0** |
| First-run tutorial | ●●●● | ● | ● | **2** |
| Template browser | ●●● | ● | ●● | **2** |
| Font picker | ●●● | ● | ●● | **2** |
| Search | ●●● | ● | ● | **3** |
| Import | ●●● | ● | ● | **3** |
| About | ●● | ● | — | **3** |
| Recovery | ●● | ● | ● | **3** |
| Backup | ●●●● | ● | ● | **4** |
| Export history | ●● | ● | ● | **4** |
| Storage management | ●● | ● | ● | **4** |
| Settings | ●● | — | ●● | **5** |

**35 invention events across twelve screens**
(3+3+5+0+4+2+2+3+4+3+2+4). They are not 35 distinct defects — the same holes recur:

| Recurring hole | Events | Screens |
|---|---|---|
| Progress / an operation underway | 5 | Search, Import ×2, Backup, Storage |
| A missing type role | 7 | Settings ×2, Export history ×3, About, Storage |
| Objects with no entry (Field, Switch, sample gallery, coach mark) | 4 | Search, Settings, Font, Tutorial |
| A state with no appearance (chosen, previewing, multi-selected) | 3 | Template, Font, Storage |
| A Row | 2 | Settings, Export history |
| A screen whose subject is not the artifact | 2 | About, Recovery |
| A platform surface we do not own | 2 | Import, Backup |
| One-offs — no-results · Settings' primary action · template card's tier · version number · cancel · partial success · damaged artifact · destructive-but-necessary · skip · data-viz | 10 | — |

**The distribution is the finding.** The system scores best on *ethics* and worst on *furniture*. It
can tell a stranger what Backup owes the user, and it cannot tell them what a list row looks like.
That is an unusual failure profile, and a benign one: the expensive half is done.

---

## 3. Rule stress test

Each defect: what it is, where it bites, and the smallest thing that is missing.

**Six defects carry a ⓟ mark.** For these, the **Premium Checklist states the obligation** and no
document supplies the means — the "obligation without means" class from §1.3. Two of them (D-22, D-27)
are worse than the gap they replaced, because an obligation that contradicts a rule is not an omission.

### 3.1 Contradictions

> **D-1 · Settings has no primary action, and every screen must have one.** *Severity: high.*
> [R2](design/DESIGN-RULES.md), [R3](design/DESIGN-RULES.md), §4.2 and Premium Checklist #58 are
> absolute; SCREEN-INVENTORY's Settings entry says *"Primary action: none."* Both accepted.
> Precedence rules *do* exist and each resolves a different axis — DESIGN-RULES settles
> **rule vs. feature** (*"the rule wins or the feature is re-scoped — escalate to an ADR"*),
> SCREEN-INVENTORY settles **itself vs. PRD/ROADMAP**, §0.2 settles **document vs. document**.
> **None settles rule vs. rule inside the design system**, which is this case. Missing: that order,
> and a stated class of screen exempt from R3.
>
> **🟡 PARTIALLY RESOLVED — [ADR-064](DECISIONS.md#adr-064) / [§1.7](ZINELY-DESIGN-SYSTEM.md),
> 2026-07-24. The remainder is ESCALATED to CI-11, by §1.7's own escalation clause.**
> §1.7 supplies the missing axis, and that much is closed: the collision is no longer *undecidable for
> want of an order*. But the order does **not** uniquely decide this one, and saying it did would be
> the error this section exists to prevent. Rank 4 (the specific over the general) appears to hand it to
> SCREEN-INVENTORY — except that doing so would let SCREEN-INVENTORY, which its own header calls *"not
> a parallel source of truth"*, defeat [R3](design/DESIGN-RULES.md), which
> [§0.2](ZINELY-DESIGN-SYSTEM.md)'s ADR-061 table records as keeping **everything** and giving up
> **nothing**. That is a document-vs-document re-ranking, which §1.7 explicitly refuses to perform.
> **Therefore it escalates**, and its remaining half already has an owner: **A-7 / CI-11**, which
> requires the exempt class be *"recorded as a narrow exemption from P1 rather than a gap closure."*
> Marking D-1 fully closed here would have granted Settings that exemption ahead of the CI-11 ruling.

> **D-2 · The accent on the user's page.** *Severity: **low** — narrowed under review.*
> §7.2 forbids our accent on the artifact's surface. An earlier draft claimed §3.2 and R1 *require* it
> for selection handles and guides; they do not — §3.2 says *"depth is the state indicator; a coloured
> outline alone is a diagram of selection,"* so handles drawn in ink are compliant, and the rule is
> better than the objection. The genuine remainder is narrow but real: **the text caret and a crop
> frame** must be visible against arbitrary user photos, where ink alone may not carry. Missing: a
> stated exemption for **transient tool overlays present only during a gesture**.
>
> **✅ RESOLVED — [ADR-064](DECISIONS.md#adr-064) / [§1.7](ZINELY-DESIGN-SYSTEM.md), 2026-07-24.**
> Resolved **by rank rather than by exemption**: accessibility (rank 2) outranks the artifact's truth
> (rank 3), so a caret or crop frame that must remain perceivable against an arbitrary user photo is
> permitted for the duration of the gesture. §1.7 names this case explicitly. This is the clause A-1
> declined to write, obtained by ordering instead — which is the reason the amended rank order was taken.

> **D-3 · Version numbers are forbidden and required.** *Severity: low, but exact.* VOICE §4 lists them
> as do-not-ship; Article 5's honesty duty and any bug report need one. Missing: a scope note (product
> chrome vs. an About surface).
>
> **✅ RESOLVED — [ADR-064](DECISIONS.md#adr-064) / [§1.7](ZINELY-DESIGN-SYSTEM.md), 2026-07-24.**
> Resolved at **rank 1, and resolved *inside* it.** VOICE is not a style preference to be outranked —
> [§0.2](ZINELY-DESIGN-SYSTEM.md) anchors it in **Sacred Thing 4**, so this is rank 1 against rank 1,
> which §1.7 would ordinarily escalate. It does not need to: Sacred Thing 4 *itself* reads **"honest
> before kind (Article 5)"**, so the constitution settles its own collision. A version number is
> permitted where honesty requires one (an About surface, a bug report) and forbidden as product chrome
> everywhere else. The scope note is now derivable rather than missing.

> **D-4 · The three motion jobs exclude motion the product already ships.** *Severity: medium.*
> §8.1: motion exists to show **causation**, preserve **continuity**, or mark **the ending**. The
> autosave-failure banner ([ADR-035](DECISIONS.md#adr-035)) appears because a *save* failed, not
> because the user acted. Missing: a fourth job — **to announce a change the user did not cause.**

### 3.2 Rules that give opposite answers to the same question

> **D-5 · "Anything representing paper is square-cornered" vs. every paper-ish chrome object.**
> *Severity: high — the most reviewer-visible defect in the report.*
> §2.7 assigns square corners to *"Paper, and anything representing paper"* and one shared radius to
> *"Chrome — trays, dialogs, cards, sheets."* **A shelf card, a page thumbnail and a template card are
> in both categories by the rule's own words** — §5.4 calls the card *"a small paper object"*, §5.10
> calls thumbnails *"small paper cards."* Five reviewers will not agree, and the disagreement is not
> about taste but about which half of one sentence applies. Missing: a rule distinguishing **the
> artifact from a representation of the artifact** — which also settles D-6.

> **D-6 · Paper may not rotate; page cards are hand-rotated.** *Severity: medium.* §8.3 forbids "the
> page rotates for effect"; §5.10, §4.3 and [R10](design/DESIGN-RULES.md) require the page strip be
> hand-placed and rotated. Same missing rule as D-5.
>
> **🟡 OPEN — intentionally deferred to CI-09 (A-5).** [ADR-064](DECISIONS.md#adr-064) does **not**
> close this, and an earlier draft of A-1 wrongly claimed it did. The question is whether a rotated page
> card is the **artifact** or a **representation** of it — a content gap, not a procedure gap. No rank
> order can settle it; the A-5 ruling can. Owner: **CI-09**.

> **D-7 · Continuity moves the work; the work may not move unless the user moves it.** *Severity: low.*
> §3.6 requires a card to *become* the editor page; §8.2 says the work never moves unless the user
> moves it. Navigating is not moving. Missing: one clause distinguishing **moving within a scene** from
> **carrying between scenes.**
>
> **✅ RESOLVED — [ADR-064](DECISIONS.md#adr-064) / [§1.7](ZINELY-DESIGN-SYSTEM.md), 2026-07-24.**
> Resolved at **rank 4, the specific over the general**: [§3.6](ZINELY-DESIGN-SYSTEM.md) governs one
> narrow case — *"when a thing exists on both sides of a **navigation**, it moves"* — while §8.2
> governs every surface at all times. The narrower rule wins inside its case and nowhere else, which
> *is* the missing clause: **moving within a scene** is §8.2's and stays forbidden; **carrying between
> scenes** is §3.6's and is not moving the work at all. Arrived at by rank, not by writing a new rule.

> **D-8 · Template loudness.** *Severity: medium.* §7.4 makes chrome quieter than "any content the user
> brings"; §1.5 has only two columns. Our artifact-register material is in neither. Missing: a third
> column — **material we supply** — displayed at artifact strength, chosen at tool precision.
>
> **✅ RESOLVED — [ADR-064](DECISIONS.md#adr-064) / [§1.7](ZINELY-DESIGN-SYSTEM.md), 2026-07-24.**
> Resolved at **rank 3, the artifact's truth**: material we supply is shown *as it will appear in the
> artifact*, so it is displayed at artifact strength and §7.4's chrome-quietness rule does not reach it.
> **The reasoning does not depend on the material having been placed** — a font name set in anything but
> its own face, or a template card muted to chrome strength, misrepresents what will print, which is
> what rank 3 forbids. That is why it also decides audit row 19, where nothing has been placed.
> **This does not presuppose [A-5](#a-5--the-artifact--representation-distinction--amendment):** rank 3
> answers *how loudly a sample is drawn*, which is a question about honesty; A-5 answers *whether a
> representation is the artifact*, which is a question about identity. D-6 needs the second; D-8 needs
> only the first.
> **Still owed (editorial, not a ruling):** §1.5's two columns are not yet three. The rank decides the
> loudness; writing **material we supply** into the split table is a docs edit that rides the next §1.5
> pass, and until it lands a reader of §1.5 alone still finds two columns.

### 3.3 Circular and unfalsifiable rules

> **D-9 · The quality benchmark is a drawing the system permits replacing.** *Severity: medium.*
> §1.3 and the §13 checklist define "finished" against "the product's own best drawing — currently the
> sheet diagram." V1-DESIGN-REFINEMENT protects that diagram, so the loop closes today. It does not
> close in two years: when the diagram is redrawn, the benchmark is defined by an artifact defined by
> the benchmark. Missing: the benchmark expressed as **properties** rather than as an exhibit.

> **D-10 · "The lowest-finish surface sets perceived quality" cannot fail a review.** *Severity: low.*
> True, useful as a priority heuristic (Premium Checklist #135), and as a checklist item —
> *"identify the least-finished element"* — neither satisfiable nor violable. Something is always
> lowest.

### 3.4 Undefined objects, and objects with no composition rules

> **D-11 · Two objects are named in the elevation rule and never defined.** *Severity: high.*
> §2.4's top tier is *"tray, sheet, dialog, popover."* §5 defines Tray and Dialog. **Sheet and popover
> have a tier and no rules** — no dismissal, no drag behaviour, no relationship to what is behind them,
> no size discipline. The Print & fold flow ships a sheet today.

> **D-12 · Four objects the product ships or immediately needs have no entry.** *Severity: high.*
> **Field** (Search, rename), **Row** (Settings, Export history), **Notice** (the shipped ADR-035
> banner and the shipped coach marks), **Menu** (the shipped card overflow). Directly responsible for
> **four** invention events (§2.1.1, §2.3.1, §2.5.3, §2.11.1) and implicated in several more — the
> Switch, the sample gallery, and both list section headers all fail for want of a container object.
> *(An earlier draft claimed 19 events; that number was not derivable from the enumeration and is
> withdrawn.)*

> **D-13 · No object owns the top of the screen.** *Severity: medium.* §5.9's Toolbar is "the tools
> belonging to the currently held thing." Screen title, back affordance and screen-level actions belong
> to none of the twelve objects. §4.6 only says nothing important goes in the top corners.

> **D-14 · Composition rules that name no owner.** *Severity: medium.* §4.4 requires silence be
> "bounded by a surface, an edge, or a deliberate proportion," but only Shelf and desk are surfaces and
> "surface" is never defined as something one can create. §4.7's "the page is the fixed point" is
> undefined on any screen with no page — which is half of the twelve.

> **D-15 · The tray's growth valve is named and never specified.** *Severity: medium.* §5.5: a tool
> joins the tray "only by displacing or **nesting** another." Nesting is the only sanctioned way the
> product may grow under Article 1, and **it has no design.**

### 3.5 States

> **D-16 ⓟ · Focus and pressed states are required and undrawable.** *Severity: high.*
> Premium Checklist **#107** — *"Focus states exist and are designed, not inherited"* — and **#108** —
> *"Pressed states are immediate"* — state the obligation plainly, and **#64** requires focus order to
> match visual order. **The design system defines neither state, and §7.1 provides no colour for
> either.** §5.8 gives Button a disabled state and nothing else. This is the "obligation without means"
> class in its purest form: a designer citing #107 has been told to design a focus state and given no
> vocabulary to draw one.
> *(An earlier draft added **"hover is the one genuine absence — nothing anywhere mentions it."** That
> is false. [ADR-049](DECISIONS.md#adr-049) decision 4 answers it with a reason and a revisit trigger:
> "**Hover states skipped.** The spec itself gates hover… every target device is touch… Revisit only if
> a pointer target (ChromeOS) ever ships." Pressed is answered in the same breath — the `:active`
> transforms are implemented. **Hover and pressed are decided, at ADR rank, in a document I did not
> open** — which is D-27 for the third time in this report, not an exception to it.)*

> **D-17 ⓟ · Selection is required on the object, and defined for one case.** *Severity: medium.*
> Premium Checklist **#106** — *"Selected states are visible on the object, not only in the chrome"* —
> is the requirement. §3.3 defines the appearance only for an object being manipulated on the page.
> Chosen-from-a-gallery (template, font, sticker), selected-in-a-list, and multi-selected are three
> further concepts with no appearance.

### 3.6 Motion and operations

> **D-18 ⓟ · There is no timing band for work that is underway.** *Severity: high — four screens.*
> Premium Checklist **#82** (*"Loading is avoided by design; where unavoidable, it shows the shape of
> what is coming"*) and **#101** (loading is a designed state) require it. §3.8's bands are Instant,
> Brief, Deliberate — and Deliberate is ring-fenced to the ending. **The obligation to design loading
> exists; the timing vocabulary to place it does not.** *(An earlier draft claimed §9.9 "assumes waits
> are absent." It does not — §9.9 says "where a wait is real, the interface shows the thing arriving."
> The narrower and correct claim is that no band accommodates it.)*

> **D-19 · Nothing in the system can be cancelled.** *Severity: medium.* §3.1's interruptibility, and
> Premium Checklist #70 and #90, are all about **animation and gesture**. An *operation* that can be
> stopped mid-flight has no rule, no object and no copy pattern — though Article 5's "every failure
> offers an exit" implies one should exist.

> **D-20 · No rule for content that changes underneath the user.** *Severity: low.* A list that
> reorders; a card that arrives while you are looking at the shelf. §8.2 forbids it — correctly for the
> editor, unhelpfully for a shelf that gains a card from a duplicate three rows up.

### 3.7 Typography gaps

> **D-21 · Five roles are missing, and they are what a utility screen is made of.** *Severity: high.*
> §6 has no register for **Value** ("A4" beside "Paper size"), **Input** (text being typed, and its
> placeholder), **Technical** (filename, size, date, version), **Link**, or **List section header**.
> Metadata is defined narrowly as facts about the artifact and does not stretch to cover them.
> Premium Checklist #11–#25 constrain the scale's *discipline* and add no roles.

### 3.8 Colour

> **D-22 ⓟ · Semantic colours are required to exist and have no job to hold them.** *Severity: high.*
> Premium Checklist **#30**: *"**Semantic colours (destructive, warning)** are never reused
> decoratively"* — a rule that presupposes they exist. **§7.1's job table has no entry for
> consequence**: its six jobs are artifact surface, the room, the artifact's ink, the user's next
> action, state-with-a-legend, and the hand. [LIB-4](V1-DESIGN-REFINEMENT.md) records that Delete *is*
> differentiated by colour in the shipped build, and the ADR-035 banner ships. So the checklist's
> presupposition is **unmet**, and the product satisfies it with a colour the system never named.
> *(An earlier draft called this a contradiction, on the grounds that §7.1's "there is no third accent
> role" forbids the colour outright. That reading is wrong and it contradicted this report's own A-3:
> §7.1's accent roles are "the user's next action" and "state that must be decoded," and a destructive
> colour claims **neither** — it marks a consequence, not a move. The prohibition survives; the job
> table is simply missing a row. **This is a gap, and a smaller one than claimed.**)*

> **D-23 ⓟ · Focused and selected have no colour job; disabled has a floor but no colour.**
> *Severity: medium.* Premium Checklist **#37** (*"Disabled is distinguishable from enabled and from
> absent"*) and **#105** state the obligations. §5.8 and §7.3 constrain disabled — *"unavailable and
> broken must not look the same"* — which is a contrast floor, not a colour. **§7.1's job table assigns
> none of the three a colour.** The states of D-16 and D-17, seen from §7.

> **D-24 · A quantity has no colour and no permitted encoding.** *Severity: medium.* §7.2 forbids
> meaning carried by colour without a legend in the same view, which is most of what a storage meter or
> a progress bar is.

### 3.9 Accessibility

> **D-25 · "Announce the artifact, not the chrome" has no meaning where there is no artifact.**
> *Severity: medium.* §11.2 is excellent for Editor, Read and Library, and inapplicable to Settings,
> About, Storage, Backup and Recovery — where the chrome **is** the content, and a rule telling you to
> de-emphasise announcing it is actively wrong.

> **D-26 ⓟ · Keyboard traversal is required in the checklist and absent from the accessibility rules.**
> *Severity: medium.* Premium Checklist **#64** — *"Reading order matches visual order matches focus
> order"* — and **#107** cover it. **§11's seven rules never mention focus traversal or a visible focus
> indicator**, and §4.5 speaks only of visual and screen-reader order. The requirement lives two
> documents away from the section that would be consulted for it.

### 3.10 The location problem

> **D-27 · Load-bearing rules are filed where a stranger will not look for them.** *Severity: high —
> this is the defect that produced this report's own first-draft failure, twice.*
> The 140-item Premium Checklist is the specification for focus, pressed, selected, disabled, loading,
> semantic colour and focus order. It lives inside
> [V1-DESIGN-REFINEMENT](V1-DESIGN-REFINEMENT.md), whose title, status line (*"design-rank proposal
> under review · not a source of truth"*) and first two hundred lines all describe findings about
> `0.9.0-beta.1`.
> The design system **does** point at that document — five times: §0.3's provenance diagram, §3.8's
> motion note, §13's final checklist box, §15's ownership table and §15's open item 4. *(An earlier
> draft claimed no pointer existed and that §15 omits it. Both were false, and the correction makes the
> finding sharper.)* **Every one of the five characterises it as findings about a past release. None
> names the Premium Checklist, and none describes it as a specification.** A pointer that mislabels
> what it points at is worse than no pointer, because it tells the reader they may skip it.
> The second instance is [ADR-049](DECISIONS.md#adr-049) decision 4, which settles hover and pressed —
> answers I filed as absences twice before finding them. **Missing: nothing conceptual.** A decision
> about where the checklist lives, and a pointer that says what it is.

### 3.11 Checklist items that cannot be objectively reviewed

Of §13's **36 boxes, 29 are objectively checkable** — a genuinely good ratio, better than most in-house
design checklists. The seven that are not:

| Item | Why it cannot be reviewed |
|---|---|
| "Optical alignment checked, not just mathematical" | No criterion, no method. Checkable only by someone who already agrees |
| "Space encodes relatedness truthfully" | Truthfully to whom |
| "Emptiness is composed and bounded, not left over" | Restates §2.2's soft rule; no test |
| "The least-finished element is identified — and it is not below the benchmark" | Circular (D-9, D-10) |
| "Someone who has never seen the screen has looked at it" | Process, not a property of the artifact. Unverifiable at review time |
| "Anything left deliberately unpolished is written down, with the reason" | Verifiable, but only against a *named* place. The box does name one — V1-DESIGN-REFINEMENT's *"Things I would leave exactly as they are"* — so this is checkable **for that document** and undefined for any new screen |
| "Every claim the screen makes is true at the moment it is made" | Correct and important, but needs runtime knowledge a design review does not have (the ADR-034 *"Saved ✨"* case) |

None should be deleted; several are the most valuable sentences in the section. They should be
**marked as judgement items**, so a reviewer knows they are being asked for an opinion.

---

## 4. Consistency audit — twenty decisions

Would five independent reviewers, with only these documents, reach the same answer?

| # | Decision | Verdict | Agree? | Missing rule |
|---|---|---|---|---|
| 1 | Does a new template card cast a shadow? | Derivable — §2.4, one light source; Premium #42, #47 | ✅ | — |
| 2 | Which elevation tier is a template card in? | Ambiguous — neither "on the page" nor obviously "above the room" | ❌ | Tier assignment for new objects |
| 3 | Is a template card square-cornered or radiused? | **Undecidable** — §2.7's two clauses both apply | ❌ | D-5 |
| 4 | Does a dialog move with the page? | Derivable — §2.4 tier 3, §8.2. **No** | ✅ | — |
| 5 | May a toolbar scroll? | Derivable — yes if it peeks (§3.5, Premium #93); never with an invisible edge (§5.9, Premium #94) | ✅ | — |
| 6 | Should paper rotate? | Derivable for the page (**no**, §8.3); contradicted for its thumbnail (**yes**, §5.10) | ❌ | D-5 / D-6 |
| 7 | May the accent appear on user content? | Derivable for handles (**no** — §3.2 prefers depth over outline); undecidable for the caret | ❌ | D-2, narrowly |
| 8 | May two primary actions exist? | Derivable — **no.** §4.2 and Premium #58; and every shipped multi-action screen ranks them (SCREEN-INVENTORY: Export = PDF primary, PNG secondary) | ✅ | — |
| 9 | Does Settings have a primary action? | **Contradiction** — R2/R3/§4.2/#58 vs. SCREEN-INVENTORY. §1.7 supplies the axis but does not decide it: rank 4 would re-rank two documents, so it **escalates** | 🟡 escalated to CI-11 | D-1 |
| 10 | What is the subject of About? | Undecidable — §4.1 admits only the user's work | ❌ | Tool-subject screen class |
| 11 | Is a search field chrome or content? | Undecidable — no Field object | ❌ | D-12 |
| 12 | What colour is Delete? | Undecidable — Premium #30 presupposes semantic colours; §7.1's job table has no row for consequence | ❌ | D-22 |
| 13 | What does keyboard focus look like? | Required (#107) and **undrawable** | ❌ | D-16 |
| 14 | What does hover look like? | Derivable — **skipped, with a revisit trigger** ([ADR-049](DECISIONS.md#adr-049) decision 4). *An earlier draft called this the one genuine absence; it is decided, in a document I did not open* | ✅ | — |
| 15 | What does a 30-second backup show? | Required (#82, #101) and **unplaceable** — no band | ❌ | D-18 |
| 16 | May a progress indicator use the accent? | Undecidable — accent means "your next action" | ❌ | D-22 / D-24 |
| 17 | Does "no results" use the empty-state invitation? | Ambiguous — §9.10 and Premium #102 read literally give the wrong answer | ❌ | Filtered-empty vs. new-empty |
| 18 | Does the page resize when a tablet side panel opens? | Ambiguous — §8.2's list omits the case; §4.7's remedy is impossible in two panes | ❌ | §5 longevity |
| 19 | Is a font name set in its own font? | ~~Undecidable — §1.1 and §1.5 disagree~~ **Decidable — yes.** §1.7 rank 3 (the artifact's truth): a sample is drawn as it will print, placed or not | ✅ [ADR-064](DECISIONS.md#adr-064) | D-8 |
| 20 | Which type role is "2.4 MB"? | **Invention** | ❌ | D-21 |

**Six of twenty would produce agreement** — rows 1, 4, 5, 8, 14 and **19**. *(Row 19 moved under
[ADR-064](DECISIONS.md#adr-064) — §1.7 rank 3 decides it. Row 9 moved from ❌ to **🟡 escalated**: §1.7
supplies the axis its "Contradiction" verdict was missing, but the order does not decide it and the
remainder is owed by CI-11. **Recount, 2026-07-24: 6 ✅ / 1 🟡 / 13 ❌.** This paragraph is recounted
whenever the table moves — the original said "five" and "fifteen", and leaving those standing after two
rows changed is precisely the failure §0.1 was written about.)*
*(Two further rows moved from ❌ to ✅ during the original review, and both moves were corrections of
mine rather than the system's: row 8 asserted that Export ships PDF and PNG as peers, when
SCREEN-INVENTORY ranks them — **"Primary action: Print at home (PDF)" / "Secondary: Save as image
(PNG)"**; row 14 called hover an absence when [ADR-049](DECISIONS.md#adr-049) decides it.)*

**Thirteen still would not, and one escalates.** The shape matters more than the count: **every agreement is about light, depth,
paper, ranking, or a decision recorded in an ADR** — and **every failure is about furniture, states, or
screens with no artifact in them.** The system has a well-built centre and no edges.

---

## 5. Longevity audit

Does each principle survive, or survive only by accumulating exceptions?

| Principle | V2 | V5 | Tablet / desktop | Foldable | Large library | A11y-first | Pro creators |
|---|---|---|---|---|---|---|---|
| The work is the hero (P1) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tool precise / artifact personal (§1.5) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| One light source (§2.4) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Three elevation tiers | ✅ | ⚠️ | ⚠️ | ⚠️ | ✅ | ✅ | ⚠️ |
| Radius family (§2.7) | ⚠️ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ |
| The page never resizes (§8.2) | ✅ | ⚠️ | ❌ | ❌ | ✅ | ✅ | ⚠️ |
| One accent, one meaning (§7.1) | ⚠️ | ⚠️ | ⚠️ | ✅ | ✅ | ❌ | ⚠️ |
| One primary per screen (§4.2) | ⚠️ | ⚠️ | ⚠️ | ✅ | ⚠️ | ✅ | ⚠️ |
| Density per surface (§2.2) | ✅ | ⚠️ | ⚠️ | ⚠️ | ❌ | ⚠️ | ⚠️ |
| Thumb zone (§4.6) | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ |
| Immediacy on touch-down (§3.1) | ✅ | ✅ | ⚠️ | ✅ | ✅ | ⚠️ | ✅ |
| Interruptibility (§3.1) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Honesty of preview (§5.11) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| The ending leaves an object (§9.4) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Ornament must do a job (§12) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Five ❌ cells, in four principles** — page-resize ×2, one-accent ×1, density ×1, thumb zone ×1.
Everything else survives or bends. *(The accent row was four ❌ in an earlier draft, on the mistaken
reading corrected in D-22. A consequence colour is not an accent, so the rule is not carrying the
exceptions I charged it with; the one surviving failure is the accessibility column, below.)*

**What survives untouched.** Everything derived from the constitution or from physics: the hero, the
split, the light, honesty, interruptibility, the ending, ornament-with-a-job. Not at risk in any
future examined. That is the system's real achievement and it should be said before the failures.

**What survives only by exception.**

- **"One accent, one meaning" survives everywhere except accessibility.** The rule constrains the
  *accent*, and the roles the product still needs — consequence, focus, progress — are none of them a
  next action, so adding them costs the rule nothing (D-22). Where it does fail is the
  accessibility-first column: high-contrast and colour-blind modes need *more* independently
  distinguishable roles, and a system whose colour discipline is built around having very few is
  structurally opposed to supplying them. That is the one column to watch, and it is watched by nobody
  today.
- **"The page never resizes" breaks on any multi-pane surface.** §4.7's remedy — the scene translates
  as one rigid body — is possible on a phone and impossible on a tablet where a panel takes permanent
  width. The rule's *intent* survives; its *letter* does not, and letters are what new designers
  follow. Needs restating as: **the page's size changes only when the space itself changes, never when
  chrome within the space changes.**
- **"Density per surface" has no scaling clause.** A shelf of three and a shelf of three hundred are
  the same surface and cannot be the same density. §5.3 never mentions quantity, and
  V1-DESIGN-REFINEMENT explicitly declares the cliff at thirty zines out of scope — correctly for a
  refinement pass, but **no accepted document owns it.**
- **Thumb zone and touch-down immediacy are input-modality rules stated as universals.** They do not
  survive a pointer, and the system has no way to say "this rule is about touch."
- **"One primary per screen" bends rather than breaks.** It has no ❌ — every shipped multi-action
  screen ranks its actions (audit row 8). Its only real failure is the utility-screen case (D-1).

**One thing gets *better* with time.** §1.6's rule that every new token needs an ADR is a growth brake
that becomes more valuable as the product ages — the mechanism by which a V5 Zinely could still look
like V1. Most design systems lack it. It is also why §7's additions must be small: each is a permanent
tax by the system's own accounting.

---

## 6. The three answers

### 6.1 If Zinely gained 50 new screens tomorrow, would they still look like one product?

**Partly — and the split is predictable rather than random.**

Screens made of existing objects, doing new things with the artifact, would be consistent. The
Duplicate derivation produced zero inventions, and that class is larger than it sounds: anything about
making, arranging, previewing, printing or finishing a zine.

Screens made of **furniture** would not. Fifty screens would need rows, fields, notices, menus, sheets,
progress, focus, hover, multi-select, quantities, filenames and section headers — and **every one
would be invented independently by whoever built it first**, then copied by everyone after. Worse than
a blank: for six of them a checklist item *demands* the state exist while no rule says how to draw it,
so each inventor would proceed confidently and differently, citing #107 or #101 as cover.

The precise prediction: **the fifty screens would agree about light, depth, paper, motion, voice and
ethics, and disagree about every list they contained.**

### 6.2 If every original designer disappeared, could a new designer continue without guessing?

**No — but the guessing is at the edges, not the middle.**

They could, without meeting anyone: derive the ethics of every screen; correctly refuse a feature;
correctly refuse an ornament; know what the ending must feel like; know why the page may not resize;
know that a preview may not flatter; and know what the product is *for*, at a depth most teams cannot
express about their own product.

They could not, without guessing: draw a settings row, colour a delete button, show a keyboard focus
ring, indicate that a backup is running, or say what shape a card's corners are. **Fifteen of the
twenty audit decisions would split a room.**

And there is a sharper answer available, from this report's own failure: **a new designer would not
find some of the rules at all.** I held the Premium Checklist in a list of accepted documents and
still audited without it, because it is filed inside a critique — and then filed hover as an absence
when an ADR had decided it. Twice, in one report, by the person best placed to know. **Sufficiency is
not only about whether a rule exists — it is about whether a stranger can find it on the day they need
it** (D-27), and that is the finding I would carry out of this exercise if I could keep only one.

### 6.3 What are the smallest possible additions needed?

Eight, specified in §7, plus one relocation that adds nothing. No new philosophy, no new screens, no
features. They close **22 of 27 defects** and **27 of 35 invention events**; §7.2 names every one of the
thirteen remaining items and why, because a report that claimed completeness would be repeating the
mistake it was written to catch.

---

## 7. The minimum additions

Each must close multiple defects, none introduces a new principle, and none touches the philosophy or
the constitution. Ordered by defects closed.

> **These are additions to [ZINELY-DESIGN-SYSTEM.md](ZINELY-DESIGN-SYSTEM.md) for the owner to accept
> or reject. This document does not make them.** Each is a system change and needs an ADR under
> §1.6. **Two are amendments rather than additions** — A-5 and the scale clause in A-8 change accepted
> text, and are flagged.

### A-1 · A precedence order for rules
*Closes D-1, D-3, D-7, D-8; narrows D-2.* **✅ ACCEPTED — [ADR-064](DECISIONS.md#adr-064), 2026-07-24
(CI-05, Option b).**
Three precedence rules exist and each covers a different axis: DESIGN-RULES settles rule vs. feature,
SCREEN-INVENTORY settles itself vs. PRD/ROADMAP, §0.2 settles document vs. document. **None settles
rule vs. rule inside the design system.** The addition is one ordered list — roughly constitution →
the artifact's truth → accessibility → the specific over the general → the object's rule over the
composition rule — plus the instruction that an unresolved collision is **recorded as a defect and
escalated**, never resolved locally by whoever hit it first. The shortest item here and the highest
value.

> **As ruled ([ADR-064](DECISIONS.md#adr-064)):** accepted with **ranks 2 and 3 transposed** —
> **accessibility outranks the artifact's truth**, on the evidence of [ADR-059](DECISIONS.md#adr-059)
> and §11.3. The escalation clause is mandatory and covers same-rank collisions. The order lands as
> [§1.7](ZINELY-DESIGN-SYSTEM.md).
>
> **Correction — this draft over-claimed D-6.** A-1 above and [A-5](#a-5--the-artifact--representation-distinction--amendment)
> below *both* claimed to close **D-6**, and they cannot both be its closer. **D-6 is not closed by
> A-1** and is not closed by ADR-064: it asks whether a rotated page card is the artifact or a
> representation of it, which is a **content** gap, not a **procedure** gap — no rank order can settle
> it. **D-6 is intentionally deferred to CI-09 (A-5)** and remains open. The duplicate claim is removed
> from this section's *Closes* line above.

### A-2 · Four objects: **Field · Row · Notice · Menu**
*Closes D-12; supplies the container the Switch, the sample gallery and both section headers need.*
Each in §5's existing three-part form. Three of the four **already ship** — the save-failure banner and
the coach marks are Notices, the card overflow is a Menu — so this is largely writing down what exists
before it is copied wrong. Adding **Sheet** and **popover** entries at the same time closes D-11, since
both are already named in §2.4's tier list.

### A-3 · A fourth colour job — **consequence** — and four control states
*Closes D-22, D-23, D-24, D-17, and the drawable half of D-16.*
One colour whose job is *this will remove, or this has broken*, explicitly **not** a third accent: it
never marks a next action, so §7.1's prohibition survives intact and Premium Checklist #30's
presupposition is finally satisfied. Plus a defined treatment for **disabled** (required by §5.8 and
#37 with no colour to draw it), **focused** (#107), **pressed** (#108) and **selected** — the last
stated for all three cases #106 implies: on the page, in a list, and in a gallery.

### A-4 · A fourth timing band — **Underway** — a fourth motion job, and cancellation
*Closes D-18, D-19, D-4, and the progress inventions in four screens.*
A band for real work with a real duration, discharging Premium Checklist #82 and #101, with the
Article 5 rule attached: **progress is truthful or absent — never decorative, never faked, and
cancellable where the operation can be stopped.** The Deliberate band's ring-fence is untouched. The
same addition supplies §8.1's missing fourth job — **to announce a change the user did not cause** —
because a system-initiated arrival and a system-initiated operation are the same family.

### A-5 · The artifact / representation distinction ⚠️ *amendment*
*Closes D-5, D-6, and audit rows 3 and 6 — the most reviewer-visible defect in the report.*
One clause: **the artifact is square-cornered; a representation of the artifact is chrome and takes the
chrome radius** — and a representation may be hand-placed, rotated and stacked in ways the artifact
itself may not.
⚠️ **This reverses accepted text, and an earlier draft wrongly called it "already de-facto practice."**
§2.7's *"anything representing paper is square-cornered"*, its §13 checklist echo, and
[V1-DESIGN-REFINEMENT](V1-DESIGN-REFINEMENT.md)'s RD-4 — which files *"a rounded cream rectangle on
black"* as a defect — all say the opposite. It needs an ADR that supersedes SYS-5, not a clarification.
The alternative resolution (make every representation square too) is equally valid and cheaper; **the
defect is the ambiguity, not the direction.**

### A-6 · Five type roles: **Value · Input · Technical · Link · Section header**
*Closes D-21 and six invention events across four screens.*
Roles only, in §6's purpose-not-size form. No new sizes: four of the five can be expressed with the
registers that already exist once their *purpose* is named.

### A-7 · A screen class whose subject is the tool ⚠️ *principle-adjacent*
*Closes D-1's other half, D-14, D-25, and the About and Recovery subject inventions.*
§4.1 admits only screens whose subject is the user's work. The addition names a second, small class —
utility surfaces — with three conditions: reachable only on purpose, may have no primary action, and
§11.2's "announce the artifact" replaced by its plain inverse.
⚠️ **This is a narrow exemption from P1**, not merely a gap closure, and should be recorded as one.
Naming the class is what stops it being claimed by screens that should be answering the user's
question instead.

### A-8 · Modality and scale clauses ⚠️ *the scale clause is an amendment*
*Closes D-26, D-20, hover in D-16, and four of §5's eight ❌ cells (page-resize ×2, thumb zone, density).*
Two short clauses, not a responsive design system:
- **Modality.** Rules that assume touch say so (thumb zone, touch-down immediacy, haptics), each
  naming its pointer and keyboard equivalent. **Keyboard focus joins §11's list** — Premium Checklist
  #64 already requires focus order, and §11 does not mention it, which is an accessibility gap today.
  Hover is defined here or explicitly declared out of scope.
- **Scale.** ⚠️ §8.2 **restated** as *the page's size changes only when the space itself changes, never
  when chrome within the space changes*, which preserves the intent on every surface. Plus one line in
  §5.3 on how the Shelf behaves as its contents grow by an order of magnitude.

### 7.1 The ninth addition, which is not a rule
*Closes D-27.*
The 140-item Premium Checklist is the specification for six of the states above and **cannot be found**
where a stranger will look. This needs no new content — only a decision about where it lives and a
pointer from the design system to it. It is listed apart from A-1…A-8 because it adds nothing; it
relocates. **It is also the highest-leverage item in this section**, since six defects above are
defects of placement rather than of thought.

### 7.2 What these eight do NOT close

Stated because a report claiming completeness would be repeating the failure it exists to catch.

| Open | Why it is not closed here |
|---|---|
| **D-2** — the caret and crop frame on user photos | A-1 only *narrows* it. The transient-tool-overlay exemption it needs is one clause, and I decline to write it for a case I inferred rather than observed on device |
| **D-9** — the benchmark defined by an exhibit | Needs a judgement about what "finished" means in properties. That is a design decision, not a missing rule |
| **D-10** — the lowest-finish rule cannot fail a review | An earlier draft claimed A-7 closed it; A-7 was rewritten and the claim was not. Nothing closes it, and §3.11's remedy — label it a judgement item — is the whole fix |
| **D-13** — no object owns the top of the screen | Might belong to an extended Toolbar rather than a new object. Pre-deciding would be inventing |
| **D-15** — the tray's "nesting" valve | The only sanctioned growth mechanism under Article 1, and designing it is a design task with real stakes |
| **Tier assignment for a new object** (1 event, §2.6) | A-5 settles the *radius* of a template card; which of §2.4's three tiers a brand-new object belongs to is audit row 2, and no addition supplies it |
| **Destructive-but-necessary framing** (1 event, §2.10) | §9.2 assumes destruction is user-initiated and undoable. Recovery's discards are neither, and the copy pattern is VOICE's as much as the drawing is ours |
| **A platform surface we do not own** (2 events) | §12.5 cannot be obeyed or waived on the OS picker and share sheet. A scope note would fix it; I decline to write the rule for a collision I only observed |
| **Filtered-empty vs. new-empty** (1 event) | One clause in §9.10 and Premium #102. Small, real, and a copy decision as much as a drawing one |
| **A damaged artifact's appearance** (1 event) | §5.1 defines Paper as whole. A "this one is wrong" state for the artifact is a genuine design problem |
| **Bulk / multi-select** (1 event) | A-3 gives the selected *appearance*; the *concept* of acting on many objects is unaddressed and arguably a feature question |
| **Partial success copy** (1 event) | Belongs to [VOICE](design/VOICE.md), not to the design system. Re-attributed, not left open |

**22 of 27 defects and 27 of 35 invention events.** All five remaining defects and all eight remaining
events are named above rather than absorbed into a number — an earlier draft said "the five are named"
while naming three, which is exactly the absorption this table exists to prevent.

### 7.3 What I deliberately did not propose

- **A component library.** Every addition is a rule or an object definition; §1.6's tax applies to all
  of them.
- **A responsive/adaptive layout system.** A-8 is two clauses. Multi-pane layout is a design problem
  for whoever ships a tablet, and pre-solving it here would be inventing for a screen nobody asked for.
- **Any resolution of the seven open collisions** already recorded in
  [ZINELY-DESIGN-SYSTEM §15](ZINELY-DESIGN-SYSTEM.md). They are decisions waiting for their owner, not
  defects of sufficiency.
- **Deleting the seven unreviewable checklist items** (§3.11). They should be **labelled** as judgement
  items, not removed. A checklist containing only mechanically-checkable boxes will pass screens that
  are lifeless, which is the worse failure.
- **Anything about the fifty hypothetical screens.** No screen in §2 was designed, and none should be
  built from this document.

---

## 8. The verdict, in one paragraph

**The design system is sufficient for the product it was written about, insufficient for the product
it will become, and — the finding I did not expect — partly sufficient in documents nobody will
find.** It specifies the hard, expensive, taste-dependent half completely: what the product is for,
what it refuses, how the work is treated, how light and depth and paper behave, what an ending owes the
user, when to say nothing. That half is what design systems normally lack. What it lacks is the cheap
half — rows, fields, notices, progress, focus, a colour for danger, a word for a number. And of the six
worst gaps, **the obligation is already written down and only the means are missing** — filed inside a
critique of a past release, where I failed to look while holding it on my own list of accepted
documents. Two further answers, hover and pressed, were sitting in an ADR; I filed them as absences
twice before I found them. Eight additions and one relocation close 22 of 27 defects; §7.2 names all five that remain,
and all eight unclosed invention events with them. That is a system with a placement problem and an
edge problem, not a thinking problem, and those are the better two to have.

---

*Validation only. No screens were designed, no features proposed, no philosophy rewritten. The eight
additions in §7 are proposals for the owner; each needs an ADR under
[§1.6](ZINELY-DESIGN-SYSTEM.md). This report was returned **NO-GO** on its first draft for auditing
without the Premium Checklist; that failure is recorded in §1.2 and became D-27 rather than being
quietly corrected.*
