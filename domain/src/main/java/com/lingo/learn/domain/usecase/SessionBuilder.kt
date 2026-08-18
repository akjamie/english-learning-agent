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
class SessionBuilder(
    private val phonicsModule: PhonicsModule = PhonicsModule()
) {

    /**
     * Expands the plan JSON for a specific day into a full [LearningSession].
     *
     * @param planSnapshotJson The raw JSON string from [Plan.snapshotData].
     * @param gradeBand Determines content complexity, question types, and session duration.
     * @param dayIndex 1-based day index (1-7) within the week.
     * @param reviewQuestions Error-book recurrence questions to merge into the quiz
     *                        (spaced-repetition due words, Sprint 7).
     * @param sentenceLengthAdjustment Words added (positive) or removed (negative) from
     *                                 sentence generation by [AdaptiveDifficultyEngine].
     * @return A fully populated [LearningSession], or null if parsing fails.
     */
    fun expandPlanToSession(
        planSnapshotJson: String,
        gradeBand: GradeBand,
        dayIndex: Int,
        reviewQuestions: List<QuizQuestion> = emptyList(),
        sentenceLengthAdjustment: Int = 0,
        challengeMode: Boolean = false
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

            val challengeBonus = if (challengeMode) 4 else 0

            // The plan's difficulty_coefficient is band-relative (set from the
            // diagnostic level, see WeeklyPlanRepositoryImpl): its delta vs the
            // band base maps to ~1 word per 0.1 step, capped at ±3 words.
            val planCoefficient = root
                .optDouble("difficulty_coefficient", gradeBand.difficultyCoefficient.toDouble())
                .toFloat()
            val difficultyWords =
                ((planCoefficient - gradeBand.difficultyCoefficient) * 10).toInt().coerceIn(-3, 3)

            val adjustedMaxWords = (gradeBand.maxWordsPerSentence + sentenceLengthAdjustment + challengeBonus + difficultyWords)
                .coerceAtLeast(4)

            val subtitles = generateSubtitles(referenceSentence, targetWords, gradeBand, adjustedMaxWords)
            val readAlong = generateReadAlong(referenceSentence, targetWords, gradeBand, adjustedMaxWords, challengeMode)
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
        gradeBand: GradeBand,
        maxWordsPerSentence: Int
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
        val maxWords = maxWordsPerSentence.coerceAtLeast(4)

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
        gradeBand: GradeBand,
        maxWordsPerSentence: Int,
        challengeMode: Boolean = false
    ): List<ReadAlongSentence> {
        val sentences = mutableListOf<ReadAlongSentence>()

        if (referenceSentence.isNotBlank()) {
            sentences.add(
                ReadAlongSentence(
                    id = 1,
                    text = referenceSentence,
                    chineseHint = ""
                )
            )
        }

        targetWords.take(maxWordsPerSentence).forEachIndexed { index, word ->
            val simpleSentence = when {
                challengeMode -> "Can you tell me more about the $word and why it matters today?"
                gradeBand == GradeBand.PRIMARY -> "I see a $word."
                gradeBand == GradeBand.JUNIOR -> "Can you find the $word?"
                else -> "The $word is very important for our lesson today."
            }
            sentences.add(
                ReadAlongSentence(
                    id = sentences.size + 1,
                    text = simpleSentence,
                    chineseHint = ""
                )
            )
        }

        return sentences.distinctBy { it.text.lowercase() }.take(if (challengeMode) 5 else 3)
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

        // Add error-book recurrence questions first (1-2, spaced-repetition due words)
        val reviewQ = reviewQuestions.take(2)
        questions.addAll(reviewQ)

        // Phonics words are used for PRIMARY blending tasks (CVC / onset-rime / minimal pairs)
        val phonicsWords = targetWords.filter { phonicsModule.isPhonicsWord(it) }
        var phonicsIndex = 0
        val phonicsTarget = if (gradeBand == GradeBand.PRIMARY && phonicsWords.isNotEmpty()) {
            (phonicsWords.size * gradeBand.defaultPhonicsRatio).toInt().coerceIn(1, 3)
        } else 0
        val phonicsIds = mutableListOf<Int>()

        // Generate fresh questions from target words to reach 10
        var loopCount = 0
        while (questions.size < 10 && targetWords.isNotEmpty() && loopCount < 5) {
            for (word in targetWords) {
                if (questions.size >= 10) break

                val distractors = SAMPLE_DISTRACTORS.shuffled().take(3)
                val options = (listOf(word) + distractors).shuffled()

                // Inject a phonics question when the PRIMARY band's phonics quota is unmet
                if (phonicsIndex < phonicsTarget && phonicsWords.isNotEmpty()) {
                    val phWord = phonicsWords[phonicsIndex % phonicsWords.size]
                    val phQuestion = phonicsModule.buildPhonicsQuestion(phWord, 700 + questions.size)
                    if (phQuestion != null) {
                        questions.add(phQuestion)
                        phonicsIds.add(phQuestion.id)
                    }
                    phonicsIndex++
                }

                if (questions.size >= 10) break

                // Mix question types
                val type = when (questions.size % 4) {
                    0 -> QuizQuestionType.LISTEN_CHOOSE_WORD
                    1 -> QuizQuestionType.IMAGE_CHOOSE_WORD
                    2 -> {
                        when (gradeBand) {
                            GradeBand.JUNIOR -> QuizQuestionType.SPELL_FILL_BLANK
                            GradeBand.SENIOR -> QuizQuestionType.SPELLING
                            else -> QuizQuestionType.SPELL_FILL_BLANK
                        }
                    }
                    else -> {
                        when (gradeBand) {
                            GradeBand.SENIOR -> QuizQuestionType.SENTENCE_ORDER
                            GradeBand.JUNIOR -> QuizQuestionType.DICTATION
                            else -> QuizQuestionType.IMAGE_CHOOSE_WORD
                        }
                    }
                }

                when (type) {
                    QuizQuestionType.LISTEN_CHOOSE_WORD -> {
                        questions.add(
                            QuizQuestion(
                                id = 100 + questions.size,
                                type = type,
                                question = "Listen and choose the correct word:",
                                audioText = word,
                                options = options,
                                correctIndex = options.indexOf(word)
                            )
                        )
                    }
                    QuizQuestionType.IMAGE_CHOOSE_WORD -> {
                        questions.add(
                            QuizQuestion(
                                id = 200 + questions.size,
                                type = type,
                                question = "Which word matches?",
                                options = options,
                                correctIndex = options.indexOf(word)
                            )
                        )
                    }
                    QuizQuestionType.SPELL_FILL_BLANK -> {
                        val letters = word.toCharArray().filter { it.isLetter() }.map { it.lowercaseChar() }
                        // Blank the 2nd letter so the word display and the correct
                        // option always agree (e.g. "s_chool" -> missing 'c').
                        val missing = if (letters.size > 1) letters[1] else letters.firstOrNull() ?: 's'
                        val missingStr = missing.toString()
                        val spellDistractors = listOf("a", "e", "o", "u").filter { it != missingStr }.take(3)
                        val spellOptions = (listOf(missingStr) + spellDistractors).shuffled()
                        questions.add(
                            QuizQuestion(
                                id = 300 + questions.size,
                                type = type,
                                question = "Complete the word: ${word.first()}_${word.drop(1)}",
                                options = spellOptions,
                                correctIndex = spellOptions.indexOf(missingStr)
                            )
                        )
                    }
                    QuizQuestionType.SENTENCE_ORDER -> {
                        questions.add(
                            QuizQuestion(
                                id = 400 + questions.size,
                                type = type,
                                question = "Order: '$word' in a sentence",
                                options = listOf("I", "a", "see", word),
                                correctOrder = listOf("I", "see", "a", word)
                            )
                        )
                    }
                    QuizQuestionType.SPELLING -> {
                        questions.add(
                            QuizQuestion(
                                id = 450 + questions.size,
                                type = type,
                                question = "Type the spelling of: '$word'",
                                audioText = word,
                                options = listOf(word),
                                correctIndex = 0
                            )
                        )
                    }
                    QuizQuestionType.DICTATION -> {
                        val sentence = "I see the $word."
                        questions.add(
                            QuizQuestion(
                                id = 460 + questions.size,
                                type = type,
                                question = "Listen and type the sentence:",
                                audioText = sentence,
                                options = listOf(sentence),
                                correctIndex = 0
                            )
                        )
                    }
                    else -> {
                        // fallback for exhaustive when
                        questions.add(
                            QuizQuestion(
                                id = 500 + questions.size,
                                type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                                question = "Which word matches?",
                                options = options,
                                correctIndex = options.indexOf(word)
                            )
                        )
                    }
                }
            }
            loopCount++
        }

        val result = questions.take(10).shuffled()
        // Keep phonics questions in the final shuffled set but never drop the
        // error-book review questions (they must not be randomly lost).
        val reviewIds = reviewQ.map { it.id }
        return result.sortedBy { if (it.id in reviewIds) 0 else if (it.id in phonicsIds) 1 else 2 }
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
