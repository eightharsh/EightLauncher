package com.eightharsh.eightharsh

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.eightharsh.eightharsh.core.common.configureNavBar
import com.eightharsh.eightharsh.core.common.configureStatusBar
import com.eightharsh.eightharsh.core.data.worker.ClearOldDataWorker
import com.eightharsh.eightharsh.core.navigation.AppNavKey
import com.eightharsh.eightharsh.core.navigation.NavEvent
import com.eightharsh.eightharsh.core.navigation.NavRouter
import com.eightharsh.eightharsh.core.theme.EscapeTheme
import com.eightharsh.eightharsh.core.theme.motion.enterTransition
import com.eightharsh.eightharsh.core.theme.motion.exitTransition
import com.eightharsh.eightharsh.feature.newwidgets.WidgetHostManager
import com.eightharsh.eightharsh.feature.onboarding.Onboarding
import com.eightharsh.eightharsh.feature.screentime.ScreenTimeViewModel
import com.eightharsh.eightharsh.feature.settings.devoptions.DevOptions
import com.eightharsh.eightharsh.feature.settings.favourites.BulkFavouritesManager
import com.eightharsh.eightharsh.feature.settings.font.ChooseFont
import com.eightharsh.eightharsh.feature.settings.font.FontLicenceDialog
import com.eightharsh.eightharsh.feature.settings.hiddenapps.BulkManageHiddenApps
import com.eightharsh.eightharsh.feature.settings.hiddenapps.HiddenApps
import com.eightharsh.eightharsh.feature.settings.mainpage.MainSettingsPage
import com.eightharsh.eightharsh.feature.settings.openchallenges.OpenChallengeSettings
import com.eightharsh.eightharsh.feature.settings.privacypolicy.PrivacyPolicyDialog
import com.eightharsh.eightharsh.feature.settings.theme.ThemeOptions
import com.eightharsh.eightharsh.feature.newwidgets.options.WidgetOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainHomeScreenActivity : ComponentActivity() {
    private val globalViewModel: GlobalViewModel by viewModels()
    private val mainPagerViewModel: MainPagerScreenViewModel by viewModels()
    private val screenTimeViewModel: ScreenTimeViewModel by viewModels()

    @Inject
    lateinit var widgetHostManager: WidgetHostManager
    @Inject
    lateinit var navRouter: NavRouter

    override fun onStart() {
        super.onStart()

        // Start listening for widgets
        try {
            widgetHostManager.startListening()
        } catch (e: Exception) {
            Log.e("Widgets", "Error starting AppWidgetHost in onStart", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Setup Splashscreen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Sort out the window
        enableEdgeToEdge()
        lifecycleScope.launch {
            globalViewModel.showStatusBar.collect { show ->
                window.configureStatusBar(hide = !show)
                window.configureNavBar(hide = false)
            }
        }

        // Set up the screen time tracking clean-up
        ClearOldDataWorker.scheduleDailyCleanup(this)

        // Determine start destination before hiding splash screen
        var startDestination by mutableStateOf<AppNavKey?>(null)
        lifecycleScope.launch {
            val isFirstTime = globalViewModel.firstTime.first()
            startDestination = if (isFirstTime) AppNavKey.Onboarding else AppNavKey.Home
        }

        // Keep splash screen visible until we know where to go and essential data is loaded
        splashScreen.setKeepOnScreenCondition {
            startDestination == null || !globalViewModel.initializationState.value.isAllLoaded
        }


        // Set up the application content
        setContent {
            val destination = startDestination
            if (destination != null) {
                EscapeTheme {
                    SetupNavDisplay(destination)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Responsible for taking you to the favourites page when you press the home button
        if ((intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) || intent.getBooleanExtra("showQuickNotes", false)) {
            // Only navigate home if we've finished onboarding
            lifecycleScope.launch {
                val isFirstTime = globalViewModel.firstTime.first()
                if (!isFirstTime) {
                    navRouter.navigateTo(AppNavKey.Home, true)
                    withContext(AndroidUiDispatcher.Main) {
                        mainPagerViewModel.animatedGoToMainPage()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // The user may have changed the default launcher while we were in the background
        mainPagerViewModel.updateLauncherStatus()

        // Check if we need to update screen time when coming back from an app
        lifecycleScope.launch(Dispatchers.IO) {
            if (screenTimeViewModel.hasActiveSession()) {
                val packageName = screenTimeViewModel.getActiveSessionPackageName() ?: ""
                if (packageName.isNotEmpty()) {
                    screenTimeViewModel.onAppClosed(packageName)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // Stop listening for widgets
        try {
            widgetHostManager.stopListening()
        } catch (e: Exception) {
            Log.e("Widgets", "Error stopping AppWidgetHost in onStop", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    /**
     * Sets up main navigation display for the app
     *
     * @param startDestination Where to start
     */
    @Composable
    private fun SetupNavDisplay(startDestination: AppNavKey) {
        val backStack = rememberNavBackStack(startDestination)
        val showWallpaper by globalViewModel.showWallpaper.collectAsState(initial = false)

        // Listener for all nav events
        LaunchedEffect(Unit) {
            navRouter.navigationEvents.collect { event ->
                when (event) {
                    is NavEvent.NavigateTo -> {
                        if (event.clearBackStack) {
                            backStack.removeAll { it != event.key }
                            if (backStack.isEmpty() || backStack.last() != event.key) {
                                backStack.add(event.key)
                            }
                        } else if (backStack.lastOrNull() != event.key) {
                            backStack.add(event.key)
                        }
                    }

                    NavEvent.PopBackStack -> {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                }
            }
        }

        if(!showWallpaper){
            Box(Modifier.background(color = MaterialTheme.colorScheme.surface).fillMaxSize())
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<AppNavKey.Home> {

                        MainPagerScreen(
                            viewModel = mainPagerViewModel,
                        )
                    }
                    entry<AppNavKey.Onboarding> {
                        Onboarding()
                    }
                    entry<AppNavKey.Settings.MainSettingsPage> {
                        MainSettingsPage(
                            goBack = {
                                backStack.removeLastOrNull()
                            },
                            onNavigate = { key ->
                                if (backStack.lastOrNull() != key) {
                                    backStack.add(key)
                                }
                            }
                        )
                    }
                    entry<AppNavKey.Settings.HiddenApps> {
                        HiddenApps(
                            goToManageHiddenApps = {
                                backStack.add(AppNavKey.Settings.BulkHiddenApps)
                            }) { backStack.removeLastOrNull() }
                    }
                    entry<AppNavKey.Settings.OpenChallenges> {
                        OpenChallengeSettings()
                    }
                    entry<AppNavKey.Settings.ChooseFont> {
                        ChooseFont { backStack.removeLastOrNull() }
                    }
                    entry<AppNavKey.Settings.DevOptions> {
                        DevOptions { backStack.removeLastOrNull() }
                    }
                    entry<AppNavKey.Settings.Theme> {
                        ThemeOptions(goBack = { backStack.removeLastOrNull() })
                    }
                    entry<AppNavKey.Settings.Widget> {
                        WidgetOptions(onBackClick = { backStack.removeLastOrNull() })
                    }
                    entry<AppNavKey.Settings.BulkHiddenApps> {
                        BulkManageHiddenApps()
                    }
                    entry<AppNavKey.Settings.BulkFavouriteApps> {
                        BulkFavouritesManager()
                    }
                    entry<AppNavKey.Settings.FontLicences> {
                        FontLicenceDialog {
                            backStack.removeLastOrNull()
                        }
                    }
                    entry<AppNavKey.Settings.PrivacyPolicy> {
                        PrivacyPolicyDialog {
                            backStack.removeLastOrNull()
                        }
                    }
                },
                transitionSpec = {
                    enterTransition()
                },
                popTransitionSpec = {
                    exitTransition()
                },
                predictivePopTransitionSpec = {
                    exitTransition()
                }
            )
        }
    }
}