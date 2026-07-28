package org.akj.lingo.learn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LingoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyReminderWorker.schedule(this)
    }
}
