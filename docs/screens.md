# Screens

The application has two screens in v1: the **Main Menu** and the **Game
Screen**. This document specifies the contents and layout of each.

## 1. Main Menu

The landing screen. Its sole purpose is to let the player start a new game
at a chosen difficulty.

### Controls

- **New Game: Easy** — Starts a new game at Easy difficulty.
- **New Game: Medium** — Starts a new game at Medium difficulty.
- **New Game: Hard** — Starts a new game at Hard difficulty.

In v1 all three buttons generate a puzzle at the same underlying
difficulty. They remain on the screen as distinct buttons so the UI is
stable across versions; later versions will tune the generator per
button. See [`roadmap.md`](roadmap.md).

### Layout (suggested)

```
+-------------------------------+
|                               |
|         Sudoku Trainer        |
|                               |
|          [   Easy   ]         |
|          [  Medium  ]         |
|          [   Hard   ]         |
|                               |
+-------------------------------+
```

### Transitions

- Pressing any difficulty button generates a new puzzle and transitions
  to the Game Screen.

## 2. Game Screen

Hosts the playable puzzle and all interactive controls.

### Regions

The Game Screen is composed of the following regions, from top to bottom:

1. **Board** — The 9x9 Sudoku grid.
2. **Mode & Color row** — The Pencil toggle and the 8-color picker.
3. **Digit row** — Nine buttons, one for each digit 1–9.
4. **Action row** — Undo, Hint, and Give Up / Solve.

### Controls

#### Board (9x9)

Each of the 81 cells displays one of:

- A **given** number (the puzzle's clue), rendered in black and not
  editable.
- A **player-entered** number, rendered in the color that was active when
  it was placed.
- Up to nine **pencil marks** (1–9) at fixed positions within the cell,
  each rendered in the color that was active when that mark was placed.
- Nothing (empty cell with no marks).

Cell tap behavior is determined by the current mode and the active
digit — see [`game-mechanics.md`](game-mechanics.md). In summary:

- **Single tap, empty cell, pencil off**: writes the active digit.
- **Single tap, empty cell, pencil on**: toggles that digit's pencil
  mark.
- **Single tap, filled cell**: no-op in either mode. Single-tap never
  overwrites or clears a value.
- **Double tap, filled non-given cell**: clears the cell (value only;
  pencil marks are retained).

#### Toggle Pencil

A single toggle button. When **off** (default), cell taps place the active
digit as the cell's value. When **on**, cell taps toggle the active digit
as a pencil mark.

#### Color Picker (8 colors)

Eight color swatches representing the 8 available guess colors. Exactly
one is active at a time. The active color is applied to any new number or
pencil mark the player writes.

- **Default active color**: blue. Reset to blue at the start of every
  new game.
- **Reserved**: black is **not** in the picker. Black is used exclusively
  for the puzzle's starting (given) numbers.

Changing the active color does not recolor anything already on the board;
it only affects subsequent entries.

#### Digit Buttons (1–9)

Nine buttons, one per digit. Exactly one digit is active at a time.
Selecting a digit:

- Makes that digit the **active number** for cell-tap placement.
- **Highlights** all matching numbers on the board (see
  [`game-mechanics.md`](game-mechanics.md) for exact highlight rules).
- **Deactivates** the previously active digit.

A digit's button is rendered in **grey** once nine cells on the board
hold that digit as their value (counting givens and player entries
together). Exhausted digits remain selectable — tapping still
highlights the matching cells and marks. They do not auto-deactivate.

#### Undo

Reverts the most recent move. Pressable repeatedly; the move history is
retained for the whole game, so the player can undo all the way back to
the puzzle's starting position. See
[`game-mechanics.md`](game-mechanics.md) for what counts as a "move."

#### Hint

Reserved for future versions. In v1 the button is present in the UI but
has no effect (or is disabled). See [`roadmap.md`](roadmap.md).

#### Give Up / Solve

A single button whose label changes based on board state:

- **Give Up** (default label) — Ends the current puzzle immediately
  and returns to the Main Menu. The solution is **not** shown.
- **Solve** — Replaces the "Give Up" label once the puzzle is **mostly
  solved**. Pressing it fills in every remaining empty cell with the
  correct solution value (in a distinct "solve" color), waits **five
  seconds** on the completed board, then returns to the Main Menu.

"Mostly solved" requires both (a) at least 70 of 81 cells match the
solution and (b) no cell holds an incorrect value. The exact rules are
in [`game-mechanics.md`](game-mechanics.md).

### Layout (suggested)

```
+-----------------------------------------------+
|  +-----------------------------------------+  |
|  |                                         |  |
|  |            9 x 9  BOARD                 |  |
|  |                                         |  |
|  +-----------------------------------------+  |
|                                               |
|  [Pencil]   [c1][c2][c3][c4][c5][c6][c7][c8] |
|                                               |
|  [1][2][3][4][5][6][7][8][9]                  |
|                                               |
|  [Undo]   [Hint]   [Give Up / Solve]          |
+-----------------------------------------------+
```
