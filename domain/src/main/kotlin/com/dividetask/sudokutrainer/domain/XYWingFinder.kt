package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.XYWing
import com.dividetask.sudokutrainer.domain.BoardGeometry.PEERS

/** Finds XY-Wings in a candidate grid. */
internal object XYWingFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): XYWing? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<XYWing> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (XYWing) -> Boolean,
    ): XYWing? {
        for (pivot in 0 until 81) {
            if (grid[pivot] != 0 || cands[pivot].size != 2) continue
            val pc = cands[pivot].toList()
            val a = pc[0]; val b = pc[1]
            val peers = PEERS[pivot]
            val pincerA = mutableListOf<Int>() // {a, c}
            val pincerB = mutableListOf<Int>() // {b, c}
            for (p in peers) {
                if (grid[p] != 0 || cands[p].size != 2) continue
                val pcs = cands[p]
                if (a in pcs && b !in pcs) pincerA += p
                if (b in pcs && a !in pcs) pincerB += p
            }
            for (pa in pincerA) {
                val cA = (cands[pa] - a).firstOrNull() ?: continue
                for (pb in pincerB) {
                    val cB = (cands[pb] - b).firstOrNull() ?: continue
                    if (cA != cB) continue
                    val shared = cA
                    val seeBoth = PEERS[pa].intersect(PEERS[pb])
                    val plan = mutableListOf<EliminationStep>()
                    for (cell in seeBoth) {
                        if (cell == pivot || grid[cell] != 0) continue
                        if (shared !in cands[cell]) continue
                        plan += EliminationStep(cell, setOf(shared), setOf(pa, pb))
                    }
                    if (plan.isNotEmpty()) XYWing(
                        keyCells = setOf(pivot, pa, pb),
                        unitCells = emptySet(),
                        plan = plan,
                        sharedDigit = shared,
                    ).let { if (emit(it)) return it }
                }
            }
        }
        return null
    }
}
