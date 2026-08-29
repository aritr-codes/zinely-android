package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.core.render.SupplyOutline
import com.aritr.zinely.render.android.SupplyPainter
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A16's searchable, deterministic Art cabinet. */
public const val BenchArtSheetTestTag: String = "bench-art-sheet"
public const val BenchArtSearchTestTag: String = "bench-art-search"
public const val BenchArtResultCountTestTag: String = "bench-art-result-count"
public const val BenchArtNoResultsTestTag: String = "bench-art-no-results"
public const val BenchArtShowAllTestTag: String = "bench-art-show-all"

public fun benchArtTileTestTag(supplyId: String): String = "bench-art-tile-$supplyId"
public fun benchArtRecentTileTestTag(supplyId: String): String = "bench-art-recent-tile-$supplyId"
public fun benchArtFavouriteTileTestTag(supplyId: String): String = "bench-art-favourite-tile-$supplyId"
public fun benchArtFavouriteTestTag(supplyId: String): String = "bench-art-favourite-$supplyId"
public fun benchArtLabelTestTag(family: String): String = "bench-art-label-$family"
public fun benchArtFamilyFilterTestTag(family: String): String = "bench-art-filter-$family"
public fun benchArtFamilyCueTestTag(family: String): String = "bench-art-filter-cue-$family"

public const val BenchArtSheetTitle: String = Copy.BenchArt.TITLE
public const val BenchArtRecentHeading: String = Copy.BenchArt.RECENT

internal const val BenchArtGridColumns: Int = 3
internal val BenchArtGridGap = ZinelyV21Dimens.gapMd
internal val BenchArtLabelSpaceAbove = ZinelyV21Dimens.gapMd
internal val BenchArtLabelSpaceBelow = ZinelyV21Dimens.gapSm
internal val BenchArtLabelSize = 9.92.sp
internal val BenchArtLabelTracking = 0.12.em
internal val BenchArtTileShape: RoundedCornerShape = RoundedCornerShape(ZinelyV21Dimens.radiusSm)
internal val BenchArtTileBorder = 1.5.dp
internal val BenchArtGlyphSize = 46.dp
internal val BenchArtCardHeight = 134.dp
internal val BenchArtRailCardWidth = 104.dp
internal val BenchArtTilt = floatArrayOf(-0.55f, 0.45f, -0.35f, 0.35f)

internal fun benchArtFamilyIndex(supplyId: String): Int =
    Copy.Supplies.BY_FAMILY.entries.indexOfFirst { supplyId in it.value }.also {
        require(it >= 0) { "unknown_art_family:$supplyId" }
    }

/**
 * Production Art sheet for ADR-107 / A16.
 *
 * Search is local and deterministic. A family filter narrows the same stable catalogue order and toggles
 * off when tapped again. The favourite star is a sibling of the placement target, never an interactive
 * child, so both actions remain valid 48dp controls. Recents and favourites are session conveniences at
 * this seam; durable preference storage can be hoisted without changing the cabinet or renderer contract.
 */
@Composable
internal fun BenchArtSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (supplyId: String) -> Unit,
    recent: List<String> = emptyList(),
) {
    var sessionRecent by rememberSaveable { mutableStateOf(recent.distinct().take(6)) }
    var favourites by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val supplyPainter = remember { SupplyPainter() }

    LaunchedEffect(supplyPainter) {
        // Warm the immutable catalogue paths off the UI thread so the first Art opening is not paying for
        // outline-to-Path construction while the sheet animates in.
        withContext(Dispatchers.Default) {
            supplyPainter.prewarm(SupplyCatalog.OUTLINES.values)
        }
    }

    ZSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = BenchArtSheetTitle,
        modifier = Modifier.fillMaxHeight(0.92f),
    ) {
        BenchArtSheetBody(
            onPick = { id ->
                sessionRecent = (listOf(id) + sessionRecent.filterNot { it == id }).take(6)
                onPick(id)
            },
            recent = sessionRecent,
            favourites = favourites.toSet(),
            onFavouriteChange = { id, favourite ->
                favourites = if (favourite) (favourites + id).distinct() else favourites.filterNot { it == id }
            },
            supplyPainter = supplyPainter,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun BenchArtSheetBody(
    onPick: (supplyId: String) -> Unit,
    recent: List<String> = emptyList(),
    favourites: Set<String> = emptySet(),
    onFavouriteChange: (supplyId: String, favourite: Boolean) -> Unit = { _, _ -> },
    supplyPainter: SupplyPainter? = null,
    modifier: Modifier = Modifier,
) {
    val sharedSupplyPainter = supplyPainter ?: remember { SupplyPainter() }
    val colors = ZinelyTheme.v21Colors
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFamily by rememberSaveable { mutableStateOf<String?>(null) }
    val matches = Copy.Supplies.matchingIds(query, selectedFamily)
    val narrowed = query.isNotBlank() || selectedFamily != null

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        modifier = modifier
            .fillMaxWidth()
            .testTag(BenchArtSheetTestTag),
    ) {
        item(key = "search") {
            BenchArtSearch(value = query, onValueChange = { query = it })
        }
        item(key = "family-filters") {
            BenchArtFamilyFilters(
                selectedFamily = selectedFamily,
                onSelect = { family -> selectedFamily = if (selectedFamily == family) null else family },
            )
        }
        item(key = "result-count") {
            Text(
                text = Copy.BenchArt.resultCount(matches.size),
                color = ZinelyTheme.v21Colors.inkSoft,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = 12.sp,
                modifier = Modifier.testTag(BenchArtResultCountTestTag).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (matches.isEmpty()) {
            item(key = "no-results") {
                BenchArtNoResults(onShowAll = { query = ""; selectedFamily = null })
            }
        } else if (narrowed) {
            Copy.Supplies.BY_FAMILY.forEach { (family, supplies) ->
                val familyMatches = supplies.keys.filter(matches::contains)
                if (familyMatches.isNotEmpty()) {
                    item(key = "label-$family") { BenchArtLabel(family, collapsed = false) }
                    items(
                        items = familyMatches.chunked(BenchArtGridColumns),
                        key = { row -> "filtered-$family-${row.first()}" },
                    ) { row ->
                        BenchArtGridRow(
                            supplyIds = row,
                            favourites = favourites,
                            onPick = onPick,
                            onFavouriteChange = onFavouriteChange,
                            tag = ::benchArtTileTestTag,
                            supplyPainter = sharedSupplyPainter,
                        )
                    }
                }
            }
        } else {
            if (recent.isNotEmpty()) {
                item(key = "label-recent") { BenchArtLabel(Copy.BenchArt.RECENT, collapsed = true) }
                item(key = "rail-recent") {
                    BenchArtRail(
                        supplyIds = recent,
                        favourites = favourites,
                        onPick = onPick,
                        onFavouriteChange = onFavouriteChange,
                        tag = ::benchArtRecentTileTestTag,
                        supplyPainter = sharedSupplyPainter,
                    )
                }
            }
            item(key = "label-favourites") {
                BenchArtLabel(Copy.BenchArt.FAVOURITES, collapsed = recent.isEmpty())
            }
            if (favourites.isEmpty()) {
                item(key = "favourites-empty") {
                    Text(
                        text = Copy.BenchArt.FAVOURITES_EMPTY,
                        color = colors.ink,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                            .background(colors.butterTint)
                            .drawBehind {
                                val stroke = 1.5.dp.toPx()
                                drawRoundRect(
                                    color = colors.ink,
                                    cornerRadius = CornerRadius(ZinelyV21Dimens.radiusMd.toPx()),
                                    style = Stroke(
                                        width = stroke,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
                                    ),
                                )
                            }
                            .padding(ZinelyV21Dimens.gapMd),
                    )
                }
            } else {
                val favouriteIds = Copy.Supplies.NAMES.keys.filter(favourites::contains)
                item(key = "rail-favourites") {
                    BenchArtRail(
                        supplyIds = favouriteIds,
                        favourites = favourites,
                        onPick = onPick,
                        onFavouriteChange = onFavouriteChange,
                        tag = ::benchArtFavouriteTileTestTag,
                        supplyPainter = sharedSupplyPainter,
                    )
                }
            }
            Copy.Supplies.BY_FAMILY.forEach { (family, supplies) ->
                item(key = "label-$family") { BenchArtLabel(family, collapsed = false) }
                items(
                    items = supplies.keys.chunked(BenchArtGridColumns),
                    key = { row -> "catalogue-$family-${row.first()}" },
                ) { row ->
                    BenchArtGridRow(
                        supplyIds = row,
                        favourites = favourites,
                        onPick = onPick,
                        onFavouriteChange = onFavouriteChange,
                        tag = ::benchArtTileTestTag,
                        supplyPainter = sharedSupplyPainter,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchArtSearch(value: String, onValueChange: (String) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = Copy.BenchArt.FIND_A_PIECE,
            color = colors.ink,
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(ArtPaperInk),
            textStyle = TextStyle(color = ArtPaperInk, fontFamily = ZinelyV21Fonts.Work, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = Copy.BenchArt.FIND_A_PIECE }
                .testTag(BenchArtSearchTestTag),
            decorationBox = { field ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(ZinelyV21Dimens.radiusLg))
                        .background(colors.paper)
                        .border(1.5.dp, colors.ink, RoundedCornerShape(ZinelyV21Dimens.radiusLg))
                        .padding(horizontal = ZinelyV21Dimens.gapMd),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = Copy.BenchArt.SEARCH_HINT,
                            color = ArtPaperInkSoft,
                            fontFamily = ZinelyV21Fonts.Work,
                            fontSize = 14.sp,
                        )
                    }
                    field()
                }
            },
        )
    }
}

@Composable
private fun BenchArtFamilyFilters(selectedFamily: String?, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        Copy.Supplies.BY_FAMILY.keys.forEach { family ->
            val isSelected = selectedFamily == family
            val colors = ZinelyTheme.v21Colors
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colors.leaf else colors.surface)
                    .border(1.5.dp, colors.ink, RoundedCornerShape(50))
                    .clickable(interactionSource = interaction, indication = null) { onSelect(family) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp)
                    .testTag(benchArtFamilyFilterTestTag(family))
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = family
                        selected = isSelected
                        onClick { onSelect(family); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = family,
                    color = if (isSelected) colors.onLeaf else colors.ink,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                if (isSelected) {
                    EditorSelectionCue(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .testTag(benchArtFamilyCueTestTag(family)),
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchArtNoResults(onShowAll: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag(BenchArtNoResultsTestTag),
    ) {
        Text(
            text = Copy.BenchArt.NO_RESULTS,
            color = colors.ink,
            fontFamily = ZinelyV21Fonts.Voice,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
        )
        Text(
            text = Copy.BenchArt.NO_RESULTS_HINT,
            color = colors.inkSoft,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = Copy.BenchArt.SHOW_ALL,
            color = colors.onLeaf,
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colors.leaf)
                .clickable(onClick = onShowAll)
                .heightIn(min = 48.dp)
                .padding(horizontal = 18.dp)
                .testTag(BenchArtShowAllTestTag)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = Copy.BenchArt.SHOW_ALL
                    onClick { onShowAll(); true }
                },
        )
    }
}

@Composable
private fun BenchArtLabel(text: String, collapsed: Boolean) {
    Text(
        text = text.uppercase(),
        color = ZinelyTheme.v21Colors.inkSoft,
        fontSize = BenchArtLabelSize,
        fontWeight = FontWeight.Bold,
        fontFamily = ZinelyV21Fonts.Work,
        letterSpacing = BenchArtLabelTracking,
        lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        modifier = Modifier
            .padding(top = if (collapsed) 0.dp else BenchArtLabelSpaceAbove, bottom = BenchArtLabelSpaceBelow)
            .testTag(benchArtLabelTestTag(text))
            .semantics { contentDescription = text; heading() },
    )
}

@Composable
private fun BenchArtGridRow(
    supplyIds: List<String>,
    favourites: Set<String>,
    onPick: (String) -> Unit,
    onFavouriteChange: (String, Boolean) -> Unit,
    tag: (String) -> String,
    supplyPainter: SupplyPainter,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BenchArtGridGap),
        modifier = Modifier.fillMaxWidth(),
    ) {
        supplyIds.forEach { id ->
            BenchArtTile(
                supplyId = id,
                favourite = id in favourites,
                onPick = onPick,
                onFavouriteChange = onFavouriteChange,
                testTag = tag(id),
                supplyPainter = supplyPainter,
                modifier = Modifier.weight(1f),
            )
        }
        repeat(BenchArtGridColumns - supplyIds.size) { Box(Modifier.weight(1f)) }
    }
}

@Composable
private fun BenchArtRail(
    supplyIds: List<String>,
    favourites: Set<String>,
    onPick: (String) -> Unit,
    onFavouriteChange: (String, Boolean) -> Unit,
    tag: (String) -> String,
    supplyPainter: SupplyPainter,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BenchArtGridGap),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        supplyIds.distinct().filter(Copy.Supplies.NAMES::containsKey).forEach { id ->
            BenchArtTile(
                supplyId = id,
                favourite = id in favourites,
                onPick = onPick,
                onFavouriteChange = onFavouriteChange,
                testTag = tag(id),
                supplyPainter = supplyPainter,
                modifier = Modifier.width(BenchArtRailCardWidth),
            )
        }
    }
}

/** Named material card: placement and favourite are separate sibling controls. */
@Composable
private fun BenchArtTile(
    supplyId: String,
    favourite: Boolean,
    onPick: (String) -> Unit,
    onFavouriteChange: (String, Boolean) -> Unit,
    testTag: String,
    supplyPainter: SupplyPainter,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val family = benchArtFamilyIndex(supplyId)
    val ground = when (family) {
        0 -> colors.leafTint
        1 -> colors.berryTint
        2 -> colors.paper
        else -> colors.butter
    }
    val mark = if (family == 3) colors.onButter else colors.onLeaf
    val outline = requireNotNull(SupplyCatalog.outlineOf(supplyId)) { "missing_art_outline:$supplyId" }
    val name = requireNotNull(Copy.Supplies.NAMES[supplyId]) { "missing_art_name:$supplyId" }
    val pick = benchTap { onPick(supplyId) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Column(
        modifier = modifier
            .height(BenchArtCardHeight)
            .graphicsLayer { rotationZ = BenchArtTilt[family] }
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, BenchArtTileShape)
            .clip(BenchArtTileShape)
            .background(ground)
            .border(BenchArtTileBorder, colors.ink, BenchArtTileShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(interactionSource = interaction, indication = null, onClick = pick)
                .testTag(testTag)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = name
                    onClick { pick(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            BenchArtMark(outline = outline, color = mark, supplyPainter = supplyPainter)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(colors.paper),
        ) {
            Text(
                text = name.replaceFirst(' ', '\n'),
                color = colors.onLeaf,
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp, end = 38.dp, top = 4.dp, bottom = 4.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp)
                    .clickable { onFavouriteChange(supplyId, !favourite) }
                    .testTag(benchArtFavouriteTestTag(supplyId))
                    .clearAndSetSemantics {
                        role = Role.Checkbox
                        contentDescription = if (favourite) Copy.BenchArt.removeFavourite(name) else Copy.BenchArt.addFavourite(name)
                        toggleableState = ToggleableState(favourite)
                        onClick { onFavouriteChange(supplyId, !favourite); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (favourite) "★" else "☆",
                    color = if (favourite) colors.leafText else colors.onLeaf,
                    fontSize = 20.sp,
                )
            }
        }
    }
}

/** The authored outline itself, using the same Android painter seam as page rendering. */
@Composable
private fun BenchArtMark(outline: SupplyOutline, color: Color, supplyPainter: SupplyPainter) {
    val argb = color.toArgb()
    Canvas(modifier = Modifier.size(BenchArtGlyphSize)) {
        drawIntoCanvas { canvas ->
            supplyPainter.drawUnitSquare(canvas.nativeCanvas, outline, argb, size.width, size.height)
        }
    }
}

internal sealed interface BenchArtPurpose {
    data object Place : BenchArtPurpose
    data class Replace(val id: String) : BenchArtPurpose
}

private val ArtPaperInk = Color(0xFF27270F)
private val ArtPaperInkSoft = Color(0xFF6A452F)
