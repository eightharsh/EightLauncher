package com.eightharsh.eightharsh.feature.homescreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.apps.GetFavoriteAppsUseCase
import com.eightharsh.eightharsh.core.domain.repository.db.ScreenTimeRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.AppearanceRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.ClockRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.LauncherBehaviorRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.OnboardingRepository
import com.eightharsh.eightharsh.core.domain.repository.settings.ScreenTimeSettingsRepository
import com.eightharsh.eightharsh.core.domain.screentime.GetAppUsageUiListUseCase
import com.eightharsh.eightharsh.core.domain.time.GetCurrentTimePartsUseCase
import com.eightharsh.eightharsh.core.model.AppUsageUiModel
import com.eightharsh.eightharsh.core.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewHomeScreenViewModel @Inject constructor(
    appearanceRepository: AppearanceRepository,
    clockRepository: ClockRepository,
    launcherBehaviorRepository: LauncherBehaviorRepository,
    onboardingRepository: OnboardingRepository,
    screenTimeSettingsRepository: ScreenTimeSettingsRepository,
    getAppUsageUiListUseCase: GetAppUsageUiListUseCase,
    getFavoriteAppsUseCase: GetFavoriteAppsUseCase,
    private val getCurrentTimeParts: GetCurrentTimePartsUseCase,
    @ApplicationContext private val context: Context,
    screenTimeRepository: ScreenTimeRepository,
    personalRepository: com.eightharsh.eightharsh.core.domain.personal.PersonalRepository
) : ViewModel() {

    // Settings
    val twelveHourClock = clockRepository.twelveHourClock
    val showClock = clockRepository.showClock
    val bigClock = clockRepository.bigClock
    val showDate = clockRepository.showDate
    val showScreenTimeHome = screenTimeSettingsRepository.showScreenTimeHome
    val showScreenTimeApp = screenTimeSettingsRepository.showScreenTimeApp
    val firstTimeHelp = onboardingRepository.firstTimeHelp
    val hapticFeedBackEnabled = launcherBehaviorRepository.hapticFeedBackEnabled
    val showWallpaper = appearanceRepository.showWallpaper

    val homeAlignment = appearanceRepository.homeAlignment.map { alignment ->
        when (alignment) {
            "Left" -> Alignment.Start
            "Center" -> Alignment.CenterHorizontally
            else -> Alignment.End
        }
    }

    val homeVAlignment = appearanceRepository.homeVAlignment.map { alignment ->
        when (alignment) {
            "Top" -> Arrangement.Top
            "Center" -> Arrangement.Center
            else -> Arrangement.Bottom
        }
    }

    // Clock
    private val _timeParts = MutableStateFlow(getCurrentTimeParts.invoke(LocalTime.now(), false))
    val timeParts: StateFlow<Triple<Int, Int, Boolean>> = _timeParts.asStateFlow()
    private var clockTickerJob: Job? = null

    private fun timeTicksFlow(): Flow<Unit> = callbackFlow {
        trySend(Unit)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    fun startClockTicker(twelveHourDisplay: Boolean) {
        clockTickerJob?.cancel()
        clockTickerJob = viewModelScope.launch {
            timeTicksFlow().collect {
                _timeParts.value = getCurrentTimeParts.invoke(LocalTime.now(), twelveHourDisplay)
            }
        }
    }

    // Favorite Apps
    val favoriteApps: StateFlow<List<InstalledApp>> = kotlinx.coroutines.flow.combine(
        getFavoriteAppsUseCase(), personalRepository.shuffleOnUnlock, personalRepository.shuffledPackages,
        personalRepository.focusSession
    ) { apps, enabled, order, focus ->
        val visibleApps = apps.filterNot { it.packageName in focus?.packages.orEmpty() }
        if (!enabled) visibleApps else visibleApps.sortedBy { app -> order.indexOf(app.packageName).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun logException(e: Exception) {
        android.util.Log.w("HomeScreen", "Home screen operation failed", e)
    }

    private fun getDatesPair(): Pair<String, String> {
        val now = Date()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val calendar = Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return today to yesterday
    }

    private val datesFlow: Flow<Pair<String, String>> = callbackFlow {
        trySend(getDatesPair())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                trySend(getDatesPair())
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged()

    val appUsageUiList: StateFlow<List<AppUsageUiModel>> = datesFlow.flatMapLatest { (today, yesterday) ->
        getAppUsageUiListUseCase(today, yesterday)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalUsage: StateFlow<Long> = datesFlow.flatMapLatest { (today, _) ->
        screenTimeRepository.getTotalUsageForDateFlow(today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    fun getScreenTime(packageName: String): Long {
        return appUsageUiList.value.find { it.packageName == packageName }?.totalTime ?: 0L
    }
}
