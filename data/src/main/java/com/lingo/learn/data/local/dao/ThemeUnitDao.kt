package org.akj.lingo.learn.data.local.dao

import androidx.room.*
import org.akj.lingo.learn.data.local.entity.ThemeUnitEntity

@Dao
interface ThemeUnitDao {
    @Query("SELECT * FROM theme_unit WHERE id = :id LIMIT 1")
    suspend fun getUnitById(id: String): ThemeUnitEntity?

    @Query("SELECT * FROM theme_unit WHERE gradeBand = :gradeBand")
    suspend fun getUnitsByGrade(gradeBand: String): List<ThemeUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<ThemeUnitEntity>)
}
