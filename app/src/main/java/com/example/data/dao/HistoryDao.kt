package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.QuestionHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: QuestionHistory): Long

    @Update
    suspend fun updateHistory(history: QuestionHistory)

    @Query("SELECT * FROM question_history WHERE status = 'RETRY_PENDING' ORDER BY answeredAt DESC LIMIT 1")
    suspend fun getPendingRetryQuestion(): QuestionHistory?

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC")
    fun getAllHistory(): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE topic = :topic ORDER BY answeredAt DESC")
    fun getHistoryByTopic(topic: String): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history WHERE status = :status ORDER BY answeredAt DESC")
    fun getHistoryByStatus(status: String): Flow<List<QuestionHistory>>

    @Query("SELECT * FROM question_history ORDER BY answeredAt DESC")
    suspend fun getAllHistoryList(): List<QuestionHistory>

    @Delete
    suspend fun deleteHistory(history: QuestionHistory)

    @Query("DELETE FROM question_history")
    suspend fun clearAllHistory()
}
