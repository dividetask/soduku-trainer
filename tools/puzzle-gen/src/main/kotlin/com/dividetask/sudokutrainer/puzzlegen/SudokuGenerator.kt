package com.dividetask.sudokutrainer.puzzlegen

import com.dividetask.sudokutrainer.domain.LogicSolver
import com.dividetask.sudokutrainer.domain.SudokuSolver
import kotlin.random.Random

class SudokuGenerator(private val random: Random = Random.Default) {

    data class GradedPuzzle(
        val givens: IntArray,
        val solution: IntArray,
        val level: Int,
    )

    fun generateFullGrid(): IntArray {
        val grid = IntArray(81)
        fillRandom(grid, 0)
        return grid
    }

    private fun fillRandom(grid: IntArray, cursor: Int): Boolean {
        if (cursor == 81) return true
        val row = cursor / 9
        val col = cursor % 9
        val order = (1..9).shuffled(random)
        for (d in order) {
            if (SudokuSolver.isValidPlacement(grid, row, col, d)) {
                grid[cursor] = d
                if (fillRandom(grid, cursor + 1)) return true
                grid[cursor] = 0
            }
        }
        return false
    }

    fun carvePuzzle(solution: IntArray, targetEmpties: Int): IntArray {
        require(solution.size == 81)
        val givens = solution.copyOf()
        val order = (0 until 81).shuffled(random)
        var empties = 0
        for (idx in order) {
            if (empties >= targetEmpties) break
            if (givens[idx] == 0) continue
            val saved = givens[idx]
            givens[idx] = 0
            val n = SudokuSolver.countSolutions(givens, limit = 2)
            if (n != 1) {
                givens[idx] = saved
            } else {
                empties++
            }
        }
        return givens
    }

    /**
     * Generate a puzzle, carve it, and grade its difficulty. Returns the
     * result including the logic-solver level (1–9).
     */
    fun generateGraded(targetEmpties: Int): GradedPuzzle {
        val solution = generateFullGrid()
        val givens = carvePuzzle(solution, targetEmpties)
        val result = LogicSolver.grade(givens)
        return GradedPuzzle(
            givens = givens,
            solution = solution,
            level = result.maxLevel,
        )
    }

    /**
     * Generate a puzzle at a specific difficulty level. Retries up to
     * [maxAttempts] times with varying clue counts.
     *
     * The empty-cell target range scales with the level so easier puzzles
     * have more clues and harder puzzles have fewer.
     */
    fun generateAtLevel(targetLevel: Int, maxAttempts: Int = 200): GradedPuzzle? {
        require(targetLevel in 1..9)
        val (minEmpties, maxEmpties) = emptyRangeForLevel(targetLevel)
        for (attempt in 0 until maxAttempts) {
            val empties = minEmpties + random.nextInt(maxEmpties - minEmpties + 1)
            val result = generateGraded(empties)
            if (result.level == targetLevel) return result
        }
        return null
    }

    companion object {
        private fun emptyRangeForLevel(level: Int): Pair<Int, Int> = when (level) {
            1 -> 34 to 46
            2 -> 36 to 50
            3 -> 38 to 56
            4 -> 40 to 58
            5 -> 42 to 60
            6 -> 44 to 62
            7 -> 46 to 62
            8 -> 48 to 64
            9 -> 50 to 64
            else -> 42 to 56
        }
    }
}
