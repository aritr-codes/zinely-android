package com.aritr.zinely.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyV2Shadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import kotlin.math.roundToInt

/**
 * The five things the frozen sheet offers, in the frozen order.
 *
 * The labels and the glyphs are the design's own bytes, which is why they live on the enum rather than
 * at the draw site: a row's icon is not a decoration chosen by the renderer, it is part of what the
 * design says that row *is*. All five survive the re-freeze unchanged, glyphs included.
 *
 * ### ⚠️ Delete is now separated **only** by colour
 *
 * V2's `.act.danger` traded the 1px hairline every other row carried for `border-top:8px solid
 * var(--desk)` — a band of the desk showing through the sheet, so the destructive row sat visibly
 * apart. V2.1's `.act` writes `border:none` and the danger row differs by ink alone (`jam-text`, and a
 * `berry-tint` icon chip instead of `butter-tint`). Transcribed as frozen, and **recorded**: separation
 * by position is a stronger guard against a mis-tap than separation by hue, and colour alone is the
 * weaker signal for anyone who cannot distinguish the two tints.
 *
 * @property label the row's spoken and printed text, verbatim from the frozen markup.
 * @property glyph the frozen `.ic` character. Three of these six codepoints (counting the shelf's `⋯`)
 *   are absent from the bundled Inter, so the device's own fallback font draws them — **D-021**.
 * @property danger `.act.danger` — the consequence ink and the berry icon chip.
 */
internal enum class ZineAction(
    val label: String,
    val glyph: String,
    val danger: Boolean = false,
) {
    Open("Open on the bench", "↗"),
    ShareExport("Share & export", "⇪"),
    Rename("Rename", "✎"),
    Duplicate("Duplicate", "⧉"),
    Delete("Delete", "⌫", danger = true),
}

/**
 * Which zine the sheet is open for, and the metadata line it discloses.
 *
 * **This is where the shelf's withheld metadata surfaces — and in V2.1 only half of it is withheld.**
 * V2's shelf showed no subtitle at all. V2.1's splits it: the paper size becomes the cover's postmark
 * and the date becomes `.sub` under the name ([ZineOnShelf]). The sheet is still the only place the two
 * appear **together**, as one line, which is what `data-sub` is.
 *
 * @property title the zine's own name — `data-name`, and the sheet header's `.sh-ttl`.
 * @property subtitle `data-sub`, verbatim: format and recency, `"A4 · 2 days ago"`.
 */
internal data class ZineActionTarget(
    val title: String,
    val subtitle: String,
)

/** The scrim behind an open sheet — `.scrim`. */
internal const val ZineActionScrimTestTag: String = "zine-action-scrim"

/** The sheet surface itself — `.sheet`. */
internal const val ZineActionSheetTestTag: String = "zine-action-sheet"

/** One action row, addressable by the action it performs. */
internal fun zineActionTestTag(action: ZineAction): String = "zine-action-${action.name}"

/** `.sh-head{border-bottom:1.5px dashed var(--hair)}` — the sheet's one divider. */
internal const val ZineActionHeadDividerTestTag: String = "zine-action-head-divider"

/** `.grab` — the handle. */
internal const val ZineActionGrabTestTag: String = "zine-action-grab"

/** `.sh-ttl` and `.sh-sub`, for the type assertions to find. */
internal const val ZineActionTitleTestTag: String = "zine-action-title"
internal const val ZineActionSubtitleTestTag: String = "zine-action-subtitle"

/** The spoken name of the sheet — the frozen `aria-label` on `role="dialog"`. */
internal const val ZineActionSheetPaneTitle: String = "Zine actions"

/**
 * The frozen Library's action sheet — `docs/design/mockups/v21-library.html`.
 *
 * ```css
 * .sheet{position:absolute;left:0;right:0;bottom:0;background:var(--paper);
 *   border-radius:var(--br-xl) var(--br-xl) 0 0;border-top:2px solid var(--ink);
 *   transform:translateY(103%);transition:transform .26s cubic-bezier(.05,.7,.1,1);
 *   padding:0 0 var(--gap-xl);box-shadow:0 -16px 40px -18px var(--soft-shadow)}
 * ```
 *
 * ### It is a bottom sheet again, and it was not one in V2
 *
 * V2's sheet was a **floating card** inset 10px from three edges with all four corners rounded at 20px.
 * V2.1's is edge to edge, bottom-anchored, two rounded top corners at `--br-xl`, with a 2px ink rule
 * along its top edge — the ordinary Android shape, arrived at from the other direction. The travel
 * shortened to match (`115%` → `103%`), because a sheet that already touches the bottom edge needs less
 * clearance to be fully off screen.
 *
 * ### Hosted in a `Dialog`, for the three things the CSS cannot say
 *
 * `role="dialog" aria-modal="true"` is a *behaviour*: the content behind is unreachable, focus is
 * contained, and Escape closes. Android's equivalents are window modality, focus containment and system
 * back — all three of which a [Dialog] provides and an in-composition `Box` would each re-implement.
 * [ADR-049](docs/DECISIONS.md#adr-049) settled exactly this trade for V1's sheets.
 *
 * **Still not M3's `ModalBottomSheet`**, and V2.1 makes that a closer call than V2 did: this sheet now
 * has a `.grab` handle, which is the affordance M3's component is built around. It has no
 * drag-to-dismiss, though — the frozen script wires the scrim and Escape and nothing else — so adopting
 * M3 would import a gesture the design does not specify along with the handle it does. The handle is
 * drawn; the drag is not invented.
 *
 * ### The two motion values are the ruling's, not the file's
 *
 * `.26s` and `.22s` are transcribed; the curves are **not**. **D-011**'s ruling is that the canonical
 * pair governs, and its table assigns this sheet **settle** (*"a surface coming to rest"*) and the scrim
 * **standard** (*"pure opacity"*). Reduced motion collapses both to zero.
 *
 * ### Selecting an action does not close the sheet
 *
 * The frozen `.act` buttons carry no handler beyond the prototype's `close()`. What follows *Rename* is
 * undesigned here, and every one of the five leads somewhere this component does not own. It reports the
 * choice and holds still.
 *
 * @param target the zine whose actions are showing, or `null` for a closed sheet.
 * @param onAction one of the five was chosen. Fires exactly once, and does not dismiss.
 * @param onDismiss the scrim was tapped or the system back was pressed.
 */
@Composable
internal fun ZineActionSheet(
    target: ZineActionTarget?,
    onAction: (ZineAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept mounted through the exit so the sheet slides out rather than blinking away.
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = target != null
    if (!shown.currentState && !shown.targetState) return

    // The last non-null target, latched, so the header keeps its text for the length of the exit slide
    // instead of blanking one frame into it.
    var latched by remember { mutableStateOf<ZineActionTarget?>(null) }
    if (target != null) latched = target
    val drawn = latched ?: return

    val motion = ZinelyTheme.v2Motion

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // The frozen scrim is drawn below; the window's own dim would stack a second one on top of it.
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }

        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visibleState = shown,
                enter = fadeIn(motion.standard(ScrimDurationMillis)),
                exit = fadeOut(motion.standard(ScrimDurationMillis)),
            ) {
                ZineActionScrim(onDismiss = onDismiss)
            }
            AnimatedVisibility(
                visibleState = shown,
                modifier = Modifier.align(Alignment.BottomCenter),
                // `transform:translateY(103%)` — of the sheet's own height.
                enter = slideInVertically(motion.settle(SheetDurationMillis)) {
                    (it * SheetSlide).roundToInt()
                },
                exit = slideOutVertically(motion.settle(SheetDurationMillis)) {
                    (it * SheetSlide).roundToInt()
                },
            ) {
                ZineActionSheetSurface(
                    target = drawn,
                    onAction = onAction,
                    modifier = modifier,
                )
            }
        }
    }
}

/**
 * The `.scrim` — the dimming, and the tap that closes.
 *
 * Split out for the same reason [ZineActionSheetSurface] is, and for one more: it is the **only** place
 * the scrim's fill is painted. A test that built its own `Box` from the fill would agree with a constant
 * while the sheet painted something else.
 *
 * ### ⚠️ The fill is a literal, and V2.1 has no scrim token
 *
 * `rgba(38,26,16,.42)` is written outside `:root`, exactly as V2's `rgba(30,25,18,.36)` was — so the
 * `prefers-color-scheme` block cannot reach it and the dark sheet dims exactly as much as the light one,
 * over a desk that is already near that colour. For V2 the owner ruled the corpus token authoritative
 * ([D-022](docs/design/V2-SPEC-DEFECTS.md)); V2.1's palette **has no scrim token to rule onto** — it was
 * not among the 25 the gate measured. So the literal is transcribed, and this is recorded as the same
 * defect recurring rather than as a value that was chosen.
 */
@Composable
internal fun ZineActionScrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .testTag(ZineActionScrimTestTag)
            .fillMaxSize()
            .background(ScrimFill)
            // `scrim.onclick = close`.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )
}

/**
 * The sheet's own body, without the window that makes it modal.
 *
 * Split out for the reason V1's `ZSheetSurface` is: a [Dialog] lives in its own window, and the
 * decor-view raster the golden harness captures cannot see it. Parity rasters compose this directly.
 *
 * **D-021 — the five icons are text, and three of the six frozen glyphs are not in the bundled family.**
 * `.ic` is a styled `<span>` holding a literal character, so the faithful transcription is text rather
 * than geometry. Measured against the app's own fonts: `↗`, `⇪` and `⌫` are in Inter; **`✎`, `⧉` and the
 * shelf's `⋯` are not**, so the platform's fallback draws them. V2.1 changes only their setting — each
 * now sits in a 30dp tinted chip rather than floating in a 20dp slot.
 */
@Composable
internal fun ZineActionSheetSurface(
    target: ZineActionTarget,
    onAction: (ZineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors

    Column(
        modifier
            .testTag(ZineActionSheetTestTag)
            .fillMaxWidth()
            // `box-shadow:0 -16px 40px -18px var(--soft-shadow)` — upward, which is the one shadow in
            // the corpus that is not the hard offset. A sheet rising off the screen is the exception
            // §5.1 allows: it is not a printed object resting on the desk, it is chrome above it.
            .zinelyV2Shadow(
                listOf(
                    ZinelyV2ShadowLayer(
                        dy = (-16).dp,
                        blur = 40.dp,
                        spread = (-18).dp,
                        color = colors.softShadow,
                    ),
                ),
                SheetShape,
            )
            .clip(SheetShape)
            .background(colors.paper)
            // `border-top:2px solid var(--ink)` — only the top edge, so a `border` would be wrong: it
            // would ring all four sides, three of which are off screen but two of which are on screen for
            // the sheet's whole height.
            //
            // **And it follows the corners.** A straight `drawLine` across the top is what this was, and
            // the `clip` above cuts it at both rounded corners — leaving `radiusXl` of curve on each side
            // where paper meets the scrim with no rule at all, which is exactly where the sheet's edge is
            // most visible against a dim background. CSS has no such gap: a `border-top` on an element
            // with `border-radius` is drawn along the arc. So the path below *is* the top edge — arc, line,
            // arc — stroked at double width, with the clip cutting the outer half. That is an inside
            // stroke of exactly [SheetTopRule] with no radius arithmetic, the same trick the loading
            // placeholder uses for its dashed border.
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
            // The safe area first, then `padding:0 0 var(--gap-xl)` on top of it — outermost-first, so the
            // frozen 24dp is measured from the top of the navigation bar rather than from the screen edge
            // underneath it.
            //
            // **Device Pass 1 found this, and it was the destructive row that paid.** A browser has no
            // gesture bar, so the frozen file writes no `env(safe-area-inset-bottom)` here — and a sheet
            // pinned to `bottom:0` on a device with three-button navigation puts its last row *behind* the
            // navigation bar. Measured on `SM-A176B` (Android 16): `Delete` occupied `[0,2114]-[1080,2277]`
            // against a bar starting at ~2235, so roughly a quarter of the row — the one row whose misfires
            // are unrecoverable — was under the system's own targets. The other two rows of the same defect
            // class are the shelf's and the dock's, cleared by `zineDockClearance` and by the dock's own
            // consuming pad.
            .windowInsetsPadding(
                WindowInsets.navigationBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom),
            )
            .padding(bottom = ZinelyV21Dimens.gapXl)
            // `role="dialog" aria-label="Zine actions"`. The Dialog window carries the modality; this
            // carries the name TalkBack announces on entering it.
            .semantics { paneTitle = ZineActionSheetPaneTitle },
    ) {
        ZineActionGrab()

        // `.sh-head{padding:var(--gap-xs) var(--gap-xl) var(--gap-md)}`
        Column(Modifier.padding(HeadPadding)) {
            Text(
                text = target.title,
                modifier = Modifier.testTag(ZineActionTitleTestTag),
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Voice,
                    fontWeight = FontWeight.Bold,
                    fontSize = TitleSize,
                    lineHeight = TitleLineHeight,
                    color = colors.ink,
                ),
            )
            Text(
                text = target.subtitle,
                modifier = Modifier
                    .testTag(ZineActionSubtitleTestTag)
                    .padding(top = ZinelyV21Dimens.gapHair),
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.Medium,
                    fontSize = SubtitleSize,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.inkSoft,
                    // `tnum`, and the freeze does not ask for it: no `font-variant-numeric` appears
                    // anywhere in `v21-library.html`. Added because this line is the one place a *changing*
                    // number is re-read under a header that stays put ("2 days ago" → "12 days ago"), and
                    // proportional digits make it twitch. An addition to the corpus, logged as one.
                    fontFeatureSettings = TabularNumerals,
                ),
            )
        }

        // `border-bottom:1.5px dashed var(--hair)` on the head — the sheet's ONLY divider. V2 drew one
        // above every row and an 8px desk band above Delete; V2.1's `.act{border:none}` deletes both.
        Box(
            Modifier
                .testTag(ZineActionHeadDividerTestTag)
                .fillMaxWidth()
                .height(HeadDivider)
                .drawBehind {
                    drawLine(
                        color = colors.hair,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(HeadDividerDash.toPx(), HeadDividerDash.toPx()),
                        ),
                    )
                },
        )

        ZineAction.entries.forEach { action ->
            ActionRow(action = action, onAction = onAction)
        }
    }
}

/**
 * `.grab{width:44px;height:5px;border-radius:var(--br-pill);background:var(--ink-faint);
 * margin:var(--gap-md) auto var(--gap-xs);opacity:.5}`
 *
 * A handle the sheet does not use: there is no drag-to-dismiss in the frozen script. It is drawn because
 * the design draws it, and it is **silent** because announcing "handle" to a reader that cannot drag it
 * would name an affordance that is not there. The dismissal paths this sheet really has — the scrim and
 * system back — are both reachable without it.
 */
@Composable
private fun ZineActionGrab() {
    Box(
        Modifier
            .testTag(ZineActionGrabTestTag)
            .fillMaxWidth()
            .padding(top = ZinelyV21Dimens.gapMd, bottom = ZinelyV21Dimens.gapXs),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = GrabWidth, height = GrabHeight)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
                .background(ZinelyTheme.v21Colors.inkFaint.copy(alpha = GrabOpacity)),
        )
    }
}

/**
 * One `.act` row.
 *
 * ```css
 * .act{display:flex;align-items:center;gap:var(--gap-lg);padding:var(--gap-lg) var(--gap-xl);
 *      font-size:1rem;font-weight:500;color:var(--ink);border:none}
 * .act .ic{width:30px;height:30px;border-radius:var(--br-sm);background:var(--butter-tint);
 *          color:var(--ink-soft);font-size:.95rem}
 * .act:active{background:var(--leaf-tint)}
 * .act.danger{color:var(--jam-text)} .act.danger .ic{background:var(--berry-tint);color:var(--jam-text)}
 * ```
 *
 * **`:active`, not `:hover`.** V2's rows washed on hover — a stylus/mouse state that is no state at all
 * under a finger, transcribed because the design stated it. V2.1 states the press instead, which is the
 * state a touch device actually has. The wash is therefore visible on every device rather than on none.
 */
@Composable
private fun ActionRow(action: ZineAction, onAction: (ZineAction) -> Unit) {
    val colors = ZinelyTheme.v21Colors

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = if (action.danger) colors.jamText else colors.ink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(zineActionTestTag(action))
            .zinelyV2Control(
                label = action.label,
                interactionSource = interaction,
                onClick = { onAction(action) },
            )
            .background(if (pressed) colors.leafTint else Color.Transparent)
            .padding(
                horizontal = ZinelyV21Dimens.gapXl,
                vertical = ZinelyV21Dimens.gapLg,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg),
    ) {
        Box(
            Modifier
                .size(IconChip)
                .clip(RoundedCornerShape(ZinelyV21Dimens.radiusSm))
                .background(if (action.danger) colors.berryTint else colors.butterTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = action.glyph,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = IconSize,
                    // `.act .ic` declares no `line-height` — inherited, as everywhere else.
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = if (action.danger) colors.jamText else colors.inkSoft,
                ),
            )
        }
        Text(
            text = action.label,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.Medium,
                fontSize = RowTextSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = ink,
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html`. Spacing is on the published scale; the
// sizes below are ratios and stay literal.
// ---------------------------------------------------------------------------------------------

/** `border-radius:var(--br-xl) var(--br-xl) 0 0` — two corners, because the card touches three edges. */
private val SheetShape: Shape = RoundedCornerShape(
    topStart = ZinelyV21Dimens.radiusXl,
    topEnd = ZinelyV21Dimens.radiusXl,
    bottomEnd = 0.dp,
    bottomStart = 0.dp,
)

/** `border-top:2px solid var(--ink)`. */
private val SheetTopRule = 2.dp

/**
 * `.scrim{background:rgba(38,26,16,.42)}` — a literal, and V2.1 publishes no scrim token to prefer over
 * it. See [ZineActionScrim] for why that is recorded rather than quietly tokenised.
 */
private val ScrimFill = Color(0x6B261A10)

/** `transform:translateY(103%)` of the sheet's own height. */
private const val SheetSlide = 1.03f

/** `transition:transform .26s` — on **settle** per the D-011 ruling, not the file's own curve. */
private const val SheetDurationMillis = 260

/** `transition:opacity .22s` on the scrim — **standard**, per the same ruling's table. */
private const val ScrimDurationMillis = 220

/** `.grab{width:44px;height:5px;opacity:.5}` */
private val GrabWidth = 44.dp
private val GrabHeight = 5.dp
private const val GrabOpacity = 0.5f

/** `.sh-head{padding:var(--gap-xs) var(--gap-xl) var(--gap-md)}` */
private val HeadPadding = PaddingValues(
    start = ZinelyV21Dimens.gapXl,
    top = ZinelyV21Dimens.gapXs,
    end = ZinelyV21Dimens.gapXl,
    bottom = ZinelyV21Dimens.gapMd,
)

/** `border-bottom:1.5px dashed var(--hair)`; the dash rhythm is a browser default, approximated. */
private val HeadDivider = 1.5.dp
private val HeadDividerDash = 3.dp

/** `.sh-ttl{font-size:1.22rem;line-height:1.15}` — 19.52px, unrounded. */
private val TitleSize = 19.52.sp
private val TitleLineHeight = 22.448.sp

/** `.sh-sub{font-size:.78rem;font-weight:500;margin-top:var(--gap-hair)}` */
private val SubtitleSize = 12.48.sp
private const val TabularNumerals = "tnum"

/** `.act{font-size:1rem;font-weight:500}` */
private val RowTextSize = 16.sp

/** `.act .ic{width:30px;height:30px;font-size:.95rem}` = 15.2px. */
private val IconChip = 30.dp
private val IconSize = 15.2.sp
