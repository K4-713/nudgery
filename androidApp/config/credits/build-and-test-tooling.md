## Build and test tooling — with thanks

None of these ship. No code from them reaches your device, they are absent from the app's release
dependency graph, and their licenses ask nothing of us because we distribute nothing of theirs.

They are listed anyway. Nudgery's entire automated test suite runs on the first group, and the
second group is what turns this source code into an app at all. Every behavior described in this
project's documentation is held to account by tests built on that work, which is as critical to the
app being trustworthy as anything that does ship.

### Testing

- JUnit — EPL-1.0 — <https://junit.org/junit4/>
- Robolectric — MIT — <http://robolectric.org>
- JSON-java (`org.json`) — Public Domain — <https://github.com/stleary/JSON-java>
- kotlinx-coroutines-test — Apache-2.0 — <https://github.com/Kotlin/kotlinx.coroutines>
- AndroidX Test — JUnit extension — Apache-2.0 — <https://developer.android.com/jetpack/androidx/releases/test>
- AndroidX WorkManager — testing artifact — Apache-2.0 — <https://developer.android.com/jetpack/androidx/releases/work>

### Building

- Gradle — Apache-2.0 — <https://gradle.org>
- Android Gradle Plugin — Apache-2.0 — <https://developer.android.com/build>
- Kotlin — the compiler, the Multiplatform and Android Gradle plugins, and the Compose compiler
  plugin — Apache-2.0 — <https://kotlinlang.org>
- SQLDelight Gradle plugin — Apache-2.0 — <https://github.com/sqldelight/sqldelight>
- AboutLibraries Gradle plugin — Apache-2.0 — <https://github.com/mikepenz/AboutLibraries> — which
  harvests the license data this very file is generated from

The SQLDelight and AboutLibraries *runtimes* do ship, and are credited in the generated list above;
only their build-time plugins are thanked here. Three artifacts declare no license in their POM —
the two AndroidX test ones and the AboutLibraries plugin — and are Apache-2.0 under their projects'
licensing. The Android SDK and the JDK the build runs on are not listed: they are platform tooling
rather than dependencies of this project.

Versions are deliberately omitted — the authoritative versions live in `gradle/libs.versions.toml`,
and this section is about the projects rather than a snapshot of them.

Unlike the generated list above, this section is maintained by hand: it is appended verbatim from
`androidApp/config/credits/build-and-test-tooling.md`. Update it when a test-only or build-time
dependency changes.
