package com.dividetask.sudokutrainer.domain

import kotlinx.serialization.Serializable

/** A pencil mark added or cleared on a specific cell — used by undo. */
@Serializable
data class AutoClearedMark(
    val row: Int,
    val col: Int,
    val digit: Int,
    val color: GuessColor,
)

@Serializable
sealed interface Move {

    @Serializable
    data class SetValue(
        val row: Int,
        val col: Int,
        val newValue: Int,
        val newValueColor: GuessColor,
        val clearedMarks: Map<Int, GuessColor>,
        val autoClearedMarks: List<AutoClearedMark> = emptyList(),
    ) : Move {
        init { require(newValue in 1..9) }
    }

    @Serializable
    data class ClearValue(
        val row: Int,
        val col: Int,
        val prevValue: Int,
        val prevValueColor: GuessColor,
    ) : Move {
        init { require(prevValue in 1..9) }
    }

    @Serializable
    data class TogglePencilMark(
        val row: Int,
        val col: Int,
        val digit: Int,
        val added: Boolean,
        val markColor: GuessColor,
    ) : Move {
        init { require(digit in 1..9) }
    }

    /**
     * One Candidate Sweep step: any number of cells gained or lost pencil
     * marks atomically. [addedMarks] are removed on undo; [removedMarks]
     * are restored.
     */
    @Serializable
    data class PencilSweep(
        val addedMarks: List<AutoClearedMark>,
        val removedMarks: List<AutoClearedMark>,
    ) : Move
}
