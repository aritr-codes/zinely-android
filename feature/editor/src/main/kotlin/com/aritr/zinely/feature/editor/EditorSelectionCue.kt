package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme

internal fun selectionCueTag(base: String): String = "$base-cue"

private val EditorSelectionCueSize = 20.dp
private val EditorSelectionCueGlyphSize = 14.dp
private val EditorSelectionCueBorder = 2.dp

@Composable
internal fun EditorSelectionCue(
    modifier: Modifier = Modifier,
    containerColor: Color = ZinelyTheme.v21Colors.paper,
    // Paper remains physically bright in both themes. `ink` becomes light room-copy in dark mode and
    // disappears on it; `onButter` is the palette's explicit dark-on-bright material ink in both themes.
    contentColor: Color = ZinelyTheme.v21Colors.onButter,
    borderColor: Color = ZinelyTheme.v21Colors.onButter,
) {
    Box(
        modifier = modifier
            .size(EditorSelectionCueSize)
            .background(containerColor, CircleShape)
            .border(EditorSelectionCueBorder, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(EditorSelectionCueGlyphSize),
        )
    }
}
