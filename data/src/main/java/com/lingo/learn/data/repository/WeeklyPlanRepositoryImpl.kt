package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.content.OfflineContentStore
import org.akj.lingo.learn.data.local.dao.PlanDao
import org.akj.lingo.learn.data.local.entity.PlanEntity
import org.akj.lingo.learn.domain.model.*
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.DayTaskSummary
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.AgentPromptRegistry
import org.akj.lingo.learn.domain.usecase.SessionBuilder
import org.akj.lingo.learn.domain.usecase.StructuredLlmUseCase
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyPlanRepositoryImpl @Inject constructor(
    private val llmRepository: LlmRepository,
    private val structuredLlmUseCase: StructuredLlmUseCase,
    private val promptRegistry: AgentPromptRegistry,
    private val planDao: PlanDao,
    private val offlineContentStore: OfflineContentStore,
    private val configRepository: ConfigRepository
) : WeeklyPlanRepository {

    private val sessionBuilder = SessionBuilder()

    override suspend fun generateAndCacheWeeklyPlan(
        grade: String,
        accuracy: Int,
        weakCategories: List<String>,
        completedMilestones: List<String>,
        difficultyAdjustment: Float
    ): Result<Plan> {
        val gradeBand = GradeBand.fromGrade(grade)
        // Sprint 10.5: combine grade band coefficient with diagnostic-level adjustment.
        // A = -0.2, B = 0, C = +0.2 — so the plan's difficulty matches the selected
        // grade AND the child's measured level.
        val baseCoefficient = gradeBand.difficultyCoefficient
        val coefficient = (baseCoefficient + difficultyAdjustment).coerceIn(0.6f, 2.0f)
        val prompt = promptRegistry.render(
            "PLAN",
            mapOf(
                "grade" to grade,
                "accuracy" to accuracy.toString(),
                "weak_categories" to weakCategories.joinToString(),
                "milestones" to completedMilestones.joinToString(),
                "coefficient" to coefficient.toString(),
                "duration" to gradeBand.defaultDurationMinutes.toString()
            )
        )

        // A 7-day plan with per-day rationale is a long JSON payload — the default
        // 500-token budget truncates it (observed: glm-5.2 spent it all on reasoning
        // with empty content; the fallback was cut mid-day-6). Match the DIAGNOSIS
        // budget so the full valid JSON survives.
        val llmResult = structuredLlmUseCase.completeJson(prompt, taskType = "PLAN", maxTokens = 1500)
        val now = System.currentTimeMillis()

        // The AI plan is the child's actual curriculum — propagate generation
        // failures so the UI can explain and offer retry, never a canned plan.
        val jsonStr = llmResult.getOrElse { return Result.failure(it) }

        // PLAN is strict content: a response that isn't valid plan JSON must not
        // be cached as a broken plan. Validate the shape before persisting so the
        // caller sees a failure it can surface, not an empty "No plan yet" plan.
        val parsedJson = try {
            JSONObject(jsonStr).also { it.optJSONArray("days") ?: throw Exception("Missing days array") }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val themeName = parsedJson.optString("theme", "Daily Life & School")

        val rationaleSnapshot = try {
            val daysArray = parsedJson.optJSONArray("days")
            if (daysArray != null) {
                val rationaleJson = JSONObject()
                val outDays = org.json.JSONArray()
                for (i in 0 until daysArray.length()) {
                    val dayObj = daysArray.getJSONObject(i)
                    val out = JSONObject()
                    out.put("day", dayObj.optInt("day", i + 1))
                    out.put("rationale", dayObj.optString("rationale", ""))
                    outDays.put(out)
                }
                rationaleJson.put("days", outDays)
                rationaleJson.toString()
            } else null
        } catch (e: Exception) {
            null
        }

        val domainPlan = Plan(
            id = UUID.randomUUID().toString(),
            type = "WEEKLY",
            startDate = now,
            endDate = now + 7 * 24 * 3600 * 1000L,
            theme = themeName,
            difficultyCoefficient = coefficient,
            reviewRatio = 0.2f,
            speechTopics = "School Life, Family, Hobbies",
            weeklyTarget = "Master 20 key words + 7 daily dialogue patterns",
            snapshotData = jsonStr,
            dialogueOutput = null,
            rationaleSnapshot = rationaleSnapshot,
            status = "ACTIVE",
            lastModified = now
        )

        planDao.insertPlan(PlanEntity.fromDomain(domainPlan))
        return Result.success(domainPlan)
    }

    override suspend fun getLatestCachedPlan(): Plan? {
        return planDao.getLatestPlan("WEEKLY")?.toDomain()
    }

    override suspend fun getDayTaskSummary(dayIndex: Int): DayTaskSummary? {
        val plan = planDao.getLatestPlan("WEEKLY") ?: return null
        val dayNumber = dayIndex.coerceIn(1, 7)
        return try {
            val json = JSONObject(plan.snapshotData)
            val days = json.optJSONArray("days") ?: return null
            for (i in 0 until days.length()) {
                val day = days.getJSONObject(i)
                if (day.optInt("day", i + 1) == dayNumber) {
                    val words = mutableListOf<String>()
                    val wordsArr = day.optJSONArray("target_words")
                    if (wordsArr != null) {
                        for (j in 0 until wordsArr.length()) words.add(wordsArr.getString(j))
                    }
                    return DayTaskSummary(
                        day = dayNumber,
                        theme = plan.theme,
                        durationMinutes = day.optInt("duration_minutes", 15),
                        targetWords = words
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getCachedLearningSession(
        dayIndex: Int,
        grade: String,
        reviewQuestions: List<org.akj.lingo.learn.domain.model.QuizQuestion>,
        sentenceLengthAdjustment: Int
    ): LearningSession {
        val gradeBand = GradeBand.fromGrade(grade)
        val dayNumber = dayIndex.coerceIn(1, 7)
        val challengeMode = configRepository.isChallengeModeEnabled()

        // Try to build from cached plan first
        val cachedPlan = planDao.getLatestPlan("WEEKLY")
        if (cachedPlan != null) {
            val expanded = sessionBuilder.expandPlanToSession(
                planSnapshotJson = cachedPlan.snapshotData,
                gradeBand = gradeBand,
                dayIndex = dayNumber,
                reviewQuestions = reviewQuestions,
                sentenceLengthAdjustment = sentenceLengthAdjustment,
                challengeMode = challengeMode
            )
            if (expanded != null) return expanded
        }

        // Fall back to grade-appropriate default content via SessionBuilder
        val defaultJson = generateDefaultPlanJson(gradeBand)
        val expanded = sessionBuilder.expandPlanToSession(
            planSnapshotJson = defaultJson,
            gradeBand = gradeBand,
            dayIndex = dayNumber,
            reviewQuestions = reviewQuestions,
            sentenceLengthAdjustment = sentenceLengthAdjustment,
            challengeMode = challengeMode
        )
        if (expanded != null) return expanded

        // Offline fallback: pre-built sessions from res/raw/ JSON files
        val offlineSession = offlineContentStore.getSessionForDay(dayNumber)
        if (offlineSession != null) {
            return LearningSession(
                theme = "Day $dayNumber: ${offlineSession.theme}",
                subtitleLines = offlineSession.subtitleLines,
                readAlongSentences = offlineSession.readAlongSentences,
                gameQuestions = offlineSession.gameQuestions,
                quizQuestions = offlineSession.quizQuestions,
                targetNewWords = offlineSession.targetNewWords
            )
        }

        // Last resort: hardcoded sample content
        val sample = SampleLearningContent.createSchoolLifeSession()
        return LearningSession(
            theme = "Day $dayNumber: ${sample.theme}",
            subtitleLines = sample.subtitleLines,
            readAlongSentences = sample.readAlongSentences,
            gameQuestions = sample.gameQuestions,
            quizQuestions = sample.quizQuestions,
            targetNewWords = sample.targetNewWords
        )
    }

    /**
     * Generates grade-appropriate default plan JSON so that SessionBuilder can
     * produce adaptive content even without a cached LLM-generated plan.
     * This makes Sprint 3 features (grade-adaptive content) visible immediately.
     *
     * @param difficultyCoefficient overrides the band default when diagnostic tuning applies.
     */
    private fun generateDefaultPlanJson(gradeBand: GradeBand, difficultyCoefficient: Float = gradeBand.difficultyCoefficient): String {
        val theme = when (gradeBand) {
            GradeBand.PRIMARY -> "Daily Life"
            GradeBand.JUNIOR -> "School & Community"
            GradeBand.SENIOR -> "Academic & Society"
        }
        val duration = gradeBand.defaultDurationMinutes

        return when (gradeBand) {
            GradeBand.PRIMARY -> """
            {
                "theme": "$theme",
                "difficulty_coefficient": $difficultyCoefficient,
                "days": [{
                    "day": 1,
                    "focus": "Vocabulary & Dialogue",
                    "target_words": ["hello", "school", "friend", "book", "teacher"],
                    "reference_sentence": "Hello! This is my school. I have a friend and a book.",
                    "duration_minutes": $duration
                },{
                    "day": 2,
                    "focus": "Classroom Objects",
                    "target_words": ["pencil", "desk", "chair", "bag", "ruler"],
                    "reference_sentence": "I have a pencil on my desk. My bag is on the chair.",
                    "duration_minutes": $duration
                },{
                    "day": 3,
                    "focus": "Daily Routines",
                    "target_words": ["morning", "breakfast", "class", "homework", "bed"],
                    "reference_sentence": "In the morning I eat breakfast before my class.",
                    "duration_minutes": $duration
                }]
            }
            """.trimIndent()

            GradeBand.JUNIOR -> """
            {
                "theme": "$theme",
                "difficulty_coefficient": $difficultyCoefficient,
                "days": [{
                    "day": 1,
                    "focus": "Grammar & Sentence Structure",
                    "target_words": ["student", "homework", "library", "subject", "schedule"],
                    "reference_sentence": "The student finishes homework in the library every afternoon.",
                    "duration_minutes": $duration
                },{
                    "day": 2,
                    "focus": "Phrasal Verbs & Social Life",
                    "target_words": ["volunteer", "community", "project", "research", "presentation"],
                    "reference_sentence": "Our class volunteer project requires research and a final presentation.",
                    "duration_minutes": $duration
                },{
                    "day": 3,
                    "focus": "Comparatives & Opinions",
                    "target_words": ["better", "difficult", "opinion", "agree", "argue"],
                    "reference_sentence": "I think reading is more interesting than watching TV.",
                    "duration_minutes": $duration
                }]
            }
            """.trimIndent()

            GradeBand.SENIOR -> """
            {
                "theme": "$theme",
                "difficulty_coefficient": $difficultyCoefficient,
                "days": [{
                    "day": 1,
                    "focus": "Academic Vocabulary & Critical Thinking",
                    "target_words": ["analyze", "conclusion", "evidence", "hypothesis", "methodology"],
                    "reference_sentence": "The researcher analyzed the evidence and drew a meaningful conclusion.",
                    "duration_minutes": $duration
                },{
                    "day": 2,
                    "focus": "Debate & Persuasive Writing",
                    "target_words": ["argument", "persuade", "counterpoint", "rhetoric", "stance"],
                    "reference_sentence": "The speaker used strong rhetoric to persuade the audience of their stance.",
                    "duration_minutes": $duration
                },{
                    "day": 3,
                    "focus": "Academic Writing & Citations",
                    "target_words": ["citation", "reference", "plagiarism", "summarize", "thesis"],
                    "reference_sentence": "Every academic essay must cite its references to avoid plagiarism.",
                    "duration_minutes": $duration
                }]
            }
            """.trimIndent()
        }
    }
}
