package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2IconPaint
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.toImageVector

/** Test tag on the Add chooser's row container. */
public const val BenchAddChooserTestTag: String = "bench-add-chooser"

/** Per-row test tags. */
public const val BenchAddChooserTextTag: String = "bench-add-chooser-text"
public const val BenchAddChooserPhotoTag: String = "bench-add-chooser-photo"

/** The frozen sheet title (`v2-bench.html:719`). */
public const val BenchAddChooserTitle: String = Copy.AddChooser.TITLE

/** The frozen `Text` row (`v2-bench.html:720`), title and subtitle verbatim. */
public const val BenchAddTextTitle: String = Copy.AddChooser.TEXT_TITLE
public const val BenchAddTextSubtitle: String = Copy.AddChooser.TEXT_SUBTITLE

/** The frozen `Photo` row (`v2-bench.html:721`), title and subtitle verbatim. */
public const val BenchAddPhotoTitle: String = Copy.AddChooser.PHOTO_TITLE
public const val BenchAddPhotoSubtitle: String = Copy.AddChooser.PHOTO_SUBTITLE

/** Frozen `.supply{gap:8px}` (`v2-bench.html:316`). */
internal val BenchOptGap = 8.dp

/** Frozen `.supply .opt{padding:13px 12px;border-radius:13px;gap:13px}` (`v2-bench.html:317`). */
internal val BenchOptPaddingH = 12.dp
internal val BenchOptPaddingV = 13.dp
internal val BenchOptRadius = 13.dp
internal val BenchOptGapInner = 13.dp

/** Frozen `.supply .opt .ico{width:38px;height:38px;border-radius:10px}` (`v2-bench.html:319`). */
internal val BenchOptIcoSize = 38.dp
internal val BenchOptIcoRadius = 10.dp

/** Frozen `.supply .opt .ico svg{width:19px;stroke-width:1.7}` (`v2-bench.html:320`). */
internal val BenchOptGlyphSize = 19.dp
internal const val BenchOptStroke: Float = 1.7f

/** Frozen `.supply .opt .tx b{font-size:14.5px;font-weight:600}` (`v2-bench.html:321`). */
internal val BenchOptTitleSize = 14.5.sp

/** Frozen `.supply .opt .tx span{font-size:12px;color:var(--ink-soft)}` (`v2-bench.html:321`). */
internal val BenchOptSubtitleSize = 12.sp

/**
 * The frozen **Add chooser** — `openSupply()`'s sheet (`v2-bench.html:718-724`), with two of its three rows;
 * [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094) rows 4.4a–4.4d.
 *
 * ### Why two rows and not three
 *
 * The freeze narrates its own intent at `v2-bench.html:729` — *"Add stays three verbs — Text · Photo ·
 * Art."* [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling) released **only Text
 * and Photo** into C4 and left `Art` where OD-2 put it, behind C8 and behind
 * [V2-BENCH-REVIEW §E.6](../../../../../../../../docs/design/V2-BENCH-REVIEW.md)'s legal pass. The ruling's
 * own words for this are *"a fence reassignment, not a capability reassignment"*: the frozen markup still
 * has three rows, and what changed is which package may build which one.
 *
 * The `Art` row is therefore **absent, not disabled**. Drawing it inert would have precedent —
 * [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling)'s *drawn and invents nothing* —
 * but C3's own Pass 2 produced this programme's sharpest sentence against exactly that pattern: a control
 * that reports truthfully and then does nothing when tapped invites the press harder than a blank one does.
 * A row that does not exist yet promises nothing; C8 adds it.
 *
 * ### Why the sheet is [ZSheet] and not a new surface
 *
 * OD-21 says so in terms — *"the chooser continues to use the existing `ZSheet` implementation"* — and the
 * shipped component already carries the frozen sheet's shape: the 22dp top radius, the grip, the serif
 * title, the `translateY` entrance and the scrim. Six production call sites depend on it. Building a second
 * one would be the duplication this phase keeps removing.
 *
 * ### Both destinations are the shipped ones
 *
 * `Text` dispatches through [addTextAndEdit], which places the box **and opens the C3 session on it** — the
 * freeze says the same thing in its own narration at `:720` (*"a new text block drops in ready to edit"*),
 * so the frozen design and the shipped flow already agreed before this package existed. OD-21 requires that
 * reuse **by name**, precisely so C3's in-place editing model survives C4 untouched. `Photo` dispatches
 * `Intent.RequestAddImage`, the same intent the retired shelf sent to the same picker.
 *
 * @param visible whether the sheet is open.
 */
@Composable
internal fun BenchAddChooser(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddText: () -> Unit,
    onAddPhoto: () -> Unit,
) {
    ZSheet(visible = visible, onDismiss = onDismiss, title = BenchAddChooserTitle) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BenchAddChooserTestTag),
            verticalArrangement = Arrangement.spacedBy(BenchOptGap),
        ) {
            BenchAddOption(
                icon = ZinelyV2Icons.Font.toImageVector(
                    BenchOptGlyphSize,
                    ZinelyV2IconPaint.Stroke(BenchOptStroke),
                ),
                title = BenchAddTextTitle,
                subtitle = BenchAddTextSubtitle,
                testTag = BenchAddChooserTextTag,
                onClick = { onDismiss(); onAddText() },
            )
            BenchAddOption(
                icon = ZinelyV2Icons.Replace.toImageVector(
                    BenchOptGlyphSize,
                    ZinelyV2IconPaint.Stroke(BenchOptStroke),
                ),
                title = BenchAddPhotoTitle,
                subtitle = BenchAddPhotoSubtitle,
                testTag = BenchAddChooserPhotoTag,
                onClick = { onDismiss(); onAddPhoto() },
            )
        }
    }
}

/**
 * Frozen `.supply .opt` — a hairline-outlined row with a matcha-tinted glyph tile and a two-line label.
 *
 * The row is one accessibility node, not three: the title, the subtitle and the tile are one control, and
 * TalkBack should offer one target reading *"Text. Type words onto the page."* rather than three fragments
 * a user has to reassemble.
 */
@Composable
private fun BenchAddOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v2Colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BenchOptRadius))
            .border(1.dp, colors.chromeLine, RoundedCornerShape(BenchOptRadius))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .clearAndSetSemantics {
                contentDescription = Copy.AddChooser.optionLabel(title, subtitle)
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(horizontal = BenchOptPaddingH, vertical = BenchOptPaddingV),
        horizontalArrangement = Arrangement.spacedBy(BenchOptGapInner),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BenchOptIcoSize)
                .clip(RoundedCornerShape(BenchOptIcoRadius))
                .background(colors.matchaTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.matchaText,
                modifier = Modifier.size(BenchOptGlyphSize),
            )
        }
        Column {
            Text(
                text = title,
                color = colors.ink,
                fontSize = BenchOptTitleSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = ZinelyTheme.v2Typography.work,
            )
            Text(
                text = subtitle,
                color = colors.inkSoft,
                fontSize = BenchOptSubtitleSize,
                fontFamily = ZinelyTheme.v2Typography.work,
            )
        }
    }
}
