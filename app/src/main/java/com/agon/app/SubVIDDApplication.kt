package com.agon.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SubVIDDApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        
        // تهيئة WorkManager يدوياً (مهم جداً!)
        WorkManager.initialize(this, workManagerConfiguration)
        
        try {
            // تهيئة بيئة yt-dlp و FFmpeg للعمل داخل التطبيق
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("SubVIDDApplication", "Failed to initialize yt-dlp or ffmpeg", e)
        }
    }

    // إعدادات WorkManager
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
