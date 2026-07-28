package org.akj.lingo.learn.domain.repository

import java.io.File

interface TtsRepository {
    suspend fun getSpeech(
        text: String,
        speed: Float = 1.0f,
        voiceId: String? = null
    ): Result<File>

    suspend fun preGenerateBatch(texts: List<String>): Result<Unit>
}
