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
import com.dividetask.sudokutrainer.domain.BoardMoves
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.EntryCheck
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.GuessColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives one game by wiring the game's parts together: player moves (and
 * the "prevent incorrect" checks), [HintFlow], [StartingSweep],
 * [AutoSolver], [GameSaver] and [BoardOverlays]. [difficulty] picks a new
 * puzzle; when [resume] is set the saved game is loaded instead (falling
 * back to a new puzzle if the save can't be read).
 */
class GameViewModel(
    repository: PuzzleRepository,
    private val config: GameConfig,
    private val settingsStore: SettingsStore,
    savedGames: SavedGameStore,
    difficulty: Difficulty,
    resume: Boolean,
) : ViewModel() {

    private val settings: Settings get() = settingsStore.settings.value

    private val resumed: GameState? = if (resume) savedGames.load() else null

    private val board = BoardStore(resumed ?: GameState.forNewGame(repository.pickRandom(difficulty)))
    val state: StateFlow<GameState> = board.flow

    private val _exitSignal = MutableStateFlow(false)
    val exitSignal: StateFlow<Boolean> = _exitSignal.asStateFlow()

    val gameConfig: GameConfig get() = config

    private val animator = HintAnimator(
        scope = viewModelScope,
        stepDelayMs = { config.hintStepMs },
        sweepCellMs = { config.sweepCellMs },
        getState = { board.value },
        setState = { board.value = it },
    )
    private val overlays = BoardOverlays(
        scope = viewModelScope,
        flashMs = { config.hintFlashMs },
        mistakesMs = { config.pendingHintMs },
        get = { board.value },
        set = { board.value = it },
    )
    private val hints = HintFlow(
        scope = viewModelScope,
        board = board,
        animator = animator,
        overlays = overlays,
        pendingHintMs = { config.pendingHintMs },
        showAgainMultiplier = { config.showAgainMultiplier },
        showWork = { settings.showHintWork },
    )
    private val startingSweep = StartingSweep(viewModelScope, board, animator)
    private val autoSolver = AutoSolver(
        scope = viewModelScope,
        solveCriteria = { config.solveCriteria },
        cellDelayMs = { config.solveSpeed },
        celebrationMs = { config.fireworkDuration },
        board = board,
        onFinished = { _exitSignal.value = true },
    )
    private val saver = GameSaver(savedGames, isFinishing = { autoSolver.started })

    init {
        if (resumed == null) startingSweep.run(settings.autofillCandidates, settings.showHintWork)
        saver.watch(viewModelScope, board.flow)
        autoSolver.watch()
    }

    fun onDigitSelected(digit: Int) {
        board.value = BoardMoves.selectDigit(board.value, digit)
    }

    fun onPencilToggled() {
        board.value = BoardMoves.togglePencil(board.value)
    }

    fun onColorSelected(color: GuessColor) {
        board.value = BoardMoves.selectColor(board.value, color)
    }

    fun onCellTapped(row: Int, col: Int) {
        applyPlayerMove(BoardMoves.tapCell(board.value, row, col))
    }

    fun onCellDoubleTapped(row: Int, col: Int) {
        overlays.clearMistakes()
        board.value = BoardMoves.doubleTapCell(board.value, row, col)
    }

    fun onUndo() {
        overlays.clearMistakes()
        board.value = BoardMoves.undo(board.value)
    }

    fun onClear() {
        if (board.value.phase != GameState.Phase.Playing) return
        overlays.clearMistakes()
        board.value = BoardMoves.resetBoard(board.value)
        startingSweep.run(settings.autofillCandidates, settings.showHintWork)
    }

    fun onHint() = hints.onHint()

    fun onShowAgain() = hints.onShowAgain()

    /**
     * Leave for the main menu. The game is saved first (finishing any
     * hint animation) so Resume picks up exactly here.
     */
    fun onMainMenu() {
        if (animator.isRunning) animator.skip()
        saver.saveNow(board.value)
        _exitSignal.value = true
    }

    /** Commit [next] unless the "prevent incorrect" settings forbid the move. */
    private fun applyPlayerMove(next: GameState) {
        overlays.clearMistakes()
        val before = board.value
        val s = settings
        if (EntryCheck.isForbidden(before, next, s.preventIncorrectValues, s.preventIncorrectNotes)) {
            overlays.flashError()
            return
        }
        // [next] was built before clearMistakes(), so carry over the
        // current overlay rather than its stale one.
        board.value = next.copy(hintUi = board.value.hintUi)
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
