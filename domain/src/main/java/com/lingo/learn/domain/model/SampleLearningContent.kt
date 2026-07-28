package org.akj.lingo.learn.domain.model

/**
 * Hardcoded sample learning content for the "School Life" theme.
 * This serves as MVP placeholder data until Sprint 3 introduces LLM-driven
 * weekly plan generation. All content follows the i+1 comprehensible input
 * principle with simple vocabulary suitable for Grade 4.
 */
object SampleLearningContent {

    fun createSchoolLifeSession(): LearningSession {
        return LearningSession(
            theme = "School Life",
            subtitleLines = listOf(
                SubtitleLine(
                    id = 1,
                    startTimeMs = 0,
                    endTimeMs = 4000,
                    text = "Good morning! Welcome to our school.",
                    newWords = listOf("morning", "welcome")
                ),
                SubtitleLine(
                    id = 2,
                    startTimeMs = 4000,
                    endTimeMs = 8000,
                    text = "This is my classroom. It is big and bright.",
                    newWords = listOf("classroom", "bright")
                ),
                SubtitleLine(
                    id = 3,
                    startTimeMs = 8000,
                    endTimeMs = 12000,
                    text = "I have many friends here. We learn together every day.",
                    newWords = listOf("friends", "together")
                ),
                SubtitleLine(
                    id = 4,
                    startTimeMs = 12000,
                    endTimeMs = 16000,
                    text = "My teacher is very kind. She helps us read and write.",
                    newWords = listOf("teacher", "kind")
                ),
                SubtitleLine(
                    id = 5,
                    startTimeMs = 16000,
                    endTimeMs = 20000,
                    text = "I love my school. Learning is fun!",
                    newWords = listOf("love", "learning")
                )
            ),
            readAlongSentences = listOf(
                ReadAlongSentence(
                    id = 1,
                    text = "Good morning, teacher!",
                    chineseHint = "老师，早上好！"
                ),
                ReadAlongSentence(
                    id = 2,
                    text = "This is my classroom.",
                    chineseHint = "这是我的教室。"
                ),
                ReadAlongSentence(
                    id = 3,
                    text = "I love my school.",
                    chineseHint = "我爱我的学校。"
                )
            ),
            gameQuestions = listOf(
                GameQuestion(
                    id = 1,
                    type = GameType.DRAG_MATCH,
                    prompt = "Which word means '教室' (classroom)?",
                    options = listOf("classroom", "teacher", "friend", "morning"),
                    correctIndex = 0
                ),
                GameQuestion(
                    id = 2,
                    type = GameType.LISTEN_CHOOSE_IMAGE,
                    prompt = "Listen and choose the correct picture",
                    audioText = "teacher",
                    options = listOf("👩‍🏫 Teacher", "🏫 School", "📚 Book", "✏️ Pencil"),
                    correctIndex = 0
                ),
                GameQuestion(
                    id = 3,
                    type = GameType.DRAG_MATCH,
                    prompt = "Which word means '朋友' (friend)?",
                    options = listOf("morning", "bright", "friend", "welcome"),
                    correctIndex = 2
                )
            ),
            quizQuestions = listOf(
                QuizQuestion(
                    id = 1,
                    type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                    question = "What is this place? 🏫",
                    options = listOf("classroom", "garden", "kitchen", "bedroom"),
                    correctIndex = 0
                ),
                QuizQuestion(
                    id = 2,
                    type = QuizQuestionType.LISTEN_CHOOSE_WORD,
                    question = "Listen and choose the word you hear",
                    audioText = "teacher",
                    options = listOf("teacher", "student", "morning", "friend"),
                    correctIndex = 0
                ),
                QuizQuestion(
                    id = 3,
                    type = QuizQuestionType.SPELL_FILL_BLANK,
                    question = "Fill in the missing letter: cla__room",
                    options = listOf("s", "t", "k", "p"),
                    correctIndex = 0,
                    isFromErrorBook = true
                ),
                QuizQuestion(
                    id = 4,
                    type = QuizQuestionType.SENTENCE_ORDER,
                    question = "Put the words in the correct order",
                    options = listOf("is", "This", "classroom", "my"),
                    correctOrder = listOf("This", "is", "my", "classroom")
                ),
                QuizQuestion(
                    id = 5,
                    type = QuizQuestionType.READ_ALOUD,
                    question = "Read this sentence aloud",
                    audioText = "I love my school.",
                    isFromErrorBook = true
                )
            ),
            targetNewWords = listOf(
                "morning", "welcome", "classroom", "bright",
                "friends", "together", "teacher", "kind", "love", "learning"
            )
        )
    }
}
