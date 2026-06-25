package com.dividetask.sudokutrainer.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.GuessColor
import com.dividetask.sudokutrainer.ui.theme.toColor

@Composable
fun DigitRow(
    state: GameState,
    onDigitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (d in 1..9) {
            val isActive = state.activeDigit == d
            val isExhausted = state.isDigitExhausted(d)
            val bg = when {
                isActive -> MaterialTheme.colorScheme.primary
                isExhausted -> Color(0xFFE0E0E0)
                else -> MaterialTheme.colorScheme.surface
            }
            val fg = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                isExhausted -> Color(0xFF9E9E9E)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onDigitSelected(d) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = d.toString(),
                    color = fg,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/**
 * Single row: [Pencil] [Solve] [color] [color] [color] [color] [color]
 *
 * Blue is rendered as a square labeled "Solve" (not a circle swatch).
 * The remaining 5 guess colors are rendered as circle swatches.
 */
@Composable
fun ColorPickerRow(
    pencilOn: Boolean,
    activeColor: GuessColor,
    onPencilToggled: () -> Unit,
    onColorSelected: (GuessColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Pencil toggle.
        val pencilBg = if (pencilOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
        val pencilFg = if (pencilOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(pencilBg)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    RoundedCornerShape(8.dp),
                )
                .clickable { onPencilToggled() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✎", color = pencilFg, style = MaterialTheme.typography.titleMedium)
        }

        // "Solve" button (Blue) — displayed as a square, not a circle.
        val solveActive = activeColor == GuessColor.Blue
        val solveBg = if (solveActive) GuessColor.Blue.toColor() else MaterialTheme.colorScheme.surface
        val solveFg = if (solveActive) Color.White else GuessColor.Blue.toColor()
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(solveBg)
                .border(
                    BorderStroke(
                        width = if (solveActive) 2.dp else 1.dp,
                        color = if (solveActive) GuessColor.Blue.toColor() else MaterialTheme.colorScheme.outline,
                    ),
                    RoundedCornerShape(6.dp),
                )
                .clickable { onColorSelected(GuessColor.Blue) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Solve",
                color = solveFg,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
            )
        }

        // Guess color swatches (circles).
        GuessColor.GUESS_ONLY.forEach { color ->
            val isActive = color == activeColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(color.toColor())
                    .border(
                        BorderStroke(
                            width = if (isActive) 3.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                        ),
                        CircleShape,
                    )
                    .clickable { onColorSelected(color) },
            )
        }
    }
}

@Composable
fun ActionRow(
    state: GameState,
    isSolveReady: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSweep: () -> Unit,
    onHint: () -> Unit,
    onShowAgain: () -> Unit,
    onGiveUpOrSolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canUndo = state.history.isNotEmpty() && state.phase == GameState.Phase.Playing
    val busy = state.phase != GameState.Phase.Playing
    val playing = state.phase == GameState.Phase.Playing
    // The hint button stays enabled while a hint is animating so pressing
    // it again skips the animation to the final placement.
    val canHint = playing || state.phase == GameState.Phase.Hinting
    val canShowAgain = playing && state.lastHint != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.weight(1f),
            ) {
                Text("Undo")
            }
            Button(
                onClick = onSweep,
                enabled = playing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                ),
            ) {
                Text("Sweep")
            }
            Button(
                onClick = onHint,
                enabled = canHint,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                ),
            ) {
                Text(if (state.phase == GameState.Phase.Hinting) "Skip" else "Hint")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(
                onClick = onShowAgain,
                enabled = canShowAgain,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A1B9A),
                ),
            ) {
                Text("Show Again")
            }
            Button(
                onClick = onClear,
                enabled = playing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Clear")
            }
            Button(
                onClick = onGiveUpOrSolve,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                colors = if (isSolveReady) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    )
                },
            ) {
                Text(if (isSolveReady) "Solve" else "Give Up")
            }
        }
    }
}
