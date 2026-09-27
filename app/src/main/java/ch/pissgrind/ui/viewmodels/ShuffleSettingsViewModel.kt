package ch.pissgrind.ui.viewmodels

import androidx.lifecycle.ViewModel
import ch.pissgrind.managers.settings.AudioSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShuffleSettingsViewModel @Inject constructor(
    val audioSettingsManager: AudioSettingsManager
) : ViewModel()
