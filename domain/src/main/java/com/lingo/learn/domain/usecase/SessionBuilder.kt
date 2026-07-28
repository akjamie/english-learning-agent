package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.*
import org.json.JSONObject

/**
 * Expands an LLM-generated weekly plan JSON into a fully structured [LearningSession].
 *
 * The plan JSON (stored in [Plan.snapshotData]) contains high-level day descriptors:
 * theme, target_words, reference_sentence, duration_minutes. This builder transforms
 * those into grade-appropriate subtitle lines, read-along sentences, quiz questions,
 * and mini-games — enabling on-demand content generation without repeated LLM calls.
 *
 * All methods are pure Kotlin with zero Android dependencies, making this class
 * fully unit-testable.
 */
class SessionBuilder {

    /**
     * Expands the plan JSON for a specific day into a full [LearningSession].
     *
     * @param planSnapshotJson The raw JSON string from [Plan.snapshotData].
     * @param gradeBand Determines content complexity, question types, and session duration.
     * @param dayIndex 1-based day index (1-7) within the week.
     * @param reviewQuestions Error-book recurrence questions to merge into the quiz.
     * @return A fully populated [LearningSession], or null if parsing fails.
     */
    fun expandPlanToSession(
        planSnapshotJson: String,
        gradeBand: GradeBand,
        dayIndex: Int,
        reviewQuestions: List<QuizQuestion> = emptyList()
    ): LearningSession? {
        return try {
            val root = JSONObject(planSnapshotJson)
            val theme = root.optString("theme", "Daily Life & School")
            val days = root.optJSONArray("days") ?: return null
            val dayIdx = dayIndex.coerceIn(1, days.length())

            val dayObj = days.getJSONObject(dayIdx - 1)
            val targetWords = parseStringArray(dayObj, "target_words")
            val referenceSentence = dayObj.optString("reference_sentence", "")
            val focus = dayObj.optString("focus", "Vocabulary & Dialogue")

            val subtitles = generateSubtitles(referenceSentence, targetWords, gradeBand)
            val readAlong = generateReadAlong(referenceSentence, targetWords, gradeBand)
            val games = generateGames(targetWords, gradeBand)
            val quiz = generateQuizQuestions(targetWords, theme, gradeBand, dayIdx, reviewQuestions)

            LearningSession(
                theme = "Day $dayIdx: $theme",
                subtitleLines = subtitles,
                readAlongSentences = readAlong,
                gameQuestions = games,
                quizQuestions = quiz,
                targetNewWords = targetWords
            )
        } catch (e: Exception) {
            null
        }
    }

    //region Subtitle Generation

    private fun generateSubtitles(
        referenceSentence: String,
        targetWords: List<String>,
        gradeBand: GradeBand
    ): List<SubtitleLine> {
        if (referenceSentence.isBlank()) {
            return targetWords.mapIndexed { index, word ->
                SubtitleLine(
                    id = index + 1,
                    startTimeMs = index * 3000L,
                    endTimeMs = (index + 1) * 3000L,
                    text = word,
                    newWords = listOf(word)
                )
            }
        }

        // Split the reference sentence into multiple lines based on grade band
        val words = referenceSentence.split(" ").filter { it.isNotBlank() }
        val maxWords = gradeBand.maxWordsPerSentence.coerceAtLeast(4)

        val lines = mutableListOf<SubtitleLine>()
        var lineId = 1
        var currentPos = 0L

        words.chunked(maxWords).forEachIndexed { chunkIndex, chunk ->
            val lineText = chunk.joinToString(" ")
            val newWords = chunk.filter { it.lowercase() in targetWords.map { w -> w.lowercase() } }
            val duration = (chunk.size * 600L).coerceAtLeast(2000L)

            lines.add(
                SubtitleLine(
                    id = lineId++,
                    startTimeMs = currentPos,
                    endTimeMs = currentPos + duration,
                    text = lineText,
                    newWords = newWords
                )
            )
            currentPos += duration
        }

        return lines
    }

    //endregion

    //region Read-Along Generation

    private fun generateReadAlong(
        referenceSentence: String,
        targetWords: List<String>,
        gradeBand: GradeBand
    ): List<ReadAlongSentence> {
        val sentences = mutableListOf<ReadAlongSentence>()

        // Primary sentence from reference
        if (referenceSentence.isNotBlank()) {
            sentences.add(
                ReadAlongSentence(
                    id = 1,
                    text = referenceSentence,
                    chineseHint = ""
                )
            )
        }

        // Additional sentences for each target word
        targetWords.take(gradeBand.maxWordsPerSentence).forEachIndexed { index, word ->
            val simpleSentence = when (gradeBand) {
                GradeBand.PRIMARY -> "I see a $word."
                GradeBand.JUNIOR -> "Can you find the $word?"
                GradeBand.SENIOR -> "The $word is very important for our lesson today."
            }
            sentences.add(
                ReadAlongSentence(
                    id = sentences.size + 1,
                    text = simpleSentence,
                    chineseHint = ""
                )
            )
        }

        return sentences.distinctBy { it.text.lowercase() }.take(3)
    }

    //endregion

    //region Mini-Game Generation

    private fun generateGames(targetWords: List<String>, gradeBand: GradeBand): List<GameQuestion> {
        val games = mutableListOf<GameQuestion>()

        if (targetWords.isNotEmpty()) {
            // DRAG_MATCH game
            val word = targetWords.first()
            val distractors = SAMPLE_DISTRACTORS.shuffled().take(3)
            val options = (listOf(word) + distractors).shuffled()
            games.add(
                GameQuestion(
                    id = 1,
                    type = GameType.DRAG_MATCH,
                    prompt = "Find the word: '$word'",
                    options = options,
                    correctIndex = options.indexOf(word)
                )
            )
        }

        if (targetWords.size > 1) {
            // LISTEN_CHOOSE_IMAGE game
            val word = targetWords[1]
            val distractors = SAMPLE_EMOJI_DISTRACTORS.shuffled().take(3)
            val options = (listOf(word) + distractors).shuffled()
            games.add(
                GameQuestion(
                    id = 2,
                    type = GameType.LISTEN_CHOOSE_IMAGE,
                    prompt = "Listen and choose:",
                    audioText = word,
                    options = options,
                    correctIndex = options.indexOf(word)
                )
            )
        }

        return games
    }

    //endregion

    //region Quiz Generation

    private fun generateQuizQuestions(
        targetWords: List<String>,
        theme: String,
        gradeBand: GradeBand,
        dayIndex: Int,
        reviewQuestions: List<QuizQuestion>
    ): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()

        // Add error-book recurrence questions first (1-2)
        val reviewQ = reviewQuestions.take(2)
        questions.addAll(reviewQ)

        // Generate fresh questions from target words
        val remainingSlots = (5 - questions.size).coerceIn(1, 3)

        // Always include at least 1 listening question
        if (targetWords.isNotEmpty()) {
            val word = targetWords[0]
            val distractors = SAMPLE_DISTRACTORS.shuffled().take(3)
            val options = (listOf(word) + distractors).shuffled()
            questions.add(
                QuizQuestion(
                    id = 100 + questions.size,
                    type = QuizQuestionType.LISTEN_CHOOSE_WORD,
                    question = "Listen and choose the correct word:",
                    audioText = word,
                    options = options,
                    correctIndex = options.indexOf(word)
                )
            )
        }

        // Grade-appropriate question types
        if (targetWords.size >= 2 && remainingSlots >= 2) {
            when (gradeBand) {
                GradeBand.PRIMARY -> {
                    // Image-choose for primary
                    val word = targetWords[1]
                    val distractors = SAMPLE_DISTRACTORS.shuffled().take(3)
                    val options = (listOf(word) + distractors).shuffled()
                    questions.add(
                        QuizQuestion(
                            id = 200 + questions.size,
                            type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                            question = "Which word matches?",
                            options = options,
                            correctIndex = options.indexOf(word)
                        )
                    )
                }
                GradeBand.JUNIOR -> {
                    // Spelling for junior
                    val word = targetWords[1]
                    val letter = word.toCharArray().filter { it.isLetter() }.firstOrNull()?.lowercase() ?: "s"
                    val distractors = listOf("a", "e", "o", "u").filter { it != letter }.take(3)
                    questions.add(
                        QuizQuestion(
                            id = 200 + questions.size,
                            type = QuizQuestionType.SPELL_FILL_BLANK,
                            question = "Complete the word: ${word.first()}_${word.drop(1)}",
                            options = (listOf(letter) + distractors).shuffled(),
                            correctIndex = 0
                        )
                    )
                }
                GradeBand.SENIOR -> {
                    // Sentence order for senior
                    val word = targetWords[1]
                    questions.add(
                        QuizQuestion(
                            id = 200 + questions.size,
                            type = QuizQuestionType.SENTENCE_ORDER,
                            question = "Order: '$word' in a sentence",
                            options = listOf("I", "a", "see", word),
                            correctOrder = listOf("I", "see", "a", word)
                        )
                    )
                }
            }
        }

        if (targetWords.size >= 3 && questions.size < 5) {
            val word = targetWords[2]
            val distractors = SAMPLE_DISTRACTORS.shuffled().take(3)
            val options = (listOf(word) + distractors).shuffled()
            questions.add(
                QuizQuestion(
                    id = 300 + questions.size,
                    type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                    question = "Choose the correct word:",
                    options = options,
                    correctIndex = options.indexOf(word)
                )
            )
        }

        return questions.take(5).shuffled()
    }

    //endregion

    //region Helpers

    private fun parseStringArray(json: JSONObject, key: String): List<String> {
        val arr = json.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }

    //endregion

    companion object {
        private val SAMPLE_DISTRACTORS = listOf("apple", "pencil", "water", "garden", "kitchen", "window", "picture")
        private val SAMPLE_EMOJI_DISTRACTORS = listOf("🌳 Garden", "🏠 House", "🐕 Dog", "🍎 Apple", "✏️ Pencil")
    }
}
