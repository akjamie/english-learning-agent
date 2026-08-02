package org.akj.lingo.learn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.akj.lingo.learn.data.local.entity.GamificationStateEntity

@Dao
interface GamificationStateDao {
    @Query("SELECT * FROM gamification_state LIMIT 1")
    suspend fun getState(): GamificationStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(entity: GamificationStateEntity)
}
