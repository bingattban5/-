package com.agon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleDao {
    @Query("SELECT * FROM subtitles ORDER BY createdAt DESC")
    fun getAllSubtitles(): Flow<List<SubtitleEntity>>

    @Query("SELECT * FROM subtitles WHERE downloadId = :downloadId")
    fun getSubtitlesByDownloadId(downloadId: String): Flow<List<SubtitleEntity>>

    @Query("SELECT * FROM subtitles WHERE id = :id")
    suspend fun getSubtitleById(id: String): SubtitleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(subtitle: SubtitleEntity)

    @Update
    suspend fun updateSubtitle(subtitle: SubtitleEntity)

    @Delete
    suspend fun deleteSubtitle(subtitle: SubtitleEntity)

    @Query("DELETE FROM subtitles WHERE downloadId = :downloadId")
    suspend fun deleteSubtitlesByDownloadId(downloadId: String)
}
