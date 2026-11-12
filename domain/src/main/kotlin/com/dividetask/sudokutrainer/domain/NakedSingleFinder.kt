package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.NakedSingle
import com.dividetask.sudokutrainer.domain.BoardGeometry.peersHolding

/** Finds naked singles from placed digits. */
internal object NakedSingleFinder {
    fun find(grid: IntArray): NakedSingle? = scan(grid) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray): List<NakedSingle> = buildList { scan(grid) { add(it); false } }

    private fun scan(grid: IntArray, emit: (NakedSingle) -> Boolean): NakedSingle? {
        for (idx in 0 until 81) {
            if (grid[idx] != 0) continue
            val row = idx / 9
            val col = idx % 9
            val candidates = mutableListOf<Int>()
            for (d in 1..9) {
                if (SudokuSolver.isValidPlacement(grid, row, col, d)) candidates += d
            }
            if (candidates.size != 1) continue
            val solution = candidates[0]
            val eliminators = mutableMapOf<Int, Set<Int>>()
            for (d in 1..9) {
                if (d == solution) continue
                eliminators[d] = peersHolding(grid, row, col, d)
            }
            NakedSingle(
                targetCell = idx,
                solutionDigit = solution,
                eliminators = eliminators,
            ).let { if (emit(it)) return it }
        }
        return null
    }
}
