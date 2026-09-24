# Colophon / About Zinely — design freeze

**Status:** DESIGN FROZEN + IMPLEMENTED · 2026-08-24 · [D-079](V2-SPEC-DEFECTS.md#d-079) option **(b)**  
**Canonical interactive specification:** [`mockups/v21-colophon.html`](mockups/v21-colophon.html)  
**Governing decisions:** [ADR-103](../DECISIONS.md#adr-103) · [ADR-104](../DECISIONS.md#adr-104) · [ZINE-DIRECTION §2.3](ZINE-DIRECTION.md)

**Owner copy amendment · 2026-08-29:** the internal small-press concept remains the **Colophon**, while the
maker-facing Shelf action is **`About`** and the destination is **`About Zinely`**. The opening below replaces
the earlier jargon-forward title and generic introduction; the four-section D-079 contract is unchanged.

**Owner hierarchy amendment · 2026-09-16:** the main About screen keeps one compact **`Licences & credits`**
row near the bottom. Typeface names and complete local notices move to a child screen. Decorative role blurbs
are retired. This supersedes the earlier direct three-card font section without removing any licence access.

**Implementation status · 2026-09-23 (beta.5):** the 2026-09-16 hierarchy is implemented in Compose (landed in
2cce221) and ships in `0.9.0-beta.5`. Of the [Compose acceptance gate](#compose-acceptance-gate), the automated
items have evidence: `ColophonScreenTest` (Back/focus through licence → credits → About → Shelf, the paper
preference, licence text and its failure state, 48dp Back) and `ColophonGoldenTest` (About and credits, light,
dark and maximum font scale), verified with `verifyRoborazziDebug --rerun-tasks`. There is no golden of a
licence screen and no maximum-font-scale dark golden of the credits screen.

**Device evidence · 2026-09-24** (Samsung SM-A176B, Android 16, beta.5 APK; record in the [beta.5 tester
package](../releases/0.9.0-beta.5.md#on-device---samsung-sm-a176b-android-16-api-36-build-a176bxxs7czh3-2026-09-24)):
the platform tree carries the frozen names (`Back to …` labels, `<family>, Read font licence` buttons), every
control is ≥ 48dp, licence text loads locally, system Back unwinds licence → credits → About → Shelf, and at 1.8×
in light theme nothing clips. **One gate item fails on device** (screens were opened by tap, not TalkBack
double-tap, so real use may differ): TalkBack's focus does not follow the focus rules above (entry lands on Back,
not the heading; return lands on Back, the first paragraph or the shelf title, not the invoking row or dock
action). **One is suspect:** the paper group's checked state and its `RadioButton` role sit on different platform
nodes ([ADR-059](../DECISIONS.md#adr-059)); only a listen can say whether it is announced correctly. Not done: a
TalkBack listen, the owner's first-time reading (Pass 2) and HTML/Compose pixel parity. Acceptance stays open.

This freeze resolves where Zinely states its privacy promise and where the product's small amount of
configuration belongs. It does not authorise Compose implementation by itself; production work still
follows the HTML-first workflow in [`CLAUDE.md`](../../CLAUDE.md#html-first-ui-workflow-mandatory).

## Screen question and ownership

The user arrives asking: **“What is Zinely, how is it made, and what small defaults can I choose?”**

- The Colophon belongs to the **Shelf**, never the Bench or Proof.
- The Shelf dock presents `Backups` and `About` as two equal, quiet secondary actions below the primary
  `Make a zine` action.
- **Colophon** remains the internal architectural name for the printer's-note concept established by ADR-103.
  Maker-facing copy uses **`About Zinely`** so no knowledge of print jargon is required; it is not named
  Settings, Privacy, or Licences.
- It presents the maker's note, default paper, the one offline/privacy sentence, one compact route to licences
  and credits, and version.

## Interaction rules

### Navigation and Back

1. `About` opens the full Shelf-owned `About Zinely` destination.
2. Its leading Back control and Android system Back both return to the existing Shelf state without reload.
3. `Licences & credits` opens a child destination containing the three bundled typeface names.
4. A typeface row there opens its complete local licence notice.
5. Back unwinds one level at a time: licence → credits → `About Zinely` → Shelf, restoring focus to the
   invoking row at each step.
6. There is no route from the Bench or Proof and no general settings hierarchy.

### Default paper

- A two-choice single-select group offers `A4` and `US Letter`.
- The selected value becomes the **leading choice** the next time the maker opens the existing Shelf create
  sheet; the other paper stays available on that same sheet and there is still no second confirmation step.
- Changing it never changes an existing zine, a currently open creation sheet, or a Proof press-run choice.
- The choice persists locally. There is no Apply/Save button; accepting a selection is immediate and the
  discrete change is announced.

### Licences and credits

- The main About screen shows one full-width `Licences & credits` button after the privacy promise and before
  version. It does not list font names or decorative design-role descriptions.
- The child screen lists `Averia Sans Libre`, `Fraunces`, and `Inter`, each named by its actual bundled family.
- Each row is a full button and opens the corresponding **locally bundled** SIL Open Font License notice.
- Licence text is selectable, vertically scrollable, and readable without network access or another app.
- The implementation may deduplicate byte-identical notices internally, but every displayed family must have
  a reliable route to the notice that governs it.

## Frozen copy

| Place | Copy |
|---|---|
| Shelf action | `About` |
| Title | `About Zinely` |
| Maker heading | `A little about this little app` |
| Maker note | The approved three-paragraph Aastra note in `Copy.Colophon` |
| Paper heading | `Paper for new zines` |
| Choices | `A4` · `US Letter` |
| Paper explanation | `We’ll suggest this paper when you start. You can always choose the other one.` |
| Main credits row | `Licences & credits` · `Open-source notices` |
| Credits introduction | `Zinely uses a few open-source typefaces. Their licence notices live here.` |
| Typeface rows | `Averia Sans Libre` · `Fraunces` · `Inter` |
| Licence row / child labels | `Read font licence` · `Font licence` |
| Privacy heading | `Your zines stay yours` |
| **The one product-level privacy sentence** | **`Zinely works offline. Your zines stay on this device unless you choose to share or back them up.`** |
| Version heading/value | `App version` · the real build version, not a hardcoded design value |

Operational recovery copy may still say that existing work is safe when a save/read/restore fails. That is
state-specific recovery guidance, not another product-level privacy slogan. Backup copy may explain what a
chosen destination or additive restore will do, but must not repeat the offline/no-upload promise.

## Layout, text scale, and themes

- The page is one continuous vertical scroll region below a stable top bar; content must never be clipped by
  the system bars or by a bottom dock.
- At maximum supported Android font scale, section order and copy remain intact and every control remains
  reachable by scrolling. No fixed-height content card may crop text.
- Touch targets are at least 48dp. Selection is not communicated by colour alone.
- In dark theme, About Zinely uses the current dark room/chrome treatment; its cards remain differentiated
  from the desk and all text remains AA. Licence text uses a high-contrast reading surface.
- Reduced motion requires no special replacement: the frozen flow has no essential animation.

## Accessibility semantics

- The main screen exposes `About Zinely` as its `paneTitle`; the credits child exposes `Licences & credits`;
  each licence child exposes its family name.
- Section headings are headings in traversal order.
- Paper is one single-select group with two radio controls exposing selected state and an accepted-change
  announcement.
- The compact main row is a button named `Licences & credits`. Typeface rows are buttons named
  `<family>, Read font licence`; decorative arrows are not announced.
- Back controls name their actual destination: `Back to My Shelf`, `Back to About Zinely`, or
  `Back to Licences & credits`.
- Version is readable text, not an interactive control.
- Focus enters at the screen heading, returns to the invoking dock action on exit, and returns to the invoking
  typeface row after closing a licence, then to the compact credits row after closing the credits child.

## Privacy-repetition amendment

The About Zinely sentence above is the sole product-level reassurance. The frozen Library empty state, Bench add
sheet/captions/Done toast, Proof ready row, and Backup sheet no longer repeat it. Failure-specific safety copy
is retained where it tells the user what happened and how to recover.

## Separate theme amendment

The owner has explicitly directed that Zinely's actual primary, secondary, accent, background, and supporting
palette be derived closely from the user-provided `37596.jpg`, not merely inspired by it. That is a **separate
theme amendment**, frozen in [`THEME-37596-FREEZE.md`](THEME-37596-FREEZE.md), affecting the shared token corpus
and every frozen surface. D-079 preserves the preceding V2.1 tokens and does not pre-empt that cross-product
colour implementation.

## Compose acceptance gate

- Shelf dock parity in content and empty states, including both quiet actions.
- Back stack and focus-return tests for Shelf → About Zinely → Licences & credits → licence.
- Preference tests proving the default affects only the next create sheet's leading paper choice.
- Tests proving all three bundled families resolve to locally readable licence text.
- Platform `AccessibilityNodeInfo` verification for radio roles/states, button names, headings, Back, focus,
  and ≥48dp targets.
- Light/dark and maximum-font-scale goldens, explicitly rerun with `verifyRoborazziDebug --rerun-tasks`.
- Two-pass physical-device verification and HTML/Compose pixel-parity review before acceptance.
