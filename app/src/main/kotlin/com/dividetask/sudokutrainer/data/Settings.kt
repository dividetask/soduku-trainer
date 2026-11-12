package com.dividetask.sudokutrainer.data

import com.dividetask.sudokutrainer.domain.Difficulty

/** Player-facing options, chosen on the Settings screen. */
data class Settings(
    /** Difficulty for New Game. Null until the player picks one; there is no default. */
    val difficulty: Difficulty? = null,
    /** Run a Candidate Sweep when a game starts, filling in every note. */
    val autofillCandidates: Boolean = true,
    /** Animate how a hint reaches its answer; off applies it at once. */
    val showHintWork: Boolean = true,
    /** Reject (flash red and undo) a wrong value placed in the Solve color. */
    val preventIncorrectValues: Boolean = true,
    /** Reject a Solve-color note that contradicts the solution. */
    val preventIncorrectNotes: Boolean = true,
)
