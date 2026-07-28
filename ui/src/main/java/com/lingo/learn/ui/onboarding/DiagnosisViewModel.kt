package org.akj.lingo.learn.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.model.PronunciationResult
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val llmRepository: LlmRepository,
    private val asrRepository: AsrRepository
) : ViewModel() {

    private val _questions = MutableStateFlow<List<DiagnosticQuestion>>(emptyList())
    val questions: StateFlow<List<DiagnosticQuestion>> = _questions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadDiagnosticQuestions(grade: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val prompt = """
                You are an English curriculum assessment expert for primary school students.
                Generate 10 grade-appropriate English diagnostic questions for a student in $grade.

                Include question types: LISTENING_EMOJI, VOCABULARY, PHONICS, SORT_WORDS, SPEAK_ALOUD.
                Return raw valid JSON array ONLY:
                [
                  {
                    "id": 1,
                    "type": "LISTENING_EMOJI",
                    "title": "1. Listen and Choose",
                    "description": "Select the word you hear:",
                    "voicePrompt": "apple",
                    "options": ["🍎 Apple", "🍌 Banana", "🐱 Cat"],
                    "correctAnswer": "🍎 Apple"
                  }
                ]
            """.trimIndent()

            val result = llmRepository.complete(prompt, taskType = "DIAGNOSIS")
            val generatedList = parseQuestionsJson(result.getOrNull())

            if (generatedList.isNotEmpty()) {
                _questions.value = generatedList
            } else {
                _questions.value = getFallbackQuestionsForGrade(grade)
            }
            _isLoading.value = false
        }
    }

    suspend fun evaluateSpeaking(audioFile: File, referenceText: String): PronunciationResult {
        val result = asrRepository.evaluatePronunciation(audioFile, referenceText)
        return result.getOrElse {
            asrRepository.getOfflineFallbackResult(referenceText)
        }
    }

    private fun parseQuestionsJson(jsonStr: String?): List<DiagnosticQuestion> {
        if (jsonStr.isNullOfBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DiagnosticQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("type", "VOCABULARY")
                val qType = try { QuestionType.valueOf(typeStr) } catch (e: Exception) { QuestionType.VOCABULARY }

                val optionsArray = obj.optJSONArray("options")
                val optionsList = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        optionsList.add(optionsArray.getString(j))
                    }
                }

                val wordsArray = obj.optJSONArray("wordsForSort")
                val wordsList = mutableListOf<String>()
                if (wordsArray != null) {
                    for (j in 0 until wordsArray.length()) {
                        wordsList.add(wordsArray.getString(j))
                    }
                }

                list.add(
                    DiagnosticQuestion(
                        id = obj.optInt("id", i + 1),
                        type = qType,
                        title = obj.optString("title", "${i + 1}. Question"),
                        description = obj.optString("description", "Choose correct answer:"),
                        voicePrompt = obj.optString("voicePrompt", null),
                        options = optionsList,
                        correctAnswer = obj.optString("correctAnswer", ""),
                        wordsForSort = wordsList
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()

    private fun getFallbackQuestionsForGrade(grade: String): List<DiagnosticQuestion> {
        val isLowerGrade = grade.contains("1") || grade.contains("2") || grade.contains("一") || grade.contains("二")
        return if (isLowerGrade) {
            listOf(
                DiagnosticQuestion(1, QuestionType.LISTENING_EMOJI, "1. Listen & Pick", "Select the word you hear:", voicePrompt = "dog", options = listOf("🐶 Dog", "🐱 Cat", "🐰 Rabbit"), correctAnswer = "🐶 Dog"),
                DiagnosticQuestion(2, QuestionType.VOCABULARY, "2. Color Match", "Which color is Red?", options = listOf("🔴 Red", "🔵 Blue", "🟡 Yellow"), correctAnswer = "🔴 Red"),
                DiagnosticQuestion(3, QuestionType.PHONICS, "3. Letter Sound", "Which word starts with /b/?", options = listOf("Ball", "Cat", "Dog"), correctAnswer = "Ball"),
                DiagnosticQuestion(4, QuestionType.SORT_WORDS, "4. Sentence Building", "Arrange words into a sentence:", wordsForSort = listOf("big", "is", "It"), correctAnswer = "It is big"),
                DiagnosticQuestion(5, QuestionType.VOCABULARY, "5. Animal Name", "Select 'Cat':", options = listOf("Cat", "Duck", "Fish"), correctAnswer = "Cat"),
                DiagnosticQuestion(6, QuestionType.LISTENING_EMOJI, "6. Listen & Pick", "Select the fruit:", voicePrompt = "banana", options = listOf("🍎 Apple", "🍌 Banana", "🍐 Pear"), correctAnswer = "🍌 Banana"),
                DiagnosticQuestion(7, QuestionType.VOCABULARY, "7. Number Word", "How many? 3", options = listOf("Three", "One", "Two"), correctAnswer = "Three"),
                DiagnosticQuestion(8, QuestionType.SPEAK_ALOUD, "8. Read Aloud", "Read aloud:", voicePrompt = "Good morning!")
            )
        } else {
            listOf(
                DiagnosticQuestion(1, QuestionType.LISTENING_EMOJI, "1. Listen and Choose", "Select the word you hear:", voicePrompt = "apple", options = listOf("🍎 Apple", "🍌 Banana", "🐱 Cat"), correctAnswer = "🍎 Apple"),
                DiagnosticQuestion(2, QuestionType.VOCABULARY, "2. Opposite Word Select", "Choose opposite of 'Hot':", options = listOf("Cold", "Warm", "Big", "Dry"), correctAnswer = "Cold"),
                DiagnosticQuestion(3, QuestionType.PHONICS, "3. Phonics & Sound", "Which word starts with /p/?", options = listOf("Pig", "Big", "Dig", "Wig"), correctAnswer = "Pig"),
                DiagnosticQuestion(4, QuestionType.SORT_WORDS, "4. Sentence Ordering (Basic)", "Arrange into sentence:", wordsForSort = listOf("like", "apples", "I"), correctAnswer = "I like apples"),
                DiagnosticQuestion(5, QuestionType.VOCABULARY, "5. Grammar & Tense", "Select correct word: 'She ___ to school every day.'", options = listOf("walks", "walked", "walking", "walk"), correctAnswer = "walks"),
                DiagnosticQuestion(6, QuestionType.LISTENING_EMOJI, "6. Listening Comprehension", "Select the animal:", voicePrompt = "cat", options = listOf("🐶 Dog", "🐱 Cat", "🐰 Rabbit"), correctAnswer = "🐱 Cat"),
                DiagnosticQuestion(7, QuestionType.VOCABULARY, "7. Contextual Antonym", "The rabbit is fast, but the turtle is ___:", options = listOf("slow", "quick", "tall", "heavy"), correctAnswer = "slow"),
                DiagnosticQuestion(8, QuestionType.VOCABULARY, "8. Idiom & Everyday English", "What does 'A piece of cake' mean?", options = listOf("Very easy", "Delicious dessert", "Hard problem"), correctAnswer = "Very easy"),
                DiagnosticQuestion(9, QuestionType.SORT_WORDS, "9. Sentence Ordering (Intermediate)", "Arrange into sentence:", wordsForSort = listOf("play", "on", "We", "football", "Sunday"), correctAnswer = "We play football on Sunday"),
                DiagnosticQuestion(10, QuestionType.SPEAK_ALOUD, "10. Speak Aloud Challenge", "Read aloud:", voicePrompt = "Practice makes perfect every day.")
            )
        }
    }
}
