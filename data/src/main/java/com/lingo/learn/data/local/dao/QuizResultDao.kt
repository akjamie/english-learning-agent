package org.akj.lingo.learn.data.local.dao

import androidx.room.*
import org.akj.lingo.learn.data.local.entity.QuizResultEntity

@Dao
interface QuizResultDao {
    @Query("SELECT * FROM quiz_result ORDER BY timestamp DESC")
    suspend fun getAllQuizResults(): List<QuizResultEntity>

    @Query("SELECT * FROM quiz_result WHERE quizType = :quizType ORDER BY timestamp DESC")
    suspend fun getResultsByType(quizType: String): List<QuizResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity)
}
