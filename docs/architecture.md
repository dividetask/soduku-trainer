# Architecture

Three Gradle modules:

- **`domain/`** — plain Kotlin, no Android. Unit-tested on the JVM.
- **`app/`** — the Android client (Jetpack Compose).
- **`tools/puzzle-gen/`** — the offline generator that writes
  `app/src/main/assets/puzzles/*.json`.

Every concept is its own small class with one job. Data holders carry
data and no flow logic; flow classes carry no data definitions.

## domain — data holders

| Class | Holds |
|-------|-------|
| `Puzzle`, `Difficulty` | A puzzle's givens and solution; the nine levels. |
| `Cell`, `CellColor`, `GuessColor` | One board cell and the colors its value / notes can have. |
| `GameState` | A game in progress: cells, undo history, phase, hint overlay. |
| `Move`, `AutoClearedMark` | One undoable move. |
| `Hint` (and its nested types) | A technique found on the board, with what's needed to teach it. |
| `HintUi`, `PendingHint` | Hint animation / staged-hint state shown by the board. |
| `SavedGame` | The persistent part of a game, for Resume. |
| `TrainerMethod`, `TrainerDrill`, `NoteRef` | A drillable method; one drill board and its targets; a (cell, digit) pair. |

## domain — flow

| Class | Job |
|-------|-----|
| `BoardMoves` | Player moves: select digit / color, pencil toggle, place, clear, undo, reset. |
| `PendingHints` | The first Hint presses: name the technique, then highlight its cells. |
| `NakedSingleSteps`, `HiddenSingleSteps`, `NoteSingleSteps`, `CandidateSweepSteps`, `EliminationSteps` | Board states for each hint animation (begin, advance, complete). |
| `AutoSolve` | Finishing a mostly-solved board. |
| `EntryCheck` | The "prevent incorrect values / notes" rules. |
| `HintEngine` | Tries the techniques in order, easiest first. |
| `HiddenSingleFinder`, `NakedSingleFinder`, `NoteSingleFinder`, `CandidateSweepFinder` | Find one technique each (placements and sweeps). |
| `NakedPairFinder`, `HiddenPairFinder`, `PointingPairFinder`, `BoxLineReductionFinder`, `NakedTripleFinder`, `HiddenTripleFinder`, `XWingFinder`, `EmptyRectangleFinder`, `SwordfishFinder`, `XYWingFinder` | Find one note-elimination technique each. |
| `Candidates`, `BoardGeometry` | Candidate sets for the finders; rows, columns, boxes and peers. |
| `LogicSolver` | Grades a puzzle's difficulty (used by the generator). |
| `SudokuSolver` | Backtracking solver: uniqueness checks. |
| `PuzzleTransformer` | Random symmetry transform of a puzzle. |
| `TrainerGenerator` | Solves a puzzle forward until a method is the easiest step; builds the drill. |
| `TrainerJudge` | Judges trainer moves and progress. |

## app

| Class | Job |
|-------|-----|
| `PuzzleRepository` | Loads the puzzle pool and deals puzzles. |
| `GameConfig` | Numbers and flags from `config.yaml`. |
| `Settings` / `SettingsStore` | Player options (data) / their persistence. |
| `SavedGameStore` | The saved game on disk. |
| `BoardStore` | The board a play screen shows, observable. |
| `HintAnimator` | Plays a hint's animation, or applies it at once. |
| `HintFlow` | The game's Hint and Show Again buttons. |
| `BoardOverlays` | Red flash and mistake highlight. |
| `StartingSweep` | The opening Candidate Sweep. |
| `AutoSolver` | Watches for a mostly-solved board and finishes it. |
| `GameSaver` | Keeps the saved game in step with the board. |
| `GameViewModel` | Wires the game's parts to the Game screen. |
| `TrainerViewModel` | Deals drills and judges moves for the drill screen. |
| Screens (`MainMenuScreen`, `SettingsScreen`, `GameScreen`, `TrainerMenuScreen`, `TrainerScreen`) and widgets (`Board`, `Controls`, `VersionLabel`, `CelebrationOverlay`) | Compose UI only. |

Platform concerns (assets, preferences, files) stay in `app/`; everything
in `domain/` runs without Android.
