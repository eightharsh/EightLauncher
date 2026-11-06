package com.eightharsh.eightharsh.core.domain.system

import com.eightharsh.eightharsh.core.domain.repository.android.SystemActionsRepository
import jakarta.inject.Inject

class SetWallpaperUseCase @Inject constructor(
    private val systemActionsRepository: SystemActionsRepository
) {
    operator fun invoke(color: Int) {
        systemActionsRepository.setSolidColorWallpaper(color)
    }
}
