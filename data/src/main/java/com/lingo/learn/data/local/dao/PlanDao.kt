package com.lingo.learn.data.local.dao

import androidx.room.*
import com.lingo.learn.data.local.entity.PlanEntity

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan WHERE type = :type ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestPlan(type: String): PlanEntity?

    @Query("SELECT * FROM plan WHERE startDate <= :timestamp AND endDate >= :timestamp LIMIT 1")
    suspend fun getPlanForTime(timestamp: Long): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)
}
