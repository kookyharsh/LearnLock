package com.learnlock.data.repository

import com.learnlock.data.dao.HistoryDao
import com.learnlock.data.entity.QuestionHistory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for quiz attempts. Screens and background components
 * depend on this instead of reaching into [HistoryDao] directly.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
) {
    suspend fun insertHistory(history: QuestionHistory): Long = historyDao.insertHistory(history)

    suspend fun updateHistory(history: QuestionHistory) = historyDao.updateHistory(history)

    suspend fun updateAttemptResult(id: Long, status: String, isCorrect: Boolean) =
        historyDao.updateAttemptResult(id, status, isCorrect)

    suspend fun finishShortAnswerGrading(id: Long, isCorrect: Boolean) =
        historyDao.finishShortAnswerGrading(id, isCorrect)

    suspend fun getHistoryByIdOnce(id: Long): QuestionHistory? = historyDao.getHistoryByIdOnce(id)

    fun getHistoryById(id: Long): Flow<QuestionHistory?> = historyDao.getHistoryById(id)

    fun getAllHistory(): Flow<List<QuestionHistory>> = historyDao.getAllHistory()

    suspend fun getAllHistoryList(): List<QuestionHistory> = historyDao.getAllHistoryList()

    fun getRecentlyViewedHistory(limit: Int = 10): Flow<List<QuestionHistory>> =
        historyDao.getRecentlyViewedHistory(limit)

    fun getStarredHistory(): Flow<List<QuestionHistory>> = historyDao.getStarredHistory()

    fun getHistoryByTopic(topic: String): Flow<List<QuestionHistory>> =
        historyDao.getHistoryByTopic(topic)

    fun getHistoryByStatus(status: String): Flow<List<QuestionHistory>> =
        historyDao.getHistoryByStatus(status)

    suspend fun getPendingRetryQuestion(questionTypes: List<String>): QuestionHistory? =
        historyDao.getPendingRetryQuestion(questionTypes)

    suspend fun getPendingShortAnswerAttempts(): List<QuestionHistory> =
        historyDao.getPendingShortAnswerAttempts()

    suspend fun getPendingRetryCountForTopic(topic: String): Int =
        historyDao.getPendingRetryCountForTopic(topic)

    suspend fun getRecentConceptTitles(sinceTime: Long): List<String> =
        historyDao.getRecentConceptTitles(sinceTime)

    suspend fun getRecentCorrectnessForConcept(topic: String, conceptTitle: String, limit: Int): List<Boolean> =
        historyDao.getRecentCorrectnessForConcept(topic, conceptTitle, limit)

    suspend fun getRecentTimestampsForConcept(topic: String, conceptTitle: String, limit: Int): List<Long> =
        historyDao.getRecentTimestampsForConcept(topic, conceptTitle, limit)

    suspend fun updateStarStatus(id: Long, isStarred: Boolean) =
        historyDao.updateStarStatus(id, isStarred)

    suspend fun updateStarStatusByTitle(title: String, isStarred: Boolean) =
        historyDao.updateStarStatusByTitle(title, isStarred)

    suspend fun updateStarStatusForConcept(topic: String, title: String, isStarred: Boolean) =
        historyDao.updateStarStatusForConcept(topic, title, isStarred)

    suspend fun getHistoryCount(): Int = historyDao.getHistoryCount()

    suspend fun deleteHistory(history: QuestionHistory) = historyDao.deleteHistory(history)

    suspend fun clearAllHistory() = historyDao.clearAllHistory()
}
