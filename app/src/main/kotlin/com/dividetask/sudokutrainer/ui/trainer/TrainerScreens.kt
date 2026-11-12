package com.dividetask.sudokutrainer.ui.trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintUi
import com.dividetask.sudokutrainer.domain.TrainerJudge
import com.dividetask.sudokutrainer.domain.TrainerMethod
import com.dividetask.sudokutrainer.domain.pluralName
import com.dividetask.sudokutrainer.ui.game.Board
import com.dividetask.sudokutrainer.ui.game.ColorPickerRow
import com.dividetask.sudokutrainer.ui.game.DigitRow
import com.dividetask.sudokutrainer.ui.game.HintButton
import com.dividetask.sudokutrainer.ui.VersionLabel
import com.dividetask.sudokutrainer.ui.game.TopBar

/** One button per solving method. */
@Composable
fun TrainerMenuScreen(
    onMethod: (TrainerMethod) -> Unit,
    onMainMenu: () -> Unit,
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(label = "Trainer", onMainMenu = onMainMenu)
            TrainerMethod.entries.forEach { method ->
                Button(onClick = { onMethod(method) }, modifier = Modifier.fillMaxWidth()) {
                    Text(method.techniqueName, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun TrainerScreen(
    viewModel: TrainerViewModel,
    onMainMenu: () -> Unit,
    onMethod: (TrainerMethod) -> Unit,
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val solvedByHint by viewModel.solvedByHint.collectAsStateWithLifecycle()
    val method = viewModel.method

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBar(
                label = trainerLabel(status, state, method, state?.let(viewModel::progress), solvedByHint),
                onMainMenu = onMainMenu,
            )
            Text(method.techniqueName, style = MaterialTheme.typography.headlineSmall)
            Text(method.explanation, style = MaterialTheme.typography.bodyMedium)
            MethodStepper(method = method, onMethod = onMethod)

            val s = state
            when {
                status == TrainerViewModel.Status.NotFound -> {
                    Text("Couldn't find a puzzle for this method.", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = viewModel::nextDrill) { Text("Try again") }
                }
                s == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                else -> {
                    Board(
                        state = s,
                        onCellTap = viewModel::onCellTapped,
                        onCellDoubleTap = viewModel::onCellDoubleTapped,
                    )
                    ColorPickerRow(
                        pencilOn = s.pencilOn,
                        activeColor = s.activeColor,
                        onPencilToggled = viewModel::onPencilToggled,
                        onColorSelected = viewModel::onColorSelected,
                    )
                    DigitRow(state = s, onDigitSelected = viewModel::onDigitSelected)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val playing = status == TrainerViewModel.Status.Playing
                        OutlinedButton(
                            onClick = viewModel::onUndo,
                            // Also works during the pause after a solve.
                            enabled = status != TrainerViewModel.Status.Loading &&
                                s.phase == GameState.Phase.Playing && s.history.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Undo")
                        }
                        HintButton(
                            state = s,
                            enabled = playing && s.phase != GameState.Phase.AutoSolving,
                            onHint = viewModel::onHint,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    VersionLabel()
                }
            }
        }
    }
}

/** Previous / next method in the trainer list, which runs easiest to hardest. */
@Composable
private fun MethodStepper(method: TrainerMethod, onMethod: (TrainerMethod) -> Unit) {
    val methods = TrainerMethod.entries
    val prev = methods.getOrNull(method.ordinal - 1)
    val next = methods.getOrNull(method.ordinal + 1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedButton(
            onClick = { prev?.let(onMethod) },
            enabled = prev != null,
            modifier = Modifier.weight(1f),
        ) {
            Text("← ${prev?.techniqueName ?: "Previous"}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
            onClick = { next?.let(onMethod) },
            enabled = next != null,
            modifier = Modifier.weight(1f),
        ) {
            Text("${next?.techniqueName ?: "Next"} →", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** e.g. "3/7 candidates\n1/2 Pointing Pairs", or "2/9 Hidden Singles". */
private fun progressText(p: TrainerJudge.Progress, method: TrainerMethod): String {
    val (done, total) = p.applications
    val apps = "$done/$total ${if (total == 1) method.techniqueName else method.pluralName}"
    val notes = p.notes ?: return apps
    return "${notes.first}/${notes.second} candidates\n$apps"
}

private fun trainerLabel(
    status: TrainerViewModel.Status,
    state: GameState?,
    method: TrainerMethod,
    progress: TrainerJudge.Progress?,
    solvedByHint: Boolean,
): String = when {
    status == TrainerViewModel.Status.Solved && solvedByHint -> "Solved by hint. Undo to look again"
    status == TrainerViewModel.Status.Solved -> "Correct! Next puzzle…"
    status == TrainerViewModel.Status.Loading -> "Finding a puzzle…"
    state?.hintUi == HintUi.ErrorFlash -> "Not a ${method.techniqueName}"
    state?.pendingHint != null -> "Look at the highlighted cells"
    progress != null -> progressText(progress, method)
    else -> ""
}
