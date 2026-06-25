package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.domain.GameReducer
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drives the per-hint board animation off a [CoroutineScope] and lets
 * callers skip an in-flight animation straight to its final placement.
 *
 * The animator is intentionally state-shape agnostic: it reads/writes the
 * caller's [GameState] via lambdas, so it can be shared by the main
 * game screen and the technique-test screen.
 */
class HintAnimator(
    private val scope: CoroutineScope,
    private val stepDelayMs: () -> Long,
    private val sweepCellMs: () -> Long,
    private val getState: () -> GameState,
    private val setState: (GameState) -> Unit,
) {

    @Volatile private var pending: HintEngine.Hint? = null
    @Volatile private var pendingCountAsHint: Boolean = true
    @Volatile private var pendingSpeedMultiplier: Float = 1f
    private var job: Job? = null

    val isRunning: Boolean get() = pending != null

    /**
     * Launch the animation for [hint] and return the [Job] so callers can
     * `join()` if they need to await completion. Only one animation runs
     * at a time; calling this while one is in flight is a no-op.
     *
     * Pass [countAsHint] = false when the player triggered the action
     * directly (e.g., the Sweep button or "Show Again") so it doesn't
     * bump hintCount. [speedMultiplier] scales every per-step delay; use
     * a value > 1 to play the animation slower than the configured pace
     * (Show Again).
     */
    fun launch(
        hint: HintEngine.Hint,
        countAsHint: Boolean = true,
        speedMultiplier: Float = 1f,
    ): Job? {
        if (pending != null) return job
        pending = hint
        pendingCountAsHint = countAsHint
        pendingSpeedMultiplier = speedMultiplier
        val j = scope.launch {
            try {
                when (hint) {
                    is HintEngine.NakedSingle -> runNakedSingle(hint, countAsHint)
                    is HintEngine.HiddenSingle -> runHiddenSingle(hint, countAsHint)
                    is HintEngine.NoteSingle -> runNoteSingle(hint, countAsHint)
                    is HintEngine.CandidateSweep -> runCandidateSweep(hint, countAsHint)
                    is HintEngine.EliminationHint -> runEliminationHint(hint, countAsHint)
                }
            } finally {
                if (pending === hint) pending = null
            }
        }
        job = j
        return j
    }

    /**
     * Cancel any in-flight animation and apply its completion step
     * immediately. No-op if no animation is running.
     */
    fun skip() {
        val h = pending ?: return
        val count = pendingCountAsHint
        pending = null
        job?.cancel()
        setState(complete(getState(), h, count))
    }

    private fun stepMs(): Long =
        (stepDelayMs() * pendingSpeedMultiplier).toLong().coerceAtLeast(1)

    private fun sweepMs(): Long =
        (sweepCellMs() * pendingSpeedMultiplier).toLong().coerceAtLeast(1)

    /** Apply every remaining step of [hint] and finish it, idempotently. */
    private fun complete(
        state: GameState,
        hint: HintEngine.Hint,
        countAsHint: Boolean,
    ): GameState = when (hint) {
        is HintEngine.NakedSingle -> GameReducer.completeNakedSingleHint(state, hint, countAsHint = countAsHint)
        is HintEngine.HiddenSingle -> GameReducer.completeHiddenSingleHint(state, hint, countAsHint = countAsHint)
        is HintEngine.NoteSingle -> GameReducer.completeNoteSingleHint(state, hint, countAsHint = countAsHint)
        is HintEngine.CandidateSweep -> {
            var s = state
            // Re-applying these is idempotent — finished cells skip.
            s = GameReducer.applyCandidateSweepFill(s)
            for (plan in hint.plan) {
                s = GameReducer.advanceCandidateSweepCell(
                    state = s,
                    currentCell = plan.cellIdx,
                    doomedDigits = plan.doomedDigits,
                    eliminatorPeers = plan.eliminatorPeers,
                )
            }
            GameReducer.completeCandidateSweep(s, hint, countAsHint = countAsHint)
        }
        is HintEngine.EliminationHint -> {
            var s = state
            for (step in hint.plan) {
                s = GameReducer.advanceEliminationStep(
                    state = s,
                    currentCell = step.cellIdx,
                    doomedDigits = step.doomedDigits,
                    explainerCells = step.explainerCells,
                )
            }
            GameReducer.completeEliminationHint(s, hint)
        }
    }

    private suspend fun runNakedSingle(hint: HintEngine.NakedSingle, countAsHint: Boolean) {
        setState(GameReducer.beginNakedSingleHint(getState(), hint))
        delay(stepMs())
        for (d in 1..9) {
            setState(GameReducer.advanceNakedSingleStep(
                state = getState(),
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            ))
            delay(stepMs())
        }
        setState(GameReducer.completeNakedSingleHint(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runNoteSingle(hint: HintEngine.NoteSingle, countAsHint: Boolean) {
        setState(GameReducer.beginNoteSingleHint(getState(), hint))
        // Two pauses so the player has a beat to recognize the cell and
        // its lone candidate before the value drops in.
        delay(stepMs())
        delay(stepMs())
        setState(GameReducer.completeNoteSingleHint(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runHiddenSingle(hint: HintEngine.HiddenSingle, countAsHint: Boolean) {
        setState(GameReducer.beginHiddenSingleHint(getState(), hint))
        delay(stepMs())
        for (cellIdx in hint.unit.cells) {
            if (cellIdx == hint.targetCell) continue
            if (getState().cells[cellIdx].value != null) continue
            setState(GameReducer.advanceHiddenSingleStep(
                state = getState(),
                currentCell = cellIdx,
                eliminatorCells = hint.eliminators[cellIdx].orEmpty(),
            ))
            delay(stepMs())
        }
        setState(GameReducer.advanceHiddenSingleStep(
            state = getState(),
            currentCell = null,
            eliminatorCells = emptySet(),
        ))
        delay(stepMs())
        setState(GameReducer.completeHiddenSingleHint(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runEliminationHint(hint: HintEngine.EliminationHint, countAsHint: Boolean) {
        setState(GameReducer.beginEliminationHint(getState(), hint))
        delay(stepMs())
        for (step in hint.plan) {
            setState(GameReducer.advanceEliminationStep(
                state = getState(),
                currentCell = step.cellIdx,
                doomedDigits = step.doomedDigits,
                explainerCells = step.explainerCells,
            ))
            delay(sweepMs())
        }
        setState(GameReducer.advanceEliminationStep(
            state = getState(),
            currentCell = null,
            doomedDigits = emptySet(),
            explainerCells = emptySet(),
        ))
        delay(stepMs())
        setState(GameReducer.completeEliminationHint(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runCandidateSweep(hint: HintEngine.CandidateSweep, countAsHint: Boolean) {
        setState(GameReducer.beginCandidateSweep(getState(), hint))
        delay(stepMs())
        if (hint.initialFill) {
            setState(GameReducer.applyCandidateSweepFill(getState()))
            delay(stepMs())
        }
        for (plan in hint.plan) {
            setState(GameReducer.advanceCandidateSweepCell(
                state = getState(),
                currentCell = plan.cellIdx,
                doomedDigits = plan.doomedDigits,
                eliminatorPeers = plan.eliminatorPeers,
            ))
            delay(sweepMs())
        }
        setState(GameReducer.advanceCandidateSweepCell(
            state = getState(),
            currentCell = null,
            doomedDigits = emptySet(),
            eliminatorPeers = emptySet(),
        ))
        delay(stepMs())
        setState(GameReducer.completeCandidateSweep(getState(), hint, countAsHint = countAsHint))
    }
}
