package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.LearningRecordDao
import org.akj.lingo.learn.data.local.entity.LearningRecordEntity
import org.akj.lingo.learn.domain.model.LearningRecord
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningRecordRepositoryImpl @Inject constructor(
    private val learningRecordDao: LearningRecordDao
) : LearningRecordRepository {

    override suspend fun saveSessionRecord(record: LearningRecord) {
        learningRecordDao.insertRecord(LearningRecordEntity.fromDomain(record))
    }

    override suspend fun getRecordsSince(timestamp: Long): List<LearningRecord> {
        return learningRecordDao.getRecordsSince(timestamp).map { it.toDomain() }
    }

    override suspend fun getWeeklyRecords(): List<LearningRecord> {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        return learningRecordDao.getRecordsSince(sevenDaysAgo)
            .filter { it.taskType == "DAILY_PRACTICE" }
            .map { it.toDomain() }
    }

    override suspend fun getMonthlyAccuracy(): Float {
        val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 3600 * 1000L
        val records = learningRecordDao.getRecordsSince(thirtyDaysAgo).filter { it.taskType == "DAILY_PRACTICE" }
        if (records.isEmpty()) return 0.0f
        return records.map { it.accuracy }.average().toFloat()
    }

    override suspend fun getWeakCategories(): List<String> {
        val records = getWeeklyRecords()
        if (records.isEmpty()) return listOf("Vocabulary", "Pronunciation")
        
        val categoryCounts = mutableMapOf<String, Int>()
        records.forEach { record ->
            if (record.accuracy < 0.7f) {
                val cat = record.taskType.ifBlank { "Vocabulary" }
                categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
            }
        }
        return if (categoryCounts.isEmpty()) listOf("Vocabulary", "Pronunciation") else categoryCounts.keys.toList()
    }

    override suspend fun getStreakDays(): Int {
        val allRecords = learningRecordDao.getAllRecords().map { it.toDomain() }
        if (allRecords.isEmpty()) return 0

        var streak = 0
        val calendar = Calendar.getInstance()

        // Group records by day timestamp (normalized to midnight)
        val activeDays = allRecords
            .map { record ->
                calendar.timeInMillis = record.timestamp
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            .distinct()
            .sortedDescending()

        val todayMidnight = getStartOfDayTimestamp()
        val yesterdayMidnight = todayMidnight - 24 * 3600 * 1000L

        var expectedDay = if (activeDays.contains(todayMidnight)) todayMidnight else yesterdayMidnight

        for (day in activeDays) {
            if (day == expectedDay) {
                streak++
                expectedDay -= 24 * 3600 * 1000L
            } else if (day < expectedDay) {
                break
            }
        }

        return streak
    }

    override suspend fun getTodayProgress(): Float {
        val todayRecord = learningRecordDao.getTodayRecord(getStartOfDayTimestamp())
        return if (todayRecord != null) 1.0f else 0.0f
    }

    private fun getStartOfDayTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
