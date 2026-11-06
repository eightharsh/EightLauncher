package com.eightharsh.eightharsh

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class EscapeApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var personalRepository: com.eightharsh.eightharsh.core.domain.personal.PersonalRepository
    private val personalScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val unlockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_USER_PRESENT) {
                personalScope.launch { runCatching { personalRepository.shuffleFavorites() } }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        androidx.core.content.ContextCompat.registerReceiver(this, unlockReceiver,
            android.content.IntentFilter(android.content.Intent.ACTION_USER_PRESENT),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        personalScope.launch {
            runCatching { personalRepository.shuffleFavorites(); personalRepository.refreshReminders() }
        }
    }

    /**
     * WorkManager configuration. The default WorkerFactory can only construct workers with a
     * (Context, WorkerParameters) constructor, so without this our @HiltWorker
     * (ClearOldDataWorker) could never be instantiated and the daily cleanup silently failed.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
