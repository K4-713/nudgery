# Next Steps

## Use Cases (`shared/commonMain`)
Implement one class per operation; this is where validation and business rules live.

- `CreateNudgeUseCase` — validate (main question cannot be TEXT; options ≤ 16), persist Nudge + Questions + Options + Schedule, trigger `NotificationScheduler.schedule` if enabled
- `UpdateNudgeUseCase` — enforce that base question type cannot change; when question/option text changes, apply split-or-in-place logic; write `NudgeEdit` audit record on in-place edits; trigger `NotificationScheduler.reschedule` when schedule changes
- `RecordAnswerUseCase` — persist an `Answer` (used by both notification-driven forms and the "Answer Now" path)
- `SetAnswerHiddenUseCase` — toggle `Answer.isHidden`
- `ComputeNextFireTimeUseCase` — given a `Schedule`, return the next `Instant` the nudge should fire (used by the list and notification scheduling)
- `ExportAnswersUseCase` — produce CSV or TSV text from `Answer` rows joined with `Question`, `Nudge`, and `QuestionOption`
- `GetVisualizationDataUseCase` — return pre-aggregated structures (`List<DailyCount>`, etc.) filtered by timeframe and excluding hidden answers; one output type per chart kind

## Fill In TDD Tests (`shared/commonTest`, `androidApp/test`)
Once each use case exists, replace its `TODO("TDD skeleton")` calls with real assertions against an in-memory SQLite database (use `JdbcSqliteDriver(IN_MEMORY)` + `NudgeryDatabase.Schema.create(driver)`).

## Android Notification Scheduling (`shared/androidMain`)
Implement `NotificationScheduler` using WorkManager. Use a `PeriodicWorkRequest` or `OneTimeWorkRequest` chain based on `ScheduleType`; account for timezone changes via a `BroadcastReceiver` on `ACTION_TIMEZONE_CHANGED`.

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
