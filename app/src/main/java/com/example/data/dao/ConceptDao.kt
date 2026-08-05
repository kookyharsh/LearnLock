package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ConceptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcept(concept: ConceptItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<ConceptItem>)

    @Query("SELECT * FROM concepts WHERE isUsed = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextUnusedConcept(): ConceptItem?

    @Query("SELECT * FROM concepts WHERE isUsed = 0 AND topic IN (:topics) ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextUnusedConceptForTopics(topics: List<String>): ConceptItem?

    @Query("UPDATE concepts SET isUsed = 1 WHERE id = :id")
    suspend fun markConceptUsed(id: Long)

    @Query("UPDATE concepts SET repetitions = :repetitions, easeFactor = :easeFactor, intervalDays = :intervalDays, nextReviewAt = :nextReviewAt, lapses = :lapses, masteryScore = :masteryScore WHERE id = :id")
    suspend fun updateReviewState(
        id: Long,
        repetitions: Int,
        easeFactor: Double,
        intervalDays: Int,
        nextReviewAt: Long?,
        lapses: Int,
        masteryScore: Double
    )

    @Query("SELECT * FROM concepts WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now ORDER BY nextReviewAt ASC LIMIT :limit")
    suspend fun getDueReviews(now: Long, limit: Int): List<ConceptItem>

    @Query("SELECT * FROM concepts WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now AND topic IN (:topics) ORDER BY nextReviewAt ASC LIMIT :limit")
    suspend fun getDueReviewsForTopics(topics: List<String>, now: Long, limit: Int): List<ConceptItem>

    @Query("SELECT COUNT(*) FROM concepts WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now")
    suspend fun getDueReviewsCount(now: Long): Int

    @Query("SELECT * FROM concepts WHERE isUsed = 0 AND topic = :topic AND conceptTitle NOT IN (:recentTitles) ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextUnusedConceptForTopicExcludingRecent(topic: String, recentTitles: List<String>): ConceptItem?

    @Query("SELECT * FROM concepts WHERE isUsed = 0 AND topic = :topic ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextUnusedConceptForTopic(topic: String): ConceptItem?

    @Query("SELECT * FROM concepts WHERE conceptTitle = :title LIMIT 1")
    suspend fun getConceptByTitle(title: String): ConceptItem?

    @Query("UPDATE concepts SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarStatus(id: Long, isStarred: Boolean)

    @Query("UPDATE concepts SET isStarred = :isStarred WHERE conceptTitle = :title")
    suspend fun updateStarStatusByTitle(title: String, isStarred: Boolean)

    @Query("SELECT COUNT(*) FROM concepts WHERE isUsed = 0")
    fun getUnusedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM concepts WHERE isUsed = 0")
    suspend fun getUnusedCountDirect(): Int

    @Query("SELECT * FROM concepts ORDER BY id DESC")
    fun getAllConcepts(): Flow<List<ConceptItem>>

    @Query("SELECT DISTINCT topic FROM concepts")
    suspend fun getDistinctTopics(): List<String>

    @Query("SELECT * FROM concepts WHERE isUsed = 1 AND masteryScore < 0.5 ORDER BY masteryScore ASC LIMIT :limit")
    suspend fun getWeakConcepts(limit: Int): List<ConceptItem>

    @Query("SELECT * FROM concepts WHERE isUsed = 0 AND conceptTitle NOT IN (:recentTitles) ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextUnusedConceptExcludingRecent(recentTitles: List<String>): ConceptItem?

    @Query("SELECT * FROM concepts WHERE isUsed = 0 AND topic IN (:topics) AND conceptTitle NOT IN (:recentTitles) ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextUnusedConceptForTopicsExcludingRecent(topics: List<String>, recentTitles: List<String>): ConceptItem?

    @Query("DELETE FROM concepts WHERE isUsed = 0")
    suspend fun clearUnusedConcepts()

    @Query("DELETE FROM concepts")
    suspend fun clearAllConcepts()
}
