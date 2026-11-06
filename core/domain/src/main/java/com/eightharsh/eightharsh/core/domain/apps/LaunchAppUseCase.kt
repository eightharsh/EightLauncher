package com.eightharsh.eightharsh.core.domain.apps

import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import com.eightharsh.eightharsh.core.model.InstalledApp
import jakarta.inject.Inject

/**
 * Launches an [InstalledApp] and reports the outcome.
 *
 * The platform-specific launch is delegated to [SystemActionsRepository], which handles
 * both the cross-profile `LauncherApps` path and the same-profile fallback.
 *
 * @property systemActionsRepository performs the platform-specific launch.
 */
class LaunchAppUseCase @Inject constructor(
    private val systemActionsRepository: SystemActionsRepository,
) {
    /**
     * Launches [app].
     *
     * Launch failures return false without propagating to the caller.
     *
     * @param app the app to launch.
     * @param onAppOpened invoked with [InstalledApp.packageName] only if the launch succeeds.
     * @return `true` if the app was launched, `false` otherwise.
     */
    operator fun invoke(
        app: InstalledApp,
        onAppOpened: ((String) -> Unit)? = null
    ): Boolean {
        return try {
            systemActionsRepository.launchApp(app)
            onAppOpened?.invoke(app.packageName)
            true
        } catch (e: Exception) {
            false
        }
    }
}
