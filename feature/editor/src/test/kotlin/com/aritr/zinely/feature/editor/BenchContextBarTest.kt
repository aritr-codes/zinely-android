package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The frozen `.ctx` verb bar, measured against the CSS that specifies it
 * ([ADR-092](../../../../../../../../../docs/DECISIONS.md#adr-092) rows 2.10–2.13a).
 *
 * The verb sets are asserted as **whole lists**, not as membership: `toolsFor()` is an ordered set per
 * kind, and a permutation would satisfy "each verb exists" while being the wrong bar.
 *
 * What this file deliberately does **not** claim is `Font`'s disabled state as seen by TalkBack. The
 * Compose semantics assertion below is necessary and insufficient — [ADR-058](../../../../../../../../../docs/DECISIONS.md#adr-058)'s
 * `ReframeControls.ZoomButton` passed `assertIsNotEnabled` here while telling the *platform* it was
 * enabled. Row 2.13a's real gate is `uiautomator dump` on hardware.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
class BenchContextBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HOST = "ctx-host"

        /** Neither `paper` nor `ink`, so the bar's own fill and border are both distinguishable. */
        val BACKDROP = Color(0xFF102030)
    }

    private var surfaceArgb: Int = 0
    private var inkArgb: Int = 0
    private var jamTextArgb: Int = 0
    private var inkSoftArgb: Int = 0
    private var leafArgb: Int = 0

    private fun host(verbs: List<BenchVerb>, visible: Boolean = true, fontScale: Float = 1f) {
        composeRule.setContent {
            ZinelyTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                    surfaceArgb = ZinelyTheme.v21Colors.surface.toArgb()
                    inkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                    jamTextArgb = ZinelyTheme.v21Colors.jamText.toArgb()
                    inkSoftArgb = ZinelyTheme.v21Colors.inkSoft.toArgb()
                    leafArgb = ZinelyTheme.v21Colors.leaf.toArgb()
                    Box(
                        Modifier.size(360.dp, 200.dp).testTag(HOST).background(BACKDROP),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        BenchContextBar(
                            visible = visible,
                            verbs = verbs,
                            onVerb = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun hostBitmap(): Bitmap {
        composeRule.waitForIdle()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val r = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        return Bitmap.createBitmap(full, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
    }

    // ── Row 2.13 — the frozen verb sets ─────────────────────────────────────────────────────────────

    @Test
    fun `the text verbs include the frozen duplicate action before Delete`() {
        assertEquals(
            listOf(
                Copy.BenchVerbs.EDIT,
                Copy.BenchVerbs.FONT,
                Copy.BenchVerbs.SIZE,
                Copy.BenchVerbs.INK,
                Copy.BenchVerbs.DUPLICATE,
                Copy.BenchVerbs.DELETE,
            ),
            benchContextVerbs(BenchVerbKind.TEXT).map { it.label },
        )
    }

    @Test
    fun `the photo verbs include Flip and the frozen spread action in order`() {
        // `Copier` is the amendment ADR-106 made to this freeze, in `v21-bench.html:690` before it was
        // made here. Order is asserted, not just membership: a permutation is the wrong bar.
        assertEquals(
            listOf(
                Copy.BenchVerbs.REFRAME,
                Copy.BenchVerbs.FLIP,
                Copy.BenchVerbs.ACROSS_FOLD,
                Copy.BenchVerbs.COPIER,
                Copy.BenchVerbs.REPLACE,
                Copy.BenchVerbs.DUPLICATE,
                Copy.BenchVerbs.DELETE,
            ),
            benchContextVerbs(BenchVerbKind.PHOTO).map { it.label },
        )
    }

    @Test
    fun `Copier is live, unlike the two verbs the bar draws without a behaviour`() {
        val copier = benchContextVerbs(BenchVerbKind.PHOTO).single { it.label == Copy.BenchVerbs.COPIER }
        assertEquals(true, copier.enabled)
        assertEquals(false, copier.danger)
        host(benchContextVerbs(BenchVerbKind.PHOTO))
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.COPIER}").assertIsEnabled()
    }

    @Test
    fun `Copier announces which way it is set, and Reframe and Delete announce no state at all`() {
        // Review caught this shipping as a stateless button: announced "Copier" before the tap and
        // "Copier" after it, with the only difference on a canvas a screen reader cannot read.
        host(benchContextVerbs(BenchVerbKind.PHOTO, copierOn = true))
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.COPIER}")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, Copy.BenchVerbs.COPIER_ON))
        // The live ordinary verbs must NOT gain one — a state on a control that has none is noise.
        //
        // `Replace` is deliberately absent from this list, and the omission is the interesting part: it
        // ships disabled and therefore DOES carry a state, its `unavailableBecause` reason. Both features
        // ride `stateDescription` and they cannot collide (a disabled verb has no setting; a toggle is
        // live by construction) — but a blanket "only Copier has a state" would have been false, and the
        // first draft of this test asserted exactly that.
        for (label in listOf(
            Copy.BenchVerbs.REFRAME,
            Copy.BenchVerbs.FLIP,
            Copy.BenchVerbs.ACROSS_FOLD,
            Copy.BenchVerbs.DUPLICATE,
            Copy.BenchVerbs.DELETE,
        )) {
            composeRule.onNodeWithTag("$BenchContextBarTestTag-$label")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription).not())
        }
    }

    @Test
    fun `Copier off announces off, not silence`() {
        host(benchContextVerbs(BenchVerbKind.PHOTO, copierOn = false))
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.COPIER}")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, Copy.BenchVerbs.COPIER_OFF))
    }

    /**
     * **D-082 Q4 — a toggle that looks the same either way.** Measured on device (SM-A176B / Android 16):
     * the bar before the tap and after it were pixel-identical, so the only feedback was the halftone on
     * the canvas — nothing at all when the photo is small, scrolled off, or behind this bar. The freeze
     * gained `.ctx button.on{background:var(--leaf);color:var(--on-leaf)}` (amendment A4) first.
     *
     * The **ground** is the assertion, not the mark: in light theme `onLeaf` and `paper` are the same
     * cream (`#FFF6E8`), so a glyph-colour probe would read identically on and off and prove nothing.
     */
    @Test
    fun `Copier on is drawn on a leaf ground, and its neighbours are not`() {
        host(benchContextVerbs(BenchVerbKind.PHOTO, copierOn = true))
        val bmp = hostBitmap()
        val on = groundOf(bmp, Copy.BenchVerbs.COPIER)
        assertTrue(
            "a Copier that is on sits on leaf, not on the card's paper",
            dist(on, leafArgb) < dist(on, surfaceArgb),
        )
        // The neighbours must not gain the ground with it — a bar that fills every verb says nothing.
        val reframe = groundOf(bmp, Copy.BenchVerbs.REFRAME)
        assertTrue("Reframe is not a toggle and keeps the tool surface", dist(reframe, surfaceArgb) < dist(reframe, leafArgb))
    }

    /** The other half of the pair — [host] may set content only once, so the off state is its own test. */
    @Test
    fun `Copier off keeps the bar's own paper, so the setting is the visible difference`() {
        host(benchContextVerbs(BenchVerbKind.PHOTO, copierOn = false))
        val off = groundOf(hostBitmap(), Copy.BenchVerbs.COPIER)
        assertTrue(
            "an off Copier draws no ground of its own",
            dist(off, surfaceArgb) < dist(off, leafArgb),
        )
    }

    @Test
    fun `Copier adds a visible state cue only while on`() {
        host(benchContextVerbs(BenchVerbKind.PHOTO, copierOn = true))
        composeRule.onNodeWithTag(
            benchVerbStateCueTag(Copy.BenchVerbs.COPIER),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun `Copier off removes the visible state cue`() {
        host(benchContextVerbs(BenchVerbKind.PHOTO, copierOn = false))
        composeRule.onNodeWithTag(
            benchVerbStateCueTag(Copy.BenchVerbs.COPIER),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun `the universal Duplicate and Delete are the only verbs shared by text and photo`() {
        val text = benchContextVerbs(BenchVerbKind.TEXT).map { it.label }.toSet()
        val photo = benchContextVerbs(BenchVerbKind.PHOTO).map { it.label }.toSet()
        // The premise of OD-11's "keep both bars": these vocabularies barely overlap, and the one verb
        // they share with the transform bar is the same one they share with each other.
        assertEquals(setOf(Copy.BenchVerbs.DUPLICATE, Copy.BenchVerbs.DELETE), text intersect photo)
    }

    /**
     * Given a `DecorElement`, when its verb set is asked for, A22 places Flip after Replace / Ink,
     * followed by universal Duplicate and destructive Delete.
     *
     * Replaces `the decor set fails loudly instead of defaulting to empty`, which asserted the
     * `error(... OD-2)` that used to sit here. That fence expired by its own terms — OD-2 re-seated
     * `DecorElement` *"beyond Phase C"*, Phase C completed 2026-08-06, and D-029's 2026-08-16 ruling
     * assigns ADR-105 as the phase that takes it. The old test is not weakened, it is *superseded*:
     * loud failure was the right behaviour for an unreachable kind and the wrong behaviour for a
     * reachable one, and the thing it was really guarding — "a bar with no verbs" — is asserted below.
     */
    @Test
    fun `the decor set includes Flip and Duplicate immediately before Delete`() {
        val decor = benchContextVerbs(BenchVerbKind.DECOR).map { it.label }
        assertEquals(
            listOf(
                Copy.BenchVerbs.REPLACE,
                Copy.BenchVerbs.INK,
                Copy.BenchVerbs.FLIP,
                Copy.BenchVerbs.DUPLICATE,
                Copy.BenchVerbs.DELETE,
            ),
            decor,
        )
    }

    @Test
    fun `every element kind yields a non-empty bar - no kind can render verbless`() {
        // The invariant the retired `error(...)` was standing in for, now asserted positively and over
        // ALL kinds rather than the one that happened to be unreachable.
        BenchVerbKind.entries.forEach { kind ->
            assertTrue("$kind must have verbs", benchContextVerbs(kind).isNotEmpty())
        }
    }

    @Test
    fun `every decor verb is live, and the row no longer carries an inert control`() {
        // ⚠ **The whole row has now flipped, one package at a time.** Ink went live with `Intent.InkSupply`;
        // Replace went live with `Intent.ReplaceSupply` plus the Art sheet re-summoned as a picker. Decor is
        // the first element kind whose frozen verb set is fully implemented — OD-9's "drawn and inert"
        // class, which this row wore for two packages, no longer applies to any of its three controls.
        val byLabel = benchContextVerbs(BenchVerbKind.DECOR).associateBy { it.label }
        assertTrue(byLabel.getValue(Copy.BenchVerbs.REPLACE).enabled)
        assertTrue(byLabel.getValue(Copy.BenchVerbs.FLIP).enabled)
        assertTrue(byLabel.getValue(Copy.BenchVerbs.DUPLICATE).enabled)
        assertTrue(byLabel.getValue(Copy.BenchVerbs.INK).enabled)
        assertTrue(byLabel.getValue(Copy.BenchVerbs.DELETE).enabled)
        // No enabled verb may still be claiming a reason for being unavailable — that text is spoken.
        assertTrue(byLabel.values.all { it.unavailableBecause == null })
        // Delete stays the only red control on the row.
        assertTrue(byLabel.getValue(Copy.BenchVerbs.DELETE).danger)
        assertFalse(byLabel.getValue(Copy.BenchVerbs.INK).danger)
        assertFalse(byLabel.getValue(Copy.BenchVerbs.REPLACE).danger)
        assertFalse(byLabel.getValue(Copy.BenchVerbs.FLIP).danger)
        assertFalse(byLabel.getValue(Copy.BenchVerbs.DUPLICATE).danger)
    }

    /**
     * The defect D-029's ruling names by hand: `benchVerbKindOf` used to end `else -> null`, so a
     * `DecorElement` would have produced **no context bar and no compile error** — a supply selectable
     * and verbless, forever, under a green suite. This asserts the total mapping directly.
     */
    @Test
    fun `benchVerbKindOf is total - every element kind maps to a kind`() {
        val t = Transform(0.0, 0.0, 10.0, 10.0)
        assertEquals(BenchVerbKind.TEXT, benchVerbKindOf(TextElement("t", t, 0, "hi")))
        assertEquals(BenchVerbKind.PHOTO, benchVerbKindOf(ImageElement("i", t, 0, "a".repeat(64))))
        assertEquals(
            BenchVerbKind.DECOR,
            benchVerbKindOf(DecorElement("d", t, 0, "tape.torn", ColorRgba.BLACK)),
        )
    }

    @Test
    fun `only Font and Replace are drawn without a behaviour`() {
        val inert = (benchContextVerbs(BenchVerbKind.TEXT) + benchContextVerbs(BenchVerbKind.PHOTO))
            .filterNot { it.enabled }
            .map { it.label }
            .toSet()
        assertEquals(setOf(Copy.BenchVerbs.FONT, Copy.BenchVerbs.REPLACE), inert)
    }

    // ── Row 2.13a — drawn, and not operable ─────────────────────────────────────────────────────────

    @Test
    fun `Font is present and not enabled, while its neighbours are`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.FONT}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}").assertIsEnabled()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.SIZE}").assertIsEnabled()
    }

    /**
     * **F-1 — a control that is drawn and disabled says why.**
     *
     * [OD-9](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) keeps `Font` and `Replace`
     * drawn and forbids inventing a capability for them. It does not make them mute, and a first-time
     * device pass found that silence is what reads as breakage rather than as "not built yet"
     * (`docs/BETA-UX-REVIEW.md` F-1). Explaining an absence invents nothing.
     *
     * The reason rides `stateDescription`, **not** the name: `Font` stays `Font`, so the verb-set
     * assertions above keep working and TalkBack announces a state rather than a differently-named control.
     *
     * The two reasons are asserted apart because they are answerable in opposite ways — `NOT_YET` is a
     * capability the product lacks, `TYPE_FIRST` is one move the user can make right now. A single
     * "unavailable" would throw away the half that is actionable.
     */
    @Test
    fun `a drawn but disabled verb announces why, and an enabled one announces no state`() {
        host(benchContextVerbs(BenchVerbKind.TEXT, styleable = false))

        fun config(label: String) = composeRule
            .onNodeWithTag("$BenchContextBarTestTag-$label")
            .fetchSemanticsNode()
            .config

        fun state(label: String) = config(label).getOrNull(SemanticsProperties.StateDescription)

        assertEquals(Copy.BenchVerbs.NOT_YET, state(Copy.BenchVerbs.FONT))
        assertEquals(Copy.BenchVerbs.TYPE_FIRST, state(Copy.BenchVerbs.SIZE))
        assertEquals(Copy.BenchVerbs.TYPE_FIRST, state(Copy.BenchVerbs.INK))
        assertEquals(Copy.BenchVerbs.TYPE_FIRST, state(Copy.BenchVerbs.DUPLICATE))
        assertEquals("an enabled verb must not carry a reason", null, state(Copy.BenchVerbs.EDIT))

        // The name is untouched — this is the assertion that fails if the reason ever migrates into it.
        assertEquals(
            listOf(Copy.BenchVerbs.FONT),
            config(Copy.BenchVerbs.FONT).getOrNull(SemanticsProperties.ContentDescription),
        )
    }

    // ── Row 2.11 — `flex:1` is GONE, and `min-width:50px` replaces it ───────────────────────────────

    /**
     * **Inverted by ADR-102 P4.** V2's `.ctx button` carried `flex:1` inside a bar pinned
     * `left:12px;right:12px`, so the verbs shared the canvas's whole width between them. V2.1 drops
     * both: the verbs are sized by `min-width:50px` (`v21-bench.html:230`) plus their own content, and the
     * bar is a content-width pill centred on the canvas (`:223`). So the assertion is turned around — the
     * card must be **narrower than the space it is offered**, and no verb may fall under the frozen floor.
     *
     * Kept rather than deleted because `flex:1` returning would be invisible everywhere else: a
     * full-width row still looks like a toolbar, and the golden's 2 % threshold would not notice.
     */
    @Test
    fun `a verb sits on the fifty dp floor and the bar does not stretch to the canvas`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val widths = benchContextVerbs(BenchVerbKind.TEXT).associate { verb ->
            verb.label to composeRule.onNodeWithTag("$BenchContextBarTestTag-${verb.label}")
                .fetchSemanticsNode().boundsInWindow.width
        }
        val floor = with(composeRule.density) { BenchContextBarButtonMinWidthDp.toPx() }
        widths.forEach { (label, w) ->
            assertTrue("$label measures ${w}px, under the frozen 50dp floor of ${floor}px", w >= floor - 1f)
        }
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        // The card is its six verbs plus five A21 1dp gaps plus 4dp of padding a side — still less than
        // the 360dp host. Under V2's `flex:1` strip it would be the host less 24dp.
        val sum = widths.values.sum() +
            with(composeRule.density) { (BenchContextBarGapDp * 5 + BenchContextBarPaddingDp * 2).toPx() }
        assertEquals("the card is exactly its content", sum.toDouble(), bar.width.toDouble(), 1.5)
        assertTrue(
            "the card measures ${bar.width}px against a ${hostB.width}px host — it is still a strip",
            bar.width < hostB.width,
        )
    }

    @Test
    fun `the frozen verb clears the 48dp touch floor on its own`() {
        // V2 drew a 40dp control and leaned on Compose's pointer-input minimum to reach Material's 48dp
        // floor. V2.1's `.ctx button` declares no height at all, and its own parts — 8 + 17 + 2 + ~15 + 8
        // — stack to 50, which is also the `min-width` the freeze gives it. So the DRAWN box now clears
        // the floor without help, and the two readings that used to diverge agree. Asserted so nobody
        // later "fixes" the bar back down under it.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val h = composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}")
            .fetchSemanticsNode().boundsInWindow.height
        val floor = with(composeRule.density) { 48.dp.toPx() }
        val drawn = with(composeRule.density) { BenchContextBarButtonHeightDp.toPx() }
        assertEquals(drawn.toDouble(), h.toDouble(), 1.0)
        assertTrue("the drawn height clears the target floor by itself", drawn >= floor)
    }

    @Test
    fun `the decor labels can grow at font scale 1_8 without spilling outside their buttons or bar`() {
        host(benchContextVerbs(BenchVerbKind.DECOR), fontScale = 1.8f)

        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val minButtonHeight = with(composeRule.density) { BenchContextBarButtonHeightDp.toPx() }
        listOf(
            Copy.BenchVerbs.REPLACE,
            Copy.BenchVerbs.INK,
            Copy.BenchVerbs.FLIP,
            Copy.BenchVerbs.DUPLICATE,
            Copy.BenchVerbs.DELETE,
        ).forEach { label ->
            val button = composeRule.onNodeWithTag("$BenchContextBarTestTag-$label")
                .fetchSemanticsNode().boundsInWindow
            assertTrue(
                "$label should grow beyond the 50dp resting minimum at fontScale 1.8; got $button",
                button.height > minButtonHeight + 1.5f,
            )
            assertTrue(
                "$label must stay inside the bar at fontScale 1.8; button=$button bar=$bar",
                button.top >= bar.top - 1f && button.bottom <= bar.bottom + 1f,
            )
        }
    }

    @Test
    fun `the seven photo verbs stay in a phone-width scrollable pill at font scale 1_8`() {
        host(benchContextVerbs(BenchVerbKind.PHOTO), fontScale = 1.8f)

        val hostBounds = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        assertEquals(hostBounds.left.toDouble(), bar.left.toDouble(), 1.0)
        assertEquals(hostBounds.right.toDouble(), bar.right.toDouble(), 1.0)

        val duplicate = composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DUPLICATE}")
        val delete = composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}")
        duplicate.performScrollTo().assertHasClickAction()
        delete.performScrollTo().assertHasClickAction()
        val deleteBounds = delete.fetchSemanticsNode().boundsInWindow
        assertTrue(
            "Delete must be reachable inside the scrolled pill at fontScale 1.8; button=$deleteBounds bar=$bar",
            deleteBounds.left >= bar.left - 1f && deleteBounds.right <= bar.right + 1f,
        )
    }

    // ── Row 2.10 — the bar's own geometry ───────────────────────────────────────────────────────────

    /**
     * **Rewritten by ADR-102 P4.** V2 pinned `.ctx` at `left:12px;right:12px;bottom:12px` — a full-width
     * strip. V2.1 pins it at `left:50%;transform:translateX(-50%);bottom:12px` (`v21-bench.html:223`): a
     * content-width pill, centred, 12dp off the foot. So the bottom inset is still 12 and the horizontal
     * insets are no longer a specification at all — what replaces them is that the two side gaps are
     * *equal* and that neither is 12.
     */
    @Test
    fun `the bar is twelve dp off the bottom edge and centred, not inset`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        // The frozen 12, transcribed HERE from `v21-bench.html:223` rather than read from the production
        // constant. Reading the constant made this test agree with any value the constant took - an
        // inset-12->0 mutation survived it, which is the whole reason the number is written out twice.
        val inset = with(composeRule.density) { 12.dp.toPx() }.toInt()

        val barBounds = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val hostBounds = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        assertEquals(inset.toDouble(), (hostBounds.bottom - barBounds.bottom).toDouble(), 1.0)

        val leftGap = barBounds.left - hostBounds.left
        val rightGap = hostBounds.right - barBounds.right
        assertEquals("the card is centred, so its two side gaps are equal", leftGap.toDouble(), rightGap.toDouble(), 1.0)
        assertTrue(
            "…and A21's six-action card remains content-width rather than becoming a full strip: leftGap=$leftGap",
            leftGap > 0f,
        )

        // **Inverted:** V2's `0 12px 30px -12px` shadow tinted the pixels around the bar, and this probe
        // asserted the corner was NOT pure backdrop because of it. `.ctx` now declares no shadow of either
        // material — the freeze's own banner (`v21-bench.html:220-222`) says a tool does not perform — so
        // the corner is exactly the backdrop again, and any shadow creeping back fails here.
        assertEquals("the bar throws no shadow onto the canvas", BACKDROP.toArgb(), bmp.getPixel(2, bmp.height - 3))
    }

    @Test
    fun `the bar is a pill, not a rounded rectangle`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val left = (bar.left - hostB.left).toInt()
        val top = (bar.top - hostB.top).toInt()
        // `border-radius:var(--br-pill)` on a card whose height is 4 + 50 + 4 = 58dp, so the arc's radius
        // is half of that: the corner is cut far more deeply than V2's 16dp was.
        val r = (bar.height / 2f).toInt()

        // Probe INSIDE a 16dp arc but still OUTSIDE the pill's. A point `a` in from both edges is outside
        // an arc of radius `r` when `a < r(1 - 1/√2)`, i.e. `a < .293r`; `.22r` clears that for the pill
        // while sitting comfortably inside the smaller arc V2 drew. Written as a fraction of the measured
        // radius rather than as a pixel count, so it does not encode this raster's density.
        val a = (r * 0.22f).toInt()
        assertNotEquals("the corner is cut by the pill radius", surfaceArgb, bmp.getPixel(left + a, top + a))
        assertEquals("past the arc, the top edge is the bar's fill", surfaceArgb, bmp.getPixel(left + r + 6, top + 5))
    }

    @Test
    fun `the bar is not in the tree at all when it is not showing`() {
        // Row 2.13c's floor: hidden means absent, not transparent — a bar that is merely invisible would
        // still take the taps aimed at the page beneath it.
        host(benchContextVerbs(BenchVerbKind.TEXT), visible = false)
        composeRule.onNodeWithTag(BenchContextBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `the bar enters from exactly 8dp below where it settles`() {
        // Row 2.10's enter is a FIXED 8px rise (V2 rose 14), not a fraction of the bar's height — so the assertion has
        // to catch it mid-flight and compare against where it lands, which is the only way to tell 14 from
        // "some slide". The clock is driven by hand: with autoAdvance on, the animation is over before the
        // first measurement and every enter offset looks identical.
        var show by mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ZinelyTheme {
                Box(
                    Modifier.size(360.dp, 200.dp).testTag(HOST).background(BACKDROP),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BenchContextBar(
                        visible = show,
                        verbs = benchContextVerbs(BenchVerbKind.TEXT),
                        onVerb = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        show = true
        // Recomposition first (the node has to exist before it can be measured), then one frame of the
        // enter. Advancing the clock without this measures a node that is not in the tree yet.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val start = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow.top
        composeRule.mainClock.advanceTimeBy(BenchContextBarEnterMillis + 100L)
        val settled = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow.top
        val expected = with(composeRule.density) { BenchContextBarEnterOffsetDp.toPx() }
        // A couple of frames of easing have already run at `start`, so the gap is at most the full 8dp
        // and unmistakably more than zero — which is what an `8 -> 0` mutation removes entirely.
        assertTrue("entered from below: $start vs $settled", start > settled)
        assertTrue("and by no more than the frozen 8dp", start - settled <= expected + 1f)
    }

    // ── Row 2.12 — `.danger` ────────────────────────────────────────────────────────────────────────

    @Test
    fun `Delete is drawn in jam-text, and its neighbours are not`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        // The darkest pixel of each control is its glyph. Delete's must differ from Edit's: dropping
        // `.danger` makes them identical, which is exactly the mutation this kills.
        assertNotEquals(inkOf(bmp, Copy.BenchVerbs.EDIT), inkOf(bmp, Copy.BenchVerbs.DELETE))

        // …and differing is not enough: `.ctx button.danger{color:var(--jam-text)}` (`v21-bench.html:232`)
        // names ONE colour — jam is V2.1's only urgent one. Swapping it for any other dark tint would
        // survive the line above, so the glyph is placed against both candidates and must land nearer
        // `jamText` than the `inkSoft` its neighbours use — while Edit's lands the other way round.
        // (Nearer, not equal: the sampled pixel is an antialiased stroke carrying some `paper` with it.)
        assertTrue(
            "Delete's glyph is drawn in jamText, not in inkSoft",
            dist(inkOf(bmp, Copy.BenchVerbs.DELETE), jamTextArgb) <
                dist(inkOf(bmp, Copy.BenchVerbs.DELETE), inkSoftArgb),
        )
        assertTrue(
            "…and Edit's is drawn in inkSoft, not in jamText",
            dist(inkOf(bmp, Copy.BenchVerbs.EDIT), inkSoftArgb) <
                dist(inkOf(bmp, Copy.BenchVerbs.EDIT), jamTextArgb),
        )
    }

    // ── Rows 2.10 / 2.13a — the two axes `clearAndSetSemantics` silently wipes ──────────────────────

    @Test
    fun `every enabled verb publishes a click action, and the inert ones publish none`() {
        // The review finding this file exists to not repeat. `clearAndSetSemantics` wipes `OnClick` by the
        // same rule it wipes the disabled state, and the first version of this file re-published only the
        // latter — so every verb announced itself as a button exposing no way to activate it. A
        // pointer tap still worked (it never consults semantics), which is precisely why nothing caught
        // it. `uiautomator dump` on device would have read `clickable="false"` on every verb.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        for (verb in benchContextVerbs(BenchVerbKind.TEXT)) {
            val node = composeRule.onNodeWithTag("$BenchContextBarTestTag-${verb.label}")
            if (verb.enabled) node.assertHasClickAction() else node.assertHasNoClickAction()
        }
    }

    @Test
    fun `an inert verb is drawn inert, and not merely announced so`() {
        // Row 2.13a's other half: without the alpha the control says "disabled" to TalkBack and "tap me"
        // to the eye. `.icon-btn:disabled{opacity:.35}` (`v21-bench.html:345`) is the corpus's own answer,
        // transcribed here rather than read from production so the constant and the assertion cannot agree
        // with each other on a wrong value.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        val font = inkOf(bmp, Copy.BenchVerbs.FONT)
        val edit = inkOf(bmp, Copy.BenchVerbs.EDIT)
        assertNotEquals("Font is dimmed; Edit is not", edit, font)
        // .35 alpha over `paper` lands between the two, and much nearer paper than full inkSoft.
        assertTrue("the dimmed glyph is lighter than the live one", luma(font) > luma(edit))
        assertTrue("…and still darker than the surface it sits on", luma(font) < luma(surfaceArgb))
        assertTrue(
            "the dim is the frozen .35, not an arbitrary fade",
            dist(font, inkSoftArgb) > dist(font, surfaceArgb),
        )
    }

    @Test
    fun `the bar carries the frozen 1_5dp ink border`() {
        // Row 2.10 claimed a border nothing read. The straight middle of the left edge is unaffected by
        // the corner arcs, so the ring there is the border colour and nothing else. V2.1 draws it in real
        // `ink` at 1.5dp, where V2 used a 1dp `--chrome-line`.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val barB = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val x = (barB.left - hostB.left).toInt() + 1
        val y = ((barB.top + barB.bottom) / 2f - hostB.top).toInt()
        assertTrue(
            "the bar's edge is ink, not its own paper fill",
            dist(bmp.getPixel(x, y), inkArgb) < dist(bmp.getPixel(x, y), surfaceArgb),
        )
    }

    // ── Pixel helpers ───────────────────────────────────────────────────────────────────────────────

    /**
     * The darkest pixel inside a verb's box — its glyph, whatever the glyph happens to be.
     *
     * **The probe, not the paint.** It used to scan the whole box inset 2px, and once V2.1 made `.ctx`
     * a content-width pill that read the wrong thing entirely. The bar's radius is now half its height
     * (29dp), and the card's own padding is 4dp — so the arc at each END of the pill cuts *through* the
     * first and last verbs' boxes. Inside Edit's box and inside Delete's box the corners are therefore
     * canvas, and [BACKDROP] (`#102030`, luma 96) is darker than any glyph drawn in `inkSoft` or
     * `jamText` on `paper`. Both ends returned the same backdrop pixel, and `assertNotEquals` failed on
     * two verbs that are painted in different colours. The `ink` border traced by that same arc is the
     * other non-glyph the old range could reach.
     *
     * So the scan is narrowed to the middle 40 % of the box, which the pill's arc cannot reach at any
     * y the box occupies, and which always contains the whole 17dp icon — the glyph the caller asked
     * for. Written as a fraction of the measured box rather than as a pixel count, so it does not
     * encode this raster's density.
     */
    private fun inkOf(bmp: Bitmap, label: String): Int {
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val b = composeRule.onNodeWithTag("$BenchContextBarTestTag-$label").fetchSemanticsNode().boundsInWindow
        val margin = b.width * 0.3f
        var best = 0
        var bestScore = Int.MAX_VALUE
        for (y in (b.top - hostB.top).toInt() + 2 until (b.bottom - hostB.top).toInt() - 2) {
            for (x in (b.left + margin - hostB.left).toInt() until (b.right - margin - hostB.left).toInt()) {
                val p = bmp.getPixel(x, y)
                val score = luma(p)
                if (score < bestScore) { bestScore = score; best = p }
            }
        }
        return best
    }

    /**
     * A verb's **ground** — the pixel at its horizontal centre, 10 % down its box.
     *
     * 10 % of the 50dp button is 5dp, inside its 8dp top padding, so nothing but the fill (or the card's
     * `paper` showing through) is ever painted there. It was 15 % first, which is 7.5dp — half a dp of
     * headroom by that reasoning, and review flagged the failure mode as the nasty kind: in **light**
     * theme `onLeaf` and `paper` are the same cream `#FFF6E8`, so a probe that slipped onto the glyph
     * would fail the on-assertion while the screen looked right. Taken at the centre x for the same
     * reason [inkOf] narrows its scan: the pill's arc cannot reach the middle.
     */
    private fun groundOf(bmp: Bitmap, label: String): Int {
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val b = composeRule.onNodeWithTag("$BenchContextBarTestTag-$label").fetchSemanticsNode().boundsInWindow
        return bmp.getPixel(
            ((b.left + b.right) / 2f - hostB.left).toInt(),
            (b.top + b.height * 0.10f - hostB.top).toInt(),
        )
    }

    private fun luma(argb: Int): Int = (argb and 0xFF) + ((argb shr 8) and 0xFF) + ((argb shr 16) and 0xFF)

    /** Manhattan distance in RGB — enough to say "nearer this colour than that one". */
    private fun dist(a: Int, b: Int): Int =
        listOf(0, 8, 16).sumOf { s -> kotlin.math.abs(((a shr s) and 0xFF) - ((b shr s) and 0xFF)) }
}
