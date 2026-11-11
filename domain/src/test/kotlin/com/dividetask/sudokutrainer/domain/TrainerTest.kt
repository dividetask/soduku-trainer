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
    fun `applying any one instance completes the drill`(method: TrainerMethod) {
        val drill = drillFor(method)
        val start = drill.toGameState()
        if (method.usesNotes) {
            for (elims in drill.instanceEliminations) {
                assertTrue(elims.none { drill.puzzle.solution[it.cell] == it.digit }, "eliminates a solution digit")
                assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, removeNotes(start, elims)))
            }
        } else {
            for (p in drill.placements) {
                assertEquals(drill.puzzle.solution[p.cell], p.digit)
                assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, place(start, p)))
            }
        }
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, start))
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
    fun `hidden single drills have no naked single available`() {
        val drill = drillFor(TrainerMethod.HiddenSingle)
        assertTrue(HintEngine.allNakedSingles(drill.grid.toIntArray()).isEmpty())
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
    fun `either X-Wing completes the drill`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (a, b) = drill.instanceEliminations
        assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, removeNotes(start, a)))
        assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, removeNotes(start, b)))
    }

    @Test
    fun `a partly applied X-Wing is still in progress`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (a, b) = drill.instanceEliminations
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, removeNotes(start, a.drop(1).toSet())))
        // Some of each, but neither finished.
        val mixed = a.drop(1).toSet() + b.drop(1).toSet()
        assertEquals(TrainerJudge.Verdict.InProgress, TrainerJudge.judge(drill, removeNotes(start, mixed)))
    }

    @Test
    fun `finishing one X-Wing after starting the other completes the drill`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (a, b) = drill.instanceEliminations
        val s = removeNotes(start, a.take(2).toSet() + b)
        assertEquals(TrainerJudge.Verdict.Complete, TrainerJudge.judge(drill, s))
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
    fun `hint picks the X-Wing closest to done`() {
        val drill = twoXWingDrill()
        val start = drill.toGameState()
        val (_, b) = drill.instanceEliminations
        assertEquals(drill.instances[0], TrainerJudge.hintFor(drill, start))
        val s = removeNotes(start, b.take(1).toSet())
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
