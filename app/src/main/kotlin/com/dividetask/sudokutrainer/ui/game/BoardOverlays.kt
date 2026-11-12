package com.dividetask.sudokutrainer.ui.game

import com.dividetask.sudokutrainer.domain.GameState
import com.dividetask.sudokutrainer.domain.HintUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short-lived board overlays: the red flash for a rejected move or a
 * missing hint, and the red highlight of mistakes. Reads and writes the
 * board through [get] / [set] (null while there is no board yet).
 */
class BoardOverlays(
    private val scope: CoroutineScope,
    private val flashMs: () -> Long,
    private val mistakesMs: () -> Long,
    private val get: () -> GameState?,
    private val set: (GameState) -> Unit,
) {
    private var mistakesJob: Job? = null

    /** Flash the board red for a rejected move. */
    fun flashError() = flash(HintUi.ErrorFlash)

    /** Flash the board red because no hint applies. */
    fun flashNoHint() = flash(HintUi.NoHintFlash)

    /** Paint [cells] red until the next board change or the timeout. */
    fun showMistakes(cells: Set<Int>) {
        val s = get() ?: return
        set(s.copy(hintUi = HintUi.Mistakes(cells)))
        mistakesJob?.cancel()
        mistakesJob = scope.launch {
            delay(mistakesMs())
            clearMistakes()
        }
    }

    /** Take down the mistake highlight, if it's showing. */
    fun clearMistakes() {
        val s = get() ?: return
        if (s.hintUi is HintUi.Mistakes) set(s.copy(hintUi = null))
    }

    private fun flash(ui: HintUi) {
        val s = get() ?: return
        set(s.copy(hintUi = ui))
        scope.launch {
            delay(flashMs())
            val now = get() ?: return@launch
            if (now.hintUi == ui) set(now.copy(hintUi = null))
        }
    }
}
