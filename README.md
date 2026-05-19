# Sudoku Trainer

A Sudoku game and training tool designed to help players practice and
improve at solving Sudoku puzzles. Players can make pencil marks, color-
code guesses, undo moves, and (in future versions) receive hints that
teach solving techniques.

## Status

**Version 1 implementation is in progress.** See
[`docs/roadmap.md`](docs/roadmap.md) for what v1 includes and what is
explicitly out of scope.

## Overview

Native Android app built in **Kotlin + Jetpack Compose**. The app has
two screens:

1. **Main Menu** — Start a new game at a chosen difficulty (Easy, Medium,
   or Hard in v1; all three draw from the same puzzle pool in v1).
2. **Game Screen** — Play the puzzle with a 9x9 board, pencil-mark notes,
   8 guess colors, per-number highlighting, undo, hint, and a give-up /
   solve action.

## Project Layout

```
.
├── app/                   Android app module (Compose UI)
├── domain/                Pure-Kotlin domain logic (state, reducer, solver)
├── tools/puzzle-gen/      Offline Kotlin tool that generates the puzzle pool
├── docs/                  Design documentation
├── gradle/libs.versions.toml   Version catalog
└── settings.gradle.kts
```

The domain module has no Android dependencies so the rules in
[`docs/game-mechanics.md`](docs/game-mechanics.md) can be tested as pure
Kotlin.

## Building

### Android Studio (app, UI, emulator)

1. Open the repository root in Android Studio (Giraffe or newer).
2. Let Gradle sync. The first sync downloads the Android Gradle Plugin
   and Compose dependencies from Google's Maven repo.
3. Run the `app` configuration on an emulator or device (min SDK 26).

### Command line (domain + generator only)

The pure-JVM modules can be built and tested without the Android SDK:

```
./gradlew -PincludeApp=false :domain:test
./gradlew -PincludeApp=false :tools:puzzle-gen:test
```

`-PincludeApp=false` excludes the `:app` module from Gradle configuration,
so the Android Gradle Plugin isn't required.

## Regenerating Puzzles

The 150-entry puzzle pool lives at
`app/src/main/assets/puzzles/v1.json` and was produced by the offline
tool under `tools/puzzle-gen/`. The generator is *not* shipped inside
the app.

To regenerate:

```
./gradlew -PincludeApp=false :tools:puzzle-gen:run \
    --args "--count 150 --seed 20260415 --out app/src/main/assets/puzzles/v1.json"
```

Arguments:
- `--count N` — number of puzzles to generate (default 150).
- `--empties N` — target empty-cell count per puzzle (default 46, i.e.
  35 givens). Uniqueness is verified by solver on every removal.
- `--seed N` — optional RNG seed for reproducible output.
- `--out PATH` — output JSON path, relative to the repo root.

## Design Documentation

All design documents live under [`docs/`](docs/):

- [`docs/design-overview.md`](docs/design-overview.md) — High-level
  product overview, goals, and architectural outline.
- [`docs/screens.md`](docs/screens.md) — Main Menu and Game Screen specs.
- [`docs/game-mechanics.md`](docs/game-mechanics.md) — Per-control
  behavior: number selection, pencil mode, color picker, undo, hint,
  give up / solve.
- [`docs/data-model.md`](docs/data-model.md) — `GameState`, `Cell`,
  `Move` variants, and derived state.
- [`docs/roadmap.md`](docs/roadmap.md) — v1 scope and future versions.
- [`docs/tech-stack.md`](docs/tech-stack.md) — Platform, language,
  framework, and puzzle-source decisions.

## Known v1 Limitations

- The app has no launcher icon yet (Android uses the generic system
  icon). Add one before shipping.
- Portrait-only. No tablet layout.
- No dark mode.
- No save/resume. Leaving the game returns to the Main Menu; the puzzle
  is not persisted.
- The Hint button is present in the UI but disabled. Hints arrive in
  v2.

## License

MIT — see [`LICENSE`](LICENSE).
