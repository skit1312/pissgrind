package ch.pissgrind.ui.viewmodels

import androidx.lifecycle.ViewModel
import ch.pissgrind.data.recommendation.AlgorithmLogger
import ch.pissgrind.data.recommendation.RecommendationCache
import ch.pissgrind.data.recommendation.TrainingDataManager
import ch.pissgrind.managers.settings.ExcludedAlbumsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlgorithmSettingsViewModel @Inject constructor(
    val algorithmLogger: AlgorithmLogger,
    val trainingDataManager: TrainingDataManager,
    val recommendationCache: RecommendationCache,
    val excludedAlbumsManager: ExcludedAlbumsManager
) : ViewModel()
