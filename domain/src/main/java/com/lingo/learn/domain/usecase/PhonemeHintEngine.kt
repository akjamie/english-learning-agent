package org.akj.lingo.learn.domain.usecase

/**
 * Sprint 8 — Phoneme Hint Engine (Transition / Bridge Solution)
 *
 * Addresses the structural gap in our ASR-based pronunciation evaluation pipeline:
 * ASR engines silently correct phoneme errors (e.g. child says "sink" for "think",
 * ASR returns "think"). Edit-distance comparison never sees the real mistake.
 *
 * This engine does NOT fix the structural problem — it works AROUND it by detecting
 * likely phoneme confusions BEFORE ASR normalisation can hide them, using two strategies:
 *
 *  1. **Minimal-pair word detection**: checks if the ASR transcript returned a known
 *     "confusion word" that is the typical substitution for a target phoneme group.
 *     E.g. if the reference contains "think" and ASR returns "sink", we detect
 *     that "sink" is the /θ/ → /s/ confusion and fire the th-sound hint.
 *
 *  2. **Target-word risk flagging**: even if ASR was unable to distinguish (and
 *     returned the reference word correctly), if the reference contains a word known
 *     to be high-risk for Chinese learners (e.g. any word with "th", "v", "r+vowel"),
 *     we proactively append a gentle reminder tip.
 *
 * This is a zero-cost-infrastructure improvement: no new API calls, no new billing,
 * pure Kotlin logic.  It runs immediately after ASR returns, before scores are shown.
 *
 * When the Pronunciation Assessment API research (Sprint 8 Item 1) concludes and a
 * proper phoneme-level API is integrated, this class can be either retired or kept
 * as a complementary heuristic layer.
 */
class PhonemeHintEngine {

    data class PhonemeHint(
        /** Short human-readable phoneme label shown to child/parent. */
        val phonemeLabel: String,
        /** Friendly tip in English (child-facing). */
        val tipEnglish: String,
        /** Parent companion tip in Chinese (parent-facing). */
        val tipChinese: String,
        /** Emoji that visually reinforces the articulation. */
        val emoji: String
    )

    /**
     * Analyses [referenceText] and [asrTranscript] and returns a list of
     * [PhonemeHint]s that should be shown to the child / parent.
     *
     * @param referenceText  the target sentence / word the child was asked to read
     * @param asrTranscript  the raw transcript returned by the ASR engine
     */
    fun detectHints(referenceText: String, asrTranscript: String): List<PhonemeHint> {
        val refWords  = referenceText.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val asrWords  = asrTranscript.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        val hints = mutableListOf<PhonemeHint>()
        val fired = mutableSetOf<String>() // de-duplicate: only one hint per phoneme group

        for (refWord in refWords) {
            val clean = refWord.replace("[^a-z]".toRegex(), "")

            // Strategy 1 — Minimal-pair confusion detection
            for ((confusionSet, hint) in CONFUSION_MAP) {
                if (clean in confusionSet.targetWords) {
                    // Check if ASR returned a known substitution instead
                    val confusionDetected = asrWords.any { it.replace("[^a-z]".toRegex(), "") in confusionSet.confusionWords }
                    if (confusionDetected && confusionSet.id !in fired) {
                        hints.add(hint)
                        fired.add(confusionSet.id)
                    }
                }
            }

            // Strategy 2 — High-risk word proactive reminder
            for ((riskSet, hint) in RISK_MAP) {
                if (riskSet.any { clean.contains(it) } && hint.phonemeLabel !in fired) {
                    if (hint.phonemeLabel !in fired) {
                        hints.add(hint)
                        fired.add(hint.phonemeLabel)
                    }
                }
            }
        }

        return hints.take(2) // Cap at 2 hints per session to avoid overwhelming the child
    }

    // -------------------------------------------------------------------------
    // Data tables
    // -------------------------------------------------------------------------

    private data class ConfusionSet(
        val id: String,
        /** Words containing the target phoneme that Chinese learners commonly mispronounce. */
        val targetWords: Set<String>,
        /** Typical ASR-output substitution words that indicate a phoneme confusion. */
        val confusionWords: Set<String>
    )

    private val CONFUSION_MAP: List<Pair<ConfusionSet, PhonemeHint>> = listOf(

        // /θ/ (voiceless th) → /s/
        Pair(
            ConfusionSet(
                id = "th_voiceless",
                targetWords  = setOf("think", "three", "thank", "thought", "through", "thin", "teeth", "both", "truth", "health", "math", "bath", "cloth"),
                confusionWords = setOf("sink", "see", "free", "sank", "sought", "sin", "tees", "bose", "truce", "heals", "mass", "bass", "class")
            ),
            PhonemeHint(
                phonemeLabel = "th_voiceless",
                tipEnglish   = "💡 The 'th' sound: gently place your tongue between your top and bottom teeth, then blow air — like you're fogging up a mirror!",
                tipChinese   = "👅 练'th'音：把舌尖轻轻夹在上下牙齿之间，然后呼气，就像往镜子上哈气一样！可以跟孩子一起做这个动作。",
                emoji = "👅"
            )
        ),

        // /ð/ (voiced th) → /d/
        Pair(
            ConfusionSet(
                id = "th_voiced",
                targetWords  = setOf("the", "this", "that", "there", "then", "them", "they", "those", "these", "though", "with", "breathe"),
                confusionWords = setOf("da", "dis", "dat", "dare", "den", "dem", "day", "dose", "dese", "doe", "wit", "breed")
            ),
            PhonemeHint(
                phonemeLabel = "th_voiced",
                tipEnglish   = "💡 The voiced 'th' (like in 'the'): same tongue position as before, but now make your voice hum while blowing — feel the vibration!",
                tipChinese   = "🎵 浊音'th'（如'the'）：舌头夹住，同时让喉咙发出声音，用手摸喉咙能感受到振动。跟孩子一起摸着喉咙练！",
                emoji = "🎵"
            )
        ),

        // /v/ → /w/ or /b/
        Pair(
            ConfusionSet(
                id = "v_sound",
                targetWords  = setOf("very", "voice", "visit", "village", "van", "vine", "vote", "have", "love", "live", "give", "arrive", "move"),
                confusionWords = setOf("wery", "woise", "wisit", "willage", "wan", "wine", "wote", "habe", "lobe", "libe", "gibe", "arribe", "moob")
            ),
            PhonemeHint(
                phonemeLabel = "v_sound",
                tipEnglish   = "💡 The 'v' sound: bite your lower lip gently with your top teeth, then blow and hum — like a buzzing bee!",
                tipChinese   = "🐝 练'v'音：用上牙轻咬下唇，然后呼气发声——就像蜜蜂嗡嗡叫！跟孩子一起做，很好玩！",
                emoji = "🐝"
            )
        ),

        // /r/ → /l/ (common for Chinese L1 speakers)
        Pair(
            ConfusionSet(
                id = "r_vs_l",
                targetWords  = setOf("right", "red", "run", "rain", "read", "road", "room", "rice", "rock", "ring", "river", "rabbit", "round"),
                confusionWords = setOf("light", "led", "lun", "lain", "lead", "load", "loom", "lice", "lock", "ling", "liver", "labbit", "lound")
            ),
            PhonemeHint(
                phonemeLabel = "r_vs_l",
                tipEnglish   = "💡 The English 'r': curl your tongue back a little — don't touch the roof of your mouth. Your tongue floats in the air!",
                tipChinese   = "👄 英语'r'音：舌头向后卷起来，但不要碰到上颚，让舌头悬在嘴里。可以让孩子对着镜子练习！",
                emoji = "👄"
            )
        ),

        // Short /ɪ/ vs Long /iː/ — sheep/ship confusion
        Pair(
            ConfusionSet(
                id = "short_long_vowel",
                targetWords  = setOf("sheep", "sleep", "feet", "seat", "heat", "beat", "meet", "feel", "wheel"),
                confusionWords = setOf("ship", "slip", "fit", "sit", "hit", "bit", "mit", "fill", "will")
            ),
            PhonemeHint(
                phonemeLabel = "short_long_vowel",
                tipEnglish   = "💡 Long 'ee' sound (like 'sheep'): stretch your lips wide like you're smiling and hold the sound longer — eeee!",
                tipChinese   = "😁 长'ee'音（如sheep）：嘴角向两边拉开，像微笑一样，把声音拉长——让孩子对镜子练习微笑发音！",
                emoji = "😁"
            )
        )
    )

    /**
     * High-risk phoneme patterns: if the reference CONTAINS these letter patterns,
     * proactively remind even if ASR did not visibly detect a substitution.
     * Keyed by substrings to search in the cleaned reference word.
     */
    private val RISK_MAP: List<Pair<List<String>, PhonemeHint>> = listOf(
        Pair(
            listOf("th"),
            PhonemeHint(
                phonemeLabel = "th_voiceless",
                tipEnglish = "💡 Remember: 'th' words need your tongue between your teeth!",
                tipChinese = "提示：含'th'的单词需要舌头夹在牙齿间发音，中文里没有这个音，需要多练！",
                emoji = "👅"
            )
        ),
        Pair(
            listOf("wh"),
            PhonemeHint(
                phonemeLabel = "wh_sound",
                tipEnglish = "💡 'wh' words: round your lips and blow gently — like blowing out a candle!",
                tipChinese = "🕯️ 含'wh'的单词：嘴唇收圆，轻轻吹气——就像吹蜡烛！",
                emoji = "🕯️"
            )
        )
    )
}
