package com.eightharsh.eightharsh.core.domain.repository.settings

interface SettingsManager {
    suspend fun resetToDefaults()
}
