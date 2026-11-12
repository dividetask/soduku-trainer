package com.dividetask.sudokutrainer.domain

/** Board states for the hidden-single hint animation. */
object HiddenSingleSteps {
    /**
     * Enter the hidden-single hint animation. Locks the phase, paints the
     * solution digit as a pencil mark in every empty cell of the unit (so
     * the sweep has something visible to erase), and stashes the UI state.
     * Player-placed marks for other digits in those cells are left alone.
     */
    fun begin(state: GameState, hint: Hint.HiddenSingle): GameState {
        val target = state.cells[hint.targetCell]
        val targetOriginalNotes = target.pencilMarks
        val clearedPeerMarks = mutableListOf<AutoClearedMark>()
        for (cellIdx in hint.unit.cells) {
            if (cellIdx == hint.targetCell) continue
            val c = state.cells[cellIdx]
            if (c.value != null) continue
            val existingColor = c.pencilMarks[hint.solutionDigit]
            if (existingColor != null) {
                clearedPeerMarks += AutoClearedMark(
                    row = c.row, col = c.col, digit = hint.solutionDigit, color = existingColor,
                )
            }
        }
        var s = state
        for (cellIdx in hint.unit.cells) {
            val c = s.cells[cellIdx]
            if (c.value != null) continue
            if (hint.solutionDigit in c.pencilMarks) continue
            s = s.replaceCell(c.copy(
                pencilMarks = c.pencilMarks + (hint.solutionDigit to GuessColor.DEFAULT),
            ))
        }
        return s.copy(
            phase = GameState.Phase.Hinting,
            activeDigit = null,
            hintUi = HintUi.HiddenSingle(
                targetCell = hint.targetCell,
                solutionDigit = hint.solutionDigit,
                unitCells = hint.unit.cells.toSet(),
                targetOriginalNotes = targetOriginalNotes,
                clearedPeerMarks = clearedPeerMarks,
            ),
        )
    }

    /**
     * Advance the unit sweep. When [currentCell] is non-null the solution
     * digit is erased from that cell's pencil marks; pass null for the
     * final settle frame where only the target retains the digit.
     */
    fun advance(
        state: GameState,
        currentCell: Int?,
        eliminatorCells: Set<Int>,
    ): GameState {
        val ui = state.hintUi as? HintUi.HiddenSingle ?: return state
        var s = state
        if (currentCell != null) {
            val c = state.cells[currentCell]
            s = s.replaceCell(c.copy(pencilMarks = c.pencilMarks - ui.solutionDigit))
        }
        return s.copy(hintUi = ui.copy(
            currentCell = currentCell,
            eliminatorCells = eliminatorCells,
        ))
    }

    /**
     * Finish the hint: place the solution digit at the target in Blue,
     * tidy lingering solution-digit notes from the rest of the unit (they
     * are provably impossible there), clear hint UI, bump hintCount.
     * Records a [Move.SetValue] whose autoClearedMarks restore any
     * player-placed solution-digit notes that the sweep erased.
     */
    fun complete(
        state: GameState,
        hint: Hint.HiddenSingle,
        countAsHint: Boolean = true,
    ): GameState {
        val ui = state.hintUi as? HintUi.HiddenSingle
        val target = state.cells[hint.targetCell]
        var s = state.replaceCell(target.copy(
            value = hint.solutionDigit,
            valueColor = CellColor.Guess(GuessColor.Blue),
            pencilMarks = emptyMap(),
        ))
        for (cellIdx in hint.unit.cells) {
            if (cellIdx == hint.targetCell) continue
            val c = s.cells[cellIdx]
            if (c.value == null && hint.solutionDigit in c.pencilMarks) {
                s = s.replaceCell(c.copy(pencilMarks = c.pencilMarks - hint.solutionDigit))
            }
        }
        val move = Move.SetValue(
            row = target.row,
            col = target.col,
            newValue = hint.solutionDigit,
            newValueColor = GuessColor.Blue,
            clearedMarks = ui?.targetOriginalNotes.orEmpty(),
            autoClearedMarks = ui?.clearedPeerMarks.orEmpty(),
        )
        return s.copy(
            phase = GameState.Phase.Playing,
            hintUi = null,
            hintCount = if (countAsHint) state.hintCount + 1 else state.hintCount,
            history = state.history + move,
            lastHint = hint,
        )
    }
}
