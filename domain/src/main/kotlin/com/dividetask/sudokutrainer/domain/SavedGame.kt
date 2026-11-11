package com.dividetask.sudokutrainer.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The persistent part of a game in progress, for Resume. Hint animation
 * state is transient and isn't kept; a game is only saved while the
 * player is free to move.
 */
@Serializable
data class SavedGame(
    val puzzle: Puzzle,
    val cells: List<Cell>,
    val history: List<Move>,
    val hintCount: Int = 0,
    val pencilOn: Boolean = false,
    val activeColor: GuessColor = GuessColor.DEFAULT,
) {
    fun toGameState(): GameState = GameState(
        puzzle = puzzle,
        cells = cells,
        history = history,
        hintCount = hintCount,
        pencilOn = pencilOn,
        activeColor = activeColor,
    )

    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun of(state: GameState) = SavedGame(
            puzzle = state.puzzle,
            cells = state.cells,
            history = state.history,
            hintCount = state.hintCount,
            pencilOn = state.pencilOn,
            activeColor = state.activeColor,
        )

        /** Null when [text] isn't a readable save (e.g. from an older version). */
        fun decode(text: String): SavedGame? =
            runCatching { JSON.decodeFromString(serializer(), text) }.getOrNull()
    }
}
