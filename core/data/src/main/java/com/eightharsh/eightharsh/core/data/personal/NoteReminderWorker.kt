package com.eightharsh.eightharsh.core.data.personal

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.eightharsh.eightharsh.core.domain.personal.PersonalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NoteReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val personal: PersonalRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("noteId") ?: return Result.failure()
        val due = inputData.getLong("dueAt", 0)
        val note = personal.notes.first().firstOrNull { it.id == id } ?: return Result.success()
        if (note.done || note.notified || note.dueAt != due) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val notifications = NotificationManagerCompat.from(applicationContext)
        if (!notifications.areNotificationsEnabled()) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("quick-notes", "Task reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: return Result.failure()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("showQuickNotes", true)
        val pending = PendingIntent.getActivity(applicationContext, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(name: String): PendingIntent {
            val actionIntent = Intent(applicationContext, NoteReminderActionReceiver::class.java)
                .setAction(name)
                .setData(Uri.Builder().scheme("eightharsh-reminder").authority(name)
                    .appendPath(id).appendPath(due.toString()).build())
                .putExtra("noteId", id)
                .putExtra("dueAt", due)
            return PendingIntent.getBroadcast(applicationContext, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(applicationContext, "quick-notes")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("eight reminder")
            .setContentText(note.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "Done", action("done"))
            .addAction(0, "Snooze 10 min", action("snooze"))
            .setContentIntent(pending).setAutoCancel(true).build()
        try { notifications.notify(id, 1, notification) } catch (_: SecurityException) { return Result.success() }
        personal.markNotified(id, due)
        return Result.success()
    }
}
