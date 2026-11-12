package com.dividetask.sudokutrainer.domain

import kotlin.random.Random

/** Builds [TrainerDrill]s by solving puzzles forward with [HintEngine]. */
object TrainerGenerator {

    /**
     * Solve [puzzle] step by step, always applying the easiest technique,
     * until [method] is the easiest one left. Returns null if the puzzle
     * is solved or gets stuck first. For placement methods, [skip] lets
     * that many earlier opportunities pass, so drills aren't always the
     * puzzle's opening position.
     */
    fun prepare(method: TrainerMethod, puzzle: Puzzle, skip: Int = 0): TrainerDrill? {
        val grid = puzzle.givens.toIntArray()
        val marks: Array<MutableSet<Int>>? =
            if (method.usesNotes) Array(81) { candidatesOf(grid, it).toMutableSet() } else null
        var toSkip = skip
        repeat(MAX_STEPS) {
            if (grid.none { it == 0 }) return null
            val hint = HintEngine.find(grid, marks?.map { it.toSet() }?.toTypedArray()) ?: return null
            if (hint.techniqueName == method.techniqueName) {
                if (toSkip-- <= 0) return buildDrill(method, puzzle, grid, marks)
            }
            if (!apply(hint, grid, marks)) return null
        }
        return null
    }

    /**
     * Draw puzzles from [source] (asked for a difficulty level) until one
     * yields a drill for [method], or [maxPuzzles] have been tried.
     */
    fun generate(
        method: TrainerMethod,
        source: (level: Int) -> Puzzle,
        random: Random = Random.Default,
        maxPuzzles: Int = 500,
    ): TrainerDrill? {
        repeat(maxPuzzles) {
            val level = method.minLevel + random.nextInt(10 - method.minLevel)
            val skip = if (method.usesNotes) 0 else random.nextInt(12)
            prepare(method, source(level), skip)?.let { return it }
        }
        return null
    }

    private fun buildDrill(
        method: TrainerMethod,
        puzzle: Puzzle,
        grid: IntArray,
        marks: Array<MutableSet<Int>>?,
    ): TrainerDrill? {
        val markList = List(81) { marks?.get(it)?.toSet() ?: emptySet() }
        return when (method) {
            TrainerMethod.NakedSingle, TrainerMethod.HiddenSingle -> {
                val hints = if (method == TrainerMethod.NakedSingle) {
                    HintEngine.allNakedSingles(grid)
                } else {
                    HintEngine.allHiddenSingles(grid)
                }
                val placements = hints.mapTo(mutableSetOf()) { h ->
                    when (h) {
                        is Hint.NakedSingle -> NoteRef(h.targetCell, h.solutionDigit)
                        is Hint.HiddenSingle -> NoteRef(h.targetCell, h.solutionDigit)
                        else -> error("unexpected $h")
                    }
                }
                // Logic only ever produces correct placements; a mismatch
                // means a malformed puzzle, so skip it.
                if (placements.isEmpty() || placements.any { puzzle.solution[it.cell] != it.digit }) return null
                val targets = singlesClosure(method, grid)
                if (!targets.containsAll(placements) || targets.any { puzzle.solution[it.cell] != it.digit }) return null
                TrainerDrill(method, puzzle, grid.toList(), markList, placements, emptyList(), targets)
            }
            else -> {
                val instances = HintEngine.allEliminations(
                    method.techniqueName, grid, marks!!.map { it.toSet() }.toTypedArray(),
                )
                if (instances.isEmpty()) return null
                val applications = eliminationClosure(method, grid, markList)
                val drill = TrainerDrill(method, puzzle, grid.toList(), markList, emptySet(), instances, applications = applications)
                if (drill.allEliminations.any { puzzle.solution[it.cell] == it.digit }) return null
                drill
            }
        }
    }

    /**
     * Every placement [method] (a single) yields from [grid]: place all
     * the singles it finds, look again, and repeat until it finds none.
     * Placing a value only removes candidates, so a single stays a
     * single and this set doesn't depend on the order singles are placed.
     */
    internal fun singlesClosure(method: TrainerMethod, grid: IntArray): Set<NoteRef> {
        val g = grid.copyOf()
        val out = mutableSetOf<NoteRef>()
        while (true) {
            val found = singlesOf(method, g).filter { g[it.cell] == 0 }
            if (found.isEmpty()) return out
            for (p in found) { g[p.cell] = p.digit; out += p }
        }
    }

    /**
     * Apply [method] to the notes over and over — the first instance it
     * finds each time, as a hint would — until it finds nothing more.
     * Returns each application's eliminations, in order.
     */
    fun eliminationClosure(method: TrainerMethod, grid: IntArray, marks: List<Set<Int>>): List<Set<NoteRef>> {
        val m = Array(81) { marks[it].toMutableSet() }
        val out = mutableListOf<Set<NoteRef>>()
        repeat(MAX_STEPS) {
            val h = HintEngine.allEliminations(method.techniqueName, grid, m.map { it.toSet() }.toTypedArray())
                .firstOrNull() ?: return out
            val e = eliminationsOf(h)
            for (n in e) m[n.cell].remove(n.digit)
            out += e
        }
        return out
    }

    /** The singles [method] finds on [grid], as placements, in hint order. */
    internal fun singlesOf(method: TrainerMethod, grid: IntArray): List<NoteRef> = when (method) {
        TrainerMethod.NakedSingle -> HintEngine.allNakedSingles(grid).map { NoteRef(it.targetCell, it.solutionDigit) }
        TrainerMethod.HiddenSingle -> HintEngine.allHiddenSingles(grid).map { NoteRef(it.targetCell, it.solutionDigit) }
        else -> emptyList()
    }.distinct()

    /** Apply [hint] to the working board. Returns false if it made no progress. */
    private fun apply(hint: Hint, grid: IntArray, marks: Array<MutableSet<Int>>?): Boolean {
        fun place(cell: Int, digit: Int) {
            grid[cell] = digit
            if (marks != null) {
                marks[cell].clear()
                for (p in peersOf(cell)) marks[p].remove(digit)
            }
        }
        when (hint) {
            is Hint.NakedSingle -> place(hint.targetCell, hint.solutionDigit)
            is Hint.HiddenSingle -> place(hint.targetCell, hint.solutionDigit)
            is Hint.NoteSingle -> place(hint.targetCell, hint.solutionDigit)
            is Hint.CandidateSweep -> {
                // Without notes the engine only offers a sweep once singles
                // run out, which is as far as a placement drill can go.
                if (marks == null || hint.plan.isEmpty()) return false
                for (p in hint.plan) marks[p.cellIdx].removeAll(p.doomedDigits)
            }
            is Hint.EliminationHint -> {
                if (marks == null) return false
                for (s in hint.plan) marks[s.cellIdx].removeAll(s.doomedDigits)
            }
        }
        return true
    }

    private fun candidatesOf(grid: IntArray, idx: Int): Set<Int> =
        if (grid[idx] != 0) emptySet()
        else (1..9).filterTo(mutableSetOf()) { SudokuSolver.isValidPlacement(grid, idx / 9, idx % 9, it) }

    private fun peersOf(idx: Int): Set<Int> {
        val r = idx / 9; val c = idx % 9
        val out = mutableSetOf<Int>()
        for (i in 0 until 9) { out += r * 9 + i; out += i * 9 + c }
        val br = r / 3 * 3; val bc = c / 3 * 3
        for (rr in br until br + 3) for (cc in bc until bc + 3) out += rr * 9 + cc
        out -= idx
        return out
    }

    private const val MAX_STEPS = 400
}
