package com.eightharsh.eightharsh.feature.workapps

import androidx.compose.ui.Alignment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.managedprofiles.CanToggleManagedProfileUseCase
import com.eightharsh.eightharsh.core.domain.managedprofiles.GetManagedProfileAppsUseCase
import com.eightharsh.eightharsh.core.domain.managedprofiles.ManagedProfileType
import com.eightharsh.eightharsh.core.domain.managedprofiles.ObserveManagedProfileUnlockedUseCase
import com.eightharsh.eightharsh.core.domain.managedprofiles.ToggleManagedProfileUseCase
import com.eightharsh.eightharsh.core.domain.managedprofiles.ToggleManagedProfileUseCaseOutput
import com.eightharsh.eightharsh.core.domain.repository.android.AppsRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.AppearanceRepository
import com.eightharsh.eightharsh.core.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkAppsViewModel @Inject constructor(
    getManagedProfileAppsUseCase: GetManagedProfileAppsUseCase,
    observeManagedProfileUnlockedUseCase: ObserveManagedProfileUnlockedUseCase,
    private val toggleManagedProfileUseCase: ToggleManagedProfileUseCase,
    canToggleManagedProfileUseCase: CanToggleManagedProfileUseCase,
    appsRepository: AppsRepository,
    appearanceRepository: AppearanceRepository
) : ViewModel() {

    val canToggleProfile: Boolean = canToggleManagedProfileUseCase(ManagedProfileType.WorkApps)

    val isUnlocked: StateFlow<Boolean> = observeManagedProfileUnlockedUseCase(ManagedProfileType.WorkApps)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val workApps: StateFlow<List<InstalledApp>> = combine(
        getManagedProfileAppsUseCase(ManagedProfileType.WorkApps),
        appsRepository.installedApps
    ) { apps, _ -> apps }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleWorkProfile(onLauncherNotDefault: () -> Unit) {
        viewModelScope.launch {
            when (toggleManagedProfileUseCase(ManagedProfileType.WorkApps)) {
                ToggleManagedProfileUseCaseOutput.FailedNotDefaultLauncher -> onLauncherNotDefault()
                else -> { /* Success cases are handled by the Flow observers */ }
            }
        }
    }

    val appsAlignment = appearanceRepository.appsAlignment.map { alignment ->
        when (alignment) {
            "Left" -> Alignment.Start
            "Center" -> Alignment.CenterHorizontally
            else -> Alignment.End
        }
    }
}
