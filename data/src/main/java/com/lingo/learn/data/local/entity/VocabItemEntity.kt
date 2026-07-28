package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.VocabItem

@Entity(tableName = "vocab_item")
data class VocabItemEntity(
    @PrimaryKey val id: String,
    val word: String,
    val phoneticSymbol: String,
    val chineseMeaning: String,
    val partOfSpeech: String,
    val imageUrl: String?,
    val audioPath: String?,
    val exampleSentence: String,
    val exampleSentenceAudio: String?,
    val exampleSentenceAudioSlow: String?,
    val gradeBand: String,
    val unit: String,
    val isBuiltIn: Boolean,
    val lastModified: Long
) {
    fun toDomain() = VocabItem(
        id = id,
        word = word,
        phoneticSymbol = phoneticSymbol,
        chineseMeaning = chineseMeaning,
        partOfSpeech = partOfSpeech,
        imageUrl = imageUrl,
        audioPath = audioPath,
        exampleSentence = exampleSentence,
        exampleSentenceAudio = exampleSentenceAudio,
        exampleSentenceAudioSlow = exampleSentenceAudioSlow,
        gradeBand = gradeBand,
        unit = unit,
        isBuiltIn = isBuiltIn,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: VocabItem) = VocabItemEntity(
            id = domain.id,
            word = domain.word,
            phoneticSymbol = domain.phoneticSymbol,
            chineseMeaning = domain.chineseMeaning,
            partOfSpeech = domain.partOfSpeech,
            imageUrl = domain.imageUrl,
            audioPath = domain.audioPath,
            exampleSentence = domain.exampleSentence,
            exampleSentenceAudio = domain.exampleSentenceAudio,
            exampleSentenceAudioSlow = domain.exampleSentenceAudioSlow,
            gradeBand = domain.gradeBand,
            unit = domain.unit,
            isBuiltIn = domain.isBuiltIn,
            lastModified = domain.lastModified
        )
    }
}
