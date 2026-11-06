package com.eightharsh.eightharsh.core.domain.launcher

import android.content.Intent
import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import jakarta.inject.Inject

class SetDefaultLauncherUseCase @Inject constructor(
    private val systemActionsRepository: SystemActionsRepository
) {
    operator fun invoke(): Intent {
        return systemActionsRepository.getPromptDefaultLauncherIntent()
    }
}
