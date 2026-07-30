## Test tooling — with thanks

None of these ship. No code from them reaches your device, they are absent from the app's release
dependency graph, and their licenses ask nothing of us because we distribute nothing of theirs.

They are listed anyway, because Nudgery's entire automated test suite runs on them. Every behavior
described in this project's documentation is held to account by tests built on this work, which is
as critical to the app being trustworthy as anything that does ship.

- JUnit — EPL-1.0 — <https://junit.org/junit4/>
- Robolectric — MIT — <http://robolectric.org>
- JSON-java (`org.json`) — Public Domain — <https://github.com/stleary/JSON-java>
- kotlinx-coroutines-test — Apache-2.0 — <https://github.com/Kotlin/kotlinx.coroutines>
- AndroidX Test — JUnit extension — Apache-2.0 — <https://developer.android.com/jetpack/androidx/releases/test>
- AndroidX WorkManager — testing artifact — Apache-2.0 — <https://developer.android.com/jetpack/androidx/releases/work>

The two AndroidX artifacts declare no license in their POM; they are Apache-2.0 under the AndroidX
project's licensing. Versions are deliberately omitted here — the authoritative versions live in
`gradle/libs.versions.toml`, and this section is about the projects rather than a snapshot of them.

Unlike the generated list above, this section is maintained by hand: it is appended verbatim from
`androidApp/config/credits/test-tooling.md`. Update it when a test-only dependency changes.
