package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.domain.BoardMoves
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.Hint
import com.dividetask.sudokutrainer.domain.HintEngine
import com.dividetask.sudokutrainer.domain.Move
import com.dividetask.sudokutrainer.domain.PendingHints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The game's Hint and Show Again buttons. Hint goes in stages: name the
 * technique, then highlight its cells, then animate it (or, with "show
 * work" off, apply it). A board with mistakes gets them highlighted
 * instead of a hint.
 */
class HintFlow(
    private val scope: CoroutineScope,
    private val board: BoardStore<GameState>,
    private val animator: HintAnimator,
    private val overlays: BoardOverlays,
    private val pendingHintMs: () -> Long,
    private val showAgainMultiplier: () -> Float,
    private val showWork: () -> Boolean,
) {
    private var pendingHintJob: Job? = null

    fun onHint() {
        // Pressing Hint while an animation is in flight skips to the end.
        if (animator.isRunning) {
            animator.skip()
            return
        }
        val s = board.value
        if (s.phase != GameState.Phase.Playing) return
        if (s.hintUi != null) return

        // Stage 2 or 3: a pending hint already exists from a prior press.
        val ph = s.pendingHint
        if (ph != null) {
            if (!ph.showCells) {
                board.value = PendingHints.revealCells(s)
                startPendingHintTimer()
            } else {
                pendingHintJob?.cancel()
                board.value = PendingHints.clear(s)
                animator.run(ph.hint, showWork())
            }
            return
        }

        // Stage 1. A board with mistakes (wrong values, or notes missing
        // the answer) would mislead the engine, so show them instead.
        val mistakes = s.mistakeCells()
        if (mistakes.isNotEmpty()) {
            overlays.showMistakes(mistakes)
            return
        }
        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        val marks = Array(81) { i -> s.cells[i].pencilMarks.keys.toSet() }
        when (val hint = HintEngine.find(grid, marks)) {
            null -> overlays.flashNoHint()
            is Hint.NakedSingle -> pend(s, hint, setOf(hint.targetCell))
            is Hint.HiddenSingle -> pend(s, hint, setOf(hint.targetCell))
            is Hint.NoteSingle -> pend(s, hint, setOf(hint.targetCell))
            // No single target cell, so no progressive flow — apply now.
            is Hint.CandidateSweep -> animator.run(hint, showWork())
            is Hint.EliminationHint -> pend(s, hint, hint.keyCells)
        }
    }

    /** Replay the last hint, slowed down. */
    fun onShowAgain() {
        val s = board.value
        if (s.phase != GameState.Phase.Playing) return
        if (animator.isRunning) return
        val hint = s.lastHint ?: return
        // If the last move is still the hint's own, undo it so the
        // animation has something to apply. (If the player already
        // pressed Undo, the hint isn't on the board anymore.)
        val base = if (hintMatchesTopOfHistory(s, hint)) BoardMoves.undo(s) else s
        board.value = base.copy(lastHint = hint)
        animator.launch(hint = hint, countAsHint = false, speedMultiplier = showAgainMultiplier())
    }

    private fun pend(s: GameState, hint: Hint, keyCells: Set<Int>) {
        board.value = PendingHints.set(s, hint, keyCells)
        startPendingHintTimer()
    }

    private fun hintMatchesTopOfHistory(state: GameState, hint: Hint): Boolean {
        val last = state.history.lastOrNull() ?: return false
        return when (hint) {
            is Hint.NakedSingle, is Hint.HiddenSingle -> last is Move.SetValue
            else -> last is Move.PencilSweep
        }
    }

    private fun startPendingHintTimer() {
        pendingHintJob?.cancel()
        pendingHintJob = scope.launch {
            delay(pendingHintMs())
            if (board.value.pendingHint != null) board.value = PendingHints.clear(board.value)
        }
    }
}
