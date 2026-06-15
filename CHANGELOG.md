# Changelog

All notable changes to Nudgery are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/); versions match the `vX.Y.Z` git tags.

At each release the notes are harvested from `git log <previous-tag>..<new-tag>` and curated into the
entry below, then distilled into the Play Store "What's new" text. See AGENTS.md →
*Tagging a new release*.

## [Unreleased]

## [0.9.0] - 2026-06-14

### Added
- **Share nudges with friends.** A new Share icon on a nudge's detail screen sends that nudge's
  *setup* — its question, follow-ups, and schedule, with none of your answer data — as a `.nudge`
  file through your device's normal share sheet. Open one on a phone that has Nudgery and it imports
  as a fresh, empty nudge, so a group can all log the same thing and compare notes.
- **Follow-up questions on every nudge type, with an "Always" trigger.** Follow-ups are no longer
  limited to questions with fixed answers: any nudge — including Freeform Text and Emoji — can now
  carry follow-ups via an "Always" trigger that asks the follow-up after every answer (handy for an
  "any notes?" prompt). The follow-up step now appears while setting up any question type.
- **Negative scale ranges.** A Scale question's range can now run below zero (for example -5 to 5);
  both ends must be whole numbers.

### Changed
- **Smoother Scale slider.** The slider is finer, so every value in a wide range is reachable, and
  it starts at zero when the range is centered on zero (e.g. -5 to 5).
- **Chart polish.** Charts now use Nudgery's teal instead of a default blue, and chart labels and
  gridlines follow light/dark mode.
- **Emoji picker.** With no recent emoji, the picker opens the first populated category instead of an
  empty Recents tab, and the selected category is easier to see.

### Fixed
- Line graphs draw a dot for a single data point (instead of an invisible line) and show a faint zero
  "tide line" when the data crosses between negative and positive values.

## [0.8.0] - 2026-06-11

### Added
- **A welcoming first-run screen.** With no Nudges yet, the main screen now greets you with a
  welcome, a line about what Nudgery does, and a clear "Create Your First Nudge" button (and a
  tappable illustration) instead of a bare corner "+".
- **Setup validation.** Nudgery now keeps you from saving a Nudge that can't work: the name and
  question text are required, option questions need at least two non-blank options, a scale's
  minimum must be below its maximum, and each follow-up needs a trigger. Next/Save stays disabled
  with inline hints until things are valid; Back and Cancel stay available.
- **Contextual help (ⓘ).** A reusable info button tucks explanations a tap away instead of
  cluttering the form — first used on the Yes/No per-day option.
- **A short intro to follow-up questions** on the create wizard's follow-up step.
- **"Import & Fix" for imperfect backups.** If an imported backup has a setup problem (e.g. a
  hand-edited file missing options or a follow-up trigger), Nudgery offers to import it and open it
  in the editor to fix — keeping all its answer history — instead of failing.

### Changed
- Relabeled the Yes/No "One Yes Per Day" option to **"Limit to one 'Yes' per day"**, with its
  explanation now behind the info button.
- Refreshed the example question prompts shown while setting up a Nudge.

### Fixed
- Placeholder ("ghost") text in entry fields is now visibly lighter than text you've typed.
- The randomly-chosen example question no longer changes as you type elsewhere on the screen.
- A follow-up you add but never fill in is now discarded automatically instead of lingering blank.

## [0.7.5] - 2026-06-09

### Added
- **Drag-to-reorder nudges** on the main list: long-press a nudge to pick it up, then drag to reorder.
  The rest of the list animates to make space, the lifted card gets a tilt/shadow/accent treatment,
  and the new order is saved.
- Full-screen charts are now titled by the **question** being charted rather than the chart-style name.

### Changed
- Tightened the spacing of the nudge detail screen's question / schedule / follow-up header.
- Wording: Nudgery says you **log** your data — "track" is reserved for the third-party user-tracking
  the app deliberately does not do.
- Licensing: project source is dedicated to the public domain under **CC0 1.0**; the original
  hand-drawn artwork is licensed **CC BY-SA 4.0**.

### Fixed
- Answering a nudge inside the app now clears its outstanding notification, so a leftover alert no
  longer prompts you to answer the same nudge twice.
- Follow-up answers now attach to the correct response in the raw-data table — an off-schedule
  "Answer Now" no longer splits a response so its follow-up floats under the table header.
- The emoji skin-tone chosen in Settings now also restyles the gender example people.
- Drag-to-reorder polish and fixes: correct pick-up targeting (off-by-one), the ability to drop a
  nudge into the very top slot, and steadier auto-scroll near the screen edges.
