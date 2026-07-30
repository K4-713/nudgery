# Overview
This project will take a Documentation Driven Development approach, in which the end-user documentation is written as if the software already exists, and is then used (by humans or agents) as directions to create the software. 
See @.claude/shared/AGENTS.md

## Dependencies
* Whenever a dependency is introduced, updated, or removed (in `gradle/libs.versions.toml` or a module's `build.gradle.kts`), regenerate the open-source credits so attribution stays accurate:
  * Run `./gradlew :androidApp:generateCredits` and commit the updated `CREDITS.md`. It is generated from the release build's actual dependency graph (harvested by the AboutLibraries Gradle plugin), so it must be refreshed by hand — it does not update itself.
  * The in-app *Settings → About → Open-source licenses* screen regenerates automatically on every build, so it needs no manual step.
  * Third-party assets that are **not** Maven dependencies (e.g. bundled fonts) are credited via manual entries under `androidApp/config/libraries/` and `androidApp/config/licenses/`. Add or remove these when such assets change.

# Regenerating Android Icon Assets
See the **Regenerating Icon Assets** section under **App Icon** in `DESIGN.md` for artwork requirements, prerequisites, and step-by-step instructions.

# Tagging a new release
Prior to tagging a new release, ensure that we are adhering to our own rules. Make and work through tasks to do the following:
* Look through the README.md file and compare the contents to the current code.
  * Identify areas of the code that need more end-user documentation
  * Identify parts of README.md that need to be corrected
  * Leave descriptive placeholders in square brackets in the README file, containing a short description of the fixes or undocumented behaviors that must be addressed.
  * Code behaviors that are currently tested in the TDDs without related information in the README.md should be prioritized.
  * Wait for the user to fix README.md before continuing to the next step.
* Have a look through the ARCHITECTURE.md, DESIGN.md, ENGINEERING_DECISIONS.md, and TODO.md files, and call out any places where the documentation doesn't match the code. Decide interactively with the user which side is more correct in each mismatch case, and change the other side to match.
* If there are any substantial items in the README.md, DESIGN.md, or ARCHITECTURE.md docs that don't have TDD tests, write and run those tests which verify accuracy of the documentation.
* Confirm every decision in ENGINEERING_DECISIONS.md has at least one TDD_ test enforcing it; write any that are missing. (Decisions still marked "implementation pending" are exempt until their feature lands.)
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
