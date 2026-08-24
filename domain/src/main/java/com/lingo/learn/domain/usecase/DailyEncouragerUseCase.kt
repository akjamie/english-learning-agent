package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LlmRepository
import javax.inject.Inject

class DailyEncouragerUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    private val promptRegistry: AgentPromptRegistry
) {
    suspend operator fun invoke(name: String, streak: Int): Result<String> {
        val prompt = promptRegistry.render(
            "ENCOURAGEMENT",
            mapOf(
                "name" to name,
                "streak" to streak.toString()
            )
        )
        
        return llmRepository.complete(prompt = prompt, taskType = "ENCOURAGEMENT", maxTokens = 60)
    }
}
