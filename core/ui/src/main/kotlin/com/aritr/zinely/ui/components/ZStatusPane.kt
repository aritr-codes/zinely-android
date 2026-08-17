package com.aritr.zinely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/**
 * The shared error/empty chrome: a badge, a voice heading, a soft body, then the caller's CTA.
 *
 * Positioning is divergent in the spec (shelf: flow child; bench/proof: absolute overlay) and
 * stays at call sites. Body width: the CSS caps at `28ch`/`29ch` — `ch` has no Compose analogue,
 * so callers pass [bodyMaxWidth] when the parity gate measures it; unbounded by default.
 *
 * ### V2.1 — ADR-102 P8
 *
 * ```css
 * .fail{gap:var(--gap-md)}
 * .fail .mk{width:60px;height:60px;border-radius:var(--br-pill);background:var(--paper);
 *   border:2px solid var(--jam);box-shadow:3px 3px 0 var(--jam)}
 * .fail h2{font-family:var(--voice);font-size:1.6rem;font-weight:700;margin:var(--gap-sm) 0 0}
 * .fail p{color:var(--ink-soft);font-size:.94rem}   .empty p{line-height:1.55}
 * ```
 *
 * **The badge keeps its parameters, and that constrains what could change here.**
 * [badgeBackground]/[badgeContent] are supplied by the caller — proof's empty state and the shelf's
 * error state pass different pairs — so this component cannot adopt `.fail .mk`'s fixed `--paper`
 * fill and `--jam` ring without deleting two parameters that live call sites in another module are
 * currently passing. What it does adopt is the *material*: the badge is now a printed object, with an
 * `ink` border and a hard `inkLine` shadow under it, at the V2.1 radius scale. The V2 tint-only chip
 * — a soft colour block with no edge — is the reading V2.1 does not have.
 *
 * **`--frame` is not drawn here.** `.fail .mk` wears none, and a ring on a *shared* component would
 * put one on every screen that mounts it, which is exactly the one-per-screen property
 * [com.aritr.zinely.ui.components.zinelyV21Frame] exists to protect.
 *
 * The type moved: V2's 22sp/14.5sp pair becomes `.fail h2`'s `1.6rem` and `.fail p`'s `.94rem` over
 * `.empty p`'s `line-height:1.55` (`.fail p` declares no line-height of its own, and the two
 * paragraphs are the same paragraph). The rhythm is `.fail`'s uniform `gap:var(--gap-md)` with the
 * heading's own `margin-top:var(--gap-sm)` on top of it, which replaces V2's 14 / 6 / 18.
 */
@Composable
public fun ZStatusPane(
    title: String,
    body: String,
    badgeBackground: Color,
    badgeContent: Color,
    modifier: Modifier = Modifier,
    bodyMaxWidth: Dp = Dp.Unspecified,
    badgeIcon: @Composable (tint: Color) -> Unit,
    cta: (@Composable () -> Unit)? = null,
) {
    val colors = ZinelyTheme.v21Colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(bottom = ZinelyV21Dimens.gapMd)
                // The hard shadow paints outside the node, so it sits LEFT of the `clip`.
                .zinelyV21HardShadow(BadgeShadow, colors.inkLine, BadgeShape)
                .size(BadgeSize)
                .clip(BadgeShape)
                .background(badgeBackground)
                .border(BadgeBorder, colors.ink, BadgeShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(BadgeIconSize)) { badgeIcon(badgeContent) }
        }
        BasicText(
            text = title,
            modifier = Modifier.padding(top = ZinelyV21Dimens.gapSm),
            style = TextStyle(
                color = colors.ink,
                fontFamily = ZinelyV21Fonts.Voice,
                fontSize = TitleSize,
                fontWeight = FontWeight.Bold,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            text = body,
            modifier = Modifier
                .padding(top = ZinelyV21Dimens.gapMd, bottom = ZinelyV21Dimens.gapMd)
                .let { if (bodyMaxWidth != Dp.Unspecified) it.widthIn(max = bodyMaxWidth) else it },
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = BodySize,
                lineHeight = BodyLineHeight,
                textAlign = TextAlign.Center,
            ),
        )
        cta?.invoke()
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html`'s `.fail` / `.empty` column.
// ---------------------------------------------------------------------------------------------

/**
 * The 56dp badge is this component's own — `.fail .mk` is 60 and `.act .ic` is 30, and neither is
 * this scaffold. Only its *material* is transcribed: `--br-md` (the corpus radius nearest V2's 16,
 * and on the published scale), a 1.5px ink edge and a printed shadow.
 */
private val BadgeSize = 56.dp
private val BadgeIconSize = 26.dp
private val BadgeShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
private val BadgeBorder = 1.5.dp

/** `box-shadow:3px 3px 0` — `.fail .mk`'s depth, on `inkLine` because the badge is not the jam mark. */
private val BadgeShadow = 3.dp

/** `.fail h2{font-size:1.6rem}` = 25.6px, Averia 700. */
private val TitleSize = 25.6.sp

/** `.fail p{font-size:.94rem}` = 15.04px, over `.empty p{line-height:1.55}`. */
private val BodySize = 15.04.sp
private val BodyLineHeight = 1.55.em
