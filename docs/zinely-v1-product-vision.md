# Zinely v1 Product Vision

**Role:** Lead Product Designer / UX Researcher / Creative Director
**Date:** 2026-07-18
**Status:** Proposal — nothing here is frozen. Every UX change below must pass the HTML-first pipeline (prototype → critique → freeze) before any Compose work.
**Ground rules honored:** the fold is sacred; privacy/offline invariants intact (one carefully-scoped amendment proposed in §11); ADR constraints listed in Appendix A were treated as immovable unless explicitly challenged.

**Priority legend:** 🔴 MUST HAVE · 🟠 SHOULD HAVE · 🟢 NICE TO HAVE · 💜 GAME CHANGER

---

## 1. Executive critique

Zinely today is a **technically honest print pipeline wearing a product's clothes**. The engineering is genuinely excellent — atomic saves, parity rendering, pure-Kotlin imposition, real TalkBack passes — and the design *documents* (VOICE.md, DESIGN-RULES.md, EXPERIENCE-MAP.md) describe a warm, handmade, emotionally intelligent product. But the shipped experience betrays both, in five structural ways:

**1. The product teaches its central idea last.** The fold — the single concept that makes Zinely exist — is explained in Proof Act 3, *after* the user has designed eight panels and exported a PDF. A first-time user edits a "scrambled on purpose" sheet with no mental model of why it's scrambled. This is like a chess app that explains checkmate after your first game ends. Blank-start research is unambiguous: unstructured, unexplained starting points are the top driver of first-session abandonment. The fold must move from epilogue to prologue.

**2. The editor never shows the user the object they're making.** The Bench correctly edits in reading order (cover → pages → back), but the rail is a flat list of panels: nothing inside the editor conveys which pages *face each other* when folded, and ADR-051 (Decision B) retired the reading-order booklet preview entirely. The user designs eight isolated rectangles and only discovers how they behave as a *book* after export. The fold — the product's central idea — is invisible in the room where all the work happens, and is formally taught only in Proof Act 3, after the fact.

**3. The riskiest moment is outsourced at its peak.** ADR-052 removed the in-app Print button, so the entire promise ("printed at home") depends on a beginner correctly setting four fields (100% scale, landscape, single-sided, paper size) inside an OEM print dialog we don't control — the documented rage-point of every printer app store listing. We can't take the dialog back, but we currently do almost nothing to de-risk it: no test-fold sheet, no calibration ruler (deferred despite ADR-012 promising it), no post-print recovery guidance. Save-to-phone (ADR-054, shipped in v0.8.0) fixed the *export destination* honestly; the *print settings gauntlet* remains fully de-risked by copy alone.

**4. The craft-table metaphor is a promise the supplies can't keep.** The design language says workbench, supplies, tape, stamps, handmade. The actual supply tray contains **two items**: text and photo. There are no stickers, no tape, no textures, no fonts to choose, no templates, no prompts. Meanwhile the shipped visual identity is flatter than the frozen prototypes (hatch fills and cover art ponytail-flattened to solid swatches and page numbers). A user who arrives expecting a zine kit finds a form with two field types. The emotional gap between what the brand promises and what the tray delivers is the product's biggest strategic hole — and its biggest opportunity, because the fold + print pipeline (the hard part) is already built.

**5. The shelf is a file manager, not a bookshelf.** Covers render as generated stand-ins, not the user's own page-1 artwork — even though the real thumbnail pipeline (ADR-045) is built and deliberately left unwired. There is no "continue where you left off," no prompt to start from, no record that a zine was ever *printed*. The screen that should say "look at these little books you made" currently says "here are your documents."

None of this is fatal. The foundation under it — parity render, imposition, durability, a11y — is exactly what lets us now spend a year on love instead of correctness.

---

## 2. Current UX scorecard

| Area | Grade | One-line verdict |
|---|---|---|
| Engineering honesty (saves, parity, a11y) | **A** | Genuinely best-in-class for an indie app. |
| Voice & copywriting | **A−** | VOICE.md is a real asset; copy warmth survives into shipped strings. |
| Print pipeline correctness | **B+** | Imposition + 300 DPI + safe areas solid; the *handoff* is the gap. |
| First-run / onboarding | **D** | None shipped. Empty shelf + a button. Core concept untaught. |
| Editor mental model | **C** | Reading-order editing is right; but no fold/facing-pair awareness in Bench (booklet preview retired by ADR-051). |
| Editor ergonomics | **C** | Steppers-as-primary for photo framing; two-item tray; no snapping. |
| Text & typography | **C−** | One font, block-level style only, silent non-Latin truncation (data loss). |
| Media handling | **B−** | Photo Picker flow + Reframe model is right; the *controls* are tedious. |
| Creative supplies (stickers, textures, templates) | **F** | Do not exist. The metaphor is unbacked. |
| Home / Shelf | **C** | Correct skeleton, zero emotion: schematic covers, no continue, no prompts. |
| Print payoff moment | **B−** | Act 3 fold guide + haptic is lovely; arrives cold, no test sheet, no ritual. |
| Error experience | **D+** | Warm dead-ends: every failure is "Try again" forever. |
| Brand-to-build fidelity | **C−** | Frozen prototypes are noticeably richer than the shipped app. |

---

## 3. Biggest pain points (ranked)

1. 🔴 **Silent non-Latin text loss** — non-Latin/emoji text renders *blank* in preview and export (ADR-028 Latin-first cmap guard), with no warning at input time. Already reported by a real tester (alpha triage 2026-07-04, findings B3/C1) and carried as a known limitation. For the poet persona this is data loss.
2. 🔴 **Fold taught last** — the core mental model arrives after the work is done; nothing in first-run or the Bench conveys it.
3. 🔴 **The user's work is shown as placeholders** — the Shelf renders a generated stand-in cover, not the zine's real page 1 (the ADR-045 thumbnail pipeline exists but is deliberately unwired — HomeScreen.kt documents it as an open M6 owner decision); Proof renders schematic page numbers.
4. 🟠 **Print handoff de-risking absent** — no test-fold sheet, no calibration ruler (ADR-012 promised it; deferred by ADR-047), no "it printed wrong, now what?" path.
5. 🟠 **Two-item supply tray** — the creative ceiling is text + photo.
6. 🟠 **Gesture ergonomics thin at the emotional peak** — ADR-053 correctly makes steppers the authoritative accessible path and frames gestures as enhancements; the enhancements (drag/pinch inside Reframe, snapping) are the part not yet shipped.
7. 🟠 **No fold awareness in the Bench** — facing pairs invisible; booklet preview retired (ADR-051 Decision B) with nothing replacing it.
8. 🟢 **Warm dead-end errors** — kindness without an exit; every failure resolves to "Try again" forever.
9. 🟢 **No settings surface** — theme, fold-help, licenses have no home; paper locked at creation (ADR-047).

---

## 4. Biggest opportunities

1. 💜 **Own the fold as theater.** No competitor (Canva, Express, none) has a moment where digital work becomes a physical object in your hands. The imposition reveal + fold ritual is Zinely's un-copyable peak — currently played as a settings page.
2. 💜 **The zine that teaches Zinely.** Onboarding as a printable tutorial zine — the medium teaching the medium. Nobody else can do this; it *is* the product demo, the tutorial, and the first print all at once.
3. 💜 **A real supplies ecosystem, offline.** Zinnia's top complaint is stickers-need-internet. An offline-first craft app with generous bundled packs + explicit-download extras turns the privacy stance into a *feature* users can feel.
4. 🟠 **Prompts kill the blank page.** Day One proved the mechanism; zine prompt packs (8 prompts = 8 pages) are a perfect-fit adaptation, fully local.
5. 🟠 **The shelf as a record of made objects.** Printed-zine "graduation" (worn edge, print date, "printed ×3") leverages the IKEA effect with zero cloud.
6. 🟢 **Share-as-image.** One rendered "look what I made" card to the OS share sheet: distribution with zero accounts or network.

---

## 5. Competitive research (principles, not clones)

Full sourced reports live with this document; distilled here. Labels: ✅ VERIFIED (sourced) · 🟨 ASSUMPTION.

**Editor (Canva mobile, Paper/FiftyThree, Procreate Pocket, Bazaart, M3, HIG, NN/g):**
- ✅ Canvas is the product; default chrome near zero (Paper added zoom with *no* new persistent UI).
- ✅ Nothing selected → insertion tools; object selected → that object's verbs only (Canva's contextual quick-actions).
- ✅ Floating bar for verbs; ≤50%-height bottom sheet for property sets, object stays visible and live-updates (M3 guidance).
- ✅ ~5 ranked actions visible, rest under More — ranking is where design opinion lives.
- ✅ Snapping + flash guides + haptic tick substitute for the precision fingers lack; objects snap on finger-lift.
- ✅ Keyboard and rich tools never fight: style strip above keyboard; opening a sheet dismisses the keyboard (Canva).
- ✅ Font names rendered in their own face, grouped by mood; curation (15–30 fonts) beats catalog.
- ✅ Style presets before raw controls: presets encode taste, sliders demand it.
- ✅ Double-tap = enter the frame (crop/reframe); Replace is a first-class verb preserving transform (matches ADR-053 — keep).
- ✅ Undo ever-present + gesture (Procreate: two-finger tap; toast names what was undone — teaches the system model).
- ✅ Figma mobile is the negative proof: desktop interaction models don't shrink — validates HTML-first-for-thumb.
- ✅ Gesture vocabulary must fit one line; every gesture needs a button equivalent (a11y).

**Assets (Canva elements, GoodNotes, Zinnia, WhatsApp/Signal packs, F-Droid, Obsidian, Google Fonts):**
- ✅ Packs are the unit of meaning (20–40 assets, named, covered, installable/deletable as one thing) — never a flat 5,000-item grid (choice-overload literature).
- ✅ Recents row beats any taxonomy; Favorites make it *my* library. Both computable locally, zero analytics.
- ✅ User-imported packs (transparent PNGs) are how GoodNotes/Zinnia ecosystems actually thrive — the privacy-safest growth path.
- ✅ Zinnia's cautionary tale: stickers gated on connectivity = top user complaint. Bundle generously; downloads are additive only.
- ✅ Catalog = routing info only (F-Droid/Obsidian model): tiny static JSON, per-pack hash, explicit user-initiated fetch, verify-then-install.
- ✅ Version packs with a monotonic int + `minAppVersion` compat gate (WhatsApp/Obsidian).
- ⚠️ **License landmine found: unDraw explicitly forbids redistribution "in packs" or "in an app."** The brief's assumption was wrong. Safe seeds: Google Fonts (OFL), Humaaans (CC0), MIT icon sets, Library of Congress free-to-use sets, museum open access (CC0), rawpixel CC0 subset. Most texture sites also forbid repackaging — commission or scan originals.

**Onboarding & print (Canva/Notion/Express onboarding, Electric Zine Maker, Day One, Procreate gallery, zine-culture guides, printer-app reviews):**
- ✅ Never open on a truly blank state; the empty state is the one screen every user sees — make it *do* something.
- ✅ Teach inside a real document, not tour overlays (Canva's "Learn the Basics" file; Express drops you into the tool).
- ✅ One question → personalized starting content is the strongest cheap activation lever (Canva/Notion), executable 100% locally.
- ✅ Electric Zine Maker proves the anti-polish position works *for zines specifically*: visually disarming lowers creative pressure; "ugly is a genre."
- 🟨 Scaffolds beat showpieces: a too-finished template inhibits like a blank page; prefer structural skeletons and rough starters.
- ✅ Prompts remove "what do I even make?" (Day One's stated rationale); themed packs of 8 map one-prompt-per-page.
- ✅ Imposition is the documented #1 zine-printing confusion; the classic fix is *physical* (fold a blank sheet and number it) — replicable as animation.
- ✅ Printer previews must earn trust: state assumptions explicitly; wasted paper is the anger driver in every printer-app review.
- ✅ Peak-end rule: Zinely's peak is physical — no cloud app can match it; spend the delight budget after "Print."
- ✅ IKEA effect + goal gradient: 8 pages is a perfect finite ladder; celebrate micro-completions; count "zines made," not day-streaks (streaks-as-guilt is off-brand).

---

## 6. Interaction improvements

| # | Change | Priority |
|---|---|---|
| I1 | **Direct manipulation first, steppers as parity path.** Drag to move photo in frame, pinch to zoom, double-tap to enter Reframe. ADR-053's steppers remain as the accessible equivalent — additive, not replaced. | 🔴 |
| I2 | **Contextual verb bar.** Nothing selected → tray shows Add verbs. Block selected → floating bar: (text) Edit · Style · Duplicate · Delete · More; (photo) Reframe · Replace · Duplicate · Delete · More. Z-order verbs under More. | 🔴 |
| I3 | **Snapping + guides.** Snap to panel center, margins, safe-area, sibling edges; flash guide + light haptic. Print punishes "almost straight" — snap rotation to 0/90/180/270 when rotation ships. | 🟠 |
| I4 | **Keyboard handoff.** Style chips strip above keyboard while typing; tapping Font/Colour dismisses keyboard, opens half-sheet, text stays visible and live-updates. | 🟠 |
| I5 | **Undo everywhere.** Keep top-bar undo/redo; add two-finger-tap undo; transient toast names the undone action ("Undid: moved photo"). | 🟢 |
| I6 | **Gesture contract (one line, frozen):** tap select · drag move · double-tap reframe/edit-text · pinch zoom canvas / scale block · long-press select-behind · two-finger-tap undo. Nothing else, ever; every gesture has a button twin. | 🟠 |
| I7 | **Errors get exits.** Every error surface offers: retry · an alternate path (e.g., export image instead of PDF) · "copy details" for bug reports. Warmth stays; dead-ends go. | 🟠 |

---

## 7. Visual improvements

| # | Change | Priority |
|---|---|---|
| V1 | **Close the brand-to-build gap.** Restore the frozen trilogy's hatch fills, vignettes, tape/paper textures in Compose. The prototypes are the spec; the flattening was expedient, not a decision. | 🟠 |
| V2 | **Real covers everywhere.** Shelf: wire the existing, deliberately-unwired ADR-045 page-1 thumbnail pipeline (cheap — an open owner decision, per HomeScreen.kt). Proof: thread the document through the Proof VM seam to replace schematic page numbers with real art. | 🔴 |
| V3 | **Ship the marker/display face.** The "friendly handmade face" has been "a follow-up" since the design language was written. Pick one OFL face, freeze it, ship it. Brand-in-type is not optional garnish. | 🟠 |
| V4 | **Printed-zine graduation.** Printed + folded zines get physical treatment on the shelf: worn edge, print-date stamp, "printed ×N". | 🟠 |
| V5 | **Dark theme gets a product home** (Settings) — it exists only in the prototype dock today. | 🟢 |

---

## 8. Information architecture

Answering the brief's direct questions:

- **Should the logo always be visible?** No. The wordmark belongs on first-run and About. The shelf's header should be *your* shelf ("Your zines"), not our brand. Brand recall comes from the paper/ink identity, not a persistent logotype.
- **Should "On this device" exist after onboarding?** Not as a persistent pill. Privacy reassurance is most valuable at *moments of doubt* (photo import, export, share) — exactly where the copy already does this well. On the shelf it becomes wallpaper that costs hierarchy. Move it: first-run + moments-of-doubt + About. (DESIGN-RULES R12 says privacy is reassurance, never a wall — a permanent badge is closer to a wall.)
- **Is the hierarchy correct?** No. Today: brand → privacy pill → search → grid → FAB. Proposed: **Continue card** (your in-flight zine, "page 5 of 8") → **Start something** (blank / prompt / starter) → **Your shelf** (finished + printed) → search/sort only at `many`.

Surface map stays three-plus-two:

```
Shelf (root)
 ├─ Continue card → Bench
 ├─ Start: blank · prompt-of-the-day · starter gallery
 ├─ Shelf grid → per-zine sheet (Open · Rename · Duplicate · Share · Delete)
 └─ Settings (NEW): theme · fold help · licenses · about
Bench (editor, reading-order) ⇄ Proof (3 acts, sheet-order shown here ONLY)
First-run (NEW, once): fold animation (4s) → one question → seeded shelf + tutorial zine
```

---

## 9. Editor redesign

**The structural foundation is already right — build the missing fold-awareness on top.** The Bench already edits in reading order with imposition confined to Proof (bench.html and the shipped EditorPageStrip both confirm it). What's missing is the *book*:

- 💜 **Facing-pairs strip:** evolve the rail from a flat list of eight cards into paired spreads showing which pages sit next to each other in the folded booklet — the fold made visible in the room where the work happens, and the foundation for panoramic spreads later (§14). This partially revisits ADR-051 Decision B (which retired the booklet preview); the lightweight strip is the honest middle ground between "full reader nobody used" and "no fold awareness at all." 🔴 as a concept to prototype.
- 🟠 **Cover status:** first and last rail cards badged "Cover" / "Back" with distinct framing (§14).
- Constraint check: imposition stays pure-Kotlin and untouched; this is presentation only. ADR-007/018 unaffected.

**Canvas & chrome:** page fills the width; bars condense on scroll/interaction; a "peek" long-press hides all chrome to admire the page. The brief's "unused horizontal space" complaint is solved by reading-order single-page focus + pinch to see the sheet neighborhood.

**Empty panel:** keep ADR-033's invitation-only rule but make the invitation *specific and rotating*: "This is page 3. A photo of something you love would look good here." The decorative arrow gains a semantic sibling for screen readers ("Supplies below: add text or a picture"). 🟠

**Text:** insert → type immediately in a sensible default; style later (never font-first). Presets before sliders: 6–10 zine-flavored text styles (ransom cut-out, typewriter, marker, neat caption). TypeBar's discrete a11y model stays; presets sit in front of it. 🟠

**Photos:** ≤3 taps to placed; lands pre-fit to tapped panel; Replace preserves frame/transform/z-order (already right per ADR-053 — extend to the verb bar). 🔴 (I1)

---

## 10. Home redesign

1. 🔴 **Continue card** at top: cover thumbnail + "Continue *Corner Store Poems* — page 5 of 8." One tap resumes. The 8-page format is a natural goal-gradient progress bar.
2. 🔴 **Real cover thumbnails** rendered as little physical booklets (fold shadow, paper edge) — the shelf shows *objects*, not files (V2).
3. 🟠 **Three on-ramps:** Blank zine (confident) · Today's prompt (unsure what) · Starters (unsure how). Never just a naked "+".
4. 🟠 **Prompt-of-the-day card:** one rotating local prompt; tap = new project pre-titled with it. No notifications, ever — the calm is the brand.
5. 🟠 **Printed graduation** (V4): the shelf becomes a record of things you *made*, not files you own.
6. 🟢 Search/sort stay gated behind `many` (current behavior is right).
7. Logo & privacy pill: per §8, demoted to first-run/About/moments-of-doubt.

---

## 11. Asset ecosystem — "Supplies"

**The honest constraint first:** the offline invariant is stronger than a principle — PRD **NFR-1** and §6 say no network calls outright, and PRD **§7's out-of-scope list explicitly excludes "networking of any kind" and a templates marketplace**. Downloadable packs therefore *reverse an explicit scope exclusion*, not merely amend a principle. That demands **a new ADR + PRD §5/§7 change**, scoped hard: *explicit, user-initiated HTTPS GETs of static, hash-verified asset catalogs and packs; never automatic, never carrying device data, no accounts, no analytics.* If the team declines, everything below still works with bundled + user-imported packs only — the catalog tier is strictly additive. 🔴 decision to make either way.

**Architecture (proven models: WhatsApp pack format, F-Droid signed index, Obsidian compat gating):**

- **Pack = the atomic unit.** Zip: `pack.json` (id, name, version int, minAppVersion, license + attribution, category, per-asset keywords), cover, thumbnails, assets (transparent PNG/vector; WebP textures). Same format for bundled, downloaded, and user-created packs — one registry, one UI.
- **Bundled core (ships in APK):** 6–8 sticker/graphic packs, 2–3 tape/texture packs, 10–16 OFL fonts, prompt packs. The app must feel complete in airplane mode forever (Zinnia's failure inverted into our feature). 🔴
- **Catalog tier (post-amendment):** one static `catalog.json` on dumb HTTPS hosting — id, description, size, version, minAppVersion, sha256, URL, `featured`/`season` flags. Fetched only when the user opens "Get more supplies" and taps refresh. Download → verify hash → unpack to app-private storage → register. Failed hash = delete + honest error. Incompatible packs shown greyed ("needs app update"), never hidden. 🟠
- **Self-contained projects invariant:** placing an asset **copies its bytes into the project document store** (extends ADR-022's content-addressed model). Deleting a pack never breaks a zine; `.zine` backups stay portable. 🔴 (non-negotiable if packs ship)
- **Storage UX:** MB shown pre-download; Manage Supplies screen with per-pack sizes + delete; soft advisory at ~200 MB; never auto-evict user choices.
- **Drawer UX:** opens on Recents → Favorites → installed packs by category (6–10 named categories, e.g. Cutouts · Tape & frames · Doodles · Paper bits). Search covers installed keywords only. Small opinionated packs, never an endless grid. 🟠
- 💜 **User packs:** "New pack from my photos" (transparent PNGs via Photo Picker). This is how GoodNotes' ecosystem actually grew, it's zero-infrastructure, and it's the most on-brand feature possible: *your* supplies, on *your* device.
- **Licensing seeds (vetted):** Google Fonts OFL · Humaaans CC0 · Bootstrap/Feather/Phosphor MIT (verify LICENSE at bundle time) · Library of Congress free-to-use sets · Smithsonian/Met/NYPL CC0 · rawpixel CC0 subset. **Not usable: unDraw (license forbids app/pack redistribution); most free-texture sites likewise.** Offline "Licenses" screen renders attribution from pack metadata. 🔴 (legal hygiene)

---

## 12. Typography system

1. 🔴 **Fix silent non-Latin blank rendering.** Non-Latin/emoji characters render blank in preview and export (ADR-028's Latin-first cmap guard; carried as a known limitation; tester-reported — alpha triage 2026-07-04, finding C1, whose own recommendation this matches). Minimum: detect uncovered characters at input time and say so honestly ("This font can't print 'ñ' yet"). Better: bundle a Noto fallback subset. Silent data loss for poets — a named persona — is the worst live bug in the product. (Latin-first can stand; *silence* cannot.)
2. 🟠 **Ship font choice as vibe groups** (already V1 roadmap): 8–12 groups ("Typewriter", "Riot grrrl", "Neat & tidy", "Loud"), 3–6 faces each, every name rendered in its own face, recents on top. Family names demoted to a detail view.
3. 🟠 **Pairings, not fonts:** named heading+body duos ("Zine Classic: Special Elite + Inter") as the primary choice; single-font selection is the advanced path.
4. 🟠 **Text style presets** (§9) sit in front of the TypeBar.
5. 🟢 **Font packs** ride the supplies pipeline (`type: "font"`, OFL text shipped inside the pack; self-hosted, never hotlinked — privacy + OFL compliance).
6. Constraint note: block-level styling (ADR-055) is *fine* for v1 — zine text is short. Per-span styling is V2 at the earliest; don't spend here yet.

---

## 13. Sticker system

- Stickers, tape, frames, doodles, paper scraps are all just **image blocks from packs** — they reuse the existing block model, Reframe, z-order, and render parity. No new element type needed for v1 of supplies. 🟠 (deliberate scope control)
- **Tape/washi as a placement behavior**, not a new engine: a tape asset dropped near a photo corner suggests a rotation snap across the corner. 🟢
- **Stamps:** one-tap placement at a sensible default size (no drag-drawing), matching the craft metaphor. 🟢
- **Drawing/handwriting layer stays V2** (already roadmapped). It's a different input pipeline (ink capture, smoothing, its own undo semantics) and shouldn't block the sticker win. Honest sequencing beats a half-baked brush.
- Every sticker respects print reality: raster assets sized so 300 DPI export never upscales past 1:1. 🔴 (quality gate)

---

## 14. Multi-page system

- **8 pages stays sacred for v1.** The single-sheet fold is the identity, the constraint that makes beginners finish, and the entire correctness story. 16-page saddle-stitch stays V2 (per roadmap); 12-page formats need a physically honest fold story before they exist at all — no page-count option ships without a real paper prototype. 🔴 (restraint as a feature)
- 🟠 **Page duplication & reordering** in reading order (rail long-press → drag; imposition recomputes silently — the engine's whole point).
- 💜 **Panoramic spreads:** one image across a facing pair (validated against the *post-fold* physical adjacency — research flagged that no mini-zine tool offers this; genuine differentiator). 🟨 ASSUMPTION on cost: pages render independently per panel today, and a facing pair lands on non-adjacent, possibly differently-rotated sheet panels — cross-panel slicing is real render + document-model + parity-test work, not a toggle. Prototype on paper first, budget as a feature, not a tweak.
- 🟢 **Smart photo slicing:** panoramic photo → auto-suggested spread placement.
- **Cover gets special status:** distinct frame in the rail ("your face to the world"), drives the shelf thumbnail live, and is where the tutorial zine begins. 🟠

---

## 15. Print workflow

The fold is sacred; everything around it gets staged as theater:

1. ✅ **Save-to-phone: already solved.** ADR-054 shipped in v0.8.0 (merge `7e2fa74`): Save PDF writes a real copy to Downloads; Share sends elsewhere; the frozen proof.html amendment explicitly keeps "Save to Files" as a distinct, non-redundant path. No action — noted here so the vision builds on the current honest baseline, not a stale one.
2. 💜 **The imposition reveal:** on entering Proof, the user's pages animate onto the flat sheet — some rotating upside-down — then the sheet folds itself into a booklet and flips open. Act 1's static "scrambled on purpose" copy becomes a 4-second proof-by-animation. This is the signature moment of the product; it converts the scariest step into the magic trick.
3. 🟠 **Test-fold sheet:** near-blank print (fold guides + tiny alignment mark + calibration ruler, finally shipping ADR-012's promise). The first wasted sheet becomes a cheap, deliberate one. Checklist stated in plain words: A4/Letter · landscape · 100% · single-sided.
4. 🟠 **Fold-along mode:** Act 3 grows into a full-screen, tap-through ritual sized for paper-in-hand, ending on "You made a zine." Keep the Success haptic; it's earned here.
5. 🟠 **Post-print recovery path:** "It came out wrong" → visual triage (shrunk? → scale was Fit-to-page; sideways? → portrait default) instead of a dead-end.
6. 🟢 **Share-as-image:** render a "look what I made" card (cover + 'made with paper, scissors & Zinely') to the OS share sheet. Distribution with zero network from us.
7. 🟢 **Printed-count tracking** feeds shelf graduation (§10.5) — a local counter, nothing more.

---

## 16. Roadmap

**Phase 0 — Repair trust (weeks, not months)** — all 🔴
Non-Latin honesty (input-time warning, per triage C1) · real shelf covers (wire the existing ADR-045 thumbnail pipeline — the plumbing is already in place) · Reframe drag/pinch enhancements (ADR-053 steppers stay authoritative) · error exits.

**Phase 1 — Teach the fold (the model year's core)** — 🔴/💜
First-run fold animation + one-question seeding · the tutorial zine ("retype me / swap this photo / … / now print me") · facing-pairs strip in the Bench rail · imposition reveal in Proof · Continue card + three on-ramps on Shelf · Settings surface.

**Phase 2 — Stock the bench** — 🟠/💜
Bundled supplies (stickers/tape/textures) + drawer with Recents/Favorites · font choice (vibe groups + pairings) + text presets · prompt-of-the-day + prompt packs · user-created packs · snapping/guides · contextual verb bar · test-fold sheet + fold-along mode · printed graduation.

**Phase 3 — Open the ecosystem** — 🟠/🟢 (gated on the network-amendment ADR)
Catalog + downloadable packs (hash-verified, explicit fetch) · seasonal spotlights · font packs · panoramic spreads + smart slicing · page duplication/reordering · share-as-image.

**Deliberately NOT in this vision:** accounts, cloud sync, community/social features, AI generation, 12/16-page options before a physical prototype, per-span rich text, a drawing engine before stickers prove the supplies model.

---

## Appendix A — Constraints honored (ADR-backed)

Imposition pure-Kotlin & canonical 8-page (ADR-007/018) · MVI editor (ADR-005) · parity renderer (ADR-006/027/028) · content-addressed private asset store, Photo Picker only (ADR-004/022/023) · atomic durability (ADR-009/021/026) · 300 DPI + safe areas (ADR-011/012) · Proof = one screen, three acts (ADR-051) · no in-app Print button (ADR-052) · fixed-frame Reframe with accessible steppers (ADR-053) · paper at creation (ADR-047) · invitation-only empty pages (ADR-033) · beginner-first progressive disclosure (ADR-008) · privacy invariant (PRD §5) — with exactly one proposed, explicitly-flagged amendment (§11).

**Explicitly challenged:** ADR-051 Decision B's total retirement of in-editor booklet/fold awareness (the facing-pairs strip revisits it in lightweight form) · the persistent shelf privacy pill · the always-visible wordmark · PRD §7's "networking of any kind" exclusion (§11's gated amendment) · the absence of any onboarding as an acceptable state (ADR-046 made Home the start destination; it didn't decide that *nothing* teaches the fold).

*Review note: this document was independently reviewed (verdict GO WITH FIXES); both Required Fixes — a stale Save-PDF claim and a false sheet-order-editing claim — were accepted and corrected against v0.8.0 `main`, along with four evidence-precision improvements.*
