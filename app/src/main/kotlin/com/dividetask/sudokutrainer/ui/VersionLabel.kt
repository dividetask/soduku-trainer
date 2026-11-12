package com.dividetask.sudokutrainer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.dividetask.sudokutrainer.BuildConfig

/** The app version, small and grey, so screenshots show which build they came from. */
@Composable
fun VersionLabel(modifier: Modifier = Modifier) {
    Text(
        text = BuildConfig.VERSION_NAME,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF9E9E9E),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
