package com.dividetask.sudokutrainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dividetask.sudokutrainer.domain.CellColor
import com.dividetask.sudokutrainer.domain.GuessColor

@Composable
fun SudokuTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF0D47A1),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF455A64),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFFF8F9FB),
        onBackground = Color(0xFF111418),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111418),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

fun GuessColor.toColor(): Color = when (this) {
    GuessColor.Blue -> Color(0xFF1565C0)
    GuessColor.Red -> Color(0xFFC62828)
    GuessColor.Green -> Color(0xFF2E7D32)
    GuessColor.Orange -> Color(0xFFEF6C00)
    GuessColor.Purple -> Color(0xFF6A1B9A)
    GuessColor.Yellow -> Color(0xFFF9A825)
}

fun CellColor.toColor(): Color = when (this) {
    is CellColor.Given -> Color.Black
    is CellColor.Solve -> Color(0xFF78909C)
    is CellColor.Guess -> color.toColor()
}
