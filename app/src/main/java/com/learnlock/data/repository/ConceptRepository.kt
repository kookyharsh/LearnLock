package com.learnlock.data.repository

import com.learnlock.data.dao.ConceptDao
import com.learnlock.data.entity.ConceptItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for stored concepts. Screens and background components
 * depend on this instead of reaching into [ConceptDao] directly.
 */
@Singleton
class ConceptRepository @Inject constructor(
    private val conceptDao: ConceptDao,
) {
    suspend fun insertConcept(concept: ConceptItem): Long = conceptDao.insertConcept(concept)

    suspend fun insertConcepts(concepts: List<ConceptItem>) = conceptDao.insertConcepts(concepts)

    suspend fun getConceptById(id: Long): ConceptItem? = conceptDao.getConceptById(id)

    suspend fun getConceptByTitle(title: String): ConceptItem? = conceptDao.getConceptByTitle(title)

    suspend fun getConceptByTopicAndTitle(topic: String, title: String): ConceptItem? =
        conceptDao.getConceptByTopicAndTitle(topic, title)

    suspend fun getDistinctTopics(): List<String> = conceptDao.getDistinctTopics()

    suspend fun getConceptCount(): Int = conceptDao.getConceptCount()

    suspend fun getUnusedCountDirect(): Int = conceptDao.getUnusedCountDirect()

    fun getUnusedCount(): Flow<Int> = conceptDao.getUnusedCount()

    fun getUnusedConcepts(): Flow<List<ConceptItem>> = conceptDao.getUnusedConcepts()

    fun getAllConcepts(): Flow<List<ConceptItem>> = conceptDao.getAllConcepts()

    suspend fun updateReviewState(
        id: Long,
        repetitions: Int,
        easeFactor: Double,
        intervalDays: Int,
        nextReviewAt: Long?,
        lapses: Int,
        masteryScore: Double,
    ) = conceptDao.updateReviewState(
        id,
        repetitions,
        easeFactor,
        intervalDays,
        nextReviewAt,
        lapses,
        masteryScore,
    )

    suspend fun markConceptUsed(id: Long) = conceptDao.markConceptUsed(id)

    suspend fun updateStarStatus(id: Long, isStarred: Boolean) =
        conceptDao.updateStarStatus(id, isStarred)

    suspend fun updateStarStatusByTitle(title: String, isStarred: Boolean) =
        conceptDao.updateStarStatusByTitle(title, isStarred)

    suspend fun updateStarStatusForConcept(topic: String, title: String, isStarred: Boolean) =
        conceptDao.updateStarStatusForConcept(topic, title, isStarred)

    suspend fun getWeakConcepts(limit: Int): List<ConceptItem> = conceptDao.getWeakConcepts(limit)

    suspend fun getDueReviews(now: Long, limit: Int): List<ConceptItem> =
        conceptDao.getDueReviews(now, limit)

    suspend fun getDueReviewsForTopics(topics: List<String>, now: Long, limit: Int): List<ConceptItem> =
        conceptDao.getDueReviewsForTopics(topics, now, limit)

    suspend fun getDueReviewsCount(now: Long): Int = conceptDao.getDueReviewsCount(now)

    suspend fun getNextUnusedConcept(): ConceptItem? = conceptDao.getNextUnusedConcept()

    suspend fun getNextUnusedConceptForTopics(topics: List<String>): ConceptItem? =
        conceptDao.getNextUnusedConceptForTopics(topics)

    suspend fun getNextUnusedConceptForTopic(topic: String): ConceptItem? =
        conceptDao.getNextUnusedConceptForTopic(topic)

    suspend fun getNextUnusedConceptExcludingRecent(recentTitles: List<String>): ConceptItem? =
        conceptDao.getNextUnusedConceptExcludingRecent(recentTitles)

    suspend fun getNextUnusedConceptForTopicsExcludingRecent(
        topics: List<String>,
        recentTitles: List<String>,
    ): ConceptItem? = conceptDao.getNextUnusedConceptForTopicsExcludingRecent(topics, recentTitles)

    suspend fun getRecycledReviewConcept(): ConceptItem? = conceptDao.getRecycledReviewConcept()

    suspend fun getRecycledReviewConceptForTopics(topics: List<String>): ConceptItem? =
        conceptDao.getRecycledReviewConceptForTopics(topics)

    suspend fun clearUnusedConcepts() = conceptDao.clearUnusedConcepts()

    suspend fun clearAllConcepts() = conceptDao.clearAllConcepts()
}
