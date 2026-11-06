package com.eightharsh.eightharsh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.repository.settings.AppearanceRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.LauncherBehaviorRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.OnboardingRepository
import com.eightharsh.eightharsh.core.domain.startup.GetAppInitializationStateUseCase
import com.eightharsh.eightharsh.core.model.AppInitializationState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GlobalViewModel @Inject constructor(
    launcherBehaviorRepository: LauncherBehaviorRepository,
    onboardingRepository: OnboardingRepository,
    appearanceRepository: AppearanceRepository,
    getAppInitializationStateUseCase: GetAppInitializationStateUseCase
) : ViewModel() {
    val firstTime = onboardingRepository.firstTime
    val showStatusBar = appearanceRepository.showStatusBar
    val showWallpaper = appearanceRepository.showWallpaper

    /**
     * The initialization state for the splash screen.
     * Combines multiple data loading signals to determine when the app is ready for interaction.
     */
    val initializationState = getAppInitializationStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppInitializationState()
        )
}
