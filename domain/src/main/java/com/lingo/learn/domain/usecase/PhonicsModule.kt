package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.QuizQuestion
import org.akj.lingo.learn.domain.model.QuizQuestionType
import javax.inject.Inject

/**
 * Phonics blending activities for the PRIMARY grade band (Sprint 7 Phase A3).
 *
 * Produces three kinds of phonics questions using letter tiles and simple
 * CVC (consonant-vowel-consonant) vocabulary:
 *  1. [QuizQuestionType.CVC_BUILD]      - child arranges onset + vowel + coda tiles
 *     into the correct order to spell a CVC word.
 *  2. [QuizQuestionType.ONSET_RIME]     - child picks the rime that completes a word
 *     given its onset (e.g. onset "b" + rime "at" -> "bat").
 *  3. [QuizQuestionType.MINIMAL_PAIRS]  - child listens to a word and picks it from a
 *     pair of words differing by one phoneme (e.g. "cat" vs "hat").
 *
 * The module is pure Kotlin with zero Android dependencies so it is fully
 * unit-testable. Words are chosen from a small curated bank so letter tiles
 * are always known.
 */
class PhonicsModule @Inject constructor() {

    /**
     * Generates a phonics question for the given target word if the word can be
     * decomposed into known phonics patterns, otherwise returns null.
     *
     * @param word       target vocabulary word (lowercased internally)
     * @param questionId unique id for the generated question
     */
    fun buildPhonicsQuestion(word: String, questionId: Int): QuizQuestion? {
        val normalized = word.lowercase().trim()
        val cvc = parseCvc(normalized) ?: return null

        // Alternate between the three phonics activity types by id so children
        // see a variety of blending tasks rather than the same one repeatedly.
        return when (questionId % 3) {
            0 -> buildCvcBuildQuestion(normalized, cvc, questionId)
            1 -> buildOnsetRimeQuestion(normalized, cvc, questionId)
            else -> buildMinimalPairsQuestion(normalized, cvc, questionId)
        }
    }

    /**
     * Returns whether a word can be used by the phonics module (parseable CVC).
     */
    fun isPhonicsWord(word: String): Boolean = parseCvc(word.lowercase().trim()) != null

    //region Question Builders

    private fun buildCvcBuildQuestion(
        word: String,
        cvc: Cvc,
        id: Int
    ): QuizQuestion {
        // Distractor tiles: vowels and consonants not used in the word, so the
        // child must pick exactly the onset / vowel / coda tiles in order.
        val distractors = (VOWELS + CONSONANTS)
            .filter { it != cvc.onset && it != cvc.vowel && it != cvc.coda }
            .shuffled()
            .take(3)

        return QuizQuestion(
            id = id,
            type = QuizQuestionType.CVC_BUILD,
            question = "Build the word: tap ${word.length} tiles in order",
            audioText = word,
            options = (listOf(cvc.onset, cvc.vowel, cvc.coda) + distractors).shuffled(),
            correctIndex = -1, // answer expressed via correctOrder
            correctOrder = listOf(cvc.onset, cvc.vowel, cvc.coda)
        )
    }

    private fun buildOnsetRimeQuestion(
        word: String,
        cvc: Cvc,
        id: Int
    ): QuizQuestion {
        val rime = cvc.vowel + cvc.coda
        // Distractor rimes from the word bank, excluding the correct one.
        val distractors = RIME_BANK
            .filter { it != rime }
            .shuffled()
            .take(3)
        val options = (listOf(rime) + distractors).shuffled()

        return QuizQuestion(
            id = id,
            type = QuizQuestionType.ONSET_RIME,
            question = "\"${cvc.onset}\" + which ending makes \"$word\"?",
            audioText = word,
            options = options,
            correctIndex = options.indexOf(rime)
        )
    }

    private fun buildMinimalPairsQuestion(
        word: String,
        cvc: Cvc,
        id: Int
    ): QuizQuestion {
        // A minimal pair differs by the onset phoneme (e.g. "cat" vs "hat").
        val pair = MINIMAL_PAIR_BANK[word]
            ?: buildFallbackPair(word, cvc)
        val options = (listOf(word, pair)).shuffled()

        return QuizQuestion(
            id = id,
            type = QuizQuestionType.MINIMAL_PAIRS,
            question = "Listen and tap the word you hear",
            audioText = word,
            options = options,
            correctIndex = options.indexOf(word)
        )
    }

    //endregion

    //region Helpers

    private fun parseCvc(word: String): Cvc? {
        if (word.length != 3) return null
        val chars = word.toCharArray()
        val first = chars[0].toString()
        val second = chars[1].toString()
        val third = chars[2].toString()
        if (first !in CONSONANTS || second !in VOWELS || third !in CONSONANTS) return null
        return Cvc(first, second, third)
    }

    private fun buildFallbackPair(word: String, cvc: Cvc): String {
        // Swap the onset with a different consonant to form a plausible pair.
        val alternate = CONSONANTS
            .filter { it != cvc.onset }
            .shuffled()
            .firstOrNull() ?: "b"
        return alternate + cvc.vowel + cvc.coda
    }

    //endregion

    private data class Cvc(val onset: String, val vowel: String, val coda: String)

    companion object {
        private val VOWELS = listOf("a", "e", "i", "o", "u")
        private val CONSONANTS = listOf(
            "b", "c", "d", "f", "g", "h", "j", "k", "l", "m",
            "n", "p", "r", "s", "t", "v", "w", "x", "y", "z"
        )

        // Common English rimes so onset-rime distractors stay plausible.
        private val RIME_BANK = listOf("at", "an", "et", "en", "ot", "op", "un", "ug", "in", "it")

        // Curated minimal pairs (word -> paired word with different onset).
        private val MINIMAL_PAIR_BANK = mapOf(
            "cat" to "hat", "bat" to "mat", "rat" to "hat", "dog" to "log",
            "hog" to "dog", "map" to "cap", "cap" to "nap", "pan" to "can",
            "sun" to "run", "run" to "fun", "pen" to "hen", "hen" to "den",
            "pin" to "bin", "bin" to "win", "tap" to "cap", "top" to "mop",
            "cup" to "pup", "bed" to "red", "red" to "bed", "big" to "pig",
            "pig" to "wig", "hot" to "pot", "pot" to "cot", "jug" to "mug",
            "mug" to "hug", "net" to "pet", "pet" to "wet", "sit" to "hit"
        )
    }
}
