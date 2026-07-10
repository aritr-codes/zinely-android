package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.components.ZPaperSurface
import com.aritr.zinely.ui.components.zinelyShadow
import com.aritr.zinely.ui.components.ZPrimaryButton
import com.aritr.zinely.ui.components.ZPrimaryButtonMetrics
import com.aritr.zinely.ui.components.ZPrimaryFill
import com.aritr.zinely.ui.components.ZStatusPane
import com.aritr.zinely.ui.components.ZToolButton
import com.aritr.zinely.ui.components.ZToolButtonMetrics
import com.aritr.zinely.ui.components.zinelySweep
import com.aritr.zinely.ui.theme.ZinelyEasing
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.delay

/** Test tags on the chrome the pre-reskin shelf had no equivalent of. */
internal const val ShelfToolsTestTag: String = "shelf-tools"
internal const val ShelfSearchFieldTestTag: String = "shelf-search-field"
internal const val ShelfSortButtonTestTag: String = "shelf-sort-button"
internal const val ShelfHeadCountTestTag: String = "shelf-head-count"
internal const val ShelfStartButtonTestTag: String = "shelf-start-button"
internal const val ShelfErrorStateTestTag: String = "shelf-error-state"
internal const val ShelfRetryButtonTestTag: String = "shelf-retry-button"
internal const val ShelfSearchMissTestTag: String = "shelf-search-miss"

/**
 * How many objects a shelf must hold before it earns search and sort.
 *
 * The frozen prototype gates the tools on a hand-flipped `state==="many"` switch rather than a
 * count, so the HTML fixes no number — it only brackets one: its `few` shelf holds 5 and shows no
 * tools, its `many` shelf holds 21 and shows them. Seven is the owner's decision and sits inside
 * that bracket, so nothing in the spec contradicts it. The principle the spec *does* state is the
 * one that matters: **curate, don't accumulate** — a shelf you can see all of needs no search.
 */
internal const val SHELF_TOOLS_THRESHOLD: Int = 7

/**
 * `.appbar` — the wordmark and the promise, on a gradient that lets the shelf scroll under it.
 *
 * The gradient is the spec's `linear-gradient(var(--desk) 78%, transparent)`: solid desk for most of
 * the bar's height, then a short fade. It is not a scrim and not a shadow; a sheet passing beneath
 * dissolves rather than sliding under an edge.
 */
@Composable
internal fun ShelfAppBar(modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0.78f to colors.desk, 1f to Color.Transparent))
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        ShelfWordmark()
        ShelfPrivacyPill("On this device")
    }
}

/** `.wordmark` — the product's name in its own voice, and the coral full stop it ends on. */
@Composable
private fun ShelfWordmark() {
    val colors = ZinelyTheme.colors
    BasicText(
        text = buildAnnotatedString {
            append("Zinely")
            withStyle(SpanStyle(color = colors.coral)) { append(".") }
        },
        modifier = Modifier.semantics { contentDescription = "Zinely" },
        style = TextStyle(
            color = colors.onDesk,
            fontFamily = ZinelyTheme.typography.voice,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp,
            letterSpacing = (-0.01).em,
        ),
    )
}

/**
 * `.privacy` — the pill that states the invariant. A live teal dot, not an icon of a lock: nothing
 * is being defended, because nothing ever leaves.
 */
@Composable
internal fun ShelfPrivacyPill(text: String, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, colors.shelfLine, CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(colors.teal))
        BasicText(
            text = text,
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 12.5.sp,
            ),
        )
    }
}

/**
 * `.tools` — search and sort, shown only once the shelf passes [SHELF_TOOLS_THRESHOLD].
 *
 * The search pill is shelf-local: it is not `ZTextField` (that is the rename field's voice, a serif
 * at 17sp), and the spec gives it its own radius and shell face. The sort button is the shared
 * `.sortbtn` tool.
 */
@Composable
internal fun ShelfTools(
    query: String,
    onQueryChange: (String) -> Unit,
    sortLabel: String,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ShelfToolsTestTag)
            .padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShelfSearchField(query, onQueryChange, Modifier.weight(1f))
        ZToolButton(
            onClick = onSortClick,
            metrics = ZToolButtonMetrics.ShelfSort,
            modifier = Modifier.testTag(ShelfSortButtonTestTag),
            text = sortLabel,
            icon = { SortGlyph(it) },
        )
    }
}

/** `.search` — a field pill whose border warms to coral while it holds focus. */
@Composable
private fun ShelfSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier) {
    val colors = ZinelyTheme.colors
    val type = ZinelyTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val textStyle = TextStyle(color = colors.onDesk, fontFamily = type.shell, fontSize = 15.sp)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .testTag(ShelfSearchFieldTestTag)
            .semantics { contentDescription = "Search your zines" },
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.coralStrong),
        singleLine = true,
        interactionSource = interaction,
        decorationBox = { field ->
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(colors.field)
                    // `.search:focus-within{ border-color:var(--coral-strong) }`
                    .border(1.dp, if (focused) colors.coralStrong else colors.fieldEdge, shape)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(17.dp)) { SearchGlyph(colors.onDeskFaint) }
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BasicText("Search your zines", style = textStyle.copy(color = colors.onDeskFaint))
                    }
                    field()
                }
            }
        },
    )
}

/** `.shelf-head` — the section title and how many objects stand under it. */
@Composable
internal fun ShelfHead(count: Int, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            text = "Your zines",
            style = TextStyle(
                color = colors.onDesk,
                fontFamily = ZinelyTheme.typography.voice,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = "$count",
            modifier = Modifier.testTag(ShelfHeadCountTestTag),
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 13.sp,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

/**
 * `.dock` — the one coral action, parked in the thumb zone.
 *
 * `pointer-events:none` on the dock and `auto` on the button: the gradient is a fade the shelf
 * scrolls through, not a bar that swallows taps. On a wide surface the button moves to the trailing
 * edge, where a hand actually rests.
 *
 * This is the *product* dock. The spec's `.proto` panel is review scaffolding — it says so — and is
 * not implemented.
 */
@Composable
internal fun ShelfDock(onStart: () -> Unit, wide: Boolean, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.42f to colors.desk))
            .padding(
                start = 20.dp,
                end = if (wide) 28.dp else 20.dp,
                top = 14.dp,
                bottom = 16.dp,
            ),
        contentAlignment = if (wide) Alignment.CenterEnd else Alignment.Center,
    ) {
        ZPrimaryButton(
            text = "Start a zine",
            onClick = onStart,
            metrics = ZPrimaryButtonMetrics.Shelf,
            modifier = Modifier.testTag(ShelfStartButtonTestTag),
            icon = { PlusGlyph(it) },
        )
    }
}

/**
 * `.skeleton` — six covers on the second paper stock, swept, holding the shelf's shape while the
 * store answers.
 *
 * The spec's skeleton is `<div class="zine"><div class="cover"></div></div>`, so each placeholder is
 * a real object: it inherits `.zine::after`'s ledge, the `nth-child` tilt, and `.cover::before`'s
 * spine. Only the *contents* are hidden (`.skeleton .cover *{visibility:hidden}`) — pseudo-elements
 * survive that rule. The shelf's shape is what is being held, and a rank of flat, plumb rectangles
 * would not be it.
 *
 * `.skeleton .cover` restates `box-shadow:var(--shadow-2)` **without** the `0 2px 0 --paper-edge`
 * hard edge the loaded cover carries, so this does not go through [ZPaperSurface] — that component
 * always appends the edge, correctly, for every sheet that has actually arrived.
 */
@Composable
internal fun ShelfLoadingSkeleton(columns: Int, roomy: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeLoadingTestTag)
            .clearAndSetSemantics { contentDescription = "Loading your zines" }
            .padding(
                horizontal = if (roomy) 28.dp else 20.dp,
                vertical = if (roomy) 22.dp else 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(if (roomy) 34.dp else 26.dp),
    ) {
        val rows = (SKELETON_COVERS + columns - 1) / columns
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(if (roomy) 24.dp else 18.dp)) {
                repeat(columns) { column ->
                    val slot = row * columns + column
                    Box(Modifier.weight(1f)) {
                        if (slot < SKELETON_COVERS) SkeletonCover(slot)
                    }
                }
            }
        }
    }
}

/** One `.zine > .cover` with nothing in it: ledge, tilt, spine, sweep. */
@Composable
private fun SkeletonCover(index: Int) {
    val colors = ZinelyTheme.colors
    val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 3.dp)
    Box(Modifier.shelfLedge().padding(bottom = 16.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .graphicsLayer {
                    rotationZ = shelfTilt(index)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .zinelyShadow(ZinelyTheme.elevation.shadow2, shape)
                .clip(shape)
                .background(colors.paper2)
                // `.cover::before` — the bound edge, which `visibility:hidden` never touched.
                .drawBehind {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.14f), Color.Transparent),
                            endX = 7.dp.toPx(),
                        ),
                        size = Size(7.dp.toPx(), size.height),
                    )
                }
                .zinelySweep(),
        )
    }
}

/** `Array.from({length:6})` — six is what fills a phone's first screen without overpromising. */
private const val SKELETON_COVERS = 6

/**
 * `.empty` — the invitation. Not "no results": a sheet of paper that scores its own fold, then
 * ghosts in the eight panels it is about to become. The empty shelf teaches the product.
 */
@Composable
internal fun ShelfEmptyState(modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeEmptyStateTestTag)
            .padding(start = 30.dp, end = 30.dp, top = 40.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SheetHint()
        BasicText(
            text = HomeEmptyHeadline,
            style = TextStyle(
                color = colors.onDesk,
                fontFamily = ZinelyTheme.typography.voice,
                fontSize = emptyHeadlineSize(),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            text = "One sheet of paper, printed at home and folded by hand into a small book. " +
                "Start one and the bench will teach you the rest.",
            modifier = Modifier.padding(top = 8.dp).widthIn(max = 300.dp),
            style = TextStyle(
                color = colors.onDeskSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 15.5.sp,
                lineHeight = 15.5.sp * 1.5f,
                textAlign = TextAlign.Center,
            ),
        )
        ShelfPrivacyPill(
            text = "Kept on this device — no account, nothing uploaded",
            modifier = Modifier.padding(top = 22.dp),
        )
    }
}

/** `clamp(23px, 6vw, 30px)`. */
@Composable
private fun emptyHeadlineSize(): TextUnit =
    (0.06f * LocalConfiguration.current.screenWidthDp).coerceIn(23f, 30f).sp

/**
 * `.sheet-hint` — a 132px sheet that scores its fold at 340ms and ghosts its eight panels at 1000ms.
 *
 * Two animations, both `both`-filled, so the sheet is blank until its moment. Under reduced motion
 * they do not play: the sheet is simply already folded and already panelled, which is the frozen
 * spec's own rule (`transition-duration:.001ms`) — arrive instantly, still arrive.
 */
@Composable
private fun SheetHint() {
    val colors = ZinelyTheme.colors
    val reduceMotion = ZinelyTheme.motion.reduceMotion
    val score = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val ghost = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            score.snapTo(1f)
            ghost.snapTo(1f)
        }
    }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            delay(340)
            score.animateTo(1f, tween(820, easing = ZinelyEasing))
        }
    }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            delay(1_000)
            ghost.animateTo(1f, tween(700, easing = ZinelyEasing))
        }
    }

    ZPaperSurface(
        modifier = Modifier
            .padding(bottom = 26.dp)
            .width(132.dp)
            .aspectRatio(3f / 4f)
            .graphicsLayer { rotationZ = -1.4f },
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 3.dp),
        // `.sheet-hint` has no `::before` — this sheet is unbound, a loose page waiting to be folded.
        boundEdgeWidth = 0.dp,
    ) {
        // `.starter` — the first block of colour someone will place, already on the page.
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = colors.coral,
                topLeft = Offset(18.dp.toPx(), 20.dp.toPx()),
                size = Size(30.dp.toPx(), 30.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx()),
                alpha = 0.16f,
                blendMode = BlendMode.Multiply,
            )
        }
        // `.panels` — the eight faces one folded sheet becomes: quarters across, halves down.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = ghost.value * 0.24f }
                .drawBehind {
                    val hairline = 1.dp.toPx()
                    for (i in 1..3) {
                        drawRect(
                            color = colors.inkFaint,
                            topLeft = Offset(0f, i * size.height / 4f - hairline),
                            size = Size(size.width, hairline),
                        )
                    }
                    drawRect(
                        color = colors.inkFaint,
                        topLeft = Offset(size.width / 2f - hairline / 2f, 0f),
                        size = Size(hairline, size.height),
                    )
                },
        )
        // `.sheet-hint::before` — the fold, scoring itself downward from the top.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleY = score.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    alpha = 0.55f
                }
                .drawBehind {
                    val hairline = 1.dp.toPx()
                    val x = size.width / 2f - hairline / 2f
                    val top = 0.12f * size.height
                    val bottom = 0.88f * size.height
                    val dash = 4.dp.toPx()
                    val period = 9.dp.toPx()
                    var y = top
                    while (y < bottom) {
                        drawRect(
                            color = colors.inkFaint,
                            topLeft = Offset(x, y),
                            size = Size(hairline, minOf(dash, bottom - y)),
                        )
                        y += period
                    }
                },
        )
    }
}

/**
 * `.errorstate` — visible and recoverable. The shelf failed to open; the zines did not vanish, and
 * the copy says so. The retry button is `.start`'s shape wearing the stamp ink: the same weight of
 * action, without claiming to be the primary one.
 *
 * The dock is hidden on this screen (`dock.classList.toggle("hide", err)`) — there is no starting a
 * new zine on a shelf that cannot be read — which is the caller's business, not this pane's.
 */
@Composable
internal fun ShelfErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ShelfErrorStateTestTag)
            .padding(start = 30.dp, end = 30.dp, top = 40.dp, bottom = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        ZStatusPane(
            title = "Couldn't open your shelf",
            body = "Your zines are safe on this device — we just couldn't read them this time.",
            // `.errorstate .badge{ background:rgba(198,78,52,.14) }` — coral-strong at 14%.
            badgeBackground = colors.coralStrong.copy(alpha = 0.14f),
            badgeContent = colors.coralText,
            badgeIcon = { ErrorGlyph(it) },
        ) {
            ZPrimaryButton(
                text = "Try again",
                onClick = onRetry,
                metrics = ZPrimaryButtonMetrics.Shelf,
                modifier = Modifier.testTag(ShelfRetryButtonTestTag),
                fill = ZPrimaryFill.Stamp,
                shadow = ZinelyTheme.elevation.shadow2,
            )
        }
    }
}

/** The search that found nothing. One quiet line where the objects would be — never an error. */
@Composable
internal fun ShelfSearchMiss(modifier: Modifier = Modifier) {
    BasicText(
        text = "Nothing here by that name.",
        modifier = modifier
            .fillMaxWidth()
            .testTag(ShelfSearchMissTestTag)
            .padding(vertical = 30.dp),
        style = TextStyle(
            color = ZinelyTheme.colors.onDeskSoft,
            fontFamily = ZinelyTheme.typography.shell,
            fontSize = 14.5.sp,
            textAlign = TextAlign.Center,
        ),
    )
}
