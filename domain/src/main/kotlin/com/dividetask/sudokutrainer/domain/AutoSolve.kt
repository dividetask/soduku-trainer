package com.dividetask.sudokutrainer.domain

/** Finishing the puzzle once it is mostly solved. */
object AutoSolve {
    fun begin(state: GameState): GameState =
        state.copy(
            history = emptyList(),
            phase = GameState.Phase.AutoSolving,
            activeDigit = null,
        )

    fun solveNextCell(state: GameState): GameState {
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

    fun solveAll(state: GameState): GameState {
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
}
