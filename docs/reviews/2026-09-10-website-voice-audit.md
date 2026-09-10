# Website voice audit

## Second pass: paper, personality, and interaction

The owner's first-pass feedback was that warmth alone did not deliver enough humor. This second pass is explicitly
authorized to improve presentation and interaction as well as copy, while preserving the existing information architecture.

### Research before implementation

- **Verified observation, creative studio:** [Mouthwash](https://mouthwash.studio/) uses a brief identity statement,
  project-led navigation, and editorial side projects. **Application:** make Zinely's maker story concrete and let the
  object do some explaining. Its agency vocabulary is not a voice model for Zinely.
- **Verified observation, unconventional brand:** [MSCHF](https://mschf.com/) presents named projects as invitations
  to investigate. [Dollar Shave Club](https://us.dollarshaveclub.com/) pairs irreverence with literal product and support
  information. **Application:** a short curiosity hook followed immediately by a useful explanation; no shock tactics.
- **Verified observation, indie / unusual products:** [Neal.fun](https://neal.fun/) identifies the person behind the
  experiments in plain language. [Are.na](https://www.are.na/) explains actions and its independent business directly.
  **Application:** say why two people built this particular thing, and show page arrangement with one reversible action.
- **Verified observation, internet culture:** [Cameron's World](https://www.cameronsworld.net/) preserves personal,
  eccentric web fragments. [Know Your Meme's This Is Fine entry](https://knowyourmeme.com/memes/this-is-fine) documents
  the mismatch between calm words and an absurd situation. **Application:** use understatement about writing software
  for folded paper. Do not reproduce memes, characters, slang, or somebody else's joke.
- **Verified gallery observations:** [Hoverstat.es](https://www.hoverstat.es/) describes portfolios using scattered
  stacks, contact sheets, restrained hover changes, and mechanical controls. These are gallery descriptions, not a
  claim that we interactively tested every featured site. **Application:** local paper movement and deliberate presses,
  never scroll effects that delay reading. Poolsuite and Awwwards were also opened, but their limited text responses
  do not support detailed interaction claims and did not drive implementation.
- **Verified accessibility guidance:** [W3C animation from interactions](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html)
  supports disabling nonessential interaction animation. **Application:** the same content and state changes work
  instantly with reduced motion, and touch/keyboard can reach every discovery.

### Direction and opportunities

Keep the main headline, product screenshots, workflow, ten-step instructions, and direct download path. Give the hero
a handwritten editorial aside, shorten excessive viewport-dependent empty space, and refine depth using paper edges.
Retain system fonts and existing assets. Plain facts surround the jokes.

Five planned moments: (1) a bounded pointer response on the hero paper with a native disclosure note for touch and
keyboard; (2) a page-order comparison that explains print arrangement, explicitly not a folding simulation; (3) a
small optional zine-idea picker; (4) folding-guide position feedback and a final-step aside without claiming physical
completion; (5) a native footer disclosure with a dry paper joke. No autoplay, random timed copy, sound, confetti,
tracking, animation library, or dependency is needed.

The homepage will carry the expressive changes. Secondary factual pages retain their release and policy content.
The public roadmap still contains older Reframe status; that factual maintenance is recorded for follow-up rather
than silently bundled into this creative task.

Voice rules live in [VOICE.md](../design/VOICE.md#public-website-voice), so this audit does not become a second voice authority.

### Second-pass implementation and validation

- Preserved the headline, core product definition, download destinations, screenshots, ten folding instructions,
  and factual accessibility/privacy/release material. Added specific dry asides, a more direct maker story, and
  selective screenshot captions. The purpose is stronger character without losing the plain-language floor.
- Implemented the five planned moments with CSS, native disclosures, and small JavaScript enhancements. The
  page-order comparison states the actual imposed order and never claims to simulate physical folding.
- Refined homepage spacing, paper edges, card shadows, editorial serif asides, mobile branding, and tap targets.
  No assets, fonts, runtime dependencies, navigation routes, or Android code were added or changed.
- Expanded `tools/check-website.cjs` and passed in headless Edge: actual print-position geometry and inversion,
  keyboard/reversible toggle behavior, native disclosures, idea changes, emulated touch, pointer reset on reduced
  motion, instantaneous reduced-motion comparison, existing folding tests, 320/390px and 200% text reflow,
  no-JavaScript and print fallback, axe checks, and zero page errors.
- Physical-device and novice paper-folding success are not established by browser automation.
- W3C Nu validation returns zero errors (one existing informational redundant-list-role advisory). JavaScript
  syntax and diff-whitespace checks pass. All 27 homepage link targets remain byte-identical; added lines across
  the complete patch contain zero em dashes. HTML, CSS, and JavaScript together add approximately 3.3 KB gzipped
  relative to PR #68, with no new network requests or dependencies.

## First-pass historical record (PR #68)

The following records the earlier copy-only pass, not the expanded scope of the second pass above.

**Date:** 2026-09-10

**Scope:** Public website information architecture and reader-facing copy.

**Outcome:** Selective copy revision only. Product behavior, routes, legal text, release facts, roadmap status,
store availability, and privacy or accessibility claims are unchanged.

## Research

### Direct observations

- ✅ **VERIFIED:** [Kinopio](https://kinopio.club/) explains its unusual canvas with familiar actions and concrete
  objects, then places its more eccentric lines at the edges of the page. Its
  [About page](https://kinopio.club/help/about) names the maker and the kind of software he wants to build. The
  personality feels credible because it is attached to a specific product philosophy and a real person.
- ✅ **VERIFIED:** [Are.na](https://www.are.na/) leads with plain product definitions, then earns more imaginative
  language by showing what its nouns mean through a short Capture, Arrange, Search, Connect sequence. Its lack of
  ads and personalized recommendations is stated as a concrete product property, not a broad virtue claim.
- ✅ **VERIFIED:** [Obsidian's manifesto](https://obsidian.md/about) turns differentiators into short principles
  backed by specific behavior: local data, open formats, privacy, and independence. It does not need superlatives
  to sound confident.
- ✅ **VERIFIED:** Mailchimp's [voice and tone guide](https://styleguide.mailchimp.com/voice-and-tone/) recommends
  plain language, warmth, subtle humor, and adjusting tone to the reader's emotional state. Its concise
  [summary](https://styleguide.mailchimp.com/tldr/) puts clarity and usefulness ahead of entertainment.

### Principles used

- 🟦 **RECOMMENDATION:** Make the core explanation work with nouns and verbs before adding personality.
- 🟦 **RECOMMENDATION:** Put wit in low-risk moments such as section headings, journey labels, and hidden details.
  Keep installation warnings, folding instructions, privacy, accessibility, and release history literal.
- 🟦 **RECOMMENDATION:** Let the makers speak in the first person only where the repository supports the claim.
  Zinely's README establishes the unmet product combination and identifies Aastra as an independent two-person
  team.
- 🟦 **RECOMMENDATION:** Prefer one memorable, product-specific line over a stack of jokes. A visitor should still
  understand Android, eight pages, one sheet, offline use, the beta status, and the printable output while scanning.

These findings extend the existing public-site evidence in
[RESEARCH R16](../RESEARCH.md#r16-public-product-site-and-optional-fold-motion) without changing its information
architecture or accessibility decisions.

## Copy audit

### Strong copy intentionally retained

- The hero headline, **“Make a little zine.”**, is short, direct, and specific.
- **“Eight pages. One sheet. Yours.”** explains both the constraint and the payoff without jargon.
- **“Physical media instead of social media.”** is the clearest compact expression of the product's point of view.
- The gallery labels, trust list, beta warning, folding guide, feedback explanation, and accessibility statement
  already answer concrete visitor questions. They were not rewritten for novelty.
- The 404 page's **“A loose page”** framing already has restrained personality and remains unchanged.

### Weaknesses selected for revision

- The hero repeated “bring together” language used again in the first how-it-works step.
- The three journey steps had parallel generic headings and hid Zinely's most unusual benefit, automatic page
  arrangement, behind “puts every page in the right place.”
- The About opening used “a simple wish,” language that could belong to almost any small creative app. It did not
  tell the more specific, repository-backed story: the team wanted this exact Android zine tool and could not find it.
- The Changelog and Roadmap link descriptions were correct but administrative.
- The final download block repeated a generic creation invitation instead of giving the reader a concrete spark.

## Changes

- Standardized both primary download actions as **“Get the Android beta”** and changed the hero's secondary action
  to **“Take a look inside.”** The repeated primary label is deliberate because both links perform the same action.
  Their destinations and download URL are unchanged.
- Made the three-step explanation more conversational: gather useful material, move it around freely, then let
  Zinely handle the unusual page arrangement needed for folding.
- Rewrote About around the product's real origin and two-person team. The revised story adds no capability claim.
- Changed the open-notebook heading and link summaries to sound like people reporting their work, not a product
  portal filing system.
- Changed the final invitation to **“There is probably a zine hiding in your camera roll.”** This is the one
  deliberately memorable line, grounded in the shipped photo-import path.
- Added one harmless source-level easter egg for visitors who inspect the HTML. It is optional and does not alter
  page behavior or visible content.

## Validation

- The repository's full `tools/check-website.cjs` suite passed in headless Edge: keyboard behavior, ten-step fold
  controls, reduced motion, mobile and 200% reflow, no-JavaScript and print fallbacks, axe checks, and zero page
  errors.
- W3C Nu 26.9.9 returned zero HTML errors. Its only message was the existing informational advisory that the
  folding guide's `role="list"` is unnecessary on an `ol` element.
- All 27 homepage link targets remain byte-identical to the prior page. `node --check website/assets/site.js` and
  `git diff --check` passed.
- Current desktop and 390-pixel mobile renders of every visibly changed section were inspected without clipping or
  reflow regressions.
- Added website-task lines and this audit contain zero em dash characters. Independent review returned **GO** with
  no required fixes.

## Boundaries and follow-up

- No legal, privacy, accessibility, install-safety, version, platform, availability, roadmap-status, or release copy
  changed.
- No navigation label, route, link target, CSS rule, script, screenshot, or app file changed.
- 🟨 **ASSUMPTION:** “Camera roll” is familiar enough to Android visitors to work as a warm shorthand for photos on
  the phone. First-visitor feedback should test that line rather than treating this review as user evidence.
- 🔭 **FUTURE:** If real visitor feedback shows uncertainty about sideloading, test a separate plain-language
  “How to install the beta” page. Do not make the hero carry more installation detail.
- 🔭 **FUTURE:** Consider a short maker note or build diary only when there is enough genuinely interesting material
  to maintain it. An empty blog would weaken the independent voice rather than strengthen it.
