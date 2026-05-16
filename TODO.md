# Next Steps

## Use Cases (`shared/commonMain`) ✅ DONE
All 7 use cases implemented in `shared/src/commonMain/kotlin/com/nudgery/shared/usecase/`:
- `CreateNudgeUseCase`, `UpdateNudgeUseCase`, `RecordAnswerUseCase`, `SetAnswerHiddenUseCase`
- `ComputeNextFireTimeUseCase`, `ExportAnswersUseCase`, `GetVisualizationDataUseCase`
Supporting types: `NudgeRequest.kt`, `Timeframe`, `ExportFormat`, `VisualizationData` (with `DailyCount`, `DataPoint`, `NamedCount`).

## TDD Tests (`shared/commonTest`, `androidApp/test`) ✅ DONE
All 8 test files filled in with real assertions against in-memory SQLite (74 tests, 0 failures).
Test utilities: `TestDatabase.kt` (in-memory repo factory), `FakeNotificationScheduler`.
Note: `androidApp/test/NudgeListViewModelTest.kt` skeletons remain (ViewModels not yet implemented).

## Android Notification Scheduling (`shared/androidMain`) ✅ DONE
- `WorkManagerNotificationScheduler` in `shared/androidMain/scheduler/` — schedules `OneTimeWorkRequest` chains per nudge using `ComputeNextFireTimeUseCase`; `ExistingWorkPolicy.REPLACE` for atomic reschedule
- `NudgeNotificationWorker` — shows notification, then re-enqueues the next fire (self-scheduling pattern)
- `RescheduleAllNudgesWorker` — rescheduled all enabled nudges; triggered on boot and timezone change
- `NudgeNotificationChannel` — creates the `nudge_prompts` notification channel on app startup
- `BootReceiver` and `TimezoneChangeReceiver` registered in `AndroidManifest.xml`
- `AppModule` provides `WorkManagerNotificationScheduler` as the `NotificationScheduler` singleton
- Instrumented tests: `WorkManagerSchedulerTest` (3 tests) and `NudgeNotificationWorkerTest` (4 tests) in `androidApp/src/androidTest/`; test manifest removes WorkManager auto-init so `WorkManagerTestInitHelper` controls initialization per test

## ViewModels (`androidApp`)
- `NudgeListViewModel` — exposes `Flow<List<NudgeSummary>>` (nudge + next fire time + enabled status)
- `CreateNudgeViewModel` — form state + calls `CreateNudgeUseCase`
- `NudgeDetailViewModel` — answer history, visualization data, "Answer Now", enabled toggle
- `EditNudgeViewModel` — edit form state + calls `UpdateNudgeUseCase`, surfaces split-or-in-place prompt

## Compose UI (`androidApp`)
- Main screen: nudge list
- Create/Edit screen: question builder, option list, schedule picker
- Detail screen: chart selector, timeframe picker, raw answer table, export button
- Answer form screen: driven by notification tap or "Answer Now"
- Navigation graph wiring in `MainActivity`
