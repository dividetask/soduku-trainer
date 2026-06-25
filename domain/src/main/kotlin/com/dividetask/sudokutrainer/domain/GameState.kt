package com.dividetask.sudokutrainer.domain

data class GameState(
    val puzzle: Puzzle,
    val cells: List<Cell>,
    val activeDigit: Int? = null,
    val pencilOn: Boolean = false,
    val activeColor: GuessColor = GuessColor.DEFAULT,
    val history: List<Move> = emptyList(),
    val phase: Phase = Phase.Playing,
    val hintCount: Int = 0,
    val hintUi: HintUi? = null,
    val pendingHint: PendingHint? = null,
    val lastHint: HintEngine.Hint? = null,
) {
    enum class Phase { Playing, AutoSolving, Celebrating, Hinting }

    init {
        require(cells.size == 81)
        require(activeDigit == null || activeDigit in 1..9)
    }

    fun cellAt(row: Int, col: Int): Cell = cells[row * 9 + col]

    val correctCount: Int get() =
        cells.count { it.value != null && it.value == puzzle.solutionAt(it.row, it.col) }

    val hasIncorrectEntry: Boolean get() =
        cells.any { it.value != null && it.value != puzzle.solutionAt(it.row, it.col) }

    /** Number of non-given cells that are still empty. */
    val emptyCellCount: Int get() =
        cells.count { !it.given && it.value == null }

    /**
     * Check if the puzzle is ready for auto-solve: at most [solveCriteria]
     * empty cells remain AND every filled cell is correct. Cell color
     * doesn't matter — any placed value (Guess, Solve, etc.) counts.
     */
    fun isMostlySolved(solveCriteria: Int): Boolean =
        !hasIncorrectEntry && emptyCellCount <= solveCriteria

    fun placedCountOf(digit: Int): Int {
        require(digit in 1..9)
        return cells.count { it.value == digit }
    }

    fun isDigitExhausted(digit: Int): Boolean = placedCountOf(digit) >= 9

    companion object {
        fun forNewGame(puzzle: Puzzle): GameState {
            val cells = List(81) { i ->
                val row = i / 9
                val col = i % 9
                val g = puzzle.givens[i]
                if (g == 0) {
                    Cell(row = row, col = col, given = false)
                } else {
                    Cell(
                        row = row,
                        col = col,
                        given = true,
                        value = g,
                        valueColor = CellColor.Given,
                    )
                }
            }
            return GameState(puzzle = puzzle, cells = cells)
        }
    }
}
