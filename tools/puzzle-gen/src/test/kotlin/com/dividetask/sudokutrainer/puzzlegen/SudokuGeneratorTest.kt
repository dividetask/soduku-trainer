package com.dividetask.sudokutrainer.puzzlegen

import com.dividetask.sudokutrainer.domain.SudokuSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SudokuGeneratorTest {

    @Test
    fun `generated full grids are valid complete sudoku`() {
        val gen = SudokuGenerator(Random(1))
        repeat(5) {
            val g = gen.generateFullGrid()
            // All values 1..9, every row/col/box is a permutation of 1..9.
            assertTrue(g.all { it in 1..9 })
            for (i in 0 until 9) {
                val row = (0 until 9).map { g[i * 9 + it] }.toSet()
                val col = (0 until 9).map { g[it * 9 + i] }.toSet()
                assertEquals((1..9).toSet(), row)
                assertEquals((1..9).toSet(), col)
            }
            for (br in 0 until 3) for (bc in 0 until 3) {
                val box = buildList {
                    for (dr in 0 until 3) for (dc in 0 until 3) {
                        add(g[(br * 3 + dr) * 9 + bc * 3 + dc])
                    }
                }.toSet()
                assertEquals((1..9).toSet(), box)
            }
        }
    }

    @Test
    fun `carved puzzle has a unique solution`() {
        val gen = SudokuGenerator(Random(2))
        repeat(3) {
            val solution = gen.generateFullGrid()
            val givens = gen.carvePuzzle(solution, targetEmpties = 46)
            assertEquals(1, SudokuSolver.countSolutions(givens, limit = 2))
        }
    }
}
