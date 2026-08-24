# Zinely theme palette — `37596.jpg`

Status: **DESIGN FREEZE — owner direction, 2026-08-24**  
Prototype: [`mockups/theme-37596.html`](mockups/theme-37596.html)  
Source reference: repository-root `37596.jpg` (**protected, untracked; do not modify, move, embed, or commit**)  
Scope: app chrome and studio surfaces only. It does **not** change maker inks, document colours, paper stock,
rendering, imposition, raster export, or PDF output.

## 1. Ruling

The product palette is no longer merely *inspired by* the reference. Its six labelled swatches are the
identity palette and must remain visually recognisable in the shipped theme:

| Source swatch | Hex | Frozen job |
|---|---:|---|
| Matcha | `#8E9546` | primary action in the day room; selected/active mark at night |
| Avocado Cream | `#BBCA6F` | secondary surface and dark-theme primary action |
| Wild Primrose | `#E9E29B` | day-room background; warm highlight at night |
| Tuft Bush | `#F2CFBB` | raised/card surface; lit colophon sheet |
| Strawberry Milk | `#F1B4AF` | soft supporting surface and gentle status ground |
| Camaron Pink | `#F28892` | punctuation/current selection; never the primary action |

The names and values above are transcribed from the user-provided image. `Tuft Bush` follows the owner's
current naming in this ruling. The image remains external evidence; the prototype cites it but does not copy
or encode it.

This amendment supersedes the old V2/V2.1 **colour values** when implemented. It preserves their semantic
discipline: one next-move colour, pink as punctuation rather than action, a separate consequence colour,
and a strict distinction between app chrome and maker-authored content.

## 2. Supporting colours — derived only where a source swatch cannot do the job

The reference contains no text ink, night-room ground, error red, or physical white/cream paper. Those jobs
cannot be filled accessibly or honestly by pretending one of the six swatches is something it is not.

| Token | Day | Night | Why it exists |
|---|---:|---:|---|
| `room` | `#E9E29B` | `#242312` | day is exact Wild Primrose; night is a derived olive-black room |
| `roomRaised` | `#BBCA6F` | `#323119` | day is exact Avocado Cream; night is a derived raised desk |
| `surface` | `#F2CFBB` | `#3D3920` | day is exact Tuft Bush; night chrome is derived, not inverted |
| `surfaceSoft` | `#F1B4AF` | `#46352E` | day is exact Strawberry Milk; night is a restrained warm support |
| `ink` | `#27270F` | `#FFF9DB` | derived high-contrast ink for running text |
| `inkSoft` | `#6A452F` | `#DAD7A0` | derived secondary ink; still AA on its allowed grounds |
| `primary` | `#8E9546` | `#BBCA6F` | exact Matcha by day; exact Avocado Cream at night for contrast |
| `onPrimary` | `#27270F` | `#242312` | derived dark ink; never pale text on these light fills |
| `secondary` | `#BBCA6F` | `#8E9546` | both exact source swatches; secondary never carries small pale text |
| `accent` | `#F28892` | `#F28892` | exact Camaron Pink in both themes |
| `supportPink` | `#F1B4AF` | `#F1B4AF` | exact Strawberry Milk, used sparingly |
| `highlight` | `#E9E29B` | `#E9E29B` | exact Wild Primrose, used sparingly at night |
| `consequence` | `#A9303D` | `#FF9CA4` | derived error/destructive colour, deliberately distinct from accent |

No additional brand hue may be introduced. Alpha variants, disabled states, dividers, shadows, and focus
rings must be derived from these tokens rather than becoming new named colours.

## 3. Accessibility and contrast rules

1. `ink` is the default text/icon colour. `inkSoft` is allowed only on `room`, `surface`, physical paper, and
   their night equivalents where it measures at least 4.5:1.
2. Text on all six source swatches uses dark `#27270F`/`#242312`. Pale text fails on the exact Matcha,
   Avocado, Primrose, Tuft, Strawberry, and Camaron swatches.
3. The day primary pairing is `#27270F` on `#8E9546` (**4.73:1**). The night primary pairing is `#242312`
   on `#BBCA6F` (**8.90:1**). These are the minimum frozen button pairings.
4. Camaron Pink is safe as a ground with dark ink (**6.30:1**) but remains punctuation, not action.
5. Focus indication must be visible without relying on colour alone: 2dp ink outline plus a 2dp offset.
6. Selected/current states combine colour with a stroke, mark, or text label. Never distinguish them only by
   Matcha versus Camaron.
7. Disabled controls retain legible labels and lose emphasis through surface/outline treatment, not opacity
   low enough to make text unreadable.
8. Consequence uses a distinct derived red and an icon/label. Camaron Pink must never mean delete or failure.
9. All implementation token/literal pairings require the existing automated contrast sweep plus large-text,
   TalkBack, and both-theme device verification.

## 4. Physical-paper honesty

The theme colours the **studio**, not the zine.

- `proofPaper` is the document's real paper stock. Existing cream/white stock values remain theme-invariant.
- A page, page thumbnail, imposed sheet, fold diagram, PDF preview, and exported output do not become Primrose,
  Tuft, or dark olive unless that colour is actually part of the document.
- Dark theme darkens the room around an artifact; it never dims or recolours the artifact.
- Maker inks are a separate `content.*` namespace. The six reference swatches do not replace the riso/content
  palette or rewrite saved documents.
- A chrome card may use Tuft Bush. A zine page may not use Tuft Bush merely because the app theme does.

This keeps the owner-requested visual match without making print preview dishonest or changing existing
projects.

## 5. Frozen component mapping

| Surface | Day | Night | Required colour behaviour |
|---|---|---|---|
| **Shelf** | Primrose room; Tuft/Strawberry supporting cards; Matcha primary | olive-black room; derived raised cards; Avocado primary | Covers keep their own paper/content colours. Camaron marks the current/recent item only |
| **Bench** | Primrose desk at the edges; real paper under the lamp | olive-black room; real paper stays lit | Tool chrome uses derived night surfaces. Selection uses Camaron plus an ink outline. Maker inks are unchanged |
| **Proof** | Primrose room around the physical sheet | olive-black room around the same physical sheet | Save/print uses primary. Fold/cut marks remain physical ink on physical paper |
| **Colophon** | Primrose room with a Tuft Bush printed card | dark room with the same lit Tuft Bush card | It is a printed page, so the card does not darken. Running text remains dark ink in both themes |
| **Sheets/dialogs** | Tuft Bush or Strawberry Milk, chosen by hierarchy | derived night surfaces | Primary action is Matcha/Avocado; Camaron is punctuation; consequence is never pink accent |
| **Snack/status** | Strawberry Milk for gentle confirmation | derived warm support surface | Status also has an icon/text label; errors use consequence |

## 6. Interaction and theme behaviour

- System day/night selection remains the default. Theme change recolours chrome in place; it does not rerender
  or mutate the document.
- No gradient is introduced. Hard offset shadows, ink outlines, typography, spacing, and motion remain governed
  by the existing V2.1 system.
- Theme transitions obey reduced-motion settings and never hide state changes.
- The exact source swatches must remain inspectable in the token source and tests. Do not approximate them via
  Material dynamic colour or wallpaper extraction.
- Dynamic colour stays off for identity-critical surfaces.

## 7. Implementation and verification gate

This is a design freeze, not permission to perform an unreviewed global repaint. Implementation must be a
focused theme package:

1. map existing semantic tokens to this table without changing component semantics;
2. preserve the paper/content namespace boundary;
3. update the canonical Shelf, Bench, Proof, and Colophon HTML/token references before Compose parity work;
4. run the literal-aware contrast gate for `color`, `fill`, `stroke`, icons, focus, disabled and error states;
5. re-record and explicitly inspect affected Roborazzi goldens in both themes;
6. verify Shelf → Bench → Proof → Colophon on a physical device, including maximum font scale and TalkBack;
7. compare the implemented chrome side-by-side with `37596.jpg` without importing that file into the app;
8. obtain independent visual/accessibility review before merge.

The interactive prototype freezes representative component relationships and both theme mappings. It is not
a production screen and adds no navigation or product behaviour.
