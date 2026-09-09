# Art panel research — 2026-08-25

Status: owner-accepted scope input for D-080 / ADR-107 on 2026-08-25. This is not the visual design freeze;
production outlines and Compose admission remain gated on the rendered A15 review.

## Question

What should Zinely keep and add in Art before the panel is finalized, and should randomization create
enough glyphs or graphics?

## Repository facts

- The shipping cabinet has sixteen authored, tintable, fill-only vector supplies in four physical families:
  Tape & fixings, Stamps & marks, Cut paper, and Cut shapes.
- `SUPPLIES-SPEC.md` already contains a worked 16 → 51 curation backlog. Its admission test is physical:
  a candidate must attach, point, tear, or cut, and must not be reproducible with a verb the maker already has.
- The legacy `v2-materials.html` and `v2-living-audit.html` use seeded random draws to test a **cover recipe**:
  a frozen grid combines title, ink, paper, motif, stamp, and layout so a shelf reads as one collection.
  They do not specify random page composition or a random Art outline generator.
- The later supply ruling is explicit: no Surprise me, shuffle, auto-arrange, composition preset, or procedural
  outline variation. It prefers several authored tears to an unlimited generated tear.
- Art remains offline, content-addressable by stable supply id, and replayed through the shared preview/raster/PDF
  render-command path. Any future variation that changes printed geometry must therefore be deterministic and
  document-stable, not chosen again at render time.

## External product patterns

The useful baseline is consistent across established creative tools, even though their catalogues are much
larger than Zinely should become:

- Canva separates Elements from text, photos, and other media; its element library combines search with media
  or format filters, while selected elements retain direct move, resize, rotate, layer, and flip controls.
- Adobe Express places stickers and shapes under one Elements entry, supports browsing or keyword search, and
  organizes mobile stickers into recognizable groups such as arrows and marks, calls to action, and shapes.
- Procreate's Source Library has more than 150 named brush shapes and adds search once browsing alone no longer
  scales. Variation belongs to brush behaviour after a stable source shape is chosen.
- Cricut Design Space separates basic Shapes from its much larger Images library. The image panel offers search,
  bookmarks, popular filters, related sets, and direct insertion; advanced filters are reserved for the large
  catalogue rather than crowding the first view.
- FigJam stays deliberately lightweight: drawing, shapes, stamps, emojis, and stickers support expression without
  turning the surface into a full design suite. Related stickers link back to their parent library.
- PicCollage treats text, stickers, photos/cutouts, and backgrounds as different material types. Its cutout flow is
  valuable evidence for a future personal-clipping tool, but it is not a reason to mix photos or backgrounds into
  Zinely's Art cabinet.

Sources:

- [Canva editing and designing](https://www.canva.com/help/editing-designing/)
- [Canva element filters](https://www.canva.com/learn/10-time-saving-canva-tricks-tips-hacks-smooth-workflow/)
- [Canva element transforms](https://www.canva.com/help/flip-and-rotate/)
- [Adobe Express stickers](https://helpx.adobe.com/express/web/add-images-and-visuals/stickers-and-qr-codes/stickers.html)
- [Adobe Express shapes](https://helpx.adobe.com/express/web/add-images-and-visuals/charts-tables-shapes/add-and-customize-shapes.html)
- [Procreate Source Library](https://help.procreate.com/procreate/handbook/5.0/brushes/brush-studio-settings)
- [Cricut image library](https://help.cricut.com/hc/en-us/articles/360009426074-Using-Images-in-Design-Space)
- [Introducing FigJam](https://www.figma.com/blog/introducing-figjam/)
- [PicCollage cutouts](https://support.piccollage.com/hc/en-us/articles/20523048318356-Guide-to-Magic-Cutouts-Magic-Lift)
- [Phosphor Icons licensing](https://phosphoricons.com/)
- [Openclipart licensing](https://openclipart.org/faq)

## Recommendation: keep the cabinet, grow the vocabulary

Do not copy Canva's breadth. Copy the interaction baseline: quick browsing, good names, search when the library
needs it, recent/favourite retrieval, and immediate placement with familiar transforms. Zinely's differentiation
is a coherent physical vocabulary, not catalogue size.

Keep in the panel:

1. The current sixteen. They establish every family and already have parity, accessibility, and device evidence.
2. Four visible family filters and local name/tag search once the set grows beyond the current one-screen cabinet.
3. Recent and favourites as shortcuts, not extra categories. A recent/favourite tile must still belong to exactly
   one physical family.
4. Stable names, useful synonyms, and truthful tile previews of the exact printed outline.
5. Current direct manipulation: place, move, scale, rotate, reorder, replace, recolour, delete, undo, and autosave.
6. A useful zero-result state that clears search and filtering without leaving the Art sheet.

Keep out of this panel:

- photos, personal cutouts, paper backgrounds, full-page templates, charts, QR codes, animated stickers, generic
  stock illustration, and online or AI-generated media;
- near-duplicate geometry the maker can already create by scale, rotation, or colour;
- generic interface symbols that read as app chrome rather than printed matter;
- controls for animation, effects, borders, blend modes, or other desktop-publishing complexity.

## Recommended admission order

Treat the existing ~51 list as the curated backlog, not as one launch batch. A first expansion to roughly 30–32
supplies is enough to make the cabinet materially richer while letting device and print evidence shape the rest.

High-value first additions:

- **Attach:** Masking tape, Saddle stitch, Eyelet, Push pin.
- **Point / print process:** Pointing hand, Crop marks, Colour bar, Copier streak, Perforation, Starburst.
- **Tear / cut paper:** Ticket stub, Postage stamp, Deckle edge, Torn hole, Folded corner.
- **Scissor geometry:** Ring (the strongest new shape because it cannot be made from the shipping filled circle).

Second wave after first-person use and printed examples:

- Bulldog clip, Safety pin, Rubber band, Thread tie, Wax seal, Punch hole;
- Chevron, Exclamation, Question mark, Check, Spiral;
- Banner, Luggage tag, Envelope, Speech bubble;
- Hexagon, Arc, Quarter round, Cross.

This order intentionally prioritizes zine/process marks over generic symbols. It also gives each family visible new
value without filling Cut shapes with geometry the maker can already approximate.

## Randomization ruling recommendation

Do **not** randomize Art placement, composition, catalogue order, or outline geometry.

Randomization would solve the wrong problem:

- It increases possible outputs without increasing the maker's understandable vocabulary.
- A shuffled catalogue weakens spatial learning, favourites, and fast repeat use.
- Generated tears and marks lose the authored hand that makes the supplies feel physical.
- Random placement makes authorship ambiguous and complicates undo, accessibility descriptions, screenshots,
  preview/PDF parity, and deterministic restoration.
- Stable geometry is especially important for a print tool: reopening and exporting must reproduce the same mark.

Retain the useful legacy idea at the correct boundary: **combinatorics, not random composition**. One authored
supply becomes many outcomes through the maker's chosen ink, scale, rotation, position, layering, and repetition.
The legacy seeded recipe remains a plausible future shelf-cover mechanism because it varies a constrained derived
cover under a fixed grid; it is not precedent for an Art-panel Surprise me button.

If authored variants are explored later, they should be a small reviewed set with stable variant identity persisted
in the document. That would be a document/rendering decision and requires its own ADR; it must never be a fresh
random draw during preview or export.

## D-080 consequence

A15's direction survives the research: distinct Art glyph, four families, local search, visible active filtering,
result feedback, and a recoverable empty state. Before design freeze it should be revised in two ways:

1. State clearly that Recent and Favourites are retrieval shortcuts over the same four-family catalogue.
2. Prototype the recommended first expansion (about 30–32), not only the current sixteen and not all fifty-one,
   so scroll depth, search value, touch behaviour, and visual fatigue can be judged on the actual near-term size.

No Compose work or supply admission should begin until that revised HTML is rendered, reviewed, and owner-ruled.
