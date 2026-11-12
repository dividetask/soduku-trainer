package com.dividetask.sudokutrainer.domain

/** Board states for the Candidate Sweep animation. */
object CandidateSweepSteps {
    /**
     * Enter the candidate-sweep hint animation. Snapshots every existing
     * pencil mark so completion can build the consolidated undo move.
     */
    fun begin(state: GameState, hint: Hint.CandidateSweep): GameState {
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
            hintUi = HintUi.CandidateSweep(
                initialFill = hint.initialFill,
                priorMarks = priorMarks,
            ),
        )
    }

    /**
     * Seed every empty cell with the full 1..9 candidate set, in the
     * default color. No-op when the hint UI isn't a sweep or when the
     * sweep wasn't an initial-fill sweep. Cells that already carry any
     * pencil marks are left alone.
     */
    fun fill(state: GameState): GameState {
        val ui = state.hintUi as? HintUi.CandidateSweep ?: return state
        if (!ui.initialFill) return state
        var s = state
        for (i in 0 until 81) {
            val c = s.cells[i]
            if (c.value != null) continue
            if (c.pencilMarks.isNotEmpty()) continue
            s = s.replaceCell(c.copy(pencilMarks = ALL_DIGIT_NOTES))
        }
        return s
    }

    /**
     * Move the sweep cursor. When [currentCell] is non-null, the cell's
     * pencil marks lose every digit in [doomedDigits]. Pass null/empty
     * for the final settle frame.
     */
    fun advance(
        state: GameState,
        currentCell: Int?,
        doomedDigits: Set<Int>,
        eliminatorPeers: Set<Int>,
    ): GameState {
        val ui = state.hintUi as? HintUi.CandidateSweep ?: return state
        var s = state
        if (currentCell != null && doomedDigits.isNotEmpty()) {
            val c = s.cells[currentCell]
            s = s.replaceCell(c.copy(
                pencilMarks = c.pencilMarks.filterKeys { it !in doomedDigits },
            ))
        }
        return s.copy(hintUi = ui.copy(
            currentCell = currentCell,
            eliminatorCells = eliminatorPeers,
        ))
    }

    /**
     * Finish the sweep: build the undo move by diffing current pencil
     * marks against the snapshot, clear hint UI, optionally bump
     * hintCount.
     *
     * Set [countAsHint] to false when the sweep was triggered directly
     * by the player (e.g., the Sweep button) rather than as a hint.
     */
    fun complete(
        state: GameState,
        hint: Hint.CandidateSweep,
        countAsHint: Boolean = true,
    ): GameState {
        val ui = state.hintUi as? HintUi.CandidateSweep
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

    private val ALL_DIGIT_NOTES: Map<Int, GuessColor> =
        (1..9).associateWith { GuessColor.DEFAULT }
}
