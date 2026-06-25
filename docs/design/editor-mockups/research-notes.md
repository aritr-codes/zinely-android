# Editor Mockups — Research Notes

> Scope: concrete, modern patterns pulled from best-in-class mobile creative apps + Material 3,
> used to ground the **throwaway** HTML/CSS mockups in this folder (`project-list.html`,
> `onboarding.html`, and the shared `style.css`). These are layout/flow references only — **not a spec**.
> Labels per project [Research standards](../../../CLAUDE.md): ✅ VERIFIED (sourced) · 🟦 RECOMMENDATION.

---

## Home / project gallery

- ✅ **VERIFIED — A FAB is the canonical M3 "create" affordance, bottom-right, ≥56dp.** Material 3
  specifies the FAB as the screen's single most-emphasized action, resting above content in the
  bottom-right and using a 56dp container (16dp inset). We map FAB → "New zine".
  [Material 3 — FAB](https://m3.material.io/components/floating-action-button/overview) ·
  [Material 3 — FAB accessibility/specs](https://m3.material.io/components/floating-action-button/specs)

- ✅ **VERIFIED — Canva's mobile home leads with a prominent create entry + a recent-designs
  rail/grid of thumbnails.** Canva surfaces "your designs" as image-first cards so users
  re-enter work fast; creation is a persistent, high-emphasis action rather than buried in a menu.
  [Canva Help — Create a design on mobile](https://www.canva.com/help/create-a-design/) ·
  [Canva mobile app](https://www.canva.com/download/)

- ✅ **VERIFIED — Adobe Express mobile home is project-grid first with quick-start templates.**
  The home/"Your stuff" surface shows existing projects as thumbnails and a quick path to start
  something new, reinforcing thumbnail-grid + prominent-create as the dominant pattern.
  [Adobe Express mobile](https://www.adobe.com/express/feature/mobile)

- ✅ **VERIFIED — Material 3 / Google Photos use a dense multi-column thumbnail grid with
  long-press to enter multi-select.** Long-press on a grid item selects it and switches the app bar
  into a contextual selection bar (count + bulk actions). We adopt long-press → multiselect + a
  contextual top-app-bar.
  [Material 3 — Lists & grids](https://m3.material.io/components/lists/overview) ·
  [Material 3 — Top app bar (contextual)](https://m3.material.io/components/top-app-bar/overview)

- 🟦 **RECOMMENDATION — Thumbnails should preview the actual zine page at a print-ish aspect.**
  Zines print to paper, so a portrait-ish card (≈3:4) reads truer than a square. Show title +
  page count + "edited" recency as a 2-line supporting block under each thumbnail (M3 list/card text).

- 🟦 **RECOMMENDATION — Keep the home top app bar minimal (title + search/overflow), let the FAB own
  "create".** Avoids competing primary actions; matches Canva/Express where create is one obvious tap.

## Empty state / first-run onboarding

- ✅ **VERIFIED — Prefer a short, benefits-oriented, skippable onboarding over a long upfront tutorial;
  contextual/just-in-time guidance beats front-loaded walkthroughs.** NN/g's analysis of mobile
  onboarding finds users skip long tutorials, and that benefits-oriented + contextual onboarding
  outperforms feature-tour carousels. We use a single value-prop empty state with one primary CTA,
  no multi-slide carousel.
  [NN/g — Mobile-App Onboarding: Components & Techniques](https://www.nngroup.com/articles/mobile-app-onboarding/)

- ✅ **VERIFIED — A first-run empty state should pair an illustration + one-line value prop + a single
  primary action.** Material 3's empty-state guidance: explain what the surface is for and give one
  clear next step rather than a blank screen.
  [Material 3 — Empty states](https://m3.material.io/foundations/content-design/empty-states) ·
  [Android — Designing for empty states](https://developer.android.com/quality/principles)

- 🟦 **RECOMMENDATION — Lead the empty state with the privacy promise.** Zinely's differentiator is
  offline + no account ([PRD principles](../PRD.md#5-product-principles-non-negotiable)). Saying
  "No account. Works offline. Stays on your phone." in the first-run state turns a constraint into the
  hook, instead of a generic "Welcome" carousel.

- 🟦 **RECOMMENDATION — The empty-state primary CTA and the home FAB resolve to the same action
  ("New zine").** One creation verb everywhere keeps the model simple for the beginner-first audience.

## Editor affordances (shared vocabulary the other mockups build on)

- ✅ **VERIFIED — On-canvas direct manipulation: drag to move, pinch to scale, two-finger rotate, with
  visible selection handles.** This is the established mobile canvas gesture set across creative tools
  (Canva, Keynote, Figma mobile); selection shows a bounded outline + corner handles for resize/rotate.
  [Canva Help — Move, resize & rotate elements](https://www.canva.com/help/edit-elements/) ·
  [Material 3 — Touch target ≥48dp](https://m3.material.io/foundations/designing/structure#touch-target)

- 🟦 **RECOMMENDATION — Thumb-reach bottom tool bar (icons + labels) for primary editor tools; a
  contextual toolbar appears near the current selection for object-specific actions.** Keeps reach
  ergonomic on tall phones and matches Canva's bottom toolbar model. All targets ≥48dp.

---

## Top 3 takeaways applied to these mockups

1. **FAB owns "create"** (bottom-right, 56dp) on the home grid; the onboarding CTA resolves to the
   same "New zine" action. [M3 FAB](https://m3.material.io/components/floating-action-button/overview)
2. **Thumbnail-grid home, long-press → multiselect → contextual app bar**, mirroring Google Photos /
   Adobe Express. [M3 Lists](https://m3.material.io/components/lists/overview)
3. **One short, benefits-led, skippable empty state** (illustration + value prop + single CTA), no
   multi-slide tour — and lead with the privacy promise.
   [NN/g onboarding](https://www.nngroup.com/articles/mobile-app-onboarding/)
