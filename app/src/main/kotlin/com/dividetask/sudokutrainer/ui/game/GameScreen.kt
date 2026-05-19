package com.dividetask.sudokutrainer.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.sudokutrainer.domain.GameState

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
                    onGiveUpOrSolve = {
                        if (viewModel.isReadyToSolve) viewModel.onSolve() else viewModel.onGiveUp()
                    },
                )
            }

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
