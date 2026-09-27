package ch.pissgrind.data.recommendation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TrainingDataEvent {
    private val _changed = MutableStateFlow(0)
    val changed: StateFlow<Int> = _changed.asStateFlow()

    fun markChanged() {
        _changed.value++
    }
}
