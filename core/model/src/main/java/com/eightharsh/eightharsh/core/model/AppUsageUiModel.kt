package com.eightharsh.eightharsh.core.model

data class AppUsageUiModel(
    val packageName: String,
    val appName: String,
    val totalTime: Long,
    val usageIncreased: Boolean
)
