# Next Steps

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
- **Versioning** ✅ DONE — `versionCode` = `git rev-list --count HEAD` (auto-increments with every commit); `versionName` = `git describe --tags --always --dirty` (e.g. `0.1.0`, `0.1.0-5-gabc1234`, `0.1.0-dirty`); `v0.1.0` tagged on initial commit; to release `0.2.0`, run `git tag v0.2.0`
- Consider enabling **Play App Signing** (Google holds the upload key; strongly recommended for new apps)

## Release Build (`androidApp`)
- Create a signing keystore and add `signingConfigs` to `androidApp/build.gradle.kts`
- Store keystore path and credentials in `local.properties` (already gitignored); never commit secrets to the repo
- Verify ProGuard/R8 rules don't strip needed classes — check SQLDelight generated code, Koin reflection, WorkManager, and Vico; add keep rules to `proguard-rules.pro` as needed
- Run `./gradlew :androidApp:bundleRelease` to produce an AAB for Play Store submission (AAB is required; APK is not accepted for new apps)
- Test the release build on a physical device before submitting
- The app declares `USE_EXACT_ALARM` (API 33+) rather than `SCHEDULE_EXACT_ALARM`; Play Store review for this permission is approval-based for reminder/scheduling apps. No special justification workflow is planned — approval is assumed.
