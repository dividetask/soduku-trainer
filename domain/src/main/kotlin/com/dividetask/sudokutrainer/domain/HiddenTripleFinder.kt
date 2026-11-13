package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.HiddenTriple
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds hidden triples in a candidate grid. */
internal object HiddenTripleFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): HiddenTriple? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<HiddenTriple> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (HiddenTriple) -> Boolean,
    ): HiddenTriple? {
        for (unit in UNITS_ORDERED) {
            val digitCells: Map<Int, List<Int>> = (1..9).associateWith { d ->
                unit.cells.filter { grid[it] == 0 && d in cands[it] }
            }
            val eligible = digitCells.filter { (_, cells) -> cells.size in 2..3 }.keys.toList()
            if (eligible.size < 3) continue
            for (i in eligible.indices) {
                for (j in i + 1 until eligible.size) {
                    for (k in j + 1 until eligible.size) {
                        val triple = setOf(eligible[i], eligible[j], eligible[k])
                        val union = digitCells[eligible[i]]!! +
                            digitCells[eligible[j]]!! + digitCells[eligible[k]]!!
                        val keys = union.toSet()
                        if (keys.size != 3) continue
                        // Two digits confined to the same two cells are a
                        // hidden pair; report that easier method instead.
                        val ds = listOf(eligible[i], eligible[j], eligible[k])
                        if ((0 until 3).any { x -> (x + 1 until 3).any { y ->
                                digitCells[ds[x]]!!.size == 2 && digitCells[ds[x]] == digitCells[ds[y]]
                            } }
                        ) continue
                        val plan = mutableListOf<EliminationStep>()
                        for (cell in keys) {
                            val doomed = cands[cell] - triple
                            if (doomed.isEmpty()) continue
                            val others = unit.cells.filter {
                                it != cell && grid[it] == 0 && it !in keys
                            }.toSet()
                            plan += EliminationStep(cell, doomed, others)
                        }
                        if (plan.isEmpty()) continue
                        HiddenTriple(
                            keyCells = keys,
                            unitCells = unit.cells.toSet(),
                            plan = plan,
                            tripleDigits = triple,
                        ).let { if (emit(it)) return it }
                    }
                }
            }
        }
        return null
    }
}
