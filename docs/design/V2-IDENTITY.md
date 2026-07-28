# V2-IDENTITY.md — Product Identity direction: the café's body

> **Status:** Product-Identity refinement pass, after the [Product Director review](#) that froze
> [Library](mockups/v2-library.html) + [Bench](mockups/v2-bench.html) + [Proof](mockups/v2-proof.html) and asked
> one question: *does Zinely feel like one handcrafted product, or three beautiful screens?* This is **content,
> material, and token direction — NOT a screen redesign.** No frozen surface is reopened; the interface stays
> quiet by design. Built on two cited research streams (RA-1 craft/materials, RA-2 starter-pack/licensing;
> labelled findings below) and the frozen [V2 tokens](V2-TOKENS.md) + [principles](V2-PRINCIPLES.md). It feeds a
> materials/texture/cover **HTML study** ([mockups/v2-materials.html](mockups/v2-materials.html)) and, where it
> hardens, reviewed ADRs (H3 asset system, H4 content-colour) that still owe **legal sign-off** before
> implementation.

---

## 0. The thesis (what the Product Director review found)

Measured across the three frozen surfaces, **matcha outnumbers every other accent combined by 2.5–6×** in
chrome (Library 6:1, Bench 28:10, Proof 26:12); the café's warm ochre/teal barely touch the interface. That is
not a bug in the chrome — the chrome is *correctly* quiet. It is evidence of where the café actually lives:

> **The café is delivered by the user's creations, not by the interface. The interface is right to be quiet;
> the warmth, colour, and personality belong to the covers, papers, inks, and marks the maker makes — and that
> creative-content layer is the least-directed part of the product.**

Today a user would more likely say *"nice, calm editor"* than *"a place to make little books,"* because the part
they touch — what they *make* — is still schematic. This document directs that layer. Everything here serves one
test: **does it make the maker's creation feel handmade, without adding interface chrome or complexity?**

Research validates the strategy directly: quiet chrome exists so colourful content can sing, and an accent's
power *is* its scarcity — overusing one hue flattens the palette (RA-1 §4; 60-30-10). So the answer is not "add
colour to the UI." It is "give the content a body."

---

## 1. Token refinement — relieve matcha's four jobs *(proposal; owner sign-off + parity re-baseline required)*

**This is the one item that could touch frozen surfaces, so it is a proposal, not an applied change.** Matcha
currently carries **four semantic roles at once** — primary action, selection/"you-are-here", success/ready, and
brand/identity mark — which is why the interface reads near-monochrome-green.

**Proposed refinement (three moves, smallest first):**

1. **Name colours by role, not hue (zero visual change).** Introduce semantic tokens — `--action`,
   `--selection`, `--confirm`, `--brand` — mapped *today* to their current matcha values. Nothing moves a pixel;
   but green's overload becomes legible in the token file, and any future tune can split a role without hunting
   hardcoded `--matcha` across components. This is pure hygiene and is safe to adopt now.
2. **Give `--confirm`/ready a *settled* tone distinct from `--action` (small visual change, parity re-baseline).**
   A success tick and a primary "go" button should not shout with identical weight. Keep `--action` solid
   matcha ("go"); let `--confirm` read as *settled* — same hue family, lower chroma / a tint-and-text treatment —
   so *done* looks different from *do*. This is a token tune applied uniformly, not a screen redesign; it needs a
   golden re-baseline (allowed post-freeze as a theme/parity change) and your nod.
3. **Move `--brand` off the chrome and into the content.** The identity mark (the Library cover stamp) is a
   *made object* — its colour belongs to the **maker-ink** palette (§4), not the interface accent. Letting the
   stamp be strawberry/ochre/any ink both relieves matcha *and* puts a supporting colour exactly where research
   says it belongs (RA-1 §4.4). This is a **cover-system** decision (§5), executed there, not a chrome edit.

**Net:** the interface stays quiet and green-led for *action*, stops using green for *everything*, and hands the
café's warm colours to the content where they carry meaning. **No frozen screen is edited by this document;**
moves 2–3 are logged for the parity/ADR stage with your approval.

---

## 2. The riso craft signature — print the process, not a filter

Riso is Zinely's material fingerprint. Done right it is the difference between "a printed PDF" and "a little
riso zine." Done wrong it is the single biggest kitsch risk. The rules, from RA-1:

- **Layered translucent spot ink, physically overprinting (RA-1 §1.1).** The soul of riso is *separate*
  semi-transparent ink layers that overlap into secondary hues — not a one-shot duotone filter. Zinely models
  ink as layers; overlaps blend.
- **Independence per layer is the anti-kitsch rule (RA-1 §1.3, §5.3).** Each ink layer gets its *own* slight
  misregistration and grain. A single global "riso filter" over a merged image is the kitsch tell. This is the
  line between craft and costume.
- **Keep the soul, drop the damage (RA-1 §1.2).** Reproduce **misregistration-offset** (≤~2–3 mm) and
  **grain/halftone** (loved, "printed by a machine with a body"). **Never** simulate smudges, roller marks, or
  streaks — those read as *broken*, not charming.
- **Home-printer-honest inks (RA-1 §1.4).** Grain, halftone, offset, and overprint blends survive any inkjet/
  laser because they are baked into the artwork. True fluorescent riso inks do **not** — so Zinely's inks stay
  **muted and achievable** (its matcha/strawberry/ochre/teal already are). We never promise neon.
- **The twee line (RA-1 §5.3, the governing guardrail):** texture reads *warm* when it is a believable
  **consequence of a process** (ink soaking paper, layers drifting, a heavier cover) and *kitsch* when
  imperfection is **pasted on as uniform decoration**. Restraint + physical plausibility is the whole line. This
  is the same guardrail the Bench froze (no deckle/coffee-ring/handwriting/random-tilt/visible-grain-as-costume).

---

## 3. The paper-stock system — material choice as the first craft cue

A single repeated "cream paper + grain" is calm; a small set of *chosen* stocks is handmade (RA-1 §2). Direction:

- **A short, named stock set — ~5 (RA-1 §2.4):** **Cream** (the warm café-paper default) · **Kraft** (earthy
  brown) · **Recycled fleck** (visible specks, "cared-for", archival, RA-1 §2.3) · **Dot-grid / ruled** (the
  notebook register) · and a distinct **heavier Cover stock role**.
- **The cover is heavier than the pages (RA-1 §2.2, §2.6).** Model **cover** and **interior** as distinct stock
  *roles*, not one uniform page — the weight difference is the single strongest "this is a real little object"
  cue.
- **Evoke paper with grain + fibre fleck + warm tone, never embossing (RA-1 §2.5, §5.1-5.2).** Low-opacity grain
  and fleck (Zinely's existing `--grain` texture is the seed), not literal 3D bumps.
- **Authored as tintable coverage (RA-2 §4).** Paper textures ship as **grayscale, tileable single-channel
  masks**, tinted by the paper's tone at render — so a stock is a *material*, not a fixed picture, and stays
  print-crisp at 300 dpi from a tiny tile.

---

## 4. The maker-ink / content-colour model — where the café's colour lives

The café's colours are **inks and materials of the creations**, held in a content namespace separate from chrome
(the Bench's frozen `content.*` H4 model). Direction, from RA-2:

- **The 10 named riso spot inks the Bench already froze** are the source of truth — verbatim, no invention:
  Matcha `#7C8A3F`, Forest `#3E5E3A`, Strawberry `#E27F89`, Brick `#B0503F`, Sunflower `#E7B53C`,
  Ochre `#D19A3C`, Aqua `#57B0A9`, Cornflower `#6E86C9`, Plum `#8A5A9B`, Ink `#2A251E` (v2-bench.html H4).
  Grouped for study into four bands — greens & earth · warm · cool · neutral — plus the Bench's presets
  (Two-colour, Warm zine, Cool zine). The materials study renders this exact set; any future widening is a
  Bench-H4 decision, not an Identity one. Note the maker palette is deliberately **broader and cooler** than the
  brand's warm chrome: the interface stays warm, the maker gets range.
- **Author coverage, not colour (RA-2 §4.2 — load-bearing).** Every tintable asset is **geometry + single-
  channel coverage**; the *ink* is resolved from the maker's palette at render. **One asset × N inks = N looks**,
  and re-theming a zine re-inks every asset for free.
- **Duotone = two tintable singles (RA-2 §4.2).** Anything wanting two inks is a **registered pair**, composited
  with paper show-through at render — exactly the two-drum riso workflow, and the honest way to get overprint
  blends.
- **Colour as role/category is available but optional (RA-1 §3.3, the Penguin move):** ink could encode a zine
  "kind" the way Penguin colour-coded genres — a collectability lever for later, not a requirement now.

---

## 5. The Maker's Cover recipe — a system that yields a collection

Collectability = **a recognisable system + per-object individuality** (RA-1 §3.1, §3.10). The canonical model is
the **Penguin/Marber grid**: one fixed layout skeleton, infinite instances (RA-1 §3.2). This extends the
Library's frozen **Maker's Cover** (title + ink + stamp), honouring **[ADR-069](../DECISIONS.md)** (no per-edit
render — a cover is generated from a recipe, never a live page thumbnail).

**The recipe = a frozen grid × swappable ingredients (RA-1 §3.5):**

```
Maker's Cover = { title + subtitle }
              × { ink        — one muted maker ink (§4) }
              × { mark/stamp — a small made mark; carries the moved --brand (§1.3) }
              × { paper       — one stock (§3) }
              × { motif       — optional single tintable motif (§6) }
              × { layout zone — the fixed grid, à la Marber }
```

Freeze the grid; vary the ingredients. Every user cover then looks **unmistakably Zinely and unmistakably
theirs** — a row of them on the Shelf reads as a *collection*, not a list with nice styling (the Library's
collectability promise, currently only asserted). The mark/stamp is where the relieved `--brand` colour (§1.3)
naturally lands — a strawberry or ochre wax-stamp on cream reads warmer and more café than one more green.

---

## 6. The Starter Creative Pack — the first zine feels *made*, not configured

So the very first zine has personality in minutes, not a blank-canvas freeze (RA-2 §1). **Generous but scannable
— one glanceable row per category (~6–10), good defaults over a big library** (RA-2 §1.3, Hick/Miller). Proposed
manifest (all CC0/OFL, tintable per §4):

| Category | ~count | Format | Why |
|---|---|---|---|
| Starter templates/themes | 3–4 | composed docs | the one filtering choice; an instant "made" first zine |
| Papers / stocks | ~5 | grayscale tileable | §3 — material variety |
| Riso textures / grain overlays | 4–5 | grayscale coverage | the riso character, tinted by any ink |
| Stickers / motifs | 8–10 | single-path vector, tintable | the most-used "personal" layer |
| Dividers / rules / ornaments | 6–8 | vector, tintable; duotone pairs | structural polish, cheap as vectors |
| Cover elements (marks, badges, mastheads) | 4–6 | vector, tintable | makes the cover look "designed" in seconds |
| Display + text fonts | 2 + 1–2 | subsetted OFL/Apache | already proven in-repo (ADR-070) |

**Everything vector by default; grayscale tileable raster only for textures** (RA-2 §5) — 300 dpi print quality
free, APK stays lean; heavier future assets move on-demand.

---

## 7. Asset philosophy — three layers, print-and-sell-safe *(governed; owes legal sign-off)*

Confirms and consolidates the Bench's governed asset architecture (H3), now with RA-2's licensing refresh:

- **Three layers:** (1) **Product Identity** — always bundled (the app's own marks/fonts); (2) **Starter
  Creative Pack** — bundled (§6); (3) **optional online asset library** — future, opt-in, **sends a keyword
  only, never user content**. Fully usable offline; user content never leaves the device.
- **CC0 / PD-first, merchandising-safe (RA-2 §2 — RA-2-confirmed; legal sign-off pending).** CC0 is the gold
  standard (zero conditions, zero notice in the export) and the **preferred licence for embedded art**. OFL and
  MIT are acceptable but **not attribution-free the way CC0 is** — both require their licence text to travel with
  the work; route those notices to an **in-app credits screen**, never into the exported zine. (OFL additionally
  forbids selling the font file alone — fine here, we embed glyphs, we don't resell fonts.) **Disqualified:**
  CC-BY-SA (viral copyleft would infect the user's own sellable zine), CC-NC, CC-ND, and merchandising-restricted
  providers (Blush, unDraw, Streamline, Storyset). The rule that makes it sell-safe: **the exported zine carries
  no attribution** — so anything with a travelling notice (OFL/MIT/Apache) is credited in-app only, and embedded
  art is CC0/PD by preference.
- **Own the riso *look*; don't buy riso *packs* (RA-2 §3.5 — the one refinement).** There is no reliable CC0
  supply of ready-made riso art; Zinely gets the look by **tinting CC0 line-art through the maker inks** (§2, §4)
  — legally cleaner and infinitely re-inkable.
- **Verify per asset, not per source (RA-2 §3.6).** Aggregators mix licences; every bundled asset is individually
  verified CC0/PD/OFL and its licence page captured at ingest.
- **Governance:** the H3 asset-system ADR and the H4 content-colour ADR remain governed and **owe human/legal
  sign-off before implementation**. This document is *direction*; those ADRs are the *decisions*.

---

## 8. Close the loop — the book comes home

The Product Director review found the journey's one emotional seam: **Fold → Shelf returns *quietly*** because
the "book comes home" settle is V2-deferred and the finished-book *reveal* is Read's (which is not among the
three frozen surfaces; [ADR-058](../DECISIONS.md#adr-058) boundary). Direction (motion + cover-system, **not** a
new screen):

- When a made zine returns to the Shelf, its **Maker's Cover arrives** with the shared **paper-settle** signature
  (the one motion the Bench/Proof already use) — the collection visibly gains a member. This is the goal-gradient
  payoff (Proof PR§C9) landing where it belongs, on the Shelf.
- This is a **direction for the eventual Read/Shelf-return beat**, flagged so it is built deliberately, not a
  request to reopen the frozen Library. It pairs naturally with the cover system (§5): the thing that "comes
  home" is a distinct little object, so the arrival *reads*.

---

## 9. What this is NOT

- **Not a screen redesign.** Library, Bench, Proof stay frozen. The interface stays quiet.
- **Not new interface colour.** The café's colour goes into *content* (inks, covers, papers, motifs), not chrome.
- **Not skeuomorphism.** Warmth via grain, fleck, layered ink, and micro-motion — texture as *consequence of a
  process*, never a pasted costume (the twee line, §2).
- **Not a feature sprawl.** A short stock set, a bounded starter pack, one cover recipe — good defaults over big
  libraries.

---

## 10. Governance & next steps

| Item | Status | Gate |
|---|---|---|
| Token role-naming (`--action/--selection/--confirm/--brand`, mapped to current values) | Proposal | Owner nod; zero visual change — safe to adopt |
| `--confirm` settled tone; `--brand` → content | Proposal | Owner approval + golden parity re-baseline |
| Riso craft signature spec | Direction | Hardens into the H4/material ADR |
| Paper-stock set + cover/interior roles | Direction | Hardens into the material ADR |
| Maker's Cover recipe (grid × ingredients) | Direction | Extends the frozen Library cover; ADR + parity |
| Maker-ink coverage-authoring model | Direction | H4 ADR |
| Starter Creative Pack manifest | Direction | H3 ADR + **legal sign-off**, per-asset verification |
| Three-layer asset philosophy + licensing | Confirmed ruling | H3 ADR + **legal sign-off** |
| Book-comes-home loop-close | Direction | Built with the Read/Shelf-return beat; not a Library reopen |

**Companion:** the [materials/texture/cover HTML study](mockups/v2-materials.html) makes this visible — paper
stocks, riso ink-layering, the tintable coverage model, and the cover recipe producing a collection — so the
direction can be *seen* and signed off before Compose.

*Product-Identity direction. No code changed, no frozen screen reopened, no token file edited. Research-backed
(RA-1/RA-2, cited); the governed items owe reviewed ADRs + legal sign-off before implementation. This direction
will be checked by an independent Review Agent alongside the HTML study before it is treated as settled.*
