package com.learnlock.data.repository

import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizMappingTest {

    @Test
    fun fromConcept_createsUnattemptedQuizSummary() {
        val concept = ConceptItem(
            id = 42L,
            topic = "Operating Systems",
            conceptTitle = "Process Synchronization",
            conceptSummary = "Semaphores and mutexes",
            questionType = "MCQ",
            questionText = "What is a semaphore?",
            optionsJson = "[\"Sync primitive\", \"A hardware cable\"]",
            correctAnswer = "Sync primitive",
            explanation = "Used for mutual exclusion",
            isUsed = false,
            createdAt = 1700000000000L,
        )

        val summary = QuizMapping.fromConcept(concept)

        assertEquals(42L, summary.id)
        assertEquals("Process Synchronization", summary.title)
        assertEquals("Operating Systems", summary.topic)
        assertEquals(1, summary.questionCount)
        assertFalse(summary.isAttempted)
        assertEquals(null, summary.score)
        assertEquals(1700000000000L, summary.date)
    }

    @Test
    fun fromSessionSummary_createsAttemptedQuizSummary() {
        val session = QuestionHistory(
            id = 100L,
            conceptTitle = "Operating Systems Quiz",
            topic = "Operating Systems",
            questionText = "What is a semaphore?",
            userAnswer = "Sync primitive",
            correctAnswer = "Sync primitive",
            isCorrect = true,
            status = "PASSED",
            explanation = "Used for mutual exclusion",
            answeredAt = 1700005000000L,
        )

        val summary = QuizMapping.fromSessionSummary(session)

        assertEquals(100L, summary.id)
        assertEquals("Operating Systems Quiz", summary.title)
        assertEquals("Operating Systems", summary.topic)
        assertEquals(1, summary.questionCount)
        assertTrue(summary.isAttempted)
        assertEquals(1, summary.score)
        assertEquals(10, summary.xpEarned)
        assertEquals(1700005000000L, summary.date)
    }
}
