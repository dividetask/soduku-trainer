package com.dividetask.sudokutrainer.domain

/** Board states for the elimination-technique animation (Naked Pair onwards). */
object EliminationSteps {
    fun begin(state: GameState, hint: Hint.EliminationHint): GameState {
        val priorMarks = buildList {
            for (i in 0 until 81) {
                val c = state.cells[i]
                for ((d, color) in c.pencilMarks) {
                    add(AutoClearedMark(c.row, c.col, d, color))
                }
            }
        }
        return state.copy(
            phase = GameState.Phase.Hinting,
            activeDigit = null,
            hintUi = HintUi.Elimination(
                techniqueName = hint.techniqueName,
                keyCells = hint.keyCells,
                unitCells = hint.unitCells,
                priorMarks = priorMarks,
            ),
        )
    }

    fun advance(
        state: GameState,
        currentCell: Int?,
        doomedDigits: Set<Int>,
        explainerCells: Set<Int>,
    ): GameState {
        val ui = state.hintUi as? HintUi.Elimination ?: return state
        var s = state
        if (currentCell != null && doomedDigits.isNotEmpty()) {
            val c = s.cells[currentCell]
            s = s.replaceCell(c.copy(
                pencilMarks = c.pencilMarks.filterKeys { it !in doomedDigits },
            ))
        }
        return s.copy(hintUi = ui.copy(
            currentCell = currentCell,
            explainerCells = explainerCells,
        ))
    }

    fun complete(
        state: GameState,
        hint: Hint.EliminationHint,
        countAsHint: Boolean = true,
    ): GameState {
        val ui = state.hintUi as? HintUi.Elimination
        val prior = ui?.priorMarks.orEmpty()
        val current = buildList {
            for (i in 0 until 81) {
                val c = state.cells[i]
                for ((d, color) in c.pencilMarks) {
                    add(AutoClearedMark(c.row, c.col, d, color))
                }
            }
        }
        val priorKeys = prior.mapTo(HashSet()) { markKey(it) }
        val currentKeys = current.mapTo(HashSet()) { markKey(it) }
        val added = current.filter { markKey(it) !in priorKeys }
        val removed = prior.filter { markKey(it) !in currentKeys }
        val move = Move.PencilSweep(addedMarks = added, removedMarks = removed)
        return state.copy(
            phase = GameState.Phase.Playing,
            hintUi = null,
            hintCount = if (countAsHint) state.hintCount + 1 else state.hintCount,
            history = state.history + move,
            lastHint = hint,
        )
    }
}
