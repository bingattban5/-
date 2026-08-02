package com.agon.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DownloadEntity::class,
        SubtitleEntity::class,
        AiModelEntity::class,
        SettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun subtitleDao(): SubtitleDao
    abstract fun aiModelDao(): AiModelDao
    abstract fun settingsDao(): SettingsDao
}
