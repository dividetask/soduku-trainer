package com.dividetask.sudokutrainer.domain

/**
 * A technique that can be applied to advance the solve, with the data
 * needed to teach it. Found by [HintEngine]; each technique's search is
 * its own finder object.
 */
sealed interface Hint {
    val techniqueName: String

    /**
     * The target cell has exactly one remaining candidate. [eliminators]
     * maps each non-solution digit (1..9) to the set of peer cells whose
     * values prove that digit can't go in the target.
     */
    data class NakedSingle(
        val targetCell: Int,
        val solutionDigit: Int,
        val eliminators: Map<Int, Set<Int>>,
    ) : Hint {
        override val techniqueName: String = "Naked Single"
    }

    enum class UnitKind { Box, Row, Column }

    /** A row, column, or 3x3 box; the unit a deduction lives in. */
    data class HintUnit(val kind: UnitKind, val cells: List<Int>)

    /**
     * Inside [unit], [solutionDigit] has exactly one legal cell — the
     * [targetCell]. [eliminators] maps each *other empty* cell of the unit
     * to the set of peer cells outside [unit] whose values prove the digit
     * can't go in that cell. Filled cells in the unit are self-evident and
     * have no entry.
     */
    data class HiddenSingle(
        val targetCell: Int,
        val solutionDigit: Int,
        val unit: HintUnit,
        val eliminators: Map<Int, Set<Int>>,
    ) : Hint {
        override val techniqueName: String = "Hidden Single"
    }

    /**
     * The player has narrowed a cell's pencil marks down to a single
     * digit (typically by applying earlier eliminations). Place it.
     *
     * Fires only when [HintEngine.find] is called with non-null
     * `marks`, after Naked/Hidden Single (which work off grid values
     * alone) but before Candidate Sweep (which would rewrite the
     * notes entirely).
     */
    data class NoteSingle(
        val targetCell: Int,
        val solutionDigit: Int,
    ) : Hint {
        override val techniqueName: String = "Note Single"
    }

    /**
     * Prune impossible pencil marks from every empty cell — any candidate
     * digit blocked by a peer in the same row, column, or box is removed.
     * When the board has no player notes at all, the sweep also *adds* the
     * full 1..9 candidate set to every empty cell before pruning, so
     * downstream techniques (pairs, etc.) have notes to work with.
     *
     * [plan] enumerates the cells that actually lose at least one mark, in
     * row-major order. [initialFill] is true iff the sweep needs to seed
     * candidates because the board had no notes at the start.
     */
    data class CandidateSweep(
        val initialFill: Boolean,
        val plan: List<CellPlan>,
    ) : Hint {
        override val techniqueName: String = "Candidate Sweep"

        /**
         * [doomedDigits] are the digits that will be erased from [cellIdx];
         * [eliminatorPeers] are the peer cells outside this cell whose
         * values prove the eliminations.
         */
        data class CellPlan(
            val cellIdx: Int,
            val doomedDigits: Set<Int>,
            val eliminatorPeers: Set<Int>,
        )
    }

    /**
     * Common payload for elimination-style techniques: every level-3+
     * technique highlights a small "pattern" set of cells and erases a
     * computed set of digits from other cells. The animator and Board
     * render any [EliminationHint] uniformly.
     *
     * - [keyCells] are the cells that *define* the pattern (e.g. the
     *   pair, the rectangle corners). They are tinted green throughout.
     * - [unitCells] optionally frames the scope of the deduction (the
     *   pair's row, the X-Wing's columns); empty when no single frame
     *   applies.
     * - [plan] is the per-cell elimination plan, in iteration order.
     */
    sealed interface EliminationHint : Hint {
        val keyCells: Set<Int>
        val unitCells: Set<Int>
        val plan: List<EliminationStep>
    }

    /**
     * One step of an elimination sweep: erase [doomedDigits] from
     * [cellIdx]'s pencil marks. [explainerCells] are the cells of the
     * pattern that prove this elimination — the Board lights them red
     * during the step (typically a subset of [EliminationHint.keyCells]).
     */
    data class EliminationStep(
        val cellIdx: Int,
        val doomedDigits: Set<Int>,
        val explainerCells: Set<Int>,
    )

    data class NakedPair(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val pairDigits: Set<Int>,
    ) : EliminationHint {
        override val techniqueName: String = "Naked Pair"
    }

    data class HiddenPair(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val pairDigits: Set<Int>,
    ) : EliminationHint {
        override val techniqueName: String = "Hidden Pair"
    }

    data class NakedTriple(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val tripleDigits: Set<Int>,
    ) : EliminationHint {
        override val techniqueName: String = "Naked Triple"
    }

    data class HiddenTriple(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val tripleDigits: Set<Int>,
    ) : EliminationHint {
        override val techniqueName: String = "Hidden Triple"
    }

    data class PointingPair(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val digit: Int,
    ) : EliminationHint {
        override val techniqueName: String = "Pointing Pair"
    }

    data class BoxLineReduction(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val digit: Int,
    ) : EliminationHint {
        override val techniqueName: String = "Box-Line Reduction"
    }

    data class XWing(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val digit: Int,
    ) : EliminationHint {
        override val techniqueName: String = "X-Wing"
    }

    /**
     * Inside a box, a digit's candidates all lie on a single row + a
     * single column (an L shape). A conjugate pair elsewhere supplies a
     * "strong link" with one endpoint on the row leg outside the box —
     * the elimination is the cell at (free-end-row, column-leg).
     *
     * Symmetric for a conjugate pair on the row through the col leg.
     */
    data class EmptyRectangle(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val digit: Int,
    ) : EliminationHint {
        override val techniqueName: String = "Empty Rectangle"
    }

    data class Swordfish(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val digit: Int,
    ) : EliminationHint {
        override val techniqueName: String = "Swordfish"
    }

    data class XYWing(
        override val keyCells: Set<Int>,
        override val unitCells: Set<Int>,
        override val plan: List<EliminationStep>,
        val sharedDigit: Int,
    ) : EliminationHint {
        override val techniqueName: String = "XY-Wing"
    }
}
