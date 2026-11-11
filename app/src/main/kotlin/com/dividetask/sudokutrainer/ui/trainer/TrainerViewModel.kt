package com.dividetask.sudokutrainer.ui.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.data.PuzzleRepository
import com.dividetask.sudokutrainer.data.SettingsStore
import com.dividetask.sudokutrainer.domain.GameReducer
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.GuessColor
import com.dividetask.sudokutrainer.domain.HintEngine
import com.dividetask.sudokutrainer.domain.HintUi
import com.dividetask.sudokutrainer.domain.TrainerDrill
import com.dividetask.sudokutrainer.domain.TrainerGenerator
import com.dividetask.sudokutrainer.domain.TrainerJudge
import com.dividetask.sudokutrainer.domain.TrainerMethod
import com.dividetask.sudokutrainer.ui.game.HintAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drills one [TrainerMethod]: deals a board where the method is the
 * easiest next step, rejects any move the method doesn't make, and deals
 * the next board once the method has been applied in full.
 */
class TrainerViewModel(
    private val repository: PuzzleRepository,
    private val config: GameConfig,
    private val settingsStore: SettingsStore,
    val method: TrainerMethod,
) : ViewModel() {

    enum class Status { Loading, Playing, Solved, NotFound }

    private val _status = MutableStateFlow(Status.Loading)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _state = MutableStateFlow<GameState?>(null)

    /** The board, or null while the first drill is being found. */
    val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _solvedByHint = MutableStateFlow(false)

    /** True while a drill the hint finished waits to be replaced; Show Again replays it. */
    val solvedByHint: StateFlow<Boolean> = _solvedByHint.asStateFlow()

    private var drill: TrainerDrill? = null
    private var advanceJob: Job? = null

    private val animator = HintAnimator(
        scope = viewModelScope,
        stepDelayMs = { config.hintStepMs },
        sweepCellMs = { config.sweepCellMs },
        getState = { _state.value!! },
        setState = { _state.value = it },
    )

    init {
        nextDrill()
        // Hints and player moves both land here; a finished drill (by
        // either route) deals the next one.
        viewModelScope.launch {
            _state.collect { s ->
                val d = drill ?: return@collect
                if (s == null || s.phase != GameState.Phase.Playing) return@collect
                if (_status.value != Status.Playing) return@collect
                if (TrainerJudge.judge(d, s) == TrainerJudge.Verdict.Complete) {
                    // Player moves clear lastHint, so it is only set when
                    // the hint made the finishing move.
                    onSolved(byHint = s.lastHint != null)
                }
            }
        }
    }

    fun nextDrill() {
        advanceJob?.cancel()
        _solvedByHint.value = false
        _status.value = Status.Loading
        viewModelScope.launch {
            val d = withContext(Dispatchers.Default) {
                TrainerGenerator.generate(method, { level -> repository.pickRandom(level) })
            }
            if (d == null) {
                _status.value = Status.NotFound
                return@launch
            }
            drill = d
            // Keep the player's pencil/color choices between drills.
            val prev = _state.value
            val fill = settingsStore.settings.value.autofillCandidates
            _state.value = d.toGameState(fillCandidates = fill).copy(
                pencilOn = prev?.pencilOn ?: d.method.usesNotes,
                activeColor = prev?.activeColor ?: GuessColor.DEFAULT,
            )
            _status.value = Status.Playing
        }
    }

    /** Progress counters for the current drill, or null before one is dealt. */
    fun progress(state: GameState): TrainerJudge.Progress? =
        drill?.let { TrainerJudge.progress(it, state) }

    private fun onSolved(byHint: Boolean) {
        _status.value = Status.Solved
        _solvedByHint.value = byHint
        // A hint's answer stays up longer, so there's time for Show Again.
        scheduleNextDrill(if (byHint) HINT_SOLVED_PAUSE_MS else SOLVED_PAUSE_MS)
    }

    private fun scheduleNextDrill(delayMs: Long) {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(delayMs)
            nextDrill()
        }
    }

    /**
     * Replay the hint that finished the drill, slowed down as in a game,
     * then wait the full pause again before the next drill.
     */
    fun onShowAgain() {
        val s = _state.value ?: return
        if (_status.value != Status.Solved || !_solvedByHint.value || animator.isRunning) return
        val hint = s.lastHint ?: return
        advanceJob?.cancel()
        // Take the hint's move back off the board so the animation can
        // make it again.
        _state.value = GameReducer.undo(s).copy(lastHint = hint)
        val job = animator.launch(hint, countAsHint = false, speedMultiplier = config.showAgainMultiplier)
        advanceJob = viewModelScope.launch {
            job?.join()
            delay(HINT_SOLVED_PAUSE_MS)
            nextDrill()
        }
    }

    private fun update(transform: (GameState) -> GameState) {
        val s = _state.value ?: return
        _state.value = transform(s)
    }

    fun onDigitSelected(digit: Int) = update { GameReducer.selectDigit(it, digit) }
    fun onPencilToggled() = update { GameReducer.togglePencil(it) }
    fun onColorSelected(color: GuessColor) = update { GameReducer.selectColor(it, color) }
    fun onUndo() = update { GameReducer.undo(it) }

    fun onCellTapped(row: Int, col: Int) = playerMove { GameReducer.tapCell(it, row, col) }
    fun onCellDoubleTapped(row: Int, col: Int) = playerMove { GameReducer.doubleTapCell(it, row, col) }

    /** Apply a move unless it isn't one the method makes; then flash red instead. */
    private fun playerMove(transform: (GameState) -> GameState) {
        val d = drill ?: return
        val s = _state.value ?: return
        if (_status.value != Status.Playing) return
        val next = transform(s)
        if (next === s) return
        if (TrainerJudge.judge(d, next) == TrainerJudge.Verdict.Wrong) {
            _state.value = s.copy(hintUi = HintUi.ErrorFlash)
            viewModelScope.launch {
                delay(config.hintFlashMs)
                update { if (it.hintUi == HintUi.ErrorFlash) it.copy(hintUi = null) else it }
            }
            return
        }
        _state.value = next
    }

    /**
     * First press highlights the cells of the pattern; the second shows
     * (or, with "show work" off, applies) the method.
     */
    fun onHint() {
        if (animator.isRunning) {
            animator.skip()
            return
        }
        val d = drill ?: return
        val s = _state.value ?: return
        if (_status.value != Status.Playing || s.phase != GameState.Phase.Playing) return
        val pending = s.pendingHint
        if (pending != null) {
            _state.value = GameReducer.clearPendingHint(s)
            animator.run(pending.hint, settingsStore.settings.value.showHintWork)
            return
        }
        val hint = TrainerJudge.hintFor(d, s) ?: return
        val keyCells = when (hint) {
            is HintEngine.NakedSingle -> setOf(hint.targetCell)
            is HintEngine.HiddenSingle -> setOf(hint.targetCell)
            is HintEngine.EliminationHint -> hint.keyCells
            else -> emptySet()
        }
        _state.value = GameReducer.advancePendingHintRevealCells(GameReducer.setPendingHint(s, hint, keyCells))
    }

    companion object {
        private const val SOLVED_PAUSE_MS = 1_000L
        private const val HINT_SOLVED_PAUSE_MS = 5_000L

        fun factory(
            repository: PuzzleRepository,
            config: GameConfig,
            settingsStore: SettingsStore,
            method: TrainerMethod,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                TrainerViewModel(repository, config, settingsStore, method) as T
        }
    }
}
