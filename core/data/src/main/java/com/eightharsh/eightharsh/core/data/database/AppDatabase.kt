package com.eightharsh.eightharsh.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eightharsh.eightharsh.core.data.entity.AppUsageEntity

@Database(entities = [AppUsageEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
}
