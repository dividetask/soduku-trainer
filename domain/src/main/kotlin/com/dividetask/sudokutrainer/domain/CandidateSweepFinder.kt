package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.CandidateSweep
import com.dividetask.sudokutrainer.domain.BoardGeometry.peersHolding

/** Finds the notes a Candidate Sweep would fill in and prune. */
internal object CandidateSweepFinder {
    fun find(grid: IntArray, marks: Array<Set<Int>>?): CandidateSweep? {
        val anyMarks = marks != null && marks.any { it.isNotEmpty() }
        val initialFill = !anyMarks
        val hasEmptyCells = (0 until 81).any { grid[it] == 0 }

        val plan = mutableListOf<CandidateSweep.CellPlan>()
        for (idx in 0 until 81) {
            if (grid[idx] != 0) continue
            val candidates = if (initialFill) ALL_DIGITS else marks!![idx]
            if (candidates.isEmpty()) continue
            val row = idx / 9
            val col = idx % 9
            val doomed = mutableSetOf<Int>()
            val peers = mutableSetOf<Int>()
            for (d in candidates) {
                val blockers = peersHolding(grid, row, col, d)
                if (blockers.isNotEmpty()) {
                    doomed += d
                    peers += blockers
                }
            }
            if (doomed.isNotEmpty()) {
                plan += CandidateSweep.CellPlan(
                    cellIdx = idx,
                    doomedDigits = doomed,
                    eliminatorPeers = peers,
                )
            }
        }

        // No work to do: either there are no empty cells to fill *and*
        // nothing to prune, or notes already exist and nothing to prune.
        val hasFillWork = initialFill && hasEmptyCells
        if (plan.isEmpty() && !hasFillWork) return null
        return CandidateSweep(initialFill = initialFill, plan = plan)
    }

    private val ALL_DIGITS: Set<Int> = (1..9).toSet()
}
