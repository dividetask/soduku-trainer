package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.domain.CandidateSweepSteps
import com.dividetask.sudokutrainer.domain.EliminationSteps
import com.dividetask.sudokutrainer.domain.HiddenSingleSteps
import com.dividetask.sudokutrainer.domain.NakedSingleSteps
import com.dividetask.sudokutrainer.domain.NoteSingleSteps
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.Hint
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

    @Volatile private var pending: Hint? = null
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
        hint: Hint,
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
                    is Hint.NakedSingle -> runNakedSingle(hint, countAsHint)
                    is Hint.HiddenSingle -> runHiddenSingle(hint, countAsHint)
                    is Hint.NoteSingle -> runNoteSingle(hint, countAsHint)
                    is Hint.CandidateSweep -> runCandidateSweep(hint, countAsHint)
                    is Hint.EliminationHint -> runEliminationHint(hint, countAsHint)
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

    /**
     * Apply [hint]'s end result straight away, with no animation — for
     * when the player has turned off "show work when giving hint". No-op
     * while an animation is running.
     */
    fun applyInstantly(hint: Hint, countAsHint: Boolean = true) {
        if (pending != null) return
        val s = getState()
        // The completion steps read the snapshot their begin step takes
        // (for undo), so begin first, then finish in the same frame.
        val begun = when (hint) {
            is Hint.NakedSingle -> NakedSingleSteps.begin(s, hint)
            is Hint.HiddenSingle -> HiddenSingleSteps.begin(s, hint)
            is Hint.NoteSingle -> NoteSingleSteps.begin(s, hint)
            is Hint.CandidateSweep -> CandidateSweepSteps.begin(s, hint)
            is Hint.EliminationHint -> EliminationSteps.begin(s, hint)
        }
        setState(complete(begun, hint, countAsHint))
    }

    /** [launch] when [showWork] is set, otherwise [applyInstantly]. Returns the animation's job, if any. */
    fun run(hint: Hint, showWork: Boolean, countAsHint: Boolean = true): Job? =
        if (showWork) {
            launch(hint, countAsHint)
        } else {
            applyInstantly(hint, countAsHint)
            null
        }

    private fun stepMs(): Long =
        (stepDelayMs() * pendingSpeedMultiplier).toLong().coerceAtLeast(1)

    private fun sweepMs(): Long =
        (sweepCellMs() * pendingSpeedMultiplier).toLong().coerceAtLeast(1)

    /** Apply every remaining step of [hint] and finish it, idempotently. */
    private fun complete(
        state: GameState,
        hint: Hint,
        countAsHint: Boolean,
    ): GameState = when (hint) {
        is Hint.NakedSingle -> NakedSingleSteps.complete(state, hint, countAsHint = countAsHint)
        is Hint.HiddenSingle -> HiddenSingleSteps.complete(state, hint, countAsHint = countAsHint)
        is Hint.NoteSingle -> NoteSingleSteps.complete(state, hint, countAsHint = countAsHint)
        is Hint.CandidateSweep -> {
            var s = state
            // Re-applying these is idempotent — finished cells skip.
            s = CandidateSweepSteps.fill(s)
            for (plan in hint.plan) {
                s = CandidateSweepSteps.advance(
                    state = s,
                    currentCell = plan.cellIdx,
                    doomedDigits = plan.doomedDigits,
                    eliminatorPeers = plan.eliminatorPeers,
                )
            }
            CandidateSweepSteps.complete(s, hint, countAsHint = countAsHint)
        }
        is Hint.EliminationHint -> {
            var s = state
            for (step in hint.plan) {
                s = EliminationSteps.advance(
                    state = s,
                    currentCell = step.cellIdx,
                    doomedDigits = step.doomedDigits,
                    explainerCells = step.explainerCells,
                )
            }
            EliminationSteps.complete(s, hint, countAsHint = countAsHint)
        }
    }

    private suspend fun runNakedSingle(hint: Hint.NakedSingle, countAsHint: Boolean) {
        setState(NakedSingleSteps.begin(getState(), hint))
        delay(stepMs())
        for (d in 1..9) {
            setState(NakedSingleSteps.advance(
                state = getState(),
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            ))
            delay(stepMs())
        }
        setState(NakedSingleSteps.complete(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runNoteSingle(hint: Hint.NoteSingle, countAsHint: Boolean) {
        setState(NoteSingleSteps.begin(getState(), hint))
        // Two pauses so the player has a beat to recognize the cell and
        // its lone candidate before the value drops in.
        delay(stepMs())
        delay(stepMs())
        setState(NoteSingleSteps.complete(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runHiddenSingle(hint: Hint.HiddenSingle, countAsHint: Boolean) {
        setState(HiddenSingleSteps.begin(getState(), hint))
        delay(stepMs())
        for (cellIdx in hint.unit.cells) {
            if (cellIdx == hint.targetCell) continue
            if (getState().cells[cellIdx].value != null) continue
            setState(HiddenSingleSteps.advance(
                state = getState(),
                currentCell = cellIdx,
                eliminatorCells = hint.eliminators[cellIdx].orEmpty(),
            ))
            delay(stepMs())
        }
        setState(HiddenSingleSteps.advance(
            state = getState(),
            currentCell = null,
            eliminatorCells = emptySet(),
        ))
        delay(stepMs())
        setState(HiddenSingleSteps.complete(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runEliminationHint(hint: Hint.EliminationHint, countAsHint: Boolean) {
        setState(EliminationSteps.begin(getState(), hint))
        delay(stepMs())
        for (step in hint.plan) {
            setState(EliminationSteps.advance(
                state = getState(),
                currentCell = step.cellIdx,
                doomedDigits = step.doomedDigits,
                explainerCells = step.explainerCells,
            ))
            delay(sweepMs())
        }
        setState(EliminationSteps.advance(
            state = getState(),
            currentCell = null,
            doomedDigits = emptySet(),
            explainerCells = emptySet(),
        ))
        delay(stepMs())
        setState(EliminationSteps.complete(getState(), hint, countAsHint = countAsHint))
    }

    private suspend fun runCandidateSweep(hint: Hint.CandidateSweep, countAsHint: Boolean) {
        setState(CandidateSweepSteps.begin(getState(), hint))
        delay(stepMs())
        if (hint.initialFill) {
            setState(CandidateSweepSteps.fill(getState()))
            delay(stepMs())
        }
        for (plan in hint.plan) {
            setState(CandidateSweepSteps.advance(
                state = getState(),
                currentCell = plan.cellIdx,
                doomedDigits = plan.doomedDigits,
                eliminatorPeers = plan.eliminatorPeers,
            ))
            delay(sweepMs())
        }
        setState(CandidateSweepSteps.advance(
            state = getState(),
            currentCell = null,
            doomedDigits = emptySet(),
            eliminatorPeers = emptySet(),
        ))
        delay(stepMs())
        setState(CandidateSweepSteps.complete(getState(), hint, countAsHint = countAsHint))
    }
}
