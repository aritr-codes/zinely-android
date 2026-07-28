# V2-BENCH-CRITIQUE.md — a critique of the *current* Bench (Editor)

> **Status:** Phase 2 deliverable of the **Bench** initiative. An evidence-based critique of the
> **current on-main** editor, measured against the [Phase 1 Bench research](V2-BENCH-RESEARCH.md) and the
> house rule that [every screen answers the user's current question](../../CLAUDE.md). It is *analysis*,
> not a decision — the decisions it motivates arrive in Phase 3+ (principles → IA → editing philosophy →
> journeys → interaction model → HTML) and are independently reviewed then. It follows the same stance as
> the product-wide [V2-CRITIQUE.md](V2-CRITIQUE.md): **elevation, not teardown.**
>
> **Method.** Current state established from a full code map of the live editor (the `:core:editor` MVI
> reducer/model/intents, the `:feature:editor` Compose surface + gestures + `EditTextSession`, the
> `:render-android` `CanvasReplayer` path, and persistence in `:core:data*`/`:data-android`), cross-checked
> against [ADR-003](../DECISIONS.md#adr-003), [-005](../DECISIONS.md#adr-005),
> [-028](../DECISIONS.md#adr-028), [-058](../DECISIONS.md#adr-058), [-069](../DECISIONS.md#adr-069),
> [-070](../DECISIONS.md#adr-070). Research is cited as [BR§n](V2-BENCH-RESEARCH.md). **One honest
> limitation:** this critique reads *structure*, not a fresh device screenshot of the current editor's
> visual chrome density — so judgments about interaction/architecture are firm; judgments about *visual*
> crowding are marked as owing a device Pass-2 look ([§6](#6-what-this-critique-cannot-see-yet)).

---

## 0. The headline: the Bench is the most mature surface we have

The Library critique could challenge almost everything because the Library was thin. **The Bench cannot** —
the editor already implements a surprising amount of what the research would otherwise "recommend," and
some of it is genuinely excellent engineering that V2 must *protect*, not disturb. Getting this critique
right means first being honest about what is already correct, so the redesign spends its energy on the few
things that actually hurt the maker — chief among them a single, high-value interaction wound.

The two sentences that matter most:

1. **`preview == export` is already structurally true and it is a competitive advantage most tools cannot
   claim** — the editor canvas is drawn by the *same* `CanvasReplayer` that renders the export PDF
   ([BR§1.4](V2-BENCH-RESEARCH.md); [ADR-028](../DECISIONS.md#adr-028)). The V2 job is not to build this —
   it is to make the maker *feel* a truth the machinery already guarantees.
2. **Text is edited where the maker isn't looking.** The one interaction at the heart of the most-used
   screen — typing words onto a page — happens in a bottom sheet spatially divorced from where the text
   actually lives on the page. This is the highest-leverage change in the whole initiative, and it is
   entangled with a *deliberately engineered* anti-desync defence, so it deserves a careful, fair reading
   (§2, and the fork in [§7](#7-the-one-decision-that-needs-you)).

---

## 1. What is already right — and must be protected (do not "redesign" these)

Naming these guards against the memory-flagged failure of scoring the incumbent against a softened brief.
Each is a research recommendation the editor *already satisfies*:

| Already built | Evidence | Research it satisfies | V2 stance |
|---|---|---|---|
| **One-engine `preview == export`** | `PagePreview` replays the `:core:render` tape through `CanvasReplayer`, the same replayer export uses; `PagePreviewParityTest` | [BR§1.4](V2-BENCH-RESEARCH.md), [BR§4](V2-BENCH-RESEARCH.md) | **Protect.** Never introduce a second render path for the Bench. |
| **Deep canvas accessibility** | `ElementSemanticsLayer` gives each element a focusable node (≥48 dp, rotated-AABB bounds, `selected` state, `Select` action, `traversalIndex` in paint order, custom actions = single-pointer twins of move/scale/rotate/reorder/delete); Reframe live-region announcements | [BR§5](V2-BENCH-RESEARCH.md) (the WCAG 2.5.1 "named twin per gesture" rule) | **Protect + extend** to any new element type. |
| **Command-based undo + debounced autosave + visible save** | `History` command mementos ([ADR-005](../DECISIONS.md#adr-005)); `AutosaveCoordinator` (1 s debounce, 5 s cap, latest-wins, flush on pause/exit); "Saved ✨" chip ([ADR-034]) + save-failure banner ([ADR-035]) | [BR§1.3](V2-BENCH-RESEARCH.md), [BR§6](V2-BENCH-RESEARCH.md) (felt safety) | **Protect.** The felt-safety base largely exists. |
| **Real direct manipulation** | `editorTransformGestures` (drag/pinch), `ResizeHandles` (8 handles, 48 dp), rotate/scale/nudge intents, image Reframe | [BR§3](V2-BENCH-RESEARCH.md) (the object is the control surface) | **Protect + refine** (see snapping, §3.4). |
| **Live coverage honesty** | `EditTextSession` warns, non-blocking, when a character's script can't print, and never strips it | [ADR-070](../DECISIONS.md#adr-070) | **Protect** — permanent behaviour. |
| **The anti-desync discipline** | `Intent.SetViewport` is *deferred* while a session/gesture is open so paper + render lag together, not one moving without the other | (a fix for a real prior bug) | **Respect** — the naive "just move the page" fix is the very thing they already ruled out. |

The last row is the crux of a fair critique: **the current design is not naive about page drift — it chose
to freeze the page during editing precisely to avoid a worse bug** (paper moving while the render stayed
behind, which "reads as the app losing the page"). Any V2 proposal must beat that, not ignore it.

---

## 2. The core wound: text is edited where the maker isn't looking

**What happens today.** Double-tapping a text element opens `EditTextSession` — a `BasicTextField` mounted
in a bottom `Surface` pinned to `Alignment.BottomCenter`, `fillMaxWidth().imePadding()`. The maker types in
that bottom panel; the words land in a different place — the text's real position on the page — only on
commit (Done / focus loss / pause). The page itself deliberately does **not** move to follow the caret.

**Why it's a problem.** The maker's mental model is *"I am writing on my page."* The research is blunt: when
the composition and the typing are spatially divorced, the honest first-timer reading is *"where did my text
go / what will it even look like there?"* — the same order-of-questions failure as
[ADR-058](../DECISIONS.md#adr-058)'s Preview, where a correct screen shown at the wrong moment felt like
loss ([BR§1.2](V2-BENCH-RESEARCH.md), [BR§3](V2-BENCH-RESEARCH.md)). The same-font draft softens it but
cannot remove the disconnect: you cannot see your words in their real size, wrap, and neighbours until you
commit and look back up.

**Why it's not a cheap shot.** The bottom-sheet approach *is* the current answer to a genuine hazard — page
drift / paper-render desync. Freezing the page while editing guarantees the two never separate. So the
critique is not "this is wrong," it is: **the current design bought steadiness by sacrificing locality, and
the research shows we can have both.**

**The V2 opportunity — get both.** Edit the caret **in place** on the page (the text at its real position,
size, and wrap), and when the keyboard opens, translate the **entire page as one rigid unit** by the minimum
needed to clear the IME — driven off `WindowInsets.ime` so it moves *with* the animating keyboard — then
settle back to the **pixel-identical** resting position on commit ([BR§1.2](V2-BENCH-RESEARCH.md),
[BR§3](V2-BENCH-RESEARCH.md)). Because paper and render already lag together as one object (§1, last row),
translating that *same* single object preserves the anti-desync guarantee — the page moves as a rigid body,
never reflowing, never desyncing. The maker sees "the page leaned in so I could write, then settled back" —
a trust-*building* motion instead of a trust-breaking disappearance.

- **Severity: highest.** It is the primary interaction of the primary screen, and it sits on the exact
  trust wound V2 exists to heal.
- **Worth changing before V1?** Yes — this is the reason the Bench initiative is worth doing.
- **Would it actually improve UX?** Yes, if and only if the return-to-rest is provably pixel-identical and
  small text remains editable (the two ⚠️ risks below). If we cannot guarantee those, the current
  bottom-sheet is the safer ship — which is exactly why this is the one decision to settle up front
  ([§7](#7-the-one-decision-that-needs-you)).
- **The two risks to design against** ([BR§3](V2-BENCH-RESEARCH.md), T2/T4): (a) a text block smaller than a
  comfortable caret/handle may need a temporary "zoom to edit" — itself a motion that must be provably
  reversible or it re-creates the drift wound; (b) the rigid-pan return position must be verified
  pixel-identical on device (edge-to-edge insets + IME animation are where this breaks).

---

## 3. Real gaps that don't need a fork (elevation work)

### 3.1 The Bench opens on a blank page — the moment most likely to end the session
There is no "first mark" or teaching empty state on a fresh page; the maker meets a blank canvas. The
research is emphatic that a blank page is a *felt threat*, not a neutral start, and that the antidote is a
small editable first mark + one clear invitation, using **real demonstrative content, never lorem-ipsum**
([BR§6](V2-BENCH-RESEARCH.md)). **Severity: high** (first-run abandonment). **Fix direction:** open on one
low-stakes editable element + a single "Tap to add your title / a photo" nudge that clears instantly, so the
first interaction is *editing* (safe), not *originating* (scary). Ties directly to the Starter-Pack work.

### 3.2 No gentle print-correctness cues while editing
Keep-clear / fold awareness lives on the imposition/proof side, not on the per-page editing canvas. A
beginner can place text where the fold or trim will eat it and only discover it at print. Research: collapse
the pro triad into **one soft keep-clear inset** (behaviour over labels — a gentle nudge only when
*text/faces* cross it; backgrounds bleed freely) and show the **fold** where it matters (the whole-booklet
view) ([BR§4](V2-BENCH-RESEARCH.md)). **Severity: medium-high** (it silently damages the printed result —
the thing the maker actually keeps). **Fix direction:** a quiet inset cue on the canvas + snapping.

### 3.3 No sense of "where am I in the 8 pages?" inside the Bench
The editor is strictly one-page-at-a-time; the finished-zine view lives on the separate Read/Proof surface
([ADR-058](../DECISIONS.md#adr-058)) — which V2 must **respect**, not rebuild here. But orientation within
the fixed 8-page structure is a different need from "see the finished book." Research: a persistent 8-page
**ribbon** ("3 of 8") + differentiated cover/back, at zero learning cost ([BR§4](V2-BENCH-RESEARCH.md)).
**Severity: medium.** **Fix direction:** a calm page ribbon; the *reveal* of the finished book stays Read's
job (§4).

### 3.4 Free-form placement with no snapping/alignment guidance
Direct manipulation is real but there is no snapping or alignment aid, and single-select only in MVP. On a
tiny mini-zine panel, "alignment the user didn't have to think about" is where calm comes from
([BR§4](V2-BENCH-RESEARCH.md)). **Severity: medium.** **Fix direction:** an invisible snapping grid +
alignment nudges; multi-select is a later question, not MVP.

### 3.5 Decoration/assets are model-thin — the 3-layer asset system is genuinely new
The page object model today is **`TextElement` + `ImageElement` only** — no stickers, patterns, shapes, or
illustration elements. So the brief's three-layer asset architecture (Product Identity / Starter Pack /
optional online library) is **new capability**, not a UI reskin: it needs a new element type in
`:core:editor`, an asset picker in the Bench, and the offline-first data layer + privacy-gated online search
from [BR§7](V2-BENCH-RESEARCH.md). **Severity: n/a (scope observation, not a defect).** This is the largest
*net-new* body of work in the initiative and carries the [BR§7](V2-BENCH-RESEARCH.md) print-safe licensing
constraint (CC0/PD/MIT-first) that must reach a governed ADR with legal sign-off before it ships.

### 3.6 Undo is session-only; there is no manual Save
Undo/redo is in-memory and does not survive process death — only the last autosaved *document* survives, not
the *history*. There is no explicit Save (autosave-only). The "Saved ✨" chip gives felt safety
([BR§1.3](V2-BENCH-RESEARCH.md)), but "undo can't cross a kill" is a latent trust gap for a nervous beginner
who backgrounds the app mid-edit. **Severity: medium** (defensible for a mini-zine tool, but worth an
explicit V2 decision rather than an accident). **Fix direction:** decide deliberately whether history should
survive a kill; at minimum keep the visible-save reassurance and never show a state readable as loss.

---

## 4. Where the Bench should stop — boundaries to honour
- **The finished-book reveal belongs to Read, not the Bench** ([ADR-058](../DECISIONS.md#adr-058)). The
  emotional peak "I made this" is Read's job; the Bench's job is the calm *making*. The Bench must **hand
  off** to Read gracefully (end the session on pride, not on a technical screen — [BR§6](V2-BENCH-RESEARCH.md))
  without absorbing Read's role. Rebuilding a finished-zine view inside the editor would repeat the
  question-at-the-wrong-moment failure.
- **No per-edit render side-effects** ([ADR-069](../DECISIONS.md#adr-069)). Any V2 "warmth" (thumbnails,
  previews) must not revive a per-edit render pipeline the project deliberately deleted.
- **MVI stays** ([ADR-005](../DECISIONS.md#adr-005)); ephemeral gesture/live state stays out of the reducer;
  only baked `Commit*` intents cross into history. New interactions must fit this seam.

## 5. Severity roll-up (what the redesign should spend on, in order)

| # | Finding | Severity | Fork? |
|---|---|---|---|
| 2 | Text edited off-page (bottom sheet vs in-place) | **Highest** | **Yes → [§7](#7-the-one-decision-that-needs-you)** |
| 3.1 | Blank page, no teaching first-mark | High | No |
| 3.2 | No gentle keep-clear/fold cue while editing | Medium-high | No |
| 3.5 | Asset system is net-new (model + IA + privacy ADR) | Scope | No (own ADR) |
| 3.3 | No "3 of 8" orientation in the Bench | Medium | No |
| 3.4 | No snapping/alignment guidance | Medium | No |
| 3.6 | Session-only undo; no manual save | Medium | No |

## 6. What this critique cannot see yet
Judgments about *visual* chrome density — is the supply tray / context bar / selection chrome too busy for
"the page is the hero"? — need a fresh **device Pass-2 look** at the current editor, which this structural
map did not provide. The immersive / chrome-quiet question ([BR§2](V2-BENCH-RESEARCH.md): the phone is the
form factor where hiding chrome pays) is therefore raised but not yet answered against the real screen. This
is a noted input owed before the Phase 8 prototype freezes, and a genuine two-pass device verification item.

## 7. The one decision that needs you
Everything in §3 is elevation work I can carry through the design-thinking phases (principles → IA →
editing philosophy → journeys → interaction model) without a fork. **§2 is different**, because it changes
the primary interaction of the primary screen *and* touches a deliberately-engineered anti-desync defence:

> **Should V2 replace the bottom-sheet text editor with in-place, on-canvas editing (caret in the real
> text) + a rigid whole-page pan to clear the keyboard — accepting the two risks (provably pixel-identical
> return; small-text "zoom to edit") — or keep the current steady bottom-sheet and improve it in gentler
> ways?**

**My recommendation: pursue in-place editing + rigid page-pan**, because it is the direct cure for the named
trust wound and the research strongly supports it — *conditioned* on the HTML prototype proving the rigid-pan
model and a device Pass-2 proving the pixel-identical return and small-text case. If either proof fails, we
fall back to hardening the bottom sheet. I'll design the prototype to make this exact decision testable
rather than assumed. But because it reshapes the core interaction, I'm surfacing it now rather than baking it
silently into the downstream phases.

---

## 8. What feeds Phase 3 (principles)
The Bench's principles will crystallise from: protect `preview == export` and felt-safety (§1); "the page is
the hero" as *maximise the page, recede the chrome, focus by dimming* (§2, [BR§2](V2-BENCH-RESEARCH.md));
"never open empty" and "templates as scaffolding you overwrite" (§3.1, [BR§6](V2-BENCH-RESEARCH.md)); "make
print-correctness felt, not taught" (§3.2, [BR§4](V2-BENCH-RESEARCH.md)); "every gesture has a named twin"
(§1, [BR§5](V2-BENCH-RESEARCH.md)); and "the Bench makes; Read reveals" (§4).

---

*Phase 2 of the Bench initiative. Analysis grounded against the current repo — no design decided, no code
changed. Next: Phase 3 principles (pending the §7 fork), then IA → editing philosophy → journeys →
interaction model → HTML prototype.*
