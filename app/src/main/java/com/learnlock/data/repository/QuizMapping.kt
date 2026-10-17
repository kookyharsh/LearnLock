package com.learnlock.data.repository

import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.model.QuizSummary
import org.json.JSONArray

object QuizMapping {

    fun fromConcept(concept: ConceptItem): QuizSummary {
        val qCount = parseQuestionCount(concept.questionsJson)
        return QuizSummary(
            id = concept.id,
            title = concept.conceptTitle,
            topic = concept.topic,
            questionCount = qCount,
            isAttempted = false,
            score = null,
            maxScore = null,
            date = concept.createdAt,
            difficulty = concept.difficulty,
            questionsJson = concept.questionsJson,
            conceptSummary = concept.conceptSummary,
            conceptId = concept.id,
            historySessionId = null,
            xpEarned = null,
            isStarred = concept.isStarred,
        )
    }

    fun fromSessionSummary(session: QuestionHistory): QuizSummary {
        val (score, total) = parseScoreAndTotal(session)
        val xp = score * 10
        return QuizSummary(
            id = session.id,
            title = session.conceptTitle,
            topic = session.topic,
            questionCount = total,
            isAttempted = true,
            score = score,
            maxScore = total,
            date = session.answeredAt,
            difficulty = session.difficulty,
            questionsJson = session.questionsJson,
            conceptSummary = session.conceptSummary,
            conceptId = null,
            historySessionId = session.id,
            xpEarned = xp,
            isStarred = session.isStarred,
        )
    }

    private fun parseQuestionCount(questionsJson: String?): Int {
        if (questionsJson.isNullOrBlank()) return 1
        return try {
            val arr = JSONArray(questionsJson)
            if (arr.length() > 0) arr.length() else 1
        } catch (_: Exception) {
            1
        }
    }

    private fun parseScoreAndTotal(session: QuestionHistory): Pair<Int, Int> {
        val perQ = session.perQuestionResultsJson
        if (!perQ.isNullOrBlank()) {
            try {
                val arr = JSONArray(perQ)
                var correct = 0
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optBoolean("isCorrect", false)) {
                        correct++
                    }
                }
                val total = if (arr.length() > 0) arr.length() else parseQuestionCount(session.questionsJson)
                return correct to total
            } catch (_: Exception) {}
        }

        // Fallback: if answers are joined with '|'
        val answers = session.userAnswer.split(" | ")
        val total = answers.size.coerceAtLeast(parseQuestionCount(session.questionsJson))
        val correct = if (session.isCorrect) total else 0
        return correct to total
    }
}

