package com.dividetask.sudokutrainer.domain

/**
 * Colors the player can use. [Blue] is special: it represents a confident
 * "solve" entry, and placing a Blue value auto-clears Blue pencil marks
 * for that digit from the same row, column, and 3x3 box. All other colors
 * are treated as guesses with no auto-clear behavior.
 *
 * The order matters: the color picker renders them in this order. Blue is
 * displayed as a distinct "Solve" button rather than a color swatch.
 */
enum class GuessColor {
    Blue,
    Red,
    Green,
    Orange,
    Purple,
    Yellow,
    ;

    val isSolveColor: Boolean get() = this == Blue

    companion object {
        val DEFAULT = Blue
        val GUESS_ONLY: List<GuessColor> = entries.filter { it != Blue }
    }
}

sealed interface CellColor {
    data object Given : CellColor
    data object Solve : CellColor
    data class Guess(val color: GuessColor) : CellColor
}
