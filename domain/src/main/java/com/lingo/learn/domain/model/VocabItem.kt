package com.lingo.learn.domain.model

data class VocabItem(
    val id: String,
    val word: String,
    val phoneticSymbol: String,
    val chineseMeaning: String,
    val partOfSpeech: String,
    val imageUrl: String?,
    val audioPath: String?,             // Local TTS MP3 file path
    val exampleSentence: String,
    val exampleSentenceAudio: String?,  // Normal speed TTS path
    val exampleSentenceAudioSlow: String?, // 0.75x slow speed TTS path
    val gradeBand: String,              // Primary / Junior / Senior
    val unit: String,                   // Associated unit name, e.g., "School Life"
    val isBuiltIn: Boolean = true,
    val lastModified: Long = System.currentTimeMillis()
)
