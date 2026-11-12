package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.HiddenPair
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds hidden pairs in a candidate grid. */
internal object HiddenPairFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): HiddenPair? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<HiddenPair> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (HiddenPair) -> Boolean,
    ): HiddenPair? {
        for (unit in UNITS_ORDERED) {
            val unsolved = unit.cells.filter { grid[it] == 0 }
            val digitCells: Map<Int, List<Int>> = (1..9).associateWith { d ->
                unsolved.filter { d in cands[it] }
            }
            val twoCellDigits = digitCells.filter { (_, cells) -> cells.size == 2 }
            val digits = twoCellDigits.keys.toList()
            for (i in digits.indices) {
                for (j in i + 1 until digits.size) {
                    val d1 = digits[i]; val d2 = digits[j]
                    if (twoCellDigits[d1] != twoCellDigits[d2]) continue
                    val pair = setOf(d1, d2)
                    val keys = twoCellDigits[d1]!!.toSet()
                    val plan = mutableListOf<EliminationStep>()
                    for (cell in keys) {
                        val doomed = cands[cell] - pair
                        if (doomed.isEmpty()) continue
                        // Explainer: the other cells of the unit where d1/d2 can't go.
                        val others = unit.cells.filter {
                            it != cell && grid[it] == 0 && it !in keys
                        }.toSet()
                        plan += EliminationStep(
                            cellIdx = cell,
                            doomedDigits = doomed,
                            explainerCells = others,
                        )
                    }
                    if (plan.isEmpty()) continue
                    HiddenPair(
                        keyCells = keys,
                        unitCells = unit.cells.toSet(),
                        plan = plan,
                        pairDigits = pair,
                    ).let { if (emit(it)) return it }
                }
            }
        }
        return null
    }
}
