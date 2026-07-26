package com.lingo.learn.domain.model

data class TtsCacheEntry(
    val cacheKey: String,              // hash(text + speed + voiceId)
    val text: String,
    val speed: Float,
    val voiceId: String,
    val filePath: String,              // MP3 file path
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastAccessedTimestamp: Long = System.currentTimeMillis(),
    val fileSize: Long
)
