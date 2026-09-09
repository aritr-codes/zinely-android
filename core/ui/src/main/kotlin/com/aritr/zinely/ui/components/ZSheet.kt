package com.aritr.zinely.ui.components

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritr.zinely.ui.theme.ZinelyV21Scrim
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import kotlin.math.roundToInt

/** Stable test tags (parity-plan M1: stable test tags preserved/introduced). */
public const val ZSheetScrimTestTag: String = "zSheetScrim"
public const val ZSheetSurfaceTestTag: String = "zSheetSurface"
public const val ZSheetCloseTestTag: String = "zSheetClose"

/**
 * The frozen `.dhead` close button — its action and the words a screen reader says for it.
 *
 * One parameter rather than two, so "a close button with no label" and "a label with no button" are
 * unrepresentable. The label is passed in because `:core:ui` does not depend on `:core:copy`: user-facing
 * strings live there ([ADR-060](../../../../../../../docs/DECISIONS.md#adr-060)), and a design-system
 * component that hardcoded English would be the exception that ends that rule.
 */
public data class ZSheetClose(val label: String, val onClose: () -> Unit)

/**
 * The frozen modal system — `.scrim` + `.sheet` — shared by all eight sheets of the trilogy.
 *
 * Deliberately NOT Material3's ModalBottomSheet (ADR-049): the frozen sheet has **no
 * drag-to-dismiss** (the grip is decorative — zero pointer handlers in the spec), and M3's sheet
 * imposes its own motion. Hosting in a plain [Dialog] instead gives window-level modality (focus
 * containment ≙ the spec's `inert`), back-dismiss ≙ Escape, and TalkBack isolation for free; the
 * scrim fade and `translateY(103%)` slide are ours, driven by the frozen `--base` motion token.
 *
 * Dismissal paths: scrim tap, system back, and — where [close] is supplied — a visible close button
 * beside the title.
 *
 * **Why [close] is opt-in rather than always drawn.** The frozen trilogy draws a `.dclose` on its two
 * full-height *drawers* and on none of its short chooser sheets, and that is a real distinction rather
 * than an inconsistency: a chooser is a list you tap an answer from, its scrim is a hand's width away,
 * and the whole surface is on screen at once. A drawer covers most of the display with content that
 * scrolls, and nothing in it names a way out. So the sheets that need the affordance ask for it, and
 * the ones that would only gain a second dismissal path do not.
 *
 * ### V2.1 — ADR-102 P8
 *
 * This is the one `:core:ui` symbol the Bench, the Library and the Proof all draw, so its re-skin
 * lands on three surfaces at once ([ADR-102 §5](../../../../../../../docs/DECISIONS.md#adr-102-coreui),
 * [§10.2](../../../../../../../docs/DECISIONS.md#adr-102-open)). The three frozen files write the same
 * object under three names — `v21-bench.html .sheet`, `v21-library.html .sheet`, `v21-proof.html
 * .drawer` — and where they disagree the value taken is recorded at the constant.
 *
 * @param close draws the frozen `.dhead` close button. Null (default) draws none.
 * @param onShown runs after the enter animation has attached and settled the dialog content. Use it
 * for deterministic initial focus.
 * @param onHidden runs after the exit animation releases the dialog window. Use it for focus return,
 * never as the user's dismiss action.
 */
@Composable
public fun ZSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    close: ZSheetClose? = null,
    onShown: (() -> Unit)? = null,
    onHidden: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    var wasShown by remember { mutableStateOf(false) }
    var shownNotified by remember { mutableStateOf(false) }
    SideEffect { if (visible) wasShown = true }
    visibleState.targetState = visible
    LaunchedEffect(visibleState.isIdle, visibleState.currentState, visibleState.targetState) {
        if (!shownNotified && visibleState.isIdle && visibleState.currentState && visibleState.targetState) {
            shownNotified = true
            onShown?.invoke()
        }
        if (wasShown && visibleState.isIdle && !visibleState.currentState && !visibleState.targetState) {
            wasShown = false
            shownNotified = false
            onHidden?.invoke()
        }
    }
    if (!visibleState.currentState && !visibleState.targetState) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // The spec's scrim is ours (`.scrim`, animated); kill the window's own dim so they don't stack.
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }

        val motion = ZinelyTheme.motion
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(motion.base()),
                exit = fadeOut(motion.base()),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(ZSheetScrimTestTag)
                        .background(ScrimFill)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                )
            }
            AnimatedVisibility(
                visibleState = visibleState,
                modifier = Modifier.align(Alignment.BottomCenter),
                // transform:translateY(103%) -> none. V2 wrote 102%; all three V2.1 files write 103%.
                enter = slideInVertically(motion.base()) { (it * SheetSlide).roundToInt() },
                exit = slideOutVertically(motion.base()) { (it * SheetSlide).roundToInt() },
            ) {
                ZSheetSurface(
                    title = title,
                    sub = sub,
                    modifier = modifier,
                    close = close,
                    content = content,
                )
            }
        }
    }
}

/**
 * The sheet body without the Dialog window — split out so goldens can rasterize it in a plain host
 * (a Dialog's window is invisible to the decor-view capture harness; pre-M1 review, Required Fix 4).
 *
 * Visibility widened `internal → public` for C2 (CI-34) as the mechanical consequence of the
 * `:core:ui` split: [ZComponentGoldenTest] rasterizes this surface but is structurally pinned in
 * `:feature:editor` (its shared `rasterizeToBitmap` golden host lives there), so it must reach this
 * across the module boundary. [VisibleForTesting] marks it as a test-visibility exposure, not a new
 * design-system entry point.
 */
@VisibleForTesting
@Composable
public fun ZSheetSurface(
    title: String,
    sub: String?,
    modifier: Modifier = Modifier,
    close: ZSheetClose? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    Column(
        modifier = modifier
            .testTag(ZSheetSurfaceTestTag)
            .widthIn(max = SheetMaxWidth)
            .fillMaxWidth()
            // `box-shadow:0 -16px 40px -18px var(--soft-shadow)` — the one blurred shadow V2.1 keeps,
            // and the one exception §5.1 allows: a sheet rising off the screen is chrome *above* the
            // desk, not a printed object resting on it. Transcribed exactly as the Library's already
            // converted `ZineActionSheet` transcribes it, so the two shells cannot drift.
            .zinelyV2Shadow(
                listOf(
                    ZinelyV2ShadowLayer(
                        dy = -SheetShadowRise,
                        blur = SheetShadowBlur,
                        spread = -SheetShadowSpread,
                        color = colors.softShadow,
                    ),
                ),
                SheetShape,
            )
            .clip(SheetShape)
            .background(colors.surface)
            // `border-top:2px solid var(--ink)` — the top edge only, and it follows the two corners.
            // A straight `drawLine` is cut by the `clip` at each arc, leaving `radiusXl` of curve with
            // no rule on it exactly where the sheet meets the scrim. So the path IS the top edge — arc,
            // line, arc — stroked at double width with the clip taking the outer half, which is an
            // inside stroke of [SheetTopRule] with no radius arithmetic.
            .drawBehind {
                val w = SheetTopRule.toPx()
                val r = ZinelyV21Dimens.radiusXl.toPx()
                val edge = Path().apply {
                    moveTo(0f, r)
                    arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, forceMoveTo = false)
                    lineTo(size.width - r, 0f)
                    arcTo(Rect(size.width - 2 * r, 0f, size.width, 2 * r), 270f, 90f, forceMoveTo = false)
                }
                drawPath(edge, color = colors.ink, style = Stroke(width = 2 * w))
            }
            // `padding:0 var(--gap-lg) var(--gap-xl)` — plus the safe area, and plus the IME for the
            // sheets that carry fields. Insets outermost, so the frozen 24dp is measured from the top
            // of the navigation bar rather than from a screen edge underneath it.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(
                start = ZinelyV21Dimens.gapLg,
                end = ZinelyV21Dimens.gapLg,
                bottom = ZinelyV21Dimens.gapXl,
            )
            // D-087: make the whole sheet a pointer hit target so inert titles, gaps and padding cannot
            // fall through to the scrim sibling. Observe without consuming: descendant controls and
            // scrolling bodies still receive the same gesture stream.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            }
            .semantics { paneTitle = title },
    ) {
        // `.grip`/`.grab{width:44px;height:5px;border-radius:var(--br-pill);background:var(--ink-faint);
        //  margin:var(--gap-md) auto var(--gap-xs);opacity:.5}` — byte-identical in all three files
        // apart from the Proof's bottom margin. It is decorative: no pointer handler exists in any of
        // the frozen scripts, so nothing here announces a drag that is not implemented.
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ZinelyV21Dimens.gapMd, bottom = ZinelyV21Dimens.gapXs)
                .width(GripWidth)
                .height(GripHeight)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
                .background(colors.inkFaint.copy(alpha = GripOpacity)),
        )
        // `.sheet h3` / `.dhead h3` (voice 1.2rem/700) and, where asked for, the close button.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = ZinelyV21Dimens.gapHair,
                        end = ZinelyV21Dimens.gapHair,
                        top = ZinelyV21Dimens.gapXs,
                        // `margin:var(--gap-xs) var(--gap-hair) var(--gap-md)`. Where a `.sh-sub`
                        // follows, that subtitle carries the block's bottom margin instead — the
                        // Library nests both inside one `.sh-head` whose padding is the same 12.
                        bottom = if (sub == null) ZinelyV21Dimens.gapMd else 0.dp,
                    ),
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = ZinelyV21Fonts.Voice,
                    fontSize = TitleSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
            if (close != null) ZSheetCloseButton(close)
        }
        // `.sh-sub{font-size:.78rem;color:var(--ink-soft);margin-top:var(--gap-hair);font-weight:500}`.
        if (sub != null) {
            BasicText(
                text = sub,
                modifier = Modifier.padding(
                    start = ZinelyV21Dimens.gapHair,
                    end = ZinelyV21Dimens.gapHair,
                    top = ZinelyV21Dimens.gapHair,
                    bottom = ZinelyV21Dimens.gapMd,
                ),
                style = TextStyle(
                    color = colors.inkSoft,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = SubSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
        }
        content()
    }
}

/**
 * The frozen `.dclose`: a **34×34 outlined pill**, `1.5px solid var(--ink)` on `--paper`, with a 15px
 * mark in `--ink-soft` and a `2px 2px 0 var(--ink-line)` printed shadow.
 *
 * It is not [ZIconButton], and the P3 review is why. That component is 44×44 and is the app's generic
 * top-bar icon button — a visibly different object from the 34px pill the spec draws here. The first
 * draft used it anyway and the ADR discussed only the parameter shape, which is precisely the
 * pixel-parity gap the handbook says must be fixed or explicitly accepted. It is fixed.
 *
 * The one deliberate departure: the frozen 34px box is below the 48dp touch minimum, so the pill is
 * *drawn* at 34dp inside a 48dp target. Visual parity and a reachable control are not in tension — only
 * the two sizes were ever conflated.
 *
 * **Press tier: [ZinelyV21Press.Flat]** (2 → 0). `.dclose` declares no `:active` of its own, but its
 * resting `2px 2px 0` and its whole rule body are byte-identical to `.iconbtn`, which the tier table
 * assigns to Flat. Taken from the class it copies rather than interpolated from its depth.
 */
@Composable
private fun ZSheetCloseButton(close: ZSheetClose) {
    val colors = ZinelyTheme.v21Colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pill = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    Box(
        modifier = Modifier
            .size(CloseTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = close.onClose,
            )
            .semantics { contentDescription = close.label }
            .testTag(ZSheetCloseTestTag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                // The focus ring and the printed shadow both paint outside the node, so both sit to
                // the LEFT of the `clip` — see [zinelyV21Pressable]'s chain contract.
                .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
                .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, pill)
                .size(CloseSize)
                .clip(pill)
                .background(colors.surface)
                .border(CloseBorder, colors.ink, pill),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(CloseGlyph)) { ZSheetCloseGlyph(colors.inkSoft) }
        }
    }
}

/** The frozen `.dclose` mark: `M6 6l12 12M18 6L6 18` on a 24-unit viewport, `stroke-width:2.2`, round caps. */
@Composable
private fun ZSheetCloseGlyph(tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val u = size.minDimension / 24f
        val w = CloseStrokeUnits * u
        drawLine(tint, Offset(6f * u, 6f * u), Offset(18f * u, 18f * u), w, cap = StrokeCap.Round)
        drawLine(tint, Offset(18f * u, 6f * u), Offset(6f * u, 18f * u), w, cap = StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values. Where the three files disagree the majority is taken and the minority named.
// ---------------------------------------------------------------------------------------------

/**
 * `.scrim{background:rgba(39,39,15,.44)}` — the shared 37596-derived ink wash. It is intentionally one
 * value in both themes and is published as [ZinelyV21Scrim] for every modal surface.
 */
private val ScrimFill = ZinelyV21Scrim

/** `transform:translateY(103%)` of the sheet's own height — V2 wrote 102%. */
private const val SheetSlide = 1.03f

/** `border-radius:var(--br-xl) var(--br-xl) 0 0` — two corners, because the sheet touches three edges. */
private val SheetShape: Shape = RoundedCornerShape(
    topStart = ZinelyV21Dimens.radiusXl,
    topEnd = ZinelyV21Dimens.radiusXl,
    bottomEnd = 0.dp,
    bottomStart = 0.dp,
)

/** `border-top:2px solid var(--ink)`. */
private val SheetTopRule = 2.dp

/**
 * `box-shadow:0 -16px 40px -18px var(--soft-shadow)` — the Library's `.sheet` and the Proof's
 * `.drawer`. The Bench's `.sheet` declares no shadow at all; the majority carries it, and a shell
 * without it reads as pasted onto the scrim.
 */
private val SheetShadowRise = 16.dp
private val SheetShadowBlur = 40.dp
private val SheetShadowSpread = 18.dp

/** Not in the frozen files (a phone-width prototype); kept from V1 so a tablet does not stretch the sheet. */
private val SheetMaxWidth = 520.dp

/** `.grip`/`.grab{width:44px;height:5px;opacity:.5}` — V2's was 38×4 on `--field-edge`. */
private val GripWidth = 44.dp
private val GripHeight = 5.dp
private const val GripOpacity = 0.5f

/**
 * `.sheet h3` / `.dhead h3{font-size:1.2rem}` = 19.2px. The Library's `.sh-ttl` is `1.22rem`; two
 * files against one, and V1's 19sp was neither.
 */
private val TitleSize = 19.2.sp

/** `.sh-sub{font-size:.78rem}` = 12.48px. */
private val SubSize = 12.48.sp

/** `.dclose{width:34px;height:34px;border:1.5px}`, drawn inside the 48dp touch minimum. */
private val CloseTouchTarget = 48.dp
private val CloseSize = 34.dp
private val CloseBorder = 1.5.dp

/** `.dclose svg{width:15px;stroke-width:2.2}` on a 24-unit viewport. */
private val CloseGlyph = 15.dp
private const val CloseStrokeUnits = 2.2f
