package org.akj.lingo.learn.domain.repository

/**
 * Clean Architecture domain repository interface for configuration settings.
 */
interface ConfigRepository {
    fun getBaseUrl(): String
    fun setBaseUrl(value: String)

    fun getAuthToken(): String
    fun setAuthToken(value: String)

    fun getGroupId(): String
    fun setGroupId(value: String)

    fun getPrimaryModel(): String
    fun setPrimaryModel(value: String)

    fun getFallbackModel(): String
    fun setFallbackModel(value: String)

    fun getTtsModel(): String
    fun setTtsModel(value: String)

    fun getAsrModel(): String
    fun setAsrModel(value: String)

    fun getTtsBaseUrl(): String
    fun setTtsBaseUrl(value: String)

    fun getTtsResourceId(): String
    fun setTtsResourceId(value: String)

    fun getTtsSpeaker(): String
    fun setTtsSpeaker(value: String)

    fun getAsrResourceId(): String
    fun setAsrResourceId(value: String)

    fun getAsrWsUrl(): String
    fun setAsrWsUrl(value: String)

    fun getAsrScoreThreshold(): Int
    fun setAsrScoreThreshold(value: Int)

    fun getMonthlyTokenLimit(): Int
    fun setMonthlyTokenLimit(value: Int)

    fun getLanguage(): String
    fun setLanguage(value: String)

    fun isReminderEnabled(): Boolean
    fun setReminderEnabled(value: Boolean)

    fun getReminderHour(): Int
    fun setReminderHour(value: Int)

    fun getShadowDelayMs(): Int
    fun setShadowDelayMs(value: Int)

    fun isChallengeModeEnabled(): Boolean
    fun setChallengeModeEnabled(value: Boolean)
}
