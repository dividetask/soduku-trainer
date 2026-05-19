# Game Mechanics

This document specifies the exact behavior of each interactive control on
the Game Screen. For the data shape that backs these mechanics see
[`data-model.md`](data-model.md). For the layout/screen context see
[`screens.md`](screens.md).

## Global State Affecting Cell Taps

Three pieces of state together determine what happens when the player
taps a cell:

| State            | Values                                  | Default |
|------------------|-----------------------------------------|---------|
| Active digit     | `null` or one of `1..9`                 | `null`  |
| Pencil toggle    | `off` or `on`                           | `off`   |
| Active color     | one of 8 picker colors (no black)       | blue    |

Cells that are **givens** (puzzle clues) never change regardless of
input.

## Digit Buttons (1–9)

Selecting a digit button `N`:

1. Sets the active digit to `N`.
2. Deactivates any previously active digit. Only one digit is active at a
   time.
3. **Highlights** every occurrence of `N` on the board:
   - Every cell whose final value is `N`.
   - Every pencil mark for `N` inside any cell.
4. Highlighting for any previously active digit is cleared.

Tapping the already-active digit again deactivates it (active digit
becomes `null` and highlighting clears). This lets the player clear the
selection without picking another digit.

### Exhausted digits

A digit `N` is **exhausted** once it appears as the final value in all
nine of the cells where it belongs (i.e. nine cells on the board have
`value == N`, regardless of whether those cells are givens or player
entries, and regardless of correctness — the count is simply nine).

- An exhausted digit's button is rendered in **grey** instead of its
  normal style.
- An exhausted digit **does not deactivate automatically**. It remains
  selectable and, when active, still highlights every matching value
  and pencil mark on the board.
- If a cell holding that digit is later cleared (by double-tap, see
  below) or changed, the digit may become non-exhausted again; the
  button returns to its normal style.

## Toggle Pencil

A stateful toggle.

- **Pencil off** (default): cell taps write the active digit as the
  cell's value (subject to the rules below).
- **Pencil on**: cell taps toggle the active digit as a pencil mark on
  empty cells only.

Toggling Pencil is a UI state change only; it is **not** recorded in the
undo history.

## Color Picker

Eight colors are available. Exactly one is the active color. The default
is **blue**.

- The active color is applied to any number or pencil mark written
  **after** the color is selected.
- Changing the active color does not modify anything already on the
  board.
- **Black is reserved** for the puzzle's starting numbers and is not in
  the picker. Player entries can never be black.
- The active color **resets to blue** whenever a new game starts.

Selecting a color is a UI state change only; it is **not** recorded in
the undo history.

## Cell Taps

Given an active digit `N`, the behavior of tapping a non-given cell `C`
is:

### Pencil off (normal mode)

- **If `C` is empty**: set `C.value` to `N`, set `C.valueColor` to the
  active color, clear all pencil marks on `C`, and record the move in
  the undo history.
- **If `C` already has a value** (any value, same or different): the
  tap is a **no-op**. Single-tapping never overwrites or clears an
  existing value. To change a filled cell the player must first clear
  it with a double-tap (see below).

### Pencil on

- **If `C` is empty**: toggle the pencil mark for `N`. If the mark is
  absent, add it with the active color; if present, remove it. Record
  the move in the undo history.
- **If `C` already has a value**: the tap is a **no-op**. Tapping a
  filled cell in pencil mode is assumed to be unintentional and is
  ignored. The value and any stored pencil marks are unchanged.

### No active digit

If the active digit is `null`, tapping a cell has no effect in either
mode.

### Double-tap (clear)

A **double-tap** on a non-given cell `C` clears it, independent of
pencil mode and active digit:

- Sets `C.value` to `null` and `C.valueColor` to `null`.
- Does **not** remove any stored pencil marks (they reappear now that
  the cell is empty).
- Recorded as a single move in the undo history.
- If `C` is already empty, the double-tap is a no-op.
- Givens cannot be cleared; double-tapping a given is a no-op.

Double-tap is the **only** way to clear a filled cell. Single-tap with
a different active digit will not overwrite, and single-tap with the
same active digit will not toggle off.

### Pencil Mark Positions

Each cell is divided into a 3x3 micro-grid. Pencil marks are placed in
fixed positions by digit:

```
+---+---+---+
| 1 | 2 | 3 |
+---+---+---+
| 4 | 5 | 6 |
+---+---+---+
| 7 | 8 | 9 |
+---+---+---+
```

Digit `N` always renders at its fixed slot; there is no reflow when
other marks are added or removed. Pencil marks are only **shown** on
cells with no value; a cell with a value hides its marks, but the
marks are retained in the data model and reappear if the cell is
cleared.

## Highlighting

When a digit `N` is active, the board highlights every occurrence of
`N`:

- Cells whose final value is `N`.
- Pencil marks for `N` within any cell.

The highlight is a visual affordance only; it does not change any cell
data and is not undoable. When the active digit changes, highlights
update immediately. Highlight style (background tint, outline, etc.) is
a rendering concern and is not fixed by this document.

## Conflict warnings

**Not supported in any version.** The game deliberately allows the
player to place incorrect digits without warning — spotting and
correcting mistakes is part of the training. Cells are never flashed,
outlined, or otherwise annotated to indicate conflicts with Sudoku
rules or with the known solution.

## Undo

The Undo button reverts the most recent entry in the move history.

A **move** is any action that changes cell data:

- Writing a value into an empty cell (normal-mode tap).
- Clearing a filled cell via double-tap.
- Adding a pencil mark (on an empty cell, pencil on).
- Removing a pencil mark (on an empty cell, pencil on).
- Clearing a cell's pencil marks as a side effect of setting its value
  (this is recorded as part of the same move as the value write, so a
  single undo restores both the cleared marks and the previous empty
  value).

Actions that are **not** moves (and thus are not undoable):

- Toggling Pencil mode.
- Selecting a color.
- Selecting a digit (active-digit change).
- Any highlighting.
- Taps that resolve to no-ops (e.g. normal-mode tap on a filled cell,
  pencil-on tap on a filled cell, double-tap on an empty cell or a
  given).

Undo can be pressed repeatedly. The history is kept for the entire game,
so the player can undo all the way back to the puzzle's starting
position. Once the board is at the starting position, further undo
presses have no effect.

The Solve and Give Up actions end the game and clear the undo history —
see below.

## Hint

Not implemented in v1. The button is present in the UI but takes no
action (or is disabled/greyed-out). The intent for later versions is
described in [`roadmap.md`](roadmap.md).

## Give Up / Solve

A single button with two labels.

### Label rule

- The button shows **Give Up** by default.
- It shows **Solve** once the puzzle is **mostly solved**, defined in
  v1 as **both** of:
  1. At least **70 of the 81** cells hold the correct solution value
     (counting givens, which are always correct). "Correct" means the
     cell's current value equals the puzzle's known solution for that
     cell.
  2. **Every** non-empty cell holds the correct solution value — that
     is, no cell has an incorrect value. Empty cells are allowed; a
     single incorrect cell prevents the Solve label from appearing.

If the player later changes the board in a way that violates either
condition (e.g. introduces an incorrect digit, or clears enough cells
to drop below 70 correct), the button reverts to **Give Up**.

The threshold (70) is tunable in later versions — see
[`roadmap.md`](roadmap.md).

### Give Up behavior

Pressing **Give Up**:

1. Ends the game immediately. The solution is **not** revealed.
2. Returns to the Main Menu.
3. The current game's state is discarded; there is no resume.

### Solve behavior

Pressing **Solve**:

1. Fills every remaining empty cell with its correct solution value.
   Filled cells are already correct (guaranteed by the label rule), so
   no existing value is changed.
2. The cells filled in by Solve are drawn in a **distinct color**
   reserved for this action (not any of the 8 guess colors and not
   black), so the player can see at a glance which cells the computer
   completed.
3. After a **5-second delay** (during which the completed board is
   visible), the game returns to the Main Menu.
4. The game state is discarded on return; undo is not available during
   the delay.
