package com.dividetask.sudokutrainer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.sudokutrainer.data.Settings
import com.dividetask.sudokutrainer.data.SettingsStore
import com.dividetask.sudokutrainer.domain.Difficulty

@Composable
fun SettingsScreen(
    store: SettingsStore,
    onDone: () -> Unit,
) {
    val settings by store.settings.collectAsStateWithLifecycle()
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Text("Puzzle difficulty", style = MaterialTheme.typography.titleLarge)
            // No default: on first launch the player picks one before Done.
            Difficulty.entries.forEach { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { store.update(settings.copy(difficulty = d)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.difficulty == d,
                        onClick = { store.update(settings.copy(difficulty = d)) },
                    )
                    Text("${d.level}. ${d.label}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text("Options", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            SettingRow(
                title = "Autofill starting candidates",
                detail = "Start each game as if Sweep were pressed, filling in every cell's notes.",
                checked = settings.autofillCandidates,
            ) { store.update(settings.copy(autofillCandidates = it)) }
            SettingRow(
                title = "Show work when giving hint",
                detail = "Animate how a hint reaches its answer. When off, the hint is applied at once.",
                checked = settings.showHintWork,
            ) { store.update(settings.copy(showHintWork = it)) }
            SettingRow(
                title = "Prevent incorrect cell values",
                detail = "Flash red and undo a wrong value placed with Solve. Colored guesses are still allowed.",
                checked = settings.preventIncorrectValues,
            ) { store.update(settings.copy(preventIncorrectValues = it)) }
            SettingRow(
                title = "Prevent incorrect notes",
                detail = "Flash red and undo a Solve note that removes a cell's answer, or adds a digit " +
                    "already placed in its row, column or box. Colored notes are still allowed.",
                checked = settings.preventIncorrectNotes,
            ) { store.update(settings.copy(preventIncorrectNotes = it)) }
            if (settings.difficulty == null) {
                Text("Pick a puzzle difficulty to continue.", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onDone,
                enabled = settings.difficulty != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text("Done", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
