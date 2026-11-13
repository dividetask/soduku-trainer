package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.NakedTriple
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds naked triples in a candidate grid. */
internal object NakedTripleFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): NakedTriple? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<NakedTriple> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (NakedTriple) -> Boolean,
    ): NakedTriple? {
        for (unit in UNITS_ORDERED) {
            val candidates = unit.cells.filter { grid[it] == 0 && cands[it].size in 2..3 }
            if (candidates.size < 3) continue
            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    for (k in j + 1 until candidates.size) {
                        val a = candidates[i]; val b = candidates[j]; val c = candidates[k]
                        val union = cands[a] + cands[b] + cands[c]
                        if (union.size != 3) continue
                        // Two cells with the same two candidates are a naked
                        // pair; report that easier method instead.
                        if (listOf(a, b, c).let { t ->
                                (0 until 3).any { i -> (i + 1 until 3).any { j -> cands[t[i]].size == 2 && cands[t[i]] == cands[t[j]] } }
                            }
                        ) continue
                        val plan = mutableListOf<EliminationStep>()
                        for (cell in unit.cells) {
                            if (cell == a || cell == b || cell == c || grid[cell] != 0) continue
                            val doomed = cands[cell].intersect(union)
                            if (doomed.isEmpty()) continue
                            plan += EliminationStep(cell, doomed, setOf(a, b, c))
                        }
                        if (plan.isEmpty()) continue
                        NakedTriple(
                            keyCells = setOf(a, b, c),
                            unitCells = unit.cells.toSet(),
                            plan = plan,
                            tripleDigits = union,
                        ).let { if (emit(it)) return it }
                    }
                }
            }
        }
        return null
    }
}
