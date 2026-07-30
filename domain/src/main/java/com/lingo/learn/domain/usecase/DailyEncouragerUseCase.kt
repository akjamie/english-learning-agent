package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LlmRepository
import javax.inject.Inject

class DailyEncouragerUseCase @Inject constructor(
    private val llmRepository: LlmRepository
) {
    suspend operator fun invoke(name: String, streak: Int): Result<String> {
        val prompt = """
            You are the motivational assistant for Lingo English.
            Generate a short push notification or banner greeting for a student named $name who is on a $streak day streak.
            
            --- Tone and Rules ---
            - Enthusiastic, warm, and gamified.
            - Must be less than 40 characters.
            - Use exactly 1 appropriate emoji.
            - Do not mention tasks as "unfinished" or "obligations". Use invitations instead.
        """.trimIndent()
        
        return llmRepository.complete(prompt = prompt, taskType = "ENCOURAGEMENT", maxTokens = 60)
    }
}
