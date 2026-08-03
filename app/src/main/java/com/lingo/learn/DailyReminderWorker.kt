package org.akj.lingo.learn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.akj.lingo.learn.data.prefs.WidgetContentCache
import org.akj.lingo.learn.domain.model.WidgetContent
import org.akj.lingo.learn.domain.model.WidgetInput
import org.akj.lingo.learn.domain.usecase.WidgetContentGenerator
import org.akj.lingo.learn.ui.StreakPrefs
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createNotificationChannel()

        // Sprint 12: respect the user's reminder preferences (mirrored plain prefs).
        val reminderPrefs = applicationContext
            .getSharedPreferences("lingo_reminder_prefs", Context.MODE_PRIVATE)
        val reminderEnabled = reminderPrefs.getBoolean("reminder_enabled", true)
        if (!reminderEnabled) {
            refreshWidget()
            return Result.success()
        }

        showNotification()
        refreshWidget()
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lingo Daily Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to practice English daily"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val streakDays = StreakPrefs.getStreakDays(applicationContext)
        val todayDone = StreakPrefs.isTodayDone(applicationContext)

        // Sprint 14: use personalized notification text from WidgetContentCache
        // if available, falling back to the template-based text.
        val cached = WidgetContentCache.get(applicationContext)
        val (title, body) = if (cached != null) {
            Pair(
                cached.emoji + " " + cached.title,
                cached.subtitle
            )
        } else {
            when {
                todayDone -> Pair(
                    "🦊 Great work today!",
                    "You're on a ${streakDays}-day streak! See you tomorrow!"
                )
                streakDays >= 7 -> Pair(
                    "🔥 ${streakDays}-day streak!",
                    "Don't break your streak! 15 minutes is all you need!"
                )
                streakDays > 0 -> Pair(
                    "🦊 Day $streakDays awaits!",
                    "Your ${streakDays}-day streak is waiting for you!"
                )
                else -> Pair(
                    "🦊 Time to practice!",
                    "15 minutes daily keeps your English growing!"
                )
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }

    /**
     * Sprint 14: refresh the widget so it picks up the latest content from
     * [WidgetContentCache]. Also pre-generates content from streak data as a
     * fallback when the DashboardViewModel hasn't run yet (e.g. fresh install).
     */
    private suspend fun refreshWidget() {
        // If no cached content exists, generate a basic version from streak data
        if (WidgetContentCache.get(applicationContext) == null) {
            val streakDays = StreakPrefs.getStreakDays(applicationContext)
            val todayDone = StreakPrefs.isTodayDone(applicationContext)
            val childName = applicationContext
                .getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                .getString("child_name", "Buddy") ?: "Buddy"
            val content = WidgetContentGenerator.generate(
                WidgetInput(
                    streakDays = streakDays,
                    todayDone = todayDone,
                    childName = childName
                )
            )
            WidgetContentCache.save(applicationContext, content)
        }
        LingoStreakWidget.refreshAll(applicationContext)
    }

    companion object {
        private const val CHANNEL_ID = "lingo_daily_reminder"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "lingo_daily_reminder"

        /**
         * Schedules the daily reminder worker. Sprint 14: respects the user's
         * configured reminder hour (from `lingo_reminder_prefs`); falls back to
         * a 1-day interval with 1-day initial delay when no hour is set.
         */
        fun schedule(context: Context) {
            val reminderHour = context
                .getSharedPreferences("lingo_reminder_prefs", Context.MODE_PRIVATE)
                .getInt("reminder_hour", -1)

            val initialDelayMinutes = if (reminderHour in 0..23) {
                // Calculate minutes until the next occurrence of reminderHour
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= now.timeInMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val diffMs = target.timeInMillis - now.timeInMillis
                (diffMs / (60 * 1000)).coerceAtLeast(15) // minimum 15 minutes
            } else {
                // No hour configured: use the original 1-day delay
                TimeUnit.DAYS.toMinutes(1)
            }

            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Re-schedules the worker with the updated reminder hour. Called when
         * the user changes the hour in Settings.
         */
        fun reschedule(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            schedule(context)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
