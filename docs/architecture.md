# Architecture

Three Gradle modules:

- **`domain/`** — plain Kotlin, no Android. Game rules, the hint engine,
  the logic grader, the Trainer, and the data types they share. Unit-tested
  on the JVM.
- **`app/`** — the Android client: Jetpack Compose screens, view models
  that drive them, and the Android-backed stores (assets, preferences,
  saved game).
- **`tools/puzzle-gen/`** — the offline generator that writes
  `app/src/main/assets/puzzles/*.json`.

The class list is being split into single-responsibility classes; this
page will list them once that's done.
