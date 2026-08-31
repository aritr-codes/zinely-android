package com.aritr.zinely.feature.library

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

public const val ColophonScreenTestTag: String = "colophon-screen"
public const val ColophonBackTestTag: String = "colophon-back"
public const val ColophonPaperGroupTestTag: String = "colophon-paper-group"
public const val ColophonVersionTestTag: String = "colophon-version"
public fun colophonPaperTestTag(paper: PaperSize): String = "colophon-paper-${paper.name.lowercase()}"
public fun colophonTypefaceTestTag(typeface: ColophonTypeface): String =
    "colophon-typeface-${typeface.name.lowercase()}"

public enum class ColophonTypeface(
    public val family: String,
    public val role: String,
    internal val assetPath: String,
) {
    AVERIA_SANS_LIBRE(
        "Averia Sans Libre",
        Copy.Colophon.AVERIA_ROLE,
        "fonts/OFL-AveriaSansLibre.txt",
    ),
    FRAUNCES("Fraunces", Copy.Colophon.FRAUNCES_ROLE, "fonts/OFL-Fraunces.txt"),
    INTER("Inter", Copy.Colophon.INTER_ROLE, "fonts/OFL-Inter.txt"),
}

public sealed interface ColophonDestination {
    public data object Main : ColophonDestination
    public data class Licence(val typeface: ColophonTypeface) : ColophonDestination
}

public suspend fun readBundledTypefaceLicence(context: Context, typeface: ColophonTypeface): String =
    withContext(Dispatchers.IO) {
        runInterruptible {
            context.assets.open(typeface.assetPath).bufferedReader().use { it.readText() }
        }
    }

@Composable
public fun ColophonScreen(
    preferredPaper: PaperSize,
    appVersion: String,
    onPreferredPaperChange: (PaperSize) -> Unit,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier,
    loadLicence: (suspend (ColophonTypeface) -> String)? = null,
) {
    val context = LocalContext.current
    val effectiveLicenceLoader: suspend (ColophonTypeface) -> String = remember(context, loadLicence) {
        loadLicence ?: { typeface -> readBundledTypefaceLicence(context, typeface) }
    }
    var destination by remember { mutableStateOf<ColophonDestination>(ColophonDestination.Main) }
    val typefaceFocusRequesters = remember {
        ColophonTypeface.entries.associateWith { FocusRequester() }
    }
    var returnFocusTo by remember { mutableStateOf<ColophonTypeface?>(null) }

    BackHandler {
        when (val current = destination) {
            ColophonDestination.Main -> onBackToShelf()
            is ColophonDestination.Licence -> {
                returnFocusTo = current.typeface
                destination = ColophonDestination.Main
            }
        }
    }

    LaunchedEffect(destination, returnFocusTo) {
        if (destination == ColophonDestination.Main) {
            returnFocusTo?.let { typefaceFocusRequesters.getValue(it).requestFocus() }
            returnFocusTo = null
        }
    }

    when (val current = destination) {
        ColophonDestination.Main -> ColophonMain(
            preferredPaper = preferredPaper,
            appVersion = appVersion,
            onPreferredPaperChange = onPreferredPaperChange,
            onBack = onBackToShelf,
            onOpenLicence = { destination = ColophonDestination.Licence(it) },
            typefaceFocusRequesters = typefaceFocusRequesters,
            modifier = modifier,
        )
        is ColophonDestination.Licence -> LicenceScreen(
            typeface = current.typeface,
            onBack = {
                returnFocusTo = current.typeface
                destination = ColophonDestination.Main
            },
            loadLicence = effectiveLicenceLoader,
            modifier = modifier,
        )
    }
}

@Composable
private fun ColophonMain(
    preferredPaper: PaperSize,
    appVersion: String,
    onPreferredPaperChange: (PaperSize) -> Unit,
    onBack: () -> Unit,
    onOpenLicence: (ColophonTypeface) -> Unit,
    typefaceFocusRequesters: Map<ColophonTypeface, FocusRequester>,
    modifier: Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val printed = remember { zinelyV21LightColors() }
    LazyColumn(
        modifier
            .fillMaxSize()
            .testTag(ColophonScreenTestTag)
            .background(colors.desk)
            .statusBarsPadding()
            .navigationBarsPadding()
            .semantics { paneTitle = Copy.Colophon.TITLE }
            .padding(horizontal = ZinelyV21Dimens.gapXl),
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapLg),
    ) {
        item { ColophonHeader(Copy.Colophon.BACK_TO_SHELF, onBack, Modifier.testTag(ColophonBackTestTag)) }
        item {
            Text(
                Copy.Colophon.TITLE,
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = ZinelyV21Fonts.Voice,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                ),
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Text(
                Copy.Colophon.TAGLINE,
                color = colors.ink,
                fontFamily = ZinelyV21Fonts.Editorial,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
            )
        }
        item { BodyText(Copy.Colophon.INTRO) }
        item { SectionHeading(Copy.Colophon.DEFAULT_PAPER) }
        item {
            Column(
                Modifier
                    .testTag(ColophonPaperGroupTestTag)
                    .fillMaxWidth()
                    .background(printed.surface, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                    .padding(ZinelyV21Dimens.gapMd)
                    .semantics { }
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs),
            ) {
                listOf(PaperSize.A4, PaperSize.LETTER).forEach { paper ->
                    val selected = paper == preferredPaper
                    Row(
                        Modifier
                            .testTag(colophonPaperTestTag(paper))
                            .fillMaxWidth()
                            .sizeIn(minHeight = 48.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { if (!selected) onPreferredPaperChange(paper) },
                            )
                            .padding(horizontal = ZinelyV21Dimens.gapSm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = printed.ink,
                                unselectedColor = printed.inkSoft,
                            ),
                        )
                        Text(
                            if (paper == PaperSize.A4) Copy.Paper.A4 else Copy.Paper.LETTER,
                            color = printed.ink,
                            fontFamily = ZinelyV21Fonts.Work,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = ZinelyV21Dimens.gapSm),
                        )
                    }
                }
            }
        }
        item { BodyText(Copy.Colophon.PAPER_EXPLANATION) }
        item { SectionHeading(Copy.Colophon.TYPEFACES) }
        items(ColophonTypeface.entries, key = { it.name }) { typeface ->
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Row(
                Modifier
                    .testTag(colophonTypefaceTestTag(typeface))
                    .focusRequester(typefaceFocusRequesters.getValue(typeface))
                    .fillMaxWidth()
                    .background(printed.surface, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                    .zinelyV2Control(
                        label = Copy.Colophon.licenceButton(typeface.family),
                        interactionSource = interaction,
                        onClick = { onOpenLicence(typeface) },
                    )
                    .padding(ZinelyV21Dimens.gapLg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(typeface.family, color = printed.ink, fontWeight = FontWeight.Bold)
                    Text(typeface.role, color = printed.inkSoft, fontSize = 13.sp)
                    Text(Copy.Colophon.LICENCE_ACTION, color = printed.leafText, fontSize = 12.sp)
                }
                Text("›", color = printed.inkSoft, fontSize = 24.sp)
            }
        }
        item { SectionHeading(Copy.Colophon.HOW_IT_WORKS) }
        item {
            BodyText(
                Copy.Colophon.OFFLINE_PROMISE,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.leafTint, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                    .padding(ZinelyV21Dimens.gapLg),
                color = colors.onLeaf,
            )
        }
        item { SectionHeading(Copy.Colophon.VERSION) }
        item { BodyText(appVersion, Modifier.testTag(ColophonVersionTestTag)) }
        item { Spacer(Modifier.height(ZinelyV21Dimens.gapXl)) }
    }
}

@Composable
private fun LicenceScreen(
    typeface: ColophonTypeface,
    onBack: () -> Unit,
    loadLicence: suspend (ColophonTypeface) -> String,
    modifier: Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val printed = remember { zinelyV21LightColors() }
    val result by produceState<Result<String>?>(null, typeface, loadLicence) {
        value = runCatching { loadLicence(typeface) }
    }
    Column(
        modifier
            .fillMaxSize()
            .testTag(ColophonScreenTestTag)
            .background(colors.desk)
            .statusBarsPadding()
            .navigationBarsPadding()
            .semantics { paneTitle = typeface.family }
            .padding(horizontal = ZinelyV21Dimens.gapXl),
    ) {
        ColophonHeader(Copy.Colophon.BACK_TO_COLOPHON, onBack, Modifier.testTag(ColophonBackTestTag))
        SectionHeading(typeface.family)
        Text(Copy.Colophon.LICENCE_TITLE, color = colors.inkSoft, fontSize = 13.sp)
        Spacer(Modifier.height(ZinelyV21Dimens.gapLg))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(printed.surface, RoundedCornerShape(ZinelyV21Dimens.radiusMd))
                .padding(ZinelyV21Dimens.gapLg),
            contentAlignment = Alignment.Center,
        ) {
            when {
                result == null -> CircularProgressIndicator(color = printed.ink)
                result?.isSuccess == true -> SelectionContainer {
                    Text(
                        result!!.getOrThrow(),
                        color = printed.ink,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    )
                }
                else -> BodyText(Copy.Colophon.LICENCE_UNAVAILABLE, color = printed.inkSoft)
            }
        }
        Spacer(Modifier.height(ZinelyV21Dimens.gapXl))
    }
}

@Composable
private fun ColophonHeader(label: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Text(
        "‹",
        color = colors.ink,
        fontSize = 34.sp,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .zinelyV2Control(label = label, interactionSource = interaction, onClick = onBack)
            .padding(top = ZinelyV21Dimens.gapSm),
    )
}

@Composable
private fun SectionHeading(text: String) {
    val colors = ZinelyTheme.v21Colors
    Text(
        text,
        color = colors.ink,
        fontFamily = ZinelyV21Fonts.Work,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ZinelyTheme.v21Colors.inkSoft,
) {
    Text(
        text,
        modifier,
        color = color,
        fontFamily = ZinelyV21Fonts.Work,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
}
