package com.dividetask.sudokutrainer.domain

/**
 * A hint the player has asked for but hasn't acted on yet — a chance to
 * figure the pattern out themselves with progressive disclosure.
 *
 * Stage progression (driven by repeated Hint presses):
 *  1. [showCells] = false: the technique name is revealed, nothing else.
 *  2. [showCells] = true:  the cells of the pattern are highlighted too.
 *  3. *next press* fires the existing animation and clears this.
 *
 * Cleared automatically when the timer expires, when the player makes
 * any board-changing action, or when the game phase changes.
 */
data class PendingHint(
    val hint: HintEngine.Hint,
    val keyCells: Set<Int>,
    val techniqueName: String,
    val showCells: Boolean = false,
)
