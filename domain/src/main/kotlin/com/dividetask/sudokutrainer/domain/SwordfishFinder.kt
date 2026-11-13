package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.Swordfish

/** Finds swordfish in a candidate grid. */
internal object SwordfishFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): Swordfish? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<Swordfish> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (Swordfish) -> Boolean,
    ): Swordfish? {
        for (d in 1..9) {
            val rowPos = Array(9) { r ->
                (0 until 9).filter { c -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            val eligibleRows = (0 until 9).filter { rowPos[it].size in 2..3 }
            if (eligibleRows.size >= 3) {
                for (i in eligibleRows.indices) for (j in i + 1 until eligibleRows.size)
                    for (k in j + 1 until eligibleRows.size) {
                        val rs = listOf(eligibleRows[i], eligibleRows[j], eligibleRows[k])
                        val cols = (rowPos[rs[0]] + rowPos[rs[1]] + rowPos[rs[2]]).toSet()
                        if (cols.size != 3) continue
                        // Two rows sharing the same two columns are an X-Wing.
                        if (containsXWing(rs.map { rowPos[it] })) continue
                        val keys = buildSet {
                            for (r in rs) for (c in cols) {
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) add(idx)
                            }
                        }
                        val plan = mutableListOf<EliminationStep>()
                        for (c in cols) {
                            for (r in 0 until 9) {
                                if (r in rs) continue
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) {
                                    plan += EliminationStep(idx, setOf(d), keys)
                                }
                            }
                        }
                        if (plan.isNotEmpty()) Swordfish(
                            keyCells = keys,
                            unitCells = rs.flatMap { r -> (0 until 9).map { r * 9 + it } }.toSet(),
                            plan = plan,
                            digit = d,
                        ).let { if (emit(it)) return it }
                    }
            }
            val colPos = Array(9) { c ->
                (0 until 9).filter { r -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            val eligibleCols = (0 until 9).filter { colPos[it].size in 2..3 }
            if (eligibleCols.size >= 3) {
                for (i in eligibleCols.indices) for (j in i + 1 until eligibleCols.size)
                    for (k in j + 1 until eligibleCols.size) {
                        val cs = listOf(eligibleCols[i], eligibleCols[j], eligibleCols[k])
                        val rows = (colPos[cs[0]] + colPos[cs[1]] + colPos[cs[2]]).toSet()
                        if (rows.size != 3) continue
                        if (containsXWing(cs.map { colPos[it] })) continue
                        val keys = buildSet {
                            for (c in cs) for (r in rows) {
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) add(idx)
                            }
                        }
                        val plan = mutableListOf<EliminationStep>()
                        for (r in rows) {
                            for (c in 0 until 9) {
                                if (c in cs) continue
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) {
                                    plan += EliminationStep(idx, setOf(d), keys)
                                }
                            }
                        }
                        if (plan.isNotEmpty()) Swordfish(
                            keyCells = keys,
                            unitCells = cs.flatMap { c -> (0 until 9).map { it * 9 + c } }.toSet(),
                            plan = plan,
                            digit = d,
                        ).let { if (emit(it)) return it }
                    }
            }
        }
        return null
    }

    /**
     * Whether two of the three lines' candidate positions fit in two
     * cross lines — an X-Wing hiding inside the Swordfish.
     */
    private fun containsXWing(lines: List<List<Int>>): Boolean =
        (0 until 3).any { i -> (i + 1 until 3).any { j -> (lines[i] + lines[j]).toSet().size <= 2 } }
}
