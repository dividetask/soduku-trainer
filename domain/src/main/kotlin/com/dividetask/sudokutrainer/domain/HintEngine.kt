package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.CandidateSweep
import com.dividetask.sudokutrainer.domain.Hint.EliminationHint
import com.dividetask.sudokutrainer.domain.Hint.HiddenSingle
import com.dividetask.sudokutrainer.domain.Hint.NakedSingle

/**
 * Finds the easiest applicable solving technique on a partial grid. Its
 * one job is the order techniques are tried in, easiest first; each
 * technique's search lives in its own finder (e.g. [XWingFinder]).
 */
object HintEngine {

    /**
     * Search [grid] for the easiest technique that produces a placement
     * or note pruning. Returns null if no implemented technique applies.
     *
     * [grid] is a flat 81-entry array with 0 = empty. [marks] is an
     * optional per-cell snapshot of pencil-mark *digits* (colors aren't
     * needed by the engine); when omitted, the engine assumes no marks.
     */
    fun find(grid: IntArray, marks: Array<Set<Int>>? = null): Hint? {
        require(grid.size == 81)
        require(marks == null || marks.size == 81)
        // Hidden singles come first: they're easier to spot than naked
        // singles, especially without notes.
        HiddenSingleFinder.find(grid)?.let { return it }
        NakedSingleFinder.find(grid)?.let { return it }
        NoteSingleFinder.find(grid, marks)?.let { return it }
        CandidateSweepFinder.find(grid, marks)?.let { return it }
        val cands = Candidates.of(grid, marks)
        NakedPairFinder.find(grid, cands)?.let { return it }
        HiddenPairFinder.find(grid, cands)?.let { return it }
        PointingPairFinder.find(grid, cands)?.let { return it }
        BoxLineReductionFinder.find(grid, cands)?.let { return it }
        NakedTripleFinder.find(grid, cands)?.let { return it }
        HiddenTripleFinder.find(grid, cands)?.let { return it }
        XWingFinder.find(grid, cands)?.let { return it }
        EmptyRectangleFinder.find(grid, cands)?.let { return it }
        SwordfishFinder.find(grid, cands)?.let { return it }
        XYWingFinder.find(grid, cands)?.let { return it }
        return null
    }

    /**
     * Every instance of the elimination technique named [techniqueName]
     * (as in [Hint.techniqueName]) on this board, in search order — so
     * the first entry is the one [find] would return. Instances whose
     * eliminations are identical are reported once. Empty for unknown
     * names and for the placement techniques.
     */
    fun allEliminations(
        techniqueName: String,
        grid: IntArray,
        marks: Array<Set<Int>>? = null,
    ): List<EliminationHint> {
        require(grid.size == 81)
        require(marks == null || marks.size == 81)
        val cands = Candidates.of(grid, marks)
        val all: List<EliminationHint> = when (techniqueName) {
            "Naked Pair" -> NakedPairFinder.all(grid, cands)
            "Hidden Pair" -> HiddenPairFinder.all(grid, cands)
            "Pointing Pair" -> PointingPairFinder.all(grid, cands)
            "Box-Line Reduction" -> BoxLineReductionFinder.all(grid, cands)
            "Naked Triple" -> NakedTripleFinder.all(grid, cands)
            "Hidden Triple" -> HiddenTripleFinder.all(grid, cands)
            "X-Wing" -> XWingFinder.all(grid, cands)
            "Empty Rectangle" -> EmptyRectangleFinder.all(grid, cands)
            "Swordfish" -> SwordfishFinder.all(grid, cands)
            "XY-Wing" -> XYWingFinder.all(grid, cands)
            else -> emptyList()
        }
        return all.distinctBy { h -> h.plan.flatMap { s -> s.doomedDigits.map { s.cellIdx * 10 + it } }.toSet() }
    }

    /** Every NakedSingle on [grid], in the order [find] would consider them. */
    fun allNakedSingles(grid: IntArray): List<NakedSingle> = NakedSingleFinder.all(grid)

    /** Every HiddenSingle on [grid], in the order [find] would consider them. */
    fun allHiddenSingles(grid: IntArray): List<HiddenSingle> = HiddenSingleFinder.all(grid)

    /**
     * Public alias for the [CandidateSweep] search so callers that want
     * to trigger a sweep without going through the easiest-first [find]
     * cascade (e.g., the sweep that opens a game) can do so directly.
     */
    fun findCandidateSweep(grid: IntArray, marks: Array<Set<Int>>? = null): CandidateSweep? {
        require(grid.size == 81)
        require(marks == null || marks.size == 81)
        return CandidateSweepFinder.find(grid, marks)
    }
}
