package com.dividetask.sudokutrainer.domain

import kotlinx.serialization.Serializable

enum class Difficulty(val level: Int, val label: String) {
    Beginner(1, "Beginner"),
    Easy(2, "Easy"),
    Moderate(3, "Moderate"),
    Intermediate(4, "Intermediate"),
    Tricky(5, "Tricky"),
    Hard(6, "Hard"),
    Expert(7, "Expert"),
    Master(8, "Master"),
    Extreme(9, "Extreme"),
    ;

    companion object {
        fun fromLevel(level: Int): Difficulty =
            entries.firstOrNull { it.level == level } ?: Extreme
    }
}

/**
 * An immutable Sudoku puzzle definition: its givens and its unique solution.
 *
 * `givens` holds the puzzle clues as a flat 81-entry list (row-major), with
 * `0` meaning "no clue" (empty). `solution` is the unique completed grid,
 * also as a flat 81-entry list, with values 1..9.
 */
@Serializable
data class Puzzle(
    val id: String,
    val difficulty: Difficulty,
    val givens: List<Int>,
    val solution: List<Int>,
) {
    init {
        require(givens.size == 81) { "givens must have 81 entries" }
        require(solution.size == 81) { "solution must have 81 entries" }
        require(givens.all { it in 0..9 }) { "givens must be 0..9 (0 = empty)" }
        require(solution.all { it in 1..9 }) { "solution must be 1..9" }
        // Every clue must match the solution.
        givens.forEachIndexed { i, g ->
            require(g == 0 || g == solution[i]) {
                "given at $i ($g) does not match solution (${solution[i]})"
            }
        }
    }

    fun givenAt(row: Int, col: Int): Int = givens[row * 9 + col]
    fun solutionAt(row: Int, col: Int): Int = solution[row * 9 + col]
}
