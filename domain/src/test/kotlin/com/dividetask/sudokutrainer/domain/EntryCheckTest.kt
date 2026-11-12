package com.dividetask.sudokutrainer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntryCheckTest {

    // Only row 0 has givens; cells (0,2) and (0,3) are empty. The answer
    // at (0,2) is 4, and row 0 holds a given 5.
    private val puzzle = Puzzle(
        id = "t",
        difficulty = Difficulty.Easy,
        givens = SOLVED.mapIndexed { i, v -> if (i < 9 && i != 2 && i != 3) v else 0 },
        solution = SOLVED,
    )
    private val start = GameState.forNewGame(puzzle)

    private fun tap(digit: Int, color: GuessColor = GuessColor.Blue, pencil: Boolean = false, from: GameState = start) =
        BoardMoves.tapCell(from.copy(activeDigit = digit, activeColor = color, pencilOn = pencil), 0, 2)

    private fun forbidden(before: GameState, after: GameState) =
        EntryCheck.isForbidden(before, after, preventValues = true, preventNotes = true)

    @Test
    fun `wrong blue value is forbidden, right one is not`() {
        assertTrue(forbidden(start, tap(6)))
        assertFalse(forbidden(start, tap(4)))
    }

    @Test
    fun `wrong guess-colored value is allowed`() {
        assertFalse(forbidden(start, tap(6, GuessColor.Red)))
    }

    @Test
    fun `value check can be switched off`() {
        assertFalse(EntryCheck.isForbidden(start, tap(6), preventValues = false, preventNotes = true))
    }

    @Test
    fun `removing the answer from blue notes is forbidden`() {
        val noted = tap(4, pencil = true)
        assertFalse(forbidden(start, noted))
        assertTrue(forbidden(noted, tap(4, pencil = true, from = noted)))
    }

    @Test
    fun `adding a blue note for a digit already in the row is forbidden`() {
        assertTrue(forbidden(start, tap(5, pencil = true)))
        // 6 isn't the answer but nothing placed rules it out yet.
        assertFalse(forbidden(start, tap(6, pencil = true)))
    }

    @Test
    fun `note check can be switched off, and guess-colored notes are free`() {
        assertFalse(EntryCheck.isForbidden(start, tap(5, pencil = true), preventValues = true, preventNotes = false))
        assertFalse(forbidden(start, tap(5, GuessColor.Green, pencil = true)))
    }

    @Test
    fun `mistakes are wrong values and notes missing the answer`() {
        assertEquals(emptySet<Int>(), start.mistakeCells())
        // A wrong value counts in any color.
        assertEquals(setOf(2), tap(6, GuessColor.Red).mistakeCells())
        // Notes that still include the answer (4) are fine...
        val withAnswer = tap(4, pencil = true).let { tap(6, pencil = true, from = it) }
        assertEquals(emptySet<Int>(), withAnswer.mistakeCells())
        // ...but notes that have lost it are a mistake.
        assertEquals(setOf(2), tap(4, pencil = true, from = withAnswer).mistakeCells())
        // The correct value isn't.
        assertEquals(emptySet<Int>(), tap(4).mistakeCells())
    }

    @Test
    fun `saved game round-trips`() {
        val noted = tap(4, pencil = true)
        val s = BoardMoves.tapCell(noted.copy(activeDigit = 6, activeColor = GuessColor.Red, pencilOn = false), 0, 3)
        val back = SavedGame.decode(SavedGame.of(s).encode())
        assertNotNull(back)
        val restored = back!!.toGameState()
        assertEquals(s.cells, restored.cells)
        assertEquals(s.history, restored.history)
        assertEquals(s.puzzle, restored.puzzle)
        assertEquals(GuessColor.Red, restored.activeColor)
        // Undo history still works after restoring.
        assertEquals(BoardMoves.undo(s).cells, BoardMoves.undo(restored).cells)
    }

    @Test
    fun `unreadable save decodes to null`() {
        assertEquals(null, SavedGame.decode("{not json"))
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
