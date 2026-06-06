# Nudgery Design Brief

## Overall Philosophy

Nudgery has two distinct modes that call for different design treatment:

- **The nudging flow** (notification → answer form → done) should feel like the OS — fast, unobtrusive, standard Material 3 components. The user is mid-task. Get out of their way.
- **The exploration space** (main list → detail screen) is where the app has its own identity. The user is in a reflective headspace, looking at patterns over time. This can be warmer, more deliberate, and more considered.

## Mood and Tone

**Minimal whimsy.** The whimsy is present throughout but lives in specific, deliberate moments — not wallpapered everywhere. Clean, unfussy structure; personality in the details. Not a health app. Not infantilizing.

Whimsy budget goes toward: empty states, the answer submission moment, the app icon. Data screens stay clean.

## Accessibility

High contrast, larger base font sizes, and strong identifying colors are first-class goals — not accommodations. This makes the app feel confident rather than timid.

## Typography

**Atkinson Hyperlegible Next** — free and open source (designed by the Braille Institute for low-vision readability). Warm but not techy, FOSS-credible, and the reason it exists aligns with why this app was built.

- **Body:** Regular, 18sp
- **Supporting / caption:** Regular, 14sp
- **Screen titles:** Bold, 24–28sp
- **Nudge names and labels:** SemiBold, for hierarchy without going full bold

Override Material 3's default `Typography` object in `Theme.kt` with these values.

## Color

Three-color palette: violet (primary), teal (secondary), golden yellow (accent). Well-separated across the color wheel, each with a distinct role. Avoid using all three at equal weight — assign roles and let violet lead.

- **Violet** — primary brand color, main interactive elements
- **Teal** — secondary UI, data visualization
- **Golden yellow** — highlights, the answer submission moment, app icon; used sparingly for whimsy

Designed dark-first. Dark mode values are lighter and less saturated than "true" values to hit accessible contrast ratios without feeling neon.

### Dark Mode Palette

| Role | Value | Notes |
|---|---|---|
| Background | `#141218` | Near-black with a violet undertone — violet is in the DNA without being loud |
| Surface (cards) | `#1e1b24` | Just enough separation from background |
| Violet primary | `#c8a8f0` | Desaturated, light enough to read on dark surfaces |
| Teal secondary | `#6ec9c0` | Matched lightness to violet so they feel like the same palette |
| Yellow accent | `#ffcc55` | Naturally high contrast on dark; stays close to preferred value |

### Light Mode Palette

| Role | Value | Notes |
|---|---|---|
| Background | `#f5f0ff` | Near-white with faint violet undertone, mirrors dark mode approach |
| Surface (cards) | `#ede8f5` | Slight step darker for card separation |
| Violet primary | `#5b3a8a` | Dark enough for contrast, same family as dark mode value |
| Teal secondary | `#1c7069` | Adjusted from initial `#1f7a72` to clear WCAG AA on card surfaces |
| Yellow accent | `#ffcc55` | Same value as dark mode, but reduced role — icon, illustration, animation only |

### Yellow Usage

Yellow is a true accent in both modes — an identifying mark, not a surface. It does not appear as a filled element. Appropriate uses:

- App icon
- Small decorative elements in empty states (dots, stars, illustration details)
- Answer submission animation
- Thin highlight on selected states (where thick enough to read)

In light mode, yellow steps back further — violet and teal carry the UI, yellow appears only in deliberate whimsy moments.

### Contrast Ratios (WCAG)

All values verified. WCAG AA requires 4.5:1 for normal text, 3:1 for large text and UI components.

**Dark mode** — all combinations exceed AAA (7:1):

| Pair | Ratio |
|---|---|
| Violet `#c8a8f0` on background | 9.13:1 AAA |
| Violet `#c8a8f0` on surface | 8.34:1 AAA |
| Teal `#6ec9c0` on background | 9.54:1 AAA |
| Teal `#6ec9c0` on surface | 8.71:1 AAA |
| Yellow `#ffcc55` on background | 12.41:1 AAA |
| Yellow `#ffcc55` on surface | 11.33:1 AAA |

**Light mode:**

| Pair | Ratio |
|---|---|
| Violet `#5b3a8a` on background | 7.77:1 AAA |
| Violet `#5b3a8a` on surface | 7.22:1 AAA |
| Teal `#1c7069` on background | 5.26:1 AA |
| Teal `#1c7069` on surface | 4.89:1 AA |
| Yellow `#ffcc55` on background | 1.34:1 FAIL |
| Yellow `#ffcc55` on surface | 1.25:1 FAIL |

Yellow failing on light surfaces is expected and intentional — see Yellow Usage above. Yellow is never used as text or outline on light surfaces.

### Color Blindness

Palette was chosen with deuteranopia/protanopia (red-green color blindness) in mind. Violet and golden yellow sit on opposite sides of the color confusion axis and remain clearly distinct under simulation.

### Chart Palettes

Three heat map palettes are available and user-selectable from Settings. All three are designed as smooth cold-to-hot scales that read naturally without any accessibility accommodation — the colorblind variants are not degraded versions, just axes chosen to remain distinguishable under specific simulations.

Each palette defines seven evenly-spaced color stops for both dark and light mode. Intensity is interpolated linearly between adjacent stops.

### Null vs. Zero Values

Where possible, charts visually distinguish between a period with **no data recorded** (null) and a period where data was recorded but the value is **explicitly zero** — for example, a day where the user answered a YES/NO nudge but all answers were "No", giving a yes-count of zero.

- **Null (no data):** The cell renders in `surfaceVariant`, which matches the chart container background. The grid skeleton is still readable, but blank periods produce no visual noise — they simply disappear into the surface.
- **Zero (recorded, value is zero):** The cell renders one small step away from the container background: `lerp(surfaceVariant, Color.White, 0.1f)` in dark mode and `lerp(surfaceVariant, Color.Black, 0.1f)` in light mode. Subtle enough not to compete with real data, but distinguishable on close inspection.

This distinction is implemented in the calendar heat map across every granularity (the day strip, the day grid, the week grid, the week strip, and the month strip). Other chart types — line graphs, bar/column charts, and packed bubble charts — simply omit periods with no data and do not have a corresponding null state to represent.

**Validation:** Adjacent stops in each palette are tested against Viénot 1999 colorblind simulation matrices (deuteranopia, protanopia, and tritanopia) applied in sRGB space. Separation is measured as Euclidean distance in sRGB. The general-purpose SPECTRUM palette requires ≥ 0.06 minimum distance; the purpose-built colorblind palettes (HORIZON, EMBER) require ≥ 0.10 (or ≥ 0.08 for Ember light under tritanopia). These thresholds are enforced by automated tests.

#### SPECTRUM — Full ROYGBV gradient (default)

Violet → Blue → Teal → Green → Yellow → Orange → Red. The brand colors serve as natural anchors: violet primary at the cold end, teal secondary in the middle, golden yellow accent near the hot end.

- **Blue stop**: `#1840E0` (dark) / `#2050C0` (light) — deliberately shifted toward pure blue rather than a standard ROYGBV blue. A more teal-adjacent blue (e.g. `#4060D0`) loses adequate perceptual distance from the adjacent teal stop under deuteranopia simulation.
- **Amber stop (light mode)**: `#A87800` rather than pure yellow — yellow (`#FFCC55`) fails WCAG on light backgrounds and reads as near-white. Dark amber preserves the warm-middle anchor while maintaining contrast.
- **Orange stop (light mode)**: `#D86020` — brighter than the naive choice (`#C05010`) to maintain sufficient luminance separation from the adjacent amber under deuteranopia simulation, where the green channel is lost and similar warm tones collapse together.

Safe for most viewers. Not specifically optimized for any form of color blindness — use HORIZON or EMBER for that.

#### HORIZON — Blue to orange (deuteranopia / protanopia safe)

Navy → Blue → Teal-blue → Light teal → Amber → Orange → Dark orange. Spans the blue-to-orange axis, which is the hue dimension most robustly preserved under red-green color blindness simulations. Avoids green entirely.

- **Light mode orange stop**: `#E08020` — the naive stop (`#C06010`) fell too close to the adjacent amber (`#A87000`) under deuteranopia; increasing luminance at the warm end restores separation.
- Reads as a natural "twilight" or "sunset" scale for viewers without color blindness.

Minimum separation threshold: 0.10 (deuteranopia and protanopia). These are higher than SPECTRUM's 0.06 to reflect that HORIZON is advertised as specifically colorblind-safe.

#### EMBER — Purple to red (tritanopia safe)

Deep plum → Purple → Hot pink/magenta → Red → Coral/rust → Dark red. Progresses through the magenta-red axis, avoiding the blue-yellow axis where tritanopia (blue-yellow color blindness) causes confusion.

Tritanopia collapses green and blue channels together — under simulation, the only reliable axis of distinction is the red channel. The light mode stops are therefore designed with a **monotonically varying R channel** across all seven stops:

| Stop | Hex | R value |
|---|---|---|
| 0 (cold) | `#6838A8` | 0.408 |
| 1 | `#881868` | 0.533 |
| 2 | `#B01850` | 0.690 |
| 3 | `#C02030` | 0.753 |
| 4 | `#B81010` | 0.722 |
| 5 | `#980C0C` | 0.596 |
| 6 (hot) | `#700808` | 0.439 |

Steps 3–6 invert direction (decreasing R), but maintain sufficient inter-stop distance because they are converging from the peak toward very dark values — the drop in overall luminance distinguishes them even as R decreases. The overall cold-to-hot narrative reads as purple → red → near-black red, which is visually coherent on a light background (darker = more intense).

Dark mode ends at warm peach (`#FAD090`) — the brightest, most saturated stop represents maximum intensity on a dark background.

Minimum separation threshold: 0.10 (dark mode), 0.08 (light mode) under tritanopia simulation.

## App Icon

### Design

Lavender (`#c8a8f0`) background with a warm golden yellow radial glow (`#ffcc55` → transparent) emanating from the lower-right corner — consistent with the brand palette's violet primary and yellow accent roles. A light purple shadow echo of the squiggle sits behind the mark.

The identifying mark is an organic black squiggle (the "good hand" N-form), centered in the safe zone. It reads as both a gesture and an initial.

Source file: `art/Nudgery Final.svg` (Inkscape).

### Adaptive Icon Structure (API 26+)

| Layer | File | Contents |
|---|---|---|
| Background | `androidApp/.../drawable/ic_launcher_background.xml` | Lavender rect + radial glow gradient |
| Foreground | `androidApp/.../drawable/ic_launcher_foreground.xml` | Shadow echo + black squiggle |
| Adaptive icon | `androidApp/.../mipmap-anydpi-v26/ic_launcher.xml` | References background + foreground |
| Round variant | `androidApp/.../mipmap-anydpi-v26/ic_launcher_round.xml` | Same layers; OS handles the mask |

### Legacy Launcher Icons (pre-API 26 fallback)

Rasterized from `Nudgery Final.svg` via Inkscape. Stored in `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` (48–192px).

### Notification Badge

`shared/.../res/drawable/ic_notification.xml` — white squiggle on transparent background, 24dp intrinsic size. Android (API 21+) uses only the alpha channel, rendering it white against the notification accent color. The shadow echo is omitted at this size.

### Play Store

512×512 PNG (`art/play_store_icon.png`). Same full composition as the launcher icon.

### Regenerating Icon Assets

#### Artwork requirements

The source SVGs must meet these structural requirements before any export. Inkscape hides complexity that breaks the toolchain.

**Both SVGs:**
- Fills may be in either SVG presentation attributes (`fill="#cfaddd"`) or CSS style strings (`style="fill:#cfaddd"`). The generation script promotes them automatically, so no Inkscape preference change is needed.
- No nested or redundant transforms. Every shape's coordinates must already be in the final 108 × 108 viewport space with no `transform` attribute on individual `<path>` elements. Inkscape stacks transforms when you resize or reposition nested objects. To flatten: select all objects in the transformed layer, cut (Ctrl+X), create a new layer with no transform, and Paste in Place (Ctrl+Alt+V). Inkscape computes absolute canvas positions on paste, baking out the stacked transforms. Note: Inkscape layers cannot be ungrouped via Ctrl+Shift+G — only plain groups can.
- No `<ellipse>` or `<circle>` elements — svg2vectordrawable silently skips them. Convert all shapes to paths in Inkscape first: *Path > Object to Path*.

**`art/Nudgery Final.svg`:**
- One layer contains only the background shapes (the lavender rect and ellipses).
- A separate layer contains only the foreground shapes (shadow echo and black squiggle).
- Hidden reference layers (e.g. a safe-zone guide circle) are fine to keep. The generation script extracts layers by ID and ignores anything not explicitly included.

**`art/Nudgery HandOnly.svg`:**
- Must contain a single compound path — the boolean difference of the outer white silhouette minus the inner cutout — using `fill-rule="evenodd"`. Perform the difference in Inkscape (*Path > Difference*) and verify the result is one `<path>` element. A separate black-filled path is not treated as a mask and will render as opaque white in the notification icon.

#### Prerequisites

- **Inkscape 1.x** — available as `inkscape` on the command line
- **svg2vectordrawable** — `npm install -g svg2vectordrawable` (invoked as `s2v`)

#### Legacy launcher icons (PNG, pre-API 26)

Run from the project root:

```bash
for density in "mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192"; do
  d="${density%%:*}"; size="${density##*:}"
  inkscape --export-type=png --export-width=$size --export-height=$size \
    --export-filename="androidApp/src/main/res/mipmap-${d}/ic_launcher.png" \
    "art/Nudgery Final.svg"
done
```

#### Play Store icon (512×512 PNG)

```bash
inkscape --export-type=png --export-width=512 --export-height=512 \
  --export-filename="art/play_store_icon.png" \
  "art/Nudgery Final.svg"
```

#### Adaptive icon layers (VectorDrawable XML, API 26+)

svg2vectordrawable does not respect `display:none` layer visibility — it includes all elements regardless. The background and foreground must be exported as separate, minimal SVGs containing only the relevant elements before conversion.

For each layer, produce a clean SVG (`art/ic_launcher_background_export.svg` and `art/ic_launcher_foreground_export.svg`) that contains only the child elements of that layer, with no Inkscape-specific attributes and no hidden elements. Then convert:

```bash
s2v -i art/ic_launcher_background_export.svg \
    -o androidApp/src/main/res/drawable/ic_launcher_background.xml

s2v -i art/ic_launcher_foreground_export.svg \
    -o androidApp/src/main/res/drawable/ic_launcher_foreground.xml
```

After converting, verify and if necessary correct the root `<vector>` attributes by hand:

```xml
android:width="108dp"
android:height="108dp"
android:viewportWidth="108"
android:viewportHeight="108"
```

The `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` files reference these drawables by name and do not need editing.

#### Notification small icon (VectorDrawable XML)

Like the adaptive icon layers, the notification icon requires a clean intermediate SVG before conversion — the source file contains a BG reference layer and fills in style strings that must be stripped first. The generation script produces `art/ic_notification_export.svg` containing only the Badge layer's compound path with promoted fill attributes. Then convert:

```bash
s2v -i art/ic_notification_export.svg \
    -o shared/src/androidMain/res/drawable/ic_notification.xml
```

After converting, verify and correct the root `<vector>` attributes by hand:

```xml
android:width="24dp"
android:height="24dp"
android:viewportWidth="108"
android:viewportHeight="108"
```

Also verify the single `<path>` element has both:
- `android:fillColor="#FFFFFFFF"`
- `android:fillType="evenOdd"`

---

## Iconography

Material Symbols Rounded. Outlined by default, filled for active/selected states. This communicates selection without relying on color alone, supporting accessibility.

## Selection Chips

All selectable chip groups throughout the app (schedule type, day of week, answer type, trigger operators, timeframe picker) use a custom `NudgeryToggleChip` component rather than M3 `FilterChip` defaults. M3's default selected state maps to `secondaryContainer`, which in this palette is the same tone as `surface`, making selected and unselected states nearly indistinguishable. The custom chip corrects this with explicit per-state colors.

**Selected state:**
- Container: `colorScheme.background` — the darkest surface in dark mode (`#141218`), the lightest in light mode (`#f5f0ff`)
- Text: `colorScheme.onBackground` — near-white in dark mode, near-black in light mode; full opacity
- Border: 1.5dp stroke in `colorScheme.primary` (violet) — the outline signals the active state without relying on fill color alone

**Unselected state:**
- Container: `colorScheme.surface` — one step lighter than background in dark mode (`#1e1b24`), one step more saturated in light mode (`#ede8f5`)
- Text: `colorScheme.onSurface` at 38% alpha — visibly dimmed against the slightly elevated background
- Border: none

The contrast pattern between states is deliberate: the selected chip uses the *darkest* background available in dark mode (and lightest in light mode), so it reads as "pressed in" or grounded. The unselected chip uses a slightly elevated surface and dim text, making it read as receded or inactive. The combination of background tone + text brightness + border presence gives three simultaneous signals of selection, which is accessible without color alone.

38% alpha for unselected text is M3's standard disabled-state alpha, appropriate here because unselected options are intentionally de-emphasized rather than permanently unavailable.

## Internationalization (i18n)

All user-visible strings must go through Android's built-in `strings.xml` / `res/values/` system. No hardcoded strings anywhere in Compose code.

- Plurals use `plurals.xml` and `quantityStringResource()`
- Formatted strings (e.g. "Next nudge: Tuesday at noon") use named arguments in `strings.xml`, not positional — argument order may differ per language
- Date and time formatting uses `DateTimeFormatter` with the device locale, never hardcoded formats

## Answer Form

Full screen. Launched from a notification or the "Answer Now" button on the detail screen.

### Layout

- One question visible at a time
- Follow-up questions appear as subsequent pages after the previous answer is submitted — user always sees their position (e.g. "1 of 2")
- Every answer type has an explicit **"Save Answer"** submit button — no auto-advance on selection, to keep taps recoverable
- If the workflow is abandoned before completion (via the close button), the entire session is discarded — no partial records are written

### Controls by Answer Type

| Type | Control |
|---|---|
| Yes / No | Two large full-width tappable buttons + "Save Answer" |
| Scale | Integer slider with snap + "Save Answer" |
| Number | Decimal number text field + "Save Answer" |
| Option Single | Large tappable list items + "Save Answer" |
| Option Multi | Checkboxes + "Save Answer" |
| Text (follow-ups only) | Text field + "Save Answer" |
| Emoji | Read-only chosen-emoji display + inline emoji picker (see **Emoji Picker**) + "Save Answer" |

### Selection Visual Treatment

For Yes/No and Option Single, the selected button renders as a filled `Button` (solid primary background) while unselected options render as `OutlinedButton` at 40% opacity (`UNSELECTED_ALPHA`). For Option Multi, the entire row (checkbox + label) drops to 40% opacity for unchecked items. In all cases, dimming only activates once at least one selection has been made — before any selection, all options display at full opacity.

### Emoji Picker

The picker (behavior in ENGINEERING_DECISIONS.md ED-3/4/8/11/13; this is the visual spec) is **embedded inline and always-open** on the EMOJI answer screen — not a pop-up or bottom sheet — so the system keyboard rising for search resizes the page normally instead of fighting a sheet.

Top-to-bottom on the answer page:

- **Chosen-emoji display** — read-only; shows the answer string (one or more emoji, at the emoji-scale size). A backspace control removes the last emoji. Tapping it never raises the system keyboard. It **stays visible even while the search keyboard is up**, so a tap that appends to the answer is always reflected on screen.
- **Search field** — pinned above the grid; the *only* element that uses the system keyboard. Typing switches the grid from categories to ranked results (ED-11). Empty search shows the browse grid. A trailing **clear (circle-✕)** control wipes the search text (shown only when the field is non-empty).
- **Category tabs** — **at the top on Android** (matching Android-user expectation; iOS will place them at the bottom per its own convention — a deliberate per-platform divergence). The **first tab is Recents, under a clock icon** (a persisted, most-recent-first list). Remaining tabs are the Unicode groups.
- **Grid** — emoji rendered with the **device font** (no shipped glyphs, ED-3), filtered to what the device can render (ED-4). A normal tap **appends** the emoji (applying the default skin tone/gender, ED-6/7) and keeps the picker open. If the search keyboard is up, the tap also **dismisses it** — restoring the full answer screen (chosen-emoji display, Save/Cancel) so the appended answer is visible; the search text is left intact (clear it via the field's ✕). **Long-press** opens a variant row (skin tone / gender / hair / direction, ED-8); emoji that have variants show a subtle corner indicator.
- A **"Save Answer"** button submits the whole string (no auto-submit on tap, consistent with the other answer types).

**Icons are generic Material Symbols and emoji come from the device font** — we copy conventions, not any vendor's assets or branding (ED-13).

The grid emoji size honors the global **emoji scale** setting (ED-14). See **Settings Screen → Emoji defaults** for the skin-tone/gender swatch selectors and the emoji-scale control.

### Top Bar

Close (✕) button only — no back arrow. Closing discards the entire session and dismisses the form. The user returns to wherever they were before (home screen, notification shade, or the app).

### Timestamps

Every answer record stores two timestamps:

- `scheduled_at` — the nudge's intended fire time; used to anchor the data point to the correct day in visualizations
- `answered_at` — when the user actually submitted the answer; kept for transparency and auditing

For on-time answers these will be close together. For late answers, the gap is informative. Visualizations plot against `scheduled_at`.

### Missed Nudge Indicator

The main list row for each nudge shows a small rotated badge (exclamation mark, ~32° angle, golden yellow) when the nudge has an unanswered missed firing.

Rules:
- Only the **most recent** scheduled fire time is considered — older unanswered firings become gaps in the data, not persistent alerts
- The indicator is shown if the most recent `scheduled_at` that has passed has no completed answer record
- The indicator clears only when that most recent scheduled answer has been fully submitted — opening the detail screen without answering does not clear it

## Motion

Default to Material 3 motion and only deviate with intention. The goal is polish that most users won't consciously register but would notice if absent.

### Everywhere (subliminal layer)
- Screen transitions: crossfade at 490ms (`NAV_TRANSITION_DURATION_MS`). The `NavHost` carries `Modifier.background(MaterialTheme.colorScheme.background)` so the blend-through color matches the active theme — dark in dark mode, light in light mode — rather than the Android window background.
- Button press: Material 3 ripple default — keep as-is
- Nudge list load: very subtle fade-in stagger — 30–50ms offset per item; the list feels like it arrived rather than popped

### Answer Form
- Question-to-question transition: gentle horizontal page slide, consistent with the step pagination
- "Save Answer" confirmation: brief checkmark or pulse before advancing (300ms)

### Detail Screen
- Charts animate in on load — Vico's built-in entry animation applies to both `LineCartesianLayer` (line graphs) and `ColumnCartesianLayer` (bar and column charts); do not suppress it
- Calendar heat map has no Vico equivalent and will use a custom Canvas composable; its entry animation should match Vico's timing
- Answer submission from the detail screen gets the most expressive treatment — golden yellow accent plays here

## Shape Language

Generously rounded corners — friendly without being cartoonish. Override Material 3's shape scale in `Theme.kt`:

| Component | Corner Radius |
|---|---|
| Cards and dialogs | 16dp |
| Buttons | 12dp |
| Input fields | 12dp |
| Small chips and tags | Full pill |

Full pill is reserved for small standalone elements (timeframe chips, the "Answer Now" button) where it reads as intentional rather than bubbly.

## Navigation

Single-stack navigation, no bottom nav bar. Implemented as a single Jetpack Compose `NavHost`.

### Destinations

| Screen | Top Bar |
|---|---|
| Main list | App name ("Nudgery") with tagline "Ask Yourself" underneath in `labelSmall` + settings icon (top right) |
| Nudge detail | Back arrow |
| Create nudge | Close (✕) — form screen, back is ambiguous |
| Edit nudge | Close (✕) — form screen, back is ambiguous |
| Answer form | Close (✕) — launched from notification, no meaningful back destination |
| Settings | Back arrow |
| About | Back arrow, or section at the bottom of Settings if content is light |

### Notes

- Settings is reachable from the main list top bar icon
- About lives inside Settings
- Answer form can be launched from a notification outside the normal back stack — close always dismisses back to wherever the user was

## Light / Dark Mode

Both from the start. Follows the OS setting by default. Users can override in Settings with a three-option toggle: System / Light / Dark.

## Font Scaling

The app must degrade gracefully at OS-level font sizes up to 2×. Implementation rules:

- Always use `sp` for text sizes, never `dp`
- Never give a container a hardcoded height if it contains text — size to content
- Test `maxLines = 1` truncation explicitly at large font scales; prefer wrapping where possible
- Maintain Material 3's 48dp minimum touch target regardless of font scale

## Schedule and Time Display

### Schedule Description Abbreviations

When a schedule is abbreviated for display (e.g. in list rows and the detail screen), day sets are collapsed to natural labels before falling back to individual abbreviations:

| Day set | Label |
|---|---|
| All 7 days | Every Day |
| Mon–Fri | Weekdays |
| Sat–Sun | Weekends |
| Any other combination | Individual days joined with `, ` |

Individual day abbreviations: **M, Tu, W, Th, F, Sa, Su**.

### Next Fire Time Format

Next fire times are formatted as local device time — no timezone suffix, no UTC notation. Format:

- If the fire date is **tomorrow** (device local date): `"Tomorrow at 2:30 PM"`
- Otherwise: `"May 20 at 9 AM"` — full month name, day number, no day-of-week, no year

AM/PM time uses no leading zero and omits minutes when on the hour.

---

## Create / Edit Nudge Wizard

Three-step wizard, used for both creating and editing. On edit, all steps are pre-populated.

Top of screen: non-tappable step progress indicator (e.g. step 1 of 3).
Bottom of screen: Back and Next buttons. Final step has a Save button in place of Next. Close (✕) in the top bar discards the entire session.

### Step 1 — The Question

- Nudge name (text field)
- Main question text (text field)
- Answer type selector: Yes/No, Scale, Number, Option Single, Option Multi
- If Option Single or Option Multi: option builder (add/remove/reorder up to 16 options)

**Answer type change warning:** If the user changes answer type after follow-up questions have been defined in step 2, or after options have been defined for an Option type, warn them that the change will discard their follow-up configuration and give them the option to cancel the type change. No warning if no follow-ups or options have been defined yet. Changing between Option Single and Option Multi is never destructive — no warning.

### Step 2 — Follow-ups

Empty state with an "Add follow-up question" button. User can ignore this entirely and tap Next to proceed without follow-ups.

Each follow-up question defines:
- A **trigger condition** — controls rendered by main question type:
  - *Yes/No*: two chips (Yes / No)
  - *Number*: operator chips (=, >, ≥, <, ≤) + numeric text field
  - *Option Single / Option Multi*: one chip per option from the main question; OPTION_MULTI uses `CONTAINS` operator so the follow-up fires when that option appears anywhere in the multi-select answer
- Question text
- Answer type (Yes/No, Number, Option Single, Option Multi, or Text)
- Options if Option type

Multiple follow-ups can be added. Follow-ups can be removed.

This step is accessible from both the creation wizard and the edit wizard. The edit wizard pre-populates it with any existing follow-up questions.

### Step 3 — Schedule

- Frequency: Daily, Weekly, Monthly, Hourly
- Time of day (defaults to noon in device timezone)
- Active days of the week (Daily and Hourly)
- Day of week (Weekly)
- Day of month (Monthly)
- For Hourly: define active hours within the active days
- Enabled toggle (defaults to on)

## Detail Screen Layout

Top to bottom:

1. **Main question text** — `titleMedium`, `onSurfaceVariant`; shown just below the nudge name
2. **Schedule** — inline with a calendar (`CalendarMonth`) icon; tapping the icon navigates to `EditNudgeScreen` at `initialStep = 2` (schedule step)
3. **Follow-up questions** — shows count ("N follow-up question(s)") when any exist, or the label "Follow-up questions" when none; inline with an edit icon (`QuestionAnswer`); tapping navigates to `EditNudgeScreen` at `initialStep = 1` (follow-ups step)
4. **"Answer Now" button** — pill-shaped, prominent
5. **Main chart** — with a vertical column of icons outside the upper-right corner:
   - Chart type icon (opens chart editor)
   - Download icon (opens export menu: Export data (CSV), Export data (TSV), Back up nudge (JSON))
   - Magnifying glass (opens expanded chart view)
   - Icons are visually small but maintain 48dp touch targets
6. **Timeframe picker** — row of chips just below the chart; changes the current view but does not persist. Persistent default is set inside the chart editor.
7. **Raw data table** — collapsed by default, with a visible header row (e.g. "12 answers") indicating content. Each row has a per-answer hide control; tapping it triggers a confirmation dialog before hiding.

### Charts and Visualizations

All charts on the detail screen are locked to a single shared time **window** so that the main
question chart and every follow-up chart always show the same period (see README — *Viewing Nudges*
for the user-facing description). The design particulars behind that behavior:

**The window equals the timeframe.** Selecting a timeframe chip sets the window span — weekly = 7
days, monthly = 30, yearly = 365, all-time = the full history — and resets it to the most recent
period. There is no separate "page" concept; earlier data is reached only by sliding the window.

**Navigation.**
- Time-based charts (calendar heat map, line graph) are dragged left/right directly; the drag
  slides the shared window.
- Categorical charts (bar, column, packed bubble) have no time axis, so a slim scrubber strip below
  the chart slides the window instead.
- A drag is a continuous gesture: one sweep moves through as many days as the finger travels, and
  the gesture is never interrupted mid-drag. One full-width swipe slides the window by three times
  the selected timeframe (3 weeks on weekly, 3 months on monthly, 3 years on yearly), so scrolling
  through history feels the same speed at every zoom level (`FULL_SWIPE_TIMEFRAME_MULTIPLIER`).

**Smooth scrolling.** A window shift reloads each chart's data, but the chart composables are kept
alive across reloads (keyed on chart *type*, not on the data object) so that:
- in-flight drag gestures survive the reload instead of being cancelled (otherwise the user has to
  re-tap to move more than one step), and
- entrance animations (e.g. the line graph growing from zero) play only on first load or when the
  user switches chart type — not on every shift.
Rapid shifts conflate to the latest resting window so a fast drag issues one reload, not a backlog.

**Heat map fill vs. scroll.** "Fill the canvas" means the whole current period is visible at once
without having to scroll *within* it — not that scrolling is disabled. Monthly, yearly, and all-time
heat maps size their cells so the entire month/year/history fits the chart; weekly shows a short,
large-celled strip of days. Every timeframe except all-time still scrolls — dragging slides the
shared window to earlier periods (see *Navigation* above). All-time already shows the entire history,
so it has nowhere to scroll.

**One square at a time.** Cells are laid out column-major to fill the grid, so sliding the window by
one cell reflows the whole grid by one position — squares visibly travel through the grid rather
than columns sliding off the edge. This is intentional: the motion conveys how the grid is arranged
and reads better than a rigid column-by-column shift. For this to stay clean, the window must step
in whole cells: the yearly heat map's cells are *weeks*, so its window steps a week (7 days) at a
time (`windowStepDays`); a day-sized step there would re-bucket partial weeks and oscillate the
week-cell count, reshuffling the grid. Weekly and monthly have day cells and step a day at a time.

**Line graph windowing.** The line graph fits exactly the window's day span; it does not scroll
internally. All-time fits the entire range.

**Line graph Y axis (fixed, not auto-fit).** The Y axis is locked to a stable range computed over
*all* the question's answers, so scrubbing the window never rescales it and absolute maxima stay at a
constant height. SCALE uses its defined min–max (a "7" is always at the same height); NUMBER and the
YES/NO daily-count line anchor at 0 and top out at the global maximum rounded up to a clean bound
(1/2/5 × 10ⁿ). The trade-off — a single large NUMBER outlier compresses normal values — is accepted
in favor of comparability across windows.

**Bar / column chart color.** Bars carry the user's chosen chart palette, but — unlike the heat map
and bubble chart, which spend color on magnitude because they have no length — a bar already encodes
its count through its length, so color is spent on *identity* instead. Each category is pinned to a
fixed palette color by its stable position in the full set (`NamedCount.orderFraction`: an option's
order among all options, spread evenly 0..1; YES and NO at the two ends). Because that position is
independent of count, rank, and the selected window, a category keeps its color as the timeframe
moves and the bars re-sort — you can track the same option across windows. (Trade-off: the palettes
are magnitude gradients, so a question with many options yields analogous neighboring hues.)

**Column chart axis labels.** Category labels are drawn smaller than Vico's default and tilted at a
shallow angle so that every label stays visible. Vico's aligned axis item placer drops labels (e.g.
shows only every other one) when the widest label is wider than the per-column spacing; shrinking
and angling each label reduces its horizontal footprint enough to keep them all. This applies in
both the inline card and the expanded full-screen view.

### Chart Editor

Accessed via the chart type icon. Contains:
- Chart type selector
- Default timeframe selector (persists across sessions)

### General Nudge Editing

All three entry points on the detail screen navigate to the same `EditNudgeScreen` via an `initialStep` parameter:

| Entry point | Step | Content |
|---|---|---|
| Pencil icon (top bar) | 0 | Question text and name |
| Follow-up icon (follow-up row) | 1 | Follow-up questions |
| Calendar icon (schedule row) | 2 | Schedule |

## Approximate Scheduling Indicators

When the exact alarm permission is not held (see ARCHITECTURE.md — *Exact Alarm Permission Strategy*), nudge notifications will still fire but may be delayed by several minutes or more. The app surfaces this degraded state visibly rather than silently.

### Affected API levels

This indicator is only ever shown on API 31+ (Android 12 and above). On earlier API levels, exact alarms are always permitted and no indicator appears.

### Next fire time treatment on the nudge list

Each nudge list item displays the next scheduled fire time (e.g. "Next: Tomorrow at 9 AM"). When exact scheduling is not available, two changes apply to this text:

- **"Around" qualifier on the time** — "around" is inserted before the time portion only, not the full string. The date/time separator changes from " at " to ", around ", giving e.g. "Next: Tomorrow, around 9 AM" or "Next: May 20, around 9 AM". The date anchor stays authoritative; only the specific time is marked approximate.
- **Golden yellow color** — the text is rendered in `colorScheme.tertiary` (golden yellow) rather than the default `onSurfaceVariant`. Golden yellow is used instead of `colorScheme.error` (red) because the situation is recoverable and informational — nudges will still fire, just less punctually. Red would imply a system failure; yellow implies "attention needed, but things are still working."

Nudges with no schedule, or with scheduling disabled, show neither the time nor the indicator.

### Explanation pathway

The approximate next fire time text is tappable. Tapping it opens an inline dialog that:

- Explains in plain language that exact notification scheduling is not enabled
- States the practical consequence: notifications will still arrive but may be a few minutes late
- Offers two actions:
  - **"Open Settings"** — fires `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, taking the user directly to the system Alarm & Reminder settings page for Nudgery. When the user toggles the permission on and returns to the app, the indicator clears.
  - **"Dismiss"** — closes the dialog without action; the indicator remains until the permission is granted

The same explanation and "Open Settings" path is also available via the diagnostic row in the Settings screen, which is the recovery path for users who dismiss the dialog without acting.

### Indicator lifecycle

The granted/not-granted state is re-evaluated on every `ON_RESUME` lifecycle event so the indicator clears immediately when the user returns from system settings having granted the permission — without requiring an app restart.

## Empty States

### Main List (no nudges yet)

A single oversized "create your first nudge" button, centered on screen. No illustration, no supporting text. The FAB/+ button is promoted to center stage when the list is empty and returns to its normal corner position once nudges exist.

Illustration may be added in a future pass if more whimsy is needed.

### Other Empty States

To be defined during implementation as needed.

## Settings Screen

| Setting | Type | Default |
|---|---|---|
| Theme | Three-option toggle: System / Light / Dark | System |
| Bold text | Toggle | Off |
| Chart palette | Three-option radio: Full spectrum / Blue to orange / Purple to red | Full spectrum |
| Import | Button: "Import Nudge from Backup" (opens file picker for `.json` backup files) | — |

Bold text toggle swaps Regular → Medium and SemiBold → Bold throughout the theme's `Typography` object.

Each chart palette option shows a small gradient swatch (64×12dp, rounded corners) so the user can preview the scale before selecting it. The swatch adapts to the current dark/light mode, reflecting exactly how the palette will render in charts. Palette descriptions note the colorblind use case: "Recommended for deuteranopia / protanopia" (HORIZON) and "Recommended for tritanopia" (EMBER). SPECTRUM is labeled "ROYGBV gradient" without a colorblind recommendation since it is the default general-purpose option.
