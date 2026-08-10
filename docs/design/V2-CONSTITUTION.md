# The Zinely V2 Constitution

> **This is the highest design authority for Zinely V2.** It does not describe screens; it describes the
> truths every screen, every feature, and every implementation decision must obey. When a lower document,
> a design, or a line of code conflicts with this one, this one wins.
>
> **Stability contract.** This document is meant to be stable for *years*. It is amended rarely, deliberately,
> and only by the owner — never as a side-effect of implementation. The frozen screens are the *application*
> of this constitution; this is the constitution itself. **Since 2026-08-10 those screens are the V2.1
> corpus** ([Library](mockups/v21-library.html) · [Bench](mockups/v21-bench.html) ·
> [Proof](mockups/v21-proof.html)), which superseded the V2 trilogy as the canonical design source under
> [ADR-099](../DECISIONS.md#adr-099). The V2 files ([v2-library](mockups/v2-library.html) ·
> [v2-bench](mockups/v2-bench.html) · [v2-proof](mockups/v2-proof.html)) remain the parity target for every
> surface not yet re-skinned. The constitution itself is unchanged by that, except through
> [Amendment 1](#amendment-log).
>
> **Authority chain.** Constitution → frozen HTML specifications → [ADRs](../DECISIONS.md) → implementation.
> Read [COMPOSE-IMPLEMENTATION-GUIDE.md](../COMPOSE-IMPLEMENTATION-GUIDE.md) for how implementation obeys it.

---

## I. Identity

### What Zinely is
Zinely is a **privacy-first, offline-first tool for making small, printable zines on your own phone.** It turns
a blank sheet into a folded little book you can hold. Everything happens on the device; nothing is uploaded.

The feeling we are building is a **quiet café where you make tiny books with your hands** — warm, calm,
unhurried, and yours. The interface is the quiet room; the zines are the warmth.

### What Zinely is NOT
- **Not a cloud service.** No account, no server, no sync, no "your data in our datacentre."
- **Not a social network or a content platform.** There is no feed, no audience, no metrics, no sharing-for-reach.
- **Not a general design/DTP tool.** It is not Canva, InDesign, or a poster maker. It makes *zines*, and it is
  opinionated about that.
- **Not a template gallery.** Starter material exists to get someone *making*, not to be browsed as the product.
- **Not analytics-driven.** We do not measure users to decide what to build. We decide from principle and craft.
- **Not magical.** It never hides what will physically happen when you print and fold.

### The emotional experience
A first-time user should feel **capable within a minute** and **proud within ten**. A returning user should feel
they are **coming back to their own desk**, where their collection has been waiting exactly as they left it. At
the moment of printing, they should feel **certain** — *"I know exactly what will come out of the printer and how
it folds."* Confidence, not surprise, is the target emotion of the whole product.

### What users should remember
Not the UI. They should remember **the little books they made** and **the calm of making them**. If a user
describes Zinely as *"it has a really nice interface,"* we have partly failed; the interface was supposed to get
out of the way. If they describe it as *"it feels like making tiny books in a quiet café,"* we have succeeded.
This is the Handmade Test, and it is the product's north star.

---

## II. Core Principles

Each principle states **what it means** and **why it exists**. They are ranked: when two collide, the earlier one
governs.

### 1. The page is always the hero
**What:** The user's page/zine is the largest, brightest, most central thing on every screen. Chrome frames it;
it never competes with it. **Why:** People come to Zinely to see and shape *their work*, not our controls. A
screen that makes its own UI the focal point has answered the wrong question.

### 2. Chrome stays quiet
**What:** The interface is restrained — few colours, calm surfaces, minimal ornament, controls that recede until
needed. **Why:** A quiet interface is what lets the creations look loud. Restraint in the tool is what buys
boldness in the artifact. (*"Conservative in the tool, bold in the artifact."*)

### 3. Creations carry the warmth
**What:** Colour, texture, and expressiveness live in the user's zines — paper, inks, covers — not in the app's
chrome. **Why:** This is the load-bearing architecture of the whole identity (see
[V2-IDENTITY.md](V2-IDENTITY.md) and the [Living Product Audit](V2-IDENTITY-AUDIT.md)). It means the app can stay
calm for a decade while every user's shelf becomes more *theirs*. The identity **improves with use** instead of
dating.

### 4. Physical metaphors over software metaphors
**What:** We reach for the vocabulary of paper, ink, presses, shelves, and folds — not files, dialogs, layers,
and exports. A page turns like a sheet; a zine sits on a shelf; printing is a press. **Why:** The product's job
is to make a *physical object*. Software metaphors break the illusion that you are making something real.

### 5. Calm over clever
**What:** When a calm solution and a clever solution both work, we ship the calm one. **Why:** Cleverness draws
attention to itself; calm draws attention to the work. Delight comes from felt reliability, not from tricks.

### 6. Honest over magical
**What:** The app never pretends. What you preview is exactly what exports and exactly what prints. It never
shows a fake capability (e.g. a "Print" button that can't really print) or hides a physical truth. **Why:** Trust
is the product's scarcest resource. One dishonest moment ("it lost my work," "that's not what printed") costs more
than a dozen delightful ones earn. (See [ADR-052](../DECISIONS.md#adr-052), [ADR-058](../DECISIONS.md#adr-058).)

### 7. Collections over documents
**What:** Zines are objects in a *collection*, not files in a list. The Library is a shelf; covers are recognisable;
a row reads as a set. **Why:** Ownership and pride come from collecting. "My files" is a chore; "my collection" is
an identity.

### 8. Confidence over explanation
**What:** We design so the user *feels sure* rather than reading instructions. The Proof room reassures ("your
pages, arranged for the fold") instead of explaining imposition. **Why:** A screen that has to explain itself has
usually failed to *show* itself. Confidence is built by seeing your own work, not by being taught the machinery.

### 9. Material consequence over decoration
**What:** Every texture is the believable result of a real material or press — paper tooth, riso grain,
misregistration, recycled fleck. Nothing is added purely for mood. **Why:** Honest materiality reads as craft;
decorative texture reads as kitsch. The rule: *if a texture cannot name its physical cause, it does not ship.*

### 10. Consistency through grammar, not repetition
**What:** Things feel like one product because they share a fixed *grammar* (grid, type pairing, inks, spacing) —
not because they look identical. Variety is expected and encouraged within that grammar. **Why:** Templates freeze
the vocabulary and feel repetitive; Zinely freezes the *grammar* and frees the vocabulary. This is how a shelf of
fifty different zines still reads as unmistakably Zinely (see [Living Product Audit §3](V2-IDENTITY-AUDIT.md)).

### 11. HTML is canonical
**What:** The frozen HTML prototypes are the authoritative design specification. Compose is an *implementation* of
them, never a reinterpretation. **Why:** A single source of visual truth is the only thing that keeps a design
faithful across many implementation sessions. If the HTML and the code disagree, the HTML is right until the HTML
is deliberately changed (see [COMPOSE-IMPLEMENTATION-GUIDE.md](../COMPOSE-IMPLEMENTATION-GUIDE.md)).

### 12. Every screen answers the user's current question
**What:** Each screen answers the one question the user is holding when they arrive — and defers everything else.
**Why:** A correct answer to the *wrong* question reads as a malfunction, not a lesson. This is the exact failure
of the beta "Preview" screen ([ADR-058](../DECISIONS.md#adr-058)).

| Screen | The question it answers |
|---|---|
| **Library** | "Which zine do I want?" |
| **Bench** (editor) | "How do I change this page?" |
| **Read** | "What have I made?" |
| **Proof / Print** | "How do I print it correctly?" |
| **Fold** | "How do I turn this into a booklet?" |

---

## III. Design Invariants

These are the constraints implementation must never violate. Detail lives in the linked specs and ADRs; the
invariant is stated here.

### Typography

> ⚖️ **AMENDED 2026-08-10 — Amendment 1, the only amendment to date.** See [§VI](#vi-amendment). The clause as
> ratified on 2026-07-28 read:
>
> > **Fraunces (voice/display) + Inter (work/body). Permanent.** Fraunces carries headings, titles, and the
> > product's voice; Inter carries UI, metadata, and running work. No third UI typeface. This pairing is fixed
> > across V2.

**Fraunces (editorial) + Inter (work) + Averia Sans Libre (voice). Permanent.** Averia carries headings, screen
titles and the maker's own short strings — the product's *voice*. Fraunces carries long-form editorial: zine body,
captions, pull quotes, guide prose. Inter carries UI, metadata and running work. **No fourth UI typeface.**

**The rule that makes three faces safe rather than amateur:** *the imperfect face never sets running text.* Voice
is for short strings; Work is for anything the user operates; Editorial is for anything long. A violation of that
sentence is a violation of this constitution, not a style preference.

### Spacing
An **8pt rhythm** governs layout. Spacing is calm and generous; the page is given room. Density is never used to
fit more chrome — if something doesn't fit calmly, it is the wrong thing to show here.

> ✅ **Scope ruled by the owner, 2026-08-10 — this clause is *not* amended, it is read as written.**
> V2.1's measured spacing ladder ([V21-SPEC §3.3](V21-SPEC.md)) is `2 · 4 · 8 · 12 · 16 · 24 · 36`, and four
> steps — 2, 4, 12, 36 — are off an 8pt grid. Three readings were put to the owner; the one chosen is the
> narrowest: **the 8pt rhythm governs *layout* — the room a page is given — and never bound sub-component
> insets, hairlines or the gaps inside a control.** The sentences that follow this clause say what it is for
> in its own words: *calm and generous*, *density is never used to fit more chrome*. A 2 px hairline inset is
> not a density decision.
>
> So: **layout spacing is 8pt** (`8 · 16 · 24` carry the ladder's structural steps, and 36 is the corpus's
> published outer step, from Maeve's `4/8/16/24/36`). No clause changes, no pixel changes, and no frozen
> corpus is re-cut for grid purity — which the other two readings would each have required.
>
> ### ⚠️ The mapping under that ruling is falsified, and the question is **open again**
>
> This note originally continued: *"`--gap-hair`, `--gap-xs` and `--gap-md` are sub-component values and out
> of this clause's scope"*, and set its own tripwire — *if a **future** surface uses `--gap-md: 12px` to
> space layout, the ruling has been stretched.* **A third review found the tripwire was already tripped, by
> the frozen corpus, at freeze time.** `--gap-md` is the corpus's workhorse: ~59 declarations, spacing a page
> grid (`.pgg`), the canvas region (`.canvasArea`), panels (`.band`, `.dbody`, `.testcard`), and page-level
> states (`.empty`, `.sh-head`) across all three prototypes.
>
> The distinction that matters: **the owner ruled on *scope* — layout, not insets — and that ruling stands.
> The classification of `gap-md` as an inset was the implementer's, and it is wrong.** It was presented here
> as though it were part of the ruling; it was not. Under the ruling as written, 12px spacing a panel is
> layout, so the corpus is off the 8pt rhythm in ~59 places.
>
> **Owner call, reopened** — the same three readings, now with real evidence under them: the rhythm is a
> *generosity* principle rather than a grid and `12` never conflicted · the corpus is re-cut to the grid
> (a visual change to a frozen design) · the clause is amended to name the real ladder. Recorded at
> [V21-SPEC §8 row 11](V21-SPEC.md) and [ADR-099 §6](../DECISIONS.md#adr-099-review-2). **Nothing in Compose
> may depend on the answer until it is given** — see [ZinelyV21Dimens].

### Motion
**Calm, sparing, and meaningful.** Motion marks the two emotional peaks (e.g. a paper-settle), not every
transition. Animation is **opt-in where it teaches** (the Proof fold), always leaves a **persistent static
end-state**, and always honours `prefers-reduced-motion`. Motion never exists to look impressive.

### Colour
Authority: **[V2-TOKENS.md](V2-TOKENS.md)** (chrome) and **[V2-IDENTITY.md §4](V2-IDENTITY.md) / the frozen
[Bench H4](mockups/v2-bench.html) 10-ink set** (maker inks).
- Chrome = **two brand hues + one consequence colour**: `matcha` (the single "your move"), `strawberry` (sparing
  punctuation), `consequence` red (delete/error only). Warm `paper`/`desk` neutrals. **No fourth chrome hue.**
- Maker inks (the 10-ink Bench H4 set) live in a **`content.*` namespace** — on the *artifact*, never in the
  interface.
- **Dark is re-derived, not inverted** — a warm charcoal room. **Dynamic/wallpaper colour stays off.**

### Interaction
**Direct manipulation with an accessible twin for every gesture.** Touch targets ≥ the platform minimum (canvas
handles 48dp). Every gesture-driven action has a **named custom accessibility action** and a **visible non-gesture
fallback** (e.g. a quiet `⋯`) — *nothing is gesture-only.* Destructive actions are always visible, separated, and
reversible-until-commit.

### Accessibility
**Not optional, ever.** AA contrast (body ≥ 4.5:1) on the ★-marked pairings is **gated in CI**, not eyeballed.
The **platform accessibility tree is the source of truth** — TalkBack reads `AccessibilityNodeInfo`, not the
Compose semantics tree, and the two can disagree (a control passed `assertIsNotEnabled` while telling the platform
it was enabled — [ADR-058](../DECISIONS.md#adr-058) branch). Verify with `adb shell uiautomator dump`, not only
Robolectric.

### Print
**Honest and exact.** Preview == export == read: **one rendering engine, one draw path** (`CanvasReplayer`,
[ADR-028](../DECISIONS.md#adr-028)); there is never a second way to draw a page. **100% actual size**, no fake
in-app "Print" button; home-print hand-off = **Save PDF + Share** (`PrintManager` deferred,
[ADR-052](../DECISIONS.md#adr-052)). Export equals what was previewed — the same single engine
([ADR-028](../DECISIONS.md#adr-028)); the Save-to-Downloads hand-off is [ADR-054](../DECISIONS.md#adr-054).

### Trust
- **Never-silent failure + loss-safe back** — an export failure is always surfaced; leaving never silently loses
  work ([ADR-051](../DECISIONS.md#adr-051)).
- **Persistence-of-place** — the user returns to their collection and their page exactly as they left them; the
  Bench treats this as a build invariant.
- **The page never drifts, reflows, or resizes during editing** — text editing moves the whole page as a rigid
  body and returns pixel-identical to rest. This was a real trust wound and its fix is non-negotiable.

### Data & privacy
**No networking libraries, no analytics SDKs, no cloud, no account.** Offline-first; the document tree is local
serialized JSON ([ADR-003](../DECISIONS.md#adr-003)). The single permitted future network touch — optional online asset
search — sends **only a keyword, never user content**, and the app is fully usable with it off. Any change that
adds network access must justify itself against the [PRD principles](../PRD.md).

---

## IV. Things We Never Do

Explicit anti-patterns. Each is banned because it violates a principle above.

- **Decorative textures** — a texture with no physical cause. (Violates §9.)
- **Fake vintage / global "riso" filters** — a single grunge/grain layer smeared over everything. Riso comes from
  *real* per-layer offset + grain, or not at all. (§9.)
- **Unnecessary chrome** — wordmarks in the steady-state library, counts, badges, toolbars that don't earn their
  space. The 12-app survey found 0/12 brand the resting library. (§1, §2.)
- **Hidden destructive actions** — delete buried in a gesture, unlabelled, or irreversible without warning.
  Destructive actions are visible, separated, and reversible-until-commit. (§6, Interaction.)
- **Unnecessary animation** — motion for spectacle, motion on every transition, motion that can't be reduced. (Motion.)
- **Decorative colour** — colour in chrome that isn't `matcha` (action), `strawberry` (punctuation), or
  `consequence` (error). Warmth comes from *content*, not from painting the UI. (§3, Colour.)
- **Skeuomorphism** — literal coffee stains, deckle edges, torn paper, handwriting fonts, tilted "polaroid"
  frames. Warmth is **structural** (grid, material, restraint), never a costume. (§4, §9.)
- **Feature-first thinking** — adding a capability because it's possible, before naming the user question it
  answers. (§12.)
- **A second draw path** — any rendering of a page that isn't the one shared engine. Preview must *be* export. (Print.)
- **Per-edit render pipelines** — rendering/encoding/caching a raster per zine for a field no composable reads
  ([ADR-069](../DECISIONS.md#adr-069)). Covers are recipe-driven.
- **Reflowing the page while editing** — resizing or re-laying-out the page to make room for a keyboard. (Trust.)
- **Embedding licence-encumbered assets** — CC-BY-SA (viral copyleft), CC-NC, CC-ND, or merchandising-restricted
  sources in a printed, sellable zine. Embedded art is CC0/PD by preference (see [V2-IDENTITY.md §7](V2-IDENTITY.md)).

---

## V. Future Growth — how new features inherit the constitution

Zinely will grow. Growth must **deepen** the identity, never dilute it. Every proposed feature passes the same
inheritance test before it is designed:

> **The Inheritance Test.** Does it (1) keep the page the hero and the chrome quiet? (2) put its expressiveness in
> *content*, not chrome? (3) answer a real user question the user is actually holding? (4) stay honest (preview ==
> export), offline, and private? (5) express variety through *grammar*, not new one-off UI? A feature that can't
> answer all five is redesigned until it can, or it doesn't ship.

| Growth area | How it inherits |
|---|---|
| **Asset packs / stickers / motifs** | Bundled, CC0/PD-first, tintable *coverage not colour* (1 asset × N inks). Live in `content.*`. Never add chrome. |
| **Templates** | Starter material that gets someone *making* fast — one filtering choice, not a gallery to browse. The product is the making, not the catalogue. |
| **Larger zines (16-page, booklets, saddle-stitch, duplex)** | **Staged** onto the frozen Proof room, which already *adapts by page count* — the maker never picks a format. Booklet/duplex ship as the next roadmap stage, honouring print honesty; the flip-edge default is a device-verification item, not a design assertion. |
| **Online asset library** | Optional, opt-in, **keyword-only** request; never user content; fully usable offline. The privacy invariant is not negotiable. |
| **Cover recipes** | Extend the frozen recipe grammar `{ title × ink × mark × paper × motif × layout }`. Recipe-driven, no per-edit render ([ADR-069](../DECISIONS.md#adr-069)). The three layout archetypes are the recipe's frozen set. |
| **Collaboration** | Only if it can be done without a server holding user content and without turning the product into a feed. If it can't inherit the privacy/offline/collection principles, it is out of scope by design — it belongs on the roadmap, not in the product, until it can. |

New features **record their decision as an ADR** ([DECISIONS.md](../DECISIONS.md)) and, if they touch UI, **update
the frozen HTML spec first** — never the reverse.

---

## VI. Amendment

This constitution is amended only by the **owner**, deliberately, as an explicit act — never implicitly through
implementation, and never by a design or engineering session on its own initiative. An amendment names what
changed and why, and (if it touches a frozen surface) updates that surface's HTML spec in the same act. Absent
such an amendment, every statement here is final and binding on all downstream work.

### Amendment log

| # | Date | Clause | What changed, and why |
|---|---|---|---|
| **1** | **2026-08-10** | [§III Typography](#typography) | **A third UI typeface is admitted: Averia Sans Libre, as the *voice* face.** V2.1 ([ADR-099](../DECISIONS.md#adr-099)) re-skins the trilogy onto a handmade design language whose voice depends on a *deliberately imperfect* display face — one generated by averaging handwriting samples. Fraunces can be warm but not wonky, and the wobble is the whole difference between "tasteful" and "made by a person" ([V21-SPEC §3.1](V21-SPEC.md)). Fraunces is **not dropped**; it moves from *voice/display* to *editorial*, which is the role its long-form cuts were always doing. Cost, measured in a built APK rather than estimated: **123.6 KB**, and subsetting cannot reduce it (an earlier 121.9 KB was a local `zlib` estimate and is retracted — see [ADR-099 §5](../DECISIONS.md#adr-099-gaps)). The compensating constraint — *the imperfect face never sets running text* — is written into the clause itself. **Owner's explicit act, requested and ruled on 2026-08-10** after the conflict was surfaced by implementation; the three V2.1 prototypes already render in Averia, so §VI's "updates that surface's HTML spec in the same act" is satisfied by the frozen V2.1 corpus. |

**Evidence for Amendment 1** (recorded because §VI requires the amendment to be the owner's *explicit act*, and
an implementer's summary is not evidence of one). Implementation stopped on finding the conflict and put it to
the owner as a choice, with the alternatives stated: amend §III explicitly · drop Averia and keep the clause ·
let ADR-099 carry the change implicitly. The owner chose **"Amend §III explicitly"** on **2026-08-10**, in the
same session that accepted the V2.1 corpus (*"all looks perfect for the version we are building for. I
accept."*). The amendment was written only after that ruling, never in anticipation of it.

⚠️ **This amendment exists because the conflict was caught, not because it was planned.** ADR-099 was written
claiming *"V2-CONSTITUTION.md survives in full — every principle survives"*, and that claim was **false**: the ADR
bundles a third UI typeface, which §III forbade in terms. The error was found while writing the Compose type
layer, *after* the owner had accepted the ADR on the strength of that very sentence. Recorded plainly, because a
constitution that quietly acquires exceptions is worth less than one that logs them.

---

*Ratified 2026-07-28 at the close of the V2 Design Program. Companion documents:
[V2-IDENTITY.md](V2-IDENTITY.md) · [V2-IDENTITY-AUDIT.md](V2-IDENTITY-AUDIT.md) · [V2-TOKENS.md](V2-TOKENS.md) ·
[COMPOSE-IMPLEMENTATION-GUIDE.md](../COMPOSE-IMPLEMENTATION-GUIDE.md) ·
[COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md).*
