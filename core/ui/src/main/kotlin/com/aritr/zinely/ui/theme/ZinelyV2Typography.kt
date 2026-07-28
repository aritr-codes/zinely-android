package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.R

/**
 * The **V2 type foundation** — the two permanent families, their weights, and the base style
 * everything else departs from.
 *
 * [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §III fixes the pairing for the life of the
 * product: **Fraunces (voice/display) + Inter (work/body). Permanent. No third UI typeface.**
 *
 * ### Why this is a foundation and not a "type scale"
 *
 * The roadmap asks Phase A for a *type scale*. The frozen trilogy does not contain one, and saying so
 * is more useful than inventing one. Measured across the three specs, app chrome uses **46 distinct
 * sans styles and 8 distinct serif styles** — sizes including 8, 8.5, 9, 10, 10.5, 10.56, 11, 11.5,
 * 12, 12.16, 12.48, 12.5, 13, 13.5, 14, 14.5, 15, 15.2, 16, 16.8, 17.92, 19.2, 20.8, 25.92 and
 * 27.52px, on no ladder, with line-heights chosen per component.
 *
 * Publishing a 46-entry object and calling it a scale would be a scale in name only, and inventing a
 * tidier ladder would put a second source of truth next to the HTML — the exact failure the
 * HTML-first workflow exists to prevent. This module therefore ships what genuinely *is* shared —
 * the families, the weights, and the inherited base — and **components carry their own frozen size,
 * weight, line-height and tracking** from Phase B onward, exactly as [ZinelyDimens] already ruled for
 * spacing and radius on the V1 trilogy.
 *
 * The named styles below are only those a measurement showed to be genuinely *recurring* across
 * screens. A one-off value stays at its call site rather than being promoted here and given a name
 * that implies a system it isn't part of.
 *
 * *(The serif count of 8 depends on one classification call worth stating: the Bench's `.t-title` and
 * `.t-body` are counted as **zine content**, not chrome, because `v2-bench.html:432,470` recolour them
 * with the maker's ink. Counting them as chrome gives 10. There is no ladder either way.)*
 */
public object ZinelyV2Fonts {
    /**
     * `--sans` / the work face — **Inter**, at the four weights the frozen chrome actually uses
     * (400/500/600/700) and no more. Bundled locally; the prototypes' `fonts.googleapis.com` stack is
     * unreachable by construction, because a CDN font request is a network request and the privacy
     * invariant forbids one.
     *
     * Declared here rather than aliased to V1's `ZinelyFonts.Shell`, which resolves to the same four
     * resources. The duplication is one line and it is deliberate: aliasing would make the V2 layer
     * depend on the V1 layer that **C0** deletes, so the retirement that should be a file deletion
     * would instead become a V2 edit. Same font, no shared lifetime.
     */
    public val Work: FontFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

    /**
     * `--serif` / the voice face — **Fraunces**, at 400 / 500 / 600.
     *
     * All three weights are real requirements of the frozen chrome, and the split between them is
     * itself an open question (**D-005** in [V2-SPEC-DEFECTS.md](docs/design/V2-SPEC-DEFECTS.md)):
     * the Bench and Proof set every serif heading at **500**, while the Library — frozen a day
     * earlier, before the `--serif` token existed — sets its headings at **600**. Both files are
     * frozen. Shipping all three weights lets Phase B proceed either way and **prejudges nothing**;
     * whichever way the owner rules, the face is already present.
     *
     * `400` is not speculative either — the Proof's fold-step caption (`.foldcap`, `:210`) is serif
     * body text at 14px with no weight set.
     *
     * **Optical size.** All three are the static **9pt** cut. The variable face would let `opsz`
     * track the point size, but Compose drives that through `FontVariation`, which the platform
     * ignores below API 26, and `minSdk` is 24. 9pt is the nearest optical size to the 14–28px range
     * the voice is actually set at. `fraunces_regular` and `fraunces_semibold` are byte-identical to
     * upstream's own statics; `fraunces_medium` is instanced from the upstream variable font at
     * `opsz=9 wght=500 SOFT=0 WONK=1` because **upstream ships no Medium static** — its advances
     * interpolate strictly between the two shipped cuts, which is the check that it came off the same
     * production line.
     *
     * No italic. Italic Fraunces appears only in *mock zine content* — the Proof's cover subtitle
     * (`:111`), pull-quote (`:114`) and back cover (`:123`), all inside the block the file itself
     * labels `/* zine content (real, not lorem) */`. Never in chrome, in any of the three files. Zine
     * content is drawn by the render engine rather than by Compose, so bundling an italic cut here
     * would only make it reachable from chrome, which no frozen spec does. See **D-004**.
     */
    public val Voice: FontFamily = FontFamily(
        Font(R.font.fraunces_regular, FontWeight.Normal),
        Font(R.font.fraunces_medium, FontWeight.Medium),
        Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    )
}

/**
 * The V2 type foundation, threaded through `LocalZinelyV2Typography`.
 *
 * Additive alongside [ZinelyTypography] (V1), which stays until C0 — see
 * [ADR-071](docs/DECISIONS.md#adr-071).
 */
@Immutable
public data class ZinelyV2Typography(
    /** Inter — UI, metadata, running work. */
    val work: FontFamily = ZinelyV2Fonts.Work,
    /** Fraunces — headings, titles, the product's voice. */
    val voice: FontFamily = ZinelyV2Fonts.Voice,
    /**
     * The inherited base: **16sp Inter 400**, the size every unstyled control resolves to.
     *
     * `body` sets no `font-size` in any of the three frozen files, so the browser default of 16px
     * governs — and four real chrome controls rely on it: the Bench's supply-sheet rows
     * (`.supply .opt`), the Library's sheet action rows (`.sheet .act`), and the Proof's READY band
     * (`.ready`) and commit buttons (`.btn`).
     *
     * **`lineHeight` is `Unspecified` because those four controls do not agree on one**, not because
     * they share a value this omits. Measured:
     *
     * | Control | Renders at |
     * |---|---|
     * | Bench `.supply .opt` | **1.5** — sets only `font-family`, so it inherits `body{line-height:1.5}` |
     * | Library `.sheet .act` | **`normal`** — `font-family:inherit`, and the Library body sets no line-height at all |
     * | Proof `.btn` | **1** — declared outright, one line after its `font:inherit` |
     * | Proof `.ready` | **`normal`** — the one clean `font:inherit`, and it renders no direct text of its own |
     *
     * So line-height is a per-component value in this corpus, like size and tracking, and pinning any
     * single number here would be wrong for at least two of the four. Phase B carries it per component.
     *
     * (An earlier revision justified this by saying the inheriting controls all use the CSS
     * `font:inherit` shorthand, which resets `line-height` to `normal`. That reached the right value
     * through wrong reasoning: only `.ready` is a clean case — of the Proof's seven `font:inherit`
     * uses, five override `font-size` and two override `line-height` — and the Bench's supply rows do
     * not use the shorthand at all and genuinely do inherit 1.5. Left recorded because the mistake is
     * more useful than the correction: a mechanism that explains one instance can look like a rule.)
     */
    val base: TextStyle = TextStyle(
        fontFamily = ZinelyV2Fonts.Work,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    /**
     * An all-caps section label — the one genuinely recurring chrome pattern, appearing six times
     * across the Bench and Proof at 10–11sp, weight 600–700, with wide positive tracking
     * (`.12em`–`.13em`) and `text-transform:uppercase`.
     *
     * Sizes and tracking still vary slightly per instance, so this carries the *shared shape* only;
     * a call site that needs 10sp/.12em rather than 10.5sp/.13em overrides those. The uppercasing is
     * **not** applied here — that is the caller's job via the copy layer, because a
     * `text-transform` baked into a `TextStyle` would also uppercase text handed to a screen reader.
     */
    val sectionLabel: TextStyle = TextStyle(
        fontFamily = ZinelyV2Fonts.Work,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        letterSpacing = 0.13.em,
    ),
)
