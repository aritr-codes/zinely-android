# V2-LIBRARY-CRITIQUE.md — a pre-release critique of the Library prototype

> **Status:** First-principles UX critique of the Phase-9 Library prototype
> ([mockups/v2-library.html](mockups/v2-library.html)), reviewed as if gating a mature commercial creative
> product before release. **No redesign here** — this identifies everything to improve first, per the
> owner's instruction. It is adversarial about its *own* prototype on purpose. Feeds a revised Library
> before the bench and the rest are drawn. Analysis, not a decision.
>
> **The lens the owner set:** *less application, more creative workspace · less interface, more collection
> · less software, more handmade object.* Every finding below is scored against that, and against one
> question per element: **can it be removed, simplified, made content, made quieter — does it help the user
> create or reduce friction?**

---

## 0. The one-sentence verdict

**The prototype is warm and on-identity, but it is still wearing three pieces of app-chrome the collection
doesn't need (a wordmark, a count, a sort control), it says each zine's name twice, and it *gestures* at
"handmade object" with a CSS tilt and full-colour emoji instead of *committing* to it — so it currently
reads as "a nice app showing my zines" when the brief is "my zines, in an app that got out of the way."**
The fixes are almost all *subtraction*.

---

## 1. The element audit — is each visible thing earning its place?

The core of the review. Verdict codes: **REMOVE · DEFER · QUIETEN · → CONTENT · FIX · KEEP.**

| Element | Verdict | Why — "does it help create / reduce friction?" |
|---|---|---|
| **"Zinely." wordmark** (top-left) | **REMOVE** | Pure chrome. A user who opened the app knows the app. It occupies the most valuable slot on the screen and says nothing about *their* work. Identity belongs at the threshold (icon, first-run), not on every visit. **(Owner's instinct is correct — §2 argues it in full.)** |
| **"Recent ▾" sort** (top-right) | **DEFER** | At six items sorting is a solution to a problem the user doesn't have yet ([R§2.6](V2-RESEARCH.md): small collections are served by recency, not controls). It's chrome that helps *manage*, not *create*. Reintroduce only when the shelf is big enough that finding is a real task — and even then, quietly. |
| **"6 little books" count** | **REMOVE** | The covers already show how many. "6" is a label for a list with no ambiguity (the exact beta finding about "Your zines 1"). It is not information the user needs; it is the interface talking about itself. |
| **Title printed on the cover *and* repeated in the meta row** | **FIX (de-duplicate)** | The single biggest hierarchy defect. Every zine's name appears **twice**, ~20px apart. The whole point of the Maker's Cover is that the cover *is* the identity — so the cover carries the title, and the row below carries only what the cover can't say. Saying it twice halves the value of saying it at all. |
| **"8 pages"** in the meta | **REMOVE** | Every Zinely zine is *always* 8 pages (single-sheet-8). A constant is not information — it's noise that dilutes the two facts that vary and matter (which paper, how recent). |
| **Format "A4 / Letter"** | **QUIETEN / KEEP** | This one *is* the physical object's identity (which paper it becomes). Keep it, but quiet — it's a property of the object, not a headline. Consider it belongs to the object's "back," not shouted on the shelf. |
| **"2 days ago" recency** | **KEEP (quiet)** | Genuinely aids recognition and return; it's the one meta fact that earns its place at a glance. |
| **The alternating ±0.8° tilt** | **FIX (commit or drop)** | It *gestures* at "handmade objects on a shelf" but is a web trick — and it slightly harms scanability and alignment ([R§1.4](V2-RESEARCH.md): normalized covers read calmer). Half-a-degree of tilt is the worst of both worlds: too little to feel physical, enough to feel slightly off. Either **commit** to a real object metaphor (weight, a ledge/edge, depth, a considered rest angle) or **drop it** for calm alignment. Don't fake it. |
| **Full-colour emoji "stamps"** (🍵 ✉️ 🌿 ☕ mixed with ◆ ✦) | **FIX** | Two problems. (1) Glossy full-colour emoji fight the muted matte-riso paper world — they read as *modern app UI* on a *printed object*. (2) The set is inconsistent (some emoji, some geometric glyphs). Keep the *idea* — a maker's mark — but render it as a **printed ink stamp** in the cover's own ink, so it belongs to the paper. |
| **Hover-lift / straighten interaction** | **FIX** | Desktop-only. Mobile has no hover; on touch this does nothing, and there is **no press state** at all. The primary interaction (tap a cover) currently has no designed feedback. |
| **Per-card actions (rename / duplicate / delete)** | **GAP** | Not present in the prototype. Long-press is invisible; there is no discoverable path to a zine's actions. A returning user *will* want to rename or delete — the affordance must exist and be findable without being chrome. |
| **The Maker's Covers themselves** | **KEEP (the content)** | These earn their place completely — they *are* the collection, and they answer "which one is mine?". Everything else on the screen should defer to them. |
| **"Start a zine" matcha dock** | **KEEP → consider CONTENT** | The one primary action; it earns its slot and its thumb-zone placement. Opportunity (not now): could it read as *a fresh blank sheet joining the shelf* rather than a software button — content over chrome? |

**Pattern:** of ~12 elements, **three should be removed outright** (wordmark, count, "8 pages"), **one
de-duplicated** (the title), **three fixed to feel like objects not UI** (tilt, stamps, press-state), **one
deferred** (sort), and **one gap filled** (actions). The screen gets *quieter and more physical* by
subtraction — exactly the owner's direction.

---

## 2. The branding challenge — should "Zinely" appear inside the app? **No.**

The owner's instinct is right, and it follows from first principles:

- **In-app branding answers a question the user isn't asking.** Inside a single-user, offline app, the user
  never wonders *which app is this?* — they launched it. A wordmark on the library is the interface
  reassuring *itself*, not helping the user. It fails every audit question: it can't be created with, it
  reduces nothing, it isn't the user's content.
- **Identity is established at the threshold, then should disappear.** The launcher icon, the (deferred)
  first-run welcome, and the store listing are where "this is Zinely, the warm little zine studio" is
  earned. By the time the user is on the shelf, that job is done. Re-asserting it every visit is like a
  café printing its own name on the inside of every cup you're already drinking from.
- **The one place the brand *should* live is on the artifact that leaves the app** — the exported/printed
  zine and the share surface. There, identity travels with a thing that reaches other people and other
  contexts, so a discreet mark earns its place. *Inside* the library it is redundant chrome; *on the
  shared PDF* it is provenance.
- **When in-app branding genuinely earns its place** (tested against the evidence, corroboration below):
  multi-tenant/shared or team products (you need to know *whose* workspace / which service you're in);
  surfaces where a document will leave the app; onboarding/first-run; and account/settings. **None of
  these describe Zinely's steady-state single-user library.**

**Research corroboration (12 mature apps — the verdict holds, decisively).** A cited survey of what sits at
the top of the primary library across Apple Notes, Journal, Photos, Goodnotes, Craft, Notion, Milanote,
Kindle, Lightroom, Procreate, Bear, and Things:

- **0 of 12 show a purely decorative brand wordmark** on the steady-state single-user library. The dominant
  pattern (10–11 of 12) is a **functional section title** ("Library", "Documents", "Folders" —
  [Goodnotes](https://support.goodnotes.com/hc/en-us/articles/7353710958991-Get-started-with-Goodnotes-6),
  [Apple Notes](https://appleinsider.com/inside/ios-18/tips/inside-apple-notes---everything-you-need-to-stay-focused-flexible),
  [Kindle](https://www.aboutamazon.com/news/devices/how-to-find-your-kindle-library)) and/or the **user's
  own content name** (Notion's workspace, [Craft](https://support.craft.do/en/introduction/navigation)'s
  Space, [Milanote](https://help.milanote.com/en/articles/9860047-what-is-the-home-board)'s home board).
- **The two apparent counter-examples prove the rule.** [Procreate](https://shrushdesign.substack.com/p/procreate-for-beginners-gallery)
  puts "Procreate" top-left of the Gallery — but it is the tap target for **version / About / recovery**, a
  useful button that happens to carry a name, not decoration. [Lightroom](https://helpx.adobe.com/lightroom-cc/using/work-with-lightroom-mobile-ios.html)
  shows "Lightroom" only as a **source tab** (cloud vs. Device vs. Community) — disambiguation, which a
  single-source app doesn't have.
- **Where these apps *do* establish identity:** the launcher icon (the OS renders the name every launch),
  the store listing, first-run/onboarding, exported artifacts/share surfaces, and an About affordance —
  **never a static wordmark on the running library.**

Zinely is the exact profile — single-user, offline, single-workspace, identity already set by icon +
first-run — where a persistent wordmark is textbook redundant chrome. The owner's instinct is correct and
the field is nearly unanimous.

**What replaces it:** most likely **nothing** (let the covers be the top of the screen). If the brand ever
wants a home inside the app, the evidence points to an **About affordance in Settings** (the Procreate
model) — never the library header. And provenance belongs on the **exported/shared PDF**, where the artifact
leaves the app.

---

## 3. First-principles review across the dimensions

**Visual hierarchy.** Inverted at the top: three chrome elements (wordmark, sort, count) claim first
attention before the covers, which should be unambiguously loudest. Removing them lets the collection lead.
The duplicated title also splits emphasis within each card.

**Information hierarchy.** Over-reported. Per card the *only* facts that vary and matter are: the cover
(identity), recency, and format. The prototype adds a constant ("8 pages") and a duplicate (the title),
so ~40% of each card's text is noise. Strip to signal.

**Whitespace.** The grid gaps are close to right, but vertical space is spent on three stacked chrome lines
above the content, and the tilt eats horizontal margin unevenly (covers don't share a baseline). Whitespace
should *frame the covers as objects*, not pad the chrome.

**Typography.** The serif titles are the right editorial move and carry warmth. But the serif *wordmark*
competes with them, and the sans chrome (count, sort) adds a third voice before the content. Long titles
need a defined truncation/wrap rule (untested). Reducing chrome lets the type hierarchy collapse to one
clear thing: cover titles.

**Navigation.** Correctly lean — no bottom bar, one primary. Good. But there's no visible route to a single
zine's *actions*; navigation-in (tap a cover) is clear, navigation-to-manage is missing.

**Discoverability.** "Start a zine" is obvious (good). Sort is discoverable-but-premature. Card actions are
undiscoverable (gap). Net: the *create* path is discoverable; the *manage* path isn't.

**Usability.** Tap targets are fine. The sort control top-right is a thumb-stretch on a tall phone. The
hover model doesn't exist on touch, leaving tap without feedback.

**Cognitive load.** Three chrome elements + a duplicated title per card = avoidable load on a screen whose
job is "recognise your book and tap it." Every removal in §1 lowers it.

**Accessibility.** (The prototype is a static `role="img"` mock, so this is about the *real* screen it
implies.) Watch: cover-title contrast on each maker ink must clear AA (cream-on-matcha/teal likely pass;
ink-on-ochre/strawberry needs checking); emoji stamps need labels or decorative-hiding; the tilt must not
leak into semantics; and removing the count/sort/wordmark also shortens the screen-reader path *to the
covers*, which is an a11y win, not just a visual one. Verify on the platform tree, not merged semantics.

**Emotional response.** The strongest dimension — it *is* warm and cozy, and the two-rooms night mode is
genuinely lovely. What holds it back from "handmade object": the wordmark and count make it read a notch
more "app," and the glossy emoji make it read a notch more "cute software" than "printed thing." The
emotional target is *quiet pride in a shelf of objects*; chrome and emoji dilute it.

**Interaction design.** Under-designed for touch: no press state, a hover behaviour that won't fire, and no
gesture/affordance for actions. The tilt-straighten is a decorative interaction, not a functional one.

**Mobile ergonomics.** Dock is well-placed (thumb zone). Sort is not (top-corner reach). Covers are
reachable. Overall fine once the top-corner control goes.

**Visual balance.** The alternating tilt introduces low-grade imbalance (covers don't align), and the top
chrome is asymmetrically weighted (wordmark left, sort right) around an otherwise symmetric grid. Removing
chrome and settling the tilt restores calm balance.

**Creation flow.** One clear primary ("Start a zine" → paper chooser → bench). Good and low-friction. The
only opportunity is making it feel like *reaching for a fresh sheet* rather than pressing a button (later).

**Editing flow.** Not on this screen — but the shelf must hand off to the bench cleanly (tap → the exact
zine, autosaved as left). That handoff is right in principle; the bench critique comes with its prototype.

**Library browsing.** Covers-first is correct and is the whole thesis. It's undercut *only* by the
redundancies (duplicate title, constant meta) and the emoji/tilt gestures. Recognition should come from the
cover *alone* — which is exactly what the Maker's Cover promises and what the extra text quietly betrays.

---

## 4. The through-line — how the findings ladder to the owner's direction

| Owner's direction | The findings that serve it |
|---|---|
| **less application** | remove the wordmark, the count, the sort |
| **more collection** | de-duplicate the title, strip the constant meta, let covers lead |
| **less interface** | drop chrome; make actions discoverable without adding a toolbar |
| **more handmade object** | commit the physical-shelf metaphor (or drop the fake tilt); replace glossy emoji with printed ink stamps; give tap a real, physical press response |

Almost nothing here is "add." It is "remove, de-duplicate, and *commit* to the object metaphor instead of
gesturing at it." That is the right shape for a pre-release pass on a calm product.

---

## 5. What I would change before proposing a new Library (priority order)

**P0 — subtract the chrome (pure removal, zero new design):**
1. Remove the "Zinely." wordmark (§2).
2. Remove the "6 little books" count.
3. Remove "8 pages" from the meta.
4. De-duplicate the title — it lives on the cover, not twice.

**P1 — make it a collection of objects (commit, don't gesture):**
5. Replace full-colour emoji stamps with printed ink marks in the cover's ink; make the set coherent.
6. Resolve the tilt: either a real physical-object treatment (weight/ledge/considered rest) or clean
   alignment — not a half-degree CSS tilt.
7. Give a cover a real **press** state; drop the hover model.

**P2 — fill the gap / defer the premature:**
8. Add a discoverable path to a zine's actions (rename/duplicate/delete) that isn't a toolbar and isn't
   invisible-long-press-only.
9. Defer sort until the collection is large enough to need it; when it returns, make it quiet and
   thumb-reachable.

**P3 — verify before freeze:**
10. AA contrast of cover titles on every maker ink; long-title truncation rule; screen-reader order after
    the chrome is removed; the empty-state variant against the same principles.

---

## Cross-references
[mockups/v2-library.html](mockups/v2-library.html) · [V2-TOKENS.md](V2-TOKENS.md) ·
[V2-PRINCIPLES.md](V2-PRINCIPLES.md) (Principles 1, 3, 5, 8) · [V2-IA-JOURNEYS.md](V2-IA-JOURNEYS.md) ·
[V2-RESEARCH.md](V2-RESEARCH.md) §1.4, §2.6 · [V2-CRITIQUE.md](V2-CRITIQUE.md) (the V1 Library findings this
carries forward).

*Compiled 2026-07-27. Critique feeding a revised Library prototype and later reviewed decisions — not a
redesign and not a ratified decision. Branding verdict corroborated by a 12-app survey (§2).*
