package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LlmRepository
import javax.inject.Inject

class ExplanationAgentUseCase @Inject constructor(
    private val llmRepository: LlmRepository
) {
    suspend operator fun invoke(word: String, errorType: String, grade: String): Result<String> {
        val prompt = """
            You are Lingo, a friendly fox tutor. Explain the word "$word" to a $grade child.
            The child got this word wrong in a quiz due to: $errorType.
            
            --- Guidelines ---
            1. Use encouraging, warm, and simple English.
            2. Provide one clear example sentence suitable for children.
            3. Highlight a quick mnemonic trick or spelling tip (e.g., "hear has an 'ear' inside it").
            4. Keep the explanation under 100 words. Do not be overly academic.
        """.trimIndent()
        
        return llmRepository.complete(prompt = prompt, taskType = "EXPLAIN", maxTokens = 150)
    }
}
