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
        val replay = replayNotes(drill, state) ?: return Verdict.Wrong
        if (replay.removed.isEmpty()) return Verdict.InProgress
        // Done once the method finds nothing more on the player's board,
        // whichever valid path they took to get there.
        val left = HintEngine.allEliminations(drill.method.techniqueName, drill.grid.toIntArray(), replay.marks)
        return if (left.isEmpty()) Verdict.Complete else Verdict.InProgress
    }

    /**
     * The player's notes, replayed move by move from the drill's start.
     * [removed] are the notes removed (and not put back); [seen] maps
     * each application of the method that was on the board at some step
     * (by its pattern's key cells) to every note it was ever shown
     * removing — a half-done pattern reappears with fewer eliminations,
     * and must still count once.
     */
    private class NoteReplay(
        val marks: Array<Set<Int>>,
        val removed: Set<NoteRef>,
        val seen: Map<Set<Int>, Set<NoteRef>>,
    )

    /**
     * Replay the history's note moves from the drill's start. Each
     * removal must be one the method makes on the board as it stood at
     * that moment (so any valid path is accepted, and a note a later
     * application removes can't be taken early); a note may be added
     * back only if it was there at the start. Null if any move breaks
     * these rules, or the board doesn't match the replay.
     */
    private fun replayNotes(drill: TrainerDrill, state: GameState): NoteReplay? {
        val grid = drill.grid.toIntArray()
        val name = drill.method.techniqueName
        val marks = Array(81) { drill.marks[it] }
        val seen = mutableMapOf<Set<Int>, MutableSet<NoteRef>>()
        fun removeAll(notes: List<NoteRef>): Boolean {
            val now = HintEngine.allEliminations(name, grid, marks)
            for (h in now) seen.getOrPut(h.keyCells) { mutableSetOf() } += eliminationsOf(h)
            val allowed = now.flatMap(::eliminationsOf).toSet()
            if (!allowed.containsAll(notes)) return false
            for (n in notes) marks[n.cell] = marks[n.cell] - n.digit
            return true
        }
        fun add(n: NoteRef): Boolean {
            if (n.digit !in drill.marks[n.cell]) return false
            marks[n.cell] = marks[n.cell] + n.digit
            return true
        }
        for (move in state.history) {
            val ok = when (move) {
                is Move.TogglePencilMark -> {
                    val n = NoteRef(move.row * 9 + move.col, move.digit)
                    if (move.added) add(n) else removeAll(listOf(n))
                }
                is Move.PencilSweep ->
                    move.addedMarks.all { add(NoteRef(it.row * 9 + it.col, it.digit)) } &&
                        removeAll(move.removedMarks.map { NoteRef(it.row * 9 + it.col, it.digit) })
                // Values are judged separately; note drills place none.
                is Move.SetValue, is Move.ClearValue -> true
            }
            if (!ok) return null
        }
        for (i in 0 until 81) {
            if (state.cells[i].value == null && state.cells[i].pencilMarks.keys != marks[i]) return null
        }
        val removed = (0 until 81).flatMap { i -> (drill.marks[i] - marks[i]).map { NoteRef(i, it) } }.toSet()
        return NoteReplay(marks, removed, seen)
    }

    /**
     * Progress counters for the drill screen. [notes] is null for
     * placement drills; [applications] counts singles placed there, and
     * fully applied instances of the method in note drills. Note-drill
     * totals are what's done plus what the method still reaches from the
     * player's board, so they follow the path the player takes.
     */
    data class Progress(val notes: Pair<Int, Int>?, val applications: Pair<Int, Int>)

    fun progress(drill: TrainerDrill, state: GameState): Progress {
        if (!drill.method.usesNotes) {
            val total = drill.targets.size
            return Progress(null, (total - remaining(drill, state)) to total)
        }
        val replay = replayNotes(drill, state) ?: return Progress(0 to drill.allEliminations.size, 0 to drill.applications.size)
        val ahead = TrainerGenerator.eliminationClosure(drill.method, drill.grid.toIntArray(), replay.marks.toList())
        val notesDone = replay.removed.size
        // Two patterns can justify the same removals; one removal is one
        // application, so count a completed set once, and not at all when
        // it sits inside a larger completed one.
        val completed = replay.seen.values.filter { replay.removed.containsAll(it) }.toSet()
        val appsDone = completed.count { set -> completed.none { other -> other != set && other.containsAll(set) } }
        return Progress(
            notesDone to notesDone + ahead.flatten().toSet().size,
            appsDone to appsDone + ahead.size,
        )
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
        // Any path the method allows is fine, so just take its next step.
        return HintEngine.allEliminations(drill.method.techniqueName, grid, marks).firstOrNull()
    }
}
