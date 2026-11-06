package com.eightharsh.eightharsh.core.domain.launcher

import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import jakarta.inject.Inject

class GetIsDefaultLauncherUseCase @Inject constructor(
    private val systemActionsRepository: SystemActionsRepository
) {
    operator fun invoke(): Boolean {
        return systemActionsRepository.isDefaultLauncher()
    }
}
