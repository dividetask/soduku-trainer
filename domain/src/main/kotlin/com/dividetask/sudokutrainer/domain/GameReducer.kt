package com.dividetask.sudokutrainer.domain

object GameReducer {

    // ---- Non-undoable UI state changes ----

    fun selectDigit(state: GameState, digit: Int): GameState {
        require(digit in 1..9)
        val new = if (state.activeDigit == digit) null else digit
        return state.copy(activeDigit = new)
    }

    fun clearActiveDigit(state: GameState): GameState =
        state.copy(activeDigit = null)

    fun togglePencil(state: GameState): GameState =
        state.copy(pencilOn = !state.pencilOn)

    fun selectColor(state: GameState, color: GuessColor): GameState =
        state.copy(activeColor = color)

    // ---- Undoable, cell-mutating actions ----

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
        return s.replaceCell(newCell).appendHistory(move)
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
        return state.replaceCell(cell.copy(pencilMarks = newMarks)).appendHistory(move)
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
        return state.replaceCell(newCell).appendHistory(move)
    }

    fun undo(state: GameState): GameState {
        if (state.phase != GameState.Phase.Playing) return state
        val last = state.history.lastOrNull() ?: return state
        val trimmedHistory = state.history.dropLast(1)

        var s = state
        // First restore auto-cleared pencil marks from peer cells.
        if (last is Move.SetValue) {
            for (acm in last.autoClearedMarks) {
                val peer = s.cellAt(acm.row, acm.col)
                s = s.replaceCell(peer.copy(
                    pencilMarks = peer.pencilMarks + (acm.digit to acm.color),
                ))
            }
        }

        val cell = s.cellAt(last.row, last.col)
        val restored: Cell = when (last) {
            is Move.SetValue -> cell.copy(
                value = null,
                valueColor = null,
                pencilMarks = last.clearedMarks,
            )
            is Move.ClearValue -> cell.copy(
                value = last.prevValue,
                valueColor = CellColor.Guess(last.prevValueColor),
            )
            is Move.TogglePencilMark -> {
                val marks = if (last.added) {
                    cell.pencilMarks - last.digit
                } else {
                    cell.pencilMarks + (last.digit to last.markColor)
                }
                cell.copy(pencilMarks = marks)
            }
        }
        return s.replaceCell(restored).copy(history = trimmedHistory)
    }

    /** Reset the board to its starting position (givens only). */
    fun resetBoard(state: GameState): GameState =
        GameState.forNewGame(state.puzzle)

    // ---- End-of-game actions ----

    fun beginAutoSolve(state: GameState): GameState =
        state.copy(
            history = emptyList(),
            phase = GameState.Phase.AutoSolving,
            activeDigit = null,
        )

    fun solveSingleCell(state: GameState): GameState {
        val idx = state.cells.indexOfFirst { !it.given && it.value == null }
        if (idx == -1) return state
        val cell = state.cells[idx]
        val newCell = cell.copy(
            value = state.puzzle.solutionAt(cell.row, cell.col),
            valueColor = CellColor.Solve,
            pencilMarks = emptyMap(),
        )
        return state.replaceCell(newCell)
    }

    fun applySolve(state: GameState): GameState {
        val newCells = state.cells.map { c ->
            if (c.isEmpty) {
                c.copy(
                    value = state.puzzle.solutionAt(c.row, c.col),
                    valueColor = CellColor.Solve,
                    pencilMarks = emptyMap(),
                )
            } else {
                c
            }
        }
        return state.copy(
            cells = newCells,
            history = emptyList(),
            phase = GameState.Phase.Celebrating,
            activeDigit = null,
        )
    }

    // ---- Helpers ----

    private fun GameState.replaceCell(newCell: Cell): GameState {
        val idx = newCell.row * 9 + newCell.col
        val list = cells.toMutableList()
        list[idx] = newCell
        return copy(cells = list)
    }

    private fun GameState.appendHistory(move: Move): GameState =
        copy(history = history + move)

    /**
     * Return the flat indices of all cells that share a row, column, or 3x3
     * box with (row, col), excluding (row, col) itself.
     */
    private fun peerIndices(row: Int, col: Int): List<Int> {
        val result = mutableSetOf<Int>()
        for (c in 0 until 9) if (c != col) result += row * 9 + c
        for (r in 0 until 9) if (r != row) result += r * 9 + col
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (r != row || c != col) result += r * 9 + c
            }
        }
        return result.toList()
    }
}
