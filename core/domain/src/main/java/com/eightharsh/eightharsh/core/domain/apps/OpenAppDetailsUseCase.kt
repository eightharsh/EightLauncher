package com.eightharsh.eightharsh.core.domain.apps

import android.graphics.Rect
import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import com.eightharsh.eightharsh.core.model.InstalledApp
import jakarta.inject.Inject

class OpenAppDetailsUseCase @Inject constructor(
    private val systemActionsRepository: SystemActionsRepository
) {
    operator fun invoke(app: InstalledApp, sourceBounds: Rect? = null) {
        systemActionsRepository.openAppDetails(app, sourceBounds)
    }
}
