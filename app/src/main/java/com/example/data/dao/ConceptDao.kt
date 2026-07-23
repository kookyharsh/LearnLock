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

    @Query("SELECT * FROM concepts WHERE isUsed = 0 ORDER BY id ASC LIMIT 1")
    suspend fun getNextUnusedConcept(): ConceptItem?

    @Query("SELECT * FROM concepts WHERE isUsed = 0 AND topic IN (:topics) ORDER BY id ASC LIMIT 1")
    suspend fun getNextUnusedConceptForTopics(topics: List<String>): ConceptItem?

    @Query("UPDATE concepts SET isUsed = 1 WHERE id = :id")
    suspend fun markConceptUsed(id: Long)

    @Query("SELECT COUNT(*) FROM concepts WHERE isUsed = 0")
    fun getUnusedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM concepts WHERE isUsed = 0")
    suspend fun getUnusedCountDirect(): Int

    @Query("SELECT * FROM concepts ORDER BY id DESC")
    fun getAllConcepts(): Flow<List<ConceptItem>>

    @Query("DELETE FROM concepts")
    suspend fun clearAllConcepts()
}
