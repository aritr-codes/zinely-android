package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.Hsv
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.PALETTE_MAX
import com.aritr.zinely.core.editor.ink.PaletteDraft
import com.aritr.zinely.core.editor.ink.colourName
import com.aritr.zinely.core.editor.ink.hexToHsv
import com.aritr.zinely.core.editor.ink.hsvToHex
import com.aritr.zinely.core.editor.ink.hueName
import com.aritr.zinely.core.editor.ink.isPale
import com.aritr.zinely.core.editor.ink.parseHex
import com.aritr.zinely.core.editor.ink.stripHex
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.rememberZinelyV21GrainBrush
import com.aritr.zinely.ui.theme.zinelyV21Grain
import kotlin.math.roundToInt

/** The help line under the hex field. Rev. 2 `HELP=''`: it shows only while the typed code is not a colour. */
internal enum class InkMixerHelp { KEEP_TYPING, BAD_HEX }

/**
 * The mixer's whole state (prototype `state` + the `#hex` input + `#hexhelp`), with the prototype's handlers
 * as pure updates. The colour is [hsv]; [hex] is derived from it, as the prototype's `currentHex()`.
 */
@Immutable
internal data class InkMixerState(val hsv: Hsv, val hexField: TextFieldValue, val help: InkMixerHelp?) {

    val hex: InkHex get() = hsvToHex(hsv.h, hsv.s, hsv.v)

    /**
     * Hex `input`: strip and cap at six (R-I4), a six-digit code moves the colour live, anything else leaves
     * the colour and says why. An emptied field keeps the help it had, as the prototype does.
     */
    fun onHex(value: TextFieldValue): InkMixerState {
        val t = stripHex(value.text).take(HEX_DIGITS)
        val field = if (t == value.text) value else TextFieldValue(t, TextRange(t.length))
        val parsed = parseHex(t, final = false)
        return when {
            parsed != null -> copy(hsv = hexToHsv(parsed, hsv.h), hexField = field, help = null)
            t.any { !it.isHexDigit() } -> copy(hexField = field, help = InkMixerHelp.BAD_HEX)
            t.isNotEmpty() -> copy(hexField = field, help = InkMixerHelp.KEEP_TYPING)
            else -> copy(hexField = field)
        }
    }

    /** Prototype `commitHex` (Done, focus loss, Use): three digits count here; the field shows the colour. */
    fun commitHex(): InkMixerState {
        val parsed = parseHex(hexField.text, final = true)
        return following(if (parsed != null) hexToHsv(parsed, hsv.h) else hsv)
    }

    /** The square: strength across, lightness up. Clamped, since a drag overshoots its box. */
    fun onSquare(s: Double, v: Double): InkMixerState = following(hsv.copy(s = unit(s), v = unit(v)))

    /** The strip: 0..355 (`Math.min(355, …)`), so the far end is not red again. */
    fun onHue(h: Double): InkMixerState =
        following(hsv.copy(h = if (h.isNaN()) 0.0 else h.coerceIn(0.0, HUE_MAX)))

    private fun following(next: Hsv): InkMixerState {
        val text = hsvToHex(next.h, next.s, next.v).value
        return InkMixerState(next, TextFieldValue(text, TextRange(text.length)), null)
    }

    companion object {
        /** Prototype `openMixer`: a grey keeps the hue the strip had ([keepHue]). */
        fun open(hex: InkHex, keepHue: Double?): InkMixerState =
            InkMixerState(hexToHsv(hex, keepHue), TextFieldValue(hex.value, TextRange(hex.value.length)), null)
    }
}

private const val HEX_DIGITS = 6
private const val HUE_MAX = 355.0
private const val SLIDER_STEP = 5

private fun unit(x: Double) = if (x.isNaN()) 0.0 else x.coerceIn(0.0, 1.0)
private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/** Rev. 2 `mixUse` label: from edit mode it adds to the palette while there is room, else only keeps it. */
internal fun inkMixerUseLabel(draft: PaletteDraft?): String = when {
    draft == null -> Copy.InkMix.USE_THIS_INK
    draft.inks.size < PALETTE_MAX -> Copy.InkMix.ADD_TO_PALETTE
    else -> Copy.InkMix.SAVE_TO_MY_INKS
}

/**
 * The Mix-an-ink sheet (`v21-ink-mixer.html` `#mixer`): `.sheet.mixer{max-height:92%}`, the body scrolls
 * under the title and the foot stays pinned. Cancel, the scrim and Back all close it ([onCancel]); Use
 * commits the typed code first (prototype `mixUse` → `commitHex()`) and hands over the colour.
 */
@Composable
internal fun InkMixerSheet(
    visible: Boolean,
    state: InkMixerState,
    onState: (InkMixerState) -> Unit,
    kind: BenchVerbKind,
    sample: String,
    library: InkLibrary?,
    useLabel: String,
    onCancel: () -> Unit,
    onUse: (InkHex) -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * InkMixerMaxHeightFraction
    // The field's focus-loss commit can land as the closing dialog detaches; a closed mixer takes no writes.
    val shown by rememberUpdatedState(visible)
    val write: (InkMixerState) -> Unit = { if (shown) onState(it) }
    ZSheet(
        visible = visible,
        onDismiss = onCancel,
        title = Copy.InkMix.TITLE,
        modifier = Modifier.heightIn(max = maxHeight),
        titleTrailing = { InkMixerName(state.hex) },
        foot = {
            InkMixerFoot(useLabel, onCancel = onCancel, onUse = {
                // The foot stays tappable while the sheet slides out: a second Use would be a second write.
                if (shown) {
                    val committed = state.commitHex()
                    write(committed)
                    onUse(committed.hex)
                }
            })
        },
    ) {
        InkMixerBody(state, write, kind, sample, library)
    }
}

/** `.sheet.mixer h3 small`: the colour's name beside the title. Not a live region (the sliders speak it). */
@Composable
internal fun InkMixerName(hex: InkHex) {
    Text(
        text = inkColourWords(colourName(hex)),
        fontFamily = ZinelyV21Fonts.Work,
        fontWeight = FontWeight.SemiBold,
        fontSize = InkMixerSmallSp,
        color = ZinelyTheme.v21Colors.inkSoft,
        modifier = Modifier.padding(start = ZinelyV21Dimens.gapSm),
    )
}

/** `.sheet.mixer .body`: the full note, the paper preview, the hex row, the square and the strip. */
@Composable
internal fun InkMixerBody(
    state: InkMixerState,
    onState: (InkMixerState) -> Unit,
    kind: BenchVerbKind,
    sample: String,
    library: InkLibrary?,
    modifier: Modifier = Modifier,
) {
    val current by rememberUpdatedState(state)
    val emit by rememberUpdatedState(onState)
    val big = LocalDensity.current.fontScale >= BenchInkLargeTextFontScale
    val hex = state.hex
    val words = inkColourWords(colourName(hex))
    Column(modifier.fillMaxWidth().padding(bottom = ZinelyV21Dimens.gapMd)) {
        library?.wouldEvict(hex)?.let { InkMixerFullNote(it) }
        InkMixerPreview(hex, kind, sample)
        InkMixerHexRow(state, onState)
        // RF7: TalkBack's form of the square and the strip, after the field. They sit under the square's
        // corner so they take no space (`.vh`) yet keep real on-screen bounds; they take the arrow keys too.
        Box {
            Row {
                InkMixerSlider(
                    tag = InkMixerStrengthTestTag,
                    label = Copy.InkMix.COLOUR_STRENGTH,
                    value = Copy.InkMix.strengthValue(words, percent(state.hsv.s)),
                    current = percent(state.hsv.s),
                    max = 100,
                ) { emit(current.onSquare(it / 100.0, current.hsv.v)) }
                InkMixerSlider(
                    tag = InkMixerLightTestTag,
                    label = Copy.InkMix.LIGHTNESS,
                    value = Copy.InkMix.lightValue(words, percent(state.hsv.v)),
                    current = percent(state.hsv.v),
                    max = 100,
                ) { emit(current.onSquare(current.hsv.s, it / 100.0)) }
                InkMixerSlider(
                    tag = InkMixerHueTestTag,
                    label = Copy.InkMix.HUE,
                    value = hueWord(hueName(state.hsv.h)),
                    current = state.hsv.h.roundToInt(),
                    max = HUE_MAX.toInt(),
                ) { emit(current.onHue(it.toDouble())) }
            }
            InkMixerSquare(state.hsv, hex, if (big) InkMixerSquareBigDp else InkMixerSquareDp) { s, v ->
                emit(current.onSquare(s, v))
            }
        }
        InkMixerHints(big)
        InkMixerHue(state.hsv.h) { emit(current.onHue(it)) }
    }
}

private fun percent(x: Double) = (x * 100).roundToInt()

/** `.fullnote`: shown only when Use would drop a kept ink; names it, beside a 24dp pot of it. */
@Composable
private fun InkMixerFullNote(evicted: InkHex) {
    val colors = ZinelyTheme.v21Colors
    val name = inkColourWords(colourName(evicted))
    val text = Copy.InkMix.fullNote(name)
    Row(
        modifier = Modifier
            .padding(bottom = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .benchDashedBorder(BenchInkPopoverBorderDp, colors.ink, ZinelyV21Dimens.radiusMd)
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(InkMixerNotePotDp)
                .clip(CircleShape)
                .background(Color(evicted.argb()))
                .border(BenchInkPopoverBorderDp, colors.ink, CircleShape),
        )
        Text(
            text = bold(text, name),
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = InkMixerNoteSp,
            lineHeight = 1.35.em,
            color = colors.ink,
            // `role="status"`: the note appears as the colour crosses into "new", and says so once.
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(InkMixerFullNoteTestTag),
        )
    }
}

private fun bold(text: String, part: String): AnnotatedString = buildAnnotatedString {
    val at = text.indexOf(part)
    if (at < 0) {
        append(text)
    } else {
        append(text.substring(0, at))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        append(text.substring(at + part.length))
    }
}

private fun InkHex.argb(): Int = (0xFF000000.toInt()) or value.toInt(16)

/**
 * `.proofcard`: white paper, not a dark panel — the ink judged where it lands. The footer is the legibility
 * line, one fixed slot (rev. 2): both states are laid out on top of each other and only one shows, so the
 * square below never moves while you drag across the pale line.
 */
@Composable
private fun InkMixerPreview(hex: InkHex, kind: BenchVerbKind, sample: String) {
    val colors = ZinelyTheme.v21Colors
    val mix = Color(hex.argb())
    val pale = isPale(hex)
    val shape = InkMixerCardShape
    val grain = rememberZinelyV21GrainBrush(InkMixerGrainTile)
    Column(
        Modifier
            .fillMaxWidth()
            .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, colors.inkLine, shape)
            .clip(shape)
            .background(Color.White)
            .border(BenchInkPopoverBorderDp, colors.ink, shape)
            .zinelyV21Grain(grain, ZinelyV21Grain.BakedAlpha * InkMixerGrainOpacity, ZinelyV21Grain.PaperBlend)
            .semantics { contentDescription = Copy.InkMix.PREVIEW_LABEL },
    ) {
        Row(
            Modifier.padding(start = ZinelyV21Dimens.gapMd, end = ZinelyV21Dimens.gapMd, top = ZinelyV21Dimens.gapSm),
            horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sample,
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = InkMixerSampleSp,
                lineHeight = 1.1.em,
                color = mix,
                modifier = Modifier.weight(1f),
            )
            InkMixerStar(mix)
        }
        Box(
            Modifier
                .padding(top = ZinelyV21Dimens.gapXs * 2) // grid row-gap + `.legib` margin-top
                .fillMaxWidth()
                .defaultMinSize(minHeight = InkMixerLegibMinDp)
                .background(if (pale) colors.butter else Color.Transparent)
                .drawBehind { benchDashedLine(BenchInkPaperInk, BenchInkPopoverBorderDp.toPx(), BenchInkPopoverBorderDp.toPx() / 2f) }
                .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapXs)
                .testTag(InkMixerLegibTestTag),
            contentAlignment = Alignment.CenterStart,
        ) {
            InkMixerLegibLine(active = !pale, warn = false, text = AnnotatedString(Copy.InkMix.READS_CLEARLY))
            val rest = if (kind == BenchVerbKind.TEXT) Copy.InkMix.PALE_TEXT else Copy.InkMix.PALE_ART
            InkMixerLegibLine(
                active = pale,
                warn = true,
                text = buildAnnotatedString {
                    // `.legib b{display:block}` (the later rule wins): the lead is a line of its own.
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Copy.InkMix.PALE_LEAD) }
                    append(InkMixerNewline)
                    append(rest.trimStart())
                },
            )
        }
    }
}

/** One state of `.legib`: the inactive one keeps its size but is not drawn and not a node. */
@Composable
private fun InkMixerLegibLine(active: Boolean, warn: Boolean, text: AnnotatedString) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (active) {
                    // The pale line is `role="status"`; the clear line is not announced.
                    if (warn) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            )
            .drawWithContent { if (active) drawContent() }
            .testTag(if (warn) InkMixerLegibPaleTestTag else InkMixerLegibClearTestTag),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(InkMixerLegibIconDp)) { if (warn) warnGlyph(BenchInkPaperInk) else tickGlyph(BenchInkPaperInk) }
        Text(
            text = text,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = InkMixerLegibSp,
            lineHeight = 1.4.em,
            color = BenchInkPaperInk,
            modifier = Modifier.weight(1f),
        )
    }
}

/** `.legib svg` (24-unit box, stroke 2, round): the warn circle with its bar and dot. */
private fun DrawScope.warnGlyph(color: Color) {
    val u = size.minDimension / 24f
    val w = 2f * u
    drawCircle(color, 9f * u, center, style = Stroke(w))
    drawLine(color, Offset(12f * u, 7.5f * u), Offset(12f * u, 13f * u), w, StrokeCap.Round)
    drawLine(color, Offset(12f * u, 16.5f * u), Offset(12f * u, 16.51f * u), w, StrokeCap.Round)
}

/** `.legib .ok-only` tick: `M5 12l5 5 9-10`. */
private fun DrawScope.tickGlyph(color: Color) {
    val u = size.minDimension / 24f
    val w = 2f * u
    drawLine(color, Offset(5f * u, 12f * u), Offset(10f * u, 17f * u), w, StrokeCap.Round)
    drawLine(color, Offset(10f * u, 17f * u), Offset(19f * u, 7f * u), w, StrokeCap.Round)
}

/** `svg.mark`: the prototype's star, 40dp, in the mixed colour. Decoration: no node. */
@Composable
private fun InkMixerStar(color: Color) {
    val path = remember { PathParser().parsePathString(InkMixerStarPath).toPath() }
    Canvas(Modifier.size(InkMixerStarDp)) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) { drawPath(path, color) }
    }
}

/**
 * `.hexrow`: the hex first. The label is visually hidden and carried by the field; the `#` and the swatch
 * are decoration. Done and focus loss commit (three digits count there); help shows only for a problem.
 */
@Composable
private fun InkMixerHexRow(state: InkMixerState, onState: (InkMixerState) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    val current by rememberUpdatedState(state)
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val err = state.help == InkMixerHelp.BAD_HEX
    val fieldStyle = TextStyle(
        fontFamily = ZinelyV21Fonts.Work,
        fontWeight = FontWeight.SemiBold,
        fontSize = InkMixerHexSp,
        letterSpacing = 0.06.em,
        color = BenchInkPaperInk,
    )
    BasicTextField(
        value = state.hexField,
        onValueChange = { onState(current.onHex(it)) },
        singleLine = true,
        textStyle = fieldStyle,
        cursorBrush = SolidColor(BenchInkPaperInk),
        visualTransformation = UpperCase,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        // `commitHex();$('hex').blur()`: Done also drops the keyboard, so the square and strip come back.
        keyboardActions = KeyboardActions(onDone = {
            onState(current.commitHex())
            focusManager.clearFocus()
        }),
        modifier = Modifier
            .padding(top = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .onFocusChanged {
                if (focused && !it.isFocused) onState(current.commitHex())
                focused = it.isFocused
            }
            // `.hexbox:focus-within{outline:3px solid var(--berry);outline-offset:2px}`.
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusMd, offset = BenchInkNameRingOffsetDp, color = colors.berry)
            .testTag(InkMixerHexFieldTestTag)
            .semantics { contentDescription = Copy.InkMix.HEX_LABEL },
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = InkMixerHexBoxDp)
                    .background(colors.paper, shape)
                    .border(if (err) InkMixerHexErrBorderDp else BenchInkPopoverBorderDp, if (err) colors.jamText else colors.ink, shape)
                    .padding(horizontal = ZinelyV21Dimens.gapMd),
                horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(InkMixerHash, style = fieldStyle.copy(color = BenchInkPaperSoft, letterSpacing = 0.sp), modifier = Modifier.clearAndSetSemantics {})
                Box(Modifier.weight(1f).padding(vertical = ZinelyV21Dimens.gapSm)) {
                    if (state.hexField.text.isEmpty()) {
                        // `placeholder` = the colour still showing.
                        Text(state.hex.value, style = fieldStyle.copy(color = BenchInkPaperSoft), modifier = Modifier.clearAndSetSemantics {})
                    }
                    inner()
                }
                Box(
                    Modifier
                        .size(InkMixerSwatchDp)
                        .clip(CircleShape)
                        .background(Color(state.hex.argb()))
                        .border(BenchInkPopoverBorderDp, BenchInkPaperInk, CircleShape),
                )
            }
        },
    )
    state.help?.let { help ->
        Text(
            text = when (help) {
                InkMixerHelp.BAD_HEX -> Copy.InkMix.badHex(state.hex.value)
                InkMixerHelp.KEEP_TYPING -> Copy.InkMix.keepTyping(state.hex.value)
            },
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = InkMixerHelpSp,
            fontWeight = if (err) FontWeight.SemiBold else FontWeight.Normal,
            color = if (err) colors.jamText else colors.inkSoft,
            modifier = Modifier
                .padding(top = ZinelyV21Dimens.gapXs, start = 2.dp, end = 2.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(InkMixerHelpTestTag),
        )
    }
}

/** `text-transform:uppercase` on the field: shown upper-case, stored as typed (the parse upper-cases). */
private val UpperCase = VisualTransformation { TransformedText(AnnotatedString(it.text.uppercase()), OffsetMapping.Identity) }

/**
 * A visually hidden `input type=range` (`.vh`): a 1dp node TalkBack can find and swipe, stepping by 5, and a
 * keyboard can focus and move with Left/Right (±5 on the 5-step grid, as an Android SeekBar; A40 rev. 2
 * amendment 2026-10-26). [onSet] gets the stepped value; the state clamps it.
 */
@Composable
private fun InkMixerSlider(tag: String, label: String, value: String, current: Int, max: Int, onSet: (Int) -> Unit) {
    val set by rememberUpdatedState(onSet)
    val now by rememberUpdatedState(current)
    Box(
        Modifier
            .size(1.dp)
            .onKeyEvent { e ->
                val d = when (e.key) {
                    // Left/Right only, as an Android SeekBar: Up/Down stay free to move focus off the 1dp node.
                    Key.DirectionRight -> SLIDER_STEP
                    Key.DirectionLeft -> -SLIDER_STEP
                    else -> return@onKeyEvent false
                }
                // Snaps to the 5-step grid first (a drag on the square can leave 37), as setProgress does.
                if (e.type == KeyEventType.KeyDown) set(((now + d) / SLIDER_STEP * SLIDER_STEP).coerceIn(0, max))
                true
            }
            .focusable()
            .semantics {
                contentDescription = label
                stateDescription = value
                progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), 0f..max.toFloat(), steps = max / SLIDER_STEP - 1)
                setProgress { target ->
                    set((target / SLIDER_STEP).roundToInt() * SLIDER_STEP)
                    true
                }
            }
            .testTag(tag),
    )
}

/**
 * `.sv`: one shade square, touch only (`aria-hidden`): the sliders above are its accessible form. White to
 * the pure hue across, to black downward; the knob sits at (s, 1 − v) in the mixed colour.
 */
@Composable
private fun InkMixerSquare(hsv: Hsv, hex: InkHex, height: Dp, onMove: (Double, Double) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    val move by rememberUpdatedState(onMove)
    val pure = Color(hsvToHex(hsv.h, 1.0, 1.0).argb())
    val mix = Color(hex.argb())
    Canvas(
        Modifier
            .padding(top = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .height(height)
            .border(BenchInkPopoverBorderDp, colors.ink, shape)
            .pointerInput(Unit) {
                fun at(p: Offset) = move((p.x / size.width).toDouble(), 1.0 - (p.y / size.height))
                track(::at)
            }
            .clearAndSetSemantics { testTag = InkMixerSquareTestTag },
    ) {
        // Rounded fills, not a clip: the knob overhangs the edge, as `.sv` (no overflow:hidden).
        val r = CornerRadius(ZinelyV21Dimens.radiusMd.toPx())
        drawRoundRect(Brush.horizontalGradient(listOf(Color.White, pure)), cornerRadius = r)
        drawRoundRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), cornerRadius = r)
        knob(Offset((hsv.s * size.width).toFloat(), ((1 - hsv.v) * size.height).toFloat()), mix)
    }
}

/** `.slbl`: what the square's two directions do. Decoration (`aria-hidden`); a column at largest text. */
@Composable
private fun InkMixerHints(big: Boolean) {
    val colors = ZinelyTheme.v21Colors
    val hint: @Composable (String) -> Unit = {
        Text(it, fontFamily = ZinelyV21Fonts.Work, fontWeight = FontWeight.SemiBold, fontSize = InkMixerHintSp, color = colors.inkSoft)
    }
    val m = Modifier.padding(top = ZinelyV21Dimens.gapXs).fillMaxWidth().clearAndSetSemantics {}
    if (big) {
        Column(m) { hint(Copy.InkMix.SQUARE_HINT_X); hint(Copy.InkMix.SQUARE_HINT_Y) }
    } else {
        Row(m, horizontalArrangement = Arrangement.SpaceBetween) { hint(Copy.InkMix.SQUARE_HINT_X); hint(Copy.InkMix.SQUARE_HINT_Y) }
    }
}

/** `.hue`: a 28dp rainbow track in a 48dp touch strip, touch only (the Hue slider is its accessible form). */
@Composable
private fun InkMixerHue(h: Double, onMove: (Double) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val move by rememberUpdatedState(onMove)
    Canvas(
        Modifier
            .padding(top = ZinelyV21Dimens.gapSm)
            .fillMaxWidth()
            .height(BenchInkTouchDp)
            .pointerInput(Unit) {
                fun at(p: Offset) = move(p.x / size.width * 360.0)
                track(::at)
            }
            .clearAndSetSemantics {},
    ) {
        val track = InkMixerHueTrackDp.toPx()
        val top = (size.height - track) / 2f
        val radius = CornerRadius(track / 2f)
        drawRoundRect(Brush.horizontalGradient(InkMixerRainbow), Offset(0f, top), Size(size.width, track), radius)
        val bw = BenchInkPopoverBorderDp.toPx()
        drawRoundRect(
            colors.ink,
            Offset(bw / 2f, top + bw / 2f),
            Size(size.width - bw, track - bw),
            radius,
            style = Stroke(bw),
        )
        knob(Offset((h / 360.0 * size.width).toFloat(), size.height / 2f), Color(hsvToHex(h, 1.0, 1.0).argb()))
    }
}

/** A down and its drag, each position handed on; consumed so the sheet's scroll does not take it. */
private suspend fun PointerInputScope.track(at: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        at(down.position)
        drag(down.id) {
            it.consume()
            at(it.position)
        }
    }
}

/** `.knob`: 26dp, a 3dp white rim, a 1.5dp ink ring outside it. */
private fun DrawScope.knob(at: Offset, fill: Color) {
    val r = InkMixerKnobDp.toPx() / 2f
    val rim = 3.dp.toPx()
    val ring = BenchInkPopoverBorderDp.toPx()
    drawCircle(BenchInkPaperInk, r + ring, at)
    drawCircle(Color.White, r, at)
    drawCircle(fill, r - rim, at)
}

/**
 * `.mixfoot` / `.mixbar`: Cancel and the primary (`flex:1.6`), behind a dashed rule; stacked full width and
 * 56dp tall at largest text.
 */
@Composable
internal fun InkMixerFoot(useLabel: String, onCancel: () -> Unit, onUse: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val big = LocalDensity.current.fontScale >= BenchInkLargeTextFontScale
    val height = if (big) InkMixerBigButtonDp else BenchInkTouchDp
    val bar = Modifier
        .fillMaxWidth()
        .background(colors.surface)
        .drawBehind { benchDashedLine(colors.hair, BenchInkPopoverBorderDp.toPx(), BenchInkPopoverBorderDp.toPx() / 2f) }
        .padding(top = ZinelyV21Dimens.gapSm, bottom = ZinelyV21Dimens.gapMd)
    val cancel: @Composable (Modifier) -> Unit = {
        BenchInkBarButton(Copy.BenchInk.CANCEL, primary = false, dim = false, onClick = onCancel, tag = InkMixerCancelTestTag, modifier = it, minHeight = height)
    }
    val use: @Composable (Modifier) -> Unit = {
        BenchInkBarButton(useLabel, primary = true, dim = false, onClick = onUse, tag = InkMixerUseTestTag, modifier = it, minHeight = height)
    }
    if (big) {
        Column(bar, verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
            cancel(Modifier.fillMaxWidth())
            use(Modifier.fillMaxWidth())
        }
    } else {
        Row(bar, horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
            cancel(Modifier.weight(1f))
            use(Modifier.weight(BenchInkGoFlex))
        }
    }
}

internal const val InkMixerHexFieldTestTag = "ink-mixer-hex"
internal const val InkMixerHelpTestTag = "ink-mixer-hex-help"
internal const val InkMixerLegibTestTag = "ink-mixer-legib"
internal const val InkMixerLegibPaleTestTag = "ink-mixer-legib-pale"
internal const val InkMixerLegibClearTestTag = "ink-mixer-legib-clear"
internal const val InkMixerFullNoteTestTag = "ink-mixer-full-note"
internal const val InkMixerStrengthTestTag = "ink-mixer-strength"
internal const val InkMixerLightTestTag = "ink-mixer-light"
internal const val InkMixerHueTestTag = "ink-mixer-hue"
internal const val InkMixerSquareTestTag = "ink-mixer-square"
internal const val InkMixerCancelTestTag = "ink-mixer-cancel"
internal const val InkMixerUseTestTag = "ink-mixer-use"

// — the frozen numbers (`v21-ink-mixer.html:747-829`) —

/** `.sheet.mixer{max-height:92%}`. */
internal const val InkMixerMaxHeightFraction = 0.92f

/** `.proofcard{border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs)}`. */
private val InkMixerCardShape = RoundedCornerShape(
    topStart = ZinelyV21Dimens.radiusXs,
    topEnd = ZinelyV21Dimens.radiusMd,
    bottomEnd = ZinelyV21Dimens.radiusMd,
    bottomStart = ZinelyV21Dimens.radiusXs,
)

/** `.proofcard::after{background-size:130px;opacity:.28}`. */
private val InkMixerGrainTile = 130.dp
private const val InkMixerGrainOpacity = 0.28f

/** `.proofcard .pt{font:700 1.32rem/1.1 var(--voice)}`. */
private val InkMixerSampleSp = 21.12.sp

/** `.proofcard svg.mark{width:40px;height:40px}`. */
private val InkMixerStarDp = 40.dp
private const val InkMixerStarPath = "M12 1l2.6 6.9 7.4.4-5.8 4.6 2 7.1L12 16l-6.2 4 2-7.1L2 8.3l7.4-.4z"

/** `.proofcard .legib{min-height:40px;font-size:.74rem}`, `.legib svg{18px}`. */
private val InkMixerLegibMinDp = 40.dp
private val InkMixerLegibSp = 11.84.sp
private val InkMixerLegibIconDp = 18.dp

/** `.sheet.mixer h3 small{font:600 .7rem}`. */
private val InkMixerSmallSp = 11.2.sp

/** `.fullnote{font-size:.76rem}`, `.fullnote .pot{24px}`. */
private val InkMixerNoteSp = 12.16.sp
private val InkMixerNotePotDp = 24.dp

/** `.hexbox{min-height:52px}`, `input{font:600 1.1rem}`, `.sw{30px}`, `.hexhelp{.72rem}`. */
private val InkMixerHexBoxDp = 52.dp
private val InkMixerHexSp = 17.6.sp
private val InkMixerSwatchDp = 30.dp
private val InkMixerHelpSp = 11.52.sp

/** `.hexbox.err{border-color:var(--jam-text);box-shadow:inset 0 0 0 1px}`: 1.5 + 1. */
private val InkMixerHexErrBorderDp = 2.5.dp
private const val InkMixerHash = "#"
private const val InkMixerNewline = "\n"

/** `.sv{height:132px}`, `[data-big] .sv{height:120px}`. */
private val InkMixerSquareDp = 132.dp
private val InkMixerSquareBigDp = 120.dp

/** `.slbl{font:600 .68rem}`. */
private val InkMixerHintSp = 10.88.sp

/** `.hue::before{height:28px}`, `.knob{26px}`. */
private val InkMixerHueTrackDp = 28.dp
private val InkMixerKnobDp = 26.dp
private val InkMixerRainbow = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF00FFFF),
    Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
)

/** `[data-big] .mixbar button{min-height:56px}`. */
private val InkMixerBigButtonDp = 56.dp
