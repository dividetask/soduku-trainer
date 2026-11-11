package com.dividetask.sudokutrainer.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import kotlin.random.Random

class TrainerTest {

    // ---- Drills generated from the bundled puzzle pool ----

    @Serializable
    private data class PuzzleFile(val version: Int, val puzzles: List<Puzzle>)

    private val pool: Map<Int, List<Puzzle>> by lazy {
        val text = File("../app/src/main/assets/puzzles/v3.json").readText()
        Json.decodeFromString(PuzzleFile.serializer(), text).puzzles.groupBy { it.difficulty.level }
    }

    private fun drillFor(method: TrainerMethod): TrainerDrill {
        val random = Random(method.ordinal)
        val drill = TrainerGenerator.generate(method, { level -> pool.getValue(level).random(random) }, random)
        assertNotNull(drill, "no drill found for $method")
        return drill!!
    }

    private fun marksOf(drill: TrainerDrill) = drill.marks.toTypedArray()

    /** A placement drill with at least two starting singles. */
    private fun drillWithSeveralSingles(method: TrainerMethod): TrainerDrill {
        val random = Random(method.ordinal + 1000)
        repeat(200) {
            val d = TrainerGenerator.generate(method, { level -> pool.getValue(level).random(random) }, random)
            if (d != null && d.placements.size >= 2) return d
        }
        error("no $method drill with two or more singles")
    }

    private fun target(hint: HintEngine.Hint): Int = when (hint) {
        is HintEngine.NakedSingle -> hint.targetCell
        is HintEngine.HiddenSingle -> hint.targetCell
        else -> error("not a single: $hint")
    }

    @ParameterizedTest
    @EnumSource(TrainerMethod::class)
    fun `generated drill has the method as its easiest step`(method: TrainerMethod) {
        val drill = drillFor(method)
        val marks = if (method.usesNotes) marksOf(drill) else null
        val hint = HintEngine.find(drill.grid.toIntArray(), marks)
        assertEquals(method.techniqueName, hint?.techniqueName)
        if (method.usesNotes) {
            assertTrue(drill.instances.isNotEmpty())
            assertTrue(drill.marks.any { it.isNotEmpty() })
        } else {
            assertTrue(drill.placements.isNotEmpty())
            assertTrue(drill.marks.all { it.isEmpty() })
        }
    }

    @ParameterizedTest
    @EnumSource(TrainerMethod::class)
    fun `applying the method to the end completes the drill`(method: TrainerMethod) {
        val drill = drillFor(method)
        val start = drill.toGameState()
        if (method.usesNotes) {
            assertTrue(drill.allEliminations.none { drill.puzzle.solution[it.cell] == it.digit }, "eliminates a solution digit")
            assertTrue(drill.applications.isNotEmpty())
            // Follow the hints, one application at a time.
            var s = start
            for ((n, app) in drill.applications.withIndex()) {
                val p = TrainerJudge.progress(drill, s)
                assertEquals(n to drill.applications.size, p.applications)
                assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, s))
                s = removeNotes(s, app)
                assertTrue(TrainerJudge.judge(drill, s) != TrainerJudge.Verdict.Wrong, "application $n judged wrong")
            }
            assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, s))
            val done = TrainerJudge.progress(drill, s)
            assertEquals(drill.allEliminations.size to drill.allEliminations.size, done.notes)
        } else {
            for (p in drill.placements) assertEquals(drill.puzzle.solution[p.cell], p.digit)
        }
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, start))
    }

    @ParameterizedTest
    @EnumSource(value = TrainerMethod::class, names = ["NakedSingle", "HiddenSingle"])
    fun `a placement drill runs until the method finds nothing more`(method: TrainerMethod) {
        val drill = drillWithSeveralSingles(method)
        assertTrue(drill.targets.containsAll(drill.placements))
        val total = drill.targets.size
        var s = drill.toGameState()
        // Play it out the way a player following hints would.
        for (n in 0 until total) {
            assertEquals(total - n, TrainerJudge.remaining(drill, s), "total must not change as singles open up")
            assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, s))
            val h = TrainerJudge.hintFor(drill, s)!!
            val p = NoteRef(target(h), drill.puzzle.solution[target(h)])
            assertTrue(p in drill.targets, "hint left the targets")
            s = place(s, p)
            assertTrue(TrainerJudge.judge(drill, s) != TrainerJudge.Verdict.Wrong)
        }
        assertEquals(0, TrainerJudge.remaining(drill, s))
        assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, s))
        // Nothing of this method is left on the board.
        val grid = IntArray(81) { s.cells[it].value ?: 0 }
        assertTrue(TrainerGenerator.singlesOf(method, grid).none { grid[it.cell] == 0 })
    }

    @ParameterizedTest
    @EnumSource(value = TrainerMethod::class, names = ["NakedSingle", "HiddenSingle"])
    fun `the total counts singles that open up later`(method: TrainerMethod) {
        val random = Random(method.ordinal + 2000)
        repeat(200) {
            val drill = TrainerGenerator.generate(method, { level -> pool.getValue(level).random(random) }, random)
                ?: return@repeat
            val later = drill.targets - drill.placements
            if (later.isEmpty()) return@repeat
            // Placing a later target before it opens up is not this method yet.
            val early = place(drill.toGameState(), later.first())
            assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, early))
            return
        }
        error("no $method drill had singles that open up later")
    }

    @ParameterizedTest
    @EnumSource(value = TrainerMethod::class, names = ["NakedSingle", "HiddenSingle"])
    fun `hint points at an unplaced target`(method: TrainerMethod) {
        val drill = drillWithSeveralSingles(method)
        var s = drill.toGameState()
        repeat(3) {
            val h = TrainerJudge.hintFor(drill, s) ?: return
            assertEquals(null, s.cells[target(h)].value)
            assertTrue(drill.targets.any { it.cell == target(h) })
            s = place(s, NoteRef(target(h), drill.puzzle.solution[target(h)]))
        }
    }

    @Test
    fun `a naked single opened up by the player's own placements is accepted`() {
        // Find a drill where placing the starting singles creates a new one.
        val random = Random(99)
        repeat(200) {
            val drill = TrainerGenerator.generate(
                TrainerMethod.NakedSingle, { level -> pool.getValue(level).random(random) }, random,
            ) ?: return@repeat
            var s = drill.toGameState()
            for (p in drill.placements) s = place(s, p)
            val grid = IntArray(81) { s.cells[it].value ?: 0 }
            val fresh = HintEngine.allNakedSingles(grid).firstOrNull() ?: return@repeat
            val p = NoteRef(fresh.targetCell, fresh.solutionDigit)
            assertTrue(p !in drill.placements)
            assertTrue(p in drill.targets)
            assertTrue(TrainerJudge.judge(drill, place(s, p)) != TrainerJudge.Verdict.Wrong)
            // A correct value that is no naked single is still wrong.
            val other = (0 until 81).first { i ->
                grid[i] == 0 && i != p.cell && HintEngine.allNakedSingles(grid).none { it.targetCell == i }
            }
            val wrong = place(place(s, p), NoteRef(other, drill.puzzle.solution[other]))
            assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, wrong))
            return
        }
        error("no drill opened up a new naked single")
    }

    @ParameterizedTest
    @EnumSource(TrainerMethod::class)
    fun `a move the method does not make is wrong`(method: TrainerMethod) {
        val drill = drillFor(method)
        val start = drill.toGameState()
        // A correct value in a cell the method doesn't solve.
        val other = (0 until 81).firstOrNull { i ->
            drill.grid[i] == 0 && drill.placements.none { it.cell == i }
        }
        if (other != null) {
            val s = place(start, NoteRef(other, drill.puzzle.solution[other]))
            assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, s))
        }
        if (method.usesNotes) {
            // Removing a note no instance eliminates (the cell's answer).
            val cell = (0 until 81).first { drill.puzzle.solution[it] in drill.marks[it] }
            val s = removeNotes(start, setOf(NoteRef(cell, drill.puzzle.solution[cell])))
            assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, s))
        }
    }

    @Test
    fun `naked single drills have no hidden single available`() {
        val drill = drillFor(TrainerMethod.NakedSingle)
        assertTrue(HintEngine.allHiddenSingles(drill.grid.toIntArray()).isEmpty())
    }

    @ParameterizedTest
    @EnumSource(value = TrainerMethod::class, names = ["HiddenSingle", "NakedSingle"])
    fun `placement drills show candidates only when asked`(method: TrainerMethod) {
        val drill = drillFor(method)
        assertTrue(drill.toGameState().cells.all { it.pencilMarks.isEmpty() })
        val filled = drill.toGameState(fillCandidates = true)
        val grid = drill.grid.toIntArray()
        for (c in filled.cells) {
            val i = c.row * 9 + c.col
            val expected = if (grid[i] != 0) emptySet()
            else (1..9).filter { SudokuSolver.isValidPlacement(grid, c.row, c.col, it) }.toSet()
            assertEquals(expected, c.pencilMarks.keys, "cell $i")
        }
        // Notes don't affect judging a placement drill.
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, filled))
    }

    @ParameterizedTest
    @EnumSource(value = TrainerMethod::class, names = ["HiddenSingle", "NakedSingle"], mode = EnumSource.Mode.EXCLUDE)
    fun `note drills always show their notes, whatever the autofill setting`(method: TrainerMethod) {
        val drill = drillFor(method)
        for (fill in listOf(false, true)) {
            val cells = drill.toGameState(fillCandidates = fill).cells
            for (i in 0 until 81) assertEquals(drill.marks[i], cells[i].pencilMarks.keys, "cell $i, fill=$fill")
        }
    }

    @Test
    fun `a later application's note removed too early is wrong`() {
        val random = Random(4242)
        for (method in TrainerMethod.entries.filter { it.usesNotes }) {
            repeat(40) {
                val drill = TrainerGenerator.generate(method, { level -> pool.getValue(level).random(random) }, random)
                    ?: return@repeat
                val start = drill.instanceEliminations.flatten().toSet()
                val late = drill.allEliminations.firstOrNull { it !in start } ?: return@repeat
                val s = removeNotes(drill.toGameState(), setOf(late))
                assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, s))
                return
            }
        }
        error("no drill had a note that only a later application removes")
    }

    @Test
    fun `hidden single is listed before naked single`() {
        assertEquals(TrainerMethod.HiddenSingle, TrainerMethod.entries[0])
        assertEquals(TrainerMethod.NakedSingle, TrainerMethod.entries[1])
    }

    // ---- Several instances of one method on the same board ----

    /**
     * An empty board where digit 1 has exactly two candidates in rows 0
     * and 1 (columns 0 and 4) and in rows 6 and 8 (columns 2 and 7):
     * two separate X-Wings. Every other note is present everywhere.
     */
    private fun twoXWingDrill(): TrainerDrill {
        val grid = IntArray(81)
        val marks = Array(81) { i ->
            val r = i / 9; val c = i % 9
            val ones = when (r) {
                0, 1 -> c == 0 || c == 4
                6, 8 -> c == 2 || c == 7
                else -> true
            }
            if (ones) (1..9).toSet() else (2..9).toSet()
        }
        val instances = HintEngine.allEliminations("X-Wing", grid, marks)
        return TrainerDrill(
            applications = TrainerGenerator.eliminationClosure(TrainerMethod.XWing, grid, marks.toList()),
            method = TrainerMethod.XWing,
            puzzle = Puzzle("two-x-wings", Difficulty.Expert, List(81) { 0 }, SOLVED),
            grid = grid.toList(),
            marks = marks.toList(),
            placements = emptySet(),
            instances = instances,
        )
    }

    @Test
    fun `allEliminations finds both X-Wings`() {
        val drill = twoXWingDrill()
        assertEquals(2, drill.instances.size)
        assertEquals(
            setOf(setOf(0, 4, 9, 13), setOf(56, 61, 74, 79)),
            drill.instances.map { it.keyCells.toSet() }.toSet(),
        )
        // The first instance is the one an ordinary hint would show.
        val cands = HintEngine.candidateGrid(drill.grid.toIntArray(), drill.marks.toTypedArray())
        assertEquals(HintEngine.findXWing(drill.grid.toIntArray(), cands), drill.instances.first())
    }

    @Test
    fun `both X-Wings are needed, in either order`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        assertEquals(2, drill.applications.size)
        val (a, b) = drill.instanceEliminations
        for (first in listOf(a, b)) {
            val s = removeNotes(start, first)
            assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, s))
            assertEquals(1 to 2, TrainerJudge.progress(drill, s).applications)
        }
        assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, removeNotes(start, a + b)))
    }

    @Test
    fun `progress counts notes and applications separately`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (a, b) = drill.instanceEliminations
        val total = (a + b).size
        assertEquals(TrainerJudge.Progress(0 to total, 0 to 2), TrainerJudge.progress(drill, start))
        // Some of each, but neither finished: notes count, applications don't.
        val mixed = a.drop(1).toSet() + b.drop(1).toSet()
        val s = removeNotes(start, mixed)
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, s))
        assertEquals(TrainerJudge.Progress(mixed.size to total, 0 to 2), TrainerJudge.progress(drill, s))
        // Finish one: that application counts.
        val s2 = removeNotes(start, a + b.take(1).toSet())
        assertEquals(TrainerJudge.Progress((a.size + 1) to total, 1 to 2), TrainerJudge.progress(drill, s2))
    }

    @Test
    fun `removing a note neither X-Wing eliminates is wrong`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (a, _) = drill.instanceEliminations
        val stray = NoteRef(cell = 40, digit = 5)
        assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, removeNotes(start, setOf(stray))))
        // Even when the rest of a full X-Wing is done too.
        assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, removeNotes(start, a + stray)))
    }

    @Test
    fun `adding a note is wrong in a note drill`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val cell = start.cells[2]
        val s = start.copy(cells = start.cells.toMutableList().also {
            it[2] = cell.copy(pencilMarks = cell.pencilMarks + (1 to GuessColor.DEFAULT))
        })
        assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, s))
    }

    @Test
    fun `undoing a wrong move returns the drill to in progress`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState().copy(activeDigit = 5, pencilOn = true)
        val wrong = GameReducer.tapCell(start, row = 4, col = 4)
        assertEquals(TrainerJudge.Verdict.Wrong, TrainerJudge.judge(drill, wrong))
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, GameReducer.undo(wrong)))
    }

    @Test
    fun `hint moves on to the X-Wing still to finish`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (a, _) = drill.instanceEliminations
        assertEquals(drill.instances[0], TrainerJudge.hintFor(drill, start))
        val s = removeNotes(start, a)
        assertEquals(drill.instances[1], TrainerJudge.hintFor(drill, s))
    }

    // ---- Helpers ----

    private fun removeNotes(state: GameState, notes: Set<NoteRef>): GameState {
        val cells = state.cells.toMutableList()
        for (n in notes) cells[n.cell] = cells[n.cell].let { it.copy(pencilMarks = it.pencilMarks - n.digit) }
        return state.copy(cells = cells)
    }

    private fun place(state: GameState, p: NoteRef): GameState {
        val cells = state.cells.toMutableList()
        cells[p.cell] = cells[p.cell].copy(value = p.digit, valueColor = CellColor.Guess(GuessColor.Blue))
        return state.copy(cells = cells)
    }

    private companion object {
        val SOLVED = listOf(
            5, 3, 4, 6, 7, 8, 9, 1, 2,
            6, 7, 2, 1, 9, 5, 3, 4, 8,
            1, 9, 8, 3, 4, 2, 5, 6, 7,
            8, 5, 9, 7, 6, 1, 4, 2, 3,
            4, 2, 6, 8, 5, 3, 7, 9, 1,
            7, 1, 3, 9, 2, 4, 8, 5, 6,
            9, 6, 1, 5, 3, 7, 2, 8, 4,
            2, 8, 7, 4, 1, 9, 6, 3, 5,
            3, 4, 5, 2, 8, 6, 1, 7, 9,
        )
    }
}
