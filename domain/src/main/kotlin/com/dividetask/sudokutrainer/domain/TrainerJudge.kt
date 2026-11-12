package com.dividetask.sudokutrainer.domain

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
     * - Placement drills: every placed value must come from the method —
     *   one of the drill's starting placements, or a single that earlier
     *   placements created. The drill is complete once every target (the
     *   singles the method reaches, repeated to the end) is placed.
     * - Note drills: no value may be placed, and notes may only be
     *   removed — only ones the method removes on its way to the end,
     *   and only once it can reach them. The drill is complete once all
     *   of [TrainerDrill.allEliminations] are removed.
     */
    fun judge(drill: TrainerDrill, state: GameState): Verdict {
        if (!drill.method.usesNotes) return judgePlacements(drill, state)

        for (i in 0 until 81) {
            val v = state.cells[i].value ?: continue
            if (drill.grid[i] != v) return Verdict.Wrong
        }
        val removed = removedNotes(drill, state) ?: return Verdict.Wrong
        val startElims = drill.instanceEliminations.flatten().toSet()
        for (r in removed) {
            if (r !in drill.allEliminations) return Verdict.Wrong
            // A later elimination only counts once the method reaches it.
            if (r !in startElims && !justifiedNow(drill, state, r)) return Verdict.Wrong
        }
        return if (removed.containsAll(drill.allEliminations)) Verdict.Complete else Verdict.InProgress
    }

    /** Notes removed since the start, or null if any note was added. */
    private fun removedNotes(drill: TrainerDrill, state: GameState): Set<NoteRef>? {
        val removed = mutableSetOf<NoteRef>()
        for (i in 0 until 81) {
            if (state.cells[i].value != null) continue
            val now = state.cells[i].pencilMarks.keys
            val start = drill.marks[i]
            if (!start.containsAll(now)) return null
            for (d in start) if (d !in now) removed += NoteRef(i, d)
        }
        return removed
    }

    /** Whether, with [note] put back, the method on the current board removes it. */
    private fun justifiedNow(drill: TrainerDrill, state: GameState, note: NoteRef): Boolean {
        val grid = IntArray(81) { state.cells[it].value ?: 0 }
        val marks = Array(81) { state.cells[it].pencilMarks.keys.toSet() }
        marks[note.cell] = marks[note.cell] + note.digit
        return HintEngine.allEliminations(drill.method.techniqueName, grid, marks).any { note in eliminationsOf(it) }
    }

    /**
     * Progress counters for the drill screen. [notes] is null for
     * placement drills; [applications] counts singles placed there, and
     * fully applied instances of the method in note drills.
     */
    data class Progress(val notes: Pair<Int, Int>?, val applications: Pair<Int, Int>)

    fun progress(drill: TrainerDrill, state: GameState): Progress {
        if (!drill.method.usesNotes) {
            val total = drill.targets.size
            return Progress(null, (total - remaining(drill, state)) to total)
        }
        val removed = removedNotes(drill, state).orEmpty()
        val notesDone = drill.allEliminations.count { it in removed }
        val appsDone = drill.applications.count { removed.containsAll(it) }
        return Progress(notesDone to drill.allEliminations.size, appsDone to drill.applications.size)
    }

    /** Targets still to place in a placement drill (0 for note drills). */
    fun remaining(drill: TrainerDrill, state: GameState): Int =
        drill.targets.count { state.cells[it.cell].value != it.digit }

    private fun judgePlacements(drill: TrainerDrill, state: GameState): Verdict {
        val grid = IntArray(81) { state.cells[it].value ?: 0 }
        for (i in 0 until 81) {
            val v = grid[i]
            if (v == 0 || drill.grid[i] == v) continue
            if (NoteRef(i, v) in drill.placements) continue
            if (!isSingle(drill.method, grid, i, v) || drill.puzzle.solution[i] != v) return Verdict.Wrong
        }
        return if (remaining(drill, state) == 0) Verdict.Complete else Verdict.InProgress
    }

    /**
     * Whether [digit] at [cell] is a single of [method] given the rest of
     * [grid] — used for singles that the player's own placements opened up.
     */
    private fun isSingle(method: TrainerMethod, grid: IntArray, cell: Int, digit: Int): Boolean {
        val g = grid.copyOf().also { it[cell] = 0 }
        val r = cell / 9; val c = cell % 9
        if (!SudokuSolver.isValidPlacement(g, r, c, digit)) return false
        return when (method) {
            TrainerMethod.NakedSingle ->
                (1..9).count { SudokuSolver.isValidPlacement(g, r, c, it) } == 1
            TrainerMethod.HiddenSingle -> unitsOf(cell).any { unit ->
                unit.none { o -> o != cell && g[o] == 0 && SudokuSolver.isValidPlacement(g, o / 9, o % 9, digit) }
            }
            else -> false
        }
    }

    private fun unitsOf(cell: Int): List<List<Int>> {
        val r = cell / 9; val c = cell % 9
        val br = r / 3 * 3; val bc = c / 3 * 3
        return listOf(
            (0 until 9).map { r * 9 + it },
            (0 until 9).map { it * 9 + c },
            (0 until 9).map { (br + it / 3) * 9 + bc + it % 3 },
        )
    }

    /** The hint to show when the player asks for one: the method's next step on the board now. */
    fun hintFor(drill: TrainerDrill, state: GameState): Hint? {
        if (drill.instances.isEmpty()) {
            // Singles open up as the player goes, so look at the board now.
            val grid = IntArray(81) { state.cells[it].value ?: 0 }
            return when (drill.method) {
                TrainerMethod.NakedSingle -> HintEngine.allNakedSingles(grid).firstOrNull()
                TrainerMethod.HiddenSingle -> HintEngine.allHiddenSingles(grid).firstOrNull()
                else -> null
            }
        }
        // Later applications open up as notes go, so look at the board now.
        val grid = IntArray(81) { state.cells[it].value ?: 0 }
        val marks = Array(81) { state.cells[it].pencilMarks.keys.toSet() }
        val now = HintEngine.allEliminations(drill.method.techniqueName, grid, marks)
        return now.firstOrNull { h -> eliminationsOf(h).all { it in drill.allEliminations } } ?: now.firstOrNull()
    }
}
