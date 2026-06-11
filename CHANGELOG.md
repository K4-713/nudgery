# Changelog

All notable changes to Nudgery are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/); versions match the `vX.Y.Z` git tags.

At each release the notes are harvested from `git log <previous-tag>..<new-tag>` and curated into the
entry below, then distilled into the Play Store "What's new" text. See AGENTS.md →
*Tagging a new release*.

## [Unreleased]

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
