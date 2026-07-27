# V2-TOKENS.md — the locked V2 colour palette

> **Status:** The **owner-approved** V2 colour identity (locked 2026-07-27), derived from the
> matcha/strawberry/cream reference per [V2-DIRECTION.md](V2-DIRECTION.md) Q-A (full re-derive) and the
> discipline in [V2-RESEARCH.md §3](V2-RESEARCH.md). This is the single source the HTML prototypes and the
> Compose implementation derive from. It becomes a **governed colour ADR** when it lands in code (the
> reopening of the colour rules the corpus ruling Q-B permits); the values here are the design-frozen
> reference that ADR will carry. Type stays **Fraunces (voice) + Inter (work)** — unchanged.
>
> **Derivation, not copy.** Four rulings taken from the reference's *feeling* — warm temperature · muted
> (not desaturated) ceiling · soft surfaces + crisp ink · the paper metaphor — then the image was set
> aside. Authored in OKLCH for even steps across hues; the AA-critical pairings (marked ★) are gated in CI
> at build, not eyeballed.

## Semantic roles — light (day room) / dark (warm night desk)

| Role | Light | Dark | Job | AA-critical |
|---|---|---|---|---|
| `paper` | `#F7F2E7` | `#2F2A22` | the artifact — the sheet you make on | — |
| `paperEdge` | `#EEE6D4` | `#39322A` | the sheet's edge/hairline | — |
| `desk` / room | `#ECE3D1` | `#201D18` | the table the paper sits on (warm; charcoal at night, never blue-black) | — |
| `deskEdge` | `#E1D6BF` | `#2A261F` | room dividers | — |
| `ink` | `#2A251E` | `#ECE4D3` | body & headings on paper | ★ on `paper` |
| `inkSoft` | `#5B5347` | `#B4AB97` | secondary text, captions | ★ on `paper` |
| `inkFaint` | `#8C8269` | `#857C69` | faint/decorative only — **not** for body text | — |
| `matcha` (primary fill) | `#5E6B2F` | `#93A257` | the one primary — "your next move" (Start a zine) | ★ w/ its on-text |
| `matchaText` | `#4C5826` | `#B7C47C` | matcha as icon/text/selected-state | ★ on `paper` |
| `matchaTint` | `#DCE3C0` | `#363826` | soft selected surface, behind dark ink | — |
| `strawberry` (punctuation) | `#E98F97` | `#D98289` | a stamp, a current-page dot — accents, never actions | — |
| `strawberryText` | `#A6474F` | `#E8A6AB` | deep strawberry when it must carry text | ★ on `paper` |
| `strawberryTint` | `#F6DAD3` | `#3C2C2A` | a soft blush surface | — |
| `consequence` | `#A6382A` | `#E0857A` | delete / real error — **kept distinct from strawberry** so a warning never reads as fruit | ★ on `paper` |

### Cover inks (the *maker's* palette, not the chrome)
Reserved for a maker-chosen zine cover — richer cuts that live on the artifact, never in the interface:
`matcha #7C8A3F` · `strawberry #E27F89` · `ochre #D19A3C` · `teal #47857B`. (Answers "which zine is mine?"
— the C1 signature move — without reviving the deleted per-edit render pipeline; see [ADR-069](../DECISIONS.md#adr-069).)

## Rules that travel with these values
- **Two brand hues + one consequence colour are the whole chrome palette.** Matcha = the single "your
  move"; strawberry = sparing punctuation; consequence = the only urgent colour. No fourth chrome hue.
- **Softness in surfaces/decoration; crisp ink for text.** Calm is not low-contrast-everywhere — the ★
  pairings must clear WCAG AA (body ≥ 4.5:1) in CI.
- **Dark is re-derived, not inverted** — a warm charcoal room, accents re-tuned to hold on it, the same
  paper grain expressed on charcoal.
- **Dynamic (wallpaper) colour stays off** — brand hues on identity-critical controls (unchanged from the
  privacy/identity stance).

## Cross-references
[V2-DIRECTION.md](V2-DIRECTION.md) · [V2-PRINCIPLES.md](V2-PRINCIPLES.md) (Principle 3) ·
[V2-RESEARCH.md](V2-RESEARCH.md) §3 · [ZINELY-DESIGN-SYSTEM.md](../ZINELY-DESIGN-SYSTEM.md) §7 (the colour
job-map these roles evolve).

*Locked 2026-07-27 on owner approval. Design-frozen reference; becomes a governed colour ADR at
implementation.*
