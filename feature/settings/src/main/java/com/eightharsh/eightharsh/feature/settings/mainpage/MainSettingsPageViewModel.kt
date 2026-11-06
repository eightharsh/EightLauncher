package com.eightharsh.eightharsh.feature.settings.mainpage

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.common.DefaultSettings
import com.eightharsh.eightharsh.core.domain.launcher.GetIsDefaultLauncherUseCase
import com.eightharsh.eightharsh.core.domain.launcher.SetDefaultLauncherUseCase
import com.eightharsh.eightharsh.core.domain.repository.AppConfiguration
import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.*
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainSettingsUiState(
    val hapticFeedBackEnabled: Boolean = DefaultSettings.HAPTIC_FEEDBACK,
    val twelveHourClock: Boolean = DefaultSettings.TWELVE_HOUR_CLOCK,
    val showClock: Boolean = DefaultSettings.SHOW_CLOCK,
    val bigClock: Boolean = DefaultSettings.BIG_CLOCK,
    val showDate: Boolean = DefaultSettings.SHOW_DATE,
    val showStatusBar: Boolean = DefaultSettings.SHOW_STATUS_BAR,
    val showScreenTimeHome: Boolean = DefaultSettings.SHOW_SCREEN_TIME_HOME,
    val showScreenTimeApp: Boolean = DefaultSettings.SHOW_SCREEN_TIME_APP,
    val homeAlignment: Int = DefaultSettings.ALIGNMENT_LEFT_INDEX,
    val homeVAlignment: Int = DefaultSettings.ALIGNMENT_CENTER_INDEX,
    val appsAlignment: Int = DefaultSettings.ALIGNMENT_LEFT_INDEX,
    val doubleTapToLock: Boolean = DefaultSettings.DOUBLE_TAP_TO_LOCK,
    val showSearchBox: Boolean = DefaultSettings.SHOW_SEARCH_BOX,
    val searchAutoOpen: Boolean = DefaultSettings.SEARCH_AUTO_OPEN,
    val automaticallyOpenAppsInSearch: Boolean = DefaultSettings.AUTOMATICALLY_OPEN_APPS_IN_SEARCH,
    val hideScreenTimePage: Boolean = DefaultSettings.HIDE_SCREEN_TIME_PAGE,
    val isDefaultLauncher: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false
)

@HiltViewModel
class MainSettingsPageViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository,
    private val clockRepository: ClockRepository,
    private val launcherBehaviorRepository: LauncherBehaviorRepository,
    private val searchSettingsRepository: SearchSettingsRepository,
    private val screenTimeSettingsRepository: ScreenTimeSettingsRepository,
    private val getIsDefaultLauncherUseCase: GetIsDefaultLauncherUseCase,
    private val setDefaultLauncherUseCase: SetDefaultLauncherUseCase,
    systemActionsRepository: SystemActionsRepository,
    val appConfiguration: AppConfiguration
) : ViewModel() {

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    val uiState: StateFlow<MainSettingsUiState> = combine(
        launcherBehaviorRepository.hapticFeedBackEnabled,
        clockRepository.twelveHourClock,
        clockRepository.showClock,
        clockRepository.bigClock,
        clockRepository.showDate,
        appearanceRepository.showStatusBar,
        screenTimeSettingsRepository.showScreenTimeHome,
        screenTimeSettingsRepository.showScreenTimeApp,
        appearanceRepository.homeAlignment,
        appearanceRepository.homeVAlignment,
        appearanceRepository.appsAlignment,
        launcherBehaviorRepository.doubleTapToLock,
        searchSettingsRepository.showSearchBox,
        searchSettingsRepository.searchAutoOpen,
        searchSettingsRepository.automaticallyOpenAppsInSearch,
        screenTimeSettingsRepository.hideScreenTimePage,
        _isDefaultLauncher,
        systemActionsRepository.isAccessibilityServiceEnabled
    ) { args: Array<Any?> ->
        MainSettingsUiState(
            hapticFeedBackEnabled = args[0] as Boolean,
            twelveHourClock = args[1] as Boolean,
            showClock = args[2] as Boolean,
            bigClock = args[3] as Boolean,
            showDate = args[4] as Boolean,
            showStatusBar = args[5] as Boolean,
            showScreenTimeHome = args[6] as Boolean,
            showScreenTimeApp = args[7] as Boolean,
            homeAlignment = when (args[8] as String) {
                "Left" -> 0
                "Center" -> 1
                else -> 2
            },
            homeVAlignment = when (args[9] as String) {
                "Top" -> 0
                "Center" -> 1
                else -> 2
            },
            appsAlignment = when (args[10] as String) {
                "Left" -> 0
                "Center" -> 1
                else -> 2
            },
            doubleTapToLock = args[11] as Boolean,
            showSearchBox = args[12] as Boolean,
            searchAutoOpen = args[13] as Boolean,
            automaticallyOpenAppsInSearch = args[14] as Boolean,
            hideScreenTimePage = args[15] as Boolean,
            isDefaultLauncher = args[16] as Boolean,
            isAccessibilityServiceEnabled = args[17] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainSettingsUiState()
    )

    fun updateLauncherStatus() {
        _isDefaultLauncher.value = getIsDefaultLauncherUseCase()
    }

    fun getPromptDefaultLauncherIntent(): Intent {
        return setDefaultLauncherUseCase()
    }

    init {
        updateLauncherStatus()
    }

    fun setHapticFeedback(value: Boolean) {
        viewModelScope.launch {
            launcherBehaviorRepository.setHapticFeedback(value)
        }
    }

    fun setTwelveHourClock(value: Boolean) {
        viewModelScope.launch {
            clockRepository.setTwelveHourClock(value)
        }
    }

    fun setShowClock(value: Boolean) {
        viewModelScope.launch {
            clockRepository.setShowClock(value)
        }
    }

    fun setBigClock(value: Boolean) {
        viewModelScope.launch {
            clockRepository.setBigClock(value)
        }
    }

    fun setShowDate(value: Boolean) {
        viewModelScope.launch {
            clockRepository.setShowDate(value)
        }
    }

    fun setShowStatusBar(value: Boolean) {
        viewModelScope.launch {
            appearanceRepository.setShowStatusBar(value)
        }
    }

    fun setShowScreenTimeHome(value: Boolean) {
        viewModelScope.launch {
            screenTimeSettingsRepository.setShowScreenTimeHome(value)
        }
    }



    fun setShowScreenTimeApp(value: Boolean) {
        viewModelScope.launch {
            screenTimeSettingsRepository.setShowScreenTimeApp(value)
        }
    }

    fun setHomeAlignment(index: Int) {
        val value = when (index) {
            0 -> "Left"
            1 -> "Center"
            else -> "Right"
        }
        viewModelScope.launch {
            appearanceRepository.setHomeAlignment(value)
        }
    }

    fun setHomeVAlignment(index: Int) {
        val value = when (index) {
            0 -> "Top"
            1 -> "Center"
            else -> "Bottom"
        }
        viewModelScope.launch {
            appearanceRepository.setHomeVAlignment(value)
        }
    }


    fun setAppsAlignment(index: Int) {
        val value = when (index) {
            0 -> "Left"
            1 -> "Center"
            else -> "Right"
        }
        viewModelScope.launch {
            appearanceRepository.setAppsAlignment(value)
        }
    }

    fun setDoubleTapToLock(value: Boolean) {
        viewModelScope.launch {
            launcherBehaviorRepository.setDoubleTapToLock(value)
        }
    }

    fun setShowSearchBox(value: Boolean) {
        viewModelScope.launch {
            searchSettingsRepository.setShowSearchBox(value)
        }
    }

    fun setSearchAutoOpen(value: Boolean) {
        viewModelScope.launch {
            searchSettingsRepository.setSearchAutoOpen(value)
        }
    }

    fun setAutomaticallyOpenAppsInSearch(value: Boolean) {
        viewModelScope.launch {
            searchSettingsRepository.setAutomaticallyOpenAppsInSearch(value)
        }
    }

    fun setHideScreenTimePage(value: Boolean) {
        viewModelScope.launch {
            screenTimeSettingsRepository.setHideScreenTimePage(value)
        }
    }


}
