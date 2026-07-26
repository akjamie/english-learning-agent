package com.lingo.learn.data.repository

import android.content.Context
import com.lingo.learn.data.local.dao.TtsCacheDao
import com.lingo.learn.data.local.entity.TtsCacheEntity
import com.lingo.learn.data.prefs.SecureConfigPrefs
import com.lingo.learn.data.remote.minimax.*
import com.lingo.learn.domain.repository.TtsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: MinimaxService,
    private val prefs: SecureConfigPrefs,
    private val ttsCacheDao: TtsCacheDao
) : TtsRepository {

    override suspend fun getSpeech(text: String, speed: Float, voiceId: String?): Result<File> = withContext(Dispatchers.IO) {
        val selectedVoice = voiceId ?: "speech-01"
        val cacheKey = generateCacheKey(text, speed, selectedVoice)

        // 1. Query cache
        val cachedEntry = ttsCacheDao.getCacheByKey(cacheKey)
        if (cachedEntry != null) {
            val file = File(cachedEntry.filePath)
            if (file.exists()) {
                ttsCacheDao.updateLastAccessed(cacheKey, System.currentTimeMillis())
                return@withContext Result.success(file)
            } else {
                ttsCacheDao.delete(cachedEntry)
            }
        }

        // 2. Call API for synthesis
        val authToken = prefs.getAuthToken()
        val apiKey = "Bearer $authToken"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val url = baseUrl.trimEnd('/') + "/v1/t2a_v2"
        val ttsModel = prefs.getTtsModel()

        if (authToken.length < 10 || groupId.isEmpty()) {
            return@withContext Result.failure(Exception("MiniMax Auth Token or Group ID is not configured"))
        }

        runCatching {
            val request = MinimaxTtsRequest(
                model = ttsModel,
                text = text,
                voiceSetting = MinimaxVoiceSetting(
                    voiceId = selectedVoice,
                    speed = speed
                )
            )
            val response = service.textToAudio(url, apiKey, groupId, request)
            if (response.isSuccessful && response.body() != null) {
                val bytes = response.body()!!.bytes()

                // Save as a local file
                val ttsDir = File(context.cacheDir, "tts").apply { if (!exists()) mkdirs() }
                val destFile = File(ttsDir, "$cacheKey.mp3")
                FileOutputStream(destFile).use { fos ->
                    fos.write(bytes)
                }

                // Write cache metadata to database
                ttsCacheDao.insertOrUpdate(
                    TtsCacheEntity(
                        cacheKey = cacheKey,
                        text = text,
                        speed = speed,
                        voiceId = selectedVoice,
                        filePath = destFile.absolutePath,
                        createdTimestamp = System.currentTimeMillis(),
                        lastAccessedTimestamp = System.currentTimeMillis(),
                        fileSize = bytes.size.toLong()
                    )
                )

                // 3. Evict old cache items using LRU policy
                checkAndEvictCache()

                return@runCatching destFile
            }
            throw Exception("MiniMax TTS synthesis failed: ${response.code()}")
        }
    }

    override suspend fun preGenerateBatch(texts: List<String>): Result<Unit> {
        // Fetch sequentially in background for simplicity in MVP V1.0
        for (text in texts) {
            getSpeech(text)
        }
        return Result.success(Unit)
    }

    private fun generateCacheKey(text: String, speed: Float, voiceId: String): String {
        val input = "$text|$speed|$voiceId"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun checkAndEvictCache() {
        val maxCacheSize = 200 * 1024 * 1024L // Limit cache size to 200MB
        val caches = ttsCacheDao.getAllCacheSortedByOldest()
        var currentTotalSize = caches.sumOf { it.fileSize }

        if (currentTotalSize > maxCacheSize) {
            for (cache in caches) {
                val file = File(cache.filePath)
                if (file.exists()) {
                    val size = file.length()
                    if (file.delete()) {
                        ttsCacheDao.delete(cache)
                        currentTotalSize -= size
                    }
                } else {
                    ttsCacheDao.delete(cache)
                }
                if (currentTotalSize <= maxCacheSize) break
            }
        }
    }
}
