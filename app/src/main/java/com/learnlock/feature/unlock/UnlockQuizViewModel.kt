package com.learnlock.feature.unlock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.learnlock.data.AppDatabase
import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.preferences.AppPreferencesManager
import com.learnlock.data.resolveWeakestTopic
import com.learnlock.domain.scheduler.AdaptiveScheduler
import com.learnlock.data.remote.ai.ConceptGenerator
import com.learnlock.data.remote.ai.ShortAnswerGradingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class LoadedQuiz(
    val concept: ConceptItem?,
    val retryItem: QuestionHistory?,
    val isStarred: Boolean,
)

@HiltViewModel
class UnlockQuizViewModel @Inject constructor(
    private val db: AppDatabase,
    private val prefs: AppPreferencesManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val questionTypes: Set<String> = prefs.getQuestionTypes()
    val questionsPerQuiz: Int = prefs.getQuestionsPerQuiz()

    fun toggleStar(topic: String, title: String, starred: Boolean) {
        viewModelScope.launch {
            db.conceptDao().updateStarStatusForConcept(topic, title, starred)
            db.historyDao().updateStarStatusForConcept(topic, title, starred)
        }
    }

    /**
     * Resolves the concept (or pending retry) to present. When [restoredConceptId]
     * / [restoredRetryId] are set, the same content is reloaded from the database
     * instead of regenerated, so rotation never re-bills the API.
     */
    suspend fun load(
        retryHistoryId: Long?,
        restoredConceptId: Long?,
        restoredRetryId: Long?,
        requestedConceptId: Long? = null,
    ): LoadedQuiz {
        val didFreshLoad = restoredConceptId == null && restoredRetryId == null
        var concept: ConceptItem? = null
        var retryItem: QuestionHistory? = null
        var starred = false
        try {
            if (!didFreshLoad) {
                restoredRetryId?.let { id ->
                    db.historyDao().getHistoryByIdOnce(id)?.let { item ->
                        retryItem = item
                        starred = item.isStarred
                    }
                }
                restoredConceptId?.let { id ->
                    db.conceptDao().getConceptById(id)?.let { c ->
                        concept = c
                        if (retryItem == null) starred = c.isStarred
                    }
                }
            } else if (requestedConceptId != null) {
                concept = db.conceptDao().getConceptById(requestedConceptId)
                concept?.let { starred = it.isStarred }
            } else {
                // A retry opened for one specific history entry wins over the
                // global newest retry, so "Retry Question Now" quizzes that card.
                val requestedRetry: QuestionHistory? = retryHistoryId
                    ?.let { db.historyDao().getHistoryByIdOnce(it) }
                val pickedRetry: QuestionHistory? = requestedRetry
                    ?: db.historyDao().getPendingRetryQuestion(prefs.getQuestionTypes().toList())
                if (pickedRetry != null) {
                    if (pickedRetry.questionType in prefs.getQuestionTypes()) {
                        retryItem = pickedRetry
                        starred = pickedRetry.isStarred
                        concept = db.conceptDao().getConceptByTopicAndTitle(
                            pickedRetry.topic,
                            pickedRetry.conceptTitle,
                        )
                    } else {
                        concept = nextUnusedForSelectedTopics()
                    }
                } else {
                    concept = selectFreshConcept()
                }
                concept?.let { starred = it.isStarred }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (didFreshLoad) {
            UnlockReceiver.pregenerateConceptsIfNeeded(context, prefs)
        }
        return LoadedQuiz(concept, retryItem, starred)
    }

    private suspend fun nextUnusedForSelectedTopics(): ConceptItem? {
        val selectedTopics = prefs.getSelectedTopics().toList()
        return if (selectedTopics.isEmpty()) {
            db.conceptDao().getNextUnusedConcept()
        } else {
            db.conceptDao().getNextUnusedConceptForTopics(selectedTopics)
        }
    }

    private suspend fun selectFreshConcept(): ConceptItem? {
        val selectedTopics = prefs.getSelectedTopics().toList()
        val cooldown24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val recentTitles = db.historyDao().getRecentConceptTitles(cooldown24h)
        val now = System.currentTimeMillis()

        // 1) Spaced repetition: surface due reviews first
        var concept = if (selectedTopics.isEmpty()) {
            db.conceptDao().getDueReviews(now, 1).firstOrNull()
        } else {
            db.conceptDao().getDueReviewsForTopics(selectedTopics, now, 1).firstOrNull()
        }

        // 2) Fresh concept from the weakest topic
        if (concept == null) {
            val weakestTopic = resolveWeakestTopic(db, selectedTopics)
            if (weakestTopic != null) {
                concept = db.conceptDao()
                    .getNextUnusedConceptForTopicExcludingRecent(weakestTopic, recentTitles)
                    ?: db.conceptDao().getNextUnusedConceptForTopic(weakestTopic)
            }
        }

        // 3) Fallback: existing random selection
        if (concept == null) {
            concept = if (selectedTopics.isEmpty()) {
                db.conceptDao().getNextUnusedConceptExcludingRecent(recentTitles)
            } else {
                db.conceptDao().getNextUnusedConceptForTopicsExcludingRecent(selectedTopics, recentTitles)
            } ?: if (selectedTopics.isEmpty()) {
                db.conceptDao().getNextUnusedConcept()
            } else {
                db.conceptDao().getNextUnusedConceptForTopics(selectedTopics)
            }
        }

        // A stored concept whose question count differs from the current
        // preference is still usable as-is: the quiz simply renders however many
        // questions it has. It must not be burned, or the queue silently shrinks.
        if (concept != null) {
            val parsedQuestions = parseQuestionsList(
                questionsJson = concept.questionsJson,
                fallbackQuestionText = concept.questionText,
                fallbackQuestionType = concept.questionType,
                fallbackOptionsJson = concept.optionsJson,
                fallbackCodePrefix = concept.codeSnippetPrefix,
                fallbackCorrectAnswer = concept.correctAnswer,
                fallbackExplanation = concept.explanation,
            )
            if (parsedQuestions.isEmpty()) {
                concept = null
            }
        }

        // If no pre-generated concept matches, generate a fresh AI concept
        if (concept == null && prefs.getApiKey().isNotBlank()) {
            val fresh = ConceptGenerator(prefs).generateBatchConcepts(
                topics = prefs.getSelectedTopics(),
                count = 1,
            )
            if (fresh.isNotEmpty()) {
                val newId = db.conceptDao().insertConcept(fresh.first())
                concept = fresh.first().copy(id = newId)
            }
        }

        // 4) Offline / empty queue fallback: recycle a previously answered concept
        if (concept == null) {
            concept = if (selectedTopics.isEmpty()) {
                db.conceptDao().getRecycledReviewConcept()
            } else {
                db.conceptDao().getRecycledReviewConceptForTopics(selectedTopics)
                    ?: db.conceptDao().getRecycledReviewConcept()
            }
        }
        return concept
    }

    /**
     * Persists the whole quiz attempt atomically and updates spaced-repetition
     * state. Quiz completion never gates device access; objective correctness is
     * kept for history, XP, and retry scheduling.
     */
    suspend fun submit(
        questions: List<QuizQuestion>,
        selectedOptionIndices: Map<Int, Int>,
        multiSelections: Map<Int, String>,
        codeAnswers: Map<Int, String>,
        title: String,
        topic: String,
        summary: String,
        rawQuestionsJson: String?,
        isStarred: Boolean,
        concept: ConceptItem?,
        pendingRetryItem: QuestionHistory?,
    ): QuizResult {
        var correctCount = 0
        var gradableCount = 0
        questions.forEachIndexed { i, q ->
            if (q.questionType != "SHORT_ANSWER" && isQuestionGradable(q)) {
                gradableCount++
                val correct = if (q.questionType == "MULTI_SELECT") {
                    val selected = multiSelections[i]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                    selected.isNotEmpty() && selected == parseIndexAnswer(q.correctAnswer).toSet()
                } else isQuestionCorrect(q, selectedOptionIndices[i], codeAnswers[i])
                if (correct) {
                    correctCount++
                }
            }
        }

        val passed = true
        val objectivePassed = gradableCount > 0 && correctCount == gradableCount

        val userAnswersList = mutableListOf<String>()
        questions.forEachIndexed { i, q ->
            if (q.questionType == "MULTI_SELECT") {
                val selected = multiSelections[i]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                userAnswersList.add(selected.sorted().joinToString(",") { q.optionsList.getOrNull(it).orEmpty() }.ifBlank { "Unanswered" })
            } else if (q.questionType == "DRAG_BLANK") {
                userAnswersList.add(codeAnswers[i]?.split(",")?.mapNotNull { it.toIntOrNull() }?.mapNotNull { q.optionsList.getOrNull(it) }?.joinToString(" → ").orEmpty().ifBlank { "Unanswered" })
            } else if (isTextAnswerQuestion(q)) {
                userAnswersList.add(
                    codeAnswers[i]?.trim().takeIf { !it.isNullOrBlank() }
                        ?: "Unanswered",
                )
            } else {
                val selIdx = selectedOptionIndices[i] ?: -1
                val ans = q.optionsList.getOrNull(selIdx)
                    ?: if (selIdx != -1) selIdx.toString() else "Unanswered"
                userAnswersList.add(ans)
            }
        }

        val formattedUserAnswer = if (userAnswersList.size == 1) {
            userAnswersList.first()
        } else {
            userAnswersList.mapIndexed { idx, a -> "Q${idx + 1}: $a" }
                .joinToString(" | ")
        }

        val firstQ = questions.first()

        val perQuestionResults = JSONArray().apply {
            questions.forEachIndexed { i, q ->
                put(JSONObject().apply {
                    put("idx", i)
                    put("isCorrect", isQuestionCorrect(q, selectedOptionIndices[i], codeAnswers[i]))
                })
            }
        }.toString()
        val answeredDifficulty = concept?.difficulty
            ?: pendingRetryItem?.difficulty
            ?: "Medium"

        // All submit writes are atomic: a crash mid-submit cannot leave history
        // and SRS half-applied.
        var masteryAfter: Double? = null
        var reviewInterval: Int? = null
        var reviewStatus: String? = null
        db.withTransaction {
            // Insert QuestionHistory entry (RETRY_PENDING if failed so user can re-practice in History)
            db.historyDao().insertHistory(
                QuestionHistory(
                    id = 0,
                    conceptTitle = title,
                    topic = topic,
                    questionText = "Quiz session · ${questions.size} questions",
                    userAnswer = formattedUserAnswer,
                    correctAnswer = firstQ.correctAnswer,
                    isCorrect = correctCount == gradableCount && gradableCount > 0,
                    status = "SESSION_SUMMARY",
                    explanation = firstQ.explanation,
                    optionsJson = JSONArray(firstQ.optionsList).toString(),
                    questionType = "SESSION_SUMMARY",
                    codeSnippetPrefix = firstQ.codeSnippetPrefix,
                    questionsJson = rawQuestionsJson,
                    conceptSummary = summary,
                    isStarred = isStarred,
                    answeredAt = System.currentTimeMillis(),
                    perQuestionResultsJson = perQuestionResults,
                    difficulty = answeredDifficulty,
                ),
            )

            questions.forEachIndexed { i, q ->
                val objectiveCorrect = when (q.questionType) {
                    "SHORT_ANSWER" -> false // evaluated after completion
                    "MULTI_SELECT" -> {
                        val selected = multiSelections[i]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                        selected.isNotEmpty() && selected == parseIndexAnswer(q.correctAnswer)
                    }
                    "DRAG_BLANK" -> codeAnswers[i]?.split(",")?.mapNotNull { it.toIntOrNull() }?.let { it.isNotEmpty() && it == parseIndexAnswer(q.correctAnswer) } ?: false
                    else -> isQuestionCorrect(q, selectedOptionIndices[i], codeAnswers[i])
                }
                val answerText = userAnswersList.getOrElse(i) { "Unanswered" }
                db.historyDao().insertHistory(
                    QuestionHistory(
                        conceptTitle = title,
                        topic = topic,
                        questionText = q.questionText,
                        userAnswer = answerText,
                        correctAnswer = q.correctAnswer,
                        isCorrect = objectiveCorrect,
                        status = when {
                            q.questionType == "SHORT_ANSWER" -> "GRADING_PENDING"
                            objectiveCorrect -> "PASSED"
                            else -> "RETRY_PENDING"
                        },
                        explanation = q.explanation,
                        optionsJson = JSONArray(q.optionsList).toString(),
                        questionType = q.questionType,
                        codeSnippetPrefix = q.codeSnippetPrefix,
                        questionsJson = JSONArray().put(JSONObject().apply {
                            put("questionType", q.questionType)
                            put("questionText", q.questionText)
                            put("options", JSONArray(q.optionsList))
                            put("correctAnswer", q.correctAnswer)
                            put("explanation", q.explanation)
                        }).toString(),
                        conceptSummary = summary,
                        isStarred = isStarred,
                        answeredAt = System.currentTimeMillis(),
                        difficulty = answeredDifficulty,
                    ),
                )
            }

            val currentItem = pendingRetryItem
            if (currentItem != null) {
                val retriedCorrectly = questions.firstOrNull()?.let { q ->
                    when (q.questionType) {
                        "MULTI_SELECT" -> {
                            val selected = multiSelections[0]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                            selected.isNotEmpty() && selected == parseIndexAnswer(q.correctAnswer)
                        }
                        "SHORT_ANSWER" -> false
                        else -> isQuestionCorrect(q, selectedOptionIndices[0], codeAnswers[0])
                    }
                } ?: false
                if (retriedCorrectly) {
                    db.historyDao().updateAttemptResult(currentItem.id, "PASSED", true)
                } else {
                    db.historyDao().updateAttemptResult(currentItem.id, "RETRY_RECORDED", false)
                }
            }

            if (concept != null) {
                db.conceptDao().markConceptUsed(concept.id)
                // Spaced-repetition scheduling + mastery update
                val answeredAt = System.currentTimeMillis()
                val srs = AdaptiveScheduler.scheduleAnswer(
                    repetitions = concept.repetitions,
                    easeFactor = concept.easeFactor,
                    intervalDays = concept.intervalDays,
                    nextReviewAt = concept.nextReviewAt,
                    lapses = concept.lapses,
                    passed = objectivePassed,
                    now = answeredAt,
                )
                val recentCorrect = db.historyDao()
                    .getRecentCorrectnessForConcept(topic, title, 10)
                val recentTimes = db.historyDao()
                    .getRecentTimestampsForConcept(topic, title, 10)
                val mastery = AdaptiveScheduler.computeMastery(recentCorrect, recentTimes, answeredAt)
                db.conceptDao().updateReviewState(
                    id = concept.id,
                    repetitions = srs.repetitions,
                    easeFactor = srs.easeFactor,
                    intervalDays = srs.intervalDays,
                    nextReviewAt = srs.nextReviewAt,
                    lapses = srs.lapses,
                    masteryScore = mastery,
                )
                masteryAfter = mastery
                reviewInterval = srs.intervalDays
                reviewStatus = AdaptiveScheduler.statusOf(srs.repetitions, srs.intervalDays, srs.nextReviewAt)
            }
        }

        if (questions.any { it.questionType == "SHORT_ANSWER" }) {
            ShortAnswerGradingWorker.enqueue(context)
        }

        return QuizResult(
            passed = passed,
            correctCount = correctCount,
            total = questions.size,
            masteryBefore = concept?.masteryScore,
            masteryAfter = masteryAfter,
            nextReviewDays = reviewInterval,
            srsStatus = reviewStatus,
            pendingCount = questions.count { it.questionType == "SHORT_ANSWER" },
        )
    }
}
