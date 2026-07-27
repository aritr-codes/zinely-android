# V2-RESEARCH.md — UX/UI discovery for the Zinely V2 redesign

> **Status:** Phase 1 deliverable (Deep research report) of the V2 UI/UX redesign. Research-and-discovery
> only — **no Zinely screens are designed here.** This document gathers cited evidence and extracts
> transferable *principles*; the redesign itself begins at Phase 4 (design principles) and is not drawn
> until the HTML-prototype phase, per the [HTML-first UI workflow](../../CLAUDE.md).
>
> **Home for durable findings.** The [Documentation Rule](../../CLAUDE.md) normally lands research in
> [RESEARCH.md](../RESEARCH.md). That file is under active owner authorship and is deliberately left
> untouched; this V2-scoped document is the interim home. Durable, decision-shaping findings should be
> reconciled into RESEARCH.md when that work settles — cross-linked, never duplicated.
>
> **Method.** Six parallel Research Agents, web-search-based, each covering a cluster (journaling/notes ·
> creative/publishing · reading/collection/zine tools · trends+Material 3 · UX foundations · craft &
> emotion). Every non-obvious claim carries a source link. Claims are labelled per the
> [Research Standards](../../CLAUDE.md): ✅ **VERIFIED** (sourced) · 🟦 **RECOMMENDATION** ·
> 🟨 **ASSUMPTION** · ⚠️ **DISPUTED**. "So-what for Zinely" notes are directional, not screen designs.
>
> **The emotional brief (owner).** Soft, warm, paper-like, cozy, calm — *"sitting in a quiet café making
> something creative."* Premium-without-luxury, modern-without-sterile, playful-without-childish. The
> supplied matcha/strawberry/cream palette image is an **emotional reference only** — do not copy it.
> Preserve Zinely's paper-first identity and privacy invariants (no account, no cloud, no network, no
> analytics).

---

## 0. The five strategic findings (read these first)

1. **The 2026 zeitgeist is already where the brief points.** Mainstream mobile design has turned toward
   calm, warm, restrained, anti-"visual theatrics," and privacy-wary — over-animated UIs now read as
   "oddly aggressive" ([Envato](https://elements.envato.com/learn/ux-ui-design-trends)); 2026 is described
   as "a departure from the cold, sterile designs of previous years," restoring "depth, warmth… and
   personality" ([Elinext](https://www.elinext.com/services/ui-ux-design/trends/key-mobile-app-ui-ux-design-trends/)).
   ✅ Zinely's direction runs *with* the industry, not against it.

2. **The privacy invariants are a positioning asset, not a limitation.** The most-hyped 2026 creative-app
   features — cloud sync, AI-native/agentic editing, analytics-driven personalization — are exactly the
   ones users increasingly distrust as "creepy" ([WebProNews](https://www.webpronews.com/7-ui-pitfalls-mobile-app-developers-should-avoid-in-2026/)).
   Zinely gets to *decline them proudly*. The table-stakes it can nail (local undo/autosave, forgiving
   deletes, warm empty states, templates, contextual actions, tasteful haptics) overlap almost entirely
   with the calm/warm mood.

3. **Nobody owns "calm, warm, cozy" in the zine-tool category.** The category is bimodal: sterile
   PDF-in/PDF-out imposition utilities (Dirty Little Zine, Zeenster, ImpositionPDF, PDF Press, …) on one
   side, and frenetic whimsy ([Electric Zine Maker](https://alienmelon.itch.io/electric-zine-maker) — beloved,
   but its chaotic animated UI causes reported nausea and accessibility harm) on the other. A warm,
   beginner-first, **offline Android-native** app is an unclaimed position. ✅

4. **"The fold" is under-taught everywhere — Zinely's biggest unclaimed trust surface.** Every tool nails
   the imposition math ("pages reorder themselves; one print, one fold, one cut"), but human guidance at
   the fold is the category's universal gap — even Electric Zine Maker is dinged for unclear
   sequencing. Clear, gentle, illustrated fold guidance is a differentiator hiding in plain sight.

5. **Adopt Material 3 Expressive's *structure*; override its *skin*.** Keep window-size navigation rules,
   FAB-menu-over-stacked-FABs, emphasized-type tokens, token theming, spring-motion capability — but
   replace stock visuals: **warm neutrals over dynamic wallpaper color, a custom humanist serif+sans over
   Roboto, restrained "standard" motion over expressive bounce, softened shapes.** The differentiation
   lever is warm-neutrals + custom type + restrained motion, *not* exotic components.
   ([Android Authority](https://www.androidauthority.com/google-material-3-expressive-features-changes-availability-supported-devices-3556392/),
   [developer.android.com](https://developer.android.com/develop/ui/compose/designsystems/material3))

---

## 1. Competitive teardown — principles, not features

Grouped by what each teaches. Full "wrong for offline Zinely" flags are consolidated in [§1.6](#16-explicitly-wrong-for-an-offline-no-account-single-artifact-zinely).

### 1.1 Tonal north stars — warmth from fidelity to the paper metaphor

- **Goodnotes** — *"Write like it's paper. Edit like it's digital."* Warmth is manufactured by **fidelity
  to a physical metaphor**: real paper textures, pressure/speed-varying ink, notebook-as-object (choose
  cover + paper up front). Digital superpowers (undo, move) layer *onto* the paper feel without breaking
  it; gestures stay in-metaphor (scribble-to-erase, circle-to-lasso).
  ([Goodnotes](https://www.goodnotes.com/features/tablet-stylus-experience)) ✅ Closest tonal neighbour to
  Zinely's target.
- **Paper by WeTransfer** — *"cozy and capable through subtraction."* A deliberately small toolset; reviewers
  praise it as usable "without instructions," and its **prompt-filled journals are "Paper's special sauce"**
  — *the container is the onboarding* ([Cult of Mac](https://www.cultofmac.com/reviews/paper-by-wetransfer-paper-store-review),
  [JustUseApp](https://justuseapp.com/en/app/506003812/paper-by-wetransfer/reviews)). ✅
- **Transferable:** warmth comes from *respecting the artifact the user is making* (their paper, their
  handwriting, their content), not from decorative UI chrome. Zinely's paper-first identity is its warmth
  engine — lean into the material metaphor.

### 1.2 Print-trust mechanics — Canva · Adobe Express · InDesign · Affinity · Mixbook

- **InDesign Print Booklet** — the mature reference for Zinely's exact mechanic. It **previews actual
  printer spreads with marks before commit**, **auto-calculates margins for bleed**, and **Live Preflights
  continuously** as you work ([Adobe: preview](https://helpx.adobe.com/indesign/desktop/print/print-booklets/preview-booklet-printing.html),
  [settings](https://helpx.adobe.com/indesign/desktop/print/print-booklets/booklet-printing-settings.html),
  [live preflight](https://helpx.adobe.com/indesign/desktop/print/preflight/live-preflighting.html)). But it
  drowns beginners in jargon (slug/marks/spreads/bleed) — the overwhelm archetype. **Zinely's opportunity:
  keep InDesign's correctness guarantees, delete its vocabulary.** ✅
- **Canva** — template-first funnel so every intermediate state already looks "done"; beginner-friendly
  "without a tutorial." Its **Automated Proofing** pre-flights low-res / cut-off / too-small-text and
  phrases them as friendly warnings before you commit ([Canva](https://www.canva.com/help/proof-designs-print/),
  [Tekpon](https://tekpon.com/software/canva/reviews/)). Tone is slick-corporate — mechanics reference, not
  tonal one. ✅
- **Adobe Express** — **Quick Actions**: single-purpose, named jobs (remove background, resize) usable
  **without signing in** ([Adobe](https://helpx.adobe.com/express/mobile/whats-new/whats-new.html)). The
  no-login entry path is directly compatible with Zinely's no-account invariant. ✅
- **Affinity Publisher (iPad)** — pro layout on touch via a **context-sensitive Quick Menu** (only the
  current object's verbs, on demand); one-off purchase, no subscription. Con: "elements are quite small" —
  **density kills coziness** ([App Store](https://apps.apple.com/us/app/affinity-publisher-2-for-ipad/id1606942224)). ✅
- **Mixbook vs Artifact Uprising — the template tension.** Mixbook tops UX charts because **templates are a
  scaffold you can always leave**, with snap-guides + "safe zone" warnings preventing cut-off faces
  ([Teoprint](https://blog.teoprint.com/photobook-ux-review/)). Artifact Uprising's beautiful *restriction*
  produces books that "won't have your personal imprint" ([Tom's Guide](https://www.tomsguide.com/us/artifact-uprising-photo-book%2Creview-5873.html))
  — **over-curation triggers the "I didn't make this" feeling.** ✅ This is Zinely's own Article-7 concern,
  independently confirmed.

### 1.3 Journaling & notes — starting, and returning

- **Apple Journal** — *kills the blank page*: the `+` opens Smart Suggestions that assemble pre-composed
  cards to react to, plus "Save Without Writing." But its flat, unsearchable timeline is its top-cited flaw —
  entries "keep piling up" and become unretrievable ([MacStories](https://www.macstories.net/reviews/apples-journal-app-journaling-for-all/)).
  ✅ Take the suggestion-first onboarding; reject the retrieval-less stream.
- **Day One** — its most-loved feature is **"On This Day"**: the artifact *returns to greet its maker*, a
  delight that compounds over time ([Reflection](https://www.reflection.app/journaling-apps/day-one)). ⚠️
  Cloud-memory mechanism — reimplement the *feeling* locally, not the sync.
- **Notion** — the cautionary tale of feature bloat ("up to a year to learn"); redeemed by **progressive
  complexity** and a **pre-populated first workspace** ([onboardme](https://onboardme.substack.com/p/how-notion-solved-the-blank-page-product-strategy-deepdive),
  [Raw.Studio](https://raw.studio/blog/how-notion-ux-converts-100-million-users/)). Flexibility is the enemy
  of calm.
- **Craft** — **restraint reads as craft**: a smaller, opinionated block set = "much easier learning curve,"
  plus the best microinteractions in the set (cursor morphs into a drag handle to signal affordance)
  ([MacStories](https://www.macstories.net/reviews/craft-review-a-powerful-native-notes-and-collaboration-app/)). ✅
- **Apple Notes / Google Keep** — auto-organization (Smart Folders) and **color-as-a-first-class organizing
  dimension** over a card grid; Keep's card wall is the closest metaphor in the set to *objects on a table*
  ([HowToGeek](https://www.howtogeek.com/apple-notes-vs-google-keep-which-note-taking-app-is-better-on-a-mac/),
  [MakeUseOf](https://www.makeuseof.com/google-keep-vs-apple-notes-for-quick-notes/)). ✅ Strong fit for a
  paper library.
- **Obsidian** — ⚠️ anti-pattern: "power through configuration" ships a blank room and asks you to furnish
  it (setup 15–45 min, ships "bare until customized") ([Lindy](https://www.lindy.ai/blog/obsidian-review)).
  Keep its offline-file ownership; reject bring-your-own-UX.

### 1.4 Reading, collection, moodboard — the library-at-scale problem

- **Kindle/Kobo** — field research is blunt: **readers refuse to organize** (*"I don't enjoy it, so I don't
  organize"*), and covers alone fail memory (*"I don't remember what it's about"*)
  ([Kindle UX case study](http://medium.com/design-bootcamp/redesigning-amazon-kindle-iphone-app-ux-case-study-by-leo-vogel-5e5f0bc9c454)).
  Amazon's *variable* cover heights read as noise; Kobo/Onyx **normalize cover height** for a calmer grid
  ([the-ebook-reader](https://blog.the-ebook-reader.com/2023/12/15/list-view-or-cover-view-which-do-you-prefer/)).
  **Stability is a feature** — Kindle's constant UI churn is actively resented
  ([the-ebook-reader](https://blog.the-ebook-reader.com/2025/09/05/the-constant-random-kindle-ui-changes-are-really-obnoxious/)). ✅
- **Readwise Reader** — "calm" is earned by **consolidation, not organizational power**; the same review
  calls its tags/filters/views **over-engineered** ([SpeedReadingLounge](https://www.speedreadinglounge.com/readwise-reader-review)).
  Every organizational affordance is a cognitive tax. ✅
- **Pinterest** — the masonry grid reads as calm because it **respects each item's native shape** and gives
  a predictable next-item scan path ([Passionate Agency](https://passionates.com/pinterest-visual-discovery-social-commerce-giant/)).
  ⚠️ But masonry's calm depends on *varied* content — for homogenous paper covers a **uniform** grid may
  read calmer; test it.
- **Milanote** — warmth via **generous negative space + soft muted palette + an anchor element** that orders
  a scene without hard rules; ships templates so a blank canvas never intimidates
  ([Milanote](https://milanote.com/product/moodboarding)). ✅

### 1.5 The zine / booklet / imposition category (Zinely's direct space)

- **Landscape:** a thick tier of PDF→imposed-booklet utilities — [make-a-zine](https://make-a-zine.github.io/),
  [Dirty Little Zine](https://dirtylittlezine.com/) (in-browser, nothing uploaded, 300 DPI),
  [Zeenster](https://zeenster.com/) (free, open-source, privacy-respecting), [Zine Creator](https://zine-creator.com/),
  [PDF Press](https://pdfpress.app/), [ImpositionPDF](https://www.impositionpdf.com/), a
  [Figma plugin](https://www.figma.com/community/plugin/1140364746184639973/zine-maker). Creative authoring:
  [Electric Zine Maker](https://alienmelon.itch.io/electric-zine-maker) (the emotional benchmark), plus
  general suites (Canva/Affinity/InDesign). Mobile/local-first entrants are thin: [Zine.la](https://zine.la/),
  [Pocket Zine](https://pocketzineclub.com/). ✅
- **Does well:** imposition math is solved and trustworthy; privacy-by-architecture is common; correct print
  specs (300 DPI, A4/Letter, single sheet) are table stakes.
- **Underserved:** (a) mostly PDF-plumbing, not a creative *home* — they assume you designed elsewhere;
  (b) **the fold is under-taught**; (c) mobile-native is thin; (d) tone is bimodal (sterile *or* frenetic) —
  **nobody owns calm/warm**; (e) **no calm library** — these are one-shot make-and-export tools that don't
  treat your growing collection as a place worth returning to.
- **Electric Zine Maker, studied precisely:** beloved because it **restores creative joy and grants
  permission to be scrappy** — but its chaos is an accessibility liability (reported nausea, aggravated
  pain, dissociation) ([itch.io](https://alienmelon.itch.io/electric-zine-maker)). **The transferable
  insight: deliver the same permission-to-be-imperfect through calm and warmth rather than chaos.** Softness
  can carry "it's okay, just make something" without the accessibility cost.

### 1.6 Explicitly WRONG for an offline, no-account, single-artifact Zinely

- ⚠️ **Cloud-memory delight** (Day One "On This Day," Notion template marketplace) — depends on
  accounts/servers; reimplement the *feeling* locally.
- ⚠️ **Cloud/AI templates, "magic" actions, agentic/generative editing, analytics activation funnels** — the
  dominant 2026 pitch, but all imply network/model calls or instrumentation Zinely forbids. Take the *design*
  lesson (great empty states, low first-run friction); reject the *measurement/cloud* mechanism. Adobe
  Express' **no-login Quick Actions** is the one directly compatible entry pattern.
- ⚠️ **Power-through-configuration** (Obsidian, deep Notion nesting) — trades calm and first-run confidence
  for flexibility.
- ⚠️ **Flat, unsearchable stream** (Apple Journal) — fine for a diary nobody revisits, fatal for a
  reprint-oriented zine library.
- ⚠️ **Freeform-everything on a phone** (total free canvas) — relies on big-screen precision; bias to
  *guided slots with escape hatches* (Project Life grid + Mixbook break-out).

---

## 2. UX foundations & interaction patterns (the 35-topic canon)

Each: **best practice → why → failure mode → so-what for a calm paper-first app.** Load-bearing claims
sourced to NN/g, Material 3, and Android/WCAG primary docs.

### 2.1 Navigation — bottom bar vs rail vs hub
✅ Phone (<600dp): **bottom navigation bar, 3–5 destinations**; **navigation rail** only for medium+ windows
(tablet/foldable); never both at once ([M3 nav bar](https://m3.material.io/components/navigation-bar/overview),
[M3 nav rail](https://m3.material.io/components/navigation-rail/guidelines),
[NN/g](https://www.nngroup.com/articles/mobile-navigation-patterns/)). Hamburger menus measurably lower
discoverability; a nav "hub/home" only works when users do one task per session.
**So-what:** 🟦 Zinely's five surfaces (Library, Editor, Read, Print, Fold) sit *exactly at the ceiling* and
**are not peers** — Library/Read are global browse homes; Editor/Print/Fold are entered *from a specific
zine*. A flat 5-tab bar would spend the whole budget and mix global with document-scoped modes. Candidate
direction (an IA decision for Phase 5, not now): a small global bar (2–3 items) + Editor/Print/Fold as
contextual destinations reached from a zine.

### 2.2 FAB usage
✅ FAB = the *single* most important constructive action, one per screen, never destructive, always labelled
([M3 FAB](https://m3.material.io/components/floating-action-button/guidelines)). Material 3 Expressive's
**FAB-menu** officially replaces speed-dials/stacked FABs ([Android dev](https://developer.android.com/develop/ui/compose/designsystems/material3)).
**So-what:** 🟦 "New zine" on **Library** is a genuine single primary — a (softened) FAB or extended FAB fits.
The **Editor has many peer actions** (text/photo/sticker/page) — the exact case a single FAB *harms*; a
persistent low **supply tray / toolbar** is correct there (validates Zinely's existing supply-tray idea).

### 2.3 Information architecture & progressive disclosure
✅ Show only what the current step needs; defer advanced/rare features to a second layer one deliberate tap
away ([NN/g](https://www.nngroup.com/videos/progressive-disclosure/), [UXPin](https://www.uxpin.com/studio/blog/what-is-progressive-disclosure/)).
Failure modes: flatten-everything (overwhelm) *or* bury the primary action (common case pays a tax).
**So-what:** 🟦 Reinforces Zinely's house rule "every screen answers the user's current question." Default
each surface to 2–3 things; put alignment/imposition/advanced-print behind a "More" sheet — but never demote
*the* action a screen exists for.

### 2.4 Visual hierarchy & spacing (the primary calm lever)
✅ Adopt an **8pt spacing scale** (4/8/16/24/32/48…), tokenized; use **space as the hierarchy tool**
(internal ≤ external spacing groups by proximity); generous negative space is linked to reduced anxiety
([UX Planet](https://uxplanet.org/everything-you-should-know-about-8-point-grid-system-in-ux-design-b69cb945b18d),
[Design Systems Collective](https://www.designsystemscollective.com/spacing-alignment-in-ui-creating-visual-rhythm-and-breathing-room-2c382b112272)).
**So-what:** 🟦 The soft/paper/calm target is **primarily a spacing-and-restraint problem, not a color
problem.** Commit to one 8pt scale; bias one step larger than feels necessary.

### 2.5 Empty states & onboarding (teach by doing)
✅ Every empty state does three jobs — communicate status, teach what belongs, offer **one direct path to the
key action** — and in-context teaching beats forced upfront tours ([NN/g](https://www.nngroup.com/articles/empty-state-interface-design/),
[Smashing](https://www.smashingmagazine.com/2017/02/user-onboarding-empty-states-mobile-apps/)). ⚠️ Severe
trust failure: showing "empty" while a load is in flight.
**So-what:** 🟦 Zinely's first-run Library = a warm single-CTA invitation ("Make your first zine"), not a
coach-mark carousel; teach folding/printing *at the moment* the user first reaches Fold/Print. **The
container can be the onboarding** (Paper's prompt-filled journals; a prompt-seeded starter zine).

### 2.6 Search, library organization, sort/filter/tags/favourites
✅ Search earns its space only past a threshold; for small collections **Recent + Favourites + a simple sort**
beat a search box; when search exists, state scope and design an actionable "no results"; keep tags a small
curated set (avoid sprawl) ([NN/g](https://www.nngroup.com/articles/search-visible-and-simple/),
[Baymard](https://baymard.com/blog/no-results-page)).
**So-what:** 🟦 A personal zine library is small-to-medium and offline. Lead with Recent/Favourites/sort;
defer or omit search until libraries realistically grow; never make the user file things (Kindle proves they
won't).

### 2.7 Gestures, context menus, selection, drag
✅ Android grammar: **tap opens; long-press enters multi-select + raises a contextual action bar (CAB); drag
reorders** — long-press is for selection, not a floating menu ([Android menus](https://developer.android.com/develop/ui/views/components/menus),
[Material selection](https://m1.material.io/patterns/selection.html)). ⚠️ Gestures are invisible — always
provide a visible fallback (per-item overflow / explicit "Reorder").
**So-what:** 🟦 For page reorder (drag) and batch ops (delete/duplicate), use the standard grammar *plus* a
visible affordance — gesture-only discovery fails, and Zinely is beginner-first.

### 2.8 Undo/redo, loading, errors, success, autosave (the trust cluster)
- **Undo-first, confirm rarely.** ✅ Do frequent low-risk reversible actions immediately + offer Undo
  (snackbar); reserve confirm dialogs for rare/irreversible; prefer soft-delete/trash
  ([UXmatters](https://www.uxmatters.com/mt/archives/2020/03/are-you-sure-versus-undo-design-and-technology.php)).
  Guarding trivial actions trains dismiss-everything habituation.
- **Loading.** ✅ Skeletons for content/layout loads (feel faster), spinners only for short (<2s) ops,
  **optimistic UI** for frequent local actions ([LogRocket](https://blog.logrocket.com/ux-design/skeleton-loading-screen-design/)).
  🟨 Offline app → favour optimistic; reserve skeletons for thumbnail/PDF-render waits.
- **Errors.** ✅ Plain language, no codes, name problem + next step, **preserve the user's work**, place
  feedback inline ([Smashing](https://www.smashingmagazine.com/2022/08/error-messages-ux-design/)).
- **Autosave/success.** ✅ A quiet, glanceable "Saving…/Saved" builds trust; loud constant "Saved!" nags
  ([GitLab Pajamas](https://design.gitlab.com/product-foundations/saving-and-feedback/)).
- **Labeled/semantic undo** ("Undo move page") beats generic undo in creative apps
  ([WP Newsify](https://wpnewsify.com/blog/custom-undo-systems-how-modern-creative-apps-improve-editing-workflows)).
**So-what:** 🟦 This cluster is the direct antidote to the beta *"it lost my work"* wound
([ADR-058](../DECISIONS.md#adr-058)): local autosave with a calm cue, undo-first for edits/deletes, honest
recoverable export errors. **Trust *is* the calm target.** (The just-shipped [ADR-070](../DECISIONS.md#adr-070)
unsupported-character notice is one instance of this principle.)

### 2.9 Microinteractions — delightful vs gratuitous
✅ Motion must have a job, stay fast (~200–500ms), and follow restraint ("as much as possible with as little
as possible"); it should make the UI feel *faster* ([DesignerUp](https://designerup.co/blog/complete-guide-to-ui-animations-micro-interactions-and-tools/)).
Failure: decorative animation on the critical path; "delight" that repeats until it annoys.
**So-what:** 🟦 Soft, near-invisible motion (gentle page-turn/fold echoes of paper, sub-300ms, easing over
bounce); spend real delight only at milestones (first zine finished, export done, fold hand-off).

### 2.10 Accessibility (first-class, and mostly *aligned* with calm)
✅ Touch targets ≥ **48×48dp**; body text contrast ≥ **4.5:1** (large ≥3:1, UI components ≥3:1); support text
scaling; honor reduced-motion; label every control incl. icon-only; logical TalkBack focus order
([Android targets](https://support.google.com/accessibility/android/answer/7101858),
[WebAIM](https://webaim.org/articles/contrast/)). ⚠️ Verify against the **platform** accessibility tree, not
the merged Compose semantics tree (a control can pass `assertIsNotEnabled` while telling the platform it's
enabled — the exact defect the house device-verification recipe catches).
**So-what:** ✅ Calm and a11y mostly reinforce each other — generous 8pt spacing gives 48dp targets for free;
the **one place they fight is contrast on soft palettes**, which must be checked explicitly (see §3.4).

---

## 3. The warm-paper design *system* — reference → tokens, not pixels

> The discipline that separates a mature redesign from a mood-board pastiche: extract from the reference a
> few **decisions** and encode them as **tokens and roles**, then throw the image away. Four rulings from the
> reference: **temperature = warm · saturation ceiling = muted · contrast = soft surfaces / crisp ink ·
> tactile metaphor = paper.** Everything below serves those four.

### 3.1 Colour — warm neutrals as "paper"
✅ The dominant surface should be a **warm off-white, not `#FFFFFF`**; the neutral ramp is *tinted toward the
brand temperature* (warm grey, never true grey) — warm neutrals read as "paper, linen, warm wood, cream
ceramics" ([ColorArchive: neutrals](https://colorarchive.org/guides/neutral-color-palettes/)). Warmth is
carried by the ~90% of pixels that are neutral; accents only punctuate.
🟦 Build a 9–12-step neutral ramp with a slight shared warm tint; **decide neutral temperature *after* the
accents** so greys agree with them. Two-tier tokens: primitive (`cream-50…ink-900`) → semantic
(`surface.paper`, `surface.raised`, `text.ink`, `text.subtle`, `border.hairline`); components consume
semantic only.

### 3.2 Colour — muted, not desaturated
✅ **Mute** accents (shift toward a neutral anchor while keeping hue identity), don't **desaturate** (drag to
grey → "muddy mid-tones with no character") ([ColorArchive: muted vs desaturated](https://colorarchive.org/notes/june-2026-muted-vs-desaturated/)).
🟦 Keep accents to **two brand hues + neutrals** (matcha = tranquil primary; strawberry = warm punctuation),
each at a consistent perceptual distance from neutral; matcha+strawberry+cream is safe *because the hues are
far apart* (low-chroma near-hues clash). Pitfall: a full-chroma accent "just for the CTA" shatters the calm
register.

### 3.3 Colour — author in OKLCH
✅ Generate ramps in a **perceptually uniform space (OKLCH/OKLab)** so equal numeric steps look equal; HSL
lies about lightness across hues, giving a "calm" palette hot spots ([ColorArchive: OKLCH](https://colorarchive.org/guides/oklch-perceptual-color-design-guide/)).
🟦 Fix an L/C target per role (all "accent-fill" tokens at one L/C, only H varies) → auditable palette;
gamut-clamp to sRGB before exporting to Compose `Color(...)`.

### 3.4 Colour — soft *and* accessible (the core tension)
✅ Put softness in **surfaces and decoration**; keep **text-to-background contrast crisp**. WCAG AA: body
**4.5:1**, large text **3:1**, UI components/icons/borders **3:1** ([TestParty](https://testparty.ai/blog/wcag-contrast-ratio-guide-2025)).
🟦 Separate **content tokens** (`text.ink`, `text.subtle` — contrast-governed, near-black warm ink) from
**expressive tokens** (`accent.matcha.surface` — mood-governed, may be soft *as a fill behind dark ink, never
as text on cream*). **Automate contrast checks in CI** on every semantic pair so a "prettier, softer" tweak
can't silently break AA. The seductive failure: muted-grey caption text on cream at ~3:1.

### 3.5 Colour — keep dark mode *warm*
✅ **Re-derive, don't invert.** Warm charcoal base at **L≈8–12%** (a dark low-chroma brown/ink, not blue-black,
not `#000`), surfaces step up by lightness; **re-tune accents for the dark ground** (muted accents "disappear"
otherwise) → a separate token set per theme pointing at the same semantic roles
([ColorArchive: dark mode](https://colorarchive.org/guides/dark-mode-color-design-guide/)). Dark mode's
default failure is going cold/clinical — the opposite of "quiet café."

### 3.6 Colour — mapping onto Material 3
✅ M3 = 5 key colours → 13-tone palettes → roles → components, accessible-by-default
([Material 3 color](https://m2.material.io/design/color/the-color-system.html)). 🟦 Seed the theme builder
with **matcha = primary, strawberry = tertiary, cream = neutral seed**, then **override generated tones toward
the muted OKLCH targets** — don't ship the generator's default vivid tones (chroma 48 / tone 40), that's the
generic bright-Material look. ⚠️ **Reject dynamic (wallpaper) color as identity** — keep brand primaries on
identity-critical controls; let dynamic tones (if any) influence only neutral surfaces.

### 3.7 Typography — serif-for-voice, sans-for-work
✅ The dependable premium pairing: **a warm humanist serif (display/subhead) + a clean humanist sans
(body/UI)** — the structural difference does the hierarchy work automatically; typical in editorial/luxury
([Jukebox](https://www.jukeboxprint.com/fonts/font-pairing/sans-serif-and-serif),
[Rajinder Gill](https://rajindersgill.com/10-beautiful-serif-sans-serif-font-pairings-for-premium-brand)).
🟦 Zines are a *print* form; a serif voice honours that lineage. Pair on mood, contrast on structure (humanist
+ humanist). Keep the serif in display/subhead only; never long body or microcopy in a high-contrast serif
(avoid cold "didone"/Playfair-class except large display). Bind fonts to Zinely's existing **type roles**,
not screens. Custom type ships locally — no network dependency, and it's the single biggest cue that beats a
generic Material look.

### 3.8 Typography — a small register scale; restraint = premium
✅ A short, well-spaced scale (~5 registers: display/subhead/body/metadata/caption on a modular scale) with
generous line-height reads more premium than many sizes/weights; let *weight and colour* (ink vs subtle)
carry secondary hierarchy ([TypeUI](https://www.typeui.sh/design-skills/premium)). Pitfall: register creep;
too-tight leading makes calm feel cramped. (Zinely already runs a 5-register system — §2.1 of the design
system.)

### 3.9 Spacing & layout that breathes
✅ **Whitespace is active** — users scan it first, it "feels like a breath," and in high-stress contexts it
mitigates anxiety (Tobii eye-tracking) ([Kanso](https://kanso.framer.media/blog/designing-for-calm-ux-beyond-the-screen)).
🟦 One tokenized 8pt `space.*` scale for all margin/padding/gap; tie line-heights to the same increments
(~1.5–1.6 multiples) for vertical rhythm; Swiss discipline — strong grid, generous margins, one focal element
per view (dovetails with "every screen answers one question"). ⚠️ Exact "+33% comprehension" figures are
secondary reporting — direction is sound, magnitudes DISPUTED.

### 3.10 Motion — calm and communicative
✅ Gentle, purposeful, tokenized (`duration.*`, `easing.*`); motion is where "quality" is *felt* — smooth
motion strengthens perceived reliability, jank "damages trust" ([Central West](https://www.centralwestgippslandpcp.com/gentle-motion-effects/)).
🟦 Use the **standard** motion scheme by default; reserve gentle spring for a few signature moments; **honor
reduced-motion as first-class** (Android "Remove animations") with a calm cross-fade/instant alternative,
never gating information behind an animation a reduced-motion user won't see
([Smashing](https://www.smashingmagazine.com/2020/09/design-reduced-motion-sensitivities/)).

### 3.11 Delight & microinteractions for *this* product
✅ Microinteractions "humanize an interface, confirm actions, guide the user" and raise **perceived** quality
by targeting friction/latency ([Charisol Pulse](https://medium.com/charisol-pulse/better-ux-through-microinteractions-the-details-that-define-delight-d8fd4b8b22a6)).
🟦 **Spend delight at Zinely's emotional-arc beats:** finishing a page, completing a zine, a successful
export, the fold hand-off — a calm warm completion moment there beats ten decorative flourishes. **Tokenize
voice** (a short copy style guide: encouraging, plain, never cute-to-childish) the way colour is tokenized;
subtle haptics on meaningful completions only; define one reusable `feedback` pattern (visual + optional
haptic + copy) so delight is coherent, not sprinkled.

### 3.12 Paper/tactile — without skeuomorphic kitsch
✅ Evoke paper through **restrained texture in the *material layer* (surfaces), not the *chrome***; a barely-
perceptible grain on `surface.paper`/canvas is warm, a leather toolbar or ring-binder nav is kitsch — "depth,
texture, material metaphors specifically where they communicate something useful," not 2010 fake leather
([Codexical](https://www.codexical.com/posts/2026-05-24-skeuomorphism-revival-flat-design-reaction),
[Oreate](https://www.oreateai.com/blog/the-art-of-paper-texture-transforming-digital-designs-with-tactile-depth/c44ff2879ca491f612711f35a6ca8a9d)).
🟦 Very low intensity; tokenize as one `surface.texture` treatment with a dark-mode variant (grain on warm
charcoal, not black); degrade to flat warm colour under reduced-transparency/low-power; bundle the asset
(tiny, offline). Pitfalls: heavy textures fight contrast; visible tiling looks cheap; curling corners / torn
edges / coffee-stains read dated — the brief's explicit "no."

---

## 4. Trends, Material 3, and 2026 expectations

### 4.1 Worth adopting
✅ Calm interfaces / reduced cognitive load; warm/human/emotional UI; **purposeful** (explanatory) motion;
**selective tactility** (disciplined material metaphor, blur/grain as emotive cues)
([Envato](https://elements.envato.com/learn/ux-ui-design-trends), [Elinext](https://www.elinext.com/services/ui-ux-design/trends/key-mobile-app-ui-ux-design-trends/),
[Codexical](https://www.codexical.com/posts/2026-05-24-skeuomorphism-revival-flat-design-reaction)).

### 4.2 Hype / handle with care
⚠️ Heavy glassmorphism / glossy "glassy motion" (off-brand for paper); ⚠️ neumorphism (borrow the softness,
not its low-contrast execution — it has a documented contrast problem); ⚠️ AI-native/agentic UX &
hyper-personalization (inapplicable *and* invariant-violating for Zinely)
([Coloura](https://coloura.co.uk/inside-2026-design-hyperpersonalized-ui-replaces-flat-with-texture-and-glassy-motion/),
[IxDF neumorphism](https://ixdf.org/literature/topics/neumorphism)).

### 4.3 Outdated / avoid
✅ Icon-only nav (comprehension −30–40%; pair icon+label); hidden gestures without fallback; hamburger/deep
nesting on touch; excessive animation; destructive actions adjacent to benign ones; dead/silent empty states;
a11y as an afterthought ([WebProNews](https://www.webpronews.com/7-ui-pitfalls-mobile-app-developers-should-avoid-in-2026/),
[NN/g proximity](https://www.nngroup.com/articles/proximity-consequential-options/)). ⚠️ The minimalist-paper
temptation to go icon-only/gesture-only for "cleanliness" is the trap to resist.

### 4.4 Material 3 / Expressive (shipped May 2025; treat as mid-rollout "3.5")
✅ Spring-physics motion (expressive vs standard schemes); expanded shape system + shape-morphing;
**emphasized** type tokens (`titleLargeEmphasized`) for focal moments; unchanged accessible tonal color +
dynamic color; window-size navigation rules; **FAB-menu** replacing stacked FABs; **floating toolbars**
([Android Authority](https://www.androidauthority.com/google-material-3-expressive-features-changes-availability-supported-devices-3556392/),
[Supercharge](https://supercharge.design/blog/material-3-expressive), [9to5Google](https://9to5google.com/2025/12/27/recap-material-3-expressive/)).
**Deviate from stock on:** dynamic-color-as-identity, Roboto, expressive-bounce-as-default, hard flat shapes,
glossy tonal-elevation overlays. Pin versions (APIs evolving).

### 4.5 Common creative-app mistakes
✅ Feature bloat / overloaded canvases; hidden affordances "for cleanliness"; weak hierarchy (can't find
features / track progress); destructive-action traps; onboarding walls; empty-state neglect; **untrustworthy
undo** ([Eleken](https://www.eleken.co/blog-posts/ux-design-mistakes), [WP Newsify](https://wpnewsify.com/blog/custom-undo-systems-how-modern-creative-apps-improve-editing-workflows)).
**So-what:** the master failure is **answering the tool's questions instead of the user's** — precisely the
beta "Preview" lesson ([ADR-058](../DECISIONS.md#adr-058)).

### 4.6 Features expected in 2026 — table-stakes vs differentiators
- **Table stakes:** rock-solid (local) undo/redo · autosave (undo tested across autosave) · forgiving
  destructive actions · guided empty-state first-run · templates/quick-create · a11y built-in · purposeful
  progress feedback.
- **Differentiators (delight):** contextual/on-object actions · gestures *with* visible fallbacks · tasteful
  haptics + gentle spring · **semantic labeled undo**.
- ⚠️ **Refuse (invariant-violating):** cloud sync/accounts · AI-native/generative/agentic features ·
  analytics personalization. Take the *design* lessons, reject the *cloud/AI/measurement* mechanisms.

### 4.7 What separates *excellent* from *merely functional*
✅ **Aesthetic-usability effect** (Kurosu & Kashimura 1995; NN/g): attractive products are perceived as more
usable and buy **tolerance for *minor* issues** ([NN/g](https://www.nngroup.com/articles/aesthetic-usability-effect/)).
For a privacy-first small app, the warm/calm system is a **trust-and-forgiveness reservoir**. ⚠️ Bounded:
beauty forgives *minor* not *major* problems, and it *masks* real defects in testing — which is exactly why
Zinely keeps its two-pass device verification (built-it-right vs is-it-right). ✅ **Calm technology** (Weiser/
Brown; Amber Case) justifies *subtraction* — the tool recedes so the zine is the loud object
([Amber Case](https://www.caseorganic.com/post/principles-of-calm-technology)).

---

## 5. Consolidated principles — the seed for Phase 4

1. **The zeitgeist is on our side** — calm/warm/restrained/privacy-wary is where 2026 already sits; lean in,
   don't chase novelty.
2. **Warmth is materials, not decoration** — paper metaphor, warm neutrals, real type; the tool recedes so
   the zine is loud.
3. **Restraint is the aesthetic** — spacing (8pt), few visible destinations, quiet motion, a small type scale.
4. **Trust is the emotional core** — local autosave + undo-first + honest recoverable errors answer the beta
   "it lost my work" wound directly.
5. **Kill the blank page** — a warm starting point / prompt-seeded starter over an empty canvas *and* over a
   tutorial wall; the container is the onboarding.
6. **Templates are a scaffold you can leave** — never a cage; ownership comes from many small tactile choices
   + the user's own content (avoid the "I didn't make this" failure).
7. **The library is a place to return to** — a calm, uniform, self-curating grid (Recent/Favourites), stable
   over time; cards defeat memory by themselves.
8. **Own the fold** — the category's under-taught trust surface; gentle, illustrated, at the moment of need.
9. **Do the print math invisibly, delete the jargon** — InDesign's correctness, none of its vocabulary; show
   the folded result before commit; preflight quietly in plain words.
10. **Deliver permission-to-be-imperfect through calm, not chaos** — Electric Zine Maker's joy without its
    accessibility cost.
11. **Adopt M3 structure, override its skin** — warm neutrals + custom type + restrained motion is the whole
    differentiation lever.
12. **Privacy invariants are a feature** — proudly decline cloud/AI/analytics; everything on-device and
    deterministic.
13. **Reference → system** — two-tier semantic tokens (colour/space/type/motion/texture/voice), roles-not-
    screens, guarded by CI contrast checks and reduced-motion/transparency fallbacks, verified by the two-pass
    device gate.

---

## 6. Open questions the research surfaced (for later phases / owner)

These are **not decisions to make now** — they are flagged so Phases 5–10 resolve them deliberately, and a
few are genuine owner calls:

- **Q1 (IA, Phase 5).** Do the five surfaces become a flat bottom bar, or a small global bar (Library/Read/
  New) + Editor/Print/Fold as document-scoped destinations? Research leans toward the latter; decide in IA.
- **Q2 (scope).** Does V2 target tablets/foldables (≥600dp)? Only then does navigation-rail work matter.
- **Q3 (owner, high-leverage).** Confirm the deviation lever from stock Material 3 = **warm neutrals + custom
  humanist serif/sans + restrained motion**. This is the single biggest identity choice.
- **Q4 (type).** Which specific warm humanist serif joins the existing sans in the display/subhead roles?
  (Bundled, offline, print-legible.)
- **Q5 (library layout).** Uniform grid vs pseudo-masonry for homogenous paper covers — resolve by testing,
  not assumption.
- **Q6 (design-system continuity).** V2 must reconcile with the *accepted* V1 design corpus (the ratified
  registers, precedence order, square-artifact rule, type roles — ADR-061…068). V2 is an *elevation* of that
  system's warmth, **not** a re-litigation of its accepted rulings; genuine conflicts get escalated, not
  quietly overridden.

---

## Sources
Consolidated inline above. Primary/authoritative anchors: Material 3 ([m3.material.io](https://m3.material.io),
[developer.android.com](https://developer.android.com/develop/ui/compose/designsystems/material3)),
Nielsen Norman Group ([nngroup.com](https://www.nngroup.com/)), WCAG/WebAIM
([webaim.org](https://webaim.org/articles/contrast/)), Android accessibility docs, and vendor product docs
(Adobe, Canva, Goodnotes, Affinity). Practitioner references (ColorArchive, MacStories, UXmatters, Smashing,
LogRocket, the-ebook-reader) corroborate synthesis points and are labelled 🟦/🟨/⚠️ where recommendation,
assumption, or contested. Known gaps to close before durable landing in RESEARCH.md: no first-party
Refactoring UI citation (color/spacing represented via convergent secondary sources); Zeenster feature detail
unconfirmed (JS-only page); vendor engagement percentages (masonry, whitespace) marked ⚠️ DISPUTED.

*Compiled 2026-07-27 from six parallel Research Agents. Research deliverable — no code/design decision, so no
Review Agent pass; fact-check any load-bearing citation before it informs a V2 ADR.*
