package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val prefs: SecureConfigPrefs
) : ConfigRepository {

    override fun getBaseUrl(): String = prefs.getBaseUrl()
    override fun setBaseUrl(value: String) = prefs.setBaseUrl(value)

    override fun getAuthToken(): String = prefs.getAuthToken()
    override fun setAuthToken(value: String) = prefs.setAuthToken(value)

    override fun getGroupId(): String = prefs.getGroupId()
    override fun setGroupId(value: String) = prefs.setGroupId(value)

    override fun getPrimaryModel(): String = prefs.getPrimaryModel()
    override fun setPrimaryModel(value: String) = prefs.setPrimaryModel(value)

    override fun getFallbackModel(): String = prefs.getFallbackModel()
    override fun setFallbackModel(value: String) = prefs.setFallbackModel(value)

    override fun getTtsModel(): String = prefs.getTtsModel()
    override fun setTtsModel(value: String) = prefs.setTtsModel(value)

    override fun getAsrModel(): String = prefs.getAsrModel()
    override fun setAsrModel(value: String) = prefs.setAsrModel(value)

    override fun getTtsBaseUrl(): String = prefs.getTtsBaseUrl()
    override fun setTtsBaseUrl(value: String) = prefs.setTtsBaseUrl(value)

    override fun getTtsResourceId(): String = prefs.getTtsResourceId()
    override fun setTtsResourceId(value: String) = prefs.setTtsResourceId(value)

    override fun getTtsSpeaker(): String = prefs.getTtsSpeaker()
    override fun setTtsSpeaker(value: String) = prefs.setTtsSpeaker(value)

    override fun getAsrResourceId(): String = prefs.getAsrResourceId()
    override fun setAsrResourceId(value: String) = prefs.setAsrResourceId(value)

    override fun getAsrWsUrl(): String = prefs.getAsrWsUrl()
    override fun setAsrWsUrl(value: String) = prefs.setAsrWsUrl(value)

    override fun getAsrScoreThreshold(): Int = prefs.getAsrScoreThreshold()
    override fun setAsrScoreThreshold(value: Int) = prefs.setAsrScoreThreshold(value)

    override fun getMonthlyTokenLimit(): Int = prefs.getMonthlyTokenLimit()
    override fun setMonthlyTokenLimit(value: Int) = prefs.setMonthlyTokenLimit(value)

    override fun getLanguage(): String = prefs.getLanguage()
    override fun setLanguage(value: String) = prefs.setLanguage(value)

    override fun isReminderEnabled(): Boolean = prefs.isReminderEnabled()
    override fun setReminderEnabled(value: Boolean) = prefs.setReminderEnabled(value)

    override fun getReminderHour(): Int = prefs.getReminderHour()
    override fun setReminderHour(value: Int) = prefs.setReminderHour(value)

    override fun getShadowDelayMs(): Int = prefs.getShadowDelayMs()
    override fun setShadowDelayMs(value: Int) = prefs.setShadowDelayMs(value)

    override fun isChallengeModeEnabled(): Boolean = prefs.isChallengeModeEnabled()
    override fun setChallengeModeEnabled(value: Boolean) = prefs.setChallengeModeEnabled(value)
}
