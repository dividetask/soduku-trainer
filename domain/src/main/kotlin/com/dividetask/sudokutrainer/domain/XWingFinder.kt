package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.XWing

/** Finds X-Wings in a candidate grid. */
internal object XWingFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): XWing? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<XWing> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (XWing) -> Boolean,
    ): XWing? {
        for (d in 1..9) {
            // Row-based: two rows where d's candidates lie in exactly the same two columns.
            val rowPos = Array(9) { r ->
                (0 until 9).filter { c -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            for (r1 in 0 until 9) {
                if (rowPos[r1].size != 2) continue
                for (r2 in r1 + 1 until 9) {
                    if (rowPos[r2] != rowPos[r1]) continue
                    val (c1, c2) = rowPos[r1]
                    val corners = setOf(r1 * 9 + c1, r1 * 9 + c2, r2 * 9 + c1, r2 * 9 + c2)
                    val plan = mutableListOf<EliminationStep>()
                    for (r in 0 until 9) {
                        if (r == r1 || r == r2) continue
                        for (c in listOf(c1, c2)) {
                            val idx = r * 9 + c
                            if (grid[idx] == 0 && d in cands[idx]) {
                                plan += EliminationStep(idx, setOf(d), corners)
                            }
                        }
                    }
                    if (plan.isNotEmpty()) XWing(
                        keyCells = corners,
                        unitCells = ((0 until 9).map { r1 * 9 + it } +
                            (0 until 9).map { r2 * 9 + it }).toSet(),
                        plan = plan,
                        digit = d,
                    ).let { if (emit(it)) return it }
                }
            }
            // Column-based.
            val colPos = Array(9) { c ->
                (0 until 9).filter { r -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            for (c1 in 0 until 9) {
                if (colPos[c1].size != 2) continue
                for (c2 in c1 + 1 until 9) {
                    if (colPos[c2] != colPos[c1]) continue
                    val (r1, r2) = colPos[c1]
                    val corners = setOf(r1 * 9 + c1, r1 * 9 + c2, r2 * 9 + c1, r2 * 9 + c2)
                    val plan = mutableListOf<EliminationStep>()
                    for (c in 0 until 9) {
                        if (c == c1 || c == c2) continue
                        for (r in listOf(r1, r2)) {
                            val idx = r * 9 + c
                            if (grid[idx] == 0 && d in cands[idx]) {
                                plan += EliminationStep(idx, setOf(d), corners)
                            }
                        }
                    }
                    if (plan.isNotEmpty()) XWing(
                        keyCells = corners,
                        unitCells = ((0 until 9).map { it * 9 + c1 } +
                            (0 until 9).map { it * 9 + c2 }).toSet(),
                        plan = plan,
                        digit = d,
                    ).let { if (emit(it)) return it }
                }
            }
        }
        return null
    }
}
