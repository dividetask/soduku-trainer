package com.dividetask.sudokutrainer.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintUi

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                HintLabel(state)
                Board(
                    state = state,
                    onCellTap = viewModel::onCellTapped,
                    onCellDoubleTap = viewModel::onCellDoubleTapped,
                )
                ColorPickerRow(
                    pencilOn = state.pencilOn,
                    activeColor = state.activeColor,
                    onPencilToggled = viewModel::onPencilToggled,
                    onColorSelected = viewModel::onColorSelected,
                )
                DigitRow(
                    state = state,
                    onDigitSelected = viewModel::onDigitSelected,
                )
                ActionRow(
                    state = state,
                    isSolveReady = viewModel.isReadyToSolve,
                    onUndo = viewModel::onUndo,
                    onClear = viewModel::onClear,
                    onSweep = viewModel::onSweep,
                    onHint = viewModel::onHint,
                    onShowAgain = viewModel::onShowAgain,
                    onGiveUpOrSolve = {
                        if (viewModel.isReadyToSolve) viewModel.onSolve() else viewModel.onGiveUp()
                    },
                )
            }

            // (overlays below)
            if (state.phase == GameState.Phase.Celebrating) {
                CelebrationOverlay(
                    speedMultiplier = viewModel.gameConfig.fireworkSpeed,
                    durationMs = viewModel.gameConfig.fireworkDuration,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun HintLabel(state: GameState) {
    val pending = state.pendingHint
    val ui = state.hintUi
    val text = when (ui) {
        is HintUi.NakedSingle -> "Naked Single"
        is HintUi.HiddenSingle -> "Hidden Single"
        is HintUi.NoteSingle -> "Note Single"
        is HintUi.CandidateSweep -> "Candidate Sweep"
        is HintUi.Elimination -> ui.techniqueName
        HintUi.NoHintFlash -> "No hint available"
        null -> pending?.techniqueName.orEmpty()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
