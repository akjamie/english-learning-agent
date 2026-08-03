package org.akj.lingo.learn.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.data.prefs.WidgetContentCache
import org.akj.lingo.learn.domain.model.WidgetContent
import org.akj.lingo.learn.domain.repository.WidgetContentRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sprint 14: Delegates to [WidgetContentCache] (plain SharedPreferences) so
 * the DashboardViewModel can write widget content via Hilt injection while
 * the Glance widget reads directly from [WidgetContentCache].
 */
@Singleton
class WidgetContentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetContentRepository {

    override fun save(content: WidgetContent) {
        WidgetContentCache.save(context, content)
    }

    override fun get(): WidgetContent? {
        return WidgetContentCache.get(context)
    }
}
