package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_history")
data class QuestionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conceptTitle: String,
    val topic: String,
    val questionText: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val status: String, // "PASSED" or "RETRY_PENDING"
    val explanation: String,
    val optionsJson: String? = null,
    val questionType: String = "MCQ",
    val codeSnippetPrefix: String? = null,
    val questionsJson: String? = null,
    val conceptSummary: String? = null,
    val isStarred: Boolean = false,
    val answeredAt: Long = System.currentTimeMillis()
)
