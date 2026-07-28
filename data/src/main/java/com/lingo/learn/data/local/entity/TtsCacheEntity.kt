package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.TtsCacheEntry

@Entity(tableName = "tts_cache")
data class TtsCacheEntity(
    @PrimaryKey val cacheKey: String,
    val text: String,
    val speed: Float,
    val voiceId: String,
    val filePath: String,
    val createdTimestamp: Long,
    val lastAccessedTimestamp: Long,
    val fileSize: Long
) {
    fun toDomain() = TtsCacheEntry(
        cacheKey = cacheKey,
        text = text,
        speed = speed,
        voiceId = voiceId,
        filePath = filePath,
        createdTimestamp = createdTimestamp,
        lastAccessedTimestamp = lastAccessedTimestamp,
        fileSize = fileSize
    )

    companion object {
        fun fromDomain(domain: TtsCacheEntry) = TtsCacheEntity(
            cacheKey = domain.cacheKey,
            text = domain.text,
            speed = domain.speed,
            voiceId = domain.voiceId,
            filePath = domain.filePath,
            createdTimestamp = domain.createdTimestamp,
            lastAccessedTimestamp = domain.lastAccessedTimestamp,
            fileSize = domain.fileSize
        )
    }
}
