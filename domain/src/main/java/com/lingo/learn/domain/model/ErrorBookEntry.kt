package org.akj.lingo.learn.domain.model

data class ErrorBookEntry(
    val id: String,
    val vocabId: String,
    val errorCount: Int,
    val errorType: String,             // SPELLED_WRONG / LISTENING_WRONG / GRAMMAR_WRONG / PRONUNCIATION_WRONG
    val lastErrorTimestamp: Long,
    val questionType: String,          // Used for calculating question type weight
    val priorityScore: Float,          // Priority score calculated based on formula in section 7.1
    val status: String,                // TO_REVIEW / CONSOLIDATED / GRADUATION_OBSERVATION / GRADUATED
    val consecutiveCorrectCount: Int,
    val graduationCheckTimestamp: Long,
    val historyJson: String,           // Error history record snapshot
    val lastModified: Long = System.currentTimeMillis()
)
