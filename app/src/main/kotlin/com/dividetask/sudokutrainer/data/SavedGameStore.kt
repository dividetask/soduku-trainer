package com.dividetask.sudokutrainer.data

import android.content.Context
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.SavedGame
import java.io.File

/** Keeps the most recent unfinished game on disk for the Resume button. */
class SavedGameStore(context: Context) {

    private val file = File(context.filesDir, "saved_game.json")
    private val lock = Any()

    fun hasSavedGame(): Boolean = synchronized(lock) { file.exists() }

    fun load(): GameState? = synchronized(lock) {
        if (!file.exists()) return null
        SavedGame.decode(file.readText())?.toGameState()
    }

    fun save(state: GameState) {
        val text = SavedGame.of(state).encode()
        synchronized(lock) {
            // Write then rename, so a crash mid-write never leaves a torn file.
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    fun clear() {
        synchronized(lock) { file.delete() }
    }
}
