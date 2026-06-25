package com.dividetask.sudokutrainer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests cover the mechanics documented in docs/game-mechanics.md:
 *
 * - Digit selection toggles on/off, only one active at a time.
 * - Normal-mode tap writes into empty cell, ignored on filled cell.
 * - Double-tap clears filled non-given, no-op elsewhere.
 * - Pencil-mode tap toggles mark on empty cell, ignored on filled cell.
 * - Side effect: writing a value hides pencil marks (stores them in the
 *   move so undo restores them).
 * - Undo reverses each of the three move kinds and is a no-op on empty
 *   history.
 * - Exhausted digit count and the mostly-solved threshold + no-incorrect
 *   rule.
 * - Solve action phase transition.
 */
class GameReducerTest {

    private val solution = intArrayOf(
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

    private val givens = intArrayOf(
        5, 3, 0, 0, 7, 0, 0, 0, 0,
        6, 0, 0, 1, 9, 5, 0, 0, 0,
        0, 9, 8, 0, 0, 0, 0, 6, 0,
        8, 0, 0, 0, 6, 0, 0, 0, 3,
        4, 0, 0, 8, 0, 3, 0, 0, 1,
        7, 0, 0, 0, 2, 0, 0, 0, 6,
        0, 6, 0, 0, 0, 0, 2, 8, 0,
        0, 0, 0, 4, 1, 9, 0, 0, 5,
        0, 0, 0, 0, 8, 0, 0, 7, 9,
    )

    private val puzzle = Puzzle(
        id = "test-1",
        difficulty = Difficulty.Easy,
        givens = givens.toList(),
        solution = solution.toList(),
    )

    private fun initial(): GameState = GameState.forNewGame(puzzle)

    @Test
    fun `selectDigit sets active and re-selecting deactivates`() {
        var s = initial()
        s = GameReducer.selectDigit(s, 5)
        assertEquals(5, s.activeDigit)
        s = GameReducer.selectDigit(s, 5)
        assertNull(s.activeDigit)
        s = GameReducer.selectDigit(s, 3)
        s = GameReducer.selectDigit(s, 7)
        assertEquals(7, s.activeDigit)
    }

    @Test
    fun `normal-mode tap on empty cell writes value in active color`() {
        var s = initial().let { GameReducer.selectDigit(it, 4) }
        s = GameReducer.selectColor(s, GuessColor.Green)
        // (0,2) is empty (given is 0). Solution there is 4, but reducer
        // must not care about correctness.
        s = GameReducer.tapCell(s, 0, 2)
        val c = s.cellAt(0, 2)
        assertEquals(4, c.value)
        assertEquals(CellColor.Guess(GuessColor.Green), c.valueColor)
        assertEquals(1, s.history.size)
        assertTrue(s.history.last() is Move.SetValue)
    }

    @Test
    fun `normal-mode tap on filled cell is a no-op (no overwrite)`() {
        var s = initial().let { GameReducer.selectDigit(it, 4) }
        s = GameReducer.tapCell(s, 0, 2)
        val afterFirst = s
        // Now try to overwrite with a different digit.
        s = GameReducer.selectDigit(s, 9)
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(afterFirst.cellAt(0, 2), s.cellAt(0, 2))
        assertEquals(afterFirst.history.size, s.history.size)
    }

    @Test
    fun `normal-mode tap on given is a no-op`() {
        var s = initial().let { GameReducer.selectDigit(it, 9) }
        // (0,0) is a given (5).
        s = GameReducer.tapCell(s, 0, 0)
        assertEquals(5, s.cellAt(0, 0).value)
        assertTrue(s.history.isEmpty())
    }

    @Test
    fun `writing a value hides (clears) that cell's pencil marks and undo restores them`() {
        var s = initial()
        // Place a few pencil marks on (0,2) in pencil mode.
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 1)
        s = GameReducer.tapCell(s, 0, 2)
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 2)
        s = GameReducer.selectDigit(s, 7)
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(setOf(1, 4, 7), s.cellAt(0, 2).pencilMarks.keys)
        // Now flip pencil off, select 9, tap — the value is written and the
        // marks are hidden (stored in the move for undo).
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 9)
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(9, s.cellAt(0, 2).value)
        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())
        // Undo: value gone, marks restored.
        s = GameReducer.undo(s)
        assertNull(s.cellAt(0, 2).value)
        assertEquals(setOf(1, 4, 7), s.cellAt(0, 2).pencilMarks.keys)
    }

    @Test
    fun `pencil-mode tap on empty cell toggles the mark`() {
        var s = initial()
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 3)
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(setOf(3), s.cellAt(0, 2).pencilMarks.keys)
        // Tapping again removes it.
        s = GameReducer.tapCell(s, 0, 2)
        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())
    }

    @Test
    fun `pencil-mode tap on filled cell is a no-op`() {
        var s = initial().let { GameReducer.selectDigit(it, 4) }
        s = GameReducer.tapCell(s, 0, 2) // write 4
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 7)
        val before = s
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(before.cellAt(0, 2), s.cellAt(0, 2))
        assertEquals(before.history.size, s.history.size)
    }

    @Test
    fun `double-tap on filled non-given clears it and pencil marks stay as-is`() {
        var s = initial()
        // Seed pencil marks, then write a value (which clears the visual
        // marks but stores them in the move), then double-tap to clear
        // the value. We need the marks to still be absent in the cell
        // (they were cleared by the SetValue move) — so double-tap leaves
        // the cell empty with no marks.
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 2)
        s = GameReducer.tapCell(s, 0, 2)
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(4, s.cellAt(0, 2).value)
        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())

        s = GameReducer.doubleTapCell(s, 0, 2)
        assertNull(s.cellAt(0, 2).value)
        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())
        assertTrue(s.history.last() is Move.ClearValue)
    }

    @Test
    fun `double-tap on empty cell and on given is a no-op`() {
        var s = initial()
        val before = s
        s = GameReducer.doubleTapCell(s, 0, 2) // empty
        assertSame(before, s)
        s = GameReducer.doubleTapCell(s, 0, 0) // given
        assertSame(before, s)
    }

    @Test
    fun `undo on empty history is a no-op`() {
        val s = initial()
        val s2 = GameReducer.undo(s)
        assertSame(s, s2)
    }

    @Test
    fun `digit is exhausted when nine cells hold it`() {
        var s = initial()
        // Place 5 in every non-given cell of row 0..8 where solution == 5
        // using the reducer. For simplicity, just force-fill all 5s via
        // taps at the cells where solution is 5. Givens already contain
        // one 5 at (0,0). Non-given solution==5 cells:
        val targets = buildList {
            for (i in 0 until 81) {
                if (solution[i] == 5 && givens[i] == 0) add(i / 9 to i % 9)
            }
        }
        // Activate digit 5 and tap each target.
        s = GameReducer.selectDigit(s, 5)
        for ((r, c) in targets) s = GameReducer.tapCell(s, r, c)
        assertEquals(9, s.placedCountOf(5))
        assertTrue(s.isDigitExhausted(5))
        assertFalse(s.isDigitExhausted(4))
    }

    @Test
    fun `mostly solved requires threshold AND no incorrect entries`() {
        var s = initial()
        // Fill all non-given cells correctly using the solution as the active digit.
        // selectDigit toggles off when invoked with the currently-active digit,
        // so we only call it when a change is needed.
        for (i in 0 until 81) {
            if (givens[i] != 0) continue
            val r = i / 9
            val c = i % 9
            val d = solution[i]
            if (s.activeDigit != d) s = GameReducer.selectDigit(s, d)
            s = GameReducer.tapCell(s, r, c)
        }
        assertEquals(81, s.correctCount)
        assertTrue(s.isMostlySolved(10))
        // Introduce one incorrect entry: pick a non-given cell, clear it,
        // and place a wrong value.
        val wrongTarget = (0 until 81).first { givens[it] == 0 }
        val wr = wrongTarget / 9
        val wc = wrongTarget % 9
        s = GameReducer.doubleTapCell(s, wr, wc)
        val correct = solution[wrongTarget]
        val wrong = if (correct == 9) 1 else correct + 1
        s = GameReducer.selectDigit(s, wrong)
        s = GameReducer.tapCell(s, wr, wc)
        assertTrue(s.hasIncorrectEntry)
        assertFalse(s.isMostlySolved(10), "any incorrect entry must block mostly-solved")
    }

    @Test
    fun `applySolve fills empty cells in Solve color and transitions phase`() {
        var s = initial()
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 2) // write a player entry (keep this as Guess)
        s = GameReducer.applySolve(s)
        assertEquals(GameState.Phase.Celebrating, s.phase)
        assertTrue(s.history.isEmpty())
        // The player entry we placed must keep its Guess color.
        assertEquals(CellColor.Guess(GuessColor.Blue), s.cellAt(0, 2).valueColor)
        // A previously-empty cell must now be filled with CellColor.Solve.
        val filled = s.cells.first { !it.given && it.row == 0 && it.col == 3 }
        assertEquals(solution[3], filled.value)
        assertEquals(CellColor.Solve, filled.valueColor)
    }

    @Test
    fun `toggling pencil and selecting color are not undoable`() {
        var s = initial()
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectColor(s, GuessColor.Purple)
        s = GameReducer.selectDigit(s, 3)
        assertTrue(s.history.isEmpty())
    }

    @Test
    fun `undo restores pencil mark toggles in sequence`() {
        var s = initial()
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 2)
        s = GameReducer.tapCell(s, 0, 2) // add 2
        s = GameReducer.selectDigit(s, 5)
        s = GameReducer.tapCell(s, 0, 2) // add 5
        s = GameReducer.tapCell(s, 0, 2) // remove 5
        assertEquals(setOf(2), s.cellAt(0, 2).pencilMarks.keys)
        s = GameReducer.undo(s) // re-add 5
        assertEquals(setOf(2, 5), s.cellAt(0, 2).pencilMarks.keys)
        s = GameReducer.undo(s) // remove 5 (inverse of add)
        assertEquals(setOf(2), s.cellAt(0, 2).pencilMarks.keys)
        s = GameReducer.undo(s) // remove 2
        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())
        // Empty history now; further undo is a no-op.
        val before = s
        s = GameReducer.undo(s)
        assertSame(before, s)
    }

    @Test
    fun `Blue (Solve) placement auto-clears Blue pencil marks from peers`() {
        var s = initial()
        // Place Blue pencil marks for digit 4 in several cells in row 0.
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 2) // add Blue 4 mark
        s = GameReducer.tapCell(s, 0, 3) // add Blue 4 mark
        s = GameReducer.tapCell(s, 0, 5) // add Blue 4 mark
        // Replace the Blue 4 mark at (0,3) with a Red one.
        // First toggle off Blue 4, then switch color and add Red 4.
        s = GameReducer.tapCell(s, 0, 3) // removes Blue 4
        s = GameReducer.selectColor(s, GuessColor.Red)
        s = GameReducer.tapCell(s, 0, 3) // adds Red 4

        assertEquals(GuessColor.Blue, s.cellAt(0, 2).pencilMarks[4])
        assertEquals(GuessColor.Red, s.cellAt(0, 3).pencilMarks[4])
        assertEquals(GuessColor.Blue, s.cellAt(0, 5).pencilMarks[4])

        // Now place digit 4 in Blue (Solve) mode at (0,2).
        // Digit 4 is already active from the pencil-mark phase above.
        s = GameReducer.togglePencil(s) // pencil off
        s = GameReducer.selectColor(s, GuessColor.Blue)
        s = GameReducer.tapCell(s, 0, 2)
        assertEquals(4, s.cellAt(0, 2).value)

        // Blue 4 mark at (0,5) should be auto-cleared (same row).
        assertNull(s.cellAt(0, 5).pencilMarks[4])
        // Red 4 mark at (0,3) should survive (not Blue).
        assertEquals(GuessColor.Red, s.cellAt(0, 3).pencilMarks[4])
    }

    @Test
    fun `undo of Blue placement restores auto-cleared marks`() {
        var s = initial()
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 5) // add Blue 4 mark at (0,5)
        s = GameReducer.togglePencil(s)
        s = GameReducer.tapCell(s, 0, 2) // place Blue 4 at (0,2) — auto-clears (0,5)
        assertNull(s.cellAt(0, 5).pencilMarks[4])
        // Undo should restore the Blue 4 mark at (0,5).
        s = GameReducer.undo(s)
        assertNull(s.cellAt(0, 2).value)
        assertEquals(GuessColor.Blue, s.cellAt(0, 5).pencilMarks[4])
    }

    @Test
    fun `non-Blue placement does not auto-clear`() {
        var s = initial()
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 5) // add Blue 4 mark
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectColor(s, GuessColor.Red)
        s = GameReducer.tapCell(s, 0, 2) // place Red 4 at (0,2)
        // Blue 4 mark at (0,5) should still be there.
        assertEquals(GuessColor.Blue, s.cellAt(0, 5).pencilMarks[4])
    }

    @Test
    fun `naked single hint flows from begin to complete`() {
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle
        val tRow = hint.targetCell / 9
        val tCol = hint.targetCell % 9
        // The found cell must match the puzzle's solution at that index.
        assertEquals(puzzle.solutionAt(tRow, tCol), hint.solutionDigit)

        var s = GameReducer.beginNakedSingleHint(s0, hint)
        assertEquals(GameState.Phase.Hinting, s.phase)
        val notes = s.cellAt(tRow, tCol).pencilMarks
        assertEquals((1..9).toSet(), notes.keys)
        assertTrue(s.hintUi is HintUi.NakedSingle)

        // Sweep: eliminate every non-solution digit.
        for (d in 1..9) {
            s = GameReducer.advanceNakedSingleStep(
                state = s,
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            )
        }
        // Only the solution digit remains as a pencil mark.
        assertEquals(setOf(hint.solutionDigit), s.cellAt(tRow, tCol).pencilMarks.keys)

        s = GameReducer.completeNakedSingleHint(s, hint)
        assertEquals(GameState.Phase.Playing, s.phase)
        assertNull(s.hintUi)
        assertEquals(hint.solutionDigit, s.cellAt(tRow, tCol).value)
        assertEquals(CellColor.Guess(GuessColor.Blue), s.cellAt(tRow, tCol).valueColor)
        assertTrue(s.cellAt(tRow, tCol).pencilMarks.isEmpty())
        assertEquals(1, s.hintCount)
    }

    @Test
    fun `hidden single hint flows from begin to complete`() {
        // Construct a grid with a clean hidden single: (0,0) is the only
        // cell in box 0 that can hold 5, because 5 appears on cols 1 & 2
        // and rows 1 & 2 outside the box.
        val grid = IntArray(81)
        grid[3 * 9 + 1] = 5
        grid[4 * 9 + 2] = 5
        grid[1 * 9 + 5] = 5
        grid[2 * 9 + 7] = 5
        val hint = HintEngine.find(grid) as HintEngine.HiddenSingle
        assertEquals(0, hint.targetCell)
        assertEquals(5, hint.solutionDigit)

        // Build a state with this grid as the givens (we only care about
        // value placement, not difficulty/solution alignment).
        val fakeSolution = IntArray(81) { i -> if (grid[i] != 0) grid[i] else 1 }
        fakeSolution[0] = 5
        val fakePuzzle = Puzzle(
            id = "hs-test",
            difficulty = Difficulty.Easy,
            givens = grid.toList(),
            solution = fakeSolution.toList(),
        )
        var s = GameState.forNewGame(fakePuzzle)

        s = GameReducer.beginHiddenSingleHint(s, hint)
        assertEquals(GameState.Phase.Hinting, s.phase)
        assertTrue(s.hintUi is HintUi.HiddenSingle)
        // Every empty cell in the unit now carries the solution digit as a mark.
        for (idx in hint.unit.cells) {
            val cell = s.cells[idx]
            if (cell.value != null) continue
            assertEquals(GuessColor.DEFAULT, cell.pencilMarks[5])
        }

        // Sweep each non-target empty cell.
        for (idx in hint.unit.cells) {
            if (idx == hint.targetCell) continue
            if (s.cells[idx].value != null) continue
            s = GameReducer.advanceHiddenSingleStep(
                state = s,
                currentCell = idx,
                eliminatorCells = hint.eliminators[idx].orEmpty(),
            )
            assertNull(s.cells[idx].pencilMarks[5])
        }
        // Settle frame: target still has the mark; everyone else doesn't.
        s = GameReducer.advanceHiddenSingleStep(s, currentCell = null, eliminatorCells = emptySet())
        assertEquals(GuessColor.DEFAULT, s.cells[hint.targetCell].pencilMarks[5])

        s = GameReducer.completeHiddenSingleHint(s, hint)
        assertEquals(GameState.Phase.Playing, s.phase)
        assertNull(s.hintUi)
        assertEquals(5, s.cells[hint.targetCell].value)
        assertEquals(CellColor.Guess(GuessColor.Blue), s.cells[hint.targetCell].valueColor)
        assertEquals(1, s.hintCount)
    }

    @Test
    fun `undo reverses a naked single hint and restores prior pencil marks`() {
        var s = initial()
        // Seed pencil marks in the cell where the puzzle has a naked single.
        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle
        val tRow = hint.targetCell / 9
        val tCol = hint.targetCell % 9
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectColor(s, GuessColor.Red)
        s = GameReducer.selectDigit(s, 2)
        s = GameReducer.tapCell(s, tRow, tCol)
        s = GameReducer.selectDigit(s, 7)
        s = GameReducer.tapCell(s, tRow, tCol)
        s = GameReducer.togglePencil(s)
        val historySizeBeforeHint = s.history.size
        assertEquals(setOf(2, 7), s.cellAt(tRow, tCol).pencilMarks.keys)

        // Run the hint to completion.
        s = GameReducer.beginNakedSingleHint(s, hint)
        for (d in 1..9) {
            s = GameReducer.advanceNakedSingleStep(
                state = s,
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            )
        }
        s = GameReducer.completeNakedSingleHint(s, hint)
        assertEquals(hint.solutionDigit, s.cellAt(tRow, tCol).value)
        assertEquals(historySizeBeforeHint + 1, s.history.size)
        assertTrue(s.history.last() is Move.SetValue)

        // Undo: value gone, prior pencil marks restored.
        s = GameReducer.undo(s)
        assertNull(s.cellAt(tRow, tCol).value)
        assertEquals(GuessColor.Red, s.cellAt(tRow, tCol).pencilMarks[2])
        assertEquals(GuessColor.Red, s.cellAt(tRow, tCol).pencilMarks[7])
        assertEquals(historySizeBeforeHint, s.history.size)
    }

    @Test
    fun `undo reverses a hidden single hint and restores sweep-erased peer marks`() {
        // Same hidden-single setup as the begin-to-complete test.
        val grid = IntArray(81)
        grid[3 * 9 + 1] = 5
        grid[4 * 9 + 2] = 5
        grid[1 * 9 + 5] = 5
        grid[2 * 9 + 7] = 5
        val hint = HintEngine.find(grid) as HintEngine.HiddenSingle
        val fakeSolution = IntArray(81) { i -> if (grid[i] != 0) grid[i] else 1 }
        fakeSolution[0] = 5
        val fakePuzzle = Puzzle(
            id = "hs-undo-test",
            difficulty = Difficulty.Easy,
            givens = grid.toList(),
            solution = fakeSolution.toList(),
        )
        var s = GameState.forNewGame(fakePuzzle)

        // Player has placed a Purple 5 pencil mark in (0,1) — a non-target
        // cell in box 0 that the sweep will erase.
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectColor(s, GuessColor.Purple)
        s = GameReducer.selectDigit(s, 5)
        s = GameReducer.tapCell(s, 0, 1)
        s = GameReducer.togglePencil(s)
        assertEquals(GuessColor.Purple, s.cellAt(0, 1).pencilMarks[5])
        val historySizeBeforeHint = s.history.size

        // Run the hidden single hint to completion.
        s = GameReducer.beginHiddenSingleHint(s, hint)
        for (idx in hint.unit.cells) {
            if (idx == hint.targetCell) continue
            if (s.cells[idx].value != null) continue
            s = GameReducer.advanceHiddenSingleStep(
                state = s,
                currentCell = idx,
                eliminatorCells = hint.eliminators[idx].orEmpty(),
            )
        }
        s = GameReducer.advanceHiddenSingleStep(s, currentCell = null, eliminatorCells = emptySet())
        s = GameReducer.completeHiddenSingleHint(s, hint)
        assertEquals(5, s.cellAt(0, 0).value)
        assertNull(s.cellAt(0, 1).pencilMarks[5])
        assertEquals(historySizeBeforeHint + 1, s.history.size)

        // Undo: value gone, player's Purple 5 mark restored on (0,1).
        s = GameReducer.undo(s)
        assertNull(s.cellAt(0, 0).value)
        assertEquals(GuessColor.Purple, s.cellAt(0, 1).pencilMarks[5])
        assertEquals(historySizeBeforeHint, s.history.size)
    }

    @Test
    fun `Candidate Sweep with initial fill seeds notes, prunes, and undoes atomically`() {
        // Three givens scattered in box 0; no naked or hidden singles.
        // The hardcoded solution avoids SudokuSolver.solveOne, which is
        // exponential on such a sparse grid.
        val solution = intArrayOf(
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
        val grid = IntArray(81)
        grid[0] = solution[0]                 // 5 at (0,0)
        grid[1 * 9 + 1] = solution[1 * 9 + 1] // 7 at (1,1)
        grid[2 * 9 + 2] = solution[2 * 9 + 2] // 8 at (2,2)
        val fake = Puzzle(
            id = "cs-flow",
            difficulty = Difficulty.Beginner,
            givens = grid.toList(),
            solution = solution.toList(),
        )
        var s = GameState.forNewGame(fake)
        val hint = HintEngine.find(grid) as HintEngine.CandidateSweep
        assertTrue(hint.initialFill)

        s = GameReducer.beginCandidateSweep(s, hint)
        assertEquals(GameState.Phase.Hinting, s.phase)

        s = GameReducer.applyCandidateSweepFill(s)
        // Every empty cell now carries 1..9 in the default color.
        for (i in 0 until 81) {
            val c = s.cells[i]
            if (c.value != null) continue
            assertEquals((1..9).toSet(), c.pencilMarks.keys, "cell $i should have all candidates")
        }

        for (plan in hint.plan) {
            s = GameReducer.advanceCandidateSweepCell(
                state = s,
                currentCell = plan.cellIdx,
                doomedDigits = plan.doomedDigits,
                eliminatorPeers = plan.eliminatorPeers,
            )
        }
        // (0,1) lost 5, 7, 8.
        val cell01 = s.cellAt(0, 1)
        assertTrue(5 !in cell01.pencilMarks.keys)
        assertTrue(7 !in cell01.pencilMarks.keys)
        assertTrue(8 !in cell01.pencilMarks.keys)

        s = GameReducer.completeCandidateSweep(s, hint)
        assertEquals(GameState.Phase.Playing, s.phase)
        assertEquals(1, s.hintCount)
        assertTrue(s.history.last() is Move.PencilSweep)

        // Undo: every empty cell loses its candidates again (we started
        // with none) and no peer cell carries leftover notes.
        s = GameReducer.undo(s)
        for (i in 0 until 81) {
            assertTrue(s.cells[i].pencilMarks.isEmpty(), "cell $i should have no notes after undo")
        }
    }

    @Test
    fun `Candidate Sweep with countAsHint=false leaves hintCount unchanged`() {
        // Same grid as the begin-to-complete sweep test.
        val solution = intArrayOf(
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
        val grid = IntArray(81)
        grid[0] = solution[0]; grid[10] = solution[10]; grid[20] = solution[20]
        val fake = Puzzle(
            id = "cs-no-count",
            difficulty = Difficulty.Beginner,
            givens = grid.toList(),
            solution = solution.toList(),
        )
        var s = GameState.forNewGame(fake)
        val hint = HintEngine.findCandidateSweep(grid)
            ?: error("expected a sweep on this grid")

        s = GameReducer.beginCandidateSweep(s, hint)
        s = GameReducer.applyCandidateSweepFill(s)
        for (plan in hint.plan) {
            s = GameReducer.advanceCandidateSweepCell(s, plan.cellIdx, plan.doomedDigits, plan.eliminatorPeers)
        }
        s = GameReducer.completeCandidateSweep(s, hint, countAsHint = false)
        assertEquals(0, s.hintCount)
        // The undo move is still recorded so the player can revert it.
        assertTrue(s.history.last() is Move.PencilSweep)
    }

    @Test
    fun `pending hint advances from highlight to method, then clears`() {
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle

        var s = GameReducer.setPendingHint(s0, hint, setOf(hint.targetCell))
        assertEquals(setOf(hint.targetCell), s.pendingHint?.keyCells)
        assertEquals(false, s.pendingHint?.showCells)
        assertEquals("Naked Single", s.pendingHint?.techniqueName)

        s = GameReducer.advancePendingHintRevealCells(s)
        assertEquals(true, s.pendingHint?.showCells)

        // Second advance is a no-op once method is shown.
        val before = s
        s = GameReducer.advancePendingHintRevealCells(s)
        assertSame(before, s)

        s = GameReducer.clearPendingHint(s)
        assertNull(s.pendingHint)
    }

    @Test
    fun `placing a value in the pending-hint target clears the hint`() {
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle
        val tr = hint.targetCell / 9
        val tc = hint.targetCell % 9

        var s = GameReducer.setPendingHint(s0, hint, setOf(hint.targetCell))
        // Place any digit (correct or not) — the highlight should clear.
        val wrong = if (hint.solutionDigit == 1) 2 else 1
        s = GameReducer.selectDigit(s, wrong)
        s = GameReducer.tapCell(s, tr, tc)
        assertEquals(wrong, s.cellAt(tr, tc).value)
        assertNull(s.pendingHint)
    }

    @Test
    fun `any cell change clears the pending hint`() {
        // Whether the player taps a key cell or a different cell — and
        // whether it's a value placement, a pencil toggle, or a
        // double-tap clear — the pending hint must disappear.
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle
        val emptyIndices = (0 until 81).filter { !s0.cells[it].given && s0.cells[it].value == null }
        val keys = emptyIndices.take(3).toSet()
        check(keys.size == 3)
        val nonKey = emptyIndices.first { it !in keys }

        // 1. Value placement in any key cell.
        for (key in keys) {
            var s = GameReducer.setPendingHint(s0, hint, keys)
            s = GameReducer.selectDigit(s, 1)
            s = GameReducer.tapCell(s, key / 9, key % 9)
            assertNull(s.pendingHint, "value in key cell $key should clear")
        }
        // 2. Value placement in a non-key cell still clears.
        var s = GameReducer.setPendingHint(s0, hint, keys)
        s = GameReducer.selectDigit(s, 1)
        s = GameReducer.tapCell(s, nonKey / 9, nonKey % 9)
        assertNull(s.pendingHint, "value in a non-key cell must also clear")
        // 3. Pencil-mark toggle clears.
        s = GameReducer.setPendingHint(s0, hint, keys)
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 1)
        s = GameReducer.tapCell(s, nonKey / 9, nonKey % 9)
        assertNull(s.pendingHint, "pencil-mark toggle must clear the pending hint")
        // 4. Double-tap clear on a filled cell clears too.
        s = initial().let { GameReducer.selectDigit(it, 4) }
        s = GameReducer.tapCell(s, nonKey / 9, nonKey % 9) // fill it
        s = GameReducer.setPendingHint(s, hint, keys)
        s = GameReducer.doubleTapCell(s, nonKey / 9, nonKey % 9)
        assertNull(s.pendingHint, "double-tap clear must clear the pending hint")
    }

    @Test
    fun `lastHint is set on completion and survives an undo`() {
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle

        var s = GameReducer.beginNakedSingleHint(s0, hint)
        for (d in 1..9) {
            s = GameReducer.advanceNakedSingleStep(
                state = s,
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            )
        }
        s = GameReducer.completeNakedSingleHint(s, hint)
        assertEquals(hint, s.lastHint)

        // Undo pops the hint move but keeps lastHint set so Show Again works.
        s = GameReducer.undo(s)
        assertEquals(hint, s.lastHint)
    }

    @Test
    fun `lastHint clears on any player cell change`() {
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle

        var s = GameReducer.beginNakedSingleHint(s0, hint)
        for (d in 1..9) {
            s = GameReducer.advanceNakedSingleStep(
                state = s,
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            )
        }
        s = GameReducer.completeNakedSingleHint(s, hint)
        assertEquals(hint, s.lastHint)

        // Find any non-given empty cell and place a value there.
        val targetEmpty = (0 until 81).first {
            !s.cells[it].given && s.cells[it].value == null
        }
        s = GameReducer.selectDigit(s, 1)
        s = GameReducer.tapCell(s, targetEmpty / 9, targetEmpty % 9)
        assertNull(s.lastHint, "any cell change must clear lastHint")
    }

    @Test
    fun `completion with countAsHint false leaves hintCount unchanged but sets lastHint`() {
        val s0 = initial()
        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 }
        val hint = HintEngine.find(grid) as HintEngine.NakedSingle

        var s = GameReducer.beginNakedSingleHint(s0, hint)
        for (d in 1..9) {
            s = GameReducer.advanceNakedSingleStep(
                state = s,
                activeDigit = d,
                eliminatorCells = hint.eliminators[d].orEmpty(),
                eliminate = d != hint.solutionDigit,
            )
        }
        s = GameReducer.completeNakedSingleHint(s, hint, countAsHint = false)
        assertEquals(0, s.hintCount)
        assertEquals(hint, s.lastHint)
    }

    @Test
    fun `resetBoard returns to starting position`() {
        var s = initial()
        s = GameReducer.selectDigit(s, 4)
        s = GameReducer.tapCell(s, 0, 2)
        s = GameReducer.togglePencil(s)
        s = GameReducer.selectDigit(s, 7)
        s = GameReducer.tapCell(s, 0, 3)
        assertTrue(s.history.isNotEmpty())
        s = GameReducer.resetBoard(s)
        assertTrue(s.history.isEmpty())
        assertNull(s.cellAt(0, 2).value)
        assertNull(s.cellAt(0, 3).value)
        assertEquals(GameState.Phase.Playing, s.phase)
    }
}
