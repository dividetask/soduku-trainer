package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.BoxLineReduction
import com.dividetask.sudokutrainer.domain.BoardGeometry.boxOf
import com.dividetask.sudokutrainer.domain.BoardGeometry.UNITS_ORDERED

/** Finds box-line reductions in a candidate grid. */
internal object BoxLineReductionFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): BoxLineReduction? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<BoxLineReduction> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (BoxLineReduction) -> Boolean,
    ): BoxLineReduction? {
        for (d in 1..9) {
            for (r in 0 until 9) {
                val rowCells = (0 until 9).map { r * 9 + it }
                val cells = rowCells.filter { grid[it] == 0 && d in cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val boxes = cells.map { boxOf(it) }.toSet()
                if (boxes.size != 1) continue
                val boxIdx = boxes.first()
                val boxCells = UNITS_ORDERED[boxIdx].cells
                val plan = mutableListOf<EliminationStep>()
                for (idx in boxCells) {
                    if (idx in cells) continue
                    if (grid[idx] == 0 && d in cands[idx]) {
                        plan += EliminationStep(idx, setOf(d), cells.toSet())
                    }
                }
                if (plan.isNotEmpty()) BoxLineReduction(
                    keyCells = cells.toSet(),
                    unitCells = rowCells.toSet(),
                    plan = plan,
                    digit = d,
                ).let { if (emit(it)) return it }
            }
            for (c in 0 until 9) {
                val colCells = (0 until 9).map { it * 9 + c }
                val cells = colCells.filter { grid[it] == 0 && d in cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val boxes = cells.map { boxOf(it) }.toSet()
                if (boxes.size != 1) continue
                val boxIdx = boxes.first()
                val boxCells = UNITS_ORDERED[boxIdx].cells
                val plan = mutableListOf<EliminationStep>()
                for (idx in boxCells) {
                    if (idx in cells) continue
                    if (grid[idx] == 0 && d in cands[idx]) {
                        plan += EliminationStep(idx, setOf(d), cells.toSet())
                    }
                }
                if (plan.isNotEmpty()) BoxLineReduction(
                    keyCells = cells.toSet(),
                    unitCells = colCells.toSet(),
                    plan = plan,
                    digit = d,
                ).let { if (emit(it)) return it }
            }
        }
        return null
    }
}
