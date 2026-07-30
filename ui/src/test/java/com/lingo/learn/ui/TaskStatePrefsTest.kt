package org.akj.lingo.learn.ui

import android.content.Context
import android.content.SharedPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito

class TaskStatePrefsTest {

    private lateinit var context: Context
    private lateinit var prefs: InMemorySharedPreferences

    @BeforeEach
    fun setup() {
        prefs = InMemorySharedPreferences()
        context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
    }

    @Test
    fun `default state is NOT_STARTED`() {
        assertEquals(TaskState.NOT_STARTED, TaskStatePrefs.getTaskState(context))
    }

    @Test
    fun `setTaskState saves and retrieves state`() {
        TaskStatePrefs.setTaskState(context, TaskState.IN_PROGRESS)
        assertEquals(TaskState.IN_PROGRESS, TaskStatePrefs.getTaskState(context))
    }

    @Test
    fun `round-trip through all states`() {
        for (state in TaskState.entries) {
            TaskStatePrefs.setTaskState(context, state)
            assertEquals(state, TaskStatePrefs.getTaskState(context))
        }
    }

    @Test
    fun `saveCheckpoint stores all fields`() {
        val checkpoint = Checkpoint(
            stage = "PRACTICE",
            phase = "READ_ALONG",
            questionIndex = 3,
            score = 42,
            timestamp = 123456789L
        )
        TaskStatePrefs.saveCheckpoint(context, checkpoint)

        val restored = TaskStatePrefs.getCheckpoint(context)
        assertEquals(checkpoint.stage, restored.stage)
        assertEquals(checkpoint.phase, restored.phase)
        assertEquals(checkpoint.questionIndex, restored.questionIndex)
        assertEquals(checkpoint.score, restored.score)
        assertEquals(checkpoint.timestamp, restored.timestamp)
    }

    @Test
    fun `getCheckpoint returns default when no checkpoint saved`() {
        val checkpoint = TaskStatePrefs.getCheckpoint(context)
        assertEquals("IMMERSION", checkpoint.stage)
        assertEquals(0, checkpoint.questionIndex)
        assertEquals(0, checkpoint.score)
    }

    @Test
    fun `clearCheckpoint resets to defaults`() {
        TaskStatePrefs.saveCheckpoint(context, Checkpoint(stage = "QUIZ", questionIndex = 5))
        TaskStatePrefs.clearCheckpoint(context)

        val checkpoint = TaskStatePrefs.getCheckpoint(context)
        assertEquals("IMMERSION", checkpoint.stage)
        assertEquals(0, checkpoint.questionIndex)
    }

    @Test
    fun `task day round-trip`() {
        assertEquals("", TaskStatePrefs.getTaskDay(context))
        TaskStatePrefs.setTaskDay(context, "2026-07-30")
        assertEquals("2026-07-30", TaskStatePrefs.getTaskDay(context))
    }

    @Test
    fun `state transitions from NOT_STARTED to IN_PROGRESS to PAUSED to COMPLETED`() {
        assertEquals(TaskState.NOT_STARTED, TaskStatePrefs.getTaskState(context))

        TaskStatePrefs.setTaskState(context, TaskState.IN_PROGRESS)
        assertEquals(TaskState.IN_PROGRESS, TaskStatePrefs.getTaskState(context))

        TaskStatePrefs.setTaskState(context, TaskState.PAUSED)
        assertEquals(TaskState.PAUSED, TaskStatePrefs.getTaskState(context))

        TaskStatePrefs.setTaskState(context, TaskState.COMPLETED)
        assertEquals(TaskState.COMPLETED, TaskStatePrefs.getTaskState(context))
    }

    @Test
    fun `checkpoint preserves score across pause resume`() {
        TaskStatePrefs.saveCheckpoint(context, Checkpoint(
            stage = "PRACTICE",
            phase = "GAME",
            questionIndex = 4,
            score = 30
        ))

        val ck = TaskStatePrefs.getCheckpoint(context)
        assertEquals(30, ck.score)
        assertEquals("GAME", ck.phase)
    }
}

/** In-memory SharedPreferences implementation for unit testing. */
class InMemorySharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll() = map.toMap()
    override fun getString(key: String, defValue: String?) = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String, defValue: MutableSet<String>?) = (map[key] as? MutableSet<String>) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = (map[key] as? Boolean) ?: defValue
    override fun getInt(key: String, defValue: Int) = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long) = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float) = (map[key] as? Float) ?: defValue
    override fun contains(key: String) = map.containsKey(key)

    override fun edit() = Editor()

    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.add(l) }
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.remove(l) }

    inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, value: MutableSet<String>?) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun remove(key: String) = apply { pending[key] = null }
        override fun clear() = apply { pending.clear() }
        override fun apply() = run { map.putAll(pending.filterValues { it != null }); pending.filterValues { it == null }.keys.forEach { map.remove(it) } }
        override fun commit() = true.also { apply() }
    }
}
