package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.WidgetContent
import org.akj.lingo.learn.domain.model.WidgetInput

/**
 * Sprint 14 (Enhancement 5): Generates personalized widget text from the child's
 * real learning data. Replaces the fixed template strings in the widget with
 * data-driven content that references the child's name, streak, today's task,
 * and error-book status.
 *
 * Pure function - no side effects, no Android dependencies. Fully unit-testable.
 */
object WidgetContentGenerator {

    /**
     * Builds a [WidgetContent] from the given [input]. Falls back gracefully
     * when optional fields (todayTaskTheme, errorCount) are absent.
     */
    fun generate(input: WidgetInput): WidgetContent {
        val name = input.childName.ifBlank { "friend" }

        return when {
            input.streakDays >= 30 -> WidgetContent(
                emoji = "🔥",
                title = "$name, ${input.streakDays} days!",
                subtitle = "Incredible streak!",
                bgColorHex = 0xFFFFF8E1,
                accentColorHex = 0xFFFF7052
            )
            input.streakDays >= 1 && input.todayDone -> WidgetContent(
                emoji = "🦊",
                title = "Done, $name!",
                subtitle = "See you tomorrow!",
                bgColorHex = 0xFFFFFDF5,
                accentColorHex = 0xFF5C6FF2
            )
            input.streakDays >= 7 -> WidgetContent(
                emoji = "🏆",
                title = "$name, Day ${input.streakDays}!",
                subtitle = personalSubtitle(input),
                bgColorHex = 0xFFF0FFF4,
                accentColorHex = 0xFF52D68A
            )
            input.streakDays >= 1 -> WidgetContent(
                emoji = "🦊",
                title = "$name, Day ${input.streakDays}",
                subtitle = personalSubtitle(input),
                bgColorHex = 0xFFFFFDF5,
                accentColorHex = 0xFF5C6FF2
            )
            else -> WidgetContent(
                emoji = "🦊",
                title = "Hi $name!",
                subtitle = if (input.todayTaskTheme != null) "Ready for ${input.todayTaskTheme}?" else "Ready to start learning?",
                bgColorHex = 0xFFFFFDF5,
                accentColorHex = 0xFF5C6FF2
            )
        }
    }

    /**
     * Picks a context-aware subtitle: task theme, error review, or generic encouragement.
     */
    private fun personalSubtitle(input: WidgetInput): String {
        return when {
            input.todayTaskTheme != null && input.errorCount > 0 ->
                "Ready for ${input.todayTaskTheme}? ${input.errorCount} to review"
            input.todayTaskTheme != null ->
                "Ready for ${input.todayTaskTheme}?"
            input.errorCount > 0 ->
                "${input.errorCount} words to review!"
            else ->
                "Keep it going!"
        }
    }
}
