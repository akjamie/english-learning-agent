package com.lingo.learn.domain.repository

import com.lingo.learn.domain.model.PronunciationResult
import java.io.File

interface AsrRepository {
    suspend fun evaluatePronunciation(
        audioFile: File,
        referenceText: String
    ): Result<PronunciationResult>

    fun getOfflineFallbackResult(referenceText: String): PronunciationResult
}
