package com.dividetask.sudokutrainer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        var wrongTarget = -1
        for (i in 0 until 81) {
            if (givens[i] == 0) { wrongTarget = i; break }
        }
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
