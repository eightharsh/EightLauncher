package com.eightharsh.eightharsh.core.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.getValue
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Android
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eightharsh.eightharsh.core.model.InstalledApp
import com.eightharsh.eightharsh.core.model.AppAction

/**
 * Bottom Sheet home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenBottomSheet(
    modifier: Modifier = Modifier,
    app: InstalledApp,
    actions: List<AppAction>,
    onDismissRequest: () -> Unit,
    sheetState: SheetState? = null,
    shortcutActions: List<AppAction> = listOf()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appIcon by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, app.componentName, app.user) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val launcher = context.getSystemService(android.content.pm.LauncherApps::class.java)
                val info = launcher.resolveActivity(android.content.Intent().setComponent(app.componentName), app.user)
                val drawable = info?.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                    ?: context.packageManager.getActivityIcon(app.componentName)
                drawable.toBitmap(96, 96)
            }.getOrNull()
        }
    }
    val screenHeight = LocalWindowInfo.current.containerDpSize.height

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState ?: rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ) {
        Column(
            modifier
                .heightIn(max = screenHeight * 0.8f)
                .fillMaxWidth()
                .padding(25.dp, 25.dp, 25.dp, 0.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                if (appIcon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = appIcon!!.asImageBitmap(),
                        contentDescription = "${app.displayName} icon",
                        modifier = Modifier.size(45.dp)
                    )
                } else {
                    Icon(Icons.Default.Android, contentDescription = "App", modifier = Modifier.size(45.dp))
                }
                Text(
                    app.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 15.dp))

            // Actions
            Column(Modifier.padding(start = 47.dp, bottom = 50.dp)) {
                if (shortcutActions.isNotEmpty()) {
                    shortcutActions.filter { it.isVisible(app) }.forEach { action ->
                        AppActionItem(action, app)
                    }

                    HorizontalDivider(Modifier.padding(vertical = 15.dp))
                }

                actions.filter { it.isVisible(app) }.forEach { action ->
                    AppActionItem(action, app)
                }
            }
        }
    }
}

@Composable
private fun AppActionItem(action: AppAction, app: InstalledApp) {
    val label = action.label ?: action.labelRes?.let { stringResource(it) } ?: ""
    BottomSheetAction(label) { action.onClick(app) }
}

@Composable
fun BottomSheetAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .padding(vertical = 10.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium
    )
}