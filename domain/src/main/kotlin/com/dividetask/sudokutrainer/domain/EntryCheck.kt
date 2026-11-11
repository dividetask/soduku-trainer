package com.dividetask.sudokutrainer.domain

/**
 * The "prevent incorrect values / notes" settings. Only entries made in
 * the Solve color (blue) are checked; the guess colors are for trying
 * things out, so they may be wrong.
 */
object EntryCheck {

    /**
     * True when the move that turned [before] into [after] is one the
     * enabled settings forbid:
     *
     * - [preventValues]: a blue value that isn't the cell's answer.
     * - [preventNotes]: removing a blue note for the cell's answer, or
     *   adding a blue note for a digit already placed correctly in the
     *   cell's row, column or box.
     */
    fun isForbidden(
        before: GameState,
        after: GameState,
        preventValues: Boolean,
        preventNotes: Boolean,
    ): Boolean {
        if (after.history.size != before.history.size + 1) return false
        val puzzle = after.puzzle
        return when (val move = after.history.last()) {
            is Move.SetValue ->
                preventValues && move.newValueColor.isSolveColor &&
                    move.newValue != puzzle.solutionAt(move.row, move.col)
            is Move.TogglePencilMark -> {
                if (!preventNotes || !move.markColor.isSolveColor) return false
                val answer = puzzle.solutionAt(move.row, move.col)
                if (!move.added) {
                    move.digit == answer
                } else {
                    move.digit != answer && peersHoldCorrect(after, move.row, move.col, move.digit)
                }
            }
            else -> false
        }
    }

    private fun peersHoldCorrect(state: GameState, row: Int, col: Int, digit: Int): Boolean =
        state.cells.any { c ->
            c.value == digit && c.value == state.puzzle.solutionAt(c.row, c.col) &&
                (c.row != row || c.col != col) &&
                (c.row == row || c.col == col || (c.row / 3 == row / 3 && c.col / 3 == col / 3))
        }
}
