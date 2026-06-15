# Next Steps

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
- [ ] Phase 5 — Docs: DESIGN.md interaction spec for the drag/lift/gap behavior; README if the
      reordering is surfaced as a user-facing feature.

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

**Work breakdown:**
- [ ] Follow the DDD flow: add end-user docs (README) and design spec (DESIGN.md) for the EMOJI type, ensure the ENGINEERING_DECISIONS.md entries are current, then write TDD_ tests, then implement.
- [ ] Shared: promote the existing emoji helpers (`extractEmojiWords`, `isSingleEmoji` in `GetVisualizationDataUseCase.kt`) into a reusable `shared/util` emoji validator; use it for live input filtering and a save-time emoji-only guard (with tests, incl. paste/garbage/edge cases).
- [ ] Model: add `QuestionType.EMOJI` routing to the TEXT path for storage/export/charts; resolve the exhaustive `when` branches across the ~10 files that match `QuestionType` (most delegate to TEXT); verify backup export/import round-trips the new type.
- [x] Data: build the `emoji-test.txt` → shared categorized-list generator + the runtime `hasGlyph` capability filter. (`buildSrc` parser/generator + `:shared:generateEmojiCatalog`; 1,894 base concepts, 3 hair-capable confirming ED-8.)
- [x] Search data (ED-10): extend the generator to parse CLDR annotations and attach keywords (+ short name) per entry, English-only. (100% keyword coverage via FE0F-normalized matching.)
- [x] Credits (ED-12): manual Unicode/CLDR credit under `androidApp/config/libraries/` + `androidApp/config/licenses/`; `generateCredits` renders the Unicode-3.0 text.
- [ ] Search logic (ED-11): shared keyword/prefix matcher with ranking (exact name > name prefix > keyword exact > keyword prefix), multi-word AND, results passed through the `hasGlyph` filter and defaults; no fuzzy/semantic in v1.
- [ ] Defaults (ED-6, ED-7): add the default-skin-tone and default-gender settings and their apply-by-rule logic (skin-tone modifier; gender mapping derived from the catalog), both neutral-base-only and never overriding explicit picks; cover gender + tone composition.
- [ ] Normalization (ED-9): VS16 emoji-presentation normalization in the validator/normalizer.
- [ ] Android UI: custom emoji picker (grid, categories, search field, recents) in a bottom sheet, with skin-tone/gender defaults applied and hair/direction (ED-8) as pick-time variants; wire into `AnswerFormScreen`, plus the create/edit question wizard.
- [ ] iOS UI: custom picker reusing the shared list (later).

## Encrypt the Database at Rest
> Part of a broader sensitive-data threat model — see `SECURITY_NOTES.md`. This section is control **C1** there, and is currently being reconsidered in favor of an optional passphrase + dormant-when-locked "Protected mode" (C2) and encrypted backups (C3).

Encrypt the local SQLDelight database on-device (SQLCipher via the Android driver factory), with the encryption key held in the Android Keystore (TEE/StrongBox-backed) — never stored beside the database. No user login/passphrase: the key is device-bound and unwraps automatically when the app runs.

**Threat model — read before scoping.** This protects only a narrow (but real) set of cases: a device obtained **locked or powered off** and analyzed *off-device*, e.g. lost/stolen phones, a repair shop, casual forensic dumps. In those cases the copied `nudgery.db` is just ciphertext and the key never left the phone's secure hardware.

It does **not** help once the device is unlocked. A key insight is the forensic **BFU vs AFU** distinction:
- **BFU (Before First Unlock)** — device locked/off, not unlocked since boot. The OS already keeps user data encrypted; our encryption is a second layer for exactly the lost/stolen/repair case above.
- **AFU (After First Unlock)** / unlocked-in-hand — OS keys are in memory, *and* our auto-unwrapping key is available to anything running on the unlocked device. So this does nothing against a borrowed/stolen-while-unlocked phone or a **compelled unlock** (e.g. an aggressive border stop). Defending those would require a user-authentication gate (app-lock), which is deliberately out of scope here.

Net: worth doing for the locked-device / offline-analysis case; not a defense against an unlocked device. When this lands, record it as an ENGINEERING_DECISIONS entry (binding internal decision) with this rationale, plus instrumented `androidTest` coverage for the encrypted round-trip and the one-time plaintext→encrypted migration.

## Play Store Listing Materials
Prepare before submitting:
- Export a 512×512 PNG icon for the Play Store store listing (see `art/play_store_icon.png`)
- Short description (max 80 characters)
- Full description (max 4000 characters) — mention `.nudge` file sharing so users searching for "nudge file" can find the app
- At least 2 phone screenshots (additional tablet/foldable screenshots improve ranking)
- Feature graphic (1024×500 PNG or JPEG)
- Privacy policy: even though no data leaves the device, Google requires a hosted privacy policy URL; a simple page stating that all data is stored locally and nothing is collected or shared is sufficient
- Complete the **Data Safety** form in Play Console (declare: no data shared with third parties, data stored on-device, no account required)
- Complete the **Content Rating** questionnaire

## applicationId and Versioning
- Confirm `applicationId = "com.nudgery.android"` is final — it cannot be changed after the first publish without losing all installs and reviews
- Consider enabling **Play App Signing** (Google holds the upload key; strongly recommended for new apps)

## Release Build (`androidApp`)
- Create a signing keystore and add `signingConfigs` to `androidApp/build.gradle.kts`
- Store keystore path and credentials in `local.properties` (already gitignored); never commit secrets to the repo
- Verify ProGuard/R8 rules don't strip needed classes — check SQLDelight generated code, Koin reflection, WorkManager, and Vico; add keep rules to `proguard-rules.pro` as needed
- Run `./gradlew :androidApp:bundleRelease` to produce an AAB for Play Store submission (AAB is required; APK is not accepted for new apps)
- Test the release build on a physical device before submitting
- The app declares `USE_EXACT_ALARM` (API 33+) rather than `SCHEDULE_EXACT_ALARM`; Play Store review for this permission is approval-based for reminder/scheduling apps. No special justification workflow is planned — approval is assumed.
