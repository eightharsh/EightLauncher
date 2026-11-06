package com.eightharsh.eightharsh

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eightharsh.eightharsh.core.common.DefaultSettings
import com.eightharsh.eightharsh.core.domain.managedprofiles.ManagedProfileType
import com.eightharsh.eightharsh.core.model.InstalledApp
import com.eightharsh.eightharsh.core.ui.DefaultSettingsUi
import com.eightharsh.eightharsh.core.ui.composables.TabbedScreen
import com.eightharsh.eightharsh.core.ui.utils.doHapticFeedBack
import com.eightharsh.eightharsh.feature.appslist.AppsListViewModel
import com.eightharsh.eightharsh.feature.homescreen.HomeScreen
import com.eightharsh.eightharsh.feature.screentime.ScreenTimeDashboard
import com.eightharsh.eightharsh.feature.screentime.ScreenTimeViewModel
import com.eightharsh.eightharsh.feature.securefolder.SecureFolderButton
import com.eightharsh.eightharsh.feature.securefolder.canUseSecureFolder
import com.eightharsh.eightharsh.screens.ParentBottomSheetOverlay
import com.eightharsh.eightharsh.feature.workapps.WorkApps
import com.eightharsh.eightharsh.privatespace.PrivateSpace
import com.eightharsh.eightharsh.screens.AppsListTabScreen
import com.eightharsh.eightharsh.screens.HomeWidgetSlot
import com.eightharsh.eightharsh.screens.OpenChallengeOverlay
import kotlinx.coroutines.launch

/**
 * Main composable for home screen:
 * contains a pager with all the pages inside of it, contains bottom sheet, contains open challenge UI
 */
@Composable
fun MainPagerScreen(
    viewModel: MainPagerScreenViewModel = hiltViewModel(),
    appsListViewModel: AppsListViewModel = hiltViewModel(),
    screenTimeViewModel: ScreenTimeViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
) {
    val appslistScrollState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val hideScreenTimePage by viewModel.hideScreenTimePage.collectAsState()
    val doubleTapToLock by viewModel.doubleTapToLock.collectAsState(initial = DefaultSettings.DOUBLE_TAP_TO_LOCK)
    val hapticFeedbackEnabled by viewModel.hapticFeedBackEnabled.collectAsState(initial = DefaultSettings.HAPTIC_FEEDBACK)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val homeAlignment by viewModel.homeAlignment.collectAsState(initial = DefaultSettingsUi.HOME_ALIGNMENT)
    val widgetHeight by viewModel.widgetHeight.collectAsState(initial = DefaultSettings.WIDGET_HEIGHT)
    val widgetWidth by viewModel.widgetWidth.collectAsState(initial = DefaultSettings.WIDGET_WIDTH)
    val widgetId by viewModel.widgetId.collectAsState(initial = DefaultSettings.WIDGET_ID)
    val widgetHostManager = viewModel.widgetHostManager
    val widgetOffsetPref by viewModel.widgetOffset.collectAsState(initial = DefaultSettings.WIDGET_OFFSET)
    val showWallpaper by viewModel.showWallpaper.collectAsState(initial = false)

    // Global action callbacks
    val handleAppClick: (InstalledApp) -> Unit = { app ->
        viewModel.openApp(
            app = app,
            overrideChallenge = false,
            onAppOpened = {
                screenTimeViewModel.onAppOpened(it)
                appsListViewModel.onSearchExpandedChanged(false)
                doHapticFeedBack(haptics, hapticFeedbackEnabled)
            }
        )
    }
    val handleAppLongClick: (InstalledApp) -> Unit = { app ->
        viewModel.setBottomSheetApp(app)
        viewModel.setBottomSheetVisible(true)
        doHapticFeedBack(haptics, hapticFeedbackEnabled)
    }

    // Make it that if you go back you go back to main page
    BackHandler(enabled = true) {
        coroutineScope.launch {
            viewModel.animatedGoToMainPage()
        }
    }

    // Generate Apps List tabs
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()
    val appsListTabs = listOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && canUseSecureFolder(context = context)) {
            TabbedScreen(
                title = "Secure Folder", icon = Icons.Default.Lock, content = {
                    SecureFolderButton(
                        modifier = Modifier.fillMaxSize()
                    )
                })
        } else {
            null
        },
        if (isDefaultLauncher && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && viewModel.managedProfileExists(
                ManagedProfileType.PrivateSpace
            )
        ) {
            TabbedScreen(
                title = "Private", icon = Icons.Default.Lock, content = {
                    PrivateSpace(
                        modifier = Modifier.fillMaxSize(),
                        onAppClick = { app -> handleAppClick(app) },
                        onAppLongClick = { app -> handleAppLongClick(app) }
                    )
                })
        } else {
            null
        },
        if (viewModel.isManagedProfileSupported(type = ManagedProfileType.WorkApps) && viewModel.managedProfileExists(
                type = ManagedProfileType.WorkApps
            )
        ) {
            TabbedScreen(
                title = "Work", icon = Icons.Default.Work, content = {
                    WorkApps(
                        modifier = Modifier.fillMaxSize(),
                        onAppClick = { app -> handleAppClick(app) },
                        onAppLongClick = { app -> handleAppLongClick(app) }
                    )
                })
        } else {
            null
        }
    )

    // Figure out where all the pages are
    val screenTimePageIndex = if (!hideScreenTimePage) 0 else -1
    val homePageIndex = if (hideScreenTimePage) 0 else 1
    val appsListPageIndex = if (hideScreenTimePage) 1 else 2

    // Some apps list stuff
    val isAppsListVisible = viewModel.pagerState.currentPage == appsListPageIndex
    val autoOpenSearch by appsListViewModel.searchAutoOpen.collectAsState(initial = DefaultSettings.SEARCH_AUTO_OPEN)

    // Tidy up apps list when it closes or opens
    LaunchedEffect(isAppsListVisible) {
        if (!isAppsListVisible) {
            appsListViewModel.onSearchExpandedChanged(false)
            appslistScrollState.scrollToItem(0)
            viewModel.setBottomSheetVisible(false)
        } else if (autoOpenSearch) {
            appsListViewModel.onSearchExpandedChanged(true)
        }
    }

    // Home Screen Pages
    HorizontalPager(
        state = viewModel.pagerState,
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClickLabel = "",
                onLongClick = {
                    viewModel.openSettings()
                    viewModel.setFirstTimeHelp(false)
                },
                indication = null,
                interactionSource = viewModel.interactionSource,
                onDoubleClick = {
                    // Turn screen off
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (doubleTapToLock) {
                            viewModel.lockScreen()
                        }
                    }
                })
    ) { page ->
        when (page) {
            screenTimePageIndex -> ScreenTimeDashboard()

            homePageIndex -> HomeScreen(
                onAppClick = handleAppClick,
                onAppLongClick = handleAppLongClick,
                widget = {
                    HomeWidgetSlot(
                        homeAlignment = homeAlignment,
                        widgetOffsetPref = widgetOffsetPref,
                        widgetId = widgetId,
                        widgetHostManager = widgetHostManager,
                        widgetWidth = widgetWidth,
                        widgetHeight = widgetHeight
                    )
                }
            )

            appsListPageIndex -> AppsListTabScreen(
                scrollState = appslistScrollState,
                appsListViewModel = appsListViewModel,
                appsListTabs = appsListTabs,
                handleAppClick = handleAppClick,
                handleAppLongClick = handleAppLongClick,
                hapticFeedbackEnabled = hapticFeedbackEnabled
            )
        }
    }

    // Single Parent-level Bottom Sheet
    ParentBottomSheetOverlay(viewModel = viewModel)

    // Open Challenge
    OpenChallengeOverlay(
        viewModel = viewModel,
        screenTimeViewModel = screenTimeViewModel,
        hapticFeedbackEnabled = hapticFeedbackEnabled
    )
}
