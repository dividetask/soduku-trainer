package com.dividetask.sudokutrainer.domain

/** Builds the per-cell candidate sets the note-based finders search. */
internal object Candidates {
    /**
     * Per-cell candidate set used by pair/triple/wing finders. When the
     * player has pencil marks for a cell, those marks (filtered to the
     * digits still legal given grid values) are the effective candidates
     * — so once a higher-level technique has erased a digit from a peer's
     * marks, the next [find] call no longer treats that digit as a
     * candidate for that cell and the same hint can't be returned again.
     *
     * When a cell has no player marks, fall back to the full set of
     * digits that grid values don't yet rule out.
     */
    fun of(grid: IntArray, marks: Array<Set<Int>>?): Array<Set<Int>> =
        Array(81) { idx ->
            if (grid[idx] != 0) return@Array emptySet()
            val r = idx / 9; val c = idx % 9
            val playerMarks = marks?.get(idx) ?: emptySet()
            if (playerMarks.isNotEmpty()) {
                playerMarks.filterTo(mutableSetOf()) {
                    SudokuSolver.isValidPlacement(grid, r, c, it)
                }
            } else {
                (1..9).filterTo(mutableSetOf()) {
                    SudokuSolver.isValidPlacement(grid, r, c, it)
                }
            }
        }
}
