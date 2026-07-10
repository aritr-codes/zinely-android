package com.aritr.zinely.feature.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyShadow
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * `.shelf` — how many objects stand side by side. The frozen breakpoints, measured against the
 * **shelf's own width** rather than the window's, so the same rule holds inside a split screen or a
 * resizable window as it does on a phone.
 */
internal fun shelfColumns(width: Dp): Int = when {
    width >= 1180.dp -> 5
    width >= 820.dp -> 4
    width >= 560.dp -> 3
    else -> 2
}

/**
 * `.shelf` — the objects, standing in rows on their ledges.
 *
 * The spec loosens the grid at the 820px breakpoint (`gap:34px 24px; padding:22px 28px 8px`) — a
 * bigger surface earns more air, not more objects crammed onto it. The `.scroll` container's
 * `padding-bottom:112px` clears the dock, and belongs to the grid because the dock floats over it.
 *
 * Keyed by project id, so a rename or a re-sort moves an object rather than rebuilding it — which
 * also keeps each card's settle animation from replaying on every scroll.
 */
@Composable
internal fun ShelfGrid(
    cards: List<HomeZineCard>,
    onOpenZine: (String) -> Unit,
    onZineActions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val roomy = maxWidth >= 820.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(shelfColumns(maxWidth)),
            contentPadding = PaddingValues(
                start = if (roomy) 28.dp else 20.dp,
                end = if (roomy) 28.dp else 20.dp,
                top = if (roomy) 22.dp else 16.dp,
                bottom = 8.dp + 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (roomy) 34.dp else 26.dp),
            horizontalArrangement = Arrangement.spacedBy(if (roomy) 24.dp else 18.dp),
        ) {
            itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                ShelfCard(
                    card = card,
                    index = index,
                    onOpen = { onOpenZine(card.id) },
                    onActions = { onZineActions(card.id) },
                )
            }
        }
    }
}

/**
 * `.zine` — one object on its ledge: the printed [ShelfCover], the ledge under it, and the `⋯`
 * affordance that is a sibling of the cover rather than a child of it (the cover is `overflow:hidden`
 * and tilts; the button must do neither).
 *
 * Two ways in, as the spec has them: tap the object to open it, long-press it for its actions. The
 * `⋯` button is the third, and the only one a screen reader or a keyboard can reach — which is why
 * the spec keeps it permanently focusable and, on a touch device (`@media (hover:none)`), permanently
 * visible. Android is that device.
 *
 * The kicker carries the zine's format and the edition line its recency, per the owner's binding on
 * cover data. The spec's bold `No.&nbsp;03` stamp and the `.wip` pencil tick have no field behind
 * them and are recorded M2 deferrals, not omissions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    card: HomeZineCard,
    index: Int,
    onOpen: () -> Unit,
    onActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.colors
    val type = ZinelyTheme.typography
    val haptics = ZinelyTheme.haptics

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()

    // The settle animates the whole object, ledge included, so it wraps the ledge rather than the
    // cover; the 16dp is `.zine{padding-bottom}`, which the ledge is positioned 9dp up from.
    Box(modifier.shelfSettle(index).shelfLedge().padding(bottom = 16.dp)) {
        ShelfCover(
            recipe = remember(card.title) { shelfCoverRecipe(card.title) },
            index = index,
            modifier = Modifier
                .testTag(homeCardTestTag(card.id))
                .zinelyFocusRing(interaction, cornerRadius = 5.dp)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    // The platform's own long-press buzz is a fifth verb `ZinelyHaptics` does not
                    // own; `Boundary` below is the spec's, and it must be the only one that plays.
                    hapticFeedbackEnabled = false,
                    onClick = { haptics.perform(ZinelyHaptic.Tick); onOpen() },
                    onLongClick = { haptics.perform(ZinelyHaptic.Boundary); onActions() },
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "${card.title}, finished zine. Open on the bench."
                },
            lifted = hovered || focused,
            pressed = pressed,
        ) {
            // `<button class="cover">` is one node to a screen reader; its text runs are the printed
            // face of the object, not three things to announce after the label already said them.
            Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                Column {
                    Text(
                        text = card.formatLabel.uppercase(),
                        fontFamily = type.shell,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.16.em,
                        color = colors.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val titleSize = coverTitleSize()
                    Text(
                        text = card.title,
                        modifier = Modifier.padding(top = 6.dp),
                        fontFamily = type.voice,
                        fontSize = titleSize,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = titleSize * 1.06f,
                        letterSpacing = (-0.015).em,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = card.editedLabel,
                    // `.edition{bottom:12px}` against the cover's own `padding:15px 14px` box.
                    modifier = Modifier.align(Alignment.BottomStart).offset(y = 3.dp),
                    fontFamily = type.shell,
                    fontSize = 10.5.sp,
                    color = colors.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        MoreButton(
            title = card.title,
            onClick = onActions,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag(homeCardMenuTestTag(card.id)),
        )
    }
}

/**
 * `.more` — a 34dp glyph plate whose `::after{inset:-8px}` grows the touch target to 50dp without
 * moving the plate. Compose has no negative-inset pseudo-element, so the *target* is the composable
 * and the plate is drawn centred inside it; the offset re-seats the enlarged box on the frozen
 * `top:6px; right:6px`.
 */
@Composable
private fun MoreButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .offset(x = 2.dp, y = (-2).dp)
            .size(TOUCH_TARGET)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = "Actions for $title",
                onClick = onClick,
            )
            .semantics { contentDescription = "Actions for $title" },
        contentAlignment = Alignment.Center,
    ) {
        val plate = RoundedCornerShape(9.dp)
        Box(
            modifier = Modifier
                .zinelyFocusRing(interaction, cornerRadius = 9.dp)
                .zinelyShadow(ZinelyTheme.elevation.shadow1, plate)
                .clip(plate)
                // The spec hardcodes light `--paper` at .86; tracking the token instead keeps the
                // plate the same tint as the cover it sits on in *both* themes.
                .background(colors.paper.copy(alpha = 0.86f))
                .size(34.dp)
                .padding(8.dp),
        ) {
            MoreGlyph(colors.inkSoft)
        }
    }
}

/** `clamp(17px, 4.6vw, 22px)` — the one type size in the trilogy that scales with the viewport. */
@Composable
private fun coverTitleSize(): TextUnit {
    val vw = LocalConfiguration.current.screenWidthDp
    return (0.046f * vw).coerceIn(17f, 22f).sp
}

/** `.more::after{inset:-8px}` — 34 + 8 + 8. Comfortably past the 48dp minimum. */
private val TOUCH_TARGET = 50.dp
