# Design Overview

## Purpose

Sudoku Trainer is a Sudoku game focused on helping players practice the
mechanics of solving puzzles. Beyond simply filling in numbers, it supports
the note-taking and guess-tracking workflows that experienced solvers use:
pencil marks at fixed positions within each cell, and color-coded entries
that let a player tag their guesses and remember which branch of reasoning
a number came from.

## Goals

- Provide a clean two-screen experience: pick a difficulty, then play.
- Support the full set of solving aids a serious player expects: pencil
  marks, colored guesses, number highlighting, and unlimited undo.
- Keep version 1 deliberately small so the core play loop is solid before
  adding intelligent features like hints.

## Non-goals (v1)

- Teaching solving techniques (the Hint button is deferred to a later
  version).
- Multiple difficulty tunings (all three difficulties generate the same
  difficulty of puzzle in v1 — see [`roadmap.md`](roadmap.md)).
- Saving games across sessions, accounts, leaderboards, timers, or
  multiplayer.

## Screens

The app has exactly two screens in v1:

1. **Main Menu** — The landing screen. Displays a button for each
   difficulty (Easy, Medium, Hard). Pressing a difficulty button starts a
   new game and transitions to the Game Screen.
2. **Game Screen** — Hosts the 9x9 board and all interactive controls:
   toggle pencil, color picker (8 colors), the digits 1–9, undo, hint, and
   give up / solve.

The full specification of each screen is in [`screens.md`](screens.md).

## Core Interaction Model

Entry into cells is driven by a single **active number** (1–9) plus a mode
(normal vs. pencil) and a **active color** (one of 8). Tapping a cell
applies the active number to that cell according to the current mode.

- **Normal mode**: writes the active number into the cell as its value, in
  the active color, and hides that cell's pencil marks.
- **Pencil mode**: toggles the active number on or off as a pencil mark in
  the cell. Pencil marks appear at fixed positions (1 = top-left, 2 = top-
  middle, 3 = top-right, 4 = middle-left, and so on through 9 = bottom-
  right).

Selecting a digit button also highlights every occurrence of that digit on
the board (both final values and pencil marks — see [`game-mechanics.md`](
game-mechanics.md) for the precise highlight rules). Only one digit can be
active at a time; selecting a new digit deactivates the previous one.

Starting (given) numbers are always rendered in **black**. The player
cannot select black from the color picker; black is reserved to
distinguish the puzzle's clues from the player's own entries.

## Platform & Stack

Sudoku Trainer is a **native Android app** written in **Kotlin** using
**Jetpack Compose** for the UI. See [`tech-stack.md`](tech-stack.md) for
the full stack, package layout, and the reasoning behind the choice.

## Architectural Outline

The logical modules below are framework-agnostic; the concrete Android
package layout is in [`tech-stack.md`](tech-stack.md).

```
+--------------------------+
|        Main Menu         |
|  [Easy] [Medium] [Hard]  |
+-----------+--------------+
            |
            v
+--------------------------+
|       Game Screen        |
|  +--------------------+  |
|  |    9x9 Board       |  |
|  +--------------------+  |
|  [Pencil] [Colors x8]    |
|  [1][2][3][4][5][6][7][8][9] |
|  [Undo] [Hint] [Give Up] |
+--------------------------+
```

Logical modules:

- **Puzzle Generator / Loader** — Produces a new puzzle (givens and
  solution) for a requested difficulty.
- **Game State** — The 9x9 grid of cells, each with a value, color, and
  pencil marks. Also tracks the active digit, active color, pencil toggle,
  and move history. See [`data-model.md`](data-model.md).
- **Input Controller** — Translates user actions (cell taps, button
  presses) into state transitions, recording each as an undoable move.
- **Renderer** — Draws the board, cell values, pencil marks in their fixed
  positions, highlights for the active digit, and button state.
- **Solver (future)** — Needed for the Hint feature (v2+) and to detect
  when the puzzle is "mostly solved" for the Give Up → Solve transition.

See [`data-model.md`](data-model.md) for the concrete state shape and
[`game-mechanics.md`](game-mechanics.md) for how each control mutates it.
