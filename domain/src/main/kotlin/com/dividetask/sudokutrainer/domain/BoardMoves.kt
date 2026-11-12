package com.dividetask.sudokutrainer.domain

/** Player moves on the board: selecting, placing, notes, clearing, undo. */
object BoardMoves {
    fun selectDigit(state: GameState, digit: Int): GameState {
        require(digit in 1..9)
        val new = if (state.activeDigit == digit) null else digit
        return state.copy(activeDigit = new)
    }

    fun togglePencil(state: GameState): GameState =
        state.copy(pencilOn = !state.pencilOn)

    fun selectColor(state: GameState, color: GuessColor): GameState =
        state.copy(activeColor = color)

    fun tapCell(state: GameState, row: Int, col: Int): GameState {
        if (state.phase != GameState.Phase.Playing) return state
        val digit = state.activeDigit ?: return state
        val cell = state.cellAt(row, col)
        if (cell.given) return state

        return if (state.pencilOn) {
            tapCellPencilOn(state, cell, digit)
        } else {
            tapCellPencilOff(state, cell, digit)
        }
    }

    private fun tapCellPencilOff(state: GameState, cell: Cell, digit: Int): GameState {
        if (!cell.isEmpty) return state

        // Auto-clear: when placing in Blue (Solve) mode, remove Blue pencil
        // marks for this digit from peer cells (same row, column, and 3x3 box).
        val autoClearedMarks = mutableListOf<AutoClearedMark>()
        var s = state
        if (state.activeColor.isSolveColor) {
            for (peer in peerIndices(cell.row, cell.col)) {
                val peerCell = s.cells[peer]
                val mark = peerCell.pencilMarks[digit]
                if (mark != null && mark.isSolveColor) {
                    autoClearedMarks += AutoClearedMark(
                        row = peerCell.row,
                        col = peerCell.col,
                        digit = digit,
                        color = mark,
                    )
                    s = s.replaceCell(peerCell.copy(
                        pencilMarks = peerCell.pencilMarks - digit,
                    ))
                }
            }
        }

        val move = Move.SetValue(
            row = cell.row,
            col = cell.col,
            newValue = digit,
            newValueColor = state.activeColor,
            clearedMarks = cell.pencilMarks,
            autoClearedMarks = autoClearedMarks,
        )
        val newCell = cell.copy(
            value = digit,
            valueColor = CellColor.Guess(state.activeColor),
            pencilMarks = emptyMap(),
        )
        // Any board-changing player action invalidates the pending hint
        // and the Show Again button — clears them both unconditionally.
        return s
            .copy(lastHint = null, pendingHint = null)
            .replaceCell(newCell)
            .appendHistory(move)
    }

    private fun tapCellPencilOn(state: GameState, cell: Cell, digit: Int): GameState {
        if (!cell.isEmpty) return state

        val existing = cell.pencilMarks[digit]
        val (newMarks, move) = if (existing == null) {
            val m = cell.pencilMarks + (digit to state.activeColor)
            val move = Move.TogglePencilMark(
                row = cell.row,
                col = cell.col,
                digit = digit,
                added = true,
                markColor = state.activeColor,
            )
            m to move
        } else {
            val m = cell.pencilMarks - digit
            val move = Move.TogglePencilMark(
                row = cell.row,
                col = cell.col,
                digit = digit,
                added = false,
                markColor = existing,
            )
            m to move
        }
        return state
            .copy(lastHint = null, pendingHint = null)
            .replaceCell(cell.copy(pencilMarks = newMarks))
            .appendHistory(move)
    }

    fun doubleTapCell(state: GameState, row: Int, col: Int): GameState {
        if (state.phase != GameState.Phase.Playing) return state
        val cell = state.cellAt(row, col)
        if (cell.given || cell.isEmpty) return state

        val prevColor = cell.valueColor as? CellColor.Guess
            ?: return state

        val move = Move.ClearValue(
            row = cell.row,
            col = cell.col,
            prevValue = cell.value!!,
            prevValueColor = prevColor.color,
        )
        val newCell = cell.copy(value = null, valueColor = null)
        return state
            .copy(lastHint = null, pendingHint = null)
            .replaceCell(newCell)
            .appendHistory(move)
    }

    fun undo(state: GameState): GameState {
        if (state.phase != GameState.Phase.Playing) return state
        val last = state.history.lastOrNull() ?: return state
        val trimmedHistory = state.history.dropLast(1)

        var s = state
        when (last) {
            is Move.SetValue -> {
                for (acm in last.autoClearedMarks) {
                    val peer = s.cellAt(acm.row, acm.col)
                    s = s.replaceCell(peer.copy(
                        pencilMarks = peer.pencilMarks + (acm.digit to acm.color),
                    ))
                }
                val cell = s.cellAt(last.row, last.col)
                s = s.replaceCell(cell.copy(
                    value = null,
                    valueColor = null,
                    pencilMarks = last.clearedMarks,
                ))
            }
            is Move.ClearValue -> {
                val cell = s.cellAt(last.row, last.col)
                s = s.replaceCell(cell.copy(
                    value = last.prevValue,
                    valueColor = CellColor.Guess(last.prevValueColor),
                ))
            }
            is Move.TogglePencilMark -> {
                val cell = s.cellAt(last.row, last.col)
                val marks = if (last.added) {
                    cell.pencilMarks - last.digit
                } else {
                    cell.pencilMarks + (last.digit to last.markColor)
                }
                s = s.replaceCell(cell.copy(pencilMarks = marks))
            }
            is Move.PencilSweep -> {
                for (m in last.addedMarks) {
                    val c = s.cellAt(m.row, m.col)
                    s = s.replaceCell(c.copy(pencilMarks = c.pencilMarks - m.digit))
                }
                for (m in last.removedMarks) {
                    val c = s.cellAt(m.row, m.col)
                    s = s.replaceCell(c.copy(
                        pencilMarks = c.pencilMarks + (m.digit to m.color),
                    ))
                }
            }
        }
        return s.copy(history = trimmedHistory)
    }

    /** Reset the board to its starting position (givens only). */
    fun resetBoard(state: GameState): GameState =
        GameState.forNewGame(state.puzzle)
}
