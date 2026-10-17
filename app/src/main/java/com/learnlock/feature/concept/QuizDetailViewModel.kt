package com.learnlock.feature.concept

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.model.QuizSummary
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.data.repository.QuizMapping
import com.learnlock.feature.unlock.QuizQuestion
import com.learnlock.feature.unlock.parseQuestionsList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class QuestionReviewItem(
    val index: Int,
    val questionText: String,
    val questionType: String,
    val optionsList: List<String>,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val isCorrect: Boolean,
    val isPendingEvaluation: Boolean,
    val codeSnippetPrefix: String? = null,
)

data class QuizDetailUiState(
    val loaded: Boolean = false,
    val quizSummary: QuizSummary? = null,
    val conceptItem: ConceptItem? = null,
    val sessionHistory: QuestionHistory? = null,
    val questionReviews: List<QuestionReviewItem> = emptyList(),
    val totalCorrect: Int = 0,
    val totalIncorrect: Int = 0,
    val totalPending: Int = 0,
    val earnedXp: Int = 0,
)

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val conceptRepository: ConceptRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quizId: Long = savedStateHandle.get<Long>("quizId")
        ?: savedStateHandle.get<Long>("historyId")
        ?: -1L

    private val isAttemptedParam: Boolean = savedStateHandle.get<Boolean>("isAttempted")
        ?: (savedStateHandle.get<String>("isAttempted")?.toBooleanStrictOrNull() ?: true)

    private val _uiState = MutableStateFlow(QuizDetailUiState())
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            if (isAttemptedParam) {
                // Attempted quiz: load session summary
                val session = historyRepository.getHistoryByIdOnce(quizId)
                if (session != null) {
                    val summary = QuizMapping.fromSessionSummary(session)
                    val questionsList = parseQuestionsList(
                        questionsJson = session.questionsJson,
                        fallbackQuestionText = session.questionText,
                        fallbackQuestionType = session.questionType,
                        fallbackOptionsJson = session.optionsJson,
                        fallbackCodePrefix = session.codeSnippetPrefix,
                        fallbackCorrectAnswer = session.correctAnswer,
                        fallbackExplanation = session.explanation,
                    )

                    // Also retrieve recorded question attempts for this session
                    val questionAttempts = historyRepository.getQuestionsForAttempt(
                        session.topic,
                        session.conceptTitle,
                        session.answeredAt,
                    )

                    val answersList = session.userAnswer.split(" | ")
                    val reviews = mutableListOf<QuestionReviewItem>()
                    var correctCount = 0
                    var incorrectCount = 0
                    var pendingCount = 0

                    questionsList.forEachIndexed { i, q ->
                        val matchingAttempt = questionAttempts.getOrNull(i)
                        val uAns = matchingAttempt?.userAnswer
                            ?: answersList.getOrNull(i)?.removePrefix("Q${i + 1}: ")
                            ?: "No answer"

                        val isPending = q.questionType == "SHORT_ANSWER" &&
                            (matchingAttempt?.status == "GRADING_PENDING" || matchingAttempt == null)

                        val isCorr = if (isPending) false else (matchingAttempt?.isCorrect ?: false)

                        if (isPending) pendingCount++
                        else if (isCorr) correctCount++
                        else incorrectCount++

                        reviews.add(
                            QuestionReviewItem(
                                index = i + 1,
                                questionText = q.questionText,
                                questionType = q.questionType,
                                optionsList = q.optionsList,
                                userAnswer = uAns,
                                correctAnswer = q.correctAnswer,
                                explanation = q.explanation,
                                isCorrect = isCorr,
                                isPendingEvaluation = isPending,
                                codeSnippetPrefix = q.codeSnippetPrefix,
                            )
                        )
                    }

                    _uiState.value = QuizDetailUiState(
                        loaded = true,
                        quizSummary = summary,
                        sessionHistory = session,
                        questionReviews = reviews,
                        totalCorrect = correctCount,
                        totalIncorrect = incorrectCount,
                        totalPending = pendingCount,
                        earnedXp = correctCount * 10,
                    )
                } else {
                    _uiState.value = QuizDetailUiState(loaded = true)
                }
            } else {
                // Unattempted quiz: load concept item
                val concept = conceptRepository.getConceptById(quizId)
                if (concept != null) {
                    val summary = QuizMapping.fromConcept(concept)
                    val questionsList = parseQuestionsList(
                        questionsJson = concept.questionsJson,
                        fallbackQuestionText = concept.questionText,
                        fallbackQuestionType = concept.questionType,
                        fallbackOptionsJson = concept.optionsJson,
                        fallbackCodePrefix = concept.codeSnippetPrefix,
                        fallbackCorrectAnswer = concept.correctAnswer,
                        fallbackExplanation = concept.explanation,
                    )

                    val reviews = questionsList.mapIndexed { i, q ->
                        QuestionReviewItem(
                            index = i + 1,
                            questionText = q.questionText,
                            questionType = q.questionType,
                            optionsList = q.optionsList,
                            userAnswer = "",
                            correctAnswer = q.correctAnswer,
                            explanation = q.explanation,
                            isCorrect = false,
                            isPendingEvaluation = false,
                            codeSnippetPrefix = q.codeSnippetPrefix,
                        )
                    }

                    _uiState.value = QuizDetailUiState(
                        loaded = true,
                        quizSummary = summary,
                        conceptItem = concept,
                        questionReviews = reviews,
                    )
                } else {
                    _uiState.value = QuizDetailUiState(loaded = true)
                }
            }
        }
    }

    fun toggleStar(isStarred: Boolean) {
        val current = _uiState.value.quizSummary ?: return
        viewModelScope.launch {
            if (current.conceptId != null) {
                conceptRepository.updateStarStatus(current.conceptId, isStarred)
            }
            if (current.historySessionId != null) {
                historyRepository.updateStarStatus(current.historySessionId, isStarred)
            }
            _uiState.value = _uiState.value.copy(
                quizSummary = current.copy(isStarred = isStarred)
            )
        }
    }
}

