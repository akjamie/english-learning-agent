package org.akj.lingo.learn.data.prefs

import org.akj.lingo.learn.domain.provider.ProviderEndpoints
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureConfigPrefs @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        "secure_config_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getBaseUrl(): String {
        val saved = prefs.getString(KEY_BASE_URL, "") ?: ""
        // Sprint 9: migrate a legacy value that missed the /v3 gateway segment.
        val normalized = ProviderEndpoints.normalizeBaseUrl(saved)
        if (normalized != saved.trim()) {
            prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        }
        return normalized
    }
    fun setBaseUrl(value: String) = prefs.edit().putString(KEY_BASE_URL, value.trim()).apply()

    fun getAuthToken(): String {
        val saved = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        return if (saved.isBlank()) "" else saved
    }
    fun setAuthToken(value: String) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    fun getGroupId(): String = prefs.getString(KEY_GROUP_ID, "") ?: ""
    fun setGroupId(value: String) = prefs.edit().putString(KEY_GROUP_ID, value).apply()

    fun getPrimaryModel(): String = prefs.getString(KEY_PRIMARY_MODEL, "glm-5.2") ?: "glm-5.2"
    fun setPrimaryModel(value: String) = prefs.edit().putString(KEY_PRIMARY_MODEL, value).apply()

    fun getFallbackModel(): String = prefs.getString(KEY_FALLBACK_MODEL, "deepseek-v4-flash") ?: "deepseek-v4-flash"
    fun setFallbackModel(value: String) = prefs.edit().putString(KEY_FALLBACK_MODEL, value).apply()

    fun getTtsModel(): String = prefs.getString(KEY_TTS_MODEL, "seed-tts-2.0") ?: "seed-tts-2.0"
    fun setTtsModel(value: String) = prefs.edit().putString(KEY_TTS_MODEL, value).apply()

    fun getAsrModel(): String = prefs.getString(KEY_ASR_MODEL, "volc.seedasr.sauc.duration") ?: "volc.seedasr.sauc.duration"
    fun setAsrModel(value: String) = prefs.edit().putString(KEY_ASR_MODEL, value).apply()

    fun getTtsSpeedNormal(): Float = prefs.getFloat(KEY_TTS_SPEED_NORMAL, 1.0f)
    fun setTtsSpeedNormal(value: Float) = prefs.edit().putFloat(KEY_TTS_SPEED_NORMAL, value).apply()

    fun getTtsSpeedSlow(): Float = prefs.getFloat(KEY_TTS_SPEED_SLOW, 0.75f)
    fun setTtsSpeedSlow(value: Float) = prefs.edit().putFloat(KEY_TTS_SPEED_SLOW, value).apply()

    fun getAsrScoreThreshold(): Int = prefs.getInt(KEY_ASR_SCORE_THRESHOLD, 60)
    fun setAsrScoreThreshold(value: Int) = prefs.edit().putInt(KEY_ASR_SCORE_THRESHOLD, value).apply()

    fun getMonthlyTokenLimit(): Int = prefs.getInt(KEY_MONTHLY_TOKEN_LIMIT, 50000)
    fun setMonthlyTokenLimit(value: Int) = prefs.edit().putInt(KEY_MONTHLY_TOKEN_LIMIT, value).apply()

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    fun setLanguage(value: String) {
        prefs.edit().putString(KEY_LANGUAGE, value).apply()
        // Mirror to plain prefs for attachBaseContext (Hilt not yet injected at that point)
        appContext.getSharedPreferences("lingo_lang_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_language", value).apply()
    }

    // Sprint 12: daily reminder preferences (used by DailyReminderWorker)
    fun isReminderEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
    fun setReminderEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()
        // Mirror to plain prefs so WorkManager (non-Hilt) can read it without the crypto dependency.
        appContext.getSharedPreferences("lingo_reminder_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("reminder_enabled", value).apply()
    }

    /** Hour of day (0-23) for the daily reminder. Default 18:00. */
    fun getReminderHour(): Int = prefs.getInt(KEY_REMINDER_HOUR, 18)
    fun setReminderHour(value: Int) = prefs.edit().putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23)).apply()

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_GROUP_ID = "group_id"
        private const val KEY_PRIMARY_MODEL = "primary_model"
        private const val KEY_FALLBACK_MODEL = "fallback_model"
        private const val KEY_TTS_MODEL = "tts_model"
        private const val KEY_ASR_MODEL = "asr_model"
        private const val KEY_TTS_SPEED_NORMAL = "tts_speed_normal"
        private const val KEY_TTS_SPEED_SLOW = "tts_speed_slow"
        private const val KEY_ASR_SCORE_THRESHOLD = "asr_score_threshold"
        private const val KEY_MONTHLY_TOKEN_LIMIT = "monthly_token_limit"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
    }
}
