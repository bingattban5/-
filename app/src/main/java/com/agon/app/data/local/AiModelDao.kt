package com.agon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models ORDER BY type, name")
    fun getAllModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE type = :type")
    fun getModelsByType(type: String): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE id = :id")
    suspend fun getModelById(id: String): AiModelEntity?

    @Query("SELECT * FROM ai_models WHERE isDownloaded = 1")
    fun getDownloadedModels(): Flow<List<AiModelEntity>>

    @Query("SELECT SUM(sizeBytes) FROM ai_models WHERE isDownloaded = 1")
    suspend fun getTotalStorageUsed(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AiModelEntity)

    @Update
    suspend fun updateModel(model: AiModelEntity)

    @Delete
    suspend fun deleteModel(model: AiModelEntity)

    @Query("DELETE FROM ai_models WHERE id = :id")
    suspend fun deleteModelById(id: String)

    @Query("UPDATE ai_models SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
