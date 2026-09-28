package com.dividetask.sudokutrainer.puzzlegen

import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.Puzzle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Offline puzzle generator with technique-based difficulty grading.
 *
 * Usage:
 *   ./gradlew -PincludeApp=false :tools:puzzle-gen:run \
 *       --args "--count 500 --out app/src/main/assets/puzzles/v3.json"
 *
 * Every generated puzzle is graded and kept under whatever level it
 * lands on (rather than discarded when it misses a target level), until
 * each requested level holds [count] unique puzzles or [--max-minutes]
 * elapses. Work is spread over [--threads] worker threads.
 */

@Serializable
private data class PuzzleFile(val version: Int, val puzzles: List<Puzzle>)

private data class Args(
    val countPerLevel: Int,
    val levels: List<Int>,
    val seed: Long?,
    val outPath: String,
    val threads: Int,
    val maxMinutes: Int,
    val version: Int,
)

private fun parseArgs(argv: Array<String>): Args {
    var count = 20
    var levels = (1..9).toList()
    var seed: Long? = null
    var out = "app/src/main/assets/puzzles/v3.json"
    var threads = Runtime.getRuntime().availableProcessors()
    var maxMinutes = 60
    var version = 3
    var i = 0
    while (i < argv.size) {
        when (argv[i]) {
            "--count" -> { count = argv[i + 1].toInt(); i += 2 }
            "--level" -> { levels = listOf(argv[i + 1].toInt()); i += 2 }
            "--seed" -> { seed = argv[i + 1].toLong(); i += 2 }
            "--out" -> { out = argv[i + 1]; i += 2 }
            "--threads" -> { threads = argv[i + 1].toInt(); i += 2 }
            "--max-minutes" -> { maxMinutes = argv[i + 1].toInt(); i += 2 }
            "--version" -> { version = argv[i + 1].toInt(); i += 2 }
            else -> error("Unknown arg: ${argv[i]}")
        }
    }
    return Args(count, levels, seed, out, threads, maxMinutes, version)
}

fun main(argv: Array<String>) {
    val args = parseArgs(argv)
    val buckets = args.levels.associateWith { LinkedHashMap<List<Int>, IntArray>() }
    val lock = Any()
    val done = AtomicBoolean(false)
    val deadline = System.currentTimeMillis() + args.maxMinutes * 60_000L

    fun needed(): List<Int> = buckets.filter { it.value.size < args.countPerLevel }.keys.toList()

    println("Generating ${args.countPerLevel} puzzles for levels ${args.levels} " +
        "on ${args.threads} threads...")

    val totalMs = measureTimeMillis {
        val workers = (0 until args.threads).map { t ->
            thread(name = "gen-$t") {
                val random = if (args.seed != null) Random(args.seed + t) else Random.Default
                val generator = SudokuGenerator(random)
                while (!done.get()) {
                    // Aim at a random level that still needs puzzles; keep
                    // the result wherever it actually grades.
                    val aim = synchronized(lock) { needed() }.randomOrNull(random) ?: break
                    val (lo, hi) = SudokuGenerator.emptyRangeForLevel(aim)
                    val result = generator.generateGraded(lo + random.nextInt(hi - lo + 1))
                    synchronized(lock) {
                        val bucket = buckets[result.level] ?: return@synchronized
                        if (bucket.size >= args.countPerLevel) return@synchronized
                        if (bucket.putIfAbsent(result.givens.toList(), result.solution) == null &&
                            bucket.size % 100 == 0
                        ) {
                            println("  level ${result.level}: ${bucket.size}/${args.countPerLevel}")
                        }
                        if (needed().isEmpty()) done.set(true)
                    }
                    if (System.currentTimeMillis() > deadline) done.set(true)
                }
            }
        }
        workers.forEach { it.join() }
    }

    val prefix = "v${args.version}"
    var idCounter = 0
    val puzzles = args.levels.flatMap { level ->
        buckets.getValue(level).map { (givens, solution) ->
            idCounter++
            Puzzle(
                id = "$prefix-%05d".format(idCounter),
                difficulty = Difficulty.fromLevel(level),
                givens = givens,
                solution = solution.toList(),
            )
        }
    }

    val json = Json { prettyPrint = false; encodeDefaults = true }
    val out = File(args.outPath)
    out.parentFile?.mkdirs()
    out.writeText(json.encodeToString(PuzzleFile.serializer(), PuzzleFile(args.version, puzzles)))

    println("\nWrote ${puzzles.size} puzzles to ${out.absolutePath}")
    for (level in args.levels) {
        val count = buckets.getValue(level).size
        val warn = if (count < args.countPerLevel) "  (WARNING: short of ${args.countPerLevel})" else ""
        println("  Level $level (${Difficulty.fromLevel(level).label}): $count$warn")
    }
    println("Total time: ${totalMs / 1000}s")
}
