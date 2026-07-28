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
