# Undo Tests

Undo must reverse exactly one move at a time, restoring everything
the move did. Each `Move` subclass has its own undo behavior; this
doc lists what each subclass must restore.

## Move.SetValue

- Restore the cell's previous (empty) state.
- Restore the cell's previous pencil marks via `clearedMarks`.
- Restore every peer cell mark in `autoClearedMarks` (only ever
  non-empty for Blue / Solve placements).

Covered by:
- `writing a value hides the cell's pencil marks and undo restores them`
- `undo of Blue placement restores auto-cleared marks`

## Move.ClearValue

- Re-insert the previously cleared value at the cell.
- Restore the cell's previous color (`prevValueColor`).

Covered by the second half of `double-tap on filled non-given clears
it` (the test also runs the undo and asserts the cell comes back).

## Move.TogglePencilMark

- If the move added a mark, undo removes it.
- If the move removed a mark, undo re-adds it with its original
  color.

Covered by `undo restores pencil mark toggles in sequence`.

## Move.PencilSweep (Candidate Sweep, Naked Pair, etc.)

- Remove every entry in `addedMarks`.
- Restore every entry in `removedMarks` with its original color.

Covered by:
- `Candidate Sweep with initial fill seeds notes, prunes, and undoes
  atomically` — empty board → seed candidates → eliminate → undo →
  board empty of notes again.
- `undo reverses a naked single hint and restores prior pencil marks`
  — the prior naked-single test demonstrates `clearedMarks` undo for
  hint-driven `SetValue` moves.
- `undo reverses a hidden single hint and restores sweep-erased peer
  marks` — exercises both `clearedMarks` and `autoClearedMarks` for
  hint moves.

## Edge cases

- **`undo on empty history is a no-op`** — `assertSame`
  before/after.
- After enough undos to drain history, further undos remain no-ops
  (exercised inside the pencil-toggle test).
