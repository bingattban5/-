package com.agon.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SubVIDDApplication : Application(), Configuration.Provider {

    // حقن مصنع الـ Workers الخاص بـ Hilt (ضروري لعمل DownloadWorker)
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            // تهيئة بيئة yt-dlp و FFmpeg للعمل داخل التطبيق
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("SubVIDDApplication", "Failed to initialize yt-dlp or ffmpeg", e)
        }
    }

    // إعدادات WorkManager مع مصنع Hilt
    // (لا نحتاج WorkManager.initialize يدوياً لأن Manifest يعطل التهيئة الافتراضية
    //  وسيقوم WorkManager بتهيئة نفسه عند أول استخدام عبر Configuration.Provider)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
