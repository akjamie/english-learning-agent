package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * Production task scoring (Sprint 7 Phase A5).
 *
 * Completes the scoring logic for active-production question types:
 *  - [TYPE_SPELLING]   : exact word spelling (ignore case)
 *  - [TYPE_DICTATION]  : transcribed sentence, graded by word overlap
 *  - [TYPE_SENTENCE]   : free-form sentence containing the target word, graded
 *    on whether the target word is present and the sentence is non-trivial.
 *
 * The scorer returns a normalized score 0.0 .. 1.0 plus a reason string used for
 * error-book feedback, keeping grading deterministic and unit-testable.
 */
class ProductionTaskScorer @Inject constructor() {

    data class ProductionScore(
        val score: Float,          // 0.0 .. 1.0
        val isCorrect: Boolean,    // score >= PASS_THRESHOLD
        val feedback: String
    )

    fun scoreSpelling(input: String, expected: String): ProductionScore {
        val cleanInput = input.trim()
        val cleanExpected = expected.trim()
        if (cleanInput.isEmpty()) return ProductionScore(0f, false, "It looks like the answer is empty. Try again!")
        if (cleanInput.equals(cleanExpected, ignoreCase = true)) {
            return ProductionScore(1f, true, "Perfect spelling!")
        }
        // Levenshtein-based partial credit so close attempts are encouraged.
        val dist = levenshtein(cleanInput.lowercase(), cleanExpected.lowercase())
        val maxLen = maxOf(cleanInput.length, cleanExpected.length)
        val similarity = 1f - dist.toFloat() / maxLen
        val score = similarity.coerceIn(0f, 1f)
        return ProductionScore(
            score = score,
            isCorrect = score >= PASS_THRESHOLD,
            feedback = if (score >= PASS_THRESHOLD) "Very close! The correct spelling is \"$cleanExpected\"." else "Almost! The correct spelling is \"$cleanExpected\"."
        )
    }

    fun scoreDictation(input: String, expected: String): ProductionScore {
        val inputWords = tokenize(input)
        val expectedWords = tokenize(expected)
        if (inputWords.isEmpty()) return ProductionScore(0f, false, "Nothing was typed. Listen again and try!")
        val overlap = inputWords.count { word -> expectedWords.any { it.equals(word, ignoreCase = true) } }
        val score = overlap.toFloat() / expectedWords.size
        return ProductionScore(
            score = score,
            isCorrect = score >= PASS_THRESHOLD,
            feedback = when {
                score == 1f -> "Perfect dictation! Every word matched."
                score >= 0.7f -> "Great listening! $overlap/${expectedWords.size} words matched."
                else -> "Keep practicing — $overlap/${expectedWords.size} words matched."
            }
        )
    }

    fun scoreSentenceWriting(input: String, targetWord: String): ProductionScore {
        val sentence = input.trim()
        if (sentence.isBlank()) return ProductionScore(0f, false, "Write a sentence to practice your writing!")
        val hasTarget = tokenize(sentence).any { it.equals(targetWord, ignoreCase = true) }
        if (!hasTarget) return ProductionScore(0f, false, "Can you include the word \"$targetWord\" in your sentence?")
        val wordCount = tokenize(sentence).size
        val score = when {
            wordCount >= 6 -> 1f
            wordCount >= 3 -> 0.8f
            else -> 0.6f
        }
        return ProductionScore(
            score = score,
            isCorrect = true,
            feedback = "Great sentence! You used \"$targetWord\"."
        )
    }

    //region Helpers

    private fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9']+")).filter { it.isNotBlank() }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    //endregion

    companion object {
        const val TYPE_SPELLING = "SPELLING"
        const val TYPE_DICTATION = "DICTATION"
        const val TYPE_SENTENCE = "SENTENCE_WRITING"
        const val PASS_THRESHOLD = 0.7f
    }
}
