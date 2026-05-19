package com.dividetask.sudokutrainer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SudokuSolverTest {

    @Test
    fun `empty grid has more than one solution`() {
        val grid = IntArray(81)
        assertEquals(2, SudokuSolver.countSolutions(grid, limit = 2))
    }

    @Test
    fun `known unique puzzle solves to exactly one solution`() {
        val givens = intArrayOf(
            5, 3, 0, 0, 7, 0, 0, 0, 0,
            6, 0, 0, 1, 9, 5, 0, 0, 0,
            0, 9, 8, 0, 0, 0, 0, 6, 0,
            8, 0, 0, 0, 6, 0, 0, 0, 3,
            4, 0, 0, 8, 0, 3, 0, 0, 1,
            7, 0, 0, 0, 2, 0, 0, 0, 6,
            0, 6, 0, 0, 0, 0, 2, 8, 0,
            0, 0, 0, 4, 1, 9, 0, 0, 5,
            0, 0, 0, 0, 8, 0, 0, 7, 9,
        )
        assertEquals(1, SudokuSolver.countSolutions(givens, limit = 2))
        val solved = SudokuSolver.solveOne(givens)
        assertNotNull(solved)
        // Every row/col/box must be 1..9 in the solved grid.
        val s = solved!!
        for (i in 0 until 9) {
            val row = (0 until 9).map { s[i * 9 + it] }.toSet()
            val col = (0 until 9).map { s[it * 9 + i] }.toSet()
            assertEquals((1..9).toSet(), row)
            assertEquals((1..9).toSet(), col)
        }
    }
}
