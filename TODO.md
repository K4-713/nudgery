# Next Steps

## Toolchain / Dependency Currency
Gradle and Kotlin are still pinned at the values set in the original scaffolding commit (`5f9c4a3`,
2026-05-12) — **Gradle 8.11.1** and **Kotlin 2.1.0**, both late-2024 releases, i.e. already ~6 months
stale when the project was scaffolded. This is *not* a deliberate compatibility pin (no ED or note
justifies it); it's just scaffolding-baseline drift. **AGP** was moved off that baseline to **8.10.1**
on 2026-07-30, but only as far as targeting Android 16 required: 8.10 is the newest line that runs on
Gradle 8.11.1, so it was the smallest step that supports `compileSdk 36`. Current AGP is 8.13.x, with
a 9.x line out. The modules also target **JVM 11** bytecode (`sourceCompatibility`/`jvmTarget`), which
is conservative. Per AGENTS.md ("dependencies must be kept current"), schedule a dedicated upgrade
pass — its own prep commit(s), separate from feature work, with a full green test run and a verified
rollback path:
- [ ] Bump the Gradle wrapper and Kotlin to current releases, and AGP the rest of the way (check
      actual latest at the time). AGP past 8.10 needs Gradle 8.13+, so those move together.
- [ ] Reconsider the JVM 11 bytecode target.
- [ ] After bumping, regenerate `CREDITS.md` (`./gradlew :androidApp:generateCredits`) if the
      dependency graph shifts.
- [ ] When bumping the Compose BOM: check whether material3's `Switch` now resets its thumb
      animation state on lazy-list reuse (`ThumbNode.onReset`, present on androidx-main as of
      2026-07 but absent through material3 1.4.0). Once a stable release has it, remove the
      `key(nudge.nudgeId)` workaround around the `Switch` in `NudgeListScreen.kt`
      (`NudgeListItem`), which exists only to stop recycled switches from visibly animating
      to their state while the main list scrolls.

## Test Debt (implemented EDs without TDD coverage)
- [ ] ED-3 (ship no emoji glyphs; render from device font): add a TDD_ test asserting no emoji image
      assets are bundled, so a future contributor can't silently start shipping glyph artwork.
- [ ] ED-4 (`PlatformEmojiGlyphFilter` hides un-renderable glyphs): needs a Robolectric/instrumented
      test exercising `Paint.hasGlyph` filtering; the JVM unit-test source set can't cover it.

## Drag-and-Drop Nudge Reordering
Long-press to pick up a nudge on the main list and drag to reorder, with an accent-tinted lift
(wash + shadow on the lifted card, accent outline on the landing gap), the rest of the list
animating to make space, auto-scroll at the edges, and haptics. Binding storage rules live in
ENGINEERING_DECISIONS.md (ED-19).

**Work breakdown:**
- [x] Phase 1 — Data foundation: `Nudge.sortOrder` column + migration 3 (backfill by `createdAt`),
      `selectAll` orders by it, inserts append, `ReorderNudgesUseCase`, TDD tests (`NudgeReorderTest`).
- [x] Phase 2 — Drag-and-drop UI on the main list: long-press lift (accent wash + shadow, scale),
      `Modifier.animateItem()` make-space, accent-tinted landing-gap outline, edge auto-scroll,
      haptics; commits the new order via `ReorderNudgesUseCase` on drop. (`NudgeReorder.kt` engine +
      `NudgeListScreen.kt` wiring; `MoveItemTest` covers the pure reorder.)
- [ ] Phase 3 — Accessibility: "Move up" / "Move down" custom semantics actions on each row so the
      reorder is usable without long-press-drag.
- [ ] Phase 4 — Backup/restore order (ED-19): full-ZIP backups preserve relative order; single-nudge
      JSON imports append. Add the field to the backup format + round-trip tests.
- [ ] Phase 5 — Docs: DESIGN.md interaction spec for the drag/lift/gap behavior. (The README half is
      done — reordering was documented as user-facing in the v1.0.0 release prep.)

## Motion Polish
DESIGN.md's Motion section specifies four animations that are not yet implemented (each tagged
*aspirational* there as of v0.9.1+):
- [ ] Nudge list load: subtle fade-in stagger, 30–50ms offset per item.
- [ ] "Save Answer" confirmation: brief checkmark or pulse (~300ms) before advancing.
- [ ] Calendar heat map entry animation matching Vico's timing.
- [ ] Detail-screen answer submission celebration — the golden-yellow whimsy moment.

## Detail Screen Edit-Affordance Redesign (exploration)
Tighten the detail-screen header and clarify how each part is edited. Not yet decided — captured
here from a design discussion. **Hard constraint:** the main chart only renders when there is data
(`if (uiState.visualizations.isNotEmpty())`), so the main question text cannot live *only* as the
chart title — it would vanish for a brand-new, answer-less nudge. The question needs an always-present
home. Note follow-up chart cards already title themselves with their question text; the main chart
does not, so titling it would add nice symmetry. Follow-ups with no chartable data have no card, so
per-card pencils can't be the *only* way to manage follow-ups — a "manage follow-ups" entry must remain.

Options considered:
- **A — Question as chart title + inline pencils:** main chart titled by the main question with a
  trailing pencil; follow-up cards get pencils too; a fallback question header covers the no-data
  state. Nice symmetry, edit co-located; downside is the question lives in two places by data state
  and jumps slot when data first appears.
- **B — Tap-the-thing-to-edit, drop the icon buttons (recommended):** question header, schedule line,
  and follow-up line each become directly tappable (open their wizard step) with a small trailing
  pencil glyph; remove the chunky 48dp `IconButton`s. Tightest, keeps deep-links, question always
  visible; tappable text is slightly less discoverable (the glyph mitigates).
- **C — Chips for metadata:** question header with a pencil; collapse schedule + follow-up into
  tappable chips that open their edit steps. Most compressed; "0 follow-ups" chip reads awkwardly.
- **D — One consolidated Edit:** drop all per-section icons; the single top-bar pencil opens the
  wizard. Cleanest body, but loses the deep-links and pushes editing a layer away.

Underlying principle: explicit pencil = most discoverable; tappable-text-with-glyph = tighter but
needs the hint; full consolidation = cleanest but least direct. (A partial down-payment already
landed: the header rows were grouped tightly and the edit icons shrunk from 48dp `IconButton`s to a
compact ~40dp `RowEditButton` to cut the dead space between the schedule and follow-up lines.)

## Emoji Question Type
Add an `EMOJI` question type: a TEXT question under the hood, but whose input is restricted to emoji-only, entered through our own emoji picker for an A+ cross-platform experience.

The binding engineering decisions and their rationale live in `ENGINEERING_DECISIONS.md` (ED-1 … ED-13): TEXT-under-the-hood storage, emoji-only validation, never shipping glyphs, per-device (not intersection) availability, generated catalog, default skin tone, default gender (neutral-only, never overriding explicit picks), hair/direction as pick-time variants (not settings), VS16 presentation normalization, CLDR-sourced search keywords, shared keyword/prefix search (no fuzzy/semantic in v1), the manual CLDR credit, and the conventional in-app picker entered via a read-only field (deliberately not documented for users). The decision *not* to use the system keyboard or Jetpack `EmojiPickerView` is captured there too.

The Android EMOJI question type has **shipped** (ED-1 … ED-14 all implemented, with tests). Only the
iOS picker remains, and it is deferred until iOS work begins.

**Work breakdown:**
- [x] Follow the DDD flow: add end-user docs (README) and design spec (DESIGN.md) for the EMOJI type, ensure the ENGINEERING_DECISIONS.md entries are current, then write TDD_ tests, then implement.
- [x] Shared: promote the existing emoji helpers (`extractEmojiWords`, `isSingleEmoji` in `GetVisualizationDataUseCase.kt`) into a reusable `shared/util` emoji validator; use it for live input filtering and a save-time emoji-only guard (with tests, incl. paste/garbage/edge cases).
- [x] Model: add `QuestionType.EMOJI` routing to the TEXT path for storage/export/charts; resolve the exhaustive `when` branches across the ~10 files that match `QuestionType` (most delegate to TEXT); verify backup export/import round-trips the new type.
- [x] Data: build the `emoji-test.txt` → shared categorized-list generator + the runtime `hasGlyph` capability filter. (`buildSrc` parser/generator + `:shared:generateEmojiCatalog`; 1,894 base concepts, 3 hair-capable confirming ED-8.)
- [x] Search data (ED-10): extend the generator to parse CLDR annotations and attach keywords (+ short name) per entry, English-only. (100% keyword coverage via FE0F-normalized matching.)
- [x] Credits (ED-12): manual Unicode/CLDR credit under `androidApp/config/libraries/` + `androidApp/config/licenses/`; `generateCredits` renders the Unicode-3.0 text.
- [x] Search logic (ED-11): shared keyword/prefix matcher with ranking (exact name > name prefix > keyword exact > keyword prefix), multi-word AND, results passed through the `hasGlyph` filter and defaults; no fuzzy/semantic in v1.
- [x] Defaults (ED-6, ED-7): add the default-skin-tone and default-gender settings and their apply-by-rule logic (skin-tone modifier; gender mapping derived from the catalog), both neutral-base-only and never overriding explicit picks; cover gender + tone composition.
- [x] Normalization (ED-9): VS16 emoji-presentation normalization in the validator/normalizer.
- [x] Android UI: custom emoji picker (grid, categories, search field, recents) in a bottom sheet, with skin-tone/gender defaults applied and hair/direction (ED-8) as pick-time variants; wire into `AnswerFormScreen`, plus the create/edit question wizard.
- [ ] iOS UI: custom picker reusing the shared list (later).

## Encrypt the Database at Rest
> Part of a broader sensitive-data threat model — see `SECURITY_NOTES.md`. This section is control **C1** there, and is currently being reconsidered in favor of an optional passphrase + dormant-when-locked "Protected mode" (C2) and encrypted backups (C3).

Encrypt the local SQLDelight database on-device (SQLCipher via the Android driver factory), with the encryption key held in the Android Keystore (TEE/StrongBox-backed) — never stored beside the database. No user login/passphrase: the key is device-bound and unwraps automatically when the app runs.

**Threat model — read before scoping.** This protects only a narrow (but real) set of cases: a device obtained **locked or powered off** and analyzed *off-device*, e.g. lost/stolen phones, a repair shop, casual forensic dumps. In those cases the copied `nudgery.db` is just ciphertext and the key never left the phone's secure hardware.

It does **not** help once the device is unlocked. A key insight is the forensic **BFU vs AFU** distinction:
- **BFU (Before First Unlock)** — device locked/off, not unlocked since boot. The OS already keeps user data encrypted; our encryption is a second layer for exactly the lost/stolen/repair case above.
- **AFU (After First Unlock)** / unlocked-in-hand — OS keys are in memory, *and* our auto-unwrapping key is available to anything running on the unlocked device. So this does nothing against a borrowed/stolen-while-unlocked phone or a **compelled unlock** (e.g. an aggressive border stop). Defending those would require a user-authentication gate (app-lock), which is deliberately out of scope here.

Net: worth doing for the locked-device / offline-analysis case; not a defense against an unlocked device. When this lands, record it as an ENGINEERING_DECISIONS entry (binding internal decision) with this rationale, plus instrumented `androidTest` coverage for the encrypted round-trip and the one-time plaintext→encrypted migration.

## Empty-State Welcome Illustration
The main list's empty state still ships the CC0 **placeholder** shape
(`res/drawable/empty_nudges_illustration.xml` — see its header comment). When the hand-drawn
illustration is ready (authored in Inkscape under `art/`, CC-BY-SA-4.0):
- [ ] Swap in the exported vector, change the drawable's SPDX line to `CC-BY-SA-4.0`, and add the
      source SVG + drawable to the `LICENSE` scope note (requirements in DESIGN.md → Empty States).
