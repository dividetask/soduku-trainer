package com.dividetask.sudokutrainer.domain

/**
 * A human-style Sudoku solver that applies logical techniques in order
 * from easiest to hardest. The hardest technique a puzzle *requires*
 * determines its difficulty level (1–9).
 *
 * Techniques by level:
 *  1 — Naked singles
 *  2 — Hidden singles
 *  3 — Naked pairs
 *  4 — Hidden pairs
 *  5 — Pointing pairs / box-line reduction
 *  6 — Naked triples / hidden triples
 *  7 — X-Wing
 *  8 — Swordfish / XY-Wing
 *  9 — Unsolvable by techniques 1–8 (requires coloring, chains, or trial)
 */
object LogicSolver {

    data class Result(
        val solved: Boolean,
        val maxLevel: Int,
    )

    fun grade(grid: IntArray): Result {
        require(grid.size == 81)
        val g = CandidateGrid(grid)
        var maxLevel = 1

        while (!g.isSolved()) {
            val level = applyNextTechnique(g)
            if (level == 0) return Result(solved = false, maxLevel = 9)
            maxLevel = maxOf(maxLevel, level)
        }
        return Result(solved = true, maxLevel = maxLevel)
    }

    /** Try techniques from level 1 upward. Return the level that made progress, or 0. */
    private fun applyNextTechnique(g: CandidateGrid): Int {
        if (nakedSingles(g)) return 1
        if (hiddenSingles(g)) return 2
        if (nakedPairs(g)) return 3
        if (hiddenPairs(g)) return 4
        if (pointingAndBoxLine(g)) return 5
        if (nakedTriples(g)) return 6
        if (hiddenTriples(g)) return 6
        if (xWing(g)) return 7
        if (swordfish(g)) return 8
        if (xyWing(g)) return 8
        return 0
    }

    // ---- Level 1: Naked Singles ----

    private fun nakedSingles(g: CandidateGrid): Boolean {
        var found = false
        for (i in 0 until 81) {
            if (g.values[i] == 0 && g.cands[i].size == 1) {
                g.place(i, g.cands[i].first())
                found = true
            }
        }
        return found
    }

    // ---- Level 2: Hidden Singles ----

    private fun hiddenSingles(g: CandidateGrid): Boolean {
        var found = false
        for (unit in UNITS) {
            for (d in 1..9) {
                val cells = unit.filter { g.values[it] == 0 && d in g.cands[it] }
                if (cells.size == 1) {
                    g.place(cells[0], d)
                    found = true
                }
            }
        }
        return found
    }

    // ---- Level 3: Naked Pairs ----

    private fun nakedPairs(g: CandidateGrid): Boolean {
        var found = false
        for (unit in UNITS) {
            val unsolved = unit.filter { g.values[it] == 0 && g.cands[it].size == 2 }
            for (i in unsolved.indices) {
                for (j in i + 1 until unsolved.size) {
                    val a = unsolved[i]; val b = unsolved[j]
                    if (g.cands[a] != g.cands[b]) continue
                    val pair = g.cands[a]
                    for (cell in unit) {
                        if (cell == a || cell == b || g.values[cell] != 0) continue
                        for (d in pair) if (g.eliminate(cell, d)) found = true
                    }
                }
            }
        }
        return found
    }

    // ---- Level 4: Hidden Pairs ----

    private fun hiddenPairs(g: CandidateGrid): Boolean {
        var found = false
        for (unit in UNITS) {
            val digits = (1..9).filter { d ->
                unit.any { g.values[it] == 0 && d in g.cands[it] }
            }
            for (i in digits.indices) {
                for (j in i + 1 until digits.size) {
                    val d1 = digits[i]; val d2 = digits[j]
                    val cells = unit.filter { g.values[it] == 0 &&
                        (d1 in g.cands[it] || d2 in g.cands[it]) }
                    val both = cells.filter { d1 in g.cands[it] && d2 in g.cands[it] }
                    if (both.size == 2 && cells.size == 2) {
                        for (cell in both) {
                            val keep = setOf(d1, d2)
                            for (d in g.cands[cell].toList()) {
                                if (d !in keep && g.eliminate(cell, d)) found = true
                            }
                        }
                    }
                }
            }
        }
        return found
    }

    // ---- Level 5: Pointing Pairs / Box-Line Reduction ----

    private fun pointingAndBoxLine(g: CandidateGrid): Boolean =
        pointingPairs(g) || boxLineReduction(g)

    private fun pointingPairs(g: CandidateGrid): Boolean {
        var found = false
        for (box in BOXES) {
            for (d in 1..9) {
                val cells = box.filter { g.values[it] == 0 && d in g.cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val rows = cells.map { it / 9 }.toSet()
                val cols = cells.map { it % 9 }.toSet()
                if (rows.size == 1) {
                    val row = rows.first()
                    for (c in 0 until 9) {
                        val idx = row * 9 + c
                        if (idx !in box && g.values[idx] == 0 && g.eliminate(idx, d)) found = true
                    }
                }
                if (cols.size == 1) {
                    val col = cols.first()
                    for (r in 0 until 9) {
                        val idx = r * 9 + col
                        if (idx !in box && g.values[idx] == 0 && g.eliminate(idx, d)) found = true
                    }
                }
            }
        }
        return found
    }

    private fun boxLineReduction(g: CandidateGrid): Boolean {
        var found = false
        for (d in 1..9) {
            for (r in 0 until 9) {
                val cells = (0 until 9).map { r * 9 + it }
                    .filter { g.values[it] == 0 && d in g.cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val boxes = cells.map { boxOf(it) }.toSet()
                if (boxes.size == 1) {
                    val box = BOXES[boxes.first()]
                    for (idx in box) {
                        if (idx !in cells && g.values[idx] == 0 && g.eliminate(idx, d)) found = true
                    }
                }
            }
            for (c in 0 until 9) {
                val cells = (0 until 9).map { it * 9 + c }
                    .filter { g.values[it] == 0 && d in g.cands[it] }
                if (cells.size < 2 || cells.size > 3) continue
                val boxes = cells.map { boxOf(it) }.toSet()
                if (boxes.size == 1) {
                    val box = BOXES[boxes.first()]
                    for (idx in box) {
                        if (idx !in cells && g.values[idx] == 0 && g.eliminate(idx, d)) found = true
                    }
                }
            }
        }
        return found
    }

    // ---- Level 6: Naked Triples / Hidden Triples ----

    private fun nakedTriples(g: CandidateGrid): Boolean {
        var found = false
        for (unit in UNITS) {
            val unsolved = unit.filter { g.values[it] == 0 && g.cands[it].size in 2..3 }
            if (unsolved.size < 3) continue
            for (i in unsolved.indices) {
                for (j in i + 1 until unsolved.size) {
                    for (k in j + 1 until unsolved.size) {
                        val union = g.cands[unsolved[i]] +
                            g.cands[unsolved[j]] + g.cands[unsolved[k]]
                        if (union.size != 3) continue
                        val triple = setOf(unsolved[i], unsolved[j], unsolved[k])
                        for (cell in unit) {
                            if (cell in triple || g.values[cell] != 0) continue
                            for (d in union) {
                                if (g.eliminate(cell, d)) found = true
                            }
                        }
                    }
                }
            }
        }
        return found
    }

    private fun hiddenTriples(g: CandidateGrid): Boolean {
        var found = false
        for (unit in UNITS) {
            val digits = (1..9).filter { d ->
                val count = unit.count { g.values[it] == 0 && d in g.cands[it] }
                count in 2..3
            }
            if (digits.size < 3) continue
            for (i in digits.indices) {
                for (j in i + 1 until digits.size) {
                    for (k in j + 1 until digits.size) {
                        val ds = setOf(digits[i], digits[j], digits[k])
                        val cells = unit.filter { idx ->
                            g.values[idx] == 0 && g.cands[idx].any { it in ds }
                        }
                        if (cells.size != 3) continue
                        for (cell in cells) {
                            for (d in g.cands[cell].toList()) {
                                if (d !in ds && g.eliminate(cell, d)) found = true
                            }
                        }
                    }
                }
            }
        }
        return found
    }

    // ---- Level 7: X-Wing ----

    private fun xWing(g: CandidateGrid): Boolean {
        var found = false
        for (d in 1..9) {
            // Row-based X-Wing
            val rowPositions = Array(9) { r ->
                (0 until 9).filter { c -> g.values[r * 9 + c] == 0 && d in g.cands[r * 9 + c] }
            }
            for (r1 in 0 until 9) {
                if (rowPositions[r1].size != 2) continue
                for (r2 in r1 + 1 until 9) {
                    if (rowPositions[r2] != rowPositions[r1]) continue
                    val (c1, c2) = rowPositions[r1]
                    for (r in 0 until 9) {
                        if (r == r1 || r == r2) continue
                        if (g.eliminate(r * 9 + c1, d)) found = true
                        if (g.eliminate(r * 9 + c2, d)) found = true
                    }
                }
            }
            // Column-based X-Wing
            val colPositions = Array(9) { c ->
                (0 until 9).filter { r -> g.values[r * 9 + c] == 0 && d in g.cands[r * 9 + c] }
            }
            for (c1 in 0 until 9) {
                if (colPositions[c1].size != 2) continue
                for (c2 in c1 + 1 until 9) {
                    if (colPositions[c2] != colPositions[c1]) continue
                    val (r1, r2) = colPositions[c1]
                    for (c in 0 until 9) {
                        if (c == c1 || c == c2) continue
                        if (g.eliminate(r1 * 9 + c, d)) found = true
                        if (g.eliminate(r2 * 9 + c, d)) found = true
                    }
                }
            }
        }
        return found
    }

    // ---- Level 8: Swordfish ----

    private fun swordfish(g: CandidateGrid): Boolean {
        var found = false
        for (d in 1..9) {
            // Row-based Swordfish
            val rowPos = Array(9) { r ->
                (0 until 9).filter { c -> g.values[r * 9 + c] == 0 && d in g.cands[r * 9 + c] }
            }
            val eligible = (0 until 9).filter { rowPos[it].size in 2..3 }
            if (eligible.size >= 3) {
                for (i in eligible.indices) {
                    for (j in i + 1 until eligible.size) {
                        for (k in j + 1 until eligible.size) {
                            val cols = (rowPos[eligible[i]] + rowPos[eligible[j]] +
                                rowPos[eligible[k]]).toSet()
                            if (cols.size != 3) continue
                            val rows = setOf(eligible[i], eligible[j], eligible[k])
                            for (c in cols) {
                                for (r in 0 until 9) {
                                    if (r in rows) continue
                                    if (g.eliminate(r * 9 + c, d)) found = true
                                }
                            }
                        }
                    }
                }
            }
            // Column-based Swordfish
            val colPos = Array(9) { c ->
                (0 until 9).filter { r -> g.values[r * 9 + c] == 0 && d in g.cands[r * 9 + c] }
            }
            val eligibleC = (0 until 9).filter { colPos[it].size in 2..3 }
            if (eligibleC.size >= 3) {
                for (i in eligibleC.indices) {
                    for (j in i + 1 until eligibleC.size) {
                        for (k in j + 1 until eligibleC.size) {
                            val rows = (colPos[eligibleC[i]] + colPos[eligibleC[j]] +
                                colPos[eligibleC[k]]).toSet()
                            if (rows.size != 3) continue
                            val cols = setOf(eligibleC[i], eligibleC[j], eligibleC[k])
                            for (r in rows) {
                                for (c in 0 until 9) {
                                    if (c in cols) continue
                                    if (g.eliminate(r * 9 + c, d)) found = true
                                }
                            }
                        }
                    }
                }
            }
        }
        return found
    }

    // ---- Level 8: XY-Wing ----

    private fun xyWing(g: CandidateGrid): Boolean {
        var found = false
        for (pivot in 0 until 81) {
            if (g.values[pivot] != 0 || g.cands[pivot].size != 2) continue
            val (a, b) = g.cands[pivot].toList()
            val peers = PEERS[pivot]
            // Find pincers: cells that see the pivot, have 2 candidates,
            // and share exactly one candidate with the pivot.
            val pincerA = mutableListOf<Int>() // cells with candidates {A, C}
            val pincerB = mutableListOf<Int>() // cells with candidates {B, C}
            for (peer in peers) {
                if (g.values[peer] != 0 || g.cands[peer].size != 2) continue
                val pc = g.cands[peer]
                if (a in pc && b !in pc) pincerA += peer
                if (b in pc && a !in pc) pincerB += peer
            }
            for (pa in pincerA) {
                val c1 = (g.cands[pa] - a).firstOrNull() ?: continue
                for (pb in pincerB) {
                    val c2 = (g.cands[pb] - b).firstOrNull() ?: continue
                    if (c1 != c2) continue
                    val c = c1
                    // Eliminate c from cells that see both pincers.
                    val seeBoth = PEERS[pa].intersect(PEERS[pb].toSet())
                    for (cell in seeBoth) {
                        if (cell == pivot || g.values[cell] != 0) continue
                        if (g.eliminate(cell, c)) found = true
                    }
                }
            }
        }
        return found
    }

    // ---- Candidate grid ----

    private class CandidateGrid(grid: IntArray) {
        val values = grid.copyOf()
        val cands = Array(81) { mutableSetOf<Int>() }

        init {
            for (i in 0 until 81) {
                if (values[i] != 0) continue
                val row = i / 9; val col = i % 9
                for (d in 1..9) {
                    if (SudokuSolver.isValidPlacement(values, row, col, d)) {
                        cands[i].add(d)
                    }
                }
            }
        }

        fun isSolved(): Boolean = values.none { it == 0 }

        fun place(idx: Int, digit: Int) {
            values[idx] = digit
            cands[idx].clear()
            for (peer in PEERS[idx]) cands[peer].remove(digit)
        }

        fun eliminate(idx: Int, digit: Int): Boolean = cands[idx].remove(digit)
    }

    // ---- Precomputed tables ----

    private val ROWS: List<List<Int>> = (0 until 9).map { r ->
        (0 until 9).map { c -> r * 9 + c }
    }
    private val COLS: List<List<Int>> = (0 until 9).map { c ->
        (0 until 9).map { r -> r * 9 + c }
    }
    private val BOXES: List<List<Int>> = buildList {
        for (br in 0 until 3) for (bc in 0 until 3) {
            add(buildList {
                for (dr in 0 until 3) for (dc in 0 until 3) {
                    add((br * 3 + dr) * 9 + bc * 3 + dc)
                }
            })
        }
    }
    private val UNITS: List<List<Int>> = ROWS + COLS + BOXES

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

    private fun boxOf(idx: Int): Int {
        val r = idx / 9; val c = idx % 9
        return (r / 3) * 3 + c / 3
    }
}
