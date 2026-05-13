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
| Notification scheduling (Android) | WorkManager | Implements shared `NotificationScheduler` interface |
| Notification scheduling (iOS, future) | UNUserNotificationCenter | Will implement the same `NotificationScheduler` interface |
| Charts (Android) | Vico | Compose-native charting library |

---

## Module Structure

```
nudgery/
├── shared/
│   ├── commonMain/          # Platform-agnostic models, repos, use cases, DB schema
│   ├── androidMain/         # Android actual implementations (e.g. notification scheduler)
│   └── iosMain/             # (future) iOS actual implementations
├── androidApp/
│   ├── ui/                  # Jetpack Compose screens and components
│   ├── viewmodel/           # AndroidX ViewModels
│   └── MainActivity.kt
└── iosApp/                  # (future) SwiftUI app target
```

---

## Architecture Pattern

MVVM with a Repository layer.

```
UI (Compose / SwiftUI)
    └── ViewModel
            └── Repository (interface in shared/commonMain)
                    ├── SQLDelight DAOs  (data access)
                    └── NotificationScheduler (interface in shared/commonMain)
```

ViewModels live in the platform app modules (`androidApp`, future `iosApp`). All business logic lives in `shared/commonMain` use cases and repositories, keeping it testable without a device.

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

**QuestionType** enum: `YES_NO`, `NUMBER`, `OPTION_SINGLE`, `OPTION_MULTI`, `TEXT`
`TEXT` is only valid for follow-up questions.

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
| recordedAt | Instant | |
| isHidden | Boolean | Hidden rows excluded from visualizations |

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
    fun cancel(nudgeId: UUID)
    fun reschedule(nudge: Nudge, schedule: Schedule)
}
```

**Android:** Implemented in `shared/androidMain` using WorkManager. Recurring work requests are used for predictable scheduling; the implementation accounts for timezone changes and DST.

**iOS (future):** Will be implemented using `UNUserNotificationCenter`. The interface contract is identical, so the shared business logic requires no changes.

---

## Data Export

CSV and TSV export logic lives in `shared/commonMain` so it is available to both platforms without duplication. Export produces one row per `Answer`, joined with its `Question`, `Nudge`, and any relevant `QuestionOption` text.

---

## Visualizations

Charts are rendered in the platform UI layer (not shared), since charting libraries are platform-specific. The shared module exposes pre-aggregated data structures (e.g. `List<DailyCount>`, `List<WeeklyRollup>`) computed from raw `Answer` rows, so the platform layer only handles rendering.

**Available chart types by QuestionType:**

| QuestionType | Available Visualizations |
|---|---|
| YES_NO | Calendar heat map, column chart |
| NUMBER | Line graph, calendar heat map |
| OPTION_SINGLE | Bar chart, column chart, tag cloud |
| OPTION_MULTI | Bar chart, tag cloud |

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
