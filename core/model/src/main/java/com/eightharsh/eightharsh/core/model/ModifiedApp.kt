package com.eightharsh.eightharsh.core.model

data class ModifiedApp(
    val packageId: String,
    val displayName: String?,
    val isHidden: Boolean,
    val isChallenge: Boolean,
    val favouritePosition: Double?
)
