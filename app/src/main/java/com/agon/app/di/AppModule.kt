package com.agon.app.di

import android.content.Context
import com.agon.app.data.AppPreferences
import com.agon.app.engine.AiModelManager
import com.agon.app.engine.ArgosTranslateEngine
import com.agon.app.engine.FFmpegEngine
import com.agon.app.engine.FileManager
import com.agon.app.engine.WhisperEngine
import com.agon.app.engine.YtDlpEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideYtDlpEngine(@ApplicationContext context: Context): YtDlpEngine {
        return YtDlpEngine(context)
    }

    @Provides
    @Singleton
    fun provideArgosTranslateEngine(@ApplicationContext context: Context): ArgosTranslateEngine {
        return ArgosTranslateEngine(context)
    }

    @Provides
    @Singleton
    fun provideAiModelManager(@ApplicationContext context: Context): AiModelManager {
        return AiModelManager(context)
    }

    @Provides
    @Singleton
    fun provideFFmpegEngine(@ApplicationContext context: Context): FFmpegEngine {
        return FFmpegEngine(context)
    }

    @Provides
    @Singleton
    fun provideWhisperEngine(@ApplicationContext context: Context): WhisperEngine {
        return WhisperEngine(context)
    }

    @Provides
    @Singleton
    fun provideFileManager(@ApplicationContext context: Context): FileManager {
        return FileManager(context)
    }
}
