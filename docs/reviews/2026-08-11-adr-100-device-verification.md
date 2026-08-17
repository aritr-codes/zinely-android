# Device verification — ADR-100's six Library rulings

Both passes, per [CLAUDE.md § Device Verification](../../CLAUDE.md#device-verification-mandatory). This is the
record ADR-100 §9 said was owed; it is what moves that ADR off `Proposed`.

| | |
|---|---|
| **Device** | Samsung `SM-A176B` (`a17x`), serial `RZCYA1VBQ2H` |
| **OS** | Android **16** |
| **Density** | 450 physical, **420 override** — so 1dp = 2.625px, and every px figure below divides by that |
| **Build** | `:app:installDebug` from the working tree of `feat/v21-freeze-and-tokens`, `versionName 0.9.0-beta.1`, `versionCode 3` |
| **Date** | 2026-08-11 |
| **TalkBack** | **not enabled** — see the honesty note under Pass 1 |

Corpus: four zines created through the real flow (Make a zine → paper choice), one renamed through the real
`⋮ → Rename` flow to *"Notes from the Sunday market, volume three"*. Both themes toggled with
`adb shell cmd uimode night yes|no`.

---

## Pass 1 — developer verification

*Does the implementation behave exactly as specified?*

### The platform tree, not the framework's

The instrument CLAUDE.md names, and the one that caught the ADR-058 defect. `uiautomator dump`, light theme,
four zines:

```
TextView  "Your shelf"                                    [42,163][381,256]
TextView  "4 zines"                                       [905,237][1006,275]
Button    desc="My zine"              clk foc             [42,353][519,1096]
Button    desc="Actions for My zine"  clk foc             [413,990][539,1116]
Button    desc="My zine"              clk foc             [561,353][1038,1096]
Button    desc="Actions for My zine"  clk foc             [932,990][1058,1116]
… ×2 more rows …
Button    desc="Make a zine"          clk foc             [340,1879][741,2025]
```

- Every tile is `android.widget.Button` with the zine's own name — not a `View` with no name, which is the
  ADR-059 failure shape.
- **Every overflow is a real `Button` announcing `"Actions for My zine"`.** ADR-100 §9 flagged the overflow's
  behaviour contract as unruled; it is in fact present and correct in the platform tree.
- **Traversal is 2N**, exactly as ADR-100 §9 predicted: 8 focus stops for 4 zines. The cost is real and is
  accepted — the alternative is a menu with no way in — but it is now measured rather than estimated.

⚠️ **Honesty note: TalkBack itself was not stepped.** The platform `AccessibilityNodeInfo` tree is the
instrument CLAUDE.md prescribes and it is clean — roles, names, clickability, focusability and traversal
count all read correctly off the real provider. But reading the tree is not the same as hearing it, and this
record does not claim otherwise.

### One reading corrected mid-pass, recorded because the first reading was wrong

At the resting scroll position the last row's **left** overflow button measured **83px = 31.6dp**, against
48.0dp for the other three — clipped by the dock's floating *Make a zine* button, and asymmetric because the
right-hand one sits outside the dock button's x-range. That looks exactly like the previous pass's failure
(Delete behind the navigation bar), and it was written up as one.

**It is not a defect.** Scrolling to the end of the list — which `ShelfDockClearance = 132.dp` guarantees and
`ZineShelfTest` asserts — restores all four to **126px = 48.0dp**, verified by a second dump. A floating dock
overlapping a mid-scroll list is ordinary and recoverable. The finding is retracted; it is kept here because
a plausible-looking measurement taken at one scroll position nearly became a fabricated defect.

### Ruling-by-ruling, measured off the device raster

Pixels sampled from `adb exec-out screencap` and composited-contrast computed per WCAG 2.x.

| Ruling | Evidence on glass | Verdict |
|---|---|---|
| **1** overflow mark | Three dots, stacked, `inkSoft`, visible in both themes; opens the action sheet; 48dp target | ✅ |
| **2** paper stocks pin | `.paper-s` **`#F9F0E2`** and `.paper-c` **`#F7E6C1`** — **byte-identical in light and dark**. Against the dark desk: **14.05** and **12.88** | ✅ |
| **3** two-line cap | *"Notes from the Sunday market, volu…"* — two lines, ellipsised, and **the neighbour's cover sits level**. Grid intact | ✅ |
| **4** count chip / ring | Chip ground is butter in both themes (`#F7B531` / `#E9B75D`); label on its own ground **7.67 / 9.09**. The dock's `--frame` ring is a visible butter halo in **light**, where it was invisible | ✅ |
| **5** tilt phase | Covers tilt in the three-cycle; no phase artifact visible | ✅ |
| **6** `ZineCover` deleted | No V2 cover renders anywhere; shelf paints from `v21Fill` | ✅ |

**On the numbers being ~1 point under the spec figures** (14.05 vs 15.39, 12.88 vs 14.02): the grain overlay
composites `multiply` over the stock, so the device pixel is legitimately darker than the token. The claim
ADR-100 §2 actually makes — that the two stocks **do not change between themes** — is exact on device: the
same hex in both.

### Ruling 2, in one sentence

In dark theme the two paper covers are now the brightest objects on the screen and read unmistakably as
printed paper resting on a desk. Before the ruling they were `#332B22` on `#241E18` — **1.18:1**, with the
hard shadow at 1.17:1 — which is not a cover on a desk but a hole cut in one. This is the ruling that most
needed glass, and glass confirms it.

**Pass 1: PASS.**

---

## Pass 2 — first-time user verification

*Would I naturally understand what this screen wants me to do?*

### The shelf answers its question

Arriving at *"Your shelf"* with four zines, the question *"which zine do I want?"* is answered by the covers
before any text is read — they are different colours, different stocks, tilted and taped like objects on a
desk. The count chip confirms the shelf's size without being asked. Nothing on the screen answers a question
belonging to a later moment. The ⋮ reads as *more actions* and not as *this name is cut short*, which is
precisely what ruling 1 set out to buy and what the frozen horizontal `⋯` would have lost.

### Finding P2-1 — the Rename sheet's red Save (Observation, out of scope, pre-existing)

**Written before I knew the reason, as CLAUDE.md asks:** *"I tapped Rename, and the confirm button is red. Am
I about to delete something?"*

Then the reason: the button is `ZinelyTheme.colors.coralStrong` = **`#C64E34`**, measured — which is **V1's
accent**, the colour V1's own palette defines as *"the user's next action"*. The rename sheet is V1 chrome on
a V2 screen, an accepted Known Limitation under [ADR-086](../DECISIONS.md#adr-086), and `ShelfSheets.kt`'s
own KDoc says so in terms.

So it is **not** a V2.1 `jam` misuse and **not** a regression from ADR-100. But the finding survives its
explanation, and that is the point worth recording: **V2.1 has taught the eye that red is consequence** —
`jam` is *"the only urgent colour: delete, the cut line, error blocks"* — so a V1 sheet that was internally
consistent now reads as a warning inside the new language. The Known Limitation acquired a cost it did not
have when it was accepted.

- **Not fixed in this pass**, deliberately. It is V1 chrome awaiting its own re-skin; repainting one V1
  control to a V2.1 token would create a third palette state on that surface, which is worse than the
  limitation.
- **Recorded as evidence** that the rename sheet should be re-skinned earlier rather than later, and that
  ADR-086's limitation entry deserves this note attached to it.

### Finding P2-2 — the first question the app asks is the printer's (Observation, pre-existing)

*Make a zine* opens **"Start a zine — choose your paper. A4 or Letter."** As a first-time user I have not yet
decided what I am making, and I am being asked a question about my printer. The zine is then called *"My
zine"* until I go and find Rename, which is why four zines made in a row are four tiles all reading *"My
zine"* — a shelf that cannot answer its own question until the user does extra work nobody prompted.

Out of ADR-100's scope entirely (it is the creation flow, not the Library re-skin) and pre-existing. Logged
because Pass 2 is where it becomes visible, and because it is cheap to fix by ordering the questions
differently rather than by adding a feature.

**Pass 2: PASS**, with two Observations, both pre-existing, neither a blocker for ADR-100.

---

## Acceptance

Both passes succeed, and they do not disagree. **ADR-100's six rulings are accepted on device.**

The two Pass 2 Observations belong to surfaces ADR-100 does not own, are recorded rather than silently
absorbed, and are carried forward to the Proof/Bench re-skins and to ADR-086's limitation entry.
