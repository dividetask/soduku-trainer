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

    /**
     * Fill every empty cell's pencil marks with its valid candidates (the
     * digits not blocked by a peer in the row/column/box). Used by the
     * test menu so puzzles meant to demo pair/triple/wing techniques
     * start with notes already in place.
     */
    fun populateValidCandidates(state: GameState): GameState {
        var s = state
        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        for (i in 0 until 81) {
            val c = s.cells[i]
            if (c.value != null) continue
            val cands = (1..9).filter {
                SudokuSolver.isValidPlacement(grid, c.row, c.col, it)
            }
            s = s.replaceCell(c.copy(
                pencilMarks = cands.associateWith { GuessColor.DEFAULT },
            ))
        }
        return s
    }

    // ---- Progressive-hint state ----

    /** Stage 1: technique name revealed, cells not highlighted yet. */
    fun setPendingHint(state: GameState, hint: HintEngine.Hint, keyCells: Set<Int>): GameState =
        state.copy(pendingHint = PendingHint(
            hint = hint,
            keyCells = keyCells,
            techniqueName = hint.techniqueName,
            showCells = false,
        ))

    /** Stage 2: also highlight the pattern's cells. No-op if already shown. */
    fun advancePendingHintRevealCells(state: GameState): GameState {
        val ph = state.pendingHint ?: return state
        if (ph.showCells) return state
        return state.copy(pendingHint = ph.copy(showCells = true))
    }

    fun clearPendingHint(state: GameState): GameState =
        if (state.pendingHint == null) state else state.copy(pendingHint = null)

    // ---- Hint animation actions ----

    /**
     * Enter the naked-single hint animation: lock the phase, paint all 1..9
     * as pencil marks in the target cell, and stash the UI state. Does not
     * touch history; the cell value is committed in [completeNakedSingleHint].
     */
    fun beginNakedSingleHint(state: GameState, hint: HintEngine.NakedSingle): GameState {
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
    fun advanceNakedSingleStep(
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
    fun completeNakedSingleHint(
        state: GameState,
        hint: HintEngine.NakedSingle,
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

    // ---- Hidden-single hint animation ----

    /**
     * Enter the hidden-single hint animation. Locks the phase, paints the
     * solution digit as a pencil mark in every empty cell of the unit (so
     * the sweep has something visible to erase), and stashes the UI state.
     * Player-placed marks for other digits in those cells are left alone.
     */
    fun beginHiddenSingleHint(state: GameState, hint: HintEngine.HiddenSingle): GameState {
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
    fun advanceHiddenSingleStep(
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
    fun completeHiddenSingleHint(
        state: GameState,
        hint: HintEngine.HiddenSingle,
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

    // ---- Note-single hint animation ----

    fun beginNoteSingleHint(state: GameState, hint: HintEngine.NoteSingle): GameState {
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

    fun completeNoteSingleHint(
        state: GameState,
        hint: HintEngine.NoteSingle,
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

    // ---- Candidate-sweep hint animation ----

    /**
     * Enter the candidate-sweep hint animation. Snapshots every existing
     * pencil mark so completion can build the consolidated undo move.
     */
    fun beginCandidateSweep(state: GameState, hint: HintEngine.CandidateSweep): GameState {
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
    fun applyCandidateSweepFill(state: GameState): GameState {
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
    fun advanceCandidateSweepCell(
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
    fun completeCandidateSweep(
        state: GameState,
        hint: HintEngine.CandidateSweep,
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

    // ---- Generic elimination-hint animation (Naked Pair onwards) ----

    fun beginEliminationHint(state: GameState, hint: HintEngine.EliminationHint): GameState {
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

    fun advanceEliminationStep(
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

    fun completeEliminationHint(
        state: GameState,
        hint: HintEngine.EliminationHint,
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

    private fun markKey(m: AutoClearedMark): Int = (m.row * 9 + m.col) * 10 + m.digit

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
