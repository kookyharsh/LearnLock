package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.QuestionHistory
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

    @Query("UPDATE question_history SET status = 'PASSED' WHERE conceptTitle = :conceptTitle AND status = 'RETRY_PENDING'")
    suspend fun markConceptPassed(conceptTitle: String)

    @Query("SELECT * FROM question_history WHERE status = 'RETRY_PENDING' ORDER BY answeredAt DESC LIMIT 1")
    suspend fun getPendingRetryQuestion(): QuestionHistory?

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC")
    fun getAllHistory(): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC LIMIT :limit")
    fun getRecentlyViewedHistory(limit: Int = 10): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE isStarred = 1 ORDER BY answeredAt DESC")
    fun getStarredHistory(): Flow<List<QuestionHistory>>

    @Query("UPDATE question_history SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarStatus(id: Long, isStarred: Boolean)

    @Query("UPDATE question_history SET isStarred = :isStarred WHERE conceptTitle = :title")
    suspend fun updateStarStatusByTitle(title: String, isStarred: Boolean)

    @Query("SELECT * FROM question_history WHERE topic = :topic ORDER BY answeredAt DESC")
    fun getHistoryByTopic(topic: String): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE status = :status ORDER BY answeredAt DESC")
    fun getHistoryByStatus(status: String): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC")
    suspend fun getAllHistoryList(): List<QuestionHistory>

    @Query("SELECT DISTINCT conceptTitle FROM question_history WHERE answeredAt > :sinceTime")
    suspend fun getRecentConceptTitles(sinceTime: Long): List<String>

    @Query("SELECT topic, SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) AS correct, COUNT(*) AS total FROM question_history GROUP BY topic")
    suspend fun getTopicAccuracy(): List<TopicAccuracy>

    @Query("SELECT isCorrect FROM question_history WHERE conceptTitle = :conceptTitle ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun getRecentCorrectnessForConcept(conceptTitle: String, limit: Int): List<Boolean>

    @Query("SELECT answeredAt FROM question_history WHERE conceptTitle = :conceptTitle ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun getRecentTimestampsForConcept(conceptTitle: String, limit: Int): List<Long>

    @Query("SELECT COUNT(*) FROM question_history WHERE status = 'RETRY_PENDING' AND topic = :topic")
    suspend fun getPendingRetryCountForTopic(topic: String): Int

    @Delete
    suspend fun deleteHistory(history: QuestionHistory)

    @Query("DELETE FROM question_history")
    suspend fun clearAllHistory()
}
