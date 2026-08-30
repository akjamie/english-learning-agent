package org.akj.lingo.learn.ui.onboarding

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.TtsRepository
import org.akj.lingo.learn.domain.usecase.AgentJsonValidator
import org.akj.lingo.learn.domain.usecase.AgentPromptRegistry
import org.akj.lingo.learn.domain.usecase.StructuredLlmUseCase
import org.akj.lingo.learn.ui.learning.SystemTtsHelper
import org.akj.lingo.learn.ui.learning.VoiceRecorder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var asrRepository: AsrRepository
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var systemTtsHelper: SystemTtsHelper
    private lateinit var ttsRepository: TtsRepository
    private lateinit var configRepository: ConfigRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = Mockito.mock(Context::class.java)
        asrRepository = Mockito.mock(AsrRepository::class.java)
        voiceRecorder = Mockito.mock(VoiceRecorder::class.java)
        systemTtsHelper = Mockito.mock(SystemTtsHelper::class.java)
        ttsRepository = Mockito.mock(TtsRepository::class.java)
        configRepository = Mockito.mock(ConfigRepository::class.java)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(llmRepository: LlmRepository): DiagnosisViewModel {
        val validator = AgentJsonValidator()
        val structuredLlm = StructuredLlmUseCase(llmRepository, validator)
        val promptRegistry = AgentPromptRegistry()

        return DiagnosisViewModel(
            context = context,
            structuredLlmUseCase = structuredLlm,
            llmRepository = llmRepository,
            promptRegistry = promptRegistry,
            asrRepository = asrRepository,
            voiceRecorder = voiceRecorder,
            systemTtsHelper = systemTtsHelper,
            ttsRepository = ttsRepository,
            configRepository = configRepository
        )
    }

    @Test
    fun `parseProgressiveQuestions extracts completed objects from partial JSON array`() {
        val fakeLlm = object : LlmRepository {
            override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> = Result.success("[]")
            override suspend fun chat(messages: List<ChatMessage>, taskType: String, maxTokens: Int): Result<String> = Result.success("[]")
            override fun completeStream(prompt: String, taskType: String, maxTokens: Int) = kotlinx.coroutines.flow.emptyFlow<String>()
        }
        val viewModel = createViewModel(fakeLlm)

        val partialJson = """
            [
              {
                "id": 1,
                "type": "VOCABULARY",
                "title": "Question 1",
                "description": "Select the right word",
                "options": ["apple", "banana"],
                "correctAnswer": "apple"
              },
              {
                "id": 2,
                "type": "SPEAK_ALOUD",
                "title": "Question 2",
                "description": "Say it",
                "voicePrompt": "good morning"
              
        """.trimIndent()

        val parsed = viewModel.parseProgressiveQuestions(partialJson)
        assertEquals(1, parsed.size)
        assertEquals(1, parsed[0].id)
        assertEquals("Question 1", parsed[0].title)
        assertEquals("apple", parsed[0].correctAnswer)
    }

    @Test
    fun `loadDiagnosticQuestions loads questions progressively from stream`() = runTest(testDispatcher) {
        val q1 = """{"id":1,"type":"VOCABULARY","title":"Q1","description":"Desc1","options":["a","b"],"correctAnswer":"a"}"""
        val q2 = """{"id":2,"type":"PHONICS","title":"Q2","description":"Desc2","options":["c","d"],"correctAnswer":"c"}"""

        val fakeLlm = object : LlmRepository {
            override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> =
                Result.success("[$q1,$q2]")

            override suspend fun chat(messages: List<ChatMessage>, taskType: String, maxTokens: Int): Result<String> =
                Result.success("[$q1,$q2]")

            override fun completeStream(prompt: String, taskType: String, maxTokens: Int) = flow {
                emit("[")
                emit(q1)
                emit(",")
                emit(q2)
                emit("]")
            }
        }

        val viewModel = createViewModel(fakeLlm)
        viewModel.loadDiagnosticQuestions("Grade 3")

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
        assertEquals(2, viewModel.questions.value.size)
        assertEquals("Q1", viewModel.questions.value[0].title)
        assertEquals("Q2", viewModel.questions.value[1].title)
    }
}
