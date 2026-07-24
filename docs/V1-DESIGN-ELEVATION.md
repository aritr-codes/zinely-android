# V1 Design Elevation — a product critique

**This is not an implementation document.** It contains no tickets, no acceptance criteria, and no
Compose. It is a judgement about how Zinely `0.9.0-beta.1` feels to use, written from the screen
rather than from the source, and a ranked argument about what to change.

**Status:** draft, reviewed once, corrected. Nothing here is approved, and nothing here should be
built until it is. §18 lists the places where this critique **contradicts a decision that was already
made deliberately** — those are the rows to argue about first.

---

## Method, and its deliberate blindness

Everything in §§1–17 was derived from **a single unbroken walkthrough of the shipped release-signed
build** (`0.9.0-beta.1`, `versionCode 3`) on a Samsung Galaxy A17 (SM-A176B, 1080×2340), in dark
theme and again in light. Eighteen screenshots are kept beside this document in
[docs/design/v1-critique-evidence/](design/v1-critique-evidence/) so every claim below can be checked
against the pixels rather than against my memory of them.

I did not read [DESIGN-LANGUAGE.md](design/DESIGN-LANGUAGE.md), [VOICE.md](design/VOICE.md),
[DESIGN-RULES.md](design/DESIGN-RULES.md), [EXPERIENCE-MAP.md](design/EXPERIENCE-MAP.md), or
[BETA-UX-REVIEW.md](BETA-UX-REVIEW.md) before writing §§1–17. That was the point. **Knowing why a
screen behaves as it does disqualifies you from judging whether it explains itself** — the rule this
repository already applies to device verification Pass 2
([CLAUDE.md](../CLAUDE.md#pass-2--first-time-user-verification)). §18 is what happened when those
documents were read afterwards, and it is the most useful section here.

Competitive research underpinning §§7–17 is summarised inline; the durable findings are landed in
[RESEARCH.md §R12](RESEARCH.md).
Claims are labelled ✅ **VERIFIED** (cited) · 🟦 **RECOMMENDATION** · 🟨 **ASSUMPTION**, per
[Research standards](../CLAUDE.md#research-standards).

### What this method cannot see — three disclosed gaps

These are limits of the evidence, not of the app. I would rather have holes in the record than
fabricate the missing frames.

1. **Motion and haptics are effectively invisible to it.** Still screenshots taken seconds after a tap
   cannot establish that a transition, animation, or vibration did *not* occur. §§8 and 12 are
   therefore **🟨 ASSUMPTION throughout** and must be re-run as a screen-recorded pass before any of
   their conclusions are acted on. This matters concretely: [ADR-051](DECISIONS.md#adr-051) specifies
   a **staged animated reveal with a success haptic** on the completion screen, and my capture — taken
   after it would have finished — shows only the end state. **I did not observe its absence. I
   observed nothing.**
2. **The photo path was never exercised.** The whole walkthrough is text-only. "Add a photo" is the
   editor's primary button, the first photo landing is the documented ★ emotional peak
   ([EXPERIENCE-MAP](design/EXPERIENCE-MAP.md)), and **Reframe** ([ADR-053](DECISIONS.md#adr-053)) is
   an entire shipped surface with its own controls. A critique that ranks the manipulation model #4
   and never places an image has a hole in exactly the place the product is most alive. **Close this
   before approving the Top 20.**
3. **The true first-run empty library was not observed** — I could not reach it without destroying a
   zine already on the device. §1 judges the first thirty seconds of a *returning* user and of the
   create flow.

---

## The thesis, in one paragraph

Zinely is not an ugly app. It is a **carefully made app that keeps interrupting itself to explain,
apologise, and take instructions.** Its best screens — the sheet diagram, the fold walkthrough, the
completion line — are genuinely good editorial design, better than anything in the zine-tool category.
Its worst moments are all the same moment: **a place where the engineering shows through the paper.**
A row of twelve identical white circles. A styling panel standing in front of the text it styles. A
page that changes size when a toolbar opens. A finished-zine celebration that draws a picture of
somebody else's zine.

The single highest-leverage change is not a redesign. It is this: **the app must show the user their
own work at every moment it currently shows them a diagram of the concept of work.**

---

## 1. First impression — the first thirty seconds

The wordmark is the best thing that happens. **"Zinely."** — high-contrast serif, tight, with a
single orange period. It is confident, it is not Roboto, and it does not look like an Android app.

Then, within two taps, three things go wrong in a row.

**The shelf is 75% empty and does not say so.** One zine card sits top-left in a two-column grid; the
remaining three-quarters of the screen is undifferentiated near-black, terminated by a floating orange
button. This is not minimalism — minimalism is a decision about what to remove. This is a grid with
one item in it. There is no shelf, no surface, no edge, no sense that the space is *for* anything.

**The first act Zinely asks of you is administrative.** "Start a zine" opens a sheet whose first line
is *"Choose your paper. You can print it at home on either."*, offering **A4** (210 × 297 mm) and
**Letter** (8.5 × 11 in) as two illustrated rectangles. The dimensions are given, and the thumbnails
do differ — A4 is meaningfully taller in proportion (1.414 vs 1.294, a 9.3% difference in aspect).
The problem is not legibility. It is **position**: the user is asked to make a print-time decision
before making a single mark, and most people opening a zine app have no view on it and no way to form
one yet.

Cosmos named this failure mode precisely when they re-engineered it away: requiring the filing
decision first "made saving feel like a decision instead of an instinct" (✅ VERIFIED,
[The Future of Cosmos](https://www.cosmos.so/blog/the-future-of-cosmos)). Zinely does the same thing
to *starting*. Paper size is a print property that has been promoted to a birth certificate.

**Then the app introduces itself twice more.** The editor's empty page says *"Let's make something
cute ✨"* over three ornament tiles, and beneath it, in small grey type, *"works offline · stays on
your phone"* — which the library already said, in a pill, forty pixels from the wordmark.

That repetition is not a matter of taste; **the app is breaking its own rule.**
[DESIGN-RULES](design/DESIGN-RULES.md) R12 says to surface that promise *"as a gift, **once**,
warmly."* I counted it twice before the first mark. And the reassurance is a commodity: every
competing zine tool already advertises local-only processing (✅ VERIFIED across
[snipzine](https://snipzine.com/), [Dirty Little Zine](https://dirtylittlezine.com/),
[Online Zine Maker](https://onlinezinemaker.netlify.app/), [Zeenster](https://zeenster.com/)).
Privacy is this category's table stakes. Said twice in thirty seconds, it spends the user's scarcest
attention on the one thing that does not distinguish the product. (See §18 — my first draft went
further than this and was wrong to.)

**The feeling produced:** *a polite, slightly anxious app that would like to reassure me before I have
asked for anything.* Not hostile. Not incompetent. Just not yet a tool.

---

## 2. Visual identity

**Does Zinely have a recognisable personality?** Partly — and it lives almost entirely in two
elements: the serif (wordmark, section heads, screen titles) and the burnt-orange / teal / cream
palette. On the printed-sheet screens, where cream paper sits on charcoal with a dashed fold line and
a red ONE CUT rule, the identity is *unmistakable*. That page could not be from any other app.

**Does it feel cohesive?** No — and the incoherence is systematic rather than random. Zinely is
running **two design languages that have not been introduced to each other**:

| | Language A — "the print shop" | Language B — "the Android app" |
|---|---|---|
| Where | Library header, Read, Print & fold, Fold, completion | Editor tray, text-style popover, bottom sheets, nudge controls |
| Type | Serif display, small-caps metadata | Sans, default weights |
| Shape | Paper rectangles, dashed rules, tape | Rounded rects, hairline-outlined cards, pills |
| Colour | Cream / charcoal / one accent | Grey-on-grey surfaces, two accents |
| Feels like | Something printed | Something configured |

The seam is visible in a single screenshot: on the text-style popover, a dark Material-ish panel with
hairline segmented buttons sits *on top of* a cream page, straddling the boundary between the paper
and the app, belonging to neither.

**Does it feel premium?** In stills, sometimes. In use, I cannot say from this evidence (see Method) —
but premium is not a palette, it is **consistency**, and Zinely's identity changes register the moment
you touch anything.

Three concrete identity leaks, all visible on device:

- **Emoji are doing icon work.** 🖼️ inside "Add a photo", ✏️ inside "Add words", 🤚 in the coach mark,
  ✨ inline in a display-serif headline. These are the platform's emoji font — a foreign illustration
  style, a foreign palette, a foreign optical weight — sitting inside a hand-tuned design system, and
  they render differently on every OEM skin. **This is the build breaking its own rule again:**
  [VOICE](design/VOICE.md) §2 already says *"emoji as seasoning, not structure."* A glyph inside a
  button is structure. (My first draft said "replace every emoji", which over-shot — see §18.) Note
  the app *does* have drawn marks: the three ornament tiles and the Undo/Redo glyphs are its own hand,
  in the same screenshot as the emoji. The inconsistency is the finding, not an absence of craft.
- **Two primary colours, no legible rule.** Orange is primary on every screen except the last fold
  step, where the primary is a deep blue (*"It's folded — show me"*). I could construct a rationale.
  A user cannot.
- **The system chrome is not themed in light mode.** In light theme the library sits under a dark
  charcoal status bar, and the editor keeps a full charcoal top bar above a cream page. (The
  navigation bar *is* themed.) It reads as an unfinished port.
  (`18-light-lib.png`, `19-light-editor.png`.)

**Light theme is materially weaker than dark.** The zine card (cream) sits on a background (beige) of
nearly the same value; it is still legible — it carries a drop shadow — but the value separation is
thin, and the "paper on a desk" reading flattens into "beige on beige". A creative tool must look
deliberate in both, or ship one.

---

## 3. Library

**The question this screen must answer: "Which zine do I want?"**
([CLAUDE.md](../CLAUDE.md#product-principle-every-screen-answers-the-users-current-question))

**It cannot answer it.** This is the most serious single finding in the document — and it is **not a
new one.** [BETA-UX-REVIEW.md §3](BETA-UX-REVIEW.md) already owns this finding, already names the root
cause (the shelf holds only a `ProjectSummary`, so the cover is synthesised from an archetype),
already carries the competitive evidence, and already prioritises it. I reached it independently
without having read that document, which I take as corroboration rather than as novelty. **The
authoritative statement of it lives there; this section is a second witness.**

The zine card shows: a paper mock-up with a dashed fold line, `8-PAGE MINI · A4`, the title, a
timestamp — and, in the middle, **a generic orange blob that is not your work.** I added text to
page 4 and returned; the timestamp updated to "Edited 1 minute ago" and the artwork did not change
(`01-library.png` → `17-card-menu.png`).

So: two zines about different things, made months apart, are distinguishable only by a name and a
relative date. The shelf of a creative tool must be **a shelf of the things you made**. Procreate's
home screen is your artwork; Apple Photos' grid is the photographs. Zinely's shelf is a form with a
decoration on it.

The card is also the wrong object. A zine's identity is its **cover** — page 1, the thing that faces
out when the booklet is folded. The card renders a *sheet with a fold line*, which is the imposition
view: a manufacturing diagram, not a book.

Lesser findings on the same screen:

- **"Your zines  1"** — a count in tiny grey type beside a serif heading, with no alignment
  relationship to it. It reads as a debug label.
- **No sort, no search, no grouping, no drafts-vs-finished distinction.** Fine at one zine. Actively
  hostile at thirty, and thirty is the success case. (Deliberately *not* in the Top 20 — see the note
  below it.)
- **"On this device"** is a pill in the top-right, the position reserved by convention for account or
  settings. The highest-status slot on the screen, spent on a category commodity (§1).
- **The overflow sheet says "Open on the bench."** See §10 — the app has three names for two places.
- **Delete is under-weighted for what it does.** It *is* differentiated — red label and red icon
  against the neutral rows — but it sits in an undivided list, immediately below Duplicate, at the
  same size and rhythm, in a product with **no backup and no restore** (✅ VERIFIED,
  [CHANGELOG](../CHANGELOG.md)). I did not tap it, so **whether a confirmation follows is unverified**;
  [DESIGN-RULES](design/DESIGN-RULES.md) R6 requires a gentle, undoable treatment, and that should be
  checked rather than assumed. The finding is the *lack of separation and ceremony*, not a lack of
  colour.

**What is genuinely good:** the card *material* — the soft shadow, the paper edge, the small-caps
`8-PAGE MINI · A4` line. If that card contained the user's cover, this screen would be close to right.

---

## 4. Editor

**The question: "How do I change this page?"** The editor answers it, eventually, and makes you climb
over its own machinery to get there.

### The page is not an object

**The page is not centred.** It is flush to the left edge of the screen, with a charcoal gutter only
on the right. In dark theme this half-reads as a canvas surround; in light theme it is unambiguously a
stray dark band beside the paper (`19-light-editor.png`).

**The page changes size when you select something.** With a block selected, the tray grows and the
page canvas shrinks — measured, the paper is **17% narrower** than a moment earlier (right edge at
878px, then 727px of 923; `10-new-zine.png` → `13-text-selected.png`). Physical objects do not resize
when you pick up a tool near them. This single behaviour does more damage to the paper metaphor than
every missing texture combined.

**The page has no edge.** No shadow, no deckle, no thickness, no surface underneath it. It is a
rectangle of cream that runs off the screen. Compare Paper by WeTransfer, whose entire emotional
proposition rested on your work being *an object you opened* (✅ VERIFIED,
[IDSA award citation](https://www.idsa.org/awards-recognition/idea/idea-gallery/paper-by-fiftythree/)).

### The nudge row is the low point of the product

Select a block and a horizontally scrolling row appears containing **twelve identical white circles**:
‹ › ∧ ∨ + − ↻ ↺ bring-forward, send-back, **A**, and a **trash can**. No labels. No grouping. No
separators. No visible scroll affordance — I only found the second half by guessing it scrolled.
(`13-text-selected.png`, `14-tools-scrolled.png`.)

This is the clearest example in the app of *engineering surfaced as interface*. Every one of those
controls exists for a defensible reason — they are the keyboard- and screen-reader-accessible
equivalents of drag, pinch and rotate, required by [DESIGN-RULES](design/DESIGN-RULES.md) R1's
prohibition on gesture-only paths, and that is an obligation this project is right to take seriously.
But an accessibility affordance has been promoted to **the primary manipulation UI for everyone**, and
the result looks like a debug palette.

And **delete lives in that row** — same size, same shape, same colour, one position from "nudge right",
in an app with no backup. That is not a styling issue.

### Everything else in the tray

- **"Supplies"** is a lovely word, set in 11px grey at the top-left of a black tray, where it reads as
  a section header nobody styled.
- **Undo and Redo are peers of Add words.** Four tiles, equal size, one row: an orange creation
  button, a white creation button, and two history controls in the same shell. History is not a supply.
- **Disabled Redo is a dark grey slab with near-illegible text** — it does not read as disabled, it
  reads as broken.
- **The coach mark is a page-mounted banner.** *"🤚 Drag to move it. Pinch to resize. **Got it**"* sits
  on the artwork, with an asymmetric notched corner, and persisted across four unrelated interactions
  (still present in `15-text-style.png`).
- **The stray caret.** On creating a text block, an orange text cursor renders at the *left edge of the
  page*, detached from the block being edited (`11-add-words.png`). I do not know the cause. I know
  what it looks like: a glitch, in the first ten seconds of the core interaction.
- **The empty text block is a hairline orange rectangle** — hard corners, no fill, no placeholder — in
  an app where every other rectangle is soft. It is the only 1990s object on screen.

### The text-style popover stands in front of its own effect

Tap **A** and a dark panel opens over the block you are styling (`15-text-style.png`). In my capture
the first line of text remained visible and the second was half-covered — how much is hidden depends
on where the block sits, and I did not test a block near the top of the page. **The categorical claim
"you cannot see the effect" is not established; the structural problem is.** A control whose only job
is to let you judge a visual change should not be anchored on top of the change.

Inside it: `12 pt`. A print unit, raw, with steppers — on a page that will print at **A7, 74 × 105 mm**.
The rows are labelled (Size / Align / Style / Colour) and Align shows a clear selected state; the
**five colour swatches do not** — no names, no selected state, and no indication that at least one of
them (teal, ✅ VERIFIED shipped limitation) falls below AA as body text on white paper.

### The page strip

The one place the editor shows the user their work: eight cards, gently rotated, with a strip of
yellow tape on the current page. The tape is charming and the rotation is *exactly* right — hand-placed,
not grid-placed. This is also the app's own documented instinct
([DESIGN-RULES](design/DESIGN-RULES.md) R10, DESIGN-LANGUAGE §2 "handmade over precise") landing
correctly, and it matches what makes something read as a zine rather than a brochure: near-but-not-exact
alignment (✅ VERIFIED, [RESEARCH.md §R12.4](RESEARCH.md)).

Two problems: **page 8 is cut off** at the screen edge with no padding at either end, so the strip
looks clipped rather than scrollable; and the thumbnails are small enough that a page with one text
block reads as a grey smudge.

---

## 5. Read

**The question: "What have I made?"** This screen was the entire reason `0.9.0-beta.1` exists
([ADR-058](DECISIONS.md#adr-058)), and structurally it is right — a swipeable stack with the next page
peeking at the right edge, "Page 1 of 8" underneath.

- **It does not know your zine's name.** The header says **"Your zine"** on a document titled *My
  zine* — on every screen from Read through the fold walkthrough, while the library card and the
  overflow sheet both say *My zine*. A titled artifact loses its title at the exact moment you are
  invited to admire it.
- **The subtitle is an instruction, permanently.** *"Read · swipe to turn the page"* occupies the slot
  where a book would put its author.
- **Three dead grey squares sit in the top-right.** On Print & fold they are the wizard's step
  indicator, with the current step orange. On Read they are inert — the same component, same position,
  no state, reading as three disabled buttons. (`03-read.png` vs `04-printfold.png`.)
- **The page has no depth.** A rounded cream rectangle on black. No shadow, no stack behind it, no
  sense that seven more pages exist under this one.
- **"Print & fold" is the primary action on a blank zine.** The screen's job is to let you look; its
  loudest element pushes you out of it.
- **The page turn.** 🟨 I could not observe the transition from stills (see Method). What I can say is
  that the *product* — a folded paper booklet — makes page physics load-bearing rather than
  decorative, and there is unusually strong evidence for that: Apple removed the page-curl from Books,
  faced sustained backlash, and **restored it in iOS 16.4 as one of three options** (✅ VERIFIED,
  [MacRumors](https://www.macrumors.com/how-to/re-enable-page-turning-animation-apple-books/),
  [M.G. Siegler](https://mgs.blog/apples-turns-the-page-on-books-app-page-turns-8f9735a11ad5)). The
  winning version tracked the finger and **fell back unturned if you changed your mind mid-gesture**
  (✅ VERIFIED,
  [Gadget Hacks](https://ios.gadgethacks.com/how-to/get-page-turning-curl-animation-back-apple-books-for-iphone-and-ipad-0385329/)).
  Whatever Read does today, *that* is the bar.

---

## 6. Print & Fold

**The best design work in the product, and I would protect most of it.**

**Step 1 — The sheet.** *"This is your sheet. One page, printed on one side. **It looks scrambled on
purpose** — the fold puts every page in order."* That is a perfect piece of interface writing: it
names the user's fear before they feel it. The diagram — dashed fold lines, a red **ONE CUT** rule
with a pill label, upside-down page numbers on the top row, a legend reading *fold lines · the one cut
· printer can't reach here* — is genuinely expert. Nothing in the zine-tool category comes close; most
of the category ends at PDF export, and Electric Zine Maker's users specifically asked for clearer
folding and page-number guidance (✅ VERIFIED, [itch.io](https://alienmelon.itch.io/electric-zine-maker)).

**And it shows page numbers instead of your artwork.** The "Front cover / Back cover" previews below
the sheet are blank rectangles containing a **1** and an **8**. At the moment of maximum pride — *this
is the thing I am about to hold* — the app shows a wireframe. This is a shipped, honestly documented
limitation ([ADR-058](DECISIONS.md#adr-058) Decision 7), and it is also the second-largest emotional
hole in the product after §3.

**Step 2 — Print.**

- **The screen called "Print" cannot print** (✅ VERIFIED shipped limitation,
  [ADR-052](DECISIONS.md#adr-052)). It offers *Save PDF* and *Share* as two identical low-emphasis
  outlined buttons at ~60% screen height, while the primary button — *"Now fold it"* — advances the
  wizard. **The user's actual next action is the least prominent thing on the screen**, with ~28% of
  the screen empty beneath it.
- **"Now fold it" is a chronological lie.** You have not printed. You cannot fold. The button is named
  for the next *screen*, not the next *act*.
- **Colour carries meaning nobody explained.** Two setting chips are orange, two teal. My best guess
  is *things that break your zine* vs *things already correct* — a good idea, invisible without a
  legend, on a screen that has already proven it knows how to draw legends.
- **Three type treatments in one line** — `100% · Actual size` orange, `— not "Fit to page"` white
  bold, the label grey — on the Scale and Orientation rows. Paper and Sides are set plainly, which
  makes the pattern read as emphasis-by-availability rather than by meaning.

**Step 3 — Fold.** Five steps, good prose (*"All folds are valleys"*, *"Push the two ends toward the
middle so the panels pop into a plus"*), teal fold lines on cream. Problems:

- **Two nested step systems.** "Step 3 of 3" (squares, top-right) contains "1 of 5" (dots, bottom
  centre), in two different visual languages, on one screen.
- **The reading order zigzags:** title → subtitle → caption (*"eight panels"*, lowercase grey, above
  the diagram) → diagram → step number and title → body → controls. The caption is orphaned above the
  thing it captions.
- **Proportions are inverted.** Title and subtitle take ~20% of the screen, the diagram ~22%, and ~35%
  below it is empty. On a screen whose job is "show me the fold", the fold is the third thing you read.
- **They are not your zine.** Generic panels, again.

**The end.** *"Your zine is a book."* — the best sentence in the product. Followed by *"Eight pages,
made by hand, kept on this device. It's on your shelf whenever you want it."*

And above that sentence, **an illustration of a generic booklet with an orange rectangle on the
cover.** Not your cover. At the single most emotional moment Zinely will ever have with a user, it
draws a picture of somebody else's zine.

Then: **"Back to bench"** — which does not go to the shelf the sentence just promised, and does not go
to the library. It goes to the editor (verified: `08-after-fold.png` → `09-back-to-bench.png`). The
body copy and the button beside it disagree about where you are going.

And the primary action at the end of making a zine is **"+ Make another"** — production over
admiration, at the one moment the user wanted to sit with the thing.

---

## 7. Typography

The serif is the strongest asset in the product and it is **under-deployed and over-decorated**.

**What works.** The wordmark. Screen titles (*"This is your sheet"*, *"Fold it into a book"*, *"Your
zine is a book."*) — display serif, tight, high contrast, genuinely editorial. The small-caps
`8-PAGE MINI · A4` metadata line. These are the app's voice.

**What does not.**

- **The hierarchy is two-note.** Serif display, then a single grey sans body size, everywhere. There
  is no intermediate step, so screens with real content (Print step 2) have nothing between "shout"
  and "mumble", and the middle is filled with **bold** and colour instead of size and space. Book
  typesetting practice is settled on this: the space between elements does as much work as the
  elements (✅ VERIFIED, [Bringhurst, summarised](https://www.inkwell.ie/typography/bringhurst.html)).
- **Straight and curly apostrophes both ship.** *"Let's make something cute"* uses `'`; *"It's folded —
  show me"*, *"can't"* and *"You'll"* use `’`. In a product about printed matter, the typographic
  apostrophe is not a nicety.
- **Emoji are set inline in display type** (§2).
- **The page's own text is the app's sans**, left-aligned at `12 pt`, with no relationship to the serif
  identity. The user's words render in the least characterful typeface in the product. One typeface is
  a documented limitation; *which* typeface was still a choice.
- **No measure feedback.** An A7 page at sane margins gives roughly 55mm of text width. A 50–75
  character measure (✅ VERIFIED, [Typography Handbook](https://typographyhandbook.com/print/)) forces
  roughly 7–8pt type at that width. The app offers a `pt` stepper and no characters-per-line
  indication, so the most common amateur mini-zine failure — type sized for a phone screen, printed at
  four words per line — is unguarded.
- **No widow or orphan awareness.** On an eight-page zine one stranded word is ~3% of a page, and
  widows/orphans are exactly what "make a book look unfinished" (✅ VERIFIED,
  [Foglio](https://www.foglioprint.com/blog/widows-and-orphans-in-typography)).

---

## 8. Motion

> 🟨 **This entire section is ASSUMPTION and must be re-run as a screen-recorded pass.** Eighteen
> stills cannot establish that motion is absent. What follows is an argument about what motion should
> carry, not a measurement of what is there. See Method, gap 1 — and note that
> [ADR-051](DECISIONS.md#adr-051) specifies a staged reveal I would not have captured.

Across the walkthrough I saw no *evidence* of a signature transition — no shared element from card to
editor, no page-turn physics — but absence of evidence here is exactly that.

What is worth arguing regardless: motion is the largest available lever on perceived quality, for a
documented reason. Google's research programme — 46 studies, 18,000+ participants — measured
expressive treatment at **+34% perceived modernity**, **+32% "subculture"** and **+30%
"rebelliousness"** versus baseline, with key elements spotted up to **4× faster** (✅ VERIFIED,
[Google Design](https://design.google/library/expressive-material-design-google-research)). Two of
those three attributes are *literally* the cultural register DIY zine publishing trades in (✅ VERIFIED,
[RESEARCH.md §R12.4](RESEARCH.md)).
That alignment is unusually clean and Zinely should be spending it.

Three motions would carry the identity, in this order:

1. **The page turn in Read** — finger-tracked, damped, *interruptible*, modelling A7 card stiffness
   rather than a floppy novel leaf (§5).
2. **Spatial continuity as the navigation grammar** — the library card *becomes* the editor; the page
   thumbnail *becomes* the page. This is the mechanism reviewers name when they call Craft documents
   "published" where Notion's are "functional" (✅ VERIFIED, [2sync](https://2sync.com/blog/craft-vs-notion)),
   and it is Apple Photos' grid-to-detail transition.
3. **The fold, actually folding** — five still diagrams become one continuous animation the user
   scrubs.

⚠️ **This section collides with a frozen spec.** [DESIGN-LANGUAGE](design/DESIGN-LANGUAGE.md) §10
already sets the motion profile — gentle ease-out, ~300–400ms screen transitions, "tiny" 3–5%
one-bounce settles, and an explicit *"avoid big springs (toy-like)"*. My instinct that a 300ms
`FastOutSlowIn` reads as stock is a **direct disagreement with that spec, not an observation of its
absence.** See §18.4.

---

## 9. Interaction design

**The good instinct:** direct manipulation is present — drag to move, pinch to resize, tap to select.

**The failure:** it is not trusted. Every direct manipulation has a redundant button, the buttons are
always visible, and the buttons are what the screen looks like.

- **No visible undo posture.** Undo exists as a tray tile — not a gesture, not held, not scrubbable.
  Procreate's two-finger undo and Paper's rotate-to-rewind exist so that *reversibility becomes a
  reflex*, which is what lets a tool drop confirmation dialogs (✅ VERIFIED that Procreate's stated
  principle was to refuse buttons and ship gestures,
  [Apple Developer](https://developer.apple.com/news/?id=e409h6ja)). Zinely has the opposite posture:
  destructive delete in a crowded row and no reflex to catch it. **Any fix must respect
  [DESIGN-RULES](design/DESIGN-RULES.md) R1 — the gesture is an addition, never a replacement.**
- **No snapping, alignment feedback, or margin guides.** Nothing tells the user where the fold will
  crease their text, or where the printer physically cannot reach — despite the app *already knowing
  both* and drawing "printer can't reach here" one screen later. The knowledge exists in the product;
  it is not present at the moment of placement.
- **No inference.** Procreate's QuickShape corrects a wobbly circle one beat *after* you commit and
  lets you keep editing it (✅ VERIFIED,
  [Procreate Handbook](https://help.procreate.com/procreate/handbook/interface-gestures/gestures)).
  Correcting *after* rather than predicting *before* gives competence without taking authorship —
  exactly the balance a zine tool needs (§17).
- **Modes are invisible.** Nothing announces that a block is selected except the handles, and nothing
  says how to deselect.
- **The lone chevron `∨` under the empty-state copy reads as an unlabelled control.** It is not one —
  it is the orienting arrow [ADR-033](DECISIONS.md#adr-033) adopted to point the eye down to the
  supply tray. **That makes the finding better, not weaker:** an intentional orientation cue is
  reading as a mystery button, which is a [DESIGN-RULES](design/DESIGN-RULES.md) R10 failure
  ("decoration must also do a job — or get out of the way"). Reframe it; don't delete it.

---

## 10. Navigation

Structurally, navigation is **fine and should not be touched**. Library → Editor → Read → Print
(3 steps) is correct, and it is the ordering fix `0.9.0-beta.1` was cut to deliver
([ADR-058](DECISIONS.md#adr-058)).

I want to say that unusually firmly, because the temptation in a design-elevation milestone is to
restructure. Two well-sourced cautionary cases argue against it. Apple's iOS 18 Photos redesign was
rejected as "confusing, unintuitive and overwhelming", the carousel was removed, and tabs were
restored in iOS 26 with Federighi saying "many of you missed using tabs" (✅ VERIFIED,
[MacRumors](https://www.macrumors.com/2024/11/21/apples-photos-app-overhaul-controversial/),
[iMore](https://www.imore.com/apps/user-backlash-has-reportedly-caused-apple-to-rethink-ios-18s-photos-redesign)).
And Google's own expressive research found that replacing an established pattern with an expressive
one **decreased usability**: "functionality must never be sacrificed for visual impact" (✅ VERIFIED,
[Google Design](https://design.google/library/expressive-material-design-google-research)).

**Change the surface. Keep the structure.** That is the governing constraint on this whole milestone.

What *is* broken is not topology, it is **naming**:

| The place | Called |
|---|---|
| The editor | "the bench" (overflow menu, completion screen) |
| The library | "Your zines" (header), "your shelf" (completion body copy) |
| Read | "Read" (its own header), "Preview ›" (the link that opens it) |

Three names for two places, plus a doorway labelled for a different room. **"Back to bench"** sits
directly beneath **"It's on your shelf"** and goes to neither shelf nor library. The word "bench" —
which I rather like — appears nowhere the user could learn it. (The Preview/Read half of this is
already conceded in print in the shipped [tester package](releases/0.9.0-beta.1.md) §3.)

Also: **the back affordances are inconsistent.** Print & fold has a `‹` in the top bar *and* a "Back"
button in the bottom bar *and* system back; Read has only `‹`; the editor has none and relies entirely
on system back.

---

## 11. Information hierarchy

The recurring shape of every screen is: **a strong title, a paragraph explaining it, content, then a
large void, then a loud button.** It appears on Print step 2, Fold, and the completion screen, and it
produces two problems.

**Explanation outranks content.** Measured on the Fold screen: title and subtitle ~20%, diagram ~22%,
~35% empty below. The instructional prose — which is *good* prose — is laid out as if it were the
subject.

**Void is doing no work.** Empty space that is a considered proportion reads as luxury; empty space
that is "content ran out" reads as an unfinished layout. Zinely's is the second kind, consistently at
the bottom, because the primary button is pinned, the content is top-aligned, and nothing negotiates
between them.

**Secondary actions get lost mid-screen.** *Save PDF* and *Share* — the two actions that produce the
user's actual output — sit at ~60% screen height in the weakest treatment on the page.

---

## 12. Microinteractions

> 🟨 **Also ASSUMPTION, for the same reason as §8.** I performed these interactions over adb; I could
> not feel haptics, and stills cannot show a transition. [ADR-051](DECISIONS.md#adr-051) specifies a
> `success` haptic and a staged reveal on completion, and [CHANGELOG](../CHANGELOG.md) records that the
> Type bar buzzes on each accepted change. **I did not verify either way.** What follows is a map of
> which moments *should* carry feeling, to be checked against what already does.

| Moment | Should be |
|---|---|
| Placing a block on the page | A soft placement tick — the physical register of a thing set down |
| Turning a page in Read | Finger-tracked curl + a page-edge cue at the flip point |
| Reaching a fold step | A single crisp click — a crease |
| Completing the fold | The one moment that has earned a real, quiet flourish |
| Snapping to a margin | The magnetic pull Canva gets right (there is no snapping today, §9) |

Android's own guidance is that effects "shouldn't overwhelm the user or feel gratuitous", with frequent
events "very subtle" and important events "stronger than changing a toggle" (✅ VERIFIED,
[Android haptics principles](https://developer.android.com/develop/ui/views/haptics/haptics-principles)).
The richer vocabulary lives in `VibrationEffect.Composition` primitives (✅ VERIFIED,
[custom haptic effects](https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects)),
which Compose's `LocalHapticFeedback` does not fully expose (✅ VERIFIED,
[sinasamaki](https://www.sinasamaki.com/haptic-feedback-in-jetpack-compose/)). This restates
[DESIGN-LANGUAGE](design/DESIGN-LANGUAGE.md) §11, which already says nearly the same thing — the
question for §18 is whether the spec is being met, not what the spec should say.

**The register at the end.** Zine culture's posture is explicitly anti-institutional (✅ VERIFIED,
[RESEARCH.md §R12.4](RESEARCH.md));
being congratulated by a cheerful app is the wrong emotion at the right moment. The correct feeling is
**quiet seriousness** — *you have made a thing, and it now exists.* The shipped line, *"Your zine is a
book."*, already has exactly that register. ⚠️ The design documents do not: DESIGN-LANGUAGE §10, VOICE
and EXPERIENCE-MAP all still specify confetti and *"Your zine is ready! 🎉"*. See §18.3 — **the docs
are stale, the build is right.**

---

## 13. Accessibility

I did not run a TalkBack pass — that is Pass 1 work, done recently and rigorously on this branch. What
I can judge is **whether accessibility currently costs the design anything, and whether the design
currently costs accessibility anything.** Both, right now.

**Accessibility is paying for the design:**

- The twelve-circle row (§4) is the R1-mandated non-gesture path, and it has been given no design
  treatment at all. **This is the clearest case in the app of a correct obligation shipped without a
  design decision** — the obligation is not the problem; treating it as exempt from design is.
- Disabled controls (Redo; the light-theme tray) sit far below any reasonable contrast floor and read
  as broken rather than unavailable.

**The design is paying for accessibility:**

- At least one of the five inks — teal in particular — falls below AA as body text on white
  (✅ VERIFIED, [CHANGELOG](../CHANGELOG.md)), and it is offered with no warning at the point of
  choice. A creative tool may absolutely offer a pale ink; it should say what it will look like on
  80gsm.
- Non-Latin text renders blank (✅ VERIFIED, shipped limitation). For an app pitched as "make something
  small and strange about your life", a user writing in Bengali or Hindi gets an empty page with no
  explanation on the screen where it happens.

🟦 Treat the nudge row as a **design problem with an accessibility requirement**, not an accessibility
problem with a design exemption. And hold R1: hidden gestures in a low-frequency app are both an
accessibility failure and a usability one.

---

## 14. Trust

Zinely is **honest**, which is rarer and more valuable than it sounds, and it is currently **spending
that honesty in the wrong places**.

**Where trust is earned:** *"It looks scrambled on purpose"* — pre-empting the exact moment a user
would think the app is broken. *"printer can't reach here"* — naming a physical constraint most tools
hide until it ruins a print. *"not 'Fit to page'"* — naming the specific dialog setting that silently
destroys the fold. The whole Print & fold sequence writes for a person standing at a printer, confused.

**Where trust leaks:**

- **The app forgets your zine's name** the moment you leave the editor (§5). Small, and corrosive.
- **Words and destinations disagree** — "Back to bench" / "on your shelf"; "Preview" opening "Read".
- **Delete sits in an undivided list with Duplicate**, in a product with no backup (§3).
- **The completion illustration is not your zine** (§6). A trust finding as much as an emotional one:
  at the moment of proof the app shows a stock drawing, and a user who notices will reasonably wonder
  what else is a placeholder.
- **Privacy is asserted twice before the first mark** (§1), against the app's own "once" rule.

---

## 15. Emotional experience

| Moment | Intended feeling | Actual feeling | Why |
|---|---|---|---|
| Open the app | *Here are my things* | *Is this loading?* | 75% empty grid; card art is not your work (§3) |
| Start a zine | *Let's go* | *Paperwork* | A print decision before the first mark (§1) |
| Empty page | *Invitation* | *Being encouraged* | Twee headline + a second privacy reminder (§1) |
| Place a text block | *Craft* | *Configuration* | Twelve circles, a covering panel, a `12 pt` stepper (§4) |
| Read | *Pride* | *Neutral* | No name, no depth (§5) |
| The sheet | *Confidence* | **Confidence** ✅ | The app's best screen (§6) |
| Print step 2 | *Almost there* | *Where's the button* | The real action is the quietest thing (§6, §11) |
| Fold | *Guided* | *Reading a manual* | Static frames of a motion (§6) |
| "Your zine is a book." | *Pride* | *Pride, then a flinch* | Perfect sentence, generic drawing (§6) |

**The pattern is one sentence: the emotional peaks are exactly the places where Zinely shows a diagram
instead of the user's work.** Library card, front/back cover previews, fold illustrations, completion
illustration. Four times, at the four moments that matter most, the app substitutes an abstraction for
the artifact.

That is not a visual-design problem. It is the product's central emotional defect, and it is
correctable without redesigning a single layout.

---

## 16. Things that feel distinctly "Zinely" — protect these

1. **The wordmark.** `Zinely.` with the orange period. ⚠️ [BETA-UX-REVIEW.md](BETA-UX-REVIEW.md)
   recommends dropping the wordmark row entirely. **My adjudication: keep the wordmark, drop the "On
   this device" chip beside it** (§3, §14) — that reclaims the vertical space the review objects to
   without discarding the app's single strongest identity asset. See §18.7.
2. **The display serif.** The reason any screen here looks like print rather than software. Use it
   *more*, and give it a middle register (§7).
3. **The voice on the print screens.** *"It looks scrambled on purpose."* / *"All folds are valleys."*
   / *"Your zine is a book."* Professional interface writing with a point of view, better than the
   visual design it sits inside.
4. **The sheet diagram and its legend.** Best-in-category. Its only fault is showing numbers instead
   of art.
5. **The paper palette** — cream, charcoal, burnt orange, teal — when used as *print* (ink on stock)
   rather than as *theme* (accent on surface).
6. **The tape on the current page thumbnail** and the hand-placed rotation of the page strip. The most
   zine-literate detail in the app, and already codified in R10.
7. **Owning the fold at all.** No competitor takes the user past PDF export. This is the moat.
8. **"Supplies"** as the word for the toolbar. Wrong typography, right vocabulary.
9. **The three-step Print → Fold → Done arc.** ADR-058 bought this. Keep it.

---

## 17. Things that should be completely rethought

**1. The library card must become the cover.** Not a sheet with a fold line, not a decorative mark.
Page 1, rendered, at the proportions of the folded booklet. (Authoritative version:
[BETA-UX-REVIEW.md §3](BETA-UX-REVIEW.md).)

**2. The block-manipulation model.** Twelve circles is not a control strip. Rethink from *what does a
hand do to a piece of paper*, then work out how each affordance is reachable without sight or fine
motor control — in that order, and **without removing the non-gesture path** (R1).

**3. The text-styling model.** A panel anchored on top of the thing it changes is structurally wrong,
and `pt` is the wrong unit for a 74mm page. Report **characters per line** — the unit that actually
predicts whether the page will read. Size-by-drag is worth exploring *as an addition to* the stepper,
never as a replacement (R1).

**4. The moment of birth.** Paper choice should not be the first thing that happens. Default it, start
the user on page 1, and move it to print time. (Naming is already handled — Rename exists in the
overflow sheet; the finding is only about *paper*.)

**5. Motion, from a recorded baseline.** §8's three motions — page turn, spatial continuity,
fold-as-animation — reconciled against the existing DESIGN-LANGUAGE §10 profile rather than proposed
from zero. **Record first, redesign second.**

**6. The completion screen.** It must show **their** cover. Same layout, same sentence, real artwork —
and the button beneath it must go where the sentence says.

**7. The vocabulary.** One name per place — *bench* or *shelf* or *library*. And rename the doorway:
it says Preview and opens Read.

**8. Light theme.** Either a designed surface with real value separation and themed system chrome, or
don't ship it.

**9. Emoji doing icon work.** 🖼️ and ✏️ inside buttons, 🤚 in the coach mark. VOICE already forbids
this ("seasoning, not structure"); the build does it anyway. ✨ in a headline is within the rule and
can stay if the type can carry it.

**10. Print step 2's action hierarchy.** *Save PDF* is what the user came for. It should look like it.

**11. The empty states.** ⚠️ This is the sharpest contradiction in the document — the empty state is
frozen by [ADR-033](DECISIONS.md#adr-033), and my objection to it is a design disagreement, not a
defect report. See §18.1.

**12. Truthful print margins on the canvas.** Draw the **format's** non-printable area on the page, at
the moment of placement — the app already computes it and already draws "printer can't reach here",
one screen too late to help. (My first draft said "query the real printer"; ADR-052 removed the in-app
print path, so there is no printer for the app to ask. The finding survives, the mechanism doesn't.)

**13. And one thing to refuse: a template gallery.** Canva's own reviewers name the failure mode —
templates are "widely used, which can make designs appear generic", forcing you to "adapt content to
pre-existing designs" (✅ VERIFIED, [Typeset](https://typeset.com/blog/canva-vs-adobe-express),
[Penji](https://penji.co/canva-vs-adobe-express/)). For most products that is a drawback. **For a zine
it is a category error: a zine that looks templated is a brochure.** If blank-page fear needs
addressing, ship *structures* — grid skeletons, page rhythms — never finished designs.

---

## 18. Where this critique contradicts a decision that was already made

**This is the section to argue about first.** §§1–17 were written blind; these are the collisions that
surfaced when the design canon was read afterwards. Each row states which side I think wins, and why.
**None of them may be built until adjudicated** — building them as written would silently overwrite a
decision that was made deliberately and reviewed, which is precisely what the
[Documentation Rule](../CLAUDE.md#documentation-rule-mandatory) exists to prevent.

| # | Collision | My call |
|---|---|---|
| **18.1** | **Empty states.** §17 #11 / Top-20 #16 call *"Let's make something cute ✨"* and the ornament tiles "an instruction wearing a costume". [ADR-033](DECISIONS.md#adr-033) (Accepted) locks exactly that: warm copy + sticker cluster + privacy line + no buttons, canonical in VOICE and DESIGN-LANGUAGE §8. | **Unresolved — and it must not be resolved by me alone.** I stand by the *judgement*, but ADR-033 is an accepted decision reached with more context than I had. This needs a superseding ADR or a withdrawal, not a redesign. Note the evidence cuts both ways: the second canonical string, *"A fresh page. What goes here?"* (`10-new-zine.png`), is markedly better than the first and shows the pattern working. |
| **18.2** | **Privacy line.** My first draft said "stop asserting privacy in the UI". [DESIGN-RULES](design/DESIGN-RULES.md) R12 says surface it *"as a gift, once, warmly"*, and ADR-033 keeps it in the empty state. | **Canon wins, and the finding improves.** "Once" is already the rule; the build says it twice (library pill + editor line). The defect is a **rule violation, not a rule to change** — and the fix is to drop one instance, most likely the "On this device" chip (§16.1). Top-20 #20 rewritten accordingly. |
| **18.3** | **The celebratory register.** §12 argues for quiet seriousness and against confetti. DESIGN-LANGUAGE §10, VOICE and EXPERIENCE-MAP all still specify *"Your zine is ready! 🎉"* and a sparkle at export. | **The critique wins, and the docs are stale — not wrong-headed.** [ADR-051](DECISIONS.md#adr-051) already replaced that moment with the quiet staged reveal and *"Your zine is a book."*, which shipped. **The reconciliation work is to update three design documents to match an ADR that superseded them**, and that is a finding in its own right. |
| **18.4** | **Motion profile.** §8 asserts that ~300ms ease-out "reads as stock" and argues for spring physics. DESIGN-LANGUAGE §10 specifies ease-out, 300–400ms screen transitions, 3–5% one-bounce settles, and *"avoid big springs (toy-like)"*. | **Genuine disagreement, and I hold it lightly.** The spec is coherent and its anti-toy instinct is right for this product. My claim is about *custom surfaces feeling default*, not about wanting bounce. **Resolve empirically:** record the app, then argue about frames rather than adjectives. §8 is ASSUMPTION until then. |
| **18.5** | **Emoji.** My first draft said replace *every* emoji. VOICE §2 rule 7 permits *"a single warm emoji… to punctuate a moment of delight"* and forbids emoji as structure. | **Canon wins; my narrow point survives and is stronger.** Emoji inside buttons is *structure* — already forbidden, and shipped anyway. §17 #9 and Top-20 #11 now say exactly that. |
| **18.6** | **Size by gesture.** §17 #3's original phrasing read as replacing the `pt` stepper with a drag. DESIGN-RULES R1 / DESIGN-LANGUAGE §3.2 forbid gesture-only paths (WCAG 2.5.1/2.5.7) — a rule §13 itself endorses. | **Canon wins outright; my draft was internally inconsistent.** Corrected: any gesture is an addition, the stepper stays. |
| **18.7** | **The wordmark.** §16 says "do not touch it". BETA-UX-REVIEW recommends dropping the wordmark row. | **Split, and I have made the call** (§16.1): keep the wordmark, drop the "On this device" chip. That reclaims the space the review wants without discarding the identity. Needs the other document's author to accept or reject it. |
| **18.8** | **Truthful margins from "the real printer".** [ADR-052](DECISIONS.md#adr-052) removed the in-app print path — Zinely hands a PDF to the OS and never sees a printer selection. | **The ADR wins; the finding survives in weaker form.** Corrected to the *format's* non-printable area (§17 #12). |

**Rediscoveries, not discoveries.** These were reached blind and are already documented; the
authoritative statement is the existing one, and this document links rather than restates: the library
card not showing your work ([BETA-UX-REVIEW §3](BETA-UX-REVIEW.md)); Preview/Read naming (conceded in
the shipped [tester package](releases/0.9.0-beta.1.md)); the imposed sheet showing numbers not artwork
([ADR-058](DECISIONS.md#adr-058) Decision 7); "Supplies" as canonical vocabulary (VOICE); tape and
hand-placed rotation (DESIGN-RULES R10); haptics-as-punctuation (DESIGN-LANGUAGE §11); snapping
feedback (DESIGN-LANGUAGE §11); delete needing a gentle undoable treatment (DESIGN-RULES R6).

**That eight of my findings were already known is the most encouraging thing in this document.** It
means the design canon is largely right and the gap is between the canon and the build — a
*reconciliation* problem, which is far cheaper than a redesign problem.

---

## The Top 20 Changes

**Ranked strictly by impact on perceived quality.** Implementation cost, architectural convenience,
and engineering risk are deliberately excluded — they belong in the plan that follows approval of this
document, not in the judgement of what matters.

⚠️ = collides with an existing decision; read §18 before acting.

| # | Change | Why it ranks here |
|---|---|---|
| **1** | **The library card shows the user's cover.** | The shelf cannot answer its own question (§3). Every creative tool that feels premium puts the user's work on its home screen. Nothing else changes the first three seconds as much. |
| **2** | **Every "your zine" illustration becomes the actual zine** — front/back cover previews, fold diagrams, and above all the completion screen. | Four emotional peaks currently show a diagram instead of the artifact (§15). One correction, four payoffs. |
| **3** | **A real page turn in Read** — finger-tracked, damped, interruptible. ⚠️ §18.4 | The product is a folded paper booklet. Apple's removal-and-restoration of the Books page curl is direct evidence this is identity, not ornament (§5). |
| **4** | **Rethink the block-manipulation model; get the twelve circles off the screen** — without removing the non-gesture path. | The most amateur object in the product, sitting in the core loop (§4, §13). |
| **5** | **Spatial continuity as the navigation grammar** — card *becomes* editor, thumbnail *becomes* page. ⚠️ §18.4 | The mechanism reviewers name when they call Craft "published" and Notion "functional" (§8). |
| **6** | **The page becomes a stable physical object** — centred, edged, and it never resizes when a tray opens. | Resizing paper is the deepest metaphor break in the app (§4). |
| **7** | **Text styling that isn't anchored on top of its own effect**, reporting characters per line rather than raw `pt`. ⚠️ §18.6 | A control that hides the change it makes (§4, §7). |
| **8** | **Kill the pre-work paper decision.** Default it; move it to print time. | Creation must be an instinct, not a decision (§1). |
| **9** | **One name per place**, and rename the "Preview" doorway. | "Back to bench" under "on your shelf", going to neither (§10). Cheap; directly attacks trust. |
| **10** | **Print step 2: make *Save PDF* the primary action** and stop calling the next screen "Now fold it". | The user's real next action is the quietest thing on the screen (§6, §11). |
| **11** | **Stop using emoji as icons** — 🖼️ / ✏️ / 🤚 become drawn marks. ⚠️ §18.5 | Enforcing a rule VOICE already sets. Highest visibility per unit of effort on the list. |
| **12** | **A typographic middle register** between display serif and grey sans body. | Every screen oscillates between shout and mumble, filling the gap with bold and colour (§7). |
| **13** | **Record a motion-and-haptics baseline, then close the gap to DESIGN-LANGUAGE §10/§11.** ⚠️ §18.3, §18.4 | Not "add haptics" — *find out what is already there.* §§8 and 12 are unverified, and ADR-051 says more exists than I could see. |
| **14** | **Fold instructions that move**, on the user's own artwork. | Five still frames of a motion, on the app's differentiating screen (§6). |
| **15** | **Reconcile the three stale design documents with ADR-051's quiet ending.** | DESIGN-LANGUAGE, VOICE and EXPERIENCE-MAP still promise confetti the build has already replaced (§18.3). Docs that contradict the product are how the next redesign goes wrong. |
| **16** | **Redesign the empty states.** ⚠️ §18.1 — **needs a superseding ADR before anything is drawn.** | Currently an instruction in costume (§1, §17 #11). The lowest-confidence row in the list, and the one most likely to be rejected. |
| **17** | **Draw the format's non-printable area and the fold-safe zone on the canvas.** ⚠️ §18.8 | Moves the app's best knowledge from the explanation screen to the moment of placement (§9). |
| **18** | **Give delete separation and ceremony**, in a product with no backup — and verify the confirmation R6 requires actually exists. | Undivided list, immediately below Duplicate (§3). |
| **19** | **Make light theme a designed surface** — real value separation, themed system chrome — or don't ship it. | Half the users will never see the app you designed (§2). |
| **20** | **Say the privacy line once, not twice.** ⚠️ §18.2 | The app is breaking its own R12. Drop the chip, keep the gift (§1, §16.1). |

**Just below the line, and cheaper than most of the above:** the **persistent coach mark** that
survived four unrelated screens, and the **stray orange caret** in the first ten seconds of the core
interaction (both §4). Neither is ranked because neither is a *design* decision — they read as defects,
and defects belong in the backlog, not in a critique's priority list. They should be filed today.

**Deliberately unranked:** library sort/search/grouping (§3). It is a real cliff at thirty zines and
irrelevant at three; ranking it by perceived quality *today* would overstate it.

**Deliberately *not* on this list:** navigation restructuring, an infinite canvas, a template gallery,
a sticker or asset store, glassmorphism, dynamic colour, and any celebratory animation. Each is argued
against in §§10, 12 and 17. The two strongest arguments — Apple Photos iOS 18 and Google's own
expressive-vs-baseline usability finding — say the same thing: **change the surface, keep the
structure.**

**Also removed from the ranking on review:** *named print processes with a per-export seed*
(photocopy / riso / newsprint, each with an intensity dial). It is the most strategically interesting
idea the research produced and it does not belong here — it is a **feature proposal**, not a judgement
about the shipped build, and it is the only candidate row not derived from an observed defect. It is
recorded in [RESEARCH.md §R12.5](RESEARCH.md)
and belongs in a roadmap conversation.

---

## What happens next

1. **Close the evidence gaps** — a screen-recorded pass for §§8 and 12, and a photo/Reframe pass for
   §4. Neither the motion rows nor the manipulation-model row should be approved before these exist.
2. **Adjudicate §18**, row by row. This is the gate. Rows 18.1 and 18.4 in particular need a decision
   from the person who owns the canon, not from me.
3. **Fix the stale documents** (§18.3) — that one is unambiguous and can proceed immediately.
4. **Then, and only then**: HTML specifications for the accepted changes, per the
   [HTML-first UI workflow](../CLAUDE.md#html-first-ui-workflow-mandatory) — prototype, critique,
   freeze, and only after the freeze, Compose.

**No implementation until this is approved.**
