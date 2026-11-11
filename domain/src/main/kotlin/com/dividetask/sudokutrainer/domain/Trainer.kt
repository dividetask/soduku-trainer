package com.dividetask.sudokutrainer.domain

import kotlin.random.Random

/**
 * A solving method the trainer can drill. [techniqueName] matches
 * [HintEngine.Hint.techniqueName]. [usesNotes] is false for the two
 * placement methods, which are drilled on a board without notes.
 * [minLevel] is the easiest puzzle difficulty worth searching for it.
 */
enum class TrainerMethod(
    val techniqueName: String,
    val usesNotes: Boolean,
    val minLevel: Int,
    val explanation: String,
) {
    NakedSingle(
        "Naked Single", usesNotes = false, minLevel = 1,
        "Every digit but one already appears in the cell's row, column or box. " +
            "The one digit left must go in that cell.",
    ),
    HiddenSingle(
        "Hidden Single", usesNotes = false, minLevel = 2,
        "A digit can go in only one cell of a row, column or box, because it " +
            "already appears in a line or box crossing every other cell. Place the " +
            "digit in the one cell left for it.",
    ),
    NakedPair(
        "Naked Pair", usesNotes = true, minLevel = 3,
        "Two cells in a row, column or box have the same two candidates and no " +
            "others. Those two digits must fill those two cells, so remove them from " +
            "every other cell in that unit.",
    ),
    HiddenPair(
        "Hidden Pair", usesNotes = true, minLevel = 3,
        "Two digits can only go in the same two cells of a row, column or box. " +
            "Those cells must hold those two digits, so remove every other candidate " +
            "from them.",
    ),
    PointingPair(
        "Pointing Pair", usesNotes = true, minLevel = 3,
        "Inside a box, every candidate for a digit lies on one row or column. The " +
            "digit must go on that line inside the box, so remove it from the rest of " +
            "the line outside the box.",
    ),
    BoxLineReduction(
        "Box-Line Reduction", usesNotes = true, minLevel = 3,
        "In a row or column, every candidate for a digit lies inside one box. The " +
            "digit must go on that line inside the box, so remove it from the box's " +
            "other cells.",
    ),
    NakedTriple(
        "Naked Triple", usesNotes = true, minLevel = 5,
        "Three cells in a row, column or box have only three digits between them " +
            "as candidates. Those digits must fill those cells, so remove them from " +
            "every other cell in that unit.",
    ),
    HiddenTriple(
        "Hidden Triple", usesNotes = true, minLevel = 5,
        "Three digits can only go in the same three cells of a row, column or box. " +
            "Those cells must hold those digits, so remove every other candidate from " +
            "them.",
    ),
    XWing(
        "X-Wing", usesNotes = true, minLevel = 6,
        "In two rows, a digit's only candidates sit in the same two columns, " +
            "forming a rectangle. The digit must take opposite corners, so remove it " +
            "from the rest of both columns (or swap rows and columns).",
    ),
    EmptyRectangle(
        "Empty Rectangle", usesNotes = true, minLevel = 7,
        "Inside a box, a digit's candidates form an L along one row and one column. " +
            "A line elsewhere with only two spots for the digit links to the L, so " +
            "wherever the digit goes it rules out the cell where that link and the L " +
            "would cross.",
    ),
    Swordfish(
        "Swordfish", usesNotes = true, minLevel = 7,
        "An X-Wing across three lines: in three rows, a digit's candidates all sit " +
            "within the same three columns. The digit must use those columns in those " +
            "rows, so remove it from the rest of the three columns (or swap rows and " +
            "columns).",
    ),
    XYWing(
        "XY-Wing", usesNotes = true, minLevel = 7,
        "A pivot cell with candidates XY sees one cell with XZ and another with YZ. " +
            "Whatever the pivot becomes, one of those two cells must be Z, so remove Z " +
            "from every cell that sees both of them.",
    ),
}

/** One pencil mark: [digit] in the cell at flat index [cell]. */
data class NoteRef(val cell: Int, val digit: Int)

/**
 * A board on which [method] is the easiest next step, plus every way the
 * player may apply it.
 *
 * - [grid] is the board's values (givens plus cells filled while solving
 *   up to this point) and [marks] its pencil marks (all empty when the
 *   method doesn't use notes).
 * - [placements] (placement methods) are the (cell, digit) placements
 *   the method produces; any one of them completes the drill.
 * - [instances] (note methods) are the distinct applications of the
 *   method on this board, as hints that can be animated. The drill is
 *   complete once every elimination of any one instance is made.
 */
data class TrainerDrill(
    val method: TrainerMethod,
    val puzzle: Puzzle,
    val grid: List<Int>,
    val marks: List<Set<Int>>,
    val placements: Set<NoteRef>,
    val instances: List<HintEngine.EliminationHint>,
    val placementHints: List<HintEngine.Hint> = emptyList(),
) {
    /** The eliminations each instance makes. */
    val instanceEliminations: List<Set<NoteRef>> =
        instances.map { h -> h.plan.flatMap { s -> s.doomedDigits.map { NoteRef(s.cellIdx, it) } }.toSet() }

    /** The starting [GameState] for this drill. */
    fun toGameState(): GameState {
        val cells = List(81) { i ->
            val row = i / 9; val col = i % 9
            when {
                puzzle.givens[i] != 0 ->
                    Cell(row, col, given = true, value = puzzle.givens[i], valueColor = CellColor.Given)
                grid[i] != 0 ->
                    Cell(row, col, given = false, value = grid[i], valueColor = CellColor.Solve)
                else ->
                    Cell(row, col, given = false, pencilMarks = marks[i].associateWith { GuessColor.DEFAULT })
            }
        }
        return GameState(puzzle = puzzle, cells = cells)
    }
}

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
                        is HintEngine.NakedSingle -> NoteRef(h.targetCell, h.solutionDigit)
                        is HintEngine.HiddenSingle -> NoteRef(h.targetCell, h.solutionDigit)
                        else -> error("unexpected $h")
                    }
                }
                // Logic only ever produces correct placements; a mismatch
                // means a malformed puzzle, so skip it.
                if (placements.isEmpty() || placements.any { puzzle.solution[it.cell] != it.digit }) return null
                TrainerDrill(method, puzzle, grid.toList(), markList, placements, emptyList(), hints)
            }
            else -> {
                val instances = HintEngine.allEliminations(
                    method.techniqueName, grid, marks!!.map { it.toSet() }.toTypedArray(),
                )
                val drill = TrainerDrill(method, puzzle, grid.toList(), markList, emptySet(), instances)
                if (instances.isEmpty()) return null
                if (drill.instanceEliminations.any { set -> set.any { puzzle.solution[it.cell] == it.digit } }) return null
                drill
            }
        }
    }

    /** Apply [hint] to the working board. Returns false if it made no progress. */
    private fun apply(hint: HintEngine.Hint, grid: IntArray, marks: Array<MutableSet<Int>>?): Boolean {
        fun place(cell: Int, digit: Int) {
            grid[cell] = digit
            if (marks != null) {
                marks[cell].clear()
                for (p in peersOf(cell)) marks[p].remove(digit)
            }
        }
        when (hint) {
            is HintEngine.NakedSingle -> place(hint.targetCell, hint.solutionDigit)
            is HintEngine.HiddenSingle -> place(hint.targetCell, hint.solutionDigit)
            is HintEngine.NoteSingle -> place(hint.targetCell, hint.solutionDigit)
            is HintEngine.CandidateSweep -> {
                // Without notes the engine only offers a sweep once singles
                // run out, which is as far as a placement drill can go.
                if (marks == null || hint.plan.isEmpty()) return false
                for (p in hint.plan) marks[p.cellIdx].removeAll(p.doomedDigits)
            }
            is HintEngine.EliminationHint -> {
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

/** Judges the player's progress on a [TrainerDrill]. */
object TrainerJudge {

    enum class Verdict {
        /** Nothing wrong yet, and not finished. */
        InProgress,

        /** The board shows a move the method doesn't make. */
        Wrong,

        /** The method has been applied in full. */
        Complete,
    }

    /**
     * Compare [state] with the drill's starting board. Judging the whole
     * board (rather than single moves) means undo needs no special care.
     *
     * - Any newly placed value must be one of the drill's placements;
     *   one correct placement completes a placement drill.
     * - In note drills, notes may only be removed, and only ones that
     *   some instance of the method eliminates. The drill is complete
     *   once every elimination of any single instance is made.
     */
    fun judge(drill: TrainerDrill, state: GameState): Verdict {
        var placedCorrectly = false
        for (i in 0 until 81) {
            val v = state.cells[i].value ?: continue
            if (drill.grid[i] == v) continue
            if (NoteRef(i, v) !in drill.placements) return Verdict.Wrong
            placedCorrectly = true
        }
        if (!drill.method.usesNotes) {
            return if (placedCorrectly) Verdict.Complete else Verdict.InProgress
        }
        if (placedCorrectly) return Verdict.Complete

        val removed = mutableSetOf<NoteRef>()
        for (i in 0 until 81) {
            if (state.cells[i].value != null) continue
            val now = state.cells[i].pencilMarks.keys
            val start = drill.marks[i]
            if (!start.containsAll(now)) return Verdict.Wrong
            for (d in start) if (d !in now) removed += NoteRef(i, d)
        }
        val allowed = drill.instanceEliminations.flatten().toSet()
        if (!allowed.containsAll(removed)) return Verdict.Wrong
        return if (drill.instanceEliminations.any { removed.containsAll(it) }) Verdict.Complete
        else Verdict.InProgress
    }

    /**
     * The hint to show when the player asks for one. For note drills it
     * is the instance closest to done, so earlier correct removals
     * aren't wasted.
     */
    fun hintFor(drill: TrainerDrill, state: GameState): HintEngine.Hint? {
        if (drill.instances.isEmpty()) return drill.placementHints.firstOrNull()
        val best = drill.instances.indices.maxBy { k ->
            drill.instanceEliminations[k].count { it.digit !in state.cells[it.cell].pencilMarks }
        }
        return drill.instances[best]
    }
}
