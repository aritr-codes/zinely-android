# Zinely — Voice & Microcopy

> **The companion design reference for how Zinely *talks*.** Brand personality, tone rules, and a
> ready-to-use microcopy library — a design reference under the canonical doc system in
> [CLAUDE.md](../../CLAUDE.md), not a parallel source of truth. The feel this voice serves lives in
> [DESIGN-LANGUAGE.md](DESIGN-LANGUAGE.md); the screens it appears on in
> [SCREEN-INVENTORY.md](SCREEN-INVENTORY.md); the journey it accompanies in
> [EXPERIENCE-MAP.md](EXPERIENCE-MAP.md). Status: design reference · 2026-06-28.

Copy is not decoration applied at the end — it is half of the interface a beginner actually
reads. This document is the reference wording. When implementing a screen, take the string from
here rather than inventing one; if a needed string is missing, add it here in the same change.

---

## 1. Brand personality

**If Zinely were a person, it would be the friend who is great at crafts and makes you feel
talented.** Not the teacher, not the tool, not the brand. The crafty friend who clears a space at
the table, slides the supplies over, and says *"ooh, do the one with your dog."*

| Zinely is… | Zinely is **not**… |
|---|---|
| warm, encouraging, a little playful | cute to the point of cloying or babyish |
| plain-spoken and concrete | technical, jargon-heavy, or clever |
| calm and unhurried | urgent, nagging, or salesy |
| on the maker's side ("you made this") | self-promoting ("powered by…", "pro features") |
| quietly confident about privacy | preachy or fear-mongering about it |

### Values the voice communicates

- **You can do this.** Every prompt assumes the user is capable and about to succeed.
- **There are no mistakes.** Undo and autosave are real; the voice says so, so experimentation
  feels safe.
- **It's yours and it's private.** Reassurance is stated once, warmly, as a gift — not a policy.
- **Making is the point.** The voice celebrates the *thing being made*, not the app's features.

## 2. Tone rules

1. **Speak to a person, in second person.** "Add a photo," "Your zine is ready" — never "User
   adds media," never "Document exported."
2. **First-person plural to invite, second-person to instruct.** *"Let's make something cute"*
   (invitation), then *"Add a photo"* (a clear next step). "Let's" is for moments of starting;
   don't overuse it.
3. **Short, concrete words.** "photo" not "media asset," "page" not "canvas," "words" not "text
   element," "make" not "create," "print at home" not "export to PDF."
4. **Verbs, not nouns.** "Write something" beats "Text." "Print & fold" beats "Export options."
5. **No system-error grammar — ever.** Never "No elements," "Invalid input," "Operation failed."
   Name what happened and the way forward, kindly.
6. **One idea per line.** Beginners scan. A label, a one-line value prop, a button. No paragraphs
   inside the UI.
7. **Emoji as seasoning, not structure.** A single warm emoji (✨ 🌸 ✂️ 📎) can punctuate a
   moment of delight. Never required to parse meaning (accessibility), never more than one per
   line, never on error or destructive copy.
8. **Earned, not constant, enthusiasm.** Exclamation marks are for genuine wins (export done,
   first photo placed). If everything shouts, nothing lands.

## 3. Microcopy library

The canonical strings. Format: **preferred** — *(avoid: the generic-Android version)*.

### Naming the core actions

| Concept | Zinely says | Avoid |
|---|---|---|
| add an image | **Add a photo** | Insert image / Import media |
| add text | **Add words** | Add text / Insert text box |
| typing text | **Write something** | Enter text |
| move/resize | **Drag to move · pinch to resize** | Transform / Manipulate |
| undo | **Undo** *(keep — universally understood)* | Revert last action |
| redo | **Redo** | Reapply |
| pages | **Pages** / "page 3 of 8" | Canvas list / Artboards |
| export | **Print & fold** | Export / Generate PDF |
| share | **Send to a friend** | Share via… |
| settings | **Settings** *(plain is fine here)* | Preferences / Configuration |

### Onboarding & first run

- Welcome headline: **"Make a little zine."**
- Welcome sub: **"Photos, words, and whatever you feel like. It folds into a tiny book."**
- Welcome primary button: **"Start making"**
- Privacy reassurance line (everywhere it appears): **"Works offline · stays on your phone"**

### Empty states

- First page, no content: **"Let's make something cute ✨"** / sub: **"Add a photo or a few
  words to start."** / **[ Add a photo ] [ Add words ]**
- A later empty page: **"A fresh page. What goes here?"** / **[ Add a photo ] [ Add words ]**
- No projects yet (future home): **"Nothing here yet — let's change that."** / **[ Start a zine ]**

### Hints (contextual, one-time, dismissible)

- After first photo: **"Drag to move it. Pinch to resize."**
- After first text: **"Tap to write. It saves as you go."**
- Discovering page 2: **"Eight pages in here — tap any to keep going."**
- Never modal, never blocking, always with an implicit/explicit "got it."

### Success & encouragement

- Autosave: **"Saved ✨"** *(quiet, transient)*
- First element placed: a small sticker-pop, no words needed — the moment speaks.
- Page feels done (heuristic, optional): **"Looking good."**
- Zine exported: **"Your zine is ready! 🎉"**

### Undo & recovery

- Undo confirmation (transient, optional): **"Undone. You can redo it."**
- Deleting an element (gentle, undoable): **"Removed — undo?"** *(action, not a modal)*
- Nothing destructive needs a scary dialog; if a true confirm is ever required:
  - Title: **"Delete this page?"** / body: **"The photos and words on it will go too."** /
    **[ Keep it ] [ Delete ]** — never "Are you sure? This cannot be undone."

### Errors (warm, specific, recoverable)

The rule: **name what happened in plain words, then the way out.** Never blame the user, never
expose a code.

| Situation | Zinely says |
|---|---|
| photo won't load | **"That photo didn't want to come in. Try another?"** |
| out of storage on save | **"Your phone's storage is full. Free up a little space and it'll save."** |
| export couldn't write file | **"Couldn't finish the file just now. Try Print & fold again?"** |
| unexpected hiccup | **"Something hiccuped. Your work is safe — try that again?"** |

Every error names the safety net ("your work is safe") when true, because for this audience the
real fear is *losing what they made*.

### Export & print

- Export sheet title: **"Print & fold"**
- Sub: **"We'll lay out all 8 pages on one sheet. Print it, fold it, done."**
- Format choice: **"Print at home (PDF)"** / **"Save as image (PNG)"** — not "Vector export" /
  "Raster 300 DPI."
- After export: **"Your zine is ready! 🎉"** / **[ Open it ] [ Send to a friend ]**
- Fold help entry: **"How do I fold it?"** — links to in-app fold steps, never assumed known.

## 4. Anti-patterns (do not ship)

- ❌ "No elements to display." → ✅ "Let's make something cute ✨"
- ❌ "Export successful." → ✅ "Your zine is ready! 🎉"
- ❌ "Are you sure you want to delete? This action cannot be undone." → ✅ "Delete this page?
  The photos and words on it will go too."
- ❌ "Invalid file format." → ✅ "That photo didn't want to come in. Try another?"
- ❌ "Tap the FAB to add an element." → ✅ "Add a photo" on a visible button.
- ❌ Walls of onboarding text, version numbers in the UI, "Pro" / upsell language (Zinely is
  free — [ADR-008](../DECISIONS.md#adr-008)).
