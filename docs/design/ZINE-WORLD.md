# The Zine World — creative direction for the whole product

**Status:** Research + proposal · 2026-08-15 · **not ratified**
**Owns:** the product-wide art direction — what world Zinely is, and the grammar every surface inherits from it.
**Does not own:** tokens ([V21-SPEC](V21-SPEC.md)) · decisions ([DECISIONS.md](../DECISIONS.md)) · scope ([PRD](../PRD.md)) · voice rules ([VOICE.md](VOICE.md)).

This document exists because the corpus has named its own world **four different times**, and the code has been
inheriting whichever one was nearest. Everything below is written to be falsifiable: claims about the repo carry
`file:line`, claims about the device carry a screenshot, claims about the outside world carry a URL.

---

## 0. The finding that reframes the whole exercise

I was asked whether the light-tool-card-inside-a-dark-room split is a meaningful principle or an implementation
artifact. The honest answer changed the shape of this document:

> **The principle is already law. The implementation is already wrong. And they are wrong in *different places
> from where I was looking*.**

The law is an owner ruling, OD-12 / D-035, 2026-08-02, codified as ADR-090:

> "**The editor represents the physical printed artifact.** … The artifact itself **does not dim**; the room
> around it may. … Dark theme darkens the surrounding studio chrome, **not the sheet**."
> — [`V2-SPEC-DEFECTS.md:4379-4381`](V2-SPEC-DEFECTS.md), [`DECISIONS.md:3008`](../DECISIONS.md)

That is not a UI trick. It is the strongest idea in the product, and on the device it is the reason the Bench
reads as a place rather than a screen. So the light/dark split **stays**. What is broken is *where the boundary
falls* — and the specific defect is one nobody has looked at yet. See [Ruling A](#ruling-a--the-two-palette-split).

---

# A. Reference library — the supplied set

Fifteen URLs, twelve read directly. **Eight of the fifteen are directories, not designs** — useful as evidence
about *curation and credit*, not as art direction. Marked accordingly rather than pretended into references.

| # | Reference | What it actually is | The mechanism (not the vibe) | Verdict |
|---|---|---|---|---|
| 1 | [cameronsworld.net](https://www.cameronsworld.net/) | Infinite-scroll collage of archived GeoCities fragments | Every fragment **links back to its Wayback source**. It isn't retro-styled, it's *sourced*. Plus one invented object: "Catscape Navigator 2.0" | **ADAPT** — take provenance, not density |
| 2 | [melonland.net](https://melonland.net/) | Web-revival community hub | A **small, one-hand ornament kit** in one palette. Reads as a place because the kit is *bounded*, not because it's busy | **ADAPT** — the bounded kit; not the GIFs |
| 3 | [spacejam.com/1996](https://www.spacejam.com/1996/) | Preserved 1996 film promo | **Navigation is an illustration** — each destination is a painted object with its own name, not a row in a list | **ADAPT** — exactly one screen may do this |
| 4 | [theuselessweb.com](https://theuselessweb.com/) + [/sites](https://theuselessweb.com/sites/) | One giant button; a curated index | **Scale as editorial judgement** — one verb at 20×. And per-entry "who made it, why, and how it got here", plus a maintained graveyard of dead entries | **BORROW** both |
| 5 | [mondrianandme.com](https://mondrianandme.com/) | Mondrian-composition toy — *unverified, JS shell* | Comparable verified tool locks palette to five colours and lines to orthogonal: **constraint is the authorship engine** | **ADAPT** (principle only) |
| 6 | [pointerpointer.com](https://pointerpointer.com/) | Photos of people pointing at your cursor — *mechanism from search* | The response is **aimed, not random**: nearest-neighbour lookup on *your* actual coordinate, with a deliberate beat before reveal | **BORROW** — "aimed response" |
| 7 | [brutalistwebsites.com](https://brutalistwebsites.com/) | 900+ site gallery, many with designer interviews | "Truth to materials" — and the **interview as metadata**: a screenshot is a look, a screenshot plus the maker's reasoning is an argument | **IGNORE the aesthetic**; borrow truth-to-materials |
| 8–10 | Webflow [handmade](https://webflow.com/made-in-webflow/handmade) · [diy](https://webflow.com/made-in-webflow/diy) · [collage](https://webflow.com/made-in-webflow/collage) | Tag-filtered marketplace feeds | Only real mechanism: **permanent creator byline on every card**. The tags are folksonomy noise — "collage" surfaces an Apple.com scroll clone | **IGNORE ×3** |
| 11 | [gossipsweb.net](https://gossipsweb.net/) | Curated personal-site directory | Tag vocabulary includes **feeling-words** (ephemera, sparse) — the curator's voice showing through taxonomy. Plus a **badge** makers apply to their own site | **BORROW** — vocabulary + badge |
| 12 | [indieseek.xyz](https://indieseek.xyz/) | Human-curated link directory + blog | Publishes **the method** alongside the results | **IGNORE** as design |
| 13 | [openweird.com](https://openweird.com/) | Useless Web clone | **Negative evidence**: copying the *shape* of an authored interface without the editorial labour reads as generated. This is Zinely's exact failure mode | **IGNORE** — control case |
| 14 | [directory.weirdnet.org](https://directory.weirdnet.org/) | 37,326 hand-picked sites | "Picked by hand, by a human, with a mouse — no crawler, no algorithm," and entries may be "dead, half-dead, abandoned, or haunted." **A directory of 37k proves nothing about care; admitting its own decay does** | **ADAPT** — highest yield, lowest risk |
| 15 | [theindex.fyi](https://theindex.fyi/) | Meta-index of 38 indie indexes | Marks one entry "Inactive" instead of deleting it — the **third independent instance** of visible decay in this set, which makes it a pattern | **IGNORE** as design; mechanism covered by #14 |

### The principles that recur, ranked by load

1. **Provenance attaches to the artefact, permanently.** Five of fifteen do this. Nothing else on the list buys as much *"a person made this"* per unit of effort.
2. **System copy is written by a person, and admits its limits.** Three independent sites keep their dead entries visible. Empty states and errors are the cheapest authored surface in a mobile app, and the one most often left as boilerplate.
3. **A bounded material kit made by one hand.** Smallness and consistency read as a studio; quantity reads as a theme store.
4. **Exactly one invented object per product.** Space Jam's painted constellation works *because it is singular*. Two invented navigational objects read as inconsistency, not craft.
5. **Truth to materials.** Show the real page, real bleed, real fold. This is also the guard rail: fake paper texture is the *opposite* of truth to materials, however handmade it looks.
6. **Constraint is the authorship engine.** Three inks and two rules produce work with a house style; a colour wheel produces work with none.
7. **Responses are aimed, not random.** Deterministic-on-input reads as craft; random reads as noise.
8. **One loud verb per screen.**

**What none of these licenses:** density · animated ornament · radial nav · deliberate latency · deliberate
discomfort · engagement metrics · ideology in-product. The gap between the principle and the artefact is exactly
where the fake-retro failure lives.

---

# B. References I found — twenty, ranked

## The five that matter most

**1. [Bookbinder JS](https://momijizukamori.github.io/bookbinder-js/)** — client-side PDF imposition. Signature
formats, folio/quarto/octavo, and a *"Wacky Small"* group (Single Sheet Zine 8-per-side, Petite 16, Tiny 32, Mini 60).
**Mechanism:** craft vocabulary *as* UI. It names real bindery terms and lets the naming teach. One playful label
carries the entire personality budget of an otherwise technical form.
**Informs:** Print/Fold, the imposition option model, fold-preset naming. → **BORROW**

**2. [Kinopio](https://kinopio.club/)** — spatial thinking canvas. Explicitly rejects "sticky notes in one of five
designer-approved pastel shades"; founder signs off with a kaomoji; custom hand-edged SVG icons.
**Mechanism:** personality lives in **copy and user-owned colour**, not chrome. The interaction model is boringly
direct — tap, type, drag — so the character costs *nothing* in usability.
**Informs:** the Bench, and every empty state. → **BORROW.** This is the existence proof for the whole brief.

**3. [Zineopolis](http://zineopolis.blogspot.com/)** — 300+ art-zine archive. Every entry records edition size,
**dimensions, binding method** (saddle-stitched, perfect bound, *"foldy"*), page count, print technique, paper stock —
and photographs **spreads**, not cover thumbnails.
**Mechanism:** the physical spec *is* the identity. A zine is identified by its fold and its print method before
its title.
**Informs:** the Shelf. It currently answers "which zine?" with a filename and a date. → **BORROW the metadata vocabulary directly.**

**4. [STUDIO·ITY Riso Simulator](https://studio-ity.com/riso/)** — browser riso sim: named ink presets, grain type
and percentage, ink spread, **misregistration as an offset in px and an angle in degrees**.
**Mechanism:** imperfection exposed as *parameters with units*, never a "vintage" toggle. The handmade quality is
engineered and tunable. (Also: "your images never leave your device" — same claim Zinely makes.)
**Informs:** print preview; the model for any imperfection we ship. → **ADAPT**

**5. [perfect-freehand](https://github.com/steveruizok/perfect-freehand)** + **[Rough.js](https://roughjs.com/)** —
handmade line as **geometry, not texture**. Nothing is a bitmap; the wobble is computed, so it scales, themes, and
prints cleanly.
**Informs:** fold/cut marks, page borders, selection outlines. → **ADAPT — one controlled use buys the whole art direction; applying it to controls would wreck touch affordance.**

## The other fifteen

| Reference | Category | Mechanism | Informs | Verdict |
|---|---|---|---|---|
| [Are.na](https://www.are.na/) | objects-on-surface | Restraint + one proprietary typeface carries the identity; content supplies the colour. "Nothing algorithmic steering you" | Shelf; the case for type-over-chrome | **BORROW** |
| [iA Writer](https://ia.net/writer) | authored tool | "No buttons, no popups, no title bar"; "a scalpel in a world of Swiss army knives" — the positioning sentence *is* the feature-cut criterion | TypeBar; what earns a place on the Bench | **BORROW** |
| [Endless Paper](https://endlesspaper.app/) | native tool | Physicality from **responsiveness** (120fps vector), not decoration. No file management | Bench pan/zoom targets; Reframe | **ADAPT** (not the infinite canvas — the bounded page *is* the product) |
| [Excalidraw](https://excalidraw.com/) | creative tool | User-controllable roughness; the hand-drawn look **signals "informal and evolving"**, lowering the bar to starting | Empty state, first run | **ADAPT** — content defaults only, never controls |
| [Sandspiel](https://sandspiel.club/) | tactile toy | Paint a substance, physics answers. No dialogs | Element placement immediacy | **ADAPT** |
| [Teenage Engineering](https://teenage.engineering/) | physical→web studio | Rigid grid + **zero marketing adjectives** = objects read as made-with-care rather than sold | Shelf cards, iconography | **ADAPT** — discipline yes, coldness no |
| [RISOTTO Studio](https://risottostudio.com/) | riso studio | Nav is literally **"Ink Palette."** You do not pick a colour, you pick an **ink** — finite, named, physical | The editor's colour model | **BORROW** — the single most "small press" decision available |
| [Nieves](https://www.nieves.ch/) | zine publisher | Homepage is *five* new zines. Editorial **scarcity**, no infinite grid | Shelf; any template gallery | **ADAPT** |
| [Velvetyne](https://velvetyne.fr/) | libre type foundry | Voice with a position; 40+ libre faces shown by *use*, not specimen | A future font offering — on-brand **and** licence-clean | **BORROW** (aesthetic *and* supply) |
| [Ableton Learning Synths](https://learningsynths.ableton.com/) | authored learning | Teaching happens **inside the instrument**; every lesson has "Open in Playground" as an escape hatch | Onboarding — directly answers the [ADR-058](../DECISIONS.md#adr-058) lesson about teaching at the wrong moment | **BORROW** |
| [Cosmos](https://www.cosmos.so/) | objects-on-surface | Draggable overlapping **clusters** — objects sit *on* each other, which a list cannot express | Shelf, if it ever becomes a surface | **ADAPT** — needs a list equivalent for a11y |
| [Hato Press](https://hatopress.net/) | riso publisher | Studio-as-**place** framing: appointments, visits, workshops | Product voice — "place" language beats "feature" language | **ADAPT** (voice only) |
| [Poolsuite](https://poolsuite.net/) | authored toy | Total commitment to one fiction, boot screen included | — | **IGNORE** — the fake-retro failure mode, named |
| [JSKIDPIX](https://kidpix.app/) | creative tool | "No guide—have fun!" Features hidden behind ⌘/⌥/⇧ | — | **CAUTIONARY** (below) |
| [NN/g on Neobrutalism](https://www.nngroup.com/articles/neobrutalism/) | cautionary | Documents the exact harms: failed contrast pairs, removed focus outlines, flattened hierarchy | Use as the **acceptance checklist** for V2.1 tokens | **BORROW as a test** |

### The three cautionary examples, and the rule they imply

**Kid Pix** is the sharpest. Its charm *is* undiscoverability — unlabelled icons named `kp-m_27`, good features
behind modifier chords. On a desktop toy with no stakes that is delight. In Zinely — touch-only, beginners, real
work — the identical pattern is icons a first-timer cannot decode and TalkBack cannot announce.

**Poolsuite** fails differently: a whole-app fiction must be maintained on *every future screen*, and the first
screen needing a real form breaks it.

**Neobrutalism** is the trap nearest our stated direction, because thick borders and flat colour *look* like
"made, not manufactured" while quietly deleting the affordance layer.

> **The rule all three imply — and the single most important line in this document:**
> **Put the handmade quality in content, typography, copy, and imperfection you can quantify.**
> **Keep it out of controls, hit targets, focus indication, and iconography.**

Every reference that survives contact with a real tool obeys that split. Every cautionary one violates it.

---

# C. Zine's world

## C.1 The corpus has named itself four times

| Metaphor | Verbatim | Where |
|---|---|---|
| **Quiet café** | "a **quiet café where you make tiny books with your hands** — warm, calm, unhurried, and yours" | [`V2-CONSTITUTION.md:28-29`](V2-CONSTITUTION.md) — and it outranks everything by its own terms (`:5`) |
| **Paper studio / craft table** | "A warm, private little **paper studio** … a **quiet craft table** that happens to be digital" | [`V2-PRINCIPLES.md:95-97`](V2-PRINCIPLES.md) — self-labelled *not ratified* (`:8`) |
| **Craft table with supplies** | "The metaphor is a **craft table** … supplies within reach. Chrome is 'supplies,' not 'toolbars'" | [`DESIGN-LANGUAGE.md:64-65`](DESIGN-LANGUAGE.md) — V1, superseded, **not marked as such** |
| **A verb, not a place** | "**FINISHING.** One word." · "The finishing business: personal publishing at the smallest possible scale" | [`zinely-constitution.md:27,138`](../zinely-constitution.md) — the *founding* constitution, higher rank still |

And a vocabulary law that quietly disagrees with all of them:

> "We reach for the vocabulary of **paper, ink, presses, shelves, and folds** — not files, dialogs, layers, and
> exports." — [`V2-CONSTITUTION.md:77-78`](V2-CONSTITUTION.md)

## C.2 The diagnosis

**Every operative noun in the vocabulary law is a print-shop noun.** Paper, ink, presses, shelves, folds. Not one
of them is a café noun. The café has been doing two jobs — supplying the *feeling* and supplying the *place* — and
it is only good at one of them.

A café explains warmth, calm, privacy, unhurriedness. It explains nothing about tools, materials, inks, the shelf,
the fold, or why the work leaves the building. You cannot ask *"what is this screen, in a café?"* and get an answer
about a Type bar. That question has been unanswerable, which is precisely why surfaces have drifted apart.

"Studio" fails for the opposite reason: a studio is a *room you look around*, and the physical research is blunt
that this does not survive a phone — peripheral vision covers a square metre of desk, a phone has one focal zone,
and anything "around" the work is off-screen.

## C.3 The recommendation

> ## Zinely is a one-person press.
> ### The café is how it feels. The press is what it is.

Not a studio (too big for one focal zone). Not a desk (a desk has no output). Not a café (you don't own the
furniture and you can't leave your work out). **A press** — small, warm, personal, and existing entirely so that
something comes *off* it, finished.

This reconciles the two constitutions rather than overruling either. The founding one says the north star is
**FINISHING**; a press is the only one of the four metaphors whose whole purpose is that something is finished.
The design one supplies the register: quiet, warm, unhurried, yours.

**The press has exactly three places, and the app already has exactly three navigation destinations.** That is not
a coincidence to engineer — it is one that already exists and has never been named:

| Place | Destination | What it is | What it answers |
|---|---|---|---|
| **The Bench** | `EditorRoute` | The lit work surface. One sheet at a time, tools at thumb reach, in fixed positions | "How do I change this page?" |
| **The Proof** | `ProofRoute` | Where you check the thing before it is real. Objects at rest | "How do I print it correctly?" / "What have I made?" |
| **The Shelf** | `HomeRoute` | Where finished work is kept | "Which zine do I want?" |

And one sentence governs every surface:

> **You are making one small thing, by hand, and it is going to come off the press.**

Any screen that cannot be placed in the Bench / Proof / Shelf is either mis-homed or shouldn't exist.

## C.4 What this costs

Adopting it requires an amendment to [`V2-CONSTITUTION.md`](V2-CONSTITUTION.md) — the constitution's spatial
metaphor becomes "a one-person press", the café is retained explicitly as the *emotional register*, and
`DESIGN-LANGUAGE.md`'s craft-table paragraph is finally marked superseded. It is a **documentation** change, not a
visual one: nothing currently shipping contradicts the press. That is itself evidence for the recommendation.

---

# D. Product Cohesion Map

Every shipping surface, placed. Sources: nav host [`ZinelyNavHost.kt:110-141`](../../app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt); full inventory in §H.

```
                    ┌─────────────────────────────────────────┐
                    │           THE ONE-PERSON PRESS          │
                    └─────────────────────────────────────────┘
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        │                               │                               │
   ┌────▼─────┐                   ┌─────▼─────┐                   ┌─────▼─────┐
   │  SHELF   │  ── open ──▶      │   BENCH   │   ── check ──▶    │   PROOF   │
   │ finished │  ◀── put away ──  │  making   │   ◀── back ────   │ finishing │
   └────┬─────┘                   └─────┬─────┘                   └─────┬─────┘
        │                               │                               │
  covers, edge-on            the sheet · the inks             the sheet at rest
  tape, stamps, labels       scraps · marks · guides          the fold · the press
        │                               │                               │
  ZineActionSheet            Add · Ink · Type · Reframe        Print details · Fold
  Create · Rename            Page grid · Page strip            Save PDF · Share
```

| Surface | Is, in the press | Material | Enters like |
|---|---|---|---|
| **Shelf** | the rack of finished copies | cover stock, tape, stamped labels | you walk in |
| Zine cover | a finished zine, seen face-on | heavier stock than pages ([`V2-IDENTITY.md:94-96`](V2-IDENTITY.md)) | at rest, tilted |
| `ZineActionSheet` | picking one up to decide what to do with it | — | a drawer opens |
| `ShelfCreateSheet` | choosing paper before you start | paper stock | a drawer opens |
| **Bench** | the lit work surface | the sheet, on a darker desk | the sheet is laid down |
| The page | **the artifact** — the thing being made | paper, always lit | it does not move |
| `SelectionChrome` | a mark you made round something | dashed ink | it is drawn |
| `BenchContextBar` / `TypeBar` / `BenchInkPopover` | **tools you picked up** | room, not paper | brought to hand |
| `ReframeOverlay` | cutting a window in a photo | dashed ink over the artifact | the cut is marked |
| `BenchPageNav` | the sheet stack, edge-on | paper | always there |
| `BenchPageGrid` | all pages laid out at once | paper | spread out |
| `BenchAddChooser` | reaching for supplies | room | a drawer opens |
| **Proof** | the checking table | the sheet at rest | you step back |
| The proof page | the artifact, tilted, at rest | paper | it settles |
| Commit band | the press itself | room, taped on | it is already there |
| `ProofPrintDetailsPanel` | the spec sheet | room | a drawer opens |
| `ProofFoldAct` | folding instructions | **should be a white sheet** — see §H | a drawer opens |
| `ZSnackbar` / `BenchSnack` | someone telling you what just happened | room | a note slid over |
| Empty states | an empty bench / an empty rack | paper | — |

**Two surfaces answer no question and belong to no place:** `BootLoading` and `BootFailure`
([`ZinelyNavHost.kt:442,532`](../../app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt)). In the press,
opening a zine is *fetching it from the shelf* — these should be part of the Shelf→Bench transition, not a
full-window nowhere.

---

# E. Zine Design Grammar

## E.1 Materials — five, and no more

The corpus's texture law is already the right one and needs no improvement, only enforcement:

> "**if a texture cannot name its physical cause, it does not ship.**" — [`V2-CONSTITUTION.md:104`](V2-CONSTITUTION.md)

| Material | Physical cause | Where it may appear | Where it may not |
|---|---|---|---|
| **Paper** | the sheet | the page, page thumbnails, the imposed sheet, covers | any tool, any chrome |
| **Ink** | a drawn line — every border, outline, stroke ([`V21-SPEC.md:478-481`](V21-SPEC.md)) | everywhere a line is drawn | as a *shadow* — shadows are `inkLine` |
| **Tape** (butter) | it is holding something down | attaching a band, a cover, a label | as an action colour ([`V21-SPEC.md:326`](V21-SPEC.md) — "material only") |
| **Stamp** (butter/berry) | pressed with an inked die | size labels, counts, page marks | as a control |
| **Room** (desk/bench) | the surface everything rests on | all chrome, all tools | over the artifact |

**Five. Not six.** Do not add cardboard, glue, staples, or pencil. The reference research is unambiguous that a
*bounded* kit reads as a studio and a large one reads as a theme store.

Riso grain stays permitted under the existing rule — `soft-light` over chrome, `multiply` over paper
([`V21-SPEC.md:513`](V21-SPEC.md)) — with the physical-research warning attached: **grain is per-ink-layer and
exists only where ink is.** A global grain PNG dirties white paper, which real riso leaves clean.

## E.2 The line alphabet — the biggest under-used asset in the product

`ProofFoldAct` already ships a **legend** in which line style carries meaning: `crease` (dashed ink) · `fold now`
(solid leaf) · `cut` (dashed jam) · `move` (solid cream) · `push or pull` (double line). It is the most deliberate
piece of visual language in the app, and it is **buried inside a modal drawer at the very end of the flow.**

Meanwhile the Bench independently arrived at the same idea: the selection outline is dashed ink, the Reframe
boundary is dashed ink, the keep-clear warning is jam. Two surfaces invented the same alphabet without knowing it.

**Promote it to a global law:**

| Line | Means | Already used by |
|---|---|---|
| **Dashed ink** | a boundary you made, or one you must respect | selection outline · Reframe boundary · crease · the `COVER · 1 OF 8` tag |
| **Dashed jam** | a cut, or a limit you are crossing | fold legend "cut" · keep-clear warning (OD-48) |
| **Solid leaf** | do this now | fold legend "fold now" · the primary action |
| **Solid cream/ink** | a thing that moves | fold legend "move" · nudge arrows |
| **Hairline** | an internal division, no meaning | dividers ([`V21-SPEC.md:328`](V21-SPEC.md)) |

Once declared, dashes stop being decoration and start being readable — and the swatch's dashed selection ring
(§F) has to justify itself against it.

## E.3 Shapes and depth — already good, keep

Unchanged from [`V21-SPEC.md`](V21-SPEC.md), and it is genuinely well-derived:

- **Hard shadow:** offset, **zero blur**, always down-right, full `inkLine`. *"It is a printed shadow, not elevation: the object physically moves under your finger"* (`:447-449`).
- **Four press tiers** — Hero / Raised / Flat / Inline. *Do not add a tier by interpolation.*
- **`--frame` ring:** 5px flat colour outside the hard shadow, **one per screen**, reserved for the primary action, justified as riso misregistration (`:490-491`).
- **Radii:** pills for controls, `--br-md`/`--br-lg` for cards, `--br-xl` for sheet tops.
- **Tilt:** *"objects at rest sit at ±0.6–2°. Objects being worked on never tilt"* (`:494`).

**That last one is the best world-logic in the product and nobody has promoted it.** On device it is visibly
true: the Shelf cover tilts, the Proof page tilts, the Bench page is dead square. The user is never told this and
does not need to be — it simply reads correctly. **Make it a global law** (§F).

## E.4 Typography

Three roles, already constitutional ([`V21-SPEC.md:436-443`](V21-SPEC.md)): **Voice** (Averia Sans Libre) for
headings and the maker's own words · **Editorial** (Fraunces) for zine body and guide prose · **Work** (Inter) for
every control, label, chip and number. The binding rule — *"the imperfect face never sets running text"* — is
constitutional text and stays.

**The gap, corrected after review.** V21-SPEC *is* **SILENT** on casing, tracking and a size ramp — verified, zero
hits. But my inference from that silence was wrong. I claimed the Type bar "invented" uppercase tracked labels and
that `ProofScreen` invented them again by coincidence. In fact **a shared style already exists in `core:ui`**:

> *"An all-caps section label — **the one genuinely recurring chrome pattern, appearing six times** across the
> Bench and Proof at 10–11sp, weight 600–700, with wide positive tracking (`.12em`–`.13em`)."*
> — [`ZinelyV2Typography.kt:154-168`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Typography.kt), `val sectionLabel`

So it is a **counted corpus pattern with an implementation**, not an accident. Two real findings survive, and they
are better than the one I thought I had:

1. **`TypeBar.kt:486` hardcodes `9.6.sp` / `0.13.em` instead of using `sectionLabel`** — as does `ProofScreen.kt:797`
   at a *different* tracking (`0.08.em`). There are now **seven distinct tracking values** across `src/main`.
2. **The pattern is implemented six times and declared zero times.** V21-SPEC's silence is the gap, not the usage.

**Recommendation, unchanged in substance:** declare it — *uppercase + tracked = a stamped label, a name pressed
onto a thing, not a sentence; never more than three words* — and then **consolidate the call sites onto
`sectionLabel`.** Declaring converts a convention into a grammar; consolidating stops it drifting into seven.

## E.5 Colour — roles, and the two that are broken

The role law is already declared ([`V21-SPEC.md:142-157`](V21-SPEC.md)) and is sound:

| Role | Token | Law |
|---|---|---|
| The thing you are making | `paper` | **never dims** (ADR-090) |
| Where you are standing | `desk` / `bench` | dims with theme |
| Every drawn line | `ink` | borders, outlines, strokes |
| Every shadow | `inkLine` | shadows only |
| **Your next move** | `leaf` | **the one action colour** |
| **Where you are now** | `berry` | "punctuation, current-page" — **never an action** |
| Urgent / destructive | `jam` | the only urgent colour |
| Material | `butter` | tape, stamps, rings — **never an action, never text** |

**Two live violations found on device:**

1. **`BenchPageGrid` marks the current page in `leaf`**, while `BenchPageNav` (one inch below it) and
   `ProofFoldAct`'s step dots both mark current in `berry`. Three surfaces, one concept, two colours — and the
   grid is the one breaking the declared law, because it spends the *action* colour on a *state*.
2. ~~**`ZineActionSheet`'s Delete renders pink-red**, in a role the law reserves for `jam`.~~ **WITHDRAWN — I was
   wrong.** `ZineActionSheet.kt:475` and `:509` both read `if (action.danger) colors.jamText else …`. The
   destructive ink **is** jam, using the token V21-SPEC:324 added for exactly this (*"jam as text or icon"*). The
   only berry is the icon-chip *ground* at `:498`, transcribed from frozen CSS quoted at `:462`. The narrow
   surviving question — a berry chip behind a jam glyph — is already recorded in the file's own header at
   `:77-82` (*"Delete is now separated only by colour … Transcribed as frozen, and recorded"*), and amending it
   means amending a frozen surface. Not a defect; an owner call that is already logged.

**Also owner-owed and still open:** butter's allow-list disagrees with itself between `V21-SPEC.md` §3.2 and §4.1
(`:422-427`), and "stamps" appears in *both* the butter and berry rows.

**The named-ink principle.** The editor already offers exactly five inks, not a colour wheel. That is the single
most "small press" decision in the product and it happened quietly. **Declare it:** *Zinely offers inks, not
colours. Inks have names. The set is finite.* Then fix the obvious wound — one of them is named **"Coral"**, in a
palette that abolished coral.

## E.6 Texture — where it belongs

Permitted: riso grain per layer · the `--frame` misregistration ring · paper edge.
**Forbidden:** global grain overlays · wood grain · coffee stains · deckle edges · torn paper · fake staples ·
any texture over the user's photograph.

The last one is a rule the Reframe work already discovered the hard way and should be written down: **the user's
photograph is not our surface.** Guides drawn over it do not follow our theme, because the photograph does not
([`ResizeHandles.kt:362`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ResizeHandles.kt) is the existing precedent).

## E.7 Motion — the weakest declared area in the corpus

What exists: *"short, `cubic-bezier(.05,.7,.1,1)` for entrances, ≤300 ms"* ([`V21-SPEC.md:514-515`](V21-SPEC.md)),
paper-settle ~300ms, and a constitutional ban on motion-for-spectacle. **SILENT on exits, stagger, springs.** And
`DESIGN-LANGUAGE.md:208-213` still permits overshoot that `V2-BENCH-PRINCIPLES.md:143` forbids — unreconciled.

**Proposed physical logic.** Every motion must name the physical cause. Four causes, and nothing else ships:

| Cause | Where | Behaviour |
|---|---|---|
| **Something was set down** | page settle, element drop, sheet close | decelerate, ~300ms, **no bounce** — paper is damped, not rubber |
| **Something was pressed** | every button | the hard-shadow press: object moves down-right under the finger, ≤100ms, instant return |
| **A drawer was pulled** | sheets, popovers, panels | translate from the edge it belongs to, 200–250ms |
| **A mark was made** | selection, guides, boundaries | the mark **appears drawn**, not faded — ≤100ms, no opacity ramp |

The fourth is new and is the one worth having. A selection outline that fades in reads as software; one that
appears at once reads as a pencil line. It is also cheaper.

**Explicitly banned by physical cause:** parallax (nothing in a press has depth-of-field) · floating/idle loops
(nothing on a bench drifts) · spring overshoot (paper does not bounce) · cross-fades between screens (you do not
dissolve between rooms — you carry something, or you walk).

## E.8 Interaction personality

From the physical research, the gestures that genuinely survive a phone:

- **Press-and-lift is the stamp.** Tap is the phone's native verb and press-lift is the stamp's native verb — *they are the same gesture.* Any "apply a mark" action should feel stamped, with per-impression variance (seeded per element, never re-rolled).
- **Tools live in fixed positions at thumb reach.** A printmaker reaches without looking; that requires the ink tool to always be in the same place. This is already true of the Bench bar and must never be violated for layout convenience.
- **The surface is never cleared.** Reopen and everything is exactly as left. Pure state management, zero pixels.
- **Placement is reversible until you commit.** The paste-up's core act is *deferral* — scraps sit loose on the desk before anything is glued. Digital canvases usually force everything to land somewhere.

**Explicitly does not survive:** paper texture/weight/tooth (a 3mm simulation nobody perceives, costing contrast) ·
enforced drying time · absolute physical scale · scalpel-precision cutting (a fingertip is ~9mm) · the full studio
spread · degraded default rendering (it conflicts with AA, the exact blocker already open on this branch) ·
irreversibility (real paste-up gets one copy; removing undo is not authenticity, it is hostility).

## E.9 Microcopy

VOICE.md is good and is being violated. Confirmed live violations, each with the rule it breaks:

| Shipped | Should be | Rule |
|---|---|---|
| `"Share & export"` ([`ZineActionSheet.kt:97`](../../feature/library/src/main/kotlin/com/aritr/zinely/library/ZineActionSheet.kt)) | "Print & fold" / "Send to a friend" | L47, L73, L74 — verbs, not nouns |
| `"Save PDF"` / `"Share"` (`Copy.kt:658-659`) | same | L73, L74 |
| `"Couldn't open this project."` | "zine", not "project"; name the way out | L45, L48, L162 |
| `"This zine is a bit big to render right now."` | no jargon — "render" | L45, L48 |
| `"Text"` in the Add chooser (`Copy.kt:239`) | "Add words" | L47, L68 |
| `"Preview ›"` | a verb | L47 |
| `"Colour"` (`Copy.kt:448`) vs `"Ink"` (`Copy.kt:187`) | one name per concept — and per §E.5 it is **Ink** | L45 |

**And one worse than a violation.** The empty page says *"Grab a photo or a few words from **the supplies
below**."* There is nothing called supplies. The bar says "Add"; the sheet says "Add to your page". A "Supply
tray" was specified in [`SCREEN-INVENTORY.md:112`](SCREEN-INVENTORY.md) and never built. **The empty state is
speaking to a screen that does not exist** — and it is the first sentence a new user reads on a blank page.

**The missing voice surface: a colophon.** Provenance was the single most-recurring principle across all 35
references, and Zinely has none. A zine's back page traditionally says who made it, on what, in what edition. An
exported Zinely PDF says nothing. *"Made on a phone, with Zinely"* — one optional line, off by default, and it
would be the most on-brand feature in the backlog.

---

# F. Global vs local

### GLOBAL — must be true everywhere

1. **Light is the thing you are making. Dark is everywhere you are standing.** No third case. (ADR-090, sharpened — see [Ruling A](#ruling-a--the-two-palette-split))
2. **Tilt means at rest. Square means in your hands.** ±0.6–2° at rest, exactly 0° when being worked on.
3. **Shadows are printed, not elevated** — zero blur, down-right, `inkLine`, and the object moves under the finger.
4. **The line alphabet** (§E.2) — dashed ink is a boundary, dashed jam is a cut, solid leaf is your next move.
5. **`berry` marks where you are. `leaf` marks what you can do. Never swap them.**
6. **One `--frame` ring per screen**, on the one primary action.
7. **Inks are named and finite.** No colour wheel, anywhere.
8. **Every motion names a physical cause** (§E.7), or it does not ship.
9. **The handmade lives in content, type, copy and quantified imperfection — never in controls, targets, focus, or icons.**
10. **Uppercase + tracked = a stamped label.** Never more than three words. *(New — requires declaring, §E.4)*

### LOCAL — correct where it is, must not spread

- **The dashed ring on a selected swatch** (TypeBar). Fine there; if dashes start meaning "selected" as well as "boundary", the alphabet in §E.2 collapses. **Watch this one.**
- **The rule-of-thirds grid** — see [Ruling B](#ruling-b--the-rule-of-thirds-grid). Remove, do not spread.
- **The fold legend's five line styles** — the full legend is local to Fold; the *alphabet* it teaches is global.
- **`ProofFoldAct`'s numbered sheet diagram** — an instructional illustration, not a UI pattern.

### DIVERGENT — Compose disagrees with the frozen spec (a parity defect, not a design question)

- **The Type bar's light palette**, and **`BenchSnack`'s**. Both sit inside the island without the opt-out that `BenchContextBar` already uses. The spec says bench. ([Ruling A](#ruling-a--the-two-palette-split))
- **The rule-of-thirds grid** — in Compose, in no spec. ([Ruling B](#ruling-b--the-rule-of-thirds-grid))

### FROZEN BUT QUESTIONABLE — transcribed faithfully; changing it needs an owner amendment

- **`BenchPageGrid`'s leaf current-page.** ⚠️ *Corrected: this is not accidental.* It transcribes `v21-bench.html:444` — `.pgc.on{background:var(--leaf-tint)}` — inside a recorded amendment. The role conflict with `BenchPageNav` (`berry`) and `ProofFold` (`berry`) is real, but the fix amends a frozen file and breaks `BenchC5Test.kt:858` plus a golden. **Not trivial.**
- **`BenchPageGrid` drawing no page content.** Follows the `.pgcell` ruling; reads to a user as *"my pages are gone."*
- **The rounded-square icon tile** in `ZineActionSheet` / `BenchAddChooser` — Material's list-item grammar wearing our colours.
- **The `⋮` kebab** on the shelf card — a foreign object in a room with no other Android furniture.

### ~~ACCIDENTAL~~ — the list I got wrong

⚠️ **My first draft filed the 8-glyph nudge row here as *"bare butter arrows, no containers, no labels, no grammar
shared with anything else."* Every clause of that is false, and the review caught it:**

- **The labels exist** — `EditorContextBar.kt:167-170` carries `Copy.A11y.MOVE_LEFT/RIGHT/UP/DOWN`, and my own device dump confirms all eight announce correctly.
- **The grammar is shared** — `:98`: *"They are the same verbs `.ctx` declares."*
- **The butter is reasoned** — `:103-104`: *"It is legal butter — a material tint marking a touch, never a state on its own."*
- **And it is a conformance path.** OD-11 ([`DECISIONS.md:3006`](../DECISIONS.md)) ruled it exists *"to satisfy **WCAG 2.5.7** (single-pointer alternative to dragging)"*, and that *"a parity phase does not remove or weaken a conformance path."*

The row has a real defect — `Bring forward` is 10px wide and reports `enabled=true, clickable=false` — but that is
a clipping bug in one control, not an absent grammar. **See [J10](#j-implementation-plan), which I have rewritten.**

### MISSING — the world has holes

- **No provenance anywhere.** (§E.9)
- **No sound, and haptics only arrived after fourteen Bench controls were found silent** ([`DECISIONS.md:9638`](../DECISIONS.md)).
- **No motion physics for exits or stagger.** (§E.7)
- **No "supplies"**, though the empty state promises them. (§E.9)
- **Read, Fold, first-run and settings have no prototype at all** ([`V21-SPEC.md:57-58`](V21-SPEC.md)).

---

# G. Experience principles

Ten. Each is falsifiable — you can point at a screen and say it fails.

1. **The artifact never dims.** The room may go dark; the thing you are making never does. If a surface represents printed matter, it is lit.
2. **Every screen is a place, and you can name it.** Bench, Proof, or Shelf. A surface you cannot place is mis-homed.
3. **Objects at rest tilt. Objects in your hands are square.**
4. **A mark is drawn, not faded.** Selections, boundaries and guides appear at once, like a pencil line.
5. **Tools are picked up; they are never part of the paper.** A tool wears the room, always, so you can always tell the work from the thing you are working with.
6. **The material is finite and named.** Five inks, five materials, three typefaces. Constraint is what gives the output a house style.
7. **Nothing is cleared.** Reopen and the bench is exactly as you left it — including the mess.
8. **Every motion names its physical cause,** or it is deleted. There is no such thing as a nice animation.
9. **Personality lives in words, type and paper — never in the controls.** A control's job is to be hit, understood, and announced.
10. **The work leaves the building.** The product's north star is a verb: *finishing*. Every screen should be legible as a step toward something coming off the press.

---

# H. Screen-by-screen audit

Device: **SM-A176B, Android 16, dark theme, `zinely-0.9.0-beta.1-debug`, 2026-08-15.** Screenshots in the session
scratchpad. Full surface inventory (43 surfaces) available; the majors are audited here.

### Shelf — `ZineLibraryScreen`
**Is:** the rack of finished copies. **Working:** the tilted taped cover with the stamped `US LETTER` label
straddling its edge is the most complete piece of world-building in the product — tilt, tape and stamp all doing
real jobs. The butter underline swash on "Your shelf". The leaf pill with butter ring.
**Inconsistent:** the `⋮` kebab is Android furniture in a handmade room. With one zine, ~60% of the screen is void
— a shelf holding one thing should read as *a shelf with room*, not as emptiness; nothing indicates the rack
continues. **Direction:** replace the kebab with a world object (a tab, a corner turn, long-press); give the rack
a visible extent; adopt Zineopolis's metadata vocabulary — fold, size, page count — over "Edited 11 hours ago".

### `ZineActionSheet`
**Is:** picking a zine up to decide what to do with it. **Working:** dark = room = correct. The dashed divider.
`"Open on the bench"` is perfect world-voice.
**Inconsistent:** four of five rows speak Android (`Share & export`, `Rename`, `Duplicate`, `Delete`) and one
speaks Zinely. Delete is pink-red where the law says `jam` (§E.5). The icon tiles are Material's grammar.
**Direction:** one voice for all five rows; Delete → `jam`; retire the tile.

### Bench — `EditorScreen`
**Is:** the lit work surface. **Working:** the page is lit and square while the room is dark — the world's
central idea, working. `1 / 8` printed on the paper itself. Dashed-ink selection with square handles.
**Inconsistent, and this is the worst screen in the app for it:**
- **Four rows of chrome stack below the page** — context pill, page strip, action row, nudge row. The page gets ~55% of the screen. For a screen whose own principle is *"the page is the hero; the tool is a guest"* ([`V2-BENCH-PRINCIPLES.md:21`](V2-BENCH-PRINCIPLES.md)), that is a lot of guests.
- **Three button grammars in one row**: ghost circle (undo/redo), filled pill (Add), outline circle (done).
- **The nudge row** — eight unlabelled butter arrows with no container, matching nothing.
- **`Preview ›`** is a bare text link in `leaf`, i.e. the action colour spent on navigation.
- **The Type bar renders light** and the context bar directly above it renders dark. ([Ruling A](#ruling-a--the-two-palette-split))
**Direction:** collapse the chrome rows; one button grammar per hierarchy level; give the nudge row a container and labels or delete it; `Preview` becomes a verb.

### `BenchPageGrid`
**Is:** all pages laid out at once. **Not working:** it draws **no page content** — eight blank cream cards with
numbers, while the strip below shows real thumbnails. It is a numbered list wearing card clothes, and the
`.pgcell`-draws-no-content decision ([`V2-SPEC-DEFECTS.md:3340`](V2-SPEC-DEFECTS.md)) reads to a user as *"my
pages are gone."* Current page is `leaf`, contradicting §E.5 and both neighbours.
**Direction:** draw the pages. Current page → `berry`.

### Proof — `ProofScreen`
**Is:** the checking table. **This is the most coherent screen in the app and should be the reference for
the others.** The page tilts because it is at rest. The `COVER · 1 OF 8` dashed tag is a stamped label. The butter
tape strip physically attaches the commit band. `"Ready when you are"` in Fraunces with a leaf tick. `"8 pages ·
one sheet, one cut · US Letter · stays on your phone"` — concrete, honest, world-voiced. `Save PDF` carries the
screen's one `--frame` ring; `Share` is correctly demoted.
**Inconsistent:** `Save PDF` / `Share` violate VOICE.md's own naming (L73–74). The `›` page chevron is a bare
glyph, same weak grammar as the nudge row.

### `ProofFoldAct`
**Is:** folding instructions. **Working:** the line legend (§E.2) — the most deliberate visual language in the
product. Berry step dots. Fraunces instruction with a leaf confirmation line.
**Not working — and Ruling A finds this one:** **the imposed sheet diagram is drawn dark-on-dark.** It is the most
literal artifact in the entire app — it *is* the printed sheet — and it renders brown while the user is holding a
white one. Under ADR-090 this is a violation.
**Direction:** the fold diagram is paper. Light, in both themes.

### Empty page — `EditorEmptyState`
**Is:** an empty bench. **Working:** the best empty state in the app — three little stamp tiles in paper/leaf/berry
with hand-drawn marks, `"A fresh page. What goes here?"`, and a chevron pointing at the tools.
**Not working:** it says *"the supplies below"* and nothing is called supplies (§E.9).

### `BenchAddChooser`
**Is:** reaching for supplies. **Inconsistent:** `"Text"` / `"Photo"` are nouns where VOICE.md mandates
`"Add words"` / `"Add a photo"`. Same Material icon tile as the action sheet.
**Working:** `"From your phone — it never leaves the device"` is exactly right.

### `BootLoading` / `BootFailure`
**Is:** nothing. Full-window states in no room, in no design document. **Direction:** fold into the Shelf→Bench
transition — fetching a zine off the shelf.

---

# I. How future features enter this world

**The rule, in one sentence:**

> **A new feature must be an object or an action that already exists in a one-person press. If it is not, it does
> not get a new visual language — it gets redesigned until it is one, or it does not ship.**

Four questions, all four must be answered before any pixels:

1. **Where does it live?** Bench, Proof, or Shelf. "Its own screen" is not an answer.
2. **What is it made of?** One of the five materials (§E.1). Not a sixth.
3. **What causes its motion?** One of the four causes (§E.7). Not a fifth.
4. **What does it take away?** The subtraction test already in [`V2-BENCH-PRINCIPLES.md:72-74`](V2-BENCH-PRINCIPLES.md).

**And the anti-drift clause:** a feature may not introduce a colour role, a line style, a press tier, a typeface,
or a motion curve. If it needs one, that is a change to *this* document and to [`V21-SPEC.md`](V21-SPEC.md) first —
which makes it a decision with an ADR, not a feature with a stylesheet.

Worked example — **stickers**, which are queued: they live on the Bench; they are made of *paper and ink* (a
printed thing you place), not a sixth material; their motion is *something was set down*; and they take away the
need to draw. They stamp — press-and-lift with per-impression variance. They pass, and they need no new grammar.

---

# The three rulings

## Ruling A — the two-palette split

### **KEEP the principle. MODIFY the boundary. The current split is drawn in the wrong place.**

**Is it meaningful?** Yes, and it is already law — ADR-090 / OD-12: *"The editor represents the physical printed
artifact… The artifact itself does not dim; the room around it may."*

**Does it reinforce the metaphor?** It *is* the metaphor. Light-thing-on-dark-surface is what a lit work bench
looks like, and it is the reason the Bench reads as a place on the device rather than as a screen.

**Does it create hierarchy?** Yes, and a rare one: it separates **the work** from **everything that is not the
work**, which is the single most useful distinction a creative tool can draw.

**Is it reusable?** Completely. It answers "what palette?" for every future surface with one question: *is this
the thing being made, or the place you are standing?*

**So what is wrong? — corrected after review. This is not an undecided design question; it is a parity defect.**

My first draft of this ruling said *"nothing chose the difference — a brace did."* **That was wrong, and the
frozen corpus falsifies it three times over:**

1. **The membership rule is declared, and deliberately a subtree rule.** ADR-102 §12.1 ([`DECISIONS.md:9137-9138`](../DECISIONS.md)):
   *"the island is a property of the **subtree**, not a list of today's call sites: a future mark that reaches for
   `leaf` on paper must get the lit value without anyone having to remember this table."*
2. **A test already asserts it** — `BenchStudioSurfaceTest.kt:163`, *"the Compose island lights exactly the tokens
   the frozen page paints"*, deriving the expected set from the frozen file.
3. **Opting out is a documented mechanism, already in use.** [`EditorScreen.kt:1283-1297`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt)
   provides the room palette back to `BenchContextBar` and `BenchInkPopover`, with the reason recorded: *"it is NOT
   part of the sheet, so it must not inherit the sheet's palette … measured at 1.05:1 on a device, an invisible
   toolbar that every Robolectric test passed."*

**And the spec already ruled on the Type bar specifically.** [`v21-typebar.html:698-700`](mockups/v21-typebar.html):

> "this card floats on the **bench** and not inside the sheet island ([ADR-102 §12.1](../DECISIONS.md)), which is
> what keeps page text on light paper in both themes"

The markup agrees structurally — `.page` closes at `:388`, `.canvasArea` at `:389`, and `.typebar` opens at
`:392` as a **sibling**. Its `background:var(--paper)` therefore resolves to the *room's* paper, `#332B22` in dark.
The file even measured the five ink fills against that dark ground and called it *"the right ground"*.

> **So: the spec says bench. Compose says island. That is Compose diverging from a frozen specification —
> a parity defect with an owner already on record, not a decision anybody owes.**

**The corrected defect list** — three surfaces are inside the island and forgot the opt-out:

| Surface | In the island? | Should be | Status |
|---|---|---|---|
| The page · page thumbnails | ✅ | paper | **correct** |
| `ReframeControls` | ❌ outside | room | **correct** |
| `TypeBar` ([`EditorScreen.kt:1380`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt)) | ✅ inside, no opt-out | **room** (spec: `v21-typebar.html:392`) | **parity defect** |
| `BenchSnack` (`EditorScreen.kt:1407`) | ✅ inside, **after** the opt-out provider closes at `:1365` | **room** — §D maps it as *"a note slid over"* | **parity defect, missed by my first draft** |
| `ProofFoldAct`'s sheet diagram | ❌ | **paper** | **deferred, not unnoticed** — OD-12 says *"`v2-proof.html` … is NOT amended here … it is Phase D's to amend, under this ruling"* ([`V2-SPEC-DEFECTS.md:4389`](V2-SPEC-DEFECTS.md)) |

**Also corrected:** there is not one island. `benchGridCardIsland` lights six tokens
([`BenchPageGrid.kt:231-241`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/BenchPageGrid.kt)),
and `benchThumbIsland` lights five *"under its own owner ruling **OD-23**, after a dark-theme thumb measured
**1.21:1** on hardware"* ([`DECISIONS.md:8841-8842`](../DECISIONS.md)). A binary in/out table cannot express this.

**The sharpened law still stands, and it should be global principle #1:**

> **Light is the thing you are making. Dark is everywhere you are standing. There is no third case.**
> A tool is never paper — however close to the paper it floats.

**What this changes:** `TypeBar` and `BenchSnack` take the existing `CompositionLocalProvider` opt-out — the same
two lines `BenchContextBar` already uses. Not a re-skin, not a redesign: **a missing opt-out.** `ReframeControls`
untouched. The Proof diagram stays Phase D's.

**Cost, corrected:** far lower than I first said. The contrast question is largely *already answered* —
`v21-typebar.html:698-702` measured the five ink fills against `#332B22`. What still needs measuring is the card's
**chrome** tokens on `bench`, because `inkFaint` strokes clear 1.4.11 by only 0.04 and *"any of them moving onto
`bench` fails"* ([`V21-SPEC.md:370-373`](V21-SPEC.md)). Plus two golden re-records.

---

## Ruling B — the rule-of-thirds grid

### **REMOVE.**

Five independent reasons, any two of which would be enough:

1. **It is undeclared.** V21-SPEC specifies no thirds grid. It entered as an implementation detail.
2. **It is the wrong craft.** Thirds is a *photographic* convention — a camera viewfinder overlay. It belongs to no printmaking, paste-up, riso or bookbinding tradition. A printmaker's guides are the **registration mark** and the **trim line**.
3. **It fails the corpus's own texture law.** *"If a texture cannot name its physical cause, it does not ship."* A thirds grid cannot name a physical cause on a paste-up table.
4. **It was invisible on device** over the test photograph — so it is not even paying for itself as a feature.
5. **It competes with the mark that matters.** The Reframe overlay's job is to show you what you are cutting. Adding a compositional grid over the user's photograph adds a second, weaker mark that dilutes the first.

**Scope, corrected after review: this is a Compose-only removal.** `v21-reframe.html` contains no thirds grid at
all (verified — zero matches over 362 lines), so there is no spec to amend. The guides exist solely at
[`ReframeOverlay.kt:203`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ReframeOverlay.kt),
whose own comment already flags them: *"⚠ OPEN (owner call): the V2.1 Reframe spec has no thirds grid."* Reason #1
above ("undeclared") is therefore not merely true — **the code has been saying so all along.**

**And there is a world-correct replacement**, which is a *separate later decision, not part of this ruling:* the
thing a user actually loses money and paper on is content falling outside the printer's reach. `berry` is already
documented as *"the printer's-reach guide"* ([`V21-SPEC.md:322`](V21-SPEC.md)) and the keep-clear boundary already
exists from ADR-102 P2. **Showing where the unprintable margin falls during a crop is a real job. Showing thirds
is a photography habit.**

---

## Ruling C — the HTML freezes

### **FREEZE BOTH. — This ruling is reversed from my first draft. All three of its blockers were false.**

I originally said "do not freeze yet." The Review Agent falsified every stated reason, and I verified each
correction directly. The corrected position is the opposite, and it is *better for the work*:

| My claimed blocker | What the file actually says |
|---|---|
| *"`v21-typebar.html` specifies the card in the light island palette"* | **False.** `:392` places `.typebar` as a **sibling** of `.page` (which closes at `:388`), and `:698-700` says in prose that the card *"floats on the **bench** and not inside the sheet island."* The spec already says what Ruling A concluded. |
| *"`v21-reframe.html` contains the rule-of-thirds guides"* | **False.** `grep -i "third\|rule-of\|composition"` over all 362 lines returns **zero matches**. The thirds exist only in Compose, at `ReframeOverlay.kt:203`, whose own comment already says *"the V2.1 Reframe spec has no thirds grid."* |
| *"Both invent uppercase tracked labels V21-SPEC is silent on"* | **Half false.** V21-SPEC *is* silent (verified — zero hits for casing/tracking/ramp). But the pattern is not an invention: [`ZinelyV2Typography.kt:154`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Typography.kt) already ships a shared `sectionLabel` described as *"the one genuinely recurring chrome pattern, **appearing six times** across the Bench and Proof."* |

**And the inversion that matters most:** CLAUDE.md's DESIGN FREEZE permits *"implementation parity fixes"* after
freeze, and forbids *visual redesign*. Because `v21-typebar.html` already specifies the bench, moving the Compose
Type bar out of the island **is a parity fix** — which means:

> **Freezing `v21-typebar.html` is what makes the Type bar fix legal. Refusing to freeze it is what blocks it.**

I had the workflow exactly backwards: I was withholding the freeze to protect a change that the freeze *authorises*.

**Freeze both.** `v21-reframe.html` has no blocker at all; Ruling B is a Compose-only removal that needs no spec
amendment. The one genuine follow-up is not a freeze question: **`TypeBar.kt:486` hardcodes `9.6.sp`/`0.13.em`
instead of using the shared `sectionLabel`** — and there are now seven distinct tracking values across `src/main`.
That is a consolidation task and a candidate V21-SPEC declaration, not a reason to hold two specs open.

**One caution survives intact.** "Tests green" was never evidence for either freeze — the Type bar's suite passed
*while a tap was falling through the card and silently disarming every 48dp target on it*. Green tests prove the
code does what the code says. And a spec being frozen is not evidence either: this ruling was wrong for two days
because I trusted my summary of a file instead of opening it.

---

# J. Implementation plan

**Nothing below is started. All of it waits on your approval of §C (the world) and the three rulings.**

### Now — if the direction is approved
| # | Work | Why now | Risk |
|---|---|---|---|
| J1 | **Measure Type bar *chrome* contrast on `bench`** | Gates J4. `inkFaint` clears AA by 0.04 and *"any of them moving onto `bench` fails"*. The five **ink fills** are already measured at `v21-typebar.html:698-702` | none — measurement only |
| J2 | **Freeze `v21-typebar.html` and `v21-reframe.html`** | Reversed from my first draft — the freeze is what makes J4 a permitted parity fix ([Ruling C](#ruling-c--the-html-freezes)) | low |
| J3 | Declare in `V21-SPEC.md`: the **line alphabet** (§E.2), the **stamped-label rule** (§E.4), the **tilt law** (§E.3), the **four motion causes** (§E.7) | Three conventions and one shipped-but-undeclared style become grammar | low — documentation |
| J4 | Give `TypeBar` and `BenchSnack` the **existing** `CompositionLocalProvider` opt-out; delete thirds from `ReframeOverlay` | Parity fix, authorised by J2. Two lines each — the ones `BenchContextBar` already uses | low |
| J5 | Consolidate `TypeBar.kt:486` and `ProofScreen.kt:797` onto `ZinelyV2Typography.sectionLabel` | Seven tracking values in `src/main`; one declared style | low |
| ~~J6~~ | ~~`BenchSheetIsland` gets a membership rule + a test~~ | **CUT — both already exist.** ADR-102 §12.1 declares the subtree rule; `BenchStudioSurfaceTest.kt:163` asserts it | — |

### Next — cheap, high-return, independent of the rulings
| # | Work |
|---|---|
| J7 | Voice pass on the **8** confirmed violations (§E.9), including `"the supplies below"` *(count corrected from 14)* |
| J8 | `BenchPageGrid` draws actual page content |
| J9 | Fix `Bring forward`: 10px wide, and `enabled=true` while `clickable=false`. **⚠️ Corrected from my first draft — this does NOT touch the rest of the nudge row.** OD-11 ruled the row is a WCAG 2.5.7 single-pointer conformance path, and *"a parity phase does not remove or weaken a conformance path."* Deleting it was never an option and I should not have offered it |
| J10 | Owner call, then amendment: `BenchPageGrid` current-page `leaf` → `berry`. **Not trivial** — amends frozen `v21-bench.html:444`, breaks `BenchC5Test.kt:858`, needs a golden re-record |
| J11 | `ProofFoldAct`'s sheet diagram becomes light paper — **already scheduled**: OD-12 assigns `v2-proof.html` to Phase D (`V2-SPEC-DEFECTS.md:4389`), so this is sequencing, not a new finding |

### Later — needs its own design cycle, do not start on a hunch
`BootLoading`/`BootFailure` folded into the Shelf→Bench transition · replacing the kebab · the Bench chrome-row
collapse · the keep-clear guide during Reframe · the colophon · shelf metadata vocabulary · the stamp gesture.

### Shared primitives vs component-local
**Shared** (`core:ui`): the line alphabet as drawing helpers · the stamped-label text style · the tilt modifier ·
the four motion specs · the island membership rule.
**Component-local:** the swatch's dashed ring · the fold legend · the Reframe boundary geometry · the numbered
sheet diagram.

### To document
`V2-CONSTITUTION.md` amendment 2 (the press as spatial metaphor, café retained as register) · `DESIGN-LANGUAGE.md`
marked superseded · a new ADR for Ruling A's sharpened boundary · `V21-SPEC.md` §J3 additions · this file
referenced from `README.md`'s doc index.

### To verify on device
Both passes on the re-skinned Type bar · contrast sweep on `bench` grounds · **Pass 2 on the Bench specifically
asking whether a first-timer can name what the eight arrows do** · the fold diagram against an actual printed
sheet.

---

## What I am least sure about

Three places where I could be wrong, stated plainly:

1. **The press metaphor could be over-reach.** The constitution ranks "quiet café" above everything, and I am proposing to demote it to a register. If you read the café as load-bearing, Ruling A still stands on ADR-090 alone — it does not depend on §C.
2. **Ruling B removes something a photographer would miss.** Thirds is genuinely useful for framing a photo, and I am arguing craft-consistency over a real utility. If you disagree, the honest compromise is *thirds during Reframe only, off by default* — but I would rather ship the printer's-reach guide.
3. **The Bench chrome-row collapse (J-later) is the biggest unproven claim here.** I am confident four rows is too many; I have not designed the three-row version, and it could easily be worse.
