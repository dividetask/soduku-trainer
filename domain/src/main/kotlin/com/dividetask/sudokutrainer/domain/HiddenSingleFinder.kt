package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.HiddenSingle
import com.dividetask.sudokutrainer.domain.BoardGeometry.peersOutsideUnitHolding
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds hidden singles from placed digits. */
internal object HiddenSingleFinder {
    fun find(grid: IntArray): HiddenSingle? = scan(grid) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray): List<HiddenSingle> = buildList { scan(grid) { add(it); false } }

    private fun scan(grid: IntArray, emit: (HiddenSingle) -> Boolean): HiddenSingle? {
        // Iteration order encodes the tie-break: boxes are tried before
        // rows, rows before columns — so when the same digit is a hidden
        // single in multiple units the box deduction wins.
        for (unit in UNITS_ORDERED) {
            val unitValues = unit.cells.mapTo(mutableSetOf()) { grid[it] }
            for (d in 1..9) {
                if (d in unitValues) continue
                var target = -1
                var multiple = false
                for (idx in unit.cells) {
                    if (grid[idx] != 0) continue
                    val row = idx / 9
                    val col = idx % 9
                    if (!SudokuSolver.isValidPlacement(grid, row, col, d)) continue
                    if (target == -1) {
                        target = idx
                    } else {
                        multiple = true
                        break
                    }
                }
                if (target == -1 || multiple) continue
                val eliminators = buildMap {
                    val unitSet = unit.cells.toSet()
                    for (idx in unit.cells) {
                        if (idx == target || grid[idx] != 0) continue
                        put(idx, peersOutsideUnitHolding(grid, idx, unitSet, d))
                    }
                }
                HiddenSingle(
                    targetCell = target,
                    solutionDigit = d,
                    unit = unit,
                    eliminators = eliminators,
                ).let { if (emit(it)) return it }
            }
        }
        return null
    }
}
