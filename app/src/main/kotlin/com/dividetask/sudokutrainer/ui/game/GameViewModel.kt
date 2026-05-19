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

    fun onSolve() {
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

    fun onHint() {
        // No-op in v1.
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
