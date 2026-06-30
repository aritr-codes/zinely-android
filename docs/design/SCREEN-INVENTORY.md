# Zinely — Screen Inventory

> **The companion design reference for *which screens exist* and what each one is for.** Every
> planned surface, its purpose, primary/secondary actions, and emotional goal — so screens are
> designed as one product, not in isolation. A design reference under the canonical doc system in
> [CLAUDE.md](../../CLAUDE.md), not a parallel source of truth: product scope is owned by
> [PRD.md](../PRD.md) and phasing/sequencing by [ROADMAP.md](../ROADMAP.md) — where this disagrees
> with them, they win. Feel: [DESIGN-LANGUAGE.md](DESIGN-LANGUAGE.md); words:
> [VOICE.md](VOICE.md); the arc that strings them together:
> [EXPERIENCE-MAP.md](EXPERIENCE-MAP.md). Status: design reference · 2026-06-28.

**Legend — build status:** ✅ shipped · 🔂 current milestone (`SUX`) · 🔭 designed, deferred.
Each surface maps to a milestone so the [roadmap](../ROADMAP.md) can sequence by journey, not by
feature.

---

## Navigation map

```mermaid
flowchart TD
    W["Welcome 🔭"] --> H["Home / My zines 🔭"]
    H -->|start a zine| ED["Editor ✅"]
    H -->|open| ED
    W -.first run, no projects.-> ED
    ED --> ES["Empty-state invitation 🔂"]
    ED --> NAV["Page navigator ✅"]
    ED --> TRAY["Supply tray 🔭"]
    TRAY --> PICK["Photo picker (system) ✅"]
    TRAY --> STK["Sticker picker 🔭"]
    ED --> TPL["Template picker 🔭"]
    ED --> PRE["Preview 🔭"]
    PRE --> EXP["Export · Print & fold 🔭"]
    EXP --> DONE["Completion · fold steps 🔭"]
    ED --> SET["Settings 🔭"]
```

> **Note (MVP truth):** today the app boots **straight into the Editor** on a fixed `"default"`
> project — Welcome and Home are designed but not built (no multi-project store yet; see
> [ROADMAP status](../ROADMAP.md)). The inventory describes the *target* product so each screen is
> designed coherently; the roadmap controls what gets built when.

## The screens

### Welcome
- **Status:** 🔭 deferred (first-run only).
- **Purpose:** in one glance, communicate "I make a tiny printable book," and start.
- **Primary action:** **Start making** → Editor (or Home, once it exists).
- **Secondary:** none (deliberately — one obvious next step).
- **Emotional goal:** invited, reassured ("works offline · stays on your phone").
- **Notes:** no carousel, no account, no permissions yet. A folding-zine illustration carries the
  promise. Shown once (a **local first-run flag**, not Room) and then routes straight to the editor
  on the `"default"` project — so it is **not blocked by the project layer** and can ship early;
  returning users land on Home/Editor once Home exists.

### Home / My zines
- **Status:** 🔭 deferred (needs the Room project layer — [ROADMAP](../ROADMAP.md)).
- **Purpose:** see and reopen the zines I've made; start a new one.
- **Primary action:** **Start a zine** (prominent, always reachable).
- **Secondary:** open / duplicate / delete a project (gentle, undoable); each as a paper-card
  thumbnail.
- **Emotional goal:** a cozy shelf of my own little books — pride and continuity.
- **Empty state:** "Nothing here yet — let's change that." / **[ Start a zine ]**.

### Editor
- **Status:** ✅ shipped (mounted on the `"default"` project, [ADR-030](../DECISIONS.md#adr-030)).
- **Purpose:** the craft table — place and arrange photos and words on the current page.
- **Primary action:** make something on this page (add photo / add words).
- **Secondary:** switch pages (navigator), transform/reorder/delete (on selection), undo/redo,
  preview, export entry.
- **Emotional goal:** a calm, forgiving worktable; "I can't break this."
- **Notes:** the spine of the app; [editor-visual-direction.md](editor-visual-direction.md) owns
  its surface specifics.

### Empty-state invitation
- **Status:** 🔂 current milestone (first implemented `SUX` slice; component shipped, being
  refined).
- **Purpose:** turn a blank page from a void into an invitation — *the* onboarding moment.
- **Primary action:** owned by the **supply tray** below, not the overlay — the empty state is an
  **invitation only** ([ADR-033](../DECISIONS.md#adr-033)): warm copy + ornament + privacy line, **no
  buttons**. The tray's **Add a photo** (coral primary) / **Add words** are the single, thumb-zone home
  for the add actions, so they're never duplicated on a blank page (DESIGN-RULES R3/R7).
- **Emotional goal:** "Let's make something cute ✨" — safe to try, no instructions needed; the subcopy
  points to the supplies below ("Grab a photo or a few words from the supplies below.").
- **Notes:** appears whenever the current page has no elements and no text session is open;
  disappears the instant the page gets content. Non-interactive (touches fall through to the canvas).
  Carries a subtle, decorative **downward cue** toward the supply tray (ADR-033 follow-up) — static
  (reduced-motion-safe) and cleared from the a11y tree, so the spoken orientation comes from the tray's
  "Supplies" heading instead.

### Page navigator
- **Status:** ✅ shipped (`EditorPageStrip`, `Intent.GoToPage`).
- **Purpose:** make all 8 pages reachable and the booklet structure obvious.
- **Primary action:** tap a page card → go to that page.
- **Secondary (future):** reorder / duplicate pages (V1).
- **Emotional goal:** "I'm making a real little book," momentum.
- **Notes:** the current page is lifted + taped; pages with content show an ink dot. Real
  mini-render thumbnails are a deferred polish.

### Supply tray
- **Status:** 🔭 next `SUX` slice (replaces the lone FAB).
- **Purpose:** make every primary action *visible* as a row of craft supplies.
- **Primary actions:** add photo, add words.
- **Secondary:** undo, redo (enabled by `canUndo`/`canRedo`); later stickers, templates.
- **Emotional goal:** supplies within reach, nothing hidden — discoverability over gestures.
- **Notes:** thumb-zone placement; ≥48dp; on-brand "supply" styling, not a toolbar. Titled with a quiet
  **"Supplies" `heading()`** so a screen reader gets section orientation before the four actions
  (ADR-033 follow-up); the heading adds a label only, no behavior, on every page.

### Photo picker
- **Status:** ✅ shipped (system picker via `Intent.RequestAddImage`,
  [ADR-031](../DECISIONS.md#adr-031)).
- **Purpose:** choose a photo from the device — offline, no upload.
- **Primary action:** pick a photo → lands centered + selected.
- **Emotional goal:** "that was easy" (the first peak).
- **Notes:** uses the OS picker (privacy-preserving, no broad storage permission).

### Sticker picker
- **Status:** 🔭 deferred (V1 expression).
- **Purpose:** a tray of cute supplies (stickers, tape, stamps) to decorate.
- **Primary action:** tap a sticker → it lands on the page, selected.
- **Secondary:** categories/scroll; "more" without overwhelming.
- **Emotional goal:** playful flow; the crafting feeling.
- **Notes:** small curated set first; bundled, offline. No store/IAP (free app, ADR-008).

### Template picker
- **Status:** 🔭 deferred (V1).
- **Purpose:** offer a gentle starting layout for people who freeze at a blank page.
- **Primary action:** pick a template → applies a layout to the page/zine.
- **Secondary:** "start blank" always present and equal — templates are help, not a requirement.
- **Emotional goal:** a head start, never a constraint; "I can still change everything."
- **Notes:** templates are scaffolding, not lock-in; everything stays editable.

### Preview
- **Status:** 🔭 deferred (pre-export).
- **Purpose:** see the whole zine as a folded booklet before printing.
- **Primary action:** **Print & fold** → Export.
- **Secondary:** page through the booklet; back to editing.
- **Emotional goal:** pride — "I made this."
- **Notes:** shows the *reader's* booklet, not the imposition sheet (that's an export detail).

### Export · Print & fold
- **Status:** 🔭 deferred (S5 — the export flow).
- **Purpose:** produce a correct, home-printable artifact.
- **Primary action:** **Print at home (PDF)**.
- **Secondary:** **Save as image (PNG)**; "Actual size" print guidance; "How do I fold it?".
- **Emotional goal:** accomplished; confidence it will print right.
- **Notes:** the imposition engine ([ADR-007](../DECISIONS.md#adr-007)) and render backends exist;
  this is the user-facing wrapper. "Actual size" guidance is non-negotiable (the classic
  prints-wrong-and-won't-fold failure).

### Completion · fold steps
- **Status:** 🔭 deferred (S5).
- **Purpose:** celebrate, and get the printed sheet folded into a book.
- **Primary action:** **Send to a friend** (system share, offline).
- **Secondary:** **Open it**; step-by-step fold instructions; "make another."
- **Emotional goal:** the payoff peak + connection; the reason to come back.
- **Notes:** "Your zine is ready! 🎉"; fold steps illustrated, never assumed known.

### Settings
- **Status:** 🔭 deferred (minimal).
- **Purpose:** the few real toggles, kept tiny and unintimidating.
- **Primary action:** none — it's a quiet utility surface.
- **Secondary:** paper size (Letter/A4), theme (light/dark, V1), about/privacy, fold help.
- **Emotional goal:** calm and sparse; never a "configuration wall."
- **Notes:** no account, no sync, no analytics toggles (there's nothing to opt out of —
  [privacy invariant](../PRD.md#5-product-principles-non-negotiable)).

## Coverage check (screen ↔ milestone)

| Screen | Milestone | Status |
|---|---|---|
| Editor, Page navigator, Photo picker | S4 / `0.4.0` | ✅ |
| Empty-state, Supply tray | `SUX` / `0.5.0` | 🔂 / 🔭 |
| Preview, Export, Completion | S5 / `0.6.0`+ | 🔭 |
| Welcome | needs only a **first-run flag** (local prefs) — *not* Room-gated | 🔭 |
| Home / My zines | needs the **Room project layer** + shelf **thumbnails** | 🔭 |
| Settings | needs only **local prefs** (DataStore) — *not* Room-gated | 🔭 |
| Sticker picker, Template picker | V1 | 🔭 |

The [HTML prototypes](mockups/) realize each of these as the working visual reference before any
Compose is written.
