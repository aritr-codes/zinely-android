# THE CONSTITUTION OF ZINELY

**Status:** Founding document. Every future roadmap, feature, HTML prototype, ADR, and implementation must obey it. If a future feature violates this constitution, the feature loses. Not the constitution.
**Inputs reconciled:** `zinely-v1-product-vision.md` (the romantic case) and `zinely-hostile-review.md` (the evidentiary case). Neither survives intact; what follows is what both were reaching for.
**Amendment rule:** a principle may be amended only by a document of this rank — never by a feature that needs the exception.

---

## I. The Job

Keep asking why:

*Make zines* — why? → *Make a small book of your own* — why? → *Put something you thought or felt into a form that exists outside your head* — why? → *Because a finished thing you can hold, give, or keep is proof that you are a person who makes things — not only a person who scrolls through things other people made.*

That is the floor. We cannot go further down.

**Zinely exists to do this job better than every other product: to take someone from a spark to a *finished thing of their own* — small enough to actually finish, real enough to give away.**

Every other creative tool on a phone optimizes for output quality, output quantity, or engagement time. None optimize for *the ending*. Documents rot in drafts; canvases scroll forever; feeds have no bottom. The unserved human need is not "more creative power" — it is **completion**: the felt experience of *done, and it's mine, and here it is*.

The zine is our form because it is the smallest complete book civilization has produced: one sheet, eight pages, one sitting, a front cover and a back cover — a built-in ending. The fold is not the job. The fold is a brilliant servant of the job.

---

## II. The North Star

**FINISHING.**

One word. Not transformation — the strongest rival, and the hostile review's own "famous for one thing" answer — because transformation fires once per zine, at the end; finishing is the engine that gets anyone *to* that moment and the loop that brings them back to start another. A product can stage a magnificent reveal that almost nobody reaches; it cannot have a finishing problem and a great reveal at the same time. Transformation is finishing made visible. Not ownership (that is finishing's precondition — you can only own what exists). Not print (that is one body a finished thing can take). Not delight (that is what finishing feels like when the tool doesn't get in the way).

**Defense, from the evidence both documents produced:**

- The dominant failure of creative tools is not bad output; it is *abandonment* — the blank-start problem and long-tutorial drop-off are the best-documented effects in the entire research base (≈70% onboarding skip; completion collapsing with step count; blank-canvas abandonment literature).
- The psychology both documents agreed on — IKEA effect, goal-gradient, peak-end — all *require completion to fire*. Effort creates love only for finished things. An abandoned draft generates the opposite of love.
- The hostile evidence that demolished "print is the peak" (printer decline, mail-order unanimity, digital-zine culture) did **not** touch the appetite for *finished tangible artifacts* — the analog revival, the Etsy printables economy, and zine culture itself are all evidence of hunger for *completed personal objects*, in whatever body.
- Every survivor of the hostile review serves finishing: the 8-page ladder, finished-looking starters, the reveal, backup (a finished thing must not die), digital output (a finished thing must be able to travel), honest print (a finished thing must not be ruined at the last step).

**The aim of Zinely is the finish rate — the fraction of started zines that reach a finished state — and the return rate to start another. Never daily engagement. Never time-in-app. Time-in-app is a cost the user pays, not a value we create.**

Because Article 3 forbids the product from watching its users, the finish rate is observed only through *consented instruments*: a named beta cohort who know they are one, diary studies and field tests (zine fairs, classrooms), and diagnostics the user can read before choosing to share. If we cannot learn it that way, we do not learn it — the aim outranks the measurement, never the reverse.

Everything else in this constitution is a consequence of taking finishing seriously.

---

## III. The Principles

### Article 1 — Constraints are a gift

**Statement:** Zinely's limits exist to make endings reachable. We choose our constraints on purpose, and we defend them.

**Rationale:** An infinite canvas is a promise of unfinished work. The single sheet with its fixed page count is why a beginner finishes in one sitting: the end is visible from the beginning. Constraint is the feature; capability-growth is the tax we pay reluctantly.

**Tradeoffs accepted:** We will lose users who want more pages, more layers, more everything. We will look "less powerful" in every feature-comparison table, forever.

**Examples in practice:** a new format (a different fold, a different page count) is admissible only if it *also* has a visible ending and one-sitting reachability — never because "users asked for more." A tool joins the tray only if it displaces or nests another; the tray never merely grows.

**We intentionally reject:** infinite canvases · unbounded page counts · feature parity with design suites as a goal · "power user mode" as an excuse to bloat the default.

### Article 2 — Every zine gets a body

**Statement:** Work is not finished until it exists *outside* Zinely — as an artifact that never needs us again. Every finished zine must be able to take a physical body (paper, via whatever printing means the user has) and a portable digital body (a file or images that travel anywhere), and both bodies must be *true*: what we showed is what they got.

**Rationale:** The job is proof-you-made-a-thing. A thing that lives only inside an app is a hostage, not a thing. This is also the honest reconciliation of the two input documents: the vision sacralized the paper body; the review proved the digital body is how the culture actually travels. The constitution refuses the choice — the *embodiment* is sacred, the medium is the user's.

**Tradeoffs accepted:** Export fidelity is expensive engineering forever (parity rendering is a constitutional obligation, not a nice-to-have). Supporting bodies we don't control (home printers, copy shops, other people's screens) means owning failure modes we didn't cause.

**Examples in practice:** preview-equals-output is an invariant, not a QA goal. "Save" saves, findably. A character that can't render is announced, never silently dropped. Print guidance treats the user's equipment with respect, not assumptions.

**We intentionally reject:** formats readable only inside Zinely · any body that requires our servers to exist · "it looks different printed" as an acceptable bug class.

### Article 3 — The user owns everything, and ownership includes survival

**Statement:** Zines live on the user's device, leave it only by the user's hand, and must survive us: our death (the app's), the device's death, and the user's change of mind. No accounts, ever. Nothing leaves the device without an explicit act. And because there is no cloud to catch them, *durability and user-held backup are constitutional duties, not features* — an ownership model that loses your work when your phone drowns is not ownership, it is negligence wearing a halo.

**Rationale:** Ownership over convenience. Everyone else's business model requires holding the user's work; ours forbids it. This is the one moat no incumbent can cross without ceasing to be themselves. The hostile review's deepest finding — the data-loss time bomb — is not an argument against this principle; it is the second half of it.

**Tradeoffs accepted:** No sync magic. No recovery we can perform for them. Slower iteration (no telemetry — learning about users happens with their knowledge and consent, or not at all). Whole monetization categories foreclosed.

**Examples in practice:** atomic saves; a user-held backup format and a moving-devices path treated with Article-2 seriousness; if the project dies, the last release still works offline forever and everything exports.

**The foreseeable hard case, settled now:** "user-held backup" means user-controlled *destinations* — the share sheet, the user's own storage, media, or drive of choice. Zinely operates no servers, ever; a backup path that depends on infrastructure we run is unconstitutional even as an opt-in. Helping the user put their work anywhere *they* control is this article's duty; holding it ourselves is this article's violation.

**We intentionally reject:** accounts · cloud-first workflows · telemetry/analytics inside the product · any revenue that monetizes user data or attention · convenience arguments for any of the above.

### Article 4 — The tool is quiet

**Statement:** Zinely never demands attention. It does not summon, guilt, streak, badge-nag, or interrupt. It competes by being *wanted*, and it accepts the retention numbers that follow.

**Rationale:** The product's psychological promise is relief from exactly the mechanics that make people feel like products. A finishing tool that nags is a contradiction: guilt produces abandonment, not endings. The hostile evidence (prompt-fatigue, streak-anxiety, the collapse of daily-prompt products) is permanently on file here.

**Tradeoffs accepted:** Worse D30 than anyone with notifications. No growth loops we can point at in a deck. Celebration is allowed only at genuine endings (the finished zine, the held book) — earned, brief, and never a lever.

**Examples in practice:** zero push notifications; prompts wait on a shelf to be browsed, they do not arrive; "come back" is a sentence Zinely has no mechanism to say.

**We intentionally reject:** streaks · daily anything · push notifications · engagement metrics as goals · FOMO mechanics · gamified progress that outlives the zine it belonged to.

### Article 5 — Honest, all the way down

**Statement:** Zinely never claims what is not true — in pixels, copy, or silence. Every promise the interface makes (a preview, a "saved," a supported character, a print setting) is kept or loudly retracted. Every failure offers an exit, not only a retry.

**Rationale:** Trust is the only asset a quiet, offline tool accumulates. One silent data loss spends years of it. Warmth without truth is the worst combination — a kind voice telling you your poem printed when its characters rendered blank.

**Tradeoffs accepted:** Honesty is often ugly ("this font can't print 'ñ' yet"). We ship admissions competitors would hide, and our release notes read humbler than our work deserves.

**Examples in practice:** known limitations documented where users will find them; errors that name what happened and offer an alternate path; no system-error grammar, but no euphemism either.

**We intentionally reject:** silent failure of any kind · overpromising copy · warm dead-ends (kindness as a substitute for an exit) · marketing claims the airplane-mode test can't verify.

### Article 6 — The first minute belongs to the beginner

**Statement:** Within the first minute, a person who has never made anything must have made something that looks worth keeping — without reading instructions, without creating anything resembling an account, without being able to make an unrecoverable mistake. Expertise is revealed progressively; it is never the price of admission. And no path is gesture-only or sight-only: every action has a visible, accessible twin.

**Rationale:** The blank page is the job's front door, and the evidence is unanimous: unstructured starts kill, long tutorials are skipped, and the "aha" that converts is a *good-looking result in minutes* — which means we hand beginners finished-looking starting points and let them make those their own, rather than handing them empty scaffolds and a lecture. Accessibility sits here because "beginner-first" is a lie if it means "able-bodied beginner-first."

**Tradeoffs accepted:** Experts will find the defaults opinionated. Teaching happens inside real work, so our "onboarding" is invisible in screenshots and gets no credit.

**Examples in practice:** starting points that already look like zines; the first mark forgiving and instantly undoable; help that arrives at the moment of need and is skippable at every moment; screen-reader parity as a merge gate, not a backlog.

**We intentionally reject:** mandatory tutorials · tours before doing · empty-state dead-ends · gating the editor behind instruction · any feature whose only usable path is a gesture.

### Article 7 — The maker makes it

**Statement:** Zinely amplifies the user's hand; it never replaces it. The finished zine must be attributable, in the user's own felt sense, to the user. Tools may assist, position, suggest, and correct — they may not *author*. And the aesthetic keeps the hand visible: made-by-a-person over machine-perfect, in every era's technology.

**Rationale:** The job is *proof that you make things*. Generated content is proof that a machine makes things — it dissolves the only value we produce. The IKEA effect is contingent on contribution: automate the authorship away and you automate the love away. This is also the timeless form of "handmade visual identity": not this decade's paper texture, but the permanent commitment that the person's own hand is what the artifact shows.

**Tradeoffs accepted:** We will be slower than every "type a prompt, get a book" product, forever, on purpose. Some starters and supplies walk a line (a template is someone else's layout; a sticker is someone else's drawing) — the test is always: *whose thing is the finished zine?* Materials and scaffolds pass; ghostwriters do not.

**Examples in practice:** supplies are raw materials the user arranges, and the strongest supply source is the user's own camera roll; assistance (snapping, alignment, fallback fonts) corrects execution without touching intent. **The starter bright line:** a starter finished *unmodified* is a known failure of this article — starters must be designed so that overwriting them is the path of least resistance, and a zine that is still someone else's layout with someone else's words has not been made by its maker.

**We intentionally reject:** generative AI that authors content presented as the user's work · one-tap "make my zine for me" · template dependency that leaves the user unable to say "I made this" with a straight face.

---

## IV. Product Identity

**What business are we in?** The finishing business: personal publishing at the smallest possible scale. We sell (or give) the experience of *completing a book of your own*.

**What are we NOT?** Not a design suite. Not a photo editor. Not a social network. Not a print-fulfillment service. Not an asset marketplace. Not a content platform. Not an education company (education may be a *channel*; it is not the identity).

**Who should never use Zinely?** Teams producing marketing assets. Anyone needing real-time collaboration. Anyone who wants the machine to make the thing. Professionals needing prepress control (they have real tools). Anyone measuring their creative life in followers.

**What success do we refuse to chase?** Daily active users. Time-in-app. Viral coefficients. Venture-scale growth. Any revenue that requires knowing things about users they didn't knowingly tell us, or holding their work hostage to a subscription.

**If Canva copied every feature tomorrow, why would Zinely still exist?** Because they can copy the features but not the refusals. Canva's business requires accounts, cloud, upsell, breadth, and engagement; this constitution forbids all five. A feature list without the refusals is a different product with similar screenshots. Whoever holds these refusals *is* Zinely — and the refusals are the one thing a committee at a platform company can never ship.

A constitution has no tempo, but the founding team does: refusals answer why we survive *after* being copied; only shipped charm answers how we matter *before* it. Urgency is roadmap rank — recorded here once so its absence below is read as delegation, not denial.

---

## V. The Sacred Things (change ≈ never)

1. **The trust model** — on-device, no accounts, nothing leaves without the user's hand, work survives us (Article 3).
2. **The ending** — every project has a reachable finished state and a body outside the app (Articles 1, 2).
3. **The quiet** — no summoning, no guilt, no engagement machinery (Article 4).
4. **The voice** — warm, second-person, concrete, jargon-free, honest before kind (Article 5). Tone is architecture: it survives redesigns.
5. **The first minute** — beginner-safe, instruction-free, accessible, un-losable (Article 6).
6. **Authorship** — the maker makes it; the hand stays visible (Article 7).
7. **Truth of preview** — what Zinely shows is what the body will be (Article 2).
8. **Editing philosophy** — direct manipulation of the thing itself, an interaction vocabulary small enough to hold in one head, and every action with an accessible, visible twin — in whatever input technology an era provides (Articles 6, 7).

## VI. We Proudly Do Not Build

- Accounts, logins, or profiles
- Social feeds, followers, likes, comments, or any algorithmic surface
- Push notifications of any kind
- Streaks, daily quests, or guilt mechanics
- Telemetry, analytics, or A/B harnesses inside the product
- Advertising, or any surface an advertiser could buy
- Generative AI that authors the user's content
- Real-time cloud collaboration (zines are swapped, gifted, and co-made *in person* — the culture's own collaboration model needs no server)
- Subscriptions that paywall creation itself (if money ever changes hands, it buys optional *things*, once — never rent on creativity)
- A content treadmill (seasonal drops, rotating catalogs, or any inventory whose value decays unless we keep feeding it)
- Cloud-first workflows, sync-required features, or servers our users' work depends on
- Proprietary formats designed to resist leaving
- Engagement dashboards for ourselves (we refuse to optimize what we refuse to measure)

## VII. The Feature Tribunal

Every feature proposed by either input document, judged by one question: *which article does it serve?* Features with no article die here, whatever document loved them.

> **Rank note:** the *articles* are constitutional; this table is their first ratified **application** and is amendable at roadmap rank. Un-deferring a deferred feature is a roadmap decision that must cite an article; only overturning an article requires constitutional revision.

| Feature (from either document) | Article served | Verdict |
|---|---|---|
| The reveal (pages → sheet → folded book, animated) | 2, 5 — finishing made visible; proof the scramble is true | **KEEP — flagship.** It is Article 2 performed as theater. |
| Non-Latin honesty + fallback coverage | 5, 2 | **KEEP — first duty.** Silent blank text violates two articles at once. |
| User-held backup / restore / migration | 3 | **KEEP — constitutional debt, overdue.** |
| Digital body (read mode, portable images/PDF, share card) | 2 | **KEEP — co-equal body.** |
| Real covers on the shelf | 5, 7 — show the user their own work | **KEEP.** |
| Finished-looking starters | 6, 7 (they expect to be overwritten) | **KEEP.** |
| First-run fold moment + seeded first project (skippable) | 6 | **KEEP** in its skippable, non-gating form only. |
| Direct manipulation + verb bar + snapping + visible twins | 6, 8 (sacred editing philosophy) | **KEEP.** |
| Small bundled supplies + fonts, license-clean | 7 (raw materials) | **KEEP, small.** Quality is the constraint (Article 1 applies to our own inventory). |
| Packs from the user's own photos | 7 — the purest expression of the article | **KEEP — the supplies strategy.** |
| Test-fold sheet, calibration, print triage, copy-shop preset | 2, 5 — respect for the paper body | **KEEP** for those who take that body. |
| Static prompt library, browsed not delivered | 6 | **KEEP, quiet form only** (Article 4 forbids the daily form). |
| Page duplication / reordering | 1-compatible convenience | **KEEP, minor.** |
| Downloadable pack catalog + manifests + seasonal drops | none — violates 4 (treadmill) and strains 3 | **DEAD.** Both documents' analyses converge here. |
| Prompt-of-the-day (daily framing) | violates 4 | **DEAD.** |
| Streak-adjacent celebration / graduation *counters* | violates 4 | **DEAD** as mechanics. A finished zine may *look* finished (a made object on the shelf) — state, not score. |
| Panoramic spreads, washi rotation-snap, stamp micro-behaviors | charm without an article | **DEFERRED INDEFINITELY.** Admissible someday as Article-7 craft; never before constitutional debts are paid. |
| Facing-pairs strip (fold awareness in the editor) | 2 — the body made visible while working | **KEEP, unhurried.** After the reveal ships; the reveal teaches the fold first. |
| Fold-along ritual (full-screen fold guide) | 2, 5 | **KEEP, minor** — polish of an existing surface, not new scope. |
| Font packs pipeline | 7 (materials) | **DEFERRED** — bundled fonts serve the article; a pipeline is inventory (Article 1 applies to us). |
| 16-page / other formats | must pass 1 | **ADMISSIBLE BY PROCEDURE** — visible ending + one-sitting reachability, proven on paper first; never "users asked for more." |
| Smart photo slicing | rides panoramic spreads | **DEFERRED with them.** |
| Drawing / handwriting layer | 7 — the hand, literally | **ADMISSIBLE someday**; sequenced after constitutional debts (backup, honesty, bodies) are paid. |
| Education kit, zine-fair presence | channels, not features | **PERMITTED** — they spend no constitutional budget; they are how a quiet tool is found. |
| Monetization experiment (one-time IAP packs) | must pass 3 and 4 | **ON TRIAL, with a trigger:** decide within one year of the first bundled-supplies release. If the experiment isn't run by then, or store-billing plumbing offends Article 3's spirit, free-forever is thereby ratified and we say so proudly. No third option; the non-choice the hostile review flagged is itself unconstitutional. |

## VIII. The Ten-Year Test

Struck from constitutional rank because they are contingent on today: printer-market trends · Canva/Adobe/anyone as named threats · Material 3, bottom sheets, today's gesture conventions · Android itself · the 45° hatch fill and this decade's paper texture · "8 pages" as a number (the *reachable ending* is constitutional; today's best-known reachable ending is one folded sheet of eight pages) · PDF as a format (the *portable body* is constitutional; PDF is today's body).

What must still be true in ten years, on whatever glass or paper exists: **people will still start things they don't finish, still scroll more than they make, still feel the difference between showing a screen and handing someone a thing they made. Zinely is the tool that ends that sentence with a finished thing. Small enough to finish. Real enough to give. Yours enough to keep.**

---

*Ratified as the founding baseline. Amendments require this document's rank: a deliberate constitutional revision — never a feature ticket, never a trend, never a competitor's launch.*
