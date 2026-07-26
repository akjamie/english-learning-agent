package com.lingo.learn.data.local.dao

import androidx.room.*
import com.lingo.learn.data.local.entity.ErrorBookEntity

@Dao
interface ErrorBookDao {
    @Query("SELECT * FROM error_book WHERE status != 'GRADUATED' ORDER BY priorityScore DESC LIMIT 30")
    suspend fun getTop30Errors(): List<ErrorBookEntity>

    @Query("SELECT * FROM error_book WHERE vocabId = :vocabId LIMIT 1")
    suspend fun getErrorByVocabId(vocabId: String): ErrorBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(errorEntry: ErrorBookEntity)

    @Update
    suspend fun update(errorEntry: ErrorBookEntity)

    @Query("SELECT * FROM error_book WHERE status = 'GRADUATION_OBSERVATION'")
    suspend fun getErrorsInObservation(): List<ErrorBookEntity>
}
