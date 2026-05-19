package com.dividetask.sudokutrainer.puzzlegen

import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.Puzzle
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Offline puzzle generator with technique-based difficulty grading.
 *
 * Usage:
 *   ./gradlew -PincludeApp=false :tools:puzzle-gen:run \
 *       --args "--count 20 --level 3 --out app/src/main/assets/puzzles/v2.json"
 *
 * If --level is omitted, generates [count] puzzles at each level 1–9
 * (total = count * 9) and writes them all to one file.
 */

@Serializable
private data class PuzzleFile(val version: Int, val puzzles: List<Puzzle>)

private data class Args(
    val countPerLevel: Int,
    val targetLevel: Int?,     // null = all levels
    val seed: Long?,
    val outPath: String,
    val maxAttempts: Int,
)

private fun parseArgs(argv: Array<String>): Args {
    var count = 20
    var level: Int? = null
    var seed: Long? = null
    var out = "app/src/main/assets/puzzles/v2.json"
    var maxAttempts = 500
    var i = 0
    while (i < argv.size) {
        when (argv[i]) {
            "--count" -> { count = argv[i + 1].toInt(); i += 2 }
            "--level" -> { level = argv[i + 1].toInt(); i += 2 }
            "--seed" -> { seed = argv[i + 1].toLong(); i += 2 }
            "--out" -> { out = argv[i + 1]; i += 2 }
            "--max-attempts" -> { maxAttempts = argv[i + 1].toInt(); i += 2 }
            else -> error("Unknown arg: ${argv[i]}")
        }
    }
    return Args(count, level, seed, out, maxAttempts)
}

fun main(argv: Array<String>) {
    val args = parseArgs(argv)
    val random = if (args.seed != null) Random(args.seed) else Random.Default
    val generator = SudokuGenerator(random)

    val levels = if (args.targetLevel != null) {
        listOf(args.targetLevel)
    } else {
        (1..9).toList()
    }

    val puzzles = mutableListOf<Puzzle>()
    var idCounter = 0

    val totalMs = measureTimeMillis {
        for (level in levels) {
            val difficulty = Difficulty.fromLevel(level)
            println("Generating ${args.countPerLevel} puzzles at level $level (${difficulty.label})...")
            var generated = 0
            var attempts = 0
            while (generated < args.countPerLevel && attempts < args.maxAttempts) {
                attempts++
                val result = generator.generateAtLevel(level, maxAttempts = 1)
                    ?: continue
                idCounter++
                val emptyCount = result.givens.count { it == 0 }
                val puzzle = Puzzle(
                    id = "v2-%04d".format(idCounter),
                    difficulty = difficulty,
                    givens = result.givens.toList(),
                    solution = result.solution.toList(),
                )
                puzzles += puzzle
                generated++
                println("  [${generated}/${args.countPerLevel}] ${puzzle.id} " +
                    "level=$level empties=$emptyCount (attempt $attempts)")
            }
            if (generated < args.countPerLevel) {
                println("  WARNING: only generated $generated/${args.countPerLevel} at level $level " +
                    "after $attempts attempts")
            }
        }
    }

    val json = Json { prettyPrint = false; encodeDefaults = true }
    val file = PuzzleFile(version = 2, puzzles = puzzles)
    val out = File(args.outPath)
    out.parentFile?.mkdirs()
    out.writeText(json.encodeToString(file))

    println("\nWrote ${puzzles.size} puzzles to ${out.absolutePath}")
    for (level in levels) {
        val count = puzzles.count { it.difficulty.level == level }
        println("  Level $level (${Difficulty.fromLevel(level).label}): $count")
    }
    println("Total time: ${totalMs / 1000}s")
}
