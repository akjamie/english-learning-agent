package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LlmRepository
import javax.inject.Inject

/**
 * Structured-output gateway for the LLM harness (Sprint 21).
 *
 * Wraps [LlmRepository.complete] with a validate-and-repair loop: the returned
 * text is validated against the task's JSON contract ([AgentJsonValidator]) and,
 * on a validation failure, the model is re-invoked once with the validation
 * error appended to the prompt. This turns one-shot "random parse failures"
 * into a self-healing call that only surfaces a failure after the repair attempt.
 *
 * @param maxRepairAttempts total LLM invocations allowed per call (1 original +
 *                          up to N repairs). Defaults to 2.
 */
class StructuredLlmUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    private val jsonValidator: AgentJsonValidator
) {
    /**
     * Runs [prompt] for [taskType], validating the response JSON shape. On invalid
     * JSON, re-invokes the model with the validation reason so it can fix its own
     * output. Returns the first valid response, or the last attempt's failure.
     */
    suspend fun completeJson(
        prompt: String,
        taskType: String,
        maxTokens: Int = 500,
        maxRepairAttempts: Int = 2
    ): Result<String> {
        var currentPrompt = prompt
        var lastResult: Result<String> = Result.failure(Exception("No LLM attempt made"))
        var attempts = 0

        while (attempts < maxRepairAttempts) {
            val result = llmRepository.complete(currentPrompt, taskType, maxTokens)
            lastResult = result
            if (result.isFailure) return result

            val content = result.getOrThrow()
            val validation = jsonValidator.validate(taskType, content)
            if (validation is AgentJsonValidator.ValidationResult.Valid) {
                return result
            }

            attempts++
            if (attempts >= maxRepairAttempts) {
                return Result.failure(
                    Exception("${(validation as AgentJsonValidator.ValidationResult.Invalid).reason} (after $maxRepairAttempts LLM attempts)")
                )
            }

            val reason = (validation as AgentJsonValidator.ValidationResult.Invalid).reason
            currentPrompt = "$prompt\n\nYour previous response was rejected because: $reason. " +
                "Please fix it and return ONLY the corrected raw JSON (no markdown, no prefix text)."
        }
        return lastResult
    }
}
