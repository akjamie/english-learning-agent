package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.WidgetContent

/**
 * Sprint 14: Abstraction for persisting pre-generated [WidgetContent] so the
 * Glance widget (no DI) and DashboardViewModel (Hilt) can share content
 * without a direct module dependency.
 */
interface WidgetContentRepository {
    fun save(content: WidgetContent)
    fun get(): WidgetContent?
}
