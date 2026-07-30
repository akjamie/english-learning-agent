package org.akj.lingo.learn.ui

import android.content.Context
import android.content.SharedPreferences

enum class TaskState {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    EXPIRED
}

data class Checkpoint(
    val stage: String = "IMMERSION",
    val phase: String? = null,
    val questionIndex: Int = 0,
    val score: Int = 0,
    val timestamp: Long = 0L
)

object TaskStatePrefs {
    private const val PREFS_NAME = "lingo_task_prefs"
    private const val KEY_TASK_STATE = "task_state"
    private const val KEY_CK_STAGE = "ck_stage"
    private const val KEY_CK_PHASE = "ck_phase"
    private const val KEY_CK_INDEX = "ck_index"
    private const val KEY_CK_SCORE = "ck_score"
    private const val KEY_CK_TIMESTAMP = "ck_timestamp"
    private const val KEY_DAY = "task_day"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTaskState(context: Context): TaskState {
        val name = getPrefs(context).getString(KEY_TASK_STATE, TaskState.NOT_STARTED.name) ?: TaskState.NOT_STARTED.name
        return try { TaskState.valueOf(name) } catch (e: Exception) { TaskState.NOT_STARTED }
    }

    fun setTaskState(context: Context, state: TaskState) {
        getPrefs(context).edit().putString(KEY_TASK_STATE, state.name).apply()
    }

    fun getCheckpoint(context: Context): Checkpoint {
        val prefs = getPrefs(context)
        return Checkpoint(
            stage = prefs.getString(KEY_CK_STAGE, "IMMERSION") ?: "IMMERSION",
            phase = prefs.getString(KEY_CK_PHASE, null),
            questionIndex = prefs.getInt(KEY_CK_INDEX, 0),
            score = prefs.getInt(KEY_CK_SCORE, 0),
            timestamp = prefs.getLong(KEY_CK_TIMESTAMP, 0L)
        )
    }

    fun saveCheckpoint(context: Context, checkpoint: Checkpoint) {
        getPrefs(context).edit()
            .putString(KEY_CK_STAGE, checkpoint.stage)
            .putString(KEY_CK_PHASE, checkpoint.phase)
            .putInt(KEY_CK_INDEX, checkpoint.questionIndex)
            .putInt(KEY_CK_SCORE, checkpoint.score)
            .putLong(KEY_CK_TIMESTAMP, checkpoint.timestamp)
            .apply()
    }

    fun clearCheckpoint(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_CK_STAGE)
            .remove(KEY_CK_PHASE)
            .remove(KEY_CK_INDEX)
            .remove(KEY_CK_SCORE)
            .remove(KEY_CK_TIMESTAMP)
            .apply()
    }

    fun getTaskDay(context: Context): String {
        return getPrefs(context).getString(KEY_DAY, "") ?: ""
    }

    fun setTaskDay(context: Context, day: String) {
        getPrefs(context).edit().putString(KEY_DAY, day).apply()
    }
}
