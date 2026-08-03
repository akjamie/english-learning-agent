package org.akj.lingo.learn

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.akj.lingo.learn.data.prefs.WidgetContentCache
import org.akj.lingo.learn.domain.model.WidgetContent
import org.akj.lingo.learn.ui.StreakPrefs

class LingoStreakWidget : GlanceAppWidget() {

    companion object {
        /**
         * Sprint 14: Re-renders every active widget instance so they pick up the
         * latest content from [WidgetContentCache]. Glance 1.0.0 has no batch
         * `updateAll` on GlanceAppWidget, so we enumerate GlanceIds and update each.
         */
        suspend fun refreshAll(context: Context) {
            GlanceAppWidgetManager(context)
                .getGlanceIds(LingoStreakWidget::class.java)
                .forEach { LingoStreakWidget().update(context, it) }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Sprint 14: read pre-generated personalized content from cache,
        // falling back to a basic template generated from streak data.
        val content = WidgetContentCache.get(context) ?: run {
            val streakDays = StreakPrefs.getStreakDays(context)
            val todayDone = StreakPrefs.isTodayDone(context)
            buildFallbackContent(streakDays, todayDone)
        }

        val bgColor = Color(content.bgColorHex)
        val accentColor = Color(content.accentColorHex)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(bgColor, bgColor))
                    .padding(Dp(12f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = content.emoji,
                    style = TextStyle(fontSize = 36.sp)
                )
                Spacer(GlanceModifier.height(Dp(4f)))
                Text(
                    text = content.title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF2C3E50), Color(0xFF2C3E50))
                    )
                )
                Spacer(GlanceModifier.height(Dp(2f)))
                Text(
                    text = content.subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = ColorProvider(accentColor, accentColor)
                    )
                )
            }
        }
    }

    /**
     * Builds basic [WidgetContent] from streak data when the pre-generated cache
     * is empty (first install, cache cleared, or before the first dashboard load).
     */
    private fun buildFallbackContent(streakDays: Int, todayDone: Boolean): WidgetContent {
        return when {
            streakDays >= 30 -> WidgetContent(
                emoji = "🔥", title = "${streakDays} Day Streak!", subtitle = "Amazing dedication!",
                bgColorHex = 0xFFFFF8E1, accentColorHex = 0xFFFF7052
            )
            streakDays >= 7 -> WidgetContent(
                emoji = "🏆", title = "${streakDays} Day Streak!", subtitle = "Keep it going!",
                bgColorHex = 0xFFF0FFF4, accentColorHex = 0xFF52D68A
            )
            streakDays >= 1 && todayDone -> WidgetContent(
                emoji = "🦊", title = "Day $streakDays", subtitle = "Great job today!",
                bgColorHex = 0xFFFFFDF5, accentColorHex = 0xFF5C6FF2
            )
            streakDays >= 1 -> WidgetContent(
                emoji = "🦊", title = "Day $streakDays Streak", subtitle = "Practice waiting!",
                bgColorHex = 0xFFFFFDF5, accentColorHex = 0xFF5C6FF2
            )
            else -> WidgetContent.DEFAULT
        }
    }
}

class LingoStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LingoStreakWidget()
}
