package com.dividetask.sudokutrainer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HintEngineTest {

    @Test
    fun `finds a naked single and reports its peer eliminators`() {
        // Row 0, col 0 has peers covering all of {1,2,3,4,5,6,7,8} via row,
        // column, and box — leaving only 9 as the candidate.
        val grid = IntArray(81)
        // Row 0: _ 1 2 3 _ _ _ _ _
        grid[1] = 1; grid[2] = 2; grid[3] = 3
        // Column 0: _ _ _ _ 4 5 _ _ _ (rows 4..5)
        grid[4 * 9 + 0] = 4; grid[5 * 9 + 0] = 5
        // Box 0 (rows 0..2, cols 0..2): add 6, 7, 8 in (1,1), (1,2), (2,1)
        grid[1 * 9 + 1] = 6; grid[1 * 9 + 2] = 7; grid[2 * 9 + 1] = 8

        val hint = HintEngine.find(grid)
        assertNotNull(hint)
        hint as HintEngine.NakedSingle
        assertEquals(0, hint.targetCell)
        assertEquals(9, hint.solutionDigit)
        assertEquals("Naked Single", hint.techniqueName)

        // For each eliminated digit, at least one peer should be reported.
        for (d in 1..8) {
            val peers = hint.eliminators[d]
            assertNotNull(peers, "digit $d should have at least one eliminator")
            assertTrue(peers!!.isNotEmpty(), "digit $d eliminator set should be non-empty")
            for (p in peers) {
                assertEquals(d, grid[p], "peer $p should hold the eliminated digit $d")
            }
        }
        // The solution digit should not appear in the eliminators map.
        assertNull(hint.eliminators[hint.solutionDigit])
    }

    @Test
    fun `Note Single fires when a cell's marks are a single legal digit`() {
        // Grid with no naked or hidden singles available, but cell (0,0)
        // has only digit 5 in its player marks. Engine should return
        // NoteSingle for that cell (not naked/hidden single).
        val grid = IntArray(81)
        // Place enough peers so (0,0) is plausibly legal for 5 but has
        // many grid-derived candidates — the deduction relies on marks.
        val marks = Array(81) { idx ->
            if (idx == 0) setOf(5) else (1..9).filter {
                SudokuSolver.isValidPlacement(grid, idx / 9, idx % 9, it)
            }.toSet()
        }
        val hint = HintEngine.find(grid, marks)
        assertTrue(hint is HintEngine.NoteSingle, "expected NoteSingle, got $hint")
        hint as HintEngine.NoteSingle
        assertEquals(0, hint.targetCell)
        assertEquals(5, hint.solutionDigit)
    }

    @Test
    fun `Note Single ignores a single mark that's no longer legal`() {
        // Cell (0,0) has mark {5}, but 5 is already placed at (0,1) so
        // 5 isn't a legal placement at (0,0). Engine must skip Note Single.
        val grid = IntArray(81)
        grid[0 * 9 + 1] = 5
        val marks = Array(81) { idx ->
            when (idx) {
                0 -> setOf(5)
                in (1..80) -> if (grid[idx] != 0) emptySet() else (1..9).filter {
                    SudokuSolver.isValidPlacement(grid, idx / 9, idx % 9, it)
                }.toSet()
                else -> emptySet()
            }
        }
        val hint = HintEngine.find(grid, marks)
        assertFalse(hint is HintEngine.NoteSingle,
            "engine must not return NoteSingle when the lone mark is illegal")
    }

    @Test
    fun `empty grid falls through to a fill-only Candidate Sweep`() {
        // No givens, no notes: no single applies, but seeding candidates
        // is itself work — CandidateSweep with initialFill = true.
        val grid = IntArray(81)
        val hint = HintEngine.find(grid)
        assertTrue(hint is HintEngine.CandidateSweep)
        hint as HintEngine.CandidateSweep
        assertTrue(hint.initialFill)
        assertTrue(hint.plan.isEmpty(), "no givens means no eliminations")
    }

    @Test
    fun `finds a hidden single in a box`() {
        // Block 5 from every cell of box 0 except (0,0) by placing 5
        // along col 1, col 2, row 1, and row 2 (outside box 0).
        val grid = IntArray(81)
        grid[3 * 9 + 1] = 5
        grid[4 * 9 + 2] = 5
        grid[1 * 9 + 5] = 5
        grid[2 * 9 + 7] = 5

        val hint = HintEngine.find(grid)
        assertNotNull(hint)
        hint as HintEngine.HiddenSingle
        assertEquals(0, hint.targetCell)
        assertEquals(5, hint.solutionDigit)
        assertEquals("Hidden Single", hint.techniqueName)
        assertEquals(HintEngine.UnitKind.Box, hint.unit.kind)
        // Every other empty cell in the unit must report at least one peer
        // outside the unit holding the digit.
        for (cellIdx in hint.unit.cells) {
            if (cellIdx == hint.targetCell) continue
            if (grid[cellIdx] != 0) continue
            val peers = hint.eliminators[cellIdx]
            assertNotNull(peers, "cell $cellIdx should have eliminator peers")
            assertTrue(peers!!.isNotEmpty())
            for (p in peers) assertEquals(5, grid[p])
        }
    }

    @Test
    fun `prefers naked single over hidden single when both apply`() {
        // (0,0) is a naked single (only 9 fits) AND simultaneously a
        // hidden single for 9 in its row/column/box. Naked should win.
        val grid = IntArray(81)
        grid[1] = 1; grid[2] = 2; grid[3] = 3
        grid[4 * 9 + 0] = 4; grid[5 * 9 + 0] = 5
        grid[1 * 9 + 1] = 6; grid[1 * 9 + 2] = 7; grid[2 * 9 + 1] = 8

        val hint = HintEngine.find(grid)
        assertTrue(hint is HintEngine.NakedSingle)
    }

    @Test
    fun `Test-menu Hidden Single preview grid yields a HiddenSingle`() {
        val solution = CANONICAL_SOLUTION
        val givenIndices = intArrayOf(3 * 9 + 1, 1 * 9 + 5, 2 * 9 + 6, 8 * 9 + 2)
        val grid = IntArray(81)
        for (idx in givenIndices) grid[idx] = solution[idx]
        val hint = HintEngine.find(grid)
        assertTrue(hint is HintEngine.HiddenSingle, "expected HiddenSingle, got $hint")
        hint as HintEngine.HiddenSingle
        assertEquals(0, hint.targetCell)
        assertEquals(5, hint.solutionDigit)
        assertEquals(HintEngine.UnitKind.Box, hint.unit.kind)
    }

    @Test
    fun `Test-menu Naked Single preview grid yields a NakedSingle`() {
        val solution = CANONICAL_SOLUTION
        val givenIndices = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val grid = IntArray(81)
        for (idx in givenIndices) grid[idx] = solution[idx]
        val hint = HintEngine.find(grid)
        assertTrue(hint is HintEngine.NakedSingle, "expected NakedSingle, got $hint")
        hint as HintEngine.NakedSingle
        assertEquals(0, hint.targetCell)
        assertEquals(5, hint.solutionDigit)
    }

    private val CANONICAL_SOLUTION = intArrayOf(
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

    @Test
    fun `Candidate Sweep fires with initialFill when no notes exist`() {
        // Three givens in box 0's diagonal. No naked singles, no hidden
        // singles — but seeded candidates would lose lots of marks.
        val grid = IntArray(81)
        grid[0] = 5
        grid[1 * 9 + 1] = 7
        grid[2 * 9 + 2] = 8

        val hint = HintEngine.find(grid)
        assertTrue(hint is HintEngine.CandidateSweep, "expected CandidateSweep, got $hint")
        hint as HintEngine.CandidateSweep
        assertTrue(hint.initialFill)
        assertTrue(hint.plan.isNotEmpty())
        // Sanity: (0,1) should lose 5 (row 0), 7 (col 1 + box 0), 8 (box 0).
        val planForCell = hint.plan.firstOrNull { it.cellIdx == 1 }
        assertNotNull(planForCell)
        assertEquals(setOf(5, 7, 8), planForCell!!.doomedDigits)
    }

    @Test
    fun `Candidate Sweep returns null when notes exist and nothing to prune`() {
        // Three givens; pretend the player already has valid candidates
        // everywhere (each empty cell has exactly the digits not blocked
        // by a peer). Then there's no pruning work.
        val grid = IntArray(81)
        grid[0] = 5
        grid[1 * 9 + 1] = 7
        grid[2 * 9 + 2] = 8
        val marks = Array(81) { idx ->
            if (grid[idx] != 0) {
                emptySet()
            } else {
                val r = idx / 9; val c = idx % 9
                (1..9).filter { SudokuSolver.isValidPlacement(grid, r, c, it) }.toSet()
            }
        }
        // Naked/hidden singles still don't apply here (every cell has many
        // candidates), so find() falls through to CandidateSweep — which
        // should now report null because there's nothing to remove.
        assertNull(HintEngine.find(grid, marks))
    }

    @Test
    fun `Test-menu Candidate Sweep grid yields a CandidateSweep`() {
        val solution = CANONICAL_SOLUTION
        val givenIndices = intArrayOf(0, 1 * 9 + 1, 2 * 9 + 2)
        val grid = IntArray(81)
        for (idx in givenIndices) grid[idx] = solution[idx]
        val hint = HintEngine.find(grid)
        assertTrue(hint is HintEngine.CandidateSweep)
    }

    @Test
    fun `Naked Pair stops firing after its eliminations are applied`() {
        // The Naked Pair test-menu grid (same one the search tool found).
        val givens = intArrayOf(
            7, 8, 12, 16, 18, 22, 24, 28, 29, 31, 39, 42, 46, 47, 52, 53,
            54, 56, 58, 60, 62, 63, 65, 66, 67, 70, 71, 74, 75, 78, 79,
        )
        val grid = IntArray(81)
        for (idx in givens) grid[idx] = CANONICAL_SOLUTION[idx]
        // Pretend candidates were just filled in (valid candidates per cell).
        val marks = Array(81) { idx ->
            if (grid[idx] != 0) emptySet() else (1..9).filter {
                SudokuSolver.isValidPlacement(grid, idx / 9, idx % 9, it)
            }.toMutableSet() as MutableSet<Int>
        }

        val first = HintEngine.find(grid, marks)
        assertTrue(first is HintEngine.NakedPair, "expected NakedPair, got $first")
        first as HintEngine.NakedPair

        // Apply the pair's eliminations to the player marks.
        for (step in first.plan) {
            (marks[step.cellIdx] as MutableSet).removeAll(step.doomedDigits)
        }

        // The same Naked Pair must not be returned again.
        val second = HintEngine.find(grid, marks)
        if (second is HintEngine.NakedPair) {
            assertTrue(second.keyCells != first.keyCells,
                "engine should not re-emit the same Naked Pair after its plan is applied")
        }
    }

    // ---- Level 3+ technique grids ----

    /**
     * Build the test grid + valid-candidate marks for the indices the
     * Test-menu uses for [techniqueName], then assert [HintEngine.find]
     * classifies the next deduction as [expectedClass].
     */
    private fun assertTechniqueGridReturns(
        techniqueName: String,
        givenIndices: IntArray,
        check: (HintEngine.Hint) -> Boolean,
    ) {
        val grid = IntArray(81)
        for (idx in givenIndices) grid[idx] = CANONICAL_SOLUTION[idx]
        val marks = Array(81) { idx ->
            if (grid[idx] != 0) emptySet() else (1..9).filter {
                SudokuSolver.isValidPlacement(grid, idx / 9, idx % 9, it)
            }.toSet()
        }
        val hint = HintEngine.find(grid, marks)
        assertNotNull(hint, "$techniqueName: find() returned null")
        assertTrue(check(hint!!), "$techniqueName: got ${hint::class.simpleName}")
    }

    @Test
    fun `Naked Pair on the test-menu grid`() {
        assertTechniqueGridReturns(
            "Naked Pair",
            intArrayOf(
                7, 8, 12, 16, 18, 22, 24, 28, 29, 31, 39, 42, 46, 47, 52, 53,
                54, 56, 58, 60, 62, 63, 65, 66, 67, 70, 71, 74, 75, 78, 79,
            ),
        ) { it is HintEngine.NakedPair }
    }

    @Test
    fun `Hidden Pair on the test-menu grid`() {
        assertTechniqueGridReturns(
            "Hidden Pair",
            intArrayOf(
                8, 12, 16, 19, 20, 21, 23, 24, 25, 30, 33, 40, 45, 48, 50,
                53, 55, 56, 61, 63, 67, 69, 71, 74, 77, 78,
            ),
        ) { it is HintEngine.HiddenPair }
    }

    @Test
    fun `Pointing Pair on the test-menu grid`() {
        assertTechniqueGridReturns(
            "Pointing Pair",
            intArrayOf(
                3, 4, 6, 10, 14, 24, 29, 33, 36, 37, 38, 43, 44, 45, 47, 52,
                53, 56, 65, 66, 67, 68, 69, 71, 78, 80,
            ),
        ) { it is HintEngine.PointingPair }
    }

    @Test
    fun `Box-Line Reduction on the test-menu grid`() {
        assertTechniqueGridReturns(
            "Box-Line Reduction",
            intArrayOf(
                18, 19, 20, 22, 24, 27, 32, 33, 34, 35, 37, 42, 52, 53, 57,
                62, 65, 69, 70, 72, 77,
            ),
        ) { it is HintEngine.BoxLineReduction }
    }

    @Test
    fun `Naked Triple on the test-menu grid`() {
        assertTechniqueGridReturns(
            "Naked Triple",
            intArrayOf(
                0, 2, 10, 13, 16, 17, 19, 22, 28, 32, 33, 41, 44, 47, 48, 49,
                54, 62, 63, 74, 78,
            ),
        ) { it is HintEngine.NakedTriple }
    }

    @Test
    fun `Hidden Triple on the test-menu grid`() {
        assertTechniqueGridReturns(
            "Hidden Triple",
            intArrayOf(
                6, 7, 13, 23, 37, 40, 42, 43, 51, 65, 69, 70, 71, 72, 74, 79,
            ),
        ) { it is HintEngine.HiddenTriple }
    }

    @Test
    fun `X-Wing on the test-menu grid`() {
        assertTechniqueGridReturns(
            "X-Wing",
            intArrayOf(
                0, 1, 10, 12, 14, 16, 20, 21, 26, 28, 32, 46, 49, 51, 64, 65,
                68, 70, 75, 77, 79,
            ),
        ) { it is HintEngine.XWing }
    }

    @Test
    fun `XY-Wing on the test-menu grid`() {
        assertTechniqueGridReturns(
            "XY-Wing",
            intArrayOf(
                2, 5, 6, 10, 11, 15, 16, 24, 28, 35, 37, 39, 42, 44, 52, 55,
                57, 60, 62, 67, 70, 71, 73, 74, 76, 78, 79,
            ),
        ) { it is HintEngine.XYWing }
    }

    @Test
    fun `Empty Rectangle eliminates the corner cell`() {
        // The worked example from docs/testing/hint-engine-tests.md:
        //  - box 3 (rows 3-5, cols 0-2) has all 4-candidates on row 4 or col 0
        //  - col 3 has exactly two 4-candidates at (4,3) and (8,3)
        //  - elimination: 4 at (8,0)
        // Built with a fill value of 1 so peer-blocking doesn't affect digit 4,
        // and explicit marks of {4} on every cell that should hold the candidate.
        val grid = IntArray(81) { 1 }
        val empties = listOf(
            3 * 9 + 0, 4 * 9 + 0, 4 * 9 + 1, 4 * 9 + 2, 5 * 9 + 0,
            4 * 9 + 3, 8 * 9 + 3, 8 * 9 + 0,
        )
        for (idx in empties) grid[idx] = 0
        val marks = Array<Set<Int>>(81) { emptySet() }
        for (idx in empties) marks[idx] = setOf(4)

        val cands = HintEngine.candidateGrid(grid, marks)
        val hint = HintEngine.findEmptyRectangle(grid, cands)

        assertNotNull(hint, "ER finder returned null on the worked example")
        hint!!
        assertEquals(4, hint.digit)
        assertEquals(1, hint.plan.size)
        val step = hint.plan[0]
        assertEquals(8 * 9 + 0, step.cellIdx, "elimination must target (8,0)")
        assertTrue(4 in step.doomedDigits)
    }

    @Test
    fun `returns null on a fully solved grid`() {
        assertNull(HintEngine.find(CANONICAL_SOLUTION))
    }
}
