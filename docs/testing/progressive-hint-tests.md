# Progressive Hint Tests

The Hint button stages a deduction across three presses:

1. **Press 1**: highlight the pattern's key cell(s). Start a 60-second
   timer (config: `pending_hint_ms`).
2. **Press 2**: also reveal the technique name. Restart the 60-second
   timer.
3. **Press 3**: clear the pending state and fire the animation.

In addition, the pending hint clears automatically when:

- the player places **any** value, toggles **any** pencil mark, or
  double-taps to clear **any** cell, or
- the 60-second timer expires.

Candidate Sweep through the Hint button does **not** use this flow
(there's no single target cell); it animates immediately.

## Tests

All in `GameReducerTest.kt`. The timer behavior lives in
`GameViewModel` and isn't reducer-testable, so these focus on the
reducer-side state transitions.

- **`pending hint advances from highlight to method, then clears`**
  - Set pending; assert `showCells == false`, `keyCells` matches.
  - Call `advancePendingHintRevealCells`; assert `showCells == true`.
  - Call again; assert `assertSame` (no-op).
  - Call `clearPendingHint`; assert `pendingHint == null`.

- **`placing a value in the pending-hint target clears the hint`**
  - Set pending for a Naked Single's `targetCell`.
  - `selectDigit(wrong)` + `tapCell(target)` — the cell now has the
    wrong value AND `pendingHint` is null.

- **`any cell change clears the pending hint`**
  - Set pending with multi-cell `keyCells`.
  - Tapping a value into a key cell clears it.
  - Tapping a value into a non-key cell also clears it.
  - Toggling a pencil mark clears it.
  - Double-tapping a filled cell to clear it also clears the hint.
