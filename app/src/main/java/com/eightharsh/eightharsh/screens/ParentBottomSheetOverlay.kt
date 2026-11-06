package com.eightharsh.eightharsh.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.eightharsh.eightharsh.MainPagerScreenViewModel
import com.eightharsh.eightharsh.core.ui.composables.HomeScreenBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentBottomSheetOverlay(
    viewModel: MainPagerScreenViewModel
) {
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    val bottomSheetApp by viewModel.bottomSheetApp.collectAsState()
    val bottomSheetActions by viewModel.bottomSheetActions.collectAsState()
    val shortcutActions by viewModel.shortcutActions.collectAsState()

    if (showBottomSheet && bottomSheetApp != null) {
        HomeScreenBottomSheet(
            app = bottomSheetApp!!,
            actions = bottomSheetActions,
            onDismissRequest = { viewModel.setBottomSheetVisible(false) },
            shortcutActions = shortcutActions,
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        )
    }
}