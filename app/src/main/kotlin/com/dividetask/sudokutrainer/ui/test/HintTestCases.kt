package com.dividetask.sudokutrainer.ui.test

import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.Puzzle

/**
 * Pre-built puzzles whose next hint is guaranteed to be a specific
 * technique. Used by the "Test" menu to preview each teaching animation
 * in turn. Ordering is newest-technique-first.
 *
 * The level-3+ cases set [Case.prefillCandidates] = true; the test
 * viewmodel then seeds every empty cell with its valid candidates
 * before running the hint, which is how those techniques work on a
 * real game (after a Candidate Sweep). Solutions are hardcoded — no
 * on-device solver call — because backtracking on sparse grids is
 * exponential.
 */
object HintTestCases {

    data class Case(
        val title: String,
        val puzzle: Puzzle,
        val prefillCandidates: Boolean = false,
    )

    /**
     * A complete, valid 9x9 grid; every case uses this as its solution.
     *
     * Declared *before* [cases] because Kotlin object properties
     * initialize top-to-bottom: [cases]'s initializer calls [build],
     * which reads this constant, so it must already hold a value.
     */
    private val CANONICAL_SOLUTION: IntArray = intArrayOf(
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

    val cases: List<Case> = listOf(
        build(
            title = "XY-Wing",
            id = "test-xy-wing",
            givenIndices = intArrayOf(
                2, 5, 6, 10, 11, 15, 16, 24, 28, 35, 37, 39, 42, 44, 52, 55,
                57, 60, 62, 67, 70, 71, 73, 74, 76, 78, 79,
            ),
            prefill = true,
        ),
        // Swordfish: no grid yet — the random search didn't find one
        // against this canonical solution within the time budget.
        build(
            title = "X-Wing",
            id = "test-xwing",
            givenIndices = intArrayOf(
                0, 1, 10, 12, 14, 16, 20, 21, 26, 28, 32,
                46, 49, 51, 64, 65, 68, 70, 75, 77, 79,
            ),
            prefill = true,
        ),
        build(
            title = "Hidden Triple",
            id = "test-hidden-triple",
            givenIndices = intArrayOf(
                6, 7, 13, 23, 37, 40, 42, 43, 51, 65, 69, 70, 71, 72, 74, 79,
            ),
            prefill = true,
        ),
        build(
            title = "Naked Triple",
            id = "test-naked-triple",
            givenIndices = intArrayOf(
                0, 2, 10, 13, 16, 17, 19, 22, 28, 32, 33, 41, 44, 47, 48, 49,
                54, 62, 63, 74, 78,
            ),
            prefill = true,
        ),
        build(
            title = "Box-Line Reduction",
            id = "test-box-line",
            givenIndices = intArrayOf(
                18, 19, 20, 22, 24, 27, 32, 33, 34, 35, 37, 42, 52, 53, 57,
                62, 65, 69, 70, 72, 77,
            ),
            prefill = true,
        ),
        build(
            title = "Pointing Pair",
            id = "test-pointing-pair",
            givenIndices = intArrayOf(
                3, 4, 6, 10, 14, 24, 29, 33, 36, 37, 38, 43, 44, 45, 47, 52,
                53, 56, 65, 66, 67, 68, 69, 71, 78, 80,
            ),
            prefill = true,
        ),
        build(
            title = "Hidden Pair",
            id = "test-hidden-pair",
            givenIndices = intArrayOf(
                8, 12, 16, 19, 20, 21, 23, 24, 25, 30, 33, 40, 45, 48, 50,
                53, 55, 56, 61, 63, 67, 69, 71, 74, 77, 78,
            ),
            prefill = true,
        ),
        build(
            title = "Naked Pair",
            id = "test-naked-pair",
            givenIndices = intArrayOf(
                7, 8, 12, 16, 18, 22, 24, 28, 29, 31, 39, 42, 46, 47, 52, 53,
                54, 56, 58, 60, 62, 63, 65, 66, 67, 70, 71, 74, 75, 78, 79,
            ),
            prefill = true,
        ),
        build(
            title = "Candidate Sweep",
            id = "test-candidate-sweep",
            givenIndices = intArrayOf(0, 1 * 9 + 1, 2 * 9 + 2),
        ),
        build(
            title = "Hidden Single",
            id = "test-hidden-single",
            givenIndices = intArrayOf(
                3 * 9 + 1, 1 * 9 + 5, 2 * 9 + 6, 8 * 9 + 2,
            ),
        ),
        build(
            title = "Naked Single",
            id = "test-naked-single",
            givenIndices = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
    )

    private fun build(
        title: String,
        id: String,
        givenIndices: IntArray,
        prefill: Boolean = false,
    ): Case {
        val givens = IntArray(81)
        for (idx in givenIndices) givens[idx] = CANONICAL_SOLUTION[idx]
        return Case(
            title = title,
            puzzle = Puzzle(
                id = id,
                difficulty = Difficulty.Beginner,
                givens = givens.toList(),
                solution = CANONICAL_SOLUTION.toList(),
            ),
            prefillCandidates = prefill,
        )
    }
}
