package com.dividetask.sudokutrainer.domain

/**
 * One cell on the 9x9 board.
 *
 * - [given] cells are puzzle clues; they always have a non-null [value] of
 *   [CellColor.Given] color and no pencil marks, and they never change.
 * - Non-given cells may be empty ([value] == null), filled with a player
 *   entry ([CellColor.Guess]), or filled by the Solve action
 *   ([CellColor.Solve]).
 * - [pencilMarks] is a map from digit (1..9) to the [GuessColor] the mark
 *   was written in. Pencil marks are only *rendered* when [value] is null,
 *   but they are retained in data when a value is set so that clearing the
 *   value reveals them again.
 */
data class Cell(
    val row: Int,
    val col: Int,
    val given: Boolean,
    val value: Int? = null,
    val valueColor: CellColor? = null,
    val pencilMarks: Map<Int, GuessColor> = emptyMap(),
) {
    init {
        require(row in 0..8 && col in 0..8)
        require(value == null || value in 1..9)
        require((value == null) == (valueColor == null)) {
            "value and valueColor must both be null or both be set"
        }
        if (given) {
            require(value != null && valueColor == CellColor.Given) {
                "givens must have a value with CellColor.Given"
            }
            require(pencilMarks.isEmpty()) { "givens cannot have pencil marks" }
        } else {
            require(valueColor != CellColor.Given) {
                "non-givens cannot have CellColor.Given"
            }
        }
        require(pencilMarks.keys.all { it in 1..9 }) {
            "pencil mark digits must be 1..9"
        }
    }

    val isEmpty: Boolean get() = value == null
}
