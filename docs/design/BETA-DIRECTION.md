> ## ⚠ SUPERSEDED by [ZINE-DIRECTION.md](ZINE-DIRECTION.md) (2026-08-15)
>
> Folded in whole, including every reconciled review correction. Kept for its working — the reasoning
> trail, the falsified claims and the corrections — but **it is no longer authoritative**. Decisions,
> the capability map and the implementation sequence live in `ZINE-DIRECTION.md`.

# Zinely — beta direction, decided

**Status:** Decisions, made · 2026-08-15 · Implementer acting as Art Director / Product Owner under owner delegation
**Companions:** [ZINE-WORLD.md](ZINE-WORLD.md) (art-direction research) · [PRODUCT-DIRECTION.md](PRODUCT-DIRECTION.md) (product research)
**Authority:** this document *decides*. Law still lives in [V2-CONSTITUTION.md](V2-CONSTITUTION.md), [V21-SPEC.md](V21-SPEC.md) and [DECISIONS.md](../DECISIONS.md). Where this document changes law, it says so and drafts the amendment.

> ### What "beta" means here
>
> **Beta is the stage we are in, not the quality bar we are aiming at.** We are putting a substantially
> complete product in front of real users to test it — we are not shipping a deliberately reduced one.
>
> The test for every item below is: **if a real person used this, would it feel like a serious, complete
> creative product that happens to still be in beta testing?** Not *"is it acceptable for a beta?"*
>
> Consequently, **"it's fine for beta" is not a valid reason to defer anything.** Three reasons are valid,
> and every deferral in this document names which one it is:
>
> | Valid reason | Meaning |
> |---|---|
> | **Unnecessary** | A complete Zinely does not contain it. Usually it belongs to a different product |
> | **Premature** | Real and probably good, but not yet validated enough to bind the document model or the design language to it |
> | **Incorrect-as-built** | It would ship a defect — wrong output, unreproducible documents, a broken claim |
>
> An earlier draft of this document deferred **six** items on the invalid reason. Those six are now in scope,
> marked **↑ RAISED** where they appear.

---

# 0. The working constitution

*A decision tool, not a manifesto. If you are about to build something, read this page and nothing else.*

**What is Zinely?** A small press that fits in one hand. You make an eight-page zine from photos, words and marks, and print it on one sheet of ordinary paper.

**Who is it for?** One person, on a phone, who wants to make something and hold it. Not a designer. Not a team.

**What problem does it solve?** Everything that makes a zine is already on your phone, and there is no way to get it onto paper without a desktop, an account, or knowing what imposition means.

**What is the world?** A small press: **a Shelf** (finished copies), **a Bench** (the lit work surface), and **the press run** (the sheet comes off, you fold it). Every surface is one of those three or it is mis-homed.

**What does DIY mean here?** *Intentional* imperfection with a named physical cause. Tape is tilted because a hand stuck it down. It never means messy, retro, or deliberately awkward.

**What does professional mean here?** Nothing is inert, nothing is a dead end, nothing explains itself twice, every control is hittable and announced. Personality is never an excuse for an unfinished surface.

**The six-step priority order.** User value → usability → coherence → quality → distinctiveness → delight. **Never reverse it.** A beautiful interaction that harms a core task is not a good interaction.

**Every new feature must answer four questions before any pixels:**
1. Which of the three places does it live in?
2. Which of the five materials is it made of? *(paper · ink · tape · stamp · room)*
3. Which of the four motion causes moves it? *(set down · pressed · drawer pulled · mark made)*
4. What does it take away?

**The anti-drift clause.** A feature may not introduce a colour role, a line style, a press tier, a typeface, or a motion curve. If it needs one, that is an amendment to [V21-SPEC.md](V21-SPEC.md) and an ADR — not a feature with a stylesheet.

**The one line that settles most arguments:**

> **Put the handmade quality in content, typography, copy, and imperfection you can quantify. Keep it out of controls, hit targets, focus indication, and iconography.**

**How much imperfection?** Roughly 80% coherent system, 20% meaningful irregularity — and the irregularity is always *seeded and persisted*, never re-rolled. Randomness may set an initial value; it may never be a running behaviour.

**What never gets added:** accounts · cloud · networking · a feed · collaboration · an asset marketplace · anything that authors *for* the user. See §6.

---

# 1. The world — decided

## 1.1 What Zinely's world is

> # Zinely is a small press that fits in one hand.
>
> **The café is how it feels. The press is what it is.**

"Small press" is not a metaphor I invented — it is the actual publishing term for a one-person independent publisher, and it carries both meanings the product needs at once: **the machine** (something comes off it) and **the institution** (it is yours, it is tiny, it answers to nobody).

The world has exactly three places, and the app already has exactly three navigation destinations:

| Place | Route | What it is | The user's question |
|---|---|---|---|
| **The Shelf** | `HomeRoute` | Where finished copies are kept | "Which zine do I want?" |
| **The Bench** | `EditorRoute` | The lit work surface. One sheet at a time | "How do I change this page?" |
| **The Press run** | `ProofRoute` | Where the sheet comes off and becomes an object | "How do I print it correctly?" |

## 1.2 Why this metaphor, and not the others

I did not pick it for flavour. It is the only candidate that survives all five tests the evidence imposes.

| Test | Café | Studio | Desk | Scrapbook | **Small press** |
|---|---|---|---|---|---|
| Explains the app's own vocabulary — *"paper, ink, presses, shelves, folds"* ([`V2-CONSTITUTION.md:77`](V2-CONSTITUTION.md)) | ✗ not one café noun | partly | partly | ✗ | **✓ every noun** |
| Explains why the north star is a **verb** — *"FINISHING. One word"* ([`zinely-constitution.md:27`](../zinely-constitution.md)) | ✗ | ✗ | ✗ | ✗ | **✓ its entire purpose** |
| Explains ADR-090 — the artifact is lit, the room may go dark | ✗ | partly | partly | ✗ | **✓ the sheet is under the lamp** |
| Survives one focal zone (a phone has no peripheral vision) | ✓ | **✗ fatal** | ✓ | ✓ | **✓ a bench, not a room** |
| Answers *"what is a Type bar, here?"* | **✗ unanswerable** | vague | ✓ | ✗ | **✓ a tool you picked up** |

The café was doing two jobs — supplying the *feeling* and supplying the *place* — and it is only good at one. **It keeps the job it is good at.** Quiet, warm, unhurried, private, yours: that is the register, and it remains constitutional. What it never supplied was an answer to "what is this screen?", which is precisely why surfaces drifted apart.

"Studio" fails hardest and most usefully: a studio is a room you look *around*, and the physical research is blunt that peripheral vision covers a square metre of desk while a phone has one focal zone. **A bench is a studio scaled to a phone.**

## 1.3 What it means for the visual language

| Rule | Consequence |
|---|---|
| **The sheet is under the lamp** | Light = the thing you are making. Dark = everywhere you are standing. No third case. Tools are never paper, however close they float |
| **Five materials, no sixth** | paper · ink · tape · stamp · room. Each names a physical cause or it does not ship |
| **Shadows are printed, not elevated** | Zero blur, down-right, `inkLine`. The object moves under your finger |
| **The line alphabet is readable** | Dashed ink = a boundary. Dashed jam = a cut. Solid leaf = your next move. Solid cream = a thing that moves |
| **Tilt means at rest** | ±0.6–2° when set down, exactly 0° when in your hands. Already true on device; now law |
| **Inks are named and finite** | No colour wheel, anywhere. Same principle now extends to type (§3.4) |
| **One `--frame` ring per screen** | On the one primary action. Riso misregistration, doing a job |

## 1.4 What it means for interaction

- **Tools live in fixed positions at thumb reach.** A printmaker reaches without looking; that requires the ink tool to always be in the same place. Never move a tool for layout convenience.
- **Press-and-lift is the stamp.** Tap is the phone's native verb; press-lift is the stamp's. They are the same gesture, and that is the product's luckiest coincidence.
- **The surface is never cleared.** Reopen and the bench is exactly as you left it, including the mess.
- **Two-finger place.** Rotate + scale + drag at once. A mouse physically cannot do this — it is the one operation where a phone beats a desktop, and collage consists of it.
- **Placement stays reversible.** Nothing commits until the press run.

## 1.5 What it means for motion

Four causes. Nothing else ships.

| Cause | Where | Behaviour |
|---|---|---|
| **Something was set down** | page settle, element drop, sheet close | Decelerate ~300ms, **no bounce** — paper is damped, not rubber |
| **Something was pressed** | every button | Hard-shadow press, ≤100ms, instant return |
| **A drawer was pulled** | sheets, popovers, panels | Translate from the edge it belongs to, 200–250ms |
| **A mark was made** | selection, guides, boundaries | Appears **drawn**, ≤100ms, **no opacity ramp** |

**Banned by physical cause:** parallax (nothing in a press has depth of field) · idle/floating loops (nothing on a bench drifts) · spring overshoot (paper does not bounce) · cross-fades between screens (you do not dissolve between rooms).

The fourth cause is new and is the one worth having: **a selection outline that fades in reads as software; one that appears at once reads as a pencil line.** It is also cheaper.

## 1.6 What it means for terminology

One name per concept, and the press supplies the name.

| Concept | Say | Never say |
|---|---|---|
| The colour of text | **Ink** | Colour |
| Adding words | **Add words** | Text, Add text |
| Adding a photo | **Add a photo** | Image, Media |
| Checking before printing | **Proof** *(verb: "Proof it")* | Preview |
| Producing the file | **Print & fold** | Export, Generate PDF, Save PDF |
| Sending it to someone | **Send a copy** | Share, Share & export |
| The finished thing | **A zine** | Project, Document, File |
| The work surface | **The bench** | Canvas, Editor, Workspace |
| Where zines live | **Your shelf** | Library, Home, My documents |
| Graphics | **Supplies** *(tape, stamps, cut paper)* | Stickers, Assets, Elements |

**"Supplies" is now real.** The empty page has been pointing at it since before it existed ([`Copy.kt:320`](../../core/copy/src/main/kotlin/com/aritr/zinely/core/copy/Copy.kt), *"from the supplies below"*), and [`SCREEN-INVENTORY.md:112`](SCREEN-INVENTORY.md) specified a supply tray that was never built. §3.5 builds it.

## 1.7 What it means for future features

Every future feature is **a new supply, a new mark, or a shorter path to the press run.** If it is none of those three, it belongs to a different product.

That single sentence rejects, without further debate: feeds, profiles, collaboration, templates-as-a-marketplace, AI layout, filters-as-a-tab, and anything with a follower count.

## 1.8 What this metaphor does **not** mean

This section exists because metaphors get over-applied, and that is how a design language turns into a costume.

- **It does not mean drawing a press.** No machinery, no levers, no wood grain, no rendered rollers. The metaphor governs *behaviour and vocabulary*, not illustration.
- **It does not mean simulating paper.** No paper texture PNG, no deckle edges, no page-curl. A phone renders a 3mm simulation of 120gsm stock that nobody perceives and that costs contrast. **Tactility is earned by the printed object, never faked on glass.**
- **It does not mean print jargon.** The maker never learns *bleed*, *imposition*, *gutter*, *signature*, or *creep*. The press does that work silently; that is the whole point of owning one.
- **It does not mean irreversibility.** Real paste-up gets one copy. Software users expect undo, and removing it is not authenticity, it is hostility.
- **It does not mean everything must be handmade.** Controls stay boringly conventional. The handmade lives in the content.
- **It does not license retro.** A small press in 2026 is a current object, not a nostalgic one.

## 1.9 The constitution amendment

Ready to paste into [`V2-CONSTITUTION.md`](V2-CONSTITUTION.md) §VI, and to land as **ADR-103**.

> ### Amendment 2 — the spatial metaphor (2026-08-15)
>
> §I's felt promise named a single image — *"a quiet café where you make tiny books with your hands"* — and that
> image has been carrying two jobs: the product's **emotional register** and its **spatial metaphor**. It is
> authoritative on the first and silent on the second, which is why four documents named four different places
> and no document could answer *"what is this screen?"*
>
> **The register is unchanged and remains binding: quiet, warm, unhurried, private, yours.**
>
> **The spatial metaphor is now named: Zinely is a small press that fits in one hand.** It has three places —
> the **Shelf** (finished copies), the **Bench** (the lit work surface), and the **press run** (where the sheet
> comes off and is folded). Every surface belongs to one of the three. A surface that belongs to none is
> mis-homed and is redesigned or removed.
>
> This amendment reconciles rather than overrules. It is required because §I's image cannot answer the object
> question, while `zinely-constitution.md`'s north star — *"FINISHING. One word."* — and §II's vocabulary law —
> *"paper, ink, presses, shelves, and folds"* — both already describe a press. The metaphor names what the
> corpus and the shipped code were already doing.
>
> **Consequent:** [`DESIGN-LANGUAGE.md`](DESIGN-LANGUAGE.md) is marked **superseded** in full. Its craft-table
> metaphor (`:64-65`), coral-on-charcoal palette (`:71-73`), tilt-and-tape licence (`:74-75`), marker-face
> typography (`:78-80`) and overshoot motion (`:208-213`) have each been overruled elsewhere without the
> document ever saying so.

---

# 2. The product as one experience

I walked the fourteen steps on the device (SM-A176B, Android 16, dark theme, 2026-08-15). **It is closer to one product than I expected, and it breaks in a small number of specific, nameable places.**

## 2.1 What already works, and should be the model

**The Proof screen is the most coherent surface in the app.** The page tilts because it is at rest. `COVER · 1 OF 8` is a stamped dashed tag. A butter tape strip physically attaches the commit band. *"8 pages · one sheet, one cut · US Letter"* is concrete and honest. `Save PDF` carries the screen's one ring; `Share` is correctly demoted.

**Every other screen should be judged against Proof, not against a mockup.**

Also genuinely working: the Shelf's tilted taped cover with the `US LETTER` stamp straddling its edge · the printed `1 / 8` on the page · the dashed-ink selection with square handles · the empty page's three stamp tiles · the fold act's line legend.

## 2.2 The discontinuities, ranked by how much they break the illusion

| # | Break | Where | Category |
|---|---|---|---|
| 1 | **Two current-page colours.** `BenchPageNav` and `ProofFold` use `berry`; `BenchPageGrid` uses `leaf` — one inch apart, same concept | frozen `v21-bench.html:444` | B → needs amendment |
| 2 | **Two icon grammars.** World objects (tape, stamps, the fold legend) coexist with Material's rounded-square icon tile in `ZineActionSheet` and `BenchAddChooser` | Compose | **C — mine** |
| 3 | **Two dead controls.** `Font` and `Replace` are drawn and permanently inert | ruled by OD-9 | **C — mine, §3.4** |
| 4 | **The page grid draws no page content** — eight blank cards with numbers, above a strip showing real thumbnails | ruled `.pgcell` | B → re-examine |
| 5 | **"the supplies below"** points at a screen that does not exist | `Copy.kt:320` | **C — mine, §1.6** |
| 6 | **Four rows of chrome** below the page on a screen whose principle is *"the page is the hero; the tool is a guest"* | Compose | **C — mine** |
| 7 | **The fold diagram is dark** — the most literal artifact in the app, rendered brown while the user holds white paper | Phase D deferred | A → sequencing |
| 8 | **The `⋮` kebab** — Android furniture in a room with no other Android furniture | Compose | **C — mine** |
| 9 | **`Bring forward` is 10px wide** and reports `enabled=true, clickable=false` — on `EditorContextBar`, whose sibling `BenchContextBar` already landed the fix | Compose | defect |
| 10 | **Terminology drift** — Ink/Colour, Text/Add words, Preview as a noun, Share & export | `Copy.kt` | **C — mine, §1.6** |

## 2.3 The one structural gap the walkthrough exposed

Step 14 is *export/share*. **But the loop does not end there — it ends when the user is holding a folded booklet.** The app knows this (the fold act exists and is good) and then hides it behind a modal drawer at the end of a screen most users will treat as the finish line.

**Decision: the fold act is promoted from a drawer to the end of the press run.** After a successful save, "Fold it up" is the primary continuation, not a secondary icon in the top bar. This is a one-screen change that makes the product's whole thesis legible.

---

# 3. Product completeness — audited and decided

**The core loop:** create → compose → edit → save → reopen → continue → **print → fold**.

Audited against "would a reasonable person expect this from a creative app at launch?" — not against Canva.

## 3.1 Documents — complete, ship as is

create ✓ · rename ✓ · reopen ✓ · delete ✓ (with undo) · duplicate ✓ · persistence ✓ · recovery ✓. **No beta gap.**

## 3.2 Pages — one gap

add ✓ · remove ✓ · navigate ✓ · duplicate ✗ · **reorder ✗**

`Intent.Reorder` is element z-order, not pages ([`Intent.kt:99`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/Intent.kt)) — `ReorderOp` operates on `Page.elements`. Page-level intents are only `GoToPage` / `AddPage` / `DeletePage`. There is no way to move page 5 before page 3.

> **DECISION: page reorder ships — and it implements the freeze rather than adding to it.** [`v21-bench.html:820`](mockups/v21-bench.html) already specifies the page grid as *"The whole zine at once — tap to jump, **drag to reorder**"*. The frozen spec has been promising this the whole time; only the code is missing.
>
> **DECISION: page duplicate ships. ↑ RAISED** — the earlier "marginal in a fixed 8-page fold" reading was wrong, and the fixed fold is exactly what makes it useful: with a *fixed* page count you are not adding pages, you are **repeating a layout you already made** — the same frame, the same caption position, the same tape. That is how zine spreads are actually built. It rides the reorder long-press menu at one extra item, and reuses `PlaceCommand`, which [`Command.kt:48-49`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/Command.kt) already notes *"generalises to Duplicate"*.
>
> ⚠ **Unlike reorder, page duplicate is not in the freeze.** It is a feature addition to a frozen surface and takes the same explicit amendment that [row 40](#4-the-decision-table) gets — not a silent one.
>
> Because the fold is fixed at 8, **Duplicate means "copy this page's contents onto another page"**, not "insert a ninth page". The grid asks which page receives it, and warns if that page is not empty.

## 3.3 Images — one gap, and it is a dead control

import ✓ · reframe ✓ · resize ✓ · position ✓ · rotate ✓ · delete ✓ · multiple ✓ · **replace ✗ (drawn, disabled)** · **camera ✗**

`Intent.ReplaceImage` **already exists in the reducer and is dispatched from nowhere.** What it needs is a picker bound to an existing element.

> **DECISION: Replace ships.** This closes D-038, which was logged as an owner capability question — I am the owner of it now, and the answer is yes. Swapping a photo without losing its frame, crop and position is a basic expectation, and the reducer is already waiting.

> **DECISION: take-a-photo ships. ↑ RAISED**
>
> Verified absent: `ACTION_IMAGE_CAPTURE` appears nowhere in `src/main`. Import is gallery-only, so the one input device the user is *literally holding* is unreachable, and the workflow is "leave Zinely → open Camera → shoot → come back → find it in the picker."
>
> This is the cheapest item in the whole document and one of the most product-defining: **`ACTION_IMAGE_CAPTURE` via `ActivityResultContracts.TakePicture`. No `CAMERA` permission is required** when the system camera app does the capture — which also keeps the permission list honest. And **the `FileProvider` is already declared** (authority `${applicationId}.fileprovider`, `@xml/file_paths`), so this needs a path entry, not a provider.
>
> It pairs directly with the photocopier filter (§3.11): *shoot it, dither it, print it* is the loop that makes Zinely feel like a zine tool rather than a photo-layout tool.
>
> ⚠ **Freeze impact:** [`v21-bench.html:825-829`](mockups/v21-bench.html) draws the add chooser. ⚠ **Corrected 2026-08-18:** this said `:773` (which is `bin={node:sel…}`, not the chooser) and said the chooser was fixed at **Text / Photo**. It now has **three** rows — `Art` landed under [ADR-105](DECISIONS.md#adr-105) S7 — so the sentence was right when written and is not right now. A **fourth** entry is a feature addition and needs an amendment — shared with the Supplies tray (§3.5), which amends the same chooser.

## 3.4 Text — one gap, and it is the more interesting one

create ✓ · edit ✓ · resize ✓ · style (bold/italic) ✓ · position ✓ · alignment ✓ · ink ✓ · **font ✗ (drawn, disabled)**

OD-9 ruled that *"a control the freeze draws is kept drawn and invents nothing,"* and a disabled control is the honest rendering of that. **That was the right call for a parity phase and it is the wrong call for a beta.** A permanently inert button reads, from the user's chair, as *"we forgot to finish this"* — which §2 of the brief names explicitly as a launch blocker.

Two ways out: delete the control, or make it work. **Make it work** — but not for the reason I first gave.

> ⚠ **Correction — the asset cost is NOT already paid.** I claimed the three faces are bundled and embedding. Verified false:
> - [`render-android/src/main/assets/fonts/`](../../render-android/src/main/assets/) contains **only Inter** (Regular / Bold / Italic / BoldItalic) + `OFL.txt`.
> - [`DocumentFontRegistry.kt:102-113`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/DocumentFontRegistry.kt) registers **one family: Inter**, and its own KDoc says document typography and UI typography are *"deliberately separate… The two are not merged."*
> - Averia and Fraunces live in [`core/ui/src/main/res/font/`](../../core/ui/src/main/res/) — **app chrome only, embedded in no PDF.**
> - `DocumentFontFamily` requires **four real static TTFs per family, never synthesised** (ADR-024, minSdk 24). Averia ships regular + bold; Fraunces ships regular/medium/semibold. **Neither has an italic or bold-italic.**
>
> **Real cost of X1:** source, subset, licence-check and add **8 static TTFs** to `render-android/assets/`, 2 registry rows, and `FontCoverageGuardTest` work. Both faces are OFL and the statics exist upstream, so it is ordinary work — but it is a day or two, not an afternoon. The one part that *is* free: `TextStyle.fontFamily: String` already exists ([`Document.kt:115`](../../core/model/src/main/kotlin/com/aritr/zinely/core/model/Document.kt)), so **no schema migration.**

> **DECISION: font choice ships anyway, limited to three faces, named in the world's language.**
>
> The corrected cost does not change the decision, because the reason was never "it's cheap" — it is that a permanently dead control is a launch blocker, and the typography law already defines exactly three roles ([`V21-SPEC.md:436-443`](V21-SPEC.md)): Averia Sans Libre (*the maker's own words*), Fraunces (*editorial*), Inter (*work*). Two of the three are already *intended* for user content and merely never reached the document renderer.
>
> **And it applies the named-ink principle to type: named voices, not a font list.**
>
> | Shown | Is | Because |
> |---|---|---|
> | **Handwritten** | Averia Sans Libre | The imperfect face — headings, short bursts, your own voice |
> | **Storybook** | Fraunces | The reading face — captions, longer passages |
> | **Plain** | Inter | Clean and neutral — labels, lists, anything that must be legible small |
>
> Three named voices, exactly as five named inks. **Never a font picker.** The typography law's binding rule — *"the imperfect face never sets running text"* — is preserved by a size cap on Handwritten, not by removing the option.
>
> ⚠ **Freeze impact:** [`v21-bench.html:564`](mockups/v21-bench.html) draws the `Aa Font` chip *(was `:514`, corrected 2026-08-18)*, so *making the drawn chip work* is a permitted parity fix. The three-voice selection surface it opens is **not** in the freeze and needs an amendment.

## 3.5 Graphics — the missing third primitive. **Ships.**

> **DECISION: GRAPHIC ships in the beta.** Not because composition tools have shapes, but because of a coherence argument specific to Zinely:
>
> **The app's own visual language is built from tape, stamps and cut paper — and the user cannot use any of them.** Tape holds the Proof band on. A stamp labels the cover `US LETTER`. Cut-paper tags carry `COVER · 1 OF 8`. The product decorates itself with a vocabulary it withholds from the person using it. That is not a missing feature; it is the product contradicting itself.
>
> Without it Zinely is a text-and-photo editor. With it, it is a composition tool.

**The pack: 16 primitives, 4 material families. ↑ RAISED from 12.** The count is still curated, not generous — this repo's own cover grammar works at 8; Truchet at 1; Recursive curates infinity to 64; NN/g shows a 100-item picker inflates time-on-task over 500%. But the earlier 12 was three *decorative* families with **no plain shapes at all**, and a composition tool where you cannot draw a rectangle, a circle or a rule is incomplete in a way no amount of nice tape fixes. Colour blocks and dividers are the two most common things anyone reaches for.

**Tape & fixings** — torn tape strip · photo corner · staple · paper clip
**Stamps & marks** — star/asterisk · arrow · halftone dot cluster · registration cross
**Cut paper** — torn strip · cut-out window frame · cut label/speech tag · marker underline
**Cut shapes ↑ NEW** — rectangle · circle · triangle · straight rule

The fourth family is not a betrayal of the metaphor: these are **shapes cut from coloured paper with scissors**, which is exactly what a paste-up artist does and exactly what the named inks are for. They keep the torn/cut edge quality of the family they sit in — a Zinely rectangle is a cut rectangle, not a `RoundedCornerShape`.

Each survives *"what physical object made this mark?"* None is a mood. Nine of sixteen are asymmetric, so mirror earns its place. All are single-ink coverage, so the named inks multiply cleanly.

### The blast radius, corrected

A third sealed `Element` subtype is the only document-model change in the plan, and my first estimate was wrong in the two ways that matter most.

| Site class | Count | Behaviour on a third type |
|---|---|---|
| **`error(...)` on an unhandled kind** | **1** | ⚠ **Throws at runtime.** [`BenchContextBar.kt:125`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/BenchContextBar.kt): `BenchVerbKind.DECOR -> error("decor verbs are unreachable until DecorElement is re-seated (OD-2)")`, with a test asserting the throw (`BenchContextBarTest.kt:137`). **The single most dangerous site, and my first draft missed it entirely** |
| Exhaustive `when (element)` | **7** (not 8) | Compile error — safe. `DefaultDocumentValidator.kt:79` · `EditorReducer.kt:66` · `Elements.kt:17,22` · `SceneRenderer.kt:56` · `BenchContextBar.kt:129` · `EditorA11y.kt:31` |
| `as?` casts | **13** repo-wide (6 in `EditorScreen.kt`: 367, 664, 1108, 1127, 1304, 1341) | Silent no-op |
| `is`-guards | **4** — `LivePreview.kt:78` · `EditorA11y.kt:51,57` · `EditorGestures.kt:52` | Silent skip |

⚠ **I had the compile-safety argument backwards.** I wrote that `EditorA11y.label()` *"has no `else`, so a third type ships unlabelled."* The opposite is true: an expression `when` over a sealed type **with no `else` is a compile error** on a new subtype. Absence of `else` is the safe case. The real silent surface is the ~17 cast-and-guard sites, roughly double what I first stated.

**Prior art I missed:** a `DecorElement` concept already exists under **OD-2**, `BenchInkPopover.kt:140` already handles `DECOR`, and [`v21-bench.html:625`](mockups/v21-bench.html) **already draws a decor verb set** (`Replace, Ink, Delete`). The freeze anticipated this primitive. That materially de-risks X7 and partly answers the freeze question — though the Supplies **tray** is still a new surface needing an amendment.

**Transformations that ship:** free rotation · uniform scale at 4 named sizes · mirror (per-primitive capability flag) · ink tint from the named set · layering · bleeding off the trim edge.

**Transformations that do not, ever:** pattern fill / auto-tiling (names no physical cause a person could produce; `V2-CONSTITUTION.md:264` bans decorative textures) · randomised scatter of N copies · non-uniform stretch · procedural generation of new primitives · auto-composition · an opacity slider (translucency is a material property of tape, not a control).

> **DECISION on the starter-pack randomisation idea: DO NOT IMPLEMENT.** The mockup's `Math.random()` placement ([`sticker-picker.html:215`](mockups/sticker-picker.html)) produces non-reproducible documents and breaks `preview == export`. **The rule: randomness may set an initial value; it may never be a running behaviour.** A graphic gets a small tilt derived deterministically from its element id — stable forever, survives reload. No shuffle, no re-roll, no random position. Position is always where the finger went.
>
> Its twelve emoji-glyph stickers die with it (§3.9). The file is marked *deferred*, never built — **nothing is sunk.**

**And "Supplies" is the tray's name**, which finally makes `Copy.kt:320` true.

## 3.6 Composition — one broken control

layering ✗ **broken** · selection ✓ · move ✓ · resize ✓ · rotate ✓ · snapping ✓ · nudge ✓ · **duplicate element ✗** · alignment tools ✗

> **DECISION: layer order is a NOW fix, not a feature — and the fix is smaller than I first said.**
>
> ⚠ **Correction:** I wrote *"there is no send-backward at all."* False. Send-backward is implemented three times over: [`ZOrder.kt:37`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/ZOrder.kt) (`SEND_BACKWARD`), a live `FlipToBack` button at [`EditorContextBar.kt:177`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorContextBar.kt), and a TalkBack custom action at [`EditorA11y.kt:70`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorA11y.kt).
>
> **Both buttons exist and both are clipped.** The defect is layout, not capability, and it lives on `EditorContextBar` — `BenchContextBar.kt:296-309` already landed the deliberate `if (verb.enabled) onClick else disabled()` fix that its sibling never got. For a collage tool this still matters — the screen-printing research is emphatic that **layer order *is* the design** — but N4 is "unclip two existing buttons and give them 48dp targets", not "add the missing half."

> **DECISION: duplicate element ships. ↑ RAISED**
>
> Verified absent from `Intent` — there is no way to copy anything. In a composition tool built on repeated marks (three stars, a row of tape strips, the same caption style on eight pages) that is a hole the user hits within minutes, and the workaround is re-placing and re-styling by hand every time.
>
> It is also nearly free: [`Command.kt:48-49`](../../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/Command.kt) already records that **`PlaceCommand`** *"generalises to Duplicate"*. New id, small offset, same page, undoable by the existing inverse. **A `Duplicate` verb on the context bar** for every element kind — no clipboard, no cross-document paste, no system-clipboard integration.
>
> ⚠ **Freeze impact:** [`v21-bench.html:674-679`](mockups/v21-bench.html) fixes the context-bar verb sets exactly *(was `:623-625`, which is `</div>` / `<script>`; corrected 2026-08-18)* (`text → Edit, Font, Size, Ink, Delete`; `photo → Reframe, Replace, Delete`; `decor → Replace, Ink, Delete`). Adding a Duplicate verb is a feature addition to a frozen surface and needs an amendment. Note the freeze **already draws a third, decor kind** — see §3.5.

> **DECISION: alignment/distribute palettes — DO NOT IMPLEMENT. Reason: unnecessary.** That is layout-tool thinking imported from a different product. Snap guides already exist and are the phone-native answer; a distribute button on a 5.5" screen solves a problem the snapping does not leave behind.

## 3.7 Output

save ✓ · PDF export ✓ · share ✓ · fold instructions ✓ · **page images ✗**

> **DECISION: promote the fold act** (§2.3) — the loop ends when a booklet is in a hand, not when a file is on disk.

> **DECISION: save pages as images ships. ↑ RAISED**
>
> The earlier reasoning — *"the loop ends in paper, and a second output format dilutes the one that matters"* — was product-purism dressed as discipline. **It is the maker's work; refusing to let them get it out of the app is a lock-in, not a principle.** People will want to post a page, text a spread, use a cover as a wallpaper, or keep a copy somewhere that is not a fold-imposed PDF. Every finished creative product lets you do that.
>
> ⚠ **Correction, and it makes this easier, not harder.** I cited the wrong machinery: `core:render` is pure Kotlin and rasterises nothing (`SceneRenderer.render()` returns `List<DrawCommand>`), and `PdfPageRenderer` emits PDF bytes. The class that actually produces a bitmap is [`RasterPageRenderer.kt:19`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/RasterPageRenderer.kt), at 300 px/pt, called from **no `src/main` code**.
>
> More to the point: **a PNG export path already ships end-to-end and is simply unreachable.** [`ZineExporter.kt:175`](../../app/src/main/java/com/aritr/zinely/export/ZineExporter.kt) handles `ExportFormat.PNG -> composer.writePng(...)` ([`SheetComposer.kt:89`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/SheetComposer.kt)); it is dead only because [`ZinelyNavHost.kt:290,310`](../../app/src/main/java/com/aritr/zinely/ZinelyNavHost.kt) hardcode `ExportFormat.PDF`.
>
> But it writes **the imposed sheet** — precisely the printer's artifact that is meaningless as an image. So X12 is *"reach an existing exporter and add a per-page reading-order mode over `RasterPageRenderer`"*: **reading order, one PNG per page, at print resolution.**
>
> The hierarchy stays intact by placement, not by absence: **Print & fold** keeps the ring; *Save pages as images* is a quiet second item under Send a copy.

## 3.8 Quality of life — mostly complete

undo/redo ✓ · persistence ✓ · error recovery ✓ · empty states ✓ · loading ✓ · destructive handling ✓ · haptics ✓ · a11y ✓ *(one defect, §3.6)*

> **DECISION: no new confirmation dialogs.** Delete already uses undo-snackbar, which is the better pattern. Do not add "are you sure?" anywhere.

## 3.9 Assets and licensing — sequence decided

> **DECISION: native curated assets only for the beta. No network. Not now, not opt-in, not later-if-someone-asks.**
>
> The claim Zinely makes is not "works offline" — it is that the app ships **zero networking libraries**, verifiable by anyone inspecting the manifest. NFR-1 (*"no network calls"*) already binds this. A falsifiable claim is a different product from a promise, and `INTERNET` converts one into the other permanently.
>
> **Sequence:** bundled curated supplies (beta) → *if and only if users ask for more* an opt-in, keyword-only, no-user-content library, which needs its own ADR and a PRD scope change ([`V2-CONSTITUTION.md:302`](V2-CONSTITUTION.md) is the only door left open) → **live remote search: never.**
>
> **DECISION: no ShareAlike, and no attribution that must reach the printed artifact.** Zinely recolours (an adaptation) and the PDF leaves the app, so compliance would mean stamping credits into a stranger's zine and attaching a copyleft obligation to their work. This excludes OpenMoji. Source pool: CC0/PD first (Openclipart, Open Doodles, Met CC0), then MIT/ISC/Apache icon sets, fonts under OFL.
>
> **DECISION: draw the sixteen supplies ourselves.** At sixteen primitives the licensing question mostly evaporates, and an authored kit by one hand is the entire point (§1.3). External sources are a *fallback*, not a plan.
>
> **DECISION: no emoji as text, ever.** Android emoji flatten to 64px/em rasters in PDF and differ by OEM — *the same zine exported on two phones would contain different artwork*. For a product promising a reproducible printed object that is a correctness bug. If emoji ever ship, they ship as bundled vectors. **Beta: not at all.**

## 3.10 Multi-page image continuity — partly ships

> **DECISION: all four spreads ship. ↑ RAISED — general spreads were deferred on the invalid reason.**
>
> **All four reading spreads (2|3, 4|5, 6|7, 8|1) meet at a fold, not the cut** — independently re-derived from [`Convention.kt:32-49`](../../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/Convention.kt) and `SingleSheet8Imposer.kt` during review, because everything below rests on it:

```
row 0 (all Rotation.HALF):  col0=5  col1=4  col2=3  col3=2
row 1 (all Rotation.NONE):  col0=6  col1=7  col2=8  col3=1
folds: y=h/2, x=w/4, x=w/2, x=3w/4     cut: y=h/2, x ∈ [w/4, 3w/4] only
```

| Spread | Cells | Shared edge | Is | Rotation parity |
|---|---|---|---|---|
| 2\|3 | (0,3)+(0,2) | `x = 3w/4` | **fold** | both HALF ✓ |
| 4\|5 | (0,1)+(0,0) | `x = w/4` | **fold** | both HALF ✓ |
| 6\|7 | (1,0)+(1,1) | `x = w/4` | **fold** | both NONE ✓ |
| 8\|1 | (1,2)+(1,3) | `x = 3w/4` | **fold** | both NONE ✓ |

The cut separates only `4|7` and `3|8` — **neither is a reading spread**, and both halves of every spread share a rotation, so no image is split across a 180° flip. `clipLocalBounds = panelLocal` (not `safeLocal`), so content may already reach the panel edge. The schema needs no change: two ordinary `ImageElement`s with complementary crops.
>
> The `8|1` wraparound cover is still the one that changes how the object *feels* — it is the difference between a zine that looks made and one that looks printed. But **the mechanism is identical for all four, and restricting it to the cover is arbitrary from the user's chair**: "why can I run a photo across the cover but not across the middle?" has no answer a user would accept, and inventing one would be the product explaining itself again (§2).
>
> So: one action, offered wherever the current page is half of a spread. Four spreads instead of one costs a lookup table of adjacent index pairs — *not* four times the work.
>
> **The model: an image property realised as a one-shot action.** Not a `Spread(pageA, pageB)` record — the only page identity in the schema is `Page.index`, which `renumber()` rewrites on every add and delete, so a spread record would silently re-point at unrelated pages. Delete one half and the survivor is an ordinary cropped photo. That degrades correctly; a relationship record does not.
>
> **Not the rejected "spread view".** [`v21-proof.html:25-27,175-179`](mockups/v21-proof.html) rules *"NO SPREAD VIEW… A tablet spread is a scope decision"* — that was a **side-by-side editing surface**, and it stays rejected. This is one image crossing a fold on the printed object, edited a page at a time on the existing bench. Same word, different feature.
>
> **Required, not optional:** suppress the keep-clear cue on the *inner* edge only — those boundaries are interior to the sheet, so the printer's margin does not apply. Get it wrong and you paint a 12mm white stripe through the middle of a "continuous" image.
>
> ⚠ **CORRECTED 2026-08-18 — this paragraph was wrong, and it contradicted this same section four lines above.**
> It claimed that suppressing the inner keep-clear edge "needs a per-edge safe-area concept `ImpositionLayout`
> does not have today" and called it "the only imposition-side work in X11." **There is no imposition-side work
> in X11 at all.** The safe area is *advisory*: `SingleSheet8Imposer` sets `clipLocalBounds = panelLocal`, never
> `safeLocal`; `LayoutValidator` hard-enforces `clip == panel`; and `ZineExporter` clips each panel by
> `clipLocalBounds`. Nothing insets by the safe area at render or export time — which this section already said
> in its own words (*"so content may already reach the panel edge"*) before contradicting it here.
>
> The keep-clear inset is a **drawn cue** — `BenchStudioSurface.keepClearInsetPx` turns the panel-space inset into
> a page-space guide for the maker's eye. So the feared "12mm white stripe through the middle" is not something
> the renderer would paint; it is something a **maker** would leave if the cue told them to keep clear of an edge
> that is interior to the sheet. The fix is one parameter on the cue, in `feature:editor` — a different module,
> and a smaller change than this paragraph budgeted for.
>
> Corrected in place as a **bug fix to a factual claim about the code**, on the precedent SUPPLIES-SPEC §0 set
> when it corrected `v21-bench.html:74`. The *decision* above is untouched; only the cost estimate was wrong.
>
> **The copy:** on the cover the button says **"Wrap this photo around the cover."** Inside it says **"Run this photo across both pages."** The warning is the same everywhere: **"The middle of this photo lands on the fold — keep faces and words away from it."** No gutter, no spread, no bleed.

## 3.11 The Android advantage — one ships, one defers

> **DECISION: share-sheet receive ships. The Clippings Tray defers to LATER.**
>
> Share-sheet receive is one manifest block, zero permissions, and it makes every other app on the phone an input device for Zinely. It is the single highest-leverage item in the entire research.
>
> The Clippings Tray stays deferred on the **premature** reason, not the invalid one: it is a *document-model change* — a per-zine holding area that is neither a page nor an element — proposed from research rather than from anyone hitting a wall. Binding the schema to a new container before a prototype has been through a device pass is how you get a v3 migration you cannot undo. Share-sheet receive works completely without it ("share a photo → pick a zine → it lands on the current page"), and the tray is a refinement of a flow that will exist by then. **Ship the entry point, earn the tray** — and it is first in line for LATER, not parked.
>
> **DECISION: the photocopier filter ships.** A 1-bit dither / halftone transform applied on-device. Zine authenticity *is* the photocopier look, the cheap implementation is the correct one (Floyd–Steinberg over a downscaled bitmap, pure Kotlin, fits `core:render`), and it turns "my phone photo looks like a phone photo" into "this looks like a zine." Highest identity-per-line-of-code in the whole queue.
>
> **DECISION: actual-size preview ships.** Render at true millimetres via `DisplayMetrics.xdpi` — *"hold this against a sheet of paper."* Trivial, and it answers the anxiety every first-time zine maker has. You cannot hold a laptop against paper.

## 3.12 Four things a complete product looks like it needs — re-examined

Raising the bar means re-testing the refusals too, not only the deferrals. These four are the ones that would most plausibly be called incomplete, and each still holds — on a valid reason.

| Item | Verdict | Reason class | Why |
|---|---|---|---|
| **Direct print (`PrintManager`)** | DEFER | **Incorrect-as-built** | The app is about printing, so this looks like the most obvious gap in the document — and it is the one most likely to ship a defect. Android's print pipeline may re-scale or re-margin a `PrintDocumentAdapter`'s output to the driver's imageable area; a zine imposed at exact page geometry and then scaled 96% **still folds, and every fold lands wrong**. The current path (Save PDF → open in the system viewer → print) puts the user in front of the printer dialog where they can see and refuse scaling. Shipping a one-tap path that silently produces subtly wrong booklets is worse than not shipping one. Ships when there is a device-verified fixed-scale path, additive to Save PDF, never replacing it |
| **Canvas zoom / pan on the bench** | DEFER | **Premature** | Verified: `Intent.SetViewport` carries `screenPxPerPt` + `pageOffset`, so the plumbing exists, but zoom is element-level only. Real precision concern — except the page is A6-ish at arm's length and *every* element already supports two-finger scale and rotate in place, plus a nudge row for 1pt work. The precision need is met by a different mechanism, and adding a second zoom would make pinch ambiguous (pinch-the-element vs pinch-the-page) on the app's single most-used gesture. Revisit if a device pass shows people fighting placement |
| **Page background colour / paper tint** | DO NOT IMPLEMENT | **Incorrect-as-built** | Looks like a one-line expressive win, and it is a trap on a home printer: no consumer inkjet or laser prints to the sheet edge, so a "full-bleed" page background prints as a colour panel inside a 5–6mm white margin — visibly wrong, on every page, using a startling amount of ink. If a page wants a coloured ground, it uses a **cut shape** (§3.5), which is honest about its edges. This is a physical constraint, not taste |
| **Templates / starter layouts** | DO NOT IMPLEMENT | **Unnecessary** | The most-requested-looking feature in the category. *"Blank is a peer"* is a standing product principle, and a browsable template gallery converts blank into the failure state. The need behind it — *"I don't know how to start"* — is answered by the empty page's stamp tiles and the supplies tray, which teach by offering material rather than by offering someone else's finished design |

---

# 4. The decision table

| # | Question | Decision | Why | Confidence | Action |
|---|---|---|---|---|---|
| **World** |
| 1 | What is Zinely's world? | **A small press that fits in one hand** | Only candidate that explains the vocabulary law, the FINISHING north star, ADR-090, one focal zone, and "what is a Type bar?" | High | Land ADR-103 + Amendment 2 |
| 2 | Does the café survive? | **KEEP as register, not as place** | It is authoritative on feeling and silent on space | High | In the amendment |
| 3 | `DESIGN-LANGUAGE.md` | **SUPERSEDE in full** | Five of its declarations are already overruled elsewhere without it saying so | High | Header note |
| **Reconciled (do not reopen)** |
| 4 | The island split | **KEEP principle; parity defect** | ADR-102 §12.1 rule + test + opt-out all exist | High | J4 |
| 5 | Rule-of-thirds | **REMOVE (Compose only)** | Absent from `v21-reframe.html`; code already flags it | High | J4 |
| 6 | The two HTML specs | **FREEZE BOTH** | The freeze is what makes the Type bar fix a permitted parity fix | High | J2 |
| 7 | `ZineActionSheet` Delete | **KEEP** — already `jamText` | Finding withdrawn on evidence | High | none |
| 8 | The nudge row | **KEEP** — WCAG 2.5.7 path under OD-11 | Labels, grammar and rationale all exist | High | none |
| **Beta scope** |
| 9 | Page reorder | **IMPLEMENT** | People compose out of order; grid is the natural surface | High | NEXT |
| 10 | Page duplicate | **IMPLEMENT ↑** | The fixed fold is what makes it useful — you repeat a layout, not add a page. Rides the reorder long-press menu | High | NEXT |
| 11 | `Replace` image | **IMPLEMENT** — closes D-038 | Reducer intent already exists; a dead control is a launch blocker | High | NEXT |
| 12 | `Font` control | **IMPLEMENT as three named voices** | Fonts already bundled and licensed; kills a dead control; applies the named-ink principle to type | High | NEXT |
| 13 | GRAPHIC primitive | **IMPLEMENT** — 16 primitives, 4 families ↑ | The app decorates itself with a vocabulary it withholds from the user. Plain cut shapes added — no composition tool is complete without a rectangle and a rule | High | NEXT, ADR |
| 14 | Sticker randomisation | **DO NOT IMPLEMENT** | Non-reproducible documents; breaks `preview == export` | High | none |
| 15 | Layer order | **MODIFY — NOW fix** | 10px button that lies to the platform; layer order *is* the design | High | NOW |
| 16 | Duplicate element | **IMPLEMENT ↑** | Verified absent from `Intent`. Repeated marks are the medium; `PlaceCommand` already generalises to it. ⚠ amends a frozen verb set | High | NEXT |
| 17 | Alignment/distribute tools | **DO NOT IMPLEMENT** — unnecessary | Layout-tool thinking; snap guides are the phone-native answer | High | none |
| 18 | Spreads — all four | **IMPLEMENT ↑** | Identical mechanism to the cover; restricting it to `8\|1` has no answer a user would accept | High | NEXT |
| 19 | Share-sheet receive | **IMPLEMENT** | Zero permissions; makes every app an input device | High | NEXT |
| 20 | Take a photo (camera) | **IMPLEMENT ↑** | Verified absent. Gallery-only import makes the device in your hand unreachable. `TakePicture`, no `CAMERA` permission | High | NEXT |
| 21 | Clippings Tray | **DEFER** — premature | Document-model change with no prototype or device pass; first in line for LATER | Medium | LATER |
| 22 | Photocopier filter | **IMPLEMENT** | Highest identity-per-line in the queue; completes shoot → dither → print | High | NEXT |
| 23 | Actual-size preview | **IMPLEMENT** | Trivial; answers a real anxiety; phone-only | High | NEXT |
| 24 | Promote the fold act | **MODIFY** | The loop ends in a folded object, not a file | High | NOW |
| 25 | Save pages as images | **IMPLEMENT ↑** | It is the maker's work; refusing to let it out is lock-in, not discipline. Renderer already exists | High | NEXT |
| 26 | Direct print (`PrintManager`) | **DEFER** — incorrect-as-built | Driver re-scaling silently mis-places every fold. Needs a device-verified fixed-scale path | High | LATER |
| 27 | Canvas zoom / pan | **DEFER** — premature | Element-level scale + nudge already meet the precision need; a second zoom makes pinch ambiguous | Medium | LATER |
| 28 | Page background colour | **DO NOT IMPLEMENT** — incorrect-as-built | Home printers cannot bleed; it prints as a panel in a white margin. Cut shapes are the honest form | High | none |
| 29 | Templates gallery | **DO NOT IMPLEMENT** — unnecessary | *"Blank is a peer."* A gallery makes blank the failure state | High | none |
| 30 | Confirmation dialogs | **DO NOT ADD** | Undo-snackbar is the better pattern and already ships | High | none |
| **Assets** |
| 31 | Network for assets | **DO NOT IMPLEMENT — ever, in this product** | Zero-networking is a falsifiable claim; NFR-1 binds it | High | none |
| 32 | Asset sequence | **Curated native first** | Sixteen authored primitives make the question mostly evaporate | High | NEXT |
| 33 | ShareAlike sources | **DO NOT USE** | Would attach copyleft to a stranger's zine | High | ADR |
| 34 | Emoji | **DO NOT IMPLEMENT** — incorrect-as-built | Per-OEM rendering makes the same zine export differently on two phones | High | none |
| **Coherence** |
| 35 | Terminology | **CONSOLIDATE to §1.6** | One name per concept | High | NOW |
| 36 | Privacy repetition | **CONSOLIDATE 4 → 1** | `VOICE.md:35` already says reassurance is stated once | High | NOW |
| 37 | Material icon tiles | **REMOVE** | Material's list grammar wearing our colours | Medium | NEXT |
| 38 | The `⋮` kebab | **MODIFY** — long-press + a world affordance | Android furniture in a room with none | Medium | NEXT |
| 39 | Page grid draws no content | **MODIFY** | Reads as *"my pages are gone"* | High | NEXT |
| 40 | Page grid current-page `leaf` | **MODIFY → `berry`** | Contradicts both neighbours and the role law; frozen, so needs amendment + 2 test updates | High | NEXT |
| 41 | Four chrome rows on the Bench | **MODIFY — collapse to three** | *"The page is the hero; the tool is a guest"* | Medium | NEXT |
| 42 | Uppercase tracked labels | **CONSOLIDATE onto `sectionLabel` + declare** | Shipped six times, declared zero times, drifted to seven tracking values | High | NOW |
| 43 | The line alphabet | **DECLARE in V21-SPEC** | Invented twice independently; promote to law | High | NOW |
| 44 | The tilt law | **DECLARE** | Already true on device, never written down | High | NOW |
| 45 | Ink-pot 38dp targets | **MODIFY** | Below R7's non-negotiable 48dp floor | High | NOW |

**Nothing on this table is returned to the owner.** Two items carry residual risk I am accepting: #40 amends a frozen file (breaks two named tests — budgeted), and #12 expands text capability (mitigated by shipping three named faces, not a picker).

**Nine items moved on the corrected bar** — **six raised into scope** (#10, #13, #16, #18, #20, #25) and **three re-justified** on valid reasons rather than "fine for beta" (#21 premature, #26 and #27 incorrect-as-built / premature). See §7 for what the raises actually cost once the review corrected my estimates.

---

# 5. The plan

## NOW — make the current beta coherent and true

*No new capability. Parity, defects, and subtraction.*

| # | Work | Depends on |
|---|---|---|
| N1 | **Measure Type bar chrome contrast on `bench`** — `inkFaint` clears AA by 0.04 and fails on `bench` | — |
| N2 | **Freeze `v21-typebar.html` + `v21-reframe.html`** | — |
| N3 | `TypeBar` + `BenchSnack` take the existing island opt-out; delete thirds from `ReframeOverlay` | N1, N2 |
| N4 | **Unclip both z-order buttons on `EditorContextBar`** (`Bring forward` is 10px and reports `enabled=true/clickable=false`; `FlipToBack` at `:177` has the same problem) and give both 48dp targets. Both already exist — this is not "add send-backward". `BenchContextBar.kt:296-309` has the pattern to copy | — |
| N5 | **Ink pots to ≥48dp** targets | — |
| N6 | **Terminology consolidation** (§1.6) — one name per concept | — |
| N7 | **Subtraction pass** — 4 privacy repetitions → 1, dead constants (⚠ *not* `ERROR_BODY`), the grab handle that promises a gesture the `Dialog` cannot accept, the imposition explainers | — |
| N8 | **Declare in V21-SPEC:** line alphabet · tilt law · stamped-label rule · four motion causes; consolidate onto `sectionLabel` | — |
| N9 | **Promote the fold act** to the press run's continuation | — |
| N10 | Land **ADR-103** (the world) + Amendment 2; supersede `DESIGN-LANGUAGE.md` | — |

## NEXT — required for a strong beta

| # | Work | Depends on |
|---|---|---|
| X1 | **Font as three named voices** | N6, N8 |
| X2 | **Replace image** (closes D-038) | — |
| X3 | **Take a photo** — `TakePicture` + `FileProvider`, no `CAMERA` permission ↑ | — |
| X4 | **Duplicate element** — one context-bar verb over `PlaceCommand` ↑ | — |
| X5 | **Page grid draws page content**; current-page `leaf` → `berry` (frozen amendment + 2 test updates) | — |
| X6 | **Page reorder + page duplicate** — one long-press menu in the grid ↑ | X5 |
| X7 | **GRAPHIC primitive + the 16 supplies.** ADR first. See the corrected blast radius in §3.5 — one runtime-crash site, 7 compile-time sites, ~17 silent-degrade sites. Schema v1→v2 + migrator. PDF export is already vector-capable ✓ | N2, ADR |
| X8 | **Share-sheet receive** | — |
| X9 | **Photocopier filter** | X3 |
| X10 | **Actual-size preview** | — |
| X11 | **Spreads — all four reading pairs** ↑ | X5 |
| X12 | **Save pages as images** ↑ | — |
| X13 | Retire the Material icon tile; replace the kebab | N2 |
| X14 | Collapse the Bench's four chrome rows to three | X5 |

**Sequencing note.** X7 is the only item here that changes the document model, and it is the one everything else must not queue behind — so it starts first and lands last. X3/X4/X12 are each roughly a day and independent; X5 gates the three page-grid items and should precede them.

## LATER — good, and waiting on a stated reason

| Item | Waiting on |
|---|---|
| **Clippings Tray** | *Premature* — HTML prototype + a device pass before the schema takes a new container |
| **Direct print (`PrintManager`)** | *Incorrect-as-built* — a device-verified fixed-scale path; additive to Save PDF, never replacing it |
| **Canvas zoom / pan** | *Premature* — evidence from a device pass that placement precision is actually being fought |
| Hand-it-over Read mode · a colophon on export · Direct Share targets · clipboard paste · a second supplies pack | *Unnecessary for completeness* — each is a refinement of a flow that will already work |

## EXPERIMENTAL — prove before it touches the core

Stylus pressure and tilt · perspective-correct paper scan · camera texture capture · the opt-in keyword-only asset library · QS tile.

---

# 6. What we deliberately refuse

*As important as the plan. A mature product is defined by this list.* Every row is refused as **unnecessary** (a complete Zinely does not contain it) or **incorrect-as-built** (it would ship a defect) — never as "later".

| Refused | Why |
|---|---|
| Accounts, sign-in, profiles | The product is that there isn't one |
| Cloud sync, backup service | Same, and it would end the falsifiable no-network claim |
| Any networking library, at any stage | NFR-1; the claim must stay inspectable in the manifest |
| A feed, sharing to a community, follower counts | Zinely is a press, not a platform |
| Real-time collaboration | Solves a problem one person on a phone does not have |
| An asset marketplace | [`zinely-constitution.md:140`](../zinely-constitution.md): *"Not an asset marketplace"* |
| AI layout, auto-compose, "surprise me" | Article 7 — the machine would author. Materials and scaffolds pass; ghostwriters do not |
| Seasonal packs | A permanent content treadmill that dates the app against the Ten-Year Test |
| Layers panel, blend modes, masks, curves | Mature-desktop features on a screen that cannot show a whole page at judgeable size |
| Alignment/distribute palettes | Layout-tool thinking; snap guides are the phone answer |
| Free colour picker | Inks are named and finite. This is the whole strategy |
| Font picker over arbitrary system fonts | Three named voices. Same reason |
| Opacity/blend sliders on supplies | Translucency is a material property, not a control |
| Pattern fill / auto-tiling | Names no physical cause a person could produce |
| Emoji in text runs | Per-OEM rendering breaks reproducible printed output |
| Full-page background colour / paper tint | Home printers cannot bleed — it prints as a panel inside a white margin, on every page. Cut shapes are the honest form |
| Paper texture, page curl, wood grain | Tactility is earned by the printed object, never faked on glass |
| More confirmation dialogs | Undo-snackbar already does it better |
| Templates as a browsable gallery | *"Blank is a peer."* A gallery makes blank feel like failure |
| Onboarding tour / coach marks | The empty states already teach. Walls of onboarding text are banned by `VOICE.md:182` |

---

# 7. What I am accepting risk on

Three calls I made on judgment rather than proof, stated so they can be checked later:

1. **The metaphor demotes a ratified constitutional image to a register.** I believe it reconciles rather than overrules, and §1.2 gives the five tests. If you read "quiet café" as spatial law rather than emotional promise, Amendment 2 is wrong — but Ruling A and the whole parity queue stand without it.
2. **Font choice expands text capability at beta**, against my own instinct to subtract. I judged a permanently dead control worse than a small, bounded feature, and bounded it to three named voices rather than a picker.
3. **Sixteen supplies is argued from shipped systems, not from a study.** No published threshold for asset-picker browsability exists. If the first device pass shows it feels thin, the fix is a second pack, not a bigger first one.
4. **NEXT is now fourteen items, six of them raised on the corrected bar** — a real scope increase, and the review corrected my cost estimates in both directions. Cheaper than I said: **X12** (a `writePng` path already ships, just unreachable), **X3** (the `FileProvider` is already declared), **N4** (both z-order buttons exist; the defect is clipping), **page reorder** (already in the frozen spec at `v21-bench.html:820`), and **X7** (the freeze already draws a decor verb set, and `DecorElement`/OD-2 is prior art). More expensive than I said: **X1**, which needs 8 static TTFs sourced and subset rather than "the asset cost is already paid," and **X7**'s hidden `error(...)` crash site.
   The honest net: the plan got bigger, and my confidence in *individual* estimates should be lower than the document's tone implied — every "nearly free" claim in the first draft that I had not personally verified turned out to cite machinery that did not do what I said. **If something has to give, the cut order is X12, X11, X6 — never X7**, which is the one that makes this a composition tool.
5. **Three raised items amend frozen HTML specs** (the Duplicate verb, the add-chooser entries for camera and Supplies, the font-voice surface). Each is flagged inline now, but they were silently amending frozen files in the first draft, which is exactly the failure the freeze rule exists to prevent. They land as one amendment with row 40, or not at all.
