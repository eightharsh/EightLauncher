package com.eightharsh.eightharsh.core.domain.apps

import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import com.eightharsh.eightharsh.core.model.InstalledApp
import jakarta.inject.Inject

class UninstallAppUseCase @Inject constructor(
    private val systemActionsRepository: SystemActionsRepository
) {
    operator fun invoke(app: InstalledApp) {
        systemActionsRepository.uninstallApp(app)
    }
}
