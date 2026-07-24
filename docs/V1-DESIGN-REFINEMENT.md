# V1 Design Refinement — making the chosen direction feel inevitable

> **Status:** design-rank proposal under review · 2026-07-22 · **not a source of truth.**
> The direction is settled ([V1-DESIGN-DIRECTIONS.md](V1-DESIGN-DIRECTIONS.md), accepted): the **tool**
> is *Creative Workbench 2.0*, the **artifact** is *DIY Zine Workshop*. This document does not revisit
> that, does not restructure navigation, does not propose features, and contains no code, no HTML and
> no ADR edits. It is a craft pass: **the same product, executed to a standard.**
>
> Subordinate to [the constitution](zinely-constitution.md). A **companion** to
> [DESIGN-RULES.md](design/DESIGN-RULES.md) — that checklist remains the live merge gate and nothing
> here replaces a rule in it. Where a refinement here appears to conflict with an accepted
> [ADR](DECISIONS.md), it is flagged and stops; it is not built.

---

## How to read this

### What "refinement" is allowed to be

The information architecture is frozen. Library → Editor → Read → Print & fold → Completion is the
shipped order and the reason `0.9.0-beta.1` exists ([ADR-058](DECISIONS.md#adr-058)). Every finding
below changes **how something is drawn, timed, weighted, spaced, worded or felt** — never where it
lives, never whether it exists.

| Permitted here | Not permitted here |
|---|---|
| Spacing, alignment, optical correction, type scale, radii, elevation, colour weight | New screens, moved screens, renamed flows |
| Timing, easing, interruption, haptic placement, gesture quality | New features, new tools, new capabilities |
| Copy tightening within [VOICE](design/VOICE.md)'s existing register | A new voice, a new palette, a new identity |
| Making an existing state visible, legible, or truthful | Adding a state that did not exist |

Two consequences worth stating, because they are the failure modes of refinement documents. First,
**"add a setting" is never a refinement** — it is a feature wearing a craft costume, and Article 1
forbids the tray merely growing. Second, **an item is only in here if I can name the mechanism by which
it costs perceived quality.** Preference is not a finding. Where I could only produce a preference, I
left it out; the last section lists the places where I stopped deliberately.

### The four things in here that are *not* refinements

A draft of this document closed by claiming it added nothing. Independent review falsified that, and the
honest version is more useful: **four findings below require new behaviour, and each is marked ⚠️ at the
point it appears.** They are kept because they are the highest-value items in the document and because
naming them stops them being smuggled in — which is the phrasing
[ADR-058](DECISIONS.md#adr-058) Decision 7 itself uses about exactly one of them.

| # | The item | Why it is not a refinement | Its parent |
|---|---|---|---|
| 1 | **The user's real cover art** on the shelf card, the Front/Back previews, the fold diagrams and the completion screen (LIB Pass 3, PF-1, CO-1) | The shelf card today renders a cover derived from the *title*, not from the page; per-panel artwork is explicitly deferred by [ADR-058](DECISIONS.md#adr-058) Decision 7 as *"a separate change with its own parity question."* This is new behaviour, not new drawing. | Feature Tribunal, *"Real covers on the shelf — **KEEP**"*, at **roadmap rank**, applying Art. 5 and 7 |
| 2 | **A scrubbable fold** (PF-7) | New control, new rendering path. | Feature Tribunal, *"Fold-along ritual… **KEEP, minor**"*, at roadmap rank |
| 3 | **A characters-per-line readout** (ED-6) | Puts a number on screen that does not exist today. | 🟦 [R12.6](RESEARCH.md); serves P9 |
| 4 | **Moving the paper choice to print time** (LIB Pass 3) | Re-sequences the creation flow, which this document's own table forbids. | 🟦 [R12.1](RESEARCH.md); the critique's Top-20 #8 |

Everything else in this document changes only how an existing thing is drawn, timed, weighted or worded.
The four above go to a roadmap conversation with their parents cited; they are not built on this
document's authority.

### Evidence grades — carried forward, not reset

The evidence base is unchanged from the critique: eighteen device stills and an adb walkthrough of
`0.9.0-beta.1` on a Galaxy A17, dark and light ([V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md)).
That base **cannot see motion or haptics.** Every finding is graded, and the grade is load-bearing:

- 🟩 **OBSERVED** — visible in the captures or measured from them.
- 🟨 **ASSUMPTION** — argued from the product's nature or the research base; *not measured on device.*
- ⚠️ **COLLIDES** — contradicts an accepted decision or a frozen spec, **or is an addition rather than a
  refinement**. Adjudicate before building.

**Read the ⚠️ marks as *additional* to a blanket, not as a complete list.** Every 🟨 finding in every
Pass 2 departs from [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md)'s motion profile somewhere; the
per-finding ⚠️ marks flag the sharpest cases and do not certify the unmarked ones as clean. **None of
Pass 2 may be built before the motion baseline is recorded.**

**The whole of Pass 2 is 🟨 unless marked otherwise, and it says so at the top of each occurrence.**
[V1-DESIGN-ELEVATION §8](V1-DESIGN-ELEVATION.md) and §12 are ASSUMPTION for the same reason, and the
critique's own ranked remedy — *"record a motion-and-haptics baseline, then close the gap"* — is a
**precondition of this document's interaction findings, not a follow-up to them.** A refinement pass
that specifies timings for animations nobody has watched is exactly the over-claim two review rounds
already caught on this milestone. I would rather be honest and incomplete.

### The finding format

**Passes 1 and 2** carry the five fields the brief asks for, compressed. **Passes 3 and 4 deliberately
do not**, and the deviation is disclosed rather than claimed away: Pass 3 answers the brief's own three
questions per *moment* (what should they feel · what weakens it · the tiny detail), and Pass 4 is a
judge's list of what still reads as generic, unfinished, cheap, over-explained, noisy, Android-by-default
or emotionally flat. Those two passes cite no principle, because a felt reaction that has to borrow an
authority to be admissible is not evidence — it is a finding looking for a parent. The five-field form:

> **ID · Title** *grade*
> **Costs:** the mechanism by which perceived quality drops.
> **Principle:** the design principle it violates, cited so the derivation can be checked.
> **Refine:** the change.
> **Gain:** the emotional delta.

**Principle** cites [the ten V1 design principles](V1-DESIGN-DIRECTIONS.md#design-principles-for-zinely-v1)
as **P1–P10**, [DESIGN-RULES](design/DESIGN-RULES.md) as **R1–R12**, and constitution articles as
**Art. n**. A finding with no citable parent says so rather than borrowing rank — that habit is the one
this milestone has been corrected on twice.

---

## The refinement thesis, in one paragraph

Zinely's problem is no longer *what it does* and is not really *what it looks like* either. It is that
**the app is drawn at several different levels of finish, and the user's eye reads the lowest one.**
A hand-tuned sheet diagram with a legend sits four taps from a horizontally scrolling row of twelve
unlabelled white circles. A display serif that could carry a magazine sits above the platform emoji
font doing icon work. The register changes mid-product, and a register that changes reads as *unfinished*
rather than *varied* — this is the same mechanism by which one misaligned tile makes a whole bathroom
look badly laid. **The single highest-value refinement is not to raise the ceiling; it is to raise the
floor until nothing on screen is visibly less finished than the sheet diagram.** Everything below is
in service of that, and the sheet diagram is the internal benchmark: when a surface is done, it should
look like it came from the same hand that drew that legend.

---

# System-level refinements

These pay off on every surface, so they are stated once rather than five times — restating them per
screen is how a checklist becomes a contradiction ([Documentation Rule](../CLAUDE.md#documentation-rule-mandatory)).
They are Pass 1 and Pass 4 work almost entirely, because the system is where "generic Android" actually
lives: not in any one screen, but in the defaults every screen inherits.

> **SYS-1 · The product has one type scale with two steps in it.** 🟩
> **Costs:** Display serif, then a single grey sans body — nothing between. Screens with real content
> have no way to express *third-most-important*, so they express it with **bold** and colour instead of
> size and space. Emphasis-by-colour is the most reliable amateur tell in interface typography, because
> colour has no ordering: three orange words on a screen do not tell you which is first.
> **Principle:** **Design-rank, no constitutional or P-rank parent** — the observation in
> [V1-DESIGN-ELEVATION §7](V1-DESIGN-ELEVATION.md) plus settled book practice, which holds that the
> space between elements does as much work as the elements (✅ VERIFIED,
> [Bringhurst summarised](https://www.inkwell.ie/typography/bringhurst.html)). *(An earlier draft cited
> P7 here; P7 is demonstration-over-explanation-over-instruction and says nothing about type scale.)*
> **Refine:** A five-step scale with **one** intermediate register between display serif and body — a
> serif or small-caps subhead — and a rule that emphasis is carried by size, weight and space *before*
> colour. Colour becomes meaning-bearing only where a legend exists (the sheet already proves we can
> draw one).
> **Gain:** Screens stop oscillating between shout and mumble. Nothing else on this list changes as
> many pixels for as little risk.

> **SYS-2 · Two apostrophes ship in a product about printed matter.** 🟩 ⚠️ *(one string is frozen)*
> **Costs:** *"Let's make something cute"* uses `'`; *"It's folded — show me"* uses `’`. Mixed
> punctuation inside one voice is not a typo the user names — it is a texture they feel, and it is the
> cheapest possible signal that nobody proofread the surface.
> **Principle:** **Design-rank, no constitutional parent.** *(An earlier draft cited P9 and Art. 5. Both
> are about claiming what is not true; a straight apostrophe makes no false claim. The real authority is
> the artifact's own medium — this is a product about set type.)*
> **Refine:** Typographic apostrophes and quotes everywhere; en/em dashes used deliberately; a single
> ellipsis character rather than three periods; non-breaking spaces before units (`8 pages`, `100 %`
> style choices made once). ⚠️ **Scope carve-out:** the evidencing string itself —
> *"Let's make something cute ✨"* — is canonical in [VOICE](design/VOICE.md) and frozen by
> [ADR-033](DECISIONS.md#adr-033). It is **excluded** from this refinement and changes only if that ADR
> is superseded. The rule applies to every string that is not ADR-frozen.
> **Gain:** The invisible one. Nobody says "nice apostrophe." Everybody feels the difference between a
> page that was set and a page that was typed.

> **SYS-3 · Emoji are doing structural work inside a hand-tuned system.** 🟩 ⚠️ *(scope only)*
> **Costs:** 🖼️ inside "Add a photo", ✏️ inside "Add words", 🤚 in the coach mark. These are the
> platform's emoji font: a foreign illustration style, a foreign palette, a foreign optical weight, and
> a different render on every OEM skin — so the app's icon set is literally not the same design on two
> devices. The app *already has* its own drawn marks (the ornament tiles, Undo/Redo) in the same
> screenshot, which makes this an inconsistency rather than an absence of craft.
> **Principle:** [VOICE §2 rule 7](design/VOICE.md) — *"emoji as seasoning, not structure."* A glyph
> inside a button is structure. ⚠️ Scope note: ✨ punctuating a headline is *within* the rule and stays.
> **Refine:** Draw the button marks in the app's own hand, at one stroke weight, on one optical grid.
> **Gain:** The icon row stops being a different app's icon row.

> **SYS-4 · Elevation is decorative rather than systematic.** 🟨
> **Costs:** Cards carry a soft paper shadow; the page carries none; trays and popovers read as flat
> tonal surfaces rather than lit ones. *(Downgraded to 🟨: whether the trays use Material's tonal
> elevation specifically is an implementation claim stills cannot establish, and the critique does not
> record it. What is 🟩 is that the card has a shadow and the page does not.)* Three shadow languages
> means the screen has three light sources, and the eye
> resolves inconsistent lighting as *collage*, not depth. A single light source is the cheapest way a
> flat design reads as physical.
> **Principle:** P3 (the work is the fixed point — depth should encode what is above the page and what
> is the page). No constitutional parent; this is design-rank, from the *Creative Workbench* physicality
> the direction adopts.
> **Refine:** One light source, declared: light from top, shadows cast down, exactly three elevation
> tiers — **the page** (on a surface, real edge and thickness), **things resting on the page**
> (selected element, tape), **things above the room** (tray, sheet, popover). Tonal elevation is not
> used at all; shadow does the work, because paper does not change colour when you lift it.
> **Gain:** The app acquires a room. This is the largest single contributor to "physical, not animated."

> **SYS-5 · Corner radii are inherited rather than chosen.** 🟨
> **Costs:** The empty text block is a hard-cornered hairline rectangle in an app where every other
> rectangle is soft (🟩 — both recorded in the critique). Whether the remaining radii form a family or a
> collection, and how many of them are platform defaults, **is an audit nobody has run** — hence 🟨, and
> hence the refinement is *declare the family*, not *fix four values*. Radius is the most legible
> signature a design system has; unrelated radii read as unrelated designers.
> **Principle:** P2 (a thing seen twice is the same thing). Design-rank.
> **Refine:** A three-value radius family with a stated rule — **paper is square-cornered** (paper does
> not have rounded corners; this is a positive identity claim, not an omission), **chrome is one
> radius**, **pills and chips are fully rounded**. Nothing else exists.
> **Gain:** Consistency the user cannot name and will not stop feeling.

> **SYS-6 · Colour has two primaries and no legible rule.** 🟩
> **Costs:** Orange is primary everywhere except the last fold step, where a deep blue is primary. Two
> setting chips on Print step 2 are orange and two teal, apparently encoding *breaks-your-zine* vs
> *already-correct* — a good idea rendered invisible by the absence of a legend, on a product that has
> already proved it knows how to draw legends.
> **Principle:** P9 (honest beats kind) and R10 (decoration must also do a job). If colour carries
> meaning it must be decodable; if it does not, it must not vary.
> **Refine:** One accent for *the user's next action*, one for *the artifact's ink*, and **no third
> role**. Where colour encodes state, it is either legended in place or replaced by a labelled state.
> **Gain:** The user stops half-noticing a pattern they cannot resolve — which is a low-grade,
> continuous cost most designs never diagnose.

> **SYS-7 · Light theme is a port, not a design.** 🟩
> **Costs:** The library sits under a dark charcoal status bar in light mode; the editor keeps a full
> charcoal top bar above a cream page; the cream card sits on beige at nearly the same value, saved only
> by its drop shadow. "Paper on a desk" flattens to "beige on beige," and unthemed system chrome reads
> as an unfinished port — which is the single most *specifically Android-amateur* signal in the product.
> **Principle:** P9. R8 (contrast, including over texture). Art. 5 — shipping a theme we have not
> designed is a small dishonesty about what is finished.
> **Refine:** Design the light surface as a **different room, not an inverted one**: the desk becomes a
> lighter warm grey with real value separation from the paper, system bars are themed in both, and the
> shadow ramp is re-tuned (shadows that read on charcoal disappear on cream).
> **Gain:** Half the users stop seeing a half-built app.
> ⚠️ **Out of scope, named so it is not smuggled in:** the critique's alternative — *"or don't ship
> it"* — is a **product-scope decision**, not a craft refinement. Dropping a shipped theme needs an ADR,
> not this document.

> **SYS-8 · Disabled states read as broken rather than unavailable.** 🟩
> **Costs:** Disabled Redo is a dark grey slab with near-illegible text. "Unavailable" and "broken" look
> identical at that contrast, and a user who reads a control as broken generalises it to the app.
> **Principle:** R8, P6 (nothing is only a picture — a state needs a non-visual signal too). This is also
> where the device-verification lesson lives: a control can *look* and *test* disabled while telling the
> platform it is enabled ([CLAUDE.md device verification](../CLAUDE.md#device-verification-mandatory)).
> **Refine:** One disabled treatment across the product, above the contrast floor, distinguishable from
> both enabled and absent — and matching what the platform accessibility tree reports.
> **Gain:** The tray stops looking like it has failed.

> **SYS-9 · Touch targets are drawn at their paint size.** 🟨
> **Costs:** The nudge row's circles, the page-strip thumbnails and the step dots are small; whether
> their *hit* areas are ≥48dp is not visible in stills. When paint and hit agree at a small size, the
> app feels finicky, and finicky is the fastest route to "cheap" on a touch screen.
> **Principle:** R7 (≥48dp, thumb zone).
> **Refine:** Verify on device that every target's touch area exceeds its paint, and that adjacent
> destructive and non-destructive targets do not share a hit boundary.
> **Gain:** The app stops punishing ordinary fingers. **Marked 🟨 deliberately: this is a measurement to
> take, not a defect I have seen.**

> **SYS-10 · The product has three names for two places.** 🟩
> **Costs:** The editor is "the bench," the library is "Your zines" *and* "your shelf," Read is reached
> through a door labelled "Preview." *"Back to bench"* sits directly beneath *"It's on your shelf"* and
> goes to neither. Naming inconsistency is not a copy defect; it is a **model** defect — the user builds
> a map of the product from its nouns, and this one has extra rooms in it.
> **Principle:** P2 (everywhere is one place). Art. 5.
> **Refine:** One name per place, applied everywhere including screen-reader labels. *"Bench"* is a good
> word that appears nowhere the user could learn it — either teach it or retire it, but do not keep it
> as a synonym.
> **Gain:** The cheapest trust repair in the document.

---

# Library

**The question: "Which zine do I want?"**

## Pass 1 — Visual craft

> **LIB-1 · The screen is a grid with one item in it.** 🟩
> **Costs:** One card top-left; three-quarters of the screen undifferentiated near-black terminated by
> a floating button. This is not restraint — restraint is a decision about what to remove. There is no
> shelf, no surface, no edge, nothing that says the space is *for* anything, so the emptiness reads as
> *not loaded yet*.
> **Principle:** R4 (every blank state invites creation), R2 (what can I do next). P1 — the hero is
> absent, so the composition has no subject.
> **Refine:** Give the space a **surface**: a defined shelf plane with an edge and a light source
> (SYS-4), the cards resting on it. A one-item shelf should look like a shelf with one thing on it —
> which is a composition — rather than a grid with one cell filled, which is a state.
> **Gain:** "Here are my things," at one item, which is the number of things most users will have.

> **LIB-2 · "Your zines 1" reads as a debug label.** 🟩
> **Costs:** A count in tiny grey type beside a serif heading, with no alignment relationship to it —
> not baseline-aligned, not cap-aligned, not optically related. Two unrelated typographic objects
> touching is how metadata looks when nobody decided where it goes.
> **Principle:** P8 (**state, not score**) — a count is a score, and it is also the Feature Tribunal's
> objection to counters, *at roadmap rank*, cited as an application rather than an article.
> **Refine:** Retire the count, or set it as small-caps metadata on the heading's baseline, in the same
> treatment as the card's `8-PAGE MINI · A4` line — which is the product's own best metadata style and
> is already right.
> **Gain:** The header stops looking instrumented.

> **LIB-3 · The highest-status slot holds a commodity.** 🟩
> **Costs:** *"On this device"* is a pill in the top-right — the position convention reserves for
> account or settings — spending the screen's most valuable real estate on a claim every competitor also
> makes (✅ VERIFIED across [snipzine](https://snipzine.com/),
> [Dirty Little Zine](https://dirtylittlezine.com/), [Zeenster](https://zeenster.com/)). It is also the
> **first** of two privacy assertions before the first mark — the editor's line is the second
> ([§18.2](V1-DESIGN-ELEVATION.md): *"library pill + editor line"*).
> **Principle:** R12 — surface it *"as a gift, once, warmly."* The build says it twice; this is a rule
> violation, not a rule to change ([V1-DESIGN-ELEVATION §18.2](V1-DESIGN-ELEVATION.md) adjudicated it
> that way).
> **Refine:** Drop the chip; keep the editor's line. Keep the wordmark
> ([§18.7](V1-DESIGN-ELEVATION.md) split call).
> **Gain:** The first screen stops reassuring a user who has not yet asked for anything.

> **LIB-4 · Delete has the same weight as Duplicate.** 🟩
> **Costs:** It *is* differentiated by colour, but it sits in an undivided list, immediately below
> Duplicate, at the same size and rhythm, in a product with **no backup and no restore** (✅ VERIFIED,
> [CHANGELOG](../CHANGELOG.md)). The finding is the absence of *separation and ceremony*, not the
> absence of red.
> **Principle:** R6 (nothing unrecoverable without a gentle confirm). Art. 3 — durability is a
> constitutional duty, and a rhythm that makes deletion a peer of duplication is a design that has not
> read the article.
> **Refine:** A divider and a spatial gap before it; a slower, calmer target; and **verify** that R6's
> gentle undoable confirmation actually exists — it was never tested and must not be assumed.
> **Gain:** The one moment where a beginner can lose everything stops looking routine.

## Pass 2 — Interaction craft 🟨

> **All findings in this pass are ASSUMPTION.** No motion was recorded. They are specifications for what
> the interactions should be *once measured*, not reports of what they are.

> **LIB-5 · The card should become the editor, not be replaced by it.** 🟨 ⚠️
> **Costs:** Without spatial continuity, opening a zine is a cut. A cut asks the user to re-locate
> themselves; continuity does not. This is the mechanism reviewers name when they call Craft documents
> "published" where Notion's are "functional" (✅ VERIFIED, [2sync](https://2sync.com/blog/craft-vs-notion)).
> **Principle:** P2 (a thing seen twice is the same thing, moved).
> **Refine:** The card's cover art is the page; it grows into the editor's page from where it sat, at
> its own aspect, and returns to the same place on back. The transition is **interruptible** — a
> half-committed open that is abandoned returns rather than completing.
> **Gain:** The product acquires continuity, which is the difference between navigating and moving.
> ⚠️ Collides with [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md), which specifies cross-fades between
> page states and a 300–400ms screen transition. **Record first, then argue about frames rather than
> adjectives** — the critique's §18.4 position, and I hold it.

> **LIB-6 · The long-press and the overflow should be the same gesture at two speeds.** 🟨
> **Costs:** Unknown today. What is knowable is the rule: a card that responds to being held should
> respond *continuously* — lifting under the finger before the menu commits — or not respond at all.
> A discrete menu that appears after a silent delay is the most common Android interaction tell.
> **Principle:** P4 (tools are held), R1 (the gesture is never the only path — the overflow button
> stays).
> **Refine:** Press produces immediate physical feedback (a lift, tracking the finger), and the menu is
> the *continuation* of that gesture rather than a separate event.
> **Gain:** The shelf feels like objects rather than rows.

## Pass 3 — Emotional craft

> **Moment: opening the app.** *What should the user feel?* **Recognition** — *these are mine.*
> *What weakens it?* The card shows a generic orange blob that does not change when the work changes
> (verified: added text, returned, timestamp updated, artwork did not). Two zines about different
> things, made months apart, are distinguishable only by a name and a relative date.
> *The tiny detail that would strengthen it:* the cover, at the **folded booklet's proportion** rather
> than the imposed sheet's — because a zine's identity is its cover, and a sheet with a fold line is a
> manufacturing diagram. **(Authoritative statement: [BETA-UX-REVIEW §3](BETA-UX-REVIEW.md); this is a
> second witness, not a new finding.)** ⚠️ **Addition #1** — the card's art is derived from the title
> today; making it track the work is new behaviour. The *proportion* half is pure refinement and can
> proceed alone.

> **Moment: starting a zine.** *What should they feel?* **Instinct** — the way you pull a sheet toward
> you. *What weakens it?* The first act Zinely asks for is administrative: choose A4 or Letter, with
> dimensions, before a single mark. Cosmos named this exact failure when they engineered it away —
> requiring the filing decision first *"made saving feel like a decision instead of an instinct"*
> (✅ VERIFIED, [Cosmos](https://www.cosmos.so/blog/the-future-of-cosmos)).
> *The tiny detail:* default the paper, start them on page 1, and let the choice live at print time
> where it is a print property again. ⚠️ **Addition #4** — an earlier draft called this *"a refinement
> of sequence, not of feature,"* which redefined the constraint rather than satisfying it. Re-sequencing
> the creation flow is exactly what the table above forbids. It is the critique's Top-20 #8 and belongs
> to that list's approval, not to this one.

## Pass 4 — Premium audit

- **Generic:** the floating action button is the single most recognisable stock-Android object in the
  product, and it sits on the screen whose job is identity. A shelf that has a place where new zines
  come from — an edge, a slot, a labelled surface — is not a feature change; it is the same action
  drawn as part of the room.
- **Unfinished:** the emptiness below the first card, which is "content ran out" rather than a
  considered proportion. Void reads as luxury only when something negotiates it.
- **Over-explained:** two privacy assertions and a subtitle before the user has done anything.
- **Emotionally flat:** everything above is downstream of one fact — the screen does not contain the
  user's work. **No amount of Pass 1 craft fixes a composition whose subject is missing.**

---

# Editor

**The question: "How do I change this page?"**

## Pass 1 — Visual craft

> **ED-1 · The page resizes when you select something.** 🟩 — *the most damaging single detail in the product*
> **Costs:** With a block selected the tray grows and the canvas shrinks: the paper is measured **17%
> narrower** than a moment earlier (right edge 878px → 727px of 923). Physical objects do not change
> size when you pick up a tool near them. This one behaviour does more damage to the paper metaphor
> than every missing texture combined, and it is invisible as a bug because the app "works."
> **Principle:** **P3 — the work never moves unless the user moves it.** (Design-rank, with a
> design-rank parent: the measurement above plus [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md). It
> has no constitutional parent and does not borrow one.)
> **Refine:** The page is the fixed point. Chrome arrives **over** the page or the whole scene *offsets*
> as one rigid body; the paper's dimensions never change in response to chrome. If the tray must claim
> space, the page translates — it does not scale.
> **Gain:** The paper becomes an object. Almost every other physicality claim in this document is
> downstream of this one being true.

> **ED-2 · The page is flush left and has no edge.** 🟩
> **Costs:** The page sits against the left screen edge with a charcoal gutter only on the right — in
> dark theme this half-reads as canvas surround; in light theme it is unambiguously a stray dark band.
> And the page has no shadow, no thickness, no surface beneath it: it is a rectangle of cream that runs
> off the screen. Paper by FiftyThree's entire emotional proposition rested on your work being *an
> object you opened* (✅ VERIFIED, [IDSA citation](https://www.idsa.org/awards-recognition/idea/idea-gallery/paper-by-fiftythree/)).
> **Principle:** P3, and the *Creative Workbench* identity's structural claim — **the zine is always
> physically present.**
> **Refine:** Centre it optically (not mathematically — the tray's visual mass pulls the balance point),
> give it a real edge with a hairline and a soft cast shadow under the single declared light source
> (SYS-4), and put a surface under it. Margins around the page are symmetric or deliberately asymmetric,
> never accidental.
> **Gain:** The user is holding something.

> **ED-3 · Twelve identical white circles.** 🟩 — *the clearest case of engineering surfaced as interface*
> **Costs:** Selecting a block reveals a horizontally scrolling row of twelve unlabelled circles —
> ‹ › ∧ ∨ + − ↻ ↺ forward, back, **A**, trash — with no labels, no grouping, no separators and no
> visible scroll affordance (the second half is found by guessing). Each control is defensible: they are
> the non-gesture twins that **R1 requires** and that this project is right to take seriously. But an
> accessibility affordance has been promoted to the primary manipulation UI for everyone and given **no
> design treatment at all.**
> **Principle:** P6 (*"this is a design principle, not a compliance task"* — the principle exists
> because of exactly this defect). R1 stands: the non-gesture path is not removed, it is **designed**.
> **Refine:** Group by verb with separators (move · scale · rotate · order · style · remove), label them
> in the app's own hand, size them on one optical grid, show the scroll boundary, and **move delete out
> of the row entirely** — same size, same shape, same colour, one position from "nudge right," in an app
> with no backup, is not a styling issue.
> **Gain:** The core loop stops looking like a debug palette. This is the highest-visibility Pass 1 item
> in the product.

> **ED-4 · The tray mixes supplies with history.** 🟩
> **Costs:** Four equal tiles in one row: an orange creation button, a white creation button, and Undo
> and Redo in the same shell. **History is not a supply.** Equal treatment for unequal categories is the
> definition of a hierarchy failure, and it is why the tray reads as a toolbar rather than a table.
> Above it, *"Supplies"* — a lovely word — is set in 11px grey where it reads as a section header nobody
> styled.
> **Principle:** R3 (one primary action; secondary actions visibly quieter). VOICE's own vocabulary is
> right and its typography is not.
> **Refine:** Two categories, visibly different in weight and position: **supplies** (add a photo, add
> words) sized as the primary act; **history** quieter, smaller, set apart. Set "Supplies" in the
> product's own metadata style (the small-caps line the card already gets right).
> **Gain:** The tray becomes a craft table with things on it, which is the identity the design language
> has claimed since §2 and never rendered.

> **ED-5 · The empty text block is the only 1990s object on screen.** 🟩
> **Costs:** A hairline orange rectangle: hard corners, no fill, no placeholder, in an app where every
> other rectangle is soft (SYS-5). It is the first thing a user sees after their first creative act.
> **Principle:** P1 (the work is the hero — an empty block is a promise about the work), R4.
> **Refine:** A block that reads as *paper waiting for words* — the placeholder string VOICE already
> owns (*"Write something"*), set in the page's own type, in a soft container consistent with the radius
> family.
> **Gain:** The first text block stops looking like a wireframe survived into the build.

> **ED-6 · The styling panel stands in front of its own effect.** 🟩
> **Costs:** Tapping **A** opens a dark panel over the block being styled. How much is hidden depends on
> where the block sits — the categorical claim "you cannot see the effect" is **not** established, and
> the structural problem is: a control whose only job is to let you judge a visual change should not be
> anchored on top of the change. Inside it, `12 pt`: a print unit, raw, with steppers, on a page that
> prints at **A7, 74 × 105 mm**. Five colour swatches carry no names and **no selected state**, while
> the Align row directly above them shows selection clearly — so the panel disagrees with itself about
> whether selection is visible.
> **Principle:** P4 (never stands in front of the thing it affects). P9 — at least one ink (teal) falls
> below AA as body text on white (✅ VERIFIED shipped limitation) and is offered with no indication of
> what it will look like on 80gsm.
> **Refine:** Anchor the panel to the *opposite* edge from the block, or to a fixed edge that never
> overlaps the selection; give swatches a selected state matching Align's; name the inks; and report
> **characters per line** beside the size control — the unit that actually predicts whether an A7 page
> reads (the 50–75 character measure is ✅ VERIFIED,
> [Typography Handbook](https://typographyhandbook.com/print/); the derivation from ~55mm to 7–8pt and
> the CPL readout itself are 🟦 [R12.6](RESEARCH.md), and the ✅ does not extend to them).
> ⚠️ Per [§18.6](V1-DESIGN-ELEVATION.md), any drag-to-size is an **addition**; the stepper stays — and
> **so is the CPL readout**: it puts a number on screen that is not there today. It is listed here
> because it is small and directly serves P9, but it is an addition and is marked as one.
> **Gain:** Styling becomes judging rather than guessing.

> **ED-7 · The page strip is clipped rather than scrollable, and its thumbnails are too small to be work.** 🟩
> **Costs:** Page 8 is cut at the screen edge with no padding at either end, which reads as *broken
> layout* rather than *more content*. And a page holding one text block renders as a grey smudge — so
> the one place the editor shows the user their work shows it below the threshold where it is work.
> **Principle:** P1. R10 — the tape and the hand-placed rotation are the app's most zine-literate detail
> and they are **already right**; this finding protects them by fixing what surrounds them.
> **Refine:** Leading and trailing padding so the strip visibly *scrolls*; a thumbnail scale where a
> single text block is legible as text; and the current page's tape as the only selection signal it
> needs.
> **Gain:** Eight pages that feel like a booklet, which is the metaphor the whole product rests on.

> **ED-8 · Two defects that are defects, not design.** 🟩
> **Costs:** A stray orange caret renders at the *left edge of the page*, detached from the block being
> edited, within the first ten seconds of the core interaction. The coach mark — a page-mounted banner
> with an asymmetric notched corner — persisted across four unrelated interactions. Both read as
> glitches, and a glitch during the first creative act is priced far above its size.
> **Principle:** Art. 5. Neither is a design decision; both belong in the backlog and should be filed
> rather than designed.
> **Refine:** File them. Listed here only so a refinement pass does not accidentally adopt them as
> intended behaviour.
> **Gain:** — (defect removal, not craft).

## Pass 2 — Interaction craft 🟨

> **ASSUMPTION throughout. Record the baseline first** — the critique's ranked remedy #13, and the
> precondition for every timing below.

> **ED-9 · Drag must track the finger at 1:1 with no lag, and release must settle rather than snap.** 🟨
> **Costs:** If drag lags, the element is not an object; it is a value being updated. Users cannot name
> input latency and unfailingly feel it.
> **Principle:** P4. [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md) already specifies exactly this
> (*"dragging follows the finger 1:1 with no lag… releasing settles with a soft ease-out"*) — **so this
> is a conformance question, not a spec proposal.**
> **Refine:** Measure it on a mid-range device under a populated page. Ephemeral gesture frame; commit
> on release; the settle is small (the spec's 3–5%, one bounce) and identical everywhere.
> **Gain:** Direct manipulation becomes trustworthy, which is what lets a tool stop apologising.

> **ED-10 · Every interaction must be interruptible.** 🟨
> **Costs:** The single most reliable divider between "physical" and "animated" is whether a motion
> already in flight accepts a new input. An animation that must finish is a **modal state wearing a
> transition costume.** Apple's winning page-curl fell back unturned if you changed your mind mid-swipe
> (✅ VERIFIED, [Gadget Hacks](https://ios.gadgethacks.com/how-to/get-page-turning-curl-animation-back-apple-books-for-iphone-and-ipad-0385329/)).
> **Principle:** P4. Art. 6 (*"without being able to make an unrecoverable mistake"* — which extends to
> gestures).
> **Refine:** Selection, tray open/close, page change and panel present all accept input mid-flight and
> retarget from current position and velocity — never from the start state.
> **Gain:** The app stops taking turns with the user.

> **ED-11 · Placement should have a physical register; almost nothing else should.** 🟨
> **Costs:** Haptics are either punctuation or noise. [DESIGN-LANGUAGE §11](design/DESIGN-LANGUAGE.md)
> already says this; Android's guidance says effects *"shouldn't overwhelm the user or feel gratuitous"*
> (✅ VERIFIED, [haptics principles](https://developer.android.com/develop/ui/views/haptics/haptics-principles)).
> The [CHANGELOG](../CHANGELOG.md) records that the Type bar buzzes on **each accepted change**, which
> is a candidate for the "too frequent" side of that rule — **unverified, and worth measuring first.**
> **Principle:** P6 (every state has a non-visual signal — and the corollary: not every event is a
> state).
> **Refine:** A soft tick on *set down*, a lighter one on *pick up*, nothing on drag frames, nothing on
> routine taps. Audit the Type bar's per-change buzz against §11 rather than assuming either way.
> **Gain:** Touch feedback that means something, because it is rare.

> **ED-12 · Modes are invisible.** 🟩 *(the observation)* / 🟨 *(the remedy)*
> **Costs:** Nothing announces that a block is selected except its handles, and nothing says how to
> deselect. An invisible mode is the classic source of *"why did that do something different?"*
> **Principle:** P6, R2.
> **Refine:** Selection is stated by the object's own elevation (SYS-4's middle tier) rather than by
> chrome appearing; tapping the page deselects and the page says so by settling. **No new control.**
> **Gain:** The user always knows what they are holding.

## Pass 3 — Emotional craft

> **Moment: the first edit.** *Should feel:* **craft.** *Currently:* **configuration** — twelve
> circles, a covering panel, a `12 pt` stepper. *The tiny detail:* the tray's tiles should look like
> supplies with material (paper, tape, a marker's weight), not like four equal buttons. The word is
> already right; only the drawing is missing.

> **Moment: the first photo.** *Should feel:* **arrival** — *my thing is in the thing.* *Currently:*
> unknown from stills; the specification exists ([DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md): a
> photo *drops in* and settles). *The tiny detail worth defending:* it should land **selected, at the
> page's centre, at a size that is obviously adjustable** — and the settle should be the only
> celebration. The user's photo arriving is already the best thing that has happened in the app; nothing
> should be layered on top of it.

## Pass 4 — Premium audit

- **Generic:** the nudge row, the bottom sheets, the flatly-lit trays. This screen is where "default
  Android" actually lives in Zinely — not in the palette, which is distinctive, but in the *components*.
- **Visually noisy:** twelve equal circles plus four equal tiles plus a persistent banner plus a page
  strip, all at once, around a page that shrinks. The screen has no visual silence anywhere.
- **Technically correct but emotionally flat:** every control here works and satisfies a rule. The
  editor is the strongest evidence in the product that **satisfying the rules is not the same as making
  the decision the rule exists to force.**
- **Over-explained:** a coach mark that outstays four interactions.

---

# Read

**The question: "What have I made?"**

## Pass 1 — Visual craft

> **RD-1 · The zine loses its name at the moment you are invited to admire it.** 🟩
> **Costs:** The header says **"Your zine"** on a document titled *My zine* — and keeps saying it from
> Read through the fold walkthrough, while the library card and the overflow sheet both use the real
> title. A titled artifact that forgets its title is a small, corrosive trust leak.
> **Principle:** P1, Art. 5.
> **Refine:** The title, set in the display serif, where a book would put it.
> **Gain:** The thing on screen becomes *that* zine rather than *a* zine.

> **RD-2 · The subtitle is a permanent instruction in the author's slot.** 🟩
> **Costs:** *"Read · swipe to turn the page"* occupies the position a book gives its author, forever —
> not once, not on first run.
> **Principle:** **P7 — show, then say, then instruct.** An instruction that never expires is a design
> that failed once and then kept the receipt.
> **Refine:** The interaction teaches itself in one page-edge peek (which the layout already has); the
> line retires after the first successful turn, or becomes the byline.
> **Gain:** The screen stops talking during the one moment it exists to be quiet.

> **RD-3 · Three dead grey squares.** 🟩
> **Costs:** The Print & fold step indicator appears in the same position on Read with no state — the
> same component, inert, reading as three disabled buttons. A user cannot tell "not applicable" from
> "not working."
> **Principle:** R10 (decoration must do a job or get out of the way); R8 (a state below the contrast
> floor is not a state) — the same parents SYS-8 cites.
> **Refine:** Remove it. It is a component that travelled.
> **Gain:** — (removal; the screen's composition improves by subtraction, which is the cheapest kind).

> **RD-4 · The page has no depth and no stack.** 🟩
> **Costs:** A rounded cream rectangle on black, with no shadow, no pages behind it, no sense that seven
> more exist under this one. The screen's whole job is *this is a booklet*, and the composition contains
> no booklet.
> **Principle:** P1; SYS-4's single light source; the *Workbench* claim that the zine is physically
> present. Note also SYS-5: **paper is square-cornered.**
> **Refine:** Real edge, cast shadow, a visible stack behind (thin, honest — eight pages is thin), and
> the next page peeking as it already does.
> **Gain:** The single largest pride delta available on this screen for the least risk.
> **✅ CONFIRMED — [ADR-065](DECISIONS.md#adr-065), 2026-07-24.** [Validation A-5](ZINELY-DESIGN-SYSTEM-VALIDATION.md)
> proposed a rule under which a *representation* of paper would take the chrome radius — which would
> have made this finding's *"rounded cream rectangle"* compliant and retired RD-4's radius point.
> **A-5 was rejected.** The artifact and every representation of it are square-cornered, so this finding
> stands as written and is cited as evidence in the ruling. **Confirmed, not superseded.**

> **RD-5 · The loudest element pushes the user out of the screen.** 🟩
> **Costs:** *"Print & fold"* is the primary action on a screen whose job is to let you look — and on a
> blank zine it is primary over nothing.
> **Principle:** R3, R2. Art. 4 (quiet) — a screen for admiration that advertises the exit is a small
> engagement mechanic.
> **Refine:** Let Read's primary action be *reading*; demote the exit to a quiet, always-available step
> that does not compete with the artifact.
> **Gain:** Permission to sit with the thing — which is the emotional beat the whole product is
> arranged around.

## Pass 2 — Interaction craft 🟨

> **RD-6 · The page turn is the product's signature interaction and it is unmeasured.** 🟨 ⚠️
> **Costs:** For a product whose output is a folded paper booklet, page physics are load-bearing rather
> than decorative — and there is unusually strong evidence: Apple removed the Books page-curl, met
> sustained backlash, and **restored it in 16.4** (✅ VERIFIED,
> [MacRumors](https://www.macrumors.com/how-to/re-enable-page-turning-animation-apple-books/),
> [M.G. Siegler](https://mgs.blog/apples-turns-the-page-on-books-app-page-turns-8f9735a11ad5)). The
> winning version tracked the finger and fell back unturned if you changed your mind mid-gesture.
> **Principle:** P4; and the bar set by **[RESEARCH §R12.2](RESEARCH.md)** — *"a canned
> non-interruptible flip is worse than a cross-fade because it exposes the metaphor as fake"* — which is
> a 🟦 RECOMMENDATION bullet, not a ✅ finding. *(Note the namespace: `R1`–`R12` in this document mean
> [DESIGN-RULES](design/DESIGN-RULES.md); RESEARCH's §R12.n sub-sections are always written out in full.)*
> **Refine:** Finger-tracked, damped, **interruptible**, modelling **A7 card stiffness** rather than a
> floppy novel leaf — a small stiff card that resists, then goes. Velocity carries; abandonment returns.
> **Gain:** The one interaction users would describe to a friend.
> ⚠️ Collides with [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md)'s cross-fade profile. **Record the
> baseline, then adjudicate.** I hold this lightly and the spec's anti-toy instinct is right.

## Pass 3 — Emotional craft

> **Moment: reading.** *Should feel:* **pride, then surprise at oneself.* *Currently:* neutral — no
> name, no depth, an instruction where the byline goes, and an exit shouting. *The tiny detail:* the
> **first** page turn should be the moment the product proves the fold is real, because it is the first
> time the user sees their pages in reading order rather than in imposed order. Nothing needs to be
> added for that; the existing turn simply has to feel like paper.

## Pass 4 — Premium audit

- **Unfinished:** three inert grey squares.
- **Cheap:** a page with no edge on a black field is the default a prototype ships with.
- **Emotionally flat:** this is the screen that exists *entirely* for feeling, and every Pass 1 finding
  above is a place where feeling was left to the content. **Read is where refinement pays the highest
  emotional return per pixel changed in the whole product.**

---

# Print & Fold

**The question: "How do I print it correctly?" — and the best design work in the product.**

## Pass 1 — Visual craft

> **PF-1 · Step 1 shows page numbers where the artwork should be.** 🟩
> **Costs:** The sheet diagram is expert — dashed folds, the red **ONE CUT** rule with a pill label,
> upside-down numbers on the top row, a legend reading *fold lines · the one cut · printer can't reach
> here.* Nothing in the category comes close. And the Front/Back cover previews beneath it are blank
> rectangles containing a **1** and an **8**, at the moment of maximum pride.
> **Principle:** **P1.** This is a shipped, honestly documented limitation
> ([ADR-058](DECISIONS.md#adr-058) Decision 7), which makes it a **known gap to close**, not a defect to
> report.
> **Refine:** Real covers, at the folded proportion. Everything else on this screen is already right.
> ⚠️ **Addition #1** — deferred by that ADR's own terms; it needs the roadmap conversation, not this
> document.
> **Gain:** The screen stops being a diagram *about* a zine and becomes a diagram *of* one.

> **PF-2 · On step 2, the user's actual next action is the quietest thing on the screen.** 🟩
> **Costs:** The screen called "Print" cannot print (✅ VERIFIED shipped limitation,
> [ADR-052](DECISIONS.md#adr-052)). *Save PDF* and *Share* — the two actions that produce the user's
> output — are identical low-emphasis outlined buttons at ~60% screen height, while the loud primary
> advances the wizard, with ~28% of the screen empty beneath it.
> **Principle:** R3 (one primary action; **the primary must be the user's next act, not the app's next
> screen**), R2.
> **Refine:** *Save PDF* becomes the primary. *Share* becomes its clear secondary. The wizard advance
> becomes navigation, drawn as navigation.
> **Gain:** The user stops hunting for the button on the screen they came to the app to reach.

> **PF-3 · "Now fold it" is a chronological lie.** 🟩
> **Costs:** You have not printed. You cannot fold. The button is named for the next *screen*, not the
> next *act* — and this is the one product in the category whose interface writing is otherwise
> scrupulously honest about physical sequence.
> **Principle:** Art. 5. P7.
> **Refine:** Name it for what the user will do next, or for the room it opens (*"How to fold it"*).
> **Gain:** The product's best asset — its honesty about the physical world — stops contradicting
> itself in the loudest object on the screen.

> **PF-4 · Three type treatments in one line.** 🟩
> **Costs:** `100% · Actual size` in orange, `— not "Fit to page"` in white bold, the label in grey, on
> the Scale and Orientation rows — while Paper and Sides are set plainly. The pattern therefore reads as
> *emphasis-by-availability* rather than emphasis-by-meaning, which trains the user to ignore emphasis.
> **Principle:** Design-rank, no constitutional parent — the same authority SYS-1 and SYS-6 cite
> (typographic practice, plus R10's requirement that a treatment do a job).
> **Refine:** One treatment for *a setting*, one for *a value*, one for *a warning* — applied to all four
> rows or none.
> **Gain:** The warning that matters (`not "Fit to page"` is the setting that silently destroys the
> fold) gets its emphasis back by being the only thing that has it.

> **PF-5 · Step 3 nests two step systems and inverts its own proportions.** 🟩
> **Costs:** "Step 3 of 3" (squares, top-right) contains "1 of 5" (dots, bottom centre) in two visual
> languages on one screen. The reading order zigzags — title → subtitle → caption → diagram → step
> number → body → controls — with the caption *orphaned above the thing it captions*. And on a screen
> whose job is "show me the fold," title and subtitle take ~20%, the diagram ~22%, and ~35% below it is
> empty: **the fold is the third thing you read.**
> **Principle:** P7 (a demonstration outranks an explanation — here the explanation is laid out as the
> subject). R2.
> **Refine:** One step system. The diagram is the largest object on the screen and the first one read;
> the caption sits under it; the prose — which is genuinely good — supports rather than leads.
> **Gain:** The differentiating screen in the category finally looks like it knows it is one.

> **PF-6 · The recurring shape: title, paragraph, content, void, loud button.** 🟩
> **Costs:** It appears on Print step 2, Fold and Completion. Empty space that is a considered
> proportion reads as luxury; empty space that is *content ran out* reads as unfinished layout. Zinely's
> is the second kind, consistently at the bottom, because the button is pinned, the content is
> top-aligned, and nothing negotiates between them.
> **Principle:** Design-rank, no constitutional parent — the measurements in
> [V1-DESIGN-ELEVATION §11](V1-DESIGN-ELEVATION.md).
> **Refine:** Content is vertically negotiated rather than top-stacked: either the artifact grows into
> the available space (it is the subject; it should take the room) or the composition is deliberately
> weighted with the void *above* rather than below.
> **Gain:** Three screens stop looking like they ran out.

## Pass 2 — Interaction craft 🟨

> **PF-7 · Five still frames are the wrong medium for a motion.** 🟨
> **Costs:** The fold is a continuous physical action taught as five discrete diagrams — and this is the
> step the category actually under-serves (Electric Zine Maker's users explicitly asked for clearer
> folding and page-number guidance: ✅ VERIFIED, [itch.io](https://alienmelon.itch.io/electric-zine-maker)).
> **Principle:** P7 (a demonstration outranks an explanation). Art. 2.
> **Refine:** One continuous fold the user **scrubs**, on their own artwork — scrubbing rather than
> playing, because a person at a table with paper in their hands needs to go backwards. The five step
> texts remain as labels on the scrub, so nothing is removed and R1's discrete path is intact.
> **Gain:** The moat gets deeper at the exact point where it is already deepest.
> ⚠️ **This is a feature, not a refinement, and it is marked as one.** A scrubbable fold is a new
> control and a new rendering path. Its constitutional parent exists — the
> [Feature Tribunal](zinely-constitution.md#vii-the-feature-tribunal) admits *"Fold-along ritual
> (full-screen fold guide) — **KEEP, minor** — polish of an existing surface, not new scope"*, at
> **roadmap rank** — so it is admissible, but it belongs in a roadmap conversation and not in this
> document's mandate. It is retained here because the *compositional* half of PF-5 (make the diagram
> the largest object) delivers most of the gain and **is** pure refinement.

> **PF-8 · A fold step earns a crease.** 🟨
> **Costs:** Nothing today (unmeasured). The rule: reaching a fold step is the rare event where a single
> crisp haptic is *representational* rather than decorative — it is the sound of paper.
> **Principle:** [DESIGN-LANGUAGE §11](design/DESIGN-LANGUAGE.md); P6 (never the only signal).
> **Refine:** One crisp click per completed fold, nothing between.
> **Gain:** The walkthrough acquires a body.

## Pass 3 — Emotional craft

> **Moment: printing.** *Should feel:* **confidence.** *Currently:* **confidence on step 1** — the
> product's best screen, and *"It looks scrambled on purpose — the fold puts every page in order"* is
> the best piece of interface writing in the app because it names the user's fear before they feel it.
> Then *"where's the button"* on step 2. *The tiny detail:* the print-settings rows are written for a
> person standing at a printer, confused — they should be **weighted** for that person too, with the
> two settings that ruin a fold visually separated from the two that are informational.

> **Moment: folding.** *Should feel:* **guided by someone who has done this.** *Currently:* **reading a
> manual** — good prose, static frames, inverted proportions. *The tiny detail:* the diagram should be
> the biggest thing on the screen. That single change does more than any wording could.

## Pass 4 — Premium audit

- **Protect, do not touch:** the sheet diagram, its legend, and the ONE CUT rule. These are the internal
  benchmark for the rest of the product.
- **Over-explained:** the fold prose is excellent and is laid out as though it were the subject. The
  refinement is compositional, not editorial — **do not cut this writing.**
- **Unfinished:** two step systems, an orphaned caption, 35% of a screen doing nothing.
- **Generic:** the wizard's outlined-button pair is the most stock object on the most differentiating
  flow.

---

# Completion

**The emotional ending, and the place a single detail is worth more than a screen.**

## Pass 1 — Visual craft

> **CO-1 · The app draws a picture of somebody else's zine.** 🟩 — *the highest-value single fix in the product*
> **Costs:** Above the best sentence in the product — *"Your zine is a book."* — sits an illustration of
> a generic booklet with an orange rectangle on the cover. At the single most emotional moment Zinely
> will ever have with a user, it substitutes an abstraction for the artifact. This is also a **trust**
> finding: a user who notices will reasonably wonder what else is a placeholder.
> **Principle:** **P1** — *"an illustration standing where the user's own work could stand is a defect
> regardless of how well it is drawn."* Feature Tribunal, at roadmap rank, applies Art. 5 and 7 to
> exactly this under *"show the user their own work."*
> **Refine:** Same layout, same sentence, **their cover.**
> **Gain:** The largest emotional delta available anywhere in this document, from a change that redesigns
> nothing. ⚠️ **Addition #1** — it redesigns nothing and it *builds* something: rendering the user's page
> here is new behaviour with a parity question of its own. Highest value in the document, and still not
> this document's to authorise.

> **CO-2 · The button and the sentence beside it disagree about where the user is going.** 🟩
> **Costs:** *"It's on your shelf whenever you want it"* sits directly above **"Back to bench"**, which
> goes to the editor — neither shelf nor library (verified across two captures). And the primary action
> at the end of making a zine is **"+ Make another"**: production over admiration, at the one moment the
> user wanted to sit with the thing.
> **Principle:** P2 (everywhere is one place), SYS-10. Art. 4 — *"celebration is earned, brief, and
> **never a lever**"*; a "make another" primary is a lever.
> **Refine:** The button goes where the sentence says. The primary action is *look at it*; making
> another is available and quiet.
> **Gain:** The ending stops nudging.

> **CO-3 · The ending should leave an object, not play an animation.** 🟩 *(observed as correct)* / ⚠️ *(the docs disagree)*
> **Costs:** None in the build — [ADR-051](DECISIONS.md#adr-051) already replaced confetti with a quiet
> staged reveal and *"Your zine is a book."*, and the register is right: zine culture is explicitly
> anti-institutional (✅ VERIFIED, [RESEARCH §R12.4](RESEARCH.md)), and being congratulated by a cheerful
> app is the wrong emotion at the right moment.
> **Principle:** **P8 — the ending is quiet, and it leaves an object.**
> **Refine:** Nothing in the build. ⚠️ **[DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md),
> [VOICE](design/VOICE.md) and [EXPERIENCE-MAP](design/EXPERIENCE-MAP.md) still specify confetti and
> *"Your zine is ready! 🎉"*.* The docs are stale and the build is right; reconciling them is
> [V1-DESIGN-ELEVATION §18.3](V1-DESIGN-ELEVATION.md)'s unambiguous item and it is **doc work, not design
> work.** Listed here so a refinement pass reading those documents does not helpfully re-add confetti.
> **Gain:** — (protection).

## Pass 2 — Interaction craft 🟨

> **CO-4 · The reveal is the one place a real flourish is earned, and it must still be interruptible.** 🟨
> **Costs:** A staged reveal exists per ADR-051 and was not captured. The risk at this moment is
> specific: the more emotionally weighted an animation, the more tempting it is to make it
> uninterruptible — and an ending the user cannot skip is the app taking a bow at its own pace.
> **Principle:** Art. 4 (earned, brief). P8. ED-10's interruption rule applies most strictly here.
> **Refine:** Measure it. Whatever it is: brief, skippable by touch, and it must leave the object on
> screen rather than ending on a button.
> **Gain:** Quiet seriousness — *you have made a thing, and it now exists.*

## Pass 3 — Emotional craft

> **Moment: completion.** *Should feel:* **quiet pride, and the small shock of having finished
> something.* *Currently:* **pride, then a flinch** — a perfect sentence over a generic drawing. *The
> tiny detail:* the artifact should arrive and **stay**. Not play and vanish. The screen's last state
> should be the user's cover, at rest, with everything else quiet around it.

> **And the artifact identity's ending, adopted from Direction B:** *"make ten, give nine away."* This is
> the single best line the exploration produced and it is a **framing**, not a feature — it belongs in
> this moment's copy register, subordinate to VOICE, and it is the sentence that turns finishing into
> giving. Nothing needs to be built for it to be true.

## Pass 4 — Premium audit

- **Emotionally flat by accident:** the layout is correct and the subject is wrong. This is the clearest
  case in the product of **technically correct, emotionally hollow** — and it is one asset swap away
  from being the best screen in the app.
- **Generic:** the illustration.
- **Over-explained:** nothing. The writing here is finished. **Leave it.**

---

# The Premium Checklist

> **Moved 2026-07-24 — [CI-13](V1-CONFORMANCE-INVENTORY.md), Option a.** The 140-item Premium Checklist
> now lives in the authoritative design system, reachable through the design-system hierarchy from
> [§0.2](ZINELY-DESIGN-SYSTEM.md#02-the-rank-collision-stated-rather-than-created)'s named owner:
> **[ZINELY-DESIGN-SYSTEM §13.1 — The Premium Checklist](ZINELY-DESIGN-SYSTEM.md#131-the-premium-checklist-140-finish-details)**.
> It was relocated so a finish gate meant to *outlive this milestone* is discoverable from the design
> system rather than buried in a milestone critique. **Nothing about the list changed in the move** —
> the same 140 items, the same numbering, the same "sits beneath [DESIGN-RULES](design/DESIGN-RULES.md)"
> framing. A reference to "Premium Checklist #N" anywhere in the docs resolves to that home.

---

# The Ten Things Users Notice Without Knowing They Notice

Not the ten most important details — the ten that operate **below articulation**, where the user
produces a verdict ("nice app" / "cheap app") without producing a reason.

1. **Whether the thing they touched moved immediately.** Response latency under about 100ms is read as
   physical causation; above it, as a request being processed. No amount of subsequent animation quality
   recovers a late start.
2. **Whether an animation can be interrupted.** This is the single reliable divider between "physical"
   and "animated." Users never say "that was uninterruptible" — they say the app feels slow, which it
   is not.
3. **Whether the light comes from one direction.** Inconsistent shadow reads as collage. A single light
   source is why a flat design can feel like a room.
4. **Whether corner radii belong to one family.** Radius is a design system's signature, and mismatched
   curvature is the most legible sign that more than one person decided without talking.
5. **Whether text sits on a shared baseline.** Two labels beside each other, misaligned by two pixels,
   produce "sloppy" without ever producing "misaligned."
6. **Whether the hit area is bigger than the paint.** Every missed tap costs more than it seems, because
   the user attributes it to the app rather than to their finger.
7. **Whether the apostrophes are curly.** Especially in a product about printed matter. Nobody mentions
   it; everybody feels the difference between set and typed.
8. **Whether emphasis means the same thing twice.** When bold, colour and size are used
   interchangeably, the user stops reading emphasis at all — and the screen goes quiet in the wrong way.
9. **Whether the app shows their work or a picture of the idea of their work.** This is the fastest
   trust judgement in a creative tool, and it is made in the first second on the first screen.
10. **Whether the ending leaves something behind.** An ending that plays and vanishes is entertainment;
    an ending that leaves an object is proof. Users describe the second one to other people.

---

# Things I would leave exactly as they are

Refinement's failure mode is that it does not stop. Everything below is at a point where further change
is more likely to make it worse than better — either because it is already the product's best work, or
because the improvement available is smaller than the risk of touching it.

**Do not touch — this is the product's best work:**

1. **The sheet diagram and its legend.** Dashed folds, the red ONE CUT rule, upside-down numbers,
   *fold lines · the one cut · printer can't reach here.* Best in category. Its only fault is the
   artwork it contains, which is PF-1 — the drawing itself is finished.
2. **"It looks scrambled on purpose — the fold puts every page in order."** It names the user's fear
   before they feel it. Do not tighten it; the length is doing work.
3. **"Your zine is a book."** The best sentence in the product, in exactly the right register. Any edit
   makes it worse.
4. **"All folds are valleys."** and *"Push the two ends toward the middle so the panels pop into a
   plus."* This is professional interface writing. The Fold screen's problem is compositional; **the
   words are done.**
5. **The wordmark.** `Zinely.` with the orange period. The single strongest identity asset in the
   product, and the reason the first screen does not look like an Android app. (Adjudicated against
   [BETA-UX-REVIEW](BETA-UX-REVIEW.md)'s recommendation to drop the row: keep the wordmark, drop the
   chip — LIB-3.)
6. **The display serif** as the identity voice. It should be used *more*, but its treatment where it
   already appears is right.
7. **The tape on the current page thumbnail, and the hand-placed rotation of the page strip.** The most
   zine-literate detail in the app, and codified in the app's own canon:
   **[DESIGN-RULES R10](design/DESIGN-RULES.md)** — *"a tape strip **is** the current-page marker"* — is
   its parent, and [V1-DESIGN-ELEVATION](V1-DESIGN-ELEVATION.md)'s rediscovery list attributes it there
   too. **Do not regularise it. Do not tune the angles. Do not make it consistent.** Consistency here
   would delete the finding.
   *(An earlier draft justified this with "near-but-not-exact alignment" cited as ✅ VERIFIED from
   RESEARCH §R12.4. Two errors, and they are the milestone's repeat offence recurring inside the section
   that claims to protect canon: that marker list is a **🟦 RECOMMENDATION** bullet, not a ✅ finding;
   and R12.4 is a finding about the **output**, whose own resolution forbids transferring it to the tool
   — which is what [V1-DESIGN-DIRECTIONS](V1-DESIGN-DIRECTIONS.md) already adjudicated. The page strip is
   tool chrome. The instruction stands; the authority for it was wrong.)*
8. **"Supplies"** as the word for the tray. The typography is wrong (ED-4); the vocabulary is right and
   canonical in [VOICE](design/VOICE.md).
9. **The small-caps `8-PAGE MINI · A4` metadata line.** It is already the product's correct metadata
   style; the refinement elsewhere is to *match* it, not to change it.
10. **The three-step Print → Fold → Done arc**, and the whole navigation topology.
    [ADR-058](DECISIONS.md#adr-058) bought this ordering and two well-sourced cases argue against
    restructuring in a visual pass (✅ VERIFIED: Apple Photos' iOS 18 rollback; Google's *"functionality
    must never be sacrificed for visual impact"*). **Change the surface, keep the structure.**
11. **The 8-page single-sheet constraint.** It is the reason the ending is reachable (Art. 1). It is
    never a refinement target.
12. **"A fresh page. What goes here?"** — the second canonical empty-state string, which is markedly
    better than the first and shows the pattern working.

**Do not touch — because it is not mine to touch:**

13. **The editor empty state.** *"Let's make something cute ✨"*, the ornament tiles, the invitation-only
    rule, the privacy line. Frozen by [ADR-033](DECISIONS.md#adr-033) (Accepted), canonical in VOICE and
    DESIGN-LANGUAGE §8. [V1-DESIGN-ELEVATION §18.1](V1-DESIGN-ELEVATION.md) deliberately left the
    disagreement unresolved, and it needs **a superseding ADR or a withdrawal, not a refinement pass
    quietly restyling it.** I have a view; a view is not a mandate.
14. **The motion profile in [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md)** — gentle ease-out,
    300–400ms screen transitions, 3–5% one-bounce settles, *"avoid big springs."* Every 🟨 finding in
    Pass 2 above departs from it somewhere. **None of them may be built before the motion baseline is
    recorded.** The spec is coherent, its anti-toy instinct is right for this product, and my
    disagreement is an argument about frames that has not yet been had with frames.
15. **The `pt` stepper.** ED-6 adds a characters-per-line readout beside it; per
    [§18.6](V1-DESIGN-ELEVATION.md) the stepper itself stays, and any drag-to-size is an addition. My
    own earlier phrasing on this was internally inconsistent with R1 and was corrected.

**Do not touch — because the improvement is smaller than the risk:**

16. **Library sort, search and grouping.** A real cliff at thirty zines, irrelevant at three, and a
    feature besides. Out of scope by definition.
17. **The palette itself.** Cream, charcoal, burnt orange, teal. The findings above are about how it is
    *applied* (SYS-6, SYS-7), never about the colours. Re-picking them would restart an identity that is
    already decided.
18. **The voice's register.** Warm, second-person, honest before kind. SYS-2 is punctuation, not tone.
    Tone is architecture and survives redesigns ([Sacred Thing 4](zinely-constitution.md#v-the-sacred-things-change--never)).

---

## Where this document stops

Three things it deliberately does not do, each because doing them would repeat a mistake this milestone
has already made once:

1. **It does not specify motion timings for animations nobody has recorded.** Pass 2 is graded 🟨
   throughout and is a *specification pending measurement.* Record first.
2. **It does not restyle the editor empty state**, which is frozen by an accepted ADR reached with more
   context than a refinement pass has.
3. **It adds four things, and it says so.** A draft of this section claimed the document added nothing;
   independent review falsified that, and the four are now tabled up front and marked ⚠️ where they
   appear — real covers, the scrubbable fold, the characters-per-line readout, and moving the paper
   choice. Each carries its Feature Tribunal or research parent, at that parent's actual rank, and each
   goes to a roadmap conversation rather than being built on this document's authority. **Everything
   else adds nothing**, and the 140-item checklist contains no new capability, which is the test of that
   narrower claim.

**Next, in order:** record the motion-and-haptics baseline → adjudicate the ⚠️ collisions (motion
profile, empty state) → HTML specification for the accepted refinements → design freeze → Compose →
both [device-verification passes](../CLAUDE.md#device-verification-mandatory).

**No implementation until this is approved.**
