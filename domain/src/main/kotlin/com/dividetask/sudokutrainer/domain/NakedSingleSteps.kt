package com.dividetask.sudokutrainer.domain

/** Board states for the naked-single hint animation. */
object NakedSingleSteps {
    /**
     * Enter the naked-single hint animation: lock the phase, paint all 1..9
     * as pencil marks in the target cell, and stash the UI state. Does not
     * touch history; the cell value is committed in [completeNakedSingleHint].
     */
    fun begin(state: GameState, hint: Hint.NakedSingle): GameState {
        val target = state.cells[hint.targetCell]
        val originalNotes = target.pencilMarks
        val notes = (1..9).associateWith { GuessColor.DEFAULT }
        return state
            .replaceCell(target.copy(pencilMarks = notes))
            .copy(
                phase = GameState.Phase.Hinting,
                activeDigit = null,
                hintUi = HintUi.NakedSingle(
                    targetCell = hint.targetCell,
                    solutionDigit = hint.solutionDigit,
                    originalNotes = originalNotes,
                ),
            )
    }

    /**
     * Advance the sweep to the next digit. If [eliminate] is true, the
     * digit is also removed from the target cell's pencil marks.
     */
    fun advance(
        state: GameState,
        activeDigit: Int,
        eliminatorCells: Set<Int>,
        eliminate: Boolean,
    ): GameState {
        val ui = state.hintUi as? HintUi.NakedSingle ?: return state
        var s = state
        if (eliminate) {
            val target = state.cells[ui.targetCell]
            s = s.replaceCell(target.copy(pencilMarks = target.pencilMarks - activeDigit))
        }
        return s.copy(
            hintUi = ui.copy(
                activeDigit = activeDigit,
                eliminatorCells = eliminatorCells,
            ),
        )
    }

    /**
     * Finish the hint: place the solution digit in Blue (Solve color),
     * clear notes, clear hint UI, bump the hint count, return to Playing.
     * Records a [Move.SetValue] so undo reverses both the placement and
     * any pencil marks the animation overwrote in the target cell.
     */
    fun complete(
        state: GameState,
        hint: Hint.NakedSingle,
        countAsHint: Boolean = true,
    ): GameState {
        val ui = state.hintUi as? HintUi.NakedSingle
        val target = state.cells[hint.targetCell]
        val placed = target.copy(
            value = hint.solutionDigit,
            valueColor = CellColor.Guess(GuessColor.Blue),
            pencilMarks = emptyMap(),
        )
        val move = Move.SetValue(
            row = target.row,
            col = target.col,
            newValue = hint.solutionDigit,
            newValueColor = GuessColor.Blue,
            clearedMarks = ui?.originalNotes.orEmpty(),
        )
        return state
            .replaceCell(placed)
            .copy(
                phase = GameState.Phase.Playing,
                hintUi = null,
                hintCount = if (countAsHint) state.hintCount + 1 else state.hintCount,
                history = state.history + move,
                lastHint = hint,
            )
    }
}
