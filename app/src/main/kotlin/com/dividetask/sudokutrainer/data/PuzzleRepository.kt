package com.dividetask.sudokutrainer.data

import android.content.Context
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.Puzzle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

class PuzzleRepository(private val context: Context) {

    @Serializable
    private data class PuzzleFile(val version: Int, val puzzles: List<Puzzle>)

    private val json = Json { ignoreUnknownKeys = true }

    private val puzzles: List<Puzzle> by lazy { loadFromAssets() }

    private fun loadFromAssets(): List<Puzzle> {
        val text = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val file = json.decodeFromString(PuzzleFile.serializer(), text)
        check(file.puzzles.isNotEmpty()) { "No puzzles in $ASSET_PATH" }
        return file.puzzles
    }

    fun pickRandom(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        val pool = puzzles.filter { it.difficulty == difficulty }
        if (pool.isEmpty()) {
            // Fallback: pick from the full pool if no puzzles at this level.
            return puzzles[random.nextInt(puzzles.size)]
        }
        return pool[random.nextInt(pool.size)]
    }

    companion object {
        private const val ASSET_PATH = "puzzles/v2.json"
    }
}
