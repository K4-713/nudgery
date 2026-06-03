# Next Steps

## Emoji Question Type
Add an `EMOJI` question type: a TEXT question under the hood, but whose input is restricted to emoji-only, entered through our own emoji picker for an A+ cross-platform experience.

The binding technical decisions and their rationale live in `TECHNICAL_DECISIONS.md` (TD-1 … TD-9): TEXT-under-the-hood storage, emoji-only validation, never shipping glyphs, per-device (not intersection) availability, generated catalog, default skin tone, default gender (neutral-only, never overriding explicit picks), hair/direction as pick-time variants (not settings), and VS16 presentation normalization. The decision *not* to use the system keyboard or Jetpack `EmojiPickerView` is captured there too.

**Work breakdown:**
- [ ] Follow the DDD flow: add end-user docs (README) and design spec (DESIGN.md) for the EMOJI type, ensure the TECHNICAL_DECISIONS.md entries are current, then write TDD_ tests, then implement.
- [ ] Shared: promote the existing emoji helpers (`extractEmojiWords`, `isSingleEmoji` in `GetVisualizationDataUseCase.kt`) into a reusable `shared/util` emoji validator; use it for live input filtering and a save-time emoji-only guard (with tests, incl. paste/garbage/edge cases).
- [ ] Model: add `QuestionType.EMOJI` routing to the TEXT path for storage/export/charts; resolve the exhaustive `when` branches across the ~10 files that match `QuestionType` (most delegate to TEXT); verify backup export/import round-trips the new type.
- [ ] Data: build the `emoji-test.txt` → shared categorized-list generator + the runtime `hasGlyph` capability filter (spike this first to confirm maintenance feels light before building the picker UI). Have the generator report the gendered-concept count as a sanity check on the TD-7 estimate (~90–110).
- [ ] Defaults (TD-6, TD-7): add the default-skin-tone and default-gender settings and their apply-by-rule logic (skin-tone modifier; gender mapping derived from the catalog), both neutral-base-only and never overriding explicit picks; cover gender + tone composition.
- [ ] Normalization (TD-9): VS16 emoji-presentation normalization in the validator/normalizer.
- [ ] Android UI: custom emoji picker (grid, categories, search, recents) in a bottom sheet, with skin-tone/gender defaults applied and hair/direction (TD-8) as pick-time variants; wire into `AnswerFormScreen`, plus the create/edit question wizard.
- [ ] iOS UI: custom picker reusing the shared list (later).

## Play Store Listing Materials
Prepare before submitting:
- Export a 512×512 PNG icon for the Play Store store listing (see `art/play_store_icon.png`)
- Short description (max 80 characters)
- Full description (max 4000 characters)
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
