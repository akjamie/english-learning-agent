package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import javax.inject.Inject

/**
 * Single source of truth for the child's learning context fed to AI agents
 * (Sprint 21 Phase B).
 *
 * Previously each caller (PlanGeneratingViewModel, WeeklyPlanViewModel,
 * WeeklyReportViewModel) derived the same student metrics independently, which
 * produced drift — most notably an accuracy bug where one path passed 0-1 as if
 * it were 0-100 (the LLM saw "1%" instead of "75%"). This service computes the
 * whole context once, in one place, exposing both the 0-1 fraction and the 0-100
 * percent so callers can't mix the two scales.
 *
 * Pure Kotlin; repositories are Room/SharedPrefs-backed and cheap, so each call
 * reads fresh data (no stale caching of a child's live accuracy).
 */
class StudentContextService @Inject constructor(
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository
) {

    /**
     * Loads the full student context for [grade] + [diagnosticLevel].
     * Repository failures degrade to sensible defaults so plan/report generation
     * never crashes on a transient DB error.
     */
    suspend fun load(grade: String, diagnosticLevel: String): StudentContext {
        val accuracyFraction = try {
            learningRecordRepository.getMonthlyAccuracy().coerceIn(0f, 1f)
        } catch (_: Exception) {
            0.75f
        }
        val weakCategories = try {
            learningRecordRepository.getWeakCategories()
        } catch (_: Exception) {
            emptyList()
        }
        val streakDays = try {
            learningRecordRepository.getStreakDays()
        } catch (_: Exception) {
            0
        }
        val errorCount = try {
            errorBookRepository.getErrorCount()
        } catch (_: Exception) {
            0
        }

        val milestones = buildList {
            if (streakDays > 0) add("${streakDays}-day streak")
            if (errorCount > 0) add("$errorCount words in error book")
            if (isEmpty()) add("First week starting")
        }

        return StudentContext(
            accuracyFraction = accuracyFraction,
            accuracyPercent = (accuracyFraction * 100).toInt(),
            weakCategories = weakCategories,
            streakDays = streakDays,
            errorCount = errorCount,
            completedMilestones = milestones,
            difficultyAdjustment = difficultyAdjustmentFor(diagnosticLevel),
            cefrLabel = CefrMapper.badge(CefrMapper.map(grade, diagnosticLevel))
        )
    }

    private fun difficultyAdjustmentFor(diagnosticLevel: String): Float = when (diagnosticLevel.uppercase()) {
        "A" -> -0.2f
        "C" -> 0.2f
        else -> 0f
    }
}

/** Learning context shared by plan generation, the weekly report and CEFR mapping. */
data class StudentContext(
    val accuracyFraction: Float,
    val accuracyPercent: Int,
    val weakCategories: List<String>,
    val streakDays: Int,
    val errorCount: Int,
    val completedMilestones: List<String>,
    val difficultyAdjustment: Float,
    val cefrLabel: String
)
