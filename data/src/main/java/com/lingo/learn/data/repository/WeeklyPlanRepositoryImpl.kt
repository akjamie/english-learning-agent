package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.PlanDao
import org.akj.lingo.learn.data.local.entity.PlanEntity
import org.akj.lingo.learn.domain.model.*
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.SessionBuilder
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyPlanRepositoryImpl @Inject constructor(
    private val llmRepository: LlmRepository,
    private val planDao: PlanDao
) : WeeklyPlanRepository {

    private val sessionBuilder = SessionBuilder()

    override suspend fun generateAndCacheWeeklyPlan(
        grade: String,
        accuracy: Int,
        weakCategories: List<String>,
        completedMilestones: List<String>
    ): Result<Plan> {
        val gradeBand = GradeBand.fromGrade(grade)
        val prompt = """
            You are the curriculum planner for Lingo English. 
            Your task is to generate a personalized 7-day English learning plan for a student in $grade using default textbook.

            --- Student Learning History ---
            - Average Accuracy: $accuracy%
            - Weak Word Categories: ${weakCategories.joinToString()}
            - Completed Milestones: ${completedMilestones.joinToString()}

            --- Constraints & Output Format ---
            You must output a raw, valid JSON object ONLY. Do not write markdown blocks like ```json or any prefix text. The JSON must match the following structure:
            {
              "theme": "Unit theme name",
              "difficulty_coefficient": ${gradeBand.difficultyCoefficient},
              "days": [
                {
                  "day": 1,
                  "focus": "Vocabulary / Grammar / Dialogue",
                  "target_words": ["word1", "word2"],
                  "reference_sentence": "Standard practice sentence of the day",
                  "duration_minutes": ${gradeBand.defaultDurationMinutes},
                  "rationale": "One sentence explaining WHY this day's content was arranged this way, referencing the student's specific learning history (e.g., 'Your listening accuracy was 72% last week, so today focuses on listening-intensive vocabulary')"
                }
              ]
            }
        """.trimIndent()

        val llmResult = llmRepository.complete(prompt, taskType = "PLAN")
        val now = System.currentTimeMillis()

        val jsonStr = llmResult.getOrNull() ?: """
            {
              "theme": "Daily Life & School",
              "difficulty_coefficient": ${gradeBand.difficultyCoefficient},
              "days": []
            }
        """.trimIndent()

        val themeName = try {
            JSONObject(jsonStr).optString("theme", "Daily Life & School")
        } catch (e: Exception) {
            "Daily Life & School"
        }

        val rationaleSnapshot = try {
            val planJson = JSONObject(jsonStr)
            val daysArray = planJson.optJSONArray("days")
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
            difficultyCoefficient = gradeBand.difficultyCoefficient,
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

    override suspend fun getCachedLearningSession(dayIndex: Int, grade: String): LearningSession {
        val gradeBand = GradeBand.fromGrade(grade)
        val dayNumber = dayIndex.coerceIn(1, 7)

        // Try to build from cached plan first
        val cachedPlan = planDao.getLatestPlan("WEEKLY")
        if (cachedPlan != null) {
            val expanded = sessionBuilder.expandPlanToSession(
                planSnapshotJson = cachedPlan.snapshotData,
                gradeBand = gradeBand,
                dayIndex = dayNumber
            )
            if (expanded != null) return expanded
        }

        // Fall back to grade-appropriate default content via SessionBuilder
        val defaultJson = generateDefaultPlanJson(gradeBand)
        val expanded = sessionBuilder.expandPlanToSession(
            planSnapshotJson = defaultJson,
            gradeBand = gradeBand,
            dayIndex = dayNumber
        )
        if (expanded != null) return expanded

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
     */
    private fun generateDefaultPlanJson(gradeBand: GradeBand): String {
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
                "difficulty_coefficient": ${gradeBand.difficultyCoefficient},
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
                }]
            }
            """.trimIndent()

            GradeBand.JUNIOR -> """
            {
                "theme": "$theme",
                "difficulty_coefficient": ${gradeBand.difficultyCoefficient},
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
                }]
            }
            """.trimIndent()

            GradeBand.SENIOR -> """
            {
                "theme": "$theme",
                "difficulty_coefficient": ${gradeBand.difficultyCoefficient},
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
                }]
            }
            """.trimIndent()
        }
    }
}
