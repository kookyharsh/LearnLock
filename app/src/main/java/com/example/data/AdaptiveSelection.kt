package com.example.data

import com.example.data.dao.TopicAccuracy

/**
 * Picks the topic with the lowest recency-weighted accuracy so fresh concepts
 * are drawn from the user's weakest areas. Topics without history are treated
 * as neutral (0.5).
 */
suspend fun resolveWeakestTopic(
    db: AppDatabase,
    selectedTopics: List<String>
): String? {
    val accuracyByTopic: Map<String, TopicAccuracy> = db.historyDao()
        .getTopicAccuracy()
        .associateBy { it.topic }

    val candidateTopics = if (selectedTopics.isNotEmpty()) {
        selectedTopics
    } else {
        db.conceptDao().getDistinctTopics()
    }
    if (candidateTopics.isEmpty()) return null

    return candidateTopics.minByOrNull { topic ->
        val accuracy = accuracyByTopic[topic]
        if (accuracy == null || accuracy.total == 0) 0.5
        else accuracy.correct.toDouble() / accuracy.total
    }
}
