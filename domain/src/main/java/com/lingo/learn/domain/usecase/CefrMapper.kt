package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.GradeBand

/** CEFR proficiency levels used for the international level indicator. */
enum class CefrLevel(val label: String) {
    PRE_A1("Pre-A1"),
    A1("A1"),
    A2("A2"),
    B1("B1"),
    B2("B2")
}

/**
 * Maps (grade, diagnosticLevel) to an estimated CEFR level.
 *
 * The grade band defines the base level (PRIMARY → A1, JUNIOR → A2,
 * SENIOR → B1) and the onboarding diagnostic level adjusts it:
 * "A" (advanced) bumps one level up, "C" (foundation) one level down,
 * "B" stays at the base. Unknown levels are treated as "B".
 */
object CefrMapper {

    fun map(grade: String, diagnosticLevel: String): CefrLevel {
        val band = GradeBand.fromGrade(grade)
        val base = when (band) {
            GradeBand.PRIMARY -> CefrLevel.A1
            GradeBand.JUNIOR -> CefrLevel.A2
            GradeBand.SENIOR -> CefrLevel.B1
        }
        return when (diagnosticLevel.trim().uppercase()) {
            "A" -> base.shiftUp()
            "C" -> base.shiftDown()
            else -> base
        }
    }

    fun badge(level: CefrLevel): String = "${level.label} 🌍"

    private fun CefrLevel.shiftUp(): CefrLevel = when (this) {
        CefrLevel.PRE_A1 -> CefrLevel.A1
        CefrLevel.A1 -> CefrLevel.A2
        CefrLevel.A2 -> CefrLevel.B1
        CefrLevel.B1 -> CefrLevel.B2
        CefrLevel.B2 -> CefrLevel.B2
    }

    private fun CefrLevel.shiftDown(): CefrLevel = when (this) {
        CefrLevel.PRE_A1 -> CefrLevel.PRE_A1
        CefrLevel.A1 -> CefrLevel.PRE_A1
        CefrLevel.A2 -> CefrLevel.A1
        CefrLevel.B1 -> CefrLevel.A2
        CefrLevel.B2 -> CefrLevel.B1
    }
}
