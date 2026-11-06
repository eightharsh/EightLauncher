package com.eightharsh.eightharsh.feature.settings.mainpage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eightharsh.eightharsh.core.domain.repository.android.AppsRepository
import com.eightharsh.eightharsh.core.ui.composables.BottomSheetAction
import java.text.DateFormat
import java.util.Date
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.personal.PersonalRepository
import com.eightharsh.eightharsh.core.ui.composables.SettingsSwitch
import com.eightharsh.eightharsh.core.ui.composables.SettingsNavigationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PersonalSettingsViewModel @Inject constructor(
    private val personal: PersonalRepository,
    appsRepository: AppsRepository
) : ViewModel() {
    val focus = personal.focusSession
    val apps = appsRepository.mainUserApps
    suspend fun startFocus(packages: Set<String>, minutes: Int) = personal.startFocus(packages, minutes)
    suspend fun stopFocus() = personal.stopFocus()

    val showNotes = personal.showNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val shuffle = personal.shuffleOnUnlock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun notes(value: Boolean) { viewModelScope.launch { personal.setShowNotes(value) } }
    fun shuffle(value: Boolean) { viewModelScope.launch { personal.setShuffleOnUnlock(value) } }
    suspend fun export() = personal.exportBackup()
    suspend fun restore(json: String) = personal.importBackup(json)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalSettings(viewModel: PersonalSettingsViewModel = hiltViewModel()) {
    val notes by viewModel.showNotes.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()
    SettingsSwitch(label = "Home-screen notes & tasks", checked = notes, isTopOfGroup = true, onCheckedChange = viewModel::notes)
    SettingsSwitch(label = "Shuffle favorites on unlock", checked = shuffle, onCheckedChange = viewModel::shuffle)
    val focus by viewModel.focus.collectAsState(initial = null)
    var showFocus by remember { mutableStateOf(false) }
    SettingsNavigationItem(
        label = if (focus == null) "Focus mode" else "Focus mode · On",
        diagonalArrow = false,
        isBottomOfGroup = true,
        onClick = { showFocus = true }
    )
    if (showFocus) {
        FocusSheet(viewModel, onDismiss = { showFocus = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusSheet(viewModel: PersonalSettingsViewModel, onDismiss: () -> Unit) {
    val focus by viewModel.focus.collectAsState(initial = null)
    val apps by viewModel.apps.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var duration by remember { mutableStateOf("25") }
    var error by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp)) {
            Text("Focus mode", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            val active = focus
            if (active != null) {
                Text(
                    "Ends at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(active.endsAt))}",
                    style = MaterialTheme.typography.bodySmall
                )
                BottomSheetAction("End focus") {
                    if (!busy) scope.launch {
                        busy = true
                        try { viewModel.stopFocus(); onDismiss() }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { error = true }
                        finally { busy = false }
                    }
                }
            } else {
                Text("Choose apps to hide from Home and the app list during focus.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) duration = it },
                    label = { Text("Minutes (1–180)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(apps.distinctBy { it.packageName }, key = { it.packageName }) { app ->
                        val checked = app.packageName in selected
                        Row(
                            Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = {
                                selected = if (it) selected + app.packageName else selected - app.packageName
                            }).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked, onCheckedChange = null)
                            Spacer(Modifier.width(12.dp))
                            Text(app.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                val minutes = duration.toIntOrNull()
                TextButton(
                    enabled = !busy && selected.isNotEmpty() && minutes != null && minutes in 1..180,
                    onClick = {
                        val length = minutes ?: return@TextButton
                        busy = true
                        scope.launch {
                            try { viewModel.startFocus(selected, length); onDismiss() }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { error = true }
                            finally { busy = false }
                        }
                    }
                ) { Text("Start focus") }
            }
            if (error) Text("Could not update Focus mode. Try again.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun BackupSettings(viewModel: PersonalSettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = runCatching {
                val backup = viewModel.export()
                withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(backup) } }
                "Backup saved. It contains your notes and app preferences; keep it private."
            }.getOrElse { "Could not save the backup." }
            busy = false
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var count = input.read(buffer)
                        while (count != -1) {
                            require(output.size() + count <= 1_000_000)
                            output.write(buffer, 0, count)
                            count = input.read(buffer)
                        }
                        output.toString("UTF-8")
                    }
                }
            }.onSuccess { pending = it }.onFailure { message = "Could not read this backup. Select an eightharsh JSON backup under 1 MB." }
            busy = false
        }
    }
    SettingsNavigationItem(label = if (busy) "Working…" else "Export settings & notes", diagonalArrow = false, isTopOfGroup = true, onClick = { if (!busy) export.launch("eightharsh-backup.json") })
    SettingsNavigationItem(label = "Restore settings & notes", diagonalArrow = false, isBottomOfGroup = true, onClick = { if (!busy) restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
    pending?.let { json -> AlertDialog(
        onDismissRequest = { if (!busy) pending = null },
        title = { Text("Restore backup?", style = MaterialTheme.typography.bodyMedium) },
        text = { Text("This replaces notes, favorites, hidden-app choices, and backed-up preferences. Usage history, widget bindings, and onboarding are kept on this device. Export your current setup first if you want to keep it.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                message = runCatching { viewModel.restore(json); "Backup restored." }.getOrElse { "Backup could not be restored. Check that it is a valid eightharsh backup." }
                pending = null; busy = false
            }
        }) { Text("Restore") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("Cancel") } }
    ) }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
}
