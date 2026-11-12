package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.PointingPair
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds pointing pairs in a candidate grid. */
internal object PointingPairFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): PointingPair? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<PointingPair> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (PointingPair) -> Boolean,
    ): PointingPair? {
        for (boxIdx in 0 until 9) {
            val box = UNITS_ORDERED[boxIdx]
            for (d in 1..9) {
                val cells = box.cells.filter { grid[it] == 0 && d in cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val rows = cells.map { it / 9 }.toSet()
                val cols = cells.map { it % 9 }.toSet()
                if (rows.size == 1) {
                    val row = rows.first()
                    val plan = mutableListOf<EliminationStep>()
                    for (c in 0 until 9) {
                        val idx = row * 9 + c
                        if (idx in box.cells) continue
                        if (grid[idx] == 0 && d in cands[idx]) {
                            plan += EliminationStep(idx, setOf(d), cells.toSet())
                        }
                    }
                    if (plan.isNotEmpty()) PointingPair(
                        keyCells = cells.toSet(),
                        unitCells = box.cells.toSet(),
                        plan = plan,
                        digit = d,
                    ).let { if (emit(it)) return it }
                }
                if (cols.size == 1) {
                    val col = cols.first()
                    val plan = mutableListOf<EliminationStep>()
                    for (r in 0 until 9) {
                        val idx = r * 9 + col
                        if (idx in box.cells) continue
                        if (grid[idx] == 0 && d in cands[idx]) {
                            plan += EliminationStep(idx, setOf(d), cells.toSet())
                        }
                    }
                    if (plan.isNotEmpty()) PointingPair(
                        keyCells = cells.toSet(),
                        unitCells = box.cells.toSet(),
                        plan = plan,
                        digit = d,
                    ).let { if (emit(it)) return it }
                }
            }
        }
        return null
    }
}
