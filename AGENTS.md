# Overview
This project will take a Documentation Driven Development approach, in which the end-user documentation is written as if the software already exists, and is then used (by humans or agents) as directions to create the software.

# Project Documentation
- See README.md

# Architecture
- See ARCHITECTURE.md

# Next Steps
- See TODO.md

# Coding Practices
Project requirements are defined by the end-user documentation. To implement these requirements:
* First, before any code is written for a new feature or requirement, write automated tests that verify the behavior outlined in the end-user documentation. Initially, these tests will fail.
  * These tests will not be the only automated tests, so they should be easily identifiable by a TDD_ prefix in the test name, and commented with the line(s) in the documentation covered by this test
* Implement the new feature(s)
* Once the feature is implemented, the documentation tests will pass, and keeping those tests will prevent regressions.
* Always note next steps for implementation, if any, in TODO.md

# Guidelines
* Update code comments when relevant changes are made to the code
* Keep ARCHITECTURE.md current
* Use best practices relating to data security
* Use best practices relating to accessibility
* Prefer performant and battery-conscious solutions
* Prefer human-readability over source code brevity, both in structure and in naming
* Code should be modular and reused wherever possible, rather than duplicated. Common patterns should be abstracted out to short reusable helper functions
* Avoid defining "magic numbers" or string constants in the code which could be system settings or config variables
* Config variables containing secrets must not be copied to committed code
* Reuse existing structures, functions, and patterns when writing new features. If existing structures don't support needed behavior, prefer refactoring those structures to add support over parallelizing or short-circuiting existing structures
* Code should be easy to deploy, and must provide a path to roll back
* Use open standards whenever possible

## Dependencies
* 3rd party dependencies must be kept current
* Avoid introducing new dependencies to production code
* Dependencies must be removed when no longer needed

## Logging
* Always log key events for system visibility
* Always use the appropriate log level
  * Errors should be reserved for system-level problems that represent an unexpected outage or partial loss of functionality, which may require developer attention to address
  * Use Warnings for events that are unexpected, not optimal, and/or poorly handled, but that do not represent a system outage or loss of functionality that a user would notice.
  * Info should be used to enbable things like counting, tracking, or monitoring performance, and general system activity audits
  * Debug should be saved for verbose logs that are usually not wanted unless there is a problem that requires temporary in-depth troubleshooting
* Changing log level must be achieveable via a settings change, rather than a code deploy

## Automated Testing
* Write tests to ensure adherence to the end-user documentation, to uncover bugs in existing code, and to prevent future regressions
* When tests fail, start by looking for bugs in the code covered by the test
* Automated tests must mock everything that may contact external services, including the local database
* All potentially destructive code (code that could delete or overwrite existing data) must have test coverage
* All code that could possibly handle a user's Personally Identifiable Information (PII) must have test coverage
* All code initiating calls to external services must have test coverage
* Examine test run output for errors and warnings, and address them appropriately
  * If they are warnings or errors we are intentionally throwing or expecting as part of the test, try to catch them gracefully before they make it to test output
  * If they are errors or warnings thrown by the test infrastructure, or unexpected messages from the code we are testing, diagnose and address the underlying issue being described
* Test the things we expect to happen. Also test things like edge cases, missing resources, garbage inputs, and successful prevention of things we don't want to happen.


## Refactoring
* Code should occasionally be refactored to:
  * Comply with new requirements or objectives
  * Simplify existing code
  * Improve performance
  * Remove or update old dependencies
  * Remove deprecated features and general cruft
* Keep code refactoring work separate from feature development
  * When new features require refactoring, do the required refactor as a separate prep commit before working on the new feature directly
* Refactoring should be targeted, with individual refactoring commits confined to one or two improvements
* When refactoring, first make sure the targeted code has thorough test coverage for all expected behavior in that part of the system. After the refactor, reuse those tests to verify that the refactor does not change any expected system behavior
  * It is not unusual to uncover and fix pre-existing bugs as part of this process. These should be documented in the commit message

# Regenerating Android Icon Assets

All Android icon assets are derived from two source SVGs in `art/`:

| Source file | Used for |
|---|---|
| `art/Nudgery Final.svg` | Launcher icon (adaptive layers + legacy PNGs) and Play Store icon |
| `art/Nudgery HandOnly.svg` | Notification small icon (solid squiggle silhouette only) |

## Prerequisites

- **Inkscape 1.x** — available as `inkscape` on the command line
- **svg2vectordrawable** — converts SVG to Android VectorDrawable XML; install with `npm install -g svg2vectordrawable`

## Legacy launcher icons (PNG, pre-API 26)

Export the full composition at five densities. Run from the project root:

```bash
for density in "mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192"; do
  d="${density%%:*}"; size="${density##*:}"
  inkscape --export-type=png --export-width=$size --export-height=$size \
    --export-filename="androidApp/src/main/res/mipmap-${d}/ic_launcher.png" \
    "art/Nudgery Final.svg"
done
```

Output files: `androidApp/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`

## Play Store icon (512×512 PNG)

```bash
inkscape --export-type=png --export-width=512 --export-height=512 \
  --export-filename="art/play_store_icon.png" \
  "art/Nudgery Final.svg"
```

## Adaptive icon layers (VectorDrawable XML, API 26+)

The adaptive icon uses two separate VectorDrawable files, each with a 108dp × 108dp canvas (viewport 108 × 108). The inner 72dp is the safe zone — keep all visible content within it.

`Nudgery Final.svg` must have two top-level Inkscape layers with these exact labels:
- **Background** — lavender rect and radial glow gradient
- **Foreground** — shadow echo and black squiggle

Before exporting each layer, verify layer labels via `Layer > Layers…` in Inkscape. Then produce a single-layer export SVG for each:

1. In Inkscape, hide all layers except **Background**. Save a copy as `art/ic_launcher_background_export.svg`.
2. Hide all layers except **Foreground**. Save a copy as `art/ic_launcher_foreground_export.svg`.
3. Convert each to VectorDrawable:

```bash
svg2vectordrawable -i art/ic_launcher_background_export.svg \
  -o androidApp/src/main/res/drawable/ic_launcher_background.xml

svg2vectordrawable -i art/ic_launcher_foreground_export.svg \
  -o androidApp/src/main/res/drawable/ic_launcher_foreground.xml
```

4. Open each output XML and confirm these attributes are present on the root `<vector>` element:
   ```xml
   android:width="108dp"
   android:height="108dp"
   android:viewportWidth="108"
   android:viewportHeight="108"
   ```
   Correct them by hand if svg2vectordrawable sets different values.

The `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` files reference these two drawables by name and do not need to be edited.

## Notification small icon (VectorDrawable XML)

The notification icon is 24dp × 24dp. Android renders only the alpha channel (API 21+), colouring the opaque areas white against the notification accent colour. The squiggle should fill as much of the 24dp area as possible — there is no required margin.

### Source SVG structure (`Nudgery HandOnly.svg`)

`HandOnly.svg` uses a two-tone convention:

- **White shapes** — the visible squiggle. These become opaque `<path>` elements in the VectorDrawable (`android:fillColor="#FFFFFFFF"`).
- **Black shapes** — masking cutouts. These must be defined as SVG `<clipPath>` elements applied to the white shape — **not** as separate filled objects. A black-filled path will be converted to a white-filled path by svg2vectordrawable and will become visible, which is wrong.

Before converting, open `Nudgery HandOnly.svg` in Inkscape and verify:

1. The squiggle shape has `fill: #ffffff`.
2. Any black cutout regions are defined as `<clipPath>` elements in the SVG XML (check via `XML editor`), not as visible filled paths.
3. The white shape's bounding box fills as much of the SVG canvas as possible. Aim for the shape to reach within 1–2% of each canvas edge. Scale and reposition via `Object > Transform…` if needed.

### Converting to VectorDrawable

```bash
svg2vectordrawable -i "art/Nudgery HandOnly.svg" \
  -o shared/src/androidMain/res/drawable/ic_notification.xml
```

After converting, open the output and verify:

1. Root `<vector>` attributes — correct by hand if svg2vectordrawable sets different values:
   ```xml
   android:width="24dp"
   android:height="24dp"
   android:viewportWidth="108"
   android:viewportHeight="108"
   ```
   The 108 × 108 viewport matches the launcher foreground coordinate space, keeping path data consistent across assets.
2. Every `<path>` element has `android:fillColor="#FFFFFFFF"`. Any path with a dark fill colour was a black-masked region that was not correctly defined as a clip-path in the source SVG — fix the source and reconvert rather than patching the XML.

---

# Tagging a new release
Prior to tagging a new release, ensure that we are adhering to our own rules. Make and work through tasks to do the following:
* Look through the README.md file and compare the contents to the current code.
  * Identify areas of the code that need more end-user documentation
  * Identify parts of README.md that need to be corrected
  * Leave descriptive placeholders in square brackets in the README file, containing a short description of the fixes or undocumented behaviors that must be addressed.
  * Code behaviors that are currently tested in the TDDs without related information in the README.md should be prioritized.
  * Wait for the user to fix README.md before continuing to the next step.
* Have a look through the ARCHITECTURE.md, DESIGN.md, and TODO.md files, and call out any places where the documentation doesn't match the code. Decide interactively with the user which side is more correct in each mismatch case, and change the other side to match.
* If there are any substantial items in the README.md, DESIGN.md, or ARCHITECTURE.md docs that don't have TDD tests, write and run those tests which verify accuracy of the documentation.
* Remove completely finished sections from TODO.md. Leave only sections that still have unfinished pieces.
