# V2 frozen-spec defect register

> **What this owns:** defects found *in the frozen V2 design artifacts* during Compose implementation —
> contradictions, stale text, omissions, and disagreements between two frozen files.
>
> **What it does not own:** implementation bugs (those are code review findings), design changes (those
> are owner amendments to the freeze), or deviations *from* the spec (those are logged in the PR and the
> relevant ADR).

## Why this register exists

The [implementation rules](../COMPOSE-IMPLEMENTATION-RULES.md) say that when a frozen artifact itself
looks wrong, the implementer **stops and raises it** rather than editing Compose around it or quietly
"correcting" the design. That rule produces findings that belong to the *design corpus*, not to any one
code change — and before this file existed they had nowhere to live, which meant the only ways to record
one were to bury it in an unrelated ADR or to let it evaporate.

Entries here are **not blockers by default.** A defect is logged, classified, and left for the design
corpus to clean up. If one genuinely blocks implementation, that is stated explicitly in its row and the
work stops until an owner ruling lands.

## How to use it

- One entry per defect. State what the artifact says, why it is wrong, and whether implementation depends
  on it.
- **Never fix the frozen artifact from an implementation session.** Amending a frozen surface is an owner
  act ([V2-CONSTITUTION.md §VI](V2-CONSTITUTION.md)); this register is the queue that feeds it.
- When an entry is resolved, mark it Resolved with the commit that cleaned it up. Do not delete it — the
  record of what was once contradictory is what stops it being reintroduced.

---

## Open

### D-001 — `v2-bench.html` header contradicts the freeze record

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-bench.html`](mockups/v2-bench.html), header comment |
| **Found** | 2026-07-28, during Phase A / A1 (independent review of the V2 chrome palette) |
| **Severity** | Documentation defect — **does not block implementation** |
| **Status** | Open |

**What it says.** The file's own header makes both claims, nine lines apart:

- line 3 — `Zinely V2 — the Bench (Editor) — canonical HTML prototype. 🔒 DESIGN FROZEN 2026-07-28 (owner-approved; V2-BENCH-REVIEW.md §E.6).`
- line 10 — `NOT frozen. For owner critique.`

**Why it is wrong.** Line 10 is stale text carried over from the pre-freeze critique revision; the freeze
commit did not strip it. The freeze is real and is the operative status: commit
`4494e95 docs(v2): Bench (Editor) — 🔒 DESIGN FREEZE (owner-approved 2026-07-28)` is the last commit to
touch the file and **is an ancestor of `main`**, the working tree is clean, and
[COMPOSE-IMPLEMENTATION-GUIDE.md §3](../COMPOSE-IMPLEMENTATION-GUIDE.md) records the Bench as frozen at
that commit. Line 3 is correct; line 10 should be deleted.

**Does implementation depend on it?** No. The Bench's `:root` token block — the part A1 consumed, and the
part [ADR-071](../DECISIONS.md#adr-071) makes canonical for shared V2 implementation tokens — is
unambiguous regardless of which header line a reader believes. A1 proceeded on that basis.

**Why it still matters.** The Bench is the single most-read spec in the remaining programme: it is the
canonical source for shared implementation tokens (ADR-071 §2) and the whole authority for Phase C. A file
that tells a fresh session it is "for owner critique" invites exactly the reinterpretation the freeze
exists to prevent — and the risk grows as Phase C reads it more heavily. Clean it up in the design corpus
before Phase C begins.

**Owner disposition (2026-07-28):** *"Correctly identified. Do not modify the implementation. Instead, log
it as a documentation defect against the design repository… No implementation work depends on resolving
it, but it should be cleaned up in the design corpus."*

**Addendum (2026-07-28, A3).** There is a **second** copy of the stale note, in the page footer at
`:367` — *"Fraunces + Inter stand in as Georgia / system-sans, pending real faces at Compose parity. Not
frozen — for critique."* It is user-visible in the rendered prototype rather than buried in a comment, so
the cleanup should strip both. Only the trailing "Not frozen — for critique" is stale: the stand-in
clause is accurate and worth keeping (see D-005's closing note).

### D-002 — two frozen cover inks put their titles below AA for normal text

| | |
|---|---|
| **Artifact** | [`docs/design/mockups/v2-library.html`](mockups/v2-library.html) lines 68, 79-82 |
| **Found** | 2026-07-28, during Phase A / A2 (modelling the `content.*` namespace) |
| **Severity** | Accessibility question — **does not block A2**; owner ruling owed before Phase B's gate |
| **Status** | Open — **owner ruling requested** |

**Measured.** The cover title (`.ct`, `color` from the ink class) against its own ink fill:

| Cover ink | Title on fill | vs 4.5:1 | vs 3.0:1 |
|---|---|---|---|
| Matcha `#F7F2E7` on `#7C8A3F` | **3.380:1** | ✗ fails | ✓ passes |
| Teal `#F7F2E7` on `#47857B` | **3.832:1** | ✗ fails | ✓ passes |
| Strawberry `#4A211F` on `#E27F89` | 4.992:1 | ✓ | ✓ |
| Ochre `#3A2A0E` on `#D19A3C` | 5.535:1 | ✓ | ✓ |
| Paper stock `#2A251E` on `#F1EBDA` | 12.763:1 | ✓ | ✓ |

**The question.** Which floor applies to a cover title? It is genuinely ambiguous, and the ambiguity
is in the corpus rather than in the reading:

- `.ct` is `font-size:1.16rem; font-weight:600` — about **18.56px semibold**. WCAG's large-text
  threshold is **18.66px bold**. The title falls just under on *both* counts (0.1px of size, and
  semibold rather than bold), so treating it as large text is a stretch rather than a fact.
- If it is **normal** text, the floor is 4.5:1 and **Matcha and Teal fail**.
- But cover inks carry **no ★** in [V2-TOKENS.md](V2-TOKENS.md), and
  [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III gates AA specifically on *"the ★-marked pairings"* —
  under which reading no CI floor is owed for cover inks at all.
- Yet [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) Phase B lists **"AA contrast per ink"** among
  the impl-gates that must be met, without naming a level.

So three governing documents point three ways, which is why this is logged rather than decided.

**What implementation did, pending the ruling.** `ZinelyContentInksTest` asserts the **3.0:1** floor —
the minimum defensible gate. This is deliberately *not* a resolution: it is strictly better than no
gate (a genuine regression still fails the build), while refusing to certify at 4.5:1 a pair that does
not clear it. **No frozen value was changed**, and no ink was quietly excluded to make the suite green.

**Why it should not be settled by implementation.** Every available fix is a design change: darken the
two ink fills, lighten their title colour, or raise the title's size/weight past the large-text
threshold. All three alter what the shelf looks like, so all three are owner amendments to the frozen
Library — [COMPOSE-IMPLEMENTATION-GUIDE.md §4](../COMPOSE-IMPLEMENTATION-GUIDE.md)'s golden rule
applies: if the HTML is wrong, the HTML is fixed first.

**Owner decision requested:** which floor governs cover titles, and if 4.5:1, which of the three fixes
should the frozen Library adopt? Needed before Phase B's *"AA contrast per ink"* gate can be assessed.

### D-003 — the maker palette is ten inks or nineteen, depending on which document you read

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) lines 391-394, 460, 463-470 · [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III · [V2-IDENTITY.md](V2-IDENTITY.md) §4 · [V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §H4 |
| **Found** | 2026-07-28, during Phase A / A2 (independent review of the `content.*` namespace) |
| **Severity** | **Specification conflict** — did not block A2; would have blocked the Phase C ink popover |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**The conflict.** Four governing documents describe the maker's palette, and they do not describe the
same thing.

| Source | What it says the maker palette is |
|---|---|
| [V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §H4 (the directive that created it) | *"**~12–18 named inks** in three bands (**inks** · **paper tints** · **neutrals**)"* |
| **The frozen `v2-bench.html`** (the implementation of that directive) | **19 swatches in three bands** — `INKS` (10) + `TINTS` (5) + `NEUT` (4), plus 3 presets |
| [V2-IDENTITY.md](V2-IDENTITY.md) §4 | *"The **10** named riso spot inks the Bench already froze are the source of truth — **verbatim, no invention**"* — and enumerates exactly the `INKS` band |
| [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III | *"Maker inks (the **10-ink** Bench H4 set)"* — inherited from Identity |

The frozen prototype is unambiguous about behaviour: `bandHTML('Inks',INKS)+bandHTML('Paper
tints',TINTS)+bandHTML('Neutrals',NEUT)` (`:460`) renders all three bands, and `applyInk`
(`:463-470`) is bound to **every** `.sw2` swatch — so a maker can apply any of the nineteen as an ink.
The Constitution and Identity describe that same set as ten.

**Why this is not self-reconciling.** The 4-vs-10 puzzle between V2-TOKENS.md and the Constitution
*was* reconciled elsewhere in the corpus ([V2-BENCH-REVIEW.md](V2-BENCH-REVIEW.md) §8 — cover inks and
in-page inks are distinct axes), and A2 followed that ruling. The same search was run for this one and
the candidate reconciler makes it **worse, not better**: V2-IDENTITY.md §4 groups the ten *"for study
into four bands — greens & earth · warm · cool · neutral"*, which is a **different banding** from the
prototype's three, and then says *"any future widening is a Bench-H4 decision, not an Identity one"* —
pointing authority back at the Bench, which has already widened to nineteen. So Identity simultaneously
fixes the set at ten and delegates widening to a document that shows nineteen.

**Three concrete values have no token home.** The `NEUT` band contains `Slate #5B5347`,
`Stone #8C8269` and `Fog #B7AD93`. None appears in [V2-TOKENS.md](V2-TOKENS.md) or V2-IDENTITY.md's
enumeration. Note that Slate and Stone are byte-identical to light-theme chrome `--ink-soft` and
`--ink-faint` — so if they *are* content inks, the chrome/content wall has two sanctioned crossings
beyond the already-known `Ink #2A251E`, and any future "no content value equals a chrome value" lint
must know that up front. `TINTS` similarly contains `Cream #F1E9D6`, which is close to but **not** the
cover stock `#F1EBDA` — near-misses like that are exactly what a token audit exists to catch.

**What implementation did, pending the ruling.** A2 models **only the `INKS` band** — the ten the
Constitution and Identity name verbatim. This is the smallest defensible claim: porting all nineteen
would contradict the Constitution, and treating ten as complete would contradict the frozen prototype,
so the field is documented as *one band of three* and `ZinelyContentInks.makerInks` carries an explicit
"do not read this as the maker's supplies — read D-003 first" warning for the Phase C session.

**✅ RESOLUTION — owner ruling, 2026-07-28.**

> *"The frozen HTML is the authority. The complete maker palette consists of: **Inks · Paper Tints ·
> Neutrals**. The constitutional '10 maker inks' refers only to the INKS band. Paper Tints and Neutrals
> are **separate categories, not additional inks**. Do not merge them. Do not rename them. Implement all
> three groups exactly as frozen when Phase C arrives. Model them as three distinct collections so the
> architecture reflects the product language rather than flattening everything into a single list."*

Neither document was wrong; the question was miscast. The Constitution's "10 maker inks" was never a
claim about the size of the palette — it names **one band within it**. "Ten inks" and "nineteen
swatches" are both true because *ink* is a category, not a synonym for *swatch*.

**Implemented** in [`ZinelyContentInks`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyContentInks.kt)
as `makerInks` / `paperTints` / `neutrals` — three collections of three **distinct types**, which is
what makes "do not merge them" enforceable rather than merely written down: there is no `List<Color>`
the three can be concatenated into, so flattening the palette is a compile error. Recorded as
[ADR-072](../DECISIONS.md#adr-072) Decision 6. The three `PRESETS` are recipes over the bands rather
than tokens and remain Phase C's.

**Two things this surfaced that outlive the defect**, both now pinned by test:

- **Three sanctioned chrome/content value coincidences.** Neutral `Ink #2A251E`, `Slate #5B5347` and
  `Stone #8C8269` are byte-identical to light chrome `ink`, `inkSoft` and `inkFaint`. A future "no
  content value equals a chrome value" lint would therefore be **wrong**; the only value-level rule the
  corpus states is the `consequence` exclusion.
- **`Ink` legitimately appears in two bands** (spot ink *and* neutral), verbatim from source — pinned so
  it is never de-duplicated into one, which would silently change what the popover offers.

### D-004 — the frozen zine content is set in Fraunces; the render engine can only draw Inter

| | |
|---|---|
| **Artifacts** | [`v2-proof.html`](mockups/v2-proof.html) lines 106-123, 210 · [`DocumentFontRegistry.kt`](../../render-android/src/main/kotlin/com/aritr/zinely/render/android/DocumentFontRegistry.kt) lines 101-113 |
| **Found** | 2026-07-28, during Phase A / A3 (typography) |
| **Severity** | **Capability gap between a frozen spec and the shipped engine** — does not block A3 or any of Phase A |
| **Status** | Open — **deferred to Phase D by owner ruling (2026-07-28)**; explicitly *not* decided by A3 |

**What the spec says.** The Proof's zine content — the block the file itself labels `/* zine content
(real, not lorem) */` — is set entirely in `var(--serif)`, which `:22` defines as
`'Fraunces',Georgia,'Times New Roman',serif`:

| Selector | Role | Style |
|---|---|---|
| `.cover h2` (`:110`) | cover title | Fraunces 500, 27px |
| `.cover .sub` (`:111`) | cover subtitle | Fraunces **italic**, 13px |
| `.h` (`:112`) | page heading | Fraunces 500, 19px |
| `.b` (`:113`) | page body | Fraunces 400, 12.5px |
| `.pull` (`:114`) | pull-quote | Fraunces **italic**, 21px |
| `.zlist` (`:115`) | list | Fraunces 400, 13px |
| `.zcap` (`:119`) | photo caption | Fraunces 400, 12px |
| `.backc p` (`:123`) | back cover | Fraunces **italic**, 12px |

**What the engine carries.** `DocumentFontRegistry.Bundled` declares exactly one family — `Inter`, with
regular/bold/italic/bold-italic assets — and `defaultFamilyName = INTER`. A document asking for Fraunces
does not fail; `resolve()` falls back, and the page renders in Inter with no error surfaced. So the
mismatch is invisible at runtime, which is what makes it worth logging now rather than discovering it in
Phase D.

**Why this is not A3's to fix.** A3 bundles Fraunces for **chrome**, under `core/ui/src/main/res/font/`.
The render module has its own font pipeline reading `assets/fonts/` by design, because
[ADR-028](../DECISIONS.md#adr-028)'s one-engine rule means preview, export and read must all draw from
the *same* registry — adding a family there changes what every exported PDF can contain. That is a
document-model decision with an on-disk format consequence, not a theming one.

**It is also not a one-line addition.** The frozen content needs **italic** Fraunces (`:111`, `:114`,
`:123`), which chrome nowhere does — so honouring the spec means bundling a cut A3 deliberately did not, and
`DocumentFontFamily` has slots for regular/bold/italic/boldItalic but not for a 500 weight, while the
frozen content asks for 500 at two of its seven roles.

**Owner decision requested.** Three shapes, none of them implementation's to pick: (a) bundle Fraunces
into the render registry, accepting the APK cost and deciding how 500 maps onto a four-slot family; (b)
rule that the Proof's serif content is *mock illustration* of a user document rather than a spec for the
default document style, in which case nothing is owed and the Proof's own note should say so; or (c)
narrow it — e.g. Fraunces for cover titles only, where the product's voice actually shows.

**A prior note this replaces.** `DocumentFontRegistry`'s KDoc already anticipates the question — *"One
family today. That is a statement about what is bundled, not about what the registry supports"* — and
correctly routes expansion to *"the designer's font/preset curation, gated on its freeze"*. That freeze
has now happened, and it asks for a second family. The comment is still true; it is simply no longer
waiting on anything.

**✓ OWNER RULING — 2026-07-28: deferred to Phase D. Entry stays open; no work is owed before then.**

> *"Do not solve this during Phase A. Leave the current engine unchanged. This is an architectural
> decision governed by the one-engine rule ([ADR-028](../DECISIONS.md#adr-028)) and belongs to Phase D
> when the rendering/export pipeline is implemented. **No workaround. No temporary font substitution. No
> second rendering path.**"*

The three prohibitions are the operative part and are worth restating for the Phase B and C sessions that
will meet this defect before Phase D does. Each names a shortcut that would look locally reasonable:

- **No workaround** — do not paper over the silent fallback at the call site, and in particular do not
  "fix" it by making `resolve()` throw or warn. The silence is a symptom of the real decision, not the
  decision itself.
- **No temporary font substitution** — do not map Fraunces onto Inter, onto a system serif, or onto the
  chrome font resources A3 bundled. `:core:ui`'s `res/font/` and the render module's `assets/fonts/` are
  separate pipelines by design, and bridging them "just for now" is exactly how a second path starts.
- **No second rendering path** — [ADR-028](../DECISIONS.md#adr-028) requires preview, export and read to
  draw through one engine. A serif-capable path added anywhere short of the registry breaks that
  invariant, and it breaks it invisibly, since all three surfaces would still *render*.

So the correct behaviour until Phase D is that zine content continues to draw in Inter, and this entry
— not a code comment, not a TODO — is where that is recorded.

### D-005 — the Library and the Bench set the same role in two different serifs at two different weights

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 37, 125, 148, 163 · [`v2-bench.html`](mockups/v2-bench.html) lines 22, 180, 198, 236 · [`v2-proof.html`](mockups/v2-proof.html) lines 163, 210, 232 |
| **Found** | 2026-07-28, during Phase A / A3 (typography) |
| **Severity** | **Disagreement between two frozen files** — did not block A3; ruling owed before Phase B implements a Library heading |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**What they say.** The two files were frozen a day apart and declare the voice face differently.

| | Library (frozen 2026-07-27) | Bench + Proof (frozen 2026-07-28) |
|---|---|---|
| **How the serif is declared** | a literal stack at each call site — `.serif{font-family:"Iowan Old Style","Palatino Linotype",Palatino,Georgia,serif}` (`:37`) | a token — `--serif:'Fraunces',Georgia,'Times New Roman',serif` (`:22`) |
| **Named face** | Iowan Old Style / Palatino / Georgia. **Fraunces appears nowhere in the file.** | Fraunces |
| **Weight for a titled heading** | **600** — `.sh-ttl` (`:125`), 1.12rem | **500** — `.sheet h3` (`:198`, 17px), `.pgrid .pgh h3` (`:236`), `.inkpop h4` (`:180`), proof `.dhead h3` (`:163`), `.done h4` (`:232`) |

**The family half is already settled; the weight half is not.**
[V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III fixes *"Fraunces (voice) + Inter (work). Permanent."* —
which makes the Library's Iowan stack **stale text from before the `--serif` token existed**, not a
competing choice. That part needs no ruling, only a corpus cleanup.

The weight does need one. `.sh-ttl` at 600 and `.sheet h3` at 500 are the same role — a short serif
heading naming a thing, at ~17-18px — rendered two visibly different ways. And it is not the kind of
difference that survives being guessed at: the Library's headings are the first type a user ever sees,
so a Phase B session picking 500 "to match the Bench" or 600 "because the Library is frozen" is choosing
the product's opening voice by coin-flip.

Note also that the Library was authored against a *fallback* face. Iowan Old Style and Georgia are
heavier on the page than Fraunces at the same nominal weight, so 600 may have been chosen to look right
in Georgia and not because 600 is wanted in Fraunces. That is an argument for re-reading the Library at
Fraunces before ruling, rather than for reading its number literally.

**What implementation did, pending the ruling.** A3 bundles Fraunces at **400, 500 and 600** and
prejudges nothing. Whichever way the ruling lands, the face is already present and Phase B is a
per-component value, not a font-bundling change. 400 is independently required regardless (proof
`.foldcap`, `:210`, is serif body text at 14px with no weight set), so only one of the three cuts is
carried speculatively.

**Owner decision requested.** Does the Library's serif heading render at **500** (harmonising with the
Bench and Proof) or at **600** (as its own frozen file states)? And separately — a documentation
cleanup, not a design question — the Library's literal `"Iowan Old Style",Palatino,Georgia` stack should
become `var(--serif)` so a future reader is not told the product has two serifs.

**✅ RESOLUTION — owner ruling, 2026-07-28.**

> *"The Constitution is the higher authority. The canonical serif family for V2 is **Fraunces**. The
> canonical weight is **500**. The Library's earlier 600 weight reflects its original fallback stack
> (Iowan/Georgia), not a lasting design decision. When Phase B implements the Library, use **Fraunces 500
> for the shared serif role** unless a specific frozen component explicitly requires another weight."*

The ruling resolves the question by **authority rather than by arbitration**, which is why it settles
cleanly: two frozen files disagreeing is not a tie to be broken on the merits, because
[V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III already sits above both. It also confirms the reading this
entry raised on its own evidence — that 600 was chosen to look right *in Georgia*, and does not transfer
to Fraunces at the same nominal number.

**What Phase B does.** The Library's serif headings (`.sh-ttl`, `.shelf-head h1`, `.empty h2`) render in
**Fraunces 500**, matching the Bench and Proof. The escape clause is deliberately narrow: *"unless a
specific frozen component explicitly requires another weight"* means a component whose frozen CSS states
a weight for a reason of its own — not a component that merely happens to sit in the Library and
therefore inherits 600 from `.sh-ttl`. If a Phase B session believes it has found such a component, that
belief is a new register entry, not a local decision.

**What implementation does now: nothing.** A3 already bundles 400/500/600 and selects between them
nowhere, so the ruling requires **no code change** — which is what "bundling both prejudges nothing"
meant when it was claimed. The 600 cut **stays bundled** and is not now dead weight: V1's
`ZinelyFonts.Voice` is built on it and remains live until **C0** retires the V1 layer. Whether V2 chrome
still needs a 600 cut after C0 is a question for C0, not for this entry.

**Still owed to the design corpus** (documentation, not implementation): the Library's literal
`"Iowan Old Style","Palatino Linotype",Palatino,Georgia,serif` stack at `:37` should become `var(--serif)`,
and `.sh-ttl`'s `font-weight:600` at `:125` should become `500`, so that the frozen file stops stating a
family and a weight the Constitution has overruled. Until that lands, **this entry is the authority and
the Library HTML is stale on both counts** — which is precisely the situation the register exists to make
visible rather than leave to be rediscovered.

**A related note, not a defect.** The Bench and Proof both state that *"Georgia / system-sans stand in
here pending real faces at parity"* (`v2-bench.html:7`, `v2-proof.html:7`). The Library carries no such
note — but it does not need one, because it never names Fraunces at all: its Georgia is a genuine
declared fallback rather than a stand-in, which is the same fact from the other direction. Either way
**none of the three prototypes renders the shipping typefaces**, so a browser screenshot of them is not
a type-parity target — only their declared families, sizes, weights and line-heights are. A9's Roborazzi
baselines and any Phase B parity pass must compare against the CSS, not against a rendering of it.

**Where else the split shows.** The disagreement is not confined to sheet headings. The Library's zine
cover title (`.ct`, `:68`, taking its family from `.serif` at `:37` via the markup at `:150`) is serif at
**600**; the Proof's cover title (`.cover h2`, `:110`) is serif at **500**. Those two are not strictly the
same object — one is a shelf card drawn by chrome, the other is mock zine content drawn by the render
engine (D-004) — so this is listed as corroboration that 500-vs-600 is a systematic split across the two
freeze dates, not as a fourth independent defect.

### D-006 — the only shape token in V2 is declared and never used

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) line 24 · [`v2-proof.html`](mockups/v2-proof.html) line 24 |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | Dead specification — **does not block A4** |
| **Status** | Open |

**What it says.** Both files declare `--r:18px` in `:root`, alongside `--serif`, `--sans`, `--settle`
and `--standard`. The Library declares no `--r` at all.

**Why it is wrong.** `--r` is referenced **zero times** in either file, and **no `border-radius` of
18px exists anywhere in V2** — not through the token and not as a literal. Every one of the sixteen
distinct chrome radii is written out at its use site: `50%`, 22, 20, 16, 14, 13, 12, 11, 10, 9, 8, 6,
5, 4, 3 and 2px, plus five asymmetric values such as the Library cover's `6px 9px 9px 6px`, whose
tighter left corners read as the spine of a printed thing. The three files do not even agree with one
another: the Library's bottom sheet is 20px where the Bench's is 22px, for the same kind of object.

So `--r` is a leftover from a revision in which a shared corner radius existed and was then designed
away, one component at a time, without the token being deleted.

**Does implementation depend on it?** No, and A4 deliberately did **not** port it. A token naming a
value that nothing uses is worse than no token: it invites a Phase B session to "restore consistency"
by applying an 18px radius the design never had, and it would pass review, because a token in the
foundation looks like an intention. `ZinelyV2Dimens` therefore carries no radius at all, and the
absence is documented at the code with a pointer here.

**Owner decision requested** (low stakes, but it should be answered rather than left): delete `--r`
from both `:root` blocks as dead, **or** state which components it was meant to govern — in which case
it is not dead and Phase B needs to know which ones.

### D-007 — the constitutional 8pt rhythm is not observable in the frozen CSS

| | |
|---|---|
| **Artifacts** | [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III *Spacing* · [V2-RESEARCH.md](V2-RESEARCH.md) §2.4, §3.9 · [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) Phase A deliverables · all three frozen mockups |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | **Conflict with the highest authority in the corpus** — held A4's spacing deliverable; would have blocked Phase B |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**What the governing documents say.** [V2-CONSTITUTION.md](V2-CONSTITUTION.md) §III, under the
invariants that are *"fixed for the life of V2"*:

> *"An **8pt rhythm** governs layout. Spacing is calm and generous; the page is given room."*

[V2-RESEARCH.md](V2-RESEARCH.md) §2.4 defines it concretely — *"Adopt an **8pt spacing scale**
(4/8/16/24/32/48…), tokenized"* — and §3.9 asks for *"one tokenized 8pt `space.*` scale for all
margin/padding/gap"*. [COMPOSE-V2-ROADMAP.md](../COMPOSE-V2-ROADMAP.md) lists A4's deliverable as
*"the 8pt rhythm and the calm elevation model as reusable primitives"*.

**What the frozen CSS does.** Every `padding`, `margin` and `gap` value across the three files:

| | multiples of 8 | multiples of 4 | neither |
|---|---|---|---|
| Chrome only (hand-classified, N=204) | **16.7%** | 38.2% | 61.8% |
| All CSS incl. scaffolding (N=252) | **17.1%** | 37.3% | 62.7% |

The two measurements were taken independently and agree, so the finding does not depend on where the
chrome/scaffolding line is drawn. The most frequent values are **12** (25), **8** (23) and **2** (19),
followed by a continuous tail of 6, 7, 9, 10, 13 and 14 that no 4pt reading accommodates — 2, 6, 7, 9,
13 and 14px together account for more chrome spacing than every multiple of 8 combined. A distribution
with no gaps between 1 and 20px is the signature of hand-tuned optical spacing, not a stepped scale.

**Why this is not the same call A3 made.** A3 declined to publish a *type* scale on the same kind of
evidence, and that was safe: no document mandates one, so "there is no scale" contradicted nothing.
Here the invariant is stated by the **Constitution**, which outranks the frozen HTML. Both available
readings are therefore decisions, and both are expensive:

- **Transcribe the frozen literals.** Pixel-parity passes. The constitutional invariant is violated on
  every screen, and the foundation ships a "spacing scale" that is 62% off-grid.
- **Snap to 8pt.** The invariant holds. Roughly three in five spacing values in the frozen trilogy
  change, which is a **visual redesign of three DESIGN-FROZEN surfaces** — an owner amendment under
  [COMPOSE-IMPLEMENTATION-GUIDE.md §4](../COMPOSE-IMPLEMENTATION-GUIDE.md), and it would guarantee
  Phase B fails its own pixel-parity gate against the HTML it is implementing.

**The freeze appears to have anticipated this.** `v2-library.html:8`, *inside* the DESIGN FROZEN
banner, lists **"8pt rhythm"** among the *implementation-time gates (P3)* — alongside AA contrast per
ink and screen-reader paths. So the frozen file does not claim to satisfy the rhythm; it defers it to
implementation. That is a strong hint that the intended answer is "snap at implementation time" — but
it is a hint, and acting on it would silently redesign three frozen surfaces, which is precisely the
act the implementation rules reserve for the owner.

**What implementation did, pending the ruling.** **Nothing.** `ZinelyV2Dimens` publishes no spacing
value at all. This is not the A3 pattern of shipping a minimum defensible gate — here there is no
minimum: any scale published now *is* the answer, since Phase B would build against it. The rest of
A4 (the hairline, the focus stroke, the shadow primitive, the radius finding) does not depend on the
ruling and shipped normally.

**Owner decision requested.** Does V2 spacing (a) transcribe the frozen literals, accepting that the
constitutional 8pt invariant describes an aspiration the frozen surfaces do not meet; (b) snap to the
4/8/16/24/32/48 scale at implementation time, accepting that this amends three frozen surfaces and
that Phase B's parity gate must then compare against an amended HTML rather than today's; or (c)
something narrower — e.g. 8pt governs *layout* spacing (page margins, section gaps, the rhythm a user
perceives) while component-internal padding stays as frozen, which would reconcile the Constitution's
own wording (*"governs **layout**"*) with the measurement, since the off-grid values are concentrated
in component internals?

*(Recorded for the record, not as a resolution: (c) is the reading that requires the fewest documents
to be wrong. It is not adopted, because "which values are layout and which are component-internal" is
itself a design judgement across three frozen surfaces.)*

**✅ RESOLUTION — owner ruling, 2026-07-28. Close to option (c), and cleaner than the question assumed.**

> *"**Do not publish a V2 spacing scale.** The Constitution expresses an implementation **aspiration**,
> not a token inventory. The frozen HTML remains the canonical authority. Until the design corpus
> explicitly defines a shared spacing scale, spacing continues to live at the component level exactly
> as frozen. Macro layout rhythm should continue to follow the constitutional guidance where
> appropriate, but **no global spacing token set should be introduced**."*

The ruling dissolves the conflict rather than picking a side, by reading the two documents as
different kinds of statement: §III is an **aspiration about layout**, not an inventory of tokens, so a
frozen surface whose component padding is 13px is not in violation of it. That reading was available
in the Constitution's own wording — *"an 8pt rhythm governs **layout**"* — and this entry reached for
it as option (c) but declined to adopt it, because drawing the layout/component line across three
frozen surfaces is itself a design judgement. The ruling makes the line unnecessary: **nothing is
tokenised**, so there is no boundary to draw and no token for a component to route around.

**What implementation does: nothing, and now permanently.** `ZinelyV2Dimens` publishes no spacing
value, which is already its state — A4's held item is now its finished one. Phase B onward transcribes
each component's frozen `padding` / `margin` / `gap` at its call site, exactly as
[ADR-073](../DECISIONS.md#adr-073) established for type and [ADR-074](../DECISIONS.md#adr-074) for
radius. The V2 foundation therefore carries **no scale of any kind** — not type, not radius, not
spacing — and that consistency is the finding rather than an accident: the frozen trilogy is a
hand-tuned optical design, and the foundation's job is to hold what is genuinely shared, not to impose
a system the design does not have.

**What would reopen it.** Only the design corpus explicitly defining a shared spacing scale — the
ruling's own condition. The companion test does **not** guard this: a "literals stand" ruling needs no
corpus change, so `ZinelyV2DimensTest` stays green either way, and that limitation is stated at the
test. This entry is the record.

### D-008 — two of the three frozen surfaces specify no focus appearance, and one removes it

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 54, 77, 95, 137 · [`v2-bench.html`](mockups/v2-bench.html) lines 99, 209 · [`v2-proof.html`](mockups/v2-proof.html) — no focus rule anywhere |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | **Accessibility gap in the frozen specification** — does not block A4 |
| **Status** | **Open by owner ruling** (2026-07-28) — approach settled; stays open until Phase C implements and verifies the affected surfaces |

**What exists.** The Library specifies focus on three product controls (and one prototype-only control), all at **2px**:

| Control | Line | Rule |
|---|---|---|
| `.zine:focus-visible` | 54 | `outline:2px solid var(--matcha-text); outline-offset:6px; border-radius:9px` |
| `.more:focus-visible` | 77 | `outline:2px solid currentColor; outline-offset:0` |
| `.start:focus-visible` | 95 | `outline:2px solid var(--ink); outline-offset:3px` |
| `.ctl:focus-visible` | 137 | `outline:2px solid var(--matcha-text); outline-offset:2px` — **prototype-only control**, not product UI; listed for completeness because it is the fourth and last focus rule in the corpus |

**What does not exist.** The **Bench and the Proof contain no `:focus`, `:focus-within` or
`:focus-visible` rule at all** — between them roughly two dozen interactive controls, including every
control in the editor. The Proof alone has some fourteen `<button>` elements with no specified focus
appearance.

**Worse than an omission, in one place.** The Bench sets `outline:none` on `.el` (`:99`) and
`.search input` (`:209`). `.el` is not decorative: it carries `tabindex="0"`, `role="button"` and an
Enter/Space `keydown` handler (`:503-504`), so it is a deliberately keyboard-operable control whose
focus indicator is deliberately removed, with nothing put in its place. A keyboard or switch-access
user can move focus through the Bench's elements and see nothing at all.

**Why this is the register's and not a code review's.** The frozen HTML is the specification. An
implementer who invents a focus ring for the Bench is designing a visual treatment for the product's
most complex surface — colour, offset, radius and how it reads against a user's own artwork — which
is a design act, not a parity act. An implementer who faithfully reproduces `outline:none` ships an
accessibility defect knowingly. Neither is implementation's call.

**Does implementation depend on it?** Not in A4 — `ZinelyV2Dimens.FocusRingWidth` carries the 2px all
four rules agree on, and no offset, since the Library's three product rules use three different
offsets (6px, 3px, 0). It binds in **Phase C**, when the Bench's controls are built.

**Owner decision requested.** What is the focus appearance for the Bench and the Proof? The narrowest
answer that resolves it is to extend the Library's treatment (2px `matchaText`, per-component offset)
across all three surfaces and delete the two `outline:none` rules — but that is a change to a frozen
surface and so belongs to the owner.

**✓ OWNER RULING — 2026-07-28: a platform responsibility, not a redesign. Entry stays open until Phase C.**

> *"These are **platform responsibilities, not redesign opportunities**. Implement accessible focus
> indicators … during Phase B and C in a manner that is **visually subordinate to the frozen design**.
> **Do not modify the visual design solely to satisfy these requirements.** Where the platform requires
> accessibility behaviour not represented in the HTML, the implementation guide already authorises
> those additions. Keep these defects open until the affected product surfaces are implemented and
> verified."*

This settles the question the entry could not: whether inventing a focus ring for the Bench is a design
act. It is not — it is a **platform obligation the HTML does not model**, in the same category as
TalkBack ordering or a touch-exploration path, and
[COMPOSE-IMPLEMENTATION-GUIDE.md](../COMPOSE-IMPLEMENTATION-GUIDE.md) already sanctions additions of
that kind. What the ruling forbids is the *other* move: reaching for a visual change — recolouring,
resizing or re-spacing a control — in order to make focus read well. **The indicator adapts to the
design; the design does not adapt to the indicator.**

**Consequence for Phase C.** The Bench's `outline:none` on `.el` and `.search input` is **not**
transcribed: reproducing it would ship a control that is keyboard-operable and invisibly focused. The
Library's own treatment (2px, `matchaText`, per-component offset) is the reference for what
"subordinate" means here, since it is the only focus appearance the design ever authored.

**Why it stays open.** The ruling settles the *approach*, not the outcome. The entry closes when Phase C
has implemented focus on the Bench and Proof **and** both device-verification passes have confirmed it
against the platform accessibility tree — the only place a focus indicator's real behaviour can be
read.

### D-009 — no control in the frozen trilogy declares a minimum touch target, and most measure well under 48dp

| | |
|---|---|
| **Artifacts** | all three frozen mockups (control sizing throughout) · [V2-RESEARCH.md](V2-RESEARCH.md) §2.4 |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | **Accessibility gap in the frozen specification** — does not block A4 |
| **Status** | **Open by owner ruling** (2026-07-28) — approach settled; stays open until Phase B/C implements and verifies the affected surfaces |

**Measured.** Not one selector in any of the three files declares `min-height` or `min-width` on an
interactive control. Every `min-*` in the trilogy is either `min-height:100vh` on `body`, a
`min-width:0` flexbox overflow fix, or the Proof's `min-height:2.6em` reserving two lines of fold
caption. Control sizes are set with explicit `width`/`height` or fall out of padding, and they land
mostly below the 48dp floor:

| Surface | Control | Declared box |
|---|---|---|
| Bench | `.mat-item` / `.icon-btn` / `.add` | 46×46 · 44×44 · h44 |
| Bench | `.gridbtn` · `.styletb .chip` · `.chip2` | 34×34 · h34 · h32 |
| Bench | `.pthumb` · `.sw2` · `.toggle` | 26×34 · 26×26 · 38×22 |
| Bench | `.tray .fold` | ≈23×19 — the smallest control in V2 |
| Proof | `.fnav` · `.iconbtn` · `.dclose` | 40×40 · 38×38 · 30×30 |
| Library | `.more` | 34×34 |
| Library | `.start` · `.sheet .act` | ≈49 · ≈50 — the only controls that clear the floor by construction |

**Why this is one finding with D-007 and still needs its own answer.** [V2-RESEARCH.md](V2-RESEARCH.md)
§2.4 predicts exactly this coupling — *"generous 8pt spacing gives 48dp targets for free"*. The
frozen surfaces did not take the 8pt spacing, and did not get the targets. So the two defects share a
cause. They do **not** share a fix: if the owner rules under D-007 that the frozen literals stand, the
targets are still below the floor and still need an answer, so this is logged separately rather than
folded in.

**Why implementation must not quietly fix it.** The Android floor is 48dp and is not negotiable, but
the two ways to reach it are both design changes: grow the controls (which reflows three frozen
layouts — the Bench's filmstrip and swatch grid could not hold their current counts at 48dp), or keep
the visual size and extend the *touch* area beyond the drawn bounds. The second is invisible in a
screenshot and is very likely the intended answer, but it is a decision about overlapping hit regions
in a dense editor, and getting it wrong produces controls that steal each other's taps.

**Does implementation depend on it?** Not for the *fix*, but A4 does now publish the floor:
`ZinelyV2Dimens.MinTouchTarget = 48.dp`. An earlier draft withheld it on the grounds that the V2 spec
states no minimum and asserting one would be implementation inventing a design value. The independent
review falsified that, and correctly: **48dp is a platform floor, not a design value** — V1's
`ZinelyDimens` says so in as many words — and the V2 spec's silence is not a contradiction of it. It
also prejudges nothing, because *both* answers below presuppose the number; withholding it would only
have meant Phase B building 26×26 controls with nothing in the foundation naming what they must clear.
The defect is the gap between that floor and the frozen sizes, and it binds in **Phase B**.

**Owner decision requested.** Do the frozen control sizes stand with the touch area extended beyond
the drawn bounds to 48dp, or do the controls themselves grow (and the layouts that hold them change)?

**✓ OWNER RULING — 2026-07-28: as D-008 — platform responsibility, visually subordinate.**

> *"… Implement … minimum touch targets during Phase B and C in a manner that is **visually subordinate
> to the frozen design**. **Do not modify the visual design solely to satisfy these requirements.**"*

Read against this entry's two options the ruling is decisive: *"do not modify the visual design"* rules
out **growing the controls**, so the floor is reached by **extending the touch area beyond the drawn
bounds** — the invisible option. The Bench's filmstrip and swatch grid keep their frozen 26×34 and
26×26 appearance and gain 48dp of reachable area around it.

**The consequence Phase B and C must design for.** Extended hit regions in a dense editor **overlap**,
and the frozen Bench places controls closer together than 48dp in several places — the swatch grid and
filmstrip most obviously. Overlap is resolved by hit-region priority and proximity, **not** by shrinking
a region back below the floor. Getting it wrong produces controls that steal each other's taps: a defect
a screenshot cannot show and a semantics test will not catch, which makes it precisely a device-pass
finding.

**Why it stays open.** `ZinelyV2Dimens.MinTouchTarget = 48.dp` states the floor; nothing meets it yet,
because no control exists. The entry closes when the affected surfaces are implemented **and** verified
on-device against the platform accessibility tree.

### D-010 — the page shadow is hard-coded to the light theme and does not adapt in the dark

| | |
|---|---|
| **Artifacts** | [`v2-bench.html`](mockups/v2-bench.html) line 85 · [`v2-proof.html`](mockups/v2-proof.html) line 98 |
| **Found** | 2026-07-28, during Phase A / A4 (shape, spacing, elevation) |
| **Severity** | Theme defect in the frozen specification — **does not block A4** |
| **Status** | Open |

**What it says.** Every other shadow in the Bench and the Proof takes its colour from
`var(--frame-shadow)`, which is re-derived for dark (`rgba(58,48,32,.28)` → `rgba(0,0,0,.5)`). Two do
not:

- `v2-bench.html:85` — `.page` — `0 14px 30px -14px rgba(58,48,32,.4), 0 2px 5px rgba(58,48,32,.14)`
- `v2-proof.html:98` — `.zpage` — `0 16px 34px -16px rgba(58,48,32,.44), 0 2px 5px rgba(58,48,32,.14)`

`rgb(58,48,32)` is the **light-theme** value of `--frame-shadow`, spelled out rather than referenced.

**Why it is wrong.** The dark-theme blocks in all three files change colour tokens only — so these two
rules keep a warm-brown shadow on a dark desk, while every neighbouring surface switches to black.
`.page` and `.zpage` are the *zine itself*: the single most important object on both screens. This is
also the one place the two-layer cast-plus-contact composition the Library expresses with two tokens
(`--shadow` + `--contact`) is reproduced by hand, which is plausibly how the tokens were lost.

Note this is a **theme** defect, not a value defect: the light-theme rendering is correct and the
prototypes are usually read in light, which is why it survived the freeze.

**Does implementation depend on it?** Not in A4 — no shadow values are transcribed yet; A4 ships only
the [`ZinelyV2ShadowLayer`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Shadow.kt)
primitive. It binds in **Phase C** and **Phase D**, when the page and the proof page are drawn. Flagged
now because a faithful transcription would carry the bug into Compose and it would then be invisible:
the Compose page would look right in light and subtly wrong in dark, which is the failure mode nobody
screenshots.

**Owner decision requested** (or simply a corpus fix): should both rules use `var(--frame-shadow)`,
and if the two-layer composition is wanted in dark as well, does the Bench/Proof palette need a
`--contact` equivalent the way the Library has one?

**✓ OWNER RULING — 2026-07-28: deferred to Phase C. A product-surface concern, not a foundation one.**

> *"Leave unchanged until Phase C. This is a **product-surface concern, not a foundation concern**. **Do
> not create alternative shadow behaviour during Phase A.**"*

The prohibition is the operative part. The tempting Phase A move would be to add a "correct" page-shadow
definition to the foundation — a `pageShadow` token, or a dark variant of `frameShadow` — so Phase C
inherits the fix for free. That is exactly *alternative shadow behaviour*, and it would put a second,
unfrozen source of truth beside the HTML for the most important object on two screens. A4 therefore
ships only the [`ZinelyV2ShadowLayer`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Shadow.kt)
primitive, with no page shadow defined anywhere.

Phase C draws the page and decides there, with this entry in hand — the point of logging it now being
that a faithful transcription carries the bug into Compose, where it becomes invisible: correct in
light, subtly wrong in dark, which is the failure mode nobody screenshots.
### D-011 — the Library declares neither easing token and animates on a curve found nowhere else

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) lines 36, 43, 52, 61, 93, 119, 122 · [`v2-bench.html`](mockups/v2-bench.html) line 24 · [`v2-proof.html`](mockups/v2-proof.html) line 24 |
| **Found** | 2026-07-28, during Phase A / A5 (motion) |
| **Severity** | Cross-file divergence — did not block A5 |
| **Status** | ✅ **RESOLVED** 2026-07-28 by owner ruling — see the resolution at the end of this entry |

**What they say.** The Bench and Proof declare two easing tokens and use them throughout:
`--settle:cubic-bezier(.05,.7,.1,1)` (nine uses) and `--standard:cubic-bezier(.2,0,0,1)` (eight).

The Library declares **neither**. Its **seven** transitions — five on product chrome, two on
prototype scaffolding — use four different things instead:

| Line | Selector | Easing |
|---|---|---|
| 122 | `.sheet` — the action sheet's slide | `cubic-bezier(.2,.8,.2,1)` — **a curve that appears nowhere else in V2** |
| 52 | `.zine` — the card press | `ease` |
| 61 | `.cover` — the shadow response | `ease` |
| 93 | `.start` — the primary button press | *unspecified* → the CSS default, `ease` |
| 119 | `.scrim` | *unspecified* → `ease` |
| 36, 43 | `body`, `.phone` — **prototype scaffolding**, listed so the inventory is complete | *unspecified* → `ease` |

**Why it is wrong.** This is the same staleness as **D-005** (the serif), from the same cause: the
Library was **authored** a day earlier (`1b2e244`, 2026-07-27) and **frozen at 09:08 on 2026-07-28**,
four and three-quarter hours before the Bench (`4494e95`, 13:52) — so it was written before the shared
token layer existed and the freeze captured it mid-evolution. `cubic-bezier(.2,.8,.2,1)` is not a third design intention — it is what `--standard`
was on its way to becoming. The bare `ease` keyword is the browser default rather than a choice at all,
and it is the one curve the V2 system explicitly moved away from: `ease` is symmetric and slightly
back-loaded, which is exactly the "generic UI" feel the settle/standard pair was chosen to avoid.

**Does implementation depend on it?** Not in A5 — only the two tokenised easings are ported, and the
Library's literals are not (porting `cubic-bezier(.2,.8,.2,1)` into the foundation would give a stale
value the same standing as a frozen token, the mistake **D-006** avoided with `--r`). It binds in
**Phase B**, which cannot animate the Library's sheet without choosing a curve.

**Owner decision requested.** Does the Library's action sheet use **`--settle`** (it is a surface
coming to rest, which is what settle is for, and it is what the Bench's and Proof's equivalent sheets
use), and its remaining transitions **`--standard`**? That is the reading that makes the trilogy one
system; the alternative is that the Library's curves are deliberate and the corpus has three easings,
in which case the third needs a name and a token. Separately — a documentation cleanup, as with D-005
— the Library's `:root` should gain `--settle`/`--standard` so a future reader is not told the product
has two motion systems.

**✅ RESOLUTION — owner ruling, 2026-07-28.**

> *"The Bench and Proof establish the **canonical V2 motion language**. The Library's unique easing
> reflects its **earlier freeze state** rather than a lasting design decision. When Phase B implements
> Library motion, use the **canonical V2 easing tokens** defined by Bench and Proof."*

The same shape as the **D-005** ruling, and for the same reason: where the Library and the later-frozen
pair disagree, the disagreement is chronology rather than intent. `cubic-bezier(.2,.8,.2,1)` was
`--standard` on its way to becoming itself, and the bare `ease` keywords were never a choice at all —
`ease` is the CSS default.

**What Phase B does.** Library motion uses `ZinelyV2Settle` and `ZinelyV2Standard`. Applying the
**paper-versus-chrome** axis that [ADR-075](../DECISIONS.md#adr-075) Decision 1 established from the
Bench and Proof — which is now the canonical language, and so is the thing to apply — that reads as:

| Library transition | Curve | Why |
|---|---|---|
| `.sheet` (`:122`) — the action sheet sliding up | **settle** | a surface coming to rest; the Bench's and Proof's equivalent sheets both use settle |
| `.cover` (`:61`) — the cover's shadow response | **settle** | the cover is the paper object itself |
| `.zine` (`:52`) — the card press | **settle** | the frozen Bench uses settle for `.pthumb`, the same gesture on the same kind of object |
| `.start` (`:93`) — the primary button press | **standard** | chrome mechanism |
| `.scrim` (`:119`) — the scrim fade | **standard** | pure opacity, and the Bench and Proof both use standard for their scrims |

*(That table is the reading Phase B should start from, not an amendment: the ruling settles the
**token set**, and the per-component assignment is transcription against the canonical axis. Any
component where the axis genuinely does not decide is a new register entry, not a local judgement.)*

**What implementation does now: nothing.** A5 deliberately did not port the Library's literals
([ADR-075](../DECISIONS.md#adr-075) Decision 4, on the **D-006** precedent), so no stale curve entered
the foundation and no code changes. Durations are unaffected — the ruling settles easings, and the
Library's `.16s`, `.24s`, `.4s` and the rest remain per-component values transcribed as frozen.

**Still owed to the design corpus** (documentation, not implementation): the Library's `:root` should
gain `--settle` and `--standard`, `:122` should reference `var(--settle)` instead of
`cubic-bezier(.2,.8,.2,1)`, and the bare/absent easings at `:52`, `:61`, `:93` and `:119` should name a
token. Until that lands, **this entry is the authority and the Library HTML is stale**.

### D-012 — the three frozen files write three different reduced-motion rules, and one of them would strobe

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) line 138 · [`v2-bench.html`](mockups/v2-bench.html) lines 110, 260 · [`v2-proof.html`](mockups/v2-proof.html) lines 245-247 |
| **Found** | 2026-07-28, during Phase A / A5 (motion) |
| **Severity** | **Accessibility inconsistency in the frozen specification** — does not block A5 |
| **Status** | **Open by owner ruling** (2026-07-28) — deliberately unresolved in Phase A; the behavioural decision belongs to **Phase C**, on physical devices |

**What they say.** Every file honours `prefers-reduced-motion`, which is the good news and is worth
stating plainly — this is not a missing-accessibility defect like **D-008**. But no two files honour it
the same way:

| File | Rule |
|---|---|
| `v2-library.html:138` | `*{transition:none!important}` — kills transitions; says nothing about animations (the Library has none) |
| `v2-bench.html:260` | `*{transition-duration:.01ms!important; animation:none!important}` — collapses transitions, **disables animations outright** |
| `v2-proof.html:246` | `*{transition-duration:.01ms!important; animation-duration:.01ms!important}` — collapses both |

**Why it matters, given they currently agree.** For the three animations that exist today — the Bench's
one-shot `mat` materialise, the Proof's one-shot `seal`, and nothing in the Library — all three rules
produce an acceptable result, which is how the divergence survived the freeze. They are still not
interchangeable, and the difference is not stylistic:

The Bench contains the trilogy's only **looping** animation — the text caret's
`animation:blink 1.05s steps(1) infinite` (`:110`). Collapsing an *infinite* animation's duration to
`.01ms`, as the Proof's rule does, does not calm it: it makes it repeat at ten thousand hertz. The
Bench's `animation:none` is the correct form, and it is correct precisely because the Bench is the file
that has a loop. So the two files each wrote the rule that suited what they contained, and the Proof's
rule is safe **only** because the Proof has no looping animation — a property of today's content, not
of the rule.

Since `prefers-reduced-motion` is in part a **photosensitivity** setting, "the rule that happens to be
safe for the current content" is not a standard worth carrying into a codebase where a shimmer, a
pulse or a progress indicator may be added later.

**The fact that makes this a real decision, not a tidy-up.** The three files were frozen in this order:

| File | Freeze commit | Frozen at | Rule |
|---|---|---|---|
| `v2-library.html` | `43a3cc9` | 2026-07-28 **09:08** | `transition:none` |
| `v2-bench.html` | `4494e95` | 2026-07-28 **13:52** | `animation:none` |
| `v2-proof.html` | `caf431c` | 2026-07-28 **15:53** | `animation-duration:.01ms` |

So the corpus's **most recent** statement is the Proof's — the corpus moved *away* from
`animation:none`, not toward it. That is very likely an oversight in a file that has no looping
animation to worry about rather than a considered reversal, but it is exactly the fact an owner needs
in order to rule, and it is the reason this entry cannot claim the matter is merely technical.

**What implementation did, and it is a choice.** A5 implements the **Bench's** reading — the older of
the two live statements — and makes the distinction explicit rather than implicit:
[`ZinelyV2Motion`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Motion.kt) collapses
one-shot durations to zero (*"arrive instantly, still arrive"*) via `durationMillis`, and exposes
`allowsContinuousMotion`, which is **false** under reduced motion, so a looping animation is gated off
entirely rather than run at zero duration.

An earlier draft of this entry claimed *"this is not implementation choosing between two design
options"*. That was wrong and is withdrawn: three written rules that are not equivalent, one of them the
most recent, is precisely a choice. What justifies making it here is not that no choice exists but that
one option is a **safety floor**: running an `infiniteRepeatable` at zero duration in Compose is not a
slow animation, it is an unbounded frame-rate loop, and `prefers-reduced-motion` is in part a
photosensitivity setting. Refusing to ship that should not require a ruling. The choice is also **free
to reverse**: the API is additive and has no callers, so a ruling either way costs one line.

**✓ OWNER RULING — 2026-07-28: stays open. The behavioural decision belongs to Phase C.**

> *"Leave open. Do not resolve this during Phase A. The implementation API is intentionally flexible
> enough to support whichever policy ultimately governs. The final behavioural decision belongs to
> **Phase C**, where motion can be evaluated on **physical devices against the implemented product**
> rather than isolated prototypes. **Do not hard-code assumptions before then.**"*

The ruling is right that the API's *shape* is policy-neutral, and it is worth being exact about why,
since "flexible enough" is a claim implementation should be able to evidence rather than accept:

- **`durationMillis(frozen)`** takes the component's own value and returns it or zero. Every candidate
  policy is expressible by changing what it returns; no call site changes.
- **`allowsContinuousMotion`** is a **question a call site asks**, not a policy it states. A call site
  written as `if (motion.allowsContinuousMotion) { /* infiniteRepeatable */ }` is correct under all
  three rules — only the answer moves.

So what is currently Bench-flavoured is the **value**, not the shape, and Phase C changes the value in
one place. Recorded plainly because *"the API is flexible"* is exactly the kind of claim that is assumed
true and turns out not to be at the moment someone relies on it.

**The one thing Phase C must not inherit unexamined.** The distinction itself — one-shot collapses,
continuous stops — is **platform behaviour rather than design policy**, and the owner approved it on
that basis at the A5 gate. What remains genuinely open is the narrower question the three files
disagree on: whether a *transition* is cancelled mid-flight (the Library's `transition:none`) or
completes instantly (the Bench's and Proof's `.01ms`), and which rule the corpus should state. Only the
latter guarantees an element still reaches its end state, which is why implementation reads `.01ms` as
zero rather than as "do not run" — but that too is a Phase C observation to confirm on a device, not a
Phase A conclusion.

**Why a device is the right venue.** Reduced motion is the one part of the motion system whose
correctness cannot be read off a prototype: the failure modes are a control that never arrives at its
end state, and a repeating animation that strobes. Neither is visible in a browser at rest, and neither
appears in a screenshot.

**Owner decision requested** (a corpus cleanup, not a design question): should all three `:root` blocks
carry the Bench's rule — `transition-duration:.01ms; animation:none` — so the prototypes state one
policy? Note that the Library's `transition:none` and the Bench's `.01ms` also differ in a subtler way:
`transition:none` cancels a transition mid-flight, while a `.01ms` duration lets it complete instantly,
and only the latter guarantees the element still reaches its end state.
---

### D-013 — the Library and the Bench bake different alpha into the same grain, so a cover shows it four to nearly seven times stronger

| | |
|---|---|
| **Artifacts** | [`v2-library.html`](mockups/v2-library.html) line 18 · [`v2-bench.html`](mockups/v2-bench.html) line 25 · [`v2-proof.html`](mockups/v2-proof.html) line 25 |
| **Found** | 2026-07-28, during Phase A / A6 (paper / grain) |
| **Severity** | Cross-file divergence in a **material** — did not block A6 |
| **Status** | ✅ **RESOLVED** 2026-07-29 by owner ruling — see the resolution at the end of this entry |

**The noise itself is identical in all three files** — same filter, same parameters, same tile:

```
<feTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/>
<feColorMatrix type='saturate' values='0'/>
```

on 140×140. What differs is one attribute on the `<rect>` the filter is applied to. The Bench and Proof
write `opacity='.5'`; the Library writes none. That alpha is **baked into the data URI**, so it
multiplies with the element's CSS `opacity` rather than replacing it, and only the product is
observable:

| Surface | Tile | Baked | CSS | **Effective** |
|---|---|---|---|---|
| Library `.cover` | 140px | 1.0 | — | **1.00** |
| Library `.sheet-ill` | 90px | 1.0 | — | **1.00** |
| Library `.book-ill` | 70px | 1.0 | — | **1.00** |
| Bench / Proof `body::before` (the desk) | 180px | .5 | .5 | **0.25** |
| Bench `.page::after` | 120px | .5 | .45 | **0.225** |
| Proof `.zpage::after` | 120px | .5 | .42 | **0.21** |
| Proof `.drawer::after` | 150px | .5 | .3 | **0.15** |
| Bench / Proof `.phone::after` | 150px | .5 | .35 | 0.175 — *prototype bezel, not product UI* |

Ten grain-drawing rules, and every one of them blends `soft-light`. The gap is **4× against the
desk and 6.7× against the Proof's drawer** — an earlier draft of this entry said "four to five",
which understated its own table.

**Why this is a question rather than obviously a bug.** Two readings both survive the evidence, and
implementation cannot choose between them from the corpus alone:

1. **It is deliberate.** The Library's grain lands on *saturated cover inks*; the Bench's and Proof's
   lands on near-white paper. `soft-light` is dramatically subtler over mid-tone colour than over a
   near-white ground — the same alpha genuinely does not produce the same perceived texture. On this
   reading the Library is compensating, and 1.00 over a cover may look like 0.25 over paper.
2. **It is drift.** The Library froze first, before the `opacity='.5'` convention existed; the Bench
   and Proof then established it, and the Library's `--grain` was never revisited. This is the same
   shape as the **D-005** and **D-011** rulings, both of which resolved *"the Library reflects its
   earlier freeze state."*

Reading 2 has precedent on its side. Reading 1 has physics on its side. The difference matters: at
1.00, soft-light noise over a mid-tone cover is a visible tooth; at 0.25 it is a suggestion. Getting
this wrong makes covers either flat or dirty, and it is not the kind of thing a code review catches.

**Owner decision requested.** Does a Library cover draw grain at its frozen effective **1.00**, or at
the Bench/Proof register (≈0.2–0.25)? If the answer is 1.00, the Library's `--grain` should gain the
same `opacity='.5'` as the others and the CSS opacities should carry the difference explicitly, so the
corpus states one material with per-surface strengths rather than two materials that happen to share a
filter.

**What implementation does now: nothing.** [`ZinelyV2Grain`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Grain.kt)
ships one tile at full strength and takes `alpha` per call site, exactly as
[ADR-076](../DECISIONS.md#adr-076) records. Every value in the table above is expressible without a
code change, so this ruling costs one argument at one call site whenever it lands. **No strength is
tokenised and no average is taken** — under the **D-007** ruling, strength stays at the component.

**✅ RESOLUTION — owner ruling, 2026-07-29: reading 1. The divergence is deliberate.**

> *"The frozen HTML is authoritative. **Do not normalize grain strength across materials.** Paper and
> printed covers are intentionally different physical materials, and their grain should remain exactly
> as frozen."*

The first ruling in this register to break the pattern the Library's other divergences established —
and the reason is instructive. **D-005** (serif weight) and **D-011** (easing) were both resolved as
*chronology*: the Library froze first, so where it disagrees with the later pair it is stale. The
obvious move was to apply that a third time. The ruling declines to, on the grounds that this
divergence is not a token that failed to get updated but a **statement about two materials**. Paper and
a printed cover take ink differently; a value that is identical across them would be the drift, not the
difference.

So the table above is not a defect table. It is the specification, and Phase B transcribes it
literally: `1.00` on the three Library surfaces, `0.15`–`0.25` on the Bench's and Proof's. The 4×–6.7×
gap is the design.

**What implementation does: nothing, and that was already true.**
[`ZinelyV2Grain`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Grain.kt) ships one
tile at full strength and takes `alpha` per call site, so every row above is already expressible. No
code change, no asset change, and — under the **D-007** ruling — still no strength token.

**One consequence worth naming for Phase B.** Because the ruling makes the Library's `--grain`
correct rather than stale, the tripwire in `ZinelyV2GrainTest` (which fires if the Library's rect ever
gains `opacity='.5'`) is now guarding the *right* behaviour rather than waiting for a fix: if that
assertion ever breaks, it means someone has normalised the materials, which this ruling forbids. Its
message should be read that way. **The frozen corpus is owed no cleanup here** — unlike D-005 and
D-011, nothing in the HTML is stale.

---

### D-014 — the paper material cannot be drawn at all on API 24–28, and the design has no reading for those devices

| | |
|---|---|
| **Artifacts** | Platform constraint against the whole frozen trilogy — every `--grain` rule (`v2-library.html` 59/105/110 · `v2-bench.html` 61/74/86 · `v2-proof.html` 61/81/101/159) |
| **Found** | 2026-07-29, during Phase A / A6 review |
| **Severity** | **Platform capability gap**, not a specification contradiction — did not block A6 |
| **Status** | ✅ **RESOLVED** 2026-07-29 by owner ruling — the safe floor is the permanent behaviour; see the end of this entry |

**What the platform does.** `android.graphics.BlendMode` is **API 29**. Below it Compose composites
through `PorterDuffXfermode`, whose mode table contains no soft-light, so `BlendMode.Softlight` falls
through to the default — **`SRC_OVER`**. The failure is not a subtler grain; it is the noise tile
painted *opaquely* over the surface. At the Library cover's effective strength of **1.00** (see
**D-013**) that is a flat grey rectangle where the artwork should be.

This project's `minSdk` is **24**, so API 24–28 is inside the supported range.

**Why this is a defect entry and not just an implementation note.** The frozen design describes one
material and assumes it always renders. It has no second reading — no fallback texture, no "flat
paper" variant, no statement about which surfaces matter enough to degrade differently. That absence
is exactly the kind of thing the register exists to name rather than let implementation fill in
silently, which is how a design decision gets made by a `when` branch.

**What implementation does now — the safe floor.** On API 24–28,
[`Modifier.zinelyV2Grain`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Grain.kt)
draws **nothing** and the surface stays flat. Losing the paper texture is a smaller and more honest
failure than replacing the artwork with grey, and unlike the grey it cannot be mistaken for a design
choice. The same shape as the **D-012** call: implement the option that cannot ship something
actively wrong, disclose it as a choice, and leave it free to reverse — here it is one branch with no
callers yet.

**Owner decision requested.** On API 24–28, should V2 paper surfaces (a) render flat, as now;
(b) render a static warm tint approximating the grain's average effect, which is authoring a second
material; or (c) be treated as out of scope, raising `minSdk` to 29? Note that (c) is not only a
grain question — it would also retire the `RuntimeShader` and `fontVariationSettings` ceilings that
shaped [ADR-073](../DECISIONS.md#adr-073) and [ADR-076](../DECISIONS.md#adr-076), so it is a product
decision with a much wider blast radius than this entry.

**This is verifiable, and was verified rather than assumed.** `AndroidPaint_androidKt.setNativeBlendMode`
branches on `SDK_INT >= 29`, and `AndroidBlendMode_androidKt.toPorterDuffMode` has no `SOFT_LIGHT`
case and returns `SRC_OVER` by default — read from the decompiled `ui-graphics` 1.10.4 artifact, not
from documentation.

**✅ RESOLUTION — owner ruling, 2026-07-29: option (a). Flat paper is correct, not a fallback.**

> *"The current implementation is approved. **Do not emulate Soft Light using another blend mode. Do
> not invent an approximation.** For API 24–28, rendering flat paper is the correct constitutional
> behaviour because it preserves **material honesty** rather than introducing an incorrect simulation."*

Implementation proposed this as a *safe floor* — the least-bad option, held provisionally until an
owner chose. The ruling accepts the behaviour and rejects the framing: it is not the least-bad option,
it is the right one. Options (b) (a static warm tint approximating the grain) and (c) (raise `minSdk`
to 29) are both refused, and (b) is refused on principle rather than on cost — an approximation of a
material is a **second material**, and V2 has one. A device that cannot draw the paper shows paper it
cannot draw rather than something that merely resembles it.

That reasoning generalises past this entry, which is why it is worth stating plainly: where the
platform cannot express the frozen design, **implementation omits rather than approximates**, and says
so. Nothing here is a fallback texture in waiting.

**What implementation does: nothing further.** `Modifier.zinelyV2Grain` is already a no-op below API
29, gated on Compose's own `BlendMode.isSupported()` — see
[ADR-076](../DECISIONS.md#adr-076) Decision 9. The gate stays; what changes is that its KDoc no longer
describes itself as provisional, and no future package should treat it as an open question.

**Not a limitation to hide.** API 24–28 users see V2's paper without its grain. That is a real,
documented difference and belongs in release notes when V2 ships — as a **Known Limitation**, in the
[CLAUDE.md](../../CLAUDE.md#release-categories--never-conflate) sense, not as a bug and not as silence.

---

### D-015 — two concepts are each drawn twice, with different geometry, and one pair is inside a single file

| | |
|---|---|
| **Artifacts** | [`v2-proof.html`](mockups/v2-proof.html) lines 296 and 376 · [`v2-bench.html`](mockups/v2-bench.html) line 346 against [`v2-proof.html`](mockups/v2-proof.html) line 294 |
| **Found** | 2026-07-29, during Phase A / A7 (icons) |
| **Severity** | Cross-file (and intra-file) inconsistency — **does not block A7**; both are transcribed as found |
| **Status** | Open — **owner ruling requested** |

The trilogy contains 36 distinct icon marks across 42 placements. Two of those 36 are duplicates in
meaning but not in drawing.

**1. Chevron-right, twice, in the same file.**

| Where | Path | Used for |
|---|---|---|
| `v2-proof.html:376` | `M9 5l7 7-7 7` | the fold navigator's *next* button |
| `v2-proof.html:296` | `M9 6l6 6-6 6` | the READY band's affordance chevron |

They are not the same shape: the first spans 7 units and starts at y=5, the second spans 6 and starts
at y=6. The tell is the **left** chevron at `:276`, `M15 5l-7 7 7 7` — an exact mirror of the first and
not of the second. So `M9 6l6 6-6 6` is the one that does not belong to the pair, and its being the
only chevron on the resting band is either a deliberately smaller mark or a transcription that drifted.

**2. A check, twice, across two files.**

| Where | Path | Used for |
|---|---|---|
| `v2-proof.html:294` | `M4 12l5 5 11-12` | the READY tick, and three more placements (`:311` seal, `:344`, `:485`) |
| `v2-bench.html:346` | `M20 6 9 17l-5-5` | the Bench's *Done* button |

Same mark, drawn from opposite ends, with different proportions. The Proof's is used four times and is
the better-established of the two.

**Why this is worth a ruling rather than a quiet fix.** Collapsing either pair is a **redesign** — it
changes what a frozen surface renders — and choosing *which* geometry survives is a design judgement
implementation has no standing to make. Both are therefore transcribed exactly as found, as
[`ChevronRight`/`ChevronRightBand`](../../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyV2Icons.kt)
and `Tick`/`Done`. The cost of leaving it is small and precise: two extra entries in the set, and two
places where Phase B must pick the right one rather than the obvious one.

**Owner decision requested.** For each pair: are these one icon or two? If one, which geometry is
canonical — and the frozen file that loses should be amended, since the register does not permit
implementation to amend it. Note the chevron pair is the sharper case, because a single file drawing
the same affordance two ways is harder to read as intent than two files that froze at different times.

**Explicitly *not* raised here, having been checked:** the Library's `StampStar` (`:153`) and the
Bench's `Favourite` (`:599`) are also two stars with different geometry, but they are different things
— one is stroked cover *artwork*, the other a filled UI mark rendered in ochre at 12px. Likewise
`Shield` (`bench:461`) and `ShieldCheck` (`bench:605`) share an outline by design, the second adding a
tick. Neither is a defect, and both are noted so a later reader does not re-open them.

---

## Resolved

| ID | Defect | Resolved |
|---|---|---|
| **D-003** | The maker palette is ten inks or nineteen, depending on which document you read | 2026-07-28 — owner ruling: three bands, three categories, three collections. Entry kept above with its full resolution. |
| **D-005** | The Library and the Bench set the same role in two different serifs at two different weights | 2026-07-28 — owner ruling: the Constitution outranks both frozen files. Canonical serif is **Fraunces at 500**; the Library's 600 reflected its Georgia fallback. No code change owed. Entry kept above. |
| **D-007** | The constitutional 8pt rhythm is not observable in the frozen CSS | 2026-07-28 — owner ruling: §III is an implementation **aspiration**, not a token inventory. **No spacing scale is published**; spacing stays per-component exactly as frozen. Entry kept above. |
| **D-013** | The Library and the Bench bake different alpha into the same grain | 2026-07-29 — owner ruling: **deliberate, not drift**. Paper and printed covers are different physical materials; grain strength is **not** normalised and stays exactly as frozen. No code change owed, and no corpus cleanup owed either. Entry kept above. |
| **D-014** | The paper material cannot be drawn at all on API 24–28 | 2026-07-29 — owner ruling: rendering **flat paper is correct**, not a fallback. No emulation, no approximation, no `minSdk` bump — where the platform cannot express the design, implementation omits and discloses. Ships as a Known Limitation. Entry kept above. |
| **D-011** | The Library declares neither easing token and animates on a curve found nowhere else | 2026-07-28 — owner ruling: the Bench and Proof are the **canonical V2 motion language**; the Library's curve reflects its earlier freeze. Phase B uses the canonical tokens. No code change owed. Entry kept above. |

*(Resolved entries stay in place rather than being deleted — the record of what was once contradictory
is what stops it being reintroduced.)*

---

*Opened 2026-07-28 during the Compose V2 implementation programme. Governed by
[V2-CONSTITUTION.md](V2-CONSTITUTION.md); process defined in
[COMPOSE-IMPLEMENTATION-RULES.md](../COMPOSE-IMPLEMENTATION-RULES.md).*
