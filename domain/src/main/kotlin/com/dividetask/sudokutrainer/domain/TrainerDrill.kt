package com.dividetask.sudokutrainer.domain

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
 *   the method produces on the starting board. [targets] adds every
 *   placement it goes on to produce once those are made, repeated until
 *   it finds nothing new; the drill is complete once all targets are
 *   placed.
 * - [instances] (note methods) are the distinct applications of the
 *   method on the starting board, as hints that can be animated.
 *   [applications] are the eliminations of every application the
 *   method makes when applied over and over (first instance each time)
 *   until it finds nothing more; the drill is complete once all of
 *   them are made.
 */
data class TrainerDrill(
    val method: TrainerMethod,
    val puzzle: Puzzle,
    val grid: List<Int>,
    val marks: List<Set<Int>>,
    val placements: Set<NoteRef>,
    val instances: List<Hint.EliminationHint>,
    val targets: Set<NoteRef> = placements,
    val applications: List<Set<NoteRef>> = instances.map(::eliminationsOf),
) {
    /** The eliminations each starting instance makes. */
    val instanceEliminations: List<Set<NoteRef>> = instances.map(::eliminationsOf)

    /** Every note the method removes on its way to the end (note drills). */
    val allEliminations: Set<NoteRef> = applications.flatten().toSet()

    /**
     * The starting [GameState] for this drill. Note drills always show
     * their notes; placement drills show every cell's candidates only
     * when [fillCandidates] is set (the "autofill starting candidates"
     * setting).
     */
    fun toGameState(fillCandidates: Boolean = false): GameState {
        val g = grid.toIntArray()
        val shown = if (method.usesNotes || !fillCandidates) marks else List(81) { i ->
            if (g[i] != 0) emptySet()
            else (1..9).filterTo(mutableSetOf()) { SudokuSolver.isValidPlacement(g, i / 9, i % 9, it) }
        }
        val cells = List(81) { i ->
            val row = i / 9; val col = i % 9
            when {
                puzzle.givens[i] != 0 ->
                    Cell(row, col, given = true, value = puzzle.givens[i], valueColor = CellColor.Given)
                grid[i] != 0 ->
                    Cell(row, col, given = false, value = grid[i], valueColor = CellColor.Solve)
                else ->
                    Cell(row, col, given = false, pencilMarks = shown[i].associateWith { GuessColor.DEFAULT })
            }
        }
        return GameState(puzzle = puzzle, cells = cells)
    }
}

/** The notes [hint] removes. */
internal fun eliminationsOf(hint: Hint.EliminationHint): Set<NoteRef> =
    hint.plan.flatMap { s -> s.doomedDigits.map { NoteRef(s.cellIdx, it) } }.toSet()
