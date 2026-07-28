package org.akj.lingo.learn.data.local.dao

import androidx.room.*
import org.akj.lingo.learn.data.local.entity.VocabItemEntity

@Dao
interface VocabItemDao {
    @Query("SELECT * FROM vocab_item WHERE id = :id LIMIT 1")
    suspend fun getVocabById(id: String): VocabItemEntity?

    @Query("SELECT * FROM vocab_item WHERE unit = :unit")
    suspend fun getVocabsByUnit(unit: String): List<VocabItemEntity>

    @Query("SELECT * FROM vocab_item WHERE gradeBand = :gradeBand")
    suspend fun getVocabsByGradeBand(gradeBand: String): List<VocabItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabs(vocabs: List<VocabItemEntity>)
}
