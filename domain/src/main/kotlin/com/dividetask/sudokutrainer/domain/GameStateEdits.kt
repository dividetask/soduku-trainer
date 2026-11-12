package com.dividetask.sudokutrainer.domain

/** Small edits shared by the board-move and hint-step reducers. */

internal fun GameState.replaceCell(newCell: Cell): GameState {
    val idx = newCell.row * 9 + newCell.col
    val list = cells.toMutableList()
    list[idx] = newCell
    return copy(cells = list)
}

internal fun GameState.appendHistory(move: Move): GameState =
    copy(history = history + move)

/** Flat indices of the cells sharing a row, column or box with (row, col). */
internal fun peerIndices(row: Int, col: Int): List<Int> = BoardGeometry.PEERS[row * 9 + col].toList()

/** A key identifying one pencil mark (cell and digit), for diffing snapshots. */
internal fun markKey(m: AutoClearedMark): Int = (m.row * 9 + m.col) * 10 + m.digit
