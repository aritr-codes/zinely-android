package com.aritr.zinely.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * The shared error/empty chrome: a 56×56 r16 tinted badge, a serif (voice) 22sp heading, a soft
 * 14.5sp body, then the caller's CTA. The badge is identical in all three error states
 * (`rgba(198,78,52,.14)` + coral-text icon); proof's empty state reuses the exact scaffold with a
 * teal badge — hence [badgeBackground]/[badgeContent] params rather than a hardcoded pair.
 *
 * Positioning is divergent in the spec (shelf: flow child; bench/proof: absolute overlay) and
 * stays at call sites. Body width: the CSS caps at `32ch`/`30ch` — `ch` has no Compose analogue,
 * so callers pass [bodyMaxWidth] when the M2+ parity gate measures it; unbounded by default.
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
    val colors = ZinelyTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(bottom = 14.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(badgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(26.dp)) { badgeIcon(badgeContent) }
        }
        BasicText(
            text = title,
            style = TextStyle(
                color = colors.onDesk,
                fontFamily = ZinelyTheme.typography.voice,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
        // p{ margin:6px 0 18px; font-size:14.5px; line-height:1.5; }
        BasicText(
            text = body,
            modifier = Modifier
                .padding(top = 6.dp, bottom = 18.dp)
                .let { if (bodyMaxWidth != Dp.Unspecified) it.widthIn(max = bodyMaxWidth) else it },
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 14.5.sp,
                lineHeight = 14.5.sp * 1.5f,
                textAlign = TextAlign.Center,
            ),
        )
        cta?.invoke()
    }
}
