package com.dividetask.sudokutrainer.domain

/**
 * Finds the easiest applicable solving technique on a partial grid and
 * returns the data needed to teach the player how the deduction works.
 *
 * Techniques are tried in order from easiest to hardest, matching the
 * progression used by [LogicSolver]. Only naked singles are implemented
 * in v1; the remaining techniques will plug into [find] without changes
 * to callers.
 */
object HintEngine {

    /** A technique that can be applied to advance the solve. */
    sealed interface Hint {
        val techniqueName: String
    }

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
        findNakedSingle(grid)?.let { return it }
        findHiddenSingle(grid)?.let { return it }
        findNoteSingle(grid, marks)?.let { return it }
        findCandidateSweepInternal(grid, marks)?.let { return it }
        val cands = candidateGrid(grid, marks)
        findNakedPair(grid, cands)?.let { return it }
        findHiddenPair(grid, cands)?.let { return it }
        findPointingPair(grid, cands)?.let { return it }
        findBoxLineReduction(grid, cands)?.let { return it }
        findNakedTriple(grid, cands)?.let { return it }
        findHiddenTriple(grid, cands)?.let { return it }
        findXWing(grid, cands)?.let { return it }
        findEmptyRectangle(grid, cands)?.let { return it }
        findSwordfish(grid, cands)?.let { return it }
        findXYWing(grid, cands)?.let { return it }
        return null
    }

    private fun findNakedSingle(grid: IntArray): NakedSingle? {
        for (idx in 0 until 81) {
            if (grid[idx] != 0) continue
            val row = idx / 9
            val col = idx % 9
            val candidates = mutableListOf<Int>()
            for (d in 1..9) {
                if (SudokuSolver.isValidPlacement(grid, row, col, d)) candidates += d
            }
            if (candidates.size != 1) continue
            val solution = candidates[0]
            val eliminators = mutableMapOf<Int, Set<Int>>()
            for (d in 1..9) {
                if (d == solution) continue
                eliminators[d] = peersHolding(grid, row, col, d)
            }
            return NakedSingle(
                targetCell = idx,
                solutionDigit = solution,
                eliminators = eliminators,
            )
        }
        return null
    }

    private fun findHiddenSingle(grid: IntArray): HiddenSingle? {
        // Iteration order encodes the tie-break: boxes are tried before
        // rows, rows before columns — so when the same digit is a hidden
        // single in multiple units the box deduction wins.
        for (unit in UNITS_ORDERED) {
            val unitValues = unit.cells.mapTo(mutableSetOf()) { grid[it] }
            for (d in 1..9) {
                if (d in unitValues) continue
                var target = -1
                var multiple = false
                for (idx in unit.cells) {
                    if (grid[idx] != 0) continue
                    val row = idx / 9
                    val col = idx % 9
                    if (!SudokuSolver.isValidPlacement(grid, row, col, d)) continue
                    if (target == -1) {
                        target = idx
                    } else {
                        multiple = true
                        break
                    }
                }
                if (target == -1 || multiple) continue
                val eliminators = buildMap {
                    val unitSet = unit.cells.toSet()
                    for (idx in unit.cells) {
                        if (idx == target || grid[idx] != 0) continue
                        put(idx, peersOutsideUnitHolding(grid, idx, unitSet, d))
                    }
                }
                return HiddenSingle(
                    targetCell = target,
                    solutionDigit = d,
                    unit = unit,
                    eliminators = eliminators,
                )
            }
        }
        return null
    }

    /**
     * Public alias for the [CandidateSweep] search so callers that want
     * to trigger a sweep without going through the easiest-first [find]
     * cascade (e.g., a dedicated Sweep button) can do so directly.
     */
    fun findCandidateSweep(grid: IntArray, marks: Array<Set<Int>>? = null): CandidateSweep? {
        require(grid.size == 81)
        require(marks == null || marks.size == 81)
        return findCandidateSweepInternal(grid, marks)
    }

    /**
     * Walks empty cells and reports the first one whose player marks are
     * exactly one digit (and that digit is still a legal placement). The
     * lone mark *might* be smaller than the grid-computed candidate set,
     * which is why this fires between Hidden Single and Candidate Sweep
     * — the player has done elimination work that grid values alone
     * don't reflect.
     */
    internal fun findNoteSingle(grid: IntArray, marks: Array<Set<Int>>?): NoteSingle? {
        if (marks == null) return null
        for (idx in 0 until 81) {
            if (grid[idx] != 0) continue
            val cellMarks = marks[idx]
            if (cellMarks.size != 1) continue
            val d = cellMarks.first()
            val r = idx / 9; val c = idx % 9
            if (!SudokuSolver.isValidPlacement(grid, r, c, d)) continue
            return NoteSingle(targetCell = idx, solutionDigit = d)
        }
        return null
    }

    private fun findCandidateSweepInternal(grid: IntArray, marks: Array<Set<Int>>?): CandidateSweep? {
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

    // ---- Level 3+ candidate-based techniques ----

    /**
     * Per-cell candidate set used by pair/triple/wing finders. When the
     * player has pencil marks for a cell, those marks (filtered to the
     * digits still legal given grid values) are the effective candidates
     * — so once a higher-level technique has erased a digit from a peer's
     * marks, the next [find] call no longer treats that digit as a
     * candidate for that cell and the same hint can't be returned again.
     *
     * When a cell has no player marks, fall back to the full set of
     * digits that grid values don't yet rule out.
     *
     * Marked `internal` for test isolation — tests that exercise a
     * single finder directly need to construct the same candidate set
     * the engine would compute.
     */
    internal fun candidateGrid(grid: IntArray, marks: Array<Set<Int>>?): Array<Set<Int>> =
        Array(81) { idx ->
            if (grid[idx] != 0) return@Array emptySet()
            val r = idx / 9; val c = idx % 9
            val playerMarks = marks?.get(idx) ?: emptySet()
            if (playerMarks.isNotEmpty()) {
                playerMarks.filterTo(mutableSetOf()) {
                    SudokuSolver.isValidPlacement(grid, r, c, it)
                }
            } else {
                (1..9).filterTo(mutableSetOf()) {
                    SudokuSolver.isValidPlacement(grid, r, c, it)
                }
            }
        }

    internal fun findNakedPair(grid: IntArray, cands: Array<Set<Int>>): NakedPair? {
        for (unit in UNITS_ORDERED) {
            val pairCells = unit.cells.filter { grid[it] == 0 && cands[it].size == 2 }
            for (i in pairCells.indices) {
                for (j in i + 1 until pairCells.size) {
                    val a = pairCells[i]; val b = pairCells[j]
                    if (cands[a] != cands[b]) continue
                    val digits = cands[a]
                    val plan = mutableListOf<EliminationStep>()
                    for (cell in unit.cells) {
                        if (cell == a || cell == b || grid[cell] != 0) continue
                        val doomed = cands[cell].intersect(digits)
                        if (doomed.isEmpty()) continue
                        plan += EliminationStep(
                            cellIdx = cell,
                            doomedDigits = doomed,
                            explainerCells = setOf(a, b),
                        )
                    }
                    if (plan.isEmpty()) continue
                    return NakedPair(
                        keyCells = setOf(a, b),
                        unitCells = unit.cells.toSet(),
                        plan = plan,
                        pairDigits = digits,
                    )
                }
            }
        }
        return null
    }

    internal fun findHiddenPair(grid: IntArray, cands: Array<Set<Int>>): HiddenPair? {
        for (unit in UNITS_ORDERED) {
            val unsolved = unit.cells.filter { grid[it] == 0 }
            val digitCells: Map<Int, List<Int>> = (1..9).associateWith { d ->
                unsolved.filter { d in cands[it] }
            }
            val twoCellDigits = digitCells.filter { (_, cells) -> cells.size == 2 }
            val digits = twoCellDigits.keys.toList()
            for (i in digits.indices) {
                for (j in i + 1 until digits.size) {
                    val d1 = digits[i]; val d2 = digits[j]
                    if (twoCellDigits[d1] != twoCellDigits[d2]) continue
                    val pair = setOf(d1, d2)
                    val keys = twoCellDigits[d1]!!.toSet()
                    val plan = mutableListOf<EliminationStep>()
                    for (cell in keys) {
                        val doomed = cands[cell] - pair
                        if (doomed.isEmpty()) continue
                        // Explainer: the other cells of the unit where d1/d2 can't go.
                        val others = unit.cells.filter {
                            it != cell && grid[it] == 0 && it !in keys
                        }.toSet()
                        plan += EliminationStep(
                            cellIdx = cell,
                            doomedDigits = doomed,
                            explainerCells = others,
                        )
                    }
                    if (plan.isEmpty()) continue
                    return HiddenPair(
                        keyCells = keys,
                        unitCells = unit.cells.toSet(),
                        plan = plan,
                        pairDigits = pair,
                    )
                }
            }
        }
        return null
    }

    internal fun findPointingPair(grid: IntArray, cands: Array<Set<Int>>): PointingPair? {
        for (boxIdx in 0 until 9) {
            val box = UNITS_ORDERED[boxIdx]
            for (d in 1..9) {
                val cells = box.cells.filter { grid[it] == 0 && d in cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val rows = cells.map { it / 9 }.toSet()
                val cols = cells.map { it % 9 }.toSet()
                if (rows.size == 1) {
                    val row = rows.first()
                    val plan = mutableListOf<EliminationStep>()
                    for (c in 0 until 9) {
                        val idx = row * 9 + c
                        if (idx in box.cells) continue
                        if (grid[idx] == 0 && d in cands[idx]) {
                            plan += EliminationStep(idx, setOf(d), cells.toSet())
                        }
                    }
                    if (plan.isNotEmpty()) return PointingPair(
                        keyCells = cells.toSet(),
                        unitCells = box.cells.toSet(),
                        plan = plan,
                        digit = d,
                    )
                }
                if (cols.size == 1) {
                    val col = cols.first()
                    val plan = mutableListOf<EliminationStep>()
                    for (r in 0 until 9) {
                        val idx = r * 9 + col
                        if (idx in box.cells) continue
                        if (grid[idx] == 0 && d in cands[idx]) {
                            plan += EliminationStep(idx, setOf(d), cells.toSet())
                        }
                    }
                    if (plan.isNotEmpty()) return PointingPair(
                        keyCells = cells.toSet(),
                        unitCells = box.cells.toSet(),
                        plan = plan,
                        digit = d,
                    )
                }
            }
        }
        return null
    }

    internal fun findBoxLineReduction(grid: IntArray, cands: Array<Set<Int>>): BoxLineReduction? {
        for (d in 1..9) {
            for (r in 0 until 9) {
                val rowCells = (0 until 9).map { r * 9 + it }
                val cells = rowCells.filter { grid[it] == 0 && d in cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val boxes = cells.map { boxOf(it) }.toSet()
                if (boxes.size != 1) continue
                val boxIdx = boxes.first()
                val boxCells = UNITS_ORDERED[boxIdx].cells
                val plan = mutableListOf<EliminationStep>()
                for (idx in boxCells) {
                    if (idx in cells) continue
                    if (grid[idx] == 0 && d in cands[idx]) {
                        plan += EliminationStep(idx, setOf(d), cells.toSet())
                    }
                }
                if (plan.isNotEmpty()) return BoxLineReduction(
                    keyCells = cells.toSet(),
                    unitCells = rowCells.toSet(),
                    plan = plan,
                    digit = d,
                )
            }
            for (c in 0 until 9) {
                val colCells = (0 until 9).map { it * 9 + c }
                val cells = colCells.filter { grid[it] == 0 && d in cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val boxes = cells.map { boxOf(it) }.toSet()
                if (boxes.size != 1) continue
                val boxIdx = boxes.first()
                val boxCells = UNITS_ORDERED[boxIdx].cells
                val plan = mutableListOf<EliminationStep>()
                for (idx in boxCells) {
                    if (idx in cells) continue
                    if (grid[idx] == 0 && d in cands[idx]) {
                        plan += EliminationStep(idx, setOf(d), cells.toSet())
                    }
                }
                if (plan.isNotEmpty()) return BoxLineReduction(
                    keyCells = cells.toSet(),
                    unitCells = colCells.toSet(),
                    plan = plan,
                    digit = d,
                )
            }
        }
        return null
    }

    internal fun findNakedTriple(grid: IntArray, cands: Array<Set<Int>>): NakedTriple? {
        for (unit in UNITS_ORDERED) {
            val candidates = unit.cells.filter { grid[it] == 0 && cands[it].size in 2..3 }
            if (candidates.size < 3) continue
            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    for (k in j + 1 until candidates.size) {
                        val a = candidates[i]; val b = candidates[j]; val c = candidates[k]
                        val union = cands[a] + cands[b] + cands[c]
                        if (union.size != 3) continue
                        val plan = mutableListOf<EliminationStep>()
                        for (cell in unit.cells) {
                            if (cell == a || cell == b || cell == c || grid[cell] != 0) continue
                            val doomed = cands[cell].intersect(union)
                            if (doomed.isEmpty()) continue
                            plan += EliminationStep(cell, doomed, setOf(a, b, c))
                        }
                        if (plan.isEmpty()) continue
                        return NakedTriple(
                            keyCells = setOf(a, b, c),
                            unitCells = unit.cells.toSet(),
                            plan = plan,
                            tripleDigits = union,
                        )
                    }
                }
            }
        }
        return null
    }

    internal fun findHiddenTriple(grid: IntArray, cands: Array<Set<Int>>): HiddenTriple? {
        for (unit in UNITS_ORDERED) {
            val digitCells: Map<Int, List<Int>> = (1..9).associateWith { d ->
                unit.cells.filter { grid[it] == 0 && d in cands[it] }
            }
            val eligible = digitCells.filter { (_, cells) -> cells.size in 2..3 }.keys.toList()
            if (eligible.size < 3) continue
            for (i in eligible.indices) {
                for (j in i + 1 until eligible.size) {
                    for (k in j + 1 until eligible.size) {
                        val triple = setOf(eligible[i], eligible[j], eligible[k])
                        val union = digitCells[eligible[i]]!! +
                            digitCells[eligible[j]]!! + digitCells[eligible[k]]!!
                        val keys = union.toSet()
                        if (keys.size != 3) continue
                        val plan = mutableListOf<EliminationStep>()
                        for (cell in keys) {
                            val doomed = cands[cell] - triple
                            if (doomed.isEmpty()) continue
                            val others = unit.cells.filter {
                                it != cell && grid[it] == 0 && it !in keys
                            }.toSet()
                            plan += EliminationStep(cell, doomed, others)
                        }
                        if (plan.isEmpty()) continue
                        return HiddenTriple(
                            keyCells = keys,
                            unitCells = unit.cells.toSet(),
                            plan = plan,
                            tripleDigits = triple,
                        )
                    }
                }
            }
        }
        return null
    }

    internal fun findXWing(grid: IntArray, cands: Array<Set<Int>>): XWing? {
        for (d in 1..9) {
            // Row-based: two rows where d's candidates lie in exactly the same two columns.
            val rowPos = Array(9) { r ->
                (0 until 9).filter { c -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            for (r1 in 0 until 9) {
                if (rowPos[r1].size != 2) continue
                for (r2 in r1 + 1 until 9) {
                    if (rowPos[r2] != rowPos[r1]) continue
                    val (c1, c2) = rowPos[r1]
                    val corners = setOf(r1 * 9 + c1, r1 * 9 + c2, r2 * 9 + c1, r2 * 9 + c2)
                    val plan = mutableListOf<EliminationStep>()
                    for (r in 0 until 9) {
                        if (r == r1 || r == r2) continue
                        for (c in listOf(c1, c2)) {
                            val idx = r * 9 + c
                            if (grid[idx] == 0 && d in cands[idx]) {
                                plan += EliminationStep(idx, setOf(d), corners)
                            }
                        }
                    }
                    if (plan.isNotEmpty()) return XWing(
                        keyCells = corners,
                        unitCells = ((0 until 9).map { r1 * 9 + it } +
                            (0 until 9).map { r2 * 9 + it }).toSet(),
                        plan = plan,
                        digit = d,
                    )
                }
            }
            // Column-based.
            val colPos = Array(9) { c ->
                (0 until 9).filter { r -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            for (c1 in 0 until 9) {
                if (colPos[c1].size != 2) continue
                for (c2 in c1 + 1 until 9) {
                    if (colPos[c2] != colPos[c1]) continue
                    val (r1, r2) = colPos[c1]
                    val corners = setOf(r1 * 9 + c1, r1 * 9 + c2, r2 * 9 + c1, r2 * 9 + c2)
                    val plan = mutableListOf<EliminationStep>()
                    for (c in 0 until 9) {
                        if (c == c1 || c == c2) continue
                        for (r in listOf(r1, r2)) {
                            val idx = r * 9 + c
                            if (grid[idx] == 0 && d in cands[idx]) {
                                plan += EliminationStep(idx, setOf(d), corners)
                            }
                        }
                    }
                    if (plan.isNotEmpty()) return XWing(
                        keyCells = corners,
                        unitCells = ((0 until 9).map { it * 9 + c1 } +
                            (0 until 9).map { it * 9 + c2 }).toSet(),
                        plan = plan,
                        digit = d,
                    )
                }
            }
        }
        return null
    }

    internal fun findEmptyRectangle(grid: IntArray, cands: Array<Set<Int>>): EmptyRectangle? {
        for (d in 1..9) {
            for (boxIdx in 0 until 9) {
                val boxRow0 = (boxIdx / 3) * 3
                val boxCol0 = (boxIdx % 3) * 3
                val boxRows = boxRow0..boxRow0 + 2
                val boxCols = boxCol0..boxCol0 + 2
                val boxCells = buildList {
                    for (r in boxRows) for (c in boxCols) add(r * 9 + c)
                }
                val cellsWithD = boxCells.filter { grid[it] == 0 && d in cands[it] }
                if (cellsWithD.size < 2) continue

                // Find a row R and column C inside the box such that every
                // candidate cell is either on R or on C, with at least one
                // candidate strictly in the row leg AND at least one strictly
                // in the column leg (i.e. not just a pointing pair).
                for (R in boxRows) {
                    for (C in boxCols) {
                        val outsideRC = cellsWithD.filter { it / 9 != R && it % 9 != C }
                        if (outsideRC.isNotEmpty()) continue
                        val inRow = cellsWithD.filter { it / 9 == R && it % 9 != C }
                        val inCol = cellsWithD.filter { it / 9 != R && it % 9 == C }
                        if (inRow.isEmpty() || inCol.isEmpty()) continue

                        // Type A: strong link is a column c outside the box.
                        // Matching endpoint sits on row R, other endpoint is
                        // outside the box -> eliminate d at (otherEndRow, C).
                        for (c in 0..8) {
                            if (c in boxCols) continue
                            val colCells = (0..8).filter { r ->
                                grid[r * 9 + c] == 0 && d in cands[r * 9 + c]
                            }
                            if (colCells.size != 2) continue
                            if (R !in colCells) continue
                            val freeRow = (colCells - R).first()
                            if (freeRow in boxRows) continue
                            val elim = freeRow * 9 + C
                            if (grid[elim] != 0 || d !in cands[elim]) continue

                            val conj = setOf(R * 9 + c, freeRow * 9 + c)
                            return EmptyRectangle(
                                keyCells = cellsWithD.toSet() + conj,
                                unitCells = boxCells.toSet() + (0..8).map { it * 9 + c }.toSet(),
                                plan = listOf(EliminationStep(
                                    cellIdx = elim,
                                    doomedDigits = setOf(d),
                                    explainerCells = cellsWithD.toSet() + conj,
                                )),
                                digit = d,
                            )
                        }
                        // Type B: strong link is a row r outside the box.
                        for (r in 0..8) {
                            if (r in boxRows) continue
                            val rowCells = (0..8).filter { c ->
                                grid[r * 9 + c] == 0 && d in cands[r * 9 + c]
                            }
                            if (rowCells.size != 2) continue
                            if (C !in rowCells) continue
                            val freeCol = (rowCells - C).first()
                            if (freeCol in boxCols) continue
                            val elim = R * 9 + freeCol
                            if (grid[elim] != 0 || d !in cands[elim]) continue

                            val conj = setOf(r * 9 + C, r * 9 + freeCol)
                            return EmptyRectangle(
                                keyCells = cellsWithD.toSet() + conj,
                                unitCells = boxCells.toSet() + (0..8).map { r * 9 + it }.toSet(),
                                plan = listOf(EliminationStep(
                                    cellIdx = elim,
                                    doomedDigits = setOf(d),
                                    explainerCells = cellsWithD.toSet() + conj,
                                )),
                                digit = d,
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    internal fun findSwordfish(grid: IntArray, cands: Array<Set<Int>>): Swordfish? {
        for (d in 1..9) {
            val rowPos = Array(9) { r ->
                (0 until 9).filter { c -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            val eligibleRows = (0 until 9).filter { rowPos[it].size in 2..3 }
            if (eligibleRows.size >= 3) {
                for (i in eligibleRows.indices) for (j in i + 1 until eligibleRows.size)
                    for (k in j + 1 until eligibleRows.size) {
                        val rs = listOf(eligibleRows[i], eligibleRows[j], eligibleRows[k])
                        val cols = (rowPos[rs[0]] + rowPos[rs[1]] + rowPos[rs[2]]).toSet()
                        if (cols.size != 3) continue
                        val keys = buildSet {
                            for (r in rs) for (c in cols) {
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) add(idx)
                            }
                        }
                        val plan = mutableListOf<EliminationStep>()
                        for (c in cols) {
                            for (r in 0 until 9) {
                                if (r in rs) continue
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) {
                                    plan += EliminationStep(idx, setOf(d), keys)
                                }
                            }
                        }
                        if (plan.isNotEmpty()) return Swordfish(
                            keyCells = keys,
                            unitCells = rs.flatMap { r -> (0 until 9).map { r * 9 + it } }.toSet(),
                            plan = plan,
                            digit = d,
                        )
                    }
            }
            val colPos = Array(9) { c ->
                (0 until 9).filter { r -> grid[r * 9 + c] == 0 && d in cands[r * 9 + c] }
            }
            val eligibleCols = (0 until 9).filter { colPos[it].size in 2..3 }
            if (eligibleCols.size >= 3) {
                for (i in eligibleCols.indices) for (j in i + 1 until eligibleCols.size)
                    for (k in j + 1 until eligibleCols.size) {
                        val cs = listOf(eligibleCols[i], eligibleCols[j], eligibleCols[k])
                        val rows = (colPos[cs[0]] + colPos[cs[1]] + colPos[cs[2]]).toSet()
                        if (rows.size != 3) continue
                        val keys = buildSet {
                            for (c in cs) for (r in rows) {
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) add(idx)
                            }
                        }
                        val plan = mutableListOf<EliminationStep>()
                        for (r in rows) {
                            for (c in 0 until 9) {
                                if (c in cs) continue
                                val idx = r * 9 + c
                                if (grid[idx] == 0 && d in cands[idx]) {
                                    plan += EliminationStep(idx, setOf(d), keys)
                                }
                            }
                        }
                        if (plan.isNotEmpty()) return Swordfish(
                            keyCells = keys,
                            unitCells = cs.flatMap { c -> (0 until 9).map { it * 9 + c } }.toSet(),
                            plan = plan,
                            digit = d,
                        )
                    }
            }
        }
        return null
    }

    internal fun findXYWing(grid: IntArray, cands: Array<Set<Int>>): XYWing? {
        for (pivot in 0 until 81) {
            if (grid[pivot] != 0 || cands[pivot].size != 2) continue
            val pc = cands[pivot].toList()
            val a = pc[0]; val b = pc[1]
            val peers = PEERS[pivot]
            val pincerA = mutableListOf<Int>() // {a, c}
            val pincerB = mutableListOf<Int>() // {b, c}
            for (p in peers) {
                if (grid[p] != 0 || cands[p].size != 2) continue
                val pcs = cands[p]
                if (a in pcs && b !in pcs) pincerA += p
                if (b in pcs && a !in pcs) pincerB += p
            }
            for (pa in pincerA) {
                val cA = (cands[pa] - a).firstOrNull() ?: continue
                for (pb in pincerB) {
                    val cB = (cands[pb] - b).firstOrNull() ?: continue
                    if (cA != cB) continue
                    val shared = cA
                    val seeBoth = PEERS[pa].intersect(PEERS[pb])
                    val plan = mutableListOf<EliminationStep>()
                    for (cell in seeBoth) {
                        if (cell == pivot || grid[cell] != 0) continue
                        if (shared !in cands[cell]) continue
                        plan += EliminationStep(cell, setOf(shared), setOf(pa, pb))
                    }
                    if (plan.isNotEmpty()) return XYWing(
                        keyCells = setOf(pivot, pa, pb),
                        unitCells = emptySet(),
                        plan = plan,
                        sharedDigit = shared,
                    )
                }
            }
        }
        return null
    }

    private fun boxOf(idx: Int): Int {
        val r = idx / 9; val c = idx % 9
        return (r / 3) * 3 + c / 3
    }

    private val PEERS: Array<Set<Int>> = Array(81) { idx ->
        val row = idx / 9; val col = idx % 9
        val result = mutableSetOf<Int>()
        for (c in 0 until 9) result.add(row * 9 + c)
        for (r in 0 until 9) result.add(r * 9 + col)
        val br = (row / 3) * 3; val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) result.add(r * 9 + c)
        result.remove(idx)
        result
    }

    /** Peer cells of [cellIdx] that lie outside [unitCells] and hold [digit]. */
    private fun peersOutsideUnitHolding(
        grid: IntArray,
        cellIdx: Int,
        unitCells: Set<Int>,
        digit: Int,
    ): Set<Int> {
        val row = cellIdx / 9
        val col = cellIdx % 9
        val out = mutableSetOf<Int>()
        for (c in 0 until 9) {
            if (c == col) continue
            val i = row * 9 + c
            if (i !in unitCells && grid[i] == digit) out += i
        }
        for (r in 0 until 9) {
            if (r == row) continue
            val i = r * 9 + col
            if (i !in unitCells && grid[i] == digit) out += i
        }
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) {
            for (c in bc until bc + 3) {
                if (r == row && c == col) continue
                val i = r * 9 + c
                if (i !in unitCells && grid[i] == digit) out += i
            }
        }
        return out
    }

    private val UNITS_ORDERED: List<HintUnit> = buildList {
        for (br in 0 until 3) for (bc in 0 until 3) {
            val cells = buildList {
                for (dr in 0 until 3) for (dc in 0 until 3) {
                    add((br * 3 + dr) * 9 + (bc * 3 + dc))
                }
            }
            add(HintUnit(UnitKind.Box, cells))
        }
        for (r in 0 until 9) {
            add(HintUnit(UnitKind.Row, (0 until 9).map { r * 9 + it }))
        }
        for (c in 0 until 9) {
            add(HintUnit(UnitKind.Column, (0 until 9).map { it * 9 + c }))
        }
    }

    /** All peer cells of (row,col) whose value equals [digit]. */
    private fun peersHolding(grid: IntArray, row: Int, col: Int, digit: Int): Set<Int> {
        val out = mutableSetOf<Int>()
        for (c in 0 until 9) {
            if (c == col) continue
            val i = row * 9 + c
            if (grid[i] == digit) out += i
        }
        for (r in 0 until 9) {
            if (r == row) continue
            val i = r * 9 + col
            if (grid[i] == digit) out += i
        }
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) {
            for (c in bc until bc + 3) {
                if (r == row && c == col) continue
                val i = r * 9 + c
                if (grid[i] == digit) out += i
            }
        }
        return out
    }
}
