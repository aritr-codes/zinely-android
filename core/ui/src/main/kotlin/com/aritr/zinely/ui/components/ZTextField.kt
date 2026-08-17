package com.aritr.zinely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/**
 * The design system's single-line text field — the corpus's `.search` box.
 *
 * ⚠ It called itself *"the rename field — the Library's rename sheet, and the Bench's inline rename"*
 * until ADR-102's second batch, and that was false in both halves: neither surface calls it (see the
 * grep below). The summary line is where a skimming reader stops, so it is corrected here rather than
 * only in the ⚠ paragraph that already contradicted it.
 *
 * ### V2.1 — ADR-102 P8, and the corpus has exactly one field
 *
 * ```css
 * .search{display:flex;align-items:center;gap:var(--gap-sm);background:var(--paper);
 *   border:1.5px solid var(--ink);border-radius:var(--br-pill);padding:var(--gap-sm) var(--gap-md);
 *   box-shadow:2px 2px 0 var(--ink-line)}
 * .search input{border:0;background:none;font-family:var(--sans);font-size:.86rem;color:var(--ink);outline:0}
 * ```
 *
 * V2 gave this a `--field` fill, a 1px `--field-edge` border that turned coral on focus, and radius 12.
 * **V2.1 publishes no `--field`, no `--field-edge` and no coral**, and the one text input the three
 * prototypes contain is `v21-bench.html .search`. So the box is that box: paper under a 1.5px ink
 * border at pill radius, over a `2px 2px 0 var(--ink-line)` printed shadow. There is no `.field` class
 * to grep for, and inventing one is what D-008 forbids.
 *
 * **The setting is `.search input`'s: `--sans` at `.86rem` (13.76sp), regular weight.**
 *
 * ⚠ It was Averia at 17sp/600, defended here as *"a zine's name is spoken in the zine voice, which is
 * this component's own frozen identity"*. A review took that apart on two counts. First, **this component
 * does not set any zine's name** — `git grep ZTextField(` finds its own declaration and two tests, and
 * nothing else; the live rename field is `ShelfSheets.RenameInput`, which is a different composable
 * citing a different rule (`.sh-ttl`, Voice at 19.52sp). So the justification described a use this code
 * does not have. Second, 17sp corresponds to **no rem in the corpus** — it is a V2 number that V2.1
 * supersedes, carried forward under the word "frozen", which is exactly the move the Documentation Rule
 * exists to stop.
 *
 * A generic field's nearest frozen relative is the corpus's only field, and that is `.search input`. So
 * this now agrees with it, and there are two answers to "how does a text field look" instead of three.
 * If a zine-voice field is wanted later, `RenameInput` already is one and is the thing to generalise.
 *
 * ### The focus indication changed shape, and did not go away
 *
 * V2 signalled focus by recolouring the border. V2.1's border is already `ink` at rest, so there is no
 * colour left to move to — and dropping the signal would be an accessibility regression, not a
 * re-skin. Focus is therefore drawn with the corpus's own focus idiom, [zinelyFocusRing]
 * (`outline:2px solid var(--ink);outline-offset:4px`), which is what every other focusable object in
 * the three files wears. Nothing else about this component's behaviour moves: the same single-line
 * IME contract, the same [keyboardOptions]/[keyboardActions] pass-through, the same interaction
 * source.
 */
@Composable
public fun ZTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = ZinelyTheme.v21Colors
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = TextStyle(
            color = colors.ink,
            fontFamily = ZinelyV21Fonts.Work,
            fontSize = FieldTextSize,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        ),
        cursorBrush = SolidColor(colors.ink),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    // Both the ring and the printed shadow paint OUTSIDE the node, so both sit to the
                    // left of the `clip` — [zinelyV21Pressable]'s chain contract, which binds every
                    // hard shadow and not only the pressable ones.
                    .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
                    .zinelyV21HardShadow(FieldShadow, colors.inkLine, shape)
                    .clip(shape)
                    .background(colors.paper)
                    .border(FieldBorder, colors.ink, shape)
                    .defaultMinSize(minHeight = FieldMinHeight)
                    // `.search{padding:var(--gap-sm) var(--gap-md)}` — the vertical half is subsumed
                    // by the 48dp touch minimum, which is larger than 8 + a line box.
                    .padding(horizontal = ZinelyV21Dimens.gapMd),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

/**
 * `.search{border:1.5px solid var(--ink);box-shadow:2px 2px 0 var(--ink-line)}`.
 *
 * The shadow is a **rest depth only** — a field is not pressed, so it takes no
 * [com.aritr.zinely.ui.theme.ZinelyV21Press] tier. 2dp is `.search`'s own value, not `Flat`'s
 * borrowed.
 */
private val FieldBorder = 1.5.dp
private val FieldShadow = 2.dp

/** The frozen 48dp minimum, unchanged — it is a touch target, not a paint value. */
private val FieldMinHeight = 48.dp

/** `.search input{font-size:.86rem}` — the corpus's only field, at the corpus's rem. See the KDoc for
 *  why this is no longer 17sp of Averia. */
private val FieldTextSize = 13.76.sp
