package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The **V2 chrome palette** — a one-to-one transcription of the `:root` custom properties in the
 * DESIGN-FROZEN V2 trilogy (`docs/design/mockups/v2-{library,bench,proof}.html`).
 *
 * Invariant: **the HTML is the specification.** A value may only change here after it has changed in
 * the frozen HTML first ([COMPOSE-IMPLEMENTATION-RULES.md](docs/COMPOSE-IMPLEMENTATION-RULES.md)).
 * `ZinelyV2ColorsTest` pins every value so drift fails the build.
 *
 * ### This is additive; [ZinelyColors] (V1) is untouched
 *
 * V2 lands alongside the V1 riso palette rather than replacing it. `ZinelyColors` is pinned to the
 * *V1* frozen trilogy (`docs/design/v1/shelf.html`) by `ZinelyTokensTest`, and 65 editor goldens are
 * recorded against it. Re-palletting in place would break both with no authority to do so — the V1
 * corpus retires when **C0** (specification reconciliation) lands, not before. Until then the two
 * palettes coexist and no call site is migrated by this change.
 *
 * ### Where each token comes from (the frozen files do not all declare the same set)
 *
 * The three frozen files declare **overlapping but unequal** token blocks. Per the owner ruling on
 * Q1 (recorded in the implementation ADR): **the Bench is the canonical source for shared V2
 * implementation tokens**; where a screen omits one, it inherits the Bench definition rather than
 * having a value inferred for it.
 *
 * | Group | Declared in | Notes |
 * |---|---|---|
 * | `paper`…`consequence` (the 14 semantic roles) | all three, byte-identical | also [V2-TOKENS.md](docs/design/V2-TOKENS.md) |
 * | `onMatcha`, `accentOnInk`, `chrome`, `chromeLine`, `sheet`, `scrim`, `frameShadow` | Bench + Proof only | absent from the Library spec; Bench-canonical |
 * | `hair`, `shadow`, `contact` | Library only | the shelf's hairline and its grounded cover shadows |
 *
 * **Deliberately NOT ported — prototype scaffolding.** `--stage` (the page backdrop *behind* the
 * phone mock, `v2-library.html:34`), `--phone` and `--phone-frame` (the fake device bezel,
 * `v2-bench.html:71-72`) describe the browser mock's staging, not any in-app surface. `--frame-shadow`
 * is **not** in this category despite its name: it draws **seven** real in-app shadows in the Bench
 * (material tiles, page thumbnails, the contextual toolbar, the ink popover, the bottom sheet, the
 * page grid) and **eleven** across Bench + Proof, so it ships as [frameShadow]. Its two remaining
 * uses *are* the mock's own `.phone` bezel and are excluded from that count — the distinction matters
 * because counting them is exactly the misreading that put `--frame-shadow` on the scaffolding list
 * in an earlier draft of this file.
 *
 * **Deliberately NOT ported — belongs to another namespace, not to chrome.** `--ink-teal`,
 * `--ink-ochre`, `--ink-straw` and `--ink-matcha` (`v2-bench.html:19`, `v2-proof.html:19`) are the
 * four **cover inks** — [V2-TOKENS.md](docs/design/V2-TOKENS.md) §"Cover inks (the *maker's* palette,
 * not the chrome)". They land in the `content.*` namespace in **A2**, together with the ten-ink Bench
 * H4 in-page maker set, and are held apart from chrome by lint. `--grain` (all three files) is a
 * texture rather than a colour and lands with the paper system in **A6**. They are named here so that
 * an audit enumerating the frozen `:root` blocks can account for **every** custom property and tell a
 * deferral apart from an omission.
 *
 * **One name per value.** The Library spells strawberry `--straw*` while the Bench, the Proof and
 * V2-TOKENS.md spell it `--strawberry*`. The values are identical; Compose carries the
 * `strawberry*` spelling only.
 *
 * ### Dark is re-derived, not inverted
 *
 * Every one of these tokens carries a distinct dark value — this palette states dark in full rather
 * than as a diff, because "a warm charcoal room, accents re-tuned to hold on it" is a re-derivation
 * and expressing it as `copy()` overrides would misrepresent it as an adjustment of the light theme.
 * Dynamic (wallpaper) colour stays off — see [ZinelyTheme].
 */
@Immutable
public data class ZinelyV2Colors(
    // ----- the artifact and the room ---------------------------------------------------------
    /** `--paper` — the sheet you make on; the artifact's surface. */
    val paper: Color,
    /** `--paper-edge` — the sheet's cut edge / hairline. */
    val paperEdge: Color,
    /** `--desk` — the table the paper sits on. Warm charcoal at night, never blue-black. */
    val desk: Color,
    /** `--desk-edge` — room dividers. */
    val deskEdge: Color,

    // ----- ink -------------------------------------------------------------------------------
    /** `--ink` — body and headings on paper. **★ AA-critical** on [paper]. */
    val ink: Color,
    /** `--ink-soft` — secondary text, captions. **★ AA-critical** on [paper]. */
    val inkSoft: Color,
    /** `--ink-faint` — faint/decorative only. **Not for body text** — see the recorded ratio in
     *  `ZinelyV2ContrastTest`. */
    val inkFaint: Color,

    // ----- matcha: the one action colour -----------------------------------------------------
    /** `--matcha` — the single primary, "your next move". **★ AA-critical** with [onMatcha]. */
    val matcha: Color,
    /** `--matcha-text` — matcha as icon/text/selected-state. **★ AA-critical** on [paper]. */
    val matchaText: Color,
    /** `--matcha-tint` — soft selected surface, sitting behind dark ink. */
    val matchaTint: Color,
    /** `--on-matcha` — the label on a matcha fill. **★ AA-critical** on [matcha]. */
    val onMatcha: Color,

    // ----- strawberry: punctuation, never an action ------------------------------------------
    /** `--strawberry` (Library: `--straw`) — a stamp, a current-page dot. Accents, **never actions**. */
    val strawberry: Color,
    /** `--strawberry-text` — deep strawberry when it must carry text. **★ AA-critical** on [paper]. */
    val strawberryText: Color,
    /** `--strawberry-tint` — a soft blush surface. */
    val strawberryTint: Color,

    // ----- consequence -----------------------------------------------------------------------
    /**
     * `--consequence` — delete / real error, and nothing else. **★ AA-critical** on [paper].
     *
     * Kept deliberately distinct from [strawberry] so a warning never reads as fruit. This is the
     * third and final chrome hue: matcha (action) + strawberry (punctuation) + consequence (error).
     * There is no fourth.
     */
    val consequence: Color,

    // ----- chrome surfaces (Bench-canonical) -------------------------------------------------
    /** `--chrome` — the toolbar/rail fill that frames the page. */
    val chrome: Color,
    /** `--chrome-line` — the hairline separating chrome from the page. The most-used chrome token
     *  in the frozen Bench (17 uses). */
    val chromeLine: Color,
    /** `--sheet` — a bottom-sheet / popover fill. */
    val sheet: Color,
    /**
     * `--scrim` — dims everything behind an open sheet.
     *
     * **Bench-canonical per the Q1 ruling.** The Proof declares a heavier scrim (`.42` light / `.55`
     * dark vs the Bench's `.34` / `.50`). That divergence is a per-screen frozen value, recorded here
     * and carried as a Proof-local override in Phase D rather than averaged into the shared token.
     */
    val scrim: Color,
    /** `--accent-on-ink` — matcha re-tuned to hold when it sits on an ink-dark ground. */
    val accentOnInk: Color,

    // ----- hairlines and shadows -------------------------------------------------------------
    /** `--hair` (Library) — the shelf's hairline; ink at low alpha. */
    val hair: Color,
    /** `--shadow` (Library) — the cast shadow that grounds a cover on the shelf. */
    val shadow: Color,
    /** `--contact` (Library) — the tight contact shadow where a cover meets the surface. Paired with
     *  [shadow]: the two together are what make a cover read as an object resting on the desk rather
     *  than a rectangle floating above it. */
    val contact: Color,
    /** `--frame-shadow` (Bench + Proof) — the elevation shadow under in-app chrome: material tiles,
     *  page thumbnails, the contextual toolbar, the ink popover, the bottom sheet, the page grid. */
    val frameShadow: Color,
)

// The literals below are the frozen `:root` bytes. rgba() tokens are expressed as base × alpha so the
// fraction stays exact rather than rounding through an 8-bit hex channel.

/**
 * `:root` — the light token block.
 *
 * Sources: `v2-bench.html:14-21` (the 14 semantic roles + the chrome group),
 * `v2-library.html:15-19` (`hair`, `shadow`, `contact`).
 */
public fun zinelyV2LightColors(): ZinelyV2Colors = ZinelyV2Colors(
    paper = Color(0xFFF7F2E7),
    paperEdge = Color(0xFFEEE6D4),
    desk = Color(0xFFECE3D1),
    deskEdge = Color(0xFFE1D6BF),

    ink = Color(0xFF2A251E),
    inkSoft = Color(0xFF5B5347),
    inkFaint = Color(0xFF8C8269),

    matcha = Color(0xFF5E6B2F),
    matchaText = Color(0xFF4C5826),
    matchaTint = Color(0xFFDCE3C0),
    onMatcha = Color(0xFFFFFFFF), // --on-matcha:#fff

    strawberry = Color(0xFFE98F97),
    strawberryText = Color(0xFFA6474F),
    strawberryTint = Color(0xFFF6DAD3),

    consequence = Color(0xFFA6382A),

    chrome = Color(0xFFFBF7EE),
    chromeLine = Color(0xFFE4DAC6),
    sheet = Color(0xFFFBF7EE),
    scrim = Color(0xFF2A251E).copy(alpha = 0.34f), // rgba(42,37,30,.34) — == ink at .34
    accentOnInk = Color(0xFFB7C47C),

    hair = Color(0xFF2A251E).copy(alpha = 0.12f), // rgba(42,37,30,.12) — == ink at .12
    shadow = Color(0xFF3C3424).copy(alpha = 0.34f), // rgba(60,52,36,.34)
    contact = Color(0xFF3C3424).copy(alpha = 0.22f), // rgba(60,52,36,.22)
    frameShadow = Color(0xFF3A3020).copy(alpha = 0.28f), // rgba(58,48,32,.28)
)

/**
 * `:root[data-theme="dark"]` — the dark token block. **Re-derived, not inverted.**
 *
 * Sources: `v2-bench.html:27-36`, `v2-library.html:22-27`. Note what the re-derivation does that an
 * inversion would not: `paper` stays a *warm* charcoal (`#2F2A22`) rather than becoming black, and
 * `ink` becomes warm cream rather than white — the sheet is still a sheet, lit by a dimmer room.
 * The three shadow tokens collapse to pure black at higher alpha, because on a dark desk a warm-brown
 * shadow reads as a stain rather than an absence of light.
 */
public fun zinelyV2DarkColors(): ZinelyV2Colors = ZinelyV2Colors(
    paper = Color(0xFF2F2A22),
    paperEdge = Color(0xFF39322A),
    desk = Color(0xFF201D18),
    deskEdge = Color(0xFF2A261F),

    ink = Color(0xFFECE4D3),
    inkSoft = Color(0xFFB4AB97),
    inkFaint = Color(0xFF857C69),

    matcha = Color(0xFF93A257),
    matchaText = Color(0xFFB7C47C),
    matchaTint = Color(0xFF363826),
    onMatcha = Color(0xFF20240E),

    strawberry = Color(0xFFD98289),
    strawberryText = Color(0xFFE8A6AB),
    strawberryTint = Color(0xFF3C2C2A),

    consequence = Color(0xFFE0857A),

    chrome = Color(0xFF252017),
    chromeLine = Color(0xFF3A332A),
    sheet = Color(0xFF252017),
    scrim = Color.Black.copy(alpha = 0.50f), // rgba(0,0,0,.5)
    accentOnInk = Color(0xFF4C5826),

    hair = Color(0xFFECE4D3).copy(alpha = 0.13f), // rgba(236,228,211,.13) — == ink at .13
    shadow = Color.Black.copy(alpha = 0.60f), // rgba(0,0,0,.6)
    contact = Color.Black.copy(alpha = 0.50f), // rgba(0,0,0,.5)
    frameShadow = Color.Black.copy(alpha = 0.50f), // rgba(0,0,0,.5)
)
