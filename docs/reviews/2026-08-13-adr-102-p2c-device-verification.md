# ADR-102 P2c / OD-49 — device verification (the warning answers for the selection)

**Device** Samsung **SM-A176B** `RZCYA1VBQ2H` · 1080×2340 · override density 420 (2.625 px/dp) · **Android 16**
**Build** `:app:installDebug`, installed 2026-08-13 18:44, measurements 18:45–18:51.
**TalkBack** Samsung TalkBack (`com.samsung.android.accessibility.talkback`), enabled for the a11y probe and
disabled again afterwards.
**Scope** [ADR-102 §12.12 / OD-49](../DECISIONS.md#adr-102-p2c) — the fix for the two failures in
[P2b's pass](2026-08-13-adr-102-p2b-device-verification.md).

---

## Pass 1 — Developer Verification

### The state matrix, measured

Every row is a real screenshot of the real editor, scanned over the sheet's rect for jam-hued pixels
(`r>g>b` and `g` below the r–b midpoint, which separates `jam` from `butter`):

| state | boundary | measured |
|---|---|---|
| selected, **inside** the reach | none | 0 px |
| **nudged** across with the arrow buttons — no gesture ever in flight | **drawn** | 6803 px · α **.886** · **3.51:1** |
| in-place **text session**, box overhanging | none | **0 px** where the boundary would be |
| that session **ended**, still selected, still outside | **drawn** | 6803 px · α **.886** · **3.51:1** |
| **deselected**, still outside | none | 0 px |

**Row 2 is the Required Fix, closed.** P2b measured **zero** cue pixels for exactly this gesture-free path;
it now draws the identical mark, to the byte, that a drag produces — `(213,93,59)` on paper `(253,243,230)`.
The nudge buttons are the accessible way to move an element, and they were the one way the crossing was
never reported.

**Row 4 is U4, closed.** The mark no longer disappears while the thing it warns about is still true.

**Rows 3 and 5 are the rule's two deliberate silences**, and both hold: a session answering its own question
raises no print alarm, and an unheld page never warns — which is the half of
[D-032](../design/V2-SPEC-DEFECTS.md#d-032-ruling) OD-49 keeps.

### ⚠ A probe that lied, and the frame that corrected it

The first scan of the text-session screenshot reported **2655 jam pixels** and an implied alpha of `.985` —
apparently a boundary drawn at full opacity in the one state that is supposed to be silent. It was not. The
**caret is `jam` too** (`v21-bench.html:215`), and Samsung's own cursor handle is a red-orange teardrop; both
sit inside the text field. Re-probing the field alone found 612 of them there, and re-probing the band where
the boundary's top edge would be found **0**.

Recorded because the near-miss is the lesson: a colour probe over a whole surface answers *"is this hue
present"*, not *"is this mark drawn"*, and the editor now has two `jam` marks on one page. The same scan also
mislabelled a still-open text session as "released" for one frame — TalkBack was on, so the `Done` tap needed
a double-tap and never landed. **Both errors were caught by looking at the screenshot rather than the
number.**

### ⚠ The spoken state could NOT be verified at the platform level

This is the half of OD-49 that the finding was actually about, and the tools available here cannot see it:

| route | result |
|---|---|
| `uiautomator dump` | **no `state-description` attribute exists in the XML at all** — the trap already recorded at `EditorContextBarA11yTest.kt:33` |
| `dumpsys accessibility` | dumps services and focused-window ids; **no node attributes** |
| logcat with TalkBack running | Samsung TTS logs `[Synthesize]` and audio routing, **never the utterance text** |

TalkBack was enabled, focused the element and **spoke** — the TTS and vibration path is in the log — but the
string it spoke cannot be captured as text by any of these. So the assertion that the state reaches the
platform rests on `ElementSemanticsLayerTest` (a **merged Compose semantics** read, mutation-checked), which
is precisely the kind of evidence [CLAUDE.md](../../CLAUDE.md#pass-1--developer-verification) says is not the
platform tree.

**This is verification debt, and it is not new.** Three shipped surfaces already carry `stateDescription`
under Robolectric-only proof (`BenchPageGrid`, `BenchPageNav`, `EditorContextBar`). The one thing that would
close it for all four is a single instrumented probe — `AccessibilityNodeInfo.getStateDescription()` is API
30+ and reachable from an `androidTest` via `UiAutomation.rootInActiveWindow`, and `app/src/androidTest`
already exists. **Booked, not done.** Stated here rather than smoothed over, because "TalkBack said
something" is not evidence that it said *this*.

### ✅ Closed later the same day

The probe was written and run on the same device:
[`KeepClearPlatformStateTest`](../../app/src/androidTest/java/com/aritr/zinely/KeepClearPlatformStateTest.kt),
two tests, both green, mutation-checked on hardware. The platform node reads verbatim:

```
cls=android.widget.Button  desc=Text: hi
    state=Selected, Too close to the edge — may be cut off when printed
```

So the spoken half no longer rests on merged semantics, and the run also covers the post-acceptance review fix
that made the state carry the selection word. The other three surfaces named above remain on the old evidence;
this test is the pattern that closes them.

⚠ **The probe's first run reported `states=[]` and was wrong.** Touching `uiAutomation` attaches the
accessibility client, and Compose exports semantics only after it observes one — so reading the tree in the
same breath returns the `ComposeView` with none of its content, which reads exactly like the defect. A full
tree dump separated them. That is the second false-negative on this ruling after the caret/`jam` scan, and
the rule they share is: **when a probe reports nothing, suspect the probe before the app.**

---

## Pass 2 — First-Time User Verification

Reset assumptions. I typed a word, then used the little arrows at the bottom to slide it left.

**It now behaves the way I expected the first time.** The box crossed some invisible line, a red dotted
rectangle appeared, and — this is the part that was wrong before — **it stayed**. It is still there while I
look at it, which matches the fact that my word is still hanging off the edge. When I moved the box back
inside, the rectangle went away on its own. Nothing to dismiss, nothing to understand.

**Typing is quiet, and I only noticed afterwards that this was a choice.** While the keyboard was up there
was no alarm; when I finished, the rectangle was waiting with the element I had just finished. That reads as
the app being polite rather than the app being late.

**⚠ Carried, and it is the same open question as before, one step further out.** Tapping the page to put the
element down clears the mark — and my word is *still* off the edge. Nothing on a page with nothing selected
says so. That is not a defect of OD-49, which is why it is carried and not filed: it is the boundary
[§12.11](../DECISIONS.md#adr-102-p2b) already named as unhandled — **the product has no committed-state
signal**, only a while-you-hold-it one. The honest version of that finding, after this package, is narrower
and sharper: *a maker can now always find out while holding the element, and still never find out while not.*
Preview or Print is where that belongs, and it is not this package's to invent.

---

## Acceptance

| | verdict |
|---|---|
| Pass 1 | **PASS** — the spoken half was booked as unverified at platform level, then closed the same day by `KeepClearPlatformStateTest` (see above) |
| Pass 2 | **PASS**, one finding carried (no committed-state signal, unchanged from §12.11) |

**The two passes agree this time**, which is worth stating because the previous two did not. Pass 1 says the
predicate does what it was ruled to do in all five states. Pass 2 says the mark now tells the truth about the
page for as long as the maker is holding the thing it is about. The disagreement that produced OD-48 and
OD-49 — *"correct but misleading"* — is resolved for the held case and openly unresolved for the unheld one.

**Device left as found**: test element deleted, light theme, TalkBack disabled, Do Not Disturb off.
