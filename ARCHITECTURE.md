# Nudgery Architecture

## Platform Strategy

Nudgery is built with **Kotlin Multiplatform (KMP)** to share business logic, data models, and database code across platforms. The current target is Android; iOS support is a planned future target.

- `shared/` — KMP module containing all platform-agnostic code
- `androidApp/` — Android UI and platform-specific wiring
- `iosApp/` — *(future)* iOS UI and platform-specific wiring

Platform-specific concerns (notifications, file I/O) are abstracted behind interfaces defined in `shared/` and implemented per-platform using Kotlin's `expect`/`actual` mechanism.

---

## Tech Stack

| Concern | Library | Notes |
|---|---|---|
| UI (Android) | Jetpack Compose | |
| UI (iOS, future) | SwiftUI | Consumes shared KMP logic via Swift/Kotlin interop |
| Database | SQLDelight | KMP-compatible; generates type-safe Kotlin from `.sq` files |
| Async / concurrency | kotlinx.coroutines | Shared across platforms |
| Date / time | kotlinx.datetime | Shared across platforms; avoids `java.time` Android API level issues |
| Dependency injection | Koin | KMP-compatible; Hilt is Android-only and therefore excluded |
| Notification scheduling (Android) | AlarmManager + WorkManager | AlarmManager provides precise timing; WorkManager handles reliable execution. Implements shared `NotificationScheduler` interface. |
| Notification scheduling (iOS, future) | UNUserNotificationCenter | Will implement the same `NotificationScheduler` interface |
| Charts (Android) | Vico | Compose-native charting library |
| Settings persistence | DataStore Preferences | Stores `ThemePreference`, bold text toggle, `ChartPalette`, and per-nudge default timeframe (keyed as `default_timeframe_<nudgeId>`); flows observed by `SettingsViewModel` and `NudgeDetailViewModel` |
| Typeface | Atkinson Hyperlegible Next | All 14 weight/style variants bundled as TTF in `androidApp/src/main/res/font/`; wired into `nudgeryTypography()` in `Type.kt` |

---

## Module Structure

```
nudgery/
├── shared/
│   ├── commonMain/          # Platform-agnostic models, repos, use cases, DB schema
│   │   ├── model/           # Nudge, Question, Schedule, Answer, etc.
│   │   ├── repository/      # Repository interfaces
│   │   ├── usecase/         # Business logic (CreateNudge, UpdateNudge, RecordAnswer, etc.)
│   │   ├── scheduler/       # NotificationScheduler interface
│   │   └── di/              # sharedModule (Koin)
│   ├── androidMain/         # Android implementations
│   │   ├── db/              # AndroidSqliteDriver, DatabaseDriverFactory
│   │   ├── notification/    # NudgeNotificationWorker, NudgeAlarmReceiver,
│   │   │                    #   NudgeNotificationChannel, RescheduleAllNudgesWorker,
│   │   │                    #   NotificationWorkerConfig
│   │   └── scheduler/       # WorkManagerNotificationScheduler
│   └── iosMain/             # (future) iOS actual implementations
├── androidApp/
│   ├── di/                  # appModule (Koin) — DatabaseDriverFactory, scheduler, ViewModels
│   ├── backup/              # NudgeBackupParser (org.json — Android SDK built-in)
│   ├── notification/        # BootReceiver, TimezoneChangeReceiver
│   ├── settings/            # AppSettings (DataStore) — themePreference, boldText, chartPalette
│   ├── viewmodel/           # AndroidX ViewModels + UiState types + form state helpers
│   ├── ui/
│   │   ├── nav/             # NudgeryScreen sealed class, route/arg constants
│   │   ├── screen/          # NudgeListScreen, CreateNudgeScreen, EditNudgeScreen,
│   │   │                    #   NudgeDetailScreen, AnswerFormScreen, SettingsScreen,
│   │   │                    #   AboutScreen, WizardSteps (shared wizard composables)
│   │   └── theme/           # NudgeryTheme, Color, Type (Atkinson Hyperlegible Next),
│   │                        #   nudgeryShapes, nudgeryTypography(bold)
│   └── MainActivity.kt
└── iosApp/                  # (future) SwiftUI app target
```

---

## Architecture Pattern

MVVM with a Use Case layer between ViewModels and Repositories.

```
UI (Compose / SwiftUI)
    └── ViewModel  (androidApp/viewmodel)
            └── Use Cases  (shared/commonMain/usecase)
                    └── Repository interfaces  (shared/commonMain/repository)
                            ├── SQLDelight implementations  (shared/androidMain/db)
                            └── NotificationScheduler  (shared/androidMain/scheduler)
```

ViewModels live in the platform app modules (`androidApp`, future `iosApp`). All business logic lives in `shared/commonMain` use cases and repositories, keeping it testable without a device.

### ViewModels

| ViewModel | Responsibility |
|---|---|
| `NudgeListViewModel` | Observes nudge list; builds `NudgeSummary` (name, schedule description, next fire time formatted as `String?` in local time, enabled); `toggleEnabled()`; holds `PendingAnswerNavigation` state for notification-tap routing |
| `CreateNudgeViewModel` | Manages `CreateNudgeFormState` (main question, follow-ups, schedule, name, enabled); calls `CreateNudgeUseCase` on `submit()` |
| `EditNudgeViewModel` | Pre-populates form from DB; tracks follow-ups as `List<EditableFollowUp>` (wraps `QuestionFormState` with an optional DB `questionId`); `addFollowUp()`, `updateFollowUp()`, `removeFollowUp()`; passes `followUpReplacements` to `UpdateNudgeUseCase` on save; detects question/option text changes; `submit()` → optional split dialog → `submitWithSplit()` / `submitInPlace()` |
| `NudgeDetailViewModel` | Loads static data on init (including `mainQuestionText` and `followUpCount` for display); reads persisted default timeframe from `AppSettings` on init and saves it when changed; live-observes answers via `combine`; loads visualizations per timeframe; `setAnswerHidden()`, `exportAnswers()` |
| `AnswerFormViewModel` | Loads questions; evaluates follow-up trigger conditions (EQ/GT/GTE/LT/LTE/CONTAINS); records each answer with its `scheduledAt` time; manages multi-step form progression |
| `SettingsViewModel` | Combines `themePreference`, `boldText`, and `chartPalette` flows from `AppSettings` (DataStore) with `importStatus` into a single `SettingsUiState`; accepts `ImportNudgeUseCase` and `NudgeBackupParser` for the import-from-backup feature |

### ViewModel conventions

- Each screen has one ViewModel that exposes a single `StateFlow<UiState>` (plus secondary flows for one-shot events like navigation).
- ViewModels accept **use cases** as constructor dependencies for mutations, and **repositories** for flows/observations.
- Form state (create and edit screens) is held in a `*FormState` data class updated via fine-grained setters; the final state is converted to a use-case request on `submit()`.
- `ScheduleFormState` and `QuestionFormState` are shared between `CreateNudgeViewModel` and `EditNudgeViewModel`.
- The edit flow for question/option text uses a `submit()` → optional split dialog → `submitWithSplit()` / `submitInPlace()` pattern, corresponding to the split-or-in-place choice described in the README.
- ViewModels are registered in `appModule` via Koin `viewModel { }` blocks. Detail and edit ViewModels receive their `nudgeId` via Koin `parametersOf(nudgeId)` at the call site.

### Time Display Formatting

`androidApp/viewmodel/TimeDisplayExtensions.kt` provides two internal extension functions used across ViewModels:

- `LocalTime.toDisplayString()` — formats a time as "9 AM" or "2:30 PM" (no leading zero, no minutes when on the hour)
- `Instant.toLocalDisplayString(timeZone, now)` — converts an `Instant` to a human-readable local-time string: "Tomorrow at 2:30 PM" if the date is tomorrow in the given timezone, otherwise "May 20 at 9 AM" (full month name, no year, no day-of-week, no timezone notation). `now` defaults to `Clock.System.now()` and can be overridden in tests for deterministic results.

`ScheduleFormState.toDescription()` uses these functions and also applies day-set grouping: all seven days → "Every Day", Mon–Fri → "Weekdays", Sat–Sun → "Weekends", otherwise individual days joined with short abbreviations (M, Tu, W, Th, F, Sa, Su).

---

## Data Model

### Nudge
The top-level user-created item. Contains one main `Question` and zero or more follow-up `Question`s.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| name | String | Derived from main question text by default |
| isEnabled | Boolean | Disabled nudges do not fire notifications |
| createdAt | Instant | |
| updatedAt | Instant | |

### Question
Belongs to a `Nudge`. The first question (orderIndex 0) is the main question; subsequent questions are follow-ups.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| nudgeId | UUID | FK → Nudge |
| text | String | |
| type | QuestionType | See below |
| orderIndex | Int | 0 = main question |
| triggerAnswerValue | String? | Null for main question; defines which answer on the parent question triggers this follow-up |
| triggerOperator | TriggerOperator? | EQ, GTE, LTE, etc. Allows range-based follow-up triggers (e.g. score ≥ 7) |
| scaleMin | Int? | Null unless `type = SCALE`; lower bound of the slider range |
| scaleMax | Int? | Null unless `type = SCALE`; upper bound of the slider range |

**QuestionType** enum: `YES_NO`, `SCALE`, `NUMBER`, `OPTION_SINGLE`, `OPTION_MULTI`, `TEXT`
`SCALE` is a bounded integer range with configurable `scaleMin` and `scaleMax` (defaults 0–10). `TEXT` is only valid for follow-up questions (`isValidForMainQuestion` is false for `TEXT`).

**TriggerOperator** enum: `EQ`, `GT`, `GTE`, `LT`, `LTE`, `CONTAINS`
`CONTAINS` is used for `OPTION_MULTI` follow-ups: the trigger fires when the stored option ID appears anywhere in the comma-separated multi-select answer string.

### QuestionOption
Selectable answer choices for `OPTION_SINGLE` and `OPTION_MULTI` questions. Up to 16 per question.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| questionId | UUID | FK → Question |
| text | String | |
| orderIndex | Int | |

### Schedule
One schedule per `Nudge`.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| nudgeId | UUID | FK → Nudge |
| type | ScheduleType | `DAILY`, `WEEKLY`, `MONTHLY`, `HOURLY` |
| timeOfDay | LocalTime | Local time; follows device timezone including travel |
| activeDaysOfWeek | Set\<DayOfWeek\>? | Used by `DAILY` and `HOURLY` |
| dayOfMonth | Int? | Used by `MONTHLY` |
| activeHours | Set\<Int\>? | Hours of day (0–23) used by `HOURLY` |

### Answer
One row per question answered per notification event.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| nudgeId | UUID | FK → Nudge |
| questionId | UUID | FK → Question |
| value | String | Serialized answer value; format depends on QuestionType |
| scheduledAt | Instant | The nudge's intended fire time; used to anchor the data point to the correct day in visualizations |
| answeredAt | Instant | When the user actually submitted the answer; kept for transparency and auditing |
| isHidden | Boolean | Hidden rows excluded from visualizations |

### NotificationFire
Written by `NudgeNotificationWorker` at the moment a notification is actually delivered (not the scheduled time). Used to determine whether a notification has been sent but not yet answered, driving the missed-nudge indicator in the UI.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| nudgeId | UUID | FK → Nudge (cascade delete) |
| firedAt | Instant | `Clock.System.now()` at delivery, not the scheduled time |

### TimezoneChangeEvent
Audit record written by `RescheduleAllNudgesWorker` each time the device timezone changes. Provides a history of when changes occurred and between which zones, useful for debugging unexpected notification timing.

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| changedAt | Instant | Wall-clock time at which the change was processed |
| fromTimezone | String | IANA timezone ID before the change (e.g. `America/New_York`) |
| toTimezone | String | IANA timezone ID after the change |

The previous timezone is tracked in `SharedPreferences` (file `nudgery_system`, key `last_known_timezone`) because `ACTION_TIMEZONE_CHANGED` arrives after the system has already applied the new timezone, making `TimeZone.currentSystemDefault()` return the new value. `NudgeryApplication` seeds this key on first launch.

### NudgeEdit
Audit record written when a nudge's question or option text is edited in-place (non-split).

| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| nudgeId | UUID | FK → Nudge |
| editedAt | Instant | |
| fieldChanged | String | Which field was changed |
| previousValue | String | Value before the edit |

---

## Notification Scheduling

The `NotificationScheduler` interface is defined in `shared/commonMain`:

```kotlin
interface NotificationScheduler {
    fun schedule(nudge: Nudge, schedule: Schedule)
    fun cancel(nudgeId: String)
    fun reschedule(nudge: Nudge, schedule: Schedule)
}
```

**Android:** Implemented in `shared/androidMain` using a two-layer approach — `AlarmManager` for precise timing, WorkManager for reliable execution.

`WorkManagerNotificationScheduler` uses `AlarmManager.setExactAndAllowWhileIdle()` to schedule each nudge at its exact fire time (`RTC_WAKEUP`, so it wakes the device). If the exact alarm permission is not held (see *Exact Alarm Permission Strategy* below), it falls back gracefully to `setAndAllowWhileIdle()` (inexact). Alarms are deduplicated by request code (`nudgeId.hashCode()`) with `FLAG_UPDATE_CURRENT`, so `reschedule()` atomically replaces any existing alarm.

When an alarm fires, `NudgeAlarmReceiver` (a `BroadcastReceiver`) receives it and immediately enqueues a `NudgeNotificationWorker` job via WorkManager with no delay. This keeps exact timing from AlarmManager while retaining WorkManager's execution guarantees (Doze-aware, survives process death). The worker shows the notification and calls `notificationScheduler.reschedule()` to set the next alarm.

`RescheduleAllNudgesWorker` is triggered on boot (`BootReceiver`) and timezone change (`TimezoneChangeReceiver`). The boot path sets a `catch_up_missed=true` input flag; the timezone-change path does not.

When `catch_up_missed` is true, the worker runs `CatchUpMissedFiresUseCase` for each enabled nudge before rescheduling. The use case walks forward from the nudge's last `NotificationFire.firedAt` (falling back to `nudge.createdAt` if no fires have ever been recorded), advancing one fire time at a time until it reaches a time that is in the future. If any past fire times are found, the most recent one is returned as a missed fire; all older ones are silently abandoned. When a miss is detected, the worker enqueues an immediate `NudgeNotificationWorker` for that nudge (which shows the notification, records the fire, and reschedules the next alarm) instead of calling `schedule()` directly. If no miss is detected, `schedule()` is called as normal.

Both the boot path and the **timezone-change path** pass `catch_up_missed=true`. This means that when the clock jumps forward (traveling east), a nudge whose scheduled time has already passed in the new timezone is caught up immediately — the user is still asked. When the clock jumps back (traveling west), `CatchUpMissedFiresUseCase` sees that the next scheduled fire in the new timezone is still in the future and returns `ScheduleNext`, so the alarm fires at the correct local time and the user receives a second nudge for that day, which is the preferred behavior.

When the timezone changes, `TimezoneChangeReceiver` reads the previous timezone from `SharedPreferences` (key `last_known_timezone`, file `nudgery_system`; seeded at first launch by `NudgeryApplication`), writes the new timezone, and passes both in the worker input data (`timezone_from`, `timezone_to`). The worker records a `TimezoneChangeEvent` when `from ≠ to`, creating an audit trail of when timezone changes occurred and between which zones.

The notification's launch `Intent` carries `EXTRA_NUDGE_ID` and `EXTRA_SCHEDULED_AT` (epoch milliseconds). `MainActivity` is declared `singleTop` and handles both cold-start taps (`onCreate`) and warm taps (`onNewIntent`) via `handleNudgeIntent()`, which routes to `NudgeListViewModel.handleNotificationIntent(nudgeId, scheduledAt)`. The scheduled time travels through the nav route as a Long argument and is reconstructed as `Instant` before being passed to `AnswerFormViewModel`, ensuring answers record the nudge's fire time rather than the wall-clock time of the tap.

**iOS (future):** Will be implemented using `UNUserNotificationCenter`. The interface contract is identical, so the shared business logic requires no changes.

### Exact Alarm Permission Strategy

Android's exact alarm permission system differs by API level. Nudgery targets each level differently.

**API < 31 (Android 11 and below):** No permission required. Exact alarms are always permitted. No action needed.

**API 31–32 (Android 12):** `SCHEDULE_EXACT_ALARM` — a user-controlled special app access permission. It cannot be requested via `requestPermissions()`; the only path is firing `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, which opens the system settings page for the app directly. The user must toggle it on manually. This applies in both testing and production.

**API 33+ (Android 13+):** Two mechanisms are available:

- `USE_EXACT_ALARM` — declared in `AndroidManifest.xml`; **automatically granted by the Play Store** for approved app categories (alarms, reminders, calendars). Nudgery is a scheduling/reminder app and is assumed to qualify. No user action is required. **This does not apply to sideloaded builds** — the auto-grant only happens through Play Store distribution.
- `SCHEDULE_EXACT_ALARM` — the user-controlled fallback, used when `USE_EXACT_ALARM` is not in effect (i.e. during pre-release testing via sideloaded APKs).

**How permission is obtained, by context:**

| Context | API level | Mechanism | User action required? |
|---|---|---|---|
| Play Store install | < 31 | None needed | No |
| Play Store install | 31–32 | `SCHEDULE_EXACT_ALARM` first-launch prompt | Yes — one-time system settings toggle |
| Play Store install | 33+ | `USE_EXACT_ALARM` auto-granted | No |
| Sideloaded (testing) | < 31 | None needed | No |
| Sideloaded (testing) | 31–32 | `SCHEDULE_EXACT_ALARM` first-launch prompt | Yes |
| Sideloaded (testing) | 33+ | `SCHEDULE_EXACT_ALARM` first-launch prompt | Yes |

The first-launch prompt (shown on API 31+ when the permission is not already granted) is a dialog explaining that on-time notifications require the permission, with a single "Open Settings" button that fires `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. This prompt exists primarily for pre-release testers and Android 12 production users; the majority of production users on Android 13+ will never see it.

**Fallback behavior (permission absent or revoked):**

`WorkManagerNotificationScheduler` already falls back from `setExactAndAllowWhileIdle()` to `setAndAllowWhileIdle()` when the exact alarm permission is not held. With inexact scheduling, the OS delivers notifications within a batch window — typically a few minutes late, but potentially longer under Doze mode. No nudge data is lost; notifications simply become less punctual until the permission is re-granted.

The Settings screen includes a diagnostic row showing the current exact alarm grant status. If not granted, it provides a button to navigate directly to the system settings page to re-grant it. This row is always visible and is the recovery path for any user who missed the first-launch prompt or later revoked the permission.

---

## UI and Navigation

Navigation uses Jetpack Navigation Compose with a single `NavHost` in `MainActivity`. There is no bottom navigation bar; the stack is a single linear back-stack with `popBackStack()` for dismissal. All routes and argument constants are defined in `ui/nav/NudgeryNavGraph.kt` — `NudgeryScreen` sealed class for routes, `ARG_NUDGE_ID`, `ARG_SCHEDULED_AT`, and `ARG_INITIAL_STEP` for nav arguments.

The `NavHost` is given `Modifier.background(MaterialTheme.colorScheme.background)` so that crossfade transitions blend through the correct theme color rather than the window background. Transition duration is controlled by `NAV_TRANSITION_DURATION_MS` (490ms) applied to all four transition lambdas (`enterTransition`, `exitTransition`, `popEnterTransition`, `popExitTransition`).

`ARG_INITIAL_STEP` is an optional integer query parameter on the `EditNudge` route (default 0). It is read by `EditNudgeScreen` to seed `currentStep`, allowing three distinct entry points from `NudgeDetailScreen`:

| Entry point | `initialStep` | Step opened |
|---|---|---|
| Pencil icon (top bar) | 0 | Question and name |
| Follow-up questions icon | 1 | Follow-up questions |
| Calendar icon | 2 | Schedule |

### Theme

`NudgeryTheme` wraps `MaterialTheme` with a custom color scheme, typography, and shapes. The user's `ThemePreference` (System/Light/Dark) and bold text toggle are stored in `AppSettings` via DataStore and applied at the `NudgeryTheme` call site in `MainActivity`.

- **Colors**: violet primary, deep teal secondary, golden yellow tertiary. Separate dark and light `ColorScheme` instances; WCAG AA contrast verified on all text/background combinations.
- **Typography**: Atkinson Hyperlegible Next, bundled as static TTF files in `res/font/` (all 14 weight/style variants). `nudgeryTypography(bold: Boolean)` steps body weight from Normal→Medium and label weight from SemiBold→Bold when the bold toggle is on.
- **Shapes**: `nudgeryShapes` — extraSmall 4dp through extraLarge 50dp (pill).

### Screens

| Screen | Entry points |
|---|---|
| `NudgeListScreen` | App launch, back-stack root |
| `CreateNudgeScreen` | FAB on NudgeList |
| `NudgeDetailScreen` | Nudge row tap on NudgeList |
| `EditNudgeScreen` | Pencil icon on NudgeDetail top bar (step 0 — question/name); follow-up icon on NudgeDetail (step 1 — follow-ups); calendar icon on NudgeDetail (step 2 — schedule) |
| `AnswerFormScreen` | "Answer Now" on NudgeDetail; notification tap |
| `SettingsScreen` | Settings icon on NudgeList |
| `AboutScreen` | About link on Settings |

---

## Data Export

CSV, TSV, and JSON backup export logic lives in `shared/commonMain` so it is available to both platforms without duplication. CSV and TSV export produces one row per `Answer`, joined with its `Question`, `Nudge`, and any relevant `QuestionOption` text. JSON backup (`ExportFormat.JSON_BACKUP`) serializes the complete nudge — questions, options, schedule, and all answer history — for round-trip import via `ImportNudgeUseCase`.

---

## Visualizations

Charts are rendered in the platform UI layer (not shared), since charting libraries are platform-specific. The shared module exposes pre-aggregated data structures computed from raw `Answer` rows via `GetVisualizationDataUseCase`; the platform layer only handles rendering. Data types: `List<DataPoint>` (Instant + Double) for time-series, `List<NamedCount>` (label + count) for categorical, `List<DailyCount>` (LocalDate + Double) for heat maps.

**Available chart types by QuestionType:**

| QuestionType | Available Visualizations |
|---|---|
| YES_NO | Calendar heat map, line graph (daily yes count), column chart |
| SCALE | Line graph, calendar heat map (daily average) |
| NUMBER | Line graph, calendar heat map (daily average) |
| OPTION_SINGLE | Bar chart, column chart, packed bubble chart |
| OPTION_MULTI | Bar chart, packed bubble chart |

**Android rendering:**

All chart composables live in `NudgeDetailScreen.kt` (private). The dispatch is in `NudgeryChart`, which switches on the `VisualizationData` sealed subtype.

| Chart type | Composable | Rendering |
|---|---|---|
| `LineGraph` | `LineGraphChart` | Vico `CartesianChartHost` + `LineCartesianLayer`; x-axis labels formatted as `month/day` from `DataPoint.at` |
| `BarChart` | `HorizontalBarChart` | Custom Compose `Column`/`Row` layout; proportional `Box` fills with `primary` color; label truncated to 80dp |
| `ColumnChart` | `NamedCountChart` | Vico `CartesianChartHost` + `ColumnCartesianLayer`; x-axis labels from `NamedCount.label` |
| `PackedBubble` | `PackedBubbleChart` | Custom Canvas with d3-style front-chain circle packing; radius ∝ `sqrt(NamedCount.count)` (area encodes frequency); cluster scaled to fit; bold centered word with count beneath |
| `CalendarHeatMap` | `CalendarHeatMapChart` | Custom Canvas grid; Monday-anchored week columns × 7 rows; cell color interpolated from `surfaceVariant` → `primary` by intensity |

`CartesianChartModelProducer` is created with `remember` and updated via `LaunchedEffect` on data change. M3 color theming is applied automatically by `vico-compose-m3`.

---

## Security and Privacy

- All data is stored locally on-device. No network calls are made and no data leaves the device except via explicit user-initiated export.
- Exported files are written to the user's chosen location via the platform file picker (no background file access).
- No analytics, telemetry, or advertising SDKs are included.

---

## iOS Readiness Notes

The following decisions were made specifically to keep iOS support achievable without major rework:

1. **SQLDelight over Room** — Room is Android-only. SQLDelight generates platform-native drivers for both Android and iOS from the same schema.
2. **Koin over Hilt** — Hilt is Android-only. Koin supports KMP and can be initialized from both Android and iOS entry points.
3. **kotlinx.datetime over java.time** — Avoids Android API level constraints and is usable from iOS via KMP.
4. **`NotificationScheduler` interface** — Decouples scheduling logic from WorkManager so an iOS implementation can be dropped in.
5. **ViewModel logic in platform modules** — Shared use cases are plain Kotlin classes; Android ViewModels wrap them. On iOS, the same use cases can be wrapped in an equivalent observable pattern (e.g. `ObservableObject`).

When iOS work begins, the primary tasks will be: adding the `iosApp` Xcode target, writing iOS `actual` implementations for platform interfaces, and building the SwiftUI layer against the already-tested shared core.
