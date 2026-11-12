package com.dividetask.sudokutrainer.ui.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The board a play screen shows: one place to read and replace it, observed by the UI. */
class BoardStore<T>(initial: T) {
    private val _flow = MutableStateFlow(initial)
    val flow: StateFlow<T> = _flow.asStateFlow()

    var value: T
        get() = _flow.value
        set(v) {
            _flow.value = v
        }
}
