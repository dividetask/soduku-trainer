# Screens

The application has these screens: **Settings**, the **Main Menu**, the
**Game Screen**, and the **Trainer** (a method list plus a drill
screen). Settings and the Trainer are specified in
[`settings-and-trainer.md`](settings-and-trainer.md); this document
covers the Main Menu and Game Screen.

On the very first launch the app opens on Settings; its **Done** button
(enabled once a puzzle difficulty is picked) leads to the Main Menu.
Every later launch opens on the Main Menu.

The Game Screen and the Trainer drill screen show the app's version
(`BuildConfig.VERSION_NAME`, e.g. `v2.015 a (e687)`) in small grey text
at the bottom, so screenshots identify the build.

## 1. Main Menu

The landing screen.

### Controls

- **Settings** — A gear icon in the top-right corner; opens Settings,
  whose Done returns here.
- **Resume** — Shown only when an unfinished game is saved; continues
  the most recent game exactly where it was left, undo history included.
- **New Game** — Starts a new game at the difficulty chosen in Settings.
  Starting a new game replaces any saved game.
- **Lesson** — Opens the Trainer's method list.

### Layout (suggested)

```
+-------------------------------+
|                          [⚙]  |
|         Sudoku Trainer        |
|          [  Resume  ]         |
|          [ New Game ]         |
|          [  Lesson  ]         |
+-------------------------------+
```

## 2. Game Screen

Hosts the playable puzzle and all interactive controls.

### Regions

The Game Screen is composed of the following regions, from top to bottom:

1. **Top bar** — A **Main menu** button, which saves the game for Resume
   and returns to the Main Menu, and the name of the hint being shown.
   The system back button does the same as Main menu.
2. **Board** — The 9x9 Sudoku grid.
3. **Mode & Color row** — The Pencil toggle and the color picker.
4. **Digit row** — Nine buttons, one for each digit 1–9.
5. **Action buttons** — Two columns: Undo / Hint, then Show Again / Clear.

There is no Sweep button: with the *Autofill starting candidates*
setting on, each game starts as if Sweep were pressed. There is no Give
Up or Solve button either: leave with Main menu, and the game finishes
itself once only a few correct cells remain (`solve_criteria` in
`config.yaml`).

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

### Layout (suggested)

```
+-----------------------------------------------+
|  [Main menu]        Hint name                 |
|  +-----------------------------------------+  |
|  |                                         |  |
|  |            9 x 9  BOARD                 |  |
|  |                                         |  |
|  +-----------------------------------------+  |
|                                               |
|  [Pencil] [Solve] [c1][c2][c3][c4][c5]       |
|                                               |
|  [1][2][3][4][5][6][7][8][9]                  |
|                                               |
|  [   Undo    ]   [    Hint    ]               |
|  [ Show Again]   [   Clear    ]               |
+-----------------------------------------------+
```
