package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.data.SavedGameStore
import com.dividetask.sudokutrainer.domain.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Keeps the saved game (for Resume) in step with the board. A finished
 * game — [isFinishing] true, or auto-solving / celebrating — is no longer
 * resumable, and mid-hint frames are never saved.
 */
class GameSaver(
    private val store: SavedGameStore,
    private val isFinishing: () -> Boolean,
) {
    /** Save every board [states] emits. The collector runs in order, so a later board is never overwritten by an earlier one. */
    fun watch(scope: CoroutineScope, states: StateFlow<GameState>) {
        scope.launch(Dispatchers.IO) {
            states.collect { s ->
                when (s.phase) {
                    GameState.Phase.Playing -> if (!isFinishing()) store.save(s)
                    GameState.Phase.AutoSolving, GameState.Phase.Celebrating -> store.clear()
                    GameState.Phase.Hinting -> Unit
                }
            }
        }
    }

    /** Save [state] right away (e.g. when leaving the game). */
    fun saveNow(state: GameState) {
        if (state.phase == GameState.Phase.Playing && !isFinishing()) {
            store.save(state.copy(hintUi = null, pendingHint = null))
        }
    }
}
