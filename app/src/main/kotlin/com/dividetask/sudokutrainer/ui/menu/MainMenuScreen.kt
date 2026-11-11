package com.dividetask.sudokutrainer.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dividetask.sudokutrainer.domain.Difficulty

@Composable
fun MainMenuScreen(
    canResume: Boolean,
    onTrainer: () -> Unit,
    onResume: () -> Unit,
    onStartGame: (Difficulty) -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sudoku Trainer",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Button(
                onClick = onTrainer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
            ) {
                Text("Trainer", style = MaterialTheme.typography.titleMedium)
            }
            if (canResume) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                ) {
                    Text("Resume", style = MaterialTheme.typography.titleMedium)
                }
            }
            Difficulty.entries.forEach { difficulty ->
                Button(
                    onClick = { onStartGame(difficulty) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${difficulty.level}. ${difficulty.label}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Settings", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
