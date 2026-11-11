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

    private var drill: TrainerDrill? = null

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
                if (TrainerJudge.judge(d, s) == TrainerJudge.Verdict.Complete) onSolved()
            }
        }
    }

    fun nextDrill() {
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
            _state.value = d.toGameState().copy(
                pencilOn = prev?.pencilOn ?: d.method.usesNotes,
                activeColor = prev?.activeColor ?: GuessColor.DEFAULT,
            )
            _status.value = Status.Playing
        }
    }

    /** Singles still to place in a placement drill; 0 for note drills. */
    fun remaining(state: GameState): Int = drill?.let { TrainerJudge.remaining(it, state) } ?: 0

    private fun onSolved() {
        _status.value = Status.Solved
        viewModelScope.launch {
            delay(SOLVED_PAUSE_MS)
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
