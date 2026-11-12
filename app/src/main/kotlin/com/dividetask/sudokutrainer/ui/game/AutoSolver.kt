package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.domain.AutoSolve
import com.dividetask.sudokutrainer.domain.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Finishes the game once at most `solve_criteria` cells are empty and
 * every filled cell is correct: fills the rest one cell at a time,
 * celebrates, then calls [onFinished]. Runs once per game.
 */
class AutoSolver(
    private val scope: CoroutineScope,
    private val solveCriteria: () -> Int,
    private val cellDelayMs: () -> Long,
    private val celebrationMs: () -> Long,
    private val board: BoardStore<GameState>,
    private val onFinished: () -> Unit,
) {
    @Volatile var started: Boolean = false
        private set

    /** Start the solve as soon as the board qualifies. */
    fun watch() {
        scope.launch {
            board.flow.collect { s ->
                if (!started && s.phase == GameState.Phase.Playing && s.isMostlySolved(solveCriteria())) start()
            }
        }
    }

    private fun start() {
        if (started) return
        started = true
        board.value = AutoSolve.begin(board.value)
        scope.launch {
            while (board.value.emptyCellCount > 0) {
                delay(cellDelayMs())
                board.value = AutoSolve.solveNextCell(board.value)
            }
            board.value = board.value.copy(phase = GameState.Phase.Celebrating)
            delay(celebrationMs())
            onFinished()
        }
    }
}
