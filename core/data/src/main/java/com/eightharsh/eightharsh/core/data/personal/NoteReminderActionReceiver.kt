package com.eightharsh.eightharsh.core.data.personal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NoteReminderActionReceiver : BroadcastReceiver() {
    @Inject lateinit var personal: PersonalRepositoryImpl

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("noteId") ?: return
        val due = intent.getLongExtra("dueAt", 0L)
        val snooze = when (intent.action) {
            "done" -> false
            "snooze" -> true
            else -> return
        }
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                personal.actOnReminder(id, due, snooze)
            } catch (e: Exception) {
                Log.e("NoteReminderAction", "Unable to update reminder", e)
            } finally {
                result.finish()
            }
        }
    }
}
