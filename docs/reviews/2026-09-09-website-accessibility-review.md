# Website accessibility review — 2026-09-09

## Scope and claim boundary

This review covers the Zinely homepage, generated privacy-policy page, and accessibility page. The conformance
target is **WCAG 2.0 Level AA**. It records source inspection and automated checks; it is not a certification and
does not replace testing with disabled users.

## Evidence

- The W3C Nu HTML checker returned no messages for the source homepage and accessibility page.
- The live generated privacy page returned no markup errors before this contact-only update.
- Every normal-size text/background pair used by the site is at least **4.71:1**. The weakest pairs are
  `#6a452f` on `#bbca6f` (4.71:1), `#27270f` on `#8e9546` (4.73:1), and `#6a452f` on `#f1b4af` (4.75:1).
- The pages use semantic headings, lists, landmarks, native links, descriptive titles, a skip link, visible focus,
  image alternatives, responsive relative sizing, and reduced-motion handling.
- The site contains no forms, audio, video, autoplay, flashing content, time limits, or scripted interaction.

## WCAG 2.0 A/AA disposition

| Criteria | Result | Evidence |
|---|---|---|
| 1.1.1 | Pass | Meaningful screenshots have descriptive alternatives; decorative brand imagery has an empty alternative inside a named home link. |
| 1.2.1–1.2.5 | Not applicable | No audio or video. |
| 1.3.1–1.3.3 | Pass | Semantic HTML, logical source order, and no instructions based only on shape, position, sound, or colour. |
| 1.4.1–1.4.2 | Pass / not applicable | Colour is not the only carrier of information; no audio. |
| 1.4.3 | Pass | Measured minimum normal-text contrast is 4.71:1. |
| 1.4.4 | Pass by inspection | Relative text sizing, responsive grids, wrapping navigation, and horizontally scrollable policy tables preserve content at narrow widths. A manual 200% browser-zoom spot check remains good practice. |
| 1.4.5 | Pass | Text remains HTML; the one product screenshot is illustrative and has an alternative. |
| 2.1.1–2.1.2 | Pass | All interaction uses native links; there is no scripted focus or keyboard trap. |
| 2.2.1–2.2.2 | Not applicable | No time limits or moving/auto-updating content. |
| 2.3.1 | Not applicable | No flashing content. |
| 2.4.1–2.4.7 | Pass | Skip link, page titles, logical focus order, descriptive links, consistent navigation, meaningful headings, and visible focus. |
| 3.1.1–3.1.2 | Pass | Pages declare English and contain no unexplained language changes. |
| 3.2.1–3.2.4 | Pass | Focus and activation do not cause unexpected context changes; navigation is consistent. |
| 3.3.1–3.3.4 | Not applicable | No user input or forms. |
| 4.1.1–4.1.2 | Pass | W3C-valid markup and native link semantics; no custom scripted controls. |

## Residual verification

After each material visual or interaction change, repeat HTML validation and contrast measurement, then manually
spot-check keyboard order, visible focus, and 200% text zoom in a supported browser. A future screenshot gallery
must retain useful alternative text and must not become an auto-advancing carousel.
