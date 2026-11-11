# Settings, Resume and the Trainer

## Settings

Shown first on the very first launch; afterwards reached from the Main
Menu. Each option is a checkbox, stored on the device, and all are **on**
by default.

| Setting | Effect |
|---------|--------|
| **Autofill starting candidates** | Also fills in the notes on Hidden / Naked Single trainer boards. A new game (or Clear) starts as if Sweep were pressed: every empty cell gets its candidate notes. The sweep is animated or instant according to *Show work*, and it is part of the starting position (Undo doesn't remove it). Placing a Solve-color value still clears that digit's Solve-color notes from its row, column and box, as before. |
| **Show work when giving hint** | When off, a hint's final press applies the result at once instead of playing the animation. The first presses (technique name, then highlighted cells) are unchanged, and Show Again still animates. |
| **Prevent incorrect cell values** | A value placed in the Solve color that isn't the cell's answer is rejected: the board flashes red and the move is undone. Guess colors may still be wrong. |
| **Prevent incorrect notes** | A Solve-color note is rejected the same way if it removes the cell's answer from its notes, or adds a digit already placed correctly in the cell's row, column or box *(interpretation)*. Guess-color notes are not checked. |

Moves made by hints and the opening sweep are never checked.

## Resume

The game in progress is saved to the device after every move, and when
the player leaves with **Main menu** or the back button. The Main Menu
shows **Resume** while a saved game exists. Starting a new game replaces
it; a finished (auto-solved) game deletes it. Hint animations aren't
saved — leaving mid-animation finishes the hint first.

## Trainer

### Method list

One button per solving method, in the order the hint engine tries them:
Hidden Single, Naked Single, Naked Pair, Hidden Pair, Pointing Pair,
Box-Line Reduction, Naked Triple, Hidden Triple, X-Wing, Empty
Rectangle, Swordfish, XY-Wing. (Note Single and Candidate Sweep are
bookkeeping steps, not methods, so they aren't listed.)

### Drill screen

From the top: the Main menu button, the method's name as a header, a
short explanation of how it works (at most three sentences), the board,
the pencil/color and digit rows, then Undo and Hint.

Hidden singles are treated as easier than naked singles everywhere the
hint engine is used: a hint shows a hidden single whenever one exists,
and a Naked Single drill is a board where no hidden single is left.
(Puzzle difficulty grading uses `LogicSolver` and is unaffected.)

### How a drill is built

`TrainerGenerator` picks a puzzle from a difficulty level where the
method can occur, then solves it step by step, always applying the
easiest technique the hint engine finds, until the selected method is
the easiest one left. If the puzzle is solved or gets stuck first, it
tries another puzzle. The board is shown at that point:

- **Hidden / Naked Single** drills show every cell's candidate notes when
  *Autofill starting candidates* is on, and no notes when it is off.
  Notes don't affect judging in these drills.
- Every other method is shown **with all candidate notes** filled in.

### Judging

`TrainerJudge` compares the board with the drill's starting position:

- **Placement methods:** every single on the starting board must be
  placed; the top bar shows progress such as "2/4 solved". A single that the
  player's own placements open up is accepted too. Any other value —
  even a correct one found by a different method — flashes red and is
  undone.
- **Note methods:** the player removes notes. Removing a note that no
  application of the method eliminates, adding a note, or placing a
  value flashes red and is undone. The drill completes once **every**
  note of one application has been removed.
- **Several applications.** When the method applies in more than one
  place (say two different X-Wings), either may be used, and removals
  from both are allowed; the drill completes as soon as one of them is
  fully applied.

On completion the top bar shows "Correct!" and a new drill is dealt.
**Hint** first highlights the pattern's cells, then shows (or applies,
with *Show work* off) the application closest to being finished.
