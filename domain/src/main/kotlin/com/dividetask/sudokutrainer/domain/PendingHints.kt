package com.dividetask.sudokutrainer.domain

/** The first Hint presses: name the technique, then highlight its cells. */
object PendingHints {
    /** Stage 1: technique name revealed, cells not highlighted yet. */
    fun set(state: GameState, hint: Hint, keyCells: Set<Int>): GameState =
        state.copy(pendingHint = PendingHint(
            hint = hint,
            keyCells = keyCells,
            techniqueName = hint.techniqueName,
            showCells = false,
        ))

    /** Stage 2: also highlight the pattern's cells. No-op if already shown. */
    fun revealCells(state: GameState): GameState {
        val ph = state.pendingHint ?: return state
        if (ph.showCells) return state
        return state.copy(pendingHint = ph.copy(showCells = true))
    }

    fun clear(state: GameState): GameState =
        if (state.pendingHint == null) state else state.copy(pendingHint = null)
}
