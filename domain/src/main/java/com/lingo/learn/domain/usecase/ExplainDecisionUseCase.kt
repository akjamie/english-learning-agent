package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.Plan
import org.json.JSONObject
import javax.inject.Inject

/**
 * Sprint 6 (Enhancement 2) - explains "why" a plan day was arranged the way it was.
 *
 * Reads the persisted [Plan.rationaleSnapshot] produced at generation time instead of
 * re-deriving an explanation via a new LLM call. Returns null when no rationale exists
 * (e.g., plans generated before Sprint 6).
 */
class ExplainDecisionUseCase @Inject constructor() {

    /** Returns the stored rationale for a single plan day, or null if unavailable. */
    fun dayRationale(plan: Plan?, day: Int): String? {
        val raw = plan?.rationaleSnapshot?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val json = JSONObject(raw)
            val daysArray = json.optJSONArray("days") ?: return null
            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                if (dayObj.optInt("day", -1) == day) {
                    return dayObj.optString("rationale", "").ifBlank { null }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /** Returns rationale for every day in the plan as (day, rationale) pairs. */
    fun allDayRationales(plan: Plan?): List<Pair<Int, String>> {
        val raw = plan?.rationaleSnapshot?.takeIf { it.isNotBlank() } ?: return emptyList()
        return try {
            val json = JSONObject(raw)
            val daysArray = json.optJSONArray("days") ?: return emptyList()
            val result = mutableListOf<Pair<Int, String>>()
            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val rationale = dayObj.optString("rationale", "")
                if (rationale.isNotBlank()) {
                    result.add(dayObj.optInt("day", i + 1) to rationale)
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
