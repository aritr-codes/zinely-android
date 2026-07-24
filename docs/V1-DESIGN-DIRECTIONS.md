# Zinely V1 — Design Directions

> **Status:** design exploration · 2026-07-22 · **a proposal under review, not a source of truth.**
> This document invents; it does not decide. Nothing here is implementable until one direction is
> chosen and the choice is recorded as an ADR in [DECISIONS.md](DECISIONS.md).
>
> **Companion to** [V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md) (the approved critique — what is
> wrong). This document asks the next question: **what should Zinely be instead?**
>
> No code. No HTML. No Compose. No screen redesigns. No tickets. Three whole product identities, a
> comparison, one recommendation, and ten principles meant to outlive all three.
>
> **Revision note.** This is the **third draft**. Draft 1 recommended C and was returned **NO-GO** by
> independent review (constitutional overreach, a silently renamed contestant, an uncited evidence
> base, an unaddressed ✅ VERIFIED caution aimed at the winner, circular scoring). Draft 2 recommended
> B and was returned **GO WITH FIXES — not ratifiable**, because two of *its* reasons over-claimed in
> exactly the same way. Draft 3 splits the identity between the tool and the artifact, which is what
> the evidence supported all along. **The recommendation moved twice**; §"How the review changed the
> answer" records every reason that failed and why, including the standing objection to the outcome.

---

## What is fixed, and what is not

The brief was *"the product constraints remain; the implementation does not."* Naming the line
precisely, because a direction that crosses it is dead on arrival and should not be evaluated on charm:

**Fixed — [the constitution](zinely-constitution.md), which no design direction may amend:**

| | The constraint | What it forbids a direction from doing |
|---|---|---|
| North star | **Finishing** — *"the finish rate — the fraction of started zines that reach a finished state — **and the return rate to start another**"* ([§II](zinely-constitution.md#ii-the-north-star)) | Any identity optimised for dwelling rather than for finishing **and coming back** |
| Art. 1 | Constraints are a gift | Infinite canvas, unbounded pages, a tool tray that merely grows |
| Art. 2 | Every zine gets a body, and the body is *true* | Any texture, colour, or flourish that appears on screen but not in the export |
| Art. 3 | The user owns everything; nothing leaves without their hand | Accounts, cloud, telemetry, servers — including as an opt-in |
| Art. 4 | The tool is quiet | Streaks, nags, celebration used as a lever, **counters of any kind** |
| Art. 5 | Honest all the way down | Warm dead-ends, silent failure, euphemism |
| Art. 6 | The first minute belongs to the beginner | Austerity, jargon, gesture-only paths, tutorials before doing |
| Art. 7 | The maker makes it; the hand stays visible | Machine-perfect aesthetics that erase the person; anything that authors *for* the user |

**Fixed at a lower rank, and the difference matters.** The [Feature Tribunal](zinely-constitution.md#vii-the-feature-tribunal)
carries its own rank note: *"the **articles** are constitutional; this table is their first ratified
**application** and is amendable at roadmap rank."* So when this document cites the Tribunal's verdict
on the reveal — *"**KEEP — flagship.** It is Article 2 performed as theater."* — that is a **ratified
roadmap position, not a constitutional one**, and an argument leaning on it carries roadmap weight only.
The first draft of this document leaned on it as though it were constitutional. It is not.

Likewise [DESIGN-LANGUAGE.md](design/DESIGN-LANGUAGE.md), which supplies the "cozy / cute / handmade"
emotional register, **self-declares as a companion reference and not a parallel source of truth.** The
constitution says only *beginner* ([Art. 6](zinely-constitution.md#article-6--the-first-minute-belongs-to-the-beginner)).
A direction may therefore propose a different emotional register; it may not propose a different
audience.

**Not fixed — everything else.** Every screen, every layout, the palette, the metaphor, the motion
vocabulary, the navigation model, the component library, the word "bench", and the craft-table identity
itself. The [Ten-Year Test](zinely-constitution.md#viii-the-ten-year-test) already struck Material 3,
today's gesture conventions, the 45° hatch fill, the paper texture, "8 pages" as a number, and PDF as a
format from constitutional rank.

---

## The shared floor — which no direction may claim as a merit

The critique's findings are **remediation owed regardless of which direction wins.** They describe a
build that fails its own canon; fixing them is not a design bet. In particular, all three directions
below are assumed to deliver [V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md)'s Top 20 — the user's own
cover on the shelf, real work at all four emotional peaks, the end of the twelve-circle row, a page
that never resizes itself, one name per place, a typographic middle register.

**Consequently, no direction scores points in the comparison for "fixes the critique."** The first
draft of this document did exactly that, and since Direction C is in large part the critique's
remediation list rendered as an identity, it was scoring the incumbent against a rubric it was written
from. That row has been deleted from the table below.

Two collisions from the critique are also **open, not settled**, and any direction that trips them
inherits an unresolved decision rather than a free choice:

- **The editor empty state** is frozen by **[ADR-033](DECISIONS.md#adr-033)** (Accepted) — which is
  explicitly editor-specific and explicitly *not* generalisable to other empty surfaces; the shelf's
  empty state is governed elsewhere. All three directions below would redesign it, so all three require
  a superseding ADR ([V1-DESIGN-ELEVATION.md §18.1](V1-DESIGN-ELEVATION.md), left deliberately
  unresolved).
- **The motion profile** is specified by [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md) — gentle
  ease-out, 200–400 ms, *"avoid big springs (toy-like)"* — and the critique's own motion section is
  flagged 🟨 ASSUMPTION pending a screen recording that has never been made. Its instruction to itself
  was **"record first, redesign second."** That instruction binds this document too.

---

## The question each direction has to answer

Not "what should it look like." **"What should using Zinely feel like?"** — and, because the north star
is finishing *and returning*: *what feeling gets a person who has never made anything from a spark to a
finished object in one sitting, and brings them back to start another?*

The three answers disagree about what the user is: an **editor**, a **maker**, or a **person at a
table**. Everything else follows.

**A note on separability, since the first draft got this wrong.** These are not cleanly mutually
exclusive. Several of their best inventions are portable, and the recommendation below deliberately
transplants some. What is *not* portable is each direction's **spine** — its emotional bet and its
structural model. Those three are genuinely incompatible, and that is what is actually being chosen.

---

# Direction A — Editorial Atelier

*A quiet publishing studio. Monocle, Cereal, Kinfolk, Swiss editorial. You are not using an app; you
are laying out an issue.*

### 1. Design philosophy

Zinely is a small publishing house and the user runs it. The interface behaves the way a good magazine
behaves: it has a grid, it keeps to it, and it says nothing it does not need to say. Structure carries
all the meaning — nothing is a card, nothing is elevated, nothing is decorated. The one physical object
in an otherwise abstract space is the sheet of paper, and it is physical precisely because everything
else is not. The user's work is the only image on screen; every other element is set in type. The
flattery is not "look what we made easy for you" but **"you have taste, and this tool assumes it."**

### 2. Emotional goal

**Competence, and the calm that comes with it.** Unhurried, taken seriously, slightly more capable than
before opening it. Not *cute* — *composed*. The delight is the quiet click of something being in the
right place. Pride is the exit emotion, and it is durable: you show people work you are proud of.

### 3. Visual language

- **Typography** does all the work and *is* the identity. One serif family with real optical sizes:
  a display cut for the wordmark and titles, a text cut for reading, and chrome set in **small,
  letterspaced capitals**, never bold. Old-style figures for dates and page numbers. Buttons are words
  with a hairline rule beneath them. There is essentially no icon set.
- **Colour** is bone (`#FAF7F2`), ink (near-black, never pure), a soft grey, and **one accent chosen
  per zine — the issue's spot colour.** Scarce, therefore meaningful. No gradients. Dark mode is
  genuinely designed rather than inverted: warm charcoal ground, bone type, the same single accent.
- **Spacing** is the luxury signal: a visible baseline grid, a strict modular scale, margins a
  productivity app would consider wasteful. Density is a choice we decline to make.
- **Surfaces:** none. Content sits on the ground plane; separation is a hairline rule or empty space.
- **Depth** is flat with exactly one exception — the sheet casts a real, soft, single-source shadow,
  because it is the only object in the room. Depth means *paper*.
- **Motion** is editorial: cuts, cross-dissolves, slides on a rail. 250–400 ms, heavy ease-out,
  **no overshoot** — a spring would be a spelling mistake here. The signature is type *setting* rather
  than animating in. (This is the only direction of the three that is fully compliant with
  [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md) as written.)

### 4. Interaction language

Manipulation is **placement against a grid**. Selection is a hairline box, not a chrome-heavy handle
rig — the object is the interface. A thin **verb rail** of text commands appears at the page edge on
selection and stays in the same place for every object type, so the eye never hunts. Nudging is in
millimetres and says so. What animates: only the object under the finger. What stays still: everything
else, always, including the page. The signature is **the measure** — text blocks show their line length
as a real typographic measure and say, gently, when it is wrong for the page. That does more to make a
beginner's page look designed than a template could, and it *teaches* rather than authors
([Art. 7](zinely-constitution.md#article-7--the-maker-makes-it)).

### 5. Library

**A contents page, not a grid.** One column of rows: real cover at true A7 proportion, title in the
display cut, date in old-style figures, page count, hairline rules between. It reads like a magazine's
index or a shelf seen spine-on. The user's own covers, in a column, with typographic dignity.

### 6. Editor

Quiet and dimensionally stable. The page is centred and **never changes size for any reason**. Margin
and grid guides are drawn in the thinnest possible line and snap firmly. No panel ever stands in front
of the thing it affects, because panels do not exist — the verb rail lives outside the page's rectangle.
It should feel like a large clean desk with very few things on it.

### 7. Read

Full-bleed, chrome fully dissolved, one page at true proportion. Tap the edges or swipe; the turn is a
**restrained** slide with a page shadow, not a curl. Nothing overlays the page until a tap brings the
controls back. The strongest thing this direction can do is get out of the way completely for thirty
seconds.

### 8. Print & Fold

**Going to press**, and this is the Atelier's home turf. The imposed sheet is presented as a real press
sheet: crop marks, fold marks, and the printer's non-printable margin **drawn honestly on the page**
rather than described in a warning. Paper is a named stock, chosen once and remembered, never asked for
before the user has made anything. The difference from every competitor: **prepress seriousness with no
prepress vocabulary** — the discipline without the jargon. Fold guidance is drawn from *your* sheet, at
*your* proportion, with your ink on it.

### 9. Completion

**An issue is published.** No celebration, no confetti, no exclamation mark. The library index gains one
new row above a hairline rule, and the ending is a **colophon**: *"Eight pages. Made 22 July. Yours."*
Quiet, permanent-feeling, and **re-readable** — a thing that stays, not an animation that plays once.
*(Note: the colophon must carry no issue number or count. The Tribunal kills
"graduation **counters**" outright — "**state, not score**" — and an incrementing issue number is
precisely a score. This corrects the first draft, which proposed one.)*

### 10. Strengths

Reads premium instantly and cheaply — typography and space are the only luxury signals that cost
nothing but discipline. No Android creative app looks like this. It is the direction most likely to age
well: a strict grid does not date, which the [Ten-Year Test](zinely-constitution.md#viii-the-ten-year-test)
rewards. It is the hardest of the three to execute *embarrassingly* — a clean grid is never humiliating,
only dull. And it is the safest against [Art. 2](zinely-constitution.md#article-2--every-zine-gets-a-body):
there is almost nothing on screen that could fail to reach the paper.

### 11. Weaknesses

**Its aesthetic standard exceeds its users' output, and that is arithmetic, not taste.** Serious
typography *exposes* weak content: a beginner's phone snapshot on an immaculate Swiss grid looks worse
than it does on a scrappy page, because the grid supplies a standard the content cannot meet. Austerity
also reads to a first-timer as *"I am not good enough to be in here"* — a direct strain on
[Art. 6](zinely-constitution.md#article-6--the-first-minute-belongs-to-the-beginner), and worst at the
empty state, already the most contested surface in the product. And it fights
[Art. 7](zinely-constitution.md#article-7--the-maker-makes-it) at the root: Swiss precision is a machine
aesthetic that erases the hand by design, while ✅ VERIFIED evidence in
[RESEARCH §R12.4](RESEARCH.md) says a zine reads as a zine through *visible authorship, mixed type
registers, near-but-not-exact alignment, density over whitespace* — a list that is, item by item, the
inverse of this direction.

---

# Direction B — DIY Zine Workshop

*Photocopier, tape, scissors, marker, riso. Handmade. Precise in the tools, messy in the output —
deliberately.*

### 1. Design philosophy

Zinely does not imitate paper; it imitates the **process**. The identity is not "a page" but "a thing
that was made and reproduced" — cut, stuck down, run off, and given away. Everything on screen is a
piece of paper on another piece of paper, fastened with something. The aesthetic argument is that a
zine's beauty is *reproductive rather than compositional*: it looks right because it was run off, not
because it was laid out. This is the register [RESEARCH §R12.4](RESEARCH.md) documents as ✅ VERIFIED
— *"the imperfections were part of the aesthetic — proof of authenticity"* — and nothing here is
machine-perfect, so nothing the user makes can fall short.

**Its range is the workshop, not one corner of it.** The brief names photocopier *and* tape *and*
scissors *and* marker *and* riso *and* punk. Read whole, that is the craft-table register with a
reproduction step added — not a subculture the beginner has to already belong to. *(The first draft
narrowed this to "a photocopier at 2am", then eliminated the direction on the grounds that beginners
want cute rather than punk. The objection was manufactured by the narrowing.)*

### 2. Emotional goal

**Permission.** One sentence: *"nothing you do in here can be wrong."*

🟨 **ASSUMPTION, and it must be labelled as one.** The claim is that beginners abandon creative tools
because they fear making something ugly in a tool that visibly expects better. **No source in
[RESEARCH.md](RESEARCH.md) establishes that**, and the constitution's abandonment passage names
*different* mechanisms: *"the blank-start problem and long-tutorial drop-off are the best-documented
effects in the entire research base"* — starting and instruction, not shame — with finished-looking
starters as its own prescribed remedy. Permission is a plausible and attractive hypothesis about a
third mechanism; it is not the evidenced one, and an earlier draft of this document presented it as
though it were.

Delight comes from **surprise**: the ink offsets, the halftone bites, and the result is better than
what was planned. The exit emotion is **glee**, and glee is what makes someone make a second one that
afternoon — the *return rate* half of the north star.

### 3. Visual language

- **Type:** a workhorse grotesque set slightly tight, plus a marker or stencil face for headings, on
  deliberately imperfect baselines. Chrome type looks *stamped*. Mixed registers on one page are a
  feature, per R12.4's marker list.
- **Colour is ink, not palette.** The app runs on 1–2 spot inks over a stock ground, and the second ink
  is chosen per zine. This is not a theme picker; it is *the ink you loaded*, it applies to the whole
  issue, and it is a genuine [Art. 1](zinely-constitution.md#article-1--constraints-are-a-gift)
  constraint that makes a beginner's output look intentional for free.
- **Spacing** is tight and poster-like; content is allowed to run off the edge (R12.4).
- **Surfaces** overlap and are fastened — tape, staples, a bulldog clip. Texture is a large-dot halftone,
  not a paper grain.
- **Depth** is shallow and hard: offset drop shadows like a copy of a copy, not soft ambient light.
- **Motion is mechanical.** Things arrive with a pass; page changes have a roller feel; motion snaps
  with a small stutter, because a machine did it and machines are not graceful. **This departs from
  [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md)'s gentle ease-out and must be adopted knowingly** —
  §10 is a companion reference, so it can be amended, but not silently.

### 4. Interaction language

Manipulation is **handling scraps**. Picking something up gives it a hard shadow and a slight tilt;
perfect 0° is the exception, not the resting state. R12.4's warning is the design rule here: *"silent
auto-correction removes the content rather than improving it — surface the issue, offer the fix, let
the user decline."* Snapping therefore *offers*; it never quietly straightens. The signature is
**misregistration as a control** — the second ink can be nudged off-register on purpose, treated as a
creative decision rather than a defect, which is a thing no competitor would ship because every
competitor's aesthetic is alignment. Deletion is a physical bin with a lid, well away from everything
else — which incidentally supplies the separation and ceremony the critique found missing.

### 5. Library

**A distro table.** Zines overlapping at angles, each showing its real cover, cheap and colourful and
crowded — a wall of things the user made, which is the emotional maximum available from a library
screen. The accessible twin is a plain titled list, and it must exist and be designed rather than
derived ([Art. 6](zinely-constitution.md#article-6--the-first-minute-belongs-to-the-beginner) does not
bend for aesthetics).

### 6. Editor

The page is a sheet on the machine's glass, and it never moves. Supplies live in a drawer — tape,
marker, scissors, stamp — and you pick one up. Text arrives as a **cut-out you place**, not a field you
fill. Editing feels like collage: fast, loose, forgiving, and impossible to do "wrong" because the
aesthetic has no standard to violate.

### 7. Read

A flip through the reproduced object, with the chosen process visible, exactly as it will print. It
should feel like reading a zine someone handed you at a show rather than previewing a document.

### 8. Print & Fold

**The strongest print story of the three by a distance.** Every competing app treats print as *export*
— a file leaves the building. This treats it as **reproduction and distribution**: the imposed sheet is
a **master copy**, and the next sentence is *"now make ten."* A high-contrast master mode that survives
a photocopier follows from the identity instead of being bolted to it, and it lands on ground
[RESEARCH §R12.5](RESEARCH.md) identifies as the category's least-served step, where Zinely already
leads ([ADR-058](DECISIONS.md#adr-058)). R12.6's note that halftone is *"both cheaper to print and more
true to the form — rare; take it"* applies here and nowhere else.

⚠️ **Rank disclosure.** R12.5's named-process proposal carries its own caveat, which must travel with
every citation of it: *"**This is a feature proposal, not a critique finding** — it belongs in a
ROADMAP conversation, not in a perceived-quality ranking."* It can support a **feature**; it cannot
by itself carry an **identity**. (An earlier draft cited it twice without this, which is the same
promotion-past-rank defect that sank the first draft's use of the Feature Tribunal — committed the
second time in the opposite direction's favour.)

### 9. Completion

**The run.** Not a certificate — a call to distribute: *"Master copy ready. Make ten. Give nine away."*
Genuinely new emotional territory, constitutionally clean (serves
[Art. 2](zinely-constitution.md#article-2--every-zine-gets-a-body), needs no servers), and it reaches
*past the app*, which is exactly where a quiet tool should point at its ending. It also serves the
return rate more directly than any other ending proposed here.

### 10. Strengths

Owns the category outright — the only direction that could not be mistaken for another product. Its
**output** is ✅ VERIFIED against what actually makes something read as a zine ([R12.4](RESEARCH.md)),
which no other direction can claim. It is the only direction in which an ordinary snapshot comes out
**better than it went in** — halftone and one-ink reproduction flatter bad photographs, a quiet and
enormous advantage for a phone-camera product. And most of it is a **surface** change, which
[R12.3](RESEARCH.md) makes the safest kind.

⚠️ **Two claims an earlier draft made here do not survive scrutiny, and are withdrawn:**

- *"Its aesthetic is verified"* — R12.4 is a finding about **what a zine looks like**, not about what a
  zine *tool* looks like. Transferring it to the chrome is forbidden by R12.4's own resolution, quoted
  in §11 below: **craft in the tool must be high; polish in the output must be optional.** Stamped
  chrome and *"deliberately imperfect baselines"* are low craft in the tool by that very standard. What
  R12.4 verifies is B's **output**, and that is a feature claim, not an identity claim.
- *"Serves Article 7 maximally"* — contestable, and downgraded to ⚠️ in the table. Riso and photocopier
  imperfection is the **machine's** hand, not the maker's, and *"the result is better than what you
  planned"* is the tool contributing an aesthetic outcome the user did not author. Article 7's line is
  *"made-by-a-person over machine-perfect"*; **machine-imperfect is a third category the article does
  not adjudicate**, and reading it as a win for B is an assumption, not a derivation.
- Not all of B is surface. Replacing text entry with *"a cut-out you place, not a field you fill"* is a
  structural change to the editor's input model, so R12.3 protects B less than an earlier draft claimed.

### 11. Weaknesses

**Article 2 is the real risk, and it is not hypothetical:** if the halftone, the ink, and the
misregistration are on screen but not in the exported PDF, the preview is a lie and the direction is
unconstitutional. There is a documented answer — R12.5's **named process**, applied to the *output* so screen and paper
agree — but it is 🟦 RECOMMENDATION, not ✅ VERIFIED, it is unbuilt, and **as written in R12.5 it
contains its own Article 2 violation**: a *per-export* seed means *"same document, slightly different
every printing"*, while Article 2's invariant is *"preview-equals-output… an invariant, not a QA
goal."* The mechanism nominated to rescue B from Article 2 is, unamended, an Article 2 defect. The only
form that survives is a **seed fixed per zine and rendered into the preview** — chosen once, shown
before committing, identical on every printing. That amendment is a precondition of B, not a detail. **Accessibility is the second risk:** halftone under text fights our AA floor, and
"imperfect baselines" fights legibility; both are survivable only under a hard rule (texture may touch
artwork, never UI text or user text). **It can collapse into costume** — a texture layer over an
unchanged interaction model is worse than no theme, and R12.4's ⚠️ DISPUTED note is the standing
warning: Electric Zine Maker is loved *because* its UI is clunky, which is a trap, not a licence. Its
own resolution is the rule to hold: **craft in the tool must be high; polish in the output must be
optional.** Finally, of the three this is the most **fashion-bound** — riso and halftone are a 2020s
design register, and the Ten-Year Test is unkind to fashion in a way it is not to grids.

---

# Direction C — Creative Workbench 2.0

*One continuous physical space. Not screens with a paper theme — a desk, and everything happens on it.*

### 1. Design philosophy

The current identity is a craft table. Pushed to its limit, that stops being a *look* and becomes a
**spatial model**: Zinely has no screens. It has one workbench, and the zine is always physically
present on it. The shelf, the sheet being edited, and the folded booklet are **the same object at
different distances** — you never navigate to them, you move toward them, and you see it happen.
Nothing appears from nowhere; nothing cross-fades. Tools are objects you pick up, and while you hold
one you can see that you are holding it. Everything generic — every stock component that announces the
framework rather than the product — is removed, not restyled.

### 2. Emotional goal

**Absorption** — losing an hour at a table without noticing. Warm, tactile, low-stakes, hand-made.
Delight comes from **continuity**: the thing you tapped is the thing you are now holding, and the
illusion never broke. That is rarer on a phone than either austerity or grit, and it is the feeling
people describe as "it just feels *nice*" without being able to say why.

### 3. Visual language

- Keep the existing **paper / desk / ink** palette, and keep dynamic colour off — it is not merely
  disabled but **deleted** by **[ADR-048](DECISIONS.md#adr-048)**, so the identity stays print-true.
  *(The first draft attributed this to ADR-008, which is the beginner-first UX decision. Corrected.)*
- **Delete the framework's face.** No stock floating action button, no default chip row, no standard
  sheet with its recognisable corner, no filled tonal button. Controls become **objects**: a tape strip,
  a paper tab, a stamp, a card with a real edge. Largest perceived-quality change available for no new
  concept.
- **Type** gains the missing middle register: display serif for identity, a text face for reading, and
  a distinct third size and weight for mid-level labels. Three registers, used consistently.
- **Depth is real, shallow and consistent** — one light source, plausible heights, and **height means
  meaning**: higher = held, resting = placed. Never decoration.
- **Motion is spatial continuity as the grammar rather than the garnish.** Nothing fades; everything
  travels. The signature invariant: **the page never resizes.** ⚠️ This is the direction's spine and it
  is the direction's least-evidenced claim — see weaknesses.

### 4. Interaction language

Direct manipulation of the object itself, with a **visible, labelled twin for every gesture** —
required by [Sacred Thing 8](zinely-constitution.md#v-the-sacred-things-change--never), and the reason
this direction handles accessibility more gracefully than the other two. Handles live on the object, so
spatial actions stay spatial; everything else is a short, labelled, grouped set of controls with
destruction physically apart from creation. The signature: **tools are held.** Picking up the marker
changes what your finger does, and you can see it until you put it down.

### 5. Library

**The shelf, made literal.** Real covers at true A7 proportion with visible thickness, one consistent
shadow. Tapping a zine does not navigate — the zine comes toward you and opens, and the editor is that
same object, closer. Not a menu of projects; a shelf of things you made.

### 6. Editor

The sheet lies on the desk and is absolutely stable. Supplies at the bottom edge, in the thumb zone.
Selecting lifts an element; nothing untouched ever moves. Editing feels like *arranging* — a verb
beginners own — rather than *configuring*, which they do not. The quality claim rests on one sentence a
user should say unprompted: **"it never surprised me."**

### 7. Read

The zine, closed, in your hands; you open it. Turning is **finger-tracked, interruptible, and weighted**
— start a turn, change your mind, let it fall back. [R12.2](RESEARCH.md) sets exactly this bar and warns
that *"a canned non-interruptible flip is worse than a cross-fade because it exposes the metaphor as
fake."*

### 8. Print & Fold

**The sheet unfolds from the book you were just holding** — same object, one continuous movement, so
the imposition explains itself without a paragraph. The Tribunal's flagship verdict applies here
(*"Article 2 performed as theater"*) — **at roadmap rank**. The printed sheet keeps its ONE CUT legend
regardless: the animation explains imposition *in the app*, while the legend serves a person holding
paper and scissors, and the critique protects it for that reason. *(The first draft claimed both "no
legend" and "keep the legend"; the scope distinction is the resolution.)*

### 9. Completion

**You are holding it.** The zine, closed, your cover on it, on the desk, with a shadow. One sentence. It
does not play and vanish — it stays, and then takes its place on the shelf. The ending is not an event
but an object that now exists, which is the constitution's job statement rendered without copy.

### 10. Strengths

The most audience-safe of the three: it flatters beginners without condescending and asks them to
belong to nothing. The most accessible by construction — physical objects want labels and twins in a
way both austerity and grit resist. It preserves everything the critique said to protect: the wordmark,
the display serif, the palette, the sheet and its legend, the tape, the print voice. And it makes the
reveal — the Tribunal's flagship — structural rather than a single screen, so its cost is amortised
across every transition instead of spent once.

### 11. Weaknesses

**The strongest ✅ VERIFIED caution in our entire research base points directly at this direction, and
the first draft omitted it.** [R12.3](RESEARCH.md) — *"change the surface, keep the structure"* — is
two-source verified: Apple's iOS 18 Photos restructuring was rejected as *"confusing, unintuitive and
overwhelming"* and tabs were restored in iOS 26; Google's own expressive research found replacing an
established pattern with an expressive one **decreased usability**, concluding *"functionality must
never be sacrificed for visual impact."* Direction C's spine is *"Zinely has no screens"* and
*"navigation becomes movement"* — a structural restructuring performed during a visual-elevation pass,
which is precisely the move both sources punish.

**Its motion grammar is unevidenced and collides with frozen canon.** It contradicts
[DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md), and it is [V1-DESIGN-ELEVATION](V1-DESIGN-ELEVATION.md)
Top-20 items **#3 and #5 — both flagged ⚠️ as colliding with an existing decision**, resting on a motion
section the critique itself marked 🟨 ASSUMPTION because no recording was ever made. The critique's
instruction was *record first, redesign second*; this direction redesigns first.

**Physicality is the hardest thing here to execute well and the most embarrassing to execute badly** —
a shadow that disobeys the light, a turn that stutters, one transition that fades, and the illusion is
not damaged but destroyed. Most of its cost is engineering, and the failure mode is buying a philosophy
and shipping a texture — which is the exact defect the critique already caught the shipped build
committing. It is also the **least differentiated**: nobody screenshots an evolution. And the metaphor
has a ceiling: real desks do not scale to sixteen pages or a drawing layer.

---

# The comparison

Scored against what is not negotiable, not against taste. ✅ serves it · ⚠️ strains it · ❌ fights it.

**The rubric, declared before the scoring, because it changed between drafts and that change decided
the first two answers.** The first draft's table contained a "fixes the critique" row, which was
circular and is **removed** (see [the shared floor](#the-shared-floor--which-no-direction-may-claim-as-a-merit)).
The second draft added two research rows, which hand B ✅✅ and C ❌ and therefore decide the table by
themselves; they are kept, because ✅ VERIFIED evidence belongs in a rubric, but **the R12.4 row is now
scored on each direction's *output*, which is what R12.4 is a finding about** — scoring it on the
chrome, as the second draft did, is the over-read corrected in B §10.

| | **A · Editorial Atelier** | **B · DIY Zine Workshop** | **C · Creative Workbench 2.0** |
|---|---|---|---|
| North star — finish rate | ✅ excellent ending, cold start | ✅ permission attacks abandonment directly | ✅ the arc is one object |
| North star — **return rate** | ⚠️ pride is quiet | ✅ **"make ten, give nine away"** | ⚠️ the shelf, and little else |
| Art. 6 — the beginner's first minute | ❌ the standard exceeds the output | 🟨 permission is an unevidenced hypothesis | ✅ warm and safe |
| Art. 7 — the hand stays visible | ❌ a machine aesthetic, by design | ⚠️ machine-*imperfect* is not the maker's hand | ✅ strong |
| Art. 2 — truth of preview | ✅ almost nothing to fake | ⚠️ **the central risk; mechanism exists but is unbuilt** | ⚠️ physics must not lie |
| Art. 1 — constraint as a gift | ✅ the grid | ✅ one ink per issue | ✅ one desk |
| Art. 4 — quiet, no counters | ✅ | ✅ | ✅ |
| Accessibility floor (AA, twins, no gesture-only) | ✅ type-led | ⚠️ halftone vs contrast — rule-manageable | ✅ objects want labels |
| ✅ R12.4 — the **output** reads as a zine, not a brochure | ❌ inverse of the marker list | ✅ **is the marker list** | ⚠️ neutral — inherits whatever the export does |
| ✅ R12.4 — **craft in the tool** is high | ✅ | ⚠️ imperfect baselines, texture under type | ✅ its entire premise |
| ✅ R12.3 — change the surface, keep the structure | ✅ surface | ⚠️ mostly surface; the input model is not | ❌ **restructures navigation** |
| Ten-Year Test — ages well | ✅ grids do not date | ⚠️ riso is a 2020s register | ⚠️ metaphor-bound |
| Differentiation | ✅ high | ✅ **highest** | ❌ lowest |
| Risk of executing it badly | ✅ dull at worst | ⚠️ costume | ❌ **cheap at worst** |
| Cost of doing it properly | low | moderate — one export path, assets, one rule | **high, mostly engineering** |

**A** is the most respected and the least appropriate. It would produce the best screenshots and the
fewest finished zines. Its failure is arithmetic: a tool whose visual standard exceeds its users' output
punishes them for using it, and the aim is a finish rate. Nearly everything good in A — the measure, the
honest press sheet, the colophon — is separable from its austerity, which is a strong hint about what A
is actually for.

**C carries one fatal claim and one valuable one, and they are separable.** The fatal claim is
*"Zinely has no screens"* — a navigation restructuring performed during a visual-elevation pass, which
is exactly what ✅ VERIFIED [R12.3](RESEARCH.md) punishes. The valuable claim is everything else:
*the zine is always physically present · the page never resizes · one light source · continuity over
fades · an interruptible page turn · delete the framework's face.* Cutting the first does not kill the
second, and the two were welded together by nothing stronger than a slogan.

That distinction matters more than it looks. **"No screens" is C's most quotable line, but it is not
C's spine.** C's emotional bet is *absorption*, and its structural model is *the zine is always
physically present on one continuous surface* — neither of which requires abolishing navigation.
R12.3 damaged the slogan; it never touched the bet.

**B is the direction with the best output and the weakest identity argument.** After the corrections
above, what survives is precise: R12.4 ✅ VERIFIED that B's **artifact** is what a zine actually looks
like, and R12.5 🟦 proposes the mechanism — but R12.5 disclaims its own rank (*"a feature proposal…
belongs in a ROADMAP conversation"*), R12.4's resolution forbids importing the aesthetic into the
chrome, permission is 🟨 unevidenced, and B's Article 7 win is contestable. Strip the over-reads and
B's remaining case is: **the best possible zine to hold, produced by a tool with a legibility problem
it has to keep fighting.**

Which sets up the actual question, and it is not the one the first two drafts asked.

---

# The recommendation

## **Direction C — Creative Workbench 2.0**, with its most quotable claim struck and B's output identity adopted.

**The question the first two drafts got wrong was *where the identity lives*.** Both assumed a single
aesthetic has to govern the tool and the artifact together. Our own research base says the opposite,
in one line that adjudicates this entire document and that both drafts quoted while breaking:

> **Craft in the tool must be high — performance, typography, motion, no jank. Polish in the output
> must be optional, never automatic.** — [R12.4](RESEARCH.md)'s resolution of its ⚠️ DISPUTED note

Read literally, that sentence splits the brief in two and hands each half to a different direction.
The **tool** should be crafted, quiet, stable and physical — which is C. The **artifact** should be
reproduced, imperfect, one-ink and unmistakably a zine — which is B. Neither has to lose, and the
argument the first two drafts were having was about a choice that did not have to be made.

So the direction is **C**, amended twice and both amendments material:

1. **"Zinely has no screens" is struck.** It is C's most distinctive line and it is the one thing
   ✅ VERIFIED evidence tells us not to do ([R12.3](RESEARCH.md): Apple Photos' restructuring rolled
   back; Google's *"functionality must never be sacrificed for visual impact"*). Navigation keeps a
   conventional structure. What survives — and it is the spine, not a consolation — is C's emotional
   bet, **absorption**, and its structural model, **the zine is always physically present**. A slogan
   died; the direction did not.
2. **B's output identity is adopted as co-equal.** The named print processes, the one ink per zine, the
   halftone, the master-copy framing and *"make ten, give nine away"* become **what a Zinely zine is**,
   applied in the export and previewed truthfully — not a texture over the chrome.

**Why that is the right split, in one sentence: the user gives away the zine, not the app.** This also
resolves C's worst weakness. C's real problem was never that it fights the constitution — it was that
it is the least differentiated thing to look at. Under this recommendation Zinely's differentiation
lives in the object someone else ends up holding, which is where a zine tool's differentiation belongs
and where nobody can screenshot it away from us.

**Four reasons, and each is one that survived a review that killed its predecessors:**

1. **It is the only arrangement compatible with all three ✅ VERIFIED findings at once.** R12.3 says
   keep the structure — struck slogan, kept structure. R12.4 says the output must read as a zine —
   B's processes, in the export. R12.4's resolution says the tool must be crafted — C's quality bar.
   Every arrangement the earlier drafts proposed had to break one of the three.
2. **It is the most audience-safe reading of a beginner-first constitution.** A tells a first-timer
   they are not sophisticated enough; B's chrome asks them to already belong to a visual culture and
   fights legibility while doing it. C asks them to belong to nothing, and hands them B's aesthetic in
   the one place it is verified to work — the printed thing.
3. **It preserves what the critique said to protect** — the wordmark, the display serif, the palette,
   the sheet and its ONE CUT legend, the tape, the print voice — and the brief was explicit that
   novelty is not the goal.
4. **It puts the riskiest bets where they are cheapest to reverse.** A print process is a render path
   and a preview; an interface identity is every screen. If the processes disappoint, we lose a
   feature. If a chrome identity disappoints, we lose the app.

## What C must absorb, and from where

- **From B — the entire output identity**, on the amended terms in B §11: named processes with a seed
  **fixed per zine and rendered into the preview**, one ink per zine, halftone, the master copy, and
  the *"make ten, give nine away"* ending, which is the single best invention in this document and does
  not come from the winning direction.
- **From A — the colophon ending** (no issue number, no count), **the honest non-printable margin drawn
  on the sheet**, **the measure guide** for text, and **the index list as the shelf's designed
  accessible twin.**
- **From C's own weaknesses, kept in view:** the motion grammar is still unevidenced and still collides
  with [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md). Striking "no screens" does not license
  physics-by-assertion. Record first.

**What a founder who disagrees is choosing.** If you believe "no screens" *is* the idea and the rest is
scaffolding, you are choosing the first draft's C and taking R12.3 head-on — a defensible bet, but make
it knowingly. If you believe the tool itself should be the zine — stamped, taped, imperfect chrome and
all — you are choosing B, and accepting a legibility fight in every release and a 🟨 hypothesis as your
north-star mechanism. Both are real positions. Neither is what the evidence supports.

## The kill criteria

Two, both stated in advance, both aimed at the risk that could actually kill this recommendation:

> **Gate 1 — prove Article 2 on the output.** Take one finished page, apply a named process
> (*Photocopy, 3rd generation*) with a seed **fixed for that zine**, and put the on-screen preview and
> the exported PDF side by side on paper. They must agree, and they must agree again on a second export
> of the same zine. *(The mechanism as written in R12.5 varies per printing, which is itself an Article
> 2 violation — this gate exists to force the fixed-seed amendment rather than to rubber-stamp it.)*
> **If it fails, the processes are cut and C ships without B's output identity** — diminished, but
> still constitutional.
>
> **Gate 2 — prove the physics.** The shelf-to-editor continuity and an interruptible finger-tracked
> page turn in Read, on a mid-range device, preceded by the motion **recording** the critique has been
> asking for. **If it fails, C's physical claims are cut back to what can be held honestly** — a stable
> page, one light source, no cross-fades — which is worth having on its own.

**There is deliberately no third fallback to Direction A.** An earlier draft named A as the terminal
fallback while scoring it ❌ against two articles, and this document's own rule is that a direction
crossing a constitutional line is dead on arrival. A direction cannot be both. If both gates fail, the
correct outcome is a new exploration, not a promotion of the direction we disqualified.

Both gates are spikes. Neither is an implementation.

## How the review changed the answer

Recorded because the changes are the most useful thing in this document — and because a recommendation
that moved twice needs its reasons auditable rather than asserted.

**Round 1 — the first draft recommended C.** Verdict: **NO-GO**. Three of its four reasons failed:

| First draft's reason for C | What survived |
|---|---|
| "The constitution calls the reveal the flagship" | **Overreach.** That verdict is in the Feature Tribunal, explicitly *"amendable at roadmap rank."* Real, but roadmap-weight. |
| "It fixes the critique's central finding structurally" | **Circular.** C *is* the critique's remediation list; the fix is owed by every direction. Deleted from the scoring. |
| "A and B both sell the audience out" | **Half true.** A does. B's version was manufactured by renaming B "The Copy Room — a photocopier at 2am" and eliminating the narrowed version. |
| "It keeps what the critique said to protect" | **Stands.** |

**Round 2 — the re-run recommended B.** Verdict: **GO WITH FIXES**, with the recommendation judged not
yet ratifiable. Two of *its* four reasons had failed in the same way the first draft's had:

| Second draft's reason for B | What survived |
|---|---|
| "Its aesthetic is ✅ VERIFIED by R12.4" | **Over-read.** R12.4 is a finding about the **output**, not the tool, and its own resolution forbids the transfer. Reduced to a feature claim — a strong one. |
| "It attacks the best-documented failure in the research base" | **Misattributed.** The constitution names *"the blank-start problem and long-tutorial drop-off"* — starting and instruction, not shame. Permission is now marked 🟨 ASSUMPTION. |
| "Surface, not structure" | **Partly.** B's cut-out text model is a structural change to input. |
| "Serves Art. 7 maximally" | **Contested.** Machine-*imperfect* is not the maker's hand; downgraded to ⚠️. |

**The pattern in both failures was identical, and it is worth more than either recommendation:
promoting a lower-rank source to carry a higher-rank claim** — the Feature Tribunal for C, then R12.5's
self-declared *"feature proposal, not a critique finding"* for B. I committed the same defect twice, in
opposite directions, which is good evidence it was a habit rather than a bias toward one answer.

**Round 3 — this draft.** The correction that resolved it was not a new argument but a line already
quoted twice and obeyed neither time: *craft in the tool, polish in the output.* Once identity is
allowed to live in two places, C wins the tool and B wins the artifact, and the question the first two
drafts were fighting over turns out to have been badly posed.

**⚠️ The standing objection to this outcome, stated by the reviewer and not dismissed.** A recommendation
that returns to the incumbent after a detour deserves suspicion, and the honest test is whether the
*substance* moved rather than the label. What moved: C's most distinctive claim is deleted, its motion
grammar is explicitly still unevidenced, and half of its identity — the half a stranger actually holds —
now comes from the direction that beat it in round 2. If that reads to you as C winning by attrition,
that reading is available on the evidence and the founder should weigh it.

---

## What happens next

1. **Choose a direction** — or reject all three. This document is a proposal; the choice is the
   founder's, and it should be recorded as an ADR, because it constrains every UI decision after it.
2. **Run both gates before committing** — Article 2 on the output, then the physics. Each can cut a
   piece of the recommendation without killing it, which is what makes them worth running first.
3. **Whatever is chosen, the editor empty state needs a superseding ADR** — all three directions
   redesign a surface frozen by [ADR-033](DECISIONS.md#adr-033) (editor-specific by its own terms), and
   [V1-DESIGN-ELEVATION §18.1](V1-DESIGN-ELEVATION.md) deliberately left that unresolved.
4. **Record the motion baseline before touching motion.** Both B's mechanical profile and C's physics
   depart from [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md), and the critique's motion findings
   remain 🟨 ASSUMPTION until a screen recording exists. This is a precondition of gate 2, not a
   follow-up to it.
5. **Only then**: HTML specification, design freeze, Compose implementation, in that order, per the
   [HTML-first workflow](../CLAUDE.md#html-first-ui-workflow-mandatory).

No implementation should begin before step 1.

---

# Design Principles for Zinely V1

Ten, and no more. Each is meant to survive every redesign, name no technology, and be able to **settle
an argument** — a principle that cannot make someone lose a design debate is decoration.

**Rank.** These are a **design-rank companion**, subordinate to
[the constitution](zinely-constitution.md) and derived from it; where one appears to conflict with an
article, the article wins and the principle is wrong. The article or source each derives from is cited
so the derivation can be **checked rather than trusted** — and where a principle has no constitutional
parent, it says so instead of borrowing one.

⚠️ **They are a companion to [DESIGN-RULES.md](design/DESIGN-RULES.md), not a replacement for it.** An
earlier draft proposed they *"absorb and replace"* that checklist. They cannot: DESIGN-RULES is the
live merge gate, and mapping it onto these ten leaves **R2** (every screen answers "what can I do
next?"), **R3** (one primary action), **R4** (a blank state invites creation), **R5** (copy from
VOICE), **R7** (≥48dp, thumb zone), **R8** (AA contrast, including over texture) and **R12** (privacy
said once, warmly) with no counterpart. Adopting the replacement would have deleted seven shipping-gate
rules — including the contrast rule that the recommended direction's own second gate depends on.
Principles state *why*; the checklist states *what is verified before merge*. Both stay.

### 1. The user's work is the hero — everywhere it can be shown, it is shown.
Never a picture of the *concept* of their work. An illustration standing where the user's own work
could stand is a defect regardless of how well it is drawn.
*Derives from:* Art. 5 and Art. 7. The [Feature Tribunal](zinely-constitution.md#vii-the-feature-tribunal)
applies them to "real covers on the shelf" under the rationale *"show the user their own work"* — at
roadmap rank, and as a paraphrase of a table row rather than a quotable sentence.
*Settles:* library thumbnails, cover previews, fold guidance, the completion screen, every future
"preview" of anything.

### 2. Everywhere is one place.
A thing seen twice is the same thing, moved — not a new screen that resembles it. The user should never
have to work out where they are or what became of what they were just holding.
*Derives from:* Art. 6 (nothing to learn), Art. 1 (a place joins by displacing another).
*Settles:* every navigation transition, and every proposal to add a screen.

### 3. The work never moves unless the user moves it.
The page is the fixed point. Chrome arrives and departs around it; it does not reflow, resize, scroll,
or shift to make room for a tool.
*Derives from:* **nothing constitutional — and it says so rather than borrowing rank.**
[Sacred Thing 8](zinely-constitution.md#v-the-sacred-things-change--never) governs *how you act on the
object*, not the object's dimensional stability, and an earlier draft cited it here anyway. The real
authority is the measured 17% page resize in
[V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md) plus [DESIGN-LANGUAGE §10](design/DESIGN-LANGUAGE.md).
This is a design-rank principle with a design-rank parent.
*Settles:* toolbars, keyboards, selection states, panels — permanently.

### 4. Tools are held, not configured.
You touch the thing you want to change. The interface never asks you to fill in a form describing your
intent, and never stands in front of the thing it affects.
*Derives from:* Sacred Thing 8, Art. 6.
*Settles:* dialogs, settings-before-doing, and any control that is a proxy for an object.

### 5. Fewer choices, offered better, and never before the first mark.
Constraint is what makes the ending reachable; a decision demanded before the user has made anything is
a toll gate, not a feature.
*Derives from:* [Art. 1](zinely-constitution.md#article-1--constraints-are-a-gift) — *"a tool joins the
tray only if it displaces or nests another; the tray never merely grows."* The *"never before the first
mark"* half is design-rank, from 🟦 [R12.1](RESEARCH.md), not from the article.
*Settles:* feature requests, format questions asked too early, and the whole class of "just add an
option."

### 6. Nothing is only a gesture, and nothing is only a picture.
Every action has a visible, labelled, accessible twin; every state has a non-visual signal. This is a
design principle, not a compliance task, and it applies before the design is finished.
*Derives from:* [Art. 6](zinely-constitution.md#article-6--the-first-minute-belongs-to-the-beginner),
Sacred Thing 8.
*Settles:* hidden double-taps, icon-only rows, colour-only states, motion as the sole feedback.

### 7. Show, then say, then — only if you must — instruct.
A demonstration outranks an explanation; an explanation outranks an instruction. Anything the interface
has to explain twice is a design that failed once.
*Derives from:* Art. 6 (*"teaching happens inside real work"*), Art. 4 (quiet).
*Settles:* coach marks, tooltips, onboarding, repeated reassurance, and every paragraph on a screen.

### 8. The ending is quiet, and it leaves an object.
Completion is marked by something that now exists and stays — never by an animation that plays and
vanishes, never by a count, never by a nudge toward the next thing.
*Derives from:* [Art. 4](zinely-constitution.md#article-4--the-tool-is-quiet) (celebration *"earned,
brief, and never a lever"*). The sharper phrasing — *"state, not score"* — is the
[Feature Tribunal](zinely-constitution.md#vii-the-feature-tribunal)'s, at roadmap rank; an earlier
draft attributed it to the article.
*Settles:* confetti, celebration screens, streaks, issue numbers, and every proposal to make finishing
into a metric.

### 9. What we show is what they get — including the bad news.
The preview is a promise. Texture that will not print, a margin the printer will eat, a character that
will render blank: each must be visible before the user commits, in the place they commit. Honest beats
kind; both beat quiet.
*Derives from:* [Art. 2](zinely-constitution.md#article-2--every-zine-gets-a-body), Art. 5.
*Settles:* decorative texture, optimistic previews, silent fallbacks, and warnings placed after the
irreversible step.

### 10. The hand stays visible, and correction is offered rather than applied.
Imperfection is evidence of a person. We amplify the maker's hand and never replace it; assistance may
correct execution but never intent, and it surfaces the issue and offers the fix rather than quietly
performing it.
*Derives from:* [Art. 7](zinely-constitution.md#article-7--the-maker-makes-it); mechanism from
✅ [R12.4](RESEARCH.md).
*Settles:* generative content, one-tap results, silent auto-straightening, template dependency, and
machine-perfect polish proposed as an upgrade.
