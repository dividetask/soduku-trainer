package com.dividetask.sudokutrainer.domain

import com.dividetask.sudokutrainer.domain.Hint.HintUnit
import com.dividetask.sudokutrainer.domain.Hint.UnitKind

/** Rows, columns, boxes and peers of the 9×9 board, shared by the finders. */
internal object BoardGeometry {
    internal fun boxOf(idx: Int): Int {
        val r = idx / 9; val c = idx % 9
        return (r / 3) * 3 + c / 3
    }

    internal val PEERS: Array<Set<Int>> = Array(81) { idx ->
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
    internal fun peersOutsideUnitHolding(
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

    internal val UNITS_ORDERED: List<HintUnit> = buildList {
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
    internal fun peersHolding(grid: IntArray, row: Int, col: Int, digit: Int): Set<Int> {
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
