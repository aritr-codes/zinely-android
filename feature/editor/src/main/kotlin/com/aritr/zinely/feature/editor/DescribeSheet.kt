package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.DescriptionText
import com.aritr.zinely.ui.components.ZPrimaryButton
import com.aritr.zinely.ui.components.ZPrimaryButtonMetrics
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

internal const val DescribeSheetTestTag = "describe-sheet"
internal const val DescribeFieldTestTag = "describe-field"
internal const val DescribeDoneTestTag = "describe-done"
internal const val DescribeCountTestTag = "describe-count"

/**
 * The Describe sheet ([`v21-bench.html` A52](../../../../../../../../docs/design/mockups/v21-bench.html),
 * [ADR-147](../../../../../../../../docs/DECISIONS.md#adr-147)): title, a labelled field, A38's helper line, a
 * count that appears only near the cap, and one Done pinned to the foot.
 *
 * **Every way out saves** (Done, Back, a scrim tap, Enter): closing never throws typing away (A52.4). There is no
 * Cancel; to abandon an edit the maker undoes it. The sheet calls [onSave] with the draft on each close and the
 * reducer drops a no-op, so "a close that changed nothing is no step" lives in one place. (`ZSheet` has no
 * drag-to-dismiss by design, so there is no swipe path to wire.)
 *
 * The field's edit filter is [DescriptionText.edit] with the text the sheet **opened** with, so a stored
 * description longer than the cap is never cut by opening and closing the sheet (A52.3).
 *
 * @param kind photo or Art; the title and the grey example differ.
 * @param opening the description the sheet opens with (empty for none). Keyed, so reopening starts from it.
 * @param onSave the draft, un-normalised; the reducer normalises it once.
 * @param onClose the sheet should go away (called after [onSave]).
 */
@Composable
internal fun DescribeSheet(
    visible: Boolean,
    kind: BenchVerbKind,
    opening: String,
    onSave: (String) -> Unit,
    onClose: () -> Unit,
) {
    var draft by rememberSaveable(visible, opening, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(opening, TextRange(opening.length)))
    }
    // The sheet slides out for a moment after it closes; a second Done or a late scrim tap must not save twice.
    val shown by rememberUpdatedState(visible)
    val finish = {
        if (shown) {
            onSave(draft.text)
            onClose()
        }
    }
    val photo = kind == BenchVerbKind.PHOTO
    ZSheet(
        visible = visible,
        onDismiss = finish,
        title = if (photo) Copy.Describe.TITLE_PHOTO else Copy.Describe.TITLE_ART,
        modifier = Modifier.testTag(DescribeSheetTestTag),
        scrollBody = true,
        foot = {
            ZPrimaryButton(
                text = Copy.Describe.DONE,
                onClick = finish,
                metrics = ZPrimaryButtonMetrics.Bench,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ZinelyV21Dimens.gapSm, bottom = ZinelyV21Dimens.gapMd)
                    .testTag(DescribeDoneTestTag),
            )
        },
    ) {
        DescribeBody(
            draft = draft,
            onDraft = { next ->
                val text = DescriptionText.edit(opening, next.text)
                draft = if (text == next.text) next else TextFieldValue(text, TextRange(minOf(next.selection.end, text.length)))
            },
            example = if (photo) Copy.Describe.EXAMPLE_PHOTO else Copy.Describe.EXAMPLE_ART,
            onDone = finish,
        )
    }
}

@Composable
private fun DescribeBody(
    draft: TextFieldValue,
    onDraft: (TextFieldValue) -> Unit,
    example: String,
    onDone: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val remaining = (DescriptionText.MAX_CODE_POINTS - draft.text.let { it.codePointCount(0, it.length) }).coerceAtLeast(0)
    val countText = if (remaining <= Copy.Describe.COUNT_SHOWN_AT) Copy.Describe.left(remaining) else null
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    Column(Modifier.fillMaxWidth()) {
        BasicText(
            text = Copy.Describe.LABEL,
            style = TextStyle(
                color = colors.ink,
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.SemiBold,
                fontSize = LabelSp,
            ),
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp).clearAndSetSemantics { },
        )
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            // The visible label is the field's name; the count is its supporting text, so it travels in the
            // field's own node (a state description) and not as a separate stop or a live region (A52.2).
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .testTag(DescribeFieldTestTag)
                .semantics {
                    contentDescription = Copy.Describe.LABEL
                    countText?.let { stateDescription = it }
                }
                .onPreviewKeyEvent {
                    // Enter is Done (A52.3): a pasted line break was already turned into a space.
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        onDone()
                        true
                    } else {
                        false
                    }
                },
            textStyle = TextStyle(color = colors.ink, fontFamily = ZinelyV21Fonts.Work, fontSize = FieldSp, lineHeight = FieldLineSp),
            cursorBrush = SolidColor(colors.ink),
            minLines = 3,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .clip(shape)
                        .background(colors.paper)
                        .border(1.5.dp, colors.ink, shape)
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    if (draft.text.isEmpty()) {
                        BasicText(
                            text = example,
                            style = TextStyle(color = colors.inkSoft, fontFamily = ZinelyV21Fonts.Work, fontSize = FieldSp, lineHeight = FieldLineSp),
                            // The grey example is a hint for the eyes; the label already names the field.
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                    inner()
                }
            },
        )
        BasicText(
            text = Copy.Describe.HELPER,
            style = TextStyle(color = colors.inkSoft, fontFamily = ZinelyV21Fonts.Work, fontSize = SmallSp),
            modifier = Modifier.padding(start = 2.dp, top = 6.dp),
        )
        if (countText != null) {
            BasicText(
                text = countText,
                style = TextStyle(color = colors.ink, fontFamily = ZinelyV21Fonts.Work, fontWeight = FontWeight.SemiBold, fontSize = SmallSp),
                // Spoken through the field's own node above, so this drawn copy is silent.
                modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = ZinelyV21Dimens.gapSm)
                    .testTag(DescribeCountTestTag).clearAndSetSemantics { },
            )
        }
    }
}

// `.sh label{font-size:.9em}` / `.help,.left{font-size:.85em}` / the field's 1em, at the sheet's 16sp body.
private val LabelSp = 14.4.sp
private val SmallSp = 13.6.sp
private val FieldSp = 16.sp
private val FieldLineSp = 22.4.sp
