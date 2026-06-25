# Hint Engine Tests

`HintEngine.find(grid, marks?)` walks the technique cascade in this
order and returns the first technique that has work to do:

```
Naked Single
  -> Hidden Single
    -> Candidate Sweep
      -> Naked Pair  -> Hidden Pair
        -> Pointing Pair  -> Box-Line Reduction
          -> Naked Triple  -> Hidden Triple
            -> X-Wing  -> Empty Rectangle  -> Swordfish  -> XY-Wing
```

Each test below seeds a specific grid (and sometimes player marks)
designed so the next deduction is the technique under test, with
nothing easier shadowing it.

## Singles

- **`finds a naked single and reports its peer eliminators`** —
  classic eight-blockers-around-a-cell setup. The returned hint must
  be `NakedSingle`, the `targetCell` must be `(0,0)`, and every
  non-solution digit must appear in `eliminators` keyed by that digit.
- **`finds a hidden single in a box`** — places four 5s on box 0's
  external peers so 5 can only live at `(0,0)` inside box 0. Returned
  hint must be `HiddenSingle`, `solutionDigit == 5`, `unit.kind == Box`.
- **`prefers naked single over hidden single when both apply`** —
  same setup as the naked-single test but also a hidden single for 9.
  Naked single must win.
- **`Note Single fires when a cell's marks are a single legal digit`**
  — the player has narrowed a cell to one pencil mark (e.g. via an
  earlier pair). Engine must classify it as `NoteSingle` even though
  the grid alone allows many candidates.
- **`Note Single ignores a single mark that's no longer legal`** —
  if the player's lone mark conflicts with a peer's placed value,
  Note Single must not fire (Candidate Sweep will tidy it later).

## Candidate Sweep

- **`empty grid falls through to a fill-only Candidate Sweep`** — no
  givens, no marks. `initialFill` must be true and `plan` may be
  empty (nothing to prune yet).
- **`Candidate Sweep fires with initialFill when no notes exist`** —
  three diagonal givens force a non-empty sweep plan once candidates
  are seeded.
- **`Candidate Sweep returns null when notes exist and nothing to
  prune`** — fully-correct candidates supplied; the engine must skip
  Sweep and try the next finder.

## Level 3+ techniques

For each level-3+ technique we hardcode a grid whose first applicable
deduction is that technique, and assert `HintEngine.find(grid, marks)`
returns the expected subclass. Marks are pre-computed valid
candidates per cell (the same shape `populateValidCandidates` would
produce) so Candidate Sweep returns null and falls through.

- **`Naked Pair on the test-menu grid`** — the same 31-given grid the
  Test menu uses. Returned hint must be `NakedPair`.
- **`Naked Pair stops firing after its eliminations are applied`** —
  call `find()`, apply the returned plan to the marks, call `find()`
  again. The same `NakedPair` (same keyCells) must not come back.
  This guards against the bug where `candidateGrid` ignored player
  marks and the engine looped on the same pair forever.
- **`Hidden Pair on the test-menu grid`** — analogous.
- **`Pointing Pair on the test-menu grid`** — analogous.
- **`Box-Line Reduction on the test-menu grid`** — analogous.
- **`Naked Triple on the test-menu grid`** — analogous.
- **`Hidden Triple on the test-menu grid`** — analogous.
- **`X-Wing on the test-menu grid`** — analogous.
- **`XY-Wing on the test-menu grid`** — analogous.
- **`Empty Rectangle eliminates the corner cell`** — the worked
  example from `Empty Rectangle` in the user-facing docs: box 3
  (rows 3-5, cols 0-2) has all candidates for digit 4 on row 4 or
  col 0; col 3 has exactly two cells with 4 (rows 4 and 8); the
  engine must eliminate 4 from `(8,0)`.

## Termination

- **`returns null on a fully solved grid`** — no candidates anywhere,
  nothing to fill, find() must return null.
