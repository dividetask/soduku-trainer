package com.dividetask.sudokutrainer.data

import android.content.Context

data class GameConfig(
    val solveCriteria: Int = 10,
    val solveSpeed: Long = 500,
    val fireworkSpeed: Float = 4f,
    val fireworkDuration: Long = 120_000,
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
                fireworkDuration = map["firework_duration"]?.toLongOrNull() ?: 120_000,
            )
        }
    }
}
