# Overview
This project will take a Documentation Driven Development approach, in which the end-user documentation is written as if the software already exists, and is then used (by humans or agents) as directions to create the software. 
See @.claude/shared/AGENTS.md

# Repository Setup
The always-on agent rules and the shared workflow skills are not stored in this repo — they come
from the [k5](https://github.com/K4-713/k5) submodule mounted at `.claude/shared`.

* **After cloning, run `git submodule update --init`.** Without it `.claude/shared` is empty: the
  `@.claude/shared/AGENTS.md` import above resolves to nothing and the skill symlinks in
  `.claude/skills/` dangle, so the shared rules and workflows silently go missing.
* **To update the shared rules:** `git -C .claude/shared pull && .claude/shared/install.sh && git add .claude`,
  then commit the moved submodule pointer along with any added or pruned skill links. Re-running
  `install.sh` is what picks up skills k5 has added or removed.
* Skills are scanned when a session starts, so start a fresh agent session after either step to pick
  up changes.

## Dependencies
* Whenever a dependency is introduced, updated, or removed (in `gradle/libs.versions.toml` or a module's `build.gradle.kts`), regenerate the open-source credits so attribution stays accurate:
  * Run `./gradlew :androidApp:generateCredits` and commit the updated `CREDITS.md`. It is generated from the release build's actual dependency graph (harvested by the AboutLibraries Gradle plugin), so it must be refreshed by hand — it does not update itself.
  * The in-app *Settings → About → Open-source licenses* screen regenerates automatically on every build, so it needs no manual step.
  * Third-party assets that are **not** Maven dependencies (e.g. bundled fonts) are credited via manual entries under `androidApp/config/libraries/` and `androidApp/config/licenses/`. Add or remove these when such assets change.
  * **Test-only dependencies** are absent from the release graph, so they are never harvested and owe us no attribution — but we credit them anyway. They live in `androidApp/config/credits/test-tooling.md`, which `generateCredits` appends verbatim to `CREDITS.md` under its own heading, keeping the generated list an accurate statement of what actually ships. Edit that file by hand when a test-only dependency is added, replaced, or removed; editing the section directly in `CREDITS.md` will be overwritten.

# Regenerating Android Icon Assets
See the **Regenerating Icon Assets** section under **App Icon** in `DESIGN.md` for artwork requirements, prerequisites, and step-by-step instructions.

# Tagging a new release
Prior to tagging a new release:
* Run the `wrap-up-work` skill to ensure that the work adheres to our own processes.
* Run the full verification pass and leave it green: `./gradlew build`. This assembles debug and
  release, runs every unit test in both modules, **and runs lint** — which nothing else does. The
  `test*` tasks skip lint, and `assembleRelease` / `bundleRelease` prepare lint models but never run
  the checks, so a lint regression is invisible unless this command is the one you run. Fix what it
  reports rather than adding a lint baseline.
* Run the instrumented tests with a device or emulator attached: `./gradlew connectedAndroidTest`.
  These cover the alarm, scheduling, and notification-posting path, and `./gradlew build` never runs
  them — so nothing else in this checklist exercises that code.
* Verify the licensing split is intact. The project is dual-licensed: Nudgery's own
  source code and non-art assets are CC0 1.0 (public domain), while the original
  hand-drawn artwork is CC BY-SA 4.0. Check that:
  * `LICENSE` (CC0 + scope note) and `art/LICENSE` (CC BY-SA 4.0 text) both exist and the
    scope note still lists every artwork file and bundled third-party component accurately.
  * The hand-drawn artwork (app icon, Play Store feature banner, alert/notification icon) and
    its derived renderings are NOT marked CC0. Source SVGs live in `art/`; derived XML
    drawables under `res/` should carry an `SPDX-License-Identifier: CC-BY-SA-4.0` comment.
  * Bundled third-party assets (Atkinson Hyperlegible Next → OFL-1.1, Unicode emoji data →
    Unicode 3.0) remain credited in CREDITS.md and the in-app licenses screen.
* Gather the release notes. Harvest the commits since the previous release with
  `git log <previous-tag>..HEAD` (e.g. `git log v0.7.5..HEAD`) and curate them into a new
  `CHANGELOG.md` entry for the version being tagged:
  * Follow Keep a Changelog (Added / Changed / Fixed). Commit messages are the raw material, not the
    final wording — rewrite for clarity, merge a feature's many commits into one bullet, and fold
    internal-only churn (prep refactors, doc/test upkeep) together or omit it.
  * Move anything under `[Unreleased]` into the new version's entry, then leave a fresh empty
    `[Unreleased]` section at the top.
  * Distill the **user-affecting** subset into the Play Store "What's new" text: a few short, friendly
    bullets a user would actually notice, in the app owner's voice (this is user-facing prose — draft
    it for the owner to finalize, don't impose wording). Internal/licensing/refactor items usually
    don't belong in "What's new".
* Remove completely finished sections from TODO.md. Leave only sections that still have unfinished pieces.
