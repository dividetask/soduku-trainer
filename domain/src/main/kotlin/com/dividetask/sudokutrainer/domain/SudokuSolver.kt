package com.dividetask.sudokutrainer.domain

/**
 * A simple backtracking Sudoku solver. Used by the puzzle generator to
 * verify solution uniqueness, and potentially by future hint features.
 *
 * Grids are represented as a flat `IntArray(81)` with 0 meaning "empty"
 * and 1..9 meaning a placed digit.
 */
object SudokuSolver {

    /**
     * Count the number of distinct completed solutions of [grid], stopping
     * early once [limit] solutions have been found. [grid] is not
     * modified.
     */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        require(grid.size == 81)
        val work = grid.copyOf()
        var count = 0
        solve(work) {
            count++
            count < limit
        }
        return count
    }

    /**
     * Produce one completed solution of [grid], or null if none exists.
     * [grid] is not modified.
     */
    fun solveOne(grid: IntArray): IntArray? {
        require(grid.size == 81)
        val work = grid.copyOf()
        var result: IntArray? = null
        solve(work) {
            result = work.copyOf()
            false // stop
        }
        return result
    }

    /**
     * DFS backtracking solver. [onSolution] is invoked each time a complete
     * solution is reached; it should return `true` to continue searching
     * for additional solutions or `false` to stop.
     */
    private fun solve(grid: IntArray, onSolution: () -> Boolean): Boolean {
        val idx = pickEmptyWithFewestCandidates(grid)
        if (idx == -1) {
            // Fully filled — this is a solution.
            return onSolution()
        }
        val row = idx / 9
        val col = idx % 9
        for (d in 1..9) {
            if (isValidPlacement(grid, row, col, d)) {
                grid[idx] = d
                val keepGoing = solve(grid, onSolution)
                grid[idx] = 0
                if (!keepGoing) return false
            }
        }
        return true
    }

    /** Pick the empty cell with the fewest legal candidates (MRV heuristic). */
    private fun pickEmptyWithFewestCandidates(grid: IntArray): Int {
        var bestIdx = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            var c = 0
            val row = i / 9
            val col = i % 9
            for (d in 1..9) if (isValidPlacement(grid, row, col, d)) c++
            if (c < bestCount) {
                bestCount = c
                bestIdx = i
                if (c <= 1) return i
            }
        }
        return bestIdx
    }

    fun isValidPlacement(grid: IntArray, row: Int, col: Int, digit: Int): Boolean {
        for (c in 0 until 9) if (grid[row * 9 + c] == digit) return false
        for (r in 0 until 9) if (grid[r * 9 + col] == digit) return false
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (grid[r * 9 + c] == digit) return false
            }
        }
        return true
    }
}
