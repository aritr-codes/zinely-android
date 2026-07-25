# Owner proposal — expanded script support (Direction B)

**Status:** proposal for owner decision · **not scheduled, not started** · drafted 2026-07-24
**Companion:** the mandatory input-time honesty behaviour (Direction A) is being implemented separately
and is **not** blocked on this. This document exists so that *rendering* additional scripts is decided
deliberately, **one family at a time**, rather than as a single "internationalization" yes/no.

> **This proposal recommends nothing as a bundle.** Each script family below is costed and gated on its
> own merits. The correct output of this document is a set of independent rulings — "yes Devanagari, no
> CJK", or any other combination — not one decision.

---

## 0. What is already true, and must survive any decision here

Two invariants are load-bearing and the reason document text is bundled-font-only today
([ADR-028 §4.2](../DECISIONS.md#adr-028)):

1. **`preview == export`.** The editor page, Read, PNG and PDF all draw through one `CanvasReplayer` +
   `SharedTextLayout`. A glyph must have the *same metrics* on every surface, which a device-variable
   system font cannot guarantee.
2. **Reproducible vector PDF.** Text is emitted as vector outlines via `StaticLayout.draw` onto a
   `PdfDocument` canvas, identical on any device.

**Key finding for this proposal:** bundling a script's font and drawing it through the *existing* shared
path **preserves both invariants** — the font is still bundled, still deterministic, still vector. The
determinism risk appears **only** if a family is served by *system* fallback. So Direction B is not a
threat to determinism per se; it is a question of **(a) bundle size**, **(b) the cross-font fallback
engineering below**, and **(c) a small number of families that break invariant 2 specifically**.

---

## 1. The one cross-cutting engineering constraint (applies to every family)

`SharedTextLayout` builds a `StaticLayout` with a **single** `TextPaint.typeface` — a
`Typeface.createFromAsset(...)` with **no fallback chain**. Android shapes and renders complex scripts
correctly **if the glyphs are in that typeface**, but it does **not** fall back to a *second bundled
font* for a code point the first lacks. Two consequences:

- **A text box in one script** (e.g. all-Devanagari) works by selecting the right bundled font for that
  box — cheap.
- **A text box mixing scripts** (Latin + Devanagari in one run) needs **cross-font fallback within a
  run.** The clean API for a bundled fallback chain is `Typeface.CustomFallbackBuilder` — **API 29+**,
  and `minSdk` is **24** ([ADR-024](../DECISIONS.md#adr-024)). Below 29 the options are: raise the
  fallback floor to 29 (degrade gracefully on 24–28), or split runs by script and lay each out with its
  own font and position them manually — a real change to the single-`StaticLayout` design and its
  `LAYOUT_SCALE` parity contract.

**This constraint, not glyph coverage, is the actual cost driver for mixed-script text.** Each family
below is rated on whether it needs only single-script support (cheap) or mixed-run fallback (the
API-29 problem).

---

## 2. Per-family evaluation

Sizes are **order-of-magnitude** for static TTFs (regular + bold, the minimum for the existing
bold/italic model — italic doubles it). They must be re-measured against the actual subsetted files
before any ruling is executed; they are here to separate "feasible" from "size-prohibitive", which they
do unambiguously.

### 2.1 Latin-Extended / Cyrillic-Extended / Polytonic Greek
- **Families:** `LATIN` beyond core, `CYRILLIC_EXTENDED`, `GREEK_EXTENDED` (already *classified* and
  refused by `SupportedScripts`).
- **Size impact:** **~0.** Largely already in Inter's `cmap`; the gap is a handful of blocks
  ([FontCoverage.kt](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/FontCoverage.kt)
  records the residue). Possibly a different Inter cut or a small supplemental face.
- **Determinism / PDF:** no change — same font family, same path, still vector.
- **API constraints:** none — same script direction, no mixed-run fallback (it's the same font).
- **Complexity:** **XS.** Mostly a coverage-set widening + a font-file swap, guarded by the existing
  `FontCoverage` cmap test.
- **Migration risk:** none — additive; previously-refused characters begin rendering.
- **Verdict input:** the cheapest win; the only question is whether anyone needs these blocks.

### 2.2 Devanagari (Hindi, Marathi, Nepali) · Bengali
- **Size impact:** **~200–350 KB per weight per script** (Noto Sans Devanagari / Bengali statics).
  Regular+Bold ≈ **0.5–0.7 MB per script**. Against a ~15 MB app, **feasible**.
- **Determinism / PDF:** **preserved.** Bundled, deterministic, vector. Indic conjuncts and matra
  positioning are shaped by Android/Minikin from the bundled font — the outlines are still vector for
  the PDF.
- **API constraints:** single-script boxes work on `minSdk 24`. **Mixed Latin+Indic runs hit the
  API-29 fallback constraint** in §1.
- **Complexity:** **M.** Bundle font + register family + fallback strategy for mixed runs. No RTL, no
  bidi. The shaping itself is free (Android does it).
- **Testing:** `FontCoverage` cmap guard extended to the new file; golden of a shaped conjunct on each
  surface; a mixed Latin+Devanagari box golden to prove run fallback.
- **Migration risk:** low — additive; no document-format change (the codepoints were always stored, just
  unrendered). A document authored before support renders correctly the moment the font ships.
- **Verdict input:** **the strongest candidate.** Real user demand (Hindi/Bengali are top-tier), feasible
  size, invariants preserved. The only real work is the mixed-run fallback.

### 2.3 Tamil · Thai
- **Size impact:** **~100–250 KB per weight per script.** Feasible.
- **Determinism / PDF:** preserved (as §2.2).
- **API constraints:** as §2.2. Thai has no inter-word spaces (line-breaking is dictionary-based); the
  current `BREAK_STRATEGY_SIMPLE` will break poorly. Tamil shapes fine.
- **Complexity:** **M** (Tamil), **M–L** (Thai, because of line-breaking).
- **Verdict input:** feasible; lower demand than §2.2; Thai carries a line-break subtlety worth pricing
  separately.

### 2.4 Arabic · Hebrew (RTL)
- **Size impact:** **~150–300 KB per weight per script.** Size is not the blocker.
- **Determinism / PDF:** preserved *if* bundled.
- **API constraints:** **the blocker is bidirectional layout.** `SharedTextLayout` pins
  `TextDirectionHeuristics.FIRSTSTRONG_LTR` and a fixed alignment mapping. RTL and mixed-direction
  (bidi) text need that reworked, and Arabic needs contextual **joining** (initial/medial/final forms) —
  Android shapes this from the font, but caret/selection/alignment in a mixed-direction box is real work
  and interacts with the reframe/selection geometry.
- **Complexity:** **L–XL.** Not the font — the layout direction model.
- **Verdict input:** defer unless there is specific demand; it is a layout-engine change, not a font
  drop.

### 2.5 CJK — Chinese (Han) · Japanese (Hiragana/Katakana + Han) · Korean (Hangul)
- **Size impact:** **the disqualifier for a naive bundle.** Noto Sans CJK is **multiple MB per weight
  per language**, and full Han coverage is **~16–20 MB per weight**. Two weights of one CJK language
  **doubles or triples the entire app**; all three languages is prohibitive.
  - *Mitigation exists but is real work:* subset to the most common few-thousand glyphs, or ship the font
    as an on-demand download (breaks "install-to-share, zero network" — [DoD 9](../zinely-v1.md), the
    privacy invariant — so an on-demand font is itself an owner decision, not a workaround).
- **Determinism / PDF:** preserved if bundled; a *subset* font risks a `.notdef` for a rarer character,
  which reintroduces the exact silent-blank problem Direction A exists to catch — so Direction A's
  warning must remain even with partial CJK.
- **API constraints:** no reordering, but the glyph count stresses everything; mixed CJK+Latin is common
  and hits §1.
- **Complexity:** **XL**, dominated by the size/packaging decision, not shaping.
- **Verdict input:** **do not bundle full CJK.** If CJK is wanted, it is a distinct project about
  *subsetting or on-demand delivery*, and the latter collides with the zero-network invariant.

### 2.6 Emoji
- **Size impact:** **~9–24 MB** (Noto Color Emoji / bitmap CBDT). App-doubling on its own.
- **Determinism / PDF:** **breaks invariant 2 uniquely.** Emoji are **colour** glyphs (CBDT bitmap or
  COLR/CPAL vector-with-palette). `StaticLayout.draw` of colour emoji onto a `PdfDocument` canvas does
  **not** produce clean reproducible vector text — it rasterises or drops colour. So emoji is the one
  family that cannot satisfy the reproducible-vector-PDF invariant without a separate rasterisation path.
- **API constraints:** colour-font support and `EmojiCompat` add their own version matrix.
- **Complexity:** **XL**, and it forces an explicit exception to a core invariant.
- **Verdict input:** **defer, and treat as a separate class from text scripts.** Emoji is not "another
  script"; it is a colour-rendering + PDF-model decision.

---

## 3. Summary matrix (owner decision surface)

| Family | Size (reg+bold) | Determinism | PDF | API-29 mixed-run issue | RTL/bidi | Complexity | Recommendation input |
|---|---|---|---|---|---|---|---|
| Latin/Cyrillic/Greek **extended** | ~0 | ✅ | ✅ | no | no | **XS** | cheapest; do if wanted |
| **Devanagari / Bengali** | ~0.5–0.7 MB/script | ✅ | ✅ | yes | no | **M** | **strongest candidate** |
| Tamil / Thai | ~0.2–0.5 MB/script | ✅ | ✅ | yes | no | M / M–L | feasible; Thai line-break |
| Arabic / Hebrew | ~0.3–0.6 MB/script | ✅ | ✅ | yes | **yes** | **L–XL** | defer; layout-engine change |
| **CJK** | **~16–40 MB** | ✅* | ✅* | yes | no | **XL** | do **not** bundle full; subset/on-demand is a separate call |
| **Emoji** | **~9–24 MB** | ✅ | **❌** | yes | no | **XL** | defer; breaks vector-PDF; separate class |

\* CJK determinism/PDF hold only for a *complete* bundled font; a subset reintroduces the silent-blank
case, which is exactly why Direction A's warning is permanent.

---

## 4. If any family is approved — the shared prerequisites

Independent of *which* families, the first execution unit is the **cross-font fallback strategy** (§1),
because every mixed-script box depends on it and it carries the only genuine architecture risk:

- Decide the `minSdk`-24 answer: graceful degradation with `CustomFallbackBuilder` on 29+, or manual
  per-script run layout that preserves `SharedTextLayout`'s `LAYOUT_SCALE` parity on all versions.
- Keep the single shared path — a second layout path would reintroduce preview≠export drift.
- Extend `FontCoverage`'s cmap guard to every newly bundled file so a promised script that the file does
  not actually cover fails the build, not the user's page.

## 5. Testing strategy (any family)

- **cmap guard** (`:render-android`, exists) extended per bundled file — the file-level promise check.
- **`analyzeTextCoverage`** (`:core:model`) updated: an approved script leaves `BUNDLED_SCRIPTS`, so
  Direction A stops warning about it automatically — verify the warning narrows correctly.
- **Golden parity** per surface (editor page, Read, PNG, PDF) for a shaped sample and a **mixed-script**
  sample, recorded on the pinned CI image.
- **PDF vector assertion** that the added glyphs emit as vector (fails for emoji — which is the point).
- **Determinism test:** same document → byte-identical PDF across two runs.

## 6. Migration risks (any family)

- **No document-format change** in any family: unsupported codepoints are already stored verbatim, so a
  document authored today renders correctly the day its font ships — *provided Direction A retained the
  characters rather than stripping them, which it does.* This is the concrete reason Direction A must
  never strip.
- **Golden churn:** any surface that gains a rendered glyph re-records goldens — reviewed per diff.
- **APK size** is the user-visible migration cost for §2.5/§2.6 and must be stated in release notes.

---

## 7. The decision this document asks for

Rule **per family**, not once:

1. Latin/Cyrillic/Greek extended — yes / no
2. Devanagari — yes / no
3. Bengali — yes / no
4. Tamil — yes / no · Thai — yes / no
5. Arabic — yes / no · Hebrew — yes / no
6. CJK — no-bundle confirmed / open a separate subsetting-or-on-demand proposal
7. Emoji — defer confirmed / open a separate colour-PDF proposal

Any "yes" then schedules the §4 fallback prerequisite first, and each family lands as its own reviewed
change with its own goldens. **Nothing here is implemented until at least one family is ruled in.**
