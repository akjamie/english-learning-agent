package org.akj.lingo.learn.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineContentStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sessionCache = mutableMapOf<String, LearningSession>()

    fun getSessionForDay(dayOfWeek: Int): LearningSession? {
        val themeIndex = ((dayOfWeek - 1) % THEMES.size).coerceIn(0, THEMES.size - 1)
        val theme = THEMES[themeIndex]
        sessionCache[theme]?.let { return it }
        return loadSession(theme)?.also { sessionCache[theme] = it }
    }

    fun getSessionForToday(): LearningSession? {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val adjustedDay = when (dayOfWeek) {
            Calendar.SUNDAY -> 7
            else -> dayOfWeek - 1
        }
        return getSessionForDay(adjustedDay)
    }

    private fun loadSession(theme: String): LearningSession? {
        return try {
            val resId = context.resources.getIdentifier(theme, "raw", context.packageName)
            if (resId == 0) return null
            val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            parseSession(root)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSession(root: JSONObject): LearningSession {
        val theme = root.optString("theme", "Daily Life")
        val targetNewWords = parseStringArray(root, "targetNewWords")
        val subtitles = parseSubtitles(root.optJSONArray("subtitleLines"))
        val readAlong = parseReadAlong(root.optJSONArray("readAlongSentences"))
        val games = parseGames(root.optJSONArray("gameQuestions"))
        val quiz = parseQuiz(root.optJSONArray("quizQuestions"))
        return LearningSession(
            theme = theme,
            subtitleLines = subtitles,
            readAlongSentences = readAlong,
            gameQuestions = games,
            quizQuestions = quiz,
            targetNewWords = targetNewWords
        )
    }

    private fun parseSubtitles(arr: JSONArray?): List<SubtitleLine> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            SubtitleLine(
                id = obj.optInt("id", i + 1),
                startTimeMs = obj.optLong("startTimeMs", i * 3000L),
                endTimeMs = obj.optLong("endTimeMs", (i + 1) * 3000L),
                text = obj.optString("text", ""),
                newWords = parseStringArray(obj, "newWords")
            )
        }
    }

    private fun parseReadAlong(arr: JSONArray?): List<ReadAlongSentence> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ReadAlongSentence(
                id = obj.optInt("id", i + 1),
                text = obj.optString("text", ""),
                chineseHint = obj.optString("chineseHint", "")
            )
        }
    }

    private fun parseGames(arr: JSONArray?): List<GameQuestion> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            GameQuestion(
                id = obj.optInt("id", i + 1),
                type = parseGameType(obj.optString("type", "DRAG_MATCH")),
                prompt = obj.optString("prompt", ""),
                audioText = obj.optString("audioText").ifBlank { null },
                options = parseStringArray(obj, "options"),
                correctIndex = obj.optInt("correctIndex", 0)
            )
        }
    }

    private fun parseQuiz(arr: JSONArray?): List<QuizQuestion> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            QuizQuestion(
                id = obj.optInt("id", i + 1),
                type = parseQuizType(obj.optString("type", "IMAGE_CHOOSE_WORD")),
                question = obj.optString("question", ""),
                audioText = obj.optString("audioText").ifBlank { null },
                options = parseStringArray(obj, "options"),
                correctIndex = obj.optInt("correctIndex", 0),
                correctOrder = parseStringArray(obj, "correctOrder"),
                isFromErrorBook = obj.optBoolean("isFromErrorBook", false)
            )
        }
    }

    private fun parseGameType(value: String): GameType = when (value.uppercase()) {
        "DRAG_MATCH" -> GameType.DRAG_MATCH
        "LISTEN_CHOOSE_IMAGE" -> GameType.LISTEN_CHOOSE_IMAGE
        else -> GameType.DRAG_MATCH
    }

    private fun parseQuizType(value: String): QuizQuestionType = when (value.uppercase()) {
        "IMAGE_CHOOSE_WORD" -> QuizQuestionType.IMAGE_CHOOSE_WORD
        "LISTEN_CHOOSE_WORD" -> QuizQuestionType.LISTEN_CHOOSE_WORD
        "SPELL_FILL_BLANK" -> QuizQuestionType.SPELL_FILL_BLANK
        "SENTENCE_ORDER" -> QuizQuestionType.SENTENCE_ORDER
        "SPELLING" -> QuizQuestionType.SPELLING
        "DICTATION" -> QuizQuestionType.DICTATION
        "READ_ALOUD" -> QuizQuestionType.READ_ALOUD
        "SENTENCE_WRITING" -> QuizQuestionType.SENTENCE_WRITING
        "CVC_BUILD" -> QuizQuestionType.CVC_BUILD
        "ONSET_RIME" -> QuizQuestionType.ONSET_RIME
        "MINIMAL_PAIRS" -> QuizQuestionType.MINIMAL_PAIRS
        else -> QuizQuestionType.IMAGE_CHOOSE_WORD
    }

    private fun parseStringArray(obj: JSONObject, key: String): List<String> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }

    companion object {
        private val THEMES = listOf("school", "animals", "food", "colors", "family")
    }
}
