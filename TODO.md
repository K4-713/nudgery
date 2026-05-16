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
