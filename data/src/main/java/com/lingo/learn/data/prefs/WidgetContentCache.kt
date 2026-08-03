package org.akj.lingo.learn.data.prefs

import android.content.Context
import org.akj.lingo.learn.domain.model.WidgetContent

/**
 * Sprint 14: Plain SharedPreferences cache for pre-generated [WidgetContent].
 *
 * Written by [org.akj.lingo.learn.ui.dashboard.DashboardViewModel] when dashboard
 * data loads (Hilt-injected, has repository access). Read by
 * [org.akj.lingo.learn.LingoStreakWidget] during Glance rendering (no DI available).
 *
 * Uses plain SharedPreferences (not SecureConfigPrefs) so the widget and
 * WorkManager can read without crypto dependencies.
 */
object WidgetContentCache {

    private const val PREFS_NAME = "lingo_widget_cache"
    private const val KEY_EMOJI = "emoji"
    private const val KEY_TITLE = "title"
    private const val KEY_SUBTITLE = "subtitle"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_LAST_UPDATED = "last_updated"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, content: WidgetContent) {
        getPrefs(context).edit()
            .putString(KEY_EMOJI, content.emoji)
            .putString(KEY_TITLE, content.title)
            .putString(KEY_SUBTITLE, content.subtitle)
            .putLong(KEY_BG_COLOR, content.bgColorHex)
            .putLong(KEY_ACCENT_COLOR, content.accentColorHex)
            .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            .apply()
    }

    fun get(context: Context): WidgetContent? {
        val prefs = getPrefs(context)
        val title = prefs.getString(KEY_TITLE, null) ?: return null
        return WidgetContent(
            emoji = prefs.getString(KEY_EMOJI, "🦊") ?: "🦊",
            title = title,
            subtitle = prefs.getString(KEY_SUBTITLE, "") ?: "",
            bgColorHex = prefs.getLong(KEY_BG_COLOR, 0xFFFFFDF5),
            accentColorHex = prefs.getLong(KEY_ACCENT_COLOR, 0xFF5C6FF2)
        )
    }
}
