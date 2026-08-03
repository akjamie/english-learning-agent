package org.akj.lingo.learn.domain.model

/**
 * Rendered widget content produced by [org.akj.lingo.learn.domain.usecase.WidgetContentGenerator].
 * Consumed by the Glance widget and the daily notification worker.
 */
data class WidgetContent(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val bgColorHex: Long,
    val accentColorHex: Long
) {
    companion object {
        /** Default content used when no cache is available (first install, cache cleared). */
        val DEFAULT = WidgetContent(
            emoji = "🦊",
            title = "Lingo English",
            subtitle = "Ready to practice?",
            bgColorHex = 0xFFFFFDF5,
            accentColorHex = 0xFF5C6FF2
        )
    }
}

/**
 * Input data for widget content generation, assembled from SharedPreferences
 * and repository queries by the DashboardViewModel.
 */
data class WidgetInput(
    val streakDays: Int,
    val todayDone: Boolean,
    val childName: String,
    val todayTaskTheme: String? = null,
    val errorCount: Int = 0,
    val totalXp: Int = 0
)
