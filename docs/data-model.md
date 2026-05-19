# Data Model

This document defines the data structures that back the game. It is
intentionally framework- and language-agnostic; the shapes below can be
translated into whatever types, records, or classes the implementation
chooses.

## Top-level Game State

```
GameState {
  puzzle:        Puzzle            // Immutable definition of this game
  board:         Cell[9][9]        // The current player-facing board
  activeDigit:   1..9 | null       // The selected digit, or none
  pencilOn:      bool              // Pencil mode toggle
  activeColor:   Color             // Currently selected guess color
  history:       Move[]            // Ordered list of player moves
  phase:         "playing"
               | "solved"
               | "gave_up"
}
```

## Puzzle

A puzzle is defined by its givens and its solution. In v1 puzzles are
loaded from a bundled pre-generated set (see
[`tech-stack.md`](tech-stack.md) and [`roadmap.md`](roadmap.md)); v3
will introduce an on-device generator producing the same shape.

```
Puzzle {
  id:          string              // Stable identifier for the bundled puzzle
  difficulty:  "Easy" | "Medium" | "Hard"
  givens:      (1..9 | null)[9][9]   // Starting clues; null if empty
  solution:    (1..9)[9][9]          // The unique completed grid
}
```

In v1 all three difficulty options draw from the same pool of puzzles;
the `difficulty` field is still recorded for later use.

## Cell

Each of the 81 cells holds:

```
Cell {
  row:         0..8
  col:         0..8
  given:       bool                 // True if this came from Puzzle.givens
  value:       1..9 | null          // The current filled-in value, if any
  valueColor:  Color | "black"      // "black" iff given; else a guess color
  pencilMarks: PencilMark[]         // Up to 9 entries, one per digit
}
```

Rules:

- If `given` is true, `value` is non-null, `valueColor` is `"black"`, and
  `pencilMarks` is empty. Givens are immutable for the life of the game.
- For non-given cells, `valueColor` is one of the 8 picker colors (never
  black).
- `pencilMarks` is a set keyed by digit: each digit 1–9 appears at most
  once. Order does not matter; render position is derived from the
  digit.

### PencilMark

```
PencilMark {
  digit:  1..9
  color:  Color                 // The active color when the mark was set
}
```

Pencil marks render at fixed positions within the cell:

```
+---+---+---+
| 1 | 2 | 3 |
+---+---+---+
| 4 | 5 | 6 |
+---+---+---+
| 7 | 8 | 9 |
+---+---+---+
```

Pencil marks are visible only when `Cell.value` is `null`. A cell with a
value hides its marks; the marks are retained in data and reappear if
the value is later cleared.

## Colors

There are 8 guess colors, selectable via the color picker.

```
Color = one of { Blue, Red, Green, Orange, Purple, Teal, Pink, Yellow }
```

- The above palette is a placeholder; the final 8 colors are a visual-
  design decision. Whatever set is chosen, `activeColor` always points
  to one of them.
- **Default** `activeColor` at game start: `Blue`. The active color is
  reset to `Blue` at the start of every new game, regardless of what
  the player had selected in the previous game.
- **Reserved** `"black"` is **not** in the `Color` set. Black is only
  used as `Cell.valueColor` for givens.
- **Reserved "solve" color**: cells filled in by the Solve action use a
  distinct color that is not in the 8-color picker and is not black.
  This is a rendering concern; in the data model these cells can carry
  a dedicated `valueColor` value such as `"solve"` to signal this.

## Moves & Undo History

Every action that changes cell data is recorded as a `Move`. Moves that
do not change cell data (toggling Pencil, choosing a color, choosing an
active digit, highlight updates) are **not** recorded.

```
Move = SetValueMove | ClearValueMove | TogglePencilMarkMove
```

### SetValueMove

Records a normal-mode single tap on an empty cell that writes a value.
Captures any pencil marks that were cleared as a side effect so a
single undo fully reverses it.

```
SetValueMove {
  kind:             "set_value"
  row, col:         0..8
  newValue:         1..9
  newValueColor:    Color
  clearedMarks:     PencilMark[]          // Pencil marks removed by this write
}
```

Single-tap never overwrites an existing value (see
[`game-mechanics.md`](game-mechanics.md)), so `prevValue` is always
`null` and is not stored; undo restores the cell to empty.

### ClearValueMove

Records a double-tap that cleared a filled non-given cell. Pencil marks
are **not** affected by a clear, so only the prior value/color is
captured.

```
ClearValueMove {
  kind:             "clear_value"
  row, col:         0..8
  prevValue:        1..9
  prevValueColor:   Color
}
```

### TogglePencilMarkMove

Records a pencil-mode tap on an empty cell. Either added or removed the
mark for one digit in one cell.

```
TogglePencilMarkMove {
  kind:       "toggle_pencil"
  row, col:   0..8
  digit:      1..9
  added:      bool          // true = mark was added, false = removed
  markColor:  Color         // Color used when adding; color of the removed mark otherwise
}
```

### Undo Semantics

`Undo` pops the most recent `Move` and applies its inverse:

- `SetValueMove`: clear the cell's value; re-add every `PencilMark` in
  `clearedMarks`.
- `ClearValueMove`: restore `prevValue` and `prevValueColor`.
- `TogglePencilMarkMove`: if `added` is true, remove the mark; if
  `added` is false, add the mark back with `markColor`.

Undo can be applied repeatedly down to the puzzle's starting state.
Once the history is empty, Undo has no effect.

Tap interactions that resolve to no-ops (e.g. single-tapping a filled
cell, pencil-mode tapping a filled cell, double-tapping an empty cell
or a given) do not produce a `Move` and are not pushed to history.

## End-of-Game Transitions

Give Up and Solve are not individual moves and do not push to history.

- **Give Up**: the game ends immediately and the app returns to the
  Main Menu. `GameState` is discarded. The solution is **not** written
  to the board.
- **Solve**: the app fills in the remaining empty cells with the
  solution value (using the reserved "solve" color described above),
  waits 5 seconds, and then returns to the Main Menu. During the 5-
  second delay the board is displayed but no user interaction is
  accepted; `history` is considered cleared and Undo is not available.

Because both actions end the game and discard state, there is no
persistent `"gave_up"` or `"solved"` phase to track beyond the delay.
An in-memory `phase` flag is still useful while the Solve delay is
running:

```
phase:  "playing" | "solving_delay"
```

## Derived / Rendering State (non-persisted)

These are computed from `GameState` at render time, not stored:

- **Highlighted cells/marks**: determined by `activeDigit`.
- **Solve/Give Up label**: determined by the two mostly-solved
  conditions in [`game-mechanics.md`](game-mechanics.md): (a) at least
  70 of 81 cells match the solution, and (b) no cell has an incorrect
  value.
- **Digit button greying**: a digit button is rendered in grey when
  exactly nine cells on the board have that digit as their value
  (counting givens and player entries equally).
- **Hint button state**: always inactive in v1.
