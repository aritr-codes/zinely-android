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
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
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
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import kotlin.math.roundToInt

/**
 * The five things the frozen sheet offers, in the frozen order — `v2-library.html:173-177`.
 *
 * The labels and the glyphs are the design's own bytes, which is why they live on the enum rather than at
 * the draw site: a row's icon is not a decoration chosen by the renderer, it is part of what the design
 * says that row *is*.
 *
 * **Delete is separated, not merely coloured.** `.act.danger` (`:130`) trades the 1px hairline every other
 * row carries for `border-top:8px solid var(--desk)` — a band of the desk showing through the sheet, so the
 * destructive row sits visibly apart rather than at the end of a list. [danger] carries both consequences.
 *
 * @property label the row's spoken and printed text, verbatim from the frozen markup.
 * @property glyph the frozen `.ic` character. See [ZineActionSheetSurface] for **D-021** — three of these
 *   six codepoints (counting the shelf's `⋯`) are absent from the bundled Inter, so the device's own
 *   fallback font draws them.
 * @property danger `.act.danger` — the consequence ink *and* the 8px desk separator above it.
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
 * **This is where the shelf's hidden metadata surfaces, and the pair is the design's whole argument.** The
 * frozen caption states it as one sentence: *"Covers only — no metadata line … Format & date are disclosed
 * there, not stamped on every card"* (`:142-144`). So [subtitle] is drawn **only** here — a shelf that shows
 * it has answered a question the user did not ask yet, which is the failure
 * [CLAUDE.md](CLAUDE.md#product-principle-every-screen-answers-the-users-current-question) records for
 * `0.9.0-beta.1`'s Preview screen.
 *
 * @property title the zine's own name — `data-name`, and the sheet header's `.sh-ttl`.
 * @property subtitle `data-sub`, verbatim: format and recency, `"A4 · 2 days ago"`. **B3 does not compose
 *   this string and states no rule for it.** The frozen file shows five example values and defines no
 *   thresholds, so producing it belongs with the data — **B5**, exactly as V1's `HomeZineCard.editedLabel`
 *   is produced outside the card that draws it.
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

/** The separator *above* a row — `border-top`, which is a box-model band rather than a drawn line. */
internal fun zineActionSeparatorTestTag(action: ZineAction): String = "zine-separator-${action.name}"

/** `.sh-ttl` and `.sh-sub`, for the type assertions to find. */
internal const val ZineActionTitleTestTag: String = "zine-action-title"
internal const val ZineActionSubtitleTestTag: String = "zine-action-subtitle"

/** The spoken name of the sheet — the frozen `aria-label` on `role="dialog"` (`:171`). */
internal const val ZineActionSheetPaneTitle: String = "Zine actions"

/**
 * The frozen Library's action sheet — `v2-library.html:119-132`, `:171-178`.
 *
 * ```css
 * .scrim{position:absolute;inset:0;background:rgba(30,25,18,.36);opacity:0;visibility:hidden;
 *        transition:opacity .2s;z-index:5}
 * .sheet{position:absolute;left:10px;right:10px;bottom:10px;background:var(--paper);border-radius:20px;
 *        box-shadow:0 30px 60px -20px var(--shadow),0 0 0 1px var(--hair);transform:translateY(115%);
 *        transition:transform .24s cubic-bezier(.2,.8,.2,1);z-index:6;overflow:hidden}
 * ```
 *
 * A floating card inset 10px from three edges — **not** an edge-to-edge bottom sheet, and not V1's shape
 * either (`ZSheet` is full-bleed with two rounded top corners). All four corners are 20px here because the
 * card does not touch an edge.
 *
 * ### Hosted in a `Dialog`, for the three things the CSS cannot say
 *
 * `role="dialog" aria-modal="true"` (`:171`) is a *behaviour*: the content behind is unreachable, focus is
 * contained, and Escape closes. Android's equivalents are window modality, focus containment and system
 * back — all three of which a [Dialog] provides and an in-composition `Box` would each have to re-implement.
 * [ADR-049](docs/DECISIONS.md#adr-049) settled exactly this trade for V1's sheets and its reasoning carries:
 * not M3's `ModalBottomSheet`, because the frozen sheet has no drag handle and no drag-to-dismiss (the mock
 * script registers pointer handlers on the scrim only), and M3 brings both plus its own motion.
 *
 * What differs from V1 is only geometry, which is why [ZineActionSheetSurface] is a separate composable
 * rather than a reuse of `ZSheetSurface`.
 *
 * ### The two motion values are the ruling's, not the file's
 *
 * `.24s` and `.2s` are transcribed; the curves are **not**. `cubic-bezier(.2,.8,.2,1)` appears nowhere else
 * in V2 and the **D-011** ruling is that the Library's easings reflect its earlier freeze — the canonical
 * pair governs, and that entry's own table assigns this sheet **settle** (*"a surface coming to rest"*) and
 * the scrim **standard** (*"pure opacity"*). Reduced motion collapses both to zero, per
 * [com.aritr.zinely.ui.theme.ZinelyV2Motion].
 *
 * ### Selecting an action does not close the sheet
 *
 * The frozen `.act` buttons carry **no handler at all** — the mock wires the scrim and the `⋯`, and nothing
 * else (`:195-209`). So what follows *Rename* is undesigned here, and every one of the five leads somewhere
 * B3 does not own: a route, a share sheet, a rename field, a copy, an undoable delete. This composable
 * therefore reports the choice and holds still. **B5** decides dismissal alongside the flow it triggers,
 * the same way B2 left the desk to the screen that owns it.
 *
 * @param target the zine whose actions are showing, or `null` for a closed sheet. `null` is the closed
 *   state rather than a separate `visible` flag so that the header can never be asked to draw a zine that
 *   is not there.
 * @param onAction one of the five was chosen. Fires exactly once, and does not dismiss — see above.
 * @param onDismiss the scrim was tapped or the system back was pressed, which are the frozen dismissal
 *   paths (`:197`) plus the platform's own.
 */
@Composable
internal fun ZineActionSheet(
    target: ZineActionTarget?,
    onAction: (ZineAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept mounted through the exit so the sheet slides out rather than blinking away — the same shape as
    // V1's ZSheet, whose `MutableTransitionState` this borrows.
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
                // `transform:translateY(115%)` — of the sheet's own height, so it starts fully clear of the
                // bottom edge including the 10px inset.
                enter = slideInVertically(motion.settle(SheetDurationMillis)) { (it * SheetSlide).roundToInt() },
                exit = slideOutVertically(motion.settle(SheetDurationMillis)) { (it * SheetSlide).roundToInt() },
            ) {
                ZineActionSheetSurface(
                    target = drawn,
                    onAction = onAction,
                    modifier = modifier.padding(SheetInset),
                )
            }
        }
    }
}

/**
 * The `.scrim` — the dimming, and the tap that closes.
 *
 * Split out for the same reason [ZineActionSheetSurface] is, and for one more: it is the **only** place the
 * scrim's fill is painted. A test that built its own `Box` from the fill would agree with a constant while
 * the sheet painted something else — which is precisely what a mutation of the paint site proved, and why
 * this exists. Both the **D-022** probe and the parity raster compose this.
 *
 * **The fill is the corpus token, not the Library's literal — [D-022](docs/design/V2-SPEC-DEFECTS.md), ruled.**
 * `v2-library.html:119` writes `rgba(30,25,18,.36)` as a hard literal *outside* its own `:root`, so the
 * frozen `prefers-color-scheme` block cannot reach it and the dark Library dimmed exactly as much as the
 * light one — over a desk that is already near that colour. The owner ruled the corpus authoritative, as it
 * was for the serif (**D-005**) and the easings (**D-011**): `--scrim` is `ink@.34` light and `black@.50`
 * dark, and the dark half is deliberately *stronger*. This is one of the few places B3 does **not** transcribe
 * the Library file, and it is a ruling rather than an inference.
 */
@Composable
internal fun ZineActionScrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .testTag(ZineActionScrimTestTag)
            .fillMaxSize()
            .background(ZinelyTheme.v2Colors.scrim)
            // `scrim.addEventListener('click', close)` — `:197`.
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
 * `.ic` is a styled `<span>` holding a literal character, so the faithful transcription is text rather than
 * geometry, and the A7 icon set has no marks for *open* or *duplicate* to substitute even if substituting
 * were parity. Measured against the app's own fonts: `↗`, `⇪` and `⌫` are in Inter; **`✎`, `⧉` and the
 * shelf's `⋯` are not**, so the platform's fallback draws them and their weight and shape vary by device.
 * They render as real glyphs rather than tofu on the test platform, which
 * `ZineActionSheetTest.every frozen glyph draws a real mark, not a tofu box` pins — but that is one
 * platform, and the register entry is open for the owner to decide whether a glyph the design cannot
 * control is acceptable here.
 */
@Composable
internal fun ZineActionSheetSurface(
    target: ZineActionTarget,
    onAction: (ZineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    val type = ZinelyTheme.v2Typography

    Column(
        modifier
            .testTag(ZineActionSheetTestTag)
            .fillMaxWidth()
            .zinelyV2Shadow(
                listOf(
                    // box-shadow:0 30px 60px -20px var(--shadow), 0 0 0 1px var(--hair)
                    ZinelyV2ShadowLayer(dy = 30.dp, blur = 60.dp, spread = (-20).dp, color = colors.shadow),
                    ZinelyV2ShadowLayer(dy = 0.dp, blur = 0.dp, spread = 1.dp, color = colors.hair),
                ),
                SheetShape,
            )
            .clip(SheetShape)
            .background(colors.paper)
            // `role="dialog" aria-label="Zine actions"`. The Dialog window carries the modality; this
            // carries the name TalkBack announces on entering it.
            .semantics { paneTitle = ZineActionSheetPaneTitle },
    ) {
        // `.sh-head{padding:17px 20px 13px}`
        Column(Modifier.padding(HeadPadding)) {
            Text(
                text = target.title,
                modifier = Modifier.testTag(ZineActionTitleTestTag),
                style = TextStyle(
                    // `.sh-ttl` names an Iowan/Palatino/Georgia stack at 600; **D-005** ruled the canonical
                    // V2 serif is Fraunces at 500 and named this very selector. The register outranks the file.
                    fontFamily = type.voice,
                    fontWeight = FontWeight.Medium,
                    fontSize = TitleSize,
                    color = colors.ink,
                ),
            )
            Text(
                text = target.subtitle,
                modifier = Modifier
                    .testTag(ZineActionSubtitleTestTag)
                    .padding(top = SubtitleGap),
                style = TextStyle(
                    fontFamily = type.work,
                    fontSize = SubtitleSize,
                    color = colors.inkFaint,
                    // `font-variant-numeric:tabular-nums` — the dates change under the same header, and
                    // proportional digits make the line twitch when they do.
                    fontFeatureSettings = TabularNumerals,
                ),
            )
        }

        ZineAction.entries.forEach { action ->
            ActionRow(action = action, onAction = onAction)
        }
    }
}

/**
 * One `.act` row, with the border-top that separates it from whatever is above.
 *
 * The separator is a sibling `Box` rather than a drawn line because a CSS `border-top` occupies layout: the
 * danger row's `8px` band pushes Delete down by eight pixels, and a border painted inside the row's own
 * bounds would put it in the same place while leaving the row eight pixels shorter.
 */
@Composable
private fun ActionRow(action: ZineAction, onAction: (ZineAction) -> Unit) {
    val colors = ZinelyTheme.v2Colors
    val type = ZinelyTheme.v2Typography

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        Modifier
            .testTag(zineActionSeparatorTestTag(action))
            .fillMaxWidth()
            .height(if (action.danger) DangerSeparator else HairlineSeparator)
            .background(if (action.danger) colors.desk else colors.hair),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(zineActionTestTag(action))
            .zinelyV2Control(
                label = action.label,
                interactionSource = interaction,
                onClick = { onAction(action) },
            )
            // `.act:hover{background:var(--matcha-tint)}` · `.act.danger:hover{background:var(--straw-tint)}`.
            // Hover is a stylus/mouse state on Android and no state at all under a finger; transcribed
            // because the design states it, and because a desktop-mode window really does hover.
            .background(
                when {
                    !hovered -> Color.Transparent
                    action.danger -> colors.strawberryTint
                    else -> colors.matchaTint
                },
            )
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        Text(
            text = action.glyph,
            modifier = Modifier.width(IconSlot),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = type.work,
                fontSize = RowTextSize,
                color = rowInk(action).copy(alpha = IconOpacity),
            ),
        )
        Text(
            text = action.label,
            style = TextStyle(
                // `font-family:inherit` at the browser's default 16px — the case
                // `ZinelyV2Typography.base` documents by name. No line-height: the Library's body sets none.
                fontFamily = type.work,
                fontSize = RowTextSize,
                color = rowInk(action),
            ),
        )
    }
}

@Composable
private fun rowInk(action: ZineAction): Color =
    if (action.danger) ZinelyTheme.v2Colors.consequence else ZinelyTheme.v2Colors.ink

// ---------------------------------------------------------------------------------------------
// The frozen values. Per-component literals, per the D-007 ruling — no scale is published, and this
// sheet's own eleven numbers sit on no ladder either (10, 13, 14, 15, 17, 20, 20, 24, 30, 60, 115%).
// ---------------------------------------------------------------------------------------------

/** `.sheet{left:10px;right:10px;bottom:10px}` — one inset, three edges. */
private val SheetInset = 10.dp

/** `border-radius:20px`, all four corners: the card touches no edge to square off against. */
private val SheetShape = RoundedCornerShape(20.dp)

// The scrim's fill is deliberately **not** here: `ZineActionScrim` takes `ZinelyV2Colors.scrim` from the
// corpus, per the **D-022** ruling. The Library file's own `rgba(30,25,18,.36)` is stale and is not
// transcribed — the one place in B3 where a frozen Library value loses to the shared token layer, and it
// lost by ruling rather than by inference. See `ZineActionScrim`'s KDoc.

/** `transform:translateY(115%)` of the sheet's own height. */
private const val SheetSlide = 1.15f

/** `transition:transform .24s` — on **settle** per the D-011 ruling, not the file's own curve. */
private const val SheetDurationMillis = 240

/** `transition:opacity .2s` on the scrim — **standard**, per the same ruling's table. */
private const val ScrimDurationMillis = 200

/** `.sh-head{padding:17px 20px 13px}` */
private val HeadPadding = PaddingValues(
    start = 20.dp,
    top = 17.dp,
    end = 20.dp,
    bottom = 13.dp,
)

/** `.sh-ttl{font-size:1.12rem}` — 1.12 × 16, unrounded for the reason B1 carried 18.56sp. */
private val TitleSize = 17.92.sp

/** `.sh-sub{font-size:.78rem;margin-top:2px}` */
private val SubtitleSize = 12.48.sp
private val SubtitleGap = 2.dp
private const val TabularNumerals = "tnum"

/** `.sheet .act{padding:15px 20px;gap:14px}` */
private val RowPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp)
private val RowGap = 14.dp

/** `font-size:1rem` — the inherited browser default the four frozen chrome controls rely on. */
private val RowTextSize = 16.sp

/** `.sheet .ic{width:20px;opacity:.85}` */
private val IconSlot = 20.dp
private const val IconOpacity = 0.85f

/** `border-top:1px solid var(--hair)` on every row · `8px solid var(--desk)` above Delete. */
private val HairlineSeparator = 1.dp
private val DangerSeparator = 8.dp
