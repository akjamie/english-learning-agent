package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.PronunciationResult
import java.io.File

interface AsrRepository {
    suspend fun evaluatePronunciation(
        audioFile: File,
        referenceText: String
    ): Result<PronunciationResult>

    fun getOfflineFallbackResult(referenceText: String): PronunciationResult
}
