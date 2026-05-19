package com.dividetask.sudokutrainer.domain

/** A pencil mark that was auto-cleared from a peer cell during a Blue/Solve write. */
data class AutoClearedMark(
    val row: Int,
    val col: Int,
    val digit: Int,
    val color: GuessColor,
)

sealed interface Move {
    val row: Int
    val col: Int

    data class SetValue(
        override val row: Int,
        override val col: Int,
        val newValue: Int,
        val newValueColor: GuessColor,
        val clearedMarks: Map<Int, GuessColor>,
        val autoClearedMarks: List<AutoClearedMark> = emptyList(),
    ) : Move {
        init { require(newValue in 1..9) }
    }

    data class ClearValue(
        override val row: Int,
        override val col: Int,
        val prevValue: Int,
        val prevValueColor: GuessColor,
    ) : Move {
        init { require(prevValue in 1..9) }
    }

    data class TogglePencilMark(
        override val row: Int,
        override val col: Int,
        val digit: Int,
        val added: Boolean,
        val markColor: GuessColor,
    ) : Move {
        init { require(digit in 1..9) }
    }
}
