package com.eightharsh.eightharsh.feature.settings.devoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.repository.settings.OnboardingRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.LauncherBehaviorRepository
import com.eightharsh.eightharsh.core.domain.system.LockScreenUseCase
import com.eightharsh.eightharsh.core.navigation.AppNavKey
import com.eightharsh.eightharsh.core.navigation.NavRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DevOptionsPageViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    launcherBehaviorRepository: LauncherBehaviorRepository,
    private val lockScreenUseCase: LockScreenUseCase,
    val navRouter: NavRouter
) : ViewModel() {
    val firstTimeHelp = onboardingRepository.firstTimeHelp
    fun setFirstTimeHelp(value: Boolean) {
        viewModelScope.launch {
            onboardingRepository.setFirstTimeHelp(value)
        }
    }

    val firstTime = onboardingRepository.firstTime
    fun setFirstTime(value: Boolean) {
        viewModelScope.launch {
            onboardingRepository.setFirstTime(value)
        }
    }


    val doubleTapToLock = launcherBehaviorRepository.doubleTapToLock

    fun lockScreen() {
        lockScreenUseCase()
    }

    fun requestReplayOnboarding() {
        navRouter.navigateTo(AppNavKey.Onboarding, true)
    }
}