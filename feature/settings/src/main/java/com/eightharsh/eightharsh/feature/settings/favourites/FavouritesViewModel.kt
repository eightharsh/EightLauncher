package com.eightharsh.eightharsh.feature.settings.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.apps.GetFavoriteAppsUseCase
import com.eightharsh.eightharsh.core.domain.repository.android.AppsRepository
import com.eightharsh.eightharsh.core.domain.repository.db.ModifiedAppsRepository
import com.eightharsh.eightharsh.core.model.InstalledApp
import com.eightharsh.eightharsh.core.navigation.NavRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    appsRepository: AppsRepository,
    getFavoriteAppsUseCase: GetFavoriteAppsUseCase,
    val navRouter: NavRouter,
    val modifiedAppsRepository: ModifiedAppsRepository
): ViewModel() {
    val installedApps = appsRepository.mainUserApps

    val favoriteApps: StateFlow<List<InstalledApp>> = getFavoriteAppsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}