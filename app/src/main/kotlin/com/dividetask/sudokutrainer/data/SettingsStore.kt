package com.dividetask.sudokutrainer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Player-facing options, chosen on the Settings screen. */
data class Settings(
    /** Run a Candidate Sweep when a game starts, filling in every note. */
    val autofillCandidates: Boolean = true,
    /** Animate how a hint reaches its answer; off applies it at once. */
    val showHintWork: Boolean = true,
    /** Reject (flash red and undo) a wrong value placed in the Solve color. */
    val preventIncorrectValues: Boolean = true,
    /** Reject a Solve-color note that contradicts the solution. */
    val preventIncorrectNotes: Boolean = true,
)

/** Persists [Settings] and whether the first-run settings screen was seen. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    /** True until the player leaves the Settings screen for the first time. */
    val isFirstRun: Boolean get() = !prefs.getBoolean(KEY_SEEN, false)

    fun update(settings: Settings) {
        prefs.edit()
            .putBoolean(KEY_AUTOFILL, settings.autofillCandidates)
            .putBoolean(KEY_SHOW_WORK, settings.showHintWork)
            .putBoolean(KEY_PREVENT_VALUES, settings.preventIncorrectValues)
            .putBoolean(KEY_PREVENT_NOTES, settings.preventIncorrectNotes)
            .apply()
        _settings.value = settings
    }

    fun markFirstRunDone() {
        prefs.edit().putBoolean(KEY_SEEN, true).apply()
    }

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            autofillCandidates = prefs.getBoolean(KEY_AUTOFILL, d.autofillCandidates),
            showHintWork = prefs.getBoolean(KEY_SHOW_WORK, d.showHintWork),
            preventIncorrectValues = prefs.getBoolean(KEY_PREVENT_VALUES, d.preventIncorrectValues),
            preventIncorrectNotes = prefs.getBoolean(KEY_PREVENT_NOTES, d.preventIncorrectNotes),
        )
    }

    private companion object {
        const val KEY_SEEN = "first_run_done"
        const val KEY_AUTOFILL = "autofill_candidates"
        const val KEY_SHOW_WORK = "show_hint_work"
        const val KEY_PREVENT_VALUES = "prevent_incorrect_values"
        const val KEY_PREVENT_NOTES = "prevent_incorrect_notes"
    }
}
