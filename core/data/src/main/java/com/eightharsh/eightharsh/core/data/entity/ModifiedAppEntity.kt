package com.eightharsh.eightharsh.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modifiedApps")
data class ModifiedAppEntity(
    @PrimaryKey val packageId: String,
    val displayName: String?,
    val isHidden: Boolean,
    val isChallenge: Boolean,
    val favouritePosition: Double?
)