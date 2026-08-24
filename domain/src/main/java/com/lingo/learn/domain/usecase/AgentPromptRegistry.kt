package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * Versioned registry of every LLM prompt the app sends (Sprint 21).
 *
 * Centralizes the prompts that used to be inlined across repositories and
 * ViewModels so they can be iterated and rolled back as a unit. Each template
 * is keyed by [taskType], carries a [version], and is rendered from `{token}`
 * placeholders. [render] fails fast on a placeholder that wasn't provided or a
 * provided key that the template doesn't contain — a typo surfaces as an
 * exception instead of a silently broken prompt.
 *
 * Pure Kotlin; no Android dependencies, fully unit-testable.
 */
class AgentPromptRegistry @Inject constructor() {

    private data class Template(val version: Int, val body: String)

    private val templates: Map<String, Template> = mapOf(
        "PLAN" to Template(
            version = 1,
            body = """
                You are the curriculum planner for Lingo English.
                Your task is to generate a personalized 7-day English learning plan for a student in {grade} using default textbook.

                --- Student Learning History ---
                - Average Accuracy: {accuracy}%
                - Weak Word Categories: {weak_categories}
                - Completed Milestones: {milestones}

                --- Constraints & Output Format ---
                You must output a raw, valid JSON object ONLY. Do not write markdown blocks like ```json or any prefix text. The JSON must match the following structure:
                {
                  "theme": "Unit theme name",
                  "difficulty_coefficient": {coefficient},
                  "days": [
                    {
                      "day": 1,
                      "focus": "Vocabulary / Grammar / Dialogue",
                      "target_words": ["word1", "word2"],
                      "reference_sentence": "Standard practice sentence of the day",
                      "duration_minutes": {duration},
                      "rationale": "One sentence explaining WHY this day's content was arranged this way, referencing the student's specific learning history (e.g., 'Your listening accuracy was 72% last week, so today focuses on listening-intensive vocabulary')"
                    }
                  ]
                }
            """.trimIndent()
        ),
        "DIAGNOSIS" to Template(
            version = 1,
            body = """
                You are an English curriculum assessment expert for {band} students.
                Generate 10 grade-appropriate English diagnostic questions for a student in {grade}.

                Difficulty coefficient: {coefficient}
                Vocabulary range: {vocabulary_range}
                Max words per sentence: {max_words}

                IMPORTANT: Generate HIGHLY RANDOMIZED and DIVERSE questions. Do not use the same questions every time.
                Mix up the vocabulary, grammar points, and scenarios completely.

                Use ONLY these question types (the app renders exactly these):
                - LISTENING_EMOJI: "voicePrompt" is the word spoken, "options" are emoji choices, "correctAnswer" matches one option.
                - VOCABULARY: simple word choice, "options" are plain word choices, "correctAnswer" matches one option.
                - PHONICS: sound phonics choice, "options" are word choices, "correctAnswer" matches one option.
                - SORT_WORDS: child taps words in order; provide "wordsForSort" (the full bank, 3-5 words) and "correctAnswer" as the words space-separated in the correct order (e.g. "I see a cat").
                - SPEAK_ALOUD: pronunciation; "voicePrompt" is the sentence to read aloud, no options needed.
                - CHOOSE_LETTER: letter completion; "wordWithBlank" shows the word with one letter replaced by "_" (e.g. "h_istory"), "options" are single letters, "correctAnswer" is the missing letter.
                - LISTEN_AND_TYPE: "voicePrompt" is the dictated sentence, "correctAnswer" is the exact sentence, no options needed.

                Return raw valid JSON array ONLY (no markdown, no backticks). Each object uses only the fields its type needs:
                [
                  {
                    "id": 1,
                    "type": "LISTENING_EMOJI",
                    "title": "1. Listen and Choose",
                    "description": "Select the word you hear:",
                    "voicePrompt": "apple",
                    "options": ["🍎 Apple", "🍌 Banana", "🐱 Cat"],
                    "correctAnswer": "🍎 Apple"
                  },
                  {
                    "id": 2,
                    "type": "SORT_WORDS",
                    "title": "2. Arrange the Words",
                    "description": "Tap the words in the right order:",
                    "wordsForSort": ["I", "a", "see", "cat"],
                    "correctAnswer": "I see a cat"
                  }
                ]
            """.trimIndent()
        ),
        "EXPLAIN" to Template(
            version = 1,
            body = """
                You are Lingo, a friendly fox tutor. The child asks: "Why can't I remember the word "{word}"?"
                Explain the word "{word}" to a {grade} child in a way that makes it stick.
                {history_section}

                --- Guidelines ---
                1. Use encouraging, warm, and simple English.
                2. Provide one clear example sentence suitable for children.
                3. Highlight a quick mnemonic trick or spelling tip (e.g., "hear has an 'ear' inside it").
                4. If the child's history shows a specific weak skill (e.g., listening vs spelling), give one targeted tip for that skill.
                5. Keep the explanation under 100 words. Do not be overly academic.
            """.trimIndent()
        ),
        "ENCOURAGEMENT" to Template(
            version = 1,
            body = """
                You are the motivational assistant for Lingo English.
                Generate a short push notification or banner greeting for a student named {name} who is on a {streak} day streak.

                --- Tone and Rules ---
                - Enthusiastic, warm, and gamified.
                - Must be less than 40 characters.
                - Use exactly 1 appropriate emoji.
                - Do not mention tasks as "unfinished" or "obligations". Use invitations instead.
            """.trimIndent()
        ),
        "LINGO_LETTER" to Template(
            version = 1,
            body = """
                You are Lingo, the fox tutor. Summarize this week's learning for a parent about their child.

                --- This Week's Data ---
                Weekly accuracy: {accuracy_pct}%
                Sessions completed: {sessions}
                Weak areas: {weak_categories}
                Words to review: {top_error_words}

                --- Guidelines ---
                1. 3-4 short sentences, warm and specific (reference actual numbers/words).
                2. Lead with progress, then ONE gentle area to practice.
                3. Max 60 words. Frame as growth, never as grades.
            """.trimIndent()
        ),
        "ROLEPLAY_SCENARIO" to Template(
            version = 1,
            body = """
                You are Lingo, a friendly fox tutor running an English roleplay session with a child
                (grade {grade}).

                --- Scenario ---
                Setting: {title} ({id})
                Today's vocabulary: {target_words}

                --- Output Format ---
                Output a raw JSON object ONLY, no markdown:
                {
                  "system_prompt": "full system prompt for the scenario (short, playful, max 5 lines)",
                  "opening_line": "Lingo's first greeting line (max 8 words)"
                }
            """.trimIndent()
        ),
        "HINT" to Template(
            version = 1,
            body = "You are Lingo Fox, a friendly English tutor. The child is stuck on a quiz question: '{question}'. The correct answer is: '{answer}'. Give a very short, simple, 1-sentence hint that guides them but doesn't give away the direct answer."
        ),
        "PING" to Template(
            version = 1,
            body = "Hello! Please reply 'OK' to confirm API connection."
        )
    )

    /** Returns the template version for a task type, or null if not registered. */
    fun version(taskType: String): Int? = templates[taskType]?.version

    /**
     * Renders the template for [taskType] with the given [values].
     *
     * @throws IllegalArgumentException if the task type is unknown, a template
     *         placeholder has no provided value, or a provided key isn't used.
     */
    fun render(taskType: String, values: Map<String, String>): String {
        val template = templates[taskType]
            ?: throw IllegalArgumentException("Unknown task type: $taskType")

        val placeholders = PLACEHOLDER_REGEX.findAll(template.body).map { it.groupValues[1] }.toSet()
        val provided = values.keys

        val missing = placeholders - provided
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("Missing values for $taskType: ${missing.joinToString(", ")}")
        }
        val extra = provided - placeholders
        if (extra.isNotEmpty()) {
            throw IllegalArgumentException("Unexpected values for $taskType: ${extra.joinToString(", ")}")
        }

        var rendered = template.body
        values.forEach { (key, value) -> rendered = rendered.replace("{$key}", value) }
        return rendered
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{([a-z_]+)}")
    }
}
