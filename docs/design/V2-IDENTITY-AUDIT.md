# V2 — Living Product Audit (identity evidence)

> **Status:** evidence for the implementation-readiness call. Companion to
> [V2-IDENTITY.md](V2-IDENTITY.md) — that doc holds the *direction*; this one holds the *proof*.
> **Reopens no frozen surface.** Library, Bench and Proof stay frozen; everything here is content-system only.
> **Board:** [`mockups/v2-living-audit.html`](mockups/v2-living-audit.html) — the interactive evidence.

The direction was approved on one sentence:

> **"The interface stays quiet. The creations carry the warmth."**

The owner's charge was to stop explaining that and start proving it — *less theory, more believable product
examples*. This document is the audit that answers it, section by section, and ends with a readiness recommendation
backed by evidence rather than assertion.

---

## 1. Product identity assessment

**Verdict: the identity is correct and now demonstrable.** The load-bearing decision — separating *interface
identity* (restrained) from *creative identity* (expressive) — is not a style choice; it is an architecture. It
means the app can stay calm for a decade while every user's shelf becomes more theirs. That is the rare identity
that *improves with use* instead of dating.

The Handmade Test the owner posed — *"nice UI"* (Person A) vs *"a quiet café making tiny books"* (Person B) — is
won not by the chrome but by what the chrome frames. The chrome's job is to disappear so the creations read as
objects. The evidence board is built to make Person B's sentence the obvious one: you look at it and see a shelf
of little books, not a screen.

What changed since the direction doc: we can now point at a **generated collection** instead of a described one.
The recipe is real, the inks are the frozen Bench H4 set (verbatim, no invention — see
[V2-IDENTITY §4](V2-IDENTITY.md#4-the-maker-ink--content-colour-model--where-the-cafés-colour-lives)), and the
output survives scrutiny at fifty books.

---

## 2. Living product demonstration

The board renders four realistic states from **one recipe**, so the identity is judged as a product, not as
isolated studies:

| Scenario | What it proves | Board section |
|---|---|---|
| **A brand-new shelf (3 books)** | The identity is present from the first zine — no "empty" or "generic" state. | §03 · Brand new |
| **A few months in (9 books)** | The collection *accretes* — new books added to the same sequence, never regenerated. | §03 · A few months |
| **Heavy use (24 books)** | Density doesn't homogenise; the shelf gains character, not noise. | §03 · Heavy use |
| **Fifty zines / six months** | The stress test: still one Zinely shelf, still no two identical. | §04 · Fifty zines |

The **accretion property** matters and is deliberately built in: the timeline shows the *first N of one fixed
sequence*, so "a few months" literally contains "brand new". A real collection grows; it doesn't reshuffle itself
every time you add a book. That single mechanic is what makes the shelf feel *owned* rather than *generated*.

---

## 3. Maker's Cover review

This is, as the owner suspected, becoming a defining idea. The review is organised around the five properties
requested.

- **Variation** — a cover is assembled from five+ independent variables on a frozen grid:
  `{ title × ink × second ink × paper × motif × stamp × layout }`. That yields **96,000 distinct configurations
  before the title is even counted** (5 papers × 10 inks × 10 second-inks × 8 motifs × 8 stamps × 3 layouts), and
  **1,344,000** once the 14-plus titles are included. (Numbers recomputed and asserted live on the board, §04.)
- **Recognisability** — variation never escapes the family because the *grammar is frozen*: the 3:4 grid, the
  Fraunces/Inter pairing, muted riso inks on warm paper, the spine, and a fixed stamp vocabulary. Change the
  vocabulary, keep the grammar — the Penguin/Marber move. See §5 (constants vs variables) below.
- **Longevity** — the birthday-collision maths says the chance any two of fifty books share a configuration is
  **under 1.3%**. Repetition isn't avoided by luck or by a "seen recently" filter; it's avoided *by construction*.
  A maker would have to make hundreds of zines before the recipe visibly repeats.
- **Collectability** — because covers are siblings, a *row* reads as a set, not a list. That is what turns "my
  files" into "my collection" — the same instinct that makes people line up Penguin spines. Optional future lever:
  ink-as-category (colour-coded "kinds"), noted in [V2-IDENTITY §4](V2-IDENTITY.md) — not required now.
- **Consistency** — three **layout archetypes** (classic · centered · banded masthead) are shown. They widen the
  visual range without breaking recognisability, because each is the *same grid* resolved differently. **Recommendation:
  freeze these three as the recipe's archetype set** (an H4/cover-recipe ADR item), so implementation reproduces a
  bounded, known space rather than an open-ended one.

Honours [ADR-069](../DECISIONS.md#adr-069) — the cover is recipe-driven; there is no per-edit render, and the frozen Library's
ShelfCover recipe is extended, not replaced.

---

## 4. Material system review

The material direction holds, and the board makes its governing rule visible rather than merely stated:

- **Paper stocks** — cream (café default), kraft, recycled fleck, dot-grid, cover-weight (the object cue). Each
  carries a caption naming *why* its texture exists.
- **Texture as consequence, never overlay** — the guardrail is now explicit and testable: **grain** = riso drum +
  paper tooth; **misregistration** = two drums in two passes; **fleck** = recycled pulp; **dot-grid** = printed
  register. *If a texture cannot name its physical cause, it does not ship.* That single sentence is the line
  between craft and kitsch, and it governs the whole content system — not just covers.
- **Restraint** — textures are low-opacity, soft-light/multiply blends tied to a material. There is no global
  "riso filter" dropped over everything (the kitsch tell called out in the craft research); each layer earns its
  own offset and grain.

No change to the material direction is recommended — only that the guardrail be carried into the asset-authoring
ADR (H3) as an acceptance criterion, so it survives contact with real assets.

---

## 5. Long-term collection review

The owner's scenario — *fifty zines over six months; does it still feel fresh?* — is answered directly on the board
(§04) and holds up:

- **Does the system create variety naturally?** Yes — 96k configurations mean variety is the default, not an effort.
- **Does it reward collecting?** Yes — sibling covers make a row legible as a set; the collection is more than its
  parts.
- **Does it avoid visual repetition?** Yes — <1.3% collision across fifty, by construction.
- **Would people recognise a shelf of Zinely books?** Yes — the frozen grammar (grid, type, muted inks, warm paper,
  spine, stamp) is the shared DNA every cover carries.

**The tension the owner named — variation vs recognisability — is resolved by deciding which is which.** Zinely
freezes the *grammar* and frees the *vocabulary*. A template does the opposite (frozen vocabulary, no real freedom)
and that is exactly why templated apps feel repetitive. The constants/variables split (board §05) is the whole
argument in one frame.

---

## 6. Colour system review

Reviewed in context, per the instruction to *not* increase interface colour:

- **The interface spends one accent** — matcha, for the single action and selection. Ink for text, paper/desk for
  ground, strawberry reserved for consequence. That is the entire chrome budget, and it should stay that way.
- **The creations spend the whole box** — 10 maker inks × 5 papers × motifs × covers. This is where the café's
  colour lives.
- **Is the creative system using enough of it?** Yes — covers deploy up to two inks each, papers add warmth, motifs and
  stamps add a third accent, and the starter pack seeds dividers/templates. The warmth budget is being spent in
  content, exactly where the thesis wants it.
- **If more warmth is ever wanted**, the board demonstrates it comes from *content* — a warmer paper default, a
  richer starter pack, more motif range — never from adding colour to chrome. Recommendation: **hold the chrome at
  one accent**; treat any "it feels cold" feedback as a *content* dial, not a *chrome* dial.

---

## 7. Final implementation recommendation

**Begin Compose implementation.** The identity has crossed from principle to evidence: one recipe demonstrably
produces a *collection* — fifty books that are individually distinct and collectively, unmistakably Zinely —
recognisable, varied, and slow to repeat by construction. The interface stays quiet in every frame; the warmth is
entirely in the creations, exactly as the approved thesis requires.

What remains is **governance, not design**:

1. **Token-role proposal** ([V2-IDENTITY.md](V2-IDENTITY.md) §1, "Token refinement") — role-renaming is zero-visual-change and safe;
   the `--confirm` tone and `--brand`→content moves need the owner's nod plus a pixel-parity re-baseline.
2. **Cover archetype set** — record the three layout archetypes as the recipe's frozen set (H4/cover-recipe ADR).
3. **Asset ADRs (H3 asset system, H4 content colour)** — owe legal sign-off before assets are bundled; the
   texture guardrail (§4) becomes an acceptance criterion in H3.

None of these is a reason to iterate the identity further. **The content system is mature. Implement the recipe;
don't redesign it.** One final identity refinement is *not* justified — the remaining work is decisions and code,
which is where a fourth refinement pass would only add theory back on top of proof.

---

### Change log
- **2026-07-28** — Living Product Audit created (evidence board + 7-part assessment). Recommendation: proceed to
  Compose; remaining work is governance (token proposal · archetype ADR · H3/H4 asset ADRs with legal sign-off).
