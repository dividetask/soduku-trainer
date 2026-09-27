package com.dividetask.sudokutrainer.data

import android.content.Context
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.Puzzle
import com.dividetask.sudokutrainer.domain.PuzzleTransformer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

class PuzzleRepository(private val context: Context) {

    @Serializable
    private data class PuzzleFile(val version: Int, val puzzles: List<Puzzle>)

    private val json = Json { ignoreUnknownKeys = true }

    private val puzzles: List<Puzzle> by lazy { loadFromAssets() }

    // Per-difficulty shuffle bags: every puzzle in a pool is dealt once
    // before any repeats, and a new bag never starts with the last one dealt.
    private val bags = mutableMapOf<Difficulty, ArrayDeque<Puzzle>>()
    private val lastDealt = mutableMapOf<Difficulty, Puzzle>()

    private fun loadFromAssets(): List<Puzzle> {
        val text = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val file = json.decodeFromString(PuzzleFile.serializer(), text)
        check(file.puzzles.isNotEmpty()) { "No puzzles in $ASSET_PATH" }
        return file.puzzles
    }

    /**
     * Deal the next puzzle for [difficulty], disguised with a random
     * symmetry transform so even a repeated base puzzle looks new.
     */
    @Synchronized
    fun pickRandom(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        // Fallback: use the full pool if no puzzles at this level.
        val pool = puzzles.filter { it.difficulty == difficulty }.ifEmpty { puzzles }
        val bag = bags.getOrPut(difficulty) { ArrayDeque() }
        if (bag.isEmpty()) {
            val refill = pool.shuffled(random).toMutableList()
            val last = lastDealt[difficulty]
            if (refill.size > 1 && refill.first() == last) {
                refill.add(refill.removeAt(0))
            }
            bag.addAll(refill)
        }
        val base = bag.removeFirst()
        lastDealt[difficulty] = base
        return PuzzleTransformer.transform(base, random)
    }

    companion object {
        private const val ASSET_PATH = "puzzles/v2.json"
    }
}
