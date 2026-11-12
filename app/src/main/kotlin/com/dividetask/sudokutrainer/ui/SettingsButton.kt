package com.dividetask.sudokutrainer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The gear icon that opens Settings; on every screen but Settings itself. */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings")
    }
}
