package com.dividetask.sudokutrainer.domain

/**
 * Transient UI state driving the hint animation. Lives on [GameState] and
 * is updated frame-by-frame by the ViewModel; the Board renders it.
 */
sealed interface HintUi {

    /**
     * Brief red flash when no technique applies. The Board paints the
     * board background red while this is set.
     */
    data object NoHintFlash : HintUi

    /**
     * Naked-single teaching animation in progress.
     *
     * - [targetCell] is shaded green for the duration.
     * - During the 1..9 sweep, [activeDigit] is the digit currently being
     *   evaluated and [eliminatorCells] are the peer cells that prove that
     *   digit can't go in the target (or empty for the solution digit).
     * - [originalNotes] captures the target cell's pencil marks at begin
     *   (which are then overwritten by the 1..9 sweep) so undo can
     *   restore them when the hint move is reversed.
     */
    data class NakedSingle(
        val targetCell: Int,
        val solutionDigit: Int,
        val activeDigit: Int? = null,
        val eliminatorCells: Set<Int> = emptySet(),
        val originalNotes: Map<Int, GuessColor> = emptyMap(),
    ) : HintUi

    /**
     * Hidden-single teaching animation in progress.
     *
     * - [unitCells] is the deduction's row/column/box; the Board tints it
     *   to frame the scope of the inference.
     * - [targetCell] is shaded green for the duration.
     * - During the per-cell sweep, [currentCell] is the unit cell whose
     *   elimination is currently being shown, and [eliminatorCells] are
     *   the peer cells *outside* the unit that prove the solution digit
     *   can't go there.
     * - [targetOriginalNotes] and [clearedPeerMarks] capture pre-hint
     *   state so undo can restore notes the animation erased.
     */
    data class HiddenSingle(
        val targetCell: Int,
        val solutionDigit: Int,
        val unitCells: Set<Int>,
        val currentCell: Int? = null,
        val eliminatorCells: Set<Int> = emptySet(),
        val targetOriginalNotes: Map<Int, GuessColor> = emptyMap(),
        val clearedPeerMarks: List<AutoClearedMark> = emptyList(),
    ) : HintUi

    /**
     * Candidate-sweep animation in progress.
     *
     * - [initialFill] indicates the board needed candidates filled in.
     * - [currentCell] is the cell currently being pruned (or null for
     *   the settle frame).
     * - [eliminatorCells] are the peers whose values prove the current
     *   cell's eliminations.
     * - [priorMarks] is the full pencil-mark snapshot captured at begin;
     *   completion uses it to build the consolidated undo move.
     */
    /**
     * Note-single teaching animation in progress. Just frames the
     * target cell green and pauses; the lone pencil mark is already
     * what the player sees.
     *
     * [originalNotes] captures pre-hint marks for undo.
     */
    data class NoteSingle(
        val targetCell: Int,
        val solutionDigit: Int,
        val originalNotes: Map<Int, GuessColor> = emptyMap(),
    ) : HintUi

    data class CandidateSweep(
        val initialFill: Boolean,
        val currentCell: Int? = null,
        val eliminatorCells: Set<Int> = emptySet(),
        val priorMarks: List<AutoClearedMark> = emptyList(),
    ) : HintUi

    /**
     * Animation state for any level-3+ elimination technique (Naked /
     * Hidden Pair, Triple, Pointing Pair, Box-Line Reduction, X-Wing,
     * Swordfish, XY-Wing). The shape is uniform so the Board and the
     * animator don't need a separate code path per technique.
     *
     * - [keyCells] = the pattern cells (green throughout).
     * - [unitCells] = optional broader unit frame (pale blue).
     * - [currentCell] / [explainerCells] = the cell currently being
     *   pruned plus the pattern subset that proves it.
     * - [priorMarks] = pre-hint snapshot for the consolidated undo move.
     */
    data class Elimination(
        val techniqueName: String,
        val keyCells: Set<Int>,
        val unitCells: Set<Int> = emptySet(),
        val currentCell: Int? = null,
        val explainerCells: Set<Int> = emptySet(),
        val priorMarks: List<AutoClearedMark> = emptyList(),
    ) : HintUi
}
