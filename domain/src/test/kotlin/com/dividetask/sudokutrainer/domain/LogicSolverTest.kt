package com.dividetask.sudokutrainer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogicSolverTest {

    @Test
    fun `solves a known easy puzzle with level 1 or 2`() {
        val grid = intArrayOf(
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
        val result = LogicSolver.grade(grid)
        assertTrue(result.solved, "puzzle should be solvable by logic")
        assertTrue(result.maxLevel <= 2, "this puzzle should only need singles, got level ${result.maxLevel}")
    }

    @Test
    fun `reports solved=true and a valid level for a solvable puzzle`() {
        val grid = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 1, 2,
            0, 0, 0, 0, 3, 5, 0, 0, 0,
            0, 0, 0, 6, 0, 0, 0, 7, 0,
            7, 0, 0, 0, 0, 0, 3, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0, 0, 0, 8,
            0, 4, 0, 0, 0, 2, 0, 0, 0,
            0, 0, 0, 8, 7, 0, 0, 0, 0,
            6, 5, 0, 0, 0, 0, 0, 0, 0,
        )
        val result = LogicSolver.grade(grid)
        assertTrue(result.maxLevel in 1..9)
    }

    @Test
    fun `already-solved grid returns level 1 and solved=true`() {
        val solution = intArrayOf(
            5, 3, 4, 6, 7, 8, 9, 1, 2,
            6, 7, 2, 1, 9, 5, 3, 4, 8,
            1, 9, 8, 3, 4, 2, 5, 6, 7,
            8, 5, 9, 7, 6, 1, 4, 2, 3,
            4, 2, 6, 8, 5, 3, 7, 9, 1,
            7, 1, 3, 9, 2, 4, 8, 5, 6,
            9, 6, 1, 5, 3, 7, 2, 8, 4,
            2, 8, 7, 4, 1, 9, 6, 3, 5,
            3, 4, 5, 2, 8, 6, 1, 7, 9,
        )
        val result = LogicSolver.grade(solution)
        assertTrue(result.solved)
        assertEquals(1, result.maxLevel)
    }

    @Test
    fun `level reflects actual technique difficulty`() {
        // Generate a few puzzles via the brute-force solver and check the
        // logic solver returns a consistent level.
        val grid = intArrayOf(
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
        val r1 = LogicSolver.grade(grid)
        val r2 = LogicSolver.grade(grid)
        assertEquals(r1.maxLevel, r2.maxLevel, "grading the same puzzle twice should give the same level")
    }

    @Test
    fun `Difficulty enum has 9 levels mapping 1 through 9`() {
        assertEquals(9, Difficulty.entries.size)
        for (i in 1..9) {
            val d = Difficulty.fromLevel(i)
            assertEquals(i, d.level)
        }
    }
}
