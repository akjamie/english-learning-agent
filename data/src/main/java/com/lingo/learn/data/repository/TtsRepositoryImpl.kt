package org.akj.lingo.learn.data.repository

import android.content.Context
import com.google.gson.Gson
import org.akj.lingo.learn.data.local.dao.TtsCacheDao
import org.akj.lingo.learn.data.local.entity.TtsCacheEntity
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.*
import org.akj.lingo.learn.domain.repository.TtsRepository
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

    private val gson = Gson()

    override suspend fun getSpeech(text: String, speed: Float, voiceId: String?): Result<File> = withContext(Dispatchers.IO) {
        val ttsUrl = prefs.getTtsBaseUrl()
        val isPlan = ttsUrl.contains("/api/v3/plan/")
        // Plan channel has its own speaker; the legacy speech-01 voiceId must never reach it.
        val selectedVoice = if (isPlan) prefs.getTtsSpeaker() else (voiceId ?: "speech-01")
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

        val authToken = prefs.getAuthToken()
        if (authToken.length < 10) {
            return@withContext Result.failure(Exception("Auth Token is not configured. Please set your API key in the Config screen."))
        }
        if (ttsUrl.isBlank()) {
            return@withContext Result.failure(Exception("TTS URL could not be derived. Please configure your LLM Base URL first."))
        }

        runCatching {
            val bytes = if (isPlan) {
                synthesizePlan(text, selectedVoice, authToken, ttsUrl)
            } else {
                synthesizeLegacy(text, speed, selectedVoice, authToken, ttsUrl)
            }
            if (bytes.isEmpty()) throw Exception("TTS synthesis returned no audio data.")

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
    }

    /** Agent Plan unidirectional TTS: NDJSON response, each line a base64 MP3 chunk. */
    private suspend fun synthesizePlan(
        text: String,
        speaker: String,
        authToken: String,
        ttsUrl: String
    ): ByteArray {
        val resourceId = prefs.getTtsResourceId()
        val request = PlanTtsRequest(
            reqParams = PlanTtsReqParams(
                text = text,
                speaker = speaker,
                audioParams = PlanTtsAudioParams()
            )
        )
        val response = service.planTextToAudio(ttsUrl, authToken, resourceId, request)
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Plan TTS synthesis failed: HTTP ${response.code()} — check your API key and model name.")
        }
        val ndjson = response.body()!!.string()
        val out = java.io.ByteArrayOutputStream()
        ndjson.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val chunk = gson.fromJson(trimmed, PlanTtsChunk::class.java)
            val data = chunk.data
            if (!data.isNullOrEmpty()) {
                out.write(android.util.Base64.decode(data, android.util.Base64.DEFAULT))
            }
        }
        return out.toByteArray()
    }

    private suspend fun synthesizeLegacy(
        text: String,
        speed: Float,
        selectedVoice: String,
        authToken: String,
        ttsUrl: String
    ): ByteArray {
        val apiKey = "Bearer $authToken"
        val request = MinimaxTtsRequest(
            model = prefs.getTtsModel(),
            input = text,
            voice = selectedVoice,
            speed = speed
        )
        val response = service.textToAudio(ttsUrl, apiKey, "", null, request)
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!.bytes()
        }
        throw Exception("Ark TTS synthesis failed: HTTP ${response.code()} — check your API key and model name.")
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
