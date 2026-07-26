package com.lingo.learn.data.repository

import com.lingo.learn.data.local.dao.PlanDao
import com.lingo.learn.data.local.entity.PlanEntity
import com.lingo.learn.domain.model.GameQuestion
import com.lingo.learn.domain.model.GameType
import com.lingo.learn.domain.model.LearningSession
import com.lingo.learn.domain.model.Plan
import com.lingo.learn.domain.model.QuizQuestion
import com.lingo.learn.domain.model.QuizQuestionType
import com.lingo.learn.domain.model.ReadAlongSentence
import com.lingo.learn.domain.model.SubtitleLine
import com.lingo.learn.domain.repository.LlmRepository
import com.lingo.learn.domain.repository.WeeklyPlanRepository
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyPlanRepositoryImpl @Inject constructor(
    private val llmRepository: LlmRepository,
    private val planDao: PlanDao
) : WeeklyPlanRepository {

    override suspend fun generateAndCacheWeeklyPlan(
        grade: String,
        accuracy: Int,
        weakCategories: List<String>,
        completedMilestones: List<String>
    ): Result<Plan> {
        val prompt = """
            You are the curriculum planner for Lingo English. 
            Generate a personalized 7-day English learning plan for a student in $grade.

            --- Student Learning History ---
            - Average Accuracy: $accuracy%
            - Weak Word Categories: ${weakCategories.joinToString()}
            - Completed Milestones: ${completedMilestones.joinToString()}

            Output raw valid JSON ONLY matching structure:
            {
              "theme": "School Life & Family",
              "difficulty_coefficient": 1.2,
              "days": [
                {
                  "day": 1,
                  "focus": "Vocabulary & Dialogue",
                  "target_words": ["classroom", "teacher", "notebook"],
                  "reference_sentence": "Welcome to our sunny classroom!",
                  "duration_minutes": 15
                }
              ]
            }
        """.trimIndent()

        val llmResult = llmRepository.complete(prompt, taskType = "PLAN")
        val now = System.currentTimeMillis()

        val jsonStr = llmResult.getOrNull() ?: """
            {
              "theme": "Daily Life & School",
              "difficulty_coefficient": 1.0,
              "days": []
            }
        """.trimIndent()

        val themeName = try {
            JSONObject(jsonStr).optString("theme", "Daily Life & School")
        } catch (e: Exception) {
            "Daily Life & School"
        }

        val domainPlan = Plan(
            id = UUID.randomUUID().toString(),
            type = "WEEKLY",
            startDate = now,
            endDate = now + 7 * 24 * 3600 * 1000L,
            theme = themeName,
            difficultyCoefficient = 1.0f,
            reviewRatio = 0.2f,
            speechTopics = "School Life, Family, Hobbies",
            weeklyTarget = "Master 20 key words + 7 daily dialogue patterns",
            snapshotData = jsonStr,
            dialogueOutput = null,
            status = "ACTIVE",
            lastModified = now
        )

        planDao.insertPlan(PlanEntity.fromDomain(domainPlan))
        return Result.success(domainPlan)
    }

    override suspend fun getLatestCachedPlan(): Plan? {
        return planDao.getLatestPlan("WEEKLY")?.toDomain()
    }

    override suspend fun getCachedLearningSession(dayIndex: Int): LearningSession {
        val dayNumber = if (dayIndex in 1..7) dayIndex else 1

        val subtitles = listOf(
            SubtitleLine(1, 0, 3000, "Welcome to our sunny classroom!", listOf("classroom")),
            SubtitleLine(2, 3000, 6500, "Our teacher is very kind and helpful.", listOf("teacher")),
            SubtitleLine(3, 6500, 10000, "Open your notebook and write down your name.", listOf("notebook"))
        )

        val readAlong = listOf(
            ReadAlongSentence(1, "Welcome to our sunny classroom!", "欢迎来到我们阳光明媚的教室！"),
            ReadAlongSentence(2, "Our teacher is very kind and helpful.", "我们的老师非常亲切且乐于助人。"),
            ReadAlongSentence(3, "Open your notebook and write down your name.", "打开你的笔记本并写下你的名字。")
        )

        val games = listOf(
            GameQuestion(1, GameType.LISTEN_CHOOSE_IMAGE, "Listen and choose the picture:", audioText = "teacher", options = listOf("🍎", "👩‍🏫", "🐱"), correctIndex = 1),
            GameQuestion(2, GameType.DRAG_MATCH, "Match 'classroom':", options = listOf("教室", "老师", "书包"), correctIndex = 0)
        )

        val quizQuestions = listOf(
            QuizQuestion(1, QuizQuestionType.LISTEN_CHOOSE_WORD, "Listen to the word and choose:", audioText = "classroom", options = listOf("🍎 Apple", "🏫 Classroom", "🐱 Cat"), correctIndex = 1),
            QuizQuestion(2, QuizQuestionType.IMAGE_CHOOSE_WORD, "Choose the word for '老师':", options = listOf("Teacher", "Student", "Doctor"), correctIndex = 0),
            QuizQuestion(3, QuizQuestionType.SPELL_FILL_BLANK, "Complete the word: cla__room", options = listOf("ss", "tt", "pp"), correctIndex = 0),
            QuizQuestion(4, QuizQuestionType.SENTENCE_ORDER, "Put the words in correct order:", options = listOf("like", "apples", "I"), correctIndex = 0, correctOrder = listOf("I", "like", "apples")),
            QuizQuestion(5, QuizQuestionType.READ_ALOUD, "Read this sentence aloud:", audioText = "Welcome to our sunny classroom!", options = emptyList(), correctIndex = 0)
        )

        return LearningSession(
            theme = "Day $dayNumber: School Life",
            subtitleLines = subtitles,
            readAlongSentences = readAlong,
            gameQuestions = games,
            quizQuestions = quizQuestions,
            targetNewWords = listOf("classroom", "teacher", "notebook")
        )
    }
}
