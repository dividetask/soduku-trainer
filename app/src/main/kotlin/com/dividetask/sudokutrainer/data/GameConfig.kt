package com.dividetask.sudokutrainer.data

import android.content.Context

data class GameConfig(
    val solveCriteria: Int = 10,
    val solveSpeed: Long = 500,
    val fireworkSpeed: Float = 4f,
    val fireworkDuration: Long = 5_000,
    val hintStepMs: Long = 500,
    val hintFlashMs: Long = 600,
    val sweepCellMs: Long = 150,
    val pendingHintMs: Long = 10_000,
    val showAgainMultiplier: Float = 3f,
    val showTestButton: Boolean = true,
) {
    companion object {
        fun loadFromAssets(context: Context): GameConfig {
            val text = context.assets.open("config.yaml")
                .bufferedReader().use { it.readText() }
            return parse(text)
        }

        fun parse(text: String): GameConfig {
            val map = mutableMapOf<String, String>()
            for (line in text.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
            return GameConfig(
                solveCriteria = map["solve_criteria"]?.toIntOrNull() ?: 10,
                solveSpeed = map["solve_speed"]?.toLongOrNull() ?: 500,
                fireworkSpeed = map["firework_speed"]?.toFloatOrNull() ?: 4f,
                fireworkDuration = map["firework_duration"]?.toLongOrNull() ?: 5_000,
                hintStepMs = map["hint_step_ms"]?.toLongOrNull() ?: 500,
                hintFlashMs = map["hint_flash_ms"]?.toLongOrNull() ?: 600,
                sweepCellMs = map["sweep_cell_ms"]?.toLongOrNull() ?: 150,
                pendingHintMs = map["pending_hint_ms"]?.toLongOrNull() ?: 10_000,
                showAgainMultiplier = map["show_again_multiplier"]?.toFloatOrNull() ?: 3f,
                showTestButton = map["show_test_button"]?.toBooleanStrictOrNull() ?: true,
            )
        }
    }
}
