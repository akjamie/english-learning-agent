package org.akj.lingo.learn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.akj.lingo.learn.data.local.entity.ConversationHistoryEntity

@Dao
interface ConversationHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationHistoryEntity)

    @Query("SELECT * FROM conversation_history WHERE scenarioId = :scenarioId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(scenarioId: String, limit: Int): List<ConversationHistoryEntity>

    @Query("DELETE FROM conversation_history WHERE scenarioId = :scenarioId")
    suspend fun clearScenario(scenarioId: String)
}
