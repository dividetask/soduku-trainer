package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintEngine
import com.dividetask.sudokutrainer.domain.Move
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * With "autofill starting candidates" on, opens a game as if Sweep were
 * pressed — animated or not per "show work". The sweep becomes part of
 * the starting position, so Undo doesn't take the notes away.
 */
class StartingSweep(
    private val scope: CoroutineScope,
    private val board: BoardStore<GameState>,
    private val animator: HintAnimator,
) {
    fun run(autofill: Boolean, showWork: Boolean) {
        if (!autofill) return
        val s = board.value
        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        val marks = Array(81) { i -> s.cells[i].pencilMarks.keys.toSet() }
        val sweep = HintEngine.findCandidateSweep(grid, marks) ?: return
        val job = animator.run(sweep, showWork, countAsHint = false)
        if (job == null) {
            forget()
        } else {
            scope.launch { job.join(); forget() }
        }
    }

    private fun forget() {
        val s = board.value
        if (s.history.size == 1 && s.history[0] is Move.PencilSweep) {
            board.value = s.copy(history = emptyList(), lastHint = null)
        }
    }
}
