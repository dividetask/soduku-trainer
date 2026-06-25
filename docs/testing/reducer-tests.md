# Reducer Tests

`GameReducer` is a pure state machine. Every test starts from
`GameState.forNewGame(puzzle)`, applies a sequence of reducer calls,
and asserts on the resulting `GameState`. No coroutines, no Android,
no animator.

## Digit selection

- **`selectDigit sets active and re-selecting deactivates`** —
  selecting 5 sets `activeDigit = 5`; selecting 5 again clears it;
  selecting 3 then 7 leaves `activeDigit = 7`.

## Cell taps (pencil off)

- **`normal-mode tap on empty cell writes value in active color`** —
  the value, color, and history entry are all set.
- **`normal-mode tap on filled cell is a no-op`** — no overwrite, no
  history growth.
- **`normal-mode tap on given is a no-op`** — givens never change.
- **`writing a value hides the cell's pencil marks and undo restores
  them`** — `Move.SetValue.clearedMarks` round-trips through undo.
- **`Blue (Solve) placement auto-clears Blue pencil marks from
  peers`** — the autoClearedMarks list is populated; non-Blue marks
  survive.
- **`undo of Blue placement restores auto-cleared marks`** —
  one-press round trip including peer-mark restoration.
- **`non-Blue placement does not auto-clear`** — color check guards the
  side effect.

## Cell taps (pencil on)

- **`pencil-mode tap on empty cell toggles the mark`** — twice
  removes; once adds.
- **`pencil-mode tap on filled cell is a no-op`** — same as
  pencil-off behavior for filled cells.
- **`undo restores pencil mark toggles in sequence`** — three
  toggles in, three undos back, history empties to no-op.

## Double-tap

- **`double-tap on filled non-given clears it`** — `value` becomes
  null, `valueColor` becomes null, history records `ClearValue`.
- **`double-tap on empty cell and on given is a no-op`** —
  `assertSame` on the state both times.

## Active-color / pencil-mode toggles

- **`toggling pencil and selecting color are not undoable`** — no
  history entries appear; subsequent undo is a no-op.

## Auto-solve / Solve / Give Up

- **`applySolve fills empty cells in Solve color and transitions
  phase`** — player entries keep their Guess color; empty cells get
  `CellColor.Solve`; phase moves to `Celebrating`; history clears.

## Exhausted digits / mostly-solved threshold

- **`digit is exhausted when nine cells hold it`** — drive nine taps
  to fill every solution-5 cell; `isDigitExhausted(5)` becomes true.
- **`mostly solved requires threshold AND no incorrect entries`** —
  fill the board correctly, assert `isMostlySolved(10)`; introduce
  one wrong digit, assert it becomes false.

## Show Again / lastHint

- **`lastHint is set on completion and survives an undo`** — after a
  hint animation `lastHint` equals the hint object; pressing Undo
  pops the hint's move but keeps `lastHint` so Show Again can replay.
- **`lastHint clears on any player cell change`** — once the player
  places a value (or tap-clears one) Show Again should disable, so
  `lastHint` must become null on that move.
- **`completion with countAsHint false leaves hintCount unchanged but
  sets lastHint`** — the Show Again replay path and the Sweep button
  both rely on this flag.

## Reset

- **`resetBoard returns to starting position`** — history wiped,
  cells back to givens-only, phase Playing.
