package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concepts")
data class ConceptItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String, // e.g., "React", "Next.js", "Python", "Java", "SQL", "DSA", "Networking", "MongoDB"
    val conceptTitle: String,
    val conceptSummary: String,
    val codeExample: String? = null,
    val questionType: String, // "MCQ" or "CODE"
    val questionText: String,
    val optionsJson: String? = null, // JSON list of strings for MCQ
    val codeSnippetPrefix: String? = null,
    val correctAnswer: String, // Index or exact string solution
    val explanation: String, // Pre-generated explanation
    val isUsed: Boolean = false,
    val questionsJson: String? = null,
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
