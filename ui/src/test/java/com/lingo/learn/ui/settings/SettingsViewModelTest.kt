package org.akj.lingo.learn.ui.settings

import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsViewModelTest {

    private class FakeConfigRepository : ConfigRepository {
        private var _baseUrl = "https://ark.cn-beijing.volces.com/api/plan/v3"
        private var _authToken = "test-token"
        private var _groupId = "test-group"
        private var _primaryModel = "glm-5.2"
        private var _fallbackModel = "deepseek-v4-flash"
        private var _ttsModel = "seed-tts-2.0"
        private var _asrModel = "volc.seedasr.sauc.duration"
        private var _asrScoreThreshold = 60
        private var _monthlyTokenLimit = 50000
        private var _language = "en"
        private var _reminderEnabled = true
        private var _reminderHour = 18
        private var _shadowDelayMs = 250
        private var _challengeModeEnabled = false

        override fun getBaseUrl() = _baseUrl
        override fun setBaseUrl(value: String) { _baseUrl = value }
        override fun getAuthToken() = _authToken
        override fun setAuthToken(value: String) { _authToken = value }
        override fun getGroupId() = _groupId
        override fun setGroupId(value: String) { _groupId = value }
        override fun getPrimaryModel() = _primaryModel
        override fun setPrimaryModel(value: String) { _primaryModel = value }
        override fun getFallbackModel() = _fallbackModel
        override fun setFallbackModel(value: String) { _fallbackModel = value }
        override fun getTtsModel() = _ttsModel
        override fun setTtsModel(value: String) { _ttsModel = value }
        override fun getAsrModel() = _asrModel
        override fun setAsrModel(value: String) { _asrModel = value }
        override fun getAsrScoreThreshold() = _asrScoreThreshold
        override fun setAsrScoreThreshold(value: Int) { _asrScoreThreshold = value }
        override fun getMonthlyTokenLimit() = _monthlyTokenLimit
        override fun setMonthlyTokenLimit(value: Int) { _monthlyTokenLimit = value }
        override fun getLanguage() = _language
        override fun setLanguage(value: String) { _language = value }
        override fun isReminderEnabled() = _reminderEnabled
        override fun setReminderEnabled(value: Boolean) { _reminderEnabled = value }
        override fun getReminderHour() = _reminderHour
        override fun setReminderHour(value: Int) { _reminderHour = value }
        override fun getShadowDelayMs() = _shadowDelayMs
        override fun setShadowDelayMs(value: Int) { _shadowDelayMs = value }
        override fun isChallengeModeEnabled() = _challengeModeEnabled
        override fun setChallengeModeEnabled(value: Boolean) { _challengeModeEnabled = value }
    }

    private class FakeLlmRepository(
        private val result: Result<String> = Result.success("OK")
    ) : LlmRepository {
        var lastPrompt: String? = null
        var lastTaskType: String? = null

        override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> {
            lastPrompt = prompt
            lastTaskType = taskType
            return result
        }

        override suspend fun chat(messages: List<ChatMessage>, taskType: String, maxTokens: Int): Result<String> = result
    }

    private fun createViewModel(
        config: FakeConfigRepository = FakeConfigRepository(),
        llm: FakeLlmRepository = FakeLlmRepository()
    ): Pair<SettingsViewModel, FakeConfigRepository> {
        val vm = SettingsViewModel(config, llm)
        return Pair(vm, config)
    }

    @Test
    fun `loadSettings populates UI state from config repository`() {
        val config = FakeConfigRepository().apply {
            setBaseUrl("https://example.com")
            setPrimaryModel("gpt-4")
            setLanguage("zh")
            setReminderHour(20)
        }
        val (vm, _) = createViewModel(config = config)

        val state = vm.uiState.value
        assertEquals("https://example.com", state.baseUrl)
        assertEquals("gpt-4", state.primaryModel)
        assertEquals("zh", state.language)
        assertEquals(20, state.reminderHour)
    }

    @Test
    fun `updateLanguage sets language in UI state`() {
        val (vm, _) = createViewModel()

        vm.updateLanguage("zh")

        assertEquals("zh", vm.uiState.value.language)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun `updateReminderHour clamps to 0-23 range`() {
        val (vm, _) = createViewModel()

        vm.updateReminderHour(25)
        assertEquals(23, vm.uiState.value.reminderHour)

        vm.updateReminderHour(-5)
        assertEquals(0, vm.uiState.value.reminderHour)

        vm.updateReminderHour(12)
        assertEquals(12, vm.uiState.value.reminderHour)
    }

    @Test
    fun `updateReminderEnabled toggles in UI state`() {
        val (vm, _) = createViewModel()

        vm.updateReminderEnabled(false)
        assertFalse(vm.uiState.value.reminderEnabled)

        vm.updateReminderEnabled(true)
        assertTrue(vm.uiState.value.reminderEnabled)
    }

    @Test
    fun `saveSettings persists all values to config repository`() {
        val config = FakeConfigRepository()
        val (vm, _) = createViewModel(config = config)

        vm.updateBaseUrl("https://new-url.com")
        vm.updatePrimaryModel("gpt-4o")
        vm.updateLanguage("zh")
        vm.updateReminderHour(20)
        vm.updateReminderEnabled(false)
        vm.saveSettings()

        assertEquals("https://new-url.com", config.getBaseUrl())
        assertEquals("gpt-4o", config.getPrimaryModel())
        assertEquals("zh", config.getLanguage())
        assertEquals(20, config.getReminderHour())
        assertFalse(config.isReminderEnabled())
        assertTrue(vm.uiState.value.isSaved)
    }

    @Test
    fun `saveSettings trims whitespace from string fields`() {
        val config = FakeConfigRepository()
        val (vm, _) = createViewModel(config = config)

        vm.updateBaseUrl("  https://example.com  ")
        vm.updateAuthToken("  token123  ")
        vm.saveSettings()

        assertEquals("https://example.com", config.getBaseUrl())
        assertEquals("token123", config.getAuthToken())
    }

    @Test
    fun `update methods set isSaved to false`() {
        val (vm, _) = createViewModel()
        vm.saveSettings()
        assertTrue(vm.uiState.value.isSaved)

        vm.updatePrimaryModel("new-model")
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun `updateAsrScoreThreshold updates UI state`() {
        val (vm, _) = createViewModel()

        vm.updateAsrScoreThreshold(80)

        assertEquals(80, vm.uiState.value.asrScoreThreshold)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun `updateMonthlyTokenLimit updates UI state`() {
        val (vm, _) = createViewModel()

        vm.updateMonthlyTokenLimit(100000)

        assertEquals(100000, vm.uiState.value.monthlyTokenLimit)
    }

    @Test
    fun `loadSettings resets isSaved flag`() {
        val (vm, _) = createViewModel()
        vm.saveSettings()
        assertTrue(vm.uiState.value.isSaved)

        vm.loadSettings()
        assertFalse(vm.uiState.value.isSaved)
    }
}
