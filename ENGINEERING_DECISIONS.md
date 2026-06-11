# Engineering Decisions

Binding engineering decisions that the implementation **must** adhere to, but which end
users never interact with and which are not visual/UX design. This is the engineering
counterpart to `README.md`: where `README.md` is the prescriptive contract for *user-observable
behavior*, this file is the prescriptive contract for the *system's internals*.

## How this file relates to the others

| Doc | Role | Prescriptive or descriptive? | Test-backed? |
| --- | --- | --- | --- |
| `README.md` | What users experience | Prescriptive (for users) | Yes — `TDD_` tests cite it |
| `ENGINEERING_DECISIONS.md` | Binding non-visible engineering decisions | Prescriptive (for internals) | Yes — `TDD_` tests cite it |
| `DESIGN.md` | Visual / UX design brief (look, feel, layout, motion) | Prescriptive (for design) | Where practical |
| `ARCHITECTURE.md` | How today's code is actually built | **Descriptive** | No obligation |

The boundary that keeps this file from overlapping `ARCHITECTURE.md`: if a statement is a
*requirement we are committing to*, it belongs here (with a test). If it is an *explanation of
how the current code happens to work*, it belongs in `ARCHITECTURE.md` and is free to change as
the code changes.

## Conventions

- **Every decision here must be backed by at least one automated test.** Reuse the `TDD_` prefix
  (these are documentation-driven tests like the README ones); in the test comment, cite the
  decision by its ID (e.g. `// ENGINEERING_DECISIONS.md ED-3`) instead of a README line.
- Decisions are **living and normative** — unlike classic immutable ADRs, an entry here is in
  force as long as it is listed. When a decision is reversed, update or remove the entry (and its
  tests) rather than appending a contradicting one.
- Each entry follows **Context → Decision → Consequences**, plus a **Tests** line and a **Status**.
- IDs (`ED-N`) are stable; do not renumber. Retired decisions are deleted outright (git holds the
  history), so a missing number is fine.

### Entry template

```
### ED-N: <short title>
**Status:** Accepted | Implemented | Superseded
**Context:** Why this decision is needed.
**Decision:** The rule the implementation must follow.
**Consequences:** What this enables, costs, or forecloses.
**Tests:** Which TDD_ test(s) enforce it (or "pending — see TODO.md").
```

---

## Emoji input

These decisions govern the `EMOJI` question type. The shared layer and the Android picker are
implemented (see each entry's **Status**); iOS (ED-9 region/ED-13) and the emoji-scale setting
(ED-14) remain. See `TODO.md` for outstanding work.

### ED-1: `EMOJI` is a TEXT question under the hood
**Status:** Implemented — `QuestionType.EMOJI` routes to the TEXT path (storage/export/charts/validation), round-trips through backup, and is selectable in the create/edit wizard.
**Context:** Users want an emoji-only answer type, but emoji answers should reuse the existing
text storage, export, and packed-bubble visualization (which already tokenizes emoji).
**Decision:** `QuestionType.EMOJI` persists, exports, and charts identically to `TEXT`. It differs
only in input restriction (ED-2). Code that branches on `QuestionType` routes `EMOJI` to the
`TEXT` path unless input handling is specifically concerned.
**Consequences:** No new storage/export/chart paths. The exhaustive `when (questionType)` sites
(~10 files) must each handle `EMOJI`, almost always by delegating to `TEXT`. Backup export/import
must round-trip the new type.
**Tests:** pending — see TODO.md.

### ED-2: Emoji answers are validated emoji-only, not merely hinted
**Status:** Implemented — `util.sanitizeToEmoji` / `util.isEmojiOnly`; the picker enforces emoji-only by construction (taps insert catalog emoji), with the validators available as the save-time guard.
**Context:** No Android/iOS API can restrict the system keyboard to emoji, so input correctness
cannot rely on the keyboard. Users may also paste, dictate, or use a hardware keyboard.
**Decision:** An emoji-only validator (promoted from the existing `extractEmojiWords` /
`isSingleEmoji` helpers into shared `util`) filters input live and guards the value at save time,
so a stored `EMOJI` answer contains only emoji.
**Consequences:** Emoji correctness is a storage invariant independent of input surface. The
shared validator is reused by both platforms.
**Tests:** pending — see TODO.md (cover paste, mixed text+emoji, garbage, empty).

### ED-3: Never ship emoji glyphs; render with the device font
**Status:** Implemented — picker/answers/charts render emoji from the device font; no glyph assets shipped
**Context:** Bundling emoji artwork would create a perpetual update treadmill and bloat.
**Decision:** Emoji are always rendered using the device's own emoji font. We ship no emoji images.
**Consequences:** New emoji artwork is the OS vendor's responsibility. We store/display code
points only.
**Tests:** pending — see TODO.md.

### ED-4: Emoji availability is per-device, never the cross-platform intersection
**Status:** Implemented — `PlatformEmojiGlyphFilter` filters the picker grid to what the device can render
**Context:** Different OS versions render different emoji sets; showing an emoji the device can't
draw yields tofu (□), and limiting to the iOS∩Android intersection would needlessly hide emoji.
**Decision:** The picker filters its displayed set at runtime by what the *current device* can
render — `PaintCompat.hasGlyph()` on Android, the glyph-availability equivalent on iOS. Each
platform shows everything it natively supports.
**Consequences:** No tofu is ever shown. Effective freshness tracks the device, so a late catalog
update only briefly hides the newest emoji on the newest OS. Availability is not capped to a
shared subset.
**Tests:** pending — see TODO.md.

### ED-5: The emoji catalog is generated from Unicode data, not hand-curated
**Status:** Implemented — `buildSrc` parser/generator + `:shared:generateEmojiCatalog` (base catalog; CLDR keywords pending in ED-10)
**Context:** Maintaining an emoji list by hand would be error-prone and would drift from Unicode.
**Decision:** A generator parses Unicode's canonical, pre-categorized `emoji-test.txt` into a
shared categorized list in the KMP module; skin-tone and ZWJ variants are produced by rule. It is
regenerated once per year when Unicode publishes (September), in the same spirit as
`generateCredits`. The single shared list feeds both the Android and iOS pickers.

**Artifact shape — generated Kotlin source, compiled in (not a bundled resource).** A build-time
Gradle task emits the catalog as Kotlin source into a generated `commonMain` source set, compiled
into the app (generated-on-build, not committed — the vendored `emoji-test.txt` is the source of
truth in VCS, so the annual diff is just that file). The parser itself is build-time-only and lives
in `buildSrc` (so it isn't shipped at runtime). This was chosen over **(b) a bundled parsed
resource** because KMP has no common resource-loading API — (b) would force per-platform loaders or
a new dependency, which fights ED-elsewhere goals — and over naive single-initializer codegen,
which would blow the JVM 64 KB per-method limit (the generator therefore **chunks** the
initializers). If the generated size becomes uncomfortable after keyword data lands ([[ED-10]]),
the fallback is **(c) a generated Kotlin file holding a packed string + a tiny parser** — same
`commonMain`, no resource API. This switch is cheap and contained: all consumers (search, defaults,
UI) depend on the in-memory catalog *model*, not on how it was produced, so the storage shape is
swappable without downstream blast radius. (Option (b)'s per-platform loaders are the one thing
that would be expensive to change later, which is why it's ruled out now.)
**Consequences:** ~once-a-year scripted regeneration; no hand curation. One update covers both
platforms. Generated source is not committed; CI/build runs the generator before compile.
**Tests:** parser correctness is unit-tested in `buildSrc`; the generator asserts catalog-shape
sanity (counts, known emoji present) at build time; a `commonTest` checks the generated catalog is
present and well-formed.

### ED-6: Default skin tone is an app setting we apply via the Unicode modifier
**Status:** Implemented — `emoji.EmojiDefaults.applySkinTone`/`apply`; default persisted in `AppSettings` with a Settings swatch selector; applied on pick by the picker (task #8)
**Context:** The OS does not expose the user's keyboard skin-tone preference, and a self-rendered
picker must decide a default tone.
**Decision:** A user setting selects a default skin tone, applied by appending the Unicode
skin-tone modifier (U+1F3FB–U+1F3FF). It applies only to emoji that arrive *without* a tone (never
overriding a tone the user explicitly chose) and correctly handles VS16 supersession and
multi-person ZWJ sequences.
**Consequences:** We own a small, stable modifier-base table (which emoji accept skin tones).
Explicit user choices are respected.
**Tests:** pending — see TODO.md (un-toned default applied; explicit tone preserved; VS16/ZWJ cases).

### ED-7: Default gender is an app setting, applied only to neutral person forms
**Status:** Implemented — `emoji.EmojiDefaults.applyGender`/`apply` (catalog-derived map; older-adult/child figures unmapped in v1); default persisted in `AppSettings` with a Settings swatch selector; applied on pick by the picker (task #8)
**Context:** Users often use a person emoji to represent themselves, so a preferred gender is worth
defaulting. Unlike skin tone (ED-6), gender is not a single appended modifier — it is encoded as
distinct base characters (🧑/👨/👩) or ZWJ sequences with ♀/♂ — and reaches a substantial set:
the spike measured **~74 man/woman-paired concepts** in emoji-test.txt v16.0 (roles, gestures,
activities/sports, fantasy beings), plus a handful of inherently-gendered concepts that don't use
the "man/woman X" naming (e.g. prince/princess, pregnant woman, Mrs. Claus). (Initial estimate was
~90–110; the measurement revised it down.)
**Decision:** A user setting selects a default gender (neutral / woman / man). It is applied by
substituting the gendered form **only** for emoji the user picks in *neutral/person* form that have
gendered variants; it **never** overrides an explicitly gendered pick (tapping 👩 keeps 👩) — the
same "convert the default, respect the explicit" rule as [[ED-6]]. The neutral↔woman↔man mapping is
*derived* from the generated catalog (ED-5) by relating each gendered sequence to its neutral base,
not hand-curated. Gender and skin-tone defaults compose (neutral person → woman → woman + tone).
In the **picker grid**, a genderable concept appears as a *single* cell: its neutral entry, shown in
the user's default gender. The concept's explicit woman/man catalog entries (ED-5 stores each as its
own entry) are folded out of the grid via `EmojiDefaults.foldedGenderVariantEmoji` and reached only
through the long-press variant tray (ED-8) — otherwise the default-gender form would appear twice
(once as the defaulted neutral cell, once as its own gendered cell). Folding is computed from the
same device-filtered (ED-4) entries the grid shows, so a group collapses only when its neutral form
is present to represent it.
**Consequences:** Real reach (~100 concepts) for the self-representation case. Requires the derived
mapping and care around ZWJ structure. **Reversible:** stored answers are plain emoji strings, so
the setting and mapping can be removed later (if mis-mappings prove grating) without migrating data.
**Tests:** pending — see TODO.md (default applied to neutral base; explicit gender preserved;
gender+tone composition; non-gendered emoji untouched). `EmojiDefaultsTest` — gendered forms with a
neutral sibling are folded out of the grid; the neutral and ungendered forms are not; no fold when
the neutral form is absent.

### ED-8: Variant axes other than skin tone and gender are pick-time only
**Status:** Implemented (skin tone + gender) — long-press variant tray via `EmojiDefaults.variants`; hair/direction variants deferred
**Context:** Hair components (red/curly/white/bald) attach only to the 3 bare adult figures
(person/man/woman) and never to role emoji; direction (facing left/right) reaches only a handful of
emoji. A global default for either would touch almost nothing.
**Decision:** Hair and direction are offered as per-emoji variant selection in the picker (e.g.
long-press / variant strip), **not** as global default settings. (Revisit only if Unicode greatly
expands where these apply.) A grid cell whose emoji has selectable variants (skin tone and/or gender)
is marked with a small bottom-corner triangle, so the long-press tray is discoverable rather than
hidden.
**Consequences:** Settings stay minimal; no hair/direction default tables to maintain. Power users
still reach these variants at pick time.
**Tests:** pending — see TODO.md (picker surfaces hair/direction variants where they exist).

### ED-9: Emoji answers are normalized to emoji presentation (VS16)
**Status:** Implemented (shared logic) — `emoji.normalizeEmojiPresentation` snaps each emoji to the catalog's fully-qualified form; UI wiring lands with the picker (task #8)
**Context:** Dual-use symbols can render as a monochrome *text* glyph (VS15, U+FE0E) or a color
*emoji* glyph (VS16, U+FE0F). In an emoji-only field the color form is always intended.
**Decision:** When storing/displaying an `EMOJI` answer, the validator/normalizer ensures emoji
presentation (apply VS16 where applicable) so e.g. `❤` is normalized to `❤️`. This is an internal
rule, not a user setting.
**Consequences:** Consistent color rendering of dual-use symbols; no monochrome surprises.
**Tests:** pending — see TODO.md (VS15→VS16 normalization; already-VS16 unchanged; non-dual-use
emoji untouched).

### ED-10: Emoji search keywords come from CLDR annotations
**Status:** Implemented — `CldrAnnotationParser` + generator; 100% keyword coverage on v16.0 / CLDR 46 (English)
**Context:** Typeahead search must surface emoji by the words a user types. `emoji-test.txt` (ED-5)
carries only the CLDR short name (😂 = "face with tears of joy"), so searching it alone misses the
synonyms people actually type ("lol", "crying laughing"). Unicode CLDR annotations
(`common/annotations/<locale>.xml`) are the canonical open-standard keyword lists (😂 →
`face | joy | laugh | tear | tears of joy`) and are localized, making them the path to translated
search later.
**Decision:** The catalog generator (ED-5) also parses CLDR annotations and attaches a keyword list
(plus the short name) to each catalog entry. **English-only initially**; additional locales are a
later i18n add-on. The half-measure of searching short names only is rejected.
**Consequences:** The generator parses a second Unicode data file; the shared catalog grows a
keyword field. Localization is unlocked by adding locales later.
**Tests:** pending — see TODO.md (generator attaches keywords; known emoji resolve expected
keywords).

### ED-11: Emoji search is shared keyword/prefix matching (no fuzzy/semantic in v1)
**Status:** Implemented (shared logic) — `emoji.EmojiSearch.search`; renderability/defaults applied by the caller (picker, task #8)
**Context:** Search should feel responsive and "related to the word," but a local, battery-conscious
app should not ship an ML/embedding model, and CLDR keywords already encode the synonyms users type.
**Decision:** Search is an in-memory prefix match of the typed token(s) against each emoji's name +
keywords, implemented as **pure shared Kotlin** (reused by both platforms; only the search field is
per-platform). Ranking: exact name > name prefix > keyword exact > keyword prefix. Multi-word
queries AND their tokens. Results must pass the device-renderability filter ([[ED-4]]) and a picked
result still flows through the skin-tone/gender defaults ([[ED-6]], [[ED-7]]). **v1 explicitly
excludes** fuzzy/typo tolerance and semantic relatedness — "related" means curated CLDR keyword
synonyms.
**Consequences:** Fast over a few thousand entries with no search index. Highly testable. Typo
tolerance is a possible later enhancement.
**Tests:** pending — see TODO.md (prefix matches on name and keyword; ranking order; multi-word AND;
unrenderable matches excluded).

### ED-12: Embedded CLDR-derived keyword data is credited manually
**Status:** Implemented — `config/libraries/unicode-emoji-data.json` + `config/licenses/unicode-3.0.json`; renders in CREDITS.md & the in-app licenses screen
**Context:** CLDR is a Unicode-licensed data source, not a Maven dependency, so the automatic
credits pipeline (AboutLibraries → `CREDITS.md`) does not cover it.
**Decision:** The CLDR-derived keyword data (and the Unicode emoji data of ED-5) are credited via a
**manual** entry under `androidApp/config/libraries/` + `androidApp/config/licenses/`, the same way
AGENTS.md handles bundled non-Maven assets like fonts.
**Consequences:** Attribution stays accurate; one manual credit entry to add when the data is
embedded and to revisit if the data source changes.
**Tests:** n/a (attribution/config, verified by the credits process, not a runtime behavior).

### ED-13: Emoji answers are entered via a conventional in-app picker, not the system keyboard
**Status:** Implemented — inline always-open `EmojiPicker` in the answer form (search, top tabs, recents, grid); EMOJI selectable in the wizard
**Context:** No platform can restrict the system keyboard to emoji (the original reason for rolling
our own picker). The picker should feel like the emoji pickers users already know, so it needs no
user-facing explanation.
**Decision:** `EMOJI` answers are entered through our in-app emoji picker, which is **embedded
inline and always-open** on the answer screen (not a pop-up/bottom sheet). An emoji answer is **one
or more emoji** (tapping appends; a Done/backspace manage the string); the read-only answer display
never raises the system keyboard. The picker's **search field is the only element that uses the
system keyboard** (for typeahead) — embedding the picker inline is deliberate so the keyboard rising
for search resizes the page normally instead of fighting a sheet. The picker mirrors standard
platform conventions — typeahead search ([[ED-11]]), a recents category, category navigation, and
long-press variant selection ([[ED-8]]) with applied defaults ([[ED-6]], [[ED-7]]). Because it
follows conventions users already know, the picker's mechanics are intentionally **not** documented
in the user-facing README; visual specifics live in DESIGN.md ("Emoji Picker").
**Consequences:** No sheet-vs-keyboard conflict; predictable fixed layout. Conventions (not vendor
assets/branding) are copied, so there is no IP exposure — emoji render from the device font (ED-3)
and icons are generic Material Symbols.
**Tests:** UX/scope decision — constituent behaviors are covered by [[ED-6]]…[[ED-11]]; the
no-system-keyboard-for-answer-input behavior is verified at the UI layer when the picker lands.

### ED-14: A global emoji-scale setting applies to emoji-only strings, by content
**Status:** Implemented — `AppSettings.emojiScale` + Settings slider (live sample), applied via `LocalEmojiScale`
**Context:** Users often want to see emoji larger (a real gap in most apps; also an accessibility
win). Because emoji render from the device font (ED-3), scaling is just a font-size multiplier.
**Decision:** A single app setting controls emoji size, applied **by content, not by surface**: any
displayed string that is **emoji-only** (no mixed text — `util.isEmojiOnly`) is scaled, wherever it
appears — emoji-only nudge names (including on the dense main list), emoji-only question text and
follow-ups, emoji-only answers in the raw-data table, the picker grid, and the EMOJI-answer display.
Strings that **mix** emoji and text ride the surrounding text size and are unaffected (selectively
enlarging emoji *within* a text run would need inline sizing spans and could break layout). Applied
via a `LocalEmojiScale` CompositionLocal and an `emojiScaledStyle(text, base)` helper that scales a
text style only when the text is emoji-only.

**The scale is a size *floor*, not a fixed size** — users pick the smallest size they're willing to
decipher. So the packed-bubble chart treats it as a minimum: in the **full-screen** chart, emoji are
at least the floor (and may go larger by frequency); the small **thumbnail** chart may render emoji
*below* the floor (it's a preview). The Settings control previews the scale on a sample emoji
(random from a small wild-animal shortlist).
**Consequences:** A new persisted setting (`AppSettings`). Emoji-only nudge names can enlarge list
rows at high scale (intended). The bubble chart's frequency encoding is preserved, just floored in
full screen.
**Tests:** pending — emoji-only strings scale; mixed text+emoji strings do not.

### ED-15: A top app bar grows to fit an emoji-only, emoji-scaled title
**Status:** Implemented — `theme.emojiScaledAppBarHeight(...)` feeds `TopAppBar(expandedHeight = …)` on the nudge detail screen
**Context:** Material's single-row `TopAppBar` centers its title in a fixed-height container and
clips to bounds. With the emoji-scale setting (ED-14), an emoji-only title scaled toward the high
end (e.g. `titleLarge` 26sp at the 2.5× ceiling → 65sp) is taller than that container and gets
cropped top and bottom.
**Decision:** When (and only when) a bar's title is **emoji-only** (`util.isEmojiOnly`) and the
emoji scale is above 1.0, the bar's `expandedHeight` grows to fit the scaled glyph plus fixed
vertical breathing room, floored at the Material default height; ordinary titles (mixed text, plain
text, or scale 1.0) keep the default height unchanged. The height is computed in a `Density` scope
so it tracks the device font-scale setting, not just the base title size.
**Consequences:** Only emoji-only titles can make the header taller, and only at elevated scale; the
common case is byte-for-byte unchanged. The rule lives in one reusable helper so other bars can
adopt it. Material's `expandedHeight` parameter is used rather than a custom bar, keeping standard
app-bar styling and scroll behavior.
**Tests:** `EmojiScaleTest` — emoji-only scaled title grows past default; mixed/plain/unscaled titles
return the default; growth never drops below the default.

### ED-16: User-typed text is trimmed at the save boundary, never at display
**Status:** Implemented — `request.normalized()` at the top of each create/update/import use case; `RecordAnswerUseCase` trims the answer value
**Context:** Soft keyboards routinely append a trailing space after autocomplete or a tapped
suggestion. Saved verbatim, that stray whitespace makes an otherwise emoji-only string fail
`util.isEmojiOnly` (so it doesn't scale, ED-14) and generally produces dirty data that every display
site would otherwise have to defend against.
**Decision:** Normalize at the **single save boundary**, not at display: every user-typed text field
is trimmed (both ends) just before it is persisted, so untrimmed text can never enter storage and no
read path needs to compensate. Covered fields: nudge name, question text, question-option text,
follow-up trigger values, and answer values (a no-op for structured or emoji answers). Optional name
fields that are blank once trimmed collapse to "absent" (`null`) rather than an empty string. Each
use case calls `request.normalized()` **before** its change-detection comparisons, so a trailing
space alone is not mistaken for an edit (which would write a spurious `NudgeEdit` and bump
`updatedAt`). Import is treated as a save boundary too; trimming uniformly keeps its option-text-keyed
trigger/answer resolution internally consistent. Internal-only whitespace is preserved.
**Consequences:** One normalization step per save path rather than scattered per-field trimming;
display code can assume stored text is already trimmed. Pre-existing untrimmed rows are cleaned the
next time they're saved (and on re-import), but are not bulk-migrated.
**Tests:** `NudgeCreationTest`, `NudgeEditTest`, `NudgeImportTest`, `AnswerRecordingTest` — names,
question text, options, and answer values are stored trimmed; a trailing-space-only edit is not
recorded as a change.

### ED-17: A Yes/No question can collapse to one Yes/No per calendar day in its charts
**Status:** Implemented — `Question.collapsePerDay` (DB column + migration 2); applied in `GetVisualizationDataUseCase.buildYesNoCharts`; "One Yes Per Day" toggle in the create/edit wizard; round-trips through export/import
**Context:** A daily yes/no — the headache case Nudgery was built for — is conceptually a per-day bit,
but the charts sum every "YES" answer. Re-answering a day (an off-schedule **Answer Now** to flip
No→Yes, or re-answering to correct a follow-up) then double-counts that day. Two semantics are both
valid: an **event tally** (sum each Yes) and a **per-day presence** bit (the day is Yes or No), so
this is an opt-in rather than a behavior change.
**Decision:** A per-Yes/No-question boolean ("One Yes Per Day"), **default off**. When on, every
chart derived from that question aggregates by **calendar day** to a single value before any further
bucketing: a day with ≥1 "YES" answer = one Yes; a day with answers but no Yes = one No; a day with
no answers is absent. It applies **uniformly across all of the question's charts** — calendar heat
map, line graph, and the Yes/No summary (which then reads **"Yes days" vs "No days"**) — so they stay
consistent. Larger heat-map buckets (week/month) sum these day-bits ("Yes days"). The flag is
**independent per question**, set separately on the main question and on each Yes/No follow-up. It is
a **display/aggregation rule only**: raw answers are never merged or deleted, so it is fully
reversible and export keeps every answer.
The collapse period is **hard-coded to the calendar day** regardless of schedule type
(DAILY/HOURLY/WEEKLY/MONTHLY): the day is the chart pipeline's atomic unit (`DailyCount`), it serves
HOURLY ("Yes day" when on vs event sum when off) and DAILY directly, and it matches "sum Yes days"
for larger buckets. A per-schedule-period collapse was rejected — it would need sub-day (hourly) and
period-aware (weekly/monthly) aggregation the pipeline lacks and would double-bucket against the
chart's own day/week/month cells, for marginal benefit (only diverges on the uncommon case of
re-answering a weekly/monthly question on different days within one period); revisitable later
without redoing the per-day work.
**Consequences:** A new boolean column on the question table — an additive migration defaulting
false, so existing questions keep summing. The create/edit wizard gains the toggle for Yes/No
questions (main and follow-up). Export/import carries the flag (absent ⇒ false for older backups).
Aggregation branches on it in `GetVisualizationDataUseCase.buildYesNoCharts` (heat map daily counts,
line points, and the Yes/No `ColumnChart`).
**Tests:** `VisualizationDataTest` (collapse on → one Yes day across heat map/line/summary; Yes+No
day = one Yes day; only-No day = one No day; off → summing preserved; line axis tops at 1 when on),
`NudgeCreationTest` (persists for YES/NO; default off; ignored for non-YES/NO), `NudgeEditTest`
(editing toggles it on an existing main question), `NudgeImportTest` + `DataExportTest` +
`NudgeBackupParserTest` (export/import round-trip; absent ⇒ false).

### ED-18: Recording an answer dismisses that nudge's outstanding alert
**Status:** Implemented — `RecordAnswerUseCase` calls `AlertPresenter.dismissAlert(nudgeId)` after persisting; Android impl `NotificationManagerAlertPresenter` cancels notification id `nudgeNotificationId(nudgeId)`
**Context:** A scheduled nudge posts a system notification (the "alert") that is only auto-cancelled
when tapped (`setAutoCancel(true)`). If the user answers the nudge in-app without opening the alert,
the alert lingers in the shade; tapping it later re-opens the answer form and invites a duplicate
("double") answer.
**Decision:** Recording an answer for a nudge clears that nudge's outstanding alert. Dismissal is
**nudge-level**, not occurrence-level: there is exactly one posted notification per nudge (id
`nudgeNotificationId(nudgeId)`, reused across fires via `FLAG_UPDATE_CURRENT`), and the app cannot
answer for an arbitrary past occurrence, so "any answer for this nudge clears its alert" is
unambiguous and correct. The rule lives in the domain layer (`RecordAnswerUseCase`) so it holds for
every answer entry point, present or future; the platform mechanism (cancelling a displayed
`NotificationManager` notification) is hidden behind the shared `AlertPresenter` interface so
commonMain stays platform-free. It is a pure presentation side effect: no answer or schedule data
changes, and the next scheduled fire is unaffected.
**Consequences:** `RecordAnswerUseCase` gains an `AlertPresenter` dependency. A new commonMain
`AlertPresenter` interface with an androidMain `NotificationManagerAlertPresenter` implementation,
bound in `appModule`. The notification id derivation is centralized in `nudgeNotificationId(nudgeId)`
so the worker (which posts) and the presenter (which dismisses) cannot drift.
**Tests:** `AnswerRecordingTest` (recording an answer dismisses that nudge's alert via a fake
`AlertPresenter`, and the dismissed id is the answered nudge's).

### ED-19: Nudge list order is user-defined and persisted
**Status:** Implemented (ordering foundation) — `Nudge.sortOrder` column (migration 3, backfilled by `createdAt`); `selectAll` orders by it; inserts append; `ReorderNudgesUseCase` rewrites positions in one transaction. Backup/restore order behavior is a separate decision that lands with the export work (full ZIP preserves relative order; single-nudge JSON appends).
**Context:** Nudges were shown in a fixed `createdAt` order with no way to reorder. Users want their
most-used nudges where they want them, reachable by drag-and-drop on the main list.
**Decision:** The list has an explicit, persisted order. A `sortOrder` integer column on `Nudge`
defines it; the list query orders by `sortOrder` (tiebreak `createdAt`). Inserts **append**
(`sortOrder = COALESCE(MAX(sortOrder), -1) + 1`, computed in the insert statement) so creating or
importing a nudge never reshuffles existing ones. Reordering rewrites **all** positions to a dense
`0..n` sequence in a single transaction — the list is a small personal set, so full reassignment is
cheap and avoids fractional-rank bookkeeping. The migration backfills existing rows by their current
`createdAt` order, so upgrading users see no change until they deliberately reorder. Reordering is a
pure ordering concern: no other nudge data (timestamps, answers, schedule) changes.
**Consequences:** Additive schema migration (migration 3) defaulting `sortOrder` to 0, then
backfilling by `createdAt`. `NudgeRepository.reorder(orderedIds)` rewrites positions transactionally.
The insert query auto-assigns the next position, so `CreateNudgeUseCase`/`ImportNudgeUseCase` need no
change to append.
**Tests:** `NudgeReorderTest` (new nudges append in creation order; `ReorderNudgesUseCase` persists
the new order; migration 3 backfills `sortOrder` by `createdAt`).

### ED-20: All answers in one answer-form session share a single scheduledAt
**Status:** Implemented — `AnswerFormViewModel.sessionScheduledAt` (captured once per session)
**Context:** A "response" in the raw-data table is one main answer plus the follow-ups it triggered,
grouped by `scheduledAt`. The answer form computed `scheduledAt.instant ?: Clock.System.now()`
*per question*, so an off-schedule "Answer Now" gave the main answer and its follow-up timestamps a
few seconds apart. They then fell into different `scheduledAt` groups, and the follow-up-only group
(answered slightly later) sorted *above* the main answer — the follow-up appeared detached, wedged
under the table header instead of beneath its answer. Notification answers were unaffected because
their `scheduledAt` is a fixed fire time shared across the session.
**Decision:** Every answer recorded in a single answer-form session (the main question plus any
follow-ups it triggers) shares one `scheduledAt`: the notification's fire time when present, or a
single timestamp captured at the first answer for an off-schedule "Answer Now". The occurrence is the
unit; follow-ups inherit the main answer's time, never their own wall-clock time. This keeps a
response intact under the table's per-`scheduledAt` grouping.
**Tests:** `AnswerFormViewModelTest` (an Answer-Now main + follow-up are recorded with identical
`scheduledAt`; the existing notification-time and current-time-for-single-answer tests still hold).

### ED-21: An untouched follow-up stub is discarded, never kept or saved
**Status:** Implemented — `CreateNudgeViewModel.pruneUntouchedFollowUps` (called on wizard Back/Next
and at the top of `submit`) and `EditNudgeViewModel.pruneUntouchedFollowUps` (called in
`performSubmit`, covering the in-place and split paths).
**Context:** "Add follow-up question" commits a blank default follow-up to the form immediately so
it can be edited inline. In the create wizard the steps are a single index, and the follow-up step
only shows its empty state when the list is empty — so after adding a stub and navigating away
(Back/Next) and back, the user re-entered the *editor* with no obvious way to clear an abandoned
stub, and on save the untouched stub was persisted as a blank follow-up question. The edit screen
shares the same `followUpReplacements` save mapping, so it produced the same blank-save outcome.
**Decision:** A follow-up still equal to a pristine default `QuestionFormState()` is discarded
rather than kept or saved. In the edit flow the rule additionally requires `questionId == null`, so
it only ever drops a newly-added stub and never an existing, stored follow-up. Pruning runs when
leaving the follow-up step (create wizard) and before every submit (create and edit). Explicit
removal of edited follow-ups remains the per-item trash control; anything the user changed (text,
type, trigger, options) is preserved.
**Tests:** `CreateNudgeViewModelTest` (untouched stub pruned; edited follow-up survives; submit
drops an untouched stub) and `EditNudgeViewModelTest` (untouched added stub not persisted on save;
an existing follow-up is untouched by the prune).
