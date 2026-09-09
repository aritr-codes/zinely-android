package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.ui.components.ZPrimaryButton
import com.aritr.zinely.ui.components.ZPrimaryButtonMetrics
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.ZStampButton
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

internal const val ImageSpreadSheetTestTag = "image-spread-sheet"
internal const val ImageSpreadConfirmTestTag = "image-spread-confirm"

internal fun imageSpreadPageNumbers(pageIndex: Int): Pair<Int, Int>? = when (pageIndex) {
    0, 7 -> 8 to 1
    1, 2 -> 2 to 3
    3, 4 -> 4 to 5
    5, 6 -> 6 to 7
    else -> null
}

internal enum class SpreadInnerEdge { LEFT, RIGHT }

/** Recognise the ordinary-image pair written by ADR-109 so the advisory cue stops marking its join. */
internal fun imageSpreadInnerEdge(
    pages: List<Page>,
    pageIndex: Int,
    selected: ImageElement?,
    pageSizePt: PtSize,
): SpreadInnerEdge? {
    if (selected == null || selected.fit != Fit.FIT || !selected.transform.isFullPage(pageSizePt)) return null
    val (partnerIndex, sourceIsLeft) = when (pageIndex) {
        0 -> 7 to false
        1 -> 2 to true
        2 -> 1 to false
        3 -> 4 to true
        4 -> 3 to false
        5 -> 6 to true
        6 -> 5 to false
        7 -> 0 to true
        else -> return null
    }
    val partner = pages.getOrNull(partnerIndex)?.elements?.filterIsInstance<ImageElement>()?.firstOrNull { candidate ->
        candidate.assetId == selected.assetId && candidate.fit == Fit.FIT &&
            candidate.transform.isFullPage(pageSizePt) &&
            if (sourceIsLeft) selected.crop.right.near(0.5) && candidate.crop.left.near(0.5)
            else selected.crop.left.near(0.5) && candidate.crop.right.near(0.5)
    } ?: return null
    if (!selected.crop.top.near(partner.crop.top) || !selected.crop.bottom.near(partner.crop.bottom)) return null
    return if (sourceIsLeft) SpreadInnerEdge.RIGHT else SpreadInnerEdge.LEFT
}

private fun com.aritr.zinely.core.model.Transform.isFullPage(pageSizePt: PtSize): Boolean =
    xPt.near(0.0) && yPt.near(0.0) && widthPt.near(pageSizePt.width) &&
        heightPt.near(pageSizePt.height) && rotationDegrees.near(0.0)

private fun Double.near(other: Double): Boolean = kotlin.math.abs(this - other) <= 1e-9

/** Frozen Bench A19: the required explanation before a one-page editor writes a two-page result. */
@Composable
internal fun ImageSpreadSheet(
    visible: Boolean,
    leftPageNumber: Int,
    rightPageNumber: Int,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = Copy.Spread.title(leftPageNumber, rightPageNumber),
        modifier = Modifier.testTag(ImageSpreadSheetTestTag),
    ) {
        Text(
            text = Copy.Spread.KEEPS_CONTENT,
            color = colors.inkSoft,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ZinelyV21Dimens.gapMd)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                .border(1.5.dp, colors.ink, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                .background(colors.ink),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(leftPageNumber, rightPageNumber).forEach { page ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                        .background(colors.paper),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Page $page",
                        color = colors.onButter,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            text = Copy.Spread.FOLD_WARNING,
            color = colors.inkSoft,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ZinelyV21Dimens.gapMd),
            horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZStampButton(text = Copy.Spread.CANCEL, onClick = onDismiss)
            ZPrimaryButton(
                text = Copy.Spread.CONFIRM,
                onClick = onConfirm,
                metrics = ZPrimaryButtonMetrics.Bench,
                modifier = Modifier.testTag(ImageSpreadConfirmTestTag),
                enabled = !busy,
            )
        }
    }
}
