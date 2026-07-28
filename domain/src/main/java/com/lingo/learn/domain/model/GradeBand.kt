package org.akj.lingo.learn.domain.model

/**
 * Grade bands for the three-level curriculum system defined in the product design doc section 0.1.
 *
 * Each band defines default difficulty parameters, session duration, and question-type
 * distribution that determine how [SessionBuilder] expands a plan JSON into a full
 * [LearningSession].
 */
enum class GradeBand(
    val displayName: String,
    val defaultDurationMinutes: Int,
    val maxWordsPerSentence: Int,
    val difficultyCoefficient: Float,
    val defaultPhonicsRatio: Float,
    val defaultGrammarRatio: Float,
    val defaultVocabularyRange: String
) {
    PRIMARY(
        displayName = "Primary (Grades 4-6)",
        defaultDurationMinutes = 15,
        maxWordsPerSentence = 8,
        difficultyCoefficient = 1.0f,
        defaultPhonicsRatio = 0.3f,
        defaultGrammarRatio = 0.1f,
        defaultVocabularyRange = "CEFR A1-A2, daily life & school themes"
    ),
    JUNIOR(
        displayName = "Junior High (Grades 7-9)",
        defaultDurationMinutes = 20,
        maxWordsPerSentence = 14,
        difficultyCoefficient = 1.3f,
        defaultPhonicsRatio = 0.1f,
        defaultGrammarRatio = 0.3f,
        defaultVocabularyRange = "CEFR A2-B1, social & academic themes, phrasal verbs"
    ),
    SENIOR(
        displayName = "Senior High (Grades 10-12)",
        defaultDurationMinutes = 25,
        maxWordsPerSentence = 20,
        difficultyCoefficient = 1.6f,
        defaultPhonicsRatio = 0.0f,
        defaultGrammarRatio = 0.25f,
        defaultVocabularyRange = "CEFR B1-B2, academic & exam-prep themes, collocations"
    );

    companion object {
        /**
         * Maps a grade string (e.g., "Grade 4", "Senior 2") to the appropriate [GradeBand].
         * Falls back to [PRIMARY] for unrecognized grades.
         */
        fun fromGrade(grade: String): GradeBand = when {
            grade.contains("Grade 4") || grade.contains("Grade 5") || grade.contains("Grade 6")
                || grade.contains("小学") || grade.contains("Primary") -> PRIMARY

            grade.contains("Grade 7") || grade.contains("Grade 8") || grade.contains("Grade 9")
                || grade.contains("初一") || grade.contains("初二") || grade.contains("初三")
                || grade.contains("Junior") -> JUNIOR

            grade.contains("Grade 10") || grade.contains("Grade 11") || grade.contains("Grade 12")
                || grade.contains("高一") || grade.contains("高二") || grade.contains("高三")
                || grade.contains("Senior") -> SENIOR

            else -> PRIMARY
        }

        /**
         * Returns the difficulty-adjusted duration in minutes based on the coefficient.
         */
        fun getAdjustedDuration(baseMinutes: Int, coefficient: Float): Int {
            return (baseMinutes * coefficient).toInt().coerceIn(10, 40)
        }
    }
}
