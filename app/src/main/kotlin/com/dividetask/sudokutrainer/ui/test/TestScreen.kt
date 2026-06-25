package com.dividetask.sudokutrainer.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.sudokutrainer.ui.game.Board

@Composable
fun TestScreen(
    viewModel: TestViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val exit by viewModel.exitSignal.collectAsStateWithLifecycle()

    LaunchedEffect(exit) {
        if (exit) onExit()
    }

    Scaffold { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val case = viewModel.cases.getOrNull(index)
                Text(
                    text = if (case != null) {
                        "${index + 1} of ${viewModel.cases.size} — ${case.title}"
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Board(
                    state = state,
                    onCellTap = { _, _ -> },
                    onCellDoubleTap = { _, _ -> },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = viewModel::onSkip,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Skip")
                    }
                    Button(
                        onClick = viewModel::onExit,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Exit")
                    }
                }
            }
        }
    }
}
