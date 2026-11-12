package com.dividetask.sudokutrainer.domain

/** Board states for the note-single hint animation. */
object NoteSingleSteps {
    fun begin(state: GameState, hint: Hint.NoteSingle): GameState {
        val target = state.cells[hint.targetCell]
        return state.copy(
            phase = GameState.Phase.Hinting,
            activeDigit = null,
            hintUi = HintUi.NoteSingle(
                targetCell = hint.targetCell,
                solutionDigit = hint.solutionDigit,
                originalNotes = target.pencilMarks,
            ),
        )
    }

    fun complete(
        state: GameState,
        hint: Hint.NoteSingle,
        countAsHint: Boolean = true,
    ): GameState {
        val ui = state.hintUi as? HintUi.NoteSingle
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
