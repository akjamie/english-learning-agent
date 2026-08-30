package org.akj.lingo.learn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.akj.lingo.learn.data.local.entity.CurriculumUnitEntity

/** Data access for the curriculum_unit table. */
@Dao
interface CurriculumDao {

    @Query("SELECT * FROM curriculum_unit WHERE id = :unitId LIMIT 1")
    suspend fun getById(unitId: String): CurriculumUnitEntity?

    @Query("SELECT * FROM curriculum_unit WHERE gradeBand = :gradeBand AND weekNumber = :weekNumber LIMIT 1")
    suspend fun getByGradeAndWeek(gradeBand: String, weekNumber: Int): CurriculumUnitEntity?

    @Query("SELECT * FROM curriculum_unit WHERE gradeBand = :gradeBand ORDER BY weekNumber ASC")
    suspend fun getAllForGrade(gradeBand: String): List<CurriculumUnitEntity>

    @Query("SELECT * FROM curriculum_unit WHERE gradeBand = :gradeBand AND isAvailableOffline = 1 ORDER BY weekNumber ASC")
    suspend fun getOfflineUnits(gradeBand: String): List<CurriculumUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unit: CurriculumUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(units: List<CurriculumUnitEntity>)

    @Query("SELECT contentVersion FROM curriculum_unit WHERE id = :unitId LIMIT 1")
    suspend fun getContentVersion(unitId: String): String?

    @Query("SELECT COUNT(*) FROM curriculum_unit WHERE gradeBand = :gradeBand")
    suspend fun countForGrade(gradeBand: String): Int
}
