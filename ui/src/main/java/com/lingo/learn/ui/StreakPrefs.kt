package org.akj.lingo.learn.ui

import android.content.Context
import android.content.SharedPreferences

object StreakPrefs {
    private const val PREFS_NAME = "lingo_streak_prefs"
    private const val KEY_STREAK_DAYS = "streak_days"
    private const val KEY_TODAY_DONE = "today_done"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getStreakDays(context: Context): Int = getPrefs(context).getInt(KEY_STREAK_DAYS, 0)

    fun isTodayDone(context: Context): Boolean = getPrefs(context).getBoolean(KEY_TODAY_DONE, false)

    fun saveStreakData(context: Context, streakDays: Int, todayDone: Boolean) {
        getPrefs(context).edit()
            .putInt(KEY_STREAK_DAYS, streakDays)
            .putBoolean(KEY_TODAY_DONE, todayDone)
            .apply()
    }
}
