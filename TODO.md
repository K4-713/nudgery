# Next Steps

## Use Cases (`shared/commonMain`) ✅ DONE
All 7 use cases implemented in `shared/src/commonMain/kotlin/com/nudgery/shared/usecase/`:
- `CreateNudgeUseCase`, `UpdateNudgeUseCase`, `RecordAnswerUseCase`, `SetAnswerHiddenUseCase`
- `ComputeNextFireTimeUseCase`, `ExportAnswersUseCase`, `GetVisualizationDataUseCase`
Supporting types: `NudgeRequest.kt`, `Timeframe`, `ExportFormat`, `VisualizationData` (with `DailyCount`, `DataPoint`, `NamedCount`).

## TDD Tests (`shared/commonTest`, `androidApp/test`) ✅ DONE
All 8 shared test files filled in with real assertions against in-memory SQLite (74 tests, 0 failures).
Test utilities: `TestDatabase.kt` (in-memory repo factory), `FakeNotificationScheduler`.
ViewModel tests added alongside the ViewModel implementation (see below); 96 total tests, 0 failures.

## Android Notification Scheduling (`shared/androidMain`) ✅ DONE
- `WorkManagerNotificationScheduler` in `shared/androidMain/scheduler/` — schedules `OneTimeWorkRequest` chains per nudge using `ComputeNextFireTimeUseCase`; `ExistingWorkPolicy.REPLACE` for atomic reschedule
- `NudgeNotificationWorker` — shows notification, then re-enqueues the next fire (self-scheduling pattern)
- `RescheduleAllNudgesWorker` — rescheduled all enabled nudges; triggered on boot and timezone change
- `NudgeNotificationChannel` — creates the `nudge_prompts` notification channel on app startup
- `BootReceiver` and `TimezoneChangeReceiver` registered in `AndroidManifest.xml`
- `AppModule` provides `WorkManagerNotificationScheduler` as the `NotificationScheduler` singleton
- Instrumented tests: `WorkManagerSchedulerTest` (3 tests) and `NudgeNotificationWorkerTest` (4 tests) in `androidApp/src/androidTest/`; test manifest removes WorkManager auto-init so `WorkManagerTestInitHelper` controls initialization per test

## ViewModels (`androidApp`) ✅ DONE
All 4 ViewModels in `androidApp/src/main/kotlin/com/nudgery/android/viewmodel/`:
- `NudgeListViewModel` — observes nudge list, builds `NudgeSummary` (name, schedule description, next fire time, enabled), exposes `toggleEnabled()` and `handleNotificationIntent()` for notification-tap navigation
- `CreateNudgeViewModel` — manages `CreateNudgeFormState` (main question, follow-ups, schedule, name, enabled), calls `CreateNudgeUseCase` on `submit()`
- `NudgeDetailViewModel` — loads static data on init, live-observes answers (joined with question/option text via `combine`), loads visualizations per timeframe, exposes `setAnswerHidden()`, `exportAnswers()`, `updateEnabled()`
- `EditNudgeViewModel` — pre-populates form from DB, detects question/option text changes, uses `submit()` → split dialog → `submitWithSplit()` / `submitInPlace()` flow
- Shared form types: `QuestionFormState`, `ScheduleFormState` (with `toRequest()`, `fromSchedule()`, `toDescription()`)
- All 4 ViewModels registered in `AppModule` via Koin `viewModel { }` blocks; detail/edit receive `nudgeId` via `parametersOf()`
- 22 new ViewModel unit tests in `androidApp/src/test/` using fake repositories and `UnconfinedTestDispatcher`

## Compose UI (`androidApp`)
- Main screen: nudge list
- Create/Edit screen: question builder, option list, schedule picker
- Detail screen: chart selector, timeframe picker, raw answer table, export button
- Answer form screen: driven by notification tap or "Answer Now"
- Navigation graph wiring in `MainActivity`

## Runtime Permissions (`androidApp`)
These need UI flows — best added alongside the Compose screens:
- **POST_NOTIFICATIONS** (Android 13+): request at runtime on first launch or when the user creates their first nudge; if denied, nudges silently fail to notify
- **SCHEDULE_EXACT_ALARM** (Android 12+): users can revoke this in Settings; detect revocation and either fall back to inexact alarms or prompt the user to re-grant it via the system Alarm & Reminder settings screen
- Both permissions should degrade gracefully if denied rather than crashing or silently breaking

## App Icon
- Design an adaptive icon (foreground layer + background layer) for the app — required for Android 8+ and modern Play Store listings
- Export a 512×512 PNG for the Play Store store listing
- Replace the current Android Studio placeholder icon in `androidApp/src/main/res/`

## Play Store Listing Materials
Prepare before submitting:
- Short description (max 80 characters)
- Full description (max 4000 characters)
- At least 2 phone screenshots (additional tablet/foldable screenshots improve ranking)
- Feature graphic (1024×500 PNG or JPEG)
- Privacy policy: even though no data leaves the device, Google requires a hosted privacy policy URL; a simple page stating that all data is stored locally and nothing is collected or shared is sufficient
- Complete the **Data Safety** form in Play Console (declare: no data shared with third parties, data stored on-device, no account required)
- Complete the **Content Rating** questionnaire

## applicationId and Versioning
- Confirm `applicationId = "com.nudgery.android"` is final — it cannot be changed after the first publish without losing all installs and reviews
- Establish a `versionCode` increment strategy before the first upload (must increase monotonically with every release)
- Consider enabling **Play App Signing** (Google holds the upload key; strongly recommended for new apps)

## Release Build (`androidApp`)
- Create a signing keystore and add `signingConfigs` to `androidApp/build.gradle.kts`
- Store keystore path and credentials in `local.properties` (already gitignored); never commit secrets to the repo
- Verify ProGuard/R8 rules don't strip needed classes — check SQLDelight generated code, Koin reflection, WorkManager, and Vico; add keep rules to `proguard-rules.pro` as needed
- Run `./gradlew :androidApp:bundleRelease` to produce an AAB for Play Store submission (AAB is required; APK is not accepted for new apps)
- Test the release build on a physical device before submitting
- Be prepared to justify `SCHEDULE_EXACT_ALARM` use during Play Store review — the justification is user-configured reminder schedules
