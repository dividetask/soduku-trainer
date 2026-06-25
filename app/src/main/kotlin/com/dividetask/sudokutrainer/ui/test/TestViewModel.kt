package com.dividetask.sudokutrainer.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.domain.GameReducer
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintEngine
import com.dividetask.sudokutrainer.ui.game.HintAnimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Walks through [HintTestCases] one at a time, auto-running the
 * appropriate hint animation on each preview puzzle so a developer can
 * compare the teaching animations side-by-side.
 *
 * Pressing the screen's Skip button cuts the current animation to its
 * final placement; the sequencer then advances to the next case after a
 * brief settle delay.
 */
class TestViewModel(
    private val config: GameConfig,
) : ViewModel() {

    val cases: List<HintTestCases.Case> = HintTestCases.cases

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _state = MutableStateFlow(GameState.forNewGame(cases.first().puzzle))
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
        viewModelScope.launch {
            for (i in cases.indices) {
                _index.value = i
                val case = cases[i]
                var s = GameState.forNewGame(case.puzzle)
                if (case.prefillCandidates) {
                    s = GameReducer.populateValidCandidates(s)
                }
                _state.value = s
                delay(INTRO_DELAY_MS)
                val cells = _state.value.cells
                val grid = IntArray(81) { idx -> cells[idx].value ?: 0 }
                val marks = Array(81) { idx -> cells[idx].pencilMarks.keys.toSet() }
                val hint: HintEngine.Hint = HintEngine.find(grid, marks)
                    ?: continue
                animator.launch(hint)?.join()
                delay(SETTLE_DELAY_MS)
            }
            _exitSignal.value = true
        }
    }

    fun onSkip() {
        animator.skip()
    }

    fun onExit() {
        animator.skip()
        _exitSignal.value = true
    }

    companion object {
        private const val INTRO_DELAY_MS = 800L
        private const val SETTLE_DELAY_MS = 1500L

        fun factory(config: GameConfig) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                TestViewModel(config) as T
        }
    }
}
