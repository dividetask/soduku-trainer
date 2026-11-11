package com.dividetask.sudokutrainer.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.data.PuzzleRepository
import com.dividetask.sudokutrainer.data.SavedGameStore
import com.dividetask.sudokutrainer.data.Settings
import com.dividetask.sudokutrainer.data.SettingsStore
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.EntryCheck
import com.dividetask.sudokutrainer.domain.GameReducer
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.GuessColor
import com.dividetask.sudokutrainer.domain.HintEngine
import com.dividetask.sudokutrainer.domain.HintUi
import com.dividetask.sudokutrainer.domain.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives one game. [difficulty] picks a new puzzle; when [resume] is set
 * the saved game is loaded instead (falling back to a new puzzle if the
 * save can't be read).
 */
class GameViewModel(
    private val repository: PuzzleRepository,
    private val config: GameConfig,
    private val settingsStore: SettingsStore,
    private val savedGames: SavedGameStore,
    difficulty: Difficulty,
    resume: Boolean,
) : ViewModel() {

    private val settings: Settings get() = settingsStore.settings.value

    private val resumed: GameState? = if (resume) savedGames.load() else null

    private val _state: MutableStateFlow<GameState> = MutableStateFlow(
        resumed ?: GameState.forNewGame(repository.pickRandom(difficulty))
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

    // Declared before init: a resumed, nearly finished game starts its
    // auto-solve from init, and a later initializer would reset this.
    @Volatile private var autoSolveStarted: Boolean = false

    init {
        if (resumed == null) runStartingSweep()

        // Keep the saved game in step with the board, for Resume. The
        // collector runs in order, so a later state is never overwritten
        // by an earlier one. Mid-hint frames are skipped; a finished game
        // is no longer resumable.
        viewModelScope.launch(Dispatchers.IO) {
            _state.collect { s ->
                when (s.phase) {
                    GameState.Phase.Playing -> if (!autoSolveStarted) savedGames.save(s)
                    GameState.Phase.AutoSolving, GameState.Phase.Celebrating -> savedGames.clear()
                    GameState.Phase.Hinting -> Unit
                }
            }
        }

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
        applyPlayerMove(GameReducer.tapCell(_state.value, row, col))
    }

    /** Commit [next] unless the "prevent incorrect" settings forbid the move. */
    private fun applyPlayerMove(next: GameState) {
        val before = _state.value
        val s = settings
        if (EntryCheck.isForbidden(before, next, s.preventIncorrectValues, s.preventIncorrectNotes)) {
            flashError()
            return
        }
        _state.value = next
    }

    /** Flash the board red; the rejected move is never applied. */
    private fun flashError() {
        _state.value = _state.value.copy(hintUi = HintUi.ErrorFlash)
        viewModelScope.launch {
            delay(config.hintFlashMs)
            if (_state.value.hintUi == HintUi.ErrorFlash) {
                _state.value = _state.value.copy(hintUi = null)
            }
        }
    }

    /**
     * With "autofill starting candidates" on, open the game as if Sweep
     * were pressed — animated or not per "show work". The sweep becomes
     * part of the starting position, so Undo doesn't take the notes away.
     */
    private fun runStartingSweep() {
        if (!settings.autofillCandidates) return
        val s = _state.value
        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        val marks = Array(81) { i -> s.cells[i].pencilMarks.keys.toSet() }
        val sweep = HintEngine.findCandidateSweep(grid, marks) ?: return
        val job = animator.run(sweep, settings.showHintWork, countAsHint = false)
        if (job == null) {
            forgetStartingSweep()
        } else {
            viewModelScope.launch { job.join(); forgetStartingSweep() }
        }
    }

    private fun forgetStartingSweep() {
        val s = _state.value
        if (s.history.size == 1 && s.history[0] is Move.PencilSweep) {
            _state.value = s.copy(history = emptyList(), lastHint = null)
        }
    }

    fun onCellDoubleTapped(row: Int, col: Int) {
        _state.value = GameReducer.doubleTapCell(_state.value, row, col)
    }

    fun onUndo() {
        _state.value = GameReducer.undo(_state.value)
    }

    fun onClear() {
        if (_state.value.phase != GameState.Phase.Playing) return
        _state.value = GameReducer.resetBoard(_state.value)
        runStartingSweep()
    }

    /**
     * Leave for the main menu. The game is saved first (finishing any
     * hint animation) so Resume picks up exactly here.
     */
    fun onMainMenu() {
        if (animator.isRunning) animator.skip()
        val s = _state.value
        if (s.phase == GameState.Phase.Playing && !autoSolveStarted) {
            savedGames.save(s.copy(hintUi = null, pendingHint = null))
        }
        _exitSignal.value = true
    }

    val gameConfig: GameConfig get() = config

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
                animator.run(ph.hint, settings.showHintWork)
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
                // No single target cell, so no progressive flow — apply now.
                animator.run(hint, settings.showHintWork)
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
        fun factory(
            repository: PuzzleRepository,
            config: GameConfig,
            settingsStore: SettingsStore,
            savedGames: SavedGameStore,
            difficulty: Difficulty,
            resume: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                GameViewModel(repository, config, settingsStore, savedGames, difficulty, resume) as T
        }
    }
}
