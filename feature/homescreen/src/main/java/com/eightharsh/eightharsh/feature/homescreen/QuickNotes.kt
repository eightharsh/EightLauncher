package com.eightharsh.eightharsh.feature.homescreen

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eightharsh.eightharsh.core.ui.composables.BottomSheetAction
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eightharsh.eightharsh.core.domain.personal.*
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

@HiltViewModel
class QuickNotesViewModel @Inject constructor(private val personal: PersonalRepository) : ViewModel() {
    val visible = personal.showNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val notes = personal.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var error by mutableStateOf<String?>(null)
        private set
    fun clearError() { error = null }
    fun save(note: QuickNote) { viewModelScope.launch {
        runCatching { personal.saveNote(note) }.onFailure { error = "Could not save note. Please try again." }
    } }
    suspend fun delete(id: String): Boolean = try {
        personal.deleteNote(id)
        true
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        error = "Could not delete note. Please try again."
        false
    }
    fun pin(note: QuickNote) = viewModelScope.launch {
        try {
            personal.setNotePinned(note.id, !note.pinned)
        } catch (e: IllegalArgumentException) {
            error = e.message ?: "Could not pin note."
        }
    }

    suspend fun clearCompleted(): List<QuickNote> = try {
        personal.clearCompletedNotes()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        error = "Could not clear completed notes."
        emptyList()
    }

    fun restoreCleared(notes: List<QuickNote>) = viewModelScope.launch {
        runCatching { personal.restoreClearedNotes(notes) }
            .onFailure { error = "Could not restore completed notes." }
    }

    fun refresh() { viewModelScope.launch { personal.refreshReminders() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotes(
    alignment: Alignment.Horizontal,
    viewModel: QuickNotesViewModel = hiltViewModel()
) {
    val visible by viewModel.visible.collectAsState()
    if (!visible) return
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<QuickNote?>(null) }
    var selected by remember { mutableStateOf<QuickNote?>(null) }
    var showAll by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val homeNotes = notes.sortedWith(compareByDescending<QuickNote> { it.pinned }.thenBy { it.dueAt ?: Long.MAX_VALUE }.thenBy { it.id })
    val ordered = homeNotes.sortedBy { it.done }
    val toggleDone: (QuickNote) -> Unit = { note ->
        viewModel.save(note.copy(done = !note.done, notified = !note.done && note.notified))
    }
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.refresh()
    }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                if (notificationsEnabled) viewModel.refresh()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val error = viewModel.error
    if (error != null) AlertDialog(
        onDismissRequest = viewModel::clearError,
        text = { Text(error) },
        confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = alignment) {
        SnackbarHost(snackbar)
        TextButton(
            onClick = { editing = QuickNote(UUID.randomUUID().toString(), "") },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Note", style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        homeNotes.take(3).forEach { note ->
            key(note.id) {
                NoteRow(note, alignment, { editing = note }, { selected = note }, { toggleDone(note) })
            }
        }
        if (ordered.size > 3 || ordered.any { it.done }) {
            TextButton(onClick = { showAll = true }) {
                Text("View all", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!notificationsEnabled && notes.any { !it.done && it.dueAt != null }) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Enable reminder notifications") }
        }
    }
    if (showAll) {
        ModalBottomSheet(onDismissRequest = { showAll = false }) {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(horizontal = 25.dp)
            ) {
                item {
                    Text("Note", style = MaterialTheme.typography.bodyMedium)
                    if (ordered.any { it.done }) {
                        BottomSheetAction("Clear completed") {
                            showAll = false
                            scope.launch {
                                val removed = viewModel.clearCompleted()
                                if (removed.isNotEmpty() &&
                                    snackbar.showSnackbar("Completed notes cleared", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed
                                ) {
                                    viewModel.restoreCleared(removed)
                                }
                            }
                        }
                    }
                }
                items(ordered.size, key = { ordered[it].id }) { index ->
                    val note = ordered[index]
                    NoteRow(note, alignment, { showAll = false; editing = note }, {
                        showAll = false
                        selected = note
                    }, { toggleDone(note) })
                }
            }
        }
    }
    selected?.let { note ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        ) {
            Column(Modifier.fillMaxWidth().padding(25.dp, 25.dp, 25.dp, 0.dp)) {
                Text(
                    note.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 32.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(Modifier.padding(vertical = 15.dp))
                Column(Modifier.padding(start = 47.dp, bottom = 50.dp)) {
                    BottomSheetAction("Edit note") {
                        selected = null
                        editing = note
                    }
                    BottomSheetAction(if (note.pinned) "Unpin from Home" else "Pin to Home") {
                        selected = null
                        viewModel.pin(note)
                    }
                    BottomSheetAction("Delete note") {
                        selected = null
                        scope.launch {
                            if (viewModel.delete(note.id) &&
                                snackbar.showSnackbar("Note deleted", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed
                            ) {
                                viewModel.save(note)
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { note ->
        NoteEditor(note, onDismiss = { editing = null }, onSave = { saved ->
            viewModel.save(saved)
            editing = null
            if (saved.dueAt != null && Build.VERSION.SDK_INT >= 33 && !notificationsEnabled) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        })
    }
}

@Composable
private fun NoteRow(
    note: QuickNote,
    alignment: Alignment.Horizontal,
    edit: () -> Unit,
    options: () -> Unit,
    toggleDone: () -> Unit
) {
    val textAlign = when (alignment) {
        Alignment.End -> TextAlign.End
        Alignment.CenterHorizontally -> TextAlign.Center
        else -> TextAlign.Start
    }
    Row(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = edit, onLongClickLabel = "Note options", onLongClick = options
        ).padding(vertical = 4.dp),
        horizontalArrangement = when (alignment) {
            Alignment.End -> Arrangement.End
            Alignment.CenterHorizontally -> Arrangement.Center
            else -> Arrangement.Start
        }
    ) {
        Box(
            Modifier.size(48.dp).toggleable(
                value = note.done, role = Role.Checkbox, onValueChange = { toggleDone() }
            ).semantics {
                contentDescription = if (note.done) "Mark unfinished" else "Mark done"
            },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(16.dp).border(
                    1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                if (note.done) {
                    Icon(
                        Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f, fill = false).padding(top = 11.dp, bottom = 8.dp),
            horizontalAlignment = alignment
        ) {
            Text(
                text = note.text,
                color = if (note.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign,
                textDecoration = if (note.done) TextDecoration.LineThrough else null
            )
            if (!note.done) note.dueAt?.let { due ->
                val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
                val today = LocalDate.now()
                val label = when (date) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(due))
                }
                Text(
                    "$label, ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(due))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = textAlign
                )
            }
        }
    }
}

@Composable
private fun NoteEditor(note: QuickNote, onDismiss: () -> Unit, onSave: (QuickNote) -> Unit) {
    val context = LocalContext.current
    var text by remember(note.id) { mutableStateOf(note.text) }
    var due by remember(note.id) { mutableStateOf(note.dueAt) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note.text.isEmpty()) "New note or task" else "Edit note", style = MaterialTheme.typography.bodyMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = text, onValueChange = { if (it.length <= 2000) text = it }, label = { Text("Note", style = MaterialTheme.typography.bodySmall) }, textStyle = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 5)
                TextButton(onClick = {
                    val initial = Calendar.getInstance().apply { timeInMillis = due ?: (System.currentTimeMillis() + 3600000) }
                    DatePickerDialog(context, { _, year, month, day ->
                        TimePickerDialog(context, { _, hour, minute ->
                            val selected = Calendar.getInstance().apply {
                                set(year, month, day, hour, minute, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            if (selected <= System.currentTimeMillis()) error = "Choose a future time."
                            else { due = selected; error = null }
                        }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(context)).show()
                    }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                }) { Text(due?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) } ?: "Add reminder") }
                if (due != null) TextButton(onClick = { due = null; error = null }) { Text("Remove reminder") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (due != null) Text("Android may delay notifications when battery restrictions are active.", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = {
            if (due != null && due != note.dueAt && due!! <= System.currentTimeMillis()) error = "Choose a future time."
            else onSave(note.copy(text = text.trim(), dueAt = due, notified = if (due == note.dueAt) note.notified else false))
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
