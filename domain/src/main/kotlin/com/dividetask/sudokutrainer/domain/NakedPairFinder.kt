package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.NakedPair
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds naked pairs in a candidate grid. */
internal object NakedPairFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): NakedPair? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<NakedPair> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (NakedPair) -> Boolean,
    ): NakedPair? {
        for (unit in UNITS_ORDERED) {
            val pairCells = unit.cells.filter { grid[it] == 0 && cands[it].size == 2 }
            for (i in pairCells.indices) {
                for (j in i + 1 until pairCells.size) {
                    val a = pairCells[i]; val b = pairCells[j]
                    if (cands[a] != cands[b]) continue
                    val digits = cands[a]
                    val plan = mutableListOf<EliminationStep>()
                    for (cell in unit.cells) {
                        if (cell == a || cell == b || grid[cell] != 0) continue
                        val doomed = cands[cell].intersect(digits)
                        if (doomed.isEmpty()) continue
                        plan += EliminationStep(
                            cellIdx = cell,
                            doomedDigits = doomed,
                            explainerCells = setOf(a, b),
                        )
                    }
                    if (plan.isEmpty()) continue
                    NakedPair(
                        keyCells = setOf(a, b),
                        unitCells = unit.cells.toSet(),
                        plan = plan,
                        pairDigits = digits,
                    ).let { if (emit(it)) return it }
                }
            }
        }
        return null
    }
}
