# V2-BENCH-REVIEW.md — comprehensive pre-freeze design review of the Bench

> **Status:** Phase 9 deliverable of the [Bench initiative](V2-BENCH-RESEARCH.md) — a comprehensive design
> review of the [current prototype](mockups/v2-bench.html) against a raised bar: not *"can users edit a
> page?"* but *"can users happily spend hours creating inside this — a calm, delightful, expressive
> **studio**?"* Builds on the prototype's specs ([principles](V2-BENCH-PRINCIPLES.md) BP/EP,
> [IA & interaction](V2-BENCH-IA-INTERACTION.md), [research](V2-BENCH-RESEARCH.md) BR§). Method: five
> independent streams — studio/long-session UX, asset-drawer architecture, identity-vs-expression colour,
> microinteractions/ergonomics, and an **adversarial falsification** of the prototype — each cited and
> labelled ✅/🟦/🟨/⚠️. The adversarial review is the independent Review-Agent pass required by
> [CLAUDE.md](../../CLAUDE.md) for substantial UI work. Research/architecture recommendations that harden
> into ADRs (asset layer, colour namespace) still owe a review + legal pass, flagged inline.
>
> **Framing accepted (owner, 2026-07-28):** the Bench is a **"studio," not a "document editor"** — a place
> to gather materials, experiment, rearrange, iterate. This is not a redesign; the current prototype is the
> baseline and its direction holds. The studio framing is what the additions below serve.

---

## 1. Overall assessment

The Bench prototype has a **correct spine and an incomplete room.** The spine — page-is-hero, edit-where-it-
lives, contextual chrome, calm paper materiality, `preview == export` — is right and, on the load-bearing
interaction, *verified*: the adversarial review confirmed the in-place caret + rigid-body page-pan returns
**pixel-identical**, with the style toolbar above the keyboard and the finished-book reveal correctly left
to Read (BP-7). That is the hardest thing in the whole initiative and it is built.

But the raised bar exposes a real gap: **the current Bench is a very good single-page editor, not yet a
studio.** A studio has one property the prototype lacks — *a place to keep materials you've gathered but not
yet placed* (BR-studio §1, Milanote). It also assumes a workspace you **return to** mid-thought, **scales**
from a 1-page note to a ~32-page zine (a scope the owner just named — the fixed 8-dot ribbon does not survive
that), and gives makers **something rich to make *with*** (colour, paper, texture, art) while keeping the app
itself quiet. None of these are redesigns; they are the elevation from "editor" to "studio," and they are
best decided while the design is still fluid.

**The one-sentence verdict:** the direction is right and the core interaction is proven, but the studio
framing introduces enough genuinely-new, high-value surface (a holding tray, page-nav that scales to 32, a
richer Art drawer, a maker colour/ink palette) that **one more focused iteration is justified before
freeze** — see [§12](#12-final-recommendation-freeze-or-iterate).

---

## 2. What is already excellent — keep unchanged

Verified in the adversarial pass and the research; **protect these, do not "improve" them:**

- **The rigid-body in-place edit.** Caret in the real text; whole page pans as one unit; returns
  pixel-identical; style toolbar above the keyboard. This is the signature and it is correct. The only
  refinement (§9) is *how* it moves — spring-not-tween, synced to the IME animation.
- **`preview == export` from one engine** (`CanvasReplayer`) — a structural trust advantage pro tools can't
  match. Never add a second render path.
- **Page-is-hero at rest; focus by dimming, not decoration.** The resting surface is quiet; selecting one
  element recedes the rest. Both research streams independently endorse this as *the* studio move that keeps
  it from becoming Canva.
- **Contextual chrome that never occludes the element**; every gesture keeps a visible twin; delete is
  soft-delete + visible undo, no modal. This is exactly the "fearless undo enables play" foundation
  (BR-studio §4, Figma model).
- **The restrained interface palette** (two brand hues + one consequence). The colour stream's clearest
  finding: **keep the chrome restrained** — a quiet chrome is *what lets the maker's colour read true*
  (BR-colour §2, NN/g 60-30-10). The answer to "the Bench feels colour-poor" is a richer *content* palette,
  never a richer interface.
- **The Bench/Read boundary** (BP-7): the finished-book reveal lives on Read. Respected. Keep it there.
- **Honest materiality**: riso grain, warm paper, dark-as-a-room-not-an-inversion. Keep.

---

## 3. High-impact improvements — before DESIGN FREEZE

These four make the Bench a *studio* and are cheap to decide now, expensive to retrofit later. (Prototype
defects RF-1…RF-5 from the adversarial pass are **already fixed** and republished — they are table stakes,
not listed here.)

### H1 — A holding tray (the single strongest "studio" signal)
Add a persistent, low-pressure **tray/shelf** — a place to drop photos and snippets you've *gathered but not
yet placed* (BR-studio §1). "Collect now, arrange later" separates gathering from committing, which lowers
the stakes of starting and is the behavioural core of the studio model (and a second antidote to blank-page
paralysis). **Cost:** one edge strip, collapsible, never covering the page. **Why before freeze:** it
changes the IA (a new holding zone distinct from the page) and the empty-state story.

### H2 — Page navigation that scales 1 → 32 (the ribbon must morph)
The 8-dot ribbon is a small-N pattern and breaks past ~8 (BR-studio §2). Adopt the **adaptive morph**, one
component: **dots (≤8) → horizontally scrolling filmstrip of small thumbnails (9–32) → a summoned
full-screen page-grid** (drag-reorder / add / delete) for whole-zine management. Small thumbnails navigate
better than large ones. **Why before freeze:** the owner named 32-page zines as in-scope; a fixed ribbon in
the frozen spec would be wrong on day one.

### H3 — "Add" evolves into one calm "Art" drawer (not many tabs)
Keep **Add = 3 verbs** (Text · Photo · Art), but make **Art a single surface** filtered by chips
(illustrations · icons · frames · patterns), **search-first** with one query box spanning bundled + online,
and a **Recent + ⭐Favourites** rail as the *only* management (recognition over recall — BR-assets §P3). Online
search stays exactly **one visible step deeper = the privacy seam**. Insertion = **tap → lands at page
centre, pre-selected**, drag optional, **snap-on-move** (BR-assets placement). **Why before freeze:** this is
the asset IA the frozen spec must encode; it also fixes O-2 (badge should mean *downloaded*, not
*downloadable*).

### H4 — Give makers a real palette to make *with* (content colour, separate namespace)
The prototype exposes almost no expressive colour. Introduce a **maker "ink" palette** modelled on
risograph spot-ink practice (BR-colour §3): **~12–18 named inks** in three bands (**inks** · **paper tints**
· **neutrals**), **swatch-first** picker with **3–5 ready-made harmonious palettes** so a beginner can't make
mud, full custom picker one step deeper. Crucially, it lives in a **separate token namespace** (`content.*`)
that no chrome component may read, and `consequence` red stays chrome-only. **Why before freeze:** it's a new
content system that touches the colour ADR and the asset drawer; deciding its *shape* now keeps the frozen
spec coherent. (The full ink set + coverage rules can land as the governed colour ADR; the *drawer's place*
must be in the frozen HTML.)

---

## 4. Medium-term improvements (post-freeze, tracked)
- **Layouts as empty skeletons** — grids/frames with empty slots that *rearrange the maker's existing
  content*, never drop a stranger's finished page (preserves the IKEA effect; dodges the ADR-058 "it
  replaced my zine" trauma). BR-assets recommends **deferring layouts entirely for MVP** — an empty page +
  snap guides may deliver 80% of the value at 0% marketplace risk. Add only if playtests show paralysis.
- **Paper stocks & textures as pickable material** (cream/kraft/blush/grey; grain/laid/recycled-fleck/riso-
  mottle) — expressive richness with zero new chrome colour (BR-colour §4).
- **Ink-coverage / legibility review** — flag pages with heavy saturated fills that band/warp/cost ink on
  home printers; guarantee text-on-fill contrast (extend the CI ★-pairing gate to content). Threshold tuned
  on real hardware (🟨).
- **Return-to-where-you-left** — reopen the Bench at the same page/scroll/zoom with the tray intact (a studio
  you re-enter mid-thought). Interacts with the session-only-undo question ([BC§3.6](V2-BENCH-CRITIQUE.md)).
- **Gesture accelerators** — two-finger-tap undo / three-finger redo, swipe between pages (each keeping its
  visible twin).

## 5. Long-term roadmap ideas
- **Downloadable asset packs** (curated, CC0/PD/MIT, print-safe) — the Layer-3 online library maturing;
  never infinite-scroll, no trending/ratings (that's a store, not a drawer).
- **Grow-your-own palettes** — save custom swatches/palettes (Procreate model), import.
- **Richer templates** as the library matures — always empty-skeleton framed.
- **Optional online asset search polish** — capped results, curated ranking, provenance stored.

---

## 6. Creative-expression audit
Scored against the owner's questions:

| Question | Today | After H1–H4 |
|---|---|---|
| Encourage experimentation? | Partial — undo is safe, but nothing to *gather/rearrange* | Yes — tray + fearless undo + rearrange |
| Reward exploration? | Weak — thin expressive vocabulary | Yes — ink palette, papers, Art drawer |
| Make creation *enjoyable*? | Neutral — competent, not yet joyful | Yes — tactile motion, materials to play with |
| Enjoy an hour inside it? | Not yet — no studio holding-state, nav won't scale | Yes — studio tray, scaling nav, return-to-state |
| Reduce creative friction? | Good on *operational* friction | Keep — but **preserve creative friction** (below) |
| Disappear while creating? | Yes — page-is-hero holds | Keep |
| Build trust? | Yes — visible autosave, soft-delete, no modals | Keep |
| Delight without playful noise? | Under-delivered (delight deferred to Compose) | §10 |

**The load-bearing nuance (BR-studio §3, flow theory):** remove *operational* friction ruthlessly, but
**preserve creative friction** — the interesting-hard of composing a page. Do **not** add auto-arrange,
"magic design," or templates-that-design-for-you. Those delete the challenge that produces flow and reframe
the maker as a *picker*, not a *maker* — the opposite of "happily spend an hour creating," and a direct hit
to the IKEA-effect ownership the whole product depends on. **This is the single most important guardrail in
this review.**

---

## 7. Asset-architecture recommendations
(Feeds the governed asset ADR; still owes review + legal sign-off per [BR§7](V2-BENCH-RESEARCH.md).)

- **Add = 3 verbs; Art = one chip-filtered surface**, never a tab-per-noun. Search-first, single query box
  spanning bundled + online.
- **Four jobs as one funnel:** discover (search + chips + curated-first) → insert (tap = place-at-centre,
  pre-selected) → manage (**only** Recent + ⭐, no folders/tags) → reuse (Recent makes the 2nd insertion
  near-zero cost — critical in long sessions).
- **Maker content ranks above clip-art.** Photo/Text at the top of Add; illustration is the *garnish*,
  ordered last. A zine that's 80% someone else's art kills ownership.
- **Bundled vs downloaded by *section*, not per-tile badges.** Bundled lives in Art; online results join
  Recent once inserted. State licensing safety **once globally**, never per tile — the calm comes from *not
  having to explain licensing*.
- **Print-safe by construction:** CC0/PD/MIT-first (the print/sell constraint from [BR§7](V2-BENCH-RESEARCH.md));
  online search sends **only a keyword** — the invariant the prototype already states honestly.
- **Guardrails against becoming a store:** finite curated bundled set, no infinite scroll, no trending/
  ratings/"more like this," online is the only unbounded surface and it's one step away.

## 8. Colour-system recommendations
The owner's question — is the restrained editor palette the right balance? — answered decisively:

- **Keep the *interface* palette restrained. Do not enrich the chrome.** A quiet, near-neutral chrome is
  what lets the maker's colour read true (BR-colour §2). Enriching chrome would dilute the single-primary
  "your next move" signal and contaminate user colour.
- **Enrich the *content* palette instead** (H4): the riso ink model, ~12–18 named inks, 3 bands, swatch-
  first, harmonious presets, custom picker one step deeper.
- **Two separate namespaces, enforced.** `chrome.*` (semantic: matcha=action, strawberry=punctuation,
  consequence=error) and `content.*` (expression). No component reads across; `consequence` red never enters
  the maker set. A lint rule keeps them apart. The 4 cover inks stay for *cover identity* (ADR-069); the
  in-page maker set is distinct and larger.
- **This is exactly the owner's caution honoured:** a semantic interface colour is not promoted to a
  user-facing colour just because it exists.

## 9. Interaction improvements (microinteractions & ergonomics)
- **Motion = spring physics, interruptible, low-bounce.** The page-settle uses `dampingRatio ≥ ~0.9` (no
  visible overshoot — bounce is the #1 "toy not paper" tell) and must resolve to the **exact stored
  transform**. A page you can grab mid-settle feels like paper; one that ignores you until a tween finishes
  feels like software (BR-micro §1).
- **Sync the keyboard pan to the IME insets animation** (`WindowInsetsAnimation` / `imePadding`) — never an
  independent tween. Two surfaces sliding out of step is the biggest "unfinished software" tell.
- **Snapping as a magnetic force-field with hysteresis:** snap to page centre-lines, safe-margin box, and
  sibling edges/centres (not a fine grid); guides appear *only during drag*, one axis at a time; **one light
  haptic tick on snap-engage only** (never on break, never per-frame).
- **Insertion materialises at its spot, pre-selected** (96→100% + fade, ~300 ms no-bounce) — "you set this
  down here," not "it flew in from a toolbar."
- **Undo reverses the *specific* action** (the deleted element fades back *at its old spot*), so the eye
  catches *what* returned; a light haptic confirms even off-screen changes.
- **Autosave stays a whisper** — persistent quiet "Saved," zero position change, slow fade, no toast, no
  haptic. A save that animates loudly reads as malfunction.
- **Haptics on discrete state-changes only** (snap-engage, grab-handle, insert-land, undo, long-press) —
  never continuous motion or every keystroke (the IME owns keyboard haptics).
- **Interaction audit → the calmest resting state:** page + "+" + quiet "Saved" + page-nav, everything else
  contextual. Selection toolbar contextual; text-edit a gesture (with visible twin); alignment folds into
  behaviour (guides auto-appear, no persistent toggle); Save disappears into autosave.
- **Reduced motion:** replace positional/scale transitions with cuts/≤100 ms cross-fades, but **keep the
  non-motion information** — snap still engages, haptics still fire, "Saved" still updates, handles still
  appear (WCAG 2.3.3).

## 10. Delight opportunities (earned, never noise)
Delight must attach to an accomplishment or it's noise (BR§6). Spend it on: the **paper-settle** when a page/
element lands; the **snap-tick** when alignment clicks home (a magnet finding its place); the **materialise-
at-spot** of a placed asset; the quiet **"Saved · on this device."** Cut: confetti, mascots, idle animation,
bouncy overshoot, anything peppy. Warmth here reads as a satisfying physical *settle*, not a celebration —
ambient delight (things feel good as you touch them) over modal delight (things stop you to congratulate you).

## 11. Risks to avoid
- **Auto-magic / template-that-designs-for-you** — deletes creative friction and ownership. The top risk.
- **Permanent multi-panel chrome / a docked inspector** — the moment resize/colour live in an always-open
  side panel, it's a document editor, not a studio. Keep it contextual and on-canvas.
- **Ribbon that doesn't morph** — breaks at 32 pages.
- **Asset drawer drifting into a store** — infinite scroll, trending, per-tile badges, ratings.
- **Enriching chrome by mistake** — direct all colour enrichment to `content.*`.
- **Bounce on the page-settle / non-pixel-identical return** — breaks the rigid-body illusion, reads as
  "it moved my work."
- **Haptic overload / loud autosave / independent keyboard tween** — all read as gimmicky or unfinished.
- **Over-removing friction into blandness** — remove *operational* friction only; keep the interesting-hard.
- **Clip-art dominance** — keep maker content top-ranked; art is garnish.
- **Freezing before the studio surface is in the spec** — see §12.

---

## 12. Final recommendation: freeze, or iterate?

**Recommendation: ONE more focused iteration, then freeze.** Not a redesign — the direction and the proven
core interaction hold. The iteration folds the four high-impact studio additions (§3: **H1 tray · H2 morphing
1→32 nav · H3 one Art drawer · H4 maker ink palette**) plus the §9 motion/snap refinements into the
prototype, so the *frozen* spec is a studio, not just an editor. The prototype defects (RF-1…RF-5) are
already fixed; the adversarial verdict on the current build was **GO WITH FIXES**, and with fixes applied it
is a clean baseline to iterate *from* — but freezing it as-is would lock in an 8-page-only, tray-less,
expressively-thin Bench that the owner's own brief asks us to surpass.

Two of the four (H3 Art drawer, H4 maker palette) also touch governed decisions — the **asset ADR** and the
**colour-namespace ADR** — which need a review + legal pass before code. So the honest sequencing is: **iterate
the prototype (H1–H4 + §9) → owner critique → freeze the HTML → then the two ADRs → then Compose.**

Because H1–H4 are sizeable and two are partly the owner's product calls (how big a maker palette? tray on by
default?), the iteration scope should be **owner-approved**, not assumed. That question is put next.

*Phase 9 of the Bench initiative. Review only — the RF fixes are applied; no new design frozen. Awaiting the
owner's scope call on the pre-freeze iteration.*

---

## Part E — the "handmade studio" physicality gate (Phase 10.5) → **DESIGN FREEZE**

> **The owner's final question before freeze:** the Studio architecture is correct and must be preserved —
> but *"does it feel like a **little handmade studio**, or still like a beautifully designed application?"*
> Method: one more independent research pass, this time on **physical** creative spaces (stationery shops,
> letterpress/riso/printmaking studios, artists' desks, junk journals, archive drawers, index-card
> catalogues, collections) — extracting **principles, not aesthetics** — then a physicality audit of the
> prototype and a defended freeze-or-iterate call. Brief constraint: *do not chase novelty, add features, or
> increase complexity.*

### E.1 — What physical making actually teaches (principles, not decoration)

Six cited findings ([full research in the session record]; labelled per [research standards](../../CLAUDE.md)):

- ✅ **Permission before capability.** A good studio *invites* before you touch anything — the paper is out,
  the tools are within reach, nothing is locked behind a setup. Warmth is felt at rest, not on first action.
- ✅ **The single highest-leverage move is the opening.** Open onto *the user's own page*, resting like paper
  on a warm surface, exactly where they left it — and make that page the loudest thing in the room. Everything
  else is secondary.
- ⚠️ **Warmth lives in surface properties, never in added objects.** Honest translations: warm off-white
  (never `#FFF`), soft *contact* shadows, discrete card edges with faint stacking depth, weighty unhurried
  motion, an ink-warm palette. **Kitsch traps to refuse:** deckle/torn-paper edges, coffee-ring stains,
  faux-handwriting chrome fonts, random tilt/jitter, visible paper grain. These *cost* trust — they read as a
  costume, not a craft.
- ✅ **Cards and drawers beat lists and grids.** Physical navigation is little sheets you riffle, with real
  edges and peeking neighbours — *never a contact sheet.* A grid overview "turns a desk into a database": it
  relocates the answer from *"here is your page"* to *"here is your inventory."*
- ✅ **A tray is arranged by reach and use, not stored.** It sits comfortably partly-empty, orders by recency,
  accepts a thing in one gesture, and **never renders an empty slot as something missing** (no dashed
  placeholders).
- ✅ **Human, gesture-based naming.** *shelf, tray, bits, spread, set it down, print it* — not
  *asset / library / manage / import*.

### E.2 — Physicality audit of the prototype (does this feel like paper or software?)

| Surface | Read before | Verdict | Action taken (all within the frozen architecture) |
|---|---|---|---|
| **The opening** | Opens on a made page (*"Mum's garden"*), warm desk ground, contact shadow, page is loudest | ✅ **Paper** | Kept. Real **persistence** (reopen exactly where you left off) is the load-bearing signal — a Compose-build guarantee, recorded in E.4, not showable in a static proto. |
| **Page navigation** | Filmstrip *pips* — rounded rects with three lines; read as slider controls | ❌ **Software** — the gap the owner named | **Rebuilt as little paper sheets**: real edges, a spine, faint text lines, soft contact shadow; the current page lifts and scales like *the one in your hand*, neighbours peek. Slider → riffled cards. |
| **Scale overview (⊞)** | Full-screen page-grid | ⚠️ near the "database" line | **Kept but justified**: cells are paper sheets (cover/back labelled), and it is **summoned**, never the default. The default nav stays sheets-in-a-row, so charm and 1→32 scalability coexist — exactly the owner's ask. |
| **The shelf/tray** | Uniform 44px rounded tiles + a **dashed** "＋" slot | ❌ **Software** — retail rack + empty-slot-as-missing | **Retuned to a maker's tray**: warm paper bits with contact shadow, set down by hand (slight organic offsets, not a matrix); the dashed slot became a soft **"＋ keep"** action (an action, not a missing thing). |
| **Copy** | *"Would add from your photos"*, *"Gather from your photos"* | ⚠️ software register | Human/gesture pass: *"Pick a photo to keep on your shelf"*, *"Keep a photo on your shelf"*. (Destructive verbs like **Delete** kept literal — clarity and a11y outrank cuteness; [BP-6](V2-BENCH-PRINCIPLES.md).) |
| **Material honesty** | Warm paper (`#F7F2E7`, not white), soft contact shadows, settle-easing, subtle grain; **no** deckle/coffee/handwriting/tilt | ✅ **Paper** | Kept. The new sheets and tray bits inherit the one shadow language and the one settle motion — no new material vocabulary introduced. |

### E.3 — The one iteration, and why it was justified (not novelty)

Two audit rows came back **software, not paper** — and both were exactly what the owner felt: the page-nav
"lost the feeling of tiny paper sheets," and the shelf read as storage. The fixes are **texture/material
only** — no architecture touched, no feature added, no new interaction. They change *how the same components
feel*, which is precisely the brief's permitted move (*"preserve this architecture… do not add features"*).
This is why one pass was warranted rather than freezing over a known gap the owner had already named.

Crucially, the research also **defended a decision I might have over-corrected**: it warned that a grid
overview turns the desk into a database — validating that the page-grid stays *summoned and sheet-shaped*, and
that the default must remain riffled cards. And it named the twee line precisely (peeking neighbours + any
visible grain sit closest to it), so the sheets carry edges and depth but **no grain, no tilt, no torn
edges** — warmth from surface properties, not costume.

### E.4 — The one thing the prototype cannot show, promoted to a build invariant

🟦 **RECOMMENDATION (freeze-blocking for the Compose build, not the HTML):** the strongest handmade signal is
**persistence of place** — reopening the Bench lands on the user's page, at the same page number, materials
still on the shelf, exactly as left. A static prototype can only *depict* this. It becomes a **verified
acceptance criterion** for the Compose implementation (and folds naturally into the existing
`AutosaveCoordinator` + session-restore path; see [BC§1](V2-BENCH-CRITIQUE.md), and the open undo-survives-kill
question in [EP-4](V2-BENCH-PRINCIPLES.md)). Recorded here so freeze does not silently drop it.

### E.5 — Freeze decision

**Recommendation: DESIGN FREEZE the Bench**, after this final texture pass.

Evidence it has reached the bar:
1. **Every audit row now reads *paper*** or is a justified, bounded exception (the summoned grid).
2. **The two named gaps are closed** with the owner's own constraint honoured — no architecture change, no new
   feature, no added complexity. The diff is surface properties and six words of copy.
3. **The kitsch line was found by research and deliberately not crossed** — the warmth is structural (tone,
   shadow, weight, edge, motion), which is the durable kind, not the costume kind that erodes trust.
4. **The remaining unshowable signal (persistence) is captured as a build invariant** (E.4), so it cannot be
   lost between freeze and Compose.
5. Continuing past here would mean *adding* — the exact move the brief forbids, and the move [BP-1 / the
   subtraction test](V2-BENCH-PRINCIPLES.md) exists to refuse.

**Benchmark answer** (*remembered for efficiency, or as a wonderful place to create?*): the page is the hero
and opens as your own paper; you riffle little sheets, not a slider; you set bits down on a tray, not into
storage; motion settles like paper and never bounces; nothing is a costume. That is a place, not a tool.

**Freeze scope.** Frozen: [`mockups/v2-bench.html`](mockups/v2-bench.html) as the canonical Bench spec, with
[principles](V2-BENCH-PRINCIPLES.md) BP/EP and [IA & interaction](V2-BENCH-IA-INTERACTION.md) as its written
authority. Per [CLAUDE.md](../../CLAUDE.md), post-freeze allows only bug/a11y/perf/parity/theme fixes; any UX
change updates the HTML spec first. **Two items stay governed and do not freeze into implementation until they
clear a review + legal pass:** the [asset layer ADR](V2-BENCH-IA-INTERACTION.md) (H3, online-search licensing)
and the colour-namespace ADR (H4, `content.*` maker inks). Compose implementation may begin on the frozen
core; those two land behind their ADRs.

*Phase 10.5 of the Bench initiative. The physicality iteration is applied to the canonical prototype and
republished to the same artifact URL; JS re-validated. Recommendation to the owner: **FREEZE**.*

### E.6 — 🔒 DESIGN FREEZE (owner-approved 2026-07-28)

The owner reviewed the republished prototype and approved the freeze.
**[`mockups/v2-bench.html`](mockups/v2-bench.html) is the frozen, canonical Bench specification**, with
[V2-BENCH-PRINCIPLES.md](V2-BENCH-PRINCIPLES.md) (BP-1..7 / EP-1..5) and
[V2-BENCH-IA-INTERACTION.md](V2-BENCH-IA-INTERACTION.md) as its written authority.

- **Post-freeze rule** ([CLAUDE.md](../../CLAUDE.md)): only bug / accessibility / performance / implementation-
  parity / theme fixes are allowed. Any UX change updates the **HTML spec first**, never Compose first.
- **Compose may begin** on the frozen core.
- **Carried into the build as invariants:** persistence-of-place (§E.4); in-place caret + rigid whole-page
  pan for text editing, conditioned on device Pass-2 proving pixel-identical return + small-text editability
  ([EP-1](V2-BENCH-PRINCIPLES.md)), fallback = harden the bottom sheet.
- **Still governed — do NOT freeze into implementation until a review + legal pass clears them:** the asset-
  layer ADR (H3 online-search licensing, CC0/MIT-first) and the colour-namespace ADR (H4 `content.*` maker
  inks). These land behind their ADRs; the rest of the Bench does not wait on them.

*Bench initiative Phases 1–10.5 complete. **The Bench is design-frozen.***
