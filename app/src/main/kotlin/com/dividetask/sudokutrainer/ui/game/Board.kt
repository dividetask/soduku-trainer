package com.dividetask.sudokutrainer.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dividetask.sudokutrainer.domain.Cell
import com.dividetask.sudokutrainer.domain.CellColor
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintUi
import com.dividetask.sudokutrainer.ui.theme.toColor

/**
 * The 9x9 Sudoku board. Renders cell values, pencil marks at fixed 3x3
 * positions, and highlights every occurrence of the active digit.
 *
 * Uses a single Canvas for the whole board so sizing is predictable and
 * we can draw crisp gridlines.
 */
@Composable
fun Board(
    state: GameState,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDoubleTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hint = state.hintUi
    val gridBg = when (hint) {
        HintUi.NoHintFlash -> Color(0xFFFFCDD2) // red flash
        else -> Color.White
    }
    val gridLine = Color(0xFFB0BEC5)
    val gridLineThick = Color(0xFF263238)
    val highlight = Color(0xFFFFF59D) // pale yellow
    val targetBg = Color(0xFFA5D6A7) // hint target: green
    val eliminatorBg = Color(0xFFFFAB91) // hint eliminator peer: light red
    val unitBg = Color(0xFFE3F2FD) // hint unit frame: very pale blue
    val currentBg = Color(0xFFFFE082) // hint sweep cursor: amber
    val pencilColor = Color(0xFF607D8B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(gridBg)
            .pointerInput(Unit) {
                // Manual double-tap detection so single taps fire instantly
                // (no 300ms delay waiting for a possible second tap).
                var lastTapTime = 0L
                var lastTapCell: Pair<Int, Int>? = null
                detectTapGestures(
                    onTap = { off ->
                        val cell = locateCell(off, size.width.toFloat(), size.height.toFloat())
                            ?: return@detectTapGestures
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300L && cell == lastTapCell) {
                            onCellDoubleTap(cell.first, cell.second)
                            lastTapTime = 0L
                            lastTapCell = null
                        } else {
                            onCellTap(cell.first, cell.second)
                            lastTapTime = now
                            lastTapCell = cell
                        }
                    },
                )
            }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val cellW = w / 9f
                val cellH = h / 9f
                val thin = with(density) { 1.dp.toPx() }
                val thick = with(density) { 2.dp.toPx() }

                val valueTextSize = cellH * 0.66f
                val pencilTextSize = cellH * 0.26f

                val valuePaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = valueTextSize
                    typeface = android.graphics.Typeface.DEFAULT
                }
                val pencilPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = pencilTextSize
                    color = pencilColor.toArgb()
                }

                onDrawBehind {
                    // Highlight every cell whose value equals the active
                    // digit. (Pencil-mark highlighting happens inline
                    // with the mark-drawing loop below.)
                    val active = state.activeDigit
                    if (active != null) {
                        for (i in 0 until 81) {
                            val cell = state.cells[i]
                            if (cell.value == active) {
                                drawRect(
                                    color = highlight,
                                    topLeft = Offset(cell.col * cellW, cell.row * cellH),
                                    size = Size(cellW, cellH),
                                )
                            }
                        }
                    }

                    // Progressive-hint key-cell highlight (stage 2 only,
                    // and only when no active animation overlay is
                    // competing for the same cells).
                    val pending = state.pendingHint
                    if (hint == null && pending != null && pending.showCells) {
                        for (idx in pending.keyCells) {
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = targetBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                    }

                    // Hint overlays: green target + red eliminator peers.
                    if (hint is HintUi.NakedSingle) {
                        for (idx in hint.eliminatorCells) {
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = eliminatorBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        val tr = hint.targetCell / 9
                        val tc = hint.targetCell % 9
                        drawRect(
                            color = targetBg,
                            topLeft = Offset(tc * cellW, tr * cellH),
                            size = Size(cellW, cellH),
                        )
                    }

                    // Generic elimination-hint overlay (pairs, triples,
                    // pointing pair, X-Wing, etc.). Order: unit frame
                    // (faint), explainer peers (red), current cell
                    // (amber), key cells (green) — green stays on top.
                    if (hint is HintUi.Elimination) {
                        for (idx in hint.unitCells) {
                            if (idx in hint.keyCells) continue
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = unitBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        for (idx in hint.explainerCells) {
                            if (idx in hint.keyCells) continue
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = eliminatorBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        val cur = hint.currentCell
                        if (cur != null && cur !in hint.keyCells) {
                            val r = cur / 9; val c = cur % 9
                            drawRect(
                                color = currentBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        for (idx in hint.keyCells) {
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = targetBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                    }

                    // Note-single overlay: just the green target cell.
                    if (hint is HintUi.NoteSingle) {
                        val r = hint.targetCell / 9
                        val c = hint.targetCell % 9
                        drawRect(
                            color = targetBg,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH),
                        )
                    }

                    // Candidate-sweep overlay: amber cursor + red
                    // eliminator peers for the cell being pruned.
                    if (hint is HintUi.CandidateSweep) {
                        for (idx in hint.eliminatorCells) {
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = eliminatorBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        val cur = hint.currentCell
                        if (cur != null) {
                            val r = cur / 9; val c = cur % 9
                            drawRect(
                                color = currentBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                    }

                    // Hidden-single overlay: unit frame, current cell,
                    // outside-unit eliminator peers, and target. Drawn in
                    // order so the target stays on top.
                    if (hint is HintUi.HiddenSingle) {
                        for (idx in hint.unitCells) {
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = unitBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        for (idx in hint.eliminatorCells) {
                            val r = idx / 9; val c = idx % 9
                            drawRect(
                                color = eliminatorBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        val cur = hint.currentCell
                        if (cur != null) {
                            val r = cur / 9; val c = cur % 9
                            drawRect(
                                color = currentBg,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW, cellH),
                            )
                        }
                        val tr = hint.targetCell / 9
                        val tc = hint.targetCell % 9
                        drawRect(
                            color = targetBg,
                            topLeft = Offset(tc * cellW, tr * cellH),
                            size = Size(cellW, cellH),
                        )
                    }

                    // Grid lines: thin for every cell, thick for the 3x3 boxes.
                    for (i in 0..9) {
                        val isThick = i % 3 == 0
                        val stroke = if (isThick) thick else thin
                        val color = if (isThick) gridLineThick else gridLine
                        // vertical
                        drawRect(
                            color = color,
                            topLeft = Offset(i * cellW - stroke / 2f, 0f),
                            size = Size(stroke, h),
                        )
                        // horizontal
                        drawRect(
                            color = color,
                            topLeft = Offset(0f, i * cellH - stroke / 2f),
                            size = Size(w, stroke),
                        )
                    }

                    // Draw values and pencil marks.
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        for (i in 0 until 81) {
                            val cell = state.cells[i]
                            val cx = cell.col * cellW + cellW / 2f
                            val cy = cell.row * cellH + cellH / 2f
                            val cellRect = Rect(
                                offset = Offset(cell.col * cellW, cell.row * cellH),
                                size = Size(cellW, cellH),
                            )
                            if (cell.value != null) {
                                valuePaint.color = cell.valueColor!!.toColor().toArgb()
                                // Vertically center the glyph.
                                val baseline = cy - (valuePaint.ascent() + valuePaint.descent()) / 2f
                                native.drawText(
                                    cell.value.toString(),
                                    cx,
                                    baseline,
                                    valuePaint,
                                )
                            } else if (cell.pencilMarks.isNotEmpty()) {
                                drawPencilMarks(
                                    native = native,
                                    cellRect = cellRect,
                                    cell = cell,
                                    active = state.activeDigit,
                                    highlight = highlight,
                                    pencilPaint = pencilPaint,
                                )
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Drawing happens via drawWithCache above.
    }
}

/** Draw pencil marks for one cell at their fixed 3x3 positions. */
private fun drawPencilMarks(
    native: android.graphics.Canvas,
    cellRect: Rect,
    cell: Cell,
    active: Int?,
    highlight: Color,
    pencilPaint: android.graphics.Paint,
) {
    val subW = cellRect.width / 3f
    val subH = cellRect.height / 3f
    for (digit in 1..9) {
        val markColor = cell.pencilMarks[digit] ?: continue
        val slotRow = (digit - 1) / 3
        val slotCol = (digit - 1) % 3
        val slotRect = Rect(
            offset = Offset(
                cellRect.left + slotCol * subW,
                cellRect.top + slotRow * subH,
            ),
            size = Size(subW, subH),
        )
        // If this digit matches the active digit, tint its slot.
        if (active == digit) {
            val bg = android.graphics.Paint().apply {
                color = highlight.toArgb()
                style = android.graphics.Paint.Style.FILL
            }
            native.drawRect(
                slotRect.left, slotRect.top, slotRect.right, slotRect.bottom, bg,
            )
        }
        pencilPaint.color = markColor.toColor().toArgb()
        val tx = slotRect.left + slotRect.width / 2f
        val ty = slotRect.top + slotRect.height / 2f -
            (pencilPaint.ascent() + pencilPaint.descent()) / 2f
        native.drawText(digit.toString(), tx, ty, pencilPaint)
    }
}

/** Map a tap offset (pixels) to a (row, col), or null if out of bounds. */
private fun locateCell(offset: Offset, width: Float, height: Float): Pair<Int, Int>? {
    if (offset.x < 0f || offset.y < 0f || offset.x >= width || offset.y >= height) return null
    val col = (offset.x / (width / 9f)).toInt().coerceIn(0, 8)
    val row = (offset.y / (height / 9f)).toInt().coerceIn(0, 8)
    return row to col
}
