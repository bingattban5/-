package com.agon.app.data.local

import androidx.room.TypeConverter
import com.agon.app.data.DownloadMode
import com.agon.app.data.DownloadStatus
import com.agon.app.data.SubtitleMethod

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return DownloadStatus.valueOf(value)
    }

    @TypeConverter
    fun fromSubtitleMethod(method: SubtitleMethod): String {
        return method.name
    }

    @TypeConverter
    fun toSubtitleMethod(value: String): SubtitleMethod {
        return SubtitleMethod.valueOf(value)
    }

    @TypeConverter
    fun fromDownloadMode(mode: DownloadMode): String {
        return mode.name
    }

    @TypeConverter
    fun toDownloadMode(value: String): DownloadMode {
        return DownloadMode.valueOf(value)
    }
}
