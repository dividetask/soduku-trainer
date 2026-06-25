package com.dividetask.sudokutrainer.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.data.PuzzleRepository
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.GameReducer
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.GuessColor
import com.dividetask.sudokutrainer.domain.HintEngine
import com.dividetask.sudokutrainer.domain.HintUi
import com.dividetask.sudokutrainer.domain.Move
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: PuzzleRepository,
    private val config: GameConfig,
    difficulty: Difficulty,
) : ViewModel() {

    private val _state: MutableStateFlow<GameState> = MutableStateFlow(
        GameState.forNewGame(repository.pickRandom(difficulty))
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _exitSignal = MutableStateFlow(false)
    val exitSignal: StateFlow<Boolean> = _exitSignal.asStateFlow()

    private val animator = HintAnimator(
        scope = viewModelScope,
        stepDelayMs = { config.hintStepMs },
        sweepCellMs = { config.sweepCellMs },
        getState = { _state.value },
        setState = { _state.value = it },
    )

    init {
        // Auto-trigger solve when the board has few enough empty cells
        // — fires once, regardless of correctness of placed values.
        viewModelScope.launch {
            _state.collect { s ->
                if (!autoSolveStarted &&
                    s.phase == GameState.Phase.Playing &&
                    s.isMostlySolved(config.solveCriteria)
                ) {
                    onSolve()
                }
            }
        }
    }

    /** Whether the puzzle qualifies for auto-solve (driven by config.yaml). */
    val isReadyToSolve: Boolean
        get() = _state.value.isMostlySolved(config.solveCriteria)

    fun onDigitSelected(digit: Int) {
        _state.value = GameReducer.selectDigit(_state.value, digit)
    }

    fun onPencilToggled() {
        _state.value = GameReducer.togglePencil(_state.value)
    }

    fun onColorSelected(color: GuessColor) {
        _state.value = GameReducer.selectColor(_state.value, color)
    }

    fun onCellTapped(row: Int, col: Int) {
        _state.value = GameReducer.tapCell(_state.value, row, col)
    }

    fun onCellDoubleTapped(row: Int, col: Int) {
        _state.value = GameReducer.doubleTapCell(_state.value, row, col)
    }

    fun onUndo() {
        _state.value = GameReducer.undo(_state.value)
    }

    fun onClear() {
        _state.value = GameReducer.resetBoard(_state.value)
    }

    fun onGiveUp() {
        _exitSignal.value = true
    }

    val gameConfig: GameConfig get() = config

    @Volatile private var autoSolveStarted: Boolean = false

    fun onSolve() {
        if (autoSolveStarted) return
        autoSolveStarted = true
        _state.value = GameReducer.beginAutoSolve(_state.value)
        viewModelScope.launch {
            while (_state.value.emptyCellCount > 0) {
                delay(config.solveSpeed)
                _state.value = GameReducer.solveSingleCell(_state.value)
            }
            _state.value = _state.value.copy(phase = GameState.Phase.Celebrating)
            delay(config.fireworkDuration)
            _exitSignal.value = true
        }
    }

    fun onShowAgain() {
        val s = _state.value
        if (s.phase != GameState.Phase.Playing) return
        if (animator.isRunning) return
        val hint = s.lastHint ?: return
        // If the last history entry is still the hint's own move, undo
        // it so the animation has something to apply. (If the player
        // already pressed Undo manually, the hint isn't on the board
        // anymore — just re-apply.)
        val needsUndo = hintMatchesTopOfHistory(s, hint)
        val base = if (needsUndo) GameReducer.undo(s) else s
        _state.value = base.copy(lastHint = hint)
        animator.launch(
            hint = hint,
            countAsHint = false,
            speedMultiplier = config.showAgainMultiplier,
        )
    }

    private fun hintMatchesTopOfHistory(state: GameState, hint: HintEngine.Hint): Boolean {
        val last = state.history.lastOrNull() ?: return false
        return when (hint) {
            is HintEngine.NakedSingle, is HintEngine.HiddenSingle -> last is Move.SetValue
            else -> last is Move.PencilSweep
        }
    }

    fun onSweep() {
        val s = _state.value
        if (s.phase != GameState.Phase.Playing) return
        if (s.hintUi != null) return
        if (animator.isRunning) return
        if (s.hasIncorrectEntry) return

        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        val marks = Array(81) { i -> s.cells[i].pencilMarks.keys.toSet() }
        val sweep = HintEngine.findCandidateSweep(grid, marks) ?: return
        animator.launch(sweep, countAsHint = false)
    }

    private var pendingHintJob: Job? = null

    fun onHint() {
        val s = _state.value
        // Pressing Hint while an animation is in flight skips to the
        // final placement.
        if (animator.isRunning) {
            animator.skip()
            return
        }
        if (s.phase != GameState.Phase.Playing) return
        if (s.hintUi != null) return

        // Stage 2 or 3: a pending hint already exists from a prior press.
        val ph = s.pendingHint
        if (ph != null) {
            if (!ph.showCells) {
                _state.value = GameReducer.advancePendingHintRevealCells(s)
                startPendingHintTimer()
            } else {
                // Stage 3: clear the pending hint and run the animation.
                pendingHintJob?.cancel()
                _state.value = GameReducer.clearPendingHint(s)
                animator.launch(ph.hint)
            }
            return
        }

        // Stage 1: find a new hint.
        // If the board has any incorrect entries, the engine would reason
        // from inconsistent state; flash red instead.
        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        val marks = Array(81) { i -> s.cells[i].pencilMarks.keys.toSet() }
        val hint = if (s.hasIncorrectEntry) null else HintEngine.find(grid, marks)

        when (hint) {
            null -> {
                _state.value = s.copy(hintUi = HintUi.NoHintFlash)
                viewModelScope.launch {
                    delay(config.hintFlashMs)
                    _state.value = _state.value.copy(hintUi = null)
                }
            }
            is HintEngine.NakedSingle -> {
                _state.value = GameReducer.setPendingHint(s, hint, setOf(hint.targetCell))
                startPendingHintTimer()
            }
            is HintEngine.HiddenSingle -> {
                _state.value = GameReducer.setPendingHint(s, hint, setOf(hint.targetCell))
                startPendingHintTimer()
            }
            is HintEngine.NoteSingle -> {
                _state.value = GameReducer.setPendingHint(s, hint, setOf(hint.targetCell))
                startPendingHintTimer()
            }
            is HintEngine.CandidateSweep -> {
                // No single target cell, so no progressive flow — animate now.
                animator.launch(hint)
            }
            is HintEngine.EliminationHint -> {
                _state.value = GameReducer.setPendingHint(s, hint, hint.keyCells)
                startPendingHintTimer()
            }
        }
    }

    private fun startPendingHintTimer() {
        pendingHintJob?.cancel()
        pendingHintJob = viewModelScope.launch {
            delay(config.pendingHintMs)
            if (_state.value.pendingHint != null) {
                _state.value = GameReducer.clearPendingHint(_state.value)
            }
        }
    }

    companion object {
        fun factory(repository: PuzzleRepository, config: GameConfig, difficulty: Difficulty) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    GameViewModel(repository, config, difficulty) as T
            }
    }
}
