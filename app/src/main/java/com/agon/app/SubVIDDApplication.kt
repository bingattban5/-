package com.agon.app

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SubVIDDApplication : Application() {
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
}
