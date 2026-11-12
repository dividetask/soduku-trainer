package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.NoteSingle

/** Finds a cell whose notes are down to one legal digit. */
internal object NoteSingleFinder {
    /**
     * Walks empty cells and reports the first one whose player marks are
     * exactly one digit (and that digit is still a legal placement). The
     * lone mark *might* be smaller than the grid-computed candidate set,
     * which is why this fires between Hidden Single and Candidate Sweep
     * — the player has done elimination work that grid values alone
     * don't reflect.
     */
    fun find(grid: IntArray, marks: Array<Set<Int>>?): NoteSingle? {
        if (marks == null) return null
        for (idx in 0 until 81) {
            if (grid[idx] != 0) continue
            val cellMarks = marks[idx]
            if (cellMarks.size != 1) continue
            val d = cellMarks.first()
            val r = idx / 9; val c = idx % 9
            if (!SudokuSolver.isValidPlacement(grid, r, c, d)) continue
            return NoteSingle(targetCell = idx, solutionDigit = d)
        }
        return null
    }
}
