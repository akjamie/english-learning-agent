package org.akj.lingo.learn

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.akj.lingo.learn.ui.StreakPrefs

class LingoStreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val streakDays = StreakPrefs.getStreakDays(context)
        val todayDone = StreakPrefs.isTodayDone(context)

        val emoji: String
        val title: String
        val subtitle: String
        val bgColor: Color
        val accentColor: Color

        when {
            streakDays >= 30 -> {
                emoji = "🔥"; title = "${streakDays} Day Streak!"; subtitle = "Amazing dedication!"
                bgColor = Color(0xFFFFF8E1); accentColor = Color(0xFFFF7052)
            }
            streakDays >= 7 -> {
                emoji = "🏆"; title = "${streakDays} Day Streak!"; subtitle = "Keep it going!"
                bgColor = Color(0xFFF0FFF4); accentColor = Color(0xFF52D68A)
            }
            streakDays >= 1 && todayDone -> {
                emoji = "🦊"; title = "Day $streakDays"; subtitle = "Great job today!"
                bgColor = Color(0xFFFFFDF5); accentColor = Color(0xFF5C6FF2)
            }
            streakDays >= 1 -> {
                emoji = "🦊"; title = "Day $streakDays Streak"; subtitle = "Practice waiting!"
                bgColor = Color(0xFFFFFDF5); accentColor = Color(0xFF5C6FF2)
            }
            else -> {
                emoji = "🦊"; title = "Lingo English"; subtitle = "Ready to practice?"
                bgColor = Color(0xFFFFFDF5); accentColor = Color(0xFF5C6FF2)
            }
        }

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
                    text = emoji,
                    style = TextStyle(fontSize = if (streakDays >= 7) 42.sp else 36.sp)
                )
                Spacer(GlanceModifier.height(Dp(4f)))
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF2C3E50), Color(0xFF2C3E50))
                    )
                )
                Spacer(GlanceModifier.height(Dp(2f)))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = ColorProvider(accentColor, accentColor)
                    )
                )
            }
        }
    }
}

class LingoStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LingoStreakWidget()
}
