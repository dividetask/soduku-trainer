package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.EliminationStep
import com.dividetask.sudokutrainer.domain.Hint.EmptyRectangle

/** Finds empty rectangles in a candidate grid. */
internal object EmptyRectangleFinder {
    fun find(grid: IntArray, cands: Array<Set<Int>>): EmptyRectangle? = scan(grid, cands) { true }

    /** Every instance, in the order [find] considers them. */
    fun all(grid: IntArray, cands: Array<Set<Int>>): List<EmptyRectangle> = buildList { scan(grid, cands) { add(it); false } }

    /** Calls [emit] on each instance in search order; stops once it returns true. */
    private fun scan(
        grid: IntArray,
        cands: Array<Set<Int>>,
        emit: (EmptyRectangle) -> Boolean,
    ): EmptyRectangle? {
        for (d in 1..9) {
            for (boxIdx in 0 until 9) {
                val boxRow0 = (boxIdx / 3) * 3
                val boxCol0 = (boxIdx % 3) * 3
                val boxRows = boxRow0..boxRow0 + 2
                val boxCols = boxCol0..boxCol0 + 2
                val boxCells = buildList {
                    for (r in boxRows) for (c in boxCols) add(r * 9 + c)
                }
                val cellsWithD = boxCells.filter { grid[it] == 0 && d in cands[it] }
                if (cellsWithD.size < 2) continue

                // Find a row R and column C inside the box such that every
                // candidate cell is either on R or on C, with at least one
                // candidate strictly in the row leg AND at least one strictly
                // in the column leg (i.e. not just a pointing pair).
                for (R in boxRows) {
                    for (C in boxCols) {
                        val outsideRC = cellsWithD.filter { it / 9 != R && it % 9 != C }
                        if (outsideRC.isNotEmpty()) continue
                        val inRow = cellsWithD.filter { it / 9 == R && it % 9 != C }
                        val inCol = cellsWithD.filter { it / 9 != R && it % 9 == C }
                        if (inRow.isEmpty() || inCol.isEmpty()) continue

                        // Type A: strong link is a column c outside the box.
                        // Matching endpoint sits on row R, other endpoint is
                        // outside the box -> eliminate d at (otherEndRow, C).
                        for (c in 0..8) {
                            if (c in boxCols) continue
                            val colCells = (0..8).filter { r ->
                                grid[r * 9 + c] == 0 && d in cands[r * 9 + c]
                            }
                            if (colCells.size != 2) continue
                            if (R !in colCells) continue
                            val freeRow = (colCells - R).first()
                            if (freeRow in boxRows) continue
                            val elim = freeRow * 9 + C
                            if (grid[elim] != 0 || d !in cands[elim]) continue

                            val conj = setOf(R * 9 + c, freeRow * 9 + c)
                            EmptyRectangle(
                                keyCells = cellsWithD.toSet() + conj,
                                unitCells = boxCells.toSet() + (0..8).map { it * 9 + c }.toSet(),
                                plan = listOf(EliminationStep(
                                    cellIdx = elim,
                                    doomedDigits = setOf(d),
                                    explainerCells = cellsWithD.toSet() + conj,
                                )),
                                digit = d,
                            ).let { if (emit(it)) return it }
                        }
                        // Type B: strong link is a row r outside the box.
                        for (r in 0..8) {
                            if (r in boxRows) continue
                            val rowCells = (0..8).filter { c ->
                                grid[r * 9 + c] == 0 && d in cands[r * 9 + c]
                            }
                            if (rowCells.size != 2) continue
                            if (C !in rowCells) continue
                            val freeCol = (rowCells - C).first()
                            if (freeCol in boxCols) continue
                            val elim = R * 9 + freeCol
                            if (grid[elim] != 0 || d !in cands[elim]) continue

                            val conj = setOf(r * 9 + C, r * 9 + freeCol)
                            EmptyRectangle(
                                keyCells = cellsWithD.toSet() + conj,
                                unitCells = boxCells.toSet() + (0..8).map { r * 9 + it }.toSet(),
                                plan = listOf(EliminationStep(
                                    cellIdx = elim,
                                    doomedDigits = setOf(d),
                                    explainerCells = cellsWithD.toSet() + conj,
                                )),
                                digit = d,
                            ).let { if (emit(it)) return it }
                        }
                    }
                }
            }
        }
        return null
    }
}
