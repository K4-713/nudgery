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

## Compose UI (`androidApp`) ✅ DONE
All 7 screens implemented in `androidApp/src/main/kotlin/com/nudgery/android/ui/`:
- `NudgeListScreen` — list with empty-state oversized FAB; `NudgeListItem` with schedule info and enable toggle
- `CreateNudgeScreen` — 3-step wizard (question/follow-ups/schedule) with shared `WizardNavBar` and `WizardSteps.kt`
- `EditNudgeScreen` — 2-step wizard (question/schedule); split-vs-in-place dialog when question text changes
- `NudgeDetailScreen` — schedule row, Answer Now button, chart section with timeframe chips, answer table with per-row hide/confirm
- `AnswerFormScreen` — animated step-by-step multi-question form; input widgets: YesNo, Number (slider), OptionSingle, OptionMulti, Text
- `SettingsScreen` — theme radio buttons (System/Light/Dark), bold text toggle, About link
- `AboutScreen` — app description and version from BuildConfig
- Theme: `NudgeryTheme` (dark/light palettes, violet/teal/yellow), `nudgeryTypography(bold)`, `nudgeryShapes`
- `AppSettings` (DataStore): `themePreference` + `boldText` flows; `SettingsViewModel` combines both
- `AnswerFormViewModel` — loads questions, evaluates follow-up triggers (EQ/GT/GTE/LT/LTE), records answer with `scheduledAt`
- `NudgeryScreen` sealed class + `ARG_NUDGE_ID` in `ui/nav/NudgeryNavGraph.kt`
- Full `NavHost` in `MainActivity` with all 7 destinations; notification intent handled via `onNewIntent`

### Remaining Compose UI work
- **Missed nudge indicator**: jaunty exclamation sticker on `NudgeListItem` when most recent scheduled fire is unanswered — requires `ComputePreviousFireTimeUseCase` (not yet implemented)
- **Vico chart integration** ✅ DONE:
  - ✅ `LineGraphChart` — `CartesianChartHost` + `LineCartesianLayer`; x-axis shows `month/day` from `DataPoint.at`
  - ✅ `NamedCountChart` (used for `ColumnChart`) — `CartesianChartHost` + `ColumnCartesianLayer`; x-axis shows `NamedCount.label`
  - ✅ `HorizontalBarChart` (used for `BarChart`) — custom Canvas-free `Column`/`Row` layout with proportional filled tracks; label left, count right
  - ✅ `CalendarHeatMapChart` — custom Canvas-based week×N grid; cell intensity = daily yes-count / max, lerped surfaceVariant→primary
  - ✅ `TagCloudChart` — custom `FlowRow`-based implementation; no Vico layer needed
- **Chart type picker**: ✅ DONE — `ModalBottomSheet` with `NudgeryToggleChip` per available chart type; icon button disabled when only one chart is available; selection persists across timeframe changes
- **Full-screen chart**: ✅ DONE — `FullScreenChartDialog` composable via `Dialog(usePlatformDefaultWidth = false)` + `Surface(fillMaxSize)`; `TopAppBar` with back arrow and optional chart type picker; timeframe chips at bottom; chart type selection hoisted to `NudgeDetailScreen` so it persists across open/close

## Runtime Permissions (`androidApp`)
- **POST_NOTIFICATIONS** (Android 13+) ✅ DONE — `NotificationPermissionEffect` composable in `MainActivity` requests the permission on first launch; result is logged; degrades gracefully if denied
- **SCHEDULE_EXACT_ALARM** (Android 12+): users can revoke this in Settings; detect revocation and either fall back to inexact alarms or prompt the user to re-grant it via the system Alarm & Reminder settings screen

## App Icon ✅ DONE
Custom adaptive icon: vector foreground (squiggle) + styled background (purple with warm yellow radial glow); `mipmap-anydpi-v26/ic_launcher.xml` for Android 8+; legacy PNGs at mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi for older devices.

## Play Store Listing Materials
Prepare before submitting:
- Export a 512×512 PNG icon for the Play Store store listing
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
- Be prepared to justify `SCHEDULE_EXACT_ALARM` use during Play Store review — the justification is user-configured reminder schedules
