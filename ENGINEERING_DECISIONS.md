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
**Consequences:** Real reach (~100 concepts) for the self-representation case. Requires the derived
mapping and care around ZWJ structure. **Reversible:** stored answers are plain emoji strings, so
the setting and mapping can be removed later (if mis-mappings prove grating) without migrating data.
**Tests:** pending — see TODO.md (default applied to neutral base; explicit gender preserved;
gender+tone composition; non-gendered emoji untouched).

### ED-8: Variant axes other than skin tone and gender are pick-time only
**Status:** Implemented (skin tone + gender) — long-press variant tray via `EmojiDefaults.variants`; hair/direction variants deferred
**Context:** Hair components (red/curly/white/bald) attach only to the 3 bare adult figures
(person/man/woman) and never to role emoji; direction (facing left/right) reaches only a handful of
emoji. A global default for either would touch almost nothing.
**Decision:** Hair and direction are offered as per-emoji variant selection in the picker (e.g.
long-press / variant strip), **not** as global default settings. (Revisit only if Unicode greatly
expands where these apply.)
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
