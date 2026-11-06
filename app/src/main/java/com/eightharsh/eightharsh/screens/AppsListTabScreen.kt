package com.eightharsh.eightharsh.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.eightharsh.eightharsh.core.common.DefaultSettings
import com.eightharsh.eightharsh.core.model.InstalledApp
import com.eightharsh.eightharsh.core.ui.DefaultSettingsUi
import com.eightharsh.eightharsh.core.ui.composables.TabDisplay
import com.eightharsh.eightharsh.core.ui.composables.TabbedScreen
import com.eightharsh.eightharsh.core.ui.utils.doHapticFeedBack
import com.eightharsh.eightharsh.feature.appslist.AppsList
import com.eightharsh.eightharsh.feature.appslist.AppsListViewModel

@Composable
fun AppsListTabScreen(
    scrollState: LazyListState,
    appsListViewModel: AppsListViewModel,
    appsListTabs: List<TabbedScreen?>,
    handleAppClick: (InstalledApp) -> Unit,
    handleAppLongClick: (InstalledApp) -> Unit,
    hapticFeedbackEnabled: Boolean
) {
    val haptics = LocalHapticFeedback.current

    val searchText by appsListViewModel.searchText.collectAsState()
    val searchExpanded by appsListViewModel.searchExpanded.collectAsState()
    val showSearchBox by appsListViewModel.showSearchBox.collectAsState(initial = DefaultSettings.SHOW_SEARCH_BOX)
    val appsListAlignment by appsListViewModel.appsAlignment.collectAsState(initial = DefaultSettingsUi.APPS_ALIGNMENT)
    val selectedTabIndex = remember { mutableIntStateOf(0) }
    val result by appsListViewModel.searchResults.collectAsState()
    val autoOpenAppInSearch by appsListViewModel.automaticallyOpenAppsInSearch.collectAsState(
        initial = DefaultSettings.AUTOMATICALLY_OPEN_APPS_IN_SEARCH
    )

    LaunchedEffect(searchText, result, autoOpenAppInSearch, searchExpanded) {
        if (searchExpanded && autoOpenAppInSearch &&
            result.query.length >= 2 && result.query == appsListViewModel.searchText.value.trim()
        ) {
            result.apps.singleOrNull()?.let(handleAppClick)
        }
    }

    TabDisplay(
        screens = listOf(
            TabbedScreen(
                title = "", icon = Icons.Rounded.Apps, content = { padding ->
                    AppsList(
                        scrollState = scrollState,
                        padding = padding,
                        onAppOpened = { app ->
                            handleAppClick(app)
                        },
                        onAppLongClick = { app ->
                            handleAppLongClick(app)
                        },
                        appsListViewModel = appsListViewModel
                    )
                })
        ) + appsListTabs.filterNotNull(),
        selectedTabIndex = selectedTabIndex,
        alignment = appsListAlignment,
        showSearch = showSearchBox,
        searchText = searchText,
        searchExpanded = searchExpanded,
        onSearchExpandedChange = {
            appsListViewModel.onSearchExpandedChanged(it)
            doHapticFeedBack(haptics, hapticFeedbackEnabled)
        },
        onSearchTextChanged = appsListViewModel::onSearchTextChanged,
        onSearchDone = { query: String, keyboardController: SoftwareKeyboardController? ->
            val current = appsListViewModel.searchResults.value
            val app = current.apps.firstOrNull()?.takeIf {
                current.query == query.trim() && current.query == appsListViewModel.searchText.value.trim()
            }
            if (app != null) {
                keyboardController?.hide()
                handleAppClick(app)
            } else {
                doHapticFeedBack(haptics, hapticFeedbackEnabled)
            }
        })
}
