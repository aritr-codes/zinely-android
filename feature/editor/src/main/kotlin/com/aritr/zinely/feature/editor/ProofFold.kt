package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyEasing
import com.aritr.zinely.ui.theme.ZinelyTheme

// Test tags for the Act 3 body the ProofScreen suite and golden address.
public const val ProofFoldGuideTestTag: String = "proof-fold-guide"
public const val ProofFoldDiagramTestTag: String = "proof-fold-diagram"
public const val ProofStepPrevTestTag: String = "proof-step-prev"
public const val ProofStepNextTestTag: String = "proof-step-next"
public const val ProofStepTitleTestTag: String = "proof-step-title"
public const val ProofFinishedTestTag: String = "proof-finished"
public const val ProofDoneHeadingTestTag: String = "proof-done-heading"

/** One fold step: its title, plain-language caption, and the index into [FoldDiagram]'s drawing. */
internal data class FoldStepSpec(val title: String, val caption: String)

/**
 * The five frozen fold steps (`proof.html` `STEPS`, DESIGN-FROZEN) — one crease/cut/wrap at a time, in
 * the exact order and copy of the spec. The last step is where the guide hands off to the climax.
 */
internal val PROOF_FOLD_STEPS: List<FoldStepSpec> = listOf(
    FoldStepSpec(
        "Crease into eight",
        "Fold the sheet in half three times, then open it flat. You’ll see eight panels. " +
            "All folds are valleys.",
    ),
    FoldStepSpec(
        "One cut — the only cut",
        "Fold in half short-end to short-end. Cut the slit across the two middle panels, " +
            "and stop at the quarter lines.",
    ),
    FoldStepSpec(
        "Open it back up",
        "Lay the sheet flat again. The cut has become a small slot right through the middle.",
    ),
    FoldStepSpec(
        "Fold the long way",
        "Fold the sheet in half so the long edges meet — one wide strip. The cut opens into a " +
            "diamond right on the fold.",
    ),
    FoldStepSpec(
        "Push in and wrap",
        "Push the two ends toward the middle so the panels pop into a plus, then wrap them flat — " +
            "front cover facing out.",
    ),
)

internal const val FOLD_LAST_STEP: Int = 4 // PROOF_FOLD_STEPS.lastIndex — the finish-hand-off step.

/**
 * **Act 3 — The Fold** (M5 B4, `proof.html` `#act2`, DESIGN-FROZEN) — the signature climax, the whole
 * delight budget. Two faces, switched by [finished]:
 *
 * - **The guide:** a five-step, pausable fold walkthrough — a stepped schematic diagram (the same
 *   paper/crease/cut vocabulary as Act 1), a live-region step caption, and a prev/dots/next nav. The
 *   step pointer ([step]) and advance/retreat ([onNext]/[onPrev]) are hoisted to [ProofScreen] so the
 *   shared bottom action bar can own the finish button on the last step (`configurePrimary`, RF-1: one
 *   finish action, never a dead primary). ←/→ keys are handled at the [ProofScreen] root.
 * - **The finished book:** revealed in timed beats ([climaxBeat] 0→5, driven by [ProofScreen]) so the
 *   moment lands — the cover swings shut, the book settles onto the desk, a shelf-line draws under it,
 *   then the words and (via the action bar) the exits arrive. Under [reduceMotion] every beat is already
 *   at its final state (the guide never animates the diagram; the book shows closed — un-settled, per the
 *   frozen reduced-motion path — with its words and shelf-line at once).
 *
 * Stateless: all state lives in [ProofScreen]. Haptics are caller-owned there (the `success` verb on
 * finish, a `tick` at the shelf-line beat), not fired here.
 */
@Composable
internal fun ProofFoldAct(
    step: Int,
    finished: Boolean,
    climaxBeat: Int,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (finished) {
        FinishedBook(climaxBeat = climaxBeat, reduceMotion = reduceMotion, modifier = modifier)
    } else {
        FoldGuide(step = step, reduceMotion = reduceMotion, onNext = onNext, onPrev = onPrev, modifier = modifier)
    }
}

/** The five-step guide: lead, stepped diagram, live-region caption, prev/dots/next nav. */
@Composable
private fun FoldGuide(
    step: Int,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier,
) {
    val colors = ZinelyTheme.colors
    val spec = PROOF_FOLD_STEPS[step]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 12.dp)
            .testTag(ProofFoldGuideTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // .lead
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            BasicText(
                text = "Fold it into a book",
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = "Five steps. Take them one at a time — tap the arrow when a step is done.",
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        FoldDiagram(step = step, reduceMotion = reduceMotion)

        // .stepcap — the aria-live region announcing the step title + caption on every change.
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .heightIn(min = 44.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = "${step + 1}. ${spec.title}",
                modifier = Modifier.testTag(ProofStepTitleTestTag),
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = spec.caption,
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        // .stepnav — prev · dots · next.
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepNavButton(
                pathData = ICON_STEP_PREV,
                contentDescription = "Previous step",
                enabled = step > 0,
                onClick = onPrev,
                testTag = ProofStepPrevTestTag,
            )
            StepDots(step = step)
            // The next arrow gives way to the action bar's finish button on the last step (RF-1).
            if (step < FOLD_LAST_STEP) {
                StepNavButton(
                    pathData = ICON_STEP_NEXT,
                    contentDescription = "Next step",
                    enabled = true,
                    onClick = onNext,
                    testTag = ProofStepNextTestTag,
                )
            }
        }
    }
}

/** A 52×52 `.stepnav button`: field-filled, bordered, a single stroked arrow; `opacity:.4` when disabled. */
@Composable
private fun StepNavButton(
    pathData: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    com.aritr.zinely.ui.components.ZToolButton(
        onClick = onClick,
        metrics = com.aritr.zinely.ui.components.ZToolButtonMetrics.ProofStepNav,
        contentDescription = contentDescription,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
        icon = { tint -> FoldGlyph(pathData, tint) },
    )
}

/** Five `.stepdots i`: faint base, `.done` at .6 for passed steps, coral `.cur` for the current one. */
@Composable
private fun StepDots(step: Int) {
    val colors = ZinelyTheme.colors
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PROOF_FOLD_STEPS.indices.forEach { i ->
            val color = when {
                i == step -> colors.coralStrong
                i < step -> colors.onDeskFaint.copy(alpha = 0.6f)
                else -> colors.onDeskFaint.copy(alpha = 0.4f)
            }
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

/**
 * The per-step schematic diagram — a 200×150 viewBox drawn with the frozen paper/crease/cut/active
 * vocabulary. Decorative micro-labels (e.g. "eight panels", "CUT HERE") are painted in; the whole node
 * is a single `role=img` labelled with the step title (`proof.html` inner `<svg role=img aria-label>`).
 * Each step change replays the `diagramIn` crease-in (opacity .25→1, +5px settle over .32s), silenced
 * under reduced motion.
 */
@Composable
private fun FoldDiagram(step: Int, reduceMotion: Boolean) {
    val colors = ZinelyTheme.colors
    val shell = ZinelyTheme.typography.shell
    val measurer = rememberTextMeasurer()
    val title = PROOF_FOLD_STEPS[step].title

    // diagramIn: reset to .25/+5px and settle in on every step change (unless reduced).
    val enter = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(step, reduceMotion) {
        if (reduceMotion) {
            enter.snapTo(1f)
        } else {
            enter.snapTo(0f)
            enter.animateTo(1f, tween(320, easing = ZinelyEasing))
        }
    }

    Canvas(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .aspectRatio(200f / 150f)
            .testTag(ProofFoldDiagramTestTag)
            .graphicsLayer {
                alpha = 0.25f + 0.75f * enter.value
                translationY = (1f - enter.value) * 5.dp.toPx()
            }
            .clearAndSetSemantics {
                role = Role.Image
                contentDescription = title
            },
    ) {
        val d = FoldDiagramScope(this, measurer, colors, shell)
        when (step) {
            0 -> d.step1()
            1 -> d.step2()
            2 -> d.step3()
            3 -> d.step4()
            else -> d.step5()
        }
    }
}

/**
 * The finished book, revealed in beats ([climaxBeat] 0→5): cover swings shut → book settles → shelf-line
 * draws → the words arrive. Under [reduceMotion] the book is shown closed (but **not** settled — the
 * frozen reduced-motion `finishFold` adds shelf/words yet omits `settled`/`close`, proof.html:855) and
 * the words + shelf-line are present at once (the action bar supplies the exits). The heading takes focus
 * once the words land.
 */
@Composable
private fun FinishedBook(climaxBeat: Int, reduceMotion: Boolean, modifier: Modifier) {
    val colors = ZinelyTheme.colors
    val headingFocus = remember { FocusRequester() }

    // Beat gates (climaxBeat is driven in ProofScreen; here we only render its stages). The settle is a
    // full-motion beat only: the frozen reduced-motion path leaves the book un-settled (translateY 0).
    val settled = climaxBeat >= 1 && !reduceMotion
    val shelfIn = climaxBeat >= 2
    val wordsH = climaxBeat >= 3
    val wordsP = climaxBeat >= 4

    // The cover swing: rotateY −148° → 6° → 0° over .95s (coverClose). Under reduced motion, or on a
    // process-death resume where the beats already ran (climaxBeat ≥ 1), the cover is simply shut.
    val cover = remember {
        Animatable(if (reduceMotion || climaxBeat >= 1) 0f else -148f)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (cover.value != -148f) return@LaunchedEffect
        cover.animateTo(
            0f,
            keyframes {
                durationMillis = 950
                (-148f) at 0 using ZinelyEasing
                6f at 570
                0f at 950
            },
        )
    }

    // settle onto the desk (translateY 7px, scale .985), .55s — 0ms under reduced motion.
    val settleFraction by animateFloatAsState(
        targetValue = if (settled) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 550, easing = ZinelyEasing),
        label = "bookSettle",
    )
    val shelfFraction by animateFloatAsState(
        targetValue = if (shelfIn) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 600, easing = ZinelyEasing),
        label = "shelfLine",
    )
    val headFraction by animateFloatAsState(
        targetValue = if (wordsH) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 420, easing = ZinelyEasing),
        label = "doneHead",
    )
    val paraFraction by animateFloatAsState(
        targetValue = if (wordsP) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 420, easing = ZinelyEasing),
        label = "donePara",
    )

    // Land focus on "Your zine is a book." once the words arrive (the reveal's payoff).
    androidx.compose.runtime.LaunchedEffect(wordsH) {
        if (wordsH) runCatching { headingFocus.requestFocus() }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(6.dp)
                .testTag(ProofFinishedTestTag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // .book — pages behind, cover swinging shut in front.
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 212.dp)
                    .graphicsLayer {
                        translationY = settleFraction * 7.dp.toPx()
                        scaleX = 1f - settleFraction * 0.015f
                        scaleY = 1f - settleFraction * 0.015f
                    }
                    .clearAndSetSemantics { },
            ) {
                // .pages — the deeper inner sheet.
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp, 4.dp, 4.dp, 2.dp))
                        .background(colors.paper2),
                )
                // .cover — the front face, swinging on its left spine.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = cover.value
                            cameraDistance = 16f * density
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .clip(RoundedCornerShape(2.dp, 4.dp, 4.dp, 2.dp))
                        .background(colors.paper),
                    contentAlignment = Alignment.Center,
                ) {
                    BookCoverFace()
                }
            }

            // .shelfline — the desk/shelf it now lives on (opacity 0→.5, scaleX .4→1).
            Box(
                Modifier
                    .size(width = 170.dp, height = 2.dp)
                    .graphicsLayer {
                        alpha = shelfFraction * 0.5f
                        scaleX = 0.4f + shelfFraction * 0.6f
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.onDeskFaint)
                    .clearAndSetSemantics { },
            )

            // .done-h — the payoff line; takes focus when it lands.
            BasicText(
                text = "Your zine is a book.",
                modifier = Modifier
                    .focusRequester(headingFocus)
                    .focusProperties { canFocus = wordsH }
                    .focusable()
                    .graphicsLayer {
                        alpha = headFraction
                        translationY = (1f - headFraction) * 8.dp.toPx()
                    }
                    .testTag(ProofDoneHeadingTestTag)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = ZinelyTheme.typography.voice,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
            )
            // .done-p — the calm coda.
            BasicText(
                text = "Eight pages, made by hand, kept on this device. It’s on your shelf whenever you want it.",
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .graphicsLayer {
                        alpha = paraFraction
                        translationY = (1f - paraFraction) * 8.dp.toPx()
                    },
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

/**
 * The front cover face of the finished book — a schematic stand-in (a coral title block over a faint
 * ruled line), matching Act 1's page-number schematic.
 *
 * ponytail: real per-panel cover artwork needs the document tree threaded through the Proof VM seam
 * (deferred with `zineName`, a later batch — see [ProofSheetAct]). The finished-book *moment* is what
 * B4 owns; the cover's real ink follows when the tree arrives.
 */
@Composable
private fun BookCoverFace() {
    val colors = ZinelyTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.coral.copy(alpha = 0.85f)),
        )
        Box(Modifier.size(0.dp, 12.dp))
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.inkFaint.copy(alpha = 0.6f)),
        )
    }
}

// ---- fold-diagram drawing (the frozen 200×150 STEPS geometry) --------------------------------

private const val ICON_STEP_PREV = "M15 5l-7 7 7 7"
private const val ICON_STEP_NEXT = "M9 5l7 7-7 7"

/** A DrawScope helper carrying the diagram's scale + palette, so each step draws in viewBox units. */
private class FoldDiagramScope(
    val ds: DrawScope,
    val measurer: androidx.compose.ui.text.TextMeasurer,
    val colors: com.aritr.zinely.ui.theme.ZinelyColors,
    val shell: FontFamily,
) {
    private val sx = ds.size.width / 200f
    private val sy = ds.size.height / 150f
    private fun o(x: Float, y: Float) = Offset(x * sx, y * sy)
    private fun w(units: Float) = units * sx

    // Vertical creases at 60/100/140; horizontal at y=75; sheet rect (20,25)-(180,125).
    private val vx = floatArrayOf(60f, 100f, 140f)
    private val hy = 75f
    private val x0 = 20f
    private val x1 = 180f
    private val y0 = 25f
    private val y1 = 125f

    private fun sheet(left: Float, top: Float, wUnits: Float, hUnits: Float) {
        ds.drawRect(colors.paper, topLeft = o(left, top), size = Size(w(wUnits), hUnits * sy))
        ds.drawRect(
            colors.paperEdge, topLeft = o(left, top), size = Size(w(wUnits), hUnits * sy),
            style = Stroke(width = w(2f)),
        )
    }

    private fun crease(x: Float, yA: Float, yB: Float, active: Boolean) =
        line(o(x, yA), o(x, yB), active)

    private fun hLine(y: Float, xA: Float, xB: Float, active: Boolean) =
        line(o(xA, y), o(xB, y), active)

    private fun line(a: Offset, b: Offset, active: Boolean) {
        if (active) {
            ds.drawLine(colors.teal, a, b, w(2.4f), cap = StrokeCap.Round)
        } else {
            ds.drawLine(
                colors.inkFaint.copy(alpha = 0.75f), a, b, w(1.4f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(w(4f), w(4f))),
            )
        }
    }

    private fun cut(xA: Float, xB: Float, y: Float) =
        ds.drawLine(colors.coralStrong, o(xA, y), o(xB, y), w(3f), cap = StrokeCap.Round)

    private fun arrow(a: Offset, b: Offset) =
        ds.drawLine(colors.coralStrong, a, b, w(2f), cap = StrokeCap.Round)

    private fun fill(points: List<Offset>) {
        val p = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        ds.drawPath(p, colors.coralStrong)
    }

    private fun stroke(points: List<Offset>, color: Color, wUnits: Float, closed: Boolean = true) {
        val p = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            if (closed) close()
        }
        ds.drawPath(p, color, style = Stroke(width = w(wUnits), join = StrokeJoin.Round))
    }

    private fun label(text: String, cx: Float, cy: Float, valley: Boolean) {
        val style = TextStyle(
            color = if (valley) colors.inkSoft else colors.coralStrong,
            fontFamily = shell,
            fontSize = with(ds) { (if (valley) 9f else 10f).let { (it * sy).toSp() } },
            fontWeight = if (valley) FontWeight.Normal else FontWeight.Bold,
            letterSpacing = if (valley) 0.em else 0.04.em,
        )
        val res = measurer.measure(text, style)
        ds.drawText(res, topLeft = Offset(cx * sx - res.size.width / 2f, cy * sy - res.size.height / 2f))
    }

    fun step1() {
        sheet(20f, 25f, 160f, 100f)
        vx.forEach { crease(it, y0, y1, active = true) }
        hLine(hy, x0, x1, active = true)
        label("eight panels", 100f, 18f, valley = true)
    }

    fun step2() {
        sheet(20f, 25f, 160f, 100f)
        crease(vx[0], y0, y1, active = false)
        crease(vx[1], y0, y1, active = true)
        crease(vx[2], y0, y1, active = false)
        hLine(hy, x0, x1, active = false)
        cut(vx[0], vx[2], hy)
        arrow(o(100f, 44f), o(100f, 66f))
        fill(listOf(o(96f, 44f), o(100f, 36f), o(104f, 44f)))
        label("CUT HERE", 100f, 98f, valley = false)
    }

    fun step3() {
        sheet(20f, 25f, 160f, 100f)
        vx.forEach { crease(it, y0, y1, active = false) }
        hLine(hy, x0, x1, active = false)
        cut(vx[0], vx[2], hy)
        // the slit, opened into a small slot: a teal ellipse around the centre.
        ds.drawOval(
            colors.teal,
            topLeft = o(80f, 71f),
            size = Size(w(40f), 8f * sy),
            style = Stroke(width = w(2f)),
        )
    }

    fun step4() {
        // half-height strip (20,50)-(180,100); the fold = the old centre line at y=50, cut → diamond.
        sheet(20f, 50f, 160f, 50f)
        hLine(50f, x0, x1, active = true)
        stroke(
            listOf(o(85f, 50f), o(100f, 43f), o(115f, 50f), o(100f, 57f)),
            colors.teal, 2.4f, closed = true,
        )
        label("one wide strip", 100f, 42f, valley = true)
    }

    fun step5() {
        // push both ends in, wrap to a book.
        arrow(o(34f, 75f), o(70f, 75f))
        fill(listOf(o(70f, 71f), o(78f, 75f), o(70f, 79f)))
        arrow(o(166f, 75f), o(130f, 75f))
        fill(listOf(o(130f, 71f), o(122f, 75f), o(130f, 79f)))
        sheet(78f, 40f, 44f, 70f)
        crease(100f, 40f, 110f, active = false)
        hLine(75f, 78f, 122f, active = true)
        label("wrap to a book", 100f, 30f, valley = true)
    }
}

/** Draws a `proof.html` 24×24 stroked path at the caller's size, in [tint] (round caps/joins, 2.2px). */
@Composable
private fun FoldGlyph(pathData: String, tint: Color) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(Modifier.fillMaxSize()) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
