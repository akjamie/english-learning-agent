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
import org.akj.lingo.learn.ui.StreakPrefs
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createNotificationChannel()

        // Sprint 12: respect the user's reminder preferences (mirrored plain prefs).
        val reminderEnabled = applicationContext
            .getSharedPreferences("lingo_reminder_prefs", Context.MODE_PRIVATE)
            .getBoolean("reminder_enabled", true)
        if (!reminderEnabled) return Result.success()

        showNotification()
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

        val (title, body) = when {
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

    companion object {
        private const val CHANNEL_ID = "lingo_daily_reminder"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "lingo_daily_reminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
