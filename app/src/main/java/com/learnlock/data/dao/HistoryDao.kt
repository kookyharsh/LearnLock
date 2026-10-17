package com.learnlock.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.learnlock.data.entity.QuestionHistory
import kotlinx.coroutines.flow.Flow

data class TopicAccuracy(
    val topic: String,
    val correct: Int,
    val total: Int
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: QuestionHistory): Long

    @Update
    suspend fun updateHistory(history: QuestionHistory)

    @Query("UPDATE question_history SET status = :status, isCorrect = :isCorrect WHERE id = :id")
    suspend fun updateAttemptResult(id: Long, status: String, isCorrect: Boolean)

    @Query("UPDATE question_history SET status = CASE WHEN :isCorrect = 1 THEN 'PASSED' ELSE 'RETRY_PENDING' END, isCorrect = :isCorrect WHERE id = :id AND status = 'GRADING_PENDING'")
    suspend fun finishShortAnswerGrading(id: Long, isCorrect: Boolean)

    @Query("SELECT * FROM question_history WHERE status = 'RETRY_PENDING' AND questionType IN (:questionTypes) ORDER BY answeredAt ASC LIMIT 1")
    suspend fun getPendingRetryQuestion(questionTypes: List<String>): QuestionHistory?

    @Query("SELECT * FROM question_history WHERE status = 'GRADING_PENDING' AND questionType = 'SHORT_ANSWER' ORDER BY answeredAt ASC")
    suspend fun getPendingShortAnswerAttempts(): List<QuestionHistory>

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC")
    fun getAllHistory(): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE questionType = 'SESSION_SUMMARY' ORDER BY answeredAt DESC")
    fun getAllSessionSummaries(): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE questionType != 'SESSION_SUMMARY' AND topic = :topic AND conceptTitle = :conceptTitle AND ABS(answeredAt - :answeredAt) <= 10000 ORDER BY id ASC")
    suspend fun getQuestionsForAttempt(topic: String, conceptTitle: String, answeredAt: Long): List<QuestionHistory>

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC LIMIT :limit")
    fun getRecentlyViewedHistory(limit: Int = 10): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE id = :id")
    fun getHistoryById(id: Long): Flow<QuestionHistory?>

    @Query("SELECT * FROM question_history WHERE id = :id")
    suspend fun getHistoryByIdOnce(id: Long): QuestionHistory?

    @Query("SELECT * FROM question_history WHERE isStarred = 1 ORDER BY answeredAt DESC")
    fun getStarredHistory(): Flow<List<QuestionHistory>>

    @Query("UPDATE question_history SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarStatus(id: Long, isStarred: Boolean)

    @Query("UPDATE question_history SET isStarred = :isStarred WHERE conceptTitle = :title")
    suspend fun updateStarStatusByTitle(title: String, isStarred: Boolean)

    @Query("UPDATE question_history SET isStarred = :isStarred WHERE topic = :topic AND conceptTitle = :title")
    suspend fun updateStarStatusForConcept(topic: String, title: String, isStarred: Boolean)

    @Query("SELECT * FROM question_history WHERE topic = :topic ORDER BY answeredAt DESC")
    fun getHistoryByTopic(topic: String): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE status = :status ORDER BY answeredAt DESC")
    fun getHistoryByStatus(status: String): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC")
    suspend fun getAllHistoryList(): List<QuestionHistory>

    @Query("SELECT DISTINCT conceptTitle FROM question_history WHERE answeredAt > :sinceTime")
    suspend fun getRecentConceptTitles(sinceTime: Long): List<String>

    @Query("SELECT topic, SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) AS correct, COUNT(*) AS total FROM question_history WHERE questionType != 'SESSION_SUMMARY' AND status != 'GRADING_PENDING' GROUP BY topic")
    suspend fun getTopicAccuracy(): List<TopicAccuracy>

    @Query("SELECT isCorrect FROM question_history WHERE topic = :topic AND conceptTitle = :conceptTitle AND questionType != 'SESSION_SUMMARY' AND status != 'GRADING_PENDING' ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun getRecentCorrectnessForConcept(topic: String, conceptTitle: String, limit: Int): List<Boolean>

    @Query("SELECT answeredAt FROM question_history WHERE topic = :topic AND conceptTitle = :conceptTitle AND questionType != 'SESSION_SUMMARY' AND status != 'GRADING_PENDING' ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun getRecentTimestampsForConcept(topic: String, conceptTitle: String, limit: Int): List<Long>

    @Query("SELECT COUNT(*) FROM question_history WHERE status = 'RETRY_PENDING' AND topic = :topic")
    suspend fun getPendingRetryCountForTopic(topic: String): Int

    @Delete
    suspend fun deleteHistory(history: QuestionHistory)

    @Query("DELETE FROM question_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM question_history")
    suspend fun getHistoryCount(): Int
}
