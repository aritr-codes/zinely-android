# Brief 02 — Document voices: Book and Plain

> ⛔ **Not ready for an implementation session.** One thing still blocks one (item 2):
> 1. ~~The owner's minimum print size for Book.~~ **Ruled 2026-10-08: 12 pt, to be confirmed on the printed
>    page** ([gate Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)). [Procedure A](#procedure-a-the-printed-book-page) is not printed yet.
>    Whether it must come before a session is the implementer's reading, recorded and put to the owner there.
> 2. **The owner's approval of the row drawn in the type bar** (gate 5). It was drawn on 2026-10-07 as the
>    proposed amendment [`v21-typebar.html` A29](../design/mockups/v21-typebar.html), with twelve questions for the owner (V-1 to V-12). It proposes the label
>    **Font** for the row this brief calls the Voice row.
> 3. ~~This preparation branch merged.~~ Merged 2026-10-07 (PR #99).
>
> Two more gates close inside the session itself. The [gate table](#gates) has all seven.
>
> **Two things were measured on 2026-10-07**, on an emulator with the released `0.9.0-beta.6`, not on the
> owner's phone. Both are in [Measured on 2026-10-07](#measured-on-2026-10-07):
> - **The card fits a 360 × 800 dp screen at font scale 2.0, with 79 dp to spare.** On 360 × 640 dp the
>   four-row card is already cut off. (Estimated, not measured: a fifth row would leave about 13 to 21 dp, less
>   than the reason line needs.)
> - **A character the face lacks is drawn from another font,** on every surface and in the PDF. No blank
>   glyph was seen.

Direction D2 of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md) (plan step 8). Rewritten on 2026-10-06 on
branch `design/document-voices-prep`, cut from `origin/main` @ `ae374ea` (release `0.9.0-beta.6`). Code
citations were read at that commit. The file keeps its old name so existing links hold; the three-voice
framing it was named for is superseded.

Labels: ✅ verified in the repository or measured · 🟦 recommendation · 🟨 assumption · ⚠️ not settled.

## What the owner ruled

All from the [decision gate](ZINELY-1X-DECISION-GATE.md), 2026-09-26. This brief carries them out and decides
nothing they left to the owner. The gate is the record; this table is a summary of it.

| Ruling | Source |
|---|---|
| Two document voices: **Book = Fraunces, Plain = Inter**. Hand (Averia) is deferred as a document voice | [Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8) |
| A document voice is not an interface typeface. Averia stays the interface voice for now. The owner read the constitution's existing wording this way; the constitution itself is not changed | Q2 |
| **The saved-file format does not change** ("no schema bump"). An older build keeps and restores the content. It may draw a voice it does not know in Inter. That is a layout change, not data loss, and its extent is documented and tested | Q2 |
| The Voice control is an amendment to the frozen `v21-typebar.html`. The owner approves the drawing | Q2, [Q4](ZINELY-1X-DECISION-GATE.md#q4-typebar--reframe-specs-o10) |
| The smallest size Book may be used at is the owner's: 12 pt, ruled 2026-10-08, to be confirmed on a printed page at 10, 12 and 14 pt | Q2 |
| The fold study does not gate this work | [Q5](ZINELY-1X-DECISION-GATE.md#q5-fold-study-before-creative-work-o14) |

Delegated by Q2 and specified below: what happens with scripts Fraunces lacks, the four document faces, the
editing-surface change, and the control's shape inside the type bar.

## Gates

| # | Gate | State on 2026-10-06 | Closes when | Who |
|---|---|---|---|---|
| 1 | PR #70 settled (the dead *Font* control removed) | ✅ Merged (`a6eb16b`, [ADR-115](../DECISIONS.md#adr-115)) | done | done |
| 2 | `v21-typebar.html` frozen | ✅ Frozen 2026-10-06; on `main` since 2026-10-07 (PR #99). Seven differences from the app were [returned to the owner](../OWNER-CHECKLIST.md#15-product--design-authorship); none blocks this brief, but gate 5 meets them | done | done |
| 3 | Four Fraunces document faces fetched, pinned and measured | ✅ Done; on `main` since 2026-10-07 (PR #99). See [The four faces](#the-four-faces). They sit in `docs/planning/voices/`, outside the app build | done | done |
| 4 | **Minimum print size for Book** | ◐ **12 pt**, ruled 2026-10-08, to be confirmed on the printed page ([gate Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)). [Procedure A](#procedure-a-the-printed-book-page) is written; the page is not printed | the page: before gate 7 (the gate's reading, put to the owner) | owner |
| 5 | **Type-bar amendment**: the Voice row drawn in `v21-typebar.html`, reviewed, owner-approved, frozen again | ◐ **Drawn and reviewed 2026-10-07 as proposed amendment A29; not approved, not frozen.** The owner's answers to V-1 to V-12 are owed ([OWNER-CHECKLIST §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship)) | before a session starts | the owner |
| 6 | **Older-build fallback test** run and recorded | ⛔ Not run. [Procedure B](#procedure-b-the-older-build-fallback-test) is written. It needs a build that can set Book, so it cannot run earlier | inside the session, before merge | implementer, with an emulator or a second phone |
| 7 | [ADR-126](../DECISIONS.md#adr-126) accepted | ⛔ Drafted as *Proposed* on this branch (2026-10-06); not accepted. It covers what [the section below](#what-the-adr-must-cover) lists, except the two figures that only the session can measure | accepted before merge | implementer drafts, owner accepts |

**Order.** The size is ruled, 12 pt, and the page that confirms it (4) can be printed at any time before
gate 7; it needs nothing from the drawing. Draw the
Voice row (5), always including how a choice looks when it cannot be used, because the script rule needs
that state whatever the size ruling. The owner approves the drawing. Then the session: Kotlin, the
older-build test (6), the ADR accepted (7), merge.

**The owner's part, in one place:** (1) ~~merge this preparation branch~~ done 2026-10-07, (2) ~~rule the minimum size~~ 12 pt, ruled 2026-10-08; print the page to confirm it
(Book is shown as unavailable below the minimum; say so if a warning is wanted instead), (3) approve the Voice row
drawing, including a TalkBack listen to the row's label, (4) accept the ADR.

What each size ruling costs. The sizes a maker can pick are 10, 12, 14, 16 pt and up. A 12 pt minimum means
Book cannot be used at 10 pt. A 14 pt minimum removes 10 and 12. For scale: Book's small letters at 12 pt
(1.99 mm, ✅ computed from the measured 10 pt figure) are about as tall as Plain's at 10 pt (1.93 mm).

## Measured on 2026-10-07

✅ Measured on an emulator (Android 16, API 36), with the released `0.9.0-beta.6` APK and the display set to
360 dp wide. **The owner's Samsung was not used**, so each figure is true for that emulator only. Sizes are
from `uiautomator dump`. "Room" is the height of the canvas area above the card's bottom edge. At font
scale 1.0 a first-run hint was on screen inside that area; measured to the hint, the card clears by 113 dp.
Samsung's largest font setting may not equal font scale 2.0.

**The type-bar card, four rows as shipped:**

| Screen (dp) | Font scale | Card (dp) | Room for it (dp) | Result |
|---|---|---|---|---|
| 360 × 800 | 1.0 | 288 × 259 | 442 | clears by 183 dp |
| 360 × 800 | 2.0 | 336 × 358 | 437 | clears by 79 dp |
| 360 × 640 | 2.0 | 336 × 277 shown | 277 | cut off by about 81 dp |

- At font scale 2.0 the card is at its 336 dp width limit and the Align row does not fit: "Right" is drawn on
  three lines. That adds about 76 dp to the card's height, and the card covers the selected text.
- On 360 × 640 dp at font scale 2.0 the Colour row cannot be reached and nothing scrolls.
- Both are faults in the app today, tracked in [issue #102](https://github.com/aritr-codes/zinely-android/issues/102). They are not caused by a fifth row.
- 🟨 **With a fifth row** (estimated from the frozen page's 46 px control and 12 px gap, not from a built row):
  at 360 × 800 dp it clears by about 125 dp at font scale 1.0, and by about 13 to 21 dp at 2.0 if neither
  word wraps. The reason line does not fit in that. So [`v21-typebar.html` A29](../design/mockups/v21-typebar.html) proposes that the card scroll inside itself
  when it does not fit (rule A29.11, owner question V-6).

**A character the face lacks.** One text holding `fi ﬁ α ж 🙂 中 ก`:

| Character | In the shipped Inter | Editor, Bench, Read | Exported PDF |
|---|---|---|---|
| `ﬁ` U+FB01 | no | a glyph from another font | the same, embedded |
| `α`, `ж` | yes | Inter | Inter |
| `🙂` | no, by design | colour emoji, from the emoji font the app bundles ([ADR-112](../DECISIONS.md#adr-112)) | colour emoji, embedded |
| `中` | no | a glyph from another font | the same, embedded |
| `ก` | no | a glyph from another font | `NotoSansThai`, embedded |

- No blank glyph on any surface, for `ﬁ`, `中` and `ก`. This matches the readiness audit §6. It does not
  match [ADR-070](../DECISIONS.md#adr-070), which records "no system-font fallback" and an unsupported glyph
  resolving to `.notdef`, for this emulator. The emoji is not part of that: it has its own bundled font.
- The typing-time notice said these characters "can’t print yet". They were in the PDF. Nothing warned later.
  Tracked in [issue #103](https://github.com/aritr-codes/zinely-android/issues/103).
- ⚠️ Not established: what the owner's phone draws (the glyphs come from the device's own fonts, so faces,
  widths and line breaks can differ between phones), and which fonts supplied `ﬁ` and `中` (the PDF names only `NotoSansThai`).

## Problem

Every zine is set in one typeface. ✅ `DocumentFontRegistry.Bundled` registers one family, Inter
(`render-android/.../DocumentFontRegistry.kt:102-113`), and any other `fontFamily` resolves to it (`:88-89`).
✅ No production code writes `fontFamily`; the style patch has no such field (`core/editor/.../Intent.kt:185-192`,
`EditorReducer.kt:496`). The dead *Font* control that used to sit on the Bench is gone (ADR-115), so today
nothing promises a choice and nothing offers one.

A maker who wants a zine to sound bookish has no way to say so.

## User story

*As a maker, I want the words in my zine to sound either bookish or plain, so the zine sounds like me without
a font list to search.*

## Experience

Select a text. Open the type bar (**Size** on the Bench). A **Voice** row offers two choices, each written in
its own face: **Book** and **Plain**. Tap one. The words on the page change. One undo step puts them back.

- The choice belongs to one text element.
- 🟦 New text starts as **Plain**, as it does today. No ruling says otherwise.
- Bold and italic keep working in both voices, with real bold and italic faces.
- ✅ Polish, Czech, Turkish and the other languages whose letters sit in the Latin-1 and Latin Extended-A
  blocks set in Book (126 of the 128 letters in Extended-A; the two missing are ŉ and ſ). Romanian ș and ț
  are present too. Latin letters beyond those blocks were not checked one by one.

⚠️ **The names are not settled.** "Voice" is the design's word. On a phone it can read as sound or read-aloud,
more so with a screen reader. The amendment (gate 5) settles the row label and the two words, and tries the
label against at least "Lettering" and "Type" with a first-time reading and a TalkBack listen.

⚠️ **The way in is inherited.** The row sits behind **Size** on the Bench. A maker looking for a different
typeface may not look under "Size". This brief does not change that; the amendment names it as a
discoverability risk and Pass 2 of the device check tests it.

## UI/UX proposal

Input to the `v21-typebar.html` amendment. Nothing here is frozen until that amendment is.

- 🟦 **A fifth row in the card, not a new surface.** The card has four rows: Size, Align, Style, Colour. Add a
  **Voice** row with two word buttons, Book and Plain. They look and behave like the Align row's buttons
  (`.tyalign`); one is always chosen, except for a text whose family this build does not know
  ([below](#data-model-implications)). No popover, no sheet, no list, no search.
- 🟦 **Each word is set in its own face**, using the **interface's** Fraunces and Inter at the Align row's size
  and weight. No document font file is loaded into the interface. The row label stays in the card's label
  style.
- **Selected state** is the Align row's: leaf ground under on-leaf ink. For a sighted maker that is a change
  of fill only. ⚠️ The amendment must show the chosen and unchosen buttons differ by at least 3:1 in
  greyscale, or add a second visible cue. Screen readers get the choice from the group's semantics.
- ⚠️ **Where the row sits** (first, or between Style and Colour) is the amendment's to draw.
- ⚠️ **Room.** A fifth row adds about 58 px at the default font size (a 46 px control and a 12 px gap), more at
  font scale 2.0 because the label and the words scale, and more again with the reason line below. The frozen
  file already records that the card was measured overflowing a 360 dp screen by about 28 dp at font scale
  2.0, and that "Right" wrapped mid-word at that scale. It was measured again on 2026-10-07
  ([above](#measured-on-2026-10-07)). The amendment checks in the HTML, at 360 px wide with enlarged text, that the card still clears the
  page and that "Book" and "Plain" do not wrap. The real check, at 360 dp and font scale 2.0 on a device, is
  the session's; a failure there is a stop condition.
- ⚠️ **Legibility of the two words.** At the Align row's size Fraunces has thin strokes. The amendment measures
  the contrast of "Book" resting, chosen and unavailable, in light and dark.
- **Teal and the other inks:** no change.
- 🟦 **The differences already returned to the owner.** The freeze returned seven type-bar differences
  ([OWNER-CHECKLIST §1.5](../OWNER-CHECKLIST.md#15-product--design-authorship); the frozen file's blocks B and E).
  The new row does not resolve them, but it has to be built one way, so:
  - **Group name.** The Voice row follows the frozen page: a named single-choice group. If the owner rules the
    other way for Align and Colour first, the Voice row follows that ruling.
  - **Focus ring.** The Voice row follows the frozen page and draws its 2 px ink focus ring. If the owner
    rules the other way for the sibling rows first, the Voice row follows that ruling.
  - The other four sites (the italic *I*'s face, two glyph shapes, a colour token) are left untouched.

## Interaction details

- ✅ existing pattern, reused: tapping a choice sends one intent and commits one undoable command. The card
  stays open, as it does for Align.
- 🟦 Tapping the current voice does nothing and adds no undo step.
- 🟦 The change is immediate, like Align, Style and Colour. (Only Size waits before committing.)
- **Undo wording** follows named undo ([ADR-123](../DECISIONS.md#adr-123), `v21-bench.html` A26). ✅ `Copy`
  already has "Text style put back" and "Text style changed" (`core/copy/.../Copy.kt:695-696`). ⚠️ Whether a
  voice change uses those lines is checked when the amendment is drawn. If it needs a new line, that is a
  separate owner-approved A26 amendment; the session stops for it.
- ⚠️ **A voice change can push words out of the box, on the current build too.** A text box has a fixed size
  and whatever falls below its bottom edge is cut off (`SharedTextLayout.kt:31`; the clip is
  `CanvasReplayer.kt:87`). That can be several lines, or half a line sliced through its letters. Two causes:
  width (Book is up to 7 % wider than Plain in bold capitals; Plain is 6 to 10 % wider than Book in italic),
  and line height (Book's lines are about 2 % taller, so a Plain box that just fits loses the foot of its
  last line in Book even with no re-wrap). So tapping a voice can make words disappear. 🟨 No cue for cut-off text was found in the editor (searched `core/copy`, `core/editor` and
  `feature/editor`). Resizing the box automatically is out of scope. The amendment must say what the maker
  sees when this happens, even if the answer is "nothing new, as with a size change today". It gets a golden
  and a Pass 2 item.

### A voice that cannot be used

*Note, 2026-10-07: the proposed amendment A29 departs from this section in two ways, and puts both to the
owner (V-8, and rule A29.7): it shows the reason line all the time, not only after a tap; and a tap on the
unavailable choice says the reason again. The text below is the input as written on 2026-10-06.*

🟦 One behaviour, for both reasons a voice can be unavailable (a script it lacks, or a size below the minimum):

- The choice stays in the row, drawn unavailable. It stays focusable and tappable. It is never hidden.
- Tapping it changes nothing and shows **one line of text inside the card**, under the row. The line says
  what is in the way, then the way out. Examples, to be settled in the amendment and written in `Copy`:
  - "Book has no Greek letters." (it names the script found in the text; the text is already Plain, so there
    is nothing more to do)
  - "Book needs 12 pt or larger. Make the text larger first."
  - On **Smaller**, for a Book text at the minimum: "Book stops at 12 pt. Switch to Plain to go smaller."
- For a screen reader the reason is part of the choice itself, so it is heard when the choice is reached and
  not repeated as a separate announcement on every pass.
- 🟦 **What the platform must report**, so the device check has a pass condition: the unavailable choice is
  **enabled and clickable** (it has to take the tap), not selected, and its spoken state says "unavailable"
  followed by the reason. In a `uiautomator dump` that is `enabled="true"`, `clickable="true"`,
  `checked="false"`, with the reason in its description. If the amendment chooses a truly disabled control
  instead, it must say where the reason is then read from.
- The current voice is never shown unavailable.
- The amendment draws the line and its height, and rewrites the frozen caption that says the two ends of the
  size stepper are "the bar's only disabled state".
- ⚠️ **A known trap.** A disabled control with a text child once told the platform it was enabled
  (`ReframeControls.ZoomButton`; the frozen file's note on `.tysize` records it). An unavailable Voice choice
  is that shape. It needs a platform-tree check (`uiautomator dump`) and a TalkBack listen, not only a Compose
  test.

### Scripts: what each voice can set

✅ Measured from the four faces and the bundled Inter faces (`python tools/build-document-fonts.py --measure`):

| | Book (Fraunces) | Plain (Inter) |
|---|---|---|
| Basic Latin | 95 of 95 | 95 of 95 |
| Latin-1 | 96 of 96 | 95 of 96 (no soft hyphen) |
| Latin Extended-A | 126 of 128 (no ŉ, no ſ) | 127 of 128 (no ŉ) |
| Greek | **0** | 105 |
| Cyrillic | **0** | 248 |
| Characters in all four faces | 637 | 2,815 |

So Book cannot set Greek or Cyrillic at all.

✅ How the app checks today:
- The typing-time check is by **script**, not by glyph. `analyzeTextCoverage(text)`
  (`core/model/.../TextCoverage.kt:50`) asks `SupportedScripts` whether each character belongs to a script
  Zinely promised: Latin, Greek, Cyrillic, emoji. Spaces, digits, punctuation and line breaks always pass.
  It has no family parameter, and its only production caller is the text editing session
  (`feature/editor/.../EditTextSession.kt:129`).
- A separate guard checks the font files. `FontCoverage.requiredCodePoints()`
  (`render-android/.../FontCoverage.kt:61-72`) demands the Latin, Greek and Russian alphabets of **every**
  registered family, in all four faces. Registering Fraunces fails it.
- ✅ **A character the face lacks is drawn from another font**, on one emulator
  ([measured 2026-10-07](#measured-on-2026-10-07)); [ADR-070](../DECISIONS.md#adr-070) records no fallback
  and a blank glyph instead. ⚠️ The owner's phone was not tested, so the same zine could still print differently from two
  phones; Procedure B check 11 and the device pass confirm it there. (A fallback Zinely controls needs an Android 10 API; Zinely supports
  Android 7.)

🟦 **The rule.**
1. **Each voice declares the scripts it sets.** Plain: Latin, Greek, Cyrillic, as today. Book: Latin only.
   Emoji, spaces, digits, punctuation and line breaks are treated for both voices exactly as today. The table
   lives in `:core:model`.
2. **`analyzeTextCoverage(text, family)`** applies the voice's script set. With no family, or with Plain, it
   behaves exactly as today. A family the build does not know is checked as Plain, because Inter draws it.
3. **Choosing a voice.** If the selected text holds a letter of a script the voice does not set, that voice is
   [unavailable](#a-voice-that-cannot-be-used), with its reason. Plain sets every script Book sets, so going
   back to Plain is always possible.
4. **Typing into a Book text.** If the maker types a Greek or Cyrillic letter, the typing-time notice appears.
   The voice is **not** switched for them. Silently changing the face is not allowed.
   - ⚠️ The notice's present line is wrong for this case: "Greek characters can’t print yet — but they’re
     saved with your zine, so nothing’s lost." (`Copy.Coverage.unsupported`). Greek does print, in Plain. The
     new line names the voice and the way out, for example "Book has no Greek letters. Switch this text to
     Plain to print them in a matching face."
   - ⚠️ Until the maker switches, the page and the PDF draw that letter in a font from the phone, which can
     differ between phones. The brief does not hide this.
   - ⚠️ Which frozen page owns that notice was not found (it is not drawn in `v21-bench.html`). The amendment
     session finds it. If the new line needs a change to a frozen page that no approved amendment covers, stop.
5. **The font-file guard becomes per family.** Inter keeps today's required set. Fraunces's set is the Latin
   part of it, with ŉ and ſ excluded and the reason recorded beside them, as ŉ already is for Inter. ✅ The four
   faces hold every other character of that Latin set.

**What this rule does not guarantee.** The check runs when a voice is chosen, when a text is opened for
editing, and while it is being typed. It does not run when a zine is opened, restored or exported. So a Book text can hold a character Book
cannot draw without the maker having been told, in these cases:
- The text was edited on an older build. There it is drawn in Inter, Greek raises no notice, and the Book
  voice is kept. Back on a current build it is Book with Greek letters drawn from a phone font.
- A backup holding such a text is restored.
- A character inside the Latin script that Fraunces happens to lack (ŉ, ſ, the thin space, rarer accented
  letters). Plain has the same kind of gap today; it is recorded in
  [ARCHITECTURE §5](../ARCHITECTURE.md) and the guard test. Book's gap is larger. The implementation lists it.
- The other way round, 17 characters exist in Fraunces and not in Inter (the single-character fi and fl
  ligatures U+FB00 to U+FB04, the soft hyphen, the letter Ǆ, a few marks and symbols). In Plain, and in a Book text on an older
  build, they come from a phone font. This is already true of Plain today.

⚠️ Closing those cases needs a check outside the editing session (on open, or at export). That is a new
decision for the owner, not part of this brief. Today's Plain has the same hole for unsupported scripts.

### Minimum print size: 12 pt

**Ruled by the owner on 2026-10-08: 12 pt, to be confirmed on the printed page** ([gate Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8), which
owns the ruling). The mechanism below is therefore built, with 12 pt as its minimum. The printed page can
still change that, so the minimum is one named value in the code.

✅ Fraunces is smaller on the body than Inter at the same point size. The small letters at 10 pt are 1.66 mm
tall in the Book regular face (1.65 mm italic, 1.69 mm bold), against 1.93 mm in Inter: 14 % shorter, with
thinner strokes. The size steps start at 10 pt (`TypeBar.kt:93`).

This is a readability decision for the people who read the printed zine, including older and low-vision
readers. The printed page ([Procedure A](#procedure-a-the-printed-book-page)) is how the owner checks it.

🟦 The mechanism, built because the ruled minimum is above 10 pt:
- Book is [unavailable](#a-voice-that-cannot-be-used) for a text smaller than the minimum.
- For a Book text, **Smaller** stops at the minimum, with its own reason line (see the examples above).
  (Smaller is an icon button with no place for text today; the amendment draws where the line goes.)
- A Book text that is already below the minimum (made on another build, or before a later ruling) is drawn
  as it is. Smaller is unavailable; Larger works.
- Nothing changes a size or a voice on the maker's behalf.

⚠️ This blocks rather than warns, and only the blocking version is specified. The owner may ask for a
warning instead; that version would need its own short specification before a session could start.
If the printed page brings the ruling down to 10 pt, none of this is needed and it is taken out.

## The four faces

✅ Fetched on 2026-10-06 (commit `bf84b67` on this branch). The files are in
[`docs/planning/voices/`](voices/) with the licence. **Nothing in the app build reads that folder.** The
implementation session moves them into `render-android/src/main/assets/fonts/` with `git mv` and checks the
hashes again.

| | |
|---|---|
| Source | [`undercasetype/Fraunces`](https://github.com/undercasetype/Fraunces) @ `7ccdec31c6028118dce3e47fe864e3744460371d` (2025-10-21), folder `fonts/ttf/`, Version 1.003 |
| Faces | `Fraunces9pt-Regular.ttf`, `-Bold.ttf`, `-Italic.ttf`, `-BoldItalic.ttf`. Upstream's own static files, **unmodified**. 448 KB in all |
| Pins | SHA-256 of each file, in [`tools/build-document-fonts.py`](../../tools/build-document-fonts.py). That script is the record; the hashes are not repeated here. It downloads, compares, and writes nothing on a mismatch |
| Licence | SIL OFL 1.1, no Reserved Font Name; `OFL-Fraunces.txt` is byte-identical to the copy the app already ships |

Facts that matter later:

1. ✅ **They are four plain static fonts**: weights 400 and 700, upright and italic, no variable-font tables,
   embedding allowed. That is what the registry requires (`DocumentFontRegistry.kt:3-9`).
2. ✅ **The Regular is the same file the interface already ships** as
   `core/ui/src/main/res/font/fraunces_regular.ttf` (same SHA-256). The other three are new. The licence is
   listed once. The interface's Medium and Semibold cuts are not document faces and stay where they are.
3. ✅ **The plain letter shapes are the default.** Fraunces also has a "wonky" set of leaning letters; in
   these files it is off unless asked for. Nothing depends on a font feature that older Android versions lack.
4. ✅ **Not the archived fork.** `googlefonts/fraunces` is an archived fork that stopped at Version 1.000. Its
   static italics lack `# $ £` and đ. An earlier draft of this brief built faces from that fork; that was
   dropped on review.

**Width against Inter**, same style and point size. ✅ computed by adding up letter widths. 🟨 Real text also
has kerning and ligatures, which this sum leaves out, so treat each figure as approximate; Procedure B
measures real lines.

| Sample | Regular | Bold | Italic | Bold italic |
|---|---|---|---|---|
| English prose | −1.5 % | +2.0 % | −7.2 % | −1.4 % |
| Pangram | −1.7 % | +1.5 % | −7.9 % | −2.2 % |
| Capitals | +3.6 % | +6.6 % | −2.7 % | +1.6 % |
| Digits | −4.3 % | 0.0 % | −5.6 % | −2.9 % |
| Polish | −4.6 % | −1.5 % | −9.5 % | −3.2 % |

A minus means Book is narrower than Plain. Line height is 1.233 of the point size in Fraunces and 1.210 in
Inter.

## Current architecture touchpoints

| Concern | Where (✅ read on `ae374ea`) |
|---|---|
| Model | `core/model/.../Document.kt:190` `fontFamily: String = "sans-serif"` |
| Registry | `render-android/.../DocumentFontRegistry.kt` (one row per family, four asset paths; `:102-113`). `isRegistered` (`:77`) has no production caller yet; it is the seam for "this build does not know this voice" |
| Resolver | `render-android/.../BundledFontResolver.kt:49-60`; never synthesises a face |
| Layout, every surface and the PDF | `render-android/.../SharedTextLayout.kt:44`, through `CanvasReplayer`; what does not fit the text box is not drawn (`SharedTextLayout.kt:31`). Both production replayers are given the bundled resolver (`ZineExporter.kt:162`; `PagePreview.kt:95`, through `previewFontResolver`) |
| Script check | `core/model/.../TextCoverage.kt:50`, `SupportedScripts.kt`; called from `feature/editor/.../EditTextSession.kt:129`; notice in `EditorCoverageNotice.kt` |
| Font-file guard | `render-android/.../FontCoverage.kt`, `CmapCoverage.kt`, `FontCoverageGuardTest` |
| **Editing surface** | `feature/editor/.../BenchEditingSurface.kt:213-224` |
| Type bar | `feature/editor/.../TypeBar.kt`; strings in `core/copy/.../Copy.kt` `object Type` |
| Reducer | `core/editor/.../EditorReducer.kt:496` (`styleText`), `Intent.kt:185-192` (the style patch's fields; no `fontFamily`) |
| Licences screen | Colophon, Licences and credits ([ADR-117](../DECISIONS.md#adr-117)); ✅ already lists Fraunces with its licence (`ColophonScreen.kt:82`) |

### The editing surface draws the wrong font today

✅ While a text is being typed, `BenchEditingSurface` sets `fontFamily = ZinelyTheme.v2Typography.work`
(`:216`), which is the **interface's** Inter, and passes `FontStyle.Italic` for an italic text (`:220`). The
interface's Inter has four weights and no italic file, so Compose slants the upright face. The page, the
preview and the PDF use the document's real italic. So for italic text the draft and the result already
differ, and with a second voice they would differ for every Book text.

🟦 Required change: the editing surface builds its font family from the **same four asset files** the
registry names for the element's family. A pure helper maps a registry row to its four faces; a thin seam
loads them. A test asserts every registered family has an editing family built from the same paths.
(`inter_regular.ttf` in the interface and `Inter-Regular.ttf` in the document set are byte-identical ✅, so
Plain's upright draft will not move. Its italic draft will, to the real italic.)

## Files/modules likely affected

- `render-android`: three new TTFs and the Regular in `assets/fonts/`; one registry row; the guard per
  family; hash pins for the four faces; goldens per voice.
- `core/model`: the per-voice script table; `analyzeTextCoverage(text, family)`; the voice ids.
- `core/editor`: `fontFamily` in the style patch; the reducer branch; a no-op when unchanged.
- `feature/editor`: the Voice row in `TypeBar`; the editing surface's fonts; the unavailable state and its
  reason line; the notice's new line.
- `core/copy`: the row label, the two names, the reason lines, the announcements.
- Docs: the ADR (gate 7); [ARCHITECTURE §5](../ARCHITECTURE.md) (registry and coverage); [PRD §7](../PRD.md#7-scope--mvp)
  (font choice was moved out of the MVP by ADR-055); `docs/design/VOICE.md` (the new reason lines); ROADMAP;
  CHANGELOG; release notes with the older-build note; the OWNER-CHECKLIST rows; this brief's status.

## Data model implications

**The saved format does not change**, by ruling. ✅ `fontFamily` is an existing saved string, defaults are
written to disk, and neither the validator nor the migrator reads it.

- 🟦 **Plain** is `"sans-serif"` (what every text holds today) and also `"Inter"`. Both show as Plain.
  Choosing Plain writes `"sans-serif"`, so a text returned to Plain is byte-identical to one never touched.
- 🟦 **Book** writes `"Fraunces"`, the registry's family name. Ids are family names, never display words.
- ✅ existing behaviour, kept: a family this build does not know is drawn in Inter and **kept** on save.
  🟦 The Voice row then shows neither choice selected, as the frozen Colour row already does for an ink it
  does not offer. ⚠️ What the row says in that case is for the amendment.

### What an older build does with a Book text

✅ `0.9.0-beta.6` and earlier have one family. They read `"Fraunces"`, keep it, and draw Inter in the same
bold or italic style. A maker meets this by restoring a backup onto a phone with an older build.

What changes on the page, from the widths above (🟨 predicted; [Procedure B](#procedure-b-the-older-build-fallback-test)
measures it):
- **Upright regular text gets 2 to 5 % wider** (English about 2 %, Polish and digits nearer 5 %). A line that
  just fitted may wrap.
- **Italic text gets 8 to 10 % wider** (digits about 6 %). Re-wrapping is likely in any italic paragraph.
- **Bold italic prose, and bold Polish, get 1 to 3 % wider.** Italic capitals get about 3 % wider; bold
  italic capitals get about 2 % narrower.
- **Upright bold English and upright capitals get 2 to 7 % narrower.** They will not gain a line, but line
  breaks can still move.
- Inter's lines sit about 2 % closer together, so the same number of lines takes slightly less room.
- ✅ A text box has a fixed size. **If the text no longer fits its box, whatever falls below the box's bottom
  edge is cut off, on screen and in the PDF.** That can be several lines, or half a line. Nothing is deleted:
  the words are still saved in the zine, and they show again on a current build.
- The maker on the older build is told nothing. Release notes for the new build do not reach that build.
- The voice survives. Opening the same zine again on a current build shows Book.

That is the "layout change, not data loss" the owner ruled acceptable. The release notes must say it in
those words **and** say that lines can be missing from the page and the PDF on an older build.

### What the ADR must cover

Gate 7's ADR ([ADR-126](../DECISIONS.md#adr-126)) records, at least:
- that it supersedes [ADR-055](../DECISIONS.md#adr-055)'s exclusion of font choice and its decision 7 ("styled
  text renders in Inter", "`fontFamily` is never written by the editor") together with the sentences in its
  architecture section and exclusion list that repeat it; and ADR-055's "font bundling" exclusion, for the
  four Book faces alone;
- that it extends [ADR-057](../DECISIONS.md#adr-057) (the font registry and the font-file guard);
- how it stands to [ADR-098](../DECISIONS.md#adr-098) §2.3, which says Fraunces becomes a document family
  only through the open owner decision OD-30. ⚠️ Gate Q2 ruled Book = Fraunces directly. OD-30 itself (nine
  selectors in the design files) is still open in the OWNER-CHECKLIST; the ADR says so and does not close it;
- the scope ruling (a document voice is not an interface typeface) and what follows from it;
- the older-build layout consequence, with Procedure B's measured worst case;
- that no saved-format change and no database migration is involved;
- the measured APK change.

## Testing strategy

- **Pure:** each id and style resolves to the right face; `"sans-serif"` and `"Inter"` both count as Plain; an
  unknown id draws Inter and keeps its value through save and load.
- **Reducer:** a voice change is one undo entry; redo restores it; the same voice again is a no-op.
- **Scripts:** Greek and Cyrillic in a Book text raise the notice; in Plain they do not, as today; an
  unsupported script still raises it in Plain as today; line breaks, digits and emoji raise nothing in either.
- **Font-file guard:** per family, against the real files, all four faces.
- **Hashes:** the four faces in `assets/fonts/` equal the pins in `tools/build-document-fonts.py`.
- **Editing surface:** every registered family has an editing family built from the same asset paths.
- **Resolver:** no production replayer is built with the default resolver (it would turn an unknown family
  into a system font).
- **Goldens:** each voice in regular, bold, italic and bold italic, on the Bench, in the page strip, in Read
  and in the exported PDF raster; a voice change that overflows its box; the type bar with the Voice row in
  light and dark and at font scale 2.0, including the unavailable state. Run `bash tools/grun.sh gold`. Read
  every `*_compare.png` before recording. **Expected re-records:** editing-surface goldens that hold italic
  text (they move to the real italic). No golden of an existing upright Plain text may change.
- **Android 7.** 🟨 Android 7 has an older text shaper than current phones. An instrumented test in
  `render-android/src/androidTest`, run on an API 24 emulator (say in the evidence that it is an emulator),
  lays out one fixed made-up paragraph in each Book face through `SharedTextLayout`. The paragraph holds
  h, m, n, s, `&`, an "fi" and an "fl". The test asserts the line count and the character offset at which
  each line ends equal the values recorded on the reference device. (Offsets, not pixels, so no tolerance.) A screenshot of the same paragraph from both is read by eye
  for the letter shapes. A Robolectric test does not count: the claim is about the phone's own text shaper.
- **APK size** measured before and after and recorded in the ADR (🟨 about +0.45 MB before compression: four files. The APK
  will hold the Regular twice, once as an interface resource and once as a document asset).
- **Device passes** (both, per CLAUDE.md) on the reference device named in
  [DEVICE-VERIFICATION §1](../DEVICE-VERIFICATION.md): the type bar with TalkBack, including the unavailable
  choice read from the platform tree; keyboard focus on the Voice row; a Book text typed, styled, exported;
  the PDF opened in two viewers.
- **Older-build fallback test** ([Procedure B](#procedure-b-the-older-build-fallback-test)).

## Procedure A: the printed Book page

**Purpose.** The owner confirms, or changes, the smallest size at which Book may be used (gate 4): 12 pt,
ruled on 2026-10-08 before anything was printed. This is a judgment made on paper, by eye. It is separate from the [print and fold study](STUDY-PRINT-AND-FOLD-PROTOCOL.md).

**What it can and cannot show.** It shows how the Book faces read on the owner's printer at 10, 12 and 14 pt,
beside Plain. It does not test Zinely's own print path, a phone's print service, a laser printer, or any
printer the owner did not use. ⚠️ The readiness audit asked for A4 and Letter and for inkjet and laser. This
procedure needs one inkjet sheet; the others are optional extra sheets. If the owner wants the full set
before ruling, print them the same way.

**Materials**
- The page: [`voices/print-test.html`](voices/print-test.html). It holds six blocks, A to F: Book and Plain at
  10, 12 and 14 pt, each with a paragraph, an italic line, a bold line and a line of look-alike characters, set
  60 mm wide (about one panel of an eight-page A4 zine).
- A home inkjet printer, the kind a maker has. Plain office paper, 80 gsm or 20 lb. A4 or Letter.
- A ruler marked in millimetres.
- A computer with Chrome or Edge. Open the file from inside the repository folder, because the page reads
  the font files beside it. If that is a nuisance, ask for a PDF of the page instead and
  print that. The ruler check in step 3 applies either way.

**Steps**
1. Open `print-test.html` in Chrome or Edge. If a box says "A FONT DID NOT LOAD", stop: the page is not valid.
2. Print it. In the print dialog open **More settings**. Set **Scale** to **Default** or **100** (some
   dialogs say "Actual size"), turn off **Fit to page** if it is offered, turn off **Headers and footers**,
   and use the printer's normal quality, in black.
3. Measure the ruler line at the top of the printed page. It must be **100 mm**, give or take 0.5 mm. If it
   is not, the page was scaled and its sizes are wrong. Fix the setting and print again. Do not judge a scaled
   page.
4. Write the date, the printer and the paper on the page.
5. Read each block held as a zine is read, about 35 to 40 cm from the eyes, in ordinary room light. Then
   read it once more in poor light. Note who read it and whether they wore glasses. 🟦 A second reader,
   older or with weaker eyesight, makes the ruling safer.
6. 🟦 Optional second sheet: print the same page in the printer's draft or economy mode. Many makers print
   that way.
7. 🟦 Optional third sheet: in the same dialog choose "Save as PDF", send the PDF to the phone, and print it
   from the phone the way a zine is printed. Measure its ruler line too.

**What the owner judges**, for Book at each size (blocks A, C and E):
- Can the paragraph be read comfortably, without effort?
- Do the thin strokes hold, or do they break up or fill in? Look at **e**, **a**, **g** and the italic line.
- Can the look-alike characters be told apart: `il1`, `rn` against `m`, `0O`, `8B`?
- Is the bold line clean, or has it blotted?
- Beside Plain at the same size (B, D, F): is Book clearly harder to read, about the same, or easier?

**The ruling.** One of:
- **10 pt.** Book is offered at every size. Nothing extra is built.
- **12 pt** or **14 pt.** Book is then unavailable below that size, as the
  [minimum print size](#minimum-print-size-12-pt) section describes.
- **Not acceptable at any of these sizes on a home printer.** That reopens the choice of face and goes back to
  the decision gate.

**Where it is recorded.** The ruling goes in the [decision gate, Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8),
with the date, the printer, the paper, and the measured length of the ruler line. This brief's gate 4 and the
ADR then link to it. Keep the printed sheets; a photo of each goes with the record if the owner wants one in
the repository.

## Procedure B: the older-build fallback test

**Purpose.** The owner ruled that an older build may draw a Book text in Inter, and that the extent of the
layout change is documented and tested ([Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8)). This test
measures that extent and checks that nothing is lost. It closes gate 6.

**When.** In the implementation session, as soon as a development build can set a text in Book, and before the
feature is merged. It needs no finished type-bar design: a debug-only way to set the voice is enough.

**Builds**

| Role | Build | Why |
|---|---|---|
| Sender | the development build | the only build that writes `"Fraunces"` |
| Receiver 1, required | `0.9.0-beta.6` (versionCode 11) | the newest build without voices; what testers hold |
| Receiver 2, required | `0.9.0-beta.5` (versionCode 10) | the build before it |
| Receiver 3, 🟦 optional | `0.9.0-beta.4-r3` (versionCode 9) | the oldest tagged build that reads the same backup |

✅ All three receivers read backup version 2 and document version 3, the same as today's `main`.

Use the published release APKs and check each file's SHA-256 against its release notes. A receiver cannot be
installed over the sender on one phone (it is a downgrade, and the signatures may differ), so use a second
phone or an emulator. 🟦 An emulator is acceptable for this test. Record which was used. Do not use the owner's
own phone if it holds zines that are not backed up.

**The fixture: one zine, made on the sender.** It is made in the app, not written by hand, and it is not
added to the repository's fixture folders.

| Page | Contents |
|---|---|
| 1 | Four Book text boxes at 12 pt holding the same paragraph (use the prose sample from `print-test.html`): regular, bold, italic, bold italic. Shrink each box until the text **just** fits, with no spare line |
| 2 | The same four at 10 pt |
| 3 | A Book title in capitals and a Book line of digits, each in a one-line box just wide enough |
| 4 | **Control:** the same paragraph in Plain, regular and italic, also just fitting |
| 5 | A Book paragraph in a box with one spare line of room |
| 6 | A Book line holding the typed letters "fi" and "fl", the single pasted characters ﬁ (U+FB01) and ﬂ (U+FB02), and a word with a soft hyphen pasted in |

Before sending, on the sender: write down the number of lines in every box, and save the PDF. That PDF is the
reference.

**Delivery.** Sender: Backups, Back up. Move the file to the receiver. Receiver: Backups, Restore. This is the
only route a maker has, so it is the route tested.

**What is measured**, on each receiver:

| # | Check | Expected |
|---|---|---|
| 1 | The restore finishes and reports success | yes |
| 2 | The zine opens; nothing crashes | yes |
| 3 | Control boxes (page 4): line count and appearance against the reference | identical |
| 4 | Each Book box: the face drawn | Inter, in the same bold or italic style |
| 5 | Each Book box: line count against the reference | may differ; **record both numbers** |
| 6 | Each Book box: is anything cut off at the bottom? How many lines, and is a line sliced through? | may be; **record it** |
| 7 | Open each Book text for editing and read it to the end, then leave without changing it | every word is there |
| 8 | Save the PDF on the receiver and compare it with the screen | the PDF shows what the screen shows, including any cut-off lines |
| 9 | Change the alignment of one Book text. Move one photo. Close and reopen the zine | the edits are kept |
| 10 | Back up on the receiver. **Delete the fixture zine on the sender first** (restore never replaces a zine and skips one it already has). Restore the receiver's backup on the sender. It must report one zine added. Open that zine and confirm it is the edited one: the alignment and the photo are as changed in check 9 | every Book box is Book again, including the one edited in check 9 |
| 11 | Page 6: how are the typed "fi" and "fl", the single characters ﬁ and ﬂ, and the soft hyphen drawn? | **record it**; a phone font may draw them |

Check 10 is the one that shows the voice survives an edit and a save on the older build. On a debug sender,
confirm it in the stored document as well: `fontFamily` reads `"Fraunces"` for those elements.

One more thing to note, not a check: if a maker resizes a Book box on the older build so the Inter text fits,
the text sits differently when it is Book again. Say so in the record if it is seen.

**Result**
- **Pass:** checks 1, 2, 3, 4, 7, 8, 9 and 10 are as expected on both required receivers. Checks 5, 6 and 11 are
  findings, not failures: they are the layout change the ruling allows.
- **Fail, and a stop condition:** a refused restore, a crash, a word missing in check 7, a control box that
  changed, or a Book box that comes back as Plain in check 10. Any of these means the ruling's "older builds
  keep the content" does not hold, and it goes back to the owner.

**Where it is recorded.** A dated note in `docs/reviews/` with the table above filled in for each receiver,
the build hashes, the phone or emulator, and screenshots of any cut-off box. The ADR's consequence section
and the release notes take their wording from the worst case found, in plain numbers ("an italic paragraph
that just fitted lost its last two lines").

## Accessibility considerations

- 🟦 The Voice row is a named single-choice group. Each choice announces its name and whether it is chosen.
  Targets are at least 48 dp.
- 🟦 The word drawn in its own face is still the choice's name: a screen reader hears "Book". ⚠️ A sighted maker
  learns what Book looks like from the letters; a TalkBack maker does not. The amendment decides, at the
  listen, whether each choice also carries a short description ("letters with serifs" / "letters without"),
  or records why not.
- An unavailable choice stays reachable, by touch, by TalkBack and by keyboard, and gives its reason in text
  and in speech ([above](#a-voice-that-cannot-be-used)).
- 🟦 Keyboard: Tab reaches the row; the two choices are reached the way the Align row's are. The 2 px focus
  ring is drawn.
- ✅ existing behaviour, kept: the interface follows the system font scale. Zine text does not, because it is
  print.
- The minimum print size is itself an accessibility decision, for readers of the paper zine.

## Design-system implications

One new row in an existing card, in existing clothes, plus one reason line. No new colour, no new interface
type role. The document faces are content, not design tokens.

## Acceptance criteria

1. Gates 2 to 5 closed before the session starts; gates 6 and 7 closed before merge.
2. Two voices selectable per text element. The Bench, the page strip, the editing surface, Read and the PDF
   draw the same face for the same text.
3. Bold and italic use real faces in both voices. The draft while typing matches the result.
4. A zine made before this change opens unchanged and saves unchanged.
5. An unknown family draws Inter and keeps its value.
6. A voice is never switched, and a size never changed, on the maker's behalf. Choosing a voice the text's
   script rules out is refused with a visible, spoken reason. Typing such a letter raises the notice. The
   cases the rule does not cover are listed in the ADR and the release notes.
7. The four faces are unmodified upstream files and equal their pinned hashes; the licence is shown; the APK
   change is recorded.
8. The release notes say "layout change, not data loss" and say that lines can be missing from the page and
   the PDF on an older build.
9. Pixel parity, both device passes, independent review.

## Stop conditions

Stop and report, do not improvise, if:
- the Voice row amendment is not approved;
- the Voice row needs a change to the frozen type bar beyond the approved amendment;
- a voice change needs an undo line A26 does not have;
- the typing-time notice needs a change to a frozen page that no approved amendment covers;
- any step appears to need a saved-format change, a backup-format change or a fixture change;
- the card with five rows and the reason line does not clear the page at 360 dp or at font scale 2.0;
- the per-family guard would change Inter's required set, or the script table and the font files disagree;
- a face's hash differs after the move;
- a golden of an existing upright Plain text changes;
- the Android 7 test shows different line breaks or letter shapes from the reference device;
- the older-build test shows anything other than re-wrap and cut-off lines (a crash, a refused restore, a
  lost voice id).

## Out of scope

A third voice, including Hand. A font list or picker. Size, tracking or line-height controls. Downloadable
fonts. Any change to interface typography. A per-zine default voice. Automatic resizing of a text box. A
check for undrawable characters on open or at export.

## Future extension

A per-zine default voice for new text. Hand, if a face with enough coverage is chosen and the owner rules on
running text. More scripts through faces that cover them ([ADR-070](../DECISIONS.md#adr-070)'s deferred work).

## Change log

| Date | Change |
|---|---|
| 2026-10-08 | Gate 4: the owner ruled Book's minimum print size, 12 pt, to be confirmed on the printed page ([gate Q2](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8); evidence in [RESEARCH R23](../RESEARCH.md#r23-the-smallest-size-for-book-on-a-mini-zine-page---verified---recommendation-with-a--assumption-edge)). The page is not printed. |
| 2026-10-07 | Two measurements recorded ([Measured on 2026-10-07](#measured-on-2026-10-07)): the card's fit at 360 dp and the largest font size, and what is drawn for a missing character. Gate 5 moved to "drawn as proposed amendment A29, not approved". The top note's third blocker struck: the preparation branch merged (PR #99). Two faults in the shipped app found by the measurements are tracked as issues #102 and #103. |
| 2026-10-06 | Rewritten as a two-voice brief. Folds in the owner's rulings (2026-09-26), the readiness audit's [§6](ZINELY-1X-READINESS-AUDIT.md#6-d2-audit--typefaces--voices) and the pre-rewrite record (last present in `ae374ea`). New: the script rule and what it does not guarantee, the unavailable state, the editing-surface change with its file and line, the saved ids, the older-build consequence with measured widths, the four faces, and the two procedures. The "Corrections pending" banner is replaced by the blocker list and the gate table. Corrected from the pre-rewrite record: the faces are upstream's own static files from `undercasetype/Fraunces`, not instanced, and the Regular is the file the interface already ships |
| 2026-09-26 | Pre-rewrite record added: rulings, and what the repository bundles |
