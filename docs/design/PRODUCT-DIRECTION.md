# Product direction — what Zinely should stop doing, and what it should become

**Status:** Research + proposal · 2026-08-15 · **not ratified. Nothing here is implemented.**
**Companion to:** [ZINE-WORLD.md](ZINE-WORLD.md) (the art direction). This document is the *product* half.
**Owns:** the subtraction list, the expansion list, and the ranked queue.
**Does not own:** decisions ([DECISIONS.md](../DECISIONS.md)) · scope ([PRD](../PRD.md)) · phasing ([ROADMAP](../ROADMAP.md)).

---

## 0. The answer to the last question first

> **"If Zinely disappeared tomorrow, what would users miss that they cannot easily get from an existing design app?"**

Every tool it might be compared to — Canva, Figma, Photoshop Express, Over, Unfold, Adobe Express — makes **a file**. A file is the end of their job. It is the *middle* of Zinely's.

> ### Zinely is the only thing on your phone that turns what is already on your phone into something you can hold.
>
> Not a design app that can export a PDF. **A finishing tool.** The imposition engine, the one cut, the eight fold steps, and the moment the sheet becomes a booklet are the product. Everything else is supply.

That is not a positioning slogan; it is what the founding constitution already says the north star is — *"**FINISHING.** One word"* ([`zinely-constitution.md:27`](../zinely-constitution.md)) — and it is the only claim on this list that no competitor is even trying to make. Canva has 141 million assets and cannot tell you how to fold a sheet of paper.

**This answer is also the roadmap filter.** A feature that shortens or strengthens the path *phone → paper object in your hand* is on-strategy. A feature that adds expressive range without touching that path is a nice-to-have, and a feature that competes on element count is how this product becomes "Canva, but brown."

**The two things that would be genuinely missed:**
1. **You do not have to know anything.** No account, no imposition, no bleed, no "which fold?" — you pick photos, and eight steps later you are holding a zine.
2. **It is yours in a verifiable way.** Not "we promise we don't upload" — *there is no networking code in the app*, and a user or a reviewer can check. That is a falsifiable claim, and it is rare enough to be a product.

Everything below serves those two, or it is cut.

---

# PART ONE — REMOVE

## 1.1 The rule, which the repo already wrote

> *"Reassurance is stated once, warmly, as a gift — not a policy."* — [`VOICE.md:35`](VOICE.md)
> *"❌ Walls of onboarding text."* — [`VOICE.md:182`](VOICE.md)

The brief and the code disagree, and **VOICE.md is already on the brief's side.** This is enforcement, not a new policy.

## 1.2 The privacy repetition — 10 live, 4 dead, 1 should survive

| Where | Literal | Verdict |
|---|---|---|
| `ZineShelfEmpty.kt:526` | "Everything stays on your phone — no account, nothing uploaded." | **KEEP — the one survivor.** Lands once, on an empty shelf, before any work exists |
| `Copy.kt:321` → `EditorEmptyState.kt:176` | "works offline · stays on your phone" | **REMOVE** — every blank page, of every zine, forever |
| `Copy.kt:242` → `BenchAddChooser.kt:273` | "From your phone — it never leaves the device" | **REMOVE** — mid-task, third statement |
| `Copy.kt:632` → `ProofScreen.kt:1040` | "… · $paper · stays on your phone" | **REMOVE** (trailing clause only) |
| `Copy.kt:255` → `BenchStatusStrip.kt:127` | "Saved on this device" | **KEEP — load-bearing** (a11y live region; the drawn chip only says "SAVED") |
| `ZineShelfFail.kt:316,325-326` · `Copy.kt:572` · `Copy.kt:418` | recovery reassurance in errors | **KEEP** — reassurance during failure is not marketing |
| `Copy.kt:252,254,499,506` | `SAVED_MARK "✿"`, `SAVED_QUALIFIER`, `ON_THIS_DEVICE`, `KEPT_ON_DEVICE` | **DELETE** — dead constants, zero references |

**Four drawn repetitions → one.** None of these is legally required; the Play data-safety declaration is a console artifact, not UI.

## 1.3 The ten highest-confidence removals

| # | Element | Where | Why |
|---|---|---|---|
| 1 | `"works offline · stays on your phone"` | `Copy.kt:321` | Repeated on every blank page, forever. VOICE.md says once |
| 2 | Dead `Copy.Shelf` constants + `SAVED_MARK` + `SAVED_QUALIFIER` | `Copy.kt:252,254,499,506,507,510,544,545` | Zero references in `src/main`; V1 shelf deleted in `2b6a71b`. ⚠️ **Range corrected after review — `ERROR_BODY` (`:508-509`) is LIVE at `ProofScreen.kt:591`.** Do not delete the block wholesale |
| 3 | `"Open on the bench"` action row | `ZineActionSheet.kt:96` | **Tapping the card already does exactly this.** A second door into the room you are standing in. ⚠️ **Conflict — [ZINE-WORLD §H](ZINE-WORLD.md) calls this row *"perfect world-voice"* and the model for the other four.** Both are true: the *wording* is the model, the *row* is redundant. Resolution: **keep the words, move them.** Retitle the sheet header to the world's voice and cut the row. Do not cut the sentence |
| 4 | The action sheet's grab handle | `ZineActionSheet.kt:436-451` | **It is a `Dialog` (`:208`) with no drag handler anywhere** — `:430` says so outright. A handle promising a gesture the sheet cannot accept is worse than no handle |
| 5 | `"What we've already done"` + the imposition/margin explainers | `Copy.kt:848,850,854` | The user asked *"how do I print it correctly?"*, not *"what did your layout engine do?"* |
| 6 | `"Zinely doesn't print for you. Save the PDF, then open it from Downloads…"` | `Copy.kt:875` | The app disclaiming its own architecture |
| 7 | `"From your phone — it never leaves the device"` | `Copy.kt:242` | Privacy line #3, mid-task |
| 8 | `"Type words onto the page"` | `Copy.kt:240` | Subtitle restating the title *"Text"* |
| 9 | `"1 zine"` / `"$count zines"` | `ZineShelf.kt:374` | Counts what is visibly on screen |
| 10 | `"one sheet"` / `"a little book"` illustration labels | `ZineShelfEmpty.kt:494-495` | Third statement of one fact on one screen |

## 1.4 Decorative elements that communicate nothing

| Element | Where | Note |
|---|---|---|
| ~~Three tilted `CraftCard` glyphs `✿ ❀ ★`~~ | `EditorEmptyState.kt:246-248` | **WITHDRAWN — I misread an a11y remedy as a design confession.** `clearAndSetSemantics {}` is on the **Row**, not the cards, and `:231-236` gives the reason: *"every `Text` contributes a `TextView` to the **platform** tree … `clearAndSetSemantics {}` on the [Row] silences the cluster whole."* That is the standard decorative-image treatment. **And [ZINE-WORLD §H](ZINE-WORLD.md) calls these the best empty state in the app — that reading stands. Keep them.** |
| `TrayCueGlyph "⌄"` | `EditorEmptyState.kt:324` | Points at a tray that is already visible and already named in words at `:154` |
| `MarkText "!"` in a 60dp rotated pill | `ZineShelfFail.kt:296-302` | Alarm mark above a headline that already says "Your shelf didn't open." |
| ~~Dashed head divider~~ | `ZineActionSheet.kt:404-414` | **WITHDRAWN — conflicts with [ZINE-WORLD §H](ZINE-WORLD.md), which lists the dashed divider as *working* and as an instance of the line alphabet (§E.2).** The alphabet outranks the duplication objection. Keep. |

**Not decorative — keep:** `NudgeSpacer()` ×5 in `ReframeControls.kt:283-298` (structural D-pad grid).

## 1.5 The structural cuts, which matter more than the words

- **"Grab a photo or a few words from the supplies below."** (`Copy.kt:320`) — **there is nothing called supplies.** A "Supply tray" was specified in [`SCREEN-INVENTORY.md:112`](SCREEN-INVENTORY.md) and never built. Either build the word or change the sentence; today the first line a new user reads points at a screen that does not exist.
- **Four rows of chrome below the page on the Bench** (context pill · page strip · action row · nudge row) on a screen whose own principle is *"the page is the hero; the tool is a guest"*.
- **`BenchPageGrid` draws no page content** — eight blank cream cards with numbers, one inch above a strip that shows real thumbnails.

## 1.6 Two defects found while measuring (device, 2026-08-15, SM-A176B)

- **`Bring forward` is 10px wide and reports `enabled=true, clickable=false`.** TalkBack announces an active button that cannot be activated. Disabled would be honest; this is not. **This is the one control at fault — the other eight in that row announce correctly, and OD-11 makes the row a WCAG 2.5.7 conformance path that must not be removed.**
- **The five ink pots present 38×48dp targets.** Measured pitch 114px ÷ 3.0 = **38.0dp**, below the 48dp floor [`DESIGN-RULES.md:30`](DESIGN-RULES.md) R7 declares non-negotiable. ⚠️ **Reconciliation owed:** `TypeBar.kt:806-825` already documents this defect and records the measurement as **40×48**, notes it clears WCAG 2.5.8 AA (24×24) while missing Material's 48dp guideline, and says `TypeBarTest` asserts it. My measurement (device, density 3.0, 2026-08-15) disagrees by 2dp. **Two candidate causes: the KDoc's figure predates the pot-size change from 32→30dp in the re-skin, or one of us measured pitch vs. reported bounds.** Either way the conclusion is unchanged — it is under 48dp — but the recorded number needs correcting to whichever survives a re-measure.

---

# PART TWO — ADD

## 2.1 The primitive nobody listed

The brief proposes TEXT · IMAGE · GRAPHIC · PAGE · RELATIONSHIP · STYLE · MOTION. **Two independent research lines, which never spoke to each other, arrived at the same missing object:**

- *Paste-up practice:* cut → arrange loose → shuffle without committing → tack down → copy. **"Arrangement is reversible until the copy is made."** Scraps sit loose on the desk. Digital canvases usually force everything to land somewhere.
- *Android:* the material comes **to** you via the share sheet, from any app, at the moment you find it. But **clipping and composing happen at different times** — you clip on the bus and compose on the sofa.

Both describe the same thing:

> ### MATERIAL — stuff you have, that is not on a page yet.
> **The Clippings Tray.** This is Zinely's missing primitive, and it is the one that makes the phone advantage real rather than rhetorical.

Without it, "share a photo into Zinely" must ask *"which zine? which page? where on the page?"* at the exact moment the user has none of those answers. With it, the answer is "into the tray" and the decision is deferred — which is what paste-up actually is.

**The revised primitive set:**

| Primitive | State | Note |
|---|---|---|
| **TEXT** | ships | |
| **IMAGE** | ships | |
| **MATERIAL** | **missing — P0** | Unplaced stuff. The tray. Makes the share sheet usable |
| **GRAPHIC** | **missing — P1** | Shapes, marks, tape, stamps |
| **PAGE** | ships | |
| **STYLE** | partial | Ink, type, size |
| **RELATIONSHIP** | **missing — P2** | Objects across pages (spreads) |

## 2.2 GRAPHIC — the strategy is right, the stated reason is wrong

**The owner's hypothesis is already ratified and already proven in this repo.** [`V2-CONSTITUTION.md:294`](V2-CONSTITUTION.md) — *"express variety through **grammar**, not new one-off UI."* And [`v2-living-audit.html`](mockups/v2-living-audit.html) ships the cover grammar `{title × ink × mark × paper × motif × layout}` = **96,000 distinct configurations from a vocabulary of eight**. This is not a decision to make; it is a grammar to extend.

*(Note on that figure: the `{title × ink × mark × paper × motif × layout}` pill is verbatim at `v2-living-audit.html:237`;
the number **96,000 is computed at runtime at `:444`**, not printed in the file. It is a derived count, not a quotation.)*

**But the stated justification is a trap.** "Tens of thousands of combinations" is not the success metric — every failure had huge combinatorics too. Open Peeps advertises **584,688** combinations and still produced [Corporate Memphis](https://en.wikipedia.org/wiki/Corporate_Memphis), a style so uniform that "consumers find it challenging to distinguish between brands." Canva has 141M assets and produced the same homogenisation. **Combinatorial count is orthogonal to authorship.** Nobody counts the space; they recognise the vocabulary.

**What separated the winners: the human selects, the machine proposes.**
Truchet published *a table*, not a generator ([1704](https://en.wikipedia.org/wiki/Truchet_tiles); Douat's 1722 treatise is 189 pages of *selected* patterns). Molnár chose from plotter output — *"I would make choices from the results."* LeWitt trained executors. [Recursive](https://www.recursive.design/) has continuous infinite axes and ships **64 named stops**. In every success the space is a reservoir a human draws from; in every failure the system did the selecting.

**Two bright lines, both implementable as tests:**
1. **One user action places exactly one object.** One tap = one sticker = authored. One tap = twelve scattered stars = generated.
2. **The variation axis must not be visible in the output.** Nees's *Schotter* reads generated precisely *because* its disorder gradient is legible. If the user can see the rule, they see the machine.

And the constitutional line that resolves the rest — [`zinely-constitution.md:124,128`](../zinely-constitution.md): tools *"may not author"*; *"Materials and scaffolds pass; ghostwriters do not."* **A graphic placed by the user is a material. A composition generated for the user is a ghostwriter.**

### Transformations — safe and dangerous

| Safe | Why |
|---|---|
| Free rotation, user-aimed | A hand-placed sticker is never axis-aligned |
| Uniform scale, 4 named sizes | Unclamped scale turns a sticker into a background |
| Mirror, **as a per-primitive capability flag** | Meaningful on an arrow, a no-op on a star, wrong on a letterform |
| Ink tint, restricted to the named inks | Already law: *"tintable coverage not colour (1 asset × N inks)"* (`V2-CONSTITUTION.md:299`) |
| Layering, and bleeding off the trim edge | Literally what paste-up does |

| Dangerous | Why |
|---|---|
| **Automatic pattern fill / tiling** | Truchet tilings read as ornament because they are edge-matched and grid-locked — a *printed* surface. A repeated sticker on a grid names no physical cause a person could produce. Fails the texture law; `V2-CONSTITUTION.md:264` bans decorative textures outright |
| Randomised scatter of N copies | Crosses bright line #1 |
| Non-uniform stretch | No physical sticker stretches; distorts stroke weight |
| Procedural generation of new primitives; auto-composition | The machine authors — Article 7 violation |
| Opacity as a user slider | Translucency is a *material property* of tape, not a control. Bake it in |

### Randomness — the principle survives, amended

[ZINE-WORLD §A](ZINE-WORLD.md) established *"responses are aimed, not random."* The evidence refines it rather than overturning it. Excalidraw was **forced** to persist a per-element `seed` because unseeded randomness meant a reloaded drawing looked different — the artifact lost its identity ([issue #70](https://github.com/excalidraw/excalidraw/issues/70)). Procreate and Photoshop expose randomness only as named magnitudes bound to the stroke you are making.

> **Randomness may set an initial value. It may never be a running behaviour.**

On placement a graphic gets a small tilt derived deterministically from its element id — stable forever, survives reload, re-renders byte-identically (which `preview == export` and the Roborazzi goldens require). **No shuffle, no re-roll, no random position.** Position is always where the finger went. This is exactly the precedent ruling **D-017** already set for covers.

### ⚠️ The existing mockup contains the violation, in code

[`sticker-picker.html:215`](mockups/sticker-picker.html) applies a fresh `Math.random()` rotation **and** random left/top on *every placement*. That is non-reproducible documents and a direct contradiction of the aimed-response rule. **It must not survive into Compose.** Its twelve stickers must not either — they are emoji/text glyphs (`data-sticker="✿"`), which render from the platform font, so the same zine looks different on different phones (see §2.3) and each one is the most literal possible case of *"someone else's drawing."*

**Survives from the mockups:** the bottom-sheet tray, its own rationale (*"a small curated set first so it never feels overwhelming"*), the **Tape** and **Stamps** chips (they name physical causes), *"all free · nothing to buy · works fully offline"*, and `template-picker.html`'s **"Blank is a peer"** principle. **Dies:** the stickers, the randomness, the "Cute" chip (names a mood, teaches nothing), and "Letters" (that is a typography feature — route it to TypeBar). Status: marked *"V1 · designed"*, never built. **Nothing is sunk.**

### The recommended first pack — 12 primitives, 3 material families

Twelve, argued from shipped systems: this repo's own grammar works at **8**; Truchet at **1**; Recursive curates to **64**; Telegram caps packs at **120**; [NN/g](https://www.nngroup.com/articles/simplicity-vs-choice/) shows a 100-item picker inflates time-on-task **over 500%**. Twelve fits one sheet without scrolling and keeps the family *names* — which are what teach the physical cause — visible.

**Tape & fixings** *(how it got stuck to the page)* — torn tape strip · photo corner · staple · paper clip
**Stamps & marks** *(how ink got onto the page)* — star/asterisk · arrow · halftone dot cluster · registration cross
**Cut paper** *(how the shape was made)* — torn strip · cut-out window frame · cut label/speech tag · marker underline

Every one survives *"what physical object made this mark?"* None is a mood. Nine of twelve are asymmetric, so mirror is genuinely useful. All twelve are single-ink coverage, so the named inks multiply cleanly. The marker underline is the one place to spend `perfect-freehand`/Rough.js geometry, which ZINE-WORLD already sanctions as *"one controlled use buys the whole art direction."*

**12 × 10 inks × 4 sizes × 2 mirror = 960 placeable objects.** Publish that number nowhere in the UI — it is a proof of sufficiency, exactly as Gerstner and Truchet used theirs.

**If 12 is too many, cut the fixings.** A staple's real physical cause is the binding, which the Proof room already renders.

## 2.3 Assets and emoji — the hard licensing rulings

### Ruling: no ShareAlike, and no attribution that must reach the printed artifact

CC BY-SA's trigger is *Adapted Material*, not use ([§1(a)](https://creativecommons.org/licenses/by-sa/4.0/legalcode.en)). Zinely **recolours to its ink palette** — a modification the licence expressly asks you to indicate — and composes it with the user's photos and text. Whether that clears the creativity bar is genuinely uncertain (🟨 no case law), but two things make it unacceptable regardless:

1. **The obligation lands on the user, not on us.** The person *sharing* the zine is the user. Zinely would be silently attaching a copyleft obligation to a stranger's creative work.
2. **The PDF leaves the app.** Attribution must travel with the material when shared. An in-app credits screen does not accompany a printed page. Complying means stamping credits into the user's zine.

**Rank: CC0/PD > MIT/ISC/Apache/OFL (attribution satisfied in-app) > CC BY (follows the artifact) > CC BY-SA (excluded).**

| Verdict | Sources |
|---|---|
| ✅ **Use** | Material Symbols (Apache-2.0) · Feather (MIT) · Lucide (ISC) · Heroicons · Tabler · Phosphor · Bootstrap Icons (MIT) · **Openclipart (CC0 — ideal for the zine aesthetic)** · Open Doodles · Humaaans (CC0) · Met CC0 subset · Google Fonts / Velvetyne (OFL) |
| ⚠️ **Only if a credit may appear in the exported PDF** | Twemoji (CC BY 4.0) · Font Awesome Free (CC BY 4.0) |
| ❌ **Reject** | **OpenMoji (CC BY-SA)** · Noun Project (no redistribution right; scraping prohibited) · Flaticon (no redistribution) · **unDraw** (*"no right to compile assets… or distribute the assets in packs"* — aimed precisely at apps like ours) · **Blush** (*"printing an Illustration straight onto a T-shirt is not allowed"* — hostile to a printing app) · Internet Archive (no archive-wide licence) |
| ⏸ **Unverified — do not use yet** | Rijksmuseum · Smithsonian · Wikimedia (per-file triage) · Open Font Library (dormant) |

### Emoji: bundle vectors, never text runs

Skia's PDF backend keeps a font embedded only if each glyph *"is empty or has an unmodified path"*; a colour glyph fails and falls to a **Type3 font** — CBDT emoji become **rasterised images at a fixed 64px/em strike**. The PDF Association confirms no colour-font format is supported in PDF, **including PDF 2.0** ([paper](https://pdfa.org/wp-content/uploads/2021/06/OpenType-Color-Fonts-in-PDF.pdf)). And Android 13+ ships COLRv1 while earlier ships CBDT, Samsung ships its own designs, and `androidx.emoji2` defaults to **downloadable fonts over the network** — which this product cannot take.

> **The same zine exported on two phones would contain different artwork.** For a product whose promise is a reproducible printed object, that is a correctness defect, not a cosmetic one.

**Recommendation:** bundled vector emoji placed as ordinary objects, never in text runs. **Unicode geometric shapes, arrows and dingbats are meaningfully safer** — monochrome outlines keep the TrueType path and embed as a proper subset — on two conditions: **ship the font**, and append **VS15 `U+FE0E`** to codepoints with `Emoji_Presentation=Yes` or they re-enter the emoji font. Monochrome ink is already the aesthetic.

### Ruling: bundled library only. No network. Ever.

⚠️ **Corrected after review — I cited the weaker of two adjacent lines and omitted the binding one.** There is no
"§5.1" clause. [`PRD.md:239`](../PRD.md) is **NFR-2**: *"Offline: all core features work in airplane mode"* — the
line I read as leaving a door open for optional downloads. But the line directly above it, `PRD.md:238`, is
**NFR-1: "Privacy: no network calls; no photo leaves the device; no analytics in MVP."** **NFR-1 closes the door
already.** The conclusion below stands; it did not need the argument I built for it.

The claim Zinely makes is not "core features work offline" — it is that the app ships **zero networking
libraries**, verifiable by inspecting the manifest and dependency graph. (`AndroidManifest.xml:7-9` confirms the
only permission is `WRITE_EXTERNAL_STORAGE maxSdkVersion=28` — no `INTERNET`.) **A falsifiable claim is a
different product from a promise.** Adding `INTERNET` converts the second into the first, permanently, and
removing it later does not restore it.

Optional packs buy only APK bytes and cost hosting, integrity, versioning, storage management, offline degradation, and a permanently weaker baseline that every future PR argues against. The bytes are cheap. The invariant is not.

**Build:** a checked-in provenance manifest — asset id, source, SPDX licence, URL, retrieval date — feeding both an in-app credits screen and a CI check that fails the build on an unlicensed asset. Cheaper now than reconstructed later.

## 2.4 RELATIONSHIP — spreads, and one carve-out worth taking

**The imposition engine already permits it.** Verified from `SingleSheet8Imposer.kt:77-86` and the convention table, computed through the real `contentToSheet` transform rather than from cell position:

```
row 0 (rotated 180°):   5 | 4 | 3 | 2
row 1 (upright):        6 | 7 | 8 | 1
```

| Spread | Boundary | Fold or cut? |
|---|---|---|
| **2 \| 3** | `V-quarter-3`, row 0 | **FOLD — image can cross** |
| **4 \| 5** | `V-quarter-1`, row 0 | **FOLD** |
| **6 \| 7** | `V-quarter-1`, row 1 | **FOLD** |
| **8 \| 1** (wrap cover) | `V-quarter-3`, row 1 | **FOLD** |

**All four reading spreads meet at a fold.** The single cut (`CutLine`, middle two columns only) separates `4|7` and `3|8` — pairs never seen side by side. Corroborated independently by the leaf structure: the uncut non-spread boundaries are exactly the back-to-back pairs, giving four leaves in a closed cycle joined at precisely the four spread boundaries. **That closed loop of folds is a mini-zine.**

**Model: an image property realised as a one-shot action, not a persisted relationship.** "Fill both pages" writes two ordinary `ImageElement`s — same `assetId`, complementary normalised crops, `Fit.FILL`. Zero imposition change, zero schema change, existing crop/fit/render path.

**Reject a `Spread(pageA, pageB)` record.** The only page identity in the schema is `Page.index`, and `renumber()` (`Elements.kt:33`) rewrites it on **every** add/delete. A spread record would silently re-point at unrelated pages after one deletion. Fixing that means stable page IDs — a schema migration to support a feature nobody asked for.

**Delete one half → the survivor is an ordinary cropped photo.** Not a dangling reference, not a repair dialog. It degrades into something that still makes sense and still prints.

**Required, not optional:** suppress the keep-clear cue and `Copy.A11y.OUTSIDE_PRINT_REACH` on the **inner** edge only — those boundaries are interior to the sheet, so the printer's margin does not apply. Get it wrong and you either paint a **12mm white stripe** through the middle of a "continuous" image, or fire a false *"may be cut off"* alarm on every spread.

**Copy, per BP-4 (*the maker never learns the word "bleed"*):** the button says **"Fill both pages."** The one warning, in the consequence-first form the photo-book industry converged on: **"The middle of this photo lands on the fold — keep faces and words away from it."**

## 2.5 The Android advantage — and a correction to the thesis

> **The drawer is not the gallery. It is the share sheet.**

A laptop has a gallery too (via Dropbox). The share sheet is the only mechanism where material comes **to** you, from any app, at the moment you find it. That asymmetry is the actual phone advantage.

| Mechanism | Permission | Note |
|---|---|---|
| `ACTION_SEND` / `SEND_MULTIPLE` receive | **none** | One manifest block. Makes every app on the phone an input device |
| Photo Picker (`PickVisualMedia`) | **none** | No storage permission at all. Feature-detect via `isPhotoPickerAvailable` |
| Screenshots album highlight | **none** | `EXTRA_PICK_IMAGES_HIGHLIGHT_ALBUM` opens the picker *already on Screenshots* |
| Direct Share targets (per-zine) | **none** | Sharing into "Zine: Ghost Bus #3" becomes two taps |
| Clipboard | **none** | Focus-gated since Android 10 |
| Haptics | `VIBRATE` (normal, no prompt) | Design for `TICK` — `THUD`/`SPIN` often absent on cheap LRAs |
| CameraX | `CAMERA` (runtime) | Heaviest cost here. Ask in-context only; never at launch |
| `PrintManager` | **none** | **Additive only** — reaches only printers with an installed print service; as a replacement its failure mode is a dead end rather than a file |

**Correction worth recording:** Android 14's screenshot-detection API fires **only for screenshots of your own visible activity** and **does not hand you the image**. It cannot notice screenshots taken elsewhere. Useless here.

**The "oh shit" moves that survive the constraints:**
- **Every app has an "Add to zine" button.** The premise is understood without a word of onboarding.
- **Two-finger place.** Pinch-rotate-drag simultaneously. **A mouse physically cannot rotate and scale at once** — desktop collage tools make you grab a corner handle, then a rotate handle, then reposition. This is the one operation where a phone is *strictly better*, and collage consists of it.
- **The photocopier.** 1-bit dither / halftone applied on-device. Zine authenticity *is* the photocopier look, so the cheap implementation is the correct one — Floyd–Steinberg over a downscaled bitmap, pure Kotlin, fits `core:render`, no dependency, no network.
- **Actual size.** Render at true millimetres via `DisplayMetrics.xdpi` — "hold this against a sheet of paper." A folded A7 8-pager is 74×105mm; a phone is about that. You cannot hold a laptop against paper.
- **Hand it over.** A chrome-free Read mode that survives being physically passed across a table. A zine's whole social form is handing it to someone.

**Dies on the invariants:** on-device speech-to-text (the default recogniser may use network) · ML Kit segmentation in unbundled form (downloads models) · anything resembling a feed.

## 2.6 The hostile case, which should shape the product more than the features

> **A 6-inch screen cannot show a whole-page composition at a judgeable size.**

Collage is a whole-surface judgement — balance, weight, negative space. You get the whole page too small to evaluate, or legible detail with no compositional context. Pinch-zoom does not fix this; it is a perception problem, not a navigation one.

**This is not fatal. It is the product's identity, stated as a constraint:**

> ### Zinely is a collage tool with few, large, coarse elements — not a layout tool.
> A phone zine with 40 fiddly elements is a bad desktop document. A phone zine with 5 bold ones is a good zine.

Every roadmap decision should be tested against that sentence. It also explains why "Canva but DIY" is not merely off-brand but *technically wrong*: the thing Canva is good at is the thing a phone is worst at.

Also true, and worth writing down: text setting is genuinely worse on a phone and always will be · your finger occludes the object you are judging · a phone has no flatbed scanner · **memory is the real engineering cost** (24 pages × full-resolution bitmaps will OOM a mid-range device if handled naively) · the phone is the most-interrupted device there is, so autosave is a precondition, not a feature.

---

# PART THREE — THE RANKED QUEUE

### P0 — essential to the product identity

| # | Capability | User problem | Why Zinely owns it | Complexity | Depends on |
|---|---|---|---|---|---|
| 1 | **Share-sheet receive** | "Getting my stuff in is the whole friction" | It *is* the material-drawer thesis, executable | Low | Manifest + URI→app-storage copy |
| 2 | **Clippings Tray (MATERIAL primitive)** | Clipping and composing happen at different times | Nothing else makes accumulation a practice; it is paste-up's core act | Medium | #1, document model, **ADR** |
| 3 | **Photo Picker + zero-permission stance** | "Why does a zine app want my whole gallery?" | The verifiable-privacy claim is the product | Low | `isPhotoPickerAvailable` + fallback |
| 4 | **Two-finger place** | Fiddly placement kills flow | The one operation phones beat desktops at | Medium | Editor MVI, transform model |
| 5 | **The subtraction pass** (§1.3–1.6) | The app explains itself instead of getting out of the way | Confidence is a feature | Low | Golden re-records. **Note: ~15 removals move Roborazzi goldens and several have asserted-absence tests** (e.g. `the_saved_chip_no_longer_paints_the_flower_or_the_qualifier`) |

### P1 — high-value creative capabilities

| # | Capability | Note |
|---|---|---|
| 6 | **GRAPHIC primitive + the 12-piece first pack** | ADR-103. Sealed-`Element` blast radius is real: 8 exhaustive `when` sites break at compile time, but **six `as?` sites in `EditorScreen.kt` degrade silently to no-ops**, and `EditorA11y.label()` has no `else` — a third type ships with no TalkBack label unless explicitly added. Schema bump v1→v2 + migrator. **Good news: PDF export is already vector-capable** — a `DrawPath` command vectorises through `CanvasReplayer` for free |
| 7 | **The photocopier filter** | Aesthetic credibility, offline, zero dependencies |
| 8 | **Wraparound cover (`8\|1` spread only)** | A tenth of the spread work for the quarter that changes how the object feels in the hand |
| 9 | **Direct Share targets** | Five taps → two |
| 10 | **Clipboard paste** | Completes the drawer |
| 11 | **Actual-size preview** | Only possible holding the device against paper |
| 12 | **Haptic seating** | Placing paper should feel like placing paper. `PRIMITIVE_TICK` |
| 13 | **Screenshots album highlight** | Free, feature-detected, and reads as the app knowing what your material is |
| 14 | **Direct print (`PrintManager`)** | Additive only — **must not replace Save-PDF** |
| 15 | **CameraX quick capture** | Gated behind an in-context permission ask |

### P2 — differentiators / delight

| # | Capability | Note |
|---|---|---|
| 16 | **Hand-it-over Read mode** | The zine's social form is being handed to someone |
| 17 | **General spreads (`2\|3`, `4\|5`, `6\|7`)** | Verified possible, cheap; not yet earning its process cost against V2.1 |
| 18 | **Camera texture scanner** | Justifies the camera permission the way object capture doesn't |
| 19 | **Colophon on export** | Provenance was the most-recurring principle across all 35 references, and Zinely has none. Optional, off by default |
| 20 | **App shortcut → capture-to-zine** | Cheaper than a QS tile, wider reach |

### P3 — experimental / someday

QS tile · stylus pressure & tilt · perspective-correct paper scan · themed packs beyond the first · the opt-in keyword-only asset library (`V2-CONSTITUTION.md:302` is the only door the constitution leaves open).

### Rejected outright, with reasons

| Idea | Why not |
|---|---|
| **Seasonal packs** | A permanent content treadmill that dates the app against the Ten-Year Test |
| **Community packs / marketplace** | [`zinely-constitution.md:140`](../zinely-constitution.md): *"Not an asset marketplace."* Also needs a server, moderation, and licence provenance |
| **Live remote asset search** | Kills the verifiable-privacy claim; queries describe what the user is making |
| **Auto-compose / "surprise me" layout** | Article 7: the machine authors. Ghostwriter, not material |
| **Pattern-fill / auto-tiling** | Names no physical cause; `V2-CONSTITUTION.md:264` bans decorative textures |
| **Opacity slider** | Translucency is a material property, not a control |

---

## The one-line filter for everything above

> **Does it shorten or strengthen the path from *what is already on your phone* to *a folded object in your hand*?**

P0 items 1–4 are all that path. GRAPHIC and the photocopier make what travels it look like a zine. Everything in P3 is optional forever.

---

---

## Review record

A Review Agent was dispatched to falsify both this document and [ZINE-WORLD.md](ZINE-WORLD.md) against actual
repository state. **Verdict: NO-GO on the rulings as first written.** It confirmed roughly 40 citations verbatim —
including the ADR-090/OD-12 foundation and the entire imposition adjacency table, re-derived independently
including the 180° row-0 trap — and falsified the following, each of which I verified myself before accepting:

| Finding | Outcome |
|---|---|
| `v21-typebar.html` already specifies the **bench**, not the island (`:392` sibling of `.page`; `:698-700` in prose) | **ACCEPTED.** Ruling C reversed from "do not freeze" to **"freeze both"**; Ruling A reframed as a parity defect |
| `v21-reframe.html` contains **no** rule-of-thirds (zero matches, 362 lines) | **ACCEPTED.** Ruling B scoped to Compose only |
| The island membership rule, its test, and the opt-out mechanism **all already exist** | **ACCEPTED.** J6 cut entirely |
| `ZineActionSheet` Delete is **already `jamText`** | **ACCEPTED.** Finding withdrawn |
| Uppercase tracked labels are a **counted `core:ui` style** (`sectionLabel`, six uses), not an invention | **ACCEPTED.** Rewritten; real finding is seven divergent tracking values |
| The nudge row has labels, shared grammar, and is a **WCAG 2.5.7 conformance path** under OD-11 | **ACCEPTED.** "Or gets deleted" withdrawn; scoped to the one clipped control |
| `BenchSnack` is also inside the island, missed by my table | **ACCEPTED.** Added |
| `BenchPageGrid`'s leaf is **transcribed from frozen CSS**, not accidental; fix breaks 2 tests | **ACCEPTED.** Reclassified and re-costed |
| NFR-1, not "§5.1", is the binding privacy line | **ACCEPTED.** Argument rebuilt |
| Three contradictions between the two documents | **ACCEPTED.** All three resolved above |
| `ERROR_BODY` is live, not dead | **ACCEPTED.** Range narrowed |
| `clearAndSetSemantics` misread as a design confession | **ACCEPTED.** Withdrawn |

**Six claims it could not verify statically** and which still need a device or a build: all §H device observations ·
the `Bring forward` platform attributes · the 38-vs-40dp pot measurement · the `inkFaint`-on-`bench` re-measure
(J1, explicitly not yet done) · whether the golden tests currently pass · the 35 external references.

---

## What I am least sure about

1. **The Clippings Tray is a document-model change proposed on research, not on a user complaint.** It is the highest-confidence idea here *and* the least validated. It deserves an HTML prototype and a device pass before any Kotlin.
2. **"12 primitives" is argued from shipped systems, not from a study.** No published threshold for asset-picker browsability exists. The number could be 8 or 16.
3. **The recolouring-as-adaptation licensing analysis has no case law behind it.** It is a risk assessment. If it is wrong, CC BY-SA sources reopen — but being wrong in the other direction attaches copyleft to a stranger's zine, so the asymmetry justifies the conservative call.
4. **Removing "works offline · stays on your phone" is the one cut that could be a mistake.** It is the product's differentiator and I am proposing to say it once instead of four times. If first-run testing shows users don't absorb it, the answer is to make the *one* statement better, not to restore the other three.
