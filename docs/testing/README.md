# Testing

This project's logic lives in the pure-JVM `:domain` module, so the
full test suite runs **without an emulator, an APK, or a device**.

## Running the tests

From the repository root:

```bash
./gradlew :domain:test -PincludeApp=false
```

That single command compiles the domain module and runs every test
listed below. The `-PincludeApp=false` flag skips the Android `:app`
module so you don't need the Google Maven repo or the Android SDK
installed — useful in a fresh checkout or in CI sandboxes without
network access to `dl.google.com`.

If you do have the Android SDK set up locally you can drop the flag
and also exercise the Android module's compile step:

```bash
./gradlew test
```

## What's covered

Each markdown file in this directory describes one test area in plain
English. The corresponding JUnit tests live under
`domain/src/test/kotlin/com/dividetask/sudokutrainer/domain/`. The
intent: a contributor (or LLM) can read the markdown to understand
what each test should prove, then jump straight to the JUnit file to
see how that's encoded.

| Doc | JUnit file | Topic |
|---|---|---|
| `hint-engine-tests.md` | `HintEngineTest.kt` | The technique-finder cascade |
| `reducer-tests.md` | `GameReducerTest.kt` | Board state, taps, pencil mode, color picker |
| `undo-tests.md` | `GameReducerTest.kt` | History semantics for every move type |
| `progressive-hint-tests.md` | `GameReducerTest.kt` | The three-stage Hint button flow |
| `sweep-tests.md` | `GameReducerTest.kt`, `HintEngineTest.kt` | Candidate Sweep behavior |

## Conventions

- One test per assertion ladder. If a test name reads
  `"foo does X when Y"`, the body asserts X under condition Y and
  nothing else.
- Hardcoded `solution` arrays preferred over `SudokuSolver.solveOne`
  because the brute-force solver is exponential on the sparse grids
  these tests use.
- Where a test depends on a specific puzzle grid, the grid is
  inlined as an `IntArray` literal — not loaded from `puzzles/v2.json`
  — so the test stays self-contained.
