package ch.pissgrind.data.recommendation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared training mode state.
 *
 * Two modes:
 * Temporary: activated by "Train" button, shows feedback during that session
 * Persistent: activated in Settings, shows feedback during ALL playback
 */
object TrainingModeManager {

    // Persistent toggle (from Settings)
    private val _isPersistentTraining = MutableStateFlow(false)
    val isPersistentTraining: StateFlow<Boolean> = _isPersistentTraining.asStateFlow()

    // Temporary flag: active training playlist (from Train button)
    private val _isTrainingPlaylist = MutableStateFlow(false)
    val isTrainingPlaylist: StateFlow<Boolean> = _isTrainingPlaylist.asStateFlow()

    /**
     * Start temporary training mode -> Train button on HomeScreen.
     */
    fun startTrainingPlaylist() {
        _isTrainingPlaylist.value = true
    }

    /**
     * Stop temporary training mode.
     */
    fun stopTrainingPlaylist() {
        _isTrainingPlaylist.value = false
    }
    /**
     * Enable/disable persistent training mode.
     */
    fun setPersistentTraining(enabled: Boolean) {
        _isPersistentTraining.value = enabled
    }

    fun savePersistentTraining(context: Context, enabled: Boolean) {
        _isPersistentTraining.value = enabled
        context.getSharedPreferences("pissgrind_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("persistent_training", enabled)
            .apply()
    }

    /**
     * Whether feedback buttons should be shown.
     * True if either temporary or persistent training is active.
     */
//    fun shouldShowFeedback(): Boolean {
//        return _isTrainingMode.value || _isPersistentTraining.value
//    }

    /**
     * Initialize persistent setting from SharedPreferences.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences("pissgrind_prefs", Context.MODE_PRIVATE)
        _isPersistentTraining.value = prefs.getBoolean("persistent_training", false)
    }
}

