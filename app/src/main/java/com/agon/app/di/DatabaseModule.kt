package com.agon.app.di

import android.content.Context
import androidx.room.Room
import com.agon.app.data.local.AiModelDao
import com.agon.app.data.local.AppDatabase
import com.agon.app.data.local.DownloadDao
import com.agon.app.data.local.SettingsDao
import com.agon.app.data.local.SubtitleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "subvidd_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: AppDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideSubtitleDao(database: AppDatabase): SubtitleDao {
        return database.subtitleDao()
    }

    @Provides
    @Singleton
    fun provideAiModelDao(database: AppDatabase): AiModelDao {
        return database.aiModelDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }
}
