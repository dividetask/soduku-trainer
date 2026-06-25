# Candidate Sweep Tests

Candidate Sweep is both a hint technique and a standalone player
action (the Sweep button). Both paths share the same reducer
machinery but differ in whether `hintCount` increments.

## Engine behavior

In `HintEngineTest.kt`:

- `empty grid falls through to a fill-only Candidate Sweep`
- `Candidate Sweep fires with initialFill when no notes exist`
- `Candidate Sweep returns null when notes exist and nothing to prune`
- `Test-menu Candidate Sweep grid yields a CandidateSweep`

## Reducer + animation

In `GameReducerTest.kt`:

- **`Candidate Sweep with initial fill seeds notes, prunes, and undoes
  atomically`** — runs through `beginCandidateSweep` ->
  `applyCandidateSweepFill` -> sequential `advanceCandidateSweepCell`
  for each step -> `completeCandidateSweep`. Confirms:
  - During fill, every empty cell receives all 1..9 marks.
  - After the sweep, peer-blocked digits are gone.
  - A `Move.PencilSweep` is appended.
  - One undo restores the *original* empty-of-notes state.

- **`Candidate Sweep with countAsHint=false leaves hintCount
  unchanged`** — the Sweep button path increments history but not
  `hintCount`. Still appends a `Move.PencilSweep` so the player can
  undo it.

## Idempotency

The animator's `skip()` path re-applies every step before calling
the completion reducer, and the reducer is structured so that:

- `applyCandidateSweepFill` skips cells that already have any marks.
- `advanceCandidateSweepCell` `filterKeys` is idempotent w.r.t. the
  doomed-digits set.

These aren't asserted by a dedicated test today; if `skip()` ever
gets a reduce-twice regression, the existing reducer flow test would
catch the visible symptom (extra marks added on the second pass).
