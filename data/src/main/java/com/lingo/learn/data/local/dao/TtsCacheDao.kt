package com.lingo.learn.data.local.dao

import androidx.room.*
import com.lingo.learn.data.local.entity.TtsCacheEntity

@Dao
interface TtsCacheDao {
    @Query("SELECT * FROM tts_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCacheByKey(key: String): TtsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: TtsCacheEntity)

    @Query("UPDATE tts_cache SET lastAccessedTimestamp = :timestamp WHERE cacheKey = :key")
    suspend fun updateLastAccessed(key: String, timestamp: Long)

    @Query("SELECT * FROM tts_cache ORDER BY lastAccessedTimestamp ASC")
    suspend fun getAllCacheSortedByOldest(): List<TtsCacheEntity>

    @Delete
    suspend fun delete(cache: TtsCacheEntity)
}
